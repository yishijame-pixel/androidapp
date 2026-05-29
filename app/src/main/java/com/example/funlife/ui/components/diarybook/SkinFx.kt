// ═══════════════════════════════════════════════════════════════════════════
// SkinFx.kt — 皮肤专属魔法粒子特效层
//
// 每个皮肤一套 fx：
//   • 蘅芜旧卷  → 烫金尘屑（缓慢上升的金光点）
//   • 霁月长明  → 银白星点 + 周期性紫色闪电折线
//   • 晴川早春  → 樱花瓣旋转飘落
//   • 赤焰天书  → 火焰粒子（橙红向上飘 + 飞溅火星）
//   • 青鸾翠竹  → 翠绿光斑 + 飘落竹叶
//   • 星河长卷  → 蓝色流星划过 + 闪烁星点
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.funlife.domain.skin.BookSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 皮肤专属特效层。优先加载 assets/skin_fx/{xxx}.json 的 Lottie 动画；
 * 文件不存在 / 加载失败时自动回退到手写 Compose Canvas 版本。
 */
@Composable
fun SkinFx(
    skin: BookSkin,
    widthDp: Dp,
    heightDp: Dp,
) {
    val assetName = lottieAssetForSkin(skin.id.raw)
    val compositionResult = if (assetName != null) {
        com.airbnb.lottie.compose.rememberLottieComposition(
            com.airbnb.lottie.compose.LottieCompositionSpec.Asset(assetName)
        )
    } else null
    val composition = compositionResult?.value

    // 不再使用 darkBacking——特效直接画在 hub 背景上，避免硬色块与浅黄页面强对比
    val needsDarkBacking = false

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (composition != null) {
            // Lottie 加载成功：用专业级动画
            if (needsDarkBacking) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawDarkVignette(skin.id.raw)
                }
            }
            com.airbnb.lottie.compose.LottieAnimation(
                composition = composition,
                iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // 没找到 Lottie / 还在加载 / 加载失败 → 回退到手写 Canvas 特效
            LegacySkinFxCanvas(
                skin = skin,
                widthDp = widthDp,
                heightDp = heightDp,
                needsDarkBacking = needsDarkBacking,
            )
        }
    }
}

/** 皮肤 raw id → assets 下的 Lottie 文件路径。返回 null 则没有 Lottie 直接用 fallback。 */
private fun lottieAssetForSkin(rawId: String): String? = when (rawId) {
    "builtin::chiyan"    -> "skin_fx/chiyan.json"
    "builtin::jiyue"     -> "skin_fx/jiyue.json"
    "builtin::qingchuan" -> "skin_fx/qingchuan.json"
    "builtin::qingluan"  -> "skin_fx/qingluan.json"
    "builtin::xinghe"    -> "skin_fx/xinghe.json"
    "builtin::hengwu"    -> "skin_fx/hengwu.json"
    else                 -> null
}

/** 兜底：手写 Compose Canvas 特效（Lottie 未提供 / 加载失败时使用）。 */
@Composable
private fun LegacySkinFxCanvas(
    skin: BookSkin,
    widthDp: Dp,
    heightDp: Dp,
    needsDarkBacking: Boolean,
) {
    var t by remember { mutableStateOf(0f) }
    // lifecycle 暂停：仅 RESUMED 时驱动 frame loop（后台 / 切走时停帧省电）
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(true) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            isActive = (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) ||
                isActive && ev != androidx.lifecycle.Lifecycle.Event.ON_PAUSE
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(isActive) {
        if (!isActive) return@LaunchedEffect
        var t0 = -1L
        while (true) {
            withFrameNanos { now ->
                if (t0 < 0) t0 = now
                t = (now - t0) / 1_000_000_000f
            }
        }
    }

    val foil   = skin.palette.foil.base
    val cover  = skin.palette.cover.base
    val accent = skin.palette.cover.accent
    val ribbon = skin.palette.ribbon

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        if (needsDarkBacking) {
            drawDarkVignette(skin.id.raw)
        }
        when (skin.id.raw) {
            "builtin::chiyan"    -> drawFire(t)
            "builtin::jiyue"     -> drawLightningStars(t, foil, accent)
            "builtin::qingchuan" -> drawSakura(t, foil)
            "builtin::qingluan"  -> drawBambooLeaves(t, cover, foil)
            "builtin::xinghe"    -> drawMeteors(t, foil, ribbon)
            else                 -> drawGoldDust(t, foil)
        }
    }
}

/** 火焰/雷电场景的暗色烘底，竖向渐变带，只在边缘加深，不挡书本。 */
private fun DrawScope.drawDarkVignette(skinIdRaw: String) {
    val w = size.width
    val h = size.height
    when (skinIdRaw) {
        "builtin::chiyan" -> {
            // 底部 35% 渐变深红黑色（火焰区背景）
            val topY = h * 0.65f
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x402A0800),
                        Color(0xB01A0500),
                    ),
                    startY = topY,
                    endY   = h,
                ),
                topLeft = Offset(0f, topY),
                size = androidx.compose.ui.geometry.Size(w, h - topY),
            )
        }
        "builtin::jiyue" -> {
            // 顶部 45% 渐变深紫黑色（雷电天幕）
            val botY = h * 0.45f
            drawRect(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xB00A0420),
                        Color(0x550A0420),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY   = botY,
                ),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, botY),
            )
        }
    }
}

/* ── helpers ──────────────────────────────────────────────────────────── */

/** 伪随机：i 给定 → 同一颗粒子拿到稳定的 0..1 */
private fun rand(i: Int, salt: Int = 0): Float {
    val v = (i * 9301 + salt * 49297) % 233280
    return ((v + 233280) % 233280) / 233280f
}

/**
 * 加色辉光圆点：用 radialGradient 让 alpha 从中心 1 平滑衰减到边缘 0（模拟高斯模糊），
 * 再用 BlendMode.Plus 在离屏 layer 内做加色合成。
 *
 * 关键：必须在父 Canvas 设置 compositingStrategy = Offscreen，
 * 否则 Plus 会作用于父背景导致颜色饱和洗白。
 */
private fun DrawScope.glowCircle(
    center: Offset, radius: Float, color: Color, blur: Float = 0f,
) {
    if (color.alpha <= 0f) return
    val totalR = radius + blur
    if (totalR <= 0.5f) return
    if (blur <= 0.5f) {
        // 实心圆（无模糊）
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            blendMode = BlendMode.Plus,
        )
    } else {
        // 用径向渐变模拟模糊：中心实色 → 边缘透明
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color,
                    color.copy(alpha = color.alpha * 0.6f),
                    color.copy(alpha = 0f),
                ),
                center = center,
                radius = totalR,
            ),
            radius = totalR,
            center = center,
            blendMode = BlendMode.Plus,
        )
    }
}

/** 加色辉光线段：4 层不同 stroke 宽度 + 半透明叠加，模拟边缘渐变。 */
private fun DrawScope.glowLine(
    from: Offset, to: Offset, width: Float, color: Color, blur: Float = 0f,
) {
    if (color.alpha <= 0f) return
    val totalW = width + blur
    if (totalW <= 0.5f) return
    if (blur <= 0.5f) {
        drawLine(
            color = color,
            start = from, end = to,
            strokeWidth = width.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus,
        )
        return
    }
    // 4 层从外宽淡 → 内窄实，模拟模糊
    val layers = 4
    for (i in 0 until layers) {
        val k = (layers - i) / layers.toFloat()                  // 1, 0.75, 0.5, 0.25
        val w = (width * (0.4f + k * 0.6f)) + blur * k * 1.4f
        val a = color.alpha * (0.18f + (1f - k) * 0.55f)         // 外层淡，内层浓
        drawLine(
            color = color.copy(alpha = a),
            start = from, end = to,
            strokeWidth = w.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus,
        )
    }
}

/** 整屏闪光（加色叠加，不用 SCREEN 模式）。 */
private fun DrawScope.screenFlash(color: Color) {
    drawRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        blendMode = BlendMode.Plus,
    )
}

/* ── 1. 蘅芜旧卷：烫金尘屑 ────────────────────────────────────────────── */

private fun DrawScope.drawGoldDust(t: Float, foil: Color) {
    val w = size.width
    val h = size.height

    // ── 1. 金粉尘屑（保留原效果，20 颗）──
    val n = 20
    for (i in 0 until n) {
        val baseX = rand(i, 1) * w
        val phase = (t * (0.20f + rand(i, 2) * 0.35f) + rand(i, 3)) % 1f
        val py = h * (1f - phase)
        val sway = sin((t * (0.6f + rand(i, 4) * 0.8f) + rand(i, 5) * 6.28f).toDouble()).toFloat()
        val px = baseX + sway * 14f
        val twinkle = 0.55f + 0.45f * sin((t * 2f + i * 0.7f).toDouble()).toFloat()
        val r = 1.2f + rand(i, 6) * 1.8f
        val a = (0.35f + rand(i, 7) * 0.45f) * (0.4f + 0.6f * twinkle) *
                (1f - phase * 0.4f)
        drawCircle(foil.copy(alpha = a),         radius = r,        center = Offset(px, py))
        drawCircle(foil.copy(alpha = a * 0.32f), radius = r * 3.4f, center = Offset(px, py))
    }

    // ── 2. 飘散的古字符号（小篆/甲骨片段感），8 个，缓慢上飘旋转溶解 ──
    val glyphs = 8
    for (i in 0 until glyphs) {
        val phase = (t * (0.10f + rand(i, 21) * 0.10f) + rand(i, 22)) % 1f
        val baseX = rand(i, 23) * w
        val sway = sin((t * 0.4f + i * 1.3f).toDouble()).toFloat() * 12f
        val px = baseX + sway
        val py = h * (1f - phase) - 10f
        val sz = 6f + rand(i, 24) * 6f                          // 6-12
        val rot = (rand(i, 25) - 0.5f) * 60f + t * 4f
        // 在 phase 中段最浓，两头淡（淡入淡出）
        val a = (1f - kotlin.math.abs(phase - 0.5f) * 2f).coerceIn(0f, 1f) * 0.55f
        if (a < 0.02f) continue
        rotate(rot, Offset(px, py)) {
            drawSealGlyph(Offset(px, py), sz, foil.copy(alpha = a), glyphIdx = i % 4)
        }
    }

    // ── 3. 周期印章红光脉冲（5s 一次，从屏幕中央偏右上扩散） ──
    val sealCycle = 5.5f
    val sealPhase = (t % sealCycle) / sealCycle
    if (sealPhase < 0.35f) {
        val pp = sealPhase / 0.35f
        val sealColor = Color(0xFFB02828)
        val cx = w * 0.62f
        val cy = h * 0.35f
        val pulse = sin((pp * PI).toFloat())
        val a = (pulse * 0.55f).coerceIn(0f, 1f)
        drawCircle(sealColor.copy(alpha = a * 0.4f), 18f + pp * 30f, Offset(cx, cy))
        drawCircle(sealColor.copy(alpha = a * 0.18f), 36f + pp * 60f, Offset(cx, cy))
        drawCircle(sealColor.copy(alpha = a * 0.08f), 80f + pp * 120f, Offset(cx, cy))
    }
}

/** 极简化的"古字符号"：用几笔横竖斜画出印章感图形（4 种循环）。 */
private fun DrawScope.drawSealGlyph(center: Offset, size: Float, color: Color, glyphIdx: Int) {
    val s = size
    val x = center.x
    val y = center.y
    val stroke = (s * 0.18f).coerceAtLeast(1f)
    when (glyphIdx) {
        0 -> {                                                          // "丨" + 两横
            drawLine(color, Offset(x, y - s), Offset(x, y + s), stroke)
            drawLine(color, Offset(x - s * 0.7f, y - s * 0.4f),
                Offset(x + s * 0.7f, y - s * 0.4f), stroke)
            drawLine(color, Offset(x - s * 0.7f, y + s * 0.4f),
                Offset(x + s * 0.7f, y + s * 0.4f), stroke)
        }
        1 -> {                                                          // "口" 方框
            drawLine(color, Offset(x - s, y - s), Offset(x + s, y - s), stroke)
            drawLine(color, Offset(x + s, y - s), Offset(x + s, y + s), stroke)
            drawLine(color, Offset(x + s, y + s), Offset(x - s, y + s), stroke)
            drawLine(color, Offset(x - s, y + s), Offset(x - s, y - s), stroke)
        }
        2 -> {                                                          // "人" 撇捺
            drawLine(color, Offset(x, y - s), Offset(x - s * 0.8f, y + s), stroke)
            drawLine(color, Offset(x, y - s), Offset(x + s * 0.8f, y + s), stroke)
        }
        else -> {                                                       // "三" 三横
            drawLine(color, Offset(x - s, y - s * 0.6f),
                Offset(x + s, y - s * 0.6f), stroke)
            drawLine(color, Offset(x - s, y),
                Offset(x + s, y), stroke)
            drawLine(color, Offset(x - s, y + s * 0.6f),
                Offset(x + s, y + s * 0.6f), stroke)
        }
    }
}

/* ── 2. 霁月长明：银白星点 + 紫色闪电 ──────────────────────────────────── */

private fun DrawScope.drawLightningStars(t: Float, foil: Color, accent: Color) {
    val cx = size.width  / 2f
    val cy = size.height / 2f

    // 银白星点
    val starN = 16
    for (i in 0 until starN) {
        val seed = i * 0.183f
        val angle = (t * 0.30f + seed * 6.28f).toDouble()
        val rx = size.width  * 0.45f * (0.55f + rand(i, 11))
        val ry = size.height * 0.42f * (0.55f + rand(i, 12) * 0.8f)
        val px = cx + cos(angle).toFloat() * rx
        val py = cy + sin(angle * 1.2).toFloat() * ry
        val twinkle = 0.5f + 0.5f * sin((t * 4f + i * 1.7f).toDouble()).toFloat()
        glowCircle(Offset(px, py), 1.8f, foil.copy(alpha = 0.85f * twinkle), blur = 0f)
        glowCircle(Offset(px, py), 6f,   foil.copy(alpha = 0.40f * twinkle), blur = 6f)
    }

    // ── 闪电：每 2.4s 一轮，一轮内 3 次连劈 + 长尾余辉 ──
    val cycle = 2.4f
    val cyclePhase = t % cycle
    val cycleIdx = (t / cycle).toInt()
    val strikes = floatArrayOf(0.00f, 0.07f, 0.16f)
    val strikeDur = 0.05f
    val afterglowDur = 0.45f

    var globalFlash = 0f
    for ((idx, sStart) in strikes.withIndex()) {
        val dt = cyclePhase - sStart
        val intensity = when {
            dt < 0f                                                       -> 0f
            dt < strikeDur                                                -> 1f
            idx == strikes.lastIndex && dt < strikeDur + afterglowDur     ->
                ((strikeDur + afterglowDur - dt) / afterglowDur).coerceAtLeast(0f) * 0.55f
            else                                                          -> 0f
        }
        if (intensity <= 0f) continue
        globalFlash = kotlin.math.max(globalFlash, intensity)

        val seed = cycleIdx * 100 + idx
        val rng = java.util.Random((seed * 31337L) xor 0xBADF00DL)
        val sx = cx + (rng.nextFloat() - 0.5f) * size.width * 0.8f
        val ex = cx + (rng.nextFloat() - 0.5f) * size.width * 0.4f
        drawRealLightning(
            from = Offset(sx, 0f),
            to   = Offset(ex, size.height),
            rng = rng, depth = 0, intensity = intensity,
            core  = Color(0xFFFFFFFF),
            glow  = Color(0xFFCEB6FF),
            outer = accent,
        )
    }
    // 整屏紫色闪光
    if (globalFlash > 0.15f) {
        screenFlash(Color(0xFFE0D0FF).copy(alpha = globalFlash * 0.22f))
    }
}

/**
 * 真闪电：递归分叉，每段三层加色辉光（外深紫模糊 → 中淡紫模糊 → 心白光）。
 */
private fun DrawScope.drawRealLightning(
    from: Offset, to: Offset,
    rng: java.util.Random, depth: Int, intensity: Float,
    core: Color, glow: Color, outer: Color,
) {
    val segments = if (depth == 0) 14 else 6
    val maxJitter = if (depth == 0) 28f else 14f
    val outerW = if (depth == 0) 28f else 14f
    val midW   = if (depth == 0) 9f  else 4f
    val coreW  = if (depth == 0) 2.4f else 1.2f

    var prev = from
    val branches = mutableListOf<Offset>()
    for (i in 1..segments) {
        val tt = i / segments.toFloat()
        val baseX = from.x + (to.x - from.x) * tt
        val baseY = from.y + (to.y - from.y) * tt
        val jitter = (rng.nextFloat() - 0.5f) * maxJitter * (1f - kotlin.math.abs(tt - 0.5f))
        val cur = Offset(baseX + jitter, baseY)
        glowLine(prev, cur, outerW, outer.copy(alpha = intensity * 0.55f), blur = 22f)
        glowLine(prev, cur, midW,   glow .copy(alpha = intensity * 0.85f), blur = 8f)
        glowLine(prev, cur, coreW,  core .copy(alpha = intensity),         blur = 1.5f)
        if (depth == 0 && i in 3..segments - 2 && rng.nextFloat() < 0.55f) {
            branches += cur
        }
        prev = cur
    }
    if (depth < 1) {
        for (bp in branches) {
            val len = 60f + rng.nextFloat() * 80f
            val ang = (40f + rng.nextFloat() * 100f) * (PI / 180f).toFloat()
            val sgn = if (rng.nextBoolean()) 1f else -1f
            val end = Offset(
                bp.x + cos(ang.toDouble()).toFloat() * len * sgn,
                bp.y + sin(ang.toDouble()).toFloat() * len * 0.7f,
            )
            drawRealLightning(bp, end, rng, depth + 1, intensity * 0.7f, core, glow, outer)
        }
    }
}

/* ── 3. 晴川早春：樱花瓣 ──────────────────────────────────────────────── */

private fun DrawScope.drawSakura(t: Float, foil: Color) {
    val w = size.width
    val h = size.height
    val pinkLight = Color(0xFFFFE3EE)
    val pinkBase  = Color(0xFFFFB7CE)
    val pinkDeep  = Color(0xFFFF8FA8)

    // ── 春风暖光带：每 9s 一道斜向暖黄光从左下滑到右上 ──
    val windCycle = 9f
    val windPhase = (t % windCycle) / windCycle
    if (windPhase < 0.55f) {
        val pp = windPhase / 0.55f
        val cx = w * (-0.2f + pp * 1.4f)                                // 越过整屏
        val cy = h * (1.05f - pp * 0.6f)
        val warm = Color(0xFFFFDDA8)
        // 淡入淡出
        val band = sin((pp * PI).toFloat())
        val a = (band * 0.18f).coerceIn(0f, 1f)
        drawCircle(warm.copy(alpha = a), w * 0.35f, Offset(cx, cy))
        drawCircle(warm.copy(alpha = a * 0.4f), w * 0.65f, Offset(cx, cy))
    }

    // 远景：30 朵小而淡的樱花，飘落速度慢
    for (i in 0 until 30) {
        val baseX = rand(i, 131) * w
        val cycle = 6f + rand(i, 132) * 5f                                 // 6 ~ 11s 一轮
        val phase = ((t + rand(i, 133) * cycle) % cycle) / cycle
        val py = h * phase - h * 0.05f
        val sway = sin((t * (0.5f + rand(i, 134) * 0.5f) + i * 0.9f).toDouble()).toFloat()
        val px = baseX + sway * 18f
        val rot = (t * (25f + rand(i, 135) * 40f) + i * 47f) % 360f
        val sz = 5f + rand(i, 136) * 4f                                    // 5 ~ 9
        val a = (1f - kotlin.math.abs(phase - 0.5f) * 0.5f).coerceIn(0.35f, 1f) * 0.6f
        rotate(rot, Offset(px, py)) {
            drawSakuraPetal(Offset(px, py), sz,
                pinkLight.copy(alpha = a), pinkBase.copy(alpha = a))
        }
    }

    // 近景：12 朵更大更鲜艳的樱花，飘落更慢、更明显
    for (i in 0 until 12) {
        val baseX = rand(i, 31) * w
        val cycle = 8f + rand(i, 32) * 4f
        val phase = ((t + rand(i, 33) * cycle) % cycle) / cycle
        val py = h * phase - h * 0.05f
        val sway = sin((t * (0.6f + rand(i, 34) * 0.5f) + i * 1.1f).toDouble()).toFloat()
        val px = baseX + sway * 34f
        val rot = (t * (45f + rand(i, 35) * 50f) + i * 53f) % 360f
        val sz = 11f + rand(i, 36) * 8f                                    // 11 ~ 19
        val a = (1f - kotlin.math.abs(phase - 0.5f) * 0.5f).coerceIn(0.5f, 1f) * 0.95f
        rotate(rot, Offset(px, py)) {
            // 加阴影层让花瓣立体
            drawSakuraPetal(Offset(px + 1.5f, py + 1.5f), sz,
                pinkDeep.copy(alpha = a * 0.45f), pinkDeep.copy(alpha = a * 0.45f))
            drawSakuraPetal(Offset(px, py), sz,
                pinkLight.copy(alpha = a), pinkBase.copy(alpha = a))
        }
    }

    // 间或洒落的玫瑰金光斑（与封面 foil 呼应）
    for (i in 0 until 6) {
        val cycle = 4f + rand(i, 91) * 3f
        val phase = ((t + rand(i, 92) * cycle) % cycle) / cycle
        val px = rand(i, 93) * w + sin((t + i).toDouble()).toFloat() * 10f
        val py = h * phase
        val a = (1f - kotlin.math.abs(phase - 0.5f) * 0.6f).coerceAtLeast(0f) * 0.7f
        drawCircle(foil.copy(alpha = a * 0.85f), 2.4f, Offset(px, py))
        drawCircle(foil.copy(alpha = a * 0.35f), 7f, Offset(px, py))
    }

    // ── 触底花瓣溅起：5 个落点周期由小变大、向上微弹后散开 ──
    for (i in 0 until 5) {
        val cycle = 3.5f + rand(i, 161) * 2f
        val phase = ((t + rand(i, 162) * cycle) % cycle) / cycle
        if (phase > 0.45f) continue                                       // 仅前 45% 时间显示
        val pp = phase / 0.45f
        val px = w * (0.1f + rand(i, 163) * 0.8f)
        val py = h * (0.94f - pp * 0.06f)                                 // 微弹起 6%
        val sz = 5f + pp * 7f
        val a = ((1f - pp) * 0.85f).coerceIn(0f, 1f)
        val rot = pp * 90f + i * 37f
        rotate(rot, Offset(px, py)) {
            drawSakuraPetal(Offset(px, py), sz,
                pinkLight.copy(alpha = a), pinkBase.copy(alpha = a))
        }
        // 落地小光斑
        drawCircle(Color(0xFFFFD0E2).copy(alpha = a * 0.5f),
            sz * 1.8f * (0.4f + pp), Offset(px, h * 0.96f))
    }
}

/** 画一片 5 瓣樱花（带花心 + 中央高光，更立体）。中心(0,0) 已被 rotate 平移过。 */
private fun DrawScope.drawSakuraPetal(center: Offset, size: Float, light: Color, base: Color) {
    for (k in 0 until 5) {
        val a = k * 72f * (PI / 180f).toFloat()
        val px = center.x + cos(a.toDouble()).toFloat() * size * 0.55f
        val py = center.y + sin(a.toDouble()).toFloat() * size * 0.55f
        // 花瓣外圈（深粉）
        drawCircle(base, size * 0.58f, Offset(px, py))
        // 花瓣内圈高光（淡粉）
        drawCircle(light, size * 0.40f,
            Offset(px - size * 0.06f, py - size * 0.06f))
    }
    // 花心（深粉小圆）
    drawCircle(base.copy(alpha = (base.alpha * 1.1f).coerceAtMost(1f)),
        size * 0.22f, center)
    // 花蕊（金黄高光）
    drawCircle(Color(0xFFFFE066).copy(alpha = base.alpha * 0.9f),
        size * 0.10f, center)
}

/* ── 4. 赤焰天书：火焰 ────────────────────────────────────────────────── */

private fun DrawScope.drawFire(t: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val baseY = h * 0.94f

    val white = Color(0xFFFFFCE5)
    val core  = Color(0xFFFFE08A)
    val mid   = Color(0xFFFF7A1A)
    val deep  = Color(0xFFB02020)
    val ember = Color(0xFFFFC04D)

    // ── 1. 底部"远景火海"：横贯整个画布的连续暗红渐变带 ──
    //    深色 → 透明，让画布底边自然消融，不再是直角切口
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                deep.copy(alpha = 0.30f),
                mid .copy(alpha = 0.55f),
                deep.copy(alpha = 0.30f),       // 最底部反而压暗（远处"地平线"消失感）
            ),
            startY = h * 0.74f,
            endY   = h,
        ),
        topLeft = Offset(0f, h * 0.74f),
        size = androidx.compose.ui.geometry.Size(w, h * 0.26f),
        blendMode = BlendMode.Plus,
    )

    // ── 2. 单一巨大椭圆基底（覆盖整个底部 30%，blur 极大让边界完全融化）──
    drawOvalGlow(Offset(cx, baseY + 6f),  w * 0.95f, h * 0.16f,
        deep.copy(alpha = 0.55f), blur = 110f)
    drawOvalGlow(Offset(cx, baseY - 2f),  w * 0.78f, h * 0.13f,
        mid .copy(alpha = 0.65f), blur = 70f)
    drawOvalGlow(Offset(cx, baseY - 6f),  w * 0.55f, h * 0.10f,
        mid .copy(alpha = 0.55f), blur = 45f)

    // ── 3. 22 颗随机位置 / 大小的"火苗根"小光斑（替代等距灯泡）──
    //    位置/大小都随机，每颗独立微弱呼吸，组成自然不规则的火势
    val rootN = 22
    for (k in 0 until rootN) {
        val s1 = rand(k, 70)
        val s2 = rand(k, 71)
        val s3 = rand(k, 72)
        val pulse = 0.85f + 0.15f * sin((t * (1.5f + s2 * 2f) + s3 * 6.28f).toDouble()).toFloat()
        // 起点横向：均匀分布到整个画布宽度
        val xK = (s1 - 0.5f) * w * 0.92f + cx
        // 纵向：在底部火带的不同高度（让"火苗根"参差）
        val yK = baseY - h * (0.01f + s2 * 0.10f)
        // 大小：边缘略小，但变化随机
        val xRel = (xK - cx) / w
        val edgeFactor = (1f - kotlin.math.abs(xRel) * 1.3f).coerceIn(0.45f, 1f)
        val sz = (0.7f + s3 * 0.7f) * edgeFactor                          // 0.31..1.4
        drawOvalGlow(Offset(xK, yK + 6f),  w * 0.10f * sz * pulse, h * 0.08f * sz,
            mid .copy(alpha = 0.65f), blur = 28f)
        drawOvalGlow(Offset(xK, yK),       w * 0.06f * sz * pulse, h * 0.05f * sz,
            core.copy(alpha = 0.75f), blur = 14f)
        // 核心白热（只在更大的火苗根上）
        if (sz > 0.75f) {
            drawOvalGlow(Offset(xK, yK - 3f), w * 0.025f * sz * pulse, h * 0.022f * sz,
                white.copy(alpha = 0.55f * (sz - 0.75f) / 0.65f), blur = 7f)
        }
    }

    // ── 3. 200 颗细小粒子均匀分布到整个画布宽度，组成翻滚的火海 ──
    val n = 200
    for (i in 0 until n) {
        val seed  = rand(i, 41)
        val seed2 = rand(i, 42)
        val seed3 = rand(i, 43)
        val cycle = 0.8f + seed * 1.6f
        val phase = ((t + seed2 * 7f) % cycle) / cycle
        val life  = 1f - phase

        // 均匀分布到整个画布宽度（去掉立方衰减）
        val xBase = (seed - 0.5f) * w * 0.95f
        val centerness = 1f - kotlin.math.abs(xBase) / (w * 0.5f + 0.001f)

        // 多频复合湍流
        val turb1 = sin((t * 2.3f + i * 0.71f).toDouble()).toFloat()
        val turb2 = sin((t * 5.7f + i * 1.31f + seed3 * 6.28f).toDouble()).toFloat()
        val turb3 = sin((t * 9.1f + i * 0.43f).toDouble()).toFloat()
        val xTurb = (turb1 * 14f + turb2 * 8f + turb3 * 5f) * phase
        val xN = cx + xBase * (0.7f + life * 0.3f) + xTurb

        // 上升高度：中心烧得高，边缘略矮
        val rise = phase * h * 0.65f * (0.7f + centerness * 0.6f) * (0.85f + seed3 * 0.3f)
        val yN = baseY - rise + turb2 * 3f

        val baseW = (5f + seed * 12f) * (0.7f + centerness * 0.5f)
        val sizeFactor = (1f - phase * 0.30f)
        val ovalW = baseW * sizeFactor
        val ovalH = ovalW * (1.4f + seed * 1.0f)
        val a = life * (0.55f + centerness * 0.35f)
        if (a < 0.05f) continue

        drawOvalGlow(Offset(xN, yN), ovalW * 2.0f, ovalH * 1.4f,
            deep.copy(alpha = a * 0.45f), blur = 16f)
        drawOvalGlow(Offset(xN, yN), ovalW * 1.2f, ovalH * 1.05f,
            mid .copy(alpha = a * 0.75f), blur = 9f)
        drawOvalGlow(Offset(xN, yN), ovalW * 0.65f, ovalH * 0.65f,
            core.copy(alpha = a * 0.95f), blur = 4.5f)
        if (life > 0.7f) {
            val k = (life - 0.7f) / 0.3f
            drawOvalGlow(Offset(xN, yN), ovalW * 0.32f, ovalH * 0.42f,
                white.copy(alpha = k * 0.85f), blur = 2.5f)
        }
    }

    // ── 4. 火星：50 颗扇形 160° 散开 + 起点分散 ──
    val sparkN = 50
    for (i in 0 until sparkN) {
        val seed  = rand(i, 51)
        val seed2 = rand(i, 52)
        val cycle = 0.4f + seed * 0.7f
        val phase = ((t * 1.7f + seed2 * 11f) % cycle) / cycle
        val life  = 1f - phase
        val ang   = (-90f + (seed - 0.5f) * 160f) * (PI / 180f).toFloat()
        val srcX  = cx + (seed2 - 0.5f) * w * 0.7f                        // 起点也分散
        val dist  = phase * h * (0.85f + seed * 0.5f)
        val turb  = sin((t * 8f + i * 1.7f).toDouble()).toFloat() * 8f
        val px = srcX + cos(ang.toDouble()).toFloat() * dist + turb
        val py = baseY + sin(ang.toDouble()).toFloat() * dist
        glowCircle(Offset(px, py), 1.4f + seed * 1.5f, ember.copy(alpha = life * 0.95f), blur = 0f)
        glowCircle(Offset(px, py), 5f + seed * 4f,     mid  .copy(alpha = life * 0.55f), blur = 5f)
    }
}

/** 加色辉光椭圆：纵向拉长的 radialGradient 椭圆（火舌单元）。 */
private fun DrawScope.drawOvalGlow(
    center: Offset, halfW: Float, halfH: Float, color: Color, blur: Float,
) {
    if (color.alpha <= 0f) return
    val totalW = halfW + blur
    val totalH = halfH + blur
    if (totalW <= 0.5f || totalH <= 0.5f) return
    // 用一个能覆盖椭圆的方形 radialGradient，再用 scale 把它压扁成椭圆
    // 简化：直接画 radialGradient 圆，但用 scaleX/Y 实现椭圆形
    scale(
        scaleX = totalW / totalH,
        scaleY = 1f,
        pivot = center,
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color,
                    color.copy(alpha = color.alpha * 0.5f),
                    color.copy(alpha = 0f),
                ),
                center = center,
                radius = totalH,
            ),
            radius = totalH,
            center = center,
            blendMode = BlendMode.Plus,
        )
    }
}

/* ── 5. 青鸾翠竹：竹叶 ────────────────────────────────────────────────── */

private fun DrawScope.drawBambooLeaves(t: Float, cover: Color, foil: Color) {
    val w = size.width
    val h = size.height
    val n = 14
    val leafGreen = Color(0xFF3A7A5C)
    val leafLight = Color(0xFF8FCDA9)
    for (i in 0 until n) {
        val baseX = rand(i, 61) * w
        val phase = (t * (0.12f + rand(i, 62) * 0.18f) + rand(i, 63)) % 1f
        val py = h * phase
        val sway = sin((t * 0.8f + i * 1.1f).toDouble()).toFloat()
        val px = baseX + sway * 32f
        val rot = (t * (45f + rand(i, 64) * 70f) + i * 30f) % 360f
        val sz = 9f + rand(i, 65) * 7f
        val a = (1f - kotlin.math.abs(phase - 0.5f) * 0.5f).coerceIn(0.4f, 1f) * 0.82f
        rotate(rot, Offset(px, py)) {
            // 椭圆叶
            drawCircle(leafGreen.copy(alpha = a), sz * 0.55f, Offset(px - sz * 0.5f, py))
            drawCircle(leafGreen.copy(alpha = a), sz * 0.55f, Offset(px + sz * 0.5f, py))
            drawCircle(leafGreen.copy(alpha = a), sz * 0.55f, Offset(px, py))
            // 叶脉
            drawLine(
                color = leafLight.copy(alpha = a),
                start = Offset(px - sz, py),
                end   = Offset(px + sz, py),
                strokeWidth = 1.2f
            )
        }
    }
    // 偶发烫银光斑
    val gPhase = (t * 0.6f) % 1f
    if (gPhase < 0.25f) {
        val a = (1f - gPhase / 0.25f) * 0.4f
        drawCircle(foil.copy(alpha = a), 5f, Offset(w * 0.5f + cos(t.toDouble()).toFloat() * w * 0.35f, h * 0.2f))
    }
}

/* ── 6. 星河长卷：流星 + 星辰 ──────────────────────────────────────────── */

private fun DrawScope.drawMeteors(t: Float, foil: Color, ribbon: Color) {
    val w = size.width
    val h = size.height

    val starLight = Color(0xFFE8F0FF)
    val milkyWay  = Color(0xFF6FA8FF)
    val nebula    = Color(0xFFA8C5FF)

    // ── 1. 横贯画布的银河带（斜对角柔和渐变，让背景不是纯黑）──
    //    顶部偏右 → 底部偏左方向流淌
    val bandCenterY = h * 0.45f
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                milkyWay.copy(alpha = 0.06f),
                nebula  .copy(alpha = 0.14f),
                milkyWay.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            start = Offset(w * 0.9f, bandCenterY - h * 0.32f),
            end   = Offset(w * 0.1f, bandCenterY + h * 0.32f),
        ),
        blendMode = BlendMode.Plus,
    )

    // ── 2. 远景星尘（40 颗超小，慢呼吸，几乎不动）──
    for (i in 0 until 40) {
        val px = rand(i, 71) * w
        val py = rand(i, 72) * h
        val twinkle = 0.4f + 0.6f * (0.5f + 0.5f * sin((t * 1.4f + i * 0.71f).toDouble()).toFloat())
        val r = 0.6f + rand(i, 73) * 0.9f
        drawCircle(starLight.copy(alpha = (0.55f * twinkle).coerceIn(0f, 1f)),
            r, Offset(px, py))
    }

    // ── 3. 中景星辰（20 颗，带十字光芒）──
    for (i in 0 until 20) {
        val px = rand(i, 81) * w
        val py = rand(i, 82) * h
        val twinkle = 0.5f + 0.5f * sin((t * 2.4f + i * 1.9f).toDouble()).toFloat()
        val r = 1.4f + rand(i, 83) * 1.6f
        val a = (0.85f * twinkle).coerceIn(0f, 1f)
        drawCircle(starLight.copy(alpha = a), r, Offset(px, py))
        // 外晕
        drawCircle(starLight.copy(alpha = (0.18f * twinkle).coerceIn(0f, 1f)),
            r * 4f, Offset(px, py))
        // 十字光芒（仅大颗）
        if (rand(i, 84) > 0.6f) {
            val sparkLen = r * 4.5f
            val sparkA = (0.55f * twinkle).coerceIn(0f, 1f)
            drawLine(starLight.copy(alpha = sparkA),
                Offset(px - sparkLen, py), Offset(px + sparkLen, py),
                strokeWidth = 0.8f)
            drawLine(starLight.copy(alpha = sparkA),
                Offset(px, py - sparkLen), Offset(px, py + sparkLen),
                strokeWidth = 0.8f)
        }
    }

    // ── 4. 偶发"超亮闪耀星"（每隔几秒某颗大星格外亮，模拟超新星脉冲）──
    for (i in 0 until 4) {
        val seed = rand(i, 91)
        val cycle = 4f + seed * 3f
        val phase = ((t + rand(i, 92) * cycle) % cycle) / cycle
        if (phase < 0.20f) {
            val pulse = sin((phase / 0.20f * PI).toDouble()).toFloat()
            val px = rand(i, 93) * w
            val py = rand(i, 94) * h * 0.85f
            val a = (pulse * 0.95f).coerceIn(0f, 1f)
            drawCircle(starLight.copy(alpha = a), 2.5f, Offset(px, py))
            drawCircle(starLight.copy(alpha = (a * 0.45f).coerceIn(0f, 1f)),
                12f, Offset(px, py))
            drawCircle(ribbon.copy(alpha = (a * 0.25f).coerceIn(0f, 1f)),
                26f, Offset(px, py))
        }
    }

    // ── 5. 三道流星不同方向 / 速度，错峰循环 ──
    drawMeteor(t, w, h, ribbon,
        cycle = 2.4f, phaseOffset = 0.0f, dir = 0,    // 右上 → 左下
        tailLen = 95f, headSize = 3.5f, seed = 11)
    drawMeteor(t, w, h, ribbon,
        cycle = 3.1f, phaseOffset = 1.1f, dir = 1,    // 左上 → 右下
        tailLen = 75f, headSize = 2.6f, seed = 22)
    drawMeteor(t, w, h, ribbon,
        cycle = 4.2f, phaseOffset = 2.0f, dir = 2,    // 顶部直下（慢）
        tailLen = 60f, headSize = 2.2f, seed = 33)
}

/** 单道流星（在画布范围内划过，自带尾巴渐隐）。 */
@Suppress("LongParameterList")
private fun DrawScope.drawMeteor(
    t: Float, w: Float, h: Float, color: Color,
    cycle: Float, phaseOffset: Float, dir: Int,
    tailLen: Float, headSize: Float, seed: Int,
) {
    val phase = ((t + phaseOffset) % cycle) / cycle
    if (phase >= 0.7f) return                                            // 30% 时间停飞
    val seedIdx = ((t + phaseOffset) / cycle).toInt() + seed
    val sX: Float; val sY: Float; val eX: Float; val eY: Float
    when (dir) {
        0 -> {                                                            // 右上 → 左下
            sX = w * (0.7f + rand(seedIdx, 1) * 0.35f); sY = -10f
            eX = w * (-0.05f + rand(seedIdx, 2) * 0.3f); eY = h * 0.85f
        }
        1 -> {                                                            // 左上 → 右下
            sX = w * (-0.05f + rand(seedIdx, 3) * 0.25f); sY = -10f
            eX = w * (0.65f + rand(seedIdx, 4) * 0.35f); eY = h * 0.85f
        }
        else -> {                                                         // 顶部直下
            sX = w * (0.2f + rand(seedIdx, 5) * 0.6f); sY = -10f
            eX = sX + (rand(seedIdx, 6) - 0.5f) * w * 0.2f; eY = h * 0.9f
        }
    }
    val pp = (phase / 0.7f).coerceIn(0f, 1f)
    val hx = sX + (eX - sX) * pp
    val hy = sY + (eY - sY) * pp
    val dx = eX - sX; val dy = eY - sY
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val ux = dx / len; val uy = dy / len
    // 尾巴粒子
    for (k in 0 until 18) {
        val kk = k / 18f
        val px = hx - ux * tailLen * kk
        val py = hy - uy * tailLen * kk
        val a = ((1f - kk) * (1f - pp * 0.4f) * 0.85f).coerceIn(0f, 1f)
        drawCircle(color.copy(alpha = a), (headSize - kk * (headSize - 0.5f)).coerceAtLeast(0.4f),
            Offset(px, py))
    }
    // 头部白光 + 蓝光晕
    val headA = ((1f - pp * 0.3f) * 0.95f).coerceIn(0f, 1f)
    drawCircle(Color.White.copy(alpha = headA), headSize * 1.2f, Offset(hx, hy))
    drawCircle(color.copy(alpha = (headA * 0.55f).coerceIn(0f, 1f)),
        headSize * 3.5f, Offset(hx, hy))
}
