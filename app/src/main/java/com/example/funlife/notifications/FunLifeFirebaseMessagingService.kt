package com.example.funlife.notifications

import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.social.SocialPushTokenRegistry
import com.example.funlife.utils.UserSessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM 推送服务：杀进程 / 后台时接收私聊与好友申请。
 * 需放置 google-services.json 并启用 [BuildConfig.FCM_ENABLED]。
 */
class FunLifeFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        if (!BuildConfig.FCM_ENABLED) return
        val userId = runCatching { UserSessionManager(applicationContext).getCurrentUserId() }
            .getOrDefault(0L)
        SocialPushTokenRegistry.saveToken(applicationContext, userId, token)
        if (userId <= 0L) {
            Log.d(TAG, "onNewToken cached pending (no user session)")
            return
        }
        Log.d(TAG, "FCM token refreshed userId=$userId")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!BuildConfig.FCM_ENABLED) return
        val data = message.data
        if (data.isNotEmpty()) {
            FcmPushHandler.handleDataMessage(applicationContext, data)
            return
        }
        message.notification?.let { n ->
            val route = message.data["deep_link"] ?: "friends"
            val isChat = route.startsWith("friend_chat/")
            NotificationCenter.notify(
                applicationContext,
                NotificationSpec(
                    channel = FunChannel.SOCIAL,
                    id = (n.title ?: route).hashCode() and 0x7FFFFFFF,
                    title = n.title ?: "FunLife",
                    body = n.body ?: "",
                    deepLinkRoute = route,
                    bypassQuietHours = false,
                    skipInbox = isChat,
                ),
            )
        }
    }

    companion object {
        private const val TAG = "FunLifeFCM"
    }
}
