package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemePalette

interface PacMazeTrailRenderer {
    val trailId: PacMazeTrailId

    fun draw(
        scope: DrawScope,
        samples: List<PacMazeTrailSample>,
        palette: PacMazeThemePalette,
        cell: Float,
        powerActive: Boolean,
    )
}
