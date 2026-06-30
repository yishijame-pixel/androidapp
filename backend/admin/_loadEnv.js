// 复用 tools/.env；支持 DATABASE_URL（PostgreSQL）或 TCB_*（CloudBase）
const fs = require("fs");
const path = require("path");

function hasTcbEnv() {
  return Boolean(
    process.env.TCB_ENV_ID &&
    process.env.TCB_SECRET_ID &&
    process.env.TCB_SECRET_KEY,
  );
}

function hasPostgres() {
  return Boolean(process.env.DATABASE_URL);
}

function loadEnv() {
  if (hasTcbEnv() || hasPostgres()) return;

  const file = path.join(__dirname, "..", "tools", ".env");
  if (!fs.existsSync(file)) {
    if (process.env.NODE_ENV === "production") {
      console.warn("[loadEnv] 无 backend/tools/.env，依赖容器环境变量");
      return;
    }
    console.error(
      "未找到 backend/tools/.env，且未设置 DATABASE_URL 或 TCB_*",
    );
    process.exit(1);
  }
  fs.readFileSync(file, "utf-8").split(/\r?\n/).forEach((line) => {
    const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
    if (m && !line.trim().startsWith("#")) {
      const k = m[1];
      let v = m[2];
      if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
        v = v.slice(1, -1);
      }
      if (!process.env[k]) process.env[k] = v;
    }
  });
}

function installDbShimIfNeeded() {
  if (!hasPostgres()) return false;
  return require(path.join(__dirname, "..", "shared", "db", "install-shim"))();
}

function initDatabase() {
  loadEnv();
  if (hasPostgres()) {
    installDbShimIfNeeded();
    const tcb = require("@cloudbase/node-sdk");
    return tcb.init({ env: process.env.TCB_ENV_ID || "funlife-local" });
  }
  if (!hasTcbEnv()) {
    throw new Error("需要 DATABASE_URL 或 TCB_ENV_ID/TCB_SECRET_ID/TCB_SECRET_KEY");
  }
  const tcb = require("@cloudbase/node-sdk");
  return tcb.init({
    env: process.env.TCB_ENV_ID,
    secretId: process.env.TCB_SECRET_ID,
    secretKey: process.env.TCB_SECRET_KEY,
  });
}

/** @deprecated 使用 initDatabase */
function initTcb() {
  return initDatabase();
}

module.exports = { loadEnv, initTcb, initDatabase, installDbShimIfNeeded, hasPostgres };
