package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.PacMazeObjectiveLine
import com.example.funlife.ui.screens.pacmaze.PacMazeOverlayCard
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.PacMazePrimaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeSecondaryButton
import com.example.funlife.ui.screens.pacmaze.PacMazeStarRow

@Composable
fun PacMazeResultOverlay(
    title: String,
    message: String,
    stars: Int,
    primary: String,
    secondary: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    objectives: List<PacMazeObjectiveLine> = emptyList(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        PacMazeOverlayCard {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    title,
                    color = PacMazePalette.accentGold,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(message, color = PacMazePalette.inkSecondary, fontSize = 15.sp)
                if (objectives.isNotEmpty()) {
                    PacMazeObjectiveSummary(objectives = objectives)
                }
                if (stars > 0) {
                    PacMazeStarRow(stars = stars, starSize = 22.sp)
                }
                PacMazePrimaryButton(text = primary, onClick = onPrimary)
                PacMazeSecondaryButton(text = secondary, onClick = onSecondary)
            }
        }
    }
}

@Composable
private fun PacMazeObjectiveSummary(objectives: List<PacMazeObjectiveLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2438), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("闯关目标", color = PacMazePalette.inkMuted, fontSize = 11.sp)
        objectives.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (line.done) "✓" else "○",
                    color = if (line.done) PacMazePalette.accentMint else PacMazePalette.inkHint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    line.label,
                    color = if (line.done) PacMazePalette.inkPrimary else PacMazePalette.inkSecondary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
