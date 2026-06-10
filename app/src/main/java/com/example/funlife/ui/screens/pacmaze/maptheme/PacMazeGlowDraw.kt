package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

internal fun DrawScope.drawGlowRoundRect(
    rect: Rect,
    cornerRadius: Float,
    fillColor: Color,
    strokeColor: Color,
    glowColor: Color,
    strokeWidth: Float = 2f,
    glowBlur: Float = 10f,
    glowAlpha: Float = 0.55f,
) {
    drawRoundRect(
        color = fillColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(cornerRadius),
    )
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            color = glowColor.copy(alpha = glowAlpha).toArgb()
            maskFilter = BlurMaskFilter(glowBlur, BlurMaskFilter.Blur.NORMAL)
        }
        drawRoundRect(
            rect.left,
            rect.top,
            rect.right,
            rect.bottom,
            cornerRadius,
            cornerRadius,
            paint,
        )
    }
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(rect.left, rect.top),
        size = Size(rect.width, rect.height),
        cornerRadius = CornerRadius(cornerRadius),
        style = Stroke(width = strokeWidth),
    )
}

internal fun DrawScope.drawGlowCircle(
    center: Offset,
    radius: Float,
    coreColor: Color,
    glowColor: Color,
    glowBlur: Float = 12f,
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = glowColor.copy(alpha = 0.45f).toArgb()
            maskFilter = BlurMaskFilter(glowBlur, BlurMaskFilter.Blur.NORMAL)
        }
        drawCircle(center.x, center.y, radius * 1.6f, paint)
    }
    drawCircle(color = coreColor, radius = radius, center = center)
}
