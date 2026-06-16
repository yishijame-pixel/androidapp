package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

/** 线条小猫：圆润头身 + 粉耳 + S 形尾 */
internal object LineKittySkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_KITTY

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val tailSway = sin(pose.animPhase * 4.8f) * 20f
            val pivot = Offset(c.x - radius * 0.38f, c.y + radius * 0.06f)
            LineArtSkinHelpers.run {
                drawFluffyTail(radius, lineW, stroke, art, tailSway, pivot, Offset(c.x - radius * 0.88f, c.y + radius * 0.08f), layers = 4)
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.46f, c.y + radius * 0.22f)
                cubicTo(c.x - radius * 0.08f, c.y - radius * 0.52f, c.x + radius * 0.42f, c.y - radius * 0.18f, c.x + radius * 0.52f, c.y + radius * 0.08f)
                cubicTo(c.x + radius * 0.58f, c.y + radius * 0.32f, c.x + radius * 0.22f, c.y + radius * 0.46f, c.x - radius * 0.08f, c.y + radius * 0.44f)
                cubicTo(c.x - radius * 0.32f, c.y + radius * 0.42f, c.x - radius * 0.48f, c.y + radius * 0.34f, c.x - radius * 0.46f, c.y + radius * 0.22f)
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val earL = Path().apply {
                moveTo(c.x - radius * 0.08f, c.y - radius * 0.24f)
                lineTo(c.x - radius * 0.26f, c.y - radius * 0.78f)
                lineTo(c.x + radius * 0.06f, c.y - radius * 0.38f)
                close()
            }
            val earR = Path().apply {
                moveTo(c.x + radius * 0.1f, c.y - radius * 0.28f)
                lineTo(c.x + radius * 0.3f, c.y - radius * 0.82f)
                lineTo(c.x + radius * 0.28f, c.y - radius * 0.36f)
                close()
            }
            val innerL = Path().apply {
                moveTo(c.x - radius * 0.1f, c.y - radius * 0.3f)
                lineTo(c.x - radius * 0.2f, c.y - radius * 0.62f)
                lineTo(c.x + radius * 0.02f, c.y - radius * 0.38f)
                close()
            }
            val innerR = Path().apply {
                moveTo(c.x + radius * 0.12f, c.y - radius * 0.34f)
                lineTo(c.x + radius * 0.22f, c.y - radius * 0.64f)
                lineTo(c.x + radius * 0.24f, c.y - radius * 0.38f)
                close()
            }
            LineArtSkinHelpers.run {
                drawEarTriangle(earL, stroke, lineW * 0.88f, innerL, art.earInner)
                drawEarTriangle(earR, stroke, lineW * 0.88f, innerR, art.earInner)
                drawBlush(c, radius, art)
                drawCuteEye(Offset(c.x + radius * 0.28f, c.y - radius * 0.06f), radius, art, stroke, lineW, lookUp = true)
                drawSimpleNose(c, radius, art, stroke, lineW)
                drawWhiskers(c, radius, stroke, lineW)
                drawSmile(c, radius, stroke, lineW)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose)
            }
        }
}

/** 线条小兔：蓬松长耳 + 圆滚身体 + 绒球尾 */
internal object LineBunnySkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_BUNNY

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val earBounce = if (pose.isMoving) sin(pose.animPhase * 5.2f) * radius * 0.05f else sin(pose.animPhase * 2f) * radius * 0.015f
            val earL = Path().apply {
                moveTo(c.x - radius * 0.18f, c.y - radius * 0.18f + earBounce)
                cubicTo(c.x - radius * 0.42f, c.y - radius * 0.55f, c.x - radius * 0.38f, c.y - radius * 0.98f, c.x - radius * 0.1f, c.y - radius * 0.58f)
                cubicTo(c.x - radius * 0.02f, c.y - radius * 0.42f, c.x - radius * 0.06f, c.y - radius * 0.28f, c.x - radius * 0.18f, c.y - radius * 0.18f + earBounce)
            }
            val earR = Path().apply {
                moveTo(c.x + radius * 0.06f, c.y - radius * 0.2f - earBounce)
                cubicTo(c.x + radius * 0.28f, c.y - radius * 0.58f, c.x + radius * 0.32f, c.y - radius * 1.02f, c.x + radius * 0.14f, c.y - radius * 0.6f)
                cubicTo(c.x + radius * 0.08f, c.y - radius * 0.44f, c.x + radius * 0.04f, c.y - radius * 0.3f, c.x + radius * 0.06f, c.y - radius * 0.2f - earBounce)
            }
            LineArtSkinHelpers.run {
                drawPath(earL, color = art.fillTop)
                drawPath(earR, color = art.fillTop)
                drawPath(earL, color = stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.82f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(earR, color = stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.82f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.44f, c.y + radius * 0.16f)
                cubicTo(c.x - radius * 0.05f, c.y - radius * 0.44f, c.x + radius * 0.38f, c.y - radius * 0.38f, c.x + radius * 0.44f, c.y + radius * 0.12f)
                cubicTo(c.x + radius * 0.42f, c.y + radius * 0.46f, c.x + radius * 0.08f, c.y + radius * 0.5f, c.x - radius * 0.06f, c.y + radius * 0.48f)
                cubicTo(c.x - radius * 0.34f, c.y + radius * 0.46f, c.x - radius * 0.44f, c.y + radius * 0.32f, c.x - radius * 0.44f, c.y + radius * 0.16f)
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val tailCenter = Offset(c.x - radius * 0.52f, c.y + radius * 0.2f)
            drawCircle(color = art.fillTop, radius = radius * 0.13f, center = tailCenter)
            drawCircle(color = art.accent.copy(alpha = 0.25f), radius = radius * 0.09f, center = tailCenter)
            drawCircle(color = stroke.copy(alpha = 0.55f), radius = radius * 0.13f, center = tailCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.55f))

            LineArtSkinHelpers.run {
                drawBlush(c, radius, art, y = 0.1f)
                drawCuteEye(Offset(c.x + radius * 0.14f, c.y - radius * 0.04f), radius, art, stroke, lineW)
                drawSimpleNose(c, radius, art, stroke, lineW)
                drawSmile(c, radius, stroke, lineW, open = pose.isMoving)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose, leftX = -0.1f, rightX = 0.12f)
            }
        }
}

/** 线条熊猫：圆滚黑白配色 + 黑眼圈 */
internal object LinePandaSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_PANDA

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val body = Path().apply {
                moveTo(c.x - radius * 0.52f, c.y + radius * 0.14f)
                cubicTo(c.x - radius * 0.06f, c.y - radius * 0.5f, c.x + radius * 0.48f, c.y - radius * 0.42f, c.x + radius * 0.54f, c.y + radius * 0.1f)
                cubicTo(c.x + radius * 0.48f, c.y + radius * 0.48f, c.x + radius * 0.06f, c.y + radius * 0.54f, c.x - radius * 0.06f, c.y + radius * 0.52f)
                cubicTo(c.x - radius * 0.42f, c.y + radius * 0.5f, c.x - radius * 0.52f, c.y + radius * 0.34f, c.x - radius * 0.52f, c.y + radius * 0.14f)
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            listOf(
                Offset(c.x - radius * 0.26f, c.y - radius * 0.38f),
                Offset(c.x + radius * 0.26f, c.y - radius * 0.38f),
            ).forEach { ear ->
                drawCircle(color = Color(0xFF263238), radius = radius * 0.14f, center = ear)
                drawCircle(color = stroke.copy(alpha = 0.4f), radius = radius * 0.14f, center = ear, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.4f))
            }

            listOf(
                Offset(c.x - radius * 0.14f, c.y - radius * 0.08f),
                Offset(c.x + radius * 0.14f, c.y - radius * 0.08f),
            ).forEach { patch ->
                LineArtSkinHelpers.run { drawPatch(patch, radius * 0.13f, Color(0xFF263238)) }
            }

            val muzzle = Path().apply {
                moveTo(c.x + radius * 0.22f, c.y + radius * 0.02f)
                cubicTo(c.x + radius * 0.34f, c.y + radius * 0.08f, c.x + radius * 0.38f, c.y + radius * 0.18f, c.x + radius * 0.32f, c.y + radius * 0.24f)
                cubicTo(c.x + radius * 0.26f, c.y + radius * 0.28f, c.x + radius * 0.18f, c.y + radius * 0.22f, c.x + radius * 0.22f, c.y + radius * 0.02f)
            }
            drawPath(muzzle, color = art.fillTop.copy(alpha = 0.92f))
            drawPath(muzzle, color = stroke.copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.45f))

            LineArtSkinHelpers.run {
                drawCircle(color = art.pupil, radius = radius * 0.028f, center = Offset(c.x - radius * 0.14f, c.y - radius * 0.06f))
                drawCircle(color = art.pupil, radius = radius * 0.028f, center = Offset(c.x + radius * 0.14f, c.y - radius * 0.06f))
                drawCircle(color = art.highlight, radius = radius * 0.012f, center = Offset(c.x - radius * 0.13f, c.y - radius * 0.07f))
                drawCircle(color = art.highlight, radius = radius * 0.012f, center = Offset(c.x + radius * 0.15f, c.y - radius * 0.07f))
                drawCircle(color = art.nose, radius = radius * 0.04f, center = Offset(c.x + radius * 0.34f, c.y + radius * 0.1f))
                drawSmile(c, radius, stroke, lineW)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose, leftX = -0.16f, rightX = 0.14f)
            }
        }
}

/** 线条小狐：尖吻 + 白胸 + 大蓬松尾 */
internal object LineFoxSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_FOX

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            val tailFluff = sin(pose.animPhase * 3.6f) * 14f
            val pivot = Offset(c.x - radius * 0.42f, c.y + radius * 0.04f)
            rotate(tailFluff, pivot = pivot) {
                val tail = Path().apply {
                    moveTo(pivot.x, pivot.y)
                    cubicTo(pivot.x - radius * 0.45f, pivot.y - radius * 0.42f, pivot.x - radius * 0.72f, pivot.y + radius * 0.08f, pivot.x - radius * 0.68f, pivot.y + radius * 0.32f)
                    cubicTo(pivot.x - radius * 0.58f, pivot.y + radius * 0.48f, pivot.x - radius * 0.46f, pivot.y + radius * 0.28f, pivot.x, pivot.y)
                }
                drawPath(tail, color = art.accent.copy(alpha = 0.55f))
                drawPath(tail, color = art.fillTop.copy(alpha = 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 1.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawPath(tail, color = stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                drawCircle(color = art.fillTop, radius = radius * 0.06f, center = Offset(pivot.x - radius * 0.66f, pivot.y + radius * 0.3f))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.42f, c.y + radius * 0.16f)
                cubicTo(c.x - radius * 0.04f, c.y - radius * 0.36f, c.x + radius * 0.48f, c.y - radius * 0.22f, c.x + radius * 0.66f, c.y - radius * 0.02f)
                lineTo(c.x + radius * 0.78f, c.y + radius * 0.06f)
                cubicTo(c.x + radius * 0.52f, c.y + radius * 0.38f, c.x + radius * 0.12f, c.y + radius * 0.44f, c.x - radius * 0.42f, c.y + radius * 0.16f)
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

            val chest = Path().apply {
                moveTo(c.x + radius * 0.08f, c.y + radius * 0.06f)
                cubicTo(c.x + radius * 0.22f, c.y + radius * 0.18f, c.x + radius * 0.18f, c.y + radius * 0.34f, c.x + radius * 0.02f, c.y + radius * 0.32f)
                cubicTo(c.x - radius * 0.08f, c.y + radius * 0.28f, c.x - radius * 0.04f, c.y + radius * 0.12f, c.x + radius * 0.08f, c.y + radius * 0.06f)
            }
            drawPath(chest, color = Color.White.copy(alpha = 0.75f))

            val ear = Path().apply {
                moveTo(c.x - radius * 0.02f, c.y - radius * 0.22f)
                lineTo(c.x - radius * 0.18f, c.y - radius * 0.68f)
                lineTo(c.x + radius * 0.1f, c.y - radius * 0.34f)
                close()
            }
            LineArtSkinHelpers.run {
                drawEarTriangle(ear, stroke, lineW * 0.85f, null, art.earInner)
                drawBlush(c, radius, art, y = 0.06f)
                drawCuteEye(Offset(c.x + radius * 0.32f, c.y - radius * 0.04f), radius, art, stroke, lineW)
                drawSimpleNose(c, radius, art, stroke, lineW)
                drawSmile(c, radius, stroke, lineW)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose)
            }
        }
}

/** 线条小熊：圆耳 + 宽脸 + 憨态 */
internal object LineBearSkinRenderer : PacMazeSkinRenderer {
    override val skinId = PacMazeSkinId.LINE_BEAR

    override fun draw(scope: DrawScope, center: Offset, radius: Float, pose: PacMazeCharacterPose, themeId: PacMazeMapThemeId, palette: PacMazeThemePalette) =
        drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
            listOf(
                Offset(c.x - radius * 0.28f, c.y - radius * 0.44f) to -1f,
                Offset(c.x + radius * 0.18f, c.y - radius * 0.46f) to 1f,
            ).forEach { (earCenter, _) ->
                drawCircle(color = art.fillBottom, radius = radius * 0.16f, center = earCenter)
                drawCircle(color = art.earInner.copy(alpha = 0.55f), radius = radius * 0.09f, center = earCenter)
                drawCircle(color = stroke, radius = radius * 0.16f, center = earCenter, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.75f))
            }

            val body = Path().apply {
                moveTo(c.x - radius * 0.56f, c.y + radius * 0.12f)
                cubicTo(c.x - radius * 0.12f, c.y - radius * 0.48f, c.x + radius * 0.42f, c.y - radius * 0.44f, c.x + radius * 0.52f, c.y + radius * 0.06f)
                cubicTo(c.x + radius * 0.48f, c.y + radius * 0.52f, c.x + radius * 0.04f, c.y + radius * 0.56f, c.x - radius * 0.08f, c.y + radius * 0.54f)
                cubicTo(c.x - radius * 0.44f, c.y + radius * 0.52f, c.x - radius * 0.56f, c.y + radius * 0.32f, c.x - radius * 0.56f, c.y + radius * 0.12f)
            }
            LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW * 1.02f) }

            val snout = Path().apply {
                moveTo(c.x + radius * 0.18f, c.y + radius * 0.04f)
                cubicTo(c.x + radius * 0.34f, c.y + radius * 0.06f, c.x + radius * 0.42f, c.y + radius * 0.16f, c.x + radius * 0.36f, c.y + radius * 0.24f)
                cubicTo(c.x + radius * 0.28f, c.y + radius * 0.28f, c.x + radius * 0.18f, c.y + radius * 0.22f, c.x + radius * 0.18f, c.y + radius * 0.04f)
            }
            drawPath(snout, color = art.fillTop.copy(alpha = 0.88f))
            drawPath(snout, color = stroke.copy(alpha = 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.45f))

            LineArtSkinHelpers.run {
                drawBlush(c, radius, art, y = 0.12f)
                drawCuteEye(Offset(c.x + radius * 0.12f, c.y - radius * 0.02f), radius, art, stroke, lineW)
                drawCircle(color = art.nose, radius = radius * 0.055f, center = Offset(c.x + radius * 0.34f, c.y + radius * 0.12f))
                drawSmile(c, radius, stroke, lineW)
                drawStubbyPaws(c, radius, lineW, stroke, art, pose, leftX = -0.18f, rightX = 0.1f)
            }
        }
}

internal inline fun drawLineArt(
    scope: DrawScope,
    center: Offset,
    radius: Float,
    pose: PacMazeCharacterPose,
    themeId: PacMazeMapThemeId,
    themePalette: PacMazeThemePalette,
    skinId: PacMazeSkinId,
    crossinline drawBody: DrawScope.(c: Offset, lineW: Float, strokeColor: Color, art: LineArtPalette) -> Unit,
) {
    val stroke = LineArtSkinHelpers.strokeColor(themeId)
    val art = LineArtSkinHelpers.paletteFor(skinId, pose, themePalette)
    val lineW = LineArtSkinHelpers.lineWidth(radius)
    val bob = LineArtSkinHelpers.bob(pose, radius)

    LineArtSkinHelpers.drawShadow(scope, center, radius)
    LineArtSkinHelpers.drawPowerAura(scope, center, radius, art.accent, pose.powerActive)

    LineArtSkinHelpers.withFacing(scope, center, pose.facing) {
        drawBody(Offset(center.x, center.y + bob), lineW, stroke, art)
    }
}
