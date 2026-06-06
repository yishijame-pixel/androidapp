package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.data.model.SocialFriendCache
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.SocialPresencePolicy
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.FriendshipDto
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.notifications.FriendRequestNotifier
import com.example.funlife.social.model.PbUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FriendsRepository(
    private val context: Context,
    private val socialDao: SocialDao,
    private val linkRepository: SocialLinkRepository,
) {
    private val api = PocketBaseApiClient(context.applicationContext)

    fun observeFriends(userId: Long): Flow<List<FriendUiModel>> =
        socialDao.observeFriends(userId).map { list ->
            list.map { it.toUi() }
        }

    suspend fun refreshFriends(
        userId: Long,
        myPbId: String,
        token: String? = null,
        notifyNewRequests: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authToken = token ?: linkRepository.getValidToken(userId)
                ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
            val dtos = api.listFriendships(authToken, myPbId)
            val oldFriends = socialDao.getFriends(userId).associateBy { it.friendPbId }
            val cache = dtos.mapNotNull { dto ->
                var entry = toCache(userId, dto, myPbId, oldFriends) ?: return@mapNotNull null
                if (entry.funlifeUsername.isBlank()) {
                    api.getUserById(authToken, entry.friendPbId)?.let { u ->
                        entry = entry.copy(
                            funlifeUsername = u.funlifeUsername,
                            displayName = u.displayName.ifBlank { u.funlifeUsername },
                            avatarUrl = u.avatarUrl,
                            online = SocialPresencePolicy.isEffectivelyOnline(u.online, u.updatedAtMs),
                        )
                    }
                }
                entry
            }
            socialDao.clearFriends(userId)
            if (cache.isNotEmpty()) socialDao.upsertFriends(cache)
            if (notifyNewRequests) {
                val pendingIn = socialDao.getFriends(userId)
                    .map { it.toUi() }
                    .filter { it.isIncomingRequest }
                FriendRequestNotifier.notifyNewIncomingRequests(
                    context.applicationContext, userId, pendingIn,
                )
            }
            Result.success(Unit)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "加载好友失败"))
        } catch (e: Exception) {
            Result.failure(Exception("加载好友失败，请检查网络"))
        }
    }

    suspend fun searchUser(
        userId: Long,
        username: String,
        token: String? = null,
    ): Result<PbUserProfile?> =
        withContext(Dispatchers.IO) {
            try {
                val authToken = token ?: linkRepository.getValidToken(userId)
                    ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
                val q = username.trim().removePrefix("@")
                if (q.length < 2) return@withContext Result.failure(IllegalArgumentException("用户名至少 2 个字符"))
                Result.success(api.findUserByFunlifeUsername(authToken, q))
            } catch (e: PocketBaseApiException) {
                Result.failure(Exception(e.message ?: "搜索失败"))
            } catch (e: Exception) {
                Result.failure(Exception("搜索失败，请检查网络"))
            }
        }

    suspend fun sendFriendRequest(
        userId: Long,
        myPbId: String,
        target: PbUserProfile,
        token: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (myPbId == target.id) {
            return@withContext Result.failure(IllegalArgumentException("不能添加自己"))
        }
        try {
            val authToken = token ?: linkRepository.getValidToken(userId)
                ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
            val exists = socialDao.getFriends(userId).any {
                it.friendPbId == target.id && it.status != "blocked"
            }
            if (exists) {
                return@withContext Result.failure(IllegalArgumentException("已是好友或请求已存在"))
            }
            val dto = api.createFriendRequest(authToken, myPbId, target.id)
            val cache = buildPendingOutCache(userId, dto, target)
            socialDao.upsertFriends(listOf(cache))
            Result.success(Unit)
        } catch (e: PocketBaseApiException) {
            Result.failure(Exception(e.message ?: "发送失败"))
        } catch (e: Exception) {
            Result.failure(Exception("发送好友请求失败"))
        }
    }

    suspend fun acceptRequest(
        userId: Long,
        myPbId: String,
        friendshipId: String,
        token: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val authToken = token ?: linkRepository.getValidToken(userId)
                    ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
                api.acceptFriendship(authToken, friendshipId)
                socialDao.updateFriendStatus(
                    userId, friendshipId, "accepted", System.currentTimeMillis(),
                )
                Result.success(Unit)
            } catch (e: PocketBaseApiException) {
                Result.failure(Exception(e.message ?: "接受失败"))
            } catch (e: Exception) {
                Result.failure(Exception("接受好友失败"))
            }
        }

    suspend fun rejectRequest(
        userId: Long,
        myPbId: String,
        friendshipId: String,
        token: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val authToken = token ?: linkRepository.getValidToken(userId)
                    ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
                socialDao.deleteFriendByFriendshipId(userId, friendshipId)
                api.deleteFriendship(authToken, friendshipId)
                FriendRequestNotifier.onFriendshipResolved(context.applicationContext, userId, friendshipId)
                Result.success(Unit)
            } catch (e: PocketBaseApiException) {
                runCatching { refreshFriends(userId, myPbId, notifyNewRequests = false) }
                Result.failure(Exception(e.message ?: "拒绝失败"))
            } catch (e: Exception) {
                runCatching { refreshFriends(userId, myPbId, notifyNewRequests = false) }
                Result.failure(Exception("拒绝好友申请失败"))
            }
        }

    suspend fun removeFriendship(
        userId: Long,
        myPbId: String,
        friendshipId: String,
        token: String? = null,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val authToken = token ?: linkRepository.getValidToken(userId)
                    ?: return@withContext Result.failure(IllegalStateException("未绑定社交账号"))
                api.deleteFriendship(authToken, friendshipId)
                socialDao.deleteFriendByFriendshipId(userId, friendshipId)
                FriendRequestNotifier.onFriendshipResolved(context.applicationContext, userId, friendshipId)
                Result.success(Unit)
            } catch (e: PocketBaseApiException) {
                Result.failure(Exception(e.message ?: "删除失败"))
            } catch (e: Exception) {
                Result.failure(Exception("删除好友失败"))
            }
        }

    suspend fun updateRemark(userId: Long, friendPbId: String, remark: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            socialDao.updateRemark(userId, friendPbId, remark.trim(), System.currentTimeMillis())
            Result.success(Unit)
        }

    suspend fun refreshAcceptedFriendsPresence(userId: Long, token: String) =
        withContext(Dispatchers.IO) {
            socialDao.getFriends(userId)
                .filter { it.status == "accepted" }
                .forEach { friend ->
                    api.getUserById(token, friend.friendPbId)?.let { profile ->
                        val effective = SocialPresencePolicy.isEffectivelyOnline(
                            profile.online,
                            profile.updatedAtMs,
                        )
                        socialDao.updateFriendOnline(
                            userId,
                            friend.friendPbId,
                            effective,
                            System.currentTimeMillis(),
                        )
                    }
                }
        }

    suspend fun updateFriendOnline(userId: Long, friendPbId: String, online: Boolean) =
        withContext(Dispatchers.IO) {
            socialDao.updateFriendOnline(userId, friendPbId, online, System.currentTimeMillis())
        }

    private fun buildPendingOutCache(
        userId: Long,
        dto: FriendshipDto,
        target: PbUserProfile,
    ): SocialFriendCache = SocialFriendCache(
        userId = userId,
        friendPbId = target.id,
        funlifeUsername = target.funlifeUsername,
        displayName = target.displayName.ifBlank { target.funlifeUsername },
        avatarUrl = target.avatarUrl,
        friendshipId = dto.id,
        status = "pending_out",
        remark = "",
        online = SocialPresencePolicy.isEffectivelyOnline(target.online, target.updatedAtMs),
        updatedAt = System.currentTimeMillis(),
    )

    private fun toCache(
        userId: Long,
        dto: FriendshipDto,
        myPbId: String,
        oldFriends: Map<String, SocialFriendCache>,
    ): SocialFriendCache? {
        val isRequester = dto.requesterId == myPbId
        val friend = if (isRequester) dto.addressee else dto.requester
        val friendId = if (isRequester) dto.addresseeId else dto.requesterId
        val profile = friend ?: return null
        if (profile.id == myPbId) return null

        val old = oldFriends[friendId]
        val username = profile.funlifeUsername.ifBlank { old?.funlifeUsername.orEmpty() }
        val displayName = profile.displayName.ifBlank { profile.funlifeUsername }
            .ifBlank { old?.displayName.orEmpty() }
            .ifBlank { username }
        val avatarUrl = profile.avatarUrl ?: old?.avatarUrl

        val statusWire = when (dto.status) {
            FriendshipStatus.ACCEPTED -> "accepted"
            FriendshipStatus.PENDING -> if (isRequester) "pending_out" else "pending_in"
            FriendshipStatus.BLOCKED -> return null
        }

        return SocialFriendCache(
            userId = userId,
            friendPbId = friendId,
            funlifeUsername = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            friendshipId = dto.id,
            status = statusWire,
            remark = old?.remark.orEmpty(),
            online = SocialPresencePolicy.isEffectivelyOnline(profile.online, profile.updatedAtMs),
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun loadUiModels(userId: Long): List<FriendUiModel> =
        socialDao.getFriends(userId).map { it.toUi() }

    private fun SocialFriendCache.toUi(): FriendUiModel {
        val fs = when (status) {
            "accepted" -> FriendshipStatus.ACCEPTED
            else -> FriendshipStatus.PENDING
        }
        return FriendUiModel(
            friendshipId = friendshipId,
            friendPbId = friendPbId,
            funlifeUsername = funlifeUsername,
            displayName = displayName,
            avatarUrl = avatarUrl,
            status = fs,
            isIncomingRequest = status == "pending_in",
            remark = remark,
            online = online,
        )
    }
}