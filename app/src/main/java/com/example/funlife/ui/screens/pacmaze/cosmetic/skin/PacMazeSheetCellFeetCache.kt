package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import java.util.concurrent.ConcurrentHashMap

/**
 * Sprite sheet 每格脚底 fy（相对格高），decode 后在 IO 线程扫一次，局内 O(1)。
 * manifest platformerMetrics 在 walk 部分帧 fy 跳变过大，不能用于步态对齐。
 */
internal object PacMazeSheetCellFeetCache {

    internal data class ContentSpan(
        val minHeightFrac: Float,
        val minWidthFrac: Float,
    )

    private val cellFeetBySheet = ConcurrentHashMap<String, FloatArray>()
    private val contentSpanBySheet = ConcurrentHashMap<String, ContentSpan>()
    private val frameOpaqueHeightBySheet = ConcurrentHashMap<String, FloatArray>()

    private fun cacheKey(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip, sheet: PacMazeSkinSheetPlayback): String =
        "${skinId.storageKey}:${clip.name}:${sheet.bitmap.width}x${sheet.bitmap.height}:s${sheet.sampleSize}"

    fun precompute(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip, sheet: PacMazeSkinSheetPlayback) {
        val key = cacheKey(skinId, clip, sheet)
        if (cellFeetBySheet.containsKey(key) &&
            contentSpanBySheet.containsKey(key) &&
            frameOpaqueHeightBySheet.containsKey(key)
        ) {
            return
        }
        val bmp = sheet.bitmap.asAndroidBitmap()
        val frameCount = sheet.frameCount.coerceAtLeast(1)
        val feet = FloatArray(frameCount) { frameIndex ->
            val rect = sheet.srcRect(frameIndex)
            PacMazeBitmapContentTrim.detectPlatformerFeetYFractionInRect(
                source = bmp,
                left = rect.left,
                top = rect.top,
                width = rect.width,
                height = rect.height,
            )
        }
        repairFeetScanOutliers(feet)

        var minHeightFrac = 1f
        var minWidthFrac = 1f
        val frameHeights = FloatArray(frameCount)
        for (frameIndex in 0 until frameCount) {
            val rect = sheet.srcRect(frameIndex)
            val span = PacMazeBitmapContentTrim.detectOpaqueContentSpanInRect(
                source = bmp,
                left = rect.left,
                top = rect.top,
                width = rect.width,
                height = rect.height,
            )
            frameHeights[frameIndex] = span.heightFrac
            minHeightFrac = minOf(minHeightFrac, span.heightFrac)
            minWidthFrac = minOf(minWidthFrac, span.widthFrac)
        }

        cellFeetBySheet[key] = feet
        contentSpanBySheet[key] = ContentSpan(minHeightFrac, minWidthFrac)
        frameOpaqueHeightBySheet[key] = frameHeights
    }

    /** 局内首帧绘制前确保 metrics 可用（升级/热更后旧 sheet 缓存可能缺 contentSpan）。 */
    fun ensureMetrics(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip, sheet: PacMazeSkinSheetPlayback) {
        val key = cacheKey(skinId, clip, sheet)
        if (contentSpanBySheet.containsKey(key) && frameOpaqueHeightBySheet.containsKey(key)) return
        precompute(skinId, clip, sheet)
    }

    /** 扫描值比 cycleMax 低过多时视为噪点（如 53–55 格），拉回到最深脚点。 */
    private const val FEET_SCAN_OUTLIER_GAP = 0.045f

    private fun repairFeetScanOutliers(feet: FloatArray) {
        val maxFeet = feet.maxOrNull() ?: return
        for (i in feet.indices) {
            if (maxFeet - feet[i] > FEET_SCAN_OUTLIER_GAP) {
                feet[i] = maxFeet
            }
        }
    }

    fun cellFeetY(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
    ): Float? {
        val key = cacheKey(skinId, clip, sheet)
        val arr = cellFeetBySheet[key] ?: return null
        return arr.getOrNull(frameIndex.coerceIn(0, arr.size - 1))
    }

    /** clip 全周期最深脚底（布局锚点参考）。 */
    fun cycleMaxFeetY(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): Float? {
        val key = cacheKey(skinId, clip, sheet)
        return cellFeetBySheet[key]?.maxOrNull()
    }

    /** clip 全周期最大 opaque 高度（高度归一化参考）。 */
    fun cycleMaxOpaqueHeightFrac(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): Float? {
        val key = cacheKey(skinId, clip, sheet)
        return frameOpaqueHeightBySheet[key]?.maxOrNull()
    }

    /** walk 全周期最深脚底（布局锚点参考）。 */
    fun walkCycleMaxFeetY(skinId: PacMazeSkinId, sheet: PacMazeSkinSheetPlayback): Float? =
        cycleMaxFeetY(skinId, PacMazeSkinAnimClip.WALK, sheet)

    fun contentFillMul(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): Float {
        val span = contentSpan(skinId, clip, sheet)
        return PacMazeIkunGameplayScale.bitmapContentFillMul(span.minHeightFrac, span.minWidthFrac)
    }

    fun contentSpan(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): ContentSpan {
        val key = cacheKey(skinId, clip, sheet)
        contentSpanBySheet[key]?.let { return it }
        ensureMetrics(skinId, clip, sheet)
        contentSpanBySheet[key]?.let { return it }
        return manifestContentSpan(skinId, clip)
    }

    /** 局内布局/contentFill：manifest 与扫描取更小不透明占比，避免扫描误判→角色越来越小。 */
    fun contentSpanForLayout(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
    ): ContentSpan {
        val manifest = manifestContentSpan(skinId, clip)
        val key = cacheKey(skinId, clip, sheet)
        val scanned = contentSpanBySheet[key] ?: run {
            ensureMetrics(skinId, clip, sheet)
            contentSpanBySheet[key]
        }
        if (scanned == null) return manifest
        return ContentSpan(
            minHeightFrac = minOf(manifest.minHeightFrac, scanned.minHeightFrac)
                .coerceIn(
                    PacMazeIkunGameplayScale.BITMAP_CONTENT_FILL_MIN_FRAC,
                    PacMazeIkunGameplayScale.BITMAP_LAYOUT_OPAQUE_HEIGHT_CAP,
                ),
            minWidthFrac = scanned.minWidthFrac
                .coerceIn(PacMazeIkunGameplayScale.BITMAP_CONTENT_FILL_MIN_FRAC, 1f),
        )
    }

    fun contentSpan(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
        forLayout: Boolean,
    ): ContentSpan = if (forLayout) contentSpanForLayout(skinId, clip, sheet) else contentSpan(skinId, clip, sheet)

    fun frameOpaqueHeightFrac(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
    ): Float {
        val key = cacheKey(skinId, clip, sheet)
        frameOpaqueHeightBySheet[key]?.let { arr ->
            return arr.getOrNull(frameIndex.coerceIn(0, arr.size - 1)) ?: arr.first()
        }
        ensureMetrics(skinId, clip, sheet)
        frameOpaqueHeightBySheet[key]?.let { arr ->
            return arr.getOrNull(frameIndex.coerceIn(0, arr.size - 1)) ?: arr.first()
        }
        return manifestContentSpan(skinId, clip).minHeightFrac
    }

    fun manifestContentSpanForLayout(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): ContentSpan =
        manifestContentSpan(skinId, clip)

    private fun manifestContentSpan(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): ContentSpan {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId)
        val manifest = config?.let { PacMazeSkinAnimManifest.load(it.assetRoot) }
        manifest?.minOpaqueContentSpanFrac(clip)?.let { (hFrac, wFrac) ->
            return ContentSpan(hFrac, wFrac)
        }
        if (manifest?.normalized == true || PacMazeSkinAnimManifest.isNormalized(config?.assetRoot.orEmpty())) {
            val inv = 1f / PacMazeIkunGameplayScale.BITMAP_NORMALIZED_SHEET_FALLBACK_FILL_MUL
            return ContentSpan(inv, inv)
        }
        return ContentSpan(0.62f, 0.55f)
    }

    private fun manifestContentFillMul(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Float {
        val span = manifestContentSpan(skinId, clip)
        return PacMazeIkunGameplayScale.bitmapContentFillMul(span.minHeightFrac, span.minWidthFrac)
    }

    fun invalidateAll() {
        cellFeetBySheet.clear()
        contentSpanBySheet.clear()
        frameOpaqueHeightBySheet.clear()
    }
}
