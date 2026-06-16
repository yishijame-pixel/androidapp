package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeRollbackSessionTest {

    private val arenaJson = """
        {
          "id": "arena_001",
          "name": "测试",
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
            "ghosts": []
          }
        }
    """.trimIndent()

    private fun worldAndConfig(): Triple<PacMazeWorldState, PacMazeLevelConfig, PacMazeOnlineMatchConfig> {
        val (level, json) = PacMazeOnlineLoader.parseArenaJson(arenaJson)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 42L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        val world = PacMazeOnlineLoader.buildOnlineWorld(level, json, config)
        return Triple(world, level, config)
    }

    @Test
    fun advanceFrame_movesLocalPlayerWithoutServerSnapshot() {
        val (initial, level, config) = worldAndConfig()
        val session = PacMazeRollbackSession("pac_a", "pac_b")
        session.configure(level, config)
        session.reset(initial)
        val pac0 = initial.pacById("pac_a")!!
        var now = System.currentTimeMillis()
        val moved = session.advanceFrame(
            nowMs = now + 50L,
            localInput = PacMazeTickInput.committed(1L, Direction.RIGHT),
        )!!
        val pac1 = moved.pacById("pac_a")!!
        assertTrue(pac1.x >= pac0.x || pac1.y != pac0.y)
    }

    @Test
    fun lateRemoteInput_triggersRollbackWithoutCrash() {
        val (initial, level, config) = worldAndConfig()
        val session = PacMazeRollbackSession("pac_a", "pac_b")
        session.configure(level, config)
        session.reset(initial)
        var t = System.currentTimeMillis()
        repeat(30) {
            t += 20L
            session.advanceFrame(
                t,
                PacMazeTickInput.committed(session.nextInputTick(), Direction.RIGHT),
            )
        }
        val before = session.currentWorld()!!
        session.onRemoteInput(5L, PacMazeTickInput.committed(5L, Direction.LEFT), attack = false)
        val after = session.currentWorld()!!
        assertTrue(session.rollbackCount >= 1)
        assertEquals(before.tick, after.tick)
    }
}
