package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction

/**
 * 位图皮肤在 Pac-Maze 走廊内绘制时的共用策略：
 * 横走时锁 Y、禁 walkBob，避免所有图片资源角色换帧/插值时上下抖。
 */
internal object PacMazeBitmapCorridorDrawPolicy {

    fun isHorizontalTravel(facing: Direction?): Boolean =
        facing == Direction.LEFT || facing == Direction.RIGHT

    /** 局内已注入过道中心时，横走不应再叠加 walkBob 或 per-frame 脚点偏移。 */
    fun shouldSuppressWalkBob(travelFacing: Direction? = PacMazeSkinRegistry.drawTravelFacing): Boolean {
        if (!isHorizontalTravel(travelFacing)) return false
        return PacMazeSkinRegistry.drawCorridorCenterPx != null ||
            PacMazeSkinRegistry.drawFeetAnchorPx != null
    }

    /** 横走且锁 Y：用固定步态帧，避免 walk 各帧 opaque 高度差造成「头/身上下跳」。 */
    fun stableHorizontalWalkFrameIndex(
        pose: com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose,
        frameCount: Int,
    ): Int = resolveWalkFrameIndex(
        pose = pose,
        frameCount = frameCount,
        animatedIndex = PacMazeSpriteWalkAnim.frameIndex(pose, frameCount),
    )

    /** 横走锁 Y 时仍播 walk 动画；[PacMazeBitmapFrameAlign] 做脚底+高度对齐。 */
    fun resolveWalkFrameIndex(
        pose: com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose,
        frameCount: Int,
        animatedIndex: Int,
    ): Int {
        if (frameCount <= 1) return 0
        pose.spriteFrameOverride?.let { return it.mod(frameCount) }
        return animatedIndex.coerceIn(0, frameCount - 1)
    }

    fun effectiveWalkBob(proposedBob: Float, travelFacing: Direction? = PacMazeSkinRegistry.drawTravelFacing): Float =
        if (shouldSuppressWalkBob(travelFacing)) 0f else proposedBob

    /** 横走且有过道中心：布局锚点 Y 固定为通道中心，不跟 center+walkBob 漂。 */
    fun corridorAnchorOrCenter(
        center: androidx.compose.ui.geometry.Offset,
        walkBob: Float,
        horizontalTravel: Boolean,
    ): androidx.compose.ui.geometry.Offset {
        val cc = PacMazeSkinRegistry.drawCorridorCenterPx
        if (horizontalTravel && cc != null) {
            return cc
        }
        val feet = PacMazeSkinRegistry.drawFeetAnchorPx
        if (horizontalTravel && feet != null) {
            return androidx.compose.ui.geometry.Offset(feet.x, feet.y + walkBob)
        }
        return androidx.compose.ui.geometry.Offset(center.x, center.y + walkBob)
    }
}
