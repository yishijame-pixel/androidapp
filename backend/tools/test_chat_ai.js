#!/usr/bin/env node
// ============================================================
// chat_ai 云函数 端到端测试脚本
// ------------------------------------------------------------
// 用法：
//   node test_chat_ai.js <URL>
//   node test_chat_ai.js https://<envid>.ap-shanghai.app.tcloudbase.com/chat_ai
//
// 测试用例：
//   T1: 缺凭证                → INVALID
//   T2: 伪造签名              → BAD_SIGNATURE
//   T3: 普通用户首次聊天      → ok=true, used=1, limit=20
//   T4: 普通用户连发 5 次     → 都 ok 且 used 递增
//   T5: 输入为空              → BAD_REQUEST
//   T6: 限流（>30/min 单设备）→ RATE_LIMITED
//
// ⚠️ T3/T4 会真实调 LLM 消耗 token；--no-llm 跳过
// ============================================================

const crypto = require("crypto");
const https = require("https");
const { URL } = require("url");

const URL_ARG = process.argv[2];
const NO_LLM = process.argv.includes("--no-llm");
if (!URL_ARG) {
  console.error("用法: node test_chat_ai.js <https://.../chat_ai>");
  process.exit(2);
}

const HMAC_SECRET = process.env.HMAC_SECRET ||
  "0f2eed661ccce58ce69d6704094f283c7e2d4ad14285a1bd1f18986ad64aaada93564581a4dcaea2ef12a9a9502d70b0";

function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach(k => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function sign(obj) {
  return crypto.createHmac("sha256", HMAC_SECRET).update(canonicalJson(obj)).digest("hex");
}
function makeCert(deviceId, vipLevel = 0) {
  const now = Math.floor(Date.now() / 1000);
  return {
    deviceId, skuCode: "TEST_SKU", vipLevel,
    expireDate: "2099-12-31", bonusCoins: 0,
    issuedAt: now, exp: now + 365 * 86400,
  };
}
function post(body) {
  return new Promise((resolve, reject) => {
    const u = new URL(URL_ARG);
    const data = JSON.stringify(body);
    const req = https.request({
      method: "POST", hostname: u.hostname, port: u.port || 443,
      path: u.pathname + (u.search || ""),
      headers: { "Content-Type": "application/json", "Content-Length": Buffer.byteLength(data) },
      timeout: 30000,
    }, res => {
      let buf = "";
      res.on("data", c => buf += c);
      res.on("end", () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(buf) }); }
        catch (e) { resolve({ status: res.statusCode, body: buf }); }
      });
    });
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("TIMEOUT")));
    req.write(data); req.end();
  });
}

const BODY_TPL = {
  mode: "chat",
  personaSystem: "你是一个温暖、口语化、用一两句话回复的朋友。",
  userText: "今天上班好累，感觉效率不高",
};

let pass = 0, fail = 0;
function expect(name, ok, got) {
  if (ok) { pass++; console.log(`✅ ${name}`); }
  else    { fail++; console.error(`❌ ${name}\n   got:`, got); }
}

(async () => {
  console.log("\n━━━━━ chat_ai e2e 测试 ━━━━━");
  console.log("URL :", URL_ARG);
  console.log("LLM :", NO_LLM ? "禁用" : "启用（会消耗 token）");

  // T1
  {
    const r = await post({ body: BODY_TPL });
    expect("T1 缺凭证 → INVALID", r.body && r.body.code === "INVALID", r.body);
  }

  // T2
  const DEV = "test_chat_" + crypto.randomBytes(4).toString("hex");
  {
    const cert = makeCert(DEV, 1);
    const r = await post({ certificate: cert, signature: "0".repeat(64), body: BODY_TPL });
    expect("T2 伪造签名 → BAD_SIGNATURE", r.body && r.body.code === "BAD_SIGNATURE", r.body);
  }

  // T5 输入为空
  {
    const cert = makeCert(DEV, 0);
    const r = await post({
      certificate: cert, signature: sign(cert),
      body: { ...BODY_TPL, userText: "" },
    });
    expect("T5 输入为空 → BAD_REQUEST", r.body && r.body.code === "BAD_REQUEST", r.body);
  }

  if (!NO_LLM) {
    // T3 首次成功
    {
      const cert = makeCert(DEV, 0);
      const r = await post({
        certificate: cert, signature: sign(cert),
        body: { ...BODY_TPL, userText: "我刚记了一笔账：餐饮 35 元，本月已消费 600 元。请用一句话回复，限 20 字内。" },
      });
      expect("T3 普通用户首次 → ok=true, used=1, limit=20",
        r.body && r.body.ok === true && r.body.used === 1 && r.body.limit === 20, r.body);
      if (r.body && r.body.reply) console.log("   AI:", String(r.body.reply).slice(0, 60));
    }

    // T4 连发 3 次（避免太烧 token），used 应递增到 4
    let last = null;
    for (let i = 0; i < 3; i++) {
      const cert = makeCert(DEV, 0);
      const r = await post({
        certificate: cert, signature: sign(cert),
        body: { ...BODY_TPL, userText: `连续测试第 ${i + 2} 条，回复 5 字内。` },
      });
      last = r.body;
      if (!r.body || !r.body.ok) break;
    }
    expect("T4 连发 3 次 → used 累积到 4",
      last && last.ok === true && last.used === 4, last);
  } else {
    console.log("\n--no-llm：跳过 T3/T4");
  }

  // T6 限流（限 30/min，跑 35 次确保命中；每次都是签名错也算限流计数）
  {
    const DEV6 = "test_chat_rl_" + crypto.randomBytes(4).toString("hex");
    let limited = false;
    for (let i = 0; i < 35; i++) {
      const r = await post({
        certificate: { deviceId: DEV6 }, signature: "x", body: BODY_TPL,
      });
      if (r.body && r.body.code === "RATE_LIMITED") { limited = true; break; }
    }
    expect("T6 限流（>30/min）→ RATE_LIMITED", limited, "35 次内未触发");
  }

  console.log(`\n━━━━━ 结果：${pass} 通过 / ${fail} 失败 ━━━━━`);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error("脚本异常:", e); process.exit(2); });
