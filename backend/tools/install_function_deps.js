#!/usr/bin/env node
/**
 * 为 backend/functions/* 安装 npm 依赖（funlife-api 本地/Docker 构建前执行）
 */
const { execSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..", "functions");
const dirs = fs.readdirSync(root).filter((d) => {
  const pkg = path.join(root, d, "package.json");
  return fs.existsSync(pkg);
});

for (const d of dirs) {
  const dir = path.join(root, d);
  console.log(`[install] ${d}`);
  execSync("npm install --omit=dev --silent", { cwd: dir, stdio: "inherit" });
}
console.log(`\n完成，共 ${dirs.length} 个云函数目录\n`);
