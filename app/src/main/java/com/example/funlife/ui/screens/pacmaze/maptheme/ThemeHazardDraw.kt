package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeHazardKind
import kotlin.math.sin

/** 各主题通用的机关绘制（颜色取自主题调色板）。 */
internal object ThemeHazardDraw {

    fun draw(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        if (world.hazards.isEmpty()) return
        val cell = ctx.cell
        val p = ctx.config.palette
        val warnColor = p.powerGlow
        val lethalColor = when (ctx.config.id) {
            PacMazeMapThemeId.GARDEN -> Color(0xFFE65100)
            PacMazeMapThemeId.FOOD -> Color(0xFFFF1744)
            PacMazeMapThemeId.CHINESE -> Color(0xFFD32F2F)
            else -> Color(0xFFFF1744)
        }

        world.hazards.forEach { def ->
            val rect = ctx.tileRect(def.x, def.y)
            when (def.kind) {
                PacMazeHazardKind.TURRET -> drawTurret(scope, rect, cell, def.direction, p.frameAccent, ctx.animPhase)
                PacMazeHazardKind.LASER_ROW, PacMazeHazardKind.LASER_COL ->
                    drawEmitter(scope, rect, cell, p.wallEdge, ctx.animPhase)
            }
        }

        world.hazardStates.forEach { runtime ->
            val def = world.hazards.firstOrNull { it.id == runtime.id } ?: return@forEach
            when (def.kind) {
                PacMazeHazardKind.LASER_ROW -> drawBeamH(
                    scope, ctx, def.y, def.rangeStart, def.rangeEnd,
                    runtime.scanPos, runtime.lethal, warnColor, lethalColor, ctx.animPhase,
                )
                PacMazeHazardKind.LASER_COL -> drawBeamV(
                    scope, ctx, def.x, def.rangeStart, def.rangeEnd,
                    runtime.scanPos, runtime.lethal, warnColor, lethalColor, ctx.animPhase,
                )
                PacMazeHazardKind.TURRET -> Unit
            }
        }

        world.enemyBullets.forEach { bullet ->
            val center = ctx.gridToScreen(bullet.x, bullet.y)
            drawBullet(scope, center, cell, bullet.direction, p.pelletGlow)
        }
    }

    private fun drawEmitter(scope: DrawScope, rect: Rect, cell: Float, accent: Color, phase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        val pulse = 0.7f + 0.3f * sin(phase * 3f)
        scope.drawCircle(color = accent.themeAlpha(0.25f * pulse), radius = cell * 0.2f, center = center)
        scope.drawCircle(color = accent.copy(alpha = 0.85f), radius = cell * 0.1f, center = center, style = Stroke(2f))
    }

    private fun drawBeamH(
        scope: DrawScope, ctx: PacMazeMapRenderContext,
        row: Int, xStart: Int, xEnd: Int, scanX: Float,
        lethal: Boolean, warn: Color, lethalColor: Color, phase: Float,
    ) {
        val cell = ctx.cell
        val y = ctx.offsetY + (row + 0.5f) * cell
        val x1 = ctx.offsetX + (xStart + 0.5f) * cell
        val x2 = ctx.offsetX + (xEnd + 0.5f) * cell
        val color = if (lethal) lethalColor else warn
        val alpha = if (lethal) 0.9f else (0.5f + 0.2f * sin(phase * 4f)).coerceIn(0f, 1f)
        scope.drawLine(color.themeAlpha(alpha), Offset(x1, y), Offset(x2, y), strokeWidth = cell * 0.07f)
        val scanCenter = Offset(ctx.offsetX + scanX * cell, y)
        val glowR = cell * (if (lethal) 0.22f else 0.14f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.themeAlpha(alpha * 0.55f), Color.Transparent),
                center = scanCenter,
                radius = glowR,
            ),
            radius = glowR,
            center = scanCenter,
        )
    }

    private fun drawBeamV(
        scope: DrawScope, ctx: PacMazeMapRenderContext,
        col: Int, yStart: Int, yEnd: Int, scanY: Float,
        lethal: Boolean, warn: Color, lethalColor: Color, phase: Float,
    ) {
        val cell = ctx.cell
        val x = ctx.offsetX + (col + 0.5f) * cell
        val y1 = ctx.offsetY + (yStart + 0.5f) * cell
        val y2 = ctx.offsetY + (yEnd + 0.5f) * cell
        val color = if (lethal) lethalColor else warn
        val alpha = if (lethal) 0.9f else (0.5f + 0.2f * sin(phase * 4f)).coerceIn(0f, 1f)
        scope.drawLine(color.themeAlpha(alpha), Offset(x, y1), Offset(x, y2), strokeWidth = cell * 0.07f)
        val scanCenter = Offset(x, ctx.offsetY + scanY * cell)
        val glowR = cell * (if (lethal) 0.22f else 0.14f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.themeAlpha(alpha * 0.55f), Color.Transparent),
                center = scanCenter,
                radius = glowR,
            ),
            radius = glowR,
            center = scanCenter,
        )
    }

    private fun drawTurret(scope: DrawScope, rect: Rect, cell: Float, direction: Direction, accent: Color, phase: Float) {
        val center = Offset(rect.center.x, rect.center.y)
        scope.drawCircle(color = accent.copy(alpha = 0.25f), radius = cell * 0.32f, center = center)
        scope.drawCircle(color = accent, radius = cell * 0.22f, center = center, style = Stroke(2f))
        val barrel = cell * 0.26f
        val tip = when (direction) {
            Direction.RIGHT -> center + Offset(barrel, 0f)
            Direction.LEFT -> center + Offset(-barrel, 0f)
            Direction.UP -> center + Offset(0f, -barrel)
            Direction.DOWN -> center + Offset(0f, barrel)
        }
        scope.drawLine(accent, center, tip, strokeWidth = cell * 0.07f)
        if (sin(phase * 2f) > 0.5f) {
            scope.drawCircle(color = Color.White.copy(alpha = 0.8f), radius = cell * 0.05f, center = tip)
        }
    }

    private fun drawBullet(scope: DrawScope, center: Offset, cell: Float, direction: Direction, color: Color) {
        val trail = when (direction) {
            Direction.RIGHT -> Offset(-cell * 0.15f, 0f)
            Direction.LEFT -> Offset(cell * 0.15f, 0f)
            Direction.UP -> Offset(0f, cell * 0.15f)
            Direction.DOWN -> Offset(0f, -cell * 0.15f)
        }
        scope.drawCircle(color.copy(alpha = 0.45f), cell * 0.08f, center + trail)
        scope.drawCircle(color, cell * 0.05f, center)
    }
}
