package com.example.funlife.ui.screens.socialgame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 按游戏类型选择专属进局加载页（避免五子棋/你画我猜复用同一套 UI）。 */
@Composable
fun SocialGameEnterLoading(
    gameId: String,
    gameTitle: String,
    gameEmoji: String,
    headline: String = "正在进入$gameTitle",
    phaseLabel: String? = null,
    subtitle: String? = null,
    progressPercent: Int = 0,
    modifier: Modifier = Modifier,
    blockBack: Boolean = true,
) {
    when (gameId) {
        "draw_guess" -> DrawGuessEnterLoadingScreen(
            gameTitle = gameTitle,
            headline = headline,
            phaseLabel = phaseLabel,
            subtitle = subtitle,
            progressPercent = progressPercent,
            modifier = modifier,
            blockBack = blockBack,
        )
        else -> GameEnterLoadingScreen(
            gameTitle = gameTitle,
            gameEmoji = gameEmoji,
            headline = headline,
            modifier = modifier,
            blockBack = blockBack,
        )
    }
}
