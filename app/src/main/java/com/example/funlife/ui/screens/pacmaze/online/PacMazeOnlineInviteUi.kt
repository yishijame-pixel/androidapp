package com.example.funlife.ui.screens.pacmaze.online

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
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.ui.screens.pacmaze.PacMazeHeroBadge
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.pacMazeClickable
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId
import com.example.funlife.ui.screens.socialgame.SocialGameAvatar

/** 豆人迷宫 · 紧凑街机风对战邀请卡片。 */
@Composable
fun PacMazeGameInvitePanel(
    invite: LocalGameRoomDraft,
    pbAuthToken: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    acceptLoading: Boolean = false,
    rejectLoading: Boolean = false,
    buttonsEnabled: Boolean = true,
) {
    val pac = invite.pacMaze
    val isCoop = pac?.matchMode == "coop_campaign"
    val accent = if (isCoop) PacMazePalette.accentMint else PacMazePalette.accentOrange
    val hostName = invite.hostDisplayName ?: invite.peerDisplayName ?: "好友"
    val modeLine = when (pac?.matchMode) {
        "coop_campaign" -> "并肩闯关 · L${pac.levelId.coerceIn(1, 8)}"
        else -> "豆人对决 · 150 秒竞速"
    }
    val detailLine = when (pac?.matchMode) {
        "coop_campaign" -> PacMazeLevelCatalog.find(pac.levelId)?.name ?: "合作模式"
        else -> "竞技场 ${pac?.arenaId?.removePrefix("arena_")?.toIntOrNull() ?: 1}"
    }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .widthIn(max = 292.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF243047), Color(0xFF141C2E)),
                ),
            )
            .border(1.5.dp, accent.copy(alpha = 0.55f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent.copy(alpha = 0.22f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "INVITE",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    "对战邀请",
                    color = PacMazePalette.inkSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text("👾", fontSize = 16.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PacMazeHeroBadge(modifier = Modifier.size(44.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "豆人迷宫",
                    color = PacMazePalette.inkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    modeLine,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detailLine,
                    color = PacMazePalette.inkMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f))
                        .border(1.5.dp, accent.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SocialGameAvatar(
                        displayName = hostName,
                        avatarUrl = invite.hostAvatarUrl ?: invite.peerAvatarUrl,
                        pbAuthToken = pbAuthToken,
                        size = 38.dp,
                        showOnline = true,
                    )
                }
                Text(
                    hostName,
                    color = PacMazePalette.inkPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            "$hostName 邀请你加入对局",
            color = PacMazePalette.inkHint,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PacMazeInviteActionButton(
                text = if (rejectLoading) "…" else "婉拒",
                accent = PacMazePalette.difficultyExtreme,
                filled = false,
                enabled = buttonsEnabled && !rejectLoading,
                modifier = Modifier.weight(1f),
                onClick = onReject,
            )
            PacMazeInviteActionButton(
                text = if (acceptLoading) "加入中" else "接受",
                accent = accent,
                filled = true,
                enabled = buttonsEnabled && !acceptLoading,
                modifier = Modifier.weight(1f),
                onClick = onAccept,
            )
        }
    }
}

@Composable
private fun PacMazeInviteActionButton(
    text: String,
    accent: Color,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(
                        Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.75f))),
                        shape,
                    )
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.06f), shape)
                        .border(1.dp, accent.copy(alpha = 0.45f), shape)
                },
            )
            .pacMazeClickable(
                sound = if (filled) PacMazeUiSoundId.PrimaryConfirm else PacMazeUiSoundId.SecondaryAction,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (filled) Color.White else accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
