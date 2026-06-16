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
import com.example.funlife.social.game.engine.pacmaze.PacMazePortals

internal object ThemeMarkerDraw {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        if (ctx.markers.isEmpty()) return
        val cell = ctx.cell
        ctx.markers.forEach { marker ->
            val rect = ctx.tileRect(marker.x, marker.y)
            val visited = when {
                marker.tag == "LINK" -> PacMazePortals.isPortalArmed(ctx.world, marker)
                marker.kind == PacMazeMarkerKind.CHECKPOINT &&
                    marker.tag.isNotBlank() &&
                    !PacMazePortals.isArmedTag(marker.tag) ->
                    marker.tag in ctx.world.visitedCheckpointTags
                else -> false
            }
            if (marker.tag == "LINK") {
                PacMazePortalVisual.drawLinkMarker(scope, ctx, rect, cell, marker)
                return@forEach
            }
            when (ctx.config.id) {
                PacMazeMapThemeId.GARDEN -> drawGardenMarker(scope, rect, cell, marker, visited)
                PacMazeMapThemeId.FOOD -> drawFoodMarker(scope, rect, cell, marker, visited)
                PacMazeMapThemeId.CHINESE -> drawChineseMarker(scope, rect, cell, marker, visited)
                PacMazeMapThemeId.MAZE -> drawMazeMarker(scope, rect, cell, marker, ctx.animPhase, visited)
                else -> Unit
            }
        }
    }

    private fun drawGardenMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker, visited: Boolean) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        val fillTop = if (visited) Color(0xFF66BB6A) else Color(0xFF8BC34A)
        val fillBottom = if (visited) Color(0xFF388E3C) else Color(0xFF558B2F)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(listOf(fillTop, fillBottom)),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
        )
        scope.drawRoundRect(
            color = if (visited) Color(0xFF1B5E20) else Color(0xFF5D4037),
            topLeft = inner.topLeft,
            size = inner.size,
            cornerRadius = CornerRadius(cell * 0.1f),
            style = Stroke(cell * 0.04f),
        )
        val text = when (marker.kind) {
            PacMazeMarkerKind.START -> "起"
            PacMazeMarkerKind.CHECKPOINT -> if (visited) "✓" else marker.tag.ifBlank { "景" }
            PacMazeMarkerKind.EXIT -> "出口"
            PacMazeMarkerKind.ITEM_FACTORY -> return
        }
        drawLabel(scope, inner, cell, text, Color(0xFFFFF8E1), cell * 0.28f)
    }

    private fun drawFoodMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker, visited: Boolean) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            if (visited) Color(0xFF43A047) else Color(0xFFFF7043),
            inner.topLeft,
            inner.size,
            cornerRadius = CornerRadius(cell * 0.12f),
        )
        scope.drawRoundRect(Color.White.copy(alpha = 0.9f), inner.topLeft, inner.size, cornerRadius = CornerRadius(cell * 0.12f), style = Stroke(2f))
        val text = when (marker.kind) {
            PacMazeMarkerKind.START -> "GO!"
            PacMazeMarkerKind.CHECKPOINT -> if (visited) "✓" else marker.label.ifBlank { "★" }
            PacMazeMarkerKind.EXIT -> "EXIT"
            PacMazeMarkerKind.ITEM_FACTORY -> return
        }
        drawLabel(scope, inner, cell, text, Color.White, cell * 0.2f)
    }

    private fun drawChineseMarker(scope: DrawScope, rect: Rect, cell: Float, marker: PacMazeMapMarker, visited: Boolean) {
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
            color = if (visited) Color(0xFF22C55E) else Color(0xFFD4AF37),
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
            PacMazeMarkerKind.CHECKPOINT -> if (visited) "✓" else marker.label.ifBlank { "关" }
            PacMazeMarkerKind.EXIT -> "出口"
            PacMazeMarkerKind.ITEM_FACTORY -> return
        }
        drawLabel(scope, plaque, cell, text, if (visited) Color(0xFFBBF7D0) else Color(0xFFFFCA28), cell * 0.2f)
        if (marker.kind == PacMazeMarkerKind.CHECKPOINT) {
            drawStoneTablet(scope, Rect(inner.left, inner.bottom - cell * 0.22f, inner.right, inner.bottom), cell)
        }
    }

    private fun drawMazeMarker(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        marker: PacMazeMapMarker,
        animPhase: Float,
        visited: Boolean,
    ) {
        val pulse = 0.7f + 0.3f * kotlin.math.sin(animPhase * 2.2f)
        val pad = cell * 0.08f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        when (marker.kind) {
            PacMazeMarkerKind.EXIT -> {
                scope.drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB74D).copy(alpha = 0.85f * pulse),
                            Color(0xFFFF9800).copy(alpha = 0.55f),
                        ),
                    ),
                    topLeft = inner.topLeft,
                    size = inner.size,
                    cornerRadius = CornerRadius(cell * 0.14f),
                )
                scope.drawRoundRect(
                    color = Color.White.copy(alpha = 0.75f),
                    topLeft = inner.topLeft,
                    size = inner.size,
                    cornerRadius = CornerRadius(cell * 0.14f),
                    style = Stroke(cell * 0.05f),
                )
                drawLabel(scope, inner, cell, "出口", Color(0xFF1A1208), cell * 0.24f)
            }
            PacMazeMarkerKind.START -> {
                scope.drawRoundRect(
                    color = Color(0xFF455A64).copy(alpha = 0.85f),
                    topLeft = inner.topLeft,
                    size = inner.size,
                    cornerRadius = CornerRadius(cell * 0.1f),
                )
                drawLabel(scope, inner, cell, "起", Color(0xFFECEFF1), cell * 0.26f)
            }
            PacMazeMarkerKind.CHECKPOINT -> {
                scope.drawRoundRect(
                    color = if (visited) Color(0xFF166534).copy(alpha = 0.9f) else Color(0xFF37474F).copy(alpha = 0.85f),
                    topLeft = inner.topLeft,
                    size = inner.size,
                    cornerRadius = CornerRadius(cell * 0.1f),
                )
                drawLabel(scope, inner, cell, if (visited) "✓" else marker.label.ifBlank { "关" }, Color.White, cell * 0.24f)
            }
            else -> Unit
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
