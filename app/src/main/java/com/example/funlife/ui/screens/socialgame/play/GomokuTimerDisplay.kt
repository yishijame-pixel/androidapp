package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.social.game.engine.GomokuTimer
import com.example.funlife.social.game.model.GomokuTimerState
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import kotlinx.coroutines.delay

/**
 * 五子棋计时器显示组件
 */
@Composable
fun GomokuTimerDisplay(
    timer: GomokuTimerState?,
    currentTurnColor: Char?,
    isMyTurn: Boolean,
    modifier: Modifier = Modifier,
) {
    if (timer == null || !timer.enabled) return

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // 每 100ms 更新一次
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            nowMs = System.currentTimeMillis()
        }
    }

    val blackRemaining = if (currentTurnColor == GomokuRules.CELL_BLACK) {
        GomokuTimer.getCurrentRemaining(timer, GomokuRules.CELL_BLACK, nowMs)
    } else {
        timer.blackRemainingMs
    }

    val whiteRemaining = if (currentTurnColor == GomokuRules.CELL_WHITE) {
        GomokuTimer.getCurrentRemaining(timer, GomokuRules.CELL_WHITE, nowMs)
    } else {
        timer.whiteRemainingMs
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerChip(
            label = "黑",
            remainingMs = blackRemaining,
            isActive = currentTurnColor == GomokuRules.CELL_BLACK,
            stoneColor = Color(0xFF1A1A2E),
        )
        Text(
            "⏱",
            fontSize = 16.sp,
            color = SocialGamePalette.inkMuted,
        )
        TimerChip(
            label = "白",
            remainingMs = whiteRemaining,
            isActive = currentTurnColor == GomokuRules.CELL_WHITE,
            stoneColor = Color.White,
            stoneBorder = Color(0xFF555555),
        )
    }
}

@Composable
private fun TimerChip(
    label: String,
    remainingMs: Long,
    isActive: Boolean,
    stoneColor: Color,
    stoneBorder: Color? = null,
    modifier: Modifier = Modifier,
) {
    val isDanger = GomokuTimer.isDanger(remainingMs)
    val isCritical = GomokuTimer.isCritical(remainingMs)

    // 紧急时闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "timerBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical && isActive) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCritical && isActive -> SocialGamePalette.accentCoral.copy(alpha = 0.15f)
            isDanger && isActive -> SocialGamePalette.accentGold.copy(alpha = 0.12f)
            isActive -> SocialGamePalette.accentPurple.copy(alpha = 0.10f)
            else -> Color.White.copy(alpha = 0.65f)
        },
        label = "timerBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isCritical && isActive -> SocialGamePalette.accentCoral
            isDanger && isActive -> SocialGamePalette.accentGold
            isActive -> SocialGamePalette.accentPurple
            else -> SocialGamePalette.glassBorder
        },
        label = "timerBorder",
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isCritical -> SocialGamePalette.accentCoral
            isDanger -> SocialGamePalette.accentGold
            else -> SocialGamePalette.inkPrimary
        },
        label = "timerText",
    )

    val timeText = if (isCritical) {
        GomokuTimer.formatTimeWithMs(remainingMs)
    } else {
        GomokuTimer.formatTime(remainingMs)
    }

    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .alpha(if (isCritical && isActive) blinkAlpha else 1f)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 棋子颜色标识
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(stoneColor)
                .then(
                    if (stoneBorder != null) {
                        Modifier.border(1.dp, stoneBorder, RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    },
                ),
        )

        // 时间显示
        Text(
            text = timeText,
            color = textColor,
            fontSize = if (isCritical) 18.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/**
 * 紧凑版计时器（用于 PlayerBar）
 */
@Composable
fun CompactTimerBadge(
    remainingMs: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val isDanger = GomokuTimer.isDanger(remainingMs)
    val isCritical = GomokuTimer.isCritical(remainingMs)

    val infiniteTransition = rememberInfiniteTransition(label = "compactBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical && isActive) 0.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "compactBlinkAlpha",
    )

    val bgColor = when {
        isCritical -> SocialGamePalette.accentCoral.copy(alpha = 0.15f)
        isDanger -> SocialGamePalette.accentGold.copy(alpha = 0.12f)
        isActive -> SocialGamePalette.accentPurple.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val textColor = when {
        isCritical -> SocialGamePalette.accentCoral
        isDanger -> SocialGamePalette.accentGold
        isActive -> SocialGamePalette.accentPurple
        else -> SocialGamePalette.inkMuted
    }

    Box(
        modifier = modifier
            .alpha(if (isCritical && isActive) blinkAlpha else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = GomokuTimer.formatTime(remainingMs),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
    }
}
