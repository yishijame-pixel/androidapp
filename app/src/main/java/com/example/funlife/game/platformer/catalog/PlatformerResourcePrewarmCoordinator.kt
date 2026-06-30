package com.example.funlife.game.platformer.catalog

import android.content.Context
import com.example.funlife.game.platformer.PlatformerCharacterAssetsLoader
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerCharacterPrefs
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.resource.GameResourceBundles
import com.example.funlife.resource.ParallelBitmapDecoder
import com.example.funlife.resource.PlatformerClipCacheIndex
import com.example.funlife.resource.PlatformerDecodeStampStore
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.platformer.GameResourceLoadCopy
import com.example.funlife.ui.screens.platformer.PlatformerBootCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 横版动画解码协调器 v3：
 * - **快速通道**：地图 + 默认角色磁盘 encode（直写 WebP，不进内存）→ 约 10–25s 可玩
 * - **后台并行**：其余 catalog 角色 3 路并发 decode
 */
object PlatformerResourcePrewarmCoordinator {

    private const val BACKGROUND_CHARACTER_PARALLELISM = 3

    data class PrewarmState(
        val running: Boolean = false,
        val backgroundRunning: Boolean = false,
        val phase: String = "",
        val percent: Int = 0,
        val completedSteps: Int = 0,
        val totalSteps: Int = 0,
        /** 默认角色 + 地图已就绪，可先玩横版 */
        val minimumPlayableReady: Boolean = false,
        val allCharactersReady: Boolean = false,
        val defaultCharacterReady: Boolean = false,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runMutex = Mutex()

    private val _state = MutableStateFlow(PrewarmState())
    val state: StateFlow<PrewarmState> = _state.asStateFlow()

    private val remoteCatalogCharacters: List<PlatformerCharacterId>
        get() = PlatformerCharacterId.entries.filter { it.isCatalogRemote }

    private val localCharacters: List<PlatformerCharacterId>
        get() = listOf(
            PlatformerCharacterId.TREASURE_HUNTER,
            PlatformerCharacterId.PIXEL_WALKER,
        )

    fun scheduleAfterBundlesReady(context: Context) {
        if (!ResourceStore.isAssetSourceConfigured()) return
        if (ResourceStore.localGameResourceStatus().pendingBundleIds.isNotEmpty()) return
        scope.launch {
            if (!needsDecodePrewarm(context)) {
                persistDecodeStampIfComplete(context)
                emitIdleComplete()
                return@launch
            }
            runDecodePrewarm(context.applicationContext)
        }
    }

    suspend fun runDecodePrewarmBlocking(context: Context) {
        runDecodePrewarm(context.applicationContext)
    }

    fun reset() {
        _state.value = PrewarmState()
        PlatformerBootCache.resetPrewarmSession()
        PlatformerAnimMemoryPool.reset()
        PlatformerSpriteAtlasCache.invalidateAll()
    }

    fun needsDecodePrewarm(context: Context): Boolean {
        if (ResourceStore.localGameResourceStatus().pendingBundleIds.isNotEmpty()) return false
        val app = context.applicationContext
        val saved = PlatformerDecodeStampStore.loadV3(app)
        if (saved != null && PlatformerDecodeStampStore.isFullyComplete(saved)) {
            return false
        }
        PlatformerAssetService.ensureInitialized(context)
        if (needsChickSheetWarm()) return true
        if (needsChickDiskDecode()) return true
        if (needsMapAssetsWarm()) return true
        if (needsLocalCharactersLoad()) return true
        if (remoteCatalogCharacters.any { !PlatformerRemoteAnimCache.isDiskFullyReady(it) }) return true
        return false
    }

    private fun needsChickDiskDecode(): Boolean {
        val skinId = PlatformerPlayerSprites.skinId
        if (PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(skinId)) return false
        return ResourceStore.isPacMazeBundleReady(GameResourceBundles.SKINS) &&
            !PacMazeRemoteSkinAnimCache.isPlatformerSkinFullyOnDisk(skinId)
    }

    private fun needsChickSheetWarm(): Boolean {
        val skinId = PlatformerPlayerSprites.skinId
        return PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(skinId) &&
            !PacMazeRemoteSkinAnimCache.isSheetBootstrapPlayable(skinId)
    }

    private fun needsMapAssetsWarm(): Boolean = !PlatformerBootCache.hasAssets()

    private fun needsLocalCharactersLoad(): Boolean =
        !PlatformerBootCache.areLocalCharactersLoaded()

    private suspend fun runDecodePrewarm(context: Context) = runMutex.withLock {
        if (_state.value.running) return@withLock
        _state.value = PrewarmState(running = true, phase = GameResourceLoadCopy.forDisplay("检查游戏资源"), percent = 1)
        try {
            if (!needsDecodePrewarm(context)) {
                persistDecodeStampIfComplete(context)
                emitIdleComplete()
                return@withLock
            }

            PlatformerAssetService.ensureInitialized(context)
            val preferred = PlatformerCharacterPrefs.get(context)
            val fastTotal = computeFastTrackSteps(preferred)
            var fastDone = 0
            var minimumReady = false

            ParallelBitmapDecoder.withBatchBoost {
                // ── 快速通道：先让用户能玩 ──
                if (needsMapAssetsWarm()) {
                    stepFast("预热坤坤大冒险地图", fastDone++, fastTotal, 20)
                    PlatformerBootCache.warmMapAssets(context)
                }

                if (needsLocalCharactersLoad()) {
                    stepFast("加载本地角色", fastDone++, fastTotal, 40)
                    withContext(Dispatchers.IO) {
                        runCatching { PlatformerCharacterAssetsLoader.load(context) }
                    }
                    localCharacters.forEach { PlatformerBootCache.markPlayable(it) }
                    PlatformerBootCache.markLocalCharactersLoaded()
                }

                if (needsChickSheetWarm()) {
                    stepFast("加载行走小鸡资源", fastDone++, fastTotal, 20)
                    withContext(Dispatchers.IO) {
                        runCatching {
                            PacMazeRemoteSkinAnimCache.preparePlatformerSheetsForBoot(PlatformerPlayerSprites.skinId) { status ->
                                emitRunning(
                                    "加载行走小鸡 · ${status.message}",
                                    12 + (status.percent * 28 / 100),
                                    fastDone,
                                    fastTotal,
                                )
                            }
                        }
                    }
                    if (PacMazeRemoteSkinAnimCache.isSheetBootstrapPlayable(PlatformerPlayerSprites.skinId)) {
                        PlatformerBootCache.markPlayable(PlatformerCharacterId.CHICK_PRO_MAX)
                    }
                } else if (needsChickDiskDecode()) {
                    stepFast("整理行走小鸡资源", fastDone++, fastTotal, 8)
                    runCatching {
                        PacMazeRemoteSkinAnimCache.preparePlatformerSkinDiskOnly(PlatformerPlayerSprites.skinId) { status ->
                            emitRunning(
                                "整理行走小鸡 · ${status.message}",
                                8 + (status.percent * 35 / 100),
                                fastDone,
                                fastTotal,
                            )
                        }
                    }
                    if (PacMazeRemoteSkinAnimCache.isPlatformerSkinFullyOnDisk(PlatformerPlayerSprites.skinId)) {
                        recordChickClips(context)
                        PlatformerBootCache.markPlayable(PlatformerCharacterId.CHICK_PRO_MAX)
                    }
                }

                if (preferred.isCatalogRemote && !PlatformerRemoteAnimCache.isDiskFullyReady(preferred)) {
                    stepFast("整理 ${preferred.displayTitle} 资源", fastDone++, fastTotal, 10)
                    runCatching { PlatformerRemoteAnimCache.prepareDiskFull(preferred) }
                    recordCatalogClips(context, preferred)
                    PlatformerBootCache.markPlayable(preferred)
                }

                minimumReady = isMinimumPlayable(context, preferred)
                if (minimumReady) {
                    PlatformerAnimMemoryPool.onCharacterFocused(preferred)
                    PlatformerCharacterPrefetch.prefetchOnSelect(preferred)
                    _state.value = _state.value.copy(
                        minimumPlayableReady = true,
                        defaultCharacterReady = true,
                        phase = "可先游玩，后台准备其余角色",
                        percent = 42,
                    )
                }

                // ── 后台：其余角色并行 encode ──
                val pending = remoteCatalogCharacters.filter {
                    !PlatformerRemoteAnimCache.isDiskFullyReady(it)
                }
                if (pending.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        backgroundRunning = true,
                        phase = "后台整理其余角色资源 (0/${pending.size})",
                        totalSteps = pending.size,
                    )
                    val bgSemaphore = Semaphore(BACKGROUND_CHARACTER_PARALLELISM)
                    var bgDone = 0
                    coroutineScope {
                        pending.map { characterId ->
                            async {
                                bgSemaphore.withPermit {
                                    val label = characterId.displayTitle
                                    runCatching {
                                        PlatformerRemoteAnimCache.prepareDiskFull(characterId)
                                    }
                                    recordCatalogClips(context, characterId)
                                    PlatformerBootCache.markPlayable(characterId)
                                    bgDone++
                                    val bgPct = 42 + (bgDone * 56 / pending.size.coerceAtLeast(1))
                                    _state.value = _state.value.copy(
                                        backgroundRunning = true,
                                        phase = GameResourceLoadCopy.forDisplay("后台整理 $label ($bgDone/${pending.size})"),
                                        percent = bgPct.coerceIn(42, 98),
                                        completedSteps = bgDone,
                                        totalSteps = pending.size,
                                    )
                                }
                            }
                        }.awaitAll()
                    }
                }
            }

            val allReady = !needsDecodePrewarm(context)
            if (allReady) {
                persistDecodeStampIfComplete(context)
                runCatching { PlatformerClipCacheIndex.purgeStale(context) }
                PlatformerDecodeWorker.cancel(context)
            } else {
                PlatformerDecodeWorker.scheduleIdle(context)
            }

            _state.value = PrewarmState(
                running = false,
                backgroundRunning = false,
                phase = if (allReady) "" else GameResourceLoadCopy.forDisplay("部分资源待加载"),
                percent = if (allReady) 100 else 90,
                minimumPlayableReady = minimumReady || allReady,
                allCharactersReady = allReady,
                defaultCharacterReady = allReady || minimumReady,
            )
        } catch (t: Throwable) {
            android.util.Log.e("PlatformerPrewarm", "decode prewarm failed", t)
            _state.value = PrewarmState(running = false, phase = "", percent = 0)
            PlatformerDecodeWorker.scheduleIdle(context)
        }
    }

    private fun isMinimumPlayable(context: Context, preferred: PlatformerCharacterId): Boolean {
        if (!PlatformerBootCache.hasAssets()) return false
        if (!PlatformerBootCache.areLocalCharactersLoaded()) return false
        val chickSkinId = PlatformerPlayerSprites.skinId
        val chickOk = when {
            PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(chickSkinId) ->
                PacMazeRemoteSkinAnimCache.isSheetBootstrapPlayable(chickSkinId)
            else ->
                !needsChickDiskDecode() ||
                    PacMazeRemoteSkinAnimCache.isPlatformerSkinFullyOnDisk(chickSkinId)
        }
        val preferredOk = when {
            preferred == PlatformerCharacterId.CHICK_PRO_MAX -> chickOk
            preferred.isCatalogRemote -> PlatformerRemoteAnimCache.isDiskFullyReady(preferred)
            else -> true
        }
        return chickOk && preferredOk
    }

    private fun computeFastTrackSteps(preferred: PlatformerCharacterId): Int {
        var steps = 0
        if (needsMapAssetsWarm()) steps++
        if (needsLocalCharactersLoad()) steps++
        if (needsChickSheetWarm() || needsChickDiskDecode()) steps++
        if (preferred.isCatalogRemote && !PlatformerRemoteAnimCache.isDiskFullyReady(preferred)) steps++
        return steps.coerceAtLeast(1)
    }

    private fun stepFast(phase: String, done: Int, total: Int, localPct: Int) {
        val overall = (done * 100 + localPct) / total.coerceAtLeast(1)
        emitRunning(phase, overall.coerceIn(1, 40), done, total)
    }

    private fun emitIdleComplete() {
        _state.value = PrewarmState(
            running = false,
            percent = 100,
            minimumPlayableReady = true,
            allCharactersReady = true,
            defaultCharacterReady = true,
        )
    }

    private fun emitRunning(phase: String, percent: Int, completedSteps: Int, totalSteps: Int) {
        _state.value = _state.value.copy(
            running = true,
            phase = GameResourceLoadCopy.forDisplay(phase),
            percent = percent.coerceIn(0, 99),
            completedSteps = completedSteps,
            totalSteps = totalSteps,
        )
    }

    private suspend fun recordChickClips(context: Context) {
        val skinId = PlatformerPlayerSprites.skinId
        listOf(
            PacMazeSkinAnimClip.WALK to "walk",
            PacMazeSkinAnimClip.JUMP to "jump",
            PacMazeSkinAnimClip.DIE to "die",
        ).forEach { (clip, folder) ->
            val count = PacMazeRemoteSkinAnimCache.diskClipFrameCount(skinId, clip)
            if (count > 0) PlatformerClipCacheIndex.recordChickClip(context, folder, count)
        }
    }

    private suspend fun recordCatalogClips(context: Context, characterId: PlatformerCharacterId) {
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return
        listOf(
            PlatformerAnimClip.WALK,
            PlatformerAnimClip.RUN,
            PlatformerAnimClip.JUMP,
            PlatformerAnimClip.DIE,
        ).filter { it in cfg.clips }.forEach { clip ->
            val count = PlatformerRemoteAnimCache.diskClipFrameCount(characterId, clip)
            if (count > 0) {
                PlatformerClipCacheIndex.recordClip(context, characterId.catalogId, clip, count)
            }
        }
    }

    private fun persistDecodeStampIfComplete(context: Context) {
        val app = context.applicationContext
        if (needsDecodePrewarm(app)) return

        val characters = buildMap {
            remoteCatalogCharacters.forEach { id ->
                val cfg = PlatformerRemoteAnimCache.config(id) ?: return@forEach
                val primary = when {
                    PlatformerAnimClip.WALK in cfg.clips -> PlatformerAnimClip.WALK
                    PlatformerAnimClip.RUN in cfg.clips -> PlatformerAnimClip.RUN
                    else -> cfg.clips.firstOrNull() ?: return@forEach
                }
                put(
                    id.catalogId,
                    PlatformerDecodeStampStore.CharacterClipStamp(
                        walk = PlatformerRemoteAnimCache.diskClipFrameCount(id, primary),
                        jump = if (PlatformerAnimClip.JUMP in cfg.clips) {
                            PlatformerRemoteAnimCache.diskClipFrameCount(id, PlatformerAnimClip.JUMP)
                        } else {
                            0
                        },
                        die = if (PlatformerAnimClip.DIE in cfg.clips) {
                            PlatformerRemoteAnimCache.diskClipFrameCount(id, PlatformerAnimClip.DIE)
                        } else {
                            0
                        },
                    ),
                )
            }
        }

        val skinId = PlatformerPlayerSprites.skinId
        val chickStamp = if (PacMazeRemoteSkinAnimCache.hasPlatformerSheetBundle(skinId)) {
            PlatformerDecodeStampStore.ChickClipStamp(
                walk = PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.WALK),
                jump = PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.JUMP),
                die = PacMazeRemoteSkinAnimCache.clipFrameTarget(skinId, PacMazeSkinAnimClip.DIE),
            )
        } else {
            PlatformerDecodeStampStore.ChickClipStamp(
                walk = PacMazeRemoteSkinAnimCache.diskClipFrameCount(skinId, PacMazeSkinAnimClip.WALK),
                jump = PacMazeRemoteSkinAnimCache.diskClipFrameCount(skinId, PacMazeSkinAnimClip.JUMP),
                die = PacMazeRemoteSkinAnimCache.diskClipFrameCount(skinId, PacMazeSkinAnimClip.DIE),
            )
        }
        val stamp = PlatformerDecodeStampStore.currentExpectedBase().copy(
            characters = characters,
            chick = chickStamp,
            mapAssetsLoaded = PlatformerBootCache.hasAssets(),
            localCharactersLoaded = PlatformerBootCache.areLocalCharactersLoaded(),
        )
        PlatformerDecodeStampStore.saveV3(app, stamp)
    }
}
