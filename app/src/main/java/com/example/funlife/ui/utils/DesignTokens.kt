package com.example.funlife.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * 🔥 设计令牌（Design Tokens）
 *
 * 统一全 App 的间距、圆角、字号、动画时长。
 * - 所有数值是「设计基线值」，会自动经 `LocalScreenAdapter` 缩放
 * - 替代散落在各处的硬编码 dp/sp 数字
 * - 修改设计系统时只需改这里
 *
 * 用法：
 * ```kotlin
 * Spacer(Modifier.height(Spacing.md))
 * Card(shape = RoundedCornerShape(Radius.lg)) { ... }
 * Text(text, fontSize = TextSize.title)
 * ```
 *
 * 命名约定（从小到大）：
 * - xxs → xs → sm → md → lg → xl → xxl → xxxl
 */

/** 间距（4dp 步进） */
object Spacing {
    val none: Dp @Composable @ReadOnlyComposable get() = 0.dp
    val xxs: Dp @Composable @ReadOnlyComposable get() = 2.rdp
    val xs: Dp @Composable @ReadOnlyComposable get() = 4.rdp
    val sm: Dp @Composable @ReadOnlyComposable get() = 8.rdp
    val md: Dp @Composable @ReadOnlyComposable get() = 16.rdp
    val lg: Dp @Composable @ReadOnlyComposable get() = 24.rdp
    val xl: Dp @Composable @ReadOnlyComposable get() = 32.rdp
    val xxl: Dp @Composable @ReadOnlyComposable get() = 48.rdp
    val xxxl: Dp @Composable @ReadOnlyComposable get() = 64.rdp
}

/** 圆角 */
object Radius {
    val none: Dp @Composable @ReadOnlyComposable get() = 0.dp
    val xs: Dp @Composable @ReadOnlyComposable get() = 4.rdp
    val sm: Dp @Composable @ReadOnlyComposable get() = 8.rdp
    val md: Dp @Composable @ReadOnlyComposable get() = 12.rdp
    val lg: Dp @Composable @ReadOnlyComposable get() = 16.rdp
    val xl: Dp @Composable @ReadOnlyComposable get() = 20.rdp
    val xxl: Dp @Composable @ReadOnlyComposable get() = 24.rdp
    val pill: Dp @Composable @ReadOnlyComposable get() = 999.dp  // 胶囊形不缩放
}

/** 阴影 elevation */
object Elevation {
    val none: Dp = 0.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
    val xl: Dp = 12.dp
    val xxl: Dp = 16.dp
}

/** 文字尺寸（sp 自动经 ScreenAdapter 保守缩放） */
object TextSize {
    val tiny: TextUnit @Composable @ReadOnlyComposable get() = 10.rsp
    val xs: TextUnit @Composable @ReadOnlyComposable get() = 11.rsp
    val sm: TextUnit @Composable @ReadOnlyComposable get() = 12.rsp
    val body: TextUnit @Composable @ReadOnlyComposable get() = 14.rsp
    val md: TextUnit @Composable @ReadOnlyComposable get() = 16.rsp
    val title: TextUnit @Composable @ReadOnlyComposable get() = 18.rsp
    val headline: TextUnit @Composable @ReadOnlyComposable get() = 20.rsp
    val display: TextUnit @Composable @ReadOnlyComposable get() = 24.rsp
    val hero: TextUnit @Composable @ReadOnlyComposable get() = 32.rsp
}

/** 图标尺寸 */
object IconSize {
    val xs: Dp @Composable @ReadOnlyComposable get() = 12.rdp
    val sm: Dp @Composable @ReadOnlyComposable get() = 16.rdp
    val md: Dp @Composable @ReadOnlyComposable get() = 20.rdp
    val lg: Dp @Composable @ReadOnlyComposable get() = 24.rdp
    val xl: Dp @Composable @ReadOnlyComposable get() = 32.rdp
    val xxl: Dp @Composable @ReadOnlyComposable get() = 48.rdp
}

/** 触控点击区域最小值（无障碍要求） */
object TouchTarget {
    /** Material 推荐最小 48dp */
    val min: Dp = 48.dp
    val small: Dp = 40.dp
}

/** 动画时长（ms），统一节奏避免有些组件过快有些过慢 */
object AnimationDuration {
    const val instant = 100
    const val fast = 200
    const val normal = 300
    const val slow = 500
    const val verySlow = 800
}
