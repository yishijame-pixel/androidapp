// ═══════════════════════════════════════════════════════════════════════════
// PageCurl.kt — 真 3D 翻页（OpenGL ES 2.0 / eschao/android-PageFlip）
//
// 防闪烁（v2）：
//   · 动画推进 / 纹理交换全部在 GL 线程完成（消除主线程竞态）
//   · 静止页预上传 next 页 second 纹理（向前翻零等待）
//   · 向后翻在 canFlipBackward 前同步 GL 预绑定 prev 页
//   · 不向 Compose 回传 currentPage，仅 jumpToPage 外部跳转
//   · deleteUnusedTextures 移到绘制之后
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.runtime.Composable
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
import com.eschao.android.widget.pageflip.Page
import com.eschao.android.widget.pageflip.PageFlip
import com.eschao.android.widget.pageflip.PageFlipState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.compose.ui.graphics.Canvas as ComposeUiCanvas

@Composable
fun PageCurl(
    pageCount: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** 外部跳转页码（目录等）；消费后需 onJumpConsumed。 */
    jumpToPage: Int? = null,
    onJumpConsumed: () -> Unit = {},
    refreshKey: Any = Unit,
    drawPage: DrawScope.(pageIndex: Int) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDir = LocalLayoutDirection.current
    val drawPageState = rememberUpdatedState(drawPage)
    val onPageChangeState = rememberUpdatedState(onPageChange)
    val pageCountState = rememberUpdatedState(pageCount)
    val jumpState = rememberUpdatedState(jumpToPage)
    val onJumpConsumedState = rememberUpdatedState(onJumpConsumed)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FunLifePageFlipView(ctx).apply {
                renderPageBitmap = { idx, w, h ->
                    if (w <= 0 || h <= 0) null
                    else renderPageToBitmap(
                        idx, w, h, density, layoutDir, drawPageState.value,
                    )
                }
                onPageChanged = { onPageChangeState.value(it) }
                getPageCount = { pageCountState.value.coerceAtLeast(1) }
            }
        },
        update = { view ->
            view.syncPageCount(pageCountState.value)
            jumpState.value?.let { target ->
                view.jumpToPage(target)
                onJumpConsumedState.value()
            }
            if (view.lastRefreshKey != refreshKey) {
                view.lastRefreshKey = refreshKey
                view.invalidateAllTextures()
            }
        },
    )
}

private fun renderPageToBitmap(
    idx: Int,
    width: Int,
    height: Int,
    density: androidx.compose.ui.unit.Density,
    layoutDir: androidx.compose.ui.unit.LayoutDirection,
    drawPage: DrawScope.(Int) -> Unit,
): Bitmap {
    val img = ImageBitmap(width, height)
    val canvas = ComposeUiCanvas(img)
    CanvasDrawScope().draw(
        density, layoutDir, canvas, Size(width.toFloat(), height.toFloat()),
    ) {
        drawPage(idx)
    }
    return img.asAndroidBitmap()
}

private class PageBitmapCache(private val maxEntries: Int = 20) {
    private val map = LinkedHashMap<Int, Bitmap>(maxEntries, 0.75f, true)

    fun getOrRender(idx: Int, w: Int, h: Int, render: (Int, Int, Int) -> Bitmap?): Bitmap? {
        map[idx]?.let { cached ->
            if (!cached.isRecycled && cached.width == w && cached.height == h) return cached
            map.remove(idx)
        }
        val fresh = render(idx, w, h) ?: return null
        val stored = if (fresh.isMutable && fresh.width == w && fresh.height == h) {
            fresh
        } else {
            fresh.copy(Bitmap.Config.ARGB_8888, false).also {
                if (fresh !== it && !fresh.isRecycled) fresh.recycle()
            }
        }
        while (map.size >= maxEntries) {
            val eldest = map.entries.firstOrNull() ?: break
            map.remove(eldest.key)
            if (!eldest.value.isRecycled) eldest.value.recycle()
        }
        map[idx] = stored
        return stored
    }

    fun clear() {
        map.values.forEach { if (!it.isRecycled) it.recycle() }
        map.clear()
    }
}

private class FunLifePageFlipView(context: Context) :
    GLSurfaceView(context),
    GLSurfaceView.Renderer,
    OnPageFlipListener {

    private val pageFlip: PageFlip = PageFlip(context).also { pf ->
        pf.setSemiPerimeterRatio(0.8f)
            .setShadowWidthOfFoldEdges(5f, 60f, 0.3f)
            .setShadowWidthOfFoldBase(5f, 80f, 0.4f)
            .setPixelsOfMesh(10)
            .setClearColor(0.96f, 0.94f, 0.88f, 1f)
            .setListener(this)
        pf.enableAutoPage(false)
    }

    private val drawLock = ReentrantLock()
    private val bitmapCache = PageBitmapCache()
    private var uploadBitmap: Bitmap? = null
    private val animateDuration = 850

    @Volatile var currentPage: Int = 0
    var lastRefreshKey: Any? = Unit
    private var cachedPageCount = 1

    var renderPageBitmap: ((idx: Int, w: Int, h: Int) -> Bitmap?)? = null
    var onPageChanged: ((Int) -> Unit)? = null
    var getPageCount: () -> Int = { 1 }

    private var drawCommand = DRAW_FULL_PAGE
    private var pendingPageNotify: Int? = null
    private var backwardPrepared = false
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun syncPageCount(count: Int) {
        if (count != cachedPageCount) {
            cachedPageCount = count
            bitmapCache.clear()
        }
    }

    override fun canFlipForward(): Boolean = currentPage < getPageCount() - 1

    override fun canFlipBackward(): Boolean {
        if (currentPage <= 0) return false
        if (backwardPrepared) return true
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val latch = CountDownLatch(1)
            var ok = false
            queueEvent {
                ok = prepareBackwardFlipOnGl()
                latch.countDown()
            }
            latch.await(80, TimeUnit.MILLISECONDS)
            return ok
        }
        return prepareBackwardFlipOnGl()
    }

    /** GL 线程：向后翻前先绑定 prev 页 first 纹理，避免首帧空白。 */
    private fun prepareBackwardFlipOnGl(): Boolean {
        if (backwardPrepared || currentPage <= 0) return currentPage > 0
        val page = pageFlip.firstPage ?: return false
        currentPage = (currentPage - 1).coerceAtLeast(0)
        blitPageIntoUpload(currentPage)
        page.setSecondTextureWithFirst()
        uploadBitmap?.let { page.setFirstTexture(it) }
        backwardPrepared = true
        return true
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                backwardPrepared = false
                prefetchAdjacentPages()
                if (!pageFlip.isAnimating && pageFlip.firstPage != null) {
                    pageFlip.onFingerDown(event.x, event.y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pageFlip.isAnimating) return true
                if (pageFlip.canAnimate(event.x, event.y)) {
                    finishWithFlip(event.x, event.y)
                } else if (pageFlip.onFingerMove(event.x, event.y)) {
                    beginMovingFrame()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finishWithFlip(event.x, event.y)
        }
        return true
    }

    private fun beginMovingFrame() {
        try {
            drawLock.lock()
            drawCommand = DRAW_MOVING_FRAME
            requestRender()
        } finally {
            drawLock.unlock()
        }
    }

    private fun finishWithFlip(x: Float, y: Float) {
        if (pageFlip.isAnimating) return
        pageFlip.onFingerUp(x, y, animateDuration)
        try {
            drawLock.lock()
            if (pageFlip.animating()) {
                drawCommand = DRAW_ANIMATING_FRAME
                renderMode = RENDERMODE_CONTINUOUSLY
            }
        } finally {
            drawLock.unlock()
        }
    }

    private fun prefetchAdjacentPages() {
        queueEvent {
            val page = pageFlip.firstPage ?: return@queueEvent
            val w = page.width().toInt().coerceAtLeast(1)
            val h = page.height().toInt().coerceAtLeast(1)
            val render = renderPageBitmap ?: return@queueEvent
            if (currentPage + 1 < getPageCount()) bitmapCache.getOrRender(currentPage + 1, w, h, render)
            if (currentPage > 0) bitmapCache.getOrRender(currentPage - 1, w, h, render)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try { pageFlip.onSurfaceCreated() } catch (_: Exception) {}
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        try {
            pageFlip.onSurfaceChanged(width, height)
            uploadBitmap?.recycle()
            val page = pageFlip.firstPage ?: return
            val w = page.width().toInt().coerceAtLeast(1)
            val h = page.height().toInt().coerceAtLeast(1)
            uploadBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmapCache.clear()
        } catch (_: Exception) {}
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            drawLock.lock()
            val page = pageFlip.firstPage ?: return

            when (drawCommand) {
                DRAW_MOVING_FRAME -> {
                    ensureFlipTextures(page)
                    pageFlip.drawFlipFrame()
                }
                DRAW_ANIMATING_FRAME -> {
                    val stillAnimating = pageFlip.animating()
                    ensureFlipTextures(page)
                    if (stillAnimating) {
                        pageFlip.drawFlipFrame()
                    } else {
                        finalizeFlipOnGl(page)
                        drawCommand = DRAW_FULL_PAGE
                        ensureFirstTexture(page)
                        pageFlip.drawPageFrame()
                        preloadSecondTexture(page)
                        schedulePageNotify()
                    }
                }
                DRAW_FULL_PAGE -> {
                    ensureFirstTexture(page)
                    pageFlip.drawPageFrame()
                    preloadSecondTexture(page)
                    schedulePageNotify()
                }
            }

            pageFlip.deleteUnusedTextures()
        } finally {
            drawLock.unlock()
        }
    }

    private fun ensureFlipTextures(page: Page) {
        if (pageFlip.flipState == PageFlipState.FORWARD_FLIP) {
            if (!page.isSecondTextureSet) {
                blitPageIntoUpload(currentPage + 1)
                uploadBitmap?.let { page.setSecondTexture(it) }
            }
        } else if (!page.isFirstTextureSet) {
            currentPage = (currentPage - 1).coerceAtLeast(0)
            blitPageIntoUpload(currentPage)
            uploadBitmap?.let { page.setFirstTexture(it) }
        }
    }

    private fun ensureFirstTexture(page: Page) {
        if (!page.isFirstTextureSet) {
            blitPageIntoUpload(currentPage)
            uploadBitmap?.let { page.setFirstTexture(it) }
        }
    }

    /** GL 线程：动画结束，交换纹理 / 更新页码。 */
    private fun finalizeFlipOnGl(page: Page) {
        backwardPrepared = false
        when (pageFlip.flipState) {
            PageFlipState.END_WITH_FORWARD -> {
                page.setFirstTextureWithSecond()
                currentPage = (currentPage + 1).coerceAtMost(getPageCount() - 1)
                pendingPageNotify = currentPage
            }
            PageFlipState.END_WITH_BACKWARD -> {
                pendingPageNotify = currentPage
            }
            PageFlipState.END_WITH_RESTORE -> {
                pendingPageNotify = null
            }
            else -> Unit
        }
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun preloadSecondTexture(page: Page) {
        if (currentPage + 1 >= getPageCount()) return
        blitPageIntoUpload(currentPage + 1)
        uploadBitmap?.let { page.setSecondTexture(it) }
    }

    private fun schedulePageNotify() {
        val idx = pendingPageNotify ?: return
        pendingPageNotify = null
        mainHandler.post { onPageChanged?.invoke(idx) }
    }

    private fun blitPageIntoUpload(idx: Int) {
        val target = uploadBitmap ?: return
        val render = renderPageBitmap ?: return
        val src = bitmapCache.getOrRender(idx, target.width, target.height, render) ?: return
        if (src !== target) {
            android.graphics.Canvas(target).drawBitmap(src, 0f, 0f, null)
        }
    }

    fun jumpToPage(idx: Int) {
        try {
            drawLock.lock()
            currentPage = idx.coerceIn(0, (getPageCount() - 1).coerceAtLeast(0))
            pendingPageNotify = null
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
        } finally {
            drawLock.unlock()
        }
    }

    fun invalidateAllTextures() {
        bitmapCache.clear()
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
    }
}
