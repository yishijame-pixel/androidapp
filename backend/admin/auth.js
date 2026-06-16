// ============================================================
// admin 鉴权：cookie + HMAC 签名（无状态、零数据库）
// ============================================================
const crypto = require("crypto");
const bcrypt = require("bcryptjs");

const COOKIE_NAME = "admin_sess";
const COOKIE_MAX_AGE_MS = 8 * 3600 * 1000; // 8 小时

function getSessionSecret() {
  const s = process.env.ADMIN_SESSION_SECRET;
  if (!s || s.length < 16) {
    throw new Error("ADMIN_SESSION_SECRET 未配置或太短（建议 32 字节随机 hex）");
  }
  return s;
}

function sign(payload) {
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  const sig = crypto.createHmac("sha256", getSessionSecret()).update(body).digest("base64url");
  return body + "." + sig;
}

function verify(token) {
  if (!token || typeof token !== "string" || !token.includes(".")) return null;
  const [body, sig] = token.split(".");
  const expected = crypto.createHmac("sha256", getSessionSecret()).update(body).digest("base64url");
  // 防时序攻击
  try {
    if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) return null;
  } catch { return null; }
  try {
    const payload = JSON.parse(Buffer.from(body, "base64url").toString("utf8"));
    if (!payload.exp || Date.now() > payload.exp) return null;
    return payload;
  } catch { return null; }
}

function checkPassword(username, password) {
  const expectedUser = process.env.ADMIN_USERNAME || "admin";
  const hash = process.env.ADMIN_PASSWORD_HASH;
  if (!hash) throw new Error("ADMIN_PASSWORD_HASH 未配置，请先 node hash-password.js <密码>");
  if (username !== expectedUser) return false;
  return bcrypt.compareSync(password || "", hash);
}

function issueCookie(res, username) {
  const token = sign({ u: username, iat: Date.now(), exp: Date.now() + COOKIE_MAX_AGE_MS });
  res.cookie(COOKIE_NAME, token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.ADMIN_COOKIE_SECURE === "1", // 部署到 HTTPS 时设为 1
    maxAge: COOKIE_MAX_AGE_MS,
    path: "/",
  });
}

function clearCookie(res) {
  res.clearCookie(COOKIE_NAME, { path: "/" });
}

/** Express 中间件：未登录直接 401 / 跳登录页 */
function requireAuth(req, res, next) {
  if (req.method === "POST" && req.path === "/pac_maze_config") {
    return next();
  }
  const payload = verify(req.cookies && req.cookies[COOKIE_NAME]);
  if (!payload) {
    if (req.path.startsWith("/api/")) {
      return res.status(401).json({ ok: false, error: "未登录或会话过期", code: "UNAUTHORIZED" });
    }
    return res.redirect("/login.html");
  }
  req.admin = payload;
  next();
}

// 简单的内存级限流：同 IP 60s 内最多 8 次登录尝试
const loginAttempts = new Map();
function rateLimitLogin(req, res, next) {
  const ip = (req.headers["x-forwarded-for"] || req.ip || "").toString().split(",")[0].trim();
  const now = Date.now();
  const win = 60 * 1000;
  const rec = loginAttempts.get(ip) || { count: 0, ts: now };
  if (now - rec.ts > win) { rec.count = 0; rec.ts = now; }
  rec.count++;
  loginAttempts.set(ip, rec);
  if (rec.count > 8) {
    return res.status(429).json({ ok: false, error: "登录过于频繁，请稍后再试" });
  }
  next();
}

module.exports = {
  COOKIE_NAME,
  checkPassword,
  issueCookie,
  clearCookie,
  requireAuth,
  rateLimitLogin,
};
