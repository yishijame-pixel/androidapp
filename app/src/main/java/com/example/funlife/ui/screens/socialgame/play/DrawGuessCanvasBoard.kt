package com.example.funlife.ui.screens.socialgame.play



import androidx.compose.foundation.Canvas

import androidx.compose.foundation.gestures.detectDragGestures

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.mutableLongStateOf

import androidx.compose.runtime.mutableStateListOf

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.runtime.setValue

import androidx.compose.runtime.withFrameNanos

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.unit.IntSize

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.util.Log

import com.example.funlife.social.drawws.DrawGuessLiveSync

import com.example.funlife.social.game.engine.DrawGuessSync

import com.example.funlife.viewmodel.DrawStrokeUi

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.sync.Mutex

import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.withContext



/**

 * 企业级画布宿主：

 * - 输入环形缓冲（无 Compose State）

 * - VSYNC 帧循环（画家/猜词方）

 * - 已提交层双缓冲 bitmap + 增量 append（无闪烁）

 * - 猜词方 chunk 直读 WS liveStrokes，stroke_end 才入 bitmap

 */

@Composable

fun DrawGuessCanvasBoard(

    committedStrokes: List<DrawStrokeUi>,

    brush: DrawBrushState,

    isDrawer: Boolean,

    useLiveWs: Boolean,

    liveWireEnabled: Boolean,

    canDraw: Boolean,

    isDrawingPhase: Boolean,

    clearToken: Int,

    onStrokeChunk: (

        strokeId: String,

        seq: Int,

        points: List<List<Float>>,

        color: String,

        width: Float,

        flushNow: Boolean,

    ) -> Unit,

    onStrokeEnd: (strokeId: String, color: String, width: Float) -> Unit,

    modifier: Modifier = Modifier,

) {

    val density = LocalDensity.current

    val inkRing = remember { DrawGuessInputRingBuffer() }

    val inkScratch = remember { mutableStateListOf<Pair<Float, Float>>() }

    val layerCache = remember { DrawGuessCommittedLayerCache() }

    var frame by remember { mutableIntStateOf(0) }

    var layerGeneration by remember { mutableIntStateOf(0) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var lastChunkIndex by remember { mutableIntStateOf(0) }

    var lastChunkAtMs by remember { mutableLongStateOf(0L) }

    var strokeSeq by remember { mutableIntStateOf(0) }



    val rxLiveStrokes by DrawGuessLiveSync.liveStrokes.collectAsStateWithLifecycle(

        initialValue = emptyList(),

    )



    val brushRef = rememberUpdatedState(brush)

    val onChunkRef = rememberUpdatedState(onStrokeChunk)

    val onEndRef = rememberUpdatedState(onStrokeEnd)

    val liveWireRef = rememberUpdatedState(liveWireEnabled)

    LaunchedEffect(clearToken) {

        inkRing.clear()

        lastChunkIndex = 0

        lastChunkAtMs = 0L

        strokeSeq = 0

    }



    LaunchedEffect(clearToken, isDrawingPhase, useLiveWs) {

        while (true) {

            withFrameNanos { frame++ }

        }

    }



    // 画家本地 ink 实时渲染；bitmap 层仅猜词方，避免拖动时 async rebuild 白屏闪烁
    val useBitmapLayer = !isDrawer
    val strokeFinalizeNonce by DrawGuessLiveSync.strokeFinalizeNonce.collectAsStateWithLifecycle(
        initialValue = 0,
    )
    val wsClearNonce by DrawGuessLiveSync.clearNonce.collectAsStateWithLifecycle(initialValue = 0)
    // WS 猜词方用 clearNonce 驱动 bitmap 清屏，避免 ledger drawClearToken  recount 闪烁
    val layerClearToken = if (useBitmapLayer && useLiveWs) wsClearNonce else clearToken
    // 猜词方 WS 活跃时 bitmap 仅跟 finalized live，勿 merge committed（PB ingest / silent refresh 会触发多余 layer sync）
    val bitmapStrokes = if (useBitmapLayer) {
        val finalizedLive = rxLiveStrokes.filter {
            DrawGuessLiveSync.isStrokeFinalized(it.strokeId.orEmpty())
        }
        val coalesced = if (useLiveWs) {
            DrawGuessSync.coalesceStrokes(finalizedLive)
        } else {
            DrawGuessSync.coalesceStrokes(committedStrokes)
        }
        if (useLiveWs) DrawGuessLayerBitmapPolicy.contiguousSeqPrefix(coalesced) else coalesced
    } else {
        emptyList()
    }

    val layerFingerprint = remember(bitmapStrokes) {

        DrawGuessLayerFingerprint.fromStrokes(bitmapStrokes)

    }



    // 画家抬手：ink 保留到 committed 入账后再清，避免抬手瞬间笔画消失
    LaunchedEffect(committedStrokes, isDrawer) {

        if (!isDrawer || inkRing.strokeId.isBlank() || inkRing.size() <= 0) return@LaunchedEffect

        val sid = inkRing.strokeId

        val committed = committedStrokes.firstOrNull { it.strokeId == sid }

        if (committed != null && committed.points.size >= inkRing.size()) {

            inkRing.clear()

            lastChunkIndex = 0

        }

    }



    val layerMutex = remember { Mutex() }



    LaunchedEffect(layerFingerprint, canvasSize, layerClearToken, useBitmapLayer, strokeFinalizeNonce) {

        if (!useBitmapLayer) return@LaunchedEffect

        val strokes = bitmapStrokes

        val w = canvasSize.width

        val h = canvasSize.height

        val token = layerClearToken

        val densityValue = density.density

        if (w <= 0 || h <= 0) return@LaunchedEffect

        val action = layerMutex.withLock {

            withContext(Dispatchers.Default) {

                layerCache.syncToStrokes(strokes, token, w, h, densityValue)

            }

        }

        when (action) {

            LayerSyncAction.CLEARED ->

                Log.i(CANVAS_LAYER_LOG, "layer clear token=$token")

            LayerSyncAction.APPENDED ->

                Log.i(

                    CANVAS_LAYER_LOG,

                    "layer append n=${strokes.size} last=${strokes.lastOrNull()?.strokeId} token=$token",

                )

            LayerSyncAction.REBUILT ->

                Log.i(

                    CANVAS_LAYER_LOG,

                    "layer rebuild n=${strokes.size} last=${strokes.lastOrNull()?.strokeId} token=$token",

                )

            LayerSyncAction.UNCHANGED, LayerSyncAction.SKIPPED -> Unit

        }

        if (action != LayerSyncAction.UNCHANGED && action != LayerSyncAction.SKIPPED) {

            layerGeneration++

        }

    }



    fun flushChunk(toIndex: Int, force: Boolean) {

        val newPoints = toIndex - lastChunkIndex

        val minPoints = if (liveWireRef.value) 1 else 2

        if (!force && newPoints < minPoints) return

        if (force && newPoints < 1) return

        val now = System.currentTimeMillis()

        val throttleMs = if (liveWireRef.value) 4L else 16L

        if (!force && now - lastChunkAtMs < throttleMs) return

        val sid = inkRing.strokeId

        if (sid.isBlank()) return

        val chunk = ArrayList<List<Float>>(newPoints.coerceAtLeast(4))

        inkRing.forEachFrom(lastChunkIndex) { x, y ->

            chunk.add(listOf(x, y))

        }

        if (chunk.isEmpty()) return

        if (!liveWireRef.value) strokeSeq += 1

        onChunkRef.value(

            sid,

            if (liveWireRef.value) 0 else strokeSeq,

            chunk,

            brushRef.value.colorForStroke,

            brushRef.value.strokeWidth,

            force,

        )

        lastChunkIndex = toIndex

        lastChunkAtMs = now

    }



    Canvas(

        modifier = modifier

            .graphicsLayer()

            .onSizeChanged { canvasSize = it }

            .pointerInput(isDrawer, isDrawingPhase, clearToken, liveWireEnabled, canDraw) {

                if (!isDrawer || !isDrawingPhase || !canDraw) return@pointerInput

                detectDragGestures(

                    onDragStart = { offset ->

                        inkRing.beginStroke(

                            DrawGuessLiveSync.newStrokeId(),

                            offset.x / size.width,

                            offset.y / size.height,

                        )

                        lastChunkIndex = 0

                        lastChunkAtMs = 0L

                        flushChunk(inkRing.size(), force = true)

                    },

                    onDrag = { change, _ ->

                        change.consume()

                        inkRing.append(

                            change.position.x / size.width,

                            change.position.y / size.height,

                        )

                        flushChunk(inkRing.size(), force = false)

                    },

                    onDragEnd = {

                        flushChunk(inkRing.size(), force = true)

                        val sid = inkRing.strokeId

                        if (sid.isNotBlank()) {

                            onEndRef.value(

                                sid,

                                brushRef.value.colorForStroke,

                                brushRef.value.strokeWidth,

                            )

                        }

                        lastChunkIndex = 0

                    },

                )

            },

    ) {

        @Suppress("UNUSED_VARIABLE")

        val _frame = frame

        @Suppress("UNUSED_VARIABLE")

        val _layerGen = layerGeneration



        if (useBitmapLayer) {

            layerCache.snapshot()?.let { bmp ->

                if (bmp.width > 0 && bmp.height > 0) {

                    drawImage(

                        image = bmp,

                        srcOffset = IntOffset.Zero,

                        srcSize = IntSize(bmp.width, bmp.height),

                        dstOffset = IntOffset.Zero,

                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),

                    )

                }

            }

        }



        if (isDrawer) {

            val activeId = inkRing.strokeId.takeIf { inkRing.size() > 0 }.orEmpty()

            committedStrokes.forEach { stroke ->

                val sid = stroke.strokeId.orEmpty()

                if (sid.isNotBlank() && sid == activeId) return@forEach

                if (stroke.points.size < 2) return@forEach

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



        if (isDrawer && inkRing.size() > 0) {

            inkScratch.clear()

            inkRing.snapshotInto(inkScratch)

            DrawGuessCanvasRenderer.drawStrokePoints(

                drawScope = this,

                points = inkScratch,

                color = brushRef.value.composeColor,

                width = brushRef.value.strokeWidth,

                canvasWidth = size.width,

                canvasHeight = size.height,

                smooth = true,

            )

        }



        if (!isDrawer && useLiveWs) {

            liveOverlayStrokes(rxLiveStrokes, committedStrokes, layerCache).forEach { stroke ->

                val sid = stroke.strokeId.orEmpty()
                // stroke_end 后勿立刻停插值，否则笔尖外推收回会产生「抬手闪一下」
                val rasterPts = layerCache.cachedPointCount(sid)
                val bitmapReady = sid.isNotBlank() &&
                    layerCache.hasStroke(sid) &&
                    rasterPts >= stroke.points.size
                val renderPts = if (bitmapReady) {
                    stroke.points
                } else {
                    DrawStrokeInterpolator.renderPoints(
                        points = stroke.points,
                        lastRxMs = DrawGuessLiveSync.strokeReceivedAt(sid),
                    )
                }

                DrawGuessCanvasRenderer.drawStrokePoints(

                    drawScope = this,

                    points = renderPts,

                    color = DrawColorPalette.toColor(stroke.color),

                    width = stroke.width,

                    canvasWidth = size.width,

                    canvasHeight = size.height,

                    smooth = true,

                )

            }

        }

    }

}



/** 猜词方实时尾：仅画 bitmap 尚未 rasterize 的增量点，避免整笔叠绘闪烁 */
private fun liveOverlayStrokes(
    liveStrokes: List<DrawStrokeUi>,
    committedStrokes: List<DrawStrokeUi>,
    layerCache: DrawGuessCommittedLayerCache,
): List<DrawStrokeUi> {
    fun tailOnly(stroke: DrawStrokeUi): DrawStrokeUi? {
        val id = stroke.strokeId.orEmpty()
        if (id.isBlank() || stroke.points.size < 2) return null

        val rasterPts = layerCache.cachedPointCount(id)
        // 仅以 bitmap 实际 rasterize 进度为准；committed 入账早于 async append 会造成空帧闪烁
        if (layerCache.hasStroke(id) && rasterPts >= stroke.points.size) return null

        if (rasterPts <= 0) return stroke

        val tailStart = (rasterPts - 1).coerceAtLeast(0)
        if (tailStart >= stroke.points.size - 1) return null

        val tail = stroke.points.drop(tailStart)
        if (tail.size < 2) {
            return stroke.copy(points = stroke.points.takeLast(2))
        }
        return stroke.copy(points = tail)
    }

    val seen = mutableSetOf<String>()
    val overlay = buildList {
        liveStrokes.forEach { stroke ->
            val id = stroke.strokeId.orEmpty()
            tailOnly(stroke)?.let {
                add(it)
                seen.add(id)
            }
        }
        committedStrokes.forEach { stroke ->
            val id = stroke.strokeId.orEmpty()
            if (id.isBlank() || id in seen) return@forEach
            tailOnly(stroke)?.let { add(it) }
        }
    }
    return overlay
}



private const val CANVAS_LAYER_LOG = "DrawGuessCanvas"



/** @deprecated 保留供单元测试；运行时使用 [DrawGuessInputRingBuffer] */

class DrawGuessInkBuffer {

    private val points = ArrayList<Pair<Float, Float>>(256)

    var strokeId: String = ""

        private set



    fun start(strokeId: String, x: Float, y: Float) {

        points.clear()

        this.strokeId = strokeId

        points.add(x to y)

    }



    fun add(x: Float, y: Float) = points.add(x to y)



    fun clear() {

        points.clear()

        strokeId = ""

    }



    fun size(): Int = points.size



    fun snapshot(): List<Pair<Float, Float>> = points.toList()



    fun subList(fromIndex: Int): List<Pair<Float, Float>> {

        if (fromIndex >= points.size) return emptyList()

        return points.subList(fromIndex, points.size).map { it.first to it.second }

    }

}


