// ═══════════════════════════════════════════════════════════════════════════
// ChineseDragonArt.kt — 中国传统侧视神龙（墨龙 / 金描）
//
// 特征：鹿角、牛鼻、虎爪、蛇身、鱼鳞、马尾；无西方翼；S 形穿云。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
enum class ChineseDragonDetail { Emblem, Ambient }

data class ChineseDragonColors(
    val ink: Color = Color(0xFF0D0A08),
    val body: Color = Color(0xFF1A1410),
    val gold: Color = Color(0xFFD4AF37),
    val goldLight: Color = Color(0xFFFFE9A8),
    val accent: Color = Color(0xFF8B6914),
    val eye: Color = Color(0xFFFFCC33),
)

/**
 * 绘制中国传统神龙。
 *
 * @param bounds 龙形占位区域（相对当前 DrawScope）
 * @param phase 0..∞ 缓慢摆动相位（Hub 传 t，封面传 0）
 * @param facingRight 龙头朝右
 */
fun DrawScope.drawChineseDragon(
    bounds: Rect,
    colors: ChineseDragonColors,
    phase: Float = 0f,
    facingRight: Boolean = true,
    detail: ChineseDragonDetail = ChineseDragonDetail.Ambient,
    bodyAlpha: Float = 1f,
) {
    val wave = sin(phase * 1.15f) * bounds.width * 0.012f
    val wave2 = cos(phase * 0.85f) * bounds.height * 0.01f

    translate(bounds.left, bounds.top) {
        scale(
            scaleX = if (facingRight) bounds.width else -bounds.width,
            scaleY = bounds.height,
            pivot = Offset.Zero,
        ) {
            val bodyPath = buildDragonBodyPath(wave / bounds.width, wave2 / bounds.height)
            val a = bodyAlpha.coerceIn(0f, 1f)

            // 墨身 + 金边
            drawPath(
                bodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.body.copy(alpha = 0.92f * a),
                        colors.ink.copy(alpha = 0.88f * a),
                        colors.body.copy(alpha = 0.85f * a),
                    ),
                    start = Offset(0.1f, 0.2f),
                    end = Offset(0.9f, 0.8f),
                ),
            )
            drawPath(
                bodyPath,
                colors.gold.copy(alpha = (if (detail == ChineseDragonDetail.Emblem) 0.95f else 0.55f) * a),
                style = Stroke(
                    width = if (detail == ChineseDragonDetail.Emblem) 0.014f else 0.008f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            drawDragonScales(bodyPath, colors, detail, a)
            drawDragonMane(colors, phase, a)
            drawDragonHead(colors, phase, detail, a)
            drawDragonLegs(colors, phase, detail, a)
            drawDragonWhiskers(colors, a)
            drawDragonHorns(colors, a)
            if (detail == ChineseDragonDetail.Emblem) {
                drawFlamingPearl(colors, phase, a)
            }
            drawDragonClawsAccent(colors, detail, a)
        }
    }
}

/** 闭合龙身轮廓（归一化 0..1 坐标，龙头在左） */
private fun buildDragonBodyPath(waveX: Float, waveY: Float): Path {
    fun oy(y: Float) = y + waveY
    fun ox(x: Float) = x + waveX * (1f - x)

    return Path().apply {
        // 从鼻尖沿背上缘至尾
        moveTo(ox(0.06f), oy(0.46f))
        cubicTo(ox(0.04f), oy(0.40f), ox(0.08f), oy(0.34f), ox(0.14f), oy(0.36f))
        cubicTo(ox(0.20f), oy(0.38f), ox(0.22f), oy(0.32f), ox(0.30f), oy(0.30f))
        cubicTo(ox(0.38f), oy(0.28f), ox(0.42f), oy(0.38f), ox(0.50f), oy(0.40f))
        cubicTo(ox(0.58f), oy(0.42f), ox(0.62f), oy(0.30f), ox(0.70f), oy(0.32f))
        cubicTo(ox(0.78f), oy(0.34f), ox(0.82f), oy(0.44f), ox(0.88f), oy(0.42f))
        cubicTo(ox(0.94f), oy(0.40f), ox(0.98f), oy(0.38f), ox(1.00f), oy(0.36f))
        // 尾鳍
        lineTo(ox(0.97f), oy(0.44f))
        cubicTo(ox(0.90f), oy(0.50f), ox(0.84f), oy(0.48f), ox(0.78f), oy(0.46f))
        // 腹线回龙头
        cubicTo(ox(0.70f), oy(0.44f), ox(0.64f), oy(0.54f), ox(0.54f), oy(0.56f))
        cubicTo(ox(0.44f), oy(0.58f), ox(0.36f), oy(0.52f), ox(0.28f), oy(0.54f))
        cubicTo(ox(0.20f), oy(0.56f), ox(0.16f), oy(0.52f), ox(0.12f), oy(0.50f))
        cubicTo(ox(0.08f), oy(0.48f), ox(0.06f), oy(0.48f), ox(0.06f), oy(0.46f))
        close()
    }
}

private fun DrawScope.drawDragonHead(
    colors: ChineseDragonColors,
    phase: Float,
    detail: ChineseDragonDetail,
    alpha: Float,
) {
    val blink = 0.75f + 0.25f * sin(phase * 2.2f)
    val eyeR = if (detail == ChineseDragonDetail.Emblem) 0.022f else 0.018f
    val ex = 0.13f; val ey = 0.41f

    // 吻部高光
    drawPath(
        Path().apply {
            moveTo(0.05f, 0.46f)
            quadraticBezierTo(0.08f, 0.44f, 0.11f, 0.45f)
        },
        colors.goldLight.copy(alpha = 0.7f * alpha),
        style = Stroke(0.006f, cap = StrokeCap.Round),
    )

    // 龙眼
    drawCircle(colors.eye.copy(alpha = blink * alpha), eyeR * 1.8f, Offset(ex, ey))
    drawCircle(colors.ink.copy(alpha = 0.9f * alpha), eyeR, Offset(ex + eyeR * 0.3f, ey))
    drawCircle(Color.White.copy(alpha = 0.85f * alpha), eyeR * 0.35f, Offset(ex - eyeR * 0.2f, ey - eyeR * 0.25f))

    // 鼻须鼓包
    drawCircle(colors.gold.copy(alpha = 0.35f * alpha), 0.012f, Offset(0.09f, 0.47f))
}

private fun DrawScope.drawDragonHorns(colors: ChineseDragonColors, alpha: Float) {
    val horn = Path().apply {
        moveTo(0.15f, 0.35f)
        quadraticBezierTo(0.12f, 0.22f, 0.18f, 0.18f)
        quadraticBezierTo(0.20f, 0.26f, 0.17f, 0.34f)
        close()
    }
    drawPath(horn, colors.gold.copy(alpha = 0.85f * alpha))
    drawPath(horn, colors.goldLight.copy(alpha = 0.4f * alpha), style = Stroke(0.004f))

    val horn2 = Path().apply {
        moveTo(0.17f, 0.36f)
        quadraticBezierTo(0.16f, 0.26f, 0.21f, 0.24f)
        lineTo(0.19f, 0.35f)
        close()
    }
    drawPath(horn2, colors.gold.copy(alpha = 0.7f * alpha))
}

private fun DrawScope.drawDragonWhiskers(colors: ChineseDragonColors, alpha: Float) {
    val stroke = Stroke(0.0035f, cap = StrokeCap.Round)
    listOf(
        floatArrayOf(0.07f, 0.47f, 0.02f, 0.44f),
        floatArrayOf(0.07f, 0.48f, 0.01f, 0.50f),
        floatArrayOf(0.07f, 0.49f, 0.03f, 0.54f),
    ).forEach { (x0, y0, x1, y1) ->
        drawPath(
            Path().apply {
                moveTo(x0, y0)
                quadraticBezierTo((x0 + x1) / 2f - 0.02f, (y0 + y1) / 2f, x1, y1)
            },
            colors.goldLight.copy(alpha = 0.75f * alpha),
            style = stroke,
        )
    }
}

private fun DrawScope.drawDragonMane(colors: ChineseDragonColors, phase: Float, alpha: Float) {
    val sway = sin(phase * 1.4f) * 0.008f
    val pts = floatArrayOf(
        0.18f, 0.34f, 0.26f, 0.28f, 0.34f, 0.32f, 0.48f, 0.36f,
        0.58f, 0.30f, 0.68f, 0.33f, 0.76f, 0.38f,
    )
    for (i in pts.indices step 2) {
        val x = pts[i]; val y = pts[i + 1] + sway * (i / 2)
        val spike = Path().apply {
            moveTo(x, y)
            lineTo(x - 0.015f, y - 0.04f)
            lineTo(x + 0.008f, y - 0.02f)
            close()
        }
        drawPath(spike, colors.gold.copy(alpha = 0.55f * alpha))
    }
}

private fun DrawScope.drawDragonLegs(
    colors: ChineseDragonColors,
    phase: Float,
    detail: ChineseDragonDetail,
    alpha: Float,
) {
    val legW = if (detail == ChineseDragonDetail.Emblem) 0.012f else 0.009f
    val legs = arrayOf(
        floatArrayOf(0.28f, 0.52f, -0.02f),
        floatArrayOf(0.42f, 0.54f, 0.01f),
        floatArrayOf(0.58f, 0.50f, -0.015f),
        floatArrayOf(0.72f, 0.48f, 0.012f),
    )
    legs.forEachIndexed { idx, (lx, ly, droop) ->
        val bob = sin(phase * 1.8f + idx * 1.2f) * 0.006f
        val footY = ly + droop + bob + 0.06f
        drawPath(
            Path().apply {
                moveTo(lx, ly)
                quadraticBezierTo(lx - 0.01f, ly + 0.04f, lx - 0.015f, footY)
            },
            colors.body.copy(alpha = 0.9f * alpha),
            style = Stroke(legW, cap = StrokeCap.Round),
        )
        drawDragonFoot(Offset(lx - 0.018f, footY), colors, alpha, detail)
    }
}

private fun DrawScope.drawDragonFoot(
    center: Offset,
    colors: ChineseDragonColors,
    alpha: Float,
    detail: ChineseDragonDetail,
) {
    val s = if (detail == ChineseDragonDetail.Emblem) 1.15f else 1f
    val clawLen = 0.018f * s
    for (a in -40..40 step 40) {
        val rad = Math.toRadians(a.toDouble() + 110.0)
        val ex = center.x + cos(rad).toFloat() * clawLen
        val ey = center.y + sin(rad).toFloat() * clawLen
        drawLine(
            colors.gold.copy(alpha = 0.9f * alpha),
            center,
            Offset(ex, ey),
            strokeWidth = 0.0035f * s,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(colors.gold.copy(alpha = 0.5f * alpha), 0.008f * s, center)
}

private fun DrawScope.drawDragonClawsAccent(
    colors: ChineseDragonColors,
    detail: ChineseDragonDetail,
    alpha: Float,
) {
    if (detail != ChineseDragonDetail.Emblem) return
    drawCircle(colors.goldLight.copy(alpha = 0.25f * alpha), 0.025f, Offset(0.26f, 0.58f))
}

private fun DrawScope.drawFlamingPearl(colors: ChineseDragonColors, phase: Float, alpha: Float) {
    val pulse = 0.7f + 0.3f * sin(phase * 2.5f)
    val cx = 0.22f; val cy = 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.95f * alpha * pulse),
                colors.eye.copy(alpha = 0.85f * alpha * pulse),
                colors.gold.copy(alpha = 0.4f * alpha),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = 0.045f,
        ),
        radius = 0.045f,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawDragonScales(
    spine: Path,
    colors: ChineseDragonColors,
    detail: ChineseDragonDetail,
    alpha: Float,
) {
    val measure = PathMeasure()
    measure.setPath(spine, true)
    val len = measure.length
    if (len <= 0f) return
    val count = when (detail) {
        ChineseDragonDetail.Emblem -> 48
        ChineseDragonDetail.Ambient -> 32
    }
    for (i in 1 until count - 2) {
        val d = len * i / count
        val position = measure.getPosition(d)
        val tangent = measure.getTangent(d)
        val angle = atan2(tangent.y, tangent.x) * 180f / PI.toFloat() + 90f
        val px = position.x; val py = position.y
        if (px < 0.12f || px > 0.92f) continue
        rotate(angle, Offset(px, py)) {
            val scaleW = 0.012f
            drawPath(
                Path().apply {
                    moveTo(px - scaleW, py)
                    quadraticBezierTo(px, py - scaleW * 0.8f, px + scaleW, py)
                },
                colors.gold.copy(alpha = 0.45f * alpha),
                style = Stroke(0.0025f, cap = StrokeCap.Round),
            )
        }
    }
}

/** 祥云托足（Ambient 用） */
fun DrawScope.drawDragonClouds(
    bounds: Rect,
    colors: ChineseDragonColors,
    phase: Float,
    alpha: Float = 0.5f,
) {
    translate(bounds.left, bounds.top) {
        scale(bounds.width, bounds.height, pivot = Offset.Zero) {
            val clouds = arrayOf(
                floatArrayOf(0.22f, 0.62f, 0.08f),
                floatArrayOf(0.48f, 0.64f, 0.10f),
                floatArrayOf(0.72f, 0.60f, 0.09f),
            )
            clouds.forEachIndexed { i, (cx, cy, r) ->
                val drift = sin(phase * 0.9f + i * 2f) * 0.015f
                drawCloudPuff(Offset(cx + drift, cy), r, colors, alpha)
            }
        }
    }
}

private fun DrawScope.drawCloudPuff(center: Offset, radius: Float, colors: ChineseDragonColors, alpha: Float) {
    val c = colors.gold.copy(alpha = 0.12f * alpha)
    drawCircle(c, radius * 0.5f, center)
    drawCircle(c, radius * 0.38f, Offset(center.x - radius * 0.35f, center.y + radius * 0.08f))
    drawCircle(c, radius * 0.38f, Offset(center.x + radius * 0.35f, center.y + radius * 0.08f))
    drawCircle(colors.goldLight.copy(alpha = 0.08f * alpha), radius * 0.25f, center)
}
