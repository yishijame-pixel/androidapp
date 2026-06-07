package com.example.funlife.social.game.model

import com.google.gson.annotations.SerializedName

data class GomokuMove(
    val x: Int,
    val y: Int,
    val color: String,
)

data class GomokuPlayState(
    val board: String = "",
    @SerializedName("move_count") val moveCount: Int = 0,
    @SerializedName("last_move") val lastMove: GomokuMove? = null,
    @SerializedName("black_pb_id") val blackPbId: String = "",
    @SerializedName("white_pb_id") val whitePbId: String = "",
    /** 计时器状态（可选） */
    @SerializedName("timer") val timer: GomokuTimerState? = null,
    /** 是否启用禁手规则（默认启用） */
    @SerializedName("forbidden_enabled") val forbiddenEnabled: Boolean = true,
    /** 超时方（仅当超时判负时填写） */
    @SerializedName("timeout_loser") val timeoutLoser: String? = null,
    /** 游戏结束原因 */
    @SerializedName("end_reason") val endReason: String? = null,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("board", board)
        put("move_count", moveCount)
        lastMove?.let { put("last_move", mapOf("x" to it.x, "y" to it.y, "color" to it.color)) }
        put("black_pb_id", blackPbId)
        put("white_pb_id", whitePbId)
        timer?.let { put("timer", it.toMap()) }
        put("forbidden_enabled", forbiddenEnabled)
        timeoutLoser?.let { put("timeout_loser", it) }
        endReason?.let { put("end_reason", it) }
    }
}

/** 游戏结束原因 */
object GomokuEndReason {
    const val FIVE_IN_ROW = "five_in_row"       // 五连胜
    const val TIMEOUT = "timeout"               // 超时判负
    const val FORBIDDEN = "forbidden"           // 禁手判负
    const val RESIGN = "resign"                 // 认输
    const val DRAW_AGREEMENT = "draw_agreement" // 协议和棋
    const val DRAW_FULL = "draw_full"           // 棋盘满和
}

enum class DrawGuessPhase(val wire: String) {
    DRAWING("drawing"),
    GUESSING("guessing"),
    ROUND_END("round_end"),
    FINISHED("finished"),
    ;

    companion object {
        fun fromWire(wire: String?): DrawGuessPhase =
            entries.firstOrNull { it.wire == wire } ?: DRAWING
    }
}

data class DrawGuessGuess(
    @SerializedName("pb_id") val pbId: String,
    val text: String,
    val correct: Boolean = false,
)

data class DrawGuessPlayState(
    val round: Int = 1,
    val phase: String = DrawGuessPhase.DRAWING.wire,
    @SerializedName("drawer_pb_id") val drawerPbId: String = "",
    val word: String = "",
    val guesses: List<DrawGuessGuess> = emptyList(),
    val scores: Map<String, Int> = emptyMap(),
    @SerializedName("stroke_seq") val strokeSeq: Int = 0,
    @SerializedName("max_rounds") val maxRounds: Int = 3,
    @SerializedName("guess_limit") val guessLimit: Int = 5,
    @SerializedName("draw_seconds") val drawSeconds: Int = 60,
    @SerializedName("guess_seconds") val guessSeconds: Int = 90,
    @SerializedName("phase_started_at_ms") val phaseStartedAtMs: Long = 0L,
    @SerializedName("used_words") val usedWords: List<String> = emptyList(),
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("round", round)
        put("phase", phase)
        put("drawer_pb_id", drawerPbId)
        put("word", word)
        put("guesses", guesses.map { mapOf("pb_id" to it.pbId, "text" to it.text, "correct" to it.correct) })
        put("scores", scores)
        put("stroke_seq", strokeSeq)
        put("max_rounds", maxRounds)
        put("guess_limit", guessLimit)
        put("draw_seconds", drawSeconds)
        put("guess_seconds", guessSeconds)
        put("phase_started_at_ms", phaseStartedAtMs)
        if (usedWords.isNotEmpty()) put("used_words", usedWords)
    }

    /** 画家作画中可见词语；猜词方在 drawing/guessing 阶段不可见（防 API 泄漏兜底）。 */
    fun visibleWord(myPbId: String): String {
        if (myPbId.isBlank() || drawerPbId != myPbId) {
            val phaseEnum = DrawGuessPhase.fromWire(phase)
            if (phaseEnum == DrawGuessPhase.DRAWING || phaseEnum == DrawGuessPhase.GUESSING) return ""
        }
        return word
    }
}
