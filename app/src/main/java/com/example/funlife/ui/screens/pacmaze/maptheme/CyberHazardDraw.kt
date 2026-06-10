package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeHazardKind
import kotlin.math.sin

internal object CyberHazardDraw {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        val ox = ctx.offsetX
        val oy = ctx.offsetY

        world.hazards.forEach { def ->
            val rect = Rect(
                ox + def.x * cell,
                oy + def.y * cell,
                ox + (def.x + 1) * cell,
                oy + (def.y + 1) * cell,
            )
            when (def.kind) {
                PacMazeHazardKind.TURRET -> drawTurretBase(scope, rect, cell, def.direction, ctx.animPhase)
                PacMazeHazardKind.LASER_ROW, PacMazeHazardKind.LASER_COL ->
                    drawLaserEmitter(scope, rect, cell, def.kind, ctx.animPhase)
            }
        }

        world.hazardStates.forEach { runtime ->
            val def = world.hazards.firstOrNull { it.id == runtime.id } ?: return@forEach
            when (def.kind) {
                PacMazeHazardKind.LASER_ROW -> drawLaserBeamH(
                    scope, ctx, def.y, def.rangeStart, def.rangeEnd,
                    runtime.scanPos, runtime.lethal, ctx.animPhase,
                )
                PacMazeHazardKind.LASER_COL -> drawLaserBeamV(
                    scope, ctx, def.x, def.rangeStart, def.rangeEnd,
                    runtime.scanPos, runtime.lethal, ctx.animPhase,
                )
                PacMazeHazardKind.TURRET -> Unit
            }
        }

        world.enemyBullets.forEach { bullet ->
            val cx = ox + (bullet.x + 0.5f) * cell
            val cy = oy + (bullet.y + 0.5f) * cell
            drawEnemyBullet(scope, Offset(cx, cy), cell, bullet.direction)
        }
    }

    private fun drawLaserEmitter(scope: DrawScope, rect: Rect, cell: Float, kind: PacMazeHazardKind, phase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawRect(
            color = Color(0xFF1A0508),
            topLeft = Offset(rect.left + cell * 0.15f, rect.top + cell * 0.15f),
            size = Size(cell * 0.7f, cell * 0.7f),
        )
        scope.drawRect(
            color = CyberVisualEffects.NeonRed.copy(alpha = 0.85f),
            topLeft = Offset(rect.left + cell * 0.15f, rect.top + cell * 0.15f),
            size = Size(cell * 0.7f, cell * 0.7f),
            style = Stroke(width = 2f),
        )
        val pulse = 0.7f + 0.3f * sin(phase * 3f)
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CyberVisualEffects.NeonRed.copy(alpha = 0.5f * pulse).toArgb()
                maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(center.x, center.y, cell * 0.12f, paint)
        }
    }

    private fun drawLaserBeamH(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        row: Int,
        xStart: Int,
        xEnd: Int,
        scanX: Float,
        lethal: Boolean,
        phase: Float,
    ) {
        val cell = ctx.cell
        val y = ctx.offsetY + (row + 0.5f) * cell
        val x1 = ctx.offsetX + (xStart + 0.5f) * cell
        val x2 = ctx.offsetX + (xEnd + 0.5f) * cell
        val beamColor = if (lethal) CyberVisualEffects.NeonRed else Color(0xFFFF9100)
        val alpha = if (lethal) 0.95f else 0.45f + 0.2f * sin(phase * 4f)
        CyberVisualEffects.drawGlowLine(
            scope, Offset(x1, y), Offset(x2, y),
            beamColor.copy(alpha = alpha), cell * 0.07f, glowBlur = if (lethal) 18f else 8f,
        )
        val scanScreenX = ctx.offsetX + scanX * cell
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = beamColor.copy(alpha = if (lethal) 0.75f else 0.35f).toArgb()
                maskFilter = BlurMaskFilter(if (lethal) 22f else 10f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(scanScreenX, y, cell * 0.14f, glow)
        }
    }

    private fun drawLaserBeamV(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        col: Int,
        yStart: Int,
        yEnd: Int,
        scanY: Float,
        lethal: Boolean,
        phase: Float,
    ) {
        val cell = ctx.cell
        val x = ctx.offsetX + (col + 0.5f) * cell
        val y1 = ctx.offsetY + (yStart + 0.5f) * cell
        val y2 = ctx.offsetY + (yEnd + 0.5f) * cell
        val beamColor = if (lethal) CyberVisualEffects.NeonRed else Color(0xFFFF9100)
        val alpha = if (lethal) 0.95f else 0.45f + 0.2f * sin(phase * 4f)
        CyberVisualEffects.drawGlowLine(
            scope, Offset(x, y1), Offset(x, y2),
            beamColor.copy(alpha = alpha), cell * 0.07f, glowBlur = if (lethal) 18f else 8f,
        )
        val scanScreenY = ctx.offsetY + scanY * cell
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = beamColor.copy(alpha = if (lethal) 0.75f else 0.35f).toArgb()
                maskFilter = BlurMaskFilter(if (lethal) 22f else 10f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(x, scanScreenY, cell * 0.14f, glow)
        }
    }

    private fun drawTurretBase(scope: DrawScope, rect: Rect, cell: Float, direction: Direction, phase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawRect(
            color = Color(0xFF101820),
            topLeft = Offset(rect.left + cell * 0.12f, rect.top + cell * 0.12f),
            size = Size(cell * 0.76f, cell * 0.76f),
        )
        scope.drawRect(
            color = Color(0xFFFF6D00),
            topLeft = Offset(rect.left + cell * 0.12f, rect.top + cell * 0.12f),
            size = Size(cell * 0.76f, cell * 0.76f),
            style = Stroke(width = 2f),
        )
        val barrel = cell * 0.28f
        val tip = when (direction) {
            Direction.RIGHT -> center + Offset(barrel, 0f)
            Direction.LEFT -> center + Offset(-barrel, 0f)
            Direction.UP -> center + Offset(0f, -barrel)
            Direction.DOWN -> center + Offset(0f, barrel)
        }
        CyberVisualEffects.drawGlowLine(scope, center, tip, Color(0xFFFF9100), cell * 0.08f, glowBlur = 8f)
        if (sin(phase * 2f) > 0.6f) {
            scope.drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.7f),
                radius = cell * 0.06f,
                center = tip,
            )
        }
    }

    private fun drawEnemyBullet(scope: DrawScope, center: Offset, cell: Float, direction: Direction) {
        val trail = when (direction) {
            Direction.RIGHT -> Offset(-cell * 0.18f, 0f)
            Direction.LEFT -> Offset(cell * 0.18f, 0f)
            Direction.UP -> Offset(0f, cell * 0.18f)
            Direction.DOWN -> Offset(0f, -cell * 0.18f)
        }
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = CyberVisualEffects.NeonRed.copy(alpha = 0.55f).toArgb()
                maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(center.x + trail.x, center.y + trail.y, cell * 0.08f, glow)
            val core = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
            }
            drawCircle(center.x, center.y, cell * 0.06f, core)
        }
        val path = Path().apply {
            moveTo(center.x, center.y - cell * 0.07f)
            lineTo(center.x + cell * 0.07f, center.y)
            lineTo(center.x, center.y + cell * 0.07f)
            lineTo(center.x - cell * 0.07f, center.y)
            close()
        }
        scope.drawPath(path, color = CyberVisualEffects.NeonRed, style = Fill)
    }
}
