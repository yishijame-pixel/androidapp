package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.funlife.BuildConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazePortals
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.renderInterpolationSnapshot
import com.example.funlife.ui.screens.pacmaze.PacMazeGhostReleaseBanner
import com.example.funlife.ui.screens.pacmaze.PacMazeGhostProximityOverlay
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeAttackButton
import com.example.funlife.ui.screens.pacmaze.components.PacMazeCanvas
import com.example.funlife.ui.screens.pacmaze.debug.PacMazeMotionDiag
import com.example.funlife.ui.screens.pacmaze.components.PacMazeEndlessWaveBanner
import com.example.funlife.ui.screens.pacmaze.components.PacMazeJoystickState
import com.example.funlife.ui.screens.pacmaze.components.PacMazeJoystickVisual
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapScalePolicy
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapLayoutInsets
import com.example.funlife.ui.screens.pacmaze.components.PacMazeModeHintBanner
import com.example.funlife.ui.screens.pacmaze.components.PacMazeActiveItemBuffRow
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMechanismHud
import com.example.funlife.ui.screens.pacmaze.components.PacMazeTutorialBanner
import com.example.funlife.social.game.engine.pacmaze.TileType
import com.example.funlife.ui.screens.pacmaze.components.PacMazePauseButton
import com.example.funlife.ui.screens.pacmaze.components.PacMazePlayHudBar
import com.example.funlife.ui.screens.pacmaze.components.PacMazePlayLoadingOverlay
import com.example.funlife.ui.screens.pacmaze.components.pacMazeJoystickInput
import com.example.funlife.ui.screens.pacmaze.components.rememberPacMazeJoystickState
import com.example.funlife.ui.screens.pacmaze.components.resetVisual
import com.example.funlife.ui.screens.pacmaze.cosmetic.skin.PacMazeRemoteSkinAnimCache
import com.example.funlife.viewmodel.PacMazeLocalViewModel
import com.example.funlife.viewmodel.PacMazeRenderFrame
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val runMode by viewModel.uiState.map { it.runMode }.collectAsStateWithLifecycle(PacMazeRunMode.CAMPAIGN)
    val endlessWave by viewModel.uiState.map { it.endlessWave }.collectAsStateWithLifecycle(0)
    val maxLevelReached by viewModel.uiState.map { it.maxLevelReached }.collectAsStateWithLifecycle(1)
    val isLoading by viewModel.uiState.map { it.isLoading }.collectAsStateWithLifecycle(false)
    val levelConfig by viewModel.uiState.map { it.levelConfig }.collectAsStateWithLifecycle(null)
    val mapThemeId by viewModel.uiState.map { it.mapThemeId }.collectAsStateWithLifecycle(
        com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId.CYBERPUNK,
    )
    val avatarLoadout by viewModel.uiState.map { it.avatarLoadout }.collectAsStateWithLifecycle(
        com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout(),
    )
    val playerDrawScale by viewModel.uiState.map { it.playerDrawScale }.collectAsStateWithLifecycle(1f)
    val movementMode by viewModel.uiState.map { it.movementMode }.collectAsStateWithLifecycle(PacMazeMovementMode.AUTO)
    val mapWidthScale by viewModel.uiState.map { it.mapWidthScale }.collectAsStateWithLifecycle(1f)
    val mapHeightScale by viewModel.uiState.map { it.mapHeightScale }.collectAsStateWithLifecycle(1f)
    val playHudPanelsExpanded by viewModel.uiState.map { it.playHudPanelsExpanded }.collectAsStateWithLifecycle(false)
    val elapsedSeconds by viewModel.uiState.map { it.elapsedSeconds }.collectAsStateWithLifecycle(0)
    val mazeDifficulty by viewModel.uiState.map { it.mazeDifficulty }.collectAsStateWithLifecycle(
        com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty.STANDARD,
    )
    val mazeUseDaily by viewModel.uiState.map { it.mazeUseDailyChallenge }.collectAsStateWithLifecycle(true)
    val mazeRunProfile by viewModel.uiState.map { it.mazeRunProfile }.collectAsStateWithLifecycle(
        com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunProfile(),
    )
    val renderFrame by viewModel.renderFrame.collectAsStateWithLifecycle(null)

    val joystickState = rememberPacMazeJoystickState()

    LockLandscape()

    if (world == null) {
        PacMazePlayLoadingOverlay(
            message = if (runMode == PacMazeRunMode.MAZE) "生成迷雾迷宫…" else "正在加载关卡…",
        )
        return
    }

    val playWorld = world!!
    val hudWorld = renderFrame?.current ?: playWorld

    val mazeBadgeLabel = if (runMode == PacMazeRunMode.MAZE) {
        buildString {
            append(mazeRunProfile.difficulty.displayName)
            if (mazeRunProfile.seedMode == com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode.DAILY) append("·每日")
            if (mazeRunProfile.contract != com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract.NONE) {
                append("·")
                append(mazeRunProfile.contract.displayName.take(2))
            }
            if (mazeRunProfile.variant != com.example.funlife.social.game.engine.pacmaze.PacMazeMazeVariant.STANDARD) {
                append("·")
                append(mazeRunProfile.variant.displayName.take(2))
            }
        }
    } else {
        null
    }
    val mazeTimeLimit = if (runMode == PacMazeRunMode.MAZE) {
        levelConfig?.modeRules?.timeLimitSeconds ?: 0
    } else {
        0
    }

    LaunchedEffect(levelId, runMode, endlessWave) {
        joystickState.resetVisual()
        viewModel.resetJoystickInput()
    }

    LaunchedEffect(avatarLoadout.skinId, levelId) {
        PacMazeRemoteSkinAnimCache.requestGameplayWarmupAsync(avatarLoadout.skinId)
    }

    // 地图铺满物理屏幕；HUD/摇杆单独加安全区，避免整体 letterbox
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408)),
    ) {
        val playLayout = PacMazePlayLayoutSpec.compute(maxWidth = maxWidth, maxHeight = maxHeight)

        CompositionLocalProvider(LocalPacMazePlayLayout provides playLayout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pacMazeJoystickInput(
                        viewModel = viewModel,
                        joystickState = joystickState,
                        zoneWidth = playLayout.joystickZoneWidth,
                        zoneHeight = playLayout.joystickZoneHeight,
                    ),
            ) {
                PacMazePlayRenderLayer(
                    viewModel = viewModel,
                    screenPhase = screenPhase,
                    levelId = levelId,
                    runMode = runMode,
                    joystickState = joystickState,
                    fallbackWorld = playWorld,
                    themeId = mapThemeId,
                    avatarLoadout = avatarLoadout,
                    playerDrawScale = playerDrawScale,
                    mapWidthScale = mapWidthScale,
                    mapHeightScale = mapHeightScale,
                    markers = levelConfig?.markers.orEmpty(),
                    levelConfig = levelConfig,
                    layoutInsets = PacMazeMapLayoutInsets(
                        top = playLayout.mapInsetTop,
                        bottom = playLayout.mapInsetBottom,
                        horizontal = playLayout.mapInsetHorizontal,
                    ),
                )

                if (runMode == PacMazeRunMode.MAZE) {
                    PacMazeGhostProximityOverlay(
                        world = playWorld,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = playLayout.hudTop + playLayout.hudHeight + playLayout.dp(4.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        levelConfig?.let { cfg ->
                            PacMazeMazeHuntPhaseBanner(playWorld, cfg)
                        }
                        PacMazeMazeEchoHintBanner(playWorld)
                        PacMazeMazeSealedRejectBanner(playWorld)
                    }
                }

                PacMazePlayHudBar(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            top = playLayout.hudTop,
                            start = playLayout.hudHorizontal,
                        ),
                    runMode = runMode,
                    levelId = levelId,
                    endlessWave = endlessWave,
                    maxLevelReached = maxLevelReached,
                    score = hudWorld.score,
                    lives = hudWorld.lives,
                    elapsedSeconds = elapsedSeconds,
                    timeLimitSeconds = mazeTimeLimit,
                    mazeBadgeLabel = mazeBadgeLabel,
                    attackCharges = hudWorld.attackCharges,
                    powerTicksLeft = hudWorld.powerTicksLeft,
                    themeId = mapThemeId,
                    onBack = viewModel::backToMenu,
                    playerDrawScale = playerDrawScale,
                    onPlayerDrawScaleChange = viewModel::setPlayerDrawScale,
                    movementMode = movementMode,
                    onMovementModeChange = viewModel::setMovementMode,
                    mapWidthScale = mapWidthScale,
                    mapHeightScale = mapHeightScale,
                    onMapWidthScaleChange = viewModel::setMapWidthScale,
                    onMapHeightScaleChange = viewModel::setMapHeightScale,
                    panelsExpanded = playHudPanelsExpanded,
                    onPanelsExpandedChange = viewModel::setPlayHudPanelsExpanded,
                )

                if (hudWorld.ghostReleaseTicksLeft > 0) {
                    PacMazeGhostReleaseBanner(
                        world = hudWorld,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = playLayout.hudTop + playLayout.hudHeight + playLayout.dp(4.dp)),
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = playLayout.hudTop,
                            end = playLayout.hudHorizontal,
                        ),
                    horizontalAlignment = Alignment.End,
                ) {
                    PacMazePauseButton(
                        onPause = viewModel::pauseGame,
                        compact = playLayout.isCompactHeight,
                    )
                    PacMazeMechanismHud(
                        world = hudWorld,
                        levelConfig = levelConfig,
                        expanded = playHudPanelsExpanded,
                        onExpandedChange = viewModel::setPlayHudPanelsExpanded,
                        modifier = Modifier.padding(top = playLayout.dp(2.dp)),
                    )
                }

                if (playHudPanelsExpanded && runMode == PacMazeRunMode.CAMPAIGN) {
                    val cfg = levelConfig
                    if (cfg != null && (levelId >= 6 || levelId in 14..18)) {
                        val portalTotal = PacMazePortals.portalCount(cfg)
                        if (portalTotal >= 2 || levelId in 14..18) {
                            PacMazeTutorialBanner(
                                levelId = levelId,
                                visitedCheckpointCount = playWorld.visitedCheckpointTags.size,
                                hasDynamicTiles = playWorld.tiles.any { it == TileType.DYNAMIC_WALL.code },
                                portalArmedCount = PacMazePortals.armedPortalCount(playWorld, cfg),
                                portalTotal = portalTotal.coerceAtLeast(2),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = playLayout.hudTop + playLayout.hudHeight + playLayout.dp(4.dp)),
                            )
                        }
                    }
                }

                if (playHudPanelsExpanded && !playLayout.isCompactHeight && runMode != PacMazeRunMode.CAMPAIGN) {
                    PacMazeModeHintBanner(
                        runMode = runMode,
                        endlessWave = endlessWave,
                        maxLevelReached = maxLevelReached,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = playLayout.hudTop + playLayout.hudHeight + playLayout.dp(4.dp)),
                    )
                }

                if (runMode == PacMazeRunMode.MAZE) {
                    var showMazeBrief by remember(levelId, mazeRunProfile) {
                        mutableStateOf(true)
                    }
                    LaunchedEffect(levelId) {
                        kotlinx.coroutines.delay(8000)
                        showMazeBrief = false
                    }
                    if (showMazeBrief) {
                        PacMazeMazeRunBriefOverlay(
                            options = mazeRunProfile.toRunOptions(
                                com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions.dailySeed(),
                            ),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(
                                    top = playLayout.hudTop + playLayout.hudHeight + playLayout.dp(6.dp),
                                    start = playLayout.hudHorizontal,
                                    end = playLayout.hudHorizontal,
                                ),
                            onDismiss = { showMazeBrief = false },
                        )
                    }
                }

                PacMazeActiveItemBuffRow(
                    shieldCharges = playWorld.shieldCharges,
                    magnetTicksLeft = playWorld.magnetTicksLeft,
                    frostTicksLeft = playWorld.frostTicksLeft,
                    speedBoostTicksLeft = playWorld.speedBoostTicksLeft,
                    scoreBoostTicksLeft = playWorld.scoreBoostTicksLeft,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(
                            start = playLayout.joystickStart,
                            bottom = playLayout.joystickBottom + playLayout.joystickSize + playLayout.dp(8.dp),
                        ),
                )

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

                if (runMode == PacMazeRunMode.MAZE) {
                    val mazeLevel = levelConfig
                    if (mazeLevel != null && mazeLevel.modeRules.intelPointsMax > 0) {
                    PacMazeMazeIntelHud(
                        world = hudWorld,
                        level = mazeLevel,
                        onRevealQuadrant = viewModel::spendMazeIntelRevealQuadrant,
                        onKeyHint = viewModel::spendMazeIntelKeyHint,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(
                                end = playLayout.actionEnd,
                                bottom = playLayout.actionBottom + if (playLayout.isCompactHeight) 72.dp else 88.dp,
                            ),
                    )
                    }
                }

                if (runMode == PacMazeRunMode.MAZE && levelConfig?.modeRules?.radarEnabled == true) {
                    val cooldownSec = (hudWorld.radarCooldownTicksLeft / PacMazeConstants.TICKS_PER_SECOND.toFloat())
                        .toInt()
                        .coerceAtLeast(0)
                    val radarActive = hudWorld.radarRevealTicksLeft > 0
                    PacMazeRadarButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(
                                end = playLayout.actionEnd,
                                bottom = playLayout.actionBottom,
                            ),
                        enabled = hudWorld.radarCooldownTicksLeft <= 0 && !radarActive,
                        cooldownSeconds = if (radarActive) 0 else cooldownSec,
                        onPulse = viewModel::requestRadar,
                        compact = playLayout.isCompactHeight,
                    )
                } else {
                    PacMazeAttackButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(
                                end = playLayout.actionEnd,
                                bottom = playLayout.actionBottom,
                            ),
                        attackCharges = playWorld.attackCharges,
                        attackEnabled = playWorld.attackCharges > 0 && playWorld.attackCooldownTicksLeft <= 0,
                        onAttack = viewModel::requestAttack,
                        runMode = runMode,
                        compact = playLayout.isCompactHeight,
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (runMode == PacMazeRunMode.ENDLESS && endlessWave > 0) {
                        PacMazeEndlessWaveBanner(
                            wave = endlessWave,
                            maxLevelReached = maxLevelReached,
                        )
                    } else {
                        PacMazePlayLoadingOverlay(message = "切换地图中…", compact = true)
                    }
                }
            }
        }
    }
}

/** 独立渲染层：仅本 Composable 随显示帧重组，HUD 不受 renderBlend 影响。 */
@Composable
private fun PacMazePlayRenderLayer(
    viewModel: PacMazeLocalViewModel,
    screenPhase: PacMazePhase,
    levelId: Int,
    runMode: PacMazeRunMode,
    joystickState: PacMazeJoystickState,
    fallbackWorld: PacMazeWorldState,
    themeId: PacMazeMapThemeId,
    avatarLoadout: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout,
    playerDrawScale: Float,
    mapWidthScale: Float,
    mapHeightScale: Float,
    markers: List<PacMazeMapMarker>,
    levelConfig: com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig?,
    layoutInsets: PacMazeMapLayoutInsets,
) {
    val renderFrame by viewModel.renderFrame.collectAsStateWithLifecycle(null)
    var frameDeltaSec by remember { mutableFloatStateOf(1f / PacMazeConstants.TICKS_PER_SECOND) }
    var displayClockNs by remember { mutableLongStateOf(0L) }

    // 逻辑 sim（固定 60Hz）与显示（vsync）解耦：120Hz 屏可在两次 tick 间连续插值。
    LaunchedEffect(screenPhase, levelId) {
        if (screenPhase != PacMazePhase.PLAYING) return@LaunchedEffect
        val stepNs = 1_000_000_000L / PacMazeConstants.TICKS_PER_SECOND
        var spanStartNs = 0L
        var lastFrameNs = 0L
        var auditTick = 0

        val simJob = launch {
            var nextTickNs = System.nanoTime() + stepNs
            while (isActive && viewModel.uiState.value.screenPhase == PacMazePhase.PLAYING) {
                val now = System.nanoTime()
                if (now < nextTickNs) {
                    kotlinx.coroutines.delay((nextTickNs - now) / 1_000_000L)
                    continue
                }
                viewModel.syncJoystickSample(
                    offsetX = joystickState.knobOffset.x,
                    offsetY = joystickState.knobOffset.y,
                    maxRadius = joystickState.maxRadiusPx,
                    fingerDown = joystickState.isActive,
                )
                if (viewModel.tickFrameSilent() != null) {
                    spanStartNs = System.nanoTime()
                }
                nextTickNs += stepNs
                val lagNs = now - nextTickNs
                if (lagNs > stepNs * 4) {
                    nextTickNs = now + stepNs
                }
            }
        }

        try {
            while (isActive && viewModel.uiState.value.screenPhase == PacMazePhase.PLAYING) {
                withFrameNanos { frameNs ->
                    displayClockNs = frameNs
                    if (lastFrameNs > 0L) {
                        frameDeltaSec = ((frameNs - lastFrameNs) / 1_000_000_000f)
                            .coerceIn(1f / 240f, 1f / 20f)
                    }
                    lastFrameNs = frameNs

                    viewModel.publishDisplayFrame(
                        spanStartNs = spanStartNs,
                        spanDurationNs = if (spanStartNs > 0L) stepNs else 0L,
                        displayClockNs = frameNs,
                    )
                    val published = viewModel.renderFrame.value
                    published?.let { frame ->
                        val spanElapsed = if (spanStartNs > 0L) {
                            (frameNs - spanStartNs).coerceAtLeast(0L)
                        } else {
                            0L
                        }
                        PacMazeMotionDiag.notePublishFrame(
                            accumNs = spanElapsed % stepNs,
                            stepNs = stepNs,
                            simDebtNs = 0L,
                            ticksThisFrame = 0,
                            hasPrevious = frame.previous != null,
                            blend = frame.blend,
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        auditTick++
                        if (auditTick % 420 == 0) {
                            PacMazeMotionDiag.finishBitmapAudit(avatarLoadout.skinId.name)
                        }
                    }
                }
            }
        } finally {
            simJob.cancel()
            if (BuildConfig.DEBUG) {
                PacMazeMotionDiag.finishBitmapAudit(avatarLoadout.skinId.name)
            }
        }
    }

    PacMazeCanvas(
        modifier = Modifier.fillMaxSize(),
        renderFrame = renderFrame ?: PacMazeRenderFrame(current = fallbackWorld, previous = null, blend = 1f),
        displayClockNs = displayClockNs,
        frameDeltaSec = frameDeltaSec,
        themeId = themeId,
        avatarLoadout = avatarLoadout,
        playerDrawScale = playerDrawScale,
        mapWidthScale = mapWidthScale,
        mapHeightScale = mapHeightScale,
        markers = markers,
        levelConfig = levelConfig,
        layoutInsets = layoutInsets,
        scalePolicy = PacMazeMapScalePolicy.FIT_MAX,
    )
}
