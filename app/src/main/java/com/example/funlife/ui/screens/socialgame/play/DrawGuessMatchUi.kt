package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.example.funlife.social.game.model.DrawGuessGuess
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.ui.screens.socialgame.SocialGameAvatar
import kotlinx.coroutines.delay

/** 参考设计稿色板 */
object DrawGuessMatchPalette {
    val bg = Color(0xFFFDF7F0)
    val white = Color(0xFFFFFFFF)
    val ink = Color(0xFF1A0E06)
    val coral = Color(0xFFFF5B35)
    val amber = Color(0xFFFF9F1C)
    val lime = Color(0xFF2EC4B6)
    val muted = Color(0xFF9E8A7A)
    val border = Color(0x1A1A0E06)
    val canvasBg = Color(0xFFFFFFFF)

    val coralAmber = Brush.linearGradient(listOf(coral, amber))
}

enum class DrawTool { BRUSH, ERASER }

data class DrawBrushState(
    val colorHex: String = DrawColorPalette.defaultHex,
    val brushSize: Float = 5f,
    val tool: DrawTool = DrawTool.BRUSH,
) {
    val strokeWidth: Float get() = if (tool == DrawTool.ERASER) brushSize * 2f else brushSize
    val colorForStroke: String get() = if (tool == DrawTool.ERASER) "#FFFFFF" else colorHex
    val composeColor: Color get() = DrawColorPalette.toColor(colorForStroke)
}

@Composable
fun DrawGuessTimerRing(
    seconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val danger = seconds <= 10
    val ringColor = if (danger) DrawGuessMatchPalette.coral else DrawGuessMatchPalette.lime
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val pad = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(pad, pad)
            drawArc(
                color = Color.Black.copy(alpha = 0.06f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val progress = (seconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = seconds.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (danger) DrawGuessMatchPalette.coral else DrawGuessMatchPalette.ink,
                lineHeight = 15.sp,
            )
            Text("\u79d2", fontSize = 7.sp, color = DrawGuessMatchPalette.muted, letterSpacing = 0.5.sp)
        }
    }
}

/** 倒计时状态隔离：仅本 composable 因 500ms tick 重组，不牵连画布区。 */
@Composable
fun DrawGuessTimedTopSection(
    play: DrawGuessPlayState,
    players: List<DrawGuessPlayerUi>,
    pbAuthToken: String?,
    onHint: () -> Unit,
    bubbles: List<DrawGuessBubbleMessage> = emptyList(),
    onDismissBubble: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val phase = DrawGuessPhase.fromWire(play.phase)
    val isDrawingPhase = phase == DrawGuessPhase.DRAWING
    var remainingSec by remember(play.round, play.phase, play.phaseStartedAtMs) {
        mutableIntStateOf(if (isDrawingPhase) play.drawSeconds else play.guessSeconds)
    }
    LaunchedEffect(play.round, play.phase, play.phaseStartedAtMs, play.drawSeconds, play.guessSeconds) {
        while (true) {
            if (play.phaseStartedAtMs > 0L) {
                val elapsed = ((System.currentTimeMillis() - play.phaseStartedAtMs) / 1000L).toInt()
                remainingSec = when (phase) {
                    DrawGuessPhase.DRAWING -> (play.drawSeconds - elapsed).coerceAtLeast(0)
                    DrawGuessPhase.GUESSING -> (play.guessSeconds - elapsed).coerceAtLeast(0)
                    else -> play.drawSeconds
                }
            }
            delay(500)
        }
    }
    val totalSec = when (phase) {
        DrawGuessPhase.DRAWING -> play.drawSeconds
        DrawGuessPhase.GUESSING -> play.guessSeconds
        else -> play.drawSeconds
    }
    DrawGuessTopSection(
        round = play.round,
        maxRounds = play.maxRounds,
        remainingSec = remainingSec,
        totalSec = totalSec,
        players = players,
        pbAuthToken = pbAuthToken,
        onHint = onHint,
        bubbles = bubbles,
        onDismissBubble = onDismissBubble,
        modifier = modifier,
    )
}

@Composable
fun DrawGuessTopSection(
    round: Int,
    maxRounds: Int,
    remainingSec: Int,
    totalSec: Int,
    players: List<DrawGuessPlayerUi>,
    pbAuthToken: String?,
    onHint: () -> Unit,
    bubbles: List<DrawGuessBubbleMessage> = emptyList(),
    onDismissBubble: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sorted = remember(players) { players.sortedByDescending { it.score } }
    val bubblesByPlayer = remember(bubbles) { bubbles.groupBy { it.playerPbId } }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DrawGuessMatchPalette.white)
            .border(width = 1.dp, color = DrawGuessMatchPalette.border)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(DrawGuessMatchPalette.ink)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "\u7b2c $round \u8f6e",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Text(
                    "/ \u5171 $maxRounds \u8f6e",
                    modifier = Modifier.padding(start = 6.dp),
                    fontSize = 10.sp,
                    color = DrawGuessMatchPalette.muted,
                )
            }
            DrawGuessTimerRing(seconds = remainingSec, totalSeconds = totalSec)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DrawGuessMatchPalette.amber.copy(alpha = 0.1f))
                    .border(1.dp, DrawGuessMatchPalette.amber.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onHint)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(Icons.Default.Lightbulb, null, tint = DrawGuessMatchPalette.amber, modifier = Modifier.size(11.dp))
                Text("提示", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DrawGuessMatchPalette.amber)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            sorted.forEachIndexed { index, player ->
                DrawGuessPlayerChip(
                    player = player,
                    rank = index,
                    pbAuthToken = pbAuthToken,
                    playerBubbles = bubblesByPlayer[player.pbId].orEmpty(),
                    tailAlign = bubbleTailAlignForSlot(index, sorted.size),
                    onDismissBubble = onDismissBubble,
                )
            }
        }
    }
}

@Composable
private fun DrawGuessPlayerChip(
    player: DrawGuessPlayerUi,
    rank: Int,
    pbAuthToken: String?,
    playerBubbles: List<DrawGuessBubbleMessage> = emptyList(),
    tailAlign: BubbleTailAlign = BubbleTailAlign.Center,
    onDismissBubble: (String) -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .widthIn(min = 52.dp, max = 96.dp),
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            DrawGuessPlayerBubbleStack(
                bubbles = playerBubbles,
                tailAlign = tailAlign,
                onDismiss = onDismissBubble,
            )
        }
        Box {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (player.isMe) DrawGuessMatchPalette.coralAmber
                        else Brush.linearGradient(listOf(Color(0x0F000000), Color(0x0F000000))),
                    )
                    .border(
                        width = if (player.isDrawer) 2.5.dp else if (player.isMe) 2.5.dp else 0.dp,
                        color = when {
                            player.isDrawer -> DrawGuessMatchPalette.coral
                            player.isMe -> DrawGuessMatchPalette.amber
                            else -> Color.Transparent
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SocialGameAvatar(
                    displayName = player.displayName,
                    avatarUrl = player.avatarUrl,
                    localAvatarUri = player.localAvatarUri,
                    pbAuthToken = pbAuthToken,
                    size = 26.dp,
                )
            }
            if (rank == 0 && player.score > 0) {
                Text(
                    "👑",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp),
                )
            } else if (rank in 1..2) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC0C0C0))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((rank + 1).toString(), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            if (player.isDrawer) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(DrawGuessMatchPalette.coral)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u270f", fontSize = 6.sp, color = Color.White)
                }
            }
        }
        Text(
            player.displayName,
            fontSize = 8.5.sp,
            fontWeight = if (player.isMe) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (player.isMe) DrawGuessMatchPalette.coral else DrawGuessMatchPalette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            "${player.score}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = DrawGuessMatchPalette.muted,
        )
    }
}

@Composable
fun DrawGuessWordBanner(
    isDrawer: Boolean,
    word: String,
    phaseLabel: String,
    modifier: Modifier = Modifier,
) {
    val charCount = word.length
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DrawGuessMatchPalette.coralAmber)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                phaseLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
            )
            if (word.isNotBlank()) {
                Text(
                    word,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                )
            } else if (!isDrawer) {
                Text("???", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (charCount > 0) {
                repeat(charCount.coerceAtMost(8)) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.6f)),
                    )
                }
                Text(
                    "${charCount}\u5b57",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
fun DrawGuessCanvasToolOverlay(
    brush: DrawBrushState,
    onBrushChange: (DrawBrushState) -> Unit,
    onFinish: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, DrawGuessMatchPalette.border, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawColorPalette.swatches.forEach { hex ->
                val selected = brush.colorHex.equals(hex, ignoreCase = true) && brush.tool == DrawTool.BRUSH
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(DrawColorPalette.toColor(hex))
                        .border(
                            width = if (selected) 2.dp else if (hex == "#FFFFFF") 1.dp else 0.dp,
                            color = if (selected) DrawGuessMatchPalette.ink else DrawGuessMatchPalette.border,
                            shape = CircleShape,
                        )
                        .clickable(enabled = enabled) {
                            onBrushChange(brush.copy(colorHex = hex, tool = DrawTool.BRUSH))
                        },
                )
            }
        }
        Box(modifier = Modifier.width(1.dp).height(20.dp).background(DrawGuessMatchPalette.border))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            DrawColorPalette.brushSizes.forEach { size ->
                val selected = brush.brushSize == size && brush.tool == DrawTool.BRUSH
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) DrawGuessMatchPalette.ink else Color(0x0D000000))
                        .clickable(enabled = enabled) {
                            onBrushChange(brush.copy(brushSize = size, tool = DrawTool.BRUSH))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size((size * 1.1f + 3f).coerceAtMost(14f).dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else DrawGuessMatchPalette.ink),
                    )
                }
            }
            ToolbarIcon(
                selected = brush.tool == DrawTool.ERASER,
                danger = false,
                onClick = { onBrushChange(brush.copy(tool = DrawTool.ERASER)) },
                enabled = enabled,
            ) {
                Icon(
                    Icons.Default.CropSquare,
                    contentDescription = null,
                    tint = if (brush.tool == DrawTool.ERASER) Color.White else DrawGuessMatchPalette.muted,
                    modifier = Modifier.size(12.dp),
                )
            }
            ToolbarIcon(
                selected = true,
                danger = false,
                onClick = onFinish,
                enabled = enabled,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun DrawGuessCanvasClearButton(
    onClear: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, DrawGuessMatchPalette.border, CircleShape)
            .clickable(enabled = enabled, onClick = onClear),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "清空",
            tint = DrawGuessMatchPalette.coral,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
fun DrawGuessCompactToolbar(
    brush: DrawBrushState,
    onBrushChange: (DrawBrushState) -> Unit,
    onClear: () -> Unit,
    onFinish: (() -> Unit)?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DrawGuessMatchPalette.white)
            .border(1.5.dp, DrawGuessMatchPalette.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DrawColorPalette.toColor(brush.colorHex))
                .border(2.5.dp, DrawGuessMatchPalette.border, RoundedCornerShape(10.dp)),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DrawColorPalette.swatches.forEach { hex ->
                val selected = brush.colorHex.equals(hex, ignoreCase = true) && brush.tool == DrawTool.BRUSH
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DrawColorPalette.toColor(hex))
                        .border(
                            width = if (selected) 2.5.dp else if (hex == "#FFFFFF") 1.5.dp else 0.dp,
                            color = if (selected) DrawGuessMatchPalette.ink else DrawGuessMatchPalette.border,
                            shape = CircleShape,
                        )
                        .clickable(enabled = enabled) {
                            onBrushChange(brush.copy(colorHex = hex, tool = DrawTool.BRUSH))
                        },
                )
            }
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DrawGuessMatchPalette.border))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DrawColorPalette.brushSizes.forEach { size ->
                val selected = brush.brushSize == size && brush.tool == DrawTool.BRUSH
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) DrawGuessMatchPalette.ink else Color(0x0D000000))
                        .clickable(enabled = enabled) {
                            onBrushChange(brush.copy(brushSize = size, tool = DrawTool.BRUSH))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size((size * 1.2f + 4f).coerceAtMost(18f).dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else DrawGuessMatchPalette.ink),
                    )
                }
            }
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DrawGuessMatchPalette.border))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ToolbarIcon(
                selected = brush.tool == DrawTool.ERASER,
                danger = false,
                onClick = { onBrushChange(brush.copy(tool = DrawTool.ERASER)) },
                enabled = enabled,
            ) {
                Icon(
                    Icons.Default.CropSquare,
                    contentDescription = null,
                    tint = if (brush.tool == DrawTool.ERASER) Color.White else DrawGuessMatchPalette.muted,
                    modifier = Modifier.size(14.dp),
                )
            }
            ToolbarIcon(
                selected = false,
                danger = true,
                onClick = onClear,
                enabled = enabled,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = DrawGuessMatchPalette.coral, modifier = Modifier.size(14.dp))
            }
            if (onFinish != null) {
                ToolbarIcon(
                    selected = true,
                    danger = false,
                    onClick = onFinish,
                    enabled = enabled,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ToolbarIcon(
    selected: Boolean,
    danger: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (selected && !danger) 24.dp else 24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    selected && !danger -> DrawGuessMatchPalette.ink
                    danger -> DrawGuessMatchPalette.coral.copy(alpha = 0.08f)
                    else -> Color(0x0D000000)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
fun DrawGuessChatFeed(
    guesses: List<DrawGuessGuess>,
    nameByPbId: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(guesses.size) {
        if (guesses.isNotEmpty()) listState.animateScrollToItem(guesses.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        itemsIndexed(guesses, key = { index, guess -> "msg_${index}_${guess.pbId}_${guess.text}_${guess.correct}" }) { _, guess ->
            val name = nameByPbId[guess.pbId] ?: "玩家"
            if (guess.correct) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DrawGuessMatchPalette.lime.copy(alpha = 0.12f))
                        .border(1.5.dp, DrawGuessMatchPalette.lime.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("\u2b50", fontSize = 12.sp)
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DrawGuessMatchPalette.lime)
                    Text("猜对了！", fontSize = 12.sp, color = Color(0xFF1A7A72))
                    Spacer(Modifier.weight(1f))
                    Text("🎉", fontSize = 16.sp)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x0F000000)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DrawGuessMatchPalette.ink)
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                                .background(DrawGuessMatchPalette.white)
                                .border(1.5.dp, DrawGuessMatchPalette.border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(guess.text, fontSize = 13.sp, color = DrawGuessMatchPalette.ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawGuessInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DrawGuessMatchPalette.white)
            .border(width = 1.dp, color = DrawGuessMatchPalette.border)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x0A000000))
                .border(1.5.dp, DrawGuessMatchPalette.border, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp),
            singleLine = true,
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text("\u8f93\u5165\u731c\u6d4b\u6216\u804a\u5929\u2026", color = DrawGuessMatchPalette.muted, fontSize = 14.sp)
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (value.isNotBlank() && enabled) DrawGuessMatchPalette.coralAmber
                    else Brush.linearGradient(listOf(Color(0x12000000), Color(0x12000000))),
                )
                .clickable(enabled = enabled && value.isNotBlank(), onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = "\u53d1\u9001",
                tint = if (value.isNotBlank() && enabled) Color.White else DrawGuessMatchPalette.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
