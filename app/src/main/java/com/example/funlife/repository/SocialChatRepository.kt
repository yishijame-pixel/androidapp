package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.data.model.SocialConversationCache
import com.example.funlife.data.model.SocialMessageCache
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.SocialChatUtils
import com.example.funlife.social.model.ChatMessageUiModel
import com.example.funlife.social.model.ChatPeerProfile
import com.example.funlife.social.model.ConversationUiModel
import com.example.funlife.social.model.MessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SocialChatRepository(
    private val context: Context,
    private val socialDao: SocialDao,
    private val linkRepository: SocialLinkRepository,
) {
    private val api = PocketBaseApiClient(context.applicationContext)

    fun observeMessages(
        userId: Long,
        myPbId: String,
        conversationId: String,
    ): Flow<List<ChatMessageUiModel>> =
        socialDao.observeMessages(userId, conversationId).map { list ->
            list.map { it.toUi(myPbId) }
        }

    fun observeConversations(userId: Long): Flow<List<ConversationUiModel>> =
        socialDao.observeConversations(userId).map { list ->
            list.map { it.toUi() }
        }

    suspend fun syncConversations(
        userId: Long,
        myPbId: String,
        token: String,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val remote = api.listConversations(token, myPbId)
            val friends = socialDao.getFriends(userId)
                .filter { it.status == "accepted" }
                .associateBy { it.friendPbId }
            val cache = buildList {
                for (dto in remote) {
                    val peerId = if (dto.memberAId == myPbId) dto.memberBId else dto.memberAId
                    if (peerId.isBlank()) continue
                    val friend = friends[peerId] ?: continue
                    val local = socialDao.getConversation(userId, dto.id)
                    add(
                        SocialConversationCache(
                            userId = userId,
                            conversationId = dto.id,
                            peerPbId = peerId,
                            peerUsername = friend.funlifeUsername,
                            peerDisplayName = friend.displayName.ifBlank { friend.funlifeUsername },
                            peerAvatarUrl = friend.avatarUrl,
                            lastPreview = dto.lastPreview.ifBlank { local?.lastPreview.orEmpty() },
                            lastMessageAt = maxOf(dto.lastMessageAt, local?.lastMessageAt ?: 0L),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
            if (cache.isNotEmpty()) socialDao.upsertConversations(cache)
            Result.success(cache.map { it.conversationId })
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "同步会话失败"))
        } catch (e: Exception) {
            Result.failure(Exception("同步会话失败，请检查网络"))
        }
    }

    suspend fun loadPeerProfile(userId: Long, peerPbId: String): ChatPeerProfile? =
        withContext(Dispatchers.IO) {
            val friend = socialDao.getFriends(userId).firstOrNull { it.friendPbId == peerPbId && it.status == "accepted" }
                ?: return@withContext null
            ChatPeerProfile(
                pbId = friend.friendPbId,
                funlifeUsername = friend.funlifeUsername,
                displayName = friend.displayName.ifBlank { friend.funlifeUsername },
                avatarUrl = friend.avatarUrl,
            )
        }

    suspend fun ensureConversation(
        userId: Long,
        myPbId: String,
        peer: ChatPeerProfile,
        token: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isAcceptedFriend(userId, peer.pbId)) {
                return@withContext Result.failure(IllegalArgumentException("只能与已接受的好友私聊"))
            }
            val dto = api.findOrCreateConversation(token, myPbId, peer.pbId)
            upsertConversationCache(userId, dto.id, peer, dto.lastPreview, dto.lastMessageAt)
            Result.success(dto.id)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "打开会话失败"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "打开会话失败"))
        }
    }

    suspend fun syncMessages(
        userId: Long,
        myPbId: String,
        conversationId: String,
        token: String,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            syncMessagesPage(userId, conversationId, token, page = 1)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "同步消息失败"))
        } catch (e: Exception) {
            Result.failure(Exception("同步消息失败，请检查网络"))
        }
    }

    /** 上拉加载更早消息；page=2 起为更旧分页（newestFirst）。 */
    suspend fun loadOlderMessages(
        userId: Long,
        conversationId: String,
        token: String,
        page: Int,
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (page < 2) return@withContext Result.success(false)
        try {
            syncMessagesPage(userId, conversationId, token, page)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "加载历史消息失败"))
        } catch (e: Exception) {
            Result.failure(Exception("加载历史消息失败，请检查网络"))
        }
    }

    private suspend fun syncMessagesPage(
        userId: Long,
        conversationId: String,
        token: String,
        page: Int,
    ): Result<Boolean> {
        val pageResult = api.listMessagesPage(
            token = token,
            conversationId = conversationId,
            page = page,
            perPage = 100,
            newestFirst = true,
        )
        if (pageResult.messages.isEmpty()) {
            return Result.success(false)
        }
        val cache = pageResult.messages.map { it.toCache(userId) }
        socialDao.upsertMessages(cache)
        pageResult.messages.maxByOrNull { it.createdAt }?.let { latest ->
            val peer = socialDao.getConversation(userId, conversationId)
            if (peer != null && page == 1) {
                socialDao.upsertConversation(
                    peer.copy(
                        lastPreview = SocialChatUtils.previewText(latest.body),
                        lastMessageAt = latest.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        return Result.success(pageResult.hasMore)
    }

    suspend fun sendMessage(
        userId: Long,
        myPbId: String,
        conversationId: String,
        body: String,
        token: String,
    ): Result<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val validated = SocialChatUtils.validateMessageBody(body).getOrElse {
                return@withContext Result.failure(it)
            }
            val conv = socialDao.getConversation(userId, conversationId)
                ?: return@withContext Result.failure(IllegalStateException("会话不存在"))
            val (memberA, memberB) = SocialChatUtils.orderedMembers(myPbId, conv.peerPbId)
            val dto = api.sendMessage(token, conversationId, myPbId, memberA, memberB, validated)
            socialDao.upsertMessage(dto.toCache(userId))
            socialDao.getConversation(userId, conversationId)?.let { conv ->
                socialDao.upsertConversation(
                    conv.copy(
                        lastPreview = SocialChatUtils.previewText(validated),
                        lastMessageAt = dto.createdAt,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
            Result.success(dto)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "发送失败"))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "发送失败"))
        }
    }

    suspend fun getCachedMessages(
        userId: Long,
        myPbId: String,
        conversationId: String,
    ): List<ChatMessageUiModel> =
        withContext(Dispatchers.IO) {
            socialDao.getRecentMessages(userId, conversationId, limit = 100)
                .sortedBy { it.createdAt }
                .map { it.toUi(myPbId) }
        }

    private suspend fun isAcceptedFriend(userId: Long, peerPbId: String): Boolean =
        socialDao.getFriends(userId).any { it.friendPbId == peerPbId && it.status == "accepted" }

    private suspend fun upsertConversationCache(
        userId: Long,
        conversationId: String,
        peer: ChatPeerProfile,
        lastPreview: String,
        lastMessageAt: Long,
    ) {
        socialDao.upsertConversation(
            SocialConversationCache(
                userId = userId,
                conversationId = conversationId,
                peerPbId = peer.pbId,
                peerUsername = peer.funlifeUsername,
                peerDisplayName = peer.displayName,
                peerAvatarUrl = peer.avatarUrl,
                lastPreview = lastPreview,
                lastMessageAt = lastMessageAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun MessageDto.toCache(userId: Long) = SocialMessageCache(
        userId = userId,
        conversationId = conversationId,
        messageId = id,
        senderPbId = senderPbId,
        body = body,
        createdAt = createdAt,
    )

    private fun SocialMessageCache.toUi(myPbId: String): ChatMessageUiModel =
        ChatMessageUiModel(
            id = messageId,
            body = body,
            isMine = senderPbId == myPbId,
            createdAt = createdAt,
        )

    private fun SocialConversationCache.toUi(): ConversationUiModel =
        ConversationUiModel(
            conversationId = conversationId,
            peerPbId = peerPbId,
            peerDisplayName = peerDisplayName.ifBlank { peerUsername },
            peerUsername = peerUsername,
            peerAvatarUrl = peerAvatarUrl,
            lastPreview = lastPreview,
            lastMessageAt = lastMessageAt,
        )
}
