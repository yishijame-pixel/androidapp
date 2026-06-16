package com.example.funlife.social.game.engine.pacmaze

enum class PacMazeOnlineMatchMode(val id: String, val displayName: String) {
    VERSUS_DUEL("versus_duel", "豆人对决"),
    COOP_CAMPAIGN("coop_campaign", "并肩闯关"),
    VERSUS_GHOST("versus_ghost", "猎手对决"),
    COOP_MAZE("coop_maze", "迷雾双探"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeOnlineMatchMode =
            entries.firstOrNull { it.id == id } ?: VERSUS_DUEL
    }
}

enum class PacMazeVersusRule(val id: String, val displayName: String) {
    RACE_PELLETS("race_pellets", "竞速清豆"),
    RACE_EXIT("race_exit", "出口冲刺"),
    LAST_LIFE("last_life", "生存淘汰"),
    ;

    companion object {
        fun fromId(id: String?): PacMazeVersusRule =
            entries.firstOrNull { it.id == id } ?: RACE_PELLETS
    }
}

data class PacMazeOnlineMatchConfig(
    val mode: PacMazeOnlineMatchMode = PacMazeOnlineMatchMode.VERSUS_DUEL,
    val versusRule: PacMazeVersusRule = PacMazeVersusRule.RACE_PELLETS,
    val timeLimitSeconds: Int = 150,
    val levelId: Int = 1,
    val arenaId: String = "arena_001",
    val matchSeed: Long = 0L,
    val hostPbId: String = "",
    val guestPbId: String = "",
    val hostEntityId: String = "pac_a",
    val guestEntityId: String = "pac_b",
    val teamLives: Int = 5,
    val playerLivesEach: Int = 3,
)

object PacMazeOnlineEndReason {
    const val NORMAL = "normal"
    const val SURRENDER = "surrender"
    const val DISCONNECT = "disconnect"
    const val TIMEOUT = "timeout"
    const val DRAW = "draw"
}
