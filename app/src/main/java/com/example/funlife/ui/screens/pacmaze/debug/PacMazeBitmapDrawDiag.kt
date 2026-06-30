package com.example.funlife.ui.screens.pacmaze.debug

import android.util.Log
import com.example.funlife.BuildConfig
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapContentTrim
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapCorridorDrawPolicy
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapFeetAnchor
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapSoleAlign
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSheetCellFeetCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimManifest
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinLayoutEngine
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinRegistry
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs

/**
 * 位图绘制链路结构化诊断（企业级排查）。
 *
 * Logcat: `adb logcat -s PacMazeMotionDiag | findstr BITMAP_DRAW`
 *
 * 自动化: `powershell -File scripts/pacmaze-motion-audit.ps1`
 */
internal object PacMazeBitmapDrawDiag {

    private const val VISUAL_JUMP_PX = 3.0f
    private const val MAX_FRAME_LOGS = 400

    private var lastFrameIndex = -1
    private var lastVisualSoleY = Float.NaN
    private var lastVisualTopY = Float.NaN
    private var lastLayoutFeetY = Float.NaN
    private var frameLogCount = 0

    private var totalDrawSamples = 0
    private var frameChanges = 0
    private var visualSoleJumps = 0
    private var visualHeadJumps = 0
    private var layoutFeetJumps = 0
    private var soleAlignZeroOnFrameChange = 0
    private var maxHeadJumpPx = 0f
    private var maxSoleJumpPx = 0f

    fun resetSession() {
        lastFrameIndex = -1
        lastVisualSoleY = Float.NaN
        lastVisualTopY = Float.NaN
        lastLayoutFeetY = Float.NaN
        frameLogCount = 0
        totalDrawSamples = 0
        frameChanges = 0
        visualSoleJumps = 0
        visualHeadJumps = 0
        layoutFeetJumps = 0
        soleAlignZeroOnFrameChange = 0
        maxHeadJumpPx = 0f
        maxSoleJumpPx = 0f
    }

    fun noteOrientedDraw(
        skinId: PacMazeSkinId?,
        frameIndex: Int,
        layout: PacMazeSkinLayoutEngine.Layout,
        soleAlignOffsetY: Float,
        scaleY: Float = 1f,
        facing: Direction,
        image: ImageBitmap?,
        sheet: PacMazeSkinSheetPlayback?,
        clip: PacMazeSkinAnimClip?,
        drawPath: String,
    ) {
        if (!BuildConfig.DEBUG || !PacMazeMotionDiag.enabled || skinId == null) return
        if (frameLogCount >= MAX_FRAME_LOGS) return

        val travel = PacMazeSkinRegistry.drawTravelFacing
        val horizontal = travel == Direction.LEFT || travel == Direction.RIGHT
        if (!horizontal) return

        val suppressBob = PacMazeBitmapCorridorDrawPolicy.shouldSuppressWalkBob()
        val refFeetY = when {
            sheet != null && clip != null -> {
                val manifest = PacMazeSkinAnimManifest.platformerClipMetrics(skinId, clip)
                manifest?.maxOf { it.feetY }
                    ?: PacMazeSheetCellFeetCache.cycleMaxFeetY(skinId, clip, sheet)
                    ?: PacMazeBitmapFeetAnchor.rawCycleMaxFeetY(skinId)
            }
            else -> PacMazeBitmapFeetAnchor.rawCycleMaxFeetY(skinId)
        }
        val frameFeetY = when {
            sheet != null && clip != null -> {
                PacMazeSkinAnimManifest.platformerFrameMetrics(skinId, clip, frameIndex)?.feetY
                    ?: PacMazeSheetCellFeetCache.cellFeetY(skinId, clip, sheet, frameIndex)
                    ?: refFeetY
            }
            image != null -> PacMazeBitmapFeetAnchor.rawFrameFeetY(image, skinId)
            else -> refFeetY
        }
        val opaqueTopFrac = resolveOpaqueTopFrac(skinId, frameIndex, image, sheet, clip)
        val h = layout.height
        val pivotY = layout.feetFrac
        val layoutFeetY = layout.feetCenter.y
        val visualSoleY = layoutFeetY + soleAlignOffsetY
        val visualTopY = layoutFeetY - h * pivotY * scaleY + opaqueTopFrac * h * scaleY + soleAlignOffsetY

        totalDrawSamples++
        val frameChanged = lastFrameIndex >= 0 && frameIndex != lastFrameIndex
        if (frameChanged) {
            frameChanges++
            if (abs(frameFeetY - refFeetY) > 0.004f && abs(soleAlignOffsetY) < 0.5f) {
                soleAlignZeroOnFrameChange++
            }
        }

        val dSole = if (!lastVisualSoleY.isNaN()) visualSoleY - lastVisualSoleY else 0f
        val dTop = if (!lastVisualTopY.isNaN()) visualTopY - lastVisualTopY else 0f
        val dLayoutFeet = if (!lastLayoutFeetY.isNaN()) layoutFeetY - lastLayoutFeetY else 0f

        if (frameChanged && abs(dSole) > VISUAL_JUMP_PX) {
            visualSoleJumps++
            maxSoleJumpPx = maxOf(maxSoleJumpPx, abs(dSole))
            logWarn(
                ">>> VISUAL_SOLE_JUMP frame=$lastFrameIndex->$frameIndex dSole=${fmt(dSole)} " +
                    "soleDy=${fmt(soleAlignOffsetY)} refFY=${fmt(refFeetY)} frameFY=${fmt(frameFeetY)}",
            )
        }
        if (frameChanged && abs(dTop) > VISUAL_JUMP_PX) {
            visualHeadJumps++
            maxHeadJumpPx = maxOf(maxHeadJumpPx, abs(dTop))
            logWarn(
                ">>> VISUAL_HEAD_JUMP frame=$lastFrameIndex->$frameIndex dTop=${fmt(dTop)} " +
                    "topFrac=${fmt(opaqueTopFrac)} soleDy=${fmt(soleAlignOffsetY)} layoutH=${fmt(h)}",
            )
        }
        if (abs(dLayoutFeet) > 1f) {
            layoutFeetJumps++
            logWarn(
                ">>> LAYOUT_FEET_JUMP dLayoutFeet=${fmt(dLayoutFeet)} feetY=${layoutFeetY.toInt()}",
            )
        }

        val shouldLog = frameChanged ||
            frameLogCount < 8 ||
            visualSoleJumps + visualHeadJumps > 0 && frameLogCount % 4 == 0
        if (shouldLog) {
            frameLogCount++
            Log.i(
                PacMazeMotionDiag.TAG,
                "BITMAP_DRAW skin=${skinId.name} path=$drawPath frame=$frameIndex " +
                    "layoutFeetY=${layoutFeetY.toInt()} pivotFY=${fmt(pivotY)} layoutH=${fmt(h)} " +
                    "soleDy=${fmt(soleAlignOffsetY)} scaleY=${fmt(scaleY)} refFY=${fmt(refFeetY)} " +
                    "frameFY=${fmt(frameFeetY)} topFrac=${fmt(opaqueTopFrac)} visualSoleY=${visualSoleY.toInt()} " +
                    "visualTopY=${visualTopY.toInt()} suppressBob=$suppressBob facing=${facing.name}",
            )
        }

        lastFrameIndex = frameIndex
        lastVisualSoleY = visualSoleY
        lastVisualTopY = visualTopY
        lastLayoutFeetY = layoutFeetY
    }

    fun printSessionSummary(skinId: String? = null) {
        if (!BuildConfig.DEBUG || !PacMazeMotionDiag.enabled) return
        val diagnosis = when {
            visualHeadJumps > visualSoleJumps && visualHeadJumps >= 2 ->
                "HEAD_BOUNCE(frame_opaque_top_varies,sole_aligned)"
            visualSoleJumps >= 2 ->
                "SOLE_MISALIGN(soleDy_insufficient)"
            soleAlignZeroOnFrameChange >= 2 ->
                "SOLE_ALIGN_INACTIVE(frameFeet_not_read)"
            layoutFeetJumps >= 2 ->
                "LAYOUT_ANCHOR_DRIFT"
            frameChanges >= 4 && visualHeadJumps == 0 && visualSoleJumps == 0 ->
                "NO_METRIC_JUMP(check_art_or_refresh_rate)"
            else -> "INCONCLUSIVE"
        }
        Log.w(
            PacMazeMotionDiag.TAG,
            ">>> AUDIT_SUMMARY skin=${skinId ?: "?"} samples=$totalDrawSamples " +
                "frameChanges=$frameChanges soleJumps=$visualSoleJumps headJumps=$visualHeadJumps " +
                "layoutFeetJumps=$layoutFeetJumps soleAlignZero=$soleAlignZeroOnFrameChange " +
                "maxHeadJump=${fmt(maxHeadJumpPx)} maxSoleJump=${fmt(maxSoleJumpPx)} " +
                "diagnosis=$diagnosis",
        )
    }

    private fun resolveOpaqueTopFrac(
        skinId: PacMazeSkinId,
        frameIndex: Int,
        image: ImageBitmap?,
        sheet: PacMazeSkinSheetPlayback?,
        clip: PacMazeSkinAnimClip?,
    ): Float {
        if (clip != null) {
            val metrics = PacMazeSkinAnimManifest.platformerFrameMetrics(skinId, clip, frameIndex)
            if (metrics != null) {
                return metrics.headTopY.coerceIn(0f, 0.95f)
            }
        }
        if (sheet != null && clip != null) {
            val feetY = PacMazeSheetCellFeetCache.cellFeetY(skinId, clip, sheet, frameIndex) ?: 0.92f
            val hFrac = PacMazeSheetCellFeetCache.frameOpaqueHeightFrac(skinId, clip, sheet, frameIndex)
            return (feetY - hFrac).coerceIn(0f, 0.95f)
        }
        if (image != null) {
            return PacMazeBitmapContentTrim.cachedOpaqueContentSpan(image).topFrac
        }
        return 0.08f
    }

    private fun logWarn(msg: String) {
        if (frameLogCount < MAX_FRAME_LOGS) {
            Log.w(PacMazeMotionDiag.TAG, msg)
        }
    }

    private fun fmt(v: Float): String = "%.3f".format(v)
}
