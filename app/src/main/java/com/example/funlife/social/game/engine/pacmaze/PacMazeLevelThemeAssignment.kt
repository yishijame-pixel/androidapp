package com.example.funlife.social.game.engine.pacmaze

/** 关卡视觉主题键（与 UI [com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId.id] 对齐）。 */
enum class PacMazeLevelThemeKey(val id: String) {
    CLASSIC("classic"),
    CYBERPUNK("cyberpunk"),
    GARDEN("garden"),
    FOOD("food"),
    CHINESE("chinese"),
    ENDLESS("endless"),
    MAZE("maze"),
    SUBMARINE("submarine"),
    ORBITAL("orbital"),
    STEAMPUNK("steampunk"),
    ARCHIVE("archive"),
    MAGMA("magma"),
    FROST("frost"),
    METRO("metro"),
    OPERA("opera"),
    VHS("vhs"),
    GREENHOUSE("greenhouse"),
    CHRONO("chrono"),
    MIRROR("mirror"),
}

object PacMazeLevelThemeAssignment {

    fun forLevel(levelId: Int): PacMazeLevelThemeKey = when (levelId) {
        1, 2, 6 -> PacMazeLevelThemeKey.CYBERPUNK
        3, 7, 12 -> PacMazeLevelThemeKey.GARDEN
        4, 8 -> PacMazeLevelThemeKey.FOOD
        5, 9, 10, 11, 13 -> PacMazeLevelThemeKey.CHINESE
        14 -> PacMazeLevelThemeKey.STEAMPUNK
        15 -> PacMazeLevelThemeKey.VHS
        16 -> PacMazeLevelThemeKey.ORBITAL
        17 -> PacMazeLevelThemeKey.MAGMA
        18 -> PacMazeLevelThemeKey.SUBMARINE
        19 -> PacMazeLevelThemeKey.FROST
        20 -> PacMazeLevelThemeKey.ARCHIVE
        21 -> PacMazeLevelThemeKey.METRO
        22 -> PacMazeLevelThemeKey.OPERA
        23 -> PacMazeLevelThemeKey.GREENHOUSE
        else -> PacMazeLevelThemeKey.CYBERPUNK
    }

    fun forRun(runMode: PacMazeRunMode, campaignLevelId: Int): PacMazeLevelThemeKey = when (runMode) {
        PacMazeRunMode.ENDLESS ->
            if (campaignLevelId >= PacMazeLevelProgression.TOTAL_LEVELS) {
                PacMazeLevelThemeKey.CHRONO
            } else {
                PacMazeLevelThemeKey.ENDLESS
            }
        PacMazeRunMode.MAZE -> PacMazeLevelThemeKey.MAZE
        else -> forLevel(campaignLevelId.coerceAtLeast(1))
    }

    fun isExtremeLevel(levelId: Int): Boolean = levelId >= 14
}
