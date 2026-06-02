// 生成 admin 密码 bcrypt 哈希
// 用法：node hash-password.js <password>
const bcrypt = require("bcryptjs");
const pwd = process.argv[2];
if (!pwd) {
  console.error("用法：node hash-password.js <你的密码>");
  process.exit(1);
}
if (pwd.length < 8) {
  console.error("密码至少 8 位");
  process.exit(1);
}
const hash = bcrypt.hashSync(pwd, 10);
console.log("\n  把下面这行加到 backend/tools/.env：\n");
console.log("  ADMIN_PASSWORD_HASH=" + hash);
console.log("\n  另外别忘了在 .env 里加：");
console.log("  ADMIN_USERNAME=admin");
console.log("  ADMIN_SESSION_SECRET=" + require("crypto").randomBytes(32).toString("hex"));
console.log("");
