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
  const res = await fetch(`${BASE}/api/collections/users/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity, password: derivedPassword(localId, username) }),
  });
  const j = await res.json();
  if (!j.token) throw new Error(`auth failed ${username}: ${JSON.stringify(j)}`);
  return { token: j.token, id: j.record.id, username };
}

async function main() {
  const host = await authUser("yishi", 1);
  const guest = await authUser("linkg", 1);
  for (const [label, cred] of [["host", host], ["guest", guest]]) {
    const filter = encodeURIComponent(
      `(host = '${cred.id}' || guest = '${cred.id}' || game_state ~ '${cred.id}') && status != 'cancelled' && status != 'finished'`,
    );
    const res = await fetch(`${BASE}/api/collections/game_rooms/records?filter=${filter}&expand=host,guest&perPage=20`, {
      headers: { Authorization: `Bearer ${cred.token}` },
    });
    const j = await res.json();
    console.log(`\n=== ${label} active rooms (${j.items?.length || 0}) ===`);
    for (const r of j.items || []) {
      const gs = r.game_state || {};
      console.log({
        id: r.id,
        status: r.status,
        host: r.host,
        guest: r.guest,
        members: gs.members,
        pending: gs.pending_invite_pb_id,
        updated: r.updated,
      });
    }
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
