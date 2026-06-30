package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import com.example.funlife.social.game.engine.pacmaze.Direction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeBitmapCorridorDrawPolicyTest {

    @After
    fun tearDown() {
        PacMazeSkinRegistry.drawCorridorCenterPx = null
        PacMazeSkinRegistry.drawFeetAnchorPx = null
        PacMazeSkinRegistry.drawTravelFacing = null
    }

    @Test
    fun suppressWalkBob_whenCorridorCenterAndHorizontal() {
        PacMazeSkinRegistry.drawCorridorCenterPx = Offset(100f, 200f)
        PacMazeSkinRegistry.drawTravelFacing = Direction.LEFT
        assertTrue(PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob())
        assertEquals(0f, PacMazeBitmapCorridorDrawPolicy.effectiveWalkBob(12f), 0.001f)
    }

    @Test
    fun keepWalkBob_whenVerticalTravel() {
        PacMazeSkinRegistry.drawCorridorCenterPx = Offset(100f, 200f)
        PacMazeSkinRegistry.drawTravelFacing = Direction.UP
        assertFalse(PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob())
        assertEquals(12f, PacMazeBitmapCorridorDrawPolicy.effectiveWalkBob(12f), 0.001f)
    }

    @Test
    fun corridorAnchor_usesInjectedCenterOnHorizontal() {
        PacMazeSkinRegistry.drawCorridorCenterPx = Offset(50f, 300f)
        val anchor = PacMazeBitmapCorridorDrawPolicy.corridorAnchorOrCenter(
            center = Offset(10f, 10f),
            walkBob = 8f,
            horizontalTravel = true,
        )
        assertEquals(50f, anchor.x, 0.001f)
        assertEquals(300f, anchor.y, 0.001f)
    }
}
