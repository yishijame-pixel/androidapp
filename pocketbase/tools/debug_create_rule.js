#!/usr/bin/env node
"use strict";

const BASE = process.env.POCKETBASE_URL || "http://127.0.0.1:8090";

async function main() {
  const admin = await (
    await fetch(`${BASE}/api/collections/_superusers/auth-with-password`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identity: "admin@funlife.local", password: "FunLifePB2026!" }),
    })
  ).json();

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

  if (!user.token) {
    console.error("user auth failed", user);
    process.exit(1);
  }

  const token = user.token;
  const hostId = user.record.id;
  console.log("hostId", hostId);

  async function tryCreate(label) {
    const code = ("Z" + Math.random().toString(36).slice(2, 8)).toUpperCase().slice(0, 6);
    const body = {
      game_type: "gomoku",
      invite_mode: "open",
      room_code: code,
      host: hostId,
      status: "waiting",
    };
    const r = await fetch(`${BASE}/api/collections/game_rooms/records`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
    console.log(label, r.status, await r.text());
  }

  async function setRule(createRule) {
    const col = await (
      await fetch(`${BASE}/api/collections/game_rooms`, {
        headers: { Authorization: `Bearer ${admin.token}` },
      })
    ).json();
    col.createRule = createRule;
    const r = await fetch(`${BASE}/api/collections/game_rooms`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${admin.token}` },
      body: JSON.stringify(col),
    });
    if (!r.ok) console.error("setRule failed", createRule, await r.text());
  }

  const rules = [
    ["empty", ""],
    ["auth only", "@request.auth.id != ''"],
    ["host eq auth", "host = @request.auth.id"],
    ["auth eq host", "@request.auth.id = host"],
    ["body host", "@request.auth.id = @request.body.host"],
    ["original", "@request.auth.id = host && @request.auth.id != ''"],
  ];

  for (const [label, rule] of rules) {
    await setRule(rule);
    await tryCreate(label);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
