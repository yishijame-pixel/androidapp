// ═══════════════════════════════════════════════════════════════════════════
// BookFrontCovers.kt — 六本魔法书各自独立的「正面封面」设计
//
// 每本书一套专属布局（不再共用同一个法阵模板）：
//   · 蘅芜旧卷 → 如意云纹团徽 + 朱砂方印（文人藏书）
//   · 霁月长明 → 满月银盘 + 环绕月相 + 星座连线（月相天盘）
//   · 晴川早春 → 樱月圆徽 + 斜枝留白 + 居中书名/署名（玫瑰金和风）
//   · 赤焰天书 → 火凤日芒法阵 + 炽金放射（焚天熔炉）
//   · 青鸾翠竹 → 翠竹 + 青鸾环徽（翠竹青鸾）
//   · 星河长卷 → 星象罗盘 + 方位刻度环（航海星图）
//
// 共用 BookMaterials.kt 的金属/宝石/柔光/凸压工具，保证统一高级质感。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.domain.skin.BookSkin
import kotlin.math.cos
import kotlin.math.sin

private const val PI2 = (Math.PI * 2).toFloat()

/** 副标题 + 书名：书名烫金，署名题签紧贴书名下方。 */
private fun DrawScope.drawTitleBlock(
    skin: BookSkin, title: String,
    titleY: Float, titleScale: Float = 0.14f,
    subtitle: String? = "一  人  一  册", subtitleY: Float = 0.685f,
    subtitleScale: Float = 0.050f,
) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val titleSizePx = w * titleScale
    drawEmbossText(
        text = title, cx = w / 2f, cy = titleY, textSize = titleSizePx,
        base = foil, accent = skin.palette.foil.accent,
    )
    if (subtitle == null || subtitle.isBlank()) return

    val isPoeticDefault = subtitle.contains("  ")
    val subSize = w * subtitleScale
    // cy 传胶囊几何中心（非 baseline），便于与背景装饰对齐
    val subCenterY = if (isPoeticDefault) h * subtitleY else titleY + titleSizePx * 1.05f

    if (isPoeticDefault) {
        val nc = drawContext.canvas.nativeCanvas
        val subPaint = android.graphics.Paint().apply {
            color = foil.copy(alpha = 0.80f).toArgb()
            textSize = subSize * 0.92f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL,
            )
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = skin.typography.titleLetterSpacingEm.coerceAtMost(0.35f)
        }
        nc.drawText(subtitle, w / 2f, subCenterY, subPaint)
    } else {
        drawCoverOwnerName(
            skin = skin,
            text = subtitle,
            cx = w / 2f,
            cy = subCenterY,
            textSize = subSize,
        )
    }
}

// ── 1. 蘅芜旧卷 · 如意云纹团徽（文人藏书）──────────────────────────────
fun DrawScope.drawHengWuFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.34f

    // 双金属边框 + 回纹内框
    val pad = (w * 0.05f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2),
        w * 0.007f, foil, accent)
    val pad2 = pad + w * 0.022f
    drawMetallicFrame(Offset(pad2, pad2), Size(w - pad2 * 2, h - pad2 * 2),
        w * 0.004f, foil, accent, alpha = 0.6f)

    // 中央团徽：柔光打底 + 双金属环 + 如意四出云头
    softGlow(Offset(cx, cy), w * 0.22f, foil, 0.25f)
    drawMetallicRing(Offset(cx, cy), w * 0.155f, w * 0.008f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.115f, w * 0.005f, foil, accent, alpha = 0.7f)
    // 四出如意云头(上下左右)
    for (i in 0 until 4) {
        rotate(i * 90f, Offset(cx, cy)) {
            drawRuyiCloud(Offset(cx, cy - w * 0.135f), w * 0.085f, 0, foil, accent,
                (w * 0.005f).coerceAtLeast(0.7f))
        }
    }
    // 团徽中心朱砂小印
    drawSealStamp(Offset(cx, cy), w * 0.10f, skin.palette.seal, "蘅", skin.palette.cover.base)

    drawCornerBrackets(foil, accent, pad + w * 0.008f, w * 0.07f, w * 0.005f)

    // 四角云纹
    drawVineOrnaments(skin, w, h)

    // 标题 + 副标
    drawTitleBlock(skin, title, titleY = h * 0.62f, subtitle = author)
    // 底部双线 + 菱形
    drawBottomFlourish(skin)
}

// ── 2. 霁月长明 · 满月银盘 + 环绕月相（月相天盘）─────────────────────────
fun DrawScope.drawJiYueFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.34f

    val pad = (w * 0.05f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2),
        w * 0.007f, foil, accent)
    drawCornerBrackets(foil, accent, pad + w * 0.006f, w * 0.065f, w * 0.004f)

    // 中央满月银盘
    softGlow(Offset(cx, cy), w * 0.24f, skin.palette.paperFiber, 0.30f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(foil.lighten(0.5f), foil.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(cx - w * 0.04f, cy - w * 0.04f), radius = w * 0.14f,
        ),
        radius = w * 0.13f, center = Offset(cx, cy),
    )
    drawMetallicRing(Offset(cx, cy), w * 0.155f, w * 0.007f, foil, accent)

    // 环绕一圈 8 个月相小盘(朔→望→晦)
    val orbitR = w * 0.155f
    for (i in 0 until 8) {
        val ang = i * (PI2 / 8) - PI2 / 4
        val mx = cx + cos(ang) * (orbitR + w * 0.07f)
        val my = cy + sin(ang) * (orbitR + w * 0.07f)
        drawMoonPhase(Offset(mx, my), w * 0.026f, i / 8f, foil)
        // 星座连线(相邻)
        if (i > 0) {
            val pa = (i - 1) * (PI2 / 8) - PI2 / 4
            drawLine(foil.copy(alpha = 0.25f),
                Offset(cx + cos(pa) * (orbitR + w * 0.07f), cy + sin(pa) * (orbitR + w * 0.07f)),
                Offset(mx, my), strokeWidth = (w * 0.0025f).coerceAtLeast(0.5f))
        }
    }

    // 银色星点散布(上半部)
    drawScatterStars(skin, count = 14, foil, topBias = true)
    // 四角月相
    drawMoonOrnaments(skin, w, h)

    drawTitleBlock(skin, title, titleY = h * 0.64f, subtitle = author)
    drawBottomFlourish(skin)
}

/** 月相小盘：phase 0=满, 0.5=朔。用裁切错位圆模拟阴影。 */
internal fun DrawScope.drawMoonPhase(center: Offset, r: Float, phase: Float, foil: Color) {
    val rr = r.coerceAtLeast(1f)
    drawCircle(foil.copy(alpha = 0.85f), rr, center)
    // 阴影盖:按 phase 横向偏移一个深色圆
    val shadowOff = (phase - 0.0f) * rr * 2.2f - rr * 1.1f
    if (kotlin.math.abs(shadowOff) < rr * 2f) {
        drawCircle(Color.Black.copy(alpha = 0.65f), rr * 0.96f,
            Offset(center.x + shadowOff, center.y))
    }
    drawCircle(foil, rr, center, style = Stroke(width = (rr * 0.18f).coerceAtLeast(0.5f)))
}

// ── 3. 晴川早春 · 樱月圆徽 + 斜枝留白 + 居中书名/署名 ───────────────────
fun DrawScope.drawQingChuanFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val pink = Color(0xFFFFC7D0)
    val pinkDeep = Color(0xFFE89BAA)
    val cx = w / 2f

    // 书名区暖光（保证署名可读）
    softGlow(Offset(cx, h * 0.60f), w * 0.40f, pink, 0.16f)
    softGlow(Offset(cx, h * 0.60f), w * 0.28f, foil, 0.08f)

    // 双层玫瑰金框 + 角饰
    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent, alpha = 0.90f)
    val pad2 = pad + w * 0.016f
    drawMetallicFrame(Offset(pad2, pad2), Size(w - pad2 * 2, h - pad2 * 2), w * 0.003f, foil, accent, alpha = 0.50f)
    drawCornerBrackets(foil, accent, pad + w * 0.005f, w * 0.060f, w * 0.004f)

    // 顶部樱月圆徽
    val emblemY = h * 0.26f
    softGlow(Offset(cx, emblemY), w * 0.20f, pink, 0.18f)
    drawMetallicRing(Offset(cx, emblemY), w * 0.155f, w * 0.007f, foil, accent)
    drawMetallicRing(Offset(cx, emblemY), w * 0.118f, w * 0.004f, foil, accent, alpha = 0.70f)
    for (i in 0 until 5) {
        val ang = i * (PI2 / 5f) - (Math.PI / 2).toFloat()
        drawBookSakura(
            Offset(cx + cos(ang) * w * 0.095f, emblemY + sin(ang) * w * 0.095f),
            w * 0.028f, rotDeg = i * 72f, light = pink, base = pinkDeep, stroke = accent,
        )
    }
    drawBookSakura(Offset(cx, emblemY), w * 0.048f, rotDeg = 15f, light = pink, base = pinkDeep, stroke = foil)

    // 斜枝：仅走上半区，不穿过书名/署名
    val sw = (w * 0.0055f).coerceAtLeast(0.7f)
    val branch = Path().apply {
        moveTo(w * 0.10f, h * 0.88f)
        quadraticBezierTo(w * 0.32f, h * 0.68f, w * 0.48f, h * 0.52f)
        quadraticBezierTo(w * 0.62f, h * 0.38f, w * 0.88f, h * 0.14f)
    }
    drawPath(branch, foil.copy(alpha = 0.72f), style = Stroke(width = sw))
    drawLine(foil.copy(alpha = 0.65f), Offset(w * 0.44f, h * 0.54f), Offset(w * 0.32f, h * 0.46f), sw * 0.75f)
    drawLine(foil.copy(alpha = 0.65f), Offset(w * 0.58f, h * 0.40f), Offset(w * 0.72f, h * 0.36f), sw * 0.75f)
    listOf(
        Triple(0.38f, 0.48f, 0.070f),
        Triple(0.52f, 0.40f, 0.085f),
        Triple(0.68f, 0.32f, 0.065f),
        Triple(0.82f, 0.18f, 0.055f),
    ).forEachIndexed { i, (fx, fy, fs) ->
        drawBookSakura(Offset(w * fx, h * fy), w * fs, rotDeg = i * 38f, light = pink, base = pinkDeep, stroke = accent)
    }

    // 轻量飘落花瓣（四角，不遮挡文字）
    listOf(
        Offset(w * 0.18f, h * 0.22f), Offset(w * 0.78f, h * 0.28f),
        Offset(w * 0.14f, h * 0.72f), Offset(w * 0.82f, h * 0.68f),
    ).forEachIndexed { i, pt ->
        drawBookSakura(pt, w * 0.020f, rotDeg = i * 55f,
            light = pink.copy(alpha = 0.75f), base = pinkDeep.copy(alpha = 0.65f), stroke = foil.copy(alpha = 0.35f))
    }

    // 底部樱瓣饰带
    drawQingChuanBottomFlourish(w, h, foil, pink, pinkDeep, accent)

    // 居中书名 + 署名（与其他皮肤一致的清晰布局）
    drawTitleBlock(skin, title, titleY = h * 0.56f, titleScale = 0.132f, subtitle = author, subtitleScale = 0.066f)
}

/** 晴川早春封面底部樱瓣连珠饰带。 */
private fun DrawScope.drawQingChuanBottomFlourish(
    w: Float, h: Float, foil: Color, pink: Color, pinkDeep: Color, accent: Color,
) {
    val baseY = h * 0.88f
    val halfSpan = w * 0.28f
    val cx = w / 2f
    drawLine(foil.copy(alpha = 0.45f), Offset(cx - halfSpan, baseY), Offset(cx + halfSpan, baseY), 0.7f)
    for (i in -2..2) {
        val px = cx + i * w * 0.07f
        val r = w * 0.014f
        if (i == 0) {
            drawBookSakura(Offset(px, baseY), w * 0.032f, rotDeg = 0f, light = pink, base = pinkDeep, stroke = foil)
        } else {
            drawCircle(pink.copy(alpha = 0.70f), r, Offset(px, baseY))
            drawCircle(Color.White.copy(alpha = 0.40f), r * 0.4f, Offset(px - r * 0.2f, baseY - r * 0.2f))
        }
    }
    drawLine(foil.copy(alpha = 0.30f), Offset(cx - halfSpan * 0.5f, baseY + w * 0.02f),
        Offset(cx + halfSpan * 0.5f, baseY + w * 0.02f), 0.45f)
}

// ── 4. 赤焰天书 · 火凤日芒法阵（焚天熔炉）─────────────────────────────
fun DrawScope.drawChiYanFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.34f

    val pad = (w * 0.05f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2),
        w * 0.008f, foil, accent)
    drawCornerBrackets(foil, accent, pad + w * 0.006f, w * 0.075f, w * 0.005f)

    // 炽金日芒(双层放射，长短交错)
    softGlow(Offset(cx, cy), w * 0.30f, Color(0xFFFF7A1A), 0.35f)
    drawRays(Offset(cx, cy), w * 0.17f, w * 0.30f, 24, foil, w * 0.004f, 0.55f)
    drawRays(Offset(cx, cy), w * 0.17f, w * 0.25f, 24, accent, w * 0.006f, 0.7f,
        angleOffset = (Math.PI / 24).toFloat())

    // 法阵双金属环
    drawMetallicRing(Offset(cx, cy), w * 0.17f, w * 0.009f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.125f, w * 0.006f, foil, accent, alpha = 0.8f)

    // 中央火凤(三笔上扬火舌组成的凤形)
    drawPhoenix(Offset(cx, cy), w * 0.10f, foil, accent)

    // 四角火舌
    drawFlameOrnaments(skin, w, h)

    // 底部五颗朱砂火印灵珠（截图中的弧形光点）
    drawArcSpiritGems(
        count = 5, yBase = 0.88f, arcHeight = 0.035f,
        gemColor = Color(0xFFFF6020), ringColor = foil,
        sizeScale = 0.024f,
    )

    drawTitleBlock(skin, title, titleY = h * 0.64f, subtitle = author)
    drawBottomFlourish(skin)
}

/** 火凤简形：中心宝石 + 上扬双翼火舌 + 尾羽。 */
internal fun DrawScope.drawPhoenix(center: Offset, size: Float, foil: Color, accent: Color) {
    val s = size.coerceAtLeast(2f)
    val sw = (s * 0.10f).coerceAtLeast(1f)
    // 双翼(左右对称上扬曲线)
    for (sgn in listOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(center.x, center.y + s * 0.2f)
            quadraticBezierTo(center.x + sgn * s * 0.9f, center.y - s * 0.1f,
                center.x + sgn * s * 0.7f, center.y - s * 0.9f)
        }
        drawPath(wing, foil.copy(alpha = 0.9f), style = Stroke(width = sw))
        val wing2 = Path().apply {
            moveTo(center.x, center.y + s * 0.2f)
            quadraticBezierTo(center.x + sgn * s * 0.6f, center.y + s * 0.0f,
                center.x + sgn * s * 0.45f, center.y - s * 0.55f)
        }
        drawPath(wing2, accent.copy(alpha = 0.8f), style = Stroke(width = sw * 0.7f))
    }
    // 尾羽(下垂三笔)
    for (i in -1..1) {
        val tail = Path().apply {
            moveTo(center.x, center.y + s * 0.2f)
            quadraticBezierTo(center.x + i * s * 0.2f, center.y + s * 0.7f,
                center.x + i * s * 0.35f, center.y + s * 1.1f)
        }
        drawPath(tail, foil.copy(alpha = 0.75f), style = Stroke(width = sw * 0.6f))
    }
    // 凤首宝石
    drawGemstone(Offset(center.x, center.y - s * 0.2f), s * 0.28f,
        Color(0xFFFFD46A), foil, sw * 0.5f)
}

// ── 5. 青鸾翠竹 · 翠竹 + 青鸾环徽（翠竹青鸾）──────────────────────────
fun DrawScope.drawQingLuanFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val green = skin.palette.cover.accent
    val cx = w / 2f
    val cy = h * 0.34f

    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2),
        w * 0.006f, foil, accent)
    drawCornerBrackets(foil, accent, pad + w * 0.005f, w * 0.065f, w * 0.004f)

    // 中央青鸾环徽：银环 + 凤鸟(青鸾)
    softGlow(Offset(cx, cy), w * 0.20f, skin.palette.ribbon, 0.28f)
    drawMetallicRing(Offset(cx, cy), w * 0.15f, w * 0.007f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.115f, w * 0.004f, foil, accent, alpha = 0.6f)
    drawLuanBird(Offset(cx, cy + h * 0.01f), w * 0.11f, foil, accent)

    // 两侧翠竹枝(对称立于环徽两旁)
    drawBambooStalk(Offset(w * 0.16f, h * 0.55f), h * 0.34f, green, foil, leftLeaves = true)
    drawBambooStalk(Offset(w * 0.84f, h * 0.55f), h * 0.34f, green, foil, leftLeaves = false)

    // 四角竹枝
    drawBambooOrnaments(skin, w, h)

    drawTitleBlock(skin, title, titleY = h * 0.64f, subtitle = author)
    drawBottomFlourish(skin)
}

/** 青鸾(凤鸟)：昂首 + 展翅 + 长尾的银线勾勒。 */
internal fun DrawScope.drawLuanBird(center: Offset, size: Float, foil: Color, accent: Color) {
    val s = size.coerceAtLeast(2f)
    val sw = (s * 0.09f).coerceAtLeast(1f)
    // 身体(S 形颈+身)
    val body = Path().apply {
        moveTo(center.x - s * 0.1f, center.y - s * 0.7f)        // 头
        quadraticBezierTo(center.x + s * 0.2f, center.y - s * 0.3f,
            center.x - s * 0.05f, center.y + s * 0.2f)          // 颈→身
        quadraticBezierTo(center.x - s * 0.3f, center.y + s * 0.5f,
            center.x - s * 0.6f, center.y + s * 0.9f)           // 尾
    }
    drawPath(body, foil.copy(alpha = 0.92f), style = Stroke(width = sw))
    // 展翅(上扬)
    val wing = Path().apply {
        moveTo(center.x, center.y - s * 0.1f)
        quadraticBezierTo(center.x + s * 0.7f, center.y - s * 0.5f,
            center.x + s * 0.95f, center.y - s * 1.0f)
    }
    drawPath(wing, accent.copy(alpha = 0.85f), style = Stroke(width = sw * 0.85f))
    // 尾羽分叉
    for (i in 0..2) {
        val tail = Path().apply {
            moveTo(center.x - s * 0.55f, center.y + s * 0.85f)
            quadraticBezierTo(center.x - s * (0.8f + i * 0.15f), center.y + s * 1.0f,
                center.x - s * (0.7f + i * 0.25f), center.y + s * (1.3f + i * 0.12f))
        }
        drawPath(tail, foil.copy(alpha = 0.7f - i * 0.12f), style = Stroke(width = sw * 0.6f))
    }
    // 头部宝石眼 + 冠
    drawGemstone(Offset(center.x - s * 0.1f, center.y - s * 0.7f), s * 0.16f,
        Color(0xFFBCEBD2), foil, sw * 0.4f)
}


/** 立竹一竿：竹节 + 数片竹叶。 */
internal fun DrawScope.drawBambooStalk(
    base: Offset, height: Float, green: Color, foil: Color, leftLeaves: Boolean,
) {
    val sw = (height * 0.025f).coerceAtLeast(1.2f)
    val top = Offset(base.x, base.y - height)
    drawLine(green.copy(alpha = 0.85f), base, top, strokeWidth = sw)
    // 竹节
    val nodes = 4
    for (k in 1..nodes) {
        val y = base.y - height * k / (nodes + 1)
        drawLine(foil.copy(alpha = 0.7f), Offset(base.x - sw, y), Offset(base.x + sw, y),
            strokeWidth = sw * 0.7f)
    }
    // 竹叶(朝外侧)
    val dir = if (leftLeaves) -1f else 1f
    for (k in 0 until 3) {
        val y = base.y - height * (0.45f + k * 0.18f)
        val leaf = Path().apply {
            moveTo(base.x, y)
            quadraticBezierTo(base.x + dir * height * 0.18f, y - height * 0.10f,
                base.x + dir * height * 0.30f, y - height * 0.04f)
            quadraticBezierTo(base.x + dir * height * 0.18f, y + height * 0.02f, base.x, y)
        }
        drawPath(leaf, green.copy(alpha = 0.7f))
        drawPath(leaf, foil.copy(alpha = 0.4f), style = Stroke(width = sw * 0.4f))
    }
}

// ── 6. 星河长卷 · 星象罗盘（航海星图）────────────────────────────────
fun DrawScope.drawXingHeFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.34f

    val pad = (w * 0.05f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2),
        w * 0.007f, foil, accent)
    drawCornerBrackets(foil, accent, pad + w * 0.006f, w * 0.068f, w * 0.004f)

    // 星象罗盘：柔光 + 三层环 + 方位刻度 + 四方位星
    softGlow(Offset(cx, cy), w * 0.26f, skin.palette.ribbon, 0.30f)
    drawMetallicRing(Offset(cx, cy), w * 0.18f, w * 0.008f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.145f, w * 0.005f, foil, accent, alpha = 0.7f)
    drawMetallicRing(Offset(cx, cy), w * 0.06f, w * 0.005f, foil, accent, alpha = 0.8f)
    // 360° 方位刻度(每 15° 一条，主方位更长)
    for (i in 0 until 24) {
        val ang = i * (PI2 / 24)
        val isMajor = i % 6 == 0
        val inner = if (isMajor) w * 0.145f else w * 0.16f
        drawLine(foil.copy(alpha = if (isMajor) 0.85f else 0.4f),
            Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
            Offset(cx + cos(ang) * w * 0.18f, cy + sin(ang) * w * 0.18f),
            strokeWidth = (w * (if (isMajor) 0.005f else 0.002f)).coerceAtLeast(0.5f))
    }
    // 四方位指针(罗盘十字)
    for (i in 0 until 4) {
        val ang = i * (PI2 / 4) - PI2 / 4
        val tip = Offset(cx + cos(ang) * w * 0.145f, cy + sin(ang) * w * 0.145f)
        val baseL = Offset(cx + cos(ang + 1.4f) * w * 0.03f, cy + sin(ang + 1.4f) * w * 0.03f)
        val baseR = Offset(cx + cos(ang - 1.4f) * w * 0.03f, cy + sin(ang - 1.4f) * w * 0.03f)
        val needle = Path().apply {
            moveTo(tip.x, tip.y); lineTo(baseL.x, baseL.y); lineTo(baseR.x, baseR.y); close()
        }
        drawPath(needle, foil.copy(alpha = if (i == 0) 0.9f else 0.55f))
    }
    // 中心宝石
    drawGemstone(Offset(cx, cy), w * 0.03f, skin.palette.ribbon, foil, w * 0.004f)

    // 星座连线(罗盘外侧散布几颗亮星 + 连线)
    drawConstellation(skin, cx, cy, w * 0.20f)
    drawScatterStars(skin, count = 16, foil, topBias = false)

    // 四角星座
    drawStarsOrnaments(skin, w, h)

    drawTitleBlock(skin, title, titleY = h * 0.64f, subtitle = author)
    drawBottomFlourish(skin)
}

/** 罗盘外的星座：5 颗亮星按弧线排布 + 连线。 */
internal fun DrawScope.drawConstellation(skin: BookSkin, cx: Float, cy: Float, r: Float) {
    val foil = skin.palette.foil.base
    val pts = listOf(
        Offset(cx - r * 1.3f, cy - r * 0.5f),
        Offset(cx - r * 0.9f, cy - r * 1.0f),
        Offset(cx + r * 0.9f, cy - r * 1.05f),
        Offset(cx + r * 1.35f, cy - r * 0.4f),
        Offset(cx + r * 1.1f, cy + r * 0.4f),
    )
    for (i in 0 until pts.size - 1) {
        drawLine(foil.copy(alpha = 0.3f), pts[i], pts[i + 1],
            strokeWidth = (cx * 0.004f).coerceAtLeast(0.5f))
    }
    pts.forEachIndexed { i, p ->
        val rr = (r * (0.06f - i * 0.006f)).coerceAtLeast(1f)
        drawCircle(foil, rr, p)
        drawCircle(foil.copy(alpha = 0.35f), rr * 2.4f, p)
    }
}

/* ── 共享小工具 ──────────────────────────────────────────────────────── */

/** 底部装饰：双横线 + 菱形(金属)。 */
private fun DrawScope.drawBottomFlourish(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    drawLine(foil.copy(alpha = 0.6f), Offset(w * 0.32f, h * 0.84f), Offset(w * 0.68f, h * 0.84f),
        strokeWidth = (w * 0.0035f).coerceAtLeast(0.6f))
    drawLine(foil.copy(alpha = 0.4f), Offset(w * 0.38f, h * 0.86f), Offset(w * 0.62f, h * 0.86f),
        strokeWidth = (w * 0.0025f).coerceAtLeast(0.5f))
    val cx = w / 2f; val cy = h * 0.92f; val r = w * 0.018f
    val p = Path().apply {
        moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
    }
    drawPath(p, foil.copy(alpha = 0.7f), style = Stroke(width = (w * 0.003f).coerceAtLeast(0.5f)))
    drawCircle(foil.copy(alpha = 0.85f), (w * 0.006f).coerceAtLeast(1f), Offset(cx, cy))
}

/** 散布星点(避开中央法阵区)。 */
internal fun DrawScope.drawScatterStars(skin: BookSkin, count: Int, foil: Color, topBias: Boolean) {
    val w = size.width
    val h = size.height
    val cx = w / 2f; val cy = h * 0.34f
    for (i in 0 until count) {
        val sx = ((i * 7919 + 31337) % 233280) / 233280f
        val sy = ((i * 6133 + 49297) % 233280) / 233280f
        val px = w * (0.10f + sx * 0.80f)
        val py = if (topBias) h * (0.08f + sy * 0.30f) else h * (0.10f + sy * 0.60f)
        val dx = px - cx; val dy = py - cy
        if (dx * dx + dy * dy < (w * 0.22f) * (w * 0.22f)) continue
        val a = 0.3f + ((i % 5) / 5f) * 0.5f
        val r = (w * 0.004f).coerceAtLeast(0.7f)
        drawCircle(foil.copy(alpha = a), r, Offset(px, py))
        if (i % 3 == 0) drawCircle(foil.copy(alpha = a * 0.4f), r * 2.6f, Offset(px, py))
    }
}


