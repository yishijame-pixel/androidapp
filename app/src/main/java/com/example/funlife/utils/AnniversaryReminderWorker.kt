package com.example.funlife.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.MainActivity
import com.example.funlife.R
import com.example.funlife.data.database.AppDatabase
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 🔔 纪念日提醒 Worker
 *
 * 用 WorkManager 周期性检查（每 6 小时）今日是否有纪念日。
 *
 * 为什么 WorkManager 比 AlarmManager 可靠：
 *   - WorkManager 在 MIUI / EMUI / ColorOS 等国产 ROM 上抗杀进程能力更强
 *   - 即使应用进程被杀，系统仍会在合适时机唤醒 WorkManager
 *   - AlarmManager 在国产 ROM 经常被强制杀掉无法触发
 *
 * 双重保险：AlarmManager（精确）+ WorkManager（兜底）同时使用
 */
class AnniversaryReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d(TAG, "WorkManager 唤醒，检查今日纪念日")

            val sessionManager = UserSessionManager(applicationContext)
            val userId = sessionManager.getCurrentUserId()
            if (userId <= 0L) {
                android.util.Log.d(TAG, "无登录用户，跳过")
                return Result.success()
            }

            val dao = AppDatabase.getDatabase(applicationContext).anniversaryDao()
            val all = dao.getAllForUserOnce(userId)
            val todays = AnniversaryReminderManager.findTodayAnniversaries(all)

            if (todays.isEmpty()) {
                android.util.Log.d(TAG, "今日无纪念日")
                return Result.success()
            }

            // 检查今天是否已经发过通知（用 SharedPreferences 持久化标记）
            val today = LocalDate.now().toString()
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastNotifyDate = prefs.getString(KEY_LAST_DATE, null)
            if (lastNotifyDate == today) {
                android.util.Log.d(TAG, "今天已发过通知，跳过")
                return Result.success()
            }

            // 🔥 调用完整闹钟流程：震动 + 循环铃声 + 悬浮通知 + 全局悬浮窗
            // 而不是仅静默通知。这样用户才能立刻知道
            todays.forEach { AnniversaryReminderManager.markTriggered(it.id) }
            // 切到主线程触发（涉及 MediaPlayer 和 UI State）
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                AnniversaryReminderManager.triggerAlarm(applicationContext)
            }
            prefs.edit().putString(KEY_LAST_DATE, today).apply()

            android.util.Log.d(TAG, "已触发完整闹钟提醒：${todays.size} 个纪念日（震动+铃声+通知）")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Worker 失败", e)
            Result.retry()
        }
    }

    private fun sendBackgroundNotification(context: Context, count: Int) {
        // 创建通知渠道（IMPORTANCE_HIGH → 弹出 Heads-up）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "纪念日后台提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "App 在后台时也能提醒今日纪念日"
                    enableVibration(true)
                    enableLights(true)
                    lightColor = 0xFFEC407A.toInt()
                    setShowBadge(true)
                }
                nm.createNotificationChannel(ch)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE else 0
        val pi = PendingIntent.getActivity(
            context, 99, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎀 今日纪念日提醒")
            .setContentText("✨ 你有 $count 个值得庆祝的日子！点击查看 💗")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("✨ 你有 $count 个值得庆祝的日子！\n点击进入查看详情，让美好瞬间不被遗忘 💗")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setColor(0xFFEC407A.toInt())
            .setColorized(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notif)
    }

    companion object {
        private const val TAG = "AnniReminderWorker"
        private const val CHANNEL_ID = "anniversary_bg_reminder"
        private const val NOTIFICATION_ID = 88521
        private const val PREFS = "anniversary_worker_prefs"
        private const val KEY_LAST_DATE = "last_notify_date"
        private const val WORK_NAME = "anniversary_reminder_periodic"

        /**
         * 注册周期性后台任务（每 6 小时执行一次）
         * 应在应用启动时调用一次，多次调用会被 KEEP 策略忽略
         */
        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder().build()
                val request = PeriodicWorkRequestBuilder<AnniversaryReminderWorker>(
                    15, TimeUnit.MINUTES,   // 间隔 15 分钟（WorkManager 最小值）
                    5, TimeUnit.MINUTES     // flex 窗口
                )
                    .setConstraints(constraints)
                    .build()

                // UPDATE 策略：版本升级时能替换旧的间隔参数（如旧版 6h → 新版 15min）
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                android.util.Log.d(TAG, "周期性后台任务已注册（每 15 分钟检查一次）")

                // 🔥 同时立即触发一次 OneTimeWork（确保首次安装/更新后能立刻测试）
                val oneTime = OneTimeWorkRequestBuilder<AnniversaryReminderWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "anniversary_reminder_immediate",
                    ExistingWorkPolicy.REPLACE,
                    oneTime
                )
                android.util.Log.d(TAG, "立即触发一次后台检查")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "注册周期任务失败", e)
            }
        }
    }
}
