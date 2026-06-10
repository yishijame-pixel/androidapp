package com.example.funlife.ui.screens.pacmaze

import com.example.funlife.ui.screens.pacmaze.PacMazeLevelCatalog.TOTAL_LEVELS

/** 每关 3 bit 星级（0–3），最多 13 关 × 3 = 39 bit。 */
object PacMazeStars {

    private const val BITS_PER_LEVEL = 3
    private const val LEVEL_MASK = 0x7

    fun decode(starsBitmask: Int, levelId: Int): Int {
        if (levelId !in 1..TOTAL_LEVELS) return 0
        val shift = (levelId - 1) * BITS_PER_LEVEL
        return ((starsBitmask shr shift) and LEVEL_MASK).coerceIn(0, 3)
    }

    fun merge(starsBitmask: Int, levelId: Int, newStars: Int): Int {
        if (levelId !in 1..TOTAL_LEVELS) return starsBitmask
        val shift = (levelId - 1) * BITS_PER_LEVEL
        val mask = LEVEL_MASK shl shift
        val prev = decode(starsBitmask, levelId)
        val merged = maxOf(prev, newStars.coerceIn(0, 3))
        return (starsBitmask and mask.inv()) or (merged shl shift)
    }

    fun totalStars(starsBitmask: Int, maxLevelReached: Int): Int {
        return (1..maxLevelReached.coerceIn(1, TOTAL_LEVELS))
            .sumOf { decode(starsBitmask, it) }
    }
}

fun decodePacMazeStars(starsBitmask: Int, levelId: Int): Int =
    PacMazeStars.decode(starsBitmask, levelId)
