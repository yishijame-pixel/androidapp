package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

/** 线条小狗：垂耳萌犬，暖色渐变填充 */
internal object LinePuppySkinRenderer : PacMazeSkinRenderer {
    override val skinId: PacMazeSkinId = PacMazeSkinId.LINE_PUPPY

    override fun draw(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        pose: PacMazeCharacterPose,
        themeId: PacMazeMapThemeId,
        palette: PacMazeThemePalette,
    ) = drawLineArt(scope, center, radius, pose, themeId, palette, skinId) { c, lineW, stroke, art ->
        val tailWag = sin(pose.animPhase * 4.2f) * 16f
        val pivot = Offset(c.x - radius * 0.4f, c.y + radius * 0.12f)
        LineArtSkinHelpers.run {
            drawFluffyTail(radius, lineW, stroke, art, tailWag, pivot, Offset(c.x - radius * 0.82f, c.y + radius * 0.06f), layers = 3)
        }

        val body = Path().apply {
            moveTo(c.x - radius * 0.5f, c.y + radius * 0.18f)
            cubicTo(c.x - radius * 0.06f, c.y - radius * 0.46f, c.x + radius * 0.44f, c.y - radius * 0.2f, c.x + radius * 0.56f, c.y + radius * 0.04f)
            cubicTo(c.x + radius * 0.62f, c.y + radius * 0.3f, c.x + radius * 0.28f, c.y + radius * 0.44f, c.x + radius * 0.02f, c.y + radius * 0.42f)
            cubicTo(c.x - radius * 0.22f, c.y + radius * 0.4f, c.x - radius * 0.48f, c.y + radius * 0.32f, c.x - radius * 0.5f, c.y + radius * 0.18f)
        }
        LineArtSkinHelpers.run { fillBody(body, art, c, radius); strokeBody(body, stroke, lineW) }

        val earL = Path().apply {
            moveTo(c.x - radius * 0.14f, c.y - radius * 0.18f)
            cubicTo(c.x - radius * 0.38f, c.y - radius * 0.28f, c.x - radius * 0.44f, c.y - radius * 0.62f, c.x - radius * 0.18f, c.y - radius * 0.52f)
            cubicTo(c.x - radius * 0.06f, c.y - radius * 0.46f, c.x - radius * 0.02f, c.y - radius * 0.32f, c.x - radius * 0.14f, c.y - radius * 0.18f)
        }
        val earR = Path().apply {
            moveTo(c.x + radius * 0.06f, c.y - radius * 0.22f)
            cubicTo(c.x + radius * 0.18f, c.y - radius * 0.32f, c.x + radius * 0.22f, c.y - radius * 0.68f, c.x + radius * 0.1f, c.y - radius * 0.56f)
            cubicTo(c.x + radius * 0.04f, c.y - radius * 0.48f, c.x + radius * 0.02f, c.y - radius * 0.34f, c.x + radius * 0.06f, c.y - radius * 0.22f)
        }
        LineArtSkinHelpers.run {
            drawPath(earL, color = art.fillBottom.copy(alpha = 0.85f))
            drawPath(earR, color = art.fillBottom.copy(alpha = 0.85f))
            drawPath(earL, color = stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(earR, color = stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(lineW * 0.85f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawBlush(c, radius, art)
            drawCuteEye(Offset(c.x + radius * 0.26f, c.y - radius * 0.08f), radius, art, stroke, lineW)
            drawSimpleNose(c, radius, art, stroke, lineW)
            drawSmile(c, radius, stroke, lineW, open = pose.isMoving && pose.powerActive)
            if (pose.powerActive) {
                drawCircle(color = art.accent.copy(alpha = 0.7f), radius = radius * 0.035f, center = Offset(c.x + radius * 0.44f, c.y + radius * 0.16f))
            }
            drawStubbyPaws(c, radius, lineW, stroke, art, pose)
        }
    }
}
