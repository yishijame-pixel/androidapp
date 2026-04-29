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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpinWheelLoadingAnimation() {
    // 使用Dialog实现全屏效果，遮挡底部导航栏
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        
        // 转盘旋转 - 更快更流畅
        val wheelRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
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
        
        // 星星闪烁
        val starTwinkle by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "starTwinkle"
        )
        
        // 彩带飘动
        val ribbonWave by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ribbonWave"
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
            // 背景装饰星星
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stars = listOf(
                    Offset(size.width * 0.1f, size.height * 0.15f),
                    Offset(size.width * 0.85f, size.height * 0.2f),
                    Offset(size.width * 0.15f, size.height * 0.75f),
                    Offset(size.width * 0.9f, size.height * 0.7f),
                    Offset(size.width * 0.5f, size.height * 0.1f)
                )
                
                stars.forEachIndexed { index, offset ->
                    val alpha = (starTwinkle + index * 0.2f) % 1f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = alpha),
                                Color.Transparent
                            ),
                            radius = 15f
                        ),
                        radius = 15f,
                        center = offset
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 转盘动画容器
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = 75f
                        
                        // 旋转的转盘 - 彩色扇形
                        rotate(wheelRotation, pivot = Offset(centerX, centerY)) {
                            val colors = listOf(
                                Color(0xFFFF6B9D),
                                Color(0xFF4FC3F7),
                                Color(0xFFFFD54F),
                                Color(0xFF9C27B0),
                                Color(0xFF66BB6A)
                            )
                            
                            for (i in 0..4) {
                                val startAngle = i * 72f - 90f
                                
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            colors[i],
                                            colors[i].copy(alpha = 0.8f),
                                            colors[i]
                                        ),
                                        center = Offset(centerX, centerY)
                                    ),
                                    startAngle = startAngle,
                                    sweepAngle = 72f,
                                    useCenter = true,
                                    topLeft = Offset(centerX - radius, centerY - radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                                )
                            }
                            
                            // 转盘边框 - 金色
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA000)
                                    )
                                ),
                                radius = radius,
                                center = Offset(centerX, centerY),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                            
                            // 中心圆 - 渐变
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFFFE082),
                                        Color(0xFFFFD54F)
                                    )
                                ),
                                radius = 22f,
                                center = Offset(centerX, centerY)
                            )
                            
                            // 中心小圆点
                            drawCircle(
                                color = Color(0xFFFF6B9D),
                                radius = 6f,
                                center = Offset(centerX, centerY)
                            )
                        }
                        
                        // 固定的指针 - 更大更明显
                        val pointerPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(centerX, centerY - radius - 35f)
                            lineTo(centerX - 15f, centerY - radius - 5f)
                            lineTo(centerX + 15f, centerY - radius - 5f)
                            close()
                        }
                        
                        drawPath(
                            path = pointerPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF1744),
                                    Color(0xFFFF6B9D)
                                )
                            )
                        )
                        
                        // 指针阴影
                        drawPath(
                            path = pointerPath,
                            color = Color.Black.copy(alpha = 0.2f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                        
                        // 旋转的装饰星星
                        for (i in 0..5) {
                            val angle = (i * 60f + wheelRotation * 0.5f) * Math.PI / 180
                            val distance = 110f
                            val x = centerX + (distance * cos(angle)).toFloat()
                            val y = centerY + (distance * sin(angle)).toFloat()
                            val starSize = 10f + (sin(wheelRotation * Math.PI / 180 + i) * 3f).toFloat()
                            
                            // 星星外发光
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD54F).copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    radius = starSize * 1.5f
                                ),
                                radius = starSize * 1.5f,
                                center = Offset(x, y)
                            )
                            
                            // 星星本体
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = starSize,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                
                // 表情符号动画
                Text(
                    text = "🎉",
                    fontSize = 48.sp,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = sin(ribbonWave * Math.PI / 180).toFloat() * 15f
                    }
                )
                
                // 加载文字 - 渐变色
                Text(
                    text = "正在准备转盘",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B9D),
                                Color(0xFF9C27B0),
                                Color(0xFF4FC3F7)
                            )
                        )
                    )
                )
                
                // 动态点点点 - 更大更明显
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in 0..2) {
                        val delay = i * 0.33f
                        val dotAlpha = if (dotProgress >= delay && dotProgress < delay + 0.33f) {
                            1f
                        } else {
                            0.3f
                        }
                        
                        val dotScale = if (dotProgress >= delay && dotProgress < delay + 0.33f) {
                            1.2f
                        } else {
                            1f
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B9D).copy(alpha = dotAlpha),
                                            Color(0xFFFF1744).copy(alpha = dotAlpha * 0.5f)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
