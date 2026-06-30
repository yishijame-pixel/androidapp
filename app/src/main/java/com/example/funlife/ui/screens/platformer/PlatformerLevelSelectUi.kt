package com.example.funlife.ui.screens.platformer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.game.platformer.PLATFORMER_SKY_LEVEL_START
import com.example.funlife.game.platformer.PLATFORMER_HERO_LEVEL_START
import com.example.funlife.game.platformer.PLATFORMER_TOTAL_LEVEL_COUNT
import com.example.funlife.game.platformer.PlatformerSuperTuxLengthSpec
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.catalog.PlatformerCharacterPrefetch
import com.example.funlife.game.platformer.catalog.PlatformerUnlockProgress
import com.example.funlife.game.platformer.catalog.catalogId
import com.example.funlife.game.platformer.PlatformerEndlessRunner
import com.example.funlife.game.platformer.PlatformerLevelDef
import com.example.funlife.game.platformer.PlatformerLevels
import com.example.funlife.game.platformer.PlatformerTheme
import kotlin.math.min

private object PlatformerHubArt {
    val duskTop = Color(0xFF1A1535)
    val duskMid = Color(0xFF2D2248)
    val duskWarm = Color(0xFF4A3058)
    val horizonGlow = Color(0xFFFF9E5C)
    val meadow = Color(0xFF6BBF59)

    val panelTop = Color(0xFF2A2238)
    val panelBottom = Color(0xFF1E1828)
    val panelBorder = Color(0xFFFFB74D).copy(alpha = 0.28f)
    val panelBorderSoft = Color(0xFFFFFFFF).copy(alpha = 0.08f)

    val gold = Color(0xFFFFCA28)
    val goldDeep = Color(0xFFFF8F00)
    val cream = Color(0xFFFFF8E7)
    val creamMuted = Color(0xFFB8A99A)
    val inkSoft = Color(0xFF8E7F92)

    val cardBase = Color(0xFF2A2438)
    val cardGlow = Color(0xFF3A3248)

    val continueGrad = listOf(Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFE65100))
    val endlessGrad = listOf(Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF26A69A))
    val unlockGrad = listOf(Color(0xFF5D4037), Color(0xFF8D6E63))
    val progressGrad = listOf(Color(0xFF81C784), Color(0xFFFFCA28))
    val tabActiveGrad = listOf(Color(0xFFFFB74D), Color(0xFFFF8F00))
}

private enum class PlatformerHubTier { Compact, Standard, Expanded }

private data class PlatformerHubLayoutSpec(
    val scale: Float,
    val tier: PlatformerHubTier,
    val areaWidth: Dp,
    val areaHeight: Dp,
    val showSidebar: Boolean,
    val stackTopActions: Boolean,
    val horizontalPad: Dp,
    val gap: Dp,
    val panelPad: Dp,
    val panelRadius: Dp,
    val sidebarWidth: Dp,
    val gridCellMin: Dp,
    val tileHeight: Dp,
    val actionChipHeight: Dp,
    val titleSp: TextUnit,
    val bodySp: TextUnit,
    val captionSp: TextUnit,
    val microSp: TextUnit,
) {
    fun dp(base: Dp): Dp = base * scale

    companion object {
        fun compute(maxWidth: Dp, maxHeight: Dp, fontScale: Float): PlatformerHubLayoutSpec {
            val ref = min(maxWidth.value, maxHeight.value)
            val scale = (ref / 360f).coerceIn(0.82f, 1.12f)
            val tier = when {
                maxHeight < 260.dp || maxWidth < 480.dp -> PlatformerHubTier.Compact
                maxHeight >= 320.dp && maxWidth >= 720.dp -> PlatformerHubTier.Expanded
                else -> PlatformerHubTier.Standard
            }
            val showSidebar = maxWidth >= 560.dp && maxHeight >= 240.dp
            val stackTopActions = maxWidth < 680.dp
            val fontBoost = fontScale.coerceIn(1f, 1.25f)
            val tileBase = when (tier) {
                PlatformerHubTier.Compact -> 92.dp
                PlatformerHubTier.Standard -> 96.dp
                PlatformerHubTier.Expanded -> 100.dp
            }
            val gridBase = when (tier) {
                PlatformerHubTier.Compact -> 132.dp
                PlatformerHubTier.Standard -> 142.dp
                PlatformerHubTier.Expanded -> 152.dp
            }
            return PlatformerHubLayoutSpec(
                scale = scale,
                tier = tier,
                areaWidth = maxWidth,
                areaHeight = maxHeight,
                showSidebar = showSidebar,
                stackTopActions = stackTopActions,
                horizontalPad = dpScaled(8.dp, scale),
                gap = dpScaled(if (tier == PlatformerHubTier.Compact) 6.dp else 8.dp, scale),
                panelPad = dpScaled(if (tier == PlatformerHubTier.Compact) 8.dp else 10.dp, scale),
                panelRadius = dpScaled(14.dp, scale),
                sidebarWidth = (maxWidth * 0.24f).coerceIn(168.dp, 220.dp),
                gridCellMin = gridBase * scale,
                tileHeight = tileBase * scale * fontBoost,
                actionChipHeight = dpScaled(48.dp, scale),
                titleSp = (14f * scale).sp,
                bodySp = (11f * scale).sp,
                captionSp = (10f * scale).sp,
                microSp = (9f * scale).sp,
            )
        }

        private fun dpScaled(base: Dp, scale: Float): Dp = base * scale
    }
}

enum class PlatformerChapterFilter(val label: String, val range: IntRange) {
    ALL("全部", 1..PLATFORMER_TOTAL_LEVEL_COUNT),
    CLASSIC("经典", 1..6),
    PACK("素材", 7..16),
    STORY("剧情", 17..22),
    EPIC("长关", 23..34),
    TIER("分层", 35..52),
    SKY("高空", PLATFORMER_SKY_LEVEL_START..(PLATFORMER_SKY_LEVEL_START + 9)),
    HEROES("群英", PLATFORMER_HERO_LEVEL_START..(PLATFORMER_HERO_LEVEL_START + 11)),
    SUPERTUX(
        "SuperTux",
        PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_START..PlatformerSuperTuxLengthSpec.SUPERTUX_LEVEL_END,
    ),
    SUPERTUX_W1(
        "南极",
        901..931,
    ),
    SUPERTUX_W2(
        "森林",
        941..978,
    ),
    SUPERTUX_BONUS(
        "Bonus",
        981..1010,
    ),
    ;

    fun chipLabel(compact: Boolean): String = when {
        !compact && this == ALL -> label
        !compact -> "$label ${range.first}–${range.last}"
        this == ALL -> label
        else -> "${range.first}–${range.last}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformerLevelSelectPanel(
    onBack: () -> Unit,
    testUnlockAll: Boolean,
    maxUnlockedLevel: Int,
    selectedCharacter: PlatformerCharacterId,
    onCharacterSelect: (PlatformerCharacterId) -> Unit,
    onUnlockAll: () -> Unit,
    onSelect: (Int) -> Unit,
    onStartEndless: () -> Unit,
    onStartTempleRun: () -> Unit = {},
    onStartPlaneShooter: () -> Unit = {},
    onStartHillClimb: () -> Unit = {},
) {
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = remember(maxWidth, maxHeight, fontScale) {
            PlatformerHubLayoutSpec.compute(maxWidth, maxHeight, fontScale)
        }

        var chapter by remember { mutableStateOf(PlatformerChapterFilter.ALL) }
        val levels = remember(chapter) {
            PlatformerLevels.all.filter { it.id in chapter.range }
        }
        val unlockedCount = if (testUnlockAll) {
            PLATFORMER_TOTAL_LEVEL_COUNT
        } else {
            maxUnlockedLevel.coerceIn(0, PLATFORMER_TOTAL_LEVEL_COUNT)
        }
        val progress = unlockedCount.toFloat() / PLATFORMER_TOTAL_LEVEL_COUNT

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(safePadding),
        ) {
            PlatformerHubBackdrop(Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = layout.horizontalPad,
                        vertical = layout.dp(6.dp),
                    ),
                verticalArrangement = Arrangement.spacedBy(layout.gap),
            ) {
                PlatformerHubTopActionBar(
                    layout = layout,
                    onBack = onBack,
                    unlockedCount = unlockedCount,
                    maxUnlockedLevel = maxUnlockedLevel,
                    testUnlockAll = testUnlockAll,
                    canContinue = unlockedCount in 1..PLATFORMER_TOTAL_LEVEL_COUNT,
                    onContinue = {
                        val idx = PlatformerLevels.all.indexOfFirst { it.id == maxUnlockedLevel }
                        if (idx >= 0) onSelect(idx)
                    },
                    onStartEndless = onStartEndless,
                    onStartTempleRun = onStartTempleRun,
                    onStartPlaneShooter = onStartPlaneShooter,
                    onStartHillClimb = onStartHillClimb,
                    onUnlockAll = onUnlockAll,
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(layout.gap),
                ) {
                    if (layout.showSidebar) {
                        PlatformerHubSidebar(
                            modifier = Modifier
                                .width(layout.sidebarWidth)
                                .fillMaxHeight(),
                            selectedCharacter = selectedCharacter,
                            onCharacterSelect = onCharacterSelect,
                            unlockedCount = unlockedCount,
                            progress = progress,
                            layout = layout,
                        )
                    }

                    PlatformerHubCatalogPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        layout = layout,
                        levels = levels,
                        chapter = chapter,
                        onChapterSelect = { chapter = it },
                        testUnlockAll = testUnlockAll,
                        maxUnlockedLevel = maxUnlockedLevel,
                        showInlineCharacters = !layout.showSidebar,
                        selectedCharacter = selectedCharacter,
                        onCharacterSelect = onCharacterSelect,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformerHubCatalogPanel(
    modifier: Modifier,
    layout: PlatformerHubLayoutSpec,
    levels: List<PlatformerLevelDef>,
    chapter: PlatformerChapterFilter,
    onChapterSelect: (PlatformerChapterFilter) -> Unit,
    testUnlockAll: Boolean,
    maxUnlockedLevel: Int,
    showInlineCharacters: Boolean,
    selectedCharacter: PlatformerCharacterId,
    onCharacterSelect: (PlatformerCharacterId) -> Unit,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.panelRadius))
            .background(
                Brush.verticalGradient(
                    listOf(PlatformerHubArt.panelTop, PlatformerHubArt.panelBottom),
                ),
            )
            .border(1.dp, PlatformerHubArt.panelBorder, RoundedCornerShape(layout.panelRadius))
            .padding(
                start = layout.panelPad,
                end = layout.panelPad,
                top = layout.dp(6.dp),
                bottom = layout.dp(6.dp),
            ),
    ) {
        PlatformerCatalogHeader(
            levelsCount = levels.size,
            chapter = chapter,
            layout = layout,
        )

        if (showInlineCharacters) {
            Spacer(Modifier.height(layout.dp(4.dp)))
            PlatformerCharacterPickerRow(
                selectedCharacter = selectedCharacter,
                onCharacterSelect = onCharacterSelect,
                layout = layout,
            )
        }

        Spacer(Modifier.height(layout.dp(4.dp)))
        PlatformerChapterTabs(
            selected = chapter,
            onSelect = onChapterSelect,
            layout = layout,
        )
        Spacer(Modifier.height(layout.dp(4.dp)))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = layout.gridCellMin),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = layout.dp(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
            verticalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            items(
                items = levels,
                key = { level -> "${level.id}-$testUnlockAll-$maxUnlockedLevel-$chapter" },
            ) { level ->
                val unlocked = testUnlockAll || PlatformerUnlockProgress.isLevelUnlocked(level.id)
                val isCurrent = !testUnlockAll && level.id == maxUnlockedLevel
                PlatformerLevelTile(
                    level = level,
                    unlocked = unlocked,
                    isCurrent = isCurrent,
                    layout = layout,
                    onClick = {
                        if (unlocked) {
                            onSelect(PlatformerLevels.all.indexOfFirst { it.id == level.id })
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlatformerCatalogHeader(
    levelsCount: Int,
    chapter: PlatformerChapterFilter,
    layout: PlatformerHubLayoutSpec,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "关卡目录",
            color = PlatformerHubArt.cream,
            fontWeight = FontWeight.Bold,
            fontSize = layout.bodySp,
            maxLines = 1,
        )
        Text(
            " · $levelsCount 关 · ${chapter.label}",
            color = PlatformerHubArt.creamMuted,
            fontSize = layout.microSp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun PlatformerCharacterPickerRow(
    selectedCharacter: PlatformerCharacterId,
    onCharacterSelect: (PlatformerCharacterId) -> Unit,
    layout: PlatformerHubLayoutSpec,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        items(PlatformerCharacterId.entries) { char ->
            val selected = selectedCharacter == char
            val unlocked = PlatformerUnlockProgress.isCharacterUnlocked(char.catalogId)
            val chipColor = characterChipColor(char)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) chipColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                    )
                    .border(
                        1.dp,
                        if (selected) chipColor.copy(alpha = 0.65f) else PlatformerHubArt.panelBorderSoft,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable(enabled = unlocked) {
                        PlatformerCharacterPrefetch.prefetchOnSelect(char)
                        onCharacterSelect(char)
                    }
                    .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(5.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(layout.dp(7.dp))
                        .background(if (selected) chipColor else PlatformerHubArt.inkSoft, CircleShape),
                )
                Text(
                    if (unlocked) char.displayTitle else "🔒",
                    modifier = Modifier.padding(start = layout.dp(5.dp)),
                    color = if (unlocked) {
                        if (selected) PlatformerHubArt.cream else PlatformerHubArt.creamMuted
                    } else {
                        PlatformerHubArt.creamMuted.copy(alpha = 0.45f)
                    },
                    fontSize = layout.captionSp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformerHubTopActionBar(
    layout: PlatformerHubLayoutSpec,
    onBack: () -> Unit,
    unlockedCount: Int,
    maxUnlockedLevel: Int,
    testUnlockAll: Boolean,
    canContinue: Boolean,
    onContinue: () -> Unit,
    onStartEndless: () -> Unit,
    onStartTempleRun: () -> Unit = {},
    onStartPlaneShooter: () -> Unit = {},
    onStartHillClimb: () -> Unit = {},
    onUnlockAll: () -> Unit,
) {
    val barShape = RoundedCornerShape(layout.panelRadius)
    val barModifier = Modifier
        .fillMaxWidth()
        .clip(barShape)
        .background(
            Brush.horizontalGradient(
                listOf(PlatformerHubArt.panelTop, PlatformerHubArt.panelBottom),
            ),
        )
        .border(1.dp, PlatformerHubArt.panelBorder, barShape)
        .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp))

    if (layout.stackTopActions) {
        Column(
            modifier = barModifier,
            verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(layout.dp(36.dp))
                        .background(Color.White.copy(alpha = 0.06f), CircleShape),
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "返回",
                        tint = PlatformerHubArt.cream,
                        modifier = Modifier.size(layout.dp(18.dp)),
                    )
                }
                Column(Modifier.padding(start = layout.dp(4.dp))) {
                    Text(
                        "坤坤大冒险",
                        color = PlatformerHubArt.cream,
                        fontWeight = FontWeight.Black,
                        fontSize = layout.titleSp,
                    )
                    Text(
                        "$unlockedCount/$PLATFORMER_TOTAL_LEVEL_COUNT 关",
                        color = PlatformerHubArt.gold.copy(alpha = 0.9f),
                        fontSize = layout.microSp,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                if (canContinue) {
                    PlatformerHubActionChip(
                        modifier = Modifier.width(layout.dp(148.dp)),
                        layout = layout,
                        gradient = PlatformerHubArt.continueGrad,
                        icon = {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(layout.dp(16.dp)),
                            )
                        },
                        title = "继续闯关",
                        subtitle = "第 $maxUnlockedLevel 关",
                        onClick = onContinue,
                    )
                }
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(148.dp)),
                    layout = layout,
                    gradient = PlatformerHubArt.endlessGrad,
                    icon = {
                        Text("♾", color = PlatformerHubArt.gold, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                    },
                    title = "无尽跑酷",
                    subtitle = "最佳 ${PlatformerEndlessRunner.bestTilesRun} 格",
                    onClick = onStartEndless,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(120.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)),
                    icon = { Text("🏃", fontSize = layout.bodySp) },
                    title = "神庙跑酷",
                    subtitle = "三车道",
                    onClick = onStartTempleRun,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(120.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                    icon = { Text("✈", fontSize = layout.bodySp) },
                    title = "天空射击",
                    subtitle = "plane",
                    onClick = onStartPlaneShooter,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(120.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF558B2F), Color(0xFF33691E)),
                    icon = { Text("🚗", fontSize = layout.bodySp) },
                    title = "登山挑战",
                    subtitle = "hillclimb",
                    onClick = onStartHillClimb,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(132.dp)),
                    layout = layout,
                    gradient = if (testUnlockAll) {
                        listOf(Color(0xFF43A047), Color(0xFF2E7D32))
                    } else {
                        PlatformerHubArt.unlockGrad
                    },
                    icon = {
                        Icon(
                            if (testUnlockAll) Icons.Default.LockOpen else Icons.Default.Lock,
                            null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(layout.dp(14.dp)),
                        )
                    },
                    title = if (testUnlockAll) "已全开" else "解锁全关",
                    subtitle = "测试用",
                    onClick = onUnlockAll,
                )
            }
        }
    } else {
        Row(
            modifier = barModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(layout.dp(36.dp))
                    .background(Color.White.copy(alpha = 0.06f), CircleShape),
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    "返回",
                    tint = PlatformerHubArt.cream,
                    modifier = Modifier.size(layout.dp(18.dp)),
                )
            }
            Column(Modifier.width(layout.dp(96.dp))) {
                Text(
                    "坤坤大冒险",
                    color = PlatformerHubArt.cream,
                    fontWeight = FontWeight.Black,
                    fontSize = layout.titleSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$unlockedCount/$PLATFORMER_TOTAL_LEVEL_COUNT 关",
                    color = PlatformerHubArt.gold.copy(alpha = 0.9f),
                    fontSize = layout.microSp,
                    maxLines = 1,
                )
            }
            if (canContinue) {
                PlatformerHubActionChip(
                    modifier = Modifier.weight(1f),
                    layout = layout,
                    gradient = PlatformerHubArt.continueGrad,
                    icon = {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(layout.dp(16.dp)),
                        )
                    },
                    title = "继续闯关",
                    subtitle = "第 $maxUnlockedLevel 关",
                    onClick = onContinue,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            PlatformerHubActionChip(
                modifier = Modifier.weight(1f),
                layout = layout,
                gradient = PlatformerHubArt.endlessGrad,
                icon = {
                    Text("♾", color = PlatformerHubArt.gold, fontSize = layout.bodySp, fontWeight = FontWeight.Bold)
                },
                title = "无尽跑酷",
                subtitle = "最佳 ${PlatformerEndlessRunner.bestTilesRun} 格",
                onClick = onStartEndless,
            )
            Row(
                modifier = Modifier
                    .weight(2.2f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(108.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)),
                    icon = { Text("🏃", fontSize = layout.bodySp) },
                    title = "神庙跑酷",
                    subtitle = "三车道",
                    onClick = onStartTempleRun,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(108.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                    icon = { Text("✈", fontSize = layout.bodySp) },
                    title = "天空射击",
                    subtitle = "plane",
                    onClick = onStartPlaneShooter,
                )
                PlatformerHubActionChip(
                    modifier = Modifier.width(layout.dp(108.dp)),
                    layout = layout,
                    gradient = listOf(Color(0xFF558B2F), Color(0xFF33691E)),
                    icon = { Text("🚗", fontSize = layout.bodySp) },
                    title = "登山挑战",
                    subtitle = "hillclimb",
                    onClick = onStartHillClimb,
                )
            }
            PlatformerHubActionChip(
                modifier = Modifier.weight(0.9f),
                layout = layout,
                gradient = if (testUnlockAll) {
                    listOf(Color(0xFF43A047), Color(0xFF2E7D32))
                } else {
                    PlatformerHubArt.unlockGrad
                },
                icon = {
                    Icon(
                        if (testUnlockAll) Icons.Default.LockOpen else Icons.Default.Lock,
                        null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(layout.dp(14.dp)),
                    )
                },
                title = if (testUnlockAll) "已全开" else "解锁全关",
                subtitle = "测试用",
                onClick = onUnlockAll,
            )
        }
    }
}

@Composable
private fun PlatformerHubActionChip(
    modifier: Modifier,
    layout: PlatformerHubLayoutSpec,
    gradient: List<Color>,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = layout.actionChipHeight),
        shape = RoundedCornerShape(layout.dp(10.dp)),
        color = Color.Transparent,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient), RoundedCornerShape(layout.dp(10.dp)))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(layout.dp(10.dp)))
                .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(26.dp))
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Column(
                Modifier
                    .padding(start = layout.dp(6.dp))
                    .weight(1f),
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = layout.bodySp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = layout.microSp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlatformerHubBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    PlatformerHubArt.duskTop,
                    PlatformerHubArt.duskMid,
                    PlatformerHubArt.duskWarm,
                    Color(0xFF3D2840),
                ),
            ),
        )
        drawWarmGlow(this, size.width * 0.82f, size.height * 0.92f, 280f, PlatformerHubArt.horizonGlow.copy(alpha = 0.22f))
        drawWarmGlow(this, size.width * 0.15f, size.height * 0.75f, 200f, PlatformerHubArt.meadow.copy(alpha = 0.12f))
    }
}

private fun drawWarmGlow(scope: DrawScope, cx: Float, cy: Float, radius: Float, color: Color) {
    scope.drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(cx, cy),
            radius = radius,
        ),
        radius = radius,
        center = Offset(cx, cy),
    )
}

@Composable
private fun PlatformerHubSidebar(
    modifier: Modifier,
    selectedCharacter: PlatformerCharacterId,
    onCharacterSelect: (PlatformerCharacterId) -> Unit,
    unlockedCount: Int,
    progress: Float,
    layout: PlatformerHubLayoutSpec,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.panelRadius))
            .background(
                Brush.verticalGradient(
                    listOf(PlatformerHubArt.panelTop, PlatformerHubArt.panelBottom),
                ),
            )
            .border(1.dp, PlatformerHubArt.panelBorder, RoundedCornerShape(layout.panelRadius))
            .padding(layout.panelPad)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PlatformerSectionLabel("闯关进度", layout)
        PlatformerGlassCard(layout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("已解锁", color = PlatformerHubArt.creamMuted, fontSize = layout.captionSp)
                Text(
                    "${(progress * 100).toInt()}%",
                    color = PlatformerHubArt.gold,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(layout.dp(5.dp)))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.dp(5.dp))
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(PlatformerHubArt.progressGrad)),
                )
            }
            Text(
                "$unlockedCount / $PLATFORMER_TOTAL_LEVEL_COUNT 关",
                color = PlatformerHubArt.creamMuted,
                fontSize = layout.microSp,
                modifier = Modifier.padding(top = layout.dp(4.dp)),
            )
        }

        PlatformerSectionLabel("出战角色", layout)
        Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
            PlatformerCharacterId.entries.forEach { char ->
                PlatformerCharacterRow(
                    char = char,
                    selected = selectedCharacter == char,
                    layout = layout,
                    onClick = { onCharacterSelect(char) },
                )
            }
        }
    }
}

@Composable
private fun PlatformerCharacterRow(
    char: PlatformerCharacterId,
    selected: Boolean,
    layout: PlatformerHubLayoutSpec,
    onClick: () -> Unit,
) {
    val chipColor = characterChipColor(char)
    val unlocked = PlatformerUnlockProgress.isCharacterUnlocked(char.catalogId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(
                if (selected) chipColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
            )
            .border(
                1.dp,
                if (selected) chipColor.copy(alpha = 0.65f) else Color.Transparent,
                RoundedCornerShape(layout.dp(8.dp)),
            )
            .clickable(enabled = unlocked, onClick = {
                PlatformerCharacterPrefetch.prefetchOnSelect(char)
                onClick()
            })
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(layout.dp(8.dp))
                .background(if (selected) chipColor else PlatformerHubArt.inkSoft, CircleShape),
        )
        Text(
            if (unlocked) char.displayTitle else "🔒 ${char.displayTitle}",
            modifier = Modifier.padding(start = layout.dp(6.dp)),
            color = if (unlocked) {
                if (selected) PlatformerHubArt.cream else PlatformerHubArt.creamMuted
            } else {
                PlatformerHubArt.creamMuted.copy(alpha = 0.45f)
            },
            fontSize = layout.captionSp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = (layout.captionSp.value * 1.25f).sp,
        )
    }
}

@Composable
private fun PlatformerGlassCard(
    layout: PlatformerHubLayoutSpec,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, PlatformerHubArt.panelBorderSoft, RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(8.dp)),
    ) {
        content()
    }
}

@Composable
private fun PlatformerSectionLabel(text: String, layout: PlatformerHubLayoutSpec) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(layout.dp(10.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(PlatformerHubArt.gold, PlatformerHubArt.goldDeep))),
        )
        Text(
            text,
            modifier = Modifier.padding(start = layout.dp(5.dp)),
            color = PlatformerHubArt.creamMuted,
            fontSize = layout.microSp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PlatformerChapterTabs(
    selected: PlatformerChapterFilter,
    onSelect: (PlatformerChapterFilter) -> Unit,
    layout: PlatformerHubLayoutSpec,
) {
    val compactTabs = layout.tier != PlatformerHubTier.Expanded || layout.areaWidth < 640.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        PlatformerChapterFilter.entries.forEach { filter ->
            val active = selected == filter
            val rangeLabel = filter.chipLabel(compactTabs)
            Box(
                modifier = Modifier
                    .heightIn(min = layout.dp(24.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) Brush.horizontalGradient(PlatformerHubArt.tabActiveGrad)
                        else Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.04f)),
                        ),
                    )
                    .border(
                        1.dp,
                        if (active) PlatformerHubArt.gold.copy(alpha = 0.55f) else PlatformerHubArt.panelBorderSoft,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(filter) }
                    .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(2.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    rangeLabel,
                    color = if (active) Color(0xFF3E2723) else PlatformerHubArt.creamMuted,
                    fontSize = layout.microSp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlatformerLevelTile(
    level: PlatformerLevelDef,
    unlocked: Boolean,
    isCurrent: Boolean,
    layout: PlatformerHubLayoutSpec,
    onClick: () -> Unit,
) {
    val accent = platformerThemeAccent(level.theme)
    val tileBg = if (unlocked) {
        Brush.horizontalGradient(
            listOf(accent.copy(alpha = 0.16f), PlatformerHubArt.cardBase, PlatformerHubArt.cardGlow),
        )
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF1E1A26), Color(0xFF181420)))
    }
    val borderBrush = when {
        isCurrent -> Brush.horizontalGradient(listOf(PlatformerHubArt.gold, PlatformerHubArt.goldDeep))
        unlocked -> Brush.horizontalGradient(listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.2f)))
        else -> Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent))
    }
    val captionLineHeight = (layout.captionSp.value * 1.3f).sp

    Surface(
        onClick = onClick,
        enabled = unlocked,
        modifier = Modifier
            .fillMaxWidth()
            .height(layout.tileHeight)
            .then(
                if (isCurrent) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(PlatformerHubArt.gold.copy(alpha = 0.2f), Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.5f),
                                radius = size.maxDimension,
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                        )
                    }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(layout.dp(10.dp)),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(tileBg)
                .border(1.dp, borderBrush, RoundedCornerShape(layout.dp(10.dp))),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = if (unlocked) 1f else 0.35f), accent.copy(alpha = 0.12f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        horizontal = layout.dp(8.dp),
                        vertical = layout.dp(7.dp),
                    ),
                verticalArrangement = Arrangement.spacedBy(layout.dp(3.dp)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        level.id.toString().padStart(2, '0'),
                        color = if (unlocked) accent else PlatformerHubArt.inkSoft,
                        fontWeight = FontWeight.Black,
                        fontSize = layout.titleSp,
                    )
                    if (!unlocked) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = PlatformerHubArt.inkSoft,
                            modifier = Modifier
                                .padding(start = layout.dp(3.dp))
                                .size(layout.dp(11.dp)),
                        )
                    }
                    if (isCurrent) {
                        Text(
                            " 进行中",
                            color = PlatformerHubArt.gold,
                            fontSize = layout.microSp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (level.enemySpawns.isNotEmpty()) {
                        Text(
                            "敌${level.enemySpawns.size}",
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFE57373).copy(alpha = 0.2f))
                                .padding(horizontal = layout.dp(4.dp), vertical = layout.dp(1.dp)),
                            color = Color(0xFFFFAB91),
                            fontSize = layout.microSp,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    level.title,
                    color = if (unlocked) PlatformerHubArt.cream else PlatformerHubArt.creamMuted.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = layout.bodySp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = captionLineHeight,
                )
                Text(
                    level.subtitle,
                    color = PlatformerHubArt.creamMuted.copy(alpha = 0.85f),
                    fontSize = layout.captionSp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = captionLineHeight,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
    }
}

private fun characterChipColor(char: PlatformerCharacterId): Color = when (char) {
    PlatformerCharacterId.CHICK_PRO_MAX -> Color(0xFFFFB300)
    PlatformerCharacterId.TREASURE_HUNTER -> Color(0xFF66BB6A)
    PlatformerCharacterId.PIXEL_WALKER -> Color(0xFF42A5F5)
    PlatformerCharacterId.TEMPLE_RUNNER -> Color(0xFFFF7043)
    PlatformerCharacterId.ADVENTURE_GIRL -> Color(0xFFEC407A)
    PlatformerCharacterId.NINJA_GIRL, PlatformerCharacterId.NINJA_BOY -> Color(0xFF78909C)
    PlatformerCharacterId.JACK -> Color(0xFF8D6E63)
    PlatformerCharacterId.RED_HAT -> Color(0xFFE53935)
    PlatformerCharacterId.ROBOT -> Color(0xFF90A4AE)
    PlatformerCharacterId.DINO -> Color(0xFF7CB342)
    PlatformerCharacterId.KNIGHT -> Color(0xFF5C6BC0)
    PlatformerCharacterId.SANTA -> Color(0xFFD32F2F)
    PlatformerCharacterId.SUPERTUX_TUX -> Color(0xFF0288D1)
    PlatformerCharacterId.CAT -> Color(0xFFFFB74D)
    PlatformerCharacterId.DOG -> Color(0xFFA1887F)
}

fun platformerThemeAccent(theme: PlatformerTheme): Color = when (theme) {
    PlatformerTheme.GRASS -> Color(0xFF7CB87C)
    PlatformerTheme.METAL -> Color(0xFFB0BEC5)
    PlatformerTheme.DESERT -> Color(0xFFFFAB40)
    PlatformerTheme.SPOOKY -> Color(0xFFCE93D8)
    PlatformerTheme.ICE -> Color(0xFF81D4FA)
    PlatformerTheme.FORTRESS -> Color(0xFFBCAAA4)
    PlatformerTheme.PACK_DESERT -> Color(0xFFFF8A65)
    PlatformerTheme.PACK_WINTER -> Color(0xFF64B5F6)
    PlatformerTheme.PACK_FOREST -> Color(0xFF66BB6A)
    PlatformerTheme.PACK_GRAVEYARD -> Color(0xFF9575CD)
    PlatformerTheme.PACK_JUNGLE -> Color(0xFF43A047)
    PlatformerTheme.PACK_SCIFI -> Color(0xFF4DD0E1)
    PlatformerTheme.PACK_GROTTO -> Color(0xFF90A4AE)
    PlatformerTheme.PACK_MINIMAL -> Color(0xFF64B5F6)
    PlatformerTheme.PACK_SUPERTUX -> Color(0xFF4FC3F7)
}
