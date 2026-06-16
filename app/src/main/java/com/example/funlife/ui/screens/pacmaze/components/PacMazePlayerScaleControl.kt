package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.PacMazeMovementMode

const val PAC_MAZE_PLAYER_SCALE_MIN = 0.5f
const val PAC_MAZE_PLAYER_SCALE_MAX = 3.5f
const val PAC_MAZE_PLAYER_SCALE_DEFAULT = 1f

private val SCALE_PRESETS = listOf(0.75f, 1f, 1.35f, 1.75f)

@Composable
fun PacMazePlayerScaleControl(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    accent: Color,
    movementMode: PacMazeMovementMode = PacMazeMovementMode.AUTO,
    onMovementModeChange: (PacMazeMovementMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "角色大小",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${(scale * 100).toInt()}%",
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            valueRange = PAC_MAZE_PLAYER_SCALE_MIN..PAC_MAZE_PLAYER_SCALE_MAX,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            SCALE_PRESETS.forEach { preset ->
                val selected = kotlin.math.abs(scale - preset) < 0.04f
                FilterChip(
                    selected = selected,
                    onClick = { onScaleChange(preset) },
                    label = { Text("${(preset * 100).toInt()}%") },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = movementMode == PacMazeMovementMode.AUTO,
                onClick = { onMovementModeChange(PacMazeMovementMode.AUTO) },
                label = { Text("滑行") },
            )
            FilterChip(
                selected = movementMode == PacMazeMovementMode.MANUAL,
                onClick = { onMovementModeChange(PacMazeMovementMode.MANUAL) },
                label = { Text("手控") },
            )
        }
        Text(
            when (movementMode) {
                PacMazeMovementMode.AUTO -> "滑行：松手后沿方向持续移动"
                PacMazeMovementMode.MANUAL -> "手控：松手即停，按住才走"
            },
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 8.sp,
        )
    }
}
