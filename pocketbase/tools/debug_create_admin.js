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

  const col = await (
    await fetch(`${BASE}/api/collections/game_rooms`, {
      headers: { Authorization: `Bearer ${admin.token}` },
    })
  ).json();

  console.log("=== RULES ===");
  console.log("create:", col.createRule);
  console.log("list:", col.listRule);
  console.log("view:", col.viewRule);
  console.log("update:", col.updateRule);

  const statusField = col.fields.find((f) => f.name === "status");
  console.log("\n=== status values ===", statusField?.values);

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

  const body = {
    game_type: "gomoku",
    invite_mode: "open",
    room_code: "ADM001",
    host: hostId,
    status: "waiting",
  };

  console.log("\n=== ADMIN CREATE ===");
  const adminRes = await fetch(`${BASE}/api/collections/game_rooms/records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${admin.token}`,
    },
    body: JSON.stringify(body),
  });
  console.log("admin", adminRes.status, await adminRes.text());

  console.log("\n=== USER CREATE ===");
  const userRes = await fetch(`${BASE}/api/collections/game_rooms/records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.token}`,
    },
    body: JSON.stringify({ ...body, room_code: "USR001" }),
  });
  console.log("user", userRes.status, await userRes.text());

  // Reset rules to setup-schema baseline
  const simpleRules = {
    listRule:
      "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    viewRule:
      "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    updateRule:
      "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')",
    createRule: "@request.auth.id = host && @request.auth.id != ''",
    deleteRule: "@request.auth.id = host",
  };

  await fetch(`${BASE}/api/collections/game_rooms`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${admin.token}`,
    },
    body: JSON.stringify(simpleRules),
  });

  console.log("\n=== USER CREATE after simple rules ===");
  const userRes2 = await fetch(`${BASE}/api/collections/game_rooms/records`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${user.token}`,
    },
    body: JSON.stringify({ ...body, room_code: "USR002" }),
  });
  console.log("user", userRes2.status, await userRes2.text());
}

main().catch(console.error);
