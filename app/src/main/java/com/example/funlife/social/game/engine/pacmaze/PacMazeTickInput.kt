package com.example.funlife.social.game.engine.pacmaze

/**
 * 单个逻辑 tick 的输入快照（模拟层唯一真相）。
 * generation 在 committed 方向变化时递增，便于测试与回放。
 */
data class PacMazeTickInput(
    val tick: Long,
    val generation: Long,
    val active: Boolean,
    val mode: PacMazeInputMode,
    val strength: Float,
    val sector: Direction?,
    val facing: Direction?,
    val committed: Direction?,
) {
    val holdOnly: Boolean get() = mode == PacMazeInputMode.DeadZone

    /** 驱动 PacMazeMotion 的期望方向（Committed/Pending 缓冲转向；Spin 只改朝向）。 */
    val desired: Direction?
        get() = when (mode) {
            PacMazeInputMode.Committed -> committed
            PacMazeInputMode.Pending -> sector
            PacMazeInputMode.Spin -> null
            else -> null
        }

    fun toLegacyState(): PacMazeInputState {
        if (!active) return PacMazeInputState.Inactive
        val dir = committed ?: sector
        return PacMazeInputState(
            current = dir,
            queued = when (mode) {
                PacMazeInputMode.Committed -> committed
                PacMazeInputMode.Pending -> sector
                PacMazeInputMode.Spin -> null
                PacMazeInputMode.DeadZone -> committed
                PacMazeInputMode.Idle -> null
            },
            active = true,
            holdOnly = holdOnly,
            mode = mode,
            facing = facing,
            generation = generation,
        )
    }

    companion object {
        val Inactive = PacMazeTickInput(
            tick = 0L,
            generation = 0L,
            active = false,
            mode = PacMazeInputMode.Idle,
            strength = 0f,
            sector = null,
            facing = null,
            committed = null,
        )

        fun committed(tick: Long, direction: Direction, generation: Long = 1L): PacMazeTickInput =
            PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.Committed,
                strength = 1f,
                sector = direction,
                facing = direction,
                committed = direction,
            )

        fun deadZone(tick: Long, lastCommitted: Direction? = null, generation: Long = 0L): PacMazeTickInput =
            PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.DeadZone,
                strength = 0f,
                sector = null,
                facing = lastCommitted,
                committed = lastCommitted,
            )

        fun pending(tick: Long, sector: Direction, strength: Float = 0.5f, generation: Long = 0L): PacMazeTickInput =
            PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.Pending,
                strength = strength,
                sector = sector,
                facing = sector,
                committed = null,
            )

        fun spin(tick: Long, sector: Direction, generation: Long = 0L): PacMazeTickInput =
            PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.Spin,
                strength = 0.5f,
                sector = sector,
                facing = sector,
                committed = null,
            )
    }
}
