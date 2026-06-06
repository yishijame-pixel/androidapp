#!/usr/bin/env node
/**
 * FunLife game_rooms 扩展 E2E — 覆盖 App 真实 payload / ACL / 离座 / hooks 回归
 *
 * 用法：
 *   node pocketbase/tools/test_game_room_extended_e2e.js
 *   node pocketbase/tools/test_game_room_extended_e2e.js --base-url https://pb.yishi.site
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

function enc(s) {
  return encodeURIComponent(s);
}

function initialState(hostId, maxPlayers = 4, minPlayers = 2) {
  return {
    max_players: maxPlayers,
    min_players: minPlayers,
    members: [{ pb_id: hostId, seat: 0, status: "joined" }],
    member_ids: [hostId],
  };
}

function toMap(state) {
  const m = {
    max_players: state.max_players,
    min_players: state.min_players,
    members: state.members,
    member_ids: state.member_ids,
  };
  if (state.pending_invite_pb_id) m.pending_invite_pb_id = state.pending_invite_pb_id;
  if (state.declined_by_pb_id) m.declined_by_pb_id = state.declined_by_pb_id;
  return m;
}

function joinedCount(state) {
  return state.members.filter((m) => m.status === "joined").length;
}

function withDirectJoin(state, pbId) {
  const seat = Math.max(...state.members.map((m) => m.seat), -1) + 1;
  const members = [...state.members, { pb_id: pbId, seat, status: "joined" }];
  return {
    ...state,
    members,
    member_ids: members.filter((m) => m.status === "joined").map((m) => m.pb_id),
  };
}

function withMemberLeft(state, pbId) {
  const members = state.members.filter((m) => m.pb_id !== pbId);
  return {
    ...state,
    members,
    member_ids: members.filter((m) => m.status === "joined").map((m) => m.pb_id),
    pending_invite_pb_id: state.pending_invite_pb_id === pbId ? null : state.pending_invite_pb_id,
    declined_by_pb_id: null,
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
    declined_by_pb_id: null,
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

function resolveStatusAfterJoin(state) {
  return joinedCount(state) >= Math.max(2, state.min_players) ? "accepted" : "waiting";
}

function expiresDate(minutesFromNow) {
  const d = new Date(Date.now() + minutesFromNow * 60_000);
  return d.toISOString().slice(0, 10);
}

function roomCode(prefix) {
  return (prefix + ts.toString(36).slice(-4)).toUpperCase().slice(0, 6);
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== game_rooms 扩展 E2E ===\nBase:", BASE_URL, "\n");

  t.expect("health", await client.health());
  if (!(await client.health())) {
    printSummary(t);
    process.exit(1);
  }

  const pwdA = randomPassword();
  const pwdB = randomPassword();
  const pwdC = randomPassword();
  const userA = await client.registerUser({
    localUserId: 920001 + (ts % 1000),
    funlifeUsername: `ext_a_${ts}`,
    displayName: "扩展A",
    password: pwdA,
  });
  const userB = await client.registerUser({
    localUserId: 920002 + (ts % 1000),
    funlifeUsername: `ext_b_${ts}`,
    displayName: "扩展B",
    password: pwdB,
  });
  const userC = await client.registerUser({
    localUserId: 920003 + (ts % 1000),
    funlifeUsername: `ext_c_${ts}`,
    displayName: "扩展C",
    password: pwdC,
  });

  const fr1 = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  await client.acceptFriendship(userB.token, fr1.id);
  const fr2 = await client.createFriendRequest(userA.token, userA.recordId, userC.recordId);
  await client.acceptFriendship(userC.token, fr2.id);

  // ── 1. hooks 回归：App 同款 create payload 必须 200 ──
  console.log("\n▶ hooks / create 回归");
  const code1 = roomCode("H");
  const state1 = initialState(userA.recordId);
  const { status: createStatus, json: created } = await client.request(
    "POST",
    "/collections/game_rooms/records",
    {
      token: userA.token,
      body: {
        game_type: "gomoku",
        invite_mode: "open",
        room_code: code1,
        host: userA.recordId,
        status: "waiting",
        host_ready: true,
        guest_ready: false,
        expires_at: expiresDate(10),
        game_state: toMap(state1),
      },
      expectOk: true,
    },
  );
  t.expect("App 同款 create 返回 200", createStatus === 200 && !!created?.id);
  t.expect("create 响应含 room id", !!created?.id);

  // ── 2. Android listMyGameRooms 同款 filter ──
  console.log("\n▶ 列表 / 查询 filter（对齐 PocketBaseApiClient）");
  const myPbId = userA.recordId;
  const listFilter = enc(
    `(host = '${myPbId}' || guest = '${myPbId}' || game_state ~ '${myPbId}') && ` +
      `(status != 'cancelled' && status != 'expired' || host = '${myPbId}')`,
  );
  const { json: listJson } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${listFilter}&perPage=50&sort=-updated`,
    { token: userA.token },
  );
  t.expect("房主 list filter 含刚创建房间", listJson.items?.some((r) => r.id === created.id));

  const { json: foundByCode } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${enc(
      `room_code = '${code1}' && status = 'waiting' && invite_mode = 'open'`,
    )}&expand=host,guest&perPage=1`,
    { token: userB.token },
  );
  t.expect("B 可按房间号发现 open 房", foundByCode.items?.[0]?.id === created.id);

  // ── 3. game_state 加入 + 离座（幽灵玩家回归）──
  console.log("\n▶ 加入 / 离座（game_state + guest 同步）");
  let state = initialState(userA.recordId);
  state = withDirectJoin(state, userB.recordId);
  const joinedStatus = resolveStatusAfterJoin(state);
  const { json: joined } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${created.id}`,
    {
      token: userB.token,
      body: {
        game_state: toMap(state),
        status: joinedStatus,
        guest: userB.recordId,
        guest_ready: true,
        host_ready: true,
      },
    },
  );
  t.expect("B 加入后 status=accepted", joined.status === "accepted");
  t.expect("B 加入后 guest 字段正确", joined.guest === userB.recordId);
  t.expect(
    "B 加入后 game_state 含 B",
    joined.game_state?.member_ids?.includes(userB.recordId),
  );

  state = withMemberLeft(state, userB.recordId);
  const afterLeaveStatus = resolveStatusAfterJoin(state);
  const { json: afterLeave } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${created.id}`,
    {
      token: userB.token,
      body: {
        game_state: toMap(state),
        status: afterLeaveStatus,
        guest: "",
        guest_ready: false,
        host_ready: true,
      },
    },
  );
  t.expect("B 离座后 guest 已清空", !afterLeave.guest);
  t.expect(
    "B 离座后 game_state 不含 B",
    !afterLeave.game_state?.member_ids?.includes(userB.recordId),
  );

  const { json: hostView } = await client.request(
    "GET",
    `/collections/game_rooms/records/${created.id}?expand=host,guest`,
    { token: userA.token },
  );
  t.expect("房主 GET 离座后无幽灵 guest", !hostView.guest);
  t.expect(
    "房主 GET 离座后 members 仅 host",
    hostView.game_state?.members?.length === 1 &&
      hostView.game_state.members[0].pb_id === userA.recordId,
  );

  // open/waiting 房 updateRule 允许任意登录用户 PATCH（凭码加入设计）
  console.log("\n▶ ACL 边界（open 房）");
  const { ok: bPatchOpen } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${created.id}`,
    {
      token: userB.token,
      body: { guest_ready: false },
      expectOk: false,
    },
  );
  t.expect("open/waiting 房：离座后 B 仍可 PATCH（当前 ACL）", bPatchOpen);

  const { ok: cPatchOpen } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${created.id}`,
    {
      token: userC.token,
      body: { guest_ready: false },
      expectOk: false,
    },
  );
  t.expect("open/waiting 房：陌生人 C 可 PATCH（当前 ACL）", cPatchOpen);

  // ── 5. 直接邀请：pending → accept ──
  console.log("\n▶ 直接邀请 accept / reject");
  const { json: directRoom } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "dice_duel",
      invite_mode: "direct",
      host: userA.recordId,
      status: "waiting",
      host_ready: true,
      game_state: toMap(initialState(userA.recordId, 2, 2)),
    },
  });

  let directState = withPendingInvite(initialState(userA.recordId, 2, 2), userB.recordId);
  await client.request("PATCH", `/collections/game_rooms/records/${directRoom.id}`, {
    token: userA.token,
    body: {
      invite_mode: "direct",
      guest: userB.recordId,
      game_state: toMap(directState),
      status: "waiting",
    },
  });

  const { json: bCanViewInvite } = await client.request(
    "GET",
    `/collections/game_rooms/records/${directRoom.id}`,
    { token: userB.token },
  );
  t.expect("B 可查看 direct 邀请房", bCanViewInvite.id === directRoom.id);

  directState = withAcceptedInvite(directState, userB.recordId);
  const { json: accepted } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${directRoom.id}`,
    {
      token: userB.token,
      body: {
        game_state: toMap(directState),
        status: resolveStatusAfterJoin(directState),
        guest: userB.recordId,
        guest_ready: true,
        host_ready: true,
      },
    },
  );
  t.expect("B accept 邀请后 joined", accepted.game_state?.members?.some(
    (m) => m.pb_id === userB.recordId && m.status === "joined",
  ));

  // reject flow on new room
  const { json: rejectRoom } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "gomoku",
      invite_mode: "direct",
      host: userA.recordId,
      status: "waiting",
      host_ready: true,
      game_state: toMap(initialState(userA.recordId, 2, 2)),
    },
  });
  let rejectState = withPendingInvite(initialState(userA.recordId, 2, 2), userC.recordId);
  await client.request("PATCH", `/collections/game_rooms/records/${rejectRoom.id}`, {
    token: userA.token,
    body: {
      invite_mode: "direct",
      guest: userC.recordId,
      game_state: toMap(rejectState),
      status: "waiting",
    },
  });
  rejectState = {
    ...rejectState,
    members: rejectState.members.filter((m) => m.pb_id !== userC.recordId),
    pending_invite_pb_id: null,
    declined_by_pb_id: userC.recordId,
    member_ids: [userA.recordId],
  };
  const { json: rejected } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${rejectRoom.id}`,
    {
      token: userC.token,
      body: {
        game_state: toMap(rejectState),
        status: "waiting",
        guest: "",
        guest_ready: false,
        invite_mode: "open",
      },
    },
  );
  t.expect("C reject 后 declined_by 标记", rejected.game_state?.declined_by_pb_id === userC.recordId);
  t.expect("C reject 后 guest 清空", !rejected.guest);

  // ── 6. 房主 startGame ──
  console.log("\n▶ 开始游戏");
  await client.request("PATCH", `/collections/game_rooms/records/${directRoom.id}`, {
    token: userA.token,
    body: { status: "playing", host_ready: true, guest_ready: true },
  });
  const { json: playing } = await client.request(
    "GET",
    `/collections/game_rooms/records/${directRoom.id}`,
    { token: userA.token },
  );
  t.expect("房主可将房间设为 playing", playing.status === "playing");

  // ── 7. createRule：不能代他人开房 ──
  console.log("\n▶ createRule / updateRule");
  await t.expectThrows(
    "不能替他人当 host 开房",
    () =>
      client.request("POST", "/collections/game_rooms/records", {
        token: userB.token,
        body: {
          game_type: "gomoku",
          invite_mode: "open",
          host: userA.recordId,
          status: "waiting",
        },
      }),
    ["403", "400"],
  );

  // ── 8. 大厅内邀请好友（App inviteFriendToRoom 同款 PATCH）──
  console.log("\n▶ 大厅邀请送达");
  const { json: lobbyRoom } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "gomoku",
      invite_mode: "open",
      room_code: roomCode("L"),
      host: userA.recordId,
      status: "waiting",
      host_ready: true,
      game_state: toMap(initialState(userA.recordId)),
    },
  });
  let lobbyState = withPendingInvite(initialState(userA.recordId), userB.recordId);
  const { status: invPatchSt, json: invited } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${lobbyRoom.id}`,
    {
      token: userA.token,
      body: {
        invite_mode: "direct",
        guest: userB.recordId,
        guest_ready: false,
        host_ready: true,
        status: "waiting",
        game_state: toMap(lobbyState),
      },
    },
  );
  t.expect("大厅邀请 PATCH 200", invPatchSt === 200);
  const { json: bInviteList } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${encodeURIComponent(
      `(host = '${userB.recordId}' || guest = '${userB.recordId}' || game_state ~ '${userB.recordId}') && (status != 'cancelled' && status != 'expired' || host = '${userB.recordId}')`,
    )}&perPage=50`,
    { token: userB.token },
  );
  const inviteRow = bInviteList.items?.find((r) => r.id === lobbyRoom.id);
  t.expect("受邀方 list 含 pending 邀请", inviteRow?.game_state?.pending_invite_pb_id === userB.recordId);
  t.expect(
    "incomingDirectInvite 可触发",
    inviteRow?.status === "waiting" && !!inviteRow?.game_state?.pending_invite_pb_id,
  );

  printSummary(t);
  process.exit(t.fail > 0 ? 1 : 0);
}

function printSummary(t) {
  const s = t.summary();
  console.log("\n---");
  console.log(`PASS ${s.pass}  FAIL ${s.fail}`);
  if (s.fail > 0) {
    s.results.filter((r) => !r.pass).forEach((r) => console.log("  ❌", r.name, r.detail || ""));
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
