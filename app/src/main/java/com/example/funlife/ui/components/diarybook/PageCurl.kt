// ═══════════════════════════════════════════════════════════════════════════
// PageCurl.kt — 真 3D 翻页（基于 OpenGL ES 2.0 / eschao/android-PageFlip）
//
// · 用 AndroidView 把一个 GLSurfaceView 嵌进 Compose
// · 每页内容由调用方提供的 drawPage(idx) DSL 通过 CanvasDrawScope
//   渲染为一张 Bitmap，再 setFirstTexture / setSecondTexture 喂给 GL
// · 折角阴影、圆柱卷曲、圆锥半径、纸背镜像（番茄阅读同款）全部
//   由 PageFlip 库的着色器在 GPU 上完成
// · onPageChange 在 GL 线程发起翻页结束后通过 main handler 回调
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.eschao.android.widget.pageflip.OnPageFlipListener
import com.eschao.android.widget.pageflip.PageFlip
import com.eschao.android.widget.pageflip.PageFlipState
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.compose.ui.graphics.Canvas as ComposeUiCanvas

@Composable
fun PageCurl(
    pageCount: Int,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 外部依赖变化时强制重画所有页面纹理（例如皮肤切换）。 */
    refreshKey: Any = Unit,
    drawPage: DrawScope.(pageIndex: Int) -> Unit
) {
    val density = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current
    val drawPageState = rememberUpdatedState(drawPage)
    val onPageChangeState = rememberUpdatedState(onPageChange)
    val pageCountState = rememberUpdatedState(pageCount)
    // 记录上一次 Compose 端 currentPage —— 只在它真的变化时才下推到 view，
    // 防止动画进行中"无关重组"误把页面拉回。
    val lastComposePage = remember { androidx.compose.runtime.mutableStateOf(currentPage) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FunLifePageFlipView(ctx).apply {
                renderPageBitmap = { idx, w, h ->
                    if (w <= 0 || h <= 0) null
                    else renderPageToBitmap(
                        idx, w, h,
                        density = density,
                        layoutDir = layoutDir,
                        drawPage = drawPageState.value
                    )
                }
                onPageChanged = { newIdx ->
                    // GL 翻完通知 Compose；同步刷新 lastComposePage 以避免下一帧 update 再次回拉
                    lastComposePage.value = newIdx
                    onPageChangeState.value(newIdx)
                }
                getPageCount = { pageCountState.value }
            }
        },
        update = { view ->
            // 仅当 Compose 端 currentPage 真正发生变化（外部按钮 / 目录跳转）时才同步到 GL
            if (currentPage != lastComposePage.value) {
                lastComposePage.value = currentPage
                if (view.currentPage != currentPage) {
                    view.jumpToPage(currentPage)
                }
            }
            // refreshKey 变化 → 清除所有纹理重新渲染当前页
            if (view.lastRefreshKey != refreshKey) {
                view.lastRefreshKey = refreshKey
                view.invalidateAllTextures()
            }
        }
    )
}

/**
 * 把一页 Compose DrawScope 内容栅格化成 Bitmap。
 */
private fun renderPageToBitmap(
    idx: Int,
    width: Int,
    height: Int,
    density: androidx.compose.ui.unit.Density,
    layoutDir: androidx.compose.ui.unit.LayoutDirection,
    drawPage: DrawScope.(Int) -> Unit
): Bitmap {
    val img = ImageBitmap(width, height)
    val canvas = ComposeUiCanvas(img)
    CanvasDrawScope().draw(density, layoutDir, canvas, Size(width.toFloat(), height.toFloat())) {
        drawPage(idx)
    }
    return img.asAndroidBitmap()
}

/**
 * 内置的 GL 翻页视图：直接套用 eschao Sample 的 SinglePageRender 逻辑，
 * 只是把"绘制页内容"那一步替换成调用 [renderPageBitmap] 回调。
 */
private class FunLifePageFlipView(context: Context) :
    GLSurfaceView(context),
    GLSurfaceView.Renderer,
    OnPageFlipListener {

    private val pageFlip: PageFlip = PageFlip(context).also { pf ->
        pf.setSemiPerimeterRatio(0.8f)
            .setShadowWidthOfFoldEdges(5f, 60f, 0.3f)
            .setShadowWidthOfFoldBase(5f, 80f, 0.4f)
            .setPixelsOfMesh(10)
            .setListener(this)
        pf.enableAutoPage(false)   // 单页模式；返回 boolean，不能加入链
    }
    private val drawLock = ReentrantLock()
    private var pageBitmap: Bitmap? = null
    private val animateDuration = 1000

    /** 当前展示的页索引（0-based）。GL 线程读写需在 drawLock 内。 */
    @Volatile var currentPage: Int = 0

    /** 记录上一次 PageCurl.refreshKey，仅 main 线程 update 访问。 */
    var lastRefreshKey: Any? = Unit

    /** Compose 回调：根据页号渲染 Bitmap。GL 线程会调用。 */
    var renderPageBitmap: ((idx: Int, w: Int, h: Int) -> Bitmap?)? = null
    /** 翻页完成回调，通过 main handler 调度到主线程。 */
    var onPageChanged: ((Int) -> Unit)? = null
    /** 页总数提供者。GL 线程读取，需保持线程安全（lambda 自身只读外部 State）。 */
    var getPageCount: () -> Int = { 1 }

    private var drawCommand: Int = DRAW_FULL_PAGE

    private val mainHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_ENDED_DRAWING_FRAME) {
                try {
                    drawLock.lock()
                    if (onEndedDrawing(msg.arg1)) requestRender()
                } finally {
                    drawLock.unlock()
                }
            }
        }
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    // ── OnPageFlipListener ───────────────────────────────────────────
    override fun canFlipForward(): Boolean = currentPage < getPageCount() - 1

    override fun canFlipBackward(): Boolean {
        val can = currentPage > 0
        if (can) {
            // 在动画开始前，把 first 纹理拷给 second（这样开始翻动后才有"下面"的下一页可见）
            pageFlip.firstPage?.setSecondTextureWithFirst()
        }
        return can
    }

    // ── 触摸事件 ─────────────────────────────────────────────────────
    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!pageFlip.isAnimating && pageFlip.firstPage != null) {
                    pageFlip.onFingerDown(event.x, event.y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pageFlip.isAnimating) return true
                if (pageFlip.canAnimate(event.x, event.y)) {
                    // 触点已超出当前页，立即结束并触发动画
                    finishWithFlip(event.x, event.y)
                } else if (pageFlip.onFingerMove(event.x, event.y)) {
                    try {
                        drawLock.lock()
                        drawCommand = DRAW_MOVING_FRAME
                        requestRender()
                    } finally {
                        drawLock.unlock()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                finishWithFlip(event.x, event.y)
            }
        }
        return true
    }

    private fun finishWithFlip(x: Float, y: Float) {
        if (pageFlip.isAnimating) return
        pageFlip.onFingerUp(x, y, animateDuration)
        try {
            drawLock.lock()
            if (pageFlip.animating()) {
                drawCommand = DRAW_ANIMATING_FRAME
                requestRender()
            }
        } finally {
            drawLock.unlock()
        }
    }

    // ── GLSurfaceView.Renderer ───────────────────────────────────────
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try { pageFlip.onSurfaceCreated() } catch (_: Exception) {}
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        try {
            pageFlip.onSurfaceChanged(width, height)
            pageBitmap?.recycle()
            val page = pageFlip.firstPage ?: return
            pageBitmap = Bitmap.createBitmap(
                page.width().toInt().coerceAtLeast(1),
                page.height().toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
        } catch (_: Exception) {}
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            drawLock.lock()
            pageFlip.deleteUnusedTextures()
            val page = pageFlip.firstPage ?: return

            when (drawCommand) {
                DRAW_MOVING_FRAME, DRAW_ANIMATING_FRAME -> {
                    if (pageFlip.flipState == PageFlipState.FORWARD_FLIP) {
                        if (!page.isSecondTextureSet) {
                            renderInto(currentPage + 1)
                            pageBitmap?.let { page.setSecondTexture(it) }
                        }
                    } else if (!page.isFirstTextureSet) {
                        // 后退翻：currentPage 已减 1（在 canFlipBackward 后），需重画当前
                        currentPage = (currentPage - 1).coerceAtLeast(0)
                        notifyPageChanged()
                        renderInto(currentPage)
                        pageBitmap?.let { page.setFirstTexture(it) }
                    }
                    pageFlip.drawFlipFrame()
                }
                DRAW_FULL_PAGE -> {
                    if (!page.isFirstTextureSet) {
                        renderInto(currentPage)
                        pageBitmap?.let { page.setFirstTexture(it) }
                    }
                    pageFlip.drawPageFrame()
                }
            }

            val msg = Message.obtain()
            msg.what = MSG_ENDED_DRAWING_FRAME
            msg.arg1 = drawCommand
            mainHandler.sendMessage(msg)
        } finally {
            drawLock.unlock()
        }
    }

    private fun onEndedDrawing(what: Int): Boolean {
        if (what == DRAW_ANIMATING_FRAME) {
            return if (pageFlip.animating()) {
                drawCommand = DRAW_ANIMATING_FRAME
                true
            } else {
                if (pageFlip.flipState == PageFlipState.END_WITH_FORWARD) {
                    pageFlip.firstPage?.setFirstTextureWithSecond()
                    currentPage = (currentPage + 1).coerceAtMost(getPageCount() - 1)
                    notifyPageChanged()
                }
                drawCommand = DRAW_FULL_PAGE
                true
            }
        }
        return false
    }

    private fun notifyPageChanged() {
        val idx = currentPage
        mainHandler.post { onPageChanged?.invoke(idx) }
    }

    private fun renderInto(idx: Int) {
        val target = pageBitmap ?: return
        val src = renderPageBitmap?.invoke(idx, target.width, target.height) ?: return
        // 把渲染好的 bitmap 复制到 target（PageFlip 内部会从 target 上传纹理）
        if (src === target) return
        val canvas = android.graphics.Canvas(target)
        canvas.drawBitmap(src, 0f, 0f, null)
        if (!src.isRecycled) src.recycle()
    }

    /** 外部（按钮、ViewModel）强制跳页 → 清掉所有纹理强制重画。 */
    fun jumpToPage(idx: Int) {
        try {
            drawLock.lock()
            currentPage = idx.coerceIn(0, (getPageCount() - 1).coerceAtLeast(0))
            queueEvent {
                pageFlip.firstPage?.deleteAllTextures()
                drawCommand = DRAW_FULL_PAGE
                requestRender()
            }
        } finally {
            drawLock.unlock()
        }
    }

    /** 仅重画纹理、保留当前页 —— 用于皮肤切换。 */
    fun invalidateAllTextures() {
        queueEvent {
            pageFlip.firstPage?.deleteAllTextures()
            try {
                drawLock.lock()
                drawCommand = DRAW_FULL_PAGE
            } finally {
                drawLock.unlock()
            }
            requestRender()
        }
    }

    companion object {
        private const val DRAW_MOVING_FRAME = 0
        private const val DRAW_ANIMATING_FRAME = 1
        private const val DRAW_FULL_PAGE = 2
        private const val MSG_ENDED_DRAWING_FRAME = 1
    }
}
