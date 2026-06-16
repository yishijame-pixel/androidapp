package com.example.funlife.ui.screens.pacmaze

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeGenerator
import com.example.funlife.social.game.engine.pacmaze.PacMazeMazeRunOptions
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MazeGridPreview(
    val width: Int,
    val height: Int,
    val rows: List<String>,
    val pacSpawn: Pair<Int, Int>?,
    val ghostSpawns: List<Pair<Int, Int>>,
    val exit: Pair<Int, Int>?,
    val keyCells: List<Pair<Int, Int>>,
)

private val MAZE_WALL_CHARS = setOf('#', 'b', 'B', 'w', 't', 'T', '&')

@Composable
fun PacMazeMazeRunPreview(
    options: PacMazeMazeRunOptions,
    modifier: Modifier = Modifier,
    showFogHint: Boolean = true,
    badge: String? = null,
    badgeAccent: Color = PacMazePalette.accentGold,
    dimmed: Boolean = false,
) {
    val layout = currentPacMazeHubLayout()
    var preview by remember(options.seed, options.difficulty, options.contract, options.dailyChallenge) {
        mutableStateOf<MazeGridPreview?>(null)
    }
    var loading by remember(options.seed, options.difficulty, options.contract, options.dailyChallenge) {
        mutableStateOf(true)
    }

    LaunchedEffect(options) {
        loading = true
        preview = withContext(Dispatchers.Default) {
            runCatching {
                val json = PacMazeMazeMazeGeneratorJson(options)
                parseMazeGridPreview(json)
            }.getOrNull()
        }
        loading = false
    }

    val shape = RoundedCornerShape(layout.dp(10.dp))
    Box(
        modifier = modifier
            .alpha(if (dimmed) 0.55f else 1f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(badgeAccent.copy(alpha = 0.22f), Color(0xFF0E1628)),
                ),
            )
            .border(
                width = if (badge != null) 1.5.dp else 1.dp,
                color = badgeAccent.copy(alpha = if (badge != null) 0.65f else 0.42f),
                shape = shape,
            )
            .padding(layout.dp(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (loading && preview == null) {
            Text("生成预览…", color = PacMazePalette.inkHint, fontSize = layout.captionSp)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = preview ?: return@Canvas
            if (data.width <= 0 || data.height <= 0) return@Canvas

            val pad = 2f
            val cellW = (size.width - pad * 2) / data.width
            val cellH = (size.height - pad * 2) / data.height
            val cell = minOf(cellW, cellH)
            val mapW = cell * data.width
            val mapH = cell * data.height
            val originX = (size.width - mapW) / 2f
            val originY = (size.height - mapH) / 2f

            drawRect(
                color = Color(0xFF081018),
                topLeft = Offset(originX - pad, originY - pad),
                size = Size(mapW + pad * 2, mapH + pad * 2),
            )

            for (y in data.rows.indices) {
                val row = data.rows[y]
                for (x in 0 until minOf(row.length, data.width)) {
                    val ch = row[x]
                    val left = originX + x * cell
                    val top = originY + y * cell
                    val color = when (ch) {
                        in MAZE_WALL_CHARS -> Color(0xFF8FA4B8)
                        'E' -> PacMazePalette.accentOrange.copy(alpha = 0.95f)
                        '=' -> PacMazePalette.accentGold.copy(alpha = 0.92f)
                        '.' -> Color(0xFF4FC3F7).copy(alpha = 0.85f)
                        'o', ' ' -> Color(0xFF2A4568)
                        else -> Color(0xFF3A5578)
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(left + 0.6f, top + 0.6f),
                        size = Size(cell - 1.2f, cell - 1.2f),
                    )
                }
            }

            data.keyCells.forEach { (kx, ky) ->
                drawCircle(
                    color = PacMazePalette.accentGold,
                    radius = cell * 0.22f,
                    center = Offset(originX + (kx + 0.5f) * cell, originY + (ky + 0.5f) * cell),
                )
            }

            data.exit?.let { (ex, ey) ->
                drawCircle(
                    color = PacMazePalette.accentOrange.copy(alpha = 0.55f),
                    radius = cell * 0.38f,
                    center = Offset(originX + (ex + 0.5f) * cell, originY + (ey + 0.5f) * cell),
                )
            }

            data.ghostSpawns.forEach { (gx, gy) ->
                drawCircle(
                    color = Color(0xFFE57373).copy(alpha = 0.9f),
                    radius = cell * 0.26f,
                    center = Offset(originX + (gx + 0.5f) * cell, originY + (gy + 0.5f) * cell),
                )
            }

            data.pacSpawn?.let { (px, py) ->
                drawCircle(
                    color = PacMazePalette.accentMint,
                    radius = cell * 0.3f,
                    center = Offset(originX + (px + 0.5f) * cell, originY + (py + 0.5f) * cell),
                )
            }

            if (showFogHint) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFF020408).copy(alpha = 0.38f)),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.58f,
                    ),
                    size = size,
                )
            }
        }
        badge?.let { label ->
            Text(
                label,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeAccent.copy(alpha = 0.88f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

private fun PacMazeMazeMazeGeneratorJson(options: PacMazeMazeRunOptions): String =
    PacMazeMazeGenerator.buildLevelJson(options)

internal fun parseMazeGridPreview(json: String): MazeGridPreview {
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

    var exit: Pair<Int, Int>? = null
    val keys = mutableListOf<Pair<Int, Int>>()
    for (y in rows.indices) {
        for (x in rows[y].indices) {
            when (rows[y][x]) {
                'E' -> exit = x to y
                '=' -> keys += x to y
            }
        }
    }

    root.getAsJsonArray("markers")?.forEach { element ->
        val obj = element.asJsonObject
        val type = obj.get("type")?.asString
        val mx = obj.get("x")?.asInt ?: return@forEach
        val my = obj.get("y")?.asInt ?: return@forEach
        when (type) {
            "exit" -> exit = mx to my
            "checkpoint" -> if (mx to my !in keys) keys += mx to my
        }
    }

    return MazeGridPreview(
        width = width,
        height = height,
        rows = rows,
        pacSpawn = pac,
        ghostSpawns = ghosts,
        exit = exit,
        keyCells = keys,
    )
}
