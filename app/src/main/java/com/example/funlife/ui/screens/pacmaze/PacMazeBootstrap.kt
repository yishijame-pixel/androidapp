package com.example.funlife.ui.screens.pacmaze

import android.content.Context
import com.example.funlife.BuildConfig
import com.example.funlife.resource.BundleEnsureFailure
import com.example.funlife.resource.BundleEnsureResult
import com.example.funlife.resource.BundleLoadProgress
import com.example.funlife.resource.PacMazeResourceBundles
import com.example.funlife.resource.PacMazeResourceUpdateNotifier
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAssetCache
import kotlinx.coroutines.delay

enum class PacMazeBootStatus {
    LOADING,
    READY,
    FAILED,
}

data class PacMazeBootUi(
    val phase: String = "正在初始化…",
    val subtitle: String? = null,
    val percent: Int = 0,
    val status: PacMazeBootStatus = PacMazeBootStatus.LOADING,
    val errorMessage: String? = null,
    val audioReady: Boolean = false,
    /** 当前正在同步的 bundle（皮肤 / 音效） */
    val activeBundleId: String? = null,
    val resourcesReady: Boolean = false,
)

sealed class PacMazeBootResult {
    data class Success(
        val audioReady: Boolean,
        val resourcesReady: Boolean,
    ) : PacMazeBootResult()

    data class Failed(val message: String) : PacMazeBootResult()
}

object PacMazeBootstrap {

    suspend fun run(
        context: Context,
        onUpdate: (PacMazeBootUi) -> Unit,
    ): PacMazeBootResult {
        fun emit(
            phase: String,
            percent: Int,
            subtitle: String? = null,
            activeBundleId: String? = null,
        ) {
            onUpdate(
                PacMazeBootUi(
                    phase = phase,
                    subtitle = subtitle,
                    percent = percent.coerceIn(0, 100),
                    status = PacMazeBootStatus.LOADING,
                    activeBundleId = activeBundleId,
                ),
            )
        }

        emit("检查游戏资源", 4, "正在读取资源清单…")
        PacMazeSkinAssetCache.ensureLoaded(context)

        val offlineBackend = BuildConfig.VIP_BACKEND_URL.isBlank()
        if (offlineBackend) {
            emit("离线模式", 20, "检测本地缓存…")
        }

        val skinsCached = ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SKINS)
        val sfxCached = ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SFX)

        if (offlineBackend) {
            val resourcesReady = skinsCached && sfxCached
            emit("加载音效", 88, "初始化本地音效…", activeBundleId = PacMazeResourceBundles.SFX)
            val bundleLoaded = runCatching { PacMazeSfx.ensureReady(context) }.getOrDefault(sfxCached)
            val diag = PacMazeSfx.diagnose(context)
            val audioReady = bundleLoaded && diag.menuBgmFound && diag.campaignBgmFound && diag.endlessBgmFound
            if (resourcesReady) {
                PacMazeResourceUpdateNotifier.markManifestSeen(ResourceStore.lastFetchedManifestVersion())
            }
            val hint = when {
                resourcesReady && audioReady -> "本地资源已就绪"
                resourcesReady -> "角色资源已就绪，音效可能不可用"
                sfxCached || skinsCached -> "部分资源可用，可进入大厅"
                else -> "未连接资源服务器，云端资源不可用"
            }
            emit("进入大厅", 100, hint)
            delay(280L)
            return PacMazeBootResult.Success(audioReady = audioReady, resourcesReady = resourcesReady)
        }

        if (skinsCached && sfxCached) {
            emit("校验本地资源", 12, "皮肤与音效包已缓存…")
        }

        val bundlesOk = ResourceStore.ensurePacMazeBootBundles(
            bundleIds = PacMazeResourceBundles.bootOrder,
        ) { bundleId, progress, overall ->
            emit(
                phase = bootPhaseLabel(progress),
                percent = overall,
                subtitle = bootSubtitle(bundleId, progress),
                activeBundleId = bundleId,
            )
        }.isSuccess

        val resourcesReady = bundlesOk &&
            ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SKINS) &&
            ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SFX)

        if (!bundlesOk && !ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SKINS) &&
            !ResourceStore.isPacMazeBundleReady(PacMazeResourceBundles.SFX)
        ) {
            return PacMazeBootResult.Failed(
                "资源下载失败，请检查网络后重试",
            )
        }

        emit("加载音效", 92, "初始化 SoundPool 与背景音乐…", activeBundleId = PacMazeResourceBundles.SFX)
        val bundleLoaded = runCatching { PacMazeSfx.ensureReady(context) }.getOrDefault(sfxCached)
        val diag = PacMazeSfx.diagnose(context)
        val uiSoundsReady = !diag.uiClickPath.isNullOrBlank()
        val audioReady = bundleLoaded && diag.menuBgmFound && diag.campaignBgmFound && diag.endlessBgmFound
        if (!uiSoundsReady) {
            android.util.Log.w(
                "PacMazeBootstrap",
                "UI sounds missing (curated/ui); bundle will re-download on next ensureBundle",
            )
        }
        if (!audioReady) {
            android.util.Log.w(
                "PacMazeBootstrap",
                "BGM not ready: bundleLoaded=$bundleLoaded diag=${diag.summary} menu=${diag.menuBgmPath}",
            )
        }

        if (resourcesReady) {
            if (!skinsCached) {
                PacMazeRemoteSkinAnimCache.invalidateAllCaches()
            } else {
                PacMazeRemoteSkinAnimCache.warmCoverCacheAsync()
            }
            PacMazeResourceUpdateNotifier.markManifestSeen(ResourceStore.lastFetchedManifestVersion())
        }

        val readyHint = when {
            resourcesReady && audioReady && diag.bgmPlaying -> "角色与音效已就绪"
            resourcesReady && audioReady -> "资源已就绪，正在启动 BGM…"
            resourcesReady -> "角色资源已就绪，音效可能不可用"
            audioReady -> "音效已就绪，部分角色资源待同步"
            skinsCached || sfxCached -> "部分资源可用，可进入大厅"
            else -> "资源不完整，可稍后在大厅更新"
        }
        onUpdate(
            PacMazeBootUi(
                phase = "准备就绪",
                subtitle = readyHint,
                percent = 100,
                status = PacMazeBootStatus.LOADING,
                audioReady = audioReady,
                resourcesReady = resourcesReady,
            ),
        )
        delay(if (audioReady && resourcesReady) 520L else 320L)
        return PacMazeBootResult.Success(
            audioReady = audioReady,
            resourcesReady = resourcesReady,
        )
    }

    private fun bootPhaseLabel(progress: BundleLoadProgress): String = when (progress.phase) {
        "manifest" -> "检查游戏资源"
        "download" -> "下载游戏资源"
        "unzip" -> "解压资源包"
        "ready" -> "校验完成"
        else -> "同步资源"
    }

    private fun bootSubtitle(bundleId: String, progress: BundleLoadProgress): String {
        val name = PacMazeResourceBundles.displayName(bundleId)
        return when (progress.phase) {
            "download" -> "下载$name… ${progress.percent}%"
            "unzip" -> "解压$name…"
            "manifest" -> "同步云端清单…"
            "ready" -> "$name 已就绪"
            else -> name
        }
    }

    @Suppress("unused")
    private fun BundleEnsureResult.toUserMessage(): String = userMessage()
}
