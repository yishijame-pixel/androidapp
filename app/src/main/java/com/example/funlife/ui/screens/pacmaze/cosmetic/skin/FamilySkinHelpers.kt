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
import com.example.funlife.ui.screens.pacmaze.maptheme.CyberVisualEffects
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette
import kotlin.math.sin

internal data class FamilyPalette(
    val fillTop: Color,
    val fillBottom: Color,
    val accent: Color,
    val detail: Color,
    val stroke: Color,
    val highlight: Color,
    val blush: Color = Color(0xFFFFAB91).copy(alpha = 0.45f),
)

internal data class FamilyMotion(
    val bob: Float,
    val step: Float,
    val swayDeg: Float,
    val pulse: Float,
    val ripple: Float,
)

internal object FamilySkinHelpers {

    fun paletteFor(skinId: PacMazeSkinId, pose: PacMazeCharacterPose, themePalette: PacMazeThemePalette): FamilyPalette {
        val power = pose.powerActive
        return when (skinId) {
            PacMazeSkinId.INK_DROP_SPIRIT -> FamilyPalette(
                fillTop = Color(0xFF424242),
                fillBottom = Color(0xFF212121),
                accent = if (power) Color(0xFFFF5252) else Color(0xFF757575),
                detail = Color(0xFFBDBDBD),
                stroke = Color(0xFF1A1A1A),
                highlight = Color(0xFFF5F5F5),
            )
            PacMazeSkinId.INK_PAPER_BIRD -> FamilyPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFFFECB3),
                accent = if (power) Color(0xFFFF7043) else Color(0xFFD84315),
                detail = Color(0xFFBF360C),
                stroke = Color(0xFF4E342E),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.INK_LION_DANCE -> FamilyPalette(
                fillTop = Color(0xFFFF7043),
                fillBottom = Color(0xFFD84315),
                accent = if (power) Color(0xFFFFD54F) else Color(0xFFFFAB40),
                detail = Color(0xFFFFEB3B),
                stroke = Color(0xFFBF360C),
                highlight = Color(0xFFFFF59D),
                blush = Color(0xFFFF8A65).copy(alpha = 0.35f),
            )
            PacMazeSkinId.INK_PORCELAIN -> FamilyPalette(
                fillTop = Color(0xFFF5F5F5),
                fillBottom = Color(0xFFE3F2FD),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF1565C0),
                detail = Color(0xFF1976D2),
                stroke = Color(0xFF37474F),
                highlight = Color(0xFFFFFFFF),
                blush = Color(0xFFFF8A80).copy(alpha = 0.42f),
            )
            PacMazeSkinId.INK_KYLIN -> FamilyPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFFFECB3),
                accent = if (power) Color(0xFFFF7043) else Color(0xFFFFB300),
                detail = Color(0xFF5D4037),
                stroke = Color(0xFF4E342E),
                highlight = Color(0xFFFFFDE7),
                blush = Color(0xFFFFCC80).copy(alpha = 0.35f),
            )
            PacMazeSkinId.INK_FAN_FAIRY -> FamilyPalette(
                fillTop = Color(0xFFFCE4EC),
                fillBottom = Color(0xFFF8BBD0),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFE91E63),
                detail = Color(0xFF880E4F),
                stroke = Color(0xFF4A148C),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.INK_LOTUS_BUD -> FamilyPalette(
                fillTop = Color(0xFFF1F8E9),
                fillBottom = Color(0xFFC8E6C9),
                accent = if (power) Color(0xFFFF7043) else Color(0xFF66BB6A),
                detail = Color(0xFF2E7D32),
                stroke = Color(0xFF1B5E20),
                highlight = Color(0xFFFFF9C4),
                blush = Color(0xFFFFAB91).copy(alpha = 0.3f),
            )
            PacMazeSkinId.INK_SHADOW_PUPPET -> FamilyPalette(
                fillTop = Color(0xFF424242),
                fillBottom = Color(0xFF212121),
                accent = if (power) Color(0xFFFF7043) else Color(0xFFB71C1C),
                detail = Color(0xFFEF5350),
                stroke = Color(0xFF000000),
                highlight = Color(0xFF757575),
            )
            PacMazeSkinId.CYBER_HOLO_CAT -> FamilyPalette(
                fillTop = CyberVisualEffects.NeonBlue.copy(alpha = 0.55f),
                fillBottom = CyberVisualEffects.NeonPink.copy(alpha = 0.45f),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFFE0F7FA),
                stroke = CyberVisualEffects.NeonBlue,
                highlight = Color.White,
            )
            PacMazeSkinId.CYBER_GLITCH_CUBE -> FamilyPalette(
                fillTop = Color(0xFF263238),
                fillBottom = Color(0xFF37474F),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonRed,
                detail = CyberVisualEffects.NeonBlue,
                stroke = Color(0xFF78909C),
                highlight = CyberVisualEffects.NeonBlue,
            )
            PacMazeSkinId.CYBER_MAGLEV_ORB -> FamilyPalette(
                fillTop = Color(0xFF7C4DFF),
                fillBottom = Color(0xFF311B92),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFFB388FF),
                stroke = Color(0xFF4527A0),
                highlight = Color(0xFFE1BEE7),
            )
            PacMazeSkinId.CYBER_WIRE_WORM -> FamilyPalette(
                fillTop = Color(0xFF546E7A),
                fillBottom = Color(0xFF37474F),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFF90A4AE),
                stroke = Color(0xFF263238),
                highlight = Color(0xFFB0BEC5),
            )
            PacMazeSkinId.CYBER_DRONE_BEE -> FamilyPalette(
                fillTop = Color(0xFF37474F),
                fillBottom = Color(0xFF263238),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFFFFEB3B),
                stroke = Color(0xFF455A64),
                highlight = Color(0xFFE0F7FA),
            )
            PacMazeSkinId.CYBER_NEON_SNAKE -> FamilyPalette(
                fillTop = CyberVisualEffects.NeonBlue.copy(alpha = 0.65f),
                fillBottom = CyberVisualEffects.NeonPink.copy(alpha = 0.5f),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFF00E676),
                stroke = CyberVisualEffects.NeonBlue,
                highlight = Color.White,
            )
            PacMazeSkinId.CYBER_CHIP_MONKEY -> FamilyPalette(
                fillTop = Color(0xFF455A64),
                fillBottom = Color(0xFF263238),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonBlue,
                detail = Color(0xFF4CAF50),
                stroke = Color(0xFF37474F),
                highlight = Color(0xFFB0BEC5),
            )
            PacMazeSkinId.CYBER_LASER_BEETLE -> FamilyPalette(
                fillTop = Color(0xFF311B92),
                fillBottom = Color(0xFF1A237E),
                accent = if (power) CyberVisualEffects.NeonYellow else CyberVisualEffects.NeonRed,
                detail = CyberVisualEffects.NeonBlue,
                stroke = Color(0xFF4527A0),
                highlight = Color(0xFFE1BEE7),
            )
            PacMazeSkinId.FOOD_MOCHI -> FamilyPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFFFF3E0),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFEF5350),
                detail = Color(0xFF8D6E63),
                stroke = Color(0xFFD7CCC8),
                highlight = Color(0xFFFFFFFF),
                blush = Color(0xFFFFAB91).copy(alpha = 0.3f),
            )
            PacMazeSkinId.FOOD_CHILI -> FamilyPalette(
                fillTop = Color(0xFFE53935),
                fillBottom = Color(0xFFB71C1C),
                accent = if (power) Color(0xFFFFD54F) else Color(0xFFFF7043),
                detail = Color(0xFF4CAF50),
                stroke = Color(0xFF7F0000),
                highlight = Color(0xFFFFCDD2),
            )
            PacMazeSkinId.FOOD_SUSHI -> FamilyPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFFFF3E0),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF8A65),
                detail = Color(0xFF37474F),
                stroke = Color(0xFF5D4037),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.FOOD_POPCORN -> FamilyPalette(
                fillTop = Color(0xFFFFFDE7),
                fillBottom = Color(0xFFFFF8E1),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFFB74D),
                detail = Color(0xFFFFCC80),
                stroke = Color(0xFF8D6E63),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.FOOD_TANGYUAN -> FamilyPalette(
                fillTop = Color(0xFFFAFAFA),
                fillBottom = Color(0xFFECEFF1),
                accent = if (power) Color(0xFFFF4081) else Color(0xFF424242),
                detail = Color(0xFF78909C),
                stroke = Color(0xFFB0BEC5),
                highlight = Color(0xFFFFFFFF),
                blush = Color(0xFFFFAB91).copy(alpha = 0.28f),
            )
            PacMazeSkinId.FOOD_DUMPLING -> FamilyPalette(
                fillTop = Color(0xFFFFF8E1),
                fillBottom = Color(0xFFFFECB3),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF7043),
                detail = Color(0xFF5D4037),
                stroke = Color(0xFF8D6E63),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.FOOD_MANGO_PUDDING -> FamilyPalette(
                fillTop = Color(0xFFFFCC80),
                fillBottom = Color(0xFFFF9800),
                accent = if (power) Color(0xFFFF4081) else Color(0xFFFF5722),
                detail = Color(0xFF4CAF50),
                stroke = Color(0xFFE65100),
                highlight = Color(0xFFFFF3E0),
                blush = Color(0xFFFFAB91).copy(alpha = 0.35f),
            )
            PacMazeSkinId.FOOD_DONUT -> FamilyPalette(
                fillTop = Color(0xFFF8BBD0),
                fillBottom = Color(0xFFF48FB1),
                accent = if (power) Color(0xFFFFEB3B) else Color(0xFFE91E63),
                detail = Color(0xFF4FC3F7),
                stroke = Color(0xFFAD1457),
                highlight = Color(0xFFFFFFFF),
            )
            PacMazeSkinId.FOOD_CHICK_DAZE -> FamilyPalette(
                fillTop = Color(0xFFFFD600),
                fillBottom = Color(0xFFFFD600),
                accent = Color(0xFFFF1744),
                detail = Color(0xFFFF6D00),
                stroke = Color(0xFF000000),
                highlight = Color(0xFFFFFFFF),
                blush = Color(0xFFFF1744),
            )
            else -> FamilyPalette(
                fillTop = Color(0xFFFFFDE7),
                fillBottom = Color(0xFFFFF8E1),
                accent = if (power) Color(0xFFFF4081) else themePalette.frameAccent,
                detail = Color(0xFF78909C),
                stroke = Color(0xFF37474F),
                highlight = Color.White,
            )
        }
    }

    fun motion(pose: PacMazeCharacterPose, radius: Float): FamilyMotion {
        val moving = pose.isMoving
        val freq = if (moving) 3.5f else 2f
        val t = pose.animPhase * freq
        val step = if (moving) sin(t) else sin(pose.animPhase * 1.2f) * 0.25f
        return FamilyMotion(
            bob = sin(t * 0.9f) * radius * (if (moving) 0.04f else 0.015f),
            step = step,
            swayDeg = sin(t * 0.7f) * (if (moving) 5f else 2.5f),
            pulse = 0.92f + 0.08f * sin(pose.animPhase * 3f),
            ripple = sin(t * 0.65f) * radius * 0.012f,
        )
    }

    fun strokeColor(themeId: PacMazeMapThemeId, palette: FamilyPalette): Color = when (themeId) {
        PacMazeMapThemeId.CYBERPUNK, PacMazeMapThemeId.ENDLESS -> palette.stroke.copy(alpha = 0.85f)
        PacMazeMapThemeId.CHINESE, PacMazeMapThemeId.GARDEN -> Color(0xFF3E2723)
        else -> palette.stroke
    }

    fun lineWidth(radius: Float): Float = (radius * 0.095f).coerceIn(2f, 6.5f)

    fun drawGroundShadow(scope: DrawScope, center: Offset, radius: Float) {
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(center.x - radius * 0.82f, center.y + radius * 0.5f),
            size = Size(radius * 1.64f, radius * 0.26f),
        )
        scope.drawOval(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(center.x - radius * 0.52f, center.y + radius * 0.48f),
            size = Size(radius * 1.04f, radius * 0.16f),
        )
    }

    fun drawMaglevPowerAura(scope: DrawScope, center: Offset, radius: Float, accent: Color, active: Boolean, animPhase: Float) {
        if (!active) return
        val pulse = 0.88f + 0.12f * sin(animPhase * 3.2f)
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.32f * pulse), accent.copy(alpha = 0.08f), Color.Transparent),
                center = center,
                radius = radius * 1.55f * pulse,
            ),
            radius = radius * 1.55f * pulse,
            center = center,
        )
        repeat(2) { ring ->
            val r = radius * (1.15f + ring * 0.22f) * pulse
            scope.drawCircle(
                color = accent.copy(alpha = 0.35f - ring * 0.1f),
                radius = r,
                center = center,
                style = Stroke(width = radius * (0.05f - ring * 0.012f)),
            )
        }
        scope.rotate(animPhase * 42f, pivot = center) {
            scope.drawArc(
                color = accent.copy(alpha = 0.45f),
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.3f, center.y - radius * 1.3f),
                size = Size(radius * 2.6f, radius * 2.6f),
                style = Stroke(width = radius * 0.04f),
            )
        }
    }

    fun drawSoftPowerAura(scope: DrawScope, center: Offset, radius: Float, accent: Color, active: Boolean) {
        if (!active) return
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.05f), Color.Transparent),
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

    /**
     * 位图皮肤四向朝向（遗留非 centerAnchored 路径）：原图默认朝左。
     * 矢量手绘用 [withFacing]（默认朝右），二者不可混用。
     */
    inline fun withBitmapFacing(
        scope: DrawScope,
        center: Offset,
        facing: Direction,
        block: DrawScope.() -> Unit,
    ) {
        when (facing) {
            Direction.LEFT -> scope.block()
            Direction.RIGHT -> scope.scale(scaleX = -1f, scaleY = 1f, pivot = center, block = block)
            Direction.UP -> scope.rotate(degrees = 90f, pivot = center, block = block)
            Direction.DOWN -> scope.rotate(degrees = -90f, pivot = center, block = block)
        }
    }

    /**
     * ikun 横版序列帧四向朝向：默认朝左，右镜像；上下绕脚点旋转（角度与旧版取反，修正「向上倒向」）。
     */
    inline fun withIkunBitmapFacing(
        scope: DrawScope,
        pivot: Offset,
        facing: Direction,
        block: DrawScope.() -> Unit,
    ) {
        when (facing) {
            Direction.LEFT -> scope.block()
            Direction.RIGHT -> scope.scale(scaleX = -1f, scaleY = 1f, pivot = pivot, block = block)
            Direction.UP -> scope.rotate(degrees = -90f, pivot = pivot, block = block)
            Direction.DOWN -> scope.rotate(degrees = 90f, pivot = pivot, block = block)
        }
    }
}

internal fun DrawScope.fillFamilyBody(path: Path, palette: FamilyPalette, center: Offset, radius: Float) {
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(palette.fillTop, palette.fillBottom, palette.highlight.copy(alpha = 0.35f)),
            center = Offset(center.x + radius * 0.06f, center.y - radius * 0.1f),
            radius = radius * 1.05f,
        ),
    )
}

internal fun DrawScope.strokeFamily(path: Path, palette: FamilyPalette, lineW: Float) {
    drawPath(
        path = path,
        color = palette.stroke,
        style = Stroke(lineW, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
    )
}

internal fun DrawScope.drawFamilyEye(
    center: Offset,
    radius: Float,
    palette: FamilyPalette,
    lineW: Float,
    large: Boolean = false,
) {
    val sclera = radius * if (large) 0.12f else 0.095f
    drawCircle(color = Color.White.copy(alpha = 0.96f), radius = sclera, center = center)
    drawCircle(color = palette.detail, radius = sclera * 0.55f, center = Offset(center.x + sclera * 0.12f, center.y + sclera * 0.06f))
    drawCircle(color = palette.highlight, radius = sclera * 0.22f, center = Offset(center.x - sclera * 0.18f, center.y - sclera * 0.2f))
    drawCircle(color = palette.stroke.copy(alpha = 0.35f), radius = sclera, center = center, style = Stroke(lineW * 0.35f))
}

internal inline fun drawFamilySkin(
    scope: DrawScope,
    center: Offset,
    radius: Float,
    pose: PacMazeCharacterPose,
    themeId: PacMazeMapThemeId,
    themePalette: PacMazeThemePalette,
    skinId: PacMazeSkinId,
    crossinline drawBody: DrawScope.(c: Offset, lineW: Float, palette: FamilyPalette, motion: FamilyMotion) -> Unit,
) {
    val palette = FamilySkinHelpers.paletteFor(skinId, pose, themePalette)
    val lineW = FamilySkinHelpers.lineWidth(radius)
    val motion = FamilySkinHelpers.motion(pose, radius)

    FamilySkinHelpers.drawGroundShadow(scope, center, radius)
    if (skinId == PacMazeSkinId.CYBER_MAGLEV_ORB) {
        FamilySkinHelpers.drawMaglevPowerAura(scope, center, radius, palette.accent, pose.powerActive, pose.animPhase)
    } else if (pose.powerActive) {
        FamilySkinHelpers.drawSoftPowerAura(scope, center, radius, palette.accent, true)
    }

    FamilySkinHelpers.withFacing(scope, center, pose.facing) {
        rotate(motion.swayDeg, pivot = Offset(center.x, center.y + motion.bob)) {
            drawBody(Offset(center.x, center.y + motion.bob), lineW, palette, motion)
        }
    }
}

internal fun walkOffset(motion: FamilyMotion, radius: Float): Float = motion.step * radius * 0.12f

internal fun blinkAlpha(phase: Float, open: Float = 0.92f): Float =
    if (sin(phase * 1.8f) > open) 0.08f else 1f
