package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PacMazeStateSnapshotEngineTest {

    @Test
    fun roundTrip_preservesTickAndScores() {
        val (level, json) = PacMazeArenaParser.loadArenaById("arena_001")
        val config = PacMazeOnlineMatchConfig(
            matchSeed = 42L,
            hostPbId = "host",
            guestPbId = "guest",
        )
        val world = PacMazeArenaParser.buildOnlineWorld(level, json, config)
            .copy(tick = 120L, playerScoreA = 10, playerScoreB = 7)
        val encoded = PacMazeStateSnapshot.encode(world)
        val decoded = PacMazeStateSnapshot.decode(encoded, world)
        assertNotNull(decoded)
        assertEquals(120L, decoded!!.tick)
        assertEquals(10, decoded.playerScoreA)
        assertEquals(7, decoded.playerScoreB)
    }
}
