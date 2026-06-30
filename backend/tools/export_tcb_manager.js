#!/usr/bin/env node
/**
 * 通过 @cloudbase/manager-node 导出集合（绕过 node-sdk 读配额）
 *
 * 策略 1: RunCommands (Mongo 命令)
 * 策略 2: DatabaseMigrateExport → COS → 下载
 *
 * 用法:
 *   node export_tcb_manager.js --only=vip_codes,vip_users
 *   node export_tcb_manager.js --method=export --only=vip_codes
 */
const fs = require("fs");
const path = require("path");
const { loadEnv } = require("./_loadEnv");

const PRIORITY = [
  "vip_codes", "vip_users", "vip_sku_config", "vip_redeem_log",
  "vip_revocations", "vip_user_bans", "vip_device_marks",
  "chat_ai_quota", "chat_ai_quota_month", "letter_quota",
  "pac_maze_ikun_disclosure", "quote_galaxy", "postcards",
];

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function parseArgs() {
  const onlyArg = process.argv.find((a) => a.startsWith("--only="));
  const methodArg = process.argv.find((a) => a.startsWith("--method="));
  const only = onlyArg ? onlyArg.slice(7).split(",").map((s) => s.trim()).filter(Boolean) : PRIORITY;
  const method = methodArg ? methodArg.slice(9) : "auto";
  return { only, method };
}

async function tryRunCommands(db, collection) {
  const variants = [
    `db.collection("${collection}").limit(100).get()`,
    `db.getCollection("${collection}").find().limit(100).toArray()`,
    JSON.stringify({ collection, type: "query", limit: 100 }),
  ];
  for (const cmd of variants) {
    try {
      const r = await db.runCommands({ MgoCommands: [cmd] });
      const raw = r.Data || r.data || r;
      const text = typeof raw === "string" ? raw : JSON.stringify(raw);
      if (/EXCEED|LimitExceeded|quota/i.test(text)) continue;
      const parsed = typeof raw === "string" ? JSON.parse(raw) : raw;
      const rows = Array.isArray(parsed) ? parsed : parsed?.data || parsed?.Data || [];
      if (Array.isArray(rows) && rows.length) return rows;
      if (Array.isArray(parsed?.[0]?.data)) return parsed[0].data;
    } catch (e) {
      if (!/EXCEED|LimitExceeded|quota/i.test(e.message || "")) {
        // try next variant
      }
    }
  }
  return null;
}

async function exportViaCos(manager, collection, outFile) {
  const storage = manager.storage;
  const objectKey = `tcb-export/${collection}_${Date.now()}.json`;
  console.log(`  [export] ${collection} → COS ${objectKey}`);
  const job = await manager.database.export(collection, {
    ObjectKey: objectKey,
    FileType: "json",
  });
  const jobId = job.JobId || job.jobId;
  if (!jobId) throw new Error(`无 JobId: ${JSON.stringify(job)}`);

  for (let i = 0; i < 60; i++) {
    await sleep(3000);
    const st = await manager.database.migrateStatus(jobId);
    const status = st.Status || st.status || st.State;
    console.log(`    job ${jobId}: ${status || JSON.stringify(st).slice(0, 120)}`);
    if (/success|finish|complete|done/i.test(String(status))) break;
    if (/fail|error/i.test(String(status))) throw new Error(`导出失败: ${JSON.stringify(st)}`);
  }

  const tmp = path.join(__dirname, ".tmp_export_" + collection + ".json");
  await storage.downloadFile({ cloudPath: objectKey, localPath: tmp });
  const content = fs.readFileSync(tmp, "utf-8");
  fs.unlinkSync(tmp);
  let docs = JSON.parse(content);
  if (!Array.isArray(docs)) {
    docs = docs.data || docs.records || docs.list || [];
  }
  fs.writeFileSync(outFile, JSON.stringify(docs, null, 2), "utf-8");
  return docs.length;
}

async function exportCollectionPaged(manager, collection, outFile) {
  const db = manager.database;
  const rows = await tryRunCommands(db, collection);
  if (rows && rows.length) {
    fs.writeFileSync(outFile, JSON.stringify(rows, null, 2), "utf-8");
    return rows.length;
  }
  return exportViaCos(manager, collection, outFile);
}

async function main() {
  loadEnv();
  const { only, method } = parseArgs();
  const CloudBase = require("@cloudbase/manager-node");
  const manager = CloudBase.init({
    secretId: process.env.TCB_SECRET_ID,
    secretKey: process.env.TCB_SECRET_KEY,
    envId: process.env.TCB_ENV_ID,
  });

  const stamp = new Date().toISOString().slice(0, 10);
  const outDir = path.join(__dirname, "..", "..", "backup", "tcb-export", stamp);
  fs.mkdirSync(outDir, { recursive: true });
  console.log(`\nManager 导出 → ${outDir}\n`);

  const list = await manager.database.listCollections();
  const counts = {};
  for (const c of list.Collections || []) counts[c.CollectionName] = c.Count;

  let total = 0;
  for (const name of only) {
    const n = counts[name];
    if (n === 0) {
      console.log(`  skip ${name} (空集合)`);
      continue;
    }
    if (n === undefined) {
      console.log(`  skip ${name} (不存在)`);
      continue;
    }
    const outFile = path.join(outDir, `${name}.json`);
    try {
      let count = 0;
      if (method === "export") {
        count = await exportViaCos(manager, name, outFile);
      } else if (method === "commands") {
        const rows = await tryRunCommands(manager.database, name);
        if (!rows) throw new Error("RunCommands 无数据");
        fs.writeFileSync(outFile, JSON.stringify(rows, null, 2), "utf-8");
        count = rows.length;
      } else {
        count = await exportCollectionPaged(manager, name, outFile);
      }
      console.log(`  ${name}: ${count} docs (cloud count ${n})`);
      total += count;
    } catch (e) {
      console.error(`  ${name} FAILED:`, e.message || e);
    }
    await sleep(2000);
  }

  fs.writeFileSync(
    path.join(outDir, "_meta.json"),
    JSON.stringify({ exportedAt: new Date().toISOString(), method: "manager-node", totalDocs: total, collections: only }, null, 2),
  );
  console.log(`\n完成，共 ${total} 条\n`);
  if (total === 0) process.exit(1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
