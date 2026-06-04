package com.example.funlife.notifications

import android.content.Context
import android.util.Log
import com.example.funlife.social.ChatFocusTracker
import com.example.funlife.social.SocialChatInbound
import com.example.funlife.social.model.MessageDto
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FCM 数据消息统一入口（杀进程 / 后台唤醒后仍可达）。
 * 与 Realtime [SocialChatInbound] 共用入库 + 通知逻辑，避免双通道行为不一致。
 */
object FcmPushHandler {

    private const val TAG = "FcmPushHandler"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun handleDataMessage(ctx: Context, data: Map<String, String>) {
        if (data.isEmpty()) return
        when (data["type"]) {
            "chat_message" -> scope.launch { handleChatMessage(ctx, data) }
            "friend_request" -> scope.launch { handleFriendRequest(ctx, data) }
            else -> Log.d(TAG, "ignore unknown type=${data["type"]}")
        }
    }

    private suspend fun handleChatMessage(ctx: Context, data: Map<String, String>) {
        val appCtx = ctx.applicationContext
        val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }.getOrDefault(0L)
        if (userId <= 0L) {
            Log.w(TAG, "chat push skipped: not logged in")
            return
        }

        val messageId = data["message_id"].orEmpty()
        val peerPbId = data["peer_pb_id"].orEmpty()
        val body = data["body"].orEmpty()
        if (messageId.isBlank() || peerPbId.isBlank()) {
            Log.w(TAG, "chat push missing ids")
            return
        }

        val myPbId = data["my_pb_id"].orEmpty()
        val conversationId = data["conversation_id"].orEmpty()
        val senderPbId = data["sender_pb_id"].orEmpty().ifBlank { peerPbId }
        val createdAt = data["created_at"]?.toLongOrNull() ?: System.currentTimeMillis()
        val displayName = data["peer_display_name"].orEmpty()
        val username = data["peer_username"].orEmpty()

        if (conversationId.isNotBlank() && myPbId.isNotBlank()) {
            val dto = MessageDto(
                id = messageId,
                conversationId = conversationId,
                senderPbId = senderPbId,
                body = body,
                createdAt = createdAt,
            )
            runCatching {
                SocialChatInbound.onIncomingMessage(
                    ctx = appCtx,
                    userId = userId,
                    myPbId = myPbId,
                    dto = dto,
                    senderDisplayName = displayName,
                    senderUsername = username,
                )
            }.onFailure { Log.w(TAG, "inbound failed: ${it.message}") }
        } else if (!ChatFocusTracker.isFocused(userId, peerPbId)) {
            ChatMessageNotifier.notifyIncoming(
                ctx = appCtx,
                userId = userId,
                messageId = messageId,
                peerPbId = peerPbId,
                peerDisplayName = displayName,
                peerUsername = username,
                body = body,
            )
        }

        ChatMessageExpeditedWorker.enqueue(appCtx)
    }

    private suspend fun handleFriendRequest(ctx: Context, data: Map<String, String>) {
        val appCtx = ctx.applicationContext
        runCatching {
            com.example.funlife.social.SocialInboxSync.syncNow(appCtx, force = true)
        }.onFailure { Log.w(TAG, "friend inbox sync failed: ${it.message}") }
        FriendRequestExpeditedWorker.enqueue(appCtx)
    }
}
