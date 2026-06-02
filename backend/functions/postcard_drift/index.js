// ============================================================
// 明信片漂流 /postcard_drift
// ------------------------------------------------------------
// VIP2 起：从星海里给随机一位陌生 VIP2+ 读者寄一张明信片。
// action: send | inbox | react
//
// 入参：{ certificate, signature, action, body }
//
// 集合：
//   postcards          { _id, fromDevice, toDevice, text, bookTitle, sentAt, reactedHeart, read }
//   postcard_quota     { deviceId, ymd, count }   // 月配额由 quotaKey=ym 形式存
//   active_readers     { deviceId, lastSeenAt, vipLevel }  // 用于挑收件人
//
// 配额：vipLevel 0/1=0  2=1/月  3=4/月（与客户端 VipQuota.postcardDriftMonthlyLimit 一致）
// 风控：
//   - publish/send: 文本 1..200 字 + 违禁词
//   - send 失败兜底：找不到合适收件人 → 自寄给"星海"占位（不消耗配额，返回 NO_RECIPIENT）
//   - inbox/react: 不消耗配额
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const { rateLimit, extractIp } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const _ = db.command;

const POSTCARDS = db.collection("postcards");
const QUOTA = db.collection("postcard_quota");
const READERS = db.collection("active_readers");
const CODES = db.collection("vip_codes");
const REVOCATIONS = db.collection("vip_revocations");

const TEXT_MIN = 1, TEXT_MAX = 200;
// 🆕 v53 多层敏感内容过滤
const sensitive = require("./sensitive-filter");

// 与客户端 VipQuota.postcardDriftMonthlyLimit 同步
const MONTHLY_LIMIT = { 0: 0, 1: 0, 2: 1, 3: 4, 99: 4 };
function monthlyLimitOf(level) { return MONTHLY_LIMIT[level] != null ? MONTHLY_LIMIT[level] : 0; }

function ok(extra) { return Object.assign({ ok: true }, extra || {}); }
function fail(code, msg, extra) { return Object.assign({ ok: false, code, msg }, extra || {}); }
function nowSec() { return Math.floor(Date.now() / 1000); }
function sign(p, s) { return crypto.createHmac("sha256", s).update(p).digest("hex"); }
function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach(k => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function constantTimeEq(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  return crypto.timingSafeEqual(Buffer.from(a), Buffer.from(b));
}
function ymKey() {
  const local = new Date(Date.now() + 8 * 3600 * 1000);
  return local.getUTCFullYear() * 100 + (local.getUTCMonth() + 1);
}
// 旧 API 兼容
function containsBlockWord(text) { return sensitive.containsBlockWord(text); }
async function ensureCollection(name) {
  try { await db.createCollection(name); } catch (e) {}
}

async function getRealVipLevel(deviceId, cert) {
  try {
    const r = await CODES.where({ usedByDevice: deviceId, status: "used" })
      .orderBy("createdAt", "desc").limit(1).get();
    const doc = r.data && r.data[0];
    if (!doc) return 0;
    if (doc.disabled) return -1;
    let level = doc.vipLevel || 0;
    if (doc.expireDate) {
      const today = new Date().toISOString().slice(0, 10);
      if (doc.expireDate < today) level = 0;
    }
    try {
      const r2 = await REVOCATIONS.where({ deviceId }).limit(5).get();
      for (const d of (r2.data || [])) {
        if (!d || !d.revokedAt) continue;
        if (d.skuCode && doc.skuCode && d.skuCode !== doc.skuCode) continue;
        if (cert.issuedAt && cert.issuedAt < d.revokedAt) { level = 0; break; }
      }
    } catch (e) {}
    return level;
  } catch (e) { return 0; }
}

async function touchReader(deviceId, vipLevel) {
  try {
    const r = await READERS.where({ deviceId }).limit(1).get();
    const doc = r.data && r.data[0];
    if (doc) {
      await READERS.doc(doc._id).update({ lastSeenAt: Date.now(), vipLevel });
    } else {
      await READERS.add({ deviceId, lastSeenAt: Date.now(), vipLevel });
    }
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("active_readers");
      try { await READERS.add({ deviceId, lastSeenAt: Date.now(), vipLevel }); } catch (_) {}
    }
  }
}

async function pickRecipient(senderDevice) {
  // 7 天内活跃，VIP2+，排除自己；随机抽 1 位
  const sevenDayAgo = Date.now() - 7 * 24 * 3600 * 1000;
  try {
    const r = await READERS.where({
      lastSeenAt: _.gt(sevenDayAgo),
      vipLevel: _.gte(2),
      deviceId: _.neq(senderDevice),
    }).limit(50).get();
    const list = r.data || [];
    if (list.length === 0) return null;
    return list[Math.floor(Math.random() * list.length)].deviceId;
  } catch (e) { return null; }
}

async function readMonthlyUsed(deviceId) {
  const ym = ymKey();
  try {
    const r = await QUOTA.where({ deviceId, ym }).limit(1).get();
    const d = r.data && r.data[0];
    return { doc: d, used: d ? (d.count || 0) : 0, ym };
  } catch (e) {
    if (String(e.message || "").includes("not exists")) await ensureCollection("postcard_quota");
    return { doc: null, used: 0, ym };
  }
}

async function bumpMonthlyUsed(deviceId, prevDoc, ym) {
  try {
    if (prevDoc) {
      await QUOTA.doc(prevDoc._id).update({ count: (prevDoc.count || 0) + 1, updatedAt: db.serverDate() });
    } else {
      await QUOTA.add({ deviceId, ym, count: 1, updatedAt: db.serverDate() });
    }
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("postcard_quota");
      try { await QUOTA.add({ deviceId, ym, count: 1, updatedAt: db.serverDate() }); } catch (_) {}
    }
  }
}

/* ─────────── handlers ─────────── */

async function handleSend(deviceId, vipLevel, payload) {
  const limit = monthlyLimitOf(vipLevel);
  if (limit <= 0) return fail("FORBIDDEN", "需要季卡及以上才能寄出明信片");
  const text = String((payload.body && payload.body.text) || "").trim();
  const bookTitle = String((payload.body && payload.body.bookTitle) || "").trim().slice(0, 80);
  if (text.length < TEXT_MIN || text.length > TEXT_MAX)
    return fail("BAD_REQUEST", "明信片文字 " + TEXT_MIN + "-" + TEXT_MAX + " 字");
  // 🆕 v53 多层敏感过滤
  const checkText = sensitive.check(text);
  const checkBook = sensitive.check(bookTitle);
  if (!checkText.ok || !checkBook.ok) {
    return fail("BLOCKED", "内容包含敏感信息（疑似导流/联系方式/违规词），请修改后再寄。");
  }
  const warnMatched = []
    .concat(checkText.matched || [])
    .concat(checkBook.matched || [])
    .filter(m => m.severity === "warn");
  const needsReview = warnMatched.length > 0;

  const { doc: quotaDoc, used, ym } = await readMonthlyUsed(deviceId);
  if (used >= limit) return fail("QUOTA_EXCEEDED", "本月配额已用完", { used, limit, vipLevel });

  const toDevice = await pickRecipient(deviceId);
  if (!toDevice) return fail("NO_RECIPIENT", "暂时找不到合适的收件人，过会再试 ✨");

  let id = "";
  try {
    const r = await POSTCARDS.add({
      fromDevice: deviceId,
      toDevice,
      text,
      bookTitle,
      sentAt: Date.now(),
      reactedHeart: false,
      read: false,
      // 🆕 v53 软警告标记
      needsReview, warnMatched: warnMatched.map(m => ({ kind: m.kind, hit: m.hit })),
      createdAt: db.serverDate(),
    });
    id = r.id || r._id;
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("postcards");
      const r = await POSTCARDS.add({
        fromDevice: deviceId, toDevice, text, bookTitle,
        sentAt: Date.now(), reactedHeart: false, read: false,
        needsReview, warnMatched: warnMatched.map(m => ({ kind: m.kind, hit: m.hit })),
        createdAt: db.serverDate(),
      });
      id = r.id || r._id;
    } else { throw e; }
  }
  await bumpMonthlyUsed(deviceId, quotaDoc, ym);
  return ok({ id, used: used + 1, limit, vipLevel });
}

async function handleInbox(deviceId) {
  let items = [];
  try {
    const r = await POSTCARDS.where({ toDevice: deviceId }).orderBy("sentAt", "desc").limit(50).get();
    items = (r.data || []).map(d => ({
      id: d._id,
      text: d.text,
      bookTitle: d.bookTitle || "",
      sentAt: d.sentAt || 0,
      reactedHeart: !!d.reactedHeart,
    }));
  } catch (e) {
    if (String(e.message || "").includes("not exists")) await ensureCollection("postcards");
  }
  return ok({ items, totalReceived: items.length });
}

async function handleReact(deviceId, payload) {
  const id = String((payload.body && payload.body.id) || "");
  if (!id) return fail("BAD_REQUEST", "缺少 id");
  try {
    const s = await POSTCARDS.doc(id).get();
    const d = s.data;
    if (!d || d.toDevice !== deviceId) return fail("NOT_FOUND", "明信片不存在");
    if (!d.reactedHeart) {
      await POSTCARDS.doc(id).update({ reactedHeart: true, reactedAt: db.serverDate() });
    }
    return ok({});
  } catch (e) {
    return fail("DB_ERROR", "操作失败");
  }
}

/* ─────────── entry ─────────── */

exports.main = async (event) => {
  const SECRET = process.env.HMAC_SECRET;
  if (!SECRET || SECRET.length < 32) return fail("SERVER_MISCONFIG", "服务端配置异常");

  let payload = event;
  if (event && event.body) {
    try { payload = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return fail("BAD_REQUEST", "请求格式错误"); }
  }
  const cert = payload.certificate, signature = payload.signature;
  const action = String(payload.action || "").slice(0, 16);
  if (!cert || !signature) return fail("INVALID", "凭证缺失");
  const deviceId = cert.deviceId || "";
  if (!deviceId) return fail("INVALID", "凭证字段缺失");
  if (!["send", "inbox", "react"].includes(action)) return fail("BAD_REQUEST", "未知 action");

  const rl = await rateLimit(db, { key: "postcard:" + deviceId, limit: 20, windowMs: 60_000 });
  if (!rl.ok) return fail("RATE_LIMITED", "请求过于频繁");

  const expected = sign(canonicalJson(cert), SECRET);
  if (!constantTimeEq(expected, signature)) return fail("BAD_SIGNATURE", "凭证签名无效");
  if (typeof cert.exp !== "number" || cert.exp < nowSec()) return fail("CERT_EXPIRED", "凭证已过期");

  const vipLevel = await getRealVipLevel(deviceId, cert);
  // 心跳收件名册：所有 VIP2+ 调用都更新 lastSeenAt
  if (vipLevel >= 2) await touchReader(deviceId, vipLevel);

  switch (action) {
    case "send":  return await handleSend(deviceId, vipLevel, payload);
    case "inbox": return await handleInbox(deviceId);
    case "react": return await handleReact(deviceId, payload);
  }
};
