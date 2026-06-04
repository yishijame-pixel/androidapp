package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.SocialConversationCache
import com.example.funlife.data.model.SocialMessageCache
import com.example.funlife.notifications.ChatMessageNotifier
import com.example.funlife.repository.SocialChatRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.model.MessageDto

/**
 * Realtime / 补拉 收到的新消息统一入库 + 通知。
 */
object SocialChatInbound {

    private const val TAG = "SocialChatInbound"

    suspend fun onIncomingMessage(
        ctx: Context,
        userId: Long,
        myPbId: String,
        dto: MessageDto,
        senderDisplayName: String?,
        senderUsername: String?,
    ) {
        if (userId <= 0L || dto.conversationId.isBlank()) return
        val appCtx = ctx.applicationContext
        val db = (appCtx as? FunLifeApplication)?.database
            ?: com.example.funlife.data.database.AppDatabase.getDatabase(appCtx)
        val socialDao = db.socialDao()

        socialDao.upsertMessage(
            SocialMessageCache(
                userId = userId,
                conversationId = dto.conversationId,
                messageId = dto.id,
                senderPbId = dto.senderPbId,
                body = dto.body,
                createdAt = dto.createdAt,
            ),
        )

        val peerPbId = when (dto.senderPbId) {
            myPbId -> socialDao.getConversation(userId, dto.conversationId)?.peerPbId
            else -> dto.senderPbId
        } ?: dto.senderPbId

        val friend = socialDao.getFriends(userId).firstOrNull { it.friendPbId == peerPbId && it.status == "accepted" }
        val displayName = friend?.displayName?.ifBlank { friend.funlifeUsername }
            ?: senderDisplayName?.ifBlank { null }
            ?: friend?.funlifeUsername
            ?: senderUsername
            ?: "好友"
        val username = friend?.funlifeUsername ?: senderUsername ?: peerPbId.take(8)
        val avatarUrl = friend?.avatarUrl

        val existing = socialDao.getConversation(userId, dto.conversationId)
        socialDao.upsertConversation(
            SocialConversationCache(
                userId = userId,
                conversationId = dto.conversationId,
                peerPbId = peerPbId,
                peerUsername = username,
                peerDisplayName = displayName,
                peerAvatarUrl = avatarUrl ?: existing?.peerAvatarUrl,
                lastPreview = SocialChatUtils.previewText(dto.body),
                lastMessageAt = dto.createdAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )

        if (dto.senderPbId == myPbId) return
        if (ChatFocusTracker.isFocused(userId, peerPbId)) {
            Log.d(TAG, "skip notify: user in chat peer=$peerPbId")
            return
        }

        ChatMessageNotifier.notifyIncoming(
            ctx = appCtx,
            userId = userId,
            messageId = dto.id,
            peerPbId = peerPbId,
            peerDisplayName = displayName,
            peerUsername = username,
            body = dto.body,
        )
    }

    /** Realtime 断线 / 回前台时补拉；发现新消息也会补通知（Realtime/FCM 不可用时的兜底） */
    suspend fun syncActiveConversations(ctx: Context, userId: Long) {
        if (userId <= 0L || !PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        val cred = SocialOperationGate.peek(appCtx, userId) ?: return
        val myPbId = cred.pbRecordId
        val db = (appCtx as? FunLifeApplication)?.database
            ?: com.example.funlife.data.database.AppDatabase.getDatabase(appCtx)
        val socialDao = db.socialDao()
        val linkRepo = SocialLinkRepository(appCtx, socialDao)
        val chatRepo = SocialChatRepository(appCtx, socialDao, linkRepo)
        chatRepo.syncConversations(userId, myPbId, cred.token)
            .onSuccess { convIds ->
                convIds.forEach { convId ->
                    val knownIds = socialDao.getRecentMessages(userId, convId, 50)
                        .map { it.messageId }
                        .toHashSet()
                    chatRepo.syncMessages(userId, myPbId, convId, cred.token)
                    val fresh = socialDao.getRecentMessages(userId, convId, 10)
                    for (msg in fresh) {
                        if (msg.messageId in knownIds) continue
                        if (msg.senderPbId == myPbId) continue
                        val conv = socialDao.getConversation(userId, convId) ?: continue
                        val peerPbId = conv.peerPbId
                        if (ChatFocusTracker.isFocused(userId, peerPbId)) continue
                        ChatMessageNotifier.notifyIncoming(
                            ctx = appCtx,
                            userId = userId,
                            messageId = msg.messageId,
                            peerPbId = peerPbId,
                            peerDisplayName = conv.peerDisplayName.ifBlank { conv.peerUsername },
                            peerUsername = conv.peerUsername,
                            body = msg.body,
                        )
                        Log.d(TAG, "notify from sync conv=$convId msg=${msg.messageId}")
                    }
                }
            }
    }
}
