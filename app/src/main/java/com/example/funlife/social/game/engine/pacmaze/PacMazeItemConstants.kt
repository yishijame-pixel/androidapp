package com.example.funlife.social.game.engine.pacmaze

object PacMazeItemConstants {
    const val SPAWNER_INTERVAL_TICKS = 480
    const val FLOOR_LIFETIME_TICKS = 540
    const val MAGNET_DURATION_TICKS = 300
    const val MAGNET_RADIUS_CELLS = 4.5f
    /** 磁力吸附飞行速度（格/秒）。 */
    const val MAGNET_PULL_SPEED_CELLS_PER_SEC = 10.5f
    /** 吸附到玩家中心的最小距离（格）。 */
    const val MAGNET_COLLECT_RADIUS_CELLS = 0.32f
    const val SHIELD_STACK = 1
    const val FROST_DURATION_TICKS = 240
    const val SPEED_DURATION_TICKS = 360
    const val SPEED_MULTIPLIER = 1.38f
    const val DOUBLE_DURATION_TICKS = 420
    const val CHARGE_BONUS = 2
    const val ITEM_PICKUP_SCORE = 25
    const val MAX_FLOOR_ITEMS = 6
}
