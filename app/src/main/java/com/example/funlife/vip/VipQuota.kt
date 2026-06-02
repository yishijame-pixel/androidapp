package com.example.funlife.vip

import com.example.funlife.data.model.VipLevel
import java.util.concurrent.TimeUnit

/**
 * 🔒 VIP 配额单一事实源（Single Source of Truth）
 *
 * 所有与"按 VIP 等级限速 / 限量"的功能必须从这里读取，禁止在业务代码里再硬编码数字。
 * 修改入口集中在此文件，便于运营调整、灰度、未来从云端 vip_config 覆盖。
 *
 * 设计约束：
 *  - 所有方法都接受 `vipLevel: Int`（沿用现有 DB / Cert 字段类型），内部映射 [VipLevel]
 *  - PERMANENT (level=99) 与 VIP3 行为完全一致
 *  - UNLIMITED = -1（与历史 LetterRepository 约定保持一致）
 */
object VipQuota {

    const val UNLIMITED = -1

    /* =====================  聊天记账 · AI 对话日额度  ===================== */

    /** 聊天记账每日 AI 对话回复条数（账单回复 + 闲聊回复合并计数；账单识别 detectBill 不计） */
    fun chatAiDailyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 20
        VipLevel.VIP1      -> 80
        VipLevel.VIP2      -> 200
        VipLevel.VIP3,
        VipLevel.PERMANENT -> UNLIMITED
    }

    /* =====================  时光信箱 · 月度寄信额度  ===================== */

    /** 时光信箱每月可发出信件数 */
    fun letterMonthlyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 1
        VipLevel.VIP1      -> 5
        VipLevel.VIP2      -> 30
        VipLevel.VIP3,
        VipLevel.PERMANENT -> UNLIMITED
    }

    /** 时光信箱最短投递延迟（毫秒）—— 等级越高越自由 */
    fun letterMinDelayMs(vipLevel: Int): Long = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> TimeUnit.DAYS.toMillis(3)
        VipLevel.VIP1      -> TimeUnit.DAYS.toMillis(1)
        VipLevel.VIP2      -> TimeUnit.HOURS.toMillis(1)
        VipLevel.VIP3,
        VipLevel.PERMANENT -> 0L
    }

    /* =====================  展示辅助  ===================== */

    /** "5 / 30 封" 或 "5 / 无限"（仅展示用） */
    fun formatLetterUsage(used: Int, vipLevel: Int): String {
        val q = letterMonthlyLimit(vipLevel)
        return if (q == UNLIMITED) "$used / 无限" else "$used / $q"
    }

    /** 最短延迟人类可读 */
    fun formatMinDelay(vipLevel: Int): String {
        val ms = letterMinDelayMs(vipLevel)
        return when {
            ms <= 0L                          -> "可立即送达"
            ms < TimeUnit.HOURS.toMillis(24)  -> "≥ ${TimeUnit.MILLISECONDS.toHours(ms)} 小时"
            else                              -> "≥ ${TimeUnit.MILLISECONDS.toDays(ms)} 天"
        }
    }

    /** "20 条 / 天" 或 "无限" */
    fun formatChatLimit(vipLevel: Int): String {
        val q = chatAiDailyLimit(vipLevel)
        return if (q == UNLIMITED) "无限对话" else "$q 条 / 天"
    }

    /** 下一档升级提示文案（用于弹窗 CTA） */
    fun nextTierTeaser(vipLevel: Int): String? = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL -> "升级月卡 → 每月寄 5 封 · AI 80 条/天"
        VipLevel.VIP1   -> "升级年卡 → 每月寄 30 封 · 投递最快 1 小时 · AI 200 条/天"
        VipLevel.VIP2   -> "升级终身 → 全部无限制 · 一次买断"
        else            -> null
    }

    /* =====================  📖 阅光书房（v53）  ===================== */

    /** 摘抄时光胶囊每月可投递条数 */
    fun readingCapsuleMonthlyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 1
        VipLevel.VIP1      -> 5
        VipLevel.VIP2      -> 20
        VipLevel.VIP3,
        VipLevel.PERMANENT -> UNLIMITED
    }

    /** AI 读书伴侣每日单次问答额度（与聊天记账日额度独立） */
    fun aiBookChatDailyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 1
        VipLevel.VIP1      -> 5
        VipLevel.VIP2      -> 20
        VipLevel.VIP3,
        VipLevel.PERMANENT -> UNLIMITED
    }

    /** 是否解锁 AI 读书伴侣 >3 轮多轮深聊（VIP3 专享） */
    fun aiBookDeepChatUnlocked(vipLevel: Int): Boolean = when (mapLevel(vipLevel)) {
        VipLevel.VIP3,
        VipLevel.PERMANENT -> true
        else               -> false
    }

    /** 是否允许向匿名摘抄星河发布（VIP1+） */
    fun galaxyPublishUnlocked(vipLevel: Int): Boolean = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL -> false
        else            -> true
    }

    /** 明信片漂流每月可寄出张数（VIP2 起） */
    fun postcardDriftMonthlyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL,
        VipLevel.VIP1      -> 0
        VipLevel.VIP2      -> 1
        VipLevel.VIP3,
        VipLevel.PERMANENT -> 4
    }

    /** 读者 DNA 画像生成最短间隔（天）；越大越久才能再生成一次 */
    fun readerDnaCooldownDays(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 365
        VipLevel.VIP1      -> 90
        VipLevel.VIP2      -> 30
        VipLevel.VIP3,
        VipLevel.PERMANENT -> 0
    }

    /** 晨光信使每周可推送次数（≥7 视为每天） */
    fun heraldWeeklyLimit(vipLevel: Int): Int = when (mapLevel(vipLevel)) {
        VipLevel.NORMAL    -> 2
        VipLevel.VIP1,
        VipLevel.VIP2,
        VipLevel.VIP3,
        VipLevel.PERMANENT -> 7
    }

    /** "5 / 20 条 / 月" 格式 */
    fun formatCapsuleUsage(used: Int, vipLevel: Int): String {
        val q = readingCapsuleMonthlyLimit(vipLevel)
        return if (q == UNLIMITED) "$used / 无限" else "$used / $q"
    }

    /** "5 / 20 条 / 天" 格式 */
    fun formatBookChatUsage(used: Int, vipLevel: Int): String {
        val q = aiBookChatDailyLimit(vipLevel)
        return if (q == UNLIMITED) "$used / 无限" else "$used / $q"
    }

    private fun mapLevel(level: Int): VipLevel = VipLevel.fromLevel(level)
}
