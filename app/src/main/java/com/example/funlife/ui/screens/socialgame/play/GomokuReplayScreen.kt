package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.social.game.engine.ParsedSgf
import com.example.funlife.social.game.engine.SgfMove
import com.example.funlife.ui.screens.socialgame.HubPrimaryButton
import com.example.funlife.ui.screens.socialgame.HubSecondaryButton
import com.example.funlife.ui.screens.socialgame.SocialGamePalette
import com.example.funlife.ui.screens.socialgame.SocialGameScaffold
import kotlinx.coroutines.delay

/**
 * 五子棋复盘播放器
 */
@Composable
fun GomokuReplayScreen(
    parsedSgf: ParsedSgf,
    onNavigateBack: () -> Unit,
    onShareSgf: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    val totalMoves = parsedSgf.moves.size
    val currentBoard = remember(currentStep) {
        buildBoardAtStep(parsedSgf.moves, currentStep)
    }
    val lastMove = if (currentStep > 0 && currentStep <= totalMoves) {
        val move = parsedSgf.moves[currentStep - 1]
        move.x to move.y
    } else {
        null
    }

    // 自动播放
    LaunchedEffect(isPlaying, currentStep, playbackSpeed) {
        if (isPlaying && currentStep < totalMoves) {
            delay((1000 / playbackSpeed).toLong())
            currentStep++
        } else if (currentStep >= totalMoves) {
            isPlaying = false
        }
    }

    SocialGameScaffold(
        title = "复盘",
        onNavigateBack = onNavigateBack,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // 对局信息
            ReplayHeader(
                blackName = parsedSgf.blackName,
                whiteName = parsedSgf.whiteName,
                result = parsedSgf.result,
            )

            Spacer(Modifier.height(12.dp))

            // 棋盘
            GomokuBoard(
                board = currentBoard,
                lastMove = lastMove,
                enabled = false,
                onCellClick = { _, _ -> },
                modifier = Modifier.fillMaxWidth(),
                animateLastMove = false,
            )

            Spacer(Modifier.height(16.dp))

            // 步数显示
            Text(
                text = "第 $currentStep / $totalMoves 手",
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // 进度条
            Slider(
                value = currentStep.toFloat(),
                onValueChange = {
                    isPlaying = false
                    currentStep = it.toInt()
                },
                valueRange = 0f..totalMoves.toFloat(),
                steps = totalMoves - 1,
                colors = SliderDefaults.colors(
                    thumbColor = SocialGamePalette.accentPurple,
                    activeTrackColor = SocialGamePalette.accentPurple,
                    inactiveTrackColor = SocialGamePalette.glassBorder,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            // 控制按钮
            ReplayControls(
                isPlaying = isPlaying,
                canGoPrev = currentStep > 0,
                canGoNext = currentStep < totalMoves,
                onPlayPause = { isPlaying = !isPlaying },
                onPrev = {
                    isPlaying = false
                    if (currentStep > 0) currentStep--
                },
                onNext = {
                    isPlaying = false
                    if (currentStep < totalMoves) currentStep++
                },
                onFirst = {
                    isPlaying = false
                    currentStep = 0
                },
                onLast = {
                    isPlaying = false
                    currentStep = totalMoves
                },
            )

            // 播放速度
            SpeedSelector(
                speed = playbackSpeed,
                onSpeedChange = { playbackSpeed = it },
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.weight(1f))

            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HubSecondaryButton(
                    text = "返回",
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                )
                HubPrimaryButton(
                    text = "分享棋谱",
                    onClick = { /* TODO: 实现分享 */ },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReplayHeader(
    blackName: String,
    whiteName: String,
    result: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SocialGamePalette.glassFill)
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 黑方
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A2E)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = blackName,
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 结果
        Text(
            text = result ?: "VS",
            color = SocialGamePalette.accentPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )

        // 白方
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = whiteName,
                color = SocialGamePalette.inkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFF444444), CircleShape),
            )
        }
    }
}

@Composable
private fun ReplayControls(
    isPlaying: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFirst: () -> Unit,
    onLast: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 跳到开始
        IconButton(onClick = onFirst, enabled = canGoPrev) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "首步",
                tint = if (canGoPrev) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        // 上一步
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "上一步",
                tint = if (canGoPrev) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
            )
        }

        // 播放/暂停
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SocialGamePalette.accentPurple)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
            )
        }

        // 下一步
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "下一步",
                tint = if (canGoNext) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
            )
        }

        // 跳到末尾
        IconButton(onClick = onLast, enabled = canGoNext) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "末步",
                tint = if (canGoNext) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SpeedSelector(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speeds = listOf(0.5f, 1f, 2f, 4f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "速度：",
            color = SocialGamePalette.inkMuted,
            fontSize = 12.sp,
        )
        speeds.forEach { s ->
            val isSelected = speed == s
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) SocialGamePalette.accentPurple.copy(alpha = 0.15f)
                        else Color.Transparent,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SocialGamePalette.accentPurple else SocialGamePalette.glassBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSpeedChange(s) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "${s}x",
                    color = if (isSelected) SocialGamePalette.accentPurple else SocialGamePalette.inkSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

/**
 * 根据步数构建棋盘状态
 */
private fun buildBoardAtStep(moves: List<SgfMove>, step: Int): String {
    var board = GomokuRules.emptyBoard()
    for (i in 0 until step.coerceAtMost(moves.size)) {
        val move = moves[i]
        if (GomokuRules.validateMove(board, move.x, move.y, move.color)) {
            board = GomokuRules.applyMove(board, move.x, move.y, move.color)
        }
    }
    return board
}
