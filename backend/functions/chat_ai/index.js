// ============================================================
// 聊天记账 AI 代理云函数 /chat_ai
// ------------------------------------------------------------
// 与 letter_ai 同套安全协议，区别：
//   - 配额按"日"扣减（chat_ai_quota collection key=device_yyyymmdd）
//   - 配额 v2：tier 0=无权益 1=体验池 2/3/4=日+月上限（见 chat_ai_limits.js）
//   - 输入：persona（人格 id 或 system 全文）+ user 文本
//   - 不做幂等（聊天每条都算独立调用），重试由客户端控制
//
// 入参：{
//   certificate, signature,
//   body: {
//     mode,                      // "bill" | "chat"  仅用于做日志区分
//     personaSystem,             // 客户端拼好的 system prompt（已 sanitize）
//     userText,                  // 用户文本（≤ 2000）
//     extra?: { monthTotal, categoryCount, billCategory, billAmount }
//   }
// }
// 出参成功：{ ok:true, reply, used, limit, vipLevel }
// 出参失败：{ ok:false, code, msg }
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const https = require("https");
const { URL } = require("url");
const SKU = require("./sku");
const LIMITS = require("./chat_ai_limits");
const { rateLimit, extractIp } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const _ = db.command;
const CODES = db.collection("vip_codes");
const REVOCATIONS = db.collection("vip_revocations");
// 按 mode 分桶：chat 与 book / reader_dna 配额完全独立
const QUOTA_BY_MODE = {
  bill:       db.collection("chat_ai_quota"),
  chat:       db.collection("chat_ai_quota"),
  book:       db.collection("chat_ai_book_quota"),
  reader_dna: db.collection("chat_ai_dna_quota"),
};
function quotaColOf(mode) { return QUOTA_BY_MODE[mode] || QUOTA_BY_MODE.chat; }
function quotaColNameOf(mode) {
  if (mode === "book") return "chat_ai_book_quota";
  if (mode === "reader_dna") return "chat_ai_dna_quota";
  return "chat_ai_quota";
}
const LOG = db.collection("vip_redeem_log");
const QUOTA_MONTH = db.collection("chat_ai_quota_month");
const TRIAL_POOL = db.collection("chat_ai_trial");

function fail(code, msg) { return { ok: false, code, msg }; }
function nowSec() { return Math.floor(Date.now() / 1000); }
function sign(payload, secret) {
  return crypto.createHmac("sha256", secret).update(payload).digest("hex");
}
function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach(k => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function constantTimeEq(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}
function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

/** 从设备已用卡密 + 凭证解析有效权益（v2） */
async function resolveEntitlement(deviceId, cert) {
  const today = todayIso();
  const candidates = [];
  try {
    const r = await CODES.where({ usedByDevice: deviceId, status: "used" }).limit(30).get();
    for (const codeDoc of (r.data || [])) {
      if (codeDoc.disabled) continue;
      if (codeDoc.expireDate && codeDoc.expireDate < today) continue;
      const sku = SKU[codeDoc.skuCode];
      if (!sku) continue;
      if (sku.type === "chat_ai") {
        const lim = LIMITS.limitsFromChatAiCodeDoc(codeDoc, sku);
        if (lim) candidates.push({ ...lim, source: lim.isTrial ? "TRIAL" : "CARD" });
      } else if (sku.type === "vip") {
        const lim = LIMITS.limitsFromVipSku(sku, codeDoc);
        if (lim) candidates.push(lim);
      }
    }
  } catch (e) {
    console.warn("resolveEntitlement db failed", e.message);
  }
  if (cert && cert.skuCode) {
    const sku = SKU[cert.skuCode];
    if (sku && (!cert.expireDate || cert.expireDate >= today)) {
      const fakeDoc = {
        skuCode: cert.skuCode,
        chatAiTier: cert.vipLevel,
        entitlementSchema: sku.type === "chat_ai" ? "v2" : undefined,
        expireDate: cert.expireDate,
      };
      if (sku.type === "chat_ai") {
        const lim = LIMITS.limitsFromChatAiCodeDoc(fakeDoc, sku);
        if (lim) candidates.push({ ...lim, source: lim.isTrial ? "TRIAL" : "CARD" });
      } else if (sku.type === "vip") {
        const lim = LIMITS.limitsFromVipSku(sku, fakeDoc);
        if (lim) candidates.push(lim);
      }
    }
  }
  let ent = LIMITS.pickBestEntitlement(candidates);
  try {
    const r2 = await REVOCATIONS.where({ deviceId }).limit(5).get();
    for (const d of ((r2 && r2.data) || [])) {
      if (!d || !d.revokedAt) continue;
      if (cert && cert.issuedAt && cert.issuedAt < d.revokedAt) {
        ent = { ...LIMITS.limitsForTier(0), source: "NONE" };
        break;
      }
    }
  } catch (e) {}
  return ent;
}

function ymdKey(d) {
  const dt = d || new Date();
  // 使用东八区日期（更贴合中国用户的"今天"）
  const local = new Date(dt.getTime() + 8 * 3600 * 1000);
  return local.getUTCFullYear() * 10000 +
         (local.getUTCMonth() + 1) * 100 +
         local.getUTCDate();
}

// book / reader_dna 仍用旧表（本需求仅改聊天记账 bill/chat）
const DAILY_TABLE_BOOK = { 0: 1, 1: 5, 2: 20, 3: -1, 99: -1 };
const DAILY_TABLE_DNA = { 0: 1, 1: 1, 2: 1, 3: 3, 99: 3 };
function dailyLimitOfLegacy(mode, vipLevel) {
  const t = mode === "book" ? DAILY_TABLE_BOOK : DAILY_TABLE_DNA;
  return t[vipLevel] != null ? t[vipLevel] : t[0];
}

function callLLM({ baseUrl, apiKey, model, system, user, timeoutMs }) {
  return new Promise((resolve, reject) => {
    const url = new URL("/v1/chat/completions", baseUrl);
    const payload = JSON.stringify({
      model, temperature: 0.8, max_tokens: 400,
      messages: [
        { role: "system", content: system },
        { role: "user", content: user },
      ],
    });
    const req = https.request({
      method: "POST",
      hostname: url.hostname,
      port: url.port || 443,
      path: url.pathname,
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + apiKey,
        "Content-Length": Buffer.byteLength(payload),
      },
      timeout: timeoutMs,
    }, res => {
      let buf = "";
      res.on("data", c => buf += c);
      res.on("end", () => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          return reject(new Error("LLM_HTTP_" + res.statusCode + ":" + buf.slice(0, 200)));
        }
        try {
          const j = JSON.parse(buf);
          const reply = j && j.choices && j.choices[0] && j.choices[0].message && j.choices[0].message.content;
          if (typeof reply !== "string" || !reply.trim()) return reject(new Error("LLM_EMPTY"));
          resolve(reply.trim());
        } catch (e) { reject(new Error("LLM_BAD_JSON")); }
      });
    });
    req.on("timeout", () => req.destroy(new Error("LLM_TIMEOUT")));
    req.on("error", e => reject(e));
    req.write(payload);
    req.end();
  });
}

async function ensureCollection(name) {
  try { await db.createCollection(name); } catch (e) { /* exists */ }
}

async function consumeTrialQuota(deviceId, ent) {
  const maxTotal = ent.trialTotal || 5;
  let doc = null;
  try {
    const r = await TRIAL_POOL.where({ deviceId }).limit(1).get();
    doc = r && r.data && r.data[0];
  } catch (e) {}
  const used = doc ? (doc.totalUsed || 0) : 0;
  if (used >= maxTotal) {
    return { allowed: false, code: "TRIAL_EXHAUSTED", used, limit: maxTotal, poolType: "trial_total" };
  }
  const writeOnce = async () => {
    if (doc) {
      await TRIAL_POOL.doc(doc._id).update({ totalUsed: used + 1, updatedAt: db.serverDate() });
    } else {
      await TRIAL_POOL.add({
        deviceId, totalUsed: 1, maxTotal, updatedAt: db.serverDate(),
      });
    }
  };
  try {
    await writeOnce();
  } catch (e) {
    const msg = String(e && e.message || e);
    if (msg.includes("collection not exists") || msg.includes("DATABASE_COLLECTION_NOT_EXIST")) {
      await ensureCollection("chat_ai_trial");
      await writeOnce();
    } else throw e;
  }
  return {
    allowed: true, used: used + 1, limit: maxTotal,
    usedMonth: 0, limitMonth: 0, poolType: "trial_total",
  };
}

async function consumeDailyQuota(deviceId, dailyLimit, mode) {
  const ymd = ymdKey();
  const QUOTA = quotaColOf(mode);
  const colName = quotaColNameOf(mode);
  let existing = null;
  try {
    const r = await QUOTA.where({ deviceId, ymd }).limit(1).get();
    if (r && r.data && r.data.length > 0) existing = r.data[0];
  } catch (e) {}
  const used = existing ? (existing.count || 0) : 0;
  if (dailyLimit > 0 && used >= dailyLimit) {
    return { allowed: false, code: "QUOTA_EXCEEDED", used, limit: dailyLimit, poolType: "daily" };
  }
  const writeOnce = async () => {
    if (existing) {
      await QUOTA.doc(existing._id).update({ count: used + 1, updatedAt: db.serverDate() });
    } else {
      await QUOTA.add({ deviceId, ymd, count: 1, updatedAt: db.serverDate() });
    }
  };
  try {
    await writeOnce();
  } catch (e) {
    const msg = String(e && e.message || e);
    if (msg.includes("collection not exists") || msg.includes("DATABASE_COLLECTION_NOT_EXIST")) {
      await ensureCollection(colName);
      await writeOnce();
    } else {
      console.error(colName + " write failed", msg);
    }
  }
  return { allowed: true, used: used + 1, limit: dailyLimit, poolType: "daily" };
}

async function consumeMonthlyQuota(deviceId, monthlyLimit) {
  if (!monthlyLimit || monthlyLimit <= 0) {
    return { allowed: true, usedMonth: 0, limitMonth: 0 };
  }
  const ym = LIMITS.ymKey();
  let existing = null;
  try {
    const r = await QUOTA_MONTH.where({ deviceId, ym }).limit(1).get();
    if (r && r.data && r.data.length > 0) existing = r.data[0];
  } catch (e) {}
  const usedMonth = existing ? (existing.count || 0) : 0;
  if (usedMonth >= monthlyLimit) {
    return { allowed: false, code: "MONTHLY_CAP", usedMonth, limitMonth: monthlyLimit, poolType: "monthly" };
  }
  const writeOnce = async () => {
    if (existing) {
      await QUOTA_MONTH.doc(existing._id).update({ count: usedMonth + 1, updatedAt: db.serverDate() });
    } else {
      await QUOTA_MONTH.add({ deviceId, ym, count: 1, updatedAt: db.serverDate() });
    }
  };
  try {
    await writeOnce();
  } catch (e) {
    const msg = String(e && e.message || e);
    if (msg.includes("collection not exists") || msg.includes("DATABASE_COLLECTION_NOT_EXIST")) {
      await ensureCollection("chat_ai_quota_month");
      await writeOnce();
    } else throw e;
  }
  return { allowed: true, usedMonth: usedMonth + 1, limitMonth: monthlyLimit };
}

/** book / reader_dna 沿用旧逻辑 */
async function consumeLegacyModeQuota(deviceId, vipLevel, mode) {
  const limit = dailyLimitOfLegacy(mode, vipLevel);
  const ymd = ymdKey();
  const QUOTA = quotaColOf(mode);
  let existing = null;
  try {
    const r = await QUOTA.where({ deviceId, ymd }).limit(1).get();
    if (r && r.data && r.data.length > 0) existing = r.data[0];
  } catch (e) {}
  const used = existing ? (existing.count || 0) : 0;
  if (limit !== -1 && used >= limit) {
    return { allowed: false, code: "QUOTA_EXCEEDED", used, limit };
  }
  if (existing) {
    await QUOTA.doc(existing._id).update({ count: used + 1, updatedAt: db.serverDate() });
  } else {
    await QUOTA.add({ deviceId, ymd, count: 1, updatedAt: db.serverDate() });
  }
  return { allowed: true, used: used + 1, limit };
}

async function consumeChatQuota(deviceId, ent, mode) {
  if (mode === "book" || mode === "reader_dna") {
    const tier = ent.tier || 0;
    const vipLevel = tier <= 4 ? tier : 0;
    return consumeLegacyModeQuota(deviceId, vipLevel, mode);
  }
  if (ent.isTrial && ent.trialTotal > 0 && ent.daily <= 0) {
    return consumeTrialQuota(deviceId, ent);
  }
  const dayQ = await consumeDailyQuota(deviceId, ent.daily, mode);
  if (!dayQ.allowed) return dayQ;
  const monthQ = await consumeMonthlyQuota(deviceId, ent.monthly);
  if (!monthQ.allowed) {
    await rollbackDaily(deviceId, mode);
    return monthQ;
  }
  return {
    allowed: true,
    used: dayQ.used,
    limit: dayQ.limit,
    usedMonth: monthQ.usedMonth,
    limitMonth: monthQ.limitMonth,
    poolType: "daily",
  };
}

async function rollbackDaily(deviceId, mode) {
  try {
    const QUOTA = quotaColOf(mode);
    const ymd = ymdKey();
    const r = await QUOTA.where({ deviceId, ymd }).limit(1).get();
    const doc = r && r.data && r.data[0];
    if (doc && (doc.count || 0) > 0) {
      await QUOTA.doc(doc._id).update({ count: _.inc(-1) });
    }
  } catch (e) {}
}

async function rollbackQuota(deviceId, mode, poolType) {
  if (poolType === "trial_total") {
    try {
      const r = await TRIAL_POOL.where({ deviceId }).limit(1).get();
      const doc = r && r.data && r.data[0];
      if (doc && (doc.totalUsed || 0) > 0) {
        await TRIAL_POOL.doc(doc._id).update({ totalUsed: _.inc(-1) });
      }
    } catch (e) {}
    return;
  }
  await rollbackDaily(deviceId, mode);
  if (poolType === "daily" || poolType === "monthly") {
    try {
      const ym = LIMITS.ymKey();
      const r = await QUOTA_MONTH.where({ deviceId, ym }).limit(1).get();
      const doc = r && r.data && r.data[0];
      if (doc && (doc.count || 0) > 0) {
        await QUOTA_MONTH.doc(doc._id).update({ count: _.inc(-1) });
      }
    } catch (e) {}
  }
}

exports.main = async (event) => {
  const SECRET = process.env.HMAC_SECRET;
  const AI_KEY = process.env.AI_API_KEY;
  const AI_BASE = process.env.AI_BASE_URL || "https://api.deepseek.com";
  const AI_MODEL = process.env.AI_MODEL || "deepseek-chat";
  const AI_TIMEOUT = parseInt(process.env.AI_TIMEOUT_MS || "20000", 10);
  if (!SECRET || SECRET.length < 32) return fail("SERVER_MISCONFIG", "服务端配置异常");
  if (!AI_KEY) return fail("SERVER_MISCONFIG", "AI 服务暂未就绪");

  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return fail("BAD_REQUEST", "请求格式错误"); }
  }
  const cert = body && body.certificate;
  const signature = body && body.signature;
  const payload = (body && body.body) || {};
  if (!cert || !signature) return fail("INVALID", "凭证缺失");
  const ip = extractIp(event);
  const deviceId = cert.deviceId || "";
  if (!deviceId) return fail("INVALID", "凭证字段缺失");

  // 限流：单设备 60s 30 次（聊天用得比信箱频繁）
  const rl = await rateLimit(db, { key: "chat_ai:" + deviceId, limit: 30, windowMs: 60_000 });
  if (!rl.ok) return fail("RATE_LIMITED", "请求过于频繁，请稍后再试");

  const expected = sign(canonicalJson(cert), SECRET);
  if (!constantTimeEq(expected, signature)) return fail("BAD_SIGNATURE", "凭证签名无效");
  if (typeof cert.exp !== "number" || cert.exp < nowSec()) {
    return fail("CERT_EXPIRED", "凭证已过期，请联网激活");
  }

  let ent;
  try {
    ent = await resolveEntitlement(deviceId, cert);
  } catch (e) {
    return fail("DB_ERROR", "服务繁忙");
  }

  const userText = String(payload.userText || "").trim();
  if (!userText) return fail("BAD_REQUEST", "输入为空");
  if (userText.length > 2000) return fail("BAD_REQUEST", "输入过长");
  const personaSystem = String(payload.personaSystem || "你是一个友好的助理。").slice(0, 2000);
  const mode = String(payload.mode || "chat").slice(0, 16);

  const isChatBill = mode !== "book" && mode !== "reader_dna";
  if (isChatBill && (ent.tier <= 0 || (ent.daily <= 0 && !ent.isTrial))) {
    return fail("NO_ENTITLEMENT", "未激活 AI 额度，请兑换卡密后使用云端对话");
  }

  const q = await consumeChatQuota(deviceId, ent, mode);
  if (!q.allowed) {
    const code = q.code || "QUOTA_EXCEEDED";
    const msg = code === "MONTHLY_CAP" ? "本月 AI 对话额度已用完"
      : code === "TRIAL_EXHAUSTED" ? "体验额度已用完，请购买月卡"
        : "今日 AI 对话额度已用完";
    return {
      ok: false, code, msg,
      used: q.used, limit: q.limit,
      usedMonth: q.usedMonth, limitMonth: q.limitMonth,
      tier: ent.tier, source: ent.source, poolType: q.poolType,
    };
  }

  let reply;
  try {
    reply = await callLLM({
      baseUrl: AI_BASE, apiKey: AI_KEY, model: AI_MODEL,
      system: personaSystem, user: userText, timeoutMs: AI_TIMEOUT,
    });
  } catch (e) {
    await rollbackQuota(deviceId, mode, q.poolType);
    try {
      await LOG.add({ data: {
        action: "chat_ai_failed", deviceId, ip, mode,
        reason: String(e && e.message || e).slice(0, 200),
        at: db.serverDate(),
      }});
    } catch (lg) {}
    return fail("LLM_FAILED", "AI 服务暂时异常");
  }

  return {
    ok: true, reply,
    used: q.used,
    limit: q.limit,
    usedMonth: q.usedMonth || 0,
    limitMonth: q.limitMonth || 0,
    tier: ent.tier,
    source: ent.source,
    poolType: q.poolType,
    vipLevel: ent.tier,
  };
};
