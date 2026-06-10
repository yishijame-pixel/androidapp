package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeHazardsTest {

    private val hazardJson = """
        {
          "id": 99,
          "name": "hazard-test",
          "width": 9,
          "height": 7,
          "grid": [
            "#########",
            "#...H...#",
            "#.......#",
            "#...>...#",
            "#.....I.#",
            "#.......#",
            "#########"
          ],
          "spawn": { "pac": [2, 5], "ghosts": [[6, 2]] },
          "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.2 }
        }
    """.trimIndent()

    @Test
    fun loader_parsesGridHazards() {
        val level = PacMazeMapLoader.parseLevelJson(hazardJson)
        assertTrue(level.hazards.any { it.kind == PacMazeHazardKind.LASER_ROW })
        assertTrue(level.hazards.any { it.kind == PacMazeHazardKind.LASER_COL })
        assertTrue(level.hazards.any { it.kind == PacMazeHazardKind.TURRET })
    }

    @Test
    fun buildInitialWorld_initializesHazardStates() {
        val level = PacMazeMapLoader.parseLevelJson(hazardJson)
        val world = PacMazeMapLoader.buildInitialWorld(level, hazardJson, seed = 1L)
        assertEquals(level.hazards.size, world.hazards.size)
        assertEquals(level.hazards.size, world.hazardStates.size)
    }

    @Test
    fun hazards_tick_advancesLaserScan() {
        val level = PacMazeMapLoader.parseLevelJson(hazardJson)
        var world = PacMazeMapLoader.buildInitialWorld(level, hazardJson, seed = 1L)
        val startPos = world.hazardStates.first { it.id.startsWith("ghz") }.scanPos
        repeat(60) {
            world = PacMazeHazards.tick(world, level)
            world = world.copy(tick = world.tick + 1)
        }
        val laterPos = world.hazardStates.first { it.id.startsWith("ghz") }.scanPos
        assertTrue(startPos != laterPos)
    }

    @Test
    fun turret_firesBullets() {
        val level = PacMazeMapLoader.parseLevelJson(hazardJson)
        var world = PacMazeMapLoader.buildInitialWorld(level, hazardJson, seed = 1L)
        var sawBullet = false
        repeat(PacMazeConstants.TURRET_FIRE_INTERVAL_TICKS + 50) {
            world = PacMazeHazards.tick(world, level)
            world = world.copy(tick = world.tick + 1)
            if (world.enemyBullets.isNotEmpty()) sawBullet = true
        }
        assertTrue(sawBullet)
    }
}
