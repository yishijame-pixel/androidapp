package com.example.funlife.ui.screens.socialgame

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.funlife.social.game.model.LocalGameRoomDraft

data class LobbySeatUi(
    val index: Int,
    val roleLabel: String,
    val displayName: String?,
    val avatarUrl: String?,
    val localAvatarUri: String?,
    val isEmpty: Boolean,
)

fun buildLobbySeats(
    room: LocalGameRoomDraft,
    hostName: String,
    hostAvatarUrl: String?,
    hostLocalAvatarUri: String?,
    maxPlayers: Int,
    friendProfiles: Map<String, Pair<String?, String?>> = emptyMap(),
): List<LobbySeatUi> {
    val cap = maxPlayers.coerceIn(2, 4)
    val joined = room.joinedMembers.sortedBy { it.seat }
    return (0 until cap).map { seatIndex ->
        val member = joined.find { it.seat == seatIndex }
        when {
            member != null && member.pbId == room.hostPbId -> LobbySeatUi(
                index = seatIndex,
                roleLabel = "房主",
                displayName = member.displayName ?: hostName,
                avatarUrl = member.avatarUrl ?: hostAvatarUrl,
                localAvatarUri = hostLocalAvatarUri,
                isEmpty = false,
            )
            member != null -> {
                val friend = friendProfiles[member.pbId]
                val fallbackName = room.guestDisplayName ?: room.peerDisplayName
                val fallbackAvatar = room.guestAvatarUrl ?: room.peerAvatarUrl
                LobbySeatUi(
                    index = seatIndex,
                    roleLabel = "玩家",
                    displayName = member.displayName?.takeIf { it.isNotBlank() }
                        ?: friend?.first?.takeIf { it.isNotBlank() }
                        ?: fallbackName?.takeIf { it.isNotBlank() }
                        ?: "好友",
                    avatarUrl = member.avatarUrl
                        ?: friend?.second?.takeIf { it.isNotBlank() }
                        ?: fallbackAvatar?.takeIf { it.isNotBlank() },
                    localAvatarUri = null,
                    isEmpty = false,
                )
            }
            else -> LobbySeatUi(
                index = seatIndex,
                roleLabel = "空位",
                displayName = null,
                avatarUrl = null,
                localAvatarUri = null,
                isEmpty = true,
            )
        }
    }
}

@Composable
fun GameLobbyCompactPanel(
    room: LocalGameRoomDraft,
    gameEmoji: String,
    maxPlayers: Int,
    minPlayers: Int,
    hostName: String,
    hostAvatarUrl: String?,
    hostLocalAvatarUri: String?,
    pbAuthToken: String?,
    roomCode: String?,
    showRoomCode: Boolean,
    onCopyRoomCode: () -> Unit,
    onShareRoomCode: () -> Unit,
    friendProfiles: Map<String, Pair<String?, String?>> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val seats = buildLobbySeats(
        room = room,
        hostName = hostName,
        hostAvatarUrl = hostAvatarUrl,
        hostLocalAvatarUri = hostLocalAvatarUri,
        maxPlayers = maxPlayers,
        friendProfiles = friendProfiles,
    )
    val joined = seats.count { !it.isEmpty }
    val cap = maxPlayers.coerceIn(2, 4)

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SocialGamePalette.bgBase),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(gameEmoji, fontSize = 22.sp)
                }
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        room.gameTitle,
                        color = SocialGamePalette.inkPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "$joined/$cap 人 · 至少 $minPlayers 人可开",
                        color = SocialGamePalette.inkMuted,
                        fontSize = 11.sp,
                    )
                }
                PlayerCountBadge(joined = joined, capacity = cap, minPlayers = minPlayers)
            }

            if (showRoomCode && !roomCode.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                CompactRoomCodeBar(
                    roomCode = roomCode,
                    onCopy = onCopyRoomCode,
                    onShare = onShareRoomCode,
                )
            }

            Spacer(Modifier.height(12.dp))
            LobbySeatGrid(
                seats = seats,
                maxPlayers = cap,
                pbAuthToken = pbAuthToken,
            )
        }
    }
}

@Composable
private fun PlayerCountBadge(joined: Int, capacity: Int, minPlayers: Int) {
    val canStartSoon = joined >= minPlayers
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (canStartSoon) SocialGamePalette.accentTeal.copy(alpha = 0.12f)
                else SocialGamePalette.accentCoral.copy(alpha = 0.10f),
            )
            .border(
                1.dp,
                if (canStartSoon) SocialGamePalette.accentTeal.copy(alpha = 0.35f)
                else SocialGamePalette.accentCoral.copy(alpha = 0.30f),
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            "$joined/$capacity",
            color = if (canStartSoon) SocialGamePalette.accentTeal else SocialGamePalette.accentCoral,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CompactRoomCodeBar(
    roomCode: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SocialGamePalette.bgBase.copy(alpha = 0.7f))
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("房间号", color = SocialGamePalette.inkMuted, fontSize = 11.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            roomCode,
            color = SocialGamePalette.accentPurple,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Default.ContentCopy,
            contentDescription = "复制",
            tint = SocialGamePalette.accentPurple,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onCopy)
                .padding(6.dp),
        )
        Icon(
            Icons.Default.Share,
            contentDescription = "分享",
            tint = SocialGamePalette.accentPurple,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onShare)
                .padding(6.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LobbySeatGrid(
    seats: List<LobbySeatUi>,
    maxPlayers: Int,
    pbAuthToken: String?,
) {
    when (maxPlayers) {
        2 -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            seats.forEach { seat ->
                CompactSeatCell(
                    seat = seat,
                    pbAuthToken = pbAuthToken,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        3 -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            seats.forEach { seat ->
                CompactSeatCell(
                    seat = seat,
                    pbAuthToken = pbAuthToken,
                    avatarSize = 46.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        else -> FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2,
        ) {
            seats.forEach { seat ->
                CompactSeatCell(
                    seat = seat,
                    pbAuthToken = pbAuthToken,
                    avatarSize = 44.dp,
                    modifier = Modifier.width(148.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactSeatCell(
    seat: LobbySeatUi,
    pbAuthToken: String?,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 52.dp,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (seat.isEmpty) Color.Transparent
                else SocialGamePalette.bgBase.copy(alpha = 0.55f),
            )
            .border(
                1.dp,
                if (seat.isEmpty) SocialGamePalette.glassBorder
                else SocialGamePalette.accentPurple.copy(alpha = 0.20f),
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (seat.isEmpty) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .border(1.dp, SocialGamePalette.glassBorder, CircleShape)
                    .background(SocialGamePalette.bgBase.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = SocialGamePalette.inkMuted, fontSize = 20.sp)
            }
        } else {
            SeatAvatar(
                name = seat.displayName.orEmpty(),
                avatarUrl = seat.avatarUrl,
                localAvatarUri = seat.localAvatarUri,
                pbAuthToken = pbAuthToken,
                size = avatarSize,
            )
        }
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false)) {
            Text(
                seat.roleLabel,
                color = if (seat.isEmpty) SocialGamePalette.inkMuted else SocialGamePalette.accentTeal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                seat.displayName ?: "待加入",
                color = if (seat.isEmpty) SocialGamePalette.inkMuted else SocialGamePalette.inkPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeatAvatar(
    name: String,
    avatarUrl: String?,
    localAvatarUri: String?,
    pbAuthToken: String?,
    size: Dp,
) {
    when {
        !localAvatarUri.isNullOrBlank() -> {
            SubcomposeAsyncImage(
                model = Uri.parse(localAvatarUri),
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        else -> SocialGameAvatar(
            displayName = name,
            avatarUrl = avatarUrl,
            pbAuthToken = pbAuthToken,
            size = size,
        )
    }
}

@Composable
fun CollapsibleLobbySection(
    title: String,
    subtitle: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggle)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = SocialGamePalette.inkPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, color = SocialGamePalette.inkMuted, fontSize = 11.sp)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SocialGamePalette.inkMuted,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun CompactInviteFriendRow(
    friends: List<com.example.funlife.social.model.FriendUiModel>,
    pbAuthToken: String?,
    enabled: Boolean,
    pendingFriendPbId: String? = null,
    invitingFriendPbId: String? = null,
    onPick: (com.example.funlife.social.model.FriendUiModel) -> Unit,
) {
    if (friends.isEmpty()) {
        Text(
            "暂无好友，先去好友页添加吧",
            color = SocialGamePalette.inkMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
        return
    }
    val sorted = friends.sortedWith(
        compareByDescending<com.example.funlife.social.model.FriendUiModel> { it.online }
            .thenBy { it.displayName.ifBlank { it.funlifeUsername } },
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sorted.forEach { friend ->
            val name = friend.displayName.ifBlank { friend.funlifeUsername }
            val isPending = pendingFriendPbId == friend.friendPbId
            val isInviting = invitingFriendPbId == friend.friendPbId && !isPending
            val canInviteThis = enabled && !isPending && !isInviting && friend.online
            LobbyInviteFriendRow(
                name = name,
                username = friend.funlifeUsername,
                avatarUrl = friend.avatarUrl,
                pbAuthToken = pbAuthToken,
                online = friend.online,
                buttonText = when {
                    isPending || isInviting -> "邀请中"
                    !friend.online -> "离线"
                    !enabled -> "等待中"
                    else -> "邀请"
                },
                buttonEnabled = canInviteThis,
                buttonLoading = isInviting,
                onInvite = { onPick(friend) },
            )
        }
    }
}

@Composable
private fun LobbyInviteFriendRow(
    name: String,
    username: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    online: Boolean,
    buttonText: String,
    buttonEnabled: Boolean,
    buttonLoading: Boolean = false,
    onInvite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SocialGamePalette.bgBase.copy(alpha = 0.55f))
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SocialGameAvatar(
            displayName = name,
            avatarUrl = avatarUrl,
            pbAuthToken = pbAuthToken,
            size = 42.dp,
            showOnline = online,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                name,
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (online) "在线 · @$username" else "离线 · @$username",
                color = if (online) SocialGamePalette.online else SocialGamePalette.inkMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LobbyInviteButton(
            text = buttonText,
            enabled = buttonEnabled,
            loading = buttonLoading,
            onClick = onInvite,
        )
    }
}

@Composable
private fun LobbyInviteButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled || loading) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(SocialGamePalette.accentCoral, SocialGamePalette.accentPurple),
                        ),
                    )
                } else {
                    Modifier
                        .background(SocialGamePalette.bgBase)
                        .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(12.dp))
                },
            )
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text,
                color = if (enabled) Color.White else SocialGamePalette.inkMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
