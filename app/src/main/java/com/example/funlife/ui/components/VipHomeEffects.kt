// VipHomeEffects.kt - VIP首页展示特效
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * VIP头像光环效果 - 优雅的渐变边框
 */
@Composable
fun VipAvatarHalo(
    vipLevel: VipLevel,
    modifier: Modifier = Modifier
) {
    val vipColors = when (vipLevel) {
        VipLevel.VIP3 -> listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFFD700)) // 金色渐变
        VipLevel.VIP2 -> listOf(Color(0xFF00D9FF), Color(0xFF0099FF), Color(0xFF00D9FF)) // 蓝色渐变
        VipLevel.VIP1 -> listOf(Color(0xFFFFB800), Color(0xFFFF8C00), Color(0xFFFFB800)) // 橙色渐变
        else -> return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    
    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // 呼吸效果
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    
    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            }
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.width / 2
                
                // 绘制渐变圆环边框
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = vipColors,
                        center = Offset(centerX, centerY)
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )
                
                // 外层柔和光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            vipColors[0].copy(alpha = 0.15f),
                            vipColors[0].copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius + 12f
                    ),
                    radius = radius + 12f,
                    center = Offset(centerX, centerY)
                )
                
                // 旋转的小光点（4个）
                repeat(4) { index ->
                    val angle = (rotation + index * 90f) * (PI / 180f).toFloat()
                    val dotRadius = radius - 2f
                    val dotX = centerX + cos(angle) * dotRadius
                    val dotY = centerY + sin(angle) * dotRadius
                    
                    // 光点
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = 2.5f,
                        center = Offset(dotX, dotY)
                    )
                    
                    // 光点光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColors[1].copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            radius = 6f
                        ),
                        radius = 6f,
                        center = Offset(dotX, dotY)
                    )
                }
            }
    )
}

/**
 * VIP徽章小图标 - 显示在头像角落（无背景，创意动画）
 */
@Composable
fun VipBadgeIcon(
    vipLevel: VipLevel,
    modifier: Modifier = Modifier
) {
    val emoji = when (vipLevel) {
        VipLevel.VIP3 -> "👑"
        VipLevel.VIP2 -> "💎"
        VipLevel.VIP1 -> "⭐"
        else -> return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    
    // 🔥 钟摆式摇摆（左右±12°，像挂件晃动）
    val swing by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swing"
    )
    
    // 🔥 呼吸光晕（透明度脉冲）
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // 🔥 微弹跳（上下浮动2dp）
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Box(
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                rotationZ = swing
                translationY = bounce.dp.toPx()
                // 从顶部中心为旋转锚点（像挂件）
                transformOrigin = TransformOrigin(0.5f, 0f)
            },
        contentAlignment = Alignment.Center
    ) {
        // 底层：柔和光晕
        Box(
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { alpha = glowAlpha }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width
                    )
                }
        )
        // 图标本体（无背景）
        Text(
            text = emoji,
            fontSize = 16.sp
        )
    }
}

/**
 * VIP用户名发光效果 - 精致的文字阴影
 */
@Composable
fun VipGlowingText(
    text: String,
    vipLevel: VipLevel,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val vipColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> Color.White
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "intensity"
    )
    
    if (vipLevel != VipLevel.NORMAL) {
        // VIP用户名 - 带阴影和渐变
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = vipColor.copy(alpha = glowIntensity * 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 12f
                )
            ),
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White,
            modifier = modifier
        )
    }
}

/**
 * 首次进入VIP光芒扩散特效
 */
@Composable
fun VipFirstEntryEffect(
    vipLevel: VipLevel,
    avatarCenter: Offset,
    onComplete: () -> Unit
) {
    val vipColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> return
    }
    
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 1500L
        
        while (animationProgress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            animationProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            delay(16)
        }
        
        onComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val progress = animationProgress
                val alpha = if (progress < 0.5f) {
                    progress * 2f
                } else {
                    (1f - progress) * 2f
                }
                
                // 扩散的光环
                repeat(5) { ring ->
                    val ringProgress = (progress - ring * 0.1f).coerceIn(0f, 1f)
                    val ringRadius = 50f + ringProgress * 300f
                    val ringAlpha = alpha * (1f - ringProgress) * (1f - ring * 0.15f)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColor.copy(alpha = ringAlpha * 0.6f),
                                vipColor.copy(alpha = ringAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = avatarCenter,
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = avatarCenter
                    )
                }
                
                // 中心闪光
                if (progress < 0.3f) {
                    val flashAlpha = (0.3f - progress) / 0.3f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha * 0.8f),
                                vipColor.copy(alpha = flashAlpha * 0.5f),
                                Color.Transparent
                            ),
                            center = avatarCenter,
                            radius = 80f
                        ),
                        radius = 80f,
                        center = avatarCenter
                    )
                }
                
                // 星星粒子
                repeat(12) { star ->
                    val starAngle = (star * 30f + progress * 60f) * (PI / 180f).toFloat()
                    val starDistance = 60f + progress * 200f
                    val starX = avatarCenter.x + cos(starAngle) * starDistance
                    val starY = avatarCenter.y + sin(starAngle) * starDistance
                    val starAlpha = alpha * (1f - progress)
                    
                    drawCircle(
                        color = vipColor.copy(alpha = starAlpha),
                        radius = 4f,
                        center = Offset(starX, starY)
                    )
                    
                    // 星星光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                vipColor.copy(alpha = starAlpha * 0.6f),
                                Color.Transparent
                            ),
                            radius = 12f
                        ),
                        radius = 12f,
                        center = Offset(starX, starY)
                    )
                }
            }
    )
}
