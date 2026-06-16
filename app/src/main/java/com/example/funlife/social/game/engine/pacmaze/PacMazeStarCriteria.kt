package com.example.funlife.social.game.engine.pacmaze

data class PacMazeStarCriteria(
    val twoStarMinScore: Int = 1500,
    val threeStarMinScore: Int = 3000,
    val threeStarMaxSeconds: Int = 0,
    val threeStarNoDeath: Boolean = false,
    /** 三星需抵达的 checkpoint tag（如 L14-W）；空则不要求。 */
    val threeStarRequiredTags: Set<String> = emptySet(),
) {
    companion object {
        fun defaults() = PacMazeStarCriteria()

        fun fromLevelJson(root: com.google.gson.JsonObject): PacMazeStarCriteria {
            val obj = root.getAsJsonObject("starCriteria") ?: return defaults()
            val requiredTags = obj.getAsJsonArray("threeStarRequiredTags")
                ?.mapNotNull { it.asString }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
            return PacMazeStarCriteria(
                twoStarMinScore = obj.get("twoStarMinScore")?.asInt ?: 1500,
                threeStarMinScore = obj.get("threeStarMinScore")?.asInt ?: 3000,
                threeStarMaxSeconds = obj.get("threeStarMaxSeconds")?.asInt ?: 0,
                threeStarNoDeath = obj.get("threeStarNoDeath")?.asBoolean ?: false,
                threeStarRequiredTags = requiredTags,
            )
        }
    }
}

object PacMazeStarEvaluator {

    fun evaluate(
        criteria: PacMazeStarCriteria,
        score: Int,
        elapsedSeconds: Int,
        deaths: Int,
        visitedCheckpointTags: Set<String> = emptySet(),
    ): Int {
        var stars = 1
        if (score >= criteria.twoStarMinScore) stars = 2
        if (score >= criteria.threeStarMinScore) {
            val timeOk = criteria.threeStarMaxSeconds <= 0 || elapsedSeconds <= criteria.threeStarMaxSeconds
            val deathOk = !criteria.threeStarNoDeath || deaths == 0
            val tagsOk = criteria.threeStarRequiredTags.isEmpty() ||
                criteria.threeStarRequiredTags.all { it in visitedCheckpointTags }
            if (timeOk && deathOk && tagsOk) stars = 3
        }
        return stars.coerceIn(1, 3)
    }
}
