package com.example.funlife.game.platformer

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.example.funlife.BuildConfig
import com.example.funlife.game.platformer.PlatformerPlayerSprites.ChickSpriteLayout
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import kotlin.math.abs

/**
 * 行走小鸡 walk 顿挫诊断。Logcat 过滤：`PlatformerChickWalk`
 *
 * ```
 * adb logcat -s PlatformerChickWalk
 * ```
 *
 * 正常走路：每换一帧一行 `frame`；`anomaly=none`。
 * 若一顿一顿，重点看带 `>>>` 的行：
 * - `FRAME_RESET`：帧号突然回到 0（animPhase / isMoving 问题）
 * - `FRAME_SKIP`：帧号跳跃 >2（dt 或 phase 不稳）
 * - `DRAW_JUMP`：drawTop/drawH 帧间变化大（layout/脚点对齐问题）
 * - `SIZE_FLICKER`：drawW/drawH 变化（逐帧缩放未关干净）
 * - `PATH_FLIP`：sheet ↔ per-frame 路径切换
 * - `CLIP_FLIP`：walk ↔ jump clip 切换
 */
internal object PlatformerChickWalkDbg {

    const val TAG = "PlatformerChickWalk"

    /** 设为 false 可临时关闭（仍仅 DEBUG 包生效）。 */
    var enabled: Boolean = true

    private const val THROTTLE_MS = 60L
    private const val DRAW_TOP_JUMP_PX = 3.5f
    private const val DRAW_SIZE_JUMP_PX = 2f

    private var lastLogMs = 0L
    private var lastFrameIdx = -1
    private var lastClip: PacMazeSkinAnimClip? = null
    private var lastPath: String? = null
    private var lastDrawTop = Float.NaN
    private var lastDrawH = Float.NaN
    private var lastDrawW = Float.NaN
    private var lastFrameOy = Float.NaN
    private var frameLogCount = 0
    private const val MAX_FRAME_LOGS = 400

    data class DrawSample(
        val clip: PacMazeSkinAnimClip,
        val frameIndex: Int,
        val frameCount: Int,
        val path: String,
        val layout: ChickSpriteLayout,
        val drawLeft: Float,
        val drawTop: Float,
        val drawW: Int,
        val drawH: Int,
        val feetScreen: Offset,
        val animTime: Float,
        val player: PlatformerPlayer,
    )

    fun onWalkDraw(sample: DrawSample) {
        if (!BuildConfig.DEBUG || !enabled) return
        when (sample.clip) {
            PacMazeSkinAnimClip.WALK -> logWalkDraw(sample)
            PacMazeSkinAnimClip.IDLE -> if (!sample.player.locomoting && sample.player.grounded) {
                logIdleDraw(sample)
            } else {
                noteNonWalk(sample)
            }
            else -> noteNonWalk(sample)
        }
    }

    private fun logWalkDraw(sample: DrawSample) {
        if (!sample.player.grounded && sample.player.vy < -20f) return
        logClipDraw(sample, PacMazeSkinAnimManifest.platformerFrameMetrics(
            PlatformerPlayerSprites.skinId,
            PacMazeSkinAnimClip.WALK,
            sample.frameIndex,
        ))
    }

    private fun logIdleDraw(sample: DrawSample) {
        logClipDraw(sample, null)
    }

    private fun logClipDraw(
        sample: DrawSample,
        metrics: PacMazeSkinAnimManifest.PlatformerFrameMetrics?,
    ) {
        if (frameLogCount >= MAX_FRAME_LOGS) return

        val now = System.currentTimeMillis()
        val anomalies = detectAnomalies(sample)
        val frameChanged = sample.frameIndex != lastFrameIdx
        val shouldLog = anomalies.isNotEmpty() ||
            frameChanged ||
            now - lastLogMs >= THROTTLE_MS

        if (!shouldLog) return
        lastLogMs = now
        frameLogCount++

        val level = if (anomalies.isNotEmpty()) Log.WARN else Log.INFO
        Log.println(
            level,
            TAG,
            buildString {
                append("clip=${sample.clip.name} ")
                append("frame=${sample.frameIndex}/${sample.frameCount - 1} ")
                append("path=${sample.path} ")
                append("phase=${fmt(sample.player.animPhase)} vx=${sample.player.vx.toInt()} ")
                append("loco=${sample.player.locomoting} grounded=${sample.player.grounded} ")
                append("fy=${metrics?.feetY?.let(::fmt) ?: "?"} ty=${metrics?.headTopY?.let(::fmt) ?: "?"} ")
                append("layout=${sample.layout.frameW.toInt()}x${sample.layout.frameH.toInt()} ")
                append("oy=${sample.layout.frameOy.toInt()} feetY=${fmt(sample.layout.feetYFrac)} ")
                append("draw=(${sample.drawLeft.toInt()},${sample.drawTop.toInt()}) ")
                append("${sample.drawW}x${sample.drawH} feetY=${sample.feetScreen.y.toInt()} ")
                if (frameChanged && lastFrameIdx >= 0) {
                    append("dFrame=${sample.frameIndex - lastFrameIdx} ")
                }
                if (!lastDrawTop.isNaN()) {
                    append("dTop=${fmt(sample.drawTop - lastDrawTop)} dH=${sample.drawH - lastDrawH.toInt()} ")
                }
                append("anomaly=")
                if (anomalies.isEmpty()) append("none") else anomalies.joinToString("|")
            },
        )
        anomalies.forEach { Log.w(TAG, ">>> $it") }

        lastFrameIdx = sample.frameIndex
        lastClip = sample.clip
        lastPath = sample.path
        lastDrawTop = sample.drawTop
        lastDrawH = sample.drawH.toFloat()
        lastDrawW = sample.drawW.toFloat()
        lastFrameOy = sample.layout.frameOy
    }

    fun logPathMiss(reason: String) {
        if (!BuildConfig.DEBUG || !enabled) return
        Log.w(TAG, ">>> PATH_FLIP sheet=null fallback reason=$reason")
    }

    fun resetSession() {
        lastFrameIdx = -1
        lastClip = null
        lastPath = null
        lastDrawTop = Float.NaN
        lastDrawH = Float.NaN
        lastDrawW = Float.NaN
        lastFrameOy = Float.NaN
        frameLogCount = 0
        lastLogMs = 0L
    }

    private fun noteNonWalk(sample: DrawSample) {
        val clipFlip = lastClip == PacMazeSkinAnimClip.WALK && sample.clip != PacMazeSkinAnimClip.WALK
        if (clipFlip) {
            Log.w(TAG, ">>> CLIP_FLIP walk→${sample.clip.name} frame=${sample.frameIndex}")
        }
        lastClip = sample.clip
        lastPath = sample.path
        lastFrameIdx = -1
    }

    private fun detectAnomalies(sample: DrawSample): List<String> {
        val out = mutableListOf<String>()
        if (lastPath != null && lastPath != sample.path) {
            out += "PATH_FLIP ${lastPath}→${sample.path}"
        }
        if (lastClip == PacMazeSkinAnimClip.WALK && sample.clip != PacMazeSkinAnimClip.WALK) {
            out += "CLIP_FLIP walk→${sample.clip.name}"
        }
        if (lastFrameIdx >= 0 && sample.frameIndex != lastFrameIdx) {
            val delta = sample.frameIndex - lastFrameIdx
            val lastIdx = sample.frameCount - 1
            val wrappedLoop = lastFrameIdx == lastIdx && sample.frameIndex <= 1
            val idleSnap = !sample.player.locomoting && sample.frameIndex == 0
            when {
                delta < 0 && !wrappedLoop && !idleSnap &&
                    sample.player.locomoting &&
                    sample.frameIndex <= 2 && lastFrameIdx > 5 ->
                    out += "FRAME_RESET $lastFrameIdx→${sample.frameIndex}"
                abs(delta) > 2 && !wrappedLoop && !idleSnap && sample.player.locomoting ->
                    out += "FRAME_SKIP $lastFrameIdx→${sample.frameIndex} (Δ=$delta)"
            }
        }
        // syncWalkCycleToSprite 会按帧改 oy（~48px 幅值），仅对异常大跳报警
        if (!lastFrameOy.isNaN() && abs(sample.layout.frameOy - lastFrameOy) > 28f) {
            out += "FRAME_OY_JUMP oy=${lastFrameOy.toInt()}→${sample.layout.frameOy.toInt()}"
        }
        if (!lastDrawTop.isNaN()) {
            val dTop = abs(sample.drawTop - lastDrawTop)
            val footSync = sample.player.grounded &&
                (sample.clip == PacMazeSkinAnimClip.WALK || sample.clip == PacMazeSkinAnimClip.IDLE)
            if (dTop > DRAW_TOP_JUMP_PX && sample.player.locomoting && !footSync) {
                out += "DRAW_JUMP dTop=${fmt(dTop)}px"
            }
            if (abs(sample.drawH - lastDrawH) > DRAW_SIZE_JUMP_PX ||
                abs(sample.drawW - lastDrawW) > DRAW_SIZE_JUMP_PX
            ) {
                out += "SIZE_FLICKER d=${sample.drawW - lastDrawW.toInt()}x${sample.drawH - lastDrawH.toInt()}"
            }
        }
        if (!sample.player.locomoting && abs(sample.player.vx) > 40f) {
            out += "LOCO_STUCK vx=${sample.player.vx.toInt()} loco=false"
        }
        return out
    }

    private fun fmt(v: Float): String = "%.2f".format(v)
}
