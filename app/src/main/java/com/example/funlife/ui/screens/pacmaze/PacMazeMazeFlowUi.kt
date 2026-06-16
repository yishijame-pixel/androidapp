package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeGhostSignature
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeKeyMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeMutator
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunProfile
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeVariant
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreview
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

@Composable
fun PacMazeMazeHomePanel(
    profile: PacMazeMazeRunProfile,
    mazeStats: PacMazeMazeStats,
    mazeBestTimeMs: Long,
    selectedSkinId: PacMazeSkinId,
    enabled: Boolean,
    onOpenDaily: () -> Unit,
    onOpenRandom: () -> Unit,
    onOpenArcade: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenContracts: () -> Unit,
    onOpenCompetitive: () -> Unit,
    onOpenCodex: () -> Unit,
    onLaunchConfirm: () -> Unit,
    onChangeCharacter: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val dailySeed = PacMazeMazeRunOptions.dailySeed()
    val dailyOptions = remember(profile, dailySeed) { profile.copy(seedMode = PacMazeMazeSeedMode.DAILY).toRunOptions(dailySeed) }
    val randomOptions = remember(profile, dailySeed) {
        profile.copy(seedMode = PacMazeMazeSeedMode.RANDOM).toRunOptions(dailySeed xor 0x5EED_CAFE0L)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp))) {
        MazeExplainerBanner()
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            Column(
                modifier = Modifier
                    .width(layout.dp(if (layout.isCompactHeight) 108.dp else 124.dp))
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                MazeCompactSideCard(
                    modifier = Modifier.weight(1f),
                    onClick = onChangeCharacter,
                ) {
                    PacMazeCharacterPreview(skinId = selectedSkinId, modifier = Modifier.size(layout.dp(34.dp)))
                    Text("换角色", color = PacMazePalette.inkSecondary, fontSize = layout.captionSp)
                }
                MazeCompactSideCard(modifier = Modifier.weight(1.2f), onClick = onOpenTracks) {
                    Text("赛道", color = PacMazePalette.accentGold, fontWeight = FontWeight.Bold, fontSize = layout.captionSp)
                    PacMazeMazeDifficulty.entries.forEach { diff ->
                        val best = mazeStats.bestTimeByDifficulty[diff.id] ?: 0L
                        Text(
                            "${diff.displayName} ${if (best > 0L) pacMazeFormatBestTime(best) else "—"}",
                            color = if (profile.difficulty == diff) PacMazePalette.accentCyan else PacMazePalette.inkHint,
                            fontSize = 8.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
                    MazePortalCard(
                        "📅", "每日挑战", "今天所有人玩同一张图",
                        PacMazePalette.accentGold, Modifier.weight(1f), onOpenDaily,
                        selected = profile.seedMode == PacMazeMazeSeedMode.DAILY,
                    )
                    MazePortalCard(
                        "🎲", "自由随机", "每局程序生成新迷宫",
                        PacMazePalette.accentCyan, Modifier.weight(1f), onOpenRandom,
                        selected = profile.seedMode == PacMazeMazeSeedMode.RANDOM,
                    )
                    MazePortalCard(
                        "⚡", "变体馆", "追猎/双迷宫等招牌玩法",
                        PacMazePalette.accentPurple, Modifier.weight(1f), onOpenArcade,
                        selected = false,
                    )
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        PacMazeMazeRunPreview(
                            options = dailyOptions,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            badge = "每日同图",
                            badgeAccent = PacMazePalette.accentGold,
                            showFogHint = false,
                        )
                        MazePreviewLegend(Modifier.fillMaxWidth())
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        PacMazeMazeRunPreview(
                            options = randomOptions,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            badge = "自由随机",
                            badgeAccent = PacMazePalette.accentCyan,
                            showFogHint = false,
                        )
                        Text(
                            "左：今日固定 Seed · 右：每局不同",
                            color = PacMazePalette.inkHint,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MazeProfileChips(profile = profile, dailySeed = dailySeed, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .width(layout.dp(132.dp))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(layout.dp(12.dp)))
                            .background(PacMazePalette.ctaGradient)
                            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(layout.dp(12.dp)))
                            .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, enabled = enabled, onClick = onLaunchConfirm)
                            .padding(horizontal = layout.dp(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(layout.dp(22.dp)))
                        Column {
                            Text("快速开局", color = Color.White, fontSize = layout.bodySp, fontWeight = FontWeight.Black)
                            Text("先确认规则", color = Color.White.copy(0.85f), fontSize = 8.sp)
                        }
                    }
                }
            }
        }
        PacMazeMazeBottomNav(onOpenTracks, onOpenContracts, onOpenCompetitive, onOpenCodex)
    }
}

@Composable
fun PacMazeMazePlayGatePanel(
    profile: PacMazeMazeRunProfile,
    mazeStats: PacMazeMazeStats,
    mazeBestTimeMs: Long,
    randomPreviewSeed: Long,
    onBack: () -> Unit,
    onSelectDifficulty: (PacMazeMazeDifficulty) -> Unit,
    onOpenContracts: () -> Unit,
    onNext: () -> Unit,
    onRefreshRandomPreview: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val seed = if (profile.seedMode == PacMazeMazeSeedMode.DAILY) {
        PacMazeMazeRunOptions.dailySeed()
    } else {
        randomPreviewSeed
    }
    val mainOptions = remember(profile, seed) { profile.toRunOptions(seed) }
    val altOptions = remember(profile, seed) { profile.toRunOptions(seed xor 0x5EED_CAFE0L) }
    val signatures = remember(seed, profile) {
        PacMazeMazeGhostSignature.forDailySeed(seed, mainOptions.effectiveGhostCount)
    }
    val mutator = profile.resolvedMutator(seed)
    val keyMode = profile.resolvedKeyMode(seed)
    val dailyBest = if (mazeStats.dailyDate == PacMazeMazeRunOptions.todayDateString()) mazeStats.dailyBestTimeMs else 0L

    PacMazeMazeScrollPanel(title = if (profile.seedMode == PacMazeMazeSeedMode.DAILY) "每日挑战" else "自由随机", onBack = onBack) {
        Text(
            if (profile.seedMode == PacMazeMazeSeedMode.DAILY) {
                "今天全员同图 · Seed ${formatMazeSeedCode(seed)}"
            } else {
                "每局程序生成新迷宫 · 可换示例预览"
            },
            color = PacMazePalette.accentGold,
            fontSize = layout.bodySp,
            fontWeight = FontWeight.Bold,
        )
        Row(modifier = Modifier.fillMaxWidth().height(layout.dp(120.dp)), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
            PacMazeMazeRunPreview(mainOptions, Modifier.weight(1f).fillMaxHeight(), badge = "本局地图", badgeAccent = PacMazePalette.accentGold, showFogHint = false)
            PacMazeMazeRunPreview(altOptions, Modifier.weight(1f).fillMaxHeight(), badge = "同规则对比", badgeAccent = PacMazePalette.accentCyan, showFogHint = false, dimmed = true)
        }
        if (profile.seedMode == PacMazeMazeSeedMode.RANDOM) {
            Text(
                "换一张示例",
                color = PacMazePalette.accentCyan,
                fontSize = layout.captionSp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PacMazePalette.accentCyan.copy(0.12f))
                    .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onRefreshRandomPreview)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        MazeInfoStrip(
            listOf(
                "钥印模式" to keyMode.displayName,
                "周赛规则" to mutator.displayName,
                "幽灵签名" to signatures.joinToString { it.displayName }.ifBlank { "标准" },
                "今日最佳" to if (dailyBest > 0L) pacMazeFormatBestTime(dailyBest) else "—",
                "历史最佳" to if (mazeBestTimeMs > 0L) pacMazeFormatBestTime(mazeBestTimeMs) else "—",
            ),
        )
        Text("难度赛道", color = PacMazePalette.accentCyan, fontWeight = FontWeight.Bold, fontSize = layout.bodySp)
        Row(horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
            PacMazeMazeDifficulty.entries.forEach { diff ->
                MazeSelectChip(diff.displayName, profile.difficulty == diff, mazeTrackAccent(diff)) { onSelectDifficulty(diff) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("契约 · ${profile.contract.displayName}", color = PacMazePalette.accentPurple, fontSize = layout.bodySp)
            Text("全部契约 →", color = PacMazePalette.accentPurple, fontSize = layout.captionSp, modifier = Modifier.pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onOpenContracts))
        }
        MazeLaunchCta("下一步：确认开局", onNext)
    }
}

@Composable
fun PacMazeMazeLaunchConfirmPanel(
    profile: PacMazeMazeRunProfile,
    previewSeed: Long,
    enabled: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val options = remember(profile, previewSeed) { profile.toRunOptions(previewSeed) }
    PacMazeMazeScrollPanel(title = "确认开局", onBack = onBack, fillHeight = true) {
        PacMazeMazeRunPreview(options, Modifier.fillMaxWidth().height(layout.dp(140.dp)), badge = "最终地图", badgeAccent = PacMazePalette.modeMaze, showFogHint = false)
        PacMazeMazeRuleChips(options = options, layout = layout)
        Text(
            "目标：在迷雾中收集钥印、找到出口；幽灵会追你，被碰到会扣命。",
            color = PacMazePalette.inkSecondary,
            fontSize = layout.captionSp,
        )
        MazeLaunchCta("进入迷雾迷宫", onStart, enabled)
    }
}

@Composable
fun PacMazeMazeTrackPickerPanel(
    profile: PacMazeMazeRunProfile,
    mazeStats: PacMazeMazeStats,
    onBack: () -> Unit,
    onSelect: (PacMazeMazeDifficulty) -> Unit,
    onOpenDetail: (PacMazeMazeDifficulty) -> Unit,
) = PacMazeMazeScrollPanel(title = "难度赛道", onBack = onBack) {
    MazeActiveSelectionBanner(
        label = "当前赛道",
        value = profile.difficulty.displayName,
        accent = mazeTrackAccent(profile.difficulty),
    )
    Text("三条独立赛道，地图大小与幽灵数量不同", color = PacMazePalette.inkHint, fontSize = currentPacMazeHubLayout().captionSp)
    PacMazeMazeDifficulty.entries.forEach { diff ->
        val best = mazeStats.bestTimeByDifficulty[diff.id] ?: 0L
        val stars = mazeStats.bestStarsByDifficulty[diff.id] ?: 0
        MazeListCard(
            icon = when (diff) {
                PacMazeMazeDifficulty.SCOUT -> "🌱"
                PacMazeMazeDifficulty.STANDARD -> "🎯"
                PacMazeMazeDifficulty.ABYSS -> "🔥"
            },
            title = diff.displayName,
            subtitle = PacMazeMazeCatalog.trackDetail(diff),
            meta = if (best > 0L) "最佳 ${pacMazeFormatBestTime(best)} · ★$stars" else "尚未挑战",
            selected = profile.difficulty == diff,
            accent = mazeTrackAccent(diff),
            onSelect = { onSelect(diff) },
            onDetail = { onOpenDetail(diff) },
        )
    }
}

@Composable
fun PacMazeMazeTrackDetailPanel(track: PacMazeMazeDifficulty, onBack: () -> Unit, onApply: () -> Unit) =
    PacMazeMazeScrollPanel(title = "${track.displayName}赛道", onBack = onBack) {
        Text(PacMazeMazeCatalog.trackDetail(track), color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().bodySp)
        Text("推荐契约", color = PacMazePalette.accentPurple, fontWeight = FontWeight.Bold, fontSize = currentPacMazeHubLayout().bodySp)
        PacMazeMazeCatalog.recommendedContracts(track).forEach {
            Text("· ${it.displayName} — ${it.tagline}", color = PacMazePalette.inkHint, fontSize = currentPacMazeHubLayout().captionSp)
        }
        MazeLaunchCta("以此赛道配置", onApply)
    }

@Composable
fun PacMazeMazeContractLabPanel(
    profile: PacMazeMazeRunProfile,
    onBack: () -> Unit,
    onSelect: (PacMazeMazeContract) -> Unit,
    onOpenDetail: (PacMazeMazeContract) -> Unit,
) = PacMazeMazeScrollPanel(title = "契约工坊", onBack = onBack) {
    MazeActiveSelectionBanner(
        label = "当前契约",
        value = profile.contract.displayName,
        accent = PacMazePalette.accentPurple,
    )
    Text("契约改变探索/逃生规则，不改变地图 Seed", color = PacMazePalette.inkHint, fontSize = currentPacMazeHubLayout().captionSp)
    PacMazeMazeContract.entries.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(currentPacMazeHubLayout().gap)) {
            row.forEach { contract ->
                MazeGridPickCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📜",
                    title = contract.displayName,
                    subtitle = contract.tagline,
                    selected = profile.contract == contract,
                    accent = PacMazePalette.accentPurple,
                    onClick = { onSelect(contract) },
                    onDetail = { onOpenDetail(contract) },
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PacMazeMazeContractDetailPanel(contract: PacMazeMazeContract, onBack: () -> Unit, onApply: () -> Unit) =
    PacMazeMazeScrollPanel(title = contract.displayName, onBack = onBack) {
        Text(PacMazeMazeCatalog.contractDetail(contract), color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().bodySp)
        MazeLaunchCta("选用此契约", onApply)
    }

@Composable
fun PacMazeMazeArcadeHallPanel(
    profile: PacMazeMazeRunProfile,
    onBack: () -> Unit,
    onSelectVariant: (PacMazeMazeVariant) -> Unit,
    onOpenVariant: (PacMazeMazeVariant) -> Unit,
) = PacMazeMazeScrollPanel(title = "变体馆", onBack = onBack) {
    val layout = currentPacMazeHubLayout()
    MazeActiveSelectionBanner(
        label = "当前变体",
        value = "${profile.variant.emoji} ${profile.variant.displayName}",
        accent = PacMazePalette.modeMaze,
    )
    Text("点选立即切换 · 详情看规则说明", color = PacMazePalette.inkHint, fontSize = layout.captionSp)
    PacMazeMazeVariant.entries.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
            row.forEach { variant ->
                val selected = profile.variant == variant
                MazeGridPickCard(
                    modifier = Modifier.weight(1f).height(layout.dp(92.dp)),
                    emoji = variant.emoji,
                    title = variant.displayName,
                    subtitle = variant.tagline,
                    selected = selected,
                    accent = if (variant == PacMazeMazeVariant.STANDARD) PacMazePalette.accentCyan else PacMazePalette.modeMaze,
                    onClick = { onSelectVariant(variant) },
                    onDetail = { onOpenVariant(variant) },
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PacMazeMazeVariantDetailPanel(
    variant: PacMazeMazeVariant,
    profile: PacMazeMazeRunProfile,
    onBack: () -> Unit,
    onApply: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val preview = remember(profile, variant) {
        profile.copy(variant = variant).toRunOptions(PacMazeMazeRunOptions.dailySeed())
    }
    PacMazeMazeScrollPanel(title = variant.displayName, onBack = onBack, fillHeight = true) {
        Row(Modifier.fillMaxWidth().height(layout.dp(130.dp)), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
            PacMazeMazeRunPreview(preview, Modifier.weight(0.55f).fillMaxHeight(), badge = variant.emoji, badgeAccent = PacMazePalette.modeMaze, showFogHint = false)
            Column(Modifier.weight(0.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(PacMazeMazeCatalog.variantDetail(variant), color = PacMazePalette.inkSecondary, fontSize = layout.captionSp)
                Text("适合：想体验不同节奏的玩家", color = PacMazePalette.inkHint, fontSize = 8.sp)
            }
        }
        MazeLaunchCta("以此变体配置", onApply)
    }
}

@Composable
fun PacMazeMazeCompetitiveHubPanel(
    onBack: () -> Unit,
    onDailyBoard: () -> Unit,
    onWeeklyBoard: () -> Unit,
    onGhostReplay: () -> Unit,
) = PacMazeMazeScrollPanel(title = "竞技中心", onBack = onBack) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(currentPacMazeHubLayout().gap)) {
        MazePortalCard("🏅", "今日榜", "同 Seed 比用时", PacMazePalette.accentGold, Modifier.weight(1f), onDailyBoard)
        MazePortalCard("📆", "周赛榜", "深渊周赛 Mutator", PacMazePalette.accentCyan, Modifier.weight(1f), onWeeklyBoard)
        MazePortalCard("👻", "Ghost 轨迹", "挑战自己最快路线", PacMazePalette.accentPurple, Modifier.weight(1f), onGhostReplay)
    }
}

@Composable
fun PacMazeMazeDailyBoardPanel(mazeStats: PacMazeMazeStats, mazeBestTimeMs: Long, onBack: () -> Unit) =
    PacMazeMazeScrollPanel(title = "今日榜", onBack = onBack) {
        val dailyBest = if (mazeStats.dailyDate == PacMazeMazeRunOptions.todayDateString()) mazeStats.dailyBestTimeMs else 0L
        MazeStatTiles(
            listOf(
                "今日最佳" to if (dailyBest > 0L) pacMazeFormatBestTime(dailyBest) else "—",
                "历史最佳" to if (mazeBestTimeMs > 0L) pacMazeFormatBestTime(mazeBestTimeMs) else "—",
                "规则" to "全员同图同 Seed",
            ),
        )
    }

@Composable
fun PacMazeMazeWeeklyBoardPanel(mutator: PacMazeMazeMutator, onBack: () -> Unit) =
    PacMazeMazeScrollPanel(title = "周赛榜", onBack = onBack) {
        Text("本周 Mutator：${mutator.displayName}", color = PacMazePalette.accentCyan, fontWeight = FontWeight.Bold, fontSize = currentPacMazeHubLayout().bodySp)
        Text(PacMazeMazeCatalog.mutatorDetail(mutator), color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().bodySp)
    }

@Composable
fun PacMazeMazeGhostReplayPanel(mazeBestTimeMs: Long, onBack: () -> Unit) =
    PacMazeMazeScrollPanel(title = "Ghost 轨迹", onBack = onBack) {
        Text("每日挑战会记录你的最快路线，局内显示半透明残影，方便分段超越。", color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().bodySp)
        MazeStatTiles(listOf("参考用时" to if (mazeBestTimeMs > 0L) pacMazeFormatBestTime(mazeBestTimeMs) else "—"))
    }

@Composable
fun PacMazeMazeCodexPanel(onBack: () -> Unit, onOpenEntry: (String) -> Unit) =
    PacMazeMazeScrollPanel(title = "规则图鉴", onBack = onBack) {
        listOf("探索", "逃生", "竞技").forEach { pillar ->
            Text(pillar, color = PacMazePalette.accentGold, fontWeight = FontWeight.Bold, fontSize = currentPacMazeHubLayout().bodySp)
            PacMazeMazeCatalog.codexEntries.filter { it.pillar == pillar }.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(currentPacMazeHubLayout().gap)) {
                    row.forEach { entry ->
                        MazeGridPickCard(
                            modifier = Modifier.weight(1f),
                            emoji = entry.emoji,
                            title = entry.title,
                            subtitle = entry.summary,
                            selected = false,
                            accent = PacMazePalette.modeMaze,
                            onClick = { onOpenEntry(entry.id) },
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

@Composable
fun PacMazeMazeCodexEntryPanel(entryId: String, onBack: () -> Unit) {
    val entry = PacMazeMazeCatalog.codexEntries.firstOrNull { it.id == entryId } ?: return
    PacMazeMazeScrollPanel(title = entry.title, onBack = onBack) {
        Text("${entry.emoji} ${entry.pillar}", color = PacMazePalette.accentGold, fontSize = currentPacMazeHubLayout().captionSp)
        Text(entry.detail, color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().bodySp)
    }
}

@Composable
private fun MazeExplainerBanner() {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A2840), Color(0xFF142238))))
            .border(1.dp, PacMazePalette.accentCyan.copy(0.35f), RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(6.dp)),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(12.dp)),
    ) {
        MazeExplainerItem("🌫", "迷雾", "只照亮周围格子")
        MazeExplainerItem("🔑", "钥印", "集齐才能开出口")
        MazeExplainerItem("📅", "每日", "今天所有人同图")
        MazeExplainerItem("🎲", "随机", "每局新迷宫")
    }
}

@Composable
private fun MazeExplainerItem(emoji: String, title: String, hint: String) {
    Column {
        Text("$emoji $title", color = PacMazePalette.inkPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = PacMazePalette.inkHint, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun MazePreviewLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("墙" to Color(0xFF8FA4B8), "路" to Color(0xFF2A4568), "钥" to PacMazePalette.accentGold, "出口" to PacMazePalette.accentOrange).forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(label, color = PacMazePalette.inkHint, fontSize = 7.sp)
            }
        }
    }
}

@Composable
private fun MazeSubPageHeader(title: String, onBack: () -> Unit) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(Color(0xFF121A2A).copy(alpha = 0.9f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(8.dp)))
            .padding(end = layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = rememberPacMazeUiClick(PacMazeUiSoundId.NavigateBack, onBack),
            modifier = Modifier.size(layout.dp(32.dp)),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = PacMazePalette.accentGold, modifier = Modifier.size(layout.dp(18.dp)))
        }
        Text(title, color = PacMazePalette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = layout.bodySp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PacMazeMazeBottomNav(onTracks: () -> Unit, onContracts: () -> Unit, onCompetitive: () -> Unit, onCodex: () -> Unit) {
    val layout = currentPacMazeHubLayout()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
        MazeNavChip("赛道", "🛤", onTracks, Modifier.weight(1f))
        MazeNavChip("契约", "📜", onContracts, Modifier.weight(1f))
        MazeNavChip("竞技", "🏆", onCompetitive, Modifier.weight(1f))
        MazeNavChip("图鉴", "📖", onCodex, Modifier.weight(1f))
    }
}

@Composable
private fun PacMazeMazeScrollPanel(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    fillHeight: Boolean = false,
    content: @Composable () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (!fillHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
    ) {
        if (title != null && onBack != null) {
            MazeSubPageHeader(title = title, onBack = onBack)
        }
        content()
    }
}

@Composable
private fun MazeCompactSideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2838), Color(0xFF141C2A))))
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.dp(10.dp)))
            .then(
                if (onClick != null) {
                    Modifier.pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(layout.dp(8.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) { content() }
}

@Composable
private fun MazePortalCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(if (selected) accent.copy(0.24f) else accent.copy(0.14f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.copy(0.9f) else accent.copy(0.5f),
                shape = RoundedCornerShape(layout.dp(10.dp)),
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(7.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(emoji, fontSize = layout.bodySp)
            if (selected) {
                Text("已选", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(title, color = accent, fontWeight = FontWeight.Black, fontSize = layout.captionSp, maxLines = 1)
        Text(subtitle, color = PacMazePalette.inkSecondary, fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MazeProfileChips(profile: PacMazeMazeRunProfile, dailySeed: Long, modifier: Modifier = Modifier) {
    data class Chip(val label: String, val accent: Color, val active: Boolean)
    val chips = listOf(
        Chip(profile.difficulty.displayName, mazeTrackAccent(profile.difficulty), true),
        Chip(profile.contract.displayName, PacMazePalette.accentPurple, true),
        Chip(
            if (profile.seedMode == PacMazeMazeSeedMode.DAILY) "每日同图" else "自由随机",
            if (profile.seedMode == PacMazeMazeSeedMode.DAILY) PacMazePalette.accentGold else PacMazePalette.accentCyan,
            true,
        ),
        Chip(profile.variant.displayName, PacMazePalette.modeMaze, true),
        Chip("Seed ${formatMazeSeedCode(dailySeed)}", PacMazePalette.inkSecondary, false),
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { chip ->
            MazeAccentChip(label = chip.label, accent = chip.accent, emphasized = chip.active)
        }
    }
}

@Composable
private fun MazeAccentChip(label: String, accent: Color, emphasized: Boolean) {
    Text(
        label,
        color = if (emphasized) Color.White else PacMazePalette.inkPrimary,
        fontSize = 8.sp,
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (emphasized) accent.copy(0.28f) else Color(0xFF10182A))
            .border(
                width = if (emphasized) 1.5.dp else 1.dp,
                color = if (emphasized) accent.copy(0.85f) else PacMazePalette.cardBorder,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun MazeActiveSelectionBanner(label: String, value: String, accent: Color) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(accent.copy(0.16f))
            .border(1.5.dp, accent.copy(0.65f), RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.dp(12.dp), vertical = layout.dp(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PacMazePalette.inkHint, fontSize = layout.captionSp)
        Text(value, color = accent, fontWeight = FontWeight.Black, fontSize = layout.bodySp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PacMazeMazeRuleChips(options: PacMazeMazeRunOptions, layout: PacMazeHubLayoutSpec) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "迷雾半径 ${options.effectiveFogRadius}",
            "地图 ${options.effectiveMapSize}×${options.effectiveMapSize}",
            "限时 ${options.effectiveTimeLimitSeconds} 秒",
            "${options.effectiveGhostCount} 只幽灵",
            "${options.effectiveKeyCount} 把钥印",
            if (options.keyMode == PacMazeMazeKeyMode.SEALED) "封印钥印（按顺序）" else "自由钥印",
            options.variant.displayName,
            options.mutator.displayName,
        ).forEach { label ->
            Text(
                label,
                color = PacMazePalette.inkPrimary,
                fontSize = 9.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PacMazePalette.modeMaze.copy(0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MazeInfoStrip(items: List<Pair<String, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(currentPacMazeHubLayout().gap),
    ) {
        items.forEach { (k, v) ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(0.06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(k, color = PacMazePalette.inkHint, fontSize = 8.sp)
                Text(v, color = PacMazePalette.inkPrimary, fontSize = currentPacMazeHubLayout().captionSp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MazeStatTiles(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(currentPacMazeHubLayout().gap)) {
        items.forEach { (k, v) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(0.05f))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
            ) {
                Text(k, color = PacMazePalette.inkHint, fontSize = 9.sp)
                Text(v, color = PacMazePalette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = currentPacMazeHubLayout().bodySp)
            }
        }
    }
}

@Composable
private fun MazeSelectChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) accent else PacMazePalette.inkSecondary,
        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
        fontSize = currentPacMazeHubLayout().bodySp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent.copy(0.18f) else Color.White.copy(0.04f))
            .border(1.dp, if (selected) accent.copy(0.6f) else PacMazePalette.cardBorder, RoundedCornerShape(10.dp))
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun MazeNavChip(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(10.dp))
            .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$emoji ", fontSize = currentPacMazeHubLayout().captionSp)
        Text(label, color = PacMazePalette.inkSecondary, fontSize = currentPacMazeHubLayout().captionSp)
    }
}

@Composable
private fun MazeGridPickCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onDetail: (() -> Unit)? = null,
) {
    val layout = currentPacMazeHubLayout()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(accent.copy(0.28f), accent.copy(0.12f)))
                } else {
                    Brush.verticalGradient(listOf(Color.White.copy(0.07f), Color.White.copy(0.03f)))
                },
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.copy(0.9f) else PacMazePalette.cardBorder,
                shape = RoundedCornerShape(layout.dp(10.dp)),
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onClick)
            .padding(layout.dp(8.dp)),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = layout.subtitleSp)
                if (onDetail != null) {
                    Text(
                        "详情",
                        color = accent,
                        fontSize = 8.sp,
                        modifier = Modifier.pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onDetail),
                    )
                }
            }
            Text(title, color = if (selected) accent else PacMazePalette.inkPrimary, fontWeight = FontWeight.Black, fontSize = layout.captionSp, maxLines = 1)
            Text(subtitle, color = PacMazePalette.inkSecondary, fontSize = 8.sp, maxLines = 2, lineHeight = 10.sp)
        }
        if (selected) {
            Text(
                "使用中",
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent.copy(0.95f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MazeListCard(
    icon: String,
    title: String,
    subtitle: String,
    meta: String,
    selected: Boolean,
    accent: Color,
    onSelect: () -> Unit,
    onDetail: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(if (selected) accent.copy(0.14f) else Color.White.copy(0.04f))
            .border(1.dp, if (selected) accent.copy(0.55f) else PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .padding(layout.dp(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
    ) {
        Text(icon, fontSize = layout.subtitleSp)
        Column(Modifier.weight(1f).pacMazeClickable(sound = PacMazeUiSoundId.ListSelect, onClick = onSelect)) {
            Text(title, color = if (selected) accent else PacMazePalette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = layout.bodySp)
            Text(subtitle, color = PacMazePalette.inkSecondary, fontSize = layout.captionSp, maxLines = 2)
            if (meta.isNotBlank()) Text(meta, color = PacMazePalette.inkHint, fontSize = 9.sp)
        }
        Text("详情", color = accent, fontSize = layout.captionSp, modifier = Modifier.pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, onClick = onDetail))
    }
}

@Composable
private fun MazeLaunchCta(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(12.dp)))
            .background(PacMazePalette.ctaGradient)
            .pacMazeClickable(sound = PacMazeUiSoundId.PrimaryConfirm, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
        Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = layout.subtitleSp)
    }
}

private fun mazeTrackAccent(diff: PacMazeMazeDifficulty): Color = when (diff) {
    PacMazeMazeDifficulty.SCOUT -> PacMazePalette.accentMint
    PacMazeMazeDifficulty.STANDARD -> PacMazePalette.accentCyan
    PacMazeMazeDifficulty.ABYSS -> PacMazePalette.difficultyExtreme
}
