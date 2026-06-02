package com.example.funlife.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 🔥 自适应组件库 —— 把"自适应"做成 1 行代码
 *
 * - `AdaptiveScaleBox` —— 全屏覆盖动画/激活弹层套这个
 * - `ResponsiveDialogBox` —— Dialog 内容套这个，自动限宽限高
 * - `AdaptiveGrid` —— 自适应列数（紧凑 N 列、平板 N+2 列）
 * - `BottomTabSafeColumn` —— Tab 屏滚动 Column 自动留底部 nav 空间
 */

/**
 * 整组内容按屏宽自动缩放（safeScale 0.85x ~ 1.25x）。
 *
 * 用于：
 * - 全屏 VIP/成就激活动画
 * - 大尺寸 Splash 元素
 * - 庆祝弹层（confetti、coinrain）
 *
 * 优点：内部 250dp / 180sp 等硬编码尺寸**完全不用动**。
 *
 * @param scaleOverride 可选的缩放倍率覆盖，否则用 LocalScreenAdapter.safeScale
 */
@Composable
fun AdaptiveScaleBox(
    modifier: Modifier = Modifier,
    scaleOverride: Float? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val sa = LocalScreenAdapter.current
    val s = scaleOverride ?: sa.safeScale
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = s
            scaleY = s
        },
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * 给 Dialog 内容用的自适应容器。
 * - 宽度限制在屏宽 92% 但不超过 480dp（平板友好）
 * - 高度上限屏高 85%
 * - 自动加 16dp 屏幕边距
 *
 * 用法：
 * ```kotlin
 * Dialog(onDismissRequest = { ... }) {
 *     ResponsiveDialogBox {
 *         Card(...) { ... }
 *     }
 * }
 * ```
 */
@Composable
fun ResponsiveDialogBox(
    modifier: Modifier = Modifier,
    horizontalScreenMargin: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val sa = LocalScreenAdapter.current
    Box(
        modifier = modifier
            .padding(horizontal = horizontalScreenMargin)
            .widthIn(max = sa.dialogMaxWidth)
            .heightIn(max = sa.dialogMaxHeight),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * 底部 Tab 屏专用滚动 Column 容器：自动留出 90dp 底部导航 + 30dp 呼吸。
 *
 * 替代写法：
 * ```kotlin
 * Column(modifier = Modifier.verticalScroll(...)) {
 *     ...
 *     Spacer(Modifier.height(120.dp))  // 容易忘
 * }
 * ```
 *
 * 改用：
 * ```kotlin
 * BottomTabSafeColumn(scrollState) {
 *     ...
 *     // 自动有底部留白
 * }
 * ```
 */
@Composable
fun BottomTabSafeColumn(
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    extraBottom: Dp = 30.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        content()
        Spacer(Modifier.height(90.dp + extraBottom))
    }
}

/**
 * 自适应宽度按钮 / 卡片：在大屏自动限制最大宽度避免拉得太宽。
 *
 * 用法：
 * ```kotlin
 * Button(onClick = ..., modifier = Modifier.adaptiveMaxWidth(360.dp)) { ... }
 * ```
 */
fun Modifier.adaptiveMaxWidth(maxWidth: Dp = 480.dp): Modifier =
    this.then(Modifier.widthIn(max = maxWidth))

/**
 * 给 Composable 加最低触控目标 (48dp，无障碍要求)
 */
fun Modifier.minTouchTarget(): Modifier =
    this.then(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp))
