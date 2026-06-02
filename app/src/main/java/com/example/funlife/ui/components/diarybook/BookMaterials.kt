// ═══════════════════════════════════════════════════════════════════════════
// BookMaterials.kt — 魔法书共享「材质级」绘制工具
//
// 把扁平纯色描边 / 填充升级为有反光的金属、刻面宝石、柔光、凸压描金文字。
// 六本书的专属封面都复用这些工具，保证统一的高级质感。
//
// 安全约束：所有 radius/size/strokeWidth 一律 coerceAtLeast(1f) 防 NaN 崩溃；
//          侧面绝不画纯白大面积高光（防深背景反白 → 杜绝白色光晕复发）。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.domain.skin.BookSkin

/* ── 颜色微调：向白/黑插值，生成高光与暗部 ────────────────────────────── */

/** 向白色插值 [f]∈0..1，生成金属高光色。 */
fun Color.lighten(f: Float): Color = Color(
    red + (1f - red) * f,
    green + (1f - green) * f,
    blue + (1f - blue) * f,
    alpha,
)

/** 向黑色插值 [f]∈0..1，生成金属暗部色。 */
fun Color.darken(f: Float): Color = Color(
    red * (1f - f),
    green * (1f - f),
    blue * (1f - f),
    alpha,
)

/* ── 金属描边框：对角线性渐变(高光→主色→暗)模拟斜射光的金属边 ────────── */

fun DrawScope.drawMetallicFrame(
    topLeft: Offset, size: Size, strokeWidth: Float,
    base: Color, accent: Color,
    cornerRadius: Float = 0f, alpha: Float = 1f,
) {
    if (size.width < 1f || size.height < 1f) return
    val sw = strokeWidth.coerceAtLeast(1f)
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                base.lighten(0.45f).copy(alpha = alpha),   // 左上高光
                base.copy(alpha = alpha),
                accent.copy(alpha = alpha),                // 右下暗金
                base.copy(alpha = alpha * 0.85f),
            ),
            start = topLeft,
            end = Offset(topLeft.x + size.width, topLeft.y + size.height),
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = sw),
    )
}

/* ── 金属圆环：sweepGradient 让光从两个对角位置反射，立体金属感 ───────── */

fun DrawScope.drawMetallicRing(
    center: Offset, radius: Float, strokeWidth: Float,
    base: Color, accent: Color, alpha: Float = 1f,
) {
    if (radius < 1f) return
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                accent.copy(alpha = alpha),
                base.lighten(0.5f).copy(alpha = alpha),
                base.copy(alpha = alpha),
                accent.copy(alpha = alpha),
                base.lighten(0.4f).copy(alpha = alpha),
                base.copy(alpha = alpha),
                accent.copy(alpha = alpha),
            ),
            center = center,
        ),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth.coerceAtLeast(1f)),
    )
}

/* ── 刻面宝石：底色 + 高光点 + 暗角 + 金属环描边，模拟镶嵌的小宝石 ────── */

fun DrawScope.drawGemstone(
    center: Offset, radius: Float,
    gem: Color, ringColor: Color, ringStroke: Float = 0f,
) {
    val r = radius.coerceAtLeast(1f)
    // 宝石本体：径向渐变(中心亮 → 边缘深)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                gem.lighten(0.55f),
                gem,
                gem.darken(0.45f),
            ),
            center = Offset(center.x - r * 0.25f, center.y - r * 0.25f),
            radius = r * 1.15f,
        ),
        radius = r,
        center = center,
    )
    // 高光小点(左上)
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = (r * 0.28f).coerceAtLeast(0.5f),
        center = Offset(center.x - r * 0.32f, center.y - r * 0.32f),
    )
    // 金属环
    if (ringStroke > 0f) {
        drawMetallicRing(center, r, ringStroke, ringColor, ringColor.darken(0.4f))
    }
}

/* ── 柔光晕：皮肤色径向辉光(绝不用纯白)，给徽记/法阵打底发光 ─────────── */

fun DrawScope.softGlow(center: Offset, radius: Float, color: Color, intensity: Float = 0.4f) {
    val r = radius.coerceAtLeast(1f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = intensity),
                color.copy(alpha = intensity * 0.4f),
                Color.Transparent,
            ),
            center = center,
            radius = r,
        ),
        radius = r,
        center = center,
    )
}

/* ── 凸压描金文字：暗影 + 金属主体(渐变) + 高光，模拟烫金压印 ──────────── */

fun DrawScope.drawEmbossText(
    text: String, cx: Float, cy: Float, textSize: Float,
    base: Color, accent: Color,
    bold: Boolean = true,
    shadowAlpha: Float = 0.55f,
    highlightAlpha: Float = 0.42f,
    letterSpacing: Float = 0f,
) {
    val nc = drawContext.canvas.nativeCanvas
    val tf = android.graphics.Typeface.create(
        android.graphics.Typeface.SERIF,
        if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL,
    )
    val off = textSize * 0.04f
    // [1] 暗影(右下偏移)
    val shadow = android.graphics.Paint().apply {
        color = Color.Black.copy(alpha = shadowAlpha).toArgb()
        this.textSize = textSize
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.CENTER
        if (letterSpacing != 0f) this.letterSpacing = letterSpacing
    }
    nc.drawText(text, cx + off, cy + off, shadow)
    // [2] 金属主体(竖向渐变 shader)
    val main = android.graphics.Paint().apply {
        this.textSize = textSize
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.CENTER
        if (letterSpacing != 0f) this.letterSpacing = letterSpacing
        shader = android.graphics.LinearGradient(
            cx, cy - textSize * 0.8f, cx, cy + textSize * 0.2f,
            intArrayOf(base.lighten(0.4f).toArgb(), base.toArgb(), accent.toArgb()),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
    }
    nc.drawText(text, cx, cy, main)
    // [3] 高光(左上偏移、半透明)
    if (highlightAlpha > 0f) {
        val hi = android.graphics.Paint().apply {
            color = Color.White.copy(alpha = highlightAlpha).toArgb()
            this.textSize = textSize
            isAntiAlias = true
            typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
            if (letterSpacing != 0f) this.letterSpacing = letterSpacing
        }
        nc.drawText(text, cx - off * 0.6f, cy - off * 0.6f, hi)
    }
}

/** 晴川早春 · 奶油樱粉题签（无深色底，玫瑰金框 + 居中署名）。 */
private fun DrawScope.drawQingChuanOwnerColophon(
    skin: BookSkin, text: String, cx: Float, cy: Float, textSize: Float,
) {
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val pink = Color(0xFFFFC7D0)
    val pinkDeep = Color(0xFFC86B7A)
    val cream = Color(0xFFFFF8F2)
    val sz = textSize * 1.12f

    val measurePaint = android.graphics.Paint().apply {
        this.textSize = sz
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD,
        )
        textAlign = android.graphics.Paint.Align.LEFT
    }
    val textW = measurePaint.measureText(text)
    val fm = measurePaint.fontMetrics
    val textH = fm.descent - fm.ascent
    val halfW = textW / 2f

    val padH = sz * 0.30f
    val padV = sz * 0.22f
    val pillW = textW + padH * 2f
    val pillH = textH + padV * 2f
    val pillLeft = cx - pillW / 2f
    val pillTop = cy - pillH / 2f
    val corner = (pillH / 2f).coerceAtMost(pillW / 2f)
    val textLeftX = cx - textW / 2f
    val textBaselineY = cy - (fm.ascent + fm.descent) / 2f

    softGlow(Offset(cx, cy), halfW + padH, pink, 0.22f)
    softGlow(Offset(cx, cy), halfW + padH * 0.6f, foil, 0.10f)

    drawRoundRect(
        brush = Brush.radialGradient(
            listOf(
                cream.copy(alpha = 0.92f),
                pink.copy(alpha = 0.55f),
                cream.copy(alpha = 0.75f),
            ),
            center = Offset(cx, cy - pillH * 0.08f),
            radius = pillW * 0.72f,
        ),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(corner, corner),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            start = Offset(pillLeft, pillTop),
            end = Offset(pillLeft, pillTop + pillH * 0.45f),
        ),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH * 0.5f),
        cornerRadius = CornerRadius(corner, corner),
    )

    val frameSw = (sz * 0.045f).coerceAtLeast(0.7f)
    drawMetallicFrame(
        Offset(pillLeft + 1f, pillTop + 1f),
        Size(pillW - 2f, pillH - 2f),
        frameSw, foil, accent,
        cornerRadius = corner * 0.94f, alpha = 0.88f,
    )

    listOf(
        Offset(pillLeft + padH * 0.35f, pillTop + padV * 0.40f),
        Offset(pillLeft + pillW - padH * 0.35f, pillTop + padV * 0.40f),
        Offset(pillLeft + padH * 0.35f, pillTop + pillH - padV * 0.40f),
        Offset(pillLeft + pillW - padH * 0.35f, pillTop + pillH - padV * 0.40f),
    ).forEachIndexed { i, pt ->
        drawBookSakura(pt, sz * 0.055f, rotDeg = i * 90f, light = pink, base = pinkDeep, stroke = foil.copy(alpha = 0.6f))
    }

    val sw = (sz * 0.022f).coerceAtLeast(0.45f)
    val ornHalfW = halfW + padH * 0.30f
    drawColophonOrnament(cx, pillTop + padV * 0.50f, ornHalfW * 0.88f, foil.lighten(0.12f), sw)
    drawColophonOrnament(cx, pillTop + pillH - padV * 0.48f, ornHalfW * 0.75f, foil, sw * 0.85f)

    val nc = drawContext.canvas.nativeCanvas
    val tf = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    val strokeW = (sz * 0.09f).coerceAtLeast(1.0f)
    val outline = android.graphics.Paint().apply {
        color = pinkDeep.darken(0.25f).copy(alpha = 0.55f).toArgb()
        this.textSize = sz
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.LEFT
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeW
    }
    nc.drawText(text, textLeftX, textBaselineY, outline)
    val main = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            textLeftX, textBaselineY - sz * 0.7f, textLeftX, textBaselineY + sz * 0.15f,
            intArrayOf(foil.lighten(0.45f).toArgb(), pinkDeep.darken(0.15f).toArgb(), foil.toArgb()),
            floatArrayOf(0f, 0.55f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        this.textSize = sz
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.LEFT
    }
    nc.drawText(text, textLeftX, textBaselineY, main)
}

fun DrawScope.drawCoverOwnerName(
    skin: BookSkin,
    text: String,
    cx: Float,
    cy: Float,
    textSize: Float,
) {
    if (text.isBlank()) return
    if (skin.id.raw == "builtin::qingchuan") {
        drawQingChuanOwnerColophon(skin, text, cx, cy, textSize)
        return
    }
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val sz = textSize * 1.10f

    // 用真实字宽 / 字高布局，避免「字数估算」导致胶囊比文字宽、视觉偏左
    val measurePaint = android.graphics.Paint().apply {
        this.textSize = sz
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD,
        )
        textAlign = android.graphics.Paint.Align.LEFT
    }
    val textW = measurePaint.measureText(text)
    val fm = measurePaint.fontMetrics
    val textH = fm.descent - fm.ascent
    val halfW = textW / 2f

    val padH = sz * 0.26f
    val padV = sz * 0.18f
    val pillW = textW + padH * 2f
    val pillH = textH + padV * 2f
    val pillLeft = cx - pillW / 2f
    val pillTop = cy - pillH / 2f
    val corner = (pillH / 2f).coerceAtMost(pillW / 2f)

    val textLeftX = cx - textW / 2f
    val textBaselineY = cy - (fm.ascent + fm.descent) / 2f

    // 1. 双层魔法辉光（以胶囊中心为准）
    softGlow(Offset(cx, cy), halfW + padH * 0.8f, seal, 0.16f)
    softGlow(Offset(cx, cy), halfW + padH * 0.5f, foil, 0.11f)

    // 2. 琉璃胶囊底
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.55f),
                Color.Black.copy(alpha = 0.44f),
                Color.Black.copy(alpha = 0.18f),
            ),
            center = Offset(cx, cy),
            radius = pillW * 0.62f,
        ),
        topLeft = Offset(pillLeft, pillTop),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(corner, corner),
    )

    // 3. 双层烫金描边框
    val frameSw = (sz * 0.048f).coerceAtLeast(0.75f)
    drawMetallicFrame(
        Offset(pillLeft + 1.5f, pillTop + 1.5f),
        Size(pillW - 3f, pillH - 3f),
        frameSw, foil, accent,
        cornerRadius = corner * 0.94f, alpha = 0.92f,
    )
    drawRoundRect(
        brush = Brush.sweepGradient(
            listOf(
                foil.copy(alpha = 0.55f),
                Color.Transparent,
                accent.copy(alpha = 0.35f),
                Color.Transparent,
                foil.copy(alpha = 0.45f),
            ),
            center = Offset(cx, cy),
        ),
        topLeft = Offset(pillLeft + padH * 0.45f, pillTop + padV * 0.35f),
        size = Size(pillW - padH * 0.9f, pillH - padV * 0.7f),
        cornerRadius = CornerRadius(corner * 0.86f, corner * 0.86f),
        style = Stroke(width = (sz * 0.020f).coerceAtLeast(0.45f)),
    )

    // 4. 左右对称金翼（锚在真实半宽）
    drawColophonWings(cx, cy, halfW + padH * 0.15f, sz, foil, accent)

    // 5. 上下菱星饰线（与胶囊等宽）
    val sw = (sz * 0.024f).coerceAtLeast(0.45f)
    val ornHalfW = halfW + padH * 0.35f
    drawColophonOrnament(cx, pillTop + padV * 0.55f, ornHalfW * 0.92f, foil.lighten(0.15f), sw)
    drawColophonOrnament(cx, pillTop + pillH - padV * 0.45f, ornHalfW * 0.78f, foil, sw * 0.88f)

    // 6. 两侧灵点
    listOf(-1f, 1f).forEach { sgn ->
        val px = cx + sgn * (halfW + padH * 0.55f)
        drawCircle(foil.copy(alpha = 0.80f), (sz * 0.048f).coerceAtLeast(0.85f), Offset(px, cy - sz * 0.04f))
        drawCircle(Color.White.copy(alpha = 0.50f), (sz * 0.022f).coerceAtLeast(0.45f), Offset(px - sgn * sz * 0.02f, cy - sz * 0.08f))
    }

    // 7. 描边 + 烈金凸压（LEFT 对齐 + 实测字宽，几何居中）
    drawOutlinedMagicText(text, textLeftX, textBaselineY, sz, foil, accent)

    // 8. 底部灵珠坠
    drawGemstone(
        Offset(cx, pillTop + pillH + sz * 0.14f),
        (sz * 0.075f).coerceAtLeast(1.6f),
        seal.lighten(0.30f), foil, (sz * 0.020f).coerceAtLeast(0.4f),
    )
}

/** 左右对称金翼曲线，魔法书题签专用。 */
private fun DrawScope.drawColophonWings(
    cx: Float, cy: Float, halfW: Float, sz: Float, foil: Color, accent: Color,
) {
    val sw = (sz * 0.028f).coerceAtLeast(0.55f)
    for (sgn in listOf(-1f, 1f)) {
        val xIn = cx + sgn * halfW * 0.90f
        val xOut = cx + sgn * (halfW + sz * 0.50f)
        val wing = Path().apply {
            moveTo(xIn, cy)
            quadraticBezierTo(cx + sgn * (halfW + sz * 0.36f), cy - sz * 0.46f, xOut, cy - sz * 0.05f)
            quadraticBezierTo(cx + sgn * (halfW + sz * 0.36f), cy + sz * 0.42f, xIn, cy)
        }
        drawPath(
            wing,
            Brush.linearGradient(
                listOf(foil.copy(alpha = 0.85f), accent.copy(alpha = 0.55f), foil.copy(alpha = 0.35f)),
                start = Offset(xIn, cy - sz * 0.3f),
                end = Offset(xOut, cy + sz * 0.2f),
            ),
            style = Stroke(width = sw),
        )
    }
}

/** 魔法署名文字：LEFT 对齐 + 实测定位，与胶囊几何中心一致。 */
private fun DrawScope.drawOutlinedMagicText(
    text: String, textLeftX: Float, baselineY: Float, textSize: Float, foil: Color, accent: Color,
) {
    val nc = drawContext.canvas.nativeCanvas
    val tf = android.graphics.Typeface.create(
        android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD,
    )
    val strokeW = (textSize * 0.11f).coerceAtLeast(1.2f)
    val outline = android.graphics.Paint().apply {
        color = Color.Black.copy(alpha = 0.82f).toArgb()
        this.textSize = textSize
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.LEFT
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeW
    }
    nc.drawText(text, textLeftX, baselineY, outline)
    val warmEdge = android.graphics.Paint().apply {
        color = accent.darken(0.35f).copy(alpha = 0.65f).toArgb()
        this.textSize = textSize
        isAntiAlias = true
        typeface = tf
        textAlign = android.graphics.Paint.Align.LEFT
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeW * 0.42f
    }
    nc.drawText(text, textLeftX, baselineY, warmEdge)

    val textW = outline.measureText(text)
    val textCx = textLeftX + textW / 2f
    drawEmbossText(
        text = text,
        cx = textCx,
        cy = baselineY,
        textSize = textSize,
        base = foil.lighten(0.68f),
        accent = Color(0xFFFFF8E8),
        bold = true,
        shadowAlpha = 0.55f,
        highlightAlpha = 0.72f,
        letterSpacing = 0f,
    )
}

/* ── 藏书题签（横排）：上下金饰线 + 朱砂凸压，无暗色底块 ───────────────── */

fun DrawScope.drawColophonOrnament(cx: Float, y: Float, halfW: Float, foil: Color, stroke: Float) {
    val sw = stroke.coerceAtLeast(0.4f)
    drawLine(foil.copy(alpha = 0.48f), Offset(cx - halfW, y), Offset(cx - halfW * 0.18f, y), sw)
    drawLine(foil.copy(alpha = 0.48f), Offset(cx + halfW * 0.18f, y), Offset(cx + halfW, y), sw)
    val r = halfW * 0.07f
    val p = Path().apply {
        moveTo(cx, y - r); lineTo(cx + r, y); lineTo(cx, y + r); lineTo(cx - r, y); close()
    }
    drawPath(p, foil.copy(alpha = 0.62f), style = Stroke(width = sw * 0.85f))
}

fun DrawScope.drawOwnerColophon(
    text: String,
    cx: Float,
    cy: Float,
    textSize: Float,
    foil: Color,
    seal: Color,
    accent: Color,
) {
    if (text.isBlank()) return
    val halfW = textSize * text.length * 0.48f + textSize * 0.22f
    val sw = (textSize * 0.022f).coerceAtLeast(0.45f)
    softGlow(Offset(cx, cy), halfW * 1.1f, seal, 0.10f)
    drawColophonOrnament(cx, cy - textSize * 0.62f, halfW, foil, sw)
    drawEmbossText(
        text = text,
        cx = cx,
        cy = cy,
        textSize = textSize,
        base = seal.lighten(0.32f),
        accent = accent,
        bold = false,
        shadowAlpha = 0.48f,
        highlightAlpha = 0.36f,
        letterSpacing = 0.02f,
    )
    val botY = cy + textSize * 0.42f
    drawLine(foil.copy(alpha = 0.38f), Offset(cx - halfW * 0.75f, botY), Offset(cx + halfW * 0.75f, botY), sw)
    drawCircle(foil.copy(alpha = 0.55f), sw * 1.4f, Offset(cx, botY))
}

/* ── 朱砂方印：留白底 + 印面渐变 + 白边 + 印文，立体钤印感 ──────────────── */

fun DrawScope.drawSealStamp(
    center: Offset, sizePx: Float, sealColor: Color, glyph: String,
    backing: Color,
) {
    val half = (sizePx / 2f).coerceAtLeast(2f)
    val nc = drawContext.canvas.nativeCanvas
    // 留白底(印泥晕染)
    drawRoundRect(
        color = backing.copy(alpha = 0.6f),
        topLeft = Offset(center.x - half - 4f, center.y - half - 4f),
        size = Size(sizePx + 8f, sizePx + 8f),
        cornerRadius = CornerRadius(6f, 6f),
    )
    // 印面(径向渐变，中心略亮)
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(sealColor.lighten(0.18f), sealColor, sealColor.darken(0.25f)),
            center = center,
            radius = sizePx * 0.8f,
        ),
        topLeft = Offset(center.x - half, center.y - half),
        size = Size(sizePx, sizePx),
        cornerRadius = CornerRadius(3f, 3f),
    )
    // 内白边
    drawRoundRect(
        color = Color.White.copy(alpha = 0.42f),
        topLeft = Offset(center.x - half + 3f, center.y - half + 3f),
        size = Size(sizePx - 6f, sizePx - 6f),
        style = Stroke(width = 1.2f),
        cornerRadius = CornerRadius(2f, 2f),
    )
    // 印文
    val p = android.graphics.Paint().apply {
        color = Color.White.copy(alpha = 0.95f).toArgb()
        textSize = sizePx * 0.62f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD,
        )
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val fm = p.fontMetrics
    nc.drawText(glyph, center.x, center.y - (fm.ascent + fm.descent) / 2f, p)
}

/* ── 放射光芒：从中心向外的 N 条渐细金线(日芒/星芒) ────────────────────── */

fun DrawScope.drawRays(
    center: Offset, innerR: Float, outerR: Float, count: Int,
    color: Color, strokeWidth: Float, alpha: Float = 0.6f, angleOffset: Float = 0f,
) {
    val n = count.coerceAtLeast(1)
    for (i in 0 until n) {
        val ang = (i * (Math.PI * 2 / n) + angleOffset).toDouble()
        val cs = kotlin.math.cos(ang).toFloat()
        val sn = kotlin.math.sin(ang).toFloat()
        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(center.x + cs * innerR, center.y + sn * innerR),
            end = Offset(center.x + cs * outerR, center.y + sn * outerR),
            strokeWidth = strokeWidth.coerceAtLeast(1f),
        )
    }
}

/* ── 绒布/皮革封面基底：交叉纹理 + 径向暗角 + 斜射高光 ─────────────────── */

/** 在 [drawCoverBase] 渐变之上叠加皮革交叉纹与体积感。 */
fun DrawScope.drawLeatherGrain(skin: BookSkin) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val foil = skin.palette.foil.base
    val materials = skin.materials
    val alpha = materials.leatherNoiseAlpha * 0.55f

    val step = (w * 0.018f).coerceIn(2f, 5f)
    var x = 0f
    while (x < w) {
        drawLine(
            color = foil.copy(alpha = alpha * 0.35f),
            start = Offset(x, 0f), end = Offset(x, h),
            strokeWidth = 0.4f,
        )
        x += step
    }
    var y = 0f
    while (y < h) {
        drawLine(
            color = Color.Black.copy(alpha = alpha * 0.25f),
            start = Offset(0f, y), end = Offset(w, y),
            strokeWidth = 0.35f,
        )
        y += step * 1.15f
    }
    val count = (materials.leatherNoiseCount / 35).coerceIn(60, 180)
    for (i in 0 until count) {
        val sx = ((i * 9301 + 49297) % 233280) / 233280f
        val sy = ((i * 12289 + 33191) % 233280) / 233280f
        val r = 0.4f + (i % 4) * 0.25f
        val a = alpha * (0.5f + (i % 7) / 7f * 0.5f)
        drawCircle(foil.copy(alpha = a), r, Offset(w * sx, h * sy))
    }
}

/** 封面体积感：四角压暗 + 左上斜射暖高光（皮肤色，非纯白）。 */
fun DrawScope.drawCoverVolume(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, palette.coverShadow.copy(alpha = 0.45f)),
            center = Offset(w / 2f, h / 2f),
            radius = w * 0.72f,
        ),
    )
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(foil.copy(alpha = 0.14f), foil.copy(alpha = 0.05f), Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(w * 0.65f, h * 0.55f),
        ),
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            startX = w * 0.82f, endX = w,
        ),
        topLeft = Offset(w * 0.82f, 0f),
        size = Size(w * 0.18f, h),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
            startY = h * 0.88f, endY = h,
        ),
        topLeft = Offset(0f, h * 0.88f),
        size = Size(w, h * 0.12f),
    )
}

/** 四角金属包角（L 形烫金护角）。 */
fun DrawScope.drawCornerBrackets(
    foil: Color, accent: Color, inset: Float, arm: Float, stroke: Float,
) {
    val w = size.width
    val h = size.height
    val sw = stroke.coerceAtLeast(1f)
    val corners = listOf(
        Offset(inset, inset) to Pair(1f, 1f),
        Offset(w - inset, inset) to Pair(-1f, 1f),
        Offset(inset, h - inset) to Pair(1f, -1f),
        Offset(w - inset, h - inset) to Pair(-1f, -1f),
    )
    corners.forEach { (c, dir) ->
        val (dx, dy) = dir
        drawLine(foil, c, Offset(c.x + dx * arm, c.y), strokeWidth = sw)
        drawLine(foil, c, Offset(c.x, c.y + dy * arm), strokeWidth = sw)
        drawLine(accent.copy(alpha = 0.7f),
            Offset(c.x + dx * arm * 0.15f, c.y + dy * arm * 0.15f),
            Offset(c.x + dx * arm * 0.85f, c.y + dy * arm * 0.15f),
            strokeWidth = sw * 0.55f)
        drawLine(accent.copy(alpha = 0.7f),
            Offset(c.x + dx * arm * 0.15f, c.y + dy * arm * 0.15f),
            Offset(c.x + dx * arm * 0.15f, c.y + dy * arm * 0.85f),
            strokeWidth = sw * 0.55f)
        drawCircle(foil.copy(alpha = 0.9f), sw * 1.2f, c)
    }
}

/** 底部弧形灵珠排（封底装饰）。 */
fun DrawScope.drawArcSpiritGems(
    count: Int, yBase: Float, arcHeight: Float,
    gemColor: Color, ringColor: Color, sizeScale: Float = 0.022f,
) {
    val w = size.width
    val h = size.height
    val n = count.coerceAtLeast(1)
    for (i in 0 until n) {
        val t = if (n == 1) 0.5f else i / (n - 1).toFloat()
        val gx = w * (0.18f + t * 0.64f)
        val gy = h * yBase - h * arcHeight * (1f - 4f * (t - 0.5f) * (t - 0.5f))
        val r = w * sizeScale * (0.85f + (1f - kotlin.math.abs(t - 0.5f) * 2f) * 0.25f)
        softGlow(Offset(gx, gy), r * 2.8f, gemColor, 0.35f)
        drawGemstone(Offset(gx, gy), r, gemColor, ringColor, r * 0.22f)
    }
}

