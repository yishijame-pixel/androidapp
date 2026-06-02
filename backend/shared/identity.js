// ============================================================
// 共享身份签发与校验
// ------------------------------------------------------------
// 用途：
//   1) 注册成功后，服务端给客户端发一个 device_token
//      (= HMAC(username + deviceId + issuedAt))
//   2) 客户端后续写入请求都要带 token，服务端验证 token 是自己签的
//      → 攻击者即使知道 username，没有 token 也写不了
//
// 设计：
//   - token 格式："v1.<payloadB64>.<sigB64>"
//   - payload = { u, d, t } (username, deviceId 前 16 位, issuedAt 秒)
//   - sig = HMAC-SHA256(payload, IDENTITY_SECRET)
//   - IDENTITY_SECRET 与 HMAC_SECRET 不同（环境变量隔离）
// ============================================================

const crypto = require("crypto");

function getSecret() {
  const s = process.env.IDENTITY_SECRET || process.env.HMAC_SECRET;
  if (!s || s.length < 32) {
    throw new Error("IDENTITY_SECRET 未配置");
  }
  return s;
}

function b64url(buf) {
  return Buffer.from(buf).toString("base64")
    .replace(/=+$/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
function b64urlDecode(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  return Buffer.from(s, "base64").toString("utf8");
}

/** 签发 device_token */
function issueDeviceToken(username, deviceId) {
  const payload = JSON.stringify({
    u: String(username || "").slice(0, 64),
    d: String(deviceId || "").slice(0, 32),  // deviceId 前 32 位足够当指纹
    t: Math.floor(Date.now() / 1000),
  });
  const payloadB64 = b64url(payload);
  const sig = crypto.createHmac("sha256", getSecret()).update(payloadB64).digest();
  return "v1." + payloadB64 + "." + b64url(sig);
}

/** 校验 device_token，返回 { username, deviceIdPrefix, issuedAt } 或 null */
function verifyDeviceToken(token) {
  if (!token || typeof token !== "string") return null;
  const parts = token.split(".");
  if (parts.length !== 3 || parts[0] !== "v1") return null;
  try {
    const expected = crypto.createHmac("sha256", getSecret()).update(parts[1]).digest();
    const expectedB64 = b64url(expected);
    // 防时序攻击
    const a = Buffer.from(parts[2]);
    const b = Buffer.from(expectedB64);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return null;
    const payload = JSON.parse(b64urlDecode(parts[1]));
    return {
      username: payload.u || "",
      deviceIdPrefix: payload.d || "",
      issuedAt: payload.t || 0,
    };
  } catch (e) {
    return null;
  }
}

/**
 * 验证请求：
 *   - body.deviceToken 必须有效
 *   - token 内的 username/device 与 body 一致
 *
 * @returns { ok: true, identity } 或 { ok:false, code, msg }
 */
function authenticate(body) {
  const token = body && body.deviceToken;
  if (!token) return { ok: false, code: "AUTH_REQUIRED", msg: "请重新登录" };
  const id = verifyDeviceToken(token);
  if (!id) return { ok: false, code: "AUTH_INVALID", msg: "身份凭证无效，请重新登录" };

  const bodyUser = String(body.username || "").trim();
  const bodyDev = String(body.deviceId || "").slice(0, 32);
  if (bodyUser && id.username && bodyUser !== id.username) {
    return { ok: false, code: "AUTH_USER_MISMATCH", msg: "凭证与用户名不匹配" };
  }
  if (bodyDev && id.deviceIdPrefix && bodyDev !== id.deviceIdPrefix) {
    return { ok: false, code: "AUTH_DEVICE_MISMATCH", msg: "凭证与设备不匹配" };
  }
  return { ok: true, identity: id };
}

module.exports = { issueDeviceToken, verifyDeviceToken, authenticate };
