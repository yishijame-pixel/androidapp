package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeOnlineSimulationTest {

    private val arenaJson = """
        {
          "id": "arena_001",
          "name": "测试竞技场",
          "width": 7,
          "height": 5,
          "grid": [
            "#######",
            "#.....#",
            "#.###.#",
            "#.....#",
            "#######"
          ],
          "spawn": {
            "pac_a": [1, 1],
            "pac_b": [5, 3],
            "ghosts": [[3, 2]]
          }
        }
    """.trimIndent()

    @Test
    fun versusWorld_hasTwoPlayersAndZones() {
        val (level, json) = PacMazeOnlineLoader.parseArenaJson(arenaJson)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 42L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        val world = PacMazeOnlineLoader.buildOnlineWorld(level, json, config)
        assertEquals(2, world.playerPacs().size)
        assertEquals(2, world.entities.size)
        assertTrue(world.entities.none { it.role == "ghost" })
        assertEquals(0, world.ghostReleaseTicksLeft)
        assertTrue(world.pelletZoneAInitial > 0)
        assertTrue(world.pelletZoneBInitial > 0)
    }

    @Test
    fun tick_advancesWithoutCrash() {
        val (level, json) = PacMazeOnlineLoader.parseArenaJson(arenaJson)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 7L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        var world = PacMazeOnlineLoader.buildOnlineWorld(level, json, config)
        repeat(120) { tick ->
            val input = mapOf(
                "pac_a" to PacMazeTickInput.committed(tick.toLong(), Direction.RIGHT),
                "pac_b" to PacMazeTickInput.committed(tick.toLong(), Direction.LEFT),
            )
            world = PacMazeOnlineSimulation.tick(world, input, level, config)
        }
        assertTrue(world.tick in 1L..120L)
        assertTrue(
            world.phase == PacMazePhase.PLAYING ||
                world.phase == PacMazePhase.LEVEL_CLEAR ||
                world.phase == PacMazePhase.GAME_OVER,
        )
    }

    @Test
    fun tick_withRightInput_movesPacA() {
        val (level, json) = PacMazeOnlineLoader.parseArenaJson(arenaJson)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 42L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        var world = PacMazeOnlineLoader.buildOnlineWorld(level, json, config)
        val startX = world.pacById("pac_a")!!.x
        repeat(30) {
            val input = mapOf(
                "pac_a" to PacMazeTickInput.committed(world.tick + 1, Direction.RIGHT),
                "pac_b" to PacMazeTickInput.Inactive,
            )
            world = PacMazeOnlineSimulation.tick(world, input, level, config)
        }
        assertTrue(
            "pac_a should move right from spawn, x=${world.pacById("pac_a")!!.x}",
            world.pacById("pac_a")!!.x > startX,
        )
    }

    @Test
    fun arena001_rightInput_movesPacA() {
        val json = java.io.File("src/main/assets/pac_maze/arenas/arena_001.json").readText()
        val (level, jsonStr) = PacMazeOnlineLoader.parseArenaJson(json)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 42L,
        )
        var world = PacMazeOnlineLoader.buildOnlineWorld(level, jsonStr, config)
        val startX = world.pacById("pac_a")!!.x
        repeat(60) {
            val input = mapOf(
                "pac_a" to PacMazeTickInput.committed(world.tick + 1, Direction.RIGHT),
                "pac_b" to PacMazeTickInput.Inactive,
            )
            world = PacMazeOnlineSimulation.tick(world, input, level, config)
        }
        assertTrue(
            "arena_001 pac_a stuck at x=${world.pacById("pac_a")!!.x} start=$startX",
            world.pacById("pac_a")!!.x > startX,
        )
    }
}
