// ============================================================
// VIP 配置端到端测试（PostgreSQL + HTTP /vip_config）
//   1) 写入 vip_sku_config override
//   2) POST /vip_config 验证 override 被读取
//   3) 还原 override
//
// 使用：node tools/test_vip_config_e2e.js
// 依赖：DATABASE_URL 或 POSTGRES_PASSWORD + pocketbase/.env
// ============================================================

const fs = require("fs");
const path = require("path");
const https = require("https");
const { loadBaseUrl } = require("./_loadBaseUrl");

require("../shared/db/install-shim")();

const TEST_SKU = "VIP_NORMAL";
const TEST_PRICE = 88.88;
const TEST_DAILY = 33;
const TEST_BONUS = 555;

function resolveDatabaseUrl() {
  if (process.env.DATABASE_URL) return process.env.DATABASE_URL;
  const pbEnv = path.join(__dirname, "..", "..", "pocketbase", ".env");
  if (fs.existsSync(pbEnv)) {
    const m = fs.readFileSync(pbEnv, "utf-8").match(/POSTGRES_PASSWORD=(.+)/);
    if (m) {
      const pass = m[1].trim().replace(/^["']|["']$/g, "");
      return `postgres://funlife:${pass}@127.0.0.1:5432/funlife_vip`;
    }
  }
  throw new Error("缺少 DATABASE_URL / POSTGRES_PASSWORD");
}

function postVipConfig(baseUrl) {
  return new Promise((resolve, reject) => {
    const data = "{}";
    const url = new URL(baseUrl + "/vip_config");
    const req = https.request(
      {
        hostname: url.hostname,
        port: url.port || 443,
        path: url.pathname,
        method: "POST",
        headers: { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) },
        timeout: 15000,
      },
      (res) => {
        let buf = "";
        res.on("data", (c) => {
          buf += c;
        });
        res.on("end", () => {
          try {
            resolve(JSON.parse(buf));
          } catch (e) {
            reject(new Error("parse: " + buf.slice(0, 200)));
          }
        });
      },
    );
    req.on("error", reject);
    req.write(data);
    req.end();
  });
}

async function main() {
  process.env.DATABASE_URL = resolveDatabaseUrl();
  require("../shared/db/install-shim")();

  const cloudbase = require("@cloudbase/node-sdk");
  const db = cloudbase.init({}).database();
  const SKU_CFG = db.collection("vip_sku_config");
  const baseUrl = loadBaseUrl();

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
    const r0 = await SKU_CFG.doc(TEST_SKU).get();
    if (r0.data && r0.data[0]) {
      originalDoc = { ...r0.data[0] };
      console.log("· 备份原 override：", JSON.stringify(originalDoc));
    } else {
      console.log("· 当前无 override，将测试新增 → 删除流程");
    }

    console.log(`\n[1/4] 写入测试 override price=${TEST_PRICE} daily=${TEST_DAILY} bonus=${TEST_BONUS}`);
    await SKU_CFG.doc(TEST_SKU).set({
      skuCode: TEST_SKU,
      price: TEST_PRICE,
      dailyCoins: TEST_DAILY,
      bonusCoins: TEST_BONUS,
      updatedAt: new Date().toISOString(),
      updatedBy: "e2e_test",
    });
    console.log("✓ 写入成功");

    console.log("\n[2/4] POST /vip_config 验证读取...");
    const r = await postVipConfig(baseUrl);
    if (!r || !r.ok) throw new Error("vip_config 返回失败：" + JSON.stringify(r));
    const lvl1 = r.data.vipLevels["1"];
    console.log("vipLevel=1 返回：", JSON.stringify(lvl1));
    if (lvl1.price !== TEST_PRICE) throw new Error(`price 不一致 期望=${TEST_PRICE} 实际=${lvl1.price}`);
    if (lvl1.dailyCoins !== TEST_DAILY) throw new Error(`dailyCoins 不一致 期望=${TEST_DAILY} 实际=${lvl1.dailyCoins}`);
    if (lvl1.bonusCoins !== TEST_BONUS) throw new Error(`bonusCoins 不一致 期望=${TEST_BONUS} 实际=${lvl1.bonusCoins}`);
    console.log("✓ vip_config 正确反映 override");

    console.log("\n[3/4] 验证 DB 直读...");
    const rd = await SKU_CFG.doc(TEST_SKU).get();
    const doc = rd.data && rd.data[0];
    if (!doc || Number(doc.bonusCoins) !== TEST_BONUS) {
      throw new Error("DB 读取异常：" + JSON.stringify(doc));
    }
    console.log("✓ DB bonusCoins =", doc.bonusCoins);

    console.log("\n[4/4] 还原 override...");
    await cleanup();

    const r2 = await postVipConfig(baseUrl);
    const lvl1b = r2.data.vipLevels["1"];
    console.log("还原后 vipLevel=1：", JSON.stringify(lvl1b));
    if (!originalDoc && lvl1b.price === TEST_PRICE) {
      throw new Error("还原后仍是测试值，可能未删除成功");
    }
    console.log("\n🎉 端到端测试全部通过！");
  } catch (e) {
    console.error("\n❌ 测试失败：", e.message);
    await cleanup();
    process.exit(1);
  } finally {
    const { closePool } = require("../shared/db/postgres");
    await closePool().catch(() => {});
  }
}

main();
