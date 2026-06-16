package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.engine.pacmaze.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeEntityVisualsTest {

    @Test
    fun travelFacing_prefersVelocityOverQueuedTurn() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 5f,
            y = 3f,
            direction = Direction.RIGHT,
            speed = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC,
            facing = Direction.UP,
            velX = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC,
            velY = 0f,
            nextDirection = Direction.UP,
        )
        assertEquals(Direction.RIGHT, PacMazeEntityVisuals.travelFacing(entity))
    }

    @Test
    fun trailAnchorOffset_opposesVelocityInScreenSpace() {
        val (ox, oy) = PacMazeEntityVisuals.trailAnchorOffset(
            velX = 1f,
            velY = 0f,
            fallbackFacing = Direction.RIGHT,
            cellX = 20f,
            cellY = 24f,
            trailDepthPx = 10f,
        )
        assertTrue(ox < 0f)
        assertEquals(0f, oy, 0.001f)
    }

    @Test
    fun isLocomoting_falseWhenStoppedWithDirectionHeld() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 1f,
            direction = Direction.RIGHT,
            speed = 0f,
            facing = Direction.RIGHT,
            velX = 0f,
            velY = 0f,
            inputActive = false,
        )
        assertFalse(PacMazeEntityVisuals.isLocomoting(entity))
    }

    @Test
    fun isLocomoting_trueWhenInputActiveBeforeVelocityStarts() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 1f,
            direction = null,
            speed = PacMazeConstants.PAC_SPEED_CELLS_PER_SEC,
            facing = Direction.RIGHT,
            velX = 0f,
            velY = 0f,
            nextDirection = Direction.RIGHT,
            inputActive = true,
        )
        assertTrue(PacMazeEntityVisuals.isLocomoting(entity))
    }
}
