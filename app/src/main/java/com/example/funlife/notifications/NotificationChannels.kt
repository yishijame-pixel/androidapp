// ═════════════════════════════════════════════════════════════════════════
// NotificationChannels.kt
// 企业级通知中心 — 通知渠道元数据（统一管理 channel id / 名称 / 等级）
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * 应用所有通知渠道的统一定义。每个 Channel 代表一类业务推送，用户可在
 * 系统通知设置 / 应用内"通知设置"中独立开关。
 */
enum class FunChannel(
    val id: String,
    val displayName: String,
    val description: String,
    val systemImportance: Int,
    /** 应用内偏好默认是否启用 */
    val defaultEnabled: Boolean = true,
    /** 是否参与"夜间静默时段"过滤（强提醒类如纪念日仍需响） */
    val respectQuietHours: Boolean = true,
    /** 应用内文字标签 */
    val emoji: String = "🔔",
    /** 🔴 是否在桌面图标上显示小红点（notification dot）。
     *  仅用户主动设的强提醒（纪念日/倒数日）为 true，其余为 false，
     *  避免每日摘要/系统提醒污染 launcher 图标 */
    val showBadge: Boolean = false
) {
    ANNIVERSARY(
        id = "fun_anniversary",
        displayName = "纪念日提醒",
        description = "纪念日到日时的强提醒（震动 + 铃声）",
        systemImportance = NotificationManager.IMPORTANCE_HIGH,
        respectQuietHours = false,
        emoji = "🎀",
        showBadge = true
    ),
    COUNTDOWN(
        id = "fun_countdown",
        displayName = "倒数日提醒",
        description = "你为某个倒数日设置的到点提醒",
        systemImportance = NotificationManager.IMPORTANCE_HIGH,
        emoji = "⏳",
        showBadge = true
    ),
    GOAL(
        id = "fun_goal",
        displayName = "目标进度",
        description = "每日目标进度推送、里程碑解锁与到期提醒",
        systemImportance = NotificationManager.IMPORTANCE_DEFAULT,
        emoji = "🎯"
    ),
    HABIT(
        id = "fun_habit",
        displayName = "习惯打卡",
        description = "未打卡习惯的每日提醒",
        systemImportance = NotificationManager.IMPORTANCE_DEFAULT,
        emoji = "✅"
    ),
    MOOD(
        id = "fun_mood",
        displayName = "心情邮箱",
        description = "每日心情寄语 / 心情驿站推送",
        systemImportance = NotificationManager.IMPORTANCE_DEFAULT,
        emoji = "💌"
    ),
    OPEN_APP(
        id = "fun_open_app",
        displayName = "今日摘要",
        description = "每日首次打开 App 时的今日提醒摘要",
        systemImportance = NotificationManager.IMPORTANCE_LOW,
        emoji = "🌅"
    ),
    WEEKLY(
        id = "fun_weekly",
        displayName = "每周精选",
        description = "每周日晚的本周心情/目标/习惯摘要",
        systemImportance = NotificationManager.IMPORTANCE_DEFAULT,
        emoji = "📊"
    ),
    SYSTEM(
        id = "fun_system",
        displayName = "系统通知",
        description = "积分到账、领取奖励、安全提示等系统消息",
        systemImportance = NotificationManager.IMPORTANCE_LOW,
        defaultEnabled = true,
        emoji = "⚙️"
    ),
    // 🆕 Phase 3：聊天记账 / 定期账单
    BOOKKEEPING(
        id = "fun_bookkeeping",
        displayName = "记账提醒",
        description = "定期账单到期自动入账、预算超额警示等记账类提醒",
        systemImportance = NotificationManager.IMPORTANCE_DEFAULT,
        emoji = "💰"
    ),
    // 🆕 Phase 4：时光信箱
    //   AI 替身回信送达时推送；建议保留 HIGH 重要度以制造"信件到了"的仪式感
    LETTER(
        id = "fun_letter",
        displayName = "时光信箱",
        description = "你写给过去/未来的信件，已由 AI 替身回复送达",
        systemImportance = NotificationManager.IMPORTANCE_HIGH,
        emoji = "✉️"
    ),
    SOCIAL(
        id = "fun_social",
        displayName = "好友与社交",
        description = "好友申请、验证消息等社交提醒",
        systemImportance = NotificationManager.IMPORTANCE_HIGH,
        respectQuietHours = false,
        emoji = "👥",
    );

    companion object {
        fun fromId(id: String?): FunChannel? = values().firstOrNull { it.id == id }
    }
}

object NotificationChannels {

    /** 一次性迁移版本号；提升后会重建受影响的 channel */
    private const val BADGE_MIGRATION_VERSION = 2
    private const val SOCIAL_CHANNEL_MIGRATION_VERSION = 4
    private const val PREFS = "fun_channels_meta"
    private const val KEY_BADGE_MIGRATION = "badge_migration_v"
    private const val KEY_SOCIAL_CHANNEL_MIGRATION = "social_channel_v"

    /** 在 App 启动 / Receiver 入口处调用，幂等。 */
    fun ensureAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔄 一次性 channel 重建：老用户的 channel 已被系统创建，
        //    setShowBadge() 改动不会生效；需要 delete + recreate 才能让桌面 dot 收敛
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_BADGE_MIGRATION, 0)
        if (current < BADGE_MIGRATION_VERSION) {
            FunChannel.values().forEach { ch ->
                if (!ch.showBadge) {
                    runCatching { mgr.deleteNotificationChannel(ch.id) }
                }
            }
            prefs.edit().putInt(KEY_BADGE_MIGRATION, BADGE_MIGRATION_VERSION).apply()
        }

        // 社交 channel 升级为 HIGH → 支持 heads-up 横幅通知（需删旧 channel 重建）
        val socialVer = prefs.getInt(KEY_SOCIAL_CHANNEL_MIGRATION, 0)
        if (socialVer < SOCIAL_CHANNEL_MIGRATION_VERSION) {
            runCatching { mgr.deleteNotificationChannel(FunChannel.SOCIAL.id) }
            runCatching { mgr.deleteNotificationChannel("fun_social") }
            prefs.edit().putInt(KEY_SOCIAL_CHANNEL_MIGRATION, SOCIAL_CHANNEL_MIGRATION_VERSION).apply()
        }

        FunChannel.values().forEach { ch ->
            ensureChannel(mgr, ch)
        }
    }

    private fun ensureChannel(mgr: NotificationManager, ch: FunChannel) {
        val existing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.getNotificationChannel(ch.id)
        } else null
        if (existing != null && !channelOutOfSync(existing, ch)) return
        runCatching { mgr.deleteNotificationChannel(ch.id) }
        createChannel(mgr, ch)
    }

    private fun channelOutOfSync(existing: NotificationChannel, ch: FunChannel): Boolean {
        if (existing.importance != ch.systemImportance) return true
        if (ch == FunChannel.SOCIAL) {
            if (!existing.shouldVibrate()) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !existing.canBypassDnd()) return true
        }
        return false
    }

    private fun createChannel(mgr: NotificationManager, ch: FunChannel) {
        val nc = NotificationChannel(ch.id, ch.displayName, ch.systemImportance).apply {
            description = ch.description
            enableLights(true)
            enableVibration(ch.systemImportance >= NotificationManager.IMPORTANCE_DEFAULT)
            setShowBadge(ch.showBadge)
            if (ch == FunChannel.SOCIAL) {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 120, 60, 120, 60, 180)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(true)
                }
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, attrs)
            }
        }
        mgr.createNotificationChannel(nc)
    }
}
