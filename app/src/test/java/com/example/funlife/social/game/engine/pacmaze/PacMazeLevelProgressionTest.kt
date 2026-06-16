package com.example.funlife.social.game.engine.pacmaze

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PacMazeLevelProgressionTest {

    @Test
    fun resolveDifficulty_isMonotonicAcrossLevels() {
        var prevSpeed = 0f
        var prevAgg = 0f
        for (id in 1..PacMazeLevelProgression.TOTAL_LEVELS) {
            val (speed, agg) = PacMazeLevelProgression.resolveDifficulty(id, 0.1f, 0.1f)
            assertTrue("L$id speed should rise", speed >= prevSpeed)
            assertTrue("L$id aggression should rise", agg >= prevAgg)
            prevSpeed = speed
            prevAgg = agg
        }
    }

    @Test
    fun allBundledLevels_haveProgressiveItemSpawners() {
        val dir = File("src/main/assets/pac_maze/levels")
        dir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val json = file.readText()
                val level = PacMazeMapLoader.parseLevelJson(json)
                val expected = PacMazeLevelProgression.spawnerCount(level.id)
                assertTrue(
                    "${file.name} should have >= $expected spawners, got ${level.itemSpawners.size}",
                    level.itemSpawners.size >= expected,
                )
                val root = JsonParser.parseString(json).asJsonObject
                val grid = root.getAsJsonArray("grid").map { it.asString }
                val linkTiles = PacMazePortals.pairs(level.markers, level.width)
                    .flatMap { pair -> listOf(pair.left.x to pair.left.y, pair.right.x to pair.right.y) }
                    .toSet()
                level.itemSpawners.forEach { spawner ->
                    assertTrue("${file.name} spawner pool empty", spawner.pool.isNotEmpty())
                    assertTrue("${file.name} interval too long", spawner.intervalTicks <= 960)
                    assertFalse(
                        "${file.name} spawner (${spawner.x},${spawner.y}) on portal tile",
                        spawner.x to spawner.y in linkTiles,
                    )
                    val row = grid.getOrNull(spawner.y)?.getOrNull(spawner.x)
                    assertFalse(
                        "${file.name} spawner on gate tile '$row'",
                        row == '=' || row == 'T' || row == 'P',
                    )
                }
            }
    }

    @Test
    fun allBundledLevels_haveMonotonicComplexity() {
        val dir = File("src/main/assets/pac_maze/levels")
        var prev = 0
        dir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val json = file.readText()
                val root = JsonParser.parseString(json).asJsonObject
                val level = PacMazeMapLoader.parseLevelJson(json)
                val grid = root.getAsJsonArray("grid").map { it.asString }
                val hazardCount = root.getAsJsonArray("hazards")?.size() ?: 0
                val cx = PacMazeLevelProgression.computeLevelComplexity(
                    levelId = level.id,
                    width = level.width,
                    height = level.height,
                    hazardCount = hazardCount,
                    grid = grid,
                )
                assertTrue(
                    "${file.name} complexity $cx should be >= $prev",
                    cx >= prev,
                )
                prev = cx
            }
    }

    @Test
    fun allBundledLevels_meetComplexityFloor() {
        val dir = File("src/main/assets/pac_maze/levels")
        dir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val json = file.readText()
                val root = JsonParser.parseString(json).asJsonObject
                val level = PacMazeMapLoader.parseLevelJson(json)
                val grid = root.getAsJsonArray("grid").map { it.asString }
                val hazardCount = root.getAsJsonArray("hazards")?.size() ?: 0
                val complexity = PacMazeLevelProgression.computeLevelComplexity(
                    levelId = level.id,
                    width = level.width,
                    height = level.height,
                    hazardCount = hazardCount,
                    grid = grid,
                )
                val floor = PacMazeLevelProgression.complexityFloor(level.id)
                assertTrue(
                    "${file.name} complexity $complexity below floor $floor for L${level.id}",
                    complexity >= floor,
                )
            }
    }

    @Test
    fun allBundledLevels_haveLinkPortalPair() {
        val dir = File("src/main/assets/pac_maze/levels")
        dir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val json = file.readText()
                val level = PacMazeMapLoader.parseLevelJson(json)
                val links = level.markers.filter { it.tag == "LINK" }
                assertEquals("${file.name} should have 2 LINK portals", 2, links.size)
                val pairs = PacMazePortals.pairs(level.markers, level.width)
                assertEquals("${file.name} should have 1 portal pair", 1, pairs.size)
            }
    }

    @Test
    fun enrichItemSpawners_fillsMissingFactories() {
        val json = """
            {
              "id": 4,
              "name": "progression-test",
              "width": 9,
              "height": 9,
              "grid": [
                "#########",
                "#.......#",
                "#.......#",
                "#.......#",
                "#...G...#",
                "#...P...#",
                "#.......#",
                "#.......#",
                "#########"
              ],
              "spawn": { "pac": [4, 5], "ghosts": [[4, 4]] },
              "difficulty": { "ghost_speed_mul": 0.2, "ai_aggression": 0.2 }
            }
        """.trimIndent()
        val level = PacMazeMapLoader.parseLevelJson(json)
        assertEquals(3, level.itemSpawners.size)
        assertTrue(level.ghostSpeedMul >= PacMazeLevelProgression.ghostSpeedFloor(4))
    }
}
