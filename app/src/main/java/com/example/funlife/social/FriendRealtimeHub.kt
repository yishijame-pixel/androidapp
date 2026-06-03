package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.notifications.FriendRequestNotifier
import com.example.funlife.repository.FriendsRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 应用级 PocketBase Realtime：好友申请秒级感知（App 进程存活时）。
 * 指数退避重连 + 连接态上报 [SocialSessionManager]。
 */
object FriendRealtimeHub {

    private const val TAG = "FriendRealtimeHub"
    private const val BACKOFF_INITIAL_MS = 2_000L
    private const val BACKOFF_MAX_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenJob: Job? = null
    private var backoffMs = BACKOFF_INITIAL_MS

    fun restart(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        stop()
        listenJob = scope.launch {
            val appCtx = ctx.applicationContext
            backoffMs = BACKOFF_INITIAL_MS
            while (isActive) {
                val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }
                    .getOrDefault(0L)
                if (userId <= 0L) {
                    SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.OFF)
                    delay(BACKOFF_INITIAL_MS)
                    continue
                }
                if (!SocialSessionManager.isLinked(appCtx, userId)) {
                    SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.OFF)
                    SocialSessionManager.warmStartAsync(appCtx)
                    delay(BACKOFF_INITIAL_MS)
                    continue
                }
                SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.CONNECTING)
                try {
                    runSession(appCtx, userId)
                    backoffMs = BACKOFF_INITIAL_MS
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Realtime session ended: ${e.message}")
                    SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.BACKOFF)
                    delay(backoffMs)
                    backoffMs = min(backoffMs * 2, BACKOFF_MAX_MS)
                    continue
                }
                SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.BACKOFF)
                delay(backoffMs)
                backoffMs = min(backoffMs * 2, BACKOFF_MAX_MS)
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.OFF)
    }

    private suspend fun runSession(ctx: Context, userId: Long) {
        val db = (ctx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(ctx)
        val link = db.socialDao().getLink(userId) ?: return
        val linkRepo = SocialLinkRepository(ctx, db.socialDao())
        val token = linkRepo.getValidToken(userId) ?: return
        val myPbId = link.pbRecordId
        val friendsRepo = FriendsRepository(ctx, db.socialDao(), linkRepo)
        val realtime = PocketBaseRealtimeClient()

        SocialSessionManager.onRealtimePhase(SocialSessionManager.RealtimePhase.LIVE)
        realtime.listenFriendships(
            authToken = token,
            myPbId = myPbId,
            onIncomingRequest = { incoming ->
                scope.launch {
                    runCatching {
                        FriendRequestNotifier.notifyIncomingRequest(ctx, userId, incoming, token)
                        friendsRepo.refreshFriends(userId, myPbId, notifyNewRequests = false)
                    }
                    SocialInboxSync.syncNowAsync(ctx, force = true)
                    SocialSessionManager.onSyncCompleted()
                }
            },
        )
    }
}
