package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import org.junit.Assert.assertEquals
import org.junit.Test

class PacMazeBitmapSoleAlignTest {

    @Test
    fun soleDelta_shiftsShallowerFrameDown() {
        val layoutH = 100f
        val reference = 0.95f
        val frame = 0.88f
        val delta = (reference - frame) * layoutH
        assertEquals(7f, delta, 0.001f)
    }

    @Test
    fun soleDelta_zeroWhenFrameMatchesReference() {
        val delta = (0.92f - 0.92f) * 80f
        assertEquals(0f, delta, 0.001f)
    }
}
