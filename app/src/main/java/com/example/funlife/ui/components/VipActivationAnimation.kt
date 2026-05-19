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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.funlife.data.model.VipLevel
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * VIP 激活动画分发器 — 不同等级有截然不同的主题视觉风格
 *
 * - VIP1 ⭐ 金色星辰：暖金粒子 + 上升五角星 + 金光环
 * - VIP2 💎 冰晶蓝钻：青蓝钻石碎片爆裂 + 闪电弧 + 冷光柱
 * - VIP3 👑 紫金王冠：超新星开场闪 + 神圣几何曼陀罗 + 紫金双色风暴
 */
@Composable
fun VipActivationAnimation(
    vipLevel: VipLevel,
    coins: Int,
    onDismiss: () -> Unit
) {
    val spec = remember(vipLevel) {
        when (vipLevel) {
            VipLevel.VIP1 -> VipAnimSpec(
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFFF8C00),
                accentColor = Color(0xFFFFE57F),
                emoji = "⭐",
                bgInner = Color(0xFF2A1F00),
                showRisingStars = true,
                showAuroraRibbon = true, // 🆕 金色极光丝带
                showConfetti = true,     // 🆕 金色纸屑
                showSpiralStream = false,
                showShootingStars = false,
                titleGradient = listOf(
                    Color(0xFFFFE57F), Color(0xFFFFD700), Color(0xFFFF8C00)
                )
            )
            VipLevel.VIP2 -> VipAnimSpec(
                primaryColor = Color(0xFF00D9FF),
                secondaryColor = Color(0xFF7C4DFF),
                accentColor = Color(0xFF80DEEA),
                emoji = "💎",
                bgInner = Color(0xFF001A2E),
                showCrystalShards = true,
                showLightPillars = true, // 🆕 垂直光柱
                showConfetti = true,     // 🆕 蓝紫纸屑
                showSpiralStream = false,
                titleGradient = listOf(
                    Color(0xFF00FFFF), Color(0xFF00D9FF), Color(0xFF7C4DFF)
                )
            )
            else -> VipAnimSpec( // VIP3 / PERMANENT
                primaryColor = Color(0xFFFFD700),
                secondaryColor = Color(0xFFE040FB),
                accentColor = Color(0xFFFFFFFF),
                emoji = if (vipLevel == VipLevel.PERMANENT) "🌟" else "👑",
                bgInner = Color(0xFF1A0033),
                showRoyalMandala = true,
                showSupernova = true,
                showDualParticles = true,
                showBurstRays = true,    // 🆕 24 道太阳放射光线
                showConfetti = true,     // 🆕 紫金纸屑
                showScreenShake = true,  // 🆕 屏幕震动
                titleGradient = listOf(
                    Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFFE040FB), Color(0xFFFFD700)
                )
            )
        }
    }

    var animationPhase by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(100); animationPhase = 1
        delay(1500); animationPhase = 2
        delay(1500); animationPhase = 3
        delay(3000); animationPhase = 4
        delay(1000); onDismiss()
    }

    // ─ VIP3 屏幕震动（前 1500ms 强烈，之后衰减为微震） ─
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val shakeX by shakeTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sx"
    )
    val shakeY by shakeTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(70, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "sy"
    )
    val shakeAmplitude = if (spec.showScreenShake) {
        when (animationPhase) {
            1 -> 1f
            2 -> 0.4f
            else -> 0f
        }
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(spec.bgInner, Color.Black.copy(alpha = 0.97f)),
                    radius = 1500f
                )
            )
            .zIndex(1000f)
            .graphicsLayer {
                translationX = shakeX * shakeAmplitude
                translationY = shakeY * shakeAmplitude
            },
        contentAlignment = Alignment.Center
    ) {
        // ─ VIP3 专属：超新星开场闪光 ─
        if (spec.showSupernova && animationPhase >= 1) {
            SupernovaFlash(spec.accentColor)
        }

        // ─ VIP3 专属：太阳放射光线爆发 ─
        if (spec.showBurstRays && animationPhase >= 1) {
            BurstRays(spec.primaryColor, spec.secondaryColor)
        }

        // ─ VIP1 专属：金色极光丝带 ─
        if (spec.showAuroraRibbon && animationPhase >= 1) {
            AuroraRibbon(spec.primaryColor, spec.accentColor)
        }

        // ─ VIP2 专属：垂直光柱 ─
        if (spec.showLightPillars && animationPhase >= 1) {
            LightPillars(spec.primaryColor, spec.secondaryColor)
        }

        // ─ 通用粒子层 ─
        if (animationPhase >= 1) {
            ParticleExplosion(spec.primaryColor)
            SecondaryParticles(spec.secondaryColor)
            if (spec.showDualParticles) {
                SecondaryParticles(spec.primaryColor)
            }
            RippleWaves(spec.primaryColor)
            RotatingRays(spec.primaryColor)
            EnergyPulseRings(spec.primaryColor)
            if (spec.showLightning) LightningBolts(spec.primaryColor)
            if (spec.showSpiralStream) SpiralParticleStream(spec.primaryColor)
        }

        // ─ 主题专属粒子 ─
        if (spec.showRisingStars && animationPhase >= 1) {
            RisingStarsField(spec.primaryColor, spec.accentColor)
        }
        if (spec.showCrystalShards && animationPhase >= 1) {
            CrystalShards(spec.primaryColor, spec.secondaryColor)
        }
        if (spec.showRoyalMandala && animationPhase >= 1) {
            RoyalMandala(spec.primaryColor, spec.secondaryColor)
        }

        if (spec.showShootingStars && animationPhase >= 2) {
            ShootingStars(spec.primaryColor)
        }
        // 五彩纸屑（phase 2 起始）
        if (spec.showConfetti && animationPhase >= 2) {
            ConfettiRain(spec.primaryColor, spec.secondaryColor)
        }
        if (animationPhase >= 3) CoinRain()

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
                    VipEmojiAnimation(spec.emoji, spec.primaryColor)
                    if (animationPhase >= 1) SparkleStars(spec.primaryColor)
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
                    // 等级标题：使用主题色渐变（VIP3 三色，VIP1/2 双色）
                    Text(
                        text = vipLevel.displayName,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(
                            brush = Brush.linearGradient(spec.titleGradient)
                        ),
                        modifier = Modifier.drawBehind {
                            repeat(4) { layer ->
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            spec.primaryColor.copy(alpha = 0.45f - layer * 0.1f),
                                            spec.secondaryColor.copy(alpha = 0.2f - layer * 0.05f),
                                            Color.Transparent
                                        ),
                                        radius = 160f + layer * 55f
                                    ),
                                    radius = 160f + layer * 55f
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

private data class VipAnimSpec(
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val emoji: String,
    val bgInner: Color,
    val showRisingStars: Boolean = false,
    val showCrystalShards: Boolean = false,
    val showRoyalMandala: Boolean = false,
    val showSupernova: Boolean = false,
    val showDualParticles: Boolean = false,
    val showLightning: Boolean = true,
    val showSpiralStream: Boolean = true,
    val showShootingStars: Boolean = true,
    val showAuroraRibbon: Boolean = false,
    val showLightPillars: Boolean = false,
    val showBurstRays: Boolean = false,
    val showConfetti: Boolean = false,
    val showScreenShake: Boolean = false,
    val titleGradient: List<Color>
)

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

// ════════════════════════════════════════════════════════════════
// 主题特效层（VIP1/VIP2/VIP3 各自独有）
// ════════════════════════════════════════════════════════════════

/**
 * VIP3 专属：超新星开场闪光（屏幕级白光爆裂 + 同心冲击波 + 太阳放射光线）
 * ⚠️ 注意：radius 必须 > 0，否则 Brush.radialGradient 会抛 IllegalArgumentException
 */
@Composable
fun SupernovaFlash(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "nova")
    val scale by transition.animateFloat(
        initialValue = 0.05f, targetValue = 14f, // ⚠️ 必须 > 0
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ns"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "na"
    )
    // 第二层冲击波（错相 600ms）
    val scale2 by transition.animateFloat(
        initialValue = 0.05f, targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ns2"
    )
    val alpha2 by transition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "na2"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            val cx = size.width / 2
            val cy = size.height / 2
            // ⚠️ coerceAtLeast 防止 radius=0 崩溃
            val r1 = (80f * scale).coerceAtLeast(1f)
            val r2 = (80f * scale2).coerceAtLeast(1f)

            // 第一波超新星
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        accentColor.copy(alpha = alpha * 0.8f),
                        accentColor.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    radius = r1
                ),
                radius = r1,
                center = Offset(cx, cy)
            )
            // 冲击波光环（白色细圆环）
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.6f),
                radius = r1,
                center = Offset(cx, cy),
                style = Stroke(width = (4f * (1f - alpha + 0.3f)).coerceAtLeast(1f))
            )
            // 第二波（错相）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha2 * 0.7f),
                        accentColor.copy(alpha = alpha2 * 0.5f),
                        Color.Transparent
                    ),
                    radius = r2
                ),
                radius = r2,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = accentColor.copy(alpha = alpha2 * 0.5f),
                radius = r2,
                center = Offset(cx, cy),
                style = Stroke(width = 3f)
            )
        }
    )
}

/**
 * 太阳放射光线爆发（24 道金色长光线从中心射出，VIP3 专属增强）
 */
@Composable
fun BurstRays(primaryColor: Color, secondaryColor: Color) {
    val transition = rememberInfiniteTransition(label = "burst")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "bp"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "br"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            val cx = size.width / 2
            val cy = size.height / 2
            val rayCount = 24
            val maxLen = (size.minDimension * 0.6f).coerceAtLeast(100f)
            val len = maxLen * progress
            val a = (1f - progress).coerceIn(0f, 1f)
            repeat(rayCount) { i ->
                val isAccent = i % 3 == 0
                val color = if (isAccent) secondaryColor else primaryColor
                val angle = (i * 360f / rayCount + rotation) * (PI / 180f).toFloat()
                val sx = cx + cos(angle) * 60f
                val sy = cy + sin(angle) * 60f
                val ex = cx + cos(angle) * (60f + len)
                val ey = cy + sin(angle) * (60f + len)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = a * 0.9f),
                            color.copy(alpha = a * 0.4f),
                            Color.Transparent
                        ),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey)
                    ),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = if (isAccent) 4f else 2f,
                    cap = StrokeCap.Round
                )
            }
        }
    )
}

/**
 * VIP1 专属：极光丝带（顶部缓慢摇曳的金色波浪带）
 */
@Composable
fun AuroraRibbon(primaryColor: Color, accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ap"
    )
    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            val w = size.width
            val h = size.height
            // 上方丝带
            for (band in 0..2) {
                val baseY = h * 0.18f + band * 18f
                val amp = 32f - band * 6f
                val path = Path()
                val steps = 60
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val y = baseY + sin(t * 4f * PI.toFloat() + phase + band * 0.6f) * amp
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val color = if (band == 0) accentColor else primaryColor
                drawPath(
                    path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = 0.55f - band * 0.12f),
                            color.copy(alpha = 0.7f - band * 0.15f),
                            color.copy(alpha = 0.55f - band * 0.12f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 6f - band * 1.4f, cap = StrokeCap.Round)
                )
            }
            // 下方丝带（反向）
            for (band in 0..1) {
                val baseY = h * 0.78f - band * 16f
                val amp = 28f - band * 6f
                val path = Path()
                val steps = 60
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val x = t * w
                    val y = baseY + sin(t * 3.5f * PI.toFloat() - phase + band * 0.8f) * amp
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.45f),
                            accentColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
            }
        }
    )
}

/**
 * VIP2 专属：垂直光柱（从屏幕底部向上升起的青蓝光束）
 */
@Composable
fun LightPillars(primaryColor: Color, secondaryColor: Color) {
    data class PillarData(val xRel: Float, val delay: Int, val width: Float, val useSecondary: Boolean)
    val pillars = remember {
        List(8) { idx ->
            PillarData(
                xRel = (idx + 0.5f) / 8f + (Random.nextFloat() - 0.5f) * 0.05f,
                delay = idx * 180,
                width = Random.nextFloat() * 28f + 18f,
                useSecondary = idx % 2 == 1
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "pillars")
    Box(modifier = Modifier.fillMaxSize()) {
        pillars.forEachIndexed { idx, p ->
            val rise by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, delayMillis = p.delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "lpr$idx"
            )
            val color = if (p.useSecondary) secondaryColor else primaryColor
            Box(modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = p.xRel * size.width
                    val height = size.height * rise * 0.85f
                    val topY = size.height - height
                    val a = (1f - rise).coerceIn(0f, 1f) *
                            if (rise < 0.15f) rise / 0.15f else 1f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                color.copy(alpha = a * 0.4f),
                                Color.White.copy(alpha = a * 0.7f)
                            ),
                            startY = topY,
                            endY = size.height
                        ),
                        topLeft = Offset(cx - p.width / 2, topY),
                        size = androidx.compose.ui.geometry.Size(p.width, height)
                    )
                    // 中心亮线
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = a * 0.95f)
                            ),
                            startY = topY,
                            endY = size.height
                        ),
                        start = Offset(cx, topY),
                        end = Offset(cx, size.height),
                        strokeWidth = 2.5f
                    )
                }
            )
        }
    }
}

/**
 * 五彩纸屑雨（phase 3 增强，所有 VIP 通用，参数控制色调）
 */
@Composable
fun ConfettiRain(primaryColor: Color, secondaryColor: Color) {
    data class Confetti(
        val xRel: Float,
        val delay: Int,
        val duration: Int,
        val size: Float,
        val rotSpeed: Float,
        val swayAmp: Float,
        val color: Color
    )
    val palette = remember(primaryColor, secondaryColor) {
        listOf(
            primaryColor,
            secondaryColor,
            Color.White,
            primaryColor.copy(alpha = 0.85f),
            secondaryColor.copy(alpha = 0.85f)
        )
    }
    val confetti = remember {
        List(40) {
            Confetti(
                xRel = Random.nextFloat(),
                delay = Random.nextInt(0, 2500),
                duration = Random.nextInt(2800, 4500),
                size = Random.nextFloat() * 8f + 6f,
                rotSpeed = Random.nextFloat() * 720f + 360f,
                swayAmp = Random.nextFloat() * 50f + 20f,
                color = palette[Random.nextInt(palette.size)]
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    Box(modifier = Modifier.fillMaxSize()) {
        confetti.forEachIndexed { idx, c ->
            val progress by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(c.duration, delayMillis = c.delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "cfp$idx"
            )
            val rotation by transition.animateFloat(
                initialValue = 0f, targetValue = c.rotSpeed,
                animationSpec = infiniteRepeatable(
                    animation = tween(c.duration, delayMillis = c.delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "cfr$idx"
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val baseX = c.xRel * size.width
                    val cx = baseX + sin(progress * 4f * PI.toFloat()) * c.swayAmp
                    val cy = -50f + progress * (size.height + 100f)
                    val a = if (progress < 0.05f) progress * 20f
                            else if (progress > 0.92f) (1f - progress) / 0.08f
                            else 1f
                    val rad = rotation * (PI / 180f).toFloat()
                    val cosR = cos(rad)
                    val sinR = sin(rad)
                    // 矩形纸片（用 4 顶点 path）
                    val w = c.size
                    val h = c.size * 0.5f
                    fun rotated(x: Float, y: Float) =
                        Offset(cx + x * cosR - y * sinR, cy + x * sinR + y * cosR)
                    val path = Path().apply {
                        val tl = rotated(-w, -h); val tr = rotated(w, -h)
                        val br = rotated(w, h); val bl = rotated(-w, h)
                        moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
                        lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
                    }
                    drawPath(path, c.color.copy(alpha = a.coerceIn(0f, 1f)))
                }
            )
        }
    }
}


/**
 * VIP1 专属：上升的金色五角星（从底部缓缓升起，伴随旋转闪烁）
 */
@Composable
fun RisingStarsField(primaryColor: Color, accentColor: Color) {
    data class StarData(
        val xRel: Float,
        val delay: Int,
        val duration: Int,
        val sizeBase: Float,
        val isAccent: Boolean
    )
    val stars = remember {
        List(18) {
            StarData(
                xRel = Random.nextFloat(),
                delay = Random.nextInt(0, 2200),
                duration = Random.nextInt(2800, 4800),
                sizeBase = Random.nextFloat() * 12f + 12f,
                isAccent = Random.nextBoolean()
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "rstars")
    Box(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { idx, s ->
            val progress by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(s.duration, delayMillis = s.delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "rsp$idx"
            )
            val rotation by transition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(s.duration, delayMillis = s.delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "rsr$idx"
            )
            val color = if (s.isAccent) accentColor else primaryColor
            Box(modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cx = s.xRel * size.width
                    val cy = size.height * (1.05f - progress * 1.1f)
                    val a = when {
                        progress < 0.1f -> progress * 10f
                        progress > 0.85f -> ((1f - progress) / 0.15f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                    // 外发光
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = a * 0.6f),
                                Color.Transparent
                            ),
                            radius = s.sizeBase * 2.2f
                        ),
                        radius = s.sizeBase * 2.2f,
                        center = Offset(cx, cy)
                    )
                    drawFiveStar(Offset(cx, cy), s.sizeBase, color.copy(alpha = a), rotation)
                    // 中心白核
                    drawCircle(
                        color = Color.White.copy(alpha = a * 0.9f),
                        radius = s.sizeBase * 0.18f,
                        center = Offset(cx, cy)
                    )
                }
            )
        }
    }
}

private fun DrawScope.drawFiveStar(center: Offset, radius: Float, color: Color, rotation: Float) {
    val path = Path()
    for (i in 0 until 10) {
        val baseAngle = -90f + i * 36f + rotation
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val rad = baseAngle * (PI / 180f).toFloat()
        val px = center.x + cos(rad) * r
        val py = center.y + sin(rad) * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
    // 描边亮光
    drawPath(path, Color.White.copy(alpha = color.alpha * 0.6f), style = Stroke(width = 1.2f))
}

/**
 * VIP2 专属：水晶钻石碎片爆裂（菱形碎片从中心向外飞射，带旋转和高光）
 */
@Composable
fun CrystalShards(primaryColor: Color, secondaryColor: Color) {
    data class ShardData(
        val angle: Float,
        val speed: Float,
        val size: Float,
        val rotationStart: Float,
        val delay: Int,
        val useSecondary: Boolean
    )
    val shards = remember {
        List(28) { idx ->
            ShardData(
                angle = idx * (360f / 28f) + Random.nextFloat() * 12f,
                speed = Random.nextFloat() * 380f + 280f,
                size = Random.nextFloat() * 14f + 10f,
                rotationStart = Random.nextFloat() * 360f,
                delay = Random.nextInt(0, 600),
                useSecondary = idx % 3 == 0
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "crystal")
    Box(modifier = Modifier.fillMaxSize()) {
        shards.forEachIndexed { idx, s ->
            val progress by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2300, delayMillis = s.delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "csp$idx"
            )
            val rot by transition.animateFloat(
                initialValue = s.rotationStart,
                targetValue = s.rotationStart + 720f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2300, delayMillis = s.delay, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "csr$idx"
            )
            val color = if (s.useSecondary) secondaryColor else primaryColor
            Box(modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val rad = s.angle * (PI / 180f).toFloat()
                    val dist = progress * s.speed
                    val cx = size.width / 2 + cos(rad) * dist
                    val cy = size.height / 2 + sin(rad) * dist
                    val a = (1f - progress).coerceIn(0f, 1f) *
                            if (progress < 0.1f) progress * 10f else 1f
                    drawDiamond(Offset(cx, cy), s.size, color.copy(alpha = a), rot)
                }
            )
        }
    }
}

private fun DrawScope.drawDiamond(center: Offset, halfSize: Float, color: Color, rotation: Float) {
    val rad = rotation * (PI / 180f).toFloat()
    val cosR = cos(rad)
    val sinR = sin(rad)
    fun rotated(x: Float, y: Float): Offset =
        Offset(center.x + x * cosR - y * sinR, center.y + x * sinR + y * cosR)

    val top = rotated(0f, -halfSize)
    val right = rotated(halfSize * 0.55f, 0f)
    val bot = rotated(0f, halfSize)
    val left = rotated(-halfSize * 0.55f, 0f)

    // 主菱形填充
    val path = Path().apply {
        moveTo(top.x, top.y)
        lineTo(right.x, right.y)
        lineTo(bot.x, bot.y)
        lineTo(left.x, left.y)
        close()
    }
    drawPath(path, color)
    // 内部高光（左上半）
    val highlight = Path().apply {
        moveTo(top.x, top.y)
        lineTo(rotated(0f, -halfSize * 0.25f).x, rotated(0f, -halfSize * 0.25f).y)
        lineTo(left.x, left.y)
        close()
    }
    drawPath(highlight, Color.White.copy(alpha = color.alpha * 0.75f))
    // 描边
    drawPath(path, Color.White.copy(alpha = color.alpha * 0.5f), style = Stroke(width = 1f))
}

/**
 * VIP3 专属：神圣几何曼陀罗（双层六边形 + 六芒星 + 同心圆，旋转脉动）
 */
@Composable
fun RoyalMandala(primaryColor: Color, secondaryColor: Color) {
    val transition = rememberInfiniteTransition(label = "mandala")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "mr"
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "ms"
    )
    val secondRot by transition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "mr2"
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            val cx = size.width / 2
            val cy = size.height / 2
            val baseR = 230f * pulseScale

            // 外层同心圆光环
            repeat(3) { i ->
                drawCircle(
                    color = primaryColor.copy(alpha = 0.18f - i * 0.05f),
                    radius = baseR + i * 55f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4f)
                )
            }

            // 双层六边形（金色顺转 + 紫色逆转）
            for (ringIdx in 0..1) {
                val r = baseR + ringIdx * 35f
                val color = if (ringIdx == 0) primaryColor else secondaryColor
                val rot = if (ringIdx == 0) rotation else secondRot
                val path = Path()
                for (i in 0..6) {
                    val a = (i * 60f + rot) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * r
                    val y = cy + sin(a) * r
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    color = color.copy(alpha = 0.55f),
                    style = Stroke(width = 2.8f)
                )
            }

            // 六芒星（两个等边三角形重叠）
            val starR = baseR * 0.72f
            for (triIdx in 0..1) {
                val color = if (triIdx == 0) primaryColor else secondaryColor
                val rotOffset = if (triIdx == 0) 0f else 60f
                val triRot = -rotation * 0.6f
                val path = Path()
                for (i in 0..3) {
                    val a = (i * 120f + rotOffset + triRot) * (PI / 180f).toFloat()
                    val x = cx + cos(a) * starR
                    val y = cy + sin(a) * starR
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    color = color.copy(alpha = 0.45f),
                    style = Stroke(width = 2.2f)
                )
            }

            // 中心放射 12 道短光线（沿曼陀罗中心向外）
            val innerR = baseR * 0.4f
            val outerR = baseR * 0.62f
            repeat(12) { i ->
                val a = (i * 30f + rotation * 1.2f) * (PI / 180f).toFloat()
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        start = Offset(cx + cos(a) * innerR, cy + sin(a) * innerR),
                        end = Offset(cx + cos(a) * outerR, cy + sin(a) * outerR)
                    ),
                    start = Offset(cx + cos(a) * innerR, cy + sin(a) * innerR),
                    end = Offset(cx + cos(a) * outerR, cy + sin(a) * outerR),
                    strokeWidth = 1.8f
                )
            }
        }
    )
}
