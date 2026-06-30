package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.PacMazePrefs
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStageDecor
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStagePreview
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailPreviewStage
import com.example.funlife.ui.screens.pacmaze.cosmetic.trail.PacMazeTrailSwatch

/** L1 · 皮肤系列选择 */
@Composable
fun PacMazeCharacterSeriesPanel(
    loadout: PacMazeAvatarLoadout,
    onOpenSeries: (PacMazeSkinSeries) -> Unit,
    onOpenTrailWorkshop: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeSectionHeader(
            title = "挑选系列",
            subtitle = "共 ${PacMazeSkinId.selectable.size} 款皮肤 · 分系列浏览更清晰",
            accentColor = pacMazeSkinAccent(loadout.skinId),
            layout = layout,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            items(PacMazeSkinSeries.entries, key = { it.name }) { series ->
                val skins = series.skins()
                val selectedInSeries = loadout.skinId in skins
                PacMazeSkinSeriesCard(
                    series = series,
                    count = skins.size,
                    selected = selectedInSeries,
                    onClick = { onOpenSeries(series) },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.gap),
                ) {
                    PacMazeHubTextChip(
                        text = "拖尾工坊",
                        accent = pacMazeTrailAccent(loadout.trailId),
                        onClick = onOpenTrailWorkshop,
                        sound = PacMazeUiSoundId.ChipAction,
                        modifier = Modifier.weight(1f),
                    )
                    PacMazeHubTextChip(
                        text = "收藏册",
                        accent = PacMazePalette.accentPurple,
                        onClick = onOpenCollection,
                        sound = PacMazeUiSoundId.ChipAction,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeSkinSeriesCard(
    series: PacMazeSkinSeries,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val accent = Color(series.accentArgb)
    val shape = RoundedCornerShape(layout.cardRadius)
    val previewSkins = series.skins().take(if (series.isBitmapWalkSeries()) 4 else 3)
    val previewSize = if (series.isBitmapWalkSeries()) layout.dp(58.dp) else layout.dp(30.dp)
    val cardGradient = Brush.linearGradient(
        0f to accent.copy(alpha = if (selected) 0.28f else 0.16f),
        0.55f to Color(0xFF151D30),
        1f to accent.copy(alpha = 0.06f),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardGradient)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else PacMazePalette.cardBorder,
                shape = shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.SeriesCard, onClick = onClick)
            .padding(layout.cardPad),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(layout.dp(52.dp))
                .offset(x = layout.dp(8.dp), y = (-layout.dp(10.dp)))
                .background(accent.copy(alpha = 0.12f), CircleShape),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.dp(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(48.dp))
                    .background(accent.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(series.emoji, fontSize = layout.titleSp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        series.title,
                        color = accent,
                        fontSize = layout.subtitleSp,
                        fontWeight = FontWeight.Black,
                    )
                    if (selected) {
                        Text(
                            "使用中",
                            color = accent,
                            fontSize = layout.captionSp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(alpha = 0.18f))
                                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(2.dp)),
                        )
                    }
                }
                Text(
                    series.subtitle,
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.captionSp,
                    maxLines = 2,
                    lineHeight = (layout.captionSp.value * 1.3f).sp,
                )
                Spacer(modifier = Modifier.height(layout.dp(6.dp)))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    previewSkins.forEach { skin ->
                        Box(
                            modifier = Modifier
                                .size(previewSize)
                                .clip(RoundedCornerShape(layout.dp(6.dp)))
                                .background(Color(0xFF0E121C))
                                .border(1.dp, pacMazeSkinAccent(skin).copy(alpha = 0.45f), RoundedCornerShape(layout.dp(6.dp))),
                        ) {
                            PacMazeCharacterStagePreview(
                                skinId = skin,
                                loadout = PacMazeAvatarLoadout(skinId = skin),
                                modifier = Modifier.fillMaxSize(),
                                selected = false,
                                animateWalk = false,
                            )
                        }
                    }
                    Text(
                        "$count 款",
                        color = PacMazePalette.inkHint,
                        fontSize = layout.captionSp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = layout.dp(2.dp)),
                    )
                }
            }
        }
    }
}

/** L2 · 系列内皮肤网格 */
@Composable
fun PacMazeCharacterGridPanel(
    series: PacMazeSkinSeries,
    loadout: PacMazeAvatarLoadout,
    onSelectSkin: (PacMazeSkinId) -> Unit,
    onOpenDetail: (PacMazeSkinId) -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val skins = series.skins()
    val columns = when (series) {
        PacMazeSkinSeries.IKUN, PacMazeSkinSeries.YISHI -> 3
        else -> if (layout.isCompactHeight) 3 else 3
    }
    val cellAspect = if (series.isBitmapWalkSeries()) 1.08f else 0.78f

    LaunchedEffect(series) {
        when (series) {
            PacMazeSkinSeries.FOOD, PacMazeSkinSeries.IKUN, PacMazeSkinSeries.YISHI -> {
                PacMazeRemoteSkinAnimCache.warmCoverCacheAsync()
                PacMazeRemoteSkinAnimCache.requestPreloadAllCoversAsync()
            }
            else -> Unit
        }
    }

    LaunchedEffect(series, loadout.skinId) {
        if (!series.isBitmapWalkSeries()) return@LaunchedEffect
        // 网格仅封面；完整 walk 序列在详情页/进局时再加载
        PacMazeRemoteSkinAnimCache.requestPreloadCoverAsync(loadout.skinId)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeSectionHeader(
            title = series.title,
            subtitle = "点击选中 · 长按或二次点击进入详情",
            accentColor = pacMazeSkinAccent(loadout.skinId),
            layout = layout,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
            verticalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            items(skins, key = { it.storageKey }) { skin ->
                PacMazeSkinGridCell(
                    skin = skin,
                    loadout = loadout,
                    selected = skin == loadout.skinId,
                    aspectRatio = cellAspect,
                    onClick = {
                        if (skin == loadout.skinId) onOpenDetail(skin) else onSelectSkin(skin)
                    },
                )
            }
        }
    }
}

@Composable
private fun PacMazeSkinGridCell(
    skin: PacMazeSkinId,
    loadout: PacMazeAvatarLoadout,
    selected: Boolean,
    aspectRatio: Float = 0.78f,
    onClick: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val accent = pacMazeSkinAccent(skin)
    val tier = PacMazeCosmeticCatalog.bodyTier(skin)
    val shape = RoundedCornerShape(layout.cardRadius)
    val topGlow = Brush.verticalGradient(
        0f to accent.copy(alpha = if (selected) 0.35f else 0.12f),
        0.45f to Color.Transparent,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(PacMazeCharacterStageDecor.neutralCardFill(selected))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else PacMazePalette.cardBorder,
                shape = shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(layout.dp(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.dp(3.dp))
                    .clip(RoundedCornerShape(topStart = layout.dp(6.dp), topEnd = layout.dp(6.dp)))
                    .background(topGlow),
            )
            PacMazeCharacterStagePreview(
                skinId = skin,
                loadout = if (selected) loadout else PacMazeAvatarLoadout(skinId = skin),
                modifier = Modifier.fillMaxSize(),
                selected = selected,
                animateWalk = selected,
                powerActive = selected && skin.hasPowerAura(),
                liteMode = true,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(layout.dp(18.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(layout.dp(12.dp)))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121828))
                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(4.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                skin.displayName,
                color = if (selected) PacMazePalette.inkPrimary else PacMazePalette.inkSecondary,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                tier.label,
                color = accent.copy(alpha = 0.85f),
                fontSize = layout.captionSp,
            )
        }
    }
}

/** L3 · 皮肤详情 */
@Composable
fun PacMazeCharacterDetailPanel(
    skinId: PacMazeSkinId,
    loadout: PacMazeAvatarLoadout,
    onSelectSkin: () -> Unit,
    onOpenTrailWorkshop: () -> Unit,
    onApplyRecommendedTrail: () -> Unit,
    onConfirm: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val accent = pacMazeSkinAccent(skinId)
    val tier = PacMazeCosmeticCatalog.bodyTier(skinId)
    val previewLoadout = loadout.copy(skinId = skinId)
    val recommended = PacMazeCosmeticCatalog.recommendedTrail(skinId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    layout.dp(
                        when {
                            PacMazeBitmapWalkCatalog.contains(skinId) && layout.isCompactHeight -> 168.dp
                            PacMazeBitmapWalkCatalog.contains(skinId) -> 210.dp
                            layout.isCompactHeight -> 120.dp
                            else -> 150.dp
                        },
                    ),
                )
                .clip(RoundedCornerShape(layout.cardRadius))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(layout.cardRadius)),
        ) {
            PacMazeCharacterStagePreview(
                skinId = skinId,
                loadout = previewLoadout,
                modifier = Modifier.fillMaxSize(),
                selected = true,
                animateWalk = true,
                powerActive = skinId.hasPowerAura(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(skinId.emoji, fontSize = layout.titleSp)
                Text(
                    skinId.displayName,
                    color = accent,
                    fontSize = layout.titleSp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                skinId.subtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = layout.bodySp,
                lineHeight = (layout.bodySp.value * 1.35f).sp,
            )
        }

        PacMazeDetailStatsRow(
            label = "体型",
            value = tier.label,
            accent = accent,
        )
        PacMazeDetailStatsRow(
            label = "移速",
            value = "${(tier.speedMul * 100).toInt()}%",
            accent = accent,
        )
        PacMazeDetailStatsRow(
            label = "系列",
            value = pacMazeSkinTag(skinId),
            accent = accent,
        )
        PacMazeDetailStatsRow(
            label = "推荐拖尾",
            value = recommended.displayName,
            accent = pacMazeTrailAccent(recommended),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            PacMazeHubTextChip(
                text = "应用推荐拖尾",
                accent = pacMazeTrailAccent(recommended),
                onClick = onApplyRecommendedTrail,
                sound = PacMazeUiSoundId.UtilityRecommend,
                modifier = Modifier.weight(1f),
            )
            PacMazeHubTextChip(
                text = "拖尾工坊",
                accent = PacMazePalette.accentCyan,
                onClick = onOpenTrailWorkshop,
                sound = PacMazeUiSoundId.ChipAction,
                modifier = Modifier.weight(1f),
            )
        }

        if (loadout.skinId != skinId) {
            PacMazePrimaryButton(text = "选用此角色", onClick = onSelectSkin)
        }
        PacMazePrimaryButton(
            text = "确认并返回选关",
            onClick = onConfirm,
        )
    }
}

@Composable
private fun PacMazeDetailStatsRow(label: String, value: String, accent: Color) {
    val layout = currentPacMazeHubLayout()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Color(0xFF151D30))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.cardPad, vertical = layout.dp(8.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PacMazePalette.inkMuted, fontSize = layout.bodySp)
        Text(value, color = accent, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
    }
}

/** 拖尾工坊：左预览坞 + 右高密度网格，最大化横屏空间利用率。 */
@Composable
fun PacMazeTrailWorkshopPanel(
    loadout: PacMazeAvatarLoadout,
    onSelectTrail: (PacMazeTrailId) -> Unit,
    onApplyRecommended: () -> Unit,
    onConfirm: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val trails = PacMazeTrailId.selectable
    val trailIndex = trails.indexOf(loadout.trailId).coerceAtLeast(0) + 1
    val recommended = PacMazeCosmeticCatalog.recommendedTrail(loadout.skinId)
    val trailAccent = pacMazeTrailAccent(loadout.trailId)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        PacMazeLoadoutPreviewDock(
            loadout = loadout,
            layout = layout,
            badgeText = String.format("%02d / %02d", trailIndex, trails.size),
            modifier = Modifier
                .width(layout.dp(if (layout.isCompactHeight) 96.dp else 112.dp))
                .fillMaxHeight(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
        ) {
            PacMazeTrailWorkshopToolbar(
                loadout = loadout,
                recommended = recommended,
                trailAccent = trailAccent,
                layout = layout,
                onApplyRecommended = onApplyRecommended,
                onConfirm = onConfirm,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val cols = when {
                    maxWidth >= layout.dp(480.dp) -> 4
                    maxWidth >= layout.dp(320.dp) -> 3
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                    verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = layout.dp(2.dp)),
                ) {
                    items(trails, key = { it.storageKey }) { trail ->
                        PacMazeCollectionTrailCell(
                            trail = trail,
                            selected = trail == loadout.trailId,
                            onClick = { onSelectTrail(trail) },
                            layout = layout,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PacMazeTrailWorkshopToolbar(
    loadout: PacMazeAvatarLoadout,
    recommended: PacMazeTrailId,
    trailAccent: Color,
    layout: PacMazeHubLayoutSpec,
    onApplyRecommended: () -> Unit,
    onConfirm: () -> Unit,
) {
    val toolbarH = layout.dp(if (layout.isCompactHeight) 36.dp else 40.dp)
    val shape = RoundedCornerShape(layout.dp(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarH)
            .clip(shape)
            .background(Color(0xFF151D30))
            .border(1.dp, PacMazePalette.cardBorder, shape)
            .padding(horizontal = layout.dp(6.dp)),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PacMazeHubTextChip(
            text = "推荐 ${recommended.emoji}",
            accent = pacMazeTrailAccent(recommended),
            onClick = onApplyRecommended,
            sound = PacMazeUiSoundId.UtilityRecommend,
            compact = true,
        )

        Text(
            "${loadout.trailId.emoji} ${loadout.trailId.displayName} · ${loadout.skinId.displayName}",
            modifier = Modifier.weight(1f),
            color = trailAccent,
            fontSize = layout.captionSp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        PacMazePrimaryButton(
            text = "确认拖尾",
            onClick = onConfirm,
            compact = true,
            dense = true,
            modifier = Modifier
                .height(toolbarH - layout.dp(8.dp))
                .widthIn(min = layout.dp(72.dp), max = layout.dp(92.dp)),
        )
    }
}

private enum class CollectionTab { Skins, Trails, Ghosts }

/** 收藏册：左预览坞 + 右高密度网格，最大化横屏空间利用率。 */
@Composable
fun PacMazeCollectionBookPanel(
    userId: Long,
    loadout: PacMazeAvatarLoadout,
    onSelectSkin: (PacMazeSkinId) -> Unit,
    onSelectTrail: (PacMazeTrailId) -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val context = LocalContext.current
    val prefs = remember(userId) { PacMazePrefs(context) }
    var tab by remember { mutableStateOf(CollectionTab.Skins) }
    val skinIndex = PacMazeSkinId.selectable.indexOf(loadout.skinId).coerceAtLeast(0) + 1

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        PacMazeLoadoutPreviewDock(
            loadout = loadout,
            layout = layout,
            badgeText = String.format("%02d / %02d", skinIndex, PacMazeSkinId.selectable.size),
            modifier = Modifier
                .width(
                    layout.dp(
                        when {
                            PacMazeBitmapWalkCatalog.contains(loadout.skinId) && layout.isCompactHeight -> 118.dp
                            PacMazeBitmapWalkCatalog.contains(loadout.skinId) -> 136.dp
                            layout.isCompactHeight -> 96.dp
                            else -> 112.dp
                        },
                    ),
                )
                .fillMaxHeight(),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
        ) {
            PacMazeCollectionTabBar(
                tab = tab,
                onTabChange = { tab = it },
                skinCount = PacMazeSkinId.selectable.size,
                trailCount = PacMazeTrailId.selectable.size,
                ghostCount = GhostKind.codexOrder.size,
                layout = layout,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (tab) {
                    CollectionTab.Skins -> {
                        val cellMin = layout.dp(58.dp)
                        val cols = (maxWidth / cellMin).toInt().coerceIn(5, 11)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                            verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                        ) {
                            items(PacMazeSkinId.selectable, key = { it.storageKey }) { skin ->
                                PacMazeCollectionSkinCell(
                                    skin = skin,
                                    loadout = loadout,
                                    onClick = { onSelectSkin(skin) },
                                    layout = layout,
                                )
                            }
                        }
                    }
                    CollectionTab.Trails -> {
                        val cols = if (maxWidth >= layout.dp(360.dp)) 3 else 2
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                            verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                        ) {
                            items(PacMazeTrailId.selectable, key = { it.storageKey }) { trail ->
                                PacMazeCollectionTrailCell(
                                    trail = trail,
                                    selected = trail == loadout.trailId,
                                    onClick = { onSelectTrail(trail) },
                                    layout = layout,
                                )
                            }
                        }
                    }
                    CollectionTab.Ghosts -> {
                        PacMazeGhostCodexGrid(
                            userId = userId,
                            prefs = prefs,
                            modifier = Modifier.fillMaxSize(),
                            layout = layout,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PacMazeLoadoutPreviewDock(
    loadout: PacMazeAvatarLoadout,
    layout: PacMazeHubLayoutSpec,
    badgeText: String,
    modifier: Modifier = Modifier,
) {
    val accent = pacMazeSkinAccent(loadout.skinId)
    val trailAccent = pacMazeTrailAccent(loadout.trailId)
    val shape = RoundedCornerShape(layout.dp(10.dp))
    val footerShape = RoundedCornerShape(
        bottomStart = layout.dp(10.dp),
        bottomEnd = layout.dp(10.dp),
    )

    Column(
        modifier = modifier
            .clip(shape)
            .border(1.dp, PacMazePalette.cardBorder, shape),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PacMazeTrailPreviewStage(
            loadout = loadout,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            showTrackHint = false,
            showBorder = false,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121828), footerShape)
                .padding(horizontal = layout.dp(4.dp), vertical = layout.dp(3.dp)),
            verticalArrangement = Arrangement.spacedBy(layout.dp(2.dp)),
        ) {
            Text(
                badgeText,
                color = PacMazePalette.inkHint,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                Text(loadout.skinId.emoji, fontSize = layout.bodySp)
                Text(
                    loadout.skinId.displayName,
                    color = accent,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
            ) {
                Text(loadout.trailId.emoji, fontSize = layout.bodySp)
                Text(
                    loadout.trailId.displayName,
                    color = trailAccent,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PacMazeCollectionTabBar(
    tab: CollectionTab,
    onTabChange: (CollectionTab) -> Unit,
    skinCount: Int,
    trailCount: Int,
    ghostCount: Int,
    layout: PacMazeHubLayoutSpec,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.dp(28.dp))
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(Color(0xFF0E121C))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(8.dp)))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CollectionTab.entries.forEach { item ->
            val selected = tab == item
            val label = when (item) {
                CollectionTab.Skins -> "皮肤 $skinCount"
                CollectionTab.Trails -> "拖尾 $trailCount"
                CollectionTab.Ghosts -> "幽灵 $ghostCount"
            }
            val accent = when (item) {
                CollectionTab.Skins -> PacMazePalette.accentGold
                CollectionTab.Trails -> PacMazePalette.accentPurple
                CollectionTab.Ghosts -> PacMazePalette.accentCyan
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(layout.dp(6.dp)))
                    .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = accent.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(layout.dp(6.dp)),
                    )
                    .pacMazeClickable(sound = PacMazeUiSoundId.TabSwitch) { onTabChange(item) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) accent else PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PacMazeCollectionSkinCell(
    skin: PacMazeSkinId,
    loadout: PacMazeAvatarLoadout,
    onClick: () -> Unit,
    layout: PacMazeHubLayoutSpec,
) {
    val selected = skin == loadout.skinId
    val accent = pacMazeSkinAccent(skin)
    val ikun = PacMazeBitmapWalkCatalog.contains(skin)
    val shape = RoundedCornerShape(layout.dp(6.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (ikun) 1.05f else 1f)
            .clip(shape)
            .background(PacMazeCharacterStageDecor.neutralCardFill(selected))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else PacMazePalette.cardBorder,
                shape = shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect, onClick = onClick)
            .padding(1.dp),
    ) {
        PacMazeCharacterStagePreview(
            skinId = skin,
            loadout = if (selected) loadout else PacMazeAvatarLoadout(skinId = skin),
            modifier = Modifier.fillMaxSize(),
            selected = selected,
            animateWalk = selected,
            liteMode = true,
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(layout.dp(12.dp))
                    .padding(1.dp),
            )
        }
    }
}

@Composable
private fun PacMazeCollectionTrailCell(
    trail: PacMazeTrailId,
    selected: Boolean,
    onClick: () -> Unit,
    layout: PacMazeHubLayoutSpec,
) {
    val accent = pacMazeTrailAccent(trail)
    val shape = RoundedCornerShape(layout.dp(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PacMazeCharacterStageDecor.neutralCardFill(selected))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent else PacMazePalette.cardBorder,
                shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect, onClick = onClick)
            .padding(layout.dp(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PacMazeTrailSwatch(
            trailId = trail,
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.dp(36.dp)),
            animate = selected,
        )
        Text(
            trail.displayName,
            color = if (selected) accent else PacMazePalette.inkSecondary,
            fontSize = layout.captionSp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}