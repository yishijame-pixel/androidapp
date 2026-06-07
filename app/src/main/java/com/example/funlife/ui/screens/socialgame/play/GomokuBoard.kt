package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import com.example.funlife.social.game.engine.ForbiddenPoint
import com.example.funlife.social.game.engine.GomokuForbiddenRules
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.viewmodel.GomokuPendingPlacement
import com.example.funlife.viewmodel.GomokuPlacementSyncState

@Composable
fun GomokuBoard(
    board: String,
    lastMove: Pair<Int, Int>?,
    enabled: Boolean,
    onCellClick: (x: Int, y: Int) -> Unit,
    modifier: Modifier = Modifier,
    showForbidden: Boolean = true,
    forbiddenPoints: List<ForbiddenPoint> = emptyList(),
    animateLastMove: Boolean = true,
    pendingPlacement: GomokuPendingPlacement? = null,
) {
    val size = GomokuRules.SIZE

    // 落子动画
    val pulseTransition = rememberInfiniteTransition(label = "pendingPulse")
    val pendingPulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pendingPulseAlpha",
    )

    val lastMoveScale = remember(lastMove) { Animatable(0f) }
    LaunchedEffect(lastMove) {
        if (lastMove != null && animateLastMove) {
            lastMoveScale.snapTo(0f)
            lastMoveScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        } else {
            lastMoveScale.snapTo(1f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(enabled, board) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val cell = (this.size.width.toFloat() / size).coerceAtLeast(1f)
                    val x = (offset.x / cell).toInt().coerceIn(0, size - 1)
                    val y = (offset.y / cell).toInt().coerceIn(0, size - 1)
                    onCellClick(x, y)
                }
            },
    ) {
        val boardSize = GomokuRules.SIZE
        val cell = this.size.width / boardSize

        // 绘制棋盘背景
        drawRect(color = Color(0xFFE8C896))

        // 绘制网格线
        for (i in 0 until boardSize) {
            val p = cell * (i + 0.5f)
            drawLine(Color(0xFF8B6914), Offset(cell * 0.5f, p), Offset(this.size.width - cell * 0.5f, p), strokeWidth = 1.5f)
            drawLine(Color(0xFF8B6914), Offset(p, cell * 0.5f), Offset(p, this.size.height - cell * 0.5f), strokeWidth = 1.5f)
        }

        // 绘制星位（天元和四角星）
        drawStarPoints(cell)

        // 绘制禁手标记
        if (showForbidden) {
            forbiddenPoints.forEach { fp ->
                drawForbiddenMark(fp.x, fp.y, cell)
            }
        }

        // 绘制棋子
        for (y in 0 until boardSize) {
            for (x in 0 until boardSize) {
                val isLastMove = lastMove?.first == x && lastMove.second == y
                val scaleValue = if (isLastMove) lastMoveScale.value else 1f

                when (GomokuRules.cell(board, x, y)) {
                    GomokuRules.CELL_BLACK -> {
                        drawStone(x, y, cell, isBlack = true, scale = scaleValue)
                    }
                    GomokuRules.CELL_WHITE -> {
                        drawStone(x, y, cell, isBlack = false, scale = scaleValue)
                    }
                }

                // 最后一手高亮
                if (isLastMove && pendingPlacement?.let { it.x == x && it.y == y } != true) {
                    drawLastMoveMark(x, y, cell)
                }
            }
        }

        pendingPlacement?.let { pending ->
            val cellChar = GomokuRules.cell(board, pending.x, pending.y)
            if (cellChar != GomokuRules.CELL_EMPTY) {
                when (pending.state) {
                    GomokuPlacementSyncState.Sending -> {
                        drawPendingRing(pending.x, pending.y, cell, pendingPulse, Color(0xFF7C4DFF))
                    }
                    GomokuPlacementSyncState.Failed -> {
                        drawPendingRing(pending.x, pending.y, cell, 1f, Color(0xFFFF5252))
                        drawFailedCross(pending.x, pending.y, cell)
                    }
                    GomokuPlacementSyncState.Sent -> Unit
                }
            }
        }
    }
}

private fun DrawScope.drawStarPoints(cell: Float) {
    val starColor = Color(0xFF5C4A14)
    val starRadius = cell * 0.08f

    // 五子棋标准星位
    val starPositions = listOf(
        3 to 3, 3 to 11, 11 to 3, 11 to 11, // 四角星
        7 to 7, // 天元
        3 to 7, 7 to 3, 7 to 11, 11 to 7, // 边星
    )

    starPositions.forEach { (x, y) ->
        val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
        drawCircle(starColor, starRadius, center)
    }
}

private fun DrawScope.drawStone(
    x: Int,
    y: Int,
    cell: Float,
    isBlack: Boolean,
    scale: Float = 1f,
) {
    val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
    val radius = cell * 0.38f

    scale(scale, pivot = center) {
        if (isBlack) {
            // 黑子 - 渐变效果
            drawCircle(Color(0xFF1A1A2E), radius, center)
            // 高光
            drawCircle(
                Color(0xFF4A4A6A),
                radius * 0.3f,
                center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
            )
            // 边框
            drawCircle(Color(0xFF4A4A6A), radius, center, style = Stroke(width = 1f))
        } else {
            // 白子
            drawCircle(Color.White, radius, center)
            // 边框
            drawCircle(Color(0xFF444444), radius, center, style = Stroke(width = 1.5f))
            // 高光
            drawCircle(
                Color(0xFFF5F5F5),
                radius * 0.25f,
                center.copy(x = center.x - radius * 0.2f, y = center.y - radius * 0.2f),
            )
        }
    }
}

private fun DrawScope.drawLastMoveMark(x: Int, y: Int, cell: Float) {
    val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
    drawCircle(
        Color(0xFFFF5252).copy(alpha = 0.65f),
        cell * 0.12f,
        center,
    )
}

private fun DrawScope.drawPendingRing(
    x: Int,
    y: Int,
    cell: Float,
    alpha: Float,
    color: Color,
) {
    val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
    drawCircle(
        color = color.copy(alpha = alpha * 0.85f),
        radius = cell * 0.44f,
        center = center,
        style = Stroke(width = 2.5f),
    )
}

private fun DrawScope.drawFailedCross(x: Int, y: Int, cell: Float) {
    val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
    val mark = cell * 0.18f
    val path = Path().apply {
        moveTo(center.x - mark, center.y - mark)
        lineTo(center.x + mark, center.y + mark)
        moveTo(center.x + mark, center.y - mark)
        lineTo(center.x - mark, center.y + mark)
    }
    drawPath(path, Color(0xFFFF5252), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawForbiddenMark(x: Int, y: Int, cell: Float) {
    val center = Offset(cell * (x + 0.5f), cell * (y + 0.5f))
    val markSize = cell * 0.25f

    // 绘制 X 形禁手标记
    val path = Path().apply {
        moveTo(center.x - markSize, center.y - markSize)
        lineTo(center.x + markSize, center.y + markSize)
        moveTo(center.x + markSize, center.y - markSize)
        lineTo(center.x - markSize, center.y + markSize)
    }

    drawPath(
        path = path,
        color = Color(0xFFFF4444).copy(alpha = 0.7f),
        style = Stroke(width = 2f),
    )
}

/**
 * 带禁手检测的棋盘（自动计算禁手点）
 */
@Composable
fun GomokuBoardWithForbidden(
    board: String,
    lastMove: Pair<Int, Int>?,
    enabled: Boolean,
    onCellClick: (x: Int, y: Int) -> Unit,
    modifier: Modifier = Modifier,
    showForbidden: Boolean = true,
    pendingPlacement: GomokuPendingPlacement? = null,
) {
    var forbiddenPoints by remember { mutableStateOf(emptyList<ForbiddenPoint>()) }
    LaunchedEffect(board, showForbidden) {
        forbiddenPoints = if (showForbidden) {
            withContext(Dispatchers.Default) {
                GomokuForbiddenRules.findAllForbiddenPoints(board)
            }
        } else {
            emptyList()
        }
    }

    GomokuBoard(
        board = board,
        lastMove = lastMove,
        enabled = enabled,
        onCellClick = onCellClick,
        modifier = modifier,
        showForbidden = showForbidden,
        forbiddenPoints = forbiddenPoints,
        pendingPlacement = pendingPlacement,
    )
}
