package com.example.funlife.ui.screens.pacmaze.maptheme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazePortals
import kotlin.math.sin

/** 主题化 LINK 传送门：地砖层 + 标记层共用配色与状态。 */
internal object PacMazePortalVisual {

    enum class PortalPhase { DORMANT, ARMED, PAIR_READY }

    data class PortalStyle(
        val primary: Color,
        val secondary: Color,
        val glow: Color,
        val label: String,
    )

    fun phase(ctx: PacMazeMapRenderContext, x: Int, y: Int): PortalPhase {
        val marker = PacMazePortals.portalMarkerAt(ctx.markers, ctx.world.width, x, y)
            ?: return PortalPhase.DORMANT
        if (!PacMazePortals.isPortalArmed(ctx.world, marker)) return PortalPhase.DORMANT
        val pair = PacMazePortals.pairForMarker(ctx.markers, ctx.world.width, marker)
        return if (pair != null && PacMazePortals.isPairReady(ctx.world, pair)) {
            PortalPhase.PAIR_READY
        } else {
            PortalPhase.ARMED
        }
    }

    fun style(ctx: PacMazeMapRenderContext, x: Int, y: Int): PortalStyle {
        val isLeft = x <= ctx.world.width / 2
        val label = if (isLeft) "001" else "002"
        return when (ctx.config.id) {
            PacMazeMapThemeId.CYBERPUNK, PacMazeMapThemeId.ENDLESS, PacMazeMapThemeId.VHS -> PortalStyle(
                primary = if (isLeft) CyberVisualEffects.NeonBlue else CyberVisualEffects.NeonPink,
                secondary = if (isLeft) CyberVisualEffects.NeonPink else CyberVisualEffects.NeonBlue,
                glow = Color(0xFF67E8F9),
                label = label,
            )
            PacMazeMapThemeId.GARDEN, PacMazeMapThemeId.GREENHOUSE -> PortalStyle(
                primary = Color(0xFF7CB342),
                secondary = Color(0xFFDCEDC8),
                glow = Color(0xFFCDDC39),
                label = label,
            )
            PacMazeMapThemeId.FOOD -> PortalStyle(
                primary = Color(0xFFFF7043),
                secondary = Color(0xFFFFD180),
                glow = Color(0xFFFFAB91),
                label = label,
            )
            PacMazeMapThemeId.CHINESE, PacMazeMapThemeId.ARCHIVE, PacMazeMapThemeId.OPERA -> PortalStyle(
                primary = Color(0xFFD4AF37),
                secondary = Color(0xFF8D6E63),
                glow = Color(0xFFFFCA28),
                label = label,
            )
            PacMazeMapThemeId.SUBMARINE, PacMazeMapThemeId.METRO -> PortalStyle(
                primary = Color(0xFF4FC3F7),
                secondary = Color(0xFF0288D1),
                glow = Color(0xFF80DEEA),
                label = label,
            )
            PacMazeMapThemeId.STEAMPUNK -> PortalStyle(
                primary = Color(0xFFBCAAA4),
                secondary = Color(0xFF8D6E63),
                glow = Color(0xFFFFB74D),
                label = label,
            )
            PacMazeMapThemeId.ORBITAL -> PortalStyle(
                primary = Color(0xFFB388FF),
                secondary = Color(0xFF7C4DFF),
                glow = Color(0xFFE1BEE7),
                label = label,
            )
            PacMazeMapThemeId.MAGMA -> PortalStyle(
                primary = Color(0xFFFF5722),
                secondary = Color(0xFFFFAB40),
                glow = Color(0xFFFF7043),
                label = label,
            )
            PacMazeMapThemeId.FROST -> PortalStyle(
                primary = Color(0xFF81D4FA),
                secondary = Color(0xFFE1F5FE),
                glow = Color(0xFFB3E5FC),
                label = label,
            )
            PacMazeMapThemeId.MAZE, PacMazeMapThemeId.CLASSIC -> PortalStyle(
                primary = Color(0xFF42A5F5),
                secondary = Color(0xFF1565C0),
                glow = Color(0xFF90CAF9),
                label = label,
            )
            else -> PortalStyle(
                primary = ctx.config.palette.tunnelAccent,
                secondary = ctx.config.palette.wallGlow,
                glow = ctx.config.palette.tunnelAccent,
                label = label,
            )
        }
    }

    fun accent(style: PortalStyle, phase: PortalPhase): Color = when (phase) {
        PortalPhase.PAIR_READY -> Color(0xFF22C55E)
        PortalPhase.ARMED -> Color(0xFF84CC16)
        PortalPhase.DORMANT -> style.primary
    }

    /** 地砖层：在 DOOR/PORTAL 格绘制主题传送门。 */
    fun drawPortalTile(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        rect: Rect,
        cell: Float,
        x: Int,
        y: Int,
    ) {
        val marker = PacMazePortals.portalMarkerAt(ctx.markers, ctx.world.width, x, y)
        if (marker == null) {
            MapThemeTiles.drawPortal(scope, rect, cell, ctx.config.palette, ctx.animPhase)
            return
        }
        drawPortalCore(scope, ctx, rect, cell, marker, tileLayer = true)
    }

    /** 标记层：LINK checkpoint 叠加绘制。 */
    fun drawLinkMarker(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        rect: Rect,
        cell: Float,
        marker: PacMazeMapMarker,
    ) {
        drawPortalCore(scope, ctx, rect, cell, marker, tileLayer = false)
    }

    private fun drawPortalCore(
        scope: DrawScope,
        ctx: PacMazeMapRenderContext,
        rect: Rect,
        cell: Float,
        marker: PacMazeMapMarker,
        tileLayer: Boolean,
    ) {
        val center = Offset(rect.center.x, rect.center.y)
        val phase = phase(ctx, marker.x, marker.y)
        val style = style(ctx, marker.x, marker.y)
        val accent = accent(style, phase)
        val accent2 = if (phase == PortalPhase.DORMANT) style.secondary else Color(0xFFBBF7D0)
        val anim = ctx.animPhase
        val pulse = 0.82f + 0.18f * sin(anim * 2.2f)
        val isLeft = marker.x <= ctx.world.width / 2

        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A1020).copy(alpha = if (tileLayer) 0.55f else 0.35f),
                    Color(0xFF020408).copy(alpha = 0.2f),
                ),
                center = center,
                radius = cell * 0.58f,
            ),
            topLeft = rect.topLeft,
            size = rect.size,
        )

        scope.drawContext.canvas.nativeCanvas.apply {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = cell * 0.1f
                color = accent.copy(alpha = 0.35f * pulse).toArgb()
                maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
            }
            drawCircle(center.x, center.y, cell * 0.36f * pulse, glow)
        }

        scope.rotate(anim * 38f, center) {
            scope.drawOval(
                color = accent.copy(alpha = 0.55f),
                topLeft = Offset(center.x - cell * 0.34f, center.y - cell * 0.22f),
                size = Size(cell * 0.68f, cell * 0.44f),
                style = Stroke(width = cell * 0.045f),
            )
        }
        scope.rotate(-anim * 52f, center) {
            scope.drawOval(
                color = accent2.copy(alpha = 0.45f),
                topLeft = Offset(center.x - cell * 0.28f, center.y - cell * 0.18f),
                size = Size(cell * 0.56f, cell * 0.36f),
                style = Stroke(width = cell * 0.035f),
            )
        }

        val beamW = cell * 0.08f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent.copy(alpha = 0.85f),
                    Color.White.copy(alpha = if (phase == PortalPhase.PAIR_READY) 0.95f else 0.75f),
                    accent2.copy(alpha = 0.85f),
                    Color.Transparent,
                ),
            ),
            topLeft = Offset(center.x - beamW / 2f, rect.top + cell * 0.08f),
            size = Size(beamW, rect.height * 0.84f),
        )

        drawChevrons(scope, rect, cell, isLeft, accent, anim)

        if (phase != PortalPhase.DORMANT) {
            scope.drawCircle(
                color = accent.copy(alpha = 0.32f),
                radius = cell * 0.42f,
                center = center,
            )
            scope.drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = cell * 0.42f,
                center = center,
                style = Stroke(width = cell * 0.04f),
            )
        }

        if (!tileLayer) {
            val label = marker.label.ifBlank { style.label }
            drawLabel(
                scope,
                Rect(rect.left, rect.top + cell * 0.04f, rect.right, rect.top + cell * 0.38f),
                cell,
                label,
                Color.White,
                cell * 0.22f,
            )
            val tagRect = Rect(
                rect.left + cell * 0.08f,
                rect.bottom - cell * 0.28f,
                rect.right - cell * 0.08f,
                rect.bottom - cell * 0.06f,
            )
            scope.drawRoundRect(
                color = Color(0xFF001820).copy(alpha = 0.92f),
                topLeft = tagRect.topLeft,
                size = tagRect.size,
                cornerRadius = CornerRadius(cell * 0.04f),
                style = Stroke(width = cell * 0.025f),
            )
            val tag = when (phase) {
                PortalPhase.PAIR_READY -> "✓ 可传送"
                PortalPhase.ARMED -> "✓ 已激活"
                PortalPhase.DORMANT -> "↕ 踩入激活"
            }
            drawLabel(scope, tagRect, cell, tag, accent, cell * 0.13f, mono = true)
        }
    }

    private fun drawChevrons(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        isLeft: Boolean,
        accent: Color,
        phase: Float,
    ) {
        val cy = rect.center.y
        val dir = if (isLeft) 1f else -1f
        val baseX = if (isLeft) rect.right - cell * 0.22f else rect.left + cell * 0.22f
        repeat(2) { i ->
            val t = phase * 1.4f + i * 0.5f
            val ox = sin(t) * cell * 0.04f
            val path = Path().apply {
                moveTo(baseX + dir * cell * 0.08f + ox, cy - cell * 0.12f)
                lineTo(baseX + ox, cy)
                lineTo(baseX + dir * cell * 0.08f + ox, cy + cell * 0.12f)
            }
            scope.drawPath(path, accent.copy(alpha = 0.65f), style = Stroke(width = cell * 0.035f))
        }
    }

    private fun drawLabel(
        scope: DrawScope,
        rect: Rect,
        cell: Float,
        text: String,
        color: Color,
        size: Float,
        mono: Boolean = false,
    ) {
        scope.drawContext.canvas.nativeCanvas.apply {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.toArgb()
                textSize = size
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                if (mono) typeface = Typeface.MONOSPACE
            }
            drawText(text, rect.center.x, rect.center.y + cell * 0.05f, paint)
        }
    }
}
