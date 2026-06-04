/**
 * FunLife FCM 中继 — PocketBase Hook 无法直接调 Google API，由此服务转发。
 *
 * 环境变量：
 *   FCM_SERVICE_ACCOUNT  — Firebase 服务账号 JSON 文件路径，或 JSON 字符串
 *   FCM_RELAY_KEY          — 可选，与 PocketBase FCM_RELAY_KEY 一致
 *   PORT                   — 默认 8787
 *
 * 启动：npm install && npm start
 * PocketBase：$env:FCM_RELAY_URL = "http://127.0.0.1:8787/push"
 */

const http = require("http");
const admin = require("firebase-admin");

const PORT = Number(process.env.PORT || 8787);
const RELAY_KEY = process.env.FCM_RELAY_KEY || "";

function loadServiceAccount() {
  const raw = process.env.FCM_SERVICE_ACCOUNT;
  if (!raw) {
    throw new Error("FCM_SERVICE_ACCOUNT is required (file path or JSON string)");
  }
  if (raw.trim().startsWith("{")) {
    return JSON.parse(raw);
  }
  // eslint-disable-next-line global-require, import/no-dynamic-require
  return require(require("path").resolve(raw));
}

const serviceAccount = loadServiceAccount();
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}

function unauthorized(res) {
  res.writeHead(401, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ ok: false, error: "unauthorized" }));
}

function badRequest(res, msg) {
  res.writeHead(400, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ ok: false, error: msg }));
}

function checkAuth(req) {
  if (!RELAY_KEY) return true;
  const auth = req.headers.authorization || "";
  return auth === `Bearer ${RELAY_KEY}`;
}

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ ok: true }));
    return;
  }

  if (req.method !== "POST" || req.url !== "/push") {
    res.writeHead(404, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ ok: false, error: "not_found" }));
    return;
  }

  if (!checkAuth(req)) {
    unauthorized(res);
    return;
  }

  let payload;
  try {
    payload = JSON.parse(await readBody(req));
  } catch {
    badRequest(res, "invalid_json");
    return;
  }

  const token = payload.token;
  if (!token) {
    badRequest(res, "missing_token");
    return;
  }

  const data = payload.data || {};
  const stringData = {};
  for (const [k, v] of Object.entries(data)) {
    stringData[k] = v == null ? "" : String(v);
  }

  const message = {
    token,
    data: stringData,
    android: {
      priority: "high",
    },
  };

  // 数据消息为主；若 Hook 带了 title/body 则同时带 notification（部分 ROM 需此才弹窗）
  if (payload.title || payload.body) {
    message.notification = {
      title: payload.title || "FunLife",
      body: payload.body || "",
    };
    message.android.notification = {
      channelId: "fun_social",
    };
  }

  try {
    const id = await admin.messaging().send(message);
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ ok: true, messageId: id }));
  } catch (err) {
    console.error("[fcm_relay] send failed:", err.message);
    res.writeHead(502, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ ok: false, error: err.message }));
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`[fcm_relay] listening on http://0.0.0.0:${PORT}/push`);
});
