package com.example.funlife.game.platformer.catalog

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerEnemyBehavior
import com.example.funlife.game.platformer.PlatformerPlayer
import com.example.funlife.game.platformer.PlatformerCombat
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.game.platformer.PlatformerRangedCombat
import com.example.funlife.resource.DecodedClipDiskIndex
import com.example.funlife.resource.ParallelBitmapDecoder
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 横版 catalog 驱动序列帧缓存：内存 + 磁盘双层 + clip_meta 索引 + 并行解码。
 */
object PlatformerRemoteAnimCache {

    private const val TAG = "PlatformerRemoteAnim"
    private const val MIN_WALK_FRAMES = 4

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clipCache = ConcurrentHashMap<String, MutableMap<PlatformerAnimClip, List<ImageBitmap>>>()
    private val configCache = ConcurrentHashMap<String, PlatformerAnimConfig>()
    private val clipMutex = ConcurrentHashMap<String, Mutex>()
    private val sheetPlaybackCache = ConcurrentHashMap<String, PacMazeSkinSheetPlayback>()
    private val sheetMutex = Mutex()

    data class CatalogSheetDraw(
        val sheet: PacMazeSkinSheetPlayback,
        val clip: PlatformerAnimClip,
        val frameIndex: Int,
    )

    fun playbackSheet(assetKey: String, clip: PlatformerAnimClip): PacMazeSkinSheetPlayback? =
        sheetPlaybackCache[sheetKey(assetKey, clip)]

    fun requestSheetPlaybackAsync(characterId: PlatformerCharacterId, clip: PlatformerAnimClip) {
        val cfg = config(characterId) ?: return
        if (clip !in cfg.clips) return
        if (playbackSheet(characterId.catalogId, clip) != null) return
        scope.launch { runCatching { ensureSheetPlayback(characterId.catalogId, cfg, clip) } }
    }

    suspend fun ensureSheetPlayback(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        clip: PlatformerAnimClip,
    ): PacMazeSkinSheetPlayback? {
        val key = sheetKey(assetKey, clip)
        sheetPlaybackCache[key]?.let { return it }
        return sheetMutex.withLock {
            sheetPlaybackCache[key]?.let { return it }
            val (sheetPath, spec) = resolveSheetAsset(cfg.assetRoot, clip) ?: return null
            val manifest = PacMazeSkinAnimManifest.load(cfg.assetRoot) ?: return null
            val frameCount = manifest.frameCountByKey(clip.name.lowercase()).coerceAtLeast(1)
            val sampleSize = platformerSheetSampleSize(cfg, manifest)
            val bitmap = withContext(Dispatchers.IO) {
                decodeSheetBitmapOnce(sheetPath, sampleSize)
            } ?: return null
            val playback = PacMazeSkinSheetPlayback(
                bitmap = bitmap,
                columns = spec.columns.coerceAtLeast(1),
                rows = spec.rows.coerceAtLeast(1),
                cellW = (spec.cellW / sampleSize).coerceAtLeast(1),
                cellH = (spec.cellH / sampleSize).coerceAtLeast(1),
                frameCount = frameCount,
                sampleSize = sampleSize,
            )
            sheetPlaybackCache[key] = playback
            Log.i(TAG, "sheet ready $assetKey/${clip.name} cell=${playback.cellW}x${playback.cellH} sample=$sampleSize")
            playback
        }
    }

    suspend fun prepareSheetsForBoot(characterId: PlatformerCharacterId): Boolean {
        if (!ResourceStore.isPacMazeBundleReady("platformer_characters")) {
            withContext(Dispatchers.IO) {
                runCatching { ResourceStore.ensureBundle("platformer_characters") }
            }
        }
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        if (!hasAnySheet(cfg.assetRoot)) return false
        ensureSheetPlayback(key, cfg, primaryClip(cfg))
        if (PlatformerAnimClip.IDLE in cfg.clips) {
            ensureSheetPlayback(key, cfg, PlatformerAnimClip.IDLE)
        }
        if (PlatformerAnimClip.JUMP in cfg.clips) {
            ensureSheetPlayback(key, cfg, PlatformerAnimClip.JUMP)
        }
        return isSheetBootstrapPlayable(characterId)
    }

    fun isSheetBootstrapPlayable(characterId: PlatformerCharacterId): Boolean {
        val cfg = config(characterId) ?: return false
        if (!hasAnySheet(cfg.assetRoot)) return false
        val key = characterId.catalogId
        if (playbackSheet(key, primaryClip(cfg)) == null) return false
        if (PlatformerAnimClip.IDLE in cfg.clips &&
            playbackSheet(key, PlatformerAnimClip.IDLE) == null
        ) {
            return false
        }
        if (PlatformerAnimClip.JUMP in cfg.clips &&
            playbackSheet(key, PlatformerAnimClip.JUMP) == null
        ) {
            return false
        }
        return true
    }

    fun resolveSheetDraw(
        characterId: PlatformerCharacterId,
        player: PlatformerPlayer,
        animTime: Float,
    ): CatalogSheetDraw? {
        val cfg = config(characterId) ?: return null
        val key = characterId.catalogId
        val clip = pickClip(cfg, player)
        requestSheetPlaybackAsync(characterId, clip)
        val resolved = resolveSheetPlayback(key, cfg, clip) ?: return null
        val (playbackClip, sheet) = resolved
        val rate = when (playbackClip) {
            PlatformerAnimClip.DIE -> 24f
            PlatformerAnimClip.JUMP -> 20f
            PlatformerAnimClip.ATTACK, PlatformerAnimClip.JUMP_ATTACK -> PlatformerCombat.ATTACK_ANIM_FPS
            PlatformerAnimClip.SHOOT, PlatformerAnimClip.RUN_SHOOT,
            PlatformerAnimClip.JUMP_SHOOT, PlatformerAnimClip.THROW,
            -> PlatformerRangedCombat.RANGED_ANIM_FPS
            else -> 14f
        }
        val t = when (playbackClip) {
            PlatformerAnimClip.DIE -> player.deathAnimTime
            PlatformerAnimClip.ATTACK, PlatformerAnimClip.JUMP_ATTACK ->
                PlatformerCombat.attackElapsedSec(player)
            PlatformerAnimClip.SHOOT, PlatformerAnimClip.RUN_SHOOT,
            PlatformerAnimClip.JUMP_SHOOT, PlatformerAnimClip.THROW,
            -> PlatformerRangedCombat.rangedElapsedSec(player)
            else -> animTime
        }
        val idx = when {
            playbackClip == PlatformerAnimClip.DIE ->
                PlatformerPlayerSprites.dieFrameIndex(player.deathAnimTime, sheet.frameCount, rate)
            playbackClip == PlatformerAnimClip.ATTACK || playbackClip == PlatformerAnimClip.JUMP_ATTACK ->
                (t * rate).toInt().coerceIn(0, (sheet.frameCount - 1).coerceAtLeast(0))
            playbackClip == PlatformerAnimClip.SHOOT || playbackClip == PlatformerAnimClip.RUN_SHOOT ||
                playbackClip == PlatformerAnimClip.JUMP_SHOOT || playbackClip == PlatformerAnimClip.THROW ->
                (t * rate).toInt().coerceIn(0, (sheet.frameCount - 1).coerceAtLeast(0))
            playbackClip != clip && clip == PlatformerAnimClip.IDLE -> 0
            else -> (t * rate).toInt().mod(sheet.frameCount.coerceAtLeast(1))
        }
        return CatalogSheetDraw(
            sheet = sheet,
            clip = playbackClip,
            frameIndex = idx.coerceIn(0, (sheet.frameCount - 1).coerceAtLeast(0)),
        )
    }

    private fun resolveSheetPlayback(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        preferred: PlatformerAnimClip,
    ): Pair<PlatformerAnimClip, PacMazeSkinSheetPlayback>? {
        playbackSheet(assetKey, preferred)?.let { return preferred to it }
        if (preferred == PlatformerAnimClip.DIE) return null
        val fallbacks = buildList {
            if (preferred == PlatformerAnimClip.IDLE) {
                if (PlatformerAnimClip.RUN in cfg.clips) add(PlatformerAnimClip.RUN)
                if (PlatformerAnimClip.WALK in cfg.clips) add(PlatformerAnimClip.WALK)
                add(primaryClip(cfg))
            }
            addAll(cfg.clips.filter { it != preferred })
        }.distinct()
        for (fallback in fallbacks) {
            playbackSheet(assetKey, fallback)?.let { return fallback to it }
        }
        return null
    }

    private fun sheetKey(assetKey: String, clip: PlatformerAnimClip): String =
        "$assetKey:${clip.name.lowercase()}"

    private fun hasAnySheet(assetRoot: String): Boolean {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot) ?: return false
        return manifest.clips.values.any { it.sheet != null }
    }

    private fun resolveSheetAsset(
        assetRoot: String,
        clip: PlatformerAnimClip,
    ): Pair<String, PacMazeSkinAnimManifest.SheetSpec>? {
        val manifest = PacMazeSkinAnimManifest.load(assetRoot) ?: return null
        val sheet = manifest.clipSheetByKey(clip.name.lowercase()) ?: return null
        val folder = manifest.clipFolderByKey(clip.name.lowercase())
        val path = "$assetRoot/$folder/${sheet.file}"
        if (!ResourceStore.resourceExists(path)) return null
        return path to sheet
    }

    private fun platformerSheetSampleSize(
        cfg: PlatformerAnimConfig,
        manifest: PacMazeSkinAnimManifest.SkinAnimManifest,
    ): Int {
        val cellH = manifest.canvas?.h ?: 256
        var sample = 1
        while (cellH / sample > 128) sample *= 2
        return sample.coerceIn(1, 16)
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

    private val decodeTag: String
        get() = ResourceStore.platformerDecodeTag()

    fun register(entry: PlatformerContentCatalog.CharacterEntry) {
        if (entry.isPacMazeSource || entry.isLocalApkSource) return
        val clips = loadManifestClips(entry.assetRoot)
        configCache[entry.id] = PlatformerAnimConfig(
            assetRoot = entry.assetRoot,
            clips = clips,
            syncWalkCycleToSprite = true,
            mirrorDefault = entry.render.mirrorDefault,
            heightCellFrac = entry.render.heightCellFrac,
        )
    }

    fun registerEnemy(entry: PlatformerContentCatalog.EnemyEntry) {
        val clips = loadManifestClips(entry.assetRoot)
        val manifest = PacMazeSkinAnimManifest.load(entry.assetRoot)
        configCache[entry.id] = PlatformerAnimConfig(
            assetRoot = entry.assetRoot,
            clips = clips,
            syncWalkCycleToSprite = manifest?.render?.syncWalkCycleToSprite ?: true,
            sampleSize = manifest?.render?.sampleSize ?: 1,
            heightCellFrac = 0.85f,
        )
    }

    fun bootstrapFromCatalog() {
        val catalog = PlatformerContentCatalog.load() ?: return
        catalog.characters.filter { it.isRemoteBundle }.forEach { register(it) }
        catalog.enemies.forEach { registerEnemy(it) }
    }

    fun invalidateDiskCache() {
        clipCache.clear()
        sheetPlaybackCache.clear()
        ResourceStore.invalidatePlatformerDecodedCache()
    }

    fun config(characterId: PlatformerCharacterId): PlatformerAnimConfig? =
        configCache[characterId.catalogId]

    fun configById(catalogId: String): PlatformerAnimConfig? = configCache[catalogId]

    fun playbackFrames(assetKey: String, clip: PlatformerAnimClip): List<ImageBitmap>? =
        clipCache[assetKey]?.get(clip)?.takeIf { it.isNotEmpty() }

    fun playbackFrameCount(assetKey: String, clip: PlatformerAnimClip): Int =
        playbackFrames(assetKey, clip)?.size ?: 0

    fun isReady(characterId: PlatformerCharacterId): Boolean = isPlayableReady(characterId)

    fun isBootstrapPlayable(characterId: PlatformerCharacterId): Boolean {
        if (isSheetBootstrapPlayable(characterId)) return true
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        return playbackFrameCount(key, primaryClip(cfg)) >= MIN_WALK_FRAMES
    }

    suspend fun preparePlayableBootstrap(characterId: PlatformerCharacterId): Boolean {
        if (!ResourceStore.isPacMazeBundleReady("platformer_characters")) {
            withContext(Dispatchers.IO) {
                runCatching { ResourceStore.ensureBundle("platformer_characters") }
            }
        }
        if (prepareSheetsForBoot(characterId)) return true
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        ensureClipFull(key, cfg, primaryClip(cfg))
        if (PlatformerAnimClip.JUMP in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.JUMP)
        }
        if (PlatformerAnimClip.IDLE in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.IDLE)
        }
        val ready = isBootstrapPlayable(characterId)
        if (ready) {
            scope.launch { runCatching { preparePlayable(characterId) } }
        }
        return ready
    }

    fun isPlayableReady(characterId: PlatformerCharacterId): Boolean {
        if (isSheetBootstrapPlayable(characterId)) return true
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        val primary = primaryClip(cfg)
        if (!isClipFullyLoaded(key, cfg, primary)) return false
        if (PlatformerAnimClip.JUMP in cfg.clips &&
            !isClipFullyLoaded(key, cfg, PlatformerAnimClip.JUMP)
        ) {
            return false
        }
        return true
    }

    fun isDiskPlayableReady(characterId: PlatformerCharacterId): Boolean {
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        val primary = primaryClip(cfg)
        if (!isDiskClipComplete(key, cfg, primary)) return false
        if (PlatformerAnimClip.JUMP in cfg.clips &&
            !isDiskClipComplete(key, cfg, PlatformerAnimClip.JUMP)
        ) {
            return false
        }
        return true
    }

    /** 磁盘全量：primary + jump + die 均已 decode 落盘。 */
    fun isDiskFullyReady(characterId: PlatformerCharacterId): Boolean {
        val cfg = config(characterId) ?: return false
        if (hasAnySheet(cfg.assetRoot)) {
            return isSheetBootstrapPlayable(characterId)
        }
        val key = characterId.catalogId
        val primary = primaryClip(cfg)
        if (!isDiskClipComplete(key, cfg, primary)) return false
        if (PlatformerAnimClip.JUMP in cfg.clips &&
            !isDiskClipComplete(key, cfg, PlatformerAnimClip.JUMP)
        ) {
            return false
        }
        if (PlatformerAnimClip.DIE in cfg.clips &&
            !isDiskClipComplete(key, cfg, PlatformerAnimClip.DIE)
        ) {
            return false
        }
        return true
    }

    fun clipManifestFrameCount(characterId: PlatformerCharacterId, clip: PlatformerAnimClip): Int {
        val key = characterId.catalogId
        diskClipFrameCount(key, clip).takeIf { it > 0 }?.let { return it }
        playbackSheet(key, clip)?.frameCount?.takeIf { it > 0 }?.let { return it }
        val cfg = config(characterId) ?: return defaultClipFrameCount(clip)
        val manifest = loadManifest(cfg.assetRoot) ?: return defaultClipFrameCount(clip)
        return manifest.clips[clip.folder]?.count
            ?: manifest.clips[clip.name.lowercase()]?.count
            ?: defaultClipFrameCount(clip)
    }

    private fun defaultClipFrameCount(clip: PlatformerAnimClip): Int = when (clip) {
        PlatformerAnimClip.JUMP_ATTACK -> 10
        PlatformerAnimClip.ATTACK -> 8
        else -> 8
    }

    fun diskClipFrameCount(assetKey: String, clip: PlatformerAnimClip): Int {
        val cfg = configCache[assetKey] ?: return 0
        val dir = diskClipDir(assetKey, clip)
        return DecodedClipDiskIndex.frameCount(dir, decodeTag)
    }

    fun diskClipFrameCount(characterId: PlatformerCharacterId, clip: PlatformerAnimClip): Int =
        diskClipFrameCount(characterId.catalogId, clip)

    /** 仅写磁盘 decode（批量预热），完成后可按需释放内存 clip。 */
    suspend fun prepareDiskFull(characterId: PlatformerCharacterId): Boolean {
        if (!ResourceStore.isPacMazeBundleReady("platformer_characters")) {
            withContext(Dispatchers.IO) {
                runCatching { ResourceStore.ensureBundle("platformer_characters") }
            }
        }
        val cfg = config(characterId) ?: return false
        if (hasAnySheet(cfg.assetRoot)) {
            prepareSheetsForBoot(characterId)
            if (PlatformerAnimClip.DIE in cfg.clips) {
                requestSheetPlaybackAsync(characterId, PlatformerAnimClip.DIE)
            }
            return isSheetBootstrapPlayable(characterId)
        }
        val key = characterId.catalogId
        ensureClipOnDisk(key, cfg, primaryClip(cfg))
        if (PlatformerAnimClip.JUMP in cfg.clips) {
            ensureClipOnDisk(key, cfg, PlatformerAnimClip.JUMP)
        }
        if (PlatformerAnimClip.DIE in cfg.clips) {
            ensureClipOnDisk(key, cfg, PlatformerAnimClip.DIE)
        }
        if (!PlatformerAnimMemoryPool.shouldRetainInMemory(characterId)) {
            releaseCharacterMemory(characterId)
        }
        return isDiskFullyReady(characterId)
    }

    /** 磁盘已全量时，并行 hydrate 进内存（切角 fast path）。 */
    suspend fun preparePlayableFromDisk(characterId: PlatformerCharacterId): Boolean {
        if (!isDiskFullyReady(characterId)) return preparePlayable(characterId)
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        ensureClipFull(key, cfg, primaryClip(cfg))
        if (PlatformerAnimClip.JUMP in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.JUMP)
        }
        if (PlatformerAnimClip.DIE in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.DIE)
        }
        if (PlatformerAnimClip.IDLE in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.IDLE)
        }
        return isPlayableReady(characterId)
    }

    fun releaseCharacterMemory(characterId: PlatformerCharacterId) {
        val key = characterId.catalogId
        clipCache.remove(key)
        sheetPlaybackCache.keys.filter { it.startsWith("$key:") }.forEach { sheetPlaybackCache.remove(it) }
        PlatformerSpriteAtlasCache.invalidateCharacter(characterId)
    }

    fun releaseMemoryClip(assetKey: String, clip: PlatformerAnimClip) {
        clipCache[assetKey]?.remove(clip)
    }

    suspend fun preparePlayable(characterId: PlatformerCharacterId): Boolean {
        if (!ResourceStore.isPacMazeBundleReady("platformer_characters")) {
            withContext(Dispatchers.IO) {
                runCatching { ResourceStore.ensureBundle("platformer_characters") }
            }
        }
        val cfg = config(characterId) ?: return false
        val key = characterId.catalogId
        ensureClipFull(key, cfg, primaryClip(cfg))
        if (PlatformerAnimClip.JUMP in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.JUMP)
        }
        if (PlatformerAnimClip.IDLE in cfg.clips) {
            ensureClipFull(key, cfg, PlatformerAnimClip.IDLE)
        }
        return isPlayableReady(characterId)
    }

    fun requestWarmup(characterId: PlatformerCharacterId) {
        scope.launch { runCatching { preparePlayable(characterId) } }
    }

    fun requestEnemySheetPlaybackAsync(catalogId: String, clip: PlatformerAnimClip) {
        val cfg = configById(catalogId) ?: return
        if (clip !in cfg.clips) return
        if (playbackSheet(catalogId, clip) != null) return
        scope.launch { runCatching { ensureSheetPlayback(catalogId, cfg, clip) } }
    }

    fun resolveEnemySheetDraw(
        catalogId: String,
        behavior: PlatformerEnemyBehavior,
        animPhase: Float,
    ): CatalogSheetDraw? {
        val cfg = configById(catalogId) ?: return null
        if (!hasAnySheet(cfg.assetRoot)) return null
        val clip = pickEnemyClip(cfg, behavior)
        requestEnemySheetPlaybackAsync(catalogId, clip)
        val resolved = resolveSheetPlayback(catalogId, cfg, clip) ?: return null
        val (playbackClip, sheet) = resolved
        val rate = when (playbackClip) {
            PlatformerAnimClip.DIE -> 18f
            else -> 10f
        }
        val idx = (animPhase * rate).toInt().mod(sheet.frameCount.coerceAtLeast(1))
        return CatalogSheetDraw(
            sheet = sheet,
            clip = playbackClip,
            frameIndex = idx.coerceIn(0, (sheet.frameCount - 1).coerceAtLeast(0)),
        )
    }

    suspend fun prepareEnemySheets(catalogId: String): Boolean {
        if (!ResourceStore.isPacMazeBundleReady("platformer_characters")) {
            withContext(Dispatchers.IO) {
                runCatching { ResourceStore.ensureBundle("platformer_characters") }
            }
        }
        val cfg = configById(catalogId) ?: return false
        if (!hasAnySheet(cfg.assetRoot)) return false
        ensureSheetPlayback(catalogId, cfg, primaryClip(cfg))
        return playbackSheet(catalogId, primaryClip(cfg)) != null
    }

    fun requestEnemyWarmup(catalogId: String) {
        val cfg = configById(catalogId) ?: return
        if (hasAnySheet(cfg.assetRoot)) {
            scope.launch { runCatching { prepareEnemySheets(catalogId) } }
            return
        }
        scope.launch { runCatching { ensureClipFull(catalogId, cfg, primaryClip(cfg)) } }
    }

    fun pickEnemyClip(cfg: PlatformerAnimConfig, behavior: PlatformerEnemyBehavior): PlatformerAnimClip {
        val clips = cfg.clips
        return when (behavior) {
            PlatformerEnemyBehavior.FLY, PlatformerEnemyBehavior.FLOAT -> when {
                PlatformerAnimClip.FLY in clips -> PlatformerAnimClip.FLY
                PlatformerAnimClip.WALK in clips -> PlatformerAnimClip.WALK
                PlatformerAnimClip.RUN in clips -> PlatformerAnimClip.RUN
                PlatformerAnimClip.IDLE in clips -> PlatformerAnimClip.IDLE
                else -> clips.first()
            }
            else -> when {
                PlatformerAnimClip.WALK in clips -> PlatformerAnimClip.WALK
                PlatformerAnimClip.RUN in clips -> PlatformerAnimClip.RUN
                PlatformerAnimClip.IDLE in clips -> PlatformerAnimClip.IDLE
                else -> clips.first()
            }
        }
    }

    suspend fun warmup(characterId: PlatformerCharacterId) {
        val cfg = config(characterId) ?: return
        if (hasAnySheet(cfg.assetRoot)) {
            prepareSheetsForBoot(characterId)
            if (PlatformerAnimClip.DIE in cfg.clips) {
                ensureSheetPlayback(characterId.catalogId, cfg, PlatformerAnimClip.DIE)
            }
            return
        }
        preparePlayable(characterId)
        if (PlatformerAnimClip.DIE in cfg.clips) {
            ensureClipFull(characterId.catalogId, cfg, PlatformerAnimClip.DIE)
        }
        if (PlatformerAnimClip.IDLE in cfg.clips) {
            ensureClipFull(characterId.catalogId, cfg, PlatformerAnimClip.IDLE)
        }
    }

    fun requestClipAsync(characterId: PlatformerCharacterId, clip: PlatformerAnimClip) {
        val cfg = config(characterId) ?: return
        if (clip !in cfg.clips) return
        if (playbackFrameCount(characterId.catalogId, clip) > 0) return
        scope.launch {
            runCatching { ensureClipFull(characterId.catalogId, cfg, clip) }
        }
    }

    fun pickClip(cfg: PlatformerAnimConfig, player: PlatformerPlayer): PlatformerAnimClip {
        val clips = cfg.clips
        return when {
            player.dying && PlatformerAnimClip.DIE in clips -> PlatformerAnimClip.DIE
            player.rangedAnimSecLeft > 0f -> player.rangedClip?.toAnimClip()
                ?: pickRangedClipForAnim(cfg, player)
            player.attackAnimSecLeft > 0f -> when {
                player.attackJumpVariant && PlatformerAnimClip.JUMP_ATTACK in clips ->
                    PlatformerAnimClip.JUMP_ATTACK
                PlatformerAnimClip.ATTACK in clips -> PlatformerAnimClip.ATTACK
                else -> primaryClip(cfg)
            }
            !player.grounded && PlatformerAnimClip.JUMP in clips -> PlatformerAnimClip.JUMP
            player.locomoting && PlatformerAnimClip.RUN in clips -> PlatformerAnimClip.RUN
            player.locomoting && PlatformerAnimClip.WALK in clips -> PlatformerAnimClip.WALK
            PlatformerAnimClip.IDLE in clips -> PlatformerAnimClip.IDLE
            PlatformerAnimClip.RUN in clips -> PlatformerAnimClip.RUN
            else -> clips.first()
        }
    }

    private fun pickRangedClipForAnim(cfg: PlatformerAnimConfig, player: PlatformerPlayer): PlatformerAnimClip {
        val clips = cfg.clips
        return when {
            player.rangedJumpVariant && PlatformerAnimClip.JUMP_SHOOT in clips ->
                PlatformerAnimClip.JUMP_SHOOT
            player.rangedRunVariant && PlatformerAnimClip.RUN_SHOOT in clips ->
                PlatformerAnimClip.RUN_SHOOT
            PlatformerAnimClip.SHOOT in clips -> PlatformerAnimClip.SHOOT
            PlatformerAnimClip.THROW in clips -> PlatformerAnimClip.THROW
            else -> primaryClip(cfg)
        }
    }

    fun resolveFrame(
        characterId: PlatformerCharacterId,
        player: PlatformerPlayer,
        animTime: Float,
    ): ImageBitmap? {
        val cfg = config(characterId) ?: return null
        val key = characterId.catalogId
        val clip = pickClip(cfg, player)
        val resolved = resolvePlaybackFrames(key, cfg, clip)
            ?: run {
                requestClipAsync(characterId, clip)
                requestWarmup(characterId)
                return null
            }
        val (playbackClip, frames) = resolved
        val rate = when (playbackClip) {
            PlatformerAnimClip.DIE -> 24f
            PlatformerAnimClip.JUMP -> 20f
            PlatformerAnimClip.ATTACK, PlatformerAnimClip.JUMP_ATTACK -> PlatformerCombat.ATTACK_ANIM_FPS
            PlatformerAnimClip.SHOOT, PlatformerAnimClip.RUN_SHOOT,
            PlatformerAnimClip.JUMP_SHOOT, PlatformerAnimClip.THROW,
            -> PlatformerRangedCombat.RANGED_ANIM_FPS
            else -> 14f
        }
        val t = when (playbackClip) {
            PlatformerAnimClip.DIE -> player.deathAnimTime
            PlatformerAnimClip.ATTACK, PlatformerAnimClip.JUMP_ATTACK ->
                PlatformerCombat.attackElapsedSec(player)
            PlatformerAnimClip.SHOOT, PlatformerAnimClip.RUN_SHOOT,
            PlatformerAnimClip.JUMP_SHOOT, PlatformerAnimClip.THROW,
            -> PlatformerRangedCombat.rangedElapsedSec(player)
            else -> animTime
        }
        val idx = when {
            playbackClip == PlatformerAnimClip.DIE ->
                PlatformerPlayerSprites.dieFrameIndex(player.deathAnimTime, frames.size, rate)
            playbackClip == PlatformerAnimClip.ATTACK || playbackClip == PlatformerAnimClip.JUMP_ATTACK ||
                playbackClip == PlatformerAnimClip.SHOOT || playbackClip == PlatformerAnimClip.RUN_SHOOT ||
                playbackClip == PlatformerAnimClip.JUMP_SHOOT || playbackClip == PlatformerAnimClip.THROW ->
                (t * rate).toInt().coerceIn(0, frames.lastIndex.coerceAtLeast(0))
            playbackClip != clip && clip == PlatformerAnimClip.IDLE -> 0
            else -> (t * rate).toInt().mod(frames.size)
        }
        return frames[idx.coerceIn(0, frames.lastIndex)]
    }

    /** 优先 pose 对应 clip；idle 未加载时回退到 run/walk，避免站立时橙色占位块。 */
    private fun resolvePlaybackFrames(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        preferred: PlatformerAnimClip,
    ): Pair<PlatformerAnimClip, List<ImageBitmap>>? {
        playbackFrames(assetKey, preferred)?.takeIf { it.isNotEmpty() }?.let { return preferred to it }
        val fallbacks = buildList {
            if (preferred == PlatformerAnimClip.IDLE) {
                if (PlatformerAnimClip.RUN in cfg.clips) add(PlatformerAnimClip.RUN)
                if (PlatformerAnimClip.WALK in cfg.clips) add(PlatformerAnimClip.WALK)
                add(primaryClip(cfg))
            }
            addAll(cfg.clips.filter { it != preferred })
        }.distinct()
        for (fallback in fallbacks) {
            playbackFrames(assetKey, fallback)?.takeIf { it.isNotEmpty() }?.let { return fallback to it }
        }
        return null
    }

    private fun primaryClip(cfg: PlatformerAnimConfig): PlatformerAnimClip = when {
        PlatformerAnimClip.WALK in cfg.clips -> PlatformerAnimClip.WALK
        PlatformerAnimClip.RUN in cfg.clips -> PlatformerAnimClip.RUN
        PlatformerAnimClip.IDLE in cfg.clips -> PlatformerAnimClip.IDLE
        else -> cfg.clips.first()
    }

    private suspend fun ensureClipOnDisk(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        clip: PlatformerAnimClip,
    ) {
        if (hasAnySheet(cfg.assetRoot)) return
        if (isDiskClipComplete(assetKey, cfg, clip)) return
        if (!encodeClipToDiskFromZip(assetKey, cfg, clip)) {
            ensureClip(assetKey, cfg, clip)
        }
        catalogIdToCharacterId(assetKey)?.let { charId ->
            if (!PlatformerAnimMemoryPool.shouldRetainInMemory(charId)) {
                releaseMemoryClip(assetKey, clip)
            }
        }
    }

    /** zip PNG 并行解码 → WebP 落盘，不常驻内存。 */
    private suspend fun encodeClipToDiskFromZip(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        clip: PlatformerAnimClip,
    ): Boolean = withContext(Dispatchers.IO) {
        if (isDiskClipComplete(assetKey, cfg, clip)) return@withContext true
        val mutex = clipMutex.getOrPut("$assetKey:${clip.name}") { Mutex() }
        mutex.withLock {
            if (isDiskClipComplete(assetKey, cfg, clip)) return@withLock true
            val manifest = loadManifest(cfg.assetRoot) ?: return@withLock false
            val entry = manifest.clips[clip.folder] ?: manifest.clips[clip.name.lowercase()] ?: return@withLock false
            val count = entry.count
            if (count <= 0) return@withLock false
            val folder = entry.folder ?: clip.folder
            val prefix = entry.prefix ?: clip.prefix
            val paths = (1..count).map { i -> "${cfg.assetRoot}/$folder/${prefix}_$i.png" }
            ResourceStore.prefetchResourceExists(paths)

            val dir = diskClipDir(assetKey, clip)
            dir.deleteRecursively()
            dir.mkdirs()
            val written = mutableListOf<File>()
            val compressFormat = if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                android.graphics.Bitmap.CompressFormat.WEBP
            }
            val workers = ParallelBitmapDecoder.batchWorkers()
            val semaphore = Semaphore(workers)

            coroutineScope {
                paths.mapIndexed { index, path ->
                    async {
                        semaphore.withPermit {
                            decode(path)?.let { frame ->
                                val out = File(dir, "${index + 1}.webp")
                                frame.asAndroidBitmap().compress(compressFormat, 90, out.outputStream())
                                synchronized(written) { written.add(out) }
                            }
                        }
                    }
                }.awaitAll()
            }

            if (written.size < count) return@withLock false
            written.sortBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }
            DecodedClipDiskIndex.writeMetaFromFiles(dir, decodeTag, written, DecodedClipDiskIndex.FORMAT_WEBP)
            true
        }
    }

    private fun catalogIdToCharacterId(catalogId: String): PlatformerCharacterId? =
        com.example.funlife.game.platformer.catalog.catalogIdToCharacterId(catalogId)

    private suspend fun ensureClipFull(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        clip: PlatformerAnimClip,
    ) {
        if (isClipFullyLoaded(assetKey, cfg, clip)) return
        hydrateClipFromDisk(assetKey, cfg, clip)
        if (isClipFullyLoaded(assetKey, cfg, clip)) return
        ensureClip(assetKey, cfg, clip)
    }

    private suspend fun ensureClip(assetKey: String, cfg: PlatformerAnimConfig, clip: PlatformerAnimClip) {
        if (!playbackFrames(assetKey, clip).isNullOrEmpty()) return
        val mutex = clipMutex.getOrPut("$assetKey:${clip.name}") { Mutex() }
        mutex.withLock {
            if (!playbackFrames(assetKey, clip).isNullOrEmpty()) return
            val manifest = loadManifest(cfg.assetRoot) ?: return
            val entry = manifest.clips[clip.folder] ?: manifest.clips[clip.name.lowercase()] ?: return
            val count = entry.count
            if (count <= 0) return
            val folder = entry.folder ?: clip.folder
            val prefix = entry.prefix ?: clip.prefix
            val paths = (1..count).map { i -> "${cfg.assetRoot}/$folder/${prefix}_$i.png" }
            ResourceStore.prefetchResourceExists(paths)
            val decoded = withContext(Dispatchers.IO) {
                buildList {
                    paths.forEach { path ->
                        decode(path)?.let { add(it) }
                    }
                }
            }
            if (decoded.isNotEmpty()) {
                clipCache.getOrPut(assetKey) { ConcurrentHashMap() }[clip] = decoded
                saveClipToDisk(assetKey, clip, decoded)
            }
        }
    }

    private fun isClipFullyLoaded(assetKey: String, cfg: PlatformerAnimConfig, clip: PlatformerAnimClip): Boolean {
        val target = clipFrameTarget(cfg, clip)
        return target > 0 && playbackFrameCount(assetKey, clip) >= target
    }

    private fun isDiskClipComplete(assetKey: String, cfg: PlatformerAnimConfig, clip: PlatformerAnimClip): Boolean {
        val target = clipFrameTarget(cfg, clip)
        if (target <= 0) return false
        return DecodedClipDiskIndex.isComplete(diskClipDir(assetKey, clip), target, decodeTag)
    }

    private fun clipFrameTarget(cfg: PlatformerAnimConfig, clip: PlatformerAnimClip): Int {
        val manifest = loadManifest(cfg.assetRoot) ?: return 0
        val entry = manifest.clips[clip.folder] ?: manifest.clips[clip.name.lowercase()] ?: return 0
        return entry.count
    }

    private suspend fun hydrateClipFromDisk(
        assetKey: String,
        cfg: PlatformerAnimConfig,
        clip: PlatformerAnimClip,
    ) {
        val target = clipFrameTarget(cfg, clip)
        if (target <= 0) return
        val dir = diskClipDir(assetKey, clip)
        if (!DecodedClipDiskIndex.isComplete(dir, target, decodeTag)) return
        if (isClipFullyLoaded(assetKey, cfg, clip)) return
        val files = DecodedClipDiskIndex.listFrameFiles(dir, decodeTag) ?: return
        val frames = ParallelBitmapDecoder.decodeFilesParallel(files)
        if (frames.size >= target) {
            clipCache.getOrPut(assetKey) { ConcurrentHashMap() }[clip] = frames
        }
    }

    private fun diskClipDir(assetKey: String, clip: PlatformerAnimClip): File =
        File(ResourceStore.decodedPlatformerCacheRoot(), "$decodeTag/$assetKey/${clip.folder}")

    private fun saveClipToDisk(assetKey: String, clip: PlatformerAnimClip, frames: List<ImageBitmap>) {
        runCatching {
            val dir = diskClipDir(assetKey, clip)
            dir.deleteRecursively()
            dir.mkdirs()
            val written = mutableListOf<File>()
            val compressFormat = if (Build.VERSION.SDK_INT >= 30) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            frames.forEachIndexed { index, frame ->
                val out = File(dir, "${index + 1}.webp")
                frame.asAndroidBitmap().compress(compressFormat, 90, out.outputStream())
                written.add(out)
            }
            DecodedClipDiskIndex.writeMetaFromFiles(dir, decodeTag, written, DecodedClipDiskIndex.FORMAT_WEBP)
        }.onFailure { Log.w(TAG, "saveClipToDisk failed $assetKey/${clip.folder}", it) }
    }

    private fun decode(path: String): ImageBitmap? = runCatching {
        ResourceStore.openInputStream(path)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()

    private fun loadManifestClips(assetRoot: String): Set<PlatformerAnimClip> {
        val manifest = loadManifest(assetRoot) ?: return emptySet()
        return manifest.clips.keys.mapNotNull { PlatformerAnimClip.fromManifestKey(it) }.toSet()
    }

    private fun loadManifest(assetRoot: String): PlatformerSkinManifest? {
        val stream = ResourceStore.openInputStream("$assetRoot/anim_manifest.json") ?: return null
        return stream.use {
            runCatching {
                parseManifest(it.bufferedReader().readText())
            }.getOrNull()
        }
    }

    private data class ManifestClip(val count: Int, val folder: String?, val prefix: String?)
    private data class PlatformerSkinManifest(val clips: Map<String, ManifestClip>)

    private fun parseManifest(json: String): PlatformerSkinManifest {
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val clipsObj = root.getAsJsonObject("clips") ?: return PlatformerSkinManifest(emptyMap())
        val clips = clipsObj.entrySet().associate { (name, el) ->
            val obj = el.asJsonObject
            name.lowercase() to ManifestClip(
                count = obj.get("count")?.asInt ?: 0,
                folder = obj.get("folder")?.asString,
                prefix = obj.get("prefix")?.asString,
            )
        }
        return PlatformerSkinManifest(clips)
    }
}
