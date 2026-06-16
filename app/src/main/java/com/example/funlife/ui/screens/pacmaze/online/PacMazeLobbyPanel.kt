package com.example.funlife.ui.screens.pacmaze.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.model.PacMazePlayState
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.PacMazePrimaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton
import com.example.funlife.ui.screens.socialgame.SocialGamePalette

@Composable
fun PacMazeLobbyPanel(
    room: LocalGameRoomDraft,
    pac: PacMazePlayState?,
    isHost: Boolean,
    myPbId: String?,
    onToggleReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val play = pac ?: return
    val modeLabel = when (play.matchMode) {
        "coop_campaign" -> "并肩闯关 · L${play.levelId.coerceIn(1, 8)}"
        else -> "豆人对决 · 150 秒竞速"
    }
    val levelName = if (play.matchMode == "coop_campaign") {
        PacMazeLevelCatalog.find(play.levelId)?.name ?: "第 ${play.levelId} 关"
    } else {
        "竞技场 ${play.arenaId.removePrefix("arena_").toIntOrNull() ?: 1}"
    }
    val myReady = when {
        myPbId == play.hostPbId -> play.playerA.ready
        myPbId == play.guestPbId || myPbId == play.playerB.pbId -> play.playerB.ready
        isHost -> play.playerA.ready
        else -> play.playerB.ready
    }
    val hostReady = play.playerA.ready
    val guestReady = play.playerB.ready && play.playerB.pbId.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, SocialGamePalette.accentMint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "豆人迷宫 · $modeLabel",
            color = SocialGamePalette.inkPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            levelName,
            color = SocialGamePalette.inkMuted,
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadyChip(label = room.hostDisplayName ?: "房主", ready = hostReady)
            ReadyChip(label = room.guestDisplayName ?: room.peerDisplayName ?: "好友", ready = guestReady)
        }
        if (room.joinedCount >= room.minPlayers) {
            if (myReady) {
                PacMazeSecondaryButton(
                    text = "取消准备",
                    onClick = { onToggleReady(false) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PacMazePrimaryButton(
                    text = "准备",
                    onClick = { onToggleReady(true) },
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Text(
                "等待好友加入后可准备",
                color = PacMazePalette.inkMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ReadyChip(label: String, ready: Boolean) {
    val accent = if (ready) PacMazePalette.accentMint else PacMazePalette.inkMuted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (ready) "✓" else "○",
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            color = SocialGamePalette.inkPrimary,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}
