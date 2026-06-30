package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale

/** 行走皮肤走廊贴合 legacy 入口。 */
internal object PacMazeBitmapCorridorFit {

    data class SymmetricFitResult(
        val width: Float,
        val height: Float,
        val scale: Float,
    )

    fun pivotFromOpaqueSpan(span: PacMazeBitmapContentTrim.OpaqueContentSpan): Pair<Float, Float> =
        PacMazeBitmapCorridorCenterFit.fromOpaqueSpan(span).pivot()

    fun pivotFromContentSpan(minHeightFrac: Float, minWidthFrac: Float): Pair<Float, Float> =
        PacMazeBitmapCorridorCenterFit.fromLayoutSpan(minHeightFrac, minWidthFrac).pivot()

    @Suppress("UNUSED_PARAMETER")
    fun resolveWalkCorridorSpriteSize(
        aspect: Float,
        contentHeightFrac: Float,
        contentWidthFrac: Float,
        wallBox: Rect?,
        cellPx: Float,
        pivotFracX: Float = 0.5f,
        pivotFracY: Float = 0.5f,
        anchor: Offset? = null,
    ): SymmetricFitResult {
        val cellX = PacMazeSkinRegistry.drawCellXPx ?: cellPx
        val cellY = PacMazeSkinRegistry.drawCellYPx ?: cellPx
        val content = PacMazeBitmapCorridorCenterFit.fromLayoutSpan(contentHeightFrac, contentWidthFrac)
        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(
            aspect = aspect,
            content = content,
            cellX = cellX,
            cellY = cellY,
            visualScale = PacMazeIkunGameplayScale.bitmapLayoutVisualScale(
                PacMazeSkinRegistry.drawUserScale,
            ),
        )
        return SymmetricFitResult(fit.width, fit.height, 1f)
    }
}
