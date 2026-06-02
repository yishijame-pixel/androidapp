// ⚠️ 自动生成，请勿手改。源文件: backend/shared/rate-limit.js
// 修改方式：编辑 shared/rate-limit.js 后运行 `node sync-sku.js`
// ============================================================
// 云函数共享：IP + 业务 key 滑动窗口限流
// ------------------------------------------------------------
// 用法：
//   const { rateLimit } = require("./rate-limit");
//   const r = await rateLimit(db, { key: "redeem:" + ip, limit: 30, windowMs: 60_000 });
//   if (!r.ok) return { ok:false, code:"RATE_LIMITED", msg:"请求过于频繁，请稍后再试" };
//
// 设计：
//   - 使用 vip_rate_limit 集合存计数（{ key, count, windowStart }）
//   - 窗口过期自动重置（窗口起点 = 第一次到达的时间）
//   - 数据库失败 → fail-open（不影响正常业务），仅记日志
// ============================================================

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
      // 首次：直接插入
      const docId = ("rl_" + key).replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 100);
      await COL.doc(docId).set({
        key, count: 1, windowStart: now, lastAt: now,
      });
      return { ok: true, count: 1, limit };
    }

    const windowStart = doc.windowStart || now;
    const expired = (now - windowStart) >= windowMs;
    if (expired) {
      // 重置窗口
      await COL.where({ key }).update({
        count: 1, windowStart: now, lastAt: now,
      });
      return { ok: true, count: 1, limit };
    }

    const newCount = (doc.count || 0) + 1;
    if (newCount > limit) {
      await COL.where({ key }).update({ count: newCount, lastAt: now });
      return { ok: false, count: newCount, limit, msg: "请求过于频繁，请稍后再试" };
    }
    await COL.where({ key }).update({ count: newCount, lastAt: now });
    return { ok: true, count: newCount, limit };
  } catch (e) {
    // fail-open：限流不能拖垮主业务
    return { ok: true, count: 0, limit, error: e.message };
  }
}

/** 从 event 提取客户端 IP（兼容多种触发器格式） */
function extractIp(event) {
  try {
    const h =
      (event && event.headers) ||
      (event && event.requestContext && event.requestContext.headers) ||
      {};
    return (h["x-forwarded-for"] || h["x-real-ip"] || "").toString().split(",")[0].trim();
  } catch (e) { return ""; }
}

module.exports = { rateLimit, extractIp };
