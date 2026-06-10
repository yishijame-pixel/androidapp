package com.example.funlife.social.game.engine.pacmaze

/** 将 [virtual-joystick-android](https://github.com/controlwear/virtual-joystick-android) 角度映射为四向。 */
fun joystickAngleToDirection(angle: Int): Direction {
    val a = ((angle % 360) + 360) % 360
    return when {
        a >= 45 && a < 135 -> Direction.UP
        a >= 135 && a < 225 -> Direction.LEFT
        a >= 225 && a < 315 -> Direction.DOWN
        else -> Direction.RIGHT
    }
}
