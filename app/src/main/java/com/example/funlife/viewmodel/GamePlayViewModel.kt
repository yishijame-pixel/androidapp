package com.example.funlife.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.social.game.engine.ForbiddenPoint
import com.example.funlife.social.game.engine.GomokuForbiddenRules
import com.example.funlife.social.game.engine.MoveValidation
import com.example.funlife.social.game.GamePlayCredentialGate
import com.example.funlife.social.game.GameRoomSyncCoordinator
import com.example.funlife.social.PocketBaseConnectionWarmer
import com.example.funlife.social.game.GamePlayInteractor
import com.example.funlife.social.game.GamePlaySyncManager
import com.example.funlife.social.game.SyncState
import com.example.funlife.social.drawws.DrawGuessLiveSync
import com.example.funlife.social.drawws.DrawGuessStrokeDispatchQueue
import com.example.funlife.social.drawws.DrawWsConfig
import com.example.funlife.social.game.engine.DrawGuessSync
import com.example.funlife.social.game.engine.GomokuBoardSync
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.GomokuMove
import com.example.funlife.social.game.model.GomokuPlayState
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.model.LobbyMember
import com.example.funlife.social.game.model.LobbyMemberStatus
import com.example.funlife.ui.screens.socialgame.play.DrawGuessBubbleManager
import com.example.funlife.ui.screens.socialgame.play.DrawGuessBubbleMessage
import com.example.funlife.ui.screens.socialgame.play.DrawGuessCanvasPublishPolicy
import com.example.funlife.ui.screens.socialgame.play.DrawGuessLayerFingerprint
import com.example.funlife.utils.AvatarImageLoader
import com.google.gson.JsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 本地落子同步状态（乐观 UI） */
enum class GomokuPlacementSyncState {
    Sending,
    Sent,
    Failed,
}

data class GomokuPendingPlacement(
    val x: Int,
    val y: Int,
    val state: GomokuPlacementSyncState,
    val errorMessage: String? = null,
)

data class DrawPendingStroke(
    val seq: Int,
    val points: List<Pair<Float, Float>>,
    val color: String,
    val width: Float,
    val state: GomokuPlacementSyncState,
    val strokeId: String? = null,
    val errorMessage: String? = null,
)

data class GamePlayUiState(
    val room: LocalGameRoomDraft? = null,
    val gameId: String = "",
    val status: GameRoomStatus = GameRoomStatus.WAITING,
    val gomoku: GomokuPlayState? = null,
    val drawGuess: DrawGuessPlayState? = null,
    val currentTurnPbId: String? = null,
    val winnerPbId: String? = null,
    val moves: List<GameMoveDto> = emptyList(),
    /** 你画我猜：当前轮画布（账本回放 + 乐观笔画） */
    val drawStrokes: List<DrawStrokeUi> = emptyList(),
    val drawClearToken: Int = 0,
    val myPbId: String? = null,
    val myDisplayName: String = "",
    val myLocalAvatarUri: String? = null,
    val pbAuthToken: String? = null,
    val identityReady: Boolean = false,
    val identityError: String? = null,
    val busy: Boolean = false,
    val toast: String? = null,
    /** 正在同步的落子（Sending / Failed 时展示叠加态与重试） */
    val pendingPlacement: GomokuPendingPlacement? = null,
    /** 你画我猜：失败笔画重试 */
    val pendingFailedStroke: DrawPendingStroke? = null,
    val syncState: SyncState = SyncState.IDLE,
    /** 禁手点列表（仅五子棋） */
    val forbiddenPoints: List<com.example.funlife.social.game.engine.ForbiddenPoint> = emptyList(),
    /** 游戏结束原因 */
    val endReason: String? = null,
    /** ELO 变化（仅对局结束后） */
    val eloChange: EloChangeInfo? = null,
    /** 你画我猜：头像旁气泡消息 */
    val drawGuessBubbles: List<DrawGuessBubbleMessage> = emptyList(),
    /** 首次进盘完成后不再退回全屏加载（防止同步抖动闪屏） */
    val bootstrapComplete: Boolean = false,
    /** 进局加载真实进度 0–100，100 表示可进盘 */
    val bootstrapProgress: Int = 0,
    /** 你画我猜：room_go 后允许作画/同步倒计时起点 */
    val drawRoundLive: Boolean = false,
    val drawRoundStartedAtMs: Long = 0L,
    val drawWsPeerCount: Int = 0,
    val drawWsReadyCount: Int = 0,
)

/** ELO 变化信息 */
data class EloChangeInfo(
    val myOldElo: Int,
    val myNewElo: Int,
    val myDelta: Int,
    val opponentOldElo: Int,
    val opponentNewElo: Int,
    val opponentDelta: Int,
    val isWinner: Boolean,
)

class GamePlayViewModel(
    application: Application,
    private val currentUserId: Long,
    private val roomId: String,
    myDisplayName: String = "",
) : AndroidViewModel(application) {

    private val interactor = GamePlayInteractor(application, currentUserId)
    private val app = application as com.example.funlife.FunLifeApplication

    private val _ui = MutableStateFlow(GamePlayUiState(myDisplayName = myDisplayName))
    val ui: StateFlow<GamePlayUiState> = _ui.asStateFlow()

    private val _rawRoom = interactor.observeRooms()
        .map { list -> list.firstOrNull { it.roomId == roomId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var syncCollectJob: Job? = null
    private var drawWsConnectJob: Job? = null
    private var drawWsCollectJob: Job? = null
    private var bootstrapPollJob: Job? = null
    private var wsBootstrapStartedMs = 0L
    private var roomSyncTimedOut = false
    private var drawWsReadySentRound = 0
    private var reconcileJob: Job? = null
    /** moveLedger 回放缓存，避免 WS 分片 60Hz 时重复 parseStrokes */
    private var ledgerStrokeCacheVersion = 0
    private val syncMutex = Mutex()
    private val refreshMutex = Mutex()
    private val moveLedger = CopyOnWriteArrayList<GameMoveDto>()
    private val pendingPlacements = CopyOnWriteArrayList<Pair<Int, Int>>()
    private val pendingDrawStrokes = CopyOnWriteArrayList<DrawStrokeUi>()
    /** 抬手后 PB 入账前本地保留，防止 pending 摘掉后画布闪空 */
    private val localFinishedStrokes = CopyOnWriteArrayList<DrawStrokeUi>()
    private val strokeSubmitMutex = Mutex()
    private val liveStrokeSeq = AtomicInteger(0)
    private var isClearingCanvas = false
    private var drawTimerJob: Job? = null
    private var lastRoomDto: GameRoomDto? = null
    private var isPlacingMove = false
    private var watermarkMoveIndex = 0
    private var lastIngestAtMs = System.currentTimeMillis()
    private var ledgerStrokeCacheAtVersion = -1
    private var ledgerStrokeCacheRound = -1
    private var ledgerStrokeCacheSinceMove = -1
    private var ledgerStrokeCache: List<DrawStrokeUi> = emptyList()
    /** 乐观清屏：在 PB draw_clear 入账前阻止 ledger/live 把旧笔画刷回画布 */
    private var forceEmptyCanvas = false
    /** 清屏后双方暂隐藏 ledger/live，直到 draw_clear 入账 */
    private var suppressCanvasUntilClearAck = false
    /** 清屏水位：此 moveIndex 之前的 ledger 笔画在 ack 前不参与 UI */
    private var clearWaterlineMoveIndex = 0
    /** 猜词方收到对方 WS clear 后，在 PB 入账前屏蔽 ledger 旧笔画 */
    private var passiveClearDisplayWaterline = 0
    private var processedGuessCount = 0
    private var lastDrawGuessRound = 0
    private val pbPreviewAtMs = ConcurrentHashMap<String, Long>()
    private companion object {
        private const val CANVAS_LOG = "DrawGuessCanvas"
        /** WS 未就绪：猜词方 SSE 兜底间隔 */
        const val PB_PREVIEW_INTERVAL_MS = 150L
        /** WS 已就绪但猜词方可能尚未 join：低频双写兜底 */
        const val PB_HEDGE_INTERVAL_MS = 400L
        /** 作画阶段开局窗口内启用 PB 双写 */
        const val OPENING_HEDGE_MS = 25_000L
        /** 等待全员 WS ready + room_go */
        const val ROOM_SYNC_GO_TIMEOUT_MS = 10_000L
        const val WS_BOOTSTRAP_HARD_CAP_MS = 12_000L
    }

    private fun traceCanvas(message: String) {
        Log.i(CANVAS_LOG, "room=$roomId $message")
    }

    init {
        DrawGuessStrokeDispatchQueue.ensureStarted()
        viewModelScope.launch {
            _rawRoom.collect { draft -> mergeRoomDraft(draft) }
        }
        viewModelScope.launch {
            app.database.userAvatarDao().getUserAvatar(currentUserId).collect { avatar ->
                val uri = avatar?.avatarUri?.takeIf { it.isNotBlank() }
                if (_ui.value.myLocalAvatarUri != uri) {
                    _ui.value = _ui.value.copy(myLocalAvatarUri = uri)
                }
            }
        }
        seedIdentityFromLocalLink()
        ensureIdentity()
        PocketBaseConnectionWarmer.warmAsync(getApplication())
        startPlaySync()
        ensureDrawWsCollectors()
        startDrawWsConnectLoop()
        startReconcileLoop()
        startDrawTimerLoop()
        startSyncStateCollector()
        refreshProfilesOnce()
        startBootstrapPoll()
        startBootstrapWatchdog()
    }

    private fun startSyncStateCollector() {
        viewModelScope.launch {
            GamePlaySyncManager.syncState.collect { sync ->
                if (_ui.value.syncState != sync) {
                    _ui.value = _ui.value.copy(syncState = sync)
                }
            }
        }
    }

    /** 本地社交链接立即注入 pbId，弱网不阻塞首屏 */
    private fun seedIdentityFromLocalLink() {
        viewModelScope.launch {
            val link = app.database.socialDao().getLink(currentUserId) ?: return@launch
            val pbId = link.pbRecordId.takeIf { it.isNotBlank() } ?: return@launch
            if (_ui.value.myPbId == pbId && _ui.value.identityReady) return@launch
            publishUi(
                _ui.value.copy(
                    myPbId = pbId,
                    identityReady = true,
                    identityError = null,
                ),
            )
        }
    }

    /** 进房立即拉 expand 资料，修正房主/对手头像（0 延迟本地头像 + 公网资料补齐） */
    private fun refreshProfilesOnce() {
        viewModelScope.launch {
            repeat(3) { attempt ->
                interactor.refreshPlayState(roomId, updateLocalCache = attempt == 0)
                    .onSuccess { (dto, moves) ->
                        hydrateProfilesFromRoom(dto)
                        ingestMoves(room = dto, incomingMoves = moves)
                        return@launch
                    }
                    .onFailure {
                        if (attempt < 2) delay(800L * (attempt + 1))
                    }
            }
            if (!_ui.value.bootstrapComplete) dismissBootstrapLoading()
        }
    }

    /** 轮询 bootstrap 进度，避免 transport 订阅被 cancel 后错过 joined */
    private fun startBootstrapPoll() {
        bootstrapPollJob?.cancel()
        bootstrapPollJob = viewModelScope.launch {
            while (!_ui.value.bootstrapComplete) {
                tickBootstrapProgress()
                delay(200L)
            }
        }
    }

    private fun tickBootstrapProgress() {
        if (_ui.value.bootstrapComplete) return
        ensureDrawGuessShellIfNeeded()
        maybeSendDrawWsReady()
        val progress = computeBootstrapProgress()
        when {
            isBootstrapReady(_ui.value) -> completeBootstrap()
            progress != _ui.value.bootstrapProgress ->
                _ui.value = _ui.value.copy(bootstrapProgress = progress)
        }
    }

    private fun completeBootstrap() {
        val cur = _ui.value
        val round = cur.drawGuess?.round ?: 1
        val live = cur.drawRoundLive ||
            !DrawWsConfig.isEnabled() ||
            !isDrawGuessRoom(cur) ||
            roomSyncTimedOut ||
            DrawGuessLiveSync.isRoomGoForRound(round)
        val startedAt = when {
            cur.drawRoundStartedAtMs > 0L -> cur.drawRoundStartedAtMs
            cur.drawGuess != null && live -> cur.drawGuess.phaseStartedAtMs.coerceAtLeast(0L)
            else -> 0L
        }
        val play = cur.drawGuess?.let { dg ->
            if (live && startedAt > 0L) dg.copy(phaseStartedAtMs = startedAt) else dg
        }
        publishUi(
            cur.copy(
                bootstrapProgress = 100,
                bootstrapComplete = true,
                drawRoundLive = live,
                drawRoundStartedAtMs = if (live) startedAt else cur.drawRoundStartedAtMs,
                drawGuess = play,
            ),
        )
        traceCanvas(
            "bootstrap complete live=$live round=$round ws=${DrawGuessLiveSync.isWsActive()} " +
                "roomGo=${DrawGuessLiveSync.isRoomGoForRound(round)} timeout=$roomSyncTimedOut",
        )
    }

    /** 进局：等 WS + room_go（或超时）后一起进盘 */
    private fun startBootstrapWatchdog() {
        viewModelScope.launch {
            val hardCapMs = if (DrawWsConfig.isEnabled() && isDrawGuessRoom(_ui.value)) {
                WS_BOOTSTRAP_HARD_CAP_MS
            } else {
                4_000L
            }
            val startMs = System.currentTimeMillis()
            while (System.currentTimeMillis() - startMs < hardCapMs && !_ui.value.bootstrapComplete) {
                ensureDrawGuessShellIfNeeded()
                tickBootstrapProgress()
                if (_ui.value.bootstrapComplete) return@launch
                val elapsed = System.currentTimeMillis() - startMs
                val round = _ui.value.drawGuess?.round ?: 1
                if (isPlayReady(_ui.value) && isDrawGuessRoom(_ui.value) && DrawWsConfig.isEnabled()) {
                    if (DrawGuessLiveSync.isRoomGoForRound(round)) {
                        applyRoomGo(DrawGuessLiveSync.roomSync.value.goAtMs, round)
                        completeBootstrap()
                        return@launch
                    }
                    if (elapsed >= ROOM_SYNC_GO_TIMEOUT_MS && !roomSyncTimedOut) {
                        roomSyncTimedOut = true
                        Log.w("DrawGuessCanvas", "room sync timeout room=$roomId — enter without room_go")
                        forceRoomSyncExit()
                        return@launch
                    }
                } else if (isPlayReady(_ui.value) && !DrawWsConfig.isEnabled()) {
                    completeBootstrap()
                    return@launch
                } else if (isPlayReady(_ui.value) && !isDrawGuessRoom(_ui.value)) {
                    completeBootstrap()
                    return@launch
                }
                delay(200L)
            }
            if (!_ui.value.bootstrapComplete) {
                Log.w("DrawGuessCanvas", "bootstrap hard cap room=$roomId — force enter")
                forceRoomSyncExit()
            }
        }
    }

    private fun maybeSendDrawWsReady() {
        if (!DrawWsConfig.isEnabled()) return
        if (!isDrawGuessRoom(_ui.value)) return
        if (_ui.value.status != GameRoomStatus.PLAYING) return
        val play = _ui.value.drawGuess ?: return
        if (!DrawGuessLiveSync.isWsActive()) return
        if (drawWsReadySentRound == play.round) return
        DrawGuessLiveSync.sendReady(play.round)
        drawWsReadySentRound = play.round
        Log.d("DrawGuessCanvas", "ws ready sent round=${play.round} room=$roomId")
    }

    private fun applyRoomGo(serverTs: Long, round: Int) {
        if (serverTs <= 0L) return
        val play = _ui.value.drawGuess ?: return
        if (play.round != round) return
        val sync = DrawGuessLiveSync.roomSync.value
        val effectiveStart = maxOf(play.phaseStartedAtMs, serverTs)
        publishUi(
            _ui.value.copy(
                drawRoundLive = true,
                drawRoundStartedAtMs = effectiveStart,
                drawWsPeerCount = sync.peerCount,
                drawWsReadyCount = sync.readyCount,
                drawGuess = play.copy(phaseStartedAtMs = effectiveStart),
            ),
        )
    }

    private fun forceRoomSyncExit() {
        val play = _ui.value.drawGuess
        val now = System.currentTimeMillis()
        val round = play?.round ?: 1
        val effectiveStart = maxOf(play?.phaseStartedAtMs ?: now, now)
        publishUi(
            _ui.value.copy(
                drawRoundLive = true,
                drawRoundStartedAtMs = effectiveStart,
                drawGuess = play?.copy(phaseStartedAtMs = effectiveStart),
            ),
        )
        if (DrawWsConfig.isEnabled() && isDrawGuessRoom(_ui.value) && !DrawGuessLiveSync.isWsActive()) {
            Log.w("DrawGuessCanvas", "enter without ws room=$roomId round=$round")
        }
        completeBootstrap()
    }

    private fun ensureDrawGuessShellIfNeeded() {
        if (_ui.value.drawGuess != null) return
        val draft = _ui.value.room
        val room = lastRoomDto
        if (draft?.gameId == "draw_guess" || room?.gameType == "draw_guess") {
            val shellDraft = draft ?: draftFromRoomDto(room!!)
            publishUi(
                _ui.value.copy(
                    gameId = "draw_guess",
                    drawGuess = buildDrawGuessShellFromDraft(shellDraft),
                ),
            )
            syncDrawWsRoomContext(republishCanvas = true)
            return
        }
        if (room?.gameType == "draw_guess" && room.status == GameRoomStatus.PLAYING) {
            publishUi(
                _ui.value.copy(
                    gameId = "draw_guess",
                    drawGuess = resolveDrawGuessPlay(room, _ui.value.myPbId)
                        ?: DrawGuessPlayState(
                            drawerPbId = room.currentTurnPbId?.takeIf { it.isNotBlank() } ?: room.hostPbId,
                            scores = drawGuessScoresShell(room),
                            phaseStartedAtMs = System.currentTimeMillis(),
                        ),
                ),
            )
            syncDrawWsRoomContext(republishCanvas = true)
        }
    }

    fun computeBootstrapProgress(state: GamePlayUiState = _ui.value): Int {
        if (!isIdentitySatisfied(state)) return 15
        if (!isDrawGuessRoom(state)) {
            return if (isPlayReady(state)) 100 else 45
        }
        if (state.drawGuess == null) return 35
        if (!DrawWsConfig.isEnabled()) return 100
        val round = state.drawGuess.round
        if (DrawGuessLiveSync.isRoomGoForRound(round) || roomSyncTimedOut) return 100
        val sync = DrawGuessLiveSync.roomSync.value
        if (sync.peerCount >= 2 && sync.readyCount >= 2) return 95
        if (DrawGuessLiveSync.isWsActive()) return 85
        val started = wsBootstrapStartedMs
        if (started == 0L) return 60
        val elapsed = System.currentTimeMillis() - started
        val sub = (elapsed * 24 / ROOM_SYNC_GO_TIMEOUT_MS).toInt().coerceIn(0, 24)
        return 60 + sub
    }

    /** 在 tick/watchdog 中调用，标记 WS 等待起点（勿在 Composable 里调用 computeBootstrapProgress） */
    private fun computeBootstrapProgress(): Int {
        if (isDrawGuessRoom(_ui.value) &&
            _ui.value.drawGuess != null &&
            DrawWsConfig.isEnabled() &&
            !DrawGuessLiveSync.isWsActive() &&
            !roomSyncTimedOut &&
            wsBootstrapStartedMs == 0L
        ) {
            wsBootstrapStartedMs = System.currentTimeMillis()
        }
        return computeBootstrapProgress(_ui.value)
    }

    fun isBootstrapReady(state: GamePlayUiState = _ui.value): Boolean {
        if (!isPlayReady(state)) return false
        if (!isDrawGuessRoom(state)) return true
        if (state.drawGuess == null) return false
        if (!DrawWsConfig.isEnabled()) return true
        val round = state.drawGuess.round
        return DrawGuessLiveSync.isRoomGoForRound(round) || roomSyncTimedOut
    }

    fun kickBootstrap() {
        viewModelScope.launch {
            ensureDrawGuessShellIfNeeded()
            if (!_ui.value.bootstrapComplete) refreshNow(showBusy = false)
            tickBootstrapProgress()
        }
    }

    private fun isDrawGuessRoom(state: GamePlayUiState = _ui.value): Boolean =
        state.gameId == "draw_guess" || lastRoomDto?.gameType == "draw_guess"

    fun bootstrapPhaseLabel(state: GamePlayUiState = _ui.value): String = when {
        !isIdentitySatisfied(state) -> "验证身份…"
        !isDrawGuessRoom(state) && !isPlayReady(state) -> "同步对局…"
        state.drawGuess == null -> "同步房间状态…"
        DrawWsConfig.isEnabled() && !DrawGuessLiveSync.isWsActive() && !roomSyncTimedOut ->
            "连接笔画通道…"
        DrawWsConfig.isEnabled() && DrawGuessLiveSync.isWsActive() &&
            state.drawGuess != null &&
            !DrawGuessLiveSync.isRoomGoForRound(state.drawGuess.round) && !roomSyncTimedOut ->
            "等待全员就绪…"
        DrawWsConfig.isEnabled() && state.drawGuess != null &&
            DrawGuessLiveSync.isRoomGoForRound(state.drawGuess.round) ->
            "全员就绪"
        else -> "即将开始…"
    }

    fun bootstrapSubtitle(state: GamePlayUiState = _ui.value): String = when {
        isDrawGuessRoom(state) && DrawWsConfig.isEnabled() &&
            DrawGuessLiveSync.isRoomGoForRound(state.drawGuess?.round ?: 1) ->
            "低延迟同步已就绪，即将开始"
        isDrawGuessRoom(state) && DrawWsConfig.isEnabled() -> {
            val sync = DrawGuessLiveSync.roomSync.value
            "已连接 ${sync.peerCount}/${sync.expectedPeers} · 就绪 ${sync.readyCount}/${sync.expectedPeers}"
        }
        else -> "正在同步对局数据"
    }

    /** UI/看门狗统一出口：超时或异常时进盘 */
    fun dismissBootstrapLoading() {
        forceRoomSyncExit()
    }

    /** 仅当头像/昵称/席位变化时更新 room，避免轮询写库导致头像闪动 */
    private fun mergeRoomDraft(incoming: LocalGameRoomDraft?) {
        if (incoming == null) {
            if (_ui.value.room != null) {
                _ui.value = _ui.value.copy(room = null)
            }
            return
        }
        val prev = _ui.value.room
        val profileChanged = prev == null || roomProfileChanged(prev, incoming)
        val statusAdvanced = incoming.status == GameRoomStatus.PLAYING &&
            _ui.value.status != GameRoomStatus.PLAYING
        if (!profileChanged && !statusAdvanced) return

        var next = _ui.value.copy(
            room = incoming,
            gameId = incoming.gameId.ifBlank { _ui.value.gameId },
        )
        if (statusAdvanced) {
            next = next.copy(status = incoming.status)
        }
        if (incoming.gameId == "draw_guess" &&
            incoming.status == GameRoomStatus.PLAYING &&
            next.drawGuess == null
        ) {
            next = next.copy(drawGuess = buildDrawGuessShellFromDraft(incoming))
        }
        publishUi(next)
        if (profileChanged) prefetchRoomAvatars(incoming)
    }

    private fun buildDrawGuessShellFromDraft(draft: LocalGameRoomDraft): DrawGuessPlayState {
        val host = draft.hostPbId
        val guest = draft.guestPbId.orEmpty()
        return DrawGuessPlayState(
            drawerPbId = host,
            scores = drawGuessScoresFromIds(host, guest),
            phaseStartedAtMs = System.currentTimeMillis(),
        )
    }

    private fun drawGuessScoresFromIds(host: String, guest: String): Map<String, Int> = buildMap {
        if (host.isNotBlank()) put(host, 0)
        if (guest.isNotBlank()) put(guest, 0)
    }

    private fun roomProfileChanged(
        prev: LocalGameRoomDraft,
        next: LocalGameRoomDraft,
    ): Boolean =
        prev.hostPbId != next.hostPbId ||
            prev.guestPbId != next.guestPbId ||
            prev.hostDisplayName != next.hostDisplayName ||
            prev.guestDisplayName != next.guestDisplayName ||
            prev.peerDisplayName != next.peerDisplayName ||
            prev.hostAvatarUrl != next.hostAvatarUrl ||
            prev.guestAvatarUrl != next.guestAvatarUrl ||
            prev.peerAvatarUrl != next.peerAvatarUrl ||
            prev.members != next.members

    /** 从 PB expand=host,guest 实时补齐头像（Realtime / 首屏刷新） */
    private fun hydrateProfilesFromRoom(dto: GameRoomDto) {
        val prev = _ui.value.room
        if (prev == null) {
            val draft = draftFromRoomDto(dto)
            _ui.value = _ui.value.copy(room = draft, gameId = dto.gameType)
            prefetchRoomAvatars(draft)
            return
        }
        val host = dto.hostProfile
        val guest = dto.guestProfile
        val hostName = host?.displayName?.ifBlank { host.funlifeUsername }
        val guestName = guest?.displayName?.ifBlank { guest.funlifeUsername }
        val peerPbId = if (dto.hostPbId == _ui.value.myPbId) dto.guestPbId else dto.hostPbId
        val peerName = when (peerPbId) {
            dto.guestPbId -> guestName
            dto.hostPbId -> hostName
            else -> null
        }
        val peerAvatar = when (peerPbId) {
            dto.guestPbId -> guest?.avatarUrl
            dto.hostPbId -> host?.avatarUrl
            else -> null
        }
        val profileById = buildMap {
            host?.let { put(it.id, it) }
            guest?.let { put(it.id, it) }
        }
        val mergedMembers = prev.members.map { member ->
            val remote = profileById[member.pbId]
            if (remote == null) member
            else member.copy(
                displayName = remote.displayName.ifBlank { remote.funlifeUsername }
                    .takeIf { it.isNotBlank() } ?: member.displayName,
                avatarUrl = remote.avatarUrl?.takeIf { it.isNotBlank() } ?: member.avatarUrl,
            )
        }
        val next = prev.copy(
            hostDisplayName = hostName?.takeIf { it.isNotBlank() } ?: prev.hostDisplayName,
            hostAvatarUrl = host?.avatarUrl?.takeIf { it.isNotBlank() } ?: prev.hostAvatarUrl,
            guestDisplayName = guestName?.takeIf { it.isNotBlank() } ?: prev.guestDisplayName,
            guestAvatarUrl = guest?.avatarUrl?.takeIf { it.isNotBlank() } ?: prev.guestAvatarUrl,
            peerDisplayName = peerName?.takeIf { it.isNotBlank() } ?: prev.peerDisplayName,
            peerAvatarUrl = peerAvatar?.takeIf { it.isNotBlank() } ?: prev.peerAvatarUrl,
            members = mergedMembers,
        )
        if (prev == next) return
        _ui.value = _ui.value.copy(room = next)
        prefetchRoomAvatars(next)
    }

    private fun prefetchRoomAvatars(room: LocalGameRoomDraft?) {
        if (room == null) return
        val token = _ui.value.pbAuthToken
        val urls = buildList {
            add(room.hostAvatarUrl)
            add(room.guestAvatarUrl)
            add(room.peerAvatarUrl)
            room.members.forEach { add(it.avatarUrl) }
        }
        viewModelScope.launch {
            AvatarImageLoader.warmAll(getApplication(), urls, token)
        }
    }

    private fun draftFromRoomDto(dto: GameRoomDto): LocalGameRoomDraft {
        val host = dto.hostProfile
        val guest = dto.guestProfile
        val hostName = host?.displayName?.ifBlank { host.funlifeUsername }
        val guestName = guest?.displayName?.ifBlank { guest.funlifeUsername }
        val peerPbId = if (dto.hostPbId == _ui.value.myPbId) dto.guestPbId else dto.hostPbId
        val peerName = when (peerPbId) {
            dto.guestPbId -> guestName
            dto.hostPbId -> hostName
            else -> null
        }
        val peerAvatar = when (peerPbId) {
            dto.guestPbId -> guest?.avatarUrl
            dto.hostPbId -> host?.avatarUrl
            else -> null
        }
        val members = buildList {
            if (dto.hostPbId.isNotBlank()) {
                add(
                    LobbyMember(
                        pbId = dto.hostPbId,
                        seat = 0,
                        status = LobbyMemberStatus.JOINED,
                        displayName = hostName,
                        avatarUrl = host?.avatarUrl,
                    ),
                )
            }
            dto.guestPbId?.takeIf { it.isNotBlank() }?.let { guestId ->
                add(
                    LobbyMember(
                        pbId = guestId,
                        seat = 1,
                        status = LobbyMemberStatus.JOINED,
                        displayName = guestName,
                        avatarUrl = guest?.avatarUrl,
                    ),
                )
            }
        }
        val entry = SocialGameCatalog.find(dto.gameType)
        return LocalGameRoomDraft(
            roomId = dto.id,
            roomCode = dto.roomCode,
            gameId = dto.gameType,
            gameTitle = entry?.title ?: dto.gameType,
            inviteMode = dto.inviteMode,
            status = dto.status,
            hostPbId = dto.hostPbId,
            guestPbId = dto.guestPbId,
            hostDisplayName = hostName,
            hostAvatarUrl = host?.avatarUrl,
            guestDisplayName = guestName,
            guestAvatarUrl = guest?.avatarUrl,
            peerDisplayName = peerName,
            peerAvatarUrl = peerAvatar,
            members = members,
            createdAtMs = dto.createdAtMs,
        )
    }

    fun ensureIdentity() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(identityError = null)
            interactor.ensurePlayCredentials()
                .onSuccess { cred -> applyCredentials(cred) }
                .onFailure { err ->
                    val keepReady = !_ui.value.myPbId.isNullOrBlank()
                    _ui.value = _ui.value.copy(
                        identityReady = keepReady,
                        identityError = interactor.mapError(err),
                    )
                }
        }
    }

    private fun isIdentitySatisfied(state: GamePlayUiState): Boolean =
        state.identityReady || !state.myPbId.isNullOrBlank()

    private fun isPlayReady(state: GamePlayUiState = _ui.value): Boolean {
        if (!isIdentitySatisfied(state)) return false
        if (isDrawGuessRoom(state)) return state.drawGuess != null
        return when (state.gameId) {
            "gomoku" -> state.gomoku != null
            else -> state.gameId.isNotBlank()
        }
    }

    /** 首次展示棋盘后锁定，避免 identity/同步抖动退回加载页 */
    private fun publishUi(next: GamePlayUiState) {
        _ui.value = when {
            next.bootstrapComplete -> next.copy(bootstrapProgress = 100)
            isBootstrapReady(next) -> {
                val round = next.drawGuess?.round ?: 1
                val live = next.drawRoundLive ||
                    !DrawWsConfig.isEnabled() ||
                    !isDrawGuessRoom(next) ||
                    roomSyncTimedOut ||
                    DrawGuessLiveSync.isRoomGoForRound(round)
                next.copy(
                    bootstrapComplete = true,
                    bootstrapProgress = 100,
                    drawRoundLive = live,
                )
            }
            else -> next
        }
    }

    /** transport/live 订阅只建一次，避免 applyCredentials 重连时掐掉 joined 回调 */
    private fun ensureDrawWsCollectors() {
        if (!DrawWsConfig.isEnabled()) return
        if (drawWsCollectJob?.isActive == true) return
        drawWsCollectJob = viewModelScope.launch {
            var lastTransport = DrawGuessLiveSync.Transport.POCKETBASE
            launch {
                DrawGuessLiveSync.transport.collect { transport ->
                    tickBootstrapProgress()
                    if (transport == DrawGuessLiveSync.Transport.WEBSOCKET &&
                        lastTransport != DrawGuessLiveSync.Transport.WEBSOCKET
                    ) {
                        syncDrawWsRoomContext(republishCanvas = true)
                        maybeSendDrawWsReady()
                        Log.d("DrawGuessCanvas", "ws ready room=$roomId — flush live canvas")
                    }
                    lastTransport = transport
                }
            }
            launch {
                DrawGuessLiveSync.roomSync.collect { sync ->
                    val round = _ui.value.drawGuess?.round ?: 1
                    publishUi(
                        _ui.value.copy(
                            drawWsPeerCount = sync.peerCount,
                            drawWsReadyCount = sync.readyCount,
                        ),
                    )
                    if (sync.goAtMs > 0L && sync.goRound == round && !_ui.value.drawRoundLive) {
                        applyRoomGo(sync.goAtMs, round)
                        if (!_ui.value.bootstrapComplete) {
                            completeBootstrap()
                        }
                    }
                    tickBootstrapProgress()
                }
            }
            launch {
                // stroke_end 后 bitmap 层直读 finalized live，不再 publishUi 触发整树重组闪烁
                DrawGuessLiveSync.strokeFinalizeNonce.collect {
                    if (_ui.value.drawGuess == null) return@collect
                    if (isDrawerSide()) return@collect
                    if (isAwaitingClearAck() || forceEmptyCanvas) return@collect
                    val round = currentDrawRound()
                    if (passiveClearDisplayWaterline > 0) {
                        passiveClearDisplayWaterline = 0
                        traceCanvas("passive clear waterline released finalize round=$round")
                    }
                }
            }
            launch {
                DrawGuessLiveSync.clearNonce.collect {
                    if (_ui.value.drawGuess == null) return@collect
                    invalidateLedgerStrokeCache()
                    pbPreviewAtMs.clear()
                    if (isClearingCanvas) {
                        if (isDrawerSide()) {
                            pendingDrawStrokes.clear()
                        }
                        // 用户主动清屏：等待 PB draw_clear 入账
                        clearWaterlineMoveIndex = maxOf(
                            clearWaterlineMoveIndex,
                            GomokuBoardSync.maxMoveIndex(moveLedger.toList()) + 1,
                        )
                        suppressCanvasUntilClearAck = true
                        if (isDrawerSide()) {
                            forceEmptyCanvas = true
                            localFinishedStrokes.clear()
                        }
                        traceCanvas(
                            "ws clear nonce user waterline=$clearWaterlineMoveIndex suppress=true",
                        )
                        publishUi(
                            _ui.value.copy(
                                drawStrokes = emptyList(),
                                drawClearToken = _ui.value.drawClearToken,
                            ),
                        )
                    } else {
                        // 被动 WS clear（对方清屏）：仅猜词方响应；画家忽略（清屏必走 isClearingCanvas）
                        if (isDrawerSide()) {
                            traceCanvas("ws clear nonce passive ignored drawer side")
                            return@collect
                        }
                        val ledgerMax = GomokuBoardSync.maxMoveIndex(moveLedger.toList())
                        if (ledgerMax <= 0) {
                            traceCanvas("ws clear nonce passive skipped empty ledger")
                            return@collect
                        }
                        passiveClearDisplayWaterline = ledgerMax + 1
                        traceCanvas(
                            "ws clear nonce passive round=${currentDrawRound()} " +
                                "waterline=$passiveClearDisplayWaterline ledgerMax=$ledgerMax",
                        )
                        // 勿 bump drawClearToken：入账会用账本 token，token 回跳会清空 bitmap 层导致笔画消失
                        publishUi(
                            _ui.value.copy(
                                drawStrokes = emptyList(),
                                drawClearToken = DrawGuessSync.clearToken(
                                    moveLedger.toList(),
                                    currentDrawRound(),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun startDrawWsConnectLoop() {
        if (!DrawWsConfig.isEnabled()) return
        drawWsConnectJob?.cancel()
        drawWsConnectJob = viewModelScope.launch {
            while (true) {
                val playHint = _ui.value.drawGuess
                val roomHint = lastRoomDto
                val roundHint = playHint?.round ?: roomHint?.gameState?.drawGuess?.round ?: 1
                val drawerHint = playHint?.drawerPbId?.takeIf { it.isNotBlank() }
                    ?: roomHint?.gameState?.drawGuess?.drawerPbId?.takeIf { it.isNotBlank() }
                    ?: roomHint?.currentTurnPbId?.takeIf { it.isNotBlank() }
                    ?: roomHint?.hostPbId.orEmpty()
                if (DrawGuessLiveSync.isConnected(roomId) || DrawGuessLiveSync.isPending(roomId)) {
                    val token = _ui.value.pbAuthToken?.takeIf { it.isNotBlank() }
                    if (!token.isNullOrBlank()) {
                        DrawGuessLiveSync.start(
                            scope = viewModelScope,
                            roomId = roomId,
                            token = token,
                            round = roundHint,
                            drawerId = drawerHint,
                        )
                    }
                    syncDrawWsRoomContext()
                    delay(if (DrawGuessLiveSync.isConnected(roomId)) 2_000 else 200)
                    continue
                }
                var token: String? = _ui.value.pbAuthToken
                repeat(24) {
                    if (!token.isNullOrBlank()) return@repeat
                    delay(250)
                    token = _ui.value.pbAuthToken
                }
                val authToken = token?.takeIf { it.isNotBlank() }
                if (authToken == null) {
                    Log.w("DrawGuessCanvas", "ws wait token room=$roomId")
                    delay(2_000)
                    continue
                }
                val play = _ui.value.drawGuess
                val room = lastRoomDto
                if (play == null && room?.gameType != "draw_guess") {
                    delay(200)
                    continue
                }
                val round = play?.round ?: room?.gameState?.drawGuess?.round ?: 1
                val drawerId = play?.drawerPbId?.takeIf { it.isNotBlank() }
                    ?: room?.gameState?.drawGuess?.drawerPbId?.takeIf { it.isNotBlank() }
                    ?: room?.currentTurnPbId?.takeIf { it.isNotBlank() }
                    ?: room?.hostPbId.orEmpty()
                DrawGuessLiveSync.start(
                    scope = viewModelScope,
                    roomId = roomId,
                    token = authToken,
                    round = round,
                    drawerId = drawerId,
                )
                delay(800)
            }
        }
    }

    private fun startDrawWsIfNeeded() {
        ensureDrawWsCollectors()
        startDrawWsConnectLoop()
    }

    private fun startPlaySync() {
        GamePlaySyncManager.startSession(
            ctx = getApplication(),
            userId = currentUserId,
            roomId = roomId,
        )
        syncCollectJob?.cancel()
        syncCollectJob = viewModelScope.launch {
            launch {
                GamePlaySyncManager.moveEvents.collect { event ->
                    if (event.roomId != roomId) return@collect
                    val myPbId = _ui.value.myPbId
                    if (isPlacingMove && event.move.playerPbId == myPbId) return@collect
                    if (DrawGuessSync.isDrawStrokeMove(event.move)) {
                        ingestDrawStrokeFast(event.move)
                        return@collect
                    }
                    ingestMoves(incomingMoves = listOf(event.move))
                }
            }
            launch {
                GamePlaySyncManager.roomEvents.collect { event ->
                    if (event.roomId != roomId) return@collect
                    if (event.moves.isEmpty()) {
                        ingestMoves(room = event.room, incomingMoves = emptyList())
                        return@collect
                    }
                    val remoteMax = GomokuBoardSync.maxMoveIndex(event.moves)
                if (isPlacingMove || isClearingCanvas || GameRoomSyncCoordinator.isMutating(roomId)) {
                    if (remoteMax <= watermarkMoveIndex) return@collect
                }
                    ingestMoves(room = event.room, incomingMoves = event.moves)
                }
            }
        }
    }

    /** 断线/漏推送时定期对账：长时间无 ingest 则拉全量 room+moves */
    private fun startReconcileLoop() {
        reconcileJob?.cancel()
        reconcileJob = viewModelScope.launch {
            while (true) {
                delay(2_500L)
                if (isPlacingMove || isClearingCanvas || _ui.value.status != GameRoomStatus.PLAYING) continue
                val quietMs = System.currentTimeMillis() - lastIngestAtMs
                val quietThreshold = if (GamePlaySyncManager.syncState.value == SyncState.CONNECTED) {
                    10_000L
                } else {
                    5_000L
                }
                if (quietMs >= quietThreshold) {
                    GamePlaySyncManager.requestRefresh()
                    lastIngestAtMs = System.currentTimeMillis()
                }
            }
        }
    }

    fun refreshNow(showBusy: Boolean = true) {
        viewModelScope.launch {
            refreshMutex.withLock {
                refreshFromServer(showBusy = showBusy, force = true)
            }
        }
    }

    private suspend fun refreshFromServer(showBusy: Boolean = true, force: Boolean = false) {
        if (!force && (isPlacingMove || isClearingCanvas || pendingPlacements.isNotEmpty())) return
        if (showBusy) _ui.value = _ui.value.copy(busy = true)
        interactor.ensurePlayCredentials()
            .onSuccess { cred -> applyCredentials(cred) }
        interactor.refreshPlayState(roomId, updateLocalCache = force)
            .onSuccess { (dto, moves) -> ingestMoves(room = dto, incomingMoves = moves) }
            .onFailure {
                _ui.value = _ui.value.copy(
                    busy = false,
                    toast = interactor.mapError(it),
                )
            }
    }

    /**
     * 唯一同步入口：合并 move 账本 → 全量 replay 棋盘 → 发布 UI。
     * 绝不增量 patch 旧棋盘（乱套根因）。
     */
    /** 笔画专用轻量入口：合并账本 + 增量追加到画布（不全量 replay）。 */
    private suspend fun ingestDrawStrokeFast(move: GameMoveDto, force: Boolean = false) {
        syncMutex.withLock {
            val kind = move.payload?.takeIf { it.isJsonObject }?.asJsonObject?.get("kind")?.asString
            if (!force && suppressCanvasUntilClearAck && kind == "draw_stroke") {
                if (clearWaterlineMoveIndex > 0 && move.moveIndex < clearWaterlineMoveIndex) {
                    traceCanvas("ingest skip: stale pre-clear move#=${move.moveIndex}")
                    return
                }
            }
            val merged = GomokuBoardSync.mergeMoves(moveLedger.toList(), listOf(move))
            val mergedMax = GomokuBoardSync.maxMoveIndex(merged)
            if (mergedMax < watermarkMoveIndex) {
                traceCanvas("ingest skip: watermark move#=${move.moveIndex} max=$mergedMax wm=$watermarkMoveIndex")
                return
            }
            moveLedger.clear()
            moveLedger.addAll(merged)
            invalidateLedgerStrokeCache()
            if (mergedMax > watermarkMoveIndex) watermarkMoveIndex = mergedMax
            if (kind == "draw_stroke" && !isAwaitingClearAck()) {
                forceEmptyCanvas = false
            }
            lastIngestAtMs = System.currentTimeMillis()
            val round = _ui.value.drawGuess?.round ?: 1
            releaseForceEmptyCanvasIfCleared(merged, round)
            if (!isDrawerSide() && kind == "draw_stroke" && passiveClearDisplayWaterline > 0) {
                passiveClearDisplayWaterline = 0
                traceCanvas("passive clear waterline released move#=${move.moveIndex}")
            }
            if (!force && isAwaitingClearAck() && kind != "draw_clear") {
                traceCanvas("ingest defer canvas move#=${move.moveIndex} kind=$kind awaiting clear")
                return
            }
            if (shouldDeferDrawerPreviewCanvasPublish(move, kind, force)) {
                traceCanvas("ingest defer canvas preview move#=${move.moveIndex} stroke active")
                return
            }
            if (isDrawerMidStroke() && kind == "draw_stroke") {
                traceCanvas("ingest ledger-only mid-stroke move#=${move.moveIndex}")
                publishUi(_ui.value.copy(moves = merged))
                return
            }
            if (!isDrawerSide() && kind == "draw_stroke" && DrawGuessLiveSync.isWsActive()) {
                val strokeId = DrawGuessSync.parseStrokeMove(move, round)?.strokeId.orEmpty()
                if (strokeId.isNotBlank() && DrawGuessLiveSync.isStrokeFinalized(strokeId)) {
                    traceCanvas("ingest ledger-only ws-finalized move#=${move.moveIndex} stroke=$strokeId")
                    publishUi(_ui.value.copy(moves = merged))
                    return
                }
            }
            val strokes = roleAwareStrokesForPublish(round)
            val clearToken = DrawGuessSync.clearToken(merged, round)
            if (kind == "draw_stroke") {
                if (DrawGuessCanvasPublishPolicy.shouldSkipRepublish(
                        strokes,
                        _ui.value.drawStrokes,
                        clearToken,
                        _ui.value.drawClearToken,
                    )
                ) {
                    traceCanvas(
                        "ingest skip canvas republish move#=${move.moveIndex} ui=${strokes.size} not richer",
                    )
                    publishUi(_ui.value.copy(moves = merged))
                    return
                }
            }
            traceCanvas(
                "ingest ok move#=${move.moveIndex} kind=$kind ui=${strokes.size}",
            )
            publishUi(
                _ui.value.copy(
                    drawStrokes = strokes,
                    moves = merged,
                    drawClearToken = clearToken,
                ),
            )
            pruneLocalFinishedConfirmed(round)
        }
    }

    private suspend fun ingestMoves(
        room: GameRoomDto? = null,
        incomingMoves: List<GameMoveDto> = emptyList(),
    ) {
        syncMutex.withLock {
            if (room != null) {
                lastRoomDto = room
                hydrateProfilesFromRoom(room)
            }
            val roomDto = lastRoomDto
            if (roomDto == null && incomingMoves.isEmpty()) return

            val merged = GomokuBoardSync.mergeMoves(moveLedger.toList(), incomingMoves)
            val mergedMax = GomokuBoardSync.maxMoveIndex(merged)

            if (incomingMoves.isNotEmpty() && mergedMax < watermarkMoveIndex) {
                if (roomDto != null) publishRoomMetaOnly(roomDto, moveLedger.toList())
                return
            }

            moveLedger.clear()
            moveLedger.addAll(merged)
            invalidateLedgerStrokeCache()
            if (mergedMax > watermarkMoveIndex) watermarkMoveIndex = mergedMax

            clearConfirmedPending(merged, _ui.value.myPbId)
            clearConfirmedDrawStrokes(merged, _ui.value.myPbId, _ui.value.drawGuess?.round ?: 1)
            pruneLocalFinishedConfirmed(_ui.value.drawGuess?.round ?: 1)
            lastIngestAtMs = System.currentTimeMillis()
            val ingestRound = _ui.value.drawGuess?.round ?: 1
            releaseForceEmptyCanvasIfCleared(merged, ingestRound)

            if (roomDto != null) {
                publishFromLedger(roomDto, merged)
            } else {
                publishFromLedgerOnlyMoves(merged)
            }
        }
    }

    private var lastSyncedDrawerId = ""
    private var lastSyncedRound = 0

    private fun shouldDeferDrawerPreviewCanvasPublish(
        move: GameMoveDto,
        kind: String?,
        force: Boolean = false,
    ): Boolean {
        if (force) return false
        if (!isDrawerSide() || kind != "draw_stroke") return false
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        val sid = obj.get("stroke_id")?.asString?.takeIf { it.isNotBlank() } ?: return false
        return pendingDrawStrokes.any { it.strokeId == sid }
    }

    private fun isDrawerSide(): Boolean {
        val play = _ui.value.drawGuess ?: return false
        val myPbId = _ui.value.myPbId ?: return false
        return play.drawerPbId == myPbId
    }

    private fun syncDrawWsRoomContext(republishCanvas: Boolean = false) {
        val play = _ui.value.drawGuess ?: return
        val drawerChanged = play.drawerPbId != lastSyncedDrawerId
        val roundChanged = play.round != lastSyncedRound
        DrawGuessLiveSync.configureRoomContext(play.round, play.drawerPbId)
        lastSyncedDrawerId = play.drawerPbId
        lastSyncedRound = play.round
        if (republishCanvas || drawerChanged || roundChanged) {
            publishDrawCanvasFromLive()
        }
    }

    private fun publishDrawCanvasFromLive() {
        val play = _ui.value.drawGuess ?: return
        val round = play.round
        publishUi(
            _ui.value.copy(
                drawStrokes = roleAwareStrokesForPublish(round),
            ),
        )
    }

    private fun currentDrawRound(): Int =
        _ui.value.drawGuess?.round
            ?: lastRoomDto?.gameState?.drawGuess?.round
            ?: 1

    private fun hasClearAckForPendingClear(round: Int): Boolean {
        if (clearWaterlineMoveIndex <= 0) return true
        return moveLedger.any { move ->
            if (move.moveIndex < clearWaterlineMoveIndex) return@any false
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
            obj.get("kind")?.asString == "draw_clear" &&
                (obj.get("round")?.asInt ?: round) == round
        }
    }

    private fun isAwaitingClearAck(): Boolean =
        clearWaterlineMoveIndex > 0 &&
            (suppressCanvasUntilClearAck || forceEmptyCanvas) &&
            !hasClearAckForPendingClear(currentDrawRound())

    private fun releaseForceEmptyCanvasIfCleared(moves: List<GameMoveDto>, round: Int) {
        val cleared = moves.any { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
            obj.get("kind")?.asString == "draw_clear" &&
                (obj.get("round")?.asInt ?: round) == round &&
                (clearWaterlineMoveIndex <= 0 || move.moveIndex >= clearWaterlineMoveIndex)
        }
        if (cleared && (
                forceEmptyCanvas ||
                    suppressCanvasUntilClearAck ||
                    clearWaterlineMoveIndex > 0 ||
                    passiveClearDisplayWaterline > 0
                )
        ) {
            forceEmptyCanvas = false
            suppressCanvasUntilClearAck = false
            clearWaterlineMoveIndex = 0
            passiveClearDisplayWaterline = 0
            traceCanvas("clear ack received round=$round")
        }
    }

    private fun invalidateLedgerStrokeCache() {
        ledgerStrokeCacheVersion++
    }

    /** PB 提交成功后立即写入本地账本，避免连抬手时重复 nextMoveIndex 撞号 */
    private fun applySubmittedMove(move: GameMoveDto) {
        val merged = GomokuBoardSync.mergeMoves(moveLedger.toList(), listOf(move))
        moveLedger.clear()
        moveLedger.addAll(merged)
        invalidateLedgerStrokeCache()
        val mergedMax = GomokuBoardSync.maxMoveIndex(merged)
        if (mergedMax > watermarkMoveIndex) watermarkMoveIndex = mergedMax
    }

    private fun replayedStrokesForRound(round: Int): List<DrawStrokeUi> {
        val passiveWaterline = if (!isDrawerSide()) passiveClearDisplayWaterline else 0
        val awaitingWaterline = if (isAwaitingClearAck()) clearWaterlineMoveIndex else 0
        val sinceMoveIndex = maxOf(passiveWaterline, awaitingWaterline)
        val cacheKey = sinceMoveIndex
        if (ledgerStrokeCacheRound == round &&
            ledgerStrokeCacheAtVersion == ledgerStrokeCacheVersion &&
            ledgerStrokeCacheSinceMove == cacheKey
        ) {
            return ledgerStrokeCache
        }
        val moves = if (sinceMoveIndex > 0) {
            moveLedger.filter { it.moveIndex > sinceMoveIndex }
        } else {
            moveLedger.toList()
        }
        val parsed = DrawGuessSync.parseStrokes(moves, round)
        ledgerStrokeCache = parsed
        ledgerStrokeCacheRound = round
        ledgerStrokeCacheAtVersion = ledgerStrokeCacheVersion
        ledgerStrokeCacheSinceMove = cacheKey
        return parsed
    }

    private fun isDrawerActivelyDrawing(): Boolean {
        val play = _ui.value.drawGuess ?: return false
        val myPbId = _ui.value.myPbId ?: return false
        return play.drawerPbId == myPbId && play.phase == DrawGuessPhase.DRAWING.wire
    }

    /** 画家正在拖笔（pending 非空）：SSE 回放不得回退画布 */
    private fun isDrawerMidStroke(): Boolean =
        isDrawerSide() && pendingDrawStrokes.isNotEmpty()

    private fun drawStrokesForRoomPublish(round: Int, moves: List<GameMoveDto>): List<DrawStrokeUi> {
        if (isAwaitingClearAck()) return _ui.value.drawStrokes
        if (isDrawerMidStroke()) return _ui.value.drawStrokes
        val candidate = roleAwareStrokesForPublish(round)
        if (!isDrawerSide() && DrawGuessLiveSync.isWsActive()) {
            val clearToken = DrawGuessSync.clearToken(moves, round)
            if (DrawGuessCanvasPublishPolicy.shouldSkipRepublish(
                    candidate,
                    _ui.value.drawStrokes,
                    clearToken,
                    _ui.value.drawClearToken,
                )
            ) {
                return _ui.value.drawStrokes
            }
        }
        return candidate
    }

    private fun resolveDrawStrokesForUi(replayed: List<DrawStrokeUi>): List<DrawStrokeUi> {
        if (forceEmptyCanvas || isAwaitingClearAck()) {
            return emptyList()
        }
        val round = currentDrawRound()
        val ledgerStrokes = replayed
        val archived = coalesceDrawStrokes(localFinishedStrokes.toList())
        val live = if (DrawWsConfig.isEnabled() && !isDrawerActivelyDrawing()) {
            DrawGuessLiveSync.liveStrokes.value
        } else {
            emptyList()
        }
        val merged = coalesceDrawStrokes(
            ledgerStrokes,
            archived,
            pendingDrawStrokes.toList(),
            live,
        )
        if (merged.isEmpty() && (archived.isNotEmpty() || pendingDrawStrokes.isNotEmpty())) {
            val clearIdx = DrawGuessSync.lastClearMoveIndex(moveLedger.toList(), round)
            traceCanvas(
                "resolve EMPTY drawer=${isDrawerSide()} ledger=${ledgerStrokes.size} " +
                    "archived=${archived.size} pending=${pendingDrawStrokes.size} " +
                    "clearIdx=$clearIdx forceEmpty=$forceEmptyCanvas awaiting=${isAwaitingClearAck()}",
            )
            if (isDrawerSide() && archived.isNotEmpty()) {
                traceCanvas("resolve keep archived fallback n=${archived.size}")
                return archived
            }
        }
        if (!isDrawerActivelyDrawing() && DrawWsConfig.isEnabled() && !DrawGuessLiveSync.isWsActive()) {
            Log.w("DrawGuessCanvas", "guesser ws inactive room=$roomId — fallback PB only")
        }
        return merged
    }

    /** 发布 UI 前统一取画布笔画，清屏等待期间不会被 ledger 全量回放刷回 */
    private fun canvasStrokesForPublish(round: Int): List<DrawStrokeUi> =
        resolveDrawStrokesForUi(replayedStrokesForRound(round))

    /** 按角色取发布笔画：画家 archived 兜底，猜词方保留 finalize 的 live */
    private fun roleAwareStrokesForPublish(round: Int): List<DrawStrokeUi> {
        val raw = if (isDrawerSide()) drawerStrokesForPublish(round) else guesserStrokesForPublish(round)
        val clearToken = DrawGuessSync.clearToken(moveLedger.toList(), round)
        return monotonicDrawStrokes(raw, clearToken)
    }

    /**
     * 同 clearToken 下 ledger 回放滞后时，保留 UI 上已有 strokeId，避免已画笔画被覆盖消失。
     */
    private fun monotonicDrawStrokes(
        computed: List<DrawStrokeUi>,
        clearToken: Int,
    ): List<DrawStrokeUi> {
        if (forceEmptyCanvas && isAwaitingClearAck()) return computed
        if (clearToken != _ui.value.drawClearToken) return computed
        val current = _ui.value.drawStrokes
        if (current.isEmpty()) return computed
        val merged = coalesceDrawStrokes(current, computed)
        if (merged.size < current.size) {
            traceCanvas(
                "monotonic guard count current=${current.size} computed=${computed.size} merged=${merged.size}",
            )
        }
        return merged
    }

    /** 猜词方 stroke_end：合并 ledger + 已 finalize 的 live（水位仅在 ingest 新笔画时解除） */
    private fun guesserStrokesForPublish(round: Int): List<DrawStrokeUi> {
        val ledger = replayedStrokesForRound(round)
        val liveFinalized = if (DrawWsConfig.isEnabled()) {
            DrawGuessLiveSync.liveStrokes.value.filter {
                DrawGuessLiveSync.isStrokeFinalized(it.strokeId)
            }
        } else {
            emptyList()
        }
        return coalesceDrawStrokes(ledger, liveFinalized)
    }

    /** 画家：ledger + archived + pending 直接合并，不经 resolve 空锁 */
    private fun drawerStrokesForPublish(round: Int): List<DrawStrokeUi> {
        if (!isDrawerSide()) return canvasStrokesForPublish(round)
        if (forceEmptyCanvas && isAwaitingClearAck()) return emptyList()
        val ledger = if (forceEmptyCanvas || isAwaitingClearAck()) {
            emptyList()
        } else {
            replayedStrokesForRound(round)
        }
        return coalesceDrawStrokes(
            ledger,
            localFinishedStrokes.toList(),
            pendingDrawStrokes.toList(),
        )
    }

    private fun coalesceDrawStrokes(vararg layers: List<DrawStrokeUi>): List<DrawStrokeUi> =
        DrawGuessSync.coalesceStrokes(layers.flatMap { it })

    private fun publishFromLedger(room: GameRoomDto, moves: List<GameMoveDto>) {
        if (room.gameType == "draw_guess") {
            val play = resolveDrawGuessPlay(room, _ui.value.myPbId)
                ?: _ui.value.drawGuess
                ?: buildDrawGuessShellFromDraft(
                    _ui.value.room ?: draftFromRoomDto(room),
                )
            val round = play.round
            val prevRound = lastDrawGuessRound
            val roundChanged = prevRound != 0 && prevRound != round
            lastDrawGuessRound = round
            if (roundChanged) {
                drawWsReadySentRound = 0
                roomSyncTimedOut = false
                localFinishedStrokes.clear()
                pendingDrawStrokes.clear()
                DrawGuessLiveSync.configureRoomContext(round, play.drawerPbId)
                DrawGuessLiveSync.resetRoomGoForRound(round)
                lastSyncedRound = round
                lastSyncedDrawerId = play.drawerPbId
            } else if (!isAwaitingClearAck()) {
                syncDrawWsRoomContext()
            }
            val bubbles = if (roundChanged) {
                resetGuessBubbleTracking(play)
                DrawGuessBubbleManager.clearAll()
            } else {
                detectAndAddGuessBubbles(play, _ui.value.drawGuessBubbles)
            }
            publishUi(
                _ui.value.copy(
                    gameId = room.gameType,
                    status = room.status,
                    drawGuess = play,
                    currentTurnPbId = resolveDrawGuessTurn(play, room.status),
                    winnerPbId = room.winnerPbId,
                    moves = moves,
                    drawStrokes = drawStrokesForRoomPublish(round, moves),
                    drawClearToken = DrawGuessSync.clearToken(moves, round),
                    drawGuessBubbles = bubbles,
                    busy = false,
                    drawRoundLive = if (roundChanged) {
                        _ui.value.bootstrapComplete &&
                            play.phase == DrawGuessPhase.DRAWING.wire
                    } else {
                        _ui.value.drawRoundLive
                    },
                    drawRoundStartedAtMs = if (roundChanged) 0L else _ui.value.drawRoundStartedAtMs,
                ),
            )
            if (roundChanged) maybeSendDrawWsReady()
            return
        }
        if (room.gameType != "gomoku") {
            publishUi(
                _ui.value.copy(
                    gameId = room.gameType,
                    status = room.status,
                    drawGuess = room.gameState?.drawGuess,
                    currentTurnPbId = room.currentTurnPbId,
                    winnerPbId = room.winnerPbId,
                    moves = moves,
                    busy = false,
                ),
            )
            return
        }

        val gomokuMeta = room.gameState?.gomoku
        val (black, white) = GomokuBoardSync.resolvePlayerIds(gomokuMeta, room)
        val snapshot = GomokuBoardSync.buildSnapshot(
            blackPbId = black,
            whitePbId = white,
            moves = moves,
            pending = pendingSnapshot(),
            myPbId = _ui.value.myPbId,
        )
        val resolvedTurn = resolveCurrentTurn(
            serverTurn = room.currentTurnPbId,
            blackPbId = black,
            whitePbId = white,
            stoneCount = snapshot.stoneCount,
            status = room.status,
        )
        val base = gomokuMeta ?: GomokuPlayState(blackPbId = black, whitePbId = white)
        val gomoku = base.copy(
            blackPbId = black,
            whitePbId = white,
            board = snapshot.board,
            moveCount = snapshot.moveCount,
            lastMove = snapshot.lastMove,
        )
        publishUi(
            _ui.value.copy(
                gameId = room.gameType,
                status = room.status,
                gomoku = gomoku,
                drawGuess = room.gameState?.drawGuess,
                currentTurnPbId = resolvedTurn,
                winnerPbId = room.winnerPbId,
                moves = moves,
                endReason = gomoku.endReason,
                busy = false,
            ),
        )
    }

    /** 仅更新回合/状态，不改动已由账本确定的棋盘 */
    private fun publishRoomMetaOnly(room: GameRoomDto, moves: List<GameMoveDto>) {
        val g = _ui.value.gomoku
        val resolvedTurn = if (g != null && room.gameType == "gomoku") {
            val stoneCount = g.board.count { it != GomokuRules.CELL_EMPTY }
            resolveCurrentTurn(
                serverTurn = room.currentTurnPbId,
                blackPbId = g.blackPbId,
                whitePbId = g.whitePbId,
                stoneCount = stoneCount,
                status = room.status,
            )
        } else if (room.gameType == "draw_guess") {
            resolveDrawGuessTurn(room.gameState?.drawGuess, room.status)
        } else {
            room.currentTurnPbId
        }
        val play = room.gameState?.drawGuess
        val round = play?.round ?: _ui.value.drawGuess?.round ?: 1
        val strokes = if (room.gameType == "draw_guess") {
            drawStrokesForRoomPublish(round, moves)
        } else {
            _ui.value.drawStrokes
        }
        publishUi(
            _ui.value.copy(
                gameId = room.gameType,
                status = room.status,
                currentTurnPbId = resolvedTurn,
                winnerPbId = room.winnerPbId,
                moves = moves,
                drawGuess = resolveDrawGuessPlay(room, _ui.value.myPbId) ?: _ui.value.drawGuess,
                drawStrokes = strokes,
                drawClearToken = if (room.gameType == "draw_guess") {
                    DrawGuessSync.clearToken(moves, round)
                } else {
                    _ui.value.drawClearToken
                },
                endReason = room.gameState?.gomoku?.endReason ?: _ui.value.endReason,
                busy = false,
            ),
        )
    }

    /** Realtime 仅推送 move、尚无 room 时，用缓存 room 元数据回放 */
    private fun publishFromLedgerOnlyMoves(moves: List<GameMoveDto>) {
        val room = lastRoomDto ?: return
        publishFromLedger(room, moves)
    }

    /** 回合只跟 move 账本手数走，与服务器 current_turn 漂移时以账本为准 */
    private fun resolveCurrentTurn(
        serverTurn: String?,
        blackPbId: String,
        whitePbId: String,
        stoneCount: Int,
        status: GameRoomStatus,
    ): String? = GomokuBoardSync.deriveTurn(
        blackPbId = blackPbId,
        whitePbId = whitePbId,
        stoneCount = stoneCount,
        status = status,
        serverTurn = serverTurn,
    )

    private fun applyCredentials(cred: com.example.funlife.social.SocialCredentials) {
        val authId = GamePlayCredentialGate.authIdFrom(cred)
        val cur = _ui.value
        if (cur.myPbId == authId && cur.pbAuthToken == cred.token && cur.identityReady) {
            syncDrawWsRoomContext()
            return
        }
        publishUi(
            cur.copy(
                myPbId = authId,
                pbAuthToken = cred.token,
                identityReady = true,
                identityError = null,
            ),
        )
        startDrawWsIfNeeded()
        syncDrawWsRoomContext()
    }

    private fun clearConfirmedPending(moves: List<GameMoveDto>, myPbId: String?) {
        if (myPbId.isNullOrBlank()) return
        pendingPlacements.removeAll { (x, y) ->
            moves.any { move ->
                if (move.playerPbId != myPbId) return@any false
                val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
                obj.get("kind")?.asString == "gomoku_place" &&
                    obj.get("x")?.asInt == x &&
                    obj.get("y")?.asInt == y
            }
        }
        val pending = _ui.value.pendingPlacement ?: return
        if (GomokuBoardSync.placementExists(moves, myPbId, pending.x, pending.y)) {
            _ui.value = _ui.value.copy(
                pendingPlacement = pending.copy(state = GomokuPlacementSyncState.Sent),
            )
            _ui.value = _ui.value.copy(pendingPlacement = null)
        }
    }

    fun placeGomoku(x: Int, y: Int) {
        viewModelScope.launch {
            if (!_ui.value.identityReady) {
                interactor.ensurePlayCredentials()
                    .onSuccess { cred -> applyCredentials(cred) }
                    .onFailure {
                        notify(interactor.mapError(it))
                        return@launch
                    }
            }
            val state = _ui.value
            val g = state.gomoku ?: return@launch
            val myPbId = state.myPbId ?: return@launch
            if (state.status != GameRoomStatus.PLAYING ||
                state.currentTurnPbId != myPbId ||
                isPlacingMove
            ) {
                return@launch
            }
            val color = GomokuRules.colorForPbId(g.blackPbId, g.whitePbId, myPbId) ?: return@launch
            if (!GomokuRules.validateMove(g.board, x, y, color)) return@launch

            // 禁手检测（仅黑棋且启用禁手规则时）
            if (g.forbiddenEnabled && color == GomokuRules.CELL_BLACK) {
                val validation = GomokuForbiddenRules.validateMoveWithForbidden(
                    board = g.board,
                    x = x,
                    y = y,
                    color = color,
                    enableForbidden = true,
                )
                if (validation is MoveValidation.Forbidden) {
                    notify("禁手：${validation.type.displayName}")
                    return@launch
                }
            }

            val newBoard = GomokuRules.applyMove(g.board, x, y, color)
            val nextTurnPbId = resolveNextTurnPbId(g, color, state.room)
            isPlacingMove = true
            pendingPlacements.add(x to y)
            publishUi(
                state.copy(
                    gomoku = g.copy(
                        board = newBoard,
                        moveCount = g.moveCount + 1,
                        lastMove = GomokuMove(x, y, color.toString()),
                    ),
                    currentTurnPbId = nextTurnPbId,
                    pendingPlacement = null,
                ),
            )
            var lastError: Throwable? = null
            var placeResult: Result<Pair<GameRoomDto, List<GameMoveDto>>>? = null
            for (attempt in 0 until 3) {
                val result = interactor.placeGomoku(
                    roomId = roomId,
                    x = x,
                    y = y,
                    roomHint = lastRoomDto,
                    cachedMoves = moveLedger.toList(),
                )
                placeResult = result
                if (result.isSuccess) {
                    lastError = null
                    break
                }
                lastError = result.exceptionOrNull()
                if (attempt < 2 && lastError != null &&
                    GamePlayCredentialGate.isRecoverableError(lastError!!)
                ) {
                    GamePlayCredentialGate.invalidateAndRebind(getApplication(), currentUserId)
                    delay(400L * (attempt + 1))
                } else {
                    break
                }
            }
            isPlacingMove = false
            if (lastError == null) {
                val payload = placeResult?.getOrNull()
                pendingPlacements.remove(x to y)
                if (payload != null) {
                    ingestMoves(room = payload.first, incomingMoves = payload.second)
                }
                _ui.value = _ui.value.copy(pendingPlacement = null)
            } else {
                var recovered = false
                val refresh = interactor.refreshPlayState(roomId)
                refresh.onSuccess { (dto, moves) ->
                    ingestMoves(room = dto, incomingMoves = moves)
                    recovered = GomokuBoardSync.placementExists(moves, myPbId, x, y)
                }
                pendingPlacements.remove(x to y)
                val errMsg = interactor.mapError(lastError)
                val isTurnMismatch = errMsg.contains("还没轮到你")
                if (!recovered) {
                    if (!refresh.isSuccess) {
                        ingestMoves(incomingMoves = emptyList())
                    }
                    _ui.value = _ui.value.copy(
                        pendingPlacement = if (isTurnMismatch) {
                            null
                        } else {
                            GomokuPendingPlacement(
                                x = x,
                                y = y,
                                state = GomokuPlacementSyncState.Failed,
                                errorMessage = errMsg,
                            )
                        },
                        toast = errMsg,
                    )
                } else {
                    _ui.value = _ui.value.copy(pendingPlacement = null)
                }
            }
        }
    }

    fun retryFailedPlacement() {
        val pending = _ui.value.pendingPlacement ?: return
        if (pending.state != GomokuPlacementSyncState.Failed) return
        val myPbId = _ui.value.myPbId
        if (myPbId == null || _ui.value.currentTurnPbId != myPbId) {
            refreshNow(showBusy = false)
            return
        }
        _ui.value = _ui.value.copy(pendingPlacement = null, toast = null)
        placeGomoku(pending.x, pending.y)
    }

    fun dismissFailedPlacement() {
        val pending = _ui.value.pendingPlacement ?: return
        if (pending.state != GomokuPlacementSyncState.Failed) return
        pendingPlacements.remove(pending.x to pending.y)
        viewModelScope.launch {
            ingestMoves(incomingMoves = emptyList())
            _ui.value = _ui.value.copy(pendingPlacement = null, toast = null)
        }
    }

    private fun resolveNextTurnPbId(
        gomoku: GomokuPlayState,
        afterColor: Char,
        room: LocalGameRoomDraft?,
    ): String? {
        val nextColor = GomokuRules.opponentColor(afterColor)
        return GomokuRules.pbIdForColor(gomoku.blackPbId, gomoku.whitePbId, nextColor)
            ?.takeIf { it.isNotBlank() }
            ?: when (nextColor) {
                GomokuRules.CELL_BLACK -> room?.hostPbId
                GomokuRules.CELL_WHITE -> room?.guestPbId
                else -> null
            }?.takeIf { it.isNotBlank() }
    }

    /**
     * 实时笔画：WS 热路径 + PB SSE 双写。
     * 对方优先收 WS；PB 保证 SSE 必达，避免「画完才显示」。
     */
    fun submitDrawStrokeLive(
        strokeId: String,
        points: List<List<Float>>,
        color: String,
        width: Float,
        flushNow: Boolean = false,
    ) {
        val play = _ui.value.drawGuess
        if (play == null) {
            traceCanvas("chunk skip: drawGuess=null")
            return
        }
        val myPbId = _ui.value.myPbId
        if (myPbId.isNullOrBlank()) {
            traceCanvas("chunk skip: myPbId=null")
            return
        }
        if (play.drawerPbId != myPbId || play.phase != DrawGuessPhase.DRAWING.wire) {
            traceCanvas(
                "chunk skip: not drawer phase drawer=${play.drawerPbId} me=$myPbId phase=${play.phase}",
            )
            return
        }
        if (!_ui.value.bootstrapComplete) {
            traceCanvas("chunk skip: bootstrapComplete=false")
            return
        }
        if (points.isEmpty()) return
        syncDrawWsRoomContext()

        val wsSent = DrawGuessLiveSync.canSendChunks() && strokeId.isNotBlank()
        if (wsSent) {
            DrawGuessStrokeDispatchQueue.enqueue(
                DrawGuessStrokeDispatchQueue.ChunkJob(
                    roomId = roomId,
                    strokeId = strokeId,
                    round = play.round,
                    color = color,
                    width = width,
                    points = points,
                    flushNow = flushNow,
                ),
            )
        }

        val groupSeq = strokeId.hashCode() and 0x7FFFFFFF
        val normalized = points.map { (it.getOrNull(0) ?: 0f) to (it.getOrNull(1) ?: 0f) }
        val pendingIdx = pendingDrawStrokes.indexOfFirst { it.strokeId == strokeId }
        if (pendingIdx >= 0) {
            val old = pendingDrawStrokes[pendingIdx]
            pendingDrawStrokes[pendingIdx] = old.copy(
                points = DrawGuessSync.mergeStrokePoints(old.points, normalized),
                color = color,
                width = width,
            )
        } else {
            pendingDrawStrokes.add(
                DrawStrokeUi(
                    seq = groupSeq,
                    points = normalized,
                    color = color,
                    width = width,
                    strokeId = strokeId,
                ),
            )
        }
        // 画家本地 ink 由 DrawGuessCanvasBoard 渲染；chunk 不发 publishUi，避免 ~250Hz 全树 recompose
        if (flushNow) {
            traceCanvas(
                "chunk flush stroke=$strokeId pts=${pendingDrawStrokes.firstOrNull { it.strokeId == strokeId }?.points?.size ?: 0} " +
                    "pending=${pendingDrawStrokes.size} ws=$wsSent wsLive=${DrawGuessLiveSync.isWsActive()}",
            )
        }

        // 作画全程 PB 双写，猜词方 SSE 必达
        if (shouldHedgePbPreview()) {
            maybePublishPbPreview(strokeId, color, width, flushNow)
        }
    }

    /**
     * WS 活跃时 chunk 仅走 WS；PB 仅在 stroke_end 归档（企业级：避免双通道争用与 SSE 洪泛）。
     * WS 不可用时作画阶段仍走 PB 预览，保证猜词方必达。
     */
    private fun shouldHedgePbPreview(): Boolean {
        val play = _ui.value.drawGuess ?: return false
        if (play.phase != DrawGuessPhase.DRAWING.wire) return false
        return !DrawGuessLiveSync.isWsActive()
    }

    /** WS 不可用时，合并 pending 点阵后低频 POST，避免猜词方开局全盲 */
    private fun maybePublishPbPreview(
        strokeId: String,
        color: String,
        width: Float,
        force: Boolean,
    ) {
        val pending = pendingDrawStrokes.firstOrNull { it.strokeId == strokeId } ?: return
        val minPoints = if (DrawWsConfig.liveWireEnabled()) 1 else 2
        if (pending.points.size < minPoints && !force) return
        val now = System.currentTimeMillis()
        val last = pbPreviewAtMs[strokeId] ?: 0L
        val interval = if (DrawGuessLiveSync.isWsActive()) PB_HEDGE_INTERVAL_MS else PB_PREVIEW_INTERVAL_MS
        if (!force && now - last < interval) return
        pbPreviewAtMs[strokeId] = now
        val wire = pending.points.map { listOf(it.first, it.second) }
        viewModelScope.launch {
            val result = strokeSubmitMutex.withLock {
                interactor.submitDrawStroke(
                    roomId = roomId,
                    seq = pending.seq,
                    points = wire,
                    color = color,
                    width = width,
                    strokeId = strokeId,
                    roomHint = lastRoomDto,
                    cachedMoves = moveLedger.toList(),
                )
            }
            result.onSuccess { move ->
                traceCanvas("pb preview ok stroke=$strokeId move#=${move.moveIndex}")
                viewModelScope.launch { ingestDrawStrokeFast(move) }
            }
                .onFailure { err ->
                    traceCanvas("pb preview FAIL stroke=$strokeId: ${err.message}")
                    Log.w(CANVAS_LOG, "pb preview failed stroke=$strokeId", err)
                }
        }
    }

    /** 抬手：WS 广播 stroke_end（全量点）+ PB 归档 */
    fun finishDrawStrokeWs(strokeId: String, color: String = "#222222", width: Float = 4f) {
        if (strokeId.isBlank()) return
        val play = _ui.value.drawGuess ?: run {
            traceCanvas("finish skip: drawGuess=null stroke=$strokeId")
            return
        }
        val pending = pendingDrawStrokes.firstOrNull { it.strokeId == strokeId }
        val pendingPoints = pending?.points.orEmpty()
        if (DrawGuessLiveSync.canSendChunks()) {
            DrawGuessStrokeDispatchQueue.flushPending()
        }
        val archived = if (DrawGuessLiveSync.canSendChunks()) {
            DrawGuessLiveSync.finishStroke(
                roomId,
                strokeId,
                play.round,
                color,
                width,
                pointsOverride = pendingPoints.takeIf { it.isNotEmpty() },
            )
        } else {
            null
        }
        val allPoints = when {
            pendingPoints.size > (archived?.second?.size ?: 0) && pendingPoints.isNotEmpty() -> pendingPoints
            archived != null && archived.second.isNotEmpty() -> archived.second
            pendingPoints.isNotEmpty() -> pendingPoints
            else -> {
                traceCanvas(
                    "finish skip: no points stroke=$strokeId pending=${pending?.points?.size ?: 0} " +
                        "wsAcc=${archived != null}",
                )
                return
            }
        }
        val wirePoints = if (allPoints.size == 1) {
            val p = allPoints.first()
            listOf(p, p)
        } else {
            allPoints
        }
        if (wirePoints.size < 2) {
            traceCanvas("finish skip: pts<2 stroke=$strokeId n=${wirePoints.size}")
            return
        }
        val seq = archived?.first ?: liveStrokeSeq.incrementAndGet()
        val wire = wirePoints.map { listOf(it.first, it.second) }
        val round = play.round
        upsertLocalFinished(
            DrawStrokeUi(
                seq = seq,
                points = wirePoints,
                color = color,
                width = width,
                strokeId = strokeId,
            ),
        )
        val finishedStrokes = roleAwareStrokesForPublish(round)
        publishUi(_ui.value.copy(drawStrokes = finishedStrokes))
        traceCanvas(
            "finish local stroke=$strokeId pts=${wirePoints.size} ui=${finishedStrokes.size} " +
                "archived=${localFinishedStrokes.size} awaiting=${isAwaitingClearAck()} wsEnd=${archived != null}",
        )
        viewModelScope.launch {
            val result = strokeSubmitMutex.withLock {
                val submitResult = interactor.submitDrawStroke(
                    roomId = roomId,
                    seq = seq,
                    points = wire,
                    color = color,
                    width = width,
                    strokeId = strokeId,
                    roomHint = lastRoomDto,
                    cachedMoves = moveLedger.toList(),
                )
                if (submitResult.isSuccess) {
                    applySubmittedMove(submitResult.getOrThrow())
                }
                submitResult
            }
            if (result.isSuccess) {
                val move = result.getOrThrow()
                traceCanvas("pb finish ok stroke=$strokeId move#=${move.moveIndex}")
                removePendingDrawStroke(strokeId)
                ingestDrawStrokeFast(move, force = true)
                val published = canvasStrokesForPublish(round)
                if (published.any { it.strokeId == strokeId }) {
                    pruneLocalFinishedConfirmed(round)
                    publishDrawCanvasFromLedger()
                } else {
                    traceCanvas(
                        "finish keep archived stroke=$strokeId published=${published.size} " +
                            "archived=${localFinishedStrokes.size}",
                    )
                    publishUi(_ui.value.copy(drawStrokes = roleAwareStrokesForPublish(round)))
                }
            } else {
                val err = result.exceptionOrNull() ?: return@launch
                traceCanvas("pb finish FAIL stroke=$strokeId: ${err.message}")
                Log.w(CANVAS_LOG, "pb stroke_end failed stroke=$strokeId", err)
                val errMsg = interactor.mapError(err)
                publishUi(
                    _ui.value.copy(
                        pendingFailedStroke = DrawPendingStroke(
                            seq = seq,
                            points = allPoints,
                            color = color,
                            width = width,
                            strokeId = strokeId,
                            state = GomokuPlacementSyncState.Failed,
                            errorMessage = errMsg,
                        ),
                        toast = errMsg,
                    ),
                )
            }
        }
    }

    fun submitDrawStroke(seq: Int, points: List<List<Float>>, color: String, width: Float) {
        viewModelScope.launch {
            val state = _ui.value
            val play = state.drawGuess ?: return@launch
            val myPbId = state.myPbId ?: return@launch
            if (play.drawerPbId != myPbId || play.phase != DrawGuessPhase.DRAWING.wire) return@launch
            val normalized = points.map { (it.getOrNull(0) ?: 0f) to (it.getOrNull(1) ?: 0f) }
            val pending = DrawStrokeUi(seq = seq, points = normalized, color = color, width = width)
            pendingDrawStrokes.add(pending)
            publishDrawCanvasFromLedger()
            _ui.value = _ui.value.copy(pendingFailedStroke = null)
            var lastError: Throwable? = null
            var strokeResult: Result<com.example.funlife.social.game.model.GameMoveDto>? = null
            strokeSubmitMutex.withLock {
                repeat(3) { attempt ->
                    strokeResult = interactor.submitDrawStroke(
                        roomId = roomId,
                        seq = seq,
                        points = points,
                        color = color,
                        width = width,
                        roomHint = lastRoomDto,
                        cachedMoves = moveLedger.toList(),
                    )
                    if (strokeResult!!.isSuccess) {
                        lastError = null
                        return@repeat
                    }
                    lastError = strokeResult!!.exceptionOrNull()
                    if (attempt < 2 && lastError != null &&
                        GamePlayCredentialGate.isRecoverableError(lastError!!)
                    ) {
                        GamePlayCredentialGate.invalidateAndRebind(getApplication(), currentUserId)
                        delay(400L * (attempt + 1))
                    } else {
                        return@repeat
                    }
                }
            }
            if (lastError == null) {
                strokeResult?.getOrNull()?.let { move -> ingestDrawStrokeFast(move) }
                _ui.value = _ui.value.copy(pendingFailedStroke = null)
            } else {
                var recovered = false
                interactor.refreshPlayState(roomId).onSuccess { (_, moves) ->
                    val round = play.round
                    recovered = DrawGuessSync.strokeExists(moves, myPbId, seq, round)
                    if (recovered) ingestMoves(incomingMoves = moves)
                }
                if (recovered) {
                    pendingDrawStrokes.remove(pending)
                    publishDrawCanvasFromLedger()
                    _ui.value = _ui.value.copy(pendingFailedStroke = null)
                } else {
                    pendingDrawStrokes.remove(pending)
                    publishDrawCanvasFromLedger()
                    val errMsg = interactor.mapError(lastError!!)
                    _ui.value = _ui.value.copy(
                        pendingFailedStroke = DrawPendingStroke(
                            seq = seq,
                            points = normalized,
                            color = color,
                            width = width,
                            state = GomokuPlacementSyncState.Failed,
                            errorMessage = errMsg,
                        ),
                        toast = errMsg,
                    )
                }
            }
        }
    }

    fun retryFailedStroke() {
        val failed = _ui.value.pendingFailedStroke ?: return
        if (failed.state != GomokuPlacementSyncState.Failed) return
        val play = _ui.value.drawGuess ?: return
        val myPbId = _ui.value.myPbId ?: return
        if (play.drawerPbId != myPbId || play.phase != DrawGuessPhase.DRAWING.wire) {
            refreshNow(showBusy = false)
            return
        }
        _ui.value = _ui.value.copy(pendingFailedStroke = null, toast = null)
        val wire = failed.points.map { listOf(it.first, it.second) }
        if (!failed.strokeId.isNullOrBlank()) {
            pendingDrawStrokes.removeAll { it.strokeId == failed.strokeId }
            pendingDrawStrokes.add(
                DrawStrokeUi(
                    seq = failed.seq,
                    points = failed.points,
                    color = failed.color,
                    width = failed.width,
                    strokeId = failed.strokeId,
                ),
            )
            publishDrawCanvasFromLedger()
            finishDrawStrokeWs(failed.strokeId, failed.color, failed.width)
        } else {
            submitDrawStroke(
                seq = failed.seq,
                points = wire,
                color = failed.color,
                width = failed.width,
            )
        }
    }

    fun dismissFailedStroke() {
        val failed = _ui.value.pendingFailedStroke ?: return
        if (failed.state != GomokuPlacementSyncState.Failed) return
        _ui.value = _ui.value.copy(pendingFailedStroke = null, toast = null)
    }

    fun clearDrawCanvas() {
        viewModelScope.launch {
            clearWaterlineMoveIndex = GomokuBoardSync.maxMoveIndex(moveLedger.toList()) + 1
            forceEmptyCanvas = true
            suppressCanvasUntilClearAck = true
            pendingDrawStrokes.clear()
            localFinishedStrokes.clear()
            pbPreviewAtMs.clear()
            DrawGuessStrokeDispatchQueue.reset()
            invalidateLedgerStrokeCache()
            val round = _ui.value.drawGuess?.round ?: 1
            traceCanvas("clear start waterline=$clearWaterlineMoveIndex round=$round")
            isClearingCanvas = true
            publishUi(
                _ui.value.copy(
                    drawStrokes = emptyList(),
                    drawClearToken = _ui.value.drawClearToken + 1,
                ),
            )
            if (DrawGuessLiveSync.canSendChunks()) {
                DrawGuessLiveSync.sendClear(roomId, round)
            }
            interactor.clearDrawCanvas(
                roomId = roomId,
                roomHint = lastRoomDto,
                cachedMoves = moveLedger.toList(),
            ).fold(
                onSuccess = { move ->
                    ingestMoves(incomingMoves = listOf(move))
                    val clearedStrokes = canvasStrokesForPublish(round)
                    if (clearedStrokes.isEmpty()) {
                        publishUi(_ui.value.copy(drawStrokes = emptyList()))
                    }
                },
                onFailure = {
                    suppressCanvasUntilClearAck = false
                    forceEmptyCanvas = false
                    clearWaterlineMoveIndex = 0
                    notify(interactor.mapError(it))
                },
            )
            isClearingCanvas = false
        }
    }

    private fun detectAndAddGuessBubbles(
        play: DrawGuessPlayState,
        existing: List<DrawGuessBubbleMessage>,
    ): List<DrawGuessBubbleMessage> {
        val guesses = play.guesses
        if (guesses.size <= processedGuessCount) return existing
        var bubbles = existing
        guesses.drop(processedGuessCount).forEach { guess ->
            bubbles = DrawGuessBubbleManager.addBubble(
                existing = bubbles,
                playerPbId = guess.pbId,
                text = guess.text,
                isCorrect = guess.correct,
            )
        }
        processedGuessCount = guesses.size
        return bubbles
    }

    private fun resetGuessBubbleTracking(play: DrawGuessPlayState?) {
        processedGuessCount = play?.guesses?.size ?: 0
    }

    fun dismissDrawGuessBubble(bubbleId: String) {
        publishUi(
            _ui.value.copy(
                drawGuessBubbles = DrawGuessBubbleManager.removeBubble(
                    _ui.value.drawGuessBubbles,
                    bubbleId,
                ),
            ),
        )
    }

    fun submitGuess(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val myPbId = _ui.value.myPbId ?: return
        val optimistic = DrawGuessBubbleManager.addBubble(
            existing = _ui.value.drawGuessBubbles,
            playerPbId = myPbId,
            text = trimmed,
            isCorrect = false,
        )
        publishUi(_ui.value.copy(drawGuessBubbles = optimistic, busy = true))
        viewModelScope.launch {
            interactor.submitGuess(roomId, trimmed)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    fun finishDrawing() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            interactor.setDrawPhase(roomId, DrawGuessPhase.ROUND_END)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    fun continueAfterRound() {
        viewModelScope.launch {
            pendingDrawStrokes.clear()
            localFinishedStrokes.clear()
            processedGuessCount = 0
            pbPreviewAtMs.clear()
            drawWsReadySentRound = 0
            roomSyncTimedOut = false
            _ui.value = _ui.value.copy(
                busy = true,
                drawGuessBubbles = DrawGuessBubbleManager.clearAll(),
                drawRoundLive = false,
                drawRoundStartedAtMs = 0L,
            )
            interactor.setDrawPhase(roomId, DrawGuessPhase.DRAWING)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
            maybeSendDrawWsReady()
        }
    }

    fun forceRoundEnd() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            interactor.setDrawPhase(roomId, DrawGuessPhase.ROUND_END)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    private fun resolveDrawGuessPlay(room: GameRoomDto, myPbId: String?): DrawGuessPlayState? {
        val raw = room.gameState?.drawGuess
        if (raw != null) return sanitizeDrawGuessForViewer(raw, myPbId)
        if (room.gameType != "draw_guess" || room.status != GameRoomStatus.PLAYING) return null
        val host = room.hostPbId
        val drawer = room.currentTurnPbId?.takeIf { it.isNotBlank() } ?: host
        return sanitizeDrawGuessForViewer(
            DrawGuessPlayState(
                drawerPbId = drawer,
                scores = drawGuessScoresShell(room),
                phaseStartedAtMs = System.currentTimeMillis(),
            ),
            myPbId,
        )
    }

    private fun drawGuessScoresShell(room: GameRoomDto): Map<String, Int> =
        drawGuessScoresFromIds(room.hostPbId, room.guestPbId.orEmpty())

    private fun sanitizeDrawGuessForViewer(
        play: DrawGuessPlayState?,
        myPbId: String?,
    ): DrawGuessPlayState? {
        if (play == null || myPbId.isNullOrBlank()) return play
        val visible = play.visibleWord(myPbId)
        return if (visible == play.word) play else play.copy(word = visible)
    }

    fun consumeToast() {
        _ui.value = _ui.value.copy(toast = null)
    }

    fun requestDrawGuessHint() {
        val play = _ui.value.drawGuess ?: return
        val myPbId = _ui.value.myPbId ?: return
        if (play.drawerPbId != myPbId) {
            notify("只有画家可以使用提示")
            return
        }
        val word = play.word
        if (word.isBlank()) {
            notify("暂无词语")
            return
        }
        notify("共 ${word.length} 字 · 首字：${word.first()}")
    }

    private fun notify(msg: String) {
        _ui.value = _ui.value.copy(toast = msg, busy = false)
    }

    fun parseDrawStrokes(moves: List<GameMoveDto>): List<DrawStrokeUi> {
        val round = _ui.value.drawGuess?.round ?: 1
        return DrawGuessSync.parseStrokes(moves, round)
    }

    private fun publishDrawCanvasFromLedger() {
        val room = lastRoomDto ?: return
        val play = _ui.value.drawGuess ?: room.gameState?.drawGuess ?: return
        val round = play.round
        publishUi(
            _ui.value.copy(
                drawStrokes = roleAwareStrokesForPublish(round),
                drawClearToken = DrawGuessSync.clearToken(moveLedger.toList(), round),
            ),
        )
    }

    private fun mergeDrawStrokes(
        replayed: List<DrawStrokeUi>,
        pending: List<DrawStrokeUi>,
    ): List<DrawStrokeUi> = coalesceDrawStrokes(replayed, pending)

    private fun removePendingDrawStroke(strokeId: String) {
        if (strokeId.isBlank()) return
        pendingDrawStrokes.removeAll { it.strokeId == strokeId }
    }

    private fun upsertLocalFinished(stroke: DrawStrokeUi) {
        val sid = stroke.strokeId?.takeIf { it.isNotBlank() } ?: return
        val merged = DrawGuessSync.coalesceStrokes(
            localFinishedStrokes.filter { it.strokeId == sid } + stroke,
        ).firstOrNull { it.strokeId == sid } ?: stroke
        localFinishedStrokes.removeAll { it.strokeId == sid }
        localFinishedStrokes.add(merged)
    }

    private fun pruneLocalFinishedConfirmed(round: Int) {
        if (localFinishedStrokes.isEmpty()) return
        if (isDrawerActivelyDrawing()) return
        val onCanvas = _ui.value.drawStrokes
        localFinishedStrokes.removeAll { local ->
            val sid = local.strokeId?.takeIf { it.isNotBlank() } ?: return@removeAll false
            val confirmed = onCanvas.firstOrNull { it.strokeId == sid } ?: return@removeAll false
            confirmed.points.size >= local.points.size && confirmed.points.size >= 2
        }
    }

    private fun clearConfirmedDrawStrokes(moves: List<GameMoveDto>, myPbId: String?, round: Int) {
        if (myPbId.isNullOrBlank()) return
        // 作画中 pending 只在抬手归档后摘除，避免 PB 预览 PATCH 中途摘掉导致笔画闪没
        if (isDrawerActivelyDrawing()) return
        val replayed = DrawGuessSync.parseStrokes(moves, round)
        pendingDrawStrokes.removeAll { pending ->
            val sid = pending.strokeId
            val confirmed = when {
                !sid.isNullOrBlank() -> replayed.firstOrNull { it.strokeId == sid }
                else -> null
            }
            val existsBySeq = if (confirmed == null && sid.isNullOrBlank()) {
                DrawGuessSync.strokeExists(moves, myPbId, pending.seq, round)
            } else {
                false
            }
            when {
                confirmed != null ->
                    confirmed.points.size >= pending.points.size && confirmed.points.size >= 2
                existsBySeq -> true
                else -> false
            }
        }
    }

    private fun resolveDrawGuessTurn(play: DrawGuessPlayState?, status: GameRoomStatus): String? {
        if (status != GameRoomStatus.PLAYING || play == null) return null
        return when (DrawGuessPhase.fromWire(play.phase)) {
            DrawGuessPhase.DRAWING -> play.drawerPbId.takeIf { it.isNotBlank() }
            DrawGuessPhase.GUESSING -> {
                val host = _ui.value.room?.hostPbId
                val guest = _ui.value.room?.guestPbId
                listOfNotNull(host, guest).firstOrNull { it.isNotBlank() && it != play.drawerPbId }
            }
            else -> play.drawerPbId.takeIf { it.isNotBlank() }
        }
    }

    private fun startDrawTimerLoop() {
        drawTimerJob?.cancel()
        drawTimerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val play = _ui.value.drawGuess ?: continue
                if (_ui.value.status != GameRoomStatus.PLAYING) continue
                if (_ui.value.busy) continue
                val started = play.phaseStartedAtMs
                if (started <= 0L) continue
                val elapsedSec = ((System.currentTimeMillis() - started) / 1000L).toInt()
                when (play.phase) {
                    DrawGuessPhase.DRAWING.wire -> {
                        if (elapsedSec >= play.drawSeconds) {
                            forceRoundEnd()
                        }
                    }
                    DrawGuessPhase.GUESSING.wire -> {
                        if (elapsedSec >= play.guessSeconds) {
                            forceRoundEnd()
                        }
                    }
                }
            }
        }
    }

    private fun pendingSnapshot(): List<Pair<Int, Int>> = pendingPlacements.toList()

    /** 对局进行中退出：先回趣玩中心，后台认输并结束房间。 */
    fun exitPlayToCenter(onDone: () -> Unit) {
        syncCollectJob?.cancel()
        reconcileJob?.cancel()
        GamePlaySyncManager.stopSession(roomId)
        onDone()
        GameRoomSyncCoordinator.runAfterNavigate(
            tag = "abandonPlay room=$roomId",
            block = { interactor.abandonPlay(roomId) },
            onResult = { result ->
                result.onFailure {
                    val msg = interactor.mapError(it)
                    android.widget.Toast.makeText(getApplication(), msg, android.widget.Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    override fun onCleared() {
        syncCollectJob?.cancel()
        drawWsConnectJob?.cancel()
        drawWsCollectJob?.cancel()
        bootstrapPollJob?.cancel()
        reconcileJob?.cancel()
        drawTimerJob?.cancel()
        DrawGuessLiveSync.stop()
        GamePlaySyncManager.stopSession(roomId)
        super.onCleared()
    }
}

data class DrawStrokeUi(
    val seq: Int,
    val points: List<Pair<Float, Float>>,
    val color: String,
    val width: Float,
    /** 同一手指拖动的分片共享 strokeId，合并为一条连续路径 */
    val strokeId: String? = null,
)
