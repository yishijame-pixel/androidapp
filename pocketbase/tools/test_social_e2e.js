#!/usr/bin/env node
/**
 * FunLife PocketBase 社交 — 企业级 E2E 场景测试
 *
 * 覆盖：注册/登录、搜索、发申请、重复拦截、pending+expand、拒绝、接受、
 *       删除好友、Token 刷新、权限边界、资料补全。
 *
 * 用法：
 *   node pocketbase/tools/test_social_e2e.js
 *   node pocketbase/tools/test_social_e2e.js --base-url http://127.0.0.1:8090
 *   node pocketbase/tools/test_social_e2e.js --keep-data
 */
"use strict";

const { PbSocialClient, TestReporter, randomPassword } = require("./social_test_lib");

const args = process.argv.slice(2);
function arg(name, fallback) {
  const i = args.indexOf(name);
  return i >= 0 && args[i + 1] ? args[i + 1] : fallback;
}
const BASE_URL = arg("--base-url", process.env.POCKETBASE_URL || "http://127.0.0.1:8090");
const KEEP_DATA = args.includes("--keep-data");
const ADMIN_EMAIL = arg("--admin-email", "admin@funlife.local");
const ADMIN_PASSWORD = arg("--admin-password", "FunLifePB2026!");

const ts = Date.now();
const USER_A = {
  localId: 900001 + (ts % 1000),
  username: `soc_a_${ts}`,
  name: "社交测试A",
};
const USER_B = {
  localId: 900002 + (ts % 1000),
  username: `soc_b_${ts}`,
  name: "社交测试B",
};

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();
  const cleanup = { friendships: [], users: [], adminToken: null };

  console.log("");
  console.log("════════════════════════════════════════════════════════");
  console.log("  FunLife PocketBase 社交 E2E");
  console.log(`  Base: ${BASE_URL}`);
  console.log("════════════════════════════════════════════════════════");
  console.log("");

  // ── 0. 健康检查 ──
  console.log("▶ 基础设施");
  const healthy = await client.health();
  t.expect("PocketBase /api/health 可达", healthy);
  if (!healthy) {
    printSummary(t);
    process.exit(1);
  }

  // ── 1. 注册双用户 ──
  console.log("\n▶ 账号绑定（模拟 App registerUser）");
  let userA, userB;
  try {
    const pwdA = randomPassword();
    const pwdB = randomPassword();
    userA = await client.registerUser({
      localUserId: USER_A.localId,
      funlifeUsername: USER_A.username,
      displayName: USER_A.name,
      password: pwdA,
    });
    userB = await client.registerUser({
      localUserId: USER_B.localId,
      funlifeUsername: USER_B.username,
      displayName: USER_B.name,
      password: pwdB,
    });
    cleanup.users.push(userA.recordId, userB.recordId);
    t.expect("用户 A 注册并拿到 Token", !!userA.token && !!userA.recordId);
    t.expect("用户 B 注册并拿到 Token", !!userB.token && !!userB.recordId);
  } catch (e) {
    t.expect("双用户注册", false, e.message);
    printSummary(t);
    process.exit(1);
  }

  // ── 2. Token 刷新 ──
  console.log("\n▶ 凭证生命周期");
  try {
    const refreshed = await client.authRefresh(userA.token);
    t.expect("auth-refresh 成功", !!refreshed.token);
    userA.token = refreshed.token;
  } catch (e) {
    t.expect("auth-refresh 成功", false, e.message);
  }

  // ── 3. 搜索 ──
  console.log("\n▶ 用户搜索");
  try {
    const found = await client.findUserByUsername(userA.token, USER_B.username);
    t.expect("A 能搜到 B (@username)", found?.funlife_username === USER_B.username);
    const missing = await client.findUserByUsername(userA.token, `no_such_${ts}`);
    t.expect("搜索不存在用户返回空", missing == null);
  } catch (e) {
    t.expect("用户搜索", false, e.message);
  }

  // ── 4. 发好友申请 ──
  console.log("\n▶ 好友申请 — 发送");
  let friendship;
  try {
    friendship = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
    cleanup.friendships.push(friendship.id);
    t.expect("A → B 创建 pending 成功", friendship.status === "pending");
    const expandReq = friendship.expand?.requester;
    const expandAdd = friendship.expand?.addressee;
    t.expect("create 后 expand 含 requester 资料", !!expandReq?.funlife_username || !!expandReq?.id);
    t.expect("create 后 expand 含 addressee 资料", !!expandAdd?.funlife_username || !!expandAdd?.id);
  } catch (e) {
    t.expect("A → B 创建 pending", false, e.message);
  }

  // ── 5. 重复申请 ──
  await t.expectThrows(
    "重复好友申请被拦截（唯一索引）",
    () => client.createFriendRequest(userA.token, userA.recordId, userB.recordId),
    "400",
  );

  // ── 6. B 的 pending 列表 + expand ──
  console.log("\n▶ 好友申请 — 收件箱");
  try {
    const pending = await client.listPendingIncoming(userB.token, userB.recordId);
    t.expect("B 收到 1 条 pending", pending.length === 1);
    const req = pending[0]?.expand?.requester;
    t.expect("pending expand 含 requester.funlife_username", req?.funlife_username === USER_A.username);
  } catch (e) {
    t.expect("pending 收件箱", false, e.message);
  }

  // ── 7. B 拒绝 ──
  console.log("\n▶ 好友申请 — 拒绝");
  try {
    await client.deleteFriendship(userB.token, friendship.id);
    cleanup.friendships = cleanup.friendships.filter((id) => id !== friendship.id);
    const afterReject = await client.listPendingIncoming(userB.token, userB.recordId);
    t.expect("拒绝后 pending 清空", afterReject.length === 0);
  } catch (e) {
    t.expect("拒绝好友申请", false, e.message);
  }

  // ── 8. 再次申请 → 接受 ──
  console.log("\n▶ 好友申请 — 接受");
  try {
    friendship = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
    cleanup.friendships.push(friendship.id);
    await client.acceptFriendship(userB.token, friendship.id);
    const listA = await client.listFriendships(userA.token, userA.recordId);
    const listB = await client.listFriendships(userB.token, userB.recordId);
    const acceptedA = listA.find((f) => f.id === friendship.id);
    const acceptedB = listB.find((f) => f.id === friendship.id);
    t.expect("接受后 A 列表 status=accepted", acceptedA?.status === "accepted");
    t.expect("接受后 B 列表 status=accepted", acceptedB?.status === "accepted");
  } catch (e) {
    t.expect("接受好友流程", false, e.message);
  }

  // ── 9. 删除好友 ──
  console.log("\n▶ 删除好友");
  try {
    await client.deleteFriendship(userA.token, friendship.id);
    cleanup.friendships = cleanup.friendships.filter((id) => id !== friendship.id);
    const listAfter = await client.listFriendships(userA.token, userA.recordId);
    t.expect("删除后好友列表不含该关系", !listAfter.some((f) => f.id === friendship.id));
  } catch (e) {
    t.expect("删除好友", false, e.message);
  }

  // ── 10. 权限边界 ──
  console.log("\n▶ 安全规则");
  try {
    const unauthList = await client.request(
      "GET",
      `/collections/users/records?perPage=1`,
      { token: null, expectOk: true },
    );
    const items = unauthList.json?.items || [];
    t.expect(
      "未登录不泄露用户列表（401/403 或空列表）",
      unauthList.status === 401 ||
        unauthList.status === 403 ||
        items.length === 0,
      `status=${unauthList.status} items=${items.length}`,
    );
  } catch (e) {
    t.expect("未登录 list users 安全", false, e.message);
  }
  await t.expectThrows(
    "未登录不能创建好友申请",
    () => client.createFriendRequest(null, userA.recordId, userB.recordId),
    ["401", "403", "400"],
  );
  await t.expectThrows(
    "不能代他人发起好友申请（createRule）",
    () => client.createFriendRequest(userB.token, userA.recordId, userB.recordId),
    ["400", "403"],
  );

  // ── 11. getUserById 资料补全（通知场景） ──
  console.log("\n▶ 资料补全（通知/列表 fallback）");
  try {
    const profile = await client.getUserById(userB.token, userA.recordId);
    t.expect("getUserById 返回 funlife_username", profile.funlife_username === USER_A.username);
    t.expect("getUserById 返回 name/display", !!profile.name);
  } catch (e) {
    t.expect("getUserById 资料补全", false, e.message);
  }

  // ── 清理 ──
  if (!KEEP_DATA) {
    console.log("\n▶ 清理测试数据");
    try {
      cleanup.adminToken = await client.adminAuth(ADMIN_EMAIL, ADMIN_PASSWORD);
      for (const fid of cleanup.friendships) {
        await client.deleteRecordAdmin(cleanup.adminToken, "friendships", fid).catch(() => {});
      }
      for (const uid of cleanup.users) {
        await client.deleteRecordAdmin(cleanup.adminToken, "users", uid).catch(() => {});
      }
      t.expect("Admin 清理测试 users/friendships", true);
    } catch (e) {
      t.expect("Admin 清理测试数据", false, e.message);
    }
  } else {
    console.log("\n▶ --keep-data：保留测试账号");
    console.log(`   A: @${USER_A.username}  B: @${USER_B.username}`);
  }

  printSummary(t);
  process.exit(t.fail > 0 ? 1 : 0);
}

function printSummary(t) {
  const s = t.summary();
  console.log("");
  console.log("════════════════════════════════════════════════════════");
  if (s.fail === 0) {
    console.log(`  ★ 全部 ${s.total} 项通过`);
  } else {
    console.log(`  ✗ ${s.fail} / ${s.total} 项失败`);
    s.results.filter((r) => !r.pass).forEach((r) => console.log(`    - ${r.name}`));
  }
  console.log("════════════════════════════════════════════════════════");
  console.log("");
}

main().catch((e) => {
  console.error("FATAL:", e);
  process.exit(2);
});
