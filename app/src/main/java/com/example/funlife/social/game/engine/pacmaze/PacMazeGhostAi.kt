package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.engine.pacmaze.GhostBehaviorArchetype
import kotlin.math.abs

object PacMazeGhostAi {

    fun firstViableDirection(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        allowReverse: Boolean = true,
        preferAvoid: Direction? = null,
        speedCellsPerSec: Float = PacMazeConstants.GHOST_SPEED_CELLS_PER_SEC,
    ): Direction? {
        val probeStep = PacMazeMotion.ghostStepPerTick(speedCellsPerSec)
        val aligned = ghost.direction?.let { PacMazeMotion.snapLaneCenter(ghost, it) } ?: ghost
        val snapped = PacMazeMotion.snapToGrid(state, aligned, forGhost = true)
        val candidates = listOf(snapped, aligned, ghost)
        for (candidate in candidates) {
            val options = viableDirections(state, candidate, allowReverse = allowReverse, probeStep = probeStep)
            val picked = options.firstOrNull { it != preferAvoid } ?: options.firstOrNull()
            if (picked != null) return picked
        }
        return null
    }

    fun pickDirection(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
        escapeOnly: Boolean = false,
        allowReverse: Boolean = false,
        speedCellsPerSec: Float = PacMazeConstants.GHOST_SPEED_CELLS_PER_SEC,
    ): Direction? {
        val probeStep = PacMazeMotion.ghostStepPerTick(speedCellsPerSec)
        val options = viableDirections(state, ghost, allowReverse = allowReverse, probeStep = probeStep)
        if (options.isEmpty()) {
            return firstViableDirection(
                state = state,
                ghost = ghost,
                allowReverse = true,
                preferAvoid = if (escapeOnly) ghost.direction else null,
                speedCellsPerSec = speedCellsPerSec,
            )
        }

        if (escapeOnly) {
            return pickEscapeDirection(state, ghost, pac, rng, level, options, allowReverse, probeStep)
        }

        val forceRandom = ghost.ghostStuckTicks >= PacMazeGhostPathfind.STUCK_FORCE_TICKS
        if (forceRandom) {
            return options[rng.nextInt(options.size)]
        }

        val currentDir = ghost.direction
        if (currentDir != null && currentDir in options && !escapeOnly && !allowReverse &&
            !PacMazeMotion.isGhostDecisionPoint(state, ghost)
        ) {
            return currentDir
        }

        val stuckEscape = ghost.ghostStuckTicks >= PacMazeGhostPathfind.STUCK_ESCAPE_TICKS
        val canReverse = allowReverse || stuckEscape

        return when (ghost.ghostMode) {
            GhostMode.FRIGHTENED -> {
                PacMazeGhostPathfind.nextStepAway(
                    state,
                    ghost,
                    PacMazeMotion.tileX(pac.x),
                    PacMazeMotion.tileY(pac.y),
                    options,
                ) ?: options[rng.nextInt(options.size)]
            }
            GhostMode.EATEN -> moveToward(
                state, ghost, level.pacSpawn.first, level.pacSpawn.second, options, canReverse,
            )
            GhostMode.SCATTER -> if (level.id <= 0) {
                pickChaseDirection(state, ghost, pac, rng, level, options, canReverse)
            } else {
                val corner = PacMazeGhostRoster.scatterCorner(ghost.ghostKind, level.width, level.height)
                moveToward(state, ghost, corner.first, corner.second, options, canReverse)
            }
            GhostMode.CHASE -> pickChaseDirection(
                state, ghost, pac, rng, level, options, canReverse,
            )
        }
    }

    private fun pickEscapeDirection(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
        options: List<Direction>,
        allowReverse: Boolean,
        probeStep: Float,
    ): Direction {
        val reverseAllowed = allowReverse || ghost.ghostStuckTicks >= PacMazeGhostPathfind.STUCK_ESCAPE_TICKS
        val allOptions = if (reverseAllowed) {
            options
        } else {
            viableDirections(state, ghost, allowReverse = true, probeStep = probeStep).ifEmpty { options }
        }
        val target = when (ghost.ghostMode) {
            GhostMode.EATEN -> level.pacSpawn
            GhostMode.SCATTER -> if (level.id <= 0) {
                chaseTarget(state, level, ghost, pac)
            } else {
                PacMazeGhostRoster.scatterCorner(ghost.ghostKind, level.width, level.height)
            }
            else -> chaseTarget(state, level, ghost, pac)
        }
        val path = PacMazeGhostPathfind.nextStepToward(
            state, ghost, target.first, target.second, allOptions, allowReverse = reverseAllowed,
        )
        if (path != null) return path
        return allOptions.firstOrNull { it != ghost.direction }
            ?: allOptions.firstOrNull()
            ?: options.first()
    }

    private fun pickChaseDirection(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
        options: List<Direction>,
        allowReverse: Boolean,
    ): Direction {
        val useGateAi = ghost.ghostSpecialty == GhostSpecialty.GATE_KEEPER ||
            ghost.ghostKind.behaviorArchetype == GhostBehaviorArchetype.FLANKER ||
            ghost.ghostKind.behaviorArchetype == GhostBehaviorArchetype.OPPORTUNIST
        if (useGateAi && level.id >= 14) {
            val ambush = PacMazeGhostGateAi.ambushTarget(state, level, ghost, pac)
            val ambushWeight = PacMazeGhostGateAi.effectiveAmbushWeight(state, level, ghost)
            if (ambush != null && rng.nextFloat() < level.aiAggression * ambushWeight) {
                return moveToward(state, ghost, ambush.first, ambush.second, options, allowReverse)
            }
        }

        if (level.id > 0 &&
            rng.nextFloat() > level.aiAggression &&
            ghost.ghostKind.behaviorArchetype != GhostBehaviorArchetype.STRIKER
        ) {
            return options[rng.nextInt(options.size)]
        }

        val target = chaseTarget(state, level, ghost, pac)
        return moveToward(state, ghost, target.first, target.second, options, allowReverse)
    }

    private fun chaseTarget(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
    ): Pair<Int, Int> = when (ghost.ghostKind.behaviorArchetype) {
        GhostBehaviorArchetype.STRIKER -> PacMazeMotion.tileX(pac.x) to PacMazeMotion.tileY(pac.y)
        GhostBehaviorArchetype.PREDICTOR -> predictiveTile(pac)
        GhostBehaviorArchetype.FLANKER -> flankTile(state, level, pac)
        GhostBehaviorArchetype.OPPORTUNIST -> opportunistTile(state, level, pac)
    }

    private fun predictiveTile(pac: PacMazeEntity): Pair<Int, Int> {
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        val dir = pac.direction ?: pac.facing
        val (dx, dy) = dir.delta()
        val lead = PacMazeGhostRoster.PREDICTOR_LOOKAHEAD_TILES
        return (px + dx * lead).coerceIn(0, 999) to (py + dy * lead).coerceIn(0, 999)
    }

    private fun flankTile(state: PacMazeWorldState, level: PacMazeLevelConfig, pac: PacMazeEntity): Pair<Int, Int> {
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        val checkpoint = level.markers
            .filter { it.kind == PacMazeMarkerKind.CHECKPOINT || it.kind == PacMazeMarkerKind.EXIT }
            .minByOrNull { abs(it.x - px) + abs(it.y - py) }
        if (checkpoint == null) return px to py
        val midX = (px + checkpoint.x) / 2
        val midY = (py + checkpoint.y) / 2
        return midX to midY
    }

    private fun opportunistTile(state: PacMazeWorldState, level: PacMazeLevelConfig, pac: PacMazeEntity): Pair<Int, Int> {
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        if (state.itemSpawners.isNotEmpty()) {
            val spawner = state.itemSpawners.minByOrNull { abs(it.x - px) + abs(it.y - py) }
            if (spawner != null && abs(spawner.x - px) + abs(spawner.y - py) <= 8) {
                return spawner.x to spawner.y
            }
        }
        return px to py
    }

    private fun snapGhost(state: PacMazeWorldState, ghost: PacMazeEntity): PacMazeEntity =
        PacMazeMotion.snapToGrid(state, ghost, forGhost = true)

    private fun viableDirections(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        allowReverse: Boolean,
        probeStep: Float,
    ): List<Direction> = Direction.entries.filter { dir ->
        (allowReverse || dir != ghost.direction?.opposite()) &&
            PacMazeMotion.canMoveInDir(
                state, ghost.x, ghost.y, dir, forGhost = true, ghost = ghost, probeStep = probeStep,
            )
    }

    private fun moveToward(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        targetX: Int,
        targetY: Int,
        options: List<Direction>,
        allowReverse: Boolean,
    ): Direction {
        val pathDir = PacMazeGhostPathfind.nextStepToward(
            state, ghost, targetX, targetY, options, allowReverse = allowReverse,
        )
        if (pathDir != null) return pathDir

        val gx = PacMazeMotion.tileX(ghost.x)
        val gy = PacMazeMotion.tileY(ghost.y)
        return options.minByOrNull { dir ->
            val (dx, dy) = dir.delta()
            val nx = gx + dx
            val ny = gy + dy
            var cost = abs(nx - targetX) + abs(ny - targetY).toFloat()
            if (dir == ghost.direction) cost -= 0.4f
            if (state.tileAt(nx, ny) == TileType.DYNAMIC_WALL &&
                !PacMazeMapDynamics.isDynamicStripeOpen(state, nx, ny) &&
                ghost.ghostSpecialty != GhostSpecialty.PHASE_WALKER
            ) {
                cost += 10f
            }
            if (state.tileAt(nx, ny) == TileType.ENERGY_GATE && !state.energyGateOpen) {
                cost += if (ghost.ghostSpecialty == GhostSpecialty.GATE_KEEPER) 2f else 8f
            }
            cost
        } ?: options.first()
    }
}
