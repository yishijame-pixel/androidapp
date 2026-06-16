package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Test

class PacMazeBitmapFeetAnchorTest {

    @After
    fun tearDown() {
        PacMazeBitmapFeetAnchor.invalidateAll()
    }

    @Test
    fun hasGameplayDefault_falseBeforeRegister() {
        assertFalse(PacMazeBitmapFeetAnchor.hasGameplayDefault(PacMazeSkinId.FOOD_MOUSE_WALK))
    }
}
