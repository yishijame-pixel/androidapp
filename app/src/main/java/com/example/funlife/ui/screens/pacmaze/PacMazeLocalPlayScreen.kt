package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeActionCluster
import com.example.funlife.ui.screens.pacmaze.components.PacMazeCanvas
import com.example.funlife.ui.screens.pacmaze.components.PacMazeCompactPlayHud
import com.example.funlife.ui.screens.pacmaze.components.PacMazeJoystickVisual
import com.example.funlife.ui.screens.pacmaze.components.pacMazeJoystickInput
import com.example.funlife.ui.screens.pacmaze.components.rememberPacMazeJoystickState
import com.example.funlife.viewmodel.PacMazeLocalViewModel
import com.example.funlife.viewmodel.PacMazeRenderFrame
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

private val JoystickSize = 148.dp
private val JoystickZoneWidth = 180.dp
private val JoystickZoneHeight = 180.dp
private val PlayHudSidebarWidth = 72.dp

@Composable
@Suppress("UNUSED_PARAMETER")
fun PacMazeLocalPlayScreen(
    viewModel: PacMazeLocalViewModel,
    onExit: () -> Unit,
) {
    val screenPhase by viewModel.uiState
        .map { it.screenPhase }
        .distinctUntilChanged()
        .collectAsStateWithLifecycle(PacMazePhase.MENU)

    val world by viewModel.uiState
        .map { it.world }
        .collectAsStateWithLifecycle(null)

    val levelId by viewModel.uiState.map { it.levelId }.collectAsStateWithLifecycle(1)
    val isLoading by viewModel.uiState.map { it.isLoading }.collectAsStateWithLifecycle(false)
    val levelConfig by viewModel.uiState.map { it.levelConfig }.collectAsStateWithLifecycle(null)
    val mapThemeId by viewModel.uiState.map { it.mapThemeId }.collectAsStateWithLifecycle(
        com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId.CYBERPUNK,
    )
    val selectedCharacterId by viewModel.uiState.map { it.selectedCharacterId }.collectAsStateWithLifecycle(
        com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId.CLASSIC_PAC,
    )
    val playerDrawScale by viewModel.uiState.map { it.playerDrawScale }.collectAsStateWithLifecycle(1f)
    val elapsedSeconds by viewModel.uiState.map { it.elapsedSeconds }.collectAsStateWithLifecycle(0)

    val joystickState = rememberPacMazeJoystickState()

    val renderFrame by viewModel.renderFrame.collectAsStateWithLifecycle(null)

    LockLandscape()

    if (world == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = Color(0xFFFF6E40))
                Text("正在加载关卡…", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
            }
        }
        return
    }

    LaunchedEffect(screenPhase, levelId) {
        if (screenPhase != PacMazePhase.PLAYING) return@LaunchedEffect
        val stepNs = 1_000_000_000L / PacMazeConstants.TICKS_PER_SECOND
        var lastTickNs = 0L
        while (isActive && viewModel.uiState.value.screenPhase == PacMazePhase.PLAYING) {
            withFrameNanos { frameNs ->
                if (lastTickNs == 0L) {
                    lastTickNs = frameNs
                    viewModel.updateRenderBlend(1f)
                    return@withFrameNanos
                }
                while (frameNs - lastTickNs >= stepNs) {
                    viewModel.tickFrame()
                    lastTickNs += stepNs
                }
                val rawBlend = (frameNs - lastTickNs).toFloat() / stepNs.toFloat()
                viewModel.updateRenderBlend(rawBlend)
            }
        }
    }

    val currentWorld = world!!

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408))
            .statusBarsPadding(),
    ) {
        PacMazeCompactPlayHud(
            modifier = Modifier
                .width(PlayHudSidebarWidth)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            levelId = levelId,
            score = currentWorld.score,
            lives = currentWorld.lives,
            elapsedSeconds = elapsedSeconds,
            attackCharges = currentWorld.attackCharges,
            powerTicksLeft = currentWorld.powerTicksLeft,
            onBack = viewModel::backToMenu,
            themeId = mapThemeId,
            sidebar = true,
            playerDrawScale = playerDrawScale,
            onPlayerDrawScaleChange = viewModel::setPlayerDrawScale,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pacMazeJoystickInput(
                    viewModel = viewModel,
                    joystickState = joystickState,
                    zoneWidth = JoystickZoneWidth,
                    zoneHeight = JoystickZoneHeight,
                ),
        ) {
            PacMazeCanvas(
                modifier = Modifier.fillMaxSize(),
                renderFrame = renderFrame ?: PacMazeRenderFrame(current = currentWorld, previous = null, blend = 1f),
                themeId = mapThemeId,
                playerCharacterId = selectedCharacterId,
                playerDrawScale = playerDrawScale,
                markers = levelConfig?.markers.orEmpty(),
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF6E40),
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            "切换地图中…",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            PacMazeJoystickVisual(
                state = joystickState,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 8.dp)
                    .size(JoystickSize),
            )

            PacMazeActionCluster(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
                attackCharges = currentWorld.attackCharges,
                attackEnabled = currentWorld.attackCharges > 0 && currentWorld.attackCooldownTicksLeft <= 0,
                onAttack = viewModel::requestAttack,
                onPause = viewModel::pauseGame,
            )
        }
    }
}
