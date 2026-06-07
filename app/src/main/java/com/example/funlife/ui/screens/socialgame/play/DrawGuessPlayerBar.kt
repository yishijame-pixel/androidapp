package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.ui.screens.socialgame.SocialGameAvatar
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import kotlinx.coroutines.delay

data class DrawGuessPlayerUi(
    val pbId: String,
    val displayName: String,
    val avatarUrl: String?,
    val localAvatarUri: String? = null,
    val score: Int,
    val isMe: Boolean,
    val isDrawer: Boolean,
)

fun buildDrawGuessPlayers(
    room: LocalGameRoomDraft?,
    play: DrawGuessPlayState,
    myPbId: String?,
    myDisplayName: String? = null,
    myLocalAvatarUri: String? = null,
): Pair<DrawGuessPlayerUi, DrawGuessPlayerUi>? {
    if (room == null) return null
    val hostId = room.hostPbId
    val guestId = room.guestPbId ?: return null
    if (hostId.isBlank() || guestId.isBlank()) return null

    fun profile(pbId: String): Triple<String, String?, String?> {
        if (pbId == myPbId) {
            return Triple(myDisplayName?.takeIf { it.isNotBlank() } ?: "我", null, myLocalAvatarUri)
        }
        room.members.firstOrNull { it.pbId == pbId }?.let { member ->
            return Triple(member.displayName?.takeIf { it.isNotBlank() } ?: "玩家", member.avatarUrl, null)
        }
        return when (pbId) {
            room.hostPbId -> Triple(room.hostDisplayName ?: "房主", room.hostAvatarUrl, null)
            room.guestPbId -> Triple(
                room.guestDisplayName ?: room.peerDisplayName ?: "对手",
                room.guestAvatarUrl ?: room.peerAvatarUrl,
                null,
            )
            else -> Triple("玩家", null, null)
        }
    }

    fun toUi(pbId: String): DrawGuessPlayerUi {
        val (name, avatar, local) = profile(pbId)
        return DrawGuessPlayerUi(
            pbId = pbId,
            displayName = name,
            avatarUrl = avatar,
            localAvatarUri = local,
            score = play.scores[pbId] ?: 0,
            isMe = pbId == myPbId,
            isDrawer = pbId == play.drawerPbId,
        )
    }

    return toUi(hostId) to toUi(guestId)
}

@Composable
fun DrawGuessPlayerBar(
    drawer: DrawGuessPlayerUi,
    guesser: DrawGuessPlayerUi,
    play: DrawGuessPlayState,
    pbAuthToken: String?,
    modifier: Modifier = Modifier,
) {
    var remainingSec by remember(play.round, play.phase, play.phaseStartedAtMs) {
        mutableIntStateOf(play.drawSeconds)
    }
    LaunchedEffect(
        play.round,
        play.phase,
        play.phaseStartedAtMs,
        play.drawSeconds,
        play.guessSeconds,
    ) {
        while (true) {
            if (play.phaseStartedAtMs > 0L) {
                val elapsed = ((System.currentTimeMillis() - play.phaseStartedAtMs) / 1000L).toInt()
                remainingSec = when (play.phase) {
                    DrawGuessPhase.DRAWING.wire -> (play.drawSeconds - elapsed).coerceAtLeast(0)
                    DrawGuessPhase.GUESSING.wire -> (play.guessSeconds - elapsed).coerceAtLeast(0)
                    else -> play.drawSeconds
                }
            } else {
                remainingSec = play.drawSeconds
            }
            delay(500)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        when (play.phase) {
            DrawGuessPhase.DRAWING.wire -> {
                Text(
                    text = "作画 ${remainingSec}s",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SocialGamePalette.bgBase)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = if (remainingSec <= 10) SocialGamePalette.accentCoral else SocialGamePalette.inkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            DrawGuessPhase.GUESSING.wire -> {
                Text(
                    text = "猜词 ${remainingSec}s",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SocialGamePalette.bgBase)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    color = if (remainingSec <= 15) SocialGamePalette.accentCoral else SocialGamePalette.inkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawGuessPlayerChip(drawer, pbAuthToken, highlight = drawer.isDrawer)
            DrawGuessPlayerChip(
                guesser,
                pbAuthToken,
                highlight = !drawer.isDrawer && play.phase == DrawGuessPhase.GUESSING.wire,
            )
        }
    }
}

@Composable
private fun DrawGuessPlayerChip(
    player: DrawGuessPlayerUi,
    pbAuthToken: String?,
    highlight: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlight) SocialGamePalette.accentPurple.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = if (highlight) 1.5.dp else 0.dp,
                color = if (highlight) SocialGamePalette.accentPurple else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Box {
            SocialGameAvatar(
                displayName = player.displayName,
                avatarUrl = player.avatarUrl ?: player.localAvatarUri,
                pbAuthToken = pbAuthToken,
                size = 36.dp,
            )
            if (player.isDrawer) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(SocialGamePalette.accentCoral)
                        .border(1.dp, Color.White, CircleShape),
                )
            }
        }
        Column {
            Text(
                text = player.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SocialGamePalette.inkPrimary,
            )
            Text(
                text = "${player.score} 分",
                fontSize = 11.sp,
                color = SocialGamePalette.inkMuted,
            )
        }
    }
}
