package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeMapMarker
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterId

data class PacMazeMapRenderContext(
    val world: PacMazeWorldState,
    val previous: PacMazeWorldState?,
    val blend: Float,
    val cell: Float,
    val offsetX: Float,
    val offsetY: Float,
    val mapW: Float,
    val mapH: Float,
    val animPhase: Float,
    val canvasSize: Size,
    val config: PacMazeThemeConfig,
    val renderAnchor: (PacMazeEntity) -> Pair<Float, Float>,
    val playerTrail: List<Offset> = emptyList(),
    val markers: List<PacMazeMapMarker> = emptyList(),
    val playerCharacterId: PacMazeCharacterId = PacMazeCharacterId.CLASSIC_PAC,
    val playerDrawScale: Float = 1f,
) {
    fun gridToScreen(gridX: Float, gridY: Float): Offset =
        Offset(offsetX + (gridX + 0.5f) * cell, offsetY + (gridY + 0.5f) * cell)

    fun entityCenter(entity: PacMazeEntity): Offset {
        val (ax, ay) = renderAnchor(entity)
        return gridToScreen(ax, ay)
    }

    fun tileRect(x: Int, y: Int): Rect = Rect(
        offsetX + x * cell,
        offsetY + y * cell,
        offsetX + (x + 1) * cell,
        offsetY + (y + 1) * cell,
    )
}
