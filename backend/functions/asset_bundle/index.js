// ============================================================
// 云存储资源包：签发 manifest + 临时下载链接
// POST /asset_bundle
//   { "action": "manifest" }
//   { "action": "sign", "bundleId": "pet" }
// ============================================================
// 与 backend/tools/asset_manifest.json 保持同步

const cloudbase = require("@cloudbase/node-sdk");

const ASSET_ROOT = "yishi-assetss/v1";
const MANIFEST = {
  version: 10,
  updatedAt: "2026-06-22T12:00:00Z",
  bundles: [
    { id: "xiangkuang", file: "xiangkuang.zip", targetDir: "xiangkuang" },
    { id: "pet", file: "pet.zip", targetDir: "pet" },
    { id: "login", file: "login.zip", targetDir: "login" },
    { id: "renge", file: "renge.zip", targetDir: "renge" },
    { id: "dibu", file: "dibu.zip", targetDir: "dibu" },
    { id: "wheel", file: "wheel.zip", targetDir: "wheel" },
    { id: "pac_maze_sfx", file: "pac_maze_sfx.zip", targetDir: "pac_maze_sfx" },
    { id: "pac_maze_skins", file: "pac_maze_skins.zip", targetDir: "pac_maze_skins" },
    {
      id: "platformer_characters",
      file: "platformer_characters.zip",
      targetDir: "platformer_characters",
    },
  ],
};

const app = cloudbase.init({ env: cloudbase.SYMBOL_CURRENT_ENV });

function resolveEnvId() {
  return (
    process.env.TCB_ENV ||
    process.env.SCF_NAMESPACE ||
    "funlife-prod-d8gxf7og0518b8253"
  );
}

/** 云存储 bucket 域名（非目录名 yishi-assetss），与 uploadFile 返回的 fileID 一致 */
function resolveStorageBucket() {
  return (
    process.env.TCB_STORAGE_BUCKET ||
    "6675-funlife-prod-d8gxf7og0518b8253-1333176506"
  );
}

function toFileId(cloudPath) {
  const envId = resolveEnvId();
  const bucket = resolveStorageBucket();
  return `cloud://${envId}.${bucket}/${cloudPath}`;
}

async function signPaths(paths, maxAgeSec = 7200) {
  if (!paths.length) return {};
  const fileList = paths.map((p) => ({ fileID: toFileId(p), maxAge: maxAgeSec }));
  const r = await app.getTempFileURL({ fileList });
  const out = {};
  (r.fileList || []).forEach((item, i) => {
    if (item && item.tempFileURL) {
      out[paths[i]] = item.tempFileURL;
    } else if (item && item.code) {
      console.warn("sign fail", paths[i], item.code, item.message || "");
    }
  });
  return out;
}

exports.main = async (event) => {
  let body = event;
  if (event && event.body) {
    try {
      body = typeof event.body === "string" ? JSON.parse(event.body) : event.body;
    } catch (e) {
      return { ok: false, code: "BAD_REQUEST" };
    }
  }

  const action = (body.action || "manifest").trim();

  try {
    if (action === "manifest") {
      const bundlePaths = MANIFEST.bundles.map((b) => `${ASSET_ROOT}/bundles/${b.file}`);
      const fileList = bundlePaths.map((p) => ({ fileID: toFileId(p), maxAge: 7200 }));
      const raw = await app.getTempFileURL({ fileList });
      const signed = await signPaths(bundlePaths);
      const bundles = MANIFEST.bundles.map((b) => {
        const cloudPath = `${ASSET_ROOT}/bundles/${b.file}`;
        return {
          id: b.id,
          file: b.file,
          targetDir: b.targetDir,
          url: signed[cloudPath] || null,
        };
      });
      const resp = {
        ok: true,
        version: MANIFEST.version,
        updatedAt: MANIFEST.updatedAt,
        bundles,
      };
      if (body.debug) {
        resp._env = resolveEnvId();
        resp._raw = raw;
      }
      return resp;
    }

    if (action === "sign") {
      const bundleId = (body.bundleId || "").trim();
      const hit = MANIFEST.bundles.find((b) => b.id === bundleId);
      if (!hit) return { ok: false, code: "NOT_FOUND", msg: "未知资源包" };
      const cloudPath = `${ASSET_ROOT}/bundles/${hit.file}`;
      const signed = await signPaths([cloudPath]);
      return {
        ok: true,
        bundleId: hit.id,
        targetDir: hit.targetDir,
        file: hit.file,
        url: signed[cloudPath] || null,
      };
    }

    return { ok: false, code: "INVALID", msg: "未知 action" };
  } catch (e) {
    return { ok: false, code: "ERROR", msg: e.message || String(e) };
  }
};
