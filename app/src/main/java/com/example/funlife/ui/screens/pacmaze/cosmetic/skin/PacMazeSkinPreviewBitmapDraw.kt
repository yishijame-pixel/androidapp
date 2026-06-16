package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId

/**
 * 选角/Hub 缩略图：位图完整 fit 进容器，不用局内 [diameterMul] 原尺寸。
 */
internal object PacMazeSkinPreviewBitmapDraw {

    enum class Slot {
        /** 32–36dp 当前角色 chip */
        Chip,
        /** 皮肤网格格 */
        GridCell,
        /** 详情页大预览 */
        Stage,
    }

    fun shouldUse(skinId: PacMazeSkinId): Boolean =
        PacMazeSkinRenderProfileCatalog.isBitmapResource(skinId) ||
            PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)

    fun drawFit(
        scope: DrawScope,
        skinId: PacMazeSkinId,
        boxWidth: Float,
        boxHeight: Float,
        center: Offset,
        facing: Direction,
        pose: PacMazeCharacterPose,
        slot: Slot = Slot.GridCell,
    ) {
        val image = resolveImage(skinId, pose) ?: return
        val padding = when (slot) {
            Slot.Chip -> 0.06f
            Slot.GridCell -> 0.10f
            Slot.Stage -> 0.08f
        }
        val maxW = boxWidth * (1f - padding * 2f)
        val maxH = boxHeight * (1f - padding * 2f)
        val aspect = image.width.toFloat() / image.height.coerceAtLeast(1)
        var h = maxH
        var w = h * aspect
        if (w > maxW) {
            w = maxW
            h = w / aspect
        }
        val layout = PacMazeSkinLayoutEngine.Layout(
            width = w,
            height = h,
            topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
            feetCenter = center,
            feetFrac = 0.5f,
            feetFracX = 0.5f,
        )
        val profile = PacMazeSkinRenderProfileCatalog.profile(skinId)
        scope.drawOrientedBitmap(image, layout, facing, profile, skinId)
    }

    fun centerYFrac(slot: Slot): Float = when (slot) {
        Slot.Chip -> 0.52f
        Slot.GridCell -> 0.54f
        Slot.Stage -> 0.52f
    }

    private fun resolveImage(skinId: PacMazeSkinId, pose: PacMazeCharacterPose): ImageBitmap? {
        if (PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) {
            if (pose.preferCoverOnly) {
                return PacMazeRemoteSkinAnimCache.cover(skinId)
            }
            val clip = PacMazeRemoteSkinAnimCatalog.pickClip(skinId, pose)
            PacMazeRemoteSkinAnimCache.frames(skinId, clip)?.takeIf { it.isNotEmpty() }?.let { frames ->
                val index = PacMazeRemoteSkinAnimCatalog.frameIndex(skinId, pose, clip, frames.size)
                return frames.getOrElse(index) { frames.first() }
            }
            return PacMazeRemoteSkinAnimCache.cover(skinId)
        }
        val sheet = PacMazeSkinAssetCache.spriteSheet(skinId)
        if (sheet != null) {
            if (!pose.isMoving || sheet.frames.isEmpty()) return sheet.frames.firstOrNull()
            val index = PacMazeSpriteWalkAnim.frameIndex(pose, sheet.frames.size)
            return sheet.frames[index]
        }
        return PacMazeSkinAssetCache.bitmap(skinId)
    }

    private fun DrawScope.drawOrientedBitmap(
        image: ImageBitmap,
        layout: PacMazeSkinLayoutEngine.Layout,
        facing: Direction,
        profile: PacMazeSkinRenderProfile?,
        skinId: PacMazeSkinId,
    ) {
        PacMazeSkinTransform.run {
            drawOrientedBitmap(image, layout, facing, profile, skinId)
        }
    }
}
