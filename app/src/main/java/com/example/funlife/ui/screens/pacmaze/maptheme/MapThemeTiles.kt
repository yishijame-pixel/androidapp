package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
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
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.TileType
import kotlin.math.sin

internal object MapThemeTiles {

    fun drawBackground(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val p = ctx.config.palette
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(p.backgroundTop, p.backgroundBottom),
                center = Offset(ctx.canvasSize.width / 2f, ctx.canvasSize.height / 2f),
                radius = maxOf(ctx.canvasSize.width, ctx.canvasSize.height) * 0.65f,
            ),
            size = ctx.canvasSize,
        )
    }

    fun drawPath(scope: DrawScope, rect: Rect, palette: PacMazeThemePalette) {
        scope.drawRect(color = palette.pathFill, topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height))
        scope.drawRect(
            color = palette.pathGrid.copy(alpha = 0.35f),
            topLeft = Offset(rect.left + rect.width * 0.5f - 0.5f, rect.top),
            size = Size(1f, rect.height),
        )
        scope.drawRect(
            color = palette.pathGrid.copy(alpha = 0.35f),
            topLeft = Offset(rect.left, rect.top + rect.height * 0.5f - 0.5f),
            size = Size(rect.width, 1f),
        )
    }

    fun drawClassicWall(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette) {
        val pad = cell * 0.06f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(palette.wallGlow.copy(alpha = 0.5f), palette.wallFill),
                start = Offset(inner.left, inner.top),
                end = Offset(inner.right, inner.bottom),
            ),
            topLeft = Offset(inner.left, inner.top),
            size = Size(inner.width, inner.height),
            cornerRadius = CornerRadius(cell * 0.14f),
        )
        scope.drawRoundRect(
            color = palette.wallEdge.copy(alpha = 0.7f),
            topLeft = Offset(inner.left, inner.top),
            size = Size(inner.width, inner.height),
            cornerRadius = CornerRadius(cell * 0.14f),
            style = Stroke(width = 1.5f),
        )
    }

    fun drawCyberWall(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette, animPhase: Float) {
        val pad = cell * 0.08f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        val breath = 0.65f + 0.35f * sin(animPhase * 1.6f)
        scope.drawGlowRoundRect(
            rect = inner,
            cornerRadius = cell * 0.1f,
            fillColor = palette.wallFill.copy(alpha = 0.92f),
            strokeColor = palette.wallEdge.copy(alpha = breath),
            glowColor = palette.wallGlow,
            strokeWidth = 2f,
            glowBlur = 8f + cell * 0.08f,
            glowAlpha = 0.45f * breath,
        )
        val pipeY = inner.center.y
        scope.drawLine(
            color = palette.wallEdge.copy(alpha = 0.35f * breath),
            start = Offset(inner.left + cell * 0.12f, pipeY),
            end = Offset(inner.right - cell * 0.12f, pipeY),
            strokeWidth = 1.2f,
        )
    }

    fun drawGardenWall(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette) {
        val pad = cell * 0.04f
        val inner = Rect(rect.left + pad, rect.top + pad, rect.right - pad, rect.bottom - pad)
        scope.drawRoundRect(
            brush = Brush.verticalGradient(listOf(palette.wallGlow, palette.wallFill)),
            topLeft = Offset(inner.left, inner.top),
            size = Size(inner.width, inner.height),
            cornerRadius = CornerRadius(cell * 0.22f),
        )
        scope.drawRoundRect(
            color = palette.wallEdge.copy(alpha = 0.55f),
            topLeft = Offset(inner.left, inner.top),
            size = Size(inner.width, inner.height),
            cornerRadius = CornerRadius(cell * 0.22f),
            style = Stroke(width = 1.2f),
        )
    }

    fun drawPelletDot(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette) {
        val r = cell * 0.07f
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawCircle(color = palette.pelletGlow.copy(alpha = 0.35f), radius = r * 1.8f, center = center)
        scope.drawCircle(color = palette.pelletPrimary, radius = r, center = center)
    }

    fun drawPelletGlyph(scope: DrawScope, rect: Rect, cell: Float, config: PacMazeThemeConfig, x: Int, y: Int) {
        val glyph = config.pelletGlyphs[(x + y * 3) % config.pelletGlyphs.size]
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawGlowCircle(
            center = center,
            radius = cell * 0.09f,
            coreColor = config.palette.pelletPrimary,
            glowColor = config.palette.pelletGlow,
            glowBlur = 8f,
        )
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = cell * 0.22f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }
            drawText(glyph, center.x, center.y + cell * 0.08f, paint)
        }
    }

    fun drawPower(scope: DrawScope, rect: Rect, cell: Float, config: PacMazeThemeConfig, animPhase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        val pulse = 0.85f + 0.15f * sin(animPhase * 2f)
        val coreR = cell * 0.13f * pulse
        scope.drawGlowCircle(
            center = center,
            radius = coreR * 1.4f,
            coreColor = config.palette.powerCore,
            glowColor = config.palette.powerGlow,
            glowBlur = 14f,
        )
        if (config.id == PacMazeMapThemeId.CYBERPUNK) {
            val diamond = Path().apply {
                moveTo(center.x, center.y - coreR)
                lineTo(center.x + coreR, center.y)
                lineTo(center.x, center.y + coreR)
                lineTo(center.x - coreR, center.y)
                close()
            }
            scope.drawPath(diamond, color = Color.White.copy(alpha = 0.9f))
        } else {
            scope.drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.White.toArgb()
                    textSize = cell * 0.28f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                drawText(config.powerLabel, center.x, center.y + cell * 0.1f, paint)
            }
        }
    }

    fun drawTunnel(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette) {
        scope.drawRect(color = palette.tunnelFill, topLeft = Offset(rect.left, rect.top), size = Size(rect.width, rect.height))
        scope.drawRect(
            color = palette.tunnelAccent.copy(alpha = 0.25f),
            topLeft = Offset(rect.left + cell * 0.2f, rect.top + cell * 0.35f),
            size = Size(rect.width * 0.6f, rect.height * 0.3f),
        )
    }

    fun drawPortal(scope: DrawScope, rect: Rect, cell: Float, palette: PacMazeThemePalette, animPhase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        val r = cell * 0.32f
        scope.drawCircle(color = palette.tunnelFill, radius = r, center = center)
        scope.rotate(animPhase * 28f, center) {
            scope.drawCircle(
                color = palette.tunnelAccent.copy(alpha = 0.55f),
                radius = r * 0.75f,
                center = center,
                style = Stroke(width = 2f),
            )
            scope.drawArc(
                color = palette.wallGlow.copy(alpha = 0.7f),
                startAngle = 0f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = Offset(center.x - r * 0.55f, center.y - r * 0.55f),
                size = Size(r * 1.1f, r * 1.1f),
                style = Stroke(width = 2.5f),
            )
        }
    }

    fun drawEnergyGate(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        palette: PacMazeThemePalette,
        open: Boolean,
        animPhase: Float,
    ) {
        drawPath(scope, rect, palette)
        val center = Offset(rect.center.x, rect.center.y)
        val label = if (open) "○" else "╳"
        val color = if (open) palette.tunnelAccent else Color(0xFFFF5252)
        val pulse = if (open) 0.7f + 0.3f * sin(animPhase * 3f) else 1f
        scope.drawGlowCircle(center, cell * 0.14f * pulse, color.copy(alpha = 0.85f), color, glowBlur = if (open) 10f else 4f)
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                textSize = cell * 0.32f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            drawText(label, center.x, center.y + cell * 0.11f, paint)
        }
    }

    fun drawDynamicWall(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        palette: PacMazeThemePalette,
        ctx: PacMazeMapRenderContext,
        x: Int,
        y: Int,
    ) {
        val closed = PacMazeMapDynamics.isTileBlocking(ctx.world, TileType.DYNAMIC_WALL, x, y, forGhost = false)
        if (closed) {
            drawCyberWall(scope, rect, cell, palette, ctx.animPhase)
        } else {
            drawPath(scope, rect, palette)
            scope.drawCircle(
                color = palette.wallGlow.copy(alpha = 0.35f),
                radius = cell * 0.06f,
                center = Offset(rect.center.x, rect.center.y),
            )
        }
    }
}
