package com.example.funlife.social.game.engine.pacmaze

/**
 * 玩家移动模式。
 * - AUTO: 松手后沿最后有效方向持续滑行，更接近经典吃豆人。
 * - MANUAL: 松手立即停，完全由用户按住/滑动控制。
 */
enum class PacMazeMovementMode {
    AUTO,
    MANUAL;

    companion object {
        val Default = AUTO
    }
}
