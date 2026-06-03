package com.example.funlife.social

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.UserSession
import com.example.funlife.notifications.FriendRequestExpeditedWorker
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.model.SocialLinkState
import com.example.funlife.utils.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 企业级社交会话中枢：绑定、Realtime、收件箱同步、网络恢复统一调度。
 * UI / Worker / 通知层均通过此入口，避免多处重复 bootstrap 与 Mutex 死锁。
 */
object SocialSessionManager {

    private const val TAG = "SocialSession"
    private const val BIND_TIMEOUT_MS = 20_000L

    enum class SessionPhase {
        NOT_CONFIGURED,
        IDLE,
        LINKING,
        READY,
        ERROR,
    }

    enum class RealtimePhase {
        OFF,
        CONNECTING,
        LIVE,
        BACKOFF,
    }

    data class Snapshot(
        val phase: SessionPhase = SessionPhase.IDLE,
        val realtime: RealtimePhase = RealtimePhase.OFF,
        val userId: Long = 0L,
        val pbRecordId: String? = null,
        val lastSyncAtMs: Long = 0L,
        val errorMessage: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val linkMutex = Mutex()
    private var linkRetryJob: Job? = null
    private val _snapshot = MutableStateFlow(initialSnapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    private val _linkState = MutableStateFlow(toLinkState(_snapshot.value))
    val linkState: StateFlow<SocialLinkState> = _linkState.asStateFlow()

    private fun initialSnapshot(): Snapshot {
        if (!PocketBaseConfig.isEnabled()) {
            return Snapshot(phase = SessionPhase.NOT_CONFIGURED)
        }
        return Snapshot(phase = SessionPhase.IDLE)
    }

    fun toLinkState(s: Snapshot): SocialLinkState = when (s.phase) {
        SessionPhase.NOT_CONFIGURED -> SocialLinkState.NotConfigured
        SessionPhase.LINKING -> SocialLinkState.Linking
        SessionPhase.READY -> SocialLinkState.Linked(s.pbRecordId.orEmpty())
        SessionPhase.ERROR -> SocialLinkState.Error(s.errorMessage ?: "社交服务异常")
        SessionPhase.IDLE -> when {
            !s.pbRecordId.isNullOrBlank() -> SocialLinkState.Linked(s.pbRecordId)
            PocketBaseConfig.isEnabled() -> SocialLinkState.Linking
            else -> SocialLinkState.NotConfigured
        }
    }

    fun warmStartAsync(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        scope.launch { warmStart(ctx) }
    }

    /** 登录 / 冷启动：优先本地缓存，再启动推送栈 */
    suspend fun warmStart(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        val session = UserSessionManager(appCtx).getSession() ?: return
        val userId = session.userId
        if (userId <= 0L) return

        if (isLinked(appCtx, userId)) {
            hydrateFromDisk(appCtx, userId)
            startPushStack(appCtx)
            SocialInboxSync.syncNow(appCtx, force = true)
            SocialPushTokenRegistry.syncToServerAsync(appCtx, userId)
            return
        }

        updateSnapshot { it.copy(phase = SessionPhase.LINKING, userId = userId) }
        ensureSession(appCtx)
    }

    /** 阻塞式绑定（好友页首次进入等） */
    suspend fun ensureSession(
        ctx: Context,
        timeoutMs: Long = BIND_TIMEOUT_MS,
        scheduleRetryOnFailure: Boolean = true,
    ): Boolean {
        if (!PocketBaseConfig.isEnabled()) return false
        val appCtx = ctx.applicationContext
        val session = UserSessionManager(appCtx).getSession() ?: return false

        if (isLinked(appCtx, session.userId)) {
            hydrateFromDisk(appCtx, session.userId)
            startPushStack(appCtx)
            return true
        }

        updateSnapshot {
            it.copy(phase = SessionPhase.LINKING, userId = session.userId, errorMessage = null)
        }

        val linked = linkMutex.withLock {
            withTimeoutOrNull(timeoutMs) {
                ensureLinkOnly(appCtx, session)
            } ?: false
        }

        if (linked) {
            hydrateFromDisk(appCtx, session.userId)
            startPushStack(appCtx)
            SocialInboxSync.syncNow(appCtx, force = true)
            SocialPushTokenRegistry.syncToServerAsync(appCtx, session.userId)
            cancelLinkRetry()
            return true
        }

        val msg = "社交账号绑定失败\n请确认 PocketBase 已启动且手机能访问服务器"
        updateSnapshot {
            it.copy(phase = SessionPhase.ERROR, userId = session.userId, errorMessage = msg)
        }
        if (scheduleRetryOnFailure) scheduleLinkRetry(appCtx)
        return false
    }

    suspend fun isLinked(ctx: Context, userId: Long): Boolean {
        if (userId <= 0L) return false
        val snap = _snapshot.value
        if (snap.userId == userId && snap.phase == SessionPhase.READY) return true
        val appCtx = ctx.applicationContext
        val db = (appCtx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(appCtx)
        val link = db.socialDao().getLink(userId) ?: return false
        return !SocialSecureStore.getToken(appCtx, userId).isNullOrBlank() &&
            link.pbRecordId.isNotBlank()
    }

    /** 好友页首帧：从 Room 恢复会话快照（不触发网络） */
    suspend fun hydrateForUser(ctx: Context, userId: Long) {
        if (userId <= 0L || !isLinked(ctx, userId)) return
        hydrateFromDisk(ctx.applicationContext, userId)
    }

    suspend fun getValidToken(ctx: Context, userId: Long): String? {
        return SocialOperationGate.peek(ctx, userId)?.token
            ?: SocialOperationGate.acquire(ctx, userId, forceSession = false).getOrNull()?.token
    }

    /** 企业级凭证门禁（Worker / 通知 / UI 统一入口） */
    suspend fun acquireCredentials(
        ctx: Context,
        userId: Long,
        forceSession: Boolean = true,
    ): Result<SocialCredentials> = SocialOperationGate.acquire(ctx, userId, forceSession)

    fun onRealtimePhase(phase: RealtimePhase) {
        updateSnapshot { it.copy(realtime = phase) }
    }

    fun onSyncCompleted() {
        updateSnapshot { it.copy(lastSyncAtMs = System.currentTimeMillis()) }
    }

    fun onNetworkRestored(ctx: Context) {
        if (!PocketBaseConfig.isEnabled()) return
        val appCtx = ctx.applicationContext
        scope.launch {
            Log.d(TAG, "network restored — restart realtime + force sync")
            FriendRealtimeHub.restart(appCtx)
            SocialInboxSync.syncNow(appCtx, force = true)
            FriendRequestExpeditedWorker.enqueue(appCtx)
        }
    }

    /** 登出：停止推送栈并重置内存态 */
    fun shutdown(ctx: Context) {
        cancelLinkRetry()
        stopPushStack(ctx)
        updateSnapshot {
            Snapshot(phase = if (PocketBaseConfig.isEnabled()) SessionPhase.IDLE else SessionPhase.NOT_CONFIGURED)
        }
    }

    private suspend fun hydrateFromDisk(appCtx: Context, userId: Long) {
        val db = (appCtx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(appCtx)
        val link = db.socialDao().getLink(userId) ?: return
        updateSnapshot {
            it.copy(
                phase = SessionPhase.READY,
                userId = userId,
                pbRecordId = link.pbRecordId,
                errorMessage = null,
            )
        }
    }

    private suspend fun ensureLinkOnly(ctx: Context, session: UserSession): Boolean {
        val userId = session.userId
        if (userId <= 0L) return false
        val displayName = session.nickname.ifBlank { session.username }
        if (displayName.isBlank() || session.username.isBlank()) {
            Log.w(TAG, "skip link: empty username")
            return false
        }
        if (isLinked(ctx, userId)) {
            Log.d(TAG, "already linked userId=$userId")
            return true
        }
        val db = (ctx as? FunLifeApplication)?.database ?: AppDatabase.getDatabase(ctx)
        val linkRepo = SocialLinkRepository(ctx, db.socialDao())
        return linkRepo.ensureLinked(userId, session.username, displayName).fold(
            onSuccess = {
                Log.d(TAG, "linked userId=$userId pb=${it.pbRecordId}")
                true
            },
            onFailure = { e ->
                Log.w(TAG, "link failed: ${e.message}")
                false
            },
        )
    }

    private fun startPushStack(ctx: Context) {
        val appCtx = ctx.applicationContext
        FriendRealtimeHub.restart(appCtx)
        SocialNetworkMonitor.register(appCtx)
        FriendRequestExpeditedWorker.enqueue(appCtx)
    }

    private fun stopPushStack(ctx: Context) {
        FriendRealtimeHub.stop()
        SocialNetworkMonitor.unregister(ctx.applicationContext)
        SocialForegroundPoller.onAppBackground()
    }

    /** 绑定失败时后台重试，避免用户未进好友页就永远收不到推送 */
    private fun scheduleLinkRetry(appCtx: Context) {
        linkRetryJob?.cancel()
        linkRetryJob = scope.launch {
            repeat(6) { attempt ->
                delay(if (attempt == 0) 3_000L else 15_000L)
                val session = UserSessionManager(appCtx).getSession() ?: return@launch
                if (isLinked(appCtx, session.userId)) {
                    warmStart(appCtx)
                    return@launch
                }
                Log.d(TAG, "link retry #${attempt + 1}")
                if (ensureSession(appCtx, timeoutMs = 15_000, scheduleRetryOnFailure = false)) return@launch
            }
        }
    }

    private fun cancelLinkRetry() {
        linkRetryJob?.cancel()
        linkRetryJob = null
    }

    private inline fun updateSnapshot(block: (Snapshot) -> Snapshot) {
        _snapshot.value = block(_snapshot.value)
        _linkState.value = toLinkState(_snapshot.value)
    }
}
