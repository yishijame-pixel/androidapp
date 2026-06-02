// ═══════════════════════════════════════════════════════════════════════════
// BookCoverPreview.kt — 通用迷你封面绘制（皮肤一致）
//
// 用于：
//   · SkinPickerSheet 里的缩略卡
//   · MagicBookWidget 主页 3D 魔法书
//   · 任何需要"小尺寸封面预览"的位置
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.funlife.domain.skin.BookSkin

/**
 * 在 [DrawScope] 里画一张迷你封面，使用 [skin] 的调色板 / 材质 / 装饰。
 *
 * 六本书各有专属正面设计，按皮肤 id 分派；未知皮肤回退到通用法阵封面。
 *
 * @param title 封面中央的书名（默认"岁时录"，外部可定制）
 * @param author 封面副标题（默认"一  人  一  册"）
 */
fun DrawScope.drawMiniCover(
    skin: BookSkin,
    title: String = "岁时录",
    author: String = "一  人  一  册"
) {
    // 1. 共同底：渐变 + 材质噪点 + 书脊阴影（所有皮肤一致的基底）
    drawCoverBase(skin)

    // 2. 专属正面设计（每本书独立布局）
    when (skin.id.raw) {
        "builtin::hengwu"    -> drawHengWuFront(skin, title, author)
        "builtin::jiyue"     -> drawJiYueFront(skin, title, author)
        "builtin::qingchuan" -> drawQingChuanFront(skin, title, author)
        "builtin::chiyan"    -> drawChiYanFront(skin, title, author)
        "builtin::qingluan"  -> drawQingLuanFront(skin, title, author)
        "builtin::xinghe"    -> drawXingHeFront(skin, title, author)
        else                 -> drawGenericFront(skin, title, author)
    }

    // 3. 共同收尾：书角磨损做旧
    drawCornerWear(size.width, size.height, skin.palette.foil.base)
}

/** 共同封面基底：渐变 + 皮革纹理 + 体积感 + 书脊阴影。 */
internal fun DrawScope.drawCoverBase(skin: BookSkin) {
    val w = size.width
    val h = size.height
    val palette = skin.palette

    // 封面主渐变（对角，更有布面纵深）
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                palette.cover.base.lighten(0.06f),
                palette.cover.accent,
                palette.coverShadow.darken(0.08f),
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h),
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w, h),
    )
    drawLeatherGrain(skin)
    drawCoverVolume(skin)
    // 左侧书脊折痕阴影
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent),
            startX = 0f,
            endX = w * 0.20f,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(w * 0.20f, h),
    )
    // 书脊折线（烫金细线）
    val foil = palette.foil.base
    drawLine(
        color = foil.copy(alpha = 0.35f),
        start = Offset(w * 0.055f, h * 0.04f),
        end = Offset(w * 0.055f, h * 0.96f),
        strokeWidth = (w * 0.002f).coerceAtLeast(0.6f),
    )
}

/**
 * 通用法阵封面（未知皮肤回退 / 旧版设计保留）：金属边框 + 中心法阵 + 凸压标题。
 */
private fun DrawScope.drawGenericFront(
    skin: BookSkin,
    title: String = "岁时录",
    author: String = "一  人  一  册"
) {
    val w = size.width
    val h = size.height
    val palette = skin.palette
    val materials = skin.materials
    val foil = palette.foil.base

    // 4. 烫金边框
    val pad = (w * 0.05f).coerceAtLeast(4f)
    drawRect(
        color = foil.copy(alpha = 0.85f),
        topLeft = Offset(pad, pad),
        size = Size(w - pad * 2, h - pad * 2),
        style = Stroke(width = (w * 0.006f).coerceAtLeast(1f))
    )
    if (materials.foilDoubleStroke) {
        val pad2 = pad + (w * 0.025f).coerceAtLeast(3f)
        drawRect(
            color = foil.copy(alpha = 0.5f),
            topLeft = Offset(pad2, pad2),
            size = Size(w - pad2 * 2, h - pad2 * 2),
            style = Stroke(width = (w * 0.004f).coerceAtLeast(0.6f))
        )
    }

    // 5. 四角烫金角饰（魔法书典型装饰）
    val cornerSize = w * 0.10f
    val cornerInset = pad + w * 0.015f
    val cornerStroke = (w * 0.005f).coerceAtLeast(0.8f)
    val cornerColor = foil.copy(alpha = 0.9f)
    // 左上角
    drawLine(cornerColor, Offset(cornerInset, cornerInset + cornerSize),
        Offset(cornerInset, cornerInset), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(cornerInset, cornerInset),
        Offset(cornerInset + cornerSize, cornerInset), strokeWidth = cornerStroke)
    drawCircle(cornerColor, radius = cornerStroke * 1.4f,
        center = Offset(cornerInset, cornerInset))
    // 右上
    drawLine(cornerColor, Offset(w - cornerInset - cornerSize, cornerInset),
        Offset(w - cornerInset, cornerInset), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(w - cornerInset, cornerInset),
        Offset(w - cornerInset, cornerInset + cornerSize), strokeWidth = cornerStroke)
    drawCircle(cornerColor, radius = cornerStroke * 1.4f,
        center = Offset(w - cornerInset, cornerInset))
    // 左下
    drawLine(cornerColor, Offset(cornerInset, h - cornerInset - cornerSize),
        Offset(cornerInset, h - cornerInset), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(cornerInset, h - cornerInset),
        Offset(cornerInset + cornerSize, h - cornerInset), strokeWidth = cornerStroke)
    drawCircle(cornerColor, radius = cornerStroke * 1.4f,
        center = Offset(cornerInset, h - cornerInset))
    // 右下
    drawLine(cornerColor, Offset(w - cornerInset - cornerSize, h - cornerInset),
        Offset(w - cornerInset, h - cornerInset), strokeWidth = cornerStroke)
    drawLine(cornerColor, Offset(w - cornerInset, h - cornerInset - cornerSize),
        Offset(w - cornerInset, h - cornerInset), strokeWidth = cornerStroke)
    drawCircle(cornerColor, radius = cornerStroke * 1.4f,
        center = Offset(w - cornerInset, h - cornerInset))

    // 6. 中心法阵（魔法书核心：双圆环 + 八方位刻线）
    val centerX = w / 2f
    val centerY = h * 0.34f
    val ringOuter = w * 0.16f
    val ringInner = w * 0.11f
    drawCircle(
        color = foil.copy(alpha = 0.55f),
        radius = ringOuter,
        center = Offset(centerX, centerY),
        style = Stroke(width = (w * 0.0035f).coerceAtLeast(0.6f))
    )
    drawCircle(
        color = foil.copy(alpha = 0.4f),
        radius = ringInner,
        center = Offset(centerX, centerY),
        style = Stroke(width = (w * 0.003f).coerceAtLeast(0.5f))
    )
    // 八方位刻线
    for (i in 0 until 8) {
        val ang = i * (Math.PI * 2 / 8)
        val cs = kotlin.math.cos(ang).toFloat()
        val sn = kotlin.math.sin(ang).toFloat()
        drawLine(
            color = foil.copy(alpha = 0.5f),
            start = Offset(centerX + cs * ringInner, centerY + sn * ringInner),
            end = Offset(centerX + cs * ringOuter, centerY + sn * ringOuter),
            strokeWidth = (w * 0.003f).coerceAtLeast(0.5f)
        )
    }
    // 中心小宝石
    drawCircle(
        color = foil.copy(alpha = 0.85f),
        radius = w * 0.012f,
        center = Offset(centerX, centerY)
    )

    // 7. 中央标题（三层凸压立体：暗阴影 + 主烫金 + 亮高光）
    val nc = drawContext.canvas.nativeCanvas
    val titleSize = w * 0.14f
    val titleY = h * 0.62f
    val titleTypeface = android.graphics.Typeface.create(
        android.graphics.Typeface.SERIF,
        android.graphics.Typeface.BOLD
    )
    // [1] 暗色阴影：向右下偏移，制造立体下沉投影
    val embossShadow = android.graphics.Paint().apply {
        color = Color.Black.copy(alpha = 0.55f).toArgb()
        textSize = titleSize
        isAntiAlias = true
        typeface = titleTypeface
        textAlign = android.graphics.Paint.Align.CENTER
    }
    nc.drawText(title, w / 2f + w * 0.004f, titleY + w * 0.006f, embossShadow)
    // [2] 主烫金主体
    val paint = android.graphics.Paint().apply {
        color = foil.toArgb()
        textSize = titleSize
        isAntiAlias = true
        typeface = titleTypeface
        textAlign = android.graphics.Paint.Align.CENTER
        setShadowLayer(w * 0.03f, 0f, w * 0.006f, palette.foil.accent.toArgb())
    }
    nc.drawText(title, w / 2f, titleY, paint)
    // [3] 亮色高光：向左上偏移、半透明，让烫金顶部"反光"突起
    val embossHighlight = android.graphics.Paint().apply {
        color = Color.White.copy(alpha = 0.42f).toArgb()
        textSize = titleSize
        isAntiAlias = true
        typeface = titleTypeface
        textAlign = android.graphics.Paint.Align.CENTER
    }
    nc.drawText(title, w / 2f - w * 0.0025f, titleY - w * 0.003f, embossHighlight)

    // 8. 副标题（小一号）
    val subPaint = android.graphics.Paint().apply {
        color = foil.copy(alpha = 0.75f).toArgb()
        textSize = w * 0.042f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.SERIF,
            android.graphics.Typeface.NORMAL
        )
        textAlign = android.graphics.Paint.Align.CENTER
        letterSpacing = skin.typography.titleLetterSpacingEm.coerceAtMost(0.4f)
    }
    nc.drawText("一  人  一  册", w / 2f, h * 0.73f, subPaint)

    // 9. 底部装饰横线（双线）
    val bottomY1 = h * 0.84f
    val bottomY2 = h * 0.86f
    drawLine(
        color = foil.copy(alpha = 0.6f),
        start = Offset(w * 0.32f, bottomY1),
        end = Offset(w * 0.68f, bottomY1),
        strokeWidth = (w * 0.0035f).coerceAtLeast(0.6f)
    )
    drawLine(
        color = foil.copy(alpha = 0.4f),
        start = Offset(w * 0.38f, bottomY2),
        end = Offset(w * 0.62f, bottomY2),
        strokeWidth = (w * 0.0025f).coerceAtLeast(0.5f)
    )
    // 底部菱形
    val diamondCx = w / 2f
    val diamondCy = h * 0.92f
    val diamondR = w * 0.018f
    val diamondPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(diamondCx, diamondCy - diamondR)
        lineTo(diamondCx + diamondR, diamondCy)
        lineTo(diamondCx, diamondCy + diamondR)
        lineTo(diamondCx - diamondR, diamondCy)
        close()
    }
    drawPath(diamondPath, foil.copy(alpha = 0.7f),
        style = Stroke(width = (w * 0.003f).coerceAtLeast(0.5f)))

    // 10. 装饰类型专属点缀（每皮肤独立四角主题装饰）
    when (skin.ornaments.cornerOrnament) {
        com.example.funlife.domain.skin.OrnamentType.Sakura -> drawSakuraOrnaments(skin, w, h)
        com.example.funlife.domain.skin.OrnamentType.Vine   -> drawVineOrnaments(skin, w, h)
        com.example.funlife.domain.skin.OrnamentType.Moon   -> drawMoonOrnaments(skin, w, h)
        com.example.funlife.domain.skin.OrnamentType.Flame  -> drawFlameOrnaments(skin, w, h)
        com.example.funlife.domain.skin.OrnamentType.Bamboo -> drawBambooOrnaments(skin, w, h)
        com.example.funlife.domain.skin.OrnamentType.Stars  -> drawStarsOrnaments(skin, w, h)
        else -> { /* None / Rune 走默认 */ }
    }
    // 书角磨损由 drawMiniCover 统一收尾
}

/** 蘅芜旧卷：四角"如意云纹"——三段连续的卷云曲线（Path 多条曲线模拟）。 */
internal fun DrawScope.drawVineOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val accent = skin.palette.foil.accent
    val pad = w * 0.07f
    val sz = w * 0.11f
    val sw = (w * 0.005f).coerceAtLeast(0.7f)
    // 4 角同样的如意纹（朝向不同）
    drawRuyiCloud(Offset(pad, pad),               sz, 0,   foil, accent, sw)
    drawRuyiCloud(Offset(w - pad, pad),           sz, 90,  foil, accent, sw)
    drawRuyiCloud(Offset(w - pad, h - pad),       sz, 180, foil, accent, sw)
    drawRuyiCloud(Offset(pad, h - pad),           sz, 270, foil, accent, sw)
}

internal fun DrawScope.drawRuyiCloud(
    center: Offset, size: Float, rotDeg: Int,
    foil: Color, accent: Color, stroke: Float,
) {
    val rad = rotDeg * Math.PI.toFloat() / 180f
    val cs = kotlin.math.cos(rad.toDouble()).toFloat()
    val sn = kotlin.math.sin(rad.toDouble()).toFloat()
    fun rotate(dx: Float, dy: Float) = Offset(
        center.x + dx * cs - dy * sn,
        center.y + dx * sn + dy * cs,
    )
    // 三个嵌套圆弧（如意头）
    for (k in 0 until 3) {
        val r = size * (0.45f - k * 0.10f)
        val cx = rotate(size * 0.0f, size * 0.12f * k)
        drawCircle(
            color = if (k == 0) foil else accent.copy(alpha = 0.7f),
            radius = r,
            center = cx,
            style = Stroke(width = stroke),
        )
    }
    // 一道连接弧线（向内卷收）
    val p = androidx.compose.ui.graphics.Path().apply {
        val a = rotate(size * 0.42f, size * 0.10f)
        val b = rotate(size * 0.65f, size * 0.45f)
        val c = rotate(size * 0.30f, size * 0.65f)
        moveTo(a.x, a.y)
        quadraticBezierTo(b.x, b.y, c.x, c.y)
    }
    drawPath(p, foil.copy(alpha = 0.7f), style = Stroke(width = stroke))
}

/** 霁月长明：四角"月相"——左上朔/右上望/左下上弦/右下下弦。 */
internal fun DrawScope.drawMoonOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val ring = foil.copy(alpha = 0.85f)
    val pad = w * 0.10f
    val r = w * 0.055f
    val sw = (w * 0.005f).coerceAtLeast(0.7f)
    // 左上：满月轮廓（圆环）
    drawCircle(ring, r, Offset(pad, pad), style = Stroke(width = sw))
    drawCircle(foil.copy(alpha = 0.18f), r * 0.85f, Offset(pad, pad))
    // 右上：上弦月（半圆 + 内侧曲线）
    drawArc(color = ring, startAngle = 270f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w - pad - r, pad - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        style = Stroke(width = sw))
    // 左下：新月（细弯月）
    drawArc(color = ring, startAngle = 30f, sweepAngle = 300f, useCenter = false,
        topLeft = Offset(pad - r, h - pad - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        style = Stroke(width = sw))
    // 右下：下弦月
    drawArc(color = ring, startAngle = 90f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w - pad - r, h - pad - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        style = Stroke(width = sw))
    // 4 个月相中心点上各加一颗星
    listOf(Offset(pad, pad), Offset(w - pad, pad),
        Offset(pad, h - pad), Offset(w - pad, h - pad)).forEachIndexed { i, c ->
        drawCircle(foil, r * 0.10f, Offset(c.x, c.y - r * 1.6f))
    }
}

/** 赤焰天书：四角向内的"火舌曲线"（多笔三角焰形）。 */
internal fun DrawScope.drawFlameOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val pad = w * 0.08f
    val sz = w * 0.10f
    val sw = (w * 0.005f).coerceAtLeast(0.7f)
    drawFlameTongue(Offset(pad, pad), sz, 45f, foil, sw)
    drawFlameTongue(Offset(w - pad, pad), sz, -45f, foil, sw)
    drawFlameTongue(Offset(w - pad, h - pad), sz, -135f, foil, sw)
    drawFlameTongue(Offset(pad, h - pad), sz, 135f, foil, sw)
}

private fun DrawScope.drawFlameTongue(
    base: Offset, size: Float, rotDeg: Float, color: Color, stroke: Float,
) {
    val rad = rotDeg * Math.PI.toFloat() / 180f
    val cs = kotlin.math.cos(rad.toDouble()).toFloat()
    val sn = kotlin.math.sin(rad.toDouble()).toFloat()
    fun pt(dx: Float, dy: Float) = Offset(base.x + dx * cs - dy * sn, base.y + dx * sn + dy * cs)
    // 三笔向"内对角"的火焰
    for (k in 0 until 3) {
        val off = k * size * 0.18f
        val p = androidx.compose.ui.graphics.Path().apply {
            val a = pt(off, 0f)
            val b = pt(off + size * 0.18f, size * 0.45f)
            val c = pt(off + size * 0.05f, size * 0.95f)
            moveTo(a.x, a.y)
            quadraticBezierTo(b.x, b.y, c.x, c.y)
        }
        drawPath(p, color.copy(alpha = 0.85f - k * 0.18f), style = Stroke(width = stroke))
    }
    // 顶端一颗火星
    drawCircle(color.copy(alpha = 0.95f), size * 0.05f, pt(size * 0.05f, size * 0.95f))
}

/** 青鸾翠竹：四角竹枝叶子。 */
internal fun DrawScope.drawBambooOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val green = skin.palette.cover.accent
    val pad = w * 0.08f
    val sz = w * 0.13f
    val sw = (w * 0.006f).coerceAtLeast(0.8f)
    drawBambooSprig(Offset(pad, pad), sz, 35f, foil, green, sw)
    drawBambooSprig(Offset(w - pad, pad), sz, -35f, foil, green, sw)
    drawBambooSprig(Offset(w - pad, h - pad), sz, -145f, foil, green, sw)
    drawBambooSprig(Offset(pad, h - pad), sz, 145f, foil, green, sw)
}

private fun DrawScope.drawBambooSprig(
    base: Offset, size: Float, rotDeg: Float,
    foil: Color, green: Color, stroke: Float,
) {
    val rad = rotDeg * Math.PI.toFloat() / 180f
    val cs = kotlin.math.cos(rad.toDouble()).toFloat()
    val sn = kotlin.math.sin(rad.toDouble()).toFloat()
    fun pt(dx: Float, dy: Float) = Offset(base.x + dx * cs - dy * sn, base.y + dx * sn + dy * cs)
    // 主枝
    drawLine(foil.copy(alpha = 0.85f), pt(0f, 0f), pt(size * 0.0f, size),
        strokeWidth = stroke)
    // 三对叶子
    for (k in 1..3) {
        val y = size * (0.20f + k * 0.22f)
        // 左叶
        val lt = pt(-size * 0.30f, y - size * 0.05f)
        val lb = pt(0f, y)
        val lp = androidx.compose.ui.graphics.Path().apply {
            moveTo(lb.x, lb.y)
            quadraticBezierTo((lt.x + lb.x) / 2f - size * 0.1f, (lt.y + lb.y) / 2f, lt.x, lt.y)
        }
        drawPath(lp, green.copy(alpha = 0.75f), style = Stroke(width = stroke * 1.5f))
        // 右叶
        val rt = pt(size * 0.32f, y - size * 0.10f)
        val rp = androidx.compose.ui.graphics.Path().apply {
            moveTo(lb.x, lb.y)
            quadraticBezierTo((rt.x + lb.x) / 2f + size * 0.1f, (rt.y + lb.y) / 2f, rt.x, rt.y)
        }
        drawPath(rp, green.copy(alpha = 0.75f), style = Stroke(width = stroke * 1.5f))
    }
    // 竹节小段
    for (k in 1..3) {
        val y = size * k * 0.30f
        drawLine(foil, pt(-size * 0.04f, y), pt(size * 0.04f, y), strokeWidth = stroke * 0.8f)
    }
}

/** 星河长卷：四角"星座连线"——3-4 颗星 + 烫银连线。 */
internal fun DrawScope.drawStarsOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val sw = (w * 0.0035f).coerceAtLeast(0.5f)
    // 4 角各一个迷你"星座"
    val pads = listOf(
        Offset(w * 0.10f, h * 0.08f),
        Offset(w * 0.90f, h * 0.08f),
        Offset(w * 0.10f, h * 0.92f),
        Offset(w * 0.90f, h * 0.92f),
    )
    pads.forEachIndexed { idx, c ->
        // 3-4 颗星，按伪随机偏移
        val pts = listOf(
            Offset(c.x, c.y),
            Offset(c.x + w * 0.05f, c.y + w * 0.04f * (if (idx % 2 == 0) 1f else -1f)),
            Offset(c.x - w * 0.04f, c.y + w * 0.06f * (if (idx < 2) 1f else -1f)),
            Offset(c.x + w * 0.02f, c.y + w * 0.09f * (if (idx < 2) 1f else -1f)),
        )
        // 连线
        for (i in 0 until pts.size - 1) {
            drawLine(foil.copy(alpha = 0.6f), pts[i], pts[i + 1], strokeWidth = sw)
        }
        // 星点
        pts.forEachIndexed { i, p ->
            val r = w * (0.013f - i * 0.002f)
            drawCircle(foil, r, p)
            drawCircle(foil.copy(alpha = 0.4f), r * 2.4f, p)
        }
    }
}

/** 书角磨损：四个角加不规则的白色/淡色擦痕（alpha 极低，古旧感）。 */
internal fun DrawScope.drawCornerWear(w: Float, h: Float, foil: Color) {
    val wearColor = Color(0xFFFFFAEC).copy(alpha = 0.18f)
    val corners = listOf(
        Offset(0f, 0f),
        Offset(w, 0f),
        Offset(0f, h),
        Offset(w, h),
    )
    corners.forEachIndexed { ci, corner ->
        // 每角 6-8 颗不规则磨损点
        for (i in 0 until 7) {
            val seed = ci * 31 + i * 7
            val rx = ((seed * 9301 + 49297) % 233280) / 233280f
            val ry = ((seed * 12289 + 33191) % 233280) / 233280f
            val dx = (rx - 0.5f) * w * 0.08f
            val dy = (ry - 0.5f) * h * 0.06f
            val px = (corner.x + (if (corner.x < 1f) w * 0.04f else -w * 0.04f) + dx)
                .coerceIn(0f, w)
            val py = (corner.y + (if (corner.y < 1f) h * 0.03f else -h * 0.03f) + dy)
                .coerceIn(0f, h)
            val r = 1.5f + rx * 3f
            drawCircle(wearColor, r, Offset(px, py))
        }
        // 一道更明显的角部"擦白"边线
        val edgeLen = w * 0.06f
        val sx = corner.x + (if (corner.x < 1f) edgeLen else -edgeLen)
        val sy = corner.y
        val ex = corner.x
        val ey = corner.y + (if (corner.y < 1f) edgeLen else -edgeLen)
        drawLine(wearColor.copy(alpha = 0.25f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = 1.4f)
    }
}

/** 晴川早春：在封面四角 + 法阵周围叠加樱花瓣装饰（玫瑰金描边 + 淡粉填充）。 */
internal fun DrawScope.drawSakuraOrnaments(skin: BookSkin, w: Float, h: Float) {
    val foil = skin.palette.foil.base
    val foilAccent = skin.palette.foil.accent
    val pink = Color(0xFFFFC7D0)
    val pinkDeep = Color(0xFFE89BAA)

    // 4 朵角樱花（左上、右上、左下、右下，朝中心方向旋转）
    val cornerPad = w * 0.13f
    val sz = w * 0.075f
    drawBookSakura(Offset(cornerPad, cornerPad), sz, rotDeg = -30f,
        light = pink, base = pinkDeep, stroke = foilAccent)
    drawBookSakura(Offset(w - cornerPad, cornerPad), sz, rotDeg = 30f,
        light = pink, base = pinkDeep, stroke = foilAccent)
    drawBookSakura(Offset(cornerPad, h - cornerPad), sz, rotDeg = -150f,
        light = pink, base = pinkDeep, stroke = foilAccent)
    drawBookSakura(Offset(w - cornerPad, h - cornerPad), sz, rotDeg = 150f,
        light = pink, base = pinkDeep, stroke = foilAccent)

    // 法阵周围撒几片小花（中心法阵 y ≈ h*0.34）
    val cx = w / 2f
    val ringY = h * 0.34f
    val ringR = w * 0.20f
    for (i in 0 until 6) {
        val ang = (i * 60f - 15f) * (Math.PI / 180.0)
        val rOff = ringR * (0.95f + (i % 2) * 0.18f)
        val px = cx + kotlin.math.cos(ang).toFloat() * rOff
        val py = ringY + kotlin.math.sin(ang).toFloat() * rOff * 0.7f
        val smallSz = w * (0.025f + (i % 3) * 0.008f)
        val rot = i * 47f
        drawBookSakura(Offset(px, py), smallSz, rotDeg = rot,
            light = pink.copy(alpha = 0.85f), base = pinkDeep.copy(alpha = 0.75f),
            stroke = foil.copy(alpha = 0.5f))
    }

    // 标题下方一条飘落樱花点缀线（h*0.78 横向 4 片）
    val titleBelowY = h * 0.79f
    for (i in 0 until 4) {
        val px = w * (0.28f + i * 0.15f)
        val py = titleBelowY + (i % 2) * w * 0.012f
        drawBookSakura(Offset(px, py), w * 0.018f, rotDeg = i * 53f,
            light = pink, base = pinkDeep, stroke = foil.copy(alpha = 0.6f))
    }
}

/** 静态樱花（5 瓣，烫金描边 + 淡粉填充）。 */
internal fun DrawScope.drawBookSakura(
    center: Offset, size: Float, rotDeg: Float,
    light: Color, base: Color, stroke: Color,
) {
    val rad = (rotDeg * Math.PI / 180.0).toFloat()
    for (k in 0 until 5) {
        val a = k * (Math.PI * 2f / 5f).toFloat() + rad
        val px = center.x + kotlin.math.cos(a.toDouble()).toFloat() * size * 0.55f
        val py = center.y + kotlin.math.sin(a.toDouble()).toFloat() * size * 0.55f
        // 花瓣填充
        drawCircle(color = base, radius = size * 0.42f, center = Offset(px, py))
        drawCircle(color = light, radius = size * 0.30f,
            center = Offset(px - size * 0.05f, py - size * 0.05f))
        // 烫金描边轮廓
        drawCircle(color = stroke.copy(alpha = 0.55f), radius = size * 0.42f,
            center = Offset(px, py),
            style = Stroke(width = size * 0.06f))
    }
    // 花心
    drawCircle(color = stroke, radius = size * 0.14f, center = center)
    drawCircle(color = Color(0xFFFFE066), radius = size * 0.07f, center = center)
}
