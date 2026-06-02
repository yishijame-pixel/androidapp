// ═══════════════════════════════════════════════════════════════════════════
// StarSeaMode.kt
// 模式 D：心情星海
//   · 用户所有 MoodEntry → 一颗会闪烁的星
//   · 越近期的越亮，越旧的越暗
//   · 颜色按 mood 等级（5=粉、3=青、1=蓝）
//   · 流星 = 今天最新一条
//   · Canvas 高性能渲染，所有 radius 用 coerceAtLeast(1f)
//
// 后续可扩展：HabitRecord 也作为星点 / 双指缩放时间穿梭 / 点击跳转记录详情
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.repository.MoodRepository
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.absoluteValue

/**
 * 星海 · 顶层入口：skin 分发 + 右上角切换器
 * CINEMATIC（电影感粒子流星）/ INK（宋画山水星河）/ MINIMAL（黑底银河带）
 */
@Composable
fun StarSeaMode(userId: Long, onPickEntry: (MoodEntry) -> Unit = {}) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("topdrawer_starsea", android.content.Context.MODE_PRIVATE) }
    var skin by remember { mutableStateOf(TopDrawerSkin.fromName(prefs.getString("skin", null))) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (skin) {
            TopDrawerSkin.CINEMATIC -> StarSeaModeCinematic(userId, onPickEntry)
            TopDrawerSkin.INK -> StarSeaModeInk(userId)
            TopDrawerSkin.MINIMAL -> StarSeaModeMinimal(userId)
        }
        SkinSwitcherPill(
            skin = skin,
            tone = "light",
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
 * 电影感皮肤：原有实现（深紫渐变 + 闪烁星 + 流星 + 用户星点彩色光晕）
 */
@Composable
private fun StarSeaModeCinematic(userId: Long, onPickEntry: (MoodEntry) -> Unit) {
    val ctx = LocalContext.current
    var moods by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId <= 0L) { loaded = true; return@LaunchedEffect }
        runCatching {
            val repo = MoodRepository(AppDatabase.getDatabase(ctx).moodDao())
            moods = repo.getAllMoods(userId).first()
        }
        loaded = true
    }

    // 把每条 mood 映射成一颗星：位置由 id 决定（伪随机），亮度由日期距离今天的天数决定
    val today = remember { LocalDate.now() }
    val stars = remember(moods) { moods.map { it.toStar(today) } }

    // 全局闪烁相位
    val transition = rememberInfiniteTransition(label = "starSea")
    val twinkle by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    val meteorPhase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "meteor"
    )

    // 外层填满整个 page（消除 Pager 切换缝隙），卡片视觉用内层 padding 实现
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080A24), Color(0xFF14163A), Color(0xFF2A1A3E))
                )
            )
    ) {
        // ── 星空画布 ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 星云光斑（两块）：紫色/青色柔光，为深紫背景添加宇宙感的体积光
            drawCircle(
                color = Color(0xFF7A4FB8).copy(alpha = 0.18f),
                radius = w * 0.45f,
                center = Offset(w * 0.22f, h * 0.35f)
            )
            drawCircle(
                color = Color(0xFF4FA3E0).copy(alpha = 0.12f),
                radius = w * 0.40f,
                center = Offset(w * 0.80f, h * 0.65f)
            )

            // 背景：远处的小星（固定噪点感）
            for (i in 0 until 40) {
                val seed = i * 73 + 11
                val x = ((seed * 9301 + 49297) % 233280) / 233280f
                val y = ((seed * 31337 + 17777) % 233280) / 233280f
                val phase = (twinkle + i * 0.07f) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f + phase * 0.25f),
                    radius = 1.2f.coerceAtLeast(1f),
                    center = Offset(w * x, h * y)
                )
            }

            // 用户记录的星星
            stars.forEachIndexed { idx, s ->
                val cx = w * s.xRatio
                val cy = h * s.yRatio
                val phase = (twinkle + idx * 0.083f) % 1f
                val finalAlpha = (s.brightness * (0.55f + phase * 0.45f)).coerceIn(0.05f, 1f)
                // 光晕
                drawCircle(
                    color = s.color.copy(alpha = (finalAlpha * 0.25f).coerceAtMost(0.6f)),
                    radius = (s.radius * 3f).coerceAtLeast(1f),
                    center = Offset(cx, cy)
                )
                // 星点
                drawCircle(
                    color = s.color.copy(alpha = finalAlpha),
                    radius = s.radius.coerceAtLeast(1f),
                    center = Offset(cx, cy)
                )
            }

            // 流星：今日最新一条
            val latestToday = stars.firstOrNull { it.daysAgo == 0 }
            if (latestToday != null) {
                val mx = w * (0.1f + meteorPhase * 0.85f)
                val my = h * (0.15f + meteorPhase * 0.55f)
                // 流星尾巴
                for (i in 0 until 8) {
                    val tailPhase = (1f - i / 8f)
                    drawCircle(
                        color = Color.White.copy(alpha = tailPhase * 0.6f),
                        radius = (3f - i * 0.3f).coerceAtLeast(1f),
                        center = Offset(mx - i * 12f, my - i * 8f)
                    )
                }
                drawCircle(
                    color = Color.White,
                    radius = 3f.coerceAtLeast(1f),
                    center = Offset(mx, my)
                )
            }
        }

        // ── 顶部 HUD ──
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                "心  情  星  海",
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 6.rsp
            )
            Spacer(Modifier.height(4.dp))
            val todayCount = stars.count { it.daysAgo == 0 }
            val weekCount = stars.count { it.daysAgo in 0..6 }
            Text(
                "今日 $todayCount  ·  本周 $weekCount  ·  总计 ${stars.size}",
                fontSize = TextSize.tiny,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 2.rsp
            )
        }

        // ── 空态 ──
        if (loaded && moods.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✨", fontSize = 42.rsp)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "夜空还没有星",
                    fontSize = TextSize.md,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "去记一条心情，就会有第一颗星亮起",
                    fontSize = TextSize.tiny,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // ── 底部图例 ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.sm)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(Color(0xFFFF6F91))
            Text("开心", fontSize = TextSize.tiny, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            LegendDot(Color(0xFF26A69A))
            Text("平静", fontSize = TextSize.tiny, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            LegendDot(Color(0xFF42A5F5))
            Text("低落", fontSize = TextSize.tiny, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.rdp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
    )
    Spacer(Modifier.width(3.dp))
}

// ─────────────────────────────────────────────────────────────────────────
// 数据映射：MoodEntry → 星
// ─────────────────────────────────────────────────────────────────────────
private data class Star(
    val xRatio: Float,   // 0..1
    val yRatio: Float,   // 0..1（上 20% 留给 HUD）
    val brightness: Float, // 0..1
    val color: Color,
    val radius: Float,    // px
    val daysAgo: Int
)

private fun MoodEntry.toStar(today: LocalDate): Star {
    // 1) 位置：根据 id 伪随机
    val seed = id.toLong().absoluteValue + 31
    val x = ((seed * 9301L + 49297L) % 233280L) / 233280f
    val y = ((seed * 31337L + 17777L) % 233280L) / 233280f
    // y 限制在 0.22..0.85 区间（让位顶部 HUD 和底部图例）
    val yClamped = 0.22f + y * 0.63f

    // 2) 亮度：日期距离今天越近越亮
    val daysAgo = runCatching {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(date), today).toInt()
    }.getOrDefault(999).coerceAtLeast(0)
    val brightness = when {
        daysAgo == 0 -> 1f
        daysAgo <= 7 -> 0.85f
        daysAgo <= 30 -> 0.6f
        daysAgo <= 90 -> 0.4f
        else -> 0.25f
    }

    // 3) 颜色：按等级映射（参考 MoodIconStore 默认调色）
    val color = when (moodLevel) {
        5 -> Color(0xFFFF6F91)
        4 -> Color(0xFFFFB74D)
        3 -> Color(0xFF26A69A)
        2 -> Color(0xFF7986CB)
        else -> Color(0xFF42A5F5)
    }

    // 4) 半径：等级越高越大（2.5..4.5 px）
    val radius = 2.5f + moodLevel * 0.4f

    return Star(x, yClamped, brightness, color, radius, daysAgo)
}
