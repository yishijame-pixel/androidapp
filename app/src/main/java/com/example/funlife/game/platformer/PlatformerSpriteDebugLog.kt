package com.example.funlife.game.platformer

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.funlife.BuildConfig
import com.example.funlife.game.platformer.PlatformerPlayerSprites.ChickSpriteLayout
import com.example.funlife.resource.ResourceStore
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeBitmapFeetAnchor
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinAnimClip
import kotlin.math.floor

/**
 * Debug 诊断：横版主角绘制是否会被 Canvas 上边界裁切。
 *
 * Logcat 过滤：`PlatformerSpriteDbg`
 *
 * 关注字段：
 * - `headTop` / `drawTop`：发型顶与贴图顶（屏幕 Y，越小越靠上）
 * - `wouldClipTop`：drawTop < 0 时会被裁
 * - `shiftY`：`adjustFeetScreenKeepHeadVisible` 下移补偿
 * - `bundleVer` / `framePx`：资源版本与帧尺寸是否匹配 v16（710×750）
 */
internal object PlatformerSpriteDebugLog {

    private const val TAG = "PlatformerSpriteDbg"
    private const val THROTTLE_MS = 280L

    private var lastLogAtMs = 0L

    data class Snapshot(
        val clip: String,
        val grounded: Boolean,
        val vy: Float,
        val framePx: String,
        val bundleVer: Int?,
        val feetYFrac: Float,
        val headTopFrac: Float,
        val frameOy: Float,
        val frameH: Float,
        val headTopBefore: Float,
        val headTopAfter: Float,
        val minHeadTop: Float,
        val shiftY: Float,
        val drawTop: Float,
        val drawBottom: Float,
        val canvasH: Float,
        val wouldClipTop: Boolean,
    )

    fun maybeLog(
        player: PlatformerPlayer,
        frame: ImageBitmap,
        layout: ChickSpriteLayout,
        feetBefore: Offset,
        feetAfter: Offset,
        minHeadTopPx: Float,
        drawTop: Float,
        drawBottom: Float,
        canvasHeight: Float,
        clip: PacMazeSkinAnimClip = if (player.grounded) PacMazeSkinAnimClip.WALK else PacMazeSkinAnimClip.JUMP,
    ) {
        if (!BuildConfig.DEBUG) return
        val now = System.currentTimeMillis()
        val headTopFrac = PacMazeBitmapFeetAnchor.platformerHeadTopFraction(
            frame,
            PlatformerPlayerSprites.skinId,
        )
        val headTopBefore = feetBefore.y - layout.frameH * layout.feetYFrac +
            layout.frameOy + layout.frameH * headTopFrac
        val headTopAfter = feetAfter.y - layout.frameH * layout.feetYFrac +
            layout.frameOy + layout.frameH * headTopFrac
        val wouldClip = drawTop < 0f
        val interesting = !player.grounded || wouldClip || headTopBefore < minHeadTopPx
        if (!interesting) return
        if (now - lastLogAtMs < THROTTLE_MS && !wouldClip) return
        lastLogAtMs = now

        val snap = Snapshot(
            clip = clip.name,
            grounded = player.grounded,
            vy = player.vy,
            framePx = "${frame.width}x${frame.height}",
            bundleVer = ResourceStore.readPacMazeSkinsBundleVersion(),
            feetYFrac = layout.feetYFrac,
            headTopFrac = headTopFrac,
            frameOy = layout.frameOy,
            frameH = layout.frameH,
            headTopBefore = headTopBefore,
            headTopAfter = headTopAfter,
            minHeadTop = minHeadTopPx,
            shiftY = feetAfter.y - feetBefore.y,
            drawTop = drawTop,
            drawBottom = drawBottom,
            canvasH = canvasHeight,
            wouldClipTop = wouldClip,
        )
        Log.w(TAG, format(snap))
    }

    private fun format(s: Snapshot): String = buildString {
        append("clip=${s.clip} grounded=${s.grounded} vy=${s.vy.toInt()} ")
        append("bundle=v${s.bundleVer ?: "?"} frame=${s.framePx} ")
        append("feetY=${fmt(s.feetYFrac)} headFrac=${fmt(s.headTopFrac)} frameOy=${s.frameOy.toInt()} frameH=${s.frameH.toInt()} ")
        append("headTop ${s.headTopBefore.toInt()}→${s.headTopAfter.toInt()} min=${s.minHeadTop.toInt()} shiftY=${s.shiftY.toInt()} ")
        append("drawTop=${floor(s.drawTop).toInt()} drawBottom=${s.drawBottom.toInt()} canvasH=${s.canvasH.toInt()} ")
        append(if (s.wouldClipTop) ">>> WOULD_CLIP_TOP <<<" else "ok")
    }

    private fun fmt(v: Float): String = "%.3f".format(v)
}
