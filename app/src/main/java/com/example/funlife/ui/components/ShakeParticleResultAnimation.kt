// ShakeParticleResultAnimation.kt - 抖动+粒子效果的结算动画
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 结算粒子数据类
data class ResultParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val life: Float,
    val maxLife: Float
)

@Composable
fun ShakeParticleResultAnimation(
    icon: String,
    title: String,
    subtitle: String = "",
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    var particles by remember { mutableStateOf<List<ResultParticle>>(emptyList()) }
    var particleTime by remember { mutableFloatStateOf(0f) }
    
    // 抖动动画
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )
    
    val shakeRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeRotation"
    )
    
    // 缩放动画
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // 背景透明度动画
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (visible) 0.7f else 0f,
        animationSpec = tween(300),
        label = "backgroundAlpha"
    )
    
    // 生成粒子
    LaunchedEffect(Unit) {
        // 初始爆发粒子
        val initialParticles = mutableListOf<ResultParticle>()
        repeat(30) { i ->
            val angle = (i * 12f) * (Math.PI / 180f)
            val speed = Random.nextFloat() * 3f + 2f
            initialParticles.add(
                ResultParticle(
                    id = i,
                    x = 0f,
                    y = 0f,
                    vx = cos(angle).toFloat() * speed,
                    vy = sin(angle).toFloat() * speed,
                    color = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA500),
                        Color(0xFFFF6B6B),
                        Color(0xFF4ECDC4),
                        Color(0xFF95E1D3)
                    ).random(),
                    size = Random.nextFloat() * 8f + 4f,
                    life = 0f,
                    maxLife = Random.nextFloat() * 1.5f + 1f
                )
            )
        }
        particles = initialParticles
        
        // 持续生成粒子
        while (visible) {
            delay(50) // 每50ms生成一批新粒子
            particleTime += 0.05f
            
            // 添加新粒子
            val newParticles = mutableListOf<ResultParticle>()
            repeat(3) { i ->
                val angle = Random.nextFloat() * 360f * (Math.PI / 180f)
                val speed = Random.nextFloat() * 2f + 1f
                newParticles.add(
                    ResultParticle(
                        id = (particleTime * 1000).toInt() + i,
                        x = Random.nextFloat() * 40f - 20f,
                        y = Random.nextFloat() * 40f - 20f,
                        vx = cos(angle).toFloat() * speed,
                        vy = sin(angle).toFloat() * speed,
                        color = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA500),
                            Color(0xFFFF6B6B),
                            Color(0xFF4ECDC4)
                        ).random(),
                        size = Random.nextFloat() * 6f + 3f,
                        life = 0f,
                        maxLife = Random.nextFloat() * 1f + 0.5f
                    )
                )
            }
            
            // 更新粒子生命周期
            particles = (particles + newParticles).map { p ->
                p.copy(life = p.life + 0.05f)
            }.filter { p ->
                p.life < p.maxLife
            }
        }
    }
    
    // 自动关闭
    LaunchedEffect(Unit) {
        delay(3500)
        visible = false
        delay(300)
        onDismiss()
    }
    
    if (visible || backgroundAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    visible = false
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                // 粒子层
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    particles.forEach { particle ->
                        val progress = (particle.life / particle.maxLife).coerceIn(0f, 1f)
                        
                        // 粒子位置
                        val x = centerX + particle.x + particle.vx * particle.life * 50f
                        val y = centerY + particle.y + particle.vy * particle.life * 50f - (particle.life * particle.life * 30f) // 重力效果
                        
                        // 粒子透明度（淡出）
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        
                        // 绘制粒子
                        drawCircle(
                            color = particle.color.copy(alpha = alpha),
                            radius = particle.size * (1f - progress * 0.5f),
                            center = Offset(x, y)
                        )
                    }
                }
                
                // 主内容卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer {
                            translationX = shakeOffset
                            rotationZ = shakeRotation
                        },
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFF9E6),
                                        Color.White
                                    )
                                )
                            )
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 顶部星星装饰
                        Text(
                            text = "✨ 狂欢抽中 ✨",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                        
                        // 主图标（带光环）
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            // 光环效果
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                                Color(0xFFFFA500).copy(alpha = 0.1f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            
                            // 图标背景圆
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF00BCD4),
                                                Color(0xFF00ACC1)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 64.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // 标题
                        Text(
                            text = title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Center
                        )
                        
                        // 副标题
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7F8C8D),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        // 底部星星
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(5) {
                                Text(text = "⭐", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
