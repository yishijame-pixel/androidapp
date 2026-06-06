#!/usr/bin/env node
/**
 * FunLife PocketBase 趣玩中心 — game_rooms E2E
 *
 * 用法：
 *   node pocketbase/tools/test_game_room_e2e.js
 *   node pocketbase/tools/test_game_room_e2e.js --base-url https://pb.yishi.site
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

const USER_A = { localId: 910001 + (ts % 1000), username: `game_a_${ts}`, name: "趣玩A" };
const USER_B = { localId: 910002 + (ts % 1000), username: `game_b_${ts}`, name: "趣玩B" };

function enc(s) {
  return encodeURIComponent(s);
}

async function createRoom(token, hostId, body) {
  const client = new PbSocialClient(BASE_URL);
  const { json } = await client.request("POST", "/collections/game_rooms/records", {
    token,
    body: { host: hostId, ...body },
  });
  return json;
}

async function patchRoom(token, roomId, patch) {
  const client = new PbSocialClient(BASE_URL);
  const { json } = await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token,
    body: patch,
  });
  return json;
}

async function findByCode(token, code) {
  const client = new PbSocialClient(BASE_URL);
  const filter = enc(`room_code = '${code}' && status = 'waiting' && invite_mode = 'open'`);
  const { json } = await client.request("GET", `/collections/game_rooms/records?filter=${filter}&perPage=1`, {
    token,
  });
  return json.items?.[0] || null;
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();
  const cleanup = { rooms: [], friendships: [], users: [] };

  console.log("\n=== game_rooms E2E ===\nBase:", BASE_URL, "\n");

  t.expect("health", await client.health());
  if (!await client.health()) {
    printSummary(t);
    process.exit(1);
  }

  const pwdA = randomPassword();
  const pwdB = randomPassword();
  const userA = await client.registerUser({
    localUserId: USER_A.localId,
    funlifeUsername: USER_A.username,
    displayName: USER_A.name,
    password: pwdA,
  });
  const userB = await client.registerUser({
    localUserId: USER_B.localId,
    funlifeUsername: USER_B.username,
    displayName: USER_B.name,
    password: pwdB,
  });
  cleanup.users.push(userA.recordId, userB.recordId);

  const fr = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
  cleanup.friendships.push(fr.id);
  await client.acceptFriendship(userB.token, fr.id);

  const roomCode = `R${(ts % 899999 + 100000).toString(36).toUpperCase().padStart(5, "2").slice(0, 5)}`;
  const openRoom = await createRoom(userA.token, userA.recordId, {
    game_type: "dice_duel",
    invite_mode: "open",
    room_code: roomCode,
    status: "waiting",
  });
  cleanup.rooms.push(openRoom.id);
  t.expect("开房间 create", openRoom.id && openRoom.room_code === roomCode);

  const found = await findByCode(userB.token, roomCode);
  t.expect("B 可按房间号查询", found?.id === openRoom.id);

  const joined = await patchRoom(userB.token, openRoom.id, {
    guest: userB.recordId,
    guest_ready: true,
    status: "accepted",
  });
  t.expect("B 加入房间", joined.guest === userB.recordId && joined.status === "accepted");

  await patchRoom(userA.token, openRoom.id, { status: "cancelled" });

  const direct = await createRoom(userA.token, userA.recordId, {
    game_type: "gomoku",
    invite_mode: "direct",
    guest: userB.recordId,
    status: "waiting",
    host_ready: true,
  });
  cleanup.rooms.push(direct.id);
  t.expect("直接邀请 create", direct.status === "waiting" && direct.guest === userB.recordId);

  const cancelled = await patchRoom(userA.token, openRoom.id, { status: "cancelled" });
  t.expect("取消房间", cancelled.status === "cancelled");

  printSummary(t);
  process.exit(t.failed > 0 ? 1 : 0);
}

function printSummary(t) {
  console.log("\n---");
  console.log(`PASS ${t.passed}  FAIL ${t.failed}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
