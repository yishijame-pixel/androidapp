package com.example.funlife.social

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.funlife.utils.UserSessionManager

/**
 * 前台自适应兜底：Realtime 正常时不轮询；SSE 断线/退避时才低频补拉。
 *
 * 企业级常见模式：推送/长连接为主，轮询仅在降级态触发（非固定高频扫库）。
 */
object SocialForegroundPoller {

    private const val TAG = "SocialForegroundPoll"
    /** Realtime 不健康时的补拉间隔（30s，接近微信后台降级体验） */
    private const val DEGRADED_POLL_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    fun onAppForeground(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        pollJob?.cancel()
        // 回前台：事件驱动，立即补拉一次
        SocialInboxSync.syncNowAsync(appCtx, force = true)
        scope.launch {
            runCatching {
                val userId = UserSessionManager(appCtx).getCurrentUserId()
                if (userId > 0L) SocialChatInbound.syncActiveConversations(appCtx, userId)
            }.onFailure { Log.w(TAG, "foreground chat sync failed: ${it.message}") }
        }
        pollJob = scope.launch {
            while (isActive) {
                delay(DEGRADED_POLL_MS)
                if (isRealtimeHealthy()) {
                    Log.d(TAG, "skip poll: realtime live")
                    continue
                }
                Log.d(TAG, "degraded poll: realtime=${SocialSessionManager.snapshot.value.realtime}")
                runCatching {
                    SocialInboxSync.syncNow(appCtx)
                    val userId = UserSessionManager(appCtx).getCurrentUserId()
                    if (userId > 0L) SocialChatInbound.syncActiveConversations(appCtx, userId)
                }
            }
        }
    }

    fun onAppBackground() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun isRealtimeHealthy(): Boolean =
        SocialSessionManager.snapshot.value.realtime == SocialSessionManager.RealtimePhase.LIVE
}
