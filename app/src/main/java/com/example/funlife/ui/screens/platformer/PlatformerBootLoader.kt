package com.example.funlife.ui.screens.platformer

import android.content.Context
import com.example.funlife.game.platformer.PlatformerAssets
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerCharacterRenderer
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.game.platformer.catalog.PlatformerAssetService
import com.example.funlife.game.platformer.catalog.PlatformerCharacterPrefetch
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * 横版冒险启动管线：
 * - 大厅已全角色磁盘 decode → 并行 hydrate → 秒进
 * - 进局门槛：isPlayableReady（walk/jump/die 全量内存）
 */
object PlatformerBootLoader {

    enum class BootPhase(val label: String, val weightStart: Int) {
        INIT("初始化", 0),
        ASSETS("地图资源", 12),
        CHARACTER("角色资源", 35),
        CACHE_HIT("缓存命中", 88),
        PLAYABLE("可玩就绪", 92),
        COMPLETE("完成", 100),
    }

    data class BootState(
        val message: String,
        val progress: Int,
        val phase: BootPhase = BootPhase.INIT,
        val fromCache: Boolean = false,
    )

    private const val MAX_BOOT_WAIT_POLLS = 120
    private const val POLL_MS = 32L

    suspend fun load(
        context: Context,
        characterId: PlatformerCharacterId,
        onUpdate: (BootState) -> Unit,
    ): PlatformerAssets = coroutineScope {
        onUpdate(BootState("初始化坤坤大冒险…", 4, BootPhase.INIT))
        PlatformerAssetService.ensureInitialized(context)

        if (PlatformerBootCache.hasAssets()) {
            val cachedAssets = PlatformerBootCache.obtainAssets(context)
            if (PlatformerBootCache.isPlayable(characterId)) {
                onUpdate(BootState("缓存命中，即时进入", 100, BootPhase.CACHE_HIT, fromCache = true))
                PlatformerBootCache.scheduleFullWarmup(characterId)
                return@coroutineScope cachedAssets
            }
            loadCharacterPlayable(characterId, onUpdate)
            PlatformerBootCache.markPlayable(characterId)
            PlatformerBootCache.scheduleFullWarmup(characterId)
            onUpdate(BootState("准备就绪，选择关卡出发！", 100, BootPhase.COMPLETE))
            return@coroutineScope cachedAssets
        }

        val assetsJob = async { PlatformerBootCache.obtainAssets(context) }
        onUpdate(BootState("加载地图与敌人资源…", 14, BootPhase.ASSETS))
        loadCharacterPlayable(characterId, onUpdate)

        val assets = assetsJob.await()
        PlatformerBootCache.markPlayable(characterId)
        PlatformerBootCache.scheduleFullWarmup(characterId)
        onUpdate(BootState("准备就绪，选择关卡出发！", 100, BootPhase.COMPLETE))
        assets
    }

    suspend fun reloadCharacter(
        characterId: PlatformerCharacterId,
        onUpdate: (BootState) -> Unit,
    ) {
        if (PlatformerBootCache.isPlayable(characterId)) {
            onUpdate(BootState("角色已缓存", 100, BootPhase.CACHE_HIT, fromCache = true))
            PlatformerBootCache.scheduleFullWarmup(characterId)
            return
        }
        val fast = runCatching {
            PlatformerCharacterPrefetch.hydrateLight(characterId)
        }.getOrDefault(false)
        if (fast || PlatformerBootCache.isPlayable(characterId)) {
            onUpdate(BootState("角色已就绪", 100, BootPhase.CACHE_HIT, fromCache = true))
            PlatformerBootCache.scheduleFullWarmup(characterId)
            return
        }
        onUpdate(BootState("切换角色…", 12, BootPhase.CHARACTER))
        loadCharacterPlayable(characterId, onUpdate, maxPolls = 50)
        PlatformerBootCache.markPlayable(characterId)
        PlatformerBootCache.scheduleFullWarmup(characterId)
        onUpdate(BootState("准备就绪", 100, BootPhase.COMPLETE))
    }

    private suspend fun loadCharacterPlayable(
        characterId: PlatformerCharacterId,
        onUpdate: (BootState) -> Unit,
        maxPolls: Int = MAX_BOOT_WAIT_POLLS,
    ) {
        when {
            characterId == PlatformerCharacterId.CHICK_PRO_MAX ->
                loadChickPlayable(onUpdate, maxPolls)
            characterId.isCatalogRemote ->
                loadCatalogPlayable(characterId, onUpdate, maxPolls)
            else ->
                loadLocalCharacter(characterId, onUpdate)
        }
    }

    private suspend fun loadLocalCharacter(id: PlatformerCharacterId, onUpdate: (BootState) -> Unit) {
        val label = id.displayTitle
        onUpdate(BootState("加载${label}资源…", 48, BootPhase.CHARACTER))
        runCatching { PlatformerCharacterRenderer.warmup(id) }
        onUpdate(BootState("${label}就绪", 94, BootPhase.PLAYABLE))
    }

    private suspend fun loadCatalogPlayable(
        characterId: PlatformerCharacterId,
        onUpdate: (BootState) -> Unit,
        maxPolls: Int = MAX_BOOT_WAIT_POLLS,
    ) {
        val entry = com.example.funlife.game.platformer.catalog.PlatformerContentCatalog.characterForEnum(characterId)
        val label = entry?.title ?: characterId.title
        onUpdate(BootState("加载 $label…", 32, BootPhase.CHARACTER))

        val fromDisk = PlatformerRemoteAnimCache.isDiskFullyReady(characterId)
        val sheetReady = runCatching {
            PlatformerRemoteAnimCache.prepareSheetsForBoot(characterId)
        }.getOrDefault(false)
        if (sheetReady) {
            onUpdate(BootState("$label 资源就绪", 96, BootPhase.PLAYABLE, fromCache = fromDisk))
            return
        }
        val ready = runCatching {
            if (fromDisk) {
                PlatformerRemoteAnimCache.preparePlayableFromDisk(characterId)
            } else {
                PlatformerRemoteAnimCache.preparePlayable(characterId)
            }
        }.getOrDefault(false)

        if (ready) {
            onUpdate(BootState("$label 就绪", 96, BootPhase.PLAYABLE, fromCache = fromDisk))
            return
        }

        PlatformerRemoteAnimCache.requestWarmup(characterId)
        pollUntilPlayable(
            maxPolls = maxPolls,
            onUpdate = onUpdate,
            readyCheck = { PlatformerCharacterRenderer.isPlayableReady(characterId) },
            progressRange = 32..94,
            messageReady = "$label 就绪",
            messageLoading = { _ -> "加载 $label 资源…" },
        )
        if (!PlatformerCharacterRenderer.isPlayableReady(characterId)) {
            runCatching { PlatformerRemoteAnimCache.warmup(characterId) }
        }
        onUpdate(BootState("$label 就绪", 96, BootPhase.PLAYABLE))
    }

    private suspend fun loadChickPlayable(
        onUpdate: (BootState) -> Unit,
        maxPolls: Int = MAX_BOOT_WAIT_POLLS,
    ) {
        val skinId = PlatformerCharacterRenderer.chickSkinId()
        onUpdate(BootState("准备行走小鸡 Pro Max…", 28, BootPhase.CHARACTER))

        val fromDisk = PacMazeRemoteSkinAnimCache.isPlatformerSkinFullyOnDisk(skinId)
        runCatching {
            withTimeout(20_000) {
                if (!PlatformerPlayerSprites.isBootstrapPlayable()) {
                    PacMazeRemoteSkinAnimCache.preparePlatformerSheetsForBoot(skinId) { status ->
                        onUpdate(
                            BootState(
                                message = status.message,
                                progress = status.percent.coerceIn(28, 96),
                                phase = BootPhase.CHARACTER,
                            ),
                        )
                    }
                }
                if (!PlatformerPlayerSprites.isBootstrapPlayable()) {
                    PlatformerPlayerSprites.warmupPlayable { message, progress ->
                        onUpdate(
                            BootState(
                                message = message,
                                progress = progress.coerceIn(28, 96),
                                phase = BootPhase.CHARACTER,
                            ),
                        )
                    }
                }
            }
        }

        PlatformerBootCache.markPlayable(PlatformerCharacterId.CHICK_PRO_MAX)
        val bootstrapOk = PlatformerPlayerSprites.isBootstrapPlayable()
        onUpdate(
            BootState(
                message = when {
                    bootstrapOk && fromDisk -> "缓存命中，行走小鸡就绪"
                    bootstrapOk -> "行走小鸡 Pro Max 就绪"
                    else -> "可先进入，资源后台加载中…"
                },
                progress = 100,
                phase = BootPhase.PLAYABLE,
                fromCache = fromDisk && bootstrapOk,
            ),
        )
    }

    private suspend fun pollUntilPlayable(
        maxPolls: Int,
        onUpdate: (BootState) -> Unit,
        readyCheck: () -> Boolean,
        progressRange: IntRange,
        messageReady: String,
        messageLoading: (Int) -> String,
    ) {
        var polls = 0
        var lastProgress = progressRange.first
        while (polls < maxPolls) {
            val ready = readyCheck()
            val span = progressRange.last - progressRange.first
            val computed = progressRange.first + (polls * span / maxPolls.coerceAtLeast(1))
            lastProgress = maxOf(lastProgress, if (ready) progressRange.last else computed)
            onUpdate(
                BootState(
                    message = if (ready) messageReady else messageLoading(polls),
                    progress = lastProgress,
                    phase = if (ready) BootPhase.PLAYABLE else BootPhase.CHARACTER,
                ),
            )
            if (ready) break
            delay(POLL_MS)
            polls++
        }
    }
}

