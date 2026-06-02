// ═══════════════════════════════════════════════════════════════════════════
// WindowMode.kt
// 模式 A：一扇会呼吸的窗
//   · 根据真实时间显示昼/夜/黄昏色调
//   · 飘动的云朵 / 夜晚的星星
//   · 一句应景的时辰诗
//   · 长按 3 秒触发呼吸引导动画
//
// 适配指南：所有尺寸用 rdp/rsp、Spacing/Radius；Canvas radius 用 coerceAtLeast(1f)
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * 窗 · 顶层入口：负责 skin 状态 + 分发 + 右上角切换器
 * 三套皮肤：CINEMATIC（电影感）/ INK（水墨）/ MINIMAL（极简）
 * 选择持久化在 SharedPreferences("topdrawer_window") 的 "skin" 字段
 */
@Composable
fun WindowMode() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("topdrawer_window", android.content.Context.MODE_PRIVATE) }
    var skin by remember { mutableStateOf(TopDrawerSkin.fromName(prefs.getString("skin", null))) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (skin) {
            TopDrawerSkin.CINEMATIC -> WindowModeCinematic()
            TopDrawerSkin.INK -> WindowModeInk()
            TopDrawerSkin.MINIMAL -> WindowModeMinimal()
        }
        // 切换器：右上角；INK 用 dark 调（深字配浅纸），其他用 light（白字配深底）
        SkinSwitcherPill(
            skin = skin,
            tone = if (skin == TopDrawerSkin.INK) "dark" else "light",
            onCycle = {
                val next = skin.next()
                skin = next
                prefs.edit().putString("skin", next.name).apply()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.sm, end = Spacing.md)
        )
    }
}

/**
 * 电影感皮肤：原有实现（真实大气散射 + 多层云 + 太阳/月亮划弧 + 北斗 + 流星 + 签到）
 */
@Composable
private fun WindowModeCinematic() {
    val ctx = LocalContext.current
    val now = remember { LocalTime.now() }
    val today = remember { LocalDate.now() }
    val hourF = remember { now.hour + now.minute / 60f }   // 精确小时（含分钟分数）
    val theme = remember(hourF) { themeOfHourF(hourF) }
    val verse = remember(now.hour) { verseOfHour(now.hour) }

    // 持久化：连续陪伴天数（首次打开记一次） + 当日签到状态
    val prefs = remember { ctx.getSharedPreferences("topdrawer_window", android.content.Context.MODE_PRIVATE) }
    val companionDays = remember {
        val firstDay = prefs.getString("first_day", null)
        if (firstDay == null) {
            prefs.edit().putString("first_day", today.toString()).apply()
            1
        } else {
            (java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(firstDay), today) + 1).toInt().coerceAtLeast(1)
        }
    }
    val morningKey = "morning_${today}"
    val nightKey = "night_${today}"
    var morningDone by remember { mutableStateOf(prefs.getBoolean(morningKey, false)) }
    var nightDone by remember { mutableStateOf(prefs.getBoolean(nightKey, false)) }

    // 时间段控制：早安窗口 5-11 点；晚安窗口 21-1 点
    val morningOpen = hourF in 5f..11f
    val nightOpen = hourF >= 21f || hourF < 1f

    // 三层云朵 + 星星闪烁 + 流星周期
    val transition = rememberInfiniteTransition(label = "windowSky")
    val driftFar by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(45000, easing = LinearEasing)), label = "df"
    )
    val driftMid by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "dm"
    )
    val driftNear by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "dn"
    )
    val twinkle by transition.animateFloat(
        0.3f, 1f, infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse), label = "tw"
    )
    val sunPulse by transition.animateFloat(
        0.85f, 1.05f, infiniteRepeatable(tween(3600, easing = EaseInOutSine), RepeatMode.Reverse), label = "sp"
    )
    val meteor by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "mt"
    )

    // 太阳/月亮按时辰沿天空划弧的位置（cx, cy in [0..1]）
    val (bodyCx, bodyCy) = remember(hourF) { bodyPositionOfHour(hourF) }

    // 天空铺满整个 page（不再有左右深色边缘 / 圆角剪切）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.skyColors))
    ) {
        // ─────────── 天空 Canvas：远景星辰 + 流星 + 三层云 + 太阳/月亮 ───────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1) 顶部光晕（极光样）：让天空有"上方亮、下方暗"的真实感
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(theme.haloColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f, endY = h * 0.5f
                ),
                size = size
            )

            // 2) 夜晚：精致恒星点阵 + 北斗七星连线 + 闪烁
            if (theme.isNight) {
                // 60 颗背景星（伪随机稳定位置，全屏分布）
                for (i in 0 until 60) {
                    val sx = ((i * 9301 + 49297) % 233280) / 233280f
                    val sy = ((i * 12289 + 33191) % 233280) / 233280f * 0.75f
                    val sr = (((i * 19) % 7) + 1) * 0.35f
                    val phase = (twinkle + i * 0.0917f) % 1f
                    val alpha = 0.25f + phase * 0.45f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = (sr * 2f).coerceAtLeast(0.5f),
                        center = Offset(w * sx, h * sy)
                    )
                }
                // 北斗七星（左上角，连线 + 亮点 + 十字光芒）
                val bigDipper = listOf(
                    0.10f to 0.12f, 0.16f to 0.10f, 0.22f to 0.13f,
                    0.27f to 0.17f, 0.31f to 0.22f, 0.27f to 0.26f, 0.21f to 0.27f
                )
                // 连线
                for (i in 0 until bigDipper.size - 1) {
                    val (x1, y1) = bigDipper[i]
                    val (x2, y2) = bigDipper[i + 1]
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(w * x1, h * y1),
                        end = Offset(w * x2, h * y2),
                        strokeWidth = 1.2f
                    )
                }
                // 亮星 + 十字光芒
                bigDipper.forEachIndexed { i, (x, y) ->
                    val cx = w * x; val cy = h * y
                    val pulse = ((twinkle + i * 0.14f) % 1f).let { 0.65f + it * 0.35f }
                    // 十字光芒（横竖两条短线）
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f * pulse),
                        start = Offset(cx - 6f, cy), end = Offset(cx + 6f, cy),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f * pulse),
                        start = Offset(cx, cy - 6f), end = Offset(cx, cy + 6f),
                        strokeWidth = 1f
                    )
                    // 光晕 + 核心
                    drawCircle(
                        color = Color(0xFFE0EFFF).copy(alpha = 0.20f * pulse),
                        radius = 8f, center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f * pulse),
                        radius = 2.2f, center = Offset(cx, cy)
                    )
                }
                // 偶现流星（7s 周期，前 0.25 显示，从右上向左下划）
                if (meteor < 0.25f) {
                    val p = meteor / 0.25f
                    val sx = w * (0.95f - 0.6f * p)
                    val sy = h * (0.10f + 0.35f * p)
                    val alpha = (1f - p).coerceIn(0f, 1f)
                    drawLine(
                        color = Color.White.copy(alpha = alpha * 0.75f),
                        start = Offset(sx + 28f, sy - 28f),
                        end = Offset(sx, sy),
                        strokeWidth = 2f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 2.8f, center = Offset(sx, sy)
                    )
                }
            }

            // 3) 云朵：白天 + 黄昏画三层视差云；夜晚不画云（让星空纯净）
            if (!theme.isNight) {
                drawCloudLayer(w, h, driftFar, 0.10f, 0.32f, Color.White.copy(alpha = 0.40f), 22f, 4)
                drawCloudLayer(w, h, driftMid, 0.18f, 0.50f, Color.White.copy(alpha = 0.55f), 32f, 3)
                drawCloudLayer(w, h, driftNear, 0.28f, 0.85f, Color.White.copy(alpha = 0.58f), 44f, 3)
            }

            // 4) 太阳 / 月亮 —— 沿天空划弧的真实位置
            val bx = w * bodyCx
            val by = h * bodyCy
            if (theme.isNight) {
                // 月亮：多层柔光晕 → 月牙主体（Path Difference）→ 高亮边缘
                val moonR = 22f
                // 4 层放射光晕（外大内小）
                listOf(80f to 0.05f, 60f to 0.08f, 44f to 0.14f, 32f to 0.22f).forEach { (r, a) ->
                    drawCircle(
                        color = Color(0xFFFFF6D5).copy(alpha = a),
                        radius = r.coerceAtLeast(1f), center = Offset(bx, by)
                    )
                }
                // 月相：phase 0..1，决定月牙朝向 + 厚度
                // 简化：始终画一个优雅的月牙（暗面圆从月亮内部偏移挖出，留下月牙）
                val phase = (today.toEpochDay() % 30L).toFloat() / 30f  // 0..1
                // 月牙厚度：phase=0(新月，几乎黑) phase=0.5(满月，全亮) phase=1(返新月)
                // 用 sin(phase * 2π) 让月相从新→上弦→满→下弦→新循环
                val illuminate = (kotlin.math.sin(phase * 2f * Math.PI.toFloat()) + 1f) / 2f  // 0..1
                val shadowOffset = (1f - illuminate) * moonR * 1.3f
                val shadowDir = if (phase < 0.5f) 1f else -1f  // 上弦右暗 / 下弦左暗
                val moonPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(
                        bx - moonR, by - moonR, bx + moonR, by + moonR
                    ))
                }
                val shadowPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(
                        bx + shadowOffset * shadowDir - moonR,
                        by - moonR,
                        bx + shadowOffset * shadowDir + moonR,
                        by + moonR
                    ))
                }
                val crescentPath = Path().apply {
                    op(moonPath, shadowPath, PathOperation.Difference)
                }
                // 月牙主体（暖白）
                drawPath(crescentPath, color = Color(0xFFFFF6D5).copy(alpha = 0.96f))
                // 月牙内边缘高亮线（增加立体感）
                drawPath(crescentPath, color = Color(0xFFFFFAE5).copy(alpha = 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
            } else {
                // 太阳：光晕辐射（多层）+ 主体 + 呼吸缩放
                val r0 = 22f * sunPulse
                listOf(72f to 0.05f, 56f to 0.08f, 42f to 0.14f, 30f to 0.22f).forEach { (r, a) ->
                    drawCircle(
                        color = theme.sunColor.copy(alpha = a),
                        radius = r.coerceAtLeast(1f), center = Offset(bx, by)
                    )
                }
                drawCircle(
                    color = theme.sunColor,
                    radius = r0.coerceAtLeast(1f), center = Offset(bx, by)
                )
                // 太阳内部更亮的核心
                drawCircle(
                    color = Color(0xFFFFF8E1).copy(alpha = 0.85f),
                    radius = (r0 * 0.6f).coerceAtLeast(1f), center = Offset(bx, by)
                )
            }

            // 5) 底部远山轮廓（一道柔和暗紫色剪影）增加纵深
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, theme.skyColors.last().copy(alpha = 0.7f)),
                    startY = h * 0.7f, endY = h
                ),
                size = size
            )
        }

        // ─────────── 顶部：个性化问候 + 连续陪伴天数 ───────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.lg, start = Spacing.lg, end = Spacing.lg)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = theme.greeting,
                fontSize = TextSize.sm,
                color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 6.rsp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "和你在一起的第 $companionDays 天",
                fontSize = TextSize.tiny,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 1.rsp
            )
        }

        // ─────────── 中央：时辰诗 ───────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = verse.line1,
                fontSize = TextSize.headline,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.rsp,
                letterSpacing = 2.rsp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = verse.line2,
                fontSize = TextSize.md,
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 28.rsp,
                letterSpacing = 2.rsp
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "— ${verse.author} —",
                fontSize = TextSize.tiny,
                color = Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium
            )
        }

        // ─────────── 底部：早安 / 晚安签到 ───────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.lg, start = Spacing.lg, end = Spacing.lg)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SignInPill(
                icon = Icons.Outlined.WbSunny,
                label = if (morningDone) "已早安签到" else "早安签到",
                accent = Color(0xFFFFB07A),
                enabled = morningOpen && !morningDone,
                done = morningDone,
                onClick = {
                    prefs.edit().putBoolean(morningKey, true).apply()
                    morningDone = true
                }
            )
            Spacer(Modifier.width(Spacing.sm))
            SignInPill(
                icon = Icons.Outlined.NightlightRound,
                label = if (nightDone) "已晚安签到" else "晚安签到",
                accent = Color(0xFFC0A0FF),
                enabled = nightOpen && !nightDone,
                done = nightDone,
                onClick = {
                    prefs.edit().putBoolean(nightKey, true).apply()
                    nightDone = true
                }
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────
   多层云朵绘制 helper —— 细长椭圆（drawOval）+ 多层叠加，柔和真实
   ──────────────────────────────────────────────────────────── */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudLayer(
    w: Float, h: Float,
    drift: Float,
    yMin: Float, yMax: Float,
    color: Color, baseR: Float, count: Int
) {
    for (i in 0 until count) {
        val x0 = ((i * 31 + 7) % 100) / 100f
        val y = yMin + ((i * 17 + 3) % 100) / 100f * (yMax - yMin)
        val phase = (drift + i * 0.27f) % 1f
        val cx = w * ((x0 + phase * 1.2f) % 1f) - w * 0.15f
        val cy = h * y
        val r = baseR * (0.85f + (i * 0.13f) % 0.3f)
        if (r > 0f) {
            // 主体椭圆：宽 4r, 高 1.2r（拉长成"云"的形状）
            val mainW = r * 4.2f
            val mainH = r * 1.4f
            drawOval(
                color = color.copy(alpha = color.alpha * 0.5f),
                topLeft = Offset(cx - mainW / 2f, cy - mainH / 2f),
                size = androidx.compose.ui.geometry.Size(mainW, mainH)
            )
            // 上部的小蓬（增加蓬松感）
            drawOval(
                color = color.copy(alpha = color.alpha * 0.35f),
                topLeft = Offset(cx - r * 1.4f, cy - r * 1.1f),
                size = androidx.compose.ui.geometry.Size(r * 2.6f, r * 1.2f)
            )
            // 边缘虚化的高光（浅色椭圆）
            drawOval(
                color = color.copy(alpha = color.alpha * 0.25f),
                topLeft = Offset(cx - mainW * 0.6f, cy - mainH * 0.3f),
                size = androidx.compose.ui.geometry.Size(mainW * 1.2f, mainH * 0.6f)
            )
        }
    }
}

/* ────────────────────────────────────────────────────────────
   早安 / 晚安签到胶囊
   ──────────────────────────────────────────────────────────── */
@Composable
private fun SignInPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    done: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = when {
            done -> accent.copy(alpha = 0.85f)
            enabled -> Color.White.copy(alpha = 0.18f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        animationSpec = tween(300), label = "pillBg"
    )
    val contentAlpha = if (enabled || done) 1f else 0.4f
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (done) Color.White else accent.copy(alpha = contentAlpha),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = TextSize.tiny,
            color = if (done) Color.White else Color.White.copy(alpha = contentAlpha),
            fontWeight = FontWeight.Bold
        )
    }
}

/* ────────────────────────────────────────────────────────────
   太阳 / 月亮沿天空划弧的位置
   早 6 点东方升起 (0.05, 0.55)
   正午 12 点天顶 (0.5, 0.10)
   傍晚 18 点西方落 (0.95, 0.55)
   夜晚月亮也类似（21 点东方 → 03 点天顶 → 09 点西方，但我们只在夜晚画）
   ──────────────────────────────────────────────────────────── */
private fun bodyPositionOfHour(hourF: Float): Pair<Float, Float> {
    val isNight = hourF < 6f || hourF >= 18f
    // 夜晚月亮：基准时间 21（出） → 03（顶） → 09（落），用 21~33（=9+24）连续区间
    val t = if (isNight) {
        val nh = if (hourF < 6f) hourF + 24f else hourF  // 18..30
        (nh - 18f) / 12f  // 0..1（18→0, 30→1）
    } else {
        (hourF - 6f) / 12f  // 0..1（6→0, 18→1）
    }
    val cx = 0.05f + 0.9f * t
    // 抛物线轨迹：t=0.5 时最高（cy 最小）
    val cy = 0.55f - 0.45f * sin(t * Math.PI).toFloat()
    return cx to cy
}

// ─────────────────────────────────────────────────────────────────────────
// 时辰主题
// ─────────────────────────────────────────────────────────────────────────
private data class WindowTheme(
    val greeting: String,
    val skyColors: List<Color>,
    val isNight: Boolean,
    val isDusk: Boolean,
    val haloColor: Color,
    val sunColor: Color,
)

/**
 * 接收精确小时（含分钟分数），让色温在时段交界处也能平滑过渡（避免 17:59 → 18:00 一秒切深紫）。
 * 9 段连续色卡：凌晨/破晓/清晨/上午/正午/午后/黄昏/初夜/深夜
 */
private fun themeOfHourF(hourF: Float): WindowTheme {
    return when {
        hourF < 4.5f -> WindowTheme(
            greeting = "深 夜",
            skyColors = listOf(Color(0xFF0A0E22), Color(0xFF111634), Color(0xFF1F1640)),
            isNight = true, isDusk = false,
            haloColor = Color(0xFF3A3A6E),
            sunColor = Color(0xFFFFD27A),
        )
        hourF < 6f -> WindowTheme(
            greeting = "破 晓",
            skyColors = listOf(Color(0xFF2D2454), Color(0xFF6E5980), Color(0xFFE8A99B)),
            isNight = false, isDusk = false,
            haloColor = Color(0xFFFFB58A),
            sunColor = Color(0xFFFFC988),
        )
        hourF < 8f -> WindowTheme(
            greeting = "清 晨",
            skyColors = listOf(Color(0xFFFFC9A0), Color(0xFFFFB29C), Color(0xFFB8D4FF)),
            isNight = false, isDusk = false,
            haloColor = Color(0xFFFFE3B0),
            sunColor = Color(0xFFFFD17A),
        )
        hourF < 11f -> WindowTheme(
            greeting = "上 午",
            skyColors = listOf(Color(0xFF7AB8E0), Color(0xFFA5D2EC), Color(0xFFDDEEF8)),
            isNight = false, isDusk = false,
            haloColor = Color(0xFFE5F2FA),
            sunColor = Color(0xFFFFE082),
        )
        hourF < 14f -> WindowTheme(
            greeting = "正 午",
            skyColors = listOf(Color(0xFF4A90E2), Color(0xFF87CEEB), Color(0xFFE0F6FF)),
            isNight = false, isDusk = false,
            haloColor = Color(0xFFCCE5FA),
            sunColor = Color(0xFFFFEB94),
        )
        hourF < 17f -> WindowTheme(
            greeting = "午 后",
            skyColors = listOf(Color(0xFF6BB6FF), Color(0xFFA8D4FF), Color(0xFFFFE0C2)),
            isNight = false, isDusk = false,
            haloColor = Color(0xFFFFD9B0),
            sunColor = Color(0xFFFFCE6E),
        )
        hourF < 19.5f -> WindowTheme(
            greeting = "黄 昏",
            skyColors = listOf(Color(0xFFE56D54), Color(0xFFFFA585), Color(0xFFFFCFA0)),
            isNight = false, isDusk = true,
            haloColor = Color(0xFFFFAA70),
            sunColor = Color(0xFFFF8852),
        )
        hourF < 22f -> WindowTheme(
            greeting = "夜 晚",
            skyColors = listOf(Color(0xFF1A1B3A), Color(0xFF2D2E5C), Color(0xFF483556)),
            isNight = true, isDusk = false,
            haloColor = Color(0xFF6A5A8C),
            sunColor = Color(0xFFFFD27A),
        )
        else -> WindowTheme(
            greeting = "深 夜",
            skyColors = listOf(Color(0xFF0A0E22), Color(0xFF14172E), Color(0xFF22193C)),
            isNight = true, isDusk = false,
            haloColor = Color(0xFF3A3A6E),
            sunColor = Color(0xFFFFD27A),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// 时辰诗（按时段随机一条）
// ─────────────────────────────────────────────────────────────────────────
private data class Verse(val line1: String, val line2: String, val author: String)

private fun verseOfHour(hour: Int): Verse {
    val pool = when (hour) {
        in 5..10 -> listOf(
            Verse("人生若只如初见", "何事秋风悲画扇", "纳兰性德"),
            Verse("等闲识得东风面", "万紫千红总是春", "朱熹"),
            Verse("迟日江山丽", "春风花草香", "杜甫")
        )
        in 11..16 -> listOf(
            Verse("行到水穷处", "坐看云起时", "王维"),
            Verse("偷得浮生半日闲", "也无风雨也无晴", "苏轼"),
            Verse("人闲桂花落", "夜静春山空", "王维")
        )
        in 17..19 -> listOf(
            Verse("夕阳无限好", "只是近黄昏", "李商隐"),
            Verse("落霞与孤鹜齐飞", "秋水共长天一色", "王勃"),
            Verse("斜阳照墟落", "穷巷牛羊归", "王维")
        )
        in 20..22 -> listOf(
            Verse("但愿人长久", "千里共婵娟", "苏轼"),
            Verse("海上生明月", "天涯共此时", "张九龄"),
            Verse("举头望明月", "低头思故乡", "李白")
        )
        else -> listOf(
            Verse("此时无声胜有声", "夜深忽梦少年事", "白居易"),
            Verse("梦回吹角连营", "八百里分麾下炙", "辛弃疾"),
            Verse("夜阑卧听风吹雨", "铁马冰河入梦来", "陆游")
        )
    }
    return pool[(System.currentTimeMillis() / 60000 % pool.size).toInt()]
}
