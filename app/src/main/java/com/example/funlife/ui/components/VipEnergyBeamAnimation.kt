// VipEnergyBeamAnimation.kt - 超炫酷VIP能量光束边框动画
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * VIP能量光束边框动画
 * 光束沿着屏幕边缘飞行一圈，然后聚集到头像位置
 * 
 * @param vipLevel VIP等级
 * @param avatarPosition 头像在屏幕中的位置（相对于屏幕顶部的偏移）
 * @param onAnimationComplete 动画完成回调
 */
@Composable
fun VipEnergyBeamAnimation(
    vipLevel: VipLevel,
    avatarPosition: Offset = Offset(60.dp.value, 100.dp.value), // 默认头像位置
    onAnimationComplete: () -> Unit
) {
    var animationPhase by remember { mutableStateOf(0) }
    
    // 动画阶段：
    // 0: 准备
    // 1: 光束沿边框飞行（2秒）
    // 2: 光束聚集到头像（0.5秒）
    // 3: VIP标识显示（0.5秒）
    // 4: 完成
    
    LaunchedEffect(Unit) {
        delay(100)
        animationPhase = 1
        delay(2000)
        animationPhase = 2
        delay(500)
        animationPhase = 3
        delay(500)
        animationPhase = 4
        onAnimationComplete()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f)
    ) {
        // 边框光束动画
        if (animationPhase >= 1 && animationPhase < 3) {
            BorderEnergyBeam(
                vipLevel = vipLevel,
                isActive = animationPhase == 1
            )
        }
        
        // 聚集到头像的光束
        if (animationPhase >= 2 && animationPhase < 4) {
            ConvergingBeams(
                vipLevel = vipLevel,
                targetPosition = avatarPosition,
                isActive = animationPhase == 2
            )
        }
        
        // VIP标识显示
        if (animationPhase >= 3) {
            VipBadgeReveal(
                vipLevel = vipLevel,
                position = avatarPosition
            )
        }
    }
}

/**
 * 边框能量光束 - 沿着屏幕边缘飞行
 */
@Composable
fun BorderEnergyBeam(
    vipLevel: VipLevel,
    isActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    
    // 光束进度（0-1，完成一圈）
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    
    // 光束颜色根据VIP等级
    val beamColors = when (vipLevel) {
        VipLevel.VIP3 -> listOf(
            Color(0xFFFFD700), // 金色
            Color(0xFFFF00FF), // 紫色
            Color(0xFF00D9FF)  // 蓝色
        )
        VipLevel.VIP2 -> listOf(
            Color(0xFF00D9FF), // 蓝色
            Color(0xFF6B5FFF), // 紫色
            Color(0xFF00FFAA)  // 青色
        )
        VipLevel.VIP1 -> listOf(
            Color(0xFFFFD700), // 金色
            Color(0xFFFFB800), // 橙金
            Color(0xFFFF9500)  // 橙色
        )
        else -> listOf(Color.White)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // 计算光束当前位置（沿着矩形边框）
        val totalPerimeter = (width + height) * 2
        val currentDistance = progress * totalPerimeter
        
        val beamPosition = when {
            // 顶边：从左到右
            currentDistance < width -> Offset(currentDistance, 0f)
            // 右边：从上到下
            currentDistance < width + height -> Offset(width, currentDistance - width)
            // 底边：从右到左
            currentDistance < width * 2 + height -> Offset(width - (currentDistance - width - height), height)
            // 左边：从下到上
            else -> Offset(0f, height - (currentDistance - width * 2 - height))
        }
        
        // 绘制多层光束效果
        repeat(5) { layer ->
            val layerProgress = (progress - layer * 0.1f).coerceIn(0f, 1f)
            val layerDistance = layerProgress * totalPerimeter
            
            val layerPosition = when {
                layerDistance < width -> Offset(layerDistance, 0f)
                layerDistance < width + height -> Offset(width, layerDistance - width)
                layerDistance < width * 2 + height -> Offset(width - (layerDistance - width - height), height)
                else -> Offset(0f, height - (layerDistance - width * 2 - height))
            }
            
            // 主光束
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        beamColors[layer % beamColors.size].copy(alpha = 0.8f - layer * 0.15f),
                        beamColors[layer % beamColors.size].copy(alpha = 0.4f - layer * 0.08f),
                        Color.Transparent
                    ),
                    radius = (30f - layer * 5f).dp.toPx()
                ),
                radius = (30f - layer * 5f).dp.toPx(),
                center = layerPosition
            )
            
            // 光束拖尾
            val trailLength = 100f
            val trailStart = when {
                layerDistance < width -> Offset((layerDistance - trailLength).coerceAtLeast(0f), 0f)
                layerDistance < width + height -> Offset(width, (layerDistance - width - trailLength).coerceAtLeast(0f))
                layerDistance < width * 2 + height -> {
                    val x = width - (layerDistance - width - height)
                    Offset((x + trailLength).coerceAtMost(width), height)
                }
                else -> {
                    val y = height - (layerDistance - width * 2 - height)
                    Offset(0f, (y + trailLength).coerceAtMost(height))
                }
            }
            
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        beamColors[layer % beamColors.size].copy(alpha = 0.3f - layer * 0.05f)
                    ),
                    start = trailStart,
                    end = layerPosition
                ),
                start = trailStart,
                end = layerPosition,
                strokeWidth = (15f - layer * 2f).dp.toPx()
            )
        }
        
        // 边框发光效果
        val glowWidth = 4.dp.toPx()
        val glowAlpha = 0.3f + sin(progress * PI.toFloat() * 4) * 0.2f
        
        // 顶边发光
        if (currentDistance < width + 100) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        beamColors[0].copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    startX = (currentDistance - 100).coerceAtLeast(0f),
                    endX = (currentDistance + 100).coerceAtMost(width)
                ),
                start = Offset((currentDistance - 100).coerceAtLeast(0f), 0f),
                end = Offset((currentDistance + 100).coerceAtMost(width), 0f),
                strokeWidth = glowWidth
            )
        }
        
        // 粒子效果
        repeat(20) { i ->
            val particleProgress = (progress + i * 0.05f) % 1f
            val particleDistance = particleProgress * totalPerimeter
            
            val particlePos = when {
                particleDistance < width -> Offset(particleDistance, 0f)
                particleDistance < width + height -> Offset(width, particleDistance - width)
                particleDistance < width * 2 + height -> Offset(width - (particleDistance - width - height), height)
                else -> Offset(0f, height - (particleDistance - width * 2 - height))
            }
            
            drawCircle(
                color = beamColors[i % beamColors.size].copy(alpha = 0.6f),
                radius = (2f + sin(particleProgress * PI.toFloat() * 2) * 1f).dp.toPx(),
                center = particlePos
            )
        }
    }
}

/**
 * 聚集光束 - 从边框聚集到头像位置
 */
@Composable
fun ConvergingBeams(
    vipLevel: VipLevel,
    targetPosition: Offset,
    isActive: Boolean
) {
    val transition = updateTransition(targetState = isActive, label = "converge")
    
    val convergence by transition.animateFloat(
        transitionSpec = {
            tween(500, easing = FastOutSlowInEasing)
        },
        label = "convergence"
    ) { active ->
        if (active) 1f else 0f
    }
    
    val beamColors = when (vipLevel) {
        VipLevel.VIP3 -> listOf(
            Color(0xFFFFD700),
            Color(0xFFFF00FF),
            Color(0xFF00D9FF)
        )
        VipLevel.VIP2 -> listOf(
            Color(0xFF00D9FF),
            Color(0xFF6B5FFF),
            Color(0xFF00FFAA)
        )
        VipLevel.VIP1 -> listOf(
            Color(0xFFFFD700),
            Color(0xFFFFB800),
            Color(0xFFFF9500)
        )
        else -> listOf(Color.White)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val targetX = targetPosition.x.dp.toPx()
        val targetY = targetPosition.y.dp.toPx()
        
        // 从四个角发射光束到头像位置
        val startPoints = listOf(
            Offset(0f, 0f),                    // 左上
            Offset(width, 0f),                 // 右上
            Offset(width, height),             // 右下
            Offset(0f, height),                // 左下
            Offset(width / 2, 0f),             // 上中
            Offset(width, height / 2),         // 右中
            Offset(width / 2, height),         // 下中
            Offset(0f, height / 2)             // 左中
        )
        
        startPoints.forEachIndexed { index, startPoint ->
            val currentX = startPoint.x + (targetX - startPoint.x) * convergence
            val currentY = startPoint.y + (targetY - startPoint.y) * convergence
            val currentPoint = Offset(currentX, currentY)
            
            // 绘制光束
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        beamColors[index % beamColors.size].copy(alpha = 0.1f),
                        beamColors[index % beamColors.size].copy(alpha = 0.6f),
                        beamColors[index % beamColors.size].copy(alpha = 0.9f)
                    ),
                    start = startPoint,
                    end = currentPoint
                ),
                start = startPoint,
                end = currentPoint,
                strokeWidth = (8f - convergence * 4f).dp.toPx()
            )
            
            // 光束头部发光
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        beamColors[index % beamColors.size].copy(alpha = 0.9f),
                        beamColors[index % beamColors.size].copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    radius = 20.dp.toPx()
                ),
                radius = 20.dp.toPx(),
                center = currentPoint
            )
        }
        
        // 目标位置爆发效果
        if (convergence > 0.8f) {
            val burstRadius = (convergence - 0.8f) * 5f * 50.dp.toPx()
            
            repeat(12) { i ->
                val angle = (i * 30f) * (PI / 180f).toFloat()
                val endX = targetX + cos(angle) * burstRadius
                val endY = targetY + sin(angle) * burstRadius
                
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            beamColors[i % beamColors.size].copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        start = Offset(targetX, targetY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(targetX, targetY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        
        // 中心光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = beamColors.map { it.copy(alpha = 0.3f * convergence) } + Color.Transparent,
                radius = 60.dp.toPx() * convergence
            ),
            radius = 60.dp.toPx() * convergence,
            center = Offset(targetX, targetY)
        )
    }
}

/**
 * VIP标识显示动画
 */
@Composable
fun VipBadgeReveal(
    vipLevel: VipLevel,
    position: Offset
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val badgeColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> Color.White
    }
    
    val badgeText = when (vipLevel) {
        VipLevel.VIP3 -> "👑"
        VipLevel.VIP2 -> "💎"
        VipLevel.VIP1 -> "⭐"
        else -> ""
    }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val x = position.x.dp.toPx()
        val y = position.y.dp.toPx()
        
        // 外圈光晕
        repeat(3) { layer ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = (0.3f - layer * 0.1f) * glow),
                        Color.Transparent
                    ),
                    radius = (40f + layer * 15f).dp.toPx() * scale
                ),
                radius = (40f + layer * 15f).dp.toPx() * scale,
                center = Offset(x, y)
            )
        }
        
        // 主圆环
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    badgeColor.copy(alpha = 0.9f),
                    badgeColor.copy(alpha = 0.6f)
                )
            ),
            radius = 25.dp.toPx() * scale,
            center = Offset(x, y)
        )
        
        // 内圈
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 20.dp.toPx() * scale,
            center = Offset(x, y)
        )
        
        // 描边
        drawCircle(
            color = badgeColor,
            radius = 25.dp.toPx() * scale,
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 旋转光芒
        repeat(8) { i ->
            val angle = (i * 45f + rotation) * (PI / 180f).toFloat()
            val startRadius = 28.dp.toPx() * scale
            val endRadius = 45.dp.toPx() * scale
            
            val startX = x + cos(angle) * startRadius
            val startY = y + sin(angle) * startRadius
            val endX = x + cos(angle) * endRadius
            val endY = y + sin(angle) * endRadius
            
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.8f * glow),
                        Color.Transparent
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
    
    // VIP图标文字（使用Text组件）
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = badgeText,
            fontSize = (24 * scale).sp,
            modifier = Modifier.offset(
                x = (position.x - 12).dp,
                y = (position.y - 12).dp
            )
        )
    }
}

/**
 * 简化版：直接在头像位置显示VIP标识（用于HomeScreen）
 */
@Composable
fun VipBadgeOverlay(
    vipLevel: VipLevel,
    modifier: Modifier = Modifier
) {
    if (vipLevel == VipLevel.NORMAL) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "vip_badge")
    
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val badgeColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> Color.White
    }
    
    val badgeIcon = when (vipLevel) {
        VipLevel.VIP3 -> "👑"
        VipLevel.VIP2 -> "💎"
        VipLevel.VIP1 -> "⭐"
        else -> ""
    }
    
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.4f * glow),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
            
            // 主圆
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        badgeColor.copy(alpha = 0.9f),
                        badgeColor.copy(alpha = 0.7f)
                    )
                ),
                radius = size.minDimension / 2.5f
            )
            
            // 内圈
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = size.minDimension / 3f
            )
        }
        
        Text(
            text = badgeIcon,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}
