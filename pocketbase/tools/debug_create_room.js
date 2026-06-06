#!/usr/bin/env node
"use strict";

const BASE = process.env.POCKETBASE_URL || "http://127.0.0.1:8090";

async function main() {
  const pwd = "TestRoom123!";
  const userRes = await fetch(`${BASE}/api/collections/users/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "u1_linkg@funlife.social.invalid", password: pwd }),
  });
  const userJson = await userRes.json();
  if (!userJson.token) {
    console.error("auth failed", userJson);
    process.exit(1);
  }
  const token = userJson.token;
  const hostId = userJson.record.id;
  console.log("host", hostId);

  let n = 0;
  const code = () => `D${String(n++).padStart(5, "0")}`;

  const variants = [
    ["base", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting" }],
    ["+ready", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting", host_ready: true, guest_ready: false }],
    ["+state", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting", host_ready: true, guest_ready: false, game_state: { max_players: 4, min_players: 2, members: [{ pb_id: hostId, seat: 0, status: "joined" }], member_ids: [hostId] } }],
    ["+expires iso", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting", host_ready: true, guest_ready: false, expires_at: new Date(Date.now() + 600000).toISOString() }],
    ["+expires date", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting", host_ready: true, guest_ready: false, expires_at: new Date(Date.now() + 600000).toISOString().slice(0, 10) }],
    ["+state+expires", { game_type: "gomoku", invite_mode: "open", room_code: code(), host: hostId, status: "waiting", host_ready: true, guest_ready: false, expires_at: "2026-06-07", game_state: { max_players: 4, min_players: 2, members: [{ pb_id: hostId, seat: 0, status: "joined" }], member_ids: [hostId] } }],
  ];

  for (const [label, body] of variants) {
    const r = await fetch(`${BASE}/api/collections/game_rooms/records`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
    const t = await r.text();
    console.log(label, r.status, t);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
