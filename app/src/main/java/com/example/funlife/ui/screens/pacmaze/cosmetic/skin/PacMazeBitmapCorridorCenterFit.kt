package com.example.funlife.ui.screens.pacmaze.cosmetic.skin



import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Rect

import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeIkunGameplayScale

import kotlin.math.max

import kotlin.math.min



/**

 * 图片资源角色走廊定标（唯一尺寸源）：

 * - 不透明 bbox 竖直方向**贴满**可走道，中心 = 过道中心 → 上下半身对称

 * - 定标只看**不透明边**，透明留白不参与缩小（防归一化 sheet 变小）

 * - 全图稳态尺寸；超出墙体的透明边/鞋底由绘制 clip 软裁，不再按格缩小 sprite

 */

internal object PacMazeBitmapCorridorCenterFit {



    data class ContentBoundsFrac(

        val topFrac: Float,

        val bottomFrac: Float,

        val leftFrac: Float,

        val rightFrac: Float,

    ) {

        val centerX: Float get() = (leftFrac + rightFrac) * 0.5f

        val centerY: Float get() = (topFrac + bottomFrac) * 0.5f

        val opaqueHeightFrac: Float get() = (bottomFrac - topFrac).coerceAtLeast(MIN_EXTENT)

        val opaqueWidthFrac: Float get() = (rightFrac - leftFrac).coerceAtLeast(MIN_EXTENT)

        val aboveCenter: Float get() = (centerY - topFrac).coerceAtLeast(MIN_EXTENT)

        val belowCenter: Float get() = (bottomFrac - centerY).coerceAtLeast(MIN_EXTENT)

        val leftOfCenter: Float get() = (centerX - leftFrac).coerceAtLeast(MIN_EXTENT)

        val rightOfCenter: Float get() = (rightFrac - centerX).coerceAtLeast(MIN_EXTENT)



        fun pivot(): Pair<Float, Float> = centerX to centerY

    }



    data class FitResult(

        val width: Float,

        val height: Float,

        val pivotFracX: Float,

        val pivotFracY: Float,

    )



    private const val MIN_EXTENT = 0.05f



    fun fromOpaqueSpan(span: PacMazeBitmapContentTrim.OpaqueContentSpan): ContentBoundsFrac =

        ContentBoundsFrac(span.topFrac, span.bottomFrac, span.leftFrac, span.rightFrac)



    fun fromLayoutSpan(minHeightFrac: Float, minWidthFrac: Float): ContentBoundsFrac {

        val h = minHeightFrac.coerceIn(

            PacMazeIkunGameplayScale.BITMAP_CONTENT_FILL_MIN_FRAC,

            1f,

        )

        val w = minWidthFrac.coerceIn(

            PacMazeIkunGameplayScale.BITMAP_CONTENT_FILL_MIN_FRAC,

            1f,

        )

        val top = (1f - h) * 0.5f

        val left = (1f - w) * 0.5f

        return ContentBoundsFrac(

            topFrac = top,

            bottomFrac = top + h,

            leftFrac = left,

            rightFrac = left + w,

        )

    }



    fun fromPlatformerMetrics(

        metrics: List<PacMazeSkinAnimManifest.PlatformerFrameMetrics>,

        normalized: Boolean,

    ): ContentBoundsFrac {

        val stable = filterStableWalkMetrics(metrics)

        if (stable.any { it.opaqueWidthFrac != null || it.opaqueHeightFrac != null }) {

            val minHeight = stable.minOf { m ->

                m.opaqueHeightFrac ?: (m.feetY - m.headTopY).coerceAtLeast(MIN_EXTENT)

            }

            val minWidth = stable.minOf { m ->

                m.opaqueWidthFrac ?: run {

                    val fxSpread = (stable.maxOf { it.feetX } - stable.minOf { it.feetX })

                        .coerceAtLeast(0.08f)

                    if (normalized) maxOf(fxSpread * 2.4f, 0.30f) else fxSpread.coerceAtLeast(0.35f)

                }

            }.coerceAtMost(1f)

            return fromLayoutSpan(minHeight, minWidth)

        }

        val topFrac = stable.maxOf { it.headTopY }.coerceIn(0f, 1f - MIN_EXTENT)

        val bottomPad = PacMazeIkunGameplayScale.BITMAP_FEET_BBOX_PAD_FRAC

        val bottomFrac = (stable.maxOf { it.feetY } + bottomPad).coerceIn(topFrac + MIN_EXTENT, 1f)

        val fxMin = stable.minOf { it.feetX }

        val fxMax = stable.maxOf { it.feetX }

        val fxSpread = (fxMax - fxMin).coerceAtLeast(0.08f)

        val widthFrac = if (normalized) {

            maxOf(fxSpread * 2.4f, 0.30f)

        } else {

            fxSpread.coerceAtLeast(0.35f)

        }.coerceAtMost(1f)

        val centerX = (fxMin + fxMax) * 0.5f

        val left = (centerX - widthFrac * 0.5f).coerceIn(0f, 1f - widthFrac)

        return ContentBoundsFrac(

            topFrac = topFrac,

            bottomFrac = bottomFrac,

            leftFrac = left,

            rightFrac = left + widthFrac,

        )

    }



    fun fromManifestPlatformer(

        manifest: PacMazeSkinAnimManifest.SkinAnimManifest,

        clip: PacMazeSkinAnimClip,

    ): ContentBoundsFrac? {

        val metrics = manifest.platformerClipMetrics(clip) ?: return null

        if (metrics.isEmpty()) return null

        return fromPlatformerMetrics(metrics, manifest.normalized)

    }



    /** 单格可走道（原点 = 过道几何中心，含地板下沉区）。 */

    fun canonicalWalkableBox(cellX: Float, cellY: Float): Rect {

        val inset = PacMazeIkunGameplayScale.CORRIDOR_WALL_INSET_FRAC

        val sink = PacMazeIkunGameplayScale.CORRIDOR_FLOOR_SINK_FRAC

        val fill = PacMazeIkunGameplayScale.BITMAP_CORRIDOR_FILL_FRAC

        val halfW = (cellX * (0.5f - inset) * fill).coerceAtLeast(1f)

        val halfH = (cellY * (0.5f - inset + sink * 0.5f) * fill).coerceAtLeast(1f)

        return Rect(-halfW, -halfH, halfW, halfH)

    }



    /**

     * 稳态尺寸：不透明内容竖直贴满 [canonicalWalkableBox]，再乘 HUD 视觉倍率。

     * 不用整帧 pivot 缩 sprite（归一化 sheet 顶侧大留白会把角色压成点）。

     */

    fun resolveStableSize(

        aspect: Float,

        content: ContentBoundsFrac,

        cellX: Float,

        cellY: Float,

        visualScale: Float = 1f,

    ): FitResult {

        val safeAspect = aspect.coerceIn(0.35f, 3.5f)

        val (px, py) = content.pivot()

        val fitCell = min(cellX, cellY)

        val box = canonicalWalkableBox(cellX, cellY)

        val oh = content.opaqueHeightFrac.coerceAtLeast(MIN_EXTENT)
        val ow = content.opaqueWidthFrac.coerceAtLeast(MIN_EXTENT)

        val hByHeight = box.height / oh
        val wByHeight = hByHeight * safeAspect
        val wByWidth = box.width / ow
        val hByWidth = wByWidth / safeAspect

        // 等比缩进通道：同时不超出高/宽（避免窄 sheet 按宽定标→身高撑满 2~3 格）。
        var h = min(hByHeight, hByWidth)
        var w = h * safeAspect

        val mul = visualScale.coerceAtLeast(0.5f) * PacMazeIkunGameplayScale.BITMAP_CORRIDOR_OPAQUE_BOOST
        h *= mul
        w = h * safeAspect

        // HUD 滑条：按 visualScale 放宽通道上限（map clip 防出界）。
        val scaleCap = visualScale.coerceIn(0.5f, PacMazeIkunGameplayScale.MAX_VISUAL_SCALE)
        val hCap = box.height * scaleCap
        val wCap = box.width * scaleCap
        if (h * oh > hCap) {
            h = hCap / oh
            w = h * safeAspect
        }
        if (w * ow > wCap) {
            w = wCap / ow
            h = w / safeAspect
        }

        return FitResult(w, h, px, py)
    }

    /** @deprecated 局内不再按格缩尺寸；保留供测试。 */
    fun finalizeAtRuntime(

        width: Float,

        height: Float,

        aspect: Float,

        content: ContentBoundsFrac,

        wallBox: Rect?,

        mapClip: Rect?,

        corridorCenter: Offset,

    ): Pair<Float, Float> {

        if (wallBox == null || !wallBox.isValid()) return width to height

        val fitCell = min(wallBox.width, wallBox.height).coerceAtLeast(1f)

        val margin = PacMazeIkunGameplayScale.bitmapWallFitMarginPx(fitCell)

        var box = insetWallBox(wallBox, margin)

        if (mapClip != null && mapClip.isValid()) {

            box = intersectRects(box, mapClip) ?: return width to height

        }

        if (!box.isValid()) return width to height

        return clampUniformForOpaqueContent(

            width = width,

            height = height,

            box = box,

            anchor = corridorCenter,

            content = content,

            aspect = aspect,

        )

    }



    /** 不透明 bbox 以 anchor 为中心贴墙；**不**用整帧 pivot 缩小。 */

    fun maxUniformForOpaqueContent(

        box: Rect,

        anchor: Offset,

        content: ContentBoundsFrac,

        aspect: Float,

    ): Pair<Float, Float> {

        val maxH = min(

            (anchor.y - box.top) / content.aboveCenter,

            (box.bottom - anchor.y) / content.belowCenter,

        )

        val maxW = min(

            (anchor.x - box.left) / content.leftOfCenter,

            (box.right - anchor.x) / content.rightOfCenter,

        )



        val safeH = when {

            maxH.isFinite() && maxH > 0f -> maxH

            else -> box.height

        }

        val safeW = when {

            maxW.isFinite() && maxW > 0f -> maxW

            else -> box.width

        }



        val hFromWidth = safeW / aspect.coerceAtLeast(0.35f)

        val h = min(safeH, hFromWidth).coerceAtLeast(0f)

        return h * aspect to h

    }



    fun clampUniformForOpaqueContent(

        width: Float,

        height: Float,

        box: Rect,

        anchor: Offset,

        content: ContentBoundsFrac,

        aspect: Float,

    ): Pair<Float, Float> {

        val wallMax = maxUniformForOpaqueContent(box, anchor, content, aspect)

        var h = min(height, wallMax.second)

        var w = h * aspect

        if (w > wallMax.first) {

            w = wallMax.first

            h = w / aspect

        }

        return w to h

    }



    fun opaqueVerticalHalfExtents(height: Float, content: ContentBoundsFrac): Pair<Float, Float> {

        val above = height * content.aboveCenter

        val below = height * content.belowCenter

        return above to below

    }



    fun spriteVerticalHalfExtents(height: Float, pivotFracY: Float): Pair<Float, Float> {

        val py = pivotFracY.coerceIn(MIN_EXTENT, 1f - MIN_EXTENT)

        return height * py to height * (1f - py)

    }



    /** 行走皮肤：仅左右贴墙 clip，底边保脚（不裁 vertical）。 */

    fun clipDrawRect(wallBox: Rect?, mapClip: Rect?): Rect? {

        if (wallBox == null || !wallBox.isValid()) return null

        val fitCell = min(wallBox.width, wallBox.height).coerceAtLeast(1f)

        val sideMargin = PacMazeIkunGameplayScale.bitmapWallFitMarginPx(fitCell)

        val bottomBleed = fitCell * PacMazeIkunGameplayScale.BITMAP_FEET_CLIP_BLEED_CELL_FRAC

        return Rect(

            left = wallBox.left + sideMargin,

            top = Float.NEGATIVE_INFINITY,

            right = wallBox.right - sideMargin,

            bottom = wallBox.bottom + bottomBleed,

        )

    }



    private fun intersectRects(a: Rect, b: Rect): Rect? {

        val left = max(a.left, b.left)

        val top = max(a.top, b.top)

        val right = min(a.right, b.right)

        val bottom = min(a.bottom, b.bottom)

        if (right - left < 1f || bottom - top < 1f) return null

        return Rect(left, top, right, bottom)

    }



    private fun insetWallBox(wallBox: Rect, margin: Float): Rect = Rect(

        left = wallBox.left + margin,

        top = wallBox.top + margin,

        right = wallBox.right - margin,

        bottom = wallBox.bottom - margin,

    )



    private fun Rect.isValid(): Boolean = width > 1f && height > 1f



    private fun filterStableWalkMetrics(

        metrics: List<PacMazeSkinAnimManifest.PlatformerFrameMetrics>,

    ): List<PacMazeSkinAnimManifest.PlatformerFrameMetrics> {

        if (metrics.size <= 2) return metrics

        val heights = metrics.map { (it.feetY - it.headTopY).coerceAtLeast(MIN_EXTENT) }

        val sorted = heights.sorted()

        val medianH = sorted[sorted.size / 2]

        val filtered = metrics.filter { m ->

            val h = m.feetY - m.headTopY

            h >= medianH * 0.78f && h <= medianH * 1.22f

        }

        return filtered.ifEmpty { metrics }

    }

}


