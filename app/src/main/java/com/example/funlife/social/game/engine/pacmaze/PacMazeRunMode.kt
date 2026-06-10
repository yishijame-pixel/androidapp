package com.example.funlife.social.game.engine.pacmaze

enum class PacMazeWinCondition {
    CLEAR_PELLETS,
    REACH_EXIT,
}

data class PacMazeModeRules(
    val winCondition: PacMazeWinCondition = PacMazeWinCondition.CLEAR_PELLETS,
    val timeLimitSeconds: Int = 0,
)

enum class PacMazeRunMode(val id: String) {
    CAMPAIGN("campaign"),
    PRACTICE("practice"),
    ENDLESS("endless"),
    MAZE("maze"),
}
