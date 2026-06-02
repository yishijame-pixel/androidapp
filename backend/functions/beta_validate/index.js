// ============================================================
// 内测邀请码校验云函数（注册阶段调用）
//
// 行为：
//   - 校验 code 存在、未禁用、未使用、属于 BETA_INVITE SKU
//   - 标记 used，记录 username + deviceId
//   - 写日志 vip_redeem_log
//
// 兼容硬编码：
//   - 老内测码（如 223498 / FUNLIFE2026 / BETA001）走客户端本地兜底
//   - 本函数只处理云端发售的 BETA_INVITE 卡密
//
// 请求体: { code, username, deviceId }
// 响应:   { ok: true } 或 { ok:false, code, msg }
// ============================================================

const cloudbase = require("@cloudbase/node-sdk");
const SKU = require("./sku");
const { rateLimit, extractIp } = require("./rate-limit");

const app = cloudbase.init({});
const db = app.database();
const CODES = db.collection("vip_codes");
const LOG = db.collection("vip_redeem_log");

function fail(code, msg) { return { ok: false, code, msg }; }
const SILENT_REASONS = new Set(["BAD_REQUEST", "DB_ERROR"]);
async function failL(reason, msg, ctx) {
  if (!SILENT_REASONS.has(reason)) {
    try {
      await LOG.add({ data: {
        action: "beta_validate_failed",
        reason,
        code: (ctx && ctx.code) || "",
        deviceId: (ctx && ctx.deviceId) || "",
        username: (ctx && ctx.username) || "",
        msg, at: db.serverDate(),
      }});
    } catch (e) {}
  }
  return fail(reason, msg);
}
function normalizeCode(s) {
  return (s || "").trim().toUpperCase().replace(/[\s\-_]/g, "");
}

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return fail("BAD_REQUEST", "请求格式错误"); }
  }

  const code = normalizeCode(body.code);
  const username = (body.username || "").trim().slice(0, 64);
  const deviceId = (body.deviceId || "").trim();
  const ctx = { code, deviceId, username };

  // 🔒 限流：每 IP/设备 60s 内最多 8 次（必须在所有 return 之前，
  //   否则攻击者发垃圾格式请求可以绕过限流轰炸日志/枚举）
  const ip = extractIp(event);
  const rl = await rateLimit(db, {
    key: "beta:" + (ip || deviceId.slice(0, 16) || "anon"),
    limit: 8, windowMs: 60_000,
  });
  if (!rl.ok) return await failL("RATE_LIMITED", "请求过于频繁，请稍后再试", ctx);

  if (!code || code.length < 8) return await failL("INVALID", "内测码格式无效", ctx);

  // 1) 查询
  const r = await CODES.where({ code }).limit(1).get();
  const doc = r.data && r.data[0];
  if (!doc) return await failL("INVALID", "内测码不存在", ctx);
  if (doc.disabled) return await failL("DISABLED", "此内测码已禁用", ctx);

  // 2) SKU 校验
  const sku = SKU[doc.skuCode];
  if (!sku) return await failL("UNKNOWN_SKU", "未知商品类型", ctx);
  if (sku.type !== "beta") return await failL("WRONG_TYPE", "此卡密不是内测邀请码", ctx);

  // 3) 已使用 → 严格一次性：无论同/异设备、同/异用户名，一律拒绝
  //    设计取舍：杜绝"清数据后用同码再注册"，代价是注册中途断网会导致整码作废
  //    如确属误激活，需运营在管理后台手动 reset code 后重新发放
  if (doc.status === "used") {
    return await failL("USED", "此内测码已被使用", ctx);
  }

  // 4) 原子标记 used
  let updated = 0;
  try {
    const u = await CODES.where({ code, status: "unused" }).update({
      status: "used",
      usedByDevice: deviceId || null,
      usedByUsername: username || null,
      usedAt: db.serverDate(),
    });
    updated = u.updated || 0;
  } catch (e) {
    return fail("DB_ERROR", "数据库写入失败");
  }
  if (updated === 0) return await failL("USED", "此内测码已被使用", ctx);

  // 5) 写日志
  try {
    await LOG.add({ data: {
      code, deviceId, username,
      skuCode: doc.skuCode, action: "beta_validate", at: db.serverDate(),
    }});
  } catch (e) {}

  return { ok: true, isReissue: false };
};
