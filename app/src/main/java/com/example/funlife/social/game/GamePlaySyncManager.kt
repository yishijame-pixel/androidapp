package com.example.funlife.social.game

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.GameMoveRepository
import com.example.funlife.repository.GameRoomRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.PocketBaseConnectionWarmer
import com.example.funlife.social.PocketBaseRealtimeClient
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.social.game.engine.DrawGuessSync
import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.GameRoomStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

/**
 * 对局实时同步管理器
 *
 * v3 策略：
 * - Realtime move → 即时 MoveEvent（0 HTTP），PLAYING 期间不再拉 meta
 * - Realtime room → 全量并行 fetch（状态/终局）
 * - 离线轮询 → 增量 moves + lite room 并行（1.2s）
 */
object GamePlaySyncManager {

    private const val TAG = "GamePlaySync"
    private const val RECONNECT_BASE_MS = 1_500L
    private const val RECONNECT_MAX_MS = 20_000L
    private const val FALLBACK_POLL_INTERVAL_MS = 1_500L
    private const val OFFLINE_POLL_INTERVAL_MS = 1_200L
    private const val FULL_SYNC_DEBOUNCE_MS = 80L
    private const val MIN_POLL_GAP_MS = 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()

    private var activeSession: PlaySession? = null

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _moveEvents = MutableSharedFlow<MoveEvent>(replay = 1, extraBufferCapacity = 32)
    val moveEvents: SharedFlow<MoveEvent> = _moveEvents.asSharedFlow()

    private val _roomEvents = MutableSharedFlow<RoomEvent>(replay = 1, extraBufferCapacity = 8)
    val roomEvents: SharedFlow<RoomEvent> = _roomEvents.asSharedFlow()

    fun startSession(
        ctx: Context,
        userId: Long,
        roomId: String,
        onMoveReceived: (GameMoveDto) -> Unit = {},
        onRoomUpdated: (GameRoomDto) -> Unit = {},
    ) {
        if (!PocketBaseConfig.isEnabled()) {
            Log.w(TAG, "PocketBase not configured, using poll-only mode")
        }

        scope.launch {
            sessionMutex.withLock {
                if (activeSession?.roomId == roomId && activeSession?.isActive == true) {
                    Log.d(TAG, "Session for $roomId already active")
                    return@launch
                }

                activeSession?.stop()

                val session = PlaySession(
                    ctx = ctx.applicationContext,
                    userId = userId,
                    roomId = roomId,
                    onMoveReceived = onMoveReceived,
                    onRoomUpdated = onRoomUpdated,
                )
                activeSession = session
                session.start()
            }
        }
    }

    fun stopSession(roomId: String? = null) {
        scope.launch {
            sessionMutex.withLock {
                if (roomId == null || activeSession?.roomId == roomId) {
                    activeSession?.stop()
                    activeSession = null
                    _syncState.value = SyncState.IDLE
                }
            }
        }
    }

    fun requestRefresh() {
        scope.launch {
            activeSession?.requestRefresh()
        }
    }

    /** 大厅满员时预连 Realtime + 预热 HTTP，开局少等 0.5~1s */
    fun prewarmSession(ctx: Context, userId: Long, roomId: String) {
        PocketBaseConnectionWarmer.warmAsync(ctx.applicationContext)
        startSession(ctx, userId, roomId)
    }

    /** 主 SSE（FriendRealtimeHub）推送落子 → 当前对局 session，避免第二条 Realtime 连接 */
    fun dispatchMove(move: GameMoveDto) {
        val session = activeSession ?: return
        if (move.roomId != session.roomId) return
        Log.d(TAG, "dispatchMove hub -> session #${move.moveIndex} room=${move.roomId}")
        scope.launch { session.ingestMove(move) }
    }

    /** 主 SSE 推送房间变更 → 当前对局 session */
    fun dispatchRoomUpdate(roomId: String) {
        val session = activeSession ?: return
        if (roomId != session.roomId) return
        scope.launch { session.ingestRoomSignal() }
    }

    private class PlaySession(
        private val ctx: Context,
        val userId: Long,
        val roomId: String,
        private val onMoveReceived: (GameMoveDto) -> Unit,
        private val onRoomUpdated: (GameRoomDto) -> Unit,
    ) {
        private val db = (ctx as? FunLifeApplication)?.database
            ?: com.example.funlife.data.database.AppDatabase.getDatabase(ctx)
        private val socialDao = db.socialDao()
        private val linkRepo = SocialLinkRepository(ctx, socialDao)
        private val roomRepo = GameRoomRepository(ctx, socialDao, linkRepo)
        private val moveRepo = GameMoveRepository(ctx, socialDao, roomRepo)
        private val api = com.example.funlife.social.PocketBaseApiClient(ctx)

        private var realtimeJob: Job? = null
        private var pollJob: Job? = null
        private var reconnectBackoff = RECONNECT_BASE_MS
        private var lastKnownMoveIndex = 0
        private var lastPollRefreshAtMs = 0L
        private var fullSyncJob: Job? = null
        private val refreshChannel = Channel<Unit>(Channel.CONFLATED)

        val isActive: Boolean get() = realtimeJob?.isActive == true || pollJob?.isActive == true

        fun start() {
            startRealtime()
            startFallbackPoll()
            startRefreshListener()
            scope.launch {
                runCatching {
                    val token = linkRepo.getValidToken(userId) ?: return@launch
                    refreshRoomAndMoves(token, silent = true)
                }
            }
        }

        fun stop() {
            realtimeJob?.cancel()
            pollJob?.cancel()
            fullSyncJob?.cancel()
            realtimeJob = null
            pollJob = null
            fullSyncJob = null
        }

        fun requestRefresh() {
            refreshChannel.trySend(Unit)
        }

        private fun startRealtime() {
            if (!PocketBaseConfig.isEnabled()) return

            realtimeJob = scope.launch {
                while (isActive) {
                    if (SocialSessionManager.snapshot.value.realtime ==
                        SocialSessionManager.RealtimePhase.LIVE
                    ) {
                        Log.d(TAG, "realtime: hub LIVE, skip dedicated SSE room=$roomId")
                        _syncState.value = SyncState.CONNECTED
                        while (isActive &&
                            SocialSessionManager.snapshot.value.realtime ==
                            SocialSessionManager.RealtimePhase.LIVE
                        ) {
                            delay(500)
                        }
                        continue
                    }
                    try {
                        _syncState.value = SyncState.CONNECTING
                        runDedicatedRealtimeSession()
                        reconnectBackoff = RECONNECT_BASE_MS
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Dedicated realtime failed: ${e.message}")
                        _syncState.value = SyncState.RECONNECTING
                        delay(reconnectBackoff)
                        reconnectBackoff = min(reconnectBackoff * 2, RECONNECT_MAX_MS)
                    }
                }
            }
        }

        suspend fun ingestMove(move: GameMoveDto) {
            val isDrawStroke = DrawGuessSync.isDrawStrokeMove(move)
            if (!isDrawStroke && GameRoomSyncCoordinator.isMutating(roomId)) {
                Log.d(TAG, "ingestMove skipped (mutating) #${move.moveIndex}")
                return
            }
            if (move.moveIndex > lastKnownMoveIndex) {
                lastKnownMoveIndex = move.moveIndex
            }
            Log.d(TAG, "ingestMove #${move.moveIndex} room=$roomId watermark=$lastKnownMoveIndex")
            _moveEvents.emit(MoveEvent(roomId, move))
            onMoveReceived(move)
        }

        suspend fun ingestRoomSignal() {
            if (GameRoomSyncCoordinator.isMutating(roomId)) return
            val token = linkRepo.getValidToken(userId) ?: return
            scheduleFullSync(token)
        }

        private suspend fun runDedicatedRealtimeSession() {
            val link = socialDao.getLink(userId) ?: return
            val token = linkRepo.getValidToken(userId) ?: return
            val myPbId = link.pbRecordId

            val realtime = PocketBaseRealtimeClient()
            _syncState.value = SyncState.CONNECTED

            realtime.listenPlay(
                authToken = token,
                myPbId = myPbId,
                roomId = roomId,
                onGameRoom = { incomingRoomId ->
                    if (incomingRoomId == roomId && !GameRoomSyncCoordinator.isMutating(roomId)) {
                        scheduleFullSync(token)
                    }
                },
                onGameMove = { move ->
                    scope.launch { ingestMove(move) }
                },
            )
        }

        private fun startFallbackPoll() {
            pollJob = scope.launch {
                while (isActive) {
                    val interval = if (_syncState.value == SyncState.CONNECTED) {
                        FALLBACK_POLL_INTERVAL_MS
                    } else {
                        OFFLINE_POLL_INTERVAL_MS
                    }
                    delay(interval)

                    if (GameRoomSyncCoordinator.isMutating(roomId)) continue
                    if (System.currentTimeMillis() - lastPollRefreshAtMs < MIN_POLL_GAP_MS) continue

                    try {
                        val token = linkRepo.getValidToken(userId) ?: continue
                        refreshIncremental(token, silent = true)
                        lastPollRefreshAtMs = System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w(TAG, "Poll refresh failed: ${e.message}")
                    }
                }
            }
        }

        private fun startRefreshListener() {
            scope.launch {
                for (unit in refreshChannel) {
                    try {
                        val token = linkRepo.getValidToken(userId) ?: continue
                        refreshRoomAndMoves(token)
                        lastPollRefreshAtMs = System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.w(TAG, "Manual refresh failed: ${e.message}")
                    }
                }
            }
        }

        private fun scheduleFullSync(token: String) {
            fullSyncJob?.cancel()
            fullSyncJob = scope.launch {
                delay(FULL_SYNC_DEBOUNCE_MS)
                refreshRoomAndMoves(token, silent = true)
            }
        }

        private suspend fun refreshRoomAndMoves(token: String, silent: Boolean = false) {
            try {
                val (room, moves) = api.fetchPlayState(token, roomId, includeProfiles = true)
                applyMovesWatermark(moves)
                _roomEvents.emit(RoomEvent(roomId, room, moves))
                onRoomUpdated(room)
            } catch (e: Exception) {
                if (!silent) throw e
                Log.w(TAG, "Silent refresh failed: ${e.message}")
            }
        }

        /** 轮询/对账：lite room + 增量 moves 并行；终局时回退全量。 */
        private suspend fun refreshIncremental(token: String, silent: Boolean = true) {
            try {
                val (room, deltaMoves, incremental) = api.fetchPlayStateDelta(
                    token, roomId, lastKnownMoveIndex,
                )
                if (!incremental) {
                    applyMovesWatermark(deltaMoves)
                    _roomEvents.emit(RoomEvent(roomId, room, deltaMoves))
                    onRoomUpdated(room)
                    return
                }
                if (deltaMoves.isNotEmpty()) {
                    applyMovesWatermark(deltaMoves)
                    _roomEvents.emit(RoomEvent(roomId, room, deltaMoves))
                    onRoomUpdated(room)
                    return
                }
                if (room.status != GameRoomStatus.PLAYING) {
                    refreshRoomAndMoves(token, silent = silent)
                    return
                }
                _roomEvents.emit(RoomEvent(roomId, room, emptyList()))
                onRoomUpdated(room)
            } catch (e: Exception) {
                if (!silent) throw e
                Log.w(TAG, "Incremental refresh failed: ${e.message}")
            }
        }

        private fun applyMovesWatermark(moves: List<GameMoveDto>) {
            val maxIndex = moves.maxOfOrNull { it.moveIndex } ?: 0
            if (maxIndex > lastKnownMoveIndex) {
                lastKnownMoveIndex = maxIndex
            }
        }
    }
}

enum class SyncState {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    POLL_ONLY,
}

data class MoveEvent(
    val roomId: String,
    val move: GameMoveDto,
)

data class RoomEvent(
    val roomId: String,
    val room: GameRoomDto,
    val moves: List<GameMoveDto> = emptyList(),
)
