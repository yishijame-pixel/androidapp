#!/usr/bin/env node
"use strict";
const crypto = require("crypto");
const BASE = process.env.POCKETBASE_URL || "https://pb.yishi.site";

function derivedPassword(userId, funlifeUsername) {
  const safe = String(funlifeUsername || "").toLowerCase().replace(/[^a-z0-9_]/g, "");
  return crypto.createHash("sha256").update(`funlife_pb_v1:${userId}:${safe}`).digest("base64").slice(0, 43);
}

async function authUser(username, localId) {
  const identity = `u${localId}_${username.toLowerCase().replace(/[^a-z0-9_]/g, "")}@funlife.social.invalid`;
  const password = derivedPassword(localId, username);
  const res = await fetch(`${BASE}/api/collections/users/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity, password }),
  });
  const j = await res.json();
  if (!j.token) throw new Error(`auth failed ${username}: ${JSON.stringify(j)}`);
  return { token: j.token, id: j.record.id, username, identity };
}

async function listInvites(token, myId) {
  const filter = encodeURIComponent(
    `status = 'waiting' && host != '${myId}' && (guest = '${myId}' || game_state ~ '${myId}')`,
  );
  const res = await fetch(`${BASE}/api/collections/game_rooms/records?filter=${filter}&expand=host,guest&perPage=20`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const j = await res.json();
  return j.items || [];
}

async function listFriends(token, myId) {
  const filter = encodeURIComponent(`(requester = '${myId}' || addressee = '${myId}') && status = 'accepted'`);
  const res = await fetch(`${BASE}/api/collections/friendships/records?filter=${filter}&expand=requester,addressee`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const j = await res.json();
  return (j.items || []).map((f) => {
    const friendId = f.requester === myId ? f.addressee : f.requester;
    const prof = f.requester === myId ? f.expand?.addressee : f.expand?.requester;
    return { friendId, username: prof?.funlife_username, name: prof?.name };
  });
}

async function hostRooms(token, myId) {
  const filter = encodeURIComponent(`host = '${myId}' && status = 'waiting'`);
  const res = await fetch(`${BASE}/api/collections/game_rooms/records?filter=${filter}&expand=guest`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const j = await res.json();
  return j.items || [];
}

async function main() {
  const host = await authUser("yishi", 1);
  const guest = await authUser("linkg", 1);

  console.log("\n=== 账号 ===");
  console.log("房主 yishi:", host.id);
  console.log("受邀 linkg:", guest.id);

  console.log("\n=== 房主好友 ===");
  for (const f of await listFriends(host.token, host.id)) {
    console.log(`- ${f.friendId} (${f.username}) ${f.name}`);
  }

  console.log("\n=== 受邀好友 ===");
  for (const f of await listFriends(guest.token, guest.id)) {
    console.log(`- ${f.friendId} (${f.username}) ${f.name}`);
  }

  console.log("\n=== 房主 waiting 房间 ===");
  for (const r of await hostRooms(host.token, host.id)) {
    console.log(`- ${r.id} guest=${r.guest} pending=${r.game_state?.pending_invite_pb_id} mode=${r.invite_mode}`);
  }

  console.log("\n=== 受邀 incoming 列表 ===");
  for (const r of await listInvites(guest.token, guest.id)) {
    console.log(`- ${r.id} host=${r.host} guest=${r.guest} pending=${r.game_state?.pending_invite_pb_id}`);
  }

  const guestInvites = await listInvites(guest.token, guest.id);
  const wouldShow = guestInvites.filter(
    (r) => r.guest === guest.id || r.game_state?.pending_invite_pb_id === guest.id,
  );
  console.log("\n=== App 应显示邀请数 ===", wouldShow.length);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
