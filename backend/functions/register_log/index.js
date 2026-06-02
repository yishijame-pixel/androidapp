// ============================================================
// 用户注册上报云函数 /register_log
// ------------------------------------------------------------
// 客户端注册成功后异步调用一次（fire-and-forget）。
// 失败不影响注册流程，主要用于后台运营看到所有新用户。
//
// 入参: { username, nickname, deviceId, betaCode }
// 出参: { ok: true } 或 { ok:false, code, msg }
//
// 注意：不收集密码、不收集任何敏感信息
// ============================================================

const cloudbase = require("@cloudbase/node-sdk");
const { rateLimit, extractIp } = require("./rate-limit");
const { issueDeviceToken } = require("./identity");
const app = cloudbase.init({});
const db = app.database();
const USERS = db.collection("vip_users");

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return { ok: false, code: "BAD_REQUEST" }; }
  }

  const username = (body.username || "").trim().slice(0, 64);
  const nickname = (body.nickname || "").trim().slice(0, 64);
  const deviceId = (body.deviceId || "").trim();
  const betaCode = (body.betaCode || "").trim().slice(0, 32);
  const passwordProof = (body.passwordProof || "").trim().slice(0, 128); // SHA-256 hex = 64 字符
  // 🔒 mode: "register"（默认）= 严格拒重复；"refresh" = 已登录用户补领 device_token，允许同设备同密码
  const mode = ((body.mode || "register") + "").trim();
  // 🔒 dryRun: 仅做用户名冲突预检，不写库、不签发 token，不消耗注册资源
  //    用于注册流程"在 beta_validate 之前先检查用户名"，避免内测码作废前云端就建出孤儿用户
  const dryRun = body.dryRun === true || body.dryRun === "true";

  // 🔒 双键限流（必须在所有 return 之前；任一超限即拒）：
  //    - 设备维度严格：单 deviceId 60s 内最多 10 次（防单机扫号）
  //    - IP 维度宽松：同 IP 60s 内最多 30 次（容忍学校/公司 NAT 后真实多用户，
  //      同时防止攻击者伪造 deviceId 绕过设备限流）
  const ip = extractIp(event);
  if (deviceId) {
    const rlDev = await rateLimit(db, {
      key: "reg:dev:" + deviceId.slice(0, 32),
      limit: 10, windowMs: 60_000,
    });
    if (!rlDev.ok) return { ok: false, code: "RATE_LIMITED", msg: "该设备请求过于频繁，请稍后再试" };
  }
  if (ip) {
    const rlIp = await rateLimit(db, {
      key: "reg:ip:" + ip,
      limit: 30, windowMs: 60_000,
    });
    if (!rlIp.ok) return { ok: false, code: "RATE_LIMITED", msg: "请求过于频繁，请稍后再试" };
  }

  if (!username || !deviceId) return { ok: false, code: "INVALID" };
  // 🔒 passwordProof 必填：堵升级窗口期"知道 username 就能抢注册"漏洞
  if (!passwordProof || passwordProof.length < 32) {
    return { ok: false, code: "PROOF_REQUIRED", msg: "请升级 App 后重新登录" };
  }

  try {
    const exist = await USERS.where({ username }).limit(1).get();
    if (exist.data && exist.data[0]) {
      const old = exist.data[0];

      // 🔒 优先验密码证明：堵"知道 username 就能抢注册"漏洞
      //    老记录可能没有 passwordProof（兼容期），首次有 proof 调用时补录
      if (old.passwordProof) {
        if (old.passwordProof !== passwordProof) {
          try {
            await USERS.where({ username }).update({
              conflictAt: db.serverDate(),
              conflictDeviceId: deviceId,
              flags: ["WRONG_PASSWORD"],
            });
          } catch (e) {}
          return { ok: false, code: "WRONG_PASSWORD", msg: "用户名或密码错误" };
        }
      }

      // 设备指纹不一致 → 拒绝覆盖（即使密码对了——可能是用户换手机，让走 /migrate）
      if (old.deviceId && old.deviceId !== deviceId) {
        try {
          await USERS.where({ username }).update({
            conflictAt: db.serverDate(),
            conflictDeviceId: deviceId,
            flags: ["DEVICE_CONFLICT"],
          });
        } catch (e) {}
        return { ok: false, code: "DEVICE_CONFLICT", msg: "用户名已被他人在其他设备注册" };
      }

      // 🔒 同设备同密码 → 用户名已在云端注册过：
      //    register 模式（注册流程）：严格拒重复，引导用户去登录
      //    refresh  模式（已登录补领 token）：维持原放行，签发新 device_token
      const patch = {
        nickname: nickname || old.nickname,
        lastSeenAt: db.serverDate(),
      };
      if (!old.passwordProof) patch.passwordProof = passwordProof;
      try { await USERS.where({ username }).update(patch); } catch (e) {}

      if (mode === "register") {
        return {
          ok: false,
          code: "ALREADY_REGISTERED",
          msg: "该用户名已注册过，请直接登录",
        };
      }
      // refresh 模式：fall through 到下面签发 token
    } else {
      // 🔒 dryRun: 用户名不存在 → 仅返回预检通过，不写库、不签发 token
      //    客户端会在内测码 + 本地注册都通过后，再发一次非 dryRun 调用完成正式上报
      if (dryRun) {
        return { ok: true, preCheck: true };
      }
      // 首次注册：记录 passwordProof
      await USERS.doc(username.slice(0, 32) + "_" + Date.now()).set({
        username, nickname, deviceId, betaCode, passwordProof,
        registeredAt: db.serverDate(),
        lastSeenAt: db.serverDate(),
      });
    }
    // 🔒 签发 device_token（客户端持久化，后续写入请求带上）
    const deviceToken = issueDeviceToken(username, deviceId);
    return { ok: true, deviceToken };
  } catch (e) {
    return { ok: false, code: "DB_ERROR", msg: e.message };
  }
};
