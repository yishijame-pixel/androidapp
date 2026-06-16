package com.example.funlife.ui.screens.pacmaze

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 豆人迷宫横屏进局加载页：左侧品牌/动画，右侧进度与状态（类似 MOBA 进大厅前加载）。
 */
@Composable
fun PacMazeEnterLoadingScreen(
    ui: PacMazeBootUi,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSkipToHub: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val blockBack = ui.status == PacMazeBootStatus.LOADING
    if (blockBack) {
        BackHandler { /* 加载中拦截返回 */ }
    } else {
        BackHandler(onBack = onBack)
    }

    val transition = rememberInfiniteTransition(label = "pacMazeLoad")
    val chomp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "chomp",
    )
    val glowPulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    val displayPercent by animateIntAsState(
        targetValue = ui.percent.coerceIn(0, 100),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "loadPercent",
    )
    val barProgress by animateFloatAsState(
        targetValue = displayPercent / 100f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "loadBar",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PacMazePalette.hubGradient),
    ) {
        PacMazeHubBackdrop()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🟡", fontSize = 42.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "豆人迷宫",
                    color = PacMazePalette.inkPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "PAC MAZE",
                    color = PacMazePalette.accentOrange.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(22.dp))
                PacMazeMiniMazeLoader(
                    modifier = Modifier
                        .size(width = 220.dp, height = 130.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    chompProgress = chomp,
                    pulse = glowPulse,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.72f)
                    .background(PacMazePalette.cardBorder),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                val headline = when (ui.status) {
                    PacMazeBootStatus.FAILED -> "资源加载失败"
                    PacMazeBootStatus.READY -> "加载完成"
                    PacMazeBootStatus.LOADING -> "正在进入豆人迷宫"
                }
                Text(
                    headline,
                    color = PacMazePalette.inkPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    ui.phase,
                    color = PacMazePalette.accentGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    ui.subtitle ?: ui.errorMessage ?: "请稍候，正在准备音效与界面资源…",
                    color = PacMazePalette.inkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    "$displayPercent%",
                    color = PacMazePalette.inkPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                PacMazeLoadProgressBar(
                    progress = barProgress,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(8.dp),
                )
                if (ui.status == PacMazeBootStatus.FAILED) {
                    Spacer(Modifier.height(28.dp))
                    PacMazePrimaryButton(
                        text = "重新下载",
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(0.92f),
                        compact = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (onSkipToHub != null) {
                        PacMazeSecondaryButton(
                            text = "继续进入（稍后在大厅更新）",
                            onClick = onSkipToHub,
                            modifier = Modifier.fillMaxWidth(0.92f),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    PacMazeSecondaryButton(
                        text = "返回",
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(0.92f),
                    )
                } else if (ui.status == PacMazeBootStatus.LOADING) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "同步角色与音效资源后将自动进入大厅",
                        color = PacMazePalette.inkHint,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeLoadProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
    ) {
        drawRoundRect(color = PacMazePalette.cardFill)
        val fillW = size.width * progress.coerceIn(0f, 1f)
        if (fillW > 0f) {
            drawRoundRect(
                color = PacMazePalette.accentOrange,
                size = Size(fillW, size.height),
            )
            drawRoundRect(
                color = PacMazePalette.accentGold.copy(alpha = 0.55f),
                topLeft = Offset(fillW * 0.55f, 0f),
                size = Size(fillW * 0.45f, size.height),
            )
        }
    }
}

@Composable
private fun PacMazeMiniMazeLoader(
    chompProgress: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val wall = Color(0xFF1E3A5F)
        val path = Color(0xFF0B1220)
        val dot = PacMazePalette.accentGold
        val pac = PacMazePalette.accentOrange

        drawRoundRect(color = path, cornerRadius = CornerRadius(18f, 18f))
        drawRoundRect(
            color = wall,
            cornerRadius = CornerRadius(18f, 18f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(3f),
        )

        val cols = 11
        val rows = 6
        val cellW = size.width / cols
        val cellH = size.height / rows
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val blocked = (c == 0 || c == cols - 1 || r == 0 || r == rows - 1) &&
                    !(c in 4..6 && r == 0)
                if (blocked) {
                    drawRoundRect(
                        color = wall,
                        topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                        size = Size(cellW - 4f, cellH - 4f),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
            }
        }

        val dotCount = cols * rows
        val eaten = (chompProgress * dotCount).toInt()
        var index = 0
        for (r in 1 until rows - 1) {
            for (c in 1 until cols - 1) {
                if (index >= eaten) {
                    drawCircle(
                        color = dot.copy(alpha = 0.85f),
                        radius = 3.2f * pulse,
                        center = Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f),
                    )
                }
                index++
            }
        }

        val pacCol = 1 + ((chompProgress * (cols - 3)).toInt())
        val pacX = pacCol * cellW + cellW / 2f
        val pacY = size.height / 2f
        drawCircle(color = pac, radius = cellH * 0.28f * pulse, center = Offset(pacX, pacY))
        drawCircle(color = Color.Black, radius = 3.5f, center = Offset(pacX + 6f, pacY - 5f))
    }
}
