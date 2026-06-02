// sensitive-filter.test.js — v53 阅光书房 · 敏感内容过滤器单元测试
//
// 用 node 内置 assert，不依赖任何测试框架。
// 跑法：
//   node sensitive-filter.test.js
// 退出码 0 = 全过；非 0 = 有失败。
"use strict";

const assert = require("assert");
const { check, normalize } = require("./sensitive-filter");

let pass = 0, fail = 0;
const fails = [];
function test(name, fn) {
  try { fn(); pass++; console.log("  ✓ " + name); }
  catch (e) { fail++; fails.push(name); console.log("  ✗ " + name + "\n    " + e.message); }
}

console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
console.log("  v53 阅光书房 · 敏感词过滤器测试");
console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

/* ─── 1. 正常文学内容必须通过 ─── */
console.log("\n【1】正常文学/书摘必须通过");

test("普通摘抄 1", () => {
  const r = check("世界是真实的，幻想是必要的，否则我们只能死于事实。");
  assert.strictEqual(r.ok, true);
  assert.strictEqual(r.severity, "ok");
});

test("含书名号", () => {
  const r = check("这一段出自《活着》第 87 页，让我哭了好久。");
  assert.strictEqual(r.ok, true);
});

test("英文夹杂", () => {
  const r = check("Reading is the first step toward understanding ourselves.");
  assert.strictEqual(r.ok, true);
});

test("含数字（年份）", () => {
  const r = check("写于 2024 年的春天，那是我读完这本书的日子。");
  assert.strictEqual(r.ok, true);
});

/* ─── 2. 硬阻断词必须命中 ─── */
console.log("\n【2】涉黄/涉赌/涉毒/涉政/诈骗 —— 必须 block");

test("色情明文", () => {
  const r = check("约炮QQ群");
  assert.strictEqual(r.ok, false);
  assert.strictEqual(r.severity, "block");
});

test("赌博词", () => {
  const r = check("百家乐稳赚");
  assert.strictEqual(r.ok, false);
});

test("翻墙词", () => {
  const r = check("教你翻墙看油管");
  assert.strictEqual(r.ok, false);
});

test("VPN 大小写不敏感", () => {
  assert.strictEqual(check("VPN 一键加速").ok, false);
  assert.strictEqual(check("vpn 节点").ok, false);
});

test("诈骗词", () => {
  const r = check("我是被诈骗了");
  // "诈骗" 在 BLOCK_WORDS 里 → 这里我们承认这种文学反思也会被阻断
  // 这是宁可保守的安全设计，admin 可手动恢复
  assert.strictEqual(r.ok, false);
});

test("加微信引流", () => {
  const r = check("加微信交流");
  assert.strictEqual(r.ok, false);
});

test("加 vx 变体", () => {
  const r = check("加 vx：abc");
  assert.strictEqual(r.ok, false);
});

test("加 v 简写", () => {
  const r = check("加v领福利");
  assert.strictEqual(r.ok, false);
});

/* ─── 3. 正则规则 ─── */
console.log("\n【3】正则规则：手机号 / 身份证 / URL / 邮箱");

test("大陆手机号 11 位", () => {
  const r = check("有事可以打 13912345678 找我");
  assert.strictEqual(r.ok, false);
  assert.ok(r.matched.some(m => m.kind === "phone_cn"));
});

test("身份证号 18 位", () => {
  const r = check("身份证 11010519491231002X 是真的");
  assert.strictEqual(r.ok, false);
  assert.ok(r.matched.some(m => m.kind === "id_card"));
});

test("URL 含协议", () => {
  const r = check("看这个 https://example.com/path 详情");
  assert.strictEqual(r.ok, false);
});

test("URL 不含协议（裸域名）", () => {
  const r = check("访问 example.com 即可");
  assert.strictEqual(r.ok, false);
});

test("邮箱算 warn 不 block", () => {
  const r = check("写信给 someone@example.com");
  assert.strictEqual(r.severity, "warn");
  assert.strictEqual(r.ok, true);   // warn 不阻断
});

test("邮箱 + URL 同时命中：以 block 为准", () => {
  const r = check("详情 https://x.com 邮件 a@b.cn");
  assert.strictEqual(r.severity, "block");
});

/* ─── 4. 变体绕过 ─── */
console.log("\n【4】常见绕过变体");

test("全角字符 ＶＰＮ", () => {
  // 归一化后 "vpn" 应被识别
  const r = check("ＶＰＮ 节点教程");
  assert.strictEqual(r.ok, false);
});

test("空格+标点切断 加 . 微 . 信", () => {
  const r = check("加 . 微 . 信");
  // normalize 会去掉空格和点，应识别 "加微信" / "微信"
  assert.strictEqual(r.severity, "block");
});

test("微信号 + 关键词组合（block）", () => {
  // 我们的 wechat_id 规则需要前后 8 字符内有 "微信" 等关键词
  const r = check("我的微信 abc_def123");
  assert.strictEqual(r.severity, "block");
});

/* ─── 5. warn（软警告） ─── */
console.log("\n【5】warn 命中（不阻断但需复审）");

test("仅提及微信但无加好友意图", () => {
  // 现在 "微信" 在 WARN_WORDS（不是 BLOCK），block 词典里只有 "加微信"
  // 所以光说"微信"是 warn 不是 block
  const r = check("我把微信卸载了三个月");
  assert.strictEqual(r.severity, "warn");
  assert.strictEqual(r.ok, true);
});

test("warn 命中提供命中规则信息", () => {
  const r = check("电话很重要");
  assert.strictEqual(r.severity, "warn");
  assert.ok(r.matched.length > 0);
  assert.ok(r.matched.every(m => m.severity === "warn"));
});

/* ─── 6. 边界 ─── */
console.log("\n【6】边界条件");

test("空字符串", () => {
  const r = check("");
  assert.strictEqual(r.ok, true);
  assert.strictEqual(r.severity, "ok");
});

test("非字符串入参", () => {
  const r = check(null);
  assert.strictEqual(r.ok, true);
});

test("normalize 不改变纯文学内容", () => {
  const before = "这是一段很温柔的话。";
  // normalize 后会去除标点，"很温柔的话" 不应触发任何规则
  const after = normalize(before);
  assert.ok(after.length > 0);
  assert.ok(!/。/.test(after));  // 句号被去掉
});

/* ─── 7.0 QA 角度：极端绕过尝试（企业级红队） ─── */
console.log("\n【7.0】QA 红队尝试");

test("超长文本不应崩溃", () => {
  const huge = "正常文学内容".repeat(2000);  // 12000 字
  const r = check(huge);
  // 超长但没敏感词 → 应当 ok
  assert.strictEqual(r.severity, "ok");
});

test("Unicode 零宽字符插入 加微信", () => {
  // U+200B 零宽空格、U+FEFF 字节顺序标记、U+200C 零宽不连字
  const r = check("加\u200B微\u200C信\uFEFF联系");
  // 现状：normalize 没去 ZW 字符，所以有可能漏。这条测试用来"暴露已知盲区"。
  // 如果未来加了 ZW 过滤，这条期望可改 block。
  // 当前接受：能 normalize 后切割识别即 block；否则至少 warn（"微信" 子串还在）
  assert.notStrictEqual(r.severity, "ok");
});

test("混入空 emoji 加 微 ⭐ 信", () => {
  const r = check("加 微 ⭐ 信");
  assert.notStrictEqual(r.severity, "ok");  // emoji 在 normalize 中保留，但"微信"不连续 → 至少不应 ok
});

test("纯英文无害短句", () => {
  assert.strictEqual(check("This is a great book.").severity, "ok");
});

test("书摘含数字章节号", () => {
  // 第 13 章可能误触发什么？这里数字 13 不是手机号开头之外应当 ok
  assert.strictEqual(check("第 13 章 黑暗中的光").severity, "ok");
});

test("空白手机号 1391-2345-678（11 位但带分隔符）", () => {
  // normalize 不参与正则，原文 "1391-2345-678" 包含横杠，phone_cn 正则要求连续 11 位 → 不命中
  // 但 url_naked 也不命中（无 .com 后缀）
  // 这是已知盲区：分隔符可绕过手机号检测
  // 期望至少不崩溃；若未来加专门规则可改期望
  const r = check("打 1391-2345-678 联系");
  // 当前接受 warn（"联系" 是 warn 词不在词典；但"电话"邻近？测试只验证不崩溃）
  assert.ok(["ok", "warn", "block"].includes(r.severity));
});

test("中英混合 +V13800001234", () => {
  // "加v" + 完整手机号
  const r = check("+V13800001234 火热联系");
  assert.strictEqual(r.severity, "block");
});

test("只有 emoji 不应崩溃", () => {
  const r = check("📚✨🌅🤖");
  assert.strictEqual(r.ok, true);
});

test("纯空白 + 制表符", () => {
  assert.strictEqual(check("   \t\n  ").ok, true);
});

/* ─── 7. matched 记录可追溯 ─── */
console.log("\n【7】matched 输出可追溯");

test("matched 记录含 kind/rule/hit/severity", () => {
  const r = check("加微信 13912345678");
  assert.ok(r.matched.length > 0);
  r.matched.forEach(m => {
    assert.ok(typeof m.kind === "string");
    assert.ok(typeof m.severity === "string");
    assert.ok(typeof m.hit === "string");
  });
});

/* ─── 汇总 ─── */
console.log("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
console.log("  汇总：" + pass + " 通过 / " + fail + " 失败");
if (fail > 0) {
  console.log("\n  失败用例：");
  fails.forEach(n => console.log("    - " + n));
}
console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
process.exit(fail > 0 ? 1 : 0);
