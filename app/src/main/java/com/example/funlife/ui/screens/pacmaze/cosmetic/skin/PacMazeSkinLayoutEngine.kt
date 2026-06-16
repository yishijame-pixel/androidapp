package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlin.math.max
import kotlin.math.min

/** 位图皮肤布局：脚点锚定 + 通道贴合 + HUD 等比视觉缩放。 */
internal object PacMazeSkinLayoutEngine {

    private const val FEET_Y_IN_SPRITE = 0.90f

    data class Layout(
        val width: Float,
        val height: Float,
        val topLeft: Offset,
        val feetCenter: Offset,
        val feetFrac: Float,
        val feetFracX: Float = 0.5f,
    )

    /**
     * 100% 滑条下的通道基准尺寸（等比）；[visualScale] 在 layout 中统一乘到 h/w。
     */
    fun computeIkunUniformSize(
        aspect: Float,
        verticalCellPx: Float,
        corridorCellPx: Float,
        cellHeightFrac: Float,
    ): Pair<Float, Float> {
        val nominalH = max(
            verticalCellPx * cellHeightFrac,
            verticalCellPx * PacMazeIkunGameplayScale.MIN_HEIGHT_CELL_FRAC,
        )
        val nominalW = nominalH * aspect
        val maxAcrossPx = PacMazeIkunGameplayScale.corridorAcrossSpanPx(corridorCellPx)
        val baseScale = maxAcrossPx / nominalH.coerceAtLeast(1f)
        return nominalH * baseScale to nominalW * baseScale
    }

    private fun readVisualScale(): Float = PacMazeIkunGameplayScale.hudVisualScale(
        PacMazeSkinRegistry.drawUserScale,
    )

    private fun applyUniformVisualScale(h: Float, w: Float, visualScale: Float): Pair<Float, Float> =
        h * visualScale to w * visualScale

    private fun clampCenterAnchoredToCorridor(
        h: Float,
        w: Float,
        corridorCellPx: Float,
    ): Pair<Float, Float> {
        val maxAcross = PacMazeIkunGameplayScale.corridorAcrossSpanPx(corridorCellPx)
        if (h <= maxAcross) return h to w
        val s = maxAcross / h.coerceAtLeast(1f)
        return h * s to w * s
    }

    fun layout(
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        image: ImageBitmap,
        walkBob: Float = 0f,
        diameterMul: Float = 2.55f,
        cellHeightFrac: Float = 0.90f,
        cellWidthFrac: Float = 0.76f,
        tallGameplay: Boolean = false,
        verticalCellPx: Float = corridorCellPx,
        tileCellPx: Float = corridorCellPx,
        tileBottomY: Float? = null,
        skinId: PacMazeSkinId? = null,
        facing: Direction? = null,
        travelFacing: Direction? = null,
        centerAnchored: Boolean = false,
    ): Layout {
        val aspect = image.width.toFloat() / image.height.coerceAtLeast(1)
        val maxH = if (tallGameplay) verticalCellPx * cellHeightFrac else corridorCellPx * cellHeightFrac
        val maxW = corridorCellPx * cellWidthFrac
        val visualScale = if (tallGameplay || centerAnchored) readVisualScale() else 1f
        var h: Float
        var w: Float
        var feetFrac = if (centerAnchored) 0.5f else FEET_Y_IN_SPRITE
        var feetFracX = 0.5f
        if (tallGameplay || centerAnchored) {
            val (fy, fx) = PacMazeBitmapFeetAnchor.gameplayFeetAnchor(image, skinId)
            feetFrac = fy.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
            feetFracX = if (tallGameplay) {
                fx.coerceIn(0.08f, 0.92f)
            } else {
                fx.coerceIn(0.12f, 0.88f)
            }
        }

        if (tallGameplay) {
            val (uh, uw) = computeIkunUniformSize(aspect, verticalCellPx, corridorCellPx, cellHeightFrac)
            val scaled = applyUniformVisualScale(uh, uw, visualScale)
            h = scaled.first
            w = scaled.second
        } else {
            h = min(radius * diameterMul, maxH)
            w = h * aspect
            if (w > maxW) {
                w = maxW
                h = w / aspect
            }
            if (centerAnchored) {
                val clamped = clampCenterAnchoredToCorridor(h, w, corridorCellPx)
                val scaled = applyUniformVisualScale(clamped.first, clamped.second, visualScale)
                h = scaled.first
                w = scaled.second
            } else {
                h *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
                w *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
            }
        }

        val travelAxis = travelFacing ?: facing
        val verticalTravel = travelAxis == Direction.UP || travelAxis == Direction.DOWN
        val horizontalTravel = travelAxis == Direction.LEFT || travelAxis == Direction.RIGHT
        /** 横走/待机：内容中心对齐走廊中心，HUD 缩放上下对称（非脚点单向长高）。 */
        val useCorridorCenterPivot = (tallGameplay || centerAnchored) && !verticalTravel

        val pivotFracY = when {
            useCorridorCenterPivot -> 0.5f
            verticalTravel && (tallGameplay || centerAnchored) -> 0.5f
            else -> feetFrac
        }
        val pivotFracX = when {
            useCorridorCenterPivot -> 0.5f
            verticalTravel && (tallGameplay || centerAnchored) -> 0.5f
            else -> feetFracX
        }

        val feet = when {
            useCorridorCenterPivot -> Offset(center.x, center.y + walkBob)
            PacMazeSkinRegistry.drawFeetAnchorPx != null -> {
                val groundNudgePx = tileCellPx * PacMazeIkunGameplayScale.FEET_GROUND_NUDGE_CELL_FRAC
                val floorLineY = (tileBottomY
                    ?: PacMazeSkinRegistry.drawTileBottomYPx
                    ?: center.y) + groundNudgePx
                val anchor = PacMazeSkinRegistry.drawFeetAnchorPx!!
                val floorY = floorLineY + walkBob
                val y = if (horizontalTravel && (tallGameplay || centerAnchored)) {
                    val opaqueBottomFrac = PacMazeBitmapFeetAnchor.feetYFraction(image, skinId)
                        .coerceIn(pivotFracY, 0.999f)
                    floorY - h * (opaqueBottomFrac - pivotFracY)
                } else {
                    anchor.y + walkBob
                }
                Offset(anchor.x, y)
            }
            else -> Offset(
                center.x,
                (tileBottomY ?: center.y) + walkBob,
            )
        }

        val top = feet.y - h * pivotFracY
        val left = feet.x - w * pivotFracX
        return Layout(
            width = w,
            height = h,
            topLeft = Offset(left, top),
            feetCenter = feet,
            feetFrac = pivotFracY,
            feetFracX = pivotFracX,
        )
    }
}
