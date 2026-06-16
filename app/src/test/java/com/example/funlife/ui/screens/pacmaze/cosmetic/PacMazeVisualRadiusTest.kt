package com.example.funlife.ui.screens.pacmaze.cosmetic

import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeVisualRadiusTest {

    @Test
    fun visualRadius_scalesLinearlyWithUserSlider() {
        val cell = 40f
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.LINE_BUNNY)
        val small = PacMazeCosmeticCatalog.visualRadius(cell, loadout, userDrawScale = 0.8f, entityDrawBoost = 1f)
        val normal = PacMazeCosmeticCatalog.visualRadius(cell, loadout, userDrawScale = 1f, entityDrawBoost = 1f)
        val large = PacMazeCosmeticCatalog.visualRadius(cell, loadout, userDrawScale = 1.5f, entityDrawBoost = 1f)
        assertTrue(small < normal)
        assertTrue(normal < large)
        assertTrue(large / small > 1.35f)
    }
}
