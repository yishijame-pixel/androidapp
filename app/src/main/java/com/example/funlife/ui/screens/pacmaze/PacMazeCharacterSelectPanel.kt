package com.example.funlife.ui.screens.pacmaze

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStagePreview
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeAvatarLoadout
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeBitmapWalkCatalog
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterStageDecor
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeCosmeticCatalog
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId

/** 左侧英雄舞台：聚光灯 + 大角色 + 名称牌 */
@Composable
fun PacMazeCharacterSelectHero(
    loadout: PacMazeAvatarLoadout,
    modifier: Modifier = Modifier,
) {
    val skinId = loadout.skinId
    val layout = currentPacMazeHubLayout()
    val accent = pacMazeSkinAccent(skinId)
    val tag = pacMazeSkinTag(skinId)
    val tier = PacMazeCosmeticCatalog.bodyTier(skinId)
    val index = PacMazeSkinId.selectable.indexOf(skinId).coerceAtLeast(0) + 1
    val total = PacMazeSkinId.selectable.size

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.gap),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(8.dp)),
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
                        .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(3.dp)),
                ) {
                    Text(
                        tag,
                        color = accent,
                        fontSize = layout.captionSp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    String.format("%02d / %02d", index, total),
                    color = PacMazePalette.inkHint,
                    fontSize = layout.bodySp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.weight(0.06f))

            Box(contentAlignment = Alignment.Center) {
                val ikun = PacMazeBitmapWalkCatalog.contains(skinId)
                val previewScale = if (ikun) 1.72f else 1f
                PacMazeCharacterStagePreview(
                    skinId = skinId,
                    loadout = loadout,
                    modifier = Modifier.size(
                        layout.characterPreviewW * previewScale,
                        layout.characterPreviewH * previewScale,
                    ),
                    selected = true,
                    animateWalk = true,
                    powerActive = skinId.hasPowerAura(),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(layout.cardRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E2838).copy(alpha = 0.95f),
                                Color(0xFF121828),
                            ),
                        ),
                    )
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(layout.cardRadius))
                    .padding(horizontal = layout.cardPad, vertical = layout.dp(10.dp)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
                ) {
                    Text(skinId.emoji, fontSize = layout.subtitleSp)
                    Text(
                        skinId.displayName,
                        color = accent,
                        fontSize = layout.titleSp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${skinId.subtitle} · 体型${tier.label} · 移速${(tier.speedMul * 100).toInt()}%",
                    color = PacMazePalette.inkSecondary,
                    fontSize = layout.bodySp,
                    lineHeight = (layout.bodySp.value * 1.35f).sp,
                    textAlign = TextAlign.Center,
                    maxLines = if (layout.isCompactHeight) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun PacMazeCharacterSelectPanel(
    loadout: PacMazeAvatarLoadout,
    onSelectSkin: (PacMazeSkinId) -> Unit,
    onSelectTrail: (PacMazeTrailId) -> Unit,
    onApplyRecommendedTrail: () -> Unit,
    onContinue: () -> Unit,
) {
    val layout = currentPacMazeHubLayout()
    val accent = pacMazeSkinAccent(loadout.skinId)
    val subtitle = if (layout.isCompactHeight) {
        "共 ${PacMazeSkinId.selectable.size} 款皮肤 · 点击切换"
    } else {
        "共 ${PacMazeSkinId.selectable.size} 款皮肤 · 可搭配拖尾特效"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        PacMazeSectionHeader(
            title = "挑选闯关角色",
            subtitle = subtitle,
            accentColor = accent,
            layout = layout,
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val cardWidth = layout.characterCardWidth(maxWidth, visibleCards = if (layout.isCompactHeight) 3 else 4)
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(layout.gap),
            ) {
                itemsIndexed(
                    PacMazeSkinId.selectable,
                    key = { _, skin -> skin.storageKey },
                ) { index, skin ->
                    PacMazeCharacterPortraitCard(
                        skin = skin,
                        loadout = loadout,
                        index = index + 1,
                        selected = skin == loadout.skinId,
                        onClick = {
                            if (skin == loadout.skinId) {
                                onContinue()
                            } else {
                                onSelectSkin(skin)
                            }
                        },
                        layout = layout,
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        PacMazeTrailSelectRow(
            selectedTrailId = loadout.trailId,
            onSelectTrail = onSelectTrail,
            onApplyRecommended = onApplyRecommendedTrail,
            recommendedTrailId = PacMazeCosmeticCatalog.recommendedTrail(loadout.skinId),
            layout = layout,
        )

        if (!layout.isCompactHeight) {
            PacMazeCharacterSelectHintBar(
                loadout = loadout,
                layout = layout,
            )
        }

        PacMazeCharacterConfirmButton(
            onClick = onContinue,
            accent = accent,
            layout = layout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PacMazeTrailSelectRow(
    selectedTrailId: PacMazeTrailId,
    onSelectTrail: (PacMazeTrailId) -> Unit,
    onApplyRecommended: () -> Unit,
    recommendedTrailId: PacMazeTrailId,
    layout: PacMazeHubLayoutSpec,
) {
    Column(verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp))) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "拖尾特效",
                color = PacMazePalette.inkPrimary,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "推荐 ${recommendedTrailId.emoji}",
                color = PacMazePalette.inkMuted,
                fontSize = layout.captionSp,
                modifier = Modifier.pacMazeClickable(sound = PacMazeUiSoundId.UtilityRecommend, onClick = onApplyRecommended),
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp))) {
            itemsIndexed(PacMazeTrailId.selectable, key = { _, t -> t.storageKey }) { _, trail ->
                val selected = trail == selectedTrailId
                val trailAccent = pacMazeTrailAccent(trail)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(layout.dp(10.dp)))
                        .background(if (selected) trailAccent.copy(alpha = 0.2f) else Color(0xFF151B28))
                        .border(
                            1.dp,
                            if (selected) trailAccent else PacMazePalette.cardBorder,
                            RoundedCornerShape(layout.dp(10.dp)),
                        )
                        .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect) { onSelectTrail(trail) }
                        .padding(horizontal = layout.dp(10.dp), vertical = layout.dp(6.dp)),
                ) {
                    Text(
                        "${trail.emoji} ${trail.displayName}",
                        color = if (selected) trailAccent else PacMazePalette.inkSecondary,
                        fontSize = layout.captionSp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeCharacterPortraitCard(
    skin: PacMazeSkinId,
    loadout: PacMazeAvatarLoadout,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    layout: PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
) {
    val accent = pacMazeSkinAccent(skin)
    val tier = PacMazeCosmeticCatalog.bodyTier(skin)
    val family = PacMazeCharacterStageDecor.familyLabel(skin)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "cardScale",
    )
    val outerShape = RoundedCornerShape(layout.cardRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .clip(outerShape)
            .background(PacMazeCharacterStageDecor.neutralCardFill(selected))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.7f) else PacMazePalette.cardBorder,
                shape = outerShape,
            )
            .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(layout.dp(6.dp)),
            verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layout.dp(3.dp))
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
                        .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(2.dp)),
                ) {
                    Text(
                        String.format("%02d", index),
                        color = if (selected) accent else PacMazePalette.inkHint,
                        fontSize = layout.captionSp,
                        fontWeight = FontWeight.Black,
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(layout.dp(16.dp))
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
                            modifier = Modifier.size(layout.dp(10.dp)),
                        )
                    }
                } else {
                    Text(skin.emoji, fontSize = layout.bodySp)
                }
            }

            PacMazeCharacterStagePreview(
                skinId = skin,
                loadout = if (selected) loadout else PacMazeAvatarLoadout(skinId = skin),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                selected = selected,
                animateWalk = selected,
                powerActive = selected && skin.hasPowerAura(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(layout.dp(8.dp)))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0E121C).copy(alpha = 0.75f),
                                if (selected) accent.copy(alpha = 0.18f) else Color(0xFF151B28),
                            ),
                        ),
                    )
                    .border(
                        1.dp,
                        if (selected) accent.copy(alpha = 0.4f) else PacMazePalette.cardBorder,
                        RoundedCornerShape(layout.dp(8.dp)),
                    )
                    .padding(horizontal = layout.dp(4.dp), vertical = layout.dp(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        skin.displayName,
                        color = if (selected) PacMazePalette.inkPrimary else PacMazePalette.inkSecondary,
                        fontSize = layout.bodySp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "$family · ${tier.label}",
                        color = if (selected) accent.copy(alpha = 0.85f) else PacMazePalette.inkHint,
                        fontSize = layout.captionSp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PacMazeCharacterSelectHintBar(
    loadout: PacMazeAvatarLoadout,
    layout: PacMazeHubLayoutSpec,
) {
    val skinId = loadout.skinId
    val accent = pacMazeSkinAccent(skinId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(layout.dp(10.dp)))
            .background(Color(0xFF151B28).copy(alpha = 0.85f))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(layout.dp(10.dp)))
            .padding(horizontal = layout.cardPad, vertical = layout.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
    ) {
        Text(skinId.emoji, fontSize = layout.subtitleSp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "当前：${skinId.displayName} + ${loadout.trailId.displayName}",
                color = PacMazePalette.inkPrimary,
                fontSize = layout.bodySp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                skinId.subtitle,
                color = PacMazePalette.inkMuted,
                fontSize = layout.captionSp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.15f))
                .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(2.dp)),
        ) {
            Text(
                pacMazeSkinTag(skinId),
                color = accent,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PacMazeCharacterConfirmButton(
    onClick: () -> Unit,
    accent: Color,
    layout: PacMazeHubLayoutSpec,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(layout.cardRadius)
    Box(
        modifier = modifier
            .height(layout.dp(if (layout.isCompactHeight) 44.dp else 48.dp))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(PacMazePalette.accentOrange, accent.copy(alpha = 0.85f)),
                ),
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), shape)
            .pacMazeClickable(sound = PacMazeUiSoundId.GridSelect, onClick = onClick)
            .padding(horizontal = layout.cardPad),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(8.dp)),
        ) {
            Text(
                "确认角色",
                color = Color.White,
                fontSize = layout.buttonSp,
                fontWeight = FontWeight.Black,
            )
            if (!layout.isCompactHeight) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = layout.dp(8.dp), vertical = layout.dp(3.dp)),
                ) {
                    Text(
                        "返回选关",
                        color = Color.White,
                        fontSize = layout.bodySp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(layout.dp(18.dp)),
            )
        }
    }
}
