package com.example.funlife.social.game.engine.pacmaze

/**
 * 玩家输入缓冲快照：当前摇杆方向 + 排队目标方向（经典 Pac-Man 提前转向）。
 */
data class PacMazeInputState(
    val current: Direction?,
    val queued: Direction?,
    val active: Boolean,
) {
    companion object {
        val Inactive = PacMazeInputState(current = null, queued = null, active = false)
    }
}
