package com.example.funlife.ui.screens.pacmaze.maptheme

import org.junit.Assert.assertTrue
import org.junit.Test

class GrassPatchAlphaTest {

    @Test
    fun grassPatchAlpha_staysWithinSrgbRange_forAllTileSeeds() {
        for (x in 0 until 32) {
            for (y in 0 until 32) {
                val seed = x * 17 + y * 31
                val alpha = grassPatchAlpha(seed)
                assertTrue("seed=$seed alpha=$alpha", alpha in 0f..1f)
            }
        }
    }
}
