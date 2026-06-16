package com.example.funlife.social.game.engine.pacmaze

data class PacMazeObjectiveLine(
    val label: String,
    val done: Boolean,
)

object PacMazeObjectiveResults {

    fun build(level: PacMazeLevelConfig, world: PacMazeWorldState): List<PacMazeObjectiveLine> {
        val required = level.starCriteria.threeStarRequiredTags
        if (required.isEmpty()) return emptyList()
        return level.markers
            .filter { it.kind == PacMazeMarkerKind.CHECKPOINT && it.tag in required }
            .map { marker ->
                PacMazeObjectiveLine(
                    label = marker.label.ifBlank { marker.tag },
                    done = marker.tag in world.visitedCheckpointTags,
                )
            }
    }
}
