package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import java.util.concurrent.ConcurrentHashMap

/**
 * 按位图内容检测脚点（0=顶，1=底）。
 * 解码阶段在 IO 线程预注册；局内绘制走 O(1) 缓存，避免主线程像素扫描卡顿。
 */
internal object PacMazeBitmapFeetAnchor {

    private const val MAX_BITMAP_CACHE = 512

    private data class DimKey(val skinId: PacMazeSkinId, val w: Int, val h: Int)

    private data class Anchor(val yFrac: Float, val xFrac: Float)

    private val bitmapAnchorCache = ConcurrentHashMap<Long, Anchor>()
    private val skinDimAnchorCache = ConcurrentHashMap<DimKey, Anchor>()
    /** walk_1 / 首帧 walk 注册的局内默认脚点，动画各帧尺寸略变时仍 O(1)。 */
    private val skinGameplayDefault = ConcurrentHashMap<PacMazeSkinId, Anchor>()

    fun feetYFraction(image: ImageBitmap, skinId: PacMazeSkinId? = null): Float =
        feetAnchor(image, skinId).first

    fun feetAnchor(image: ImageBitmap, skinId: PacMazeSkinId? = null): Pair<Float, Float> {
        resolveAnchor(image, skinId)?.let { return it.yFrac to it.xFrac }

        val anchor = computeAnchor(image)
        trimCachesIfNeeded()
        bitmapAnchorCache[bitmapKey(image)] = anchor
        skinId?.let { storeSkinDim(it, image, anchor) }
        return anchor.yFrac to anchor.xFrac
    }

    /** 局内贴地：优先 skin 默认脚点，禁止在渲染线程做 detectFeet。 */
    fun gameplayFeetAnchor(image: ImageBitmap, skinId: PacMazeSkinId? = null): Pair<Float, Float> {
        val anchor = resolveAnchor(image, skinId) ?: computeAnchor(image)
        val biasedY = (anchor.yFrac - PacMazeIkunGameplayScale.FEET_FRAC_GAMEPLAY_BIAS)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        return biasedY to anchor.xFrac
    }

    /** IO / 资源加载线程：预计算脚点，供局内零卡顿绘制。 */
    fun registerGameplayAnchor(skinId: PacMazeSkinId, image: ImageBitmap, asDefault: Boolean = false) {
        val anchor = computeAnchor(image)
        storeSkinDim(skinId, image, anchor)
        if (asDefault || !skinGameplayDefault.containsKey(skinId)) {
            skinGameplayDefault[skinId] = anchor
        }
    }

    fun hasGameplayDefault(skinId: PacMazeSkinId): Boolean = skinGameplayDefault.containsKey(skinId)

    private fun resolveAnchor(image: ImageBitmap, skinId: PacMazeSkinId?): Anchor? {
        skinId?.let { id ->
            skinGameplayDefault[id]?.let { return it }
            skinDimAnchorCache[DimKey(id, image.width, image.height)]?.let { return it }
        }
        return bitmapAnchorCache[bitmapKey(image)]
    }

    private fun computeAnchor(image: ImageBitmap): Anchor = Anchor(
        yFrac = PacMazeBitmapContentTrim.detectFeetYFraction(image),
        xFrac = PacMazeBitmapContentTrim.detectFeetXFraction(image),
    )

    private fun storeSkinDim(skinId: PacMazeSkinId, image: ImageBitmap, anchor: Anchor) {
        skinDimAnchorCache[DimKey(skinId, image.width, image.height)] = anchor
    }

    private fun bitmapKey(image: ImageBitmap): Long =
        (image.width.toLong() shl 32) or (image.height.toLong() and 0xFFFF_FFFFL)

    private fun trimCachesIfNeeded() {
        if (bitmapAnchorCache.size >= MAX_BITMAP_CACHE) {
            bitmapAnchorCache.keys.take(MAX_BITMAP_CACHE / 4).forEach { bitmapAnchorCache.remove(it) }
        }
        if (skinDimAnchorCache.size >= MAX_BITMAP_CACHE) {
            skinDimAnchorCache.keys.take(MAX_BITMAP_CACHE / 4).forEach { skinDimAnchorCache.remove(it) }
        }
    }

    fun invalidateAll() {
        bitmapAnchorCache.clear()
        skinDimAnchorCache.clear()
        skinGameplayDefault.clear()
    }
}
