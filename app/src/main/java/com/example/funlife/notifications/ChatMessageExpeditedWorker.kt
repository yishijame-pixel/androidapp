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
import com.example.funlife.social.SocialChatInbound
import com.example.funlife.utils.UserSessionManager

/**
 * FCM / 网络恢复后加急补拉私聊会话（比 15 分钟周期 Worker 更快）。
 */
class ChatMessageExpeditedWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!PocketBaseConfig.isEnabled()) return Result.success()
        return try {
            val userId = UserSessionManager(applicationContext).getCurrentUserId()
            if (userId > 0L) {
                SocialChatInbound.syncActiveConversations(applicationContext, userId)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w(TAG, "chat expedited sync failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ChatMsgExpedited"
        private const val WORK_NAME = "chat_message_expedited"

        fun enqueue(context: Context) {
            if (!PocketBaseConfig.isEnabled()) return
            try {
                val request = OneTimeWorkRequestBuilder<ChatMessageExpeditedWorker>()
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
