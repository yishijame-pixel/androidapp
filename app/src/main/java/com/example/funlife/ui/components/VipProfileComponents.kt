// VipProfileComponents.kt - VIP个人主页组件
package com.example.funlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.AvatarFrame
import com.example.funlife.data.model.ProfileBackground
import kotlinx.coroutines.delay

/**
 * VIP个人主页背景
 * 根据VIP等级和背景ID显示不同的背景效果
 */
@Composable
fun VipProfileBackground(
    vipLevel: Int,
    background: ProfileBackground?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = parseGradientColors(background?.gradientColors ?: "#F5F5F5,#FFFFFF")
    val particleType = background?.particleType ?: "none"
    
    Box(modifier = modifier.fillMaxSize()) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = colors)
                )
        )
        
        // 粒子效果
        when (particleType) {
            "gold" -> GoldParticleEffect()
            "diamond" -> DiamondParticleEffect()
            "crown" -> CrownParticleEffect()
            "star" -> StarParticleEffect()
            "meteor" -> MeteorEffect()
            "flame" -> FlameParticleEffect()
            "lightning" -> LightningEffect()
            "rainbow" -> RainbowParticleEffect()
        }
        
        // 内容
        content()
    }
}

/**
 * 解析渐变色字符串
 */
private fun parseGradientColors(colorString: String): List<Color> {
    return colorString.split(",").mapNotNull { colorHex ->
        try {
            Color(android.graphics.Color.parseColor(colorHex.trim()))
        } catch (e: Exception) {
            null
        }
    }.ifEmpty { listOf(Color(0xFFF5F5F5), Color.White) }
}

/**
 * 金色粒子效果（VIP1）
 */
@Composable
fun GoldParticleEffect() {
    val particles = remember { (1..20).map { ParticleState() } }
    
    particles.forEach { particle ->
        var offsetY by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(0.5f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = tween(
                        durationMillis = (5000..8000).random(),
                        easing = LinearEasing
                    )
                ) { value, _ ->
                    offsetY = value
                    alpha = (0.3f + 0.5f * (1 - value / 1000f)).coerceIn(0f, 0.8f)
                }
                offsetY = 0f
            }
        }
        
        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = offsetY.dp)
                .size((2..4).random().dp)
                .alpha(alpha)
                .background(Color(0xFFFFD700), CircleShape)
        )
    }
}

/**
 * 钻石粒子效果（VIP2）
 */
@Composable
fun DiamondParticleEffect() {
    val particles = remember { (1..30).map { ParticleState() } }
    
    particles.forEach { particle ->
        var offsetY by remember { mutableStateOf(0f) }
        var rotation by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(0.5f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = tween(
                        durationMillis = (4000..7000).random(),
                        easing = LinearEasing
                    )
                ) { value, _ ->
                    offsetY = value
                    rotation = value * 360f / 1000f
                    alpha = (0.3f + 0.5f * (1 - value / 1000f)).coerceIn(0f, 0.8f)
                }
                offsetY = 0f
                rotation = 0f
            }
        }
        
        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = offsetY.dp)
                .size(6.dp)
                .rotate(rotation)
                .alpha(alpha)
                .background(Color(0xFF00BCD4), RoundedCornerShape(2.dp))
        )
    }
}

/**
 * 皇冠粒子效果（VIP3）
 */
@Composable
fun CrownParticleEffect() {
    val particles = remember { (1..40).map { ParticleState() } }
    
    particles.forEach { particle ->
        var offsetY by remember { mutableStateOf(0f) }
        var scale by remember { mutableStateOf(1f) }
        var alpha by remember { mutableStateOf(0.6f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = tween(
                        durationMillis = (3000..6000).random(),
                        easing = LinearEasing
                    )
                ) { value, _ ->
                    offsetY = value
                    scale = 0.5f + 0.5f * (1 - value / 1000f)
                    alpha = (0.4f + 0.4f * (1 - value / 1000f)).coerceIn(0f, 0.8f)
                }
                offsetY = 0f
            }
        }
        
        Text(
            text = "👑",
            fontSize = 12.sp,
            modifier = Modifier
                .offset(x = particle.x.dp, y = offsetY.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}

/**
 * 星星粒子效果
 */
@Composable
fun StarParticleEffect() {
    val particles = remember { (1..25).map { ParticleState() } }
    
    particles.forEach { particle ->
        var alpha by remember { mutableStateOf(0.5f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0.2f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = (1000..2000).random(),
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    )
                ) { value, _ ->
                    alpha = value
                }
            }
        }
        
        Text(
            text = "⭐",
            fontSize = 10.sp,
            modifier = Modifier
                .offset(x = particle.x.dp, y = particle.y.dp)
                .alpha(alpha)
        )
    }
}

/**
 * 流星效果（VIP3专属）
 */
@Composable
fun MeteorEffect() {
    var showMeteor by remember { mutableStateOf(false) }
    var meteorX by remember { mutableStateOf(0f) }
    var meteorY by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000..7000).random().toLong())
            meteorX = (50..300).random().toFloat()
            meteorY = -50f
            showMeteor = true
            
            animate(
                initialValue = -50f,
                targetValue = 1000f,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = FastOutSlowInEasing
                )
            ) { value, _ ->
                meteorY = value
                meteorX += 2f
            }
            
            showMeteor = false
        }
    }
    
    if (showMeteor) {
        Box(
            modifier = Modifier
                .offset(x = meteorX.dp, y = meteorY.dp)
                .size(width = 40.dp, height = 2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFD700),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 火焰粒子效果
 */
@Composable
fun FlameParticleEffect() {
    val particles = remember { (1..30).map { ParticleState() } }
    
    particles.forEach { particle ->
        var offsetY by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(0.6f) }
        var scale by remember { mutableStateOf(1f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0f,
                    targetValue = 800f,
                    animationSpec = tween(
                        durationMillis = (2000..4000).random(),
                        easing = LinearEasing
                    )
                ) { value, _ ->
                    offsetY = value
                    alpha = (0.6f * (1 - value / 800f)).coerceIn(0f, 0.8f)
                    scale = 0.5f + 0.5f * (1 - value / 800f)
                }
                offsetY = 0f
            }
        }
        
        Text(
            text = "🔥",
            fontSize = 14.sp,
            modifier = Modifier
                .offset(x = particle.x.dp, y = offsetY.dp)
                .scale(scale)
                .alpha(alpha)
        )
    }
}

/**
 * 闪电效果
 */
@Composable
fun LightningEffect() {
    var showLightning by remember { mutableStateOf(false) }
    var lightningX by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay((2000..5000).random().toLong())
            lightningX = (50..300).random().toFloat()
            showLightning = true
            delay(200)
            showLightning = false
        }
    }
    
    if (showLightning) {
        Box(
            modifier = Modifier
                .offset(x = lightningX.dp, y = 0.dp)
                .size(width = 3.dp, height = 200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00BCD4),
                            Color(0xFF9C27B0),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * 彩虹粒子效果
 */
@Composable
fun RainbowParticleEffect() {
    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFFD93D),
        Color(0xFF6BCF7F),
        Color(0xFF4D96FF),
        Color(0xFF9B59B6)
    )
    
    val particles = remember { (1..35).map { ParticleState() } }
    
    particles.forEachIndexed { index, particle ->
        var offsetY by remember { mutableStateOf(0f) }
        var alpha by remember { mutableStateOf(0.5f) }
        
        LaunchedEffect(Unit) {
            while (true) {
                animate(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = tween(
                        durationMillis = (4000..7000).random(),
                        easing = LinearEasing
                    )
                ) { value, _ ->
                    offsetY = value
                    alpha = (0.3f + 0.5f * (1 - value / 1000f)).coerceIn(0f, 0.7f)
                }
                offsetY = 0f
            }
        }
        
        Box(
            modifier = Modifier
                .offset(x = particle.x.dp, y = offsetY.dp)
                .size((3..5).random().dp)
                .alpha(alpha)
                .background(colors[index % colors.size], CircleShape)
        )
    }
}

/**
 * 粒子状态
 */
private data class ParticleState(
    val x: Float = (0..400).random().toFloat(),
    val y: Float = (0..800).random().toFloat()
)

/**
 * VIP头像框
 * 根据头像框ID显示不同的边框效果
 * 即使是普通用户也会显示清晰的圆形边框
 */
@Composable
fun VipAvatarFrame(
    frame: AvatarFrame?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationType = frame?.animationType ?: "none"
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 根据动画类型显示不同效果
        when (animationType) {
            "glow" -> GlowFrameEffect()
            "rotate" -> RotateFrameEffect()
            "3d" -> ThreeDFrameEffect()
            "scan" -> ScanFrameEffect()
            "twinkle" -> TwinkleFrameEffect()
            "flow" -> FlowFrameEffect()
            "flame" -> FlameFrameEffect()
            "lightning" -> LightningFrameEffect()
            else -> {
                // 普通用户的默认边框 - 清晰的圆形边框
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .border(
                            width = 3.dp,
                            color = Color(0xFFE0E0E0),
                            shape = CircleShape
                        )
                )
            }
        }
        
        // 头像内容
        content()
    }
}

/**
 * 微光边框效果（VIP1）
 */
@Composable
fun GlowFrameEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .alpha(alpha)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )
}

/**
 * 旋转边框效果（VIP2）
 */
@Composable
fun RotateFrameEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(110.dp)
            .rotate(rotation)
            .border(
                width = 3.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00BCD4),
                        Color.Transparent,
                        Color(0xFF00BCD4)
                    )
                ),
                shape = CircleShape
            )
    )
}

/**
 * 3D旋转边框效果（VIP3）
 */
@Composable
fun ThreeDFrameEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "3d")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .rotate(rotation)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                CircleShape
            )
    )
}

/**
 * 扫描线边框效果
 */
@Composable
fun ScanFrameEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = offsetY.dp)
                .background(Color(0xFF00BCD4).copy(alpha = 0.6f))
        )
    }
}

/**
 * 闪烁边框效果
 */
@Composable
fun TwinkleFrameEffect() {
    val stars = remember { (1..8).map { it * 45f } }
    
    stars.forEach { angle ->
        var alpha by remember { mutableStateOf(0.5f) }
        
        LaunchedEffect(angle) {
            delay((angle * 10).toLong())
            while (true) {
                animate(
                    initialValue = 0.2f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                ) { value, _ ->
                    alpha = value
                }
            }
        }
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(angle),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(Color(0xFFFFD700), CircleShape)
            )
        }
    }
}

/**
 * 流动边框效果
 */
@Composable
fun FlowFrameEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "flow")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    Box(
        modifier = Modifier
            .size(110.dp)
            .border(
                width = 3.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF6B9D),
                        Color(0xFFFFD700),
                        Color(0xFFFF6B9D)
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        x = 0.5f + offset * 0.1f,
                        y = 0.5f + offset * 0.1f
                    )
                ),
                shape = CircleShape
            )
    )
}

/**
 * 火焰边框效果
 */
@Composable
fun FlameFrameEffect() {
    val flames = remember { (1..12).map { it * 30f } }
    
    flames.forEach { angle ->
        var scale by remember { mutableStateOf(1f) }
        var alpha by remember { mutableStateOf(0.6f) }
        
        LaunchedEffect(angle) {
            delay((angle * 5).toLong())
            while (true) {
                animate(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                ) { value, _ ->
                    scale = value
                    alpha = 0.8f - (value - 0.8f)
                }
            }
        }
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .rotate(angle),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6F00),
                                Color(0xFFFF3D00)
                            )
                        ),
                        CircleShape
                    )
            )
        }
    }
}

/**
 * 闪电边框效果
 */
@Composable
fun LightningFrameEffect() {
    var showLightning by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay((1000..3000).random().toLong())
            showLightning = true
            delay(100)
            showLightning = false
        }
    }
    
    if (showLightning) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .border(
                    width = 3.dp,
                    color = Color(0xFF00BCD4),
                    shape = CircleShape
                )
        )
    }
}

/**
 * VIP等级标识
 */
@Composable
fun VipLevelBadge(
    vipLevel: Int,
    modifier: Modifier = Modifier
) {
    val (icon, text, colors) = when (vipLevel) {
        1 -> Triple("⭐", "普通VIP", listOf(Color(0xFFFFD700), Color(0xFFFFA500)))
        2 -> Triple("💎", "年费VIP", listOf(Color(0xFF00BCD4), Color(0xFF0097A7)))
        3 -> Triple("👑", "终身VIP", listOf(Color(0xFFFF6B9D), Color(0xFFFFD700)))
        else -> Triple("👤", "普通用户", listOf(Color(0xFF95A5A6), Color(0xFF7F8C8D)))
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(colors = colors.map { it.copy(alpha = 0.15f) })
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 16.sp
                )
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors[0]
                )
            }
        }
    }
}
