#!/usr/bin/env node
/**
 * 对局同步延迟对比基准（上一版 vs 当前版）
 *
 * 用法：
 *   node pocketbase/tools/test_game_sync_latency.js --base-url https://pb.yishi.site
 */
"use strict";

const { PbSocialClient, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}
function hasFlag(name) {
  return args.includes(name);
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const SKIP_SSE = hasFlag("--skip-sse");
const ts = Date.now();

/** 上一次测试记录（2026-06-03 公网 pb.yishi.site，优化 v2） */
const PREV = {
  entrySequential: 998,
  entryParallel: 572,
  metaLite: 501,
  pollPerceive: 498,
  pollIntervalMs: 1500,
  moveAftermathHttp: 851, // 350 debounce + 501 meta
  lobbyPollLiveMs: 800,
  lobbyPollOfflineMs: 1200,
};

/** 更早期（优化 v1 前，test_game_play_latency 估算） */
const LEGACY = {
  entryExpandRoom: 2000,
  entryTotal: 3300,
  pollIntervalMs: 3000,
  moveAftermathHttp: 1350, // 350 + 998 sequential refresh
};

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function avg(arr) {
  if (!arr.length) return 0;
  return arr.reduce((a, b) => a + b, 0) / arr.length;
}

function pct(saved, base) {
  if (!base) return "—";
  return `${((saved / base) * 100).toFixed(1)}%`;
}

function row(label, before, after) {
  const saved = before - after;
  const tag = saved > 0 ? "faster" : saved < 0 ? "slower" : "same";
  const abs = Math.abs(saved).toFixed(0);
  console.log(
    `  ${label.padEnd(22)} ${String(Math.round(before)).padStart(5)}ms -> ${String(Math.round(after)).padStart(5)}ms  ${tag} ${abs}ms (${pct(Math.max(saved, 0), before)})`,
  );
  return { before, after, saved: Math.max(saved, 0) };
}

async function fetchRoom(client, token, roomId, expand) {
  const q = expand ? `?expand=${encodeURIComponent("host,guest")}` : "";
  const { json } = await client.request("GET", `/collections/game_rooms/records/${roomId}${q}`, { token });
  return json;
}

async function listMoves(client, token, roomId, afterIndex = 0) {
  const filter =
    afterIndex > 0
      ? encodeURIComponent(`room = '${roomId}' && move_index > ${afterIndex}`)
      : encodeURIComponent(`room = '${roomId}'`);
  const { json } = await client.request(
    "GET",
    `/collections/game_moves/records?filter=${filter}&sort=move_index&perPage=50`,
    { token },
  );
  return json.items || [];
}

async function createMove(client, token, roomId, playerId, moveIndex, x, y) {
  const t0 = performance.now();
  await client.request("POST", "/collections/game_moves/records", {
    token,
    body: {
      room: roomId,
      player: playerId,
      move_index: moveIndex,
      payload: { kind: "gomoku_place", x, y },
    },
  });
  return performance.now() - t0;
}

async function fetchSequential(client, token, roomId) {
  const t0 = performance.now();
  await fetchRoom(client, token, roomId, true);
  await listMoves(client, token, roomId);
  return performance.now() - t0;
}

async function fetchParallel(client, token, roomId) {
  const t0 = performance.now();
  await Promise.all([fetchRoom(client, token, roomId, true), listMoves(client, token, roomId)]);
  return performance.now() - t0;
}

async function fetchMetaLite(client, token, roomId) {
  const t0 = performance.now();
  await fetchRoom(client, token, roomId, false);
  return performance.now() - t0;
}

/** v3：lite room + 增量 moves 并行 */
async function fetchIncremental(client, token, roomId, afterIndex) {
  const t0 = performance.now();
  await Promise.all([
    fetchRoom(client, token, roomId, false),
    listMoves(client, token, roomId, afterIndex),
  ]);
  return performance.now() - t0;
}

/** 模拟旧版落子后同步：debounce + 串行全量 */
async function simulateOldMoveAftermath(client, token, roomId) {
  await sleep(350);
  return fetchSequential(client, token, roomId);
}

/** 模拟 v2：debounce + lite meta */
async function simulateV2MoveAftermath(client, token, roomId) {
  await sleep(100);
  return fetchMetaLite(client, token, roomId);
}

/** v3：PLAYING 期间 0 HTTP */
async function simulateV3MoveAftermath() {
  return 0;
}

async function pollUntilMove(client, token, roomId, moveIndex, intervalMs, timeoutMs = 8000) {
  const t0 = performance.now();
  while (performance.now() - t0 < timeoutMs) {
    const moves = await listMoves(client, token, roomId);
    if (moves.some((m) => m.move_index === moveIndex)) {
      return performance.now() - t0;
    }
    await sleep(intervalMs);
  }
  return null;
}

async function measureSseMoveLatency(client, token, roomId, expectedMoveIndex, triggerMove) {
  const apiBase = `${BASE_URL.replace(/\/$/, "")}/api`;
  const controller = new AbortController();
  const tConnect = performance.now();

  const res = await fetch(`${apiBase}/realtime`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "text/event-stream",
      "Cache-Control": "no-cache",
    },
    signal: controller.signal,
  });
  if (!res.ok) throw new Error(`SSE connect failed ${res.status}`);

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let clientId = null;
  let moveReceivedAt = null;
  let connectMs = null;

  const parseBlock = (block) => {
    const lines = block.split("\n");
    let event = "";
    let data = "";
    for (const line of lines) {
      if (line.startsWith("event:")) event = line.slice(6).trim();
      if (line.startsWith("data:")) data += (data ? "\n" : "") + line.slice(5).trim();
    }
    if (!data) return;
    const ev = event || (data.includes("clientId") ? "PB_CONNECT" : "PB_EVENT");
    if (ev === "PB_CONNECT" || data.includes("clientId")) {
      clientId = JSON.parse(data).clientId;
      connectMs = performance.now() - tConnect;
      return;
    }
    if (ev !== "PB_EVENT" || !data.includes("move_index")) return;
    try {
      const root = JSON.parse(data);
      if (root.action !== "create" && root.action !== "update") return;
      const rec = root.record;
      if (!rec || rec.move_index == null) return;
      const moveRoom =
        typeof rec.room === "string" ? rec.room : rec.room?.id || rec.expand?.room?.id || rec.expand?.room;
      if (!moveRoom || moveRoom !== roomId) return;
      if (rec.move_index === expectedMoveIndex) moveReceivedAt = performance.now();
    } catch {
      /* ignore */
    }
  };

  const readLoop = (async () => {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let idx;
      while ((idx = buffer.indexOf("\n\n")) >= 0) {
        parseBlock(buffer.slice(0, idx));
        buffer = buffer.slice(idx + 2);
      }
    }
  })();

  const waitConnect = performance.now();
  while (!clientId && performance.now() - waitConnect < 5000) await sleep(20);
  if (!clientId) {
    controller.abort();
    throw new Error("SSE: no clientId within 5s");
  }

  await client.request("POST", "/realtime", {
    token,
    body: { clientId, subscriptions: ["game_moves", "game_rooms"] },
  });
  await sleep(200);

  const tMove = performance.now();
  await triggerMove();
  const createMs = performance.now() - tMove;

  const waitMove = performance.now();
  while (!moveReceivedAt && performance.now() - waitMove < 8000) await sleep(10);

  controller.abort();
  try {
    await readLoop;
  } catch {
    /* aborted */
  }

  return {
    connectMs,
    createMs,
    sseLatencyMs: moveReceivedAt ? moveReceivedAt - tMove : null,
  };
}

/** 上一版 v3 测试基准（pb.yishi.site 2026-06-03） */
const PREV_V3 = {
  entryParallel: 544,
  placeHttpOld: 1500, // list + create + patch 估算
  placeHttpNew: 500, // 仅 create
  moveAftermath: 0,
};

/** v4 实测基准（pb.yishi.site 2026-06-03，落子前仍可能 PATCH） */
const PREV_V4 = {
  patchTurnOnly: 527,
  placeWithPrePatch: 1073, // patch + POST 估算
  placePostOnly: 546,
};

/** 大厅进房 / 接受邀请（优化前 App 估算） */
const PREV_LOBBY = {
  enterOldSeq: 1600, // refreshRooms 2 list + getGameRoom expand
  enterNewLite: 550,
  hostPerceivePoll800: 1300, // 800ms 轮询 + lite GET
  hostPerceivePoll350: 900,
  guestAcceptPatch: 550,
};

function lobbyInitialState(hostId) {
  return {
    max_players: 2,
    min_players: 2,
    members: [{ pb_id: hostId, seat: 0, status: "joined" }],
    member_ids: [hostId],
  };
}

function withPendingInvite(state, guestId) {
  return {
    ...state,
    pending_invite_pb_id: guestId,
    members: [
      ...state.members,
      { pb_id: guestId, seat: 1, status: "pending" },
    ],
    member_ids: [...new Set([...state.member_ids, guestId])],
  };
}

function withAcceptedInvite(state, guestId) {
  const members = state.members.map((m) =>
    m.pb_id === guestId ? { ...m, status: "joined" } : m,
  );
  return {
    ...state,
    pending_invite_pb_id: null,
    members,
    member_ids: [...new Set([...state.member_ids, guestId])],
  };
}

function joinedCount(roomJson) {
  return (roomJson.game_state?.members || []).filter((m) => m.status === "joined").length;
}

async function listMyGameRooms(client, token, myPbId) {
  const statusClause = ["waiting", "accepted", "playing"].map((s) => `status = '${s}'`).join(" || ");
  const filter = encodeURIComponent(
    `(host = '${myPbId}' || guest = '${myPbId}' || game_state ~ '${myPbId}') && (${statusClause})`,
  );
  const { json } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${filter}&perPage=50&sort=-updated`,
    { token },
  );
  return json.items || [];
}

async function listIncomingGameInvites(client, token, myPbId) {
  const filter = encodeURIComponent(
    `(game_state.pending_invite_pb_id = '${myPbId}' || guest = '${myPbId}') && status = 'waiting'`,
  );
  const { json } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${filter}&perPage=20&sort=-updated`,
    { token },
  );
  return json.items || [];
}

async function timedRoomFetch(client, token, roomId, expand) {
  const t0 = performance.now();
  await fetchRoom(client, token, roomId, expand);
  return performance.now() - t0;
}

/** 旧版进大厅：全量 list + expand room（GameLobbyScreen 旧 LaunchedEffect） */
async function simulateOldLobbyEntry(client, token, myPbId, roomId) {
  const t0 = performance.now();
  await listMyGameRooms(client, token, myPbId);
  await listIncomingGameInvites(client, token, myPbId);
  await fetchRoom(client, token, roomId, true);
  return performance.now() - t0;
}

/** 新版进大厅：仅 lite room（enterLobby / startLobbyWatch 首次刷新） */
async function simulateNewLobbyEntry(client, token, roomId) {
  return timedRoomFetch(client, token, roomId, false);
}

async function pollHostUntilGuestJoined(client, token, roomId, intervalMs, timeoutMs = 10_000) {
  const t0 = performance.now();
  while (performance.now() - t0 < timeoutMs) {
    const { json } = await client.request("GET", `/collections/game_rooms/records/${roomId}`, {
      token,
    });
    if (joinedCount(json) >= 2) return performance.now() - t0;
    await sleep(intervalMs);
  }
  return null;
}

async function setupInviteRoom(client, userA, userB) {
  const hostId = userA.recordId;
  const guestId = userB.recordId;
  const { json: lobbyRoom } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "gomoku",
      invite_mode: "direct",
      host: hostId,
      status: "waiting",
      host_ready: true,
      game_state: lobbyInitialState(hostId),
    },
  });
  const roomId = lobbyRoom.id;
  const pendingState = withPendingInvite(lobbyInitialState(hostId), guestId);
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token: userA.token,
    body: {
      invite_mode: "direct",
      guest: guestId,
      game_state: pendingState,
      status: "waiting",
    },
  });
  return { roomId, pendingState, hostId, guestId };
}

async function guestAcceptInvite(client, token, roomId, pendingState, guestId) {
  const t0 = performance.now();
  const acceptedState = withAcceptedInvite(pendingState, guestId);
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body: {
      game_state: acceptedState,
      status: "accepted",
      guest: guestId,
      guest_ready: true,
      host_ready: true,
    },
  });
  return performance.now() - t0;
}

async function measureLobbyAcceptFlow(client, userA, userB) {
  const { roomId, pendingState, guestId } = await setupInviteRoom(client, userA, userB);

  const enterOld = await simulateOldLobbyEntry(client, userB.token, guestId, roomId);
  const enterNew = await simulateNewLobbyEntry(client, userB.token, roomId);
  const guestAcceptMs = await guestAcceptInvite(client, userB.token, roomId, pendingState, guestId);

  const hostLiteMs = await timedRoomFetch(client, userA.token, roomId, false);
  const hostExpandMs = await timedRoomFetch(client, userA.token, roomId, true);

  return { roomId, enterOld, enterNew, guestAcceptMs, hostLiteMs, hostExpandMs };
}

async function measureHealth(client) {
  const t0 = performance.now();
  await client.health();
  return performance.now() - t0;
}

async function simulateOldPlaceChain(client, token, roomId, playerId, moveIndex, x, y) {
  const t0 = performance.now();
  await listMoves(client, token, roomId);
  await client.request("POST", "/collections/game_moves/records", {
    token,
    body: { room: roomId, player: playerId, move_index: moveIndex, payload: { kind: "gomoku_place", x, y } },
  });
  await fetchRoom(client, token, roomId, true);
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body: { current_turn: playerId },
  });
  return performance.now() - t0;
}

async function simulateNewPlaceChain(client, token, roomId, playerId, moveIndex, x, y) {
  const t0 = performance.now();
  await client.request("POST", "/collections/game_moves/records", {
    token,
    body: { room: roomId, player: playerId, move_index: moveIndex, payload: { kind: "gomoku_place", x, y } },
  });
  return performance.now() - t0;
}

/** v4 App：落子前先 patchCurrentTurnOnly，再 POST（最多 2 RTT） */
async function simulateV4PlaceChainWithPrePatch(client, token, roomId, playerId, moveIndex, x, y) {
  const t0 = performance.now();
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body: { current_turn: playerId },
  });
  await client.request("POST", "/collections/game_moves/records", {
    token,
    body: { room: roomId, player: playerId, move_index: moveIndex, payload: { kind: "gomoku_place", x, y } },
  });
  return performance.now() - t0;
}

/** v5 App：仅 POST（PB hook 写棋盘+回合，去掉落子前 PATCH） */
async function simulateV5PlaceChain(client, token, roomId, playerId, moveIndex, x, y) {
  return simulateNewPlaceChain(client, token, roomId, playerId, moveIndex, x, y);
}

async function measurePatchCurrentTurnOnly(client, token, roomId, nextTurnPbId) {
  const t0 = performance.now();
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body: { current_turn: nextTurnPbId },
  });
  return performance.now() - t0;
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  console.log("\n=== 对局同步延迟对比（上一版 vs 当前版）===");
  console.log("Base:", BASE_URL, "\n");

  if (!(await client.health())) {
    console.log("FAIL: PocketBase 不可达");
    process.exit(1);
  }

  const userA = await client.registerUser({
    localUserId: 960001 + (ts % 1000),
    funlifeUsername: `sync_a_${ts}`,
    displayName: "同步A",
    password: randomPassword(),
  });
  const userB = await client.registerUser({
    localUserId: 960002 + (ts % 1000),
    funlifeUsername: `sync_b_${ts}`,
    displayName: "同步B",
    password: randomPassword(),
  });

  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "gomoku",
      invite_mode: "direct",
      host: userA.recordId,
      guest: userB.recordId,
      status: "playing",
      host_ready: true,
      guest_ready: true,
      current_turn: userA.recordId,
      game_state: {
        max_players: 2,
        min_players: 2,
        members: [
          { pb_id: userA.recordId, seat: 0, status: "joined" },
          { pb_id: userB.recordId, seat: 1, status: "joined" },
        ],
        member_ids: [userA.recordId, userB.recordId],
        gomoku: {
          board: ".".repeat(225),
          move_count: 0,
          black_pb_id: userA.recordId,
          white_pb_id: userB.recordId,
        },
      },
    },
  });
  const roomId = room.id;

  console.log("▶ 连接预热（冷 vs 热，模拟 ConnectionPool）\n");
  const coldHealth = await measureHealth(client);
  const warmSamples = [];
  for (let i = 0; i < 5; i++) warmSamples.push(await measureHealth(client));
  const warmHealth = avg(warmSamples);
  console.log(`  首次 health: ${coldHealth.toFixed(0)}ms`);
  console.log(`  复用连接后(5次均值): ${warmHealth.toFixed(0)}ms`);
  console.log(`  连接复用节省: ~${Math.max(0, coldHealth - warmHealth).toFixed(0)}ms\n`);

  console.log("▶ 实测采样（各 5 次均值）\n");

  const seqSamples = [];
  const parSamples = [];
  const metaSamples = [];
  const incSamples = [];
  for (let i = 0; i < 5; i++) {
    seqSamples.push(await fetchSequential(client, userB.token, roomId));
    parSamples.push(await fetchParallel(client, userB.token, roomId));
    metaSamples.push(await fetchMetaLite(client, userB.token, roomId));
  }

  let moveIndex = 1;
  for (const [x, y] of [
    [7, 7],
    [7, 8],
    [8, 7],
  ]) {
    await createMove(client, userA.token, roomId, userA.recordId, moveIndex, x, y);
    moveIndex++;
  }
  const afterIndex = moveIndex - 1;
  for (let i = 0; i < 5; i++) {
    incSamples.push(await fetchIncremental(client, userB.token, roomId, afterIndex));
  }

  const cur = {
    entrySequential: avg(seqSamples),
    entryParallel: avg(parSamples),
    metaLite: avg(metaSamples),
    incrementalPoll: avg(incSamples),
    pollIntervalMs: 1200,
    moveAftermathV3: 0,
    lobbyPollOfflineMs: 1200,
  };

  const oldAftermathSamples = [];
  const v2AftermathSamples = [];
  for (let i = 0; i < 3; i++) {
    oldAftermathSamples.push(await simulateOldMoveAftermath(client, userB.token, roomId));
    v2AftermathSamples.push(await simulateV2MoveAftermath(client, userB.token, roomId));
  }
  const oldAftermath = avg(oldAftermathSamples);
  const v2Aftermath = avg(v2AftermathSamples);

  console.log("  指标                    上一版      当前版      提升");
  console.log("  " + "-".repeat(58));
  const results = [];
  results.push(row("进房(串行)", PREV.entrySequential, cur.entrySequential));
  results.push(row("进房(并行)", PREV.entryParallel, cur.entryParallel));
  results.push(row("落子后 meta(lite)", PREV.metaLite, cur.metaLite));
  results.push(row("轮询增量对账", PREV.metaLite + 200, cur.incrementalPoll));
  results.push(row("落子后 HTTP(v2)", PREV.moveAftermathHttp, v2Aftermath));
  results.push(row("落子后 HTTP(v3)", PREV.moveAftermathHttp, cur.moveAftermathV3));
  results.push(row("离线轮询周期", PREV.pollIntervalMs, cur.pollIntervalMs));

  console.log("\n▶ 落子传播（150ms 轮询粒度）");
  const poll150 = [];
  for (const [x, y] of [
    [9, 9],
    [10, 9],
    [9, 10],
  ]) {
    const t0 = performance.now();
    await createMove(client, userA.token, roomId, userA.recordId, moveIndex, x, y);
    const postMs = performance.now() - t0;
    const seen = await pollUntilMove(client, userB.token, roomId, moveIndex, 150);
    poll150.push(seen ?? 8000);
    console.log(`  move#${moveIndex}: POST ${postMs.toFixed(0)}ms | B 看见 ${seen ? seen.toFixed(0) : "超时"}ms`);
    moveIndex++;
  }
  cur.pollPerceive = avg(poll150);

  console.log("\n▶ 落子链路对比（旧 4 HTTP vs 新 1 HTTP）");
  let mi = moveIndex;
  const oldPlaceSamples = [];
  const newPlaceSamples = [];
  for (const [x, y] of [
    [11, 11],
    [12, 11],
    [11, 12],
  ]) {
    oldPlaceSamples.push(
      await simulateOldPlaceChain(client, userA.token, roomId, userA.recordId, mi, x, y),
    );
    mi++;
  }
  for (const [x, y] of [
    [13, 13],
    [14, 13],
    [13, 14],
  ]) {
    newPlaceSamples.push(
      await simulateNewPlaceChain(client, userA.token, roomId, userA.recordId, mi, x, y),
    );
    mi++;
  }
  cur.placeOld = avg(oldPlaceSamples);
  cur.placeNew = avg(newPlaceSamples);
  row("落子链路(旧)", cur.placeOld, cur.placeNew);
  moveIndex = mi;

  console.log("\n▶ v5 落子链路（去掉落子前 patchCurrentTurnOnly）");
  const patchOnlySamples = [];
  for (let i = 0; i < 3; i++) {
    patchOnlySamples.push(
      await measurePatchCurrentTurnOnly(client, userA.token, roomId, userB.recordId),
    );
  }
  cur.patchTurnOnly = avg(patchOnlySamples);

  const v4PrePatchSamples = [];
  const v5PostOnlySamples = [];
  for (const [x, y] of [
    [15, 15],
    [16, 15],
    [15, 16],
  ]) {
    v4PrePatchSamples.push(
      await simulateV4PlaceChainWithPrePatch(
        client,
        userA.token,
        roomId,
        userA.recordId,
        moveIndex,
        x,
        y,
      ),
    );
    moveIndex++;
  }
  for (const [x, y] of [
    [17, 17],
    [18, 17],
    [17, 18],
  ]) {
    v5PostOnlySamples.push(
      await simulateV5PlaceChain(client, userA.token, roomId, userA.recordId, moveIndex, x, y),
    );
    moveIndex++;
  }
  cur.placeV4WithPatch = avg(v4PrePatchSamples);
  cur.placeV5PostOnly = avg(v5PostOnlySamples);

  console.log("  指标                    v4 基准     当前实测     提升");
  console.log("  " + "-".repeat(58));
  row("落子前 PATCH 单项", PREV_V4.patchTurnOnly, cur.patchTurnOnly);
  row("落子 v4(patch+POST)", PREV_V4.placeWithPrePatch, cur.placeV4WithPatch);
  row("落子 v5(仅 POST)", PREV_V4.placePostOnly, cur.placeV5PostOnly);

  row("对手轮询感知", PREV.pollPerceive, cur.pollPerceive);

  let sseMs = null;
  if (!SKIP_SSE) {
    console.log("\n▶ Realtime SSE");
    try {
      const sse = await measureSseMoveLatency(client, userB.token, roomId, moveIndex, async () => {
        await createMove(client, userA.token, roomId, userA.recordId, moveIndex, 11, 11);
      });
      sseMs = sse.sseLatencyMs;
      console.log(`  连接 ${sse.connectMs?.toFixed(0) ?? "?"}ms | POST ${sse.createMs.toFixed(0)}ms | SSE ${sseMs != null ? sseMs.toFixed(0) : "超时"}ms`);
      moveIndex++;
    } catch (e) {
      console.log(`  SSE 跳过: ${e.message}`);
    }
  }

  console.log("\n=== 对比汇总（v3 -> v4）===");
  console.log(`  entry parallel: ${PREV_V3.entryParallel}ms -> ${Math.round(cur.entryParallel)}ms, saved ${Math.max(0, PREV_V3.entryParallel - cur.entryParallel).toFixed(0)}ms`);
  console.log(`  place chain: ~${Math.round(cur.placeOld)}ms -> ~${Math.round(cur.placeNew)}ms, saved ~${Math.max(0, cur.placeOld - cur.placeNew).toFixed(0)}ms (${pct(Math.max(0, cur.placeOld - cur.placeNew), cur.placeOld)})`);
  console.log(`  connection pool: cold ${coldHealth.toFixed(0)}ms -> warm ${warmHealth.toFixed(0)}ms`);
  console.log(`  legacy entry ~3300ms -> now ~${Math.round(cur.entryParallel)}ms (${pct(3300 - cur.entryParallel, 3300)})`);
  console.log("\n=== 对比汇总（v4 -> v5）===");
  console.log(
    `  patchCurrentTurnOnly: ~${PREV_V4.patchTurnOnly}ms -> ~${Math.round(cur.patchTurnOnly)}ms (v5 落子路径已跳过)`,
  );
  console.log(
    `  place chain: ~${Math.round(cur.placeV4WithPatch)}ms -> ~${Math.round(cur.placeV5PostOnly)}ms, saved ~${Math.max(0, cur.placeV4WithPatch - cur.placeV5PostOnly).toFixed(0)}ms (${pct(Math.max(0, cur.placeV4WithPatch - cur.placeV5PostOnly), cur.placeV4WithPatch)})`,
  );
  console.log(`  v5 统一 SSE: App 内 FriendRealtimeHub 订阅 game_moves；本脚本 SSE 仍仅作参考`);
  if (sseMs != null) {
    console.log(`  Realtime SSE: ~${sseMs.toFixed(0)}ms vs poll ~${Math.round(cur.pollPerceive)}ms`);
  } else {
    console.log(`  Realtime SSE: script timeout; App uses SSE + 0 HTTP on opponent side`);
  }

  console.log("\n▶ 大厅：接受邀请 / 进房 / 房主看见宾客（HTTP 模拟）");
  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr.id);

  const lobbySamples = [];
  for (let i = 0; i < 3; i++) {
    lobbySamples.push(await measureLobbyAcceptFlow(client, userA, userB));
  }
  const lobby = {
    enterOld: avg(lobbySamples.map((s) => s.enterOld)),
    enterNew: avg(lobbySamples.map((s) => s.enterNew)),
    guestAccept: avg(lobbySamples.map((s) => s.guestAcceptMs)),
    hostLite: avg(lobbySamples.map((s) => s.hostLiteMs)),
    hostExpand: avg(lobbySamples.map((s) => s.hostExpandMs)),
  };

  console.log("  指标                    旧版估算    当前实测     提升");
  console.log("  " + "-".repeat(58));
  row("宾客进大厅(旧 list+expand)", PREV_LOBBY.enterOldSeq, lobby.enterOld);
  row("宾客进大厅(新 lite)", PREV_LOBBY.enterNewLite, lobby.enterNew);
  row("宾客 accept PATCH", PREV_LOBBY.guestAcceptPatch, lobby.guestAccept);
  row("房主拉 room lite", PREV_LOBBY.enterNewLite, lobby.hostLite);
  row("房主拉 room expand", PREV_LOBBY.enterOldSeq / 3, lobby.hostExpand);

  console.log("\n  房主「看见宾客」轮询模拟（accept 后立即 poll + lite GET）");
  const poll800 = [];
  const poll350 = [];
  for (let i = 0; i < 2; i++) {
    const setup = await setupInviteRoom(client, userA, userB);
    await guestAcceptInvite(client, userB.token, setup.roomId, setup.pendingState, setup.guestId);
    const seen800 = await pollHostUntilGuestJoined(client, userA.token, setup.roomId, 800);
    poll800.push(seen800 ?? 10_000);
    console.log(`    轮询#${i + 1} 800ms 粒度: ${seen800 ? seen800.toFixed(0) : "超时"}ms`);

    const setup2 = await setupInviteRoom(client, userA, userB);
    await guestAcceptInvite(client, userB.token, setup2.roomId, setup2.pendingState, setup2.guestId);
    const seen350 = await pollHostUntilGuestJoined(client, userA.token, setup2.roomId, 350);
    poll350.push(seen350 ?? 10_000);
    console.log(`    轮询#${i + 1} 350ms 粒度: ${seen350 ? seen350.toFixed(0) : "超时"}ms`);
  }
  lobby.hostPoll800 = avg(poll800);
  lobby.hostPoll350 = avg(poll350);
  row("房主轮询感知(800ms)", PREV_LOBBY.hostPerceivePoll800, lobby.hostPoll800);
  row("房主轮询感知(350ms)", PREV_LOBBY.hostPerceivePoll350, lobby.hostPoll350);

  console.log("\n=== 对比汇总（大厅 accept → 房主看见）===");
  console.log(
    `  宾客进房 HTTP: ~${Math.round(lobby.enterOld)}ms -> ~${Math.round(lobby.enterNew)}ms, saved ~${Math.max(0, lobby.enterOld - lobby.enterNew).toFixed(0)}ms (${pct(Math.max(0, lobby.enterOld - lobby.enterNew), lobby.enterOld)})`,
  );
  console.log(
    `  房主轮询兜底: 800ms -> 350ms 粒度, ~${Math.round(lobby.hostPoll800)}ms -> ~${Math.round(lobby.hostPoll350)}ms`,
  );
  console.log("  App SSE 即时写缓存（cacheRoomFromRemoteDto）: 本脚本不可测，真机看 FriendRealtimeHub / GameRoomSync 日志");
  console.log("");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
