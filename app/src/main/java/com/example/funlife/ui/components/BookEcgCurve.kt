// BookEcgCurve.kt — v53 阅读心电图（按 page bucket 渲染）
//
// 输入：EcgPoint 列表（已按 page asc）；权重 0..1。
// 视觉：金色折线 + 高光峰值 + 下方进度刻度。
package com.example.funlife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.repository.EcgPoint
import com.example.funlife.ui.theme.ReadingRoomTheme as RT

@Composable
fun BookEcgCurve(
    points: List<EcgPoint>,
    totalPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(RT.CardCream)) {
        if (points.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📈 还没有心电图\n阅读打卡时填一下当前页码，曲线会自动浮现。",
                    color = RT.MutedInk, fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Box
        }
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Text(
                "📊 阅读心电图",
                color = RT.PrimaryInk, fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width; val h = size.height
                val maxW = points.maxOf { it.weight }.coerceAtLeast(0.0001f)
                // 把 page 映射到 [0, w]
                val pageMax = (totalPages.takeIf { it > 0 } ?: points.maxOf { it.page })
                    .coerceAtLeast(1)
                val pts = points.map { p ->
                    val x = (p.page.toFloat() / pageMax) * w
                    val y = h - (p.weight / maxW) * h * 0.85f
                    Offset(x, y)
                }
                // 下方填充
                val fill = Path().apply {
                    moveTo(pts.first().x, h)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, h); close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        listOf(
                            RT.AccentGold.copy(alpha = 0.55f),
                            RT.AccentGold.copy(alpha = 0.05f),
                        )
                    )
                )
                // 折线
                val line = Path().apply {
                    pts.forEachIndexed { i, p ->
                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                }
                drawPath(line, color = RT.AccentOrange, style = Stroke(width = 3f))
                // 数据点
                pts.forEach { drawCircle(RT.AccentRose, 3.5f, it) }
                // 当前进度竖线
                if (currentPage in 1..pageMax) {
                    val cx = (currentPage.toFloat() / pageMax) * w
                    drawLine(
                        color = RT.AccentSky,
                        start = Offset(cx, 0f), end = Offset(cx, h),
                        strokeWidth = 2f
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Text("第 1 页", color = RT.MutedInk, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                if (totalPages > 0) {
                    Text("共 $totalPages 页 · 当前 $currentPage",
                        color = RT.SecondaryInk, fontSize = 10.sp)
                }
            }
        }
    }
}
