package com.example.funlife.social.game.engine.pacmaze

object PacMazeRules {

    fun isWalkable(state: PacMazeWorldState, x: Int, y: Int, forGhost: Boolean = false, ghost: PacMazeEntity? = null): Boolean {
        if (x !in 0 until state.width || y !in 0 until state.height) return false
        val tile = state.tileAt(x, y)
        return !PacMazeMapDynamics.isTileBlocking(state, tile, x, y, forGhost, ghost)
    }

    fun canTurn(state: PacMazeWorldState, x: Int, y: Int, dir: Direction, forGhost: Boolean = false): Boolean {
        val (dx, dy) = dir.delta()
        return isWalkable(state, x + dx, y + dy, forGhost)
    }

    fun isCentered(value: Float): Boolean = kotlin.math.abs(value - value.toInt() - 0.5f) < 0.08f ||
        kotlin.math.abs(value - value.toInt()) < 0.08f

    fun alignedCoord(value: Float): Int = if (value - value.toInt() < 0.5f) value.toInt() else (value + 0.5f).toInt()

    fun tileIndex(state: PacMazeWorldState, x: Float, y: Float): Int {
        val tx = x.toInt().coerceIn(0, state.width - 1)
        val ty = y.toInt().coerceIn(0, state.height - 1)
        return ty * state.width + tx
    }

    fun eatPellet(
        state: PacMazeWorldState,
        x: Float,
        y: Float,
        winCondition: PacMazeWinCondition = PacMazeWinCondition.CLEAR_PELLETS,
        level: PacMazeLevelConfig? = null,
    ): PacMazeWorldState {
        val tx = PacMazeMotion.tileX(x)
        val ty = PacMazeMotion.tileY(y)
        if (tx !in 0 until state.width || ty !in 0 until state.height) return state
        val idx = ty * state.width + tx
        val tile = state.tiles[idx]
        if (tile != TileType.PELLET.code && tile != TileType.POWER.code) return state
        val newTiles = state.tiles.copyOf()
        newTiles[idx] = TileType.EMPTY.code
        val addScore = if (tile == TileType.POWER.code) {
            PacMazeConstants.POWER_SCORE
        } else {
            PacMazeItems.pelletScore(state)
        }
        val power = if (tile == TileType.POWER.code) {
            PacMazeConstants.POWER_DURATION_TICKS
        } else {
            state.powerTicksLeft
        }
        val attackCharges = if (tile == TileType.POWER.code) {
            state.attackCharges + 1
        } else {
            state.attackCharges
        }
        val pelletsLeft = (state.pelletsRemaining - 1).coerceAtLeast(0)
        val phase = if (pelletsLeft == 0 && state.phase == PacMazePhase.PLAYING && winCondition == PacMazeWinCondition.CLEAR_PELLETS) {
            PacMazePhase.LEVEL_CLEAR
        } else {
            state.phase
        }
        return state.copy(
            tiles = newTiles,
            score = state.score + addScore,
            pelletsRemaining = pelletsLeft,
            powerTicksLeft = power,
            attackCharges = attackCharges,
            phase = phase,
        ).let { eaten ->
            if (tile == TileType.PELLET.code && level != null) {
                PacMazeMazeMechanics.onPelletEaten(eaten, level, tx, ty)
            } else {
                eaten
            }
        }
    }

    fun checkExitReached(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (level.modeRules.winCondition != PacMazeWinCondition.REACH_EXIT) return state
        if (state.phase != PacMazePhase.PLAYING) return state
        val required = level.modeRules.requiredKeyTags
        if (required.isNotEmpty() && !required.all { it in state.visitedCheckpointTags }) {
            return state
        }
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        val onExit = level.markers.any { it.kind == PacMazeMarkerKind.EXIT && it.x == tx && it.y == ty }
        return if (onExit) state.copy(phase = PacMazePhase.LEVEL_CLEAR) else state
    }

    fun checkTimeLimit(state: PacMazeWorldState, level: PacMazeLevelConfig, elapsedSeconds: Int): PacMazeWorldState {
        val limit = level.modeRules.timeLimitSeconds
        if (limit <= 0 || state.phase != PacMazePhase.PLAYING) return state
        return if (elapsedSeconds >= limit) state.copy(phase = PacMazePhase.GAME_OVER) else state
    }
}
