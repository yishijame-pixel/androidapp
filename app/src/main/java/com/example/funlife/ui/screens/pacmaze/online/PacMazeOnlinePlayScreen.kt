package com.example.funlife.ui.screens.pacmaze.online

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.FunLifeApplication
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.BuildConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeOnlineMatchMode
import com.example.funlife.ui.screens.pacmaze.LocalPacMazePlayLayout
import com.example.funlife.ui.screens.pacmaze.PacMazeGhostReleaseBanner
import com.example.funlife.ui.screens.pacmaze.PacMazeOverlayCard
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.ui.screens.pacmaze.PacMazePlayLayoutSpec
import com.example.funlife.ui.screens.pacmaze.PacMazePrimaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeAttackButton
import com.example.funlife.ui.screens.pacmaze.components.PacMazeCanvas
import com.example.funlife.ui.screens.pacmaze.components.PacMazeJoystickVisual
import com.example.funlife.ui.screens.pacmaze.components.PacMazePlayLoadingOverlay
import com.example.funlife.ui.screens.pacmaze.components.pacMazeJoystickInput
import com.example.funlife.ui.screens.pacmaze.components.rememberPacMazeJoystickState
import com.example.funlife.ui.screens.pacmaze.components.resetVisual
import com.example.funlife.viewmodel.PacMazeOnlineViewModel
import com.example.funlife.viewmodel.PacMazeOnlineViewModelFactory
import com.example.funlife.viewmodel.PacMazeRenderFrame
import kotlinx.coroutines.isActive

@Composable
fun PacMazeOnlinePlayScreen(
    roomId: String,
    userId: Long,
    onExit: () -> Unit,
    onBackToLobby: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: PacMazeOnlineViewModel = viewModel(
        key = "pac_online_$roomId",
        factory = PacMazeOnlineViewModelFactory(
            app = context.applicationContext as FunLifeApplication,
            userId = userId,
            roomId = roomId,
        ),
    )
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val renderFrame by viewModel.renderFrame.collectAsStateWithLifecycle()
    val joystickState = rememberPacMazeJoystickState()

    LockLandscape()

    LaunchedEffect(ui.toast) {
        ui.toast?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    if (ui.loading || ui.world == null) {
        PacMazePlayLoadingOverlay(message = ui.loadingMessage)
        return
    }

    val world = ui.world ?: renderFrame?.current ?: return
    val level = ui.levelConfig ?: return
    var renderBlend by remember(ui.loading, ui.countdown) { mutableFloatStateOf(1f) }

    LaunchedEffect(ui.avatarLoadout.skinId, ui.levelConfig?.id) {
        PacMazeRemoteSkinAnimCache.requestGameplayWarmupAsync(ui.avatarLoadout.skinId)
    }

    LaunchedEffect(ui.loading, ui.countdown) {
        if (ui.loading || ui.countdown > 0) return@LaunchedEffect
        joystickState.resetVisual()
        viewModel.resetJoystickInput()
        val stepNs = 1_000_000_000L / PacMazeConstants.TICKS_PER_SECOND
        var lastTickNs = 0L
        while (isActive) {
            withFrameNanos { frameNs ->
                viewModel.syncJoystickSample(
                    offsetX = joystickState.knobOffset.x,
                    offsetY = joystickState.knobOffset.y,
                    maxRadius = joystickState.maxRadiusPx,
                    fingerDown = joystickState.isActive,
                )
                if (viewModel.useServerInterpolation()) {
                    viewModel.advanceOnlineFrame(frameNs)
                    renderBlend = 1f
                    return@withFrameNanos
                }
                if (lastTickNs == 0L) {
                    lastTickNs = frameNs
                    renderBlend = 1f
                    viewModel.advanceOnlineFrame(frameNs)
                    return@withFrameNanos
                }
                var ticksThisFrame = 0
                while (frameNs - lastTickNs >= stepNs && ticksThisFrame < PacMazeConstants.MAX_SIM_TICKS_PER_FRAME) {
                    viewModel.advanceOnlineFrame(frameNs)
                    lastTickNs += stepNs
                    ticksThisFrame++
                }
                if (ticksThisFrame >= PacMazeConstants.MAX_SIM_TICKS_PER_FRAME &&
                    frameNs - lastTickNs >= stepNs
                ) {
                    lastTickNs = frameNs - ((frameNs - lastTickNs) % stepNs)
                }
                renderBlend = ((frameNs - lastTickNs).toFloat() / stepNs.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val frame = renderFrame ?: PacMazeRenderFrame(current = world, previous = null, blend = 1f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val playLayout = PacMazePlayLayoutSpec.compute(maxWidth = maxWidth, maxHeight = maxHeight)
        CompositionLocalProvider(LocalPacMazePlayLayout provides playLayout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .pacMazeJoystickInput(
                        joystickState = joystickState,
                        zoneWidth = playLayout.joystickZoneWidth,
                        zoneHeight = playLayout.joystickZoneHeight,
                        onSample = viewModel::onJoystickSample,
                    ),
            ) {
                PacMazeCanvas(
                    modifier = Modifier.fillMaxSize(),
                    renderFrame = frame.copy(blend = renderBlend),
                    themeId = ui.mapThemeId,
                    avatarLoadout = ui.avatarLoadout,
                    levelConfig = level,
                    onlineLocalEntityId = ui.myEntityId,
                )

                PacMazeOnlineHudBar(
                    ui = ui,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = playLayout.hudHorizontal, vertical = playLayout.hudTop),
                )

                if (frame.current.ghostReleaseTicksLeft > 0) {
                    PacMazeGhostReleaseBanner(
                        world = frame.current,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = playLayout.hudTop + playLayout.dp(44.dp)),
                    )
                }

                if (BuildConfig.DEBUG && ui.syncModeLabel.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = playLayout.hudHorizontal, top = playLayout.hudTop + playLayout.dp(36.dp)),
                    ) {
                        Text(
                            text = "${ui.syncModeLabel} · ${ui.wsStatus} · tick ${ui.lastSnapshotTick}" +
                                if (ui.rttMs > 0L) " · ${ui.rttMs}ms" else "",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                        )
                        if (ui.mapGhostReleaseSec > 0) {
                            Text(
                                text = "地图幽灵 ${ui.mapGhostReleaseSec}s 后出动",
                                color = PacMazePalette.accentOrange.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }

                if (ui.countdown > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ui.countdown.toString(),
                            color = PacMazePalette.accentGold,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                PacMazeJoystickVisual(
                    state = joystickState,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(
                            start = playLayout.joystickStart,
                            bottom = playLayout.joystickBottom,
                        )
                        .size(playLayout.joystickSize),
                )

                if (frame.current.attackCharges > 0) {
                    PacMazeAttackButton(
                        attackCharges = frame.current.attackCharges,
                        attackEnabled = frame.current.attackCharges > 0 && !ui.showResult,
                        onAttack = viewModel::onAttack,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = playLayout.actionEnd, bottom = playLayout.actionBottom),
                    )
                }

                if (ui.showResult) {
                    PacMazeOnlineResultOverlay(
                        title = ui.resultTitle,
                        message = ui.resultMessage,
                        eloDelta = ui.eloDelta,
                        isVersus = ui.matchConfig?.mode == PacMazeOnlineMatchMode.VERSUS_DUEL,
                        onPrimary = onBackToLobby,
                        onSecondary = onExit,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeOnlineHudBar(
    ui: com.example.funlife.viewmodel.PacMazeOnlineUiState,
    modifier: Modifier = Modifier,
) {
    val world = ui.world ?: return
    val play = LocalPacMazePlayLayout.current
    val isCoop = ui.matchConfig?.mode == PacMazeOnlineMatchMode.COOP_CAMPAIGN
    val myScore = if (ui.myEntityId == ui.matchConfig?.hostEntityId) world.playerScoreA else world.playerScoreB
    val peerScore = if (ui.myEntityId == ui.matchConfig?.hostEntityId) world.playerScoreB else world.playerScoreA
    val timeText = if (isCoop) {
        "%d:%02d".format(world.onlineElapsedSeconds / 60, world.onlineElapsedSeconds % 60)
    } else {
        val limit = ui.matchConfig?.timeLimitSeconds ?: 150
        val remain = (limit - world.onlineElapsedSeconds).coerceAtLeast(0)
        "%d:%02d".format(remain / 60, remain % 60)
    }
    Row(
        modifier = modifier
            .background(PacMazePalette.hudGradient, RoundedCornerShape(play.hudBarRadius))
            .padding(horizontal = play.dp(8.dp), vertical = play.dp(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("你 $myScore", color = PacMazePalette.accentMint, fontSize = play.statSp)
        Text(
            if (isCoop) "♥${world.teamLives.coerceAtLeast(world.lives)}" else timeText,
            color = PacMazePalette.accentGold,
            fontSize = play.statSp,
            fontWeight = FontWeight.Bold,
        )
        Text("${ui.peerName} $peerScore", color = PacMazePalette.accentOrange, fontSize = play.statSp)
    }
}

@Composable
private fun PacMazeOnlineResultOverlay(
    title: String,
    message: String,
    eloDelta: Int,
    isVersus: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        PacMazeOverlayCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = PacMazePalette.accentGold, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(message, color = PacMazePalette.inkSecondary, fontSize = 15.sp)
                if (isVersus && eloDelta != 0) {
                    Text(
                        "ELO ${if (eloDelta > 0) "+" else ""}$eloDelta",
                        color = if (eloDelta > 0) PacMazePalette.accentMint else PacMazePalette.accentOrange,
                        fontSize = 16.sp,
                    )
                }
                PacMazePrimaryButton(text = "返回大厅", onClick = onPrimary)
                PacMazeSecondaryButton(text = "退出", onClick = onSecondary)
            }
        }
    }
}
