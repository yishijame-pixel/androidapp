package com.example.funlife.ui.screens.pacmaze.maptheme

import androidx.compose.ui.graphics.Color

/** Compose Color 要求 alpha ∈ [0,1]，动画/噪声计算后必须钳制。 */
internal fun Color.themeAlpha(alpha: Float): Color {
    val safe = when {
        alpha.isNaN() -> 1f
        alpha < 0f -> 0f
        alpha > 1f -> 1f
        else -> alpha
    }
    return copy(alpha = safe)
}

/** 草地格渐变用的 patch 亮度，保证任意 (x,y) 不越界。 */
internal fun grassPatchAlpha(seed: Int): Float {
    val patch = (seed % 7) / 14f
    return (0.78f + patch * 0.18f).coerceIn(0f, 1f)
}
