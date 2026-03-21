package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 增强版顶部导航栏
 * @param title 标题
 * @param subtitle 副标题（可选）
 * @param icon 左侧图标
 * @param actions 右侧操作按钮列表
 * @param gradientColors 渐变色列表（可选，默认使用主题色）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTopBar(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    actions: List<TopBarAction> = emptyList(),
    gradientColors: List<Color>? = null,
    onIconClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "topbar")
    
    // 光晕动画
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val defaultGradient = listOf(
        Color(0xFF6A1B9A),
        Color(0xFF8E24AA),
        Color(0xFFAB47BC)
    )
    
    val colors = gradientColors ?: defaultGradient
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) 100.dp else 80.dp)
            .shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        // 背景渐变
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(colors),
                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
        )
        
        // 装饰性背景动画
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // 漂浮圆点
            for (i in 0..8) {
                val x = width * (i % 3) / 3f + width / 6f
                val y = height * (i / 3) / 3f + height / 6f
                val offset = shimmer * 50f
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = 15f + (i % 3) * 5f,
                    center = Offset(x + offset, y)
                )
            }
            
            // 装饰线条
            for (i in 0..3) {
                val angle = (i * 45f + shimmer * 360f) * Math.PI.toFloat() / 180f
                val startX = width - 50f
                val startY = 30f
                val endX = startX + 30f * cos(angle)
                val endY = startY + 30f * sin(angle)
                
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }
        
        // 内容
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(enabled = onIconClick != null) { onIconClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 标题区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            
            // 右侧操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (action.highlighted) {
                                    Color.White.copy(alpha = 0.3f)
                                } else {
                                    Color.White.copy(alpha = 0.15f)
                                }
                            )
                            .clickable { action.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (action.badge != null) {
                            Box {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.contentDescription,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                // 徽章
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF5252)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = action.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 顶部栏操作按钮数据类
 */
data class TopBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val badge: String? = null,
    val highlighted: Boolean = false
)

/**
 * 预设的渐变色方案
 */
object TopBarGradients {
    val Purple = listOf(
        Color(0xFF6A1B9A),
        Color(0xFF8E24AA),
        Color(0xFFAB47BC)
    )
    
    val Blue = listOf(
        Color(0xFF1976D2),
        Color(0xFF2196F3),
        Color(0xFF42A5F5)
    )
    
    val Green = listOf(
        Color(0xFF388E3C),
        Color(0xFF4CAF50),
        Color(0xFF66BB6A)
    )
    
    val Orange = listOf(
        Color(0xFFE65100),
        Color(0xFFFF6F00),
        Color(0xFFFF8F00)
    )
    
    val Pink = listOf(
        Color(0xFFC2185B),
        Color(0xFFE91E63),
        Color(0xFFF06292)
    )
    
    val Teal = listOf(
        Color(0xFF00796B),
        Color(0xFF009688),
        Color(0xFF26A69A)
    )
    
    val DeepPurple = listOf(
        Color(0xFF512DA8),
        Color(0xFF673AB7),
        Color(0xFF7E57C2)
    )
    
    val Indigo = listOf(
        Color(0xFF303F9F),
        Color(0xFF3F51B5),
        Color(0xFF5C6BC0)
    )
}
