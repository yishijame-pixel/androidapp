// FriendRequestNotifier.kt — 好友申请 → 系统通知栏 + 应用内收件箱（铃铛红点）
package com.example.funlife.notifications

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.repository.FriendsRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object FriendRequestNotifier {

    private const val TAG = "FriendRequestNotif"
    private const val PREFS = "fun_friend_request_notif"
    private const val K_NOTIFIED_PREFIX = "notified_"
    private val notifyLock = Any()

    /**
     * @param pruneNotified 全量补拉时为 true，清理已不存在的 pending；Realtime 单条推送应为 false
     */
    fun notifyNewIncomingRequests(
        ctx: Context,
        userId: Long,
        pendingIn: List<FriendUiModel>,
        pruneNotified: Boolean = false,
    ) {
        if (userId <= 0L || pendingIn.isEmpty()) return
        synchronized(notifyLock) {
            val appCtx = ctx.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = "${K_NOTIFIED_PREFIX}$userId"
            val notified = HashSet(prefs.getStringSet(key, emptySet()) ?: emptySet())

            var deliveredCount = 0
            pendingIn.filter { it.isIncomingRequest }.forEach { friend ->
                if (friend.friendshipId in notified) return@forEach
                val name = friend.displayName.ifBlank { friend.funlifeUsername }
                val username = friend.funlifeUsername.ifBlank { "用户" }
                val dedupeKey = "friend_req_${friend.friendshipId}"
                val delivered = NotificationCenter.notify(
                    appCtx,
                    NotificationSpec(
                        channel = FunChannel.SOCIAL,
                        id = friend.friendshipId.hashCode() and 0x7FFFFFFF,
                        title = "新的好友申请",
                        body = "$name (@$username) 请求添加你为好友",
                        deepLinkRoute = "friends",
                        dedupWindowMs = 0L,
                        bypassQuietHours = true,
                        alwaysDeliverInbox = true,
                        inboxDedupeKey = dedupeKey,
                    ),
                )
                if (delivered) {
                    notified.add(friend.friendshipId)
                    deliveredCount++
                    SocialAlertBus.publish(
                        SocialHeadsUpAlert(
                            id = friend.friendshipId,
                            title = "新的好友申请",
                            body = "$name (@$username) 请求添加你为好友",
                            deepLinkRoute = "friends",
                        ),
                    )
                }
            }

            if (pruneNotified) {
                val activeIds = pendingIn.map { it.friendshipId }.toSet()
                notified.retainAll(activeIds)
            }
            prefs.edit().putStringSet(key, notified).apply()
            InboxStore.refreshUnread(appCtx)
            if (deliveredCount > 0) {
                Log.d(TAG, "delivered $deliveredCount friend request(s) userId=$userId")
            }
        }
    }

    /** Realtime 单条推送：先补全申请人资料再发通知 */
    suspend fun notifyIncomingRequest(
        ctx: Context,
        userId: Long,
        incoming: FriendUiModel,
        token: String,
    ) {
        val api = PocketBaseApiClient(ctx.applicationContext)
        val enriched = enrichIncoming(incoming, token, api)
        notifyNewIncomingRequests(ctx.applicationContext, userId, listOf(enriched))
    }

    private suspend fun enrichIncoming(
        incoming: FriendUiModel,
        token: String,
        api: PocketBaseApiClient,
    ): FriendUiModel {
        if (incoming.funlifeUsername.length >= 2 &&
            incoming.funlifeUsername != incoming.friendPbId.take(8)
        ) {
            return incoming
        }
        val profile = api.getUserById(token, incoming.friendPbId) ?: return incoming
        if (profile.funlifeUsername.isBlank()) return incoming
        return incoming.copy(
            funlifeUsername = profile.funlifeUsername,
            displayName = profile.displayName.ifBlank { profile.funlifeUsername },
            avatarUrl = profile.avatarUrl,
        )
    }

    /** 拒绝/删除好友后：取消系统通知 + 清理去重状态 */
    fun onFriendshipResolved(ctx: Context, userId: Long, friendshipId: String) {
        synchronized(notifyLock) {
            val appCtx = ctx.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = "${K_NOTIFIED_PREFIX}$userId"
            val notified = HashSet(prefs.getStringSet(key, emptySet()) ?: emptySet())
            notified.remove(friendshipId)
            prefs.edit().putStringSet(key, notified).apply()
            NotificationCenter.cancel(appCtx, friendshipId.hashCode() and 0x7FFFFFFF)
            InboxStore.removeByDedupeKey(appCtx, "friend_req_$friendshipId")
            InboxStore.refreshUnread(appCtx)
        }
    }

    fun pollAsync(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { pollOnce(ctx) }
                .onFailure { Log.w(TAG, "poll failed: ${it.message}") }
        }
    }

    /** 首页/后台补拉：先轻量查 pending → 写收件箱，再同步好友缓存 */
    suspend fun pollOnce(ctx: Context) = withContext(Dispatchers.IO) {
        if (!PocketBaseConfig.isEnabled()) return@withContext
        val userId = runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
        if (userId <= 0L) return@withContext
        val app = ctx.applicationContext

        val cred = SocialSessionManager.acquireCredentials(app, userId, forceSession = true)
            .getOrElse {
                Log.w(TAG, "poll skipped: ${it.message} userId=$userId")
                return@withContext
            }

        val api = PocketBaseApiClient(app)
        val pendingIn = runCatching {
            api.listPendingIncoming(cred.token, cred.pbRecordId).map { dto ->
                val requester = dto.requester?.takeUnless { it.funlifeUsername.isBlank() }
                    ?: api.getUserById(cred.token, dto.requesterId)
                dtoToIncomingUi(dto.copy(requester = requester ?: dto.requester))
            }
        }.getOrElse {
            Log.w(TAG, "poll network failed: ${it.message}")
            return@withContext
        }
        if (pendingIn.isNotEmpty()) {
            notifyNewIncomingRequests(app, userId, pendingIn, pruneNotified = true)
        }

        val db = (app as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(app)
        val linkRepo = SocialLinkRepository(app, db.socialDao())
        runCatching {
            FriendsRepository(app, db.socialDao(), linkRepo)
                .refreshFriends(userId, cred.pbRecordId, cred.token, notifyNewRequests = false)
        }.onFailure { Log.w(TAG, "cache refresh failed: ${it.message}") }
    }

    private fun dtoToIncomingUi(dto: com.example.funlife.social.model.FriendshipDto): FriendUiModel {
        val requester = dto.requester
        val username = requester?.funlifeUsername?.ifBlank { null }
            ?: requester?.displayName?.ifBlank { null }
            ?: "用户"
        val displayName = requester?.displayName?.ifBlank { null }
            ?: requester?.funlifeUsername?.ifBlank { null }
            ?: "新好友"
        return FriendUiModel(
            friendshipId = dto.id,
            friendPbId = dto.requesterId,
            funlifeUsername = username,
            displayName = displayName,
            avatarUrl = requester?.avatarUrl,
            status = FriendshipStatus.PENDING,
            isIncomingRequest = true,
            remark = "",
        )
    }
}
