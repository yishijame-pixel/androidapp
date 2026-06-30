package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostBehaviorArchetype
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.GhostSpecialty
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.isPlayerPac
import com.example.funlife.ui.screens.pacmaze.components.PacMazeEntityComfortScale
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazePlayerCosmeticDraw
import com.example.funlife.ui.screens.pacmaze.pacMazeGhostAccent
import kotlin.math.cos
import kotlin.math.sin

internal object CyberEntityDraw {

    fun drawEntities(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        PacMazeEntityDraw.withEntityMapClip(scope, ctx) {
            drawEntitiesUnclipped(this, ctx)
        }
    }

    private fun drawEntitiesUnclipped(scope: DrawScope, ctx: PacMazeMapRenderContext) {
        val cell = ctx.entityCell
        val ghostColors = ctx.config.palette.ghostColors
        ctx.world.entities.filter { it.role == "ghost" }.forEachIndexed { index, entity ->
            val center = ctx.entityCenter(entity)
            val tint = ghostColors.getOrElse(index % ghostColors.size) { CyberVisualEffects.NeonPink }
            val radiusMul = entity.ghostKind.speedMul.coerceIn(0.85f, 1.12f)
            drawCyberGhost(
                scope = scope,
                center = center,
                radius = PacMazeEntityComfortScale.resolveGhostRadius(
                    entityCell = cell,
                    boost = ctx.entityDrawBoost,
                    minRadiusPx = ctx.minGhostRadiusPx,
                    kindMul = radiusMul,
                ),
                mode = entity.ghostMode,
                facing = entity.facing,
                tint = tint,
                animPhase = ctx.animPhase + index * 0.7f,
                kind = entity.ghostKind,
                specialty = entity.ghostSpecialty,
                burstActive = entity.opportunistBurstTicksLeft > 0,
                hitStunTicksLeft = entity.hitStunTicksLeft,
                powerTicksLeft = ctx.world.powerTicksLeft,
                moving = entity.velX != 0f || entity.velY != 0f,
            )
        }
        ctx.world.projectiles.forEach { p ->
            val center = ctx.projectileCenter(p)
            scope.drawCircle(
                color = CyberVisualEffects.NeonYellow,
                radius = cell * 0.1f * ctx.entityDrawBoost,
                center = center,
            )
        }
        PacMazeEntityDraw.drawPlayerPacs(scope, ctx)
    }

    private fun drawCyberGhost(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        mode: GhostMode,
        facing: Direction,
        tint: Color,
        animPhase: Float,
        kind: GhostKind,
        specialty: GhostSpecialty,
        burstActive: Boolean,
        hitStunTicksLeft: Int = 0,
        powerTicksLeft: Int = 0,
        moving: Boolean = true,
    ) {
        val kindAccent = pacMazeGhostAccent(kind)
        val ghostColor = when (mode) {
            GhostMode.FRIGHTENED -> Color(0xFF536DFE)
            GhostMode.EATEN -> Color.Gray.copy(alpha = 0.45f)
            else -> PacMazeGhostVisualEffects.resolveBodyColor(
                baseTint = tint,
                mode = mode,
                powerTicksLeft = powerTicksLeft,
                hitStunTicksLeft = hitStunTicksLeft,
                ghostsFrozen = false,
                kindAccent = kindAccent,
            )
        }
        val stunActive = hitStunTicksLeft > 0 && mode != GhostMode.EATEN
        val baseRotation = when (facing) {
            Direction.RIGHT -> 0f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.UP -> 270f
        }
        val wobbleScale = if (moving && hitStunTicksLeft <= 0) 0.01f else 0f
        val wobble = sin(animPhase * (if (kind.behaviorArchetype == GhostBehaviorArchetype.OPPORTUNIST) 1.8f else 1.3f)) * radius * wobbleScale
        val spinJitter = if (moving) sin(animPhase * (if (kind.behaviorArchetype == GhostBehaviorArchetype.PREDICTOR) 1.2f else 0.9f)) * 2f else 0f
        val spin = baseRotation + spinJitter
        val c = center + Offset(0f, wobble)
        if (stunActive) {
            PacMazeGhostVisualEffects.drawHitStunAccent(
                scope, c, radius, kindAccent, hitStunTicksLeft, animPhase,
            )
        }
        for (layer in 3 downTo 1) {
            val scale = 1f + layer * 0.18f
            val alpha = 0.07f * layer
            PacMazeGhostCyberDraw.drawBody(
                scope, c, radius * scale, kind, ghostColor.copy(alpha = alpha), spin,
            )
        }
        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ghostColor.copy(alpha = 0.42f).toArgb()
                maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            }
            val path = android.graphics.Path()
            buildDiamondPoints(c, radius * 1.12f, spin, path)
            drawPath(path, glow)
        }
        PacMazeGhostCyberDraw.drawBody(scope, c, radius, kind, ghostColor, spin)
        val outlineStroke = if (stunActive) 3.5f else 2f
        val outlinePath = PacMazeGhostCyberDraw.outlinePath(c, radius * 0.94f, kind, spin)
        scope.drawPath(outlinePath, Color.White.copy(alpha = if (stunActive) 0.92f else 0.75f), style = Stroke(width = outlineStroke))
        if (stunActive) {
            scope.drawPath(
                PacMazeGhostCyberDraw.outlinePath(c, radius * 1.04f, kind, spin),
                kindAccent.copy(alpha = 0.95f),
                style = Stroke(width = radius * 0.09f),
            )
        }
        PacMazeGhostCyberDraw.drawBody(
            scope, c, radius * 0.42f, kind, Color.White.copy(alpha = 0.45f), spin,
        )
        if (mode != GhostMode.EATEN) {
            drawGhostEyes(scope, c, radius, facing)
            PacMazeGhostShapeDraw.drawSpecialtyBadge(scope, c, radius, specialty, animPhase)
            if (burstActive) {
                scope.drawCircle(
                    color = CyberVisualEffects.NeonYellow.copy(alpha = 0.35f),
                    radius = radius * 1.2f,
                    center = c,
                )
            }
            PacMazeGhostVisualEffects.drawStunSparks(scope, c, radius, hitStunTicksLeft, animPhase)
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
            val label = if (secondsLeft > 0.5f) "START ${secondsLeft.toInt()}s" else "START"
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
