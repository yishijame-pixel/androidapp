// ═══════════════════════════════════════════════════════════════════════════
// BookExtendedSkinsArt.kt — 六款扩展魔法书皮肤 · 封面 / 后封专属绘制
//
//   · 玄冰古卷 → 六角冰晶法阵 + 霜蓝银箔
//   · 紫霄雷典 → 雷云紫电 + 电弧环阵
//   · 鎏金沙经 → 金字塔日轮 + 流沙金纹
//   · 墨龙天书 → 中国传统神龙徽 + 墨金 lacquer
//   · 珊瑚秘海 → 珊瑚枝 + 气泡灵珠
//   · 晶棱幻书 → 三棱折射 + 虹彩切面
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

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
import com.example.funlife.domain.skin.BookSkin
import kotlin.math.cos
import kotlin.math.sin

private const val EPI2 = (Math.PI * 2).toFloat()

private fun DrawScope.drawExtTitleBlock(
    skin: BookSkin, title: String, author: String, titleY: Float, titleScale: Float = 0.13f,
) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    drawEmbossText(
        text = title, cx = w / 2f, cy = titleY, textSize = w * titleScale,
        base = foil, accent = skin.palette.foil.accent,
    )
    if (author.isBlank()) return
    val subSize = w * 0.048f
    val subY = if (author.contains("  ")) h * 0.685f else titleY + w * titleScale * 1.05f
    if (author.contains("  ")) {
        val nc = drawContext.canvas.nativeCanvas
        val p = android.graphics.Paint().apply {
            color = foil.copy(alpha = 0.82f).toArgb()
            textSize = subSize * 0.92f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        nc.drawText(author, w / 2f, subY, p)
    } else {
        drawCoverOwnerName(skin, author, w / 2f, subY, subSize)
    }
}

private fun DrawScope.drawExtBottomFlourish(skin: BookSkin) {
    val w = size.width; val h = size.height; val foil = skin.palette.foil.base
    drawLine(foil.copy(alpha = 0.55f), Offset(w * 0.32f, h * 0.84f), Offset(w * 0.68f, h * 0.84f), w * 0.0035f)
    val cx = w / 2f; val cy = h * 0.92f; val r = w * 0.016f
    val p = Path().apply { moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close() }
    drawPath(p, foil.copy(alpha = 0.65f), style = Stroke(w * 0.003f))
}

private fun DrawScope.drawHexCrystal(center: Offset, r: Float, foil: Color, accent: Color, fill: Color) {
    val pts = (0 until 6).map { i ->
        val a = i * (EPI2 / 6) - EPI2 / 4
        Offset(center.x + cos(a) * r, center.y + sin(a) * r)
    }
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, fill.copy(alpha = 0.35f))
    drawPath(path, foil, style = Stroke(r * 0.08f))
    for (i in 0 until 6) {
        drawLine(accent.copy(alpha = 0.5f), center, pts[i], r * 0.04f)
    }
    drawGemstone(center, r * 0.22f, fill, foil, r * 0.05f)
}

// ── 1. 玄冰古卷 ───────────────────────────────────────────────────────────
fun DrawScope.drawXuanBingFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val ice = Color(0xFF9ED4FF); val cx = w / 2f; val cy = h * 0.34f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)
    drawCornerBrackets(foil, accent, pad + w * 0.006f, w * 0.06f, w * 0.004f)
    softGlow(Offset(cx, cy), w * 0.28f, ice, 0.35f)
    for (i in 0 until 3) {
        val rr = w * (0.18f - i * 0.045f)
        drawMetallicRing(Offset(cx, cy), rr, w * 0.005f, foil, accent, alpha = 0.85f - i * 0.15f)
    }
    for (i in 0 until 6) {
        val ang = i * (EPI2 / 6)
        drawHexCrystal(
            Offset(cx + cos(ang) * w * 0.12f, cy + sin(ang) * w * 0.12f),
            w * 0.045f, foil, accent, ice,
        )
    }
    drawHexCrystal(Offset(cx, cy), w * 0.11f, foil, accent, ice)
    drawSealStamp(Offset(cx, cy), w * 0.09f, skin.palette.seal, "冰", skin.palette.cover.base)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawXuanBingBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val ice = Color(0xFF9ED4FF); val cx = w / 2f; val cy = h * 0.46f
    val pad = w * 0.055f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.005f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.18f, ice, 0.22f)
    drawHexCrystal(Offset(cx, cy), w * 0.12f, foil, accent, ice)
    drawSealStamp(Offset(cx, cy), w * 0.12f, skin.palette.seal, "霜", skin.palette.cover.base)
}

// ── 2. 紫霄雷典 ───────────────────────────────────────────────────────────
fun DrawScope.drawZiXiaoFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val bolt = Color(0xFF6FE8FF); val cx = w / 2f; val cy = h * 0.34f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.007f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.30f, bolt, 0.28f)
    drawMetallicRing(Offset(cx, cy), w * 0.17f, w * 0.007f, foil, accent)
    // 闪电折线
    val boltPath = Path().apply {
        moveTo(cx - w * 0.04f, cy - w * 0.16f)
        lineTo(cx + w * 0.02f, cy - w * 0.02f)
        lineTo(cx - w * 0.01f, cy - w * 0.02f)
        lineTo(cx + w * 0.05f, cy + w * 0.18f)
    }
    drawPath(boltPath, bolt.copy(alpha = 0.95f), style = Stroke(w * 0.012f))
    drawPath(boltPath, Color.White.copy(alpha = 0.45f), style = Stroke(w * 0.005f))
    for (i in 0 until 8) {
        val ang = i * (EPI2 / 8)
        drawLine(accent.copy(alpha = 0.35f),
            Offset(cx + cos(ang) * w * 0.10f, cy + sin(ang) * w * 0.10f),
            Offset(cx + cos(ang) * w * 0.17f, cy + sin(ang) * w * 0.17f), w * 0.003f)
    }
    drawSealStamp(Offset(cx, cy + w * 0.20f), w * 0.08f, skin.palette.seal, "雷", skin.palette.cover.base)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawZiXiaoBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.45f
    drawMetallicFrame(Offset(w * 0.055f, w * 0.055f), Size(w * 0.89f, h * 0.89f), w * 0.005f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.20f, accent, 0.25f)
    drawMetallicRing(Offset(cx, cy), w * 0.14f, w * 0.006f, foil, accent)
    drawSealStamp(Offset(cx, cy), w * 0.11f, skin.palette.seal, "霄", skin.palette.cover.base)
}

// ── 3. 鎏金沙经 ───────────────────────────────────────────────────────────
fun DrawScope.drawLiuJinFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val sand = Color(0xFFE8C878); val cx = w / 2f; val cy = h * 0.36f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.007f, foil, accent)
    softGlow(Offset(cx, cy - w * 0.06f), w * 0.26f, sand, 0.30f)
    // 金字塔
    val pyr = Path().apply {
        moveTo(cx, cy - w * 0.14f)
        lineTo(cx - w * 0.16f, cy + w * 0.10f)
        lineTo(cx + w * 0.16f, cy + w * 0.10f); close()
    }
    drawPath(pyr, sand.copy(alpha = 0.25f))
    drawPath(pyr, foil, style = Stroke(w * 0.006f))
    drawCircle(brush = Brush.radialGradient(listOf(foil.lighten(0.5f), accent), center = Offset(cx, cy - w * 0.18f), radius = w * 0.08f),
        radius = w * 0.07f, center = Offset(cx, cy - w * 0.18f))
    // 流沙弧
    for (i in 0 until 5) {
        val y = cy + w * 0.12f + i * w * 0.025f
        drawLine(sand.copy(alpha = 0.35f - i * 0.05f), Offset(w * 0.22f, y), Offset(w * 0.78f, y), w * 0.002f)
    }
    drawSealStamp(Offset(cx, cy + w * 0.02f), w * 0.09f, skin.palette.seal, "沙", skin.palette.cover.base)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawLiuJinBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.46f
    drawMetallicFrame(Offset(w * 0.055f, w * 0.055f), Size(w * 0.89f, h * 0.89f), w * 0.006f, foil, accent)
    drawCircle(foil.copy(alpha = 0.5f), w * 0.12f, Offset(cx, cy - w * 0.08f), style = Stroke(w * 0.005f))
    drawSealStamp(Offset(cx, cy), w * 0.12f, skin.palette.seal, "金", skin.palette.cover.base)
}

// ── 4. 墨龙天书 ───────────────────────────────────────────────────────────
fun DrawScope.drawMoLongFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.34f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.007f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.28f, foil, 0.28f)
    drawMetallicRing(Offset(cx, cy), w * 0.19f, w * 0.007f, foil, accent)
    val emblem = w * 0.34f
    drawChineseDragon(
        bounds = Rect(
            left = cx - emblem / 2f,
            top = cy - emblem * 0.42f,
            right = cx + emblem / 2f,
            bottom = cy + emblem * 0.42f,
        ),
        colors = ChineseDragonColors(
            ink = Color(0xFF0D0A08),
            body = skin.palette.cover.base.darken(0.15f),
            gold = foil,
            goldLight = foil.lighten(0.35f),
            accent = accent,
            eye = Color(0xFFFFD54F),
        ),
        phase = 0f,
        facingRight = true,
        detail = ChineseDragonDetail.Emblem,
        bodyAlpha = 1f,
    )
    drawGemstone(Offset(cx + w * 0.11f, cy + w * 0.10f), w * 0.028f, accent, foil, w * 0.003f)
    drawSealStamp(Offset(cx, cy), w * 0.09f, skin.palette.seal, "龙", skin.palette.cover.base)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawMoLongBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.45f
    drawMetallicFrame(Offset(w * 0.055f, w * 0.055f), Size(w * 0.89f, h * 0.89f), w * 0.006f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.18f, foil, 0.2f)
    val emblem = w * 0.42f
    drawChineseDragon(
        bounds = Rect(
            left = cx - emblem / 2f,
            top = cy - emblem * 0.35f,
            right = cx + emblem / 2f,
            bottom = cy + emblem * 0.35f,
        ),
        colors = ChineseDragonColors(
            ink = Color(0xFF0D0A08),
            body = skin.palette.cover.base.darken(0.12f),
            gold = foil,
            goldLight = foil.lighten(0.3f),
            accent = accent,
        ),
        phase = 0f,
        facingRight = false,
        detail = ChineseDragonDetail.Emblem,
        bodyAlpha = 0.92f,
    )
    drawSealStamp(Offset(cx, cy + w * 0.16f), w * 0.10f, skin.palette.seal, "墨", skin.palette.cover.base)
}

// ── 5. 珊瑚秘海 ───────────────────────────────────────────────────────────
fun DrawScope.drawShanHuFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val coral = Color(0xFFFF8A7A); val teal = Color(0xFF4ECDC4)
    val cx = w / 2f; val cy = h * 0.36f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.26f, teal, 0.25f)
    // 珊瑚枝
    val branch = Path().apply {
        moveTo(cx - w * 0.08f, cy + w * 0.12f)
        lineTo(cx - w * 0.04f, cy - w * 0.04f)
        lineTo(cx + w * 0.06f, cy + w * 0.08f)
        lineTo(cx + w * 0.10f, cy - w * 0.10f)
    }
    drawPath(branch, coral.copy(alpha = 0.85f), style = Stroke(w * 0.009f))
    listOf(Offset(cx - w * 0.04f, cy - w * 0.04f), Offset(cx + w * 0.10f, cy - w * 0.10f), Offset(cx + w * 0.02f, cy + w * 0.02f))
        .forEach { drawGemstone(it, w * 0.028f, coral, foil, w * 0.003f) }
    // 气泡
    for (i in 0 until 6) {
        val bx = cx + cos(i * 1.1f) * w * 0.14f
        val by = cy + sin(i * 0.9f) * w * 0.10f - w * 0.05f
        drawCircle(teal.copy(alpha = 0.25f), w * 0.022f, Offset(bx, by), style = Stroke(w * 0.002f))
        drawCircle(Color.White.copy(alpha = 0.35f), w * 0.006f, Offset(bx - w * 0.006f, by - w * 0.006f))
    }
    drawSealStamp(Offset(cx - w * 0.12f, cy + w * 0.14f), w * 0.08f, skin.palette.seal, "海", skin.palette.cover.base)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawShanHuBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val coral = Color(0xFFFF8A7A)
    val cx = w / 2f; val cy = h * 0.46f
    drawMetallicFrame(Offset(w * 0.055f, w * 0.055f), Size(w * 0.89f, h * 0.89f), w * 0.005f, foil, skin.palette.foil.accent)
    softGlow(Offset(cx, cy), w * 0.16f, coral, 0.18f)
    drawGemstone(Offset(cx, cy), w * 0.05f, coral, foil, w * 0.004f)
    drawSealStamp(Offset(cx, cy), w * 0.11f, skin.palette.seal, "珊", skin.palette.cover.base)
}

// ── 6. 晶棱幻书 ───────────────────────────────────────────────────────────
fun DrawScope.drawJingLengFront(skin: BookSkin, title: String, author: String = "一  人  一  册") {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.34f
    val pad = w * 0.05f
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.007f, foil, accent)
    softGlow(Offset(cx, cy), w * 0.28f, accent, 0.30f)
    // 三棱柱
    val prism = Path().apply {
        moveTo(cx, cy - w * 0.16f)
        lineTo(cx - w * 0.14f, cy + w * 0.10f)
        lineTo(cx + w * 0.14f, cy + w * 0.10f); close()
    }
    drawPath(prism, Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFF6B9DFF), Color(0xFF9DFF6B)), start = Offset(cx - w * 0.14f, cy - w * 0.16f), end = Offset(cx + w * 0.14f, cy + w * 0.10f)))
    drawPath(prism, foil.copy(alpha = 0.7f), style = Stroke(w * 0.005f))
    drawLine(Color.White.copy(alpha = 0.5f), Offset(cx, cy - w * 0.16f), Offset(cx, cy + w * 0.10f), w * 0.004f)
    for (i in 0 until 3) {
        rotate(i * 120f, Offset(cx, cy)) {
            drawLine(accent.copy(alpha = 0.4f), Offset(cx, cy), Offset(cx, cy - w * 0.18f), w * 0.003f)
        }
    }
    drawGemstone(Offset(cx, cy - w * 0.02f), w * 0.04f, Color(0xFFE8ECFF), foil, w * 0.004f)
    drawExtTitleBlock(skin, title, author, h * 0.62f)
    drawExtBottomFlourish(skin)
}

fun DrawScope.drawJingLengBack(skin: BookSkin) {
    val w = size.width; val h = size.height
    val foil = skin.palette.foil.base; val accent = skin.palette.foil.accent
    val cx = w / 2f; val cy = h * 0.45f
    drawMetallicFrame(Offset(w * 0.055f, w * 0.055f), Size(w * 0.89f, h * 0.89f), w * 0.005f, foil, accent)
    for (i in 0 until 6) {
        val ang = i * (EPI2 / 6)
        drawLine(
            Brush.linearGradient(listOf(Color(0xFFFF6B9D), Color(0xFF6B9DFF))),
            Offset(cx, cy), Offset(cx + cos(ang) * w * 0.14f, cy + sin(ang) * w * 0.14f), w * 0.004f,
        )
    }
    drawGemstone(Offset(cx, cy), w * 0.045f, accent, foil, w * 0.004f)
    drawSealStamp(Offset(cx, cy), w * 0.10f, skin.palette.seal, "棱", skin.palette.cover.base)
}
