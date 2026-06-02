// ═════════════════════════════════════════════════════════════════════════
// EngagementWorker.kt
// 拉活通知周期任务：用户长期不开 App 时，WorkManager 在合适时机唤醒，
//   扫描各模块空状态推送鼓励通知（与 App 启动时触发同一套逻辑）。
// 每 6 小时检查一次，每模块仍受 EngagementNotifier 内部 24h 节流约束。
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class EngagementWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        android.util.Log.d(TAG, "EngagementWorker 唤醒，扫描空模块")
        // 复用同步实现（suspend），保证逻辑一致
        EngagementNotifier.runOnce(applicationContext)
        Result.success()
    } catch (e: Exception) {
        android.util.Log.e(TAG, "EngagementWorker 失败", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "EngagementWorker"
        private const val WORK_NAME = "engagement_periodic"

        /** App 启动时调用一次，幂等。 */
        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
                val request = PeriodicWorkRequestBuilder<EngagementWorker>(
                    6, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
                ).setConstraints(constraints).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
                android.util.Log.d(TAG, "已注册周期任务（每 6 小时）")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "注册周期任务失败", e)
            }
        }
    }
}
