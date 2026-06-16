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
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

/** 海底游动：正弦平滑，无陆地式抖动。 */
internal data class SwimMotion(
    val glideY: Float,
    val bodySwayDeg: Float,
    val tailBeatDeg: Float,
    val finBeatDeg: Float,
    val ripple: Float,
)

internal data class SeaPalette(
    val fillTop: Color,
    val fillBottom: Color,
    val fin: Color,
    val accent: Color,
    val belly: Color,
    val stroke: Color,
    val detail: Color,
)

internal object SeaCreatureSkinHelpers {

    fun paletteFor(skinId: PacMazeSkinId, pose: PacMazeCharacterPose, themePalette: PacMazeThemePalette): SeaPalette {
        val power = pose.powerActive
        return when (skinId) {
            PacMazeSkinId.SEA_SHARK -> SeaPalette(
                fillTop = Color(0xFF78909C),
                fillBottom = Color(0xFF546E7A),
                fin = Color(0xFF455A64),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF90A4AE),
                belly = Color(0xFFECEFF1),
                stroke = Color(0xFF37474F),
                detail = Color(0xFF263238),
            )
            PacMazeSkinId.SEA_CLOWNFISH -> SeaPalette(
                fillTop = Color(0xFFFF8A50),
                fillBottom = Color(0xFFE65100),
                fin = Color(0xFFFFCC80),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFAB40),
                belly = Color(0xFFFFF8E1),
                stroke = Color(0xFF4E342E),
                detail = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.SEA_JELLYFISH -> SeaPalette(
                fillTop = Color(0xFFE1BEE7).copy(alpha = 0.85f),
                fillBottom = Color(0xFFCE93D8).copy(alpha = 0.65f),
                fin = Color(0xFFBA68C8),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFAB47BC),
                belly = Color(0xFFF3E5F5).copy(alpha = 0.5f),
                stroke = Color(0xFF8E24AA),
                detail = Color(0xFFEA80FC),
            )
            PacMazeSkinId.SEA_OCTOPUS -> SeaPalette(
                fillTop = Color(0xFFFF6E6E),
                fillBottom = Color(0xFFD32F2F),
                fin = Color(0xFFFFAB91),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF5252),
                belly = Color(0xFFFFCDD2),
                stroke = Color(0xFF880E4F),
                detail = Color(0xFF37474F),
            )
            PacMazeSkinId.SEA_TURTLE -> SeaPalette(
                fillTop = Color(0xFF7CB342),
                fillBottom = Color(0xFF33691E),
                fin = Color(0xFF9CCC65),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF26A69A),
                belly = Color(0xFFDCEDC8),
                stroke = Color(0xFF1B5E20),
                detail = Color(0xFF558B2F),
            )
            PacMazeSkinId.SEA_MANTA -> SeaPalette(
                fillTop = Color(0xFF546E7A),
                fillBottom = Color(0xFF263238),
                fin = Color(0xFF78909C),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF90A4AE),
                belly = Color(0xFFF5F5F5),
                stroke = Color(0xFF212121),
                detail = Color(0xFFB0BEC5),
            )
            PacMazeSkinId.SEA_SEAHORSE -> SeaPalette(
                fillTop = Color(0xFFFFB74D),
                fillBottom = Color(0xFFFF8F00),
                fin = Color(0xFFFFCC80),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFA726),
                belly = Color(0xFFFFF3E0),
                stroke = Color(0xFFE65100),
                detail = Color(0xFF5D4037),
            )
            PacMazeSkinId.SEA_DOLPHIN -> SeaPalette(
                fillTop = Color(0xFF4FC3F7),
                fillBottom = Color(0xFF0288D1),
                fin = Color(0xFF81D4FA),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF29B6F6),
                belly = Color(0xFFE1F5FE),
                stroke = Color(0xFF01579B),
                detail = Color(0xFF263238),
            )
            PacMazeSkinId.SEA_SQUID -> SeaPalette(
                fillTop = Color(0xFF7E57C2),
                fillBottom = Color(0xFF4527A0),
                fin = Color(0xFFB39DDB),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFEA80FC),
                belly = Color(0xFFE1BEE7).copy(alpha = 0.7f),
                stroke = Color(0xFF311B92),
                detail = Color(0xFFCE93D8),
            )
            PacMazeSkinId.SEA_ANGLER -> SeaPalette(
                fillTop = Color(0xFF455A64),
                fillBottom = Color(0xFF263238),
                fin = Color(0xFF546E7A),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFEE58),
                belly = Color(0xFF78909C).copy(alpha = 0.6f),
                stroke = Color(0xFF1A237E),
                detail = Color(0xFFFFF59D),
            )
            PacMazeSkinId.SEA_HERMIT -> SeaPalette(
                fillTop = Color(0xFFFFAB91),
                fillBottom = Color(0xFFFF7043),
                fin = Color(0xFFFF8A65),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF5722),
                belly = Color(0xFFFFCCBC),
                stroke = Color(0xFFBF360C),
                detail = Color(0xFF6D4C41),
            )
            PacMazeSkinId.SEA_STARFISH -> SeaPalette(
                fillTop = Color(0xFFFF8A65),
                fillBottom = Color(0xFFE64A19),
                fin = Color(0xFFFFAB91),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF6E40),
                belly = Color(0xFFFFCCBC),
                stroke = Color(0xFFBF360C),
                detail = Color(0xFFFFF3E0),
            )
            PacMazeSkinId.SEA_EEL -> SeaPalette(
                fillTop = Color(0xFF37474F),
                fillBottom = Color(0xFF263238),
                fin = Color(0xFF546E7A),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF40C4FF),
                belly = Color(0xFF607D8B).copy(alpha = 0.55f),
                stroke = Color(0xFF212121),
                detail = Color(0xFFFFEE58),
            )
            PacMazeSkinId.SEA_SUNFISH -> SeaPalette(
                fillTop = Color(0xFF90A4AE),
                fillBottom = Color(0xFF546E7A),
                fin = Color(0xFFB0BEC5),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF78909C),
                belly = Color(0xFFECEFF1),
                stroke = Color(0xFF37474F),
                detail = Color(0xFF263238),
            )
            else -> SeaPalette(
                fillTop = Color(0xFF4FC3F7),
                fillBottom = Color(0xFF0288D1),
                fin = Color(0xFF81D4FA),
                accent = if (power) Color(0xFFFF4081) else themePalette.frameAccent,
                belly = Color(0xFFE1F5FE),
                stroke = Color(0xFF01579B),
                detail = Color(0xFF0277BD),
            )
        }
    }

    /** 低频正弦游动：待机慢游、移动加速摆尾，无高频抖动。 */
    fun swimMotion(pose: PacMazeCharacterPose, radius: Float): SwimMotion {
        val moving = pose.isMoving
        val freq = if (moving) 3.2f else 1.8f
        val t = pose.animPhase * freq
        val amp = if (moving) 1f else 0.55f
        return SwimMotion(
            glideY = sin(t * 0.45f) * radius * 0.022f * amp,
            bodySwayDeg = sin(t * 0.55f) * (if (moving) 3.5f else 2f) * amp,
            tailBeatDeg = sin(t) * (if (moving) 18f else 10f),
            finBeatDeg = sin(t + 0.6f) * (if (moving) 14f else 8f),
            ripple = sin(t * 0.7f) * radius * 0.015f * amp,
        )
    }

    fun strokeColor(themeId: PacMazeMapThemeId): Color = when (themeId) {
        PacMazeMapThemeId.CYBERPUNK, PacMazeMapThemeId.ENDLESS -> Color(0xFFB3E5FC)
        else -> Color(0xFF37474F)
    }

    fun lineWidth(radius: Float): Float = (radius * 0.1f).coerceIn(2f, 6.5f)

    fun drawWaterShadow(scope: DrawScope, center: Offset, radius: Float) {
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(center.x - radius * 0.75f, center.y + radius * 0.48f),
            size = Size(radius * 1.5f, radius * 0.22f),
        )
    }

    fun drawPowerBubble(scope: DrawScope, center: Offset, radius: Float, accent: Color, active: Boolean) {
        if (!active) return
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.25f), Color(0xFF4FC3F7).copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = radius * 1.35f,
            ),
            radius = radius * 1.35f,
            center = center,
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

internal fun DrawScope.fillSeaBody(path: Path, palette: SeaPalette, center: Offset, radius: Float) {
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(palette.fillTop, palette.fillBottom, palette.belly.copy(alpha = 0.85f)),
            start = Offset(center.x - radius * 0.3f, center.y - radius * 0.2f),
            end = Offset(center.x + radius * 0.5f, center.y + radius * 0.3f),
        ),
    )
}

internal fun DrawScope.strokeSea(path: Path, palette: SeaPalette, lineW: Float) {
    drawPath(
        path = path,
        color = palette.stroke,
        style = Stroke(lineW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
    )
}

internal fun DrawScope.drawFishEye(center: Offset, radius: Float, palette: SeaPalette, lineW: Float) {
    drawCircle(color = Color.White.copy(alpha = 0.95f), radius = radius * 0.09f, center = center)
    drawCircle(color = palette.detail, radius = radius * 0.05f, center = Offset(center.x + radius * 0.012f, center.y))
    drawCircle(color = Color.White, radius = radius * 0.018f, center = Offset(center.x - radius * 0.015f, center.y - radius * 0.02f))
    drawCircle(color = palette.stroke.copy(alpha = 0.35f), radius = radius * 0.09f, center = center, style = Stroke(lineW * 0.35f))
}

internal fun DrawScope.drawCausticBubbles(c: Offset, radius: Float, phase: Float) {
    repeat(3) { i ->
        val bx = c.x - radius * (0.55f - i * 0.18f) + sin(phase + i * 1.2f) * radius * 0.04f
        val by = c.y - radius * (0.35f + i * 0.12f) - (phase * 0.08f + i * 0.15f) % 1f * radius * 0.25f
        drawCircle(
            color = Color.White.copy(alpha = 0.12f + i * 0.04f),
            radius = radius * (0.025f + i * 0.008f),
            center = Offset(bx, by),
        )
    }
}

/** 小丑鱼标志性白条纹：黑边 + 白芯 */
internal fun DrawScope.drawClownfishStripe(
    c: Offset,
    radius: Float,
    centerX: Float,
    ripple: Float,
    sea: SeaPalette,
    lineW: Float,
) {
    val stripe = Path().apply {
        moveTo(c.x + radius * centerX, c.y - radius * 0.32f + ripple)
        cubicTo(
            c.x + radius * (centerX + 0.05f), c.y - radius * 0.04f + ripple,
            c.x + radius * (centerX + 0.04f), c.y + radius * 0.22f + ripple,
            c.x + radius * centerX, c.y + radius * 0.32f + ripple,
        )
        cubicTo(
            c.x + radius * (centerX - 0.04f), c.y + radius * 0.22f + ripple,
            c.x + radius * (centerX - 0.05f), c.y - radius * 0.04f + ripple,
            c.x + radius * centerX, c.y - radius * 0.32f + ripple,
        )
    }
    drawPath(stripe, color = sea.stroke.copy(alpha = 0.85f), style = Stroke(lineW * 1.35f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawPath(stripe, color = sea.detail, style = Stroke(lineW * 0.95f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

/** 海龟背甲六边形鳞片 */
internal fun DrawScope.drawTurtleScutes(
    shellCenter: Offset,
    radius: Float,
    sea: SeaPalette,
    lineW: Float,
) {
    val rows = listOf(
        listOf(0f),
        listOf(-0.14f, 0.14f),
        listOf(-0.22f, 0f, 0.22f),
        listOf(-0.14f, 0.14f),
    )
    rows.forEachIndexed { row, cols ->
        cols.forEach { col ->
            val sc = Offset(
                shellCenter.x + radius * col,
                shellCenter.y - radius * 0.14f + row * radius * 0.13f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sea.fillTop, sea.detail.copy(alpha = 0.75f), sea.fillBottom),
                    center = sc,
                    radius = radius * 0.09f,
                ),
                radius = radius * 0.085f,
                center = sc,
            )
            drawCircle(
                color = sea.stroke.copy(alpha = 0.45f),
                radius = radius * 0.085f,
                center = sc,
                style = Stroke(lineW * 0.35f),
            )
        }
    }
    drawOval(
        color = sea.stroke.copy(alpha = 0.35f),
        topLeft = Offset(shellCenter.x - radius * 0.38f, shellCenter.y - radius * 0.26f),
        size = Size(radius * 0.76f, radius * 0.52f),
        style = Stroke(lineW * 0.4f),
    )
}

/** 章鱼触须吸盘（沿触须方向等距分布） */
internal fun DrawScope.drawTentacleSuckers(
    start: Offset,
    end: Offset,
    sea: SeaPalette,
    radius: Float,
    count: Int,
    lineW: Float,
) {
    repeat(count) { i ->
        val t = (i + 1f) / (count + 1f)
        val pt = Offset(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t,
        )
        drawCircle(
            color = sea.belly.copy(alpha = 0.55f),
            radius = radius * (0.028f - i * 0.002f),
            center = pt,
        )
        drawCircle(
            color = sea.stroke.copy(alpha = 0.25f),
            radius = radius * (0.028f - i * 0.002f),
            center = pt,
            style = Stroke(lineW * 0.25f),
        )
    }
}

/** 魔鬼鱼头部角状 cephalic fin */
internal fun DrawScope.drawMantaCephalicFin(
    bodyCenter: Offset,
    radius: Float,
    side: Float,
    sea: SeaPalette,
    lineW: Float,
) {
    val fin = Path().apply {
        moveTo(bodyCenter.x + radius * 0.18f * side, bodyCenter.y - radius * 0.06f)
        cubicTo(
            bodyCenter.x + radius * 0.34f * side, bodyCenter.y - radius * 0.22f,
            bodyCenter.x + radius * 0.42f * side, bodyCenter.y - radius * 0.04f,
            bodyCenter.x + radius * 0.28f * side, bodyCenter.y + radius * 0.06f,
        )
        cubicTo(
            bodyCenter.x + radius * 0.2f * side, bodyCenter.y + radius * 0.02f,
            bodyCenter.x + radius * 0.16f * side, bodyCenter.y - radius * 0.02f,
            bodyCenter.x + radius * 0.18f * side, bodyCenter.y - radius * 0.06f,
        )
    }
    fillSeaBody(fin, sea, bodyCenter, radius)
    strokeSea(fin, sea, lineW * 0.65f)
}

internal fun DrawScope.drawCuteEye(
    center: Offset,
    eyeRadius: Float,
    sea: SeaPalette,
    lineW: Float,
    large: Boolean = false,
) {
    val sclera = if (large) eyeRadius * 1.15f else eyeRadius
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFFF5F5F5)),
            center = center,
            radius = sclera,
        ),
        radius = sclera,
        center = center,
    )
    drawCircle(
        color = sea.detail,
        radius = eyeRadius * 0.58f,
        center = Offset(center.x + eyeRadius * 0.12f, center.y + eyeRadius * 0.05f),
    )
    drawCircle(
        color = Color.White,
        radius = eyeRadius * 0.22f,
        center = Offset(center.x - eyeRadius * 0.18f, center.y - eyeRadius * 0.2f),
    )
    drawCircle(
        color = sea.stroke.copy(alpha = 0.4f),
        radius = sclera,
        center = center,
        style = Stroke(lineW * 0.35f),
    )
}

internal inline fun drawSeaCreature(
    scope: DrawScope,
    center: Offset,
    radius: Float,
    pose: PacMazeCharacterPose,
    themeId: PacMazeMapThemeId,
    themePalette: PacMazeThemePalette,
    skinId: PacMazeSkinId,
    crossinline drawBody: DrawScope.(c: Offset, lineW: Float, palette: SeaPalette, swim: SwimMotion) -> Unit,
) {
    val palette = SeaCreatureSkinHelpers.paletteFor(skinId, pose, themePalette)
    val lineW = SeaCreatureSkinHelpers.lineWidth(radius)
    val swim = SeaCreatureSkinHelpers.swimMotion(pose, radius)
    val anchor = Offset(center.x, center.y + swim.glideY)

    SeaCreatureSkinHelpers.drawWaterShadow(scope, center, radius)
    SeaCreatureSkinHelpers.drawPowerBubble(scope, center, radius, palette.accent, pose.powerActive)

    SeaCreatureSkinHelpers.withFacing(scope, center, pose.facing) {
        rotate(swim.bodySwayDeg, pivot = anchor) {
            drawBody(anchor, lineW, palette, swim)
        }
    }
}
