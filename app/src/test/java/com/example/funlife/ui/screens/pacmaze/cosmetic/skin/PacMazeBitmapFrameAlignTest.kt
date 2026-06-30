package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import org.junit.Assert.assertEquals
import org.junit.Test

class PacMazeBitmapFrameAlignTest {

    @Test
    fun opaqueHeightScale_normalizesShorterFrame() {
        val scale = PacMazeBitmapFrameAlign.opaqueHeightScale(referenceOpaqueH = 0.70f, frameOpaqueH = 0.65f)
        assertEquals(0.70f / 0.65f, scale, 0.001f)
    }

    @Test
    fun compose_appliesSoleAndHeightScaleWhenFramesDiffer() {
        val refFeet = 0.95f
        val frameFeet = 0.88f
        val refOpaque = 0.70f
        val frameOpaque = 0.65f
        val layoutH = 100f
        val scale = PacMazeBitmapFrameAlign.opaqueHeightScale(refOpaque, frameOpaque)
        val soleDy = (refFeet - frameFeet) * layoutH * scale
        assertEquals(1.077f, scale, 0.01f)
        assertEquals(7.54f, soleDy, 0.1f)
    }
}
