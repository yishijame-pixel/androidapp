package com.example.funlife.utils

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

import com.example.funlife.resource.ResourceStore

object ImageCache {
    // 🔥 LruCache：自动淘汰最少使用的图，防止内存累积导致 OOM
    // 上限 = min(应用可用内存的 1/8, 48MB)，对中低端设备友好
    private val maxBytes: Int by lazy {
        val runtimeMax = (Runtime.getRuntime().maxMemory() / 8).toInt()
        runtimeMax.coerceAtMost(48 * 1024 * 1024).coerceAtLeast(16 * 1024 * 1024)
    }

    private val cache = object : android.util.LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            // ARGB_8888 → 每像素 4 字节
            return value.width * value.height * 4
        }
    }

    /** 全分辨率加载（兼容旧调用） */
    fun loadImage(context: Context, assetPath: String): ImageBitmap? =
        loadImage(context, assetPath, sampleSize = 1)

    /**
     * 🚀 性能优化版：支持降采样
     * @param sampleSize 2 = 解码为 1/2 宽高 = 1/4 内存；4 = 1/16 内存
     * 列表/网格小图标建议 sampleSize=2 或 4，足够清晰且大幅降低 GPU/内存压力
     */
    fun loadImage(context: Context, assetPath: String, sampleSize: Int): ImageBitmap? {
        val key = if (sampleSize <= 1) assetPath else "$assetPath@$sampleSize"
        cache.get(key)?.let { return it }
        return try {
            ResourceStore.openInputStream(assetPath)?.use { input ->
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(input, null, opts)?.asImageBitmap()?.also {
                    cache.put(key, it)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        cache.evictAll()
    }

    /** 应对 onLowMemory / onTrimMemory 调用，释放一半缓存 */
    fun trim() {
        cache.trimToSize(cache.size() / 2)
    }
}
