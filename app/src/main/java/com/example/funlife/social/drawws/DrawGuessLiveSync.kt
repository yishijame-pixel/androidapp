package com.example.funlife.social.drawws

import android.util.Log
import com.example.funlife.social.game.engine.DrawGuessSync
import com.example.funlife.viewmodel.DrawStrokeUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

    enum class Transport { POCKETBASE, WEBSOCKET }

    private var session: DrawWsSession? = null
    private var collectJob: Job? = null
    private var activeRoomId: String? = null
    private val strokeSeq = AtomicInteger(0)
    private val chunkByStroke = ConcurrentHashMap<String, AtomicInteger>()

    private val _transport = MutableStateFlow(Transport.POCKETBASE)
    val transport: StateFlow<Transport> = _transport.asStateFlow()

    private val strokeLock = Any()
    /** strokeId → 合并后的笔画（线程安全） */
    private val strokeById = ConcurrentHashMap<String, DrawStrokeUi>()

    private val _liveStrokes = MutableStateFlow<List<DrawStrokeUi>>(emptyList())
    val liveStrokes: StateFlow<List<DrawStrokeUi>> = _liveStrokes.asStateFlow()

    private fun publishLiveStrokes() {
        _liveStrokes.value = strokeById.values.toList()
    }

    /** PB 已归档的笔画从 live 层移除，避免与 ledger 回放抢同一 strokeId 导致路径漂移 */
    fun dropLiveStroke(strokeId: String) {
        if (strokeId.isBlank()) return
        synchronized(strokeLock) {
            if (strokeById.remove(strokeId) != null) publishLiveStrokes()
        }
        strokeAccumulator.remove(strokeId)
        chunkByStroke.remove(strokeId)
    }

    private fun upsertLiveStroke(
        strokeId: String,
        color: String,
        width: Float,
        points: List<Pair<Float, Float>>,
        replace: Boolean = false,
    ) {
        synchronized(strokeLock) {
            val seq = strokeSeqKey(strokeId)
            val old = strokeById[strokeId]
            val mergedPoints = when {
                old == null -> points
                replace && points.size >= old.points.size -> points
                replace -> DrawGuessSync.mergeStrokePoints(old.points, points)
                else -> DrawGuessSync.mergeStrokePoints(old.points, points)
            }
            strokeById[strokeId] = DrawStrokeUi(
                seq = seq,
                points = mergedPoints,
                color = color,
                width = width,
                strokeId = strokeId,
            )
            publishLiveStrokes()
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "live stroke=$strokeId pts=${mergedPoints.size} replace=$replace")
            }
        }
    }

    /** strokeId → 累积点（用于 stroke_end 归档） */
    private val strokeAccumulator = ConcurrentHashMap<String, MutableList<Pair<Float, Float>>>()

    private fun clearLiveStrokes() {
        synchronized(strokeLock) {
            strokeById.clear()
            _liveStrokes.value = emptyList()
        }
    }

    fun isConnected(roomId: String): Boolean =
        session != null && activeRoomId == roomId && _transport.value == Transport.WEBSOCKET

    /** TCP 已连或鉴权中，避免重入 start() 把会话掐掉 */
    fun isPending(roomId: String): Boolean =
        session != null && activeRoomId == roomId && !isConnected(roomId)

    fun start(scope: CoroutineScope, roomId: String, token: String) {
        if (isConnected(roomId) || isPending(roomId)) return
        stop()
        if (!DrawWsConfig.isEnabled() || token.isBlank()) {
            _transport.value = Transport.POCKETBASE
            return
        }
        activeRoomId = roomId
        val ws = DrawWsSession(scope, roomId, token)
        session = ws
        collectJob = scope.launch {
            ws.events.collect { event ->
                when (event) {
                    is DrawWsEvent.Joined -> {
                        _transport.value = Transport.WEBSOCKET
                        Log.d(TAG, "joined ws room=${event.roomId}")
                    }
                    is DrawWsEvent.StrokeChunk -> {
                        Log.d(TAG, "rx chunk stroke=${event.data.strokeId} pts=${event.data.points.size}")
                        mergeIncoming(event.data, finalize = false)
                    }
                    is DrawWsEvent.StrokeEnd -> {
                        Log.d(TAG, "rx end stroke=${event.data.strokeId} pts=${event.data.points.size}")
                        mergeIncoming(event.data, finalize = true)
                    }
                    is DrawWsEvent.Clear -> {
                        clearLiveStrokes()
                        strokeAccumulator.clear()
                    }
                    is DrawWsEvent.Replay -> event.events.forEach { replay ->
                        when (replay) {
                            is DrawWsEvent.StrokeChunk -> mergeIncoming(replay.data, finalize = false)
                            is DrawWsEvent.StrokeEnd -> mergeIncoming(replay.data, finalize = true)
                            is DrawWsEvent.Clear -> clearLiveStrokes()
                            else -> Unit
                        }
                    }
                    is DrawWsEvent.Disconnected -> _transport.value = Transport.POCKETBASE
                    is DrawWsEvent.Error -> Log.w(TAG, "ws error ${event.code}")
                }
            }
        }
        ws.start()
    }

    fun stop() {
        collectJob?.cancel()
        session?.stop()
        session = null
        collectJob = null
        activeRoomId = null
        _transport.value = Transport.POCKETBASE
        clearLiveStrokes()
        strokeAccumulator.clear()
        chunkByStroke.clear()
    }

    fun isWsActive(): Boolean =
        DrawWsConfig.isEnabled() && session != null && _transport.value == Transport.WEBSOCKET

    fun newStrokeId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    /** 发送分片（热路径，不走 HTTP） */
    fun sendChunk(
        roomId: String,
        strokeId: String,
        round: Int,
        color: String,
        width: Float,
        points: List<List<Float>>,
    ): Boolean {
        val ws = session ?: return false
        val chunk = chunkByStroke.getOrPut(strokeId) { AtomicInteger(0) }.getAndIncrement()
        val normalized = points.map { (it.getOrNull(0) ?: 0f) to (it.getOrNull(1) ?: 0f) }
        val acc = strokeAccumulator.getOrPut(strokeId) { mutableListOf() }
        val mergedAcc = DrawGuessSync.mergeStrokePoints(acc.toList(), normalized)
        acc.clear()
        acc.addAll(mergedAcc)
        upsertLiveStroke(strokeId, color, width, normalized)
        val ok = ws.send(
            DrawWsProtocol.strokeChunk(roomId, strokeId, chunk, round, color, width, points),
        )
        if (!ok) Log.w(TAG, "tx chunk failed stroke=$strokeId chunk=$chunk")
        return ok
    }

    /** 抬手：广播 stroke_end，返回归档用 seq + 全量点 */
    fun finishStroke(
        roomId: String,
        strokeId: String,
        round: Int,
        color: String,
        width: Float,
    ): Pair<Int, List<Pair<Float, Float>>>? {
        val ws = session ?: return null
        val allPoints = strokeAccumulator.remove(strokeId)?.toList().orEmpty()
        if (allPoints.size < 2) return null
        chunkByStroke.remove(strokeId)
        val seq = strokeSeq.incrementAndGet()
        val wire = allPoints.map { listOf(it.first, it.second) }
        ws.send(
            DrawWsProtocol.strokeEnd(roomId, strokeId, round, seq, color, width, wire),
        )
        return seq to allPoints
    }

    fun sendClear(roomId: String, round: Int): Boolean {
        strokeAccumulator.clear()
        clearLiveStrokes()
        return session?.send(DrawWsProtocol.clear(roomId, round)) == true
    }

    private fun strokeSeqKey(strokeId: String): Int = strokeId.hashCode() and 0x7FFFFFFF

    private fun mergeIncoming(data: DrawWsStrokeChunk, finalize: Boolean) {
        upsertLiveStroke(
            strokeId = data.strokeId,
            color = data.color,
            width = data.width,
            points = data.points,
            replace = finalize,
        )
    }
}
