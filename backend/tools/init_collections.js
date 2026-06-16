// ============================================================
// 一次性集合初始化脚本
// ------------------------------------------------------------
// 背景：CloudBase 数据库不会自动创建集合（首次写入会报
//      [ResourceNotFound] Db or Table not exist），导致 rate-limit
//      等需要"先 get 再 set"的逻辑直接走 fail-open 兜底。
//
// 用法：
//   cd backend/tools
//   node init_collections.js          # 创建所有缺失的集合
//   node init_collections.js --check  # 只检查不创建
//
// 幂等：已存在的集合会跳过；重复执行无副作用
// ============================================================

const path = require("path");
const fs = require("fs");
const { loadEnv } = require("./_loadEnv");
loadEnv();

const CloudBase = require("@cloudbase/manager-node");

// 项目里所有需要的集合（按业务用途列出）
const COLLECTIONS = [
  // 限流
  "vip_rate_limit",
  // 卡密 / 兑换
  "vip_codes",
  "vip_redeem_log",
  // 用户
  "vip_users",
  // 金币
  "vip_coin_logs",
  "vip_coin_snapshots",
  "vip_recover_log",
  // 防重放
  "vip_nonce_logs",
  // 审计 / 封禁
  "vip_ban_log",
  "vip_admin_audit",
  // 运行时 SKU 配置（后台可改 bonusCoins / dailyCoins / durationDays / price / name）
  // doc _id = SKU code (VIP_NORMAL / VIP_YEAR / VIP_LIFETIME)
  "vip_sku_config",
  // 豆人迷宫 · ikun 类进入须知
  "pac_maze_ikun_disclosure",
  // 🆕 v51 时光信箱 月度配额（按 deviceId+ym 唯一）
  "letter_quota",
  // 🆕 v51 聊天记账 AI 日额度（按 deviceId+ymd 唯一）
  "chat_ai_quota",
];

async function main() {
  const envId = process.env.TCB_ENV_ID;
  const secretId = process.env.TCB_SECRET_ID;
  const secretKey = process.env.TCB_SECRET_KEY;
  if (!envId || !secretId || !secretKey) {
    console.error("缺少 TCB_ENV_ID / TCB_SECRET_ID / TCB_SECRET_KEY");
    process.exit(1);
  }

  const manager = new CloudBase({ secretId, secretKey, envId });
  const checkOnly = process.argv.includes("--check");

  // 列出现有集合
  const list = await manager.database.checkCollectionExists("__placeholder__");
  // checkCollectionExists 单查，不会列举；改用 listCollections
  const all = await manager.database.listCollections();
  const existing = new Set((all.Collections || all || []).map((c) => c.CollectionName || c.name || c));
  console.log(`\n云端已有集合 (${existing.size}):`, Array.from(existing).sort().join(", ") || "(空)");

  let created = 0,
    skipped = 0,
    failed = 0;

  for (const name of COLLECTIONS) {
    if (existing.has(name)) {
      console.log(`  ⏭  ${name} 已存在，跳过`);
      skipped++;
      continue;
    }
    if (checkOnly) {
      console.log(`  ⚠️ ${name} 缺失（--check 模式不创建）`);
      continue;
    }
    try {
      const r = await manager.database.createCollectionIfNotExists(name);
      if (r && (r.IsCreated || r.RequestId)) {
        console.log(`  ✅ ${name} 创建成功`);
        created++;
      } else {
        console.log(`  ✅ ${name} 已就绪`);
        created++;
      }
    } catch (e) {
      // 已存在的并发请求会报错，但属于幂等行为，吞掉即可
      if ((e && (e.code === "ResourceInUse" || /already exist/i.test(e.message || "")))) {
        console.log(`  ⏭  ${name} 已存在 (race)`);
        skipped++;
      } else {
        console.error(`  ❌ ${name} 失败:`, e.message || e);
        failed++;
      }
    }
  }

  console.log(`\n=== 结果: created=${created}, skipped=${skipped}, failed=${failed} ===\n`);
  process.exit(failed === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error("脚本异常:", e);
  process.exit(2);
});
