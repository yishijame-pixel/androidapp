#!/usr/bin/env node
/**
 * 通过 Manager RunCommands (FlexDB QUERY) 导出，格式同 tcb db nosql execute
 */
const fs = require("fs");
const path = require("path");
const { loadEnv } = require("./_loadEnv");

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function buildQuery(collection, skip, limit) {
  return [
    {
      TableName: collection,
      CommandType: "QUERY",
      Command: JSON.stringify({ find: collection, skip, limit }),
    },
  ];
}

async function queryPage(db, collection, skip, limit) {
  const payload = buildQuery(collection, skip, limit);
  const r = await db.runCommands({ MgoCommands: payload });
  if (r.Error) throw new Error(r.Error.Message || JSON.stringify(r.Error));

  let data = r.Data;
  if (typeof data === "string") data = JSON.parse(data);
  if (Array.isArray(data) && typeof data[0] === "string") {
    data = data.map((s) => JSON.parse(s));
  }

  // 响应形态: [{ Cursor: { FirstBatch: [...] } }] 或 { documents: [] }
  const first = Array.isArray(data) ? data[0] : data;
  if (!first) return [];

  if (first.Cursor && first.Cursor.FirstBatch) return first.Cursor.FirstBatch;
  if (first.Documents) return first.Documents;
  if (first.documents) return first.documents;
  if (Array.isArray(first)) return first;
  if (first.data) return first.data;

  const text = JSON.stringify(first);
  if (/EXCEED|LimitExceeded|quota/i.test(text)) {
    throw new Error("EXCEED_REQUEST_LIMIT");
  }
  console.log("  unknown shape:", text.slice(0, 300));
  return [];
}

async function exportCollection(db, collection, outDir) {
  const all = [];
  const pageSize = 20;
  let skip = 0;
  let attempt = 0;

  while (true) {
    try {
      const batch = await queryPage(db, collection, skip, pageSize);
      if (!batch.length) break;
      all.push(...batch);
      skip += batch.length;
      attempt = 0;
      console.log(`  ${collection}: +${batch.length} (total ${all.length})`);
      if (batch.length < pageSize) break;
      await sleep(800);
    } catch (e) {
      if (/EXCEED|LimitExceeded|quota/i.test(e.message || "")) {
        attempt++;
        const wait = Math.min(60000, 5000 * attempt);
        console.log(`  ${collection} 限流，${wait}ms 后重试 (${attempt}/12)`);
        if (attempt >= 12) break;
        await sleep(wait);
        continue;
      }
      throw e;
    }
  }

  if (!all.length) return 0;
  const parsed = all.map((row) => (typeof row === "string" ? JSON.parse(row) : row));
  fs.writeFileSync(path.join(outDir, `${collection}.json`), JSON.stringify(parsed, null, 2), "utf-8");
  return parsed.length;
}

async function main() {
  loadEnv();
  const onlyArg = process.argv.find((a) => a.startsWith("--only="));
  const only = onlyArg
    ? onlyArg.slice(7).split(",").map((s) => s.trim())
    : ["vip_codes", "vip_users", "vip_sku_config"];

  const CloudBase = require("@cloudbase/manager-node");
  const manager = CloudBase.init({
    secretId: process.env.TCB_SECRET_ID,
    secretKey: process.env.TCB_SECRET_KEY,
    envId: process.env.TCB_ENV_ID,
  });

  const stamp = new Date().toISOString().slice(0, 10);
  const outDir = path.join(__dirname, "..", "..", "backup", "tcb-export", stamp);
  fs.mkdirSync(outDir, { recursive: true });
  console.log(`\nNoSQL RunCommands 导出 → ${outDir}\n`);

  let total = 0;
  for (const name of only) {
    total += await exportCollection(manager.database, name, outDir);
    await sleep(2000);
  }

  fs.writeFileSync(
    path.join(outDir, "_meta.json"),
    JSON.stringify({ exportedAt: new Date().toISOString(), method: "runCommands", totalDocs: total }, null, 2),
  );
  console.log(`\n完成，共 ${total} 条\n`);
  if (total === 0) process.exit(1);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
