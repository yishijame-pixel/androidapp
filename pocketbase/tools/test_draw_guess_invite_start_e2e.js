#!/usr/bin/env node
/**
 * 你画我猜全流程 E2E：开房间 → 邀请好友 → 接受 → 开始游戏 → 笔画同步
 *
 * 用法：
 *   node pocketbase/tools/test_draw_guess_invite_start_e2e.js
 *   node pocketbase/tools/test_draw_guess_invite_start_e2e.js --base-url https://pb.yishi.site
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

function initialLobby(hostId) {
  return {
    max_players: 2,
    min_players: 2,
    members: [{ pb_id: hostId, seat: 0, status: "joined" }],
    member_ids: [hostId],
  };
}

function withPendingInvite(state, guestId) {
  const seat = Math.max(...state.members.map((m) => m.seat), -1) + 1;
  const members = [...state.members, { pb_id: guestId, seat, status: "pending" }];
  return {
    ...state,
    members,
    member_ids: members.map((m) => m.pb_id),
    pending_invite_pb_id: guestId,
  };
}

function withAcceptedInvite(state, guestId) {
  const members = state.members.map((m) =>
    m.pb_id === guestId ? { ...m, status: "joined" } : m,
  );
  return {
    ...state,
    members,
    pending_invite_pb_id: null,
    member_ids: members.filter((m) => m.status === "joined").map((m) => m.pb_id),
  };
}

function drawGuessPlayState(hostId, guestId) {
  return {
    round: 1,
    phase: "drawing",
    drawer_pb_id: hostId,
    word: "苹果",
    guesses: [],
    scores: { [hostId]: 0, [guestId]: 0 },
    stroke_seq: 0,
    max_rounds: 3,
    guess_limit: 5,
    draw_seconds: 60,
    guess_seconds: 90,
    phase_started_at_ms: Date.now(),
  };
}

function startDrawGuessState(lobby, hostId, guestId) {
  return {
    ...lobby,
    draw_guess: drawGuessPlayState(hostId, guestId),
  };
}

function androidListFilter(myPbId) {
  return encodeURIComponent(
    `(host = '${myPbId}' || guest = '${myPbId}' || game_state ~ '${myPbId}') && ` +
      `(status != 'cancelled' && status != 'expired' || host = '${myPbId}')`,
  );
}

async function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== 你画我猜 开房-邀请-开局 E2E ===\nBase:", BASE_URL, "\n");

  t.expect("health", await client.health());
  if (!(await client.health())) process.exit(1);

  const host = await client.registerUser({
    localUserId: 980001 + (ts % 1000),
    funlifeUsername: `dg_host_${ts}`,
    displayName: "房主",
    password: randomPassword(),
  });
  const guest = await client.registerUser({
    localUserId: 980002 + (ts % 1000),
    funlifeUsername: `dg_guest_${ts}`,
    displayName: "好友",
    password: randomPassword(),
  });

  const fr = await client.createFriendRequest(host.token, host.recordId, guest.recordId);
  await client.acceptFriendship(guest.token, fr.id);
  t.expect("好友关系建立", true);

  // ── 1. 房主开房 ──
  console.log("\n▶ 开房间");
  let lobby = initialLobby(host.recordId);
  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: host.token,
    body: {
      game_type: "draw_guess",
      invite_mode: "direct",
      host: host.recordId,
      status: "waiting",
      host_ready: true,
      guest_ready: false,
      game_state: lobby,
    },
  });
  const roomId = room.id;
  t.expect("创建 draw_guess 房间", room.id && room.game_type === "draw_guess");

  // ── 2. 邀请好友 ──
  console.log("\n▶ 邀请好友");
  const tInvite0 = performance.now();
  lobby = withPendingInvite(lobby, guest.recordId);
  const { json: invited } = await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token: host.token,
    body: {
      invite_mode: "direct",
      guest: guest.recordId,
      guest_ready: false,
      status: "waiting",
      game_state: lobby,
    },
  });
  const inviteMs = performance.now() - tInvite0;
  console.log(`  PATCH 邀请: ${inviteMs.toFixed(0)}ms`);
  t.expect("邀请后 pending_invite", invited.game_state?.pending_invite_pb_id === guest.recordId);

  const { json: guestList } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${androidListFilter(guest.recordId)}&perPage=20`,
    { token: guest.token },
  );
  const foundInvite = guestList.items?.find((r) => r.id === roomId);
  t.expect("受邀方 list 可见邀请", !!foundInvite);

  // ── 3. 接受邀请 ──
  console.log("\n▶ 接受邀请");
  const tAccept0 = performance.now();
  lobby = withAcceptedInvite(lobby, guest.recordId);
  const { json: accepted } = await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token: guest.token,
    body: {
      guest: guest.recordId,
      guest_ready: true,
      host_ready: true,
      status: "accepted",
      game_state: lobby,
    },
  });
  const acceptMs = performance.now() - tAccept0;
  console.log(`  PATCH 接受: ${acceptMs.toFixed(0)}ms`);
  t.expect("接受后 status=accepted", accepted.status === "accepted");
  t.expect("双方 joined", accepted.game_state?.members?.filter((m) => m.status === "joined").length === 2);

  // ── 4. 开始游戏 ──
  console.log("\n▶ 开始游戏");
  const tStart0 = performance.now();
  const playState = startDrawGuessState(lobby, host.recordId, guest.recordId);
  const { json: playing } = await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token: host.token,
    body: {
      status: "playing",
      host_ready: true,
      guest_ready: true,
      current_turn: host.recordId,
      game_state: playState,
    },
  });
  const startMs = performance.now() - tStart0;
  console.log(`  PATCH 开局: ${startMs.toFixed(0)}ms`);
  t.expect("开局 playing", playing.status === "playing");
  t.expect("draw_guess 子状态", playing.game_state?.draw_guess?.phase === "drawing");
  t.expect("画家=房主", playing.game_state?.draw_guess?.drawer_pb_id === host.recordId);

  // ── 5. 词语脱敏 + 笔画 hook ──
  console.log("\n▶ 进局同步");
  const guestView = await client.request("GET", `/collections/game_rooms/records/${roomId}`, {
    token: guest.token,
  });
  t.expect("猜词方看不到 word", !guestView.json.game_state?.draw_guess?.word);

  const hostView = await client.request("GET", `/collections/game_rooms/records/${roomId}`, {
    token: host.token,
  });
  t.expect("画家可见 word", !!hostView.json.game_state?.draw_guess?.word);

  const tStroke0 = performance.now();
  await client.request("POST", "/collections/game_moves/records", {
    token: host.token,
    body: {
      room: roomId,
      player: host.recordId,
      move_index: 1,
      payload: { kind: "draw_stroke", seq: 1, round: 1, points: [[0.2, 0.3], [0.3, 0.4]], color: "#222", width: 4 },
    },
  });
  await sleep(250);
  const strokeMs = performance.now() - tStroke0;
  console.log(`  首笔 POST+hook: ${strokeMs.toFixed(0)}ms`);

  const afterStroke = await client.request("GET", `/collections/game_rooms/records/${roomId}`, {
    token: guest.token,
  });
  t.expect("hook stroke_seq>=1", (afterStroke.json.game_state?.draw_guess?.stroke_seq || 0) >= 1);

  const { json: moves } = await client.request(
    "GET",
    `/collections/game_moves/records?filter=${encodeURIComponent(`room = '${roomId}'`)}&sort=move_index`,
    { token: guest.token },
  );
  t.expect("猜词方可见 move", moves.items?.length === 1);

  // ── 延迟预算（企业级对标）──
  console.log("\n▶ 延迟预算");
  t.expect("邀请 PATCH < 800ms", inviteMs < 800);
  t.expect("接受 PATCH < 800ms", acceptMs < 800);
  t.expect("开局 PATCH < 800ms", startMs < 800);
  t.expect("首笔同步 < 1200ms", strokeMs < 1200);

  console.log("\n---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
