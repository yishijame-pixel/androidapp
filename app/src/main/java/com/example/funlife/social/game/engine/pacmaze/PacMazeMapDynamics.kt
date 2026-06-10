package com.example.funlife.social.game.engine.pacmaze

/**
 * 动态地图逻辑：能量门开合、移动墙相位。
 * 逻辑地图不变，仅运行时阻挡状态变化。
 */
object PacMazeMapDynamics {

    const val GATE_CYCLE_TICKS = 300
    const val DYNAMIC_PHASE_TICKS = 200

    fun tick(state: PacMazeWorldState): PacMazeWorldState {
        val nextDynamics = state.dynamicsTick + 1
        var world = state.copy(dynamicsTick = nextDynamics)
        if (nextDynamics % GATE_CYCLE_TICKS == 0) {
            world = world.copy(energyGateOpen = !world.energyGateOpen)
        }
        return world
    }

    fun isTileBlocking(state: PacMazeWorldState, tile: TileType, x: Int, y: Int, forGhost: Boolean): Boolean =
        when (tile) {
            TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL -> true
            TileType.DOOR -> false
            TileType.DYNAMIC_WALL -> isDynamicWallClosed(state, x, y)
            TileType.ENERGY_GATE -> !state.energyGateOpen
            else -> false
        }

    private fun isDynamicWallClosed(state: PacMazeWorldState, x: Int, y: Int): Boolean {
        val phase = (state.dynamicsTick / DYNAMIC_PHASE_TICKS) % 4
        val wave = (x + y + phase) % 3
        return wave != 0
    }

    fun hasDynamicTiles(state: PacMazeWorldState): Boolean =
        state.tiles.any { code ->
            code == TileType.DYNAMIC_WALL.code || code == TileType.ENERGY_GATE.code
        }
}
