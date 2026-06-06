package com.example.funlife.social

import android.content.Context
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.repository.FriendsRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.PbUserProfile
import kotlinx.coroutines.flow.Flow

/**
 * 好友域业务编排层：UI 只与此类交互，不直接碰 Token / Session / Repository 细节。
 */
class FriendsInteractor(
    private val context: Context,
    private val userId: Long,
    private val funlifeUsername: String,
    private val displayName: String,
) {
    private val appCtx = context.applicationContext
    private val socialDao: SocialDao = (appCtx as FunLifeApplication).database.socialDao()
    private val linkRepo = SocialLinkRepository(appCtx, socialDao)
    private val friendsRepo = FriendsRepository(appCtx, socialDao, linkRepo)

    fun observeFriends(): Flow<List<FriendUiModel>> = friendsRepo.observeFriends(userId)

    /** 进入好友页：本地 hydrate + 可选绑定 + 同步列表 */
    suspend fun bootstrap(forceBind: Boolean = false): Result<Unit> {
        if (!PocketBaseConfig.isEnabled()) {
            return Result.failure(SocialFailureException(SocialFailure.NotConfigured))
        }
        SocialSessionManager.hydrateForUser(appCtx, userId)
        if (forceBind || !SocialSessionManager.isLinked(appCtx, userId)) {
            SocialOperationGate.acquire(appCtx, userId, forceSession = true).getOrElse { return Result.failure(it) }
        } else {
            SocialOperationGate.warmCredentials(appCtx, userId)
        }
        SocialSessionManager.warmStartAsync(appCtx)
        val friendsResult = syncFriends(forceSession = false)
        friendsResult.onSuccess {
            runCatching { ChatInteractor(appCtx, userId).syncConversations() }
        }
        return friendsResult
    }

    suspend fun peekCredentials(): SocialCredentials? =
        SocialOperationGate.peek(appCtx, userId)

    suspend fun syncFriends(forceSession: Boolean = false): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "同步好友",
            timeoutMs = SocialOperationGate.TIMEOUT_SYNC_MS,
            forceSession = forceSession,
        ) { cred ->
            friendsRepo.refreshFriends(userId, cred.pbRecordId, cred.token, notifyNewRequests = false)
        }

    suspend fun syncFriendsPresenceForLobby(): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "同步好友在线",
            timeoutMs = SocialOperationGate.TIMEOUT_SYNC_MS,
            forceSession = false,
        ) { cred ->
            friendsRepo.refreshAcceptedFriendsPresence(userId, cred.token)
            Result.success(Unit)
        }

    suspend fun searchUser(rawQuery: String): Result<PbUserProfile?> {
        val query = SocialOperationGate.validateSearchQuery(rawQuery).getOrElse { return Result.failure(it) }
        return SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "搜索用户",
            timeoutMs = SocialOperationGate.TIMEOUT_SEARCH_MS,
        ) { cred ->
            friendsRepo.searchUser(userId, query, cred.token).map { profile ->
                if (profile != null && profile.id == cred.pbRecordId) {
                    throw SocialFailureException(SocialFailure.Validation("不能添加自己"))
                }
                profile
            }
        }
    }

    suspend fun sendFriendRequest(target: PbUserProfile): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "发送好友请求",
            timeoutMs = SocialOperationGate.TIMEOUT_MUTATION_MS,
        ) { cred ->
            friendsRepo.sendFriendRequest(userId, cred.pbRecordId, target, cred.token)
        }

    suspend fun acceptRequest(friendshipId: String): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "接受好友",
            timeoutMs = SocialOperationGate.TIMEOUT_MUTATION_MS,
        ) { cred ->
            friendsRepo.acceptRequest(userId, cred.pbRecordId, friendshipId, cred.token)
        }

    suspend fun rejectRequest(friendshipId: String): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "拒绝好友",
            timeoutMs = SocialOperationGate.TIMEOUT_MUTATION_MS,
        ) { cred ->
            friendsRepo.rejectRequest(userId, cred.pbRecordId, friendshipId, cred.token)
        }

    suspend fun removeFriend(friendshipId: String): Result<Unit> =
        SocialOperationGate.run(
            ctx = appCtx,
            userId = userId,
            operation = "删除好友",
            timeoutMs = SocialOperationGate.TIMEOUT_MUTATION_MS,
        ) { cred ->
            friendsRepo.removeFriendship(userId, cred.pbRecordId, friendshipId, cred.token)
        }

    suspend fun updateRemark(friendPbId: String, remark: String): Result<Unit> =
        friendsRepo.updateRemark(userId, friendPbId, remark)

    fun observeConversations(): Flow<List<com.example.funlife.social.model.ConversationUiModel>> =
        ChatInteractor(appCtx, userId).observeConversations()

    suspend fun syncConversations(): Result<Unit> =
        ChatInteractor(appCtx, userId).syncConversations()
}
