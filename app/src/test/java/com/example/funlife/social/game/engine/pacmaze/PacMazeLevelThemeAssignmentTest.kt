package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeLevelThemeAssignmentTest {

    @Test
    fun extremeLevels_useDedicatedThemes() {
        assertEquals(PacMazeLevelThemeKey.STEAMPUNK, PacMazeLevelThemeAssignment.forLevel(14))
        assertEquals(PacMazeLevelThemeKey.GREENHOUSE, PacMazeLevelThemeAssignment.forLevel(23))
        assertTrue(PacMazeLevelThemeAssignment.isExtremeLevel(14))
    }

    @Test
    fun ghostAffinity_weightsFeaturedKindsOnExtremeLevels() {
        val pool = PacMazeGhostThemeAffinity.weightedPool(
            levelId = 20,
            themeKey = PacMazeLevelThemeKey.ARCHIVE,
        )
        assertTrue(pool.contains(GhostKind.PREDICTOR))
        assertTrue(pool.count { it == GhostKind.PREDICTOR } >= 2)
    }

    @Test
    fun mazeRun_usesMirrorTheme() {
        assertEquals(
            PacMazeLevelThemeKey.MIRROR,
            PacMazeLevelThemeAssignment.forRun(PacMazeRunMode.MAZE, campaignLevelId = 10),
        )
    }
}
