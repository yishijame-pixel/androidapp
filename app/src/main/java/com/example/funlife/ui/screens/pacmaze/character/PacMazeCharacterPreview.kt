package com.example.funlife.ui.screens.pacmaze.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.pacMazeCharacterAccent
import kotlinx.coroutines.delay

@Composable
fun PacMazeCharacterPreview(
    characterId: PacMazeCharacterId,
    modifier: Modifier = Modifier,
    animateWalk: Boolean = true,
    facing: Direction = Direction.RIGHT,
    powerActive: Boolean = false,
    selected: Boolean = false,
) {
    var animPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animateWalk) {
        while (true) {
            delay(16L)
            animPhase += if (animateWalk) 0.12f else 0.04f
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF1A2236))
            .then(
                if (selected) {
                    Modifier.border(2.dp, PacMazePalette.accentGold, CircleShape)
                } else {
                    Modifier.border(1.dp, PacMazePalette.cardBorder, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.78f)) {
            val r = size.minDimension * 0.34f
            val c = Offset(size.width / 2f, size.height / 2f)
            PacMazeCharacterDraw.draw(
                scope = this,
                characterId = characterId,
                center = c,
                radius = r,
                pose = PacMazeCharacterPose(
                    facing = facing,
                    animPhase = animPhase,
                    isMoving = animateWalk,
                    powerActive = powerActive,
                ),
            )
        }
    }
}

/** 选角卡片专用：聚光灯舞台 + 更大绘制区域 */
@Composable
fun PacMazeCharacterStagePreview(
    characterId: PacMazeCharacterId,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    animateWalk: Boolean = true,
    powerActive: Boolean = false,
) {
    val accent = pacMazeCharacterAccent(characterId)
    var animPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animateWalk, characterId) {
        while (true) {
            delay(16L)
            animPhase += if (animateWalk) 0.12f else 0.04f
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = if (selected) 0.22f else 0.08f),
                        Color(0xFF0D1220),
                        Color(0xFF151B28),
                    ),
                ),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accent.copy(alpha = 0.75f) else PacMazePalette.cardBorder,
                shape = RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stageCenter = Offset(size.width / 2f, size.height * 0.62f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (selected) 0.35f else 0.12f),
                        Color.Transparent,
                    ),
                    center = stageCenter,
                    radius = size.minDimension * 0.48f,
                ),
                radius = size.minDimension * 0.48f,
                center = stageCenter,
            )
            drawOval(
                color = Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(
                    stageCenter.x - size.width * 0.28f,
                    stageCenter.y + size.height * 0.08f,
                ),
                size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.1f),
            )
        }
        Canvas(modifier = Modifier.fillMaxSize(0.88f)) {
            val r = size.minDimension * 0.38f
            val c = Offset(size.width / 2f, size.height * 0.52f)
            PacMazeCharacterDraw.draw(
                scope = this,
                characterId = characterId,
                center = c,
                radius = r,
                pose = PacMazeCharacterPose(
                    facing = Direction.RIGHT,
                    animPhase = animPhase,
                    isMoving = animateWalk,
                    powerActive = powerActive,
                ),
            )
        }
    }
}
