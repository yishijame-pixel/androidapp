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
    /** 全帧扫描到的最深脚点（横走 sole 对齐参考）。 */
    private val skinCycleMaxFeet = ConcurrentHashMap<PacMazeSkinId, Float>()
    /** 全帧扫描到的最大 opaque 高度（横走高度归一化参考）。 */
    private val skinCycleMaxOpaqueH = ConcurrentHashMap<PacMazeSkinId, Float>()
    private val frameOpaqueHeightCache = ConcurrentHashMap<ImageBitmap, Float>()

    private val frameAnchorCache = ConcurrentHashMap<ImageBitmap, Anchor>()
    /** 横版专用脚点缓存（bbox 底边），与走廊 [frameAnchorCache] 分离避免误用偏上的 contact 行。 */
    private val platformerDimAnchorCache = ConcurrentHashMap<DimKey, Anchor>()
    /** 横版按帧脚点：jump 各帧 bbox 底边可差 15%+，不能只用尺寸维度的单一锚点。 */
    private val platformerFrameAnchorCache = ConcurrentHashMap<ImageBitmap, Anchor>()
    private val platformerHeadTopCache = ConcurrentHashMap<ImageBitmap, Float>()
    /** 已把 manifest/带宽 fy 与 bbox 鞋底合并，避免重复扫像素。 */
    private val platformerSoleMerged = ConcurrentHashMap<ImageBitmap, Boolean>()

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
        return biasedGameplayAnchor(anchor)
    }

    /** 横版等平台：整段动画使用 walk_1 注册的稳定脚点，避免待机帧尺寸变化导致位移。 */
    fun gameplayFeetAnchorForSkin(skinId: PacMazeSkinId): Pair<Float, Float> {
        val anchor = skinGameplayDefault[skinId]
            ?: skinDimAnchorCache.values.firstOrNull()
            ?: Anchor(0.92f, 0.5f)
        return biasedGameplayAnchor(anchor)
    }

    private fun biasedGameplayAnchor(anchor: Anchor): Pair<Float, Float> {
        val biasedY = (anchor.yFrac - PacMazeIkunGameplayScale.FEET_FRAC_GAMEPLAY_BIAS)
            .coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        return biasedY to anchor.xFrac.coerceIn(0.12f, 0.88f)
    }

    /** 横版局内按帧脚点；fy 取 max(manifest, bbox 底) 对齐真实鞋底。 */
    fun platformerFeetAnchor(image: ImageBitmap, skinId: PacMazeSkinId): Pair<Float, Float> {
        if (platformerSoleMerged[image] == true) {
            val cached = platformerFrameAnchorCache[image] ?: computePlatformerAnchor(image)
            val y = cached.yFrac.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
            return y to cached.xFrac.coerceIn(0.12f, 0.88f)
        }
        val bbox = computePlatformerAnchor(image)
        val merged = platformerFrameAnchorCache[image]?.let { cached ->
            Anchor(
                yFrac = maxOf(cached.yFrac, bbox.yFrac),
                xFrac = cached.xFrac,
            )
        } ?: bbox
        platformerFrameAnchorCache[image] = merged
        platformerDimAnchorCache[DimKey(skinId, image.width, image.height)] = merged
        platformerSoleMerged[image] = true
        val y = merged.yFrac.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f)
        return y to merged.xFrac.coerceIn(0.12f, 0.88f)
    }

    /** 横版：头顶不透明边（0=顶）；manifest 预写入时 O(1)。 */
    fun platformerHeadTopFraction(image: ImageBitmap, skinId: PacMazeSkinId): Float {
        platformerHeadTopCache[image]?.let { return it }
        return PacMazeBitmapContentTrim.detectContentTopFraction(image).also {
            platformerHeadTopCache[image] = it
        }
    }

    /**
     * IO / 读盘 hydration：优先 manifest [platformerMetrics]，避免 122 帧 × 全图扫像素。
     */
    fun registerPlatformerFrameMetrics(
        skinId: PacMazeSkinId,
        image: ImageBitmap,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
    ) {
        PacMazeSkinAnimManifest.platformerFrameMetrics(skinId, clip, frameIndex)?.let { metrics ->
            val bbox = computePlatformerAnchor(image)
            val soleY = maxOf(metrics.feetY, bbox.yFrac)
            platformerFrameAnchorCache[image] = Anchor(soleY, metrics.feetX)
            platformerHeadTopCache[image] = metrics.headTopY
            platformerSoleMerged[image] = true
            return
        }
        // manifest 无 metrics（旧包 v15）：读盘阶段不扫像素，局内首帧绘制时再 lazy 检测。
    }

    /** IO / 横版预热：预注册 bbox 脚点，局内 O(1)。 */
    fun registerPlatformerFrameAnchor(skinId: PacMazeSkinId, image: ImageBitmap) {
        val anchor = computePlatformerAnchor(image)
        platformerFrameAnchorCache[image] = anchor
        platformerDimAnchorCache[DimKey(skinId, image.width, image.height)] = anchor
    }

    /** IO / 资源加载线程：预计算脚点，供局内零卡顿绘制。 */
    fun registerGameplayAnchor(skinId: PacMazeSkinId, image: ImageBitmap, asDefault: Boolean = false) {
        val anchor = computeAnchor(image)
        val opaqueH = PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image).heightFrac
        frameAnchorCache[image] = anchor
        frameOpaqueHeightCache[image] = opaqueH
        storeSkinDim(skinId, image, anchor)
        if (asDefault || !skinGameplayDefault.containsKey(skinId)) {
            skinGameplayDefault[skinId] = anchor
        }
        skinCycleMaxFeet[skinId] = maxOf(skinCycleMaxFeet[skinId] ?: anchor.yFrac, anchor.yFrac)
        skinCycleMaxOpaqueH[skinId] = maxOf(skinCycleMaxOpaqueH[skinId] ?: opaqueH, opaqueH)
        logFrameMismatchIfNeeded(skinId, image, anchor.yFrac, opaqueH)
    }

    /** 解码每一帧时注册，供横版按帧脚点绘制。 */
    fun registerFrameAnchor(skinId: PacMazeSkinId, image: ImageBitmap) {
        registerGameplayAnchor(skinId, image, asDefault = false)
    }

    fun cycleMaxFeetYForSkin(skinId: PacMazeSkinId): Float? =
        skinCycleMaxFeet[skinId] ?: skinGameplayDefault[skinId]?.yFrac

    fun cycleMaxOpaqueHeightForSkin(skinId: PacMazeSkinId): Float? = skinCycleMaxOpaqueH[skinId]

    fun opaqueHeightFrac(image: ImageBitmap, skinId: PacMazeSkinId? = null): Float {
        frameOpaqueHeightCache[image]?.let { return it }
        return PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image).heightFrac.also {
            frameOpaqueHeightCache[image] = it
        }
    }

    private fun logFrameMismatchIfNeeded(
        skinId: PacMazeSkinId,
        image: ImageBitmap,
        feetY: Float,
        opaqueH: Float,
    ) {
        if (!com.example.funlife.BuildConfig.DEBUG) return
        val refFeet = skinCycleMaxFeet[skinId] ?: return
        val refOpaque = skinCycleMaxOpaqueH[skinId] ?: return
        val feetGap = kotlin.math.abs(refFeet - feetY)
        val opaqueGap = kotlin.math.abs(refOpaque - opaqueH)
        if (feetGap > 0.012f || opaqueGap > 0.025f) {
            android.util.Log.i(
                "PacMazeMotionDiag",
                "SPRITE_FRAME_MISMATCH skin=${skinId.name} size=${image.width}x${image.height} " +
                    "feetY=${"%.3f".format(feetY)} refFeet=${"%.3f".format(refFeet)} " +
                    "opaqueH=${"%.3f".format(opaqueH)} refOpaqueH=${"%.3f".format(refOpaque)}",
            )
        }
    }

    /** 横走 sole+高度对齐：用 raw fy（与 [PacMazeSheetCellFeetCache] / 像素扫描一致，不含 gameplay bias）。 */
    fun rawCycleMaxFeetY(skinId: PacMazeSkinId): Float =
        cycleMaxFeetYForSkin(skinId) ?: skinGameplayDefault[skinId]?.yFrac ?: 0.92f

    fun rawFrameFeetY(image: ImageBitmap, skinId: PacMazeSkinId): Float =
        frameAnchorCache[image]?.yFrac
            ?: skinDimAnchorCache[DimKey(skinId, image.width, image.height)]?.yFrac
            ?: rawCycleMaxFeetY(skinId)

    /** sole 对齐参考：全周期最深脚点（与 layout 稳定 pivot 一致，含 gameplay bias）。 */
    fun soleAlignReferenceFeetY(skinId: PacMazeSkinId): Float {
        val raw = cycleMaxFeetYForSkin(skinId) ?: skinGameplayDefault[skinId]?.yFrac ?: 0.92f
        return biasedGameplayAnchor(Anchor(raw, 0.5f)).first
    }

    /**
     * sole 对齐按帧脚点：必须绕过 [skinGameplayDefault]，否则各帧 fy 相同 → offset 恒为 0。
     */
    fun soleAlignFrameFeetY(image: ImageBitmap, skinId: PacMazeSkinId): Float {
        val raw = frameAnchorCache[image]?.yFrac
            ?: skinDimAnchorCache[DimKey(skinId, image.width, image.height)]?.yFrac
            ?: cycleMaxFeetYForSkin(skinId)
            ?: 0.92f
        return biasedGameplayAnchor(Anchor(raw, 0.5f)).first
    }

    fun hasGameplayDefault(skinId: PacMazeSkinId): Boolean = skinGameplayDefault.containsKey(skinId)

    /** manifest 驱动：build 阶段已统一脚点，运行时不再扫描像素。 */
    fun registerManifestAnchor(skinId: PacMazeSkinId, anchorFrac: PacMazeSkinAnimManifest.AnchorFrac) {
        val anchor = Anchor(
            yFrac = anchorFrac.y.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f),
            xFrac = anchorFrac.x.coerceIn(0.12f, 0.88f),
        )
        skinGameplayDefault[skinId] = anchor
        skinCycleMaxFeet[skinId] = anchor.yFrac
    }

    /** 归一化画布：所有帧同尺寸同脚点，直接返回 manifest 锚点。 */
    fun normalizedFeetAnchor(skinId: PacMazeSkinId): Pair<Float, Float>? {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId) ?: return null
        if (!manifest.normalized) return null
        val frac = manifest.anchorFrac ?: return null
        return frac.y.coerceIn(PacMazeIkunGameplayScale.FEET_Y_FRAC_MIN, 0.999f) to
            frac.x.coerceIn(0.12f, 0.88f)
    }

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

    private fun computePlatformerAnchor(image: ImageBitmap): Anchor = Anchor(
        yFrac = PacMazeBitmapContentTrim.detectPlatformerFeetYFraction(image),
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
        skinCycleMaxFeet.clear()
        skinCycleMaxOpaqueH.clear()
        frameOpaqueHeightCache.clear()
        frameAnchorCache.clear()
        platformerDimAnchorCache.clear()
        platformerFrameAnchorCache.clear()
        platformerHeadTopCache.clear()
        platformerSoleMerged.clear()
    }
}
