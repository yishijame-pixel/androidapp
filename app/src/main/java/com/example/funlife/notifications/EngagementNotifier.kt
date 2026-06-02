// ═════════════════════════════════════════════════════════════════════════
// EngagementNotifier.kt
// 拉活通知中心：扫描各模块（心情/纪念日/目标/习惯/倒数日）的空状态，
//   对空模块推送鼓励性通知（系统通知 + 收件箱），引导用户回流。
//
// 节流规则：每个模块每 24 小时最多推一次；当模块有数据后不再推。
// 调用入口：MainActivity onCreate 末尾 + 可由 Worker 周期触发。
// 严格遵循开发原则：异步 IO / 不抛异常 / 多用户隔离 / 去重 / 渠道开关尊重。
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object EngagementNotifier {
    private const val TAG = "EngagementNotifier"
    private const val PREF = "fun_engagement"
    private const val THROTTLE_MS = 24L * 60L * 60L * 1000L  // 每模块 24h 一次

    /** 异步触发一次扫描；安全可重复调用，本身节流 */
    fun triggerAsync(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try { runOnce(ctx) } catch (e: Throwable) {
                android.util.Log.e(TAG, "engagement scan failed", e)
            }
        }
    }

    /** Worker 直接调用（已在 IO 线程） */
    suspend fun runOnce(ctx: Context) {
        val userId = runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
        if (userId <= 0L) return  // 未登录不打扰
        if (!NotificationPrefs.isEngagementEnabled(ctx)) return  // 用户关闭了拉活提醒
        val db = AppDatabase.getDatabase(ctx)
        val prefs = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        // 每个 entry：(模块 key, 是否为空 lambda, 通知 spec 构造)
        val tasks = listOf(
            Module(
                key = "mood",
                isEmpty = { db.moodDao().getAllMoodEntries(userId).first().isEmpty() },
                channel = FunChannel.MOOD,
                title = "今天的心情还没记 💭",
                body = "一句话、一个 emoji 也可以。点进来记一下今天的心情吧～",
                deepLink = "mood",
                notifyId = 81001
            ),
            Module(
                key = "anniversary",
                isEmpty = { db.anniversaryDao().getAllForUserOnce(userId).isEmpty() },
                channel = FunChannel.ANNIVERSARY,
                title = "添加一个纪念日 🎀",
                body = "生日、相识日、入职日…让重要的日子不被遗忘。",
                deepLink = "anniversary",
                notifyId = 81002
            ),
            Module(
                key = "goal",
                isEmpty = { db.goalDao().getActiveGoals(userId).first().isEmpty() },
                channel = FunChannel.GOAL,
                title = "立一个小目标 🎯",
                body = "再小的目标也是开始。一步一步，慢慢来～",
                deepLink = "goal",
                notifyId = 81003
            ),
            Module(
                key = "countdown",
                isEmpty = { db.goalDao().getAllCountdowns(userId).first().isEmpty() },
                channel = FunChannel.COUNTDOWN,
                title = "设一个倒数日 ⏳",
                body = "期待的旅行、考试、约会…让等待变得有仪式感。",
                deepLink = "goal",
                notifyId = 81004
            ),
            Module(
                key = "habit",
                isEmpty = { db.habitDao().getAllActiveHabits(userId).first().isEmpty() },
                channel = FunChannel.HABIT,
                title = "开始一个习惯吧 ✅",
                body = "喝水、阅读、运动…坚持 21 天，让生活变得更好。",
                deepLink = "main",
                notifyId = 81005
            )
        )

        val now = System.currentTimeMillis()
        for (t in tasks) {
            try {
                if (!t.isEmpty()) continue                    // 用户已有数据，跳过
                val lastKey = "${t.key}_u${userId}_last"
                val last = prefs.getLong(lastKey, 0L)
                if (now - last < THROTTLE_MS) continue        // 24h 内已推过
                val sent = NotificationCenter.notify(
                    ctx,
                    NotificationSpec(
                        id = t.notifyId,
                        channel = t.channel,
                        title = t.title,
                        body = t.body,
                        deepLinkRoute = t.deepLink,
                        emojiPrefix = false,
                        dedupWindowMs = 0L  // 自己已节流
                    )
                )
                if (sent) prefs.edit().putLong(lastKey, now).apply()
            } catch (e: Throwable) {
                android.util.Log.w(TAG, "module ${t.key} scan failed: ${e.message}")
            }
        }
    }

    private data class Module(
        val key: String,
        val isEmpty: suspend () -> Boolean,
        val channel: FunChannel,
        val title: String,
        val body: String,
        val deepLink: String?,
        val notifyId: Int
    )
}
