package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.sqrt

/** UI 层上报的原始摇杆样本（与 sim tick 解耦）。 */
data class PacMazeRawJoystickSample(
    val offsetX: Float,
    val offsetY: Float,
    val maxRadius: Float,
    val fingerDown: Boolean,
) {
    val strength: Float
        get() = if (maxRadius <= 0f) 0f else {
            (sqrt(offsetX * offsetX + offsetY * offsetY) / maxRadius).coerceIn(0f, 1f)
        }

    val sector: Direction?
        get() = if (strength < PacMazeInputConfig.Default.deadZone) {
            null
        } else {
            joystickOffsetToDirection(offsetX, offsetY)
        }

    companion object {
        val Released = PacMazeRawJoystickSample(
            offsetX = 0f,
            offsetY = 0f,
            maxRadius = 1f,
            fingerDown = false,
        )
    }
}
