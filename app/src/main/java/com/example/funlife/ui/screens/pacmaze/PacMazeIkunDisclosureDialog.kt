package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PacMazeIkunDisclosureDialog(
    loading: Boolean,
    onConfirm: () -> Unit,
) {
    val revision = PacMazeIkunDisclosureConfig.revision
    val content = remember(revision) { PacMazeIkunDisclosureConfig.current }
    val scrollState = rememberScrollState()
    val reachedBottom by remember {
        derivedStateOf {
            scrollState.maxValue <= 4 || scrollState.value >= scrollState.maxValue - 4
        }
    }
    val canConfirm = !loading && reachedBottom

    Dialog(
        onDismissRequest = { /* 不可主动关闭 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6020408))
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            val layout = PacMazeHubLayoutSpec.computeScreen(maxWidth = maxWidth, maxHeight = maxHeight)
            val cardShape = RoundedCornerShape(24.dp)

            CompositionLocalProvider(LocalPacMazeHubLayout provides layout) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.88f)
                        .clip(cardShape)
                        .background(PacMazePalette.overlayCardGradient)
                        .border(1.5.dp, PacMazePalette.cardBorderStrong, cardShape)
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = content.title,
                        color = PacMazePalette.accentGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    )

                    if (loading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = PacMazePalette.accentMint)
                        }
                    } else {
                        // weight + verticalScroll 必须套 Box，否则正文撑满 Column 会把底部按钮挤出屏幕
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                content.body.split('\n').forEach { paragraph ->
                                    val line = paragraph.trim()
                                    if (line.isNotEmpty()) {
                                        Text(
                                            text = line,
                                            color = PacMazePalette.inkPrimary,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (reachedBottom) content.footerHint else "↓ 向下滑动阅读全文后可点同意",
                        color = if (reachedBottom) PacMazePalette.inkHint else PacMazePalette.accentOrange,
                        fontSize = 13.sp,
                    )

                    PacMazePrimaryButton(
                        text = content.agreeButtonText,
                        onClick = onConfirm,
                        enabled = canConfirm,
                    )
                }
            }
        }
    }
}
