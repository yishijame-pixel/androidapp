// ============================================================
// pac_maze_config 云函数
// ------------------------------------------------------------
// 客户端 POST /pac_maze_config 拉取豆人迷宫运行时配置。
// 当前支持：ikun 类进入前必读须知（后台 pac_maze_ikun_disclosure 集合）
// ============================================================

const tcb = require("@cloudbase/node-sdk");

const app = tcb.init({ env: tcb.SYMBOL_CURRENT_ENV });
const db = app.database();
const CFG = db.collection("pac_maze_ikun_disclosure");
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
};

exports.main = async () => {
  try {
    let doc = null;
    try {
      const r = await CFG.doc(DOC_ID).get();
      doc = r.data || null;
    } catch (e) {
      // 集合未创建 → 默认
    }
    const merged = { ...DEFAULT, ...(doc || {}) };
    return {
      ok: true,
      data: {
        version: Number(merged.version) || DEFAULT.version,
        enabled: merged.enabled !== false,
        title: String(merged.title || DEFAULT.title),
        body: String(merged.body || DEFAULT.body),
        agreeButtonText: String(merged.agreeButtonText || DEFAULT.agreeButtonText),
        footerHint: String(merged.footerHint || DEFAULT.footerHint),
        updatedAt: merged.updatedAt || null,
      },
    };
  } catch (e) {
    console.error("pac_maze_config error", e);
    return { ok: true, data: { ...DEFAULT, updatedAt: null } };
  }
};
