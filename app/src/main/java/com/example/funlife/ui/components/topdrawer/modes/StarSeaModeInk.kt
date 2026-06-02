// ═══════════════════════════════════════════════════════════════════════════
// StarSeaModeInk.kt — 星海 · 宋画水墨皮肤
// 设计：上方夜空淡墨蓝，山下宣纸；远山三层墨色；用户记录的星点用朱砂/赭石/青色
// 不闪烁、不流星，靠"留白 + 星宿点"取胜
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.text.font.FontStyle
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
import kotlin.math.sin

@Composable
fun StarSeaModeInk(userId: Long) {
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

    // 飞鸟 + 雾气动画
    val tr = rememberInfiniteTransition(label = "inkSea")
    val drift by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "d"
    )
    val mistDrift by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(50000, easing = LinearEasing)), label = "m"
    )

    val skyTop = Color(0xFF2A3142)        // 淡墨蓝夜空
    val skyMid = Color(0xFF4A4E5E)
    val paper = Color(0xFFEFE8D2)
    val mountainNear = Color(0xFF1F1D1A).copy(alpha = 0.92f)
    val mountainMid = Color(0xFF3D3A33).copy(alpha = 0.65f)
    val mountainFar = Color(0xFF6E685A).copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to skyTop, 0.55f to skyMid, 0.70f to paper, 1f to paper
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 远处天空背景小星（克制，固定，不闪烁）
            for (i in 0 until 35) {
                val sx = ((i * 9301 + 49297) % 233280) / 233280f
                val sy = ((i * 12289 + 33191) % 233280) / 233280f * 0.55f
                drawCircle(
                    color = Color(0xFFEDE8D8).copy(alpha = 0.35f),
                    radius = 0.9f,
                    center = Offset(w * sx, h * sy)
                )
            }

            // 用户记录的星 —— 限制在天空区域（y < 0.55h）
            moods.forEach { m ->
                val seed = m.id.toLong().absoluteValue + 31
                val rx = ((seed * 9301L + 49297L) % 233280L) / 233280f
                val ry = ((seed * 31337L + 17777L) % 233280L) / 233280f
                val cx = w * (0.05f + rx * 0.90f)
                val cy = h * (0.10f + ry * 0.42f)
                val daysAgo = runCatching {
                    java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(m.date), today).toInt()
                }.getOrDefault(999).coerceAtLeast(0)
                val alpha = when {
                    daysAgo == 0 -> 1f
                    daysAgo <= 7 -> 0.85f
                    daysAgo <= 30 -> 0.65f
                    else -> 0.45f
                }
                // 朱砂 / 赭石 / 黛青：宋人色
                val color = when (m.moodLevel) {
                    5 -> Color(0xFFB23A48)   // 朱砂
                    4 -> Color(0xFFD4A36A)   // 赭石
                    3 -> Color(0xFFEDE8D8)   // 月白
                    2 -> Color(0xFF6F8AA3)   // 黛青
                    else -> Color(0xFF3F4D62) // 深黛
                }
                // 主体：实心小圆
                drawCircle(color = color.copy(alpha = alpha), radius = 2.6f, center = Offset(cx, cy))
                // 极淡光晕（一圈描边，宋画"罩染"感）
                drawCircle(
                    color = color.copy(alpha = alpha * 0.35f),
                    radius = 5f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 0.8f)
                )
            }

            // 一弯月（右上）—— path 减法的真月牙
            val mr = 22f
            val mbx = w * 0.80f
            val mby = h * 0.13f
            val mp = Path().apply { addOval(androidx.compose.ui.geometry.Rect(mbx - mr, mby - mr, mbx + mr, mby + mr)) }
            val mShadow = Path().apply { addOval(androidx.compose.ui.geometry.Rect(mbx - mr + 12f, mby - mr, mbx + mr + 12f, mby + mr)) }
            val crescent = Path().apply { op(mp, mShadow, androidx.compose.ui.graphics.PathOperation.Difference) }
            drawPath(crescent, color = Color(0xFFF7F1DC).copy(alpha = 0.94f))
            drawPath(crescent, color = Color(0xFFEDE8D8).copy(alpha = 0.5f), style = Stroke(width = 0.8f))

            // —— 横向飘雾（山顶上方、夜空与宊纸交界处）
            for (i in 0 until 4) {
                val phase = ((mistDrift + i * 0.27f) % 1f)
                val mx = w * (-0.2f + phase * 1.4f) - w * 0.08f * i
                val my = h * 0.60f + i * 4f
                val mw = 200f + i * 35f
                val mh = 12f + i
                drawOval(
                    color = Color(0xFFEDE8D8).copy(alpha = 0.18f),
                    topLeft = Offset(mx - mw / 2f, my - mh / 2f),
                    size = androidx.compose.ui.geometry.Size(mw, mh)
                )
            }

            // —— 三两只飞鸟剑影（在山顶右侧）
            val flockX = (drift * w * 1.3f) - w * 0.15f
            val flockY = h * 0.42f
            listOf(0f to 0f, -18f to 6f, 18f to 6f).forEachIndexed { i, (ox, oy) ->
                val cx = flockX + ox
                val cy = flockY + oy + sin((drift + i * 0.13f) * 6.28f) * 1.5f
                if (cx in -15f..(w + 15f)) {
                    val span = 6f
                    val dip = 2.4f
                    val p = Path().apply {
                        moveTo(cx - span, cy + dip)
                        quadraticBezierTo(cx - span / 2f, cy - dip, cx, cy + dip)
                        quadraticBezierTo(cx + span / 2f, cy - dip, cx + span, cy + dip)
                    }
                    drawPath(p, color = Color(0xFFEDE8D8).copy(alpha = 0.55f), style = Stroke(width = 1.1f))
                }
            }

            // 远山三层
            fun drawMountain(layer: Int, baseY: Float, amp: Float, color: Color) {
                val path = Path()
                path.moveTo(0f, h)
                path.lineTo(0f, h * baseY)
                val steps = 26
                for (s in 0..steps) {
                    val tx = s.toFloat() / steps
                    val k = sin(tx * 6.28f * (1.5f + layer * 0.6f) + layer * 1.7f)
                    val k2 = sin(tx * 6.28f * (3f + layer) + layer * 0.4f) * 0.4f
                    val y = h * baseY - (k + k2) * amp
                    path.lineTo(w * tx, y)
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path, color = color)
            }
            drawMountain(0, 0.66f, 12f, mountainFar)
            drawMountain(1, 0.74f, 18f, mountainMid)
            drawMountain(2, 0.84f, 22f, mountainNear)

            // 朱砂方印（右下，内嵌十字篆刻感）
            val stampSize = 22f
            val sx = w - 32f - stampSize
            val sy = h - 40f - stampSize
            drawRect(
                color = Color(0xFFB23A48).copy(alpha = 0.82f),
                topLeft = Offset(sx, sy),
                size = androidx.compose.ui.geometry.Size(stampSize, stampSize)
            )
            drawLine(
                color = Color(0xFFEFE8D2),
                start = Offset(sx + stampSize / 2f, sy + 4f),
                end = Offset(sx + stampSize / 2f, sy + stampSize - 4f),
                strokeWidth = 2.2f
            )
            drawLine(
                color = Color(0xFFEFE8D2),
                start = Offset(sx + 4f, sy + stampSize / 2f),
                end = Offset(sx + stampSize - 4f, sy + stampSize / 2f),
                strokeWidth = 2.2f
            )
        }

        // —— 顶部 HUD（毛笔字感）
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                "心  情  星  海",
                fontSize = TextSize.title,
                fontWeight = FontWeight.Black,
                color = Color(0xFFF1ECDB),
                letterSpacing = 6.rsp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${moods.size} 颗星  ·  此刻无声",
                fontSize = TextSize.tiny,
                color = Color(0xFFF1ECDB).copy(alpha = 0.65f),
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.rsp
            )
        }

        // —— 底部题款（在宊纸区域）
        if (moods.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Spacing.lg, bottom = Spacing.lg + 8.dp)
            ) {
                Text(
                    "今夜新星 ${moods.count { runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) }.getOrDefault(999L) == 0L }} 颗",
                    fontSize = TextSize.tiny,
                    color = Color(0xFF3A352D).copy(alpha = 0.75f),
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 3.rsp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "总览 ${moods.size} 颗  ·  本周 ${moods.count { runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) }.getOrDefault(999L) in 0L..6L }} 颗",
                    fontSize = TextSize.tiny,
                    color = Color(0xFF3A352D).copy(alpha = 0.50f),
                    letterSpacing = 2.rsp
                )
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
                    "夜  阑  无  星",
                    fontSize = TextSize.headline,
                    color = Color(0xFFF1ECDB),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.rsp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "记一笔心情  即得一星",
                    fontSize = TextSize.sm,
                    color = Color(0xFFF1ECDB).copy(alpha = 0.65f),
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 4.rsp
                )
            }
        }
    }
}
