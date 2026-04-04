// LuckyValueImageBar.kt - 基于原型图图片的幸运值进度条
package com.example.funlife.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 基于原型图图片的幸运值进度条组件
 * 
 * 使用设计稿切图作为背景，在上面叠加交互元素
 * 
 * @param currentValue 当前幸运值
 * @param maxValue 最大幸运值
 * @param onDiceClick 点击骰子按钮的回调
 */
@Composable
fun LuckyValueImageBar(
    modifier: Modifier = Modifier,
    currentValue: Int = 0,
    maxValue: Int = 100,
    onDiceClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // 加载图片
    val bitmap = remember {
        try {
            context.assets.open("dibu/xyz.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("LuckyValueImageBar", "Failed to load image: ${e.message}")
            null
        }
    }
    
    // 按钮动画状态
    var buttonScale by remember { mutableFloatStateOf(1f) }
    var buttonRotation by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    // 光束动画状态
    var lightBeamProgress by remember { mutableFloatStateOf(0f) }
    var showLightBeam by remember { mutableStateOf(false) }
    
    // 数字飘动动画状态
    data class FloatingNumber(
        val value: Int,
        val startTime: Long,
        val x: Float,
        val y: Float,
        val id: Int
    )
    var floatingNumbers by remember { mutableStateOf<List<FloatingNumber>>(emptyList()) }
    var nextNumberId by remember { mutableIntStateOf(0) }
    
    // 清理过期的飘动数字
    LaunchedEffect(floatingNumbers) {
        if (floatingNumbers.isNotEmpty()) {
            delay(1500)
            val currentTime = System.currentTimeMillis()
            floatingNumbers = floatingNumbers.filter { 
                currentTime - it.startTime < 1500 
            }
        }
    }
    
    // 按钮呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    
    // 星星闪烁动画
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )
    
    // 星星旋转动画
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )
    
    // 星星粒子数据（增加到12个）
    data class StarParticle(val x: Float, val y: Float, val size: Float, val rotation: Float)
    val starParticles = remember {
        List(12) { index ->
            StarParticle(
                x = 0.15f + (Random.nextFloat() * 0.7f),
                y = 0.25f + (Random.nextFloat() * 0.5f),
                size = Random.nextFloat() * 3f + 5f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp)
    ) {
        // 背景图片
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "幸运值进度条",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "图片加载中...",
                    color = Color.White
                )
            }
        }
        
        // 光束动画层（点击时触发）
        if (showLightBeam) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val beamX = size.width * 0.15f + (size.width * 0.7f) * lightBeamProgress
                val beamY = size.height * 0.5f
                
                // 绘制光束（带拖尾效果）
                for (i in 0..3) {
                    val offsetX = beamX - (i * 15f)
                    val alpha = (1f - i * 0.25f) * (1f - lightBeamProgress * 0.3f)
                    
                    // 外层光晕
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = alpha * 0.8f),
                                Color(0xFFFFD700).copy(alpha = alpha * 0.6f),
                                Color(0xFFFFA500).copy(alpha = alpha * 0.3f),
                                Color.Transparent
                            )
                        ),
                        radius = 25f - i * 5f,
                        center = Offset(offsetX, beamY)
                    )
                    
                    // 核心亮点
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 8f - i * 2f,
                        center = Offset(offsetX, beamY)
                    )
                }
            }
        }
        
        // 星星粒子层（真正的星星形状）
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制星星形状的函数
            fun drawStar(
                center: Offset,
                starSize: Float,
                rotation: Float,
                color: Color
            ) {
                val path = androidx.compose.ui.graphics.Path()
                val outerRadius = starSize
                val innerRadius = starSize * 0.4f
                val points = 5
                
                for (i in 0 until points * 2) {
                    val angle = (rotation + i * 36f) * (Math.PI / 180f).toFloat()
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = center.x + radius * kotlin.math.cos(angle)
                    val y = center.y + radius * kotlin.math.sin(angle)
                    
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                
                drawPath(path, color)
            }
            
            starParticles.forEach { particle ->
                val particleAlpha = starAlpha * (0.6f + Random.nextFloat() * 0.4f)
                
                // 星星位置
                val x = size.width * particle.x
                val y = size.height * particle.y
                val center = Offset(x, y)
                
                // 绘制发光效果（外层）
                drawStar(
                    center = center,
                    starSize = particle.size * 1.8f,
                    rotation = particle.rotation + starRotation * 0.3f,
                    color = Color(0xFFFFFFFF).copy(alpha = particleAlpha * 0.3f)
                )
                
                // 绘制星星主体
                drawStar(
                    center = center,
                    starSize = particle.size,
                    rotation = particle.rotation + starRotation * 0.3f,
                    color = Color(0xFFFFD700).copy(alpha = particleAlpha)
                )
            }
        }
        
        // 数字飘动动画层
        floatingNumbers.forEach { floatingNum ->
            val elapsed = (System.currentTimeMillis() - floatingNum.startTime).toFloat()
            val progress = (elapsed / 1500f).coerceIn(0f, 1f)
            
            // 上升和淡出动画
            val offsetY = -60f * progress
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val scale = 1f + progress * 0.3f
            
            Box(
                modifier = Modifier
                    .offset(
                        x = floatingNum.x.dp,
                        y = (floatingNum.y + offsetY).dp
                    )
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                Text(
                    text = "+${floatingNum.value}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700),
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
        
        // 交互层：左侧"幸运值"和数值
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 50.dp)
                .offset(y = 3.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "幸运值",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(1.2f, 1.2f),
                        blurRadius = 3f
                    ),
                    letterSpacing = 1.sp
                )
            )
            
            Text(
                text = currentValue.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.offset(y = (-1).dp)
            )
        }
        
        // 交互层：可爱风格按钮（增强动画）
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-40).dp)
                .size(56.dp)
                .graphicsLayer {
                    scaleX = buttonScale * breatheScale
                    scaleY = buttonScale * breatheScale
                    rotationZ = buttonRotation
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        // 生成随机增加值
                        val increment = Random.nextInt(1, 11)
                        
                        // 添加飘动数字动画
                        val newFloatingNumber = FloatingNumber(
                            value = increment,
                            startTime = System.currentTimeMillis(),
                            x = 100f + Random.nextFloat() * 50f,
                            y = 20f,
                            id = nextNumberId++
                        )
                        floatingNumbers = floatingNumbers + newFloatingNumber
                        
                        // 触发光束动画
                        showLightBeam = true
                        lightBeamProgress = 0f
                        
                        // 光束移动动画
                        launch {
                            val startTime = System.currentTimeMillis()
                            val duration = 800L
                            
                            while (lightBeamProgress < 1f) {
                                val elapsed = System.currentTimeMillis() - startTime
                                lightBeamProgress = (elapsed.toFloat() / duration).coerceAtMost(1f)
                                delay(16)
                            }
                            
                            delay(100)
                            showLightBeam = false
                        }
                        
                        // 按钮动画
                        buttonScale = 0.7f
                        buttonRotation = -15f
                        delay(80)
                        buttonScale = 1.3f
                        buttonRotation = 15f
                        delay(80)
                        buttonScale = 0.95f
                        buttonRotation = -5f
                        delay(80)
                        buttonScale = 1.05f
                        buttonRotation = 5f
                        delay(80)
                        buttonScale = 1f
                        buttonRotation = 0f
                        
                        onDiceClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 外层光晕（随呼吸动画变化）
            Canvas(modifier = Modifier.size(70.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = starAlpha * 0.5f),
                            Color(0xFFFFA500).copy(alpha = starAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = size.width / 2f
                )
            }
            
            // 星星图标（带旋转）
            Box(
                modifier = Modifier.graphicsLayer {
                    rotationZ = starRotation
                }
            ) {
                Text(
                    text = "✨",
                    fontSize = 40.sp,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = Shadow(
                            color = Color(0xFFFFD700).copy(alpha = starAlpha * 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = 16f
                        )
                    )
                )
            }
        }
    }
}

/**
 * 简单使用示例
 */
@Composable
fun LuckyValueImageBarExample() {
    var luckyValue by remember { mutableIntStateOf(50) }
    
    LuckyValueImageBar(
        currentValue = luckyValue,
        maxValue = 100,
        onDiceClick = {
            luckyValue = (luckyValue + Random.nextInt(1, 11)).coerceAtMost(100)
        }
    )
}
