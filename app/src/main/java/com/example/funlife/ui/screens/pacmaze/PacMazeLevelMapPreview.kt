package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.funlife.social.game.engine.pacmaze.GhostKind
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LevelGridPreview(
    val width: Int,
    val height: Int,
    val rows: List<String>,
    val pacSpawn: Pair<Int, Int>?,
    val ghostSpawns: List<Pair<Int, Int>>,
)

private val WALL_CHARS = setOf('#', 'b', 'B', 'w', 't', 'T', '&')

@Composable
fun PacMazeLevelMapPreview(
    levelId: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val layout = currentPacMazeHubLayout()
    var preview by remember(levelId) { mutableStateOf<LevelGridPreview?>(null) }

    LaunchedEffect(levelId) {
        preview = withContext(Dispatchers.IO) {
            runCatching {
                val path = "pac_maze/levels/level_%03d.json".format(levelId)
                context.assets.open(path).bufferedReader().use { reader ->
                    parseGridPreview(reader.readText())
                }
            }.getOrNull()
        }
    }

    val shape = RoundedCornerShape(layout.dp(10.dp))
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.16f), Color(0xFF0E121C)),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.38f), shape)
            .padding(layout.dp(6.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = preview
            if (data == null || data.width <= 0 || data.height <= 0) {
                drawRect(Color.White.copy(alpha = 0.04f), size = size)
                return@Canvas
            }

            val pad = 2f
            val cellW = (size.width - pad * 2) / data.width
            val cellH = (size.height - pad * 2) / data.height
            val cell = minOf(cellW, cellH)
            val mapW = cell * data.width
            val mapH = cell * data.height
            val originX = (size.width - mapW) / 2f
            val originY = (size.height - mapH) / 2f

            drawRect(
                color = Color(0xFF080C14),
                topLeft = Offset(originX - pad, originY - pad),
                size = Size(mapW + pad * 2, mapH + pad * 2),
            )

            for (y in data.rows.indices) {
                val row = data.rows[y]
                for (x in row.indices) {
                    if (x >= data.width) continue
                    val ch = row[x]
                    val left = originX + x * cell
                    val top = originY + y * cell
                    val isWall = ch in WALL_CHARS
                    val color = when {
                        isWall -> accent.copy(alpha = 0.82f)
                        ch == '.' || ch == 'o' || ch == ' ' -> Color(0xFF1A2438)
                        ch == '*' -> PacMazePalette.accentGold.copy(alpha = 0.55f)
                        ch == '=' -> PacMazePalette.accentCyan.copy(alpha = 0.45f)
                        else -> Color(0xFF243048)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(left + 0.5f, top + 0.5f),
                        size = Size(cell - 1f, cell - 1f),
                    )
                }
            }

            data.ghostSpawns.forEachIndexed { index, (gx, gy) ->
                if (gx in 0 until data.width && gy in 0 until data.height) {
                    val kind = PacMazeLevelGhostsUi.rosterForLevel(levelId, data.ghostSpawns.size)
                        .getOrNull(index)?.kind ?: GhostKind.STRIKER
                    drawCircle(
                        color = pacMazeGhostAccent(kind).copy(alpha = 0.85f),
                        radius = cell * 0.28f,
                        center = Offset(originX + (gx + 0.5f) * cell, originY + (gy + 0.5f) * cell),
                    )
                }
            }

            data.pacSpawn?.let { (px, py) ->
                if (px in 0 until data.width && py in 0 until data.height) {
                    drawCircle(
                        color = PacMazePalette.accentGold,
                        radius = cell * 0.32f,
                        center = Offset(originX + (px + 0.5f) * cell, originY + (py + 0.5f) * cell),
                    )
                }
            }
        }
    }
}

private fun parseGridPreview(json: String): LevelGridPreview {
    val root = JsonParser.parseString(json).asJsonObject
    val rows = root.getAsJsonArray("grid").map { it.asString }
    val width = root.get("width")?.asInt ?: rows.firstOrNull()?.length ?: 0
    val height = root.get("height")?.asInt ?: rows.size
    val spawn = root.getAsJsonObject("spawn")
    val pac = spawn?.getAsJsonArray("pac")?.let { arr ->
        if (arr.size() >= 2) arr[0].asInt to arr[1].asInt else null
    }
    val ghosts = spawn?.getAsJsonArray("ghosts")?.mapNotNull { element ->
        val arr = element.asJsonArray
        if (arr.size() >= 2) arr[0].asInt to arr[1].asInt else null
    }.orEmpty()
    return LevelGridPreview(width = width, height = height, rows = rows, pacSpawn = pac, ghostSpawns = ghosts)
}
