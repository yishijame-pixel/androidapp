package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.PacMazeItemKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 道具辨识度：每种道具固定外形 + 中文标签色。 */
internal object PacMazeItemVisuals {

    data class ItemStyle(
        val primary: Color,
        val secondary: Color,
        val label: String,
        val badgeShape: ItemBadgeShape,
    )

    enum class ItemBadgeShape { CIRCLE, DIAMOND, HEX, SHIELD, BOLT }

    fun style(kind: PacMazeItemKind): ItemStyle = when (kind) {
        PacMazeItemKind.MAGNET -> ItemStyle(
            primary = Color(0xFF7E57C2),
            secondary = Color(0xFFEF5350),
            label = "磁力",
            badgeShape = ItemBadgeShape.CIRCLE,
        )
        PacMazeItemKind.SHIELD -> ItemStyle(
            primary = Color(0xFF00BCD4),
            secondary = Color(0xFF00838F),
            label = "护盾",
            badgeShape = ItemBadgeShape.SHIELD,
        )
        PacMazeItemKind.FROST -> ItemStyle(
            primary = Color(0xFF29B6F6),
            secondary = Color(0xFFE1F5FE),
            label = "冰霜",
            badgeShape = ItemBadgeShape.HEX,
        )
        PacMazeItemKind.SPEED -> ItemStyle(
            primary = Color(0xFFFFB300),
            secondary = Color(0xFFFFF176),
            label = "迅捷",
            badgeShape = ItemBadgeShape.BOLT,
        )
        PacMazeItemKind.DOUBLE -> ItemStyle(
            primary = Color(0xFF00E676),
            secondary = Color(0xFFB9F6CA),
            label = "双倍",
            badgeShape = ItemBadgeShape.DIAMOND,
        )
        PacMazeItemKind.CHARGE -> ItemStyle(
            primary = Color(0xFFFF5252),
            secondary = Color(0xFFFF8A80),
            label = "充能",
            badgeShape = ItemBadgeShape.CIRCLE,
        )
    }

    fun drawBadgeBackground(scope: DrawScope, center: Offset, radius: Float, style: ItemStyle, pulse: Float) {
        val r = radius * pulse
        when (style.badgeShape) {
            ItemBadgeShape.CIRCLE -> scope.drawCircle(
                brush = Brush.radialGradient(listOf(style.primary, style.secondary.copy(alpha = 0.5f)), center, r),
                radius = r,
                center = center,
            )
            ItemBadgeShape.DIAMOND -> drawDiamond(scope, center, r, style.primary, style.secondary)
            ItemBadgeShape.HEX -> drawHex(scope, center, r, style.primary, style.secondary)
            ItemBadgeShape.SHIELD -> drawShieldBadge(scope, center, r, style.primary, style.secondary)
            ItemBadgeShape.BOLT -> drawBoltBadge(scope, center, r, style.primary, style.secondary)
        }
        scope.drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r, center = center, style = Stroke(r * 0.08f))
    }

    fun drawKindGlyph(scope: DrawScope, kind: PacMazeItemKind, center: Offset, size: Float) {
        val style = style(kind)
        when (kind) {
            PacMazeItemKind.MAGNET -> drawMagnetGlyph(scope, center, size, style)
            PacMazeItemKind.SHIELD -> drawShieldGlyph(scope, center, size, style)
            PacMazeItemKind.FROST -> drawFrostGlyph(scope, center, size)
            PacMazeItemKind.SPEED -> drawSpeedGlyph(scope, center, size, style)
            PacMazeItemKind.DOUBLE -> drawDoubleGlyph(scope, center, size)
            PacMazeItemKind.CHARGE -> drawChargeGlyph(scope, center, size, style)
        }
    }

    fun drawLabelBanner(scope: DrawScope, center: Offset, cell: Float, style: ItemStyle) {
        val w = cell * 0.72f
        val h = cell * 0.22f
        val left = center.x - w / 2f
        val top = center.y + cell * 0.2f
        scope.drawRoundRect(
            brush = Brush.horizontalGradient(listOf(style.primary.copy(alpha = 0.95f), style.secondary.copy(alpha = 0.85f))),
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f),
        )
        scope.drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f),
            style = Stroke(1.2f),
        )
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.White.toArgb()
                textSize = cell * 0.16f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            drawText(style.label, center.x, top + h * 0.72f, paint)
        }
    }

    private fun drawMagnetGlyph(scope: DrawScope, center: Offset, size: Float, style: ItemStyle) {
        val gap = size * 0.1f
        scope.drawRoundRect(
            style.primary,
            Offset(center.x - size - gap / 2f, center.y - size * 0.85f),
            Size(size, size * 1.7f),
            CornerRadius(size * 0.18f),
        )
        scope.drawRoundRect(
            style.secondary,
            Offset(center.x + gap / 2f, center.y - size * 0.85f),
            Size(size, size * 1.7f),
            CornerRadius(size * 0.18f),
        )
        scope.drawRect(Color.White, Offset(center.x - size - gap / 2f, center.y - size * 0.12f), Size(size * 2f + gap, size * 0.24f))
    }

    private fun drawShieldGlyph(scope: DrawScope, center: Offset, size: Float, style: ItemStyle) {
        val path = Path().apply {
            moveTo(center.x, center.y - size)
            lineTo(center.x + size * 0.9f, center.y - size * 0.25f)
            lineTo(center.x + size * 0.55f, center.y + size)
            lineTo(center.x - size * 0.55f, center.y + size)
            lineTo(center.x - size * 0.9f, center.y - size * 0.25f)
            close()
        }
        scope.drawPath(path, Brush.linearGradient(listOf(Color.White, style.primary)), style = Fill)
        scope.drawPath(path, style.secondary, style = Stroke(size * 0.1f))
    }

    private fun drawFrostGlyph(scope: DrawScope, center: Offset, size: Float) {
        repeat(6) { i ->
            val angle = i * 60f
            val ex = center.x + cos(angle * PI / 180.0).toFloat() * size
            val ey = center.y + sin(angle * PI / 180.0).toFloat() * size
            scope.drawLine(Color.White, center, Offset(ex, ey), strokeWidth = size * 0.16f, cap = StrokeCap.Round)
        }
        scope.drawCircle(Color(0xFFE1F5FE), radius = size * 0.22f, center = center)
    }

    private fun drawSpeedGlyph(scope: DrawScope, center: Offset, size: Float, style: ItemStyle) {
        val bolt = Path().apply {
            moveTo(center.x + size * 0.1f, center.y - size)
            lineTo(center.x - size * 0.45f, center.y + size * 0.05f)
            lineTo(center.x - size * 0.05f, center.y + size * 0.05f)
            lineTo(center.x - size * 0.25f, center.y + size)
            lineTo(center.x + size * 0.55f, center.y - size * 0.2f)
            lineTo(center.x + size * 0.08f, center.y - size * 0.2f)
            close()
        }
        scope.drawPath(bolt, Brush.linearGradient(listOf(style.secondary, style.primary)), style = Fill)
        scope.drawPath(bolt, Color.White.copy(alpha = 0.85f), style = Stroke(size * 0.07f))
    }

    private fun drawDoubleGlyph(scope: DrawScope, center: Offset, size: Float) {
        scope.drawCircle(Color.White, radius = size * 0.62f, center = center, style = Stroke(size * 0.14f))
        scope.drawLine(Color.White, Offset(center.x - size * 0.42f, center.y), Offset(center.x + size * 0.42f, center.y), strokeWidth = size * 0.14f, cap = StrokeCap.Round)
        scope.drawLine(Color.White, Offset(center.x, center.y - size * 0.42f), Offset(center.x, center.y + size * 0.42f), strokeWidth = size * 0.14f, cap = StrokeCap.Round)
        drawMini2x(scope, center + Offset(size * 0.5f, -size * 0.5f), size * 0.38f)
    }

    private fun drawChargeGlyph(scope: DrawScope, center: Offset, size: Float, style: ItemStyle) {
        scope.drawCircle(brush = Brush.radialGradient(listOf(style.secondary, style.primary), center, size), radius = size, center = center)
        val core = Path().apply {
            moveTo(center.x, center.y - size * 0.7f)
            lineTo(center.x + size * 0.5f, center.y)
            lineTo(center.x, center.y + size * 0.7f)
            lineTo(center.x - size * 0.5f, center.y)
            close()
        }
        scope.drawPath(core, Color.White.copy(alpha = 0.92f), style = Fill)
    }

    private fun drawMini2x(scope: DrawScope, center: Offset, size: Float) {
        scope.drawRoundRect(Color.White, Offset(center.x - size * 0.5f, center.y - size * 0.32f), Size(size, size * 0.64f), CornerRadius(size * 0.12f))
        scope.drawLine(Color(0xFF00C853), Offset(center.x - size * 0.22f, center.y), Offset(center.x + size * 0.28f, center.y), strokeWidth = size * 0.14f, cap = StrokeCap.Round)
    }

    private fun drawDiamond(scope: DrawScope, center: Offset, radius: Float, primary: Color, secondary: Color) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x - radius, center.y)
            close()
        }
        scope.drawPath(path, Brush.linearGradient(listOf(primary, secondary)), style = Fill)
    }

    private fun drawHex(scope: DrawScope, center: Offset, radius: Float, primary: Color, secondary: Color) {
        val path = Path()
        for (i in 0 until 6) {
            val angle = (60f * i - 30f) * PI / 180.0
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        scope.drawPath(path, Brush.radialGradient(listOf(secondary, primary), center, radius), style = Fill)
    }

    private fun drawShieldBadge(scope: DrawScope, center: Offset, radius: Float, primary: Color, secondary: Color) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.95f, center.y - radius * 0.2f)
            lineTo(center.x + radius * 0.65f, center.y + radius * 0.95f)
            lineTo(center.x - radius * 0.65f, center.y + radius * 0.95f)
            lineTo(center.x - radius * 0.95f, center.y - radius * 0.2f)
            close()
        }
        scope.drawPath(path, Brush.verticalGradient(listOf(secondary, primary)), style = Fill)
    }

    private fun drawBoltBadge(scope: DrawScope, center: Offset, radius: Float, primary: Color, secondary: Color) {
        val path = Path().apply {
            moveTo(center.x + radius * 0.1f, center.y - radius)
            lineTo(center.x - radius * 0.45f, center.y + radius * 0.05f)
            lineTo(center.x + radius * 0.05f, center.y + radius * 0.05f)
            lineTo(center.x - radius * 0.15f, center.y + radius)
            lineTo(center.x + radius * 0.55f, center.y - radius * 0.2f)
            close()
        }
        scope.drawPath(path, Brush.linearGradient(listOf(secondary, primary)), style = Fill)
    }
}
