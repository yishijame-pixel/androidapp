package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeMarkerKind

internal object ThemeMarkerDraw {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        if (ctx.markers.isEmpty()) return
        val cell = ctx.cell
        ctx.markers.forEach { marker ->
            val rect = ctx.tileRect(marker.x, marker.y)
            when (ctx.config.id) {
                PacMazeMapThemeId.GARDEN -> drawGardenMarker(scope, rect, cell, marker)
                PacMazeMapThemeId.FOOD -> drawFoodMarker(scope, rect, cell, marker)
                PacMazeMapThemeId.CHINESE -> drawChineseMarker(scope, rect, cell, marker)
                else -> Unit
            }
        }
    }

    private fun drawGardenMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF8BC34A), Color(0xFF558B2F)),
            ),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
        )
        scope.drawRoundRect(
            color = Color(0xFF5D4037),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
            style = Stroke(cell * 0.04f),
        )
        val text = when (marker.kind) {
            PacMazeMarkerKind.START -> "起"
            PacMazeMarkerKind.CHECKPOINT -> marker.tag.ifBlank { "景" }
            PacMazeMarkerKind.EXIT -> "出口"
        }
        drawLabel(scope, inner, cell, text, Color(0xFFFFF8E1), cell * 0.28f)
    }

    private fun drawFoodMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(Color(0xFFFF7043), inner.topLeft, inner.size, cornerRadius = CornerRadius(cell * 0.12f))
        scope.drawRoundRect(Color.White.copy(alpha = 0.9f), inner.topLeft, inner.size, cornerRadius = CornerRadius(cell * 0.12f), style = Stroke(2f))
        val text = when (marker.kind) {
            PacMazeMarkerKind.START -> "GO!"
            PacMazeMarkerKind.CHECKPOINT -> marker.label.ifBlank { "★" }
            PacMazeMarkerKind.EXIT -> "EXIT"
        }
        drawLabel(scope, inner, cell, text, Color.White, cell * 0.2f)
    }

    private fun drawChineseMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker) {
        val pad = cell * 0.04f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        val plaque = Rect(inner.left, inner.top, inner.right, inner.top + inner.height * 0.72f)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF121212))),
            topLeft = plaque.topLeft,
            size = plaque.size,
            cornerRadius = CornerRadius(cell * 0.04f),
        )
        scope.drawRoundRect(
            color = Color(0xFFD4AF37),
            topLeft = plaque.topLeft,
            size = plaque.size,
            cornerRadius = CornerRadius(cell * 0.04f),
            style = Stroke(cell * 0.035f),
        )
        val roof = Rect(inner.left - cell * 0.02f, inner.top - cell * 0.06f, inner.right + cell * 0.02f, inner.top + cell * 0.14f)
        scope.drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF3D5248), Color(0xFF5C7568), Color(0xFF3D5248))),
            topLeft = roof.topLeft,
            size = roof.size,
            cornerRadius = CornerRadius(cell * 0.03f),
        )
        scope.drawLine(
            color = Color(0xFFD4AF37).copy(alpha = 0.8f),
            start = Offset(roof.left + cell * 0.08f, roof.center.y),
            end = Offset(roof.right - cell * 0.08f, roof.center.y),
            strokeWidth = cell * 0.025f,
        )
        val text = when (marker.kind) {
            PacMazeMarkerKind.START -> "起点"
            PacMazeMarkerKind.CHECKPOINT -> marker.label.ifBlank { "关" }
            PacMazeMarkerKind.EXIT -> "出口"
        }
        drawLabel(scope, plaque, cell, text, Color(0xFFFFCA28), cell * 0.2f)
        if (marker.kind == PacMazeMarkerKind.CHECKPOINT) {
            drawStoneTablet(scope, Rect(inner.left, inner.bottom - cell * 0.22f, inner.right, inner.bottom), cell)
        }
    }

    private fun drawStoneTablet(scope: DrawScope, rect: Rect, cell: Float) {
        scope.drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF9E9E90), Color(0xFF757568))),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(cell * 0.03f),
        )
        scope.drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(rect.left + cell * 0.12f, rect.center.y),
            end = Offset(rect.right - cell * 0.12f, rect.center.y),
            strokeWidth = 0.8f,
        )
    }

    private fun drawLabel(scope: DrawScope, rect: Rect, cell: Float, text: String, color: Color, size: Float) {
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                textSize = size
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                typeface = Typeface.DEFAULT_BOLD
            }
            drawText(text, rect.center.x, rect.center.y + cell * 0.08f, paint)
        }
    }
}
