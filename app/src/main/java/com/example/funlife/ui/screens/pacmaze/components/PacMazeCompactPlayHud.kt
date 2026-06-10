package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.PacMazePalette

/** 全屏地图 HUD：sidebar 模式贴左侧，不遮挡地图。 */
@Composable
fun PacMazeCompactPlayHud(
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    attackCharges: Int,
    powerTicksLeft: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    themeId: PacMazeMapThemeId = PacMazeMapThemeId.CLASSIC,
    sidebar: Boolean = false,
    playerDrawScale: Float = 1f,
    onPlayerDrawScaleChange: ((Float) -> Unit)? = null,
) {
    when (themeId) {
        PacMazeMapThemeId.CYBERPUNK -> {
            if (sidebar) {
                PacMazeCyberPlayHudSidebar(
                    levelId = levelId,
                    score = score,
                    lives = lives,
                    elapsedSeconds = elapsedSeconds,
                    attackCharges = attackCharges,
                    powerTicksLeft = powerTicksLeft,
                    onBack = onBack,
                    playerDrawScale = playerDrawScale,
                    onPlayerDrawScaleChange = onPlayerDrawScaleChange,
                    modifier = modifier,
                )
            } else {
                PacMazeCyberPlayHud(
                    levelId = levelId,
                    score = score,
                    lives = lives,
                    elapsedSeconds = elapsedSeconds,
                    attackCharges = attackCharges,
                    powerTicksLeft = powerTicksLeft,
                    onBack = onBack,
                    modifier = modifier,
                )
            }
            return
        }
        PacMazeMapThemeId.GARDEN,
        PacMazeMapThemeId.FOOD,
        PacMazeMapThemeId.CHINESE,
        -> {
            PacMazeThemedPlayHud(
                themeId = themeId,
                levelId = levelId,
                score = score,
                lives = lives,
                elapsedSeconds = elapsedSeconds,
                attackCharges = attackCharges,
                powerTicksLeft = powerTicksLeft,
                onBack = onBack,
                modifier = modifier,
                sidebar = sidebar,
                playerDrawScale = playerDrawScale,
                onPlayerDrawScaleChange = onPlayerDrawScaleChange,
            )
            return
        }
        else -> Unit
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PacMazePalette.hudGradient)
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(16.dp))
            .padding(start = 2.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = PacMazePalette.inkPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        HudChip("关卡", levelId.toString(), PacMazePalette.accentCyan)
        HudChip("得分", score.toString(), PacMazePalette.accentGold)
        HudChip("生命", lives.coerceAtLeast(0).toString(), Color(0xFFFF6B6B))
        if (attackCharges > 0) {
            HudChip("攻击", attackCharges.toString(), PacMazePalette.accentOrange)
        }
        if (powerTicksLeft > 0) {
            HudChip("能量", "ON", PacMazePalette.accentMint)
        }
        HudChip(
            "时间",
            "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
            PacMazePalette.inkSecondary,
        )
    }
}

@Composable
private fun HudChip(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = PacMazePalette.inkMuted, fontSize = 10.sp)
        Text(
            value,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
