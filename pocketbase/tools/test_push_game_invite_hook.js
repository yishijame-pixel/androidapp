#!/usr/bin/env node
/**
 * 推送 hook 冒烟：给受邀方写入 fcm_token，触发 game_invite 全路径。
 * REST 仍返回 200；请在 PocketBase 控制台确认无 hook error，应见：
 *   [push] game_invite status=...  或  skip: no_relay
 *
 * 用法：
 *   node pocketbase/tools/test_push_game_invite_hook.js
 *   node pocketbase/tools/test_push_game_invite_hook.js --base-url https://pb.yishi.site
 */
"use strict";

const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const ADMIN_EMAIL = arg("--admin-email", "admin@funlife.local");
const ADMIN_PASSWORD = arg("--admin-password", "FunLifePB2026!");
const ts = Date.now();

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

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();

  console.log("\n=== 游戏邀请 Push Hook 冒烟 ===\nBase:", BASE_URL, "\n");
  t.expect("health", await client.health());
  if (!(await client.health())) process.exit(1);

  const host = await client.registerUser({
    localUserId: 940001 + (ts % 1000),
    funlifeUsername: `push_host_${ts}`,
    displayName: "推送房主",
    password: randomPassword(),
  });
  const guest = await client.registerUser({
    localUserId: 940002 + (ts % 1000),
    funlifeUsername: `push_guest_${ts}`,
    displayName: "推送受邀",
    password: randomPassword(),
  });

  const adminToken = await client.adminAuth(ADMIN_EMAIL, ADMIN_PASSWORD);
  const fcmToken = `e2e_hook_smoke_${ts}`;
  const { status: patchUserStatus } = await client.request(
    "PATCH",
    `/collections/users/records/${guest.recordId}`,
    {
      token: adminToken,
      body: { fcm_token: fcmToken },
    },
  );
  t.expect("admin 写入 guest fcm_token", patchUserStatus === 200);

  const fr = await client.createFriendRequest(host.token, host.recordId, guest.recordId);
  await client.acceptFriendship(guest.token, fr.id);

  const code = ("P" + ts.toString(36).slice(-5)).toUpperCase().slice(0, 6);
  const lobby = initialLobby(host.recordId);
  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: host.token,
    body: {
      game_type: "draw_guess",
      invite_mode: "open",
      room_code: code,
      host: host.recordId,
      status: "waiting",
      host_ready: true,
      game_state: lobby,
    },
  });
  t.expect("创建房间", !!room?.id);

  const inviteState = withPendingInvite(lobby, guest.recordId);
  const { status: inviteStatus } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${room.id}`,
    {
      token: host.token,
      body: {
        invite_mode: "direct",
        guest: guest.recordId,
        guest_ready: false,
        host_ready: true,
        status: "waiting",
        game_state: inviteState,
      },
    },
  );
  t.expect("PATCH 邀请触发 update hook", inviteStatus === 200);

  // 再 PATCH 一次（模拟重新邀请）触发 onRecordAfterUpdateSuccess
  const { status: reinviteStatus } = await client.request(
    "PATCH",
    `/collections/game_rooms/records/${room.id}`,
    {
      token: host.token,
      body: {
        invite_message: `reinvite_${ts}`,
        game_state: inviteState,
      },
    },
  );
  t.expect("重复 PATCH 仍 200", reinviteStatus === 200);

  console.log("\n请在 PocketBase 控制台确认：");
  console.log("  - 无 game_invite hook error");
  console.log("  - 有 [push] game_invite status= 或 skip: no_relay\n");

  console.log("---");
  console.log(`PASS ${t.pass}  FAIL ${t.fail}`);
  process.exit(t.fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
