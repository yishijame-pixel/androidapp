package com.example.funlife.social.drawws

import android.util.Log
import com.example.funlife.social.game.engine.DrawGuessSync
import com.example.funlife.viewmodel.DrawStrokeUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 你画我猜笔画热路径：WebSocket 广播 + 本地合并。
 * 冷路径归档（stroke_end → PB game_moves）由 ViewModel 负责。
 */
object DrawGuessLiveSync {

    private const val TAG = "DrawGuessLiveSync"
    private const val PUBLISH_INTERVAL_MS = 4L
    private const val TX_COALESCE_MS = 4L

    enum class Transport { POCKETBASE, WEBSOCKET }

    private var session: DrawWsSession? = null
    private var collectJob: Job? = null
    private var publishJob: Job? = null
    private var txFlushJob: Job? = null
    private var syncScope: CoroutineScope? = null
    private var activeRoomId: String? = null
    @Volatile
    private var activeRound: Int = 1
    @Volatile
    private var drawerPbId: String = ""
    private val strokeSeq = AtomicInteger(0)
    private val chunkByStroke = ConcurrentHashMap<String, AtomicInteger>()

    private val _transport = MutableStateFlow(Transport.POCKETBASE)
    val transport: StateFlow<Transport> = _transport.asStateFlow()

    private val _roomSync = MutableStateFlow(DrawWsRoomSync())
    val roomSync: StateFlow<DrawWsRoomSync> = _roomSync.asStateFlow()

    fun isRoomGoForRound(round: Int): Boolean =
        _roomSync.value.goAtMs > 0L && _roomSync.value.goRound == round.coerceAtLeast(1)

    fun resetRoomGoForRound(round: Int) {
        _roomSync.value = DrawWsRoomSync(
            peerCount = _roomSync.value.peerCount,
            readyCount = _roomSync.value.readyCount,
            expectedPeers = _roomSync.value.expectedPeers,
            goAtMs = 0L,
            goRound = round.coerceAtLeast(1),
        )
    }

    private val strokeLock = Any()
    private val strokeById = ConcurrentHashMap<String, DrawStrokeUi>()
    private val strokeRxAtMs = ConcurrentHashMap<String, Long>()
    private val finalizedStrokeIds = ConcurrentHashMap.newKeySet<String>()
    private val endedStrokeIds = ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var publishDirty = false

    private data class TxBatch(
        val strokeId: String,
        val round: Int,
        var color: String,
        var width: Float,
        val points: MutableList<List<Float>> = mutableListOf(),
    )

    private val txBatchByStroke = ConcurrentHashMap<String, TxBatch>()

    fun strokeReceivedAt(strokeId: String): Long =
        strokeRxAtMs[strokeId]?.takeIf { strokeId.isNotBlank() } ?: 0L

    fun isStrokeFinalized(strokeId: String?): Boolean =
        !strokeId.isNullOrBlank() && finalizedStrokeIds.contains(strokeId)

    fun isStrokeEnded(strokeId: String?): Boolean =
        !strokeId.isNullOrBlank() && endedStrokeIds.contains(strokeId)

    private val _strokeFinalizeNonce = MutableStateFlow(0)
    val strokeFinalizeNonce: StateFlow<Int> = _strokeFinalizeNonce.asStateFlow()

    fun configureRoomContext(round: Int, drawerId: String) {
        val nextRound = round.coerceAtLeast(1)
        if (nextRound != activeRound) {
            activeRound = nextRound
            resetRoomGoForRound(nextRound)
            clearLiveStrokes()
            strokeAccumulator.clear()
            chunkByStroke.clear()
            txBatchByStroke.clear()
            _clearNonce.value += 1
        }
        drawerPbId = drawerId.trim()
    }

    fun prewarm(
        scope: CoroutineScope,
        roomId: String,
        token: String,
        round: Int = 1,
        drawerId: String = "",
    ) {
        start(scope, roomId, token, round, drawerId)
    }

    fun stopIfRoom(roomId: String) {
        if (activeRoomId == roomId) stop()
    }

    private val _liveStrokes = MutableStateFlow<List<DrawStrokeUi>>(emptyList())
    val liveStrokes: StateFlow<List<DrawStrokeUi>> = _liveStrokes.asStateFlow()

    private val _clearNonce = MutableStateFlow(0)
    val clearNonce: StateFlow<Int> = _clearNonce.asStateFlow()

    private fun markPublishDirty(flushNow: Boolean = false) {
        publishDirty = true
        if (flushNow) {
            publishJob?.cancel()
            flushLiveStrokesNow()
            return
        }
        val scope = syncScope ?: run {
            flushLiveStrokesNow()
            return
        }
        if (publishJob?.isActive == true) return
        publishJob = scope.launch {
            delay(PUBLISH_INTERVAL_MS)
            flushLiveStrokesNow()
        }
    }

    private fun flushLiveStrokesNow() {
        if (!publishDirty) return
        publishDirty = false
        synchronized(strokeLock) {
            _liveStrokes.value = strokeById.values.toList()
        }
    }

    fun dropLiveStroke(strokeId: String) {
        if (strokeId.isBlank()) return
        synchronized(strokeLock) {
            if (strokeById.remove(strokeId) != null) markPublishDirty()
        }
        strokeAccumulator.remove(strokeId)
        chunkByStroke.remove(strokeId)
        strokeRxAtMs.remove(strokeId)
        txBatchByStroke.remove(strokeId)
    }

    private fun upsertLiveStroke(
        strokeId: String,
        color: String,
        width: Float,
        points: List<Pair<Float, Float>>,
        replace: Boolean = false,
        flushNow: Boolean = false,
    ) {
        synchronized(strokeLock) {
            val seq = strokeSeqKey(strokeId)
            val old = strokeById[strokeId]
            val mergedPoints = when {
                replace && points.isNotEmpty() -> {
                    when {
                        old == null -> points
                        points.size >= old.points.size -> points
                        else -> old.points
                    }
                }
                old == null -> points
                points.isEmpty() -> old.points
                else -> listOf(
                    old.points,
                    points,
                    DrawGuessSync.mergeStrokePoints(old.points, points),
                    DrawGuessSync.mergeStrokePoints(points, old.points),
                ).maxByOrNull { it.size } ?: points
            }
            strokeById[strokeId] = DrawStrokeUi(
                seq = seq,
                points = mergedPoints,
                color = color,
                width = width,
                strokeId = strokeId,
            )
            markPublishDirty(flushNow = flushNow)
        }
    }

    private val strokeAccumulator = ConcurrentHashMap<String, MutableList<Pair<Float, Float>>>()

    private fun clearLiveStrokes() {
        synchronized(strokeLock) {
            strokeById.clear()
            publishDirty = false
            _liveStrokes.value = emptyList()
        }
        strokeRxAtMs.clear()
        finalizedStrokeIds.clear()
        endedStrokeIds.clear()
    }

    fun isConnected(roomId: String): Boolean =
        session != null && activeRoomId == roomId && _transport.value == Transport.WEBSOCKET

    fun isPending(roomId: String): Boolean =
        session != null && activeRoomId == roomId && !isConnected(roomId)

    /** 会话已建立：含 TCP 已连 / 鉴权中（出站会入队直到 joined） */
    fun canSendChunks(): Boolean =
        DrawWsConfig.isEnabled() && session != null

    fun start(
        scope: CoroutineScope,
        roomId: String,
        token: String,
        round: Int = 1,
        drawerId: String = "",
    ) {
        configureRoomContext(round, drawerId)
        val scopeChanged = syncScope !== scope
        syncScope = scope
        if (session != null && activeRoomId == roomId && (isConnected(roomId) || isPending(roomId))) {
            if (scopeChanged) attachEventCollector(scope)
            return
        }
        stop()
        if (!DrawWsConfig.isEnabled() || token.isBlank()) {
            _transport.value = Transport.POCKETBASE
            return
        }
        activeRoomId = roomId
        activeRound = round.coerceAtLeast(1)
        drawerPbId = drawerId.trim()
        val ws = DrawWsSession(scope, roomId, token)
        session = ws
        attachEventCollector(scope)
        ws.start()
    }

    private fun attachEventCollector(scope: CoroutineScope) {
        collectJob?.cancel()
        val ws = session ?: return
        collectJob = scope.launch {
            ws.events.collect { handleWsEvent(it) }
        }
    }

    private fun handleWsEvent(event: DrawWsEvent) {
        when (event) {
            is DrawWsEvent.Joined -> {
                _transport.value = Transport.WEBSOCKET
                _roomSync.value = DrawWsRoomSync(
                    peerCount = event.peerCount,
                    readyCount = event.readyCount,
                    expectedPeers = event.expectedPeers,
                    goAtMs = _roomSync.value.goAtMs,
                    goRound = _roomSync.value.goRound,
                )
                flushAllTxNow()
                flushLiveStrokesNow()
                Log.i(TAG, "joined room=${event.roomId} peers=${event.peerCount} round=$activeRound")
            }
            is DrawWsEvent.StrokeChunk -> {
                markTransportLive()
                mergeIncoming(event.data, finalize = false)
            }
            is DrawWsEvent.StrokeEnd -> {
                markTransportLive()
                mergeIncoming(event.data, finalize = true)
            }
            is DrawWsEvent.Clear -> {
                clearLiveStrokes()
                strokeAccumulator.clear()
                txBatchByStroke.clear()
                _clearNonce.value += 1
            }
            is DrawWsEvent.Replay, is DrawWsEvent.Snapshot -> {
                markTransportLive()
                val items = when (event) {
                    is DrawWsEvent.Replay -> event.events
                    is DrawWsEvent.Snapshot -> event.events
                    else -> emptyList()
                }
                items.forEach { replay ->
                    when (replay) {
                        is DrawWsEvent.StrokeChunk -> mergeIncoming(replay.data, finalize = false)
                        is DrawWsEvent.StrokeEnd -> mergeIncoming(replay.data, finalize = true)
                        is DrawWsEvent.Clear -> clearLiveStrokes()
                        else -> Unit
                    }
                }
                flushLiveStrokesNow()
            }
            is DrawWsEvent.RoomState -> {
                _roomSync.value = _roomSync.value.copy(
                    peerCount = event.peerCount,
                    readyCount = event.readyCount,
                    expectedPeers = event.expectedPeers,
                )
            }
            is DrawWsEvent.RoomGo -> {
                markTransportLive()
                _roomSync.value = _roomSync.value.copy(
                    goAtMs = event.serverTs,
                    goRound = event.round,
                )
                Log.d(TAG, "room_go round=${event.round} ts=${event.serverTs}")
            }
            is DrawWsEvent.Disconnected -> _transport.value = Transport.POCKETBASE
            is DrawWsEvent.Error -> Log.w(TAG, "ws error ${event.code}")
        }
    }

    /** 进盘 bootstrap 完成后上报 ready，等待 room_go */
    fun sendReady(round: Int = activeRound): Boolean {
        val ws = session ?: return false
        return ws.send(DrawWsProtocol.ready(round.coerceAtLeast(1)))
    }

    private fun markTransportLive() {
        if (_transport.value != Transport.WEBSOCKET) {
            _transport.value = Transport.WEBSOCKET
        }
    }

    fun stop() {
        collectJob?.cancel()
        publishJob?.cancel()
        txFlushJob?.cancel()
        session?.stop()
        session = null
        collectJob = null
        publishJob = null
        txFlushJob = null
        syncScope = null
        activeRoomId = null
        _transport.value = Transport.POCKETBASE
        _roomSync.value = DrawWsRoomSync()
        clearLiveStrokes()
        strokeAccumulator.clear()
        chunkByStroke.clear()
        strokeRxAtMs.clear()
        txBatchByStroke.clear()
    }

    fun isWsActive(): Boolean =
        DrawWsConfig.isEnabled() && session != null && _transport.value == Transport.WEBSOCKET

    fun newStrokeId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    fun sendChunk(
        roomId: String,
        strokeId: String,
        round: Int,
        color: String,
        width: Float,
        points: List<List<Float>>,
        flushNow: Boolean = false,
    ): Boolean {
        val ws = session ?: return false
        if (strokeId.isBlank() || points.isEmpty()) return false
        if (endedStrokeIds.contains(strokeId)) return false
        val normalized = points.map { (it.getOrNull(0) ?: 0f) to (it.getOrNull(1) ?: 0f) }
        val acc = strokeAccumulator.getOrPut(strokeId) { mutableListOf() }
        val mergedAcc = DrawGuessSync.mergeStrokePoints(acc.toList(), normalized)
        acc.clear()
        acc.addAll(mergedAcc)
        // TX 路径不写 liveStrokes，避免画家侧自反馈触发 publishUi

        val batch = txBatchByStroke.getOrPut(strokeId) {
            TxBatch(strokeId = strokeId, round = round, color = color, width = width)
        }
        batch.color = color
        batch.width = width
        batch.points.addAll(points)
        if (flushNow) {
            flushTxForStroke(strokeId, ws)
        } else {
            scheduleTxFlush()
        }
        return true
    }

    private fun scheduleTxFlush() {
        val scope = syncScope ?: run {
            flushAllTxNow()
            return
        }
        if (txFlushJob?.isActive == true) return
        txFlushJob = scope.launch {
            delay(TX_COALESCE_MS)
            flushAllTxNow()
        }
    }

    private fun flushAllTxNow() {
        val ws = session ?: return
        txBatchByStroke.keys.toList().forEach { strokeId ->
            flushTxForStroke(strokeId, ws)
        }
    }

    private fun flushTxForStroke(strokeId: String, ws: DrawWsSession) {
        val batch = txBatchByStroke.remove(strokeId) ?: return
        if (batch.points.isEmpty()) return
        val chunk = chunkByStroke.getOrPut(strokeId) { AtomicInteger(0) }.getAndIncrement()
        val wirePoints = batch.points
        val ok = if (DrawWsConfig.useBinaryWire()) {
            ws.sendBinary(
                DrawWsBinaryCodec.encodeStrokeChunk(
                    strokeId = strokeId,
                    chunk = chunk,
                    round = batch.round,
                    width = batch.width,
                    color = batch.color,
                    points = wirePoints,
                ),
            )
        } else {
            ws.send(
                DrawWsProtocol.strokeChunk(
                    activeRoomId.orEmpty(),
                    strokeId,
                    chunk,
                    batch.round,
                    batch.color,
                    batch.width,
                    wirePoints,
                ),
            )
        }
        if (!ok) Log.w(TAG, "tx chunk failed stroke=$strokeId chunk=$chunk")
        else Log.i(TAG, "tx chunk stroke=$strokeId n=$chunk pts=${wirePoints.size} round=${batch.round}")
    }

    fun finishStroke(
        roomId: String,
        strokeId: String,
        round: Int,
        color: String,
        width: Float,
        pointsOverride: List<Pair<Float, Float>>? = null,
    ): Pair<Int, List<Pair<Float, Float>>>? {
        val ws = session ?: return null
        flushTxForStroke(strokeId, ws)
        val accPoints = strokeAccumulator.remove(strokeId)?.toList().orEmpty()
        val allPoints = when {
            !pointsOverride.isNullOrEmpty() && pointsOverride.size >= accPoints.size -> pointsOverride
            accPoints.isNotEmpty() -> accPoints
            !pointsOverride.isNullOrEmpty() -> pointsOverride
            else -> emptyList()
        }
        if (allPoints.isEmpty()) return null
        endedStrokeIds.add(strokeId)
        chunkByStroke.remove(strokeId)
        txBatchByStroke.remove(strokeId)
        val seq = strokeSeq.incrementAndGet()
        val wire = allPoints.map { listOf(it.first, it.second) }
        if (DrawWsConfig.useBinaryWire()) {
            ws.sendBinary(
                DrawWsBinaryCodec.encodeStrokeChunk(
                    strokeId = strokeId,
                    chunk = 0,
                    round = round,
                    width = width,
                    color = color,
                    points = wire,
                    seq = seq,
                    type = DrawWsBinaryCodec.TYPE_END,
                ),
            )
        } else {
            ws.send(
                DrawWsProtocol.strokeEnd(roomId, strokeId, round, seq, color, width, wire),
            )
        }
        Log.i(TAG, "tx end stroke=$strokeId pts=${allPoints.size} seq=$seq round=$round")
        return seq to allPoints
    }

    fun sendClear(roomId: String, round: Int): Boolean {
        flushAllTxNow()
        strokeAccumulator.clear()
        txBatchByStroke.clear()
        clearLiveStrokes()
        _clearNonce.value += 1
        val ws = session ?: return false
        return if (DrawWsConfig.useBinaryWire()) {
            ws.sendBinary(DrawWsBinaryCodec.encodeClear(round))
        } else {
            ws.send(DrawWsProtocol.clear(roomId, round))
        }
    }

    private fun strokeSeqKey(strokeId: String): Int = strokeId.hashCode() and 0x7FFFFFFF

    private fun mergeIncoming(data: DrawWsStrokeChunk, finalize: Boolean) {
        if (data.round != activeRound) {
            Log.w(TAG, "rx drop round stroke=${data.strokeId} rx=${data.round} active=$activeRound")
            return
        }
        if (!finalize && finalizedStrokeIds.contains(data.strokeId)) {
            Log.d(TAG, "rx drop stale chunk stroke=${data.strokeId} n=${data.chunk}")
            return
        }
        strokeRxAtMs[data.strokeId] = System.currentTimeMillis()
        upsertLiveStroke(
            strokeId = data.strokeId,
            color = data.color,
            width = data.width,
            points = data.points,
            replace = finalize,
            flushNow = true,
        )
        if (finalize) {
            finalizedStrokeIds.add(data.strokeId)
            _strokeFinalizeNonce.value += 1
        }
    }
}
