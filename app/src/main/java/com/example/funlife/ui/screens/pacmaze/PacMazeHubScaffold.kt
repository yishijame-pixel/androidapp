package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Hub 骨架：顶栏 + 主内容区。
 * - [hero] 非空：左品牌区 + 右操作区（选模式）
 * - [hero] 为空：主内容全宽（选关地图）
 */
@Composable
fun PacMazeHubScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    hero: (@Composable () -> Unit)? = null,
    topBarTrailing: (@Composable (isWide: Boolean) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PacMazePalette.hubGradient),
    ) {
        PacMazeHubBackdrop()
        if (hero != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f)
                    .background(PacMazePalette.heroGlow),
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val isWide = maxWidth > maxHeight * 1.2f
            val horizontalPad = if (isWide) 20.dp else 14.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPad, vertical = 6.dp),
            ) {
                PacMazeArcadeHubTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    isWide = isWide,
                    showBadge = hero == null,
                    trailing = topBarTrailing?.let { trailing -> { trailing(isWide) } },
                )

                if (hero == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PacMazePalette.contentPanelGradient)
                            .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.32f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(PacMazePalette.heroPanelGradient)
                                .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            hero()
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.68f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(PacMazePalette.contentPanelGradient)
                                .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}
