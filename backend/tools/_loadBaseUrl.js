const fs = require("fs");
const path = require("path");

function loadBaseUrl(defaultUrl = "https://api.yishi.site") {
  if (process.env.VIP_BACKEND_URL) return process.env.VIP_BACKEND_URL.replace(/\/$/, "");
  if (process.env.FUNLIFE_BACKEND_URL) return process.env.FUNLIFE_BACKEND_URL.replace(/\/$/, "");
  const lp = path.join(__dirname, "..", "..", "local.properties");
  if (fs.existsSync(lp)) {
    const m = fs.readFileSync(lp, "utf-8").match(/VIP_BACKEND_URL=(.+)/);
    if (m) return m[1].trim().replace(/\/$/, "");
  }
  return defaultUrl;
}

module.exports = { loadBaseUrl };
