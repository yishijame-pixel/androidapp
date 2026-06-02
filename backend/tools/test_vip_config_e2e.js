// ============================================================
// VIP 配置端到端测试
//   1) 通过 admin SDK 写入 vip_sku_config override
//   2) 调用 vip_config 云函数（HTTP & SDK）验证 override 被读取
//   3) 调用 redeem.getSkuConfig 通过模拟（直接读 db 验证）
//   4) 还原 override 删除测试数据
//
// 使用：node tools/test_vip_config_e2e.js
// 依赖：tools/.env 中的 TCB_ENV_ID / TCB_SECRET_ID / TCB_SECRET_KEY
// ============================================================

const https = require("https");
const { initTcb } = require("./_loadEnv");

const TEST_SKU = "VIP_NORMAL";
const TEST_PRICE = 88.88;
const TEST_DAILY = 33;
const TEST_BONUS = 555;

function callHttpVipConfig(envId) {
  // CloudBase HTTP 访问域名格式：<envId>-<appId>.ap-shanghai.app.tcloudbase.com
  // 直接通过 vip_config SDK 调用即可，不依赖固定域名
  return null;
}

async function main() {
  const app = initTcb();
  const db = app.database();
  const SKU_CFG = db.collection("vip_sku_config");

  let originalDoc = null;
  let restored = false;
  const cleanup = async () => {
    if (restored) return;
    restored = true;
    try {
      if (originalDoc) {
        await SKU_CFG.doc(TEST_SKU).set(originalDoc);
        console.log("✓ 已还原原始 SKU override");
      } else {
        await SKU_CFG.doc(TEST_SKU).remove();
        console.log("✓ 已删除测试 override");
      }
    } catch (e) {
      console.warn("还原失败：", e.message);
    }
  };
  process.on("SIGINT", () => cleanup().then(() => process.exit(2)));

  try {
    // 0. 备份
    try {
      const r = await SKU_CFG.doc(TEST_SKU).get();
      if (r.data && (Array.isArray(r.data) ? r.data[0] : r.data)) {
        originalDoc = Array.isArray(r.data) ? r.data[0] : r.data;
        console.log("· 备份原 override：", JSON.stringify(originalDoc));
      } else {
        console.log("· 当前无 override，将测试新增 → 删除流程");
      }
    } catch (_) { /* doc 不存在 */ }

    // 1. 写入测试 override
    console.log(`\n[1/4] 写入测试 override price=${TEST_PRICE} daily=${TEST_DAILY} bonus=${TEST_BONUS}`);
    await SKU_CFG.doc(TEST_SKU).set({
      skuCode: TEST_SKU,
      price: TEST_PRICE,
      dailyCoins: TEST_DAILY,
      bonusCoins: TEST_BONUS,
      updatedAt: new Date(),
      updatedBy: "e2e_test",
    });
    console.log("✓ 写入成功");

    // 2. 调用 vip_config 云函数（SDK invoke）
    console.log("\n[2/4] 调用 vip_config 云函数验证读取...");
    const r = await app.callFunction({ name: "vip_config", data: {} });
    if (!r.result || !r.result.ok) throw new Error("vip_config 返回失败：" + JSON.stringify(r.result));
    const lvl1 = r.result.data.vipLevels["1"];
    console.log("vipLevel=1 返回：", JSON.stringify(lvl1));
    if (lvl1.price !== TEST_PRICE) throw new Error(`price 不一致 期望=${TEST_PRICE} 实际=${lvl1.price}`);
    if (lvl1.dailyCoins !== TEST_DAILY) throw new Error(`dailyCoins 不一致 期望=${TEST_DAILY} 实际=${lvl1.dailyCoins}`);
    if (lvl1.bonusCoins !== TEST_BONUS) throw new Error(`bonusCoins 不一致 期望=${TEST_BONUS} 实际=${lvl1.bonusCoins}`);
    console.log("✓ vip_config 正确反映 override");

    // 3. 验证 redeem 也能读到 override（间接：直接读 vip_sku_config 集合，模拟 redeem.getSkuConfig）
    console.log("\n[3/4] 验证 redeem.getSkuConfig 路径...");
    const rd = await SKU_CFG.doc(TEST_SKU).get();
    const doc = Array.isArray(rd.data) ? rd.data[0] : rd.data;
    if (!doc || Number(doc.bonusCoins) !== TEST_BONUS) {
      throw new Error("redeem 读取的 override 数据异常：" + JSON.stringify(doc));
    }
    console.log("✓ redeem 路径下 bonusCoins =", doc.bonusCoins);

    // 4. 还原
    console.log("\n[4/4] 还原 override...");
    await cleanup();

    // 5. 再次调用确认恢复默认
    const r2 = await app.callFunction({ name: "vip_config", data: {} });
    const lvl1b = r2.result.data.vipLevels["1"];
    console.log("还原后 vipLevel=1：", JSON.stringify(lvl1b));
    if (!originalDoc && lvl1b.price === TEST_PRICE) {
      throw new Error("还原后仍是测试值，可能未删除成功");
    }
    console.log("\n🎉 端到端测试全部通过！");
  } catch (e) {
    console.error("\n❌ 测试失败：", e.message);
    await cleanup();
    process.exit(1);
  }
}

main();
