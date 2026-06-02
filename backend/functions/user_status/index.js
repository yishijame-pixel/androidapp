// ============================================================
// 用户/设备封号状态查询 /user_status
// ------------------------------------------------------------
// 客户端登录成功 / 启动时调用。
// 检查规则（任一命中即返回 banned）：
//   1) vip_user_bans 里 key = "user:用户名" 存在
//   2) vip_user_bans 里 key = "device:设备指纹" 存在
//   3) vip_device_marks 里该设备 mark = "blacklist"
//
// 入参: { username, deviceId }
// 出参成功: { ok:true, banned:false }
//          { ok:true, banned:true, reason:"...", scope:"user|device" }
// ============================================================

const cloudbase = require("@cloudbase/node-sdk");
const app = cloudbase.init({});
const db = app.database();
const _ = db.command;
const BANS = db.collection("vip_user_bans");
const MARKS = db.collection("vip_device_marks");

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return { ok: false, code: "BAD_REQUEST" }; }
  }

  const username = (body.username || "").trim().slice(0, 64);
  const deviceId = (body.deviceId || "").trim();

  if (!username && !deviceId) return { ok: true, banned: false };

  try {
    // 一次查 bans
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
        const reasonText = raw ? `${label}\n原因：${raw}` : label;
        return {
          ok: true,
          banned: true,
          reason: reasonText,
          rawReason: raw,
          scope,
          bannedAt: hit.bannedAt,
        };
      }
    }

    // 兼容旧：vip_device_marks 黑名单
    if (deviceId) {
      const m = await MARKS.where({ deviceId, mark: "blacklist" }).limit(1).get();
      if (m.data && m.data[0]) {
        const raw = (m.data[0].note || "").trim();
        return {
          ok: true, banned: true,
          reason: raw ? `该设备已被加入黑名单\n原因：${raw}` : "该设备已被加入黑名单",
          rawReason: raw,
          scope: "device",
          bannedAt: m.data[0].updatedAt || m.data[0].createdAt,
        };
      }
    }

    return { ok: true, banned: false };
  } catch (e) {
    // 查询失败 → 不封禁，避免数据库故障导致大面积封号
    return { ok: true, banned: false, queryError: e.message };
  }
};
