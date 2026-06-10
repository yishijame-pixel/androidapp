package com.example.funlife.social.game.engine.pacmaze

data class PacMazeStarCriteria(
    val twoStarMinScore: Int = 1500,
    val threeStarMinScore: Int = 3000,
    val threeStarMaxSeconds: Int = 0,
    val threeStarNoDeath: Boolean = false,
) {
    companion object {
        fun defaults() = PacMazeStarCriteria()

        fun fromLevelJson(root: com.google.gson.JsonObject): PacMazeStarCriteria {
            val obj = root.getAsJsonObject("starCriteria") ?: return defaults()
            return PacMazeStarCriteria(
                twoStarMinScore = obj.get("twoStarMinScore")?.asInt ?: 1500,
                threeStarMinScore = obj.get("threeStarMinScore")?.asInt ?: 3000,
                threeStarMaxSeconds = obj.get("threeStarMaxSeconds")?.asInt ?: 0,
                threeStarNoDeath = obj.get("threeStarNoDeath")?.asBoolean ?: false,
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
    ): Int {
        var stars = 1
        if (score >= criteria.twoStarMinScore) stars = 2
        if (score >= criteria.threeStarMinScore) {
            val timeOk = criteria.threeStarMaxSeconds <= 0 || elapsedSeconds <= criteria.threeStarMaxSeconds
            val deathOk = !criteria.threeStarNoDeath || deaths == 0
            if (timeOk && deathOk) stars = 3
        }
        return stars.coerceIn(1, 3)
    }
}
