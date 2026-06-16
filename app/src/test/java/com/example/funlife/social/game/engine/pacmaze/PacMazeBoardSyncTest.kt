package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameMoveKind
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeBoardSyncTest {

    private val config = PacMazeOnlineMatchConfig(
        mode = PacMazeOnlineMatchMode.VERSUS_DUEL,
        hostPbId = "host_pb",
        guestPbId = "guest_pb",
        hostEntityId = "pac_a",
        guestEntityId = "pac_b",
    )

    @Test
    fun parseInputMoves_mapsPbIdToEntityAndDirection() {
        val payload = JsonParser.parseString(
            """
            {
              "kind": "${GameMoveKind.PAC_INPUT_FRAME.wire}",
              "from_tick": 12,
              "frames": [
                { "tick": 12, "gen": 1, "mode": "committed", "dir": "RIGHT", "attack": false }
              ]
            }
            """.trimIndent(),
        )
        val move = GameMoveDto(
            id = "m1",
            roomId = "room1",
            playerPbId = "guest_pb",
            moveIndex = 1,
            payload = payload,
            createdAtMs = 0L,
        )
        val parsed = PacMazeBoardSync.parseInputMoves(listOf(move), config)
        assertEquals(1, parsed.size)
        assertEquals("pac_b", parsed.first().entityId)
        assertEquals(12L, parsed.first().tick)
        assertEquals(Direction.RIGHT, parsed.first().input.committed)
        assertTrue(parsed.first().input.mode == PacMazeInputMode.Committed)
    }
}
