package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

const val PAC_MAZE_PLAYER_SCALE_MIN = 0.5f
const val PAC_MAZE_PLAYER_SCALE_MAX = 1.5f
const val PAC_MAZE_PLAYER_SCALE_DEFAULT = 1f

@Composable
fun PacMazePlayerScaleControl(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "角色",
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
    }
}
