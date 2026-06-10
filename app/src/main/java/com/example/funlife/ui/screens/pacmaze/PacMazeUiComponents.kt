package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                .padding(vertical = 11.dp),
        )
        Column {
            Text(
                title,
                color = PacMazePalette.inkPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                color = PacMazePalette.inkSecondary,
                fontSize = 13.sp,
            )
        }
    }
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
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PacMazePalette.ctaGradient)
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PacMazeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = PacMazePalette.inkPrimary,
            fontSize = 15.sp,
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

/** 街机风 Hub 顶栏：渐变底、霓虹底线、可选右侧信息胶囊。 */
@Composable
fun PacMazeArcadeHubTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    isWide: Boolean,
    showBadge: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2A3550), Color(0xFF243047), Color(0xFF2A3550)),
                ),
            )
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(16.dp))
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151B28))
                .border(1.dp, PacMazePalette.accentGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = PacMazePalette.accentGold,
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(PacMazePalette.accentGold, PacMazePalette.accentOrange),
                    ),
                )
                .padding(horizontal = 1.5.dp)
                .padding(vertical = 14.dp),
        )

        if (showBadge) {
            PacMazeHeroBadge(modifier = Modifier.size(if (isWide) 36.dp else 32.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = PacMazePalette.accentGold,
                fontSize = if (isWide) 19.sp else 17.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(PacMazePalette.accentGold.copy(alpha = 0.14f))
                        .border(1.dp, PacMazePalette.accentGold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        subtitle,
                        color = PacMazePalette.inkPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text("◆", color = PacMazePalette.accentGold.copy(alpha = 0.45f), fontSize = 8.sp)
                if (isWide) {
                    Text("PAC-MAZE", color = PacMazePalette.inkHint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        trailing?.invoke()
    }
}

@Composable
fun PacMazeTopBarChip(
    emoji: String,
    value: String,
    label: String,
    valueColor: Color = PacMazePalette.inkPrimary,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151B28).copy(alpha = 0.88f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(emoji, fontSize = 13.sp)
        Column {
            Text(
                value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(label, color = PacMazePalette.inkMuted, fontSize = 8.sp, maxLines = 1)
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
