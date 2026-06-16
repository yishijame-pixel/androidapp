package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.CornerRadius
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
import com.example.funlife.ui.screens.pacmaze.maptheme.CyberVisualEffects
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 墨滴小妖：圆润墨滴 + 甩须墨痕 + 留白点睛 */
internal object InkDropSpiritSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_DROP_SPIRIT

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val wobble = sin(pose.animPhase * (if (pose.isMoving) 4f else 2f)) * radius * 0.04f
            repeat(3) { i ->
                val tendril = Path().apply {
                    val base = Offset(c.x - radius * (0.18f + i * 0.08f), c.y + radius * 0.12f + motion.ripple)
                    moveTo(base.x, base.y)
                    cubicTo(
                        base.x - radius * 0.22f, base.y + radius * 0.18f + wobble,
                        base.x - radius * 0.08f + sin(pose.animPhase + i) * radius * 0.06f, base.y + radius * 0.32f,
                        base.x + radius * 0.04f, base.y + radius * 0.38f,
                    )
                }
                drawPath(tendril, color = pal.stroke.copy(alpha = 0.55f - i * 0.12f), style = Stroke(lineW * (0.85f - i * 0.15f), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.38f, c.y + radius * 0.18f + motion.ripple)
                cubicTo(c.x - radius * 0.12f, c.y - radius * 0.52f, c.x + radius * 0.42f, c.y - radius * 0.38f, c.x + radius * 0.48f, c.y + radius * 0.02f)
                cubicTo(c.x + radius * 0.44f, c.y + radius * 0.38f, c.x + radius * 0.02f, c.y + radius * 0.46f, c.x - radius * 0.22f, c.y + radius * 0.4f)
                cubicTo(c.x - radius * 0.42f, c.y + radius * 0.34f, c.x - radius * 0.38f, c.y + radius * 0.18f, c.x - radius * 0.38f, c.y + radius * 0.18f + motion.ripple)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(pal.highlight.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c.x + radius * 0.12f, c.y - radius * 0.14f),
                    radius = radius * 0.22f,
                ),
                radius = radius * 0.16f,
                center = Offset(c.x + radius * 0.1f, c.y - radius * 0.12f),
            )
            drawFamilyEye(Offset(c.x + radius * 0.22f, c.y - radius * 0.04f), radius, pal, lineW, large = true)
            drawCircle(color = pal.highlight, radius = radius * 0.035f, center = Offset(c.x + radius * 0.38f, c.y + radius * 0.08f))
        }
}

/** 剪纸雀：折纸镂空翅 + 三角尾 */
internal object InkPaperBirdSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_PAPER_BIRD

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val wingBeat = sin(pose.animPhase * (if (pose.isMoving) 5.5f else 2.5f)) * (if (pose.isMoving) 18f else 8f)
            val bodyCenter = Offset(c.x + radius * 0.04f, c.y + motion.ripple)

            listOf(-1f, 1f).forEach { side ->
                rotate(wingBeat * side, pivot = bodyCenter) {
                    val wing = Path().apply {
                        moveTo(bodyCenter.x, bodyCenter.y)
                        lineTo(bodyCenter.x - radius * 0.58f * side, bodyCenter.y - radius * 0.38f)
                        lineTo(bodyCenter.x - radius * 0.42f * side, bodyCenter.y + radius * 0.08f)
                        close()
                    }
                    drawPath(wing, color = pal.fillTop)
                    drawPath(wing, color = pal.stroke, style = Stroke(lineW * 0.75f))
                    drawLine(
                        pal.accent.copy(alpha = 0.45f),
                        bodyCenter,
                        Offset(bodyCenter.x - radius * 0.38f * side, bodyCenter.y - radius * 0.12f),
                        lineW * 0.4f,
                    )
                }
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.12f, c.y + radius * 0.22f)
                cubicTo(c.x + radius * 0.08f, c.y - radius * 0.28f, c.x + radius * 0.38f, c.y - radius * 0.12f, c.x + radius * 0.42f, c.y + radius * 0.08f)
                cubicTo(c.x + radius * 0.38f, c.y + radius * 0.28f, c.x + radius * 0.04f, c.y + radius * 0.32f, c.x - radius * 0.12f, c.y + radius * 0.22f)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW)

            val tail = Path().apply {
                moveTo(c.x - radius * 0.14f, c.y + radius * 0.16f)
                lineTo(c.x - radius * 0.38f, c.y + radius * 0.28f)
                lineTo(c.x - radius * 0.22f, c.y + radius * 0.08f)
                close()
            }
            drawPath(tail, color = pal.accent.copy(alpha = 0.75f))
            strokeFamily(tail, pal, lineW * 0.65f)

            drawCircle(color = pal.detail, radius = radius * 0.04f, center = Offset(c.x + radius * 0.32f, c.y - radius * 0.02f))
            drawLine(pal.stroke, Offset(c.x + radius * 0.36f, c.y), Offset(c.x + radius * 0.48f, c.y - radius * 0.04f), lineW * 0.55f)
        }
}

/** 舞狮头豆：绒球狮首 + 眨眼 + 摆须 */
internal object InkLionDanceSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_LION_DANCE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val head = Offset(c.x + radius * 0.06f, c.y - radius * 0.04f + motion.ripple)
            val maneWave = sin(pose.animPhase * 2.8f) * radius * 0.05f

            repeat(8) { i ->
                rotate(180f + i * 22.5f + maneWave * 8f, pivot = head) {
                    val tuft = Path().apply {
                        moveTo(head.x, head.y + radius * 0.22f)
                        cubicTo(
                            head.x - radius * 0.08f, head.y + radius * 0.42f,
                            head.x + radius * 0.06f, head.y + radius * 0.52f,
                            head.x + radius * 0.02f, head.y + radius * 0.38f,
                        )
                    }
                    drawPath(tuft, color = pal.accent.copy(alpha = 0.7f - i * 0.04f), style = Stroke(lineW * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }

            drawCircle(
                brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = head, radius = radius * 0.42f),
                radius = radius * 0.38f,
                center = head,
            )
            drawCircle(color = pal.stroke.copy(alpha = 0.55f), radius = radius * 0.38f, center = head, style = Stroke(lineW * 0.75f))

            drawCircle(color = pal.detail, radius = radius * 0.12f, center = Offset(head.x, head.y + radius * 0.14f))
            drawCircle(color = pal.highlight, radius = radius * 0.05f, center = Offset(head.x - radius * 0.04f, head.y + radius * 0.1f))

            val eyeAlpha = blinkAlpha(pose.animPhase)
            listOf(-0.12f, 0.12f).forEach { dx ->
                drawOval(
                    color = Color.White.copy(alpha = eyeAlpha),
                    topLeft = Offset(head.x + radius * dx - radius * 0.06f, head.y - radius * 0.06f),
                    size = Size(radius * 0.12f, radius * 0.14f * eyeAlpha.coerceAtLeast(0.15f)),
                )
                if (eyeAlpha > 0.5f) {
                    drawCircle(color = pal.stroke, radius = radius * 0.035f, center = Offset(head.x + radius * dx, head.y - radius * 0.02f))
                }
            }

            repeat(2) { i ->
                val whisker = Path().apply {
                    moveTo(head.x + radius * 0.28f, head.y + radius * 0.08f)
                    quadraticBezierTo(
                        head.x + radius * 0.52f, head.y + radius * (0.12f + i * 0.08f) + sin(pose.animPhase * 3f + i) * radius * 0.04f,
                        head.x + radius * 0.62f, head.y + radius * (0.06f + i * 0.12f),
                    )
                }
                drawPath(whisker, color = pal.stroke.copy(alpha = 0.6f), style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.28f, c.y + radius * 0.32f)
                cubicTo(c.x - radius * 0.04f, c.y + radius * 0.12f, c.x + radius * 0.22f, c.y + radius * 0.18f, c.x + radius * 0.24f, c.y + radius * 0.38f)
                cubicTo(c.x + radius * 0.08f, c.y + radius * 0.52f, c.x - radius * 0.18f, c.y + radius * 0.48f, c.x - radius * 0.28f, c.y + radius * 0.32f)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW * 0.85f)
        }
}

/** 瓷娃灵：青花裂纹 + 红晕瓷肌 */
internal object InkPorcelainSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_PORCELAIN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val head = Offset(c.x + radius * 0.04f, c.y - radius * 0.08f + motion.ripple)

            val body = Path().apply {
                moveTo(c.x - radius * 0.34f, c.y + radius * 0.28f)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.08f, c.x + radius * 0.28f, c.y - radius * 0.06f, c.x + radius * 0.32f, c.y + radius * 0.22f)
                cubicTo(c.x + radius * 0.28f, c.y + radius * 0.46f, c.x - radius * 0.06f, c.y + radius * 0.5f, c.x - radius * 0.34f, c.y + radius * 0.28f)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW)

            drawCircle(
                brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = head, radius = radius * 0.34f),
                radius = radius * 0.3f,
                center = head,
            )
            drawCircle(color = pal.stroke.copy(alpha = 0.45f), radius = radius * 0.3f, center = head, style = Stroke(lineW * 0.65f))

            listOf(
                Offset(-0.08f, -0.06f) to Offset(0.06f, 0.08f),
                Offset(0.04f, 0.02f) to Offset(0.14f, 0.16f),
                Offset(-0.14f, 0.1f) to Offset(-0.02f, 0.2f),
            ).forEach { (a, b) ->
                drawLine(
                    pal.accent.copy(alpha = 0.35f),
                    Offset(head.x + radius * a.x, head.y + radius * a.y),
                    Offset(head.x + radius * b.x, head.y + radius * b.y),
                    lineW * 0.35f,
                )
            }

            listOf(-0.16f, 0.04f).forEach { dx ->
                drawCircle(color = pal.blush, radius = radius * 0.09f, center = Offset(head.x + radius * dx, head.y + radius * 0.06f))
            }
            drawFamilyEye(Offset(head.x + radius * 0.1f, head.y - radius * 0.04f), radius, pal, lineW)
            drawArc(
                color = pal.detail.copy(alpha = 0.55f),
                startAngle = 15f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(head.x + radius * 0.14f, head.y + radius * 0.06f),
                size = Size(radius * 0.12f, radius * 0.08f),
                style = Stroke(lineW * 0.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            drawRoundRect(
                color = pal.accent.copy(alpha = 0.55f),
                topLeft = Offset(c.x - radius * 0.06f, c.y + radius * 0.08f),
                size = Size(radius * 0.12f, radius * 0.18f),
                cornerRadius = CornerRadius(radius * 0.04f),
            )
        }
}

/** 全息猫：半透明体 + 扫描线 */
internal object CyberHoloCatSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_HOLO_CAT

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val tailSway = sin(pose.animPhase * 4.5f) * 18f
            val pivot = Offset(c.x - radius * 0.36f, c.y + radius * 0.08f)
            rotate(tailSway, pivot = pivot) {
                drawPath(
                    Path().apply {
                        moveTo(pivot.x, pivot.y)
                        quadraticBezierTo(pivot.x - radius * 0.42f, pivot.y - radius * 0.28f, pivot.x - radius * 0.72f, pivot.y + radius * 0.06f)
                    },
                    color = pal.accent.copy(alpha = 0.55f),
                    style = Stroke(lineW * 0.9f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y + radius * 0.22f)
                cubicTo(c.x - radius * 0.06f, c.y - radius * 0.48f, c.x + radius * 0.38f, c.y - radius * 0.22f, c.x + radius * 0.5f, c.y + radius * 0.06f)
                cubicTo(c.x + radius * 0.52f, c.y + radius * 0.34f, c.x + radius * 0.16f, c.y + radius * 0.44f, c.x - radius * 0.08f, c.y + radius * 0.42f)
                cubicTo(c.x - radius * 0.32f, c.y + radius * 0.4f, c.x - radius * 0.42f, c.y + radius * 0.32f, c.x - radius * 0.42f, c.y + radius * 0.22f)
            }
            drawPath(body, brush = Brush.linearGradient(listOf(pal.fillTop, pal.fillBottom), start = Offset(c.x - radius * 0.2f, c.y - radius * 0.2f), end = Offset(c.x + radius * 0.3f, c.y + radius * 0.3f)))
            strokeFamily(body, pal, lineW * 0.75f)

            listOf(-0.1f, 0.12f).forEach { dx ->
                val ear = Path().apply {
                    moveTo(c.x + radius * dx, c.y - radius * 0.22f)
                    lineTo(c.x + radius * (dx - 0.12f), c.y - radius * 0.58f)
                    lineTo(c.x + radius * (dx + 0.1f), c.y - radius * 0.32f)
                    close()
                }
                drawPath(ear, color = pal.accent.copy(alpha = 0.5f))
                strokeFamily(ear, pal, lineW * 0.55f)
            }

            repeat(4) { i ->
                val scanY = c.y - radius * 0.32f + ((pose.animPhase * 0.35f + i * 0.22f) % 1f) * radius * 0.72f
                drawLine(pal.highlight.copy(alpha = 0.18f), Offset(c.x - radius * 0.38f, scanY), Offset(c.x + radius * 0.42f, scanY), lineW * 0.35f)
            }

            drawFamilyEye(Offset(c.x + radius * 0.24f, c.y - radius * 0.06f), radius, pal, lineW, large = true)
        }
}

/** 故障方块：像素拼合 + glitch 抖动 */
internal object CyberGlitchCubeSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_GLITCH_CUBE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val glitch = if (pose.isMoving) sin(pose.animPhase * 12f) * radius * 0.025f else 0f
            val spin = if (pose.isMoving) pose.animPhase * 18f else pose.animPhase * 6f

            rotate(spin * 0.15f, pivot = c) {
                val s = radius * 0.52f * motion.pulse
                listOf(
                    Offset(-s * 0.5f + glitch, -s * 0.5f) to pal.fillBottom,
                    Offset(s * 0.08f - glitch, -s * 0.42f) to pal.detail,
                    Offset(-s * 0.38f, s * 0.06f + glitch) to pal.accent.copy(alpha = 0.75f),
                    Offset(s * 0.12f, s * 0.18f) to pal.fillTop,
                ).forEach { (off, color) ->
                    drawRect(
                        color = color,
                        topLeft = Offset(c.x + off.x, c.y + off.y),
                        size = Size(s * 0.42f, s * 0.38f),
                    )
                }
                drawRect(
                    color = pal.stroke,
                    topLeft = Offset(c.x - s * 0.5f, c.y - s * 0.5f),
                    size = Size(s, s),
                    style = Stroke(lineW * 0.75f),
                )
            }

            if (pose.isMoving && sin(pose.animPhase * 9f) > 0.6f) {
                drawRect(
                    color = CyberVisualEffects.NeonRed.copy(alpha = 0.35f),
                    topLeft = Offset(c.x + radius * 0.08f + glitch * 2f, c.y - radius * 0.18f),
                    size = Size(radius * 0.22f, radius * 0.08f),
                )
            }

            drawRect(color = Color.White.copy(alpha = 0.85f), topLeft = Offset(c.x + radius * 0.14f - radius * 0.05f, c.y - radius * 0.05f), size = Size(radius * 0.1f, radius * 0.1f))
        }
}

/** 磁浮球：悬浮环带 + 能量核心（power 时强化光环） */
internal object CyberMaglevOrbSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_MAGLEV_ORB

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val hover = sin(pose.animPhase * 2.2f) * radius * 0.06f
            val core = Offset(c.x, c.y - hover)
            val ringSpin = if (pose.isMoving) pose.animPhase * 32f else pose.animPhase * 10f

            repeat(2) { ring ->
                rotate(ringSpin * (if (ring == 0) 1f else -0.7f), pivot = core) {
                    drawOval(
                        color = pal.accent.copy(alpha = 0.45f - ring * 0.12f),
                        topLeft = Offset(core.x - radius * (0.72f + ring * 0.12f), core.y - radius * (0.18f + ring * 0.04f)),
                        size = Size(radius * (1.44f + ring * 0.24f), radius * (0.36f + ring * 0.08f)),
                        style = Stroke(lineW * (0.55f - ring * 0.1f)),
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(pal.highlight, pal.fillTop, pal.fillBottom),
                    center = core,
                    radius = radius * 0.38f,
                ),
                radius = radius * 0.34f * motion.pulse,
                center = core,
            )
            drawCircle(color = pal.stroke.copy(alpha = 0.5f), radius = radius * 0.34f, center = core, style = Stroke(lineW * 0.65f))

            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), pal.accent.copy(alpha = 0.6f)), center = core, radius = radius * 0.14f),
                radius = radius * 0.12f,
                center = core,
            )

            if (pose.powerActive) {
                repeat(3) { i ->
                    rotate(ringSpin * 1.4f + i * 120f, pivot = core) {
                        drawLine(
                            pal.accent.copy(alpha = 0.55f),
                            core,
                            Offset(core.x + radius * 0.55f, core.y),
                            lineW * 0.45f,
                        )
                    }
                }
            }
        }
}

/** 数据线虫：线缆缠绕 + 插头尾梢 */
internal object CyberWireWormSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_WIRE_WORM

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val wave = sin(pose.animPhase * (if (pose.isMoving) 4f else 2f))
            val segments = Path().apply {
                moveTo(c.x - radius * 0.62f, c.y + radius * 0.08f + wave * radius * 0.04f)
                cubicTo(
                    c.x - radius * 0.38f, c.y - radius * 0.18f + wave * radius * 0.06f,
                    c.x - radius * 0.12f, c.y + radius * 0.22f - wave * radius * 0.05f,
                    c.x + radius * 0.18f, c.y + radius * 0.04f,
                )
                cubicTo(
                    c.x + radius * 0.34f, c.y - radius * 0.08f,
                    c.x + radius * 0.42f, c.y + radius * 0.12f,
                    c.x + radius * 0.48f, c.y + radius * 0.02f,
                )
            }
            drawPath(segments, color = pal.detail, style = Stroke(lineW * 1.15f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(segments, color = pal.stroke.copy(alpha = 0.45f), style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            drawRoundRect(
                color = pal.fillBottom,
                topLeft = Offset(c.x - radius * 0.72f, c.y - radius * 0.06f + wave * radius * 0.04f),
                size = Size(radius * 0.18f, radius * 0.22f),
                cornerRadius = CornerRadius(radius * 0.04f),
            )
            repeat(3) { pin ->
                drawRect(
                    color = pal.accent,
                    topLeft = Offset(c.x - radius * 0.7f + pin * radius * 0.05f, c.y + radius * 0.04f + wave * radius * 0.04f),
                    size = Size(radius * 0.025f, radius * 0.06f),
                )
            }

            drawCircle(color = pal.fillTop, radius = radius * 0.22f, center = Offset(c.x + radius * 0.28f, c.y - radius * 0.06f))
            drawCircle(color = pal.stroke.copy(alpha = 0.55f), radius = radius * 0.22f, center = Offset(c.x + radius * 0.28f, c.y - radius * 0.06f), style = Stroke(lineW * 0.6f))
            drawFamilyEye(Offset(c.x + radius * 0.36f, c.y - radius * 0.08f), radius, pal, lineW)
        }
}

/** 麻薯团子：软糯圆体 + 内馅微露 */
internal object FoodMochiSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_MOCHI

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val squash = 1f + if (pose.isMoving) sin(pose.animPhase * 5f) * 0.05f else sin(pose.animPhase) * 0.02f
            scale(scaleX = squash, scaleY = 2f - squash, pivot = c) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = c, radius = radius * 0.42f),
                    radius = radius * 0.4f,
                    center = c,
                )
                drawCircle(color = pal.stroke.copy(alpha = 0.25f), radius = radius * 0.4f, center = c, style = Stroke(lineW * 0.55f))
            }

            drawArc(
                color = pal.accent.copy(alpha = 0.75f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - radius * 0.08f, c.y + radius * 0.02f),
                size = Size(radius * 0.22f, radius * 0.14f),
                style = Stroke(lineW * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawFamilyEye(Offset(c.x + radius * 0.12f, c.y - radius * 0.06f), radius, pal, lineW, large = true)
            listOf(-0.12f, 0.02f).forEach { dx ->
                drawCircle(color = pal.blush, radius = radius * 0.07f, center = Offset(c.x + radius * dx, c.y + radius * 0.04f))
            }
        }
}

/** 辣椒侠：火焰眉睫 + 蹦跳 */
internal object FoodChiliSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_CHILI

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val bounce = walkOffset(motion, radius)
            val body = Path().apply {
                moveTo(c.x - radius * 0.18f, c.y + radius * 0.38f + bounce)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.42f, c.x + radius * 0.28f, c.y - radius * 0.38f, c.x + radius * 0.34f, c.y + radius * 0.08f + bounce)
                cubicTo(c.x + radius * 0.28f, c.y + radius * 0.42f, c.x - radius * 0.06f, c.y + radius * 0.48f, c.x - radius * 0.18f, c.y + radius * 0.38f + bounce)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW)

            val stem = Path().apply {
                moveTo(c.x + radius * 0.02f, c.y - radius * 0.34f + bounce)
                cubicTo(c.x + radius * 0.08f, c.y - radius * 0.52f, c.x + radius * 0.18f, c.y - radius * 0.48f, c.x + radius * 0.14f, c.y - radius * 0.36f)
            }
            drawPath(stem, color = pal.detail, style = Stroke(lineW * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            repeat(3) { i ->
                val flame = Path().apply {
                    moveTo(c.x + radius * 0.18f, c.y - radius * 0.38f + bounce)
                    lineTo(c.x + radius * (0.28f + i * 0.04f), c.y - radius * (0.58f + i * 0.06f) - abs(sin(pose.animPhase * 4f + i)) * radius * 0.06f)
                    lineTo(c.x + radius * (0.34f + i * 0.02f), c.y - radius * 0.36f + bounce)
                    close()
                }
                drawPath(flame, color = pal.accent.copy(alpha = 0.65f - i * 0.12f))
            }

            drawFamilyEye(Offset(c.x + radius * 0.18f, c.y - radius * 0.06f + bounce), radius, pal, lineW)
            drawArc(
                color = pal.stroke.copy(alpha = 0.55f),
                startAngle = 10f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.22f, c.y + radius * 0.06f + bounce),
                size = Size(radius * 0.14f, radius * 0.1f),
                style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
}

/** 寿司卷精：米粒圆卷 + 鱼生顶饰 */
internal object FoodSushiSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_SUSHI

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val roll = Offset(c.x + radius * 0.02f, c.y + radius * 0.06f + motion.ripple)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(pal.fillTop, pal.fillBottom)),
                topLeft = Offset(roll.x - radius * 0.38f, roll.y - radius * 0.28f),
                size = Size(radius * 0.76f, radius * 0.52f),
                cornerRadius = CornerRadius(radius * 0.16f),
            )
            drawRoundRect(
                color = pal.stroke.copy(alpha = 0.35f),
                topLeft = Offset(roll.x - radius * 0.38f, roll.y - radius * 0.28f),
                size = Size(radius * 0.76f, radius * 0.52f),
                cornerRadius = CornerRadius(radius * 0.16f),
                style = Stroke(lineW * 0.55f),
            )

            repeat(5) { i ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = radius * 0.028f,
                    center = Offset(roll.x - radius * 0.22f + i * radius * 0.1f, roll.y + radius * 0.06f),
                )
            }

            val fish = Path().apply {
                moveTo(roll.x - radius * 0.12f, roll.y - radius * 0.28f)
                cubicTo(roll.x + radius * 0.08f, roll.y - radius * 0.48f, roll.x + radius * 0.32f, roll.y - radius * 0.42f, roll.x + radius * 0.38f, roll.y - radius * 0.22f)
                cubicTo(roll.x + radius * 0.28f, roll.y - radius * 0.08f, roll.x + radius * 0.04f, roll.y - radius * 0.1f, roll.x - radius * 0.12f, roll.y - radius * 0.28f)
            }
            drawPath(fish, brush = Brush.linearGradient(listOf(pal.accent, pal.accent.copy(alpha = 0.7f)), start = Offset(roll.x, roll.y - radius * 0.4f), end = Offset(roll.x + radius * 0.3f, roll.y - radius * 0.1f)))
            strokeFamily(fish, pal, lineW * 0.55f)

            drawLine(pal.detail, Offset(roll.x + radius * 0.08f, roll.y - radius * 0.34f), Offset(roll.x + radius * 0.22f, roll.y - radius * 0.38f), lineW * 0.35f)
            drawFamilyEye(Offset(roll.x + radius * 0.22f, roll.y - radius * 0.08f), radius, pal, lineW)
        }
}

/** 爆米花球：蓬松米壳 + 米花蹦出 */
internal object FoodPopcornSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_POPCORN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val pop = if (pose.isMoving) sin(pose.animPhase * 7f) else sin(pose.animPhase * 2f)
            listOf(
                Offset(0f, 0f) to 0.34f,
                Offset(-0.22f, -0.12f) to 0.18f,
                Offset(0.18f, -0.18f) to 0.16f,
                Offset(-0.08f, 0.16f) to 0.15f,
                Offset(0.24f, 0.1f) to 0.14f,
            ).forEachIndexed { i, (off, rMul) ->
                val puffR = radius * rMul * (1f + if (i > 0) pop * 0.06f else 0f)
                val centerP = Offset(c.x + radius * off.x, c.y + radius * off.y + motion.ripple)
                drawCircle(
                    brush = Brush.radialGradient(listOf(pal.fillTop, pal.detail.copy(alpha = 0.85f)), center = centerP, radius = puffR),
                    radius = puffR,
                    center = centerP,
                )
                drawCircle(color = pal.stroke.copy(alpha = 0.22f), radius = puffR, center = centerP, style = Stroke(lineW * 0.35f))
            }

            if (pop > 0.4f) {
                repeat(3) { j ->
                    val angle = pose.animPhase * 2f + j * 2.1f
                    drawCircle(
                        color = pal.accent.copy(alpha = 0.65f),
                        radius = radius * 0.045f,
                        center = Offset(c.x + cos(angle) * radius * 0.42f, c.y - radius * 0.28f + sin(angle) * radius * 0.12f),
                    )
                }
            }

            drawFamilyEye(Offset(c.x + radius * 0.12f, c.y - radius * 0.04f), radius, pal, lineW, large = true)
        }
}

/** 麒麟幼灵：云纹角 + 瑞兽短蹄 */
internal object InkKylinSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_KYLIN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val head = Offset(c.x + radius * 0.08f, c.y - radius * 0.1f + motion.ripple)
            val tailSway = sin(pose.animPhase * 3f) * radius * 0.08f

            val tail = Path().apply {
                moveTo(c.x - radius * 0.28f, c.y + radius * 0.18f)
                cubicTo(c.x - radius * 0.52f + tailSway, c.y + radius * 0.08f, c.x - radius * 0.58f, c.y - radius * 0.12f, c.x - radius * 0.48f, c.y - radius * 0.22f)
            }
            drawPath(tail, color = pal.accent.copy(alpha = 0.7f), style = Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val body = Path().apply {
                moveTo(c.x - radius * 0.32f, c.y + radius * 0.32f)
                cubicTo(c.x - radius * 0.04f, c.y - radius * 0.08f, c.x + radius * 0.28f, c.y - radius * 0.06f, c.x + radius * 0.34f, c.y + radius * 0.22f)
                cubicTo(c.x + radius * 0.22f, c.y + radius * 0.48f, c.x - radius * 0.1f, c.y + radius * 0.52f, c.x - radius * 0.32f, c.y + radius * 0.32f)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW)

            drawCircle(brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = head, radius = radius * 0.32f), radius = radius * 0.28f, center = head)
            drawCircle(color = pal.stroke.copy(alpha = 0.55f), radius = radius * 0.28f, center = head, style = Stroke(lineW * 0.65f))

            listOf(-0.1f, 0.06f).forEach { dx ->
                val horn = Path().apply {
                    moveTo(head.x + radius * dx, head.y - radius * 0.18f)
                    lineTo(head.x + radius * (dx - 0.04f), head.y - radius * 0.42f)
                    lineTo(head.x + radius * (dx + 0.08f), head.y - radius * 0.2f)
                    close()
                }
                drawPath(horn, color = pal.accent)
                strokeFamily(horn, pal, lineW * 0.45f)
            }

            repeat(3) { i ->
                drawArc(
                    color = pal.detail.copy(alpha = 0.35f),
                    startAngle = 200f + i * 18f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(c.x - radius * 0.18f + i * radius * 0.08f, c.y + radius * 0.04f),
                    size = Size(radius * 0.28f, radius * 0.16f),
                    style = Stroke(lineW * 0.35f),
                )
            }

            drawFamilyEye(Offset(head.x + radius * 0.12f, head.y - radius * 0.02f), radius, pal, lineW)
            listOf(-0.14f, 0.08f).forEach { dx ->
                drawCircle(color = pal.blush, radius = radius * 0.06f, center = Offset(head.x + radius * dx, head.y + radius * 0.08f))
            }
        }
}

/** 团扇仙：开合折扇 + 飘带 */
internal object InkFanFairySkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_FAN_FAIRY

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val open = 0.55f + 0.45f * ((sin(pose.animPhase * (if (pose.isMoving) 4f else 2f)) + 1f) * 0.5f)
            val fanPivot = Offset(c.x - radius * 0.08f, c.y + radius * 0.06f + motion.ripple)

            rotate(-12f + open * 8f, pivot = fanPivot) {
                val fan = Path().apply {
                    moveTo(fanPivot.x, fanPivot.y)
                    lineTo(fanPivot.x - radius * 0.52f * open, fanPivot.y - radius * 0.38f)
                    quadraticBezierTo(fanPivot.x - radius * 0.08f, fanPivot.y - radius * 0.48f, fanPivot.x + radius * 0.48f * open, fanPivot.y - radius * 0.32f)
                    close()
                }
                drawPath(fan, brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = fanPivot, radius = radius * 0.55f))
                strokeFamily(fan, pal, lineW * 0.65f)
                repeat(5) { i ->
                    val t = i / 4f
                    drawLine(
                        pal.accent.copy(alpha = 0.45f),
                        fanPivot,
                        Offset(fanPivot.x + radius * (0.48f * open * (t * 2f - 1f)), fanPivot.y - radius * (0.32f - t * 0.08f)),
                        lineW * 0.35f,
                    )
                }
            }

            val body = Path().apply {
                moveTo(c.x + radius * 0.02f, c.y + radius * 0.28f)
                cubicTo(c.x + radius * 0.18f, c.y - radius * 0.12f, c.x + radius * 0.38f, c.y - radius * 0.08f, c.x + radius * 0.4f, c.y + radius * 0.12f)
                cubicTo(c.x + radius * 0.32f, c.y + radius * 0.32f, c.x + radius * 0.08f, c.y + radius * 0.34f, c.x + radius * 0.02f, c.y + radius * 0.28f)
            }
            fillFamilyBody(body, pal, c, radius)
            strokeFamily(body, pal, lineW * 0.75f)
            drawFamilyEye(Offset(c.x + radius * 0.28f, c.y - radius * 0.02f), radius, pal, lineW, large = true)

            val ribbon = Path().apply {
                moveTo(c.x + radius * 0.34f, c.y + radius * 0.08f)
                quadraticBezierTo(c.x + radius * 0.52f, c.y + radius * 0.22f + sin(pose.animPhase * 3f) * radius * 0.04f, c.x + radius * 0.44f, c.y + radius * 0.36f)
            }
            drawPath(ribbon, color = pal.accent.copy(alpha = 0.65f), style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
}

/** 莲蕊童：荷瓣层叠 + 莲蓬 */
internal object InkLotusBudSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_LOTUS_BUD

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val bloom = sin(pose.animPhase * (if (pose.isMoving) 3f else 1.5f)) * 6f
            repeat(6) { i ->
                rotate(i * 60f + bloom, pivot = c) {
                    val petal = Path().apply {
                        moveTo(c.x, c.y + radius * 0.08f)
                        cubicTo(c.x - radius * 0.12f, c.y - radius * 0.38f, c.x + radius * 0.18f, c.y - radius * 0.42f, c.x + radius * 0.14f, c.y + radius * 0.06f)
                        close()
                    }
                    drawPath(petal, color = pal.fillTop.copy(alpha = 0.85f - i * 0.04f))
                    strokeFamily(petal, pal, lineW * 0.45f)
                }
            }

            drawCircle(
                brush = Brush.radialGradient(listOf(pal.accent, pal.detail), center = c, radius = radius * 0.16f),
                radius = radius * 0.14f,
                center = Offset(c.x, c.y + radius * 0.02f + motion.ripple),
            )
            repeat(8) { i ->
                val angle = i * 45f + pose.animPhase * 8f
                drawCircle(
                    color = pal.detail.copy(alpha = 0.75f),
                    radius = radius * 0.025f,
                    center = Offset(c.x + cos(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.1f, c.y + radius * 0.02f + sin(Math.toRadians(angle.toDouble())).toFloat() * radius * 0.1f),
                )
            }

            drawFamilyEye(Offset(c.x + radius * 0.16f, c.y - radius * 0.06f + motion.ripple), radius, pal, lineW, large = true)
            listOf(-0.1f, 0.04f).forEach { dx ->
                drawCircle(color = pal.blush, radius = radius * 0.07f, center = Offset(c.x + radius * dx, c.y + radius * 0.06f))
            }
        }
}

/** 皮影戏偶：镂空剪影 + 提线关节 */
internal object InkShadowPuppetSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.INK_SHADOW_PUPPET

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val swing = sin(pose.animPhase * 2.5f) * radius * 0.04f
            listOf(-0.22f, 0f, 0.22f).forEach { dx ->
                drawLine(
                    pal.accent.copy(alpha = 0.45f),
                    Offset(c.x + radius * dx, c.y - radius * 0.52f),
                    Offset(c.x + radius * 0.04f + swing, c.y - radius * 0.28f),
                    lineW * 0.35f,
                )
            }

            val silhouette = Path().apply {
                moveTo(c.x - radius * 0.28f + swing, c.y + radius * 0.38f)
                lineTo(c.x - radius * 0.18f + swing, c.y - radius * 0.08f)
                lineTo(c.x + radius * 0.02f + swing, c.y - radius * 0.42f)
                lineTo(c.x + radius * 0.26f + swing, c.y - radius * 0.06f)
                lineTo(c.x + radius * 0.32f + swing, c.y + radius * 0.38f)
                cubicTo(c.x + radius * 0.08f, c.y + radius * 0.48f, c.x - radius * 0.12f, c.y + radius * 0.46f, c.x - radius * 0.28f + swing, c.y + radius * 0.38f)
            }
            drawPath(silhouette, color = pal.fillBottom.copy(alpha = 0.92f))
            strokeFamily(silhouette, pal, lineW * 0.75f)

            listOf(
                Offset(-0.12f, -0.02f) to Offset(0.08f, 0.12f),
                Offset(0.04f, 0.02f) to Offset(0.18f, 0.18f),
            ).forEach { (a, b) ->
                drawLine(
                    pal.accent.copy(alpha = 0.55f),
                    Offset(c.x + radius * a.x + swing, c.y + radius * a.y),
                    Offset(c.x + radius * b.x + swing, c.y + radius * b.y),
                    lineW * 0.35f,
                )
            }

            drawCircle(color = pal.accent, radius = radius * 0.05f, center = Offset(c.x + radius * 0.14f + swing, c.y - radius * 0.12f))
            drawCircle(color = pal.highlight.copy(alpha = 0.85f), radius = radius * 0.035f, center = Offset(c.x + radius * 0.16f + swing, c.y - radius * 0.14f))
        }
}

/** 巡逻蜂：四旋翼 + 复眼 */
internal object CyberDroneBeeSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_DRONE_BEE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val spin = if (pose.isMoving) pose.animPhase * 48f else pose.animPhase * 18f
            val hover = sin(pose.animPhase * 4f) * radius * 0.05f
            val core = Offset(c.x, c.y - hover)

            listOf(-0.38f to -0.28f, 0.38f to -0.28f, -0.38f to 0.22f, 0.38f to 0.22f).forEach { (dx, dy) ->
                rotate(spin, pivot = Offset(core.x + radius * dx, core.y + radius * dy)) {
                    drawOval(
                        color = pal.accent.copy(alpha = 0.35f),
                        topLeft = Offset(core.x + radius * dx - radius * 0.14f, core.y + radius * dy - radius * 0.04f),
                        size = Size(radius * 0.28f, radius * 0.08f),
                    )
                }
            }

            drawRoundRect(
                brush = Brush.linearGradient(listOf(pal.fillTop, pal.fillBottom)),
                topLeft = Offset(core.x - radius * 0.28f, core.y - radius * 0.18f),
                size = Size(radius * 0.56f, radius * 0.36f),
                cornerRadius = CornerRadius(radius * 0.1f),
            )
            drawRoundRect(color = pal.stroke.copy(alpha = 0.55f), topLeft = Offset(core.x - radius * 0.28f, core.y - radius * 0.18f), size = Size(radius * 0.56f, radius * 0.36f), cornerRadius = CornerRadius(radius * 0.1f), style = Stroke(lineW * 0.65f))

            repeat(3) { ring ->
                drawCircle(color = pal.detail.copy(alpha = 0.5f - ring * 0.12f), radius = radius * (0.1f - ring * 0.02f), center = Offset(core.x + radius * 0.18f, core.y - radius * 0.04f), style = Stroke(lineW * 0.4f))
            }
            drawCircle(color = pal.detail, radius = radius * 0.04f, center = Offset(core.x + radius * 0.18f, core.y - radius * 0.04f))
        }
}

/** 霓虹蛇：S 形光带 + 鳞片 */
internal object CyberNeonSnakeSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_NEON_SNAKE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val wave = sin(pose.animPhase * (if (pose.isMoving) 5f else 2.5f))
            val spine = Path().apply {
                moveTo(c.x - radius * 0.58f, c.y + radius * 0.12f + wave * radius * 0.05f)
                cubicTo(c.x - radius * 0.32f, c.y - radius * 0.22f, c.x - radius * 0.08f, c.y + radius * 0.28f, c.x + radius * 0.22f, c.y + radius * 0.04f)
                cubicTo(c.x + radius * 0.38f, c.y - radius * 0.1f, c.x + radius * 0.48f, c.y + radius * 0.08f, c.x + radius * 0.52f, c.y - radius * 0.06f)
            }
            drawPath(spine, color = pal.accent.copy(alpha = 0.55f), style = Stroke(lineW * 1.35f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(spine, color = pal.fillTop.copy(alpha = 0.75f), style = Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            repeat(6) { i ->
                val t = i / 5f
                val px = c.x - radius * 0.48f + t * radius * 0.95f
                val py = c.y + sin(pose.animPhase * 3f + i) * radius * 0.08f + wave * radius * 0.03f * (1f - t)
                drawCircle(color = pal.detail.copy(alpha = 0.65f), radius = radius * 0.045f, center = Offset(px, py))
                drawCircle(color = pal.highlight.copy(alpha = 0.45f), radius = radius * 0.018f, center = Offset(px - radius * 0.02f, py - radius * 0.02f))
            }

            val head = Offset(c.x + radius * 0.48f, c.y - radius * 0.08f + wave * radius * 0.04f)
            drawCircle(brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = head, radius = radius * 0.2f), radius = radius * 0.18f, center = head)
            drawFamilyEye(Offset(head.x + radius * 0.06f, head.y - radius * 0.02f), radius, pal, lineW)
        }
}

/** 芯片猿：电路纹路 + 数据线尾 */
internal object CyberChipMonkeySkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_CHIP_MONKEY

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val tailSway = sin(pose.animPhase * 4f) * 22f
            rotate(tailSway, pivot = Offset(c.x - radius * 0.32f, c.y + radius * 0.1f)) {
                val tail = Path().apply {
                    moveTo(c.x - radius * 0.32f, c.y + radius * 0.1f)
                    cubicTo(c.x - radius * 0.58f, c.y + radius * 0.02f, c.x - radius * 0.62f, c.y - radius * 0.22f, c.x - radius * 0.48f, c.y - radius * 0.28f)
                }
                drawPath(tail, color = pal.detail, style = Stroke(lineW * 0.9f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.34f, c.y + radius * 0.32f)
                cubicTo(c.x - radius * 0.06f, c.y - radius * 0.42f, c.x + radius * 0.34f, c.y - radius * 0.28f, c.x + radius * 0.42f, c.y + radius * 0.08f)
                cubicTo(c.x + radius * 0.38f, c.y + radius * 0.42f, c.x + radius * 0.02f, c.y + radius * 0.48f, c.x - radius * 0.34f, c.y + radius * 0.32f)
            }
            drawPath(body, brush = Brush.linearGradient(listOf(pal.fillTop, pal.fillBottom), start = Offset(c.x - radius * 0.2f, c.y - radius * 0.2f), end = Offset(c.x + radius * 0.2f, c.y + radius * 0.3f)))
            strokeFamily(body, pal, lineW * 0.75f)

            listOf(-0.14f, 0.1f).forEach { dx ->
                drawRoundRect(color = pal.accent.copy(alpha = 0.35f), topLeft = Offset(c.x + radius * dx - radius * 0.06f, c.y - radius * 0.08f), size = Size(radius * 0.12f, radius * 0.08f), cornerRadius = CornerRadius(radius * 0.02f))
            }
            drawLine(pal.detail, Offset(c.x - radius * 0.08f, c.y + radius * 0.04f), Offset(c.x + radius * 0.18f, c.y + radius * 0.12f), lineW * 0.45f)
            drawLine(pal.detail, Offset(c.x + radius * 0.18f, c.y + radius * 0.12f), Offset(c.x + radius * 0.22f, c.y - radius * 0.06f), lineW * 0.45f)

            drawCircle(color = pal.fillBottom, radius = radius * 0.24f, center = Offset(c.x + radius * 0.22f, c.y - radius * 0.12f))
            drawFamilyEye(Offset(c.x + radius * 0.3f, c.y - radius * 0.1f), radius, pal, lineW, large = true)
        }
}

/** 镭射甲虫：硬壳 + 激光触须 */
internal object CyberLaserBeetleSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.CYBER_LASER_BEETLE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val shell = Path().apply {
                moveTo(c.x - radius * 0.38f, c.y + radius * 0.22f + motion.ripple)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.48f, c.x + radius * 0.32f, c.y - radius * 0.38f, c.x + radius * 0.4f, c.y + radius * 0.06f + motion.ripple)
                cubicTo(c.x + radius * 0.28f, c.y + radius * 0.38f, c.x - radius * 0.18f, c.y + radius * 0.42f, c.x - radius * 0.38f, c.y + radius * 0.22f + motion.ripple)
            }
            drawPath(shell, brush = Brush.linearGradient(listOf(pal.fillTop, pal.fillBottom, pal.accent.copy(alpha = 0.4f)), start = Offset(c.x - radius * 0.2f, c.y - radius * 0.3f), end = Offset(c.x + radius * 0.3f, c.y + radius * 0.2f)))
            strokeFamily(shell, pal, lineW * 0.75f)
            drawLine(pal.highlight.copy(alpha = 0.45f), Offset(c.x - radius * 0.12f, c.y - radius * 0.18f), Offset(c.x + radius * 0.18f, c.y - radius * 0.08f), lineW * 0.35f)

            listOf(-0.08f, 0.08f).forEach { dx ->
                val antenna = Path().apply {
                    moveTo(c.x + radius * dx, c.y - radius * 0.28f + motion.ripple)
                    lineTo(c.x + radius * (dx + 0.12f * if (dx < 0) -1f else 1f), c.y - radius * 0.52f - abs(sin(pose.animPhase * 5f)) * radius * 0.04f)
                }
                drawPath(antenna, color = pal.accent.copy(alpha = 0.75f), style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawCircle(color = pal.accent, radius = radius * 0.035f, center = Offset(c.x + radius * (dx + 0.12f * if (dx < 0) -1f else 1f), c.y - radius * 0.52f))
            }

            drawFamilyEye(Offset(c.x + radius * 0.18f, c.y - radius * 0.02f + motion.ripple), radius, pal, lineW)
        }
}

/** 汤圆精：滚圆白团 + 芝麻馅 */
internal object FoodTangyuanSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_TANGYUAN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val wobble = sin(pose.animPhase * (if (pose.isMoving) 4.5f else 2f)) * radius * 0.03f
            drawCircle(
                brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = Offset(c.x, c.y + wobble), radius = radius * 0.42f),
                radius = radius * 0.38f,
                center = Offset(c.x, c.y + wobble),
            )
            drawCircle(color = pal.stroke.copy(alpha = 0.2f), radius = radius * 0.38f, center = Offset(c.x, c.y + wobble), style = Stroke(lineW * 0.5f))

            drawArc(
                color = pal.accent.copy(alpha = 0.55f),
                startAngle = 210f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(c.x - radius * 0.06f, c.y + radius * 0.04f + wobble),
                size = Size(radius * 0.18f, radius * 0.12f),
                style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            repeat(4) { i ->
                drawCircle(color = pal.accent.copy(alpha = 0.65f), radius = radius * 0.022f, center = Offset(c.x - radius * 0.04f + i * radius * 0.03f, c.y + radius * 0.1f + wobble))
            }
            drawFamilyEye(Offset(c.x + radius * 0.1f, c.y - radius * 0.06f + wobble), radius, pal, lineW, large = true)
            listOf(-0.1f, 0.02f).forEach { dx ->
                drawCircle(color = pal.blush, radius = radius * 0.06f, center = Offset(c.x + radius * dx, c.y + radius * 0.04f + wobble))
            }
        }
}

/** 饺子侠：褶边元宝 + 热气 */
internal object FoodDumplingSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_DUMPLING

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val bounce = walkOffset(motion, radius)
            val dumpling = Path().apply {
                moveTo(c.x - radius * 0.32f, c.y + radius * 0.28f + bounce)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.38f, c.x + radius * 0.28f, c.y - radius * 0.34f, c.x + radius * 0.34f, c.y + radius * 0.06f + bounce)
                cubicTo(c.x + radius * 0.28f, c.y + radius * 0.38f, c.x - radius * 0.06f, c.y + radius * 0.42f, c.x - radius * 0.32f, c.y + radius * 0.28f + bounce)
            }
            fillFamilyBody(dumpling, pal, c, radius)
            strokeFamily(dumpling, pal, lineW)

            repeat(5) { i ->
                val foldX = c.x - radius * 0.18f + i * radius * 0.09f
                drawLine(pal.detail.copy(alpha = 0.45f), Offset(foldX, c.y - radius * 0.22f + bounce), Offset(foldX + radius * 0.02f, c.y + radius * 0.08f + bounce), lineW * 0.35f)
            }

            if (pose.isMoving || sin(pose.animPhase * 2f) > 0.3f) {
                repeat(2) { i ->
                    val steam = Path().apply {
                        moveTo(c.x + radius * (0.08f + i * 0.12f), c.y - radius * 0.34f + bounce)
                        quadraticBezierTo(c.x + radius * (0.14f + i * 0.1f), c.y - radius * 0.48f, c.x + radius * (0.06f + i * 0.14f), c.y - radius * 0.56f)
                    }
                    drawPath(steam, color = Color.White.copy(alpha = 0.35f), style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }

            drawFamilyEye(Offset(c.x + radius * 0.16f, c.y - radius * 0.04f + bounce), radius, pal, lineW)
        }
}

/** 芒果布丁：Q 弹橙黄 + 顶饰绿叶 */
internal object FoodMangoPuddingSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_MANGO_PUDDING

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val jiggle = sin(pose.animPhase * (if (pose.isMoving) 5f else 2.5f)) * radius * 0.04f
            scale(scaleX = 1f + jiggle * 0.015f, scaleY = 1f - jiggle * 0.01f, pivot = c) {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(pal.fillTop, pal.fillBottom)),
                    topLeft = Offset(c.x - radius * 0.34f, c.y - radius * 0.22f),
                    size = Size(radius * 0.68f, radius * 0.48f),
                    cornerRadius = CornerRadius(radius * 0.14f, radius * 0.18f),
                )
                drawRoundRect(color = pal.stroke.copy(alpha = 0.28f), topLeft = Offset(c.x - radius * 0.34f, c.y - radius * 0.22f), size = Size(radius * 0.68f, radius * 0.48f), cornerRadius = CornerRadius(radius * 0.14f, radius * 0.18f), style = Stroke(lineW * 0.55f))
            }

            val leaf = Path().apply {
                moveTo(c.x + radius * 0.08f, c.y - radius * 0.22f)
                quadraticBezierTo(c.x + radius * 0.22f, c.y - radius * 0.42f, c.x + radius * 0.28f, c.y - radius * 0.24f)
                quadraticBezierTo(c.x + radius * 0.14f, c.y - radius * 0.18f, c.x + radius * 0.08f, c.y - radius * 0.22f)
            }
            drawPath(leaf, color = pal.detail)
            strokeFamily(leaf, pal, lineW * 0.45f)

            drawCircle(color = pal.highlight.copy(alpha = 0.55f), radius = radius * 0.08f, center = Offset(c.x - radius * 0.12f, c.y - radius * 0.08f))
            drawFamilyEye(Offset(c.x + radius * 0.14f, c.y - radius * 0.02f), radius, pal, lineW, large = true)
            drawCircle(color = pal.blush, radius = radius * 0.07f, center = Offset(c.x + radius * 0.02f, c.y + radius * 0.08f))
        }
}

/** 甜甜圈精：糖霜彩针 + 中空 */
internal object FoodDonutSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_DONUT

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, pal, motion ->
            val tilt = sin(pose.animPhase * 2.5f) * 4f
            rotate(tilt, pivot = c) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(pal.fillTop, pal.fillBottom), center = c, radius = radius * 0.42f),
                    radius = radius * 0.38f,
                    center = c,
                )
                drawCircle(color = Color(0xFF151D30).copy(alpha = 0.85f), radius = radius * 0.14f, center = c)
                drawCircle(color = pal.stroke.copy(alpha = 0.25f), radius = radius * 0.38f, center = c, style = Stroke(lineW * 0.55f))

                drawCircle(
                    brush = Brush.radialGradient(listOf(pal.accent.copy(alpha = 0.85f), pal.accent.copy(alpha = 0.35f)), center = Offset(c.x, c.y - radius * 0.06f), radius = radius * 0.32f),
                    radius = radius * 0.34f,
                    center = c,
                )

                listOf(
                    Color(0xFFFFEB3B),
                    Color(0xFF4FC3F7),
                    Color(0xFFFF4081),
                    Color(0xFF69F0AE),
                ).forEachIndexed { i, dotColor ->
                    val angle = pose.animPhase * 1.5f + i * 1.57f
                    drawCircle(
                        color = dotColor,
                        radius = radius * 0.025f,
                        center = Offset(c.x + cos(angle) * radius * 0.28f, c.y + sin(angle) * radius * 0.22f),
                    )
                }
            }

            drawFamilyEye(Offset(c.x + radius * 0.12f, c.y - radius * 0.04f), radius, pal, lineW, large = true)
        }
}

/** 呆脸小鸡： ikun/meme 中分鸡 — 超大贴靠白眼、中分假发、扁橙嘴 */
internal object FoodChickDazeSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.FOOD_CHICK_DAZE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawFamilySkin(scope, center, radius, pose, themeId, palette, skinId) { c, _, pal, motion ->
            val outline = (radius * 0.125f).coerceIn(3.2f, 8f)
            val hairShift = sin(pose.animPhase * (if (pose.isMoving) 2.8f else 1.4f)) * radius * 0.018f
            val partX = c.x + hairShift

            val faceR = radius * 0.42f
            val faceCenter = Offset(c.x, c.y + radius * 0.02f)

            // ① 扁平亮黄圆脸
            drawCircle(color = pal.fillTop, radius = faceR, center = faceCenter)
            drawCircle(color = pal.stroke, radius = faceR, center = faceCenter, style = Stroke(outline))

            // ② 两侧大红晕（与嘴同高）
            val cheekR = radius * 0.105f
            val cheekY = faceCenter.y + radius * 0.14f
            drawCircle(color = pal.blush, radius = cheekR, center = Offset(faceCenter.x - radius * 0.3f, cheekY))
            drawCircle(color = pal.blush, radius = cheekR, center = Offset(faceCenter.x + radius * 0.3f, cheekY))

            // ③ 超大白眼 — 中央紧贴，占脸 65%+
            val eyeR = radius * 0.305f
            val eyeY = faceCenter.y - radius * 0.04f
            val eyeGap = radius * 0.028f
            val leftEye = Offset(faceCenter.x - eyeGap, eyeY)
            val rightEye = Offset(faceCenter.x + eyeGap, eyeY)

            drawCircle(color = pal.highlight, radius = eyeR, center = leftEye)
            drawCircle(color = pal.highlight, radius = eyeR, center = rightEye)
            drawCircle(color = pal.stroke, radius = eyeR, center = leftEye, style = Stroke(outline))
            drawCircle(color = pal.stroke, radius = eyeR, center = rightEye, style = Stroke(outline))

            // ④ 眼桥连线（眼镜下缘感）
            val bridgeY = eyeY + eyeR * 0.52f
            drawLine(
                color = pal.stroke,
                start = Offset(faceCenter.x - radius * 0.06f, bridgeY),
                end = Offset(faceCenter.x + radius * 0.06f, bridgeY),
                strokeWidth = outline * 0.85f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )

            // ⑤ 小瞳孔偏左上 — 呆滞放空
            val pupilR = radius * 0.042f
            drawCircle(color = pal.stroke, radius = pupilR, center = Offset(leftEye.x - radius * 0.07f, eyeY - radius * 0.09f))
            drawCircle(color = pal.stroke, radius = pupilR, center = Offset(rightEye.x - radius * 0.05f, eyeY - radius * 0.09f))

            // ⑥ 宽扁橙嘴 — 两眼正中下方
            val beakW = radius * 0.24f
            val beakH = radius * 0.095f
            val beakTop = Offset(faceCenter.x - beakW / 2f, eyeY + eyeR * 0.38f)
            drawOval(color = pal.detail, topLeft = beakTop, size = Size(beakW, beakH))
            drawOval(color = pal.stroke, topLeft = beakTop, size = Size(beakW, beakH), style = Stroke(outline * 0.9f))

            // ⑦ 中分灰紫假发 — 两坨下垂遮眼上缘（最后绘制）
            val hairMain = Color(0xFF5C5568)
            val hairDeep = Color(0xFF3F3A4A)

            val leftWig = Path().apply {
                moveTo(partX, faceCenter.y - radius * 0.46f)
                cubicTo(
                    partX - radius * 0.04f, faceCenter.y - radius * 0.42f,
                    partX - radius * 0.34f, faceCenter.y - radius * 0.4f,
                    partX - radius * 0.4f, faceCenter.y - radius * 0.18f,
                )
                cubicTo(
                    partX - radius * 0.36f, faceCenter.y - radius * 0.02f,
                    partX - radius * 0.22f, faceCenter.y - radius * 0.06f,
                    partX - radius * 0.1f, faceCenter.y - radius * 0.14f,
                )
                cubicTo(
                    partX - radius * 0.02f, faceCenter.y - radius * 0.2f,
                    partX, faceCenter.y - radius * 0.28f,
                    partX, faceCenter.y - radius * 0.46f,
                )
                close()
            }
            val rightWig = Path().apply {
                moveTo(partX, faceCenter.y - radius * 0.46f)
                cubicTo(
                    partX + radius * 0.04f, faceCenter.y - radius * 0.42f,
                    partX + radius * 0.34f, faceCenter.y - radius * 0.4f,
                    partX + radius * 0.4f, faceCenter.y - radius * 0.18f,
                )
                cubicTo(
                    partX + radius * 0.36f, faceCenter.y - radius * 0.02f,
                    partX + radius * 0.22f, faceCenter.y - radius * 0.06f,
                    partX + radius * 0.1f, faceCenter.y - radius * 0.14f,
                )
                cubicTo(
                    partX + radius * 0.02f, faceCenter.y - radius * 0.2f,
                    partX, faceCenter.y - radius * 0.28f,
                    partX, faceCenter.y - radius * 0.46f,
                )
                close()
            }

            drawPath(leftWig, color = hairMain)
            drawPath(rightWig, color = hairDeep.copy(alpha = 0.95f))

            // 发顶蓬松体积
            val topPuff = Path().apply {
                moveTo(partX - radius * 0.22f, faceCenter.y - radius * 0.44f)
                cubicTo(
                    partX - radius * 0.18f, faceCenter.y - radius * 0.56f,
                    partX + radius * 0.18f, faceCenter.y - radius * 0.56f,
                    partX + radius * 0.22f, faceCenter.y - radius * 0.44f,
                )
                cubicTo(
                    partX + radius * 0.1f, faceCenter.y - radius * 0.5f,
                    partX - radius * 0.1f, faceCenter.y - radius * 0.5f,
                    partX - radius * 0.22f, faceCenter.y - radius * 0.44f,
                )
                close()
            }
            drawPath(topPuff, color = hairMain)

            // 中分缝 + 发丝纹理
            drawLine(
                color = hairDeep,
                start = Offset(partX, faceCenter.y - radius * 0.54f),
                end = Offset(partX, faceCenter.y - radius * 0.28f),
                strokeWidth = outline * 0.35f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            listOf(-0.18f, -0.06f, 0.08f, 0.2f).forEach { dx ->
                val strand = Path().apply {
                    moveTo(partX + radius * dx, faceCenter.y - radius * 0.48f)
                    quadraticBezierTo(
                        partX + radius * (dx * 1.15f) + hairShift,
                        faceCenter.y - radius * 0.32f,
                        partX + radius * (dx * 0.9f),
                        faceCenter.y - radius * 0.16f,
                    )
                }
                drawPath(strand, color = hairDeep.copy(alpha = 0.55f), style = Stroke(outline * 0.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            // 假发外轮廓
            drawPath(leftWig, color = pal.stroke.copy(alpha = 0.55f), style = Stroke(outline * 0.5f))
            drawPath(rightWig, color = pal.stroke.copy(alpha = 0.55f), style = Stroke(outline * 0.5f))
        }
}
