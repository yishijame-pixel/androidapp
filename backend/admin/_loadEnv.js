// 复用 tools/.env 的加载逻辑
const fs = require("fs");
const path = require("path");

function loadEnv() {
  // admin 也用 tools/.env，不重复维护
  const file = path.join(__dirname, "..", "tools", ".env");
  if (!fs.existsSync(file)) {
    console.error("未找到 backend/tools/.env，请先按 README 配置工具脚本");
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
  const tcb = require("@cloudbase/node-sdk");
  return tcb.init({
    env: process.env.TCB_ENV_ID,
    secretId: process.env.TCB_SECRET_ID,
    secretKey: process.env.TCB_SECRET_KEY,
  });
}

module.exports = { loadEnv, initTcb };
