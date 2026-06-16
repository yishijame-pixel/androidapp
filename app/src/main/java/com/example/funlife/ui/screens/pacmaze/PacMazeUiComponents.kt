package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Hub 背景装饰：微光网格点，提升层次感。 */
@Composable
fun PacMazeHubBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 28.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = 1.2f,
                    center = Offset(x, y),
                )
                y += step
            }
            x += step
        }
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    PacMazePalette.accentPurple.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.82f, size.height * 0.35f),
                radius = size.minDimension * 0.55f,
            ),
        )
    }
}

@Composable
fun PacMazeSectionHeader(
    title: String,
    subtitle: String,
    accentColor: Color = PacMazePalette.accentGold,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.55f)),
                    ),
                )
                .padding(horizontal = 2.dp)
                .padding(vertical = layout.dp(if (layout.isCompactHeight) 7.dp else 11.dp)),
        )
        Column {
            Text(
                title,
                color = PacMazePalette.inkPrimary,
                fontSize = layout.titleSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!layout.isCompactHeight || subtitle.length <= 28) {
                Text(
                    subtitle,
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.subtitleSp,
                    maxLines = if (layout.isCompactHeight) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Hub 可点击区域：按 [PacMazeUiSoundId] 规范播放 UI 音效。 */
@Composable
fun Modifier.pacMazeClickable(
    sound: PacMazeUiSoundId,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val context = LocalContext.current
    val handler = remember(onClick, sound) { pacMazeUiClick(context, sound, onClick) }
    return clickable(enabled = enabled, onClick = handler)
}

@Composable
fun rememberPacMazeUiClick(
    sound: PacMazeUiSoundId,
    onClick: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    return remember(onClick, sound) { pacMazeUiClick(context, sound, onClick) }
}

@Composable
fun PacMazeOverlayCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(PacMazePalette.overlayCardGradient)
            .border(1.5.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(24.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun PacMazePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    dense: Boolean = false,
    fontSize: TextUnit? = null,
    enabled: Boolean = true,
) {
    val layout = LocalPacMazeHubLayout.current
    val textSize = fontSize ?: when {
        dense -> layout.captionSp
        compact -> layout.buttonSp
        else -> (16f * layout.scale).sp
    }
    val context = LocalContext.current
    val corner = layout.dp(when {
        dense -> 8.dp
        compact -> 12.dp
        else -> 14.dp
    })
    val vPad = layout.dp(when {
        dense -> 0.dp
        compact -> 8.dp
        else -> 14.dp
    })
    Box(
        modifier = modifier
            .then(if (compact || dense) Modifier else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(corner))
            .background(
                if (enabled) PacMazePalette.ctaGradient
                else Brush.linearGradient(listOf(Color(0xFF3A4250), Color(0xFF2A3040))),
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.22f else 0.08f), RoundedCornerShape(corner))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = pacMazeUiClick(context, PacMazeUiSoundId.PrimaryConfirm, onClick))
                } else {
                    Modifier
                },
            )
            .then(if (dense) Modifier else Modifier.padding(vertical = vPad)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.45f),
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PacMazeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(if (compact) 12.dp else 14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                1.dp,
                PacMazePalette.cardBorder,
                RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
            .clickable(onClick = pacMazeUiClick(context, PacMazeUiSoundId.SecondaryAction, onClick))
            .padding(vertical = if (compact) 8.dp else 12.dp, horizontal = if (compact) 12.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = PacMazePalette.inkPrimary,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PacMazeStatPill(
    label: String,
    value: String,
    valueColor: Color = PacMazePalette.inkPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = PacMazePalette.inkMuted, fontSize = 11.sp)
        Text(
            value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 紧凑 Hub 顶栏：单行面包屑 + 可选右侧胶囊，减少垂直占用。 */
@Composable
fun PacMazeArcadeHubTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    layout: PacMazeHubLayoutSpec,
    showBadge: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val breadcrumb = buildString {
        append(title)
        if (subtitle.isNotBlank() && subtitle != title) {
            append(" · ")
            append(subtitle)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Color(0xFF151D30).copy(alpha = 0.94f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .padding(start = 2.dp, end = layout.dp(6.dp), top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        IconButton(
            onClick = rememberPacMazeUiClick(PacMazeUiSoundId.NavigateBack, onBack),
            modifier = Modifier
                .size(layout.dp(34.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2838))
                .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(8.dp)),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = PacMazePalette.accentGold,
                modifier = Modifier.size(layout.dp(18.dp)),
            )
        }

        if (showBadge) {
            PacMazeHeroBadge(modifier = Modifier.size(layout.dp(28.dp)))
        }

        Text(
            breadcrumb,
            color = PacMazePalette.inkPrimary,
            fontSize = layout.topBarTitleSp * 0.88f,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (trailing != null) {
            Box(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterEnd,
            ) {
                trailing.invoke()
            }
        }
    }
}

/** 模式选择顶栏右侧：填满空白，展示各模式战绩。 */
@Composable
fun PacMazeModeSelectHubStats(
    highScore: Int,
    maxLevelReached: Int,
    totalLevels: Int,
    endlessBestScore: Int,
    endlessBestWave: Int,
    mazeBestTimeMs: Long,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PacMazeTopBarChip(
            "🏆",
            highScore.toString(),
            "最高分",
            PacMazePalette.accentGold,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        PacMazeTopBarChip(
            "🎯",
            "$maxLevelReached/$totalLevels",
            "闯关",
            PacMazePalette.accentCyan,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        PacMazeTopBarChip(
            "♾️",
            if (endlessBestScore > 0) endlessBestScore.toString() else "—",
            if (endlessBestWave > 0) "W$endlessBestWave" else "无尽",
            PacMazePalette.modeEndless,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        PacMazeTopBarChip(
            "🧭",
            if (mazeBestTimeMs > 0L) {
                val sec = (mazeBestTimeMs / 1000).toInt()
                if (sec >= 60) "${sec / 60}m" else "${sec}s"
            } else {
                "—"
            },
            "迷宫",
            PacMazePalette.modeMaze,
            valueSp = layout.topBarChipValueSp,
            compact = layout.useCompactTopBarChips,
        )
        if (!layout.useCompactTopBarChips) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PacMazePalette.accentMint.copy(alpha = 0.12f))
                    .border(1.dp, PacMazePalette.accentMint.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = layout.dp(8.dp), vertical = 4.dp),
            ) {
                Text(
                    "左摇杆 · 右攻击",
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 换角色顶栏：当前序号与阵营标签。 */
@Composable
fun PacMazeCharacterSelectHubStats(
    loadout: com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    val skinId = loadout.skinId
    val accent = pacMazeSkinAccent(skinId)
    val index = com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId.selectable
        .indexOf(skinId)
        .coerceAtLeast(0) + 1
    val total = com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId.selectable.size
    Row(
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PacMazeTopBarChip(
            skinId.emoji,
            String.format("%02d/%02d", index, total),
            "皮肤",
            accent,
            valueSp = layout.topBarChipValueSp,
            compact = true,
        )
        PacMazeTopBarChip(
            loadout.trailId.emoji,
            loadout.trailId.displayName,
            "拖尾",
            pacMazeTrailAccent(loadout.trailId),
            valueSp = layout.topBarChipValueSp,
            compact = true,
        )
    }
}

@Composable
fun PacMazeTopBarChip(
    emoji: String,
    value: String,
    label: String,
    valueColor: Color = PacMazePalette.inkPrimary,
    modifier: Modifier = Modifier,
    valueSp: TextUnit = 12.sp,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151B28).copy(alpha = 0.88f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 3.dp else 5.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
    ) {
        Text(emoji, fontSize = if (compact) 11.sp else 13.sp)
        Column {
            Text(
                value,
                color = valueColor,
                fontSize = valueSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (!compact) {
                Text(label, color = PacMazePalette.inkMuted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun PacMazeLevelSelectTopBarStats(
    highScore: Int,
    totalStars: Int,
    maxLevelReached: Int,
    totalLevels: Int,
    isWide: Boolean,
) {
    if (!isWide) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PacMazeTopBarChip("🏆", highScore.toString(), "最高分", PacMazePalette.accentGold)
            PacMazeTopBarChip("★", totalStars.toString(), "星级", PacMazePalette.starFilled)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PacMazeTopBarChip("🏆", highScore.toString(), "最高分", PacMazePalette.accentGold)
        PacMazeTopBarChip("★", totalStars.toString(), "总星级", PacMazePalette.starFilled)
        PacMazeTopBarChip("🎯", "$maxLevelReached/$totalLevels", "解锁", PacMazePalette.accentMint)
    }
}
