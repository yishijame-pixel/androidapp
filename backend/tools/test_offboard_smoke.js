#!/usr/bin/env node
/**
 * 离云迁移后 · 核心功能冒烟测试（api.yishi.site + PostgreSQL）
 *
 * 用法:
 *   cd backend/tools && node test_offboard_smoke.js
 *   VIP_BACKEND_URL=https://api.yishi.site node test_offboard_smoke.js
 */
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const https = require("https");
const { URL } = require("url");

const BASE = (() => {
  if (process.env.VIP_BACKEND_URL) return process.env.VIP_BACKEND_URL.replace(/\/$/, "");
  const lp = path.join(__dirname, "..", "..", "local.properties");
  if (fs.existsSync(lp)) {
    const m = fs.readFileSync(lp, "utf-8").match(/VIP_BACKEND_URL=(.+)/);
    if (m) return m[1].trim().replace(/\/$/, "");
  }
  return "https://api.yishi.site";
})();

const HMAC_SECRET =
  process.env.HMAC_SECRET ||
  process.env.VIP_HMAC_SECRET ||
  (() => {
    const lp = path.join(__dirname, "..", "..", "local.properties");
    const m = fs.readFileSync(lp, "utf-8").match(/VIP_HMAC_SECRET=(.+)/);
    return m ? m[1].trim() : "";
  })();

function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach((k) => {
    sorted[k] = obj[k];
  });
  return JSON.stringify(sorted);
}
function sign(obj) {
  return crypto.createHmac("sha256", HMAC_SECRET).update(canonicalJson(obj)).digest("hex");
}
function normalizeCode(c) {
  return String(c || "").trim().toUpperCase().replace(/[\s\-_]/g, "");
}

function request(method, urlPath, body) {
  return new Promise((resolve, reject) => {
    const u = new URL(BASE + urlPath);
    const data = body != null ? JSON.stringify(body) : null;
    const req = https.request(
      {
        method,
        hostname: u.hostname,
        port: u.port || 443,
        path: u.pathname + (u.search || ""),
        headers: data
          ? { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) }
          : {},
        timeout: 30000,
      },
      (res) => {
        let buf = "";
        res.on("data", (c) => {
          buf += c;
        });
        res.on("end", () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(buf) });
          } catch {
            resolve({ status: res.statusCode, body: buf, raw: true });
          }
        });
      },
    );
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("TIMEOUT")));
    if (data) req.write(data);
    req.end();
  });
}
const post = (p, b) => request("POST", p, b);
const get = (p) => request("GET", p, null);

let pass = 0;
let fail = 0;
function expect(name, ok, got) {
  if (ok) {
    pass++;
    console.log(`  ✅ ${name}`);
  } else {
    fail++;
    console.error(`  ❌ ${name}`);
    if (got !== undefined) console.error("     ", typeof got === "object" ? JSON.stringify(got).slice(0, 400) : got);
  }
}

async function pickUnusedVipCode() {
  if (process.env.SMOKE_TEST_CODE) return normalizeCode(process.env.SMOKE_TEST_CODE);
  const pgPass = process.env.POSTGRES_PASSWORD;
  if (pgPass) {
    const { getPool, closePool } = require("../shared/db/postgres");
    process.env.DATABASE_URL =
      process.env.DATABASE_URL ||
      `postgres://funlife:${pgPass}@127.0.0.1:5432/funlife_vip`;
    const pool = getPool();
    const r = await pool.query(
      `SELECT data->>'code' AS code FROM documents
       WHERE collection='vip_codes' AND data->>'status'='unused'
         AND (data->>'disabled')::boolean IS NOT TRUE
         AND data->>'skuCode' LIKE 'VIP_%'
       LIMIT 1`,
    );
    await closePool();
    if (r.rows[0]?.code) return r.rows[0].code;
  }
  return null;
}

async function main() {
  console.log("\n========== FunLife 离云冒烟测试 ==========");
  console.log("BASE:", BASE);
  console.log("HMAC:", HMAC_SECRET ? "已配置" : "缺失");

  // 1. health
  {
    const r = await get("/health");
    expect("GET /health → ok", r.body?.ok === true, r.body);
    expect("GET /health → db=postgres", r.body?.db === "postgres", r.body);
  }

  // 2. vip_config
  {
    const r = await post("/vip_config", {});
    expect("POST /vip_config → ok", r.body?.ok === true, r.body);
    expect("POST /vip_config → vipLevels.1", !!r.body?.data?.vipLevels?.["1"], r.body?.data);
  }

  // 3. pac_maze_config
  {
    const r = await post("/pac_maze_config", {});
    expect("POST /pac_maze_config → ok", r.body?.ok === true, r.body);
    expect("POST /pac_maze_config → title", !!r.body?.data?.title, r.body?.data);
  }

  // 4. redeem invalid
  const deviceId = "smoke_" + crypto.randomBytes(16).toString("hex");
  {
    const r = await post("/redeem", { code: "FL-INVALID-SMOKE-0000", deviceId, userId: 900001 });
    expect("POST /redeem 无效码 → INVALID", r.body?.code === "INVALID", r.body);
  }

  // 5. redeem + verify 迁移卡密
  const testCode = await pickUnusedVipCode();
  if (!testCode) {
    console.warn("  ⚠ 跳过 redeem/verify（无 POSTGRES_PASSWORD 或无 unused VIP 卡）");
  } else {
    console.log(`  · 测试卡密: ${testCode.slice(0, 4)}…${testCode.slice(-4)} (unused VIP)`);
    let cert;
    let signature;
    {
      const r = await post("/redeem", { code: testCode, deviceId, userId: 900002 });
      expect("POST /redeem 历史卡 → ok", r.body?.ok === true, r.body);
      cert = r.body?.certificate;
      signature = r.body?.signature;
      expect("POST /redeem → certificate+signature", !!cert && !!signature, r.body);
      if (cert) {
        expect("redeem 签名可本地复验", sign(cert) === signature, null);
        expect("certificate.skuCode 存在", !!cert.skuCode, cert);
      }
    }
    if (cert && signature) {
      const r = await post("/verify", { certificate: cert, signature, deviceId });
      expect("POST /verify → ok", r.body?.ok === true, r.body);
      const r2 = await post("/redeem", { code: testCode, deviceId, userId: 900002 });
      expect("POST /redeem 同设备复兑 → isReissue", r2.body?.ok && r2.body?.isReissue === true, r2.body);
    }
  }

  // 6. user_status
  {
    const r = await post("/user_status", { deviceId });
    expect("POST /user_status → 有响应", r.body && typeof r.body === "object", r.body);
  }

  // 7. register_log dryRun
  {
    const ts = Date.now();
    const proof = crypto.createHash("sha256").update(`FunLifeAuth|smoke_${ts}|Test123!`, "utf8").digest("hex");
    const r = await post("/register_log", {
      username: `smoke_${ts}`,
      nickname: "冒烟",
      deviceId,
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: true,
    });
    expect("POST /register_log dryRun → ok 或业务码", r.body?.ok === true || !!r.body?.code, r.body);
  }

  // 8. chat_ai 鉴权（不调用 LLM）
  {
    const r = await post("/chat_ai", { body: { mode: "chat", userText: "hi" } });
    expect("POST /chat_ai 无凭证 → INVALID/BAD", r.body?.code === "INVALID" || r.body?.code === "BAD_SIGNATURE", r.body);
  }

  // 9. assets manifest（静态站，非 API）
  {
    const manifestUrl = process.env.ASSET_MANIFEST_URL || "https://assets.yishi.site/manifest.json";
    const r = await new Promise((resolve, reject) => {
      https
        .get(manifestUrl, { timeout: 15000 }, (res) => {
          let buf = "";
          res.on("data", (c) => {
            buf += c;
          });
          res.on("end", () => {
            try {
              const text = buf.replace(/^\uFEFF/, "");
              resolve(JSON.parse(text));
            } catch {
              resolve(null);
            }
          });
        })
        .on("error", reject);
    });
    expect("assets.yishi.site/manifest.json 可读", r && (r.version || r.bundles), r);
  }

  console.log(`\n========== 结果: ${pass} 通过 / ${fail} 失败 ==========\n`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error("FATAL", e);
  process.exit(2);
});
