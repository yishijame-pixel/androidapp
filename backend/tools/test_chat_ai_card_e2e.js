#!/usr/bin/env node
// ============================================================
// 聊天 AI 额度卡密 · 端到端测试
// ------------------------------------------------------------
// 用法：
//   node test_chat_ai_card_e2e.js
//   node test_chat_ai_card_e2e.js --no-llm          # 跳过真实 LLM 调用
//   node test_chat_ai_card_e2e.js --code FL-XXXX... # 使用已有卡密（不生成新码）
//
// 环境变量（可选，默认读 local.properties / tools/.env）：
//   VIP_BACKEND_URL  或 BASE_URL
//   HMAC_SECRET
//   TCB_*            生成测试卡密时需要
// ============================================================

const crypto = require("crypto");
const https = require("https");
const fs = require("fs");
const path = require("path");
const { URL } = require("url");

const NO_LLM = process.argv.includes("--no-llm");
const CODE_ARG = (() => {
  const i = process.argv.indexOf("--code");
  return i >= 0 ? process.argv[i + 1] : null;
})();

const HMAC_SECRET = process.env.HMAC_SECRET ||
  "0f2eed661ccce58ce69d6704094f283c7e2d4ad14285a1bd1f18986ad64aaada93564581a4dcaea2ef12a9a9502d70b0";

function loadBaseUrl() {
  if (process.env.VIP_BACKEND_URL) return process.env.VIP_BACKEND_URL.replace(/\/$/, "");
  if (process.env.BASE_URL) return process.env.BASE_URL.replace(/\/$/, "");
  const lp = path.join(__dirname, "..", "..", "local.properties");
  if (fs.existsSync(lp)) {
    const m = fs.readFileSync(lp, "utf-8").match(/VIP_BACKEND_URL=(.+)/);
    if (m) return m[1].trim().replace(/\/$/, "");
  }
  return "https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com";
}

const BASE = loadBaseUrl();
const REDEEM_URL = BASE + "/redeem";
const CHAT_AI_URL = BASE + "/chat_ai";

function canonicalJson(obj) {
  const sorted = {};
  Object.keys(obj).sort().forEach((k) => { sorted[k] = obj[k]; });
  return JSON.stringify(sorted);
}
function sign(obj) {
  return crypto.createHmac("sha256", HMAC_SECRET).update(canonicalJson(obj)).digest("hex");
}
function normalizeCode(input) {
  return String(input || "").trim().toUpperCase().replace(/[\s\-_]/g, "");
}

function postJson(url, body) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const data = JSON.stringify(body);
    const req = https.request({
      method: "POST",
      hostname: u.hostname,
      port: u.port || 443,
      path: u.pathname + (u.search || ""),
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(data),
      },
      timeout: 45000,
    }, (res) => {
      let buf = "";
      res.on("data", (c) => { buf += c; });
      res.on("end", () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(buf) });
        } catch (e) {
          resolve({ status: res.statusCode, body: buf, raw: true });
        }
      });
    });
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("TIMEOUT")));
    req.write(data);
    req.end();
  });
}

let pass = 0;
let fail = 0;
function expect(name, ok, got) {
  if (ok) {
    pass++;
    console.log(`✅ ${name}`);
  } else {
    fail++;
    console.error(`❌ ${name}`);
    console.error("   got:", typeof got === "object" ? JSON.stringify(got, null, 2) : got);
  }
}

async function generateTestCode() {
  const { initTcb } = require("./_loadEnv");
  const SKU = require("../shared/sku");
  const app = initTcb();
  const db = app.database();
  const CODES = db.collection("vip_codes");
  const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
  let display, code;
  for (let attempt = 0; attempt < 20; attempt++) {
    const buf = crypto.randomBytes(12);
    let s = "";
    for (let i = 0; i < 12; i++) s += ALPHABET[buf[i] % ALPHABET.length];
    display = `FL-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}`;
    code = normalizeCode(display);
    const exists = await CODES.where({ code }).limit(1).get();
    if (!exists.data || exists.data.length === 0) break;
  }
  const batch = `e2e_chat_ai_${Date.now()}`;
  await CODES.add({
    code,
    skuCode: "CHAT_AI_BASIC",
    productType: "chat_ai",
    chatAiTier: 2,
    entitlementSchema: "v2",
    status: "unused",
    batch,
    createdAt: db.serverDate(),
  });
  console.log(`\n📝 已生成测试卡密: ${display} (CHAT_AI_BASIC)\n`);
  return { display, code, batch };
}

const BODY_CHAT = {
  mode: "chat",
  personaSystem: "你是温暖的朋友，一句话回复。",
  userText: "测试：午饭花了 35 元，请用 15 字内回复。",
};

(async () => {
  console.log("\n━━━━━ 聊天 AI 卡密 E2E ━━━━━");
  console.log("BASE   :", BASE);
  console.log("REDEEM :", REDEEM_URL);
  console.log("CHAT_AI:", CHAT_AI_URL);
  console.log("LLM    :", NO_LLM ? "跳过" : "启用");

  const deviceId = "e2e_chatai_" + crypto.randomBytes(8).toString("hex");
  const userId = 999001;

  // R1 无效卡密
  {
    const r = await postJson(REDEEM_URL, {
      code: "FL-INVALID-TEST-0000",
      deviceId,
      userId,
    });
    expect("R1 无效卡密 → INVALID", r.body && r.body.code === "INVALID", r.body);
  }

  // 准备卡密
  let testCode = CODE_ARG ? normalizeCode(CODE_ARG) : null;
  let testDisplay = CODE_ARG || null;
  if (!testCode) {
    try {
      const gen = await generateTestCode();
      testCode = gen.code;
      testDisplay = gen.display;
    } catch (e) {
      console.warn("⚠ 无法生成卡密（需 tools/.env TCB 凭证）:", e.message);
      console.warn("  可手动: node generate_codes.js CHAT_AI_BASIC 1 e2e_test");
      console.warn("  然后: node test_chat_ai_card_e2e.js --code FL-XXXX-XXXX-XXXX");
      process.exit(2);
    }
  }

  // R2 激活 CHAT_AI_BASIC
  let cert, signature;
  {
    const r = await postJson(REDEEM_URL, {
      code: testCode,
      deviceId,
      userId,
    });
    const ok = r.body && r.body.ok === true && r.body.certificate;
    expect("R2 激活 AI 卡 → ok + certificate", ok, r.body);
    if (ok) {
      cert = r.body.certificate;
      signature = r.body.signature;
      expect("R2a skuCode=CHAT_AI_BASIC", cert.skuCode === "CHAT_AI_BASIC", cert);
      expect("R2b vipLevel=2 (chatAiTier v2 BASIC)", cert.vipLevel === 2, cert);
      expect("R2c bonusCoins=0", cert.bonusCoins === 0, cert);
      expect("R2d expireDate 存在", !!cert.expireDate, cert);
      const sigOk = sign(cert) === signature;
      expect("R2e 签名可本地复验", sigOk, { local: sign(cert).slice(0, 16), remote: String(signature).slice(0, 16) });
      expect("R2f 凭证含 productType=chat_ai", cert.productType === "chat_ai", cert);
      console.log(`   卡密: ${testDisplay || testCode}`);
    }
  }

  if (!cert || !signature) {
    console.log("\n━━━━━ 结果：", pass, "通过 /", fail, "失败（兑换未成功，跳过后续）━━━━━");
    process.exit(1);
  }

  // R3 幂等：同设备再兑 → isReissue（重装可拿回凭证，不重复发卡权益）
  {
    const r = await postJson(REDEEM_URL, { code: testCode, deviceId, userId });
    expect("R3 同设备复兑 → ok + isReissue", r.body && r.body.ok && r.body.isReissue === true, r.body);
    expect("R3a 复兑不赠金币 bonusCoins=0", r.body && r.body.certificate && r.body.certificate.bonusCoins === 0, r.body);
  }

  // S1 已激活卡密 · 换设备 → 拒绝（一卡一设备）
  {
    const otherDev = "e2e_other_" + crypto.randomBytes(8).toString("hex");
    const r = await postJson(REDEEM_URL, { code: testCode, deviceId: otherDev, userId: 888002 });
    expect("S1 其他设备再兑 → USED", r.body && r.body.code === "USED", r.body);
  }

  // S2 已激活卡密 · 同设备换账号 → USER_MISMATCH（首激绑定了 userId）
  {
    const otherUid = 888003;
    const r = await postJson(REDEEM_URL, { code: testCode, deviceId, userId: otherUid });
    expect("S2 同设备换账号 → USER_MISMATCH", r.body && r.body.code === "USER_MISMATCH", r.body);
  }

  // S3 卡密格式容错：带横杠/小写 与库内规范化 code 一致
  if (testDisplay) {
    const r = await postJson(REDEEM_URL, {
      code: testDisplay.toLowerCase(),
      deviceId,
      userId,
    });
    expect("S3 小写+横杠复兑 → isReissue", r.body && r.body.ok && r.body.isReissue === true, r.body);
  }

  // S4 设备指纹过短
  {
    const r = await postJson(REDEEM_URL, { code: testCode, deviceId: "short", userId });
    expect("S4 设备指纹过短 → INVALID", r.body && r.body.code === "INVALID", r.body);
  }

  // C1 chat_ai 无凭证
  {
    const r = await postJson(CHAT_AI_URL, { body: BODY_CHAT });
    expect("C1 无凭证 → INVALID", r.body && r.body.code === "INVALID", r.body);
  }

  // C2 伪造签名
  {
    const r = await postJson(CHAT_AI_URL, {
      certificate: cert,
      signature: "0".repeat(64),
      body: BODY_CHAT,
    });
    expect("C2 伪造签名 → BAD_SIGNATURE", r.body && r.body.code === "BAD_SIGNATURE", r.body);
  }

  if (!NO_LLM) {
    // C3 激活后首次聊天 → limit=80
    {
      const r = await postJson(CHAT_AI_URL, {
        certificate: cert,
        signature,
        body: BODY_CHAT,
      });
      const ok = r.body && r.body.ok === true && r.body.limit === 30 && r.body.used >= 1;
      expect("C3 AI 卡首次聊天 → ok, limit=30 (v2 BASIC)", ok, r.body);
      if (r.body && r.body.reply) console.log("   AI:", String(r.body.reply).slice(0, 80));
    }

    // C4 第二条 used 递增
    {
      const r = await postJson(CHAT_AI_URL, {
        certificate: cert,
        signature,
        body: { ...BODY_CHAT, userText: "第二条测试，5字内回复。" },
      });
      expect("C4 第二条 → used>=2", r.body && r.body.ok && r.body.used >= 2, r.body);
    }
  } else {
    console.log("\n--no-llm：跳过 C3/C4（需云端 AI_API_KEY）");
    // 无 LLM 时仍可用 mock：检查服务端至少接受凭证（若 LLM 未配置会 SERVER_MISCONFIG/LLM_FAILED）
    const r = await postJson(CHAT_AI_URL, { certificate: cert, signature, body: BODY_CHAT });
    const accepted = r.body && (
      r.body.ok === true ||
      r.body.code === "LLM_FAILED" ||
      r.body.code === "SERVER_MISCONFIG" ||
      r.body.code === "QUOTA_EXCEEDED"
    );
    expect("C3' 凭证被服务端接受（非 BAD_SIGNATURE）", accepted, r.body);
  }

  // S5 禁用卡密
  try {
    const genD = await generateTestCode();
    const { initTcb } = require("./_loadEnv");
    const app = initTcb();
    await app.database().collection("vip_codes").where({ code: genD.code }).update({ disabled: true });
    const r = await postJson(REDEEM_URL, {
      code: genD.code,
      deviceId: "e2e_dis_" + crypto.randomBytes(6).toString("hex"),
      userId: 999010,
    });
    expect("S5 已禁用卡密 → DISABLED", r.body && r.body.code === "DISABLED", r.body);
  } catch (e) {
    console.warn("⚠ S5 跳过:", e.message);
  }

  // S6 并发双设备抢同一 unused 卡（仅一方首激成功）
  try {
    const genC = await generateTestCode();
    const devA = "e2e_race_a_" + crypto.randomBytes(6).toString("hex");
    const devB = "e2e_race_b_" + crypto.randomBytes(6).toString("hex");
    const [ra, rb] = await Promise.all([
      postJson(REDEEM_URL, { code: genC.code, deviceId: devA, userId: 999011 }),
      postJson(REDEEM_URL, { code: genC.code, deviceId: devB, userId: 999012 }),
    ]);
    const wins = [ra, rb].filter((x) => x.body && x.body.ok === true).length;
    const loses = [ra, rb].filter((x) => x.body && (x.body.code === "USED" || x.body.ok === false)).length;
    expect("S6 并发抢码 → 恰一方成功一方失败", wins === 1 && loses >= 1, { ra: ra.body, rb: rb.body });
  } catch (e) {
    console.warn("⚠ S6 跳过:", e.message);
  }

  // R5 同 tier 续期
  let code2 = null;
  try {
    const gen2 = await generateTestCode();
    code2 = gen2.code;
    const r = await postJson(REDEEM_URL, { code: code2, deviceId, userId });
    if (r.body && r.body.ok && r.body.certificate) {
      const exp1 = cert.expireDate;
      const exp2 = r.body.certificate.expireDate;
      expect("R5 同档续期 → expireDate 延长或不变", exp2 >= exp1, { exp1, exp2 });
    }
  } catch (e) {
    console.log("⚠ R5 续期测试跳过:", e.message);
  }

  console.log("\n━━━━━ 安全场景说明 ━━━━━");
  console.log("· 一卡只能用一次「首激」；首激后 status=used，绑 usedByDevice + usedByUser");
  console.log("· 同设备同账号可复兑拿凭证（重装），isReissue=true，不重复赠币");
  console.log("· 其他设备/其他账号复用同一串卡密 → USED / USER_MISMATCH");
  console.log("· 并发双激：DB where status=unused 原子更新，仅一方成功");
  console.log("· 凭证 HMAC 在云端签发；改 cert/签名 → chat_ai BAD_SIGNATURE");
  console.log("· 限流：redeem 10次/分钟；chat_ai 30次/分钟（见 test_chat_ai.js T6）");

  console.log(`\n━━━━━ 结果：${pass} 通过 / ${fail} 失败 ━━━━━`);
  if (testDisplay) console.log(`测试卡密（已消耗）: ${testDisplay}`);
  process.exit(fail === 0 ? 0 : 1);
})().catch((e) => {
  console.error("脚本异常:", e);
  process.exit(2);
});
