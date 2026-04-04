// LuckyValueSystem.kt - 幸运值系统组件（严格还原原型图）
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
 * 幸运值系统 - 严格还原原型图
 */
@Composable
fun LuckyValueSystem(
    modifier: Modifier = Modifier,
    luckyValue: Int = 0,
    onLuckyValueChange: (Int) -> Unit = {},
    onResetRequest: (() -> Int)? = null
) {
    var internalLuckyValue by remember { mutableStateOf(luckyValue) }
    
    LaunchedEffect(luckyValue) {
        internalLuckyValue = luckyValue
    }
    
    var maxLucky by remember { mutableStateOf(100) }
    var particles by remember { mutableStateOf<List<LuckyParticle>>(emptyList()) }
    var showFullEffect by remember { mutableStateOf(false) }
    var buttonScale by remember { mutableFloatStateOf(1f) }
    var showPlusOne by remember { mutableStateOf(false) }
    var plusOneOffset by remember { mutableFloatStateOf(0f) }
    var plusOneAlpha by remember { mutableFloatStateOf(1f) }
    
    val scope = rememberCoroutineScope()
    val progress = internalLuckyValue.toFloat() / maxLucky
    
    // 星星闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )
    
    // 骰子周围圆点旋转
    val dotRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotRotation"
    )
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )
    
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
    
    LaunchedEffect(showFullEffect) {
        if (showFullEffect) {
            delay(1500)
            showFullEffect = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp) // 更窄的高度
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(30.dp))
    ) {
        // 使用Canvas绘制整个组件
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val cornerRadius = 30.dp.toPx()
            
            // 1. 绘制深邃宇宙背景渐变（青→紫→绿）
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2B8A9A), // 深青色
                        Color(0xFF6B5B8A), // 深紫色
                        Color(0xFF5B8A7B)  // 深绿色
                    )
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                size = androidx.compose.ui.geometry.Size(width, height)
            )
            
            // 2. 绘制浩瀚密集的宇宙星星（✨十字星形状）
            val starCount = 40 // 大幅增加星星数量
            repeat(starCount) { i ->
                // 使用更随机的分布
                val x = (width / 8) * (i % 8) + (i * 13f) % (width / 8)
                val y = (height * 0.15f) + (i * 19f) % (height * 0.7f)
                val twinklePhase = (starTwinkle + i * 25f) % 360f
                val alpha = (sin(Math.toRadians(twinklePhase.toDouble())).toFloat() * 0.6f + 0.4f).coerceIn(0.3f, 1f)
                val starSize = when {
                    i % 5 == 0 -> 6f // 大星星
                    i % 3 == 0 -> 4f // 中星星
                    else -> 2.5f     // 小星星
                }
                
                // 绘制十字星
                // 横向光芒
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(x - starSize * 2, y),
                    end = Offset(x + starSize * 2, y),
                    strokeWidth = 1.5f
                )
                // 纵向光芒
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(x, y - starSize * 2),
                    end = Offset(x, y + starSize * 2),
                    strokeWidth = 1.5f
                )
                
                // 星星中心光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            Color.White.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        radius = starSize * 1.5f
                    ),
                    radius = starSize * 1.5f,
                    center = Offset(x, y)
                )
                
                // 星星核心
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starSize * 0.5f,
                    center = Offset(x, y)
                )
            }
            
            // 3. 绘制彩虹进度条光束（细长管道样式，完整显示）
            // 彩虹光束是一个细管道，从左边延伸到右边
            val fullProgressWidth = width // 完整宽度
            val pipeHeight = 16.dp.toPx() // 管道高度（细管道）
            val pipeY = (height - pipeHeight) / 2 // 垂直居中
            
            // 进度条发光底层（扩大光晕范围）
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF5DD3E0).copy(alpha = 0.5f), // 青色光晕
                        Color(0xFFB47FE5).copy(alpha = 0.5f), // 紫色光晕
                        Color(0xFFFF9EC8).copy(alpha = 0.5f), // 粉色光晕
                        Color(0xFFFFD93D).copy(alpha = 0.5f)  // 黄色光晕
                    )
                ),
                topLeft = Offset(0f, pipeY - 4.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius((pipeHeight + 8.dp.toPx()) / 2),
                size = androidx.compose.ui.geometry.Size(fullProgressWidth, pipeHeight + 8.dp.toPx())
            )
            
            // 进度条主体（彩虹光束管道 - 完整显示）
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF5DD3E0), // 青色
                        Color(0xFFB47FE5), // 紫色
                        Color(0xFFFF9EC8), // 粉色
                        Color(0xFFFFD93D)  // 黄色
                    )
                ),
                topLeft = Offset(0f, pipeY),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pipeHeight / 2),
                size = androidx.compose.ui.geometry.Size(fullProgressWidth, pipeHeight),
                alpha = 0.9f
            )
            
            // 在实际进度位置添加一个标记（可选，用于显示真实进度）
            val actualProgressWidth = width * animatedProgress
            if (actualProgressWidth > 0) {
                // 在进度位置画一个微弱的标记
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            Color.Transparent
                        ),
                        radius = 20f
                    ),
                    radius = 20f,
                    center = Offset(actualProgressWidth, height / 2)
                )
            }
            
            // 4. 绘制点击粒子效果
            particles.forEach { particle ->
                if (particle.size <= 0f) return@forEach
                
                val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
                
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
                    center = Offset(particle.x, particle.y)
                )
            }
        }
        
        // 内容层（文字和按钮）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：四叶草在左上角，幸运值和数值在同一行
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 四叶草
                    Text(
                        text = "🍀",
                        fontSize = 32.sp
                    )
                    
                    // 幸运值文字和数值在同一行
                    Column {
                        Text(
                            text = "幸运值",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    offset = Offset(1.5f, 1.5f),
                                    blurRadius = 3f
                                )
                            )
                        )
                        
                        // 数值
                        val animatedValue by animateIntAsState(
                            targetValue = internalLuckyValue,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "value"
                        )
                        
                        Box {
                            Text(
                                text = "$animatedValue",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                            
                            // +1飘字
                            if (showPlusOne) {
                                Text(
                                    "+${Random.nextInt(1, 6)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier
                                        .offset(x = 40.dp, y = plusOneOffset.dp)
                                        .graphicsLayer { alpha = plusOneAlpha }
                                )
                            }
                        }
                    }
                }
            }

            // 右侧：骰子按钮
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .scale(buttonScale)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            buttonScale = 0.85f
                            delay(80)
                            buttonScale = 1.15f
                            delay(80)
                            buttonScale = 1f

                            showPlusOne = true

                            val increment = Random.nextInt(1, 6)
                            internalLuckyValue =
                                (internalLuckyValue + increment).coerceAtMost(maxLucky)
                            onLuckyValueChange(internalLuckyValue)

                            // 生成粒子
                            val newParticles = mutableListOf<LuckyParticle>()
                            repeat(30) {
                                val angle = Random.nextFloat() * 360f
                                val speed = Random.nextFloat() * 10f + 5f
                                newParticles.add(
                                    LuckyParticle(
                                        x = 350f,
                                        y = 30f,
                                        vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                                        vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 3f,
                                        life = 1f,
                                        maxLife = 1f,
                                        color = listOf(
                                            Color(0xFFFFEA00),
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500)
                                        ).random(),
                                        size = Random.nextFloat() * 3f + 2f
                                    )
                                )
                            }
                            particles = particles + newParticles

                            if (internalLuckyValue >= maxLucky) {
                                showFullEffect = true
                            }
                        }
                    }
            ) {
                // 使用Canvas绘制骰子按钮
                Canvas(modifier = Modifier.size(60.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 外层金色圆环（带多层光晕）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.4f),
                                Color(0xFFFFA500).copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 32.dp.toPx()
                        ),
                        radius = 32.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFB800),
                                Color(0xFFFFA500)
                            )
                        ),
                        radius = 30.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    
                    // 内层白色背景
                    drawCircle(
                        color = Color.White,
                        radius = 24.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                    
                    // 绘制白色小圆点（围绕金色圆环，更明显）
                    repeat(12) { i ->
                        val angle = (i * 30f + dotRotation) * Math.PI.toFloat() / 180f
                        val dotRadius = 28.dp.toPx()
                        val dotX = centerX + cos(angle) * dotRadius
                        val dotY = centerY + sin(angle) * dotRadius
                        
                        // 小圆点光晕（更大更明显）
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 1f),
                                    Color.White.copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                radius = 6f
                            ),
                            radius = 6f,
                            center = Offset(dotX, dotY)
                        )
                        
                        // 小圆点核心
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(dotX, dotY)
                        )
                    }
                }
                
                // 骰子emoji
                Text(
                    text = "🎲",
                    fontSize = 32.sp
                )
            }
        }
        
        // 满值特效
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
                    .offset(y = (-20).dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    elevation = CardDefaults.cardElevation(16.dp),
                    modifier = Modifier.scale(fullEffectScale)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 24.sp)
                        Text(
                            "幸运满值！",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text("✨", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}
