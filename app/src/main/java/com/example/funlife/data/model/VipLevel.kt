// VipLevel.kt - VIP等级枚举
package com.example.funlife.data.model

enum class VipLevel(
    val level: Int,
    val displayName: String,
    val icon: String,
    val color: Long,
    val inventoryLimit: Int,  // 0表示无限
    val minShopPrice: Int,     // 商品最低价格（金币）
    val dailyCoins: Int,
    val benefits: List<String>
) {
    NORMAL(
        level = 0,
        displayName = "普通用户",
        icon = "👤",
        color = 0xFF9E9E9E,
        inventoryLimit = 100,
        minShopPrice = 100,
        dailyCoins = 0,
        benefits = listOf(
            "背包容量：100个",
            "商品价格：100金币起",
            "无每日金币奖励"
        )
    ),
    VIP1(
        level = 1,
        displayName = "普通VIP",
        icon = "⭐",
        color = 0xFFFFD700,
        inventoryLimit = 100,
        minShopPrice = 50,
        dailyCoins = 20,
        benefits = listOf(
            "背包容量：100个",
            "商品价格：50金币起",
            "每日领取：20金币",
            "永久有效"
        )
    ),
    VIP2(
        level = 2,
        displayName = "年费VIP",
        icon = "💎",
        color = 0xFF00BCD4,
        inventoryLimit = 1000,
        minShopPrice = 20,
        dailyCoins = 50,
        benefits = listOf(
            "背包容量：1000个",
            "商品价格：20金币起",
            "每日领取：50金币",
            "年费有效"
        )
    ),
    VIP3(
        level = 3,
        displayName = "终生VIP",
        icon = "👑",
        color = 0xFFFF6B9D,
        inventoryLimit = 0,  // 无限
        minShopPrice = 1,
        dailyCoins = 100,
        benefits = listOf(
            "背包容量：无限",
            "商品价格：1金币起",
            "每日领取：100金币",
            "后续功能优先使用权",
            "专属至尊标识"
        )
    ),
    PERMANENT(
        level = 99,
        displayName = "永久VIP",
        icon = "🌟",
        color = 0xFFFF4081,
        inventoryLimit = 0,  // 无限
        minShopPrice = 1,
        dailyCoins = 100,
        benefits = listOf(
            "背包容量：无限",
            "商品价格：1金币起",
            "每日领取：100金币",
            "永久有效",
            "所有VIP特权",
            "专属永久标识"
        )
    );

    companion object {
        fun fromLevel(level: Int): VipLevel {
            return values().find { it.level == level } ?: NORMAL
        }
    }
}
