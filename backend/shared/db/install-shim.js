/**
 * 拦截 @cloudbase/node-sdk：
 * - DATABASE_URL → PostgreSQL 兼容 database()
 * - TCB_SECRET_* → 注入凭证，兼容云函数 tcb.init({ env: SYMBOL_CURRENT_ENV })
 */
const fs = require("fs");
const path = require("path");

function resolveTcbSdkPath() {
  const root = path.join(__dirname, "..", "..");
  const candidates = [
    path.join(root, "api", "node_modules", "@cloudbase", "node-sdk"),
    path.join(root, "admin", "node_modules", "@cloudbase", "node-sdk"),
    path.join(root, "tools", "node_modules", "@cloudbase", "node-sdk"),
    path.join(root, "functions", "redeem", "node_modules", "@cloudbase", "node-sdk"),
  ];
  for (const p of candidates) {
    if (fs.existsSync(path.join(p, "package.json"))) return p;
  }
  return "@cloudbase/node-sdk";
}

function installShim() {
  if (global.__FUNLIFE_DB_SHIM__) return true;

  const hasPg = Boolean(process.env.DATABASE_URL);
  const hasTcb =
    Boolean(process.env.TCB_ENV_ID) &&
    Boolean(process.env.TCB_SECRET_ID) &&
    Boolean(process.env.TCB_SECRET_KEY);

  if (!hasPg && !hasTcb) return false;

  const Module = require("module");
  const originalLoad = Module._load;

  let realTcb = null;
  function loadRealTcb() {
    if (!realTcb) {
      const resolved = resolveTcbSdkPath();
      realTcb = originalLoad.call(Module, resolved, Module, false);
    }
    return realTcb;
  }

  let bridge = null;
  function getBridge() {
    if (bridge) return bridge;

    if (hasPg) {
      bridge = {
        SYMBOL_CURRENT_ENV: process.env.TCB_ENV_ID || "funlife-local",
        init: () => ({
          database: () => require("./postgres").createDatabase(),
        }),
      };
      console.log(
        "[db-shim] PostgreSQL:",
        String(process.env.DATABASE_URL).replace(/:[^:@/]+@/, ":***@"),
      );
      return bridge;
    }

    const sdk = loadRealTcb();
    const envId = process.env.TCB_ENV_ID;
    bridge = {
      ...sdk,
      SYMBOL_CURRENT_ENV: envId,
      init: (cfg = {}) =>
        sdk.init({
          env: cfg.env || envId,
          secretId: process.env.TCB_SECRET_ID,
          secretKey: process.env.TCB_SECRET_KEY,
          ...cfg,
        }),
    };
    console.log("[db-shim] CloudBase 凭证注入:", envId);
    return bridge;
  }

  Module._load = function patchedLoad(request, parent, isMain) {
    if (request === "@cloudbase/node-sdk") {
      return getBridge();
    }
    return originalLoad.call(this, request, parent, isMain);
  };

  global.__FUNLIFE_DB_SHIM__ = true;
  return true;
}

module.exports = installShim;
