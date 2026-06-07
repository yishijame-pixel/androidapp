package com.example.funlife.social.game.engine

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 五子棋 ELO 评分计算引擎（纯函数，可单测）
 *
 * ELO 公式：
 * - 期望得分：E = 1 / (1 + 10^((Rb - Ra) / 400))
 * - 新评分：Ra' = Ra + K * (S - E)
 *
 * K 因子根据段位和对局数调整：
 * - 新手（<30局）: K = 40
 * - 常规: K = 32
 * - 高段位（>2000）: K = 16
 */
object GomokuEloCalculator {

    /** 初始 ELO */
    const val INITIAL_ELO = 1200

    /** 最低 ELO（不会低于此值） */
    const val MIN_ELO = 100

    /** K 因子：新手 */
    private const val K_NEWBIE = 40

    /** K 因子：常规 */
    private const val K_NORMAL = 32

    /** K 因子：高段 */
    private const val K_HIGH = 16

    /** 高段 ELO 阈值 */
    private const val HIGH_ELO_THRESHOLD = 2000

    /** 新手对局数阈值 */
    private const val NEWBIE_GAMES_THRESHOLD = 30

    /**
     * 计算期望得分
     * @param myElo 我方 ELO
     * @param opponentElo 对方 ELO
     * @return 期望得分 (0.0 ~ 1.0)
     */
    fun expectedScore(myElo: Int, opponentElo: Int): Double {
        return 1.0 / (1.0 + 10.0.pow((opponentElo - myElo) / 400.0))
    }

    /**
     * 获取 K 因子
     * @param elo 当前 ELO
     * @param gamesPlayed 已对局数
     * @return K 因子
     */
    fun getKFactor(elo: Int, gamesPlayed: Int): Int {
        return when {
            gamesPlayed < NEWBIE_GAMES_THRESHOLD -> K_NEWBIE
            elo >= HIGH_ELO_THRESHOLD -> K_HIGH
            else -> K_NORMAL
        }
    }

    /**
     * 计算对局后的 ELO 变化
     * @param winnerElo 胜方 ELO
     * @param loserElo 负方 ELO
     * @param winnerGames 胜方已对局数
     * @param loserGames 负方已对局数
     * @param isDraw 是否和棋
     * @return (胜方新ELO, 负方新ELO, 胜方变化, 负方变化)
     */
    fun calculate(
        winnerElo: Int,
        loserElo: Int,
        winnerGames: Int,
        loserGames: Int,
        isDraw: Boolean = false,
    ): EloResult {
        val winnerK = getKFactor(winnerElo, winnerGames)
        val loserK = getKFactor(loserElo, loserGames)

        val winnerExpected = expectedScore(winnerElo, loserElo)
        val loserExpected = expectedScore(loserElo, winnerElo)

        val (winnerActual, loserActual) = if (isDraw) {
            0.5 to 0.5
        } else {
            1.0 to 0.0
        }

        val winnerDelta = (winnerK * (winnerActual - winnerExpected)).roundToInt()
        val loserDelta = (loserK * (loserActual - loserExpected)).roundToInt()

        val newWinnerElo = (winnerElo + winnerDelta).coerceAtLeast(MIN_ELO)
        val newLoserElo = (loserElo + loserDelta).coerceAtLeast(MIN_ELO)

        return EloResult(
            winnerNewElo = newWinnerElo,
            loserNewElo = newLoserElo,
            winnerDelta = newWinnerElo - winnerElo,
            loserDelta = newLoserElo - loserElo,
        )
    }

    /**
     * 根据 ELO 获取段位
     */
    fun getRank(elo: Int): GomokuRank {
        return GomokuRank.fromElo(elo)
    }

    /**
     * 获取到下一段位需要的 ELO
     */
    fun eloToNextRank(elo: Int): Int? {
        val currentRank = getRank(elo)
        val nextRank = GomokuRank.entries.getOrNull(currentRank.ordinal + 1) ?: return null
        return nextRank.minElo - elo
    }

    /**
     * 计算胜率预测
     * @param myElo 我方 ELO
     * @param opponentElo 对方 ELO
     * @return 胜率百分比 (0 ~ 100)
     */
    fun predictWinRate(myElo: Int, opponentElo: Int): Int {
        return (expectedScore(myElo, opponentElo) * 100).roundToInt()
    }
}

/**
 * ELO 计算结果
 */
data class EloResult(
    val winnerNewElo: Int,
    val loserNewElo: Int,
    val winnerDelta: Int,
    val loserDelta: Int,
) {
    val winnerDeltaDisplay: String
        get() = if (winnerDelta >= 0) "+$winnerDelta" else "$winnerDelta"

    val loserDeltaDisplay: String
        get() = if (loserDelta >= 0) "+$loserDelta" else "$loserDelta"
}

/**
 * 五子棋段位
 */
enum class GomokuRank(
    val displayName: String,
    val shortName: String,
    val minElo: Int,
    val iconEmoji: String,
    val colorHex: Long,
) {
    BRONZE("青铜", "铜", 0, "🥉", 0xFFCD7F32),
    SILVER("白银", "银", 1200, "🥈", 0xFFC0C0C0),
    GOLD("黄金", "金", 1400, "🥇", 0xFFFFD700),
    PLATINUM("铂金", "铂", 1600, "💎", 0xFFE5E4E2),
    DIAMOND("钻石", "钻", 1800, "💠", 0xFFB9F2FF),
    MASTER("大师", "师", 2000, "🏆", 0xFF9400D3),
    GRANDMASTER("宗师", "宗", 2200, "👑", 0xFFFF4500),
    LEGEND("传奇", "传", 2500, "🌟", 0xFFFFD700),
    ;

    companion object {
        fun fromElo(elo: Int): GomokuRank {
            return entries.lastOrNull { elo >= it.minElo } ?: BRONZE
        }
    }
}

/**
 * 玩家战绩统计
 */
data class GomokuPlayerStats(
    val pbId: String,
    val eloRating: Int = GomokuEloCalculator.INITIAL_ELO,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val gamesDrawn: Int = 0,
    val winStreak: Int = 0,
    val bestStreak: Int = 0,
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    val rank: GomokuRank get() = GomokuEloCalculator.getRank(eloRating)

    val winRate: Float
        get() = if (gamesPlayed > 0) gamesWon.toFloat() / gamesPlayed else 0f

    val winRatePercent: Int
        get() = (winRate * 100).roundToInt()

    /** 更新胜利后的统计 */
    fun onWin(newElo: Int): GomokuPlayerStats {
        val newStreak = winStreak + 1
        return copy(
            eloRating = newElo,
            gamesPlayed = gamesPlayed + 1,
            gamesWon = gamesWon + 1,
            winStreak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
            updatedAtMs = System.currentTimeMillis(),
        )
    }

    /** 更新失败后的统计 */
    fun onLoss(newElo: Int): GomokuPlayerStats = copy(
        eloRating = newElo,
        gamesPlayed = gamesPlayed + 1,
        gamesLost = gamesLost + 1,
        winStreak = 0,
        updatedAtMs = System.currentTimeMillis(),
    )

    /** 更新和棋后的统计 */
    fun onDraw(newElo: Int): GomokuPlayerStats = copy(
        eloRating = newElo,
        gamesPlayed = gamesPlayed + 1,
        gamesDrawn = gamesDrawn + 1,
        // 和棋不打断连胜
        updatedAtMs = System.currentTimeMillis(),
    )

    fun toMap(): Map<String, Any?> = buildMap {
        put("user", pbId)
        put("elo_rating", eloRating)
        put("games_played", gamesPlayed)
        put("games_won", gamesWon)
        put("games_lost", gamesLost)
        put("games_drawn", gamesDrawn)
        put("win_streak", winStreak)
        put("best_streak", bestStreak)
    }
}
