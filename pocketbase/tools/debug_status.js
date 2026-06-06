"use strict";
const BASE = process.argv[2] || "http://127.0.0.1:8090";

(async () => {
  const api = `${BASE}/api`;
  const auth = await fetch(`${api}/collections/_superusers/auth-with-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ identity: "admin@funlife.local", password: "FunLifePB2026!" }),
  });
  const { token } = await auth.json();
  const col = await fetch(`${api}/collections/game_rooms`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const c = await col.json();
  const statusField = c.fields.find((f) => f.name === "status");
  console.log("status values:", statusField.values);

  const users = await fetch(`${api}/collections/users/records?perPage=1`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const u = (await users.json()).items[0].id;

  for (const st of statusField.values) {
    const res = await fetch(`${api}/collections/game_rooms/records`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        host: u,
        game_type: "dice_duel",
        invite_mode: "direct",
        guest: u,
        status: st,
      }),
    });
    console.log(st, "=>", res.status, (await res.text()).slice(0, 80));
  }
})();
