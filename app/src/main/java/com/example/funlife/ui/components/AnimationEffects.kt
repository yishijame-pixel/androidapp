// AnimationEffects.kt - 动画效果组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 粒子数据类
 */
data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    val color: Color,
    val size: Float
)

/**
 * 转盘旋转粒子效果
 */
@Composable
fun SpinParticleEffect(
    isSpinning: Boolean,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var time by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            // 创建粒子
            particles = List(30) {
                val angle = Random.nextFloat() * 360f
                val speed = Random.nextFloat() * 2f + 1f
                Particle(
                    x = 0f,
                    y = 0f,
                    vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                    vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed,
                    life = 1f,
                    maxLife = Random.nextFloat() * 0.5f + 0.5f,
                    color = listOf(
                        Color(0xFFFF6B9D),
                        Color(0xFF4FACFE),
                        Color(0xFFFFA726),
                        Color(0xFF66BB6A),
                        Color(0xFFAB47BC)
                    ).random(),
                    size = Random.nextFloat() * 4f + 2f
                )
            }
        }
    }
    
    LaunchedEffect(isSpinning) {
        while (isSpinning) {
            withFrameMillis { frameTime ->
                time = frameTime / 1000f
                
                // 更新粒子
                particles = particles.map { particle ->
                    particle.copy(
                        x = particle.x + particle.vx,
                        y = particle.y + particle.vy,
                        life = particle.life - 0.02f,
                        vy = particle.vy + 0.1f // 重力
                    )
                }.filter { it.life > 0 }
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        particles.forEach { particle ->
            val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(
                    centerX + particle.x,
                    centerY + particle.y
                )
            )
        }
    }
}

/**
 * 烟花动画效果
 */
@Composable
fun FireworksEffect(
    trigger: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var isAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(trigger) {
        if (trigger && !isAnimating) {
            isAnimating = true
            
            // 创建烟花粒子
            val centerX = 0f
            val centerY = -100f
            
            particles = List(60) {
                val angle = (it * 6f) + Random.nextFloat() * 6f
                val speed = Random.nextFloat() * 8f + 4f
                Particle(
                    x = centerX,
                    y = centerY,
                    vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                    vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed,
                    life = 1f,
                    maxLife = 1f,
                    color = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500),
                        Color(0xFFFF6B9D),
                        Color(0xFF4FACFE),
                        Color(0xFFFFFFFF)
                    ).random(),
                    size = Random.nextFloat() * 6f + 3f
                )
            }
            
            // 动画循环
            var animationTime = 0f
            while (animationTime < 2000f) {
                withFrameMillis { frameTime ->
                    animationTime += 16f
                    
                    particles = particles.map { particle ->
                        particle.copy(
                            x = particle.x + particle.vx,
                            y = particle.y + particle.vy,
                            vx = particle.vx * 0.98f,
                            vy = particle.vy * 0.98f + 0.2f,
                            life = particle.life - 0.01f
                        )
                    }.filter { it.life > 0 }
                }
            }
            
            isAnimating = false
            particles = emptyList()
            onComplete()
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        particles.forEach { particle ->
            val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
            
            // 绘制粒子轨迹
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.color.copy(alpha = alpha),
                        particle.color.copy(alpha = alpha * 0.5f),
                        Color.Transparent
                    ),
                    radius = particle.size * 2
                ),
                radius = particle.size * 2,
                center = Offset(
                    centerX + particle.x,
                    centerY + particle.y
                )
            )
            
            // 绘制粒子核心
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(
                    centerX + particle.x,
                    centerY + particle.y
                )
            )
        }
    }
}

/**
 * 金币获得动画
 */
@Composable
fun CoinGainAnimation(
    amount: Int,
    trigger: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var scale by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            // 向上飘动画
            animate(0f, -100f, animationSpec = tween(1500, easing = FastOutSlowInEasing)) { value, _ ->
                offsetY = value
            }
            
            // 缩放动画
            animate(1f, 1.5f, animationSpec = tween(300)) { value, _ ->
                scale = value
            }
            animate(1.5f, 1f, animationSpec = tween(200)) { value, _ ->
                scale = value
            }
            
            // 延迟后淡出
            kotlinx.coroutines.delay(1000)
            animate(1f, 0f, animationSpec = tween(500)) { value, _ ->
                alpha = value
            }
            
            onComplete()
        }
    }
    
    if (trigger) {
        Box(
            modifier = modifier
                .offset(y = offsetY.dp)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    )
            ) {
                androidx.compose.material3.Text(
                    "+$amount 💰",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

/**
 * 模式切换过渡动画
 */
@Composable
fun ModeTransitionEffect(
    isTransitioning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "modeTransition")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    if (isTransitioning) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // 绘制旋转的光环
            for (i in 0..2) {
                val radius = 100f + i * 30f
                val alpha = 0.3f - i * 0.1f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = alpha * scale),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius * scale
                    ),
                    radius = radius * scale,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

/**
 * 保底触发特效
 */
@Composable
fun GuaranteeTriggeredEffect(
    trigger: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rings by remember { mutableStateOf<List<Float>>(emptyList()) }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            rings = listOf(0f, 0.2f, 0.4f)
            
            var time = 0f
            while (time < 1500f) {
                withFrameMillis {
                    time += 16f
                    rings = rings.map { it + 0.02f }.filter { it < 2f }
                }
            }
            
            rings = emptyList()
            onComplete()
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        rings.forEach { progress ->
            val radius = 150f * progress
            val alpha = (1f - progress / 2f).coerceIn(0f, 1f)
            
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = alpha),
                radius = radius,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
        }
    }
}
