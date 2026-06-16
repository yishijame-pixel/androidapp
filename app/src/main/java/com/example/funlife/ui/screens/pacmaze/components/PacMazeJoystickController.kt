package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.PacMazeRawJoystickSample
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.viewmodel.PacMazeLocalViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 摇杆视觉状态，与游戏世界 StateFlow 隔离，避免 60fps 重组冲掉手势协程。 */
@Stable
class PacMazeJoystickState {
    var knobOffset by mutableStateOf(Offset.Zero)
    var isActive by mutableStateOf(false)
    var areaSize by mutableStateOf(IntSize.Zero)
    var maxRadiusPx by mutableStateOf(1f)
}

@Composable
fun rememberPacMazeJoystickState(): PacMazeJoystickState = remember { PacMazeJoystickState() }

/** 新开一局或重进关卡时重置摇杆视觉，避免与逻辑输入不同步。 */
fun PacMazeJoystickState.resetVisual() {
    knobOffset = Offset.Zero
    isActive = false
}

/**
 * 挂在全屏稳定容器上的摇杆触摸层。
 * 仅消费左下角区域内的指针，其余区域不拦截（暂停键等可正常点击）。
 */
@Composable
fun Modifier.pacMazeJoystickInput(
    viewModel: PacMazeLocalViewModel,
    joystickState: PacMazeJoystickState,
    zoneWidth: Dp = 120.dp,
    zoneHeight: Dp = 200.dp,
    deadZone: Float = 0.08f,
): Modifier {
    val density = LocalDensity.current
    val zoneWidthPx = with(density) { zoneWidth.toPx() }
    val zoneHeightPx = with(density) { zoneHeight.toPx() }
    return this.then(
        pointerInput(viewModel, zoneWidthPx, zoneHeightPx, deadZone) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!isInJoystickZone(down.position, zoneWidthPx, zoneHeightPx)) return@awaitEachGesture

                down.consume()
                val center = joystickCenter(zoneWidthPx, zoneHeightPx)
                val maxRadius = min(zoneWidthPx, zoneHeightPx) * 0.32f
                joystickState.maxRadiusPx = maxRadius

                applyJoystickOffset(
                    viewModel = viewModel,
                    joystickState = joystickState,
                    offset = clampJoystickOffset(down.position - center, maxRadius),
                    maxRadius = maxRadius,
                )

                drag(down.id) { change ->
                    change.consume()
                    applyJoystickOffset(
                        viewModel = viewModel,
                        joystickState = joystickState,
                        offset = clampJoystickOffset(change.position - center, maxRadius),
                        maxRadius = maxRadius,
                    )
                }

                joystickState.resetVisual()
                viewModel.releaseJoystick()
            }
        },
    )
}

@Composable
fun Modifier.pacMazeJoystickInput(
    joystickState: PacMazeJoystickState,
    zoneWidth: Dp = 120.dp,
    zoneHeight: Dp = 200.dp,
    onSample: (PacMazeRawJoystickSample) -> Unit,
): Modifier {
    val density = LocalDensity.current
    val zoneWidthPx = with(density) { zoneWidth.toPx() }
    val zoneHeightPx = with(density) { zoneHeight.toPx() }
    return this.then(
        pointerInput(zoneWidthPx, zoneHeightPx, onSample) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!isInJoystickZone(down.position, zoneWidthPx, zoneHeightPx)) return@awaitEachGesture
                down.consume()
                val center = joystickCenter(zoneWidthPx, zoneHeightPx)
                val maxRadius = min(zoneWidthPx, zoneHeightPx) * 0.32f
                joystickState.maxRadiusPx = maxRadius
                fun push(offset: Offset) {
                    joystickState.knobOffset = offset
                    joystickState.isActive = true
                    onSample(
                        PacMazeRawJoystickSample(
                            offsetX = offset.x,
                            offsetY = offset.y,
                            maxRadius = maxRadius,
                            fingerDown = true,
                        ),
                    )
                }
                push(clampJoystickOffset(down.position - center, maxRadius))
                drag(down.id) { change ->
                    change.consume()
                    push(clampJoystickOffset(change.position - center, maxRadius))
                }
                joystickState.resetVisual()
                onSample(PacMazeRawJoystickSample.Released)
            }
        },
    )
}

@Composable
fun PacMazeJoystickVisual(
    state: PacMazeJoystickState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.onSizeChanged { state.areaSize = it },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = min(size.width, size.height) * 0.42f
            val innerR = outerR * 0.62f
            val knobR = outerR * 0.28f
            val knobCenter = center + state.knobOffset

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (state.isActive) 0.22f else 0.14f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = outerR,
                ),
                radius = outerR,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = if (state.isActive) 0.55f else 0.32f),
                radius = outerR,
                center = center,
                style = Stroke(width = 3.5f),
            )
            drawCircle(
                color = PacMazePalette.accentOrange.copy(alpha = if (state.isActive) 0.22f else 0.14f),
                radius = innerR,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PacMazePalette.accentGold, PacMazePalette.accentOrange),
                    center = knobCenter - Offset(knobR * 0.2f, knobR * 0.2f),
                    radius = knobR,
                ),
                radius = knobR,
                center = knobCenter,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = knobR * 0.55f,
                center = knobCenter - Offset(knobR * 0.25f, knobR * 0.25f),
            )
            if (state.isActive) {
                val guideLen = outerR * 0.85f
                val angle = atan2(state.knobOffset.y, state.knobOffset.x)
                val end = center + Offset(cos(angle) * guideLen, sin(angle) * guideLen)
                drawLine(
                    color = PacMazePalette.accentGold.copy(alpha = 0.45f),
                    start = center,
                    end = end,
                    strokeWidth = 4f,
                )
            }
        }
    }
}

private fun PointerInputScope.isInJoystickZone(
    position: Offset,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
): Boolean = position.x <= zoneWidthPx && position.y >= size.height - zoneHeightPx

private fun PointerInputScope.joystickCenter(zoneWidthPx: Float, zoneHeightPx: Float): Offset =
    Offset(zoneWidthPx / 2f, size.height - zoneHeightPx / 2f)

private fun applyJoystickOffset(
    viewModel: PacMazeLocalViewModel,
    joystickState: PacMazeJoystickState,
    offset: Offset,
    maxRadius: Float,
) {
    joystickState.knobOffset = offset
    joystickState.isActive = true
    joystickState.maxRadiusPx = maxRadius
    viewModel.updateJoystickRaw(
        PacMazeRawJoystickSample(
            offsetX = offset.x,
            offsetY = offset.y,
            maxRadius = maxRadius,
            fingerDown = true,
        ),
    )
}

private fun clampJoystickOffset(offset: Offset, maxRadius: Float): Offset {
    val dist = sqrt(offset.x * offset.x + offset.y * offset.y)
    if (dist <= maxRadius || dist == 0f) return offset
    val scale = maxRadius / dist
    return Offset(offset.x * scale, offset.y * scale)
}
