package com.example.funlife.ui.screens.pacmaze.online

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.PacMazePlayMode
import com.example.funlife.ui.screens.pacmaze.currentPacMazeHubLayout
import com.example.funlife.ui.screens.pacmaze.pacMazeClickable
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId

@Composable
fun PacMazeOnlineHubScreen(
    subMode: String,
    versusRating: Int,
    versusGames: Int,
    coopAssists: Int,
    coopLevel: Int,
    onCoopLevelChange: (Int) -> Unit,
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    activeRoomCode: String?,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onResumeRoom: (() -> Unit)?,
    isCreating: Boolean,
    busyLabel: String?,
) {
    val layout = currentPacMazeHubLayout()
    val isCoop = subMode == "coop_campaign"
    val mode = if (isCoop) PacMazePlayMode.ONLINE_COOP else PacMazePlayMode.ONLINE_VERSUS
    val accent = if (isCoop) PacMazePalette.accentMint else PacMazePalette.accentOrange

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(layout.gap),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(layout.panelRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.22f), Color(0xFF141C2E)),
                        ),
                    )
                    .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(layout.panelRadius))
                    .padding(layout.panelPad),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp))) {
                    PacMazeOnlineAccentBar(accent = accent, layout = layout)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
                    ) {
                        Text(mode.emoji, fontSize = (28f * layout.scale).sp)
                        Column {
                            Text(
                                mode.title,
                                color = Color.White,
                                fontSize = layout.titleSp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                            Text(
                                if (isCoop) "共享 5 命 · L1–L8" else "150 秒竞速清豆 · 1v1",
                                color = PacMazePalette.inkSecondary,
                                fontSize = layout.captionSp,
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (isCoop) {
                    Column(verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp))) {
                        Text(
                            "合作关卡",
                            color = PacMazePalette.inkMuted,
                            fontSize = layout.captionSp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(layout.dp(5.dp)),
                        ) {
                            (1..4).forEach { id ->
                                PacMazeCoopLevelChip(
                                    level = id,
                                    selected = coopLevel == id,
                                    accent = accent,
                                    layout = layout,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCoopLevelChange(id) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(layout.dp(5.dp)),
                        ) {
                            (5..8).forEach { id ->
                                PacMazeCoopLevelChip(
                                    level = id,
                                    selected = coopLevel == id,
                                    accent = accent,
                                    layout = layout,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onCoopLevelChange(id) },
                                )
                            }
                        }
                        Text(
                            PacMazeLevelCatalog.find(coopLevel)?.name ?: "第 $coopLevel 关",
                            color = PacMazePalette.accentGold,
                            fontSize = layout.bodySp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "协作助攻 $coopAssists 次",
                            color = PacMazePalette.inkMuted,
                            fontSize = layout.captionSp,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
                    ) {
                        PacMazeOnlineStatTile(
                            label = "ELO",
                            value = "$versusRating",
                            valueColor = PacMazePalette.accentGold,
                            modifier = Modifier.weight(1f),
                            layout = layout,
                        )
                        PacMazeOnlineStatTile(
                            label = "对局",
                            value = "$versusGames",
                            modifier = Modifier.weight(1f),
                            layout = layout,
                        )
                    }
                }

                Text(
                    if (isCoop) "邀请好友一起推关" else "匹配通道 · 实时同步",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(layout.gap),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(layout.panelRadius))
                        .background(PacMazePalette.ctaGradient)
                        .border(1.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(layout.panelRadius))
                        .pacMazeClickable(
                            sound = PacMazeUiSoundId.PrimaryConfirm,
                            enabled = !isCreating,
                            onClick = onCreateRoom,
                        )
                        .padding(layout.panelPad),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                    ) {
                        Text("⚔", fontSize = (32f * layout.scale).sp)
                        Text(
                            if (isCreating) "正在开房…" else "创建房间",
                            color = Color.White,
                            fontSize = (20f * layout.scale).sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (isCoop) "生成 6 位房间号 · 邀请好友" else "生成竞技场房间 · 邀请好友",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = layout.captionSp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (activeRoomCode != null && onResumeRoom != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(layout.dp(12.dp)))
                            .background(PacMazePalette.accentPurple.copy(alpha = 0.18f))
                            .border(1.dp, PacMazePalette.accentPurple.copy(alpha = 0.45f), RoundedCornerShape(layout.dp(12.dp)))
                            .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, onClick = onResumeRoom)
                            .padding(horizontal = layout.dp(12.dp), vertical = layout.dp(10.dp)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "进行中的房间",
                                color = PacMazePalette.inkSecondary,
                                fontSize = layout.captionSp,
                            )
                            Text(
                                activeRoomCode,
                                color = PacMazePalette.accentGold,
                                fontSize = layout.bodySp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(layout.panelRadius))
                        .background(PacMazePalette.contentPanelGradient)
                        .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(layout.panelRadius))
                        .padding(layout.panelPad),
                    verticalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
                ) {
                    Text(
                        "输入房间号加入",
                        color = PacMazePalette.inkPrimary,
                        fontSize = layout.bodySp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = joinCode,
                            onValueChange = onJoinCodeChange,
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = PacMazePalette.accentGold,
                                fontSize = (20f * layout.scale).sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                            ),
                            cursorBrush = SolidColor(PacMazePalette.accentGold),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(layout.dp(10.dp)))
                                .background(Color(0xFF0E1524))
                                .border(
                                    1.5.dp,
                                    PacMazePalette.accentCyan.copy(alpha = 0.35f),
                                    RoundedCornerShape(layout.dp(10.dp)),
                                )
                                .padding(horizontal = layout.dp(14.dp), vertical = layout.dp(12.dp)),
                            decorationBox = { inner ->
                                if (joinCode.isEmpty()) {
                                    Text(
                                        "6 位房间号",
                                        color = PacMazePalette.inkMuted,
                                        fontSize = layout.bodySp,
                                        letterSpacing = 1.sp,
                                    )
                                }
                                inner()
                            },
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(layout.dp(10.dp)))
                                .background(accent.copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(layout.dp(10.dp)))
                                .pacMazeClickable(
                                    sound = PacMazeUiSoundId.SecondaryAction,
                                    enabled = !isCreating,
                                    onClick = onJoinRoom,
                                )
                                .padding(horizontal = layout.dp(16.dp), vertical = layout.dp(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "加入",
                                color = Color.White,
                                fontSize = layout.buttonSp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        if (isCreating) {
            PacMazeOnlineBusyOverlay(
                headline = busyLabel ?: "正在创建房间",
                subtitle = "同步联机通道 · 分配房间号",
            )
        }
    }
}

@Composable
private fun PacMazeCoopLevelChip(
    level: Int,
    selected: Boolean,
    accent: Color,
    layout: com.example.funlife.ui.screens.pacmaze.PacMazeHubLayoutSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(layout.dp(8.dp))
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (selected) accent else PacMazePalette.cardBorder,
                shape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.SecondaryAction, onClick = onClick)
            .padding(vertical = layout.dp(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "L$level",
            color = if (selected) Color.White else PacMazePalette.inkSecondary,
            fontSize = layout.captionSp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun PacMazeOnlineBusyOverlay(
    headline: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val layout = currentPacMazeHubLayout()
    val transition = rememberInfiniteTransition(label = "onlineBusy")
    val chomp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "chomp",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC060A14)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(layout.panelRadius))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1E2A42), Color(0xFF121A2C))),
                )
                .border(1.5.dp, PacMazePalette.accentOrange.copy(alpha = 0.45f), RoundedCornerShape(layout.panelRadius))
                .padding(horizontal = layout.dp(20.dp), vertical = layout.dp(16.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(18.dp)),
        ) {
            PacMazeOnlineArcadeLoader(
                chompProgress = chomp,
                pulse = pulse,
                modifier = Modifier.size(width = layout.dp(140.dp), height = layout.dp(84.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
            ) {
                Text(
                    headline,
                    color = PacMazePalette.inkPrimary,
                    fontSize = layout.titleSp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    color = PacMazePalette.accentGold,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                )
                PacMazeOnlineLoadBar(
                    progress = chomp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layout.dp(6.dp)),
                )
            }
        }
    }
}

@Composable
private fun PacMazeOnlineArcadeLoader(
    chompProgress: Float,
    pulse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    ) {
        val wall = Color(0xFF1E3A5F)
        val path = Color(0xFF0B1220)
        val dot = PacMazePalette.accentGold
        val pac = PacMazePalette.accentOrange

        drawRoundRect(color = path, cornerRadius = CornerRadius(12f, 12f))
        drawRoundRect(color = wall, cornerRadius = CornerRadius(12f, 12f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))

        val cols = 9
        val rows = 5
        val cellW = size.width / cols
        val cellH = size.height / rows
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val blocked = c == 0 || c == cols - 1 || r == 0 || r == rows - 1
                if (blocked) {
                    drawRoundRect(
                        color = wall,
                        topLeft = Offset(c * cellW + 1.5f, r * cellH + 1.5f),
                        size = Size(cellW - 3f, cellH - 3f),
                        cornerRadius = CornerRadius(3f, 3f),
                    )
                }
            }
        }

        val dotCount = (cols - 2) * (rows - 2)
        val eaten = (chompProgress * dotCount).toInt()
        var index = 0
        for (r in 1 until rows - 1) {
            for (c in 1 until cols - 1) {
                if (index >= eaten) {
                    drawCircle(
                        color = dot.copy(alpha = 0.85f),
                        radius = 2.8f * pulse,
                        center = Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f),
                    )
                }
                index++
            }
        }

        val pacCol = 1 + ((chompProgress * (cols - 3)).toInt())
        val pacX = pacCol * cellW + cellW / 2f
        val pacY = size.height / 2f
        drawCircle(color = pac, radius = cellH * 0.26f * pulse, center = Offset(pacX, pacY))
        drawCircle(color = Color.Black, radius = 3f, center = Offset(pacX + 5f, pacY - 4f))
    }
}

@Composable
private fun PacMazeOnlineLoadBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.clip(RoundedCornerShape(3.dp))) {
        drawRoundRect(color = PacMazePalette.cardFill)
        val fillW = size.width * progress.coerceIn(0f, 1f)
        if (fillW > 0f) {
            drawRoundRect(color = PacMazePalette.accentOrange, size = Size(fillW, size.height))
        }
    }
}

@Composable
internal fun PacMazeOnlineAccentBar(
    accent: Color,
    layout: com.example.funlife.ui.screens.pacmaze.PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(layout.dp(36.dp))
            .height(layout.dp(3.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.4f)))),
    )
}

@Composable
internal fun PacMazeOnlineStatTile(
    label: String,
    value: String,
    valueColor: Color = PacMazePalette.inkPrimary,
    layout: com.example.funlife.ui.screens.pacmaze.PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(8.dp)),
    ) {
        Text(label, color = PacMazePalette.inkMuted, fontSize = layout.captionSp)
        Text(
            value,
            color = valueColor,
            fontSize = layout.bodySp,
            fontWeight = FontWeight.Black,
        )
    }
}
