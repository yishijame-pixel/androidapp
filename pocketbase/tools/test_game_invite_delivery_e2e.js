#!/usr/bin/env node
/** 游戏邀请送达 E2E：房主 PATCH 邀请后，受邀方能否 list/get */
"use strict";

const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

const BASE = process.env.POCKETBASE_URL || "http://127.0.0.1:8090";
const ts = Date.now();

function initialState(hostId) {
  return {
    max_players: 4,
    min_players: 2,
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
  return m;
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

function androidListFilter(myPbId) {
  return encodeURIComponent(
    `(host = '${myPbId}' || guest = '${myPbId}' || game_state ~ '${myPbId}') && ` +
      `(status != 'cancelled' && status != 'expired' || host = '${myPbId}')`,
  );
}

async function main() {
  const client = new PbSocialClient(BASE);
  const t = new TestReporter();
  console.log("\n=== 游戏邀请送达 E2E ===\nBase:", BASE, "\n");

  t.expect("health", await client.health());
  if (!(await client.health())) process.exit(1);

  const a = await client.registerUser({
    localUserId: 930001,
    funlifeUsername: `inv_a_${ts}`,
    displayName: "房主",
    password: randomPassword(),
  });
  const b = await client.registerUser({
    localUserId: 930002,
    funlifeUsername: `inv_b_${ts}`,
    displayName: "受邀",
    password: randomPassword(),
  });

  const fr = await client.createFriendRequest(a.token, a.recordId, b.recordId);
  await client.acceptFriendship(b.token, fr.id);

  const code = ("I" + ts.toString(36).slice(-5)).toUpperCase().slice(0, 6);
  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: a.token,
    body: {
      game_type: "gomoku",
      invite_mode: "open",
      room_code: code,
      host: a.recordId,
      status: "waiting",
      host_ready: true,
      game_state: toMap(initialState(a.recordId)),
    },
  });

  const inviteState = withPendingInvite(initialState(a.recordId), b.recordId);
  const { status: patchStatus, json: patched } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${room.id}`,
    {
      token: a.token,
      body: {
        invite_mode: "direct",
        guest: b.recordId,
        guest_ready: false,
        host_ready: true,
        status: "waiting",
        game_state: toMap(inviteState),
        expires_at: new Date(Date.now() + 300_000).toISOString().slice(0, 10),
      },
    },
  );
  t.expect("房主 PATCH 邀请 200", patchStatus === 200);
  t.expect("PATCH 后 guest 字段", patched.guest === b.recordId);
  t.expect("PATCH 后 pending_invite", patched.game_state?.pending_invite_pb_id === b.recordId);

  const { json: guestList } = await client.request(
    "GET",
    `/collections/game_rooms/records?filter=${androidListFilter(b.recordId)}&perPage=50`,
    { token: b.token },
  );
  const found = guestList.items?.find((r) => r.id === room.id);
  t.expect("受邀方 list 可见邀请房", !!found);
  t.expect("受邀方 list pending_invite", found?.game_state?.pending_invite_pb_id === b.recordId);

  const { status: getStatus, json: guestGet } = await client.request(
    "GET",
    `/collections/game_rooms/records/${room.id}`,
    { token: b.token },
  );
  t.expect("受邀方 GET 200", getStatus === 200);
  t.expect("受邀方 GET guest=自己", guestGet.guest === b.recordId);

  // 模拟 incomingDirectInvite 判定
  const wouldShowOverlay =
    !!found &&
    found.status === "waiting" &&
    found.game_state?.pending_invite_pb_id === b.recordId &&
    (found.guest === b.recordId || found.game_state?.pending_invite_pb_id === b.recordId);
  t.expect("App incomingDirectInvite 条件满足", wouldShowOverlay);

  console.log("\n---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
