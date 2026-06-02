// ═════════════════════════════════════════════════════════════════════════
// NotificationPrefs.kt
// 企业级通知中心 — 用户偏好（多用户隔离）
// 包含：总开关 / 每渠道开关 / 静默时段 / 24h 去重时间戳 / 每日推送时刻
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context
import android.content.SharedPreferences
import com.example.funlife.utils.UserSessionManager

/**
 * 通知偏好持久化（SharedPreferences）。所有 key 以 userId 隔离，
 * 不同账号互不干扰。未登录(0L)时使用 "guest"。
 */
object NotificationPrefs {

    private const val FILE = "fun_notif_prefs"

    // ── keys（拼接 userId）──
    private const val K_GLOBAL = "global_enabled"
    private const val K_CHANNEL_PREFIX = "ch_enabled_"
    private const val K_QUIET_ENABLED = "quiet_enabled"
    private const val K_QUIET_FROM = "quiet_from_min" // 自 0 时起的分钟数
    private const val K_QUIET_TO = "quiet_to_min"
    private const val K_LAST_FIRED_PREFIX = "last_fired_" // channel id → epochMs
    private const val K_DAILY_GOAL_HOUR = "daily_goal_hour"
    private const val K_DAILY_GOAL_MIN = "daily_goal_min"
    private const val K_DAILY_HABIT_HOUR = "daily_habit_hour"
    private const val K_DAILY_HABIT_MIN = "daily_habit_min"
    private const val K_DAILY_MOOD_HOUR = "daily_mood_hour"
    private const val K_DAILY_MOOD_MIN = "daily_mood_min"
    private const val K_OPEN_APP_LAST_DATE = "open_app_last"
    private const val K_OPEN_APP_ENABLED = "open_app_enabled"
    private const val K_DEDUP_ENABLED = "dedup_enabled"
    private const val K_ENGAGEMENT_ENABLED = "engagement_enabled"

    // ── 默认值 ──
    private const val DEFAULT_QUIET_FROM_MIN = 22 * 60 // 22:00
    private const val DEFAULT_QUIET_TO_MIN = 8 * 60    // 08:00
    const val DEFAULT_GOAL_HOUR = 20
    const val DEFAULT_GOAL_MIN = 0
    const val DEFAULT_HABIT_HOUR = 21
    const val DEFAULT_HABIT_MIN = 0
    const val DEFAULT_MOOD_HOUR = 9
    const val DEFAULT_MOOD_MIN = 30
    const val WEEKLY_HOUR = 20 // 周日 20:00
    const val WEEKLY_MIN = 0

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun uidTag(ctx: Context): String {
        val uid = runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
        return if (uid > 0L) uid.toString() else "guest"
    }

    private fun key(ctx: Context, base: String): String = "${base}_${uidTag(ctx)}"

    // ── 总开关 ──
    fun isGlobalEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_GLOBAL), true)

    fun setGlobalEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_GLOBAL), enabled).apply()
    }

    // ── 单渠道开关 ──
    fun isChannelEnabled(ctx: Context, ch: FunChannel): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_CHANNEL_PREFIX + ch.id), ch.defaultEnabled)

    fun setChannelEnabled(ctx: Context, ch: FunChannel, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_CHANNEL_PREFIX + ch.id), enabled).apply()
    }

    // ── 静默时段 ──
    fun isQuietHoursEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_QUIET_ENABLED), true)

    fun setQuietHoursEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_QUIET_ENABLED), enabled).apply()
    }

    fun getQuietFromMin(ctx: Context): Int =
        prefs(ctx).getInt(key(ctx, K_QUIET_FROM), DEFAULT_QUIET_FROM_MIN)

    fun getQuietToMin(ctx: Context): Int =
        prefs(ctx).getInt(key(ctx, K_QUIET_TO), DEFAULT_QUIET_TO_MIN)

    fun setQuietRange(ctx: Context, fromMin: Int, toMin: Int) {
        prefs(ctx).edit()
            .putInt(key(ctx, K_QUIET_FROM), fromMin.coerceIn(0, 24 * 60 - 1))
            .putInt(key(ctx, K_QUIET_TO), toMin.coerceIn(0, 24 * 60 - 1))
            .apply()
    }

    /** 当前时间是否在静默时段（支持跨午夜，例如 22:00 → 08:00） */
    fun nowInQuietHours(ctx: Context): Boolean {
        if (!isQuietHoursEnabled(ctx)) return false
        val cal = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val from = getQuietFromMin(ctx)
        val to = getQuietToMin(ctx)
        return if (from == to) false
        else if (from < to) nowMin in from until to
        else nowMin >= from || nowMin < to
    }

    // ── 24h 去重 ──
    fun isDedupEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_DEDUP_ENABLED), true)

    fun setDedupEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_DEDUP_ENABLED), enabled).apply()
    }

    fun getLastFired(ctx: Context, ch: FunChannel): Long =
        prefs(ctx).getLong(key(ctx, K_LAST_FIRED_PREFIX + ch.id), 0L)

    fun markFired(ctx: Context, ch: FunChannel, atMs: Long = System.currentTimeMillis()) {
        prefs(ctx).edit().putLong(key(ctx, K_LAST_FIRED_PREFIX + ch.id), atMs).apply()
    }

    fun firedWithin(ctx: Context, ch: FunChannel, windowMs: Long): Boolean {
        if (!isDedupEnabled(ctx)) return false
        val last = getLastFired(ctx, ch)
        return last > 0 && System.currentTimeMillis() - last < windowMs
    }

    // ── 每日推送时刻 ──
    fun getGoalTime(ctx: Context): Pair<Int, Int> = Pair(
        prefs(ctx).getInt(key(ctx, K_DAILY_GOAL_HOUR), DEFAULT_GOAL_HOUR),
        prefs(ctx).getInt(key(ctx, K_DAILY_GOAL_MIN), DEFAULT_GOAL_MIN)
    )

    fun setGoalTime(ctx: Context, h: Int, m: Int) {
        prefs(ctx).edit()
            .putInt(key(ctx, K_DAILY_GOAL_HOUR), h.coerceIn(0, 23))
            .putInt(key(ctx, K_DAILY_GOAL_MIN), m.coerceIn(0, 59))
            .apply()
    }

    fun getHabitTime(ctx: Context): Pair<Int, Int> = Pair(
        prefs(ctx).getInt(key(ctx, K_DAILY_HABIT_HOUR), DEFAULT_HABIT_HOUR),
        prefs(ctx).getInt(key(ctx, K_DAILY_HABIT_MIN), DEFAULT_HABIT_MIN)
    )

    fun setHabitTime(ctx: Context, h: Int, m: Int) {
        prefs(ctx).edit()
            .putInt(key(ctx, K_DAILY_HABIT_HOUR), h.coerceIn(0, 23))
            .putInt(key(ctx, K_DAILY_HABIT_MIN), m.coerceIn(0, 59))
            .apply()
    }

    fun getMoodTime(ctx: Context): Pair<Int, Int> = Pair(
        prefs(ctx).getInt(key(ctx, K_DAILY_MOOD_HOUR), DEFAULT_MOOD_HOUR),
        prefs(ctx).getInt(key(ctx, K_DAILY_MOOD_MIN), DEFAULT_MOOD_MIN)
    )

    fun setMoodTime(ctx: Context, h: Int, m: Int) {
        prefs(ctx).edit()
            .putInt(key(ctx, K_DAILY_MOOD_HOUR), h.coerceIn(0, 23))
            .putInt(key(ctx, K_DAILY_MOOD_MIN), m.coerceIn(0, 59))
            .apply()
    }

    // ── 每日打开 App 摘要 ──
    fun isOpenAppEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_OPEN_APP_ENABLED), true)

    fun setOpenAppEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_OPEN_APP_ENABLED), enabled).apply()
    }

    fun getOpenAppLastDate(ctx: Context): String? =
        prefs(ctx).getString(key(ctx, K_OPEN_APP_LAST_DATE), null)

    fun setOpenAppLastDate(ctx: Context, date: String) {
        prefs(ctx).edit().putString(key(ctx, K_OPEN_APP_LAST_DATE), date).apply()
    }

    // ── 拉活通知（空模块推送）独立开关，默认开启 ──
    fun isEngagementEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(key(ctx, K_ENGAGEMENT_ENABLED), true)

    fun setEngagementEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(key(ctx, K_ENGAGEMENT_ENABLED), enabled).apply()
    }
}
