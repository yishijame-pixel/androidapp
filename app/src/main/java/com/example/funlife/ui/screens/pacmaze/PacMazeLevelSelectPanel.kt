package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPreview
import com.example.funlife.ui.screens.pacmaze.components.PacMazeMapSelectorRow
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeAccent
import com.example.funlife.ui.screens.pacmaze.pacMazeThemeEmoji

/**
 * 选关：左侧紧凑档案栏 + 右侧 S 形豆粒闯关径（街机风，高空间利用率）。
 */
@Composable
fun PacMazeLevelSelectPanel(
    maxLevelReached: Int,
    starsBitmask: Int,
    continueLevelId: Int,
    selectedCharacterId: PacMazeCharacterId,
    isLoading: Boolean,
    loadError: String?,
    onContinue: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    onPracticeLevel: (Int) -> Unit,
    onChangeCharacter: () -> Unit,
) {
    val totalLevels = PacMazeLevelCatalog.levels.size

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PacMazeLevelSidebar(
                maxLevelReached = maxLevelReached,
                totalLevels = totalLevels,
                continueLevelId = continueLevelId,
                continueLevelName = PacMazeLevelCatalog.find(continueLevelId)?.name ?: "",
                continueLevelSubtitle = PacMazeLevelCatalog.find(continueLevelId)?.subtitle ?: "",
                selectedCharacterId = selectedCharacterId,
                enabled = !isLoading,
                onContinue = onContinue,
                onPractice = { onPracticeLevel(continueLevelId) },
                onChangeCharacter = onChangeCharacter,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF151D30))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(18.dp)),
            ) {
                PacMazeSerpentineLevelMap(
                    maxLevelReached = maxLevelReached,
                    starsBitmask = starsBitmask,
                    continueLevelId = continueLevelId,
                    isLoading = isLoading,
                    onSelectLevel = onSelectLevel,
                )

                loadError?.let { msg ->
                    Text(
                        msg,
                        color = Color(0xFFFFAB91),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC2A1510))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Text(
                    "滑动闯关径 · 或下方快速选图",
                    color = PacMazePalette.inkHint,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x66151D30))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )

                PacMazeMapSelectorRow(
                    selectedLevelId = continueLevelId,
                    maxLevelReached = maxLevelReached,
                    isLoading = isLoading,
                    onSelectLevel = onSelectLevel,
                    unlockAll = com.example.funlife.BuildConfig.DEBUG,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PacMazeLevelSidebar(
    maxLevelReached: Int,
    totalLevels: Int,
    continueLevelId: Int,
    continueLevelName: String,
    continueLevelSubtitle: String,
    selectedCharacterId: PacMazeCharacterId,
    enabled: Boolean,
    onContinue: () -> Unit,
    onPractice: () -> Unit,
    onChangeCharacter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF243047), Color(0xFF1A2236))),
            )
            .border(1.dp, PacMazePalette.cardBorderStrong, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("当前目标", color = PacMazePalette.inkMuted, fontSize = 10.sp)
                Text(
                    continueLevelName,
                    color = PacMazePalette.accentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    continueLevelSubtitle,
                    color = PacMazePalette.inkSecondary,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                )
                val theme = PacMazeThemeRegistry.themeForLevel(continueLevelId)
                val themeColor = pacMazeThemeAccent(theme)
                Text(
                    "${pacMazeThemeEmoji(theme)} ${theme.displayName}",
                    color = themeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(themeColor.copy(alpha = 0.12f))
                        .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            PacMazeCheckpointStrip(
                total = totalLevels,
                reached = maxLevelReached,
                current = continueLevelId,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onChangeCharacter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PacMazeCharacterPreview(
                    characterId = selectedCharacterId,
                    modifier = Modifier.size(40.dp),
                    animateWalk = false,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("当前角色", color = PacMazePalette.inkMuted, fontSize = 9.sp)
                    Text(
                        selectedCharacterId.displayName,
                        color = PacMazePalette.inkPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("更换", color = PacMazePalette.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PacMazePalette.ctaGradient)
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onContinue)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("继续", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "第${continueLevelId}关 · $continueLevelName",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 12.sp,
                )
            }
        }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E3A28))
                    .border(1.dp, PacMazePalette.accentMint.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled, onClick = onPractice)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "练习本关（无限命）",
                    color = PacMazePalette.accentMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PacMazeCheckpointStrip(total: Int, reached: Int, current: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("闯关径", color = PacMazePalette.inkMuted, fontSize = 10.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(total) { index ->
                val levelId = index + 1
                val unlocked = levelId <= reached
                val isCurrent = levelId == current
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> PacMazePalette.accentGold
                                unlocked -> PacMazePalette.accentMint.copy(alpha = 0.85f)
                                else -> PacMazePalette.locked.copy(alpha = 0.55f)
                            },
                        )
                        .then(
                            if (isCurrent) {
                                Modifier.border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun PacMazeSerpentineLevelMap(
    maxLevelReached: Int,
    starsBitmask: Int,
    continueLevelId: Int,
    isLoading: Boolean,
    onSelectLevel: (Int) -> Unit,
) {
    val levels = PacMazeLevelCatalog.levels
    val nodeSize = if (levels.size > 6) 44.dp else 56.dp
    val nodeColumnWidth = if (levels.size > 6) 68.dp else 76.dp
    val mapScroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, bottom = 76.dp),
        ) {
            val count = levels.size
            if (count < 2) return@Canvas
            val stepX = size.width / count
            val points = levels.indices.map { index ->
                val row = index % 2
                Offset(
                    x = stepX * (index + 0.5f),
                    y = size.height * if (row == 0) 0.68f else 0.32f,
                )
            }
            drawPelletTrail(points)
            drawPathGlow(points)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(mapScroll)
                .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            levels.forEachIndexed { index, level ->
                val unlocked = level.id <= maxLevelReached
                val stars = if (unlocked) decodePacMazeStars(starsBitmask, level.id) else 0
                val highlighted = level.id == continueLevelId && unlocked
                val zigzagOffset = if (index % 2 == 0) 14.dp else (-14).dp
                val diffColor = PacMazeLevelCatalog.difficultyColor(level.difficulty)
                val themeColor = pacMazeThemeAccent(PacMazeThemeRegistry.themeForLevel(level.id))

                Column(
                    modifier = Modifier
                        .width(nodeColumnWidth)
                        .offset(y = zigzagOffset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (index) {
                        0 -> PacMazePathTag("起点", PacMazePalette.accentMint)
                        levels.lastIndex -> PacMazePathTag("终章", PacMazePalette.accentOrange)
                        else -> Spacer(Modifier.height(12.dp))
                    }

                    Box(
                        modifier = Modifier.height(nodeSize + 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PacMazeOrbNode(
                            level = level,
                            unlocked = unlocked,
                            highlighted = highlighted,
                            themeColor = themeColor,
                            enabled = unlocked && !isLoading,
                            onClick = { onSelectLevel(level.id) },
                            size = nodeSize,
                        )
                        if (highlighted) {
                            PacMazeHeroBadge(
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            level.name,
                            color = if (unlocked) PacMazePalette.inkPrimary else PacMazePalette.inkHint,
                            fontSize = if (levels.size > 6) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        if (unlocked) {
                            Text(
                                level.difficulty,
                                color = diffColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        PacMazeStarRow(stars = stars, maxStars = 3, starSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PacMazePathTag(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun PacMazeOrbNode(
    level: PacMazeLevelMeta,
    unlocked: Boolean,
    highlighted: Boolean,
    themeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
) {
    val ringColor = when {
        highlighted -> PacMazePalette.accentGold
        unlocked -> themeColor
        else -> PacMazePalette.locked
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (unlocked) {
                        Brush.radialGradient(
                            listOf(ringColor.copy(alpha = 0.35f), Color(0xFF1A2236)),
                        )
                    } else {
                        Brush.radialGradient(listOf(Color(0xFF2A3144), Color(0xFF151B28)))
                    },
                )
                .border(
                    width = if (highlighted) 3.dp else 2.dp,
                    color = ringColor.copy(alpha = if (unlocked) 0.9f else 0.45f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                Text(
                    level.id.toString(),
                    color = Color.White,
                    fontSize = if (size < 50.dp) 14.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                )
            } else {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PacMazePalette.inkHint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathGlow(points: List<Offset>) {
    if (points.size < 2) return
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticBezierTo(mid.x, mid.y, end.x, end.y)
        }
        drawPath(
            path = path,
            color = PacMazePalette.accentGold.copy(alpha = 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 14f, cap = StrokeCap.Round),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPelletTrail(points: List<Offset>) {
    if (points.size < 2) return
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val segments = 14
        for (step in 0..segments) {
            val t = step / segments.toFloat()
            val oneMinus = 1f - t
            val x = oneMinus * oneMinus * start.x + 2f * oneMinus * t * mid.x + t * t * end.x
            val y = oneMinus * oneMinus * start.y + 2f * oneMinus * t * mid.y + t * t * end.y
            val pelletRadius = if (step % 3 == 0) 4.5f else 2.5f
            drawCircle(
                color = if (step % 3 == 0) {
                    PacMazePalette.accentGold.copy(alpha = 0.55f)
                } else {
                    Color.White.copy(alpha = 0.18f)
                },
                radius = pelletRadius,
                center = Offset(x, y),
            )
        }
    }
    points.forEach { center ->
        drawCircle(
            color = PacMazePalette.accentGold.copy(alpha = 0.18f),
            radius = 12f,
            center = center,
        )
    }
}

@Composable
fun PacMazeHeroBadge(
    modifier: Modifier = Modifier,
    characterId: PacMazeCharacterId = PacMazeCharacterId.CLASSIC_PAC,
) {
    PacMazeCharacterPreview(
        characterId = characterId,
        modifier = modifier,
        animateWalk = true,
        selected = true,
    )
}

@Composable
fun PacMazeStarRow(stars: Int, maxStars: Int = 3, starSize: TextUnit = 14.sp) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(maxStars) { index ->
            Text(
                if (index < stars) "★" else "☆",
                color = if (index < stars) PacMazePalette.starFilled else PacMazePalette.starEmpty.copy(alpha = 0.75f),
                fontSize = starSize,
                fontWeight = if (index < stars) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
