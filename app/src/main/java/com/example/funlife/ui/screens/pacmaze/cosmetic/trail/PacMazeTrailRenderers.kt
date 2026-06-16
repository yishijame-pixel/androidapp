package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.maptheme.CyberVisualEffects
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal object NoneTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.NONE
    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) = Unit
}

internal object NeonPixelTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.NEON_PIXEL

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        val color = if (powerActive) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonRed
        samples.forEach { sample ->
            val t = sample.age
            val size = cell * 0.22f * t * if (powerActive) 1.2f else 1f
            scope.drawRect(
                color = color.copy(alpha = t * 0.55f),
                topLeft = Offset(sample.position.x - size / 2f, sample.position.y - size / 2f),
                size = Size(size, size),
            )
        }
    }
}

internal object IonWakeTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.ION_WAKE

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        if (samples.size < 2) return
        val head = samples.last()
        val tail = samples.first()
        val core = if (powerActive) palette.powerCore else palette.frameAccent
        val angle = atan2(head.velocity.y, head.velocity.x)
        val len = cell * (if (powerActive) 1.8f else 1.2f)
        val tip = head.position
        val base = Offset(
            tip.x - cos(angle) * len,
            tip.y - sin(angle) * len,
        )
        scope.drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, core.copy(alpha = 0.15f), core.copy(alpha = 0.75f)),
                start = base,
                end = tip,
            ),
            start = base,
            end = tip,
            strokeWidth = cell * 0.28f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        scope.drawCircle(color = Color.White.copy(alpha = 0.85f), radius = cell * 0.08f, center = tip)
        samples.dropLast(1).forEach { sample ->
            scope.drawCircle(
                color = core.copy(alpha = sample.age * 0.35f),
                radius = cell * 0.06f * sample.age,
                center = sample.position,
            )
        }
    }
}

internal object GhostEchoTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.GHOST_ECHO

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        val echoes = samples.takeLast(4).dropLast(1)
        val baseR = cell * 0.38f
        echoes.forEachIndexed { index, sample ->
            val t = (index + 1) / 4f
            val alpha = (1f - t) * 0.35f
            val r = baseR * (0.55f + t * 0.35f)
            scope.drawCircle(
                color = palette.frameAccent.copy(alpha = alpha),
                radius = r,
                center = sample.position,
                style = Stroke(width = cell * 0.05f),
            )
            scope.drawCircle(
                color = palette.frameAccent.copy(alpha = alpha * 0.5f),
                radius = r * 0.55f,
                center = sample.position,
            )
        }
    }
}

internal object StarCometTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.STAR_COMET
    private val seed = Random(42)

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        val colors = if (powerActive) {
            listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF7C4DFF))
        } else {
            listOf(palette.frameAccent, palette.powerGlow, Color.White)
        }
        samples.forEachIndexed { index, sample ->
            val t = sample.age
            val sparkCount = if (powerActive) 3 else 2
            repeat(sparkCount) { i ->
                val angle = seed.nextFloat() * 6.28f + index * 0.7f + i
                val dist = cell * (0.08f + t * 0.35f) * (0.6f + seed.nextFloat())
                val pos = Offset(
                    sample.position.x + cos(angle) * dist,
                    sample.position.y + sin(angle) * dist,
                )
                val color = colors[(index + i) % colors.size]
                val r = cell * (0.03f + t * 0.07f)
                scope.drawCircle(color = color.copy(alpha = t * 0.8f), radius = r, center = pos)
            }
        }
        if (samples.isNotEmpty()) {
            val head = samples.last()
            scope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), colors.first().copy(alpha = 0.4f), Color.Transparent),
                    center = head.position,
                    radius = cell * 0.22f,
                ),
                radius = cell * 0.22f,
                center = head.position,
            )
        }
    }
}
