package com.example.funlife.game.platformer.catalog

import com.example.funlife.game.platformer.PlatformerCombat
import com.example.funlife.game.platformer.PlatformerRangedCombat
import com.example.funlife.game.platformer.PlatformerCharacterId
import com.example.funlife.game.platformer.PlatformerCharacterRenderer
import com.example.funlife.game.platformer.PlatformerPlayerSprites
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.platformer.PlatformerBootCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 选角页预 hydrate：磁盘全量已就绪时，并行读盘进内存，切换角色体感秒开。
 */
object PlatformerCharacterPrefetch {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun prefetchAttackClips(characterId: PlatformerCharacterId) {
        PlatformerRangedCombat.prefetchClips(characterId)
        if (!PlatformerCombat.canAttack(characterId)) return
        PlatformerRemoteAnimCache.requestSheetPlaybackAsync(
            characterId,
            PlatformerAnimClip.ATTACK,
        )
        val cfg = PlatformerRemoteAnimCache.config(characterId) ?: return
        if (PlatformerAnimClip.JUMP_ATTACK in cfg.clips) {
            PlatformerRemoteAnimCache.requestSheetPlaybackAsync(
                characterId,
                PlatformerAnimClip.JUMP_ATTACK,
            )
        }
    }

    fun prefetchOnSelect(characterId: PlatformerCharacterId) {
        PlatformerAnimMemoryPool.onCharacterFocused(characterId)
        if (PlatformerBootCache.isPlayable(characterId)) return
        scope.launch {
            runCatching { hydrateSync(characterId) }
                .onSuccess { ok ->
                    if (ok) PlatformerBootCache.markPlayable(characterId)
                }
        }
    }

    fun prefetchOnHover(characterId: PlatformerCharacterId) {
        if (PlatformerBootCache.isPlayable(characterId)) return
        scope.launch {
            runCatching { hydrateLight(characterId) }
                .onSuccess { ok ->
                    if (ok) PlatformerBootCache.markPlayable(characterId)
                }
        }
    }

    /** 轻量预热：sheet / bootstrap，不切全量 61 帧。 */
    suspend fun hydrateLight(characterId: PlatformerCharacterId): Boolean {
        PlatformerAnimMemoryPool.onCharacterFocused(characterId)
        return when {
            characterId == PlatformerCharacterId.CHICK_PRO_MAX -> {
                PlatformerPlayerSprites.prefetchAttackSheet()
                if (PlatformerPlayerSprites.isBootstrapPlayable()) return true
                runCatching { PlatformerPlayerSprites.prepareSheetsForPlay() }
                    .getOrDefault(false)
            }
            characterId.isCatalogRemote -> {
                if (PlatformerCharacterRenderer.isBootstrapPlayable(characterId)) {
                    prefetchAttackClips(characterId)
                    return true
                }
                runCatching {
                    PlatformerRemoteAnimCache.prepareSheetsForBoot(characterId)
                }.getOrDefault(false).also { ok ->
                    if (ok) prefetchAttackClips(characterId)
                }
            }
            else -> {
                runCatching { PlatformerCharacterRenderer.warmup(characterId) }.isSuccess &&
                    PlatformerCharacterRenderer.isBootstrapPlayable(characterId)
            }
        }.also { ok ->
            if (ok) PlatformerBootCache.markPlayable(characterId)
        }
    }

    /** 同步 hydrate（磁盘 → 内存），供横屏切角 fast path。 */
    suspend fun hydrateSync(characterId: PlatformerCharacterId): Boolean {
        PlatformerAnimMemoryPool.onCharacterFocused(characterId)
        val ready = when {
            characterId == PlatformerCharacterId.CHICK_PRO_MAX -> {
                PlatformerPlayerSprites.prefetchAttackSheet()
                if (PlatformerPlayerSprites.isBootstrapPlayable()) return true
                runCatching { PlatformerPlayerSprites.prepareSheetsForPlay() }
                    .getOrDefault(false)
            }
            characterId.isCatalogRemote -> {
                if (PlatformerCharacterRenderer.isPlayableReady(characterId)) return true
                if (PlatformerRemoteAnimCache.isSheetBootstrapPlayable(characterId)) return true
                if (!PlatformerRemoteAnimCache.isDiskFullyReady(characterId)) return false
                runCatching {
                    PlatformerRemoteAnimCache.prepareSheetsForBoot(characterId) ||
                        PlatformerRemoteAnimCache.preparePlayableFromDisk(characterId)
                }.getOrDefault(false)
            }
            else -> {
                runCatching { PlatformerCharacterRenderer.warmup(characterId) }.isSuccess &&
                    PlatformerCharacterRenderer.isPlayableReady(characterId)
            }
        }
        if (ready) {
            PlatformerBootCache.markPlayable(characterId)
        }
        return ready
    }
}
