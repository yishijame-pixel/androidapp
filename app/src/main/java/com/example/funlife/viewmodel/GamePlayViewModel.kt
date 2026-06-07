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
import com.google.gson.JsonParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    /** 首次进盘完成后不再退回全屏加载（防止同步抖动闪屏） */
    val bootstrapComplete: Boolean = false,
    /** 进局加载真实进度 0–100，100 表示可进盘 */
    val bootstrapProgress: Int = 0,
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
    private var reconcileJob: Job? = null
    private val syncMutex = Mutex()
    private val refreshMutex = Mutex()
    private val moveLedger = CopyOnWriteArrayList<GameMoveDto>()
    private val pendingPlacements = CopyOnWriteArrayList<Pair<Int, Int>>()
    private val pendingDrawStrokes = CopyOnWriteArrayList<DrawStrokeUi>()
    private val strokeSubmitMutex = Mutex()
    private val liveStrokeSeq = AtomicInteger(0)
    private var isClearingCanvas = false
    private var drawTimerJob: Job? = null
    private var lastRoomDto: GameRoomDto? = null
    private var isPlacingMove = false
    private var watermarkMoveIndex = 0
    private var lastIngestAtMs = System.currentTimeMillis()
    private var drawWsBootstrapTimedOut = false

    init {
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
        val progress = computeBootstrapProgress()
        when {
            isBootstrapReady(_ui.value) -> completeBootstrap()
            drawWsBootstrapTimedOut && isPlayReady(_ui.value) -> completeBootstrap()
            progress != _ui.value.bootstrapProgress ->
                _ui.value = _ui.value.copy(bootstrapProgress = progress)
        }
    }

    private fun completeBootstrap() {
        publishUi(_ui.value.copy(bootstrapProgress = 100, bootstrapComplete = true))
    }

    /** 进局加载：身份+房间就绪后最多等 WS 2.5s，硬上限 4s 必进盘 */
    private fun startBootstrapWatchdog() {
        viewModelScope.launch {
            val wsGraceMs = if (DrawWsConfig.isEnabled()) 2_500L else 0L
            val hardCapMs = 4_000L
            val startMs = System.currentTimeMillis()
            while (System.currentTimeMillis() - startMs < hardCapMs && !_ui.value.bootstrapComplete) {
                ensureDrawGuessShellIfNeeded()
                tickBootstrapProgress()
                if (_ui.value.bootstrapComplete) return@launch
                val elapsed = System.currentTimeMillis() - startMs
                if (isPlayReady(_ui.value) && elapsed >= wsGraceMs) {
                    if (DrawWsConfig.isEnabled() && !DrawGuessLiveSync.isWsActive()) {
                        drawWsBootstrapTimedOut = true
                        Log.w("DrawGuessCanvas", "ws bootstrap grace elapsed room=$roomId — enter with PB fallback")
                    }
                    forceBootstrapExit()
                    return@launch
                }
                delay(200L)
            }
            if (!_ui.value.bootstrapComplete) {
                drawWsBootstrapTimedOut = true
                Log.w("DrawGuessCanvas", "bootstrap hard cap room=$roomId — force enter")
                forceBootstrapExit()
            }
        }
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
        }
    }

    fun computeBootstrapProgress(state: GamePlayUiState = _ui.value): Int {
        if (!isIdentitySatisfied(state)) return 15
        if (!isDrawGuessRoom(state)) {
            return if (isPlayReady(state)) 100 else 45
        }
        if (state.drawGuess == null) return 35
        if (!DrawWsConfig.isEnabled()) return 100
        if (DrawGuessLiveSync.isWsActive()) return 100
        if (drawWsBootstrapTimedOut) return 100
        val started = wsBootstrapStartedMs
        if (started == 0L) return 72
        val elapsed = System.currentTimeMillis() - started
        val sub = (elapsed * 27 / 2_500L).toInt().coerceIn(0, 27)
        return 72 + sub
    }

    /** 在 tick/watchdog 中调用，标记 WS 等待起点（勿在 Composable 里调用 computeBootstrapProgress） */
    private fun computeBootstrapProgress(): Int {
        if (isDrawGuessRoom(_ui.value) &&
            _ui.value.drawGuess != null &&
            DrawWsConfig.isEnabled() &&
            !DrawGuessLiveSync.isWsActive() &&
            !drawWsBootstrapTimedOut &&
            wsBootstrapStartedMs == 0L
        ) {
            wsBootstrapStartedMs = System.currentTimeMillis()
        }
        return computeBootstrapProgress(_ui.value)
    }

    fun isBootstrapReady(state: GamePlayUiState = _ui.value): Boolean =
        computeBootstrapProgress(state) >= 100 && isPlayReady(state)

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
        DrawWsConfig.isEnabled() && !DrawGuessLiveSync.isWsActive() && !drawWsBootstrapTimedOut ->
            "连接笔画通道…"
        DrawWsConfig.isEnabled() && DrawGuessLiveSync.isWsActive() -> "笔画通道已就绪"
        else -> "即将开始…"
    }

    fun bootstrapSubtitle(state: GamePlayUiState = _ui.value): String = when {
        isDrawGuessRoom(state) && DrawWsConfig.isEnabled() && DrawGuessLiveSync.isWsActive() ->
            "低延迟同步已就绪"
        isDrawGuessRoom(state) && DrawWsConfig.isEnabled() ->
            "正在连接 WebSocket 画板中继"
        else -> "正在同步对局数据"
    }

    /** UI/看门狗统一出口：先本地种子状态进盘，后台继续同步 */
    fun dismissBootstrapLoading() {
        forceBootstrapExit()
    }

    private fun forceBootstrapExit() {
        val room = lastRoomDto
        val cur = _ui.value
        val next = when {
            room?.gameType == "draw_guess" -> cur.copy(
                gameId = "draw_guess",
                status = room.status,
                drawGuess = resolveDrawGuessPlay(room, cur.myPbId)
                    ?: DrawGuessPlayState(
                        drawerPbId = room.currentTurnPbId?.takeIf { it.isNotBlank() } ?: room.hostPbId,
                        scores = drawGuessScoresShell(room),
                        phaseStartedAtMs = System.currentTimeMillis(),
                    ),
                currentTurnPbId = room.currentTurnPbId
                    ?: resolveDrawGuessTurn(room.gameState?.drawGuess, room.status),
                bootstrapComplete = true,
                bootstrapProgress = 100,
                busy = false,
            )
            room != null -> cur.copy(
                gameId = room.gameType.ifBlank { cur.gameId },
                status = room.status,
                bootstrapComplete = true,
                bootstrapProgress = 100,
                busy = false,
            )
            else -> cur.copy(bootstrapComplete = true, bootstrapProgress = 100, busy = false)
        }
        publishUi(next)
        viewModelScope.launch { refreshNow(showBusy = false) }
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
            _ui.value = _ui.value.copy(room = draftFromRoomDto(dto), gameId = dto.gameType)
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
        val marked = when {
            next.bootstrapComplete -> next.copy(bootstrapProgress = 100)
            isBootstrapReady(next) -> next.copy(bootstrapComplete = true, bootstrapProgress = 100)
            else -> next
        }
        _ui.value = marked
    }

    /** transport/live 订阅只建一次，避免 applyCredentials 重连时掐掉 joined 回调 */
    private fun ensureDrawWsCollectors() {
        if (!DrawWsConfig.isEnabled()) return
        if (drawWsCollectJob?.isActive == true) return
        drawWsCollectJob = viewModelScope.launch {
            launch {
                DrawGuessLiveSync.transport.collect { tickBootstrapProgress() }
            }
            launch {
                DrawGuessLiveSync.liveStrokes.collect {
                    if (_ui.value.drawGuess == null) return@collect
                    if (!DrawGuessLiveSync.isWsActive()) return@collect
                    val round = _ui.value.drawGuess?.round ?: 1
                    val replayed = DrawGuessSync.parseStrokes(moveLedger.toList(), round)
                    publishUi(_ui.value.copy(drawStrokes = resolveDrawStrokesForUi(replayed)))
                }
            }
        }
    }

    private fun startDrawWsConnectLoop() {
        if (!DrawWsConfig.isEnabled()) return
        drawWsConnectJob?.cancel()
        drawWsConnectJob = viewModelScope.launch {
            while (true) {
                if (DrawGuessLiveSync.isConnected(roomId)) {
                    delay(5_000)
                    continue
                }
                if (DrawGuessLiveSync.isPending(roomId)) {
                    delay(400)
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
                DrawGuessLiveSync.start(viewModelScope, roomId, authToken)
                delay(2_000)
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
    private suspend fun ingestDrawStrokeFast(move: GameMoveDto) {
        syncMutex.withLock {
            val merged = GomokuBoardSync.mergeMoves(moveLedger.toList(), listOf(move))
            val mergedMax = GomokuBoardSync.maxMoveIndex(merged)
            if (mergedMax < watermarkMoveIndex) return
            moveLedger.clear()
            moveLedger.addAll(merged)
            if (mergedMax > watermarkMoveIndex) watermarkMoveIndex = mergedMax
            clearConfirmedDrawStrokes(merged, _ui.value.myPbId, _ui.value.drawGuess?.round ?: 1)
            pruneLiveStrokesConfirmedInLedger(merged, _ui.value.drawGuess?.round ?: 1)
            lastIngestAtMs = System.currentTimeMillis()
            val round = _ui.value.drawGuess?.round ?: 1
            val replayed = DrawGuessSync.parseStrokes(merged, round)
            publishUi(
                _ui.value.copy(
                    drawStrokes = resolveDrawStrokesForUi(replayed),
                    moves = merged,
                    drawClearToken = DrawGuessSync.clearToken(merged, round),
                ),
            )
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
            if (mergedMax > watermarkMoveIndex) watermarkMoveIndex = mergedMax

            clearConfirmedPending(merged, _ui.value.myPbId)
            clearConfirmedDrawStrokes(merged, _ui.value.myPbId, _ui.value.drawGuess?.round ?: 1)
            lastIngestAtMs = System.currentTimeMillis()

            if (roomDto != null) {
                publishFromLedger(roomDto, merged)
            } else {
                publishFromLedgerOnlyMoves(merged)
            }
        }
    }

    private fun resolveDrawStrokesForUi(replayed: List<DrawStrokeUi>): List<DrawStrokeUi> {
        val play = _ui.value.drawGuess
        val myPbId = _ui.value.myPbId
        val isDrawerDrawing = play != null &&
            !myPbId.isNullOrBlank() &&
            play.drawerPbId == myPbId &&
            play.phase == DrawGuessPhase.DRAWING.wire
        // 绘画者：本地 pending + localPath 已覆盖当前笔，不再叠 WS live（避免 2 点分片竞态）
        val live = if (!isDrawerDrawing && DrawGuessLiveSync.isWsActive()) {
            DrawGuessLiveSync.liveStrokes.value
        } else {
            emptyList()
        }
        // WS live 优先：ledger 同 strokeId 不参与合并，防止 PB 归档后覆盖热路径导致笔画跳动
        val liveIds = live.mapNotNull { it.strokeId?.takeIf { id -> id.isNotBlank() } }.toSet()
        val replayedFiltered = if (liveIds.isEmpty()) {
            replayed
        } else {
            replayed.filter { it.strokeId.isNullOrBlank() || it.strokeId !in liveIds }
        }
        val merged = coalesceDrawStrokes(replayedFiltered, pendingDrawStrokes.toList(), live)
        if (Log.isLoggable("DrawGuessCanvas", Log.DEBUG)) {
            Log.d(
                "DrawGuessCanvas",
                "resolve drawer=$isDrawerDrawing ws=${DrawGuessLiveSync.isWsActive()} " +
                    "strokes=${merged.size} pts=${merged.sumOf { it.points.size }} " +
                    "pending=${pendingDrawStrokes.size} live=${live.size}",
            )
        }
        if (!isDrawerDrawing && DrawWsConfig.isEnabled() && !DrawGuessLiveSync.isWsActive()) {
            Log.w("DrawGuessCanvas", "guesser ws inactive room=$roomId — fallback PB only")
        }
        return merged
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
            val replayed = DrawGuessSync.parseStrokes(moves, round)
            publishUi(
                _ui.value.copy(
                    gameId = room.gameType,
                    status = room.status,
                    drawGuess = play,
                    currentTurnPbId = resolveDrawGuessTurn(play, room.status),
                    winnerPbId = room.winnerPbId,
                    moves = moves,
                    drawStrokes = resolveDrawStrokesForUi(replayed),
                    drawClearToken = DrawGuessSync.clearToken(moves, round),
                    busy = false,
                ),
            )
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
            resolveDrawStrokesForUi(DrawGuessSync.parseStrokes(moves, round))
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
        if (cur.myPbId == authId && cur.pbAuthToken == cred.token && cur.identityReady) return
        publishUi(
            cur.copy(
                myPbId = authId,
                pbAuthToken = cred.token,
                identityReady = true,
                identityError = null,
            ),
        )
        startDrawWsIfNeeded()
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
    fun submitDrawStrokeLive(strokeId: String, points: List<List<Float>>, color: String, width: Float) {
        val play = _ui.value.drawGuess ?: return
        val myPbId = _ui.value.myPbId ?: return
        if (play.drawerPbId != myPbId || play.phase != DrawGuessPhase.DRAWING.wire) return
        if (points.size < 2) return

        if (DrawGuessLiveSync.isWsActive() && strokeId.isNotBlank()) {
            DrawGuessLiveSync.sendChunk(roomId, strokeId, play.round, color, width, points)
        }

        val seq = liveStrokeSeq.incrementAndGet()
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
        val round = play.round
        val replayed = DrawGuessSync.parseStrokes(moveLedger.toList(), round)
        publishUi(
            _ui.value.copy(
                drawStrokes = resolveDrawStrokesForUi(replayed),
            ),
        )

        // WS 活跃时仅走热路径分片；PB 在抬手 stroke_end 一次性归档，避免双写导致猜词方笔画漂移
        if (!DrawGuessLiveSync.isWsActive()) {
            viewModelScope.launch {
                val result = strokeSubmitMutex.withLock {
                    interactor.submitDrawStroke(
                        roomId = roomId,
                        seq = seq,
                        points = points,
                        color = color,
                        width = width,
                        strokeId = strokeId.takeIf { it.isNotBlank() },
                        roomHint = lastRoomDto,
                        cachedMoves = moveLedger.toList(),
                    )
                }
                result.onSuccess { move ->
                    ingestDrawStrokeFast(move)
                }.onFailure {
                    pendingDrawStrokes.removeAll { it.strokeId == strokeId }
                }
            }
        }
    }

    /** 抬手：WS 广播全量 + PB 归档（对方 PB-only 也能看到完整笔画） */
    fun finishDrawStrokeWs(strokeId: String, color: String = "#222222", width: Float = 4f) {
        if (!DrawGuessLiveSync.isWsActive() || strokeId.isBlank()) return
        val play = _ui.value.drawGuess ?: return
        val archived = DrawGuessLiveSync.finishStroke(roomId, strokeId, play.round, color, width)
            ?: return
        val (seq, allPoints) = archived
        if (allPoints.size < 2) return
        val wire = allPoints.map { listOf(it.first, it.second) }
        viewModelScope.launch {
            strokeSubmitMutex.withLock {
                interactor.submitDrawStroke(
                    roomId = roomId,
                    seq = seq,
                    points = wire,
                    color = color,
                    width = width,
                    strokeId = strokeId,
                    roomHint = lastRoomDto,
                    cachedMoves = moveLedger.toList(),
                )
            }.onSuccess { move -> ingestDrawStrokeFast(move) }
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
        submitDrawStroke(
            seq = failed.seq,
            points = failed.points.map { listOf(it.first, it.second) },
            color = failed.color,
            width = failed.width,
        )
    }

    fun dismissFailedStroke() {
        val failed = _ui.value.pendingFailedStroke ?: return
        if (failed.state != GomokuPlacementSyncState.Failed) return
        _ui.value = _ui.value.copy(pendingFailedStroke = null, toast = null)
    }

    fun clearDrawCanvas() {
        viewModelScope.launch {
            pendingDrawStrokes.clear()
            if (DrawGuessLiveSync.isWsActive()) {
                DrawGuessLiveSync.sendClear(roomId, _ui.value.drawGuess?.round ?: 1)
            }
            publishDrawCanvasFromLedger()
            isClearingCanvas = true
            interactor.clearDrawCanvas(
                roomId = roomId,
                roomHint = lastRoomDto,
                cachedMoves = moveLedger.toList(),
            ).fold(
                onSuccess = { move -> ingestMoves(incomingMoves = listOf(move)) },
                onFailure = { notify(interactor.mapError(it)) },
            )
            isClearingCanvas = false
        }
    }

    fun submitGuess(text: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            interactor.submitGuess(roomId, text)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    fun finishDrawing() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true)
            interactor.setDrawPhase(roomId, DrawGuessPhase.GUESSING)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
        }
    }

    fun continueAfterRound() {
        viewModelScope.launch {
            pendingDrawStrokes.clear()
            _ui.value = _ui.value.copy(busy = true)
            interactor.setDrawPhase(roomId, DrawGuessPhase.DRAWING)
                .onSuccess { dto -> ingestMoves(room = dto, incomingMoves = emptyList()) }
                .onFailure { notify(interactor.mapError(it)) }
            _ui.value = _ui.value.copy(busy = false)
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
        val replayed = DrawGuessSync.parseStrokes(moveLedger.toList(), round)
        publishUi(
            _ui.value.copy(
                drawStrokes = resolveDrawStrokesForUi(replayed),
                drawClearToken = DrawGuessSync.clearToken(moveLedger.toList(), round),
            ),
        )
    }

    private fun mergeDrawStrokes(
        replayed: List<DrawStrokeUi>,
        pending: List<DrawStrokeUi>,
    ): List<DrawStrokeUi> = coalesceDrawStrokes(replayed, pending)

    private fun clearConfirmedDrawStrokes(moves: List<GameMoveDto>, myPbId: String?, round: Int) {
        if (myPbId.isNullOrBlank()) return
        val replayed = DrawGuessSync.parseStrokes(moves, round)
        pendingDrawStrokes.removeAll { pending ->
            val sid = pending.strokeId
            if (!sid.isNullOrBlank()) {
                val confirmed = replayed.firstOrNull { it.strokeId == sid }
                confirmed != null && confirmed.points.size >= pending.points.size
            } else {
                DrawGuessSync.strokeExists(moves, myPbId, pending.seq, round)
            }
        }
    }

    /** ledger 已含完整笔画时，从 WS live 摘掉，交给 replay 单一数据源渲染 */
    private fun pruneLiveStrokesConfirmedInLedger(moves: List<GameMoveDto>, round: Int) {
        if (!DrawGuessLiveSync.isWsActive()) return
        val replayed = DrawGuessSync.parseStrokes(moves, round)
        replayed.forEach { stroke ->
            val sid = stroke.strokeId?.takeIf { it.isNotBlank() } ?: return@forEach
            val live = DrawGuessLiveSync.liveStrokes.value.firstOrNull { it.strokeId == sid } ?: return@forEach
            if (stroke.points.size >= live.points.size) {
                DrawGuessLiveSync.dropLiveStroke(sid)
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
                            finishDrawing()
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
