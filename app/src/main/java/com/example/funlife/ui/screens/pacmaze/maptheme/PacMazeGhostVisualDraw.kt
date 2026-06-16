package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostSilhouette
import kotlin.math.cos
import kotlin.math.sin

/**
 * 幽灵专属 UI：经典吃豆人圆顶裙边 + 器物/符咒/故障/机关，与玩家皮肤完全分离。
 */
internal object PacMazeGhostVisualDraw {

    fun drawBody(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        bodyColor: Color,
        animPhase: Float,
        wobble: Float,
    ) {
        when (kind.silhouette) {
            GhostSilhouette.CLASSIC_ARCADE -> drawClassicArcade(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.PENDULUM -> drawPendulum(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.TALISMAN -> drawTalisman(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.TWIN_ECHO -> drawTwinEcho(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.WISP -> drawWillOWisp(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.ORIGAMI -> drawOrigami(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.ERROR_PANEL -> drawErrorSpecter(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.HOURGLASS -> drawHourglass(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.GLITCH_BLOCKS -> drawGlitch(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.ABACUS -> drawAbacus(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.SPIDER_NODE -> drawRouterSpider(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.GATE_STATUE -> drawGateStatue(scope, center, radius, bodyColor, animPhase, wobble)
            GhostSilhouette.CACHE_STACK -> drawCacheBlob(scope, center, radius, bodyColor, animPhase, wobble)
        }
    }

    fun drawBodyOutline(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        outlineColor: Color,
        strokeWidth: Float,
        animPhase: Float,
        wobble: Float,
    ) {
        val path = bodyPath(center, radius, kind, animPhase, wobble) ?: return
        scope.drawPath(path, outlineColor, style = Stroke(strokeWidth))
    }

    fun drawEyes(
        scope: DrawScope,
        center: Offset,
        radius: Float,
        kind: GhostKind,
        direction: Direction?,
        frightened: Boolean,
        top: Float,
        animPhase: Float,
    ) {
        val pupil = if (frightened) Color(0xFFFF5252) else Color(0xFF263238)
        when (kind.silhouette) {
            GhostSilhouette.CLASSIC_ARCADE -> drawClassicArcadeEyes(scope, center, radius, top, pupil, direction, frightened)
            GhostSilhouette.PENDULUM -> drawSlitEye(scope, center, radius, pupil, animPhase)
            GhostSilhouette.TALISMAN -> drawRuneEye(scope, center, radius, top, pupil)
            GhostSilhouette.TWIN_ECHO -> drawDualOffsetEyes(scope, center, radius, top, pupil, direction)
            GhostSilhouette.WISP -> drawWispCore(scope, center, radius, animPhase, frightened)
            GhostSilhouette.ORIGAMI -> drawInkDotEyes(scope, center, radius, top, pupil)
            GhostSilhouette.ERROR_PANEL -> drawErrorEyes(scope, center, radius, top)
            GhostSilhouette.HOURGLASS -> drawHourglassEye(scope, center, radius, animPhase, pupil)
            GhostSilhouette.GLITCH_BLOCKS -> drawGlitchEye(scope, center, radius, pupil, animPhase)
            GhostSilhouette.ABACUS -> drawBeadEyes(scope, center, radius, top, pupil, animPhase)
            GhostSilhouette.SPIDER_NODE -> drawNodeEye(scope, center, radius, pupil, animPhase)
            GhostSilhouette.GATE_STATUE -> drawStoneEyes(scope, center, radius, top, pupil)
            GhostSilhouette.CACHE_STACK -> drawStackEyes(scope, center, radius, top, pupil)
        }
    }

    private fun bodyPath(center: Offset, radius: Float, kind: GhostKind, animPhase: Float, wobble: Float): Path? {
        val top = center.y - radius + wobble
        val left = center.x - radius
        return when (kind.silhouette) {
            GhostSilhouette.CLASSIC_ARCADE -> classicArcadePath(center, radius, animPhase, wobble)
            GhostSilhouette.PENDULUM -> pendulumPath(center, radius, animPhase, wobble)
            GhostSilhouette.TALISMAN -> talismanPath(left, top, radius)
            GhostSilhouette.TWIN_ECHO -> twinPath(center, radius, top, left, animPhase)
            GhostSilhouette.WISP -> wispPath(center, radius, animPhase)
            GhostSilhouette.ORIGAMI -> origamiPath(center, radius, top, left, animPhase)
            GhostSilhouette.ERROR_PANEL -> errorPath(left, top, radius)
            GhostSilhouette.HOURGLASS -> hourglassPath(center, radius, top, left)
            GhostSilhouette.GLITCH_BLOCKS -> glitchPath(center, radius, animPhase)
            GhostSilhouette.ABACUS -> abacusPath(left, top, radius)
            GhostSilhouette.SPIDER_NODE -> spiderPath(center, radius, animPhase)
            GhostSilhouette.GATE_STATUE -> gatePath(left, top, radius)
            GhostSilhouette.CACHE_STACK -> cachePath(left, top, radius, animPhase)
        }
    }

    // --- CLASSIC: Pac-Man arcade ghost ---
    private fun drawClassicArcade(
        scope: DrawScope,
        c: Offset,
        r: Float,
        color: Color,
        phase: Float,
        wobble: Float,
    ) {
        scope.drawPath(classicArcadePath(c, r, phase, wobble), color)
    }

    private fun classicArcadePath(c: Offset, r: Float, phase: Float, wobble: Float): Path {
        val top = c.y - r * 0.95f + wobble
        val left = c.x - r
        val right = c.x + r
        val skirtY = c.y + r * 0.55f + wobble
        return Path().apply {
            moveTo(left, skirtY)
            val segments = 4
            val segW = (right - left) / segments
            repeat(segments) { i ->
                val x1 = left + segW * (i + 1)
                val cpX = left + segW * i + segW * 0.5f
                val dip = sin(phase * 4f + i * 1.1f) * r * 0.06f
                quadraticBezierTo(cpX, skirtY + r * 0.22f + dip, x1, skirtY)
            }
            arcTo(
                rect = Rect(left, top, right, top + r * 1.85f),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false,
            )
            close()
        }
    }

    private fun drawClassicArcadeEyes(
        scope: DrawScope,
        c: Offset,
        r: Float,
        top: Float,
        pupil: Color,
        dir: Direction?,
        frightened: Boolean,
    ) {
        if (frightened) {
            val y = top + r * 0.72f
            listOf(-r * 0.28f, r * 0.12f).forEach { dx ->
                val x = c.x + dx
                scope.drawLine(Color.White, Offset(x - r * 0.1f, y - r * 0.06f), Offset(x + r * 0.1f, y + r * 0.06f), r * 0.05f)
                scope.drawLine(Color.White, Offset(x - r * 0.1f, y + r * 0.06f), Offset(x + r * 0.1f, y - r * 0.06f), r * 0.05f)
            }
            return
        }
        val lookX = when (dir) {
            Direction.LEFT -> -r * 0.08f
            Direction.RIGHT -> r * 0.08f
            else -> 0f
        }
        listOf(-r * 0.28f, r * 0.12f).forEach { dx ->
            scope.drawCircle(Color.White, r * 0.22f, Offset(c.x + dx, top + r * 0.72f))
            scope.drawCircle(pupil, r * 0.11f, Offset(c.x + dx + lookX, top + r * 0.72f))
        }
    }

    // --- STRIKER: Pendulum ---
    private fun drawPendulum(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val swing = sin(phase * 1.6f) * r * 0.12f
        scope.drawLine(Color.Gray.copy(alpha = 0.5f), Offset(c.x, c.y - r * 1.35f), Offset(c.x + swing, c.y - r * 0.15f), r * 0.04f)
        scope.drawPath(pendulumPath(c, r, phase, wobble), color)
    }

    private fun pendulumPath(c: Offset, r: Float, phase: Float, wobble: Float): Path {
        val swing = sin(phase * 1.6f) * r * 0.12f
        val cx = c.x + swing
        val cy = c.y + wobble
        return Path().apply {
            moveTo(cx, cy - r * 0.95f)
            lineTo(cx + r * 0.72f, cy + r * 0.35f)
            lineTo(cx, cy + r * 1.05f)
            lineTo(cx - r * 0.72f, cy + r * 0.35f)
            close()
        }
    }

    // --- PREDICTOR: Talisman ---
    private fun drawTalisman(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.55f
        scope.drawPath(talismanPath(left, top, r), color)
        val glow = sin(phase * 2.2f) * 0.15f + 0.25f
        scope.drawRoundRect(
            Color(0xFFFFEB3B).copy(alpha = glow),
            topLeft = Offset(left + r * 0.25f, top + r * 0.55f),
            size = Size(r * 0.6f, r * 0.08f),
            cornerRadius = CornerRadius(r * 0.04f),
        )
    }

    private fun talismanPath(left: Float, top: Float, r: Float): Path = Path().apply {
        moveTo(left + r * 0.55f, top)
        lineTo(left + r * 1.05f, top + r * 0.15f)
        lineTo(left + r * 0.95f, top + r * 2.05f)
        lineTo(left + r * 0.15f, top + r * 2.05f)
        lineTo(left, top + r * 0.15f)
        close()
    }

    // --- FLANKER: Twin Echo ---
    private fun drawTwinEcho(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r
        val offset = r * 0.18f + sin(phase * 1.4f) * r * 0.04f
        scope.drawPath(twinPath(Offset(c.x - offset, c.y), r * 0.92f, top, left, phase), color.copy(alpha = 0.45f))
        scope.drawPath(twinPath(c, r, top, left, phase), color)
    }

    private fun twinPath(c: Offset, r: Float, top: Float, @Suppress("UNUSED_PARAMETER") left: Float, @Suppress("UNUSED_PARAMETER") phase: Float): Path =
        Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(c.x - r * 0.85f, top + r * 0.08f, c.x + r * 0.85f, top + r * 2f),
                    cornerRadius = CornerRadius(r * 0.35f),
                ),
            )
        }

    // --- OPPORTUNIST: Will-o-wisp ---
    private fun drawWillOWisp(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        scope.drawCircle(color.copy(alpha = 0.35f), r * 1.25f, c)
        scope.drawPath(wispPath(c, r, phase), color)
        repeat(3) { i ->
            val t = phase * 2f + i * 2.1f
            scope.drawCircle(
                color.copy(alpha = 0.5f),
                r * 0.12f,
                Offset(c.x + cos(t) * r * 0.55f, c.y + r * 0.65f + sin(t * 1.3f) * r * 0.08f),
            )
        }
    }

    private fun wispPath(c: Offset, r: Float, phase: Float): Path = Path().apply {
        moveTo(c.x - r * 0.55f, c.y + r * 0.35f)
        quadraticBezierTo(c.x, c.y - r * 1.05f, c.x + r * 0.55f, c.y + r * 0.35f)
        lineTo(c.x + r * 0.35f, c.y + r * 0.95f + sin(phase) * r * 0.05f)
        lineTo(c.x - r * 0.35f, c.y + r * 0.95f)
        close()
    }

    // --- ORIGAMI ---
    private fun drawOrigami(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        scope.drawPath(origamiPath(c, r, c.y - r + wobble, c.x - r, phase), color)
        scope.drawLine(Color.White.copy(alpha = 0.35f), Offset(c.x, c.y - r), Offset(c.x + r * 0.4f, c.y + r * 0.5f), r * 0.03f)
    }

    private fun origamiPath(c: Offset, r: Float, top: Float, left: Float, phase: Float): Path = Path().apply {
        val tilt = sin(phase * 1.5f) * r * 0.06f
        moveTo(c.x + tilt, top + r * 0.1f)
        lineTo(left + r * 1.9f, top + r * 0.55f)
        lineTo(c.x + r * 0.3f, top + r * 2f)
        lineTo(left + r * 0.15f, top + r * 0.65f)
        close()
    }

    // --- ERROR SPECTER ---
    private fun drawErrorSpecter(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.85f
        scope.drawPath(errorPath(left, top, r), color)
        scope.drawRect(Color(0xFF212121), topLeft = Offset(left + r * 0.15f, top + r * 0.35f), size = Size(r * 1.35f, r * 1.15f))
        scope.drawRect(color.copy(alpha = 0.9f), topLeft = Offset(left + r * 0.15f, top + r * 0.35f), size = Size(r * 1.35f, r * 0.22f))
    }

    private fun errorPath(left: Float, top: Float, r: Float): Path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(left, top + r * 0.2f, left + r * 1.65f, top + r * 1.75f),
                CornerRadius(r * 0.12f),
            ),
        )
    }

    // --- HOURGLASS ---
    private fun drawHourglass(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.65f
        scope.drawPath(hourglassPath(c, r, top, left), color)
        val sandY = top + r * (0.95f + sin(phase * 1.2f) * 0.08f)
        scope.drawCircle(Color(0xFFFFE082).copy(alpha = 0.7f), r * 0.18f, Offset(c.x, sandY))
    }

    private fun hourglassPath(c: Offset, r: Float, top: Float, left: Float): Path = Path().apply {
        moveTo(left + r * 0.65f, top + r * 0.15f)
        lineTo(left + r * 1.25f, top + r * 0.15f)
        lineTo(c.x + r * 0.15f, top + r * 0.95f)
        lineTo(left + r * 1.25f, top + r * 1.85f)
        lineTo(left + r * 0.65f, top + r * 1.85f)
        lineTo(c.x - r * 0.15f, top + r * 0.95f)
        close()
    }

    // --- GLITCH ---
    private fun drawGlitch(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val dx = sin(phase * 3.7f) * r * 0.08f
        listOf(
            color.copy(alpha = 0.85f) to Offset(dx, 0f),
            Color.Red.copy(alpha = 0.35f) to Offset(-r * 0.06f, r * 0.04f),
            Color.Cyan.copy(alpha = 0.35f) to Offset(r * 0.06f, -r * 0.04f),
        ).forEach { (col, off) ->
            scope.drawPath(glitchPath(c + off, r, phase), col)
        }
    }

    private fun glitchPath(c: Offset, r: Float, phase: Float): Path = Path().apply {
        val j = sin(phase * 5f) * r * 0.05f
        addRect(Rect(c.x - r * 0.75f + j, c.y - r * 0.85f, c.x + r * 0.55f, c.y + r * 0.25f))
        addRect(Rect(c.x - r * 0.45f, c.y + r * 0.05f, c.x + r * 0.85f - j, c.y + r * 0.95f))
    }

    // --- ABACUS ---
    private fun drawAbacus(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.45f
        scope.drawRoundRect(Color(0xFF5D4037), Offset(left, top + r * 0.2f), Size(r * 0.9f, r * 1.65f), CornerRadius(r * 0.08f))
        repeat(4) { row ->
            val y = top + r * (0.35f + row * 0.38f)
            scope.drawLine(Color(0xFF3E2723), Offset(left + r * 0.08f, y), Offset(left + r * 0.82f, y), r * 0.035f)
            repeat(3) { col ->
                val bx = left + r * (0.18f + col * 0.28f) + sin(phase + row + col) * r * 0.02f
                scope.drawCircle(if (row == 1 && col == 1) color else color.copy(alpha = 0.75f), r * 0.14f, Offset(bx, y))
            }
        }
    }

    private fun abacusPath(left: Float, top: Float, r: Float): Path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(left, top + r * 0.2f, left + r * 0.9f, top + r * 1.85f),
                CornerRadius(r * 0.08f),
            ),
        )
    }

    // --- ROUTER SPIDER ---
    private fun drawRouterSpider(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        scope.drawPath(spiderPath(c, r, phase), color)
        repeat(6) { i ->
            val angle = (i * 60f + phase * 18f) * (Math.PI / 180.0)
            val legEnd = Offset(
                c.x + cos(angle).toFloat() * r * 1.15f,
                c.y + sin(angle).toFloat() * r * 1.15f,
            )
            scope.drawLine(color.copy(alpha = 0.8f), c, legEnd, r * 0.07f)
        }
        scope.drawCircle(Color(0xFF304FFE), r * 0.35f, c)
    }

    private fun spiderPath(c: Offset, r: Float, phase: Float): Path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(c.x - r * 0.55f, c.y - r * 0.55f, c.x + r * 0.55f, c.y + r * 0.55f),
                CornerRadius(r * 0.15f),
            ),
        )
    }

    // --- GATE STATUE ---
    private fun drawGateStatue(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.75f
        scope.drawPath(gatePath(left, top, r), color)
        scope.drawRect(Color(0xFFFFD54F).copy(alpha = 0.85f), Offset(left + r * 0.55f, top + r * 0.55f), Size(r * 0.55f, r * 0.22f))
    }

    private fun gatePath(left: Float, top: Float, r: Float): Path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(left, top + r * 0.25f, left + r * 1.45f, top + r * 1.85f),
                CornerRadius(r * 0.12f),
            ),
        )
        moveTo(left + r * 0.25f, top + r * 0.25f)
        lineTo(left + r * 0.72f, top)
        lineTo(left + r * 1.2f, top + r * 0.25f)
        close()
    }

    // --- CACHE BLOB ---
    private fun drawCacheBlob(scope: DrawScope, c: Offset, r: Float, color: Color, phase: Float, wobble: Float) {
        val top = c.y - r + wobble
        val left = c.x - r * 0.7f
        repeat(3) { i ->
            val shrink = i * r * 0.08f
            scope.drawRoundRect(
                color.copy(alpha = 1f - i * 0.15f),
                Offset(left + shrink, top + r * 0.35f + i * r * 0.22f),
                Size(r * 1.35f - shrink * 2, r * 0.55f),
                CornerRadius(r * 0.1f),
            )
        }
    }

    private fun cachePath(left: Float, top: Float, r: Float, phase: Float): Path = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                Rect(left, top + r * 0.35f, left + r * 1.35f, top + r * 1.15f),
                CornerRadius(r * 0.1f),
            ),
        )
    }

    // --- Eyes ---
    private fun drawSlitEye(scope: DrawScope, c: Offset, r: Float, pupil: Color, @Suppress("UNUSED_PARAMETER") phase: Float) {
        scope.drawRoundRect(pupil, Offset(c.x - r * 0.22f, c.y - r * 0.06f), Size(r * 0.44f, r * 0.12f), CornerRadius(r * 0.06f))
    }

    private fun drawRuneEye(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color) {
        scope.drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.85f), r * 0.18f, Offset(c.x, top + r * 0.75f))
        scope.drawCircle(pupil, r * 0.07f, Offset(c.x, top + r * 0.75f))
    }

    private fun drawDualOffsetEyes(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color, dir: Direction?) {
        val shift = if (dir == Direction.LEFT) -r * 0.06f else if (dir == Direction.RIGHT) r * 0.06f else 0f
        listOf(-r * 0.22f + shift, r * 0.28f - shift).forEach { dx ->
            scope.drawCircle(Color.White.copy(alpha = 0.9f), r * 0.14f, Offset(c.x + dx, top + r * 0.72f))
            scope.drawCircle(pupil, r * 0.07f, Offset(c.x + dx, top + r * 0.72f))
        }
    }

    private fun drawWispCore(scope: DrawScope, c: Offset, r: Float, phase: Float, frightened: Boolean) {
        val col = if (frightened) Color(0xFFFF5252) else Color(0xFFB9F6CA)
        scope.drawCircle(col.copy(alpha = 0.9f), r * 0.22f + sin(phase * 3f) * r * 0.04f, c)
    }

    private fun drawInkDotEyes(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color) {
        listOf(-r * 0.2f, r * 0.2f).forEach { dx ->
            scope.drawCircle(pupil, r * 0.08f, Offset(c.x + dx, top + r * 0.65f))
        }
    }

    private fun drawErrorEyes(scope: DrawScope, c: Offset, r: Float, top: Float) {
        scope.drawRect(Color.White, Offset(c.x - r * 0.35f, top + r * 0.55f), Size(r * 0.7f, r * 0.18f))
    }

    private fun drawHourglassEye(scope: DrawScope, c: Offset, r: Float, phase: Float, pupil: Color) {
        scope.drawCircle(pupil, r * 0.1f, Offset(c.x, c.y - r * 0.05f + sin(phase) * r * 0.03f))
    }

    private fun drawGlitchEye(scope: DrawScope, c: Offset, r: Float, pupil: Color, phase: Float) {
        scope.drawRect(pupil, Offset(c.x - r * 0.12f + sin(phase * 4f) * 2f, c.y - r * 0.08f), Size(r * 0.24f, r * 0.08f))
    }

    private fun drawBeadEyes(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color, phase: Float) {
        scope.drawCircle(pupil, r * 0.11f, Offset(c.x + sin(phase) * r * 0.05f, top + r * 0.55f))
    }

    private fun drawNodeEye(scope: DrawScope, c: Offset, r: Float, pupil: Color, phase: Float) {
        scope.drawCircle(Color.White.copy(alpha = 0.85f), r * 0.2f, c)
        scope.drawCircle(pupil, r * 0.1f, c)
        scope.drawCircle(Color(0xFF00E676).copy(alpha = 0.6f), r * 0.35f + sin(phase) * r * 0.05f, c)
    }

    private fun drawStoneEyes(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color) {
        listOf(-r * 0.18f, r * 0.18f).forEach { dx ->
            scope.drawRoundRect(pupil, Offset(c.x + dx - r * 0.08f, top + r * 0.62f), Size(r * 0.16f, r * 0.1f), CornerRadius(2f))
        }
    }

    private fun drawStackEyes(scope: DrawScope, c: Offset, r: Float, top: Float, pupil: Color) {
        scope.drawRect(pupil, Offset(c.x - r * 0.15f, top + r * 0.5f), Size(r * 0.3f, r * 0.06f))
    }
}
