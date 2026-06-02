// sensitive-filter.js — v53 阅光书房 · 敏感内容初筛
//
// 设计原则：
//   - 单点真理：所有 v53 公域内容（quote_galaxy / postcard_drift）必须经过同一规则
//   - 多层防御：词典 + 正则 + 变体归一化 + 拆字
//   - 输出可追溯：返回触发的具体规则，便于审计与人工复核
//   - 误伤可控：常见文学/书摘允许通过；只拦"导流 / 涉黄涉政 / 联系方式 / 商业广告"
//
// 接口：
//   const { check } = require("./sensitive-filter");
//   const r = check("text...");
//   r.ok           true/false 是否通过
//   r.severity     "block" | "warn" | "ok"
//   r.matched      [{ kind, rule, hit }] 命中的规则列表（仅 block / warn 时非空）

"use strict";

/* ── 1. 文本归一化：去除变体（空格 / 全角 / 同音字符 / 零宽字符 / emoji） ── */
function normalize(text) {
  if (!text) return "";
  let s = String(text).toLowerCase();
  // 1.0 零宽字符（防 "加\u200B微\u200C信" 这类 spammer 套路）
  //     U+200B 零宽空格、U+200C 零宽不连字、U+200D 零宽连字、U+FEFF BOM、U+2060 字间词
  s = s.replace(/[\u200B-\u200D\u2060\uFEFF]/g, "");
  // 1.1 emoji / 符号 / 私用区（防 "加 微 ⭐ 信" 用 emoji 切断关键词）
  //     覆盖：表情/补充表情、杂项符号、装饰符号(含 ⭐ U+2B50)、地图符号、传输符、私用区
  s = s.replace(
    /[\u{1F000}-\u{1FFFF}\u{2300}-\u{27BF}\u{2900}-\u{2BFF}\u{E000}-\u{F8FF}]/gu,
    ""
  );
  // 1.2 去掉所有空白 / 标点 / 控制字符（保留中英文/数字）
  s = s.replace(/[\s\u3000.,，。;；:：!！?？\-_/\\|`'"“”‘’()（）【】\[\]{}<>~@#$%^&*+=]/g, "");
  // 1.3 全角数字/字母 → 半角
  s = s.replace(/[\uff10-\uff19]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xfee0));
  s = s.replace(/[\uff21-\uff3a\uff41-\uff5a]/g, c => String.fromCharCode(c.charCodeAt(0) - 0xfee0));
  // 1.4 常见同形/异体字归一
  const map = {
    "壹": "1", "贰": "2", "叁": "3", "肆": "4", "伍": "5", "陆": "6", "柒": "7", "捌": "8", "玖": "9", "零": "0",
    "①": "1", "②": "2", "③": "3", "④": "4", "⑤": "5", "⑥": "6", "⑦": "7", "⑧": "8", "⑨": "9", "⓪": "0",
    "Ⅰ": "1", "Ⅱ": "2", "Ⅲ": "3", "Ⅳ": "4", "Ⅴ": "5",
  };
  s = s.replace(/[\u4e00-\u9fff\u2160-\u2188\u2460-\u2473]/g, c => map[c] != null ? map[c] : c);
  return s;
}

/* ── 2. 词典：分两级（block 强阻断 / warn 标记不阻断） ── */
const BLOCK_WORDS = [
  // 涉黄
  "色情", "黄色", "做爱", "约炮", "援交", "卖淫", "嫖娼",
  // 涉赌
  "赌博", "彩票", "六合彩", "百家乐",
  // 涉政（关键政策红线，宁可保守）
  "翻墙", "vpn", "梯子", "tor",
  // 涉毒
  "冰毒", "大麻", "贩毒", "毒品",
  // 诈骗 / 引流
  "诈骗", "传销", "代购", "微商", "刷单", "杀猪盘",
  // 引流联系方式（中英文）
  "加微信", "加vx", "加v", "加qq", "微信号", "扣扣号", "私聊",
  // 商业广告强词
  "代理招商", "免费送", "限时抢", "扫码进群",
];

const WARN_WORDS = [
  // 单提到不一定违规，但需要标记便于审核
  "微信", "qq", "电话", "手机号", "联系方式",
  "广告", "推广", "招商",
];

/* ── 3. 正则规则集 ── */
const REGEX_RULES = [
  { kind: "phone_cn", rule: /1[3-9]\d{9}/, severity: "block" },                    // 大陆手机号
  { kind: "id_card",  rule: /\b\d{17}[\dxX]\b/, severity: "block" },               // 身份证号
  // QQ 号：5-12 位数字，前后 8 字符内有"qq/扣扣/q号"等关键词（前后任一方向）
  { kind: "qq_num_after",  rule: /\b[1-9]\d{4,11}\b(?=.{0,8}(qq|扣扣|q号))/i, severity: "block" },
  { kind: "qq_num_before", rule: /(?:qq|扣扣|q号)[^a-zA-Z0-9]{0,8}\b[1-9]\d{4,11}\b/i, severity: "block" },
  // 微信号：6-20 位字母开头 + 字母/数字/_/-，前后 8 字符有"微信/wx/vx/v信/wechat"
  { kind: "wechat_id_after",  rule: /\b[a-zA-Z][a-zA-Z0-9_-]{5,19}\b(?=.{0,8}(微信|wx|vx|v信|wechat))/i, severity: "block" },
  { kind: "wechat_id_before", rule: /(?:微信|wx|vx|v信|wechat)[^a-zA-Z0-9]{0,8}\b[a-zA-Z][a-zA-Z0-9_-]{5,19}\b/i, severity: "block" },
  { kind: "url",      rule: /https?:\/\/[^\s\u4e00-\u9fff]{3,}/i, severity: "block" },
  // 裸域名：前面不能是 @（避免和邮箱冲突）
  { kind: "url_naked",rule: /(?:^|[^@a-z0-9._-])((?:[a-z0-9-]+\.)+(?:com|cn|net|org|xyz|info|top|tech|club|shop|store|vip))\b/i, severity: "block" },
  { kind: "bank_card",rule: /\b(?:\d[ -]?){15,18}\b/, severity: "warn" },          // 银行卡号
  { kind: "email",    rule: /\b[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}\b/i, severity: "warn" },
];

/* ── 4. 主入口 ── */
function check(rawText) {
  if (!rawText || typeof rawText !== "string") {
    return { ok: true, severity: "ok", matched: [] };
  }
  const matched = [];

  // 4.1 正则用原文（保留大小写 / 数字格式）
  for (const r of REGEX_RULES) {
    const m = rawText.match(r.rule);
    if (m) matched.push({ kind: r.kind, rule: r.rule.source, hit: m[0], severity: r.severity });
  }

  // 4.2 词典用归一化后文本
  const norm = normalize(rawText);
  for (const w of BLOCK_WORDS) {
    if (norm.includes(w)) matched.push({ kind: "block_word", rule: w, hit: w, severity: "block" });
  }
  for (const w of WARN_WORDS) {
    if (norm.includes(w)) matched.push({ kind: "warn_word", rule: w, hit: w, severity: "warn" });
  }

  // 4.3 汇总
  const hasBlock = matched.some(x => x.severity === "block");
  const hasWarn = matched.some(x => x.severity === "warn");
  return {
    ok: !hasBlock,
    severity: hasBlock ? "block" : (hasWarn ? "warn" : "ok"),
    matched,
  };
}

/** 旧 API 兼容：仅返回布尔。 */
function containsBlockWord(text) {
  return check(text).severity === "block";
}

module.exports = { check, containsBlockWord, normalize };
