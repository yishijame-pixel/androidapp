package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset

data class PacMazeTrailSample(
    val position: Offset,
    val velocity: Offset,
    val age: Float,
    val powerBoost: Boolean,
)
