// ============================================================
// 写入 pac_maze_ikun_disclosure / ikun_disclosure 默认文档
// 用法：cd backend/tools && node seed_pac_maze_ikun_disclosure.js
// 幂等：已存在且 version>0 时默认跳过（--force 覆盖）
// ============================================================

const { loadEnv } = require("./_loadEnv");
loadEnv();

const tcb = require("@cloudbase/node-sdk");

const DOC_ID = "ikun_disclosure";
const DEFAULT = {
  enabled: true,
  version: 1,
  title: "ikun类角色使用须知",
  body: [
    "欢迎使用「ikun类」梗图行走角色。进入本分类前，请先阅读以下说明：",
    "",
    "1. 本分类角色为娱乐向二次创作形象，部分素材来自网络梗图或用户上传的云端动画包，仅供游戏内娱乐体验。",
    "",
    "2. 请理性使用角色形象，勿用于侮辱、诽谤、骚扰他人，或从事任何违法违规活动。",
    "",
    "3. 若您认为某角色形象涉及侵权或不当内容，可通过应用内反馈渠道联系我们，我们将及时核实处理。",
    "",
    "4. 继续使用即表示您已理解上述说明，并同意在合法、合规、尊重他人的前提下使用本分类角色。",
    "",
    "感谢您的配合，祝您游戏愉快。",
  ].join("\n"),
  agreeButtonText: "我已阅读并同意",
  footerHint: "请滑动阅读全文后再点击同意",
  updatedAt: new Date().toISOString(),
  updatedBy: "seed_script",
};

async function main() {
  const envId = process.env.TCB_ENV_ID;
  const secretId = process.env.TCB_SECRET_ID;
  const secretKey = process.env.TCB_SECRET_KEY;
  if (!envId || !secretId || !secretKey) {
    console.error("缺少 TCB_ENV_ID / TCB_SECRET_ID / TCB_SECRET_KEY（见 tools/.env）");
    process.exit(1);
  }

  const force = process.argv.includes("--force");
  const app = tcb.init({ env: envId, secretId, secretKey });
  const db = app.database();
  const col = db.collection("pac_maze_ikun_disclosure");

  let existing = null;
  try {
    const r = await col.doc(DOC_ID).get();
    existing = r.data || null;
  } catch (e) {
    try {
      await db.createCollection("pac_maze_ikun_disclosure");
      console.log("已创建集合 pac_maze_ikun_disclosure");
    } catch (createErr) {
      const msg = String(createErr && (createErr.message || createErr.code) || createErr);
      if (!/exist/i.test(msg)) throw createErr;
    }
  }

  if (existing && !force) {
    console.log(`文档已存在 (version=${existing.version})，跳过。使用 --force 覆盖。`);
    return;
  }

  await col.doc(DOC_ID).set(DEFAULT);
  console.log(`已写入 ${DOC_ID}，version=${DEFAULT.version}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
