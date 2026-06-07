#!/usr/bin/env node
/**
 * 你画我猜同步 E2E — 覆盖 App 真实链路：
 *   POST game_moves (draw_stroke / draw_phase / draw_clear / draw_guess)
 *   PATCH game_rooms (game_state.draw_guess)
 *   pb_hooks draw_guess 兜底（stroke_seq / phase / current_turn）
 *
 * 用法：
 *   node pocketbase/tools/test_draw_guess_sync_e2e.js
 *   node pocketbase/tools/test_draw_guess_sync_e2e.js --base-url https://pb.yishi.site
 */
"use strict";

const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const ts = Date.now();

function relationId(field) {
  if (!field) return "";
  if (typeof field === "string") return field;
  return field.id || "";
}

function drawGuessState(hostId, guestId, word = "苹果") {
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

function strokeRound(payload, currentRound) {
  if (payload.round == null) return currentRound === 1;
  return payload.round === currentRound;
}

function parseStrokesForRound(moves, round) {
  const strokes = [];
  for (const move of moves.sort((a, b) => a.move_index - b.move_index)) {
    const p = move.payload || {};
    const kind = p.kind;
    if (kind === "draw_clear" && strokeRound(p, round)) {
      strokes.length = 0;
      continue;
    }
    if (kind === "draw_phase" && p.phase === "drawing" && strokeRound(p, round)) {
      strokes.length = 0;
      continue;
    }
    if (kind === "draw_stroke" && strokeRound(p, round)) {
      strokes.push({ seq: p.seq, player: move.player, points: p.points?.length || 0 });
    }
  }
  return strokes;
}

async function listMoves(client, token, roomId) {
  const filter = encodeURIComponent(`room = '${roomId}'`);
  const { json } = await client.request(
    "GET",
    `/collections/game_moves/records?filter=${filter}&sort=move_index&perPage=100`,
    { token },
  );
  return json.items || [];
}

async function createMove(client, token, roomId, playerId, moveIndex, payload) {
  const { json } = await client.request("POST", "/collections/game_moves/records", {
    token,
    body: { room: roomId, player: playerId, move_index: moveIndex, payload },
  });
  return json;
}

async function patchRoom(client, token, roomId, body) {
  const { json } = await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body,
  });
  return json;
}

async function getRoom(client, token, roomId) {
  const { json } = await client.request("GET", `/collections/game_rooms/records/${roomId}`, { token });
  return json;
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== 你画我猜同步 E2E ===\nBase:", BASE_URL, "\n");

  t.expect("health", await client.health());
  if (!(await client.health())) {
    console.log("\nFAIL: PocketBase 不可达，请先启动 PB");
    process.exit(1);
  }

  const userA = await client.registerUser({
    localUserId: 950001 + (ts % 1000),
    funlifeUsername: `draw_a_${ts}`,
    displayName: "画家A",
    password: randomPassword(),
  });
  const userB = await client.registerUser({
    localUserId: 950002 + (ts % 1000),
    funlifeUsername: `draw_b_${ts}`,
    displayName: "猜词B",
    password: randomPassword(),
  });

  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr.id);

  const word = "苹果";
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
      game_state: drawGuessState(userA.recordId, userB.recordId, word),
    },
  });
  const roomId = room.id;
  t.expect("开局房间 playing + drawing", room.status === "playing" && room.game_state?.draw_guess?.phase === "drawing");

  // ── 画家落笔 ──
  console.log("\n▶ 画家 stroke 同步");
  const strokePoints = [
    [0.2, 0.3],
    [0.25, 0.35],
    [0.3, 0.4],
  ];
  const t0 = performance.now();
  const move1 = await createMove(client, userA.token, roomId, userA.recordId, 1, {
    kind: "draw_stroke",
    seq: 1,
    round: 1,
    points: strokePoints,
    color: "#222222",
    width: 4,
  });
  const strokeMs = performance.now() - t0;
  console.log(`  POST draw_stroke: ${strokeMs.toFixed(0)}ms`);
  t.expect("create draw_stroke #1", move1.id && move1.move_index === 1);

  await sleep(300);
  const roomAfterStroke = await getRoom(client, userB.token, roomId);
  const seqFromHook = roomAfterStroke.game_state?.draw_guess?.stroke_seq;
  t.expect("hook 同步 stroke_seq >= 1", seqFromHook >= 1);

  const movesB1 = await listMoves(client, userB.token, roomId);
  t.expect("猜词方可见 game_moves", movesB1.length === 1 && movesB1[0].payload?.kind === "draw_stroke");
  const replay1 = parseStrokesForRound(movesB1, 1);
  t.expect("轮次 1 回放 1 笔", replay1.length === 1 && replay1[0].seq === 1);

  // ── 画好了 → 猜词 ──
  console.log("\n▶ 阶段切换 drawing → guessing");
  await createMove(client, userA.token, roomId, userA.recordId, 2, {
    kind: "draw_phase",
    phase: "guessing",
    round: 1,
  });
  const stateGuessing = drawGuessState(userA.recordId, userB.recordId, word);
  stateGuessing.draw_guess.phase = "guessing";
  stateGuessing.draw_guess.guesses = [];
  stateGuessing.draw_guess.stroke_seq = 1;
  stateGuessing.draw_guess.phase_started_at_ms = Date.now();

  const patchedGuess = await patchRoom(client, userA.token, roomId, {
    game_state: stateGuessing,
    status: "playing",
    current_turn: userB.recordId,
  });
  t.expect("PATCH 后轮到猜词方", relationId(patchedGuess.current_turn) === userB.recordId);

  await sleep(300);
  const roomGuessPhase = await getRoom(client, userB.token, roomId);
  const hookPhase = roomGuessPhase.game_state?.draw_guess?.phase;
  t.expect("hook/房间 phase=guessing", hookPhase === "guessing");

  // ── 词语脱敏（猜词方不可见）──
  console.log("\n▶ 词语脱敏");
  const roomGuesserView = await getRoom(client, userB.token, roomId);
  t.expect("猜词方 GET 看不到 word", !roomGuesserView.game_state?.draw_guess?.word);
  const roomDrawerView = await getRoom(client, userA.token, roomId);
  t.expect("画家 GET 可见 word", roomDrawerView.game_state?.draw_guess?.word === word);

  // ── 猜词（正确，hook 权威计分）──
  console.log("\n▶ 猜词正确");
  await createMove(client, userB.token, roomId, userB.recordId, 3, {
    kind: "draw_guess",
    text: "苹果",
    round: 1,
  });
  await sleep(300);
  const roomRoundEnd = await getRoom(client, userA.token, roomId);
  t.expect("hook 猜对后 round_end", roomRoundEnd.game_state?.draw_guess?.phase === "round_end");
  t.expect("hook 猜词方 +1 分", roomRoundEnd.game_state?.draw_guess?.scores?.[userB.recordId] === 1);
  await createMove(client, userB.token, roomId, userB.recordId, 99, {
    kind: "draw_guess",
    text: "苹果",
    round: 1,
  });
  await sleep(300);
  const afterDup = await getRoom(client, userA.token, roomId);
  const dupGuesses = (afterDup.game_state?.draw_guess?.guesses || []).filter(
    (g) => g.pb_id === userB.recordId,
  );
  t.expect("猜词去重（重复提交）", dupGuesses.length === 1);

  // ── 下一轮：清屏 + 新轮 stroke 隔离 ──
  console.log("\n▶ 下一轮清屏 + 轮次隔离");
  await createMove(client, userA.token, roomId, userA.recordId, 4, {
    kind: "draw_clear",
    round: 2,
  });
  await createMove(client, userA.token, roomId, userA.recordId, 5, {
    kind: "draw_phase",
    phase: "drawing",
    round: 2,
  });

  const round2State = drawGuessState(userA.recordId, userB.recordId, "香蕉");
  round2State.draw_guess.round = 2;
  round2State.draw_guess.drawer_pb_id = userB.recordId;
  round2State.draw_guess.word = "香蕉";
  round2State.draw_guess.guesses = [];
  round2State.draw_guess.scores = { [userA.recordId]: 0, [userB.recordId]: 1 };
  round2State.draw_guess.stroke_seq = 0;
  round2State.draw_guess.phase_started_at_ms = Date.now();

  await patchRoom(client, userA.token, roomId, {
    game_state: round2State,
    status: "playing",
    current_turn: userB.recordId,
  });

  await createMove(client, userB.token, roomId, userB.recordId, 6, {
    kind: "draw_stroke",
    seq: 1,
    round: 2,
    points: [
      [0.5, 0.5],
      [0.55, 0.55],
    ],
    color: "#222222",
    width: 4,
  });

  const allMoves = await listMoves(client, userA.token, roomId);
  const round1Strokes = parseStrokesForRound(allMoves, 1);
  const round2Strokes = parseStrokesForRound(allMoves, 2);
  t.expect("轮次 1 回放仍 1 笔", round1Strokes.length === 1);
  t.expect("轮次 2 回放仅新笔（清屏后）", round2Strokes.length === 1 && round2Strokes[0].seq === 1);

  // ── 增量拉取（模拟 GamePlaySyncManager watermark）──
  console.log("\n▶ 增量 move 拉取");
  const filterSince = encodeURIComponent(`room = '${roomId}' && move_index > 4`);
  const { json: deltaJson } = await client.request(
    "GET",
    `/collections/game_moves/records?filter=${filterSince}&sort=move_index&perPage=50`,
    { token: userB.token },
  );
  const delta = deltaJson.items || [];
  t.expect("move_index>4 增量 >= 2", delta.length >= 2);

  console.log("\n---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
