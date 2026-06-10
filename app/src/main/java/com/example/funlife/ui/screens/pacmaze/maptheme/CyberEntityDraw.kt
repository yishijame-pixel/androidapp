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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterDraw
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import kotlin.math.cos
import kotlin.math.sin

internal object CyberEntityDraw {

    fun drawEntities(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val cell = ctx.cell
        val offsetX = ctx.offsetX
        val offsetY = ctx.offsetY
        val ghostColors = ctx.config.palette.ghostColors

        ctx.world.entities.filter { it.role == "ghost" }.forEachIndexed { index, entity ->
            val (ax, ay) = ctx.renderAnchor(entity)
            val cx = offsetX + (ax + 0.5f) * cell
            val cy = offsetY + (ay + 0.5f) * cell
            val tint = ghostColors.getOrElse(index % ghostColors.size) { CyberVisualEffects.NeonPink }
            drawCyberGhost(
                scope,
                Offset(cx, cy),
                cell * 0.38f,
                entity.ghostMode,
                entity.facing,
                tint,
                ctx.animPhase + index * 0.7f,
            )
        }

        ctx.world.projectiles.forEach { p ->
            val cx = offsetX + (p.x + 0.5f) * cell
            val cy = offsetY + (p.y + 0.5f) * cell
            scope.drawCircle(
                color = CyberVisualEffects.NeonYellow,
                radius = cell * 0.1f,
                center = Offset(cx, cy),
            )
        }

        val pac = ctx.world.entities.firstOrNull { it.role == "pac" } ?: return
        val (ax, ay) = ctx.renderAnchor(pac)
        val cx = offsetX + (ax + 0.5f) * cell
        val cy = offsetY + (ay + 0.5f) * cell

        ctx.playerTrail.forEachIndexed { index, point ->
            val t = (index + 1).toFloat() / (ctx.playerTrail.size.coerceAtLeast(1)).toFloat()
            val alpha = t * 0.5f
            val trailSize = cell * 0.24f * t
            scope.drawRect(
                color = CyberVisualEffects.NeonRed.copy(alpha = alpha),
                topLeft = Offset(point.x - trailSize / 2f, point.y - trailSize / 2f),
                size = Size(trailSize, trailSize),
            )
        }

        val radius = cell * 0.44f * ctx.playerDrawScale.coerceIn(0.5f, 1.5f)
        val pose = PacMazeCharacterDraw.poseFrom(pac, ctx.animPhase, ctx.world.powerTicksLeft)
        PacMazeCharacterDraw.draw(
            scope = scope,
            characterId = ctx.playerCharacterId,
            center = Offset(cx, cy),
            radius = radius,
            pose = pose,
            themeId = ctx.config.id,
        )

        if (ctx.world.ghostReleaseTicksLeft > 0) {
            drawStartBadge(
                scope,
                Offset(cx, cy - cell * 0.65f),
                cell,
                (ctx.world.ghostReleaseTicksLeft / PacMazeConstants.TICKS_PER_SECOND.toFloat()).coerceAtLeast(0f),
            )
        }
    }

    private fun drawCyberGhost(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        mode: GhostMode,
        facing: Direction,
        tint: Color,
        animPhase: Float,
    ) {
        val ghostColor = when (mode) {
            GhostMode.FRIGHTENED -> Color(0xFF536DFE)
            GhostMode.EATEN -> Color.Gray.copy(alpha = 0.45f)
            else -> tint
        }
        val spin = sin(animPhase * 1.6f) * 4f
        val wobble = sin(animPhase * 2.4f) * radius * 0.05f
        val c = center + Offset(0f, wobble)

        for (layer in 3 downTo 1) {
            val scale = 1f + layer * 0.22f
            val alpha = 0.08f * layer
            drawDiamond(
                scope, c, radius * scale, ghostColor.copy(alpha = alpha), fill = true, rotation = spin,
            )
        }

        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ghostColor.copy(alpha = 0.42f).toArgb()
                maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            }
            val path = android.graphics.Path()
            buildDiamondPoints(c, radius * 1.15f, spin, path)
            drawPath(path, glow)
        }

        drawDiamond(scope, c, radius, ghostColor, fill = true, rotation = spin)
        drawDiamond(scope, c, radius * 0.92f, Color.White.copy(alpha = 0.75f), fill = false, stroke = 2f, rotation = spin)
        drawDiamond(scope, c, radius * 0.48f, Color.White.copy(alpha = 0.55f), fill = true, rotation = spin)
        drawDiamond(scope, c, radius * 0.22f, ghostColor.copy(alpha = 0.9f), fill = true, rotation = spin)

        if (mode != GhostMode.EATEN) {
            drawGhostEyes(scope, c, radius, facing)
        }
    }

    private fun drawGhostEyes(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        facing: Direction,
    ) {
        val eyeOffset = when (facing) {
            Direction.LEFT -> Offset(-radius * 0.22f, -radius * 0.08f) to Offset(-radius * 0.22f, radius * 0.08f)
            Direction.RIGHT -> Offset(radius * 0.22f, -radius * 0.08f) to Offset(radius * 0.22f, radius * 0.08f)
            Direction.UP -> Offset(-radius * 0.12f, -radius * 0.22f) to Offset(radius * 0.12f, -radius * 0.22f)
            Direction.DOWN -> Offset(-radius * 0.12f, radius * 0.22f) to Offset(radius * 0.12f, radius * 0.22f)
        }
        val r = radius * 0.09f
        scope.drawCircle(color = Color.White, radius = r, center = center + eyeOffset.first)
        scope.drawCircle(color = Color.White, radius = r, center = center + eyeOffset.second)
        scope.drawCircle(color = CyberVisualEffects.NeonBlue, radius = r * 0.55f, center = center + eyeOffset.first)
        scope.drawCircle(color = CyberVisualEffects.NeonBlue, radius = r * 0.55f, center = center + eyeOffset.second)
    }

    private fun drawDiamond(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        color: Color,
        fill: Boolean,
        rotation: Float = 0f,
        stroke: Float = 1.5f,
    ) {
        scope.rotate(rotation, center) {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            if (fill) {
                drawPath(path, color = color, style = Fill)
            } else {
                drawPath(path, color = color, style = Stroke(width = stroke))
            }
        }
    }

    private fun buildDiamondPoints(center: Offset, radius: Float, rotationDeg: Float, path: android.graphics.Path) {
        val rad = Math.toRadians(rotationDeg.toDouble())
        val cosR = cos(rad).toFloat()
        val sinR = sin(rad).toFloat()
        fun rot(x: Float, y: Float): Pair<Float, Float> {
            val dx = x - center.x
            val dy = y - center.y
            return center.x + dx * cosR - dy * sinR to center.y + dx * sinR + dy * cosR
        }
        val pts = listOf(
            rot(center.x, center.y - radius),
            rot(center.x + radius, center.y),
            rot(center.x, center.y + radius),
            rot(center.x - radius, center.y),
        )
        path.moveTo(pts[0].first, pts[0].second)
        pts.drop(1).forEach { path.lineTo(it.first, it.second) }
        path.close()
    }

    private fun drawStartBadge(scope: DrawScope, center: Offset, cell: Float, secondsLeft: Float) {
        val w = cell * 1.6f
        val h = cell * 0.58f
        val left = center.x - w / 2f
        val top = center.y - h / 2f
        scope.drawRect(color = Color(0xFF1A1A1A), topLeft = Offset(left, top), size = Size(w, h))
        scope.drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(w, h),
            style = Stroke(width = 1.5f),
        )
        drawHazardStripes(scope, Rect(left, top, left + w, top + h * 0.22f), Color(0xFFFF1744))
        drawHazardStripes(scope, Rect(left, top + h * 0.78f, left + w, top + h), Color(0xFFFF1744))
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = cell * 0.22f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val label = if (secondsLeft > 0.5f) "START · ${secondsLeft.toInt()}s" else "START"
            drawText(label, center.x, center.y + cell * 0.08f, paint)
        }
    }

    private fun drawHazardStripes(scope: DrawScope, rect: Rect, accent: Color) {
        var x = rect.left
        while (x < rect.right) {
            val stripeIndex = ((x - rect.left) / 6f).toInt()
            scope.drawRect(
                color = if (stripeIndex % 2 == 0) accent else Color.Black,
                topLeft = Offset(x, rect.top),
                size = Size(6f, rect.height),
            )
            x += 6f
        }
    }
}
