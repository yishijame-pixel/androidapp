// ============================================================
// 后台 Express 通用工具
// ------------------------------------------------------------
//   sendError(res, status, code, message, e?)
//     - 仅返回业务 code + 中文 message
//     - 把 e.stack 写本地日志（不返回前端）
//   wrap(handler)
//     - async 路由的统一异常包装，自动 catch → sendError
//   audit(db, req, action, payload)
//     - 写 admin_audit 集合，记录 admin 用户、IP、UA、时间
// ============================================================

const fs = require("fs");
const path = require("path");

const LOG_DIR = path.join(__dirname, ".logs");
try { fs.mkdirSync(LOG_DIR, { recursive: true }); } catch (e) {}
const LOG_FILE = path.join(LOG_DIR, "admin-error.log");

function logToFile(line) {
  try {
    fs.appendFileSync(LOG_FILE, `[${new Date().toISOString()}] ${line}\n`);
  } catch (e) { /* ignore */ }
}

/** 统一错误响应 */
function sendError(res, status, code, userMessage, e) {
  if (e) {
    const stack = e.stack || e.message || String(e);
    logToFile(`${code} | ${userMessage} | ${stack}`);
    console.error(`[admin-api] ${code}:`, e.message);
  }
  // 给前端的 error 字段必须是中文友好提示
  res.status(status).json({ ok: false, code, error: userMessage });
}

/** async route 自动错误兜底 */
function wrap(handler) {
  return async (req, res, next) => {
    try {
      await handler(req, res, next);
    } catch (e) {
      sendError(res, 500, "INTERNAL", "服务异常，请稍后重试或查看日志", e);
    }
  };
}

/** 操作审计 - 写 vip_admin_audit 集合 */
async function audit(db, req, action, payload = {}) {
  try {
    const COL = db.collection("vip_admin_audit");
    await COL.add({ data: {
      action,
      admin: (req.admin && req.admin.username) || "unknown",
      ip: (req.headers["x-forwarded-for"] || req.ip || "").toString().split(",")[0].trim(),
      ua: (req.headers["user-agent"] || "").slice(0, 200),
      payload,
      at: db.serverDate(),
    }});
  } catch (e) {
    // 审计失败不影响主流程，但要记录到本地
    logToFile(`AUDIT_FAILED | ${action} | ${e.message}`);
  }
}

module.exports = { sendError, wrap, audit, logToFile };
