package com.example.funlife.game.platformer.catalog

import android.content.Context
import com.example.funlife.resource.GameResourceBundles
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.platformer.PlatformerBootCache

/** 横版 catalog 启动服务：加载目录、注册动画、触发企业级预热。 */
object PlatformerAssetService {

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            ResourceStore.init(context)
            PlatformerUnlockProgress.init(context)
            PlatformerContentCatalog.load(force = true)
            PlatformerRemoteAnimCache.bootstrapFromCatalog()
            initialized = true
        }
    }

    /** 后台预热：zip 就绪后 decode 动画写磁盘，进横版秒开。 */
    fun prewarmAsync(context: Context) {
        ensureInitialized(context)
        PlatformerBootCache.startPrewarm(context)
    }

    suspend fun ensureBundles() {
        if (!ResourceStore.isPacMazeBundleReady(GameResourceBundles.PLATFORMER)) {
            ResourceStore.ensureBundle(GameResourceBundles.PLATFORMER)
        }
    }
}
