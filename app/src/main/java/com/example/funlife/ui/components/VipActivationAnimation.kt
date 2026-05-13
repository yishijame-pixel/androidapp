// VipActivationAnimation.kt - 全新炫酷VIP激活动画（无矩形版）
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * 全新VIP激活动画 - 炫酷粒子爆炸效果
 */
@Composable
fun VipActivationAnimation(
    vipLevel: VipLevel,
    coins: Int,
    onDismiss: () -> Unit
) {
    var animationPhase by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        delay(100)
        animationPhase = 1
        delay(1500)
        animationPhase = 2
        delay(1500)
        animationPhase = 3
        delay(3000)
        animationPhase = 4
        delay(1000)
        onDismiss()
    }
    
    val vipColor = when (vipLevel) {
        VipLevel.VIP3 -> Color(0xFFFFD700)
        VipLevel.VIP2 -> Color(0xFF00D9FF)
        VipLevel.VIP1 -> Color(0xFFFFB800)
        else -> Color(0xFFFFD700)
    }
    
    val vipEmoji = when (vipLevel) {
        VipLevel.VIP3 -> "👑"
        VipLevel.VIP2 -> "💎"
        VipLevel.VIP1 -> "⭐"
        else -> "⭐"
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .zIndex(1000f),
        contentAlignment = Alignment.Center
    ) {
        // 粒子爆炸效果（增强版）
        if (animationPhase >= 1) {
            ParticleExplosion(vipColor)
            // 新增：第二层粒子
            SecondaryParticles(vipColor)
        }
        
        // 新增：波纹扩散效果
        if (animationPhase >= 1) {
            RippleWaves(vipColor)
        }
        
        // 新增：旋转光线
        if (animationPhase >= 1) {
            RotatingRays(vipColor)
        }
        
        // 新增：螺旋粒子流
        if (animationPhase >= 1) {
            SpiralParticleStream(vipColor)
        }
        
        // 新增：能量脉冲环
        if (animationPhase >= 1) {
            EnergyPulseRings(vipColor)
        }
        
        // 新增：闪电效果
        if (animationPhase >= 1) {
            LightningBolts(vipColor)
        }
        
        // 新增：流星雨
        if (animationPhase >= 2) {
            ShootingStars(vipColor)
        }
        
        // 金币雨
        if (animationPhase >= 3) {
            CoinRain()
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            AnimatedVisibility(
                visible = animationPhase >= 1,
                enter = scaleIn(
                    initialScale = 0.3f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                Box {
                    VipEmojiAnimation(vipEmoji, vipColor)
                    // 新增：星星环绕
                    if (animationPhase >= 1) {
                        SparkleStars(vipColor)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            AnimatedVisibility(
                visible = animationPhase >= 2,
                enter = fadeIn(animationSpec = tween(800)) + 
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎉 恭喜您 🎉",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Text(
                        text = vipLevel.displayName,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = vipColor,
                        modifier = Modifier.drawBehind {
                            // 增强发光效果
                            repeat(3) { layer ->
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            vipColor.copy(alpha = 0.4f - layer * 0.1f),
                                            Color.Transparent
                                        ),
                                        radius = 150f + layer * 50f
                                    ),
                                    radius = 150f + layer * 50f
                                )
                            }
                        }
                    )
                    
                    Text(
                        text = "已激活",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            AnimatedVisibility(
                visible = animationPhase >= 3,
                enter = scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                CoinReward(coins)
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VipEmojiAnimation(emoji: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "emoji")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // 新增：3D旋转效果
    val rotationYValue by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationY"
    )
    
    Box(
        modifier = Modifier
            .size(250.dp)
            .graphicsLayer {
                rotationZ = rotation
                rotationY = rotationYValue
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // 增强光晕效果 - 多层渐变
                repeat(8) { layer ->
                    val radius = (80f + layer * 25f) * scale
                    val alpha = (0.5f - layer * 0.06f) * scale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = alpha),
                                color.copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            ),
                            radius = radius
                        ),
                        radius = radius
                    )
                }
                
                // 新增：脉冲光环
                val pulseRadius = 120f * scale
                drawCircle(
                    color = color.copy(alpha = 0.3f * scale),
                    radius = pulseRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 180.sp,
            color = Color.Unspecified,
            modifier = Modifier.drawBehind {
                // 为emoji添加发光边缘
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        radius = 100f
                    ),
                    radius = 100f
                )
            }
        )
    }
}

@Composable
fun ParticleExplosion(color: Color) {
    val particles = remember {
        List(100) {
            ParticleData(
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 400f + 200f,
                size = Random.nextFloat() * 8f + 4f,
                delay = Random.nextInt(0, 300),
                color = color.copy(
                    red = (color.red + Random.nextFloat() * 0.2f - 0.1f).coerceIn(0f, 1f),
                    green = (color.green + Random.nextFloat() * 0.2f - 0.1f).coerceIn(0f, 1f),
                    blue = (color.blue + Random.nextFloat() * 0.2f - 0.1f).coerceIn(0f, 1f)
                )
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            AnimatedParticle(particle)
        }
    }
}

data class ParticleData(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val delay: Int,
    val color: Color
)

@Composable
fun AnimatedParticle(particle: ParticleData) {
    val infiniteTransition = rememberInfiniteTransition(label = "particle")
    
    val distance by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = particle.speed,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.delay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "distance"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                delayMillis = particle.delay,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    val angleRad = particle.angle * (PI / 180f).toFloat()
    val offsetX = cos(angleRad) * distance
    val offsetY = sin(angleRad) * distance
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val centerX = size.width / 2 + offsetX
                val centerY = size.height / 2 + offsetY
                
                drawCircle(
                    color = particle.color.copy(alpha = alpha.coerceAtLeast(0f)),
                    radius = particle.size,
                    center = Offset(centerX, centerY)
                )
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            particle.color.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        radius = particle.size * 2
                    ),
                    radius = particle.size * 2,
                    center = Offset(centerX, centerY)
                )
            }
    )
}

@Composable
fun CoinRain() {
    val coins = remember {
        List(30) {
            CoinData(
                startX = Random.nextFloat(),
                delay = Random.nextInt(0, 2000),
                duration = Random.nextInt(2000, 3500),
                size = Random.nextInt(24, 40)
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        coins.forEach { coin ->
            FallingCoin(coin)
        }
    }
}

data class CoinData(
    val startX: Float,
    val delay: Int,
    val duration: Int,
    val size: Int
)

@Composable
fun FallingCoin(coin: CoinData) {
    val infiniteTransition = rememberInfiniteTransition(label = "coin")
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = coin.duration,
                delayMillis = coin.delay,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = coin.duration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val x = coin.startX * size.width
                val y = offsetY
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = coin.size.toFloat() + 10f
                    ),
                    radius = coin.size.toFloat() + 10f,
                    center = Offset(x, y)
                )
            }
    ) {
        Text(
            text = "🪙",
            fontSize = coin.size.sp,
            modifier = Modifier
                .offset(
                    x = (coin.startX * 350).dp,
                    y = offsetY.dp
                )
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
    }
}

@Composable
fun CoinReward(coins: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "reward")
    
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = bounce.dp)
    ) {
        Text(
            text = "🪙",
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "赠送金币",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Text(
            text = "+$coins",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFD700),
            modifier = Modifier.drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        radius = 150f
                    ),
                    radius = 150f
                )
            }
        )
    }
}


/**
 * 星星闪烁环绕效果
 */
@Composable
fun SparkleStars(color: Color) {
    val starCount = 12
    
    repeat(starCount) { index ->
        val infiniteTransition = rememberInfiniteTransition(label = "star$index")
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1500
                    0.3f at 0
                    1f at (index * 125) % 1500
                    0.3f at 1500
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        val angle = (index * 360f / starCount + rotation) * (PI / 180f).toFloat()
        val radius = 140f
        val offsetX = cos(angle) * radius
        val offsetY = sin(angle) * radius
        
        Box(
            modifier = Modifier
                .size(250.dp)
                .drawBehind {
                    val centerX = size.width / 2 + offsetX
                    val centerY = size.height / 2 + offsetY
                    
                    // 星星光点
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = 6f,
                        center = Offset(centerX, centerY)
                    )
                    
                    // 十字光芒
                    drawLine(
                        color = color.copy(alpha = alpha * 0.8f),
                        start = Offset(centerX - 10f, centerY),
                        end = Offset(centerX + 10f, centerY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = color.copy(alpha = alpha * 0.8f),
                        start = Offset(centerX, centerY - 10f),
                        end = Offset(centerX, centerY + 10f),
                        strokeWidth = 2f
                    )
                }
        )
    }
}

/**
 * 旋转光线效果
 */
@Composable
fun RotatingRays(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "rays")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val rayCount = 16
                
                repeat(rayCount) { index ->
                    val angle = ((index * 360f / rayCount) + rotation) * (PI / 180f).toFloat()
                    val length = 300f
                    
                    val endX = centerX + cos(angle) * length
                    val endY = centerY + sin(angle) * length
                    
                    // 绘制光线
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = 0.6f),
                                color.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }
            }
    )
}

/**
 * 波纹扩散效果
 */
@Composable
fun RippleWaves(color: Color) {
    repeat(3) { waveIndex ->
        val infiniteTransition = rememberInfiniteTransition(label = "wave$waveIndex")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = waveIndex * 600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    delayMillis = waveIndex * 600,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val radius = 150f * scale
                    
                    // 绘制波纹圆环
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = alpha * 0.5f),
                                color.copy(alpha = alpha * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                }
        )
    }
}

/**
 * 第二层粒子效果（更小更密集）
 */
@Composable
fun SecondaryParticles(color: Color) {
    val particles = remember {
        List(60) {
            ParticleData(
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 250f + 150f,
                size = Random.nextFloat() * 4f + 2f,
                delay = Random.nextInt(200, 800),
                color = color.copy(
                    alpha = Random.nextFloat() * 0.5f + 0.3f
                )
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            AnimatedParticle(particle)
        }
    }
}

/**
 * 螺旋彩虹光束效果（带拖尾线条）
 */
@Composable
fun SpiralParticleStream(color: Color) {
    val beamCount = 6  // 6条光束
    
    // 彩虹颜色数组
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000), // 红
            Color(0xFFFF7F00), // 橙
            Color(0xFFFFFF00), // 黄
            Color(0xFF00FF00), // 绿
            Color(0xFF00FFFF), // 青
            Color(0xFF0000FF), // 蓝
            Color(0xFF8B00FF)  // 紫
        )
    }
    
    repeat(beamCount) { beamIndex ->
        val infiniteTransition = rememberInfiniteTransition(label = "beam$beamIndex")
        
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2500,
                    delayMillis = beamIndex * 400,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (progress < 0.05f) return@drawBehind
                    
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 螺旋轨迹参数
                    val spiralTurns = 2f
                    val maxRadius = 450f
                    
                    // 计算拖尾长度（光束的尾巴）
                    val trailLength = 0.25f  // 拖尾占总进度的25%
                    val trailStart = (progress - trailLength).coerceAtLeast(0f)
                    
                    // 绘制光束拖尾 - 使用连续的线段
                    val segments = 50  // 线段数量，越多越平滑
                    
                    for (i in 0 until segments) {
                        val t1 = trailStart + (progress - trailStart) * (i.toFloat() / segments)
                        val t2 = trailStart + (progress - trailStart) * ((i + 1).toFloat() / segments)
                        
                        if (t1 <= 0f) continue
                        
                        // 计算起点和终点
                        val angle1 = (beamIndex * 60f + t1 * spiralTurns * 360f) * (PI / 180f).toFloat()
                        val radius1 = t1 * maxRadius
                        val x1 = centerX + cos(angle1) * radius1
                        val y1 = centerY + sin(angle1) * radius1
                        
                        val angle2 = (beamIndex * 60f + t2 * spiralTurns * 360f) * (PI / 180f).toFloat()
                        val radius2 = t2 * maxRadius
                        val x2 = centerX + cos(angle2) * radius2
                        val y2 = centerY + sin(angle2) * radius2
                        
                        // 计算当前段的彩虹颜色
                        val colorProgress = (t1 + t2) / 2f
                        val colorIndex = ((colorProgress * rainbowColors.size * 1.5f) % rainbowColors.size).toInt()
                        val beamColor = rainbowColors[colorIndex]
                        
                        // 透明度：从尾部到头部渐亮
                        val segmentPosition = i.toFloat() / segments
                        val alpha = (segmentPosition * 0.7f + 0.3f) * (1f - progress * 0.3f)
                        
                        // 光束宽度：头部粗，尾部细
                        val strokeWidth = (4f + segmentPosition * 8f).coerceIn(3f, 12f)
                        
                        // 绘制光束主线
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    beamColor.copy(alpha = alpha * 0.8f),
                                    beamColor.copy(alpha = alpha)
                                ),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2)
                            ),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                        
                        // 绘制外发光层
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    beamColor.copy(alpha = alpha * 0.3f),
                                    beamColor.copy(alpha = alpha * 0.4f)
                                ),
                                start = Offset(x1, y1),
                                end = Offset(x2, y2)
                            ),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = strokeWidth * 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                    
                    // 绘制光束头部亮点
                    val headAngle = (beamIndex * 60f + progress * spiralTurns * 360f) * (PI / 180f).toFloat()
                    val headRadius = progress * maxRadius
                    val headX = centerX + cos(headAngle) * headRadius
                    val headY = centerY + sin(headAngle) * headRadius
                    
                    val headColorIndex = ((progress * rainbowColors.size * 1.5f) % rainbowColors.size).toInt()
                    val headColor = rainbowColors[headColorIndex]
                    
                    // 头部白色核心
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - progress) * 0.9f),
                        radius = 8f,
                        center = Offset(headX, headY)
                    )
                    
                    // 头部彩色光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                headColor.copy(alpha = (1f - progress) * 0.8f),
                                headColor.copy(alpha = (1f - progress) * 0.4f),
                                Color.Transparent
                            ),
                            radius = 30f
                        ),
                        radius = 30f,
                        center = Offset(headX, headY)
                    )
                }
        )
    }
}

/**
 * 能量脉冲环效果
 */
@Composable
fun EnergyPulseRings(color: Color) {
    repeat(5) { ringIndex ->
        val infiniteTransition = rememberInfiniteTransition(label = "pulse$ringIndex")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1200,
                    delayMillis = ringIndex * 240,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1200,
                    delayMillis = ringIndex * 240,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val baseRadius = 100f + ringIndex * 30f
                    val radius = baseRadius * scale
                    
                    // 绘制能量环
                    drawCircle(
                        color = color.copy(alpha = alpha * 0.3f),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                    
                    // 外发光
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = alpha * 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = radius + 10f
                        ),
                        radius = radius + 10f,
                        center = Offset(centerX, centerY)
                    )
                }
        )
    }
}

/**
 * 闪电效果
 */
@Composable
fun LightningBolts(color: Color) {
    val boltCount = 8
    
    repeat(boltCount) { index ->
        val infiniteTransition = rememberInfiniteTransition(label = "lightning$index")
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3000
                    0f at 0
                    1f at (index * 375) % 3000
                    0.8f at ((index * 375) + 50) % 3000
                    0f at ((index * 375) + 150) % 3000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )
        
        val angle = (index * 360f / boltCount) * (PI / 180f).toFloat()
        val length = 200f
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 主闪电
                    val endX = centerX + cos(angle) * length
                    val endY = centerY + sin(angle) * length
                    
                    // 绘制闪电主干
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = alpha),
                                color.copy(alpha = alpha * 0.5f),
                                Color.Transparent
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 4f
                    )
                    
                    // 闪电分支
                    val branchAngle1 = angle + 0.5f
                    val branchLength = length * 0.4f
                    val branchStartX = centerX + cos(angle) * (length * 0.6f)
                    val branchStartY = centerY + sin(angle) * (length * 0.6f)
                    val branchEndX = branchStartX + cos(branchAngle1) * branchLength
                    val branchEndY = branchStartY + sin(branchAngle1) * branchLength
                    
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = alpha * 0.7f),
                                Color.Transparent
                            ),
                            start = Offset(branchStartX, branchStartY),
                            end = Offset(branchEndX, branchEndY)
                        ),
                        start = Offset(branchStartX, branchStartY),
                        end = Offset(branchEndX, branchEndY),
                        strokeWidth = 2f
                    )
                }
        )
    }
}

/**
 * 流星雨效果
 */
@Composable
fun ShootingStars(color: Color) {
    val starCount = 15
    
    repeat(starCount) { index ->
        val infiniteTransition = rememberInfiniteTransition(label = "shooting$index")
        
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    delayMillis = index * 200,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        
        val startAngle = Random.nextFloat() * 360f
        val angle = startAngle * (PI / 180f).toFloat()
        val distance = progress * 400f
        val startDistance = -50f
        
        val startX = cos(angle) * startDistance
        val startY = sin(angle) * startDistance
        val endX = cos(angle) * distance
        val endY = sin(angle) * distance
        
        val alpha = if (progress < 0.2f) {
            progress / 0.2f
        } else if (progress > 0.8f) {
            (1f - progress) / 0.2f
        } else {
            1f
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    val currentX = centerX + endX
                    val currentY = centerY + endY
                    val tailX = centerX + startX + (endX - startX) * 0.7f
                    val tailY = centerY + startY + (endY - startY) * 0.7f
                    
                    // 流星尾迹
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = alpha * 0.6f),
                                color.copy(alpha = alpha)
                            ),
                            start = Offset(tailX, tailY),
                            end = Offset(currentX, currentY)
                        ),
                        start = Offset(tailX, tailY),
                        end = Offset(currentX, currentY),
                        strokeWidth = 3f
                    )
                    
                    // 流星头部
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 4f,
                        center = Offset(currentX, currentY)
                    )
                    
                    // 流星光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = alpha * 0.8f),
                                Color.Transparent
                            ),
                            radius = 12f
                        ),
                        radius = 12f,
                        center = Offset(currentX, currentY)
                    )
                }
        )
    }
}
