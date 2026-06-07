package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class DrawGuessBubbleMessage(
    val bubbleId: String,
    val playerPbId: String,
    val text: String,
    val isCorrect: Boolean,
    val timestamp: Long,
    val durationMs: Long = 3500L,
)

/** 气泡尾巴朝向：指向下方头像 */
enum class BubbleTailAlign {
    Start,
    Center,
    End,
}

fun bubbleTailAlignForSlot(index: Int, total: Int): BubbleTailAlign = when {
    total <= 1 -> BubbleTailAlign.Center
    index == 0 -> BubbleTailAlign.End
    index == total - 1 -> BubbleTailAlign.Start
    else -> BubbleTailAlign.Center
}

/**
 * 钉在单个玩家头像正上方的气泡栈（本地坐标系，无全局 offset 误差）。
 */
@Composable
fun DrawGuessPlayerBubbleStack(
    bubbles: List<DrawGuessBubbleMessage>,
    tailAlign: BubbleTailAlign,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bubbles.isEmpty()) return
    val visibleBubbles = remember(bubbles) {
        bubbles.sortedBy { it.timestamp }.takeLast(3)
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = when (tailAlign) {
            BubbleTailAlign.Start -> Alignment.Start
            BubbleTailAlign.Center -> Alignment.CenterHorizontally
            BubbleTailAlign.End -> Alignment.End
        },
    ) {
        visibleBubbles.forEachIndexed { index, bubble ->
            DrawGuessBubbleItem(
                bubble = bubble,
                stackIndex = index,
                tailAlign = tailAlign,
                onDismiss = { onDismiss(bubble.bubbleId) },
            )
        }
    }
}

@Composable
fun DrawGuessBubbleItem(
    bubble: DrawGuessBubbleMessage,
    stackIndex: Int,
    tailAlign: BubbleTailAlign,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember(bubble.bubbleId) { mutableStateOf(true) }
    val scale = remember(bubble.bubbleId) { Animatable(0.3f) }
    val shimmer = remember(bubble.bubbleId) { Animatable(0f) }

    LaunchedEffect(bubble.bubbleId) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
        if (bubble.isCorrect) {
            shimmer.animateTo(1f, animationSpec = tween(400))
            delay(200)
            shimmer.animateTo(0f, animationSpec = tween(300))
        }
        delay(bubble.durationMs)
        visible = false
        delay(400)
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(200)) + scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300),
        ),
        exit = fadeOut(tween(300)) + scaleOut(
            targetScale = 0.7f,
            animationSpec = tween(300),
        ) + slideOutVertically(
            targetOffsetY = { -it / 3 },
            animationSpec = tween(300),
        ),
    ) {
        SpeechBubbleContent(
            text = bubble.text,
            isCorrect = bubble.isCorrect,
            shimmerAlpha = shimmer.value,
            tailAlign = tailAlign,
            onClick = {
                visible = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun SpeechBubbleContent(
    text: String,
    isCorrect: Boolean,
    shimmerAlpha: Float,
    tailAlign: BubbleTailAlign,
    onClick: () -> Unit,
) {
    val bgColor = if (isCorrect) Color(0xFF4CAF50) else Color.White
    val borderColor = if (isCorrect) Color(0xFFFFD700) else DrawGuessMatchPalette.border
    val textColor = if (isCorrect) Color.White else DrawGuessMatchPalette.ink
    val bubbleShape = RoundedCornerShape(10.dp)

    Column(
        horizontalAlignment = when (tailAlign) {
            BubbleTailAlign.Start -> Alignment.Start
            BubbleTailAlign.Center -> Alignment.CenterHorizontally
            BubbleTailAlign.End -> Alignment.End
        },
        modifier = Modifier.graphicsLayer {
            if (isCorrect && shimmerAlpha > 0f) {
                alpha = 1f + shimmerAlpha * 0.2f
                scaleX = 1f + shimmerAlpha * 0.06f
                scaleY = 1f + shimmerAlpha * 0.06f
            }
        },
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 28.dp, max = 120.dp)
                .shadow(
                    elevation = if (isCorrect) 4.dp else 2.dp,
                    shape = bubbleShape,
                )
                .clip(bubbleShape)
                .background(bgColor)
                .border(
                    width = if (isCorrect) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = bubbleShape,
                )
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                )
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isCorrect) {
                    Text("✅", fontSize = 11.sp)
                }
                Text(
                    text = text,
                    fontSize = 11.sp,
                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BubbleTail(
            align = tailAlign,
            fillColor = bgColor,
            borderColor = borderColor,
        )
    }
}

@Composable
private fun BubbleTail(
    align: BubbleTailAlign,
    fillColor: Color,
    borderColor: Color,
) {
    val tailOffsetX = when (align) {
        BubbleTailAlign.Start -> 8.dp
        BubbleTailAlign.Center -> 0.dp
        BubbleTailAlign.End -> (-8).dp
    }
    Box(
        modifier = Modifier.offset(x = tailOffsetX, y = (-1).dp),
        contentAlignment = when (align) {
            BubbleTailAlign.Start -> Alignment.TopStart
            BubbleTailAlign.Center -> Alignment.TopCenter
            BubbleTailAlign.End -> Alignment.TopEnd
        },
    ) {
        Canvas(modifier = Modifier.size(width = 10.dp, height = 7.dp)) {
            val path = Path()
            when (align) {
                BubbleTailAlign.End -> {
                    path.moveTo(size.width, 0f)
                    path.lineTo(size.width * 0.35f, size.height)
                    path.lineTo(size.width * 0.65f, 0f)
                }
                BubbleTailAlign.Start -> {
                    path.moveTo(0f, 0f)
                    path.lineTo(size.width * 0.35f, size.height)
                    path.lineTo(size.width * 0.65f, 0f)
                }
                BubbleTailAlign.Center -> {
                    path.moveTo(size.width * 0.2f, 0f)
                    path.lineTo(size.width * 0.5f, size.height)
                    path.lineTo(size.width * 0.8f, 0f)
                }
            }
            path.close()
            drawPath(path, fillColor)
            drawPath(
                Path().apply {
                    when (align) {
                        BubbleTailAlign.End -> {
                            moveTo(size.width, 0f)
                            lineTo(size.width * 0.35f, size.height)
                        }
                        BubbleTailAlign.Start -> {
                            moveTo(0f, 0f)
                            lineTo(size.width * 0.35f, size.height)
                        }
                        BubbleTailAlign.Center -> {
                            moveTo(size.width * 0.2f, 0f)
                            lineTo(size.width * 0.5f, size.height)
                        }
                    }
                },
                borderColor,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

object DrawGuessBubbleManager {
    private const val MAX_BUBBLES_PER_PLAYER = 3
    private const val MAX_TOTAL_BUBBLES = 12
    private const val DUPLICATE_THRESHOLD_MS = 1000L

    fun addBubble(
        existing: List<DrawGuessBubbleMessage>,
        playerPbId: String,
        text: String,
        isCorrect: Boolean,
    ): List<DrawGuessBubbleMessage> {
        if (playerPbId.isBlank() || text.isBlank()) return existing

        val now = System.currentTimeMillis()
        val trimmedText = text.take(50)
        val existingIdx = existing.indexOfFirst { bubble ->
            bubble.playerPbId == playerPbId && bubble.text == trimmedText
        }
        if (existingIdx >= 0) {
            val existingBubble = existing[existingIdx]
            if (isCorrect && !existingBubble.isCorrect) {
                return existing.toMutableList().apply {
                    this[existingIdx] = existingBubble.copy(
                        isCorrect = true,
                        durationMs = 4000L,
                    )
                }
            }
            if (now - existingBubble.timestamp < DUPLICATE_THRESHOLD_MS) return existing
        }

        val newBubble = DrawGuessBubbleMessage(
            bubbleId = "bubble_${playerPbId}_${now}",
            playerPbId = playerPbId,
            text = trimmedText,
            isCorrect = isCorrect,
            timestamp = now,
            durationMs = if (isCorrect) 4000L else 3500L,
        )

        val playerBubbles = existing.filter { it.playerPbId == playerPbId }
        val toRemove = if (playerBubbles.size >= MAX_BUBBLES_PER_PLAYER) {
            playerBubbles.sortedBy { it.timestamp }.take(1).map { it.bubbleId }.toSet()
        } else {
            emptySet()
        }

        return (existing.filterNot { it.bubbleId in toRemove } + newBubble)
            .sortedBy { it.timestamp }
            .takeLast(MAX_TOTAL_BUBBLES)
    }

    fun removeBubble(
        existing: List<DrawGuessBubbleMessage>,
        bubbleId: String,
    ): List<DrawGuessBubbleMessage> = existing.filterNot { it.bubbleId == bubbleId }

    fun clearPlayerBubbles(
        existing: List<DrawGuessBubbleMessage>,
        playerPbId: String,
    ): List<DrawGuessBubbleMessage> = existing.filterNot { it.playerPbId == playerPbId }

    fun clearAll(): List<DrawGuessBubbleMessage> = emptyList()

    fun forPlayer(
        bubbles: List<DrawGuessBubbleMessage>,
        playerPbId: String,
    ): List<DrawGuessBubbleMessage> = bubbles.filter { it.playerPbId == playerPbId }
}
