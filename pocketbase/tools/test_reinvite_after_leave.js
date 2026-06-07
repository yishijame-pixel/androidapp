#!/usr/bin/env node
/** 复现：接受 → 离座 → 再邀 → 再接受 */
"use strict";

const { PbSocialClient, randomPassword } = require("./social_test_lib");

const BASE = process.argv[2] || process.env.POCKETBASE_URL || "https://pb.yishi.site";

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

function withMemberLeft(state, pbId) {
  const members = state.members.filter((m) => m.pb_id !== pbId);
  return {
    ...state,
    members,
    member_ids: members.filter((m) => m.status === "joined").map((m) => m.pb_id),
    pending_invite_pb_id: null,
  };
}

function resolveStatus(s) {
  return s.members.filter((m) => m.status === "joined").length >= 2 ? "accepted" : "waiting";
}

function listFilter(myPbId) {
  return encodeURIComponent(
    `status = 'waiting' && host != '${myPbId}' && (guest = '${myPbId}' || game_state ~ '${myPbId}')`,
  );
}

async function main() {
  const c = new PbSocialClient(BASE);
  console.log("Base:", BASE);
  if (!(await c.health())) {
    console.error("PB unhealthy");
    process.exit(1);
  }

  const pwd = randomPassword();
  const ts = Date.now();
  const a = await c.registerUser({ localUserId: ts, funlifeUsername: `reinv_a_${ts}`, displayName: "Host A", password: pwd });
  const b = await c.registerUser({ localUserId: ts + 1, funlifeUsername: `reinv_b_${ts}`, displayName: "Guest B", password: pwd });
  await c.request("POST", "/collections/friendships/records", {
    token: a.token,
    body: { requester: a.recordId, addressee: b.recordId, status: "pending" },
  });
  const fr = await c.request(
    "GET",
    `/collections/friendships/records?filter=${encodeURIComponent(`addressee='${b.recordId}'`)}`,
    { token: b.token },
  );
  await c.request("PATCH", `/collections/friendships/records/${fr.json.items[0].id}`, {
    token: b.token,
    body: { status: "accepted" },
  });

  const hostId = a.recordId;
  const guestId = b.recordId;
  let state = {
    max_players: 2,
    min_players: 2,
    members: [{ pb_id: hostId, seat: 0, status: "joined" }],
    member_ids: [hostId],
  };

  const { json: room } = await c.request("POST", "/collections/game_rooms/records", {
    token: a.token,
    body: {
      host: hostId,
      game_type: "gomoku",
      invite_mode: "direct",
      status: "waiting",
      room_code: `RI${String(ts).slice(-4)}`,
      game_state: toMap(state),
    },
  });
  console.log("room", room.id);

  state = withPendingInvite(state, guestId);
  await c.request("PATCH", `/collections/game_rooms/records/${room.id}`, {
    token: a.token,
    body: {
      guest: guestId,
      status: "waiting",
      game_state: toMap(state),
      invite_message: String(Date.now()),
    },
  });

  state = withAcceptedInvite(state, guestId);
  await c.request("PATCH", `/collections/game_rooms/records/${room.id}`, {
    token: b.token,
    body: {
      guest: guestId,
      guest_ready: true,
      status: resolveStatus(state),
      game_state: toMap(state),
    },
  });
  console.log("1st accept status", resolveStatus(state));

  state = withMemberLeft(state, guestId);
  const leaveStatus = resolveStatus(state);
  try {
    await c.request("PATCH", `/collections/game_rooms/records/${room.id}`, {
      token: b.token,
      body: {
        guest: "",
        guest_ready: false,
        status: leaveStatus,
        game_state: toMap(state),
      },
    });
    console.log("leave OK status", leaveStatus);
  } catch (e) {
    console.log("leave FAIL", e.message);
  }

  const afterLeave = await c.request("GET", `/collections/game_rooms/records/${room.id}`, {
    token: a.token,
  });
  console.log(
    "server after leave:",
    afterLeave.json.status,
    "guest=",
    JSON.stringify(afterLeave.json.guest),
    "members=",
    JSON.stringify(afterLeave.json.game_state?.members),
  );

  state = withPendingInvite(state, guestId);
  await c.request("PATCH", `/collections/game_rooms/records/${room.id}`, {
    token: a.token,
    body: {
      guest: guestId,
      status: "waiting",
      game_state: toMap(state),
      invite_message: String(Date.now()),
    },
  });

  const list = await c.request(
    "GET",
    `/collections/game_rooms/records?filter=${listFilter(guestId)}`,
    { token: b.token },
  );
  console.log("guest list", list.json.items?.length, list.json.items?.map((i) => i.id));

  try {
    const g = await c.request("GET", `/collections/game_rooms/records/${room.id}`, { token: b.token });
    console.log("guest GET", g.json.status, "guest=", g.json.guest);
  } catch (e) {
    console.log("guest GET 404", e.message);
  }

  state = withAcceptedInvite(state, guestId);
  try {
    await c.request("PATCH", `/collections/game_rooms/records/${room.id}`, {
      token: b.token,
      body: {
        guest: guestId,
        guest_ready: true,
        status: resolveStatus(state),
        game_state: toMap(state),
      },
    });
    console.log("2nd accept OK");
  } catch (e) {
    console.log("2nd accept FAIL", e.message);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
