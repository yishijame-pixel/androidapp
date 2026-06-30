#!/usr/bin/env node
/**
 * 静态资源站 E2E（替代旧 /asset_bundle 云函数）
 * 用法: node test_assets_static.js
 */
const https = require("https");
const { loadBaseUrl } = require("./_loadBaseUrl");

const MANIFEST_URL = process.env.ASSET_MANIFEST_URL || "https://assets.yishi.site/manifest.json";
const BASE = loadBaseUrl();

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { timeout: 20000 }, (res) => {
        let buf = "";
        res.on("data", (c) => {
          buf += c;
        });
        res.on("end", () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(buf.replace(/^\uFEFF/, "")) });
          } catch (e) {
            resolve({ status: res.statusCode, body: null, raw: buf.slice(0, 200) });
          }
        });
      })
      .on("error", reject);
  });
}

function head(url) {
  return new Promise((resolve, reject) => {
    const req = https.request(url, { method: "HEAD", timeout: 20000 }, (res) => {
      res.resume();
      resolve({ status: res.statusCode, len: res.headers["content-length"] });
    });
    req.on("error", reject);
    req.on("timeout", () => req.destroy(new Error("TIMEOUT")));
    req.end();
  });
}

let pass = 0;
let fail = 0;
function expect(name, ok, detail) {
  if (ok) {
    pass++;
    console.log(`  ✅ ${name}`);
  } else {
    fail++;
    console.error(`  ❌ ${name}`);
    if (detail !== undefined) console.error("    ", detail);
  }
}

async function main() {
  console.log("\n=== E2E assets static ===");
  console.log("MANIFEST:", MANIFEST_URL);
  console.log("API BASE:", BASE, "(bundle URL 应与 manifest 一致)\n");

  const m = await fetchJson(MANIFEST_URL);
  expect("manifest HTTP 200", m.status === 200, m.status);
  expect("manifest.version", !!m.body?.version, m.body);
  expect("manifest.bundles 非空", Array.isArray(m.body?.bundles) && m.body.bundles.length > 0, null);

  const login = m.body?.bundles?.find((b) => b.id === "login");
  expect("bundles 含 login", !!login, null);
  expect("login.url 指向 assets.yishi.site", login?.url?.includes("assets.yishi.site"), login?.url);

  if (login?.url) {
    const h = await head(login.url);
    expect("login.zip HEAD 200", h.status === 200, h);
    expect("login.zip 有 content-length", Number(h.len) > 1000, h.len);
  }

  console.log(`\n=== 结果: ${pass} 通过 / ${fail} 失败 ===\n`);
  process.exit(fail > 0 ? 1 : 0);
}

main().catch((e) => {
  console.error("FATAL", e);
  process.exit(2);
});
