package com.example.funlife.vip

/**
 * 聊天记账 · AI 额度 v2（与 backend/shared/chat_ai_limits.js 一致）
 */
object ChatAiLimits {

    const val TIER_INACTIVE = 0
    const val TIER_TRIAL = 1
    const val TIER_BASIC = 2
    const val TIER_PLUS = 3
    const val TIER_PRO = 4

    const val TRIAL_TOTAL = 5

    data class LimitRow(val daily: Int, val monthly: Int, val trialTotal: Int = 0)

    private val LIMIT_V2 = mapOf(
        0 to LimitRow(0, 0),
        1 to LimitRow(0, 0, TRIAL_TOTAL),
        2 to LimitRow(30, 600),
        3 to LimitRow(80, 1500),
        4 to LimitRow(150, 3500),
    )

    fun dailyLimit(tier: Int): Int = LIMIT_V2[tier]?.daily ?: 0

    fun monthlyLimit(tier: Int): Int = LIMIT_V2[tier]?.monthly ?: 0

    fun trialTotal(tier: Int): Int = if (tier == TIER_TRIAL) TRIAL_TOTAL else 0

    /** VIP vipLevel → v2 聊天档位 */
    fun vipLevelToChatTier(vipLevel: Int): Int = when {
        vipLevel >= 99 -> TIER_PRO
        vipLevel >= 3 -> TIER_PRO
        vipLevel == 2 -> TIER_PLUS
        vipLevel == 1 -> TIER_BASIC
        else -> TIER_INACTIVE
    }

    fun hasCloudEntitlement(tier: Int): Boolean =
        tier > TIER_INACTIVE

    fun formatDailyMonthly(daily: Int, monthly: Int): String =
        if (monthly > 0) "$daily 条/天 · 月 $monthly 条" else "$daily 条/天"
}
