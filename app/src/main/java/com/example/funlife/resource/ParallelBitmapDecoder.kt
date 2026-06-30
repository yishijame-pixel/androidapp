package com.example.funlife.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 并行 PNG/WebP 解码 + 可选 inBitmap 复用，降低磁盘缓存命中后的 CPU 耗时。
 */
object ParallelBitmapDecoder {

    private const val DEFAULT_WORKERS = 4
    private const val BATCH_WORKERS = 6
    private const val POOL_MAX = 8

    private val bitmapPool = ConcurrentLinkedQueue<Bitmap>()

    @Volatile
    private var workerBoost: Int? = null

    /** 批量预热期间提高并行度（首次 decode 写盘）。 */
    fun batchWorkers(): Int = workerBoost ?: BATCH_WORKERS.coerceAtMost(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(4),
    )

    suspend fun <T> withBatchBoost(block: suspend () -> T): T {
        workerBoost = batchWorkers()
        try {
            return block()
        } finally {
            workerBoost = null
        }
    }

    private fun effectiveWorkers(requested: Int): Int =
        (workerBoost ?: requested).coerceIn(1, 8)

    suspend fun decodeFilesParallel(
        files: List<File>,
        workers: Int = DEFAULT_WORKERS,
        onProgress: ((loaded: Int, total: Int) -> Unit)? = null,
    ): List<ImageBitmap> = withContext(Dispatchers.IO) {
        if (files.isEmpty()) return@withContext emptyList()
        val total = files.size
        val semaphore = Semaphore(effectiveWorkers(workers))
        coroutineScope {
            files.mapIndexed { index, file ->
                async {
                    semaphore.withPermit {
                        val bmp = decodeFileToBitmap(file) ?: return@withPermit null
                        onProgress?.invoke(index + 1, total)
                        bmp.asImageBitmap()
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    fun decodeFileToBitmap(file: File): Bitmap? {
        val reuse = pollReusableBitmap(file)
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
            if (reuse != null) {
                inMutable = true
                inBitmap = reuse
            }
        }
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()?.also { decoded ->
            if (reuse != null && reuse !== decoded && !reuse.isRecycled) {
                reuse.recycle()
            }
        } ?: run {
            if (reuse != null && !reuse.isRecycled) reuse.recycle()
            null
        }
    }

    fun offerBitmapForReuse(bitmap: Bitmap) {
        if (bitmapPool.size >= POOL_MAX) return
        if (!bitmap.isMutable || bitmap.isRecycled) return
        bitmapPool.offer(bitmap)
    }

    fun clearPool() {
        while (true) {
            val b = bitmapPool.poll() ?: break
            if (!b.isRecycled) b.recycle()
        }
    }

    private fun pollReusableBitmap(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        val it = bitmapPool.iterator()
        while (it.hasNext()) {
            val candidate = it.next()
            if (candidate.isRecycled) {
                it.remove()
                continue
            }
            if (candidate.width == w && candidate.height == h && candidate.isMutable) {
                it.remove()
                return candidate
            }
        }
        return null
    }
}
