package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeAccent
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeEmoji

/** 花园 / 糖果 / 古风主题顶栏：主题色条 + 统一信息布局。 */
@Composable
fun PacMazeThemedPlayHud(
    themeId: PacMazeMapThemeId,
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    attackCharges: Int,
    powerTicksLeft: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sidebar: Boolean = false,
    playerDrawScale: Float = 1f,
    onPlayerDrawScaleChange: ((Float) -> Unit)? = null,
) {
    val accent = pacMazeThemeAccent(themeId)
    if (sidebar) {
        PacMazeThemedPlayHudSidebar(
            themeId = themeId,
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
        return
    }
    val barColors = when (themeId) {
        PacMazeMapThemeId.GARDEN -> listOf(Color(0xFF2E5E3A), Color(0xFF1E3D28))
        PacMazeMapThemeId.FOOD -> listOf(Color(0xFF6D3A2E), Color(0xFF4A2418))
        PacMazeMapThemeId.CHINESE -> listOf(Color(0xFF5C3D1E), Color(0xFF3D2810))
        else -> listOf(PacMazePalette.cardFill, Color(0xFF1A2236))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(barColors))
            .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(start = 2.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.22f)),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White, modifier = Modifier.size(17.dp))
        }

        Text(
            pacMazeThemeEmoji(themeId),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        ThemedHudStat("关", levelId.toString(), accent)
        ThemedHudStat("分", score.toString(), PacMazePalette.accentGold)
        ThemedHudStat("命", lives.coerceAtLeast(0).toString(), Color(0xFFFF8A80))
        if (attackCharges > 0) {
            ThemedHudStat("攻", attackCharges.toString(), PacMazePalette.accentOrange)
        }
        if (powerTicksLeft > 0) {
            ThemedHudStat("能", "ON", PacMazePalette.accentMint)
        }
        ThemedHudStat(
            "时",
            "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
            PacMazePalette.inkSecondary,
        )
    }
}

@Composable
private fun PacMazeThemedPlayHudSidebar(
    themeId: PacMazeMapThemeId,
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    attackCharges: Int,
    powerTicksLeft: Int,
    onBack: () -> Unit,
    playerDrawScale: Float,
    onPlayerDrawScaleChange: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accent = pacMazeThemeAccent(themeId)
    val barColors = when (themeId) {
        PacMazeMapThemeId.GARDEN -> listOf(Color(0xFF2E5E3A), Color(0xFF1E3D28))
        PacMazeMapThemeId.FOOD -> listOf(Color(0xFF6D3A2E), Color(0xFF4A2418))
        PacMazeMapThemeId.CHINESE -> listOf(Color(0xFF5C3D1E), Color(0xFF3D2810))
        else -> listOf(PacMazePalette.cardFill, Color(0xFF1A2236))
    }

    Column(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(barColors))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(pacMazeThemeEmoji(themeId), fontSize = 14.sp)
            ThemedSidebarStat("关", levelId.toString(), accent)
            ThemedSidebarStat("分", score.toString(), PacMazePalette.accentGold)
            ThemedSidebarStat("命", lives.coerceAtLeast(0).toString(), Color(0xFFFF8A80))
            if (attackCharges > 0) {
                ThemedSidebarStat("攻", attackCharges.toString(), PacMazePalette.accentOrange)
            }
            if (powerTicksLeft > 0) {
                ThemedSidebarStat("能", "ON", PacMazePalette.accentMint)
            }
            ThemedSidebarStat(
                "时",
                "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                PacMazePalette.inkSecondary,
            )
        }

        if (onPlayerDrawScaleChange != null) {
            Spacer(modifier = Modifier.weight(1f))
            PacMazePlayerScaleControl(
                scale = playerDrawScale,
                onScaleChange = onPlayerDrawScaleChange,
                accent = accent,
            )
        }
    }
}

@Composable
private fun ThemedSidebarStat(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 8.sp)
        Text(value, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemedHudStat(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 9.sp)
        Text(value, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
