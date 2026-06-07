#!/usr/bin/env node
/**
 * 你画我猜 WebSocket 笔画通道 E2E
 *
 * 覆盖 Android DrawGuessLiveSync / DrawWsSession 真实协议：
 *   - health / joined / replay / stroke_chunk 广播 / ping-pong
 *   - 双端连接（画手发、猜词方收）
 *   - 断线重连 replay
 *
 * 用法：
 *   node pocketbase/tools/test_draw_ws_e2e.js
 *   node pocketbase/tools/test_draw_ws_e2e.js --base-url https://pb.yishi.site --ws-url wss://draw.yishi.site/ws
 *   node pocketbase/tools/test_draw_ws_e2e.js --ws-url ws://127.0.0.1:8790/ws
 */
"use strict";

const path = require("path");
const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

let WebSocket;
try {
  WebSocket = require(path.join(__dirname, "draw_ws", "node_modules", "ws"));
} catch {
  console.error("缺少 ws 依赖，请先执行: cd pocketbase/tools/draw_ws && npm install");
  process.exit(1);
}

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const WS_URL = arg("--ws-url", process.env.DRAW_WS_URL || "ws://127.0.0.1:8790/ws");
const JOIN_TIMEOUT_MS = Number(arg("--join-timeout", process.env.DRAW_WS_JOIN_TIMEOUT_MS || "8000"));
const ts = Date.now();

function drawGuessState(hostId, guestId, word = "测试词") {
  return {
    max_players: 2,
    min_players: 2,
    members: [
      { pb_id: hostId, seat: 0, status: "joined" },
      { pb_id: guestId, seat: 1, status: "joined" },
    ],
    member_ids: [hostId, guestId],
    draw_guess: {
      round: 1,
      phase: "drawing",
      drawer_pb_id: hostId,
      word,
      guesses: [],
      scores: { [hostId]: 0, [guestId]: 0 },
      stroke_seq: 0,
      max_rounds: 3,
      guess_limit: 5,
      draw_seconds: 60,
      guess_seconds: 90,
      phase_started_at_ms: Date.now(),
    },
  };
}

function healthHttpUrl(wsUrl) {
  const u = new URL(wsUrl.replace(/^wss:/, "https:").replace(/^ws:/, "http:"));
  if (u.pathname.endsWith("/ws")) {
    u.pathname = u.pathname.slice(0, -"/ws".length) + "/health";
  } else {
    u.pathname = "/health";
  }
  u.search = "";
  return u.toString();
}

async function fetchHealth(url) {
  const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
  if (!res.ok) return null;
  return res.json();
}

function connectWs(wsBase, token, roomId) {
  const u = new URL(wsBase);
  u.searchParams.set("token", token);
  u.searchParams.set("room", roomId);
  return new WebSocket(u.toString());
}

/** 从连接起缓冲消息，避免 joined 在 listener 注册前到达 */
function attachInbox(ws) {
  const inbox = [];
  ws.on("message", (data) => {
    try {
      inbox.push(JSON.parse(data.toString("utf8")));
    } catch {
      inbox.push({ _raw: data.toString("utf8") });
    }
  });
  return inbox;
}

async function connectReady(wsBase, token, roomId, timeoutMs) {
  const ws = connectWs(wsBase, token, roomId);
  const inbox = attachInbox(ws);
  let closeCode = null;
  let closeReason = "";
  ws.once("close", (code, reason) => {
    closeCode = code;
    closeReason = reason?.toString() || "";
  });

  await waitOpen(ws, timeoutMs);
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const joined = inbox.find((m) => m.t === "joined");
    if (joined) return { ws, inbox, joined };
    if (closeCode != null) {
      throw new Error(`ws_closed code=${closeCode} reason=${closeReason}`);
    }
    await sleep(50);
  }
  throw new Error(`join_timeout inbox=${JSON.stringify(inbox.slice(0, 3))}`);
}

function waitOpen(ws, ms) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("ws_open_timeout")), ms);
    ws.once("open", () => {
      clearTimeout(timer);
      resolve();
    });
    ws.once("error", (e) => {
      clearTimeout(timer);
      reject(e);
    });
  });
}

function collectMessages(ws, ms) {
  const list = [];
  return new Promise((resolve) => {
    const onMsg = (data) => {
      try {
        list.push(JSON.parse(data.toString("utf8")));
      } catch {
        list.push({ _raw: data.toString("utf8") });
      }
    };
    ws.on("message", onMsg);
    setTimeout(() => {
      ws.off("message", onMsg);
      resolve(list);
    }, ms);
  });
}

function waitForMessage(ws, predicate, ms) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      ws.off("message", onMsg);
      reject(new Error("wait_message_timeout"));
    }, ms);
    const onMsg = (data) => {
      let msg;
      try {
        msg = JSON.parse(data.toString("utf8"));
      } catch {
        return;
      }
      if (predicate(msg)) {
        clearTimeout(timer);
        ws.off("message", onMsg);
        resolve(msg);
      }
    };
    ws.on("message", onMsg);
  });
}

function closeWs(ws) {
  return new Promise((resolve) => {
    if (!ws || ws.readyState === WebSocket.CLOSED) {
      resolve();
      return;
    }
    ws.once("close", resolve);
    ws.close(1000, "test_done");
    setTimeout(resolve, 500);
  });
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== 你画我猜 WebSocket E2E ===");
  console.log("PB:  ", BASE_URL);
  console.log("WS:  ", WS_URL, "\n");

  // ── 0. 健康检查 ──
  console.log("▶ 服务健康");
  t.expect("PocketBase health", await client.health());
  if (!(await client.health())) {
    console.log("\nFAIL: PocketBase 不可达");
    process.exit(1);
  }

  const healthUrl = healthHttpUrl(WS_URL);
  let wsHealth = null;
  try {
    wsHealth = await fetchHealth(healthUrl);
  } catch (e) {
    console.log(`  ⚠ draw_ws health 不可达 (${healthUrl}): ${e.message}`);
  }
  t.expect("draw_ws health ok", wsHealth?.ok === true);
  if (!wsHealth?.ok) {
    console.log("\nFAIL: draw_ws 未启动。请先:");
    console.log("  cd pocketbase/tools/draw_ws");
    console.log(`  $env:PB_BASE_URL="${BASE_URL}"; npm start`);
    process.exit(1);
  }
  console.log(`  draw_ws rooms=${wsHealth.rooms} service=${wsHealth.service}`);

  // ── 1. 建局用户 + 房间 ──
  console.log("\n▶ 创建对局房间");
  const userA = await client.registerUser({
    localUserId: 960001 + (ts % 1000),
    funlifeUsername: `ws_draw_${ts}`,
    displayName: "WS画家",
    password: randomPassword(),
  });
  const userB = await client.registerUser({
    localUserId: 960002 + (ts % 1000),
    funlifeUsername: `ws_guess_${ts}`,
    displayName: "WS猜词",
    password: randomPassword(),
  });
  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr.id);

  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "draw_guess",
      invite_mode: "direct",
      host: userA.recordId,
      guest: userB.recordId,
      status: "playing",
      host_ready: true,
      guest_ready: true,
      current_turn: userA.recordId,
      game_state: drawGuessState(userA.recordId, userB.recordId),
    },
  });
  const roomId = room.id;
  t.expect("房间 playing + draw_guess", room.status === "playing" && room.game_type === "draw_guess");

  // ── 2. 双端连接 + joined ──
  console.log("\n▶ WebSocket 连接与 joined");
  let wsDrawer;
  let wsGuesser;
  let joinedDrawer;
  let joinedGuesser;

  try {
    const tJoin = performance.now();
    const drawerConn = await connectReady(WS_URL, userA.token, roomId, JOIN_TIMEOUT_MS);
    wsDrawer = drawerConn.ws;
    joinedDrawer = drawerConn.joined;
    const guesserConn = await connectReady(WS_URL, userB.token, roomId, JOIN_TIMEOUT_MS);
    wsGuesser = guesserConn.ws;
    joinedGuesser = guesserConn.joined;
    t.expect("画手 joined", joinedDrawer.userId === userA.recordId);
    t.expect("猜词方 joined", joinedGuesser.userId === userB.recordId);
    console.log(`  joined 握手完成: ${(performance.now() - tJoin).toFixed(0)}ms`);

    // ── 3. ping/pong ──
    console.log("\n▶ ping/pong");
    wsDrawer.send(JSON.stringify({ t: "ping" }));
    const pong = await waitForMessage(wsDrawer, (m) => m.t === "pong", 3000);
    t.expect("pong 响应", typeof pong.ts === "number");

    // ── 4. stroke_chunk 广播（画手 → 猜词方）──
    console.log("\n▶ stroke_chunk 热路径广播");
    const strokeId = "teststroke01";
    const chunkPayload = {
      t: "stroke_chunk",
      room: roomId,
      strokeId,
      chunk: 0,
      round: 1,
      color: "#222222",
      width: 4,
      points: [
        [0.1, 0.2],
        [0.15, 0.25],
        [0.2, 0.3],
      ],
    };
    const rxPromise = waitForMessage(
      wsGuesser,
      (m) => m.t === "stroke_chunk" && m.strokeId === strokeId,
      5000,
    );
    const t0 = performance.now();
    wsDrawer.send(JSON.stringify(chunkPayload));
    const rxChunk = await rxPromise;
    const latencyMs = performance.now() - t0;
    const isLocalWs = /localhost|127\.0\.0\.1/i.test(WS_URL);
    const latencyBudgetMs = isLocalWs ? 800 : 3000;
    console.log(`  猜词方收到 chunk: ${latencyMs.toFixed(0)}ms (budget ${latencyBudgetMs}ms)`);
    t.expect("猜词方收到 stroke_chunk", !!rxChunk);
    t.expect("chunk from=画手", rxChunk?.from === userA.recordId);
    t.expect("chunk 点数=3", rxChunk?.points?.length === 3);
    t.expect(`热路径延迟 < ${latencyBudgetMs}ms`, latencyMs < latencyBudgetMs, `${latencyMs.toFixed(0)}ms`);

    // 画手不应收到自己的 echo
    const drawerEcho = await collectMessages(wsDrawer, 300);
    const selfEcho = drawerEcho.find((m) => m.t === "stroke_chunk" && m.strokeId === strokeId);
    t.expect("画手不收 echo", !selfEcho);

    // ── 5. stroke_end + clear ──
    console.log("\n▶ stroke_end / clear");
    const endCollector = collectMessages(wsGuesser, 2000);
    wsDrawer.send(
      JSON.stringify({
        t: "stroke_end",
        room: roomId,
        strokeId,
        round: 1,
        seq: 1,
        color: "#222222",
        width: 4,
        points: chunkPayload.points,
      }),
    );
    const endMsgs = await endCollector;
    t.expect("猜词方收到 stroke_end", endMsgs.some((m) => m.t === "stroke_end" && m.strokeId === strokeId));

    const clearCollector = collectMessages(wsGuesser, 2000);
    wsDrawer.send(JSON.stringify({ t: "clear", room: roomId, round: 1 }));
    const clearMsgs = await clearCollector;
    t.expect("猜词方收到 clear", clearMsgs.some((m) => m.t === "clear"));

    // ── 6. 断线 replay ──
    console.log("\n▶ 断线重连 replay");
    wsDrawer.send(
      JSON.stringify({
        t: "stroke_chunk",
        room: roomId,
        strokeId: "replay01",
        chunk: 0,
        round: 1,
        color: "#ff0000",
        width: 3,
        points: [
          [0.4, 0.4],
          [0.45, 0.45],
        ],
      }),
    );
    await sleep(200);

    const guesser2Conn = await connectReady(WS_URL, userB.token, roomId, JOIN_TIMEOUT_MS);
    const wsGuesser2 = guesser2Conn.ws;
    const msgs2 = guesser2Conn.inbox;
    await sleep(500);
    t.expect("重连收到 joined", msgs2.some((m) => m.t === "joined"));
    const replay = msgs2.find((m) => m.t === "replay");
    t.expect("重连收到 replay", !!replay);
    const replayChunks = (replay?.events || []).filter((e) => e.t === "stroke_chunk");
    t.expect("replay 含 stroke_chunk", replayChunks.length >= 1);
    await closeWs(wsGuesser2);

    // ── 7. 模拟进局 bootstrap 时序（joined 应在超时前完成）──
    console.log("\n▶ 模拟 App bootstrap 时序 (4s 硬上限)");
    const bootstrapStart = performance.now();
    const bootConn = await connectReady(WS_URL, userA.token, roomId, JOIN_TIMEOUT_MS);
    const wsBoot = bootConn.ws;
    const bootstrapMs = performance.now() - bootstrapStart;
    console.log(`  bootstrap WS 就绪: ${bootstrapMs.toFixed(0)}ms`);
    t.expect("bootstrap < 4000ms (App 硬上限)", bootstrapMs < 4000, `${bootstrapMs.toFixed(0)}ms`);
    t.expect("bootstrap < 2500ms (App WS 宽限)", bootstrapMs < 2500, `${bootstrapMs.toFixed(0)}ms (warn)`);
    await closeWs(wsBoot);

    // ── 8. 非法 token 应拒绝 ──
    console.log("\n▶ 鉴权失败路径");
    const wsBad = connectWs(WS_URL, "invalid.token.here", roomId);
    await waitOpen(wsBad, 3000).catch(() => {});
    await new Promise((resolve) => {
      wsBad.once("close", resolve);
      setTimeout(resolve, 3000);
    });
    t.expect("非法 token 连接关闭", wsBad.readyState === WebSocket.CLOSED);
  } finally {
    await closeWs(wsDrawer);
    await closeWs(wsGuesser);
  }

  console.log("\n---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
