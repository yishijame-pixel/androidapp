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
 * 幸运值系统 - 超级增强动画版
 * @param luckyValue 当前幸运值（外部管理）
 * @param onLuckyValueChange 幸运值变化回调
 */
@Composable
fun LuckyValueSystem(
    modifier: Modifier = Modifier,
    luckyValue: Int = 0,
    onLuckyValueChange: (Int) -> Unit = {},
    onResetRequest: (() -> Int)? = null
) {
    var internalLuckyValue by remember { mutableStateOf(luckyValue) }
    
    // 同步外部状态
    LaunchedEffect(luckyValue) {
        internalLuckyValue = luckyValue
    }
    
    var maxLucky by remember { mutableStateOf(100) }
    var particles by remember { mutableStateOf<List<LuckyParticle>>(emptyList()) }
    var showFullEffect by remember { mutableStateOf(false) }
    var buttonScale by remember { mutableFloatStateOf(1f) }
    var cardScale by remember { mutableFloatStateOf(1f) }
    var progressGlow by remember { mutableFloatStateOf(0f) }
    var showPlusOne by remember { mutableStateOf(false) }
    var plusOneOffset by remember { mutableFloatStateOf(0f) }
    var plusOneAlpha by remember { mutableFloatStateOf(1f) }
    var borderGlow by remember { mutableFloatStateOf(0f) }
    var buttonRotation by remember { mutableFloatStateOf(0f) }
    var explosionScale by remember { mutableFloatStateOf(0f) }
    var showExplosion by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val progress = internalLuckyValue.toFloat() / maxLucky
    
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
    
    // 边框光效动画
    LaunchedEffect(Unit) {
        while (true) {
            animate(0f, 360f, animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )) { value, _ ->
                borderGlow = value
            }
        }
    }
    
    // 按钮旋转动画
    LaunchedEffect(Unit) {
        while (true) {
            animate(0f, 360f, animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )) { value, _ ->
                buttonRotation = value
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
    
    // 爆炸效果动画
    LaunchedEffect(showExplosion) {
        if (showExplosion) {
            explosionScale = 0.1f // 从0.1开始而不是0,避免radius为0
            animate(0.1f, 2f, animationSpec = tween(400, easing = FastOutSlowInEasing)) { value, _ ->
                explosionScale = value
            }
            delay(400)
            showExplosion = false
            explosionScale = 0f
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
        // 背景星星粒子层
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            repeat(15) { i ->
                val x = (size.width / 15) * i + (borderGlow * 2) % 50
                val y = 20f + sin((borderGlow + i * 30) * 0.05f) * 15f
                val alpha = (sin((borderGlow + i * 45) * 0.08f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = alpha * 0.6f),
                            Color.Transparent
                        ),
                        radius = 12f
                    ),
                    radius = 12f,
                    center = Offset(x, y)
                )
                
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = alpha),
                    radius = 3f,
                    center = Offset(x, y)
                )
            }
        }
        
        // 粒子层 - 优化性能版
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            particles.forEach { particle ->
                if (particle.size <= 0f) return@forEach
                
                val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
                val glowRadius = (particle.size * 2.5f).coerceAtLeast(0.1f)
                
                // 简化为两层：光晕 + 主体
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            particle.color.copy(alpha = alpha * 0.7f),
                            particle.color.copy(alpha = alpha * 0.3f),
                            Color.Transparent
                        ),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(particle.x, particle.y)
                )
                
                // 粒子主体
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size,
                    center = Offset(particle.x, particle.y)
                )
            }
        }
        
        // 爆炸效果层
        if (showExplosion && explosionScale > 0.01f) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val centerX = size.width - 60.dp.toPx() // 按钮位置
                val centerY = 40f
                
                repeat(3) { ring ->
                    val ringRadius = (explosionScale * 80f * (1 + ring * 0.3f)).coerceAtLeast(0.1f)
                    val ringAlpha = (1f - explosionScale * 0.5f) * (1f - ring * 0.3f)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = ringAlpha * 0.6f),
                                Color(0xFFFFA500).copy(alpha = ringAlpha * 0.3f),
                                Color.Transparent
                            ),
                            radius = ringRadius
                        ),
                        radius = ringRadius,
                        center = Offset(centerX, centerY)
                    )
                }
                
                repeat(12) { i ->
                    val angle = (i * 30f + explosionScale * 60f) * Math.PI.toFloat() / 180f
                    val length = explosionScale * 60f
                    val startX = centerX + cos(angle) * 10f
                    val startY = centerY + sin(angle) * 10f
                    val endX = centerX + cos(angle) * length
                    val endY = centerY + sin(angle) * length
                    val lineAlpha = 1f - explosionScale * 0.5f
                    
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = lineAlpha),
                                Color(0xFFFFA500).copy(alpha = lineAlpha * 0.5f),
                                Color.Transparent
                            ),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )
                }
            }
        }
        
        // 主要能量条组件 - 超级炫酷融合设计
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(35.dp)) // 裁剪超出圆角范围的内容
        ) {
            // 能量进度动画 - 移到Canvas外部
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "energyProgress"
            )
            
            // 超级炫酷背景条 - 多层渐变和动画
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                // 第一层：深色底层阴影
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E).copy(alpha = 0.8f),
                            Color(0xFF16213E).copy(alpha = 0.8f),
                            Color(0xFF0F3460).copy(alpha = 0.8f)
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(35.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 70.dp.toPx())
                )
                
                // 第二层：流动的彩虹渐变背景
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00F5FF).copy(alpha = 0.4f),
                            Color(0xFF00D9FF).copy(alpha = 0.4f),
                            Color(0xFF7B2FF7).copy(alpha = 0.4f),
                            Color(0xFFFF006E).copy(alpha = 0.4f),
                            Color(0xFFFFBE0B).copy(alpha = 0.4f),
                            Color(0xFF00F5A0).copy(alpha = 0.4f)
                        ),
                        startX = size.width * progressGlow * 0.5f,
                        endX = size.width * (1f + progressGlow * 0.5f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(35.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 70.dp.toPx())
                )
                
                // 第三层：闪烁的星光点缀
                repeat(12) { i ->
                    val starX = (size.width / 12) * i + (progressGlow * 3) % 50
                    val starY = 35.dp.toPx() + sin((progressGlow + i * 30) * 0.08f) * 20f
                    val starAlpha = (sin((progressGlow + i * 50) * 0.12f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                    
                    // 星光光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = starAlpha * 0.8f),
                                Color(0xFF00F5FF).copy(alpha = starAlpha * 0.5f),
                                Color.Transparent
                            ),
                            radius = 15f
                        ),
                        radius = 15f,
                        center = Offset(starX, starY)
                    )
                    
                    // 星光核心
                    drawCircle(
                        color = Color.White.copy(alpha = starAlpha),
                        radius = 3f,
                        center = Offset(starX, starY)
                    )
                }
                
                // 能量进度填充 - 超级炫酷渐变
                if (animatedProgress > 0) {
                    // 主能量填充 - 多层渐变
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFEA00),
                                Color(0xFFFFD700),
                                Color(0xFFFFA500),
                                Color(0xFFFF8C00),
                                Color(0xFFFF6B35),
                                Color(0xFFFFD700)
                            ),
                            endX = size.width * animatedProgress
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(35.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * animatedProgress, 70.dp.toPx()),
                        alpha = 0.9f
                    )
                    
                    // 能量前端柔和光效 - 完全去除白色
                    val energyEndX = size.width * animatedProgress
                    
                    // 外层金色光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.7f),
                                Color(0xFFFFA500).copy(alpha = 0.5f),
                                Color(0xFFFF8C00).copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = 60f
                        ),
                        radius = 60f,
                        center = Offset(energyEndX, 35.dp.toPx())
                    )
                    
                    // 中层金色光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFEA00).copy(alpha = 0.6f),
                                Color(0xFFFFD700).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            radius = 35f
                        ),
                        radius = 35f,
                        center = Offset(energyEndX, 35.dp.toPx())
                    )
                    
                    // 核心亮点 - 纯金色
                    drawCircle(
                        color = Color(0xFFFFEA00).copy(alpha = 0.8f),
                        radius = 12f,
                        center = Offset(energyEndX, 35.dp.toPx())
                    )
                    
                    // 流动的能量波纹 - 金色为主
                    val flowOffset = (progressGlow * size.width * animatedProgress) % (size.width * animatedProgress)
                    repeat(5) { i ->
                        val lightX = flowOffset + i * (size.width * animatedProgress / 5)
                        if (lightX <= size.width * animatedProgress) {
                            val waveAlpha = (sin((progressGlow + i * 72) * 0.15f) * 0.4f + 0.6f).coerceIn(0f, 1f)
                            
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFEA00).copy(alpha = waveAlpha * 0.7f),
                                        Color(0xFFFFD700).copy(alpha = waveAlpha * 0.5f),
                                        Color.Transparent
                                    ),
                                    radius = 35f
                                ),
                                radius = 35f,
                                center = Offset(lightX, 35.dp.toPx())
                            )
                        }
                    }
                    
                    // 能量粒子效果
                    repeat(8) { i ->
                        val particleX = (size.width * animatedProgress * (i / 8f) + progressGlow * 20) % (size.width * animatedProgress)
                        val particleY = 35.dp.toPx() + sin((progressGlow * 2 + i * 45) * 0.1f) * 15f
                        val particleAlpha = (sin((progressGlow + i * 45) * 0.15f) * 0.5f + 0.5f).coerceIn(0f, 1f)
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFEA00).copy(alpha = particleAlpha),
                                    Color.Transparent
                                ),
                                radius = 8f
                            ),
                            radius = 8f,
                            center = Offset(particleX, particleY)
                        )
                    }
                }
                
                // 多层旋转边框光环
                repeat(3) { layer ->
                    val rotationOffset = borderGlow + layer * 120f
                    val layerAlpha = 0.9f - layer * 0.2f
                    
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF00F5FF).copy(alpha = layerAlpha),
                                Color(0xFF7B2FF7).copy(alpha = layerAlpha),
                                Color(0xFFFF006E).copy(alpha = layerAlpha),
                                Color(0xFFFFBE0B).copy(alpha = layerAlpha),
                                Color(0xFF00F5A0).copy(alpha = layerAlpha),
                                Color(0xFF00F5FF).copy(alpha = layerAlpha)
                            ),
                            center = Offset(size.width / 2, 35.dp.toPx())
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius((35 + layer * 2).dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 70.dp.toPx()),
                        style = Stroke(width = (3f - layer * 0.5f)),
                        alpha = layerAlpha
                    )
                }
                
                // 旋转的光点装饰
                val angle = borderGlow * Math.PI.toFloat() / 180f
                repeat(8) { i ->
                    val pointAngle = angle + i * Math.PI.toFloat() / 4
                    val x = size.width / 2 + cos(pointAngle) * (size.width / 2 + 5f)
                    val y = 35.dp.toPx() + sin(pointAngle) * 35f
                    
                    // 光点光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFF00F5FF).copy(alpha = 0.7f),
                                Color(0xFFFFBE0B).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            radius = 15f
                        ),
                        radius = 15f,
                        center = Offset(x, y)
                    )
                    
                    // 光点核心
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }
            
            // 内容层
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧:四叶草 + 幸运值文字
                Row(
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
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    ) {
                        Text("🍀", fontSize = 24.sp)
                    }
                    
                    Column {
                        // "幸运值"文字 - 多层描边效果
                        Box {
                            // 描边层
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
                            )
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = 1.dp, y = (-1).dp)
                            )
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = (-1).dp, y = 1.dp)
                            )
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                            )
                            // 主文字
                            Text(
                                "幸运值",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // 数值显示
                        val animatedValue by animateIntAsState(
                            targetValue = internalLuckyValue,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "value"
                        )
                        
                        Box {
                            // 描边层
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = (-1.5).dp, y = (-1.5).dp)
                            )
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = 1.5.dp, y = (-1.5).dp)
                            )
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = (-1.5).dp, y = 1.5.dp)
                            )
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.offset(x = 1.5.dp, y = 1.5.dp)
                            )
                            // 主文字
                            Text(
                                "$animatedValue",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            
                            // +1飘字效果
                            if (showPlusOne) {
                                Text(
                                    "+${Random.nextInt(1, 6)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700),
                                    modifier = Modifier
                                        .offset(x = 30.dp, y = plusOneOffset.dp)
                                        .graphicsLayer { alpha = plusOneAlpha }
                                )
                            }
                        }
                    }
                }
                
                // 右侧:超级炫酷按钮
                Box(contentAlignment = Alignment.Center) {
                    // 最外层超大脉冲光环
                    val outerPulse by rememberInfiniteTransition(label = "outerPulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "outerPulse"
                    )
                    
                    Canvas(
                        Modifier
                            .size(100.dp)
                            .scale(outerPulse)
                    ) {
                        // 多层脉冲光环
                        repeat(3) { ring ->
                            val ringAlpha = (0.4f - ring * 0.1f) / outerPulse
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFEA00).copy(alpha = ringAlpha),
                                        Color(0xFFFFD700).copy(alpha = ringAlpha * 0.7f),
                                        Color(0xFFFFA500).copy(alpha = ringAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    radius = size.minDimension / 2
                                ),
                                radius = size.minDimension / 2 - ring * 10f,
                                center = Offset(size.width / 2, size.height / 2)
                            )
                        }
                    }
                    
                    // 中层多重旋转光环
                    Canvas(
                        Modifier
                            .size(80.dp)
                            .graphicsLayer { rotationZ = buttonRotation }
                    ) {
                        // 外层彩虹光环
                        repeat(5) { ring ->
                            val ringRadius = (40.dp.toPx() - ring * 6.dp.toPx()).coerceAtLeast(0.1f)
                            val ringAlpha = 0.6f - ring * 0.1f
                            
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFFFEA00).copy(alpha = ringAlpha),
                                        Color(0xFFFFD700).copy(alpha = ringAlpha),
                                        Color(0xFFFFA500).copy(alpha = ringAlpha),
                                        Color(0xFFFF6B35).copy(alpha = ringAlpha),
                                        Color(0xFFFF006E).copy(alpha = ringAlpha),
                                        Color(0xFF7B2FF7).copy(alpha = ringAlpha),
                                        Color(0xFF00F5FF).copy(alpha = ringAlpha),
                                        Color(0xFFFFEA00).copy(alpha = ringAlpha)
                                    ),
                                    center = Offset(size.width / 2, size.height / 2)
                                ),
                                radius = ringRadius,
                                center = Offset(size.width / 2, size.height / 2),
                                style = Stroke(width = 3f)
                            )
                        }
                        
                        // 旋转的超亮光点
                        repeat(12) { i ->
                            val angle = (buttonRotation + i * 30f) * Math.PI.toFloat() / 180f
                            val radius = 36.dp.toPx()
                            val x = size.width / 2 + cos(angle) * radius
                            val y = size.height / 2 + sin(angle) * radius
                            
                            // 光点外层光晕
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.95f),
                                        Color(0xFFFFEA00).copy(alpha = 0.8f),
                                        Color(0xFFFFD700).copy(alpha = 0.5f),
                                        Color.Transparent
                                    ),
                                    radius = 12f
                                ),
                                radius = 12f,
                                center = Offset(x, y)
                            )
                            
                            // 光点核心
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                radius = 3.5f,
                                center = Offset(x, y)
                            )
                        }
                        
                        // 反向旋转的星星
                        repeat(6) { i ->
                            val angle = (-buttonRotation * 1.5f + i * 60f) * Math.PI.toFloat() / 180f
                            val radius = 26.dp.toPx()
                            val x = size.width / 2 + cos(angle) * radius
                            val y = size.height / 2 + sin(angle) * radius
                            
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFEA00).copy(alpha = 0.9f),
                                        Color(0xFFFFD700).copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    radius = 8f
                                ),
                                radius = 8f,
                                center = Offset(x, y)
                            )
                        }
                    }
                    
                    // 按钮主体
                    Button(
                        onClick = {
                            scope.launch {
                                buttonScale = 0.85f
                                cardScale = 1.05f
                                showExplosion = true
                                delay(80)
                                buttonScale = 1.15f
                                delay(80)
                                buttonScale = 1f
                                cardScale = 1f
                                
                                showPlusOne = true
                                
                                val increment = Random.nextInt(1, 6)
                                internalLuckyValue = (internalLuckyValue + increment).coerceAtMost(maxLucky)
                                onLuckyValueChange(internalLuckyValue)
                                
                                // 优化粒子生成 - 性能优化版
                                val centerX = 350f
                                val centerY = 40f
                                
                                val newParticles = mutableListOf<LuckyParticle>()
                                
                                // 主要粒子（40个）- 快速小粒子
                                repeat(40) {
                                    val angle = Random.nextFloat() * 360f
                                    val speed = Random.nextFloat() * 12f + 6f
                                    newParticles.add(
                                        LuckyParticle(
                                            x = centerX,
                                            y = centerY,
                                            vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                                            vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 4f,
                                            life = 1f,
                                            maxLife = 1f,
                                            color = listOf(
                                                Color(0xFFFFEA00),
                                                Color(0xFFFFD700),
                                                Color(0xFFFFA500),
                                                Color(0xFFFF8C00),
                                                Color(0xFFFF6B35)
                                            ).random(),
                                            size = Random.nextFloat() * 4f + 3f
                                        )
                                    )
                                }
                                
                                // 大粒子（20个）- 慢速
                                repeat(20) {
                                    val angle = Random.nextFloat() * 360f
                                    val speed = Random.nextFloat() * 6f + 3f
                                    newParticles.add(
                                        LuckyParticle(
                                            x = centerX,
                                            y = centerY,
                                            vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
                                            vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 3f,
                                            life = 1f,
                                            maxLife = 1f,
                                            color = Color(0xFFFFD700),
                                            size = Random.nextFloat() * 6f + 5f
                                        )
                                    )
                                }
                                
                                particles = particles + newParticles
                                
                                if (internalLuckyValue >= maxLucky) {
                                    showFullEffect = true
                                    // 不再自动清零，保持在100
                                }
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .scale(buttonScale),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 按钮背景 - 超级炫酷多层渐变
                            Canvas(Modifier.fillMaxSize()) {
                                // 最外层光晕
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFEA00).copy(alpha = 0.8f),
                                            Color(0xFFFFD700).copy(alpha = 0.6f),
                                            Color(0xFFFFA500).copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        radius = size.minDimension / 2
                                    ),
                                    radius = size.minDimension / 2,
                                    center = Offset(size.width / 2, size.height / 2)
                                )
                                
                                // 主体渐变
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFEA00),
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500),
                                            Color(0xFFFF8C00)
                                        ),
                                        radius = size.minDimension / 2.2f
                                    ),
                                    radius = size.minDimension / 2.2f,
                                    center = Offset(size.width / 2, size.height / 2)
                                )
                                
                                // 顶部超强高光
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.9f),
                                            Color.White.copy(alpha = 0.5f),
                                            Color.Transparent
                                        ),
                                        radius = size.minDimension / 3.5f
                                    ),
                                    radius = size.minDimension / 3.5f,
                                    center = Offset(size.width / 2 - size.width * 0.12f, size.height / 2 - size.height * 0.12f)
                                )
                                
                                // 流动光效
                                val flowAngle = (buttonRotation * 2) * Math.PI.toFloat() / 180f
                                val flowX = size.width / 2 + cos(flowAngle) * size.width * 0.25f
                                val flowY = size.height / 2 + sin(flowAngle) * size.height * 0.25f
                                
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.7f),
                                            Color(0xFFFFEA00).copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        radius = size.minDimension / 4
                                    ),
                                    radius = size.minDimension / 4,
                                    center = Offset(flowX, flowY)
                                )
                                
                                // 边缘金色光环
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFFFD700).copy(alpha = 0.6f),
                                            Color(0xFFFFEA00).copy(alpha = 0.8f)
                                        ),
                                        radius = size.minDimension / 2.2f,
                                        center = Offset(size.width / 2, size.height / 2)
                                    ),
                                    radius = size.minDimension / 2.2f,
                                    center = Offset(size.width / 2, size.height / 2),
                                    style = Stroke(width = 3f)
                                )
                            }
                            
                            // 按钮内容 - 超级脉冲动画
                            val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
                                initialValue = 1f,
                                targetValue = 1.25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            
                            Box(
                                modifier = Modifier.scale(pulseScale),
                                contentAlignment = Alignment.Center
                            ) {
                                // 骰子多层阴影
                                Text(
                                    "🎲",
                                    fontSize = 34.sp,
                                    modifier = Modifier.offset(x = 2.dp, y = 2.dp),
                                    color = Color.Black.copy(alpha = 0.5f)
                                )
                                Text(
                                    "🎲",
                                    fontSize = 34.sp,
                                    modifier = Modifier.offset(x = 1.dp, y = 1.dp),
                                    color = Color.Black.copy(alpha = 0.3f)
                                )
                                // 骰子主体
                                Text("🎲", fontSize = 34.sp)
                            }
                        }
                    }
                }
            }
        }
        
        // 满值特效 - 超级增强动画（烟花效果）
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
                // 烟花爆炸背景
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    val centerX = size.width / 2
                    val centerY = 30f
                    
                    // 多层烟花爆炸
                    repeat(20) { i ->
                        val angle = (i * 18f) * Math.PI.toFloat() / 180f
                        val distance = fullEffectScale * 80f
                        val endX = centerX + cos(angle) * distance
                        val endY = centerY + sin(angle) * distance
                        
                        // 烟花射线
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.8f),
                                    Color(0xFFFFA500).copy(alpha = 0.6f),
                                    Color(0xFFFF6B9D).copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                start = Offset(centerX, centerY),
                                end = Offset(endX, endY)
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY),
                            strokeWidth = 3f
                        )
                        
                        // 烟花末端光点
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color(0xFFFFD700).copy(alpha = 0.6f),
                                    Color.Transparent
                                ),
                                radius = 8f
                            ),
                            radius = 8f,
                            center = Offset(endX, endY)
                        )
                    }
                    
                    // 中心爆炸光环
                    repeat(4) { ring ->
                        val ringRadius = (fullEffectScale * 30f * (1 + ring * 0.4f)).coerceAtLeast(0.1f)
                        val ringAlpha = (1f - fullEffectScale * 0.3f) * (1f - ring * 0.2f)
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = ringAlpha * 0.8f),
                                    Color(0xFFFFD700).copy(alpha = ringAlpha * 0.5f),
                                    Color.Transparent
                                ),
                                radius = ringRadius
                            ),
                            radius = ringRadius,
                            center = Offset(centerX, centerY)
                        )
                    }
                }
                
                // 满值提示卡片
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700)
                    ),
                    elevation = CardDefaults.cardElevation(16.dp),
                    modifier = Modifier.scale(fullEffectScale)
                ) {
                    Box {
                        // 卡片内部光效
                        Canvas(Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    radius = size.maxDimension
                                )
                            )
                        }
                        
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✨", fontSize = 20.sp)
                            Text(
                                "幸运满值！",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text("✨", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
