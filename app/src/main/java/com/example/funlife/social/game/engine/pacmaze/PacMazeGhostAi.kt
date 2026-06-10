package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

object PacMazeGhostAi {

    fun pickDirection(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
        escapeOnly: Boolean = false,
    ): Direction? {
        val options = viableDirections(state, ghost, allowReverse = escapeOnly)
        if (options.isEmpty()) {
            return viableDirections(state, ghost, allowReverse = true).firstOrNull()
                ?: viableDirections(state, snapGhost(state, ghost), allowReverse = true).firstOrNull()
        }

        if (escapeOnly) {
            return options.firstOrNull { it != ghost.direction } ?: options.first()
        }

        val currentDir = ghost.direction
        if (currentDir != null && currentDir in options && !PacMazeMotion.isGhostDecisionPoint(state, ghost)) {
            return currentDir
        }

        return when (ghost.ghostMode) {
            GhostMode.FRIGHTENED -> options[rng.nextInt(options.size)]
            GhostMode.EATEN -> moveToward(options, ghost, level.pacSpawn.first, level.pacSpawn.second)
            GhostMode.SCATTER -> {
                val corner = scatterCorner(ghost.id, level.width, level.height)
                moveToward(options, ghost, corner.first, corner.second)
            }
            GhostMode.CHASE -> {
                if (rng.nextFloat() > level.aiAggression) {
                    options[rng.nextInt(options.size)]
                } else {
                    moveToward(
                        options,
                        ghost,
                        PacMazeMotion.tileX(pac.x),
                        PacMazeMotion.tileY(pac.y),
                    )
                }
            }
        }
    }

    private fun snapGhost(state: PacMazeWorldState, ghost: PacMazeEntity): PacMazeEntity =
        PacMazeMotion.snapToGrid(state, ghost, forGhost = true)

    private fun viableDirections(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        allowReverse: Boolean,
    ): List<Direction> = Direction.entries.filter { dir ->
        (allowReverse || dir != ghost.direction?.opposite()) &&
            PacMazeMotion.canMoveInDir(state, ghost.x, ghost.y, dir, forGhost = true)
    }

    private fun scatterCorner(ghostId: String, width: Int, height: Int): Pair<Int, Int> = when (ghostId) {
        "ghost_0" -> 1 to 1
        "ghost_1" -> width - 2 to 1
        "ghost_2" -> 1 to height - 2
        else -> width - 2 to height - 2
    }

    private fun moveToward(
        options: List<Direction>,
        ghost: PacMazeEntity,
        targetX: Int,
        targetY: Int,
    ): Direction {
        val gx = PacMazeMotion.tileX(ghost.x)
        val gy = PacMazeMotion.tileY(ghost.y)
        return options.minByOrNull { dir ->
            val (dx, dy) = dir.delta()
            abs((gx + dx) - targetX) + abs((gy + dy) - targetY)
        } ?: options.first()
    }
}
