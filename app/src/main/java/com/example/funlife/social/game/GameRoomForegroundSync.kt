package com.example.funlife.social.game

import android.content.Context
import android.util.Log
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 应用级对局房间同步：首页/任意 Tab 都能收到游戏邀请，不依赖 [GameCenterViewModel] 是否已创建。
 */
object GameRoomForegroundSync {

    private const val TAG = "GameRoomFgSync"
    private const val POLL_LIVE_MS = 10_000L
    private const val POLL_OFFLINE_MS = 3_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    fun onAppForeground(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        scope.launch { refreshNow(appCtx, lightweight = false) }
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val live = SocialSessionManager.snapshot.value.realtime ==
                    SocialSessionManager.RealtimePhase.LIVE
                delay(if (live) POLL_LIVE_MS else POLL_OFFLINE_MS)
                refreshNow(appCtx, lightweight = live)
            }
        }
    }

    fun onAppBackground() {
        pollJob?.cancel()
        pollJob = null
    }

    fun refreshNowAsync(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        scope.launch {
            val live = SocialSessionManager.snapshot.value.realtime ==
                SocialSessionManager.RealtimePhase.LIVE
            refreshNow(ctx.applicationContext, lightweight = live)
        }
    }

    private suspend fun refreshNow(appCtx: Context, lightweight: Boolean) {
        val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }.getOrDefault(0L)
        if (userId <= 0L) return
        GameRoomSyncCoordinator.requestFullRefresh {
            val interactor = GameRoomInteractor(appCtx, userId)
            val result = if (lightweight) {
                interactor.refreshIncomingInvitesOnly()
            } else {
                interactor.refreshRoomsForDelivery()
            }
            if (result.isFailure) {
                Log.w(TAG, "refreshRooms failed: ${result.exceptionOrNull()?.message}")
            } else {
                runCatching { GameInviteNotifier.publishNewInvites(appCtx, userId) }
                    .onFailure { Log.w(TAG, "publish invites failed: ${it.message}") }
                Log.d(TAG, "refreshRooms ok userId=$userId")
            }
            result
        }
    }
}
