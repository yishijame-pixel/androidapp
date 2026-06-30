package com.example.funlife.game.platformer

import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback

/**
 * 行走小鸡进局诊断。Logcat 过滤：`PlatformerChickLoad`
 *
 * 正常 sheet（walk 8×8）decode 后约：
 * - sample=8 → bitmap ~992×744，cell ~124×116，decodeMs 通常 <800ms
 * - 若 sample=1 → bitmap ~7900×7400，会卡顿数秒且可能 OOM
 */
internal object PlatformerChickLoadLog {

    private const val TAG = "PlatformerChickLoad"
    private const val MAX_LOGS = 12
    private var logCount = 0

    fun logSheetDraw(
        sheet: PacMazeSkinSheetPlayback,
        clip: PacMazeSkinAnimClip,
        layout: PlatformerPlayerSprites.ChickSpriteLayout,
        path: String,
    ) {
        if (!BuildConfig.DEBUG) return
        if (logCount >= MAX_LOGS) return
        logCount++
        val skinId = PlatformerPlayerSprites.skinId
        val walkSheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.WALK)
        val jumpSheet = PacMazeRemoteSkinAnimCache.playbackSheet(skinId, PacMazeSkinAnimClip.JUMP)
        Log.i(
            TAG,
            "draw path=$path clip=${clip.name} frameIdx path sheet " +
                "bitmap=${sheet.bitmap.width}x${sheet.bitmap.height} " +
                "cell=${sheet.cellW}x${sheet.cellH} sample=${sheet.sampleSize} " +
                "layout frame=${layout.frameW.toInt()}x${layout.frameH.toInt()} " +
                "box=${layout.dstW.toInt()}x${layout.dstH.toInt()} " +
                "walkReady=${walkSheet != null} jumpReady=${jumpSheet != null} " +
                "bootstrap=${PlatformerPlayerSprites.isBootstrapPlayable()}",
        )
        if (sheet.sampleSize <= 1 && sheet.cellH > 200) {
            Log.w(TAG, ">>> HUGE SHEET SAMPLE (sample=1) — expect freeze; should be sample>=8 for chick")
        }
    }

    fun logFallbackFrame(
        clip: PacMazeSkinAnimClip,
        frameW: Int,
        frameH: Int,
        reason: String,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.w(
            TAG,
            "fallback per-frame clip=${clip.name} frame=${frameW}x${frameH} reason=$reason",
        )
    }

    fun resetSession() {
        logCount = 0
    }
}
