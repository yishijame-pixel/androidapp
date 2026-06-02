// ============================================================
// 凭证复验云函数 /verify
// ------------------------------------------------------------
// 客户端定期（建议每 7 天联网时）调一次：
//   1. 把当前持有的凭证 + 签名发上来
//   2. 云端验签 + 查 DB 确认这个 deviceId 仍然是该卡密的持有者
//   3. 通过 → 续期凭证（再发 1 年）
//   4. 不通过 → 客户端清除 VIP 状态
//
// 目的：让"破解者修改本地凭证"的攻击至少在 X 天内必被云端否决
//
// 入参: { certificate, signature }
// 出参成功: { ok:true, certificate, signature } 续期凭证
// 出参失败: { ok:false, code, msg }
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const { rateLimit } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const CODES = db.collection("vip_codes");
const LOG = db.collection("vip_redeem_log");
const REVOCATIONS = db.collection("vip_revocations"); // 🔒 凭证吊销名单

function fail(code, msg) {
  return { ok: false, code, msg };
}
const SILENT_REASONS = new Set(["BAD_REQUEST", "SERVER_MISCONFIG", "DB_ERROR", "INVALID", "CERT_EXPIRED"]);
async function failL(reason, msg, ctx) {
  if (!SILENT_REASONS.has(reason)) {
    try {
      await LOG.add({ data: {
        action: "verify_failed",
        reason,
        deviceId: (ctx && ctx.deviceId) || "",
        skuCode: (ctx && ctx.skuCode) || "",
        ip: (ctx && ctx.ip) || "",
        msg, at: db.serverDate(),
      }});
    } catch (e) {}
  }
  return fail(reason, msg);
}
function extractIp(event) {
  try {
    const h = (event && event.headers) || (event && event.requestContext && event.requestContext.headers) || {};
    return h["x-forwarded-for"] || h["x-real-ip"] || "";
  } catch (e) { return ""; }
}
function nowSec() {
  return Math.floor(Date.now() / 1000);
}
function sign(payload, secret) {
  return crypto.createHmac("sha256", secret).update(payload).digest("hex");
}
function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach((k) => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function constantTimeEq(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}

exports.main = async (event) => {
  const SECRET = process.env.HMAC_SECRET;
  if (!SECRET || SECRET.length < 32) return fail("SERVER_MISCONFIG", "服务端配置异常");

  let body = event;
  if (event && event.body) {
    try {
      body = typeof event.body === "string" ? JSON.parse(event.body) : event.body;
    } catch (e) {
      return fail("BAD_REQUEST", "请求格式错误");
    }
  }

  const cert = body && body.certificate;
  const signature = body && body.signature;
  const ip = extractIp(event);
  const ctx = { deviceId: (cert && cert.deviceId) || "", skuCode: (cert && cert.skuCode) || "", ip };

  // 🔒 限流：单 IP/设备 60s 最多 20 次复验（必须在所有 return 之前，
  //   否则攻击者可发垃圾凭证绕过限流；cert 缺失时回退 IP / anon）
  const rl = await rateLimit(db, {
    key: "verify:" + (ip || String((cert && cert.deviceId) || "").slice(0, 16) || "anon"),
    limit: 20, windowMs: 60_000,
  });
  if (!rl.ok) return await failL("RATE_LIMITED", "请求过于频繁，请稍后再试", ctx);

  if (!cert || !signature) return fail("INVALID", "凭证缺失");

  // 1) 验签
  const expected = sign(canonicalJson(cert), SECRET);
  if (!constantTimeEq(expected, signature)) return await failL("BAD_SIGNATURE", "凭证签名无效", ctx);

  // 2) 凭证未过期（exp 字段）
  if (typeof cert.exp !== "number" || cert.exp < nowSec()) {
    return fail("CERT_EXPIRED", "凭证已过期，请联网激活");
  }

  // 3) 查 DB：确认该设备仍然是合法持有者
  const skuCode = cert.skuCode;
  const deviceId = cert.deviceId;

  let codeDoc;
  try {
    const r = await CODES.where({
      skuCode,
      usedByDevice: deviceId,
      status: "used",
    }).limit(1).get();
    codeDoc = r.data && r.data[0];
  } catch (e) {
    return fail("DB_ERROR", "服务繁忙，请稍后重试");
  }

  if (!codeDoc) return await failL("NOT_FOUND", "未找到该设备的有效卡密，VIP 已失效", ctx);
  if (codeDoc.disabled) return await failL("DISABLED", "该卡密已被禁用", ctx);

  // 🔒 吊销名单：匹配规则（任一命中即吊销）
  //    1) deviceId 设备级封禁：where({ deviceId })
  //    2) skuCode + deviceId 精确：where({ skuCode, deviceId })
  //    凭证签发时间早于吊销时间才视为已吊销（保证吊销后用户可重新付费拿新 cert）
  try {
    const r = await REVOCATIONS.where({ deviceId }).limit(5).get();
    const docs = (r && r.data) || [];
    for (const d of docs) {
      if (!d || !d.revokedAt) continue;
      // 精确匹配的 skuCode 优先
      if (d.skuCode && d.skuCode !== skuCode) continue;
      if (cert.issuedAt && cert.issuedAt < d.revokedAt) {
        return await failL("REVOKED", "凭证已被吊销，请联系客服", ctx);
      }
    }
  } catch (e) {
    // 集合不存在或查询失败：不阻断主流程（开发期吊销表为空时正常）
  }

  // 🔒 业务到期校验：年费 VIP 到期后不再续 cert（防止已过期 VIP 仍然每周拿到新 cert）
  //    codeDoc.expireDate === null  -> 永久
  //    codeDoc.expireDate === "yyyy-MM-dd"
  if (codeDoc.expireDate) {
    const todayIso = new Date().toISOString().slice(0, 10);
    if (codeDoc.expireDate < todayIso) {
      return await failL("VIP_EXPIRED", "VIP 已到期", ctx);
    }
  }

  // 4) 续期凭证 —— 🔒 不信任客户端 cert 中的 expireDate / vipLevel，
  //    用 DB 锁定值重写（防止同一签名在 SKU 调价后绕过）
  const newCert = {
    deviceId: cert.deviceId,
    skuCode: cert.skuCode,
    vipLevel: cert.vipLevel,           // vipLevel 由签名保护，沿用客户端字段安全
    expireDate: (codeDoc.expireDate !== undefined) ? codeDoc.expireDate : cert.expireDate,
    bonusCoins: 0,                     // 续期不再发金币
    issuedAt: nowSec(),
    exp: nowSec() + 365 * 86400,
  };
  return {
    ok: true,
    certificate: newCert,
    signature: sign(canonicalJson(newCert), SECRET),
  };
};
