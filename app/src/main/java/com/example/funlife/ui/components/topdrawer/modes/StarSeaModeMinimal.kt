// ═══════════════════════════════════════════════════════════════════════════
// StarSeaModeMinimal.kt — 星海 · 极简几何皮肤
// 设计：纯黑底 + 散点小白星（不闪烁）+ 一条贝塞尔曲线代表银河 + 用户的星按 mood 染色
// 无光晕、无流星、无 HUD 装饰
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.repository.MoodRepository
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rsp
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import kotlin.math.absoluteValue

@Composable
fun StarSeaModeMinimal(userId: Long) {
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
    val today = remember { LocalDate.now() }

    // 动画：银河呼吸 + 用户星微闪
    val tr = rememberInfiniteTransition(label = "minSea")
    val galaxyBreath by tr.animateFloat(
        0.7f, 1.0f, infiniteRepeatable(tween(5600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gb"
    )
    val twinkle by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "tw"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF000000), Color(0xFF050608), Color(0xFF000000))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // —— 1) 散点星（小白点，固定不闪，密度低）
            for (i in 0 until 80) {
                val sx = ((i * 9301 + 49297) % 233280) / 233280f
                val sy = ((i * 12289 + 33191) % 233280) / 233280f
                val r = ((i * 19) % 7).let { if (it < 5) 0.6f else 1.0f }
                val a = if (r > 0.8f) 0.65f else 0.32f
                drawCircle(
                    color = Color.White.copy(alpha = a),
                    radius = r,
                    center = Offset(w * sx, h * sy)
                )
            }

            // —— 2) 银河：一条优雅的贝塞尔曲线，多层发光带呼吸动画
            val galaxy = Path().apply {
                moveTo(0f, h * 0.32f)
                cubicTo(
                    w * 0.28f, h * 0.08f,
                    w * 0.72f, h * 0.58f,
                    w, h * 0.36f
                )
            }
            // 多次描线营造发光带（外很淡、中间发光、中心亮；呼吸使辉晕强度变化）
            drawPath(galaxy, color = Color.White.copy(alpha = 0.04f * galaxyBreath), style = Stroke(width = 80f))
            drawPath(galaxy, color = Color.White.copy(alpha = 0.07f * galaxyBreath), style = Stroke(width = 44f))
            drawPath(galaxy, color = Color.White.copy(alpha = 0.14f * galaxyBreath), style = Stroke(width = 20f))
            drawPath(galaxy, color = Color.White.copy(alpha = 0.55f), style = Stroke(width = 1.0f))

            // —— 3) 用户的星：稍大、按 mood 着色，但克制（不光晕）
            moods.forEach { m ->
                val seed = m.id.toLong().absoluteValue + 31
                val rx = ((seed * 9301L + 49297L) % 233280L) / 233280f
                val ry = ((seed * 31337L + 17777L) % 233280L) / 233280f
                val cx = w * (0.06f + rx * 0.88f)
                val cy = h * (0.18f + ry * 0.66f)
                val daysAgo = runCatching {
                    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(m.date), today).toInt()
                }.getOrDefault(999).coerceAtLeast(0)
                val alpha = when {
                    daysAgo == 0 -> 1f
                    daysAgo <= 7 -> 0.85f
                    daysAgo <= 30 -> 0.6f
                    else -> 0.4f
                }
                val color = when (m.moodLevel) {
                    5 -> Color(0xFFFF6F91)
                    4 -> Color(0xFFFFB74D)
                    3 -> Color(0xFFEDEDED)
                    2 -> Color(0xFF7986CB)
                    else -> Color(0xFF42A5F5)
                }
                // 微闪：每颗星相位不同，营造不同步的呼吸感
                val tw = ((twinkle + m.id * 0.013f) % 1f).let { 0.65f + it * 0.35f }
                // 极淡外圈（让亮星不会"决不闪"异常）
                drawCircle(color = color.copy(alpha = alpha * 0.20f), radius = 4.5f, center = Offset(cx, cy))
                drawCircle(color = color.copy(alpha = alpha * tw), radius = 2.6f, center = Offset(cx, cy))
            }

            // 一条极细水平装饰线（区隔 HUD 与重心）
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(w * 0.08f, h * 0.78f),
                end = Offset(w * 0.92f, h * 0.78f),
                strokeWidth = 0.4f
            )
        }

        // —— 极简 HUD：左上角标题 + 大数字（不抢戏）
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                "STAR  SEA",
                fontSize = TextSize.tiny,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.65f),
                letterSpacing = 8.rsp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${moods.size}",
                fontSize = (TextSize.title.value * 1.3f).rsp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = 1.rsp
            )
            Text(
                "STARS",
                fontSize = TextSize.tiny,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.40f),
                letterSpacing = 6.rsp
            )
        }

        // —— 底部：今日/本周 细粒度统计
        if (moods.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.lg),
                horizontalArrangement = Arrangement.Center
            ) {
                val todayCount = moods.count { runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) }.getOrDefault(999L) == 0L }
                val weekCount = moods.count { runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) }.getOrDefault(999L) in 0L..6L }
                MinimalStat("TODAY", todayCount.toString())
                Spacer(Modifier.width(36.dp))
                MinimalStat("WEEK", weekCount.toString())
                Spacer(Modifier.width(36.dp))
                MinimalStat("TOTAL", moods.size.toString())
            }
        }

        // 空态
        if (loaded && moods.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.lg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "·",
                    fontSize = (TextSize.title.value * 2f).rsp,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "EMPTY",
                    fontSize = TextSize.sm,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 8.rsp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun MinimalStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = (TextSize.sm.value * 1.6f).rsp,
            color = Color.White,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.rsp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = TextSize.tiny,
            color = Color.White.copy(alpha = 0.45f),
            fontWeight = FontWeight.Light,
            letterSpacing = 6.rsp
        )
    }
}
