package com.example.funlife.social.game.engine.pacmaze

enum class PacMazeWinCondition {
    CLEAR_PELLETS,
    REACH_EXIT,
}

data class PacMazeModeRules(
    val winCondition: PacMazeWinCondition = PacMazeWinCondition.CLEAR_PELLETS,
    val timeLimitSeconds: Int = 0,
    val fogEnabled: Boolean = false,
    val fogRadius: Int = 2,
    val radarEnabled: Boolean = false,
    val requiredKeyTags: Set<String> = emptySet(),
    /** 有序钥印 tag（封印模式按序收集）。 */
    val orderedKeyTags: List<String> = emptyList(),
    val scoreMultiplier: Float = 1f,
    val starTimeBonusSeconds: Int = 0,
    val sealedKeyOrder: Boolean = false,
    val hintPelletsEnabled: Boolean = true,
    val intelPointsMax: Int = 0,
    val huntEscalation: Boolean = false,
    val ghostSignatureIds: List<String> = emptyList(),
    val mutatorId: String = "none",
    val variantId: String = "standard",
    val mirrorDynamicWalls: Boolean = false,
    val radarCooldownMultiplier: Float = 1f,
    val revealExitOnLastKey: Boolean = false,
    val extraGhostCount: Int = 0,
    val dynamicWallSpeedMul: Float = 1f,
)

enum class PacMazeRunMode(val id: String) {
    CAMPAIGN("campaign"),
    PRACTICE("practice"),
    ENDLESS("endless"),
    MAZE("maze"),
}
