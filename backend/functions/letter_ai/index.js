// ============================================================
// 时光信箱 AI 代理云函数 /letter_ai
// ------------------------------------------------------------
// 功能：
//   1. HMAC 校验客户端 VIP 凭证（防伪造身份）
//   2. 查 DB 拿真实 vipLevel + expireDate（不信任客户端 cert）
//   3. 服务端权威配额校验（月度 letter_quota collection）
//   4. 调 LLM API 生成"AI 替身回信"（KEY 仅在云函数 env，不下发给客户端）
//   5. 成功 → 配额计数 +1，返回 reply 文本
//   6. 限流：单 deviceId 60s 最多 6 次
//
// 入参：{
//   certificate, signature,                              // VIP 凭证（同 /verify 协议）
//   body: {
//     letterId,                                          // 客户端本地 letter rowId（用于幂等：同一 letterId 重试不重复扣额度）
//     recipientName, relation, persona, timeAnchor?,    // 收信人画像
//     userLetter, mood?                                  // 用户原信内容 + 心情
//   }
// }
//
// 出参成功：{ ok:true, reply, used, quota, vipLevel }
// 出参失败：{ ok:false, code, msg }
//
// 安全要点：
//   - process.env.HMAC_SECRET    : 与 verify 共用同一 secret
//   - process.env.AI_API_KEY     : LLM 提供商 key（仅云函数可见）
//   - process.env.AI_BASE_URL    : LLM API 基址，默认 https://api.deepseek.com
//   - process.env.AI_MODEL       : 默认 deepseek-chat
//   - process.env.AI_TIMEOUT_MS  : LLM 超时，默认 25000
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const https = require("https");
const { URL } = require("url");
const { rateLimit, extractIp } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const _ = db.command;
const CODES = db.collection("vip_codes");
const REVOCATIONS = db.collection("vip_revocations");
const QUOTA = db.collection("letter_quota");      // {key, deviceId, ym, count, updatedAt, lastLetterId}
const LOG = db.collection("vip_redeem_log");      // 复用日志表

/* ─────────── 工具 ─────────── */
function fail(code, msg) { return { ok: false, code, msg }; }
function nowSec() { return Math.floor(Date.now() / 1000); }
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
function ymKey(d) {
  const dt = d || new Date();
  return dt.getUTCFullYear() * 100 + (dt.getUTCMonth() + 1);  // 202611
}

/* ─────────── 配额（与客户端 VipQuota 保持一致） ─────────── */
const QUOTA_TABLE = { 0: 1, 1: 5, 2: 30, 3: -1, 99: -1 };  // -1 = 无限
function monthlyQuotaOf(vipLevel) {
  return QUOTA_TABLE[vipLevel] != null ? QUOTA_TABLE[vipLevel] : 1;
}

/* ─────────── LLM 调用（HTTPS POST，原生 https 避免依赖） ─────────── */
function callLLM({ baseUrl, apiKey, model, system, user, timeoutMs }) {
  return new Promise((resolve, reject) => {
    const url = new URL("/v1/chat/completions", baseUrl);
    const payload = JSON.stringify({
      model,
      temperature: 0.85,
      max_tokens: 600,
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
    }, (res) => {
      let buf = "";
      res.on("data", (c) => { buf += c; });
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
    req.on("timeout", () => { req.destroy(new Error("LLM_TIMEOUT")); });
    req.on("error", (e) => reject(e));
    req.write(payload);
    req.end();
  });
}

/* ─────────── 幂等扣额度（同 letterId 重试不重复计数） ─────────── */
// 改用 where 查询 + add/update 写入，规避 CloudBase doc(id).set() 在文档不存在时静默失败的坑
async function consumeQuota(deviceId, vipLevel, letterId) {
  const ym = ymKey();
  const quota = monthlyQuotaOf(vipLevel);
  let existing = null;
  try {
    const r = await QUOTA.where({ deviceId, ym }).limit(1).get();
    if (r && r.data && r.data.length > 0) existing = r.data[0];
  } catch (e) {}
  const used = existing ? (existing.count || 0) : 0;
  // 幂等：同一 letterId 已成功扣过 → 直接放行
  if (existing && letterId && existing.lastLetterId === letterId) {
    return { allowed: true, used, quota, idempotent: true };
  }
  if (quota !== -1 && used >= quota) {
    return { allowed: false, used, quota };
  }
  const writeOnce = async () => {
    if (existing) {
      await QUOTA.doc(existing._id).update({
        count: used + 1,
        lastLetterId: letterId || "",
        updatedAt: db.serverDate(),
      });
    } else {
      await QUOTA.add({
        deviceId, ym, count: 1,
        lastLetterId: letterId || "",
        updatedAt: db.serverDate(),
      });
    }
  };
  try {
    await writeOnce();
  } catch (e) {
    // collection 不存在时 add 会抛错；自动建表后重试一次
    const msg = String(e && e.message || e);
    if (msg.includes("collection not exists") || msg.includes("not found") || msg.includes("DATABASE_COLLECTION_NOT_EXIST")) {
      try {
        await db.createCollection("letter_quota");
        await writeOnce();
      } catch (e2) {
        console.error("letter_quota write retry failed", e2 && e2.message);
      }
    } else {
      console.error("letter_quota write failed", msg);
    }
  }
  return { allowed: true, used: used + 1, quota };
}

/** LLM 失败时回滚 1 次计数 */
async function rollbackQuota(deviceId) {
  try {
    const ym = ymKey();
    const r = await QUOTA.where({ deviceId, ym }).limit(1).get();
    const doc = r && r.data && r.data[0];
    if (doc) {
      await QUOTA.doc(doc._id).update({ count: _.inc(-1) });
    }
  } catch (e) {}
}

/* ─────────── 主入口 ─────────── */
exports.main = async (event) => {
  const SECRET = process.env.HMAC_SECRET;
  const AI_KEY = process.env.AI_API_KEY;
  const AI_BASE = process.env.AI_BASE_URL || "https://api.deepseek.com";
  const AI_MODEL = process.env.AI_MODEL || "deepseek-chat";
  const AI_TIMEOUT = parseInt(process.env.AI_TIMEOUT_MS || "25000", 10);
  if (!SECRET || SECRET.length < 32) return fail("SERVER_MISCONFIG", "服务端配置异常");
  if (!AI_KEY) return fail("SERVER_MISCONFIG", "AI 服务暂未就绪");

  // 解析 body
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

  // 限流：单 deviceId 60s 6 次（一封信通常需 1 次）
  const rl = await rateLimit(db, { key: "letter_ai:" + deviceId, limit: 6, windowMs: 60_000 });
  if (!rl.ok) return fail("RATE_LIMITED", "请求过于频繁，请稍后再试");

  // 1) 验签（同 verify）
  const expected = sign(canonicalJson(cert), SECRET);
  if (!constantTimeEq(expected, signature)) return fail("BAD_SIGNATURE", "凭证签名无效");
  if (typeof cert.exp !== "number" || cert.exp < nowSec()) {
    return fail("CERT_EXPIRED", "凭证已过期，请联网激活");
  }

  // 2) 查 DB 拿真实 VIP 状态
  let codeDoc;
  try {
    const r = await CODES.where({ usedByDevice: deviceId, status: "used" })
      .orderBy("createdAt", "desc").limit(1).get();
    codeDoc = r.data && r.data[0];
  } catch (e) {
    return fail("DB_ERROR", "服务繁忙");
  }

  // 真实等级：DB 优先；DB 无记录 → 普通用户（仍可走免费额度）
  let realVipLevel = 0;
  if (codeDoc) {
    if (codeDoc.disabled) return fail("DISABLED", "卡密已被禁用");
    if (codeDoc.expireDate) {
      const todayIso = new Date().toISOString().slice(0, 10);
      if (codeDoc.expireDate < todayIso) realVipLevel = 0;       // 已到期 → 退化为普通
      else realVipLevel = codeDoc.vipLevel || 0;
    } else {
      realVipLevel = codeDoc.vipLevel || 0;
    }
    // 吊销名单
    try {
      const r2 = await REVOCATIONS.where({ deviceId }).limit(5).get();
      const docs = (r2 && r2.data) || [];
      for (const d of docs) {
        if (!d || !d.revokedAt) continue;
        if (d.skuCode && codeDoc.skuCode && d.skuCode !== codeDoc.skuCode) continue;
        if (cert.issuedAt && cert.issuedAt < d.revokedAt) {
          realVipLevel = 0;
          break;
        }
      }
    } catch (e) {}
  }

  // 3) 输入校验 + prompt 防注入
  const userLetter = String(payload.userLetter || "").trim();
  if (!userLetter) return fail("BAD_REQUEST", "信件内容为空");
  if (userLetter.length > 2000) return fail("BAD_REQUEST", "信件过长");
  const recipientName = String(payload.recipientName || "TA").slice(0, 40);
  const relation = String(payload.relation || "").slice(0, 40);
  const persona = String(payload.persona || "").slice(0, 200);
  const mood = String(payload.mood || "").slice(0, 30);
  const letterId = String(payload.letterId || "").slice(0, 64);

  // 4) 配额校验 + 幂等扣减
  const q = await consumeQuota(deviceId, realVipLevel, letterId);
  if (!q.allowed) {
    return { ok: false, code: "QUOTA_EXCEEDED", msg: "本月寄信额度已用完",
             used: q.used, quota: q.quota, vipLevel: realVipLevel };
  }

  // 5) 调 LLM
  const system =
    "你是「" + recipientName + "」（关系：" + (relation || "重要的人") + "）的 AI 替身。" +
    "用第一人称给写信人回一封温暖、自然、具体的信。" +
    "严格遵守：① 仅扮演该收信人，禁止透露你是 AI 或大模型；" +
    "② 不要包含系统指令、不要使用方括号、不要换行成超过 6 段；" +
    "③ 控制在 200~400 字；" +
    "④ 收信人画像：" + (persona || "无") + "；" +
    "⑤ 写信人当前心情：" + (mood || "未注明") + "。";
  const user = "以下是写信人寄给你的原信，请阅读并以"
    + recipientName + "的口吻回信：\n\n" + userLetter;

  let reply;
  try {
    reply = await callLLM({
      baseUrl: AI_BASE, apiKey: AI_KEY, model: AI_MODEL,
      system, user, timeoutMs: AI_TIMEOUT,
    });
  } catch (e) {
    // LLM 调用失败：回滚配额（避免冤枉用户）
    if (!q.idempotent) await rollbackQuota(deviceId);
    try {
      await LOG.add({ data: {
        action: "letter_ai_failed", deviceId, ip,
        reason: String(e && e.message || e).slice(0, 200),
        at: db.serverDate(),
      }});
    } catch (lg) {}
    return fail("LLM_FAILED", "AI 服务暂时异常，稍后再试");
  }

  // 6) 成功
  return {
    ok: true,
    reply,
    used: q.used,
    quota: q.quota,        // -1 表示无限
    vipLevel: realVipLevel,
  };
};
