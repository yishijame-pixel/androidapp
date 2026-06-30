#!/usr/bin/env node
/**
 * 空 PostgreSQL 库初始化：DDL + 默认 SKU / ikun 须知
 * 用法: DATABASE_URL=postgres://... node seed_pg_defaults.js
 */
const path = require("path");
const { getPool, runMigration, closePool } = require("../shared/db/postgres");
const SKU = require("../shared/sku");

const IKUN = {
  _id: "ikun_disclosure",
  enabled: true,
  version: 1,
  title: "ikun类角色使用须知",
  body: "欢迎使用「ikun类」梗图行走角色。\n\n请理性使用角色形象，勿用于侮辱、诽谤、骚扰他人，或从事任何违法违规活动。\n\n继续使用即表示您已理解并同意在合法、合规、尊重他人的前提下使用本分类角色。",
  agreeButtonText: "我已阅读并同意",
  footerHint: "请滑动阅读全文后再点击同意",
  updatedAt: new Date().toISOString(),
};

async function upsert(pool, collection, docId, data) {
  const payload = { ...data, _id: docId };
  await pool.query(
    `INSERT INTO documents (collection, doc_id, data, updated_at)
     VALUES ($1, $2, $3::jsonb, now())
     ON CONFLICT (collection, doc_id) DO NOTHING`,
    [collection, docId, JSON.stringify(payload)],
  );
}

async function main() {
  if (!process.env.DATABASE_URL) throw new Error("DATABASE_URL 未设置");
  const mig = path.join(__dirname, "..", "migrations", "postgres", "001_documents.sql");
  await runMigration(mig);
  const pool = getPool();

  for (const [code, sku] of Object.entries(SKU)) {
    if (!sku || typeof sku !== "object") continue;
    await upsert(pool, "vip_sku_config", code, {
      skuCode: code,
      ...sku,
      updatedAt: new Date().toISOString(),
    });
  }
  await upsert(pool, "pac_maze_ikun_disclosure", IKUN._id, IKUN);

  console.log("\nseed 完成: vip_sku_config + pac_maze_ikun_disclosure\n");
  await closePool();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
