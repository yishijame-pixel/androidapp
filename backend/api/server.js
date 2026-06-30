/**
 * FunLife VIP / AI API 网关
 * 自托管替代腾讯云 CloudBase 云函数；DATABASE_URL 启用 PostgreSQL 兼容层。
 *
 * 本地: cd backend/api && npm install && DATABASE_URL=... HMAC_SECRET=... node server.js
 */
const path = require("path");
const express = require("express");

// 加载 backend/tools/.env
require("../admin/_loadEnv").loadEnv();

if (!require("../shared/db/install-shim")()) {
  console.warn("[funlife-api] 未设置 DATABASE_URL 或 TCB_*，云函数数据库调用将失败");
}

const ROOT = path.join(__dirname, "..", "functions");

const ROUTES = [
  "redeem",
  "verify",
  "migrate",
  "chat_ai",
  "letter_ai",
  "vip_config",
  "coin_log",
  "register_log",
  "user_status",
  "account_recover",
  "beta_validate",
  "postcard_drift",
  "quote_galaxy",
  "pac_maze_config",
];

function wrapEvent(req) {
  return {
    body: req.body,
    headers: req.headers,
    httpMethod: req.method,
    path: req.path,
    requestContext: { headers: req.headers },
  };
}

async function invokeHandler(name, event) {
  const mod = require(path.join(ROOT, name, "index.js"));
  const fn = mod.main || mod.exports?.main;
  if (typeof fn !== "function") throw new Error(`handler ${name} has no exports.main`);
  return fn(event);
}

const app = express();
app.use(express.json({ limit: "2mb" }));

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "funlife-api",
    db: process.env.DATABASE_URL ? "postgres" : "cloudbase",
  });
});

for (const name of ROUTES) {
  app.post(`/${name}`, async (req, res) => {
    try {
      const out = await invokeHandler(name, wrapEvent(req));
      res.status(200).json(out);
    } catch (e) {
      console.error(`[${name}]`, e);
      res.status(500).json({ ok: false, code: "INTERNAL", msg: e.message || "server error" });
    }
  });
  app.get(`/${name}`, async (req, res) => {
    try {
      const out = await invokeHandler(name, wrapEvent({ ...req, body: req.query }));
      res.status(200).json(out);
    } catch (e) {
      console.error(`[${name}]`, e);
      res.status(500).json({ ok: false, code: "INTERNAL", msg: e.message || "server error" });
    }
  });
}

const port = Number(process.env.PORT || 3400);
const host = process.env.HOST || "0.0.0.0";
app.listen(port, host, () => {
  console.log(`\n  FunLife API → http://${host === "0.0.0.0" ? "localhost" : host}:${port}`);
  console.log(`  DB: ${process.env.DATABASE_URL ? "PostgreSQL" : "CloudBase"}`);
  console.log(`  Routes: ${ROUTES.map((r) => "/" + r).join(", ")}\n`);
});
