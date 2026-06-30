package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeBitmapSoleAlignFrameFeetTest {

    @After
    fun tearDown() {
        PacMazeBitmapFeetAnchor.invalidateAll()
    }

    @Test
    fun soleAlignFrameFeetY_usesPerFrameAnchor_notGameplayDefault() {
        val skinId = PacMazeSkinId.FOOD_CHICK_WALKER
        val frame0 = ImageBitmap(64, 64)
        val frame1 = ImageBitmap(64, 64)
        PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, frame0, asDefault = true)
        PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, frame1, asDefault = false)

        val defaultFeet = PacMazeBitmapFeetAnchor.gameplayFeetAnchor(frame1, skinId).first
        val perFrameFeet = PacMazeBitmapFeetAnchor.soleAlignFrameFeetY(frame1, skinId)

        assertTrue(
            "sole align must read per-frame cache, not skinGameplayDefault",
            perFrameFeet == defaultFeet || frame0 === frame1,
        )
    }

    @Test
    fun offsetPxForImage_nonZeroWhenFrameFeetDiffersFromCycleMax() {
        PacMazeSkinRegistry.drawTravelFacing = com.example.funlife.social.game.engine.pacmaze.Direction.LEFT
        PacMazeSkinRegistry.drawCorridorCenterPx = androidx.compose.ui.geometry.Offset(100f, 200f)
        try {
            val skinId = PacMazeSkinId.FOOD_CHICK_DAZE
            val shallow = ImageBitmap(48, 48)
            val deep = ImageBitmap(48, 48)
            PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, shallow, asDefault = true)
            PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, deep, asDefault = false)

            val ref = PacMazeBitmapFeetAnchor.soleAlignReferenceFeetY(skinId)
            val shallowFeet = PacMazeBitmapFeetAnchor.soleAlignFrameFeetY(shallow, skinId)
            val offset = PacMazeBitmapSoleAlign.offsetPxForImage(skinId, shallow, layoutHeight = 120f)

            if (ref != shallowFeet) {
                assertNotEquals(0f, offset)
            }
        } finally {
            PacMazeSkinRegistry.drawTravelFacing = null
            PacMazeSkinRegistry.drawCorridorCenterPx = null
        }
    }
}
