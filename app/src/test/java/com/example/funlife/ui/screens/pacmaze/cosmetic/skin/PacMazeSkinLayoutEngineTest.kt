package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeSkinLayoutEngineTest {

    private val corridorPx = 40f
    private val verticalPx = 48f
    private val heightFrac = PacMazeIkunGameplayScale.HEIGHT_CELL_FRAC

    private fun targetAcross(cellPerp: Float): Float =
        PacMazeIkunGameplayScale.corridorAcrossSpanPx(cellPerp)

    @Test
    fun baseSize_usesBitmapCorridorSpanWithDefaultContentFill() {
        val aspect = 1.45f
        val (h, _) = PacMazeSkinLayoutEngine.computeIkunUniformSize(
            aspect = aspect,
            verticalCellPx = verticalPx,
            corridorCellPx = corridorPx,
            cellHeightFrac = heightFrac,
        )
        val expected = PacMazeIkunGameplayScale.bitmapCorridorAcrossSpanPx(corridorPx) *
            PacMazeIkunGameplayScale.bitmapContentFillMul(1f, 1f) *
            PacMazeIkunGameplayScale.BITMAP_CORRIDOR_OPAQUE_BOOST
        assertEquals(expected, h, 1.5f)
    }

    @Test
    fun uniformVisualScale_isLinearWithHudSlider() {
        assertEquals(1f, PacMazeIkunGameplayScale.uniformVisualScale(1f), 0.001f)
        assertEquals(1.5f, PacMazeIkunGameplayScale.uniformVisualScale(1.5f), 0.001f)
        assertEquals(0.75f, PacMazeIkunGameplayScale.uniformVisualScale(0.75f), 0.001f)
    }

    @Test
    fun visualScale_doublesBothDimensionsPreservingAspect() {
        val aspect = 2.2f
        val base = PacMazeSkinLayoutEngine.computeIkunUniformSize(
            aspect, verticalPx, corridorPx, heightFrac,
        )
        val scale = 2f
        val h = base.first * scale
        val w = base.second * scale
        assertEquals(aspect, w / h, 0.02f)
        assertEquals(base.first * 2f, h, 0.01f)
    }

    @Test
    fun corridorFitCellPx_usesMinAxis() {
        assertEquals(30f, PacMazeIkunGameplayScale.corridorFitCellPx(50f, 30f), 0.01f)
        assertEquals(
            PacMazeIkunGameplayScale.corridorFitCellPx(50f, 30f),
            PacMazeIkunGameplayScale.perpendicularCorridorCellPx(50f, 30f, null),
            0.01f,
        )
    }

    @Test
    fun ikunSize_sameForAllFacings_whenCellAspectDiffers() {
        val aspect = 1.6f
        val cellX = 52f
        val cellY = 34f
        val fit = PacMazeIkunGameplayScale.corridorFitCellPx(cellX, cellY)
        val sizeA = PacMazeSkinLayoutEngine.computeIkunUniformSize(aspect, cellY, fit, heightFrac)
        val sizeB = PacMazeSkinLayoutEngine.computeIkunUniformSize(aspect, cellY, fit, heightFrac)
        assertEquals(sizeA.first, sizeB.first, 0.01f)
        assertEquals(sizeA.second, sizeB.second, 0.01f)
    }

    @Test
    fun visualScale_growsFromCenterPivot_symmetricUpDown() {
        val centerY = 200f
        val baseH = 40f
        val scaledH = baseH * 2f
        fun top(h: Float) = centerY - h * 0.5f
        fun bottom(h: Float) = centerY + h * 0.5f
        assertEquals(centerY, (top(baseH) + bottom(baseH)) * 0.5f, 0.01f)
        assertEquals(top(baseH) - (scaledH - baseH) * 0.5f, top(scaledH), 0.01f)
        assertEquals(bottom(baseH) + (scaledH - baseH) * 0.5f, bottom(scaledH), 0.01f)
    }

    @Test
    fun horizontalFloorSnap_alignsOpaqueBottomToFloorLine() {
        val floorLine = 200f
        val h = 60f
        val pivotFrac = 0.85f
        val opaqueBottomFrac = 0.96f
        val pivotY = floorLine - h * (opaqueBottomFrac - pivotFrac)
        val opaqueContactY = pivotY + h * (opaqueBottomFrac - pivotFrac)
        assertEquals(floorLine, opaqueContactY, 0.01f)
    }

    @Test
    fun wideAspect_keepsUniformScale() {
        val aspect = 5.5f
        val (h, w) = PacMazeSkinLayoutEngine.computeIkunUniformSize(
            aspect, verticalPx, corridorPx, heightFrac,
        )
        val expected = PacMazeIkunGameplayScale.bitmapCorridorAcrossSpanPx(corridorPx) *
            PacMazeIkunGameplayScale.bitmapContentFillMul(1f, 1f) *
            PacMazeIkunGameplayScale.BITMAP_CORRIDOR_OPAQUE_BOOST
        assertEquals(expected, h, 1.5f)
        assertEquals(aspect, w / h, 0.02f)
    }
}
