package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeMarkerKind
import com.example.funlife.social.game.engine.pacmaze.PacMazePortals
import kotlin.math.cos
import kotlin.math.sin

internal object CyberMapDecorations {

    fun drawMarkers(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        if (ctx.markers.isEmpty()) return
        ctx.markers.forEach { marker ->
            val rect = ctx.tileRect(marker.x, marker.y)
            val cell = ctx.tileMetric(rect)
            when (marker.kind) {
                PacMazeMarkerKind.START -> drawStartTile(scope, rect, cell)
                PacMazeMarkerKind.CHECKPOINT, PacMazeMarkerKind.EXIT -> {
                    if (marker.tag == "HINT") {
                        drawHintPellet(scope, rect, cell)
                    } else if (marker.tag == "LINK") {
                        PacMazePortalVisual.drawLinkMarker(scope, ctx, rect, cell, marker)
                    } else {
                        drawCyberPortal(scope, rect, cell, marker, ctx)
                    }
                }
                PacMazeMarkerKind.ITEM_FACTORY -> Unit
            }
        }
    }

    private fun drawHintPellet(scope: DrawScope, rect: Rect, cell: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawCircle(
            color = Color(0xFF4FC3F7).copy(alpha = 0.35f),
            radius = cell * 0.34f,
            center = center,
        )
        scope.drawCircle(
            color = Color(0xFF4FC3F7),
            radius = cell * 0.16f,
            center = center,
        )
    }

    private fun drawStartTile(scope: DrawScope, rect: Rect, cell: Float) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRect(color = Color(0xFF0A0A0A), topLeft = inner.topLeft, size = inner.size)
        scope.drawRect(
            color = Color.White,
            topLeft = inner.topLeft,
            size = inner.size,
            style = Stroke(width = cell * 0.05f),
        )
        drawHazardStripes(
            scope,
            Rect(inner.left, inner.top, inner.right, inner.top + inner.height * 0.22f),
            CyberVisualEffects.NeonRed,
        )
        drawHazardStripes(
            scope,
            Rect(inner.left, inner.bottom - inner.height * 0.22f, inner.right, inner.bottom),
            CyberVisualEffects.NeonRed,
        )
        drawCenteredLabel(scope, inner, cell, "START", Color.White, cell * 0.19f)
    }

    /** 赛博传送门：001 ↔ 002 成对跃迁。 */
    private fun drawCyberPortal(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        marker: PacMazeMapMarker,
        ctx: PacMazeMapRenderContext,
    ) {
        val center = Offset(rect.center.x, rect.center.y)
        val phase = ctx.animPhase
        val isLeftGate = marker.x <= ctx.world.width / 2
        val visited = when {
            marker.tag == "LINK" -> PacMazePortals.isPortalArmed(ctx.world, marker)
            marker.kind == PacMazeMarkerKind.CHECKPOINT &&
                marker.tag.isNotBlank() &&
                !PacMazePortals.isArmedTag(marker.tag) ->
                marker.tag in ctx.world.visitedCheckpointTags
            else -> false
        }
        val label = when {
            marker.label.isNotBlank() -> marker.label
            isLeftGate -> "001"
            else -> "002"
        }
        val accent = when {
            visited -> Color(0xFF22C55E)
            isLeftGate -> CyberVisualEffects.NeonBlue
            else -> CyberVisualEffects.NeonPink
        }
        val accent2 = when {
            visited -> Color(0xFF86EFAC)
            isLeftGate -> CyberVisualEffects.NeonPink
            else -> CyberVisualEffects.NeonBlue
        }

        // 暗色底 + 网格
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0A1020), Color(0xFF020408)),
                center = center,
                radius = cell * 0.55f,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
        )
        CyberCollectibles.drawFloor(scope, rect, marker.x, marker.y)

        // 外环呼吸光晕
        val pulse = 0.82f + 0.18f * sin(phase * 2.2f)
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = cell * 0.12f
                color = accent.copy(alpha = 0.35f * pulse).toArgb()
                maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(center.x, center.y, cell * 0.38f * pulse, glow)
        }

        // 横向传送环 + 竖直能量束（开口朝上下通道）
        scope.rotate(phase * 42f, center) {
            scope.drawOval(
                color = accent.copy(alpha = 0.55f),
                topLeft = Offset(center.x - cell * 0.34f, center.y - cell * 0.22f),
                size = Size(cell * 0.68f, cell * 0.44f),
                style = Stroke(width = cell * 0.045f),
            )
        }
        scope.rotate(-phase * 58f, center) {
            scope.drawOval(
                color = accent2.copy(alpha = 0.45f),
                topLeft = Offset(center.x - cell * 0.28f, center.y - cell * 0.18f),
                size = Size(cell * 0.56f, cell * 0.36f),
                style = Stroke(width = cell * 0.035f),
            )
        }

        val beamW = cell * 0.08f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.95f),
                    accent2.copy(alpha = 0.85f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(center.x - beamW / 2f, rect.top + cell * 0.08f),
            size = Size(beamW, rect.height * 0.84f),
        )

        repeat(6) { i ->
            val t = phase * 1.6f + i * 1.05f
            val r = cell * (0.12f + 0.06f * sin(t * 2f))
            val px = center.x + cos(t) * r
            val py = center.y + sin(t * 1.3f) * r * 0.65f
            scope.drawCircle(
                color = accent.copy(alpha = 0.35f + 0.25f * sin(t)),
                radius = cell * 0.025f,
                center = Offset(px, py),
            )
        }

        drawPortalChevrons(scope, rect, cell, isLeftGate, accent, phase)

        if (visited) {
            scope.drawCircle(
                color = Color(0xFF22C55E).copy(alpha = 0.35f),
                radius = cell * 0.44f,
                center = center,
            )
            scope.drawCircle(
                color = Color(0xFF22C55E).copy(alpha = 0.85f),
                radius = cell * 0.44f,
                center = center,
                style = Stroke(width = cell * 0.04f),
            )
        }

        // 全息编号
        drawCenteredLabel(
            scope,
            Rect(rect.left, rect.top + cell * 0.04f, rect.right, rect.top + cell * 0.38f),
            cell,
            label,
            Color.White,
            cell * 0.22f,
        )

        // 底部标签
        val tagRect = Rect(rect.left + cell * 0.08f, rect.bottom - cell * 0.28f, rect.right - cell * 0.08f, rect.bottom - cell * 0.06f)
        scope.drawRoundRect(
            color = Color(0xFF001820).copy(alpha = 0.92f),
            topLeft = tagRect.topLeft,
            size = tagRect.size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.04f),
            style = Stroke(width = cell * 0.025f),
        )
        val tag = when {
            visited -> "✓ ${marker.label.ifBlank { marker.tag }}"
            marker.tag == "LINK" -> "↕ LINK"
            marker.label.isNotBlank() -> marker.label
            else -> if (isLeftGate) "↕ 001" else "↕ 002"
        }
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent.toArgb()
                textSize = cell * 0.13f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                isFakeBoldText = true
            }
            drawText(tag, tagRect.center.x, tagRect.center.y + cell * 0.05f, paint)
        }
    }

    private fun drawPortalChevrons(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        isLeftGate: Boolean,
        accent: Color,
        phase: Float,
    ) {
        val cx = rect.center.x
        val offset = cell * 0.04f * sin(phase * 3f)
        val chevronW = cell * 0.1f
        // 001 入口：箭头指向门内；002 出口：箭头指向门外（上下穿出）
        if (isLeftGate) {
            listOf(
                chevronPathVertical(cx, rect.top + cell * 0.14f - offset, chevronW, pointsUp = false),
                chevronPathVertical(cx, rect.bottom - cell * 0.14f + offset, chevronW, pointsUp = true),
            )
        } else {
            listOf(
                chevronPathVertical(cx, rect.top + cell * 0.14f - offset, chevronW, pointsUp = true),
                chevronPathVertical(cx, rect.bottom - cell * 0.14f + offset, chevronW, pointsUp = false),
            )
        }.forEach { path ->
            scope.drawPath(path, color = accent.copy(alpha = 0.8f), style = Stroke(width = cell * 0.035f))
        }
    }

    private fun chevronPathVertical(cx: Float, cy: Float, w: Float, pointsUp: Boolean): Path {
        val h = w * 0.85f
        return Path().apply {
            if (pointsUp) {
                moveTo(cx, cy)
                lineTo(cx - w, cy + h)
                moveTo(cx, cy)
                lineTo(cx + w, cy + h)
            } else {
                moveTo(cx, cy)
                lineTo(cx - w, cy - h)
                moveTo(cx, cy)
                lineTo(cx + w, cy - h)
            }
        }
    }

    private fun drawCenteredLabel(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        text: String,
        color: Color,
        textSize: Float,
        yBias: Float = 0f,
    ) {
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
                isFakeBoldText = true
                setShadowLayer(6f, 0f, 0f, CyberVisualEffects.NeonBlue.copy(alpha = 0.8f).toArgb())
            }
            drawText(text, rect.center.x, rect.center.y + cell * 0.08f + yBias, paint)
        }
    }

    private fun drawHazardStripes(
        scope: DrawScope,
        rect: Rect,
        accent: Color,
        stripeW: Float = 5f,
    ) {
        var x = rect.left
        var i = 0
        while (x < rect.right) {
            scope.drawRect(
                color = if (i % 2 == 0) accent else Color.Black,
                topLeft = Offset(x, rect.top),
                size = Size(stripeW, rect.height),
            )
            x += stripeW
            i++
        }
    }
}
