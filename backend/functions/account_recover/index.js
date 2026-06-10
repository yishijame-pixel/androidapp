// ============================================================
// 账号恢复云函数 /account_recover
// ------------------------------------------------------------
// 清本机数据后：username + passwordProof + deviceId
//   → 验密 → 签发 device_token → 返回 nickname + 钱包快照
//
// 入参: { username, passwordProof, deviceId }
// 出参: { ok:true, nickname, deviceToken, wallet } 或 { ok:false, code, msg }
// ============================================================

const cloudbase = require("@cloudbase/node-sdk");
const { rateLimit, extractIp } = require("./rate-limit");
const { issueDeviceToken } = require("./identity");
const { evaluateAccountRecover } = require("./account-recover-core");

const app = cloudbase.init({});
const db = app.database();
const _ = db.command;
const USERS = db.collection("vip_users");
const BANS = db.collection("vip_user_bans");
const MARKS = db.collection("vip_device_marks");
const SNAPSHOTS = db.collection("vip_coin_snapshots");
const RECOVER_LOG = db.collection("vip_recover_log");

async function isBanned(username, deviceId) {
  const keys = [];
  if (username) keys.push("user:" + username);
  if (deviceId) keys.push("device:" + deviceId);
  if (keys.length) {
    const r = await BANS.where({ key: _.in(keys) }).limit(1).get();
    if (r.data && r.data[0]) {
      const hit = r.data[0];
      const scope = hit.key.startsWith("user:") ? "user" : "device";
      const label = scope === "device" ? "该设备已被封禁" : "该账号已被封禁";
      const raw = (hit.reason || "").trim();
      return { banned: true, reason: raw ? `${label}\n原因：${raw}` : label };
    }
  }
  if (deviceId) {
    const m = await MARKS.where({ deviceId, mark: "blacklist" }).limit(1).get();
    if (m.data && m.data[0]) {
      const raw = (m.data[0].note || "").trim();
      return {
        banned: true,
        reason: raw ? `该设备已被加入黑名单\n原因：${raw}` : "该设备已被加入黑名单",
      };
    }
  }
  return { banned: false };
}

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return { ok: false, code: "BAD_REQUEST" }; }
  }

  const username = (body.username || "").trim().slice(0, 64);
  const passwordProof = (body.passwordProof || "").trim().slice(0, 128);
  const deviceId = (body.deviceId || "").trim();
  const ip = extractIp(event);

  if (deviceId) {
    const rlDev = await rateLimit(db, {
      key: "recover:dev:" + deviceId.slice(0, 32),
      limit: 8,
      windowMs: 60_000,
    });
    if (!rlDev.ok) {
      return { ok: false, code: "RATE_LIMITED", msg: "该设备请求过于频繁，请稍后再试" };
    }
  }
  if (username) {
    const rlUser = await rateLimit(db, {
      key: "recover:user:" + username.slice(0, 32),
      limit: 8,
      windowMs: 60_000,
    });
    if (!rlUser.ok) {
      return { ok: false, code: "RATE_LIMITED", msg: "请求过于频繁，请稍后再试" };
    }
  }
  if (ip) {
    const rlIp = await rateLimit(db, {
      key: "recover:ip:" + ip,
      limit: 24,
      windowMs: 60_000,
    });
    if (!rlIp.ok) {
      return { ok: false, code: "RATE_LIMITED", msg: "请求过于频繁，请稍后再试" };
    }
  }

  try {
    const exist = await USERS.where({ username }).limit(1).get();
    const user = exist.data && exist.data[0] ? exist.data[0] : null;

    const ban = await isBanned(username, deviceId);
    let walletSnapshot = null;
    if (user) {
      try {
        const snapR = await SNAPSHOTS.where({ username }).limit(1).get();
        walletSnapshot = snapR.data && snapR.data[0] ? snapR.data[0] : null;
      } catch (e) { /* best-effort */ }
    }

    const verdict = evaluateAccountRecover({
      username,
      passwordProof,
      deviceId,
      user,
      banned: ban.banned,
      banReason: ban.reason,
      walletSnapshot,
    });

    try {
      await RECOVER_LOG.add({
        data: {
          username,
          deviceId: deviceId.slice(0, 32),
          ip: ip || "",
          ok: verdict.ok,
          code: verdict.code || (verdict.ok ? "OK" : "UNKNOWN"),
          flag: verdict.audit && verdict.audit.flag,
          at: db.serverDate(),
        },
      });
    } catch (e) { /* audit best-effort */ }

    if (!verdict.ok) {
      if (user && verdict.audit && verdict.audit.flag) {
        try {
          const flags = Array.isArray(user.flags) ? user.flags.slice() : [];
          if (!flags.includes(verdict.audit.flag)) flags.push(verdict.audit.flag);
          await USERS.where({ username }).update({
            conflictAt: db.serverDate(),
            conflictDeviceId: deviceId,
            flags,
          });
        } catch (e) { /* best-effort */ }
      }
      return {
        ok: false,
        code: verdict.code,
        msg: verdict.msg,
      };
    }

    const patch = {
      nickname: verdict.patchUser.nickname,
      lastSeenAt: db.serverDate(),
      recoveredAt: db.serverDate(),
      recoverCount: verdict.patchUser.recoverCount,
    };
    if (verdict.patchUser.passwordProof) {
      patch.passwordProof = verdict.patchUser.passwordProof;
    }
    try {
      await USERS.where({ username }).update(patch);
    } catch (e) { /* best-effort */ }

    const deviceToken = issueDeviceToken(username, deviceId);
    return {
      ok: true,
      nickname: verdict.nickname,
      deviceToken,
      wallet: verdict.wallet,
      recovered: true,
    };
  } catch (e) {
    return { ok: false, code: "DB_ERROR", msg: e.message };
  }
};
