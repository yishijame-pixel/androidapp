package com.example.funlife.vip

/**
 * P4 · Google Play 内购对接预留（当前未接入 BillingClient）。
 *
 * 正式接入时：
 * 1. 在 Play Console 创建与 [retailTiers] skuCode 对应的商品 ID
 * 2. 购买成功后由服务端 webhook 或客户端持收据调专用激活接口
 * 3. 勿在客户端本地直接写 VIP / AI 凭证，仍走 /redeem 或专用 verify
 */
object ChatAiBilling {

    const val BILLING_ENABLED = false

    /** Play 商品 ID 映射（示例，上架前在 Console 配置） */
    fun playProductIdForSku(skuCode: String): String? = when (skuCode.uppercase()) {
        "CHAT_AI_BASIC" -> "chat_ai_basic_monthly"
        "CHAT_AI_PLUS" -> "chat_ai_plus_monthly"
        "CHAT_AI_PRO" -> "chat_ai_pro_monthly"
        "CHAT_AI_PRO_YEAR" -> "chat_ai_pro_yearly"
        else -> null
    }

    fun purchaseUnavailableHint(): String =
        "应用内购买即将上线，请先在「AI 额度」输入卡密激活，或联系客服购买。"
}
