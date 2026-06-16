package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 横屏大厅右栏：尺寸来自 [LocalPacMazeHubLayout]。 */
@Composable
fun PacMazeModeSelectPanel(
    maxLevelReached: Int,
    totalLevels: Int,
    endlessBestScore: Int,
    endlessBestWave: Int,
    mazeBestTimeMs: Long,
    onSelectMode: (PacMazePlayMode) -> Unit,
) {
    val layout = currentPacMazeHubLayout()

    if (layout.modeSelectStacked) {
        PacMazeModeSelectScrollLayout(
                layout = layout,
                maxLevelReached = maxLevelReached,
                totalLevels = totalLevels,
                endlessBestScore = endlessBestScore,
                endlessBestWave = endlessBestWave,
                mazeBestTimeMs = mazeBestTimeMs,
                onSelectMode = onSelectMode,
        )
    } else {
        PacMazeModeSelectGridLayout(
            layout = layout,
            maxLevelReached = maxLevelReached,
            totalLevels = totalLevels,
            endlessBestScore = endlessBestScore,
            endlessBestWave = endlessBestWave,
            mazeBestTimeMs = mazeBestTimeMs,
            onSelectMode = onSelectMode,
        )
    }
}

@Composable
private fun PacMazeModeSelectGridLayout(
    layout: PacMazeHubLayoutSpec,
    maxLevelReached: Int,
    totalLevels: Int,
    endlessBestScore: Int,
    endlessBestWave: Int,
    mazeBestTimeMs: Long,
    onSelectMode: (PacMazePlayMode) -> Unit,
) {
    val mainRowHeight = layout.contentAreaHeight * 0.58f
    val onlineRowMin = layout.dp(52.dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(mainRowHeight),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeFeaturedModeCard(
                mode = PacMazePlayMode.SOLO,
                statLine = "进度 $maxLevelReached / $totalLevels",
                footnote = PacMazeLevelCatalog.find(maxLevelReached)?.name ?: "第 $maxLevelReached 关",
                layout = layout,
                modifier = Modifier
                    .weight(layout.featuredWeight)
                    .fillMaxHeight(),
                onClick = { onSelectMode(PacMazePlayMode.SOLO) },
            )

            Column(
                modifier = Modifier
                    .weight(layout.secondaryColumnWeight)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layout.gap),
            ) {
                PacMazeSecondaryModeCard(
                    mode = PacMazePlayMode.ENDLESS,
                    statLine = when {
                        maxLevelReached >= PacMazeLevelCatalog.TOTAL_LEVELS ->
                            "熔炉无尽 · 最佳 $endlessBestScore"
                        endlessBestScore > 0 ->
                            "最佳 $endlessBestScore · W$endlessBestWave"
                        else -> "W1–7 基础 · 通关 L23 解锁"
                    },
                    layout = layout,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectMode(PacMazePlayMode.ENDLESS) },
                )
                PacMazeSecondaryModeCard(
                    mode = PacMazePlayMode.MAZE,
                    statLine = "最佳 ${pacMazeFormatBestTime(mazeBestTimeMs)}",
                    layout = layout,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectMode(PacMazePlayMode.MAZE) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = onlineRowMin),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeSecondaryModeCard(
                mode = PacMazePlayMode.ONLINE_VERSUS,
                statLine = "邀请好友 · 大厅内开房",
                layout = layout,
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode(PacMazePlayMode.ONLINE_VERSUS) },
            )
            PacMazeSecondaryModeCard(
                mode = PacMazePlayMode.ONLINE_COOP,
                statLine = "合作 L1–L8 · 大厅内开房",
                layout = layout,
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode(PacMazePlayMode.ONLINE_COOP) },
            )
        }
    }
}

@Composable
private fun PacMazeModeSelectScrollLayout(
    layout: PacMazeHubLayoutSpec,
    maxLevelReached: Int,
    totalLevels: Int,
    endlessBestScore: Int,
    endlessBestWave: Int,
    mazeBestTimeMs: Long,
    onSelectMode: (PacMazePlayMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeFeaturedModeCard(
            mode = PacMazePlayMode.SOLO,
            statLine = "进度 $maxLevelReached / $totalLevels",
            footnote = PacMazeLevelCatalog.find(maxLevelReached)?.name ?: "第 $maxLevelReached 关",
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.dp(88.dp)),
            onClick = { onSelectMode(PacMazePlayMode.SOLO) },
        )
        PacMazeSecondaryModeCard(
            mode = PacMazePlayMode.ENDLESS,
            statLine = when {
                endlessBestScore > 0 -> "最佳 $endlessBestScore · W$endlessBestWave"
                else -> "W1–7 基础 · 通关 L23 解锁熔炉"
            },
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.dp(56.dp)),
            onClick = { onSelectMode(PacMazePlayMode.ENDLESS) },
        )
        PacMazeSecondaryModeCard(
            mode = PacMazePlayMode.MAZE,
            statLine = "最佳 ${pacMazeFormatBestTime(mazeBestTimeMs)}",
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.dp(56.dp)),
            onClick = { onSelectMode(PacMazePlayMode.MAZE) },
        )
        PacMazeSecondaryModeCard(
            mode = PacMazePlayMode.ONLINE_VERSUS,
            statLine = "邀请好友 · 大厅内开房",
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.dp(56.dp)),
            onClick = { onSelectMode(PacMazePlayMode.ONLINE_VERSUS) },
        )
        PacMazeSecondaryModeCard(
            mode = PacMazePlayMode.ONLINE_COOP,
            statLine = "合作 L1–L8 · 大厅内开房",
            layout = layout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.dp(56.dp)),
            onClick = { onSelectMode(PacMazePlayMode.ONLINE_COOP) },
        )
    }
}

@Composable
fun PacMazeModeHero(
    continueLevelId: Int,
    onContinueCampaign: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val levelMeta = PacMazeLevelCatalog.find(continueLevelId)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(layout.gap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = layout.dp(4.dp)),
    ) {
        PacMazeHeroBadge(modifier = Modifier.size(layout.heroBadgeSize))
        Text(
            "豆人迷宫",
            color = PacMazePalette.accentGold,
            fontSize = layout.titleSp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Text(
            "街机风 · 横屏吃豆",
            color = PacMazePalette.inkSecondary,
            fontSize = layout.subtitleSp,
            maxLines = 1,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(14.dp)))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2A3550), Color(0xFF1A2236)),
                    ),
                )
                .border(1.5.dp, PacMazePalette.accentOrange.copy(alpha = 0.45f), RoundedCornerShape(layout.dp(14.dp)))
                .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(10.dp)),
            verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "快速继续",
                color = PacMazePalette.inkMuted,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                levelMeta?.let { "L${it.id} · ${it.name}" } ?: "L$continueLevelId",
                color = PacMazePalette.inkPrimary,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (layout.bodySp.value * 1.2f).sp,
            )
            PacMazePrimaryButton(
                text = "继续第 $continueLevelId 关",
                onClick = onContinueCampaign,
                compact = true,
            )
        }

        if (!layout.isVeryCompactHeight) {
            Text(
                "下方可选对决 / 合作联机",
                color = PacMazePalette.inkHint,
                fontSize = layout.captionSp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PacMazeFeaturedModeCard(
    mode: PacMazePlayMode,
    statLine: String,
    footnote: String,
    layout: PacMazeHubLayoutSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = PacMazePalette.accentOrange
    val shape = RoundedCornerShape(layout.cardRadius)
    val emojiSp = (if (layout.isCompactHeight) 22f else 28f) * layout.scale
    val titleSp = (if (layout.isCompactHeight) 15f else 17f) * layout.scale
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.24f), Color(0xFF1A2236)),
                ),
            )
            .border(1.5.dp, accent.copy(alpha = 0.55f), shape)
            .pacMazeClickable(sound = PacMazeUiSoundId.ModeFeatured, onClick = onClick)
            .padding(layout.cardPad),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                ModeAccentBar(accent = accent, height = layout.dp(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                ) {
                    Text(mode.emoji, fontSize = emojiSp.sp)
                    Text(
                        mode.title,
                        color = Color.White,
                        fontSize = titleSp.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    mode.subtitle,
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.captionSp,
                    maxLines = if (layout.isCompactHeight) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (layout.captionSp.value * 1.15f).sp,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .size(layout.playIconLarge)
                        .clip(CircleShape)
                        .background(PacMazePalette.ctaGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(layout.playIconLarge * 0.58f),
                    )
                }
                Text(
                    statLine,
                    color = PacMazePalette.accentGold,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    footnote,
                    color = PacMazePalette.inkMuted,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PacMazeSecondaryModeCard(
    mode: PacMazePlayMode,
    statLine: String,
    layout: PacMazeHubLayoutSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (mode) {
        PacMazePlayMode.ENDLESS -> PacMazePalette.modeEndless
        PacMazePlayMode.MAZE -> PacMazePalette.modeMaze
        PacMazePlayMode.ONLINE_VERSUS -> PacMazePalette.accentOrange
        PacMazePlayMode.ONLINE_COOP -> PacMazePalette.accentMint
        else -> PacMazePalette.accentMint
    }
    val shape = RoundedCornerShape(layout.dp(14.dp))
    val emojiSp = (if (layout.isCompactHeight) 15f else 18f) * layout.scale
    val titleSp = (if (layout.isCompactHeight) 12f else 13f) * layout.scale
    val iconSize = if (layout.isCompactHeight) layout.dp(26.dp) else layout.playIconSmall
    val vPad = if (layout.isCompactHeight) layout.dp(6.dp) else layout.dp(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.18f), Color(0xFF1A2236)),
                ),
            )
            .border(1.5.dp, accent.copy(alpha = 0.5f), shape)
            .pacMazeClickable(sound = PacMazeUiSoundId.ModeOption, onClick = onClick)
            .padding(horizontal = layout.dp(10.dp), vertical = vPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = layout.dp(4.dp)),
                verticalArrangement = Arrangement.spacedBy(layout.dp(2.dp)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(5.dp)),
                ) {
                    Text(mode.emoji, fontSize = emojiSp.sp)
                    Text(
                        mode.title,
                        color = PacMazePalette.inkPrimary,
                        fontSize = titleSp.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (layout.tier != PacMazeHubLayoutTier.Compact) {
                    Text(
                        mode.subtitle,
                        color = PacMazePalette.inkSecondary,
                        fontSize = layout.captionSp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = (layout.captionSp.value * 1.1f).sp,
                    )
                }
                Text(
                    statLine,
                    color = accent,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (layout.captionSp.value * 1.15f).sp,
                )
            }
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize * 0.56f),
                )
            }
        }
    }
}

@Composable
private fun ModeAccentBar(
    accent: Color,
    height: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.36f)
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accent, accent.copy(alpha = 0.35f)),
                ),
            ),
    )
}
