package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.social.game.engine.pacmaze.GhostMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class FrozenGhostShape {
    DIAMOND,
    ROUND_GHOST,
}

internal object PacMazeGhostVisualEffects {

    fun resolveBodyColor(
        baseTint: Color,
        mode: GhostMode,
        powerTicksLeft: Int,
        hitStunTicksLeft: Int,
        ghostsFrozen: Boolean,
        kindAccent: Color = baseTint,
    ): Color {
        if (ghostsFrozen && mode != GhostMode.EATEN) {
            val chilled = lerpColor(baseTint, Color(0xFF78909C), 0.55f)
            return lerpColor(chilled, Color(0xFFB3E5FC), 0.38f)
        }
        if (hitStunTicksLeft > 0 && mode != GhostMode.EATEN) {
            val ratio = hitStunTicksLeft.toFloat() / PacMazeConstants.GHOST_HIT_STUN_TICKS
            // 保留底色 + 性格强调色，仅叠少量受击暖光，避免在浅色地面上「隐身」
            val kindTint = lerpColor(baseTint, kindAccent, 0.62f)
            val warmed = lerpColor(kindTint, Color(0xFFFFF176), (ratio * 0.22f).coerceIn(0f, 0.22f))
            return warmed.copy(alpha = 1f)
        }
        return when (mode) {
            GhostMode.FRIGHTENED -> {
                val ratio = (powerTicksLeft.toFloat() / PacMazeConstants.POWER_DURATION_TICKS).coerceIn(0f, 1f)
                lerpColor(baseTint, Color(0xFF536DFE), ratio)
            }
            GhostMode.EATEN -> Color.Gray.copy(alpha = 0.45f)
            else -> baseTint
        }
    }

    fun drawIceEncasement(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        animPhase: Float,
        frostRatio: Float,
        shape: FrozenGhostShape = FrozenGhostShape.ROUND_GHOST,
        spin: Float = 0f,
    ) {
        if (frostRatio <= 0f) return
        val t = frostRatio.coerceIn(0f, 1f)
        val pulse = 0.94f + 0.06f * sin(animPhase * 1.4f)

        drawColdAura(scope, center, radius, t, pulse)

        val shell = buildIceShellPath(center, radius * pulse, shape, spin)
        drawIceShellLayers(scope, shell, center, radius, t)
        drawIceCracks(scope, center, radius * pulse, t, animPhase, shape, spin)
        drawIceSpecular(scope, center, radius * pulse, t, animPhase, shape, spin)
        drawIceRim(scope, shell, t)
        drawBaseFrost(scope, center, radius, t)
        drawSurfaceCrystals(scope, center, radius * pulse, t, animPhase, shape, spin)
    }

    fun drawFrozenFace(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        frostRatio: Float,
        shape: FrozenGhostShape = FrozenGhostShape.ROUND_GHOST,
    ) {
        if (frostRatio <= 0f) return
        val t = frostRatio.coerceIn(0f, 1f)
        val eyeY = when (shape) {
            FrozenGhostShape.DIAMOND -> center.y - radius * 0.05f
            FrozenGhostShape.ROUND_GHOST -> center.y - radius * 0.08f
        }
        val eyeSpread = radius * 0.28f
        val eyeR = radius * 0.17f

        listOf(-eyeSpread, eyeSpread).forEach { dx ->
            val eye = Offset(center.x + dx, eyeY)
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE1F5FE).copy(alpha = 0.92f * t),
                        Color(0xFF81D4FA).copy(alpha = 0.65f * t),
                    ),
                    center = eye,
                    radius = eyeR,
                ),
                radius = eyeR,
                center = eye,
            )
            scope.drawCircle(
                color = Color(0xFF0277BD).copy(alpha = 0.55f * t),
                radius = eyeR,
                center = eye,
                style = Stroke(eyeR * 0.12f),
            )
            val cross = eyeR * 0.55f
            scope.drawLine(
                Color(0xFF01579B).copy(alpha = 0.75f * t),
                eye - Offset(cross, cross),
                eye + Offset(cross, cross),
                strokeWidth = eyeR * 0.16f,
                cap = StrokeCap.Round,
            )
            scope.drawLine(
                Color(0xFF01579B).copy(alpha = 0.75f * t),
                eye + Offset(-cross, cross),
                eye + Offset(cross, -cross),
                strokeWidth = eyeR * 0.16f,
                cap = StrokeCap.Round,
            )
        }

        scope.drawRoundRect(
            color = Color.White.copy(alpha = 0.35f * t),
            topLeft = Offset(center.x - radius * 0.55f, eyeY + radius * 0.18f),
            size = Size(radius * 1.1f, radius * 0.14f),
            cornerRadius = CornerRadius(radius * 0.07f),
        )
    }

    fun drawScreenFrostOverlay(
        scope: DrawScope,
        mapLeft: Float,
        mapTop: Float,
        mapW: Float,
        mapH: Float,
        cell: Float,
        frostRatio: Float,
        animPhase: Float,
        coldCenters: List<Offset>,
    ) {
        if (frostRatio <= 0f) return
        val t = frostRatio.coerceIn(0f, 1f)

        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFBBDEFB).copy(alpha = 0.08f * t),
                    Color(0xFF64B5F6).copy(alpha = 0.18f * t),
                ),
                center = Offset(mapLeft + mapW / 2f, mapTop + mapH / 2f),
                radius = maxOf(mapW, mapH) * 0.72f,
            ),
            topLeft = Offset(mapLeft, mapTop),
            size = Size(mapW, mapH),
        )

        val edgeStrips = listOf(
            Offset(mapLeft, mapTop) to Size(mapW, cell * 1.6f),
            Offset(mapLeft, mapTop + mapH - cell * 1.6f) to Size(mapW, cell * 1.6f),
            Offset(mapLeft, mapTop) to Size(cell * 1.4f, mapH),
            Offset(mapLeft + mapW - cell * 1.4f, mapTop) to Size(cell * 1.4f, mapH),
        )
        edgeStrips.forEach { (origin, size) ->
            scope.drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD).copy(alpha = 0.28f * t),
                        Color.Transparent,
                    ),
                ),
                topLeft = origin,
                size = size,
            )
        }

        coldCenters.forEachIndexed { index, point ->
            val mistR = cell * (2.2f + 0.15f * sin(animPhase * 1.1f + index))
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE1F5FE).copy(alpha = 0.22f * t),
                        Color(0xFF81D4FA).copy(alpha = 0.08f * t),
                        Color.Transparent,
                    ),
                    center = point,
                    radius = mistR,
                ),
                radius = mistR,
                center = point,
            )
        }

        var x = mapLeft
        while (x < mapLeft + mapW) {
            val h = cell * (0.35f + 0.25f * sin((x * 0.04f) + animPhase * 0.6f))
            scope.drawLine(
                Color.White.copy(alpha = 0.04f * t),
                Offset(x, mapTop),
                Offset(x + cell * 0.4f, mapTop + h),
                strokeWidth = 1.2f,
            )
            x += cell * 0.85f
        }
    }

    fun drawStunSparks(scope: DrawScope, center: Offset, radius: Float, hitStunTicksLeft: Int, animPhase: Float) {
        if (hitStunTicksLeft <= 0) return
        val ratio = hitStunTicksLeft.toFloat() / PacMazeConstants.GHOST_HIT_STUN_TICKS
        repeat(4) { i ->
            val angle = animPhase * 50f + i * 90f
            val dist = radius * (1.1f + 0.08f * sin((animPhase + i) * 5f))
            val end = Offset(
                center.x + cos(angle * PI / 180.0).toFloat() * dist,
                center.y + sin(angle * PI / 180.0).toFloat() * dist,
            )
            scope.drawLine(
                Color(0xFFFFF176).copy(alpha = 0.65f * ratio),
                center,
                end,
                strokeWidth = radius * 0.08f,
                cap = StrokeCap.Round,
            )
        }
    }

    /** 受击时性格色描边 + 暗色衬底，保证浅色地图上轮廓可见。 */
    fun drawHitStunAccent(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kindAccent: Color,
        hitStunTicksLeft: Int,
        animPhase: Float,
    ) {
        if (hitStunTicksLeft <= 0) return
        val ratio = hitStunTicksLeft.toFloat() / PacMazeConstants.GHOST_HIT_STUN_TICKS
        val pulse = 0.86f + 0.14f * sin(animPhase * 7.5f)
        scope.drawCircle(
            color = Color(0xFF1A1A2E).copy(alpha = 0.42f * ratio),
            radius = radius * 1.12f * pulse,
            center = center,
            style = Stroke(width = radius * 0.14f),
        )
        scope.drawCircle(
            color = kindAccent.copy(alpha = 0.92f * ratio * pulse),
            radius = radius * 1.06f * pulse,
            center = center,
            style = Stroke(width = radius * 0.1f),
        )
        scope.drawCircle(
            color = Color(0xFFFFF176).copy(alpha = 0.28f * ratio),
            radius = radius * 0.55f,
            center = center,
        )
    }

    private fun drawColdAura(scope: DrawScope, center: Offset, radius: Float, t: Float, pulse: Float) {
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF4FC3F7).copy(alpha = 0.14f * t),
                    Color(0xFF0288D1).copy(alpha = 0.06f * t),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 1.65f * pulse,
            ),
            radius = radius * 1.65f * pulse,
            center = center,
        )
    }

    private fun drawIceShellLayers(scope: DrawScope, shell: Path, center: Offset, radius: Float, t: Float) {
        scope.drawPath(
            shell,
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE1F5FE).copy(alpha = 0.52f * t),
                    Color(0xFF81D4FA).copy(alpha = 0.34f * t),
                    Color(0xFF0288D1).copy(alpha = 0.22f * t),
                ),
                center = center - Offset(radius * 0.12f, radius * 0.18f),
                radius = radius * 1.35f,
            ),
            style = Fill,
        )
        scope.drawPath(
            shell,
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f * t),
                    Color(0xFFB3E5FC).copy(alpha = 0.12f * t),
                    Color(0xFF0277BD).copy(alpha = 0.2f * t),
                ),
                startY = center.y - radius,
                endY = center.y + radius,
            ),
            style = Fill,
        )
    }

    private fun drawIceCracks(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        t: Float,
        animPhase: Float,
        shape: FrozenGhostShape,
        spin: Float,
    ) {
        val crackCount = 7
        val seed = (center.x * 17 + center.y * 31).toInt()
        repeat(crackCount) { i ->
            val baseAngle = i * (360f / crackCount) + seed * 0.17f +
            if (shape == FrozenGhostShape.DIAMOND) spin else 0f
            val angleRad = (baseAngle + sin(animPhase * 0.35f + i) * 4f) * PI / 180.0
            val startR = radius * (0.55f + (i % 3) * 0.08f)
            val endR = radius * (0.92f + (i % 2) * 0.06f)
            val start = Offset(
                center.x + cos(angleRad).toFloat() * startR,
                center.y + sin(angleRad).toFloat() * startR,
            )
            val end = Offset(
                center.x + cos(angleRad).toFloat() * endR,
                center.y + sin(angleRad).toFloat() * endR,
            )
            scope.drawLine(
                Color(0xFF0277BD).copy(alpha = 0.45f * t),
                start,
                end,
                strokeWidth = radius * 0.045f,
                cap = StrokeCap.Round,
            )
            scope.drawLine(
                Color.White.copy(alpha = 0.35f * t),
                start,
                end,
                strokeWidth = radius * 0.018f,
                cap = StrokeCap.Round,
            )
        }
    }

    private fun drawIceSpecular(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        t: Float,
        animPhase: Float,
        shape: FrozenGhostShape,
        spin: Float,
    ) {
        val shimmer = 0.75f + 0.25f * sin(animPhase * 2f)
        fun drawHighlight(offset: Offset, length: Float, angle: Float) {
            scope.rotate(angle + if (shape == FrozenGhostShape.DIAMOND) spin else 0f, center) {
                val start = center + offset
                val end = start + Offset(length * 0.85f, -length * 0.35f)
                scope.drawLine(
                    Color.White.copy(alpha = 0.55f * t * shimmer),
                    start,
                    end,
                    strokeWidth = radius * 0.055f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawHighlight(Offset(-radius * 0.35f, -radius * 0.42f), radius * 0.55f, -28f)
        drawHighlight(Offset(radius * 0.08f, -radius * 0.28f), radius * 0.35f, -18f)
    }

    private fun drawIceRim(scope: DrawScope, shell: Path, t: Float) {
        scope.drawPath(shell, Color.White.copy(alpha = 0.62f * t), style = Stroke(width = 1.8f))
        scope.drawPath(shell, Color(0xFF4FC3F7).copy(alpha = 0.35f * t), style = Stroke(width = 3.6f))
    }

    private fun drawBaseFrost(scope: DrawScope, center: Offset, radius: Float, t: Float) {
        val baseY = center.y + radius * 0.72f
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f * t),
                    Color(0xFFB3E5FC).copy(alpha = 0.25f * t),
                    Color.Transparent,
                ),
                center = Offset(center.x, baseY),
                radius = radius * 0.55f,
            ),
            topLeft = Offset(center.x - radius * 0.62f, baseY - radius * 0.12f),
            size = Size(radius * 1.24f, radius * 0.34f),
        )
        repeat(3) { i ->
            val ox = center.x + (i - 1) * radius * 0.22f
            scope.drawCircle(
                color = Color.White.copy(alpha = (0.28f - i * 0.05f) * t),
                radius = radius * (0.08f - i * 0.012f),
                center = Offset(ox, baseY + radius * 0.06f),
            )
        }
    }

    private fun drawSurfaceCrystals(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        t: Float,
        animPhase: Float,
        shape: FrozenGhostShape,
        spin: Float,
    ) {
        val angles = floatArrayOf(-50f, 15f, 78f, 145f, 210f, 288f)
        angles.forEachIndexed { i, angle ->
            val wobble = sin(animPhase * 1.2f + i * 1.7f) * 3f
            val rad = (angle + wobble + if (shape == FrozenGhostShape.DIAMOND) spin else 0f) * PI / 180.0
            val dist = radius * (0.92f + (i % 2) * 0.06f)
            val crystalCenter = Offset(
                center.x + cos(rad).toFloat() * dist,
                center.y + sin(rad).toFloat() * dist,
            )
            drawIceCrystal(scope, crystalCenter, radius * 0.16f, angle + 90f, t)
        }
    }

    private fun drawIceCrystal(scope: DrawScope, center: Offset, size: Float, angle: Float, alpha: Float) {
        scope.rotate(angle, center) {
            val path = Path().apply {
                moveTo(center.x, center.y - size)
                lineTo(center.x + size * 0.28f, center.y - size * 0.15f)
                lineTo(center.x + size * 0.18f, center.y + size * 0.55f)
                lineTo(center.x - size * 0.18f, center.y + size * 0.55f)
                lineTo(center.x - size * 0.28f, center.y - size * 0.15f)
                close()
            }
            scope.drawPath(path, Color(0xFFE1F5FE).copy(alpha = 0.82f * alpha), style = Fill)
            scope.drawPath(path, Color(0xFF0288D1).copy(alpha = 0.55f * alpha), style = Stroke(size * 0.06f))
            scope.drawLine(
                Color.White.copy(alpha = 0.7f * alpha),
                Offset(center.x, center.y - size * 0.85f),
                Offset(center.x, center.y + size * 0.35f),
                strokeWidth = size * 0.08f,
                cap = StrokeCap.Round,
            )
        }
    }

    private fun buildIceShellPath(
        center: Offset,
        radius: Float,
        shape: FrozenGhostShape,
        spin: Float,
    ): Path {
        return when (shape) {
            FrozenGhostShape.DIAMOND -> {
                val spinRad = spin * PI / 180.0
                val cosR = cos(spinRad).toFloat()
                val sinR = sin(spinRad).toFloat()
                fun rot(x: Float, y: Float): Offset {
                    return Offset(
                        center.x + x * cosR - y * sinR,
                        center.y + x * sinR + y * cosR,
                    )
                }
                val corners = listOf(
                    rot(0f, -radius * 1.12f),
                    rot(radius * 1.08f, 0f),
                    rot(0f, radius * 1.12f),
                    rot(-radius * 1.08f, 0f),
                )
                Path().apply {
                    moveTo(corners[0].x, corners[0].y)
                    corners.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
            }
            FrozenGhostShape.ROUND_GHOST -> Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            center.x - radius * 1.02f,
                            center.y - radius * 1.08f,
                            center.x + radius * 1.02f,
                            center.y + radius * 0.92f,
                        ),
                        cornerRadius = CornerRadius(radius * 0.52f, radius * 0.52f),
                    ),
                )
            }
        }
    }

    private fun lerpColor(from: Color, to: Color, t: Float): Color {
        val ratio = t.coerceIn(0f, 1f)
        return Color(
            red = from.red + (to.red - from.red) * ratio,
            green = from.green + (to.green - from.green) * ratio,
            blue = from.blue + (to.blue - from.blue) * ratio,
            alpha = from.alpha + (to.alpha - from.alpha) * ratio,
        )
    }
}
