// ============================================================
// 设备迁移云函数 /migrate
// ------------------------------------------------------------
// 用户场景：换了新手机，想把已激活的 VIP 迁过去
//   1. 用户在新设备 App 内点"迁移VIP"
//   2. 输入原卡密 + 新设备指纹
//   3. 云端：验证卡密属于该用户（status=used） →
//      把 usedByDevice 改为新设备 → 发新凭证
//   4. 原设备启动时 verify 失败 → 自动降级为普通用户
//
// 反滥用策略：
//   - 每次迁移 +1 migrateCount
//   - 默认上限 3 次（防止刷凭证）
//   - 超过上限返回 BLOCKED，让用户联系客服手动重置
//
// 入参: { code, newDeviceId }
// 出参: 同 redeem（成功时 bonusCoins=0，不再赠送金币）
// ============================================================

const tcb = require("@cloudbase/node-sdk");
const crypto = require("crypto");
const SKU = require("./sku");
const { rateLimit, extractIp } = require("./rate-limit");
const { authenticate } = require("./identity");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();

const CODES = db.collection("vip_codes");
const LOG = db.collection("vip_redeem_log");

const MAX_MIGRATE_TIMES = 3;

function fail(code, msg) {
  return { ok: false, code, msg };
}
const SILENT_REASONS = new Set(["BAD_REQUEST", "SERVER_MISCONFIG", "DB_ERROR"]);
async function failL(reason, msg, ctx) {
  if (!SILENT_REASONS.has(reason)) {
    try {
      await LOG.add({ data: {
        action: "migrate_failed",
        reason,
        code: (ctx && ctx.code) || "",
        deviceId: (ctx && ctx.deviceId) || "",
        msg, at: db.serverDate(),
      }});
    } catch (e) {}
  }
  return fail(reason, msg);
}
function nowSec() {
  return Math.floor(Date.now() / 1000);
}
function normalizeCode(s) {
  return (s || "").trim().toUpperCase().replace(/[\s\-_]/g, "");
}
function sign(payload, secret) {
  return crypto.createHmac("sha256", secret).update(payload).digest("hex");
}
function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach((k) => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function calcExpireDate(d) {
  if (d < 0) return null;
  const x = new Date();
  x.setDate(x.getDate() + d);
  return x.toISOString().slice(0, 10);
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

  const code = normalizeCode(body && body.code);
  const newDeviceId = (body && body.newDeviceId) || "";
  const oldDeviceId = (body && body.oldDeviceId) || ""; // 🔒 新增：必须证明你是原持有人
  const ctx = { code, deviceId: newDeviceId };

  // 🔒 限流：单 IP/新设备 60s 内最多 5 次迁移尝试（必须在所有 return 之前，
  //   否则攻击者可发垃圾格式请求绕过限流；newDeviceId 缺失时回退 IP / anon）
  const ip = extractIp(event);
  const rl = await rateLimit(db, {
    key: "migrate:" + (ip || (newDeviceId && newDeviceId.slice(0, 16)) || "anon"),
    limit: 5, windowMs: 60_000,
  });
  if (!rl.ok) return await failL("RATE_LIMITED", "请求过于频繁，请稍后再试", ctx);

  if (!code || code.length < 8) return await failL("INVALID", "兑换码格式错误", ctx);
  if (!newDeviceId || newDeviceId.length < 16) return await failL("INVALID", "新设备指纹缺失", ctx);

  // 🔒 device_token 鉴权：必须证明拥有当前账号
  //    防止"知道 code + oldDeviceId"就能凭空抢走 VIP（截屏分享攻击）
  const auth = authenticate({
    deviceToken: body.deviceToken,
    username: body.username,
    deviceId: newDeviceId.slice(0, 32),
  });
  if (!auth.ok) {
    return await failL(auth.code, auth.msg, ctx);
  }

  // 1) 查卡密
  let codeDoc;
  try {
    const r = await CODES.where({ code }).limit(1).get();
    codeDoc = r.data && r.data[0];
  } catch (e) {
    return fail("DB_ERROR", "服务繁忙，请稍后重试");
  }

  if (!codeDoc) return await failL("INVALID", "兑换码不存在", ctx);
  if (codeDoc.disabled) return await failL("DISABLED", "兑换码已被禁用", ctx);
  if (codeDoc.status !== "used") return await failL("NOT_REDEEMED", "该卡密尚未激活，请直接兑换", ctx);

  // 2) 如果新设备 ID 就是当前绑定的设备 → 幂等
  if (codeDoc.usedByDevice === newDeviceId) {
    return buildCert(codeDoc, newDeviceId, SECRET);
  }

  // 🔒 3) 必须证明是原设备发起：oldDeviceId 必填且必须等于当前绑定
  //    这是关键的反滥用：防止知道 code 的人凭空抢走 VIP
  if (!oldDeviceId || oldDeviceId.length < 16) {
    return await failL("OLD_DEVICE_REQUIRED",
      "迁移需要原设备指纹，请在原手机的「我的-VIP-迁移」导出迁移凭证后再操作", ctx);
  }
  if (codeDoc.usedByDevice !== oldDeviceId) {
    return await failL("OLD_DEVICE_MISMATCH",
      "原设备指纹不匹配，无权迁移此卡密。如原设备已遗失请联系客服", ctx);
  }

  // 4) 反滥用：次数限制
  const migrateCount = codeDoc.migrateCount || 0;
  if (migrateCount >= MAX_MIGRATE_TIMES) {
    return await failL("BLOCKED", `迁移次数已达上限(${MAX_MIGRATE_TIMES})，请联系客服`, ctx);
  }

  // 5) 原子更新设备绑定（额外用 usedByDevice=oldDeviceId 作为 where 条件，防并发抢迁）
  //    🔒 不更新 expireDate，确保 VIP 期限锁定在首次兑换那天 + N 天
  //    兼容旧记录：如果老记录没存 expireDate，迁移时用首次 usedAt 作为基准补录
  const sku = SKU[codeDoc.skuCode];
  let lockedExpireDate = codeDoc.expireDate;
  if (lockedExpireDate === undefined && sku) {
    // 老记录补救：基于 usedAt 计算（usedAt 是 Date 对象/serverDate）
    let base;
    try {
      base = codeDoc.usedAt && codeDoc.usedAt.toDate ? codeDoc.usedAt.toDate()
           : codeDoc.usedAt instanceof Date ? codeDoc.usedAt
           : codeDoc.usedAt ? new Date(codeDoc.usedAt) : new Date();
    } catch (e) { base = new Date(); }
    if (sku.durationDays < 0) {
      lockedExpireDate = null;
    } else {
      const x = new Date(base.getTime());
      x.setDate(x.getDate() + sku.durationDays);
      lockedExpireDate = x.toISOString().slice(0, 10);
    }
  }

  let updated = 0;
  try {
    const u = await CODES.where({ code, usedByDevice: oldDeviceId }).update({
      usedByDevice: newDeviceId,
      migrateCount: migrateCount + 1,
      lastMigrateAt: db.serverDate(),
      expireDate: lockedExpireDate, // 锁定（旧记录就此补录）
    });
    updated = u.updated || 0;
  } catch (e) {
    return fail("DB_ERROR", "服务繁忙，请稍后重试");
  }
  if (updated === 0) {
    return await failL("RACE", "迁移失败，请刷新后重试", ctx);
  }

  // 5) 写日志
  try {
    await LOG.add({
      data: {
        code,
        deviceId: newDeviceId,
        oldDeviceId: codeDoc.usedByDevice,
        skuCode: codeDoc.skuCode,
        action: "migrate",
        at: db.serverDate(),
      },
    });
  } catch (e) {
    console.warn("写日志失败", e.message);
  }

  return buildCert(codeDoc, newDeviceId, SECRET, lockedExpireDate);
};

function buildCert(codeDoc, deviceId, secret, lockedExpireDate) {
  const sku = SKU[codeDoc.skuCode];
  if (!sku) return fail("UNKNOWN_SKU", "未知商品类型");

  // 🔒 优先用 codeDoc 锁定的 expireDate；显式传入的 lockedExpireDate 优先（用于刚算好的）
  const expireDate = (lockedExpireDate !== undefined)
    ? lockedExpireDate
    : (codeDoc.expireDate !== undefined ? codeDoc.expireDate : calcExpireDate(sku.durationDays));

  const cert = {
    deviceId,
    skuCode: codeDoc.skuCode,
    vipLevel: sku.vipLevel,
    expireDate,
    bonusCoins: 0, // 迁移不再赠送金币
    issuedAt: nowSec(),
    exp: nowSec() + 365 * 86400,
  };
  return {
    ok: true,
    certificate: cert,
    signature: sign(canonicalJson(cert), secret),
  };
}
