// ============================================================
// 卡密销售情况查询
// ------------------------------------------------------------
// 用法：
//   node admin_query.js                    # 总览所有 SKU
//   node admin_query.js batch <批次名>      # 按批次查
//   node admin_query.js code <兑换码>       # 查单个卡密
// ============================================================

const { initTcb } = require("./_loadEnv");

const app = initTcb();
const db = app.database();
const _ = db.command;
const CODES = db.collection("vip_codes");

function normalize(code) {
  return (code || "").replace(/[\s\-_]/g, "").toUpperCase();
}

async function summary() {
  const skus = ["VIP_NORMAL", "VIP_YEAR", "VIP_LIFETIME"];
  console.log("\n========== 卡密销售总览 ==========\n");
  console.log("SKU             总数    未用    已用    禁用");
  console.log("---------------------------------------------");

  for (const sku of skus) {
    const all = await CODES.where({ skuCode: sku }).count();
    const unused = await CODES.where({ skuCode: sku, status: "unused", disabled: _.neq(true) }).count();
    const used = await CODES.where({ skuCode: sku, status: "used" }).count();
    const disabled = await CODES.where({ skuCode: sku, disabled: true }).count();
    console.log(
      `${sku.padEnd(15)} ${String(all.total).padStart(5)}   ${String(unused.total).padStart(5)}   ${String(used.total).padStart(5)}   ${String(disabled.total).padStart(5)}`
    );
  }
  console.log();
}

async function byBatch(batch) {
  const r = await CODES.where({ batch }).limit(1000).get();
  const items = r.data || [];
  console.log(`\n批次 [${batch}]：共 ${items.length} 条`);
  let unused = 0, used = 0, disabled = 0;
  items.forEach((x) => {
    if (x.disabled) disabled++;
    else if (x.status === "used") used++;
    else unused++;
  });
  console.log(`未使用 ${unused}，已使用 ${used}，已禁用 ${disabled}\n`);
}

async function byCode(input) {
  const code = normalize(input);
  const r = await CODES.where({ code }).limit(1).get();
  if (!r.data || !r.data[0]) {
    console.log("未找到该卡密");
    return;
  }
  const x = r.data[0];
  console.log("\n========== 卡密详情 ==========");
  console.log("code:        ", x.code);
  console.log("skuCode:     ", x.skuCode);
  console.log("status:      ", x.status, x.disabled ? "(已禁用)" : "");
  console.log("batch:       ", x.batch);
  console.log("createdAt:   ", x.createdAt);
  if (x.usedByDevice) {
    console.log("usedByDevice:", x.usedByDevice.slice(0, 16) + "...");
    console.log("usedAt:      ", x.usedAt);
    console.log("migrateCount:", x.migrateCount || 0);
  }
  console.log();
}

async function main() {
  const [, , cmd, arg] = process.argv;
  if (!cmd) await summary();
  else if (cmd === "batch" && arg) await byBatch(arg);
  else if (cmd === "code" && arg) await byCode(arg);
  else {
    console.log("用法:");
    console.log("  node admin_query.js                总览");
    console.log("  node admin_query.js batch <批次>   按批次");
    console.log("  node admin_query.js code <卡密>    查单个");
  }
}

main().catch((e) => {
  console.error("查询失败:", e);
  process.exit(1);
});
