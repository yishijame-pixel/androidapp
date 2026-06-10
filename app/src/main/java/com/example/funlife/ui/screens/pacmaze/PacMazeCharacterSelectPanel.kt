package com.example.funlife.ui.screens.pacmaze

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStagePreview
import com.example.funlife.ui.screens.pacmaze.character.hasPowerAura

/** 左侧英雄舞台：聚光灯 + 大角色 + 名称牌 */
@Composable
fun PacMazeCharacterSelectHero(
    characterId: PacMazeCharacterId,
    modifier: Modifier = Modifier,
) {
    val accent = pacMazeCharacterAccent(characterId)
    val tag = pacMazeCharacterTag(characterId)
    val index = PacMazeCharacterId.selectable.indexOf(characterId).coerceAtLeast(0) + 1
    val total = PacMazeCharacterId.selectable.size

    val pulse = rememberInfiniteTransition(label = "heroPulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.55f,
                ),
                radius = size.minDimension * 0.55f,
                center = center,
            )
            drawCircle(
                color = accent.copy(alpha = 0.12f),
                radius = size.minDimension * 0.28f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        tag,
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(
                    String.format("%02d / %02d", index, total),
                    color = PacMazePalette.inkHint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.weight(0.08f))

            Box(contentAlignment = Alignment.Center) {
                PacMazeCharacterStagePreview(
                    characterId = characterId,
                    modifier = Modifier
                        .size(width = 132.dp, height = 118.dp),
                    selected = true,
                    animateWalk = true,
                    powerActive = characterId.hasPowerAura(),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A2236).copy(alpha = 0.92f), Color(0xFF121828)),
                        ),
                    )
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    "${characterId.emoji} ${characterId.displayName}",
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    characterId.subtitle,
                    color = PacMazePalette.inkSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PacMazeCharacterSelectPanel(
    selectedCharacterId: PacMazeCharacterId,
    onSelectCharacter: (PacMazeCharacterId) -> Unit,
    onContinue: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 62.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PacMazeSectionHeader(
                title = "挑选闯关角色",
                subtitle = "共 ${PacMazeCharacterId.selectable.size} 位 · 点击卡片切换 · 双击已选进入选关",
                accentColor = pacMazeCharacterAccent(selectedCharacterId),
            )

            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    PacMazeCharacterId.selectable,
                    key = { _, c -> c.storageKey },
                ) { index, character ->
                    PacMazeCharacterPortraitCard(
                        character = character,
                        index = index + 1,
                        selected = character == selectedCharacterId,
                        onClick = {
                            if (character == selectedCharacterId) {
                                onContinue()
                            } else {
                                onSelectCharacter(character)
                            }
                        },
                        modifier = Modifier
                            .width(108.dp)
                            .fillMaxHeight(),
                    )
                }
            }

            PacMazeCharacterSelectHintBar(selectedCharacterId = selectedCharacterId)
        }

        PacMazeCharacterConfirmButton(
            onClick = onContinue,
            accent = pacMazeCharacterAccent(selectedCharacterId),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .zIndex(1f),
        )
    }
}

@Composable
private fun PacMazeCharacterPortraitCard(
    character: PacMazeCharacterId,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = pacMazeCharacterAccent(character)
    val pulse = rememberInfiniteTransition(label = "cardPulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cardGlow",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "cardScale",
    )
    val outerShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(outerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) accent.copy(alpha = 0.18f) else Color(0xFF2A3550),
                        Color(0xFF121828),
                    ),
                ),
            )
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                brush = if (selected) {
                    Brush.verticalGradient(listOf(accent.copy(alpha = glowAlpha), accent.copy(alpha = 0.35f)))
                } else {
                    Brush.verticalGradient(listOf(PacMazePalette.cardBorder, Color(0xFF1E2838)))
                },
                shape = outerShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = accent.copy(alpha = 0.08f * glowAlpha),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width / 2f, size.height * 0.45f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = if (selected) 1f else 0.35f),
                                accent.copy(alpha = if (selected) 0.5f else 0.15f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.25f) else Color(0xFF151B28),
                        )
                        .border(
                            1.dp,
                            if (selected) accent.copy(alpha = 0.6f) else PacMazePalette.cardBorder,
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                ) {
                    Text(
                        String.format("%02d", index),
                        color = if (selected) accent else PacMazePalette.inkHint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(accent, accent.copy(alpha = 0.6f))),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                } else {
                    Text(character.emoji, fontSize = 12.sp)
                }
            }

            PacMazeCharacterStagePreview(
                characterId = character,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                selected = selected,
                animateWalk = selected,
                powerActive = selected && character.hasPowerAura(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF151B28).copy(alpha = 0.6f),
                                if (selected) accent.copy(alpha = 0.22f) else Color(0xFF1A2236),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        if (selected) accent.copy(alpha = 0.35f) else PacMazePalette.cardBorder,
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    character.displayName,
                    color = if (selected) PacMazePalette.inkPrimary else PacMazePalette.inkSecondary,
                    fontSize = if (selected) 11.sp else 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PacMazeCharacterSelectHintBar(
    selectedCharacterId: PacMazeCharacterId,
) {
    val accent = pacMazeCharacterAccent(selectedCharacterId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF151B28).copy(alpha = 0.85f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(selectedCharacterId.emoji, fontSize = 16.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "当前：${selectedCharacterId.displayName}",
                color = PacMazePalette.inkPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                selectedCharacterId.subtitle,
                color = PacMazePalette.inkMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                pacMazeCharacterTag(selectedCharacterId),
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PacMazeCharacterConfirmButton(
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(PacMazePalette.accentOrange, accent.copy(alpha = 0.85f)),
                ),
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "确认角色",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "去选关",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
