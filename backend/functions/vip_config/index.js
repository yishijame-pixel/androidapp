// ============================================================
// vip_config 云函数
// ------------------------------------------------------------
// 客户端启动时调用此函数拉取 VIP 运行时配置。
// 主要内容：每个 vipLevel 的 dailyCoins / 时长 / 名称 等可调字段。
//
// 入参：无
// 出参：
//   {
//     ok: true,
//     data: {
//       version: "<updatedAt 的 max 时间戳>",
//       vipLevels: {
//         "1": { dailyCoins: 30, name: "月卡 VIP", durationDays: 30, bonusCoins: 50 },
//         "2": { dailyCoins: 80, ... },
//         "3": { dailyCoins: 200, ... }
//       }
//     }
//   }
//
// 实现策略：
//   - 读取 vip_sku_config 集合的所有 doc（max 50 条）
//   - 按 vipLevel 索引返回；缺失字段使用 sku.js 默认值兜底
//   - 集合不存在 / 读失败 → 返回硬编码默认（不阻塞客户端）
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const SKU = require("./sku");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const SKU_CFG = db.collection("vip_sku_config");

// 默认每日金币（与客户端 VipLevel.kt 默认值同步）
const DEFAULT_DAILY_COINS = { 1: 30, 2: 80, 3: 200, 99: 200 };

exports.main = async (event) => {
  try {
    // 1. 读 override
    const overrides = {};
    try {
      const r = await SKU_CFG.limit(50).get();
      (r.data || []).forEach((d) => {
        overrides[d._id || d.skuCode] = d;
      });
    } catch (e) {
      // 集合不存在 → 走默认
    }

    // 2. 按 vipLevel 聚合
    const vipLevels = {};
    let maxUpdated = 0;
    for (const [code, def] of Object.entries(SKU)) {
      if (def.type !== "vip") continue;
      const o = overrides[code] || {};
      const lvl = String(def.vipLevel);
      vipLevels[lvl] = {
        skuCode: code,
        name: o.name ?? def.name,
        durationDays: (o.durationDays !== undefined && o.durationDays !== null)
          ? Number(o.durationDays) : def.durationDays,
        bonusCoins: (o.bonusCoins !== undefined && o.bonusCoins !== null)
          ? Number(o.bonusCoins) : def.bonusCoins,
        dailyCoins: (o.dailyCoins !== undefined && o.dailyCoins !== null)
          ? Number(o.dailyCoins) : (DEFAULT_DAILY_COINS[def.vipLevel] || 0),
        price: o.price ?? def.price,
      };
      if (o.updatedAt) {
        const t = new Date(o.updatedAt).getTime();
        if (t > maxUpdated) maxUpdated = t;
      }
    }

    // 终身=PERMANENT(99) 与 VIP3(3) 同档（兼容旧用户）
    if (vipLevels["3"]) {
      vipLevels["99"] = { ...vipLevels["3"] };
    }

    return {
      ok: true,
      data: {
        version: maxUpdated || 0,
        vipLevels,
      },
    };
  } catch (e) {
    console.error("vip_config error", e);
    // 全失败时返回纯默认
    const fallback = {};
    for (const [code, def] of Object.entries(SKU)) {
      if (def.type !== "vip") continue;
      fallback[String(def.vipLevel)] = {
        skuCode: code,
        name: def.name,
        durationDays: def.durationDays,
        bonusCoins: def.bonusCoins,
        dailyCoins: DEFAULT_DAILY_COINS[def.vipLevel] || 0,
        price: def.price,
      };
    }
    if (fallback["3"]) fallback["99"] = { ...fallback["3"] };
    return { ok: true, data: { version: 0, vipLevels: fallback } };
  }
};
