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
                assertTrue(
                    "${file.name} missing item spawners",
                    level.itemSpawners.size >= PacMazeLevelProgression.spawnerCount(level.id),
                )
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

                val fromPac = floodAcrossDynamics(world, pac.first, pac.second)
                val fromGhosts = mutableSetOf<Pair<Int, Int>>()
                level.ghostSpawns.forEach { (gx, gy) ->
                    fromGhosts.addAll(floodAcrossDynamics(world, gx, gy, forGhost = true))
                }

                assertTrue(
                    "${file.name}: pac and ghosts disconnected",
                    fromPac.any { it in fromGhosts },
                )

                val allWalkable = walkableTiles(world)
                val unreachable = allWalkable.count { it !in fromPac }
                assertTrue(
                    "${file.name}: $unreachable tiles unreachable from pac (limit 4)",
                    unreachable <= 4,
                )

                if (PacMazeMapDynamics.hasDynamicTiles(world)) {
                    assertDynamicGatesReachable(file.name, rows, world, pac, fromPac)
                }
            }
    }

    /** 闸道 & 格在某个相位须从玩家位置可进入。 */
    private fun assertDynamicGatesReachable(
        fileName: String,
        grid: List<String>,
        world: PacMazeWorldState,
        pac: Pair<Int, Int>,
        fromPac: Set<Pair<Int, Int>>,
    ) {
        val rate = PacMazeMapDynamics.dynamicPhaseTicks(world.levelId)
        grid.forEachIndexed { y, row ->
            for (x in row.indices) {
                if (row[x] != '&') continue
                val reachableAtSomePhase = (0 until 3).any { phase ->
                    val tick = phase * rate
                    val probe = world.copy(dynamicsTick = tick)
                    x to y in flood(probe, pac.first, pac.second)
                }
                assertTrue(
                    "$fileName: dynamic gate $x,$y not enterable from pac",
                    reachableAtSomePhase || (x to y) in fromPac,
                )
            }
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

    private fun floodAcrossDynamics(
        world: PacMazeWorldState,
        sx: Int,
        sy: Int,
        forGhost: Boolean = false,
    ): Set<Pair<Int, Int>> {
        if (!PacMazeMapDynamics.hasDynamicTiles(world)) {
            return flood(world, sx, sy, forGhost)
        }
        val union = mutableSetOf<Pair<Int, Int>>()
        val rate = PacMazeMapDynamics.dynamicPhaseTicks(world.levelId)
        repeat(3) { phase ->
            union.addAll(flood(world.copy(dynamicsTick = phase * rate), sx, sy, forGhost))
        }
        return union
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
