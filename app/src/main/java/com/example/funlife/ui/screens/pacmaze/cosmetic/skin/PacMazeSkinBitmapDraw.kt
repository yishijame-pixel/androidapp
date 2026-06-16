package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlin.math.max
import kotlin.math.min

/**
 * 序列帧/位图皮肤：脚点锚定 + 四向旋转 + 完整 PNG。
 * ikun 可视觉覆盖墙体；完整绘制 PNG，不做 clip。
 */
internal object PacMazeSkinBitmapDraw {

    private const val CELL_HEIGHT_FRAC = 0.82f
    private const val CELL_WIDTH_FRAC = 0.68f
    private const val CLIP_INSET_FRAC = 0.08f

    val defaultCellHeightFrac: Float get() = CELL_HEIGHT_FRAC
    val defaultCellWidthFrac: Float get() = CELL_WIDTH_FRAC

    const val IKUN_CELL_HEIGHT_FRAC = PacMazeIkunGameplayScale.HEIGHT_CELL_FRAC
    const val IKUN_CELL_WIDTH_FRAC = PacMazeIkunGameplayScale.WIDTH_CELL_FRAC

    fun estimateCorridorCellPx(radius: Float): Float = radius / 0.44f

    fun layout(
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        image: ImageBitmap,
        walkBob: Float = 0f,
        diameterMul: Float = 2.55f,
        cellHeightFrac: Float = CELL_HEIGHT_FRAC,
        cellWidthFrac: Float = CELL_WIDTH_FRAC,
        tallGameplay: Boolean = false,
        verticalCellPx: Float = corridorCellPx,
        tileCellPx: Float = corridorCellPx,
        tileBottomY: Float? = null,
        skinId: PacMazeSkinId? = null,
        facing: Direction? = null,
        travelFacing: Direction? = null,
        centerAnchored: Boolean = false,
    ): PacMazeSkinLayoutEngine.Layout = PacMazeSkinLayoutEngine.layout(
        center, radius, corridorCellPx, image, walkBob, diameterMul,
        cellHeightFrac, cellWidthFrac, tallGameplay, verticalCellPx, tileCellPx, tileBottomY, skinId, facing,
        travelFacing, centerAnchored,
    )

    fun drawGroundShadow(
        scope: DrawScope,
        layout: PacMazeSkinLayoutEngine.Layout,
        radius: Float,
        corridorCellPx: Float = radius / 0.44f,
        tallGameplay: Boolean = false,
    ) {
        val feet = layout.feetCenter
        val shadowW = if (tallGameplay) {
            min(max(layout.width, layout.height) * 0.62f, corridorCellPx * 0.90f)
        } else {
            radius * 1.44f
        }
        val shadowH = if (tallGameplay) corridorCellPx * 0.18f else radius * 0.22f
        scope.drawOval(
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.26f),
            topLeft = Offset(feet.x - shadowW * 0.5f, feet.y - shadowH * 0.55f),
            size = Size(shadowW, shadowH),
        )
    }

    fun clipCorridor(
        scope: DrawScope,
        center: Offset,
        corridorCellPx: Float,
        block: DrawScope.() -> Unit,
    ) {
        val inset = corridorCellPx * CLIP_INSET_FRAC
        scope.clipRect(
            left = center.x - corridorCellPx * 0.5f + inset,
            top = center.y - corridorCellPx * 0.5f + inset,
            right = center.x + corridorCellPx * 0.5f - inset,
            bottom = center.y + corridorCellPx * 0.5f - inset,
            block = block,
        )
    }

    fun draw(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        image: ImageBitmap,
        facing: Direction,
        walkBob: Float = 0f,
        diameterMul: Float = 2.55f,
        cellHeightFrac: Float = CELL_HEIGHT_FRAC,
        cellWidthFrac: Float = CELL_WIDTH_FRAC,
        tallGameplay: Boolean = false,
        verticalCellPx: Float = corridorCellPx,
        tileCellPx: Float = corridorCellPx,
        tileBottomY: Float? = null,
        skinId: PacMazeSkinId? = null,
        centerAnchored: Boolean = false,
        travelFacing: Direction? = PacMazeSkinRegistry.drawTravelFacing,
    ) {
        val layout = layout(
            center, radius, corridorCellPx, image, walkBob, diameterMul,
            cellHeightFrac, cellWidthFrac, tallGameplay, verticalCellPx, tileCellPx, tileBottomY,
            skinId, facing = facing, travelFacing = travelFacing, centerAnchored = centerAnchored,
        )
        val profile = skinId?.let { PacMazeSkinRenderProfileCatalog.profile(it) }
        drawGroundShadow(scope, layout, radius, corridorCellPx, tallGameplay || centerAnchored)

        if (tallGameplay || centerAnchored) {
            scope.drawOrientedBitmap(image, layout, facing, profile, skinId)
        } else {
            FamilySkinHelpers.withBitmapFacing(scope, center, facing) {
                val left = layout.topLeft.x.toInt()
                val top = layout.topLeft.y.toInt()
                val w = layout.width.toInt().coerceAtLeast(1)
                val h = layout.height.toInt().coerceAtLeast(1)
                drawImage(
                    image = image,
                    dstOffset = androidx.compose.ui.unit.IntOffset(left, top),
                    dstSize = androidx.compose.ui.unit.IntSize(w, h),
                )
            }
        }
    }

    private fun DrawScope.drawOrientedBitmap(
        image: ImageBitmap,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: PacMazeSkinId?,
    ) {
        PacMazeSkinTransform.run {
            drawOrientedBitmap(image, layout, facing, profile, skinId)
        }
    }
}
