package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import java.util.concurrent.ConcurrentHashMap

/**
 * 局内位图皮肤：首帧冻结 h/w/pivot，避免 walk 序列帧 opaque bbox 略差导致布局 Y 逐帧跳变。
 * 按 HUD 滑条分桶，避免调节尺寸不生效。
 */
internal object PacMazeBitmapStableLayoutCache {

    data class FrozenMetrics(
        val height: Float,
        val width: Float,
        val pivotFracX: Float,
        val pivotFracY: Float,
    )

    private val byKey = ConcurrentHashMap<String, FrozenMetrics>()

    private fun cacheKey(skinId: PacMazeSkinId, layoutScale: Float): String =
        "${skinId.storageKey}:s${"%.3f".format(layoutScale.coerceIn(0.25f, 4f))}"

    fun stabilize(
        skinId: PacMazeSkinId,
        height: Float,
        width: Float,
        pivotFracX: Float,
        pivotFracY: Float,
        layoutScale: Float = PacMazeSkinRegistry.drawUserScale,
    ): FrozenMetrics = byKey.getOrPut(cacheKey(skinId, layoutScale)) {
        FrozenMetrics(height, width, pivotFracX, pivotFracY)
    }

    fun clear() {
        byKey.clear()
    }
}
