package com.example.funlife.ui.screens.socialgame

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * 你画我猜专属进局加载页：画笔轨迹 + 画布，与五子棋棋盘加载区分。
 * 文案由 ViewModel 按真实同步进度驱动（含 WS 连接状态）。
 */
@Composable
fun DrawGuessEnterLoadingScreen(
    gameTitle: String = "你画我猜",
    headline: String = "正在进入$gameTitle",
    phaseLabel: String? = null,
    subtitle: String? = null,
    progressPercent: Int = 0,
    modifier: Modifier = Modifier,
    blockBack: Boolean = true,
) {
    if (blockBack) {
        BackHandler { /* 加载中拦截返回 */ }
    }

    val phase = phaseLabel ?: "连接画板…"
    val hint = subtitle ?: "正在建立低延迟笔画通道"
    val progress = progressPercent.coerceIn(0, 100) / 100f

    val transition = rememberInfiniteTransition(label = "drawGuessLoad")
    val strokeProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "strokeProgress",
    )
    val pencilBob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pencilBob",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        SocialGameBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🎨", fontSize = 44.sp)
            Spacer(Modifier.height(16.dp))
            DrawGuessCanvasLoader(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp)),
                strokeProgress = strokeProgress,
                pencilBob = pencilBob,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                headline,
                color = SocialGamePalette.inkPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                phase,
                color = SocialGamePalette.accentCoral,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                hint,
                color = SocialGamePalette.inkMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "$progressPercent%",
                color = SocialGamePalette.inkPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(8.dp))
            DrawGuessSyncBar(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(6.dp),
            )
        }
    }
}

@Composable
private fun DrawGuessCanvasLoader(
    strokeProgress: Float,
    pencilBob: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = Color.White,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
        )
        drawRoundRect(
            color = Color(0xFFE8E4F0),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            style = Stroke(width = 2f),
        )

        val w = size.width
        val h = size.height
        val padX = w * 0.12f
        val padY = h * 0.2f
        val usableW = w - padX * 2
        val usableH = h - padY * 2

        val doodle = Path()
        val samples = 48
        for (i in 0..samples) {
            val t = i / samples.toFloat()
            val x = padX + usableW * t
            val y = padY + usableH * (0.45f + 0.22f * sin(t * PI * 2.2f).toFloat())
            if (i == 0) doodle.moveTo(x, y) else doodle.lineTo(x, y)
        }

        val partial = Path()
        val endIndex = (samples * strokeProgress).toInt().coerceIn(1, samples)
        for (i in 0..endIndex) {
            val t = i / samples.toFloat()
            val x = padX + usableW * t
            val y = padY + usableH * (0.45f + 0.22f * sin(t * PI * 2.2f).toFloat())
            if (i == 0) partial.moveTo(x, y) else partial.lineTo(x, y)
        }

        drawPath(
            doodle,
            color = Color(0xFFE0DCE8),
            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            partial,
            color = Color(0xFF2D2A3E),
            style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        val tipT = endIndex / samples.toFloat()
        val tipX = padX + usableW * tipT
        val tipY = padY + usableH * (0.45f + 0.22f * sin(tipT * PI * 2.2f).toFloat())
        val bob = (pencilBob - 0.5f) * 6f
        drawCircle(Color(0xFFFF6B6B), 7f, Offset(tipX, tipY + bob))
        drawCircle(Color.White, 3f, Offset(tipX - 2f, tipY + bob - 2f))
    }
}

@Composable
private fun DrawGuessSyncBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(2.dp)),
    ) {
        drawRoundRect(color = SocialGamePalette.glassBorder.copy(alpha = 0.5f))
        drawRoundRect(
            color = SocialGamePalette.accentPurple,
            size = androidx.compose.ui.geometry.Size(size.width * progress.coerceIn(0f, 1f), size.height),
        )
    }
}
