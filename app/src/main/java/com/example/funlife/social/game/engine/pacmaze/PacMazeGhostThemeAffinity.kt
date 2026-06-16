package com.example.funlife.social.game.engine.pacmaze

/** 高难度主题关卡的幽灵出场权重（与主题气质匹配，仍受 [PacMazeGhostRoster.activeKindsForLevel] 约束）。 */
object PacMazeGhostThemeAffinity {

    fun featuredKinds(themeKey: PacMazeLevelThemeKey, levelId: Int): List<GhostKind> {
        if (levelId < 14) return emptyList()
        return when (themeKey) {
            PacMazeLevelThemeKey.STEAMPUNK ->
                listOf(GhostKind.STRIKER, GhostKind.ORIGAMI, GhostKind.GATE_STATUE)
            PacMazeLevelThemeKey.VHS ->
                listOf(GhostKind.ERROR_SPECTER, GhostKind.GLITCH, GhostKind.ROUTER_SPIDER)
            PacMazeLevelThemeKey.ORBITAL ->
                listOf(GhostKind.ROUTER_SPIDER, GhostKind.PREDICTOR, GhostKind.HOURGLASS)
            PacMazeLevelThemeKey.MAGMA ->
                listOf(GhostKind.STRIKER, GhostKind.HOURGLASS, GhostKind.GATE_STATUE)
            PacMazeLevelThemeKey.SUBMARINE ->
                listOf(GhostKind.OPPORTUNIST, GhostKind.CACHE_BLOB, GhostKind.PREDICTOR)
            PacMazeLevelThemeKey.FROST ->
                listOf(GhostKind.PREDICTOR, GhostKind.FLANKER, GhostKind.ABACUS)
            PacMazeLevelThemeKey.ARCHIVE ->
                listOf(GhostKind.PREDICTOR, GhostKind.HOURGLASS, GhostKind.ABACUS, GhostKind.ERROR_SPECTER)
            PacMazeLevelThemeKey.METRO ->
                listOf(GhostKind.FLANKER, GhostKind.GATE_STATUE, GhostKind.ROUTER_SPIDER)
            PacMazeLevelThemeKey.OPERA ->
                listOf(GhostKind.FLANKER, GhostKind.GLITCH, GhostKind.ORIGAMI)
            PacMazeLevelThemeKey.GREENHOUSE ->
                listOf(GhostKind.OPPORTUNIST, GhostKind.ORIGAMI, GhostKind.CACHE_BLOB)
            PacMazeLevelThemeKey.CHRONO ->
                listOf(GhostKind.HOURGLASS, GhostKind.PREDICTOR, GhostKind.STRIKER)
            PacMazeLevelThemeKey.MIRROR ->
                listOf(GhostKind.FLANKER, GhostKind.GLITCH, GhostKind.ABACUS)
            else -> emptyList()
        }
    }

    fun weightedPool(levelId: Int, themeKey: PacMazeLevelThemeKey): List<GhostKind> {
        val unlocked = PacMazeGhostRoster.activeKindsForLevel(levelId)
        val featured = featuredKinds(themeKey, levelId).filter { it in unlocked }
        return (featured + unlocked).distinctBy { it.silhouette }
    }
}
