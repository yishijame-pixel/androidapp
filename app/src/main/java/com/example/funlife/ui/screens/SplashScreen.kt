package com.example.funlife.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // 缩短加载时间到1.5秒
        for (i in 0..100) {
            progress = i / 100f
            delay(15) // 总共1.5秒
        }
        onTimeout()
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    // 小狗跳动
    val dogBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // 小狗缩放
    val dogScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // 耳朵摇摆
    val earWiggle by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear"
    )
    
    // 尾巴摇摆
    val tailWag by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail"
    )
    
    // 眨眼
    val eyeBlink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing, delayMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )
    
    // 光环旋转
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo"
    )
    
    // 粒子动画
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle"
    )
    
    // 彩虹色相
    val rainbowHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFE082),
                        Color(0xFFFFB74D)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2
            
            // 旋转光环
            rotate(haloRotation, pivot = Offset(centerX, centerY)) {
                for (i in 0..7) {
                    val angle = (i * 45f) * Math.PI / 180
                    val radius = 250f
                    val x = centerX + (radius * cos(angle)).toFloat()
                    val y = centerY + (radius * sin(angle)).toFloat()
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.hsv((rainbowHue + i * 45f) % 360f, 0.5f, 1f).copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 80f
                        ),
                        radius = 80f,
                        center = Offset(x, y)
                    )
                }
            }
            
            // 漂浮星星
            for (i in 0..25) {
                val angle = (i * 14.4f + particlePhase * 360f) * Math.PI / 180
                val distance = 180f + (i % 4) * 60f
                val x = centerX + (distance * cos(angle)).toFloat()
                val y = centerY + (distance * sin(angle)).toFloat()
                
                val starPath = Path().apply {
                    moveTo(x, y - 6f)
                    lineTo(x - 2f, y - 2f)
                    lineTo(x - 6f, y)
                    lineTo(x - 2f, y + 2f)
                    lineTo(x, y + 6f)
                    lineTo(x + 2f, y + 2f)
                    lineTo(x + 6f, y)
                    lineTo(x + 2f, y - 2f)
                    close()
                }
                
                drawPath(
                    path = starPath,
                    color = Color.hsv((rainbowHue + i * 14.4f) % 360f, 0.6f, 1f).copy(alpha = 0.7f)
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 小狗图标
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(y = dogBounce.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    scale(dogScale, pivot = Offset(centerX, centerY)) {
                        // 外层彩虹光晕
                        for (i in 0..3) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.hsv((rainbowHue + i * 30f) % 360f, 0.5f, 1f).copy(alpha = 0.2f - i * 0.05f),
                                        Color.Transparent
                                    ),
                                    radius = 110f - i * 15f
                                ),
                                radius = 110f - i * 15f,
                                center = Offset(centerX, centerY)
                            )
                        }
                        
                        // 尾巴
                        rotate(tailWag, pivot = Offset(centerX + 70f, centerY + 30f)) {
                            val tailPath = Path().apply {
                                moveTo(centerX + 70f, centerY + 30f)
                                quadraticBezierTo(
                                    centerX + 85f, centerY + 10f,
                                    centerX + 90f, centerY - 10f
                                )
                            }
                            
                            drawPath(
                                path = tailPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFB74D),
                                        Color(0xFFFF8A65)
                                    )
                                ),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 12f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                        
                        // 身体
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF9C4),
                                    Color(0xFFFFE082)
                                ),
                                center = Offset(centerX - 10f, centerY + 45f)
                            ),
                            radius = 45f,
                            center = Offset(centerX, centerY + 45f)
                        )
                        
                        drawCircle(
                            color = Color(0xFFFFB74D),
                            radius = 46f,
                            center = Offset(centerX, centerY + 45f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                        
                        // 头部
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFDE7),
                                    Color(0xFFFFF9C4),
                                    Color(0xFFFFE082)
                                ),
                                center = Offset(centerX - 20f, centerY - 20f)
                            ),
                            radius = 75f,
                            center = Offset(centerX, centerY)
                        )
                        
                        drawCircle(
                            color = Color(0xFFFFB74D),
                            radius = 77f,
                            center = Offset(centerX, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                        
                        // 左耳朵
                        rotate(earWiggle, pivot = Offset(centerX - 55f, centerY - 45f)) {
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFB74D),
                                        Color(0xFFFF8A65)
                                    )
                                ),
                                topLeft = Offset(centerX - 70f, centerY - 60f),
                                size = androidx.compose.ui.geometry.Size(30f, 45f)
                            )
                            
                            drawOval(
                                color = Color(0xFFFF6B9D),
                                topLeft = Offset(centerX - 65f, centerY - 55f),
                                size = androidx.compose.ui.geometry.Size(20f, 35f)
                            )
                        }
                        
                        // 右耳朵
                        rotate(-earWiggle, pivot = Offset(centerX + 55f, centerY - 45f)) {
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFB74D),
                                        Color(0xFFFF8A65)
                                    )
                                ),
                                topLeft = Offset(centerX + 40f, centerY - 60f),
                                size = androidx.compose.ui.geometry.Size(30f, 45f)
                            )
                            
                            drawOval(
                                color = Color(0xFFFF6B9D),
                                topLeft = Offset(centerX + 45f, centerY - 55f),
                                size = androidx.compose.ui.geometry.Size(20f, 35f)
                            )
                        }
                        
                        // 左眼
                        scale(1f, eyeBlink, pivot = Offset(centerX - 28f, centerY - 15f)) {
                            drawOval(
                                color = Color.White,
                                topLeft = Offset(centerX - 42f, centerY - 30f),
                                size = androidx.compose.ui.geometry.Size(28f, 32f)
                            )
                            
                            drawCircle(
                                color = Color(0xFF424242),
                                radius = 11f,
                                center = Offset(centerX - 28f, centerY - 14f)
                            )
                            
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(centerX - 32f, centerY - 18f)
                            )
                            
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = Offset(centerX - 24f, centerY - 12f)
                            )
                        }
                        
                        // 右眼
                        scale(1f, eyeBlink, pivot = Offset(centerX + 28f, centerY - 15f)) {
                            drawOval(
                                color = Color.White,
                                topLeft = Offset(centerX + 14f, centerY - 30f),
                                size = androidx.compose.ui.geometry.Size(28f, 32f)
                            )
                            
                            drawCircle(
                                color = Color(0xFF424242),
                                radius = 11f,
                                center = Offset(centerX + 28f, centerY - 14f)
                            )
                            
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(centerX + 24f, centerY - 18f)
                            )
                            
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = Offset(centerX + 32f, centerY - 12f)
                            )
                        }
                        
                        // 鼻子
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF424242),
                                    Color(0xFF212121)
                                )
                            ),
                            radius = 10f,
                            center = Offset(centerX, centerY + 18f)
                        )
                        
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 4f,
                            center = Offset(centerX - 3f, centerY + 15f)
                        )
                        
                        // 嘴巴
                        val mouthPath1 = Path().apply {
                            moveTo(centerX, centerY + 28f)
                            quadraticBezierTo(
                                centerX - 20f, centerY + 42f,
                                centerX - 35f, centerY + 35f
                            )
                        }
                        
                        drawPath(
                            path = mouthPath1,
                            color = Color(0xFF424242),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        
                        val mouthPath2 = Path().apply {
                            moveTo(centerX, centerY + 28f)
                            quadraticBezierTo(
                                centerX + 20f, centerY + 42f,
                                centerX + 35f, centerY + 35f
                            )
                        }
                        
                        drawPath(
                            path = mouthPath2,
                            color = Color(0xFF424242),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 4f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        
                        // 舌头
                        val tonguePath = Path().apply {
                            moveTo(centerX, centerY + 32f)
                            quadraticBezierTo(
                                centerX - 8f, centerY + 38f,
                                centerX, centerY + 44f
                            )
                            quadraticBezierTo(
                                centerX + 8f, centerY + 38f,
                                centerX, centerY + 32f
                            )
                            close()
                        }
                        
                        drawPath(
                            path = tonguePath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF6B9D),
                                    Color(0xFFFF1744)
                                )
                            )
                        )
                        
                        // 腮红
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8A65).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(centerX - 75f, centerY + 5f),
                            size = androidx.compose.ui.geometry.Size(25f, 20f)
                        )
                        
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF8A65).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(centerX + 50f, centerY + 5f),
                            size = androidx.compose.ui.geometry.Size(25f, 20f)
                        )
                        
                        // 呆毛
                        for (i in -1..1) {
                            val hairPath = Path().apply {
                                moveTo(centerX + i * 10f, centerY - 75f)
                                quadraticBezierTo(
                                    centerX + i * 12f, centerY - 65f,
                                    centerX + i * 8f, centerY - 55f
                                )
                            }
                            
                            drawPath(
                                path = hairPath,
                                color = Color(0xFFFFB74D),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 6f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            )
                        }
                        
                        // 小爪子
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFE082),
                                    Color(0xFFFFB74D)
                                )
                            ),
                            radius = 15f,
                            center = Offset(centerX - 50f, centerY + 65f)
                        )
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFE082),
                                    Color(0xFFFFB74D)
                                )
                            ),
                            radius = 15f,
                            center = Offset(centerX + 50f, centerY + 65f)
                        )
                        
                        // 爪子肉垫
                        for (i in 0..2) {
                            drawCircle(
                                color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                                radius = 2.5f,
                                center = Offset(centerX - 50f + (i - 1) * 5f, centerY + 63f)
                            )
                            
                            drawCircle(
                                color = Color(0xFFFF6B9D).copy(alpha = 0.5f),
                                radius = 2.5f,
                                center = Offset(centerX + 50f + (i - 1) * 5f, centerY + 63f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // 品牌名称 - 一十（带特效）
            Box(
                contentAlignment = Alignment.Center
            ) {
                // 外层光晕
                Text(
                    text = "一十",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFB74D).copy(alpha = 0.3f),
                    letterSpacing = 4.sp,
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                )
                
                // 主文字
                Text(
                    text = "一十",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF424242),
                                Color(0xFF6D4C41)
                            )
                        )
                    ),
                    letterSpacing = 4.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 副标题 - 趣味生活（带装饰）
            Box(
                contentAlignment = Alignment.Center
            ) {
                // 背景装饰
                Box(
                    modifier = Modifier
                        .offset(y = 2.dp)
                        .background(
                            Color.White.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
                
                // 文字
                Text(
                    text = "趣味生活",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D4C41),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "让每一天都充满惊喜",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8D6E63).copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 加载进度条
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // 背景
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(0f, 0f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
                    )
                    
                    // 进度
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.hsv(rainbowHue % 360f, 0.7f, 1f),
                                Color.hsv((rainbowHue + 60f) % 360f, 0.7f, 1f),
                                Color.hsv((rainbowHue + 120f) % 360f, 0.7f, 1f)
                            )
                        ),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(width * progress, height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
                    )
                    
                    // 光点
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                Color.Transparent
                            ),
                            radius = 12f
                        ),
                        radius = 12f,
                        center = Offset(width * particlePhase, height / 2)
                    )
                }
            }
        }
    }
}
