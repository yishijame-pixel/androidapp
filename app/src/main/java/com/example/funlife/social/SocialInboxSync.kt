package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.notifications.FriendRequestNotifier
import com.example.funlife.notifications.InboxStore
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 首页/后台统一拉取好友待办并刷新铃铛红点（不依赖用户打开「好友」页）。
 * 带节流，避免多处铃铛/生命周期重复触发导致连发通知。
 */
object SocialInboxSync {

    private const val TAG = "SocialInboxSync"
    /** 非 force 同步的最小间隔（防多处触发重复请求） */
    private const val MIN_INTERVAL_MS = 30_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    @Volatile private var lastSyncAtMs = 0L

    fun syncNowAsync(ctx: Context, force: Boolean = false) {
        if (!PocketBaseConfig.isEnabled()) return
        val now = System.currentTimeMillis()
        if (!force && now - lastSyncAtMs < MIN_INTERVAL_MS) return
        scope.launch {
            runCatching { syncNow(ctx, force) }
                .onFailure { Log.w(TAG, "sync failed: ${it.message}") }
        }
    }

    suspend fun syncNow(ctx: Context, force: Boolean = false) {
        if (!PocketBaseConfig.isEnabled()) return
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && now - lastSyncAtMs < MIN_INTERVAL_MS) return
            lastSyncAtMs = now

            val appCtx = ctx.applicationContext
            val userId = runCatching { UserSessionManager(appCtx).getCurrentUserId() }.getOrDefault(0L)
            if (userId <= 0L) return

            if (!SocialSessionManager.isLinked(appCtx, userId)) {
                withTimeoutOrNull(15_000) { SocialSessionManager.ensureSession(appCtx) }
            }
            runCatching { FriendRequestNotifier.pollOnce(appCtx) }
                .onFailure { Log.w(TAG, "pollOnce failed: ${it.message}") }
            withContext(Dispatchers.Main) {
                InboxStore.refreshUnread(appCtx)
            }
            SocialSessionManager.onSyncCompleted()
            Log.d(TAG, "sync done userId=$userId unread=${InboxStore.unreadCount(appCtx)}")
        }
    }
}
