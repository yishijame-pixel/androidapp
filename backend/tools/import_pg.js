#!/usr/bin/env node
/**
 * 将 export_tcb.js 导出的 JSON 导入 PostgreSQL documents 表
 * 用法:
 *   DATABASE_URL=postgres://... node import_pg.js
 *   DATABASE_URL=... node import_pg.js backup/tcb-export/2026-06-21
 */
const fs = require("fs");
const path = require("path");
const { getPool, runMigration, closePool } = require("../shared/db/postgres");

function unwrapMongo(v) {
  if (v == null) return v;
  if (typeof v !== "object") return v;
  if (v.$date !== undefined) {
    const n = v.$date.$numberLong ?? v.$date;
    return new Date(Number(n)).toISOString();
  }
  if (v.$numberInt !== undefined) return parseInt(v.$numberInt, 10);
  if (v.$numberLong !== undefined) return Number(v.$numberLong);
  if (v.$numberDouble !== undefined) return parseFloat(v.$numberDouble);
  if (Array.isArray(v)) return v.map(unwrapMongo);
  const o = {};
  for (const [k, val] of Object.entries(v)) o[k] = unwrapMongo(val);
  return o;
}

function normalizeImportedDoc(raw) {
  let doc = raw;
  if (typeof doc === "string") doc = JSON.parse(doc);
  doc = unwrapMongo(doc);
  if (doc.data && typeof doc.data === "object" && !doc.code && !doc.skuCode) {
    const inner = unwrapMongo(doc.data);
    doc = { ...inner, _id: doc._id || inner._id };
    delete doc.data;
  }
  return doc;
}

function resolveImportDir(arg) {
  if (arg) return path.resolve(arg);
  const base = path.join(__dirname, "..", "..", "backup", "tcb-export");
  if (!fs.existsSync(base)) throw new Error(`缺少目录 ${base}，请先运行 export_tcb.js`);
  const dirs = fs.readdirSync(base).filter((d) => fs.statSync(path.join(base, d)).isDirectory()).sort();
  if (!dirs.length) throw new Error("无导出目录");
  return path.join(base, dirs[dirs.length - 1]);
}

async function importFile(pool, collection, filePath) {
  const raw = fs.readFileSync(filePath, "utf-8");
  const docs = JSON.parse(raw);
  if (!Array.isArray(docs) || !docs.length) return 0;
  let n = 0;
  for (const raw of docs) {
    const doc = normalizeImportedDoc(raw);
    const docId = String(doc._id || doc.code || doc.key || doc.id || `${collection}_${n}`);
    const payload = { ...doc, _id: docId };
    await pool.query(
      `INSERT INTO documents (collection, doc_id, data, updated_at)
       VALUES ($1, $2, $3::jsonb, now())
       ON CONFLICT (collection, doc_id) DO UPDATE SET data = EXCLUDED.data, updated_at = now()`,
      [collection, docId, JSON.stringify(payload)],
    );
    n++;
  }
  return n;
}

async function main() {
  const dir = resolveImportDir(process.argv[2]);
  if (!process.env.DATABASE_URL) throw new Error("DATABASE_URL 未设置");

  const mig = path.join(__dirname, "..", "migrations", "postgres", "001_documents.sql");
  await runMigration(mig);

  const pool = getPool();
  console.log(`\n导入 ${dir}\n`);

  const files = fs.readdirSync(dir).filter((f) => f.endsWith(".json") && !f.startsWith("_"));
  let total = 0;
  for (const file of files) {
    const collection = file.replace(/\.json$/, "");
    const n = await importFile(pool, collection, path.join(dir, file));
    console.log(`  ${collection}: ${n}`);
    total += n;
  }
  console.log(`\n完成，共 ${total} 条\n`);
  await closePool();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
