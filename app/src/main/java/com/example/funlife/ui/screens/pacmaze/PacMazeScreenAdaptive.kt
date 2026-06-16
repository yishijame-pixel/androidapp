package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** 大厅布局档位：由内容区宽高自动判定。 */
enum class PacMazeHubLayoutTier {
    /** 矮/窄屏：单列滚动、紧凑字号 */
    Compact,
    /** 常见横屏手机 */
    Standard,
    /** 大屏 / 平板横屏 */
    Expanded,
}

enum class PacMazeModeSelectLayout {
    /** 参考图：上二下一网格 */
    Grid,
    /** 卡片纵向滚动，保证全部可见 */
    Scroll,
}

/**
 * 大厅/Hub 自适应规格。
 *
 * - 屏幕级： [PacMazeHubLayoutSpec.computeScreen]（含顶栏扣减）
 * - 面板级： [PacMazeHubLayoutSpec.computeContent]（直接用面板 [BoxWithConstraints] 的 max 宽高）
 */
data class PacMazeHubLayoutSpec(
    val scale: Float,
    val isWide: Boolean,
    val tier: PacMazeHubLayoutTier,
    val contentAreaWidth: Dp,
    val contentAreaHeight: Dp,
    val heroWeight: Float,
    val contentWeight: Float,
    val horizontalPad: Dp,
    val panelPad: Dp,
    val gap: Dp,
    val panelRadius: Dp,
    val heroBadgeSize: Dp,
    val titleSp: TextUnit,
    val subtitleSp: TextUnit,
    val bodySp: TextUnit,
    val captionSp: TextUnit,
    val buttonSp: TextUnit,
    val cardPad: Dp,
    val cardRadius: Dp,
    val playIconLarge: Dp,
    val playIconSmall: Dp,
    val featuredWeight: Float,
    val secondaryColumnWeight: Float,
    val topBarTitleSp: TextUnit,
    val topBarChipValueSp: TextUnit,
    val useCompactTopBarChips: Boolean,
    val modeSelectLayout: PacMazeModeSelectLayout,
) {
    val isCompactHeight: Boolean get() = tier == PacMazeHubLayoutTier.Compact
    val isVeryCompactHeight: Boolean get() = tier == PacMazeHubLayoutTier.Compact && contentAreaHeight < 200.dp
    val modeSelectStacked: Boolean get() = modeSelectLayout == PacMazeModeSelectLayout.Scroll
    @Deprecated("Use modeSelectLayout", ReplaceWith("modeSelectLayout"))
    val useScrollableHubContent: Boolean get() = false

    fun dp(base: Dp): Dp = base * scale

    fun characterCardWidth(contentWidth: Dp, visibleCards: Int = 4): Dp {
        if (visibleCards <= 0) return 96.dp * scale
        val gaps = gap * (visibleCards - 1)
        val raw = (contentWidth - gaps) / visibleCards
        return raw.coerceIn(84.dp * scale, 128.dp * scale)
    }

    val characterPreviewW: Dp get() = dp(if (isCompactHeight) 114.dp else 142.dp)
    val characterPreviewH: Dp get() = dp(if (isCompactHeight) 102.dp else 128.dp)

    /** 闯关侧栏：按内容区宽度 22–26%，限制在可读区间。 */
    val levelSidebarWidth: Dp
        get() {
            val fraction = when (tier) {
                PacMazeHubLayoutTier.Compact -> 0.26f
                PacMazeHubLayoutTier.Standard -> 0.24f
                PacMazeHubLayoutTier.Expanded -> 0.22f
            }
            return (contentAreaWidth * fraction).coerceIn(dp(112.dp), dp(156.dp))
        }

    companion object {
        val Default = computeContent(maxWidth = 640.dp, maxHeight = 300.dp)

        /** 全屏约束（Hub 顶栏外层） */
        fun computeScreen(maxWidth: Dp, maxHeight: Dp): PacMazeHubLayoutSpec {
            val scale = scaleFrom(maxWidth, maxHeight)
            val topBar = 40.dp * scale
            val pad = 10.dp * scale
            return computeContent(
                maxWidth = maxWidth,
                maxHeight = (maxHeight - topBar - pad).coerceAtLeast(140.dp),
                scaleHint = scale,
            )
        }

        /** 内容面板约束（已扣 panel 内边距后的可用区域） */
        fun computeContent(
            maxWidth: Dp,
            maxHeight: Dp,
            scaleHint: Float? = null,
        ): PacMazeHubLayoutSpec {
            val scale = scaleHint ?: scaleFrom(maxWidth, maxHeight)
            val isWide = maxWidth > maxHeight * 1.12f
            val w = maxWidth
            val h = maxHeight

            val tier = when {
                !isWide -> PacMazeHubLayoutTier.Compact
                h < 195.dp || w < 300.dp -> PacMazeHubLayoutTier.Compact
                h >= 285.dp && w >= 520.dp -> PacMazeHubLayoutTier.Expanded
                else -> PacMazeHubLayoutTier.Standard
            }

            val isCompact = tier == PacMazeHubLayoutTier.Compact
            val isTabletWide = w >= 880.dp

            val heroWeight = when {
                isTabletWide -> 0.22f
                tier == PacMazeHubLayoutTier.Compact -> 0.25f
                tier == PacMazeHubLayoutTier.Standard -> 0.27f
                else -> 0.28f
            }

            val modeSelectLayout = when {
                tier == PacMazeHubLayoutTier.Compact -> PacMazeModeSelectLayout.Scroll
                h < 255.dp -> PacMazeModeSelectLayout.Scroll
                w < 420.dp -> PacMazeModeSelectLayout.Scroll
                else -> PacMazeModeSelectLayout.Grid
            }

            return PacMazeHubLayoutSpec(
                scale = scale,
                isWide = isWide,
                tier = tier,
                contentAreaWidth = w,
                contentAreaHeight = h,
                heroWeight = heroWeight,
                contentWeight = 1f - heroWeight,
                horizontalPad = (if (isWide) 14.dp else 8.dp) * scale,
                panelPad = (if (isCompact) 5.dp else 8.dp) * scale,
                gap = (if (isCompact) 5.dp else 7.dp) * scale,
                panelRadius = 16.dp * scale,
                heroBadgeSize = (when (tier) {
                    PacMazeHubLayoutTier.Compact -> 52.dp
                    PacMazeHubLayoutTier.Standard -> 64.dp
                    PacMazeHubLayoutTier.Expanded -> 72.dp
                }) * scale,
                titleSp = (when (tier) {
                    PacMazeHubLayoutTier.Compact -> 16f
                    PacMazeHubLayoutTier.Standard -> 18f
                    PacMazeHubLayoutTier.Expanded -> 20f
                } * scale).sp,
                subtitleSp = (11f * scale).sp,
                bodySp = (12f * scale).sp,
                captionSp = (10f * scale).coerceAtLeast(9f).sp,
                buttonSp = (14f * scale).sp,
                cardPad = (if (isCompact) 7.dp else 10.dp) * scale,
                cardRadius = (if (isCompact) 12.dp else 15.dp) * scale,
                playIconLarge = (if (isCompact) 36.dp else 42.dp) * scale,
                playIconSmall = (if (isCompact) 26.dp else 30.dp) * scale,
                featuredWeight = if (isCompact) 1.15f else 1.28f,
                secondaryColumnWeight = if (isCompact) 1f else 0.92f,
                topBarTitleSp = ((if (isWide) 18f else 16f) * scale).sp,
                topBarChipValueSp = (11f * scale).coerceAtLeast(9f).sp,
                useCompactTopBarChips = isCompact || w < 680.dp,
                modeSelectLayout = modeSelectLayout,
            )
        }

        private fun scaleFrom(maxWidth: Dp, maxHeight: Dp): Float {
            val minSide = min(maxWidth.value, maxHeight.value)
            return (minSide / 360f).coerceIn(0.66f, 1.28f)
        }

        @Deprecated("Use computeScreen or computeContent", ReplaceWith("computeScreen(maxWidth, maxHeight)"))
        fun compute(maxWidth: Dp, maxHeight: Dp): PacMazeHubLayoutSpec = computeScreen(maxWidth, maxHeight)
    }
}

/** 对局横屏自适应规格。 */
data class PacMazePlayLayoutSpec(
    val scale: Float,
    val isCompactHeight: Boolean,
    val hudHeight: Dp,
    val hudHorizontal: Dp,
    val hudTop: Dp,
    val hudIconSize: Dp,
    val hudBarRadius: Dp,
    val joystickSize: Dp,
    val joystickZoneWidth: Dp,
    val joystickZoneHeight: Dp,
    val joystickStart: Dp,
    val joystickBottom: Dp,
    val actionEnd: Dp,
    val actionBottom: Dp,
    val statSp: TextUnit,
    val modeBadgeSp: TextUnit,
) {
    fun dp(base: Dp): Dp = base * scale

    val mapInsetTop: Dp get() = 0.dp
    val mapInsetBottom: Dp get() = 0.dp
    val mapInsetHorizontal: Dp get() = 0.dp

    companion object {
        val Default = compute(maxWidth = 800.dp, maxHeight = 360.dp)

        fun compute(maxWidth: Dp, maxHeight: Dp): PacMazePlayLayoutSpec {
            val minSide = min(maxWidth.value, maxHeight.value)
            val scale = (minSide / 360f).coerceIn(0.72f, 1.18f)
            val compact = maxHeight < 320.dp
            val veryWide = maxWidth > maxHeight * 1.22f
            return PacMazePlayLayoutSpec(
                scale = scale,
                isCompactHeight = compact,
                hudHeight = (if (compact) 32.dp else 36.dp) * scale,
                hudHorizontal = (if (veryWide) 10.dp else 8.dp) * scale,
                hudTop = (if (compact) 4.dp else 6.dp) * scale,
                hudIconSize = (if (compact) 28.dp else 30.dp) * scale,
                hudBarRadius = 14.dp * scale,
                joystickSize = (if (compact) 114.dp else 128.dp) * scale,
                joystickZoneWidth = (if (compact) 138.dp else 156.dp) * scale,
                joystickZoneHeight = (if (compact) 138.dp else 156.dp) * scale,
                joystickStart = (if (veryWide) 14.dp else 12.dp) * scale,
                joystickBottom = (if (compact) 10.dp else 12.dp) * scale,
                actionEnd = (if (veryWide) 14.dp else 12.dp) * scale,
                actionBottom = (if (compact) 10.dp else 12.dp) * scale,
                statSp = (10f * scale).coerceAtLeast(8.5f).sp,
                modeBadgeSp = (10f * scale).coerceAtLeast(9f).sp,
            )
        }
    }
}

val LocalPacMazeHubLayout = compositionLocalOf { PacMazeHubLayoutSpec.Default }
val LocalPacMazePlayLayout = compositionLocalOf { PacMazePlayLayoutSpec.Default }

@Composable
fun currentPacMazeHubLayout(): PacMazeHubLayoutSpec = LocalPacMazeHubLayout.current

@Composable
fun currentPacMazePlayLayout(): PacMazePlayLayoutSpec = LocalPacMazePlayLayout.current

/**
 * 在父级已分配的面板区域内重新测量并注入 [PacMazeHubLayoutSpec]。
 * 所有 Hub 子面板应用此包裹，确保不同手机按**实际可用宽高**布局。
 */
@Composable
fun PacMazeAdaptiveHubPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layout = PacMazeHubLayoutSpec.computeContent(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )
        CompositionLocalProvider(LocalPacMazeHubLayout provides layout) {
            content()
        }
    }
}
