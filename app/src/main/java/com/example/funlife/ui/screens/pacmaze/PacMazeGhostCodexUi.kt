package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.funlife.data.PacMazePrefs
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.example.funlife.social.game.engine.pacmaze.GhostSpecialty
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeGhostShapeDraw

@Composable
fun PacMazeGhostCodexGrid(
    userId: Long,
    prefs: PacMazePrefs,
    modifier: Modifier = Modifier,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        items(GhostKind.codexOrder, key = { it.id }) { kind ->
            PacMazeGhostCodexCell(
                kind = kind,
                unlocked = prefs.isGhostCodexUnlocked(userId, kind),
                deaths = prefs.ghostDeathCount(userId, kind),
                eats = prefs.ghostEatCount(userId, kind),
                layout = layout,
            )
        }
    }
}

@Composable
fun PacMazeGhostCodexCell(
    kind: GhostKind,
    unlocked: Boolean,
    deaths: Int,
    eats: Int,
    modifier: Modifier = Modifier,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    val accent = pacMazeGhostAccent(kind)
    val shape = RoundedCornerShape(layout.dp(8.dp))
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (unlocked) Color(0xFF151D30) else Color(0xFF10141E))
            .border(1.dp, if (unlocked) accent.copy(alpha = 0.45f) else PacMazePalette.cardBorder, shape)
            .padding(layout.dp(6.dp)),
        verticalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layout.dp(6.dp)),
        ) {
            Box(
                modifier = Modifier
                    .size(layout.dp(36.dp))
                    .clip(RoundedCornerShape(layout.dp(6.dp)))
                    .background(accent.copy(alpha = if (unlocked) 0.14f else 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                if (unlocked) {
                    GhostKindPreviewCanvas(kind = kind, accent = accent, modifier = Modifier.fillMaxSize())
                } else {
                    Text("?", color = PacMazePalette.locked, fontSize = layout.subtitleSp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (unlocked) kind.codexTitle else "？？？",
                    color = if (unlocked) accent else PacMazePalette.locked,
                    fontSize = layout.captionSp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (unlocked) kind.behaviorHint else "遭遇后解锁",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp * 0.92f,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (unlocked) {
            Text(
                "击倒 $eats · 被捉 $deaths",
                color = PacMazePalette.inkSecondary,
                fontSize = layout.captionSp * 0.9f,
            )
        }
    }
}

@Composable
fun PacMazeLevelGhostChip(
    kind: GhostKind,
    specialty: GhostSpecialty,
    modifier: Modifier = Modifier,
    layout: PacMazeHubLayoutSpec = currentPacMazeHubLayout(),
) {
    val accent = pacMazeGhostAccent(kind)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(layout.dp(8.dp)))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(layout.dp(8.dp)))
            .padding(horizontal = layout.dp(6.dp), vertical = layout.dp(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(layout.dp(4.dp)),
    ) {
        Box(modifier = Modifier.size(layout.dp(22.dp))) {
            GhostKindPreviewCanvas(kind = kind, accent = accent, modifier = Modifier.fillMaxSize())
        }
        Column {
            Text(
                "${kind.emoji} ${kind.displayName}",
                color = accent,
                fontSize = layout.captionSp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            if (specialty.isActive) {
                Text(
                    "${specialty.emoji} ${specialty.displayName}",
                    color = PacMazePalette.inkHint,
                    fontSize = layout.captionSp * 0.88f,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun GhostKindPreviewCanvas(
    kind: GhostKind,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.padding(2.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.34f
        PacMazeGhostShapeDraw.drawBody(
            scope = this,
            center = center,
            radius = radius,
            kind = kind,
            bodyColor = accent.copy(alpha = 0.92f),
            animPhase = 0.4f,
            wobble = 0f,
        )
        PacMazeGhostShapeDraw.drawEyes(
            scope = this,
            center = center,
            radius = radius,
            kind = kind,
            direction = null,
            frightened = false,
            top = center.y - radius,
            animPhase = 0.4f,
        )
    }
}
