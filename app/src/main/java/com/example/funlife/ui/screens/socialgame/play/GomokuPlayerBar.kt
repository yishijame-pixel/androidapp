package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.GomokuEloCalculator
import com.example.funlife.social.game.engine.GomokuRank
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.social.game.engine.GomokuTimer
import com.example.funlife.social.game.model.GomokuPlayState
import com.example.funlife.social.game.model.GomokuTimerState
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.ui.screens.socialgame.SocialGameAvatar
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import kotlinx.coroutines.delay

data class GomokuPlayerUi(
    val pbId: String,
    val displayName: String,
    val avatarUrl: String?,
    val localAvatarUri: String? = null,
    val stoneLabel: String,
    val isMe: Boolean,
    val eloRating: Int? = null,
    val rank: GomokuRank? = null,
)

fun buildGomokuPlayers(
    room: LocalGameRoomDraft?,
    gomoku: GomokuPlayState,
    myPbId: String?,
    myDisplayName: String? = null,
    myLocalAvatarUri: String? = null,
): Pair<GomokuPlayerUi, GomokuPlayerUi>? {
    if (room == null || gomoku.blackPbId.isBlank() || gomoku.whitePbId.isBlank()) return null

    fun profile(pbId: String): Triple<String, String?, String?> {
        if (pbId == myPbId) {
            val name = myDisplayName?.takeIf { it.isNotBlank() } ?: "我"
            return Triple(name, null, myLocalAvatarUri?.takeIf { it.isNotBlank() })
        }
        room.members.firstOrNull { it.pbId == pbId }?.let { member ->
            val name = member.displayName?.takeIf { it.isNotBlank() } ?: "玩家"
            return Triple(name, member.avatarUrl, null)
        }
        return when (pbId) {
            room.hostPbId -> Triple(
                room.hostDisplayName ?: "房主",
                room.hostAvatarUrl,
                null,
            )
            room.guestPbId -> Triple(
                room.guestDisplayName ?: room.peerDisplayName ?: "对手",
                room.guestAvatarUrl ?: room.peerAvatarUrl,
                null,
            )
            else -> Triple("玩家", null, null)
        }
    }

    fun toUi(pbId: String, stoneLabel: String): GomokuPlayerUi {
        val (name, avatar, localUri) = profile(pbId)
        return GomokuPlayerUi(
            pbId = pbId,
            displayName = name,
            avatarUrl = avatar,
            localAvatarUri = localUri,
            stoneLabel = stoneLabel,
            isMe = myPbId == pbId,
        )
    }

    return toUi(gomoku.blackPbId, "黑棋") to toUi(gomoku.whitePbId, "白棋")
}

@Composable
fun GomokuPlayerBar(
    black: GomokuPlayerUi,
    white: GomokuPlayerUi,
    currentTurnPbId: String?,
    pbAuthToken: String?,
    timer: GomokuTimerState? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 计时器显示（如果启用）
        if (timer != null && timer.enabled) {
            GomokuTimerRow(
                timer = timer,
                currentTurnPbId = currentTurnPbId,
                blackPbId = black.pbId,
                whitePbId = white.pbId,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }

        // 玩家卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GomokuPlayerChip(
                player = black,
                isActive = currentTurnPbId == black.pbId,
                pbAuthToken = pbAuthToken,
                stoneColor = Color(0xFF1A1A2E),
                modifier = Modifier.weight(1f),
            )
            Text("VS", color = SocialGamePalette.inkMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            GomokuPlayerChip(
                player = white,
                isActive = currentTurnPbId == white.pbId,
                pbAuthToken = pbAuthToken,
                stoneColor = Color.White,
                stoneBorder = Color(0xFF555555),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GomokuTimerRow(
    timer: GomokuTimerState,
    currentTurnPbId: String?,
    blackPbId: String,
    whitePbId: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveCompactTimerBadge(
            timer = timer,
            playerColor = GomokuRules.CELL_BLACK,
            currentTurnPbId = currentTurnPbId,
            blackPbId = blackPbId,
            whitePbId = whitePbId,
        )
        Text(
            " ⏱ ",
            color = SocialGamePalette.inkMuted,
            fontSize = 14.sp,
        )
        LiveCompactTimerBadge(
            timer = timer,
            playerColor = GomokuRules.CELL_WHITE,
            currentTurnPbId = currentTurnPbId,
            blackPbId = blackPbId,
            whitePbId = whitePbId,
        )
    }
}

/** 本地头像优先（与大厅一致），避免进房先显示占位符 */
@Composable
private fun PlayPlayerAvatar(
    displayName: String,
    avatarUrl: String?,
    localAvatarUri: String?,
    pbAuthToken: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    val localBitmap = com.example.funlife.utils.AvatarImageLoader.rememberLocalAvatarBitmap(
        localAvatarUri ?: avatarUrl,
    )
    if (localBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = localBitmap,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        return
    }
    SocialGameAvatar(
        displayName = displayName,
        avatarUrl = avatarUrl,
        pbAuthToken = pbAuthToken,
        size = size,
    )
}

/** 计时器自刷新，不带动父级头像卡片重绘 */
@Composable
private fun LiveCompactTimerBadge(
    timer: GomokuTimerState,
    playerColor: Char,
    currentTurnPbId: String?,
    blackPbId: String,
    whitePbId: String,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }
    val currentColor = when (currentTurnPbId) {
        blackPbId -> GomokuRules.CELL_BLACK
        whitePbId -> GomokuRules.CELL_WHITE
        else -> null
    }
    val isActive = currentColor == playerColor
    val remaining = if (isActive) {
        GomokuTimer.getCurrentRemaining(timer, playerColor, nowMs)
    } else {
        if (playerColor == GomokuRules.CELL_BLACK) timer.blackRemainingMs else timer.whiteRemainingMs
    }
    CompactTimerBadge(remainingMs = remaining, isActive = isActive)
}

@Composable
private fun GomokuPlayerChip(
    player: GomokuPlayerUi,
    isActive: Boolean,
    pbAuthToken: String?,
    stoneColor: Color,
    stoneBorder: Color? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (isActive) SocialGamePalette.accentPurple.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.65f),
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) SocialGamePalette.accentPurple else SocialGamePalette.glassBorder,
                shape = shape,
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            key(player.pbId, player.avatarUrl, player.localAvatarUri) {
                PlayPlayerAvatar(
                    displayName = player.displayName,
                    avatarUrl = player.avatarUrl,
                    localAvatarUri = player.localAvatarUri,
                    pbAuthToken = pbAuthToken,
                    size = 44.dp,
                )
            }
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(stoneColor)
                    .then(
                        if (stoneBorder != null) {
                            Modifier.border(1.dp, stoneBorder, CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        Text(
            player.displayName + if (player.isMe) "（我）" else "",
            color = SocialGamePalette.inkPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        // 段位显示（如果有）
        player.rank?.let { rank ->
            Spacer(Modifier.height(2.dp))
            CompactRankTag(rank = rank)
        }

        Text(
            player.stoneLabel,
            color = SocialGamePalette.inkMuted,
            fontSize = 11.sp,
        )
        if (isActive) {
            Text(
                "思考中",
                color = SocialGamePalette.accentPurple,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
