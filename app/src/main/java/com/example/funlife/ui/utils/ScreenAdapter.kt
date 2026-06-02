package com.example.funlife.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * 🔥 全局屏幕适配器
 * 让所有页面、动画在不同型号手机（小屏 5"、主流 6.5"、平板 8"+）上自适应。
 *
 * 设计基线：宽 375dp（iPhone 标准 / 普通 6.1" 安卓机）。
 * 实际宽度按比例缩放，但限制在 [0.85, 1.25] 之间，避免超大或超小屏极端缩放。
 */

/** 设备尺寸级别 */
enum class ScreenSizeClass {
    /** 紧凑 (<600dp) - 大部分手机 */
    COMPACT,
    /** 中等 (600-840dp) - 大屏手机/小平板 */
    MEDIUM,
    /** 扩展 (>840dp) - 平板/折叠屏展开 */
    EXPANDED
}

/** 设备方向 */
enum class ScreenOrientation { PORTRAIT, LANDSCAPE }

/**
 * 适配器数据类，提供常用响应式数值与工具函数。
 *
 * 企业级特性：
 * - 字号自适应分离（safeScale 不直接缩 sp，避免和系统字体设置叠加放大）
 * - 提供 sizeClass 快捷判断（isCompact/isMedium/isExpanded）
 * - 提供 fontScale 感知（系统大字号用户不再被强制缩放）
 * - 提供方向（portrait/landscape）感知，便于横屏布局切换
 */
data class ScreenAdapter(
    val widthDp: Float,
    val heightDp: Float,
    val sizeClass: ScreenSizeClass,
    val orientation: ScreenOrientation,
    /** 相对设计基线 (375dp) 的缩放因子 */
    val scale: Float,
    /** 真正用于 UI 缩放的安全因子，被 clamp 在合理区间 */
    val safeScale: Float,
    /** 系统字体缩放（用户在系统设置里调大字号会反映在这里） */
    val fontScale: Float,
    /** 是否短屏（高度 <640dp，比如老款小屏机） */
    val isShortScreen: Boolean,
    /** 是否大屏（宽 >=600dp） */
    val isLargeScreen: Boolean
) {
    val isCompact: Boolean get() = sizeClass == ScreenSizeClass.COMPACT
    val isMedium: Boolean get() = sizeClass == ScreenSizeClass.MEDIUM
    val isExpanded: Boolean get() = sizeClass == ScreenSizeClass.EXPANDED
    val isPortrait: Boolean get() = orientation == ScreenOrientation.PORTRAIT
    val isLandscape: Boolean get() = orientation == ScreenOrientation.LANDSCAPE

    /**
     * 缩放 dp 值。例如 `adapter.dp(20)` 在大屏上得到约 22dp，在小屏上约 17dp。
     * 用于：固定尺寸卡片、按钮、icon、动画 offset 等。
     */
    fun dp(value: Float): Dp = (value * safeScale).dp
    fun dp(value: Int): Dp = (value * safeScale).dp

    /**
     * 缩放 sp 值。仅保守缩放，避免和系统 fontScale 叠加。
     * 公式 = base * (1 + (safeScale - 1) * 0.5)
     */
    fun sp(value: Float): TextUnit = (value * (1f + (safeScale - 1f) * 0.5f)).sp
    fun sp(value: Int): TextUnit = (value * (1f + (safeScale - 1f) * 0.5f)).sp

    /** 给底部 Tab 屏的滚动列表用：bottom padding = 底导航 90dp + 30dp 呼吸 */
    fun bottomTabPadding(extra: Dp = 30.dp): Dp = 90.dp + extra

    /**
     * 给 Dialog/弹窗用：返回最大允许宽度（屏宽 90% 但不超过 480dp，平板友好）。
     */
    val dialogMaxWidth: Dp
        get() = (widthDp * 0.92f).dp.coerceAtMost(480.dp)

    /**
     * 给 Dialog 用：最大高度 = 屏高 85%。
     */
    val dialogMaxHeight: Dp
        get() = (heightDp * 0.85f).dp

    /**
     * 选择两个值之一：紧凑屏返回 compact，否则返回 regular。常用于布局列数。
     * 例：`val cols = sa.choose(2, 4)` —— 手机 2 列，平板 4 列
     */
    fun <T> choose(compact: T, regular: T): T = if (isCompact) compact else regular
}

/** 全局 CompositionLocal，无需层层传递 */
val LocalScreenAdapter = compositionLocalOf<ScreenAdapter> {
    // 默认值（在 Provider 之外取用时使用）
    ScreenAdapter(
        widthDp = 375f,
        heightDp = 812f,
        sizeClass = ScreenSizeClass.COMPACT,
        orientation = ScreenOrientation.PORTRAIT,
        scale = 1f,
        safeScale = 1f,
        fontScale = 1f,
        isShortScreen = false,
        isLargeScreen = false
    )
}

/**
 * 在 Composition 中获取当前 ScreenAdapter。
 * 用法：`val sa = rememberScreenAdapter()`
 */
@Composable
fun rememberScreenAdapter(): ScreenAdapter {
    val cfg = LocalConfiguration.current
    return remember(cfg.screenWidthDp, cfg.screenHeightDp, cfg.orientation, cfg.fontScale) {
        val w = cfg.screenWidthDp.toFloat()
        val h = cfg.screenHeightDp.toFloat()
        val baseline = 375f
        val rawScale = w / baseline
        val safe = rawScale.coerceIn(0.85f, 1.25f)
        val sizeClass = when {
            w < 600f -> ScreenSizeClass.COMPACT
            w < 840f -> ScreenSizeClass.MEDIUM
            else -> ScreenSizeClass.EXPANDED
        }
        val orient = if (cfg.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
            ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT
        ScreenAdapter(
            widthDp = w,
            heightDp = h,
            sizeClass = sizeClass,
            orientation = orient,
            scale = rawScale,
            safeScale = safe,
            fontScale = cfg.fontScale,
            isShortScreen = h < 640f,
            isLargeScreen = w >= 600f
        )
    }
}

/**
 * 简便扩展：在任意 Composable 中直接调 `20.rdp` 得到响应式 dp。
 * （rdp = responsive dp）
 */
val Int.rdp: Dp
    @Composable
    @ReadOnlyComposable
    get() {
        val cfg = LocalConfiguration.current
        val safe = (cfg.screenWidthDp / 375f).coerceIn(0.85f, 1.25f)
        return (this * safe).dp
    }

val Float.rdp: Dp
    @Composable
    @ReadOnlyComposable
    get() {
        val cfg = LocalConfiguration.current
        val safe = (cfg.screenWidthDp / 375f).coerceIn(0.85f, 1.25f)
        return (this * safe).dp
    }

val Int.rsp: TextUnit
    @Composable
    @ReadOnlyComposable
    get() {
        val cfg = LocalConfiguration.current
        val safe = (cfg.screenWidthDp / 375f).coerceIn(0.85f, 1.25f)
        return (this * (1f + (safe - 1f) * 0.6f)).sp
    }

val Float.rsp: TextUnit
    @Composable
    @ReadOnlyComposable
    get() {
        val cfg = LocalConfiguration.current
        val safe = (cfg.screenWidthDp / 375f).coerceIn(0.85f, 1.25f)
        return (this * (1f + (safe - 1f) * 0.6f)).sp
    }

/**
 * 标准底部 Tab 屏内容 padding：避免内容被底部导航栏 + 系统手势条/虚拟按键遮挡。
 *
 * 公式：90dp（App 底部 Tab 栏自身高度） + 系统 navigationBars insets + extraBottom 呼吸距
 * 用法：`LazyColumn(contentPadding = bottomTabContentPadding())`
 */
@Composable
fun bottomTabContentPadding(
    top: Dp = 0.dp,
    horizontal: Dp = 0.dp,
    extraBottom: Dp = 30.dp
): PaddingValues {
    val sysNav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        start = horizontal,
        end = horizontal,
        top = top,
        bottom = 90.dp + sysNav + extraBottom
    )
}
