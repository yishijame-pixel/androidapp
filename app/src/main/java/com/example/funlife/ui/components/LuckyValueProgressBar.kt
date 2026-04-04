// LuckyValueProgressBar.kt - 高度还原设计稿的幸运值进度条组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 星光粒子数据类
 */
data class StarParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinklePhase: Float
)

/**
 * 幸运值进度条组件 - Dribbble级UI设计
 * 
 * 特性：
 * - 5层结构：背景层、进度条层、星光粒子层、信息区、骰子按钮
 * - 高级渐变质感
 * - 发光效果（glow + blur）
 * - 星光粒子闪烁动画
 * - 流光动画
 * - 骰子按钮呼吸光效
 */
@Composable
fun LuckyValueProgressBar(
    modifier: Modifier = Modifier,
    currentValue: Int = 0,
    maxValue: Int = 100,
    onDiceClick: () -> Unit = {}
) {
    // 动画状态
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    
    // 流光动画 - 渐变偏移
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    // 星光闪烁动画
    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTwinkle"
    )
    
    // 骰子呼吸光效
    val diceGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diceGlow"
    )
    
    // 骰子旋转圆点
    val diceDotsRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diceDotsRotation"
    )
    
    // 进度动画
    val animatedProgress by animateFloatAsState(
        targetValue = (currentValue.toFloat() / maxValue).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )
    
    // 按钮点击动画
    var buttonScale by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()
    
    // 生成星光粒子（固定位置，只改变透明度）- 增加数量和密度
    val starParticles = remember {
        List(120) { // 从60增加到120个星星
            StarParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = when (Random.nextInt(10)) {
                    in 0..1 -> Random.nextFloat() * 1.5f + 2.5f // 10% 大星星
                    in 2..4 -> Random.nextFloat() * 1f + 1.5f   // 30% 中星星
                    else -> Random.nextFloat() * 0.8f + 0.8f    // 60% 小星星
                },
                alpha = Random.nextFloat() * 0.5f + 0.5f,
                twinklePhase = Random.nextFloat() * 360f
            )
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp)
    ) {
        // 主Canvas - 绘制所有层
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val cornerRadius = height / 2f
            
            // ========== 第1层：背景层 ==========
            drawBackgroundLayer(width, height, cornerRadius)
            
            // ========== 第2层：彩色进度条层 ==========
            drawProgressLayer(width, height, cornerRadius, animatedProgress, shimmerOffset)
            
            // ========== 第3层：星光粒子层 ==========
            drawStarParticles(width, height, starParticles, starTwinkle, animatedProgress)
        }
        
        // ========== 第4层：左侧信息区 ==========
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 四叶草图标
            Text(
                text = "🍀",
                fontSize = 36.sp,
                modifier = Modifier.offset(y = (-2).dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 文本信息
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                // "幸运值"文字
                Text(
                    text = "幸运值",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
                
                // 数值
                Text(
                    text = currentValue.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
        
        // ========== 第5层：右侧骰子按钮 ==========
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 4.dp) // 略微突出
                .size(56.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        // 按钮按下动画
                        buttonScale = 0.9f
                        delay(100)
                        buttonScale = 1.1f
                        delay(100)
                        buttonScale = 1f
                        
                        onDiceClick()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDiceButton(diceGlowScale, diceDotsRotation)
            }
            
            // 骰子图标
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎲",
                    fontSize = 28.sp
                )
            }
        }
    }
}

/**
 * 绘制背景层 - 丰富的宇宙星空背景
 */
private fun DrawScope.drawBackgroundLayer(
    width: Float,
    height: Float,
    cornerRadius: Float
) {
    // 外层阴影（模拟深度）
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.3f),
                Color.Transparent
            )
        ),
        topLeft = Offset(0f, height * 0.05f),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // 主背景渐变：青蓝 → 紫色 → 粉紫 → 青绿色（更丰富的渐变）
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF3B7A8C), // 深青蓝
                Color(0xFF6B5B95), // 深紫色
                Color(0xFF8B5B85), // 粉紫色
                Color(0xFF4A8A7A)  // 青绿色
            )
        ),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // 内阴影效果（顶部暗边）
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.25f),
                Color.Transparent
            ),
            endY = height * 0.3f
        ),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
    
    // 底部高光
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.12f)
            ),
            startY = height * 0.7f
        ),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius)
    )
}

/**
 * 绘制彩色进度条层 - 完整显示彩虹光束
 */
private fun DrawScope.drawProgressLayer(
    width: Float,
    height: Float,
    cornerRadius: Float,
    progress: Float,
    shimmerOffset: Float
) {
    // 彩虹光束始终完整显示，不受进度影响
    val progressWidth = width // 始终是完整宽度
    val progressHeight = height * 0.35f // 进度条高度为容器的35%
    val progressY = (height - progressHeight) / 2f // 垂直居中
    val progressCornerRadius = progressHeight / 2f
    
    // 外发光层（多层模糊效果）- 更强的发光
    for (i in 4 downTo 1) {
        val glowAlpha = 0.2f * i
        val glowExpand = 10f * i
        
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF00D4FF).copy(alpha = glowAlpha), // 亮蓝
                    Color(0xFFB47FE5).copy(alpha = glowAlpha), // 紫
                    Color(0xFFFF6B9D).copy(alpha = glowAlpha), // 粉
                    Color(0xFFFFB84D).copy(alpha = glowAlpha), // 橙
                    Color(0xFFFFE66D).copy(alpha = glowAlpha)  // 黄
                ),
                endX = progressWidth
            ),
            topLeft = Offset(-glowExpand / 2, progressY - glowExpand / 2),
            size = Size(progressWidth + glowExpand, progressHeight + glowExpand),
            cornerRadius = CornerRadius(progressCornerRadius + glowExpand / 2)
        )
    }
    
    // 主进度条渐变 - 更鲜艳的彩虹色
    val mainGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00D4FF), // 亮蓝
            Color(0xFF8B5CF6), // 紫
            Color(0xFFFF6B9D), // 粉
            Color(0xFFFFB84D), // 橙
            Color(0xFFFFE66D)  // 黄
        ),
        endX = progressWidth
    )
    
    drawRoundRect(
        brush = mainGradient,
        topLeft = Offset(0f, progressY),
        size = Size(progressWidth, progressHeight),
        cornerRadius = CornerRadius(progressCornerRadius)
    )
    
    // 流光动画层 - 更明显的流光
    val shimmerGradient = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.5f),
            Color.Transparent
        ),
        startX = progressWidth * (shimmerOffset - 0.2f),
        endX = progressWidth * (shimmerOffset + 0.2f)
    )
    
    drawRoundRect(
        brush = shimmerGradient,
        topLeft = Offset(0f, progressY),
        size = Size(progressWidth, progressHeight),
        cornerRadius = CornerRadius(progressCornerRadius)
    )
    
    // 顶部高光 - 增强立体感
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.Transparent
            ),
            startY = progressY,
            endY = progressY + progressHeight * 0.5f
        ),
        topLeft = Offset(0f, progressY),
        size = Size(progressWidth, progressHeight * 0.5f),
        cornerRadius = CornerRadius(progressCornerRadius)
    )
    
    // 在实际进度位置添加一个光标标记
    if (progress > 0f) {
        val actualProgressX = width * progress
        
        // 光标发光效果
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                radius = 25f
            ),
            radius = 25f,
            center = Offset(actualProgressX, height / 2)
        )
        
        // 光标核心
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(actualProgressX, height / 2)
        )
    }
}

/**
 * 绘制星光粒子层 - 密集的星空效果
 */
private fun DrawScope.drawStarParticles(
    width: Float,
    height: Float,
    particles: List<StarParticle>,
    twinkleTime: Float,
    progress: Float
) {
    particles.forEach { particle ->
        val x = width * particle.x
        val y = height * particle.y
        
        // 计算闪烁透明度
        val twinklePhase = (twinkleTime + particle.twinklePhase) % 360f
        val twinkleAlpha = (sin(Math.toRadians(twinklePhase.toDouble())).toFloat() * 0.5f + 0.5f)
        val finalAlpha = particle.alpha * twinkleAlpha
        
        // 星星大小分类
        val isLargeStar = particle.size > 2f
        val isMediumStar = particle.size > 1.5f && particle.size <= 2f
        
        // 绘制星光光晕（更大更明显）
        val haloSize = when {
            isLargeStar -> particle.size * 4f
            isMediumStar -> particle.size * 3f
            else -> particle.size * 2.5f
        }
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = finalAlpha * 0.9f),
                    Color.White.copy(alpha = finalAlpha * 0.5f),
                    Color.Transparent
                ),
                radius = haloSize
            ),
            radius = haloSize,
            center = Offset(x, y)
        )
        
        // 绘制星光核心
        drawCircle(
            color = Color.White.copy(alpha = finalAlpha),
            radius = particle.size,
            center = Offset(x, y)
        )
        
        // 十字光芒（中大星星）
        if (isMediumStar || isLargeStar) {
            val rayLength = particle.size * 2.5f
            val rayAlpha = finalAlpha * 0.7f
            
            // 横向光芒
            drawLine(
                color = Color.White.copy(alpha = rayAlpha),
                start = Offset(x - rayLength, y),
                end = Offset(x + rayLength, y),
                strokeWidth = 1f
            )
            // 纵向光芒
            drawLine(
                color = Color.White.copy(alpha = rayAlpha),
                start = Offset(x, y - rayLength),
                end = Offset(x, y + rayLength),
                strokeWidth = 1f
            )
        }
        
        // 大星星额外的斜向光芒
        if (isLargeStar) {
            val diagonalLength = particle.size * 2f
            val diagonalAlpha = finalAlpha * 0.5f
            
            // 左上到右下
            drawLine(
                color = Color.White.copy(alpha = diagonalAlpha),
                start = Offset(x - diagonalLength, y - diagonalLength),
                end = Offset(x + diagonalLength, y + diagonalLength),
                strokeWidth = 0.8f
            )
            // 右上到左下
            drawLine(
                color = Color.White.copy(alpha = diagonalAlpha),
                start = Offset(x + diagonalLength, y - diagonalLength),
                end = Offset(x - diagonalLength, y + diagonalLength),
                strokeWidth = 0.8f
            )
        }
    }
}

/**
 * 绘制骰子按钮
 */
private fun DrawScope.drawDiceButton(
    glowScale: Float,
    dotsRotation: Float
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val buttonRadius = size.width / 2f - 4.dp.toPx()
    
    // 最外层呼吸光晕
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD700).copy(alpha = 0.4f * glowScale),
                Color(0xFFFFA500).copy(alpha = 0.2f * glowScale),
                Color.Transparent
            ),
            radius = buttonRadius * 1.5f * glowScale
        ),
        radius = buttonRadius * 1.5f * glowScale,
        center = Offset(centerX, centerY)
    )
    
    // 外层金色光环
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE66D),
                Color(0xFFFFB84D),
                Color(0xFFFFA500)
            )
        ),
        radius = buttonRadius,
        center = Offset(centerX, centerY)
    )
    
    // 阴影（提升悬浮感）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.3f),
                Color.Transparent
            ),
            radius = buttonRadius * 0.9f
        ),
        radius = buttonRadius * 0.9f,
        center = Offset(centerX, centerY + 2.dp.toPx())
    )
    
    // 中层橙黄色渐变圆
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD93D),
                Color(0xFFFFB84D)
            )
        ),
        radius = buttonRadius * 0.85f,
        center = Offset(centerX, centerY)
    )
    
    // 内层白色背景
    drawCircle(
        color = Color.White,
        radius = buttonRadius * 0.7f,
        center = Offset(centerX, centerY)
    )
    
    // 白色装饰圆点（旋转）
    val dotCount = 8
    val dotOrbitRadius = buttonRadius * 0.95f
    
    repeat(dotCount) { i ->
        val angle = Math.toRadians((i * 360f / dotCount + dotsRotation).toDouble())
        val dotX = centerX + cos(angle).toFloat() * dotOrbitRadius
        val dotY = centerY + sin(angle).toFloat() * dotOrbitRadius
        
        // 圆点光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                radius = 4.dp.toPx()
            ),
            radius = 4.dp.toPx(),
            center = Offset(dotX, dotY)
        )
        
        // 圆点核心
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }
    
    // 内圈装饰线
    drawCircle(
        color = Color(0xFFFFE66D).copy(alpha = 0.3f),
        radius = buttonRadius * 0.72f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.dp.toPx())
    )
}
