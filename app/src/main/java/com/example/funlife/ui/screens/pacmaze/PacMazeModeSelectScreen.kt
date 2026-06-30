package com.example.funlife.ui.screens.pacmaze

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.zIndex
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.example.funlife.ui.screens.pacmaze.PacMazeOverlayCard
import com.example.funlife.ui.screens.pacmaze.PacMazePrimaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeStarRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.social.game.engine.pacmaze.PacMazeObjectiveResults
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.example.funlife.ui.screens.pacmaze.PacMazeSfx
import com.example.funlife.ui.screens.pacmaze.components.PacMazeResultOverlay
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapSelectorRow
import com.example.funlife.data.model.UserSession
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.ui.screens.platformer.PlatformerPrewarmBanner
import com.example.funlife.ui.screens.pacmaze.online.PacMazeOnlineHubPanel
import com.example.funlife.ui.screens.pacmaze.online.PacMazeOnlineInHubLobby
import com.example.funlife.ui.screens.pacmaze.online.PacMazeOnlinePlayScreen
import com.example.funlife.ui.screens.socialgame.rememberGameCenterViewModel
import com.example.funlife.viewmodel.GameCenterViewModel
import com.example.funlife.viewmodel.PacMazeLocalViewModel

@Composable
fun PacMazeModeSelectScreen(
    viewModel: PacMazeLocalViewModel,
    userSession: UserSession?,
    onNavigateBack: () -> Unit,
    autoStart: Boolean = false,
    onlineLobbyRoomId: String? = null,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val gameCenterVm: GameCenterViewModel? = userSession?.let { rememberGameCenterViewModel(it) }
    val myDisplayName = userSession?.nickname?.ifBlank { userSession.username } ?: "玩家"

    LaunchedEffect(ui.screenPhase, ui.runMode) {
        PacMazeSfx.syncBgm(context, ui.screenPhase, ui.runMode)
    }

    DisposableEffect(Unit) {
        onDispose { PacMazeSfx.stopBgm(context) }
    }

    LaunchedEffect(ui.loadError) {
        val message = ui.loadError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        viewModel.clearLoadError()
    }

    LaunchedEffect(autoStart, onlineLobbyRoomId) {
        when {
            autoStart -> viewModel.continueCampaign()
            !onlineLobbyRoomId.isNullOrBlank() -> viewModel.openOnlineLobbyFromDeepLink(onlineLobbyRoomId)
            else -> viewModel.ensureHubRoot()
        }
    }

    val onlineLobbyRoute = ui.currentMenuRoute as? PacMazeMenuRoute.OnlineLobby
    if (onlineLobbyRoute != null && userSession != null && gameCenterVm != null) {
        val rooms by gameCenterVm.rooms.collectAsStateWithLifecycle()
        val room = rooms.firstOrNull { it.roomId == onlineLobbyRoute.roomId }
        if (room?.status == GameRoomStatus.PLAYING) {
            PacMazeOnlinePlayScreen(
                roomId = onlineLobbyRoute.roomId,
                userId = userSession.userId,
                onExit = viewModel::backToModeSelect,
                onBackToLobby = viewModel::hubBack,
            )
            return
        }
    }

    when (ui.screenPhase) {
        PacMazePhase.PLAYING,
        PacMazePhase.PAUSED,
        -> PacMazeLocalPlayScreen(
            viewModel = viewModel,
            onExit = onNavigateBack,
        )

        PacMazePhase.LEVEL_CLEAR -> {
            val score = ui.world?.score ?: 0
            val stars = when (ui.runMode) {
                PacMazeRunMode.PRACTICE -> 0
                PacMazeRunMode.MAZE -> viewModel.evaluateStars(score, ui.elapsedSeconds, ui.deathsThisRun)
                PacMazeRunMode.CAMPAIGN -> viewModel.evaluateStars(score, ui.elapsedSeconds, ui.deathsThisRun)
                else -> 0
            }
            val clearTitle: String
            val clearMessage: String
            val clearPrimary: String
            val onClearPrimary: () -> Unit
            when (ui.runMode) {
                PacMazeRunMode.PRACTICE -> {
                    clearTitle = "练习完成"
                    clearMessage = "得分 $score · 不记录进度"
                    clearPrimary = "再练一次"
                    onClearPrimary = { viewModel.startPracticeLevel(ui.levelId) }
                }
                PacMazeRunMode.MAZE -> {
                    clearTitle = "走出迷宫！"
                    clearMessage = buildString {
                        append("用时 ${ui.elapsedSeconds}s · 得分 $score")
                        if (ui.deathsThisRun > 0) append(" · 死亡 ${ui.deathsThisRun}")
                        if (stars > 0) append(" · ★$stars")
                    }
                    clearPrimary = "再来一局"
                    onClearPrimary = viewModel::startMaze
                }
                PacMazeRunMode.ENDLESS -> {
                    clearTitle = "波次完成"
                    val waveInfo = PacMazeEndlessWaveUi.resolve(ui.endlessWave, ui.maxLevelReached)
                    clearMessage = PacMazeEndlessWaveUi.resultMessage(waveInfo, score)
                    clearPrimary = "继续"
                    onClearPrimary = viewModel::backToMenu
                }
                else -> {
                    clearTitle = "过关！"
                    clearMessage = buildString {
                        append("得分 $score · 用时 ${ui.elapsedSeconds}s")
                        if (ui.deathsThisRun > 0) append(" · 死亡 ${ui.deathsThisRun}")
                    }
                    clearPrimary = if (ui.levelId < PacMazeLevelCatalog.TOTAL_LEVELS) "下一关" else "返回选关"
                    onClearPrimary = {
                        if (ui.levelId < PacMazeLevelCatalog.TOTAL_LEVELS) viewModel.nextLevel() else viewModel.backToMenu()
                    }
                }
            }
            val objectives = run {
                val config = ui.levelConfig
                val world = ui.world
                if (config != null && world != null) PacMazeObjectiveResults.build(config, world) else emptyList()
            }
            if (ui.runMode != PacMazeRunMode.ENDLESS) {
                PacMazeResultOverlay(
                    title = clearTitle,
                    message = clearMessage,
                    stars = stars,
                    primary = clearPrimary,
                    secondary = "返回",
                    onPrimary = onClearPrimary,
                    onSecondary = viewModel::backToMenu,
                    objectives = objectives,
                )
            }
        }

        PacMazePhase.GAME_OVER -> {
            val score = ui.world?.score ?: 0
            val (title, message) = when (ui.runMode) {
                PacMazeRunMode.ENDLESS -> {
                    val waveInfo = PacMazeEndlessWaveUi.resolve(ui.endlessWave, ui.maxLevelReached)
                    val msg = PacMazeEndlessWaveUi.resultMessage(waveInfo, score)
                    "无尽终结" to "$msg · 最佳 ${ui.endlessBestScore}"
                }
                PacMazeRunMode.MAZE -> "迷宫失败" to "得分 $score · 最佳用时 ${formatMazeTime(ui.mazeBestTimeMs)}"
                else -> "游戏结束" to "得分 $score · 最高 ${ui.highScore}"
            }
            PacMazeResultOverlay(
                title = title,
                message = message,
                stars = 0,
                primary = "重试",
                secondary = "返回",
                onPrimary = when (ui.runMode) {
                    PacMazeRunMode.MAZE -> viewModel::startMaze
                    PacMazeRunMode.ENDLESS -> viewModel::startEndless
                    else -> viewModel::retryLevel
                },
                onSecondary = viewModel::backToMenu,
            )
        }

        PacMazePhase.MENU -> {
            val route = ui.currentMenuRoute
            val hubBack: () -> Unit = when (route) {
                PacMazeMenuRoute.ModeSelect -> onNavigateBack
                PacMazeMenuRoute.ChapterOverview -> viewModel::backToModeSelect
                PacMazeMenuRoute.MazeHub, PacMazeMenuRoute.MazeHome -> viewModel::backToModeSelect
                is PacMazeMenuRoute.OnlineHub -> viewModel::backToModeSelect
                is PacMazeMenuRoute.OnlineLobby -> viewModel::hubBack
                else -> viewModel::hubBack
            }
            BackHandler(onBack = hubBack)

            val continueLevelId = ui.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS)
            val totalLevels = PacMazeLevelCatalog.levels.size
            val totalStars = (1..ui.maxLevelReached).sumOf { decodePacMazeStars(ui.starsBitmask, it) }

            val hubTitle = when (route) {
                PacMazeMenuRoute.ModeSelect -> "豆人迷宫"
                PacMazeMenuRoute.MazeHub, PacMazeMenuRoute.MazeHome -> "迷雾迷宫"
                PacMazeMenuRoute.CollectionBook -> "收藏册"
                is PacMazeMenuRoute.OnlineHub ->
                    if (route.subMode == "coop_campaign") "并肩闯关" else "豆人对决"
                is PacMazeMenuRoute.OnlineLobby -> "对战大厅"
                is PacMazeMenuRoute.CharacterDetail,
                PacMazeMenuRoute.CharacterSeries,
                is PacMazeMenuRoute.CharacterGrid,
                PacMazeMenuRoute.TrailWorkshop,
                -> if (ui.selectedMode == PacMazePlayMode.SOLO) "单人闯关" else ui.selectedMode.title
                else -> if (route.isMazeRoute()) "迷雾迷宫" else "单人闯关"
            }
            val hubSubtitle = route.subtitle()

            val useHero = when (route) {
                PacMazeMenuRoute.CharacterSeries,
                is PacMazeMenuRoute.CharacterGrid,
                is PacMazeMenuRoute.CharacterDetail,
                -> true
                else -> route == PacMazeMenuRoute.ModeSelect
            }

            Box(Modifier.fillMaxSize()) {
            PlatformerPrewarmBanner(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .zIndex(2f),
            )
            PacMazeHubScaffold(
                title = hubTitle,
                subtitle = hubSubtitle,
                onBack = hubBack,
                showTopBar = !route.usesCompactMazeChrome(),
                hubBanner = {
                    PacMazeResourceUpdateBanner(modifier = Modifier.fillMaxWidth())
                },
                topBarTrailing = { isWide ->
                    when (route) {
                        PacMazeMenuRoute.ModeSelect -> PacMazeModeSelectHubStats(
                            highScore = ui.highScore,
                            maxLevelReached = ui.maxLevelReached,
                            totalLevels = totalLevels,
                            endlessBestScore = ui.endlessBestScore,
                            endlessBestWave = ui.endlessBestWave,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                        )
                        PacMazeMenuRoute.ChapterOverview,
                        is PacMazeMenuRoute.ChapterLevels,
                        is PacMazeMenuRoute.LevelDetail,
                        PacMazeMenuRoute.SerpentineMap,
                        -> PacMazeLevelSelectTopBarStats(
                            highScore = ui.highScore,
                            totalStars = totalStars,
                            maxLevelReached = ui.maxLevelReached,
                            totalLevels = totalLevels,
                            isWide = isWide,
                        )
                        PacMazeMenuRoute.CharacterSeries,
                        is PacMazeMenuRoute.CharacterGrid,
                        is PacMazeMenuRoute.CharacterDetail,
                        PacMazeMenuRoute.TrailWorkshop,
                        PacMazeMenuRoute.CollectionBook,
                        -> Unit
                        PacMazeMenuRoute.MazeHub,
                        PacMazeMenuRoute.MazeHome,
                        -> PacMazeMazeHubTopStats(
                            mazeStats = ui.mazeStats,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            selectedDifficulty = ui.mazeRunProfile.difficulty,
                            useDailyChallenge = ui.mazeRunProfile.seedMode == com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode.DAILY,
                        )
                        else -> Unit
                    }
                },
                hero = if (useHero) {
                    {
                        when (route) {
                            PacMazeMenuRoute.ModeSelect -> PacMazeModeHero(
                                continueLevelId = continueLevelId,
                                onContinueCampaign = viewModel::continueCampaign,
                            )
                            else -> PacMazeCharacterSelectHero(loadout = ui.avatarLoadout)
                        }
                    }
                } else {
                    null
                },
                content = {
                    when (route) {
                        PacMazeMenuRoute.ModeSelect -> PacMazeModeSelectPanel(
                            maxLevelReached = ui.maxLevelReached,
                            totalLevels = totalLevels,
                            endlessBestScore = ui.endlessBestScore,
                            endlessBestWave = ui.endlessBestWave,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            onSelectMode = viewModel::selectMode,
                        )
                        is PacMazeMenuRoute.OnlineHub -> {
                            if (userSession != null && gameCenterVm != null) {
                                PacMazeOnlineHubPanel(
                                    subMode = route.subMode,
                                    userId = userSession.userId,
                                    gameCenterVm = gameCenterVm,
                                    onEnterLobby = viewModel::openOnlineLobby,
                                )
                            } else {
                                Text(
                                    "请先登录并绑定社交账号后再联机",
                                    color = PacMazePalette.inkSecondary,
                                )
                            }
                        }
                        is PacMazeMenuRoute.OnlineLobby -> {
                            if (userSession != null && gameCenterVm != null) {
                                PacMazeOnlineInHubLobby(
                                    roomId = route.roomId,
                                    userSession = userSession,
                                    gameCenterVm = gameCenterVm,
                                    myDisplayName = myDisplayName,
                                    onLobbyClosed = viewModel::hubBack,
                                )
                            }
                        }
                        PacMazeMenuRoute.ChapterOverview,
                        PacMazeMenuRoute.SerpentineMap,
                        -> PacMazeChapterOverviewPanel(
                            maxLevelReached = ui.maxLevelReached,
                            starsBitmask = ui.starsBitmask,
                            continueLevelId = continueLevelId,
                            selectedSkinId = ui.avatarLoadout.skinId,
                            enabled = !ui.isLoading,
                            onContinue = { viewModel.startLevel(continueLevelId) },
                            onPractice = { viewModel.startPracticeLevel(continueLevelId) },
                            onOpenChapter = { chapter -> viewModel.openChapter(chapter.themeId) },
                            onSelectLevel = viewModel::openLevelDetail,
                            onChangeCharacter = viewModel::openCharacterSelect,
                            onOpenCollection = viewModel::openCollectionBook,
                        )
                        is PacMazeMenuRoute.ChapterLevels -> PacMazeChapterLevelListPanel(
                            themeId = route.themeId,
                            maxLevelReached = ui.maxLevelReached,
                            starsBitmask = ui.starsBitmask,
                            onSelectLevel = viewModel::openLevelDetail,
                        )
                        is PacMazeMenuRoute.LevelDetail -> PacMazeLevelDetailPanel(
                            levelId = route.levelId,
                            maxLevelReached = ui.maxLevelReached,
                            starsBitmask = ui.starsBitmask,
                            onStart = { viewModel.startLevel(route.levelId) },
                            onPractice = { viewModel.startPracticeLevel(route.levelId) },
                        )
                        PacMazeMenuRoute.MazeHub,
                        PacMazeMenuRoute.MazeHome,
                        -> PacMazeMazeHomePanel(
                            profile = ui.mazeRunProfile,
                            mazeStats = ui.mazeStats,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            selectedSkinId = ui.avatarLoadout.skinId,
                            enabled = !ui.isLoading,
                            onOpenDaily = { viewModel.openMazePlayGate(com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode.DAILY) },
                            onOpenRandom = { viewModel.openMazePlayGate(com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode.RANDOM) },
                            onOpenArcade = viewModel::openMazeArcadeHall,
                            onOpenTracks = viewModel::openMazeTrackPicker,
                            onOpenContracts = viewModel::openMazeContractLab,
                            onOpenCompetitive = viewModel::openMazeCompetitiveHub,
                            onOpenCodex = viewModel::openMazeCodex,
                            onLaunchConfirm = viewModel::openMazeLaunchConfirm,
                            onChangeCharacter = viewModel::openCharacterSelect,
                        )
                        is PacMazeMenuRoute.MazePlayGate -> PacMazeMazePlayGatePanel(
                            profile = ui.mazeRunProfile,
                            mazeStats = ui.mazeStats,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            randomPreviewSeed = ui.mazeRandomPreviewSeed,
                            onBack = hubBack,
                            onSelectDifficulty = viewModel::selectMazeDifficulty,
                            onOpenContracts = viewModel::openMazeContractLab,
                            onNext = viewModel::openMazeLaunchConfirm,
                            onRefreshRandomPreview = viewModel::refreshMazeRandomPreviewSeed,
                        )
                        PacMazeMenuRoute.MazeLaunchConfirm -> PacMazeMazeLaunchConfirmPanel(
                            profile = ui.mazeRunProfile,
                            previewSeed = if (ui.mazeRunProfile.seedMode == com.example.funlife.social.game.engine.pacmaze.PacMazeMazeSeedMode.DAILY) {
                                com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions.dailySeed()
                            } else {
                                ui.mazeRandomPreviewSeed
                            },
                            enabled = !ui.isLoading,
                            onBack = hubBack,
                            onStart = viewModel::startMaze,
                        )
                        PacMazeMenuRoute.MazeTrackPicker -> PacMazeMazeTrackPickerPanel(
                            profile = ui.mazeRunProfile,
                            mazeStats = ui.mazeStats,
                            onBack = hubBack,
                            onSelect = viewModel::selectMazeDifficulty,
                            onOpenDetail = viewModel::openMazeTrackDetail,
                        )
                        is PacMazeMenuRoute.MazeTrackDetail -> PacMazeMazeTrackDetailPanel(
                            track = route.track,
                            onBack = hubBack,
                            onApply = {
                                viewModel.selectMazeDifficulty(route.track)
                                viewModel.hubBack()
                            },
                        )
                        PacMazeMenuRoute.MazeContractLab -> PacMazeMazeContractLabPanel(
                            profile = ui.mazeRunProfile,
                            onBack = hubBack,
                            onSelect = viewModel::selectMazeContract,
                            onOpenDetail = viewModel::openMazeContractDetail,
                        )
                        is PacMazeMenuRoute.MazeContractDetail -> PacMazeMazeContractDetailPanel(
                            contract = route.contract,
                            onBack = hubBack,
                            onApply = {
                                viewModel.selectMazeContract(route.contract)
                                viewModel.hubBack()
                            },
                        )
                        PacMazeMenuRoute.MazeArcadeHall -> PacMazeMazeArcadeHallPanel(
                            profile = ui.mazeRunProfile,
                            onBack = hubBack,
                            onSelectVariant = { variant ->
                                viewModel.updateMazeProfile { it.copy(variant = variant) }
                            },
                            onOpenVariant = viewModel::openMazeVariantDetail,
                        )
                        is PacMazeMenuRoute.MazeVariantDetail -> PacMazeMazeVariantDetailPanel(
                            variant = route.variant,
                            profile = ui.mazeRunProfile,
                            onBack = hubBack,
                            onApply = {
                                viewModel.updateMazeProfile { it.copy(variant = route.variant) }
                                viewModel.hubBack()
                            },
                        )
                        PacMazeMenuRoute.MazeCompetitiveHub -> PacMazeMazeCompetitiveHubPanel(
                            onBack = hubBack,
                            onDailyBoard = viewModel::openMazeDailyBoard,
                            onWeeklyBoard = viewModel::openMazeWeeklyBoard,
                            onGhostReplay = viewModel::openMazeGhostReplay,
                        )
                        PacMazeMenuRoute.MazeDailyBoard -> PacMazeMazeDailyBoardPanel(
                            mazeStats = ui.mazeStats,
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            onBack = hubBack,
                        )
                        PacMazeMenuRoute.MazeWeeklyBoard -> PacMazeMazeWeeklyBoardPanel(
                            mutator = ui.mazeRunProfile.resolvedMutator(com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions.dailySeed()),
                            onBack = hubBack,
                        )
                        PacMazeMenuRoute.MazeGhostReplay -> PacMazeMazeGhostReplayPanel(
                            mazeBestTimeMs = ui.mazeBestTimeMs,
                            onBack = hubBack,
                        )
                        PacMazeMenuRoute.MazeCodex -> PacMazeMazeCodexPanel(
                            onBack = hubBack,
                            onOpenEntry = viewModel::openMazeCodexEntry,
                        )
                        is PacMazeMenuRoute.MazeCodexEntry -> PacMazeMazeCodexEntryPanel(
                            entryId = route.entryId,
                            onBack = hubBack,
                        )
                        PacMazeMenuRoute.CharacterSeries -> PacMazeCharacterSeriesPanel(
                            loadout = ui.avatarLoadout,
                            onOpenSeries = viewModel::requestOpenCharacterGrid,
                            onOpenTrailWorkshop = viewModel::openTrailWorkshop,
                            onOpenCollection = viewModel::openCollectionBook,
                        )
                        is PacMazeMenuRoute.CharacterGrid -> PacMazeCharacterGridPanel(
                            series = route.series,
                            loadout = ui.avatarLoadout,
                            onSelectSkin = viewModel::requestSelectSkin,
                            onOpenDetail = viewModel::openCharacterDetail,
                        )
                        is PacMazeMenuRoute.CharacterDetail -> PacMazeCharacterDetailPanel(
                            skinId = route.skinId,
                            loadout = ui.avatarLoadout,
                            onSelectSkin = { viewModel.requestSelectSkin(route.skinId) },
                            onOpenTrailWorkshop = viewModel::openTrailWorkshop,
                            onApplyRecommendedTrail = viewModel::applyRecommendedTrail,
                            onConfirm = viewModel::confirmCharacterAndGoToLevels,
                        )
                        PacMazeMenuRoute.TrailWorkshop -> PacMazeTrailWorkshopPanel(
                            loadout = ui.avatarLoadout,
                            onSelectTrail = viewModel::selectTrail,
                            onApplyRecommended = viewModel::applyRecommendedTrail,
                            onConfirm = viewModel::confirmTrailAndBack,
                        )
                        PacMazeMenuRoute.CollectionBook -> PacMazeCollectionBookPanel(
                            userId = viewModel.currentUserId,
                            loadout = ui.avatarLoadout,
                            onSelectSkin = viewModel::requestSelectSkin,
                            onSelectTrail = viewModel::selectTrail,
                        )
                    }
                },
            )

            if (ui.ikunDisclosureVisible) {
                BackHandler { /* 须知未同意前不可返回关闭 */ }
                Box(Modifier.fillMaxSize().zIndex(20f)) {
                    PacMazeIkunDisclosureDialog(
                        loading = ui.ikunDisclosureLoading,
                        onConfirm = viewModel::confirmIkunDisclosure,
                    )
                }
            }
            }
        }

        else -> {
            BackHandler(onBack = onNavigateBack)
            val continueLevelId = ui.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS)
            Box(Modifier.fillMaxSize()) {
                PlatformerPrewarmBanner(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .zIndex(2f),
                )
            PacMazeHubScaffold(
                title = "豆人迷宫",
                subtitle = "模式选择",
                onBack = onNavigateBack,
                hubBanner = {
                    PacMazeResourceUpdateBanner(modifier = Modifier.fillMaxWidth())
                },
                topBarTrailing = { _ ->
                    PacMazeModeSelectHubStats(
                        highScore = ui.highScore,
                        maxLevelReached = ui.maxLevelReached,
                        totalLevels = PacMazeLevelCatalog.levels.size,
                        endlessBestScore = ui.endlessBestScore,
                        endlessBestWave = ui.endlessBestWave,
                        mazeBestTimeMs = ui.mazeBestTimeMs,
                    )
                },
                hero = {
                    PacMazeModeHero(
                        continueLevelId = continueLevelId,
                        onContinueCampaign = viewModel::continueCampaign,
                    )
                },
                content = {
                    PacMazeModeSelectPanel(
                        maxLevelReached = ui.maxLevelReached,
                        totalLevels = PacMazeLevelCatalog.levels.size,
                        endlessBestScore = ui.endlessBestScore,
                        endlessBestWave = ui.endlessBestWave,
                        mazeBestTimeMs = ui.mazeBestTimeMs,
                        onSelectMode = viewModel::selectMode,
                    )
                },
            )
            }
        }
    }

    if (ui.isLoading && ui.screenPhase == PacMazePhase.MENU) {
        PacMazeLoadingOverlay(
            message = if (ui.selectedMode == PacMazePlayMode.MAZE) "生成迷雾迷宫…" else "正在加载关卡…",
        )
    }

    if (ui.screenPhase == PacMazePhase.PAUSED) {
        PacMazePauseOverlay(
            runMode = ui.runMode,
            levelId = ui.levelId,
            maxLevelReached = ui.maxLevelReached,
            mazeDifficulty = ui.mazeDifficulty,
            mazeContract = ui.mazeContract,
            mazeUseDaily = ui.mazeUseDailyChallenge,
            isLoading = ui.isLoading,
            onResume = viewModel::resumeGame,
            onSelectLevel = viewModel::startLevel,
            onBackToLevelSelect = viewModel::backToMenu,
            onExit = onNavigateBack,
        )
    }
}

@Composable
private fun PacMazeLoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000)),
        contentAlignment = Alignment.Center,
    ) {
        PacMazeOverlayCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(color = PacMazePalette.accentOrange)
                Text(
                    message,
                    color = PacMazePalette.inkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun PacMazePauseOverlay(
    runMode: PacMazeRunMode,
    levelId: Int,
    maxLevelReached: Int,
    mazeDifficulty: com.example.funlife.social.game.engine.pacmaze.PacMazeMazeDifficulty,
    mazeContract: com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract,
    mazeUseDaily: Boolean,
    isLoading: Boolean,
    onResume: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onBackToLevelSelect: () -> Unit,
    onExit: () -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "已暂停",
                    color = PacMazePalette.accentGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (runMode == PacMazeRunMode.MAZE) {
                    Text(
                        buildString {
                            append(mazeDifficulty.displayName)
                            append(" · ")
                            append(if (mazeUseDaily) "每日挑战" else "自由种子")
                            if (mazeContract != com.example.funlife.social.game.engine.pacmaze.PacMazeMazeContract.NONE) {
                                append(" · ${mazeContract.displayName}")
                            }
                        },
                        color = PacMazePalette.inkSecondary,
                        fontSize = 14.sp,
                    )
                } else {
                    Text("点击继续游戏", color = PacMazePalette.inkSecondary, fontSize = 14.sp)
                }
                if (runMode != PacMazeRunMode.MAZE) {
                    PacMazeMapSelectorRow(
                        selectedLevelId = levelId,
                        maxLevelReached = maxLevelReached,
                        isLoading = isLoading,
                        onSelectLevel = onSelectLevel,
                        unlockAll = PacMazeTestUnlock.enabled,
                        compact = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PacMazePrimaryButton(text = "继续游戏", onClick = onResume)
                PacMazeSecondaryButton(
                    text = if (runMode == PacMazeRunMode.MAZE) "返回迷宫大厅" else "返回选关",
                    onClick = onBackToLevelSelect,
                )
                PacMazeSecondaryButton(text = "退出游戏", onClick = onExit)
            }
        }
    }
}

private fun formatMazeTime(ms: Long): String {
    if (ms <= 0L) return "—"
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return if (min > 0) "${min}分${sec}秒" else "${sec}秒"
}
