// ═════════════════════════════════════════════════════════════════════════
// OpenAppNotifier.kt
// 企业级通知中心 — 每日首次打开 App 推送一条"今日摘要"通知
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 在 App 进入前台时调用一次。如当天首次打开（按 userId + 日期判断），
 * 则推送一条今日摘要通知 —— 由 NotificationCenter 统一闸门控制。
 */
object OpenAppNotifier {

    private const val TAG = "OpenAppNotifier"

    fun trigger(context: Context) {
        try {
            if (!NotificationPrefs.isOpenAppEnabled(context)) return
            val today = LocalDate.now().toString()
            val last = NotificationPrefs.getOpenAppLastDate(context)
            if (last == today) return // 今天已发过

            val app = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = runCatching { UserSessionManager(app).getCurrentUserId() }.getOrDefault(0L)
                    if (userId <= 0L) return@launch

                    val db = AppDatabase.getDatabase(app)
                    val goals = runCatching { db.goalDao().getActiveGoals(userId).first() }
                        .getOrDefault(emptyList())
                    val countdowns = runCatching { db.goalDao().getAllCountdowns(userId).first() }
                        .getOrDefault(emptyList())
                    val habits = runCatching { db.habitDao().getAllActiveHabits(userId).first() }
                        .getOrDefault(emptyList())

                    val today2 = LocalDate.now()
                    val dueToday = countdowns.count {
                        runCatching { LocalDate.parse(it.targetDate) == today2 }.getOrDefault(false)
                    }
                    val dueSoon = countdowns.count {
                        runCatching {
                            val d = LocalDate.parse(it.targetDate)
                            val days = java.time.temporal.ChronoUnit.DAYS.between(today2, d)
                            days in 0..3L
                        }.getOrDefault(false)
                    }

                    if (goals.isEmpty() && habits.isEmpty() && countdowns.isEmpty()) {
                        // 数据为空就不打扰
                        NotificationPrefs.setOpenAppLastDate(app, today)
                        return@launch
                    }

                    val parts = mutableListOf<String>()
                    if (goals.isNotEmpty()) parts.add("🎯 ${goals.size} 个目标")
                    if (habits.isNotEmpty()) parts.add("✅ ${habits.size} 个习惯")
                    if (dueToday > 0) parts.add("⏳ $dueToday 个倒数日今日到期")
                    else if (dueSoon > 0) parts.add("⏳ $dueSoon 个倒数日临近")

                    val title = "早呀，今天的安排来啦"
                    val body = buildString {
                        append(parts.joinToString(" · "))
                        append("\n点击查看，开启美好的一天 ✨")
                    }
                    NotificationCenter.notify(
                        app,
                        NotificationSpec(
                            channel = FunChannel.OPEN_APP,
                            id = 7710,
                            title = title,
                            body = body,
                            deepLinkRoute = "home",
                            dedupWindowMs = 20L * 60 * 60 * 1000 // 20h 内只推一次
                        )
                    )
                    NotificationPrefs.setOpenAppLastDate(app, today)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "trigger failed", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "trigger outer failed", e)
        }
    }
}
