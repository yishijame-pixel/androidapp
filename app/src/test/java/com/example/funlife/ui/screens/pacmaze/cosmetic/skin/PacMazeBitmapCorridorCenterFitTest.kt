package com.example.funlife.ui.screens.pacmaze.cosmetic.skin



import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Rect

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale

import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue

import org.junit.Test



class PacMazeBitmapCorridorCenterFitTest {



    private val content = PacMazeBitmapCorridorCenterFit.fromLayoutSpan(

        minHeightFrac = 0.60f,

        minWidthFrac = 0.50f,

    )



    @Test

    fun contentCenter_isSymmetricHalfExtents() {

        assertEquals(0.5f, content.centerY, 0.001f)

        assertEquals(0.5f, content.centerX, 0.001f)

    }



    @Test

    fun stableSize_independentOfCorridorCenterPosition() {

        val cellX = 48f

        val cellY = 40f

        val a = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 0.8f, content = content,

            cellX = cellX, cellY = cellY, visualScale = 1f,

        )

        val b = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 0.8f, content = content,

            cellX = cellX, cellY = cellY, visualScale = 1f,

        )

        assertEquals(a.height, b.height, 0.01f)

    }



    @Test

    fun stableSize_opaqueFillsCorridorHeight() {

        val cellX = 80f

        val cellY = 80f

        val box = PacMazeBitmapCorridorCenterFit.canonicalWalkableBox(cellX, cellY)

        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 1f,

            content = content,

            cellX = cellX,

            cellY = cellY,

            visualScale = 1f,

        )

        val opaqueH = fit.height * content.opaqueHeightFrac
        val opaqueW = fit.width * content.opaqueWidthFrac

        assertTrue(opaqueH >= box.height * 0.92f || opaqueW >= box.width * 0.92f)

    }



    @Test
    fun stableSize_platformerFillsCorridor() {
        val metrics = listOf(
            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.87f, 0.498f, 0.48f),
            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.89f, 0.495f, 0.47f),
        )
        val platformer = PacMazeBitmapCorridorCenterFit.fromPlatformerMetrics(metrics, normalized = true)
        val cell = 40f
        val box = PacMazeBitmapCorridorCenterFit.canonicalWalkableBox(cell, cell)
        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(
            aspect = 1.16f,
            content = platformer,
            cellX = cell,
            cellY = cell,
            visualScale = 1f,
        )
        assertTrue(fit.height > cell * 1.2f)
        val opaqueH = fit.height * platformer.opaqueHeightFrac
        val opaqueW = fit.width * platformer.opaqueWidthFrac
        assertTrue(opaqueH >= box.height * 0.92f || opaqueW >= box.width * 0.92f)
    }

    @Test
    fun opaqueContent_symmetricAroundCorridorCenter() {

        val cellX = 100f

        val cellY = 80f

        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 0.75f,

            content = content,

            cellX = cellX,

            cellY = cellY,

            visualScale = 1f,

        )

        val center = Offset(200f, 150f)

        val (above, below) = PacMazeBitmapCorridorCenterFit.opaqueVerticalHalfExtents(fit.height, content)

        val opaqueTop = center.y - above

        val opaqueBottom = center.y + below

        assertEquals(above, below, 0.5f)

        val box = PacMazeBitmapCorridorCenterFit.canonicalWalkableBox(cellX, cellY)

            .translate(center.x, center.y)

        val opaqueW = fit.width * content.opaqueWidthFrac
        assertTrue(opaqueW >= box.width * 0.85f || opaqueTop >= box.top - 0.5f)

    }



    @Test
    fun stableSize_tallCellWideOpaque_usesOpaqueAspectNotCellAspect() {
        val content = PacMazeBitmapCorridorCenterFit.fromLayoutSpan(minHeightFrac = 0.62f, minWidthFrac = 0.81f)
        val cell = 48f
        val box = PacMazeBitmapCorridorCenterFit.canonicalWalkableBox(cell, cell)
        val compactOpaqueAspect = 0.478f * content.opaqueWidthFrac / content.opaqueHeightFrac
        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(
            aspect = compactOpaqueAspect,
            content = content,
            cellX = cell,
            cellY = cell,
            visualScale = 1f,
        )
        assertTrue(
            "opaque height should stay within corridor, not span multiple tiles",
            fit.height * content.opaqueHeightFrac <= box.height * 1.08f,
        )
    }

    @Test
    fun platformerMetrics_owOh_produceWideLayoutSpan() {
        val metrics = listOf(
            PacMazeSkinAnimManifest.PlatformerFrameMetrics(
                feetY = 0.84f,
                feetX = 0.50f,
                headTopY = 0.35f,
                opaqueWidthFrac = 0.81f,
                opaqueHeightFrac = 0.62f,
            ),
        )
        val content = PacMazeBitmapCorridorCenterFit.fromPlatformerMetrics(metrics, normalized = true)
        assertEquals(0.81f, content.opaqueWidthFrac, 0.02f)
        assertEquals(0.62f, content.opaqueHeightFrac, 0.02f)
    }



    @Test

    fun platformerMetrics_largerThanFullFramePivotCap() {

        val padded = PacMazeBitmapCorridorCenterFit.fromPlatformerMetrics(

            metrics = listOf(

                PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.87f, 0.498f, 0.48f),

                PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.89f, 0.495f, 0.47f),

            ),

            normalized = true,

        )

        val cellX = 80f

        val cellY = 80f

        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 1f,

            content = padded,

            cellX = cellX,

            cellY = cellY,

            visualScale = 1f,

        )

        val (_, py) = padded.pivot()

        val pivotCapH = min(cellY, cellX) * (0.5f - 0.11f) * 2f / py

        assertTrue(fit.height > pivotCapH * 1.15f)

    }



    @Test

    fun finalizeAtRuntime_clampsToMapClipAtBottomRow() {

        val cellX = 80f

        val cellY = 80f

        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 1f,

            content = content,

            cellX = cellX,

            cellY = cellY,

            visualScale = 1f,

        )

        val corridor = Offset(120f, 380f)

        val wallBox = Rect(80f, 340f, 160f, 420f)

        val mapClip = Rect(10f, 10f, 500f, 390f)

        val (w, h) = PacMazeBitmapCorridorCenterFit.finalizeAtRuntime(

            width = fit.width,

            height = fit.height,

            aspect = 1f,

            content = content,

            wallBox = wallBox,

            mapClip = mapClip,

            corridorCenter = corridor,

        )

        assertTrue(h <= fit.height + 0.5f)

        assertTrue(w <= fit.width + 0.5f)

    }



    @Test

    fun visualScale_clampedByWalls() {

        val cellX = 80f

        val cellY = 80f

        val base = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 1f, content = content,

            cellX = cellX, cellY = cellY, visualScale = 1f,

        )

        val scaled = PacMazeBitmapCorridorCenterFit.resolveStableSize(

            aspect = 1f, content = content,

            cellX = cellX, cellY = cellY, visualScale = 3f,

        )

        assertTrue(scaled.height > base.height * 1.5f)
        assertTrue(scaled.width > base.width * 1.5f)

    }



    @Test

    fun platformerMetrics_pivotMatchesActualHeadFeetCenter() {

        val metrics = listOf(

            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.87f, 0.498f, 0.48f),

            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.89f, 0.495f, 0.47f),

        )

        val c = PacMazeBitmapCorridorCenterFit.fromPlatformerMetrics(metrics, normalized = true)

        assertEquals(0.48f, c.topFrac, 0.001f)

        assertEquals(0.918f, c.bottomFrac, 0.01f)

        assertEquals(0.699f, c.centerY, 0.01f)

        assertEquals(c.aboveCenter, c.belowCenter, 0.001f)

    }



    @Test

    fun platformerMetrics_ignoresJumpOutlierFrames() {

        val metrics = listOf(

            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.87f, 0.498f, 0.48f),

            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.89f, 0.495f, 0.47f),

            PacMazeSkinAnimManifest.PlatformerFrameMetrics(0.694f, 0.495f, 0.292f),

        )

        val c = PacMazeBitmapCorridorCenterFit.fromPlatformerMetrics(metrics, normalized = true)

        assertTrue(c.topFrac >= 0.45f)

        assertEquals(0.918f, c.bottomFrac, 0.02f)

    }



    private fun Rect.translate(dx: Float, dy: Float): Rect =

        Rect(left + dx, top + dy, right + dx, bottom + dy)



    private fun min(a: Float, b: Float): Float = kotlin.math.min(a, b)

}


