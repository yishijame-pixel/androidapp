package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PacMazeStateSnapshotTest {

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
            "ghosts": [[3, 2]]
          }
        }
    """.trimIndent()

    @Test
    fun encodeDecode_roundTrip_preservesTickAndScores() {
        val (level, json) = PacMazeOnlineLoader.parseArenaJson(arenaJson)
        val config = PacMazeOnlineMatchConfig(
            mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
            matchSeed = 1L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        val world = PacMazeOnlineLoader.buildOnlineWorld(level, json, config).copy(
            tick = 42L,
            playerScoreA = 10,
            playerScoreB = 7,
            onlineElapsedSeconds = 5,
        )
        val wire = PacMazeStateSnapshot.encode(world)
        val decoded = PacMazeStateSnapshot.decode(wire, world)
        assertNotNull(decoded)
        assertEquals(42L, decoded!!.tick)
        assertEquals(10, decoded.playerScoreA)
        assertEquals(7, decoded.playerScoreB)
        assertEquals(world.entities.size, decoded.entities.size)
        assertEquals(world.tiles.size, decoded.tiles.size)
    }
}
