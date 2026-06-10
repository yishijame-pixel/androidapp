// ============================================================
// E2E：账号恢复 /account_recover
// 用法:
//   cd backend
//   node tools/test_account_recover.js
//
// 流程:
//   1) register_log 注册临时账号
//   2) account_recover 同设备同密码 → 成功 + wallet + token
//   3) 错误密码 / 异设备 / 不存在用户 / 缺 proof → 拒绝
// ============================================================

const crypto = require("crypto");
const https = require("https");

const BASE_URL = process.env.FUNLIFE_BACKEND_URL ||
  "https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com";

const ts = Date.now();
const TEST_USERNAME = `e2e_recover_${ts}`;
const TEST_PASSWORD = "RecoverE2E123!";
const TEST_NICKNAME = "恢复测试";
const TEST_DEVICE = "e2e_recover_dev_" + ts.toString(16);
const OTHER_DEVICE = "e2e_recover_other_" + ts.toString(16);

function passwordProof(username, password) {
  return crypto
    .createHash("sha256")
    .update(`FunLifeAuth|${username.trim()}|${password}`, "utf8")
    .digest("hex");
}

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const url = new URL(BASE_URL + path);
    const req = https.request(
      {
        hostname: url.hostname,
        path: url.pathname,
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(data),
        },
        timeout: 15000,
      },
      (res) => {
        let buf = "";
        res.on("data", (c) => (buf += c));
        res.on("end", () => {
          try {
            resolve(JSON.parse(buf));
          } catch (e) {
            resolve({ _raw: buf, _status: res.statusCode });
          }
        });
      },
    );
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.write(data);
    req.end();
  });
}

let pass = 0;
let fail = 0;
function expect(name, cond, detail) {
  if (cond) {
    console.log(`  ✅ ${name}`);
    pass++;
  } else {
    console.log(`  ❌ ${name}`);
    if (detail !== undefined) console.log(`     → ${JSON.stringify(detail)}`);
    fail++;
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const proof = passwordProof(TEST_USERNAME, TEST_PASSWORD);

function isInvalidPath(resp) {
  return resp && resp.code === "INVALID_PATH";
}

async function preflightOrExit() {
  const probe = await post("/account_recover", {
    username: "__preflight__",
    passwordProof: "a".repeat(64),
    deviceId: "__preflight_dev__",
  });
  if (isInvalidPath(probe)) {
    console.error("\n❌ /account_recover 未部署或未配置 HTTP 触发器 (INVALID_PATH)");
    console.error("   1) cd backend && .\\deploy-account-recover.ps1");
    console.error("   2) CloudBase 控制台 → account_recover → HTTP 触发器 → 路径 /account_recover");
    console.error("   3) 重新运行: node tools/test_account_recover.js\n");
    process.exit(2);
  }
}

async function main() {
  console.log("\n=== E2E account_recover ===");
  console.log("BASE:", BASE_URL);
  console.log("USER:", TEST_USERNAME, "DEVICE:", TEST_DEVICE.slice(0, 24) + "…\n");

  await preflightOrExit();

  console.log("【1】register_log 注册临时账号");
  const reg = await post("/register_log", {
    username: TEST_USERNAME,
    nickname: TEST_NICKNAME,
    deviceId: TEST_DEVICE,
    betaCode: "",
    passwordProof: proof,
    mode: "register",
    dryRun: false,
  });
  expect("register ok", reg.ok === true, reg);
  await sleep(800);

  console.log("\n【2】recover 未部署/不存在用户前先测 ghost");
  const ghost = await post("/account_recover", {
    username: "ghost_user_" + ts,
    passwordProof: proof,
    deviceId: TEST_DEVICE,
  });
  expect(
    "ghost user rejected",
    ghost.ok === false && (ghost.code === "CREDENTIALS_INVALID" || ghost.code === "WRONG_PASSWORD"),
    ghost,
  );

  console.log("\n【3】recover 正确密码 + 同设备");
  const ok = await post("/account_recover", {
    username: TEST_USERNAME,
    passwordProof: proof,
    deviceId: TEST_DEVICE,
  });
  expect("recover ok", ok.ok === true, ok);
  expect("recover has token", typeof ok.deviceToken === "string" && ok.deviceToken.startsWith("v1."), ok);
  expect("recover nickname", ok.nickname === TEST_NICKNAME, ok);
  expect("recover wallet object", ok.wallet && typeof ok.wallet === "object", ok);
  expect("recovered flag", ok.recovered === true, ok);

  console.log("\n【4】recover 错误密码");
  const bad = await post("/account_recover", {
    username: TEST_USERNAME,
    passwordProof: passwordProof(TEST_USERNAME, "WrongPwd999"),
    deviceId: TEST_DEVICE,
  });
  expect(
    "wrong password rejected",
    bad.ok === false && (bad.code === "CREDENTIALS_INVALID" || bad.code === "WRONG_PASSWORD"),
    bad,
  );

  console.log("\n【5】recover 异设备");
  const devConflict = await post("/account_recover", {
    username: TEST_USERNAME,
    passwordProof: proof,
    deviceId: OTHER_DEVICE,
  });
  expect("device conflict", devConflict.ok === false && devConflict.code === "DEVICE_CONFLICT", devConflict);

  console.log("\n【6】recover 缺 proof");
  const noProof = await post("/account_recover", {
    username: TEST_USERNAME,
    passwordProof: "",
    deviceId: TEST_DEVICE,
  });
  expect("missing proof", noProof.ok === false && noProof.code === "PROOF_REQUIRED", noProof);

  console.log("\n【7】recover 重复恢复仍应成功（refresh 场景）");
  const again = await post("/account_recover", {
    username: TEST_USERNAME,
    passwordProof: proof,
    deviceId: TEST_DEVICE,
  });
  expect("second recover ok", again.ok === true, again);

  console.log(`\n结果: ${pass} passed, ${fail} failed\n`);
  if (fail > 0) process.exit(1);
}

main().catch((e) => {
  console.error("FATAL", e);
  process.exit(1);
});
