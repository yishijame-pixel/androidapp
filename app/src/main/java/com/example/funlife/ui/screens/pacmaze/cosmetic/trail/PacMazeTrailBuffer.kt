package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

class PacMazeTrailBuffer(private val capacity: Int = 24) {
    private val samples = ArrayDeque<PacMazeTrailSample>()

    fun push(
        position: Offset,
        velocity: Offset,
        powerBoost: Boolean,
        minStepPx: Float = 0.75f,
    ) {
        if (samples.isNotEmpty()) {
            val last = samples.last().position
            if (hypot(position.x - last.x, position.y - last.y) < minStepPx) return
        }
        samples.addLast(
            PacMazeTrailSample(
                position = position,
                velocity = velocity,
                age = 0f,
                powerBoost = powerBoost,
            ),
        )
        while (samples.size > capacity) samples.removeFirst()
    }

    fun reset() {
        samples.clear()
    }

    fun snapshot(): List<PacMazeTrailSample> {
        val size = samples.size.coerceAtLeast(1)
        return samples.mapIndexed { index, sample ->
            sample.copy(age = (index + 1).toFloat() / size.toFloat())
        }
    }
}
