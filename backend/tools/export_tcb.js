#!/usr/bin/env node
/**
 * 从 CloudBase 导出全部集合到 backup/tcb-export/<date>/
 *
 * 若遇 EXCEED_REQUEST_LIMIT，请改用:
 *   node export_tcb_nosql.js [--only=vip_codes,vip_users]
 *
 * 用法: cd backend/tools && node export_tcb.js
 */
const fs = require("fs");
const path = require("path");
const { initTcb } = require("./_loadEnv");

const ALL_COLLECTIONS = [
  "vip_rate_limit", "vip_codes", "vip_redeem_log", "vip_users",
  "vip_coin_logs", "vip_coin_snapshots", "vip_coin_nonces", "vip_recover_log",
  "vip_admin_audit", "vip_sku_config", "vip_revocations", "vip_user_bans",
  "vip_device_marks", "pac_maze_ikun_disclosure",
  "letter_quota", "chat_ai_quota", "chat_ai_quota_month", "chat_ai_trial",
  "chat_ai_book_quota", "chat_ai_dna_quota",
  "quote_galaxy", "galaxy_lights", "galaxy_reports", "galaxy_publish_quota",
  "postcards", "postcard_quota", "active_readers",
];

async function exportCollection(db, name, outDir) {
  const col = db.collection(name);
  const all = [];
  const pageSize = 100;
  let offset = 0;
  while (true) {
    let batch = [];
    let attempt = 0;
    while (attempt < 8) {
      try {
        const r = await col.skip(offset).limit(pageSize).get();
        batch = r.data || [];
        break;
      } catch (e) {
        const msg = e.message || "";
        if (/not exist|NOT_FOUND/i.test(msg)) {
          console.log(`  skip ${name} (集合不存在)`);
          return 0;
        }
        if (/EXCEED_REQUEST_LIMIT|LimitExceeded|OutOfReadRequestQuota/i.test(msg)) {
          attempt++;
          const wait = Math.min(30000, 2000 * attempt);
          console.log(`  ${name} 读配额限流，${wait}ms 后重试 (${attempt}/8)`);
          await new Promise((r) => setTimeout(r, wait));
          continue;
        }
        throw e;
      }
    }
    if (attempt >= 8) {
      console.warn(`  skip ${name}（读配额持续超限，稍后单独重试）`);
      return 0;
    }
    if (!batch.length) break;
    all.push(...batch);
    offset += batch.length;
    if (batch.length < pageSize) break;
    await new Promise((r) => setTimeout(r, 500));
  }
  fs.writeFileSync(
    path.join(outDir, `${name}.json`),
    JSON.stringify(all, null, 2),
    "utf-8",
  );
  console.log(`  ${name}: ${all.length} docs`);
  return all.length;
}

async function main() {
  const onlyArg = process.argv.find((a) => a.startsWith("--only="));
  const only = onlyArg ? onlyArg.slice(7).split(",") : null;
  const COLLECTIONS = only
    ? ALL_COLLECTIONS.filter((c) => only.includes(c))
    : ALL_COLLECTIONS;
  if (!COLLECTIONS.length) {
    console.error("无匹配集合，可用:", ALL_COLLECTIONS.join(", "));
    process.exit(1);
  }

  const app = initTcb();
  const db = app.database();
  const stamp = new Date().toISOString().slice(0, 10);
  const outDir = path.join(__dirname, "..", "..", "backup", "tcb-export", stamp);
  fs.mkdirSync(outDir, { recursive: true });
  console.log(`\n导出 → ${outDir}\n`);

  let total = 0;
  for (const name of COLLECTIONS) {
    total += await exportCollection(db, name, outDir);
    await new Promise((r) => setTimeout(r, 1500));
  }
  const meta = { exportedAt: new Date().toISOString(), collections: COLLECTIONS, totalDocs: total };
  fs.writeFileSync(path.join(outDir, "_meta.json"), JSON.stringify(meta, null, 2));
  console.log(`\n完成，共 ${total} 条文档\n`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
