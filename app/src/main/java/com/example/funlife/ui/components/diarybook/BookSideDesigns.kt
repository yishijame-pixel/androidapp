// ═══════════════════════════════════════════════════════════════════════════
// BookSideDesigns.kt — 魔法书侧面专属设计（书脊 / 页摞 / 顶底切口）
//
// · 书脊：装帧脊带 + 竖排书名 + 用户署名烫印（朱砂/金属）
// · 页摞：古籍纸叠 + 皮肤主题纹样（不刻署名）
// · 顶底：切口金边 + 皮肤点缀
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
import kotlin.math.cos
import kotlin.math.sin

/* ── 书脊 · 六皮肤专属装帧（dispatch → 各皮肤独立工艺）────────────────── */

fun DrawScope.drawSpineVerticalEnhanced(
    skin: BookSkin,
    bookTitle: String,
    ownerNameRaw: String,
) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return

    when (skin.id.raw) {
        "builtin::hengwu"    -> drawHengWuSpine(skin, bookTitle, ownerNameRaw)
        "builtin::jiyue"     -> drawJiYueSpine(skin, bookTitle, ownerNameRaw)
        "builtin::qingchuan" -> drawQingChuanSpine(skin, bookTitle, ownerNameRaw)
        "builtin::chiyan"    -> drawChiYanSpine(skin, bookTitle, ownerNameRaw)
        "builtin::qingluan"  -> drawQingLuanSpine(skin, bookTitle, ownerNameRaw)
        "builtin::xinghe"    -> drawXingHeSpine(skin, bookTitle, ownerNameRaw)
        else                 -> drawHengWuSpine(skin, bookTitle, ownerNameRaw)
    }
}

/* ── 共享装帧基础层 ───────────────────────────────────────────────────── */

private fun DrawScope.drawSpineLeatherBody(skin: BookSkin, w: Float, h: Float) {
    val palette = skin.palette
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                palette.coverShadow.darken(0.18f),
                palette.spine.base.darken(0.06f),
                palette.spine.base,
                palette.spine.accent.lighten(0.10f),
                palette.spine.base,
                palette.spine.base.darken(0.06f),
                palette.coverShadow.darken(0.18f),
            ),
        ),
    )
    // 中央弧面高光（圆柱书脊）
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent, Color.Transparent,
                palette.foil.base.copy(alpha = 0.05f),
                palette.foil.base.copy(alpha = 0.14f),
                palette.foil.base.copy(alpha = 0.05f),
                Color.Transparent, Color.Transparent,
            ),
            startX = w * 0.22f, endX = w * 0.78f,
        ),
        topLeft = Offset(w * 0.22f, h * 0.04f),
        size = Size(w * 0.56f, h * 0.92f),
    )
    // 皮革细纹
    for (i in 0 until 18) {
        val y = h * (0.06f + i * 0.052f)
        val a = 0.08f + (i % 5) * 0.025f
        drawLine(
            palette.coverShadow.copy(alpha = a),
            Offset(w * 0.12f, y), Offset(w * 0.88f, y), 0.4f,
        )
    }
}

private fun DrawScope.drawSpineRaisedRails(
    w: Float, h: Float, foil: Color, shadow: Color, railWidth: Float = 0.11f,
) {
    listOf(w * 0.09f, w * 0.91f).forEach { cx ->
        drawLine(shadow.copy(alpha = 0.65f), Offset(cx, h * 0.03f), Offset(cx, h * 0.97f), w * railWidth)
        drawLine(foil.copy(alpha = 0.55f), Offset(cx, h * 0.04f), Offset(cx, h * 0.96f), w * railWidth * 0.38f)
        // 脊绳节（每隔一段一个结）
        for (i in 0 until 5) {
            val ky = h * (0.14f + i * 0.17f)
            drawCircle(foil.copy(alpha = 0.70f), w * 0.028f, Offset(cx, ky))
            drawCircle(foil.lighten(0.30f).copy(alpha = 0.35f), w * 0.012f,
                Offset(cx - w * 0.006f, ky - w * 0.006f))
        }
    }
}

private fun DrawScope.drawSpineCapPlate(
    skin: BookSkin, w: Float, h: Float, cy: Float, foil: Color, accent: Color,
) {
    val plateH = h * 0.11f
    val plateTop = cy - plateH / 2f
    drawSpinePlateBevel(
        w * 0.14f, plateTop, w * 0.72f, plateH,
        skin.palette.spine.base.darken(0.12f), foil, skin.palette.coverShadow,
        cornerR = w * 0.04f,
    )
    drawSpineEmblem(skin, Offset(w / 2f, cy), w * 0.22f)
    // 上下双线勒边
    listOf(plateTop + 1.5f, plateTop + plateH - 1.5f).forEach { ly ->
        drawLine(foil.copy(alpha = 0.45f), Offset(w * 0.18f, ly), Offset(w * 0.82f, ly), 0.55f)
    }
}

private fun DrawScope.drawSpinePlateBevel(
    left: Float, top: Float, width: Float, height: Float,
    base: Color, highlight: Color, shadow: Color, cornerR: Float,
) {
    val cr = CornerRadius(cornerR.coerceAtLeast(1f), cornerR.coerceAtLeast(1f))
    drawRoundRect(
        Color.Black.copy(alpha = 0.30f),
        topLeft = Offset(left + 0.8f, top + 1.2f),
        size = Size(width, height),
        cornerRadius = cr,
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(base.lighten(0.14f), base, base.darken(0.10f), base.darken(0.18f)),
            start = Offset(left, top),
            end = Offset(left + width, top + height),
        ),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = cr,
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(highlight.copy(alpha = 0.35f), Color.Transparent),
            start = Offset(left, top),
            end = Offset(left, top + height * 0.35f),
        ),
        topLeft = Offset(left + 1f, top + 1f),
        size = Size(width - 2f, height * 0.38f),
        cornerRadius = cr,
    )
    drawRoundRect(
        highlight.copy(alpha = 0.55f),
        topLeft = Offset(left + 2f, top + 2f),
        size = Size(width - 4f, height - 4f),
        cornerRadius = CornerRadius(cornerR * 0.88f, cornerR * 0.88f),
        style = Stroke(width = 0.65f),
    )
    drawRoundRect(
        shadow.copy(alpha = 0.40f),
        topLeft = Offset(left + 4f, top + 4f),
        size = Size(width - 8f, height - 8f),
        cornerRadius = CornerRadius(cornerR * 0.75f, cornerR * 0.75f),
        style = Stroke(width = 0.45f),
    )
}

private fun DrawScope.drawSpinePlateCornerOrnaments(
    skin: BookSkin, left: Float, top: Float, width: Float, height: Float,
    foil: Color, accent: Color,
) {
    val s = kotlin.math.min(width, height) * 0.14f
    val corners = listOf(
        Offset(left + s * 0.6f, top + s * 0.6f),
        Offset(left + width - s * 0.6f, top + s * 0.6f),
        Offset(left + s * 0.6f, top + height - s * 0.6f),
        Offset(left + width - s * 0.6f, top + height - s * 0.6f),
    )
    when (skin.id.raw) {
        "builtin::xinghe" -> corners.forEach { pt ->
            drawCircle(foil.copy(alpha = 0.75f), s * 0.18f, pt)
            for (i in 0 until 4) {
                val ang = i * (Math.PI / 2).toFloat() + (Math.PI / 4).toFloat()
                drawLine(foil.copy(alpha = 0.50f), pt,
                    Offset(pt.x + cos(ang) * s * 0.35f, pt.y + sin(ang) * s * 0.35f), 0.45f)
            }
        }
        "builtin::jiyue" -> corners.forEach { pt ->
            drawCircle(foil.copy(alpha = 0.60f), s * 0.22f, pt, style = Stroke(0.55f))
            drawCircle(accent.copy(alpha = 0.70f), s * 0.08f, pt)
        }
        "builtin::chiyan" -> corners.forEach { pt ->
            for (i in 0 until 3) {
                val ang = i * 2.09f
                drawLine(Color(0xFFFF7040).copy(alpha = 0.55f), pt,
                    Offset(pt.x + cos(ang) * s * 0.4f, pt.y + sin(ang) * s * 0.4f), 0.5f)
            }
        }
        "builtin::qingluan" -> corners.forEachIndexed { i, pt ->
            val leaf = Path().apply {
                moveTo(pt.x, pt.y + s * 0.3f)
                quadraticBezierTo(pt.x + (if (i % 2 == 0) 1 else -1) * s * 0.35f, pt.y,
                    pt.x + (if (i % 2 == 0) 1 else -1) * s * 0.2f, pt.y - s * 0.35f)
            }
            drawPath(leaf, accent.copy(alpha = 0.65f), style = Stroke(0.55f))
        }
        "builtin::qingchuan" -> corners.forEach { pt ->
            drawBookSakura(pt, s * 0.28f, rotDeg = 0f,
                light = Color(0xFFFFC7D0), base = Color(0xFFE89BAA), stroke = foil.copy(alpha = 0.6f))
        }
        else -> corners.forEach { pt ->
            val p = Path().apply {
                moveTo(pt.x, pt.y - s * 0.35f); lineTo(pt.x + s * 0.35f, pt.y)
                lineTo(pt.x, pt.y + s * 0.35f); lineTo(pt.x - s * 0.35f, pt.y); close()
            }
            drawPath(p, foil.copy(alpha = 0.55f), style = Stroke(0.55f))
        }
    }
}

private fun spineTitleOwner(skin: BookSkin, bookTitle: String, ownerNameRaw: String) =
    Pair(
        bookTitle.trim().ifBlank { "岁时录" }.take(4).map { it.toString() },
        ownerNameRaw.trim().take(4).map { it.toString() },
    )

private fun DrawScope.finishSpineEdges(w: Float, h: Float) {
    drawLine(Color.Black.copy(alpha = 0.45f), Offset(0f, 0.5f), Offset(w, 0.5f), 1.0f)
    drawLine(Color.Black.copy(alpha = 0.45f), Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1.0f)
}

/* ── 1. 蘅芜旧卷 · 文人古籍脊（云纹勒带 + 朱砂印 + 烫金铭牌）────────── */

private fun DrawScope.drawHengWuSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val nc = drawContext.canvas.nativeCanvas
    drawSpineLeatherBody(skin, w, h)
    drawSpineRaisedRails(w, h, foil, skin.palette.coverShadow)

    // 上下云纹勒带
    listOf(h * 0.10f, h * 0.90f).forEach { cy -> drawSpineCapPlate(skin, w, h, cy, foil, accent) }

    // 两侧如意云头
    listOf(w * 0.20f, w * 0.80f).forEach { cx ->
        for (i in 0 until 3) {
            val cy = h * (0.28f + i * 0.20f)
            val cloud = Path().apply {
                moveTo(cx, cy)
                quadraticBezierTo(cx + w * 0.06f, cy - h * 0.025f, cx + w * 0.04f, cy)
                quadraticBezierTo(cx + w * 0.06f, cy + h * 0.025f, cx, cy)
            }
            drawPath(cloud, foil.copy(alpha = 0.50f), style = Stroke(0.55f))
        }
    }

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.50f, foil, 0.08f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, seal, skin)
    drawSpineRuneStrip(skin, w, h, centerY = if (ownerChars.isNotEmpty()) h * 0.76f else h * 0.68f)
    finishSpineEdges(w, h)
}

/* ── 2. 霁月长明 · 黑曜月相脊（银弧 + 紫雾 + 月轮铭牌）────────────────── */

private fun DrawScope.drawJiYueSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val nc = drawContext.canvas.nativeCanvas
    drawSpineLeatherBody(skin, w, h)

    // 紫雾纵向晕染
    drawRect(
        brush = Brush.verticalGradient(
            listOf(seal.copy(alpha = 0.10f), Color.Transparent, Color.Transparent, seal.copy(alpha = 0.08f)),
        ),
    )

    drawSpineRaisedRails(w, h, foil, skin.palette.coverShadow, railWidth = 0.09f)

    // 顶 / 底 月相盘
    listOf(h * 0.10f, h * 0.90f).forEach { cy ->
        drawSpineCapPlate(skin, w, h, cy, foil, accent)
        drawCircle(foil.copy(alpha = 0.35f), w * 0.16f, Offset(w / 2f, cy), style = Stroke(0.5f))
    }

    // 银弧月轨（左右对称）
    listOf(-1f, 1f).forEach { sgn ->
        val arc = Path().apply {
            moveTo(w / 2f + sgn * w * 0.08f, h * 0.18f)
            quadraticBezierTo(w / 2f + sgn * w * 0.28f, h * 0.46f, w / 2f + sgn * w * 0.08f, h * 0.74f)
        }
        drawPath(arc, foil.copy(alpha = 0.40f), style = Stroke(0.55f))
        for (i in 0 until 4) {
            val py = h * (0.24f + i * 0.14f)
            drawCircle(foil.copy(alpha = 0.55f), 1.1f, Offset(w / 2f + sgn * w * 0.14f, py))
        }
    }

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.48f, seal, 0.12f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, seal, skin)
    drawSpineRuneStrip(skin, w, h, centerY = if (ownerChars.isNotEmpty()) h * 0.78f else h * 0.70f)
    finishSpineEdges(w, h)
}

/* ── 3. 晴川早春 · 樱枝玫瑰脊（奶油皮革 + 樱徽 + 丝绸饰带）────────────── */

private fun DrawScope.drawQingChuanSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val palette = skin.palette
    val foil = palette.foil.base; val accent = palette.foil.accent
    val pink = Color(0xFFFFC7D0); val pinkDeep = Color(0xFFE89BAA)
    val nc = drawContext.canvas.nativeCanvas

    drawSpineLeatherBody(skin, w, h)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(pink.copy(alpha = 0.10f), Color.Transparent, Color.Transparent, pinkDeep.copy(alpha = 0.08f)),
        ),
    )

    listOf(w * 0.11f, w * 0.89f).forEach { bx ->
        drawLine(palette.coverShadow.copy(alpha = 0.45f), Offset(bx, h * 0.05f), Offset(bx, h * 0.95f), w * 0.07f)
        drawLine(foil.copy(alpha = 0.65f), Offset(bx, h * 0.06f), Offset(bx, h * 0.94f), w * 0.024f)
    }

    // 左缘攀缘樱枝
    val branch = Path().apply {
        moveTo(w * 0.13f, h * 0.90f)
        quadraticBezierTo(w * 0.09f, h * 0.62f, w * 0.15f, h * 0.38f)
        quadraticBezierTo(w * 0.17f, h * 0.16f, w * 0.11f, h * 0.07f)
    }
    drawPath(branch, foil.copy(alpha = 0.62f), style = Stroke(width = (w * 0.020f).coerceAtLeast(0.5f)))
    listOf(0.10f to 0.09f, 0.14f to 0.30f, 0.11f to 0.52f, 0.13f to 0.72f).forEachIndexed { i, (fx, fy) ->
        drawBookSakura(Offset(w * fx, h * fy), w * (0.07f + i * 0.008f), rotDeg = i * 47f,
            light = pink, base = pinkDeep, stroke = accent)
    }

    listOf(h * 0.10f, h * 0.90f).forEach { cy ->
        drawSpineCapPlate(skin, w, h, cy, foil, accent)
        drawBookSakura(Offset(w / 2f, cy), w * 0.09f, rotDeg = 0f, light = pink, base = pinkDeep, stroke = foil)
    }

    // 中段丝绸饰带
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, palette.ribbon.copy(alpha = 0.50f),
                palette.ribbon.copy(alpha = 0.80f), palette.ribbon.copy(alpha = 0.50f), Color.Transparent),
        ),
        topLeft = Offset(w * 0.08f, h * 0.495f - h * 0.011f),
        size = Size(w * 0.84f, h * 0.022f),
    )

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.52f, pink, 0.14f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, pinkDeep, skin)
    finishSpineEdges(w, h)
}

/* ── 4. 赤焰天书 · 熔炉烈焰脊（火纹勒带 + 烈金绳 + 焚文铭牌）──────────── */

private fun DrawScope.drawChiYanSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val ember = Color(0xFFFF6020)
    val nc = drawContext.canvas.nativeCanvas
    drawSpineLeatherBody(skin, w, h)

    // 底部余烬暖光
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color.Transparent, ember.copy(alpha = 0.12f), ember.copy(alpha = 0.18f)),
            startY = h * 0.55f, endY = h,
        ),
    )

    drawSpineRaisedRails(w, h, foil, skin.palette.coverShadow, railWidth = 0.12f)

    listOf(h * 0.10f, h * 0.90f).forEach { cy ->
        drawSpineCapPlate(skin, w, h, cy, foil, accent)
        // 火焰放射
        for (i in 0 until 6) {
            val ang = i * (Math.PI / 3).toFloat() - (Math.PI / 2).toFloat()
            drawLine(ember.copy(alpha = 0.45f), Offset(w / 2f, cy),
                Offset(w / 2f + cos(ang) * w * 0.14f, cy + sin(ang) * w * 0.14f), 0.5f)
        }
    }

    // 两侧熔金火纹
    listOf(w * 0.22f, w * 0.78f).forEach { cx ->
        val flame = Path().apply {
            moveTo(cx, h * 0.78f)
            quadraticBezierTo(cx + w * 0.04f, h * 0.58f, cx, h * 0.42f)
            quadraticBezierTo(cx - w * 0.04f, h * 0.58f, cx, h * 0.78f)
        }
        drawPath(flame, ember.copy(alpha = 0.35f), style = Stroke(0.6f))
    }

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.50f, ember, 0.14f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, seal, skin)
    drawSpineRuneStrip(skin, w, h, centerY = if (ownerChars.isNotEmpty()) h * 0.76f else h * 0.68f)
    finishSpineEdges(w, h)
}

/* ── 5. 青鸾翠竹 · 翠玉竹节脊（竹纹槽 + 银箔 + 鸾羽铭牌）──────────────── */

private fun DrawScope.drawQingLuanSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val jade = Color(0xFF7AAB8F)
    val nc = drawContext.canvas.nativeCanvas
    drawSpineLeatherBody(skin, w, h)

    drawSpineRaisedRails(w, h, foil, skin.palette.coverShadow, railWidth = 0.10f)

    // 竹节槽（纵向分段）
    for (i in 0 until 4) {
        val sy = h * (0.20f + i * 0.18f)
        drawLine(jade.copy(alpha = 0.35f), Offset(w * 0.16f, sy), Offset(w * 0.84f, sy), 0.7f)
        drawLine(skin.palette.coverShadow.copy(alpha = 0.30f), Offset(w * 0.16f, sy + 1.5f),
            Offset(w * 0.84f, sy + 1.5f), 0.4f)
    }

    listOf(h * 0.10f, h * 0.90f).forEach { cy -> drawSpineCapPlate(skin, w, h, cy, foil, accent) }

    // 鸾羽纹（左右）
    listOf(-1f, 1f).forEach { sgn ->
        val feather = Path().apply {
            moveTo(w / 2f + sgn * w * 0.10f, h * 0.30f)
            quadraticBezierTo(w / 2f + sgn * w * 0.26f, h * 0.46f, w / 2f + sgn * w * 0.10f, h * 0.62f)
        }
        drawPath(feather, accent.copy(alpha = 0.45f), style = Stroke(0.55f))
    }

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.48f, jade, 0.10f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, seal, skin)
    drawSpineRuneStrip(skin, w, h, centerY = if (ownerChars.isNotEmpty()) h * 0.78f else h * 0.70f)
    finishSpineEdges(w, h)
}

/* ── 6. 星河长卷 · 星海航海脊（星座连线 + 罗盘徽 + 银箔铭牌）──────────── */

private fun DrawScope.drawXingHeSpine(skin: BookSkin, bookTitle: String, ownerNameRaw: String) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val seal = skin.palette.seal
    val nebula = Color(0xFF6FB3FF)
    val nc = drawContext.canvas.nativeCanvas
    drawSpineLeatherBody(skin, w, h)

    // 星云纵向晕染
    drawRect(
        brush = Brush.verticalGradient(
            listOf(seal.copy(alpha = 0.08f), Color.Transparent, nebula.copy(alpha = 0.06f), Color.Transparent),
        ),
    )

    drawSpineRaisedRails(w, h, foil, skin.palette.coverShadow, railWidth = 0.09f)

    // 顶 / 底 罗盘徽记
    listOf(h * 0.10f, h * 0.90f).forEach { cy ->
        drawSpineCapPlate(skin, w, h, cy, foil, accent)
        drawMetallicRing(Offset(w / 2f, cy), w * 0.14f, w * 0.004f, foil, accent, alpha = 0.60f)
    }

    // 左侧星座连线（猎户座式）
    val stars = listOf(
        Offset(w * 0.18f, h * 0.22f), Offset(w * 0.22f, h * 0.30f),
        Offset(w * 0.16f, h * 0.38f), Offset(w * 0.24f, h * 0.46f),
        Offset(w * 0.18f, h * 0.56f), Offset(w * 0.20f, h * 0.68f),
    )
    for (i in 0 until stars.size - 1) {
        drawLine(foil.copy(alpha = 0.35f), stars[i], stars[i + 1], 0.45f)
    }
    stars.forEach { pt ->
        drawCircle(foil.copy(alpha = 0.85f), 1.4f, pt)
        drawCircle(nebula.copy(alpha = 0.30f), 3.5f, pt)
    }
    // 右侧散星
    listOf(0.78f to 0.28f, 0.82f to 0.44f, 0.76f to 0.58f, 0.80f to 0.72f).forEach { (fx, fy) ->
        drawCircle(foil.copy(alpha = 0.65f), 1.0f, Offset(w * fx, h * fy))
    }

    val (titleChars, ownerChars) = spineTitleOwner(skin, bookTitle, ownerNameRaw)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.52f, nebula, 0.16f)
    softGlow(Offset(w / 2f, h * 0.46f), w * 0.35f, foil, 0.08f)
    drawSpineNameplateBlock(nc, w, h, titleChars, ownerChars, foil, accent, seal, skin)
    drawSpineRuneStrip(skin, w, h, centerY = if (ownerChars.isNotEmpty()) h * 0.78f else h * 0.70f)
    finishSpineEdges(w, h)
}

/* ── 雕花铭牌 · 竖排署名 / 书名（全皮肤统一工艺，主题角饰）────────────── */

private fun vertBlockHeight(charCount: Int, textSize: Float, gapFactor: Float): Float {
    if (charCount <= 0) return 0f
    val measure = android.graphics.Paint().apply {
        this.textSize = textSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    }
    val fm = measure.fontMetrics
    val charH = fm.descent - fm.ascent
    if (charCount == 1) return charH
    return (charCount - 1) * textSize * gapFactor + charH
}

/** 竖排文字布局：FontMetrics 精确居中 + 自动缩放以适应铭牌内区。 */
private data class VerticalSpineTextLayout(
    val textSize: Float,
    val lineGap: Float,
    val firstBaseline: Float,
    val blockHeight: Float,
)

private fun computeVerticalSpineTextLayout(
    charCount: Int,
    centerY: Float,
    preferredTextSize: Float,
    maxBlockHeight: Float,
    lineGapFactor: Float = 0.98f,
): VerticalSpineTextLayout {
    if (charCount <= 0) {
        return VerticalSpineTextLayout(preferredTextSize, 0f, centerY, 0f)
    }
    val tf = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    val measure = android.graphics.Paint().apply {
        isAntiAlias = true
        typeface = tf
    }

    fun blockHeightFor(size: Float): Float {
        measure.textSize = size
        val fm = measure.fontMetrics
        val charH = fm.descent - fm.ascent
        val gap = size * lineGapFactor
        return if (charCount <= 1) charH else (charCount - 1) * gap + charH
    }

    var textSize = preferredTextSize
    val minSize = (preferredTextSize * 0.50f).coerceAtLeast(4f)
    while (blockHeightFor(textSize) > maxBlockHeight && textSize > minSize) {
        textSize *= 0.92f
    }

    measure.textSize = textSize
    val fm = measure.fontMetrics
    val lineGap = textSize * lineGapFactor
    val blockH = blockHeightFor(textSize)
    val firstBaseline = centerY - blockH / 2f - fm.ascent
    return VerticalSpineTextLayout(textSize, lineGap, firstBaseline, blockH)
}

private fun DrawScope.drawSpineNameplateBlock(
    nc: android.graphics.Canvas,
    w: Float, h: Float,
    titleChars: List<String>,
    ownerChars: List<String>,
    foil: Color, accent: Color, glowColor: Color,
    skin: BookSkin,
) {
    val hasOwner = ownerChars.isNotEmpty()
    val spineSize = kotlin.math.min(w * 0.54f, h * 0.068f).coerceAtLeast(4f)

    if (hasOwner) {
        val preferredSize = spineSize * 0.88f
        val plateCenterY = h * 0.47f
        val plateLeft = w * 0.16f
        val plateW = w * 0.68f
        val maxTextH = (h * 0.19f).coerceAtMost(preferredSize * 3.2f)
        val layout = computeVerticalSpineTextLayout(
            ownerChars.size, plateCenterY, preferredSize, maxTextH, lineGapFactor = 0.96f,
        )
        // 描边/辉光留白（按实际字号计算）
        val innerPadV = layout.textSize * 0.50f
        val plateH = layout.blockHeight + innerPadV * 2f
        val plateTop = plateCenterY - plateH / 2f

        drawSpinePlateBevel(
            plateLeft, plateTop, plateW, plateH,
            skin.palette.spine.base.darken(0.08f), foil, skin.palette.coverShadow,
            cornerR = w * 0.05f,
        )
        drawSpinePlateCornerOrnaments(skin, plateLeft, plateTop, plateW, plateH, foil, accent)
        drawMetallicFrame(
            Offset(plateLeft + 2f, plateTop + 2f),
            Size(plateW - 4f, plateH - 4f),
            (w * 0.018f).coerceAtLeast(0.5f), foil, accent,
            cornerRadius = w * 0.045f, alpha = 0.75f,
        )

        drawVerticalSpineCharsEngraved(
            nc, ownerChars, w / 2f, layout,
            foil.lighten(0.55f), accent, glowColor,
        )

        val zuoSize = layout.textSize * 0.48f
        val zuoGap = layout.textSize * 0.34f
        val zuoBaselineY = plateTop - zuoGap
        val zuoPaint = android.graphics.Paint().apply {
            color = foil.lighten(0.25f).copy(alpha = 0.85f).toArgb()
            textSize = zuoSize
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
        }
        nc.drawText("著", w / 2f, zuoBaselineY, zuoPaint)
        drawSpineDivider(w / 2f, plateTop - zuoGap * 0.40f, w * 0.12f, foil)
        return
    }

    // 无署名：竖排书名 + 年份
    val titleLayout = computeVerticalSpineTextLayout(
        titleChars.size, h * 0.40f, spineSize, h * 0.18f, lineGapFactor = 0.98f,
    )
    val titlePadV = titleLayout.textSize * 0.38f
    val plateTop = h * 0.40f - titleLayout.blockHeight / 2f - titlePadV
    val plateBot = h * 0.40f + titleLayout.blockHeight / 2f + titlePadV
    val plateLeft = w * 0.16f
    val plateW = w * 0.68f

    drawSpinePlateBevel(
        plateLeft, plateTop, plateW, plateBot - plateTop,
        skin.palette.spine.base.darken(0.08f), foil, skin.palette.coverShadow,
        cornerR = w * 0.05f,
    )
    drawSpinePlateCornerOrnaments(skin, plateLeft, plateTop, plateW, plateBot - plateTop, foil, accent)
    drawVerticalSpineCharsEngraved(nc, titleChars, w / 2f, titleLayout, foil.lighten(0.50f), accent, glowColor)

    val yearChars = listOf("二", "○", "二", "六")
    val yearLayout = computeVerticalSpineTextLayout(
        yearChars.size, h * 0.62f, spineSize * 0.38f, h * 0.10f, lineGapFactor = 0.98f,
    )
    drawVerticalSpineCharsEngraved(
        nc, yearChars, w / 2f, yearLayout, foil.copy(alpha = 0.85f), accent, glowColor,
    )
}

private fun DrawScope.drawSpineDivider(cx: Float, cy: Float, halfW: Float, foil: Color) {
    drawLine(foil.copy(alpha = 0.55f), Offset(cx - halfW, cy), Offset(cx + halfW, cy), strokeWidth = 0.8f)
    val r = halfW * 0.18f
    val p = Path().apply {
        moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
    }
    drawPath(p, foil.copy(alpha = 0.65f), style = Stroke(width = 0.65f))
}

/** 竖排烫金刻字：描边 + 渐变填色 + 主题辉光（layout 精确居中）。 */
private fun drawVerticalSpineCharsEngraved(
    nc: android.graphics.Canvas,
    chars: List<String>,
    cx: Float,
    layout: VerticalSpineTextLayout,
    base: Color, accent: Color, glow: Color,
) {
    if (chars.isEmpty()) return
    drawVerticalSpineCharsEngraved(
        nc, chars, cx, layout.firstBaseline, layout.textSize, layout.lineGap,
        base, accent, glow,
    )
}

private fun drawVerticalSpineCharsEngraved(
    nc: android.graphics.Canvas,
    chars: List<String>,
    cx: Float, centerY: Float, textSize: Float,
    base: Color, accent: Color, glow: Color,
    lineGapFactor: Float = 1.06f,
) {
    val maxH = textSize * chars.size * 1.8f
    val layout = computeVerticalSpineTextLayout(chars.size, centerY, textSize, maxH, lineGapFactor)
    drawVerticalSpineCharsEngraved(
        nc, chars, cx, layout.firstBaseline, layout.textSize, layout.lineGap,
        base, accent, glow,
    )
}

private fun drawVerticalSpineCharsEngraved(
    nc: android.graphics.Canvas,
    chars: List<String>,
    cx: Float, firstBaseline: Float, textSize: Float, lineGap: Float,
    base: Color, accent: Color, glow: Color,
) {
    if (chars.isEmpty()) return
    val tf = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
    val strokeW = (textSize * 0.09f).coerceAtLeast(0.9f)
    val shadowR = (textSize * 0.08f).coerceAtLeast(0.8f)

    chars.forEachIndexed { idx, ch ->
        val cy = firstBaseline + idx * lineGap
        val outline = android.graphics.Paint().apply {
            color = Color.Black.copy(alpha = 0.55f).toArgb()
            this.textSize = textSize
            isAntiAlias = true
            typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeW
        }
        nc.drawText(ch, cx, cy, outline)
        val warmEdge = android.graphics.Paint().apply {
            color = accent.darken(0.20f).copy(alpha = 0.50f).toArgb()
            this.textSize = textSize
            isAntiAlias = true
            typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
            style = android.graphics.Paint.Style.STROKE
            this.strokeWidth = strokeW * 0.45f
        }
        nc.drawText(ch, cx, cy, warmEdge)
        val main = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                cx, cy - textSize * 0.75f, cx, cy + textSize * 0.12f,
                intArrayOf(base.lighten(0.35f).toArgb(), base.toArgb(), accent.toArgb()),
                floatArrayOf(0f, 0.50f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
            this.textSize = textSize
            isAntiAlias = true
            typeface = tf
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(shadowR, 0f, textSize * 0.015f, glow.copy(alpha = 0.28f).toArgb())
        }
        nc.drawText(ch, cx, cy, main)
    }
}

private fun DrawScope.drawSpineRuneStrip(skin: BookSkin, w: Float, h: Float, centerY: Float) {
    val foil = skin.palette.foil.base
    val accent = skin.palette.cover.accent
    when (skin.id.raw) {
        "builtin::xinghe" -> {
            for (i in 0 until 5) {
                val py = centerY + (i - 2) * h * 0.022f
                drawLine(foil.copy(alpha = 0.45f), Offset(w * 0.35f, py), Offset(w * 0.65f, py), 0.5f)
                drawCircle(foil.copy(alpha = 0.7f), 1.2f, Offset(w / 2f, py))
            }
        }
        "builtin::chiyan" -> {
            for (i in 0 until 4) {
                val px = w * (0.38f + i * 0.08f)
                drawCircle(Color(0xFFFF7040).copy(alpha = 0.55f), 1.5f, Offset(px, centerY))
            }
        }
        "builtin::jiyue" -> {
            drawCircle(foil.copy(alpha = 0.5f), w * 0.12f, Offset(w / 2f, centerY), style = Stroke(0.6f))
            drawCircle(foil.copy(alpha = 0.35f), w * 0.08f, Offset(w / 2f, centerY), style = Stroke(0.4f))
        }
        "builtin::qingchuan" -> {
            for (i in 0 until 3) {
                val ang = i * 2.1f
                drawCircle(accent.copy(alpha = 0.5f), 1.3f,
                    Offset(w / 2f + cos(ang) * w * 0.08f, centerY + sin(ang) * h * 0.012f))
            }
        }
        "builtin::qingluan" -> {
            val leaf = Path().apply {
                moveTo(w / 2f, centerY + h * 0.015f)
                quadraticBezierTo(w * 0.62f, centerY, w * 0.58f, centerY - h * 0.018f)
            }
            drawPath(leaf, accent.copy(alpha = 0.55f), style = Stroke(0.7f))
        }
        else -> {
            drawMetallicRing(Offset(w / 2f, centerY), w * 0.14f, 0.6f, foil, skin.palette.foil.accent, alpha = 0.5f)
        }
    }
}

/* ── 页摞（右侧面）────────────────────────────────────────────────────── */

fun DrawScope.drawPageEdgeVerticalEnhanced(skin: BookSkin) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val palette = skin.palette
    val foil = palette.foil.base

    // 1. 纸叠体：内深外浅 + 纵向体积
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(
                palette.pageEdgeDark,
                palette.pageEdge.copy(alpha = 0.92f),
                palette.pageEdge,
                palette.pageEdge.copy(alpha = 0.95f),
                palette.pageEdgeDark.copy(alpha = 0.85f),
            ),
        ),
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                palette.coverShadow.copy(alpha = 0.35f),
                Color.Transparent,
                Color.Transparent,
                palette.coverShadow.copy(alpha = 0.25f),
            ),
        ),
    )

    // 2. 烫金切口（弧形感：中间厚、两侧薄）
    val gildH = h * 0.09f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(foil.lighten(0.3f), foil, foil.darken(0.2f)),
        ),
        topLeft = Offset(0f, 0f), size = Size(w, gildH),
    )
    drawRect(
        brush = Brush.verticalGradient(
            listOf(foil.darken(0.2f), foil, foil.lighten(0.3f)),
        ),
        topLeft = Offset(0f, h - gildH), size = Size(w, gildH),
    )
    drawLine(palette.pageEdgeDark.copy(alpha = 0.75f), Offset(0f, gildH), Offset(w, gildH), 0.9f)
    drawLine(palette.pageEdgeDark.copy(alpha = 0.75f), Offset(0f, h - gildH), Offset(w, h - gildH), 0.9f)

    // 3. 密织页线
    val pagesTop = gildH
    val pagesBot = h - gildH
    val pagesH = pagesBot - pagesTop
    val lines = (pagesH / 1.2f).toInt().coerceAtMost(skin.geometry.pageStackCountHigh)
    val gap = pagesH / lines.coerceAtLeast(1)
    for (i in 0 until lines) {
        val a = 0.12f + ((i * 5) % 9 / 9f) * 0.35f
        drawLine(
            palette.pageEdgeDark.copy(alpha = a),
            Offset(0f, pagesTop + i * gap), Offset(w * 0.85f, pagesTop + i * gap),
            strokeWidth = if (i % 17 == 0) 1.2f else 0.65f,
        )
    }

    // 4. 外缘烫金书口线
    drawLine(foil.copy(alpha = 0.55f), Offset(w - 0.8f, gildH), Offset(w - 0.8f, h - gildH), 1.4f)
    drawLine(foil.copy(alpha = 0.25f), Offset(w - 2f, gildH), Offset(w - 2f, h - gildH), 0.6f)

    // 5. 丝绸书签（皮肤 seal 色）
    val ribbonY = h * 0.48f
    val ribbonH = h * 0.032f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(
                palette.seal.copy(alpha = 0.4f),
                palette.seal.copy(alpha = 0.95f),
                palette.seal.darken(0.12f).copy(alpha = 0.85f),
                palette.seal.copy(alpha = 0.4f),
            ),
        ),
        topLeft = Offset(0f, ribbonY - ribbonH / 2f),
        size = Size(w, ribbonH),
    )
    drawLine(foil.copy(alpha = 0.4f), Offset(0f, ribbonY), Offset(w, ribbonY), 0.5f)

    // 6. 章节索引签（两枚、圆角感渐变，不再大块纯色）
    listOf(0.22f, 0.68f).forEach { p ->
        val cy = h * p
        val tabH = h * 0.042f
        val tabW = w * 0.55f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    foil.copy(alpha = 0.75f),
                    foil.lighten(0.2f).copy(alpha = 0.9f),
                    foil.copy(alpha = 0.5f),
                ),
            ),
            topLeft = Offset(w - tabW, cy - tabH / 2f),
            size = Size(tabW, tabH),
        )
        drawLine(Color.Black.copy(alpha = 0.4f),
            Offset(w - tabW, cy - tabH / 2f), Offset(w, cy - tabH / 2f), 0.6f)
    }

    // 7. 页摞侧面不显示署名（仅书脊 / 封面刻印）
    drawPageEdgeSkinAccent(skin, w, h, gildH)

    // 9. 装订珠（foil 色，非纯白）
    val beadR = (w * 0.05f).coerceAtLeast(1f)
    listOf(0.25f, 0.50f, 0.75f).forEach { px ->
        listOf(gildH * 0.5f, h - gildH * 0.5f).forEach { by ->
            drawCircle(foil.copy(alpha = 0.85f), beadR, Offset(w * px, by))
            drawCircle(foil.lighten(0.35f).copy(alpha = 0.45f), beadR * 0.45f,
                Offset(w * px - beadR * 0.15f, by - beadR * 0.15f))
        }
    }
}

private fun DrawScope.drawPageEdgeSkinAccent(skin: BookSkin, w: Float, h: Float, gildH: Float) {
    val foil = skin.palette.foil.base
    when (skin.id.raw) {
        "builtin::xinghe" -> {
            for (i in 0 until 8) {
                val py = gildH + (h - 2 * gildH) * (i / 7f)
                drawCircle(foil.copy(alpha = 0.35f), 0.9f, Offset(w * 0.15f, py))
                if (i % 2 == 0) {
                    drawLine(foil.copy(alpha = 0.2f),
                        Offset(w * 0.12f, py), Offset(w * 0.22f, py - w * 0.3f), 0.4f)
                }
            }
        }
        "builtin::chiyan" -> {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xFFFF6020).copy(alpha = 0.12f)),
                    startY = h * 0.75f, endY = h - gildH,
                ),
                topLeft = Offset(0f, h * 0.75f),
                size = Size(w, h * 0.25f - gildH),
            )
        }
        "builtin::jiyue" -> {
            drawLine(Color(0xFF9C78E0).copy(alpha = 0.25f),
                Offset(w * 0.2f, gildH), Offset(w * 0.35f, h * 0.15f), 0.5f)
        }
        else -> { /* 其余皮肤保持克制 */ }
    }
}

/* ── 顶 / 底切口 ──────────────────────────────────────────────────────── */

fun DrawScope.drawPageEdgeHorizontalEnhanced(skin: BookSkin) {
    val w = size.width
    val h = size.height
    if (w < 1f || h < 1f) return
    val palette = skin.palette
    val foil = palette.foil.base

    drawRect(
        brush = Brush.verticalGradient(
            listOf(palette.pageEdge, palette.pageEdge.copy(alpha = 0.88f), palette.pageEdgeDark),
        ),
    )
    // 密线
    val lines = (w / 1.3f).toInt().coerceAtLeast(1)
    for (i in 0 until lines) {
        val a = 0.1f + ((i % 11) / 11f) * 0.35f
        drawLine(
            palette.pageEdgeDark.copy(alpha = a),
            Offset(i * 1.3f, 0f), Offset(i * 1.3f, h),
            strokeWidth = if (i % 10 == 0) 1.0f else 0.55f,
        )
    }
    // 外缘金线
    drawRect(
        brush = Brush.verticalGradient(
            listOf(foil.copy(alpha = 0.5f), foil.copy(alpha = 0.15f), Color.Transparent),
        ),
        topLeft = Offset(0f, 0f), size = Size(w, (h * 0.2f).coerceAtLeast(1f)),
    )
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(palette.coverShadow.copy(alpha = 0.45f), Color.Transparent),
            startX = 0f, endX = w * 0.12f,
        ),
        topLeft = Offset(0f, 0f), size = Size(w * 0.12f, h),
    )

    drawSpineEmblem(skin, Offset(w * 0.12f, h / 2f), h * 0.65f)
    drawLine(palette.pageEdgeDark.copy(alpha = 0.65f), Offset(0.5f, 0f), Offset(0.5f, h), 1.0f)
    drawLine(palette.pageEdgeDark.copy(alpha = 0.65f), Offset(w - 0.5f, 0f), Offset(w - 0.5f, h), 1.0f)
}

/* ── 书脊徽记（各皮肤）──────────────────────────────────────────────── */

internal fun DrawScope.drawSpineEmblem(skin: BookSkin, center: Offset, size: Float) {
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val s = size.coerceAtLeast(2f)
    val sw = (s * 0.10f).coerceAtLeast(0.6f)
    val cx = center.x
    val cy = center.y
    when (skin.id.raw) {
        "builtin::jiyue" -> {
            drawCircle(foil.copy(alpha = 0.85f), s * 0.5f, center, style = Stroke(width = sw))
            drawCircle(foil.copy(alpha = 0.9f), s * 0.16f, center)
        }
        "builtin::qingchuan" -> {
            for (i in 0 until 5) {
                val ang = i * (Math.PI * 2 / 5).toFloat() - (Math.PI / 2).toFloat()
                val px = cx + cos(ang) * s * 0.34f
                val py = cy + sin(ang) * s * 0.34f
                drawCircle(foil.copy(alpha = 0.8f), s * 0.16f, Offset(px, py), style = Stroke(width = sw * 0.8f))
            }
            drawCircle(skin.palette.seal.copy(alpha = 0.9f), s * 0.10f, center)
        }
        "builtin::chiyan" -> {
            for (i in 0 until 8) {
                val ang = i * (Math.PI * 2 / 8).toFloat()
                drawLine(foil.copy(alpha = 0.8f),
                    Offset(cx + cos(ang) * s * 0.18f, cy + sin(ang) * s * 0.18f),
                    Offset(cx + cos(ang) * s * 0.5f, cy + sin(ang) * s * 0.5f),
                    strokeWidth = sw)
            }
            drawCircle(accent.copy(alpha = 0.9f), s * 0.14f, center)
        }
        "builtin::qingluan" -> {
            for (i in -1..1) {
                val leaf = Path().apply {
                    moveTo(cx, cy + s * 0.4f)
                    quadraticBezierTo(cx + i * s * 0.4f, cy, cx + i * s * 0.25f, cy - s * 0.45f)
                }
                drawPath(leaf, foil.copy(alpha = 0.8f), style = Stroke(width = sw))
            }
        }
        "builtin::xinghe" -> {
            for (i in 0 until 4) {
                val ang = i * (Math.PI / 2).toFloat()
                drawLine(foil.copy(alpha = 0.85f), center,
                    Offset(cx + cos(ang) * s * 0.5f, cy + sin(ang) * s * 0.5f), strokeWidth = sw)
                val ang2 = ang + (Math.PI / 4).toFloat()
                drawLine(foil.copy(alpha = 0.5f), center,
                    Offset(cx + cos(ang2) * s * 0.3f, cy + sin(ang2) * s * 0.3f), strokeWidth = sw * 0.7f)
            }
            drawCircle(foil, s * 0.10f, center)
        }
        else -> {
            val p = Path().apply {
                moveTo(cx, cy - s * 0.5f); lineTo(cx + s * 0.5f, cy)
                lineTo(cx, cy + s * 0.5f); lineTo(cx - s * 0.5f, cy); close()
            }
            drawPath(p, foil.copy(alpha = 0.78f), style = Stroke(width = sw))
            drawCircle(foil.copy(alpha = 0.9f), s * 0.12f, center)
        }
    }
}
