package com.example.funlife.ui.screens.platformer

import android.content.Context
import com.example.funlife.game.platformer.PlatformerAssets
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerCharacterRenderer
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerResourcePrewarmCoordinator
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.PlatformerAnimMemoryPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 横版冒险会话级启动缓存：可玩就绪 vs 完整预热分离，二次进入秒开。
 */
object PlatformerBootCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var cachedAssets: PlatformerAssets? = null

    @Volatile
    private var prewarmStarted = false

    @Volatile
    private var localCharactersLoaded = false

    private val playableCharacters = ConcurrentHashMap.newKeySet<PlatformerCharacterId>()
    private val fullWarmedCharacters = ConcurrentHashMap.newKeySet<PlatformerCharacterId>()

    fun hasAssets(): Boolean = cachedAssets != null

    fun areLocalCharactersLoaded(): Boolean = localCharactersLoaded

    fun markLocalCharactersLoaded() {
        localCharactersLoaded = true
    }

    fun isPlayable(characterId: PlatformerCharacterId): Boolean =
        playableCharacters.contains(characterId) &&
            (PlatformerCharacterRenderer.isPlayableReady(characterId) ||
                PlatformerCharacterRenderer.isBootstrapPlayable(characterId))

    fun invalidatePlayable() {
        playableCharacters.clear()
        fullWarmedCharacters.clear()
        localCharactersLoaded = false
    }

    fun resetPrewarmSession() {
        prewarmStarted = false
        invalidatePlayable()
    }

    fun isFullyWarmed(characterId: PlatformerCharacterId): Boolean =
        fullWarmedCharacters.contains(characterId)

    fun forceMarkPlayable(characterId: PlatformerCharacterId) {
        playableCharacters.add(characterId)
    }

    fun markPlayable(characterId: PlatformerCharacterId) {
        if (PlatformerCharacterRenderer.isPlayableReady(characterId) ||
            PlatformerCharacterRenderer.isBootstrapPlayable(characterId)
        ) {
            playableCharacters.add(characterId)
        }
    }

    fun markFullyWarmed(characterId: PlatformerCharacterId) {
        fullWarmedCharacters.add(characterId)
    }

    /** 预加载地图/图块（与角色 decode 并行）。 */
    suspend fun warmMapAssets(context: Context) {
        if (cachedAssets == null) {
            cachedAssets = PlatformerAssets.loadFallback(context)
        }
    }

    /** App / 资源下载完成后后台预热（委托企业级协调器）。 */
    fun startPrewarm(context: Context) {
        if (prewarmStarted) return
        prewarmStarted = true
        PlatformerResourcePrewarmCoordinator.scheduleAfterBundlesReady(context.applicationContext)
    }

    suspend fun obtainAssets(context: Context): PlatformerAssets {
        cachedAssets?.let { return it }
        val loaded = PlatformerAssets.loadFallback(context)
        cachedAssets = loaded
        return loaded
    }

    fun scheduleFullWarmup(characterId: PlatformerCharacterId) {
        if (isFullyWarmed(characterId)) return
        scope.launch {
            runCatching {
                PlatformerAnimMemoryPool.onCharacterFocused(characterId)
                when (characterId) {
                    PlatformerCharacterId.CHICK_PRO_MAX ->
                        PlatformerPlayerSprites.warmupDieSheetOnly()
                    PlatformerCharacterId.TREASURE_HUNTER, PlatformerCharacterId.PIXEL_WALKER ->
                        PlatformerCharacterRenderer.warmup(characterId)
                    else -> if (characterId.isCatalogRemote) {
                        PlatformerRemoteAnimCache.requestSheetPlaybackAsync(
                            characterId,
                            PlatformerAnimClip.DIE,
                        )
                    }
                }
                markFullyWarmed(characterId)
            }
        }
    }
}
