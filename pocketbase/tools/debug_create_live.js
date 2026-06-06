#!/usr/bin/env node
"use strict";

const BASE = process.env.POCKETBASE_URL || "http://127.0.0.1:8090";

async function main() {
  const user = await (
    await fetch(`${BASE}/api/collections/users/auth-with-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        identity: "u1_linkg@funlife.social.invalid",
        password: "TestRoom123!",
      }),
    })
  ).json();
  const hostId = user.record.id;
  const code = "LIVE" + Date.now().toString(36).slice(-4).toUpperCase();

  const body = {
    game_type: "gomoku",
    invite_mode: "open",
    room_code: code,
    host: hostId,
    status: "waiting",
  };

  const r = await fetch(`${BASE}/api/collections/game_rooms/records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.token}`,
    },
    body: JSON.stringify(body),
  });
  const text = await r.text();
  console.log("POST", r.status, text);

  const admin = await (
    await fetch(`${BASE}/api/collections/_superusers/auth-with-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identity: "admin@funlife.local", password: "FunLifePB2026!" }),
    })
  ).json();

  const list = await (
    await fetch(`${BASE}/api/collections/game_rooms/records?filter=room_code='${code}'`, {
      headers: { Authorization: `Bearer ${admin.token}` },
    })
  ).json();
  console.log("admin find by code:", JSON.stringify(list.items?.[0] || null, null, 2));

  if (list.items?.[0]) {
    const id = list.items[0].id;
    const userGet = await fetch(`${BASE}/api/collections/game_rooms/records/${id}`, {
      headers: { Authorization: `Bearer ${user.token}` },
    });
    console.log("user GET by id", userGet.status, await userGet.text());
  }
}

main().catch(console.error);
