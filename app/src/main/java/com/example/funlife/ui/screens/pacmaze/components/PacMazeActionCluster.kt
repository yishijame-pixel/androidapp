package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import com.example.funlife.social.game.engine.pacmaze.PacMazeRunMode
import com.example.funlife.ui.screens.pacmaze.currentPacMazePlayLayout

@Composable
fun PacMazeAttackButton(
    modifier: Modifier = Modifier,
    attackCharges: Int,
    attackEnabled: Boolean,
    onAttack: () -> Unit,
    runMode: PacMazeRunMode = PacMazeRunMode.CAMPAIGN,
    compact: Boolean = false,
) {
    val attackAccent = when (runMode) {
        PacMazeRunMode.ENDLESS -> Color(0xFFB388FF)
        PacMazeRunMode.MAZE -> Color(0xFFFFB74D)
        PacMazeRunMode.PRACTICE -> PacMazePalette.modePractice
        else -> Color(0xFFFFE082)
    }
    val attackSize = if (compact) 64.dp else 76.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(
            onClick = onAttack,
            enabled = attackEnabled,
            modifier = Modifier
                .size(attackSize)
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
                    color = if (attackEnabled) attackAccent else PacMazePalette.cardBorder,
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
}

@Composable
fun PacMazePauseButton(
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val play = currentPacMazePlayLayout()
    val size = play.dp(if (compact) 30.dp else 34.dp)
    val iconSize = play.dp(if (compact) 14.dp else 16.dp)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(play.dp(1.dp)),
    ) {
        IconButton(
            onClick = onPause,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.32f))
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape),
        ) {
            Icon(
                Icons.Default.Pause,
                contentDescription = "暂停",
                tint = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.size(iconSize),
            )
        }
        Text(
            "暂停",
            color = PacMazePalette.inkMuted.copy(alpha = 0.75f),
            fontSize = (7f * play.scale).coerceAtLeast(6f).sp,
        )
    }
}

/** @deprecated 攻击与暂停已拆分；对局使用 [PacMazeAttackButton] + 顶部 [PacMazePauseButton]。 */
@Composable
fun PacMazeActionCluster(
    modifier: Modifier = Modifier,
    attackCharges: Int,
    attackEnabled: Boolean,
    onAttack: () -> Unit,
    onPause: () -> Unit,
    runMode: PacMazeRunMode = PacMazeRunMode.CAMPAIGN,
    compact: Boolean = false,
) {
    PacMazeAttackButton(
        modifier = modifier,
        attackCharges = attackCharges,
        attackEnabled = attackEnabled,
        onAttack = onAttack,
        runMode = runMode,
        compact = compact,
    )
}
