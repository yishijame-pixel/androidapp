// ============================================================
// 禁用 / 启用卡密
// ------------------------------------------------------------
// 用户退款时把对应卡密 disable 掉：
//   - 未兑换 → 直接作废
//   - 已兑换 → 该设备下次 verify 时被拒，VIP 失效
//
// 用法：
//   node admin_disable.js <兑换码>          # 禁用
//   node admin_disable.js <兑换码> enable   # 重新启用
// ============================================================

const { initTcb } = require("./_loadEnv");

const app = initTcb();
const db = app.database();
const CODES = db.collection("vip_codes");

function normalize(code) {
  return (code || "").replace(/[\s\-_]/g, "").toUpperCase();
}

async function main() {
  const [, , inputCode, action] = process.argv;
  if (!inputCode) {
    console.log("用法:");
    console.log("  node admin_disable.js <兑换码>          禁用");
    console.log("  node admin_disable.js <兑换码> enable   启用");
    process.exit(1);
  }

  const code = normalize(inputCode);
  const disabled = action !== "enable";

  const r = await CODES.where({ code }).update({
    disabled,
    disabledAt: disabled ? db.serverDate() : null,
  });
  console.log(`已${disabled ? "禁用" : "启用"} ${code}，影响 ${r.updated || 0} 条记录`);
}

main().catch((e) => {
  console.error("操作失败:", e);
  process.exit(1);
});
