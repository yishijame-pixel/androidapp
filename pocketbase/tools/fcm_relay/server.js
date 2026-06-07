/**
 * FunLife FCM 中继 — PocketBase Hook 无法直接调 Google API，由此服务转发。
 * 推送为尽力而为：任何失败均快速返回 HTTP 200 + { ok:false }，不阻塞业务 hook。
 */

const http = require("http");
const admin = require("firebase-admin");

const PORT = Number(process.env.PORT || 8787);
const RELAY_KEY = process.env.FCM_RELAY_KEY || "";
const FCM_SEND_TIMEOUT_MS = 2500;
const REQUEST_TIMEOUT_MS = 3500;

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

function reply(res, status, payload) {
  if (res.headersSent) return;
  res.writeHead(status, { "Content-Type": "application/json" });
  res.end(JSON.stringify(payload));
}

function deferred(res, error, tokenPrefix) {
  const msg = error || "send_failed";
  console.warn("[fcm_relay] deferred:", msg.slice(0, 160), "token=" + (tokenPrefix || ""));
  reply(res, 200, { ok: false, error: msg, ignored: true });
}

function withTimeout(promise, ms, label) {
  return Promise.race([
    promise,
    new Promise((_, reject) => {
      setTimeout(() => reject(new Error(label || "timeout")), ms);
    }),
  ]);
}

const server = http.createServer(async (req, res) => {
  req.setTimeout(REQUEST_TIMEOUT_MS, () => {
    deferred(res, "request_timeout");
  });

  if (req.method === "GET" && req.url === "/health") {
    reply(res, 200, { ok: true });
    return;
  }

  if (req.method !== "POST" || req.url !== "/push") {
    reply(res, 404, { ok: false, error: "not_found" });
    return;
  }

  if (RELAY_KEY) {
    const auth = req.headers.authorization || "";
    if (auth !== `Bearer ${RELAY_KEY}`) {
      reply(res, 401, { ok: false, error: "unauthorized" });
      return;
    }
  }

  let payload;
  try {
    payload = JSON.parse(await readBody(req));
  } catch {
    reply(res, 400, { ok: false, error: "invalid_json" });
    return;
  }

  const token = payload.token;
  if (!token) {
    reply(res, 400, { ok: false, error: "missing_token" });
    return;
  }

  const tokenPrefix = String(token).slice(0, 12);
  const data = payload.data || {};
  const stringData = {};
  for (const [k, v] of Object.entries(data)) {
    stringData[k] = v == null ? "" : String(v);
  }

  const message = {
    token,
    data: stringData,
    android: { priority: "high" },
  };

  if (payload.title || payload.body) {
    message.notification = {
      title: payload.title || "FunLife",
      body: payload.body || "",
    };
    message.android.notification = { channelId: "fun_social" };
  }

  try {
    const id = await withTimeout(
      admin.messaging().send(message),
      FCM_SEND_TIMEOUT_MS,
      "fcm_send_timeout",
    );
    reply(res, 200, { ok: true, messageId: id });
  } catch (err) {
    deferred(res, err.message || String(err), tokenPrefix);
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`[fcm_relay] listening on http://0.0.0.0:${PORT}/push (timeout=${FCM_SEND_TIMEOUT_MS}ms)`);
});
