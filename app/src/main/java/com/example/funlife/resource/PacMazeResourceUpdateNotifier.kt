package com.example.funlife.resource

import android.content.Context
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.PlatformerResourcePrewarmCoordinator
import com.example.funlife.ui.screens.pacmaze.PacMazeSfx
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.platformer.PlatformerBootCache
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
 * 游戏资源更新提醒：启动/回前台/首页横幅时检测本地与 manifest，
 * 有缺口时自动下载；完成后触发横版动画企业级后台预热。
 */
object PacMazeResourceUpdateNotifier {

    private const val PREFS = "resource_store"
    private const val KEY_LAST_SEEN_MANIFEST = "pac_maze_last_seen_manifest"
    private const val KEY_DISMISSED_UPDATE = "pac_maze_dismissed_update_key"
    const val ERROR_RETRY_DELAY_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobMutex = Mutex()
    @Volatile private var applying = false
    @Volatile private var progressEpoch = 0

    private val _notice = MutableStateFlow<PacMazeResourceUpdateStatus?>(null)
    val notice: StateFlow<PacMazeResourceUpdateStatus?> = _notice.asStateFlow()

    private val _syncUi = MutableStateFlow(GameResourceSyncUiState())
    val syncUi: StateFlow<GameResourceSyncUiState> = _syncUi.asStateFlow()

    fun refreshAsync() {
        scope.launch { refresh() }
    }

    suspend fun refresh(): PacMazeResourceUpdateStatus = jobMutex.withLock {
        if (applying) return@withLock _notice.value ?: PacMazeResourceUpdateStatus()
        val dismissedKey = prefs().getString(KEY_DISMISSED_UPDATE, null)

        val localPending = ResourceStore.localGameResourceStatus().pendingBundleIds
        if (localPending.isNotEmpty()) {
            val localStatus = PacMazeResourceUpdateStatus(
                manifestVersion = ResourceStore.lastFetchedManifestVersion(),
                pendingBundleIds = localPending,
            )
            if (updateNoticeKey(localStatus) != dismissedKey) {
                _notice.value = localStatus
            }
        }

        val status = ResourceStore.checkPacMazeResourceUpdates()
        val currentKey = updateNoticeKey(status)
        _notice.value = when {
            status.hasPending && currentKey != dismissedKey -> status
            !status.hasPending -> null
            else -> _notice.value
        }
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
        dismissDownloadBanner()
    }

    private fun prefs() = ResourceStore.prefsAccessor()

    suspend fun autoApplyPendingUpdates(context: Context) {
        if (applying || _syncUi.value.isSyncing) return
        val pending = _notice.value?.pendingBundleIds?.takeIf { it.isNotEmpty() }
            ?: ResourceStore.localGameResourceStatus().pendingBundleIds
        if (pending.isEmpty()) return
        if (pending.all { ResourceStore.isPacMazeBundleReady(it) }) {
            reconcileLocallyReady()
            return
        }
        applyPendingUpdates(context) { _, _, _ -> }
    }

    /** App 回前台：先 refresh，仅真正缺文件时才走横幅下载。 */
    fun onAppForeground(context: Context) {
        scope.launch { onAppForegroundInternal(context.applicationContext) }
    }

    private suspend fun onAppForegroundInternal(context: Context) {
        if (applying || _syncUi.value.isSyncing) return
        val status = refresh()
        if (!status.hasPending) return
        if (status.pendingBundleIds.all { ResourceStore.isPacMazeBundleReady(it) }) {
            dismissDownloadBanner()
            ResourceStore.clearActiveDownloadProgress()
            markManifestSeen(ResourceStore.lastFetchedManifestVersion())
            return
        }
        autoApplyPendingUpdates(context)
    }

    fun clearSyncError() {
        if (_syncUi.value.errorMessage.isNullOrBlank()) return
        _syncUi.value = _syncUi.value.copy(errorMessage = null)
    }

    fun retryPendingUpdatesAsync(context: Context) {
        clearSyncError()
        autoApplyPendingUpdatesAsync(context.applicationContext)
    }

    fun autoApplyPendingUpdatesAsync(context: Context) {
        scope.launch { autoApplyPendingUpdates(context.applicationContext) }
    }

    /** 本地 bundle 已就绪但 manifest 元数据未同步时，静默标记已读，不弹下载横幅。 */
    fun reconcileLocallyReadyAsync() {
        scope.launch { reconcileLocallyReady() }
    }

    suspend fun reconcileLocallyReady() {
        if (applying || _syncUi.value.isSyncing) return
        val status = refresh()
        if (!status.hasPending) {
            dismissDownloadBanner()
            ResourceStore.clearActiveDownloadProgress()
        }
    }

    suspend fun applyPendingUpdates(
        context: Context? = null,
        onProgress: (bundleId: String, progress: BundleLoadProgress, overallPercent: Int) -> Unit,
    ): BundleEnsureResult = jobMutex.withLock {
        applying = true
        val epoch = ++progressEpoch
        val appCtx = context?.applicationContext
        try {
            val pending = ResourceStore.checkPacMazeResourceUpdates().pendingBundleIds
            if (pending.isEmpty()) {
                markManifestSeen(ResourceStore.lastFetchedManifestVersion())
                dismissDownloadBanner(epoch)
                return@withLock BundleEnsureResult.Success
            }
            if (pending.all { ResourceStore.isPacMazeBundleReady(it) }) {
                markManifestSeen(ResourceStore.lastFetchedManifestVersion())
                dismissDownloadBanner(epoch)
                return@withLock BundleEnsureResult.Success
            }
            _syncUi.value = GameResourceSyncUiState(
                isSyncing = true,
                overallPercent = 2,
                statusMessage = "检查资源清单…",
            )
            val pendingSnapshot = pending.toList()
            var bannerFinished = false
            ResourceStore.beginGlobalDownloadUi()
            val result = try {
                ResourceStore.ensurePacMazeBootBundles(
                bundleIds = pendingSnapshot,
                onProgress = { bundleId, progress, overall ->
                    if (epoch != progressEpoch) return@ensurePacMazeBootBundles
                    if (overall >= 100) {
                        if (!bannerFinished) {
                            bannerFinished = true
                            finishDownloadUi(epoch)
                        }
                        return@ensurePacMazeBootBundles
                    }
                    val phaseLabel = ActiveBundleDownloadProgress(bundleId, progress.phase, progress.percent).label
                    _syncUi.value = GameResourceSyncUiState(
                        isSyncing = true,
                        overallPercent = overall.coerceAtLeast(1),
                        activeBundleId = bundleId,
                        statusMessage = phaseLabel,
                    )
                    onProgress(bundleId, progress, overall)
                },
                )
            } finally {
                ResourceStore.endGlobalDownloadUi()
            }
            if (!result.isSuccess) {
                failDownloadUi(epoch)
                val message = result.userMessage().ifBlank { "更新失败，请检查网络" }
                _syncUi.value = GameResourceSyncUiState(
                    isSyncing = false,
                    errorMessage = message,
                )
                appCtx?.let { GameResourceDownloadNotifier.notifyFailure(it, message) }
                return@withLock result
            }

            if (!bannerFinished) {
                finishDownloadUi(epoch)
            }
            markManifestSeen(ResourceStore.lastFetchedManifestVersion())
            appCtx?.let { GameResourceDownloadNotifier.notifySuccess(it, pendingSnapshot) }

            if (appCtx != null) {
                scope.launch {
                    runPostDownloadTasks(appCtx, pendingSnapshot)
                }
            }
            result
        } catch (t: Throwable) {
            failDownloadUi(epoch)
            val message = t.message?.ifBlank { null } ?: "更新失败，请重试"
            _syncUi.value = GameResourceSyncUiState(
                isSyncing = false,
                errorMessage = message,
            )
            appCtx?.let { GameResourceDownloadNotifier.notifyFailure(it, message) }
            BundleEnsureResult.Failed(BundleEnsureFailure.DOWNLOAD_FAILED, t.message)
        } finally {
            applying = false
        }
    }

    private suspend fun runPostDownloadTasks(context: Context, pending: List<String>) {
        if (pending.any { it == PacMazeResourceBundles.SKINS }) {
            PacMazeRemoteSkinAnimCache.invalidateAllCaches()
            PlatformerBootCache.resetPrewarmSession()
            PlatformerResourcePrewarmCoordinator.reset()
            PlatformerDecodeStampStore.clear(context)
        }
        if (pending.any { it == GameResourceBundles.PLATFORMER }) {
            PlatformerRemoteAnimCache.invalidateDiskCache()
            PlatformerBootCache.resetPrewarmSession()
            PlatformerResourcePrewarmCoordinator.reset()
            PlatformerDecodeStampStore.clear(context)
        }
        if (pending.any { it == PacMazeResourceBundles.SFX }) {
            val sfxOk = runCatching { PacMazeSfx.ensureReady(context) }.getOrDefault(false)
            PacMazeSfx.reloadAfterBundleUpdate(context)
            if (!sfxOk) {
                val message = "音效包已下载，但初始化失败，请重试"
                _syncUi.value = GameResourceSyncUiState(
                    isSyncing = false,
                    errorMessage = message,
                )
                GameResourceDownloadNotifier.notifyFailure(context, message)
                return
            }
        }
        PlatformerResourcePrewarmCoordinator.scheduleAfterBundlesReady(context)
    }

    private fun finishDownloadUi(expectedEpoch: Int) {
        if (expectedEpoch != progressEpoch) return
        progressEpoch++
        ResourceStore.clearActiveDownloadProgress()
        _notice.value = null
        _syncUi.value = GameResourceSyncUiState()
    }

    private fun failDownloadUi(expectedEpoch: Int) {
        if (expectedEpoch != progressEpoch) return
        progressEpoch++
        ResourceStore.clearActiveDownloadProgress()
    }

    private fun dismissDownloadBanner(expectedEpoch: Int? = null) {
        if (expectedEpoch != null && expectedEpoch != progressEpoch) return
        ResourceStore.clearActiveDownloadProgress()
        _notice.value = null
        _syncUi.value = GameResourceSyncUiState()
    }

    private fun updateNoticeKey(status: PacMazeResourceUpdateStatus): String {
        val skinsBv = ResourceStore.requiredBundleVersion("pac_maze_skins")
            ?: ResourceStore.PAC_MAZE_SKINS_BUNDLE_VERSION
        return "${status.manifestVersion}:skins_bv$skinsBv:" +
            status.pendingBundleIds.sorted().joinToString(",")
    }

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
