package com.example.funlife.ui.screens.pacmaze.online

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.game.model.InviteMode
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.model.PacMazePlayState
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.currentPacMazeHubLayout
import com.example.funlife.ui.screens.pacmaze.pacMazeClickable
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId
import com.example.funlife.ui.screens.socialgame.SocialGameAvatar
import com.example.funlife.ui.screens.socialgame.buildLobbySeats

@Composable
fun PacMazeOnlineLobbyScreen(
    room: LocalGameRoomDraft?,
    pac: PacMazePlayState?,
    isHost: Boolean,
    myPbId: String?,
    myDisplayName: String,
    pbAuthToken: String?,
    acceptedFriends: List<FriendUiModel>,
    canInviteFriend: Boolean,
    effectiveInvitePending: Boolean,
    pendingFriendName: String,
    pendingFriendPbId: String?,
    invitingFriendPbId: String?,
    canStart: Boolean,
    isStartingGame: Boolean,
    busy: Boolean,
    onCopyRoomCode: () -> Unit,
    onShareRoomCode: () -> Unit,
    onToggleReady: (Boolean) -> Unit,
    onInviteFriend: (FriendUiModel) -> Unit,
    onWithdrawInvite: () -> Unit,
    onStartGame: () -> Unit,
    onDissolveRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()

    if (room == null) {
        PacMazeOnlineBusyOverlay(
            headline = "正在进入对战大厅",
            subtitle = "拉取房间状态 · 同步玩家席位",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    if (isStartingGame) {
        PacMazeOnlineBusyOverlay(
            headline = "正在进入对局",
            subtitle = playModeLine(pac) + " · 加载竞技场",
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val play = pac
    val accent = if (play?.matchMode == "coop_campaign") PacMazePalette.accentMint else PacMazePalette.accentOrange
    val hostName = if (isHost) myDisplayName else (room.hostDisplayName ?: "房主")
    val seats = remember(room, hostName) {
        buildLobbySeats(
            room = room,
            hostName = hostName,
            hostAvatarUrl = room.hostAvatarUrl,
            hostLocalAvatarUri = null,
            maxPlayers = room.maxPlayers,
        )
    }
    val hostSeat = seats.firstOrNull { !it.isEmpty && it.roleLabel == "房主" } ?: seats.first()
    val guestSeat = seats.drop(1).firstOrNull { !it.isEmpty }
        ?: seats.getOrNull(1)
        ?: seats.last()

    val myReady = resolveMyReady(play, myPbId, isHost)
    val hostReady = play?.playerA?.ready == true
    val guestReady = play?.playerB?.ready == true && !play.playerB.pbId.isNullOrBlank()
    val canToggleReady = room.joinedCount >= room.minPlayers

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(10.dp)))
                .background(Color(0xFF151D30).copy(alpha = 0.92f))
                .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(layout.dp(10.dp)))
                .padding(horizontal = layout.dp(12.dp), vertical = layout.dp(8.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playModeLine(play),
                    color = PacMazePalette.inkPrimary,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    arenaLine(play),
                    color = PacMazePalette.accentGold,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                )
            }
            Text(
                "${room.joinedCount}/${room.maxPlayers}",
                color = accent,
                fontSize = layout.titleSp,
                fontWeight = FontWeight.Black,
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeOnlinePlayerPod(
                seatLabel = hostSeat.roleLabel,
                displayName = hostSeat.displayName ?: "房主",
                avatarUrl = hostSeat.avatarUrl,
                pbAuthToken = pbAuthToken,
                ready = hostReady,
                accent = PacMazePalette.accentGold,
                isFilled = !hostSeat.isEmpty,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            PacMazeOnlineVsColumn(
                roomCode = room.roomCode.takeIf { isHost && room.inviteMode == InviteMode.OPEN && it.isNotBlank() },
                invitePending = effectiveInvitePending,
                pendingFriendName = pendingFriendName,
                onCopyRoomCode = onCopyRoomCode,
                onShareRoomCode = onShareRoomCode,
                onWithdrawInvite = onWithdrawInvite,
                accent = accent,
                modifier = Modifier
                    .width(layout.dp(108.dp))
                    .fillMaxHeight(),
            )

            PacMazeOnlinePlayerPod(
                seatLabel = if (guestSeat.isEmpty) "空位" else "挑战者",
                displayName = if (guestSeat.isEmpty) "待加入" else (guestSeat.displayName ?: "好友"),
                avatarUrl = guestSeat.avatarUrl,
                pbAuthToken = pbAuthToken,
                ready = guestReady,
                accent = PacMazePalette.accentCyan,
                isFilled = !guestSeat.isEmpty,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        if (isHost && !room.isRoomFull) {
            PacMazeOnlineFriendRail(
                friends = acceptedFriends,
                pbAuthToken = pbAuthToken,
                enabled = canInviteFriend,
                pendingFriendPbId = pendingFriendPbId,
                invitingFriendPbId = invitingFriendPbId,
                onPick = onInviteFriend,
            )
        }

        PacMazeOnlineLobbyDock(
            canToggleReady = canToggleReady,
            myReady = myReady,
            canStart = canStart,
            isHost = isHost,
            busy = busy,
            joinedCount = room.joinedCount,
            maxPlayers = room.maxPlayers,
            onToggleReady = onToggleReady,
            onStartGame = onStartGame,
            onDissolveRoom = onDissolveRoom,
        )
    }
}

@Composable
private fun PacMazeOnlinePlayerPod(
    seatLabel: String,
    displayName: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    ready: Boolean,
    accent: Color,
    isFilled: Boolean,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()
    val transition = rememberInfiniteTransition(label = "podPulse")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (ready) 0.75f else 0.55f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val borderColor = when {
        ready -> PacMazePalette.accentMint.copy(alpha = glow)
        isFilled -> accent.copy(alpha = 0.55f)
        else -> PacMazePalette.cardBorder
    }
    val shape = RoundedCornerShape(layout.panelRadius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isFilled) accent.copy(alpha = 0.14f) else Color(0xFF141C2E),
                        Color(0xFF101828),
                    ),
                ),
            )
            .border(1.5.dp, borderColor, shape)
            .padding(layout.panelPad),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                seatLabel,
                color = accent,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (ready) "READY" else if (isFilled) "WAIT" else "OPEN",
                color = if (ready) PacMazePalette.accentMint else PacMazePalette.inkHint,
                fontSize = (10f * layout.scale).sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(layout.dp(72.dp))
                    .clip(CircleShape)
                    .background(
                        if (ready) PacMazePalette.accentMint.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.25f),
                    )
                    .border(
                        2.dp,
                        if (ready) PacMazePalette.accentMint else borderColor,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isFilled) {
                    SocialGameAvatar(
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                        pbAuthToken = pbAuthToken,
                        size = layout.dp(58.dp),
                    )
                } else {
                    Text("+", color = PacMazePalette.inkHint, fontSize = (28f * layout.scale).sp)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                displayName,
                color = PacMazePalette.inkPrimary,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (isFilled) {
                Text(
                    if (ready) "已准备" else "未准备",
                    color = if (ready) PacMazePalette.accentMint else PacMazePalette.inkMuted,
                    fontSize = layout.captionSp,
                )
            } else {
                Text(
                    "邀请好友加入",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                )
            }
        }
    }
}

@Composable
private fun PacMazeOnlineVsColumn(
    roomCode: String?,
    invitePending: Boolean,
    pendingFriendName: String,
    onCopyRoomCode: () -> Unit,
    onShareRoomCode: () -> Unit,
    onWithdrawInvite: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            "VS",
            color = accent,
            fontSize = (26f * layout.scale).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )

        if (!roomCode.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                Text(
                    "ROOM",
                    color = PacMazePalette.inkHint,
                    fontSize = (9f * layout.scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Text(
                    roomCode,
                    color = PacMazePalette.accentGold,
                    fontSize = (14f * layout.scale).sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp))) {
                    Box(
                        modifier = Modifier
                            .size(layout.dp(28.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, onClick = onCopyRoomCode),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = PacMazePalette.inkSecondary,
                            modifier = Modifier.size(layout.dp(14.dp)),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(layout.dp(28.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, onClick = onShareRoomCode),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = PacMazePalette.inkSecondary,
                            modifier = Modifier.size(layout.dp(14.dp)),
                        )
                    }
                }
            }
        }

        if (invitePending) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                Text(
                    "邀请中",
                    color = PacMazePalette.accentPurple,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pendingFriendName,
                    color = PacMazePalette.inkSecondary,
                    fontSize = (10f * layout.scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "撤回",
                    color = PacMazePalette.accentOrange,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.pacMazeClickable(
                        sound = PacMazeUiSoundId.SecondaryAction,
                        onClick = onWithdrawInvite,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PacMazeOnlineFriendRail(
    friends: List<FriendUiModel>,
    pbAuthToken: String?,
    enabled: Boolean,
    pendingFriendPbId: String?,
    invitingFriendPbId: String?,
    onPick: (FriendUiModel) -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    if (friends.isEmpty()) {
        Text(
            "暂无好友可邀请",
            color = PacMazePalette.inkHint,
            fontSize = layout.captionSp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        return
    }
    val sorted = friends.sortedWith(
        compareByDescending<FriendUiModel> { it.online }
            .thenBy { it.displayName.ifBlank { it.funlifeUsername } },
    )
    Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
        Text(
            "邀请在线好友",
            color = PacMazePalette.inkSecondary,
            fontSize = layout.captionSp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
        ) {
            sorted.forEach { friend ->
                val name = friend.displayName.ifBlank { friend.funlifeUsername }
                val isPending = pendingFriendPbId == friend.friendPbId
                val isInviting = invitingFriendPbId == friend.friendPbId && !isPending
                val canInvite = enabled && !isPending && !isInviting && friend.online
                PacMazeOnlineFriendChip(
                    name = name,
                    avatarUrl = friend.avatarUrl,
                    pbAuthToken = pbAuthToken,
                    online = friend.online,
                    label = when {
                        isPending || isInviting -> "…"
                        !friend.online -> "离线"
                        !enabled -> "—"
                        else -> "邀请"
                    },
                    enabled = canInvite,
                    onClick = { onPick(friend) },
                )
            }
        }
    }
}

@Composable
private fun PacMazeOnlineFriendChip(
    name: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    online: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val shape = RoundedCornerShape(layout.dp(12.dp))
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF1A2438))
            .border(1.dp, PacMazePalette.cardBorder, shape)
            .pacMazeClickable(
                sound = PacMazeUiSoundId.SecondaryAction,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        SocialGameAvatar(
            displayName = name,
            avatarUrl = avatarUrl,
            pbAuthToken = pbAuthToken,
            size = layout.dp(30.dp),
            showOnline = online,
        )
        Column {
            Text(
                name,
                color = PacMazePalette.inkPrimary,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                color = if (online) PacMazePalette.accentMint else PacMazePalette.inkHint,
                fontSize = (10f * layout.scale).sp,
            )
        }
    }
}

@Composable
private fun PacMazeOnlineLobbyDock(
    canToggleReady: Boolean,
    myReady: Boolean,
    canStart: Boolean,
    isHost: Boolean,
    busy: Boolean,
    joinedCount: Int,
    maxPlayers: Int,
    onToggleReady: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onDissolveRoom: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canToggleReady) {
            val readyShape = RoundedCornerShape(layout.dp(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(readyShape)
                    .then(
                        if (myReady) {
                            Modifier.background(Color.White.copy(alpha = 0.1f), readyShape)
                        } else {
                            Modifier.background(PacMazePalette.ctaGradient, readyShape)
                        },
                    )
                    .border(
                        1.dp,
                        if (myReady) PacMazePalette.cardBorder else Color.White.copy(alpha = 0.22f),
                        readyShape,
                    )
                    .pacMazeClickable(
                        sound = PacMazeUiSoundId.PrimaryConfirm,
                        enabled = !busy,
                        onClick = { onToggleReady(!myReady) },
                    )
                    .padding(vertical = layout.dp(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (myReady) "取消准备" else "准备就绪",
                    color = Color.White,
                    fontSize = layout.buttonSp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(layout.dp(12.dp)))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(vertical = layout.dp(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "等待好友加入后可准备",
                    color = PacMazePalette.inkMuted,
                    fontSize = layout.captionSp,
                )
            }
        }

        if (canStart) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(layout.dp(12.dp)))
                    .background(PacMazePalette.ctaGradient)
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
                    .pacMazeClickable(
                        sound = PacMazeUiSoundId.PrimaryConfirm,
                        enabled = !busy,
                        onClick = onStartGame,
                    )
                    .padding(vertical = layout.dp(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "开始 · $joinedCount/$maxPlayers",
                    color = Color.White,
                    fontSize = layout.buttonSp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(layout.dp(10.dp)))
                .background(Color(0x33F87171))
                .border(1.dp, Color(0x66F87171), RoundedCornerShape(layout.dp(10.dp)))
                .pacMazeClickable(
                    sound = PacMazeUiSoundId.SecondaryAction,
                    enabled = !busy && isHost,
                    onClick = onDissolveRoom,
                )
                .padding(horizontal = layout.dp(14.dp), vertical = layout.dp(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isHost) "解散" else "离开",
                color = PacMazePalette.difficultyExtreme,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun playModeLine(pac: PacMazePlayState?): String =
    when (pac?.matchMode) {
        "coop_campaign" -> "并肩闯关 · 共享 5 命"
        else -> "豆人对决 · 150 秒竞速"
    }

private fun arenaLine(pac: PacMazePlayState?): String =
    when (pac?.matchMode) {
        "coop_campaign" -> PacMazeLevelCatalog.find(pac.levelId)?.name ?: "第 ${pac.levelId} 关"
        else -> "竞技场 ${pac?.arenaId?.removePrefix("arena_")?.toIntOrNull() ?: 1}"
    }

private fun resolveMyReady(pac: PacMazePlayState?, myPbId: String?, isHost: Boolean): Boolean {
    if (pac == null) return false
    return when {
        myPbId == pac.hostPbId -> pac.playerA.ready
        myPbId == pac.guestPbId || myPbId == pac.playerB.pbId -> pac.playerB.ready
        isHost -> pac.playerA.ready
        else -> pac.playerB.ready
    }
}
