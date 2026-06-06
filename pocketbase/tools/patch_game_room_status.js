#!/usr/bin/env node
/** 移除 game_rooms.status 的 invite_pending（PocketBase API 无法写入该值） */
"use strict";

const BASE = process.argv[2] || process.env.POCKETBASE_URL || "http://127.0.0.1:8090";

async function adminToken(api) {
  const res = await fetch(`${api}/collections/_superusers/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "admin@funlife.local", password: "FunLifePB2026!" }),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`admin auth failed: ${JSON.stringify(json)}`);
  return json.token;
}

async function main() {
  const api = `${BASE.replace(/\/$/, "")}/api`;
  const token = await adminToken(api);
  const headers = { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };

  const colRes = await fetch(`${api}/collections/game_rooms`, { headers });
  const col = await colRes.json();
  if (!colRes.ok) throw new Error(`game_rooms missing: ${JSON.stringify(col)}`);

  const fields = col.fields.map((f) => {
    if (f.name !== "status") return f;
    return {
      ...f,
      values: [
        "waiting",
        "accepted",
        "playing",
        "finished",
        "cancelled",
        "expired",
        "abandoned",
      ],
    };
  });

  const patchRes = await fetch(`${api}/collections/game_rooms`, {
    method: "PATCH",
    headers,
    body: JSON.stringify({ fields }),
  });
  const patchJson = await patchRes.json();
  if (!patchRes.ok) throw new Error(`patch failed: ${JSON.stringify(patchJson)}`);

  console.log(`OK: game_rooms.status values patched on ${BASE}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
