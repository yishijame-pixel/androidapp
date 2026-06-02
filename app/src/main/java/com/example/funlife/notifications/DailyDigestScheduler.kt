// ═════════════════════════════════════════════════════════════════════════
// DailyDigestScheduler.kt
// 企业级通知中心 — 每日定时推送调度（目标/习惯/心情/每周）
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 推送类型 — 调度时区分。
 */
enum class DailyDigestKind(
    val requestCode: Int,
    val intentKey: String,
    val channel: FunChannel
) {
    GOAL(770_001, "kind_goal", FunChannel.GOAL),
    HABIT(770_002, "kind_habit", FunChannel.HABIT),
    MOOD(770_003, "kind_mood", FunChannel.MOOD),
    WEEKLY(770_004, "kind_weekly", FunChannel.WEEKLY);
}

object DailyDigestScheduler {

    private const val TAG = "DailyDigestScheduler"

    fun rescheduleAll(context: Context) {
        DailyDigestKind.values().forEach { schedule(context, it) }
    }

    fun cancelAll(context: Context) {
        DailyDigestKind.values().forEach { cancel(context, it) }
    }

    fun schedule(context: Context, kind: DailyDigestKind) {
        try {
            val triggerMs = computeNextTriggerMs(context, kind) ?: return
            val app = context.applicationContext
            val intent = Intent(app, DailyDigestReceiver::class.java).apply {
                action = "com.example.funlife.DAILY_DIGEST_${kind.name}"
                putExtra("kind", kind.name)
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getBroadcast(app, kind.requestCode, intent, piFlags)
            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            } catch (_: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
            android.util.Log.d(TAG, "scheduled ${kind.name} @ ${java.util.Date(triggerMs)}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "schedule ${kind.name} failed", e)
        }
    }

    fun cancel(context: Context, kind: DailyDigestKind) {
        try {
            val app = context.applicationContext
            val intent = Intent(app, DailyDigestReceiver::class.java)
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getBroadcast(app, kind.requestCode, intent, piFlags)
            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
        } catch (_: Throwable) {}
    }

    private fun computeNextTriggerMs(context: Context, kind: DailyDigestKind): Long? {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val (h, m) = when (kind) {
            DailyDigestKind.GOAL -> NotificationPrefs.getGoalTime(context)
            DailyDigestKind.HABIT -> NotificationPrefs.getHabitTime(context)
            DailyDigestKind.MOOD -> NotificationPrefs.getMoodTime(context)
            DailyDigestKind.WEEKLY -> Pair(NotificationPrefs.WEEKLY_HOUR, NotificationPrefs.WEEKLY_MIN)
        }
        val time = LocalTime.of(h, m)
        val candidate = if (kind == DailyDigestKind.WEEKLY) {
            // 下一个周日 20:00
            val nextSun = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            val dt = LocalDateTime.of(nextSun, time)
            if (!dt.isAfter(now)) {
                LocalDateTime.of(nextSun.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)), time)
            } else dt
        } else {
            val today = LocalDateTime.of(now.toLocalDate(), time)
            if (today.isAfter(now)) today else today.plusDays(1)
        }
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }
}

/**
 * 每日定时触发的广播接收器。根据 kind 查询当前用户数据，构造摘要 → 通过
 * NotificationCenter 投递。完成后无论成败都会重新调度下一次（每日/每周）。
 */
class DailyDigestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kindName = intent.getStringExtra("kind") ?: return
        val kind = runCatching { DailyDigestKind.valueOf(kindName) }.getOrNull() ?: return
        val app = context.applicationContext
        // 异步处理：DB 查询不能阻塞 onReceive
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = runCatching { UserSessionManager(app).getCurrentUserId() }.getOrDefault(0L)
                if (userId > 0L && NotificationPrefs.isGlobalEnabled(app)
                    && NotificationPrefs.isChannelEnabled(app, kind.channel)
                ) {
                    when (kind) {
                        DailyDigestKind.GOAL -> sendGoalDigest(app, userId)
                        DailyDigestKind.HABIT -> sendHabitDigest(app, userId)
                        DailyDigestKind.MOOD -> sendMoodDigest(app, userId)
                        DailyDigestKind.WEEKLY -> sendWeeklyDigest(app, userId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DailyDigestReceiver", "process failed", e)
            } finally {
                // 重新调度下一次
                runCatching { DailyDigestScheduler.schedule(app, kind) }
                pending.finish()
            }
        }
    }

    private suspend fun sendGoalDigest(ctx: Context, userId: Long) {
        val db = AppDatabase.getDatabase(ctx)
        val active = runCatching { db.goalDao().getActiveGoals(userId).first() }.getOrDefault(emptyList())
        val countdowns = runCatching { db.goalDao().getAllCountdowns(userId).first() }.getOrDefault(emptyList())
        if (active.isEmpty() && countdowns.isEmpty()) return
        val today = LocalDate.now()
        val dueSoon = countdowns.mapNotNull { c ->
            runCatching { LocalDate.parse(c.targetDate) }.getOrNull()?.let { d ->
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, d)
                if (days in 0..7L) Triple(c.title, days, d) else null
            }
        }.sortedBy { it.second }

        val title = if (active.isNotEmpty()) "今天还有 ${active.size} 个目标在等你"
        else "你有 ${dueSoon.size} 个倒数日临近"
        val sb = StringBuilder()
        if (active.isNotEmpty()) {
            val avg = active.map { it.progress }.average().toInt()
            sb.append("活跃目标 ${active.size} 个，平均进度 ${avg}%。\n")
            active.take(3).forEach { sb.append("• ${it.title}（${it.progress}%）\n") }
        }
        if (dueSoon.isNotEmpty()) {
            sb.append("\n临近倒数日：\n")
            dueSoon.take(3).forEach { (t, d, _) ->
                sb.append("• $t — ${if (d == 0L) "就是今天" else "还有 ${d}天"}\n")
            }
        }

        NotificationCenter.notify(
            ctx,
            NotificationSpec(
                channel = FunChannel.GOAL,
                id = 7700,
                title = title,
                body = sb.toString().trim(),
                deepLinkRoute = "goal",
                dedupWindowMs = 23L * 60 * 60 * 1000
            )
        )
    }

    private suspend fun sendHabitDigest(ctx: Context, userId: Long) {
        val db = AppDatabase.getDatabase(ctx)
        val habits = runCatching { db.habitDao().getAllActiveHabits(userId).first() }.getOrDefault(emptyList())
        if (habits.isEmpty()) return
        val title = "今晚还有 ${habits.size} 个习惯等你打卡"
        val body = buildString {
            append("坚持是最好的礼物 ✅\n")
            habits.take(4).forEach { append("• ${it.name}\n") }
            if (habits.size > 4) append("…还有 ${habits.size - 4} 个")
        }
        NotificationCenter.notify(
            ctx,
            NotificationSpec(
                channel = FunChannel.HABIT,
                id = 7701,
                title = title,
                body = body.trim(),
                deepLinkRoute = "habit",
                dedupWindowMs = 23L * 60 * 60 * 1000
            )
        )
    }

    private suspend fun sendMoodDigest(ctx: Context, userId: Long) {
        // 心情邮箱：随机一句早安寄语 + 提醒记录今日心情
        val msg = MOOD_LETTERS.random()
        NotificationCenter.notify(
            ctx,
            NotificationSpec(
                channel = FunChannel.MOOD,
                id = 7702,
                title = "心情邮箱 · 今日寄语",
                body = "$msg\n\n点击记录今天的心情，让美好被看见 💌",
                deepLinkRoute = "mood",
                dedupWindowMs = 23L * 60 * 60 * 1000
            )
        )
    }

    private suspend fun sendWeeklyDigest(ctx: Context, userId: Long) {
        val db = AppDatabase.getDatabase(ctx)
        val moods = runCatching { db.moodDao().getAllMoodEntries(userId).first() }.getOrDefault(emptyList())
        val goals = runCatching { db.goalDao().getActiveGoals(userId).first() }.getOrDefault(emptyList())
        val sevenDaysAgo = LocalDate.now().minusDays(7).toString()
        val weekMoods = moods.filter { it.date >= sevenDaysAgo }
        val body = buildString {
            append("过去一周心情记录：${weekMoods.size} 次\n")
            append("活跃目标：${goals.size} 个\n")
            if (goals.isNotEmpty()) {
                val avg = goals.map { it.progress }.average().toInt()
                append("平均进度：${avg}%\n")
            }
            append("\n点击查看详细数据看板 →")
        }
        NotificationCenter.notify(
            ctx,
            NotificationSpec(
                channel = FunChannel.WEEKLY,
                id = 7703,
                title = "本周精选 · 数据回顾",
                body = body.trim(),
                deepLinkRoute = "home",
                dedupWindowMs = 6L * 24 * 60 * 60 * 1000
            )
        )
    }

    companion object {
        private val MOOD_LETTERS = listOf(
            "今天也要记得，照顾好自己的小情绪。",
            "你不必时刻闪闪发光，平静本身就是一种温柔。",
            "把今天的烦恼写下来，明天会有更好的答案。",
            "没人能替你呼吸，但有人愿意陪你前行。",
            "允许自己慢一点，世界没有那么急。",
            "情绪不是敌人，是身体在说话。",
            "今天哭过笑过的你，都很可爱。"
        )
    }
}
