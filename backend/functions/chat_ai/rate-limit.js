// ⚠️ 复制自 verify/rate-limit.js，保持同步
async function rateLimit(db, opts) {
  const COL = db.collection("vip_rate_limit");
  const key = String(opts.key || "").slice(0, 128);
  const limit = opts.limit || 30;
  const windowMs = opts.windowMs || 60_000;
  if (!key) return { ok: true, count: 0 };
  const now = Date.now();
  try {
    const r = await COL.where({ key }).limit(1).get();
    const doc = r.data && r.data[0];
    if (!doc) {
      const docId = ("rl_" + key).replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 100);
      await COL.doc(docId).set({ key, count: 1, windowStart: now, lastAt: now });
      return { ok: true, count: 1, limit };
    }
    const windowStart = doc.windowStart || now;
    if ((now - windowStart) >= windowMs) {
      await COL.where({ key }).update({ count: 1, windowStart: now, lastAt: now });
      return { ok: true, count: 1, limit };
    }
    const newCount = (doc.count || 0) + 1;
    if (newCount > limit) {
      await COL.where({ key }).update({ count: newCount, lastAt: now });
      return { ok: false, count: newCount, limit };
    }
    await COL.where({ key }).update({ count: newCount, lastAt: now });
    return { ok: true, count: newCount, limit };
  } catch (e) {
    return { ok: true, count: 0, limit, error: e.message };
  }
}

function extractIp(event) {
  try {
    const h = (event && event.headers) || (event && event.requestContext && event.requestContext.headers) || {};
    return (h["x-forwarded-for"] || h["x-real-ip"] || "").toString().split(",")[0].trim();
  } catch (e) { return ""; }
}

module.exports = { rateLimit, extractIp };
