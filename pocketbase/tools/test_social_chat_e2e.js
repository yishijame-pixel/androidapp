#!/usr/bin/env node
/**
 * FunLife PocketBase Phase 2 — 私聊 E2E
 *
 * 覆盖：会话创建、双向发消息、列表拉取、非参与者越权。
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
const USER_A = { localId: 910001 + (ts % 1000), username: `chat_a_${ts}`, name: "私聊测试A" };
const USER_B = { localId: 910002 + (ts % 1000), username: `chat_b_${ts}`, name: "私聊测试B" };
const USER_C = { localId: 910003 + (ts % 1000), username: `chat_c_${ts}`, name: "私聊测试C" };

async function main() {
  const client = new PbSocialClient(BASE_URL);
  const t = new TestReporter();
  const cleanup = { friendships: [], conversations: [], messages: [], users: [], adminToken: null };

  console.log("");
  console.log("════════════════════════════════════════════════════════");
  console.log("  FunLife PocketBase 私聊 E2E (Phase 2)");
  console.log(`  Base: ${BASE_URL}`);
  console.log("════════════════════════════════════════════════════════");

  t.expect("PocketBase /api/health 可达", await client.health());
  if (!(await client.health())) {
    printSummary(t);
    process.exit(1);
  }

  let userA, userB, userC;
  try {
    userA = await client.registerUser({ localUserId: USER_A.localId, funlifeUsername: USER_A.username, displayName: USER_A.name, password: randomPassword() });
    userB = await client.registerUser({ localUserId: USER_B.localId, funlifeUsername: USER_B.username, displayName: USER_B.name, password: randomPassword() });
    userC = await client.registerUser({ localUserId: USER_C.localId, funlifeUsername: USER_C.username, displayName: USER_C.name, password: randomPassword() });
    cleanup.users.push(userA.recordId, userB.recordId, userC.recordId);
    t.expect("三用户注册成功", true);
  } catch (e) {
    t.expect("三用户注册", false, e.message);
    printSummary(t);
    process.exit(1);
  }

  console.log("\n▶ 成为好友");
  let friendship;
  try {
    friendship = await client.createFriendRequest(userA.token, userA.recordId, userB.recordId);
    cleanup.friendships.push(friendship.id);
    await client.acceptFriendship(userB.token, friendship.id);
    t.expect("A ↔ B 已是好友", true);
  } catch (e) {
    t.expect("成为好友", false, e.message);
  }

  console.log("\n▶ 会话创建");
  let conversation;
  try {
    conversation = await client.findOrCreateConversation(userA.token, userA.recordId, userB.recordId);
    cleanup.conversations.push(conversation.id);
    t.expect("findOrCreate 返回会话 id", !!conversation.id);
    const again = await client.findOrCreateConversation(userB.token, userB.recordId, userA.recordId);
    t.expect("重复 findOrCreate 同一 pair_key", again.id === conversation.id);
  } catch (e) {
    t.expect("会话创建", false, e.message);
  }

  console.log("\n▶ 发消息 / 拉取");
  try {
    const pairKey = require("./social_test_lib").computePairKey(userA.recordId, userB.recordId);
    const [memberA, memberB] = pairKey.split("|");
    const msgA = await client.sendMessage(userA.token, conversation.id, userA.recordId, memberA, memberB, "你好 B");
    cleanup.messages.push(msgA.id);
    const msgB = await client.sendMessage(userB.token, conversation.id, userB.recordId, memberA, memberB, "你好 A");
    cleanup.messages.push(msgB.id);
    const listA = await client.listMessages(userA.token, conversation.id);
    t.expect("A 能看到 2 条消息", listA.length >= 2);
    const listB = await client.listMessages(userB.token, conversation.id);
    t.expect("B 能看到 2 条消息", listB.length >= 2);
  } catch (e) {
    t.expect("发消息/拉取", false, e.message);
  }

  console.log("\n▶ 安全：非参与者 C 不能读会话消息");
  try {
    const itemsC = await client.listMessages(userC.token, conversation.id);
    t.expect("C 拉取消息为空（Rules 隔离）", itemsC.length === 0);
  } catch (e) {
    t.expect("C 拉取消息被拒绝（403/404）", String(e.message).includes("403") || String(e.message).includes("404"));
  }

  if (!KEEP_DATA) {
    console.log("\n▶ 清理测试数据");
    try {
      cleanup.adminToken = await client.adminAuth(ADMIN_EMAIL, ADMIN_PASSWORD);
      for (const id of cleanup.messages) {
        await client.deleteRecordAdmin(cleanup.adminToken, "messages", id).catch(() => {});
      }
      for (const id of cleanup.conversations) {
        await client.deleteRecordAdmin(cleanup.adminToken, "conversations", id).catch(() => {});
      }
      for (const id of cleanup.friendships) {
        await client.deleteRecordAdmin(cleanup.adminToken, "friendships", id).catch(() => {});
      }
      for (const id of cleanup.users) {
        await client.deleteRecordAdmin(cleanup.adminToken, "users", id).catch(() => {});
      }
      t.expect("Admin 清理完成", true);
    } catch (e) {
      t.expect("Admin 清理", false, e.message);
    }
  }

  printSummary(t);
  process.exit(t.summary().fail > 0 ? 1 : 0);
}

function printSummary(t) {
  const s = t.summary();
  console.log("");
  console.log(`结果: ${s.pass}/${s.total} 通过`);
  if (s.fail > 0) {
    console.log("失败项:");
    s.results.filter((r) => !r.pass).forEach((r) => console.log(`  - ${r.name}`));
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
