// ParticleEffect.kt - 超流畅粒子效果组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * 礼物盒emoji粒子效果 - 从卡片边缘向外散发
 */
@Composable
fun GiftBoxParticles(
    particleCount: Int = 8,
    startRadius: Float = 80f,
    endRadius: Float = 180f,
    duration: Int = 1000
) {
    val particles = remember {
        List(particleCount) { i ->
            AnimatedParticle(
                angle = i * (360f / particleCount),
                radius = Animatable(startRadius),
                alpha = Animatable(1f),
                scale = Animatable(1f)
            )
        }
    }
    
    LaunchedEffect(Unit) {
        particles.forEach { particle ->
            launch {
                particle.radius.animateTo(endRadius, tween(duration, easing = FastOutSlowInEasing))
            }
            launch {
                kotlinx.coroutines.delay((duration * 0.5).toLong())
                particle.alpha.animateTo(0f, tween((duration * 0.5).toInt()))
            }
            launch {
                particle.scale.animateTo(1.3f, tween(duration, easing = LinearOutSlowInEasing))
            }
        }
    }
    
    Box(
        modifier = Modifier.size((endRadius * 2.5f).dp),
        contentAlignment = Alignment.Center
    ) {
        particles.forEach { particle ->
            val angleRad = Math.toRadians(particle.angle.toDouble())
            val offsetX = (particle.radius.value * cos(angleRad)).toFloat()
            val offsetY = (particle.radius.value * sin(angleRad)).toFloat()
            
            Text(
                text = "🎁",
                fontSize = 24.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = offsetX
                    translationY = offsetY
                    alpha = particle.alpha.value
                    scaleX = particle.scale.value
                    scaleY = particle.scale.value
                }
            )
        }
    }
}

// 粒子数据类（避免与 AnimationEffects.kt 中的 Particle 冲突）
private data class AnimatedParticle(
    val angle: Float,
    val radius: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>
)
