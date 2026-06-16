package com.example.funlife.ui.screens.pacmaze.online

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.data.PacMazePrefs
import com.example.funlife.data.model.UserSession
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.viewmodel.GameCenterViewModel

/** 本地豆人大厅内：在线开房 / 加入子页。 */
@Composable
fun PacMazeOnlineHubPanel(
    subMode: String,
    userId: Long,
    gameCenterVm: GameCenterViewModel,
    onEnterLobby: (String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PacMazePrefs(context) }
    val busyMessage by gameCenterVm.busyMessage.collectAsState()
    val joinCode by gameCenterVm.joinCode.collectAsState()
    val navigateRoomId by gameCenterVm.navigateToRoomId.collectAsState()
    val activeRoom = gameCenterVm.activeHostRoom("pac_maze")
    var coopLevel by remember(subMode) { mutableIntStateOf(1) }
    val isCoop = subMode == "coop_campaign"

    LaunchedEffect(navigateRoomId) {
        navigateRoomId?.let { roomId ->
            onEnterLobby(roomId)
            gameCenterVm.consumeNavigateToRoom()
        }
    }

    LaunchedEffect(Unit) {
        gameCenterVm.toast.collect { msg ->
            msg?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                gameCenterVm.consumeToast()
            }
        }
    }

    PacMazeOnlineHubScreen(
        subMode = subMode,
        versusRating = prefs.versusRating(userId),
        versusGames = prefs.versusGames(userId),
        coopAssists = prefs.coopAssists(userId),
        coopLevel = coopLevel,
        onCoopLevelChange = { coopLevel = it },
        joinCode = joinCode,
        onJoinCodeChange = gameCenterVm::onJoinCodeChange,
        activeRoomCode = activeRoom?.roomCode?.takeIf { it.isNotBlank() },
        onCreateRoom = {
            gameCenterVm.createOpenRoom(
                gameId = "pac_maze",
                pacSubMode = subMode,
                pacLevelId = if (isCoop) coopLevel else null,
            )
        },
        onJoinRoom = gameCenterVm::joinByCode,
        onResumeRoom = activeRoom?.let { { onEnterLobby(it.roomId) } },
        isCreating = busyMessage != null,
        busyLabel = busyMessage,
    )
}

/** 本地大厅内的对战房间（邀请 / 准备 / 开始）。 */
@Composable
fun PacMazeOnlineInHubLobby(
    roomId: String,
    @Suppress("UNUSED_PARAMETER") userSession: UserSession,
    gameCenterVm: GameCenterViewModel,
    myDisplayName: String,
    onLobbyClosed: () -> Unit,
) {
    val rooms by gameCenterVm.rooms.collectAsState()
    val acceptedFriends by gameCenterVm.acceptedFriends.collectAsState()
    val optimisticPending by gameCenterVm.optimisticPendingInvite.collectAsState()
    val myPbId by gameCenterVm.myPbId.collectAsState()
    val pbAuthToken by gameCenterVm.pbAuthToken.collectAsState()
    val busyMessage by gameCenterVm.busyMessage.collectAsState()
    val startingGameRoomId by gameCenterVm.startingGameRoomId.collectAsState()
    val room = rooms.firstOrNull { it.roomId == roomId }
    val context = LocalContext.current

    LaunchedEffect(roomId) {
        gameCenterVm.enterLobby(roomId)
        gameCenterVm.refreshFriendsForLobby(force = true)
    }
    DisposableEffect(roomId) {
        onDispose {
            gameCenterVm.stopLobbySync(roomId)
            gameCenterVm.clearStartingGame()
        }
    }
    LaunchedEffect(Unit) {
        gameCenterVm.toast.collect { msg ->
            msg?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                gameCenterVm.consumeToast()
            }
        }
    }

    if (room != null) {
        val isHost = myPbId != null && room.hostPbId == myPbId
        val optimisticFriendPbId = optimisticPending[roomId]
        val effectiveInvitePending = room.isInvitePending || optimisticFriendPbId != null
        val canInviteFriend = isHost && room.status == GameRoomStatus.WAITING &&
            !effectiveInvitePending && !room.isRoomFull
        val canStart = isHost && room.canStartGame
        val isStartingGame = startingGameRoomId == roomId
        val pendingFriendPbId = room.pendingInvitePbId
            ?: room.guestPbId.takeIf { room.isInvitePending }
            ?: optimisticFriendPbId
        val pendingFriendName = acceptedFriends.firstOrNull { it.friendPbId == pendingFriendPbId }
            ?.displayName?.ifBlank {
                acceptedFriends.firstOrNull { it.friendPbId == pendingFriendPbId }?.funlifeUsername
            }
            ?: room.guestDisplayName ?: room.peerDisplayName ?: "好友"

        PacMazeOnlineLobbyScreen(
            modifier = Modifier.fillMaxSize(),
            room = room,
            pac = room.pacMaze,
            isHost = isHost,
            myPbId = myPbId,
            myDisplayName = myDisplayName,
            pbAuthToken = pbAuthToken,
            acceptedFriends = acceptedFriends,
            canInviteFriend = canInviteFriend,
            effectiveInvitePending = effectiveInvitePending,
            pendingFriendName = pendingFriendName,
            pendingFriendPbId = pendingFriendPbId,
            invitingFriendPbId = optimisticFriendPbId,
            canStart = canStart,
            isStartingGame = isStartingGame,
            busy = busyMessage != null,
            onCopyRoomCode = { copyPacRoomShare(context, room.roomCode) },
            onShareRoomCode = { copyPacRoomShare(context, room.roomCode) },
            onToggleReady = { ready -> gameCenterVm.togglePacMazeReady(roomId, ready) },
            onInviteFriend = { friend -> gameCenterVm.inviteFriendInRoom(roomId, friend.friendPbId) },
            onWithdrawInvite = { gameCenterVm.withdrawInvite(roomId) },
            onStartGame = { gameCenterVm.startGame(roomId) },
            onDissolveRoom = { gameCenterVm.dissolveRoom(roomId, onLobbyClosed) },
        )
    } else {
        PacMazeOnlineLobbyScreen(
            modifier = Modifier.fillMaxSize(),
            room = null,
            pac = null,
            isHost = false,
            myPbId = myPbId,
            myDisplayName = myDisplayName,
            pbAuthToken = pbAuthToken,
            acceptedFriends = emptyList(),
            canInviteFriend = false,
            effectiveInvitePending = false,
            pendingFriendName = "",
            pendingFriendPbId = null,
            invitingFriendPbId = null,
            canStart = false,
            isStartingGame = false,
            busy = true,
            onCopyRoomCode = {},
            onShareRoomCode = {},
            onToggleReady = {},
            onInviteFriend = {},
            onWithdrawInvite = {},
            onStartGame = {},
            onDissolveRoom = onLobbyClosed,
        )
    }
}

private fun copyPacRoomShare(context: Context, roomCode: String) {
    val text = "来一局豆人迷宫！房间号 $roomCode — 打开趣生活 → 豆人迷宫 → 输入房间号"
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("room_code", text))
    android.widget.Toast.makeText(context, "房间号已复制", android.widget.Toast.LENGTH_SHORT).show()
}
