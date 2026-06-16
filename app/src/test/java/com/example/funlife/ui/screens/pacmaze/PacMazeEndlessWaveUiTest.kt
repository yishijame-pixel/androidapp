package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.social.game.engine.pacmaze.PacMazeLevelProgression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeEndlessWaveUiTest {

    @Test
    fun resolve_chunkForEarlyWaves() {
        val info = PacMazeEndlessWaveUi.resolve(3, maxLevelReached = 1)
        assertEquals(PacMazeEndlessSegment.CHUNK, info.segment)
        assertTrue(PacMazeEndlessWaveUi.hintText(info).contains("第 3 波"))
    }

    @Test
    fun resolve_preheatWhenMoltenLocked() {
        val info = PacMazeEndlessWaveUi.resolve(8, maxLevelReached = 10)
        assertEquals(PacMazeEndlessSegment.PREHEAT, info.segment)
        assertTrue(PacMazeEndlessWaveUi.hintText(info).contains("预热"))
    }

    @Test
    fun resolve_moltenWhenUnlocked() {
        val info = PacMazeEndlessWaveUi.resolve(8, maxLevelReached = PacMazeLevelProgression.TOTAL_LEVELS)
        assertEquals(PacMazeEndlessSegment.MOLTEN, info.segment)
        assertEquals(14, info.moltenLevelId)
        assertTrue(PacMazeEndlessWaveUi.badgeLabel(info).contains("🔥"))
        assertTrue(PacMazeEndlessWaveUi.bannerSubtitle(info).contains("L14"))
    }

    @Test
    fun resolve_moltenCyclesLevels() {
        val info = PacMazeEndlessWaveUi.resolve(17, maxLevelReached = PacMazeLevelProgression.TOTAL_LEVELS)
        assertEquals(23, info.moltenLevelId)
    }
}
