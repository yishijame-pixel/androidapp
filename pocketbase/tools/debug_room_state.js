#!/usr/bin/env node
"use strict";
const BASE = process.env.POCKETBASE_URL || "https://pb.yishi.site";
const roomId = process.argv[2] || "bu159w5st42kcbq";
const guestId = process.argv[3] || "ps6qek9wbx2imhy";

async function main() {
  const auth = await fetch(`${BASE}/api/collections/users/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "u1_linkg@funlife.social.invalid", password: "TestRoom123!" }),
  }).then((r) => r.json());
  const token = auth.token;

  const room = await fetch(`${BASE}/api/collections/game_rooms/records/${roomId}?expand=host,guest`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());

  console.log("\n=== Room", roomId, "===");
  console.log(JSON.stringify({
    host: room.host,
    guest: room.guest,
    status: room.status,
    invite_mode: room.invite_mode,
    pending: room.game_state?.pending_invite_pb_id,
    members: room.game_state?.members,
  }, null, 2));

  const filter = encodeURIComponent(
    `status = 'waiting' && (guest = '${guestId}' || game_state ~ '${guestId}')`,
  );
  const list = await fetch(`${BASE}/api/collections/game_rooms/records?filter=${filter}&perPage=20`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((r) => r.json());

  console.log("\n=== Guest", guestId, "incoming list ===");
  console.log("count:", list.items?.length ?? 0);
  for (const item of list.items || []) {
    console.log("-", item.id, "guest=", item.guest, "pending=", item.game_state?.pending_invite_pb_id);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
