package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.FriendsRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 将 PocketBase users.online 与 App 进程生命周期对齐：
 * - 进入前台 → online = true，并启动心跳（刷新 users.updated）
 * - 退到后台 → online = false
 *
 * 强杀进程不会走 onStop；好友侧通过 [SocialPresencePolicy] 结合 updated 过期判定离线。
 */
object SocialPresenceManager {

    private const val TAG = "SocialPresence"
    private const val HEARTBEAT_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null

    fun onAppForeground(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        scope.launch { setOnline(appCtx, online = true) }
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MS)
                setOnline(appCtx, online = true)
            }
        }
    }

    fun onAppBackground(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.launch { setOnline(ctx.applicationContext, online = false) }
    }

    private suspend fun setOnline(appCtx: Context, online: Boolean) {
        val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }.getOrDefault(0L)
        if (userId <= 0L) return
        if (!SocialSessionManager.isLinked(appCtx, userId)) return

        val cred = SocialOperationGate.acquire(appCtx, userId, forceSession = false).getOrNull()
            ?: SocialOperationGate.acquire(appCtx, userId, forceSession = true).getOrNull()
            ?: run {
                Log.w(TAG, "skip online=$online: no credentials userId=$userId")
                return
            }

        runCatching {
            PocketBaseApiClient(appCtx).updateOnline(cred.token, online)
            Log.d(TAG, "online=$online userId=$userId")
        }.onFailure {
            Log.w(TAG, "updateOnline failed: ${it.message}")
            return
        }

        if (online) {
            runCatching { refreshFriendPresence(appCtx, userId, cred) }
                .onFailure { Log.w(TAG, "refreshFriendPresence failed: ${it.message}") }
        }
    }

    private suspend fun refreshFriendPresence(
        appCtx: Context,
        userId: Long,
        cred: SocialCredentials,
    ) {
        val db = (appCtx as? FunLifeApplication)?.database
            ?: com.example.funlife.data.database.AppDatabase.getDatabase(appCtx)
        val friendsRepo = FriendsRepository(appCtx, db.socialDao(), SocialLinkRepository(appCtx, db.socialDao()))
        friendsRepo.refreshAcceptedFriendsPresence(userId, cred.token)
    }
}
