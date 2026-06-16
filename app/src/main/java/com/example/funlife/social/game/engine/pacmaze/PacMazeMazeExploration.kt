package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

/** 迷宫探索：战争迷雾 + 回声雷达 */
object PacMazeMazeExploration {

    const val RADAR_REVEAL_TICKS = 180
    const val RADAR_COOLDOWN_TICKS = 720
    const val RADAR_RADIUS = 5

    fun tileIndex(width: Int, x: Int, y: Int): Int = y * width + x

    fun initExplored(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (!level.modeRules.fogEnabled) return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        return revealAround(state, level, tx, ty, level.modeRules.fogRadius)
    }

    fun tick(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (!level.modeRules.fogEnabled) return state
        var world = state
        if (world.radarRevealTicksLeft > 0) {
            world = world.copy(radarRevealTicksLeft = world.radarRevealTicksLeft - 1)
        }
        if (world.radarCooldownTicksLeft > 0) {
            world = world.copy(radarCooldownTicksLeft = world.radarCooldownTicksLeft - 1)
        }
        val pac = world.entities.firstOrNull { it.role == "pac" } ?: return world
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        world = revealAround(world, level, tx, ty, level.modeRules.fogRadius)
        if (world.radarRevealTicksLeft > 0) {
            world = revealAround(world, level, tx, ty, RADAR_RADIUS)
        }
        return world
    }

    fun tickForEntity(state: PacMazeWorldState, level: PacMazeLevelConfig, pac: PacMazeEntity): PacMazeWorldState {
        if (!level.modeRules.fogEnabled) return state
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        return revealAround(state, level, tx, ty, level.modeRules.fogRadius)
    }

    fun tryPulseRadar(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (!level.modeRules.radarEnabled) return state
        if (state.radarCooldownTicksLeft > 0 || state.radarRevealTicksLeft > 0) return state
        return state.copy(
            radarRevealTicksLeft = RADAR_REVEAL_TICKS,
            radarCooldownTicksLeft = (RADAR_COOLDOWN_TICKS * level.modeRules.radarCooldownMultiplier).toInt(),
        )
    }

    fun isTileVisible(state: PacMazeWorldState, level: PacMazeLevelConfig, x: Int, y: Int): Boolean {
        if (!level.modeRules.fogEnabled) return true
        val idx = tileIndex(state.width, x, y)
        if (idx in state.exploredTiles) return true
        if (state.radarRevealTicksLeft <= 0) return false
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return false
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        return abs(x - px) + abs(y - py) <= RADAR_RADIUS
    }

    private fun revealAround(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        centerX: Int,
        centerY: Int,
        radius: Int,
    ): PacMazeWorldState {
        val explored = state.exploredTiles.toMutableSet()
        var changed = false
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                if (abs(dx) + abs(dy) > radius) continue
                val x = centerX + dx
                val y = centerY + dy
                if (x !in 0 until state.width || y !in 0 until state.height) continue
                val idx = tileIndex(state.width, x, y)
                if (explored.add(idx)) changed = true
            }
        }
        return if (changed) state.copy(exploredTiles = explored) else state
    }
}
