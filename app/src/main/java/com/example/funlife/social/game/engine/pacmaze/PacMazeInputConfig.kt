package com.example.funlife.social.game.engine.pacmaze

/** 摇杆输入控制器参数（与 UI 布局无关的逻辑阈值）。 */
data class PacMazeInputConfig(
    val deadZone: Float = PacMazeConstants.JOYSTICK_DEAD_ZONE,
    val commitThreshold: Float = PacMazeConstants.JOYSTICK_COMMIT_THRESHOLD,
    /** 轻推提交：略低于 commitThreshold，稳定 1 tick 即可。 */
    val softCommitThreshold: Float = PacMazeConstants.JOYSTICK_SOFT_COMMIT_THRESHOLD,
    val stableTicksRequired: Int = PacMazeConstants.JOYSTICK_STABLE_TICKS,
    val spinSectorChanges: Int = PacMazeConstants.JOYSTICK_SPIN_SECTOR_CHANGES,
    val spinWindowTicks: Int = PacMazeConstants.JOYSTICK_SPIN_WINDOW_TICKS,
    val spinAngleDeg: Float = PacMazeConstants.JOYSTICK_SPIN_ANGLE_DEG,
    val spinReleaseStableTicks: Int = PacMazeConstants.JOYSTICK_SPIN_RELEASE_STABLE_TICKS,
    val spinBreakoutStrength: Float = PacMazeConstants.JOYSTICK_SPIN_BREAKOUT_STRENGTH,
    val spinBreakoutStableTicks: Int = PacMazeConstants.JOYSTICK_SPIN_BREAKOUT_STABLE_TICKS,
) {
    companion object {
        val Default = PacMazeInputConfig()

        /** 在线对战：低延迟、无转圈锁定，推杆即走。 */
        val OnlineVersus = PacMazeInputConfig(
            deadZone = 0.10f,
            commitThreshold = 0.18f,
            softCommitThreshold = 0.14f,
            stableTicksRequired = 1,
            spinSectorChanges = 99,
            spinAngleDeg = 999f,
            spinReleaseStableTicks = 1,
            spinBreakoutStrength = 0.5f,
            spinBreakoutStableTicks = 1,
        )
    }
}

enum class PacMazeInputMode {
    Idle,
    DeadZone,
    /** 扇区已识别但尚未提交：缓冲转向，保持当前滑行。 */
    Pending,
    /** 360° 转圈锁定：只更新朝向，不启动新移动。 */
    Spin,
    /** 扇区已稳定提交，驱动移动。 */
    Committed,
}
