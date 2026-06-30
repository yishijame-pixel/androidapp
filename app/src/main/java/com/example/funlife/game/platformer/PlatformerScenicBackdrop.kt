package com.example.funlife.game.platformer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

/** 1–6 关 Goodly 主题手绘远景（多层视差 + 大气渐变）。 */
object PlatformerScenicBackdrop {

    fun draw(
        scope: DrawScope,
        level: PlatformerLevelDef,
        viewportW: Float,
        viewportH: Float,
        camX: Float,
        scale: Float,
        animTime: Float,
    ) {
        if (level.id !in 1..6 && !PlatformerCampaignVisuals.showsScenicBackdrop(level)) return
        when (level.theme) {
            PlatformerTheme.GRASS -> drawGrass(scope, level, viewportW, viewportH, camX, scale, animTime)
            PlatformerTheme.METAL -> drawDungeon(scope, level, viewportW, viewportH, camX, scale, animTime)
            PlatformerTheme.DESERT -> drawDesert(scope, level, viewportW, viewportH, camX, scale, animTime)
            PlatformerTheme.SPOOKY -> drawSpooky(scope, level, viewportW, viewportH, camX, scale, animTime)
            PlatformerTheme.ICE -> drawIce(scope, level, viewportW, viewportH, camX, scale, animTime)
            PlatformerTheme.FORTRESS -> drawFortress(scope, level, viewportW, viewportH, camX, scale, animTime)
            else -> drawGrass(scope, level, viewportW, viewportH, camX, scale, animTime)
        }
    }

    private fun drawGrass(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        sun(scope, vw, vh, 0.88f, 0.14f, Color(0xFFFFF176), 0.11f)
        clouds(scope, vw, vh, t, alpha = 0.88f)
        mountainLayer(scope, vw, vh, camX, scale, 0.06f, Color(0xFF7CB87C), 0.58f, 0.22f, seed = 1)
        mountainLayer(scope, vw, vh, camX, scale, 0.11f, Color(0xFF5FA85F), 0.64f, 0.28f, seed = 3)
        treeSilhouettes(scope, vw, vh, camX, scale, 0.16f, Color(0xFF4A8F4A), count = 9)
        groundGlow(scope, vw, vh, Color(0xFF8BC34A))
    }

    private fun drawDungeon(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        scope.drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF3D3D55).copy(alpha = 0.5f), Color.Transparent),
                center = Offset(vw * 0.5f, vh * 0.2f),
                radius = vw * 0.7f,
            ),
            size = Size(vw, vh),
        )
        repeat(8) { i ->
            val px = ((i * 180f - camX * scale * 0.08f) % (vw + 140f)) - 70f
            val ph = vh * (0.35f + (i % 3) * 0.08f)
            scope.drawRect(
                color = Color(0xFF1A1A28).copy(alpha = 0.75f),
                topLeft = Offset(px, vh * 0.12f),
                size = Size(28f * scale.coerceIn(1f, 2.5f), ph),
            )
            scope.drawRect(
                color = Color(0xFF2A2A3E),
                topLeft = Offset(px + 6f, vh * 0.12f + ph * 0.15f),
                size = Size(10f, 14f),
            )
        }
        torchFlicker(scope, vw, vh, t)
        groundGlow(scope, vw, vh, Color(0xFF4A4A5A))
    }

    private fun drawDesert(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        sun(scope, vw, vh, 0.82f, 0.18f, Color(0xFFFFD54F), 0.14f)
        heatHaze(scope, vw, vh, t)
        duneLayer(scope, vw, vh, camX, scale, 0.08f, Color(0xFFD4A574), 0.68f, 0.2f)
        duneLayer(scope, vw, vh, camX, scale, 0.14f, Color(0xFFC4956A), 0.72f, 0.26f)
        cactusSilhouettes(scope, vw, vh, camX, scale, 0.12f)
        groundGlow(scope, vw, vh, Color(0xFFE8C07A))
    }

    private fun drawSpooky(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        moon(scope, vw, vh)
        fogLayer(scope, vw, vh, t, Color(0xFF1B263B).copy(alpha = 0.35f))
        deadTrees(scope, vw, vh, camX, scale, 0.1f)
        mountainLayer(scope, vw, vh, camX, scale, 0.07f, Color(0xFF0F172A), 0.55f, 0.3f, seed = 5)
        groundGlow(scope, vw, vh, Color(0xFF2D3A4F))
    }

    private fun drawIce(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        aurora(scope, vw, vh, t)
        mountainLayer(scope, vw, vh, camX, scale, 0.05f, Color(0xFFB8D4E8), 0.5f, 0.24f, seed = 2)
        mountainLayer(scope, vw, vh, camX, scale, 0.1f, Color(0xFF9EC5DB), 0.58f, 0.3f, seed = 4)
        snowflakes(scope, vw, vh, t)
        groundGlow(scope, vw, vh, Color(0xFFE8F4FC))
    }

    private fun drawFortress(
        scope: DrawScope, level: PlatformerLevelDef,
        vw: Float, vh: Float, camX: Float, scale: Float, t: Float,
    ) {
        skyGradient(scope, vw, vh, Color(level.skyTop), Color(level.skyBottom))
        clouds(scope, vw, vh, t * 0.6f, alpha = 0.65f)
        castleSilhouettes(scope, vw, vh, camX, scale, 0.09f)
        mountainLayer(scope, vw, vh, camX, scale, 0.08f, Color(0xFF6B8E6B), 0.6f, 0.25f, seed = 6)
        treeSilhouettes(scope, vw, vh, camX, scale, 0.14f, Color(0xFF558B55), count = 6)
        groundGlow(scope, vw, vh, Color(0xFF9CCC65))
    }

    private fun skyGradient(scope: DrawScope, vw: Float, vh: Float, top: Color, bottom: Color) {
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(top, bottom, bottom.copy(alpha = 0.85f)),
                startY = 0f,
                endY = vh,
            ),
            size = Size(vw, vh),
        )
    }

    private fun sun(scope: DrawScope, vw: Float, vh: Float, fx: Float, fy: Float, core: Color, radiusFrac: Float) {
        val cx = vw * fx
        val cy = vh * fy
        val r = vw * radiusFrac
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(core, core.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r,
            ),
            radius = r,
            center = Offset(cx, cy),
        )
    }

    private fun moon(scope: DrawScope, vw: Float, vh: Float) {
        val cx = vw * 0.78f
        val cy = vh * 0.16f
        val r = vw * 0.045f
        scope.drawCircle(Color(0xFFE8EAF6), r, Offset(cx, cy))
        scope.drawCircle(Color(0xFF0D1B2A), r * 0.85f, Offset(cx + r * 0.35f, cy - r * 0.15f))
    }

    private fun clouds(scope: DrawScope, vw: Float, vh: Float, t: Float, alpha: Float) {
        repeat(5) { i ->
            val size = vw * (0.1f + i * 0.02f)
            val x = ((i * 240f + t * (22f + i * 6f)) % (vw + size * 2f)) - size
            val y = vh * (0.1f + (i % 3) * 0.06f)
            val c = Color.White.copy(alpha = alpha - i * 0.06f)
            scope.drawCircle(c, size * 0.28f, Offset(x, y))
            scope.drawCircle(c, size * 0.34f, Offset(x + size * 0.28f, y - size * 0.06f))
            scope.drawCircle(c, size * 0.3f, Offset(x + size * 0.52f, y))
        }
    }

    private fun mountainLayer(
        scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float,
        parallaxMul: Float, color: Color, baseYFrac: Float, heightFrac: Float, seed: Int,
    ) {
        val parallax = camX * scale * parallaxMul
        val baseY = vh * baseYFrac
        val h = vh * heightFrac
        val w = vw * 0.65f
        var x = -parallax % (w * 0.85f) - w * 0.2f
        var idx = 0
        while (x < vw + w) {
            val path = Path().apply {
                moveTo(x, baseY + h)
                lineTo(x + w * 0.18f, baseY + h * 0.35f + (idx + seed) % 3 * 12f)
                lineTo(x + w * 0.38f, baseY + h * 0.55f)
                lineTo(x + w * 0.55f, baseY + h * 0.2f)
                lineTo(x + w * 0.72f, baseY + h * 0.48f)
                lineTo(x + w * 0.9f, baseY + h * 0.32f)
                lineTo(x + w, baseY + h)
                close()
            }
            scope.drawPath(path, color.copy(alpha = 0.55f + (idx % 2) * 0.12f))
            x += w * 0.72f
            idx++
        }
    }

    private fun duneLayer(
        scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float,
        parallaxMul: Float, color: Color, baseYFrac: Float, heightFrac: Float,
    ) {
        val parallax = camX * scale * parallaxMul
        val baseY = vh * baseYFrac
        val h = vh * heightFrac
        val w = vw * 0.8f
        var x = -parallax % w - w * 0.3f
        while (x < vw + w) {
            val path = Path().apply {
                moveTo(x, baseY + h)
                quadraticBezierTo(x + w * 0.25f, baseY, x + w * 0.5f, baseY + h * 0.35f)
                quadraticBezierTo(x + w * 0.78f, baseY + h * 0.7f, x + w, baseY + h)
                close()
            }
            scope.drawPath(path, color.copy(alpha = 0.7f))
            x += w * 0.85f
        }
    }

    private fun treeSilhouettes(
        scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float,
        parallaxMul: Float, color: Color, count: Int,
    ) {
        val parallax = camX * scale * parallaxMul
        val baseY = vh * 0.72f
        repeat(count) { i ->
            val tx = ((i * (vw / count) - parallax) % (vw + 80f)) - 40f
            val th = vh * (0.12f + (i % 3) * 0.03f)
            val tw = th * 0.55f
            scope.drawOval(color.copy(alpha = 0.55f), Offset(tx, baseY - th), Size(tw * 2f, th * 1.1f))
            scope.drawRect(color.copy(alpha = 0.65f), Offset(tx + tw * 0.85f, baseY - th * 0.25f), Size(tw * 0.3f, th * 0.35f))
        }
    }

    private fun groundGlow(scope: DrawScope, vw: Float, vh: Float, tint: Color) {
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, tint.copy(alpha = 0.18f), tint.copy(alpha = 0.32f)),
                startY = vh * 0.55f,
                endY = vh,
            ),
            size = Size(vw, vh),
        )
    }

    private fun heatHaze(scope: DrawScope, vw: Float, vh: Float, t: Float) {
        repeat(3) { i ->
            val y = vh * (0.45f + i * 0.08f) + sin(t * 2f + i) * 4f
            scope.drawRect(
                color = Color.White.copy(alpha = 0.04f),
                topLeft = Offset(0f, y),
                size = Size(vw, 8f),
            )
        }
    }

    private fun cactusSilhouettes(scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float, parallaxMul: Float) {
        val parallax = camX * scale * parallaxMul
        val baseY = vh * 0.74f
        repeat(5) { i ->
            val x = ((i * 160f - parallax) % (vw + 60f)) - 30f
            val h = vh * 0.1f
            scope.drawRoundRect(
                Color(0xFF6D8B3C).copy(alpha = 0.7f),
                Offset(x, baseY - h),
                Size(h * 0.35f, h),
                androidx.compose.ui.geometry.CornerRadius(4f),
            )
        }
    }

    private fun fogLayer(scope: DrawScope, vw: Float, vh: Float, t: Float, color: Color) {
        repeat(4) { i ->
            val y = vh * (0.5f + i * 0.06f) + sin(t + i) * 6f
            scope.drawRect(color, Offset(0f, y), Size(vw, vh * 0.08f))
        }
    }

    private fun deadTrees(scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float, parallaxMul: Float) {
        val parallax = camX * scale * parallaxMul
        repeat(6) { i ->
            val x = ((i * 130f - parallax) % (vw + 50f)) - 25f
            val base = vh * 0.7f
            val h = vh * 0.14f
            scope.drawLine(Color(0xFF1B263B), Offset(x, base), Offset(x, base - h), strokeWidth = 4f)
            scope.drawLine(Color(0xFF1B263B), Offset(x, base - h * 0.6f), Offset(x - 12f, base - h * 0.85f), strokeWidth = 3f)
            scope.drawLine(Color(0xFF1B263B), Offset(x, base - h * 0.75f), Offset(x + 14f, base - h), strokeWidth = 3f)
        }
    }

    private fun aurora(scope: DrawScope, vw: Float, vh: Float, t: Float) {
        val wave = sin(t * 0.8f) * 20f
        scope.drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF80CBC4).copy(alpha = 0.25f),
                    Color(0xFF4DD0E1).copy(alpha = 0.18f),
                    Color.Transparent,
                ),
                start = Offset(0f, vh * 0.1f + wave),
                end = Offset(vw, vh * 0.35f),
            ),
            size = Size(vw, vh * 0.4f),
        )
    }

    private fun snowflakes(scope: DrawScope, vw: Float, vh: Float, t: Float) {
        repeat(20) { i ->
            val sx = (i * 53f + t * (18f + i % 5)) % vw
            val sy = (i * 37f + t * (10f + i % 3)) % vh
            scope.drawCircle(Color.White.copy(alpha = 0.5f), radius = 1.5f + (i % 3), center = Offset(sx, sy))
        }
    }

    private fun castleSilhouettes(scope: DrawScope, vw: Float, vh: Float, camX: Float, scale: Float, parallaxMul: Float) {
        val parallax = camX * scale * parallaxMul
        repeat(3) { i ->
            val x = ((i * 280f - parallax) % (vw + 120f)) - 60f
            val base = vh * 0.68f
            val h = vh * 0.16f
            scope.drawRect(Color(0xFF5D6D7E).copy(alpha = 0.55f), Offset(x, base - h), Size(h * 1.6f, h))
            scope.drawRect(Color(0xFF5D6D7E).copy(alpha = 0.55f), Offset(x + h * 0.3f, base - h * 1.25f), Size(h * 0.35f, h * 0.35f))
            scope.drawRect(Color(0xFF5D6D7E).copy(alpha = 0.55f), Offset(x + h * 1f, base - h * 1.15f), Size(h * 0.35f, h * 0.35f))
        }
    }

    private fun torchFlicker(scope: DrawScope, vw: Float, vh: Float, t: Float) {
        repeat(4) { i ->
            val x = vw * (0.15f + i * 0.22f)
            val y = vh * 0.42f
            val flicker = 0.7f + 0.3f * sin(t * 8f + i * 2f)
            scope.drawCircle(
                Color(0xFFFF8F00).copy(alpha = 0.35f * flicker),
                radius = 18f * flicker,
                center = Offset(x, y),
            )
        }
    }
}
