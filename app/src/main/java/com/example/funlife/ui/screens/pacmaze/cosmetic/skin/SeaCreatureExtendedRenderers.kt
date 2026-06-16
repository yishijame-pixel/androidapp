package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.cos
import kotlin.math.sin

/** 荧光乌贼：伞体脉动 · 触须星点 */
internal object SeaSquidSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_SQUID

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val pulse = 1f + sin(pose.animPhase * (if (pose.isMoving) 2.6f else 1.4f)) * 0.08f
            val mantleCenter = Offset(c.x + radius * 0.12f, c.y - radius * 0.06f + swim.ripple)

            scale(scaleX = pulse, scaleY = 1f / pulse.coerceAtLeast(0.85f), pivot = mantleCenter) {
                val mantle = Path().apply {
                    moveTo(c.x - radius * 0.18f, c.y + radius * 0.12f + swim.ripple)
                    cubicTo(
                        c.x - radius * 0.08f, c.y - radius * 0.42f,
                        c.x + radius * 0.38f, c.y - radius * 0.38f,
                        c.x + radius * 0.44f, c.y + radius * 0.04f + swim.ripple,
                    )
                    cubicTo(
                        c.x + radius * 0.42f, c.y + radius * 0.22f + swim.ripple,
                        c.x + radius * 0.08f, c.y + radius * 0.28f + swim.ripple,
                        c.x - radius * 0.18f, c.y + radius * 0.12f + swim.ripple,
                    )
                }
                fillSeaBody(mantle, sea, c, radius)
                strokeSea(mantle, sea, lineW)

                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(sea.accent.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(mantleCenter.x - radius * 0.08f, mantleCenter.y - radius * 0.1f),
                        radius = radius * 0.28f,
                    ),
                    topLeft = Offset(mantleCenter.x - radius * 0.22f, mantleCenter.y - radius * 0.18f),
                    size = Size(radius * 0.44f, radius * 0.3f),
                )
            }

            val waveFreq = if (pose.isMoving) 3.4f else 1.8f
            repeat(6) { i ->
                val phase = pose.animPhase * waveFreq + i * 0.9f
                val baseX = c.x - radius * 0.06f + i * radius * 0.07f
                val tentacle = Path().apply {
                    moveTo(baseX, c.y + radius * 0.2f + swim.ripple)
                    cubicTo(
                        baseX + sin(phase) * radius * 0.1f, c.y + radius * 0.42f,
                        baseX - sin(phase * 0.85f) * radius * 0.12f, c.y + radius * 0.62f,
                        baseX + sin(phase * 1.15f) * radius * 0.08f, c.y + radius * 0.82f,
                    )
                }
                drawPath(
                    tentacle,
                    color = sea.detail.copy(alpha = 0.5f + i * 0.05f),
                    style = Stroke(lineW * (0.6f - i * 0.04f), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                repeat(3) { j ->
                    val t = (j + 1f) / 4f
                    val pt = Offset(
                        baseX + sin(phase + j) * radius * 0.04f,
                        c.y + radius * (0.2f + 0.62f * t) + swim.ripple,
                    )
                    drawCircle(color = sea.accent.copy(alpha = 0.7f), radius = radius * 0.022f, center = pt)
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = radius * 0.01f, center = pt)
                }
            }

            drawFishEye(Offset(c.x + radius * 0.32f, c.y - radius * 0.08f + swim.ripple), radius, sea, lineW)
            if (pose.powerActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sea.accent.copy(alpha = 0.9f), Color.Transparent),
                        center = Offset(c.x + radius * 0.5f, c.y + radius * 0.04f),
                        radius = radius * 0.12f,
                    ),
                    radius = radius * 0.1f,
                    center = Offset(c.x + radius * 0.5f, c.y + radius * 0.04f),
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 灯笼鱼：柔光灯笼 · 深渊引路 */
internal object SeaAnglerSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_ANGLER

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val lureGlow = 0.65f + sin(pose.animPhase * 2.8f) * 0.35f
            val ripple = swim.ripple

            val tailPivot = Offset(c.x - radius * 0.44f, c.y + ripple)
            rotate(swim.tailBeatDeg, pivot = tailPivot) {
                val tail = Path().apply {
                    moveTo(tailPivot.x, tailPivot.y)
                    lineTo(c.x - radius * 0.78f, c.y - radius * 0.18f + ripple)
                    lineTo(c.x - radius * 0.78f, c.y + radius * 0.18f + ripple)
                    close()
                }
                fillSeaBody(tail, sea, c, radius)
                strokeSea(tail, sea, lineW * 0.8f)
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y + ripple)
                cubicTo(
                    c.x - radius * 0.1f, c.y - radius * 0.36f,
                    c.x + radius * 0.38f, c.y - radius * 0.3f,
                    c.x + radius * 0.58f, c.y + radius * 0.02f + ripple,
                )
                cubicTo(
                    c.x + radius * 0.48f, c.y + radius * 0.32f,
                    c.x + radius * 0.04f, c.y + radius * 0.34f,
                    c.x - radius * 0.18f, c.y + radius * 0.28f,
                )
                cubicTo(
                    c.x - radius * 0.36f, c.y + radius * 0.22f,
                    c.x - radius * 0.42f, c.y + radius * 0.1f,
                    c.x - radius * 0.42f, c.y + ripple,
                )
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW)

            val dorsal = Path().apply {
                moveTo(c.x + radius * 0.06f, c.y - radius * 0.2f + ripple)
                cubicTo(
                    c.x + radius * 0.04f, c.y - radius * 0.48f,
                    c.x + radius * 0.18f, c.y - radius * 0.62f,
                    c.x + radius * 0.22f, c.y - radius * 0.38f,
                )
            }
            drawPath(dorsal, color = sea.fin.copy(alpha = 0.85f), style = Stroke(lineW * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val lureStem = Path().apply {
                moveTo(c.x + radius * 0.14f, c.y - radius * 0.22f + ripple)
                cubicTo(
                    c.x + radius * 0.22f, c.y - radius * 0.48f,
                    c.x + radius * 0.34f, c.y - radius * 0.58f,
                    c.x + radius * 0.38f, c.y - radius * 0.72f,
                )
            }
            drawPath(lureStem, color = sea.stroke.copy(alpha = 0.6f), style = Stroke(lineW * 0.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val lureCenter = Offset(c.x + radius * 0.4f, c.y - radius * 0.76f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sea.accent.copy(alpha = lureGlow),
                        sea.accent.copy(alpha = lureGlow * 0.4f),
                        Color.Transparent,
                    ),
                    center = lureCenter,
                    radius = radius * 0.2f,
                ),
                radius = radius * 0.18f,
                center = lureCenter,
            )
            drawCircle(color = sea.detail.copy(alpha = 0.9f), radius = radius * 0.06f, center = lureCenter)
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = radius * 0.025f, center = Offset(lureCenter.x - radius * 0.02f, lureCenter.y - radius * 0.02f))

            drawFishEye(Offset(c.x + radius * 0.36f, c.y - radius * 0.04f + ripple), radius, sea, lineW)
            repeat(4) { i ->
                val gx = c.x + radius * (0.08f - i * 0.08f)
                drawLine(
                    sea.detail.copy(alpha = 0.3f),
                    Offset(gx, c.y + radius * 0.04f + ripple),
                    Offset(gx, c.y + radius * 0.16f + ripple),
                    lineW * 0.38f,
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 寄居蟹：螺壳护身 · 钳舞横行 */
internal object SeaHermitSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_HERMIT

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val scuttle = sin(pose.animPhase * (if (pose.isMoving) 4f else 2f)) * radius * 0.03f
            val ripple = swim.ripple + scuttle

            val shell = Path().apply {
                moveTo(c.x - radius * 0.08f, c.y + radius * 0.28f + ripple)
                cubicTo(
                    c.x - radius * 0.38f, c.y - radius * 0.12f,
                    c.x - radius * 0.28f, c.y - radius * 0.52f,
                    c.x + radius * 0.08f, c.y - radius * 0.48f,
                )
                cubicTo(
                    c.x + radius * 0.36f, c.y - radius * 0.44f,
                    c.x + radius * 0.42f, c.y - radius * 0.08f,
                    c.x + radius * 0.28f, c.y + radius * 0.22f + ripple,
                )
                cubicTo(
                    c.x + radius * 0.18f, c.y + radius * 0.38f + ripple,
                    c.x - radius * 0.02f, c.y + radius * 0.4f + ripple,
                    c.x - radius * 0.08f, c.y + radius * 0.28f + ripple,
                )
            }
            drawPath(
                shell,
                brush = Brush.linearGradient(
                    colors = listOf(sea.fillTop, sea.fillBottom, sea.detail.copy(alpha = 0.8f)),
                    start = Offset(c.x - radius * 0.2f, c.y - radius * 0.3f),
                    end = Offset(c.x + radius * 0.3f, c.y + radius * 0.2f),
                ),
            )
            strokeSea(shell, sea, lineW)
            repeat(4) { i ->
                val spiral = Offset(
                    c.x + radius * (-0.06f + i * 0.06f),
                    c.y - radius * (0.18f - i * 0.05f) + ripple,
                )
                drawArc(
                    color = sea.stroke.copy(alpha = 0.35f),
                    startAngle = 30f + i * 40f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(spiral.x - radius * 0.1f, spiral.y - radius * 0.08f),
                    size = Size(radius * 0.2f, radius * 0.16f),
                    style = Stroke(lineW * 0.35f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }

            val body = Path().apply {
                moveTo(c.x + radius * 0.18f, c.y + radius * 0.14f + ripple)
                cubicTo(
                    c.x + radius * 0.34f, c.y + radius * 0.08f,
                    c.x + radius * 0.42f, c.y + radius * 0.18f,
                    c.x + radius * 0.38f, c.y + radius * 0.28f + ripple,
                )
                cubicTo(
                    c.x + radius * 0.3f, c.y + radius * 0.34f,
                    c.x + radius * 0.2f, c.y + radius * 0.3f,
                    c.x + radius * 0.18f, c.y + radius * 0.14f + ripple,
                )
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW * 0.85f)

            listOf(-0.08f to swim.finBeatDeg, 0.22f to -swim.finBeatDeg).forEach { (dx, beat) ->
                val clawPivot = Offset(c.x + radius * dx, c.y + radius * 0.22f + ripple)
                rotate(beat, pivot = clawPivot) {
                    val claw = Path().apply {
                        moveTo(clawPivot.x, clawPivot.y)
                        cubicTo(
                            clawPivot.x + radius * 0.14f, clawPivot.y - radius * 0.06f,
                            clawPivot.x + radius * 0.22f, clawPivot.y + radius * 0.04f,
                            clawPivot.x + radius * 0.16f, clawPivot.y + radius * 0.14f,
                        )
                        cubicTo(
                            clawPivot.x + radius * 0.08f, clawPivot.y + radius * 0.1f,
                            clawPivot.x + radius * 0.02f, clawPivot.y + radius * 0.04f,
                            clawPivot.x, clawPivot.y,
                        )
                    }
                    drawPath(claw, color = sea.fin)
                    strokeSea(claw, sea, lineW * 0.65f)
                }
            }

            listOf(-0.14f, 0.06f).forEach { dx ->
                val leg = Offset(c.x + radius * dx, c.y + radius * 0.34f + ripple)
                drawLine(sea.stroke.copy(alpha = 0.5f), leg, Offset(leg.x - radius * 0.04f, leg.y + radius * 0.12f), lineW * 0.45f)
            }

            drawCuteEye(Offset(c.x + radius * 0.32f, c.y + radius * 0.12f + ripple), radius * 0.09f, sea, lineW)
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 海星精灵：五腕旋转 · 吸盘爬行 */
internal object SeaStarfishSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_STARFISH

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val spin = sin(pose.animPhase * (if (pose.isMoving) 2.2f else 1.2f)) * 8f
            val starCenter = Offset(c.x + radius * 0.06f, c.y + radius * 0.04f + swim.ripple * 0.5f)

            rotate(spin, pivot = starCenter) {
                val star = Path().apply {
                    val arms = 5
                    val outerR = radius * 0.46f
                    val innerR = radius * 0.18f
                    repeat(arms) { i ->
                        val angle = (i * 360f / arms - 90f) * (Math.PI / 180).toFloat()
                        val tipX = starCenter.x + cos(angle) * outerR
                        val tipY = starCenter.y + sin(angle) * outerR
                        val innerAngle1 = angle - (36f * (Math.PI / 180).toFloat())
                        val innerAngle2 = angle + (36f * (Math.PI / 180).toFloat())
                        val inner1X = starCenter.x + cos(innerAngle1) * innerR
                        val inner1Y = starCenter.y + sin(innerAngle1) * innerR
                        val inner2X = starCenter.x + cos(innerAngle2) * innerR
                        val inner2Y = starCenter.y + sin(innerAngle2) * innerR
                        if (i == 0) moveTo(tipX, tipY) else lineTo(tipX, tipY)
                        cubicTo(
                            starCenter.x + cos(angle - 0.25f) * outerR * 0.55f,
                            starCenter.y + sin(angle - 0.25f) * outerR * 0.55f,
                            inner1X, inner1Y,
                            inner2X, inner2Y,
                        )
                    }
                    close()
                }
                fillSeaBody(star, sea, c, radius)
                strokeSea(star, sea, lineW)

                repeat(5) { i ->
                    val angle = (i * 72f - 90f + spin) * (Math.PI / 180).toFloat()
                    val armMid = Offset(
                        starCenter.x + cos(angle) * radius * 0.28f,
                        starCenter.y + sin(angle) * radius * 0.28f,
                    )
                    repeat(3) { j ->
                        drawCircle(
                            color = sea.belly.copy(alpha = 0.55f),
                            radius = radius * (0.028f - j * 0.004f),
                            center = Offset(
                                armMid.x + cos(angle + 1.57f) * j * radius * 0.04f,
                                armMid.y + sin(angle + 1.57f) * j * radius * 0.04f,
                            ),
                        )
                    }
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sea.accent.copy(alpha = 0.5f), Color.Transparent),
                        center = starCenter,
                        radius = radius * 0.14f,
                    ),
                    radius = radius * 0.12f,
                    center = starCenter,
                )
                drawCuteEye(Offset(starCenter.x + radius * 0.06f, starCenter.y - radius * 0.04f), radius * 0.08f, sea, lineW, large = true)
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 电鳗闪击：S 形电纹 · 尾扫疾驰 */
internal object SeaEelSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_EEL

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val waveFreq = if (pose.isMoving) 4.2f else 2.4f
            val segments = 6
            val eelPath = Path()
            var prev = Offset(c.x - radius * 0.52f, c.y + swim.ripple)
            eelPath.moveTo(prev.x, prev.y)
            repeat(segments) { i ->
                val t = (i + 1f) / segments
                val next = Offset(
                    c.x - radius * 0.52f + t * radius * 1.08f,
                    c.y + sin(pose.animPhase * waveFreq + i * 0.9f) * radius * 0.18f + swim.ripple,
                )
                val ctrl = Offset(
                    (prev.x + next.x) / 2f + sin(pose.animPhase * waveFreq + i * 0.5f) * radius * 0.12f,
                    (prev.y + next.y) / 2f,
                )
                eelPath.quadraticBezierTo(ctrl.x, ctrl.y, next.x, next.y)
                prev = next
            }

            drawPath(
                eelPath,
                color = sea.stroke,
                style = Stroke(lineW * 2.2f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
            drawPath(
                eelPath,
                brush = Brush.linearGradient(
                    colors = listOf(sea.fillTop, sea.fillBottom, sea.fillTop),
                    start = Offset(c.x - radius * 0.5f, c.y),
                    end = Offset(c.x + radius * 0.55f, c.y),
                ),
                style = Stroke(lineW * 1.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )

            repeat(5) { i ->
                val t = i / 4f
                val boltX = c.x - radius * 0.3f + t * radius * 0.7f
                val boltY = c.y + sin(pose.animPhase * waveFreq + i) * radius * 0.14f + swim.ripple
                val bolt = Path().apply {
                    moveTo(boltX, boltY - radius * 0.06f)
                    lineTo(boltX + radius * 0.04f, boltY)
                    lineTo(boltX - radius * 0.02f, boltY + radius * 0.02f)
                    lineTo(boltX + radius * 0.06f, boltY + radius * 0.08f)
                }
                val flash = 0.4f + sin(pose.animPhase * 8f + i * 1.5f).coerceAtLeast(0f) * 0.6f
                drawPath(bolt, color = sea.accent.copy(alpha = flash), style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            drawFishEye(Offset(c.x + radius * 0.42f, c.y - radius * 0.06f + swim.ripple), radius, sea, lineW)
            if (pose.powerActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sea.accent.copy(alpha = 0.6f), Color.Transparent),
                        center = c,
                        radius = radius * 0.5f,
                    ),
                    radius = radius * 0.45f,
                    center = c,
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 翻车鱼：扁圆呆萌 · 侧鳍慢摆 */
internal object SeaSunfishSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_SUNFISH

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val ripple = swim.ripple
            val finSway = sin(pose.animPhase * (if (pose.isMoving) 2f else 1.2f)) * 10f

            val body = Path().apply {
                moveTo(c.x - radius * 0.08f, c.y - radius * 0.42f + ripple)
                cubicTo(
                    c.x + radius * 0.48f, c.y - radius * 0.44f,
                    c.x + radius * 0.52f, c.y + radius * 0.08f + ripple,
                    c.x + radius * 0.38f, c.y + radius * 0.38f + ripple,
                )
                cubicTo(
                    c.x + radius * 0.2f, c.y + radius * 0.48f + ripple,
                    c.x - radius * 0.2f, c.y + radius * 0.46f + ripple,
                    c.x - radius * 0.38f, c.y + radius * 0.32f + ripple,
                )
                cubicTo(
                    c.x - radius * 0.48f, c.y + radius * 0.12f,
                    c.x - radius * 0.32f, c.y - radius * 0.36f,
                    c.x - radius * 0.08f, c.y - radius * 0.42f + ripple,
                )
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW)

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(sea.belly.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(c.x + radius * 0.08f, c.y + radius * 0.08f + ripple),
                    radius = radius * 0.3f,
                ),
                topLeft = Offset(c.x - radius * 0.12f, c.y - radius * 0.08f + ripple),
                size = Size(radius * 0.44f, radius * 0.36f),
            )

            val dorsalPivot = Offset(c.x + radius * 0.04f, c.y - radius * 0.38f + ripple)
            rotate(finSway * 0.4f, pivot = dorsalPivot) {
                val dorsal = Path().apply {
                    moveTo(dorsalPivot.x, dorsalPivot.y)
                    lineTo(c.x + radius * 0.12f, c.y - radius * 0.58f)
                    lineTo(c.x + radius * 0.2f, dorsalPivot.y)
                    close()
                }
                drawPath(dorsal, color = sea.fin)
                strokeSea(dorsal, sea, lineW * 0.65f)
            }

            val tailPivot = Offset(c.x - radius * 0.32f, c.y + ripple)
            rotate(swim.tailBeatDeg * 0.5f, pivot = tailPivot) {
                val tail = Path().apply {
                    moveTo(tailPivot.x, tailPivot.y)
                    lineTo(c.x - radius * 0.48f, c.y - radius * 0.22f + ripple)
                    lineTo(c.x - radius * 0.48f, c.y + radius * 0.22f + ripple)
                    close()
                }
                drawPath(tail, color = sea.fin.copy(alpha = 0.9f))
                strokeSea(tail, sea, lineW * 0.6f)
            }

            listOf(0.12f to finSway, -0.18f to -finSway).forEach { (dy, beat) ->
                val finPivot = Offset(c.x + radius * 0.18f, c.y + radius * dy + ripple)
                rotate(beat, pivot = finPivot) {
                    val pectoral = Path().apply {
                        moveTo(finPivot.x, finPivot.y)
                        cubicTo(
                            finPivot.x + radius * 0.18f, finPivot.y + radius * 0.06f,
                            finPivot.x + radius * 0.14f, finPivot.y + radius * 0.18f,
                            finPivot.x, finPivot.y + radius * 0.12f,
                        )
                    }
                    drawPath(pectoral, color = sea.fin.copy(alpha = 0.88f), style = Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }

            drawCuteEye(Offset(c.x + radius * 0.22f, c.y - radius * 0.06f + ripple), radius * 0.1f, sea, lineW, large = true)
            drawArc(
                color = sea.stroke.copy(alpha = 0.4f),
                startAngle = 5f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.28f, c.y + radius * 0.06f + ripple),
                size = Size(radius * 0.14f, radius * 0.08f),
                style = Stroke(lineW * 0.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}
