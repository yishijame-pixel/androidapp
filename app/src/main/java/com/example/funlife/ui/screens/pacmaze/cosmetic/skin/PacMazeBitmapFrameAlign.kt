package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/**
 * 商业级 Sprite 对齐（运行时补偿 AI/Trim 导致的帧间差异）：
 *
 * 1. [soleOffsetY] — 各帧脚底 fy 对齐到周期最深脚点（基线一致）
 * 2. [scaleY] — 各帧 opaque 高度归一化到周期最大高度（绕脚点缩放，消除 1~2px 头/身跳）
 *
 * 对应资源规范：同尺寸 canvas + 统一 Bottom-Center pivot + 脚底同一水平线。
 */
internal object PacMazeBitmapFrameAlign {

    data class Result(
        val soleOffsetY: Float = 0f,
        val scaleY: Float = 1f,
    )

    private const val MIN_SCALE = 0.88f
    private const val MAX_SCALE = 1.12f

    fun forSheet(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
        layoutHeight: Float,
    ): Result {
        if (!PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob()) return Result()
        manifestAlign(skinId, clip, frameIndex, layoutHeight)?.let { return it }
        PacMazeSheetCellFeetCache.ensureMetrics(skinId, clip, sheet)
        val refFeet = PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, clip, sheet) ?: return Result()
        val frameFeet = PacMazeSheetCellFeetCache.cellFeetY(skinId, clip, sheet, frameIndex) ?: refFeet
        val refOpaqueH = PacMazeSheetCellFeetCache.cycleMaxOpaqueHeightFrac(skinId, clip, sheet)
            ?: return soleOnly(refFeet, frameFeet, layoutHeight)
        val frameOpaqueH = PacMazeSheetCellFeetCache.frameOpaqueHeightFrac(skinId, clip, sheet, frameIndex)
        return compose(refFeet, frameFeet, refOpaqueH, frameOpaqueH, layoutHeight)
    }

    /** build 阶段 platformerMetrics：比 webp 像素扫描更准（尤其 normalized sheet）。 */
    private fun manifestAlign(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
        layoutHeight: Float,
    ): Result? {
        val metrics = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, clip) ?: return null
        if (metrics.isEmpty()) return null
        val frame = metrics.getOrNull(frameIndex.coerceIn(0, metrics.lastIndex)) ?: return null
        val refFeet = metrics.maxOf { it.feetY }
        val refOpaqueH = metrics.maxOf { (it.feetY - it.headTopY).coerceAtLeast(0.08f) }
        val frameOpaqueH = (frame.feetY - frame.headTopY).coerceAtLeast(0.08f)
        return compose(refFeet, frame.feetY, refOpaqueH, frameOpaqueH, layoutHeight)
    }

    fun forImage(
        skinId: PacMazeSkinId,
        image: ImageBitmap,
        layoutHeight: Float,
    ): Result {
        if (!PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob()) return Result()
        val refFeet = PacMazeBitmapFeetAnchor.rawCycleMaxFeetY(skinId)
        val frameFeet = PacMazeBitmapFeetAnchor.rawFrameFeetY(image, skinId)
        val refOpaqueH = PacMazeBitmapFeetAnchor.cycleMaxOpaqueHeightForSkin(skinId)
            ?: PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image).heightFrac
        val frameOpaqueH = PacMazeBitmapFeetAnchor.opaqueHeightFrac(image, skinId)
        return compose(refFeet, frameFeet, refOpaqueH, frameOpaqueH, layoutHeight)
    }

    private fun compose(
        refFeet: Float,
        frameFeet: Float,
        refOpaqueH: Float,
        frameOpaqueH: Float,
        layoutHeight: Float,
    ): Result {
        val scaleY = opaqueHeightScale(refOpaqueH, frameOpaqueH)
        val effectiveH = layoutHeight * scaleY
        val soleOffsetY = (refFeet - frameFeet) * effectiveH
        return Result(soleOffsetY = soleOffsetY, scaleY = scaleY)
    }

    private fun soleOnly(refFeet: Float, frameFeet: Float, layoutHeight: Float): Result =
        Result(soleOffsetY = (refFeet - frameFeet) * layoutHeight, scaleY = 1f)

    /** 周期最大 opaque 高度为 1.0；矮帧放大、高帧缩小，均绕脚点。 */
    fun opaqueHeightScale(referenceOpaqueH: Float, frameOpaqueH: Float): Float {
        if (referenceOpaqueH <= 0.01f || frameOpaqueH <= 0.01f) return 1f
        return (referenceOpaqueH / frameOpaqueH).coerceIn(MIN_SCALE, MAX_SCALE)
    }
}
