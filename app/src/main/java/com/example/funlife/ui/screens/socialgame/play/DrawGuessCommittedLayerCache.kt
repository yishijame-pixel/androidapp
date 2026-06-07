package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.funlife.viewmodel.DrawStrokeUi

/**
 * 双缓冲已提交层：整层重建 + 单笔增量 append（stroke_end 后仅画新增一笔）。
 */
class DrawGuessCommittedLayerCache {
    private var bitmap: ImageBitmap? = null
    private var layerWidth = 0
    private var layerHeight = 0
    private var cachedStrokeIds: List<String> = emptyList()
    private var cachedPointCounts: List<Int> = emptyList()
    private var cachedClearToken = -1

    fun snapshot(): ImageBitmap? = bitmap

    fun hasStroke(strokeId: String?): Boolean {
        if (strokeId.isNullOrBlank()) return false
        return cachedStrokeIds.contains(strokeId)
    }

    fun cachedPointCount(strokeId: String?): Int {
        if (strokeId.isNullOrBlank()) return 0
        val idx = cachedStrokeIds.indexOf(strokeId)
        return if (idx >= 0) cachedPointCounts.getOrElse(idx) { 0 } else 0
    }

    /** 已 rasterize 的笔画 id + 点数与目标一致时跳过整层 rebuild */
    fun matchesRasterizedStrokes(strokes: List<DrawStrokeUi>, clearToken: Int): Boolean {
        if (cachedClearToken != clearToken) return false
        val ids = strokes.map { it.strokeId.orEmpty() }
        val counts = strokes.map { it.points.size }
        return cachedStrokeIds == ids && cachedPointCounts == counts
    }

    /**
     * 原子同步到目标笔画集：先逐笔 append，失败再一次 rebuild。
     * 必须在单线程/互斥下调用，避免连续 finalize 并发导致 append 半途 rebuild 闪烁。
     */
    fun syncToStrokes(
        strokes: List<DrawStrokeUi>,
        clearToken: Int,
        width: Int,
        height: Int,
        density: Float,
    ): LayerSyncAction {
        if (width <= 0 || height <= 0) return LayerSyncAction.SKIPPED
        if (strokes.isEmpty()) {
            if (cachedStrokeIds.isEmpty()) {
                cachedClearToken = clearToken
                return LayerSyncAction.UNCHANGED
            }
            clear(clearToken, width, height)
            return LayerSyncAction.CLEARED
        }
        if (matchesRasterizedStrokes(strokes, clearToken)) {
            return LayerSyncAction.UNCHANGED
        }
        if (cachedClearToken != clearToken) {
            allocateIfNeeded(width, height)
            cachedStrokeIds = emptyList()
            cachedPointCounts = emptyList()
            cachedClearToken = clearToken
            paintBackground()
        }
        var appended = 0
        while (cachedStrokeIds.size < strokes.size) {
            val nextIndex = cachedStrokeIds.size
            val prefix = strokes.take(nextIndex + 1)
            if (!tryAppendStroke(strokes[nextIndex], prefix, clearToken, width, height, density)) {
                break
            }
            appended++
        }
        if (matchesRasterizedStrokes(strokes, clearToken)) {
            return if (appended > 0) LayerSyncAction.APPENDED else LayerSyncAction.UNCHANGED
        }
        if (tryExtendLastStroke(strokes, clearToken, width, height, density)) {
            return LayerSyncAction.APPENDED
        }
        rebuild(strokes, clearToken, width, height, density)
        return LayerSyncAction.REBUILT
    }

    fun clear(clearToken: Int, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        allocateIfNeeded(width, height)
        cachedStrokeIds = emptyList()
        cachedPointCounts = emptyList()
        cachedClearToken = clearToken
        paintBackground()
    }

    fun tryAppendStroke(
        stroke: DrawStrokeUi,
        strokes: List<DrawStrokeUi>,
        clearToken: Int,
        width: Int,
        height: Int,
        density: Float,
    ): Boolean {
        if (width <= 0 || height <= 0) return false
        allocateIfNeeded(width, height)
        if (cachedClearToken != clearToken) {
            cachedStrokeIds = emptyList()
            cachedPointCounts = emptyList()
            paintBackground()
            cachedClearToken = clearToken
        }
        if (!DrawGuessLayerAppendPolicy.canAppend(cachedStrokeIds, strokes)) return false
        val ids = strokes.map { it.strokeId.orEmpty() }
        val counts = strokes.map { it.points.size }

        val bmp = bitmap ?: return false
        val canvas = Canvas(bmp)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            DrawGuessCanvasRenderer.drawStrokePoints(
                drawScope = this,
                points = stroke.points,
                color = DrawColorPalette.toColor(stroke.color),
                width = stroke.width,
                canvasWidth = size.width,
                canvasHeight = size.height,
                smooth = true,
            )
        }
        cachedStrokeIds = ids
        cachedPointCounts = counts
        return true
    }

    fun rebuild(
        strokes: List<DrawStrokeUi>,
        clearToken: Int,
        width: Int,
        height: Int,
        density: Float,
    ) {
        if (width <= 0 || height <= 0) return
        allocateIfNeeded(width, height)
        paintBackground()
        val bmp = bitmap ?: return
        val canvas = Canvas(bmp)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            strokes.forEach { stroke ->
                DrawGuessCanvasRenderer.drawStrokePoints(
                    drawScope = this,
                    points = stroke.points,
                    color = DrawColorPalette.toColor(stroke.color),
                    width = stroke.width,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                    smooth = true,
                )
            }
        }
        cachedStrokeIds = strokes.map { it.strokeId.orEmpty() }
        cachedPointCounts = strokes.map { it.points.size }
        cachedClearToken = clearToken
    }

    /** 同 id 列表下仅末笔补点：只画增量尾段，避免 stroke_end 后 ingest 触发整层 rebuild 闪烁 */
    private fun tryExtendLastStroke(
        strokes: List<DrawStrokeUi>,
        clearToken: Int,
        width: Int,
        height: Int,
        density: Float,
    ): Boolean {
        if (width <= 0 || height <= 0 || cachedStrokeIds.isEmpty() || strokes.isEmpty()) return false
        val ids = strokes.map { it.strokeId.orEmpty() }
        if (cachedStrokeIds != ids || cachedPointCounts.size != ids.size) return false
        val lastIdx = strokes.lastIndex
        val newCount = strokes[lastIdx].points.size
        val oldCount = cachedPointCounts[lastIdx]
        if (newCount <= oldCount) return false
        if (cachedPointCounts.dropLast(1) != strokes.dropLast(1).map { it.points.size }) return false

        val stroke = strokes[lastIdx]
        val tailStart = (oldCount - 1).coerceAtLeast(0)
        val tail = stroke.points.drop(tailStart)
        if (tail.size < 2) {
            cachedPointCounts = strokes.map { it.points.size }
            return true
        }

        allocateIfNeeded(width, height)
        if (cachedClearToken != clearToken) return false
        val bmp = bitmap ?: return false
        val canvas = Canvas(bmp)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(density),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat()),
        ) {
            DrawGuessCanvasRenderer.drawStrokePoints(
                drawScope = this,
                points = tail,
                color = DrawColorPalette.toColor(stroke.color),
                width = stroke.width,
                canvasWidth = size.width,
                canvasHeight = size.height,
                smooth = true,
            )
        }
        cachedPointCounts = strokes.map { it.points.size }
        return true
    }

    private fun allocateIfNeeded(width: Int, height: Int) {
        if (bitmap == null || layerWidth != width || layerHeight != height) {
            bitmap = ImageBitmap(width, height)
            layerWidth = width
            layerHeight = height
            cachedStrokeIds = emptyList()
            cachedPointCounts = emptyList()
        }
    }

    private fun paintBackground() {
        val bmp = bitmap ?: return
        val canvas = Canvas(bmp)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(layerWidth.toFloat(), layerHeight.toFloat()),
        ) {
            drawRect(color = DrawGuessMatchPalette.canvasBg)
        }
    }
}

/** 猜词方 bitmap 入层策略：仅 seq 连续前缀，避免中间笔画晚到触发整层 rebuild。 */
internal object DrawGuessLayerBitmapPolicy {
    fun contiguousSeqPrefix(strokes: List<DrawStrokeUi>): List<DrawStrokeUi> {
        if (strokes.isEmpty()) return strokes
        val sorted = strokes.sortedBy { it.seq }
        var expect = sorted.first().seq
        val out = ArrayList<DrawStrokeUi>(sorted.size)
        for (stroke in sorted) {
            if (stroke.seq != expect) break
            out.add(stroke)
            expect++
        }
        return out
    }
}

/** 增量 append 判定：与 bitmap 无关，可 JVM 单测。 */
internal object DrawGuessLayerAppendPolicy {
    fun canAppend(cachedStrokeIds: List<String>, strokes: List<DrawStrokeUi>): Boolean {
        if (strokes.isEmpty()) return false
        val ids = strokes.map { it.strokeId.orEmpty() }
        if (cachedStrokeIds.isEmpty()) return strokes.size == 1
        return strokes.size == cachedStrokeIds.size + 1 && cachedStrokeIds == ids.dropLast(1)
    }
}

/** bitmap 层 fingerprint：strokeId + 点数（同 id 补点会触发层同步） */
internal object DrawGuessLayerFingerprint {
    fun fromStrokes(strokes: List<DrawStrokeUi>): Int {
        var h = strokes.size
        strokes.forEach { s ->
            h = 31 * h + (s.strokeId?.hashCode() ?: 0)
            h = 31 * h + s.points.size
        }
        return h
    }
}

/**
 * 猜词方 canvas republish 判定：仅当候选比当前更「富」（更多笔或同笔更多点）才刷新 UI，
 * 避免 finalize / ingest 双路径触发重复 bitmap rebuild 闪烁。
 */
internal object DrawGuessCanvasPublishPolicy {
    fun shouldSkipRepublish(
        candidate: List<DrawStrokeUi>,
        current: List<DrawStrokeUi>,
        clearToken: Int,
        currentClearToken: Int,
    ): Boolean {
        if (clearToken != currentClearToken) return false
        if (candidate.isEmpty() && current.isEmpty()) return true
        if (DrawGuessLayerFingerprint.fromStrokes(candidate) ==
            DrawGuessLayerFingerprint.fromStrokes(current)
        ) {
            return true
        }
        return !isRicherThan(candidate, current)
    }

    fun isRicherThan(candidate: List<DrawStrokeUi>, current: List<DrawStrokeUi>): Boolean {
        if (candidate.size > current.size) return true
        if (candidate.size < current.size) return false
        val curById = current.associateBy { it.strokeId.orEmpty() }
        var anyRicher = false
        for (c in candidate) {
            val sid = c.strokeId.orEmpty()
            if (sid.isBlank()) continue
            val cur = curById[sid] ?: return true
            when {
                c.points.size > cur.points.size -> anyRicher = true
                c.points.size < cur.points.size -> return false
            }
        }
        return anyRicher
    }
}

enum class LayerSyncAction { SKIPPED, UNCHANGED, CLEARED, APPENDED, REBUILT }
