package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * 裁掉 PNG 透明留白，并检测真实脚点纵坐标（相对图高 0~1，1=图底）。
 */
internal object PacMazeBitmapContentTrim {

    private const val ALPHA_FLOOR = 14
    private const val DEFAULT_FEET_Y = 0.94f

    fun trimToOpaqueContent(source: ImageBitmap): ImageBitmap =
        trimAndroidBitmap(source.asAndroidBitmap()).asImageBitmap()

    fun detectFeetYFraction(source: ImageBitmap): Float =
        detectFeetYFraction(source.asAndroidBitmap())

    /**
     * 脚点 = 底缘带宽行（车轮/鞋底）+ bbox 底边取较低者，与地砖线对齐最稳。
     */
    fun detectFeetYFraction(source: Bitmap): Float {
        val w = source.width
        val h = source.height
        if (w <= 1 || h <= 1) return DEFAULT_FEET_Y

        var maxY = -1
        var minY = h
        val step = if (w * h > 480_000) 2 else 1
        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    maxY = max(maxY, y)
                    minY = min(minY, y)
                }
            }
        }
        if (maxY < 0) return DEFAULT_FEET_Y

        val bboxFeet = (maxY + 0.5f) / h
        val wheelFeet = detectWheelBandFeet(source, w, h, maxY, minY, step)
        val centerFeet = detectCenterContactFeet(source, w, h, maxY, minY, step)
        val contact = when {
            centerFeet > 0f && wheelFeet - centerFeet > 0.06f -> centerFeet
            else -> wheelFeet
        }
        return min(contact, bboxFeet * 0.995f).coerceIn(0.72f, 0.998f)
    }

    /**
     * 宽体梗图（龙虾爪等）：在底缘带 + 中心 50% 宽内找触地行，避免全宽 bbox 把脚点抬到爪尖。
     */
    private fun detectCenterContactFeet(
        source: Bitmap,
        w: Int,
        h: Int,
        maxY: Int,
        minY: Int,
        step: Int,
    ): Float {
        val bandHeight = (h * 0.28f).toInt().coerceAtLeast(2)
        val bandTop = max(minY, maxY - bandHeight)
        val left = (w * 0.25f).toInt()
        val right = (w * 0.75f).toInt().coerceAtMost(w - 1)
        var touchY = -1
        for (y in maxY downTo bandTop step step) {
            var hasPixel = false
            var x = left
            while (x <= right) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    hasPixel = true
                    break
                }
                x += step
            }
            if (hasPixel) {
                touchY = y
                break
            }
        }
        return if (touchY >= 0) (touchY + 0.5f) / h else -1f
    }

    /** 底缘 24% 带内找水平跨度最大的行（车轮/轮椅底缘），忽略上方人脸等窄条。 */
    private fun detectWheelBandFeet(
        source: Bitmap,
        w: Int,
        h: Int,
        maxY: Int,
        minY: Int,
        step: Int,
    ): Float {
        val bandHeight = (h * 0.24f).toInt().coerceAtLeast(2)
        val bandTop = max(minY, maxY - bandHeight)
        var bestY = maxY
        var bestSpan = 0
        var bestMass = 0
        for (y in maxY downTo bandTop step step) {
            var rowMinX = w
            var rowMaxX = -1
            var mass = 0
            var x = 0
            while (x < w) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    rowMinX = min(rowMinX, x)
                    rowMaxX = max(rowMaxX, x)
                    mass++
                }
                x += step
            }
            val span = if (rowMaxX >= rowMinX) rowMaxX - rowMinX + 1 else 0
            val better = span > bestSpan || (span == bestSpan && mass > bestMass) ||
                (span == bestSpan && mass == bestMass && y > bestY)
            if (better) {
                bestSpan = span
                bestMass = mass
                bestY = y
            }
        }
        if (bestSpan <= 0) return (maxY + 0.5f) / h

        // 宽爪行（span > 55% 图宽）优先取更靠下的触地行，而不是横向最宽行
        if (bestSpan > w * 0.55f) {
            var touchY = maxY
            for (y in maxY downTo bandTop step step) {
                var rowMinX = w
                var rowMaxX = -1
                var x = 0
                while (x < w) {
                    if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                        rowMinX = min(rowMinX, x)
                        rowMaxX = max(rowMaxX, x)
                    }
                    x += step
                }
                val span = if (rowMaxX >= rowMinX) rowMaxX - rowMinX + 1 else 0
                if (span in 1..(w * 0.55f).toInt()) {
                    bestY = y
                    break
                }
                touchY = y
            }
            if (bestSpan > w * 0.55f) bestY = touchY
        }

        // 带宽行向下再扫 1~2 行取真正触地边（轮缘）
        var touchY = bestY
        val touchScan = min(3, maxY - bestY + 1)
        for (y in bestY..min(maxY, bestY + touchScan)) {
            var hasPixel = false
            var x = 0
            while (x < w) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    hasPixel = true
                    break
                }
                x += step
            }
            if (hasPixel) touchY = y
        }
        return (touchY + 0.5f) / h
    }

    /** 脚点横坐标（相对图宽 0~1），取车轮带中心，竖向移动时对齐走廊中心。 */
    fun detectFeetXFraction(source: ImageBitmap): Float =
        detectFeetXFraction(source.asAndroidBitmap())

    fun detectFeetXFraction(source: Bitmap): Float {
        val w = source.width
        val h = source.height
        if (w <= 1 || h <= 1) return 0.5f

        var maxY = -1
        var minY = h
        val step = if (w * h > 480_000) 2 else 1
        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    maxY = max(maxY, y)
                    minY = min(minY, y)
                }
            }
        }
        if (maxY < 0) return 0.5f

        val bandHeight = (h * 0.24f).toInt().coerceAtLeast(2)
        val bandTop = max(minY, maxY - bandHeight)
        var bestY = maxY
        var bestSpan = 0
        var bestMinX = w / 2
        var bestMaxX = w / 2
        for (y in maxY downTo bandTop step step) {
            var rowMinX = w
            var rowMaxX = -1
            var x = 0
            while (x < w) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    rowMinX = min(rowMinX, x)
                    rowMaxX = max(rowMaxX, x)
                }
                x += step
            }
            val span = if (rowMaxX >= rowMinX) rowMaxX - rowMinX + 1 else 0
            if (span > bestSpan || (span == bestSpan && y > bestY)) {
                bestSpan = span
                bestY = y
                bestMinX = rowMinX
                bestMaxX = rowMaxX
            }
        }
        if (bestSpan <= 0) return 0.5f
        return ((bestMinX + bestMaxX) * 0.5f + 0.5f) / w.toFloat().coerceAtLeast(1f)
    }

    fun trimAndroidBitmap(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 1 || h <= 1) return source

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        val step = if (w * h > 640_000) 2 else 1
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                if (Color.alpha(source.getPixel(x, y)) > ALPHA_FLOOR) {
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                }
                x += step
            }
            y += step
        }
        if (maxX < minX || maxY < minY) return source

        val padX = ((maxX - minX + 1) * 0.02f).toInt().coerceAtLeast(1)
        val padY = ((maxY - minY + 1) * 0.008f).toInt().coerceAtMost(2)
        val left = (minX - padX).coerceAtLeast(0)
        val top = (minY - padY).coerceAtLeast(0)
        val right = (maxX + padX + 1).coerceAtMost(w)
        val bottom = (maxY + padY).coerceAtMost(h)
        val cw = right - left
        val ch = bottom - top
        if (cw <= 0 || ch <= 0) return source
        if (left == 0 && top == 0 && cw == w && ch == h) return source

        return Bitmap.createBitmap(source, left, top, cw, ch)
    }
}
