package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.PacMazePalette

@Composable
fun PacMazeActionCluster(
    modifier: Modifier = Modifier,
    attackCharges: Int,
    attackEnabled: Boolean,
    onAttack: () -> Unit,
    onPause: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onAttack,
                enabled = attackEnabled,
                modifier = Modifier
                    .size(76.dp)
                    .alpha(if (attackEnabled) 1f else 0.5f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (attackEnabled) {
                                listOf(Color(0xFFFFAB40), Color(0xFFE64A19))
                            } else {
                                listOf(Color(0xFF334155), Color(0xFF1E293B))
                            },
                        ),
                    )
                    .border(
                        width = if (attackEnabled) 3.dp else 1.5.dp,
                        color = if (attackEnabled) Color(0xFFFFE082) else PacMazePalette.cardBorder,
                        shape = CircleShape,
                    ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "攻",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (attackCharges > 0) {
                        Text(
                            attackCharges.toString(),
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                if (attackEnabled) "能量弹" else if (attackCharges > 0) "冷却中" else "需能量豆",
                color = if (attackEnabled) PacMazePalette.accentGold else PacMazePalette.inkMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PacMazePalette.cardFillElevated,
                                Color(0xFF151B2B),
                            ),
                        ),
                    )
                    .border(1.5.dp, PacMazePalette.cardBorderStrong, CircleShape),
            ) {
                Icon(Icons.Default.Pause, contentDescription = "暂停", tint = PacMazePalette.inkPrimary)
            }
            Text(
                "暂停",
                color = PacMazePalette.inkMuted,
                fontSize = 11.sp,
            )
        }
    }
}
