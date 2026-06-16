package com.example.funlife.social.game.engine.pacmaze

/** 记录玩家抵达的 checkpoint 标记（非 LINK 传送门对）。 */
object PacMazeCheckpointVisits {

    fun apply(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (level.markers.isEmpty()) return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        var changed = false
        var rejectFlash = false
        val visited = state.visitedCheckpointTags.toMutableSet()
        level.markers.forEach { marker ->
            if (marker.kind != PacMazeMarkerKind.CHECKPOINT) return@forEach
            if (marker.tag.isBlank() || marker.tag == "LINK") return@forEach
            if (marker.x != tx || marker.y != ty) return@forEach
            if (marker.tag in visited) return@forEach
            val result = PacMazeMazeMechanics.tryVisitCheckpoint(state, level, marker.tag)
            if (result.accepted && visited.add(marker.tag)) {
                changed = true
            } else if (result.rejectFlash) {
                rejectFlash = true
            }
        }
        if (!changed && !rejectFlash) return state
        var next = if (changed) state.copy(visitedCheckpointTags = visited) else state
        if (rejectFlash) {
            next = next.copy(sealedKeyRejectFlashTicks = 45)
        }
        if (changed && level.modeRules.revealExitOnLastKey) {
            val allKeys = level.modeRules.requiredKeyTags
            if (allKeys.isNotEmpty() && allKeys.all { it in visited }) {
                val exit = level.markers.firstOrNull { it.kind == PacMazeMarkerKind.EXIT }
                if (exit != null) {
                    val w = next.width
                    val explored = next.exploredTiles.toMutableSet()
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val x = exit.x + dx
                            val y = exit.y + dy
                            if (x in 0 until w && y in 0 until next.height) {
                                explored.add(y * w + x)
                            }
                        }
                    }
                    next = next.copy(exploredTiles = explored)
                }
            }
        }
        return next
    }

    fun applyForEntity(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        pac: PacMazeEntity,
    ): PacMazeWorldState {
        if (level.markers.isEmpty()) return state
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        var changed = false
        var rejectFlash = false
        val visited = state.visitedCheckpointTags.toMutableSet()
        level.markers.forEach { marker ->
            if (marker.kind != PacMazeMarkerKind.CHECKPOINT) return@forEach
            if (marker.tag.isBlank() || marker.tag == "LINK") return@forEach
            if (marker.x != tx || marker.y != ty) return@forEach
            if (marker.tag in visited) return@forEach
            val result = PacMazeMazeMechanics.tryVisitCheckpoint(state, level, marker.tag)
            if (result.accepted && visited.add(marker.tag)) changed = true
            else if (result.rejectFlash) rejectFlash = true
        }
        if (!changed && !rejectFlash) return state
        var next = if (changed) state.copy(visitedCheckpointTags = visited) else state
        if (rejectFlash) next = next.copy(sealedKeyRejectFlashTicks = 45)
        return next
    }
}
