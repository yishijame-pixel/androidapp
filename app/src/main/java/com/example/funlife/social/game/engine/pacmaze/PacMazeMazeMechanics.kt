package com.example.funlife.social.game.engine.pacmaze

/** 迷宫专属机制：回声豆、封印钥印、情报点、追猎升级。 */
object PacMazeMazeMechanics {

    private const val ECHO_HINT_TICKS = 180
    private const val SEALED_REJECT_FLASH_TICKS = 45
    private const val HUNT_PHASE_SECONDS = 30

    fun onPelletEaten(state: PacMazeWorldState, level: PacMazeLevelConfig, tx: Int, ty: Int): PacMazeWorldState {
        if (!level.modeRules.hintPelletsEnabled) return state
        val isHint = level.markers.any { it.tag == "HINT" && it.x == tx && it.y == ty }
        if (!isHint) return state
        val target = nextUnvisitedKey(state, level) ?: return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val dir = directionToward(pac.x, pac.y, target.first.toFloat(), target.second.toFloat())
        return state.copy(
            echoHintTicksLeft = ECHO_HINT_TICKS,
            echoHintDirection = dir,
            echoTargetKeyTag = target.third,
        )
    }

    fun tick(state: PacMazeWorldState, level: PacMazeLevelConfig, elapsedSeconds: Int): PacMazeWorldState {
        var world = state
        if (world.echoHintTicksLeft > 0) {
            world = world.copy(echoHintTicksLeft = world.echoHintTicksLeft - 1)
        }
        if (world.sealedKeyRejectFlashTicks > 0) {
            world = world.copy(sealedKeyRejectFlashTicks = world.sealedKeyRejectFlashTicks - 1)
        }
        if (level.modeRules.huntEscalation) {
            val phase = (elapsedSeconds / HUNT_PHASE_SECONDS).coerceAtMost(4)
            if (phase != world.huntPhase) {
                world = world.copy(huntPhase = phase)
            }
        }
        return world
    }

    fun huntGhostSpeedMul(state: PacMazeWorldState, level: PacMazeLevelConfig): Float =
        if (!level.modeRules.huntEscalation) 1f else 1f + state.huntPhase * 0.12f

    fun huntExtraGhosts(level: PacMazeLevelConfig, elapsedSeconds: Int): Int {
        if (!level.modeRules.huntEscalation) return 0
        return (elapsedSeconds / HUNT_PHASE_SECONDS).coerceAtMost(2)
    }

    fun tryVisitCheckpoint(state: PacMazeWorldState, level: PacMazeLevelConfig, tag: String): PacMazeCheckpointResult {
        if (!level.modeRules.sealedKeyOrder) {
            return PacMazeCheckpointResult(accepted = true)
        }
        val order = level.modeRules.orderedKeyTags
        if (order.isEmpty()) return PacMazeCheckpointResult(accepted = true)
        val nextTag = order.firstOrNull { it !in state.visitedCheckpointTags } ?: return PacMazeCheckpointResult(accepted = false)
        return if (tag == nextTag) {
            PacMazeCheckpointResult(accepted = true)
        } else {
            PacMazeCheckpointResult(accepted = false, rejectFlash = true)
        }
    }

    fun spendIntelRevealQuadrant(state: PacMazeWorldState, level: PacMazeLevelConfig, quadrant: Int): PacMazeWorldState {
        if (level.modeRules.intelPointsMax <= 0) return state
        if (state.intelPointsRemaining <= 0) return state
        if (quadrant in state.intelQuadrantsRevealed) return state
        val w = state.width
        val h = state.height
        val explored = state.exploredTiles.toMutableSet()
        val qx0 = if (quadrant % 2 == 0) 0 else w / 2
        val qx1 = if (quadrant % 2 == 0) w / 2 else w
        val qy0 = if (quadrant < 2) 0 else h / 2
        val qy1 = if (quadrant < 2) h / 2 else h
        for (y in qy0 until qy1) {
            for (x in qx0 until qx1) {
                explored.add(y * w + x)
            }
        }
        return state.copy(
            intelPointsRemaining = state.intelPointsRemaining - 1,
            intelQuadrantsRevealed = state.intelQuadrantsRevealed + quadrant,
            exploredTiles = explored,
        )
    }

    fun spendIntelKeyDistance(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (level.modeRules.intelPointsMax <= 0 || state.intelPointsRemaining < 2) return state
        val target = nextUnvisitedKey(state, level) ?: return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val dir = directionToward(pac.x, pac.y, target.first.toFloat(), target.second.toFloat())
        return state.copy(
            intelPointsRemaining = state.intelPointsRemaining - 2,
            echoHintTicksLeft = ECHO_HINT_TICKS,
            echoHintDirection = dir,
            echoTargetKeyTag = target.third,
        )
    }

    fun nearestGhostDistanceTiles(state: PacMazeWorldState): Int? {
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return null
        val px = PacMazeMotion.tileX(pac.x)
        val py = PacMazeMotion.tileY(pac.y)
        return state.entities
            .filter { it.role == "ghost" && it.ghostMode != GhostMode.EATEN }
            .minOfOrNull { ghost ->
                val gx = PacMazeMotion.tileX(ghost.x)
                val gy = PacMazeMotion.tileY(ghost.y)
                kotlin.math.abs(gx - px) + kotlin.math.abs(gy - py)
            }
    }

    private fun nextUnvisitedKey(state: PacMazeWorldState, level: PacMazeLevelConfig): Triple<Int, Int, String>? {
        val order = if (level.modeRules.orderedKeyTags.isNotEmpty()) {
            level.modeRules.orderedKeyTags
        } else {
            level.modeRules.requiredKeyTags.toList()
        }
        val nextTag = order.firstOrNull { it !in state.visitedCheckpointTags } ?: return null
        val marker = level.markers.firstOrNull { it.tag == nextTag && it.kind == PacMazeMarkerKind.CHECKPOINT }
            ?: return null
        return Triple(marker.x, marker.y, nextTag)
    }

    private fun directionToward(fx: Float, fy: Float, tx: Float, ty: Float): Direction {
        val dx = tx - fx
        val dy = ty - fy
        return if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
            if (dx >= 0) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy >= 0) Direction.DOWN else Direction.UP
        }
    }
}

data class PacMazeCheckpointResult(
    val accepted: Boolean,
    val rejectFlash: Boolean = false,
)
