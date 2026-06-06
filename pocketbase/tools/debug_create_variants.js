#!/usr/bin/env node
"use strict";

const BASE = process.env.POCKETBASE_URL || "http://127.0.0.1:8090";

async function adminAuth() {
  const r = await fetch(`${BASE}/api/collections/_superusers/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "admin@funlife.local", password: "FunLifePB2026!" }),
  });
  const j = await r.json();
  if (!j.token) throw new Error(JSON.stringify(j));
  return j.token;
}

async function main() {
  const token = await adminAuth();
  const h = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

  const list = await (await fetch(`${BASE}/api/collections/game_rooms/records?perPage=5`, { headers: h })).json();
  console.log("existing records:", list.totalItems);

  const user = await (
    await fetch(`${BASE}/api/collections/users/auth-with-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identity: "u1_linkg@funlife.social.invalid", password: "TestRoom123!" }),
    })
  ).json();
  const hostId = user.record.id;

  const variants = [
    ["minimal", { game_type: "gomoku", invite_mode: "open", host: hostId, status: "waiting" }],
    ["no code", { game_type: "gomoku", invite_mode: "open", host: hostId, status: "waiting", room_code: "" }],
    ["direct", { game_type: "gomoku", invite_mode: "direct", host: hostId, status: "waiting" }],
    ["draw_guess", { game_type: "draw_guess", invite_mode: "open", host: hostId, status: "waiting" }],
    ["bad status", { game_type: "gomoku", invite_mode: "open", host: hostId, status: "invite_pending" }],
    ["bad game", { game_type: "chess", invite_mode: "open", host: hostId, status: "waiting" }],
    ["no host", { game_type: "gomoku", invite_mode: "open", status: "waiting" }],
  ];

  for (const [label, body] of variants) {
    const r = await fetch(`${BASE}/api/collections/game_rooms/records`, {
      method: "POST",
      headers: h,
      body: JSON.stringify(body),
    });
    const t = await r.text();
    console.log(label, r.status, t.slice(0, 300));
  }

  const col = await (await fetch(`${BASE}/api/collections/game_rooms`, { headers: h })).json();
  console.log("\nindexes:", JSON.stringify(col.indexes, null, 2));
  console.log("\nrequired fields:", col.fields.filter((f) => f.required && !f.system).map((f) => f.name));
}

main().catch(console.error);
