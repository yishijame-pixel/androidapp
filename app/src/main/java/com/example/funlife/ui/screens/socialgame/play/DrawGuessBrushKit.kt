package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.ui.graphics.Color

object DrawColorPalette {
    val defaultHex = "#1A1A1A"
    val brushSizes = listOf(2f, 5f, 10f, 18f)
    val swatches = listOf(
        "#1A1A1A", "#FFFFFF", "#FF5B35", "#FF9F1C",
        "#FFD166", "#2EC4B6", "#4CC9F0", "#7B2D8B",
        "#F72585", "#4361EE", "#06D6A0", "#EF476F",
    )

    fun toColor(hex: String): Color =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(Color(0xFF1A1A1A))
}
