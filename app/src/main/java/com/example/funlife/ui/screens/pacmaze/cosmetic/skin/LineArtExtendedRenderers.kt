package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

/** 线条企鹅：圆肚短翅 · 摇摆滑行 */
internal object LinePenguinSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_PENGUIN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val waddle = sin(pose.animPhase * (if (pose.isMoving) 4.5f else 2.2f)) * radius * 0.06f
            val wingFlap = sin(pose.animPhase * 5.8f) * 12f

            val body = Path().apply {
                moveTo(c.x - radius * 0.38f + waddle, c.y + radius * 0.42f)
                cubicTo(
                    c.x - radius * 0.08f, c.y - radius * 0.52f,
                    c.x + radius * 0.36f, c.y - radius * 0.48f,
                    c.x + radius * 0.42f + waddle, c.y + radius * 0.08f,
                )
                cubicTo(
                    c.x + radius * 0.44f, c.y + radius * 0.42f,
                    c.x + radius * 0.12f, c.y + radius * 0.56f,
                    c.x - radius * 0.06f + waddle, c.y + radius * 0.54f,
                )
                cubicTo(
                    c.x - radius * 0.28f, c.y + radius * 0.52f,
                    c.x - radius * 0.4f, c.y + radius * 0.48f,
                    c.x - radius * 0.38f + waddle, c.y + radius * 0.42f,
                )
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val belly = Path().apply {
                moveTo(c.x + radius * 0.04f, c.y - radius * 0.12f)
                cubicTo(
                    c.x + radius * 0.22f, c.y + radius * 0.08f,
                    c.x + radius * 0.18f, c.y + radius * 0.36f,
                    c.x + radius * 0.02f, c.y + radius * 0.38f,
                )
                cubicTo(
                    c.x - radius * 0.1f, c.y + radius * 0.36f,
                    c.x - radius * 0.08f, c.y + radius * 0.1f,
                    c.x + radius * 0.04f, c.y - radius * 0.12f,
                )
            }
            drawPath(belly, color = art.fillTop.copy(alpha = 0.95f))
            drawPath(belly, color = stroke.copy(alpha = 0.35f), style = Stroke(lineW * 0.4f))

            val wingPivot = Offset(c.x - radius * 0.06f, c.y + radius * 0.04f)
            rotate(wingFlap, pivot = wingPivot) {
                val wing = Path().apply {
                    moveTo(wingPivot.x, wingPivot.y)
                    cubicTo(
                        c.x - radius * 0.28f, c.y + radius * 0.02f,
                        c.x - radius * 0.34f, c.y + radius * 0.22f,
                        c.x - radius * 0.18f, c.y + radius * 0.28f,
                    )
                    cubicTo(
                        c.x - radius * 0.08f, c.y + radius * 0.22f,
                        c.x - radius * 0.02f, c.y + radius * 0.1f,
                        wingPivot.x, wingPivot.y,
                    )
                }
                drawPath(wing, color = art.fillBottom.copy(alpha = 0.9f))
                drawPath(wing, color = stroke, style = Stroke(lineW * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val beak = Path().apply {
                moveTo(c.x + radius * 0.38f, c.y + radius * 0.02f)
                lineTo(c.x + radius * 0.58f, c.y + radius * 0.08f)
                lineTo(c.x + radius * 0.38f, c.y + radius * 0.14f)
                close()
            }
            drawPath(beak, color = art.accent)
            drawPath(beak, color = stroke.copy(alpha = 0.5f), style = Stroke(lineW * 0.45f))

            LineArtSkinHelpers.run {
                drawCuteEye(Offset(c.x + radius * 0.22f, c.y - radius * 0.14f), radius, art, stroke, lineW)
                drawBlush(c, radius, art, y = 0.06f)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose, leftX = -0.12f, rightX = 0.08f)
            }

            val footSwing = sin(pose.animPhase * 6f) * radius * 0.08f
            listOf(-0.06f to footSwing, 0.1f to -footSwing).forEach { (fx, off) ->
                val foot = Offset(c.x + radius * fx + off, c.y + radius * 0.52f)
                drawOval(
                    color = art.accent,
                    topLeft = Offset(foot.x - radius * 0.08f, foot.y - radius * 0.03f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.16f, radius * 0.07f),
                )
                drawOval(
                    color = stroke.copy(alpha = 0.55f),
                    topLeft = Offset(foot.x - radius * 0.08f, foot.y - radius * 0.03f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.16f, radius * 0.07f),
                    style = Stroke(lineW * 0.4f),
                )
            }
        }
}

/** 线条猫头鹰：大圆眼 · 夜行眨眼 */
internal object LineOwlSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_OWL

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val blink = if (sin(pose.animPhase * 1.4f) > 0.92f) 0.12f else 1f
            val headTilt = sin(pose.animPhase * 2.6f) * 4f

            rotate(headTilt, pivot = c) {
                listOf(-0.22f, 0.14f).forEach { dx ->
                    val tuft = Path().apply {
                        moveTo(c.x + radius * dx, c.y - radius * 0.38f)
                        lineTo(c.x + radius * (dx - 0.06f), c.y - radius * 0.72f)
                        lineTo(c.x + radius * (dx + 0.1f), c.y - radius * 0.42f)
                        close()
                    }
                    drawPath(tuft, color = art.fillBottom)
                    drawPath(tuft, color = stroke, style = Stroke(lineW * 0.7f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }

                val body = Path().apply {
                    moveTo(c.x - radius * 0.46f, c.y + radius * 0.28f)
                    cubicTo(
                        c.x - radius * 0.1f, c.y - radius * 0.42f,
                        c.x + radius * 0.38f, c.y - radius * 0.38f,
                        c.x + radius * 0.48f, c.y + radius * 0.06f,
                    )
                    cubicTo(
                        c.x + radius * 0.44f, c.y + radius * 0.48f,
                        c.x + radius * 0.04f, c.y + radius * 0.54f,
                        c.x - radius * 0.12f, c.y + radius * 0.52f,
                    )
                    cubicTo(
                        c.x - radius * 0.38f, c.y + radius * 0.48f,
                        c.x - radius * 0.46f, c.y + radius * 0.42f,
                        c.x - radius * 0.46f, c.y + radius * 0.28f,
                    )
                }
                LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

                val chest = Path().apply {
                    moveTo(c.x + radius * 0.06f, c.y + radius * 0.04f)
                    cubicTo(
                        c.x + radius * 0.2f, c.y + radius * 0.18f,
                        c.x + radius * 0.16f, c.y + radius * 0.38f,
                        c.x + radius * 0.02f, c.y + radius * 0.36f,
                    )
                    cubicTo(
                        c.x - radius * 0.06f, c.y + radius * 0.32f,
                        c.x - radius * 0.02f, c.y + radius * 0.12f,
                        c.x + radius * 0.06f, c.y + radius * 0.04f,
                    )
                }
                drawPath(chest, color = art.fillTop.copy(alpha = 0.88f))
                drawPath(chest, color = stroke.copy(alpha = 0.3f), style = Stroke(lineW * 0.38f))

                val beak = Path().apply {
                    moveTo(c.x + radius * 0.34f, c.y + radius * 0.04f)
                    lineTo(c.x + radius * 0.5f, c.y + radius * 0.1f)
                    lineTo(c.x + radius * 0.34f, c.y + radius * 0.16f)
                    close()
                }
                drawPath(beak, color = art.accent)
                drawPath(beak, color = stroke.copy(alpha = 0.45f), style = Stroke(lineW * 0.35f))

                listOf(-0.14f, 0.14f).forEach { dx ->
                    val eyeCenter = Offset(c.x + radius * dx, c.y - radius * 0.1f)
                    val eyeR = radius * 0.14f * blink
                    drawCircle(color = Color.White.copy(alpha = 0.95f), radius = eyeR, center = eyeCenter)
                    drawCircle(color = stroke.copy(alpha = 0.3f), radius = eyeR, center = eyeCenter, style = Stroke(lineW * 0.35f))
                    if (blink > 0.5f) {
                        drawCircle(color = art.pupil, radius = radius * 0.06f, center = Offset(eyeCenter.x + radius * 0.01f, eyeCenter.y))
                        drawCircle(color = art.highlight, radius = radius * 0.024f, center = Offset(eyeCenter.x - radius * 0.02f, eyeCenter.y - radius * 0.025f))
                    } else {
                        drawLine(stroke.copy(alpha = 0.6f), Offset(eyeCenter.x - eyeR * 0.7f, eyeCenter.y), Offset(eyeCenter.x + eyeR * 0.7f, eyeCenter.y), lineW * 0.5f)
                    }
                }

                val wingSway = sin(pose.animPhase * 3.2f) * 8f
                rotate(wingSway, pivot = Offset(c.x - radius * 0.28f, c.y + radius * 0.08f)) {
                    val wing = Path().apply {
                        moveTo(c.x - radius * 0.28f, c.y + radius * 0.08f)
                        cubicTo(
                            c.x - radius * 0.48f, c.y + radius * 0.02f,
                            c.x - radius * 0.52f, c.y + radius * 0.28f,
                            c.x - radius * 0.34f, c.y + radius * 0.32f,
                        )
                        cubicTo(
                            c.x - radius * 0.24f, c.y + radius * 0.26f,
                            c.x - radius * 0.2f, c.y + radius * 0.14f,
                            c.x - radius * 0.28f, c.y + radius * 0.08f,
                        )
                    }
                    drawPath(wing, color = art.fillBottom.copy(alpha = 0.85f))
                    drawPath(wing, color = stroke, style = Stroke(lineW * 0.72f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
        }
}

/** 线条刺猬：背刺起伏 · 小短腿奔 */
internal object LineHedgehogSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_HEDGEHOG

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val quillWave = sin(pose.animPhase * (if (pose.isMoving) 5.5f else 2.8f))

            val body = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y + radius * 0.22f)
                cubicTo(
                    c.x - radius * 0.06f, c.y - radius * 0.28f,
                    c.x + radius * 0.42f, c.y - radius * 0.22f,
                    c.x + radius * 0.52f, c.y + radius * 0.06f,
                )
                cubicTo(
                    c.x + radius * 0.48f, c.y + radius * 0.38f,
                    c.x + radius * 0.08f, c.y + radius * 0.44f,
                    c.x - radius * 0.08f, c.y + radius * 0.42f,
                )
                cubicTo(
                    c.x - radius * 0.34f, c.y + radius * 0.38f,
                    c.x - radius * 0.42f, c.y + radius * 0.32f,
                    c.x - radius * 0.42f, c.y + radius * 0.22f,
                )
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            repeat(9) { i ->
                val t = i / 8f
                val baseX = c.x - radius * 0.28f + t * radius * 0.52f
                val baseY = c.y - radius * 0.18f
                val quillLen = radius * (0.22f + (i % 3) * 0.04f)
                val sway = quillWave * (6f + i * 1.5f)
                val tipX = baseX + sin(sway * 0.05f + t * 2f) * radius * 0.04f
                val tipY = baseY - quillLen + sin(sway * 0.08f) * radius * 0.03f
                drawLine(
                    art.earInner,
                    Offset(baseX, baseY),
                    Offset(tipX, tipY),
                    lineW * (0.55f + (i % 2) * 0.1f),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawCircle(color = art.accent.copy(alpha = 0.5f), radius = radius * 0.018f, center = Offset(tipX, tipY))
            }

            val snout = Path().apply {
                moveTo(c.x + radius * 0.28f, c.y + radius * 0.02f)
                cubicTo(
                    c.x + radius * 0.44f, c.y + radius * 0.04f,
                    c.x + radius * 0.48f, c.y + radius * 0.14f,
                    c.x + radius * 0.38f, c.y + radius * 0.2f,
                )
                cubicTo(
                    c.x + radius * 0.3f, c.y + radius * 0.22f,
                    c.x + radius * 0.24f, c.y + radius * 0.14f,
                    c.x + radius * 0.28f, c.y + radius * 0.02f,
                )
            }
            drawPath(snout, color = art.fillTop.copy(alpha = 0.9f))
            drawPath(snout, color = stroke.copy(alpha = 0.4f), style = Stroke(lineW * 0.42f))

            LineArtSkinHelpers.run {
                drawCuteEye(Offset(c.x + radius * 0.18f, c.y - radius * 0.04f), radius, art, stroke, lineW)
                drawCircle(color = art.nose, radius = radius * 0.035f, center = Offset(c.x + radius * 0.44f, c.y + radius * 0.1f))
                drawSmile(c, radius, stroke, lineW, open = pose.isMoving)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose, leftX = -0.14f, rightX = 0.06f)
            }
        }
}

/** 线条柴犬：卷尾吐舌 · 元气摇尾 */
internal object LineShibaSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_SHIBA

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val tailCurl = sin(pose.animPhase * 4.8f) * 18f
            val pivot = Offset(c.x - radius * 0.36f, c.y + radius * 0.02f)
            rotate(tailCurl, pivot = pivot) {
                val tail = Path().apply {
                    moveTo(pivot.x, pivot.y)
                    cubicTo(
                        pivot.x - radius * 0.22f, pivot.y - radius * 0.38f,
                        pivot.x - radius * 0.08f, pivot.y - radius * 0.58f,
                        pivot.x + radius * 0.12f, pivot.y - radius * 0.48f,
                    )
                    cubicTo(
                        pivot.x + radius * 0.22f, pivot.y - radius * 0.38f,
                        pivot.x + radius * 0.14f, pivot.y - radius * 0.18f,
                        pivot.x, pivot.y,
                    )
                }
                drawPath(tail, color = art.accent.copy(alpha = 0.7f))
                drawPath(tail, color = art.fillTop.copy(alpha = 0.8f), style = Stroke(lineW * 0.95f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(tail, color = stroke, style = Stroke(lineW * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.44f, c.y + radius * 0.18f)
                cubicTo(
                    c.x - radius * 0.04f, c.y - radius * 0.4f,
                    c.x + radius * 0.44f, c.y - radius * 0.24f,
                    c.x + radius * 0.58f, c.y + radius * 0.02f,
                )
                cubicTo(
                    c.x + radius * 0.54f, c.y + radius * 0.36f,
                    c.x + radius * 0.14f, c.y + radius * 0.44f,
                    c.x - radius * 0.06f, c.y + radius * 0.42f,
                )
                cubicTo(
                    c.x - radius * 0.32f, c.y + radius * 0.4f,
                    c.x - radius * 0.44f, c.y + radius * 0.3f,
                    c.x - radius * 0.44f, c.y + radius * 0.18f,
                )
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val chest = Path().apply {
                moveTo(c.x + radius * 0.1f, c.y + radius * 0.04f)
                cubicTo(
                    c.x + radius * 0.24f, c.y + radius * 0.16f,
                    c.x + radius * 0.2f, c.y + radius * 0.32f,
                    c.x + radius * 0.04f, c.y + radius * 0.3f,
                )
                cubicTo(
                    c.x - radius * 0.04f, c.y + radius * 0.26f,
                    c.x, c.y + radius * 0.1f,
                    c.x + radius * 0.1f, c.y + radius * 0.04f,
                )
            }
            drawPath(chest, color = Color.White.copy(alpha = 0.78f))

            listOf(-0.12f, 0.16f).forEach { dx ->
                val ear = Path().apply {
                    moveTo(c.x + radius * dx, c.y - radius * 0.22f)
                    lineTo(c.x + radius * (dx - 0.08f), c.y - radius * 0.58f)
                    lineTo(c.x + radius * (dx + 0.12f), c.y - radius * 0.3f)
                    close()
                }
                LineArtSkinHelpers.run { drawEarTriangle(ear, stroke, lineW * 0.82f, null, art.earInner) }
            }

            LineArtSkinHelpers.run {
                drawBlush(c, radius, art, y = 0.1f)
                drawCuteEye(Offset(c.x + radius * 0.28f, c.y - radius * 0.06f), radius, art, stroke, lineW)
                drawSimpleNose(c, radius, art, stroke, lineW)
            }

            val tongueWag = sin(pose.animPhase * 6.5f) * radius * 0.02f
            val tongue = Path().apply {
                moveTo(c.x + radius * 0.48f, c.y + radius * 0.12f)
                cubicTo(
                    c.x + radius * 0.56f, c.y + radius * 0.18f + tongueWag,
                    c.x + radius * 0.54f, c.y + radius * 0.28f + tongueWag,
                    c.x + radius * 0.46f, c.y + radius * 0.24f + tongueWag,
                )
            }
            drawPath(tongue, color = art.accent.copy(alpha = 0.85f), style = Stroke(lineW * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            LineArtSkinHelpers.run { drawStubbyPaws(c, radius, lineW, stroke, art, pose) }
        }
}

/** 线条水獭：抱鱼划水 · 流线摆尾 */
internal object LineOtterSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_OTTER

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val tailSway = sin(pose.animPhase * 4.2f) * 14f
            val pivot = Offset(c.x - radius * 0.48f, c.y + radius * 0.1f)
            LineArtSkinHelpers.run {
                drawFluffyTail(radius, lineW, stroke, art, tailSway, pivot, Offset(c.x - radius * 0.88f, c.y + radius * 0.14f), layers = 3)
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.5f, c.y + radius * 0.16f)
                cubicTo(
                    c.x - radius * 0.08f, c.y - radius * 0.34f,
                    c.x + radius * 0.4f, c.y - radius * 0.28f,
                    c.x + radius * 0.54f, c.y + radius * 0.04f,
                )
                cubicTo(
                    c.x + radius * 0.48f, c.y + radius * 0.38f,
                    c.x + radius * 0.06f, c.y + radius * 0.46f,
                    c.x - radius * 0.1f, c.y + radius * 0.44f,
                )
                cubicTo(
                    c.x - radius * 0.38f, c.y + radius * 0.4f,
                    c.x - radius * 0.5f, c.y + radius * 0.28f,
                    c.x - radius * 0.5f, c.y + radius * 0.16f,
                )
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val belly = Path().apply {
                moveTo(c.x + radius * 0.04f, c.y + radius * 0.06f)
                cubicTo(
                    c.x + radius * 0.2f, c.y + radius * 0.2f,
                    c.x + radius * 0.16f, c.y + radius * 0.36f,
                    c.x + radius * 0.02f, c.y + radius * 0.34f,
                )
                cubicTo(
                    c.x - radius * 0.08f, c.y + radius * 0.3f,
                    c.x - radius * 0.04f, c.y + radius * 0.14f,
                    c.x + radius * 0.04f, c.y + radius * 0.06f,
                )
            }
            drawPath(belly, color = art.fillTop.copy(alpha = 0.9f))

            val pawBob = sin(pose.animPhase * 5f) * radius * 0.05f
            val fish = Path().apply {
                moveTo(c.x + radius * 0.08f, c.y + radius * 0.18f + pawBob)
                cubicTo(
                    c.x + radius * 0.22f, c.y + radius * 0.08f + pawBob,
                    c.x + radius * 0.34f, c.y + radius * 0.14f + pawBob,
                    c.x + radius * 0.38f, c.y + radius * 0.22f + pawBob,
                )
                cubicTo(
                    c.x + radius * 0.34f, c.y + radius * 0.3f + pawBob,
                    c.x + radius * 0.2f, c.y + radius * 0.32f + pawBob,
                    c.x + radius * 0.08f, c.y + radius * 0.18f + pawBob,
                )
            }
            drawPath(fish, color = art.accent.copy(alpha = 0.85f))
            drawPath(fish, color = stroke.copy(alpha = 0.5f), style = Stroke(lineW * 0.45f))
            drawCircle(color = art.pupil, radius = radius * 0.018f, center = Offset(c.x + radius * 0.28f, c.y + radius * 0.2f + pawBob))

            listOf(-0.04f, 0.14f).forEach { dx ->
                val paw = Offset(c.x + radius * dx, c.y + radius * 0.28f + pawBob * (if (dx < 0) 1f else -0.6f))
                drawOval(
                    color = art.fillBottom.copy(alpha = 0.88f),
                    topLeft = Offset(paw.x - radius * 0.07f, paw.y - radius * 0.04f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.14f, radius * 0.09f),
                )
                drawOval(
                    color = stroke.copy(alpha = 0.55f),
                    topLeft = Offset(paw.x - radius * 0.07f, paw.y - radius * 0.04f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.14f, radius * 0.09f),
                    style = Stroke(lineW * 0.42f),
                )
            }

            val whiskerTwitch = sin(pose.animPhase * 7f) * radius * 0.01f
            listOf(0.02f, 0.1f).forEach { dy ->
                val start = Offset(c.x + radius * 0.46f, c.y + radius * dy + whiskerTwitch)
                drawLine(stroke.copy(alpha = 0.4f), start, Offset(start.x + radius * 0.18f, start.y - radius * 0.02f), lineW * 0.32f)
            }

            LineArtSkinHelpers.run {
                drawCuteEye(Offset(c.x + radius * 0.24f, c.y - radius * 0.08f), radius, art, stroke, lineW)
                drawCircle(color = art.nose, radius = radius * 0.04f, center = Offset(c.x + radius * 0.46f, c.y + radius * 0.06f))
                drawSmile(c, radius, stroke, lineW)
            }
        }
}

/** 线条考拉：抱枝慢摇 · 大耳憨眠 */
internal object LineKoalaSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_KOALA

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val sway = sin(pose.animPhase * (if (pose.isMoving) 2.8f else 1.6f)) * radius * 0.04f

            val branch = Path().apply {
                moveTo(c.x - radius * 0.62f, c.y + radius * 0.38f + sway)
                cubicTo(
                    c.x - radius * 0.2f, c.y + radius * 0.32f + sway,
                    c.x + radius * 0.1f, c.y + radius * 0.36f + sway,
                    c.x + radius * 0.48f, c.y + radius * 0.3f + sway,
                )
            }
            drawPath(branch, color = art.accent.copy(alpha = 0.75f), style = Stroke(lineW * 1.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            repeat(3) { i ->
                val leafX = c.x - radius * 0.1f + i * radius * 0.22f
                val leaf = Path().apply {
                    moveTo(leafX, c.y + radius * 0.28f + sway)
                    cubicTo(
                        leafX + radius * 0.06f, c.y + radius * 0.14f + sway,
                        leafX + radius * 0.14f, c.y + radius * 0.2f + sway,
                        leafX + radius * 0.04f, c.y + radius * 0.3f + sway,
                    )
                }
                drawPath(leaf, color = art.earInner.copy(alpha = 0.6f), style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            listOf(-0.28f, 0.2f).forEach { dx ->
                val ear = Path().apply {
                    moveTo(c.x + radius * dx, c.y - radius * 0.2f)
                    cubicTo(
                        c.x + radius * (dx - 0.14f), c.y - radius * 0.52f,
                        c.x + radius * (dx + 0.18f), c.y - radius * 0.58f,
                        c.x + radius * (dx + 0.08f), c.y - radius * 0.28f,
                    )
                    cubicTo(
                        c.x + radius * (dx + 0.04f), c.y - radius * 0.24f,
                        c.x + radius * dx, c.y - radius * 0.22f,
                        c.x + radius * dx, c.y - radius * 0.2f,
                    )
                }
                drawPath(ear, color = art.fillBottom)
                val inner = Path().apply {
                    moveTo(c.x + radius * dx, c.y - radius * 0.24f)
                    cubicTo(
                        c.x + radius * (dx - 0.06f), c.y - radius * 0.42f,
                        c.x + radius * (dx + 0.1f), c.y - radius * 0.46f,
                        c.x + radius * (dx + 0.04f), c.y - radius * 0.3f,
                    )
                    close()
                }
                drawPath(inner, color = art.earInner.copy(alpha = 0.7f))
                drawPath(ear, color = stroke, style = Stroke(lineW * 0.78f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.38f, c.y + radius * 0.24f + sway)
                cubicTo(
                    c.x - radius * 0.06f, c.y - radius * 0.36f,
                    c.x + radius * 0.32f, c.y - radius * 0.32f,
                    c.x + radius * 0.4f, c.y + radius * 0.04f + sway,
                )
                cubicTo(
                    c.x + radius * 0.38f, c.y + radius * 0.38f + sway,
                    c.x + radius * 0.02f, c.y + radius * 0.44f + sway,
                    c.x - radius * 0.1f, c.y + radius * 0.42f + sway,
                )
                cubicTo(
                    c.x - radius * 0.3f, c.y + radius * 0.38f + sway,
                    c.x - radius * 0.38f, c.y + radius * 0.32f + sway,
                    c.x - radius * 0.38f, c.y + radius * 0.24f + sway,
                )
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val nose = Path().apply {
                moveTo(c.x + radius * 0.18f, c.y + radius * 0.02f + sway)
                cubicTo(
                    c.x + radius * 0.28f, c.y + radius * 0.06f + sway,
                    c.x + radius * 0.3f, c.y + radius * 0.14f + sway,
                    c.x + radius * 0.24f, c.y + radius * 0.18f + sway,
                )
                cubicTo(
                    c.x + radius * 0.18f, c.y + radius * 0.16f + sway,
                    c.x + radius * 0.14f, c.y + radius * 0.08f + sway,
                    c.x + radius * 0.18f, c.y + radius * 0.02f + sway,
                )
            }
            drawPath(nose, color = art.nose)
            drawPath(nose, color = stroke.copy(alpha = 0.35f), style = Stroke(lineW * 0.35f))

            val armBob = sin(pose.animPhase * 3f) * radius * 0.03f
            listOf(-0.08f, 0.12f).forEach { dx ->
                val arm = Path().apply {
                    moveTo(c.x + radius * dx, c.y + radius * 0.14f + sway)
                    cubicTo(
                        c.x + radius * (dx - 0.06f), c.y + radius * 0.32f + sway + armBob,
                        c.x + radius * (dx + 0.04f), c.y + radius * 0.36f + sway,
                        c.x + radius * (dx + 0.1f), c.y + radius * 0.3f + sway,
                    )
                }
                drawPath(arm, color = art.fillBottom.copy(alpha = 0.88f), style = Stroke(lineW * 0.72f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(arm, color = stroke.copy(alpha = 0.55f), style = Stroke(lineW * 0.42f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val sleepy = !pose.isMoving && sin(pose.animPhase * 1.2f) > 0.3f
            listOf(-0.1f, 0.1f).forEach { dx ->
                val eyeY = c.y - radius * 0.06f + sway
                if (sleepy) {
                    drawArc(
                        color = stroke.copy(alpha = 0.55f),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(c.x + radius * dx - radius * 0.06f, eyeY - radius * 0.02f),
                        size = androidx.compose.ui.geometry.Size(radius * 0.12f, radius * 0.06f),
                        style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                } else {
                    LineArtSkinHelpers.run {
                        drawCuteEye(Offset(c.x + radius * dx, eyeY), radius, art, stroke, lineW)
                    }
                }
            }
            LineArtSkinHelpers.run { drawBlush(c, radius, art, y = 0.12f) }
        }
}
