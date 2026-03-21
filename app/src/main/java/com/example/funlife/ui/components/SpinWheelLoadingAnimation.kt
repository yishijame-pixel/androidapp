// SpinWheelLoadingAnimation.kt - 转盘页面加载动画（优化版）
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpinWheelLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // 转盘旋转 - 更快更流畅
    val wheelRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheelRotation"
    )
    
    // 缩放呼吸效果
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // 进度点
    val dotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotProgress"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF9C4),
                        Color(0xFFFFE082),
                        Color(0xFFFFCA28)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // 简化的转盘动画
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = 70f
                    
                    // 旋转的转盘 - 简化为3个扇形
                    rotate(wheelRotation, pivot = Offset(centerX, centerY)) {
                        val colors = listOf(
                            Color(0xFFFF6B9D),
                            Color(0xFF4FC3F7),
                            Color(0xFFFFD54F)
                        )
                        
                        for (i in 0..2) {
                            val startAngle = i * 120f - 90f
                            
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        colors[i],
                                        colors[i].copy(alpha = 0.7f)
                                    ),
                                    center = Offset(centerX, centerY),
                                    radius = radius
                                ),
                                startAngle = startAngle,
                                sweepAngle = 120f,
                                useCenter = true,
                                topLeft = Offset(centerX - radius, centerY - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                            )
                        }
                        
                        // 转盘边框
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                        
                        // 中心圆
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFFE082)
                                )
                            ),
                            radius = 18f,
                            center = Offset(centerX, centerY)
                        )
                    }
                    
                    // 固定的指针
                    val pointerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerX, centerY - radius - 30f)
                        lineTo(centerX - 12f, centerY - radius - 5f)
                        lineTo(centerX + 12f, centerY - radius - 5f)
                        close()
                    }
                    
                    drawPath(
                        path = pointerPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF6B9D),
                                Color(0xFFFF1744)
                            )
                        )
                    )
                    
                    // 简化的星星 - 只有4颗
                    for (i in 0..3) {
                        val angle = (i * 90f + wheelRotation * 0.3f) * Math.PI / 180
                        val distance = 100f
                        val x = centerX + (distance * cos(angle)).toFloat()
                        val y = centerY + (distance * sin(angle)).toFloat()
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color.Transparent
                                ),
                                radius = 8f
                            ),
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
            
            // 加载文字
            Text(
                text = "🎉",
                fontSize = 40.sp
            )
            
            Text(
                text = "正在准备转盘",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            
            // 动态点点点
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 0..2) {
                    val delay = i * 0.33f
                    val dotAlpha = if (dotProgress >= delay && dotProgress < delay + 0.33f) {
                        1f
                    } else {
                        0.3f
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                Color(0xFFFFB74D).copy(alpha = dotAlpha),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}
