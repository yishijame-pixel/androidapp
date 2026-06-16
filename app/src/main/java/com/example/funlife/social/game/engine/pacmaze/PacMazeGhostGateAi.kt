package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

/**
 * 高关闸道博弈：幽灵在翼舱入口蹲守，与玩家在移动墙相位上博弈。
 */
object PacMazeGhostGateAi {

    fun ambushTarget(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        ghost: PacMazeEntity,
        pac: PacMazeEntity,
    ): Pair<Int, Int>? {
        if (level.id < 14) return null
        val wings = level.markers.filter {
            it.kind == PacMazeMarkerKind.CHECKPOINT &&
                (it.tag.endsWith("-W") || it.tag.endsWith("-E"))
        }
        if (wings.isEmpty()) return null

        val cx = level.width / 2
        val pacX = PacMazeMotion.tileX(pac.x)
        val homeWing = when {
            PacMazeGhostRoster.prefersWestWing(ghost.ghostKind, ghost.ghostSpecialty) ->
                wings.firstOrNull { it.tag.endsWith("-W") }
            PacMazeGhostRoster.prefersEastWing(ghost.ghostKind, ghost.ghostSpecialty) ->
                wings.firstOrNull { it.tag.endsWith("-E") }
            else -> wings.firstOrNull { (it.x < cx) == (PacMazeMotion.tileX(ghost.x) < cx) }
        } ?: wings.first()

        val pacOppositeWing = (pacX < cx) != (homeWing.x < cx)
        if (!pacOppositeWing) return null

        val approach = gateApproachTile(level, homeWing) ?: return homeWing.x to homeWing.y
        if (PacMazeRules.isWalkable(state, approach.first, approach.second, forGhost = true, ghost = ghost)) {
            return approach
        }
        return holdNearGate(state, ghost, homeWing, approach)
    }

    /** 闸道关闭时在入口附近蹲守，临近开放时仍指向蹲守点以便第一时间冲入。 */
    private fun holdNearGate(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        wing: PacMazeMapMarker,
        approach: Pair<Int, Int>,
    ): Pair<Int, Int>? {
        val gx = PacMazeMotion.tileX(ghost.x)
        val gy = PacMazeMotion.tileY(ghost.y)
        val dist = abs(gx - wing.x) + abs(gy - wing.y)
        if (dist > 5) return null

        val rate = PacMazeMapDynamics.dynamicPhaseTicks(state.levelId)
        val ticksLeft = PacMazeMapDynamics.ticksUntilDynamicStripeOpen(state, approach.first, approach.second)
        if (ticksLeft <= rate / 4) return approach

        if (PacMazeRules.isWalkable(state, gx, gy, forGhost = true, ghost = ghost)) return gx to gy
        return null
    }

    /** 蹲守点：翼舱 checkpoint 朝主轴方向一格。 */
    private fun gateApproachTile(level: PacMazeLevelConfig, wing: PacMazeMapMarker): Pair<Int, Int>? {
        val cx = level.width / 2
        val dx = if (wing.x < cx) 1 else if (wing.x > cx) -1 else 0
        val approachX = wing.x + dx
        val approachY = wing.y
        if (approachX in 0 until level.width && approachY in 0 until level.height) {
            return approachX to approachY
        }
        return null
    }

    fun ambushWeight(level: PacMazeLevelConfig): Float =
        when {
            level.id >= 20 -> 0.72f
            level.id >= 17 -> 0.62f
            level.id >= 14 -> 0.52f
            else -> 0f
        }

    /** 结合移动墙相位：闸道即将开放或已开放时提高蹲守倾向。 */
    fun effectiveAmbushWeight(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        ghost: PacMazeEntity,
    ): Float {
        val base = ambushWeight(level)
        if (base <= 0f) return 0f

        val wings = level.markers.filter {
            it.kind == PacMazeMarkerKind.CHECKPOINT &&
                (it.tag.endsWith("-W") || it.tag.endsWith("-E"))
        }
        val homeWing = when {
            PacMazeGhostRoster.prefersWestWing(ghost.ghostKind, ghost.ghostSpecialty) ->
                wings.firstOrNull { it.tag.endsWith("-W") }
            PacMazeGhostRoster.prefersEastWing(ghost.ghostKind, ghost.ghostSpecialty) ->
                wings.firstOrNull { it.tag.endsWith("-E") }
            else -> wings.firstOrNull()
        } ?: return base

        val approach = gateApproachTile(level, homeWing) ?: return base
        val rate = PacMazeMapDynamics.dynamicPhaseTicks(state.levelId)
        val ticksLeft = PacMazeMapDynamics.ticksUntilDynamicStripeOpen(state, approach.first, approach.second)
        val phaseMul = when {
            PacMazeMapDynamics.isDynamicStripeOpen(state, approach.first, approach.second) -> 1.2f
            ticksLeft <= rate / 4 -> 1.15f
            ticksLeft <= rate / 2 -> 0.95f
            else -> 0.75f
        }
        val specialtyMul = if (ghost.ghostSpecialty == GhostSpecialty.GATE_KEEPER) 1.12f else 1f
        return (base * phaseMul * specialtyMul).coerceAtMost(0.95f)
    }
}
