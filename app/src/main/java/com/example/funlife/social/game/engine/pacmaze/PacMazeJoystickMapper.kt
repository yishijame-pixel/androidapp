package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs
import kotlin.math.atan2

/** 将 [virtual-joystick-android](https://github.com/controlwear/virtual-joystick-android) 角度映射为四向。 */
fun joystickAngleToDirection(angle: Int): Direction = joystickAngleDegToDirection(angle.toFloat())

fun joystickOffsetToDirection(offsetX: Float, offsetY: Float): Direction {
    val angleDeg = Math.toDegrees(atan2(-offsetY.toDouble(), offsetX.toDouble())).toFloat()
    return joystickAngleDegToDirection(angleDeg)
}

/** 屏幕坐标摇杆偏移 → 四向。 */
fun joystickAngleDegToDirection(angleDeg: Float): Direction {
    val normalized = ((angleDeg % 360f) + 360f) % 360f
    return when {
        normalized >= 45f && normalized < 135f -> Direction.UP
        normalized >= 135f && normalized < 225f -> Direction.LEFT
        normalized >= 225f && normalized < 315f -> Direction.DOWN
        else -> Direction.RIGHT
    }
}

/**
 * 带边界迟滞的扇区解析，避免转圈在 45° 分界来回跳变。
 */
fun joystickResolveSector(
    offsetX: Float,
    offsetY: Float,
    strength: Float,
    deadZone: Float,
    previous: Direction?,
    hysteresisDeg: Float = 10f,
): Direction? {
    if (strength < deadZone) return null
    val angleDeg = Math.toDegrees(atan2(-offsetY.toDouble(), offsetX.toDouble())).toFloat()
    val normalized = ((angleDeg % 360f) + 360f) % 360f
    val candidate = joystickAngleDegToDirection(normalized)
    if (previous == null || previous == candidate) return candidate
    val nearBoundary = isJoystickNearBoundary(normalized, hysteresisDeg)
    return if (nearBoundary) previous else candidate
}

private fun isJoystickNearBoundary(angleDeg: Float, hysteresisDeg: Float): Boolean {
    val boundaries = floatArrayOf(45f, 135f, 225f, 315f)
    return boundaries.any { boundary ->
        val diff = abs(((angleDeg - boundary + 180f) % 360f) - 180f)
        diff <= hysteresisDeg
    }
}

fun joystickSampleAngleDeg(offsetX: Float, offsetY: Float): Float =
    ((Math.toDegrees(atan2(-offsetY.toDouble(), offsetX.toDouble())).toFloat() % 360f) + 360f) % 360f

/** 两角度间最短路径差（度），恒为非负。 */
fun joystickAngleDeltaDeg(fromDeg: Float, toDeg: Float): Float {
    var delta = toDeg - fromDeg
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return abs(delta)
}
