package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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

/** 选模式：主推单人闯关大卡片 + 次要模式横排。 */
@Composable
fun PacMazeModeSelectPanel(
    highScore: Int,
    maxLevelReached: Int,
    totalLevels: Int,
    onSelectMode: (PacMazePlayMode) -> Unit,
) {
    val solo = PacMazePlayMode.SOLO
    val secondaryPlayable = listOf(PacMazePlayMode.ENDLESS, PacMazePlayMode.MAZE)

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "选择游戏模式",
            color = PacMazePalette.inkPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "横屏游玩 · 左下摇杆 · 右下攻击",
            color = PacMazePalette.inkSecondary,
            fontSize = 13.sp,
        )

        PacMazeFeaturedModeCard(
            mode = solo,
            onClick = { onSelectMode(solo) },
        )

        Text(
            "更多模式",
            color = PacMazePalette.inkMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            secondaryPlayable.forEach { mode ->
                PacMazeCompactModeCard(
                    mode = mode,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(mode) },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PacMazeStatPill(
                label = "最高得分",
                value = highScore.toString(),
                valueColor = PacMazePalette.accentGold,
                modifier = Modifier.weight(1f),
            )
            PacMazeStatPill(
                label = "闯关进度",
                value = "$maxLevelReached / $totalLevels",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun PacMazeModeHero(highScore: Int, maxLevelReached: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(Modifier.weight(0.15f))
        PacMazeHeroBadge(modifier = Modifier.size(100.dp))
        Text(
            "豆人迷宫",
            color = PacMazePalette.accentGold,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "街机风迷宫吃豆",
            color = PacMazePalette.inkSecondary,
            fontSize = 14.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A2236))
                .border(1.dp, PacMazePalette.accentGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("历史最高", color = PacMazePalette.inkMuted, fontSize = 12.sp)
            Text(
                highScore.toString(),
                color = PacMazePalette.accentGold,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "已解锁 $maxLevelReached 关",
                color = PacMazePalette.inkPrimary,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PacMazeFeaturedModeCard(
    mode: PacMazePlayMode,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .background(PacMazePalette.ctaGradient)
            .border(2.dp, Color.White.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(mode.emoji, fontSize = 40.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        mode.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        mode.subtitle,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                    )
                    Text(
                        "点击开始闯关",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun PacMazeCompactModeCard(
    mode: PacMazePlayMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val playable = true
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(shape)
            .background(
                if (playable) {
                    Brush.linearGradient(listOf(Color(0xFF243047), Color(0xFF1A2236)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF1E2638), Color(0xFF171D2C)))
                },
            )
            .border(
                width = if (playable) 1.5.dp else 1.dp,
                color = if (playable) PacMazePalette.accentMint.copy(alpha = 0.45f) else PacMazePalette.cardBorder,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(mode.emoji, fontSize = 22.sp)
            Column {
                Text(
                    mode.title,
                    color = if (playable) PacMazePalette.inkPrimary else PacMazePalette.inkHint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    if (playable) "点击开始" else "即将上线",
                    color = if (playable) PacMazePalette.accentMint else PacMazePalette.inkHint.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }
    }
}
