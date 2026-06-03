package com.example.funlife.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.social.PocketBaseConfig
import java.util.concurrent.TimeUnit

/**
 * 后台补拉好友申请（Android 周期任务最短 15 分钟；与 Realtime 双通道）。
 */
class FriendRequestPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!PocketBaseConfig.isEnabled()) return Result.success()
        return try {
            com.example.funlife.social.SocialInboxSync.syncNow(applicationContext)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "poll failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FriendRequestPoll"
        private const val WORK_NAME = "friend_request_poll"

        fun schedule(context: Context) {
            if (!PocketBaseConfig.isEnabled()) return
            try {
                val request = PeriodicWorkRequestBuilder<FriendRequestPollWorker>(
                    15, TimeUnit.MINUTES,
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "schedule failed", e)
            }
        }
    }
}
