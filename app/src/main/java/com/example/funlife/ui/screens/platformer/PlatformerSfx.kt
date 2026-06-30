package com.example.funlife.ui.screens.platformer

import android.content.Context
import android.util.Log
import com.example.funlife.game.platformer.PlatformerSuperTuxLevelCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 横版冒险音效入口（全局 `platformer_sfx` bundle，覆盖全部关卡）。 */
object PlatformerSfx {

    private const val TAG = "PlatformerSfx"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var manager: PlatformerAudioManager? = null
    private var lastBgmEvent: PlatformerSfxEvent? = null

    fun prefetch(context: Context) {
        ioScope.launch {
            ensureReady(context.applicationContext)
            withContext(Dispatchers.Main.immediate) {
                lastBgmEvent?.let { audio(context.applicationContext).startGameplayBgm(it) }
            }
        }
    }

    suspend fun ensureReady(context: Context): Boolean =
        audio(context.applicationContext).ensureBundle()

    fun reloadAfterBundleUpdate(context: Context) {
        audio(context.applicationContext).reloadAfterBundleUpdate()
    }

    fun prepare(context: Context) {
        audio(context.applicationContext).prepare()
    }

    /** 进入关卡时按主题选择 BGM。 */
    fun startLevelBgm(context: Context, levelId: Int) {
        val event = if (PlatformerSuperTuxLevelCatalog.isSuperTuxLevel(levelId)) {
            PlatformerSfxEvent.BGM_SUPERTUX_ANTARCTIC
        } else {
            PlatformerSfxEvent.BGM_PLATFORMER
        }
        lastBgmEvent = event
        ioScope.launch {
            runCatching { ensureReady(context.applicationContext) }
                .onFailure { Log.w(TAG, "ensureReady before bgm failed", it) }
            withContext(Dispatchers.Main.immediate) {
                audio(context.applicationContext).startGameplayBgm(event)
            }
        }
    }

    fun pauseBgm(context: Context) {
        audio(context.applicationContext).pauseBgm()
    }

    fun stopBgm(context: Context) {
        lastBgmEvent = null
        audio(context.applicationContext).stopBgm()
    }

    fun play(context: Context, event: PlatformerSfxEvent) {
        audio(context.applicationContext).play(event)
    }

    fun playJump(context: Context, bigJump: Boolean = false) {
        play(context, if (bigJump) PlatformerSfxEvent.PLAYER_BIG_JUMP else PlatformerSfxEvent.PLAYER_JUMP)
    }

    fun playLand(context: Context) = play(context, PlatformerSfxEvent.PLAYER_LAND)
    fun playGem(context: Context) = play(context, PlatformerSfxEvent.PICKUP_GEM)
    fun playStomp(context: Context) = play(context, PlatformerSfxEvent.ENEMY_STOMP)
    fun playSpring(context: Context) = play(context, PlatformerSfxEvent.SPRING_BOUNCE)
    fun playHurt(context: Context) = play(context, PlatformerSfxEvent.PLAYER_HURT)
    fun playDie(context: Context) {
        stopBgm(context)
        play(context, PlatformerSfxEvent.PLAYER_DIE)
    }

    fun playLevelClear(context: Context) {
        stopBgm(context)
        play(context, PlatformerSfxEvent.LEVEL_CLEAR)
    }

    fun playShoot(context: Context) = play(context, PlatformerSfxEvent.SHOOT)

    fun release(context: Context) {
        manager?.release()
        manager = null
        lastBgmEvent = null
    }

    private fun audio(context: Context): PlatformerAudioManager =
        manager ?: PlatformerAudioManager.getInstance(context).also { manager = it }
}
