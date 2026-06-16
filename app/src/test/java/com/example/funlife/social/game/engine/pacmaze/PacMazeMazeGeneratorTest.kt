package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeMazeGeneratorTest {

    @Test
    fun generatedLevel_reachesExitWinCondition() {
        val options = PacMazeMazeRunOptions(seed = 42L)
        val json = PacMazeMazeGenerator.buildLevelJson(options)
        val level = PacMazeMapLoader.parseLevelJson(json)
        assertEquals(PacMazeWinCondition.REACH_EXIT, level.modeRules.winCondition)
        assertTrue(level.markers.any { it.kind == PacMazeMarkerKind.EXIT })
        assertTrue(level.modeRules.fogEnabled)
        assertTrue(level.modeRules.radarEnabled)
        assertTrue(level.modeRules.requiredKeyTags.isNotEmpty())
    }

    @Test
    fun dailySeed_isDeterministicForSameDay() {
        val a = PacMazeMazeRunOptions.fromParams(
            PacMazeLoadParams(
                runMode = PacMazeRunMode.MAZE,
                seed = 999L,
                mazeDailyChallenge = true,
            ),
        )
        val b = PacMazeMazeRunOptions.fromParams(
            PacMazeLoadParams(
                runMode = PacMazeRunMode.MAZE,
                seed = 12345L,
                mazeDailyChallenge = true,
            ),
        )
        assertEquals(a.seed, b.seed)
    }

    @Test
    fun rushContract_shortensTimeLimit() {
        val base = PacMazeMazeRunOptions(
            seed = 1L,
            difficulty = PacMazeMazeDifficulty.STANDARD,
            contract = PacMazeMazeContract.NONE,
        )
        val rush = base.copy(contract = PacMazeMazeContract.RUSH)
        assertTrue(rush.effectiveTimeLimitSeconds < base.effectiveTimeLimitSeconds)
    }
}
