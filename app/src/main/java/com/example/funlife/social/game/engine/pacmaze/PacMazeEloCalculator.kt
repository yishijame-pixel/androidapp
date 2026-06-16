package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.engine.GomokuEloCalculator
import kotlin.math.roundToInt

object PacMazeEloCalculator {

    fun calculateVersusDelta(
        winnerElo: Int,
        loserElo: Int,
        winnerGames: Int,
        loserGames: Int,
        isDraw: Boolean,
    ): Pair<Int, Int> {
        val result = GomokuEloCalculator.calculate(
            winnerElo = winnerElo,
            loserElo = loserElo,
            winnerGames = winnerGames,
            loserGames = loserGames,
            isDraw = isDraw,
        )
        return result.winnerDelta to result.loserDelta
    }

    fun applyDelta(elo: Int, delta: Int): Int =
        (elo + delta).coerceAtLeast(GomokuEloCalculator.MIN_ELO)
}
