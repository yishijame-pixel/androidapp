package com.example.funlife.vip

/**
 * 聊天记账 · AI 额度卡 SKU 常量与工具。
 * 与 backend/shared/sku.js 中 CHAT_AI_* 保持一致。
 */
object ChatAiSku {

    private val CHAT_AI_SKUS = setOf(
        "CHAT_AI_TRIAL",
        "CHAT_AI_BASIC",
        "CHAT_AI_PLUS",
        "CHAT_AI_PRO",
        "CHAT_AI_PRO_YEAR",
    )

    fun isChatAiSku(skuCode: String, productType: String? = null): Boolean {
        if (productType == "chat_ai") return true
        if (productType == "vip") return false
        return skuCode.uppercase() in CHAT_AI_SKUS
    }

    fun isChatAiCert(cert: VipCertificate): Boolean =
        isChatAiSku(cert.skuCode, cert.productType)

    fun tierFromCert(cert: VipCertificate): Int =
        if (isChatAiCert(cert)) cert.vipLevel.coerceAtLeast(0) else 0

    fun displayName(skuCode: String): String = when (skuCode.uppercase()) {
        "CHAT_AI_TRIAL"     -> "聊天AI·体验包"
        "CHAT_AI_BASIC"     -> "聊天AI·标准月卡"
        "CHAT_AI_PLUS"      -> "聊天AI·进阶月卡"
        "CHAT_AI_PRO"       -> "聊天AI·专业月卡"
        "CHAT_AI_PRO_YEAR"  -> "聊天AI·专业年卡"
        else                -> "AI 额度包"
    }

    fun isTrialSku(skuCode: String): Boolean =
        skuCode.uppercase() == "CHAT_AI_TRIAL"

    fun isActive(cert: VipCertificate): Boolean {
        if (!isChatAiSku(cert.skuCode)) return false
        if (!cert.isValidNow()) return false
        return cert.isVipActive()
    }

    /** P2 · 弹窗「套餐与购买说明」展示用（价格与 backend/shared/sku.js 对齐） */
    val retailTiers: List<RetailTier> = listOf(
        RetailTier(
            skuCode = "CHAT_AI_BASIC",
            title = displayName("CHAT_AI_BASIC"),
            quotaLabel = ChatAiLimits.formatDailyMonthly(
                ChatAiLimits.dailyLimit(ChatAiLimits.TIER_BASIC),
                ChatAiLimits.monthlyLimit(ChatAiLimits.TIER_BASIC),
            ),
            priceLabel = "¥12.9",
            days = 30,
        ),
        RetailTier(
            skuCode = "CHAT_AI_PLUS",
            title = displayName("CHAT_AI_PLUS"),
            quotaLabel = ChatAiLimits.formatDailyMonthly(
                ChatAiLimits.dailyLimit(ChatAiLimits.TIER_PLUS),
                ChatAiLimits.monthlyLimit(ChatAiLimits.TIER_PLUS),
            ),
            priceLabel = "¥24.9",
            days = 30,
        ),
        RetailTier(
            skuCode = "CHAT_AI_PRO",
            title = displayName("CHAT_AI_PRO"),
            quotaLabel = ChatAiLimits.formatDailyMonthly(
                ChatAiLimits.dailyLimit(ChatAiLimits.TIER_PRO),
                ChatAiLimits.monthlyLimit(ChatAiLimits.TIER_PRO),
            ),
            priceLabel = "¥39.9",
            days = 30,
        ),
        RetailTier(
            skuCode = "CHAT_AI_PRO_YEAR",
            title = displayName("CHAT_AI_PRO_YEAR"),
            quotaLabel = ChatAiLimits.formatDailyMonthly(
                ChatAiLimits.dailyLimit(ChatAiLimits.TIER_PRO),
                ChatAiLimits.monthlyLimit(ChatAiLimits.TIER_PRO),
            ),
            priceLabel = "¥99",
            days = 365,
        ),
    )

    fun friendlyRedeemError(code: String?, fallback: String?): String = when (code) {
        "INVALID"       -> "卡密不存在，请核对后重试"
        "USED"          -> "该卡密已被使用"
        "USER_MISMATCH" -> "该卡密已绑定其他账号"
        "DISABLED"      -> "该卡密已被禁用"
        "TIER_TOO_LOW"  -> "你当前的 AI 额度已更高，无需更换"
        "TRIAL_ALREADY_USED" -> "体验卡每设备/账号仅可使用一次"
        "WRONG_TYPE"    -> "此为 VIP 专用卡，请前往会员页兑换"
        "RATE_LIMITED"  -> "请求过于频繁，请稍后再试"
        "NETWORK_ERROR" -> "网络异常，请稍后重试"
        "USER_REQUIRED" -> "请先登录账号后再激活"
        else            -> fallback ?: "激活失败，请稍后重试"
    }
}

/** 弹窗状态条枚举 */
enum class ChatAiBarState {
    INACTIVE, TRIAL, ACTIVE_CARD, ACTIVE_VIP, ACTIVE_BOTH,
    EXPIRED, EXHAUSTED_DAY, EXHAUSTED_MONTH,
    @Deprecated("v2 无无限档") UNLIMITED,
    @Deprecated("改用 INACTIVE") FREE,
    @Deprecated("改用 EXHAUSTED_DAY") EXHAUSTED,
}

data class RetailTier(
    val skuCode: String,
    val title: String,
    val quotaLabel: String,
    val priceLabel: String,
    val days: Int,
)

/** 聊天页 AI 额度弹窗 UI 状态 */
data class ChatAiEntitlementUi(
    val state: ChatAiBarState,
    val packageName: String?,
    val usedToday: Int,
    val dailyLimit: Int,
    val usedMonth: Int = 0,
    val monthlyLimit: Int = 0,
    val trialRemaining: Int? = null,
    val expireDate: String?,
    val progress: Float?,
    val sourceLabel: String?,
    val effectiveTier: Int,
    val hasCloudEntitlement: Boolean = false,
)
