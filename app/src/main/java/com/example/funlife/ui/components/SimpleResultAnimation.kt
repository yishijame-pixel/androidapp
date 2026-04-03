// SimpleResultAnimation.kt - 抖动卡片 + 明显粒子
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.SpinWheelMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SimpleResultAnimation(
    result: String,
    mode: SpinWheelMode,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    val overallAlpha = remember { Animatable(1f) }
    
    // 抖动效果 - 只抖0.3秒就停止
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // 快速抖动3次（每次0.1秒）
        launch {
            repeat(3) {
                shakeOffset.animateTo(8f, tween(50))
                shakeOffset.animateTo(-8f, tween(50))
            }
            shakeOffset.animateTo(0f, tween(50))  // 回到中心，停止
        }
        
        // 展示2秒
        delay(2000)
        overallAlpha.animateTo(0f, tween(200))
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overallAlpha.value },
            Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            )
            
            // 先显示卡片（在粒子下面）
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = shakeOffset.value
                }
            ) {
                StaticCard(result = result, mode = mode)
            }
            
            // 粒子层（在卡片上面，更明显）
            Box(
                modifier = Modifier.size(700.dp),
                contentAlignment = Alignment.Center
            ) {
                repeat(12) { i ->
                    BigGiftParticle(
                        angle = i * 30f,
                        startRadius = 150f,
                        endRadius = 300f
                    )
                }
            }
        }
    }
}

@Composable
fun BigGiftParticle(
    angle: Float,
    startRadius: Float,
    endRadius: Float
) {
    val radius = remember { Animatable(startRadius) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1.2f) }
    
    LaunchedEffect(Unit) {
        launch {
            radius.animateTo(endRadius, tween(1400, easing = FastOutSlowInEasing))
        }
        launch {
            delay(700)
            alpha.animateTo(0f, tween(700))
        }
        launch {
            scale.animateTo(1.8f, tween(1400, easing = LinearOutSlowInEasing))
        }
    }
    
    val angleRad = Math.toRadians(angle.toDouble())
    val offsetX = (radius.value * cos(angleRad)).toFloat()
    val offsetY = (radius.value * sin(angleRad)).toFloat()
    
    Text(
        text = "🎁",
        fontSize = 32.sp,  // 更大的粒子
        modifier = Modifier.graphicsLayer {
            translationX = offsetX
            translationY = offsetY
            this.alpha = alpha.value
            scaleX = scale.value
            scaleY = scale.value
        }
    )
}

@Composable
fun StaticCard(
    result: String,
    mode: SpinWheelMode
) {
    val (bgColors, accentColor) = when (mode) {
        SpinWheelMode.NORMAL -> Pair(
            listOf(Color(0xFFE3F2FD), Color(0xFF90CAF9)),
            Color(0xFF1976D2)
        )
        SpinWheelMode.ADVANCED -> Pair(
            listOf(Color(0xFFF3E5F5), Color(0xFFCE93D8)),
            Color(0xFF7B1FA2)
        )
        SpinWheelMode.LUCKY -> Pair(
            listOf(Color(0xFFFFF9C4), Color(0xFFFFEE58)),
            Color(0xFFF57F17)
        )
    }
    
    Card(
        modifier = Modifier
            .width(360.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = bgColors))
                .padding(40.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Text(
                    "✨ 恭喜获得 ✨",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White, bgColors[0])
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 90.sp)
                }
                
                Text(
                    result,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.15f),
                            offset = Offset(2f, 2f),
                            blurRadius = 3f
                        )
                    )
                )
            }
        }
    }
}
