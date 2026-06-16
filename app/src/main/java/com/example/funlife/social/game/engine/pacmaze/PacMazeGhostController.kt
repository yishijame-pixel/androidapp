package com.example.funlife.social.game.engine.pacmaze

/**
 * 单人/闯关幽灵移动统一控制器：速度解算、路口决策、卡死脱困与亚像素积分。
 */
object PacMazeGhostController {

    fun resolveSpeed(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        ghost: PacMazeEntity,
        mode: GhostMode,
    ): Float {
        var kindMul = ghost.ghostKind.speedMul
        if (ghost.ghostKind.behaviorArchetype == GhostBehaviorArchetype.OPPORTUNIST &&
            ghost.opportunistBurstTicksLeft > 0
        ) {
            kindMul *= PacMazeGhostRoster.OPPORTUNIST_BURST_SPEED_MUL
        }
        val huntMul = PacMazeMazeMechanics.huntGhostSpeedMul(state, level)
        return PacMazeConstants.ghostSpeedCellsPerSec(mode, level.ghostSpeedMul * kindMul * huntMul)
    }

    fun tick(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
    ): PacMazeEntity {
        var current = entity
        val mode = when {
            state.powerTicksLeft > 0 && current.ghostMode != GhostMode.EATEN -> GhostMode.FRIGHTENED
            current.ghostMode == GhostMode.EATEN -> GhostMode.EATEN
            else -> state.ghostMode
        }
        val speed = resolveSpeed(state, level, current, mode)
        val probeStep = PacMazeMotion.ghostStepPerTick(speed)
        val currentDir = current.direction

        fun isBlocked(entity: PacMazeEntity, dir: Direction?): Boolean {
            if (dir == null) return true
            return !PacMazeMotion.canMoveInDir(
                state, entity.x, entity.y, dir,
                forGhost = true, ghost = entity, probeStep = probeStep,
            )
        }

        val atDecision = PacMazeMotion.isGhostDecisionPoint(state, current)
        val tileKey = PacMazeMotion.tileX(current.x) * 10000 + PacMazeMotion.tileY(current.y)
        val atFreshDecision = atDecision && tileKey != current.ghostDecisionTileKey
        val blockedNow = isBlocked(current, currentDir)
        val stuckEscape = current.ghostStuckTicks >= PacMazeGhostPathfind.STUCK_ESCAPE_TICKS &&
            current.ghostStuckTicks % PacMazeGhostPathfind.STUCK_ESCAPE_TICKS == 0

        val mustRepick = when {
            currentDir == null -> true
            atFreshDecision -> true
            stuckEscape -> true
            blockedNow -> true
            else -> false
        }

        if (mustRepick) {
            val allowReverse = blockedNow || stuckEscape || level.id <= 0
            val escapeOnly = blockedNow && !atFreshDecision

            var picked = PacMazeGhostAi.pickDirection(
                state = state,
                ghost = current.copy(ghostMode = mode),
                pac = pac,
                rng = rng,
                level = level,
                escapeOnly = escapeOnly,
                allowReverse = allowReverse,
                speedCellsPerSec = speed,
            )
            if (picked == null || (blockedNow && isBlocked(current, picked))) {
                picked = PacMazeGhostAi.firstViableDirection(
                    state = state,
                    ghost = current,
                    allowReverse = true,
                    preferAvoid = if (blockedNow) currentDir else null,
                    speedCellsPerSec = speed,
                )
            }

            val nextDir = picked ?: currentDir
            if (nextDir != null) {
                if (blockedNow) {
                    current = PacMazeMotion.snapLaneCenter(current, nextDir)
                    if (isBlocked(current, nextDir)) {
                        current = PacMazeMotion.snapToGrid(state, current, forGhost = true)
                    }
                }
                current = current.copy(
                    ghostMode = mode,
                    speed = speed,
                    direction = nextDir,
                    facing = nextDir,
                    ghostDecisionTileKey = if (atFreshDecision) tileKey else current.ghostDecisionTileKey,
                )
            } else {
                current = current.copy(ghostMode = mode, speed = speed)
            }
        } else {
            current = current.copy(ghostMode = mode, speed = speed)
        }

        var dir = current.direction ?: return current
        if (isBlocked(current, dir)) {
            val recovered = recoverBlocked(
                state = state,
                current = current,
                dir = dir,
                mode = mode,
                speed = speed,
                isBlocked = ::isBlocked,
            )
            if (recovered == null) {
                return current.copy(
                    ghostMode = mode,
                    speed = speed,
                    velX = 0f,
                    velY = 0f,
                    ghostStuckTicks = current.ghostStuckTicks + 1,
                )
            }
            current = recovered.entity
            dir = recovered.direction
        }

        val beforeTileX = PacMazeMotion.tileX(current.x)
        val beforeTileY = PacMazeMotion.tileY(current.y)
        val moved = PacMazePortals.applyTransit(
            state,
            PacMazeMotion.tickGhost(state, current, dir, speed),
            level,
        )
        val afterTileX = PacMazeMotion.tileX(moved.x)
        val afterTileY = PacMazeMotion.tileY(moved.y)
        val progressed = moved.x != current.x || moved.y != current.y
        val stuckTicks = if (!progressed && beforeTileX == afterTileX && beforeTileY == afterTileY) {
            current.ghostStuckTicks + 1
        } else {
            0
        }
        val leftTile = beforeTileX != afterTileX || beforeTileY != afterTileY
        return moved.copy(
            facing = moved.direction ?: dir,
            ghostStuckTicks = stuckTicks,
            ghostDecisionTileKey = if (leftTile) -1 else moved.ghostDecisionTileKey,
        )
    }

    private data class RecoveredGhost(val entity: PacMazeEntity, val direction: Direction)

    private fun recoverBlocked(
        state: PacMazeWorldState,
        current: PacMazeEntity,
        dir: Direction,
        mode: GhostMode,
        speed: Float,
        isBlocked: (PacMazeEntity, Direction?) -> Boolean,
    ): RecoveredGhost? {
        var aligned = PacMazeMotion.snapLaneCenter(current, dir)
        if (!isBlocked(aligned, dir)) {
            return RecoveredGhost(
                aligned.copy(ghostMode = mode, speed = speed, direction = dir, facing = dir),
                dir,
            )
        }
        aligned = PacMazeMotion.snapToGrid(state, aligned, forGhost = true)
        val emergency = PacMazeGhostAi.firstViableDirection(
            state = state,
            ghost = aligned,
            allowReverse = true,
            preferAvoid = dir,
            speedCellsPerSec = speed,
        ) ?: Direction.entries.firstOrNull { candidate ->
            !isBlocked(aligned, candidate)
        } ?: return null
        return RecoveredGhost(
            aligned.copy(
                ghostMode = mode,
                speed = speed,
                direction = emergency,
                facing = emergency,
                velX = 0f,
                velY = 0f,
                ghostStuckTicks = current.ghostStuckTicks + 1,
            ),
            emergency,
        )
    }
}
