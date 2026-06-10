package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.ui.screens.pacmaze.PacMazeStars
import org.junit.Assert.assertEquals
import org.junit.Test

class PacMazeStarsTest {

    @Test
    fun merge_keepsMaxPerLevel() {
        var bits = PacMazeStars.merge(0, 1, 1)
        bits = PacMazeStars.merge(bits, 1, 3)
        assertEquals(3, PacMazeStars.decode(bits, 1))
    }

    @Test
    fun levelsDoNotOverlap() {
        var bits = PacMazeStars.merge(0, 1, 3)
        bits = PacMazeStars.merge(bits, 2, 2)
        assertEquals(3, PacMazeStars.decode(bits, 1))
        assertEquals(2, PacMazeStars.decode(bits, 2))
    }
}
