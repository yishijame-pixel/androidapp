// ═══════════════════════════════════════════════════════════════════════════
// BookBackCovers.kt — 六本魔法书各自独立的「后封 / 版心」设计
//
// 后封定位为「书末钤印 / 藏书票」，与正面呼应但更收敛：
//   · 蘅芜旧卷 → 回纹方框 + 中央朱砂方印「啟」 + 藏书铭
//   · 霁月长明 → 满月银印「霁」 + 环绕月相 + 星点
//   · 晴川早春 → 留白单枝樱 + 玫瑰金小印「晴」（极简和风）
//   · 赤焰天书 → 炽金日芒小阵 + 火印「焚」
//   · 青鸾翠竹 → 青鸾小徽 + 双竹 + 银印「鸾」
//   · 星河长卷 → 星象小罗盘 + 星座 + 银印「渊」
//
// 复用 BookMaterials.kt 与 BookFrontCovers.kt 的金属/宝石/徽记工具，统一质感。
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

private const val BPI2 = (Math.PI * 2).toFloat()

/** 后封统一入口：先画底，再按皮肤分派专属版心。 */
fun DrawScope.drawBackCoverArt(skin: BookSkin) {
    drawCoverBase(skin)
    when (skin.id.raw) {
        "builtin::hengwu"    -> drawHengWuBack(skin)
        "builtin::jiyue"     -> drawJiYueBack(skin)
        "builtin::qingchuan" -> drawQingChuanBack(skin)
        "builtin::chiyan"    -> drawChiYanBack(skin)
        "builtin::qingluan"  -> drawQingLuanBack(skin)
        "builtin::xinghe"    -> drawXingHeBack(skin)
        "builtin::xuanbing"  -> drawXuanBingBack(skin)
        "builtin::zixiao"    -> drawZiXiaoBack(skin)
        "builtin::liujin"    -> drawLiuJinBack(skin)
        "builtin::molong"    -> drawMoLongBack(skin)
        "builtin::shanhu"    -> drawShanHuBack(skin)
        "builtin::jingleng"  -> drawJingLengBack(skin)
        else                 -> drawGenericBack(skin)
    }
    drawCornerWear(size.width, size.height, skin.palette.foil.base)
}

/** 后封竖排闭合铭文（金属小字，居中）。 */
private fun DrawScope.drawColophon(skin: BookSkin, text: String, cy: Float, sizeScale: Float = 0.034f) {
    val w = size.width
    val foil = skin.palette.foil.base
    val nc = drawContext.canvas.nativeCanvas
    val p = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.7f).toArgb()
        textSize = w * sizeScale
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL,
        )
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.28f
    }
    nc.drawText(text, w / 2f, cy, p)
}

/** 后封上下细装饰横线 + 中央菱点（与正面 bottomFlourish 呼应）。 */
private fun DrawScope.drawBackRule(skin: BookSkin, y: Float) {
    val w = size.width
    val foil = skin.palette.foil.base
    drawLine(foil.copy(alpha = 0.55f), Offset(w * 0.30f, y), Offset(w * 0.44f, y),
        strokeWidth = (w * 0.003f).coerceAtLeast(0.5f))
    drawLine(foil.copy(alpha = 0.55f), Offset(w * 0.56f, y), Offset(w * 0.70f, y),
        strokeWidth = (w * 0.003f).coerceAtLeast(0.5f))
    val r = w * 0.012f
    val p = Path().apply {
        moveTo(w / 2f, y - r); lineTo(w / 2f + r, y); lineTo(w / 2f, y + r); lineTo(w / 2f - r, y); close()
    }
    drawPath(p, foil.copy(alpha = 0.7f), style = Stroke(width = (w * 0.0025f).coerceAtLeast(0.5f)))
}

// ── 1. 蘅芜旧卷 · 回纹方框 + 朱砂方印「啟」(藏书票) ────────────────────────
fun DrawScope.drawHengWuBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.46f

    // 双金属边框
    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)
    val pad2 = pad + w * 0.02f
    drawMetallicFrame(Offset(pad2, pad2), Size(w - pad2 * 2, h - pad2 * 2),
        w * 0.0035f, foil, accent, alpha = 0.55f)

    // 上铭：藏书印记
    drawColophon(skin, "蘅  芜  旧  卷", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)

    // 中央团徽：柔光 + 双环 + 朱砂方印「啟」 + 四出云头(收敛)
    softGlow(Offset(cx, cy), w * 0.20f, foil, 0.20f)
    drawMetallicRing(Offset(cx, cy), w * 0.155f, w * 0.007f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.118f, w * 0.004f, foil, accent, alpha = 0.6f)
    for (i in 0 until 4) {
        rotate(45f + i * 90f, Offset(cx, cy)) {
            drawRuyiCloud(Offset(cx, cy - w * 0.135f), w * 0.06f, 0, foil, accent,
                (w * 0.004f).coerceAtLeast(0.6f))
        }
    }
    drawSealStamp(Offset(cx, cy), w * 0.135f, skin.palette.seal, "啟", skin.palette.cover.base)

    // 下铭
    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "惟  此  册  以  记  岁  时", h * 0.80f, 0.032f)
    drawColophon(skin, "蘅  芜  阁  藏", h * 0.87f, 0.028f)
}

// ── 2. 霁月长明 · 满月银印「霁」 + 环绕月相 ────────────────────────────────
fun DrawScope.drawJiYueBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.45f

    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)

    drawScatterStars(skin, count = 12, foil, topBias = true)
    drawColophon(skin, "霁  月  长  明", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)

    // 满月银盘 + 印「霁」
    softGlow(Offset(cx, cy), w * 0.22f, skin.palette.paperFiber, 0.26f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(foil.lighten(0.45f), foil.copy(alpha = 0.5f), Color.Transparent),
            center = Offset(cx - w * 0.03f, cy - w * 0.03f), radius = w * 0.15f,
        ),
        radius = w * 0.14f, center = Offset(cx, cy),
    )
    drawMetallicRing(Offset(cx, cy), w * 0.16f, w * 0.006f, foil, accent)
    // 环绕 8 月相
    val orbitR = w * 0.16f + w * 0.055f
    for (i in 0 until 8) {
        val ang = i * (BPI2 / 8) - BPI2 / 4
        drawMoonPhase(Offset(cx + cos(ang) * orbitR, cy + sin(ang) * orbitR), w * 0.022f, i / 8f, foil)
    }
    drawSealStamp(Offset(cx, cy), w * 0.115f, skin.palette.seal, "霁", skin.palette.cover.base)

    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "月  印  千  江", h * 0.80f, 0.032f)
}

// ── 3. 晴川早春 · 樱月圆徽 + 留白樱枝 + 居中「晴」印 ─────────────────────
fun DrawScope.drawQingChuanBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val pink = Color(0xFFFFC7D0)
    val pinkDeep = Color(0xFFE89BAA)
    val cx = w / 2f

    val pad = (w * 0.06f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.005f, foil, accent, alpha = 0.82f)
    val pad2 = pad + w * 0.014f
    drawMetallicFrame(Offset(pad2, pad2), Size(w - pad2 * 2, h - pad2 * 2), w * 0.003f, foil, accent, alpha = 0.45f)

    // 中央淡樱月徽（与封面呼应）
    val emblemY = h * 0.38f
    softGlow(Offset(cx, emblemY), w * 0.18f, pink, 0.12f)
    drawMetallicRing(Offset(cx, emblemY), w * 0.12f, w * 0.005f, foil, accent, alpha = 0.60f)
    drawBookSakura(Offset(cx, emblemY), w * 0.038f, rotDeg = 12f, light = pink, base = pinkDeep, stroke = foil)

    // 右下樱枝（留白和风）
    val branch = Path().apply {
        moveTo(w * 0.90f, h * 0.90f)
        quadraticBezierTo(w * 0.72f, h * 0.78f, w * 0.62f, h * 0.58f)
    }
    drawPath(branch, foil.copy(alpha = 0.65f), style = Stroke(width = (w * 0.0045f).coerceAtLeast(0.65f)))
    listOf(Triple(0.62f, 0.57f, 0.055f), Triple(0.72f, 0.70f, 0.048f), Triple(0.82f, 0.82f, 0.042f))
        .forEachIndexed { i, (fx, fy, fs) ->
            drawBookSakura(Offset(w * fx, h * fy), w * fs, rotDeg = i * 53f, light = pink, base = pinkDeep, stroke = accent)
        }

    drawBookSakura(Offset(w * 0.28f, h * 0.36f), w * 0.020f, rotDeg = 30f,
        light = pink.copy(alpha = 0.75f), base = pinkDeep.copy(alpha = 0.65f), stroke = foil.copy(alpha = 0.35f))
    drawBookSakura(Offset(w * 0.72f, h * 0.24f), w * 0.018f, rotDeg = 110f,
        light = pink.copy(alpha = 0.70f), base = pinkDeep.copy(alpha = 0.60f), stroke = foil.copy(alpha = 0.32f))

    drawSealStamp(Offset(w * 0.28f, h * 0.22f), w * 0.105f, skin.palette.seal, "晴", skin.palette.cover.base)

    val nc = drawContext.canvas.nativeCanvas
    val p = android.graphics.Paint().apply {
        color = pinkDeep.copy(alpha = 0.88f).toArgb()
        textSize = w * 0.034f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.NORMAL)
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = 0.20f
    }
    nc.drawText("一  期  一  会", cx, h * 0.86f, p)
    drawLine(foil.copy(alpha = 0.35f), Offset(cx - w * 0.22f, h * 0.88f), Offset(cx + w * 0.22f, h * 0.88f), 0.55f)
}

// ── 4. 赤焰天书 · 炽金日芒小阵 + 火印「焚」 ───────────────────────────────
fun DrawScope.drawChiYanBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.46f

    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.007f, foil, accent)

    drawColophon(skin, "赤  焰  天  书", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)

    // 炽金日芒(收敛) + 双环 + 火印
    softGlow(Offset(cx, cy), w * 0.24f, Color(0xFFFF7A1A), 0.30f)
    drawRays(Offset(cx, cy), w * 0.16f, w * 0.24f, 24, foil, w * 0.0035f, 0.5f)
    drawRays(Offset(cx, cy), w * 0.16f, w * 0.20f, 24, accent, w * 0.005f, 0.65f,
        angleOffset = (Math.PI / 24).toFloat())
    drawMetallicRing(Offset(cx, cy), w * 0.16f, w * 0.008f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.122f, w * 0.005f, foil, accent, alpha = 0.75f)
    drawSealStamp(Offset(cx, cy), w * 0.13f, skin.palette.seal, "焚", skin.palette.cover.base)

    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "烈  火  炼  心", h * 0.80f, 0.032f)
}

// ── 5. 青鸾翠竹 · 青鸾小徽 + 双竹 + 银印「鸾」 ───────────────────────────
fun DrawScope.drawQingLuanBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val green = skin.palette.cover.accent
    val cx = w / 2f
    val cy = h * 0.44f

    val pad = (w * 0.06f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)

    drawColophon(skin, "青  鸾  翠  竹", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)

    // 中央青鸾环徽
    softGlow(Offset(cx, cy), w * 0.18f, skin.palette.ribbon, 0.24f)
    drawMetallicRing(Offset(cx, cy), w * 0.145f, w * 0.006f, foil, accent)
    drawLuanBird(Offset(cx, cy + h * 0.008f), w * 0.095f, foil, accent)
    // 两侧双竹(收敛)
    drawBambooStalk(Offset(w * 0.17f, h * 0.62f), h * 0.26f, green, foil, leftLeaves = true)
    drawBambooStalk(Offset(w * 0.83f, h * 0.62f), h * 0.26f, green, foil, leftLeaves = false)
    // 银印
    drawSealStamp(Offset(cx, cy + h * 0.0f), w * 0.07f, skin.palette.seal, "鸾", skin.palette.cover.base)

    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "竹  影  鸾  栖", h * 0.80f, 0.032f)
}

// ── 6. 星河长卷 · 星象小罗盘 + 星座 + 银印「渊」 ─────────────────────────
fun DrawScope.drawXingHeBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.46f

    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)

    drawScatterStars(skin, count = 14, foil, topBias = false)
    drawColophon(skin, "星  河  长  卷", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)

    // 小罗盘：双环 + 24 刻度 + 中央印
    softGlow(Offset(cx, cy), w * 0.22f, skin.palette.ribbon, 0.26f)
    drawMetallicRing(Offset(cx, cy), w * 0.16f, w * 0.007f, foil, accent)
    drawMetallicRing(Offset(cx, cy), w * 0.128f, w * 0.004f, foil, accent, alpha = 0.6f)
    for (i in 0 until 24) {
        val ang = i * (BPI2 / 24)
        val major = i % 6 == 0
        val inner = if (major) w * 0.128f else w * 0.142f
        drawLine(foil.copy(alpha = if (major) 0.8f else 0.35f),
            Offset(cx + cos(ang) * inner, cy + sin(ang) * inner),
            Offset(cx + cos(ang) * w * 0.16f, cy + sin(ang) * w * 0.16f),
            strokeWidth = (w * (if (major) 0.0045f else 0.002f)).coerceAtLeast(0.5f))
    }
    drawConstellation(skin, cx, cy, w * 0.185f)
    drawSealStamp(Offset(cx, cy), w * 0.105f, skin.palette.seal, "渊", skin.palette.cover.base)

    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "星  垂  平  野", h * 0.80f, 0.032f)
}

// ── 通用后封(未知皮肤回退)：金属边框 + 中央印「啟」 ────────────────────────
fun DrawScope.drawGenericBack(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val cx = w / 2f
    val cy = h * 0.46f

    val pad = (w * 0.055f).coerceAtLeast(4f)
    drawMetallicFrame(Offset(pad, pad), Size(w - pad * 2, h - pad * 2), w * 0.006f, foil, accent)
    drawColophon(skin, "光  阴  之  录", h * 0.16f, 0.04f)
    drawBackRule(skin, h * 0.205f)
    softGlow(Offset(cx, cy), w * 0.18f, foil, 0.18f)
    drawMetallicRing(Offset(cx, cy), w * 0.15f, w * 0.007f, foil, accent)
    drawSealStamp(Offset(cx, cy), w * 0.13f, skin.palette.seal, "啟", skin.palette.cover.base)
    drawBackRule(skin, h * 0.74f)
    drawColophon(skin, "惟  此  册  以  记  岁  时", h * 0.80f, 0.032f)
}


