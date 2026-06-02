// MonthlyMinutesChart.kt — v53 阅光书房 · 月度阅读时长曲线
//
// 输入：DailyMinutes 列表（按 dateYmd 升序），自动归一化到 0..1。
// 渲染：纯 Canvas 折线 + 渐变填充 + 高亮今日点。
package com.example.funlife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import com.example.funlife.data.dao.DailyMinutes
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlyMinutesChart(
    data: List<DailyMinutes>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("暂无数据 · 完成第一次打卡后这里会亮起",
                color = RT.MutedInk, fontSize = 12.sp)
        }
        return
    }

    val maxV = remember(data) { (data.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1) }
    val sumV = remember(data) { data.sumOf { it.minutes } }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize().padding(top = 18.dp, bottom = 8.dp)) {
            val w = size.width; val h = size.height
            val n = data.size
            val stepX = if (n > 1) w / (n - 1) else 0f
            val pts = data.mapIndexed { i, d ->
                val x = i * stepX
                val y = h - (d.minutes.toFloat() / maxV) * h * 0.85f
                Offset(x, y)
            }

            // 网格基线
            val gridStroke = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
            drawLine(
                color = RT.MutedInk.copy(alpha = 0.4f),
                start = Offset(0f, h - 1f),
                end = Offset(w, h - 1f),
                strokeWidth = 1f
            )
            drawLine(
                color = RT.MutedInk.copy(alpha = 0.25f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1f,
                pathEffect = gridStroke.pathEffect
            )

            // 渐变填充
            val fillPath = Path().apply {
                moveTo(0f, h)
                pts.forEach { lineTo(it.x, it.y) }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(
                        RT.AccentOrange.copy(alpha = 0.45f),
                        RT.AccentOrange.copy(alpha = 0.05f)
                    )
                )
            )
            // 折线
            val linePath = Path().apply {
                pts.forEachIndexed { i, p ->
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
            }
            drawPath(
                path = linePath,
                color = RT.AccentRose,
                style = Stroke(width = 3f)
            )
            // 数据点 + 高亮最末一个
            pts.forEachIndexed { i, p ->
                val r = if (i == pts.lastIndex) 6f else 3.5f
                drawCircle(color = RT.AccentRose, radius = r, center = p)
                if (i == pts.lastIndex) {
                    drawCircle(
                        color = RT.AccentRose.copy(alpha = 0.25f),
                        radius = 12f, center = p
                    )
                }
            }
        }
        // 顶部统计信息
        Text(
            "总计 $sumV 分钟 · 最高 $maxV / 天",
            color = RT.SecondaryInk, fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
    }
}
