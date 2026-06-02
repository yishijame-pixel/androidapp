// ═══════════════════════════════════════════════════════════════════════════
// TopDrawer.kt —— 企业级顶部下拉抽屉（重设计版）
//
// 核心架构：
//   ① 状态层：AnchoredDraggableState（Compose 官方）— 已被 BottomSheetScaffold
//      验证的工业级状态机，自带 fling、velocity、settle、cancellation。
//   ② 手势层：anchoredDraggable + NestedScrollConnection 桥接子层 scrollable，
//      实现"全屏任意位置下拉、子内容已到顶则开抽屉、否则正常滚动"。
//   ③ 渲染层：主内容用 Modifier.offset{}（placement phase，比 graphicsLayer 更轻），
//      抽屉本体在屏顶常驻（背景层），mode 内容仅在 visible 时渲染避免无效动画。
//
// 视觉：抽屉本体固定屏顶 z=0，主内容 z=1 向下偏移，露出抽屉。与微信一致。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animate
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.example.funlife.data.TopDrawerPrefs
import com.example.funlife.ui.components.topdrawer.modes.DiaryMode
import com.example.funlife.ui.components.topdrawer.modes.StarSeaMode
import com.example.funlife.ui.components.topdrawer.modes.WindowMode
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.utils.VibrationHelper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────
// 状态控制器：自实现状态机（mutableFloatStateOf + animate + Job 取消）
// 设计要点：
//   · progress 用 mutableFloatStateOf，NestedScroll 同步写不阻塞
//   · settle 动画用 animate (suspend)，settleJob.cancel() 保证不重入
//   · 不依赖 AnchoredDraggableState（foundation 1.5.4 没有）
// ─────────────────────────────────────────────────────────────────────────
/**
 * 4 阶段拉力交互：
 *   ① IDLE   : pullPx == 0
 *   ② PULL_1 : 0 < pullPx < APPEAR_PX (25dp)            主内容微下移，无 indicator
 *   ③ PULL_2 : APPEAR_PX <= pullPx < SPLIT_PX (75dp)    单球出现 + 渐显，跨阈给一次轻震
 *   ④ PULL_3 : SPLIT_PX <= pullPx < TRIGGER_PX (120dp)  球分裂三颗 + 模糊渐显，跨阈给中震
 *   ⑤ COMMIT : pullPx >= TRIGGER_PX                     强震 + 自动展开抽屉
 */
class TopDrawerState internal constructor() {
    /** 抽屉展开进度 0..1（COMMIT 后由动画驱动） */
    var progress by mutableFloatStateOf(0f)
        internal set

    /** 拉力下拉量（px），仅在 progress=0 阶段累计 */
    var pullPx by mutableFloatStateOf(0f)
        internal set

    /** 是否已触发（跨过 trigger 阈值，松手必开） */
    var committed by mutableStateOf(false)
        internal set

    /** 是否正在回弹/展开（true 时 dispatchRawDelta 全部忽略） */
    var releasing by mutableStateOf(false)
        internal set

    /** 抽屉最大偏移（= 抽屉高度 px），由 Host 测量后写入 */
    internal var maxOffsetPx by mutableFloatStateOf(0f)

    /** 拉力阶段阈值（px），由 Host 注入（dp→px） */
    internal var appearPx by mutableFloatStateOf(0f)
    internal var splitPx by mutableFloatStateOf(0f)
    internal var triggerPx by mutableFloatStateOf(0f)

    /** 用于震动反馈的"已震过"标记 */
    internal var hapticAppearFired = false
    internal var hapticSplitFired = false

    var enabled by mutableStateOf(true)

    /** MutatorMutex：保证 settle 动画串行执行，新动画自动取消旧的 */
    private val mutator = androidx.compose.foundation.MutatorMutex()

    /**
     * NestedScroll 同步消费 dy（返回实际消费 px）。
     * 区分两种模式：
     *  · 抽屉已展开（progress > 0）→ 直接调 progress
     *  · 抽屉收起（progress == 0）→ 累加 pullPx；超 trigger 则 commit
     */
    internal fun dispatchRawDelta(delta: Float): Float {
        if (maxOffsetPx <= 0f) return 0f
        // 回弹/展开动画中：吃掉所有事件，不响应实时手指
        if (releasing) return delta

        // 已 committed 或抽屉已展开
        if (committed || progress > 0f) {
            // 上滑 → 取消 commit + 触发自动关闭（不跟手）
            if (delta < 0f) {
                committed = false
                releasing = true
                return delta
            }
            // 下滑：跟手扩展（直到 1f）
            val newP = (progress + delta / maxOffsetPx).coerceIn(0f, 1f)
            val consumed = (newP - progress) * maxOffsetPx
            progress = newP
            return consumed
        }

        // 拉力模式（progress 仍为 0）
        // ① 上滑且有拉力：触发自动回弹（不跟手），吃掉 dy
        if (delta < 0f && pullPx > 0f) {
            releasing = true
            return delta
        }

        // ② 下拉：累加 pullPx
        if (delta > 0f) {
            val newPull = pullPx + delta
            pullPx = newPull
            // 越过触发阈值：记录 commit。不立即展开，等用户松手后由 NestedScroll fling 调 release()。
            if (!committed && triggerPx > 0f && pullPx >= triggerPx) {
                committed = true
            }
            return delta
        }
        return 0f
    }

    /** 松手 / 上滑回弹时调用 */
    internal suspend fun release() {
        releasing = true
        try {
            if (committed) {
                pullPx = 0f
                hapticAppearFired = false
                hapticSplitFired = false
                animateToTarget(1f)
                committed = false
            } else if (progress > 0f) {
                // 抽屉已开 + 上滑触发 → 自动关闭，同步清 pullPx 残留
                pullPx = 0f
                hapticAppearFired = false
                hapticSplitFired = false
                animateToTarget(0f)
            } else if (pullPx > 0f) {
                // 快速回弹：spring 高刚度 + 临界阻尼，几乎无延迟
                mutator.mutate {
                    animate(
                        initialValue = pullPx,
                        targetValue = 0f,
                        animationSpec = spring(stiffness = 3000f, dampingRatio = 1.0f)
                    ) { v, _ -> pullPx = v }
                }
                hapticAppearFired = false
                hapticSplitFired = false
            }
        } finally {
            releasing = false
        }
    }

    /** 弹性动画到 target；并发调用自动取消前一个 */
    internal suspend fun animateToTarget(target: Float, initialVelocity: Float = 0f) {
        val safe = target.coerceIn(0f, 1f)
        mutator.mutate {
            animate(
                initialValue = progress,
                targetValue = safe,
                initialVelocity = initialVelocity,
                animationSpec = spring(stiffness = 2000f, dampingRatio = 1.0f)
            ) { v, _ -> progress = v }
        }
    }

    suspend fun open() = animateToTarget(1f)
    suspend fun close() {
        pullPx = 0f
        committed = false
        animateToTarget(0f)
    }

    val isVisible: Boolean get() = progress > 0.001f
    val isOpen: Boolean get() = progress >= 0.999f
}

@Composable
fun rememberTopDrawerState(): TopDrawerState = remember { TopDrawerState() }

// ─────────────────────────────────────────────────────────────────────────
// NestedScrollConnection 桥接：让子层 verticalScroll/LazyColumn 的过滚事件
// 转发到抽屉状态机，实现"全屏任意位置下拉，子已到顶则开抽屉"。
// ─────────────────────────────────────────────────────────────────────────
private fun topDrawerNestedScrollConnection(
    state: TopDrawerState,
    scope: kotlinx.coroutines.CoroutineScope
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // 仅在拉力阶段（pullPx>0）吃掉上滑 dy；抽屉打开后让子内容先滚动
        val delta = available.y
        return if (state.enabled && delta < 0f && state.pullPx > 0f && source == NestedScrollSource.Drag) {
            Offset(0f, state.dispatchRawDelta(delta))
        } else Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (!state.enabled || source != NestedScrollSource.Drag) return Offset.Zero
        val rest = available.y
        // 子用尽剩余 dy>0（atTop 下拉）：开抽屉
        if (rest > 0f && state.progress < 1f) {
            return Offset(0f, state.dispatchRawDelta(rest))
        }
        // ★ 移除"上滑关抽屉"分支：用户在内容内向上滚动查看更多时
        // 即使 LazyColumn 已到底也不应该关抽屉。关闭仅通过：
        // ① 点击"上滑收起"区域 ② 抽屉空白处 draggable 拖动 ③ 点 scrim
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!state.enabled) return Velocity.Zero
        // 水平 fling 完全交给子层（HorizontalPager）
        if (kotlin.math.abs(available.x) > kotlin.math.abs(available.y)) {
            return Velocity.Zero
        }
        // 仍在拉力阶段（未到完全打开）→ 由 release 决定回弹/确认
        if (state.pullPx > 0f) {
            state.release()
            return Velocity(0f, available.y)
        }
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (!state.enabled) return Velocity.Zero
        if (kotlin.math.abs(available.x) > kotlin.math.abs(available.y)) {
            return Velocity.Zero
        }
        // 拉力阶段：release 处理
        if (state.pullPx > 0f) {
            state.release()
            return Velocity(0f, available.y)
        }
        // ★ 企业级方案：抽屉已稳定打开，且子层（LazyColumn）已耗尽却仍有强向上 fling
        // 速度（available.y < CLOSE_FLING_THRESHOLD）→ 用户的决定性甩动 → 关抽屉。
        // 阈值设为 -1200 px/s：日常缓速浏览不触发；明确"甩出去"才关闭。
        if (state.progress > 0.99f && available.y < -1200f) {
            state.close()
            return Velocity(0f, available.y)
        }
        return Velocity.Zero
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Host 容器：包裹整个 App 内容
// ─────────────────────────────────────────────────────────────────────────
/**
 * @param state 抽屉状态，由 [rememberTopDrawerState] 创建
 * @param userId 当前用户 id，用于持久化模式选择
 * @param content App 主内容
 */
@Composable
fun TopDrawerHost(
    state: TopDrawerState,
    userId: Long,
    content: @Composable () -> Unit
) {
    val cfg = LocalConfiguration.current
    val density = LocalDensity.current
    val ctx = LocalContext.current
    val haptic = remember { VibrationHelper(ctx) }
    val scope = rememberCoroutineScope()

    // 抽屉高度 = 屏高 88%（底部留一线主内容可见，让用户感知"被推下去"）
    val drawerHeightPx = with(density) { cfg.screenHeightDp.dp.toPx() } * 1.0f
    val drawerHeightDp = with(density) { drawerHeightPx.toDp() }

    // 同步 maxOffset（drawerHeight 变化时更新）
    // 拉力阶段阈值（微信式长距离下拉）
    val appearPx = with(density) { 60.dp.toPx() }
    val splitPx = with(density) { 180.dp.toPx() }
    val triggerPx = with(density) { 320.dp.toPx() }
    val pullDamping = 0.65f  // 拉力阻尼系数：实际下移 = pullPx * 阻尼

    LaunchedEffect(drawerHeightPx, appearPx, splitPx, triggerPx) {
        state.maxOffsetPx = drawerHeightPx
        state.appearPx = appearPx
        state.splitPx = splitPx
        state.triggerPx = triggerPx
    }

    // 当前用户选中的模式
    var currentMode by remember(userId) {
        mutableStateOf(TopDrawerMode.fromId(TopDrawerPrefs.getMode(ctx, userId)))
    }

    // NestedScroll 桥接（remember 一次）
    val nestedConn = remember(state, scope) {
        topDrawerNestedScrollConnection(state, scope)
    }

    // ── Polling 兜底：仅在中间态期间激活，空闲时 0 开销 ──
    LaunchedEffect(state) {
        snapshotFlow {
            state.pullPx > 0f || (state.progress > 0.001f && state.progress < 0.999f)
        }.collect { active ->
            if (!active) return@collect
            var lastPx = state.pullPx
            var lastPr = state.progress
            var lastChangeMs = System.currentTimeMillis()
            while (state.pullPx > 0f || (state.progress > 0.001f && state.progress < 0.999f)) {
                kotlinx.coroutines.delay(40L)
                val px = state.pullPx
                val pr = state.progress
                if (px != lastPx || pr != lastPr) {
                    lastPx = px; lastPr = pr
                    lastChangeMs = System.currentTimeMillis()
                    continue
                }
                if (System.currentTimeMillis() - lastChangeMs >= 80L && !state.releasing &&
                    (px > 0f || (pr > 0.001f && pr < 0.999f))
                ) {
                    state.releasing = true
                    lastChangeMs = System.currentTimeMillis()
                }
            }
        }
    }

    // ── 跨阈值震动：单球出现 / 球分裂 ──
    LaunchedEffect(state) {
        snapshotFlow { state.pullPx }.collect { px ->
            if (!state.hapticAppearFired && px >= appearPx) {
                state.hapticAppearFired = true
                haptic.vibrateShort(8)
            } else if (state.hapticAppearFired && px < appearPx * 0.6f) {
                state.hapticAppearFired = false
            }
            if (!state.hapticSplitFired && px >= splitPx) {
                state.hapticSplitFired = true
                haptic.vibrateShort(20)
            } else if (state.hapticSplitFired && px < splitPx * 0.85f) {
                state.hapticSplitFired = false
            }
        }
    }

    // ── releasing 触发：自动跑 release()（committed 自动展开 / 否则回弹） ──
    val releasing = state.releasing
    LaunchedEffect(releasing) {
        if (releasing) {
            // commit 触发的回弹给强震
            if (state.committed) haptic.vibrateShort(35)
            state.release()
        }
    }

    // 打开/关闭过渡时给一次震动
    val isOpen = state.isOpen
    LaunchedEffect(isOpen) {
        if (isOpen) haptic.vibrateShort(15) else haptic.vibrateShort(8)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1B2E))
            // 在外层 Box 监听 gesture lifecycle：Initial pass 不消费事件，
            // 子节点（抽屉/主内容）正常接收 click/drag；松手时触发 release。
            .pointerInput(state, scope) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    try {
                        while (true) {
                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                            if (ev.changes.none { it.pressed }) break
                        }
                    } finally {
                        if (state.pullPx > 0f || state.committed ||
                            (state.progress > 0.001f && state.progress < 0.999f)
                        ) {
                            scope.launch {
                                if (!state.releasing) state.release()
                            }
                        }
                    }
                }
            }
    ) {

        // ─────────────────────────────────────────────────────────────
        // ① 抽屉本体：仅由 progress 驱动；面板上加 draggable 支持手势关闭
        // ─────────────────────────────────────────────────────────────
        val drawerDragState = rememberDraggableState { dy -> state.dispatchRawDelta(dy) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // ★ 用 fillMaxHeight + max 上限：IME 弹起时 root 缩短，抽屉自动收缩到 IME 上沿
                .fillMaxHeight()
                .heightIn(max = drawerHeightDp)
                .align(Alignment.TopStart)
                // 用 layout 而不是 offset { lambda }：后者是 draw-time graphics offset，
                // 不影响 hit-test，会导致抽屉关闭时仍拦截点击
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val y = ((state.progress - 1f) * state.maxOffsetPx).roundToInt()
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, y)
                    }
                }
                // 背景由 TopDrawerContent 内部按当前 mode 设置（A+B+C 改造）
                // 抽屉本体也接 NestedScroll：内部 scrollable 滚到顶后上滑 → 关抽屉
                .nestedScroll(nestedConn)
                // 面板空白处的拖动：仅在抽屉已展开时启用
                .draggable(
                    state = drawerDragState,
                    orientation = Orientation.Vertical,
                    enabled = state.progress > 0.001f,
                    onDragStopped = { scope.launch { state.release() } }
                )
        ) {
            TopDrawerContent(
                initialMode = currentMode,
                onSettleMode = { settled ->
                    if (settled != currentMode) {
                        currentMode = settled
                        TopDrawerPrefs.setMode(ctx, userId, settled.id)
                        haptic.vibrateShort(12)
                    }
                },
                onClose = { scope.launch { state.close() } },
                userId = userId,
                visible = state.isVisible
            )
        }

        // ─────────────────────────────────────────────────────────────
        // ② 主内容：拉力阶段 = pullPx * 阻尼；展开阶段 = progress * maxOffset
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                // NestedScroll：处理子层 atTop 下拉 → 累积 pullPx
                .nestedScroll(nestedConn)
                // 用 layout 使主内容被推下后 hit-test 也跟随下移
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val drawerY = state.progress * state.maxOffsetPx
                    val pullY = state.pullPx * pullDamping
                    val y = kotlin.math.max(drawerY, pullY).roundToInt()
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, y)
                    }
                }
        ) {
            content()
            // ── Scrim：随 progress 渐黑，提升层级感（不拦截事件） ──
            if (state.progress > 0.001f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = state.progress.coerceIn(0f, 1f) * 0.45f }
                        .background(Color.Black)
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // ③ 拉力指示器（小球 + 分裂 + 模糊）：只在拉力阶段显示
        // ─────────────────────────────────────────────────────────────
        PullIndicator(
            pullPx = state.pullPx,
            appearPx = appearPx,
            splitPx = splitPx,
            triggerPx = triggerPx,
            damping = pullDamping
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 拉力指示器：4 阶段视觉
//   · pullPx < appearPx          → 不显示
//   · appearPx ≤ pullPx < splitPx → 单球渐显
//   · splitPx ≤ pullPx < triggerPx → 球分裂为 3 颗 + 模糊遮罩渐显
//   · pullPx ≥ triggerPx         → 三球到达最远 + 模糊达最大
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun BoxScope.PullIndicator(
    pullPx: Float,
    appearPx: Float,
    splitPx: Float,
    triggerPx: Float,
    damping: Float
) {
    if (pullPx <= 0f) return

    // 单球淡入：appearPx → splitPx 间 0..1
    val ballAlpha = ((pullPx - appearPx) / (splitPx - appearPx)).coerceIn(0f, 1f)
    // 分裂进度：splitPx → triggerPx 间 0..1
    val splitProgress = ((pullPx - splitPx) / (triggerPx - splitPx)).coerceIn(0f, 1f)
    // 球大小（dp）和分裂偏移（dp）
    val ballSize = 12f + 4f * splitProgress  // 12→16dp
    val splitDistance = 28f * splitProgress  // 0→28dp

    // 球的位置：贴着主内容顶边上方 ~24dp（如微信下拉小球，悬浮在被推下去露出的暗色区域中）
    val mainTopDp = with(LocalDensity.current) { (pullPx * damping).toDp() }
    // offset.y 是球容器（40dp 高）的顶。希望容器中心 = mainTop - 16dp → offset.y = mainTop - 36dp
    val centerY = (mainTopDp - 36.dp).coerceAtLeast((-8).dp)

    // 模糊遮罩（splitProgress > 0 时显现）
    if (splitProgress > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = splitProgress * 0.35f }
                .background(Color.Black)
        )
    }

    // 球容器：屏幕顶部居中
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .offset(y = centerY)
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // ball color: pullPx>=triggerPx 高亮（粉），否则中性灰
        val ballColor = if (pullPx >= triggerPx) Color(0xFFFF8FB1) else Color(0xFFB0B5C0)

        // 中心球
        Box(
            modifier = Modifier
                .size(ballSize.dp)
                .graphicsLayer { alpha = ballAlpha }
                .clip(RoundedCornerShape(50))
                .background(ballColor)
        )
        // 左球（splitProgress > 0 时分裂出来）
        if (splitProgress > 0f) {
            Box(
                modifier = Modifier
                    .offset(x = -splitDistance.dp)
                    .size((ballSize * 0.85f).dp)
                    .graphicsLayer { alpha = splitProgress }
                    .clip(RoundedCornerShape(50))
                    .background(ballColor)
            )
            Box(
                modifier = Modifier
                    .offset(x = splitDistance.dp)
                    .size((ballSize * 0.85f).dp)
                    .graphicsLayer { alpha = splitProgress }
                    .clip(RoundedCornerShape(50))
                    .background(ballColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 抽屉内容：顶部模式切换条 + 模式内容区 + 底部把手
// ─────────────────────────────────────────────────────────────────────────
@Composable
private fun TopDrawerContent(
    initialMode: TopDrawerMode,
    onSettleMode: (TopDrawerMode) -> Unit,
    onClose: () -> Unit,
    userId: Long,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    // ★ 单一事实来源：pagerState.currentPage（含滑动中的预览/未 settle 的值）
    //    其它 UI（Tab 激活态、背景渐变、持久化）都从这里派生。
    //    这样消除了"点 Tab 立即 setMode + 触发 LaunchedEffect 再 animate"的循环冲突。
    val modes = remember { TopDrawerMode.values().toList() }
    val pagerState = rememberPagerState(initialPage = initialMode.ordinal) { modes.size }
    val scope = rememberCoroutineScope()

    // 当前实际 mode（含半滑状态时显示 currentPage 对应那个，Tab 高亮跟手）
    val livePage = pagerState.currentPage.coerceIn(0, modes.size - 1)
    val liveMode = modes[livePage]

    // settled 后才持久化偏好 + 震动（避免滑动中频繁写 prefs）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val nm = modes[page.coerceIn(0, modes.size - 1)]
            onSettleMode(nm)
        }
    }

    // C. 背景跟随 liveMode 平滑渐变 → 滑动过程中背景就开始变（沉浸感）
    val target = backgroundOf(liveMode)
    val c0 by animateColorAsState(target[0], tween(450), label = "bg0")
    val c1 by animateColorAsState(target[1], tween(450), label = "bg1")
    val c2 by animateColorAsState(target[2], tween(450), label = "bg2")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c0, c1, c2)))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Spacer(Modifier.height(Spacing.sm))

        // A. 全新 Tab Bar：图标 + 文字 + 激活态短杠指示器
        ModeSwitcher(
            current = liveMode,
            onPick = { picked ->
                // 仅调 pager.animateScrollToPage；不再直接改 mode，避免循环
                scope.launch { pagerState.animateScrollToPage(picked.ordinal) }
            }
        )

        Spacer(Modifier.height(Spacing.sm))

        // B. HorizontalPager 替代 AnimatedContent：手指左右划即可切换 mode
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (visible) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 0.dp,
                ) { page ->
                    when (modes[page]) {
                        TopDrawerMode.WINDOW -> WindowMode()
                        TopDrawerMode.DIARY -> DiaryMode(userId = userId)
                        TopDrawerMode.STAR_SEA -> StarSeaMode(userId = userId, onPickEntry = { onClose() })
                    }
                }
            }
        }

        // 底部关闭区域：
        // ① navigationBarsPadding 确保不被系统手势导航栏遮住
        // ② 自带半透明深色渐变底条 → 浅色 paper 背景（如日记米黄页）下也清晰可见
        // ③ 文字 + 箭头双语义
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .height(48.rdp)
                    .padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "︿  上滑收起  ︿",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 4.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────
   A. Tab Bar：图标 + 文字 + 激活短杠 + Material Icons（emoji-free）
   ──────────────────────────────────────────────────────────── */
@Composable
private fun ModeSwitcher(
    current: TopDrawerMode,
    onPick: (TopDrawerMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TopDrawerMode.values().forEach { m ->
            val active = m == current
            val accent = accentOf(m)
            val indicatorWidth by animateDpAsState(
                targetValue = if (active) 18.dp else 0.dp,
                animationSpec = tween(durationMillis = 260),
                label = "ind_${m.id}"
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.lg))
                    .clickable { onPick(m) }
                    .padding(vertical = 6.dp)
                    .semantics {
                        contentDescription = "${m.title} 模式"
                        role = androidx.compose.ui.semantics.Role.Tab
                        selected = active
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = iconFor(m),
                    contentDescription = null,
                    tint = if (active) accent else Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    m.title,
                    fontSize = 12.sp,
                    color = if (active) Color.White else Color.White.copy(alpha = 0.55f),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                // 激活态下方短杠指示器（mode 专属色） — 比胶囊背景更克制更高级
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(accent)
                )
            }
        }
    }
}

/* ────────────────────────────────────────────────────────────
   Mode → 视觉资源映射（图标 / 强调色 / 背景渐变三件套）
   每个 mode 都有专属的"时空温度"：窗=晨蓝、日记=暖橙、星海=星紫
   ──────────────────────────────────────────────────────────── */
private fun iconFor(m: TopDrawerMode): ImageVector = when (m) {
    TopDrawerMode.WINDOW -> Icons.Outlined.WbSunny
    TopDrawerMode.DIARY -> Icons.Outlined.MenuBook
    TopDrawerMode.STAR_SEA -> Icons.Outlined.AutoAwesome
}

private fun accentOf(m: TopDrawerMode): Color = when (m) {
    TopDrawerMode.WINDOW -> Color(0xFF8BC4E8)   // 晨蓝
    TopDrawerMode.DIARY -> Color(0xFFFFB07A)    // 暖橙
    TopDrawerMode.STAR_SEA -> Color(0xFFC0A0FF) // 星紫
}

/** 三段式渐变背景（Brush.verticalGradient 用） */
private fun backgroundOf(m: TopDrawerMode): List<Color> = when (m) {
    TopDrawerMode.WINDOW -> listOf(
        Color(0xFF1B2238), Color(0xFF243454), Color(0xFF2A3B5C)
    )
    TopDrawerMode.DIARY -> listOf(
        Color(0xFF2D2418), Color(0xFF3D2A1F), Color(0xFF4D3525)
    )
    TopDrawerMode.STAR_SEA -> listOf(
        Color(0xFF1A1B2E), Color(0xFF14112A), Color(0xFF0A0B1E)
    )
}
