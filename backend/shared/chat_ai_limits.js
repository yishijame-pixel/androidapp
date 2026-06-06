// 聊天记账 · AI 额度表 v2（单一事实源，与 docs/chat_ai_entitlement_model_v2.md 一致）
// 修改后运行 node sync-sku.js

/** v2 档位 → 日/月上限；tier 1 为体验卡总量池 */
const LIMIT_V2 = {
  0: { daily: 0, monthly: 0 },
  1: { daily: 0, monthly: 0, trialTotal: 5 },
  2: { daily: 30, monthly: 600 },
  3: { daily: 80, monthly: 1500 },
  4: { daily: 150, monthly: 3500 },
};

/** v1 已激活卡密就高过渡（entitlementSchema !== "v2"） */
const LEGACY_V1_LIMITS = {
  CHAT_AI_BASIC: { daily: 80, monthly: 0 },
  CHAT_AI_PLUS: { daily: 200, monthly: 0 },
  CHAT_AI_PRO: { daily: 150, monthly: 3500 },
  CHAT_AI_PRO_YEAR: { daily: 150, monthly: 3500 },
};

const TRIAL_SKU = "CHAT_AI_TRIAL";

function vipLevelToChatTier(vipLevel) {
  const v = Number(vipLevel) || 0;
  if (v >= 99) return 4;
  if (v === 3) return 4;
  if (v === 2) return 3;
  if (v === 1) return 2;
  return 0;
}

function limitsForTier(tier) {
  const t = Number(tier) || 0;
  const row = LIMIT_V2[t] || LIMIT_V2[0];
  return {
    tier: t,
    daily: row.daily || 0,
    monthly: row.monthly || 0,
    isTrial: t === 1,
    trialTotal: row.trialTotal || 0,
  };
}

function limitsFromChatAiCodeDoc(doc, sku) {
  if (!doc || !sku || sku.type !== "chat_ai") return null;
  const skuCode = doc.skuCode || sku.name;
  if (skuCode === TRIAL_SKU || sku.chatAiTier === 1 && sku.trialPool) {
    return limitsForTier(1);
  }
  if (doc.entitlementSchema === "v2") {
    return limitsForTier(sku.chatAiTier || doc.chatAiTier || 0);
  }
  const leg = LEGACY_V1_LIMITS[skuCode];
  if (leg) {
    return {
      tier: sku.chatAiTier || doc.chatAiTier || 0,
      daily: leg.daily,
      monthly: leg.monthly || 0,
      isTrial: false,
      trialTotal: 0,
      legacy: true,
    };
  }
  return limitsForTier(sku.chatAiTier || doc.chatAiTier || 0);
}

function limitsFromVipSku(sku, doc) {
  if (!sku || sku.type !== "vip") return null;
  const vipLevel = sku.vipLevel || doc.vipLevel || 0;
  const tier = vipLevelToChatTier(vipLevel);
  return { ...limitsForTier(tier), source: "VIP" };
}

/** 多来源取日额度最高者（付费优先；无付费时用体验池） */
function pickBestEntitlement(candidates) {
  let best = {
    tier: 0, daily: 0, monthly: 0, isTrial: false, trialTotal: 0, source: "NONE",
  };
  let trialCandidate = null;
  for (const c of candidates) {
    if (!c) continue;
    if (c.isTrial && c.trialTotal > 0) {
      trialCandidate = { ...c, source: c.source || "TRIAL" };
      continue;
    }
    if (c.daily > best.daily || (c.daily === best.daily && (c.monthly || 0) > (best.monthly || 0))) {
      best = { ...c, source: c.source || best.source };
    }
  }
  if (best.daily > 0 || best.monthly > 0) return best;
  if (trialCandidate) return trialCandidate;
  return best;
}

function ymKey(d) {
  const dt = d || new Date();
  const local = new Date(dt.getTime() + 8 * 3600 * 1000);
  return local.getUTCFullYear() * 100 + (local.getUTCMonth() + 1);
}

module.exports = {
  LIMIT_V2,
  LEGACY_V1_LIMITS,
  TRIAL_SKU,
  vipLevelToChatTier,
  limitsForTier,
  limitsFromChatAiCodeDoc,
  limitsFromVipSku,
  pickBestEntitlement,
  ymKey,
};
