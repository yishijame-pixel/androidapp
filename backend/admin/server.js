// ============================================================
// FunLife VIP 管理后台（本地运行，不部署到公网）
//   启动：cd backend/admin && npm install && npm start
//   访问：http://localhost:3300
// ============================================================

const express = require("express");
const crypto = require("crypto");
const path = require("path");
const fs = require("fs");
const cookieParser = require("cookie-parser");
const { initTcb, loadEnv } = require("./_loadEnv");
const SKU = require("../shared/sku");
const { sendError, audit } = require("./_helpers");

// 先加载 .env，再 require auth（auth 需要读取环境变量）
loadEnv();
const auth = require("./auth");

const app = express();
app.use(express.json({ limit: "1mb" }));
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());

// 登录页面（公开） + 登录 API（公开 + 限流）
app.get("/login.html", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "login.html"));
});
app.post("/api/login", auth.rateLimitLogin, (req, res) => {
  try {
    const { username = "", password = "" } = req.body || {};
    if (!auth.checkPassword(username, password)) {
      return res.status(401).json({ ok: false, code: "BAD_CREDENTIALS", error: "用户名或密码错误" });
    }
    auth.issueCookie(res, username);
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "登录异常，请重试", e); }
});
app.post("/api/logout", (req, res) => { auth.clearCookie(res); res.json({ ok: true }); });
app.get("/api/me", auth.requireAuth, (req, res) => { res.json({ ok: true, admin: req.admin }); });

// 所有其它路由（页面 + API）都需要登录
app.use(auth.requireAuth);
app.use(express.static(path.join(__dirname, "public")));

const tcb = initTcb();
const db = tcb.database();
const _ = db.command;
const CODES = db.collection("vip_codes");
const LOG = db.collection("vip_redeem_log");

// 字符集：去掉易混字符（与 generate_codes.js 保持一致）
const ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

function normalize(code) {
  return (code || "").trim().toUpperCase().replace(/[\s\-_]/g, "");
}
function genCode() {
  const buf = crypto.randomBytes(12);
  let s = "";
  for (let i = 0; i < 12; i++) s += ALPHABET[buf[i] % ALPHABET.length];
  return `FL-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}`;
}
function displayCode(rawCode) {
  // 数据库里存的是无分隔形态，展示时加分隔
  if (!rawCode) return "";
  const s = rawCode.replace(/^FL/, "");
  return `FL-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}`;
}

// ─────────────────────────────────────────────
// API 路由
// ─────────────────────────────────────────────

/** 仪表盘汇总 */
app.get("/api/stats", async (req, res) => {
  try {
    const out = {
      total: 0, unused: 0, used: 0, disabled: 0, revenue: 0,
      bySku: {},
      recentRedeem: 0,
    };
    for (const [sku, cfg] of Object.entries(SKU)) {
      const all = (await CODES.where({ skuCode: sku }).count()).total;
      const used = (await CODES.where({ skuCode: sku, status: "used" }).count()).total;
      const disabled = (await CODES.where({ skuCode: sku, disabled: true }).count()).total;
      const unused = all - used - disabled;
      out.bySku[sku] = {
        name: cfg.name, price: cfg.price,
        total: all, used, unused, disabled,
        revenue: used * cfg.price,
      };
      out.total += all; out.used += used; out.unused += unused; out.disabled += disabled;
      out.revenue += used * cfg.price;
    }
    // 最近 7 天的兑换次数
    const since = new Date(Date.now() - 7 * 86400 * 1000);
    try {
      const r = await LOG.where({ at: _.gte(since) }).count();
      out.recentRedeem = r.total;
    } catch (e) { out.recentRedeem = 0; }
    res.json({ ok: true, data: out });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "取仪表盘数据失败", e);
  }
});

const CHAT_AI_SKU_CODES = Object.keys(SKU).filter((k) => SKU[k].type === "chat_ai");
const VIP_SKU_CODES = Object.keys(SKU).filter((k) => SKU[k].type === "vip");

/** 聊天 AI 卡密运营统计（P3） */
app.get("/api/stats/chat_ai_products", async (req, res) => {
  try {
    const rows = [];
    for (const skuCode of CHAT_AI_SKU_CODES) {
      const def = SKU[skuCode];
      const total = (await CODES.where({ skuCode }).count()).total;
      const unused = (await CODES.where({ skuCode, status: "unused", disabled: _.neq(true) }).count()).total;
      const used = (await CODES.where({ skuCode, status: "used" }).count()).total;
      const disabled = (await CODES.where({ skuCode, disabled: true }).count()).total;
      rows.push({
        skuCode,
        name: def.name,
        price: def.price,
        chatAiTier: def.chatAiTier,
        durationDays: def.durationDays,
        total,
        unused,
        used,
        disabled,
        revenue: used * (def.price || 0),
      });
    }
    const sum = rows.reduce(
      (a, x) => ({
        total: a.total + x.total,
        unused: a.unused + x.unused,
        used: a.used + x.used,
        disabled: a.disabled + x.disabled,
        revenue: a.revenue + x.revenue,
      }),
      { total: 0, unused: 0, used: 0, disabled: 0, revenue: 0 }
    );
    res.json({ ok: true, data: { rows, sum } });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "AI 卡统计失败", e);
  }
});

/** 卡密列表（分页 + 筛选） */
app.get("/api/codes", async (req, res) => {
  try {
    const { sku, status, batch, q, productType, page = "1", limit = "20" } = req.query;
    const where = {};
    if (sku) where.skuCode = sku;
    else if (productType === "chat_ai") where.skuCode = _.in(CHAT_AI_SKU_CODES);
    else if (productType === "vip") where.skuCode = _.in(VIP_SKU_CODES);
    if (status === "unused") { where.status = "unused"; where.disabled = _.neq(true); }
    else if (status === "used") where.status = "used";
    else if (status === "disabled") where.disabled = true;
    if (batch) where.batch = batch;
    if (q) where.code = db.RegExp({ regexp: normalize(q), options: "i" });

    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 20));

    const total = (await CODES.where(where).count()).total;
    const r = await CODES.where(where)
      .orderBy("createdAt", "desc")
      .skip((pageNum - 1) * lim)
      .limit(lim)
      .get();

    const items = (r.data || []).map((x) => ({
      ...x,
      display: displayCode(x.code),
    }));
    res.json({ ok: true, data: { items, total, page: pageNum, limit: lim } });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "查询卡密列表失败", e);
  }
});

/** 批量生成卡密 */
app.post("/api/codes/generate", async (req, res) => {
  try {
    const { skuCode, count, batch } = req.body;
    if (!SKU[skuCode]) return res.status(400).json({ ok: false, error: "未知 SKU" });
    const n = parseInt(count, 10);
    if (isNaN(n) || n < 1 || n > 1000) {
      return res.status(400).json({ ok: false, error: "数量须 1-1000" });
    }
    const batchId = (batch || `batch_${Date.now()}`).slice(0, 64);

    const seen = new Set();
    const items = [];
    while (items.length < n) {
      const display = genCode();
      const code = normalize(display);
      if (seen.has(code)) continue;
      seen.add(code);
      items.push({ display, code });
    }

    let success = 0;
    for (const it of items) {
      try {
        const skuDef = SKU[skuCode];
        const doc = {
          code: it.code,
          skuCode,
          status: "unused",
          batch: batchId,
          createdAt: db.serverDate(),
          disabled: false,
          migrateCount: 0,
        };
        if (skuDef && skuDef.type === "chat_ai") {
          doc.productType = "chat_ai";
          doc.chatAiTier = skuDef.chatAiTier || 0;
          doc.vipLevel = skuDef.chatAiTier || 0;
        } else if (skuDef && skuDef.type === "vip") {
          doc.productType = "vip";
          doc.vipLevel = skuDef.vipLevel || 0;
        }
        await CODES.doc(it.code).set(doc);
        success++;
      } catch (e) {
        console.error("写入失败", it.display, e.message);
      }
    }
    // 审计
    await audit(db, req, "codes_generate", { skuCode, count: success, batch: batchId });
    res.json({
      ok: true,
      data: {
        success,
        batch: batchId,
        skuCode,
        items: items.map((x) => x.display),
      },
    });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "生成卡密失败", e);
  }
});

/** 单条详情 */
app.get("/api/codes/:code", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const r = await CODES.where({ code }).limit(1).get();
    const x = r.data && r.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");
    res.json({ ok: true, data: { ...x, display: displayCode(x.code) } });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "查询卡密详情失败", e);
  }
});

/** 禁用 / 启用 */
app.post("/api/codes/:code/toggle", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const r1 = await CODES.where({ code }).limit(1).get();
    const x = r1.data && r1.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");
    const next = !x.disabled;
    await CODES.where({ code }).update({
      disabled: next,
      disabledAt: next ? db.serverDate() : null,
    });
    await audit(db, req, next ? "code_disable" : "code_enable", { code });
    res.json({ ok: true, data: { disabled: next } });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "切换卡密状态失败", e);
  }
});

/** 强制把卡密迁到另一设备（客服手动操作） */
app.post("/api/codes/:code/force_migrate", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const { newDeviceId } = req.body;
    if (!newDeviceId || newDeviceId.length < 16) {
      return sendError(res, 400, "INVALID_DEVICE", "新设备指纹无效");
    }
    const r1 = await CODES.where({ code }).limit(1).get();
    const x = r1.data && r1.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");

    await CODES.where({ code }).update({
      usedByDevice: newDeviceId,
      forceMigrateAt: db.serverDate(),
    });
    // 写日志
    try {
      await LOG.add({ data: {
        code, deviceId: newDeviceId, oldDeviceId: x.usedByDevice,
        skuCode: x.skuCode, action: "admin_force_migrate", at: db.serverDate(),
      }});
    } catch (e) {}
    await audit(db, req, "code_force_migrate", { code, oldDeviceId: x.usedByDevice, newDeviceId });
    res.json({ ok: true });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "强制迁移失败", e);
  }
});

/** 兑换日志（兼容 CloudBase add({data:{...}}) 嵌套写入） */
app.get("/api/logs", async (req, res) => {
  try {
    const { limit = "50", action, code, device } = req.query;
    // 取最近 N 条全部回来（云函数写入是嵌套结构，无法在云端按嵌套字段过滤可靠）
    const r = await LOG.orderBy("at", "desc").limit(Math.min(500, parseInt(limit, 10) * 5 || 250)).get();
    let items = (r.data || []).map((x) => {
      // 兼容两种写入方式：1) 字段顶层平铺  2) 字段嵌套在 .data 下
      if (x && x.data && typeof x.data === "object" && (x.data.action || x.data.code)) {
        return { _id: x._id, ...x.data };
      }
      return x;
    });
    // 内存层过滤（数据量小，简单粗暴）
    if (action) items = items.filter((x) => x.action === action);
    if (code) {
      const c = normalize(code);
      items = items.filter((x) => x.code === c);
    }
    if (device) items = items.filter((x) => x.deviceId === device);
    items = items.slice(0, Math.min(200, parseInt(limit, 10) || 50));
    res.json({ ok: true, data: items });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "查询日志失败", e);
  }
});

/** 按设备查询 */
app.get("/api/device/:deviceId/codes", async (req, res) => {
  try {
    const r = await CODES.where({ usedByDevice: req.params.deviceId }).get();
    const items = (r.data || []).map((x) => ({ ...x, display: displayCode(x.code) }));
    res.json({ ok: true, data: items });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "查询设备名下卡密失败", e);
  }
});

/** SKU 配置（前端显示用，仅静态定义） */
app.get("/api/sku", (req, res) => {
  res.json({ ok: true, data: SKU });
});

// ─────────────────────────────────────────────
// 运行时 SKU 配置 vip_sku_config
// 用途：后台可改 bonusCoins / dailyCoins / durationDays / price / name
//      redeem 云函数和客户端 vip_config 都从此集合读，存空时回退 sku.js 默认
// ─────────────────────────────────────────────
const SKU_CFG = db.collection("vip_sku_config");
const SKU_CFG_FIELDS = ["name", "price", "vipLevel", "durationDays", "bonusCoins", "dailyCoins"];
// 默认 dailyCoins（此前写在客户端 VipLevel.kt，这里集中管理）
const DEFAULT_DAILY_COINS = { 1: 30, 2: 80, 3: 200 };

/** 读：返回所有 SKU 的"运行时合并值"（数据库 ⊕ sku.js 默认值） */
app.get("/api/sku/config", async (req, res) => {
  try {
    const r = await SKU_CFG.limit(50).get();
    const overrides = {};
    (r.data || []).forEach((d) => { overrides[d._id || d.skuCode] = d; });
    const merged = {};
    for (const [code, def] of Object.entries(SKU)) {
      if (def.type !== "vip") continue;
      const o = overrides[code] || {};
      merged[code] = {
        skuCode: code,
        name: o.name ?? def.name,
        price: o.price ?? def.price,
        vipLevel: def.vipLevel,                                 // vipLevel 由代码定义，不允许后台改
        durationDays: o.durationDays ?? def.durationDays,
        bonusCoins: o.bonusCoins ?? def.bonusCoins,
        dailyCoins: o.dailyCoins ?? DEFAULT_DAILY_COINS[def.vipLevel] ?? 0,
        // 元信息
        hasOverride: !!overrides[code],
        updatedAt: o.updatedAt || null,
        updatedBy: o.updatedBy || null,
      };
    }
    res.json({ ok: true, data: merged });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "读取 SKU 配置失败", e);
  }
});

/** 写：更新单个 SKU（_id = skuCode），不允许改 vipLevel */
app.post("/api/sku/config/:skuCode", async (req, res) => {
  try {
    const { skuCode } = req.params;
    if (!SKU[skuCode] || SKU[skuCode].type !== "vip") {
      return sendError(res, 400, "BAD_SKU", "未知或非 VIP SKU");
    }
    const body = req.body || {};
    const patch = {};
    for (const f of SKU_CFG_FIELDS) {
      if (f === "vipLevel") continue;            // 安全：禁止改 vipLevel
      if (body[f] !== undefined && body[f] !== null && body[f] !== "") {
        // 数值字段强制 Number + 范围校验，防止误输入异常值波及线上
        if (["price", "durationDays", "bonusCoins", "dailyCoins"].includes(f)) {
          const n = Number(body[f]);
          if (Number.isNaN(n)) return sendError(res, 400, "BAD_VALUE", `${f} 必须是数字`);
          if (f === "price" && (n < 0 || n > 99999)) {
            return sendError(res, 400, "BAD_VALUE", "price 应在 0~99999 元之间");
          }
          if (f === "durationDays" && n !== -1 && (n < 1 || n > 36500)) {
            return sendError(res, 400, "BAD_VALUE", "durationDays 应为 -1（永久）或 1~36500");
          }
          if (f === "bonusCoins" && (n < 0 || n > 1000000)) {
            return sendError(res, 400, "BAD_VALUE", "bonusCoins 应在 0~1000000 之间");
          }
          if (f === "dailyCoins" && (n < 0 || n > 100000)) {
            return sendError(res, 400, "BAD_VALUE", "dailyCoins 应在 0~100000 之间");
          }
          patch[f] = n;
        } else {
          // 字符串字段：长度限制，防超长写入
          const s = String(body[f]);
          if (s.length > 64) return sendError(res, 400, "BAD_VALUE", `${f} 长度不能超过 64`);
          patch[f] = s;
        }
      }
    }
    if (Object.keys(patch).length === 0) {
      return sendError(res, 400, "EMPTY", "没有可更新的字段");
    }
    patch.updatedAt = db.serverDate();
    patch.updatedBy = (req.headers["x-admin-user"] || "admin");

    // upsert by _id = skuCode
    let updated = 0;
    try {
      const u = await SKU_CFG.doc(skuCode).update(patch);
      updated = u.updated || 0;
    } catch (e) { /* doc 不存在 */ }
    if (!updated) {
      await SKU_CFG.doc(skuCode).set({ ...patch, skuCode, createdAt: db.serverDate() });
    }

    await audit(db, req, "sku_config_update", { skuCode, patch });
    res.json({ ok: true, data: { skuCode, patch } });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "保存 SKU 配置失败", e);
  }
});

/** 重置：删除某 SKU 的 override，回退到 sku.js 默认 */
app.delete("/api/sku/config/:skuCode", async (req, res) => {
  try {
    const { skuCode } = req.params;
    if (!SKU[skuCode]) return sendError(res, 400, "BAD_SKU", "未知 SKU");
    try { await SKU_CFG.doc(skuCode).remove(); } catch (e) {}
    await audit(db, req, "sku_config_reset", { skuCode });
    res.json({ ok: true });
  } catch (e) {
    sendError(res, 500, "INTERNAL", "重置 SKU 配置失败", e);
  }
});

/** 导出 CSV（批次） */
app.get("/api/export/batch/:batch", async (req, res) => {
  try {
    const r = await CODES.where({ batch: req.params.batch }).get();
    const items = r.data || [];
    const rows = ["兑换码,SKU,状态,设备,使用时间,到期时间,生成时间"];
    items.forEach((x) => {
      rows.push([
        displayCode(x.code),
        x.skuCode,
        x.disabled ? "已禁用" : x.status,
        (x.usedByDevice || "").slice(0, 16),
        x.usedAt || "",
        (x.expireDate === null || x.expireDate === undefined) ? "永久" : x.expireDate,
        x.createdAt || "",
      ].join(","));
    });
    res.setHeader("Content-Type", "text/csv; charset=utf-8");
    res.setHeader("Content-Disposition", `attachment; filename="batch_${req.params.batch}.csv"`);
    res.send("\uFEFF" + rows.join("\n"));  // UTF-8 BOM 让 Excel 识别中文
  } catch (e) {
    res.status(500).send(e.message);
  }
});

// ─────────────────────────────────────────────
// 批次管理
// ─────────────────────────────────────────────
const MARKS = db.collection("vip_device_marks"); // 设备标签集合（按需自动创建）

/** 容忍集合不存在的安全查询包装 */
async function safeQuery(promise) {
  try { return await promise; }
  catch (e) {
    if (String(e.message || "").includes("not exist")) return { data: [] };
    throw e;
  }
}

/** 全部批次汇总 */
app.get("/api/batches", async (req, res) => {
  try {
    // 一次取所有 codes 在内存做聚合（量不大）
    const r = await CODES.limit(2000).get();
    const map = {};
    (r.data || []).forEach((x) => {
      const b = x.batch || "(未命名)";
      if (!map[b]) map[b] = {
        batch: b, total: 0, used: 0, unused: 0, disabled: 0,
        revenue: 0, skuCounts: {}, firstAt: null, lastAt: null,
      };
      const cfg = SKU[x.skuCode];
      map[b].total += 1;
      if (x.disabled) map[b].disabled += 1;
      else if (x.status === "used") {
        map[b].used += 1;
        if (cfg) map[b].revenue += cfg.price;
      } else {
        map[b].unused += 1;
      }
      map[b].skuCounts[x.skuCode] = (map[b].skuCounts[x.skuCode] || 0) + 1;
      const t = x.createdAt;
      if (t && (!map[b].firstAt || t < map[b].firstAt)) map[b].firstAt = t;
      if (t && (!map[b].lastAt  || t > map[b].lastAt))  map[b].lastAt  = t;
    });
    const list = Object.values(map).sort((a, b) => String(b.lastAt || "").localeCompare(String(a.lastAt || "")));
    res.json({ ok: true, data: list });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 整批禁用 / 启用 */
app.post("/api/batches/:batch/toggle", async (req, res) => {
  try {
    const batch = req.params.batch;
    const { disable = true } = req.body;
    const r = await CODES.where({ batch }).update({
      disabled: !!disable,
      disabledAt: disable ? db.serverDate() : null,
    });
    await audit(db, req, disable ? "batch_disable" : "batch_enable", { batch, updated: r.updated || 0 });
    res.json({ ok: true, data: { updated: r.updated || 0 } });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 导出该批次（可指定状态） */
app.get("/api/batches/:batch/export_csv", async (req, res) => {
  try {
    const { status } = req.query; // 可为 unused / used / all
    const where = { batch: req.params.batch };
    if (status === "unused") { where.status = "unused"; where.disabled = _.neq(true); }
    else if (status === "used") where.status = "used";
    const r = await CODES.where(where).get();
    const rows = ["兑换码,SKU,状态,使用设备,使用时间,到期时间,生成时间"];
    (r.data || []).forEach((x) => rows.push([
      displayCode(x.code), x.skuCode,
      x.disabled ? "已禁用" : x.status,
      (x.usedByDevice || "").slice(0, 16),
      x.usedAt || "",
      (x.expireDate === null || x.expireDate === undefined) ? "永久" : x.expireDate,
      x.createdAt || "",
    ].join(",")));
    res.setHeader("Content-Type", "text/csv; charset=utf-8");
    res.setHeader("Content-Disposition", `attachment; filename="${req.params.batch}_${status||'all'}.csv"`);
    res.send("\uFEFF" + rows.join("\n"));
  } catch (e) { res.status(500).send(e.message); }
});

// ─────────────────────────────────────────────
// 设备详情：时间线 + 标记
// ─────────────────────────────────────────────

/** 设备时间线（综合卡密 + 日志） */
app.get("/api/devices/:deviceId/timeline", async (req, res) => {
  try {
    const deviceId = req.params.deviceId;
    // 卡密
    const c = await CODES.where({ usedByDevice: deviceId }).get();
    const codes = (c.data || []).map((x) => ({
      type: "code", at: x.usedAt, code: x.code, display: displayCode(x.code),
      skuCode: x.skuCode, status: x.disabled ? "disabled" : x.status,
    }));
    // 日志（兼容嵌套）
    const l = await LOG.orderBy("at", "desc").limit(500).get();
    const logs = (l.data || [])
      .map((x) => (x && x.data && (x.data.action || x.data.code) ? { _id: x._id, ...x.data } : x))
      .filter((x) => x.deviceId === deviceId)
      .map((x) => ({ type: "log", at: x.at, action: x.action, code: x.code, skuCode: x.skuCode, reason: x.reason || x.msg || "" }));
    // 设备标记
    const m = await safeQuery(MARKS.where({ deviceId }).limit(1).get());
    const mark = m.data && m.data[0] ? m.data[0] : null;

    const timeline = [...codes, ...logs].sort((a, b) => String(b.at || "").localeCompare(String(a.at || "")));
    res.json({ ok: true, data: { mark, timeline, codeCount: codes.length, logCount: logs.length } });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 标记设备：trusted / suspicious / blacklist */
app.post("/api/devices/:deviceId/mark", async (req, res) => {
  try {
    const deviceId = req.params.deviceId;
    const { mark, note = "" } = req.body;
    if (!["trusted", "suspicious", "blacklist", "clear"].includes(mark)) {
      return res.status(400).json({ ok: false, error: "未知标签" });
    }
    if (mark === "clear") {
      try { await MARKS.where({ deviceId }).remove(); } catch (e) {}
    } else {
      // upsert
      const exist = await safeQuery(MARKS.where({ deviceId }).limit(1).get());
      if (exist.data && exist.data[0]) {
        await MARKS.where({ deviceId }).update({ mark, note, updatedAt: db.serverDate() });
      } else {
        await MARKS.doc(deviceId.slice(0, 32)).set({ deviceId, mark, note, createdAt: db.serverDate() });
      }
    }
    await audit(db, req, "device_mark", { deviceId: deviceId.slice(0, 16) + "...", mark, note });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

// ─────────────────────────────────────────────
// 安全监控
// ─────────────────────────────────────────────
app.get("/api/security/alerts", async (req, res) => {
  try {
    // 时间窗口：最近 7 天
    const since7d = new Date(Date.now() - 7 * 86400 * 1000);
    const since24h = new Date(Date.now() - 24 * 3600 * 1000);

    // 全部最近卡密 + 日志，内存层聚合
    const codeRes = await CODES.where({ usedAt: _.gte(since7d) }).limit(2000).get();
    const codes = codeRes.data || [];
    const logRes = await LOG.orderBy("at", "desc").limit(1000).get();
    const logs = (logRes.data || [])
      .map((x) => (x && x.data && (x.data.action || x.data.code) ? { _id: x._id, ...x.data } : x));

    // 1) 同设备多卡（24h 内成功激活 ≥3 张同 SKU）
    const dupDevice = {};
    codes.forEach((x) => {
      if (!x.usedByDevice || !x.usedAt) return;
      if (new Date(x.usedAt) < since24h) return;
      const k = `${x.usedByDevice}__${x.skuCode}`;
      dupDevice[k] = dupDevice[k] || { deviceId: x.usedByDevice, skuCode: x.skuCode, count: 0, codes: [] };
      dupDevice[k].count += 1;
      dupDevice[k].codes.push(displayCode(x.code));
    });
    const suspectDevices = Object.values(dupDevice).filter((x) => x.count >= 3);

    // 2) 失败兑换日志（按 deviceId 聚合）—— 只有云函数已写失败日志才有
    const failedActions = ["redeem_failed", "verify_failed", "beta_validate_failed", "migrate_failed"];
    const failureMap = {};
    logs.forEach((x) => {
      if (!failedActions.includes(x.action)) return;
      if (!x.at || new Date(x.at) < since24h) return;
      const k = x.deviceId || x.ip || "unknown";
      failureMap[k] = failureMap[k] || { id: k, count: 0, reasons: {}, lastAt: x.at };
      failureMap[k].count += 1;
      failureMap[k].reasons[x.reason || x.action] = (failureMap[k].reasons[x.reason || x.action] || 0) + 1;
    });
    const suspectFailures = Object.values(failureMap)
      .filter((x) => x.count >= 5)
      .sort((a, b) => b.count - a.count);

    // 3) BAD_SIGNATURE 命中（明确破解尝试）
    const badSignatureHits = logs
      .filter((x) => x.reason === "BAD_SIGNATURE" || x.action === "verify_failed")
      .slice(0, 20);

    // 4) 已标记设备
    const marksRes = await safeQuery(MARKS.limit(200).get());
    const marks = marksRes.data || [];

    // 5) 整体计数
    const stats = {
      successRedeem24h: codes.filter((x) => x.usedAt && new Date(x.usedAt) >= since24h).length,
      failureCount24h: Object.values(failureMap).reduce((s, x) => s + x.count, 0),
      blacklistCount: marks.filter((m) => m.mark === "blacklist").length,
      suspiciousCount: marks.filter((m) => m.mark === "suspicious").length,
    };

    res.json({ ok: true, data: { stats, suspectDevices, suspectFailures, badSignatureHits, marks } });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

// ─────────────────────────────────────────────
// 用户管理
// ─────────────────────────────────────────────
const USERS = db.collection("vip_users");
const BANS  = db.collection("vip_user_bans");

/** 用户列表（聚合 vip_users + 兑换日志 + 卡密） */
app.get("/api/users", async (req, res) => {
  try {
    const { q, banned } = req.query;
    // 1) 注册上报的用户
    const u = await USERS.orderBy("registeredAt", "desc").limit(1000).get();
    const usersByName = {};
    (u.data || []).forEach((x) => {
      usersByName[x.username] = {
        username: x.username,
        nickname: x.nickname || "",
        deviceId: x.deviceId || "",
        betaCode: x.betaCode || "",
        registeredAt: x.registeredAt,
        lastSeenAt: x.lastSeenAt,
        source: "register_log",
      };
    });
    // 2) 从 beta_validate 日志补全（旧用户/未上报用户）
    const l = await LOG.orderBy("at", "desc").limit(2000).get();
    const logs = (l.data || []).map((x) => (x && x.data && (x.data.action || x.data.code) ? { _id: x._id, ...x.data } : x));
    logs.filter((x) => x.action === "beta_validate" && x.username).forEach((x) => {
      if (!usersByName[x.username]) {
        usersByName[x.username] = {
          username: x.username,
          nickname: "",
          deviceId: x.deviceId || "",
          betaCode: x.code || "",
          registeredAt: x.at,
          lastSeenAt: x.at,
          source: "beta_log",
        };
      }
    });

    // 3) 加上消费/卡密信息
    const users = Object.values(usersByName);
    const allDevices = users.map((u) => u.deviceId).filter(Boolean);
    const codeMap = {};
    if (allDevices.length) {
      const cr = await CODES.where({ usedByDevice: _.in(allDevices.slice(0, 100)) }).limit(2000).get();
      (cr.data || []).forEach((x) => {
        codeMap[x.usedByDevice] = codeMap[x.usedByDevice] || [];
        codeMap[x.usedByDevice].push(x);
      });
    }
    users.forEach((u) => {
      const arr = codeMap[u.deviceId] || [];
      u.vipCodes = arr.length;
      u.totalSpend = arr.reduce((s, c) => s + ((SKU[c.skuCode] && SKU[c.skuCode].price) || 0), 0);
      u.skuList = [...new Set(arr.map((c) => c.skuCode))];
    });

    // 4) 封号状态
    const bansR = await BANS.limit(500).get();
    const bansMap = {};
    (bansR.data || []).forEach((b) => { bansMap[b.key] = b; });
    users.forEach((u) => {
      const ub = bansMap["user:" + u.username];
      const db_ = bansMap["device:" + u.deviceId];
      const ban = ub || db_;
      if (ban) {
        u.banned = true;
        u.banReason = ban.reason;
        u.banScope = ub ? "user" : "device";
        u.bannedAt = ban.bannedAt;
      } else {
        u.banned = false;
      }
    });

    // 5) 金币快照 join
    try {
      const COIN_SNAP = db.collection("vip_coin_snapshots");
      const snapR = await safeQuery(COIN_SNAP.limit(1000).get());
      const snapMap = {};
      (snapR.data || []).forEach((s) => { snapMap[s.username] = s; });
      users.forEach((u) => {
        const s = snapMap[u.username];
        if (s) {
          u.balance = s.balance || 0;
          u.totalEarned = s.totalEarned || 0;
          u.totalSpent  = s.totalSpent || 0;
          u.coinFlags = s.flags || [];
          u.coinUpdatedAt = s.updatedAt;
        }
      });
    } catch (e) {}

    // 6) 过滤
    let result = users;
    if (q) {
      const k = q.toLowerCase();
      result = result.filter((u) =>
        (u.username || "").toLowerCase().includes(k) ||
        (u.nickname || "").toLowerCase().includes(k) ||
        (u.deviceId || "").toLowerCase().includes(k));
    }
    if (banned === "true") result = result.filter((u) => u.banned);
    if (banned === "false") result = result.filter((u) => !u.banned);

    result.sort((a, b) => String(b.registeredAt || "").localeCompare(String(a.registeredAt || "")));
    res.json({ ok: true, data: result, total: result.length });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 封号 */
app.post("/api/users/ban", async (req, res) => {
  try {
    const { scope, target, reason = "" } = req.body;
    if (!["user", "device"].includes(scope)) return res.status(400).json({ ok: false, error: "scope 必须 user/device" });
    if (!target) return res.status(400).json({ ok: false, error: "target 不能为空" });
    const key = scope + ":" + target;
    // upsert
    const exist = await BANS.where({ key }).limit(1).get();
    if (exist.data && exist.data[0]) {
      await BANS.where({ key }).update({ reason, bannedAt: db.serverDate() });
    } else {
      const docId = (scope + "_" + target).replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 64) + "_" + Date.now();
      await BANS.doc(docId).set({ key, scope, target, reason, bannedAt: db.serverDate() });
    }
    // 写日志
    try {
      await LOG.add({ data: { action: "admin_ban", scope, target, reason, at: db.serverDate() } });
    } catch (e) {}
    await audit(db, req, "user_ban", { scope, target, reason });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 某用户金币流水（最近 100 条） */
app.get("/api/coin/logs/:username", async (req, res) => {
  try {
    const username = req.params.username;
    const COIN_LOGS = db.collection("vip_coin_logs");
    const r = await safeQuery(COIN_LOGS.where({ username }).orderBy("at", "desc").limit(100).get());
    res.json({ ok: true, data: r.data || [] });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 金币异常用户列表（按 flags 过滤） */
app.get("/api/coin/suspicious", async (req, res) => {
  try {
    const COIN_SNAP = db.collection("vip_coin_snapshots");
    const r = await safeQuery(COIN_SNAP.limit(1000).get());
    const all = r.data || [];
    const suspicious = all
      .filter((s) => (s.flags && s.flags.length > 0) || (s.balance || 0) > 500000)
      .sort((a, b) => (b.balance || 0) - (a.balance || 0));
    res.json({ ok: true, data: suspicious, total: suspicious.length });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 后台对异常用户的客服指令：扣回金币 / 清零（不影响客户端本地余额，但记录到云端审计） */
app.post("/api/coin/adjust", async (req, res) => {
  try {
    const { username, action, amount = 0, reason = "" } = req.body;
    if (!username) return res.status(400).json({ ok: false, error: "username 必填" });
    if (!["reset", "deduct", "flag"].includes(action)) {
      return res.status(400).json({ ok: false, error: "action 必须为 reset/deduct/flag" });
    }
    const COIN_SNAP = db.collection("vip_coin_snapshots");
    const COIN_LOGS = db.collection("vip_coin_logs");
    const snapR = await safeQuery(COIN_SNAP.where({ username }).limit(1).get());
    const snap = (snapR.data || [])[0];
    if (!snap) return res.status(404).json({ ok: false, error: "用户金币快照不存在" });

    let newBalance = snap.balance || 0;
    const flags = new Set(snap.flags || []);
    if (action === "reset")  { newBalance = 0; flags.add("ADMIN_RESET"); }
    if (action === "deduct") {
      newBalance = Math.max(0, newBalance - Math.max(0, parseInt(amount) || 0));
      flags.add("ADMIN_DEDUCT");
    }
    if (action === "flag")   { flags.add("ADMIN_FLAGGED"); }

    await COIN_SNAP.where({ username }).update({
      balance: newBalance,
      flags: Array.from(flags),
      adminAdjustedAt: db.serverDate(),
      adminReason: reason,
    });
    await COIN_LOGS.add({ data: {
      username, op: "admin_" + action, amount, reason,
      balance: newBalance, at: db.serverDate(), source: "admin",
    }});
    await audit(db, req, "coin_" + action, { username, amount, reason, newBalance });
    res.json({ ok: true, newBalance });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

// ─────────────────────────────────────────────
// 🎯 积分监控（shopPoints）
// ─────────────────────────────────────────────

/** 24h 积分总览：发放/消耗/异常账号数/总余额 */
app.get("/api/points/stats", async (req, res) => {
  try {
    const COIN_LOGS = db.collection("vip_coin_logs");
    const COIN_SNAP = db.collection("vip_coin_snapshots");
    const since24h = new Date(Date.now() - 24 * 3600 * 1000);

    // 24h 流水
    const logR = await safeQuery(
      COIN_LOGS.where({ op: _.in(["point_earn", "point_spend"]), at: _.gte(since24h) }).limit(1000).get()
    );
    const logs = logR.data || [];
    const earn24h = logs.filter(x => x.op === "point_earn").reduce((s, x) => s + (x.amount || 0), 0);
    const spend24h = logs.filter(x => x.op === "point_spend").reduce((s, x) => s + (x.amount || 0), 0);

    // 全量快照
    const snapR = await safeQuery(COIN_SNAP.limit(2000).get());
    const snaps = snapR.data || [];
    const POINT_FLAGS = ["BIG_SINGLE_POINT_EARN", "BIG_POINT_BALANCE", "BIG_DAILY_POINT_EARN", "POINT_JUMP"];
    const abnormalAccounts = snaps.filter(s =>
      (s.flags || []).some(f => POINT_FLAGS.includes(f))
    ).length;
    const totalPointsHeld = snaps.reduce((s, x) => s + (x.pointsBalance || 0), 0);
    const usersWithPoints = snaps.filter(x => (x.pointsBalance || 0) > 0).length;

    res.json({
      ok: true,
      data: {
        earn24h, spend24h,
        abnormalAccounts,
        totalPointsHeld,
        usersWithPoints,
      },
    });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 积分异常账号列表（含 POINT_* flag 的快照） */
app.get("/api/points/suspicious", async (req, res) => {
  try {
    const COIN_SNAP = db.collection("vip_coin_snapshots");
    const r = await safeQuery(COIN_SNAP.limit(1000).get());
    const POINT_FLAGS = ["BIG_SINGLE_POINT_EARN", "BIG_POINT_BALANCE", "BIG_DAILY_POINT_EARN", "POINT_JUMP"];
    const suspicious = (r.data || [])
      .filter(s => (s.flags || []).some(f => POINT_FLAGS.includes(f)))
      .map(s => ({
        username: s.username,
        deviceId: s.deviceId,
        pointsBalance: s.pointsBalance || 0,
        flags: (s.flags || []).filter(f => POINT_FLAGS.includes(f)),
        updatedAt: s.updatedAt,
      }))
      .sort((a, b) => b.pointsBalance - a.pointsBalance);
    res.json({ ok: true, data: suspicious, total: suspicious.length });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 用户积分流水（最近 100 条 point_earn / point_spend） */
app.get("/api/points/logs/:username", async (req, res) => {
  try {
    const username = req.params.username;
    const COIN_LOGS = db.collection("vip_coin_logs");
    const r = await safeQuery(
      COIN_LOGS.where({ username, op: _.in(["point_earn", "point_spend"]) })
        .orderBy("at", "desc").limit(100).get()
    );
    res.json({ ok: true, data: r.data || [] });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 解封 */
app.post("/api/users/unban", async (req, res) => {
  try {
    const { scope, target } = req.body;
    if (!scope || !target) return res.status(400).json({ ok: false, error: "参数缺失" });
    const key = scope + ":" + target;
    await BANS.where({ key }).remove();
    try {
      await LOG.add({ data: { action: "admin_unban", scope, target, at: db.serverDate() } });
    } catch (e) {}
    await audit(db, req, "user_unban", { scope, target });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "操作失败，请稍后重试或查看日志", e); }
});

/** 删除用户（清理幽灵号/测试号） */
app.post("/api/users/:username/delete", async (req, res) => {
  try {
    const username = decodeURIComponent(req.params.username || "").trim();
    const { force = false } = req.body || {};
    if (!username) return sendError(res, 400, "INVALID", "用户名为空");
    if (username === "admin") return sendError(res, 400, "FORBIDDEN", "不能删除 admin 用户");

    // 1) 查用户
    const ur = await USERS.where({ username }).limit(1).get();
    const u = ur.data && ur.data[0];
    if (!u) return sendError(res, 404, "NOT_FOUND", "用户不存在");

    // 2) 查关联的 used 卡密（已激活）→ 风险
    const codeR = await CODES.where({ usedByUser: username, status: "used" }).limit(20).get();
    const usedCount = (codeR.data || []).length;
    if (usedCount > 0 && !force) {
      return sendError(res, 400, "USER_HAS_USED_CODES",
        `该用户名下有 ${usedCount} 张已激活卡密，需要 force=true 才能删除（卡密本身不会被删，仅清理用户档案）`);
    }

    // 3) 清理多个关联集合（每个失败都不阻塞，最后汇总）
    const cleaned = { user: 0, bans: 0, coinSnapshot: 0, revocations: 0 };
    try { const r = await USERS.where({ username }).remove();          cleaned.user = r.deleted || 0; } catch (e) {}
    try { const r = await BANS.where({ key: _.in(["user:" + username]) }).remove(); cleaned.bans = r.deleted || 0; } catch (e) {}
    try { const r = await db.collection("vip_coin_snapshots").where({ username }).remove(); cleaned.coinSnapshot = r.deleted || 0; } catch (e) {}
    try { const r = await db.collection("vip_revocations").where({ username }).remove(); cleaned.revocations = r.deleted || 0; } catch (e) {}
    // vip_coin_logs 故意保留作为流水审计

    await audit(db, req, "user_delete", { username, deviceId: (u.deviceId || "").slice(0, 16) + "...", usedCodes: usedCount, force: !!force, cleaned });
    res.json({ ok: true, data: { cleaned, usedCodes: usedCount } });
  } catch (e) { sendError(res, 500, "INTERNAL", "删除用户失败", e); }
});

/** 批量删除用户（清理幽灵号 - 注册后从未激活任何卡密的） */
app.post("/api/users/batch_delete_ghost", async (req, res) => {
  try {
    const { dryRun = true, daysSinceRegister = 7 } = req.body || {};
    // 取所有用户
    const ur = await USERS.limit(2000).get();
    const all = ur.data || [];
    // 取所有 used 卡密的 usedByUser 集合
    const cr = await CODES.where({ status: "used" }).field({ usedByUser: true }).limit(5000).get();
    const usedUsers = new Set((cr.data || []).map((x) => x.usedByUser).filter(Boolean));

    const since = new Date(Date.now() - daysSinceRegister * 86400 * 1000);
    const ghosts = all.filter((u) => {
      if (!u.username || u.username === "admin") return false;
      if (usedUsers.has(u.username)) return false; // 有激活的不算幽灵
      // 注册时间足够久（避免误删刚注册还没买的真用户）
      const reg = u.registeredAt || u.createdAt;
      if (!reg || new Date(reg) > since) return false;
      return true;
    });

    if (dryRun) {
      return res.json({
        ok: true,
        dryRun: true,
        data: {
          count: ghosts.length,
          sample: ghosts.slice(0, 50).map((u) => ({
            username: u.username, nickname: u.nickname,
            registeredAt: u.registeredAt || u.createdAt,
            deviceId: (u.deviceId || "").slice(0, 16) + "...",
          })),
        },
      });
    }

    let deleted = 0;
    for (const u of ghosts) {
      try {
        await USERS.where({ username: u.username }).remove();
        await BANS.where({ key: "user:" + u.username }).remove().catch(() => {});
        await db.collection("vip_coin_snapshots").where({ username: u.username }).remove().catch(() => {});
        deleted++;
      } catch (e) { /* skip */ }
    }
    await audit(db, req, "user_batch_delete_ghost", { matched: ghosts.length, deleted, daysSinceRegister });
    res.json({ ok: true, data: { matched: ghosts.length, deleted } });
  } catch (e) { sendError(res, 500, "INTERNAL", "批量删除失败", e); }
});

// ─────────────────────────────────────────────
// 阶段 2：删除 / 重置 / 备注 / 编辑 SKU
// ─────────────────────────────────────────────

/** 永久删除卡密（仅 unused 可删；used 强制要求带 force=true） */
app.post("/api/codes/:code/delete", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const { force = false } = req.body || {};
    const r1 = await CODES.where({ code }).limit(1).get();
    const x = r1.data && r1.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");
    if (x.status === "used" && !force) {
      return sendError(res, 400, "USED_REQUIRE_FORCE", "已激活的卡密需要 force=true 才能删除");
    }
    await CODES.where({ code }).remove();
    await audit(db, req, "code_delete", { code, wasStatus: x.status, force: !!force });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "删除卡密失败", e); }
});

/** 重置卡密为 unused（仅极敏感，给客服用：例如用户反馈兑换有问题） */
app.post("/api/codes/:code/reset", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const { reason = "" } = req.body || {};
    const r1 = await CODES.where({ code }).limit(1).get();
    const x = r1.data && r1.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");
    if (x.status !== "used") {
      return sendError(res, 400, "NOT_USED", "只有 used 状态的卡密可以重置");
    }
    await CODES.where({ code }).update({
      status: "unused",
      usedByDevice: null,
      usedByUser: null,
      usedAt: null,
      adminResetAt: db.serverDate(),
      adminResetReason: reason.slice(0, 200),
    });
    await audit(db, req, "code_reset", { code, oldDevice: x.usedByDevice, reason });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "重置卡密失败", e); }
});

/** 修改卡密备注 / SKU（unused 才能改 SKU） */
app.post("/api/codes/:code/update", async (req, res) => {
  try {
    const code = normalize(req.params.code);
    const { note, skuCode } = req.body || {};
    const r1 = await CODES.where({ code }).limit(1).get();
    const x = r1.data && r1.data[0];
    if (!x) return sendError(res, 404, "NOT_FOUND", "卡密不存在");

    const update = {};
    if (typeof note === "string") update.note = note.slice(0, 200);
    if (skuCode) {
      if (!SKU[skuCode]) return sendError(res, 400, "INVALID_SKU", "未知 SKU");
      if (x.status !== "unused") {
        return sendError(res, 400, "NOT_UNUSED", "已激活的卡密不可改 SKU");
      }
      update.skuCode = skuCode;
    }
    if (Object.keys(update).length === 0) {
      return sendError(res, 400, "EMPTY", "没有可更新的字段");
    }
    await CODES.where({ code }).update(update);
    await audit(db, req, "code_update", { code, ...update });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "更新卡密失败", e); }
});

// ─────────────────────────────────────────────
// 阶段 3：操作审计 + 系统健康 + 7 日趋势
// ─────────────────────────────────────────────
const AUDIT = db.collection("vip_admin_audit");

/** admin 操作审计：分页查询 */
app.get("/api/audit", async (req, res) => {
  try {
    const { admin, action, page = "1", limit = "50" } = req.query;
    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const lim = Math.min(200, Math.max(1, parseInt(limit, 10) || 50));

    // 内存层过滤（量小，简单）
    const r = await safeQuery(AUDIT.orderBy("at", "desc").limit(500).get());
    // 🔧 兼容 CloudBase add({data:{...}}) 写入的嵌套结构
    let items = (r.data || []).map((x) =>
      (x && x.data && (x.data.action || x.data.admin)) ? { _id: x._id, ...x.data } : x
    );
    if (admin) items = items.filter((x) => x.admin === admin);
    if (action) items = items.filter((x) => x.action === action);
    const total = items.length;
    items = items.slice((pageNum - 1) * lim, pageNum * lim);
    res.json({ ok: true, data: { items, total, page: pageNum, limit: lim } });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询审计日志失败", e); }
});

/** 系统健康：各集合存量 + 限流命中 + 最近错误 */
app.get("/api/health", async (req, res) => {
  try {
    const since1h = new Date(Date.now() - 3600 * 1000);
    const since24h = new Date(Date.now() - 24 * 3600 * 1000);

    const collections = [
      "vip_codes", "vip_users", "vip_user_bans", "vip_redeem_log",
      "vip_coin_logs", "vip_coin_snapshots", "vip_rate_limit",
      "vip_admin_audit", "vip_device_marks", "vip_revocations", "vip_coin_nonces",
    ];
    const counts = {};
    for (const name of collections) {
      try {
        const r = await db.collection(name).count();
        counts[name] = r.total || 0;
      } catch (e) {
        counts[name] = -1; // 集合不存在
      }
    }

    // 限流命中率：近 1h 与 24h 的 RATE_LIMITED 拒绝次数（在 vip_redeem_log 里查）
    let rateLimitHits1h = 0, rateLimitHits24h = 0;
    try {
      const r = await LOG.where({ at: _.gte(since24h) }).limit(1000).get();
      const items = (r.data || []).map((x) =>
        (x && x.data && (x.data.action || x.data.reason)) ? { _id: x._id, ...x.data } : x);
      rateLimitHits24h = items.filter((x) => x.reason === "RATE_LIMITED").length;
      rateLimitHits1h = items.filter((x) =>
        x.reason === "RATE_LIMITED" && x.at && new Date(x.at) >= since1h
      ).length;
    } catch (e) {}

    // 最近 5 条 admin 错误（从本地日志读尾部）
    let recentErrors = [];
    try {
      const file = path.join(__dirname, ".logs", "admin-error.log");
      if (fs.existsSync(file)) {
        const lines = fs.readFileSync(file, "utf-8").trim().split("\n");
        recentErrors = lines.slice(-10).reverse().map((l) => l.slice(0, 300));
      }
    } catch (e) {}

    res.json({
      ok: true,
      data: {
        collections: counts,
        rateLimitHits: { last1h: rateLimitHits1h, last24h: rateLimitHits24h },
        recentErrors,
        serverTime: new Date().toISOString(),
      },
    });
  } catch (e) { sendError(res, 500, "INTERNAL", "系统健康检查失败", e); }
});

/** 7 日兑换趋势（按天聚合 used 卡密数 + 收入） */
app.get("/api/trend/redeem", async (req, res) => {
  try {
    const days = Math.min(30, Math.max(1, parseInt(req.query.days, 10) || 7));
    const since = new Date(Date.now() - days * 86400 * 1000);
    // usedAt 是 used 时间戳；按 yyyy-mm-dd 聚合
    const r = await CODES.where({ usedAt: _.gte(since) }).limit(2000).get();
    const buckets = {};
    for (let i = 0; i < days; i++) {
      const d = new Date(Date.now() - i * 86400 * 1000);
      const key = d.toISOString().slice(0, 10);
      buckets[key] = { date: key, count: 0, revenue: 0, bySku: {} };
    }
    (r.data || []).forEach((x) => {
      if (!x.usedAt) return;
      const key = new Date(x.usedAt).toISOString().slice(0, 10);
      if (!buckets[key]) return;
      const cfg = SKU[x.skuCode];
      buckets[key].count += 1;
      buckets[key].revenue += (cfg && cfg.price) || 0;
      buckets[key].bySku[x.skuCode] = (buckets[key].bySku[x.skuCode] || 0) + 1;
    });
    const list = Object.values(buckets).sort((a, b) => a.date.localeCompare(b.date));
    res.json({ ok: true, data: list });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询趋势失败", e); }
});

// ─────────────────────────────────────────────
// 🆕 v51 AI 调用看板
// ─────────────────────────────────────────────

/** 工具：当前 ym（202605） / ymd（20260527），与云函数实现一致（UTC） */
function _currentYm() {
  const dt = new Date();
  return dt.getUTCFullYear() * 100 + (dt.getUTCMonth() + 1);
}
function _currentYmd() {
  const local = new Date(Date.now() + 8 * 3600 * 1000); // 东八区，与 chat_ai 保持一致
  return local.getUTCFullYear() * 10000 + (local.getUTCMonth() + 1) * 100 + local.getUTCDate();
}

/** 单次调用估算成本（人民币元）。基于 deepseek-chat 实测 ~300-500 tokens */
const COST_PER_CALL = 0.001;

const LETTER_QUOTA = db.collection("letter_quota");
const CHAT_AI_QUOTA = db.collection("chat_ai_quota");

/** 总览：本月信件代理 + 今日聊天代理 + 异常率 + 成本估算 */
app.get("/api/ai/stats", async (req, res) => {
  try {
    const ym = _currentYm();
    const ymd = _currentYmd();

    // letter_ai 本月
    const lr = await safeQuery(LETTER_QUOTA.where({ ym }).limit(1000).get());
    const letterDocs = lr.data || [];
    const letterTotal = letterDocs.reduce((s, d) => s + (d.count || 0), 0);
    const letterDevices = letterDocs.length;

    // chat_ai 今日
    const cr = await safeQuery(CHAT_AI_QUOTA.where({ ymd }).limit(2000).get());
    const chatDocs = cr.data || [];
    const chatTotal = chatDocs.reduce((s, d) => s + (d.count || 0), 0);
    const chatDevices = chatDocs.length;

    // 失败次数（最近 24h，从 vip_redeem_log 里 action=*_failed 计数）
    const since24h = new Date(Date.now() - 24 * 3600 * 1000);
    const flr = await safeQuery(
      LOG.where({ action: "letter_ai_failed", at: _.gte(since24h) }).limit(500).get()
    );
    const fcr = await safeQuery(
      LOG.where({ action: "chat_ai_failed", at: _.gte(since24h) }).limit(500).get()
    );
    const letterFailed = (flr.data || []).length;
    const chatFailed = (fcr.data || []).length;

    res.json({
      ok: true,
      data: {
        period: { ym, ymd },
        letter: {
          monthlyCalls: letterTotal,
          activeDevices: letterDevices,
          failed24h: letterFailed,
          failureRate: letterTotal > 0
            ? Number((letterFailed / (letterTotal + letterFailed) * 100).toFixed(2)) : 0,
          estCostCny: Number((letterTotal * COST_PER_CALL).toFixed(2)),
        },
        chat: {
          dailyCalls: chatTotal,
          activeDevices: chatDevices,
          failed24h: chatFailed,
          failureRate: chatTotal > 0
            ? Number((chatFailed / (chatTotal + chatFailed) * 100).toFixed(2)) : 0,
          estCostCny: Number((chatTotal * COST_PER_CALL).toFixed(2)),
        },
        totalCostCnyTodayApprox: Number(((letterTotal + chatTotal) * COST_PER_CALL).toFixed(2)),
      },
    });
  } catch (e) { sendError(res, 500, "INTERNAL", "AI 看板查询失败", e); }
});

/** 调用排行 Top 20（哪些设备调用最多 → 异常 / 滥用排查） */
app.get("/api/ai/top_devices", async (req, res) => {
  try {
    const ym = _currentYm();
    const ymd = _currentYmd();
    const lr = await safeQuery(LETTER_QUOTA.where({ ym }).limit(2000).get());
    const cr = await safeQuery(CHAT_AI_QUOTA.where({ ymd }).limit(2000).get());

    const merged = {};
    (lr.data || []).forEach(d => {
      const k = d.deviceId; if (!k) return;
      if (!merged[k]) merged[k] = { deviceId: k, letter: 0, chat: 0 };
      merged[k].letter += d.count || 0;
    });
    (cr.data || []).forEach(d => {
      const k = d.deviceId; if (!k) return;
      if (!merged[k]) merged[k] = { deviceId: k, letter: 0, chat: 0 };
      merged[k].chat += d.count || 0;
    });
    const list = Object.values(merged)
      .map(x => ({ ...x, total: x.letter + x.chat }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 20);
    res.json({ ok: true, data: list });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询失败", e); }
});

/** 最近失败日志 */
app.get("/api/ai/failures", async (req, res) => {
  try {
    const limit = Math.min(parseInt(req.query.limit || "50", 10), 200);
    const r = await safeQuery(
      LOG.where({ action: _.in(["letter_ai_failed", "chat_ai_failed"]) })
        .orderBy("at", "desc").limit(limit).get()
    );
    // 兼容历史 add({data:{...}}) 嵌套结构
    const items = (r.data || []).map(x => {
      const d = (x && x.data && typeof x.data === "object") ? x.data : x;
      return {
        action: d.action || x.action,
        deviceId: d.deviceId || x.deviceId,
        reason: d.reason || x.reason,
        ip: d.ip || x.ip,
        at: d.at || x.at,
      };
    });
    res.json({ ok: true, data: items });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询失败", e); }
});

// ═══════════════════════════════════════════════════════════════
// 🆕 v53 阅光书房 · 内容审核 + AI 调用监控
// ═══════════════════════════════════════════════════════════════

const QUOTE_GALAXY = db.collection("quote_galaxy");
const GALAXY_REPORTS = db.collection("galaxy_reports");
const POSTCARDS = db.collection("postcards");
const CHAT_AI_BOOK_QUOTA = db.collection("chat_ai_book_quota");
const CHAT_AI_DNA_QUOTA = db.collection("chat_ai_dna_quota");

// ─── v53.1 摘抄星河审核 ───
/** 星河内容列表
 *  ?status=reported     仅看被举报过的（reportCount > 0）
 *  ?status=hidden       仅看已隐藏的
 *  ?status=needsReview  仅看新过滤器标记的软警告（needsReview=true）
 *  ?status=all          全部
 */
app.get("/api/v53/galaxy/items", async (req, res) => {
  try {
    const status = String(req.query.status || "reported");
    const limit = Math.min(parseInt(req.query.limit || "100", 10), 500);
    let q;
    if (status === "hidden") {
      q = QUOTE_GALAXY.where({ hidden: true }).orderBy("publishedAt", "desc").limit(limit);
    } else if (status === "needsReview") {
      q = QUOTE_GALAXY.where({ needsReview: true }).orderBy("publishedAt", "desc").limit(limit);
    } else if (status === "all") {
      q = QUOTE_GALAXY.orderBy("publishedAt", "desc").limit(limit);
    } else {
      // reported: reportCount > 0
      q = QUOTE_GALAXY.where({ reportCount: _.gt(0) }).orderBy("reportCount", "desc").limit(limit);
    }
    const r = await safeQuery(q.get());
    res.json({ ok: true, data: r.data || [] });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询星河内容失败", e); }
});

/** 强制隐藏一条星河内容 */
app.post("/api/v53/galaxy/items/:id/hide", async (req, res) => {
  try {
    const id = req.params.id;
    const { reason = "" } = req.body || {};
    await QUOTE_GALAXY.doc(id).update({ hidden: true, hiddenAt: Date.now(), hiddenBy: req.admin && req.admin.username, hiddenReason: reason });
    await LOG.add({ data: { action: "v53_galaxy_hide", id, by: req.admin && req.admin.username, reason, at: db.serverDate() } });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "隐藏失败", e); }
});

/** 恢复一条被隐藏的星河内容 */
app.post("/api/v53/galaxy/items/:id/restore", async (req, res) => {
  try {
    const id = req.params.id;
    await QUOTE_GALAXY.doc(id).update({ hidden: false, restoredAt: Date.now(), restoredBy: req.admin && req.admin.username });
    await LOG.add({ data: { action: "v53_galaxy_restore", id, by: req.admin && req.admin.username, at: db.serverDate() } });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "恢复失败", e); }
});

/** 某条星河的举报详情 */
app.get("/api/v53/galaxy/items/:id/reports", async (req, res) => {
  try {
    const id = req.params.id;
    const r = await safeQuery(GALAXY_REPORTS.where({ starId: id }).orderBy("at", "desc").limit(100).get());
    res.json({ ok: true, data: r.data || [] });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询举报失败", e); }
});

/** 星河汇总指标 */
app.get("/api/v53/galaxy/stats", async (req, res) => {
  try {
    const all = await safeQuery(QUOTE_GALAXY.limit(2000).get());
    const items = all.data || [];
    const total = items.length;
    const hidden = items.filter(x => x.hidden).length;
    const reported = items.filter(x => (x.reportCount || 0) > 0).length;
    const needsReview = items.filter(x => x.needsReview).length;
    const totalLights = items.reduce((s, x) => s + (x.lightCount || 0), 0);
    const totalReports = items.reduce((s, x) => s + (x.reportCount || 0), 0);
    res.json({ ok: true, data: { total, hidden, reported, needsReview, totalLights, totalReports } });
  } catch (e) { sendError(res, 500, "INTERNAL", "汇总失败", e); }
});

// ─── v53.1 明信片漂流审核 ───
/** 明信片内容列表 */
app.get("/api/v53/postcards/items", async (req, res) => {
  try {
    const status = String(req.query.status || "all");
    const limit = Math.min(parseInt(req.query.limit || "100", 10), 500);
    let q;
    if (status === "hidden") {
      q = POSTCARDS.where({ hidden: true }).orderBy("sentAt", "desc").limit(limit);
    } else {
      q = POSTCARDS.orderBy("sentAt", "desc").limit(limit);
    }
    const r = await safeQuery(q.get());
    res.json({ ok: true, data: r.data || [] });
  } catch (e) { sendError(res, 500, "INTERNAL", "查询明信片失败", e); }
});

/** 强制隐藏一张明信片 */
app.post("/api/v53/postcards/items/:id/hide", async (req, res) => {
  try {
    const id = req.params.id;
    const { reason = "" } = req.body || {};
    await POSTCARDS.doc(id).update({ hidden: true, hiddenAt: Date.now(), hiddenBy: req.admin && req.admin.username, hiddenReason: reason });
    await LOG.add({ data: { action: "v53_postcard_hide", id, by: req.admin && req.admin.username, reason, at: db.serverDate() } });
    res.json({ ok: true });
  } catch (e) { sendError(res, 500, "INTERNAL", "隐藏失败", e); }
});

/** 明信片汇总指标 */
app.get("/api/v53/postcards/stats", async (req, res) => {
  try {
    const all = await safeQuery(POSTCARDS.limit(2000).get());
    const items = all.data || [];
    const total = items.length;
    const reacted = items.filter(x => x.reactedHeart).length;
    const hidden = items.filter(x => x.hidden).length;
    res.json({ ok: true, data: { total, reacted, hidden } });
  } catch (e) { sendError(res, 500, "INTERNAL", "汇总失败", e); }
});

// ─── v53.1 AI 调用监控扩展（book / reader_dna 桶） ───
/** v53 AI 调用今日总览 · 按 mode 分桶 */
app.get("/api/v53/ai/stats", async (req, res) => {
  try {
    const ymd = _currentYmd();
    const [chat, book, dna] = await Promise.all([
      safeQuery(CHAT_AI_QUOTA.where({ ymd }).limit(2000).get()),
      safeQuery(CHAT_AI_BOOK_QUOTA.where({ ymd }).limit(2000).get()),
      safeQuery(CHAT_AI_DNA_QUOTA.where({ ymd }).limit(2000).get()),
    ]);
    function summarize(docs) {
      const total = docs.reduce((s, d) => s + (d.count || 0), 0);
      const devices = docs.length;
      const avg = devices > 0 ? +(total / devices).toFixed(2) : 0;
      const max = docs.reduce((m, d) => Math.max(m, d.count || 0), 0);
      return { total, devices, avg, max };
    }
    res.json({
      ok: true, data: {
        ymd,
        chat: summarize(chat.data || []),
        book: summarize(book.data || []),
        reader_dna: summarize(dna.data || []),
        cost_estimate: {
          chat: +(((chat.data || []).reduce((s, d) => s + (d.count || 0), 0)) * COST_PER_CALL).toFixed(4),
          book: +(((book.data || []).reduce((s, d) => s + (d.count || 0), 0)) * COST_PER_CALL).toFixed(4),
          reader_dna: +(((dna.data || []).reduce((s, d) => s + (d.count || 0), 0)) * COST_PER_CALL).toFixed(4),
        }
      }
    });
  } catch (e) { sendError(res, 500, "INTERNAL", "AI 统计失败", e); }
});

/** v53 AI 调用排行 Top 20（合并三个 mode） */
app.get("/api/v53/ai/top_devices", async (req, res) => {
  try {
    const ymd = _currentYmd();
    const [chat, book, dna] = await Promise.all([
      safeQuery(CHAT_AI_QUOTA.where({ ymd }).limit(2000).get()),
      safeQuery(CHAT_AI_BOOK_QUOTA.where({ ymd }).limit(2000).get()),
      safeQuery(CHAT_AI_DNA_QUOTA.where({ ymd }).limit(2000).get()),
    ]);
    const merged = {};
    function bump(docs, key) {
      (docs || []).forEach(d => {
        const id = d.deviceId || "unknown";
        if (!merged[id]) merged[id] = { deviceId: id, chat: 0, book: 0, reader_dna: 0, total: 0 };
        merged[id][key] += d.count || 0;
        merged[id].total += d.count || 0;
      });
    }
    bump(chat.data, "chat");
    bump(book.data, "book");
    bump(dna.data, "reader_dna");
    const sorted = Object.values(merged).sort((a, b) => b.total - a.total).slice(0, 20);
    res.json({ ok: true, data: sorted });
  } catch (e) { sendError(res, 500, "INTERNAL", "排行失败", e); }
});

// ─────────────────────────────────────────────
// 主页
// ─────────────────────────────────────────────
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "index.html"));
});

// 🆕 v53 阅光书房 · 内容审核独立面板
app.get("/v53", (req, res) => {
  res.sendFile(path.join(__dirname, "public", "v53-dashboard.html"));
});

const PORT = 3300;
app.listen(PORT, () => {
  console.log(`\n  📊 FunLife VIP 管理后台\n  → http://localhost:${PORT}\n  按 Ctrl+C 退出\n`);
});
