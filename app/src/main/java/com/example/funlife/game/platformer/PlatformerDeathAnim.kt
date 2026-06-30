package com.example.funlife.game.platformer

import com.example.funlife.game.platformer.catalog.PlatformerAnimClip
import com.example.funlife.game.platformer.catalog.PlatformerRemoteAnimCache
import com.example.funlife.game.platformer.catalog.catalogId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import kotlin.math.ceil

/** 横版死亡阶段：动画时长 + 复活倒计时。 */
object PlatformerDeathAnim {

    fun dieFrameCount(characterId: PlatformerCharacterId): Int = when {
        characterId == PlatformerCharacterId.CHICK_PRO_MAX -> {
            val skinId = PlatformerPlayerSprites.skinId
            val sheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.DIE)
            if (sheet != null) sheet.frameCount.coerceAtLeast(1)
            else {
                val manifest = PacMazeSkinAnimManifest.loadForSkin(skinId)
                manifest?.frameCount(PacMazeSkinAnimClip.DIE)?.coerceAtLeast(1) ?: 10
            }
        }
        characterId.isCatalogRemote -> {
            val cfg = PlatformerRemoteAnimCache.config(characterId)
            if (cfg == null) 10
            else {
                val sheet = PlatformerRemoteAnimCache.playbackSheet(
                    characterId.catalogId,
                    PlatformerAnimClip.DIE,
                )
                if (sheet != null) sheet.frameCount.coerceAtLeast(1)
                else {
                    val manifest = PacMazeSkinAnimManifest.load(cfg.assetRoot)
                    manifest?.frameCountByKey(PlatformerAnimClip.DIE.name.lowercase())?.coerceAtLeast(1) ?: 10
                }
            }
        }
        else -> 8
    }

    fun animDurationSec(characterId: PlatformerCharacterId): Float =
        PlatformerPlayerSprites.deathAnimDurationSec(dieFrameCount(characterId))

    fun totalPhaseSec(characterId: PlatformerCharacterId): Float =
        PlatformerPlayerSprites.totalDeathPhaseSec(dieFrameCount(characterId))

    fun overlayMessage(
        characterId: PlatformerCharacterId,
        deathAnimTime: Float,
        deathHint: String?,
    ): String {
        val animDur = animDurationSec(characterId)
        if (deathAnimTime < animDur) return deathHint ?: "阵亡"
        val countdownLeft = totalPhaseSec(characterId) - deathAnimTime
        val sec = ceil(countdownLeft).toInt().coerceAtLeast(1)
        return "复活 $sec"
    }
}
