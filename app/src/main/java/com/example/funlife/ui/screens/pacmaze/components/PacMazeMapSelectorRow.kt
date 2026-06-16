package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.PacMazeTestUnlock
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.pacMazeClickable
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeAccent

/**
 * 测试用地图选择器：全部关卡可点，展示主题标签。
 */
@Composable
fun PacMazeMapSelectorRow(
    selectedLevelId: Int,
    maxLevelReached: Int,
    isLoading: Boolean,
    onSelectLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unlockAll: Boolean = PacMazeTestUnlock.enabled,
    compact: Boolean = false,
) {
    val scroll = rememberScrollState()
    val chipWidth = if (compact) 52.dp else 80.dp
    val selectedIndex = (selectedLevelId - 1).coerceIn(0, PacMazeLevelCatalog.TOTAL_LEVELS - 1)

    LaunchedEffect(selectedLevelId) {
        val target = (selectedIndex * chipWidth.value * 1.15f).toInt()
        scroll.animateScrollBy((target - scroll.value).toFloat())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        if (!compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "快速选图",
                    color = PacMazePalette.accentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (unlockAll) "测试 · 全部可进" else "已解锁至第 $maxLevelReached 关",
                    color = PacMazePalette.inkHint,
                    fontSize = 10.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp),
        ) {
            PacMazeLevelCatalog.levels.forEach { level ->
                val theme = PacMazeThemeRegistry.themeForLevel(level.id)
                val unlocked = unlockAll || level.id <= maxLevelReached
                val selected = level.id == selectedLevelId
                PacMazeMapSelectorChip(
                    levelId = level.id,
                    levelName = level.name,
                    terrainHint = level.subtitle.substringBefore(" ·"),
                    themeName = theme.displayName,
                    themeColor = pacMazeThemeAccent(theme),
                    selected = selected,
                    enabled = unlocked && !isLoading,
                    compact = compact,
                    chipWidth = chipWidth,
                    onClick = { onSelectLevel(level.id) },
                )
            }
        }
    }
}

@Composable
private fun PacMazeMapSelectorChip(
    levelId: Int,
    levelName: String,
    terrainHint: String,
    themeName: String,
    themeColor: Color,
    selected: Boolean,
    enabled: Boolean,
    compact: Boolean,
    chipWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compact) 8.dp else 11.dp)
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = Modifier
            .width(chipWidth)
            .clip(shape)
            .background(
                if (selected) themeColor.copy(alpha = 0.12f) else Color(0xFF1A2238).copy(alpha = alpha),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) themeColor else Color.White.copy(alpha = 0.12f),
                shape = shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.MapChip, enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = if (compact) 4.dp else 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(themeColor.copy(alpha = if (enabled) 0.9f else 0.35f)),
        ) {}
        Text(
            "L$levelId",
            color = if (selected) themeColor else Color.White.copy(alpha = 0.92f),
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
        if (!compact) {
            Text(
                levelName,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                terrainHint,
                color = themeColor.copy(alpha = 0.8f),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!compact) {
            Text(
                themeName,
                color = themeColor.copy(alpha = 0.75f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
