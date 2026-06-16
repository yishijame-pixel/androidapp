package com.example.funlife.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.PacMazePrefs
import com.example.funlife.repository.PacMazeProgressRepository
import com.example.funlife.social.game.GamePlayInteractor
import com.example.funlife.social.game.GamePlaySyncManager
import com.example.funlife.social.game.engine.pacmaze.CampaignLevelSource
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeBoardSync
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeEloCalculator
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeLoadParams
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineEndReason
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineInput
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineLoader
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineMatchConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineMatchMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineSimulation
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeRawJoystickSample
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeStateSnapshot
import com.example.funlife.social.game.engine.pacmaze.PacMazeTickInput
import com.example.funlife.social.game.engine.pacmaze.PacMazeVersusRule
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.model.GameMoveKind
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.PacMazeMatchResultWire
import com.example.funlife.social.game.model.PacMazePlayState
import com.example.funlife.social.game.engine.pacmaze.PacMazeRollbackSession
import com.example.funlife.social.pacmazews.PacMazeOnlineDiagnostics
import com.example.funlife.social.pacmazews.PacMazeWsConfig
import com.example.funlife.social.pacmazews.PacMazeWsEvent
import com.example.funlife.social.pacmazews.PacMazeWsSession
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class PacMazeOnlineUiState(
    val loading: Boolean = true,
    val loadingMessage: String = "正在同步对局…",
    val countdown: Int = 0,
    val world: PacMazeWorldState? = null,
    val levelConfig: PacMazeLevelConfig? = null,
    val matchConfig: PacMazeOnlineMatchConfig? = null,
    val playState: PacMazePlayState? = null,
    val myPbId: String = "",
    val myEntityId: String = "",
    val peerName: String = "对手",
    val peerEntityId: String = "",
    val isHost: Boolean = false,
    val isAuthoritative: Boolean = false,
    val roomId: String = "",
    val status: GameRoomStatus = GameRoomStatus.PLAYING,
    val avatarLoadout: PacMazeAvatarLoadout = PacMazeAvatarLoadout(),
    val mapThemeId: PacMazeMapThemeId = PacMazeMapThemeId.CYBERPUNK,
    val showResult: Boolean = false,
    val resultTitle: String = "",
    val resultMessage: String = "",
    val eloDelta: Int = 0,
    val syncLagMs: Long = 0,
    val lastSnapshotTick: Long = 0,
    val useServerRenderBlend: Boolean = false,
    /** Debug：同步模式文案（WS权威 / 房主权威 / 客人PB） */
    val syncModeLabel: String = "",
    /** 地图幽灵放出倒计时（秒）；0 = 已在场内追击 */
    val mapGhostReleaseSec: Int = 0,
    /** WS 链路：连接中 / 已加入 / 对局中 / 已回退PB */
    val wsStatus: String = "",
    /** 到对战服 RTT（ms） */
    val rttMs: Long = 0,
    val toast: String? = null,
)

/**
 * 在线对战 ViewModel。
 *
 * - **WS 模式**（[PacMazeWsConfig.isEnabled]）：权威 Ktor 服模拟，客户端只渲染 + 发输入
 * - **PB 过渡模式**：房主权威快照 + 客人发输入（Phase1 fallback）
 */
class PacMazeOnlineViewModel(
    app: Application,
    private val userId: Long,
    private val roomId: String,
) : AndroidViewModel(app) {

    private val appContext: Context = app.applicationContext
    private val interactor = GamePlayInteractor(app, userId)
    private val progressRepo = PacMazeProgressRepository(
        (appContext as FunLifeApplication).database.pacMazeProgressDao(),
    )
    private val prefs = PacMazePrefs(appContext)
    private val campaignSource = CampaignLevelSource(appContext)

    private val _ui = MutableStateFlow(PacMazeOnlineUiState(roomId = roomId))
    val ui: StateFlow<PacMazeOnlineUiState> = _ui.asStateFlow()

    private val _renderFrame = MutableStateFlow<PacMazeRenderFrame?>(null)
    val renderFrame: StateFlow<PacMazeRenderFrame?> = _renderFrame.asStateFlow()

    private var worldTemplate: PacMazeWorldState? = null
    private var levelJson: String = ""
    private var simulationWorld: PacMazeWorldState? = null
    private var localEntityId: String = ""
    private var peerEntityId: String = ""
    private var isHost: Boolean = false

    private var rawJoystick: PacMazeRawJoystickSample = PacMazeRawJoystickSample.Released
    private var inputGeneration: Long = 0L
    private var lastCommittedDir: Direction? = null
    private var pendingAttack = false

    private val guestInputRef = AtomicReference<PacMazeTickInput>(PacMazeTickInput.Inactive)
    private val guestAttackPending = AtomicReference(false)
    private lateinit var wsRollback: PacMazeRollbackSession
    private var outboundMoveIndex = 0
    private var lastProcessedMoveIndex = 0
    private var lastBroadcastTick: Long = -1L
    private var lastSnapshotReceivedMs: Long = 0L
    private var lastSentDir: Direction? = null
    private var lastInputSentMs: Long = 0L

    private var sessionSimAnchorMs: Long = 0L
    private var simRunning = false

    private val wsModeConfigured = PacMazeWsConfig.isEnabled()
    private var useWsAuthority = wsModeConfigured
    private var wsJoined = false
    private var wsRoomGoReceived = false
    private var wsWatchdogJob: Job? = null
    private var wsSession: PacMazeWsSession? = null
    private var wsCollectJob: kotlinx.coroutines.Job? = null
    private val outboundSeq = AtomicLong(0L)
    private var wsReadySent = false

    companion object {
        private const val TAG = PacMazeOnlineDiagnostics.VM_TAG
        private const val COUNTDOWN_MS = 2_100L
        /** 房主每 N 逻辑 tick 广播一次快照（60Hz / 4 ≈ 15Hz）。 */
        private const val SNAPSHOT_EVERY_TICKS = 4L
        /** 固定逻辑帧率推进（权威端 / 渲染插值基准）。 */
        private const val SIM_TICKS_PER_FRAME_MAX = PacMazeConstants.MAX_SIM_TICKS_PER_FRAME
        private const val WS_JOIN_TIMEOUT_MS = 10_000L
        private const val WS_ROOM_GO_TIMEOUT_MS = 12_000L
        private const val WS_INPUT_INTERVAL_MS = 8L
    }

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        _ui.update { it.copy(loading = true, loadingMessage = "正在获取房间…") }
        val creds = interactor.ensurePlayCredentials().getOrElse {
            _ui.update { s -> s.copy(loading = false, toast = interactor.mapError(it)) }
            return
        }
        val refresh = interactor.refreshPlayState(roomId, updateLocalCache = true).getOrElse {
            _ui.update { s -> s.copy(loading = false, toast = interactor.mapError(it)) }
            return
        }
        val (room, moves) = refresh
        if (room.gameType != "pac_maze") {
            _ui.update { it.copy(loading = false, toast = "非豆人迷宫房间") }
            return
        }
        val play = room.gameState?.pacMaze ?: run {
            _ui.update { it.copy(loading = false, toast = "对局状态缺失") }
            return
        }
        val myPbId = creds.pbRecordId
        isHost = room.hostPbId == myPbId
        val matchMode = PacMazeOnlineMatchMode.fromId(play.matchMode)
        val matchConfig = PacMazeOnlineMatchConfig(
            mode = matchMode,
            versusRule = PacMazeVersusRule.fromId(play.versusRule),
            timeLimitSeconds = play.timeLimitSec,
            levelId = play.levelId,
            arenaId = play.arenaId,
            matchSeed = play.matchSeed,
            hostPbId = play.hostPbId,
            guestPbId = play.guestPbId,
            hostEntityId = play.playerA.entityId.ifBlank { "pac_a" },
            guestEntityId = play.playerB.entityId.ifBlank { "pac_b" },
        )
        localEntityId = if (isHost) matchConfig.hostEntityId else matchConfig.guestEntityId
        peerEntityId = if (isHost) matchConfig.guestEntityId else matchConfig.hostEntityId
        wsRollback = PacMazeRollbackSession(localEntityId, peerEntityId)
        val peerName = if (isHost) {
            room.guestProfile?.displayName?.ifBlank { "好友" } ?: "好友"
        } else {
            room.hostProfile?.displayName?.ifBlank { "房主" } ?: "房主"
        }

        _ui.update { it.copy(loadingMessage = "加载地图…") }
        val (level, json) = withContext(Dispatchers.IO) {
            when (matchMode) {
                PacMazeOnlineMatchMode.VERSUS_DUEL -> PacMazeOnlineLoader.loadArena(appContext, play.arenaId)
                PacMazeOnlineMatchMode.COOP_CAMPAIGN -> {
                    val payload = campaignSource.load(
                        PacMazeLoadParams(
                            runMode = PacMazeRunMode.CAMPAIGN,
                            levelId = play.levelId.coerceIn(1, 8),
                            seed = play.matchSeed,
                            userId = userId,
                        ),
                    )
                    payload.level to payload.json
                }
                else -> PacMazeOnlineLoader.loadArena(appContext, play.arenaId)
            }
        }
        levelJson = json
        val freshWorld = PacMazeOnlineLoader.buildOnlineWorld(level, json, matchConfig)
        worldTemplate = freshWorld
        simulationWorld = freshWorld
        lastProcessedMoveIndex = moves.maxOfOrNull { it.moveIndex } ?: 0
        outboundMoveIndex = lastProcessedMoveIndex
        publishRenderFrame(blend = 1f)
        if (useWsAuthority) {
            wsRollback.updatePeers(localEntityId, peerEntityId)
            wsRollback.configure(level, matchConfig)
            wsRollback.reset(freshWorld)
        }

        GamePlaySyncManager.startSession(
            ctx = appContext,
            userId = userId,
            roomId = roomId,
            replaceExisting = true,
            initialMoveIndex = lastProcessedMoveIndex,
            onMoveReceived = { move -> viewModelScope.launch { handleMove(move) } },
            onRoomUpdated = { dto ->
                if (dto.status == GameRoomStatus.FINISHED) {
                    viewModelScope.launch { showFinishedFromRoom(dto.gameState?.pacMaze) }
                }
            },
        )

        if (useWsAuthority) {
            startWsSession(creds.token)
            scheduleWsWatchdog()
        } else {
            Log.w(TAG, "WS未启用 url=${PacMazeWsConfig.url()} → 回退房主PB权威模式")
        }

        val syncLabel = when {
            useWsAuthority -> "Rollback网"
            isHost -> "房主PB权威"
            else -> "客人PB渲染"
        }
        val wsStatus = if (useWsAuthority) "连接中" else ""
        Log.i(
            TAG,
            "bootstrap room=$roomId sync=$syncLabel wsUrl=${PacMazeWsConfig.url()} " +
                "entity=$localEntityId host=$isHost",
        )

        _ui.update {
            it.copy(
                loading = false,
                world = simulationWorld,
                levelConfig = level,
                matchConfig = matchConfig,
                playState = play,
                myPbId = myPbId,
                myEntityId = localEntityId,
                peerEntityId = peerEntityId,
                peerName = peerName,
                isHost = isHost,
                isAuthoritative = !useWsAuthority && isHost,
                useServerRenderBlend = useWsAuthority,
                syncModeLabel = syncLabel,
                wsStatus = wsStatus,
                status = room.status,
                avatarLoadout = prefs.avatarLoadout(userId),
                countdown = 3,
            )
        }
        runCountdownAndStart(play.startedAtMs)
    }

    private fun startWsSession(token: String) {
        wsCollectJob?.cancel()
        wsSession?.stop()
        val session = PacMazeWsSession(viewModelScope, roomId, token)
        wsSession = session
        wsCollectJob = viewModelScope.launch {
            session.events.collect { event -> handleWsEvent(event) }
        }
        session.start()
        Log.i(TAG, "ws connect url=${PacMazeWsConfig.url()} room=$roomId")
    }

    private fun handleWsEvent(event: PacMazeWsEvent) {
        val cfg = _ui.value.matchConfig ?: return
        when (event) {
            is PacMazeWsEvent.Joined -> {
                wsJoined = true
                wsWatchdogJob?.cancel()
                wsWatchdogJob = viewModelScope.launch {
                    delay(WS_ROOM_GO_TIMEOUT_MS)
                    if (!wsRoomGoReceived && useWsAuthority) {
                        activatePbFallback("WS 未收到 room_go")
                    }
                }
                if (event.entityId.isNotBlank()) {
                    localEntityId = event.entityId
                    peerEntityId = if (event.isHost) cfg.guestEntityId else cfg.hostEntityId
                    isHost = event.isHost
                    if (useWsAuthority) wsRollback.updatePeers(localEntityId, peerEntityId)
                }
                event.state?.let { applyRemoteStateMap(it, cfg) }
                _ui.update {
                    it.copy(
                        myEntityId = localEntityId,
                        peerEntityId = peerEntityId,
                        isHost = isHost,
                        isAuthoritative = false,
                        useServerRenderBlend = true,
                        wsStatus = "已加入",
                    )
                }
                sendWsReadyIfNeeded()
                Log.i(
                    TAG,
                    "ws joined entity=$localEntityId host=$isHost tick=${event.tick} " +
                        "peers=${event.state?.let { "state" } ?: "no-state"}",
                )
            }
            is PacMazeWsEvent.RoomState -> Unit
            is PacMazeWsEvent.State -> applyRemoteStateMap(event.payload, cfg)
            is PacMazeWsEvent.PeerInput -> ingestPeerInput(event)
            is PacMazeWsEvent.Pong -> {
                _ui.update { it.copy(rttMs = event.rttMs) }
            }
            is PacMazeWsEvent.RoomGo -> {
                wsRoomGoReceived = true
                wsWatchdogJob?.cancel()
                resetSession()
                val fresh = simulationWorld ?: return
                wsRollback.reset(fresh)
                sessionSimAnchorMs = event.startMs
                simRunning = true
                wsReadySent = true
                _ui.update { it.copy(countdown = 0, wsStatus = "对局中") }
                advanceWsRenderFrame(System.currentTimeMillis())
                Log.i(TAG, "ws room_go startMs=${event.startMs}")
            }
            is PacMazeWsEvent.MatchEnd -> {
                if (_ui.value.showResult) return
                val world = simulationWorld ?: return
                val winnerEntity = event.winnerEntityId
                val ended = world.copy(
                    phase = PacMazePhase.LEVEL_CLEAR,
                    playerScoreA = event.scoreA,
                    playerScoreB = event.scoreB,
                    onlineWinnerEntityId = winnerEntity,
                    onlineEndReason = event.reason,
                )
                simulationWorld = ended
                publishRenderFrame(blend = 1f)
                viewModelScope.launch { finalizeMatch(ended, cfg) }
            }
            is PacMazeWsEvent.Error -> {
                _ui.update { it.copy(toast = "对战服：${event.message}", wsStatus = "错误") }
                activatePbFallback("WS error ${event.code}")
            }
            PacMazeWsEvent.Disconnected -> {
                if (useWsAuthority && !wsRoomGoReceived) {
                    _ui.update { it.copy(wsStatus = "重连中") }
                }
            }
        }
    }

    private fun applyRemoteStateMap(map: Map<String, Any?>, cfg: PacMazeOnlineMatchConfig) {
        val template = worldTemplate ?: simulationWorld ?: return
        val snap = if (map["kind"] != null) map else map + ("kind" to PacMazeStateSnapshot.WIRE_KIND)
        val decoded = PacMazeStateSnapshot.decode(snap, template) ?: return
        val previous = simulationWorld ?: decoded
        val ghostsJustReleased = previous.ghostReleaseTicksLeft > 0 && decoded.ghostReleaseTicksLeft == 0
        if (ghostsJustReleased) {
            Log.i(TAG, "地图幽灵已放出（对战规则：约4秒后追击，非 bug）")
        }
        if (decoded.tick % 60L == 0L) {
            Log.d(
                TAG,
                "state tick=${decoded.tick} lagMs=${_ui.value.syncLagMs} " +
                    "ghostReleaseSec=${PacMazeOnlineDiagnostics.ghostReleaseSeconds(decoded.ghostReleaseTicksLeft)} " +
                    PacMazeOnlineDiagnostics.entitySummary(decoded),
            )
        }
        if (useWsAuthority) {
            ingestPeerInputsFromState(map, cfg, decoded.tick)
            wsRollback.onAuthoritativeSync(decoded)
            wsRollback.currentWorld()?.let { simulationWorld = it }
        } else {
            simulationWorld = decoded
            publishRenderFrame(previous = previous, blend = 0f)
        }
        lastSnapshotReceivedMs = System.currentTimeMillis()
        _ui.update {
            val displayWorld = if (useWsAuthority) {
                wsRollback.currentWorld() ?: _renderFrame.value?.current ?: simulationWorld ?: decoded
            } else {
                decoded
            }
            it.copy(
                world = displayWorld,
                lastSnapshotTick = decoded.tick,
                mapGhostReleaseSec = PacMazeOnlineDiagnostics.ghostReleaseSeconds(decoded.ghostReleaseTicksLeft),
                syncLagMs = if (sessionSimAnchorMs > 0L) {
                    System.currentTimeMillis() - sessionSimAnchorMs -
                        (decoded.tick * 1_000L / PacMazeConstants.TICKS_PER_SECOND)
                } else {
                    System.currentTimeMillis() - lastSnapshotReceivedMs
                },
            )
        }
        if (decoded.phase != PacMazePhase.PLAYING) {
            checkTerminal(decoded, cfg)
        }
    }

    private fun sampleLocalInputForRender(tick: Long = simulationWorld?.tick ?: 0L): PacMazeTickInput {
        val input = PacMazeOnlineInput.sampleDirect(rawJoystick, tick, inputGeneration)
        val latchedDir = resolveOutboundDirection(input)
        if (latchedDir != null) {
            if (latchedDir != lastCommittedDir) {
                inputGeneration++
                lastCommittedDir = latchedDir
            }
            return PacMazeTickInput.committed(tick, latchedDir, inputGeneration)
        }
        if (!rawJoystick.fingerDown) lastCommittedDir = null
        return input
    }

    private fun advanceWsRenderFrame(nowMs: Long) {
        val cfg = _ui.value.matchConfig ?: return
        val level = _ui.value.levelConfig ?: return
        wsRollback.configure(level, cfg)
        val inputTick = wsRollback.nextInputTick()
        val localInput = sampleLocalInputForRender(inputTick)
        val world = wsRollback.advanceFrame(nowMs, localInput) ?: return
        simulationWorld = world
        _renderFrame.value = PacMazeRenderFrame(current = world, previous = null, blend = 1f)
        _ui.update { it.copy(world = world, lastSnapshotTick = world.tick) }
    }

    private fun ingestPeerInput(event: PacMazeWsEvent.PeerInput) {
        if (event.entityId.isBlank() || event.entityId == localEntityId) return
        if (event.entityId != peerEntityId) return
        val tick = event.tick.takeIf { it > 0L } ?: wsRollback.nextInputTick()
        val input = when {
            event.release -> PacMazeTickInput.Inactive.copy(tick = tick)
            !event.dir.isNullOrBlank() -> runCatching { Direction.valueOf(event.dir) }.getOrNull()?.let { dir ->
                PacMazeTickInput.committed(tick, dir)
            } ?: return
            else -> return
        }
        wsRollback.onRemoteInput(tick, input, event.attack)
        wsRollback.currentWorld()?.let { world ->
            simulationWorld = world
            _renderFrame.value = PacMazeRenderFrame(current = world, previous = null, blend = 1f)
        }
    }

    private fun ingestPeerInputsFromState(
        map: Map<String, Any?>,
        cfg: PacMazeOnlineMatchConfig,
        serverTick: Long,
    ) {
        val isPeerHost = peerEntityId == cfg.hostEntityId
        val release = if (isPeerHost) map["input_host_release"] == true else map["input_guest_release"] == true
        val tick = serverTick.coerceAtLeast(1L)
        if (release) {
            wsRollback.onRemoteInput(tick, PacMazeTickInput.Inactive.copy(tick = tick), attack = false)
            return
        }
        val dirWire = (if (isPeerHost) map["input_host"] else map["input_guest"])?.toString()?.takeIf { it.isNotBlank() }
            ?: return
        runCatching { Direction.valueOf(dirWire) }.getOrNull()?.let { dir ->
            wsRollback.onRemoteInput(tick, PacMazeTickInput.committed(tick, dir), attack = false)
        }
    }

    private fun scheduleWsWatchdog() {
        wsWatchdogJob?.cancel()
        wsWatchdogJob = viewModelScope.launch {
            delay(WS_JOIN_TIMEOUT_MS)
            if (!wsJoined && useWsAuthority) {
                activatePbFallback("WS ${WS_JOIN_TIMEOUT_MS / 1000}s 内未加入")
                return@launch
            }
            delay(WS_ROOM_GO_TIMEOUT_MS - WS_JOIN_TIMEOUT_MS)
            if (!wsRoomGoReceived && useWsAuthority) {
                activatePbFallback("WS ${WS_ROOM_GO_TIMEOUT_MS / 1000}s 内未开局")
            }
        }
    }

    private fun activatePbFallback(reason: String) {
        if (!useWsAuthority) return
        useWsAuthority = false
        wsWatchdogJob?.cancel()
        wsSession?.stop()
        wsSession = null
        wsCollectJob?.cancel()
        wsCollectJob = null
        Log.w(TAG, "fallback PB: $reason")
        val syncLabel = if (isHost) "房主PB权威" else "客人PB渲染"
        _ui.update {
            it.copy(
                syncModeLabel = syncLabel,
                wsStatus = "已回退PB",
                useServerRenderBlend = false,
                isAuthoritative = isHost,
                toast = "对战服不可用，已切换房主同步",
            )
        }
        if (_ui.value.countdown <= 0 && !simRunning) {
            sessionSimAnchorMs = System.currentTimeMillis()
            simRunning = true
            publishRenderFrame(blend = 1f)
            Log.i(TAG, "PB fallback session start host=$isHost entity=$localEntityId")
        }
    }

    private fun canSendOnlineInput(): Boolean {
        if (_ui.value.countdown > 0 || _ui.value.showResult) return false
        return when {
            useWsAuthority -> wsSession?.isJoined == true
            isHost -> simRunning
            else -> simRunning
        }
    }

    private fun sendWsReadyIfNeeded() {
        if (!useWsAuthority || wsReadySent) return
        if (_ui.value.countdown > 0) return
        wsSession?.sendReady()
        wsReadySent = true
    }

    private suspend fun runCountdownAndStart(startedAtMs: Long) {
        val now = System.currentTimeMillis()
        val countdownEndMs = when {
            startedAtMs > 0L -> {
                val serverEnd = startedAtMs + COUNTDOWN_MS
                if (serverEnd > now + 400L) serverEnd else now + COUNTDOWN_MS
            }
            else -> now + COUNTDOWN_MS
        }
        while (true) {
            val remainingMs = countdownEndMs - System.currentTimeMillis()
            val countdownNum = ((remainingMs + 699L) / 700L).toInt().coerceIn(0, 3)
            _ui.update { it.copy(countdown = countdownNum) }
            if (remainingMs <= 0L) break
            delay(minOf(remainingMs, 50L))
        }
        _ui.update { it.copy(countdown = 0) }
        resetSession()
        if (useWsAuthority) {
            sendWsReadyIfNeeded()
            Log.d(TAG, "ws countdown done — waiting for room_go (input+prediction active)")
            return
        }
        sessionSimAnchorMs = System.currentTimeMillis()
        simRunning = true
        publishRenderFrame(blend = 1f)
        Log.d(TAG, "authoritative session start host=$isHost entity=$localEntityId")
    }

    private fun resetSession() {
        val cfg = _ui.value.matchConfig ?: return
        val level = _ui.value.levelConfig ?: return
        rawJoystick = PacMazeRawJoystickSample.Released
        inputGeneration = 0L
        lastCommittedDir = null
        pendingAttack = false
        guestInputRef.set(PacMazeTickInput.Inactive)
        guestAttackPending.set(false)
        lastBroadcastTick = -1L
        val fresh = PacMazeOnlineLoader.buildOnlineWorld(level, levelJson, cfg)
        worldTemplate = fresh
        simulationWorld = fresh
        if (useWsAuthority && ::wsRollback.isInitialized) wsRollback.reset(fresh)
        publishRenderFrame(blend = 1f)
    }

    /** Play 屏每帧调用；[frameNs] 为 withFrameNanos 时间戳（可选）。 */
    fun advanceOnlineFrame(frameNs: Long = 0L): PacMazeRenderFrame? {
        val nowMs = if (frameNs > 0L) frameNs / 1_000_000L else System.currentTimeMillis()
        if (useWsAuthority && _ui.value.countdown <= 0) {
            advanceWsRenderFrame(nowMs)
            updateGuestSyncLag()
            return _renderFrame.value
        }
        if (!simRunning) return _renderFrame.value
        val cfg = _ui.value.matchConfig ?: return _renderFrame.value
        val level = _ui.value.levelConfig ?: return _renderFrame.value

        if (isHost) {
            advanceHostFrame(cfg, level)
        } else {
            updateGuestSyncLag()
        }
        return _renderFrame.value
    }

    private fun advanceHostFrame(cfg: PacMazeOnlineMatchConfig, level: PacMazeLevelConfig) {
        var world = simulationWorld ?: return
        if (world.phase != PacMazePhase.PLAYING) {
            checkTerminal(world, cfg)
            return
        }

        val targetTick = computeTargetTick()
        if (world.tick >= targetTick) return

        val previousWorld = world
        var ticks = 0
        while (world.tick < targetTick && world.phase == PacMazePhase.PLAYING && ticks < SIM_TICKS_PER_FRAME_MAX) {
            val hostInput = sampleLocalInput(world.tick)
            val guestInput = guestInputRef.get()
            val guestAttack = guestAttackPending.getAndSet(false)
            val attacks = buildSet {
                if (pendingAttack) add(localEntityId)
                pendingAttack = false
                if (guestAttack) add(peerEntityId)
            }
            val inputs = mapOf(
                cfg.hostEntityId to if (localEntityId == cfg.hostEntityId) hostInput else guestInput,
                cfg.guestEntityId to if (localEntityId == cfg.guestEntityId) hostInput else guestInput,
            )
            world = PacMazeOnlineSimulation.tick(world, inputs, level, cfg, attacks)
            maybeBroadcastSnapshot(world)
            ticks++
        }
        simulationWorld = world
        publishRenderFrame(previous = previousWorld, blend = 0f)
        if (world.phase != PacMazePhase.PLAYING) {
            broadcastSnapshotNow(world)
            checkTerminal(world, cfg)
        }
    }

    private fun broadcastSnapshotNow(world: PacMazeWorldState) {
        if (!isHost) return
        lastBroadcastTick = world.tick
        fireMove(PacMazeStateSnapshot.encode(world))
    }

    private fun computeTargetTick(): Long {
        if (sessionSimAnchorMs <= 0L) return 0L
        val elapsedMs = System.currentTimeMillis() - sessionSimAnchorMs
        return (elapsedMs * PacMazeConstants.TICKS_PER_SECOND / 1_000L).coerceAtLeast(0L)
    }

    private fun sampleLocalInput(tick: Long): PacMazeTickInput {
        val input = PacMazeOnlineInput.sampleDirect(rawJoystick, tick, inputGeneration)
        val dir = input.committed
        if (dir != null && dir != lastCommittedDir) {
            inputGeneration++
            lastCommittedDir = dir
            return input.copy(generation = inputGeneration)
        }
        if (!rawJoystick.fingerDown) {
            lastCommittedDir = null
        }
        return if (dir != null) input.copy(generation = inputGeneration) else input
    }

    private fun sendLocalInputToHost() {
        if (useWsAuthority) {
            sendLocalInputViaWs()
            return
        }
        if (isHost) return
        val input = PacMazeOnlineInput.sampleDirect(rawJoystick, 0L, inputGeneration)
        val dir = resolveOutboundDirection(input)
        val attack = pendingAttack
        val now = System.currentTimeMillis()
        val released = !rawJoystick.fingerDown && lastSentDir != null
        val dirChanged = dir != lastSentDir
        val due = now - lastInputSentMs >= WS_INPUT_INTERVAL_MS
        val holdRepeat = rawJoystick.fingerDown && dir != null && due
        if (!attack && !dirChanged && !released && !holdRepeat) return
        if (dir != null && dir != lastCommittedDir) {
            inputGeneration++
            lastCommittedDir = dir
        }
        if (released) lastCommittedDir = null
        pendingAttack = false
        lastSentDir = if (released) null else dir
        lastInputSentMs = now
        val payload = mutableMapOf<String, Any?>(
            "kind" to PacMazeOnlineInput.WIRE_KIND,
            "seq" to outboundSeq.incrementAndGet(),
            "attack" to attack,
        )
        if (released) {
            payload["release"] = true
        } else if (dir != null) {
            payload["dir"] = dir.name
        }
        fireMove(payload)
    }

    /** 摇杆按住期间锁存最后有效方向，避免 dead-zone 包把服务端输入清成 Inactive。 */
    private fun resolveOutboundDirection(input: PacMazeTickInput): Direction? {
        if (!rawJoystick.fingerDown) return null
        if (input.committed != null) return input.committed
        return lastCommittedDir
    }

    private fun sendLocalInputViaWs() {
        val input = PacMazeOnlineInput.sampleDirect(rawJoystick, 0L, inputGeneration)
        val dir = resolveOutboundDirection(input)
        val attack = pendingAttack
        val now = System.currentTimeMillis()
        val released = !rawJoystick.fingerDown && lastSentDir != null
        val dirChanged = dir != lastSentDir
        val due = now - lastInputSentMs >= WS_INPUT_INTERVAL_MS
        val holdRepeat = rawJoystick.fingerDown && dir != null && due
        if (!attack && !dirChanged && !released && !holdRepeat) return
        if (dir != null && dir != lastCommittedDir) {
            inputGeneration++
            lastCommittedDir = dir
        }
        if (released) lastCommittedDir = null
        val inputTick = if (::wsRollback.isInitialized) wsRollback.nextInputTick() else 1L
        if (attack && ::wsRollback.isInitialized) wsRollback.onLocalAttack(inputTick)
        pendingAttack = false
        lastSentDir = if (released) null else dir
        lastInputSentMs = now
        wsSession?.sendInput(
            tick = inputTick,
            dir = dir?.name,
            attack = attack,
            seq = outboundSeq.incrementAndGet(),
            release = released,
        )
    }

    private fun maybeBroadcastSnapshot(world: PacMazeWorldState) {
        if (!isHost) return
        if (world.tick - lastBroadcastTick < SNAPSHOT_EVERY_TICKS) return
        lastBroadcastTick = world.tick
        val payload = PacMazeStateSnapshot.encode(world)
        fireMove(payload)
    }

    private fun fireMove(payload: Map<String, Any?>) {
        val moveIndex = ++outboundMoveIndex
        viewModelScope.launch(Dispatchers.IO) {
            interactor.submitPacMoveAtIndex(roomId, moveIndex, payload)
                .onFailure { error ->
                    Log.w(TAG, "move failed idx=$moveIndex: ${error.message}")
                }
        }
    }

    private suspend fun handleMove(move: com.example.funlife.social.game.model.GameMoveDto) {
        if (move.moveIndex > lastProcessedMoveIndex) {
            lastProcessedMoveIndex = move.moveIndex
        }
        val obj = move.payload?.asJsonObject ?: return
        val kind = obj.get("kind")?.asString ?: return
        val cfg = _ui.value.matchConfig ?: return

        when (kind) {
            GameMoveKind.PAC_SURRENDER.wire -> handleSurrender(move.playerPbId, cfg)
            GameMoveKind.PAC_INPUT_DIRECT.wire -> {
                if (useWsAuthority || !isHost) return
                if (move.playerPbId == _ui.value.myPbId) return
                ingestGuestDirectInput(obj)
            }
            GameMoveKind.PAC_STATE_SNAPSHOT.wire -> {
                if (useWsAuthority || isHost) return
                if (move.playerPbId != cfg.hostPbId) return
                applySnapshot(obj, cfg)
            }
            GameMoveKind.PAC_INPUT_FRAME.wire -> {
                if (useWsAuthority || !isHost) return
                PacMazeBoardSync.parseInputMoves(listOf(move), cfg).forEach { parsed ->
                    if (parsed.entityId == peerEntityId) {
                        guestInputRef.set(parsed.input)
                        if (parsed.attack) guestAttackPending.set(true)
                    }
                }
            }
        }
    }

    private fun ingestGuestDirectInput(obj: JsonObject) {
        val map = obj.entrySet().associate { it.key to it.value?.let { v ->
            when {
                v.isJsonPrimitive && v.asJsonPrimitive.isBoolean -> v.asBoolean
                v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asNumber
                v.isJsonPrimitive -> v.asString
                else -> v.toString()
            }
        } }
        if (map["release"]?.toString()?.toBooleanStrictOrNull() == true) {
            guestInputRef.set(PacMazeTickInput.Inactive)
            return
        }
        val dirWire = map["dir"]?.toString()?.takeIf { it.isNotBlank() }
        val attack = map["attack"]?.toString()?.toBooleanStrictOrNull() == true
        if (dirWire != null) {
            val dir = runCatching { Direction.valueOf(dirWire) }.getOrNull()
            if (dir != null) {
                guestInputRef.set(PacMazeTickInput.committed(0L, dir))
            }
        }
        if (attack) guestAttackPending.set(true)
    }

    private fun applySnapshot(obj: JsonObject, cfg: PacMazeOnlineMatchConfig) {
        val template = worldTemplate ?: simulationWorld ?: return
        val map = jsonObjectToMap(obj)
        val decoded = PacMazeStateSnapshot.decode(map, template) ?: return
        val previous = simulationWorld ?: decoded
        simulationWorld = decoded
        lastSnapshotReceivedMs = System.currentTimeMillis()
        publishRenderFrame(previous = previous, blend = 0f)
        _ui.update {
            it.copy(
                world = decoded,
                lastSnapshotTick = decoded.tick,
                syncLagMs = System.currentTimeMillis() - sessionSimAnchorMs -
                    (decoded.tick * 1_000L / PacMazeConstants.TICKS_PER_SECOND),
            )
        }
        if (decoded.phase != PacMazePhase.PLAYING) {
            checkTerminal(decoded, cfg)
        }
    }

    private fun jsonObjectToMap(obj: JsonObject): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        obj.entrySet().forEach { (key, value) ->
            result[key] = when {
                value == null || value.isJsonNull -> null
                value.isJsonPrimitive -> {
                    val p = value.asJsonPrimitive
                    when {
                        p.isBoolean -> p.asBoolean
                        p.isNumber -> p.asNumber
                        else -> p.asString
                    }
                }
                value.isJsonArray -> value.asJsonArray.map { el ->
                    when {
                        el.isJsonObject -> jsonObjectToMap(el.asJsonObject)
                        el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asNumber
                        el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
                        el.isJsonPrimitive -> el.asString
                        else -> el.toString()
                    }
                }
                value.isJsonObject -> jsonObjectToMap(value.asJsonObject)
                else -> value.toString()
            }
        }
        return result
    }

    private fun updateGuestSyncLag() {
        if (lastSnapshotReceivedMs <= 0L) return
        val world = simulationWorld ?: return
        _ui.update {
            it.copy(
                syncLagMs = System.currentTimeMillis() - lastSnapshotReceivedMs,
                lastSnapshotTick = world.tick,
            )
        }
    }

    private fun publishRenderFrame(previous: PacMazeWorldState? = null, blend: Float) {
        val world = simulationWorld ?: return
        val prev = previous ?: _renderFrame.value?.current ?: world
        _renderFrame.value = PacMazeRenderFrame(current = world, previous = prev, blend = blend)
        _ui.update { it.copy(world = world) }
    }

    private fun checkTerminal(world: PacMazeWorldState, cfg: PacMazeOnlineMatchConfig) {
        if (!_ui.value.showResult && world.phase != PacMazePhase.PLAYING) {
            simRunning = false
            if (isHost || useWsAuthority) {
                viewModelScope.launch { finalizeMatch(world, cfg) }
            } else {
                _ui.update {
                    it.copy(
                        showResult = true,
                        resultTitle = when (world.phase) {
                            PacMazePhase.LEVEL_CLEAR -> if (world.onlineWinnerEntityId == localEntityId) "胜利！" else "惜败"
                            PacMazePhase.GAME_OVER -> "游戏结束"
                            else -> "对局结束"
                        },
                        resultMessage = "得分 ${if (localEntityId == cfg.hostEntityId) world.playerScoreA else world.playerScoreB}",
                    )
                }
            }
        }
    }

    private suspend fun finalizeMatch(world: PacMazeWorldState, cfg: PacMazeOnlineMatchConfig) {
        val winnerEntity = world.onlineWinnerEntityId
        val winnerPbId = when (winnerEntity) {
            cfg.hostEntityId -> cfg.hostPbId
            cfg.guestEntityId -> cfg.guestPbId
            else -> null
        }
        val isDraw = winnerPbId == null && cfg.mode == PacMazeOnlineMatchMode.VERSUS_DUEL
        val iWon = winnerPbId == _ui.value.myPbId
        val title = when {
            isDraw -> "平局"
            iWon -> "胜利！"
            else -> "惜败"
        }
        val message = buildString {
            append("得分 ${if (localEntityId == cfg.hostEntityId) world.playerScoreA else world.playerScoreB}")
            append(" · 用时 ${world.onlineElapsedSeconds}s")
        }
        var eloDelta = 0
        if (cfg.mode == PacMazeOnlineMatchMode.VERSUS_DUEL) {
            val myElo = prefs.versusRating(userId)
            val (winDelta, loseDelta) = PacMazeEloCalculator.calculateVersusDelta(
                winnerElo = myElo,
                loserElo = myElo,
                winnerGames = prefs.versusGames(userId),
                loserGames = prefs.versusGames(userId),
                isDraw = isDraw,
            )
            eloDelta = when {
                isDraw -> winDelta
                iWon -> winDelta
                else -> loseDelta
            }
            prefs.recordVersusResult(userId, won = iWon, draw = isDraw, eloDelta = eloDelta)
        }
        _ui.update {
            it.copy(
                showResult = true,
                resultTitle = title,
                resultMessage = message,
                eloDelta = eloDelta,
                world = world,
            )
        }
        if (!isHost) return
        interactor.finishPacMatch(
            roomId = roomId,
            result = PacMazeMatchResultWire(
                winnerPbId = winnerPbId,
                endReason = world.onlineEndReason ?: PacMazeOnlineEndReason.NORMAL,
                durationSec = world.onlineElapsedSeconds,
                scores = mapOf(
                    cfg.hostPbId to world.playerScoreA,
                    cfg.guestPbId to world.playerScoreB,
                ),
                draw = isDraw,
            ),
        )
    }

    private suspend fun handleSurrender(pbId: String, cfg: PacMazeOnlineMatchConfig) {
        if (!isHost) return
        val world = simulationWorld?.copy(
            phase = PacMazePhase.LEVEL_CLEAR,
            onlineWinnerEntityId = if (pbId == cfg.hostPbId) cfg.guestEntityId else cfg.hostEntityId,
            onlineEndReason = PacMazeOnlineEndReason.SURRENDER,
        ) ?: return
        simulationWorld = world
        maybeBroadcastSnapshot(world)
        finalizeMatch(world, cfg)
    }

    private fun showFinishedFromRoom(play: PacMazePlayState?) {
        val result = play?.result ?: return
        if (_ui.value.showResult) return
        val iWon = result.winnerPbId == _ui.value.myPbId
        _ui.update {
            it.copy(
                showResult = true,
                resultTitle = when {
                    result.draw -> "平局"
                    iWon -> "胜利！"
                    else -> "惜败"
                },
                resultMessage = "对局已结束",
                eloDelta = result.eloDelta[_ui.value.myPbId] ?: 0,
            )
        }
    }

    fun onJoystickSample(sample: PacMazeRawJoystickSample?) {
        rawJoystick = sample ?: PacMazeRawJoystickSample.Released
        if (canSendOnlineInput()) sendLocalInputToHost()
    }

    fun syncJoystickSample(offsetX: Float, offsetY: Float, maxRadius: Float, fingerDown: Boolean) {
        rawJoystick = PacMazeRawJoystickSample(
            offsetX = offsetX,
            offsetY = offsetY,
            maxRadius = maxRadius.coerceAtLeast(1f),
            fingerDown = fingerDown,
        )
        if (canSendOnlineInput()) sendLocalInputToHost()
    }

    fun resetJoystickInput() {
        rawJoystick = PacMazeRawJoystickSample.Released
        lastCommittedDir = null
    }

    fun onAttack() {
        pendingAttack = true
        if (canSendOnlineInput()) sendLocalInputToHost()
    }

    fun surrender() {
        viewModelScope.launch(Dispatchers.IO) {
            interactor.submitPacMove(roomId, mapOf("kind" to GameMoveKind.PAC_SURRENDER.wire))
            if (isHost) {
                val cfg = _ui.value.matchConfig ?: return@launch
                handleSurrender(_ui.value.myPbId, cfg)
            }
        }
    }

    fun consumeToast() {
        _ui.update { it.copy(toast = null) }
    }

    /** 每帧查询，避免 Compose LaunchedEffect 内 ui 快照过期。 */
    fun useServerInterpolation(): Boolean = _ui.value.useServerRenderBlend

    override fun onCleared() {
        GamePlaySyncManager.stopSession(roomId)
        wsWatchdogJob?.cancel()
        wsCollectJob?.cancel()
        wsSession?.stop()
        wsSession = null
        simRunning = false
        super.onCleared()
    }
}
