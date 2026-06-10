// E2E: asset_bundle 云函数
// 用法: cd backend && node tools/test_asset_bundle.js

const https = require("https");

const BASE_URL = process.env.FUNLIFE_BACKEND_URL ||
  "https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com";

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
        timeout: 20000,
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
      },
    );
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("timeout")));
    req.write(data);
    req.end();
  });
}

async function main() {
  console.log("\n=== E2E asset_bundle ===\n");

  const manifest = await post("/asset_bundle", { action: "manifest" });
  if (!manifest.ok) {
    console.error("manifest failed", manifest);
    process.exit(1);
  }
  console.log("manifest version:", manifest.version, "bundles:", manifest.bundles.length);
  const withUrl = manifest.bundles.filter((b) => b.url);
  if (withUrl.length === 0) {
    console.error("no signed urls");
    process.exit(1);
  }
  console.log("signed urls:", withUrl.map((b) => b.id).join(", "));

  const sign = await post("/asset_bundle", { action: "sign", bundleId: "login" });
  if (!sign.ok || !sign.url) {
    console.error("sign failed", sign);
    process.exit(1);
  }
  console.log("sign login ok");

  console.log("\nAll asset_bundle tests passed.\n");
}

main().catch((e) => {
  console.error("FATAL", e);
  process.exit(1);
});
