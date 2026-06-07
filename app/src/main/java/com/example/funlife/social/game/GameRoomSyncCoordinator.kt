package com.example.funlife.social.game

import android.util.Log
import com.example.funlife.social.SocialSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 对局房间同步协调器：合并 Realtime 推送、大厅轮询、全量刷新，避免重复 HTTP 与互相覆盖。
 */
object GameRoomSyncCoordinator {

    private const val TAG = "GameRoomSync"
    private const val DEBOUNCE_MS = 100L
    /** 大厅内 Realtime 离线时的轮询（宾客离座等需房主尽快感知） */
    private const val LOBBY_POLL_OFFLINE_MS = 1_200L
    /** Realtime 在线时大厅兜底 */
    private const val LOBBY_POLL_LIVE_MS = 800L
    /** 房主等待宾客接受 / 刚进大厅 */
    private const val LOBBY_POLL_URGENT_MS = 350L
    private const val LOBBY_URGENT_WINDOW_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val roomRefreshJobs = ConcurrentHashMap<String, Job>()
    private val lobbyWatchJobs = ConcurrentHashMap<String, Job>()
    private var fullRefreshJob: Job? = null
    private val mutatingRooms = ConcurrentHashMap.newKeySet<String>()
    private val pendingRoomRefresh = ConcurrentHashMap<String, suspend () -> Result<Unit>>()

    fun markMutating(roomId: String) {
        mutatingRooms.add(roomId)
    }

    fun clearMutating(roomId: String) {
        mutatingRooms.remove(roomId)
        val pending = pendingRoomRefresh.remove(roomId) ?: return
        scope.launch {
            Log.d(TAG, "flush pending refresh room=$roomId")
            runCatching { pending() }
        }
    }

    fun isMutating(roomId: String): Boolean = roomId in mutatingRooms

    /** Realtime / 变更后：防抖刷新单个房间 */
    fun requestRoomRefresh(roomId: String, block: suspend () -> Result<Unit>) {
        roomRefreshJobs[roomId]?.cancel()
        roomRefreshJobs[roomId] = scope.launch {
            delay(DEBOUNCE_MS)
            if (roomId in mutatingRooms) return@launch
            runCatching { block() }
        }
    }

    /** 全量列表刷新（趣玩中心 / 会话恢复） */
    fun requestFullRefresh(block: suspend () -> Result<Unit>) {
        fullRefreshJob?.cancel()
        fullRefreshJob = scope.launch {
            delay(DEBOUNCE_MS)
            runCatching { block() }
        }
    }

    /** Realtime 邀请等时效路径：不防抖，避免被轮询 cancel 掉 */
    fun requestFullRefreshImmediate(block: suspend () -> Result<Unit>) {
        fullRefreshJob?.cancel()
        fullRefreshJob = scope.launch {
            runCatching { block() }
        }
    }

    /** 单房间即时刷新（接受邀请 / Realtime 推送） */
    fun requestRoomRefreshImmediate(roomId: String, block: suspend () -> Result<Unit>) {
        roomRefreshJobs[roomId]?.cancel()
        roomRefreshJobs[roomId] = scope.launch {
            if (roomId in mutatingRooms) {
                pendingRoomRefresh[roomId] = block
                Log.d(TAG, "defer refresh (mutating) room=$roomId")
                return@launch
            }
            runCatching { block() }
        }
    }

    /**
     * 大厅生命周期内监听房间：Realtime 在线时低频兜底；[urgent] 时 350ms 轮询（等待宾客入座）。
     */
    fun startLobbyWatch(
        roomId: String,
        urgent: Boolean = false,
        block: suspend () -> Result<Unit>,
    ) {
        lobbyWatchJobs[roomId]?.cancel()
        val urgentUntilMs = if (urgent) System.currentTimeMillis() + LOBBY_URGENT_WINDOW_MS else 0L
        lobbyWatchJobs[roomId] = scope.launch {
            if (roomId !in mutatingRooms) {
                runCatching { block() }
            }
            while (isActive) {
                val interval = when {
                    urgentUntilMs > System.currentTimeMillis() -> LOBBY_POLL_URGENT_MS
                    SocialSessionManager.snapshot.value.realtime ==
                        SocialSessionManager.RealtimePhase.LIVE -> LOBBY_POLL_LIVE_MS
                    else -> LOBBY_POLL_OFFLINE_MS
                }
                delay(interval)
                if (roomId !in mutatingRooms) {
                    requestRoomRefreshImmediate(roomId, block)
                }
            }
        }
    }

    fun stopLobbyWatch(roomId: String) {
        lobbyWatchJobs.remove(roomId)?.cancel()
        roomRefreshJobs.remove(roomId)?.cancel()
    }

    /**
     * 离房/拒邀等：先导航再 PATCH。必须在应用级 scope 执行，否则 Nav 弹出会 cancel ViewModel 导致服务端未更新。
     */
    fun runAfterNavigate(
        tag: String,
        block: suspend () -> Result<Unit>,
        onResult: suspend (Result<Unit>) -> Unit = {},
    ) {
        scope.launch {
            Log.i(TAG, "bg mutation start: $tag")
            val result = runCatching { block() }.getOrElse { Result.failure(it) }
            Log.i(TAG, "bg mutation done: $tag ok=${result.isSuccess} err=${result.exceptionOrNull()?.message}")
            withContext(Dispatchers.Main) { onResult(result) }
        }
    }
}
