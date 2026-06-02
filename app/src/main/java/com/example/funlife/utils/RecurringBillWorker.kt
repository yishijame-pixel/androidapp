// RecurringBillWorker.kt — 定期账单后台扫描 Worker（Phase 3 · 企业级）
//
// 🔔 设计：
//   - WorkManager 周期任务（每 6 小时唤醒一次），抗 OEM 后台杀进程
//   - 取当前登录 userId（无登录跳过），扫描 RecurringBill 自动生成漏掉的账单
//   - 生成后弹高优通知 + 写入 Inbox（与纪念日通知同模式）
//   - 通知点击 → 拉起 MainActivity → ChatBill route
//
// 🔒 数据隔离：
//   - 严格按当前 userId 过滤；不会跨账号生成
//   - SharedPreferences 标记按 userId 命名空间
package com.example.funlife.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.NotificationCenter
import com.example.funlife.notifications.NotificationSpec
import com.example.funlife.repository.RecurringBillRepository
import java.util.concurrent.TimeUnit

class RecurringBillWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val sessionManager = UserSessionManager(applicationContext)
            val userId = sessionManager.getCurrentUserId()
            if (userId <= 0L) {
                android.util.Log.d(TAG, "no logged-in user, skip")
                return Result.success()
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val repo = RecurringBillRepository(db.recurringBillDao(), db.billDao())
            val now = System.currentTimeMillis()

            // 防抖：同一用户 4 小时内不重复扫描
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastScanKey = "last_scan_$userId"
            val lastScan = prefs.getLong(lastScanKey, 0L)
            if (now - lastScan < TimeUnit.HOURS.toMillis(4)) {
                android.util.Log.d(TAG, "skipped: recent scan within 4h")
                return Result.success()
            }

            val generated = repo.generateDueBills(userId, now)
            prefs.edit().putLong(lastScanKey, now).apply()

            if (generated > 0) {
                NotificationCenter.notify(
                    applicationContext,
                    NotificationSpec(
                        channel = FunChannel.BOOKKEEPING,
                        id = NOTIFICATION_ID + userId.toInt(),
                        title = "定期账单已自动入账",
                        body = "已为您新增 $generated 笔定期账单（如房租 / 订阅服务等）。点击进入聊天记账查看详情。",
                        deepLinkRoute = DEEP_LINK_CHAT_BILL,
                        dedupWindowMs = TimeUnit.HOURS.toMillis(2)
                    )
                )
            }

            android.util.Log.d(TAG, "scan completed, generated=$generated bills for userId=$userId")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "RecurringBillWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RecurringBillWorker"
        const val NOTIFICATION_ID = 88600
        const val PREFS = "recurring_bill_worker_prefs"
        const val WORK_NAME = "recurring_bill_periodic"
        const val DEEP_LINK_CHAT_BILL = "chat_bill"

        /**
         * 注册周期任务。每 6 小时执行一次。多次调用走 KEEP 策略，幂等。
         */
        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder().build()
                val request = PeriodicWorkRequestBuilder<RecurringBillWorker>(
                    6, TimeUnit.HOURS,
                    1, TimeUnit.HOURS
                ).setConstraints(constraints).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                android.util.Log.d(TAG, "schedulePeriodic OK")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "schedulePeriodic failed", e)
            }
        }
    }
}
