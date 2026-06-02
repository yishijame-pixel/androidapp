#!/usr/bin/env node
// ============================================================
// letter_ai 云函数 端到端测试脚本
// ------------------------------------------------------------
// 用法：
//   node test_letter_ai.js <CLOUDFUNCTION_HTTP_URL>
//   例如：
//   node test_letter_ai.js https://funlife-prod-xxxx.service.tcloudbase.com/letter_ai
//
// 前置：letter_ai 已部署，且 HTTP 触发器路径为 /letter_ai
//
// 测试用例：
//   T1: 缺凭证          → 期望 INVALID
//   T2: 伪造签名        → 期望 BAD_SIGNATURE
//   T3: 普通用户调用    → 期望 ok=true 且 used=1, quota=1（首次）
//   T4: 普通用户重复    → 期望 QUOTA_EXCEEDED（第 2 次）
//   T5: 同 letterId 重试 → 期望 ok=true 且 idempotent（不扣额度）
//   T6: 限流            → 60s 内连发 8 次，第 7+ 次应被限
//
// ⚠️ T3 会真实调 LLM 消耗 token（≈¥0.01）。如要跳过，给 --no-llm 参数。
// ============================================================

const crypto = require("crypto");
const https = require("https");
const { URL } = require("url");

const URL_ARG = process.argv[2];
const NO_LLM = process.argv.includes("--no-llm");
if (!URL_ARG) {
  console.error("用法: node test_letter_ai.js <https://.../letter_ai>");
  process.exit(2);
}

// ⚠️ 与 cloudbaserc.json 中 letter_ai 的 HMAC_SECRET 一致；
//    本脚本仅本地用，secret 不会泄露到客户端
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
    deviceId,
    skuCode: "TEST_SKU",
    vipLevel,
    expireDate: "2099-12-31",
    bonusCoins: 0,
    issuedAt: now,
    exp: now + 365 * 86400,
  };
}

function post(body) {
  return new Promise((resolve, reject) => {
    const u = new URL(URL_ARG);
    const data = JSON.stringify(body);
    const req = https.request({
      method: "POST",
      hostname: u.hostname,
      port: u.port || 443,
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

const FAKE_LETTER = {
  letterId: "test_" + Date.now(),
  recipientName: "5 年前的我",
  relation: "过去的自己",
  persona: "正在大学里迷茫，对未来充满想象但又害怕做选择",
  timeAnchor: Math.floor(Date.now() / 1000) - 5 * 365 * 86400,
  userLetter: "你最近怎么样？我现在已经在工作了，回头看那段日子，其实挺珍贵的。",
  mood: "怀旧"
};

let pass = 0, fail = 0;
function expect(name, ok, got) {
  if (ok) { pass++; console.log(`✅ ${name}`); }
  else    { fail++; console.error(`❌ ${name}\n   got:`, got); }
}

(async () => {
  console.log("\n━━━━━ letter_ai e2e 测试 ━━━━━");
  console.log("URL :", URL_ARG);
  console.log("LLM :", NO_LLM ? "禁用" : "启用（会消耗 token）");

  // T1 缺凭证
  {
    const r = await post({ body: FAKE_LETTER });
    expect("T1 缺凭证 → INVALID",
      r.body && r.body.code === "INVALID", r.body);
  }

  // T2 伪造签名
  const FAKE_DEVICE = "test_device_" + crypto.randomBytes(4).toString("hex");
  {
    const cert = makeCert(FAKE_DEVICE, 1);
    const r = await post({
      certificate: cert,
      signature: "0".repeat(64),
      body: FAKE_LETTER,
    });
    expect("T2 伪造签名 → BAD_SIGNATURE",
      r.body && r.body.code === "BAD_SIGNATURE", r.body);
  }

  if (NO_LLM) {
    console.log("\n--no-llm 已设：跳过 T3-T5（依赖真实 LLM 调用）");
  } else {
    // T3 普通用户首次（vipLevel=0，DB 无记录 → 服务端按 0 处理，配额=1）
    let firstResp;
    {
      const cert = makeCert(FAKE_DEVICE, 0);
      const r = await post({
        certificate: cert, signature: sign(cert),
        body: { ...FAKE_LETTER, letterId: "t3_" + Date.now() },
      });
      firstResp = r;
      expect("T3 普通用户首次 → ok=true 且 used=1/1",
        r.body && r.body.ok === true && r.body.used === 1 && r.body.quota === 1, r.body);
      if (r.body && r.body.reply) {
        console.log("   AI 回信预览:", String(r.body.reply).slice(0, 80) + "...");
      }
    }

    // T4 普通用户第 2 封 → 配额耗尽
    {
      const cert = makeCert(FAKE_DEVICE, 0);
      const r = await post({
        certificate: cert, signature: sign(cert),
        body: { ...FAKE_LETTER, letterId: "t4_" + Date.now() },
      });
      expect("T4 普通用户第 2 封 → QUOTA_EXCEEDED",
        r.body && r.body.code === "QUOTA_EXCEEDED", r.body);
    }

    // T5 同 letterId 重试 → 幂等不扣额度
    if (firstResp && firstResp.body && firstResp.body.ok) {
      // 用一个全新设备，先扣 1，再用同 letterId 重发 → 仍 ok 不冤枉
      const D5 = "test_device_t5_" + crypto.randomBytes(4).toString("hex");
      const cert = makeCert(D5, 0);
      const fixedLetterId = "t5_fixed_" + Date.now();
      const r1 = await post({
        certificate: cert, signature: sign(cert),
        body: { ...FAKE_LETTER, letterId: fixedLetterId },
      });
      const r2 = await post({
        certificate: cert, signature: sign(cert),
        body: { ...FAKE_LETTER, letterId: fixedLetterId },
      });
      expect("T5 同 letterId 重试 → 第二次仍 ok（幂等）",
        r2.body && r2.body.ok === true, { r1: r1.body, r2: r2.body });
    }
  }

  // T6 限流：用一个新设备连发 8 次（不带 LLM 也会被签名验证拦下，但仍占限流额度）
  {
    const D6 = "test_device_t6_" + crypto.randomBytes(4).toString("hex");
    let limited = false;
    for (let i = 0; i < 8; i++) {
      const r = await post({
        certificate: { deviceId: D6 }, signature: "x",
        body: FAKE_LETTER,
      });
      if (r.body && r.body.code === "RATE_LIMITED") { limited = true; break; }
    }
    expect("T6 限流（>6/min）→ RATE_LIMITED", limited, "8 次内未触发");
  }

  console.log(`\n━━━━━ 结果：${pass} 通过 / ${fail} 失败 ━━━━━`);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error("脚本异常:", e); process.exit(2); });
