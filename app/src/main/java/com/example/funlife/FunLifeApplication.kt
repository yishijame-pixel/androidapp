// FunLifeApplication.kt - 应用程序类
package com.example.funlife

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.funlife.utils.AuditLogger
import com.example.funlife.security.SecurityInitializer

class FunLifeApplication : Application(), ImageLoaderFactory {
    
    // 数据库实例
    val database: com.example.funlife.data.database.AppDatabase by lazy {
        com.example.funlife.data.database.AppDatabase.getDatabase(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 🔒 初始化安全系统（优先级最高）
        SecurityInitializer.initialize(this)
        
        // 初始化审计日志系统
        AuditLogger.initialize(this)
        
        // 执行安全自检（可选，仅在调试模式下）
        try {
            // 检查是否为调试模式
            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebug) {
                val checkResult = SecurityInitializer.performSecurityCheck(this)
                android.util.Log.d("FunLifeApplication", checkResult.getSummary())
            }
        } catch (e: Exception) {
            android.util.Log.e("FunLifeApplication", "安全自检失败", e)
        }
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
