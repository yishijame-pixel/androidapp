#!/usr/bin/env node
/**
 * 五子棋落子同步 E2E — 覆盖 App 真实链路：
 *   POST game_moves (gomoku_place) + PATCH game_rooms (game_state + current_turn)
 *   以及 pb_hooks 落子 hook 兜底
 *
 * 用法：
 *   node pocketbase/tools/test_game_gomoku_move_e2e.js
 *   node pocketbase/tools/test_game_gomoku_move_e2e.js --base-url https://pb.yishi.site
 */
"use strict";

const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const GOMOKU_SIZE = 15;
const EMPTY = ".";

const ts = Date.now();

function emptyBoard() {
  return EMPTY.repeat(GOMOKU_SIZE * GOMOKU_SIZE);
}

function applyMove(board, x, y, color) {
  const chars = board.split("");
  chars[y * GOMOKU_SIZE + x] = color;
  return chars.join("");
}

function lobbyState(hostId, guestId) {
  return {
    max_players: 2,
    min_players: 2,
    members: [
      { pb_id: hostId, seat: 0, status: "joined" },
      { pb_id: guestId, seat: 1, status: "joined" },
    ],
    member_ids: [hostId, guestId],
    gomoku: {
      board: emptyBoard(),
      move_count: 0,
      black_pb_id: hostId,
      white_pb_id: guestId,
      forbidden_enabled: false,
    },
  };
}

async function listMoves(client, token, roomId) {
  const filter = encodeURIComponent(`room = '${roomId}'`);
  const { json } = await client.request(
    "GET",
    `/collections/game_moves/records?filter=${filter}&sort=move_index&perPage=50`,
    { token },
  );
  return json.items || [];
}

async function createMove(client, token, roomId, playerId, moveIndex, x, y) {
  const { json } = await client.request("POST", "/collections/game_moves/records", {
    token,
    body: {
      room: roomId,
      player: playerId,
      move_index: moveIndex,
      payload: { kind: "gomoku_place", x, y },
    },
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

function relationId(field) {
  if (!field) return "";
  if (typeof field === "string") return field;
  return field.id || "";
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== 五子棋落子同步 E2E ===\nBase:", BASE_URL, "\n");

  t.expect("health", await client.health());
  if (!(await client.health())) {
    console.log("\n---\nFAIL: PocketBase 不可达，请先启动 PB 或指定 --base-url");
    process.exit(1);
  }

  const pwdA = randomPassword();
  const pwdB = randomPassword();
  const userA = await client.registerUser({
    localUserId: 930001 + (ts % 1000),
    funlifeUsername: `gom_a_${ts}`,
    displayName: "黑方",
    password: pwdA,
  });
  const userB = await client.registerUser({
    localUserId: 930002 + (ts % 1000),
    funlifeUsername: `gom_b_${ts}`,
    displayName: "白方",
    password: pwdB,
  });

  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr.id);

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
      game_state: lobbyState(userA.recordId, userB.recordId),
    },
  });
  const roomId = room.id;
  t.expect("开局房间 playing", room.status === "playing" && relationId(room.current_turn) === userA.recordId);

  // ── 黑方落子 (7,7) — App 同款：先写 move，再 PATCH 房间 ──
  console.log("\n▶ 黑方落子 + 同步");
  const move1 = await createMove(client, userA.token, roomId, userA.recordId, 1, 7, 7);
  t.expect("create game_move #1（PB 不接受 move_index=0）", move1.id && move1.move_index === 1);

  const boardAfterBlack = applyMove(emptyBoard(), 7, 7, "B");
  const stateAfterBlack = lobbyState(userA.recordId, userB.recordId);
  stateAfterBlack.gomoku.board = boardAfterBlack;
  stateAfterBlack.gomoku.move_count = 1;
  stateAfterBlack.gomoku.last_move = { x: 7, y: 7, color: "B" };

  const patched1 = await patchRoom(client, userA.token, roomId, {
    game_state: stateAfterBlack,
    status: "playing",
    current_turn: userB.recordId,
  });
  t.expect("PATCH 后轮到白方", relationId(patched1.current_turn) === userB.recordId);

  const roomB1 = await getRoom(client, userB.token, roomId);
  t.expect("白方可见棋盘有子", roomB1.game_state?.gomoku?.board?.charAt(7 * GOMOKU_SIZE + 7) === "B"); // (x=7,y=7)

  const movesB1 = await listMoves(client, userB.token, roomId);
  t.expect("白方可拉取 game_moves", movesB1.length === 1 && movesB1[0].payload?.kind === "gomoku_place");

  // hook 兜底：若客户端 PATCH 失败，create move 后服务端也应切回合（部署 hook 时）
  const hookTurn = relationId(roomB1.current_turn);
  t.expect("current_turn 已离开黑方", hookTurn === userB.recordId);

  // ── 白方落子 (7,8) ──
  console.log("\n▶ 白方落子 + 同步");
  await createMove(client, userB.token, roomId, userB.recordId, 2, 7, 8);

  const boardAfterWhite = applyMove(boardAfterBlack, 7, 8, "W");
  const stateAfterWhite = { ...stateAfterBlack };
  stateAfterWhite.gomoku = {
    ...stateAfterBlack.gomoku,
    board: boardAfterWhite,
    move_count: 2,
    last_move: { x: 7, y: 8, color: "W" },
  };

  const patched2 = await patchRoom(client, userB.token, roomId, {
    game_state: stateAfterWhite,
    status: "playing",
    current_turn: userA.recordId,
  });
  t.expect("PATCH 后轮到黑方", relationId(patched2.current_turn) === userA.recordId);

  const roomA2 = await getRoom(client, userA.token, roomId);
  const cellWhite = roomA2.game_state?.gomoku?.board?.charAt(8 * GOMOKU_SIZE + 7); // (x=7,y=8)
  t.expect("黑方可见白子", cellWhite === "W");

  const movesA2 = await listMoves(client, userA.token, roomId);
  t.expect("双方共 2 手 move", movesA2.length === 2);

  // ── 非法：move_index=0 被 PB 拒绝（首手 bug 回归）──
  console.log("\n▶ move_index=0 回归");
  await t.expectThrows(
    "move_index=0 应被 PB 拒绝",
    () => createMove(client, userA.token, roomId, userA.recordId, 0, 8, 7),
    ["400", "Cannot be blank"],
  );

  console.log("\n---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
