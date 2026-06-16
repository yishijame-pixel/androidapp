package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.cos
import kotlin.math.sin

internal data class LineArtPalette(
    val fillTop: Color,
    val fillBottom: Color,
    val blush: Color,
    val earInner: Color,
    val nose: Color,
    val accent: Color,
    val pupil: Color = Color(0xFF37474F),
    val highlight: Color = Color(0xFFFFFFFF),
)

internal object LineArtSkinHelpers {

    fun paletteFor(skinId: PacMazeSkinId, pose: PacMazeCharacterPose, themePalette: PacMazeThemePalette): LineArtPalette {
        val power = pose.powerActive
        return when (skinId) {
            PacMazeSkinId.LINE_PUPPY -> LineArtPalette(
                fillTop = Color(0xFFFFF8E7),
                fillBottom = Color(0xFFFFECB3),
                blush = Color(0xFFFFAB91).copy(alpha = 0.45f),
                earInner = Color(0xFFFFCCBC),
                nose = Color(0xFF8D6E63),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFB74D),
            )
            PacMazeSkinId.LINE_KITTY -> LineArtPalette(
                fillTop = Color(0xFFFAFAFA),
                fillBottom = Color(0xFFF5F5F5),
                blush = Color(0xFFF48FB1).copy(alpha = 0.42f),
                earInner = Color(0xFFF8BBD0),
                nose = Color(0xFFEF9A9A),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFCE93D8),
            )
            PacMazeSkinId.LINE_BUNNY -> LineArtPalette(
                fillTop = Color(0xFFFFFDF7),
                fillBottom = Color(0xFFFFF0F5),
                blush = Color(0xFFF8BBD0).copy(alpha = 0.5f),
                earInner = Color(0xFFFCE4EC),
                nose = Color(0xFFF48FB1),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFF48FB1),
            )
            PacMazeSkinId.LINE_PANDA -> LineArtPalette(
                fillTop = Color(0xFFFAFAFA),
                fillBottom = Color(0xFFECEFF1),
                blush = Color(0xFFE0E0E0).copy(alpha = 0.35f),
                earInner = Color(0xFF424242),
                nose = Color(0xFF37474F),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF78909C),
            )
            PacMazeSkinId.LINE_FOX -> LineArtPalette(
                fillTop = Color(0xFFFFF3E0),
                fillBottom = Color(0xFFFFE0B2),
                blush = Color(0xFFFFAB91).copy(alpha = 0.38f),
                earInner = Color(0xFFFFCCBC),
                nose = Color(0xFF5D4037),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF7043),
            )
            PacMazeSkinId.LINE_BEAR -> LineArtPalette(
                fillTop = Color(0xFFEFEBE9),
                fillBottom = Color(0xFFD7CCC8),
                blush = Color(0xFFBCAAA4).copy(alpha = 0.4f),
                earInner = Color(0xFFBCAAA4),
                nose = Color(0xFF5D4037),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFA1887F),
            )
            PacMazeSkinId.LINE_PENGUIN -> LineArtPalette(
                fillTop = Color(0xFFF5F5F5),
                fillBottom = Color(0xFF37474F),
                blush = Color(0xFFB0BEC5).copy(alpha = 0.35f),
                earInner = Color(0xFF263238),
                nose = Color(0xFFFF8F00),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF6F00),
            )
            PacMazeSkinId.LINE_OWL -> LineArtPalette(
                fillTop = Color(0xFFD7CCC8),
                fillBottom = Color(0xFF8D6E63),
                blush = Color(0xFFA1887F).copy(alpha = 0.38f),
                earInner = Color(0xFF5D4037),
                nose = Color(0xFF4E342E),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFB300),
            )
            PacMazeSkinId.LINE_HEDGEHOG -> LineArtPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFBCAAA4),
                blush = Color(0xFFFFCC80).copy(alpha = 0.42f),
                earInner = Color(0xFF5D4037),
                nose = Color(0xFF3E2723),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF8D6E63),
            )
            PacMazeSkinId.LINE_SHIBA -> LineArtPalette(
                fillTop = Color(0xFFFFF3E0),
                fillBottom = Color(0xFFE65100),
                blush = Color(0xFFFFAB91).copy(alpha = 0.45f),
                earInner = Color(0xFFFFCC80),
                nose = Color(0xFF4E342E),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF7043),
            )
            PacMazeSkinId.LINE_OTTER -> LineArtPalette(
                fillTop = Color(0xFFBCAAA4),
                fillBottom = Color(0xFF6D4C41),
                blush = Color(0xFFA1887F).copy(alpha = 0.4f),
                earInner = Color(0xFF8D6E63),
                nose = Color(0xFF3E2723),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF4FC3F7),
            )
            PacMazeSkinId.LINE_KOALA -> LineArtPalette(
                fillTop = Color(0xFFECEFF1),
                fillBottom = Color(0xFF78909C),
                blush = Color(0xFFB0BEC5).copy(alpha = 0.38f),
                earInner = Color(0xFFCFD8DC),
                nose = Color(0xFF37474F),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF66BB6A),
            )
            else -> LineArtPalette(
                fillTop = Color(0xFFFFFDE7),
                fillBottom = Color(0xFFFFF8E1),
                blush = Color(0xFFFFAB91).copy(alpha = 0.35f),
                earInner = Color(0xFFFFCCBC),
                nose = Color(0xFF8D6E63),
                accent = if (power) Color(0xFFFF4081) else themePalette.frameAccent,
            )
        }
    }

    fun strokeColor(themeId: PacMazeMapThemeId): Color = when (themeId) {
        PacMazeMapThemeId.CYBERPUNK, PacMazeMapThemeId.ENDLESS -> Color(0xFFECEFF1)
        PacMazeMapThemeId.CHINESE, PacMazeMapThemeId.GARDEN -> Color(0xFFFFF8E1)
        else -> Color(0xFF37474F)
    }

    fun lineWidth(radius: Float): Float = (radius * 0.11f).coerceIn(2.2f, 7f)

    fun bob(pose: PacMazeCharacterPose, radius: Float): Float {
        val amp = if (pose.isMoving) 0.035f else 0.012f
        val speed = if (pose.isMoving) 3.5f else 2.2f
        return sin(pose.animPhase * speed) * radius * amp
    }

    fun drawShadow(scope: DrawScope, center: Offset, radius: Float) {
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.18f),
            topLeft = Offset(center.x - radius * 0.85f, center.y + radius * 0.52f),
            size = Size(radius * 1.7f, radius * 0.28f),
        )
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(center.x - radius * 0.55f, center.y + radius * 0.5f),
            size = Size(radius * 1.1f, radius * 0.18f),
        )
    }

    fun drawPowerAura(scope: DrawScope, center: Offset, radius: Float, accent: Color, active: Boolean) {
        if (!active) return
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.06f), Color.Transparent),
                center = center,
                radius = radius * 1.4f,
            ),
            radius = radius * 1.4f,
            center = center,
        )
    }

    fun DrawScope.fillBody(path: Path, palette: LineArtPalette, center: Offset, radius: Float) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(palette.fillTop, palette.fillBottom),
                center = Offset(center.x + radius * 0.08f, center.y - radius * 0.12f),
                radius = radius * 1.05f,
            ),
        )
    }

    fun DrawScope.strokeBody(path: Path, stroke: Color, lineW: Float) {
        drawPath(
            path = path,
            color = stroke,
            style = Stroke(lineW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }

    fun DrawScope.drawBlush(c: Offset, radius: Float, palette: LineArtPalette, y: Float = 0.08f) {
        listOf(-0.14f, 0.06f).forEach { dx ->
            drawCircle(
                color = palette.blush,
                radius = radius * 0.11f,
                center = Offset(c.x + radius * dx, c.y + radius * y),
            )
        }
    }

    fun DrawScope.drawCuteEye(
        center: Offset,
        radius: Float,
        palette: LineArtPalette,
        stroke: Color,
        lineW: Float,
        lookUp: Boolean = false,
    ) {
        val whiteR = radius * 0.11f
        drawCircle(color = Color.White.copy(alpha = 0.95f), radius = whiteR, center = center)
        drawCircle(color = stroke.copy(alpha = 0.35f), radius = whiteR, center = center, style = Stroke(lineW * 0.35f))
        val pupilCenter = Offset(
            center.x + radius * 0.015f,
            center.y + if (lookUp) -radius * 0.02f else radius * 0.01f,
        )
        drawCircle(color = palette.pupil, radius = radius * 0.055f, center = pupilCenter)
        drawCircle(color = palette.highlight.copy(alpha = 0.9f), radius = radius * 0.022f, center = Offset(pupilCenter.x - radius * 0.02f, pupilCenter.y - radius * 0.025f))
    }

    fun DrawScope.drawSimpleNose(c: Offset, radius: Float, palette: LineArtPalette, stroke: Color, lineW: Float) {
        val noseCenter = Offset(c.x + radius * 0.52f, c.y + radius * 0.06f)
        drawCircle(color = palette.nose, radius = radius * 0.045f, center = noseCenter)
        drawCircle(color = stroke.copy(alpha = 0.25f), radius = radius * 0.045f, center = noseCenter, style = Stroke(lineW * 0.3f))
        drawLine(
            color = stroke.copy(alpha = 0.55f),
            start = Offset(noseCenter.x - radius * 0.02f, noseCenter.y + radius * 0.04f),
            end = Offset(noseCenter.x - radius * 0.06f, noseCenter.y + radius * 0.1f),
            strokeWidth = lineW * 0.45f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = stroke.copy(alpha = 0.55f),
            start = Offset(noseCenter.x + radius * 0.02f, noseCenter.y + radius * 0.04f),
            end = Offset(noseCenter.x + radius * 0.06f, noseCenter.y + radius * 0.1f),
            strokeWidth = lineW * 0.45f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }

    fun DrawScope.drawSmile(c: Offset, radius: Float, stroke: Color, lineW: Float, open: Boolean = false) {
        if (open) {
            drawArc(
                color = stroke.copy(alpha = 0.65f),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.38f, c.y + radius * 0.04f),
                size = Size(radius * 0.22f, radius * 0.16f),
                style = Stroke(lineW * 0.55f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        } else {
            drawArc(
                color = stroke.copy(alpha = 0.5f),
                startAngle = 10f,
                sweepAngle = 55f,
                useCenter = false,
                topLeft = Offset(c.x + radius * 0.4f, c.y + radius * 0.08f),
                size = Size(radius * 0.18f, radius * 0.1f),
                style = Stroke(lineW * 0.45f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }
    }

    fun DrawScope.drawEarTriangle(
        path: Path,
        stroke: Color,
        lineW: Float,
        innerPath: Path? = null,
        innerColor: Color? = null,
    ) {
        innerPath?.let { ip ->
            innerColor?.let { drawPath(ip, color = it) }
        }
        drawPath(path, color = stroke, style = Stroke(lineW, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }

    fun DrawScope.drawWhiskers(c: Offset, radius: Float, stroke: Color, lineW: Float) {
        listOf(-0.04f, 0.06f, 0.16f).forEachIndexed { i, dy ->
            val len = radius * (0.22f - i * 0.02f)
            val start = Offset(c.x + radius * 0.48f, c.y + radius * dy)
            drawLine(stroke.copy(alpha = 0.45f - i * 0.08f), start, Offset(start.x + len, start.y - radius * 0.02f), lineW * 0.35f)
            drawLine(stroke.copy(alpha = 0.45f - i * 0.08f), start, Offset(start.x + len, start.y + radius * 0.03f), lineW * 0.35f)
        }
    }

    fun DrawScope.drawStubbyPaws(
        c: Offset,
        radius: Float,
        lineW: Float,
        stroke: Color,
        palette: LineArtPalette,
        pose: PacMazeCharacterPose,
        leftX: Float = -0.08f,
        rightX: Float = 0.18f,
    ) {
        if (!pose.isMoving) return
        val swing = sin(pose.animPhase * 6f) * radius * 0.1f
        listOf(leftX to swing, rightX to -swing).forEach { (x, off) ->
            val foot = Offset(c.x + radius * x + off, c.y + radius * 0.58f)
            drawLine(stroke.copy(alpha = 0.75f), Offset(foot.x, foot.y - radius * 0.12f), foot, lineW * 0.65f)
            drawOval(
                color = palette.fillBottom.copy(alpha = 0.85f),
                topLeft = Offset(foot.x - radius * 0.07f, foot.y - radius * 0.04f),
                size = Size(radius * 0.14f, radius * 0.08f),
            )
            drawOval(
                color = stroke.copy(alpha = 0.6f),
                topLeft = Offset(foot.x - radius * 0.07f, foot.y - radius * 0.04f),
                size = Size(radius * 0.14f, radius * 0.08f),
                style = Stroke(lineW * 0.45f),
            )
        }
    }

    fun DrawScope.drawFluffyTail(
        radius: Float,
        lineW: Float,
        stroke: Color,
        palette: LineArtPalette,
        swayDeg: Float,
        pivot: Offset,
        tip: Offset,
        layers: Int = 3,
    ) {
        rotate(swayDeg, pivot = pivot) {
            for (i in 0 until layers) {
                val t = i / (layers - 1).coerceAtLeast(1).toFloat()
                val path = Path().apply {
                    moveTo(pivot.x, pivot.y)
                    quadraticBezierTo(
                        pivot.x - radius * (0.35f + t * 0.15f),
                        pivot.y - radius * (0.22f - t * 0.08f),
                        tip.x - radius * t * 0.05f,
                        tip.y + radius * t * 0.04f,
                    )
                }
                val color = when (i) {
                    0 -> palette.accent.copy(alpha = 0.35f)
                    layers - 1 -> stroke
                    else -> palette.accent.copy(alpha = 0.65f)
                }
                val w = lineW * (1.1f - t * 0.35f)
                drawPath(path, color = color, style = Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }

    fun DrawScope.drawPatch(center: Offset, radius: Float, color: Color) {
        drawRoundRect(
            color = color,
            topLeft = Offset(center.x - radius, center.y - radius * 0.85f),
            size = Size(radius * 2f, radius * 1.7f),
            cornerRadius = CornerRadius(radius * 0.55f),
        )
    }

    inline fun withFacing(
        scope: DrawScope,
        center: Offset,
        facing: Direction,
        block: DrawScope.() -> Unit,
    ) {
        when (facing) {
            Direction.RIGHT -> scope.block()
            Direction.LEFT -> scope.scale(scaleX = -1f, scaleY = 1f, pivot = center, block = block)
            Direction.UP -> scope.rotate(degrees = -90f, pivot = center, block = block)
            Direction.DOWN -> scope.rotate(degrees = 90f, pivot = center, block = block)
        }
    }
}
