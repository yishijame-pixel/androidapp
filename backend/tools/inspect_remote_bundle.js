const https = require("https");
const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const BASE =
  process.env.FUNLIFE_BACKEND_URL ||
  "https://funlife-prod-d8gxf7og0518b8253-1333176506.ap-shanghai.app.tcloudbase.com";

function post(body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const url = new URL(BASE + "/asset_bundle");
    const req = https.request(
      {
        hostname: url.hostname,
        path: url.pathname,
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(data),
        },
        timeout: 30000,
      },
      (res) => {
        let buf = "";
        res.on("data", (c) => (buf += c));
        res.on("end", () => resolve(JSON.parse(buf)));
      },
    );
    req.on("error", reject);
    req.write(data);
    req.end();
  });
}

function download(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https
      .get(url, (res) => {
        if (res.statusCode !== 200) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }
        res.pipe(file);
        file.on("finish", () => file.close(resolve));
      })
      .on("error", reject);
  });
}

function zipEntryText(zipPath, entry) {
  const ps = `
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::OpenRead('${zipPath.replace(/\\/g, "/")}')
$e = $z.GetEntry('${entry}')
if ($e) { $sr = New-Object IO.StreamReader($e.Open()); $sr.ReadToEnd() } else { 'MISSING' }
$z.Dispose()
`;
  return execSync(`powershell -NoProfile -Command "${ps.replace(/"/g, '\\"')}"`, {
    encoding: "utf8",
  }).trim();
}

function zipHasEntry(zipPath, entry) {
  const ps = `
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [IO.Compression.ZipFile]::OpenRead('${zipPath.replace(/\\/g, "/")}')
$ok = $z.GetEntry('${entry}') -ne $null
$z.Dispose()
Write-Output $ok
`;
  return execSync(`powershell -NoProfile -Command "${ps.replace(/"/g, '\\"')}"`, {
    encoding: "utf8",
  }).trim();
}

async function main() {
  const manifest = await post({ action: "manifest" });
  console.log("manifest version:", manifest.version);
  console.log(
    "bundles:",
    manifest.bundles.map((b) => `${b.id}${b.url ? "" : "(no url)"}`).join(", "),
  );

  for (const id of ["pac_maze_skins", "platformer_characters"]) {
    const hit = manifest.bundles.find((b) => b.id === id);
    if (!hit) {
      console.log(`\n${id}: NOT IN DEPLOYED MANIFEST`);
      continue;
    }
    if (!hit.url) {
      console.log(`\n${id}: no signed url`);
      continue;
    }
    const zipPath = path.join(__dirname, `..`, `.tmp_${id}.zip`);
    console.log(`\nDownloading ${id}...`);
    await download(hit.url, zipPath);
    console.log("size:", fs.statSync(zipPath).size);
    const version = zipEntryText(zipPath, `${id}/bundle_version.txt`);
    console.log("bundle_version.txt:", version);
    if (id === "pac_maze_skins") {
      for (const mk of [
        "pac_maze_skins/food_chick_walker_pro_max/walk/walk_1.png",
        "pac_maze_skins/xia_walk/walk/walk_1.png",
      ]) {
        console.log(mk, zipHasEntry(zipPath, mk));
      }
    }
    fs.unlinkSync(zipPath);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
