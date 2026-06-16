package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreview
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapSelectorRow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.PathEffect
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeAccent

/**
 * 选关：左侧紧凑档案栏 + 右侧 S 形豆粒闯关径（街机风，高空间利用率）。
 */
@Composable
fun PacMazeLevelSelectPanel(
    maxLevelReached: Int,
    starsBitmask: Int,
    continueLevelId: Int,
    selectedSkinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId,
    isLoading: Boolean,
    loadError: String?,
    onContinue: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onPracticeLevel: (Int) -> Unit,
    onChangeCharacter: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val totalLevels = PacMazeLevelCatalog.levels.size

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeCampaignSidebar(
                maxLevelReached = maxLevelReached,
                totalLevels = totalLevels,
                continueLevelId = continueLevelId,
                continueLevelName = PacMazeLevelCatalog.find(continueLevelId)?.name ?: "",
                continueLevelSubtitle = PacMazeLevelCatalog.find(continueLevelId)?.subtitle ?: "",
                selectedSkinId = selectedSkinId,
                enabled = !isLoading,
                layout = layout,
                onContinue = onContinue,
                onPractice = { onPracticeLevel(continueLevelId) },
                onChangeCharacter = onChangeCharacter,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(layout.panelRadius))
                    .background(Color(0xFF151D30))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.panelRadius)),
            ) {
                PacMazeSerpentineMapPanel(
                    maxLevelReached = maxLevelReached,
                    starsBitmask = starsBitmask,
                    continueLevelId = continueLevelId,
                    isLoading = isLoading,
                    onSelectLevel = onSelectLevel,
                )

                loadError?.let { msg ->
                    Text(
                        msg,
                        color = Color(0xFFFFAB91),
                        fontSize = layout.captionSp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = layout.dp(4.dp))
                            .clip(RoundedCornerShape(layout.dp(8.dp)))
                            .background(Color(0xCC2A1510))
                            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(3.dp)),
                    )
                }

                Text(
                    "滑动闯关径 · 或下方快速选图",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = layout.dp(if (layout.isCompactHeight) 58.dp else 68.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x66151D30))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )

                PacMazeMapSelectorRow(
                    selectedLevelId = continueLevelId,
                    maxLevelReached = maxLevelReached,
                    isLoading = isLoading,
                    onSelectLevel = onSelectLevel,
                    unlockAll = PacMazeTestUnlock.enabled,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
fun PacMazeCampaignSidebar(
    maxLevelReached: Int,
    totalLevels: Int,
    continueLevelId: Int,
    continueLevelName: String,
    continueLevelSubtitle: String,
    selectedSkinId: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId,
    enabled: Boolean,
    layout: PacMazeHubLayoutSpec,
    onContinue: () -> Unit,
    onPractice: () -> Unit,
    onChangeCharacter: () -> Unit,
    showPractice: Boolean = true,
) {
    val compact = layout.isCompactHeight
    val btnVPad = layout.dp(if (compact) 7.dp else 10.dp)
    val practiceLabel = if (compact) "练习本关" else "练习本关（无限命）"

    Column(
        modifier = Modifier
            .widthIn(min = layout.levelSidebarWidth, max = layout.dp(152.dp))
            .fillMaxHeight()
            .clip(RoundedCornerShape(layout.panelRadius))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF243047), Color(0xFF1A2236))),
            )
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(10.dp)))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(6.dp)),
            verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
        ) {
            Text("当前目标", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
            Text(
                continueLevelName,
                color = PacMazePalette.accentGold,
                fontSize = layout.subtitleSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                continueLevelSubtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = layout.captionSp,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (layout.captionSp.value * 1.3f).sp,
            )
            val theme = PacMazeThemeRegistry.themeForLevel(continueLevelId)
            val themeColor = pacMazeThemeAccent(theme)
            Text(
                "${pacMazeThemeEmoji(theme)} ${theme.displayName}",
                color = themeColor,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(themeColor.copy(alpha = 0.12f))
                    .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                    .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(2.dp)),
            )
        }

        PacMazeCheckpointStrip(
            total = totalLevels,
            reached = maxLevelReached,
            current = continueLevelId,
            layout = layout,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(10.dp)))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
                .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onChangeCharacter)
                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PacMazeCharacterPreview(
                skinId = selectedSkinId,
                modifier = Modifier.size(layout.dp(if (compact) 32.dp else 36.dp)),
                animateWalk = false,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("当前角色", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
                Text(
                    selectedSkinId.displayName,
                    color = PacMazePalette.inkPrimary,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "更换",
                color = PacMazePalette.accentCyan,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(12.dp)))
                .background(PacMazePalette.ctaGradient)
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
                .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, enabled = enabled, onClick = onContinue)
                .padding(horizontal = layout.dp(8.dp), vertical = btnVPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(if (compact) 28.dp else 32.dp))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(layout.dp(16.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("继续", color = Color.White, fontSize = layout.subtitleSp, fontWeight = FontWeight.Bold)
                Text(
                    "第${continueLevelId}关 · $continueLevelName",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = layout.captionSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showPractice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(layout.dp(12.dp)))
                    .background(Color(0xFF1E3A28))
                    .border(1.dp, PacMazePalette.accentMint.copy(alpha = 0.45f), RoundedCornerShape(layout.dp(12.dp)))
                    .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, enabled = enabled, onClick = onPractice)
                    .padding(horizontal = layout.dp(8.dp), vertical = btnVPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    practiceLabel,
                    color = PacMazePalette.accentMint,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PacMazeCheckpointStrip(
    total: Int,
    reached: Int,
    current: Int,
    layout: PacMazeHubLayoutSpec,
) {
    val scroll = rememberScrollState()
    Column(verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("闯关径", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
            Text(
                "$reached/$total",
                color = PacMazePalette.accentMint,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total) { index ->
                val levelId = index + 1
                val unlocked = levelId <= reached
                val isCurrent = levelId == current
                val dotSize = when {
                    isCurrent -> layout.dp(12.dp)
                    layout.isCompactHeight -> layout.dp(8.dp)
                    else -> layout.dp(10.dp)
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> PacMazePalette.accentGold
                                unlocked -> PacMazePalette.accentMint.copy(alpha = 0.85f)
                                else -> PacMazePalette.locked.copy(alpha = 0.55f)
                            },
                        )
                        .then(
                            if (isCurrent) {
                                Modifier.border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun PacMazeMechanismChipRow(
    mechanisms: List<PacMazeMechanismKind>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        mechanisms.take(4).forEach { kind ->
            Text(
                text = kind.glyph,
                color = PacMazePalette.inkHint,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
fun PacMazeSerpentineMapPanel(
    maxLevelReached: Int,
    starsBitmask: Int,
    continueLevelId: Int,
    isLoading: Boolean,
    onSelectLevel: (Int) -> Unit,
    showQuickSelect: Boolean = true,
    showChapterZones: Boolean = false,
    unlockAll: Boolean = PacMazeTestUnlock.enabled,
) {
    val layout = currentPacMazeHubLayout()
    val levels = PacMazeLevelCatalog.levels
    val metrics = rememberSerpentineMetrics(levels.size, layout, showChapterZones)
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val contentWidth = metrics.contentWidth(levels.size)
    val chapterStarts = remember(levels.size) {
        levels.mapIndexedNotNull { index, level ->
            val prev = levels.getOrNull(index - 1)
            if (prev == null || PacMazeChapterCatalog.chapterForLevel(prev.id) != PacMazeChapterCatalog.chapterForLevel(level.id)) {
                index to PacMazeChapterCatalog.chapterForLevel(level.id)
            } else {
                null
            }
        }
    }
    val chapterStartMap = remember(chapterStarts) { chapterStarts.toMap() }

    LaunchedEffect(continueLevelId, contentWidth) {
        val index = (continueLevelId - 1).coerceIn(0, levels.lastIndex)
        val stepPx = with(density) { (metrics.columnWidth + metrics.hGap).toPx() }
        val target = (index * stepPx - stepPx * 1.5f).coerceAtLeast(0f)
        scroll.animateScrollBy(target - scroll.value)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        if (showChapterZones) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(4.dp)),
                horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "全景闯关径",
                    color = PacMazePalette.accentCyan,
                    fontSize = layout.subtitleSp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    "滑动浏览 · 点节点进详情",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scroll),
            ) {
                Box(
                    modifier = Modifier
                        .width(contentWidth)
                        .fillMaxHeight(),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (showChapterZones) {
                            drawChapterZoneBackgrounds(
                                levels = levels,
                                metrics = metrics,
                                density = density,
                            )
                        }
                        val centers = levels.indices.map { metrics.nodeCenter(it, density) }
                        drawSerpentineConnections(
                            centers = centers,
                            maxLevelReached = maxLevelReached,
                            unlockAll = unlockAll,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.hGap),
                        verticalAlignment = Alignment.Top,
                    ) {
                        levels.forEachIndexed { index, level ->
                            val unlocked = unlockAll || level.id <= maxLevelReached
                            val stars = if (unlocked) decodePacMazeStars(starsBitmask, level.id) else 0
                            val highlighted = level.id == continueLevelId && unlocked
                            val diffColor = PacMazeLevelCatalog.difficultyColor(level.difficulty)
                            val themeColor = pacMazeThemeAccent(PacMazeThemeRegistry.themeForLevel(level.id))
                            val zoneChapter = chapterStartMap[index]

                            Column(
                                modifier = Modifier
                                    .width(metrics.columnWidth)
                                    .padding(top = metrics.columnTop(index)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
                            ) {
                                when {
                                    zoneChapter != null -> PacMazeChapterZoneTag(
                                        chapter = zoneChapter,
                                        layout = layout,
                                    )
                                    index == 0 -> PacMazePathTag("起点", PacMazePalette.accentMint, layout)
                                    index == levels.lastIndex -> PacMazePathTag("终章", PacMazePalette.accentOrange, layout)
                                    else -> Spacer(Modifier.height(metrics.tagRowHeight))
                                }

                                Box(
                                    modifier = Modifier.height(metrics.orbBoxHeight),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PacMazeOrbNode(
                                        level = level,
                                        unlocked = unlocked,
                                        highlighted = highlighted,
                                        themeColor = themeColor,
                                        enabled = unlocked && !isLoading,
                                        onClick = { onSelectLevel(level.id) },
                                        size = metrics.nodeSize,
                                    )
                                    if (highlighted) {
                                        PacMazeHeroBadge(
                                            modifier = Modifier
                                                .size(layout.dp(20.dp))
                                                .align(Alignment.TopEnd)
                                                .offset(x = layout.dp(4.dp), y = -layout.dp(4.dp))
                                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                ) {
                                    Text(
                                        level.name,
                                        color = if (unlocked) PacMazePalette.inkPrimary else PacMazePalette.inkHint,
                                        fontSize = layout.captionSp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )
                                    if (unlocked) {
                                        Text(
                                            level.difficulty,
                                            color = diffColor,
                                            fontSize = layout.captionSp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                    }
                                    PacMazeStarRow(
                                        stars = stars,
                                        maxStars = 3,
                                        starSize = (10f * layout.scale).sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!showChapterZones && !layout.isCompactHeight) {
                Text(
                    "滑动闯关径 · 路径连接相邻关卡",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = layout.dp(2.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x88151D30))
                        .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(2.dp)),
                )
            }
        }

        if (showQuickSelect) {
            PacMazeMapSelectorRow(
                selectedLevelId = continueLevelId,
                maxLevelReached = maxLevelReached,
                isLoading = isLoading,
                onSelectLevel = onSelectLevel,
                unlockAll = unlockAll,
                compact = true,
            )
        }
    }
}

private data class SerpentineMetrics(
    val nodeSize: Dp,
    val columnWidth: Dp,
    val hGap: Dp,
    val tagRowHeight: Dp,
    val orbBoxHeight: Dp,
    val zigzagLowTop: Dp,
    val zigzagHighTop: Dp,
) {
    fun contentWidth(levelCount: Int): Dp =
        columnWidth * levelCount + hGap * (levelCount - 1).coerceAtLeast(0)

    fun columnTop(index: Int): Dp = if (index % 2 == 0) zigzagLowTop else zigzagHighTop

    fun nodeCenter(index: Int, density: androidx.compose.ui.unit.Density): Offset {
        val x = with(density) {
            index * (columnWidth + hGap).toPx() + columnWidth.toPx() / 2f
        }
        val y = with(density) {
            columnTop(index).toPx() + tagRowHeight.toPx() + orbBoxHeight.toPx() / 2f
        }
        return Offset(x, y)
    }
}

@Composable
private fun PacMazeChapterZoneTag(
    chapter: PacMazeCampaignChapter,
    layout: PacMazeHubLayoutSpec,
) {
    val accent = pacMazeThemeAccent(chapter.themeId)
    Column(
        modifier = Modifier
            .height(layout.dp(28.dp))
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(layout.dp(8.dp)))
            .padding(horizontal = layout.dp(4.dp), vertical = layout.dp(2.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(pacMazeThemeEmoji(chapter.themeId), fontSize = layout.captionSp)
        Text(
            chapter.displayName,
            color = accent,
            fontSize = (layout.captionSp.value * 0.85f).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawChapterZoneBackgrounds(
    levels: List<PacMazeLevelMeta>,
    metrics: SerpentineMetrics,
    density: androidx.compose.ui.unit.Density,
) {
    if (levels.isEmpty()) return
    var zoneStart = 0
    var zoneChapter = PacMazeChapterCatalog.chapterForLevel(levels.first().id)
    fun flushZone(endIndex: Int) {
        if (zoneStart >= endIndex) return
        val accent = pacMazeThemeAccent(zoneChapter.themeId)
        val left = with(density) {
            zoneStart * (metrics.columnWidth + metrics.hGap).toPx() - metrics.hGap.toPx() * 0.5f
        }.coerceAtLeast(0f)
        val right = with(density) {
            endIndex * (metrics.columnWidth + metrics.hGap).toPx() - metrics.hGap.toPx() * 0.5f
        }
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.06f),
                    accent.copy(alpha = 0.12f),
                    accent.copy(alpha = 0.06f),
                ),
                startX = left,
                endX = right,
            ),
            topLeft = Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(
                width = (right - left).coerceAtLeast(0f),
                height = size.height,
            ),
        )
    }
    levels.forEachIndexed { index, level ->
        val chapter = PacMazeChapterCatalog.chapterForLevel(level.id)
        if (chapter != zoneChapter) {
            flushZone(index)
            zoneStart = index
            zoneChapter = chapter
        }
    }
    flushZone(levels.size)
}

@Composable
private fun rememberSerpentineMetrics(
    levelCount: Int,
    layout: PacMazeHubLayoutSpec,
    showChapterZones: Boolean = false,
): SerpentineMetrics {
    val dense = levelCount > 12
    return androidx.compose.runtime.remember(
        levelCount,
        layout.scale,
        layout.isCompactHeight,
        layout.isVeryCompactHeight,
        showChapterZones,
    ) {
        SerpentineMetrics(
            nodeSize = layout.dp(
                when {
                    layout.isVeryCompactHeight -> 32.dp
                    dense -> 36.dp
                    layout.isCompactHeight -> 40.dp
                    else -> 46.dp
                },
            ),
            columnWidth = layout.dp(
                when {
                    layout.isVeryCompactHeight -> 48.dp
                    dense -> 54.dp
                    layout.isCompactHeight -> 58.dp
                    else -> 66.dp
                },
            ),
            hGap = layout.dp(if (dense || layout.isCompactHeight) 3.dp else 4.dp),
            tagRowHeight = layout.dp(
                when {
                    showChapterZones && layout.isCompactHeight -> 24.dp
                    showChapterZones -> 28.dp
                    else -> 12.dp
                },
            ),
            orbBoxHeight = layout.dp(
                when {
                    layout.isVeryCompactHeight -> 38.dp
                    dense -> 44.dp
                    else -> 50.dp
                },
            ),
            zigzagLowTop = layout.dp(if (layout.isCompactHeight) 1.dp else 6.dp),
            zigzagHighTop = layout.dp(
                when {
                    layout.isVeryCompactHeight -> 18.dp
                    layout.isCompactHeight -> 24.dp
                    else -> 34.dp
                },
            ),
        )
    }
}

@Composable
private fun PacMazePathTag(text: String, color: Color, layout: PacMazeHubLayoutSpec) {
    Text(
        text,
        color = color,
        fontSize = layout.captionSp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .height(layout.dp(12.dp))
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = layout.dp(6.dp)),
    )
}

@Composable
private fun PacMazeOrbNode(
    level: PacMazeLevelMeta,
    unlocked: Boolean,
    highlighted: Boolean,
    themeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val ringColor = when {
        highlighted -> PacMazePalette.accentGold
        unlocked -> themeColor
        else -> PacMazePalette.locked
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .pacMazeClickable(sound = PacMazeUiSoundId.MapNode, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (unlocked) {
                        Brush.radialGradient(
                            listOf(ringColor.copy(alpha = 0.35f), Color(0xFF1A2236)),
                        )
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF2A3144), Color(0xFF151B28)))
                    },
                )
                .border(
                    width = if (highlighted) 3.dp else 2.dp,
                    color = ringColor.copy(alpha = if (unlocked) 0.9f else 0.45f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                Text(
                    level.id.toString(),
                    color = Color.White,
                    fontSize = if (size < 50.dp) 14.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                )
            } else {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PacMazePalette.inkHint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSerpentineConnections(
    centers: List<Offset>,
    maxLevelReached: Int,
    unlockAll: Boolean,
) {
    if (centers.size < 2) return
    for (i in 0 until centers.lastIndex) {
        val fromLevel = i + 1
        val toLevel = i + 2
        val segmentUnlocked = unlockAll || toLevel <= maxLevelReached
        val start = centers[i]
        val end = centers[i + 1]
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                x1 = (start.x + end.x) / 2f,
                y1 = start.y,
                x2 = (start.x + end.x) / 2f,
                y2 = end.y,
                x3 = end.x,
                y3 = end.y,
            )
        }
        if (segmentUnlocked) {
            drawPath(
                path = path,
                color = PacMazePalette.accentMint.copy(alpha = 0.12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f, cap = StrokeCap.Round),
            )
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        PacMazePalette.accentMint.copy(alpha = 0.55f),
                        PacMazePalette.accentGold.copy(alpha = 0.85f),
                    ),
                    start = start,
                    end = end,
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round),
            )
            drawPelletsAlongCubic(start, end, active = true)
        } else {
            drawPath(
                path = path,
                color = PacMazePalette.locked.copy(alpha = 0.35f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
                ),
            )
        }
        drawCircle(
            color = if (segmentUnlocked) {
                PacMazePalette.accentGold.copy(alpha = 0.35f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
            radius = if (fromLevel <= maxLevelReached || unlockAll) 6f else 4f,
            center = start,
        )
    }
    val lastUnlocked = unlockAll || centers.size <= maxLevelReached
    drawCircle(
        color = if (lastUnlocked) PacMazePalette.accentGold.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
        radius = 6f,
        center = centers.last(),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPelletsAlongCubic(
    start: Offset,
    end: Offset,
    active: Boolean,
) {
    val cx1 = (start.x + end.x) / 2f
    val cy1 = start.y
    val cx2 = (start.x + end.x) / 2f
    val cy2 = end.y
    val steps = 12
    for (step in 0..steps) {
        val t = step / steps.toFloat()
        val omt = 1f - t
        val x = omt * omt * omt * start.x +
            3f * omt * omt * t * cx1 +
            3f * omt * t * t * cx2 +
            t * t * t * end.x
        val y = omt * omt * omt * start.y +
            3f * omt * omt * t * cy1 +
            3f * omt * t * t * cy2 +
            t * t * t * end.y
        drawCircle(
            color = if (active && step % 2 == 0) {
                PacMazePalette.accentGold.copy(alpha = 0.65f)
            } else {
                Color.White.copy(alpha = 0.2f)
            },
            radius = if (step % 2 == 0) 3.5f else 2f,
            center = Offset(x, y),
        )
    }
}

@Composable
fun PacMazeHeroBadge(
    modifier: Modifier = Modifier,
    characterId: PacMazeCharacterId = PacMazeCharacterId.CLASSIC_PAC,
) {
    PacMazeCharacterPreview(
        characterId = characterId,
        modifier = modifier,
        animateWalk = true,
        selected = true,
    )
}

@Composable
fun PacMazeStarRow(stars: Int, maxStars: Int = 3, starSize: TextUnit = 14.sp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(maxStars) { index ->
            Text(
                if (index < stars) "★" else "☆",
                color = if (index < stars) PacMazePalette.starFilled else PacMazePalette.starEmpty.copy(alpha = 0.75f),
                fontSize = starSize,
                fontWeight = if (index < stars) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
