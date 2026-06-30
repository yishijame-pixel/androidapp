package com.example.funlife.ui.screens.pacmaze

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.funlife.ui.components.GameResourceBanner
import com.example.funlife.ui.components.GameResourceBannerStyle

/**
 * 豆人迷宫大厅顶部资源更新横幅（委托 [GameResourceBanner]）。
 */
@Composable
fun PacMazeResourceUpdateBanner(
    modifier: Modifier = Modifier,
) {
    GameResourceBanner(
        modifier = modifier,
        style = GameResourceBannerStyle.PacMaze,
    )
}
