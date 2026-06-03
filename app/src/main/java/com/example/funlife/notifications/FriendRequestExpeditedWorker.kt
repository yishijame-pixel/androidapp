package com.example.funlife.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialInboxSync
import com.example.funlife.social.SocialSessionManager

/**
 * 加急一次性同步：网络恢复 / 会话就绪后立即补拉好友申请（比 15 分钟周期 Worker 更快）。
 */
class FriendRequestExpeditedWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!PocketBaseConfig.isEnabled()) return Result.success()
        return try {
            SocialSessionManager.ensureSession(applicationContext, timeoutMs = 12_000)
            SocialInboxSync.syncNow(applicationContext, force = true)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "expedited sync failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FriendReqExpedited"
        private const val WORK_NAME = "friend_request_expedited"

        fun enqueue(context: Context) {
            if (!PocketBaseConfig.isEnabled()) return
            try {
                val request = OneTimeWorkRequestBuilder<FriendRequestExpeditedWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            } catch (e: Exception) {
                android.util.Log.w(TAG, "enqueue failed: ${e.message}")
            }
        }
    }
}
