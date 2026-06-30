package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.resource.BundleEnsureResult
import com.example.funlife.resource.DecodedClipDiskIndex
import com.example.funlife.resource.ParallelBitmapDecoder
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val decodeQualityTag: String
        get() = ResourceStore.pacMazeSkinsDecodeTag()
    /** 网格封面 decode 采样（更小更快；详情/局内仍用原图） */
    private const val COVER_DECODE_SAMPLE = 3
    /** 内存中同时保留完整动画的角色数（当前选中 + 最近使用）。 */
    private const val MAX_FULL_ANIM_SKINS = 4
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
    private val continueClipLoadJobs = ConcurrentHashMap.newKeySet<Pair<PacMazeSkinId, PacMazeSkinAnimClip>>()
    private var activeFullAnimSkin: PacMazeSkinId? = null

    private val _status = MutableStateFlow<Map<PacMazeSkinId, RemoteSkinLoadStatus>>(emptyMap())
    val status: StateFlow<Map<PacMazeSkinId, RemoteSkinLoadStatus>> = _status.asStateFlow()

    private val _playbackRevision = MutableStateFlow(0)
    val playbackRevision: StateFlow<Int> = _playbackRevision.asStateFlow()

    fun isCoverReady(skinId: PacMazeSkinId): Boolean = cover(skinId) != null

    /** 封面是否已有缓存（内存或磁盘 decode 产物），用于 UI 避免重复转圈。 */
    fun hasCoverCache(skinId: PacMazeSkinId): Boolean = synchronized(this) {
        coverCache[skinId] != null
    }

    /** 磁盘或内存是否有封面（仅 IO 线程调用）。 */
    fun hasCoverCacheOnDisk(skinId: PacMazeSkinId): Boolean = synchronized(this) {
        coverCache[skinId] != null || coverDiskFile(skinId).isFile
    }

    fun isReady(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val primary = config.primaryClip()
        if (playbackSheet(skinId, primary) != null) return true
        if (frames(skinId, primary)?.isNotEmpty() == true) return true
        return hasClipOnDisk(skinId, primary)
    }

    fun isAnimInMemory(skinId: PacMazeSkinId): Boolean {
        if (isPlaybackReady(skinId)) return true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        return frames(skinId, config.primaryClip())?.isNotEmpty() == true
    }

    fun cover(skinId: PacMazeSkinId): ImageBitmap? = synchronized(this) { coverCache[skinId] }

    /** 局内可播放的序列帧（含渐进加载的部分帧）。 */
    fun playbackFrames(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): List<ImageBitmap>? =
        frames(skinId, clip)?.takeIf { it.isNotEmpty() }

    fun playbackFrameCount(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Int =
        playbackFrames(skinId, clip)?.size
            ?: playbackSheet(skinId, clip)?.frameCount
            ?: 0

    fun isPlaybackReady(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val primary = config.primaryClip()
        if (hasSheetBundle(skinId)) {
            return playbackSheet(skinId, primary) != null
        }
        return playbackFrameCount(skinId, primary) >= MIN_BOOTSTRAP_FRAMES
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

    private fun applyManifestFeetAnchor(skinId: PacMazeSkinId) {
        val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId) ?: return
        if (manifest.normalized && manifest.anchorFrac != null) {
            PacMazeBitmapFeetAnchor.registerManifestAnchor(skinId, manifest.anchorFrac)
        }
    }

    /** 各 clip 首帧兜底（idle/jump/die 等），避免待机误用 walk 单帧。 */
    fun peekSingleClipFrame(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): ImageBitmap? {
        if (clip == PacMazeSkinAnimClip.WALK) return peekSingleWalkFrame(skinId)
        playbackFrames(skinId, clip)?.firstOrNull()?.let { return it }
        synchronized(singleClipFrameCache) {
            return singleClipFrameCache[skinId]?.get(clip)
        }
    }

    private suspend fun ensureSingleWalkFrame(skinId: PacMazeSkinId) {
        ensureSingleClipFrame(skinId, PacMazeSkinAnimClip.WALK)
    }

    private suspend fun ensureSingleClipFrame(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        if (peekSingleClipFrame(skinId, clip) != null) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (clip !in config.clips) return
        applyManifestFeetAnchor(skinId)

        resolveSheetAsset(config.assetRoot, clip)?.let { (sheetPath, sheet) ->
            val manifest = PacMazeSkinAnimManifest.load(config.assetRoot)
            val sampleSize = platformerSheetSampleSize(config, manifest)
            val decoded = withContext(Dispatchers.IO) {
                decodeSheetToFrames(
                    skinId = skinId,
                    clip = clip,
                    sheetPath = sheetPath,
                    sheet = sheet,
                    frameCount = 1,
                    sampleSize = sampleSize,
                    assetRoot = config.assetRoot,
                    registerFeet = shouldRegisterFeetOnDecode(skinId),
                ).firstOrNull()
            } ?: return
            cacheSingleClipFrame(skinId, clip, decoded)
            return
        }

        val folder = clipFolder(config.assetRoot, clip)
        val prefix = clipPrefix(config.assetRoot, clip)
        val path = "${config.assetRoot}/$folder/${prefix}_1.png"
        if (!ResourceStore.resourceExists(path)) return
        val decoded = withContext(Dispatchers.IO) {
            decodeFrame(path, config.sampleSize, config.assetRoot)
        } ?: return
        registerDecodedFeetAnchors(
            skinId = skinId,
            frame = decoded,
            clip = clip,
            frameIndex = 0,
            asDefault = clip == PacMazeSkinAnimClip.WALK,
            registerFeet = !PacMazeSkinAnimManifest.isNormalized(config.assetRoot),
        )
        cacheSingleClipFrame(skinId, clip, decoded)
    }

    private fun cacheSingleClipFrame(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        decoded: ImageBitmap,
    ) {
        if (clip == PacMazeSkinAnimClip.WALK) {
            synchronized(singleWalkFrameCache) {
                singleWalkFrameCache[skinId] = decoded
            }
        } else {
            synchronized(singleClipFrameCache) {
                singleClipFrameCache.getOrPut(skinId) { mutableMapOf() }[clip] = decoded
            }
        }
        _playbackRevision.update { it + 1 }
    }

    private const val MIN_BOOTSTRAP_FRAMES = 4
    /** 横版进局最低 walk 帧。 */
    const val BOOT_WALK_FRAMES = 4
    /** 横版进局最低 jump 帧。 */
    const val BOOT_JUMP_FRAMES = 2
    /** 同步解码帧数：够播 walk 即可进局，其余后台补齐。 */
    private const val BOOTSTRAP_FRAME_COUNT = MIN_BOOTSTRAP_FRAMES
    /** 并行 decode 批量（磁盘/资源包全量解码）。 */
    private const val DECODE_BATCH_SIZE = 14

    private val singleWalkFrameCache = mutableMapOf<PacMazeSkinId, ImageBitmap>()
    private val singleClipFrameCache = mutableMapOf<PacMazeSkinId, MutableMap<PacMazeSkinAnimClip, ImageBitmap>>()
    private val sheetPlaybackCache = ConcurrentHashMap<String, PacMazeSkinSheetPlayback>()
    private val sheetPlaybackMutex = Mutex()

    fun playbackSheet(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): PacMazeSkinSheetPlayback? =
        sheetPlaybackCache[sheetPlaybackKey(skinId, clip)]

    fun sheetPlaybackFrameCount(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Int =
        playbackSheet(skinId, clip)?.frameCount ?: clipFrameTarget(skinId, clip)

    /** bundle 内 walk clip 使用 sprite sheet（走廊 + 坤坤大冒险共用，一次 decode）。 */
    fun hasSheetBundle(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        return resolveSheetAsset(config.assetRoot, PacMazeSkinAnimClip.WALK) != null
    }

    /** @see hasSheetBundle */
    fun hasPlatformerSheetBundle(skinId: PacMazeSkinId): Boolean = hasSheetBundle(skinId)

    /** 横版 sheet 模式：walk + jump 各一张图 decode 完成即可进局。 */
    fun isSheetBootstrapPlayable(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val walkOk = playbackSheet(skinId, PacMazeSkinAnimClip.WALK) != null
        val jumpOk = if (PacMazeSkinAnimClip.JUMP in config.clips) {
            playbackSheet(skinId, PacMazeSkinAnimClip.JUMP) != null
        } else {
            true
        }
        return walkOk && jumpOk && PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)
    }

    fun requestSheetPlaybackAsync(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        if (playbackSheet(skinId, clip) != null) return
        scope.launch { runCatching { ensureSheetPlayback(skinId, clip) } }
    }

    suspend fun ensureSheetPlayback(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
    ): PacMazeSkinSheetPlayback? {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return null
        if (clip !in config.clips) return null
        val key = sheetPlaybackKey(skinId, clip)
        val manifestForSample = PacMazeSkinAnimManifest.load(config.assetRoot)
        val expectedSample = platformerSheetSampleSize(config, manifestForSample)
        sheetPlaybackCache[key]?.let { cached ->
            if (cached.sampleSize == expectedSample) return cached
            sheetPlaybackCache.remove(key)
        }
        return sheetPlaybackMutex.withLock {
            sheetPlaybackCache[key]?.let { cached ->
                if (cached.sampleSize == expectedSample) return cached
                sheetPlaybackCache.remove(key)
            }
            applyManifestFeetAnchor(skinId)
            val (sheetPath, sheetSpec) = resolveSheetAsset(config.assetRoot, clip) ?: return null
            val manifest = manifestForSample ?: PacMazeSkinAnimManifest.load(config.assetRoot)
            val frameCount = manifest?.frameCount(clip)?.coerceAtLeast(1) ?: return null
            val sampleSize = expectedSample
            val decodeStart = SystemClock.elapsedRealtime()
            val bitmap = withContext(Dispatchers.IO) {
                decodeSheetBitmapOnce(sheetPath, sampleSize)
            } ?: return null
            // normalized manifest：cellW/cellH = 单格 canvas 尺寸（非 sheet 总宽高）；勿除以 columns
            val cellW = (sheetSpec.cellW / sampleSize).coerceAtLeast(1)
            val cellH = (sheetSpec.cellH / sampleSize).coerceAtLeast(1)
            val playback = PacMazeSkinSheetPlayback(
                bitmap = bitmap,
                columns = sheetSpec.columns.coerceAtLeast(1),
                rows = sheetSpec.rows.coerceAtLeast(1),
                cellW = cellW,
                cellH = cellH,
                frameCount = frameCount,
                sampleSize = sampleSize,
            )
            withContext(Dispatchers.IO) {
                if (clip == PacMazeSkinAnimClip.WALK ||
                    clip == PacMazeSkinAnimClip.RUN ||
                    clip == PacMazeSkinAnimClip.IDLE
                ) {
                    PacMazeSheetCellFeetCache.precompute(skinId, clip, playback)
                }
            }
            sheetPlaybackCache[key] = playback
            _playbackRevision.update { it + 1 }
            val bmp = playback.bitmap
            Log.i(
                TAG,
                "sheet playback ready ${skinId.storageKey}/${clip.name} " +
                    "${playback.columns}x${playback.rows} cell=${cellW}x${cellH} " +
                    "bitmap=${bmp.width}x${bmp.height} sample=$sampleSize " +
                    "decodeMs=${SystemClock.elapsedRealtime() - decodeStart}",
            )
            playback
        }
    }

    /** 横版启动：优先 sheet 一次 decode（walk + jump），不拆 61 格进内存。 */
    suspend fun preparePlatformerSheetsForBoot(
        skinId: PacMazeSkinId,
        onStatus: (RemoteSkinLoadStatus) -> Unit = {},
    ): Boolean {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        retainFullAnimFor(skinId)
        applyManifestFeetAnchor(skinId)

        fun report(status: RemoteSkinLoadStatus) {
            setStatus(skinId, status)
            onStatus(status)
        }

        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Downloading, 8, "下载角色资源包…"))
            if (!ensureBundleWithProgress(skinId)) return false
        }

        report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 40, "加载角色动作资源…"))
        ensureSheetPlayback(skinId, PacMazeSkinAnimClip.WALK)
        if (PacMazeSkinAnimClip.JUMP in config.clips) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 62, "加载角色跳跃资源…"))
            ensureSheetPlayback(skinId, PacMazeSkinAnimClip.JUMP)
        }
        if (PacMazeSkinAnimClip.IDLE in PacMazeRemoteSkinAnimCatalog.pacMazeClips(skinId)) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 76, "加载角色形象资源…"))
            ensureSheetPlayback(skinId, PacMazeSkinAnimClip.IDLE)
        }
        val ready = isSheetBootstrapPlayable(skinId)
        report(
            RemoteSkinLoadStatus(
                phase = if (ready) RemoteSkinLoadPhase.Ready else RemoteSkinLoadPhase.Decoding,
                percent = if (ready) 100 else 80,
                message = if (ready) "角色资源已就绪" else "角色资源加载中…",
            ),
        )
        if (ready) {
            requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.DIE)
        }
        return ready
    }

    private fun sheetPlaybackKey(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): String =
        "${skinId.storageKey}:${clip.name.lowercase()}"

    private fun platformerSheetSampleSize(
        config: PacMazeRemoteSkinAnimConfig,
        manifest: PacMazeSkinAnimManifest.SkinAnimManifest?,
    ): Int {
        val cellH = manifest?.canvas?.h ?: 928
        var sample = 1
        while (cellH / sample > 128) sample *= 2
        // 走廊 manifest 常为 sampleSize=1；横版只 decode 一张 sheet，必须降采样避免 8×8 全分辨率 OOM/卡顿。
        return sample.coerceIn(1, 16).coerceAtLeast(config.sampleSize.coerceAtLeast(1))
    }

    private fun decodeSheetBitmapOnce(path: String, sampleSize: Int): ImageBitmap? {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return ResourceStore.openInputStream(path)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
        }
    }

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
        _playbackRevision.update { it + 1 }
    }

    fun clearMemoryCache() {
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
            singleWalkFrameCache.clear()
            singleClipFrameCache.clear()
            activeFullAnimSkin = null
        }
        sheetPlaybackCache.clear()
    }

    /** 皮肤包更新后：内存 + 磁盘解码缓存一并失效。 */
    fun invalidateAllCaches() {
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
            singleWalkFrameCache.clear()
            singleClipFrameCache.clear()
            activeFullAnimSkin = null
        }
        sheetPlaybackCache.clear()
        PacMazeBitmapFeetAnchor.invalidateAll()
        PacMazeSheetCellFeetCache.invalidateAll()
        PacMazeSkinAnimManifest.invalidateCache()
        ResourceStore.decodedSkinCacheRoot().deleteRecursively()
        ResourceStore.clearPlatformerDecodeStamp()
    }

    /** 裁边算法升级后清磁盘缓存，避免继续用未裁边的旧帧。 */
    fun invalidateDecodedDiskCache() {
        ResourceStore.decodedSkinCacheRoot().deleteRecursively()
        ResourceStore.clearPlatformerDecodeStamp()
        synchronized(this) {
            clipCache.clear()
            coverCache.clear()
        }
    }

    fun requestEnsureBundleAsync() {
        scope.launch {
            runCatching {
                if (!ResourceStore.ensureBundle("pac_maze_skins")) return@runCatching
                PacMazeRemoteSkinAnimCatalog.remoteSkinIds.forEach { skinId ->
                    runCatching { reloadClipsIfIncomplete(skinId) }
                    val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return@forEach
                    config.clips.forEach { clip ->
                        requestContinueClipLoadAsync(skinId, clip)
                    }
                }
            }
        }
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

    /** 加载页：优先从 APK/assets 或缓存解码封面与首帧，不必等云端整包。 */
    fun requestLoadingHeroBootstrapAsync(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        scope.launch { runCatching { bootstrapLoadingHero(skinId) } }
    }

    suspend fun bootstrapLoadingHero(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        hydrateCoverFromDisk(skinId)
        if (!isCoverReady(skinId)) {
            runCatching { preloadCoverFromLocalSources(skinId) }
        }
        withContext(Dispatchers.IO) {
            runCatching {
                ensureSingleWalkFrame(skinId)
                if (PacMazeSkinAnimClip.IDLE in PacMazeRemoteSkinAnimCatalog.pacMazeClips(skinId)) {
                    ensureSingleClipFrame(skinId, PacMazeSkinAnimClip.IDLE)
                }
                ensureSingleClipFrame(skinId, PacMazeSkinAnimClip.JUMP)
            }
        }
        if (ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            reloadClipsIfIncomplete(skinId)
        } else {
            requestEnsureBundleAsync()
        }
    }

    /** 按序号解码前 N 帧（walk_1..4 等），供横版进局前同步预热。 */
    suspend fun ensureBootstrapFrames(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        targetCount: Int,
    ) {
        ensureExplicitBootstrapFrames(skinId, clip, targetCount)
    }

    /** 横版进局：bootstrap 帧 + 脚点即可，全量 clip 后台补齐。 */
    fun isBootstrapPlayable(skinId: PacMazeSkinId): Boolean {
        if (isSheetBootstrapPlayable(skinId)) return true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val walkOk = playbackFrameCount(skinId, PacMazeSkinAnimClip.WALK) >= BOOT_WALK_FRAMES
        val jumpOk = if (PacMazeSkinAnimClip.JUMP in config.clips) {
            playbackFrameCount(skinId, PacMazeSkinAnimClip.JUMP) >= BOOT_JUMP_FRAMES
        } else {
            true
        }
        return walkOk && jumpOk && PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)
    }

    /** 后台补齐动画（sheet 模式仅补 die，不拆 walk 全量进内存）。 */
    fun requestFullPlatformerSkinAsync(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        if (hasSheetBundle(skinId)) {
            requestSheetPlaybackAsync(skinId, PacMazeSkinAnimClip.DIE)
            return
        }
        scope.launch { runCatching { preparePlatformerSkinFull(skinId) } }
    }

    /**
     * 横版启动快速通道：下载包 → 尽力读盘 → bootstrap 帧 → 立即返回；全量在后台。
     */
    suspend fun preparePlatformerSkinForBoot(
        skinId: PacMazeSkinId,
        onStatus: (RemoteSkinLoadStatus) -> Unit = {},
    ): Boolean {
        if (hasSheetBundle(skinId)) {
            return preparePlatformerSheetsForBoot(skinId, onStatus)
        }
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        retainFullAnimFor(skinId)
        applyManifestFeetAnchor(skinId)

        fun report(status: RemoteSkinLoadStatus) {
            setStatus(skinId, status)
            onStatus(status)
        }

        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Downloading, 8, "下载角色资源包…"))
            if (!ensureBundleWithProgress(skinId)) return false
        }

        report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 35, "读取本地资源…"))
        hydrateBootstrapClipFromDisk(skinId, PacMazeSkinAnimClip.WALK) { loaded, target ->
            val pct = 35 + (loaded * 18 / target.coerceAtLeast(1))
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, pct, GameResourceLoadCopy.progress("读取动作资源", loaded, target)))
        }
        hydrateBootstrapClipFromDisk(skinId, PacMazeSkinAnimClip.JUMP) { loaded, target ->
            val pct = 55 + (loaded * 12 / target.coerceAtLeast(1))
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, pct, GameResourceLoadCopy.progress("读取跳跃资源", loaded, target)))
        }

        withContext(Dispatchers.IO) {
            runCatching { ensureSingleWalkFrame(skinId) }
        }
        ensureExplicitBootstrapFrames(skinId, PacMazeSkinAnimClip.WALK, BOOT_WALK_FRAMES)
        if (PacMazeSkinAnimClip.JUMP in config.clips) {
            ensureExplicitBootstrapFrames(skinId, PacMazeSkinAnimClip.JUMP, BOOT_JUMP_FRAMES)
        }
        if (!isBootstrapPlayable(skinId)) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 72, "加载角色资源…"))
            ensureClip(skinId, PacMazeSkinAnimClip.WALK)
            if (PacMazeSkinAnimClip.JUMP in config.clips) {
                ensureClip(skinId, PacMazeSkinAnimClip.JUMP)
            }
        }

        val walk = playbackFrameCount(skinId, PacMazeSkinAnimClip.WALK)
        val ready = isBootstrapPlayable(skinId)
        report(
            RemoteSkinLoadStatus(
                if (ready) RemoteSkinLoadPhase.Ready else RemoteSkinLoadPhase.Decoding,
                if (ready) 100 else 88,
                if (ready) "资源已就绪" else GameResourceLoadCopy.progress("资源加载中", walk, BOOT_WALK_FRAMES),
            ),
        )
        if (ready) {
            requestFullPlatformerSkinAsync(skinId)
        }
        return ready
    }

    /**
     * 首次加载快速通道：PNG → WebP 直写磁盘，不整 clip 进内存（比 [preparePlatformerSkinFull] 快 2–4 倍）。
     */
    suspend fun preparePlatformerSkinDiskOnly(
        skinId: PacMazeSkinId,
        onStatus: (RemoteSkinLoadStatus) -> Unit = {},
    ): Boolean = withContext(Dispatchers.IO) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return@withContext true
        if (isPlatformerSkinFullyOnDisk(skinId)) return@withContext true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return@withContext false
        applyManifestFeetAnchor(skinId)
        if (hasSheetBundle(skinId)) return@withContext true

        fun report(phase: RemoteSkinLoadPhase, pct: Int, msg: String) {
            val status = RemoteSkinLoadStatus(phase, pct, msg)
            setStatus(skinId, status)
            onStatus(status)
        }

        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            report(RemoteSkinLoadPhase.Downloading, 8, "下载角色资源包…")
            if (!ensureBundleWithProgress(skinId)) return@withContext false
        }

        ParallelBitmapDecoder.withBatchBoost {
            val clips = buildList {
                add(PacMazeSkinAnimClip.WALK)
                if (PacMazeSkinAnimClip.JUMP in config.clips) add(PacMazeSkinAnimClip.JUMP)
                if (PacMazeSkinAnimClip.DIE in config.clips) add(PacMazeSkinAnimClip.DIE)
            }
            clips.forEachIndexed { index, clip ->
                if (isDiskClipComplete(skinId, clip)) return@forEachIndexed
                val basePct = 12 + (index * 70 / clips.size.coerceAtLeast(1))
                report(RemoteSkinLoadPhase.Decoding, basePct, "整理角色资源…")
                encodeClipToDiskFromAssets(skinId, clip, config.sampleSize) { loaded, total ->
                    val pct = basePct + (loaded * 20 / total.coerceAtLeast(1))
                    report(RemoteSkinLoadPhase.Decoding, pct, GameResourceLoadCopy.progress("整理角色资源", loaded, total))
                }
            }
        }
        isPlatformerSkinFullyOnDisk(skinId)
    }

    /** 从 zip PNG 并行解码并写 WebP 落盘，帧 bitmap 即时释放。 */
    suspend fun encodeClipToDiskFromAssets(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sampleSize: Int = 1,
        onProgress: ((loaded: Int, total: Int) -> Unit)? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        if (isDiskClipComplete(skinId, clip)) return@withContext true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return@withContext false
        resolveSheetAsset(config.assetRoot, clip)?.let { (sheetPath, sheet) ->
            return@withContext encodeClipToDiskFromSheet(
                skinId = skinId,
                clip = clip,
                sheetPath = sheetPath,
                sheet = sheet,
                sampleSize = config.sampleSize,
                onProgress = onProgress,
            )
        }
        val paths = discoverFramePaths(config.assetRoot, clip)
        if (paths.isEmpty()) return@withContext false

        val dir = diskClipDir(skinId, clip)
        dir.deleteRecursively()
        dir.mkdirs()
        val written = mutableListOf<File>()
        val compressFormat = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        val workers = ParallelBitmapDecoder.batchWorkers()
        val semaphore = kotlinx.coroutines.sync.Semaphore(workers)

        coroutineScope {
            paths.mapIndexed { index, path ->
                async {
                    semaphore.withPermit {
                        val frame = decodeFrame(path, sampleSize, config.assetRoot) ?: return@withPermit
                        if (clip == PacMazeSkinAnimClip.WALK && index == 0) {
                            registerDecodedFeetAnchors(
                                skinId = skinId,
                                frame = frame,
                                clip = clip,
                                frameIndex = 0,
                                asDefault = true,
                                registerFeet = shouldRegisterFeetOnDecode(skinId),
                            )
                        }
                        val out = File(dir, "${index + 1}.webp")
                        frame.asAndroidBitmap().compress(compressFormat, 90, out.outputStream())
                        synchronized(written) { written.add(out) }
                        onProgress?.invoke(written.size, paths.size)
                    }
                }
            }.awaitAll()
        }

        if (written.size < paths.size) return@withContext false
        written.sortBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }
        DecodedClipDiskIndex.writeMetaFromFiles(
            dir = dir,
            decodeTag = decodeQualityTag,
            files = written,
            format = DecodedClipDiskIndex.FORMAT_WEBP,
        )
        true
    }

    /** 从 bundle 内单张 sheet 解码并写 WebP 落盘（逐格 decode，避免整图 OOM）。 */
    private suspend fun encodeClipToDiskFromSheet(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheetPath: String,
        sheet: PacMazeSkinAnimManifest.SheetSpec,
        sampleSize: Int,
        onProgress: ((loaded: Int, total: Int) -> Unit)?,
    ): Boolean = withContext(Dispatchers.IO) {
        val target = clipFrameTarget(skinId, clip).coerceAtLeast(1)
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return@withContext false
        val registerFeet = shouldRegisterFeetOnDecode(skinId)

        val dir = diskClipDir(skinId, clip)
        dir.deleteRecursively()
        dir.mkdirs()
        val written = mutableListOf<File>()
        val compressFormat = if (android.os.Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        val ok = decodeSheetCellsStreaming(
            skinId = skinId,
            clip = clip,
            sheetPath = sheetPath,
            sheet = sheet,
            frameCount = target,
            sampleSize = sampleSize,
            assetRoot = config.assetRoot,
            registerFeet = registerFeet,
        ) { index, frame ->
            val out = File(dir, "${index + 1}.webp")
            val bmp = frame.asAndroidBitmap()
            bmp.compress(compressFormat, 90, out.outputStream())
            written.add(out)
            onProgress?.invoke(index + 1, target)
        }
        if (!ok || written.size < target) return@withContext false

        DecodedClipDiskIndex.writeMetaFromFiles(
            dir = dir,
            decodeTag = decodeQualityTag,
            files = written,
            format = DecodedClipDiskIndex.FORMAT_WEBP,
        )
        true
    }

    /** 横版进局：下载整包 → 磁盘缓存秒读 / 首次全量解码写盘。 */
    suspend fun preparePlatformerSkinFull(
        skinId: PacMazeSkinId,
        onStatus: (RemoteSkinLoadStatus) -> Unit = {},
    ): Boolean {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return true
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        retainFullAnimFor(skinId)
        applyManifestFeetAnchor(skinId)

        fun report(status: RemoteSkinLoadStatus) {
            setStatus(skinId, status)
            onStatus(status)
        }

        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Downloading, 8, "下载角色资源包…"))
            if (!ensureBundleWithProgress(skinId)) return false
        }

        val walkTarget = clipFrameTarget(skinId, PacMazeSkinAnimClip.WALK)

        report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 35, "读取本地资源…"))
        hydrateFullClipFromDisk(skinId, PacMazeSkinAnimClip.WALK) { loaded, target ->
            val pct = 35 + (loaded * 20 / target.coerceAtLeast(1))
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, pct, GameResourceLoadCopy.progress("读取动作资源", loaded, target)))
        }
        hydrateFullClipFromDisk(skinId, PacMazeSkinAnimClip.JUMP) { loaded, target ->
            val pct = 55 + (loaded * 15 / target.coerceAtLeast(1))
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, pct, GameResourceLoadCopy.progress("读取跳跃资源", loaded, target)))
        }

        if (!isClipFullyLoaded(skinId, PacMazeSkinAnimClip.WALK)) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 50, "加载角色动作资源…"))
            ensureFullClip(skinId, PacMazeSkinAnimClip.WALK)
        }
        if (!isClipFullyLoaded(skinId, PacMazeSkinAnimClip.JUMP)) {
            report(RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 72, "加载角色跳跃资源…"))
            ensureFullClip(skinId, PacMazeSkinAnimClip.JUMP)
        }

        val walkLoaded = playbackFrameCount(skinId, PacMazeSkinAnimClip.WALK)
        val ready = isClipFullyLoaded(skinId, PacMazeSkinAnimClip.WALK) &&
            isClipFullyLoaded(skinId, PacMazeSkinAnimClip.JUMP) &&
            PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)
        report(
            RemoteSkinLoadStatus(
                if (ready) RemoteSkinLoadPhase.Ready else RemoteSkinLoadPhase.Decoding,
                if (ready) 100 else 90,
                if (ready) {
                    val fromDisk = isPlatformerSkinOnDisk(skinId)
                    if (fromDisk) {
                        GameResourceLoadCopy.progress("本地资源命中", walkLoaded, walkTarget)
                    } else {
                        GameResourceLoadCopy.progress("角色动作资源已就绪", walkLoaded, walkTarget)
                    }
                } else {
                    GameResourceLoadCopy.progress("角色资源未完成", walkLoaded, walkTarget)
                },
            ),
        )
        return ready
    }

    /** @deprecated 使用 [preparePlatformerSkinFull] */
    suspend fun bootstrapPlatformerPlayable(skinId: PacMazeSkinId) {
        preparePlatformerSkinFull(skinId)
    }

    fun diskClipFrameCount(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Int {
        val dir = diskClipDir(skinId, clip)
        return DecodedClipDiskIndex.frameCount(dir, decodeQualityTag)
    }

    /** 磁盘 clip 是否完整（O(1) meta 校验）。 */
    fun isDiskClipComplete(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Boolean {
        val target = clipFrameTarget(skinId, clip)
        if (target <= 0) return false
        val dir = diskClipDir(skinId, clip)
        return DecodedClipDiskIndex.isComplete(dir, target, decodeQualityTag)
    }

    /** 启动阶段：仅从磁盘加载 bootstrap 帧数，避免并行解码整 clip 导致 OOM。 */
    private suspend fun hydrateBootstrapClipFromDisk(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        onProgress: ((loaded: Int, target: Int) -> Unit)? = null,
    ): List<ImageBitmap>? {
        if (diskClipFrameCount(skinId, clip) <= 0) return null
        val bootTarget = when (clip) {
            PacMazeSkinAnimClip.WALK -> BOOT_WALK_FRAMES
            PacMazeSkinAnimClip.JUMP -> BOOT_JUMP_FRAMES
            else -> BOOTSTRAP_FRAME_COUNT
        }
        if (playbackFrameCount(skinId, clip) >= bootTarget) return playbackFrames(skinId, clip)
        return loadClipFromDiskProgressive(
            skinId = skinId,
            clip = clip,
            completeSync = false,
            maxFrames = bootTarget,
            onProgress = onProgress,
        )
    }

    /** 磁盘已有完整 clip 时灌入内存（归一化皮肤跳过逐帧脚点扫描）。 */
    suspend fun hydrateFullClipFromDisk(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        onProgress: ((loaded: Int, target: Int) -> Unit)? = null,
    ): List<ImageBitmap>? {
        val target = clipFrameTarget(skinId, clip)
        if (target <= 0) return null
        if (diskClipFrameCount(skinId, clip) <= 0) return null
        if (isClipFullyLoaded(skinId, clip)) return playbackFrames(skinId, clip)
        return loadClipFromDiskProgressive(skinId, clip, completeSync = true, onProgress = onProgress)
    }

    /**
     * 按序号解码 walk_1..N（不依赖 manifest 61 帧扫描），APK 引导包只有前几帧时也能播循环。
     */
    private suspend fun ensureExplicitBootstrapFrames(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        targetCount: Int,
    ) {
        if (playbackFrameCount(skinId, clip) >= targetCount) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (clip !in config.clips) return
        retainFullAnimFor(skinId)

        resolveSheetAsset(config.assetRoot, clip)?.let { (sheetPath, sheet) ->
            val decoded = withContext(Dispatchers.IO) {
                decodeSheetToFrames(
                    skinId = skinId,
                    clip = clip,
                    sheetPath = sheetPath,
                    sheet = sheet,
                    frameCount = targetCount,
                    sampleSize = config.sampleSize,
                    assetRoot = config.assetRoot,
                    registerFeet = shouldRegisterFeetOnDecode(skinId),
                )
            }
            if (decoded.isEmpty()) return
            val existing = playbackFrames(skinId, clip).orEmpty()
            val merged = if (existing.size >= decoded.size) existing else decoded
            if (merged.size > existing.size) publishClipFrames(skinId, clip, merged)
            return
        }

        val folder = clipFolder(config.assetRoot, clip)
        val prefix = clipPrefix(config.assetRoot, clip)
        val decoded = withContext(Dispatchers.IO) {
            buildList {
                for (index in 1..targetCount) {
                    val path = "${config.assetRoot}/$folder/${prefix}_$index.png"
                    if (!ResourceStore.resourceExists(path)) continue
                    decodeFrame(path, config.sampleSize, config.assetRoot)?.let { frame ->
                        registerDecodedFeetAnchors(
                            skinId = skinId,
                            frame = frame,
                            clip = clip,
                            frameIndex = index - 1,
                            asDefault = clip == PacMazeSkinAnimClip.WALK && index == 1,
                        )
                        add(frame)
                    }
                }
            }
        }
        if (decoded.isEmpty()) return
        val existing = playbackFrames(skinId, clip).orEmpty()
        val merged = if (existing.size >= decoded.size) existing else decoded
        if (merged.size <= existing.size) return
        publishClipFrames(skinId, clip, merged)
    }

    /** 云端包就绪后：内存未加载但磁盘完整时只灌盘；真正缺帧才从 zip 解码。 */
    suspend fun reloadClipsIfIncomplete(skinId: PacMazeSkinId) {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) return
        applyManifestFeetAnchor(skinId)
        if (hasSheetBundle(skinId)) {
            config.clips.forEach { clip ->
                runCatching { ensureSheetPlayback(skinId, clip) }
            }
            return
        }

        for (clip in config.clips) {
            if (!isClipFullyLoaded(skinId, clip) && isDiskClipComplete(skinId, clip)) {
                hydrateFullClipFromDisk(skinId, clip)
            }
        }

        var needsReload = false
        PacMazeSkinAnimManifest.load(config.assetRoot)?.let { manifest ->
            val canvas = manifest.canvas
            if (manifest.normalized && canvas != null) {
                val frame = peekSingleWalkFrame(skinId)
                    ?: playbackFrames(skinId, PacMazeSkinAnimClip.WALK)?.firstOrNull()
                frame?.let {
                    if (it.width != canvas.w || it.height != canvas.h) {
                        needsReload = true
                        invalidateDecodedDiskCache()
                    }
                }
            }
        }
        if (!needsReload) {
            for (clip in config.clips) {
                if (isClipFullyLoaded(skinId, clip)) continue
                val target = clipFrameTarget(skinId, clip)
                if (target > 1) {
                    needsReload = true
                    break
                }
            }
        }
        if (!needsReload) return
        synchronized(this) {
            clipCache.remove(skinId)
            singleWalkFrameCache.remove(skinId)
            singleClipFrameCache.remove(skinId)
        }
        retainFullAnimFor(skinId)
        withContext(Dispatchers.IO) { runCatching { ensureSingleWalkFrame(skinId) } }
        ensureFullClip(skinId, PacMazeSkinAnimClip.WALK)
        ensureFullClip(skinId, PacMazeSkinAnimClip.JUMP)
        config.clips.filter { it !in setOf(PacMazeSkinAnimClip.WALK, PacMazeSkinAnimClip.JUMP) }.forEach { clip ->
            ensureClip(skinId, clip)
        }
        _playbackRevision.update { it + 1 }
    }

    fun discoverFramePathCount(assetRoot: String, clip: PacMazeSkinAnimClip): Int {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot)
        if (manifest?.hasSheet(clip) == true) {
            return manifest.frameCount(clip).coerceAtLeast(0)
        }
        return discoverFramePaths(assetRoot, clip).size
    }

    fun clipFrameTarget(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Int {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return 0
        val manifest = PacMazeSkinAnimManifest.load(config.assetRoot)
        val manifestCount = manifest?.frameCount(clip) ?: 0
        if (manifestCount > 0) return manifestCount
        return discoverFramePathCount(config.assetRoot, clip)
    }

    fun isClipFullyLoaded(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Boolean {
        val target = clipFrameTarget(skinId, clip)
        return target > 0 && playbackFrameCount(skinId, clip) >= target
    }

    fun isPlatformerSkinOnDisk(skinId: PacMazeSkinId): Boolean =
        isDiskClipComplete(skinId, PacMazeSkinAnimClip.WALK) &&
            isDiskClipComplete(skinId, PacMazeSkinAnimClip.JUMP)

    /** 磁盘全量：walk + jump + die 均已 decode 落盘。 */
    fun isPlatformerSkinFullyOnDisk(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        if (!isDiskClipComplete(skinId, PacMazeSkinAnimClip.WALK)) return false
        if (PacMazeSkinAnimClip.JUMP in config.clips &&
            !isDiskClipComplete(skinId, PacMazeSkinAnimClip.JUMP)
        ) {
            return false
        }
        if (PacMazeSkinAnimClip.DIE in config.clips &&
            !isDiskClipComplete(skinId, PacMazeSkinAnimClip.DIE)
        ) {
            return false
        }
        return true
    }

    /** 横版进局：同步解码 clip 全部序列帧（walk 61 帧等）。 */
    suspend fun ensureFullClip(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): List<ImageBitmap>? {
        if (hasSheetBundle(skinId)) {
            ensureSheetPlayback(skinId, clip)
            return playbackFrames(skinId, clip)
        }
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return null
        if (clip !in config.clips) return null
        retainFullAnimFor(skinId)
        if (!hasClipAssetSource(config.assetRoot, clip)) {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
                        ResourceStore.ensureBundle("pac_maze_skins")
                    }
                }
            }
        }
        repeat(3) {
            loadClip(config, clip, completeSync = true)
            if (isClipFullyLoaded(skinId, clip)) {
                return playbackFrames(skinId, clip)
            }
            kotlinx.coroutines.yield()
        }
        return playbackFrames(skinId, clip)
    }

    fun isClipAnimatable(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip): Boolean =
        playbackFrameCount(skinId, clip) > 1

    /** 进局前预热：walk_1 脚点 + 4 帧 bootstrap；不拉封面、不阻塞下载 bundle。 */
    suspend fun warmUpForGameplay(skinId: PacMazeSkinId) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (isPlaybackReady(skinId) && PacMazeBitmapFeetAnchor.hasGameplayDefault(skinId)) return
        retainFullAnimFor(skinId)
        applyManifestFeetAnchor(skinId)
        if (hasSheetBundle(skinId)) {
            PacMazeRemoteSkinAnimCatalog.pacMazeClips(skinId).forEach { clip ->
                ensureSheetPlayback(skinId, clip)
            }
            return
        }
        val primary = config.primaryClip()
        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            requestEnsureBundleAsync()
        }
        withContext(Dispatchers.IO) {
            runCatching { ensureSingleWalkFrame(skinId) }
        }
        ensureClip(skinId, primary)
    }

    /** 局内预热指定片段（横版 idle / jump / die 等）。 */
    suspend fun warmUpGameplayClips(skinId: PacMazeSkinId, vararg clips: PacMazeSkinAnimClip) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
            requestEnsureBundleAsync()
        }
        retainFullAnimFor(skinId)
        withContext(Dispatchers.IO) {
            runCatching { ensureSingleWalkFrame(skinId) }
        }
        clips.filter { it in config.clips }.forEach { clip ->
            ensureClip(skinId, clip)
        }
    }

    fun hasWalkFrames(skinId: PacMazeSkinId): Boolean {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return false
        val clip = if (PacMazeSkinAnimClip.WALK in config.clips) {
            PacMazeSkinAnimClip.WALK
        } else {
            config.primaryClip()
        }
        return playbackSheet(skinId, clip) != null || frames(skinId, clip)?.isNotEmpty() == true
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
            if (preloadCoverFromLocalSources(skinId)) return
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
                publishCover(skinId, it)
                return
            }
            val bitmap = coverDecodeSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    runCatching { decodeFrame(path, COVER_DECODE_SAMPLE) }.getOrNull()
                }
            }
            if (bitmap == null) {
                setStatus(skinId, failedStatus("封面资源加载失败"))
                return
            }
            cacheCoverToDisk(skinId, bitmap)
            publishCover(skinId, bitmap)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "cover OOM $skinId", oom)
            setStatus(skinId, failedStatus("内存不足，请稍后重试"))
        } finally {
            synchronized(coverJobs) { coverJobs.remove(skinId) }
        }
    }

    /** 从 APK assets 或 resource_cache 解码封面，无需云端整包。 */
    private suspend fun preloadCoverFromLocalSources(skinId: PacMazeSkinId): Boolean {
        val path = PacMazeRemoteSkinAnimCatalog.resolvePreviewAssetPath(skinId) ?: return false
        if (!ResourceStore.resourceExists(path)) return false
        loadCoverFromDisk(skinId)?.let {
            publishCover(skinId, it)
            return true
        }
        val bitmap = coverDecodeSemaphore.withPermit {
            withContext(Dispatchers.IO) {
                runCatching { decodeFrame(path, COVER_DECODE_SAMPLE) }.getOrNull()
            }
        } ?: return false
        cacheCoverToDisk(skinId, bitmap)
        publishCover(skinId, bitmap)
        return true
    }

    private fun publishCover(skinId: PacMazeSkinId, bitmap: ImageBitmap) {
        synchronized(this) {
            coverCache[skinId] = bitmap
        }
        setStatus(skinId, coverReadyStatus())
        _playbackRevision.update { it + 1 }
    }

    suspend fun preloadForSkin(skinId: PacMazeSkinId) {
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (isPlaybackReady(skinId)) {
            setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
            return
        }
        if (hasSheetBundle(skinId)) {
            setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 40, "加载角色动作…"))
            val primary = config.primaryClip()
            ensureSheetPlayback(skinId, primary)
            if (isPlaybackReady(skinId)) {
                setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
            }
            return
        }
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
                        setStatus(skinId, failedStatus("角色资源不完整，请检查网络后重试"))
                    }
                }
            }
            if (isAnimInMemory(skinId)) {
                setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Ready, 100, "已就绪"))
            } else if (_status.value[skinId]?.phase != RemoteSkinLoadPhase.Failed) {
                setStatus(skinId, failedStatus("资源加载未完成"))
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
        if (!hasClipAssetSource(config.assetRoot, clip)) {
            if (!ResourceStore.isPacMazeBundleReady("pac_maze_skins")) {
                requestEnsureBundleAsync()
            }
            return null
        }
        retainFullAnimFor(skinId)
        return loadClip(config, clip)
    }

    fun requestClipAsync(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        if (hasSheetBundle(skinId)) {
            requestSheetPlaybackAsync(skinId, clip)
            return
        }
        if (isClipFullyLoaded(skinId, clip)) return
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

    /** 局内持续补齐完整序列；若仍不足则允许再次调度（云端包晚到场景）。 */
    fun requestContinueClipLoadAsync(skinId: PacMazeSkinId, clip: PacMazeSkinAnimClip) {
        if (!PacMazeRemoteSkinAnimCatalog.usesRemoteAnim(skinId)) return
        val config = PacMazeRemoteSkinAnimCatalog.config(skinId) ?: return
        if (hasSheetBundle(skinId)) {
            requestSheetPlaybackAsync(skinId, clip)
            return
        }
        val available = discoverFramePathCount(config.assetRoot, clip)
        if (playbackFrameCount(skinId, clip) >= available) return
        val key = skinId to clip
        if (!continueClipLoadJobs.add(key)) return
        scope.launch {
            try {
                runCatching {
                    if (ResourceStore.ensureBundle("pac_maze_skins")) {
                        reloadClipsIfIncomplete(skinId)
                    }
                    ensureFullClip(skinId, clip)
                }
            } finally {
                continueClipLoadJobs.remove(key)
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
        if (ResourceStore.isPacMazeBundleReady("pac_maze_skins")) return true
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
        setStatus(skinId, RemoteSkinLoadStatus(RemoteSkinLoadPhase.Decoding, 84, "解析角色资源…"))
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
            if (entry.isDirectory && entry.name != decodeQualityTag) {
                entry.deleteRecursively()
            }
        }
    }

    private fun setStatus(skinId: PacMazeSkinId, value: RemoteSkinLoadStatus) {
        val display = value.copy(message = GameResourceLoadCopy.forDisplay(value.message))
        _status.update { it + (skinId to display) }
    }

    private suspend fun loadClip(
        config: PacMazeRemoteSkinAnimConfig,
        clip: PacMazeSkinAnimClip,
        completeSync: Boolean = false,
    ): List<ImageBitmap>? = clipMutexes.getOrPut(config.skinId) { Mutex() }.withLock {
        if (clip !in config.clips) return null

        resolveSheetAsset(config.assetRoot, clip)?.let { (sheetPath, sheet) ->
            return loadClipFromBundleSheet(config, clip, sheetPath, sheet, completeSync)
        }

        val paths = discoverFramePaths(config.assetRoot, clip)
        if (paths.isEmpty()) return null

        val targetCount = clipFrameTarget(config.skinId, clip).coerceAtLeast(paths.size)

        val existing = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (existing.size >= targetCount) return existing

        if (existing.isEmpty()) {
            loadClipFromDiskProgressive(config.skinId, clip, completeSync)?.let { fromDisk ->
                if (fromDisk.size >= targetCount) return fromDisk
            }
        }

        val merged = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (merged.size >= targetCount) return merged

        val startIndex = merged.size
        val pendingPaths = paths.drop(startIndex)
        if (pendingPaths.isEmpty()) return merged

        val bootstrapPaths = if (completeSync) pendingPaths else pendingPaths.take(BOOTSTRAP_FRAME_COUNT)
        val bootstrap = withContext(Dispatchers.IO) {
            decodePathsBatched(
                skinId = config.skinId,
                clip = clip,
                paths = bootstrapPaths,
                sampleSize = config.sampleSize,
                registerFeet = !PacMazeSkinAnimManifest.isNormalized(config.assetRoot),
                assetRoot = config.assetRoot,
                startFrameIndex = startIndex,
            )
        }
        if (bootstrap.isEmpty()) return merged.takeIf { it.isNotEmpty() }

        val afterBootstrap = merged + bootstrap
        publishClipFrames(config.skinId, clip, afterBootstrap)

        if (completeSync) {
            saveClipToDisk(afterBootstrap, config.skinId, clip)
            return afterBootstrap
        }

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
                            clip = clip,
                            paths = stillPending,
                            sampleSize = config.sampleSize,
                            registerFeet = !PacMazeSkinAnimManifest.isNormalized(config.assetRoot),
                            assetRoot = config.assetRoot,
                            startFrameIndex = current.size,
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

    private fun registerDecodedFeetAnchors(
        skinId: PacMazeSkinId,
        frame: ImageBitmap,
        clip: PacMazeSkinAnimClip,
        frameIndex: Int,
        asDefault: Boolean,
        registerFeet: Boolean = true,
    ) {
        if (registerFeet) {
            PacMazeBitmapFeetAnchor.registerGameplayAnchor(skinId, frame, asDefault)
        }
        if (skinId == PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX) {
            PacMazeBitmapFeetAnchor.registerPlatformerFrameMetrics(skinId, frame, clip, frameIndex)
        }
    }

    private fun shouldRegisterFeetOnDecode(skinId: PacMazeSkinId): Boolean {
        val assetRoot = PacMazeRemoteSkinAnimCatalog.config(skinId)?.assetRoot ?: return true
        return !PacMazeSkinAnimManifest.isNormalized(assetRoot)
    }

    private suspend fun decodePathsBatched(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        paths: List<String>,
        sampleSize: Int,
        registerFeet: Boolean,
        assetRoot: String,
        startFrameIndex: Int = 0,
    ): List<ImageBitmap> {
        if (paths.isEmpty()) return emptyList()
        val out = ArrayList<ImageBitmap>(paths.size)
        var frameIndex = startFrameIndex
        for (chunk in paths.chunked(DECODE_BATCH_SIZE)) {
            chunk.forEach { path ->
                decodeFrame(path, sampleSize, assetRoot)?.let { frame ->
                    registerDecodedFeetAnchors(
                        skinId = skinId,
                        frame = frame,
                        clip = clip,
                        frameIndex = frameIndex,
                        asDefault = clip == PacMazeSkinAnimClip.WALK && frameIndex == 0,
                        registerFeet = registerFeet,
                    )
                    frameIndex++
                    out.add(frame)
                }
            }
            kotlinx.coroutines.yield()
        }
        return out
    }

    private fun coverDiskFile(skinId: PacMazeSkinId): File =
        File(ResourceStore.decodedSkinCacheRoot(), "$decodeQualityTag/cover/${skinId.storageKey}.png")

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
        File(ResourceStore.decodedSkinCacheRoot(), "$decodeQualityTag/${skinId.storageKey}/${clip.folder}")

    private suspend fun loadClipFromDiskProgressive(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        completeSync: Boolean = false,
        maxFrames: Int? = null,
        onProgress: ((loaded: Int, target: Int) -> Unit)? = null,
    ): List<ImageBitmap>? {
        val dir = diskClipDir(skinId, clip)
        val files = DecodedClipDiskIndex.listFrameFiles(dir, decodeQualityTag) ?: return null
        if (files.isEmpty()) return null

        val registerFeet = shouldRegisterFeetOnDecode(skinId)
        val capped = maxFrames?.coerceAtMost(files.size) ?: files.size
        val loadFiles = if (completeSync) files.take(capped) else files.take(minOf(capped, BOOTSTRAP_FRAME_COUNT))
        val targetCount = loadFiles.size

        val bootstrap = ParallelBitmapDecoder.decodeFilesParallel(loadFiles, onProgress = onProgress)
            .mapIndexed { index, frame ->
                registerDecodedFeetAnchors(
                    skinId = skinId,
                    frame = frame,
                    clip = clip,
                    frameIndex = index,
                    asDefault = clip == PacMazeSkinAnimClip.WALK && index == 0,
                    registerFeet = registerFeet,
                )
                frame
            }
        onProgress?.invoke(bootstrap.size, targetCount)
        if (bootstrap.isEmpty()) return null
        publishClipFrames(skinId, clip, bootstrap)

        if (completeSync || files.size <= loadFiles.size) return bootstrap

        scope.launch {
            runCatching {
                clipMutexes.getOrPut(skinId) { Mutex() }.withLock {
                    val current = synchronized(this@PacMazeRemoteSkinAnimCache) {
                        clipCache[skinId]?.get(clip)
                    }.orEmpty()
                    if (current.size >= files.size) return@withLock
                    val pendingFiles = files.drop(current.size)
                    if (pendingFiles.isEmpty()) return@withLock
                    val remaining = ParallelBitmapDecoder.decodeFilesParallel(pendingFiles)
                        .mapIndexed { offset, frame ->
                            registerDecodedFeetAnchors(
                                skinId = skinId,
                                frame = frame,
                                clip = clip,
                                frameIndex = current.size + offset,
                                asDefault = false,
                                registerFeet = registerFeet,
                            )
                            frame
                        }
                    if (remaining.isEmpty()) return@withLock
                    publishClipFrames(skinId, clip, current + remaining)
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
            val written = mutableListOf<File>()
            frames.forEachIndexed { index, frame ->
                val out = File(dir, "${index + 1}.webp")
                val format = if (android.os.Build.VERSION.SDK_INT >= 30) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                frame.asAndroidBitmap().compress(format, 90, out.outputStream())
                written.add(out)
            }
            DecodedClipDiskIndex.writeMetaFromFiles(
                dir = dir,
                decodeTag = decodeQualityTag,
                files = written,
                format = DecodedClipDiskIndex.FORMAT_WEBP,
            )
        }.onFailure { Log.w(TAG, "saveClipToDisk failed ${skinId.storageKey}/${clip.folder}", it) }
    }

    private fun clipFolder(assetRoot: String, clip: PacMazeSkinAnimClip): String =
        PacMazeSkinAnimManifest.load(assetRoot)?.clipFolder(clip) ?: clip.folder

    private fun clipPrefix(assetRoot: String, clip: PacMazeSkinAnimClip): String =
        PacMazeSkinAnimManifest.load(assetRoot)?.clipPrefix(clip) ?: clip.prefix

    private fun resolveSheetAsset(
        assetRoot: String,
        clip: PacMazeSkinAnimClip,
    ): Pair<String, PacMazeSkinAnimManifest.SheetSpec>? {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot) ?: return null
        val sheet = manifest.clipSheet(clip) ?: return null
        val folder = manifest.clipFolder(clip)
        val path = "$assetRoot/$folder/${sheet.file}"
        if (!ResourceStore.resourceExists(path)) return null
        return path to sheet
    }

    private fun hasClipAssetSource(assetRoot: String, clip: PacMazeSkinAnimClip): Boolean =
        resolveSheetAsset(assetRoot, clip) != null || discoverFramePaths(assetRoot, clip).isNotEmpty()

    private suspend fun loadClipFromBundleSheet(
        config: PacMazeRemoteSkinAnimConfig,
        clip: PacMazeSkinAnimClip,
        sheetPath: String,
        sheet: PacMazeSkinAnimManifest.SheetSpec,
        completeSync: Boolean,
    ): List<ImageBitmap>? {
        if (hasSheetBundle(config.skinId)) {
            ensureSheetPlayback(config.skinId, clip)
            return synchronized(this) { clipCache[config.skinId]?.get(clip) }
        }
        val targetCount = clipFrameTarget(config.skinId, clip).coerceAtLeast(1)

        val existing = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (existing.size >= targetCount) return existing

        if (existing.isEmpty()) {
            loadClipFromDiskProgressive(config.skinId, clip, completeSync)?.let { fromDisk ->
                if (fromDisk.size >= targetCount) return fromDisk
            }
        }

        val merged = synchronized(this) { clipCache[config.skinId]?.get(clip) }.orEmpty()
        if (merged.size >= targetCount) return merged

        val frames = withContext(Dispatchers.IO) {
            decodeSheetToFrames(
                skinId = config.skinId,
                clip = clip,
                sheetPath = sheetPath,
                sheet = sheet,
                frameCount = targetCount,
                sampleSize = config.sampleSize,
                assetRoot = config.assetRoot,
                registerFeet = shouldRegisterFeetOnDecode(config.skinId),
            )
        }
        if (frames.isEmpty()) return merged.takeIf { it.isNotEmpty() }

        publishClipFrames(config.skinId, clip, frames)
        if (completeSync || frames.size >= targetCount) {
            saveClipToDisk(frames, config.skinId, clip)
        } else {
            scheduleSaveClipToDisk(frames, config.skinId, clip)
        }
        return frames
    }

    private fun decodeSheetToFrames(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheetPath: String,
        sheet: PacMazeSkinAnimManifest.SheetSpec,
        frameCount: Int,
        sampleSize: Int,
        assetRoot: String,
        registerFeet: Boolean,
    ): List<ImageBitmap> {
        val out = ArrayList<ImageBitmap>(frameCount.coerceAtLeast(1))
        val ok = decodeSheetCellsStreaming(
            skinId = skinId,
            clip = clip,
            sheetPath = sheetPath,
            sheet = sheet,
            frameCount = frameCount,
            sampleSize = sampleSize,
            assetRoot = assetRoot,
            registerFeet = registerFeet,
        ) { _, frame -> out.add(frame) }
        if (!ok) {
            Log.w(TAG, "decodeSheetToFrames failed $sheetPath clip=${clip.name} got=${out.size}/$frameCount")
        }
        return out
    }

    /**
     * 逐格 decode sprite sheet（BitmapRegionDecoder），避免 8×8 大图一次性进内存 OOM。
     */
    private fun decodeSheetCellsStreaming(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheetPath: String,
        sheet: PacMazeSkinAnimManifest.SheetSpec,
        frameCount: Int,
        sampleSize: Int,
        assetRoot: String,
        registerFeet: Boolean,
        onCell: (index: Int, frame: ImageBitmap) -> Unit,
    ): Boolean {
        val cols = sheet.columns.coerceAtLeast(1)
        val rows = sheet.rows.coerceAtLeast(1)
        val count = frameCount.coerceAtMost(cols * rows).coerceAtLeast(0)
        if (count == 0) return false
        val normalized = PacMazeSkinAnimManifest.isNormalized(assetRoot)
        val cellW = sheet.cellW.coerceAtLeast(1)
        val cellH = sheet.cellH.coerceAtLeast(1)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val regionOk = ResourceStore.withBitmapRegionDecoder(sheetPath) { decoder ->
            try {
                for (index in 0 until count) {
                    val col = index % cols
                    val row = index / cols
                    val rect = Rect(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)
                    val cell = decoder.decodeRegion(rect, opts)
                        ?: run {
                            Log.w(TAG, "decodeRegion null $sheetPath cell=$index")
                            return@withBitmapRegionDecoder false
                        }
                    val frame = finalizeSheetCell(cell, normalized)
                    registerDecodedFeetAnchors(
                        skinId = skinId,
                        frame = frame,
                        clip = clip,
                        frameIndex = index,
                        asDefault = clip == PacMazeSkinAnimClip.WALK && index == 0,
                        registerFeet = registerFeet,
                    )
                    onCell(index, frame)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "sheet region decode error $sheetPath", e)
                false
            }
        }
        if (regionOk) return true

        Log.w(TAG, "sheet region decode failed, trying stream/fallback: $sheetPath")
        val stream = ResourceStore.openInputStream(sheetPath)
        if (stream == null) {
            Log.w(TAG, "sheet stream missing: $sheetPath")
            return decodeSheetCellsFullBitmapFallback(
                skinId, clip, sheetPath, sheet, count, sampleSize, assetRoot, registerFeet, onCell,
            )
        }

        return stream.use {
            val decoder = runCatching {
                BitmapRegionDecoder.newInstance(it, false)
            }.getOrNull()
            if (decoder != null) {
                try {
                    for (index in 0 until count) {
                        val col = index % cols
                        val row = index / cols
                        val rect = Rect(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)
                        val cell = decoder.decodeRegion(rect, opts)
                            ?: return@use decodeSheetCellsFullBitmapFallback(
                                skinId, clip, sheetPath, sheet, count, sampleSize, assetRoot, registerFeet, onCell,
                            )
                        val frame = finalizeSheetCell(cell, normalized)
                        registerDecodedFeetAnchors(
                            skinId = skinId,
                            frame = frame,
                            clip = clip,
                            frameIndex = index,
                            asDefault = clip == PacMazeSkinAnimClip.WALK && index == 0,
                            registerFeet = registerFeet,
                        )
                        onCell(index, frame)
                    }
                    true
                } finally {
                    decoder.recycle()
                }
            } else {
                decodeSheetCellsFullBitmapFallback(
                    skinId, clip, sheetPath, sheet, count, sampleSize, assetRoot, registerFeet, onCell,
                )
            }
        }
    }

    private fun finalizeSheetCell(cell: Bitmap, normalized: Boolean): ImageBitmap {
        // 不可 recycle：Compose ImageBitmap 与底层 Bitmap 共享内存，recycle 会导致局内绘制空白。
        if (normalized) {
            return cell.asImageBitmap()
        }
        val img = cell.asImageBitmap()
        return PacMazeBitmapContentTrim.trimToOpaqueContent(img)
    }

    /** RegionDecoder 不可用时降级：整图 decode + 切格（可能 OOM，仅兜底）。 */
    private fun decodeSheetCellsFullBitmapFallback(
        skinId: PacMazeSkinId,
        clip: PacMazeSkinAnimClip,
        sheetPath: String,
        sheet: PacMazeSkinAnimManifest.SheetSpec,
        count: Int,
        sampleSize: Int,
        assetRoot: String,
        registerFeet: Boolean,
        onCell: (index: Int, frame: ImageBitmap) -> Unit,
    ): Boolean {
        Log.w(TAG, "sheet region decode unavailable, fallback full bitmap: $sheetPath")
        val stream = ResourceStore.openInputStream(sheetPath) ?: return false
        val source = stream.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        } ?: return false

        val cols = sheet.columns.coerceAtLeast(1)
        val cellW = source.width / cols
        val cellH = source.height / sheet.rows.coerceAtLeast(1)
        val normalized = PacMazeSkinAnimManifest.isNormalized(assetRoot)
        return runCatching {
            for (index in 0 until count) {
                val col = index % cols
                val row = index / cols
                val cell = Bitmap.createBitmap(source, col * cellW, row * cellH, cellW, cellH)
                val frame = finalizeSheetCell(cell, normalized)
                registerDecodedFeetAnchors(
                    skinId = skinId,
                    frame = frame,
                    clip = clip,
                    frameIndex = index,
                    asDefault = clip == PacMazeSkinAnimClip.WALK && index == 0,
                    registerFeet = registerFeet,
                )
                onCell(index, frame)
            }
            true
        }.onFailure { e ->
            Log.e(TAG, "sheet full fallback failed $sheetPath", e)
        }.getOrDefault(false).also {
            source.recycle()
        }
    }

    private fun discoverFramePaths(assetRoot: String, clip: PacMazeSkinAnimClip): List<String> {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot)
        val folder = manifest?.clipFolder(clip) ?: clip.folder
        val prefix = manifest?.clipPrefix(clip) ?: clip.prefix
        val relDir = "$assetRoot/$folder"
        val filePrefix = "${prefix}_"
        val manifestCount = manifest?.frameCount(clip) ?: 0
        if (manifestCount > 0) {
            val paths = (1..manifestCount).map { index -> "$relDir/${filePrefix}$index.png" }
            ResourceStore.prefetchResourceExists(paths)
            return paths.filter { ResourceStore.resourceExists(it) }
        }
        return discoverFramePathsLegacy(relDir, filePrefix)
    }

    private fun discoverFramePathsLegacy(relDir: String, prefix: String): List<String> {
        val out = mutableListOf<String>()
        var index = 1
        while (index <= 120) {
            val path = "$relDir/${prefix}${index}.png"
            if (!ResourceStore.resourceExists(path)) break
            out.add(path)
            index++
        }
        return out
    }

    private fun decodeFrame(relativePath: String, sampleSize: Int, assetRoot: String? = null): ImageBitmap? {
        val stream = ResourceStore.openInputStream(relativePath) ?: return null
        val root = assetRoot ?: relativePath.substringBeforeLast('/').substringBeforeLast('/')
        return stream.use {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeStream(it, null, opts)?.asImageBitmap() ?: return null
            if (PacMazeSkinAnimManifest.isNormalized(root)) decoded
            else PacMazeBitmapContentTrim.trimToOpaqueContent(decoded)
        }
    }
}
