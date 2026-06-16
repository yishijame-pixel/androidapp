package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose

/**
 * 序列帧行走：逐步切换 0→1→2→3，每帧持帧时间固定，避免浮点跳帧或只播 1～2 帧。
 */
internal object PacMazeSpriteWalkAnim {

    const val FRAME_COUNT = 4

    /** 预览：每帧持帧 animPhase 量（4 帧 × 0.95 ≈ 3.8 周期） */
    private const val PREVIEW_PHASE_PER_FRAME = 0.95f

    /** 局内：约 10 tick/帧（60tps → ~167ms/帧，整圈 ~670ms） */
    private const val GAMEPLAY_PHASE_PER_FRAME = 0.80f

    fun frameIndex(pose: PacMazeCharacterPose, frameCount: Int): Int {
        if (frameCount <= 0) return 0
        pose.spriteFrameOverride?.let { return it.mod(frameCount) }
        if (!pose.isMoving) return 0
        val perFrame = if (pose.walkPreview) PREVIEW_PHASE_PER_FRAME else GAMEPLAY_PHASE_PER_FRAME
        return ((pose.animPhase / perFrame).toInt()).mod(frameCount)
    }

    fun frameProgress(pose: PacMazeCharacterPose, frameCount: Int): Float {
        if (frameCount <= 0) return 0f
        pose.spriteFrameOverride?.let { return it.toFloat() }
        if (!pose.isMoving) return 0f
        val perFrame = if (pose.walkPreview) PREVIEW_PHASE_PER_FRAME else GAMEPLAY_PHASE_PER_FRAME
        val raw = pose.animPhase / perFrame
        val index = raw.toInt().mod(frameCount)
        val blend = raw - raw.toInt()
        return index + blend
    }

    fun walkBob(radius: Float, pose: PacMazeCharacterPose, frameCount: Int): Float {
        if (!pose.isMoving) {
            return kotlin.math.sin(pose.animPhase * 0.85f) * radius * 0.008f
        }
        val progress = frameProgress(pose, frameCount)
        val index = progress.toInt().coerceIn(0, frameCount - 1)
        val blend = progress - index
        val next = (index + 1) % frameCount
        val bobFor = { frame: Int ->
            when (frame.mod(4)) {
                0, 2 -> -radius * 0.016f
                else -> radius * 0.045f
            }
        }
        val from = bobFor(index)
        val to = bobFor(next)
        return from + (to - from) * blend
    }
}
