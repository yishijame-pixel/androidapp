package com.example.funlife.resource

import android.content.Context
import com.example.funlife.ui.screens.pacmaze.PacMazeSfx
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 豆人迷宫资源更新提醒：启动/回大厅时检测 manifest 与本地 bundle 校验，
 * 有缺口时在 UI 展示横幅，用户可一键同步。
 */
object PacMazeResourceUpdateNotifier {

    private const val PREFS = "resource_store"
    private const val KEY_LAST_SEEN_MANIFEST = "pac_maze_last_seen_manifest"
    private const val KEY_DISMISSED_UPDATE = "pac_maze_dismissed_update_key"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMutex = Mutex()
    @Volatile private var applying = false

    private val _notice = MutableStateFlow<PacMazeResourceUpdateStatus?>(null)
    val notice: StateFlow<PacMazeResourceUpdateStatus?> = _notice.asStateFlow()

    fun refreshAsync() {
        scope.launch { refresh() }
    }

    suspend fun refresh(): PacMazeResourceUpdateStatus = jobMutex.withLock {
        if (applying) return@withLock _notice.value ?: PacMazeResourceUpdateStatus()
        val status = ResourceStore.checkPacMazeResourceUpdates()
        val dismissedKey = prefs().getString(KEY_DISMISSED_UPDATE, null)
        val currentKey = updateNoticeKey(status)
        _notice.value = status.takeIf { it.hasPending && currentKey != dismissedKey }
        status
    }

    fun dismissCurrent() {
        if (applying) return
        val pending = _notice.value?.pendingBundleIds.orEmpty()
        val status = _notice.value
        if (status != null && pending.isNotEmpty()) {
            prefs().edit()
                .putString(KEY_DISMISSED_UPDATE, updateNoticeKey(status))
                .apply()
        }
        _notice.value = null
    }

    private fun prefs() = ResourceStore.prefsAccessor()

    suspend fun applyPendingUpdates(
        context: Context? = null,
        onProgress: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
    ): BundleEnsureResult = jobMutex.withLock {
        applying = true
        try {
            val pending = ResourceStore.checkPacMazeResourceUpdates().pendingBundleIds
            if (pending.isEmpty()) {
                markManifestSeen(ResourceStore.lastFetchedManifestVersion())
                _notice.value = null
                return@withLock BundleEnsureResult.Success
            }
            val result = ResourceStore.ensurePacMazeBootBundles(
                bundleIds = pending,
                onProgress = onProgress,
            )
            if (!result.isSuccess) return@withLock result

            if (pending.any { it == PacMazeResourceBundles.SKINS }) {
                PacMazeRemoteSkinAnimCache.invalidateAllCaches()
            }
            if (context != null && pending.any { it == PacMazeResourceBundles.SFX }) {
                val sfxOk = runCatching {
                    PacMazeSfx.ensureReady(context.applicationContext)
                }.getOrDefault(false)
                PacMazeSfx.reloadAfterBundleUpdate(context.applicationContext)
                if (!sfxOk) {
                    return@withLock BundleEnsureResult.Failed(
                        BundleEnsureFailure.VALIDATION_FAILED,
                        "音效包已下载，但初始化失败，请重试",
                    )
                }
            }
            markManifestSeen(ResourceStore.lastFetchedManifestVersion())
            _notice.value = null
            result
        } finally {
            applying = false
        }
    }

    private fun updateNoticeKey(status: PacMazeResourceUpdateStatus): String =
        "${status.manifestVersion}:${status.pendingBundleIds.sorted().joinToString(",")}"

    fun markManifestSeen(version: Int) {
        if (version <= 0) return
        ResourceStore.prefsAccessor().edit().putInt(KEY_LAST_SEEN_MANIFEST, version).apply()
        if (_notice.value?.hasPending != true) {
            _notice.value = null
        }
    }

    fun lastSeenManifestVersion(): Int =
        ResourceStore.prefsAccessor().getInt(KEY_LAST_SEEN_MANIFEST, 0)
}
