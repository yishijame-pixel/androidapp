package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.ui.screens.socialgame.HubPrimaryButton
import com.example.funlife.ui.screens.socialgame.HubSecondaryButton
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import com.example.funlife.social.drawws.DrawGuessLiveSync
import com.example.funlife.viewmodel.DrawStrokeUi

@Composable
fun DrawGuessPlayPanel(
    play: DrawGuessPlayState,
    myPbId: String,
    strokes: List<DrawStrokeUi>,
    clearToken: Int,
    scoreLabels: Map<String, String> = emptyMap(),
    useLiveWs: Boolean = false,
    onStrokeChunk: (strokeId: String, seq: Int, points: List<List<Float>>) -> Unit,
    onStrokeEnd: (strokeId: String) -> Unit = {},
    onClear: () -> Unit,
    onFinishDrawing: () -> Unit,
    onSubmitGuess: (String) -> Unit,
    onContinueRound: () -> Unit,
    guessInput: String,
    onGuessChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDrawer = play.drawerPbId == myPbId
    val visibleWord = play.visibleWord(myPbId)
    val showWord = visibleWord.isNotBlank() && play.phase == DrawGuessPhase.DRAWING.wire
    var strokeSeq by remember(play.round, clearToken) { mutableIntStateOf(play.strokeSeq) }
    LaunchedEffect(play.strokeSeq) {
        if (play.strokeSeq > strokeSeq) strokeSeq = play.strokeSeq
    }
    var activeStrokeId by remember(play.round, clearToken) { mutableStateOf("") }
    var lastChunkIndex by remember(play.round, clearToken) { mutableIntStateOf(0) }
    var lastChunkAtMs by remember(play.round, clearToken) { mutableLongStateOf(0L) }
    val localPath = remember { mutableStateListOf<Pair<Float, Float>>() }
    val chunkIntervalMs = 16L
    val minChunkPoints = 2

    fun flushChunk(toIndex: Int, force: Boolean = false) {
        val newPoints = toIndex - lastChunkIndex
        if (!force && newPoints < minChunkPoints) return
        if (force && newPoints < 2) return
        val now = System.currentTimeMillis()
        if (!force && now - lastChunkAtMs < chunkIntervalMs) return
        val chunk = localPath.subList(lastChunkIndex, toIndex)
            .map { listOf(it.first, it.second) }
        if (chunk.size < 2) return
        val sid = activeStrokeId
        if (sid.isBlank()) return
        if (!useLiveWs) strokeSeq += 1
        onStrokeChunk(sid, if (useLiveWs) 0 else strokeSeq, chunk)
        // 保留 1 点重叠，下一片从最后确认点续画
        lastChunkIndex = (toIndex - 1).coerceAtLeast(0)
        lastChunkAtMs = now
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("第 ${play.round}/${play.maxRounds} 轮", color = SocialGamePalette.inkPrimary, fontWeight = FontWeight.Bold)
            Text(
                when (play.phase) {
                    DrawGuessPhase.DRAWING.wire -> if (isDrawer) "你来画" else "对方作画中…"
                    DrawGuessPhase.GUESSING.wire -> if (isDrawer) "等对方猜" else "请猜词"
                    DrawGuessPhase.ROUND_END.wire -> "本轮结束"
                    else -> "对局结束"
                },
                color = SocialGamePalette.accentPurple,
                fontSize = 14.sp,
            )
        }
        if (showWord) {
            Text(
                "词语：$visibleWord",
                modifier = Modifier.padding(top = 8.dp),
                color = SocialGamePalette.inkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .pointerInput(isDrawer, play.phase, clearToken) {
                    if (!isDrawer || play.phase != DrawGuessPhase.DRAWING.wire) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            localPath.clear()
                            lastChunkIndex = 0
                            lastChunkAtMs = 0L
                            activeStrokeId = DrawGuessLiveSync.newStrokeId()
                            localPath.add(offset.x / size.width to offset.y / size.height)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            localPath.add(change.position.x / size.width to change.position.y / size.height)
                            flushChunk(localPath.size, force = false)
                        },
                        onDragEnd = {
                            flushChunk(localPath.size, force = true)
                            if (activeStrokeId.isNotBlank()) {
                                onStrokeEnd(activeStrokeId)
                            }
                            localPath.clear()
                            lastChunkIndex = 0
                            activeStrokeId = ""
                        },
                    )
                },
        ) {
            val committedStrokes = if (isDrawer && activeStrokeId.isNotBlank()) {
                strokes.filter { it.strokeId != activeStrokeId }
            } else {
                strokes
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                committedStrokes.forEach { stroke ->
                    val path = Path()
                    stroke.points.forEachIndexed { i, (nx, ny) ->
                        val px = nx * size.width
                        val py = ny * size.height
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path,
                        color = parseStrokeColor(stroke.color),
                        style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                if (localPath.size >= 2) {
                    val path = Path()
                    localPath.forEachIndexed { i, (nx, ny) ->
                        val px = nx * size.width
                        val py = ny * size.height
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path,
                        color = Color(0xFF222222),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
        if (isDrawer && play.phase == DrawGuessPhase.DRAWING.wire) {
            Row(
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HubSecondaryButton(text = "清屏", onClick = onClear, modifier = Modifier.weight(1f))
                HubPrimaryButton(text = "画好了", onClick = onFinishDrawing, modifier = Modifier.weight(1f))
            }
        }
        if (!isDrawer && play.phase == DrawGuessPhase.GUESSING.wire) {
            BasicTextField(
                value = guessInput,
                onValueChange = onGuessChange,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SocialGamePalette.bgBase)
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (guessInput.isBlank()) {
                        Text("输入你的猜测…", color = SocialGamePalette.inkMuted)
                    }
                    inner()
                },
            )
            HubPrimaryButton(
                text = "提交猜测",
                onClick = { onSubmitGuess(guessInput) },
                modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                enabled = guessInput.isNotBlank(),
            )
        }
        if (play.phase == DrawGuessPhase.ROUND_END.wire) {
            val lastGuess = play.guesses.lastOrNull()
            val revealWord = play.word.ifBlank { visibleWord }
            Text(
                if (lastGuess?.correct == true) "猜对了！答案是「$revealWord」" else "本轮答案：$revealWord",
                modifier = Modifier.padding(top = 10.dp),
                color = SocialGamePalette.inkMuted,
            )
            if (play.round < play.maxRounds) {
                HubPrimaryButton(
                    text = "下一轮",
                    onClick = onContinueRound,
                    modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
                )
            }
        }
        Text(
            "比分：${formatScores(play.scores, scoreLabels)}",
            modifier = Modifier.padding(top = 8.dp),
            color = SocialGamePalette.inkSecondary,
            fontSize = 13.sp,
        )
    }
}

private fun formatScores(scores: Map<String, Int>, labels: Map<String, String>): String =
    scores.entries.joinToString(" · ") { (pbId, score) ->
        "${labels[pbId] ?: "玩家"} $score"
    }

private fun parseStrokeColor(raw: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(raw)) }
        .getOrDefault(Color(0xFF222222))
