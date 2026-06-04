package com.example.funlife.social

import android.content.Context
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.SocialChatRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.model.ChatMessageUiModel
import com.example.funlife.social.model.ChatPeerProfile
import com.example.funlife.social.model.ConversationUiModel
import kotlinx.coroutines.flow.Flow

/**
 * 私聊域业务编排：UI 只与此类交互。
 */
class ChatInteractor(
    private val context: Context,
    private val userId: Long,
) {
    private val appCtx = context.applicationContext
    private val socialDao = (appCtx as FunLifeApplication).database.socialDao()
    private val linkRepo = SocialLinkRepository(appCtx, socialDao)
    private val chatRepo = SocialChatRepository(appCtx, socialDao, linkRepo)

    suspend fun loadPeer(peerPbId: String): ChatPeerProfile? =
        chatRepo.loadPeerProfile(userId, peerPbId)

    fun observeConversations(): Flow<List<ConversationUiModel>> =
        chatRepo.observeConversations(userId)

    suspend fun syncConversations(): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "同步会话",
        ) { cred ->
            chatRepo.syncConversations(userId, cred.pbRecordId, cred.token).map { }
        }

    suspend fun loadCachedSession(peerPbId: String): ChatBootstrap? {
        val peer = loadPeer(peerPbId) ?: return null
        val conv = socialDao.getConversationByPeer(userId, peerPbId) ?: return null
        val cred = SocialOperationGate.peek(appCtx, userId) ?: return null
        return ChatBootstrap(
            conversationId = conv.conversationId,
            peer = peer,
            myPbId = cred.pbRecordId,
            token = cred.token,
            hasMoreHistory = false,
        )
    }

    suspend fun bootstrap(peerPbId: String): Result<ChatBootstrap> {
        if (!PocketBaseConfig.isEnabled()) {
            return Result.failure(SocialFailureException(SocialFailure.NotConfigured))
        }
        val peer = loadPeer(peerPbId)
            ?: return Result.failure(SocialFailureException(SocialFailure.Validation("只能与已接受的好友私聊")))
        return SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "打开私聊",
        ) { cred ->
            chatRepo.ensureConversation(userId, cred.pbRecordId, peer, cred.token)
                .map { conversationId ->
                    // 消息同步放到 UI 后台，避免经隧道时整页转圈
                    ChatBootstrap(conversationId, peer, cred.pbRecordId, cred.token)
                }
        }
    }

    fun observeMessages(myPbId: String, conversationId: String): Flow<List<ChatMessageUiModel>> =
        chatRepo.observeMessages(userId, myPbId, conversationId)

    suspend fun syncMessages(conversationId: String): Result<Boolean> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "同步消息",
        ) { cred ->
            chatRepo.syncMessages(userId, cred.pbRecordId, conversationId, cred.token)
        }

    suspend fun loadOlderMessages(conversationId: String, page: Int): Result<Boolean> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "加载历史消息",
        ) { cred ->
            chatRepo.loadOlderMessages(userId, conversationId, cred.token, page)
        }

    suspend fun sendMessage(conversationId: String, body: String): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "发送消息",
        ) { cred ->
            chatRepo.sendMessage(userId, cred.pbRecordId, conversationId, body, cred.token).map { }
        }

    suspend fun peekToken(): String? =
        SocialOperationGate.peek(appCtx, userId)?.token

    data class ChatBootstrap(
        val conversationId: String,
        val peer: ChatPeerProfile,
        val myPbId: String,
        val token: String,
        val hasMoreHistory: Boolean = false,
    )
}
