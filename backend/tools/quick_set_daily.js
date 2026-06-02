// 真机验证用：把指定 SKU 的 dailyCoins 改成给定值（不还原）
//   node tools/quick_set_daily.js VIP_NORMAL 99
//   验证完后再跑：node tools/quick_set_daily.js VIP_NORMAL reset
const { initTcb } = require("./_loadEnv");
const [, , sku, val] = process.argv;
if (!sku || !val) { console.error("用法: node tools/quick_set_daily.js <SKU> <number|reset>"); process.exit(1); }
(async () => {
  const app = initTcb();
  const col = app.database().collection("vip_sku_config");
  if (val === "reset") {
    try { await col.doc(sku).remove(); console.log(`✓ ${sku} 已重置为 sku.js 默认`); }
    catch (e) { console.log(`(已是默认或不存在: ${e.message})`); }
  } else {
    const n = Number(val);
    if (!Number.isFinite(n)) { console.error("数字非法"); process.exit(1); }
    try {
      const u = await col.doc(sku).update({ dailyCoins: n, updatedAt: new Date(), updatedBy: "quick_set" });
      if (!u.updated) throw new Error("doc 不存在");
      console.log(`✓ ${sku}.dailyCoins = ${n}`);
    } catch (_) {
      await col.doc(sku).set({ skuCode: sku, dailyCoins: n, updatedAt: new Date(), updatedBy: "quick_set" });
      console.log(`✓ ${sku}.dailyCoins = ${n} (新建)`);
    }
  }
})();
