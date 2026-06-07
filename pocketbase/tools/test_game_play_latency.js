#!/usr/bin/env node
/**
 * 对局页资料/同步延迟探测（公网 PocketBase）
 *
 * 用法：
 *   node pocketbase/tools/test_game_play_latency.js --base-url https://pb.yishi.site
 */
"use strict";

const { PbSocialClient, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}

const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const ts = Date.now();

function ms(start) {
  return `${(performance.now() - start).toFixed(0)}ms`;
}

function expandAvatar(user) {
  if (!user?.avatar) return null;
  const name = user.avatar;
  if (typeof name === "string" && name.startsWith("http")) return name;
  return `${BASE_URL}/api/files/users/${user.id}/${user.avatar}`;
}

async function timed(label, fn) {
  const t0 = performance.now();
  const result = await fn();
  const elapsed = performance.now() - t0;
  console.log(`  ${label}: ${elapsed.toFixed(0)}ms`);
  return { result, elapsed };
}

async function main() {
  const client = new PbSocialClient(BASE_URL);
  console.log("\n=== 对局资料/同步延迟探测 ===");
  console.log("Base:", BASE_URL, "\n");

  if (!(await client.health())) {
    console.log("FAIL: PocketBase 不可达");
    process.exit(1);
  }

  const userA = await client.registerUser({
    localUserId: 940001 + (ts % 1000),
    funlifeUsername: `lat_a_${ts}`,
    displayName: "延迟测试A",
    password: randomPassword(),
  });
  const userB = await client.registerUser({
    localUserId: 940002 + (ts % 1000),
    funlifeUsername: `lat_b_${ts}`,
    displayName: "延迟测试B",
    password: randomPassword(),
  });

  const { json: room } = await client.request("POST", "/collections/game_rooms/records", {
    token: userA.token,
    body: {
      game_type: "gomoku",
      invite_mode: "direct",
      host: userA.recordId,
      guest: userB.recordId,
      status: "playing",
      host_ready: true,
      guest_ready: true,
      current_turn: userA.recordId,
      game_state: {
        max_players: 2,
        min_players: 2,
        members: [
          { pb_id: userA.recordId, seat: 0, status: "joined" },
          { pb_id: userB.recordId, seat: 1, status: "joined" },
        ],
        member_ids: [userA.recordId, userB.recordId],
        gomoku: {
          board: ".".repeat(225),
          move_count: 0,
          black_pb_id: userA.recordId,
          white_pb_id: userB.recordId,
        },
      },
    },
  });
  const roomId = room.id;

  console.log("▶ 单次 HTTP 延迟（模拟进房首屏）");
  const health = await timed("health", () => client.health());
  const roomNoExpand = await timed("getGameRoom 无 expand", async () => {
    const { json } = await client.request("GET", `/collections/game_rooms/records/${roomId}`, {
      token: userB.token,
    });
    return json;
  });
  const roomExpand = await timed("getGameRoom expand=host,guest", async () => {
    const expand = encodeURIComponent("host,guest");
    const { json } = await client.request(
      "GET",
      `/collections/game_rooms/records/${roomId}?expand=${expand}`,
      { token: userB.token },
    );
    return json;
  });
  const moves = await timed("listGameMoves", async () => {
    const filter = encodeURIComponent(`room = '${roomId}'`);
    const { json } = await client.request(
      "GET",
      `/collections/game_moves/records?filter=${filter}&sort=move_index&perPage=50`,
      { token: userB.token },
    );
    return json.items || [];
  });

  const host = roomExpand.result.expand?.host;
  const guest = roomExpand.result.expand?.guest;
  console.log("\n▶ expand 资料是否即时可用");
  console.log(`  房主 name: ${host?.name || "(空)"}`);
  console.log(`  房主 avatar: ${expandAvatar(host) || "(空)"}`);
  console.log(`  对手 name: ${guest?.name || "(空)"}`);

  console.log("\n▶ 模拟 App 落子链路 HTTP 次数与耗时");
  const placeChainStart = performance.now();
  await client.request("POST", "/collections/game_moves/records", {
    token: userA.token,
    body: {
      room: roomId,
      player: userA.recordId,
      move_index: 1,
      payload: { kind: "gomoku_place", x: 7, y: 7 },
    },
  });
  const tMove = performance.now() - placeChainStart;
  console.log(`  createGameMove: ${tMove.toFixed(0)}ms`);

  const tPatch0 = performance.now();
  await client.request("PATCH", `/collections/game_rooms/records/${roomId}`, {
    token: userA.token,
    body: { current_turn: userB.recordId },
  });
  console.log(`  patch current_turn: ${(performance.now() - tPatch0).toFixed(0)}ms`);

  const tGuestSee = performance.now();
  const { json: guestRoom } = await client.request(
    "GET",
    `/collections/game_rooms/records/${roomId}?expand=${encodeURIComponent("host,guest")}`,
    { token: userB.token },
  );
  const guestSeeMs = performance.now() - tGuestSee;
  console.log(`  对手拉 room+expand: ${guestSeeMs.toFixed(0)}ms`);
  console.log(`  对手可见 current_turn=${guestRoom.current_turn === userB.recordId ? "白方" : guestRoom.current_turn}`);

  const totalPlayEntry = health.elapsed + roomExpand.elapsed + moves.elapsed;
  console.log("\n--- 汇总（估算）---");
  console.log(`  进房首屏（health+room expand+moves）: ~${totalPlayEntry.toFixed(0)}ms`);
  console.log(`  无 expand 的 room（旧缓存路径）: ${roomNoExpand.elapsed.toFixed(0)}ms`);
  console.log(`  有 expand 的 room（推荐路径）: ${roomExpand.elapsed.toFixed(0)}ms`);
  console.log(`  对手感知落子（HTTP 拉取）: ~${(tMove + guestSeeMs).toFixed(0)}ms`);
  console.log(`  Realtime SSE 推送（本脚本无法测，App 内通常 <500ms）`);
  console.log("");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
