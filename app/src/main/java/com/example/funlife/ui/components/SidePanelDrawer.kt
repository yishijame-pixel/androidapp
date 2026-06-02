// =========================================================================
// SidePanelDrawer.kt
// 全局侧边面板：左边缘右滑唤出，跟手动画 + 震动反馈
// 严格遵循 docs/屏幕适配指南.md（rdp/rsp/Spacing）和 DEVELOPMENT_PRINCIPLES.md
// =========================================================================
package com.example.funlife.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.funlife.utils.VibrationHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 抽屉状态控制器。
 * 在 root 用 [rememberSidePanelState] 创建，把状态传给 [SidePanelDrawer]。
 * 业务页面可调用 state.open() 程式化打开；手势打开由 [SidePanelEdgeDetector] 处理。
 */
class SidePanelState internal constructor() {
    /** 0..1，0 = 完全关闭，1 = 完全打开 */
    var progress by mutableFloatStateOf(0f)
        internal set
    var enabled by mutableStateOf(true)

    /** 同步设置（拖动时使用，不会卡顿） */
    fun snapTo(v: Float) { progress = v.coerceIn(0f, 1f) }

    /** 平滑动画到打开 */
    suspend fun open() {
        androidx.compose.animation.core.animate(
            initialValue = progress,
            targetValue = 1f,
            animationSpec = tween(280)
        ) { value, _ -> progress = value }
    }

    /** 平滑动画到关闭 */
    suspend fun close() {
        androidx.compose.animation.core.animate(
            initialValue = progress,
            targetValue = 0f,
            animationSpec = tween(220)
        ) { value, _ -> progress = value }
    }

    val isVisible: Boolean get() = progress > 0.001f
}

@Composable
fun rememberSidePanelState(): SidePanelState = remember { SidePanelState() }

/**
 * 全局抽屉容器。请放在最根层（在 NavGraph 之后），覆盖整个屏幕。
 * 抽屉宽度 = 屏宽 * 0.82
 */
@Composable
fun SidePanelDrawer(
    state: SidePanelState,
    onNavigate: (String) -> Unit = {},
    content: @Composable () -> Unit
) {
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { cfg.screenWidthDp.dp.toPx() }
    val drawerWidthPx = screenWidthPx * 0.88f

    val ctx = LocalContext.current
    val haptic = remember { VibrationHelper(ctx) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1F2937))) {
        // 性能要点：所有跟手位移都用 graphicsLayer（仅 draw 阶段，不触发 relayout / 重组）

        // 1) 主内容：常驻、用 graphicsLayer 平移
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = state.progress
                    translationX = drawerWidthPx * p
                    val rPx = 20.dp.toPx() * p
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(rPx)
                    clip = p > 0f
                    shadowElevation = if (p > 0f) 24f else 0f
                }
        ) { content() }

        // 2) 半透明遮罩：常驻、graphicsLayer 同步平移 + alpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = state.progress
                    translationX = drawerWidthPx * p
                    alpha = 0.45f * p
                }
                .background(Color.Black)
                .let { m ->
                    if (state.progress > 0.95f) m.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { scope.launch { state.close() } }
                    ) else m
                }
        )

        // 3) 抽屉本体：常驻、graphicsLayer 平移
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { drawerWidthPx.toDp() })
                .graphicsLayer {
                    val p = state.progress
                    translationX = -drawerWidthPx + drawerWidthPx * p
                }
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF7FA), Color(0xFFFDF6F0))
                    )
                )
                // 抽屉内向左拖关闭
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (state.progress < 0.55f) {
                                    haptic.vibrateShort(15)
                                    state.close()
                                } else {
                                    state.open()
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        val delta = dragAmount / drawerWidthPx
                        state.snapTo(state.progress + delta)
                    }
                }
        ) {
            SidePanelContent(
                onCloseRequest = { scope.launch { state.close() } },
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * 包裹式手势检测器：把 [content] 作为子节点放在内部。
 * 这样 Compose 的 Initial→Main→Final 传播链中，子元素先在 Main 消费事件，
 * 父检测器在 Final 看到 isConsumed，从而正确避让；子元素没消费且滑动方向偏右
 * 才接管。这是企业级 WeChat 风格抽屉的标准实现。
 */
@Composable
fun SidePanelEdgeDetector(
    state: SidePanelState,
    content: @Composable () -> Unit
) {
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { cfg.screenWidthDp.dp.toPx() }
    val drawerWidthPx = screenWidthPx * 0.88f

    val ctx = LocalContext.current
    val haptic = remember { VibrationHelper(ctx) }
    val scope = rememberCoroutineScope()

    val view = LocalView.current
    var detectorBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }

    // 系统手势排除区：左缘 24dp × 中部 200dp（避开 Android 10+ 返回手势）
    DisposableEffect(detectorBounds, state.enabled) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && state.enabled) {
            runCatching {
                detectorBounds?.let { view.systemGestureExclusionRects = listOf(it) }
            }
        }
        onDispose {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                runCatching { view.systemGestureExclusionRects = emptyList() }
            }
        }
    }

    val screenHeightDp = cfg.screenHeightDp.dp
    val stripHeight = 200.dp
    val topPadding = ((screenHeightDp - stripHeight) / 2).coerceAtLeast(0.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.enabled) {
                if (!state.enabled) return@pointerInput
                val slop = viewConfiguration.touchSlop
                while (true) {
                    awaitPointerEventScope {
                        // 等首次按下：Final pass，子元素已经在 Main pass 处理过
                        val firstDown = awaitPointerEvent(PointerEventPass.Final)
                            .changes.firstOrNull { it.pressed } ?: return@awaitPointerEventScope
                        var totalDx = 0f
                        var totalDy = 0f
                        var triggered = false
                        var thresholdHaptic = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == firstDown.id }
                                ?: break
                            if (!change.pressed) break

                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalDx += dx
                            totalDy += dy

                            if (!triggered) {
                                // 触发条件：子元素未消费 + 朝右位移超 slop + 横向显著大于竖向
                                if (!change.isConsumed
                                    && totalDx > slop
                                    && totalDx > kotlin.math.abs(totalDy) * 1.8f
                                ) {
                                    triggered = true
                                    haptic.vibrateShort(8)
                                    // 关键：触发瞬间把面板"补齐"到手指当前位置，
                                    // 之后用 totalDx 持续驱动，实现 1:1 同步
                                    state.snapTo((totalDx / drawerWidthPx).coerceIn(0f, 1f))
                                    change.consume()
                                }
                            } else {
                                // 持续阶段：用 totalDx 直接驱动（绝对位置，而非增量）
                                val target = (totalDx / drawerWidthPx).coerceIn(0f, 1f)
                                state.snapTo(target)
                                if (!thresholdHaptic && target >= 0.30f) {
                                    thresholdHaptic = true
                                    haptic.vibrateShort(28)
                                }
                                change.consume()
                            }
                        }

                        if (triggered) {
                            scope.launch {
                                if (state.progress > 0.30f) {
                                    if (!thresholdHaptic) haptic.vibrateShort(28)
                                    state.open()
                                } else {
                                    haptic.vibrateShort(15)
                                    state.close()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // 1) 真正的内容：子元素先收事件、先消费
        content()

        // 2) 左缘 200dp 高的占位（仅用于 systemGestureExclusionRects 报告矩形给系统，
        //    占位本身不消费事件，因为没有 pointerInput）
        Box(
            modifier = Modifier
                .padding(top = topPadding)
                .width(24.dp)
                .height(stripHeight)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val size = coords.size
                    detectorBounds = android.graphics.Rect(
                        pos.x.toInt(),
                        pos.y.toInt(),
                        pos.x.toInt() + size.width,
                        pos.y.toInt() + size.height
                    )
                }
        )
    }
}
