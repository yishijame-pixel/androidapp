// ============================================================
// 后台管理系统 E2E 测试
// ------------------------------------------------------------
// 用法（admin 服务必须先启动在 :3300）：
//   cd backend/admin
//   node test_admin.js                              # 提示输入密码
//   node test_admin.js --password=mypassword        # 直接传密码
//   node test_admin.js --password=mypassword --keep # 跑完不清理生成的卡密
//
// 覆盖：
//   - 登录 / 登出 / /api/me（cookie 维持）
//   - 卡密 CRUD：生成 → list → 编辑 → 禁用/启用 → 重置（用 used 假数据）→ 删除
//   - 批次：list / toggle
//   - 监控：health / audit / trend
//   - 用户：list（只读）
//   - 安全监控：alerts
//   - 错误路径：未登录、404、参数错误必须返回中文友好 message，绝不能暴露 e.stack
// ============================================================

const http = require("http");
const { URL } = require("url");

const BASE = "http://localhost:3300";

const args = process.argv.slice(2).reduce((m, x) => {
  const [k, v] = x.replace(/^--/, "").split("=");
  m[k] = v ?? true;
  return m;
}, {});

let PASSWORD = args.password || process.env.ADMIN_PASSWORD || "";
const KEEP = args.keep === true;
const VERBOSE = args.verbose === true;

if (!PASSWORD) {
  // 命令行交互式询问
  console.log("ℹ️  请通过 --password=<明文密码> 或环境变量 ADMIN_PASSWORD 提供登录密码");
  console.log("   （admin 用户名默认 admin，password hash 在 backend/tools/.env）");
  process.exit(1);
}

// ─────────── HTTP helper（手动 cookie jar） ───────────
let cookieJar = "";
function request(method, path, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE + path);
    const data = body ? JSON.stringify(body) : null;
    const headers = { "Content-Type": "application/json" };
    if (data) headers["Content-Length"] = Buffer.byteLength(data);
    if (cookieJar) headers["Cookie"] = cookieJar;
    const req = http.request(
      { hostname: url.hostname, port: url.port, path: url.pathname + url.search, method, headers, timeout: 10000 },
      (res) => {
        // 抓 cookie
        const sc = res.headers["set-cookie"];
        if (sc && sc.length) {
          for (const c of sc) {
            const m = c.match(/^([^=]+)=([^;]+)/);
            if (m && m[1] === "admin_sess") cookieJar = `${m[1]}=${m[2]}`;
          }
        }
        let buf = "";
        res.on("data", (c) => (buf += c));
        res.on("end", () => {
          let json = null;
          try { json = JSON.parse(buf); } catch (e) {}
          resolve({ status: res.statusCode, json, raw: buf });
        });
      }
    );
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("timeout")));
    if (data) req.write(data);
    req.end();
  });
}
const get = (p) => request("GET", p);
const post = (p, b) => request("POST", p, b);

// ─────────── 测试断言 ───────────
let pass = 0, fail = 0;
const fails = [];
function check(name, cond, detail) {
  if (cond) {
    console.log(`  ✅ ${name}`);
    pass++;
  } else {
    console.log(`  ❌ ${name}`);
    if (detail !== undefined) console.log(`     → ${typeof detail === "string" ? detail : JSON.stringify(detail).slice(0, 300)}`);
    fail++;
    fails.push(name);
  }
}
function looksLikeStackOrEnglish(s) {
  if (!s || typeof s !== "string") return false;
  // 简单识别：英文堆栈通常含 "at " 或 ":" + 行号，或 6+ 连续英文单词
  if (/\bat \w+/.test(s) || /Error:/.test(s) || /\bundefined\b/.test(s)) return true;
  if (/[A-Za-z]{8,}/.test(s) && !/[\u4e00-\u9fa5]/.test(s)) return true;
  return false;
}

// ─────────── 主测试流程 ───────────
async function main() {
  console.log(`\n=== FunLife 后台 E2E 测试 (${BASE}) ===\n`);

  // ────────────────────────────────────────────
  // 1. 未登录访问受保护资源
  // ────────────────────────────────────────────
  console.log("【1】未登录访问应被拒");
  cookieJar = "";
  {
    const r = await get("/api/stats");
    check("无 cookie → 401", r.status === 401, r);
    check("error 是中文友好提示", !looksLikeStackOrEnglish(r.json && r.json.error), r.json);
  }

  // ────────────────────────────────────────────
  // 2. 登录
  // ────────────────────────────────────────────
  console.log("\n【2】登录");
  {
    const r = await post("/api/login", { username: "admin", password: PASSWORD });
    check("登录返回 ok:true", r.json && r.json.ok === true, r);
    check("已写入 cookie", cookieJar.startsWith("admin_sess="), cookieJar.slice(0, 30));
  }
  {
    const r = await post("/api/login", { username: "admin", password: "wrong_pwd" });
    check("错密 → 401", r.status === 401, r);
    check("错密 error 中文", r.json && r.json.error && r.json.error.includes("错误"), r.json);
  }

  // 重新登录正确密码继续
  await post("/api/login", { username: "admin", password: PASSWORD });

  // ────────────────────────────────────────────
  // 3. /api/me + /api/sku
  // ────────────────────────────────────────────
  console.log("\n【3】登录后基础 API");
  {
    const r = await get("/api/me");
    check("/api/me ok", r.json && r.json.ok === true, r);
    check("/api/me 含 admin.u", r.json && r.json.admin && r.json.admin.u === "admin", r);
  }
  {
    const r = await get("/api/sku");
    check("/api/sku 返回 SKU map", r.json && r.json.ok && typeof r.json.data === "object", r);
  }
  {
    const r = await get("/api/stats");
    check("/api/stats 含 total / unused / used", r.json && r.json.ok && typeof r.json.data.total === "number", r);
  }

  // ────────────────────────────────────────────
  // 4. 卡密 CRUD
  // ────────────────────────────────────────────
  console.log("\n【4】卡密 CRUD");
  let testBatch = "e2e_test_" + Date.now();
  let generatedCodes = [];
  {
    // 先拿一个有效 SKU
    const skuRes = await get("/api/sku");
    const firstSku = Object.keys(skuRes.json.data)[0];

    // 生成 3 张
    const r = await post("/api/codes/generate", { skuCode: firstSku, count: 3, batch: testBatch });
    check("生成 3 张 ok", r.json && r.json.ok && r.json.data.success === 3, r);
    generatedCodes = (r.json.data.items || []).map((d) => d.replace(/-/g, ""));
    check("返回 3 个 display 码", generatedCodes.length === 3);
  }

  if (generatedCodes.length === 0) {
    console.log("\n⚠️  生成失败，后续依赖测试跳过");
  } else {
    const code1 = generatedCodes[0];
    const code2 = generatedCodes[1];
    const code3 = generatedCodes[2];

    // 列表能查到
    {
      const r = await get(`/api/codes?batch=${testBatch}`);
      check("批次过滤能查到 3 张", r.json && r.json.data.total === 3, r.json && r.json.data);
    }

    // 详情
    {
      const r = await get(`/api/codes/${code1}`);
      check("查询单条 ok", r.json && r.json.ok && r.json.data.code === code1, r);
    }

    // 编辑备注
    {
      const r = await post(`/api/codes/${code1}/update`, { note: "E2E 自动测试 · 备注" });
      check("更新备注 ok", r.json && r.json.ok === true, r);
      const r2 = await get(`/api/codes/${code1}`);
      check("备注已写入", r2.json && r2.json.data.note === "E2E 自动测试 · 备注", r2.json);
    }

    // 改 SKU
    {
      const skuRes = await get("/api/sku");
      const skuKeys = Object.keys(skuRes.json.data);
      if (skuKeys.length >= 2) {
        const newSku = skuKeys[1];
        const r = await post(`/api/codes/${code1}/update`, { skuCode: newSku });
        check("改 SKU ok", r.json && r.json.ok === true, r);
        const r2 = await get(`/api/codes/${code1}`);
        check("SKU 已变更", r2.json && r2.json.data.skuCode === newSku, r2.json);
      } else {
        console.log("  ⏭  只有 1 种 SKU，跳过改 SKU 测试");
      }
    }

    // 禁用 / 启用
    {
      const r = await post(`/api/codes/${code2}/toggle`);
      check("禁用 ok", r.json && r.json.ok && r.json.data.disabled === true, r);
      const r2 = await post(`/api/codes/${code2}/toggle`);
      check("启用 ok", r2.json && r2.json.ok && r2.json.data.disabled === false, r2);
    }

    // 重置 used 卡密：先模拟一张 used 的，但我们生成的都是 unused
    // 直接对 unused 调 reset 应该返回 NOT_USED 错误
    {
      const r = await post(`/api/codes/${code3}/reset`, { reason: "E2E 测试 reset on unused" });
      check("对 unused 调 reset → 400 NOT_USED", r.status === 400 && r.json.code === "NOT_USED", r);
      check("error 中文", r.json && r.json.error && !looksLikeStackOrEnglish(r.json.error), r);
    }

    // 删除 unused
    {
      if (!KEEP) {
        const r = await post(`/api/codes/${code3}/delete`);
        check("删除 unused ok", r.json && r.json.ok === true, r);
        const r2 = await get(`/api/codes/${code3}`);
        check("删除后查询返回 404", r2.status === 404, r2);
      } else {
        console.log("  ⏭  --keep 模式，不删除");
      }
    }

    // 不存在的 code
    {
      const r = await get(`/api/codes/NONEXIST_FAKE_CODE`);
      check("不存在 → 404", r.status === 404, r);
      check("404 error 中文", r.json && r.json.error && !looksLikeStackOrEnglish(r.json.error), r);
    }

    // 删除不存在的 code
    {
      const r = await post(`/api/codes/NONEXIST_FAKE_CODE/delete`);
      check("删除不存在 → 404", r.status === 404, r);
    }
  }

  // ────────────────────────────────────────────
  // 5. 批次管理
  // ────────────────────────────────────────────
  console.log("\n【5】批次管理");
  {
    const r = await get("/api/batches");
    check("批次列表 ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
    const found = r.json && r.json.data.find((x) => x.batch === testBatch);
    check("能找到 E2E 批次", !!found, found);
  }
  {
    const r = await post(`/api/batches/${testBatch}/toggle`, { disable: true });
    check("批次禁用 ok", r.json && r.json.ok === true, r);
    const r2 = await post(`/api/batches/${testBatch}/toggle`, { disable: false });
    check("批次启用 ok", r2.json && r2.json.ok === true, r2);
  }

  // ────────────────────────────────────────────
  // 6. 健康监控
  // ────────────────────────────────────────────
  console.log("\n【6】健康监控");
  {
    const r = await get("/api/health");
    check("/api/health ok", r.json && r.json.ok, r);
    check("含 collections", r.json && typeof r.json.data.collections === "object", r);
    check("含 rateLimitHits", r.json && r.json.data.rateLimitHits, r);
    check("vip_codes 集合存在", r.json && r.json.data.collections.vip_codes >= 0, r);
    check("vip_admin_audit 集合存在", r.json && r.json.data.collections.vip_admin_audit >= 0, r);
  }

  // ────────────────────────────────────────────
  // 7. 7 日趋势
  // ────────────────────────────────────────────
  console.log("\n【7】7 日趋势");
  {
    const r = await get("/api/trend/redeem?days=7");
    check("/api/trend/redeem ok", r.json && r.json.ok, r);
    check("返回 7 个桶", r.json && r.json.data.length === 7, r.json && r.json.data.length);
    const bucket = r.json.data[0];
    check("桶含 date / count / revenue", bucket && bucket.date && typeof bucket.count === "number", bucket);
  }
  {
    const r = await get("/api/trend/redeem?days=30");
    check("30 天返回 30 个桶", r.json && r.json.data.length === 30, r.json && r.json.data.length);
  }

  // ────────────────────────────────────────────
  // 8. 操作审计
  // ────────────────────────────────────────────
  console.log("\n【8】操作审计");
  {
    const r = await get("/api/audit?limit=50");
    check("/api/audit ok", r.json && r.json.ok, r);
    check("返回 items 数组", r.json && Array.isArray(r.json.data.items), r);
    // 应该包含我们刚才的写操作
    const items = r.json.data.items || [];
    const recent = items.slice(0, 30); // 最近 30 条
    check("含 codes_generate 审计", recent.some((x) => x.action === "codes_generate"), recent.map((x) => x.action));
    check("含 code_update 审计", recent.some((x) => x.action === "code_update"), recent.map((x) => x.action));
    if (!KEEP) {
      check("含 code_delete 审计", recent.some((x) => x.action === "code_delete"), recent.map((x) => x.action));
    }
    check("含 batch_disable 审计", recent.some((x) => x.action === "batch_disable"), recent.map((x) => x.action));
  }
  {
    const r = await get("/api/audit?action=codes_generate&limit=10");
    check("按 action 过滤 ok", r.json && r.json.data.items.every((x) => x.action === "codes_generate"), r.json);
  }

  // ────────────────────────────────────────────
  // 9. 用户 / 安全（只读）
  // ────────────────────────────────────────────
  console.log("\n【9】用户 / 安全（只读）");
  {
    const r = await get("/api/users");
    check("/api/users ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }
  {
    const r = await get("/api/security/alerts");
    check("/api/security/alerts ok", r.json && r.json.ok && r.json.data.stats, r);
  }
  {
    const r = await get("/api/points/stats");
    check("/api/points/stats ok", r.json && r.json.ok && typeof r.json.data.totalPointsHeld === "number", r);
  }
  {
    const r = await get("/api/coin/suspicious");
    check("/api/coin/suspicious ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }

  // ────────────────────────────────────────────
  // 10. 错误响应不泄漏堆栈
  // ────────────────────────────────────────────
  console.log("\n【10】错误响应安全检查");
  {
    // 无效封号请求（参数错）
    const r = await post("/api/users/ban", { scope: "INVALID_SCOPE", target: "x" });
    check("非法 scope → 400", r.status === 400, r);
    check("错误中文 + 不含堆栈", r.json && r.json.error && !looksLikeStackOrEnglish(r.json.error), r.json);
  }
  {
    // 强制迁移参数错
    const r = await post("/api/codes/SOMETHING/force_migrate", { newDeviceId: "tooshort" });
    check("短 deviceId → 400", r.status === 400, r);
    check("错误中文", r.json && !looksLikeStackOrEnglish(r.json.error), r.json);
  }

  // ────────────────────────────────────────────
  // 11. 🆕 v53 阅光书房 · 内容审核 + AI 监控
  // ────────────────────────────────────────────
  console.log("\n【11】v53 阅光书房 · 内容审核 / AI 监控（只读探活）");
  {
    const r = await get("/api/v53/galaxy/stats");
    check("/api/v53/galaxy/stats ok", r.json && r.json.ok &&
      typeof r.json.data.total === "number" &&
      typeof r.json.data.hidden === "number" &&
      typeof r.json.data.reported === "number" &&
      typeof r.json.data.totalLights === "number", r);
  }
  {
    const r = await get("/api/v53/galaxy/items?status=reported&limit=10");
    check("/api/v53/galaxy/items ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }
  {
    const r = await get("/api/v53/galaxy/items?status=hidden&limit=10");
    check("/api/v53/galaxy/items?status=hidden ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }
  {
    const r = await get("/api/v53/galaxy/items?status=all&limit=10");
    check("/api/v53/galaxy/items?status=all ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }
  {
    const r = await get("/api/v53/postcards/stats");
    check("/api/v53/postcards/stats ok", r.json && r.json.ok &&
      typeof r.json.data.total === "number" &&
      typeof r.json.data.reacted === "number" &&
      typeof r.json.data.hidden === "number", r);
  }
  {
    const r = await get("/api/v53/postcards/items?status=all&limit=10");
    check("/api/v53/postcards/items ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
  }
  {
    const r = await get("/api/v53/ai/stats");
    check("/api/v53/ai/stats ok", r.json && r.json.ok &&
      r.json.data.chat && r.json.data.book && r.json.data.reader_dna, r);
    check("v53 AI stats 三桶皆有 total 字段",
      r.json && r.json.ok &&
      typeof r.json.data.chat.total === "number" &&
      typeof r.json.data.book.total === "number" &&
      typeof r.json.data.reader_dna.total === "number", r);
    check("v53 AI stats 含成本估算",
      r.json && r.json.ok && r.json.data.cost_estimate &&
      typeof r.json.data.cost_estimate.chat === "number", r);
  }
  {
    const r = await get("/api/v53/ai/top_devices");
    check("/api/v53/ai/top_devices ok", r.json && r.json.ok && Array.isArray(r.json.data), r);
    // 如果有数据，校验记录字段完整
    if (r.json && r.json.data && r.json.data.length > 0) {
      const d = r.json.data[0];
      check("top_devices 记录含 chat/book/reader_dna/total",
        typeof d.chat === "number" &&
        typeof d.book === "number" &&
        typeof d.reader_dna === "number" &&
        typeof d.total === "number", d);
    }
  }
  {
    // 隐藏不存在的 id → 应当报错（且不暴露堆栈）
    const r = await post("/api/v53/galaxy/items/__nonexistent_id__/hide", { reason: "e2e test" });
    check("隐藏不存在 id 应返回错误", r.status >= 400 || (r.json && r.json.ok === false), r);
    check("v53 错误响应不含英文堆栈", r.json && !looksLikeStackOrEnglish(r.json.msg || r.json.error || ""), r.json);
  }
  {
    // v53 面板入口
    const r = await get("/v53");
    check("/v53 dashboard 页面可访问 (200)", r.status === 200, { status: r.status });
  }

  // ────────────────────────────────────────────
  // 12. 登出
  // ────────────────────────────────────────────
  console.log("\n【12】登出");
  {
    const r = await post("/api/logout");
    check("登出 ok", r.json && r.json.ok, r);
  }
  // 登出后再访问
  cookieJar = ""; // 客户端清 cookie
  {
    const r = await get("/api/stats");
    check("登出后 → 401", r.status === 401, r);
  }

  // ─────────── 最终汇总 ───────────
  console.log(`\n=================================`);
  console.log(`  ${pass} 通过 / ${fail} 失败`);
  if (fail > 0) {
    console.log("\n失败用例：");
    fails.forEach((n) => console.log("  - " + n));
  }
  console.log(`=================================\n`);
  process.exit(fail === 0 ? 0 : 1);
}

main().catch((e) => {
  console.error("脚本异常:", e);
  process.exit(2);
});
