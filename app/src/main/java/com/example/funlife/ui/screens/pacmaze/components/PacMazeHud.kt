package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.screens.pacmaze.PacMazePalette

@Composable
fun PacMazeHud(
    modifier: Modifier = Modifier,
    levelId: Int,
    score: Int,
    lives: Int,
    elapsedSeconds: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xCC0B1020),
                        Color(0x99151B33),
                        Color(0xCC0B1020),
                    ),
                ),
            )
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = PacMazePalette.inkPrimary)
        }

        HudChip(label = "关卡", value = "第 $levelId 关")

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(lives.coerceAtLeast(0)) {
                Text("♥", color = Color(0xFFFF5252), fontSize = 18.sp)
            }
        }

        HudChip(label = "得分", value = score.toString(), accent = PacMazePalette.accentGold)

        HudChip(
            label = "时间",
            value = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
        )
    }
}

@Composable
private fun HudChip(
    label: String,
    value: String,
    accent: Color = PacMazePalette.inkPrimary,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = PacMazePalette.inkMuted, fontSize = 11.sp)
            Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
