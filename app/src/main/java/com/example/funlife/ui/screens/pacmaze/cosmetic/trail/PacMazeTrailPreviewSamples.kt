package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin

/** 工坊/收藏册预览用静态拖尾采样，避免首帧空白或未开启动画时缩略图全黑。 */
object PacMazeTrailPreviewSamples {

    fun swatch(
        width: Float,
        height: Float,
        phase: Float = 0f,
        count: Int = 16,
    ): List<PacMazeTrailSample> = buildSamples(
        count = count,
        phase = phase,
        step = 0.05f,
    ) { t ->
        Offset(
            x = (80f + sin(t) * 28f) / 160f * width,
            y = (40f + cos(t * 1.3f) * 6f) / 80f * height,
        ) to Offset(cos(t) * 40f, 0f)
    }

    fun stage(
        width: Float,
        height: Float,
        phase: Float = 0f,
        count: Int = 24,
        powerBoost: Boolean = false,
    ): List<PacMazeTrailSample> = buildSamples(
        count = count,
        phase = phase,
        step = 0.042f,
        powerBoost = powerBoost,
    ) { t ->
        Offset(
            x = (0.5f + sin(t) * 0.36f) * width,
            y = (0.54f + sin(t * 1.55f + 0.6f) * 0.14f) * height,
        ) to Offset(cos(t) * 55f, cos(t * 1.55f) * 18f)
    }

    private inline fun buildSamples(
        count: Int,
        phase: Float,
        step: Float,
        powerBoost: Boolean = false,
        positionAt: (Float) -> Pair<Offset, Offset>,
    ): List<PacMazeTrailSample> {
        if (count <= 0) return emptyList()
        return List(count) { index ->
            val t = phase - (count - 1 - index) * step
            val (position, velocity) = positionAt(t)
            PacMazeTrailSample(
                position = position,
                velocity = velocity,
                age = (index + 1f) / count,
                powerBoost = powerBoost,
            )
        }
    }
}
