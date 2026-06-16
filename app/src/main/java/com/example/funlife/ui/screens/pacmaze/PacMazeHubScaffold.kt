package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Hub 骨架：顶栏 + 主内容区；在 [BoxWithConstraints] 内计算 [PacMazeHubLayoutSpec] 并注入子树。
 */
@Composable
fun PacMazeHubScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    hero: (@Composable () -> Unit)? = null,
    topBarTrailing: (@Composable (isWide: Boolean) -> Unit)? = null,
    showTopBar: Boolean = true,
    hubBanner: (@Composable () -> Unit)? = null,
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
                .navigationBarsPadding()
                .padding(top = 2.dp),
        ) {
            val layout = PacMazeHubLayoutSpec.computeScreen(maxWidth = maxWidth, maxHeight = maxHeight)

            CompositionLocalProvider(LocalPacMazeHubLayout provides layout) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = layout.horizontalPad, vertical = layout.dp(2.dp)),
                ) {
                    if (showTopBar) {
                        PacMazeArcadeHubTopBar(
                            title = title,
                            subtitle = subtitle,
                            onBack = onBack,
                            layout = layout,
                            showBadge = hero == null,
                            trailing = topBarTrailing?.let { trailing -> { trailing(layout.isWide) } },
                        )
                    }
                    if (hubBanner != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (showTopBar) layout.gap else layout.dp(0.dp)),
                        ) {
                            hubBanner()
                        }
                    }

                    val contentTopPad = when {
                        hubBanner != null -> layout.gap
                        showTopBar -> layout.gap
                        else -> layout.dp(0.dp)
                    }
                    if (hero == null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = contentTopPad)
                                .clip(RoundedCornerShape(layout.panelRadius))
                                .background(PacMazePalette.contentPanelGradient)
                                .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
                                .padding(layout.panelPad),
                        ) {
                            PacMazeAdaptiveHubPanel {
                                content()
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = contentTopPad),
                            horizontalArrangement = Arrangement.spacedBy(layout.gap),
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(layout.heroWeight)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(layout.panelRadius))
                                    .background(PacMazePalette.heroPanelGradient)
                                    .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
                                    .padding(layout.panelPad),
                            ) {
                                PacMazeAdaptiveHubPanel {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        hero()
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(layout.contentWeight)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(layout.panelRadius))
                                    .background(PacMazePalette.contentPanelGradient)
                                    .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
                                    .padding(layout.panelPad),
                            ) {
                                PacMazeAdaptiveHubPanel {
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
