// ============================================================
// 共享代码同步脚本：把 shared/*.js 复制到所有云函数目录
// ------------------------------------------------------------
// 云函数部署是按目录打包，无法跨目录 require，所以维护时
// 改 shared/xxx.js → 跑 `node sync-sku.js` → 再 deploy
// ============================================================
const fs = require("fs");
const path = require("path");

const HEADER_TPL = (src) =>
  `// ⚠️ 自动生成，请勿手改。源文件: backend/shared/${src}\n` +
  `// 修改方式：编辑 shared/${src} 后运行 \`node sync-sku.js\`\n`;

// { 源文件 -> [需要它的云函数目录] }
const SYNC_MAP = {
  "sku.js": [
    "functions/redeem", "functions/migrate", "functions/beta_validate",
    "functions/vip_config", "functions/chat_ai",
  ],
  "chat_ai_limits.js": [
    "functions/redeem", "functions/chat_ai",
  ],
  "rate-limit.js": [
    "functions/redeem", "functions/migrate", "functions/verify",
    "functions/beta_validate", "functions/register_log",
    "functions/user_status", "functions/coin_log",
    "functions/account_recover",
  ],
  "identity.js": [
    "functions/register_log", "functions/coin_log", "functions/migrate",
    "functions/account_recover",
  ],
  "account-recover-core.js": [
    "functions/account_recover",
  ],
};

let count = 0;
for (const [filename, dirs] of Object.entries(SYNC_MAP)) {
  const srcPath = path.join(__dirname, "shared", filename);
  if (!fs.existsSync(srcPath)) {
    console.warn("⚠ 源文件不存在，跳过:", filename);
    continue;
  }
  const src = fs.readFileSync(srcPath, "utf8");
  const header = HEADER_TPL(filename);
  for (const d of dirs) {
    const target = path.join(__dirname, d, filename);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, header + src);
    console.log("✓ synced →", path.relative(__dirname, target));
    count++;
  }
}
console.log(`\n${count} 个文件同步完成`);
