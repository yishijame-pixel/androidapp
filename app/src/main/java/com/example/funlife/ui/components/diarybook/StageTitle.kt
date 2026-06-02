// ═══════════════════════════════════════════════════════════════════════════
// StageTitle.kt — 剧场标题：金属渐变 + 呼吸光晕 + 扫光 shimmer
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.diarybook

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StageTitle(
    text: String,
    stage: BookStageTheme,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "title_shimmer")
    val shimmer by infinite.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 6.sp,
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        stage.title,
                        stage.haloCore,
                        stage.title,
                        stage.halo.copy(alpha = 0.85f),
                        stage.title,
                    ),
                    start = Offset(wPx * shimmer, 0f),
                    end = Offset(wPx * shimmer + wPx * 0.45f, 0f),
                ),
                shadow = Shadow(
                    color = stage.halo.copy(alpha = glowPulse),
                    offset = Offset(0f, 0f),
                    blurRadius = 28f,
                ),
            ),
        )
    }
}

@Composable
fun StageSubtitle(
    text: String,
    stage: BookStageTheme,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontFamily = FontFamily.Serif,
        color = stage.subtitle,
        letterSpacing = 2.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
    )
}
