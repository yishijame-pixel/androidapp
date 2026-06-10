package com.example.funlife.ui.screens.pacmaze

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.example.funlife.ui.screens.pacmaze.PacMazeOverlayCard
import com.example.funlife.ui.screens.pacmaze.PacMazePrimaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeStarRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapSelectorRow
import com.example.funlife.viewmodel.PacMazeLocalViewModel

@Composable
fun PacMazeModeSelectScreen(
    viewModel: PacMazeLocalViewModel,
    onNavigateBack: () -> Unit,
    autoStart: Boolean = false,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    LockLandscape()

    var didAutoStart by remember { mutableStateOf(false) }
    LaunchedEffect(autoStart, ui.isLoading, ui.screenPhase, ui.menuStep) {
        if (autoStart &&
            !didAutoStart &&
            !ui.isLoading &&
            ui.screenPhase == PacMazePhase.MENU &&
            ui.menuStep == PacMazeMenuStep.MODE_SELECT
        ) {
            didAutoStart = true
            viewModel.selectMode(PacMazePlayMode.SOLO)
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
                    clearMessage = "用时 ${ui.elapsedSeconds}s · 得分 $score"
                    clearPrimary = "再来一局"
                    onClearPrimary = viewModel::startMaze
                }
                PacMazeRunMode.ENDLESS -> {
                    clearTitle = "波次完成"
                    clearMessage = "第 ${ui.endlessWave} 波 · 得分 $score"
                    clearPrimary = "继续"
                    onClearPrimary = viewModel::backToMenu
                }
                else -> {
                    clearTitle = "过关！"
                    clearMessage = "得分 $score"
                    clearPrimary = if (ui.levelId < PacMazeLevelCatalog.TOTAL_LEVELS) "下一关" else "返回选关"
                    onClearPrimary = {
                        if (ui.levelId < PacMazeLevelCatalog.TOTAL_LEVELS) viewModel.nextLevel() else viewModel.backToMenu()
                    }
                }
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
                )
            }
        }

        PacMazePhase.GAME_OVER -> {
            val score = ui.world?.score ?: 0
            val (title, message) = when (ui.runMode) {
                PacMazeRunMode.ENDLESS -> "无尽终结" to "第 ${ui.endlessWave} 波 · 得分 $score · 最佳 ${ui.endlessBestScore}"
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

        PacMazePhase.MENU -> when (ui.menuStep) {
            PacMazeMenuStep.MODE_SELECT -> {
                BackHandler(onBack = onNavigateBack)
                PacMazeHubScaffold(
                    title = "豆人迷宫",
                    subtitle = "模式选择",
                    onBack = onNavigateBack,
                    topBarTrailing = { isWide ->
                        if (isWide) {
                            PacMazeTopBarChip(
                                emoji = "🏆",
                                value = ui.highScore.toString(),
                                label = "最高分",
                                valueColor = PacMazePalette.accentGold,
                            )
                        }
                    },
                    hero = {
                        PacMazeModeHero(
                            highScore = ui.highScore,
                            maxLevelReached = ui.maxLevelReached,
                        )
                    },
                    content = {
                        PacMazeModeSelectPanel(
                            highScore = ui.highScore,
                            maxLevelReached = ui.maxLevelReached,
                            totalLevels = PacMazeLevelCatalog.levels.size,
                            onSelectMode = viewModel::selectMode,
                        )
                    },
                )
            }

            PacMazeMenuStep.CHARACTER_SELECT -> {
                BackHandler(onBack = viewModel::backToModeSelect)
                PacMazeHubScaffold(
                    title = ui.selectedMode.title,
                    subtitle = "角色选择",
                    onBack = viewModel::backToModeSelect,
                    hero = {
                        PacMazeCharacterSelectHero(
                            characterId = ui.selectedCharacterId,
                        )
                    },
                    content = {
                        PacMazeCharacterSelectPanel(
                            selectedCharacterId = ui.selectedCharacterId,
                            onSelectCharacter = viewModel::selectCharacter,
                            onContinue = viewModel::confirmCharacterAndGoToLevels,
                        )
                    },
                )
            }

            PacMazeMenuStep.LEVEL_SELECT -> {
                BackHandler(onBack = viewModel::backToCharacterSelect)
                val totalLevels = PacMazeLevelCatalog.levels.size
                val totalStars = (1..ui.maxLevelReached).sumOf { decodePacMazeStars(ui.starsBitmask, it) }
                PacMazeHubScaffold(
                    title = "单人闯关",
                    subtitle = "关卡选择",
                    onBack = viewModel::backToCharacterSelect,
                    topBarTrailing = { isWide ->
                        PacMazeLevelSelectTopBarStats(
                            highScore = ui.highScore,
                            totalStars = totalStars,
                            maxLevelReached = ui.maxLevelReached,
                            totalLevels = totalLevels,
                            isWide = isWide,
                        )
                    },
                    content = {
                        PacMazeLevelSelectPanel(
                            maxLevelReached = ui.maxLevelReached,
                            starsBitmask = ui.starsBitmask,
                            continueLevelId = ui.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS),
                            selectedCharacterId = ui.selectedCharacterId,
                            isLoading = ui.isLoading,
                            loadError = ui.loadError,
                            onContinue = {
                                viewModel.startLevel(ui.maxLevelReached.coerceIn(1, PacMazeLevelCatalog.TOTAL_LEVELS))
                            },
                            onSelectLevel = viewModel::startLevel,
                            onPracticeLevel = viewModel::startPracticeLevel,
                            onChangeCharacter = viewModel::backToCharacterSelect,
                        )
                    },
                )
            }
        }

        else -> {
            BackHandler(onBack = onNavigateBack)
            PacMazeHubScaffold(
                title = "豆人迷宫",
                subtitle = "模式选择",
                onBack = onNavigateBack,
                hero = {
                    PacMazeModeHero(
                        highScore = ui.highScore,
                        maxLevelReached = ui.maxLevelReached,
                    )
                },
                content = {
                    PacMazeModeSelectPanel(
                        highScore = ui.highScore,
                        maxLevelReached = ui.maxLevelReached,
                        totalLevels = PacMazeLevelCatalog.levels.size,
                        onSelectMode = viewModel::selectMode,
                    )
                },
            )
        }
    }

    if (ui.isLoading && ui.screenPhase == PacMazePhase.MENU && ui.menuStep == PacMazeMenuStep.LEVEL_SELECT) {
        PacMazeLoadingOverlay(
            message = "正在加载关卡…",
        )
    }

    if (ui.screenPhase == PacMazePhase.PAUSED) {
        PacMazePauseOverlay(
            levelId = ui.levelId,
            maxLevelReached = ui.maxLevelReached,
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
    levelId: Int,
    maxLevelReached: Int,
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
                Text("点击继续游戏", color = PacMazePalette.inkSecondary, fontSize = 14.sp)
                PacMazeMapSelectorRow(
                    selectedLevelId = levelId,
                    maxLevelReached = maxLevelReached,
                    isLoading = isLoading,
                    onSelectLevel = onSelectLevel,
                    unlockAll = com.example.funlife.BuildConfig.DEBUG,
                    compact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                PacMazePrimaryButton(text = "继续游戏", onClick = onResume)
                PacMazeSecondaryButton(text = "返回选关", onClick = onBackToLevelSelect)
                PacMazeSecondaryButton(text = "退出游戏", onClick = onExit)
            }
        }
    }
}

@Composable
private fun PacMazeResultOverlay(
    title: String,
    message: String,
    stars: Int,
    primary: String,
    secondary: String,
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    title,
                    color = PacMazePalette.accentGold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(message, color = PacMazePalette.inkSecondary, fontSize = 15.sp)
                if (stars > 0) {
                    PacMazeStarRow(stars = stars, starSize = 22.sp)
                }
                PacMazePrimaryButton(text = primary, onClick = onPrimary)
                PacMazeSecondaryButton(text = secondary, onClick = onSecondary)
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
