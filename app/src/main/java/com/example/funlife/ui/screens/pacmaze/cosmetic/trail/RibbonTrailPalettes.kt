package com.example.funlife.ui.screens.pacmaze.cosmetic.trail

import androidx.compose.ui.graphics.Color
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeTrailId

internal data class RibbonTrailPalette(
    val trailId: PacMazeTrailId,
    val glowTail: Color,
    val glowMid: Color,
    val glowHead: Color,
    val footInner: Color,
    val footOuter: Color,
    val sparkleSecondary: Color,
    val coreMid: Color,
    val sparkSeed: Int,
    val colorAt: (progress: Float, powerActive: Boolean) -> Color,
)

internal fun lerpRibbonColor(a: Color, b: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f,
    )
}

private fun steppedColor(
    t: Float,
    powerActive: Boolean,
    normal: List<Pair<Float, Color>>,
    powered: List<Pair<Float, Color>>,
): Color {
    val stops = if (powerActive) powered else normal
    val clamped = t.coerceIn(0f, 1f)
    for (i in 1 until stops.size) {
        val (t1, c1) = stops[i]
        if (clamped <= t1) {
            val (t0, c0) = stops[i - 1]
            val span = (t1 - t0).coerceAtLeast(0.001f)
            return lerpRibbonColor(c0, c1, (clamped - t0) / span)
        }
    }
    return stops.last().second
}

internal object RibbonTrailPalettes {

    val FLOW = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_FLOW,
        glowTail = Color(0xFF4338CA),
        glowMid = Color(0xFF818CF8),
        glowHead = Color(0xFFFF8A80),
        footInner = Color(0xFFFF8A80),
        footOuter = Color(0xFF7DD3FC),
        sparkleSecondary = Color(0xFF7DD3FC),
        coreMid = Color(0xFFFFAB91),
        sparkSeed = 7,
        colorAt = { t, power ->
            if (power) {
                steppedColor(
                    t, true,
                    normal = emptyList(),
                    powered = listOf(
                        0f to Color(0xFF312E81),
                        0.35f to Color(0xFF6366F1),
                        0.65f to Color(0xFF38BDF8),
                        1f to Color(0xFFFFD54F),
                    ),
                )
            } else {
                steppedColor(
                    t, false,
                    normal = listOf(
                        0f to Color(0xFF3730A3),
                        0.4f to Color(0xFF6366F1),
                        0.72f to Color(0xFF7DD3FC),
                        1f to Color(0xFFFF8A80),
                    ),
                    powered = emptyList(),
                )
            }
        },
    )

    val SAKURA = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_SAKURA,
        glowTail = Color(0xFFF472B6),
        glowMid = Color(0xFFFBCFE8),
        glowHead = Color(0xFFFFF1F2),
        footInner = Color(0xFFFFB7C5),
        footOuter = Color(0xFFF9A8D4),
        sparkleSecondary = Color(0xFFFFE4E6),
        coreMid = Color(0xFFFFCCD5),
        sparkSeed = 13,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFFBE185D),
                    0.35f to Color(0xFFEC4899),
                    0.68f to Color(0xFFF9A8D4),
                    1f to Color(0xFFFFF1F2),
                ),
                powered = listOf(
                    0f to Color(0xFF9D174D),
                    0.32f to Color(0xFFF43F5E),
                    0.64f to Color(0xFFFDA4AF),
                    1f to Color(0xFFFFFFFF),
                ),
            )
        },
    )

    val AURORA = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_AURORA,
        glowTail = Color(0xFF064E3B),
        glowMid = Color(0xFF2DD4BF),
        glowHead = Color(0xFFC4B5FD),
        footInner = Color(0xFF5EEAD4),
        footOuter = Color(0xFF818CF8),
        sparkleSecondary = Color(0xFF99F6E4),
        coreMid = Color(0xFFA7F3D0),
        sparkSeed = 21,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF134E4A),
                    0.38f to Color(0xFF14B8A6),
                    0.7f to Color(0xFF67E8F9),
                    1f to Color(0xFFC4B5FD),
                ),
                powered = listOf(
                    0f to Color(0xFF022C22),
                    0.3f to Color(0xFF2DD4BF),
                    0.6f to Color(0xFF38BDF8),
                    1f to Color(0xFFE9D5FF),
                ),
            )
        },
    )

    val PHOENIX = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_PHOENIX,
        glowTail = Color(0xFF7C2D12),
        glowMid = Color(0xFFF97316),
        glowHead = Color(0xFFFDE68A),
        footInner = Color(0xFFFF7043),
        footOuter = Color(0xFFFFD54F),
        sparkleSecondary = Color(0xFFFFAB40),
        coreMid = Color(0xFFFFCC80),
        sparkSeed = 33,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF9A3412),
                    0.4f to Color(0xFFEA580C),
                    0.72f to Color(0xFFFBBF24),
                    1f to Color(0xFFFFF59D),
                ),
                powered = listOf(
                    0f to Color(0xFF7F1D1D),
                    0.32f to Color(0xFFEF4444),
                    0.62f to Color(0xFFF59E0B),
                    1f to Color(0xFFFFF176),
                ),
            )
        },
    )

    val SOUL = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_SOUL,
        glowTail = Color(0xFF312E81),
        glowMid = Color(0xFF818CF8),
        glowHead = Color(0xFF67E8F9),
        footInner = Color(0xFFA78BFA),
        footOuter = Color(0xFF22D3EE),
        sparkleSecondary = Color(0xFFC4B5FD),
        coreMid = Color(0xFFBAE6FD),
        sparkSeed = 42,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF3730A3),
                    0.42f to Color(0xFF6366F1),
                    0.74f to Color(0xFF38BDF8),
                    1f to Color(0xFFE0F2FE),
                ),
                powered = listOf(
                    0f to Color(0xFF1E1B4B),
                    0.35f to Color(0xFF7C3AED),
                    0.65f to Color(0xFF22D3EE),
                    1f to Color(0xFFF0ABFC),
                ),
            )
        },
    )

    val JADE = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_JADE,
        glowTail = Color(0xFF14532D),
        glowMid = Color(0xFF4ADE80),
        glowHead = Color(0xFFFDE68A),
        footInner = Color(0xFF86EFAC),
        footOuter = Color(0xFFFACC15),
        sparkleSecondary = Color(0xFFBBF7D0),
        coreMid = Color(0xFFD9F99D),
        sparkSeed = 55,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF166534),
                    0.4f to Color(0xFF22C55E),
                    0.72f to Color(0xFFA3E635),
                    1f to Color(0xFFFEF08A),
                ),
                powered = listOf(
                    0f to Color(0xFF14532D),
                    0.34f to Color(0xFF16A34A),
                    0.66f to Color(0xFFEAB308),
                    1f to Color(0xFFFFF9C4),
                ),
            )
        },
    )

    val CINNABAR = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_CINNABAR,
        glowTail = Color(0xFF7F1D1D),
        glowMid = Color(0xFFDC2626),
        glowHead = Color(0xFFFECACA),
        footInner = Color(0xFFEF4444),
        footOuter = Color(0xFFF97316),
        sparkleSecondary = Color(0xFFFBBF24),
        coreMid = Color(0xFFFCA5A5),
        sparkSeed = 61,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF991B1B),
                    0.38f to Color(0xFFDC2626),
                    0.7f to Color(0xFFFB923C),
                    1f to Color(0xFFFEF2F2),
                ),
                powered = listOf(
                    0f to Color(0xFF450A0A),
                    0.32f to Color(0xFFEF4444),
                    0.62f to Color(0xFFF59E0B),
                    1f to Color(0xFFFFF7ED),
                ),
            )
        },
    )

    val CELADON = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_CELADON,
        glowTail = Color(0xFF134E4A),
        glowMid = Color(0xFF5EEAD4),
        glowHead = Color(0xFFE0F2FE),
        footInner = Color(0xFF2DD4BF),
        footOuter = Color(0xFF7DD3FC),
        sparkleSecondary = Color(0xFF99F6E4),
        coreMid = Color(0xFFA7F3D0),
        sparkSeed = 67,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF115E59),
                    0.4f to Color(0xFF14B8A6),
                    0.72f to Color(0xFF67E8F9),
                    1f to Color(0xFFF0FDFA),
                ),
                powered = listOf(
                    0f to Color(0xFF042F2E),
                    0.34f to Color(0xFF2DD4BF),
                    0.66f to Color(0xFF38BDF8),
                    1f to Color(0xFFEFF6FF),
                ),
            )
        },
    )

    val VIOLET = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_VIOLET,
        glowTail = Color(0xFF4C1D95),
        glowMid = Color(0xFF8B5CF6),
        glowHead = Color(0xFFE9D5FF),
        footInner = Color(0xFFA78BFA),
        footOuter = Color(0xFF34D399),
        sparkleSecondary = Color(0xFFC4B5FD),
        coreMid = Color(0xFFDDD6FE),
        sparkSeed = 71,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF5B21B6),
                    0.38f to Color(0xFF7C3AED),
                    0.7f to Color(0xFF6EE7B7),
                    1f to Color(0xFFF5F3FF),
                ),
                powered = listOf(
                    0f to Color(0xFF3B0764),
                    0.32f to Color(0xFF9333EA),
                    0.62f to Color(0xFF22D3EE),
                    1f to Color(0xFFFAE8FF),
                ),
            )
        },
    )

    val GINKGO = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_GINKGO,
        glowTail = Color(0xFF854D0E),
        glowMid = Color(0xFFFACC15),
        glowHead = Color(0xFFFEF9C3),
        footInner = Color(0xFFFDE047),
        footOuter = Color(0xFFF97316),
        sparkleSecondary = Color(0xFFFEF08A),
        coreMid = Color(0xFFFEF3C7),
        sparkSeed = 73,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFFA16207),
                    0.4f to Color(0xFFEAB308),
                    0.72f to Color(0xFFFBBF24),
                    1f to Color(0xFFFFFBEB),
                ),
                powered = listOf(
                    0f to Color(0xFF713F12),
                    0.34f to Color(0xFFF59E0B),
                    0.66f to Color(0xFFFB923C),
                    1f to Color(0xFFFFF7ED),
                ),
            )
        },
    )

    val MINT_BUBBLE = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_MINT_BUBBLE,
        glowTail = Color(0xFF065F46),
        glowMid = Color(0xFF6EE7B7),
        glowHead = Color(0xFFECFEFF),
        footInner = Color(0xFF5EEAD4),
        footOuter = Color(0xFFBAE6FD),
        sparkleSecondary = Color(0xFFA7F3D0),
        coreMid = Color(0xFFCCFBF1),
        sparkSeed = 79,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF047857),
                    0.38f to Color(0xFF34D399),
                    0.7f to Color(0xFF7DD3FC),
                    1f to Color(0xFFF0FDFA),
                ),
                powered = listOf(
                    0f to Color(0xFF064E3B),
                    0.32f to Color(0xFF2DD4BF),
                    0.62f to Color(0xFF38BDF8),
                    1f to Color(0xFFFFFFFF),
                ),
            )
        },
    )

    val NIGHT_INK = RibbonTrailPalette(
        trailId = PacMazeTrailId.RIBBON_NIGHT_INK,
        glowTail = Color(0xFF0F172A),
        glowMid = Color(0xFF312E81),
        glowHead = Color(0xFF94A3B8),
        footInner = Color(0xFF6366F1),
        footOuter = Color(0xFF1E293B),
        sparkleSecondary = Color(0xFF818CF8),
        coreMid = Color(0xFF475569),
        sparkSeed = 83,
        colorAt = { t, power ->
            steppedColor(
                t, power,
                normal = listOf(
                    0f to Color(0xFF020617),
                    0.42f to Color(0xFF1E1B4B),
                    0.74f to Color(0xFF334155),
                    1f to Color(0xFFCBD5E1),
                ),
                powered = listOf(
                    0f to Color(0xFF000000),
                    0.35f to Color(0xFF4338CA),
                    0.65f to Color(0xFF0EA5E9),
                    1f to Color(0xFFE2E8F0),
                ),
            )
        },
    )

    fun forTrail(trailId: PacMazeTrailId): RibbonTrailPalette? = when (trailId) {
        PacMazeTrailId.RIBBON_FLOW -> FLOW
        PacMazeTrailId.RIBBON_SAKURA -> SAKURA
        PacMazeTrailId.RIBBON_AURORA -> AURORA
        PacMazeTrailId.RIBBON_PHOENIX -> PHOENIX
        PacMazeTrailId.RIBBON_SOUL -> SOUL
        PacMazeTrailId.RIBBON_JADE -> JADE
        PacMazeTrailId.RIBBON_CINNABAR -> CINNABAR
        PacMazeTrailId.RIBBON_CELADON -> CELADON
        PacMazeTrailId.RIBBON_VIOLET -> VIOLET
        PacMazeTrailId.RIBBON_GINKGO -> GINKGO
        PacMazeTrailId.RIBBON_MINT_BUBBLE -> MINT_BUBBLE
        PacMazeTrailId.RIBBON_NIGHT_INK -> NIGHT_INK
        else -> null
    }
}
