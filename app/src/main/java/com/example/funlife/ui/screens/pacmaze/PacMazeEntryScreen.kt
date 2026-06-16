package com.example.funlife.ui.screens.pacmaze

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.resource.PacMazeResourceUpdateNotifier
import com.example.funlife.ui.screens.pacmaze.components.LockLandscape
import com.example.funlife.ui.screens.pacmaze.components.PacMazeHideSystemBars
import com.example.funlife.data.model.UserSession
import com.example.funlife.viewmodel.PacMazeLocalViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class PacMazeEntryScene {
    SPLASH,
    LOADING,
    HUB,
}

private const val SPLASH_MIN_MS = 520L
private const val READY_HOLD_MS = 380L

/**
 * 进豆人迷宫的统一入口：启动画面 → 横屏加载页 → 大厅（分阶段淡入，避免生硬跳转）。
 */
@Composable
fun PacMazeEntryScreen(
    viewModel: PacMazeLocalViewModel,
    userSession: UserSession?,
    autoStart: Boolean,
    onlineLobbyRoomId: String? = null,
    onNavigateBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PacMazePalette.bgTop),
    ) {
        LockLandscape()
        PacMazeHideSystemBars()

        val context = LocalContext.current
        var bootUi by remember { mutableStateOf(PacMazeBootUi()) }
        var scene by remember { mutableStateOf(PacMazeEntryScene.SPLASH) }
        var attempt by remember { mutableIntStateOf(0) }

        LaunchedEffect(attempt) {
            val isFirstEntry = attempt == 0
            scene = if (isFirstEntry) PacMazeEntryScene.SPLASH else PacMazeEntryScene.LOADING
            bootUi = PacMazeBootUi(phase = "正在初始化…", percent = 0)

            coroutineScope {
                val splashDone = async {
                    if (isFirstEntry) delay(SPLASH_MIN_MS)
                }
                val bootstrap = async {
                    runCatching {
                        PacMazeBootstrap.run(context) { update ->
                            if (isActive) bootUi = update
                        }
                    }.getOrElse { error ->
                        android.util.Log.e("PacMazeEntry", "bootstrap crashed", error)
                        PacMazeBootResult.Failed("加载异常：${error.message ?: "请重试"}")
                    }
                }
                splashDone.await()
                if (!isActive) return@coroutineScope
                if (scene == PacMazeEntryScene.SPLASH) {
                    scene = PacMazeEntryScene.LOADING
                }
                when (val result = bootstrap.await()) {
                    is PacMazeBootResult.Success -> {
                        if (!isActive) return@coroutineScope
                        val readySubtitle = when {
                            result.resourcesReady && result.audioReady -> "角色与音效已就绪"
                            result.resourcesReady -> "角色资源已就绪"
                            result.audioReady -> "音效已就绪"
                            else -> "部分资源待同步，可在大厅更新"
                        }
                        bootUi = bootUi.copy(
                            status = PacMazeBootStatus.READY,
                            percent = 100,
                            phase = "进入大厅",
                            subtitle = readySubtitle,
                            audioReady = result.audioReady,
                            resourcesReady = result.resourcesReady,
                        )
                        delay(READY_HOLD_MS)
                        if (!isActive) return@coroutineScope
                        scene = PacMazeEntryScene.HUB
                        PacMazeResourceUpdateNotifier.refreshAsync()
                        PacMazeSfx.startMenuBgm(context)
                    }
                    is PacMazeBootResult.Failed -> {
                        bootUi = bootUi.copy(
                            status = PacMazeBootStatus.FAILED,
                            phase = "下载未完成",
                            errorMessage = result.message,
                            subtitle = null,
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = scene,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(380, easing = FastOutSlowInEasing),
                ) togetherWith fadeOut(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                )
            },
            label = "pacMazeEntryScene",
        ) { current ->
            when (current) {
                PacMazeEntryScene.SPLASH -> PacMazeLaunchSplash()
                PacMazeEntryScene.LOADING -> PacMazeEnterLoadingScreen(
                    ui = bootUi,
                    onRetry = { attempt++ },
                    onBack = onNavigateBack,
                    onSkipToHub = if (bootUi.status == PacMazeBootStatus.FAILED) {
                        {
                            scene = PacMazeEntryScene.HUB
                            PacMazeResourceUpdateNotifier.refreshAsync()
                        }
                    } else {
                        null
                    },
                )
                PacMazeEntryScene.HUB -> PacMazeModeSelectScreen(
                    viewModel = viewModel,
                    userSession = userSession,
                    autoStart = autoStart,
                    onlineLobbyRoomId = onlineLobbyRoomId,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}
