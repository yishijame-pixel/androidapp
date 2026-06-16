package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.ui.screens.pacmaze.pacMazeUiClick
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.ui.screens.pacmaze.PacMazeEndlessWaveInfo
import com.example.funlife.ui.screens.pacmaze.PacMazeEndlessSegment
import com.example.funlife.ui.screens.pacmaze.PacMazeEndlessWaveUi
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.currentPacMazePlayLayout
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeAccent
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_MAP_SCALE_DEFAULT
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_MAP_SCALE_MAX
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_MAP_SCALE_MIN
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_MAP_SCALE_STEP
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MAX
import com.example.funlife.ui.screens.pacmaze.components.PAC_MAZE_PLAYER_SCALE_MIN
import kotlin.math.roundToInt

/**
 * 横屏对局顶栏：左侧紧凑胶囊 + 可选右侧缩放，不铺满全屏宽度。
 */
@Composable
fun PacMazePlayHudBar(
    runMode: PacMazeRunMode,
    levelId: Int,
    endlessWave: Int,
    maxLevelReached: Int = 1,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    timeLimitSeconds: Int = 0,
    mazeBadgeLabel: String? = null,
    attackCharges: Int,
    powerTicksLeft: Int,
    themeId: PacMazeMapThemeId,
    onBack: () -> Unit,
    playerDrawScale: Float,
    onPlayerDrawScaleChange: (Float) -> Unit,
    movementMode: PacMazeMovementMode = PacMazeMovementMode.AUTO,
    onMovementModeChange: (PacMazeMovementMode) -> Unit = {},
    mapWidthScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    mapHeightScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    onMapWidthScaleChange: (Float) -> Unit = {},
    onMapHeightScaleChange: (Float) -> Unit = {},
    panelsExpanded: Boolean = true,
    onPanelsExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val play = currentPacMazePlayLayout()
    val accent = pacMazeThemeAccent(themeId)
    val modeAccentColor = modeAccent(runMode)
    val context = LocalContext.current
    val endlessInfo = if (runMode == PacMazeRunMode.ENDLESS && endlessWave > 0) {
        PacMazeEndlessWaveUi.resolve(endlessWave, maxLevelReached)
    } else {
        null
    }
    val iconSize = play.hudIconSize
    val iconInner = play.dp(13.dp)
    val capsuleRadius = play.hudBarRadius
    val timeText = if (runMode == PacMazeRunMode.MAZE && timeLimitSeconds > 0) {
        val remaining = (timeLimitSeconds - elapsedSeconds).coerceAtLeast(0)
        "$remaining/${timeLimitSeconds}s"
    } else {
        "%d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
    }
    val timeColor = when {
        runMode != PacMazeRunMode.MAZE || timeLimitSeconds <= 0 -> PacMazePalette.modeMaze
        (timeLimitSeconds - elapsedSeconds) <= 10 -> Color(0xFFFF5252)
        (timeLimitSeconds - elapsedSeconds) <= 30 -> PacMazePalette.accentOrange
        else -> PacMazePalette.modeMaze
    }
    Row(
        modifier = modifier.height(play.hudHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(play.dp(4.dp)),
    ) {
        IconButton(
            onClick = pacMazeUiClick(context, PacMazeUiSoundId.NavigateBack, onClick = onBack),
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(iconInner),
            )
        }

        if (panelsExpanded) {
            Row(
                modifier = Modifier
                    .height(play.hudHeight)
                    .widthIn(max = play.dp(330.dp))
                    .clip(RoundedCornerShape(capsuleRadius))
                    .background(
                        Brush.horizontalGradient(hudBarColors(themeId, runMode)),
                    )
                    .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(capsuleRadius))
                    .padding(horizontal = play.dp(6.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(play.dp(6.dp)),
            ) {
                ModeBadge(
                    runMode = runMode,
                    levelId = levelId,
                    endlessInfo = endlessInfo,
                    mazeBadgeLabel = mazeBadgeLabel,
                    accent = modeAccentColor,
                    fontSize = play.modeBadgeSp,
                )

                HudCapsuleDivider()

                when (runMode) {
                    PacMazeRunMode.MAZE -> {
                        HudInlineStat("⏱", timeText, timeColor, play.statSp, emphasized = true)
                        HudCapsuleDivider()
                        HudInlineStat("🏆", score.toString(), PacMazePalette.accentGold, play.statSp)
                        HudCapsuleDivider()
                        HudInlineStat("❤", lives.coerceAtLeast(0).toString(), Color(0xFFFF8A80), play.statSp, emojiScale = 1.02f)
                    }
                    else -> {
                        HudInlineStat("🏆", score.toString(), PacMazePalette.accentGold, play.statSp)
                        HudCapsuleDivider()
                        HudInlineStat("❤", lives.coerceAtLeast(0).toString(), Color(0xFFFF8A80), play.statSp, emojiScale = 1.02f)
                        if (!play.isCompactHeight) {
                            HudCapsuleDivider()
                            HudInlineStat("⏱", timeText, PacMazePalette.inkSecondary, play.statSp)
                        }
                    }
                }

                if (attackCharges > 0) {
                    HudCapsuleDivider()
                    HudInlineStat("⚔", attackCharges.toString(), PacMazePalette.accentOrange, play.statSp)
                }
                if (powerTicksLeft > 0) {
                    HudCapsuleDivider()
                    HudInlineStat("✦", "ON", PacMazePalette.accentMint, play.statSp)
                }
            }
        }

        PacMazeHudPanelToggle(
            expanded = panelsExpanded,
            onToggle = { onPanelsExpandedChange(!panelsExpanded) },
            size = if (panelsExpanded) play.dp(20.dp) else play.dp(16.dp),
            iconSize = if (panelsExpanded) play.dp(12.dp) else play.dp(9.dp),
            compact = !panelsExpanded,
        )

        if (panelsExpanded && !play.isCompactHeight) {
            Spacer(modifier = Modifier.width(play.dp(2.dp)))
            CompactAxisScaleControl(
                label = "角色",
                scale = playerDrawScale,
                onScaleChange = onPlayerDrawScaleChange,
                accent = accent,
                height = play.hudHeight,
                buttonSize = play.dp(20.dp),
                iconSize = play.dp(11.dp),
                fontSize = (8f * play.scale).sp,
                radius = play.dp(8.dp),
                min = PAC_MAZE_PLAYER_SCALE_MIN,
                max = PAC_MAZE_PLAYER_SCALE_MAX,
                step = 0.1f,
            )
            CompactMovementModeToggle(
                mode = movementMode,
                onModeChange = onMovementModeChange,
                accent = accent,
                height = play.hudHeight,
                fontSize = (8f * play.scale).sp,
                radius = play.dp(8.dp),
            )
            CompactAxisScaleControl(
                label = "宽",
                scale = mapWidthScale,
                onScaleChange = onMapWidthScaleChange,
                accent = accent.copy(alpha = 0.92f),
                height = play.hudHeight,
                buttonSize = play.dp(20.dp),
                iconSize = play.dp(11.dp),
                fontSize = (8f * play.scale).sp,
                radius = play.dp(8.dp),
                min = PAC_MAZE_MAP_SCALE_MIN,
                max = PAC_MAZE_MAP_SCALE_MAX,
                step = PAC_MAZE_MAP_SCALE_STEP,
            )
            CompactAxisScaleControl(
                label = "高",
                scale = mapHeightScale,
                onScaleChange = onMapHeightScaleChange,
                accent = accent.copy(alpha = 0.92f),
                height = play.hudHeight,
                buttonSize = play.dp(20.dp),
                iconSize = play.dp(11.dp),
                fontSize = (8f * play.scale).sp,
                radius = play.dp(8.dp),
                min = PAC_MAZE_MAP_SCALE_MIN,
                max = PAC_MAZE_MAP_SCALE_MAX,
                step = PAC_MAZE_MAP_SCALE_STEP,
            )
        }
    }
}

/** @deprecated 使用 [PacMazePlayHudBar] */
@Composable
fun PacMazeFloatingPlayHud(
    runMode: PacMazeRunMode,
    levelId: Int,
    endlessWave: Int,
    maxLevelReached: Int = 1,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    timeLimitSeconds: Int = 0,
    mazeBadgeLabel: String? = null,
    attackCharges: Int,
    powerTicksLeft: Int,
    themeId: PacMazeMapThemeId,
    onBack: () -> Unit,
    playerDrawScale: Float,
    onPlayerDrawScaleChange: (Float) -> Unit,
    movementMode: PacMazeMovementMode = PacMazeMovementMode.AUTO,
    onMovementModeChange: (PacMazeMovementMode) -> Unit = {},
    mapWidthScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    mapHeightScale: Float = PAC_MAZE_MAP_SCALE_DEFAULT,
    onMapWidthScaleChange: (Float) -> Unit = {},
    onMapHeightScaleChange: (Float) -> Unit = {},
    panelsExpanded: Boolean = true,
    onPanelsExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PacMazePlayHudBar(
        runMode = runMode,
        levelId = levelId,
        endlessWave = endlessWave,
        maxLevelReached = maxLevelReached,
        score = score,
        lives = lives,
        elapsedSeconds = elapsedSeconds,
        timeLimitSeconds = timeLimitSeconds,
        mazeBadgeLabel = mazeBadgeLabel,
        attackCharges = attackCharges,
        powerTicksLeft = powerTicksLeft,
        themeId = themeId,
        onBack = onBack,
        playerDrawScale = playerDrawScale,
        onPlayerDrawScaleChange = onPlayerDrawScaleChange,
        movementMode = movementMode,
        onMovementModeChange = onMovementModeChange,
        mapWidthScale = mapWidthScale,
        mapHeightScale = mapHeightScale,
        onMapWidthScaleChange = onMapWidthScaleChange,
        onMapHeightScaleChange = onMapHeightScaleChange,
        panelsExpanded = panelsExpanded,
        onPanelsExpandedChange = onPanelsExpandedChange,
        modifier = modifier,
    )
}

@Composable
fun PacMazeHudPanelToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bgAlpha = if (compact) 0.18f else 0.36f
    val borderAlpha = if (compact) 0.08f else 0.12f
    val iconAlpha = if (compact) 0.55f else 0.82f
    IconButton(
        onClick = pacMazeUiClick(context, PacMazeUiSoundId.Toggle, onClick = onToggle),
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = bgAlpha))
            .border(0.5.dp, Color.White.copy(alpha = borderAlpha), CircleShape),
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "收起面板" else "展开面板",
            tint = Color.White.copy(alpha = iconAlpha),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun HudCapsuleDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight(0.5f)
            .width(1.dp)
            .background(Color.White.copy(alpha = 0.18f)),
    )
}

@Composable
private fun HudInlineStat(
    emoji: String,
    value: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    emphasized: Boolean = false,
    compact: Boolean = false,
    emojiScale: Float? = null,
) {
    val resolvedEmojiScale = emojiScale ?: if (compact) 0.78f else 0.92f
    val valueScale = if (emphasized) 1.06f else 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp),
    ) {
        Text(emoji, fontSize = (fontSize.value * resolvedEmojiScale).sp)
        Text(
            value,
            color = color,
            fontSize = (fontSize.value * valueScale).sp,
            fontWeight = if (compact) FontWeight.SemiBold else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun PacMazeModeHintBanner(
    runMode: PacMazeRunMode,
    endlessWave: Int,
    maxLevelReached: Int = 1,
    modifier: Modifier = Modifier,
) {
    val play = currentPacMazePlayLayout()
    val (text, accent) = when (runMode) {
        PacMazeRunMode.ENDLESS -> {
            val info = PacMazeEndlessWaveUi.resolve(endlessWave, maxLevelReached)
            val color = when (info.segment) {
                PacMazeEndlessSegment.MOLTEN -> Color(0xFFFF6D00)
                PacMazeEndlessSegment.PREHEAT -> Color(0xFFFFB74D)
                PacMazeEndlessSegment.CHUNK -> PacMazePalette.modeEndless
            }
            PacMazeEndlessWaveUi.hintText(info) to color
        }
        PacMazeRunMode.MAZE -> "集齐钥印 · 迷雾探索 · 雷达 12s 冷却" to PacMazePalette.modeMaze
        PacMazeRunMode.PRACTICE -> "练习 · 无限命" to PacMazePalette.modePractice
        else -> return
    }
    Box(
        modifier = modifier
            .widthIn(max = play.dp(360.dp))
            .clip(RoundedCornerShape(play.dp(999.dp)))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(play.dp(999.dp)))
            .padding(horizontal = play.dp(10.dp), vertical = play.dp(2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = play.statSp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PacMazeEndlessWaveBanner(
    wave: Int,
    maxLevelReached: Int = 1,
    modifier: Modifier = Modifier,
) {
    val info = PacMazeEndlessWaveUi.resolve(wave, maxLevelReached)
    val (gradient, borderColor) = when (info.segment) {
        PacMazeEndlessSegment.MOLTEN -> listOf(Color(0xFF7F0000), Color(0xFFFF6D00)) to Color(0xFFFFAB40)
        PacMazeEndlessSegment.PREHEAT -> listOf(Color(0xFF4A148C), Color(0xFFFF8F00)) to Color(0xFFFFCC80)
        PacMazeEndlessSegment.CHUNK -> listOf(Color(0xFF4A148C), Color(0xFF7C4DFF)) to Color.White.copy(alpha = 0.35f)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(gradient))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                PacMazeEndlessWaveUi.bannerTitle(info),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                PacMazeEndlessWaveUi.bannerSubtitle(info),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModeBadge(
    runMode: PacMazeRunMode,
    levelId: Int,
    endlessInfo: PacMazeEndlessWaveInfo?,
    mazeBadgeLabel: String? = null,
    accent: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    val label = when (runMode) {
        PacMazeRunMode.ENDLESS -> endlessInfo?.let { PacMazeEndlessWaveUi.badgeLabel(it) } ?: "W?"
        PacMazeRunMode.MAZE -> mazeBadgeLabel ?: "迷宫"
        PacMazeRunMode.PRACTICE -> "练"
        PacMazeRunMode.CAMPAIGN -> "L$levelId"
    }
    val badgeColor = when (endlessInfo?.segment) {
        PacMazeEndlessSegment.MOLTEN -> Color(0xFFFFAB40)
        PacMazeEndlessSegment.PREHEAT -> Color(0xFFFFCC80)
        else -> accent
    }
    Text(
        label,
        color = badgeColor,
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        fontFamily = if (runMode == PacMazeRunMode.ENDLESS || runMode == PacMazeRunMode.CAMPAIGN) {
            FontFamily.Monospace
        } else {
            FontFamily.Default
        },
    )
}

@Composable
fun CompactAxisScaleControl(
    label: String,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    accent: Color,
    height: androidx.compose.ui.unit.Dp,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    radius: androidx.compose.ui.unit.Dp,
    min: Float,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(radius)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = (fontSize.value * 0.85f).sp,
            modifier = Modifier.padding(start = 6.dp),
        )
        IconButton(
            onClick = {
                val next = (scale - step).coerceIn(min, max)
                val steps = (1f / step).toInt().coerceAtLeast(1)
                onScaleChange((next * steps).roundToInt() / steps.toFloat())
            },
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(Icons.Filled.Remove, contentDescription = "缩小", tint = Color.White, modifier = Modifier.size(iconSize))
        }
        Text(
            "${(scale * 100).toInt()}",
            color = accent,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(IntrinsicSize.Min),
        )
        IconButton(
            onClick = {
                val next = (scale + step).coerceIn(min, max)
                val steps = (1f / step).toInt().coerceAtLeast(1)
                onScaleChange((next * steps).roundToInt() / steps.toFloat())
            },
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "放大", tint = Color.White, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun CompactMovementModeToggle(
    mode: PacMazeMovementMode,
    onModeChange: (PacMazeMovementMode) -> Unit,
    accent: Color,
    height: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    radius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = when (mode) {
        PacMazeMovementMode.AUTO -> "滑行"
        PacMazeMovementMode.MANUAL -> "手控"
    }
    val nextMode = if (mode == PacMazeMovementMode.AUTO) PacMazeMovementMode.MANUAL else PacMazeMovementMode.AUTO
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(radius))
            .clickable(onClick = pacMazeUiClick(context, PacMazeUiSoundId.ChipAction) {
                onModeChange(nextMode)
            })
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "控",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = (fontSize.value * 0.85f).sp,
        )
        Text(
            label,
            color = accent,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun modeAccent(runMode: PacMazeRunMode): Color = when (runMode) {
    PacMazeRunMode.ENDLESS -> PacMazePalette.modeEndless
    PacMazeRunMode.MAZE -> PacMazePalette.modeMaze
    PacMazeRunMode.PRACTICE -> PacMazePalette.modePractice
    PacMazeRunMode.CAMPAIGN -> PacMazePalette.accentCyan
}

@Composable
fun PacMazePlayLoadingOverlay(
    message: String,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(if (compact) 0.45f else 1f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp),
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color(0xFFFF6E40),
                modifier = Modifier.size(if (compact) 36.dp else 48.dp),
            )
            androidx.compose.material3.Text(
                message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = if (compact) 13.sp else 15.sp,
            )
        }
    }
}

private fun hudBarColors(themeId: PacMazeMapThemeId, runMode: PacMazeRunMode): List<Color> {
    if (runMode == PacMazeRunMode.ENDLESS) {
        return listOf(Color(0xE6281248), Color(0xCC140820))
    }
    if (runMode == PacMazeRunMode.MAZE) {
        return listOf(Color(0xE6282834), Color(0xCC14121A))
    }
    return when (themeId) {
        PacMazeMapThemeId.CYBERPUNK, PacMazeMapThemeId.ENDLESS ->
            listOf(Color(0xE61A1030), Color(0xCC0A0818))
        PacMazeMapThemeId.GARDEN ->
            listOf(Color(0xE62E5E3A), Color(0xCC1E3D28))
        PacMazeMapThemeId.FOOD ->
            listOf(Color(0xE66D3A2E), Color(0xCC4A2418))
        PacMazeMapThemeId.CHINESE ->
            listOf(Color(0xE65C3D1E), Color(0xCC3D2810))
        PacMazeMapThemeId.MAZE, PacMazeMapThemeId.MIRROR ->
            listOf(Color(0xE6282834), Color(0xCC14121A))
        PacMazeMapThemeId.STEAMPUNK, PacMazeMapThemeId.MAGMA, PacMazeMapThemeId.OPERA ->
            listOf(Color(0xE64E2A18), Color(0xCC301808))
        PacMazeMapThemeId.SUBMARINE, PacMazeMapThemeId.ORBITAL, PacMazeMapThemeId.FROST ->
            listOf(Color(0xE61A3858), Color(0xCC0C2038))
        PacMazeMapThemeId.VHS, PacMazeMapThemeId.CHRONO ->
            listOf(Color(0xE6281848), Color(0xCC140820))
        PacMazeMapThemeId.ARCHIVE, PacMazeMapThemeId.METRO, PacMazeMapThemeId.GREENHOUSE ->
            listOf(Color(0xE6384028), Color(0xCC202818))
        else -> listOf(Color(0xE6243047), Color(0xCC121828))
    }
}
