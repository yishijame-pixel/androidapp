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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterDraw
import kotlin.math.sin

internal object PacMazeEntityDraw {

    fun drawEntities(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val world = ctx.world
        val cell = ctx.cell
        val animPhase = ctx.animPhase
        val palette = ctx.config.palette

        world.entities.filter { it.role == "ghost" }.forEach { entity ->
            val center = ctx.entityCenter(entity)
            val radius = cell * 0.34f
            val idx = entity.id.removePrefix("ghost_").toIntOrNull() ?: 0
            scope.drawGhost(
                center = center,
                radius = radius,
                baseColor = palette.ghostColors[idx % palette.ghostColors.size],
                mode = entity.ghostMode,
                direction = entity.direction ?: entity.facing,
                animPhase = animPhase + idx,
            )
        }

        world.projectiles.forEach { projectile ->
            val center = ctx.gridToScreen(projectile.x, projectile.y)
            val radius = cell * 0.14f
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.powerCore, palette.powerGlow, Color.Transparent),
                    center = center,
                    radius = radius * 2.2f,
                ),
                radius = radius * 2.2f,
                center = center,
            )
            scope.drawCircle(color = Color.White, radius = radius, center = center)
        }

        val pacEntity = world.entities.firstOrNull { it.role == "pac" }
        val showPlayerHint = world.ghostReleaseTicksLeft > 0
        pacEntity?.let { entity ->
            val center = ctx.entityCenter(entity)
            val radius = cell * 0.44f * ctx.playerDrawScale.coerceIn(0.5f, 1.5f)
            val pulse = 0.45f + 0.35f * sin(animPhase * 2.2f)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.frameAccent.themeAlpha(0.4f * pulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 1.35f,
                ),
                radius = radius * 1.35f,
                center = center,
            )
            val pose = PacMazeCharacterDraw.poseFrom(entity, animPhase, world.powerTicksLeft)
            PacMazeCharacterDraw.draw(
                scope = scope,
                characterId = ctx.playerCharacterId,
                center = center,
                radius = radius,
                pose = pose,
                themeId = ctx.config.id,
            )
            if (showPlayerHint) {
                scope.drawPlayerMarker(
                    center = Offset(center.x, center.y - radius * 1.2f),
                    cell = cell,
                    secondsLeft = (world.ghostReleaseTicksLeft / PacMazeConstants.TICKS_PER_SECOND.toFloat())
                        .coerceAtLeast(0f),
                )
            }
        }
    }

    fun drawMapFrame(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        scope.drawRoundRect(
            color = ctx.config.palette.frameAccent.copy(alpha = 0.35f),
            topLeft = Offset(ctx.offsetX - 3f, ctx.offsetY - 3f),
            size = Size(ctx.mapW + 6f, ctx.mapH + 6f),
            cornerRadius = CornerRadius(6f),
            style = Stroke(width = 2.5f),
        )
    }

    private fun DrawScope.drawGhost(
        center: Offset,
        radius: Float,
        baseColor: Color,
        mode: GhostMode,
        direction: Direction?,
        animPhase: Float,
    ) {
        val bodyColor = when (mode) {
            GhostMode.FRIGHTENED -> Color(0xFF3F51B5)
            GhostMode.EATEN -> Color(0xFFB0BEC5).copy(alpha = 0.65f)
            else -> baseColor
        }
        val wobble = sin(animPhase * 2.5f) * radius * 0.04f
        val top = center.y - radius + wobble
        val left = center.x - radius
        val width = radius * 2f
        val height = radius * 2.1f
        val bodyPath = Path().apply {
            moveTo(left, top + radius)
            arcTo(
                rect = Rect(left, top, left + width, top + radius * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            val waveH = radius * 0.22f
            val seg = width / 3f
            cubicTo(left + seg * 0.5f, top + height, left + seg * 0.5f, top + height - waveH, left + seg, top + height)
            cubicTo(left + seg * 1.5f, top + height + waveH * 0.5f, left + seg * 2f, top + height - waveH, left + width, top + height)
            close()
        }
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(bodyColor.copy(alpha = 0.95f), bodyColor.copy(alpha = 0.75f)),
                startY = top,
                endY = top + height,
            ),
        )
        if (mode != GhostMode.EATEN) {
            val eyeOffsetX = when (direction) {
                Direction.LEFT -> -radius * 0.08f
                Direction.RIGHT -> radius * 0.08f
                else -> 0f
            }
            val eyeY = top + radius * 0.55f
            listOf(-radius * 0.28f, radius * 0.28f).forEach { dx ->
                val eyeCenter = Offset(center.x + dx + eyeOffsetX, eyeY)
                drawCircle(color = Color.White, radius = radius * 0.22f, center = eyeCenter)
                drawCircle(
                    color = if (mode == GhostMode.FRIGHTENED) Color(0xFFFF5252) else Color(0xFF1565C0),
                    radius = radius * 0.11f,
                    center = eyeCenter,
                )
            }
        }
    }

    private fun DrawScope.drawPlayerMarker(center: Offset, cell: Float, secondsLeft: Float) {
        val badgeW = cell * 1.6f
        val badgeH = cell * 0.5f
        val left = center.x - badgeW / 2f
        val top = center.y - badgeH / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(PacMazePalette.accentOrange, PacMazePalette.accentGold)),
            topLeft = Offset(left, top),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(badgeH / 2f),
        )
        drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = cell * 0.34f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            drawText(
                if (secondsLeft > 0.5f) "你 · ${secondsLeft.toInt()}s" else "你",
                center.x,
                center.y + cell * 0.12f,
                paint,
            )
        }
    }
}
