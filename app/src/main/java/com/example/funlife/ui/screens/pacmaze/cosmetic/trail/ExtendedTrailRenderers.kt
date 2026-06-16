package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.maptheme.CyberVisualEffects
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val petalSeed = Random(17)
private val noteSeed = Random(23)
private val candySeed = Random(31)
private val snowSeed = Random(37)
private val dataSeed = Random(43)
private val cubeSeed = Random(47)

private fun DrawScope.drawPetal(center: Offset, size: Float, rotation: Float, color: Color, alpha: Float) {
    rotate(rotation, pivot = center) {
        val petal = Path().apply {
            moveTo(center.x, center.y - size)
            cubicTo(center.x + size * 0.55f, center.y - size * 0.35f, center.x + size * 0.45f, center.y + size * 0.35f, center.x, center.y + size * 0.15f)
            cubicTo(center.x - size * 0.45f, center.y + size * 0.35f, center.x - size * 0.55f, center.y - size * 0.35f, center.x, center.y - size)
        }
        drawPath(petal, color = color.copy(alpha = alpha))
    }
}

private fun DrawScope.drawMusicNote(center: Offset, cell: Float, color: Color, alpha: Float) {
    val headR = cell * 0.07f
    drawOval(
        color = color.copy(alpha = alpha),
        topLeft = Offset(center.x - headR, center.y - headR * 0.7f),
        size = Size(headR * 2f, headR * 1.4f),
    )
    drawLine(color.copy(alpha = alpha), Offset(center.x + headR * 0.6f, center.y - headR * 0.2f), Offset(center.x + headR * 0.6f, center.y - cell * 0.28f), cell * 0.035f)
    drawLine(color.copy(alpha = alpha), Offset(center.x + headR * 0.6f, center.y - cell * 0.28f), Offset(center.x + cell * 0.12f, center.y - cell * 0.18f), cell * 0.03f)
}

private fun DrawScope.drawHexCell(center: Offset, radius: Float, color: Color, alpha: Float, strokeW: Float) {
    val path = Path()
    repeat(6) { i ->
        val angle = Math.toRadians((60.0 * i - 30.0))
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color.copy(alpha = alpha * 0.35f))
    drawPath(path, color = color.copy(alpha = alpha), style = Stroke(strokeW))
}

private fun DrawScope.drawMiniCube(center: Offset, cell: Float, rotation: Float, color: Color, alpha: Float) {
    rotate(rotation, pivot = center) {
        val s = cell * 0.14f
        drawRect(color = color.copy(alpha = alpha), topLeft = Offset(center.x - s / 2f, center.y - s / 2f), size = Size(s, s))
        drawRect(color = Color.White.copy(alpha = alpha * 0.35f), topLeft = Offset(center.x - s * 0.15f, center.y - s * 0.55f), size = Size(s * 0.55f, s * 0.35f))
        drawRect(color = color.copy(alpha = alpha * 0.85f), topLeft = Offset(center.x - s / 2f, center.y - s / 2f), size = Size(s, s), style = Stroke(cell * 0.02f))
    }
}

private fun DrawScope.drawPaw(center: Offset, cell: Float, angle: Float, color: Color, alpha: Float) {
    rotate(angle, pivot = center) {
        drawOval(color = color.copy(alpha = alpha), topLeft = Offset(center.x - cell * 0.1f, center.y - cell * 0.08f), size = Size(cell * 0.2f, cell * 0.16f))
        listOf(-0.08f, -0.02f, 0.04f, 0.1f).forEach { dx ->
            drawCircle(color = color.copy(alpha = alpha * 0.9f), radius = cell * 0.035f, center = Offset(center.x + cell * dx, center.y - cell * 0.12f))
        }
    }
}

internal object PetalShowerTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.PETAL_SHOWER

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val colors = if (powerActive) {
                listOf(Color(0xFFFF4081), Color(0xFFF48FB1), Color(0xFFFFCDD2))
            } else {
                listOf(Color(0xFFF06292), Color(0xFFF8BBD0), Color(0xFFFFEBEE))
            }
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                if (t < 0.15f) return@forEachIndexed
                repeat(if (powerActive) 3 else 2) { j ->
                    val drift = petalSeed.nextFloat() * 6.28f + index * 0.6f + j
                    val dist = cell * (0.1f + t * 0.32f) * (0.7f + petalSeed.nextFloat() * 0.5f)
                    val pos = Offset(
                        sample.position.x + cos(drift) * dist,
                        sample.position.y + sin(drift) * dist + t * cell * 0.08f,
                    )
                    drawPetal(pos, cell * (0.06f + t * 0.05f), drift * 57f + index * 12f, colors[(index + j) % colors.size], t * 0.7f)
                }
            }
        }
    }
}

internal object NoteHopTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.NOTE_HOP

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val colors = if (powerActive) {
                listOf(Color(0xFFFFD54F), Color(0xFF7C4DFF), Color(0xFF40C4FF))
            } else {
                listOf(Color(0xFF5C6BC0), Color(0xFF7986CB), Color(0xFF9575CD))
            }
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                val hop = sin((index + sample.age) * 1.8f + noteSeed.nextFloat()) * cell * 0.12f * t
                val pos = sample.position + Offset(0f, hop - t * cell * 0.04f)
                drawMusicNote(pos, cell, colors[index % colors.size], t * 0.75f)
                if (t > 0.5f) {
                    drawCircle(color = colors[(index + 1) % colors.size].copy(alpha = t * 0.25f), radius = cell * 0.025f, center = pos + Offset(cell * 0.08f, -cell * 0.06f))
                }
            }
        }
    }
}

internal object CandyCrumbTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.CANDY_CRUMB

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val colors = if (powerActive) {
                listOf(Color(0xFFFF4081), Color(0xFFFFD54F), Color(0xFF69F0AE), Color(0xFF40C4FF))
            } else {
                listOf(Color(0xFFFF80AB), Color(0xFFFFB74D), Color(0xFF81D4FA), Color(0xFFB39DDB))
            }
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                repeat(if (powerActive) 4 else 3) { j ->
                    val angle = candySeed.nextFloat() * 6.28f + index * 0.8f
                    val dist = cell * (0.05f + t * 0.28f) * (0.5f + candySeed.nextFloat())
                    val pos = Offset(sample.position.x + cos(angle) * dist, sample.position.y + sin(angle) * dist)
                    val crumbW = cell * (0.04f + candySeed.nextFloat() * 0.05f)
                    drawRoundRect(
                        color = colors[(index + j) % colors.size].copy(alpha = t * 0.8f),
                        topLeft = Offset(pos.x - crumbW / 2f, pos.y - crumbW / 2f),
                        size = Size(crumbW, crumbW * 0.75f),
                        cornerRadius = CornerRadius(crumbW * 0.25f),
                    )
                }
            }
        }
    }
}

internal object SnowSwirlTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.SNOW_SWIRL

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val core = if (powerActive) Color(0xFFB3E5FC) else Color.White
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                val swirl = sample.age * 4.2f + index * 0.55f
                repeat(if (powerActive) 3 else 2) { j ->
                    val dist = cell * (0.08f + t * 0.24f)
                    val angle = swirl + j * 2.1f + snowSeed.nextFloat()
                    val pos = Offset(sample.position.x + cos(angle) * dist, sample.position.y + sin(angle) * dist)
                    val arm = cell * 0.045f * t
                    repeat(6) { spoke ->
                        rotate(spoke * 60f + angle * 57f, pivot = pos) {
                            drawLine(core.copy(alpha = t * 0.65f), pos, Offset(pos.x, pos.y - arm), cell * 0.018f)
                        }
                    }
                    drawCircle(color = core.copy(alpha = t * 0.5f), radius = cell * 0.02f, center = pos)
                }
            }
        }
    }
}

internal object HexHoneyTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.HEX_HONEY

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val honey = if (powerActive) Color(0xFFFFD54F) else Color(0xFFFFB300)
            val wax = if (powerActive) Color(0xFFFF8F00) else Color(0xFFFFA000)
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                val hexR = cell * (0.08f + t * 0.1f)
                drawHexCell(sample.position, hexR, honey, t * 0.75f, cell * 0.025f)
                if (index % 2 == 0) {
                    drawHexCell(sample.position + Offset(cell * 0.09f, cell * 0.05f), hexR * 0.72f, wax, t * 0.55f, cell * 0.02f)
                }
                if (t > 0.4f) {
                    drawCircle(color = honey.copy(alpha = t * 0.2f), radius = hexR * 0.35f, center = sample.position)
                }
            }
        }
    }
}

internal object DataCascadeTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.DATA_CASCADE

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val colors = if (powerActive) {
                listOf(CyberVisualEffects.NeonYellow, CyberVisualEffects.NeonBlue, CyberVisualEffects.NeonPink)
            } else {
                listOf(CyberVisualEffects.NeonBlue, Color(0xFF00E676), CyberVisualEffects.NeonBlueDeep)
            }
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                repeat(if (powerActive) 4 else 3) { row ->
                    val fall = (t * 0.8f + row * 0.18f + dataSeed.nextFloat() * 0.1f) * cell * 0.55f
                    val xJitter = (dataSeed.nextFloat() - 0.5f) * cell * 0.18f
                    val barTop = sample.position.y - cell * 0.08f + fall
                    val barH = cell * (0.12f + t * 0.22f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(colors[(index + row) % colors.size].copy(alpha = 0.05f), colors[(index + row) % colors.size].copy(alpha = t * 0.55f)),
                            startY = barTop,
                            endY = barTop + barH,
                        ),
                        topLeft = Offset(sample.position.x + xJitter - cell * 0.025f, barTop),
                        size = Size(cell * 0.05f, barH),
                    )
                    if (row == 0 && t > 0.35f) {
                        drawCircle(color = colors[index % colors.size].copy(alpha = t * 0.85f), radius = cell * 0.035f, center = Offset(sample.position.x + xJitter, barTop + barH))
                    }
                }
            }
            if (samples.isNotEmpty()) {
                val head = samples.last()
                drawLine(
                    brush = Brush.linearGradient(listOf(Color.Transparent, colors.first().copy(alpha = 0.45f)), start = head.position + Offset(0f, -cell * 0.4f), end = head.position),
                    start = head.position + Offset(0f, -cell * 0.4f),
                    end = head.position,
                    strokeWidth = cell * 0.06f,
                )
            }
        }
    }
}

internal object RadarSweepTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.RADAR_SWEEP

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        if (samples.isEmpty()) return
        scope.run {
            val head = samples.last()
            val sweepColor = if (powerActive) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue
            val maxR = cell * (if (powerActive) 1.6f else 1.1f)
            repeat(3) { ring ->
                drawCircle(color = sweepColor.copy(alpha = 0.12f - ring * 0.03f), radius = maxR * (0.35f + ring * 0.22f), center = head.position, style = Stroke(cell * 0.025f))
            }
            val sweepAngle = head.age * 280f + samples.size * 8f
            rotate(sweepAngle, pivot = head.position) {
                drawLine(
                    brush = Brush.linearGradient(listOf(sweepColor.copy(alpha = 0.08f), sweepColor.copy(alpha = 0.65f)), start = head.position, end = head.position + Offset(maxR, 0f)),
                    start = head.position,
                    end = head.position + Offset(maxR, 0f),
                    strokeWidth = cell * 0.05f,
                )
                drawArc(color = sweepColor.copy(alpha = 0.18f), startAngle = -28f, sweepAngle = 56f, useCenter = true, topLeft = Offset(head.position.x - maxR, head.position.y - maxR), size = Size(maxR * 2f, maxR * 2f))
            }
            samples.dropLast(1).forEach { sample ->
                drawCircle(color = sweepColor.copy(alpha = sample.age * 0.25f), radius = cell * 0.04f * sample.age, center = sample.position)
            }
        }
    }
}

internal object CubeShatterTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.CUBE_SHATTER

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val colors = if (powerActive) {
                listOf(CyberVisualEffects.NeonBlue, CyberVisualEffects.NeonPink, Color.White)
            } else {
                listOf(Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFE1F5FE))
            }
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                repeat(if (powerActive) 4 else 3) { j ->
                    val angle = cubeSeed.nextFloat() * 6.28f + index * 0.9f
                    val dist = cell * (0.06f + t * 0.38f) * (0.6f + cubeSeed.nextFloat())
                    val pos = Offset(sample.position.x + cos(angle) * dist, sample.position.y + sin(angle) * dist)
                    drawMiniCube(pos, cell, angle * 57f + index * 15f, colors[(index + j) % colors.size], t * 0.75f)
                }
            }
        }
    }
}

internal object PawPrintTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.PAW_PRINT

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val color = if (powerActive) Color(0xFFFFAB40) else palette.frameAccent.copy(alpha = 0.85f)
            samples.forEachIndexed { index, sample ->
                val t = sample.age
                if (t < 0.2f) return@forEachIndexed
                val velocityAngle = if (sample.velocity.x != 0f || sample.velocity.y != 0f) {
                    atan2(sample.velocity.y, sample.velocity.x) * 57.2958f
                } else {
                    0f
                }
                val side = if (index % 2 == 0) -1f else 1f
                val offset = Offset(cos((velocityAngle + 90f) * 0.017453292f) * cell * 0.08f * side, sin((velocityAngle + 90f) * 0.017453292f) * cell * 0.08f * side)
                drawPaw(sample.position + offset, cell, velocityAngle + side * 12f, color, t * 0.55f)
            }
        }
    }
}

internal object RippleStepTrailRenderer : PacMazeTrailRenderer {
    override val trailId = PacMazeTrailId.RIPPLE_STEP

    override fun draw(scope: DrawScope, samples: List<PacMazeTrailSample>, palette: PacMazeThemePalette, cell: Float, powerActive: Boolean) {
        scope.run {
            val core = if (powerActive) palette.powerCore else palette.frameAccent
            samples.forEach { sample ->
                val t = sample.age
                repeat(3) { ring ->
                    val expand = t * (0.22f + ring * 0.12f)
                    val r = cell * expand
                    if (r >= cell * 0.02f) {
                        drawCircle(color = core.copy(alpha = (1f - expand) * 0.35f), radius = r, center = sample.position, style = Stroke(width = cell * 0.035f * (1f - ring * 0.15f)))
                    }
                }
                if (t > 0.45f) {
                    drawCircle(color = Color.White.copy(alpha = t * 0.25f), radius = cell * 0.03f, center = sample.position)
                }
            }
        }
    }
}
