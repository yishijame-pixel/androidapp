// ============================================================
// E2E 测试：验证"内测码严格一次性 + 用户名重复注册被拒"
// 用法：
//   cd backend
//   node tools/test_strict_register.js
//
// 默认会临时注册一个 e2e_test_<时间戳> 账号，跑完测试后留在云端
// （register_log 没有删除接口；不影响线上数据）
// ============================================================

const crypto = require("crypto");
const https = require("https");

const BASE_URL = "https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com";

// 已知"已 used"的内测码（用户截图中确认作废）
const USED_BETA_CODE = "FL-AUEX-ZABY-JBQN";

// 测试账号
const ts = Date.now();
const TEST_USERNAME = `e2e_test_${ts}`;
const TEST_PASSWORD = "TestPwd123!";
const TEST_NICKNAME = "E2E测试号";
const TEST_DEVICE = "e2e_device_" + ts.toString(16);

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
        timeout: 10000,
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
      }
    );
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.write(data);
    req.end();
  });
}

let pass = 0,
  fail = 0;
function expect(name, cond, detail) {
  if (cond) {
    console.log(`  ✅ ${name}`);
    pass++;
  } else {
    console.log(`  ❌ ${name}`);
    if (detail) console.log(`     → ${JSON.stringify(detail)}`);
    fail++;
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
async function cooldown(label = "") {
  console.log(`\n⏳ 限流冷却 65s ${label}...`);
  await sleep(65_000);
}

(async () => {
  console.log(`\n=== E2E 测试 (账号: ${TEST_USERNAME}) ===\n`);

  // ────────────────────────────────────────────────
  // 1. beta_validate：已 used 的码必须被拒
  // ────────────────────────────────────────────────
  console.log("【1】beta_validate · 已 used 的码必须拒绝");
  {
    const r = await post("/beta_validate", {
      code: USED_BETA_CODE,
      username: TEST_USERNAME,
      deviceId: TEST_DEVICE,
    });
    expect(
      `已 used 码返回 ok:false`,
      r.ok === false,
      r
    );
    expect(
      `错误码 = USED`,
      r.code === "USED",
      r
    );
  }

  // ────────────────────────────────────────────────
  // 2. beta_validate：格式无效的码
  // ────────────────────────────────────────────────
  console.log("\n【2】beta_validate · 格式无效");
  {
    const r = await post("/beta_validate", {
      code: "abc",
      username: TEST_USERNAME,
      deviceId: TEST_DEVICE,
    });
    expect(`格式短 → INVALID`, r.ok === false && r.code === "INVALID", r);
  }

  // ────────────────────────────────────────────────
  // 3. register_log mode=register · 首次注册：拿 token
  // ────────────────────────────────────────────────
  console.log("\n【3】register_log mode=register · 首次注册");
  const proof = passwordProof(TEST_USERNAME, TEST_PASSWORD);
  {
    const r = await post("/register_log", {
      username: TEST_USERNAME,
      nickname: TEST_NICKNAME,
      deviceId: TEST_DEVICE,
      betaCode: "TEST_DUMMY",
      passwordProof: proof,
      mode: "register",
    });
    expect(`首次注册 ok:true`, r.ok === true, r);
    expect(
      `返回 deviceToken`,
      typeof r.deviceToken === "string" && r.deviceToken.length > 0,
      r
    );
  }

  // ────────────────────────────────────────────────
  // 4. register_log mode=register · 同账号再注册必须被拒
  // ────────────────────────────────────────────────
  console.log("\n【4】register_log mode=register · 重复注册必须拒绝");
  {
    const r = await post("/register_log", {
      username: TEST_USERNAME,
      nickname: TEST_NICKNAME,
      deviceId: TEST_DEVICE,
      betaCode: "TEST_DUMMY",
      passwordProof: proof,
      mode: "register",
    });
    expect(`重复注册 ok:false`, r.ok === false, r);
    expect(
      `错误码 = ALREADY_REGISTERED`,
      r.code === "ALREADY_REGISTERED",
      r
    );
  }

  // ────────────────────────────────────────────────
  // 5. register_log mode=refresh · 已存在用户补领 token
  // ────────────────────────────────────────────────
  console.log("\n【5】register_log mode=refresh · 已注册用户补领 token");
  {
    const r = await post("/register_log", {
      username: TEST_USERNAME,
      nickname: TEST_NICKNAME,
      deviceId: TEST_DEVICE,
      betaCode: "",
      passwordProof: proof,
      mode: "refresh",
    });
    expect(`refresh 放行 ok:true`, r.ok === true, r);
    expect(
      `返回新 deviceToken`,
      typeof r.deviceToken === "string" && r.deviceToken.length > 0,
      r
    );
  }

  // ────────────────────────────────────────────────
  // 6. register_log · 错误密码必须被拒
  // ────────────────────────────────────────────────
  console.log("\n【6】register_log · 错误密码必须拒绝");
  {
    const wrongProof = passwordProof(TEST_USERNAME, "wrong_password");
    const r = await post("/register_log", {
      username: TEST_USERNAME,
      nickname: TEST_NICKNAME,
      deviceId: TEST_DEVICE,
      betaCode: "",
      passwordProof: wrongProof,
      mode: "refresh",
    });
    expect(`错密 ok:false`, r.ok === false, r);
    expect(`错误码 = WRONG_PASSWORD`, r.code === "WRONG_PASSWORD", r);
  }

  // ────────────────────────────────────────────────
  // 7. register_log · 异设备必须被拒
  // ────────────────────────────────────────────────
  console.log("\n【7】register_log · 异设备必须拒绝");
  {
    const r = await post("/register_log", {
      username: TEST_USERNAME,
      nickname: TEST_NICKNAME,
      deviceId: TEST_DEVICE + "_OTHER",
      betaCode: "",
      passwordProof: proof,
      mode: "refresh",
    });
    expect(`异设备 ok:false`, r.ok === false, r);
    expect(`错误码 = DEVICE_CONFLICT`, r.code === "DEVICE_CONFLICT", r);
  }

  // ────────────────────────────────────────────────
  // 8. dryRun 关键回归：内测码无效时云端 NOT 应创建用户
  //    场景：用一个全新用户名 + dryRun=true 预检 → 期望 ok 但不写库
  //    然后查 vip_users 确认没记录
  // ────────────────────────────────────────────────
  console.log("\n【8】dryRun 预检 · 新用户名只预检不写库");
  const ghostUser = `e2e_ghost_${ts}`;
  const ghostProof = passwordProof(ghostUser, "ghost_pwd");
  {
    const r = await post("/register_log", {
      username: ghostUser,
      nickname: "幽灵号",
      deviceId: TEST_DEVICE,
      betaCode: "",
      passwordProof: ghostProof,
      mode: "register",
      dryRun: true,
    });
    expect(`dryRun 新名 ok:true`, r.ok === true, r);
    expect(`dryRun 不签发 token`, !r.deviceToken, r);
    expect(`返回 preCheck 标记`, r.preCheck === true, r);
  }

  // 紧接着用同名 dryRun=false 也应该能正常注册（说明刚才的 dryRun 真的没写）

  console.log("\n【9】dryRun 后用同名实际注册 · 应当成功");
  {
    const r = await post("/register_log", {
      username: ghostUser,
      nickname: "幽灵号",
      deviceId: TEST_DEVICE,
      betaCode: "DUMMY",
      passwordProof: ghostProof,
      mode: "register",
      dryRun: false,
    });
    expect(
      `dryRun 没污染 → 后续正式注册成功`,
      r.ok === true && typeof r.deviceToken === "string",
      r
    );
  }

  console.log(`\n=== 结果: ${pass} passed, ${fail} failed ===\n`);
  process.exit(fail === 0 ? 0 : 1);
})().catch((e) => {
  console.error("脚本异常:", e);
  process.exit(2);
});
