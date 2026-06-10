package com.example.funlife.social.game.engine.pacmaze

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.ArrayDeque

/** 关卡 JSON 连通性：玩家与幽灵必须同区，且主要可走格从玩家出生点可达。 */
class PacMazeLevelConnectivityTest {

    @Test
    fun allBundledLevels_areConnectedAndCompletable() {
        val dir = File("src/main/assets/pac_maze/levels")
        assertTrue("levels dir missing: ${dir.absolutePath}", dir.isDirectory)
        dir.listFiles { f -> f.name.startsWith("level_") && f.name.endsWith(".json") }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                val json = file.readText()
                val rows = gridRows(json)
                val level = PacMazeMapLoader.parseLevelJson(json)
                assertEquals("${file.name} grid row count", level.height, rows.size)
                rows.forEachIndexed { y, row ->
                    assertEquals("${file.name} row $y width", level.width, row.length)
                }

                var world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 1L)
                world = world.copy(energyGateOpen = true)

                val pac = level.pacSpawn
                assertTrue("${file.name} pac spawn not walkable", PacMazeRules.isWalkable(world, pac.first, pac.second))
                level.ghostSpawns.forEach { (gx, gy) ->
                    assertTrue("${file.name} ghost $gx,$gy not walkable", PacMazeRules.isWalkable(world, gx, gy, forGhost = true))
                }

                val fromPac = flood(world, pac.first, pac.second)
                val fromGhosts = mutableSetOf<Pair<Int, Int>>()
                level.ghostSpawns.forEach { (gx, gy) ->
                    fromGhosts.addAll(flood(world, gx, gy, forGhost = true))
                }

                assertTrue(
                    "${file.name}: pac and ghosts disconnected",
                    fromPac.any { it in fromGhosts },
                )

                val allWalkable = walkableTiles(world)
                val unreachable = allWalkable.count { it !in fromPac }
                assertTrue(
                    "${file.name}: $unreachable tiles unreachable from pac",
                    unreachable <= 4,
                )
            }
    }

    private fun gridRows(json: String): List<String> =
        JsonParser.parseString(json).asJsonObject.getAsJsonArray("grid").map { it.asString }

    private fun walkableTiles(world: PacMazeWorldState): Set<Pair<Int, Int>> =
        buildSet {
            for (y in 0 until world.height) {
                for (x in 0 until world.width) {
                    if (PacMazeRules.isWalkable(world, x, y)) add(x to y)
                }
            }
        }

    private fun flood(
        world: PacMazeWorldState,
        sx: Int,
        sy: Int,
        forGhost: Boolean = false,
    ): Set<Pair<Int, Int>> {
        val seen = mutableSetOf(sx to sy)
        val q = ArrayDeque(listOf(sx to sy))
        while (q.isNotEmpty()) {
            val (x, y) = q.removeFirst()
            for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val nx = x + dx
                val ny = y + dy
                if ((nx to ny) !in seen && PacMazeRules.isWalkable(world, nx, ny, forGhost)) {
                    seen.add(nx to ny)
                    q.add(nx to ny)
                }
            }
        }
        return seen
    }
}
