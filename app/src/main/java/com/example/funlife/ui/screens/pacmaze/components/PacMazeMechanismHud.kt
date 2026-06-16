package com.example.funlife.ui.screens.pacmaze.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import com.example.funlife.social.game.engine.pacmaze.PacMazeHazardKind
import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelConfig
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapDynamics
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeMarkerKind
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.TileType
import com.example.funlife.ui.screens.pacmaze.PacMazePalette
import com.example.funlife.ui.screens.pacmaze.currentPacMazePlayLayout
import com.example.funlife.ui.screens.pacmaze.pacMazeUiClick
import com.example.funlife.ui.screens.pacmaze.PacMazeUiSoundId

/** 机关相位与闯关目标进度（移动墙 / 能量门 / checkpoint）。 */
@Composable
fun PacMazeMechanismHud(
    world: PacMazeWorldState,
    levelConfig: PacMazeLevelConfig?,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val play = currentPacMazePlayLayout()
    val hasDynamic = world.tiles.any { it == TileType.DYNAMIC_WALL.code }
    val hasEnergyGate = world.tiles.any { it == TileType.ENERGY_GATE.code }
    val hasLasers = world.hazards.any {
        it.kind == PacMazeHazardKind.LASER_ROW || it.kind == PacMazeHazardKind.LASER_COL
    }
    val fogEnabled = levelConfig?.modeRules?.fogEnabled == true
    val radarEnabled = levelConfig?.modeRules?.radarEnabled == true
    val objectives = objectiveMarkers(levelConfig, world)
    if (!hasDynamic && !hasEnergyGate && !hasLasers && objectives.isEmpty() && !fogEnabled && !radarEnabled) return

    val context = LocalContext.current
    val labelSp = (7.5f * play.scale).coerceAtLeast(6.5f).sp
    val valueSp = (7.5f * play.scale).coerceAtLeast(6.5f).sp
    val radius = play.dp(8.dp)

    if (!expanded) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(radius))
                .background(Color.Black.copy(alpha = 0.2f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(radius))
                .clickable(onClick = pacMazeUiClick(context, PacMazeUiSoundId.Toggle) { onExpandedChange(true) })
                .padding(horizontal = play.dp(5.dp), vertical = play.dp(3.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(play.dp(1.dp)),
        ) {
            Text("机关", color = PacMazePalette.inkMuted.copy(alpha = 0.7f), fontSize = labelSp)
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "展开机关信息",
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(play.dp(9.dp)),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Color(0xCC0D1524))
            .border(1.dp, PacMazePalette.cardBorder, RoundedCornerShape(radius))
            .padding(horizontal = play.dp(6.dp), vertical = play.dp(4.dp)),
        verticalArrangement = Arrangement.spacedBy(play.dp(2.dp)),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = pacMazeUiClick(context, PacMazeUiSoundId.Toggle) { onExpandedChange(false) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(play.dp(4.dp)),
        ) {
            Text("机关", color = PacMazePalette.inkMuted, fontSize = labelSp, fontWeight = FontWeight.Medium)
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "收起机关信息",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(play.dp(11.dp)),
            )
        }
        if (hasDynamic) {
            DynamicPhaseRow(world = world, labelSp = labelSp, valueSp = valueSp)
        }
        if (hasEnergyGate) {
            EnergyGateRow(open = world.energyGateOpen, synced = world.levelId >= 20, labelSp = labelSp, valueSp = valueSp)
        }
        if (hasLasers) {
            LaserPhaseRow(world = world, labelSp = labelSp, valueSp = valueSp)
        }
        if (fogEnabled || radarEnabled) {
            FogRadarRow(
                world = world,
                fogEnabled = fogEnabled,
                radarEnabled = radarEnabled,
                labelSp = labelSp,
                valueSp = valueSp,
            )
        }
        if (objectives.isNotEmpty()) {
            ObjectiveRow(
                objectives = objectives,
                visited = world.visitedCheckpointTags,
                labelSp = labelSp,
                valueSp = valueSp,
            )
        }
    }
}

@Composable
private fun DynamicPhaseRow(
    world: PacMazeWorldState,
    labelSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    val play = currentPacMazePlayLayout()
    val rate = PacMazeMapDynamics.dynamicPhaseTicks(world.levelId)
    val phase = (world.dynamicsTick / rate) % 3
    val ticksLeft = rate - (world.dynamicsTick % rate)
    val secondsLeft = (ticksLeft + 59) / 60
    val dotSize = play.dp(5.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(play.dp(3.dp)),
    ) {
        Text("移动墙", color = PacMazePalette.inkMuted, fontSize = labelSp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(play.dp(2.dp))) {
            repeat(3) { index ->
                val active = index == phase.toInt()
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            if (active) PacMazePalette.accentCyan else Color(0xFF334155),
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (active) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                            shape = CircleShape,
                        ),
                )
            }
        }
        Text(
            "${secondsLeft}s",
            color = PacMazePalette.accentCyan,
            fontSize = valueSp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LaserPhaseRow(
    world: PacMazeWorldState,
    labelSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    val cycle = PacMazeConstants.LASER_WARN_TICKS + PacMazeConstants.LASER_LETHAL_TICKS
    val phase = (world.tick % cycle).toInt()
    val lethal = phase >= PacMazeConstants.LASER_WARN_TICKS
    val ticksLeft = if (lethal) cycle - phase else PacMazeConstants.LASER_WARN_TICKS - phase
    val secondsLeft = (ticksLeft + 59) / 60

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("激光", color = PacMazePalette.inkMuted, fontSize = labelSp, fontWeight = FontWeight.Medium)
        Text(
            if (lethal) "扫射" else "预警",
            color = if (lethal) PacMazePalette.accentOrange else PacMazePalette.accentMint,
            fontSize = valueSp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${secondsLeft}s",
            color = if (lethal) PacMazePalette.accentOrange else PacMazePalette.inkSecondary,
            fontSize = valueSp,
        )
    }
}

@Composable
private fun EnergyGateRow(
    open: Boolean,
    synced: Boolean,
    labelSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("能量门", color = PacMazePalette.inkMuted, fontSize = labelSp)
        Text(
            if (open) "开放" else "关闭",
            color = if (open) PacMazePalette.accentMint else PacMazePalette.accentOrange,
            fontSize = valueSp,
            fontWeight = FontWeight.Bold,
        )
        if (synced) {
            Text("· 同步条纹", color = PacMazePalette.inkHint, fontSize = (valueSp.value * 0.92f).sp)
        }
    }
}

@Composable
private fun FogRadarRow(
    world: PacMazeWorldState,
    fogEnabled: Boolean,
    radarEnabled: Boolean,
    labelSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    val exploredPct = if (world.width * world.height > 0) {
        (world.exploredTiles.size * 100) / (world.width * world.height)
    } else {
        0
    }
    val radarCooldownSec = (world.radarCooldownTicksLeft + PacMazeConstants.TICKS_PER_SECOND - 1) /
        PacMazeConstants.TICKS_PER_SECOND
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (fogEnabled) {
            Text("迷雾", color = PacMazePalette.inkMuted, fontSize = labelSp)
            Text("${exploredPct}%", color = PacMazePalette.accentCyan, fontSize = valueSp, fontWeight = FontWeight.Bold)
        }
        if (radarEnabled) {
            if (fogEnabled) {
                Text("·", color = PacMazePalette.inkHint, fontSize = valueSp)
            }
            Text("雷达", color = PacMazePalette.inkMuted, fontSize = labelSp)
            Text(
                when {
                    world.radarRevealTicksLeft > 0 -> "扫描中"
                    radarCooldownSec > 0 -> "${radarCooldownSec}s"
                    else -> "就绪"
                },
                color = when {
                    world.radarRevealTicksLeft > 0 -> PacMazePalette.accentCyan
                    radarCooldownSec > 0 -> PacMazePalette.inkSecondary
                    else -> PacMazePalette.accentMint
                },
                fontSize = valueSp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ObjectiveRow(
    objectives: List<PacMazeMapMarker>,
    visited: Set<String>,
    labelSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("目标", color = PacMazePalette.inkMuted, fontSize = labelSp)
        objectives.forEach { marker ->
            val done = marker.tag in visited
            Text(
                text = marker.label.ifBlank { "?" },
                color = if (done) PacMazePalette.accentMint else PacMazePalette.inkSecondary,
                fontSize = valueSp,
                fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (done) Color(0x3322C55E) else Color(0x332563EB))
                    .padding(horizontal = 3.dp, vertical = 0.dp),
            )
        }
    }
}

private fun objectiveMarkers(
    levelConfig: PacMazeLevelConfig?,
    @Suppress("UNUSED_PARAMETER") world: PacMazeWorldState,
): List<PacMazeMapMarker> {
    val required = levelConfig?.starCriteria?.threeStarRequiredTags ?: return emptyList()
    if (required.isEmpty()) return emptyList()
    return levelConfig.markers.filter {
        it.kind == PacMazeMarkerKind.CHECKPOINT && it.tag in required
    }
}
