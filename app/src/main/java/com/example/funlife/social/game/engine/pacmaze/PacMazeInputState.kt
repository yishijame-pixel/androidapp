package com.example.funlife.social.game.engine.pacmaze

/**
 * 玩家输入缓冲快照：当前摇杆方向 + 排队目标方向（经典 Pac-Man 提前转向）。
 * @deprecated 新代码请使用 [PacMazeTickInput]；本类型由 [PacMazeTickInput.toLegacyState] 生成供过渡。
 */
data class PacMazeInputState(
    val current: Direction?,
    val queued: Direction?,
    val active: Boolean,
    /** 死区内按住：保持滑行，不把缓冲里的旧方向当作新的期望方向。 */
    val holdOnly: Boolean = false,
    val mode: PacMazeInputMode = if (active) {
        if (holdOnly) PacMazeInputMode.DeadZone else PacMazeInputMode.Committed
    } else {
        PacMazeInputMode.Idle
    },
    val facing: Direction? = current,
    val generation: Long = 0L,
) {
    companion object {
        val Inactive = PacMazeInputState(current = null, queued = null, active = false, holdOnly = false)
    }
}
