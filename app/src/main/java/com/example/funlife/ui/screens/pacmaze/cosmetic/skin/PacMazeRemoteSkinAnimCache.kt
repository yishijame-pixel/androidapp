package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.resource.BundleEnsureResult
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 云端皮肤资源管理：
 * - preview.png：网格封面，秒开
 * - 完整序列帧：选中/局内 lazy load，原图 decode，磁盘 + 内存 LRU
 */
object PacMazeRemoteSkinAnimCache {

    private const val TAG = "PacMazeRemoteSkinAnim"
    private const val DECODE_QUALITY_TAG = "trim_v6"
    /** 网格封面 decode 采样（更小更快；详情/局内仍用原图） */
    private const val COVER_DECODE_SAMPLE = 3
    /** 内存中同时保留完整动画的角色数（当前选中 + 上一个，减少来回切详情重解码） */
    private const val MAX_FULL_ANIM_SKINS = 2
    /** 同时解码封面数量，避免选角页多格并行 OOM */
    private const val MAX_CONCURRENT_COVER_DECODES = 2

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clipCache = mutableMapOf<PacMazeSkinId, MutableMap<PacMazeSkinAnimClip, List<ImageBitmap>>>()
    private val coverCache = mutableMapOf<PacMazeSkinId, ImageBitmap>()
    private val clipMutexes = ConcurrentHashMap<PacMazeSkinId, Mutex>()
    private val coverDecodeSemaphore = Semaphore(MAX_CONCURRENT_COVER_DECODES)
    private val coverJobs = mutableSetOf<PacMazeSkinId>()
    private val preloadJobs = mutableSetOf<PacMazeSkinId>()
    private val clipLoadJobs = ConcurrentHashMap.newKeySet<Pair<PacMazeSkinId, PacMazeSkinAnimClip>>()
    private var activeFullAnimSkin: PacMazeSkinId? = null

    private val _status = MutableStateFlow<Map<PacMazeSkinId, RemoteSkinLoadStatus>>(emptyMap())
    val status: StateFlow<Map<PacMazeSkinId, RemoteSkinLoadStatus>> = _status.asStateFlow()

    fun isCoverReady(skinId: PacMazeSkinId): Boolean = cover(skinId) != null

    /** 封面是否已有缓存（内存或磁盘 decode 产物），用于 UI 避免重复转圈。 */
    fun hasCoverCache(skinId: PacMazeSkinId): Boolean = synchronized(this) {
        coverCache[skinId] != null || coverDiskFile(skinId).isFile
    }

    fun isReady(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val primary = config.primaryClip()
        if (frames(skinId, primary)?.isNotEmpty() == true) return true
        return hasClipOnDisk(skinId, primary)
    }

    fun isAnimInMemory(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        return frames(skinId, config.primaryClip())?.isNotEmpty() == true
    }

    fun cover(skinId: PacMazeSkinId): ImageBitmap? = synchronized(this) { coverCache[skinId] }

    /** 局内可播放的序列帧（含渐进加载的部分帧）。 */
    fun playbackFrames(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): List<ImageBitmap>? =
        frames(skinId, clip)?.takeIf { it.isNotEmpty() }

    fun playbackFrameCount(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Int =
        playbackFrames(skinId, clip)?.size ?: 0

    fun isPlaybackReady(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        return playbackFrameCount(skinId, config.primaryClip()) >= MIN_BOOTSTRAP_FRAMES
    }

    /** 进大厅/选角后后台预热 walk（不阻塞 UI）。 */
    fun requestGameplayWarmupAsync(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        scope.launch { runCatching { warmUpForGameplay(skinId) } }
    }

    /** 局内首帧兜底（仅读内存，不在渲染线程 decode）。 */
    fun peekSingleWalkFrame(skinId: PacMazeSkinId): ImageBitmap? {
        playbackFrames(skinId, PacMazeSkinAnimClip.WALK)?.firstOrNull()?.let { return it }
        synchronized(singleWalkFrameCache) {
            return singleWalkFrameCache[skinId]
        }
    }

    private suspend fun ensureSingleWalkFrame(skinId: PacMazeSkinId) {
        if (peekSingleWalkFrame(skinId) != null) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (PacMazeSkinAnimClip.WALK !in config.clips) return
        val path = "${config.assetRoot}/walk/walk_1.png"
        if (ResourceStore.resolveFile(path) == null) return
        val decoded = withContext(Dispatchers.IO) {
            decodeFrame(path, config.sampleSize)
        } ?: return
        PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, decoded, asDefault = true)
        synchronized(singleWalkFrameCache) {
            singleWalkFrameCache[skinId] = decoded
        }
    }

    private const val MIN_BOOTSTRAP_FRAMES = 4
    /** 同步解码帧数：够播 walk 即可进局，其余后台补齐。 */
    private const val BOOTSTRAP_FRAME_COUNT = MIN_BOOTSTRAP_FRAMES
    /** 并行 decode 批量，平衡首帧速度与卡顿。 */
    private const val DECODE_BATCH_SIZE = 6

    private val singleWalkFrameCache = mutableMapOf<PacMazeSkinId, ImageBitmap>()

    fun frames(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): List<ImageBitmap>? =
        synchronized(this) { clipCache[skinId]?.get(clip) }

    private fun publishClipFrames(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        frames: List<ImageBitmap>,
    ) {
        synchronized(this) {
            clipCache.getOrPut(skinId) { mutableMapOf() }[clip] = frames
        }
    }

    fun clearMemoryCache() {
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
            singleWalkFrameCache.clear()
            activeFullAnimSkin = null
        }
    }

    /** 皮肤包更新后：内存 + 磁盘解码缓存一并失效。 */
    fun invalidateAllCaches() {
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
            singleWalkFrameCache.clear()
            activeFullAnimSkin = null
        }
        PacMazeBitmapFeetAnchor.invalidateAll()
        ResourceStore.decodedSkinCacheRoot().deleteRecursively()
    }

    /** 裁边算法升级后清磁盘缓存，避免继续用未裁边的旧帧。 */
    fun invalidateDecodedDiskCache() {
        ResourceStore.decodedSkinCacheRoot().deleteRecursively()
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
        }
    }

    fun requestEnsureBundleAsync() {
        scope.launch { runCatching { ResourceStore.ensureBundle("pac_maze_skins") } }
    }

    fun requestPreloadAllCoversAsync() {
        scope.launch {
            hydrateAllCoversFromDisk()
            if (!ResourceStore.ensureBundle("pac_maze_skins")) return@launch
            PacMazeRemoteSkinAnimCatalog.remoteSkinIds.forEach { skinId ->
                if (!hasCoverCache(skinId)) {
                    runCatching { preloadCover(skinId) }
                }
            }
        }
    }

    /** App 启动 / 进大厅后：把磁盘封面灌回内存，二次进入 ikun 秒开。 */
    fun warmCoverCacheAsync() {
        scope.launch { hydrateAllCoversFromDisk() }
    }

    /**
     * 从磁盘 decode 缓存恢复封面到内存；命中时返回 true。
     * 应在 IO 线程调用。
     */
    fun hydrateCoverFromDisk(skinId: PacMazeSkinId): Boolean {
        synchronized(this) {
            coverCache[skinId]?.let { return true }
            loadCoverFromDisk(skinId)?.let { bitmap ->
                coverCache[skinId] = bitmap
                setStatus(skinId, coverReadyStatus())
                return true
            }
            return false
        }
    }

    fun hydrateAllCoversFromDisk(): Int {
        var hydrated = 0
        PacMazeRemoteSkinAnimCatalog.remoteSkinIds.forEach { skinId ->
            if (hydrateCoverFromDisk(skinId)) hydrated++
        }
        return hydrated
    }

    fun requestPreloadAsync(skinId: PacMazeSkinId) {
        if (isReady(skinId)) return
        scope.launch { runCatching { preloadForSkin(skinId) } }
    }

    /** 仅预载网格封面，不拉完整 walk 序列（省内存/CPU）。 */
    fun requestPreloadCoverAsync(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        if (isCoverReady(skinId)) return
        scope.launch { runCatching { preloadCover(skinId) } }
    }

    /** 进局前预热：walk_1 脚点 + 4 帧 bootstrap；不拉封面、不阻塞下载 bundle。 */
    suspend fun warmUpForGameplay(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (isPlaybackReady(skinId) && PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)) return
        retainFullAnimFor(skinId)
        val primary = config.primaryClip()
        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            requestEnsureBundleAsync()
            return
        }
        withContext(Dispatchers.IO) {
            runCatching { ensureSingleWalkFrame(skinId) }
        }
        ensureClip(skinId, primary)
    }

    fun hasWalkFrames(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val clip = if (PacMazeSkinAnimClip.WALK in config.clips) {
            PacMazeSkinAnimClip.WALK
        } else {
            config.primaryClip()
        }
        return frames(skinId, clip)?.isNotEmpty() == true
    }

    suspend fun preloadCover(skinId: PacMazeSkinId) {
        if (isCoverReady(skinId)) {
            setStatus(skinId, coverReadyStatus())
            return
        }
        if (hydrateCoverFromDisk(skinId)) return
        synchronized(coverJobs) {
            if (skinId in coverJobs) return
            coverJobs.add(skinId)
        }
        try {
            setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Downloading, 2, "加载封面…"))
            if (!ResourceStore.ensureBundle("pac_maze_skins")) {
                setStatus(skinId, failedStatus("资源包未就绪"))
                return
            }
            val path = PacMazeRemoteSkinAnimCatalog.resolvePreviewAssetPath(skinId)
            if (path == null) {
                setStatus(skinId, failedStatus("封面路径无效"))
                return
            }
            loadCoverFromDisk(skinId)?.let {
                coverCache[skinId] = it
                setStatus(skinId, coverReadyStatus())
                return
            }
            val bitmap = coverDecodeSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    runCatching { decodeFrame(path, COVER_DECODE_SAMPLE) }.getOrNull()
                }
            }
            if (bitmap == null) {
                setStatus(skinId, failedStatus("封面解码失败"))
                return
            }
            cacheCoverToDisk(skinId, bitmap)
            coverCache[skinId] = bitmap
            setStatus(skinId, coverReadyStatus())
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "cover OOM $skinId", oom)
            setStatus(skinId, failedStatus("内存不足，请稍后重试"))
        } finally {
            synchronized(coverJobs) { coverJobs.remove(skinId) }
        }
    }

    suspend fun preloadForSkin(skinId: PacMazeSkinId) {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (frames(skinId, config.primaryClip())?.isNotEmpty() == true) {
            setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
            return
        }
        val primary = config.primaryClip()
        if (hasClipOnDisk(skinId, primary)) {
            setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 88, "从本地缓存恢复…"))
            retainFullAnimFor(skinId)
            val restored = loadClip(config, primary)
            if (!restored.isNullOrEmpty()) {
                setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
                scope.launch { runCatching { loadRemainingClips(config, skinId, config.clips - primary) } }
                return
            }
        }
        synchronized(preloadJobs) {
            if (skinId in preloadJobs) return
            preloadJobs.add(skinId)
        }
        try {
            purgeLegacyDecodedCache()
            retainFullAnimFor(skinId)
            var attempt = 0
            while (attempt < 2 && !isAnimInMemory(skinId)) {
                attempt++
                if (attempt > 1) {
                    Log.w(TAG, "retry preload $skinId — clear decoded cache only")
                    synchronized(this) {
                        clipCache.remove(skinId)
                        coverCache.remove(skinId)
                    }
                    ResourceStore.decodedSkinCacheRoot().deleteRecursively()
                    if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
                        ResourceStore.invalidatePacMazeSkinsBundle()
                    }
                }
                if (!ensureBundleWithProgress(skinId)) return
                if (!loadPrimaryClipForPreview(config, skinId)) {
                    if (attempt >= 2) {
                        setStatus(skinId, failedStatus("动画资源不完整，请检查网络后重试"))
                    }
                }
            }
            if (isAnimInMemory(skinId)) {
                setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
            } else if (_status.value[skinId]?.phase != RemoteSkinLoadPhase.Failed) {
                setStatus(skinId, failedStatus("动画加载未完成"))
            }
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "full preload OOM $skinId", oom)
            setStatus(skinId, failedStatus("内存不足，请稍后重试"))
        } finally {
            synchronized(preloadJobs) { preloadJobs.remove(skinId) }
        }
    }

    suspend fun ensureClip(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): List<ImageBitmap>? {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return null
        if (!ResourceStore.ensureBundle("pac_maze_skins")) return null
        retainFullAnimFor(skinId)
        return loadClip(config, clip)
    }

    fun requestClipAsync(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        if (playbackFrameCount(skinId, clip) >= MIN_BOOTSTRAP_FRAMES) return
        val key = skinId to clip
        if (!clipLoadJobs.add(key)) return
        scope.launch {
            try {
                runCatching { ensureClip(skinId, clip) }
            } finally {
                clipLoadJobs.remove(key)
            }
        }
    }

    private fun coverReadyStatus() = RemoteSkinLoadStatus(
        RemoteSkinLoadPhase.Ready,
        100,
        "封面已就绪",
    )

    private fun failedStatus(message: String) = RemoteSkinLoadStatus(
        RemoteSkinLoadPhase.Failed,
        0,
        message,
    )

    private fun retainFullAnimFor(skinId: PacMazeSkinId) {
        val previous = activeFullAnimSkin
        if (previous == skinId) return
        val keep = setOfNotNull(skinId, previous).take(MAX_FULL_ANIM_SKINS).toSet()
        clipCache.keys.filter { it !in keep }.forEach { clipCache.remove(it) }
        activeFullAnimSkin = skinId
    }

    private fun hasClipOnDisk(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Boolean {
        val dir = diskClipDir(skinId, clip)
        if (!dir.isDirectory) return false
        return dir.listFiles()?.any { it.isFile && it.extension.equals("png", ignoreCase = true) } == true
    }

    private suspend fun ensureBundleWithProgress(skinId: PacMazeSkinId): Boolean {
        setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Downloading, 0, "连接资源服务器…"))
        val bundleOk = ResourceStore.ensureBundleResult("pac_maze_skins") { progress ->
            val (phase, pct, msg) = when (progress.phase) {
                "download" -> Triple(RemoteSkinLoadPhase.Downloading, progress.percent, "下载角色资源 ${progress.percent}%")
                "unzip" -> Triple(RemoteSkinLoadPhase.Decoding, 70 + progress.percent / 5, "解压资源包…")
                "manifest" -> Triple(RemoteSkinLoadPhase.Downloading, 5, "同步资源清单…")
                else -> Triple(RemoteSkinLoadPhase.Downloading, progress.percent, "同步资源…")
            }
            setStatus(skinId, RemoteSkinLoadStatus(phase, pct, msg))
        }
        if (bundleOk !is BundleEnsureResult.Success) {
            setStatus(
                skinId,
                RemoteSkinLoadStatus(
                    RemoteSkinLoadPhase.Failed,
                    0,
                    (bundleOk as? BundleEnsureResult.Failed)?.detail ?: "资源下载失败",
                ),
            )
            return false
        }
        return true
    }

    private suspend fun loadPrimaryClipForPreview(
        config: PacMazeRemoteSkinAnimConfig,
        skinId: PacMazeSkinId,
    ): Boolean {
        val primary = config.primaryClip()
        setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 84, "解析 ${primary.folder}…"))
        val loaded = loadClip(config, primary)
        if (loaded.isNullOrEmpty()) {
            Log.w(TAG, "missing primary clip ${config.assetRoot}/${primary.folder} for $skinId")
            return false
        }
        setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
        val remaining = config.clips.filter { it != primary }
        if (remaining.isNotEmpty()) {
            scope.launch {
                runCatching { loadRemainingClips(config, skinId, remaining) }
            }
        }
        return true
    }

    private suspend fun loadRemainingClips(
        config: PacMazeRemoteSkinAnimConfig,
        skinId: PacMazeSkinId,
        clips: Collection<PacMazeSkinAnimClip>,
    ) {
        clips.sortedWith(
            compareBy({ if (it == PacMazeSkinAnimClip.WALK) 0 else 1 }, { it.ordinal }),
        ).forEach { clip ->
            loadClip(config, clip)
        }
    }

    private fun purgeLegacyDecodedCache() {
        ResourceStore.decodedSkinCacheRoot().listFiles()?.forEach { entry ->
            if (entry.isDirectory && entry.name != DECODE_QUALITY_TAG) {
                entry.deleteRecursively()
            }
        }
    }

    private fun setStatus(skinId: PacMazeSkinId, value: RemoteSkinLoadStatus) {
        _status.update { it + (skinId to value) }
    }

    private suspend fun loadClip(
        config: PacMazeRemoteSkinAnimConfig,
        clip: PacMazeSkinAnimClip,
    ): List<ImageBitmap>? = clipMutexes.getOrPut(config.skinId) { Mutex() }.withLock {
        if (clip !in config.clips) return null

        val relDir = "${config.assetRoot}/${clip.folder}"
        val prefix = "${clip.prefix}_"
        val paths = discoverFramePaths(relDir, prefix)
        if (paths.isEmpty()) return null

        val existing = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (existing.size >= paths.size) return existing

        if (existing.isEmpty()) {
            loadClipFromDiskProgressive(config.skinId, clip)?.let { fromDisk ->
                if (fromDisk.size >= paths.size) return fromDisk
            }
        }

        val merged = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (merged.size >= paths.size) return merged

        val startIndex = merged.size
        val pendingPaths = paths.drop(startIndex)
        if (pendingPaths.isEmpty()) return merged

        val bootstrapPaths = pendingPaths.take(BOOTSTRAP_FRAME_COUNT)
        val bootstrap = withContext(Dispatchers.IO) {
            decodePathsBatched(config.skinId, bootstrapPaths, config.sampleSize, registerFeet = clip == PacMazeSkinAnimClip.WALK)
        }
        if (bootstrap.isEmpty()) return merged.takeIf { it.isNotEmpty() }

        val afterBootstrap = merged + bootstrap
        publishClipFrames(config.skinId, clip, afterBootstrap)

        val remainingPaths = pendingPaths.drop(BOOTSTRAP_FRAME_COUNT)
        if (remainingPaths.isEmpty()) {
            scheduleSaveClipToDisk(afterBootstrap, config.skinId, clip)
            return afterBootstrap
        }

        scope.launch {
            runCatching {
                clipMutexes.getOrPut(config.skinId) { Mutex() }.withLock {
                    val current = synchronized(this@PacMazeRemoteSkinAnimCache) {
                        clipCache[config.skinId]?.get(clip)
                    }.orEmpty()
                    if (current.size >= paths.size) return@withLock
                    val stillPending = paths.drop(current.size)
                    if (stillPending.isEmpty()) return@withLock
                    val remaining = withContext(Dispatchers.IO) {
                        decodePathsBatched(
                            skinId = config.skinId,
                            paths = stillPending,
                            sampleSize = config.sampleSize,
                            registerFeet = clip == PacMazeSkinAnimClip.WALK,
                        )
                    }
                    if (remaining.isEmpty()) return@withLock
                    val full = current + remaining
                    publishClipFrames(config.skinId, clip, full)
                    scheduleSaveClipToDisk(full, config.skinId, clip)
                }
            }
        }
        return afterBootstrap
    }

    private fun scheduleSaveClipToDisk(frames: List<ImageBitmap>, skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        scope.launch {
            runCatching { saveClipToDisk(frames, skinId, clip) }
        }
    }

    private suspend fun decodePathsBatched(
        skinId: PacMazeSkinId,
        paths: List<String>,
        sampleSize: Int,
        registerFeet: Boolean,
    ): List<ImageBitmap> {
        if (paths.isEmpty()) return emptyList()
        val out = ArrayList<ImageBitmap>(paths.size)
        for (chunk in paths.chunked(DECODE_BATCH_SIZE)) {
            chunk.forEach { path ->
                decodeFrame(path, sampleSize)?.let { frame ->
                    if (registerFeet) {
                        PacMazeBitmapFeetAnchor.registerGameplayAnchor(
                            skinId = skinId,
                            image = frame,
                            asDefault = out.isEmpty(),
                        )
                    }
                    out.add(frame)
                }
            }
            kotlinx.coroutines.yield()
        }
        return out
    }

    private fun coverDiskFile(skinId: PacMazeSkinId): File =
        File(ResourceStore.decodedSkinCacheRoot(), "$DECODE_QUALITY_TAG/cover/${skinId.storageKey}.png")

    private fun loadCoverFromDisk(skinId: PacMazeSkinId): ImageBitmap? {
        val file = coverDiskFile(skinId)
        if (!file.isFile) return null
        return runCatching {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    private fun cacheCoverToDisk(skinId: PacMazeSkinId, bitmap: ImageBitmap) {
        runCatching {
            val out = coverDiskFile(skinId)
            out.parentFile?.mkdirs()
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 92, out.outputStream())
        }.onFailure { Log.w(TAG, "save cover failed $skinId", it) }
    }

    private fun diskClipDir(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): File =
        File(ResourceStore.decodedSkinCacheRoot(), "$DECODE_QUALITY_TAG/${skinId.storageKey}/${clip.folder}")

    private suspend fun loadClipFromDiskProgressive(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
    ): List<ImageBitmap>? {
        val dir = diskClipDir(skinId, clip)
        if (!dir.isDirectory) return null
        val files = dir.listFiles()
            ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }
            ?: return null
        if (files.isEmpty()) return null

        val bootstrapFiles = files.take(BOOTSTRAP_FRAME_COUNT)
        val bootstrap = withContext(Dispatchers.IO) {
            bootstrapFiles.mapNotNull { file ->
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also { frame ->
                    if (clip == PacMazeSkinAnimClip.WALK) {
                        PacMazeBitmapFeetAnchor.registerGameplayAnchor(
                            skinId = skinId,
                            image = frame,
                            asDefault = true,
                        )
                    }
                }
            }
        }
        if (bootstrap.isEmpty()) return null
        publishClipFrames(skinId, clip, bootstrap)

        if (files.size <= bootstrapFiles.size) return bootstrap

        scope.launch {
            runCatching {
                clipMutexes.getOrPut(skinId) { Mutex() }.withLock {
                    val current = synchronized(this@PacMazeRemoteSkinAnimCache) {
                        clipCache[skinId]?.get(clip)
                    }.orEmpty()
                    if (current.size >= files.size) return@withLock
                    val pendingFiles = files.drop(current.size)
                    if (pendingFiles.isEmpty()) return@withLock
                    val remaining = withContext(Dispatchers.IO) {
                        pendingFiles.mapNotNull { file ->
                            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()?.also { frame ->
                                if (clip == PacMazeSkinAnimClip.WALK) {
                                    PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, frame)
                                }
                            }
                        }
                    }
                    if (remaining.isEmpty()) return@withLock
                    val full = current + remaining
                    publishClipFrames(skinId, clip, full)
                }
            }
        }
        return bootstrap
    }

    private fun saveClipToDisk(frames: List<ImageBitmap>, skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        runCatching {
            val dir = diskClipDir(skinId, clip)
            dir.deleteRecursively()
            dir.mkdirs()
            frames.forEachIndexed { index, frame ->
                val out = File(dir, "${index + 1}.png")
                frame.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out.outputStream())
            }
        }.onFailure { Log.w(TAG, "saveClipToDisk failed ${skinId.storageKey}/${clip.folder}", it) }
    }

    private fun discoverFramePaths(relDir: String, prefix: String): List<String> {
        val out = mutableListOf<String>()
        var index = 1
        while (index <= 120) {
            val path = "$relDir/${prefix}${index}.png"
            if (ResourceStore.resolveFile(path) == null) break
            out.add(path)
            index++
        }
        return out
    }

    private fun decodeFrame(relativePath: String, sampleSize: Int): ImageBitmap? {
        val stream = ResourceStore.openInputStream(relativePath) ?: return null
        return stream.use {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap()?.let(PacMazeBitmapContentTrim::trimToOpaqueContent)
        }
    }
}
