package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlin.random.Random

class PacMazeParticleField(
    private val config: PacMazeParticleConfig,
    seed: Long = 42L,
) {
    private data class Particle(
        var x: Float,
        var y: Float,
        val speed: Float,
        val glyph: String,
        val sizeScale: Float,
    )

    private val particles: List<Particle> = if (!config.enabled) {
        emptyList()
    } else {
        val rng = Random(seed)
        List(config.count) {
            Particle(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                speed = config.speedMin + rng.nextFloat() * (config.speedMax - config.speedMin),
                glyph = config.glyphPool.random(rng),
                sizeScale = 0.7f + rng.nextFloat() * 0.6f,
            )
        }
    }

    fun advance(deltaSec: Float) {
        if (!config.enabled) return
        particles.forEach { p ->
            p.y += p.speed * deltaSec / 520f
            if (p.y > 1.05f) {
                p.y = -0.05f
                p.x = Random.nextFloat()
            }
        }
    }

    fun draw(scope: DrawScope, canvasSize: Size, animPhase: Float) {
        if (!config.enabled || particles.isEmpty()) return
        scope.drawContext.canvas.nativeCanvas.apply {
            particles.forEach { p ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.color.copy(alpha = config.alpha).toArgb()
                    textSize = 10f * p.sizeScale + (animPhase % 1f)
                    typeface = Typeface.MONOSPACE
                }
                drawText(
                    p.glyph,
                    p.x * canvasSize.width,
                    p.y * canvasSize.height,
                    paint,
                )
            }
        }
    }
}
