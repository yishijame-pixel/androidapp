package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeItemConstants
import com.example.funlife.ui.screens.pacmaze.currentPacMazePlayLayout

data class PacMazeActiveBuff(
    val emoji: String,
    val label: String,
    val color: Color,
    val ticksLeft: Int,
    val maxTicks: Int,
    val stack: Int = 0,
)

@Composable
fun PacMazeActiveItemBuffRow(
    shieldCharges: Int,
    magnetTicksLeft: Int,
    frostTicksLeft: Int,
    speedBoostTicksLeft: Int,
    scoreBoostTicksLeft: Int,
    modifier: Modifier = Modifier,
) {
    val buffs = buildList {
        if (shieldCharges > 0) {
            add(
                PacMazeActiveBuff(
                    emoji = "🛡",
                    label = "护盾",
                    color = Color(0xFF00E5FF),
                    ticksLeft = 1,
                    maxTicks = 1,
                    stack = shieldCharges,
                ),
            )
        }
        if (magnetTicksLeft > 0) {
            add(
                PacMazeActiveBuff(
                    emoji = "🧲",
                    label = "磁力",
                    color = Color(0xFFB388FF),
                    ticksLeft = magnetTicksLeft,
                    maxTicks = PacMazeItemConstants.MAGNET_DURATION_TICKS,
                ),
            )
        }
        if (frostTicksLeft > 0) {
            add(
                PacMazeActiveBuff(
                    emoji = "❄",
                    label = "冰霜",
                    color = Color(0xFF80D8FF),
                    ticksLeft = frostTicksLeft,
                    maxTicks = PacMazeItemConstants.FROST_DURATION_TICKS,
                ),
            )
        }
        if (speedBoostTicksLeft > 0) {
            add(
                PacMazeActiveBuff(
                    emoji = "⚡",
                    label = "迅捷",
                    color = Color(0xFFFFD54F),
                    ticksLeft = speedBoostTicksLeft,
                    maxTicks = PacMazeItemConstants.SPEED_DURATION_TICKS,
                ),
            )
        }
        if (scoreBoostTicksLeft > 0) {
            add(
                PacMazeActiveBuff(
                    emoji = "✦",
                    label = "双倍",
                    color = Color(0xFF69F0AE),
                    ticksLeft = scoreBoostTicksLeft,
                    maxTicks = PacMazeItemConstants.DOUBLE_DURATION_TICKS,
                ),
            )
        }
    }
    if (buffs.isEmpty()) return

    val play = currentPacMazePlayLayout()
    val chipSize = play.dp(36.dp)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(play.dp(999.dp)))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(play.dp(999.dp)))
            .padding(horizontal = play.dp(8.dp), vertical = play.dp(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(play.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        buffs.forEach { buff ->
            ItemBuffChip(buff = buff, size = chipSize)
        }
    }
}

@Composable
private fun ItemBuffChip(
    buff: PacMazeActiveBuff,
    size: androidx.compose.ui.unit.Dp,
) {
    val progress = if (buff.maxTicks <= 0) 1f else (buff.ticksLeft.toFloat() / buff.maxTicks).coerceIn(0f, 1f)
    val seconds = (buff.ticksLeft / PacMazeConstants.TICKS_PER_SECOND.toFloat()).coerceAtLeast(1f).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val stroke = size.toPx() * 0.07f
                val radius = size.toPx() * 0.46f
                val center = androidx.compose.ui.geometry.Offset(size.toPx() / 2f, size.toPx() / 2f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius,
                    center = center,
                    style = Stroke(stroke),
                )
                drawArc(
                    color = buff.color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }

            Box(
                modifier = Modifier
                    .size(size * 0.78f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(buff.color.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.55f)),
                        ),
                    )
                    .border(1.dp, buff.color.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(buff.emoji, fontSize = (size.value * 0.34f).sp)
            }

            if (buff.stack > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(buff.color)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                ) {
                    Text(
                        buff.stack.toString(),
                        color = Color.Black,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            } else if (buff.maxTicks > 1) {
                Text(
                    "${seconds}s",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        Text(
            buff.label,
            color = buff.color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
