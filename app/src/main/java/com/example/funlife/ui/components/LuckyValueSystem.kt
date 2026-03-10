// LuckyValueSystem.kt - 幸运值系统组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class LuckyParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float,
    val maxLife: Float,
    val color: Color,
    val size: Float
)

/**
 * 幸运值系统 - 增强动画版
 */
@Composable
fun LuckyValueSystem(
    modifier: Modifier = Modifier,
    onLuckyValueChange: (Int) -> Unit = {}
) {
    var luckyValue by remember { mutableStateOf(0) }
    var maxLucky by remember { mutableStateOf(100) }
    var particles by remember { mutableStateOf<List<LuckyParticle>>(emptyList()) }
    var showFullEffect by remember { mutableStateOf(false) }
    var buttonScale by remember { mutableFloatStateOf(1f) }
    var cardScale by remember { mutableFloatStateOf(1f) }
    var progressGlow by remember { mutableFloatStateOf(0f) }
    var showPlusOne by remember { mutableStateOf(false) }
    var plusOneOffset by remember { mutableFloatStateOf(0f) }
    var plusOneAlpha by remember { mutableFloatStateOf(1f) }
    
    val scope = rememberCoroutineScope()
    val progress = luckyValue.toFloat() / maxLucky
    
    // 进度条光效动画
    LaunchedEffect(Unit) {
        while (true) {
            animate(0f, 1f, animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )) { value, _ ->
                progressGlow = value
            }
        }
    }
    
    // +1动画
    LaunchedEffect(showPlusOne) {
        if (showPlusOne) {
            plusOneOffset = 0f
            plusOneAlpha = 1f
            animate(0f, -50f, animationSpec = tween(800)) { value, _ ->
                plusOneOffset = value
            }
            animate(1f, 0f, animationSpec = tween(800)) { value, _ ->
                plusOneAlpha = value
            }
            showPlusOne = false
        }
    }
    
    // 粒子动画
    LaunchedEffect(particles.size) {
        if (particles.isNotEmpty()) {
            while (particles.isNotEmpty()) {
                delay(16)
                particles = particles.map { particle ->
                    particle.copy(
                        x = particle.x + particle.vx,
                        y = particle.y + particle.vy,
                        vy = particle.vy + 0.2f,
                        life = particle.life - 0.02f
                    )
                }.filter { it.life > 0 }
            }
        }
    }
    
    // 满值特效
    LaunchedEffect(showFullEffect) {
        if (showFullEffect) {
            delay(1500)
            showFullEffect = false
        }
    }
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 粒子层
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            particles.forEach { particle ->
                val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
                
                // 粒子光晕（更大更亮）
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            particle.color.copy(alpha = alpha * 0.8f),
                            particle.color.copy(alpha = alpha * 0.4f),
                            Color.Transparent
                        ),
                        radius = particle.size * 3
                    ),
                    radius = particle.size * 3,
                    center = Offset(particle.x, particle.y)
                )
                
                // 粒子主体
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size,
                    center = Offset(particle.x, particle.y)
                )
                
                // 粒子内核高光
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    radius = particle.size * 0.4f,
                    center = Offset(particle.x - particle.size * 0.2f, particle.y - particle.size * 0.2f)
                )
            }
        }
        
        // 紧凑的幸运值显示
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 幸运值卡片 - 增强动画
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF9E6)
                ),
                modifier = Modifier.scale(cardScale)
            ) {
                Box {
                    // 背景动画光效
                    Canvas(Modifier.matchParentSize()) {
                        // 流动的渐变背景
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFE5B4),
                                    Color(0xFFFFF9E6),
                                    Color(0xFFFFE5B4)
                                ),
                                startX = size.width * progressGlow,
                                endX = size.width * (progressGlow + 0.5f)
                            )
                        )
                        
                        // 动态光点
                        if (progress > 0) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    radius = 60f
                                ),
                                radius = 60f,
                                center = Offset(size.width * progress, size.height / 2)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 旋转的四叶草
                        val infiniteTransition = rememberInfiniteTransition(label = "clover")
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
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                            }
                        ) {
                            Text("🍀", fontSize = 18.sp)
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35)
                            )
                            // 进度条 - 增强动画
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.5f))
                            ) {
                                // 进度条填充 - 动画过渡
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progress,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "progress"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFFD700),
                                                    Color(0xFFFFA500),
                                                    Color(0xFFFF8C00)
                                                )
                                            )
                                        )
                                )
                                
                                // 进度条光效 - 流动动画
                                if (animatedProgress > 0) {
                                    Canvas(Modifier.matchParentSize()) {
                                        val glowX = size.width * animatedProgress * progressGlow
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.8f),
                                                    Color.Transparent
                                                ),
                                                radius = 30f
                                            ),
                                            radius = 30f,
                                            center = Offset(glowX, size.height / 2)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 数值显示 - 带动画
                        val animatedValue by animateIntAsState(
                            targetValue = luckyValue,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "value"
                        )
                        
                        Box {
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF8C00)
                            )
                            
                            // +1飘字效果
                            if (showPlusOne) {
                                Text(
                                    "+${Random.nextInt(1, 6)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier
                                        .offset(x = 20.dp, y = plusOneOffset.dp)
                                        .graphicsLayer { alpha = plusOneAlpha }
                                )
                            }
                        }
                    }
                }
            }
            
            // 小巧的点击按钮 - 增强动画
            Button(
                onClick = {
                    scope.launch {
                        // 按钮动画
                        buttonScale = 0.85f
                        cardScale = 1.05f
                        delay(80)
                        buttonScale = 1.15f
                        delay(80)
                        buttonScale = 1f
                        cardScale = 1f
                        
                        // 显示+1
                        showPlusOne = true
                        
                        val increment = Random.nextInt(1, 6)
                        luckyValue = (luckyValue + increment).coerceAtMost(maxLucky)
                        onLuckyValueChange(luckyValue)
                        
                        // 创建更多粒子
                        val centerX = 400f
                        val centerY = 40f
                        val newParticles = List(30) { // 增加到30个
                            val angle = Random.nextFloat() * 360f
                            val speed = Random.nextFloat() * 8f + 4f
                            LuckyParticle(
                                x = centerX,
                                y = centerY,
                                vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                                vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 3f,
                                life = 1f,
                                maxLife = 1f,
                                color = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500),
                                    Color(0xFFFF6B9D),
                                    Color(0xFF4FACFE),
                                    Color(0xFFFFEA00),
                                    Color(0xFFFF1744)
                                ).random(),
                                size = Random.nextFloat() * 5f + 4f
                            )
                        }
                        particles = particles + newParticles
                        
                        if (luckyValue >= maxLucky) {
                            showFullEffect = true
                            delay(500)
                            luckyValue = 0
                        }
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .scale(buttonScale),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                // 按钮内容 - 带脉冲动画
                val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                
                Box(modifier = Modifier.scale(pulseScale)) {
                    Text("🎲", fontSize = 28.sp)
                }
            }
        }
        
        // 满值特效 - 增强动画
        if (showFullEffect) {
            val fullEffectScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "fullEffect"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp)
                    .scale(fullEffectScale),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Text(
                            "幸运满值！",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text("✨", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
