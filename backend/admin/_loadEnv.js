// 复用 tools/.env 的加载逻辑；Docker 可直接注入环境变量
const fs = require("fs");
const path = require("path");

function hasTcbEnv() {
  return Boolean(
    process.env.TCB_ENV_ID &&
    process.env.TCB_SECRET_ID &&
    process.env.TCB_SECRET_KEY,
  );
}

function loadEnv() {
  if (hasTcbEnv()) return;

  const file = path.join(__dirname, "..", "tools", ".env");
  if (!fs.existsSync(file)) {
    console.error(
      "未找到 backend/tools/.env，且容器/进程未设置 TCB_ENV_ID、TCB_SECRET_ID、TCB_SECRET_KEY",
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

function initTcb() {
  loadEnv();
  if (!hasTcbEnv()) {
    throw new Error("TCB_ENV_ID / TCB_SECRET_ID / TCB_SECRET_KEY 未配置");
  }
  const tcb = require("@cloudbase/node-sdk");
  return tcb.init({
    env: process.env.TCB_ENV_ID,
    secretId: process.env.TCB_SECRET_ID,
    secretKey: process.env.TCB_SECRET_KEY,
  });
}

module.exports = { loadEnv, initTcb };
