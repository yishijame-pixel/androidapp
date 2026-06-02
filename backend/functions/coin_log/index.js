// ============================================================
// 金币变动上报 /coin_log
// ------------------------------------------------------------
// 客户端在金币变动后异步调用一次（fire-and-forget），目的：
//   1) 后台能看到所有用户的金币余额和变动趋势
//   2) 服务端做异常检测（短时暴涨、领奖速度异常）
//   3) 必要时可在后台一键扣回 / 封号
//
// 入参: { username, deviceId, balance, totalEarned, totalSpent,
//        op: "earn|spend|set", amount, reason }
// 出参: { ok:true, suspicious?:true, reason?:"..." }
//
// 注意：不接受 op="set"（防止客户端伪造任意余额）
// ============================================================

const cloudbase = require("@cloudbase/node-sdk");
const { rateLimit, extractIp } = require("./rate-limit");
const { authenticate } = require("./identity");
const app = cloudbase.init({});
const db = app.database();
const _  = db.command;

const SNAPSHOTS = db.collection("vip_coin_snapshots"); // 用户级最新余额（upsert）
const LOGS      = db.collection("vip_coin_logs");      // 全量流水
const USERS     = db.collection("vip_users");          // 用户绑定（注册时由 register_log 写入）
const NONCES    = db.collection("vip_coin_nonces");    // 🔒 nonce 去重表（防重放）

// nonce 时间窗：客户端 ts 偏离服务器超过 10 分钟则拒绝
const NONCE_WINDOW_MS = 10 * 60 * 1000;

// 异常阈值
const SUSPICIOUS_SINGLE_EARN   = 50000;   // 单次入账超过这个数
const SUSPICIOUS_DAILY_EARN    = 200000;  // 单日累计入账超这个数
const SUSPICIOUS_BALANCE       = 500000;  // 余额超过这个数

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try { body = typeof event.body === "string" ? JSON.parse(event.body) : event.body; }
    catch (e) { return { ok: false, code: "BAD_REQUEST" }; }
  }

  const username    = (body.username || "").trim().slice(0, 64);
  const deviceId    = (body.deviceId || "").trim();
  const balance     = Math.max(0, Math.min(1e9, parseInt(body.balance) || 0));
  const totalEarned = Math.max(0, Math.min(1e9, parseInt(body.totalEarned) || 0));
  const totalSpent  = Math.max(0, Math.min(1e9, parseInt(body.totalSpent) || 0));
  const op          = String(body.op || "").trim();        // earn | spend | point_earn | point_spend
  const amount      = Math.max(0, Math.min(1e9, parseInt(body.amount) || 0));
  const reason      = (body.reason || "").trim().slice(0, 64);
  const nonce       = String(body.nonce || "").trim().slice(0, 64);
  const clientTs    = parseInt(body.ts) || 0;
  // 🔒 积分余额（可选）：仅在 point_earn/point_spend 时使用
  const pointsBalance = Math.max(0, Math.min(1e9, parseInt(body.pointsBalance) || 0));

  // 🔒 限流：单设备 60s 最多 60 次（必须在所有 return 之前，否则攻击者发垃圾请求可绕过）
  const ip = extractIp(event);
  const rl = await rateLimit(db, {
    key: "coinlog:" + (ip || (deviceId && deviceId.slice(0, 16)) || "anon"),
    limit: 60, windowMs: 60_000,
  });
  if (!rl.ok) return { ok: false, code: "RATE_LIMITED", msg: "请求过于频繁" };

  // 基本输入校验（在限流之后做，保证恶意垃圾包先消耗配额）
  if (!username || !deviceId) return { ok: false, code: "INVALID" };
  if (!["earn", "spend", "point_earn", "point_spend", "snapshot"].includes(op)) {
    return { ok: false, code: "INVALID_OP" };
  }
  const isPointOp = (op === "point_earn" || op === "point_spend");
  const isSnapshot = (op === "snapshot");

  if (nonce && clientTs > 0) {
    const drift = Math.abs(Date.now() - clientTs);
    if (drift > NONCE_WINDOW_MS) {
      return { ok: false, code: "TS_OUT_OF_WINDOW", msg: "时间戳偏离过大" };
    }
  }

  // 🔒 身份验证：必须带服务端签发的 deviceToken（注册时拿到）
  //    deviceToken 内含 username + deviceId 前缀，验签后必须与 body 一致
  //    → 攻击者即使知道 username 也无法伪造请求（拿不到对应 token）
  const auth = authenticate({ ...body, deviceId: deviceId.slice(0, 32) });
  if (!auth.ok) {
    try {
      await LOGS.add({ data: {
        username, deviceId, op, amount, reason,
        flags: ["AUTH_" + auth.code],
        at: db.serverDate(),
      }});
    } catch (e) {}
    return { ok: false, code: auth.code, msg: auth.msg };
  }

  // 🔒 鉴权通过后做 nonce 去重（防止未鉴权污染表）
  if (nonce && clientTs > 0) {
    try {
      const exist = await NONCES.doc(nonce).get().catch(() => null);
      const hit = exist && exist.data && (Array.isArray(exist.data) ? exist.data.length > 0 : !!exist.data);
      if (hit) {
        return { ok: false, code: "REPLAY", msg: "重复请求" };
      }
      await NONCES.doc(nonce).set({
        username, deviceId, op, amount, ts: clientTs, at: db.serverDate(),
      });
    } catch (e) { /* 去重表异常不阻断主流程 */ }
  }

  const flags = [];
  // 🔒 snapshot：与历史 snapshot 对比，若 pointsBalance 凭空增加 > 阈值则标记
  if (isSnapshot) {
    try {
      const prev = await SNAPSHOTS.where({ username }).limit(1).get();
      const prevDoc = (prev.data || [])[0];
      if (prevDoc) {
        const prevPts = parseInt(prevDoc.pointsBalance) || 0;
        const prevBal = parseInt(prevDoc.balance) || 0;
        if (pointsBalance - prevPts > 200) flags.push("POINT_JUMP");
        if (balance - prevBal > 50000) flags.push("COIN_JUMP");
      }
    } catch (e) {}
  }
  // 异常 1：单次入账过大【金币】
  if (op === "earn" && amount > SUSPICIOUS_SINGLE_EARN) {
    flags.push("BIG_SINGLE_EARN");
  }
  // 异常 1b：单次积分获取过大【积分】
  //   商店购买送 5 分、转盘最高 10 分，超 50 即极可疑
  if (op === "point_earn" && amount > 50) {
    flags.push("BIG_SINGLE_POINT_EARN");
  }
  // 异常 2：金币余额过大
  if (balance > SUSPICIOUS_BALANCE) {
    flags.push("BIG_BALANCE");
  }
  // 异常 2b：积分余额过大（1 万分几乎不可能是正常获取）
  if (isPointOp && pointsBalance > 10000) {
    flags.push("BIG_POINT_BALANCE");
  }
  // 异常 3：当日累计入账过大（查询过去 24h 的入账总和）
  try {
    if (op === "earn") {
      const since = Date.now() - 24 * 3600 * 1000;
      const r = await LOGS.where({
        username, op: "earn", at: _.gte(new Date(since)),
      }).limit(500).get();
      const today = (r.data || []).reduce((s, x) => s + (x.amount || 0), 0) + amount;
      if (today > SUSPICIOUS_DAILY_EARN) flags.push("BIG_DAILY_EARN");
    }
    // 积分同样查 24h 累计
    if (op === "point_earn") {
      const since = Date.now() - 24 * 3600 * 1000;
      const r = await LOGS.where({
        username, op: "point_earn", at: _.gte(new Date(since)),
      }).limit(500).get();
      const today = (r.data || []).reduce((s, x) => s + (x.amount || 0), 0) + amount;
      if (today > 200) flags.push("BIG_DAILY_POINT_EARN"); // 24h 打到 200 分已是极限
    }
  } catch (e) {}

  // 写流水（best-effort）—— snapshot 不写流水（避免噪音），但保留 flags 到快照
  if (!isSnapshot) {
    try {
      const logData = {
        username, deviceId, balance, totalEarned, totalSpent,
        op, amount, reason, flags,
        at: db.serverDate(),
      };
      if (isPointOp) logData.pointsBalance = pointsBalance;
      await LOGS.add({ data: logData });
    } catch (e) {}
  }

  // upsert 快照（按 username）
  try {
    const baseSnap = { deviceId, balance, totalEarned, totalSpent, flags };
    // 积分类 op 与启动快照都写入 pointsBalance
    if (isPointOp || isSnapshot) baseSnap.pointsBalance = pointsBalance;
    const exist = await SNAPSHOTS.where({ username }).limit(1).get();
    if (exist.data && exist.data[0]) {
      await SNAPSHOTS.where({ username }).update({
        ...baseSnap,
        updatedAt: db.serverDate(),
      });
    } else {
      const docId = ("u_" + username).replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 60);
      await SNAPSHOTS.doc(docId).set({
        username, ...baseSnap,
        createdAt: db.serverDate(),
        updatedAt: db.serverDate(),
      });
    }
  } catch (e) {
    return { ok: false, code: "DB_ERROR", msg: e.message };
  }

  return { ok: true, suspicious: flags.length > 0, flags };
};
