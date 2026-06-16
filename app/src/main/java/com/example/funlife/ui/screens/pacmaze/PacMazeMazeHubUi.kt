package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.PacMazeMazeStats
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreview
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

@Composable
fun PacMazeMazeHubPanel(
    mazeStats: PacMazeMazeStats,
    mazeBestTimeMs: Long,
    selectedSkinId: PacMazeSkinId,
    selectedDifficulty: PacMazeMazeDifficulty,
    selectedContract: PacMazeMazeContract,
    useDailyChallenge: Boolean,
    enabled: Boolean,
    onSelectDifficulty: (PacMazeMazeDifficulty) -> Unit,
    onSelectContract: (PacMazeMazeContract) -> Unit,
    onToggleDaily: (Boolean) -> Unit,
    onStart: () -> Unit,
    onChangeCharacter: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val dailyLabel = PacMazeMazeRunOptions.dailyLabel()
    val dailySeed = PacMazeMazeRunOptions.dailySeed()
    val dailyBest = if (mazeStats.dailyDate == PacMazeMazeRunOptions.todayDateString()) {
        mazeStats.dailyBestTimeMs
    } else {
        0L
    }
    val baseOptions = remember(selectedDifficulty, selectedContract) {
        PacMazeMazeRunOptions(
            seed = dailySeed,
            difficulty = selectedDifficulty,
            contract = selectedContract,
            dailyChallenge = true,
        )
    }
    val dailyPreviewOptions = remember(baseOptions) {
        baseOptions.copy(seed = dailySeed, dailyChallenge = true)
    }
    val randomSampleSeed = remember(selectedDifficulty, selectedContract) {
        dailySeed xor 0x5EED_CAFE0L
    }
    val randomSampleOptions = remember(baseOptions, randomSampleSeed) {
        baseOptions.copy(seed = randomSampleSeed, dailyChallenge = false)
    }
    val activePreviewOptions = if (useDailyChallenge) dailyPreviewOptions else randomSampleOptions
    val configScroll = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeMazeHubSidebar(
            activePreviewOptions = activePreviewOptions,
            mazeStats = mazeStats,
            selectedDifficulty = selectedDifficulty,
            selectedContract = selectedContract,
            useDailyChallenge = useDailyChallenge,
            dailySeed = dailySeed,
            selectedSkinId = selectedSkinId,
            enabled = enabled,
            layout = layout,
            onStart = onStart,
            onChangeCharacter = onChangeCharacter,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        ) {
            PacMazeMazeSeedModeSection(
                dailyLabel = dailyLabel,
                dailySeed = dailySeed,
                useDailyChallenge = useDailyChallenge,
                dailyBestMs = dailyBest,
                globalBestMs = mazeBestTimeMs,
                dailyPreviewOptions = dailyPreviewOptions,
                randomSampleOptions = randomSampleOptions,
                layout = layout,
                onSelectDaily = { onToggleDaily(true) },
                onSelectRandom = { onToggleDaily(false) },
            )

            PacMazeMazePillarStrip(layout = layout)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(layout.panelRadius))
                    .background(Color(0xFF121828))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.panelRadius))
                    .verticalScroll(configScroll)
                    .padding(layout.cardPad),
                verticalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
            ) {
                Text(
                    "本局规则 · 三系玩法同时生效",
                    color = PacMazePalette.accentGold,
                    fontSize = layout.subtitleSp,
                    fontWeight = FontWeight.Black,
                )
                PacMazeMazeActiveRulesRow(options = activePreviewOptions, layout = layout)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.gap),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                    ) {
                        Text("难度赛道", color = PacMazePalette.accentCyan, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                        Text(
                            "三条独立赛道 · 尺寸/鬼数不同",
                            color = PacMazePalette.inkHint,
                            fontSize = layout.captionSp,
                        )
                        PacMazeMazeDifficulty.entries.forEach { diff ->
                            val best = mazeStats.bestTimeByDifficulty[diff.id] ?: 0L
                            val stars = mazeStats.bestStarsByDifficulty[diff.id] ?: 0
                            PacMazeMazeOptionChip(
                                title = diff.displayName,
                                subtitle = buildDifficultySubtitle(diff, selectedContract),
                                meta = if (best > 0L) "最佳 ${pacMazeFormatBestTime(best)} · ★$stars" else "尚未挑战",
                                selected = selectedDifficulty == diff,
                                accent = difficultyAccent(diff),
                                onClick = { onSelectDifficulty(diff) },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                    ) {
                        Text("开局契约", color = PacMazePalette.accentPurple, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                        Text(
                            "修饰规则 · 冲击三星与排行榜",
                            color = PacMazePalette.inkHint,
                            fontSize = layout.captionSp,
                        )
                        PacMazeMazeContract.entries.forEach { contract ->
                            PacMazeMazeOptionChip(
                                title = contract.displayName,
                                subtitle = contract.tagline,
                                meta = null,
                                selected = selectedContract == contract,
                                accent = PacMazePalette.accentPurple,
                                onClick = { onSelectContract(contract) },
                            )
                        }
                    }
                }
            }

            PacMazeMazeStartBar(
                selectedDifficulty = selectedDifficulty,
                useDailyChallenge = useDailyChallenge,
                dailySeed = dailySeed,
                enabled = enabled,
                layout = layout,
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun PacMazeMazeSeedModeSection(
    dailyLabel: String,
    dailySeed: Long,
    useDailyChallenge: Boolean,
    dailyBestMs: Long,
    globalBestMs: Long,
    dailyPreviewOptions: PacMazeMazeRunOptions,
    randomSampleOptions: PacMazeMazeRunOptions,
    layout: PacMazeHubLayoutSpec,
    onSelectDaily: () -> Unit,
    onSelectRandom: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.panelRadius))
            .background(Color(0xFF10182A))
            .border(1.5.dp, PacMazePalette.modeMaze.copy(alpha = 0.45f), RoundedCornerShape(layout.panelRadius))
            .padding(layout.cardPad),
        verticalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("地图来源", color = PacMazePalette.accentGold, fontSize = layout.subtitleSp, fontWeight = FontWeight.Black)
                Text("程序生成 · 选一种开局方式", color = PacMazePalette.inkHint, fontSize = layout.captionSp)
            }
            Text("∞ 无限变体", color = PacMazePalette.accentCyan, fontSize = layout.captionSp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeMazeSeedModeCard(
                icon = "📅",
                title = "每日挑战",
                headline = "今日全员同一张图",
                bullets = listOf(
                    dailyLabel,
                    "Seed ${formatMazeSeedCode(dailySeed)}",
                    "今日最佳 ${if (dailyBestMs > 0L) pacMazeFormatBestTime(dailyBestMs) else "—"}",
                ),
                accent = PacMazePalette.accentGold,
                selected = useDailyChallenge,
                layout = layout,
                modifier = Modifier.weight(1f),
                onClick = onSelectDaily,
            )
            PacMazeMazeSeedModeCard(
                icon = "🎲",
                title = "自由随机",
                headline = "每局不同迷宫",
                bullets = listOf(
                    "开局自动生成新种子",
                    "可无限重开刷图",
                    "历史最佳 ${if (globalBestMs > 0L) pacMazeFormatBestTime(globalBestMs) else "—"}",
                ),
                accent = PacMazePalette.accentCyan,
                selected = !useDailyChallenge,
                layout = layout,
                modifier = Modifier.weight(1f),
                onClick = onSelectRandom,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
            Text("对比预览（同难度 · 不同种子）", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(layout.gap),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
                ) {
                    PacMazeMazeRunPreview(
                        options = dailyPreviewOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(layout.dp(if (layout.isCompactHeight) 72.dp else 88.dp)),
                        badge = "今日固定",
                        badgeAccent = PacMazePalette.accentGold,
                        dimmed = !useDailyChallenge,
                        showFogHint = true,
                    )
                    Text(
                        "🔒 换难度会变，但今日种子不变",
                        color = if (useDailyChallenge) PacMazePalette.accentGold else PacMazePalette.inkHint,
                        fontSize = 9.sp,
                        maxLines = 2,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
                ) {
                    PacMazeMazeRunPreview(
                        options = randomSampleOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(layout.dp(if (layout.isCompactHeight) 72.dp else 88.dp)),
                        badge = "下局会变",
                        badgeAccent = PacMazePalette.accentCyan,
                        dimmed = useDailyChallenge,
                        showFogHint = true,
                    )
                    Text(
                        "🔀 示例随机图 · 每开一局布局都不同",
                        color = if (!useDailyChallenge) PacMazePalette.accentCyan else PacMazePalette.inkHint,
                        fontSize = 9.sp,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeMazeSeedModeCard(
    icon: String,
    title: String,
    headline: String,
    bullets: List<String>,
    accent: Color,
    selected: Boolean,
    layout: PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(12.dp)))
            .background(
                if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.75f) else PacMazePalette.cardBorder,
                shape = RoundedCornerShape(layout.dp(12.dp)),
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(layout.dp(8.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
        ) {
            Text(icon, fontSize = layout.bodySp)
            Text(title, color = if (selected) accent else PacMazePalette.inkPrimary, fontSize = layout.bodySp, fontWeight = FontWeight.Black)
            if (selected) {
                Text("✓", color = accent, fontSize = layout.bodySp, fontWeight = FontWeight.Black)
            }
        }
        Text(headline, color = if (selected) Color.White else PacMazePalette.inkSecondary, fontSize = layout.captionSp, fontWeight = FontWeight.SemiBold)
        bullets.forEach { line ->
            Text("· $line", color = PacMazePalette.inkHint, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable
private fun PacMazeMazeHubSidebar(
    activePreviewOptions: PacMazeMazeRunOptions,
    mazeStats: PacMazeMazeStats,
    selectedDifficulty: PacMazeMazeDifficulty,
    selectedContract: PacMazeMazeContract,
    useDailyChallenge: Boolean,
    dailySeed: Long,
    selectedSkinId: PacMazeSkinId,
    enabled: Boolean,
    layout: PacMazeHubLayoutSpec,
    onStart: () -> Unit,
    onChangeCharacter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(layout.dp(if (layout.isCompactHeight) 132.dp else 152.dp))
            .fillMaxHeight()
            .clip(RoundedCornerShape(layout.panelRadius))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2838), Color(0xFF141C2A))))
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
            .padding(layout.dp(8.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        PacMazeMazeSeedModeBadge(
            useDailyChallenge = useDailyChallenge,
            dailySeed = dailySeed,
            layout = layout,
        )

        PacMazeMazeRunPreview(
            options = activePreviewOptions,
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.dp(if (layout.isCompactHeight) 100.dp else 118.dp)),
            badge = if (useDailyChallenge) "已选·每日" else "已选·随机",
            badgeAccent = if (useDailyChallenge) PacMazePalette.accentGold else PacMazePalette.accentCyan,
        )

        if (useDailyChallenge) {
            Text(
                "今日 00:00 后换新图",
                color = PacMazePalette.inkHint,
                fontSize = 8.sp,
            )
        } else {
            Text(
                "本局种子开局时生成",
                color = PacMazePalette.inkHint,
                fontSize = 8.sp,
            )
        }

        PacMazeMazePreviewLegend(layout = layout)

        PacMazeMazeDifficultyPath(
            selected = selectedDifficulty,
            stats = mazeStats.bestTimeByDifficulty,
            layout = layout,
        )

        Spacer(modifier = Modifier.weight(1f))

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
                modifier = Modifier.size(layout.dp(34.dp)),
                animateWalk = false,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("出战角色", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
                Text(
                    selectedSkinId.displayName,
                    color = PacMazePalette.inkPrimary,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text("更换", color = PacMazePalette.accentCyan, fontSize = layout.captionSp, fontWeight = FontWeight.Bold)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(layout.dp(12.dp)))
                .background(PacMazePalette.ctaGradient)
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
                .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, enabled = enabled, onClick = onStart)
                .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(30.dp))
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(layout.dp(18.dp)))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("进入迷宫", color = Color.White, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                Text(
                    buildString {
                        append(selectedDifficulty.displayName)
                        append(" · ")
                        append(if (useDailyChallenge) "每日固定" else "自由随机")
                        if (selectedContract != PacMazeMazeContract.NONE) append(" · ${selectedContract.displayName}")
                    },
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = layout.captionSp,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun PacMazeMazeSeedModeBadge(
    useDailyChallenge: Boolean,
    dailySeed: Long,
    layout: PacMazeHubLayoutSpec,
) {
    val accent = if (useDailyChallenge) PacMazePalette.accentGold else PacMazePalette.accentCyan
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(layout.dp(8.dp)))
            .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(5.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (useDailyChallenge) "📅 每日挑战模式" else "🎲 自由随机模式",
            color = accent,
            fontSize = layout.captionSp,
            fontWeight = FontWeight.Black,
        )
        Text(
            if (useDailyChallenge) {
                "Seed ${formatMazeSeedCode(dailySeed)} · 全天锁定"
            } else {
                "∞ 无限变体 · 每局新迷宫"
            },
            color = PacMazePalette.inkSecondary,
            fontSize = 9.sp,
            maxLines = 2,
        )
    }
}

@Composable
private fun PacMazeMazePreviewLegend(layout: PacMazeHubLayoutSpec) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        listOf("🟢你" to PacMazePalette.accentMint, "🟡钥" to PacMazePalette.accentGold, "🔴鬼" to Color(0xFFE57373), "🟠口" to PacMazePalette.accentOrange).forEach { (label, color) ->
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PacMazeMazeDifficultyPath(
    selected: PacMazeMazeDifficulty,
    stats: Map<String, Long>,
    layout: PacMazeHubLayoutSpec,
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp))) {
        Text("三条赛道", color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            PacMazeMazeDifficulty.entries.forEach { diff ->
                val accent = difficultyAccent(diff)
                val isSelected = diff == selected
                val best = stats[diff.id] ?: 0L
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) layout.dp(16.dp) else layout.dp(12.dp))
                            .clip(CircleShape)
                            .background(if (isSelected) accent else accent.copy(alpha = 0.35f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                    Text(
                        diff.displayName,
                        color = if (isSelected) accent else PacMazePalette.inkHint,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (best > 0L) {
                        Text(pacMazeFormatBestTime(best), color = PacMazePalette.inkHint, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PacMazeMazePillarStrip(layout: PacMazeHubLayoutSpec) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        PacMazeMazeFeatureCard(
            icon = "🌫",
            title = "探索",
            lines = listOf("迷雾", "线索豆", "雷达"),
            accent = PacMazePalette.accentCyan,
            modifier = Modifier.width(layout.dp(118.dp)),
            compact = true,
        )
        PacMazeMazeFeatureCard(
            icon = "⚡",
            title = "逃生",
            lines = listOf("动态墙", "多鬼", "限时"),
            accent = PacMazePalette.accentOrange,
            modifier = Modifier.width(layout.dp(118.dp)),
            compact = true,
        )
        PacMazeMazeFeatureCard(
            icon = "🏁",
            title = "速通",
            lines = listOf("每日图", "契约", "三星"),
            accent = PacMazePalette.accentGold,
            modifier = Modifier.width(layout.dp(118.dp)),
            compact = true,
        )
    }
}

@Composable
private fun PacMazeMazeActiveRulesRow(
    options: PacMazeMazeRunOptions,
    layout: PacMazeHubLayoutSpec,
) {
    val chips = listOf(
        "🌫 视野${options.effectiveFogRadius}格",
        "👻 ${options.effectiveGhostCount}鬼",
        "🗝 ${options.effectiveKeyCount}钥印",
        if (options.difficulty.dynamicWalls && options.contract != PacMazeMazeContract.SILENT) "⚡ 动态墙" else "🧱 静墙",
        "⏱ ${options.effectiveTimeLimitSeconds}s",
        "📡 雷达",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(5.dp)),
    ) {
        chips.forEach { chip ->
            Text(
                chip,
                color = PacMazePalette.inkPrimary,
                fontSize = layout.captionSp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PacMazePalette.modeMaze.copy(alpha = 0.12f))
                    .border(1.dp, PacMazePalette.modeMaze.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(4.dp)),
            )
        }
    }
}

@Composable
private fun PacMazeMazeStartBar(
    selectedDifficulty: PacMazeMazeDifficulty,
    useDailyChallenge: Boolean,
    dailySeed: Long,
    enabled: Boolean,
    layout: PacMazeHubLayoutSpec,
    onStart: () -> Unit,
) {
    val modeLine = if (useDailyChallenge) {
        "每日挑战 · 今日固定图 Seed ${formatMazeSeedCode(dailySeed)}"
    } else {
        "自由随机 · 本局生成全新迷宫"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(12.dp)))
            .background(PacMazePalette.ctaGradient)
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
            .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, enabled = enabled, onClick = onStart)
            .padding(horizontal = layout.dp(14.dp), vertical = layout.dp(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(10.dp)),
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(layout.dp(22.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Text("进入迷雾迷宫", color = Color.White, fontSize = layout.subtitleSp, fontWeight = FontWeight.Black)
            Text(
                "${selectedDifficulty.displayName} · $modeLine",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = layout.captionSp,
                maxLines = 2,
            )
        }
        Text("GO", color = Color.White, fontSize = layout.subtitleSp, fontWeight = FontWeight.Black)
    }
}

private fun buildDifficultySubtitle(
    diff: PacMazeMazeDifficulty,
    contract: PacMazeMazeContract,
): String {
    val size = when (contract) {
        PacMazeMazeContract.LABYRINTH -> (diff.mapSize + 4).coerceAtMost(23)
        else -> diff.mapSize
    }
    val ghosts = when (contract) {
        PacMazeMazeContract.SILENT -> 0
        else -> diff.ghostCount
    }
    val keys = when (contract) {
        PacMazeMazeContract.LABYRINTH -> (diff.keyCount + 1).coerceAtMost(4)
        else -> diff.keyCount
    }
    return "${size}×$size · ${diff.timeLimitSeconds}s · ${ghosts}鬼 · ${keys}钥"
}

private fun difficultyAccent(diff: PacMazeMazeDifficulty): Color = when (diff) {
    PacMazeMazeDifficulty.SCOUT -> PacMazePalette.accentMint
    PacMazeMazeDifficulty.STANDARD -> PacMazePalette.accentCyan
    PacMazeMazeDifficulty.ABYSS -> PacMazePalette.difficultyExtreme
}

@Composable
private fun PacMazeMazeFeatureCard(
    icon: String,
    title: String,
    lines: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(if (compact) 5.dp else 8.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(2.dp)),
    ) {
        Text("$icon $title", color = accent, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
        if (compact) {
            Text(lines.joinToString(" · "), color = PacMazePalette.inkSecondary, fontSize = layout.captionSp, maxLines = 1)
        } else {
            lines.forEach { line ->
                Text(line, color = PacMazePalette.inkSecondary, fontSize = layout.captionSp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PacMazeMazeOptionChip(
    title: String,
    subtitle: String,
    meta: String?,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(if (selected) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, color = if (selected) accent else PacMazePalette.inkPrimary, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = PacMazePalette.inkSecondary, fontSize = layout.captionSp, maxLines = 2)
        meta?.let {
            Text(it, color = PacMazePalette.accentGold, fontSize = layout.captionSp)
        }
    }
}

@Composable
fun PacMazeMazeHubTopStats(
    mazeStats: PacMazeMazeStats,
    mazeBestTimeMs: Long,
    selectedDifficulty: PacMazeMazeDifficulty,
    useDailyChallenge: Boolean,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    val dailyBest = if (mazeStats.dailyDate == PacMazeMazeRunOptions.todayDateString()) {
        mazeStats.dailyBestTimeMs
    } else {
        0L
    }
    val diffBest = mazeStats.bestTimeByDifficulty[selectedDifficulty.id] ?: 0L
    Row(
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PacMazeTopBarChip(
            "📅",
            if (useDailyChallenge && dailyBest > 0L) pacMazeFormatBestTime(dailyBest) else "—",
            "今日",
            PacMazePalette.accentGold,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        PacMazeTopBarChip(
            "🎯",
            if (diffBest > 0L) pacMazeFormatBestTime(diffBest) else "—",
            selectedDifficulty.displayName,
            PacMazePalette.modeMaze,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        PacMazeTopBarChip(
            "🏁",
            if (mazeBestTimeMs > 0L) pacMazeFormatBestTime(mazeBestTimeMs) else "—",
            "历史",
            PacMazePalette.accentCyan,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
    }
}

@Composable
fun PacMazeMazeRunBriefOverlay(
    options: PacMazeMazeRunOptions,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val layout = currentPacMazePlayLayout()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(12.dp)))
            .background(Color(0xE6101828))
            .border(1.dp, PacMazePalette.modeMaze.copy(alpha = 0.5f), RoundedCornerShape(layout.dp(12.dp)))
            .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onDismiss)
            .padding(horizontal = layout.dp(12.dp), vertical = layout.dp(8.dp)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
            Text("迷雾迷宫 · 本局规则", color = PacMazePalette.accentGold, fontSize = layout.statSp, fontWeight = FontWeight.Bold)
            Text(
                "① 战争迷雾探索  ② 收集${options.effectiveKeyCount}把钥印  ③ ${options.effectiveTimeLimitSeconds}秒内到达出口",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = (layout.statSp.value * 0.92f).sp,
                maxLines = 2,
            )
            Text(
                "右下角雷达探路 · 动态墙会开合 · 点击关闭",
                color = PacMazePalette.inkHint,
                fontSize = (layout.statSp.value * 0.85f).sp,
            )
        }
    }
}

@Composable
fun PacMazeRadarButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    cooldownSeconds: Int,
    onPulse: () -> Unit,
    compact: Boolean = false,
) {
    val size = if (compact) 64.dp else 76.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onPulse,
            enabled = enabled,
            modifier = Modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.5f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (enabled) {
                            listOf(Color(0xFF4FC3F7), Color(0xFF0277BD))
                        } else {
                            listOf(Color(0xFF334155), Color(0xFF1E293B))
                        },
                    ),
                )
                .border(2.dp, Color(0xFF81D4FA).copy(alpha = if (enabled) 0.85f else 0.35f), CircleShape),
        ) {
            Icon(Icons.Filled.Explore, contentDescription = "雷达", tint = Color.White, modifier = Modifier.size(if (compact) 28.dp else 32.dp))
        }
        Text(
            if (cooldownSeconds > 0) "${cooldownSeconds}s" else if (enabled) "雷达" else "扫描",
            color = PacMazePalette.inkMuted.copy(alpha = 0.75f),
            fontSize = 10.sp,
        )
    }
}
