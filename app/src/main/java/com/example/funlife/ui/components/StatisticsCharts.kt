// StatisticsCharts.kt - 统计图表组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 饼图组件 - 显示选项分布
 */
@Composable
fun PieChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier,
    colors: List<Color> = defaultPieColors
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("暂无数据")
        return
    }
    
    val total = data.values.sum()
    var animationProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(1000, easing = FastOutSlowInEasing)) { value, _ ->
            animationProgress = value
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 饼图
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            
            var startAngle = -90f
            
            data.entries.forEachIndexed { index, (option, count) ->
                val sweepAngle = (count.toFloat() / total * 360f) * animationProgress
                val color = colors[index % colors.size]
                
                // 绘制扇形
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                
                // 绘制边框
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f)
                )
                
                startAngle += sweepAngle
            }
            
            // 中心白色圆
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
        }
        
        // 图例
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.entries.forEachIndexed { index, (option, count) ->
                val percentage = (count.toFloat() / total * 100).toInt()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    colors[index % colors.size],
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Text(
                        "$count 次 ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 折线图组件 - 显示趋势
 */
@Composable
fun LineChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("暂无数据")
        return
    }
    
    var animationProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(1000, easing = FastOutSlowInEasing)) { value, _ ->
            animationProgress = value
        }
    }
    
    val maxValue = data.maxOfOrNull { it.second } ?: 1
    val minValue = data.minOfOrNull { it.second } ?: 0
    val range = maxValue - minValue
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 折线图
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1).coerceAtLeast(1)
            
            // 绘制网格线
            for (i in 0..4) {
                val y = height * i / 4
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            // 绘制折线
            val path = Path()
            data.forEachIndexed { index, (_, value) ->
                val x = index * spacing
                val y = height - ((value - minValue).toFloat() / range.coerceAtLeast(1) * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x * animationProgress, y)
                }
            }
            
            // 绘制线条
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f)
            )
            
            // 绘制数据点
            data.forEachIndexed { index, (_, value) ->
                val x = index * spacing * animationProgress
                val y = height - ((value - minValue).toFloat() / range.coerceAtLeast(1) * height)
                
                // 外圈
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 8f,
                    center = Offset(x, y)
                )
                
                // 内圈
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
        }
        
        // X轴标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 柱状图组件 - 显示时段分析
 */
@Composable
fun BarChart(
    data: Map<Int, Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder("暂无数据")
        return
    }
    
    var animationProgress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(1000, easing = FastOutSlowInEasing)) { value, _ ->
            animationProgress = value
        }
    }
    
    val maxValue = data.values.maxOrNull() ?: 1f
    val minValue = data.values.minOrNull() ?: 0f
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 柱状图
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / data.size * 0.7f
            val spacing = width / data.size
            
            data.entries.forEachIndexed { index, (hour, value) ->
                val barHeight = ((value - minValue) / (maxValue - minValue).coerceAtLeast(0.1f) * height) * animationProgress
                val x = index * spacing + spacing / 2 - barWidth / 2
                val y = height - barHeight
                
                // 绘制柱子
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            barColor,
                            barColor.copy(alpha = 0.7f)
                        ),
                        startY = y,
                        endY = height
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
                
                // 绘制边框
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    style = Stroke(width = 2f)
                )
            }
        }
        
        // X轴标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.keys.sorted().forEach { hour ->
                Text(
                    "${hour}时",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 空状态占位符
 */
@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📊", fontSize = 48.sp)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 默认饼图颜色
private val defaultPieColors = listOf(
    Color(0xFFFF6B9D),
    Color(0xFF4FACFE),
    Color(0xFFFFA726),
    Color(0xFF66BB6A),
    Color(0xFFAB47BC),
    Color(0xFFFFCA28),
    Color(0xFF26C6DA),
    Color(0xFFEF5350),
    Color(0xFF5C6BC0),
    Color(0xFF78909C)
)
