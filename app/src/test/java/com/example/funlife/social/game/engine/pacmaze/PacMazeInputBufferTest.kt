package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PacMazeInputBufferTest {

    @Test
    fun push_nullDirection_doesNotThrowAndMarksInactive() {
        val buffer = PacMazeInputBuffer()
        buffer.push(PacMazeConstants.PLAYER_ID, Direction.RIGHT)
        buffer.push(PacMazeConstants.PLAYER_ID, null)

        val state = buffer.poll(PacMazeConstants.PLAYER_ID)
        assertEquals(Direction.RIGHT, state.current)
        assertEquals(Direction.RIGHT, state.queued)
        assertFalse(state.active)
    }
}
