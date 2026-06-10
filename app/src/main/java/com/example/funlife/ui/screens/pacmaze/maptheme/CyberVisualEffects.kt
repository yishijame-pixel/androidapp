package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.TileType
import kotlin.math.sin

internal object CyberVisualEffects {

    val NeonBlue = Color(0xFF00D4FF)
    val NeonBlueDeep = Color(0xFF0066FF)
    val NeonRed = Color(0xFFFF1744)
    val NeonPink = Color(0xFFFF00AA)
    val NeonYellow = Color(0xFFFFEA00)

    fun drawBackground(scope: DrawScope, size: androidx.compose.ui.geometry.Size) {
        scope.drawRect(color = Color(0xFF020208), size = size)
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0A1020).copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.75f,
            ),
            size = size,
        )
    }

    fun drawGlowLine(
        scope: DrawScope,
        start: Offset,
        end: Offset,
        color: Color,
        strokeWidth: Float,
        glowBlur: Float = 14f,
        glowAlpha: Float = 0.75f,
    ) {
        scope.drawContext.canvas.nativeCanvas.apply {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth * 2.2f
                this.color = color.copy(alpha = glowAlpha).toArgb()
                maskFilter = BlurMaskFilter(glowBlur, BlurMaskFilter.Blur.NORMAL)
                strokeCap = Paint.Cap.ROUND
            }
            drawLine(start.x, start.y, end.x, end.y, glowPaint)
            val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                this.color = Color.White.copy(alpha = 0.95f).toArgb()
                strokeCap = Paint.Cap.ROUND
            }
            drawLine(start.x, start.y, end.x, end.y, corePaint)
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth * 0.35f
                this.color = color.toArgb()
                strokeCap = Paint.Cap.ROUND
            }
            drawLine(start.x, start.y, end.x, end.y, innerPaint)
        }
    }

    fun drawCrtOverlay(scope: DrawScope, size: androidx.compose.ui.geometry.Size, animPhase: Float) {
        val h = size.height
        var y = 0f
        while (y < h) {
            scope.drawRect(
                color = Color.Black.copy(alpha = 0.07f),
                topLeft = Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, 1f),
            )
            y += 3f
        }
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f,
            ),
            size = size,
        )
        val flicker = 0.015f * sin(animPhase * 0.7f)
        scope.drawRect(color = Color.White.copy(alpha = 0.02f + flicker), size = size)
    }

    fun drawNeonFrame(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val left = ctx.offsetX - 4f
        val top = ctx.offsetY - 4f
        drawGlowLine(
            scope,
            Offset(left, top),
            Offset(left + ctx.mapW + 8f, top),
            NeonBlue,
            strokeWidth = 2f,
            glowBlur = 10f,
        )
        drawGlowLine(
            scope,
            Offset(left, top + ctx.mapH + 8f),
            Offset(left + ctx.mapW + 8f, top + ctx.mapH + 8f),
            NeonBlue,
            strokeWidth = 2f,
            glowBlur = 10f,
        )
    }
}

internal object CyberMazeWallPass {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        val ox = ctx.offsetX
        val oy = ctx.offsetY
        val breath = 0.75f + 0.25f * sin(ctx.animPhase * 1.4f)
        val stroke = (cell * 0.10f).coerceIn(2.5f, 9f)
        val inset = cell * 0.10f
        val doubleGap = cell * 0.05f

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                if (!isSolidWall(ctx, x, y)) continue
                val left = ox + x * cell
                val top = oy + y * cell
                val right = left + cell
                val bottom = top + cell

                if (!isSolidWall(ctx, x, y - 1)) {
                    drawDoubleEdge(
                        scope, left + inset, right - inset, top + inset,
                        horizontal = true, stroke, doubleGap, breath,
                    )
                }
                if (!isSolidWall(ctx, x, y + 1)) {
                    drawDoubleEdge(
                        scope, left + inset, right - inset, bottom - inset,
                        horizontal = true, stroke, doubleGap, breath,
                    )
                }
                if (!isSolidWall(ctx, x - 1, y)) {
                    drawDoubleEdge(
                        scope, top + inset, bottom - inset, left + inset,
                        horizontal = false, stroke, doubleGap, breath,
                    )
                }
                if (!isSolidWall(ctx, x + 1, y)) {
                    drawDoubleEdge(
                        scope, top + inset, bottom - inset, right - inset,
                        horizontal = false, stroke, doubleGap, breath,
                    )
                }

                if (x % 2 == 0 && y % 2 == 0) {
                    drawConduitNode(scope, (left + right) / 2f, (top + bottom) / 2f, cell, breath)
                }
            }
        }
    }

    private fun drawDoubleEdge(
        scope: DrawScope,
        a: Float,
        b: Float,
        fixed: Float,
        horizontal: Boolean,
        stroke: Float,
        gap: Float,
        breath: Float,
    ) {
        val color = CyberVisualEffects.NeonBlue.copy(alpha = breath)
        val deep = CyberVisualEffects.NeonBlueDeep.copy(alpha = breath)
        if (horizontal) {
            CyberVisualEffects.drawGlowLine(scope, Offset(a, fixed - gap), Offset(b, fixed - gap), color, stroke * 0.85f)
            CyberVisualEffects.drawGlowLine(scope, Offset(a, fixed + gap), Offset(b, fixed + gap), deep, stroke * 0.75f)
        } else {
            CyberVisualEffects.drawGlowLine(scope, Offset(fixed - gap, a), Offset(fixed - gap, b), color, stroke * 0.85f)
            CyberVisualEffects.drawGlowLine(scope, Offset(fixed + gap, a), Offset(fixed + gap, b), deep, stroke * 0.75f)
        }
    }

    private fun drawConduitNode(scope: DrawScope, cx: Float, cy: Float, cell: Float, breath: Float) {
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CyberVisualEffects.NeonBlue.copy(alpha = 0.18f * breath).toArgb()
                strokeWidth = 1.2f
            }
            drawLine(cx - cell * 0.18f, cy, cx + cell * 0.18f, cy, paint)
            drawLine(cx, cy - cell * 0.18f, cx, cy + cell * 0.18f, paint)
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.copy(alpha = 0.35f).toArgb()
            }
            drawCircle(cx, cy, cell * 0.04f, dot)
        }
    }

    private fun isSolidWall(ctx: PacMazeMapRenderContext, x: Int, y: Int): Boolean {
        val world = ctx.world
        if (x !in 0 until world.width || y !in 0 until world.height) return true
        return when (val tile = world.tileAt(x, y)) {
            TileType.WALL, TileType.BRICK_WALL, TileType.WOOD_WALL, TileType.TILE_WALL -> true
            TileType.DOOR -> false
            TileType.DYNAMIC_WALL ->
                PacMazeMapDynamics.isTileBlocking(world, tile, x, y, forGhost = false)
            else -> false
        }
    }
}

internal object CyberCollectibles {

    fun drawFloor(scope: DrawScope, rect: Rect, x: Int = 0, y: Int = 0) {
        scope.drawRect(color = Color(0xFF04060C), topLeft = Offset(rect.left, rect.top), size = rect.size)
        val gridStep = rect.width / 4f
        var gx = rect.left + gridStep
        while (gx < rect.right) {
            scope.drawRect(
                color = Color(0xFF0A1424).copy(alpha = 0.55f),
                topLeft = Offset(gx, rect.top),
                size = androidx.compose.ui.geometry.Size(1f, rect.height),
            )
            gx += gridStep
        }
        var gy = rect.top + gridStep
        while (gy < rect.bottom) {
            scope.drawRect(
                color = Color(0xFF0A1424).copy(alpha = 0.45f),
                topLeft = Offset(rect.left, gy),
                size = androidx.compose.ui.geometry.Size(rect.width, 1f),
            )
            gy += gridStep
        }
        if ((x + y) % 5 == 0) {
            scope.drawContext.canvas.nativeCanvas.apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = CyberVisualEffects.NeonBlue.copy(alpha = 0.08f).toArgb()
                    textSize = rect.width * 0.22f
                    textAlign = Paint.Align.CENTER
                }
                drawText(if ((x + y) % 2 == 0) "0" else "1", rect.center.x, rect.center.y + rect.height * 0.08f, paint)
            }
        }
    }

    fun drawPellet(scope: DrawScope, rect: Rect, cell: Float, x: Int, y: Int) {
        val center = Offset(rect.center.x, rect.center.y)
        val useSquare = (x + y) % 3 == 0
        val r = cell * 0.09f
        if (useSquare) {
            val half = r * 0.85f
            scope.drawContext.canvas.nativeCanvas.apply {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f
                    color = CyberVisualEffects.NeonBlue.copy(alpha = 0.5f).toArgb()
                    maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
                }
                drawRect(center.x - half, center.y - half, center.x + half, center.y + half, glow)
                val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    color = Color.White.copy(alpha = 0.9f).toArgb()
                }
                drawRect(center.x - half * 0.7f, center.y - half * 0.7f, center.x + half * 0.7f, center.y + half * 0.7f, core)
            }
        } else {
            scope.drawContext.canvas.nativeCanvas.apply {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = CyberVisualEffects.NeonBlue.copy(alpha = 0.45f).toArgb()
                    maskFilter = BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL)
                }
                drawCircle(center.x, center.y, r, glow)
                val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                    color = Color.White.copy(alpha = 0.85f).toArgb()
                }
                drawCircle(center.x, center.y, r * 0.75f, ring)
            }
        }
    }

    fun drawPower(scope: DrawScope, rect: Rect, cell: Float, animPhase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        val pulse = 0.88f + 0.12f * sin(animPhase * 2.5f)
        val r = cell * 0.16f * pulse
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = CyberVisualEffects.NeonYellow.copy(alpha = 0.55f).toArgb()
                maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(center.x, center.y, r, glow)
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = CyberVisualEffects.NeonYellow.toArgb()
            }
            drawCircle(center.x, center.y, r * 0.85f, ring)
            val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.White.copy(alpha = 0.35f).toArgb()
            }
            drawCircle(center.x, center.y, r * 0.25f, inner)
        }
    }
}
