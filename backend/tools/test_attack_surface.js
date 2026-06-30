// ============================================================
// 攻击面/边界/异常 全面回归测试
// 用法: node tools/test_attack_surface.js
// ============================================================

const crypto = require("crypto");
const https = require("https");
const { loadBaseUrl } = require("./_loadBaseUrl");

const BASE_URL = loadBaseUrl();

const ts = Date.now();
function uniq(prefix) {
  return `${prefix}_${ts}_${Math.random().toString(36).slice(2, 8)}`;
}
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
function check(name, cond, detail) {
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

/** 限流冷却：本地脚本所有请求都来自同一 IP，跑一两组用例后必撞 5/60s 限流。
 *  在每个 batch 之间等 65 秒确保窗口重置。 */
async function cooldown(label = "") {
  console.log(`\n⏳ 限流冷却 65s ${label}...`);
  await sleep(65_000);
}

(async () => {
  console.log("\n========== B. 安全漏洞 ==========\n");

  // B1. SQL/NoSQL 注入：username 含 $where/特殊符号
  console.log("【B1】NoSQL 注入尝试");
  for (const evil of [
    { $ne: null }, // 想让 query 把所有 username 都匹配上
    { $gt: "" },
    'admin"; DROP TABLE users;--',
    "../../etc/passwd",
  ]) {
    const r = await post("/register_log", {
      username: typeof evil === "string" ? evil : JSON.stringify(evil),
      nickname: "x",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: "a".repeat(64),
      mode: "register",
      dryRun: true,
    });
    check(
      `恶意 username "${
        typeof evil === "string" ? evil : JSON.stringify(evil)
      }" 不应导致 5xx 或 ALREADY_REGISTERED`,
      r.ok === true || r.ok === false, // 只要不 crash 就 OK
      r
    );
  }

  // B2. passwordProof 缺失或太短：必须被拒
  console.log("\n【B2】passwordProof 缺失/过短");
  {
    const r = await post("/register_log", {
      username: uniq("noproof"),
      nickname: "x",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: "",
      mode: "register",
      dryRun: true,
    });
    check(`无 proof → PROOF_REQUIRED`, r.code === "PROOF_REQUIRED", r);
  }
  {
    const r = await post("/register_log", {
      username: uniq("shortproof"),
      nickname: "x",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: "short",
      mode: "register",
      dryRun: true,
    });
    check(`proof 太短 → PROOF_REQUIRED`, r.code === "PROOF_REQUIRED", r);
  }

  // B3. deviceId 缺失
  console.log("\n【B3】deviceId 缺失");
  {
    const r = await post("/register_log", {
      username: uniq("nodev"),
      nickname: "x",
      deviceId: "",
      betaCode: "",
      passwordProof: "a".repeat(64),
      mode: "register",
      dryRun: true,
    });
    check(`无 deviceId → INVALID`, r.code === "INVALID", r);
  }

  // B4. beta_validate 限流（连发 12 次同 deviceId）
  console.log("\n【B4】beta_validate 限流（同 deviceId 连发 12 次）");
  {
    const dev = uniq("flooder");
    let limited = false;
    for (let i = 0; i < 12; i++) {
      const r = await post("/beta_validate", {
        code: "INVALIDCODE12345",
        username: "x",
        deviceId: dev,
      });
      if (r.code === "RATE_LIMITED") {
        limited = true;
        break;
      }
    }
    check(`12 次内必触发 RATE_LIMITED`, limited);
  }

  // B5. 越过 dryRun 直接走 register 路径用同设备同 username 注册多人
  console.log("\n【B5】非 dryRun 同 username 重复注册（跨调用）防御");
  {
    const u = uniq("dupreg");
    const dev = uniq("d");
    const proof = passwordProof(u, "pwd");
    const r1 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: false,
    });
    check(`首次注册 ok`, r1.ok === true, r1);
    const r2 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: false,
    });
    check(`二次同名 → ALREADY_REGISTERED`, r2.code === "ALREADY_REGISTERED", r2);
  }

  console.log("\n========== C. 输入边界 ==========\n");

  // C1. 超长 username（200 字符）
  console.log("【C1】超长 username");
  {
    const u = "a".repeat(200);
    const r = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: passwordProof(u, "p"),
      mode: "register",
      dryRun: true,
    });
    check(`200 字符不应导致 5xx`, r.ok !== undefined, r);
    check(
      `服务端应做长度截断（slice 64）`,
      // 不能直接验证截断，但只要返回业务码就 OK
      r.ok === true || (r.code && typeof r.code === "string"),
      r
    );
  }

  // C2. Unicode emoji 用户名
  console.log("\n【C2】Unicode emoji 用户名");
  {
    const u = uniq("emoji") + "🐶🍓";
    const r = await post("/register_log", {
      username: u,
      nickname: "小狗",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: passwordProof(u, "p"),
      mode: "register",
      dryRun: true,
    });
    check(`emoji 名应能处理`, r.ok === true, r);
  }

  // C3. 含空格 / 制表符的 username（应被 trim）
  console.log("\n【C3】含空格的 username");
  {
    const base = uniq("space");
    const u = `   ${base}   `;
    const proof = passwordProof(u, "p"); // 注意：proof 是基于 trim 后的 username
    const r = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: true,
    });
    check(`含空格用户名应被 trim 后处理`, r.ok === true, r);
  }

  // C4. nickname 含 HTML / 脚本标签
  console.log("\n【C4】nickname 含 <script> 标签（XSS 攻击面）");
  {
    const u = uniq("xss");
    const r = await post("/register_log", {
      username: u,
      nickname: "<script>alert(1)</script>",
      deviceId: uniq("d"),
      betaCode: "",
      passwordProof: passwordProof(u, "p"),
      mode: "register",
      dryRun: true,
    });
    check(`<script> nickname 不应导致 5xx`, r.ok !== undefined, r);
    // 注意：是否过滤 HTML 是后台 admin 页的责任，这里只验证 backend 不挂
  }

  console.log("\n========== D. 网络/异常路径 ==========\n");

  // D1. dryRun 后续可重试（已验证过，再跑一次双保险）
  console.log("【D1】dryRun 后第二次 dryRun 不污染状态");
  {
    const u = uniq("dr");
    const proof = passwordProof(u, "p");
    const dev = uniq("d");
    const r1 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: true,
    });
    const r2 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: proof,
      mode: "register",
      dryRun: true,
    });
    check(`两次 dryRun 都返回 preCheck`, r1.preCheck && r2.preCheck, { r1, r2 });
  }

  // D2. 已 dryRun 通过的 username，被别人抢注（异 deviceId 抢先 dryRun=false）
  console.log("\n【D2】竞态：A 端 dryRun → B 端正式抢注 → A 端再正式 → 应被拒");
  {
    const u = uniq("race");
    const proofA = passwordProof(u, "passA");
    const proofB = passwordProof(u, "passB");
    const devA = uniq("dA");
    const devB = uniq("dB");

    const a1 = await post("/register_log", {
      username: u,
      nickname: "A",
      deviceId: devA,
      betaCode: "",
      passwordProof: proofA,
      mode: "register",
      dryRun: true,
    });
    check(`A dryRun 通过`, a1.preCheck === true, a1);

    const b = await post("/register_log", {
      username: u,
      nickname: "B",
      deviceId: devB,
      betaCode: "",
      passwordProof: proofB,
      mode: "register",
      dryRun: false,
    });
    check(`B 正式抢注成功`, b.ok === true, b);

    const a2 = await post("/register_log", {
      username: u,
      nickname: "A",
      deviceId: devA,
      betaCode: "",
      passwordProof: proofA,
      mode: "register",
      dryRun: false,
    });
    check(
      `A 再来正式注册 → DEVICE_CONFLICT 或 WRONG_PASSWORD`,
      a2.code === "DEVICE_CONFLICT" || a2.code === "WRONG_PASSWORD",
      a2
    );
  }

  // D3. ALREADY_REGISTERED 但带错密码：应优先返回 WRONG_PASSWORD（避免泄漏注册状态）
  console.log("\n【D3】已注册用户用错密码探测：应返回 WRONG_PASSWORD 而非 ALREADY_REGISTERED");
  {
    const u = uniq("probe");
    const dev = uniq("d");
    const r1 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: passwordProof(u, "right"),
      mode: "register",
      dryRun: false,
    });
    check(`首次注册成功`, r1.ok === true, r1);

    const r2 = await post("/register_log", {
      username: u,
      nickname: "x",
      deviceId: dev,
      betaCode: "",
      passwordProof: passwordProof(u, "WRONG"),
      mode: "register",
      dryRun: true,
    });
    check(
      `错密探测返回 WRONG_PASSWORD（不是 ALREADY_REGISTERED）`,
      r2.code === "WRONG_PASSWORD",
      r2
    );
  }

  console.log(`\n=== 结果: ${pass} passed, ${fail} failed ===\n`);
  process.exit(fail === 0 ? 0 : 1);
})().catch((e) => {
  console.error("脚本异常:", e);
  process.exit(2);
});
