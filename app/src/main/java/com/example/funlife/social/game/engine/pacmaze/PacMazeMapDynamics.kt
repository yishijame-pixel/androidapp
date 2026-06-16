package com.example.funlife.social.game.engine.pacmaze

/**
 * 动态地图逻辑：能量门开合、移动墙相位。
 * 逻辑地图不变，仅运行时阻挡状态变化。
 */
object PacMazeMapDynamics {

    const val GATE_CYCLE_TICKS = 300
    const val DYNAMIC_PHASE_TICKS = 200
    private const val DYNAMIC_STRIPE_COUNT = 3

    /** 高关卡移动墙切换更快，形成更明显的机关节奏。 */
    fun dynamicPhaseTicks(levelId: Int, speedMul: Float = 1f): Int =
        ((DYNAMIC_PHASE_TICKS - (levelId - 1) * 5).coerceIn(70, DYNAMIC_PHASE_TICKS) / speedMul.coerceAtLeast(0.5f)).toInt().coerceAtLeast(50)

    fun tick(state: PacMazeWorldState): PacMazeWorldState {
        val nextDynamics = state.dynamicsTick + 1
        var world = state.copy(dynamicsTick = nextDynamics)
        if (state.levelId >= 20) {
            val rate = dynamicPhaseTicks(state.levelId, state.dynamicWallSpeedMul)
            val phase = (nextDynamics / rate) % DYNAMIC_STRIPE_COUNT
            world = world.copy(energyGateOpen = phase == 0)
        } else if (nextDynamics % GATE_CYCLE_TICKS == 0) {
            world = world.copy(energyGateOpen = !world.energyGateOpen)
        }
        return world
    }

    fun isTileBlocking(state: PacMazeWorldState, tile: TileType, x: Int, y: Int, forGhost: Boolean, ghost: PacMazeEntity? = null): Boolean =
        when (tile) {
            TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL -> true
            TileType.DOOR -> false
            TileType.DYNAMIC_WALL -> {
                val closed = isDynamicWallClosed(state, x, y)
                if (forGhost && closed && ghost?.ghostSpecialty == GhostSpecialty.PHASE_WALKER &&
                    ghost.phaseWalkCooldownTicksLeft <= 0
                ) {
                    false
                } else {
                    closed
                }
            }
            TileType.ENERGY_GATE -> !state.energyGateOpen
            else -> false
        }

    fun currentDynamicPhase(state: PacMazeWorldState): Int {
        val rate = dynamicPhaseTicks(state.levelId, state.dynamicWallSpeedMul)
        return ((state.dynamicsTick / rate) % DYNAMIC_STRIPE_COUNT).toInt()
    }

    fun isDynamicStripeOpen(state: PacMazeWorldState, x: Int, y: Int): Boolean {
        val open = (x + y) % DYNAMIC_STRIPE_COUNT == currentDynamicPhase(state)
        return if (state.mirrorDynamicWalls) !open else open
    }

    /** 距下一相位开放该条纹格还需多少逻辑帧；已开放则 0。 */
    fun ticksUntilDynamicStripeOpen(state: PacMazeWorldState, x: Int, y: Int): Int {
        if (isDynamicStripeOpen(state, x, y)) return 0
        val rate = dynamicPhaseTicks(state.levelId, state.dynamicWallSpeedMul)
        return rate - (state.dynamicsTick % rate)
    }

    private fun isDynamicWallClosed(state: PacMazeWorldState, x: Int, y: Int): Boolean =
        !isDynamicStripeOpen(state, x, y)

    fun hasDynamicTiles(state: PacMazeWorldState): Boolean =
        state.tiles.any { code ->
            code == TileType.DYNAMIC_WALL.code || code == TileType.ENERGY_GATE.code
        }
}
