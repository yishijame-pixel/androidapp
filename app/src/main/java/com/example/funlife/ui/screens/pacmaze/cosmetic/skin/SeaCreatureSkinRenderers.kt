package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
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
import kotlin.math.sin

/** 流线小鲨：纺锤体 + 摆尾推进 */
internal object SeaSharkSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_SHARK

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val tailPivot = Offset(c.x - radius * 0.52f, c.y + swim.ripple * 0.3f)
            rotate(swim.tailBeatDeg, pivot = tailPivot) {
                val tail = Path().apply {
                    moveTo(tailPivot.x, tailPivot.y)
                    lineTo(c.x - radius * 0.82f, c.y - radius * 0.22f + swim.ripple)
                    lineTo(c.x - radius * 0.82f, c.y + radius * 0.22f + swim.ripple)
                    close()
                }
                fillSeaBody(tail, sea, c, radius)
                strokeSea(tail, sea, lineW * 0.85f)
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.48f, c.y + swim.ripple)
                cubicTo(c.x - radius * 0.15f, c.y - radius * 0.38f + swim.ripple, c.x + radius * 0.42f, c.y - radius * 0.28f, c.x + radius * 0.62f, c.y + radius * 0.02f)
                cubicTo(c.x + radius * 0.68f, c.y + radius * 0.18f, c.x + radius * 0.38f, c.y + radius * 0.32f, c.x - radius * 0.08f, c.y + radius * 0.28f)
                cubicTo(c.x - radius * 0.32f, c.y + radius * 0.26f, c.x - radius * 0.48f, c.y + radius * 0.14f, c.x - radius * 0.48f, c.y + swim.ripple)
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW)

            val belly = Path().apply {
                moveTo(c.x - radius * 0.2f, c.y + radius * 0.06f + swim.ripple)
                cubicTo(c.x + radius * 0.08f, c.y + radius * 0.18f, c.x + radius * 0.32f, c.y + radius * 0.14f, c.x + radius * 0.38f, c.y + radius * 0.04f)
            }
            drawPath(belly, color = sea.belly.copy(alpha = 0.75f), style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val dorsal = Path().apply {
                moveTo(c.x - radius * 0.02f, c.y - radius * 0.22f + swim.ripple)
                lineTo(c.x + radius * 0.08f, c.y - radius * 0.52f)
                lineTo(c.x + radius * 0.18f, c.y - radius * 0.2f)
                close()
            }
            drawPath(dorsal, color = sea.fin)
            strokeSea(dorsal, sea, lineW * 0.7f)

            val finPivot = Offset(c.x + radius * 0.02f, c.y + radius * 0.08f + swim.ripple)
            rotate(swim.finBeatDeg, pivot = finPivot) {
                val pectoral = Path().apply {
                    moveTo(finPivot.x, finPivot.y)
                    quadraticBezierTo(c.x - radius * 0.08f, c.y + radius * 0.28f, c.x - radius * 0.22f, c.y + radius * 0.18f)
                }
                drawPath(pectoral, color = sea.fin.copy(alpha = 0.9f), style = Stroke(lineW * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            drawFishEye(Offset(c.x + radius * 0.38f, c.y - radius * 0.06f + swim.ripple), radius, sea, lineW)
            repeat(3) { i ->
                val gx = c.x + radius * (0.12f - i * 0.1f)
                drawLine(
                    sea.stroke.copy(alpha = 0.35f),
                    Offset(gx, c.y - radius * 0.02f + swim.ripple),
                    Offset(gx, c.y + radius * 0.12f + swim.ripple),
                    lineW * 0.4f,
                )
            }
            if (pose.powerActive) {
                drawCircle(color = sea.accent.copy(alpha = 0.85f), radius = radius * 0.04f, center = Offset(c.x + radius * 0.58f, c.y + radius * 0.1f))
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 小丑鱼：橙白条纹 · 珊瑚游侠 — 圆润体态 + 三白带 + 黑边鳍 */
internal object SeaClownfishSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_CLOWNFISH

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val ripple = swim.ripple

            // 尾鳍
            val tailPivot = Offset(c.x - radius * 0.34f, c.y + ripple * 0.4f)
            rotate(swim.tailBeatDeg, pivot = tailPivot) {
                val tail = Path().apply {
                    moveTo(tailPivot.x, tailPivot.y)
                    cubicTo(
                        c.x - radius * 0.58f, c.y - radius * 0.2f + ripple,
                        c.x - radius * 0.62f, c.y + ripple,
                        c.x - radius * 0.58f, c.y + radius * 0.2f + ripple,
                    )
                    close()
                }
                drawPath(tail, color = sea.fillBottom)
                drawPath(tail, color = sea.stroke.copy(alpha = 0.55f), style = Stroke(lineW * 0.7f))
                // 尾鳍黑边
                drawLine(
                    sea.stroke.copy(alpha = 0.7f),
                    Offset(c.x - radius * 0.58f, c.y - radius * 0.18f + ripple),
                    Offset(c.x - radius * 0.58f, c.y + radius * 0.18f + ripple),
                    lineW * 0.55f,
                )
            }

            // 主体 — 饱满椭圆
            val body = Path().apply {
                moveTo(c.x - radius * 0.34f, c.y + ripple)
                cubicTo(
                    c.x - radius * 0.06f, c.y - radius * 0.46f,
                    c.x + radius * 0.42f, c.y - radius * 0.4f,
                    c.x + radius * 0.48f, c.y + radius * 0.02f + ripple,
                )
                cubicTo(
                    c.x + radius * 0.46f, c.y + radius * 0.36f,
                    c.x + radius * 0.04f, c.y + radius * 0.42f,
                    c.x - radius * 0.24f, c.y + radius * 0.34f,
                )
                cubicTo(
                    c.x - radius * 0.36f, c.y + radius * 0.28f,
                    c.x - radius * 0.34f, c.y + radius * 0.1f,
                    c.x - radius * 0.34f, c.y + ripple,
                )
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW)

            // 腹部高光
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(sea.belly.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(c.x + radius * 0.08f, c.y + radius * 0.12f + ripple),
                    radius = radius * 0.28f,
                ),
                topLeft = Offset(c.x - radius * 0.08f, c.y + radius * 0.02f + ripple),
                size = androidx.compose.ui.geometry.Size(radius * 0.36f, radius * 0.22f),
            )

            // 三条白带（黑边白芯）
            listOf(0.06f, 0.2f, 0.34f).forEach { dx ->
                drawClownfishStripe(c, radius, dx, ripple, sea, lineW)
            }

            // 背鳍 ×2
            listOf(-0.14f, -0.02f).forEach { yOff ->
                val dorsal = Path().apply {
                    moveTo(c.x + radius * 0.06f, c.y + radius * yOff + ripple)
                    lineTo(c.x + radius * 0.12f, c.y + radius * (yOff - 0.18f) + ripple)
                    lineTo(c.x + radius * 0.2f, c.y + radius * yOff + ripple)
                    close()
                }
                drawPath(dorsal, color = sea.fin)
                drawPath(dorsal, color = sea.stroke.copy(alpha = 0.5f), style = Stroke(lineW * 0.45f))
            }

            // 胸鳍
            val finPivot = Offset(c.x + radius * 0.06f, c.y + radius * 0.12f + ripple)
            rotate(swim.finBeatDeg, pivot = finPivot) {
                val pectoral = Path().apply {
                    moveTo(finPivot.x, finPivot.y)
                    cubicTo(
                        c.x - radius * 0.04f, c.y + radius * 0.28f + ripple,
                        c.x - radius * 0.2f, c.y + radius * 0.22f + ripple,
                        c.x - radius * 0.22f, c.y + radius * 0.1f + ripple,
                    )
                }
                drawPath(pectoral, color = sea.fin.copy(alpha = 0.92f), style = Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(pectoral, color = sea.stroke.copy(alpha = 0.35f), style = Stroke(lineW * 0.35f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            // 腹鳍
            drawPath(
                Path().apply {
                    moveTo(c.x + radius * 0.02f, c.y + radius * 0.22f + ripple)
                    lineTo(c.x - radius * 0.06f, c.y + radius * 0.32f + ripple)
                    lineTo(c.x + radius * 0.08f, c.y + radius * 0.28f + ripple)
                    close()
                },
                color = sea.fin.copy(alpha = 0.85f),
            )

            // 眼睛 + 微笑
            drawCuteEye(Offset(c.x + radius * 0.3f, c.y - radius * 0.08f + ripple), radius * 0.1f, sea, lineW, large = true)
            drawArc(
                color = sea.stroke.copy(alpha = 0.45f),
                startAngle = 10f,
                sweepAngle = 35f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.36f, c.y + radius * 0.04f + ripple),
                size = androidx.compose.ui.geometry.Size(radius * 0.12f, radius * 0.08f),
                style = Stroke(lineW * 0.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 梦幻水母：伞体脉动 + 触须飘动 */
internal object SeaJellyfishSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_JELLYFISH

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val pulse = 1f + sin(pose.animPhase * (if (pose.isMoving) 2.4f else 1.5f)) * 0.07f
            val bellCenter = Offset(c.x + radius * 0.08f, c.y - radius * 0.08f + swim.glideY * 0.5f)

            scale(scaleX = 1f, scaleY = pulse, pivot = bellCenter) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(sea.fillTop, sea.fillBottom, sea.belly),
                        center = bellCenter,
                        radius = radius * 0.55f,
                    ),
                    topLeft = Offset(bellCenter.x - radius * 0.42f, bellCenter.y - radius * 0.34f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.84f, radius * 0.58f),
                )
                drawOval(
                    color = sea.stroke.copy(alpha = 0.55f),
                    topLeft = Offset(bellCenter.x - radius * 0.42f, bellCenter.y - radius * 0.34f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.84f, radius * 0.58f),
                    style = Stroke(lineW * 0.75f),
                )
                drawArc(
                    color = sea.detail.copy(alpha = 0.35f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(bellCenter.x - radius * 0.28f, bellCenter.y - radius * 0.12f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.56f, radius * 0.28f),
                    style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }

            repeat(5) { i ->
                val phase = pose.animPhase * (if (pose.isMoving) 2.8f else 1.6f) + i * 0.85f
                val sx = c.x - radius * 0.12f + i * radius * 0.08f
                val tentacle = Path().apply {
                    moveTo(sx, c.y + radius * 0.18f + swim.ripple)
                    cubicTo(
                        sx + sin(phase) * radius * 0.08f, c.y + radius * 0.38f,
                        sx - sin(phase * 0.8f) * radius * 0.1f, c.y + radius * 0.58f,
                        sx + sin(phase * 1.1f) * radius * 0.06f, c.y + radius * 0.78f,
                    )
                }
                drawPath(
                    tentacle,
                    color = sea.detail.copy(alpha = 0.45f + i * 0.06f),
                    style = Stroke(lineW * (0.55f - i * 0.05f), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 小章鱼：圆头八足 · 软萌潜行 — 渐变圆头 + 吸盘触须 + 大眼 */
internal object SeaOctopusSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_OCTOPUS

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val head = Offset(c.x + radius * 0.1f, c.y - radius * 0.04f + swim.ripple * 0.35f)
            val waveFreq = if (pose.isMoving) 3.2f else 1.8f

            // 八足触须（后层）
            repeat(8) { i ->
                val baseAngle = 175f + i * 22.5f
                val wave = sin(pose.animPhase * waveFreq + i * 0.85f) * (if (pose.isMoving) 16f else 10f)
                val lengthScale = 0.78f + (i % 3) * 0.06f
                rotate(baseAngle + wave, pivot = head) {
                    val tentStart = Offset(head.x, head.y + radius * 0.24f)
                    val tentEnd = Offset(head.x - radius * 0.04f, head.y + radius * 0.82f * lengthScale)
                    val tent = Path().apply {
                        moveTo(tentStart.x, tentStart.y)
                        cubicTo(
                            head.x - radius * 0.1f, head.y + radius * 0.42f,
                            head.x + radius * 0.08f * (i % 2 * 2 - 1), head.y + radius * 0.62f * lengthScale,
                            tentEnd.x, tentEnd.y,
                        )
                        cubicTo(
                            head.x + radius * 0.02f, head.y + radius * 0.72f * lengthScale,
                            head.x - radius * 0.06f, head.y + radius * 0.52f,
                            tentStart.x, tentStart.y,
                        )
                    }
                    drawPath(
                        tent,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                sea.fillTop.copy(alpha = 0.9f - i * 0.03f),
                                sea.fillBottom.copy(alpha = 0.85f - i * 0.04f),
                            ),
                            start = head,
                            end = tentEnd,
                        ),
                    )
                    drawPath(tent, color = sea.stroke.copy(alpha = 0.35f), style = Stroke(lineW * 0.55f))
                    drawTentacleSuckers(tentStart, tentEnd, sea, radius, count = 4 - i / 3, lineW = lineW)
                }
            }

            // 圆头 — 径向渐变 + 高光
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sea.fillTop, sea.fillBottom, sea.stroke.copy(alpha = 0.6f)),
                    center = Offset(head.x - radius * 0.06f, head.y - radius * 0.08f),
                    radius = radius * 0.42f,
                ),
                radius = radius * 0.38f,
                center = head,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(head.x - radius * 0.12f, head.y - radius * 0.14f),
                    radius = radius * 0.18f,
                ),
                radius = radius * 0.14f,
                center = Offset(head.x - radius * 0.1f, head.y - radius * 0.12f),
            )
            drawCircle(color = sea.stroke.copy(alpha = 0.55f), radius = radius * 0.38f, center = head, style = Stroke(lineW * 0.75f))

            // 头部纹理斑点
            listOf(
                Offset(-0.08f, -0.1f) to 0.04f,
                Offset(0.06f, -0.06f) to 0.035f,
                Offset(-0.02f, 0.08f) to 0.03f,
            ).forEach { (spot, r) ->
                drawCircle(
                    color = sea.stroke.copy(alpha = 0.15f),
                    radius = radius * r,
                    center = Offset(head.x + radius * spot.x, head.y + radius * spot.y),
                )
            }

            // 腮红
            listOf(-0.18f, 0.18f).forEach { dx ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8A80).copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(head.x + radius * dx, head.y + radius * 0.06f),
                        radius = radius * 0.1f,
                    ),
                    radius = radius * 0.08f,
                    center = Offset(head.x + radius * dx, head.y + radius * 0.06f),
                )
            }

            // 大眼
            listOf(-0.11f, 0.11f).forEach { dx ->
                drawCuteEye(
                    Offset(head.x + radius * dx, head.y - radius * 0.06f),
                    radius * 0.1f,
                    sea,
                    lineW,
                    large = true,
                )
            }

            // 小嘴
            drawArc(
                color = sea.stroke.copy(alpha = 0.5f),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(head.x - radius * 0.08f, head.y + radius * 0.06f),
                size = androidx.compose.ui.geometry.Size(radius * 0.16f, radius * 0.1f),
                style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 海龟游侠：稳壳划水 — 六边鳞甲 + 四鳍划波 + 侧眼 */
internal object SeaTurtleSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_TURTLE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val ripple = swim.ripple * 0.35f
            val shellCenter = Offset(c.x + radius * 0.02f, c.y + ripple)

            // 后鳍（先画）
            listOf(
                Offset(c.x - radius * 0.42f, c.y + radius * 0.06f + ripple) to swim.finBeatDeg,
                Offset(c.x - radius * 0.32f, c.y + radius * 0.22f + ripple) to -swim.finBeatDeg * 0.6f,
            ).forEach { (pivot, beat) ->
                rotate(beat, pivot = pivot) {
                    val flipper = Path().apply {
                        moveTo(pivot.x, pivot.y)
                        cubicTo(
                            pivot.x - radius * 0.16f, pivot.y - radius * 0.08f,
                            pivot.x - radius * 0.22f, pivot.y + radius * 0.06f,
                            pivot.x - radius * 0.1f, pivot.y + radius * 0.1f,
                        )
                        cubicTo(
                            pivot.x, pivot.y + radius * 0.06f,
                            pivot.x + radius * 0.04f, pivot.y,
                            pivot.x, pivot.y,
                        )
                    }
                    fillSeaBody(flipper, sea, c, radius)
                    strokeSea(flipper, sea, lineW * 0.55f)
                }
            }

            // 背甲 — 多层渐变
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(sea.fillTop, sea.detail.copy(alpha = 0.7f), sea.fillBottom),
                    center = shellCenter,
                    radius = radius * 0.52f,
                ),
                topLeft = Offset(shellCenter.x - radius * 0.46f, shellCenter.y - radius * 0.34f),
                size = androidx.compose.ui.geometry.Size(radius * 0.92f, radius * 0.66f),
            )
            drawTurtleScutes(shellCenter, radius, sea, lineW)
            drawOval(
                color = sea.stroke.copy(alpha = 0.6f),
                topLeft = Offset(shellCenter.x - radius * 0.46f, shellCenter.y - radius * 0.34f),
                size = androidx.compose.ui.geometry.Size(radius * 0.92f, radius * 0.66f),
                style = Stroke(lineW * 0.85f),
            )

            // 腹甲边缘
            drawArc(
                color = sea.belly.copy(alpha = 0.7f),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(shellCenter.x - radius * 0.38f, shellCenter.y + radius * 0.08f + ripple),
                size = androidx.compose.ui.geometry.Size(radius * 0.76f, radius * 0.28f),
                style = Stroke(lineW * 0.65f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            // 前鳍
            listOf(
                Offset(c.x + radius * 0.3f, c.y - radius * 0.3f + ripple) to -swim.finBeatDeg * 0.75f,
                Offset(c.x + radius * 0.34f, c.y + radius * 0.18f + ripple) to swim.finBeatDeg * 0.55f,
            ).forEach { (pivot, beat) ->
                rotate(beat, pivot = pivot) {
                    val flipper = Path().apply {
                        moveTo(pivot.x, pivot.y)
                        cubicTo(
                            pivot.x + radius * 0.18f, pivot.y - radius * 0.06f,
                            pivot.x + radius * 0.24f, pivot.y + radius * 0.08f,
                            pivot.x + radius * 0.12f, pivot.y + radius * 0.12f,
                        )
                        cubicTo(
                            pivot.x + radius * 0.02f, pivot.y + radius * 0.08f,
                            pivot.x - radius * 0.02f, pivot.y + radius * 0.02f,
                            pivot.x, pivot.y,
                        )
                    }
                    fillSeaBody(flipper, sea, c, radius)
                    strokeSea(flipper, sea, lineW * 0.55f)
                }
            }

            // 头 + 颈
            val headCenter = Offset(c.x + radius * 0.5f, c.y + radius * 0.02f + ripple)
            val neck = Path().apply {
                moveTo(c.x + radius * 0.36f, c.y + ripple)
                cubicTo(
                    c.x + radius * 0.42f, c.y - radius * 0.04f + ripple,
                    c.x + radius * 0.46f, c.y + radius * 0.02f + ripple,
                    headCenter.x - radius * 0.06f, headCenter.y,
                )
            }
            drawPath(neck, color = sea.fin, style = Stroke(lineW * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val head = Path().apply {
                moveTo(headCenter.x - radius * 0.08f, headCenter.y)
                cubicTo(
                    headCenter.x + radius * 0.04f, headCenter.y - radius * 0.1f,
                    headCenter.x + radius * 0.12f, headCenter.y - radius * 0.04f,
                    headCenter.x + radius * 0.1f, headCenter.y + radius * 0.06f,
                )
                cubicTo(
                    headCenter.x + radius * 0.06f, headCenter.y + radius * 0.1f,
                    headCenter.x - radius * 0.04f, headCenter.y + radius * 0.08f,
                    headCenter.x - radius * 0.08f, headCenter.y,
                )
            }
            fillSeaBody(head, sea, c, radius)
            strokeSea(head, sea, lineW * 0.65f)

            drawCuteEye(Offset(headCenter.x + radius * 0.04f, headCenter.y - radius * 0.02f), radius * 0.075f, sea, lineW)

            // 喙
            drawPath(
                Path().apply {
                    moveTo(headCenter.x + radius * 0.08f, headCenter.y + radius * 0.02f)
                    lineTo(headCenter.x + radius * 0.14f, headCenter.y + radius * 0.04f)
                    lineTo(headCenter.x + radius * 0.08f, headCenter.y + radius * 0.06f)
                },
                color = sea.belly,
                style = Stroke(lineW * 0.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            // 尾
            rotate(swim.tailBeatDeg * 0.45f, pivot = Offset(c.x - radius * 0.44f, c.y + ripple)) {
                drawPath(
                    Path().apply {
                        moveTo(c.x - radius * 0.44f, c.y + ripple)
                        cubicTo(
                            c.x - radius * 0.52f, c.y - radius * 0.06f + ripple,
                            c.x - radius * 0.54f, c.y + radius * 0.06f + ripple,
                            c.x - radius * 0.48f, c.y + radius * 0.04f + ripple,
                        )
                    },
                    color = sea.fin,
                    style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 魔鬼鱼：宽翼滑翔 · 深海翱翔 — 巨型翼展 + 头鳍 + 白腹斑 */
internal object SeaMantaSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_MANTA

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val bodyCenter = Offset(c.x + radius * 0.06f, c.y + swim.ripple * 0.35f)
            val wingBeat = swim.finBeatDeg * 0.5f

            // 双翼 — 宽大三角滑翔
            listOf(-1f to -0.06f, 1f to 0.06f).forEach { (side, yOff) ->
                rotate(wingBeat * side, pivot = bodyCenter) {
                    val wing = Path().apply {
                        moveTo(bodyCenter.x, bodyCenter.y + radius * yOff)
                        cubicTo(
                            bodyCenter.x - radius * 0.62f * side, bodyCenter.y - radius * 0.48f,
                            bodyCenter.x - radius * 0.92f * side, bodyCenter.y + radius * 0.04f,
                            bodyCenter.x - radius * 0.72f * side, bodyCenter.y + radius * 0.28f,
                        )
                        cubicTo(
                            bodyCenter.x - radius * 0.42f * side, bodyCenter.y + radius * 0.18f,
                            bodyCenter.x - radius * 0.12f * side, bodyCenter.y + radius * 0.06f,
                            bodyCenter.x, bodyCenter.y + radius * yOff,
                        )
                    }
                    drawPath(
                        wing,
                        brush = Brush.linearGradient(
                            colors = listOf(sea.fillTop, sea.fillBottom, sea.detail.copy(alpha = 0.5f)),
                            start = bodyCenter,
                            end = Offset(bodyCenter.x - radius * 0.8f * side, bodyCenter.y),
                        ),
                    )
                    strokeSea(wing, sea, lineW * 0.75f)

                    // 翼尖白斑
                    val tipX = bodyCenter.x - radius * 0.78f * side
                    val tipY = bodyCenter.y + radius * 0.16f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(sea.belly.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(tipX, tipY),
                            radius = radius * 0.12f,
                        ),
                        radius = radius * 0.1f,
                        center = Offset(tipX, tipY),
                    )
                }
            }

            // 躯干
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(sea.fillTop, sea.fillBottom),
                    center = bodyCenter,
                    radius = radius * 0.24f,
                ),
                topLeft = Offset(bodyCenter.x - radius * 0.22f, bodyCenter.y - radius * 0.16f),
                size = androidx.compose.ui.geometry.Size(radius * 0.44f, radius * 0.32f),
            )
            drawOval(
                color = sea.stroke.copy(alpha = 0.55f),
                topLeft = Offset(bodyCenter.x - radius * 0.22f, bodyCenter.y - radius * 0.16f),
                size = androidx.compose.ui.geometry.Size(radius * 0.44f, radius * 0.32f),
                style = Stroke(lineW * 0.65f),
            )

            // 头鳍（cephalic fins）
            listOf(-1f, 1f).forEach { side ->
                drawMantaCephalicFin(bodyCenter, radius, side, sea, lineW)
            }

            // 鳃裂
            repeat(3) { i ->
                drawArc(
                    color = sea.detail.copy(alpha = 0.45f),
                    startAngle = if (i == 1) 160f else 170f,
                    sweepAngle = 20f,
                    useCenter = false,
                    topLeft = Offset(bodyCenter.x + radius * 0.14f, bodyCenter.y - radius * 0.06f + i * radius * 0.06f),
                    size = androidx.compose.ui.geometry.Size(radius * 0.1f, radius * 0.08f),
                    style = Stroke(lineW * 0.35f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }

            // 腹面白点
            listOf(-0.1f, 0.04f, 0.16f).forEach { dy ->
                drawCircle(
                    color = sea.belly.copy(alpha = 0.75f),
                    radius = radius * 0.028f,
                    center = Offset(bodyCenter.x + radius * 0.28f, bodyCenter.y + radius * dy),
                )
            }

            drawCuteEye(Offset(bodyCenter.x + radius * 0.2f, bodyCenter.y - radius * 0.06f), radius * 0.085f, sea, lineW)

            // 尾鞭
            val tailPivot = Offset(c.x - radius * 0.38f, c.y + swim.ripple * 0.3f)
            rotate(swim.tailBeatDeg * 0.4f, pivot = tailPivot) {
                drawPath(
                    Path().apply {
                        moveTo(tailPivot.x, tailPivot.y)
                        cubicTo(
                            c.x - radius * 0.58f, c.y + sin(pose.animPhase * 2.2f) * radius * 0.06f + swim.ripple,
                            c.x - radius * 0.72f, c.y + sin(pose.animPhase * 2.8f) * radius * 0.1f + swim.ripple,
                            c.x - radius * 0.82f, c.y + sin(pose.animPhase * 3f) * radius * 0.06f + swim.ripple,
                        )
                    },
                    color = sea.stroke.copy(alpha = 0.65f),
                    style = Stroke(lineW * 0.6f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
                // 尾刺
                drawLine(
                    sea.detail.copy(alpha = 0.7f),
                    Offset(c.x - radius * 0.8f, c.y + swim.ripple),
                    Offset(c.x - radius * 0.86f, c.y + radius * 0.04f + swim.ripple),
                    lineW * 0.45f,
                )
            }
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}

/** 海马：S 形竖曲 + 尾梢轻卷 + 背鳍颤动 */
internal object SeaSeahorseSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_SEAHORSE

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val t = pose.animPhase * (if (pose.isMoving) 2.4f else 1.4f)
            val tailCurl = sin(t) * (if (pose.isMoving) 14f else 8f)
            val floatY = sin(t * 0.45f) * radius * 0.02f

            rotate(tailCurl * 0.25f, pivot = Offset(c.x - radius * 0.2f, c.y + floatY)) {
                val body = Path().apply {
                    moveTo(c.x + radius * 0.42f, c.y - radius * 0.22f + floatY)
                    cubicTo(c.x + radius * 0.18f, c.y - radius * 0.42f, c.x - radius * 0.02f, c.y - radius * 0.18f, c.x - radius * 0.12f, c.y + radius * 0.08f)
                    cubicTo(c.x - radius * 0.22f, c.y + radius * 0.32f, c.x - radius * 0.38f, c.y + radius * 0.42f, c.x - radius * 0.48f, c.y + radius * 0.28f)
                    cubicTo(c.x - radius * 0.42f, c.y + radius * 0.12f, c.x - radius * 0.28f, c.y + radius * 0.02f, c.x - radius * 0.18f, c.y + radius * 0.1f)
                }
                fillSeaBody(body, sea, c, radius)
                strokeSea(body, sea, lineW)

                repeat(4) { i ->
                    val fx = c.x - radius * 0.06f + i * radius * 0.04f
                    val flutter = sin(t + i * 0.5f) * radius * 0.04f
                    drawLine(
                        sea.fin.copy(alpha = 0.8f),
                        Offset(fx, c.y - radius * 0.2f + floatY),
                        Offset(fx + flutter, c.y - radius * 0.32f + floatY),
                        lineW * 0.5f,
                    )
                }
            }

            drawOval(
                color = sea.fillTop,
                topLeft = Offset(c.x + radius * 0.22f, c.y - radius * 0.34f + floatY),
                size = androidx.compose.ui.geometry.Size(radius * 0.32f, radius * 0.32f),
            )
            drawOval(
                color = sea.stroke.copy(alpha = 0.55f),
                topLeft = Offset(c.x + radius * 0.22f, c.y - radius * 0.34f + floatY),
                size = androidx.compose.ui.geometry.Size(radius * 0.32f, radius * 0.32f),
                style = Stroke(lineW * 0.75f),
            )
            drawFishEye(Offset(c.x + radius * 0.44f, c.y - radius * 0.14f + floatY), radius * 0.82f, sea, lineW)
            drawCircle(color = sea.detail, radius = radius * 0.03f, center = Offset(c.x + radius * 0.52f, c.y + radius * 0.02f + floatY))
        }
}

/** 海豚：流线微笑 + 尾鳍拍水 */
internal object SeaDolphinSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.SEA_DOLPHIN

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawSeaCreature(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, sea, swim ->
            val tailPivot = Offset(c.x - radius * 0.46f, c.y + swim.ripple * 0.4f)
            rotate(swim.tailBeatDeg, pivot = tailPivot) {
                val fluke = Path().apply {
                    moveTo(tailPivot.x, tailPivot.y)
                    lineTo(c.x - radius * 0.72f, c.y - radius * 0.2f + swim.ripple)
                    lineTo(c.x - radius * 0.68f, c.y + swim.ripple)
                    lineTo(c.x - radius * 0.72f, c.y + radius * 0.2f + swim.ripple)
                    close()
                }
                fillSeaBody(fluke, sea, c, radius)
                strokeSea(fluke, sea, lineW * 0.8f)
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y + swim.ripple)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.34f, c.x + radius * 0.38f, c.y - radius * 0.3f, c.x + radius * 0.58f, c.y - radius * 0.08f)
                cubicTo(c.x + radius * 0.66f, c.y + radius * 0.06f, c.x + radius * 0.48f, c.y + radius * 0.26f, c.x + radius * 0.12f, c.y + radius * 0.24f)
                cubicTo(c.x - radius * 0.14f, c.y + radius * 0.22f, c.x - radius * 0.38f, c.y + radius * 0.12f, c.x - radius * 0.42f, c.y + swim.ripple)
            }
            fillSeaBody(body, sea, c, radius)
            strokeSea(body, sea, lineW)

            val belly = Path().apply {
                moveTo(c.x - radius * 0.12f, c.y + radius * 0.04f + swim.ripple)
                cubicTo(c.x + radius * 0.18f, c.y + radius * 0.16f, c.x + radius * 0.38f, c.y + radius * 0.1f, c.x + radius * 0.42f, c.y)
            }
            drawPath(belly, color = sea.belly.copy(alpha = 0.8f), style = Stroke(lineW * 0.5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            val dorsal = Path().apply {
                moveTo(c.x + radius * 0.02f, c.y - radius * 0.18f + swim.ripple)
                lineTo(c.x + radius * 0.1f, c.y - radius * 0.42f)
                lineTo(c.x + radius * 0.18f, c.y - radius * 0.16f)
                close()
            }
            drawPath(dorsal, color = sea.fin)
            strokeSea(dorsal, sea, lineW * 0.65f)

            rotate(swim.finBeatDeg, pivot = Offset(c.x + radius * 0.06f, c.y + radius * 0.1f)) {
                drawPath(
                    Path().apply {
                        moveTo(c.x + radius * 0.06f, c.y + radius * 0.1f)
                        quadraticBezierTo(c.x - radius * 0.1f, c.y + radius * 0.28f, c.x - radius * 0.2f, c.y + radius * 0.16f)
                    },
                    color = sea.fin.copy(alpha = 0.9f),
                    style = Stroke(lineW * 0.7f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                )
            }

            drawFishEye(Offset(c.x + radius * 0.4f, c.y - radius * 0.1f + swim.ripple), radius, sea, lineW)
            drawArc(
                color = sea.stroke.copy(alpha = 0.55f),
                startAngle = 15f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.48f, c.y + radius * 0.02f),
                size = androidx.compose.ui.geometry.Size(radius * 0.14f, radius * 0.1f),
                style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
            drawCausticBubbles(c, radius, pose.animPhase)
        }
}
