package com.example.funlife.ui.screens.socialgame.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.ui.screens.socialgame.CenteredBusyOverlay
import com.example.funlife.ui.screens.socialgame.CenteredConfirmDialog
import com.example.funlife.ui.screens.socialgame.SocialGameEnterLoading
import com.example.funlife.ui.screens.socialgame.HubPrimaryButton
import com.example.funlife.ui.screens.socialgame.HubSecondaryButton
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import com.example.funlife.ui.screens.socialgame.SocialGameScaffold
import com.example.funlife.ui.screens.socialgame.SocialGameToastHost
import com.example.funlife.social.game.SyncState
import com.example.funlife.viewmodel.GamePlayViewModel
import com.example.funlife.viewmodel.GomokuPlacementSyncState

@Composable
fun GamePlayScreen(
    roomId: String,
    viewModel: GamePlayViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLobby: () -> Unit,
    onNavigateToGameCenter: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    var guessInput by remember(roomId) { mutableStateOf("") }
    var showExitConfirm by remember(roomId) { mutableStateOf(false) }
    val entry = remember(ui.gameId) { SocialGameCatalog.find(ui.gameId) }
    val title = entry?.title ?: "对局中"
    val identityOk = ui.identityReady || !ui.myPbId.isNullOrBlank()
    val playReady = when (ui.gameId) {
        "gomoku" -> ui.gomoku != null && identityOk
        "draw_guess" -> ui.drawGuess != null && identityOk
        else -> identityOk && ui.gameId.isNotBlank()
    }
    LaunchedEffect(roomId) {
        viewModel.kickBootstrap()
    }
    val showBootstrapLoading =
        ui.status == GameRoomStatus.PLAYING && !ui.bootstrapComplete
    val showResyncOverlay = ui.bootstrapComplete && ui.status == GameRoomStatus.PLAYING &&
        (!playReady || ui.busy)
    val canExitPlay = ui.status == GameRoomStatus.PLAYING
    val confirmExit = {
        showExitConfirm = false
        viewModel.exitPlayToCenter(onNavigateToGameCenter)
    }
    val requestExit = { showExitConfirm = true }

    BackHandler(enabled = canExitPlay) { requestExit() }

    if (showBootstrapLoading) {
        SocialGameEnterLoading(
            gameId = ui.gameId.ifBlank { "draw_guess" },
            gameTitle = title,
            gameEmoji = entry?.iconEmoji ?: "🎮",
            headline = "正在同步$title",
            phaseLabel = viewModel.bootstrapPhaseLabel(ui),
            subtitle = viewModel.bootstrapSubtitle(ui),
            progressPercent = ui.bootstrapProgress,
            blockBack = false,
        )
        SocialGameToastHost(
            toast = ui.toast,
            onDismiss = viewModel::consumeToast,
        )
        if (showExitConfirm) {
            PlayExitConfirmDialog(
                onConfirm = confirmExit,
                onDismiss = { showExitConfirm = false },
            )
        }
        return
    }

    SocialGameScaffold(
        title = title,
        onNavigateBack = if (canExitPlay) requestExit else onNavigateBack,
        compactHeader = ui.gameId == "draw_guess" && ui.status == GameRoomStatus.PLAYING,
    ) {
        val drawGuessPlaying = ui.gameId == "draw_guess" && ui.status == GameRoomStatus.PLAYING
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when {
                        drawGuessPlaying -> Modifier
                        else -> Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    },
                ),
        ) {
            if (!drawGuessPlaying) {
                TurnBanner(
                    currentTurnPbId = ui.currentTurnPbId,
                    myPbId = ui.myPbId,
                    status = ui.status,
                    winnerPbId = ui.winnerPbId,
                    pendingPlacement = ui.pendingPlacement,
                )
            }
            SyncReconnectBanner(syncState = ui.syncState)

            if (!ui.identityReady) {
                ui.identityError?.let { err ->
                    IdentityNotice(
                        message = err,
                        onRetry = viewModel::ensureIdentity,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            when (ui.gameId) {
                "gomoku" -> {
                    val g = ui.gomoku
                    if (g != null) {
                        val players = remember(
                            ui.room?.roomId,
                            g.blackPbId,
                            g.whitePbId,
                            ui.room?.hostAvatarUrl,
                            ui.room?.guestAvatarUrl,
                            ui.room?.peerAvatarUrl,
                            ui.room?.hostDisplayName,
                            ui.room?.guestDisplayName,
                            ui.room?.peerDisplayName,
                            ui.room?.members,
                            ui.myPbId,
                            ui.myDisplayName,
                            ui.myLocalAvatarUri,
                        ) {
                            buildGomokuPlayers(
                                room = ui.room,
                                gomoku = g,
                                myPbId = ui.myPbId,
                                myDisplayName = ui.myDisplayName,
                                myLocalAvatarUri = ui.myLocalAvatarUri,
                            )
                        }
                        players?.let { (black, white) ->
                            GomokuPlayerBar(
                                black = black,
                                white = white,
                                currentTurnPbId = ui.currentTurnPbId,
                                pbAuthToken = ui.pbAuthToken,
                                timer = g.timer,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        val last = g.lastMove?.let { it.x to it.y }
                        val myTurn = ui.status == GameRoomStatus.PLAYING &&
                            ui.identityReady &&
                            ui.currentTurnPbId != null &&
                            ui.myPbId != null &&
                            ui.currentTurnPbId == ui.myPbId
                        GomokuBoardWithForbidden(
                            board = g.board,
                            lastMove = last,
                            enabled = myTurn && !ui.busy &&
                                ui.pendingPlacement?.state != GomokuPlacementSyncState.Sending,
                            onCellClick = { x, y -> viewModel.placeGomoku(x, y) },
                            modifier = Modifier.padding(top = 12.dp),
                            showForbidden = g.forbiddenEnabled,
                            pendingPlacement = ui.pendingPlacement,
                        )
                        ui.pendingPlacement?.takeIf { it.state == GomokuPlacementSyncState.Failed }?.let { failed ->
                            FailedPlacementBanner(
                                message = failed.errorMessage ?: "落子同步失败",
                                onRetry = viewModel::retryFailedPlacement,
                                onDismiss = viewModel::dismissFailedPlacement,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                "draw_guess" -> {
                    ui.drawGuess?.let { play ->
                        val drawStrokes by remember {
                            derivedStateOf { ui.drawStrokes }
                        }
                        val drawClearToken by remember {
                            derivedStateOf { ui.drawClearToken }
                        }
                        val onStrokeChunk = remember(viewModel) {
                            { strokeId: String, _: Int, pts: List<List<Float>>, color: String, width: Float, flushNow: Boolean ->
                                viewModel.submitDrawStrokeLive(strokeId, pts, color, width, flushNow)
                            }
                        }
                        val onStrokeEnd = remember(viewModel) {
                            { strokeId: String, color: String, width: Float ->
                                viewModel.finishDrawStrokeWs(strokeId, color, width)
                            }
                        }
                        val onClearCanvas = remember(viewModel) { { viewModel.clearDrawCanvas() } }
                        val onFinishDrawing = remember(viewModel) { { viewModel.finishDrawing() } }
                        val onContinueRound = remember(viewModel) { { viewModel.continueAfterRound() } }
                        val onHint = remember(viewModel) { { viewModel.requestDrawGuessHint() } }
                        val onDismissBubble = remember(viewModel) { viewModel::dismissDrawGuessBubble }
                        val playerList = remember(
                            ui.room?.roomId,
                            play.drawerPbId,
                            play.scores,
                            ui.room?.hostPbId,
                            ui.room?.guestPbId,
                            ui.room?.members,
                            ui.myPbId,
                        ) {
                            buildDrawGuessPlayerList(
                                room = ui.room,
                                play = play,
                                myPbId = ui.myPbId,
                                myDisplayName = ui.myDisplayName,
                                myLocalAvatarUri = ui.myLocalAvatarUri,
                            )
                        }
                        val nameByPbId = remember(playerList, ui.room?.roomId) {
                            playerList.associate { it.pbId to it.displayName }
                        }
                        val wsTransport by com.example.funlife.social.drawws.DrawGuessLiveSync.transport
                            .collectAsStateWithLifecycle(
                                initialValue = com.example.funlife.social.drawws.DrawGuessLiveSync.Transport.POCKETBASE,
                            )
                        val liveWireEnabled = com.example.funlife.social.drawws.DrawWsConfig.liveWireEnabled()
                        val useLiveWs = liveWireEnabled &&
                            wsTransport == com.example.funlife.social.drawws.DrawGuessLiveSync.Transport.WEBSOCKET
                        DrawGuessPlayPanel(
                            play = play,
                            myPbId = ui.myPbId.orEmpty(),
                            players = playerList,
                            pbAuthToken = ui.pbAuthToken,
                            strokes = drawStrokes,
                            clearToken = drawClearToken,
                            nameByPbId = nameByPbId,
                            useLiveWs = useLiveWs,
                            liveWireEnabled = liveWireEnabled,
                            canDraw = ui.bootstrapComplete &&
                                play.drawerPbId == ui.myPbId.orEmpty() &&
                                DrawGuessPhase.fromWire(play.phase) == DrawGuessPhase.DRAWING,
                            onStrokeChunk = onStrokeChunk,
                            onStrokeEnd = onStrokeEnd,
                            onClear = onClearCanvas,
                            onFinishDrawing = onFinishDrawing,
                            onSubmitGuess = { text ->
                                viewModel.submitGuess(text)
                                guessInput = ""
                            },
                            onContinueRound = onContinueRound,
                            onHint = onHint,
                            bubbles = ui.drawGuessBubbles,
                            onDismissBubble = onDismissBubble,
                            guessInput = guessInput,
                            onGuessChange = { guessInput = it },
                            modifier = if (drawGuessPlaying) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.padding(top = 8.dp)
                            },
                        )
                        ui.pendingFailedStroke?.let { failed ->
                            FailedPlacementBanner(
                                message = failed.errorMessage ?: "笔画同步失败",
                                onRetry = viewModel::retryFailedStroke,
                                onDismiss = viewModel::dismissFailedStroke,
                                retryLabel = "重试笔画",
                                dismissLabel = "撤销笔画",
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        "该游戏对局页尚未实现",
                        color = SocialGamePalette.inkMuted,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            if (!drawGuessPlaying) {
                Spacer(Modifier.height(16.dp))

                when (ui.status) {
                    GameRoomStatus.FINISHED -> {
                        ResultPanel(
                            winnerPbId = ui.winnerPbId,
                            myPbId = ui.myPbId,
                            drawGuessScores = ui.drawGuess?.scores,
                        )
                        HubPrimaryButton(
                            text = "返回趣玩中心",
                            onClick = onNavigateToGameCenter,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    GameRoomStatus.PLAYING -> {
                        HubSecondaryButton(
                            text = "退出对局",
                            onClick = requestExit,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        HubSecondaryButton(
                            text = "回到大厅",
                            onClick = onNavigateToLobby,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        CenteredBusyOverlay(
            when {
                ui.busy -> "同步中…"
                showResyncOverlay && !playReady -> "同步中…"
                else -> null
            },
        )
    }

    if (showExitConfirm) {
        PlayExitConfirmDialog(
            onConfirm = confirmExit,
            onDismiss = { showExitConfirm = false },
        )
    }

    SocialGameToastHost(
        toast = ui.toast,
        onDismiss = viewModel::consumeToast,
    )
}

@Composable
private fun PlayExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenteredConfirmDialog(
        title = "退出对局",
        message = "确定要退出本局吗？将判你负，对手获胜，并返回趣玩中心。",
        confirmText = "确定退出",
        dismissText = "继续对局",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun IdentityNotice(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
    ) {
        Text(
            message,
            color = SocialGamePalette.accentCoral,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        HubSecondaryButton(
            text = "重试连接",
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun SyncReconnectBanner(syncState: SyncState) {
    val text = when (syncState) {
        SyncState.CONNECTING -> "正在连接…"
        SyncState.RECONNECTING -> "正在重连…"
        else -> return
    }
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        textAlign = TextAlign.Center,
        color = SocialGamePalette.accentCoral,
        fontSize = 13.sp,
    )
}

@Composable
private fun FailedPlacementBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    retryLabel: String = "重试落子",
    dismissLabel: String = "撤销本手",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            message,
            color = SocialGamePalette.accentCoral,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        HubPrimaryButton(
            text = retryLabel,
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        HubSecondaryButton(
            text = dismissLabel,
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun TurnBanner(
    currentTurnPbId: String?,
    myPbId: String?,
    status: GameRoomStatus,
    winnerPbId: String?,
    pendingPlacement: com.example.funlife.viewmodel.GomokuPendingPlacement? = null,
) {
    val text = when {
        pendingPlacement?.state == GomokuPlacementSyncState.Sending -> "落子同步中…"
        status == GameRoomStatus.FINISHED && winnerPbId != null ->
            if (winnerPbId == myPbId) "你赢了！" else "本局结束"
        status == GameRoomStatus.FINISHED -> "平局"
        currentTurnPbId != null && myPbId != null && currentTurnPbId == myPbId -> "轮到你"
        else -> "等待对方…"
    }
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = SocialGamePalette.accentPurple,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
}

@Composable
private fun ResultPanel(
    winnerPbId: String?,
    myPbId: String?,
    drawGuessScores: Map<String, Int>?,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            when {
                drawGuessScores != null -> {
                    val myScore = myPbId?.let { drawGuessScores[it] } ?: 0
                    val oppScore = drawGuessScores.filterKeys { it != myPbId }.values.firstOrNull() ?: 0
                    when {
                        myScore > oppScore -> "你赢了！"
                        myScore < oppScore -> "惜败"
                        else -> "平局"
                    }
                }
                winnerPbId == myPbId -> "恭喜获胜！"
                winnerPbId != null -> "本局失利"
                else -> "平局"
            },
            color = SocialGamePalette.inkPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
