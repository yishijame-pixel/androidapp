#!/usr/bin/env node
/** 将 PocketBase 用户密码同步为 App 端 pocketBasePassword 算法（跨设备一致） */
"use strict";

const crypto = require("crypto");

const BASE = process.env.POCKETBASE_URL || "https://pb.yishi.site";
const ADMIN_ID = process.env.PB_ADMIN_EMAIL || "admin@funlife.local";
const ADMIN_PWD = process.env.PB_ADMIN_PASSWORD || "FunLifePB2026!";

function derivedPassword(userId, funlifeUsername) {
  const safe = String(funlifeUsername || "").toLowerCase().replace(/[^a-z0-9_]/g, "");
  const seed = `funlife_pb_v1:${userId}:${safe}`;
  return crypto.createHash("sha256").update(seed).digest("base64").slice(0, 43);
}

async function main() {
  const adminRes = await fetch(`${BASE}/api/collections/_superusers/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: ADMIN_ID, password: ADMIN_PWD }),
  });
  const admin = await adminRes.json();
  if (!admin.token) {
    console.error("admin auth failed", admin);
    process.exit(1);
  }
  const headers = { Authorization: `Bearer ${admin.token}`, "Content-Type": "application/json" };

  let page = 1;
  let updated = 0;
  let skipped = 0;
  while (true) {
    const res = await fetch(`${BASE}/api/collections/users/records?perPage=100&page=${page}`, { headers });
    const json = await res.json();
    const items = json.items || [];
    if (items.length === 0) break;

    for (const u of items) {
      const localId = u.funlife_local_id;
      const username = u.funlife_username;
      if (!localId || !username) {
        skipped++;
        continue;
      }
      const pwd = derivedPassword(localId, username);
      const patch = await fetch(`${BASE}/api/collections/users/records/${u.id}`, {
        method: "PATCH",
        headers,
        body: JSON.stringify({ password: pwd, passwordConfirm: pwd }),
      });
      if (patch.ok) {
        console.log("OK", u.id, username, "localId=", localId);
        updated++;
      } else {
        console.warn("FAIL", u.id, await patch.text());
      }
    }
    if (items.length < 100) break;
    page++;
  }
  console.log(`\nDone: updated=${updated} skipped=${skipped}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
