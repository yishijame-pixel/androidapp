package com.example.funlife.ui.screens.pacmaze.components

import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeEntityComfortScaleTest {

    @Test
    fun compute_keepsSmallMapsNearUnity() {
        val m = PacMazeEntityComfortScale.compute(
            baseCellPx = 36f,
            canvasContentHeightPx = 420f,
            playerUserScale = 1f,
            playerTierScale = 1f,
            densityPxPerDp = 2.5f,
        )
        assertTrue(m.boost <= 1.05f)
    }

    @Test
    fun compute_boostsLargeMapAtTinyCells() {
        val m = PacMazeEntityComfortScale.compute(
            baseCellPx = 12f,
            canvasContentHeightPx = 380f,
            playerUserScale = 1f,
            playerTierScale = 1f,
            densityPxPerDp = 2.5f,
        )
        assertTrue(m.boost in 2.0f..2.75f)
    }

    @Test
    fun compute_skipsAutoBoostWhenUserAlreadyEnlarged() {
        val m = PacMazeEntityComfortScale.compute(
            baseCellPx = 12f,
            canvasContentHeightPx = 380f,
            playerUserScale = 2f,
            playerTierScale = 1f,
            densityPxPerDp = 2.5f,
        )
        assertTrue(m.boost <= 2.75f)
        val playerRadius = PacMazeEntityComfortScale.resolvePlayerRadius(
            entityCell = m.entityCell,
            tierScale = 1f,
            userScale = 2f,
            boost = 1f,
            minRadiusPx = 0f,
        )
        assertTrue(playerRadius < 380f * 0.052f)
    }
}
