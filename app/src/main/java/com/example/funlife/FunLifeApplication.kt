// FunLifeApplication.kt - 应用程序类
package com.example.funlife

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.funlife.utils.AuditLogger
import com.example.funlife.security.SecurityInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FunLifeApplication : Application(), ImageLoaderFactory {

    private val isSupertuxEngineProcess: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()?.endsWith(":supertux") == true
        } else {
            false
        }
    
    // 数据库实例
    val database: com.example.funlife.data.database.AppDatabase by lazy {
        com.example.funlife.data.database.AppDatabase.getDatabase(this)
    }
    
    override fun onCreate() {
        super.onCreate()

        // SuperTux 独立进程只需最小初始化，避免 5～6 秒黑屏等待主 App 逻辑
        if (isSupertuxEngineProcess) {
            com.example.funlife.utils.CrashHandler.install(this)
            android.util.Log.i("FunLifeApplication", "Lite init for :supertux engine process")
            return
        }

        com.example.funlife.utils.UserAvatarBitmapCache.install(this)
        com.example.funlife.resource.ResourceStore.init(this)
        com.example.funlife.game.platformer.catalog.PlatformerUnlockProgress.init(this)
        com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAssetCache.ensureLoaded(this)

        // 🛡️ 全局崩溃兜底：必须最早安装，才能捕获后续初始化中的异常
        com.example.funlife.utils.CrashHandler.install(this)

        // � Debug 包：开启 StrictMode 检测主线程磁盘读写 / 网络
        // Release 自动跳过，不影响性能与用户体验
        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()  // 只在 logcat 警告，不崩溃
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // �🔒 初始化安全系统（优先级最高）
        SecurityInitializer.initialize(this)

        // 🔒 应用签名自校验：防重打包（仅在 BuildConfig.APP_SIGN_SHA256 已配置时生效）
        try {
            com.example.funlife.security.AppSignatureGuard.verify(this)
        } catch (e: Exception) {
            android.util.Log.w("FunLifeApplication", "AppSignatureGuard verify 失败", e)
        }

        // 🔒 防时间回拨：启动时把系统时间喂入时钟（仅当大于历史最大才更新）
        try {
            com.example.funlife.security.MonotonicClock.get(this).bootstrap()
        } catch (e: Exception) {
            android.util.Log.w("FunLifeApplication", "MonotonicClock bootstrap 失败", e)
        }
        
        // 初始化审计日志系统
        AuditLogger.initialize(this)

        // 🔄 VIP 运行时配置：缓存加载 + 异步刷新都放到 IO 线程
        //   （loadFromCache 走 SharedPreferences + JSON 解析，主线程会触发 StrictMode）
        try {
            com.example.funlife.vip.VipRuntimeConfig.bootstrapAsync(this)
        } catch (e: Exception) {
            android.util.Log.w("FunLifeApplication", "VipRuntimeConfig 初始化失败", e)
        }

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { com.example.funlife.resource.ResourceStore.syncAndPrefetchOnLaunch() }
                .onFailure { android.util.Log.w("FunLifeApplication", "ResourceStore prefetch failed", it) }
            runCatching {
                if (com.example.funlife.resource.ResourceStore.isBundleReadyAsync("pac_maze_skins")) {
                    com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache.warmCoverCacheAsync()
                }
            }.onFailure { android.util.Log.w("FunLifeApplication", "Cover cache warm failed", it) }
        }

        // 🔄 进程级前台监听：App 从后台回到前台时刷新 VIP 配置
        //   节流交给 VipRuntimeConfig 内部 30s 控制，这里只是触发
        try {
            com.example.funlife.notifications.SocialAlertBus.installProcessObserver()
            warmCurrentUserAvatarAsync()
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : androidx.lifecycle.DefaultLifecycleObserver {
                    override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                        com.example.funlife.vip.VipRuntimeConfig.refreshAsync(applicationContext)
                        com.example.funlife.social.SocialSessionManager.warmStartAsync(applicationContext)
                        com.example.funlife.social.SocialPresenceManager.onAppForeground(applicationContext)
                        com.example.funlife.social.SocialForegroundPoller.onAppForeground(applicationContext)
                        com.example.funlife.social.game.GameRoomForegroundSync.onAppForeground(applicationContext)
                        com.example.funlife.resource.PacMazeResourceUpdateNotifier
                            .onAppForeground(applicationContext)
                        warmCurrentUserAvatarAsync()
                    }

                    override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                        com.example.funlife.social.SocialPresenceManager.onAppBackground(applicationContext)
                        com.example.funlife.social.SocialForegroundPoller.onAppBackground()
                        com.example.funlife.social.game.GameRoomForegroundSync.onAppBackground()
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.w("FunLifeApplication", "ProcessLifecycleOwner 注册失败", e)
        }
        
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
    
    // 🚀 系统内存压力时主动释放图片缓存，防止 OOM
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                com.example.funlife.utils.ImageCache.clear()
                com.example.funlife.utils.UserAvatarBitmapCache.clear()
            }
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_BACKGROUND -> {
                com.example.funlife.utils.ImageCache.trim()
                com.example.funlife.utils.UserAvatarBitmapCache.trim()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        com.example.funlife.utils.ImageCache.clear()
        com.example.funlife.utils.UserAvatarBitmapCache.clear()
    }

    private fun warmCurrentUserAvatarAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val userId = com.example.funlife.utils.UserSessionManager(applicationContext).getCurrentUserId()
                if (userId > 0L) {
                    com.example.funlife.utils.UserAvatarBitmapCache.warmUser(applicationContext, userId)
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
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
            .crossfade(false)
            .logger(DebugLogger())
            .build()
    }
}
