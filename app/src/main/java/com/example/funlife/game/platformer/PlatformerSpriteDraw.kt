package com.example.funlife.game.platformer

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.funlife.game.platformer.catalog.PlatformerSpriteAtlasCache
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeSkinSheetPlayback
import kotlin.math.roundToInt

/** 横版精灵：脚点锚定 + 水平镜像（绝对坐标 drawImage；镜像走 nativeCanvas，避免 Android 16 withTransform 失效）。 */
internal object PlatformerSpriteDraw {

    private val filteredBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun drawChickLayout(
        scope: DrawScope,
        frame: ImageBitmap,
        layout: PlatformerPlayerSprites.ChickSpriteLayout,
        feetScreen: Offset,
        mirrorHorizontally: Boolean,
        debugPlayer: PlatformerPlayer? = null,
        debugFeetBefore: Offset? = null,
        debugMinHeadTopPx: Float? = null,
    ) {
        val drawW = layout.frameW.roundToInt().coerceAtLeast(1)
        val drawH = layout.frameH.roundToInt().coerceAtLeast(1)
        val left = feetScreen.x - layout.frameW * layout.feetXFrac + layout.frameOx
        val top = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dest = RectF(
            dstOffset.x.toFloat(),
            dstOffset.y.toFloat(),
            dstOffset.x + drawW.toFloat(),
            dstOffset.y + drawH.toFloat(),
        )
        if (debugPlayer != null && debugFeetBefore != null && debugMinHeadTopPx != null) {
            PlatformerSpriteDebugLog.maybeLog(
                player = debugPlayer,
                frame = frame,
                layout = layout,
                feetBefore = debugFeetBefore,
                feetAfter = feetScreen,
                minHeadTopPx = debugMinHeadTopPx,
                drawTop = dest.top,
                drawBottom = dest.bottom,
                canvasHeight = scope.size.height,
            )
        }
        if (!mirrorHorizontally) {
            scope.drawImage(
                image = frame,
                dstOffset = dstOffset,
                dstSize = IntSize(drawW, drawH),
                filterQuality = FilterQuality.High,
            )
            return
        }
        scope.drawContext.canvas.nativeCanvas.apply {
            save()
            scale(-1f, 1f, feetScreen.x, feetScreen.y)
            drawBitmap(frame.asAndroidBitmap(), null, dest, filteredBitmapPaint)
            restore()
        }
    }

    /** Sprite Sheet 主路径：整张 sheet 只 decode 一次，局内 srcRect 切帧。 */
    fun drawSheetLayout(
        scope: DrawScope,
        sheet: PacMazeSkinSheetPlayback,
        frameIndex: Int,
        layout: PlatformerPlayerSprites.ChickSpriteLayout,
        feetScreen: Offset,
        mirrorHorizontally: Boolean,
        debugPlayer: PlatformerPlayer? = null,
        debugFeetBefore: Offset? = null,
        debugMinHeadTopPx: Float? = null,
    ) {
        val src = sheet.srcRect(frameIndex)
        val drawW = layout.frameW.roundToInt().coerceAtLeast(1)
        val drawH = layout.frameH.roundToInt().coerceAtLeast(1)
        val left = feetScreen.x - layout.frameW * layout.feetXFrac + layout.frameOx
        val top = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dest = RectF(
            dstOffset.x.toFloat(),
            dstOffset.y.toFloat(),
            dstOffset.x + drawW.toFloat(),
            dstOffset.y + drawH.toFloat(),
        )
        if (debugPlayer != null && debugFeetBefore != null && debugMinHeadTopPx != null) {
            PlatformerSpriteDebugLog.maybeLog(
                player = debugPlayer,
                frame = sheet.bitmap,
                layout = layout,
                feetBefore = debugFeetBefore,
                feetAfter = feetScreen,
                minHeadTopPx = debugMinHeadTopPx,
                drawTop = dest.top,
                drawBottom = dest.bottom,
                canvasHeight = scope.size.height,
            )
        }
        val srcRect = android.graphics.Rect(src.left, src.top, src.right, src.bottom)
        val drawScale = layout.drawScale.coerceIn(0.72f, 1f)
        val pivotX = feetScreen.x
        val pivotY = feetScreen.y
        val needsNativeDraw = mirrorHorizontally || drawScale < 0.999f
        if (!needsNativeDraw) {
            scope.drawImage(
                image = sheet.bitmap,
                srcOffset = IntOffset(src.left, src.top),
                srcSize = IntSize(src.width, src.height),
                dstOffset = dstOffset,
                dstSize = IntSize(drawW, drawH),
                filterQuality = FilterQuality.High,
            )
            return
        }
        val scaleX = if (mirrorHorizontally) -drawScale else drawScale
        scope.drawContext.canvas.nativeCanvas.apply {
            save()
            scale(scaleX, drawScale, pivotX, pivotY)
            drawBitmap(sheet.bitmap.asAndroidBitmap(), srcRect, dest, filteredBitmapPaint)
            restore()
        }
    }

    /** Atlas 纹理 fast path：单张合图 + srcRect 采样，减少逐帧 bitmap 切换。 */
    fun drawAtlasLayout(
        scope: DrawScope,
        atlasFrame: PlatformerSpriteAtlasCache.AtlasFrame,
        layout: PlatformerPlayerSprites.ChickSpriteLayout,
        feetScreen: Offset,
        mirrorHorizontally: Boolean,
    ) {
        val src = atlasFrame.srcRect
        val drawW = layout.frameW.roundToInt().coerceAtLeast(1)
        val drawH = layout.frameH.roundToInt().coerceAtLeast(1)
        val left = feetScreen.x - layout.frameW * layout.feetXFrac + layout.frameOx
        val top = feetScreen.y - layout.frameH * layout.feetYFrac + layout.frameOy
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dest = RectF(
            dstOffset.x.toFloat(),
            dstOffset.y.toFloat(),
            dstOffset.x + drawW.toFloat(),
            dstOffset.y + drawH.toFloat(),
        )
        val srcRect = android.graphics.Rect(src.left, src.top, src.right, src.bottom)
        if (!mirrorHorizontally) {
            scope.drawImage(
                image = atlasFrame.atlas,
                srcOffset = IntOffset(src.left, src.top),
                srcSize = IntSize(src.width, src.height),
                dstOffset = dstOffset,
                dstSize = IntSize(drawW, drawH),
                filterQuality = FilterQuality.High,
            )
            return
        }
        scope.drawContext.canvas.nativeCanvas.apply {
            save()
            scale(-1f, 1f, feetScreen.x, feetScreen.y)
            drawBitmap(
                atlasFrame.atlas.asAndroidBitmap(),
                srcRect,
                dest,
                filteredBitmapPaint,
            )
            restore()
        }
    }
}
