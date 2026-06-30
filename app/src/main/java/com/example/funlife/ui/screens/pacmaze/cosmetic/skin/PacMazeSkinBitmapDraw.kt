package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.debug.PacMazeBitmapDrawDiag
import com.example.funlife.ui.screens.pacmaze.debug.PacMazeMotionDiag
import kotlin.math.max
import kotlin.math.min

/**
 * 序列帧/位图皮肤：内容中心锚定 + 四向旋转；行走皮肤裁剪至 wallBox ∩ 地图内缘。
 */
internal object PacMazeSkinBitmapDraw {

    private const val CELL_HEIGHT_FRAC = 1.0f
    private const val CELL_WIDTH_FRAC = 0.92f
    private const val CLIP_INSET_FRAC = 0.08f

    val defaultCellHeightFrac: Float get() = CELL_HEIGHT_FRAC
    val defaultCellWidthFrac: Float get() = CELL_WIDTH_FRAC

    const val IKUN_CELL_HEIGHT_FRAC = PacMazeIkunGameplayScale.HEIGHT_CELL_FRAC
    const val IKUN_CELL_WIDTH_FRAC = PacMazeIkunGameplayScale.WIDTH_CELL_FRAC

    fun estimateCorridorCellPx(radius: Float): Float = radius / 0.44f

    private fun noteLayoutFeetIfNeeded(
        layout: PacMazeSkinLayoutEngine.Layout,
        skinId: PacMazeSkinId?,
        frameIndex: Int = PacMazeSkinRegistry.drawDiagFrameIndex,
    ) {
        if (skinId == null) return
        val entityId = PacMazeSkinRegistry.drawDiagEntityId ?: return
        PacMazeMotionDiag.noteBitmapLayoutFeet(
            entityId = entityId,
            skinId = skinId.name,
            feetY = layout.feetCenter.y,
            layoutTopY = layout.topLeft.y,
            logicY = PacMazeSkinRegistry.drawDiagLogicY,
            frameIndex = frameIndex,
        )
    }

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
        val walkSkin = tallGameplay && skinId != null && PacMazeBitmapWalkCatalog.contains(skinId)
        val layout = if (walkSkin) {
            val (cellW, cellH) = PacMazeBitmapWalkLayoutSizing.manifestWalkCellSize(skinId!!)
                ?: (image.width to image.height)
            val layoutSpan = PacMazeBitmapWalkLayoutSizing.layoutOpaqueSpan(skinId, PacMazeSkinAnimClip.WALK)
            PacMazeSkinLayoutEngine.layoutFromCell(
                center = center,
                radius = radius,
                corridorCellPx = corridorCellPx,
                cellW = cellW,
                cellH = cellH,
                walkBob = walkBob,
                diameterMul = diameterMul,
                cellHeightFrac = cellHeightFrac,
                cellWidthFrac = cellWidthFrac,
                tallGameplay = tallGameplay,
                verticalCellPx = verticalCellPx,
                tileCellPx = tileCellPx,
                tileBottomY = tileBottomY,
                skinId = skinId,
                facing = facing,
                travelFacing = travelFacing,
                centerAnchored = centerAnchored,
                contentHeightFrac = layoutSpan.minHeightFrac,
                contentWidthFrac = layoutSpan.minWidthFrac,
                clip = PacMazeSkinAnimClip.WALK,
            )
        } else {
            layout(
                center, radius, corridorCellPx, image, walkBob, diameterMul,
                cellHeightFrac, cellWidthFrac, tallGameplay, verticalCellPx, tileCellPx, tileBottomY,
                skinId, facing = facing, travelFacing = travelFacing, centerAnchored = centerAnchored,
            )
        }
        val profile = skinId?.let { PacMazeSkinRenderProfileCatalog.profile(it) }
        noteLayoutFeetIfNeeded(layout, skinId)
        drawGroundShadow(scope, layout, radius, corridorCellPx, tallGameplay || centerAnchored)

        val drawBlock: DrawScope.() -> Unit = {
            if (tallGameplay || centerAnchored) {
                drawOrientedBitmapOrSheet(
                    walkSkin, image, sheet = null, frameIndex = 0, layout, facing, profile, skinId,
                )
            } else {
                FamilySkinHelpers.withBitmapFacing(this, center, facing) {
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

        clipWalkBitmap(scope, walkSkin) { drawBlock() }
    }

    /** 行走皮肤局内不 clip（定标已贴通道；clip 易切脚/压尺寸感）。 */
    private fun clipWalkBitmap(scope: DrawScope, walkSkin: Boolean, block: DrawScope.() -> Unit) {
        block(scope)
    }

    private fun DrawScope.drawOrientedBitmapOrSheet(
        walkSkin: Boolean,
        image: ImageBitmap,
        sheet: PacMazeSkinSheetPlayback?,
        frameIndex: Int,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: PacMazeSkinId?,
        clip: PacMazeSkinAnimClip? = null,
    ) {
        val frameAlign = when {
            sheet != null && skinId != null && clip != null ->
                PacMazeBitmapFrameAlign.forSheet(skinId, clip, sheet, frameIndex, layout.height)
            skinId != null && sheet == null ->
                PacMazeBitmapFrameAlign.forImage(skinId, image, layout.height)
            else -> PacMazeBitmapFrameAlign.Result()
        }
        PacMazeBitmapDrawDiag.noteOrientedDraw(
            skinId = skinId,
            frameIndex = frameIndex,
            layout = layout,
            soleAlignOffsetY = frameAlign.soleOffsetY,
            scaleY = frameAlign.scaleY,
            facing = facing,
            image = if (sheet == null) image else null,
            sheet = sheet,
            clip = clip,
            drawPath = if (sheet != null) "oriented_sheet" else "oriented_image",
        )
        if (sheet != null) {
            drawOrientedSheet(
                sheet, frameIndex, layout, facing, profile, skinId,
                frameAlign.soleOffsetY, frameAlign.scaleY,
            )
        } else {
            drawOrientedBitmap(
                image, layout, facing, profile, skinId,
                frameAlign.soleOffsetY, frameAlign.scaleY,
            )
        }
    }

    fun drawSheet(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
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
        clip: PacMazeSkinAnimClip? = null,
        centerAnchored: Boolean = false,
        travelFacing: Direction? = PacMazeSkinRegistry.drawTravelFacing,
    ) {
        val layoutClip = clip ?: PacMazeSkinAnimClip.WALK
        val (layoutCellW, layoutCellH) = if (skinId != null) {
            PacMazeBitmapWalkLayoutSizing.resolveSheetLayoutCellSize(skinId, layoutClip, sheet)
        } else {
            sheet.cellW to sheet.cellH
        }
        val feetY = when {
            skinId != null && clip != null ->
                PacMazeSheetCellFeetCache.cellFeetY(skinId, clip, sheet, frameIndex)
                    ?: PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, clip, sheet)
            else -> null
        } ?: 0.90f
        if (tallGameplay && skinId != null && clip != null) {
            PacMazeSheetCellFeetCache.ensureMetrics(skinId, clip, sheet)
        }
        val stableFeetY = if (skinId != null && clip != null) {
            PacMazeBitmapWalkLayoutSizing.resolveSheetLayoutFeetFracY(skinId, layoutClip, sheet)
                ?: PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, clip, sheet)
                ?: feetY
        } else {
            feetY
        }
        val layoutSpan = if (skinId != null) {
            PacMazeBitmapWalkLayoutSizing.layoutOpaqueSpan(skinId, PacMazeSkinAnimClip.WALK)
        } else {
            null
        }
        val layout = PacMazeSkinLayoutEngine.layoutFromCell(
            center = center,
            radius = radius,
            corridorCellPx = corridorCellPx,
            cellW = layoutCellW,
            cellH = layoutCellH,
            walkBob = walkBob,
            diameterMul = diameterMul,
            cellHeightFrac = cellHeightFrac,
            cellWidthFrac = cellWidthFrac,
            tallGameplay = tallGameplay,
            verticalCellPx = verticalCellPx,
            tileCellPx = tileCellPx,
            tileBottomY = tileBottomY,
            skinId = skinId,
            facing = facing,
            travelFacing = travelFacing,
            centerAnchored = centerAnchored,
            feetFracY = stableFeetY,
            contentHeightFrac = layoutSpan?.minHeightFrac
                ?: (1f / PacMazeIkunGameplayScale.BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL),
            contentWidthFrac = layoutSpan?.minWidthFrac
                ?: (1f / PacMazeIkunGameplayScale.BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL),
            clip = layoutClip,
        )
        val walkSkin = tallGameplay && skinId != null && PacMazeBitmapWalkCatalog.contains(skinId)
        val profile = skinId?.let { PacMazeSkinRenderProfileCatalog.profile(it) }
        noteLayoutFeetIfNeeded(layout, skinId, frameIndex)
        drawGroundShadow(scope, layout, radius, corridorCellPx, tallGameplay || centerAnchored)

        val drawBlock: DrawScope.() -> Unit = {
            if (tallGameplay || centerAnchored) {
                drawOrientedBitmapOrSheet(
                    walkSkin, image = sheet.bitmap, sheet, frameIndex, layout, facing, profile, skinId, layoutClip,
                )
            } else {
                val src = sheet.srcRect(frameIndex)
                FamilySkinHelpers.withBitmapFacing(this, center, facing) {
                    val left = layout.topLeft.x.toInt()
                    val top = layout.topLeft.y.toInt()
                    val w = layout.width.toInt().coerceAtLeast(1)
                    val h = layout.height.toInt().coerceAtLeast(1)
                    drawImage(
                        image = sheet.bitmap,
                        srcOffset = IntOffset(src.left, src.top),
                        srcSize = IntSize(src.width, src.height),
                        dstOffset = IntOffset(left, top),
                        dstSize = IntSize(w, h),
                    )
                }
            }
        }
        clipWalkBitmap(scope, walkSkin) { drawBlock() }
    }

    private fun DrawScope.drawOrientedBitmap(
        image: ImageBitmap,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: PacMazeSkinId?,
        soleAlignOffsetY: Float = 0f,
        scaleY: Float = 1f,
    ) {
        PacMazeSkinTransform.run {
            drawOrientedBitmap(
                image, layout, facing, profile, skinId,
                soleAlignOffsetY = soleAlignOffsetY, scaleY = scaleY,
            )
        }
    }

    private fun DrawScope.drawOrientedSheet(
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: PacMazeSkinId?,
        soleAlignOffsetY: Float = 0f,
        scaleY: Float = 1f,
    ) {
        PacMazeSkinTransform.run {
            drawOrientedSheet(
                sheet, frameIndex, layout, facing, profile, skinId,
                soleAlignOffsetY = soleAlignOffsetY, scaleY = scaleY,
            )
        }
    }
}
