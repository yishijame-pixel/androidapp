package com.example.funlife.ui.screens.platformer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/**
 * 透明虚拟摇杆：默认几乎不可见，拖动时显示半透明拇指，不遮挡角色。
 */
@Composable
fun PlatformerVirtualJoystick(
    modifier: Modifier = Modifier,
    onDirection: (left: Boolean, right: Boolean) -> Unit,
) {
    var thumbOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var active by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val maxDragPx = with(density) { 34.dp.toPx() }
    val deadZone = 0.22f

    fun emitFromOffset(offset: Offset) {
        val nx = (offset.x / maxDragPx).coerceIn(-1f, 1f)
        onDirection(nx < -deadZone, nx > deadZone)
    }

    fun reset() {
        thumbOffsetPx = Offset.Zero
        active = false
        onDirection(false, false)
    }

    Box(
        modifier = modifier
            .size(104.dp)
            .pointerInput(maxDragPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    active = true
                    val center = Offset(size.width / 2f, size.height / 2f)
                    fun applyPointer(change: PointerInputChange) {
                        val delta = change.position - center
                        val dist = hypot(delta.x.toDouble(), delta.y.toDouble()).toFloat()
                        val clamped = if (dist > maxDragPx && dist > 0f) {
                            delta / dist * maxDragPx
                        } else {
                            delta
                        }
                        thumbOffsetPx = Offset(clamped.x, clamped.y * 0.25f)
                        emitFromOffset(clamped)
                    }
                    applyPointer(down)
                    var pointer = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointer = change
                        if (change.positionChanged()) applyPointer(change)
                    }
                    reset()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = size.minDimension / 2f - 2f
            drawCircle(
                color = Color.White.copy(alpha = if (active) 0.14f else 0.07f),
                radius = baseR,
                center = center,
                style = Stroke(width = if (active) 2.5f else 1.5f),
            )
            val thumbCenter = center + thumbOffsetPx
            val thumbR = if (active) 22f else 14f
            drawCircle(
                color = Color.White.copy(alpha = if (active) 0.42f else 0.16f),
                radius = thumbR,
                center = thumbCenter,
            )
            if (active) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = thumbR + 4f,
                    center = thumbCenter,
                    style = Stroke(width = 1.5f),
                )
            }
        }
    }
}

@Composable
fun PlatformerJumpButton(
    modifier: Modifier = Modifier,
    airJumpsLeft: Int = 0,
    onPress: () -> Unit,
    onRelease: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "jumpScale",
    )
    val readyDouble = airJumpsLeft > 0

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPress()
                    var pointer: PointerInputChange = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointer = change
                    }
                    pressed = false
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 2f
            val fillAlpha = when {
                pressed -> 0.38f
                readyDouble -> 0.28f
                else -> 0.18f
            }
            val borderAlpha = if (pressed) 0.55f else 0.28f
            drawCircle(
                color = Color(0xFF66BB6A).copy(alpha = fillAlpha),
                radius = r,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = borderAlpha),
                radius = r,
                center = center,
                style = Stroke(width = if (pressed) 2.5f else 1.5f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "跳",
                color = Color.White.copy(alpha = if (pressed) 0.95f else 0.75f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            if (readyDouble) {
                Text("↑×2", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlatformerAttackButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cooldownFraction: Float = 0f,
    onPress: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "attackScale",
    )
    val ready = enabled && cooldownFraction <= 0f

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!ready) return@awaitEachGesture
                    pressed = true
                    onPress()
                    var pointer: PointerInputChange = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointer = change
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 2f
            val fillAlpha = when {
                !enabled -> 0.08f
                pressed -> 0.42f
                ready -> 0.26f
                else -> 0.14f
            }
            val borderAlpha = when {
                !enabled -> 0.12f
                pressed -> 0.58f
                ready -> 0.34f
                else -> 0.2f
            }
            drawCircle(
                color = Color(0xFFFF7043).copy(alpha = fillAlpha),
                radius = r,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = borderAlpha),
                radius = r,
                center = center,
                style = Stroke(width = if (pressed) 2.5f else 1.5f),
            )
            if (cooldownFraction > 0f) {
                drawArc(
                    color = Color.Black.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f * cooldownFraction,
                    useCenter = true,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                )
            }
        }
        Text(
            "攻",
            color = Color.White.copy(alpha = when {
                !enabled -> 0.35f
                pressed -> 0.95f
                ready -> 0.82f
                else -> 0.55f
            }),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun PlatformerRangedButton(
    modifier: Modifier = Modifier,
    label: String = "射",
    enabled: Boolean = true,
    cooldownFraction: Float = 0f,
    onPress: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "rangedScale",
    )
    val ready = enabled && cooldownFraction <= 0f

    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!ready) return@awaitEachGesture
                    pressed = true
                    onPress()
                    var pointer: PointerInputChange = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        pointer = change
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f - 2f
            val fillAlpha = when {
                !enabled -> 0.08f
                pressed -> 0.42f
                ready -> 0.26f
                else -> 0.14f
            }
            val borderAlpha = when {
                !enabled -> 0.12f
                pressed -> 0.58f
                ready -> 0.34f
                else -> 0.2f
            }
            drawCircle(
                color = Color(0xFF42A5F5).copy(alpha = fillAlpha),
                radius = r,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = borderAlpha),
                radius = r,
                center = center,
                style = Stroke(width = if (pressed) 2.5f else 1.5f),
            )
            if (cooldownFraction > 0f) {
                drawArc(
                    color = Color.Black.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 360f * cooldownFraction,
                    useCenter = true,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                )
            }
        }
        Text(
            label,
            color = Color.White.copy(alpha = when {
                !enabled -> 0.35f
                pressed -> 0.95f
                ready -> 0.82f
                else -> 0.55f
            }),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
