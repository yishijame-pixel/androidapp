// ═══════════════════════════════════════════════════════════════════════════
// WindowModeMinimal.kt — 窗 · 极简几何皮肤
// 设计：纯色双段渐变 + 一个圆（日/月）+ 两条横线代表云 + 底部小字时间
// 整屏极致克制：每个像素都精雕细琢，没有任何装饰
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rsp
import java.time.LocalTime

@Composable
fun WindowModeMinimal() {
    val now = remember { LocalTime.now() }
    val hourF = now.hour + now.minute / 60f
    val isNight = hourF < 6f || hourF >= 18f
    val isDusk = hourF in 17f..19.5f

    // 极简调色：每个时段两个颜色（顶/底），冷暖对比
    val (top, bottom, fg) = when {
        isNight -> Triple(Color(0xFF0F1118), Color(0xFF1F2230), Color.White)
        isDusk -> Triple(Color(0xFFF4A37C), Color(0xFFE36B5C), Color.White)
        hourF < 11f -> Triple(Color(0xFFFFE9C7), Color(0xFFFFC58A), Color(0xFF3D2A1F))
        else -> Triple(Color(0xFFB7DBF5), Color(0xFFE8F2FA), Color(0xFF2E3D52))
    }

    val timeStr = remember { "%02d : %02d".format(now.hour, now.minute) }
    val today = remember { java.time.LocalDate.now() }
    val dateStr = remember(today) { today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy . MM . dd")) }
    val weekStr = remember(today) {
        when (today.dayOfWeek.value) { 1 -> "MON"; 2 -> "TUE"; 3 -> "WED"; 4 -> "THU"; 5 -> "FRI"; 6 -> "SAT"; else -> "SUN" }
    }

    // 呼吸动画：太阳/月亮轻轻脚动
    val tr = rememberInfiniteTransition(label = "minBreath")
    val breath by tr.animateFloat(
        0.97f, 1.03f, infiniteRepeatable(tween(4200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "b"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // 黄金分割布局：圆在左 0.30 / 高 0.30，水平线在 0.618 高处
            val cx = w * 0.30f
            val cy = h * 0.30f
            val r = 40f * breath

            if (isNight) {
                // 月：纯色圆 + 偏移背景色圆 = 月牙
                drawCircle(color = fg.copy(alpha = 0.95f), radius = r, center = Offset(cx, cy))
                drawCircle(color = top, radius = r * 0.88f, center = Offset(cx + r * 0.40f, cy - r * 0.04f))
                // 月旁 3 颗极小淡星
                listOf(
                    Triple(cx + r * 1.6f, cy - r * 0.4f, 1.4f),
                    Triple(cx + r * 2.1f, cy + r * 0.3f, 0.9f),
                    Triple(cx - r * 1.4f, cy - r * 0.7f, 1.1f)
                ).forEach { (sx, sy, sr) ->
                    drawCircle(color = fg.copy(alpha = 0.70f), radius = sr, center = Offset(sx, sy))
                }
            } else {
                // 日：实心圆 + 辐射 8 根极细光线（逆时针从正上方）
                drawCircle(color = fg.copy(alpha = 0.95f), radius = r, center = Offset(cx, cy))
                drawCircle(
                    color = fg.copy(alpha = 0.25f),
                    radius = r + 10f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 0.6f)
                )
                // 辐射光线
                val rayInner = r + 18f
                val rayOuter = r + 30f
                for (i in 0 until 8) {
                    val angle = i * (Math.PI.toFloat() * 2f / 8f) - Math.PI.toFloat() / 2f
                    val x1 = cx + cos(angle) * rayInner
                    val y1 = cy + sin(angle) * rayInner
                    val x2 = cx + cos(angle) * rayOuter
                    val y2 = cy + sin(angle) * rayOuter
                    drawLine(
                        color = fg.copy(alpha = 0.30f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 1.0f
                    )
                }
            }

            // 两条横线代表云 / 地平线（黄金分割位置）
            val lineY1 = h * 0.58f
            val lineY2 = h * 0.63f
            drawLine(
                color = fg.copy(alpha = 0.38f),
                start = Offset(w * 0.08f, lineY1),
                end = Offset(w * 0.55f, lineY1),
                strokeWidth = 1.0f
            )
            drawLine(
                color = fg.copy(alpha = 0.20f),
                start = Offset(w * 0.50f, lineY2),
                end = Offset(w * 0.92f, lineY2),
                strokeWidth = 1.0f
            )

            // 主水平线（黄金分割 0.618）
            drawLine(
                color = fg.copy(alpha = 0.45f),
                start = Offset(w * 0.05f, h * 0.618f),
                end = Offset(w * 0.95f, h * 0.618f),
                strokeWidth = 0.8f
            )
            // 底部竖线点缀（让纯几何不会过于冷）
            for (i in 0 until 3) {
                val x = w * (0.20f + i * 0.30f)
                drawLine(
                    color = fg.copy(alpha = 0.22f),
                    start = Offset(x, h * 0.83f),
                    end = Offset(x, h * 0.88f),
                    strokeWidth = 0.8f
                )
            }
        }

        // —— 顶部：周几 + 日期（极细字体 + 大字距）
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.lg)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = weekStr,
                fontSize = TextSize.tiny,
                color = fg.copy(alpha = 0.65f),
                fontWeight = FontWeight.Light,
                letterSpacing = 6.rsp
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = dateStr,
                fontSize = TextSize.tiny,
                color = fg.copy(alpha = 0.65f),
                fontWeight = FontWeight.Light,
                letterSpacing = 4.rsp
            )
        }

        // —— 中央：大字时间 + 下方一句极简注脚
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                fontSize = (TextSize.title.value * 1.4f).rsp,
                color = fg,
                fontWeight = FontWeight.ExtraLight,
                textAlign = TextAlign.Center,
                letterSpacing = 12.rsp
            )
            Spacer(Modifier.height(20.dp))
            // 一根极短水平装饰线
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(0.6.dp)
                    .background(fg.copy(alpha = 0.55f))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (isNight) "NIGHT" else if (isDusk) "DUSK" else if (hourF < 11f) "MORNING" else "DAY",
                fontSize = TextSize.tiny,
                color = fg.copy(alpha = 0.55f),
                letterSpacing = 8.rsp,
                fontWeight = FontWeight.Light
            )
        }

        // —— 底部：极细序号 / 哲思位
        Text(
            text = "No. " + now.hour.toString().padStart(2, '0') + now.minute.toString().padStart(2, '0'),
            fontSize = TextSize.tiny,
            color = fg.copy(alpha = 0.40f),
            letterSpacing = 4.rsp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.lg)
        )
    }
}
