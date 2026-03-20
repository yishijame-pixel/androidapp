package com.example.funlife.ui.screens

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // 启动动画
    LaunchedEffect(Unit) {
        delay(3000) // 3秒后跳转
        onTimeout()
    }
    
    // 动画值
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    // 小狗整体旋转
    val dogRotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dogRotation"
    )
    
    // 小狗上下跳动
    val dogOffsetY by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dogOffsetY"
    )
    
    // 小狗缩放动画
    val dogScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dogScale"
    )
    
    // 左耳朵摇摆
    val leftEarRotation by infiniteTransition.animateFloat(
        initialValue = -45f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leftEar"
    )
    
    // 右耳朵摇摆
    val rightEarRotation by infiniteTransition.animateFloat(
        initialValue = 45f,
        targetValue = -45f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rightEar"
    )
    
    // 眼睛眨眼
    val eyeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye"
    )
    
    // 项圈旋转
    val collarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "collar"
    )
    
    // 背景旋转
    val bgRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgRotation"
    )
    
    // 粒子动画
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle"
    )
    
    // 品牌文字缩放
    val brandScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brand"
    )
    
    // 彩虹色变化
    val colorPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFE082),
                        Color(0xFFFFB74D)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 旋转的背景装饰
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2
            
            rotate(bgRotation, pivot = Offset(centerX, centerY)) {
                // 旋转的大圆圈
                for (i in 0..5) {
                    val angle = (i * 60f + bgRotation) * Math.PI / 180
                    val radius = 200f
                    val x = centerX + (radius * cos(angle)).toFloat()
                    val y = centerY + (radius * sin(angle)).toFloat()
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x60FFB74D),
                                Color(0x20FFB74D),
                                Color(0x00FFB74D)
                            )
                        ),
                        radius = 80f,
                        center = Offset(x, y)
                    )
                }
            }
            
            // 飞舞的粒子
            for (i in 0..20) {
                val angle = (i * 18f) * Math.PI / 180
                val distance = particleOffset + i * 20f
                val x = centerX + (distance * cos(angle)).toFloat()
                val y = centerY + (distance * sin(angle)).toFloat()
                
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 1f - particleOffset / 100f),
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 超萌小狗图标 - 现代卡通风格
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 应用旋转和位移
                    rotate(dogRotation, pivot = Offset(centerX, centerY)) {
                        translate(0f, dogOffsetY) {
                            scale(dogScale, pivot = Offset(centerX, centerY)) {
                                // 外层彩虹光晕
                                for (i in 0..3) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.hsv((colorPhase + i * 30f) % 360f, 0.6f, 1f).copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        ),
                                        radius = 130f - i * 15f,
                                        center = Offset(centerX, centerY)
                                    )
                                }
                                
                                // 身体 - 可爱的圆形身体
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFF9C4),
                                            Color(0xFFFFE082),
                                            Color(0xFFFFD54F)
                                        ),
                                        center = Offset(centerX - 15f, centerY + 50f)
                                    ),
                                    radius = 55f,
                                    center = Offset(centerX, centerY + 50f)
                                )
                                
                                // 身体边框
                                drawCircle(
                                    color = Color(0xFFFFB74D),
                                    radius = 56f,
                                    center = Offset(centerX, centerY + 50f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )
                                
                                // 头部主体 - 超大圆润头部
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFFDE7),
                                            Color(0xFFFFF9C4),
                                            Color(0xFFFFE082)
                                        ),
                                        center = Offset(centerX - 25f, centerY - 25f)
                                    ),
                                    radius = 90f,
                                    center = Offset(centerX, centerY)
                                )
                                
                                // 头部边框
                                drawCircle(
                                    color = Color(0xFFFFB74D),
                                    radius = 92f,
                                    center = Offset(centerX, centerY),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                )
                                
                                // 左耳朵 - 超级可爱的垂耳
                                rotate(leftEarRotation, pivot = Offset(centerX - 65f, centerY - 50f)) {
                                    // 耳朵外层 - 椭圆形
                                    drawOval(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFB74D),
                                                Color(0xFFFF8A65)
                                            ),
                                            center = Offset(centerX - 65f, centerY - 35f)
                                        ),
                                        topLeft = Offset(centerX - 80f, centerY - 60f),
                                        size = androidx.compose.ui.geometry.Size(30f, 50f)
                                    )
                                    // 耳朵内层
                                    drawOval(
                                        color = Color(0xFFFF6B9D),
                                        topLeft = Offset(centerX - 75f, centerY - 55f),
                                        size = androidx.compose.ui.geometry.Size(20f, 40f)
                                    )
                                }
                                
                                // 右耳朵 - 超级可爱的垂耳
                                rotate(rightEarRotation, pivot = Offset(centerX + 65f, centerY - 50f)) {
                                    // 耳朵外层 - 椭圆形
                                    drawOval(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFB74D),
                                                Color(0xFFFF8A65)
                                            ),
                                            center = Offset(centerX + 65f, centerY - 35f)
                                        ),
                                        topLeft = Offset(centerX + 50f, centerY - 60f),
                                        size = androidx.compose.ui.geometry.Size(30f, 50f)
                                    )
                                    // 耳朵内层
                                    drawOval(
                                        color = Color(0xFFFF6B9D),
                                        topLeft = Offset(centerX + 55f, centerY - 55f),
                                        size = androidx.compose.ui.geometry.Size(20f, 40f)
                                    )
                                }
                                
                                // 左眼 - 超萌大眼睛（卡通风格）
                                scale(1f, eyeScale, pivot = Offset(centerX - 32f, centerY - 20f)) {
                                    // 眼白
                                    drawOval(
                                        color = Color.White,
                                        topLeft = Offset(centerX - 50f, centerY - 35f),
                                        size = androidx.compose.ui.geometry.Size(36f, 40f)
                                    )
                                    // 眼珠 - 大而圆
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF424242),
                                                Color(0xFF212121)
                                            )
                                        ),
                                        radius = 13f,
                                        center = Offset(centerX - 32f, centerY - 15f)
                                    )
                                    // 超大高光1
                                    drawCircle(
                                        color = Color.White,
                                        radius = 7f,
                                        center = Offset(centerX - 38f, centerY - 22f)
                                    )
                                    // 高光2
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        radius = 3f,
                                        center = Offset(centerX - 28f, centerY - 12f)
                                    )
                                }
                                
                                // 右眼 - 超萌大眼睛（卡通风格）
                                scale(1f, eyeScale, pivot = Offset(centerX + 32f, centerY - 20f)) {
                                    // 眼白
                                    drawOval(
                                        color = Color.White,
                                        topLeft = Offset(centerX + 14f, centerY - 35f),
                                        size = androidx.compose.ui.geometry.Size(36f, 40f)
                                    )
                                    // 眼珠 - 大而圆
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF424242),
                                                Color(0xFF212121)
                                            )
                                        ),
                                        radius = 13f,
                                        center = Offset(centerX + 32f, centerY - 15f)
                                    )
                                    // 超大高光1
                                    drawCircle(
                                        color = Color.White,
                                        radius = 7f,
                                        center = Offset(centerX + 26f, centerY - 22f)
                                    )
                                    // 高光2
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        radius = 3f,
                                        center = Offset(centerX + 36f, centerY - 12f)
                                    )
                                }
                                
                                // 鼻子 - 超大可爱的圆形鼻子
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF424242),
                                            Color(0xFF212121)
                                        ),
                                        center = Offset(centerX - 5f, centerY + 15f)
                                    ),
                                    radius = 12f,
                                    center = Offset(centerX, centerY + 20f)
                                )
                                
                                // 鼻子超大高光
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.8f),
                                    radius = 6f,
                                    center = Offset(centerX - 4f, centerY + 16f)
                                )
                                
                                // 嘴巴 - W 形可爱嘴巴
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 32f)
                                        quadraticBezierTo(
                                            centerX - 15f, centerY + 45f,
                                            centerX - 30f, centerY + 38f
                                        )
                                    },
                                    color = Color(0xFF424242),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 5f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 32f)
                                        quadraticBezierTo(
                                            centerX + 15f, centerY + 45f,
                                            centerX + 30f, centerY + 38f
                                        )
                                    },
                                    color = Color(0xFF424242),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 5f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                
                                // 舌头 - 超可爱的小舌头
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 38f)
                                        quadraticBezierTo(
                                            centerX - 8f, centerY + 44f,
                                            centerX, centerY + 50f
                                        )
                                        quadraticBezierTo(
                                            centerX + 8f, centerY + 44f,
                                            centerX, centerY + 38f
                                        )
                                        close()
                                    },
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B9D),
                                            Color(0xFFFF1744)
                                        )
                                    )
                                )
                                
                                // 腮红 - 超大超萌
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF8A65).copy(alpha = 0.7f),
                                            Color(0xFFFF8A65).copy(alpha = 0.3f),
                                            Color(0x00FF8A65)
                                        )
                                    ),
                                    topLeft = Offset(centerX - 85f, centerY + 5f),
                                    size = androidx.compose.ui.geometry.Size(30f, 25f)
                                )
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF8A65).copy(alpha = 0.7f),
                                            Color(0xFFFF8A65).copy(alpha = 0.3f),
                                            Color(0x00FF8A65)
                                        )
                                    ),
                                    topLeft = Offset(centerX + 55f, centerY + 5f),
                                    size = androidx.compose.ui.geometry.Size(30f, 25f)
                                )
                                
                                // 头顶呆毛 - 三根可爱的毛发
                                for (i in -1..1) {
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(centerX + i * 12f, centerY - 90f)
                                            quadraticBezierTo(
                                                centerX + i * 15f, centerY - 80f,
                                                centerX + i * 10f, centerY - 70f
                                            )
                                        },
                                        color = Color(0xFFFFB74D),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 7f,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                                
                                // 小爪子 - 左
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFE082),
                                            Color(0xFFFFB74D)
                                        )
                                    ),
                                    radius = 18f,
                                    center = Offset(centerX - 60f, centerY + 70f)
                                )
                                // 爪子肉垫
                                for (i in 0..2) {
                                    drawCircle(
                                        color = Color(0xFFFF6B9D).copy(alpha = 0.6f),
                                        radius = 3f,
                                        center = Offset(centerX - 60f + (i - 1) * 6f, centerY + 68f)
                                    )
                                }
                                
                                // 小爪子 - 右
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFE082),
                                            Color(0xFFFFB74D)
                                        )
                                    ),
                                    radius = 18f,
                                    center = Offset(centerX + 60f, centerY + 70f)
                                )
                                // 爪子肉垫
                                for (i in 0..2) {
                                    drawCircle(
                                        color = Color(0xFFFF6B9D).copy(alpha = 0.6f),
                                        radius = 3f,
                                        center = Offset(centerX + 60f + (i - 1) * 6f, centerY + 68f)
                                    )
                                }
                                
                                // 项圈 - 带旋转的闪亮项圈
                                rotate(collarRotation, pivot = Offset(centerX, centerY + 85f)) {
                                    // 项圈主体
                                    drawPath(
                                        path = Path().apply {
                                            addArc(
                                                oval = androidx.compose.ui.geometry.Rect(
                                                    centerX - 75f,
                                                    centerY + 55f,
                                                    centerX + 75f,
                                                    centerY + 115f
                                                ),
                                                startAngleDegrees = 200f,
                                                sweepAngleDegrees = 140f
                                            )
                                        },
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFF5252),
                                                Color(0xFFFF1744),
                                                Color(0xFFFF5252)
                                            )
                                        ),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 10f,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                    
                                    // 项圈装饰 - 超大闪亮的星星
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(centerX, centerY + 78f)
                                            lineTo(centerX - 8f, centerY + 86f)
                                            lineTo(centerX, centerY + 94f)
                                            lineTo(centerX + 8f, centerY + 86f)
                                            close()
                                        },
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA000)
                                            )
                                        )
                                    )
                                    
                                    // 星星中心
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4f,
                                        center = Offset(centerX, centerY + 86f)
                                    )
                                }
                                
                                // 装饰小星星 - 围绕小狗
                                for (i in 0..5) {
                                    val angle = (i * 60f + colorPhase) * Math.PI / 180
                                    val distance = 110f
                                    val x = centerX + (distance * cos(angle)).toFloat()
                                    val y = centerY + (distance * sin(angle)).toFloat()
                                    
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(x, y - 4f)
                                            lineTo(x - 3f, y)
                                            lineTo(x, y + 4f)
                                            lineTo(x + 3f, y)
                                            close()
                                        },
                                        color = Color(0xFFFFD700).copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 应用旋转和位移
                    rotate(dogRotation, pivot = Offset(centerX, centerY)) {
                        translate(0f, dogOffsetY) {
                            scale(dogScale, pivot = Offset(centerX, centerY)) {
                                // 外层光晕
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x40FFD700),
                                            Color(0x20FFD700),
                                            Color(0x00FFD700)
                                        )
                                    ),
                                    radius = 120f,
                                    center = Offset(centerX, centerY)
                                )
                                
                                // 头部主体 - 圆润可爱
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFF8E1),
                                            Color(0xFFFFE082)
                                        ),
                                        center = Offset(centerX - 20f, centerY - 20f)
                                    ),
                                    radius = 80f,
                                    center = Offset(centerX, centerY)
                                )
                                
                                // 头部边框
                                drawCircle(
                                    color = Color(0xFFFFB74D),
                                    radius = 82f,
                                    center = Offset(centerX, centerY),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                                )
                                
                                // 左耳朵 - 圆润可爱
                                rotate(leftEarRotation, pivot = Offset(centerX - 55f, centerY - 45f)) {
                                    // 耳朵外层
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFB74D),
                                                Color(0xFFFF8A65)
                                            )
                                        ),
                                        radius = 28f,
                                        center = Offset(centerX - 55f, centerY - 45f)
                                    )
                                    // 耳朵内层
                                    drawCircle(
                                        color = Color(0xFFFF6B9D),
                                        radius = 12f,
                                        center = Offset(centerX - 55f, centerY - 45f)
                                    )
                                }
                                
                                // 右耳朵 - 圆润可爱
                                rotate(rightEarRotation, pivot = Offset(centerX + 55f, centerY - 45f)) {
                                    // 耳朵外层
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFB74D),
                                                Color(0xFFFF8A65)
                                            )
                                        ),
                                        radius = 28f,
                                        center = Offset(centerX + 55f, centerY - 45f)
                                    )
                                    // 耳朵内层
                                    drawCircle(
                                        color = Color(0xFFFF6B9D),
                                        radius = 12f,
                                        center = Offset(centerX + 55f, centerY - 45f)
                                    )
                                }
                                
                                // 左眼 - 超大可爱眼睛
                                scale(1f, eyeScale, pivot = Offset(centerX - 28f, centerY - 15f)) {
                                    // 眼白
                                    drawCircle(
                                        color = Color.White,
                                        radius = 16f,
                                        center = Offset(centerX - 28f, centerY - 15f)
                                    )
                                    // 眼珠
                                    drawCircle(
                                        color = Color(0xFF424242),
                                        radius = 10f,
                                        center = Offset(centerX - 28f, centerY - 12f)
                                    )
                                    // 高光1
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5f,
                                        center = Offset(centerX - 32f, centerY - 16f)
                                    )
                                    // 高光2
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2f,
                                        center = Offset(centerX - 24f, centerY - 10f)
                                    )
                                }
                                
                                // 右眼 - 超大可爱眼睛
                                scale(1f, eyeScale, pivot = Offset(centerX + 28f, centerY - 15f)) {
                                    // 眼白
                                    drawCircle(
                                        color = Color.White,
                                        radius = 16f,
                                        center = Offset(centerX + 28f, centerY - 15f)
                                    )
                                    // 眼珠
                                    drawCircle(
                                        color = Color(0xFF424242),
                                        radius = 10f,
                                        center = Offset(centerX + 28f, centerY - 12f)
                                    )
                                    // 高光1
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5f,
                                        center = Offset(centerX + 24f, centerY - 16f)
                                    )
                                    // 高光2
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2f,
                                        center = Offset(centerX + 32f, centerY - 10f)
                                    )
                                }
                                
                                // 鼻子 - 心形
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 20f)
                                        cubicTo(
                                            centerX - 15f, centerY + 5f,
                                            centerX - 25f, centerY + 15f,
                                            centerX, centerY + 28f
                                        )
                                        cubicTo(
                                            centerX + 25f, centerY + 15f,
                                            centerX + 15f, centerY + 5f,
                                            centerX, centerY + 20f
                                        )
                                        close()
                                    },
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF424242),
                                            Color(0xFF212121)
                                        )
                                    )
                                )
                                
                                // 鼻子高光
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.6f),
                                    radius = 4f,
                                    center = Offset(centerX - 5f, centerY + 18f)
                                )
                                
                                // 嘴巴 - 超级开心的笑脸
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 28f)
                                        quadraticBezierTo(
                                            centerX - 30f, centerY + 55f,
                                            centerX - 45f, centerY + 40f
                                        )
                                    },
                                    color = Color(0xFF424242),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 4f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 28f)
                                        quadraticBezierTo(
                                            centerX + 30f, centerY + 55f,
                                            centerX + 45f, centerY + 40f
                                        )
                                    },
                                    color = Color(0xFF424242),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 4f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                
                                // 舌头 - 可爱的小舌头
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY + 35f)
                                        quadraticBezierTo(
                                            centerX - 10f, centerY + 42f,
                                            centerX, centerY + 48f
                                        )
                                        quadraticBezierTo(
                                            centerX + 10f, centerY + 42f,
                                            centerX, centerY + 35f
                                        )
                                        close()
                                    },
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B9D),
                                            Color(0xFFFF1744)
                                        )
                                    )
                                )
                                
                                // 腮红 - 更大更可爱
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF8A65).copy(alpha = 0.6f),
                                            Color(0xFFFF8A65).copy(alpha = 0.2f),
                                            Color(0x00FF8A65)
                                        )
                                    ),
                                    radius = 18f,
                                    center = Offset(centerX - 60f, centerY + 10f)
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFF8A65).copy(alpha = 0.6f),
                                            Color(0xFFFF8A65).copy(alpha = 0.2f),
                                            Color(0x00FF8A65)
                                        )
                                    ),
                                    radius = 18f,
                                    center = Offset(centerX + 60f, centerY + 10f)
                                )
                                
                                // 头顶毛发 - 可爱的呆毛
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX, centerY - 80f)
                                        quadraticBezierTo(
                                            centerX - 8f, centerY - 70f,
                                            centerX - 5f, centerY - 60f
                                        )
                                    },
                                    color = Color(0xFFFFB74D),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 6f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawPath(
                                    path = Path().apply {
                                        moveTo(centerX + 10f, centerY - 78f)
                                        quadraticBezierTo(
                                            centerX + 15f, centerY - 68f,
                                            centerX + 8f, centerY - 60f
                                        )
                                    },
                                    color = Color(0xFFFFB74D),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 6f,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                
                                // 项圈 - 带旋转的闪亮项圈
                                rotate(collarRotation, pivot = Offset(centerX, centerY + 70f)) {
                                    // 项圈主体
                                    drawPath(
                                        path = Path().apply {
                                            addArc(
                                                oval = androidx.compose.ui.geometry.Rect(
                                                    centerX - 70f,
                                                    centerY + 40f,
                                                    centerX + 70f,
                                                    centerY + 100f
                                                ),
                                                startAngleDegrees = 200f,
                                                sweepAngleDegrees = 140f
                                            )
                                        },
                                        color = Color(0xFFFF5252),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 8f,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                    
                                    // 项圈装饰 - 闪亮的星星
                                    drawPath(
                                        path = Path().apply {
                                            moveTo(centerX, centerY + 65f)
                                            lineTo(centerX - 6f, centerY + 71f)
                                            lineTo(centerX, centerY + 77f)
                                            lineTo(centerX + 6f, centerY + 71f)
                                            close()
                                            moveTo(centerX - 4f, centerY + 71f)
                                            lineTo(centerX, centerY + 65f)
                                            lineTo(centerX + 4f, centerY + 71f)
                                            lineTo(centerX, centerY + 77f)
                                            close()
                                        },
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA000)
                                            )
                                        )
                                    )
                                    
                                    // 星星高光
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        radius = 2f,
                                        center = Offset(centerX - 2f, centerY + 69f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(50.dp))
            
            // 品牌文字 - 美化版
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(100.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerX = width / 2
                    
                    scale(brandScale, pivot = Offset(centerX, height / 2)) {
                        // 外层阴影
                        drawRoundRect(
                            color = Color(0x40000000),
                            topLeft = Offset(4f, 4f),
                            size = androidx.compose.ui.geometry.Size(width - 8f, height - 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(25f, 25f)
                        )
                        
                        // 背景卡片 - 渐变
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFFFF8E1)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(width, height)
                            ),
                            topLeft = Offset(0f, 0f),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(25f, 25f)
                        )
                        
                        // 内层光晕
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x20FFD700),
                                    Color(0x00FFD700)
                                ),
                                center = Offset(centerX, height / 2)
                            ),
                            topLeft = Offset(10f, 10f),
                            size = androidx.compose.ui.geometry.Size(width - 20f, height - 20f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                        )
                        
                        // 装饰线条 - 左上
                        drawPath(
                            path = Path().apply {
                                moveTo(20f, 15f)
                                lineTo(50f, 15f)
                            },
                            color = Color(0x40FFB74D)
                        )
                        
                        // 装饰线条 - 右上
                        drawPath(
                            path = Path().apply {
                                moveTo(width - 50f, 15f)
                                lineTo(width - 20f, 15f)
                            },
                            color = Color(0x40FFB74D)
                        )
                        
                        // 装饰线条 - 左下
                        drawPath(
                            path = Path().apply {
                                moveTo(20f, height - 15f)
                                lineTo(50f, height - 15f)
                            },
                            color = Color(0x40FFB74D)
                        )
                        
                        // 装饰线条 - 右下
                        drawPath(
                            path = Path().apply {
                                moveTo(width - 50f, height - 15f)
                                lineTo(width - 20f, height - 15f)
                            },
                            color = Color(0x40FFB74D)
                        )
                        
                        // "一" - 上横线主体
                        val color1 = Color.hsv(colorPhase, 0.7f, 0.95f)
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    color1.copy(alpha = 0.8f),
                                    color1,
                                    color1.copy(alpha = 0.8f)
                                )
                            ),
                            topLeft = Offset(50f, 28f),
                            size = androidx.compose.ui.geometry.Size(width - 100f, 10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // "一" - 上横线阴影
                        drawRoundRect(
                            color = Color(0x30000000),
                            topLeft = Offset(52f, 30f),
                            size = androidx.compose.ui.geometry.Size(width - 104f, 10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // "十" - 竖线主体
                        val color2 = Color.hsv((colorPhase + 120f) % 360f, 0.7f, 0.95f)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    color2.copy(alpha = 0.8f),
                                    color2,
                                    color2.copy(alpha = 0.8f)
                                )
                            ),
                            topLeft = Offset(centerX - 5f, 20f),
                            size = androidx.compose.ui.geometry.Size(10f, height - 40f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // "十" - 竖线阴影
                        drawRoundRect(
                            color = Color(0x30000000),
                            topLeft = Offset(centerX - 3f, 22f),
                            size = androidx.compose.ui.geometry.Size(10f, height - 44f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // "十" - 横线主体
                        val color3 = Color.hsv((colorPhase + 240f) % 360f, 0.7f, 0.95f)
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    color3.copy(alpha = 0.8f),
                                    color3,
                                    color3.copy(alpha = 0.8f)
                                )
                            ),
                            topLeft = Offset(50f, height - 38f),
                            size = androidx.compose.ui.geometry.Size(width - 100f, 10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // "十" - 横线阴影
                        drawRoundRect(
                            color = Color(0x30000000),
                            topLeft = Offset(52f, height - 36f),
                            size = androidx.compose.ui.geometry.Size(width - 104f, 10f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                        )
                        
                        // 左侧装饰 - 小星星
                        drawPath(
                            path = Path().apply {
                                moveTo(25f, height / 2)
                                lineTo(28f, height / 2 - 5f)
                                lineTo(31f, height / 2)
                                lineTo(28f, height / 2 + 5f)
                                close()
                            },
                            color = Color(0xFFFFD700).copy(alpha = 0.6f)
                        )
                        
                        // 右侧装饰 - 小星星
                        drawPath(
                            path = Path().apply {
                                moveTo(width - 25f, height / 2)
                                lineTo(width - 28f, height / 2 - 5f)
                                lineTo(width - 31f, height / 2)
                                lineTo(width - 28f, height / 2 + 5f)
                                close()
                            },
                            color = Color(0xFFFFD700).copy(alpha = 0.6f)
                        )
                        
                        // 左侧装饰 - 小圆点
                        drawCircle(
                            color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(35f, 25f)
                        )
                        drawCircle(
                            color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(35f, height - 25f)
                        )
                        
                        // 右侧装饰 - 小圆点
                        drawCircle(
                            color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(width - 35f, 25f)
                        )
                        drawCircle(
                            color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                            radius = 3f,
                            center = Offset(width - 35f, height - 25f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 加载文字
            Text(
                text = "正在加载...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
