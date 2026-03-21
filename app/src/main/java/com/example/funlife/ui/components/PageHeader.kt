package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 页面头部组件 - 美化版
 * 用于替换简单的 emoji + 文字头部
 */
@Composable
fun PageHeader(
    title: String,
    emoji: String,
    gradientColors: List<Color>,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    
    // 光晕动画
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // Emoji 跳动
    val emojiScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 140.dp else 120.dp)
            .shadow(12.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        // 背景渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(gradientColors),
                    RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
        )
        
        // 装饰性背景
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // 波浪装饰
            val wavePath = Path().apply {
                moveTo(0f, height * 0.6f)
                for (x in 0..width.toInt() step 30) {
                    val y = height * 0.6f + 20f * sin((x / 80f + shimmer * 2f) * Math.PI).toFloat()
                    lineTo(x.toFloat(), y)
                }
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            
            // 漂浮圆点
            for (i in 0..12) {
                val angle = (i * 30f + shimmer * 360f) * Math.PI.toFloat() / 180f
                val radius = 80f + (i % 3) * 40f
                val x = width * 0.85f + radius * cos(angle)
                val y = height * 0.3f + radius * sin(angle)
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 8f + (i % 3) * 3f,
                    center = Offset(x, y)
                )
            }
            
            // 装饰星星
            for (i in 0..5) {
                val x = width * (0.1f + i * 0.15f)
                val y = height * 0.2f + (i % 2) * 15f
                val starSize = 6f
                
                val starPath = Path().apply {
                    moveTo(x, y - starSize)
                    lineTo(x - starSize * 0.3f, y - starSize * 0.3f)
                    lineTo(x - starSize, y)
                    lineTo(x - starSize * 0.3f, y + starSize * 0.3f)
                    lineTo(x, y + starSize)
                    lineTo(x + starSize * 0.3f, y + starSize * 0.3f)
                    lineTo(x + starSize, y)
                    lineTo(x + starSize * 0.3f, y - starSize * 0.3f)
                    close()
                }
                
                drawPath(
                    path = starPath,
                    color = Color.White.copy(alpha = 0.2f)
                )
            }
        }
        
        // 内容
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮（如果需要）
            if (showBackButton && onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Emoji 图标容器
            Box(
                modifier = Modifier
                    .size(if (subtitle != null) 80.dp else 70.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // 内层圆
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Emoji
                    Text(
                        text = emoji,
                        fontSize = if (subtitle != null) 42.sp else 36.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = emojiScale
                            scaleY = emojiScale
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // 文字区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = if (subtitle != null) 32.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        // 底部装饰线
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.9f)
                .height(4.dp)
                .offset(y = (-8).dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                
                // 渐变装饰线
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
                )
            }
        }
    }
}

/**
 * 预设的页面头部渐变色方案
 */
object PageHeaderGradients {
    // 首页 - 温暖橙色
    val Home = listOf(
        Color(0xFFFF6B35),
        Color(0xFFFF8C42),
        Color(0xFFFFA94D)
    )
    
    // 打卡 - 清新绿色
    val CheckIn = listOf(
        Color(0xFF43A047),
        Color(0xFF66BB6A),
        Color(0xFF81C784)
    )
    
    // 心情 - 温柔粉色
    val Mood = listOf(
        Color(0xFFEC407A),
        Color(0xFFF06292),
        Color(0xFFF48FB1)
    )
    
    // 我的 - 优雅蓝色
    val Profile = listOf(
        Color(0xFF1E88E5),
        Color(0xFF42A5F5),
        Color(0xFF64B5F6)
    )
    
    // 转盘 - 神秘紫色
    val Wheel = listOf(
        Color(0xFF8E24AA),
        Color(0xFFAB47BC),
        Color(0xFFBA68C8)
    )
    
    // 目标 - 活力橙色
    val Goal = listOf(
        Color(0xFFFB8C00),
        Color(0xFFFF9800),
        Color(0xFFFFA726)
    )
    
    // 纪念日 - 浪漫紫粉
    val Anniversary = listOf(
        Color(0xFF9C27B0),
        Color(0xFFBA68C8),
        Color(0xFFCE93D8)
    )
    
    // 商店 - 青色
    val Shop = listOf(
        Color(0xFF00ACC1),
        Color(0xFF26C6DA),
        Color(0xFF4DD0E1)
    )
}
