// ============================================================
// 兑换码兑换云函数 /redeem
// ------------------------------------------------------------
// 入参 (POST JSON):
//   { code: "FL-XXXX-XXXX-XXXX", deviceId: "sha512..." }
//
// 出参（成功）:
//   {
//     ok: true,
//     certificate: {
//       deviceId, skuCode, vipLevel,
//       expireDate,   // null=永久，否则 ISO 日期字符串
//       bonusCoins,   // 客户端首次兑换需加上
//       issuedAt,     // 签发时间戳（秒）
//       exp           // 凭证本身过期时间戳（秒，1 年）
//     },
//     signature: "hex hmac-sha256"
//   }
//
// 出参（失败）:
//   { ok: false, code: "INVALID|USED|EXPIRED|DISABLED|UNKNOWN_SKU|...", msg: "..." }
//
// 防并发：使用 db.where({status:'unused'}).update() 的原子语义
//   ↳ 即便两个请求同时到达，只有一个能把 status 改成 'used'，另一个 updated=0
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const SKU = require("./sku");
const { rateLimit } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const _ = db.command;

const CODES = db.collection("vip_codes");
const LOG = db.collection("vip_redeem_log");
const SKU_CFG = db.collection("vip_sku_config");

/**
 * 读取 SKU 的"运行时合并配置"
 *   - 优先读 vip_sku_config 集合中 _id=skuCode 的 override
 *   - 缺失字段回退到 sku.js 硬编码默认
 *   - 集合不存在或读失败时静默降级到默认（不阻塞兑换主流程）
 */
async function getSkuConfig(skuCode) {
  const def = SKU[skuCode];
  if (!def) return null;
  let override = {};
  try {
    const r = await SKU_CFG.doc(skuCode).get();
    if (r.data && r.data[0]) override = r.data[0];
    else if (r.data && !Array.isArray(r.data)) override = r.data;
  } catch (e) {
    // 集合不存在或文档不存在 → 用默认
  }
  return {
    type: def.type,
    name: override.name ?? def.name,
    price: override.price ?? def.price,
    vipLevel: def.vipLevel,                                  // vipLevel 永远以代码为准
    durationDays: (override.durationDays !== undefined && override.durationDays !== null)
      ? Number(override.durationDays) : def.durationDays,
    bonusCoins: (override.bonusCoins !== undefined && override.bonusCoins !== null)
      ? Number(override.bonusCoins) : def.bonusCoins,
  };
}

// ─────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────

function fail(code, msg) {
  return { ok: false, code, msg };
}

// 业务级失败写日志（系统级如 BAD_REQUEST/SERVER_MISCONFIG/DB_ERROR 不写，避免噪音）
const SILENT_REASONS = new Set(["BAD_REQUEST", "SERVER_MISCONFIG", "DB_ERROR"]);
async function failL(reason, msg, ctx) {
  if (!SILENT_REASONS.has(reason)) {
    try {
      await LOG.add({ data: {
        action: "redeem_failed",
        reason,
        code: (ctx && ctx.code) || "",
        deviceId: (ctx && ctx.deviceId) || "",
        ip: (ctx && ctx.ip) || "",
        msg, at: db.serverDate(),
      }});
    } catch (e) {}
  }
  return fail(reason, msg);
}

function nowSec() {
  return Math.floor(Date.now() / 1000);
}

/** 把字符串规范化：去掉分隔符、转大写。让用户输入 fl-9f2l-... 也能匹配 */
function normalizeCode(input) {
  if (!input || typeof input !== "string") return "";
  return input.trim().toUpperCase().replace(/[\s\-_]/g, "");
}

/** 计算 HMAC-SHA256 签名（hex 字符串） */
function sign(payloadJson, secret) {
  return crypto
    .createHmac("sha256", secret)
    .update(payloadJson)
    .digest("hex");
}

/** 按字段名字母排序输出 JSON，与 Android Gson 默认行为对齐 */
function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach((k) => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}

/** 计算到期日期：永久返回 null，否则 ISO yyyy-MM-dd */
function calcExpireDate(durationDays) {
  if (durationDays < 0) return null;
  const d = new Date();
  d.setDate(d.getDate() + durationDays);
  return d.toISOString().slice(0, 10);
}

// ─────────────────────────────────────────────
// 主入口
// ─────────────────────────────────────────────

exports.main = async (event /* , context */) => {
  const SECRET = process.env.HMAC_SECRET;
  if (!SECRET || SECRET.length < 32) {
    console.error("HMAC_SECRET 未配置或太短");
    return fail("SERVER_MISCONFIG", "服务端配置异常，请联系客服");
  }

  // 1) 解析请求体（支持 HTTP 触发器和 SDK 直接调用两种入口）
  let body = event;
  if (event && event.body) {
    try {
      body = typeof event.body === "string" ? JSON.parse(event.body) : event.body;
    } catch (e) {
      return fail("BAD_REQUEST", "请求格式错误");
    }
  }

  const code = normalizeCode(body && body.code);
  const deviceId = (body && body.deviceId) || "";
  // 🔒 用户身份：客户端必须传当前登录 userId，把卡密绑定到账号
  // 兼容旧客户端：未传 userId（=0/未定义）时仅做设备级幂等（不写 usedByUser）
  const rawUid = body && body.userId;
  const userId = (typeof rawUid === "number" && rawUid > 0) ? Math.floor(rawUid)
               : (typeof rawUid === "string" && /^\d+$/.test(rawUid)) ? parseInt(rawUid, 10)
               : 0;
  const ip = extractIp(event);
  const ctx = { code, deviceId, ip };

  // 🔒 限流：单 IP/设备 60s 内最多 10 次兑换尝试（必须在所有 return 之前，
  //   否则攻击者可发垃圾格式请求绕过限流轰炸日志/枚举卡密）
  const rl = await rateLimit(db, {
    key: "redeem:" + (ip || (deviceId && deviceId.slice(0, 16)) || "anon"),
    limit: 10, windowMs: 60_000,
  });
  if (!rl.ok) return await failL("RATE_LIMITED", "请求过于频繁，请稍后再试", ctx);

  if (!code || code.length < 8) return await failL("INVALID", "兑换码格式错误", ctx);
  if (!deviceId || deviceId.length < 16) return await failL("INVALID", "设备指纹缺失", ctx);

  // 2) 查询卡密
  let codeDoc;
  try {
    const res = await CODES.where({ code }).limit(1).get();
    codeDoc = res.data && res.data[0];
  } catch (e) {
    console.error("查询卡密失败", e);
    return fail("DB_ERROR", "服务繁忙，请稍后重试");
  }

  if (!codeDoc) return await failL("INVALID", "兑换码不存在", ctx);
  if (codeDoc.disabled) return await failL("DISABLED", "兑换码已被禁用", ctx);

  // 3) 如果已被该设备使用 → 幂等返回凭证（用户重装/重启可以重新拿到）
  if (codeDoc.status === "used") {
    if (codeDoc.usedByDevice !== deviceId) {
      return await failL("USED", "兑换码已被其他设备使用", ctx);
    }
    // 🔒 同设备多账号防白嫖：如果首激时已绑定 userId，本次必须同账号
    //    兼容旧记录：codeDoc.usedByUser 不存在时跳过此校验
    if (codeDoc.usedByUser != null && codeDoc.usedByUser !== 0) {
      if (userId === 0) {
        return await failL("USER_REQUIRED",
          "请先登录账号后再兑换", { ...ctx, deviceId });
      }
      if (Number(codeDoc.usedByUser) !== userId) {
        return await failL("USER_MISMATCH",
          "兑换码已绑定其他账号，无法在本账号下复用", { ...ctx, deviceId });
      }
    } else if (userId > 0) {
      // 旧记录补录 usedByUser（首次以 reissue 形式回填）
      try {
        await CODES.where({ code, status: "used", usedByDevice: deviceId })
          .update({ usedByUser: userId });
      } catch (e) { /* 忽略，不影响主流程 */ }
    }
    return await buildCertResponse(codeDoc, deviceId, SECRET, /* isReissue */ true /* sku */);
  }

  // 4) SKU 校验（运行时配置：优先 vip_sku_config，回退 sku.js）
  const sku = await getSkuConfig(codeDoc.skuCode);
  if (!sku) return await failL("UNKNOWN_SKU", "未知商品类型，请联系客服", ctx);
  if (sku.type && sku.type !== "vip") {
    return await failL("WRONG_TYPE", "此卡密不可在 VIP 兑换处使用", ctx);
  }

  // 5) 原子标记 used（防并发关键步骤）
  //    🔒 同时锁定 expireDate（首次兑换时计算并存库），避免重装/迁移续命
  const lockedExpireDate = calcExpireDate(sku.durationDays);
  let updated = 0;
  try {
    const r = await CODES.where({ code, status: "unused" }).update({
      status: "used",
      usedByDevice: deviceId,
      usedByUser: userId, // 🔒 0=未登录（兼容游客），>0=绑定账号
      usedAt: db.serverDate(),
      expireDate: lockedExpireDate, // null = 永久
    });
    updated = r.updated || 0;
  } catch (e) {
    console.error("原子更新失败", e);
    return fail("DB_ERROR", "服务繁忙，请稍后重试");
  }

  if (updated === 0) {
    // 中途被别人抢了
    return await failL("USED", "兑换码刚刚被使用，请检查或联系客服", ctx);
  }

  // 6) 写审计日志（失败不影响主流程）
  try {
    await LOG.add({
      data: {
        code,
        deviceId,
        userId,
        skuCode: codeDoc.skuCode,
        action: "redeem",
        ip,
        at: db.serverDate(),
      },
    });
  } catch (e) {
    console.warn("写日志失败（已忽略）", e.message);
  }

  // 7) 重新读最新文档（拿到 serverDate）
  let finalDoc = codeDoc;
  try {
    const r = await CODES.where({ code }).limit(1).get();
    if (r.data && r.data[0]) finalDoc = r.data[0];
  } catch (e) {
    // 忽略
  }

  // 7·) 复用第 4 步已读取的 sku，避免重复读 vip_sku_config
  return await buildCertResponse(finalDoc, deviceId, SECRET, /* isReissue */ false, sku);
};

// ─────────────────────────────────────────────
// 凭证组装 + 签名
// ─────────────────────────────────────────────
async function buildCertResponse(codeDoc, deviceId, secret, isReissue, preloadedSku) {
  // 优先复用主流程中已读取的 sku，减少一次 DB IO
  const sku = preloadedSku || await getSkuConfig(codeDoc.skuCode);
  if (!sku) return fail("UNKNOWN_SKU", "未知商品类型");

  // 重新签发时不再发放金币（防止用户重装无限领）
  const bonusCoins = isReissue ? 0 : sku.bonusCoins;

  // 🔒 优先用 codeDoc 中锁定的 expireDate（首次兑换时算好的）
  //    旧记录没有此字段时回退到 calc（这意味着旧用户首次重发会续期一次，可接受）
  const expireDate = (codeDoc.expireDate !== undefined)
    ? codeDoc.expireDate
    : calcExpireDate(sku.durationDays);
  const cert = {
    deviceId,
    skuCode: codeDoc.skuCode,
    vipLevel: sku.vipLevel,
    expireDate,
    bonusCoins,
    issuedAt: nowSec(),
    exp: nowSec() + 365 * 86400, // 凭证本身一年有效，到期后需联网重新 verify
  };

  const payloadJson = canonicalJson(cert);
  const signature = sign(payloadJson, secret);

  return {
    ok: true,
    isReissue,
    certificate: cert,
    signature,
  };
}

function extractIp(event) {
  try {
    const h =
      (event && event.headers) ||
      (event && event.requestContext && event.requestContext.headers) ||
      {};
    return h["x-forwarded-for"] || h["x-real-ip"] || "";
  } catch (e) {
    return "";
  }
}
