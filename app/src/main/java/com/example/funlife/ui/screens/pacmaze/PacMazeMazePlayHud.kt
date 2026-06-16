package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.ghostReleaseSecondsCeil
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeMechanics
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState

@Composable
fun PacMazeGhostReleaseBanner(
    world: PacMazeWorldState,
    modifier: Modifier = Modifier,
) {
    if (world.ghostReleaseTicksLeft <= 0) return
    val seconds = world.ghostReleaseSecondsCeil()
    val play = currentPacMazePlayLayout()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE6182030))
            .border(1.dp, PacMazePalette.accentGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = play.dp(12.dp), vertical = play.dp(6.dp)),
        horizontalArrangement = Arrangement.spacedBy(play.dp(6.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🛡️", fontSize = play.statSp)
        Text(
            if (seconds > 0) "安全期 · 幽灵 ${seconds}s 后出动" else "幽灵即将出动",
            color = PacMazePalette.accentGold,
            fontSize = play.statSp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun PacMazeGhostProximityOverlay(
    world: PacMazeWorldState,
    modifier: Modifier = Modifier,
) {
    val dist = PacMazeMazeMechanics.nearestGhostDistanceTiles(world) ?: return
    if (dist > 5) return
    val alpha = ((6 - dist) / 6f).coerceIn(0.08f, 0.42f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFD32F2F).copy(alpha = alpha)),
    )
}

@Composable
fun PacMazeMazeEchoHintBanner(
    world: PacMazeWorldState,
    modifier: Modifier = Modifier,
) {
    if (world.echoHintTicksLeft <= 0 || world.echoHintDirection == null) return
    val arrow = when (world.echoHintDirection) {
        Direction.UP -> "↑"
        Direction.DOWN -> "↓"
        Direction.LEFT -> "←"
        Direction.RIGHT -> "→"
    }
    Text(
        "回声指引 $arrow ${world.echoTargetKeyTag.replace("MAZE_KEY_", "钥印 ")}",
        color = PacMazePalette.accentGold,
        fontSize = currentPacMazePlayLayout().statSp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6182030))
            .border(1.dp, PacMazePalette.accentGold.copy(0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
fun PacMazeMazeSealedRejectBanner(
    world: PacMazeWorldState,
    modifier: Modifier = Modifier,
) {
    if (world.sealedKeyRejectFlashTicks <= 0) return
    Text(
        "封印未解 · 请按顺序收集钥印",
        color = Color(0xFFFF8A80),
        fontSize = currentPacMazePlayLayout().statSp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6301820))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
fun PacMazeMazeIntelHud(
    world: PacMazeWorldState,
    level: PacMazeLevelConfig,
    onRevealQuadrant: (Int) -> Unit,
    onKeyHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (level.modeRules.intelPointsMax <= 0) return
    val layout = currentPacMazePlayLayout()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE6101828))
            .border(1.dp, PacMazePalette.accentCyan.copy(0.45f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("情报 ${world.intelPointsRemaining}/${level.modeRules.intelPointsMax}", color = PacMazePalette.accentCyan, fontSize = layout.statSp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..3).forEach { q ->
                val revealed = q in world.intelQuadrantsRevealed
                Text(
                    if (revealed) "Q${q + 1}✓" else "Q${q + 1}",
                    color = if (revealed) PacMazePalette.inkHint else PacMazePalette.accentGold,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(0.06f))
                        .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, enabled = !revealed && world.intelPointsRemaining > 0, onClick = { onRevealQuadrant(q) })
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            "钥印方向 (-2)",
            color = if (world.intelPointsRemaining >= 2) PacMazePalette.accentGold else PacMazePalette.inkHint,
            fontSize = 9.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PacMazePalette.accentGold.copy(0.12f))
                .pacMazeClickable(sound = PacMazeUiSoundId.ChipAction, enabled = world.intelPointsRemaining >= 2, onClick = onKeyHint)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun PacMazeMazeHuntPhaseBanner(
    world: PacMazeWorldState,
    level: PacMazeLevelConfig,
    modifier: Modifier = Modifier,
) {
    if (!level.modeRules.huntEscalation || world.huntPhase <= 0) return
    Text(
        "追猎阶段 ${world.huntPhase} · 幽灵强化中",
        color = Color(0xFFFFAB40),
        fontSize = currentPacMazePlayLayout().statSp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xE6281808))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
