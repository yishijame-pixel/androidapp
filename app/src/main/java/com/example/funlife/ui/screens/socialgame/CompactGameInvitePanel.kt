package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.ui.screens.pacmaze.online.PacMazeGameInvitePanel

/** 紧凑深色对战邀请卡片（非豆人迷宫游戏）。 */
@Composable
fun CompactGameInvitePanel(
    invite: LocalGameRoomDraft,
    pbAuthToken: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    acceptLoading: Boolean = false,
    rejectLoading: Boolean = false,
    buttonsEnabled: Boolean = true,
) {
    val entry = SocialGameCatalog.find(invite.gameId)
    val hostName = invite.hostDisplayName ?: invite.peerDisplayName ?: "好友"
    val accent = entry?.accentColors?.firstOrNull()?.let { Color(it) } ?: SocialGamePalette.accentPurple
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .widthIn(max = 292.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2438), Color(0xFF121828)),
                ),
            )
            .border(1.dp, SocialGamePalette.glassBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "对战邀请",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (entry != null) {
                GameCatalogHeroIcon(entry = entry, size = 28.dp)
            } else {
                Text("🎮", fontSize = 18.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry?.title ?: invite.gameTitle,
                    color = SocialGamePalette.inkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$hostName 邀请你对战",
                    color = SocialGamePalette.inkMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f))
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SocialGameAvatar(
                    displayName = hostName,
                    avatarUrl = invite.hostAvatarUrl ?: invite.peerAvatarUrl,
                    pbAuthToken = pbAuthToken,
                    size = 36.dp,
                    showOnline = true,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HubSecondaryButton(
                text = if (rejectLoading) "…" else "婉拒",
                onClick = onReject,
                enabled = buttonsEnabled && !rejectLoading,
                loading = rejectLoading,
                modifier = Modifier.weight(1f),
            )
            HubPrimaryButton(
                text = if (acceptLoading) "加入中" else "接受",
                onClick = onAccept,
                enabled = buttonsEnabled && !acceptLoading,
                loading = acceptLoading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun GameInviteRequestPanel(
    invite: LocalGameRoomDraft,
    pbAuthToken: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    acceptLoading: Boolean = false,
    rejectLoading: Boolean = false,
    buttonsEnabled: Boolean = true,
) {
    if (invite.gameId == "pac_maze") {
        PacMazeGameInvitePanel(
            invite = invite,
            pbAuthToken = pbAuthToken,
            onAccept = onAccept,
            onReject = onReject,
            modifier = modifier,
            acceptLoading = acceptLoading,
            rejectLoading = rejectLoading,
            buttonsEnabled = buttonsEnabled,
        )
    } else {
        CompactGameInvitePanel(
            invite = invite,
            pbAuthToken = pbAuthToken,
            onAccept = onAccept,
            onReject = onReject,
            modifier = modifier,
            acceptLoading = acceptLoading,
            rejectLoading = rejectLoading,
            buttonsEnabled = buttonsEnabled,
        )
    }
}
