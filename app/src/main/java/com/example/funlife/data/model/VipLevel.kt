// VipLevel.kt - VIP等级枚举
package com.example.funlife.data.model

/**
 * VIP 详细权益项（用于「完整权益弹窗」展示）
 *   emoji: 标识图标
 *   title: 标题（粗体）
 *   desc : 详细说明（小字辅助）
 */
data class VipBenefit(val emoji: String, val title: String, val desc: String)

/**
 * VIP 等级（三档体系：月卡 / 年卡 / 永久）
 *
 * 注：VIP3 与 PERMANENT 在新体系下视为同一档「永久会员」，数值文案完全一致。
 *     PERMANENT(level=99) 仅作为兼容旧用户数据存档（极少数历史数据用了 99）。
 *     新售卖只走 VIP_LIFETIME → vipLevel=3。
 *
 * dailyCoins 是默认硬编码值，运行时可被云端 vip_config 覆盖（VipRuntimeConfig）。
 *
 * benefits     —— 卡片正面显示（精炼诱人，前 3 条最关键，最长 4 条）
 * fullBenefits —— 「查看完整权益」弹窗展示（详细 + 情感化文案）
 */
enum class VipLevel(
    val level: Int,
    val displayName: String,
    val icon: String,
    val color: Long,
    val inventoryLimit: Int,  // 0表示无限
    val minShopPrice: Int,     // 商品最低价格（金币）
    val dailyCoins: Int,       // 默认值，可被云端配置覆盖
    val benefits: List<String>,
    val fullBenefits: List<VipBenefit> = emptyList()
) {
    NORMAL(
        level = 0,
        displayName = "普通用户",
        icon = "👤",
        color = 0xFF9E9E9E,
        inventoryLimit = 100,
        minShopPrice = 100,
        dailyCoins = 10,
        benefits = listOf(
            "背包容量：100个",
            "商品价格：100金币起",
            "每日领取：10金币"
        )
    ),
    VIP1(
        level = 1,
        displayName = "月卡 VIP",
        icon = "⭐",
        color = 0xFFFFD700,
        inventoryLimit = 300,
        minShopPrice = 30,
        dailyCoins = 30,
        // 卡片正面（前 3 条最诱人）
        benefits = listOf(
            "💼 背包扩容 3 倍 (300 个)",
            "💰 每日领 30 金币 · 月省 900",
            "🎁 激活立送 50 金币"
        ),
        // 弹窗完整权益（情感化文案 + 数字对比）
        fullBenefits = listOf(
            VipBenefit("🎁", "激活大礼包", "首次激活立送 50 金币，等同 1.5 天的领取量"),
            VipBenefit("💰", "每日金币 ×3", "普通用户 10 金/天 → 月卡 30 金/天，月省 600 金币"),
            VipBenefit("💼", "背包容量 ×3", "100 个 → 300 个，再也不用纠结收纳"),
            VipBenefit("🛒", "商城更便宜", "商品起售价 100 → 30 金币，省钱看得见"),
            VipBenefit("⭐", "月卡专属标识", "首页头像旁佩戴 ⭐ 金色星辰徽章"),
            VipBenefit("📅", "30 天畅享", "一杯奶茶钱，全月解锁完整体验"),
            VipBenefit("🚀", "随时升级抵扣", "升级年卡/终身可抵扣已付金额（开发中）")
        )
    ),
    VIP2(
        level = 2,
        displayName = "年卡 VIP",
        icon = "💎",
        color = 0xFF00BCD4,
        inventoryLimit = 2000,
        minShopPrice = 10,
        dailyCoins = 80,
        benefits = listOf(
            "💼 背包扩容 20 倍 (2000 个)",
            "💰 每日 80 金 · 全年豪领 29200",
            "🤖 AI 好友 200 条/天"
        ),
        fullBenefits = listOf(
            VipBenefit("🎁", "激活壕送 300 金", "首次激活立送 300 金币，相当于 4 天免费畅领"),
            VipBenefit("💰", "每日金币 ×8", "普通 10 金 → 年卡 80 金，全年豪领 29200 金币"),
            VipBenefit("💼", "背包容量 ×20", "2000 个收纳空间，物品再多也游刃有余"),
            VipBenefit("🛒", "商城价 1/10", "商品起售价 100 → 10 金币，购物如同白送"),
            VipBenefit("🤖", "AI 好友放开聊", "每天 200 条 AI 对话额度（普通用户仅 20 条）"),
            VipBenefit("💎", "年卡冰蓝徽章", "首页佩戴 💎 冰晶蓝钻，闪烁动效更亮眼"),
            VipBenefit("📅", "整整 365 天", "平均每天不到 0.3 元，体验完整一年"),
            VipBenefit("🚀", "升级终身抵扣", "随时升级终身 VIP 可全额抵扣（开发中）")
        )
    ),
    VIP3(
        level = 3,
        displayName = "终身 VIP",
        icon = "👑",
        color = 0xFFAB47BC,
        inventoryLimit = 0,  // 无限
        minShopPrice = 1,
        dailyCoins = 200,
        benefits = listOf(
            "♾️ 背包无限 · 每日 200 金",
            "🤖 AI 好友无限对话",
            "👑 一次买断 · 后续功能永久免费"
        ),
        fullBenefits = listOf(
            VipBenefit("🎁", "激活壕送 1000 金", "首次激活立送 1000 金币，土豪开场"),
            VipBenefit("💰", "每日金币 ×20", "普通 10 金 → 终身 200 金，一年攒 73000 金"),
            VipBenefit("♾️", "背包容量无限", "想收多少收多少，仓鼠党的终极归宿"),
            VipBenefit("🛒", "商品 1 金起", "几乎免费扫货，限定款随便拿"),
            VipBenefit("🤖", "AI 无限畅聊", "AI 好友每天不限次数对话，深夜陪伴"),
            VipBenefit("🆕", "新功能永久免费", "后续所有新功能首发，永久 0 元解锁"),
            VipBenefit("👑", "紫金王冠标识", "💎 + 👑 双徽章，全站最高级身份"),
            VipBenefit("♾️", "永久有效不限时", "一次买断，账号在权益就在"),
            VipBenefit("⚡", "优先级特权", "未来新功能内测、专属客服通道")
        )
    ),
    PERMANENT(
        level = 99,
        displayName = "终身 VIP",
        icon = "👑",
        color = 0xFFAB47BC,
        inventoryLimit = 0,
        minShopPrice = 1,
        dailyCoins = 200,
        benefits = listOf(
            "♾️ 背包无限 · 每日 200 金",
            "🤖 AI 好友无限对话",
            "👑 一次买断 · 后续功能永久免费"
        ),
        fullBenefits = listOf(
            VipBenefit("🎁", "激活壕送 1000 金", "首次激活立送 1000 金币，土豪开场"),
            VipBenefit("💰", "每日金币 ×20", "普通 10 金 → 终身 200 金，一年攒 73000 金"),
            VipBenefit("♾️", "背包容量无限", "想收多少收多少，仓鼠党的终极归宿"),
            VipBenefit("🛒", "商品 1 金起", "几乎免费扫货，限定款随便拿"),
            VipBenefit("🤖", "AI 无限畅聊", "AI 好友每天不限次数对话，深夜陪伴"),
            VipBenefit("🆕", "新功能永久免费", "后续所有新功能首发，永久 0 元解锁"),
            VipBenefit("👑", "紫金王冠标识", "💎 + 👑 双徽章，全站最高级身份"),
            VipBenefit("♾️", "永久有效不限时", "一次买断，账号在权益就在"),
            VipBenefit("⚡", "优先级特权", "未来新功能内测、专属客服通道")
        )
    );

    companion object {
        fun fromLevel(level: Int): VipLevel {
            return values().find { it.level == level } ?: NORMAL
        }
    }
}
