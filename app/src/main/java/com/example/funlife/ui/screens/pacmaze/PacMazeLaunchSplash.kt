package com.example.funlife.ui.screens.pacmaze

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 进豆人迷宫前的短启动画面：遮住横竖屏切换，再柔和过渡到加载页。
 */
@Composable
fun PacMazeLaunchSplash(
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "splashAlpha",
    )
    val ringTransition = rememberInfiniteTransition(label = "splashRing")
    val ringScale by ringTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val ringAlpha by ringTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PacMazePalette.bgTop),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PacMazePalette.accentOrange.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.46f),
                    radius = size.minDimension * 0.42f,
                ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(132.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.38f * ringScale
                    drawCircle(
                        color = PacMazePalette.accentGold.copy(alpha = ringAlpha),
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        color = PacMazePalette.accentOrange,
                        radius = size.minDimension * 0.22f,
                        center = center,
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = size.minDimension * 0.035f,
                        center = Offset(center.x + size.minDimension * 0.07f, center.y - size.minDimension * 0.05f),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "豆人迷宫",
                color = PacMazePalette.inkPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "正在启动…",
                color = PacMazePalette.inkMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
