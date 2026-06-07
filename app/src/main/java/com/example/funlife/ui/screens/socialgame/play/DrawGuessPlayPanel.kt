package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.ui.screens.socialgame.HubPrimaryButton
import com.example.funlife.viewmodel.DrawStrokeUi

@Composable
fun DrawGuessPlayPanel(
    play: DrawGuessPlayState,
    myPbId: String,
    players: List<DrawGuessPlayerUi>,
    pbAuthToken: String?,
    strokes: List<DrawStrokeUi>,
    clearToken: Int,
    nameByPbId: Map<String, String>,
    useLiveWs: Boolean = false,
    liveWireEnabled: Boolean = false,
    canDraw: Boolean = true,
    onStrokeChunk: (strokeId: String, seq: Int, points: List<List<Float>>, color: String, width: Float, flushNow: Boolean) -> Unit,
    onStrokeEnd: (strokeId: String, color: String, width: Float) -> Unit = { _, _, _ -> },
    onClear: () -> Unit,
    onFinishDrawing: () -> Unit,
    onSubmitGuess: (String) -> Unit,
    onContinueRound: () -> Unit,
    onHint: () -> Unit = {},
    bubbles: List<DrawGuessBubbleMessage> = emptyList(),
    onDismissBubble: (String) -> Unit = {},
    guessInput: String,
    onGuessChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDrawer = play.drawerPbId == myPbId
    val phase = DrawGuessPhase.fromWire(play.phase)
    val isDrawingPhase = phase == DrawGuessPhase.DRAWING
    val visibleWord = play.visibleWord(myPbId)

    var brush by remember(play.round) { mutableStateOf(DrawBrushState()) }

    val wordBannerLabel = when {
        phase == DrawGuessPhase.ROUND_END -> "本轮答案"
        isDrawer && isDrawingPhase -> "你正在画"
        isDrawer -> "等待猜词"
        isDrawingPhase || phase == DrawGuessPhase.GUESSING -> "猜猜看"
        else -> "对局结束"
    }
    val wordBannerText = when {
        phase == DrawGuessPhase.ROUND_END -> play.word.ifBlank { visibleWord }
        isDrawer && isDrawingPhase -> visibleWord
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DrawGuessMatchPalette.bg),
    ) {
        DrawGuessTimedTopSection(
            play = play,
            players = players,
            pbAuthToken = pbAuthToken,
            onHint = onHint,
            bubbles = bubbles,
            onDismissBubble = onDismissBubble,
        )

        if (phase != DrawGuessPhase.FINISHED) {
            DrawGuessWordBanner(
                isDrawer = isDrawer,
                word = wordBannerText,
                phaseLabel = wordBannerLabel,
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .weight(1f)
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(DrawGuessMatchPalette.canvasBg)
                .border(1.5.dp, DrawGuessMatchPalette.border, RoundedCornerShape(14.dp)),
        ) {
            DrawGuessCanvasBoard(
                committedStrokes = strokes,
                brush = brush,
                isDrawer = isDrawer,
                useLiveWs = useLiveWs,
                liveWireEnabled = liveWireEnabled,
                canDraw = canDraw,
                isDrawingPhase = isDrawingPhase,
                clearToken = clearToken,
                onStrokeChunk = onStrokeChunk,
                onStrokeEnd = onStrokeEnd,
                modifier = Modifier.fillMaxSize(),
            )

            if (isDrawer && isDrawingPhase) {
                DrawGuessCanvasClearButton(
                    onClear = onClear,
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
                DrawGuessCanvasToolOverlay(
                    brush = brush,
                    onBrushChange = { brush = it },
                    onFinish = onFinishDrawing,
                    enabled = true,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (phase == DrawGuessPhase.ROUND_END && play.round < play.maxRounds) {
                HubPrimaryButton(
                    text = "下一轮",
                    onClick = onContinueRound,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            when {
                (phase == DrawGuessPhase.DRAWING || phase == DrawGuessPhase.GUESSING) && !isDrawer -> {
                    DrawGuessInputBar(
                        value = guessInput,
                        onValueChange = onGuessChange,
                        onSend = { onSubmitGuess(guessInput) },
                        enabled = true,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                phase == DrawGuessPhase.ROUND_END -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DrawGuessMatchPalette.white)
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "等待下一轮…",
                            color = DrawGuessMatchPalette.muted,
                            fontSize = 13.sp,
                        )
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
