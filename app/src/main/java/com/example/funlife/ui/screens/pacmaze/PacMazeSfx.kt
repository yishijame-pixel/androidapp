package com.example.funlife.ui.screens.pacmaze



import android.content.Context

import android.util.Log

import com.example.funlife.social.game.engine.pacmaze.PacMazePhase

import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode

import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.SupervisorJob

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext



/** 豆人迷宫音效入口（云端 pac_maze_sfx，不回退转盘/猜谜 raw）。 */

object PacMazeSfx {



    private const val TAG = "PacMazeSfx"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var manager: PacMazeAudioManager? = null

    private var lastBgmPhase: PacMazePhase? = null

    private var lastRunMode: PacMazeRunMode = PacMazeRunMode.CAMPAIGN



    fun prefetch(context: Context) {

        ioScope.launch {

            ensureReady(context.applicationContext)

            withContext(Dispatchers.Main.immediate) {

                replayBgmIfNeeded(context.applicationContext)

            }

        }

    }



    suspend fun ensureReady(context: Context): Boolean =

        audio(context.applicationContext).ensureBundle()

    fun reloadAfterBundleUpdate(context: Context) {

        audio(context.applicationContext).reloadAfterBundleUpdate()

    }



    fun diagnose(context: Context): PacMazeAudioDiagnostics =

        audio(context.applicationContext).diagnose()



    fun isBgmAvailable(context: Context): Boolean {

        val diag = diagnose(context)

        return diag.menuBgmFound && diag.campaignBgmFound && diag.endlessBgmFound

    }



    fun prepare(context: Context) {

        audio(context.applicationContext).prepare()

    }



    /** 按界面阶段 + 运行模式播放 BGM。 */

    fun syncBgm(

        context: Context,

        phase: PacMazePhase,

        runMode: PacMazeRunMode = lastRunMode,

    ) {

        lastBgmPhase = phase

        if (phase == PacMazePhase.PLAYING || phase == PacMazePhase.PAUSED) {

            lastRunMode = runMode

        }

        ioScope.launch {

            runCatching { ensureReady(context.applicationContext) }

                .onFailure { Log.w(TAG, "ensureReady before bgm failed", it) }

            withContext(Dispatchers.Main.immediate) {

                applyBgmPhase(context.applicationContext, phase, runMode)

            }

        }

    }



    private fun replayBgmIfNeeded(context: Context) {

        val phase = lastBgmPhase ?: return

        applyBgmPhase(context, phase, lastRunMode)

    }



    private fun applyBgmPhase(context: Context, phase: PacMazePhase, runMode: PacMazeRunMode) {

        val audio = audio(context)

        when (phase) {

            PacMazePhase.MENU -> audio.startMenuBgm()

            PacMazePhase.PLAYING -> when (runMode) {

                PacMazeRunMode.ENDLESS -> audio.startEndlessBgm()

                else -> audio.startCampaignBgm()

            }

            PacMazePhase.PAUSED -> audio.pauseBgm()

            else -> audio.stopBgm()

        }

    }



    /** 播放 Hub UI 规范音效（固定映射，见 [PacMazeUiSoundId]）。 */

    fun playUiSound(context: Context, sound: PacMazeUiSoundId) {

        audio(context.applicationContext).play(sound.event)

    }



    fun startMenuBgm(context: Context) {

        syncBgm(context, PacMazePhase.MENU)

    }



    fun pauseBgm(context: Context) {

        lastBgmPhase = PacMazePhase.PAUSED

        audio(context.applicationContext).pauseBgm()

    }



    fun stopBgm(context: Context) {

        lastBgmPhase = null

        audio(context.applicationContext).stopBgm()

    }



    fun playPowerPellet(context: Context) {

        audio(context.applicationContext).play(PacMazeAudioEvent.POWER_PELLET)

    }



    fun playAttack(context: Context) {

        audio(context.applicationContext).play(PacMazeAudioEvent.ATTACK)

    }



    fun playGhostHit(context: Context) {

        audio(context.applicationContext).play(PacMazeAudioEvent.GHOST_HIT)

    }



    fun playPlayerHurt(context: Context) {

        audio(context.applicationContext).play(PacMazeAudioEvent.PLAYER_HURT)

    }



    fun playVictory(context: Context) {

        audio(context.applicationContext).stopBgm()

        audio(context.applicationContext).play(PacMazeAudioEvent.VICTORY)

    }



    fun playDefeat(context: Context) {

        audio(context.applicationContext).stopBgm()

        audio(context.applicationContext).play(PacMazeAudioEvent.DEFEAT)

    }



    fun playLevelClear(context: Context) = playVictory(context)



    fun playGameOver(context: Context) = playDefeat(context)



    fun playCheckpoint(context: Context) {

        audio(context.applicationContext).play(PacMazeAudioEvent.CHECKPOINT)

    }



    fun release(context: Context) {

        manager?.release()

        manager = null

        lastBgmPhase = null

    }



    private fun audio(context: Context): PacMazeAudioManager {

        return manager ?: PacMazeAudioManager.getInstance(context).also { manager = it }

    }

}



fun pacMazeUiClick(

    context: Context,

    sound: PacMazeUiSoundId,

    onClick: () -> Unit,

): () -> Unit = {

    PacMazeSfx.playUiSound(context, sound)

    onClick()

}


