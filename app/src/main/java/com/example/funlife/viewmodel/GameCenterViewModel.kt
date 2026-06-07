package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.social.FriendsInteractor
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.PocketBaseConnectionWarmer
import com.example.funlife.social.game.GamePlaySyncManager
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.SocialOperationGate
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.utils.AvatarImageLoader
import com.example.funlife.social.game.GameInviteNotifier
import com.example.funlife.social.game.GameRoomInteractor
import com.example.funlife.social.game.GameRoomSyncCoordinator
import com.example.funlife.social.game.GameCenterPrefs
import com.example.funlife.social.game.catalog.GameCatalogStatus
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.catalog.SocialGameEntry
import com.example.funlife.social.game.model.GameCenterTab
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.InviteMode
import com.example.funlife.social.game.model.LobbyExitAction
import kotlinx.coroutines.async
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.model.MyGameItemUi
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.social.model.SocialLinkState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameCenterViewModel(
    application: Application,
    val currentUserId: Long,
    myFunlifeUsername: String,
    displayName: String,
) : AndroidViewModel(application) {

    private val prefs = GameCenterPrefs(application.applicationContext)
    private val friendsInteractor = FriendsInteractor(
        application.applicationContext,
        currentUserId,
        myFunlifeUsername,
        displayName,
    )
    private val gameRoomInteractor = GameRoomInteractor(application.applicationContext, currentUserId)

    private val _selectedTab = MutableStateFlow(GameCenterTab.ONLINE)
    val selectedTab: StateFlow<GameCenterTab> = _selectedTab.asStateFlow()

    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _navigateToRoomId = MutableStateFlow<String?>(null)
    val navigateToRoomId: StateFlow<String?> = _navigateToRoomId.asStateFlow()

    /** 乐观接受失败后退回上一页 */
    private val _requestPopLobby = MutableStateFlow(false)
    val requestPopLobby: StateFlow<Boolean> = _requestPopLobby.asStateFlow()

    private val _showTutorial = MutableStateFlow(false)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val _busyMessage = MutableStateFlow<String?>(null)
    val busyMessage: StateFlow<String?> = _busyMessage.asStateFlow()
    val isBusy: StateFlow<Boolean> = _busyMessage.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** 房主点击「开始游戏」后保持全屏加载，直到跳转对局页 */
    private val _startingGameRoomId = MutableStateFlow<String?>(null)
    val startingGameRoomId: StateFlow<String?> = _startingGameRoomId.asStateFlow()

    val linkState: StateFlow<SocialLinkState> = SocialSessionManager.linkState

    val rooms: StateFlow<List<LocalGameRoomDraft>> = gameRoomInteractor
        .observeRooms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _myPbId = MutableStateFlow<String?>(null)
    val myPbId: StateFlow<String?> = _myPbId.asStateFlow()

    private val _pbAuthToken = MutableStateFlow<String?>(null)
    val pbAuthToken: StateFlow<String?> = _pbAuthToken.asStateFlow()

    private val _myLocalAvatarUri = MutableStateFlow<String?>(null)
    val myLocalAvatarUri: StateFlow<String?> = _myLocalAvatarUri.asStateFlow()

    private val notifiedDeclineIds = mutableSetOf<String>()
    private var lastFriendsSyncMs = 0L

    private val _optimisticPendingInvite = MutableStateFlow<Map<String, String>>(emptyMap())
    val optimisticPendingInvite: StateFlow<Map<String, String>> = _optimisticPendingInvite.asStateFlow()

    /** 用户已点接受、服务端尚未确认前：隐藏重复邀请弹层 */
    private val _optimisticAcceptedRoomIds = MutableStateFlow<Set<String>>(emptySet())
    val optimisticAcceptedRoomIds: StateFlow<Set<String>> = _optimisticAcceptedRoomIds.asStateFlow()

    val handledInviteRoomIds: StateFlow<Set<String>> = GameInviteNotifier.handledSnapshot
        .map { it[currentUserId].orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val incomingDirectInvite: StateFlow<LocalGameRoomDraft?> = combine(
        rooms,
        _myPbId,
        SocialSessionManager.snapshot,
        GameInviteNotifier.handledSnapshot,
        _optimisticAcceptedRoomIds,
    ) { list, vmPbId, snap, handledMap, optimisticAccepted ->
        val myId = vmPbId?.takeIf { it.isNotBlank() }
            ?: snap.pbRecordId?.takeIf { it.isNotBlank() }
            ?: return@combine null
        val handled = handledMap[currentUserId].orEmpty() + optimisticAccepted
        list.firstOrNull { room ->
            room.roomId !in handled &&
                room.isIncomingInviteFor(myId) &&
                room.joinedMembers.none { it.pbId == myId }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val acceptedFriends: StateFlow<List<FriendUiModel>> = friendsInteractor
        .observeFriends()
        .map { list -> list.filter { it.status == FriendshipStatus.ACCEPTED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recentIds = MutableStateFlow(prefs.recentGameIds(currentUserId))

    val sortedOnlineGames: StateFlow<List<SocialGameEntry>> = _recentIds
        .map { recent ->
            val recentSet = recent.toSet()
            SocialGameCatalog.onlineGames().sortedWith(
                compareBy<SocialGameEntry> { it.gameId !in recentSet }
                    .thenBy { it.status.ordinal }
                    .thenBy { it.sortOrder },
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SocialGameCatalog.onlineGames())

    val myGames: StateFlow<List<MyGameItemUi>> = rooms
        .map { roomList ->
            roomList.map { room ->
                val entry = SocialGameCatalog.find(room.gameId)
                MyGameItemUi(
                    roomId = room.roomId,
                    gameId = room.gameId,
                    gameTitle = room.gameTitle,
                    gameEmoji = entry?.iconEmoji ?: "🎮",
                    status = room.status,
                    subtitle = roomSubtitle(room),
                    accentColors = entry?.accentColors ?: listOf(0xFF7C4DFF, 0xFF536DFE),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingInviteCount: StateFlow<Int> = combine(
        rooms,
        _myPbId,
        SocialSessionManager.snapshot,
        GameInviteNotifier.handledSnapshot,
        _optimisticAcceptedRoomIds,
    ) { list, vmPbId, snap, handledMap, optimisticAccepted ->
        val myId = vmPbId?.takeIf { it.isNotBlank() }
            ?: snap.pbRecordId?.takeIf { it.isNotBlank() }
            ?: return@combine 0
        val handled = handledMap[currentUserId].orEmpty() + optimisticAccepted
        list.count { room ->
            room.roomId !in handled &&
                room.isIncomingInviteFor(myId) &&
                room.joinedMembers.none { it.pbId == myId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        if (!prefs.isTutorialSeen(currentUserId)) {
            _showTutorial.value = true
        }
        bootstrapSocialAndRooms()
        viewModelScope.launch {
            val link = getApplication<FunLifeApplication>().database.socialDao().getLink(currentUserId)
            link?.pbRecordId?.takeIf { it.isNotBlank() }?.let { _myPbId.value = it }
            SocialOperationGate.warmCredentials(getApplication(), currentUserId)
            refreshCredentialSnapshot()
        }
        viewModelScope.launch {
            var prevPhase: SocialSessionManager.SessionPhase? = null
            var prevRealtime: SocialSessionManager.RealtimePhase? = null
            SocialSessionManager.snapshot.collect { snap ->
                refreshCredentialSnapshot()
                if (!SocialSessionManager.isLinked(getApplication(), currentUserId)) return@collect
                val becameReady = prevPhase != SocialSessionManager.SessionPhase.READY &&
                    snap.phase == SocialSessionManager.SessionPhase.READY
                val becameLive = prevRealtime != SocialSessionManager.RealtimePhase.LIVE &&
                    snap.realtime == SocialSessionManager.RealtimePhase.LIVE
                prevPhase = snap.phase
                prevRealtime = snap.realtime
                if (becameReady || becameLive) {
                    refreshRoomsQuietly()
                }
            }
        }
        viewModelScope.launch {
            rooms.collect { list ->
                val myId = _myPbId.value?.takeIf { it.isNotBlank() }
                    ?: SocialSessionManager.snapshot.value.pbRecordId?.takeIf { it.isNotBlank() }
                    ?: return@collect
                val staleAccepted = _optimisticAcceptedRoomIds.value.filter { roomId ->
                    val room = list.firstOrNull { it.roomId == roomId } ?: return@filter true
                    room.joinedMembers.any { it.pbId == myId } ||
                        !room.isIncomingInviteFor(myId)
                }
                if (staleAccepted.isNotEmpty()) {
                    _optimisticAcceptedRoomIds.value = _optimisticAcceptedRoomIds.value - staleAccepted.toSet()
                }
                if (list.any { room ->
                        room.isIncomingInviteFor(myId) &&
                            !GameInviteNotifier.isHandled(currentUserId, room.roomId) &&
                            room.joinedMembers.none { m -> m.pbId == myId }
                    }) {
                    runCatching {
                        GameInviteNotifier.publishNewInvites(getApplication(), currentUserId)
                    }
                }
                list.forEach { room ->
                    val myId = _myPbId.value?.takeIf { it.isNotBlank() }
                        ?: SocialSessionManager.snapshot.value.pbRecordId?.takeIf { it.isNotBlank() }
                    if (myId != null && room.isIncomingInviteFor(myId)) {
                        GameInviteNotifier.reconcileHandled(currentUserId, room, myId)
                    }
                    if (room.isInvitePending) {
                        _optimisticPendingInvite.value = _optimisticPendingInvite.value - room.roomId
                    }
                    if (
                        room.hostPbId == myId &&
                        room.declinedByGuest &&
                        room.status == GameRoomStatus.WAITING &&
                        room.isSoloLobby &&
                        room.roomId !in notifiedDeclineIds
                    ) {
                        notifiedDeclineIds.add(room.roomId)
                        _toast.value = "${room.guestDisplayName ?: room.peerDisplayName ?: "好友"} 婉拒了邀请"
                    }
                }
            }
        }
        viewModelScope.launch {
            val app = getApplication<FunLifeApplication>()
            app.database.userAvatarDao().getUserAvatar(currentUserId).collect { avatar ->
                _myLocalAvatarUri.value = avatar?.avatarUri
            }
        }
        viewModelScope.launch {
            combine(acceptedFriends, _pbAuthToken) { friends, token -> friends to token }
                .collect { (friends, token) ->
                    AvatarImageLoader.warmAll(
                        getApplication(),
                        friends.map { it.avatarUrl },
                        token,
                    )
                }
        }
        viewModelScope.launch {
            combine(rooms, _pbAuthToken) { list, token -> list to token }
                .collect { (list, token) ->
                    AvatarImageLoader.warmAll(
                        getApplication(),
                        list.flatMap { room ->
                            listOfNotNull(
                                room.hostAvatarUrl,
                                room.guestAvatarUrl,
                                room.peerAvatarUrl,
                            )
                        },
                        token,
                    )
                }
        }
    }

    fun bootstrapSocialAndRooms() {
        viewModelScope.launch {
            refreshCredentialSnapshot()
            coroutineScope {
                launch { runCatching { friendsInteractor.syncFriends(forceSession = false) } }
                if (socialReady()) {
                    launch {
                        GameRoomSyncCoordinator.requestFullRefresh {
                            gameRoomInteractor.refreshRooms()
                        }
                    }
                }
            }
        }
    }

    fun refreshFriendsForLobby(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFriendsSyncMs < 8_000L) return
        lastFriendsSyncMs = now
        viewModelScope.launch {
            friendsInteractor.syncFriendsPresenceForLobby()
        }
    }

    /** 进入大厅：单房间同步 + 好友在线，不拉全量房间列表 */
    fun enterLobby(roomId: String) {
        PocketBaseConnectionWarmer.warmAsync(getApplication())
        refreshFriendsForLobby(force = true)
        // 乐观接受尚未完成时不同步，避免 GET 404 弹错；acceptInvite 成功后会 startLobbySync
        if (roomId in _optimisticAcceptedRoomIds.value) return
        startLobbySync(roomId, urgent = true)
    }

    fun refreshRoomsQuietly() {
        GameRoomSyncCoordinator.requestFullRefresh {
            gameRoomInteractor.refreshRooms()
        }
    }

    /** 首页邀请弹层：轻量拉取待处理邀请（应用级 ForegroundSync 负责持续轮询）。 */
    fun refreshRoomsForDeliveryQuietly() {
        GameRoomSyncCoordinator.requestFullRefresh {
            val result = gameRoomInteractor.refreshIncomingInvitesOnly()
            if (result.isSuccess) {
                runCatching {
                    GameInviteNotifier.publishNewInvites(getApplication(), currentUserId)
                }
            }
            result
        }
    }

    fun refreshRoomByIdQuietly(roomId: String) {
        GameRoomSyncCoordinator.requestRoomRefresh(roomId) {
            gameRoomInteractor.refreshRoomById(roomId)
        }
    }

    fun startLobbySync(roomId: String, urgent: Boolean = true) {
        PocketBaseConnectionWarmer.warmAsync(getApplication())
        GameRoomSyncCoordinator.startLobbyWatch(roomId, urgent = urgent) {
            gameRoomInteractor.refreshRoomById(roomId)
        }
    }

    /** 大厅满员可开局时预连对局 Realtime（与 Play 页共用 session） */
    fun prewarmPlaySync(roomId: String) {
        if (!pocketBaseConfigured() || !socialReady()) return
        GamePlaySyncManager.prewarmSession(getApplication(), currentUserId, roomId)
    }

    fun stopLobbySync(roomId: String) {
        GameRoomSyncCoordinator.stopLobbyWatch(roomId)
    }

    private suspend fun refreshCredentialSnapshot() {
        val cred = friendsInteractor.peekCredentials()
        val tokenId = cred?.token?.let { token ->
            runCatching { PocketBaseApiClient.recordIdFromToken(token) }.getOrNull()
        }
        val linkPbId = getApplication<FunLifeApplication>().database.socialDao()
            .getLink(currentUserId)?.pbRecordId
        _pbAuthToken.value = cred?.token
        _myPbId.value = tokenId?.takeIf { it.isNotBlank() }
            ?: cred?.pbRecordId?.takeIf { it.isNotBlank() }
            ?: linkPbId?.takeIf { it.isNotBlank() }
            ?: SocialSessionManager.snapshot.value.pbRecordId?.takeIf { it.isNotBlank() }
    }

    fun activeHostRoom(gameId: String): LocalGameRoomDraft? {
        val myId = _myPbId.value ?: return null
        return rooms.value.firstOrNull { room ->
            room.hostPbId == myId &&
                room.gameId == gameId &&
                room.status in GameRoomStatus.ACTIVE
        }
    }

    fun pocketBaseConfigured(): Boolean = PocketBaseConfig.isEnabled()

    fun socialReady(): Boolean = linkState.value is SocialLinkState.Linked

    fun selectTab(tab: GameCenterTab) {
        _selectedTab.value = tab
    }

    fun dismissTutorial() {
        prefs.setTutorialSeen(currentUserId)
        _showTutorial.value = false
    }

    fun onJoinCodeChange(value: String) {
        _joinCode.value = value.uppercase().filter { it in ROOM_CODE_CHARS }.take(6)
    }

    fun consumeToast() {
        _toast.value = null
    }

    /** 居中 Toast + 可选系统 Toast（离开页面后仍可见）。 */
    private fun notifyUser(message: String, systemToast: Boolean = false) {
        _toast.value = message
        if (systemToast) {
            android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun consumeNavigateToRoom() {
        _navigateToRoomId.value = null
    }

    fun consumePopLobby() {
        _requestPopLobby.value = false
    }

    fun refreshRooms() {
        viewModelScope.launch {
            gameRoomInteractor.refreshRooms()
                .onFailure { _toast.value = gameRoomInteractor.mapErrorMessage(it) }
        }
    }

    fun touchGame(gameId: String) {
        prefs.touchRecentGame(currentUserId, gameId)
        _recentIds.value = prefs.recentGameIds(currentUserId)
    }

    fun createOpenRoom(gameId: String, thenInviteGuestPbId: String? = null) {
        val entry = SocialGameCatalog.find(gameId) ?: return
        if (_busyMessage.value != null) {
            _toast.value = "请稍候，正在处理上一操作…"
            return
        }
        if (entry.status == GameCatalogStatus.COMING_SOON) {
            _toast.value = "该游戏即将上线"
            return
        }
        if (!ensureOnlineReady(entry)) return

        touchGame(gameId)
        PocketBaseConnectionWarmer.warmAsync(getApplication())
        viewModelScope.launch {
            _busyMessage.value = "正在开房间…"
            gameRoomInteractor.createOpenRoom(gameId)
                .onSuccess { roomId ->
                    _toast.value = "房间已创建"
                    _navigateToRoomId.value = roomId
                    if (!thenInviteGuestPbId.isNullOrBlank()) {
                        inviteFriendInRoom(roomId, thenInviteGuestPbId, showBusy = false)
                    }
                }
                .onFailure { _toast.value = gameRoomInteractor.mapErrorMessage(it) }
            _busyMessage.value = null
        }
    }

    fun inviteFriendInRoom(roomId: String, guestPbId: String, showBusy: Boolean = true) {
        if (_optimisticPendingInvite.value.containsKey(roomId)) return
        if (_busyMessage.value != null && showBusy) {
            _toast.value = "请稍候，正在处理上一操作…"
            return
        }
        if (!socialReady()) {
            _toast.value = "请先在好友页完成社交账号绑定"
            return
        }
        _optimisticPendingInvite.value = _optimisticPendingInvite.value + (roomId to guestPbId)
        PocketBaseConnectionWarmer.warmAsync(getApplication())
        startLobbySync(roomId, urgent = true)
        viewModelScope.launch {
            if (showBusy) _busyMessage.value = "正在发送邀请…"
            gameRoomInteractor.inviteFriendToRoom(roomId, guestPbId)
                .onSuccess {
                    _toast.value = "邀请已发送，等待好友接受"
                    GameRoomSyncCoordinator.requestRoomRefreshImmediate(roomId) {
                        gameRoomInteractor.refreshRoomById(roomId)
                    }
                }
                .onFailure {
                    _optimisticPendingInvite.value = _optimisticPendingInvite.value - roomId
                    _toast.value = gameRoomInteractor.mapErrorMessage(it)
                }
            if (showBusy) _busyMessage.value = null
        }
    }

    fun withdrawInvite(roomId: String) {
        viewModelScope.launch {
            gameRoomInteractor.withdrawInvite(roomId)
                .onSuccess { notifyUser("已撤回邀请") }
                .onFailure { notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true) }
        }
    }

    fun dismissDeclineNotice(roomId: String) {
        viewModelScope.launch {
            gameRoomInteractor.dismissDeclineNotice(roomId)
        }
    }

    fun joinByCode() {
        val code = _joinCode.value.trim()
        if (code.length < 6) {
            _toast.value = "请输入 6 位房间号"
            return
        }
        if (!socialReady()) {
            _toast.value = "请先完成社交账号绑定"
            return
        }
        if (_busyMessage.value != null) {
            _toast.value = "请稍候…"
            return
        }
        viewModelScope.launch {
            _busyMessage.value = "正在加入…"
            gameRoomInteractor.joinByRoomCode(code)
                .onSuccess { _navigateToRoomId.value = it }
                .onFailure { _toast.value = gameRoomInteractor.mapErrorMessage(it) }
            _busyMessage.value = null
        }
    }

    /** 同步门禁：点击接受/婉拒的第一时间调用（应用级单例，跨 MainActivity / NavGraph VM 共享） */
    fun acknowledgeIncomingInvite(roomId: String) {
        _optimisticAcceptedRoomIds.value = _optimisticAcceptedRoomIds.value + roomId
        val updatedAt = rooms.value.firstOrNull { it.roomId == roomId }?.updatedAtMs ?: 0L
        GameInviteNotifier.markHandled(currentUserId, roomId, updatedAt)
    }

    private fun unacknowledgeIncomingInvite(roomId: String) {
        _optimisticAcceptedRoomIds.value = _optimisticAcceptedRoomIds.value - roomId
        GameInviteNotifier.unmarkHandled(currentUserId, roomId)
    }

    fun acceptInvite(
        roomId: String,
        onAccepted: (() -> Unit)? = null,
        optimistic: Boolean = false,
        onSettled: (() -> Unit)? = null,
    ) {
        GameRoomSyncCoordinator.markMutating(roomId)
        acknowledgeIncomingInvite(roomId)
        if (optimistic) {
            onAccepted?.invoke()
        }
        viewModelScope.launch {
            try {
                gameRoomInteractor.acceptInvite(roomId)
                    .onSuccess {
                        startLobbySync(roomId, urgent = true)
                        GameRoomSyncCoordinator.requestRoomRefreshImmediate(roomId) {
                            gameRoomInteractor.refreshRoomById(roomId)
                        }
                        if (!optimistic) {
                            onAccepted?.invoke()
                            if (onAccepted == null) _navigateToRoomId.value = roomId
                        }
                    }
                    .onFailure {
                        unacknowledgeIncomingInvite(roomId)
                        if (optimistic) _requestPopLobby.value = true
                        notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = optimistic)
                    }
            } finally {
                GameRoomSyncCoordinator.clearMutating(roomId)
                onSettled?.invoke()
            }
        }
    }

    fun rejectInvite(roomId: String, onRejected: (() -> Unit)? = null, onSettled: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                gameRoomInteractor.rejectInvite(roomId)
                    .onSuccess { onRejected?.invoke() }
                    .onFailure { notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true) }
            } finally {
                onSettled?.invoke()
            }
        }
    }

    fun exitLobby(
        roomId: String,
        action: LobbyExitAction,
        onDone: () -> Unit,
        onSettled: (() -> Unit)? = null,
    ) {
        if (action == LobbyExitAction.LOCAL_ONLY) {
            stopLobbySync(roomId)
            onDone()
            onSettled?.invoke()
            return
        }
        if (action == LobbyExitAction.REJECT_INVITE) {
            acknowledgeIncomingInvite(roomId)
        }
        stopLobbySync(roomId)
        onDone()
        onSettled?.invoke()
        GameRoomSyncCoordinator.runAfterNavigate(
            tag = "exitLobby action=$action room=$roomId",
            block = {
                when (action) {
                    LobbyExitAction.REJECT_INVITE -> gameRoomInteractor.rejectInvite(roomId)
                    LobbyExitAction.LEAVE_SEAT -> gameRoomInteractor.leaveRoom(roomId)
                    LobbyExitAction.LOCAL_ONLY -> Result.success(Unit)
                }
            },
            onResult = { result ->
                result.onSuccess {
                    if (action == LobbyExitAction.LEAVE_SEAT) {
                        gameRoomInteractor.dismissLocalRoom(roomId)
                        GameInviteNotifier.unmarkHandled(currentUserId, roomId)
                    }
                }.onFailure {
                    if (action == LobbyExitAction.REJECT_INVITE) {
                        unacknowledgeIncomingInvite(roomId)
                    }
                    notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true)
                }
            },
        )
    }

    fun dissolveRoom(roomId: String, onDone: () -> Unit, onSettled: (() -> Unit)? = null) {
        stopLobbySync(roomId)
        onDone()
        onSettled?.invoke()
        GameRoomSyncCoordinator.runAfterNavigate(
            tag = "dissolveRoom room=$roomId",
            block = { gameRoomInteractor.cancelRoom(roomId) },
            onResult = { result ->
                result.onSuccess { notifyUser("房间已解散") }
                    .onFailure { notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true) }
            },
        )
    }

    fun cancelRoom(roomId: String) {
        viewModelScope.launch {
            gameRoomInteractor.cancelRoom(roomId)
                .onSuccess { notifyUser("已取消") }
                .onFailure { notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true) }
        }
    }

    fun startGame(roomId: String, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            _startingGameRoomId.value = roomId
            gameRoomInteractor.startGame(roomId)
                .onSuccess {
                    delay(350L)
                    onStarted()
                    _startingGameRoomId.value = null
                }
                .onFailure {
                    _startingGameRoomId.value = null
                    notifyUser(gameRoomInteractor.mapErrorMessage(it), systemToast = true)
                }
        }
    }

    fun clearStartingGame() {
        _startingGameRoomId.value = null
    }

    private fun roomSubtitle(room: LocalGameRoomDraft): String = when {
        room.declinedByGuest && room.isSoloLobby ->
            "对方已拒绝 · 房间仍开放"
        room.isInvitePending ->
            "等待 ${room.guestDisplayName ?: room.peerDisplayName ?: "好友"} 接受"
        room.status == GameRoomStatus.WAITING ->
            "房间号 ${room.roomCode} · ${room.joinedCount}/${room.maxPlayers} 人"
        else -> when (room.status) {
            GameRoomStatus.ACCEPTED -> "${room.joinedCount} 人就绪，可开始"
            GameRoomStatus.PLAYING -> "对局进行中"
            else -> room.status.name
        }
    }

    private fun ensureOnlineReady(entry: SocialGameEntry): Boolean {
        if (!entry.minPocketBase) return true
        if (!pocketBaseConfigured()) {
            _toast.value = "社交服务未配置，暂无法在线对战"
            return false
        }
        if (!socialReady()) {
            _toast.value = "正在连接社交服务，请稍候…"
            viewModelScope.launch { friendsInteractor.syncFriends(forceSession = true) }
            return false
        }
        return true
    }

    companion object {
        private val ROOM_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toSet()
    }
}
