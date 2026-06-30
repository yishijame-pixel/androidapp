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

    @Test
    fun generatedLevel_includesPelletsAndMechanisms() {
        val options = PacMazeMazeRunOptions(
            seed = 77L,
            difficulty = PacMazeMazeDifficulty.STANDARD,
        )
        val json = PacMazeMazeGenerator.buildLevelJson(options)
        val level = PacMazeMapLoader.parseLevelJson(json)
        assertTrue(level.hazards.isNotEmpty() || json.contains("\"G\"") || json.contains("\"H\""))
        assertTrue(json.contains("\".\"") || json.contains("*"))
        assertTrue(level.itemSpawners.isNotEmpty())
    }

    @Test
    fun campaignLevel_usesProgressionStarCriteriaWhenMissing() {
        val json = """
            {"id":5,"name":"test","width":11,"height":11,"grid":["###########"],"spawn":{"pac":[1,1],"ghosts":[]}}
        """.trimIndent()
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val criteria = PacMazeStarCriteria.fromLevelJson(root)
        val expected = PacMazeLevelProgression.starCriteria(5)
        assertEquals(expected.twoStarMinScore, criteria.twoStarMinScore)
        assertEquals(expected.threeStarMaxSeconds, criteria.threeStarMaxSeconds)
    }
}
