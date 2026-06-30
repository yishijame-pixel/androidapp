package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlin.math.max
import kotlin.math.min

/** 位图皮肤布局：内容中心 ↔ 过道中心 + 四向触墙对称定标 + HUD 等比缩放。 */
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
        contentHeightFrac: Float = 1f,
        contentWidthFrac: Float = 1f,
    ): Pair<Float, Float> {
        val targetOpaqueH = PacMazeIkunGameplayScale.bitmapOpaqueTargetHeightPx(
            verticalCellPx = verticalCellPx,
            corridorCellPx = corridorCellPx,
            cellHeightFrac = cellHeightFrac,
            contentHeightFrac = contentHeightFrac,
            contentWidthFrac = contentWidthFrac,
        )
        val h = targetOpaqueH
        val w = h * aspect
        return h to w
    }

    private fun readVisualScale(): Float = PacMazeIkunGameplayScale.bitmapLayoutVisualScale(
        PacMazeSkinRegistry.drawUserScale,
    )

    private fun applyUniformVisualScale(h: Float, w: Float, visualScale: Float): Pair<Float, Float> =
        h * visualScale to w * visualScale

    private fun applyBitmapContentFill(
        h: Float,
        w: Float,
        contentFillMul: Float,
    ): Pair<Float, Float> =
        if (contentFillMul > 1f) h * contentFillMul to w * contentFillMul else h to w

    private fun bitmapContentSpanForImage(
        image: ImageBitmap,
        skinId: PacMazeSkinId?,
    ): Pair<Float, Float> {
        val span = PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image)
        return span.heightFrac to span.widthFrac
    }

    private fun applyCorridorContentFit(
        @Suppress("UNUSED_PARAMETER") width: Float,
        @Suppress("UNUSED_PARAMETER") height: Float,
        @Suppress("UNUSED_PARAMETER") walkBob: Float,
        @Suppress("UNUSED_PARAMETER") pivotFracX: Float,
        @Suppress("UNUSED_PARAMETER") pivotFracY: Float,
        skinId: PacMazeSkinId?,
        tallGameplay: Boolean,
        @Suppress("UNUSED_PARAMETER") contentHeightFrac: Float = 1f,
        @Suppress("UNUSED_PARAMETER") contentWidthFrac: Float = 1f,
        @Suppress("UNUSED_PARAMETER") clip: PacMazeSkinAnimClip = PacMazeSkinAnimClip.WALK,
    ): Pair<Float, Float> {
        if (!usesCorridorSymmetricFit(skinId, tallGameplay) || skinId == null) {
            return width to height
        }
        val (baseW, baseH) = PacMazeBitmapWalkLayoutSizing.resolveGameplaySize(skinId)
        return baseW to baseH
    }

    private fun resolveCorridorContentPivot(
        image: ImageBitmap?,
        skinId: PacMazeSkinId?,
        contentHeightFrac: Float,
        contentWidthFrac: Float,
        @Suppress("UNUSED_PARAMETER") feetFracY: Float,
        @Suppress("UNUSED_PARAMETER") feetFracX: Float,
    ): Pair<Float, Float> {
        if (image != null && skinId != null && PacMazeBitmapWalkCatalog.contains(skinId)) {
            val span = PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image)
            return PacMazeBitmapCorridorFit.pivotFromOpaqueSpan(span)
        }
        return PacMazeBitmapCorridorFit.pivotFromContentSpan(contentHeightFrac, contentWidthFrac)
    }

    private fun resolveBitmapAnchor(
        center: Offset,
        walkBob: Float,
        skinId: PacMazeSkinId?,
        tallGameplay: Boolean,
        useCorridorSymmetric: Boolean,
        useContentCenterCorridor: Boolean,
        useCorridorCenterPivot: Boolean,
        horizontalTravel: Boolean,
        verticalTravel: Boolean,
        tallOrCenterAnchored: Boolean,
        h: Float,
        pivotFracY: Float,
        image: ImageBitmap?,
        feetFrac: Float,
        tileCellPx: Float,
        tileBottomY: Float?,
    ): Offset {
        if (useCorridorSymmetric || useContentCenterCorridor) {
            val cc = PacMazeSkinRegistry.drawCorridorCenterPx!!
            return Offset(cc.x, cc.y)
        }
        if (useCorridorCenterPivot) {
            return PacMazeBitmapCorridorDrawPolicy.corridorAnchorOrCenter(center, walkBob, horizontalTravel)
        }
        if (PacMazeSkinRegistry.drawFeetAnchorPx != null) {
            val groundNudgePx = tileCellPx * PacMazeIkunGameplayScale.FEET_GROUND_NUDGE_CELL_FRAC
            val floorLineY = (tileBottomY
                ?: PacMazeSkinRegistry.drawTileBottomYPx
                ?: center.y) + groundNudgePx
            val anchor = PacMazeSkinRegistry.drawFeetAnchorPx!!
            val floorY = floorLineY + walkBob
            val y = if (horizontalTravel && tallOrCenterAnchored && image != null) {
                val opaqueBottomFrac = stableGameplayFeetAnchor(skinId, image)
                    .first
                    .coerceIn(pivotFracY, 0.999f)
                floorY - h * (opaqueBottomFrac - pivotFracY)
            } else if (horizontalTravel && tallOrCenterAnchored) {
                floorY - h * (feetFrac.coerceIn(pivotFracY, 0.999f) - pivotFracY)
            } else {
                anchor.y + walkBob
            }
            return Offset(anchor.x, y)
        }
        return Offset(center.x, (tileBottomY ?: center.y) + walkBob)
    }

    private fun usesCorridorSymmetricFit(skinId: PacMazeSkinId?, tallGameplay: Boolean): Boolean =
        tallGameplay &&
            skinId != null &&
            PacMazeBitmapWalkCatalog.contains(skinId) &&
            PacMazeSkinRegistry.drawCorridorCenterPx != null &&
            PacMazeSkinRegistry.drawWallBoxRect != null

    private fun applyHorizontalSolePivot(
        skinId: PacMazeSkinId?,
        horizontalTravel: Boolean,
        pivotFracY: Float,
    ): Float {
        if (skinId == null || !horizontalTravel) return pivotFracY
        if (!PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob()) return pivotFracY
        PacMazeSkinAnimManifest.loadForSkin(skinId)?.anchorFrac?.y?.let { anchorY ->
            return anchorY.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        }
        return PacMazeBitmapFeetAnchor.rawCycleMaxFeetY(skinId)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
    }

    /** 横走动画：全帧共用 walk_1 脚点，避免换帧时鞋底 Y 跳变。 */
    private fun stableGameplayFeetAnchor(
        skinId: PacMazeSkinId?,
        image: ImageBitmap,
    ): Pair<Float, Float> {
        if (skinId != null && PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)) {
            return PacMazeBitmapFeetAnchor.gameplayFeetAnchorForSkin(skinId)
        }
        return PacMazeBitmapFeetAnchor.gameplayFeetAnchor(image, skinId)
    }

    /** 图片资源：wallBox + 过道中心可用时，走内容中心对称触墙定标。 */
    private fun resolveCorridorCenterFitSize(
        aspect: Float,
        contentHeightFrac: Float,
        contentWidthFrac: Float,
        image: ImageBitmap?,
        skinId: PacMazeSkinId?,
    ): Pair<Float, Float>? {
        if (PacMazeSkinRegistry.drawCorridorCenterPx == null) return null
        if (skinId != null && PacMazeBitmapWalkCatalog.contains(skinId)) {
            return PacMazeBitmapWalkLayoutSizing.resolveGameplaySize(skinId)
        }
        val cellX = PacMazeSkinRegistry.drawCellXPx ?: PacMazeSkinRegistry.drawCorridorCellPx ?: return null
        val cellY = PacMazeSkinRegistry.drawCellYPx ?: PacMazeSkinRegistry.drawVerticalCellPx ?: cellX
        val content = if (image != null) {
            PacMazeBitmapCorridorCenterFit.fromOpaqueSpan(
                PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image),
            )
        } else {
            PacMazeBitmapCorridorCenterFit.fromLayoutSpan(contentHeightFrac, contentWidthFrac)
        }
        val fit = PacMazeBitmapCorridorCenterFit.resolveStableSize(
            aspect = aspect,
            content = content,
            cellX = cellX,
            cellY = cellY,
            visualScale = readVisualScale(),
        )
        return fit.width to fit.height
    }

    private fun clampCenterAnchoredToCorridor(
        h: Float,
        w: Float,
        corridorCellPx: Float,
    ): Pair<Float, Float> {
        val maxAcross = PacMazeIkunGameplayScale.bitmapCorridorAcrossSpanPx(corridorCellPx)
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
        cellHeightFrac: Float = 0.96f,
        cellWidthFrac: Float = 0.80f,
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
        var usedContentCenterCorridor = false
        if (tallGameplay) {
            val (fy, fx) = stableGameplayFeetAnchor(skinId, image)
            feetFrac = fy.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
            feetFracX = fx.coerceIn(0.08f, 0.92f)
        }

        if (tallGameplay && usesCorridorSymmetricFit(skinId, tallGameplay) && skinId != null) {
            val corridorSize = PacMazeBitmapWalkLayoutSizing.resolveGameplaySize(skinId)
            w = corridorSize.first
            h = corridorSize.second
        } else if (tallGameplay) {
            val span = bitmapContentSpanForImage(image, skinId)
            val (layoutH, layoutW) = PacMazeIkunGameplayScale.bitmapLayoutOpaqueSpan(span.first, span.second)
            val (uh, uw) = computeIkunUniformSize(
                aspect = aspect,
                verticalCellPx = verticalCellPx,
                corridorCellPx = corridorCellPx,
                cellHeightFrac = cellHeightFrac,
                contentHeightFrac = layoutH,
                contentWidthFrac = layoutW,
            )
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
                val span = bitmapContentSpanForImage(image, skinId)
                val corridorFit = resolveCorridorCenterFitSize(
                    aspect = aspect,
                    contentHeightFrac = span.first,
                    contentWidthFrac = span.second,
                    image = image,
                    skinId = skinId,
                )
                if (corridorFit != null) {
                    h = corridorFit.second
                    w = corridorFit.first
                    usedContentCenterCorridor = true
                } else {
                    val clamped = clampCenterAnchoredToCorridor(h, w, corridorCellPx)
                    val filled = applyBitmapContentFill(
                        clamped.first,
                        clamped.second,
                        PacMazeIkunGameplayScale.bitmapContentFillMul(span.first, span.second),
                    )
                    val scaled = applyUniformVisualScale(filled.first, filled.second, visualScale)
                    h = scaled.first
                    w = scaled.second
                }
            } else {
                h *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
                w *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
            }
        }

        val travelAxis = travelFacing ?: facing
        val verticalTravel = travelAxis == Direction.UP || travelAxis == Direction.DOWN
        val horizontalTravel = travelAxis == Direction.LEFT || travelAxis == Direction.RIGHT
        val effectiveWalkBob = PacMazeBitmapCorridorDrawPolicy.effectiveWalkBob(walkBob, travelAxis)
        val useCorridorSymmetric = usesCorridorSymmetricFit(skinId, tallGameplay)
        val tallOrCenterAnchored = tallGameplay || centerAnchored
        val layoutClip = PacMazeSkinAnimClip.WALK
        val contentPivot = when {
            useCorridorSymmetric && skinId != null ->
                PacMazeBitmapWalkLayoutSizing.walkContentCenterPivot(skinId)
            useCorridorSymmetric -> {
                val spanForPivot = bitmapContentSpanForImage(image, skinId)
                resolveCorridorContentPivot(
                    image = image,
                    skinId = skinId,
                    contentHeightFrac = spanForPivot.first,
                    contentWidthFrac = spanForPivot.second,
                    feetFracY = feetFrac,
                    feetFracX = feetFracX,
                )
            }
            usedContentCenterCorridor && image != null && skinId != null &&
                PacMazeBitmapWalkCatalog.contains(skinId) ->
                PacMazeBitmapCorridorFit.pivotFromOpaqueSpan(
                    PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image),
                )
            usedContentCenterCorridor && skinId != null &&
                PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId) -> {
                val anchor = PacMazeBitmapFeetAnchor.gameplayFeetAnchorForSkin(skinId)
                anchor.second to anchor.first
            }
            usedContentCenterCorridor -> {
                val spanForPivot = bitmapContentSpanForImage(image, skinId)
                PacMazeBitmapCorridorFit.pivotFromContentSpan(spanForPivot.first, spanForPivot.second)
            }
            else -> feetFracX to feetFrac
        }
        val useCorridorCenterPivot = !useCorridorSymmetric && tallOrCenterAnchored && !verticalTravel

        var pivotFracY = when {
            useCorridorSymmetric -> contentPivot.second
            usedContentCenterCorridor -> contentPivot.second
            useCorridorCenterPivot -> 0.5f
            verticalTravel && tallOrCenterAnchored -> 0.5f
            else -> feetFrac
        }
        var pivotFracX = when {
            useCorridorSymmetric -> contentPivot.first
            usedContentCenterCorridor -> contentPivot.first
            useCorridorCenterPivot -> 0.5f
            verticalTravel && tallOrCenterAnchored -> 0.5f
            else -> feetFracX
        }

        pivotFracY = applyHorizontalSolePivot(skinId, horizontalTravel, pivotFracY)

        if (skinId != null && horizontalTravel && (centerAnchored || tallGameplay)) {
            val frozen = PacMazeBitmapStableLayoutCache.stabilize(
                skinId = skinId,
                height = h,
                width = w,
                pivotFracX = pivotFracX,
                pivotFracY = pivotFracY,
                layoutScale = PacMazeIkunGameplayScale.bitmapLayoutVisualScale(
                    PacMazeSkinRegistry.drawUserScale,
                ),
            )
            h = frozen.height
            w = frozen.width
            pivotFracX = frozen.pivotFracX
            pivotFracY = frozen.pivotFracY
        }

        val feet = resolveBitmapAnchor(
            center = center,
            walkBob = effectiveWalkBob,
            skinId = skinId,
            tallGameplay = tallGameplay,
            useCorridorSymmetric = useCorridorSymmetric,
            useContentCenterCorridor = usedContentCenterCorridor,
            useCorridorCenterPivot = useCorridorCenterPivot,
            horizontalTravel = horizontalTravel,
            verticalTravel = verticalTravel,
            tallOrCenterAnchored = tallOrCenterAnchored,
            h = h,
            pivotFracY = pivotFracY,
            image = image,
            feetFrac = feetFrac,
            tileCellPx = tileCellPx,
            tileBottomY = tileBottomY,
        )

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

    /** Sprite sheet 单格布局：用格宽高与预计算脚点，避免拆 61 帧进内存。 */
    fun layoutFromCell(
        center: Offset,
        radius: Float,
        corridorCellPx: Float,
        cellW: Int,
        cellH: Int,
        walkBob: Float = 0f,
        diameterMul: Float = 2.55f,
        cellHeightFrac: Float = 0.96f,
        cellWidthFrac: Float = 0.80f,
        tallGameplay: Boolean = false,
        verticalCellPx: Float = corridorCellPx,
        tileCellPx: Float = corridorCellPx,
        tileBottomY: Float? = null,
        skinId: PacMazeSkinId? = null,
        facing: Direction? = null,
        travelFacing: Direction? = null,
        centerAnchored: Boolean = false,
        feetFracY: Float = FEET_Y_IN_SPRITE,
        feetFracX: Float = 0.5f,
        contentHeightFrac: Float = 1f,
        contentWidthFrac: Float = 1f,
        clip: PacMazeSkinAnimClip? = null,
    ): Layout {
        val aspect = cellW.toFloat() / cellH.coerceAtLeast(1)
        val maxH = if (tallGameplay) verticalCellPx * cellHeightFrac else corridorCellPx * cellHeightFrac
        val maxW = corridorCellPx * cellWidthFrac
        val visualScale = if (tallGameplay || centerAnchored) readVisualScale() else 1f
        var h: Float
        var w: Float
        var feetFrac = if (centerAnchored) 0.5f else feetFracY.coerceIn(
            PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN,
            0.999f,
        )
        var feetFracXLocal = feetFracX.coerceIn(0.08f, 0.92f)
        var usedContentCenterCorridor = false

        if (tallGameplay && usesCorridorSymmetricFit(skinId, tallGameplay) && skinId != null) {
            val corridorSize = PacMazeBitmapWalkLayoutSizing.resolveGameplaySize(skinId)
            w = corridorSize.first
            h = corridorSize.second
        } else if (tallGameplay) {
            val (layoutH, layoutW) = PacMazeIkunGameplayScale.bitmapLayoutOpaqueSpan(
                contentHeightFrac,
                contentWidthFrac,
            )
            val (uh, uw) = computeIkunUniformSize(
                aspect = aspect,
                verticalCellPx = verticalCellPx,
                corridorCellPx = corridorCellPx,
                cellHeightFrac = cellHeightFrac,
                contentHeightFrac = layoutH,
                contentWidthFrac = layoutW,
            )
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
                val corridorFit = resolveCorridorCenterFitSize(
                    aspect = aspect,
                    contentHeightFrac = contentHeightFrac,
                    contentWidthFrac = contentWidthFrac,
                    image = null,
                    skinId = skinId,
                )
                if (corridorFit != null) {
                    h = corridorFit.second
                    w = corridorFit.first
                    usedContentCenterCorridor = true
                } else {
                    val clamped = clampCenterAnchoredToCorridor(h, w, corridorCellPx)
                    val filled = applyBitmapContentFill(
                        clamped.first,
                        clamped.second,
                        PacMazeIkunGameplayScale.bitmapContentFillMul(contentHeightFrac, contentWidthFrac),
                    )
                    val scaled = applyUniformVisualScale(filled.first, filled.second, visualScale)
                    h = scaled.first
                    w = scaled.second
                }
            } else {
                h *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
                w *= PacMazeIkunGameplayScale.LEGACY_BITMAP_GAMEPLAY_SCALE
            }
        }

        val travelAxis = travelFacing ?: facing
        val verticalTravel = travelAxis == Direction.UP || travelAxis == Direction.DOWN
        val horizontalTravel = travelAxis == Direction.LEFT || travelAxis == Direction.RIGHT
        val effectiveWalkBob = PacMazeBitmapCorridorDrawPolicy.effectiveWalkBob(walkBob, travelAxis)
        val useCorridorSymmetric = usesCorridorSymmetricFit(skinId, tallGameplay)
        val tallOrCenterAnchored = tallGameplay || centerAnchored
        val contentPivot = when {
            useCorridorSymmetric && skinId != null ->
                PacMazeBitmapWalkLayoutSizing.walkContentCenterPivot(skinId)
            useCorridorSymmetric ->
                resolveCorridorContentPivot(
                    image = null,
                    skinId = skinId,
                    contentHeightFrac = contentHeightFrac,
                    contentWidthFrac = contentWidthFrac,
                    feetFracY = feetFrac,
                    feetFracX = feetFracXLocal,
                )
            usedContentCenterCorridor ->
                PacMazeBitmapCorridorFit.pivotFromContentSpan(contentHeightFrac, contentWidthFrac)
            else -> feetFracXLocal to feetFrac
        }
        val useCorridorCenterPivot = !useCorridorSymmetric && tallOrCenterAnchored && !verticalTravel

        var pivotFracY = when {
            useCorridorSymmetric -> contentPivot.second
            usedContentCenterCorridor -> contentPivot.second
            useCorridorCenterPivot -> 0.5f
            verticalTravel && tallOrCenterAnchored -> 0.5f
            else -> feetFrac
        }
        var pivotFracX = when {
            useCorridorSymmetric -> contentPivot.first
            usedContentCenterCorridor -> contentPivot.first
            useCorridorCenterPivot -> 0.5f
            verticalTravel && tallOrCenterAnchored -> 0.5f
            else -> feetFracXLocal
        }

        pivotFracY = applyHorizontalSolePivot(skinId, horizontalTravel, pivotFracY)

        if (skinId != null && horizontalTravel && (centerAnchored || tallGameplay)) {
            val frozen = PacMazeBitmapStableLayoutCache.stabilize(
                skinId = skinId,
                height = h,
                width = w,
                pivotFracX = pivotFracX,
                pivotFracY = pivotFracY,
                layoutScale = PacMazeIkunGameplayScale.bitmapLayoutVisualScale(
                    PacMazeSkinRegistry.drawUserScale,
                ),
            )
            h = frozen.height
            w = frozen.width
            pivotFracX = frozen.pivotFracX
            pivotFracY = frozen.pivotFracY
        }

        val feet = resolveBitmapAnchor(
            center = center,
            walkBob = effectiveWalkBob,
            skinId = skinId,
            tallGameplay = tallGameplay,
            useCorridorSymmetric = useCorridorSymmetric,
            useContentCenterCorridor = usedContentCenterCorridor,
            useCorridorCenterPivot = useCorridorCenterPivot,
            horizontalTravel = horizontalTravel,
            verticalTravel = verticalTravel,
            tallOrCenterAnchored = tallOrCenterAnchored,
            h = h,
            pivotFracY = pivotFracY,
            image = null,
            feetFrac = feetFrac,
            tileCellPx = tileCellPx,
            tileBottomY = tileBottomY,
        )

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
