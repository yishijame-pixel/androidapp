package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class GradientRibbonTrailRenderer(
    private val palette: RibbonTrailPalette,
) : PacMazeTrailRenderer {
    override val trailId: PacMazeTrailId = palette.trailId
    private val sparkSeed = Random(palette.sparkSeed)

    override fun draw(
        scope: DrawScope,
        samples: List<PacMazeTrailSample>,
        palette: PacMazeThemePalette,
        cell: Float,
        powerActive: Boolean,
    ) {
        if (samples.isEmpty()) return
        val p = this.palette
        val lastSample = samples.last()
        val vel = lastSample.velocity
        val velSpeed = kotlin.math.hypot(vel.x.toDouble(), vel.y.toDouble()).toFloat()
        val bleed = com.example.funlife.social.game.engine.pacmaze.PacMazeEntityVisuals.trailHeadBleedPx(
            cell,
            powerActive,
        )
        val headBleed = if (velSpeed > 1f) {
            Offset(vel.x / velSpeed * bleed, vel.y / velSpeed * bleed)
        } else {
            Offset.Zero
        }
        val points = samples.map { it.position }
        val visualHead = points.last() + headBleed
        val pathPoints = if (headBleed != Offset.Zero) points + visualHead else points
        val path = buildSmoothTrailPath(pathPoints) ?: return
        val head = visualHead
        val tail = points.first()

        listOf(
            cell * 0.62f to 0.10f,
            cell * 0.46f to 0.16f,
            cell * 0.32f to 0.24f,
        ).forEach { (width, alphaMul) ->
            scope.drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        p.glowTail.copy(alpha = 0.04f * alphaMul * 4f),
                        p.glowMid.copy(alpha = 0.10f * alphaMul * 4f),
                        p.glowHead.copy(alpha = 0.14f * alphaMul * 4f),
                    ),
                    start = tail,
                    end = head,
                ),
                style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }

        for (i in 0 until points.size - 1) {
            val t0 = i.toFloat() / (points.size - 1).coerceAtLeast(1)
            val t1 = (i + 1).toFloat() / (points.size - 1).coerceAtLeast(1)
            val c0 = p.colorAt(t0, powerActive)
            val c1 = p.colorAt(t1, powerActive)
            val segWidth = cell * (0.12f + t1 * 0.38f) * if (powerActive) 1.15f else 1f
            scope.drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        c0.copy(alpha = 0.08f + t0 * 0.35f),
                        c1.copy(alpha = 0.18f + t1 * 0.55f),
                    ),
                    start = points[i],
                    end = points[i + 1],
                ),
                start = points[i],
                end = points[i + 1],
                strokeWidth = segWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }

        if (points.size >= 2) {
            val coreSource = if (headBleed != Offset.Zero) {
                points.takeLast((points.size * 0.65f).toInt().coerceAtLeast(2)) + visualHead
            } else {
                points.takeLast((points.size * 0.65f).toInt().coerceAtLeast(2))
            }
            val corePath = buildSmoothTrailPath(coreSource) ?: path
            scope.drawPath(
                path = corePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        p.coreMid.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.75f),
                    ),
                    start = tail,
                    end = head,
                ),
                style = Stroke(width = cell * 0.10f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }

        samples.forEachIndexed { index, sample ->
            val t = sample.age
            if (t < 0.25f) return@forEachIndexed
            val sparkCount = if (powerActive) 3 else 2
            repeat(sparkCount) { j ->
                val angle = sparkSeed.nextFloat() * 6.283f + index * 0.55f + j
                val spread = cell * (0.06f + t * 0.22f)
                val pos = Offset(
                    sample.position.x + cos(angle) * spread,
                    sample.position.y + sin(angle) * spread,
                )
                val sparkleAlpha = (t - 0.2f) * (0.35f + sparkSeed.nextFloat() * 0.45f)
                val r = cell * (0.025f + t * 0.045f)
                scope.drawCircle(
                    color = Color.White.copy(alpha = sparkleAlpha),
                    radius = r,
                    center = pos,
                )
                scope.drawCircle(
                    color = p.sparkleSecondary.copy(alpha = sparkleAlpha * 0.65f),
                    radius = r * 0.55f,
                    center = pos,
                )
            }
        }

        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    p.footInner.copy(alpha = if (powerActive) 0.62f else 0.50f),
                    p.footOuter.copy(alpha = 0.28f),
                    Color.Transparent,
                ),
                center = head,
                radius = cell * 0.50f,
            ),
            radius = cell * 0.50f,
            center = head,
        )
    }
}

internal object RibbonFlowTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.FLOW)
internal object RibbonSakuraTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.SAKURA)
internal object RibbonAuroraTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.AURORA)
internal object RibbonPhoenixTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.PHOENIX)
internal object RibbonSoulTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.SOUL)
internal object RibbonJadeTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.JADE)
internal object RibbonCinnabarTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.CINNABAR)
internal object RibbonCeladonTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.CELADON)
internal object RibbonVioletTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.VIOLET)
internal object RibbonGinkgoTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.GINKGO)
internal object RibbonMintBubbleTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.MINT_BUBBLE)
internal object RibbonNightInkTrailRenderer : PacMazeTrailRenderer by GradientRibbonTrailRenderer(RibbonTrailPalettes.NIGHT_INK)
