// FunLifeApplication.kt - 应用程序类
package com.example.funlife

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.funlife.utils.AuditLogger

class FunLifeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化审计日志系统
        AuditLogger.initialize(this)
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}
