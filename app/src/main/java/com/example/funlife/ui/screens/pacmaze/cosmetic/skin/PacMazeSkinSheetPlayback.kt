package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect

/**
 * 运行时 Sprite Sheet 播放：整张 sheet 只 decode 一次，局内用 srcRect 切帧绘制。
 */
data class PacMazeSkinSheetPlayback(
    val bitmap: ImageBitmap,
    val columns: Int,
    val rows: Int,
    /** 解码后 bitmap 中每格宽高（已含 sampleSize） */
    val cellW: Int,
    val cellH: Int,
    val frameCount: Int,
    val sampleSize: Int,
) {
    fun srcRect(frameIndex: Int): IntRect {
        val index = frameIndex.coerceIn(0, (frameCount - 1).coerceAtLeast(0))
        val col = index % columns.coerceAtLeast(1)
        val row = index / columns.coerceAtLeast(1)
        val left = col * cellW
        val top = row * cellH
        return IntRect(left, top, left + cellW, top + cellH)
    }
}
