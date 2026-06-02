package com.example.funlife.vip

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.utils.UserSessionManager
import java.util.concurrent.TimeUnit

/**
 * VIP 凭证周期复验 Worker
 *
 * 每 7 天（仅在联网时）调一次云端 verify：
 *   - 成功 → 续期凭证（再发 1 年）
 *   - 云端拒绝 → VipManager 自动降级用户为非 VIP
 *
 * 这是真正的"破解防御"——破解者改本地数据库 / 凭证最多撑 7 天就被云端否决。
 */
class VipReverifyWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val uid = UserSessionManager(applicationContext).getCurrentUserId()
            if (uid > 0L) {
                VipManager(applicationContext).reverify(uid)
            }
            Result.success()
        } catch (e: Exception) {
            // 网络异常等不算 failure，下个周期会再来
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE = "VipReverifyWorker"

        fun schedulePeriodic(context: Context) {
            val req = PeriodicWorkRequestBuilder<VipReverifyWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
