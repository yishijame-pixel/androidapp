// 最小化的 .env 加载（不引入额外依赖）
const fs = require("fs");
const path = require("path");

function loadEnv() {
  const file = path.join(__dirname, ".env");
  if (!fs.existsSync(file)) {
    console.error("缺少 .env 文件，请复制 .env.example 为 .env 并填值");
    process.exit(1);
  }
  const text = fs.readFileSync(file, "utf-8");
  text.split(/\r?\n/).forEach((line) => {
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
  const envId = process.env.TCB_ENV_ID;
  const secretId = process.env.TCB_SECRET_ID;
  const secretKey = process.env.TCB_SECRET_KEY;
  if (!envId || !secretId || !secretKey) {
    console.error("TCB_ENV_ID / TCB_SECRET_ID / TCB_SECRET_KEY 必须配置");
    process.exit(1);
  }
  return tcb.init({ env: envId, secretId, secretKey });
}

module.exports = { loadEnv, initTcb };
