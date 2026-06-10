package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeMazeGeneratorTest {

    @Test
    fun generatedLevel_reachesExitWinCondition() {
        val json = PacMazeMazeGenerator.buildLevelJson(seed = 42L)
        val level = PacMazeMapLoader.parseLevelJson(json)
        assertEquals(PacMazeWinCondition.REACH_EXIT, level.modeRules.winCondition)
        assertTrue(level.markers.any { it.kind == PacMazeMarkerKind.EXIT })
    }
}
