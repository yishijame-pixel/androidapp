// MorningHeraldWorker.kt — v53 阅光书房 · 晨光信使
//
// 🎯 职责：每天早晨 7:30 ± 5 分钟随机触发，向所有有 quote 记录的用户推一张"晨光卡"
//   内容池权重：
//     40% 昨日新增 quote
//     30% 即将到期 / 当日的胶囊预告
//     20% 心情近况配方书金句（直接使用质量摘抄兜底）
//     10% 心情低谷"过去的你"（与 LetterDeliveryWorker 不冲突，会跳过当月已发的）
//
// 🛡️ 多用户隔离：以 quotes 表的 distinctUserIds 为遍历集合；morning_herald_log
//   复合主键 (userId, dateYmd) 防同日重复；普通用户按 VipQuota.heraldWeeklyLimit 限频。
package com.example.funlife.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MorningHeraldLog
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.NotificationCenter
import com.example.funlife.notifications.NotificationSpec
import com.example.funlife.vip.VipQuota
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MorningHeraldWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            // 🔒 时段守卫：只在 6-10 点之间真正发推；其余时段直接成功（避免周期任务在错误时间扰民）
            if (hour !in 6..10) {
                android.util.Log.d(TAG, "skip outside morning window (hour=$hour)")
                return Result.success()
            }

            val users = db.quoteDao().distinctUserIds().filter { it > 0L }
            if (users.isEmpty()) return Result.success()

            val today = ymdOf(cal)
            val weekStart = ymdOf((cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) })

            for (uid in users) {
                runCatching { sendForUser(db, uid, today, weekStart) }
                    .onFailure { android.util.Log.w(TAG, "herald uid=$uid failed: ${it.message}") }
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "MorningHeraldWorker failed", e)
            Result.retry()
        }
    }

    private suspend fun sendForUser(db: AppDatabase, userId: Long, todayYmd: Int, weekStartYmd: Int) {
        val logDao = db.morningHeraldLogDao()
        val quoteDao = db.quoteDao()

        // 当日防重
        if (logDao.getOf(userId, todayYmd) != null) return

        // VIP 周频控
        val vipLevel = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
        val weekLimit = VipQuota.heraldWeeklyLimit(vipLevel)
        val sentThisWeek = logDao.countSince(userId, weekStartYmd)
        if (sentThisWeek >= weekLimit) {
            android.util.Log.d(TAG, "uid=$userId weekly limit reached ($sentThisWeek/$weekLimit)")
            return
        }

        // 抽内容（按权重）
        val roll = Random.nextInt(100)
        val (title, body, type) = when {
            roll < 40 -> pickYesterdayQuote(quoteDao, userId)
                ?: pickQualityQuote(quoteDao, userId)
                ?: return
            roll < 70 -> pickUpcomingCapsule(quoteDao, userId)
                ?: pickQualityQuote(quoteDao, userId)
                ?: return
            roll < 90 -> pickQualityQuote(quoteDao, userId) ?: return
            else      -> pickQualityQuote(quoteDao, userId) ?: return
        }

        NotificationCenter.notify(
            applicationContext,
            NotificationSpec(
                channel = FunChannel.LETTER,
                id = 89500 + (userId % 1000).toInt(),
                title = title,
                body = body,
                deepLinkRoute = "reading_room",
                dedupWindowMs = TimeUnit.HOURS.toMillis(20),
                bypassQuietHours = false,
            )
        )
        logDao.insertIgnore(
            MorningHeraldLog(
                userId = userId,
                dateYmd = todayYmd,
                contentType = type,
                payloadSummary = body.take(80),
                sentAt = System.currentTimeMillis(),
            )
        )
        android.util.Log.d(TAG, "herald sent uid=$userId type=$type")
    }

    /* ─────────── 取材 ─────────── */

    private suspend fun pickYesterdayQuote(
        quoteDao: com.example.funlife.data.dao.QuoteDao, userId: Long
    ): Triple<String, String, String>? {
        val (s, e) = yesterdayMsRange()
        val q = quoteDao.pickRandomCreatedBetween(userId, s, e) ?: return null
        return Triple(
            "🌅 昨天的你写下",
            q.text.take(80),
            "quote_yesterday"
        )
    }

    private suspend fun pickUpcomingCapsule(
        quoteDao: com.example.funlife.data.dao.QuoteDao, userId: Long
    ): Triple<String, String, String>? {
        val window = 7L * 24 * 3600 * 1000
        val q = quoteDao.pickUpcomingCapsule(userId, System.currentTimeMillis(), window) ?: return null
        val deltaDays = ((q.capsuleDeliveryAt - System.currentTimeMillis()) / (24 * 3600 * 1000L)).toInt()
        val title = if (deltaDays <= 0) "📜 今天有一封胶囊到期"
        else "📜 还有 $deltaDays 天，会有一封胶囊到达"
        return Triple(title, q.text.take(80), "capsule_preview")
    }

    private suspend fun pickQualityQuote(
        quoteDao: com.example.funlife.data.dao.QuoteDao, userId: Long
    ): Triple<String, String, String>? {
        val q = quoteDao.pickRandomQuality(userId) ?: return null
        return Triple("🌅 一句晨光", q.text.take(80), "quote_quality")
    }

    private fun yesterdayMsRange(): Pair<Long, Long> {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val s = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        return s to c.timeInMillis
    }

    private fun ymdOf(c: Calendar): Int =
        c.get(Calendar.YEAR) * 10000 +
        (c.get(Calendar.MONTH) + 1) * 100 +
        c.get(Calendar.DAY_OF_MONTH)

    companion object {
        private const val TAG = "MorningHeraldWorker"
        const val WORK_NAME = "morning_herald_periodic"

        /**
         * 注册周期任务：每 24h 一次，初始延迟到下一个 7:30。
         * 任务内部还会做 6-10 点时段守卫，防 WorkManager 调度漂移。
         */
        fun schedulePeriodic(context: Context) {
            try {
                val now = Calendar.getInstance()
                val target = (now.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 7)
                    set(Calendar.MINUTE, 30 + (Math.random() * 10).toInt() - 5) // 7:25-7:35 抖动
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
                }
                val initialDelay = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = PeriodicWorkRequestBuilder<MorningHeraldWorker>(
                    24, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES,
                ).setConstraints(constraints)
                 .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                 .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                android.util.Log.d(TAG, "schedulePeriodic OK delay=${initialDelay / 60000}min")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "schedulePeriodic failed", e)
            }
        }
    }
}
