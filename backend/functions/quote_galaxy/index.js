// ============================================================
// 匿名摘抄星河 /quote_galaxy
// ------------------------------------------------------------
// action: feed | publish | light | report
//
// 入参：{ certificate, signature, action, body }
// 公共校验：HMAC + 凭证有效期 + 限流（30/min/设备）
// 不同 action 额外校验：
//   - publish: 仅 vipLevel >= 1；text 1..200 字；违禁词过滤；日 1 条
//   - light:   不限 VIP；同一设备 24h 内对同一星仅一次
//   - report:  不限 VIP；同一设备同一星 24h 内一次；阈值 5 自动隐藏
//   - feed:    不限 VIP，每次最多 30 条
//
// 集合：
//   quote_galaxy   { _id, deviceHash, text, bookTitle, publishedAt, lightCount, reportCount, hidden }
//   galaxy_lights  { deviceId, starId, at }   一个 light 一行（防重）
//   galaxy_reports { deviceId, starId, at, reason }
//   galaxy_publish_quota { deviceId, ymd, count }   日 1 条
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const { rateLimit, extractIp } = require("./rate-limit");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const _ = db.command;

const STARS = db.collection("quote_galaxy");
const LIGHTS = db.collection("galaxy_lights");
const REPORTS = db.collection("galaxy_reports");
const PUB_QUOTA = db.collection("galaxy_publish_quota");
const CODES = db.collection("vip_codes");
const REVOCATIONS = db.collection("vip_revocations");

const REPORT_HIDE_THRESHOLD = 5;
const PUBLISH_TEXT_MIN = 1;
const PUBLISH_TEXT_MAX = 200;
// 🆕 v53 敏感内容过滤 —— 多层（词典 + 正则 + 变体归一化），block 直拒、warn 标记入审
const sensitive = require("./sensitive-filter");

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
function hashDevice(deviceId, salt) {
  return crypto.createHash("sha256").update(deviceId + "|" + salt).digest("hex").slice(0, 24);
}
function ymdKey() {
  const local = new Date(Date.now() + 8 * 3600 * 1000);
  return local.getUTCFullYear() * 10000 + (local.getUTCMonth() + 1) * 100 + local.getUTCDate();
}
// 旧 API 兼容：仅返回是否硬阻断
function containsBlockWord(text) { return sensitive.containsBlockWord(text); }

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

async function ensureCollection(name) {
  try { await db.createCollection(name); } catch (e) { /* ignore */ }
}

/* ─────────── action handlers ─────────── */

async function handleFeed(payload) {
  const limit = Math.min(Math.max(parseInt(payload.body && payload.body.limit) || 30, 1), 50);
  const cursor = payload.body && payload.body.cursor || null;
  let q = STARS.where({ hidden: _.neq(true) }).orderBy("publishedAt", "desc").limit(limit);
  if (cursor) {
    const cursorMs = parseInt(cursor, 10);
    if (Number.isFinite(cursorMs)) {
      q = STARS.where({ hidden: _.neq(true), publishedAt: _.lt(cursorMs) })
        .orderBy("publishedAt", "desc").limit(limit);
    }
  }
  let items = [];
  try {
    const r = await q.get();
    items = (r.data || []).map(d => ({
      id: d._id,
      text: d.text,
      bookTitle: d.bookTitle || "",
      publishedAt: d.publishedAt || 0,
      lightCount: d.lightCount || 0,
    }));
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("quote_galaxy");
    }
  }
  const nextCursor = items.length === limit ? String(items[items.length - 1].publishedAt) : null;
  return ok({ items, nextCursor });
}

async function handlePublish(deviceId, vipLevel, payload, salt) {
  if (vipLevel < 1) return fail("FORBIDDEN", "需要月卡及以上才能在星河发声");
  const text = String((payload.body && payload.body.text) || "").trim();
  const bookTitle = String((payload.body && payload.body.bookTitle) || "").trim().slice(0, 80);
  if (text.length < PUBLISH_TEXT_MIN || text.length > PUBLISH_TEXT_MAX) {
    return fail("BAD_REQUEST", "摘抄长度需在 " + PUBLISH_TEXT_MIN + "-" + PUBLISH_TEXT_MAX + " 字之间");
  }
  // 🆕 v53 多层敏感内容过滤
  const checkText = sensitive.check(text);
  const checkBook = sensitive.check(bookTitle);
  if (!checkText.ok || !checkBook.ok) {
    return fail("BLOCKED", "内容包含敏感信息（疑似导流/联系方式/违规词），请修改后再寄。");
  }
  // warn 命中：不阻断，但标记需要人工复核 + 写审计日志
  const warnMatched = []
    .concat(checkText.matched || [])
    .concat(checkBook.matched || [])
    .filter(m => m.severity === "warn");
  const needsReview = warnMatched.length > 0;
  // 日额度 1 条
  const ymd = ymdKey();
  let quotaDoc = null;
  try {
    const r = await PUB_QUOTA.where({ deviceId, ymd }).limit(1).get();
    quotaDoc = r.data && r.data[0];
  } catch (e) {}
  if (quotaDoc && (quotaDoc.count || 0) >= 1) {
    return fail("QUOTA_EXCEEDED", "今日已发过一条，明天再来");
  }
  const deviceHash = hashDevice(deviceId, salt);
  const nowMs = Date.now();
  let starId = "";
  try {
    const r = await STARS.add({
      deviceHash, text, bookTitle,
      publishedAt: nowMs, lightCount: 0, reportCount: 0, hidden: false,
      // 🆕 v53 软警告标记：admin 面板可优先审核此类内容
      needsReview, warnMatched: warnMatched.map(m => ({ kind: m.kind, hit: m.hit })),
      createdAt: db.serverDate(),
    });
    starId = r.id || r._id;
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("quote_galaxy");
      const r = await STARS.add({
        deviceHash, text, bookTitle,
        publishedAt: nowMs, lightCount: 0, reportCount: 0, hidden: false,
        needsReview, warnMatched: warnMatched.map(m => ({ kind: m.kind, hit: m.hit })),
        createdAt: db.serverDate(),
      });
      starId = r.id || r._id;
    } else { throw e; }
  }
  // 配额 +1
  try {
    if (quotaDoc) {
      await PUB_QUOTA.doc(quotaDoc._id).update({ count: (quotaDoc.count || 0) + 1, updatedAt: db.serverDate() });
    } else {
      await PUB_QUOTA.add({ deviceId, ymd, count: 1, updatedAt: db.serverDate() });
    }
  } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("galaxy_publish_quota");
      try { await PUB_QUOTA.add({ deviceId, ymd, count: 1, updatedAt: db.serverDate() }); } catch (_) {}
    }
  }
  return ok({ id: starId });
}

async function handleLight(deviceId, payload) {
  const starId = String((payload.body && payload.body.id) || "");
  if (!starId) return fail("BAD_REQUEST", "缺少 id");
  // 24h 内同设备同星只允许 1 次
  const dayAgo = Date.now() - 24 * 3600 * 1000;
  let already = null;
  try {
    const r = await LIGHTS.where({ deviceId, starId, at: _.gt(dayAgo) }).limit(1).get();
    already = r.data && r.data[0];
  } catch (e) {
    if (String(e.message || "").includes("not exists")) await ensureCollection("galaxy_lights");
  }
  if (already) {
    // 仍返回当前 lightCount，不计入再次自增
    let cur = 0;
    try { const s = await STARS.doc(starId).get(); cur = (s.data && s.data.lightCount) || 0; } catch (_) {}
    return ok({ lightCount: cur });
  }
  try { await LIGHTS.add({ deviceId, starId, at: Date.now() }); } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("galaxy_lights");
      try { await LIGHTS.add({ deviceId, starId, at: Date.now() }); } catch (_) {}
    }
  }
  let lightCount = 0;
  try {
    await STARS.doc(starId).update({ lightCount: _.inc(1) });
    const s = await STARS.doc(starId).get();
    lightCount = (s.data && s.data.lightCount) || 0;
  } catch (e) {}
  return ok({ lightCount });
}

async function handleReport(deviceId, payload) {
  const starId = String((payload.body && payload.body.id) || "");
  const reason = String((payload.body && payload.body.reason) || "").slice(0, 80);
  if (!starId) return fail("BAD_REQUEST", "缺少 id");
  // 24h 同设备同星只一次
  const dayAgo = Date.now() - 24 * 3600 * 1000;
  let already = null;
  try {
    const r = await REPORTS.where({ deviceId, starId, at: _.gt(dayAgo) }).limit(1).get();
    already = r.data && r.data[0];
  } catch (e) {
    if (String(e.message || "").includes("not exists")) await ensureCollection("galaxy_reports");
  }
  if (already) return ok({ reported: true, dedup: true });
  try { await REPORTS.add({ deviceId, starId, at: Date.now(), reason }); } catch (e) {
    if (String(e.message || "").includes("not exists")) {
      await ensureCollection("galaxy_reports");
      try { await REPORTS.add({ deviceId, starId, at: Date.now(), reason }); } catch (_) {}
    }
  }
  // STAR.reportCount + 1，达阈值自动隐藏
  try {
    await STARS.doc(starId).update({ reportCount: _.inc(1) });
    const s = await STARS.doc(starId).get();
    const cnt = (s.data && s.data.reportCount) || 0;
    if (cnt >= REPORT_HIDE_THRESHOLD) {
      await STARS.doc(starId).update({ hidden: true });
    }
  } catch (e) {}
  return ok({ reported: true });
}

/* ─────────── entry ─────────── */

exports.main = async (event) => {
  const SECRET = process.env.HMAC_SECRET;
  const SALT = process.env.GALAXY_SALT || (SECRET || "default-salt");
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
  if (!["feed", "publish", "light", "report"].includes(action)) {
    return fail("BAD_REQUEST", "未知 action");
  }

  // 限流：单设备 60s 30 次
  const rl = await rateLimit(db, { key: "galaxy:" + deviceId, limit: 30, windowMs: 60_000 });
  if (!rl.ok) return fail("RATE_LIMITED", "请求过于频繁");

  // HMAC + 有效期
  const expected = sign(canonicalJson(cert), SECRET);
  if (!constantTimeEq(expected, signature)) return fail("BAD_SIGNATURE", "凭证签名无效");
  if (typeof cert.exp !== "number" || cert.exp < nowSec()) {
    return fail("CERT_EXPIRED", "凭证已过期");
  }

  const vipLevel = await getRealVipLevel(deviceId, cert);

  switch (action) {
    case "feed": return await handleFeed(payload);
    case "publish": return await handlePublish(deviceId, vipLevel, payload, SALT);
    case "light": return await handleLight(deviceId, payload);
    case "report": return await handleReport(deviceId, payload);
  }
};
