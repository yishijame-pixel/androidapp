package com.example.funlife.ui.screens.socialgame

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.InviteMode
import com.example.funlife.social.game.model.LobbyExitPolicy
import com.example.funlife.viewmodel.GameCenterViewModel

@Composable
fun GameLobbyScreen(
    roomId: String,
    viewModel: GameCenterViewModel,
    myDisplayName: String,
    onNavigateBack: () -> Unit,
    onNavigateToGameCenter: () -> Unit = onNavigateBack,
    onNavigateToRoom: (roomId: String) -> Unit = {},
    onStartGame: (roomId: String) -> Unit,
    onNavigateToLocalPacMaze: () -> Unit = {},
) {
    val rooms by viewModel.rooms.collectAsState()
    val acceptedFriends by viewModel.acceptedFriends.collectAsState()
    val optimisticPending by viewModel.optimisticPendingInvite.collectAsState()
    val handledInvite by viewModel.handledInviteRoomIds.collectAsState()
    val optimisticAccepted by viewModel.optimisticAcceptedRoomIds.collectAsState()
    val myPbId by viewModel.myPbId.collectAsState()
    val pbAuthToken by viewModel.pbAuthToken.collectAsState()
    val myLocalAvatarUri by viewModel.myLocalAvatarUri.collectAsState()
    val busyMessage by viewModel.busyMessage.collectAsState()
    val startingGameRoomId by viewModel.startingGameRoomId.collectAsState()
    val isStartingGame = startingGameRoomId == roomId
    val navigateRoomId by viewModel.navigateToRoomId.collectAsState()
    val requestPopLobby by viewModel.requestPopLobby.collectAsState()
    val room = rooms.firstOrNull { it.roomId == roomId }
    val context = LocalContext.current

    LaunchedEffect(roomId) {
        viewModel.enterLobby(roomId)
    }
    DisposableEffect(roomId) {
        onDispose {
            viewModel.stopLobbySync(roomId)
            viewModel.clearStartingGame()
        }
    }
    LaunchedEffect(navigateRoomId) {
        navigateRoomId?.let { newRoomId ->
            if (newRoomId != roomId) onNavigateToRoom(newRoomId)
            viewModel.consumeNavigateToRoom()
        }
    }
    LaunchedEffect(requestPopLobby) {
        if (requestPopLobby) {
            viewModel.consumePopLobby()
            onNavigateToGameCenter()
        }
    }
    LaunchedEffect(room?.status, room?.gameId, roomId) {
        if (room?.status == GameRoomStatus.PLAYING) {
            viewModel.clearStartingGame()
            onStartGame(roomId)
        }
    }
    LaunchedEffect(room?.gameId, roomId, pbAuthToken) {
        if (room?.gameId == "draw_guess" && !pbAuthToken.isNullOrBlank()) {
            viewModel.prewarmDrawWs(roomId)
        }
    }
    LaunchedEffect(room?.roomId, room?.joinedCount, room?.status, room?.minPlayers) {
        if (room != null && room.canStartGame) {
            viewModel.prewarmPlaySync(roomId)
        }
    }

    var showLeaveConfirm by remember { mutableStateOf(false) }

    if (room == null) {
        LaunchedEffect(roomId) {
            kotlinx.coroutines.delay(6_000L)
            if (rooms.none { it.roomId == roomId }) {
                onNavigateToGameCenter()
            }
        }
        GameEnterLoadingScreen(
            gameTitle = "对战大厅",
            gameEmoji = "🎮",
            headline = if (roomId in optimisticAccepted) "正在确认邀请…" else "正在进入对战大厅…",
        )
        return
    }

    val entry = SocialGameCatalog.find(room.gameId)
    val maxPlayers = entry?.maxPlayers?.coerceIn(2, 4) ?: room.maxPlayers
    val minPlayers = entry?.minPlayers?.coerceIn(2, maxPlayers) ?: room.minPlayers
    val isHost = myPbId != null && room.hostPbId == myPbId
    val optimisticFriendPbId = optimisticPending[roomId]
    val effectiveInvitePending = room.isInvitePending || optimisticFriendPbId != null
    val guestCanRespond = roomId !in handledInvite &&
        roomId !in optimisticAccepted &&
        !myPbId.isNullOrBlank() && room.isInvitePending &&
        room.joinedMembers.none { it.pbId == myPbId } &&
        (room.guestPbId == myPbId || room.pendingInvitePbId == myPbId)
    val canInviteFriend = isHost && room.status == GameRoomStatus.WAITING &&
        !effectiveInvitePending && !room.isRoomFull
    val showDeclineBanner = isHost && room.declinedByGuest && room.isSoloLobby
    val showInviteSection = isHost && !guestCanRespond && !room.isRoomFull && !isStartingGame
    val joinedCount = room.joinedCount
    val canStart = isHost && room.canStartGame
    val isJoinedGuest = !isHost && !myPbId.isNullOrBlank() &&
        room.joinedMembers.any { it.pbId == myPbId }
    val isRoomParticipant = !isHost && !myPbId.isNullOrBlank() &&
        room.members.any { it.pbId == myPbId }
    val exitAction = LobbyExitPolicy.resolve(
        isHost = isHost,
        guestCanRespond = guestCanRespond,
        isRoomParticipant = isRoomParticipant,
        isJoinedGuest = isJoinedGuest,
    )
    val pendingFriendPbId = room.pendingInvitePbId
        ?: room.guestPbId.takeIf { room.isInvitePending }
        ?: optimisticFriendPbId
    val pendingFriendName = acceptedFriends.firstOrNull { it.friendPbId == pendingFriendPbId }
        ?.displayName?.ifBlank { acceptedFriends.firstOrNull { it.friendPbId == pendingFriendPbId }?.funlifeUsername }
        ?: room.guestDisplayName ?: room.peerDisplayName ?: "好友"

    var inviteExpanded by remember { mutableStateOf(true) }

    val friendProfiles = remember(acceptedFriends) {
        acceptedFriends.associate { friend ->
            friend.friendPbId to (
                friend.displayName.ifBlank { friend.funlifeUsername } to friend.avatarUrl
            )
        }
    }

    val performExit = {
        if (LobbyExitPolicy.needsServerNotify(exitAction)) {
            viewModel.exitLobby(roomId, exitAction, onNavigateBack)
        } else {
            viewModel.stopLobbySync(roomId)
            onNavigateBack()
        }
    }

    val handleBackRequest = { showLeaveConfirm = true }

    BackHandler(onBack = if (isStartingGame) ({ }) else handleBackRequest)

    if (isStartingGame) {
        SocialGameEnterLoading(
            gameId = room.gameId,
            gameTitle = room.gameTitle,
            gameEmoji = entry?.iconEmoji ?: "🎮",
            headline = "正在进入${room.gameTitle}",
        )
        return
    }

    SocialGameScaffold(
        title = when {
            guestCanRespond -> "${room.gameTitle} · 邀请"
            else -> "${room.gameTitle} · 对战大厅"
        },
        onNavigateBack = handleBackRequest,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (showDeclineBanner) {
                        DeclineNoticeCard(
                            peerName = room.guestDisplayName ?: "好友",
                            onDismiss = { viewModel.dismissDeclineNotice(roomId) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    when {
                        guestCanRespond -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                GameInviteRequestPanel(
                                    invite = room,
                                    pbAuthToken = pbAuthToken,
                                    onAccept = {
                                        viewModel.acknowledgeIncomingInvite(roomId)
                                        viewModel.acceptInvite(roomId)
                                    },
                                    onReject = {
                                        viewModel.exitLobby(
                                            roomId = roomId,
                                            action = com.example.funlife.social.game.model.LobbyExitAction.REJECT_INVITE,
                                            onDone = onNavigateBack,
                                        )
                                    },
                                )
                            }
                        }
                        else -> {
                            GameLobbyCompactPanel(
                                room = room,
                                gameId = entry?.gameId ?: room.gameId,
                                gameEmoji = entry?.iconEmoji ?: "🎮",
                                maxPlayers = maxPlayers,
                                minPlayers = minPlayers,
                                hostName = if (isHost) myDisplayName else (room.hostDisplayName ?: "房主"),
                                hostAvatarUrl = room.hostAvatarUrl,
                                hostLocalAvatarUri = if (isHost) myLocalAvatarUri else null,
                                pbAuthToken = pbAuthToken,
                                roomCode = room.roomCode.takeIf { it.isNotBlank() },
                                showRoomCode = isHost && room.inviteMode == InviteMode.OPEN,
                                onCopyRoomCode = { copyRoomShare(context, room.gameTitle, room.roomCode) },
                                onShareRoomCode = { copyRoomShare(context, room.gameTitle, room.roomCode) },
                                friendProfiles = friendProfiles,
                            )

                            if (room.gameId == "pac_maze") {
                                Spacer(Modifier.height(8.dp))
                                com.example.funlife.ui.screens.pacmaze.online.PacMazeLobbyPanel(
                                    room = room,
                                    pac = room.pacMaze,
                                    isHost = isHost,
                                    myPbId = myPbId,
                                    onToggleReady = { ready ->
                                        viewModel.togglePacMazeReady(roomId, ready)
                                    },
                                )
                            }

                            if (effectiveInvitePending && isHost && !isStartingGame) {
                                Spacer(Modifier.height(8.dp))
                                InviteStatusBanner(
                                    peerName = pendingFriendName,
                                    onWithdraw = { viewModel.withdrawInvite(roomId) },
                                )
                            }

                            if (showInviteSection) {
                                Spacer(Modifier.height(8.dp))
                                CollapsibleLobbySection(
                                    title = "邀请好友",
                                    subtitle = if (canInviteFriend) {
                                        "仅在线好友可邀请，点击右侧「邀请」发送对战请求"
                                    } else {
                                        "邀请处理中，可撤回后重新邀请"
                                    },
                                    expanded = inviteExpanded,
                                    onToggle = { inviteExpanded = !inviteExpanded },
                                ) {
                                    CompactInviteFriendRow(
                                        friends = acceptedFriends,
                                        pbAuthToken = pbAuthToken,
                                        enabled = canInviteFriend,
                                        pendingFriendPbId = pendingFriendPbId.takeIf { room.isInvitePending },
                                        invitingFriendPbId = optimisticFriendPbId,
                                        onPick = { friend ->
                                            viewModel.inviteFriendInRoom(roomId, friend.friendPbId)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (!guestCanRespond) {
                    Spacer(Modifier.height(12.dp))
                    when {
                        canStart -> {
                            HubPrimaryButton(
                                text = when {
                                    isStartingGame -> "正在进入游戏…"
                                    room.gameId == "pac_maze" && !room.canStartGame -> "等待双方准备"
                                    else -> "开始游戏 · $joinedCount/$maxPlayers"
                                },
                                onClick = {
                                    if (!isStartingGame) {
                                        viewModel.startGame(roomId) { onStartGame(roomId) }
                                    }
                                },
                                enabled = busyMessage == null && !isStartingGame && room.canStartGame,
                                loading = isStartingGame,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            HubSecondaryButton(
                                text = "解散房间",
                                onClick = {
                                    viewModel.dissolveRoom(roomId, onNavigateToGameCenter)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        isHost -> {
                            HubSecondaryButton(
                                text = "解散房间",
                                onClick = { viewModel.dissolveRoom(roomId, onNavigateToGameCenter) },
                                enabled = busyMessage == null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showLeaveConfirm) {
        CenteredConfirmDialog(
            title = "离开房间",
            message = if (isHost) {
                "您确定离开该房间吗？房间仍会保留，可稍后从「我的对局」返回。"
            } else if (guestCanRespond) {
                "您确定离开吗？将视为拒绝本次对战邀请。"
            } else if (isJoinedGuest) {
                "您确定离开该房间吗？离开后将从座位中移除。"
            } else {
                "您确定离开该房间吗？"
            },
            confirmText = "确定离开",
            dismissText = "再想想",
            onConfirm = {
                showLeaveConfirm = false
                performExit()
            },
            onDismiss = { showLeaveConfirm = false },
        )
    }
}

@Composable
private fun DeclineNoticeCard(peerName: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, SocialGamePalette.accentCoral.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SocialGamePalette.accentCoral.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("💬", fontSize = 16.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                "$peerName 婉拒了邀请",
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "房间仍开放，可继续邀请其他好友",
                color = SocialGamePalette.inkMuted,
                fontSize = 12.sp,
            )
        }
        Icon(
            Icons.Default.Close,
            contentDescription = "关闭",
            tint = SocialGamePalette.inkMuted,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(4.dp),
        )
    }
}

private fun copyRoomShare(context: Context, gameTitle: String, roomCode: String) {
    val text = "来一局$gameTitle！房间号 $roomCode — 打开趣生活 → 趣玩中心 → 输入房间号"
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("room_code", text))
}
