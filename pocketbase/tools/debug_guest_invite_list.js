#!/usr/bin/env node
"use strict";
const BASE = process.env.POCKETBASE_URL || "https://pb.yishi.site";

async function main() {
  const auth = await fetch(`${BASE}/api/collections/users/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "u1_linkg@funlife.social.invalid", password: "TestRoom123!" }),
  }).then((r) => r.json());
  const myPbId = auth.record.id;
  const token = auth.token;
  console.log("logged in as", myPbId);

  const filter1 = encodeURIComponent(
    `status = 'waiting' && (guest = '${myPbId}' || game_state ~ '${myPbId}')`,
  );
  const filter2 = encodeURIComponent(
    `status = 'waiting' && host != '${myPbId}' && (guest = '${myPbId}' || game_state ~ '${myPbId}')`,
  );
  const filter3 = encodeURIComponent(`status = 'waiting' && guest = '${myPbId}'`);

  for (const [label, filter] of [
    ["current app filter", filter1],
    ["fixed filter", filter2],
    ["guest-only", filter3],
  ]) {
    const list = await fetch(`${BASE}/api/collections/game_rooms/records?filter=${filter}&perPage=20`, {
      headers: { Authorization: `Bearer ${token}` },
    }).then((r) => r.json());
    console.log(label, "count=", list.items?.length, list.items?.map((i) => `${i.id}(host=${i.host},guest=${i.guest})`));
  }
}

main();
