package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextAlign
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
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreview
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import androidx.compose.ui.unit.sp

/** L1 · 全景闯关径：主区 S 形路径 + 侧栏继续/角色 + 底栏章节快捷入口 */
@Composable
fun PacMazeChapterOverviewPanel(
    maxLevelReached: Int,
    starsBitmask: Int,
    continueLevelId: Int,
    selectedSkinId: PacMazeSkinId,
    enabled: Boolean,
    onContinue: () -> Unit,
    onPractice: () -> Unit,
    onOpenChapter: (PacMazeCampaignChapter) -> Unit,
    onSelectLevel: (Int) -> Unit,
    onChangeCharacter: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val continueMeta = PacMazeLevelCatalog.find(continueLevelId)
    val bottomRailReserve = layout.dp(if (layout.tier == PacMazeHubLayoutTier.Compact) 48.dp else 58.dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = layout.contentAreaHeight - bottomRailReserve),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeCampaignSidebar(
                maxLevelReached = maxLevelReached,
                totalLevels = PacMazeLevelCatalog.TOTAL_LEVELS,
                continueLevelId = continueLevelId,
                continueLevelName = continueMeta?.name ?: "",
                continueLevelSubtitle = continueMeta?.subtitle ?: "",
                selectedSkinId = selectedSkinId,
                enabled = enabled,
                layout = layout,
                onContinue = onContinue,
                onPractice = onPractice,
                onChangeCharacter = onChangeCharacter,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(layout.panelRadius))
                    .background(Color(0xFF121828))
                    .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius)),
            ) {
                PacMazeSerpentineMapPanel(
                    maxLevelReached = maxLevelReached,
                    starsBitmask = starsBitmask,
                    continueLevelId = continueLevelId,
                    isLoading = !enabled,
                    onSelectLevel = onSelectLevel,
                    showChapterZones = true,
                    showQuickSelect = false,
                    unlockAll = PacMazeTestUnlock.enabled,
                )
            }
        }

        PacMazeChapterQuickRail(
            maxLevelReached = maxLevelReached,
            starsBitmask = starsBitmask,
            onOpenChapter = onOpenChapter,
            onOpenCollection = onOpenCollection,
        )
    }
}

@Composable
private fun PacMazeChapterQuickRail(
    maxLevelReached: Int,
    starsBitmask: Int,
    onOpenChapter: (PacMazeCampaignChapter) -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()
    val scroll = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(layout.dp(if (layout.isCompactHeight) 2.dp else 4.dp)),
    ) {
        if (!layout.isCompactHeight) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "按章节浏览",
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "点章节查看关卡列表",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PacMazeCampaignChapter.entries.forEach { chapter ->
                val progress = PacMazeChapterCatalog.chapterProgress(chapter, maxLevelReached, starsBitmask)
                PacMazeChapterQuickChip(
                    progress = progress,
                    onClick = { onOpenChapter(chapter) },
                )
            }
            PacMazeHubTextChip(
                text = "收藏册",
                accent = PacMazePalette.accentPurple,
                onClick = onOpenCollection,
                sound = PacMazeUiSoundId.ChipAction,
                modifier = Modifier.widthIn(min = layout.dp(72.dp)),
            )
        }
    }
}

@Composable
private fun PacMazeChapterQuickChip(
    progress: ChapterProgress,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val accent = pacMazeThemeAccent(progress.chapter.themeId)
    val emoji = pacMazeThemeEmoji(progress.chapter.themeId)
    val active = progress.currentLevelId != null && progress.clearedCount < progress.totalCount
    val shape = RoundedCornerShape(layout.dp(10.dp))

    Column(
        modifier = Modifier
            .widthIn(min = layout.dp(if (layout.isCompactHeight) 80.dp else 88.dp))
            .clip(shape)
            .background(
                if (active) accent.copy(alpha = 0.16f) else Color(0xFF151D30),
            )
            .border(
                1.dp,
                if (active) accent.copy(alpha = 0.55f) else PacMazePalette.cardBorder,
                shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.MapChip, onClick = onClick)
            .padding(
                horizontal = layout.dp(8.dp),
                vertical = layout.dp(if (layout.isCompactHeight) 4.dp else 6.dp),
            ),
        verticalArrangement = Arrangement.spacedBy(layout.dp(2.dp)),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = layout.bodySp)
            Text(
                progress.chapter.displayName,
                color = accent,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${progress.clearedCount}/${progress.totalCount}",
                color = PacMazePalette.inkPrimary,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "★${progress.totalStars}",
                color = PacMazePalette.accentGold,
                fontSize = layout.captionSp,
            )
        }
    }
}

/** L2 · 章节内关卡列表 */
@Composable
fun PacMazeChapterLevelListPanel(
    themeId: PacMazeMapThemeId,
    maxLevelReached: Int,
    starsBitmask: Int,
    onSelectLevel: (Int) -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val chapter = PacMazeChapterCatalog.chapters.first { it.themeId == themeId }
    val levels = PacMazeChapterCatalog.levelsInChapter(chapter)
    val accent = pacMazeThemeAccent(themeId)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeSectionHeader(
            title = "${pacMazeThemeEmoji(themeId)} ${chapter.displayName}",
            subtitle = chapter.tagline,
            accentColor = accent,
            layout = layout,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            items(levels, key = { it }) { levelId ->
                val meta = PacMazeLevelCatalog.find(levelId) ?: return@items
                PacMazeChapterLevelRow(
                    levelId = levelId,
                    meta = meta,
                    unlocked = PacMazeTestUnlock.isLevelUnlocked(levelId, maxLevelReached),
                    stars = if (PacMazeTestUnlock.isLevelUnlocked(levelId, maxLevelReached)) decodePacMazeStars(starsBitmask, levelId) else 0,
                    isCurrent = levelId == maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS),
                    unlockHint = PacMazeLevelDetailInfo.unlockHint(levelId, maxLevelReached),
                    onClick = { onSelectLevel(levelId) },
                )
            }
        }
    }
}

@Composable
private fun PacMazeChapterLevelRow(
    levelId: Int,
    meta: PacMazeLevelMeta,
    unlocked: Boolean,
    stars: Int,
    isCurrent: Boolean,
    unlockHint: String?,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val diffColor = PacMazeLevelCatalog.difficultyColor(meta.difficulty)
    val accent = if (isCurrent) PacMazePalette.accentGold else PacMazePalette.inkPrimary
    val shape = RoundedCornerShape(layout.cardRadius)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) Color(0xFF2A2410) else Color(0xFF151D30))
            .border(
                1.dp,
                if (isCurrent) PacMazePalette.accentGold.copy(alpha = 0.55f) else PacMazePalette.cardBorder,
                shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(layout.cardPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(10.dp)),
    ) {
        Box(
            modifier = Modifier
                .size(layout.dp(42.dp))
                .clip(CircleShape)
                .background(diffColor.copy(alpha = if (unlocked) 0.25f else 0.1f))
                .border(1.dp, diffColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                Text(
                    "$levelId",
                    color = accent,
                    fontSize = layout.subtitleSp,
                    fontWeight = FontWeight.Black,
                )
            } else {
                Icon(Icons.Filled.Lock, null, tint = PacMazePalette.locked, modifier = Modifier.size(layout.dp(18.dp)))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                meta.name,
                color = if (unlocked) PacMazePalette.inkPrimary else PacMazePalette.inkMuted,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                meta.subtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = layout.captionSp,
                maxLines = 2,
            )
            if (unlockHint != null) {
                Text(unlockHint, color = PacMazePalette.locked, fontSize = layout.captionSp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    meta.mechanisms.forEach { kind ->
                        Text("${kind.glyph} ${kind.label}", color = PacMazePalette.inkHint, fontSize = layout.captionSp)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(meta.difficulty, color = diffColor, fontSize = layout.captionSp, fontWeight = FontWeight.SemiBold)
            if (unlocked) PacMazeStarRow(stars = stars, maxStars = 3, starSize = (12f * layout.scale).sp)
        }
    }
}

/** L3 · 关卡详情：左地图预览坞 + 右双栏信息 + 底栏操作，横屏高密度布局。 */
@Composable
fun PacMazeLevelDetailPanel(
    levelId: Int,
    maxLevelReached: Int,
    starsBitmask: Int,
    onStart: () -> Unit,
    onPractice: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val meta = PacMazeLevelCatalog.find(levelId) ?: return
    val theme = PacMazeThemeRegistry.themeForLevel(levelId)
    val accent = pacMazeThemeAccent(theme)
    val diffColor = PacMazeLevelCatalog.difficultyColor(meta.difficulty)
    val unlocked = PacMazeTestUnlock.isLevelUnlocked(levelId, maxLevelReached)
    val stars = if (unlocked) decodePacMazeStars(starsBitmask, levelId) else 0
    val starGoals = PacMazeLevelDetailInfo.starGoals(levelId)
    val tutorial = meta.tutorialHint.ifBlank { tutorialHintFor(levelId) }
    val unlockHint = PacMazeLevelDetailInfo.unlockHint(levelId, maxLevelReached)
    val ghostRoster = PacMazeLevelGhostsUi.rosterForLevel(levelId)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeLevelDetailPreviewDock(
            levelId = levelId,
            meta = meta,
            theme = theme,
            accent = accent,
            diffColor = diffColor,
            unlocked = unlocked,
            stars = stars,
            onStart = onStart,
            modifier = Modifier
                .widthIn(min = layout.dp(148.dp), max = layout.dp(220.dp))
                .fillMaxHeight()
                .weight(0.38f, fill = false),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                PacMazeLevelDetailHeader(
                    levelId = levelId,
                    meta = meta,
                    theme = theme,
                    accent = accent,
                    diffColor = diffColor,
                    unlocked = unlocked,
                    stars = stars,
                    layout = layout,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                ) {
                    ghostRoster.forEach { spawn ->
                        PacMazeLevelGhostChip(
                            kind = spawn.kind,
                            specialty = spawn.specialty,
                            modifier = Modifier.weight(1f),
                            layout = layout,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.gap),
                ) {
                    PacMazeLevelDetailInfoCard(
                        title = "本关机关",
                        accent = accent,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (meta.mechanisms.isEmpty()) {
                            PacMazeLevelDetailEmptyHint("基础吃豆 · 无特殊机关")
                        } else {
                            meta.mechanisms.forEach { kind ->
                                PacMazeMechanismChip(kind = kind, accent = accent, layout = layout)
                            }
                        }
                    }

                    PacMazeLevelDetailInfoCard(
                        title = "三星条件",
                        accent = PacMazePalette.accentGold,
                        modifier = Modifier.weight(1f),
                    ) {
                        starGoals.forEach { goal ->
                            PacMazeStarGoalLine(
                                goal = goal,
                                achieved = unlocked && stars >= goal.stars,
                                layout = layout,
                            )
                        }
                    }
                }

                if (tutorial.isNotBlank()) {
                    PacMazeLevelDetailInfoCard(
                        title = "攻略提示",
                        accent = PacMazePalette.accentCyan,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true,
                    ) {
                        Text(
                            tutorial,
                            color = PacMazePalette.inkSecondary,
                            fontSize = layout.bodySp,
                            lineHeight = (layout.bodySp.value * 1.35f).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(layout.dp(4.dp)))
            PacMazeLevelDetailActionBar(
                unlocked = unlocked,
                unlockHint = unlockHint,
                onStart = onStart,
                onPractice = onPractice,
                layout = layout,
            )
        }
    }
}

@Composable
private fun PacMazeLevelDetailPreviewDock(
    levelId: Int,
    meta: PacMazeLevelMeta,
    theme: PacMazeMapThemeId,
    accent: Color,
    diffColor: Color,
    unlocked: Boolean,
    stars: Int,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()
    val shape = RoundedCornerShape(layout.panelRadius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF121828))
            .border(1.dp, PacMazePalette.cardBorderStrong, shape)
            .padding(layout.dp(6.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        PacMazeLevelMapPreview(
            levelId = levelId,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Column(verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                Text(pacMazeThemeEmoji(theme), fontSize = layout.subtitleSp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "第 $levelId 关",
                        color = PacMazePalette.inkMuted,
                        fontSize = layout.captionSp,
                    )
                    Text(
                        meta.name,
                        color = accent,
                        fontSize = layout.subtitleSp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (unlocked) {
                    PacMazeStarRow(stars = stars, maxStars = 3, starSize = (13f * layout.scale).sp)
                } else {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = PacMazePalette.locked,
                        modifier = Modifier.size(layout.dp(18.dp)),
                    )
                }
            }

            Text(
                meta.subtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = layout.captionSp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                PacMazeLevelDetailTag(meta.difficulty, diffColor, layout, Modifier.weight(1f))
                PacMazeLevelDetailTag(theme.displayName, accent, layout, Modifier.weight(1f))
            }

            if (unlocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(layout.dp(10.dp)))
                        .background(PacMazePalette.ctaGradient)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(layout.dp(10.dp)))
                        .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, onClick = onStart)
                        .padding(vertical = layout.dp(10.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(layout.dp(18.dp)))
                    Spacer(modifier = Modifier.width(layout.dp(6.dp)))
                    Text("开始游戏", color = Color.White, fontSize = layout.subtitleSp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PacMazeLevelDetailHeader(
    levelId: Int,
    meta: PacMazeLevelMeta,
    theme: PacMazeMapThemeId,
    accent: Color,
    diffColor: Color,
    unlocked: Boolean,
    stars: Int,
    layout: PacMazeHubLayoutSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.18f), Color(0xFF151D30)),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.cardPad, vertical = layout.dp(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${pacMazeThemeEmoji(theme)} ${meta.name}",
                color = accent,
                fontSize = layout.subtitleSp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta.subtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = layout.captionSp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "第 $levelId 关",
                color = PacMazePalette.inkMuted,
                fontSize = layout.captionSp,
            )
            if (unlocked) {
                PacMazeStarRow(stars = stars, maxStars = 3, starSize = (14f * layout.scale).sp)
            }
        }
        PacMazeLevelDetailTag(meta.difficulty, diffColor, layout)
    }
}

@Composable
private fun PacMazeLevelDetailTag(
    text: String,
    accent: Color,
    layout: PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(layout.dp(8.dp)))
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = accent,
            fontSize = layout.captionSp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PacMazeLevelDetailInfoCard(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(layout.dp(12.dp)))
            .background(Color(0xFF151D30))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(12.dp)))
            .padding(if (compact) layout.dp(8.dp) else layout.cardPad),
        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        Text(title, color = accent, fontSize = layout.subtitleSp, fontWeight = FontWeight.Bold)
        Column(
            modifier = if (compact) Modifier else Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(layout.dp(5.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun PacMazeLevelDetailEmptyHint(text: String) {
    val layout = currentPacMazeHubLayout()
    Text(
        text,
        color = PacMazePalette.inkSecondary,
        fontSize = layout.bodySp,
        lineHeight = (layout.bodySp.value * 1.35f).sp,
    )
}

@Composable
private fun PacMazeMechanismChip(
    kind: PacMazeMechanismKind,
    accent: Color,
    layout: PacMazeHubLayoutSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(layout.dp(8.dp)))
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp)),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(kind.glyph, color = accent, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
        Text(
            kind.label,
            color = PacMazePalette.inkPrimary,
            fontSize = layout.bodySp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PacMazeStarGoalLine(
    goal: PacMazeLevelDetailInfo.StarGoalLine,
    achieved: Boolean,
    layout: PacMazeHubLayoutSpec,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.Top,
    ) {
        PacMazeStarRow(
            stars = goal.stars.coerceAtMost(3),
            maxStars = 3,
            starSize = (11f * layout.scale).sp,
        )
        Text(
            goal.text,
            color = if (achieved) PacMazePalette.inkPrimary else PacMazePalette.inkSecondary,
            fontSize = layout.bodySp,
            lineHeight = (layout.bodySp.value * 1.3f).sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PacMazeLevelDetailActionBar(
    unlocked: Boolean,
    unlockHint: String?,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    layout: PacMazeHubLayoutSpec,
) {
    if (!unlocked) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(12.dp)))
                .background(Color(0xFF2A1A1A))
                .border(1.dp, PacMazePalette.locked.copy(alpha = 0.45f), RoundedCornerShape(layout.dp(12.dp)))
                .padding(horizontal = layout.cardPad, vertical = layout.dp(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
        ) {
            Icon(Icons.Filled.Lock, null, tint = PacMazePalette.locked, modifier = Modifier.size(layout.dp(20.dp)))
            Text(
                unlockHint ?: "尚未解锁",
                color = PacMazePalette.locked,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1.35f)
                .clip(RoundedCornerShape(layout.dp(12.dp)))
                .background(PacMazePalette.ctaGradient)
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
                .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, onClick = onStart)
                .padding(horizontal = layout.dp(12.dp), vertical = layout.dp(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(32.dp))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(layout.dp(18.dp)))
            }
            Column {
                Text("开始闯关", color = Color.White, fontSize = layout.subtitleSp, fontWeight = FontWeight.Bold)
                Text("正式计入进度与星级", color = Color.White.copy(alpha = 0.82f), fontSize = layout.captionSp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(layout.dp(12.dp)))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(12.dp)))
                .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, onClick = onPractice)
                .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("练习本关", color = PacMazePalette.inkPrimary, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                Text("无限命 · 不计星", color = PacMazePalette.inkHint, fontSize = layout.captionSp)
            }
        }
    }
}

@Composable
fun PacMazeHubTextChip(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    sound: PacMazeUiSoundId,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val layout = currentPacMazeHubLayout()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(if (compact) 8.dp else 10.dp)))
            .background(accent.copy(alpha = 0.12f))
            .border(
                1.dp,
                accent.copy(alpha = 0.4f),
                RoundedCornerShape(layout.dp(if (compact) 8.dp else 10.dp)),
            )
            .pacMazeClickable(sound = sound, onClick = onClick)
            .padding(
                horizontal = layout.dp(if (compact) 8.dp else 12.dp),
                vertical = layout.dp(if (compact) 4.dp else 10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = accent,
            fontSize = if (compact) layout.captionSp else layout.bodySp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
