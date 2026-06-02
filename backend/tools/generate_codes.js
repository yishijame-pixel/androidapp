// ============================================================
// 卡密批量生成脚本
// ------------------------------------------------------------
// 用法：
//   node generate_codes.js <SKU> <数量> [批次名]
//
// 示例：
//   node generate_codes.js VIP_NORMAL 50 batch_2026Q2
//   node generate_codes.js VIP_LIFETIME 10 vip_lifetime_001
//
// 输出：
//   1) 写入云端 vip_codes 集合
//   2) 在当前目录生成 codes_export_<时间>.csv（你拿这个 CSV 发货）
// ============================================================

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { initTcb } = require("./_loadEnv");
const SKU = require("../shared/sku");

const app = initTcb();
const db = app.database();
const CODES = db.collection("vip_codes");

// 去掉易混字符 0/O/1/I/L
const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

function genCode() {
  // 12 位随机字符 → 拆成 4-4-4 显示
  const buf = crypto.randomBytes(12);
  let s = "";
  for (let i = 0; i < 12; i++) {
    s += ALPHABET[buf[i] % ALPHABET.length];
  }
  return `FL-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}`;
}

/** 数据库里存"标准化"形态（无分隔），方便查询 */
function normalize(code) {
  return code.replace(/[\s\-_]/g, "").toUpperCase();
}

async function main() {
  const [, , skuCode, countStr, batchId] = process.argv;
  if (!skuCode || !countStr) {
    console.log("用法: node generate_codes.js <SKU> <数量> [批次名]");
    console.log("可用 SKU: " + Object.keys(SKU).join(", "));
    process.exit(1);
  }
  if (!SKU[skuCode]) {
    console.error(`未知 SKU: ${skuCode}`);
    console.error("可用 SKU: " + Object.keys(SKU).join(", "));
    process.exit(1);
  }
  const count = parseInt(countStr, 10);
  if (isNaN(count) || count < 1 || count > 5000) {
    console.error("数量必须是 1-5000 的整数");
    process.exit(1);
  }

  const batch = batchId || `batch_${Date.now()}`;
  const sku = SKU[skuCode];

  console.log(`\n开始生成 ${count} 个 [${skuCode}] (${sku.name}, ¥${sku.price}) 卡密`);
  console.log(`批次名：${batch}\n`);

  // 1) 先在内存里生成所有卡密（去重）
  const seen = new Set();
  const items = [];
  while (items.length < count) {
    const display = genCode();
    const code = normalize(display);
    if (seen.has(code)) continue;
    seen.add(code);
    items.push({ display, code });
  }

  // 2) 批量写入云端
  console.log("正在写入云端...");
  let success = 0;
  for (let i = 0; i < items.length; i++) {
    const it = items[i];
    try {
      // 用 code 当文档 _id，避免 add({data:{}}) 在某些 SDK 版本下被嵌套
      await CODES.doc(it.code).set({
        code: it.code,
        skuCode,
        status: "unused",
        batch,
        createdAt: db.serverDate(),
        disabled: false,
        migrateCount: 0,
      });
      success++;
      process.stdout.write(`\r已写入 ${success}/${count}`);
    } catch (e) {
      console.error(`\n写入失败 ${it.display}:`, e.message);
    }
  }
  console.log("\n\n");

  // 3) 导出 CSV
  const ts = new Date().toISOString().replace(/[:.]/g, "-").slice(0, 19);
  const csvPath = path.join(__dirname, `codes_export_${batch}_${ts}.csv`);
  const header = "兑换码,SKU,商品名称,价格,批次,生成时间\n";
  const rows = items
    .map(
      (it) =>
        `${it.display},${skuCode},"${sku.name}",${sku.price},${batch},${new Date().toISOString()}`
    )
    .join("\n");
  fs.writeFileSync(csvPath, header + rows, "utf-8");

  console.log(`✅ 完成`);
  console.log(`   云端写入：${success} / ${count}`);
  console.log(`   CSV 文件：${csvPath}`);
  console.log(`   ⚠️  CSV 是发货底稿，妥善保管！\n`);
}

main().catch((e) => {
  console.error("生成失败：", e);
  process.exit(1);
});
