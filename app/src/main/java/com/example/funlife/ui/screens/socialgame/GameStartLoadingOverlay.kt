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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val loadingPhases = listOf(
    "正在准备棋盘…",
    "正在同步玩家…",
    "正在初始化对局…",
    "即将进入游戏…",
)

/**
 * 真·全屏进入对局加载页（覆盖大厅/空白对局页，不露出底层 UI）。
 */
@Composable
fun GameEnterLoadingScreen(
    gameTitle: String,
    gameEmoji: String,
    headline: String = "正在进入$gameTitle",
    modifier: Modifier = Modifier,
    blockBack: Boolean = true,
) {
    if (blockBack) {
        BackHandler { /* 加载中拦截返回 */ }
    }

    var phaseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900L)
            phaseIndex = (phaseIndex + 1) % loadingPhases.size
        }
    }

    val transition = rememberInfiniteTransition(label = "gameEnterPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(gameEmoji, fontSize = 52.sp)
            Spacer(Modifier.height(20.dp))
            MiniGomokuLoader(
                modifier = Modifier.size(160.dp),
                pulse = pulse,
                sweep = sweep,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                headline,
                color = SocialGamePalette.inkPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                loadingPhases[phaseIndex],
                color = SocialGamePalette.accentPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.5.dp,
                color = SocialGamePalette.accentPurple,
            )
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = sweep,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = SocialGamePalette.accentCoral,
                trackColor = SocialGamePalette.glassBorder.copy(alpha = 0.6f),
            )
        }
    }
}

/** @deprecated 使用 [GameEnterLoadingScreen] */
@Composable
fun GameStartLoadingOverlay(
    gameTitle: String,
    gameEmoji: String,
    modifier: Modifier = Modifier,
) = GameEnterLoadingScreen(
    gameTitle = gameTitle,
    gameEmoji = gameEmoji,
    modifier = modifier,
)

@Composable
private fun MiniGomokuLoader(
    modifier: Modifier = Modifier,
    pulse: Float,
    sweep: Float,
) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 5f
        val boardColor = Color(0xFFE8C896)
        val lineColor = Color(0xFF8B6914)
        drawRoundRect(
            color = boardColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
        )
        for (i in 0 until 5) {
            val p = cell * (i + 0.5f)
            drawLine(lineColor, Offset(cell * 0.5f, p), Offset(size.width - cell * 0.5f, p), 2f)
            drawLine(lineColor, Offset(p, cell * 0.5f), Offset(p, size.height - cell * 0.5f), 2f)
        }
        val stones = listOf(
            2 to 2 to Color(0xFF1A1A2E),
            2 to 3 to Color.White,
            3 to 2 to Color(0xFF1A1A2E),
        )
        stones.forEachIndexed { index, (pos, color) ->
            val (x, y) = pos
            val alpha = when {
                index < (sweep * stones.size).toInt() -> 1f
                index == (sweep * stones.size).toInt() ->
                    sweep * stones.size - (sweep * stones.size).toInt()
                else -> 0.3f
            }
            val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
            val radius = cell * 0.36f * pulse
            drawCircle(color.copy(alpha = alpha.coerceIn(0.25f, 1f)), radius, center)
            if (color == Color.White) {
                drawCircle(
                    Color(0xFF555555),
                    radius,
                    center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2f),
                )
            }
        }
    }
}
