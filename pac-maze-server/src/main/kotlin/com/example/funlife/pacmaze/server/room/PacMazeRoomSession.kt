package com.example.funlife.pacmaze.server.room

import com.example.funlife.pacmaze.server.JsonUtil
import com.example.funlife.pacmaze.server.auth.PacMazeAuthResult
import com.example.funlife.pacmaze.server.auth.PacMazeRoomMemberSummary
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeArenaParser
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineEndReason
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineMatchConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineMatchMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineSimulation
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeTickInput
import com.example.funlife.social.game.engine.pacmaze.PacMazeVersusRule
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.PacMazeWsProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class PacMazePeer(
    val userId: String,
    val entityId: String,
    val isHost: Boolean,
    val session: WebSocketSession,
)

private data class DisconnectedPeer(
    val entityId: String,
    val disconnectedAtMs: Long,
)

class PacMazeRoomSession(
    val roomId: String,
    private val roomSummary: PacMazeRoomMemberSummary,
    private val scope: CoroutineScope,
) {
    private val peers = ConcurrentHashMap<String, PacMazePeer>()
    private val disconnected = ConcurrentHashMap<String, DisconnectedPeer>()
    private val readyUsers = ConcurrentHashMap.newKeySet<String>()
    private val latestInputs = ConcurrentHashMap<String, PacMazeTickInput>()
    private val pendingAttacks = ConcurrentHashMap.newKeySet<String>()
    private val tickRunning = AtomicBoolean(false)
    private var tickJob: Job? = null
    private var roomGoTimerJob: Job? = null

    lateinit var matchConfig: PacMazeOnlineMatchConfig
        private set
    lateinit var levelConfig: PacMazeLevelConfig
        private set
    private lateinit var levelJson: String
    private var world: PacMazeWorldState? = null
    private var worldTemplate: PacMazeWorldState? = null
    private var roomGoSent = false
    private var roomGoStartMs: Long = 0L

    private val disconnectGraceMs = System.getenv("PAC_MAZE_DISCONNECT_MS")?.toLongOrNull() ?: 30_000L
    private val roomGoTimeoutMs = System.getenv("PAC_MAZE_ROOM_GO_MS")?.toLongOrNull() ?: 4_000L

    fun initFromAuth(auth: PacMazeAuthResult) {
        if (::matchConfig.isInitialized) return
        matchConfig = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            versusRule = PacMazeVersusRule.RACE_PELLETS,
            arenaId = roomSummary.arenaId,
            matchSeed = roomSummary.matchSeed,
            hostPbId = roomSummary.hostPbId,
            guestPbId = roomSummary.guestPbId,
            hostEntityId = "pac_a",
            guestEntityId = "pac_b",
        )
        val (level, json) = PacMazeArenaParser.loadArenaById(matchConfig.arenaId)
        levelConfig = level
        levelJson = json
        val fresh = PacMazeArenaParser.buildOnlineWorld(level, json, matchConfig)
        worldTemplate = fresh
        world = fresh.copy(phase = PacMazePhase.PLAYING)
    }

    suspend fun attachPeer(auth: PacMazeAuthResult, ws: WebSocketSession) {
        initFromAuth(auth)
        disconnected.remove(auth.userId)
        peers[auth.userId] = PacMazePeer(
            userId = auth.userId,
            entityId = auth.entityId,
            isHost = auth.isHost,
            session = ws,
        )
        latestInputs.putIfAbsent(auth.entityId, PacMazeTickInput.Inactive)
        val snap = world
        ws.send(
            Frame.Text(
                JsonUtil.toJson(
                    PacMazeWsProtocol.buildJoined(
                        entityId = auth.entityId,
                        isHost = auth.isHost,
                        tick = snap?.tick ?: 0L,
                        state = snap,
                    ),
                ),
            ),
        )
        broadcastRoomState()
        if (roomGoSent) {
            sendTo(ws, PacMazeWsProtocol.buildRoomGo(roomGoStartMs))
            val w = world
            if (w != null) {
                sendTo(ws, PacMazeWsProtocol.buildStateMessage(w))
            }
        } else {
            scheduleRoomGoFallback()
        }
    }

    suspend fun detachPeer(userId: String) {
        val peer = peers.remove(userId) ?: return
        disconnected[userId] = DisconnectedPeer(
            entityId = peer.entityId,
            disconnectedAtMs = System.currentTimeMillis(),
        )
        latestInputs[peer.entityId] = PacMazeTickInput.Inactive
        broadcastRoomState()
        if (peers.isEmpty() && !roomGoSent) {
            roomGoTimerJob?.cancel()
            tickRunning.set(false)
        }
    }

    fun handleMessage(userId: String, payload: Map<String, Any?>) {
        val peer = peers[userId] ?: return
        when (payload["t"]?.toString()) {
            PacMazeWsProtocol.KIND_INPUT -> handleInput(peer, payload)
            PacMazeWsProtocol.KIND_PING -> scope.launch {
                sendTo(
                    peer.session,
                    mapOf(
                        "t" to PacMazeWsProtocol.KIND_PONG,
                        "clientMs" to payload["clientMs"],
                        "serverMs" to System.currentTimeMillis(),
                    ),
                )
            }
            PacMazeWsProtocol.KIND_READY -> scope.launch { markReady(userId) }
        }
    }

    private suspend fun markReady(userId: String) {
        if (!peers.containsKey(userId)) return
        readyUsers.add(userId)
        broadcastRoomState()
        tryFireRoomGo()
    }

    private fun handleInput(peer: PacMazePeer, payload: Map<String, Any?>) {
        val tick = world?.tick ?: 0L
        val attack = payload["attack"] == true
        if (attack) pendingAttacks.add(peer.entityId)
        if (payload["release"] == true) {
            latestInputs[peer.entityId] = PacMazeTickInput.Inactive
            scope.launch { broadcast(PacMazeWsProtocol.buildPeerInput(peer.entityId, payload)) }
            return
        }
        val dirWire = payload["dir"]?.toString()?.takeIf { it.isNotBlank() } ?: return
        val dir = runCatching { Direction.valueOf(dirWire) }.getOrNull() ?: return
        val inputTick = payload["tick"]?.toString()?.toLongOrNull() ?: tick
        latestInputs[peer.entityId] = PacMazeTickInput.committed(inputTick, dir)
        scope.launch { broadcast(PacMazeWsProtocol.buildPeerInput(peer.entityId, payload)) }
    }

    private fun scheduleRoomGoFallback() {
        if (roomGoSent || roomGoTimerJob?.isActive == true) return
        roomGoTimerJob = scope.launch {
            delay(roomGoTimeoutMs)
            fireRoomGo()
        }
    }

    private suspend fun tryFireRoomGo() {
        if (roomGoSent) return
        if (peers.isNotEmpty() && peers.keys.all { readyUsers.contains(it) }) {
            fireRoomGo()
        }
    }

    private suspend fun fireRoomGo() {
        if (roomGoSent) return
        roomGoSent = true
        roomGoTimerJob?.cancel()
        roomGoStartMs = System.currentTimeMillis()
        val fresh = PacMazeArenaParser.buildOnlineWorld(levelConfig, levelJson, matchConfig)
            .copy(phase = PacMazePhase.PLAYING)
        worldTemplate = fresh
        world = fresh
        broadcast(PacMazeWsProtocol.buildRoomGo(roomGoStartMs))
        startTickLoop()
    }

    private fun startTickLoop() {
        if (!tickRunning.compareAndSet(false, true)) return
        tickJob = scope.launch {
            val tickDurationMs = 1000L / PacMazeConstants.TICKS_PER_SECOND
            var nextTickAt = System.currentTimeMillis()
            while (isActive && (peers.isNotEmpty() || disconnected.isNotEmpty())) {
                if (checkDisconnectForfeit()) break
                stepSimulation()
                if (world?.phase != PacMazePhase.PLAYING) break
                nextTickAt += tickDurationMs
                val sleepMs = (nextTickAt - System.currentTimeMillis()).coerceAtLeast(1L)
                delay(sleepMs)
            }
            tickRunning.set(false)
        }
    }

    private suspend fun checkDisconnectForfeit(): Boolean {
        if (disconnected.isEmpty()) return false
        val w = world ?: return false
        if (w.phase != PacMazePhase.PLAYING) return false
        val now = System.currentTimeMillis()
        val cfg = matchConfig
        val timedOut = disconnected.values.filter { now - it.disconnectedAtMs >= disconnectGraceMs }
        if (timedOut.isEmpty()) return false
        if (timedOut.size >= 2 || (peers.isEmpty() && timedOut.size >= 1)) {
            endMatchDisconnect(w, timedOut.first().entityId)
            return true
        }
        if (peers.size == 1 && timedOut.size == 1) {
            val remaining = peers.values.first()
            endMatchDisconnect(w, timedOut.first().entityId, winnerEntityId = remaining.entityId)
            return true
        }
        return false
    }

    private suspend fun endMatchDisconnect(
        world: PacMazeWorldState,
        disconnectedEntityId: String,
        winnerEntityId: String? = null,
    ) {
        val cfg = matchConfig
        val winner = winnerEntityId ?: when (disconnectedEntityId) {
            cfg.hostEntityId -> cfg.guestEntityId
            cfg.guestEntityId -> cfg.hostEntityId
            else -> null
        }
        val ended = world.copy(
            phase = PacMazePhase.LEVEL_CLEAR,
            onlineWinnerEntityId = winner,
            onlineEndReason = PacMazeOnlineEndReason.DISCONNECT,
        )
        this.world = ended
        broadcast(
            PacMazeWsProtocol.buildMatchEnd(
                winnerEntityId = winner,
                scoreA = ended.playerScoreA,
                scoreB = ended.playerScoreB,
                reason = PacMazeOnlineEndReason.DISCONNECT,
            ),
        )
        tickJob?.cancel()
        tickRunning.set(false)
    }

    private fun stepSimulation() {
        val cfg = matchConfig
        val level = levelConfig
        var w = world ?: return
        if (w.phase != PacMazePhase.PLAYING) return
        val inputs = buildMap {
            put(cfg.hostEntityId, latestInputs[cfg.hostEntityId] ?: PacMazeTickInput.Inactive)
            put(cfg.guestEntityId, latestInputs[cfg.guestEntityId] ?: PacMazeTickInput.Inactive)
        }
        val attacks = pendingAttacks.toSet()
        pendingAttacks.clear()
        w = PacMazeOnlineSimulation.tick(w, inputs, level, cfg, attacks)
        world = w
        if (w.tick % SNAPSHOT_EVERY_TICKS == 0L) {
            val inputSnap = buildMap {
                put(cfg.hostEntityId, latestInputs[cfg.hostEntityId] ?: PacMazeTickInput.Inactive)
                put(cfg.guestEntityId, latestInputs[cfg.guestEntityId] ?: PacMazeTickInput.Inactive)
            }
            scope.launch {
                broadcast(PacMazeWsProtocol.buildStateMessage(w, inputSnap, cfg))
            }
        }
        if (w.phase == PacMazePhase.LEVEL_CLEAR || w.phase == PacMazePhase.GAME_OVER) {
            scope.launch {
                broadcast(
                    PacMazeWsProtocol.buildMatchEnd(
                        winnerEntityId = w.onlineWinnerEntityId,
                        scoreA = w.playerScoreA,
                        scoreB = w.playerScoreB,
                        reason = w.onlineEndReason ?: PacMazeOnlineEndReason.NORMAL,
                    ),
                )
                tickJob?.cancel()
                tickRunning.set(false)
            }
        }
    }

    private suspend fun broadcastRoomState() {
        broadcast(
            PacMazeWsProtocol.buildRoomState(
                peerCount = peers.size,
                readyCount = readyUsers.size,
            ),
        )
    }

    private suspend fun broadcast(msg: Map<String, Any?>) {
        val raw = JsonUtil.toJson(msg)
        peers.values.forEach { peer ->
            runCatching { peer.session.send(Frame.Text(raw)) }
        }
    }

    private suspend fun sendTo(session: WebSocketSession, msg: Map<String, Any?>) {
        session.send(Frame.Text(JsonUtil.toJson(msg)))
    }

    companion object {
        private const val SNAPSHOT_EVERY_TICKS = 60L
    }
}

class PacMazeRoomRegistry(private val scope: CoroutineScope) {
    private val rooms = ConcurrentHashMap<String, PacMazeRoomSession>()

    fun getOrCreate(roomId: String, summary: PacMazeRoomMemberSummary): PacMazeRoomSession =
        rooms.computeIfAbsent(roomId) { PacMazeRoomSession(roomId, summary, scope) }

    fun activeRoomCount(): Int = rooms.size
}
