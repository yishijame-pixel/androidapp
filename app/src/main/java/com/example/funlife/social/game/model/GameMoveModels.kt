package com.example.funlife.social.game.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class GameMoveDto(
    val id: String,
    @SerializedName("room_id") val roomId: String,
    @SerializedName("player_pb_id") val playerPbId: String,
    @SerializedName("move_index") val moveIndex: Int,
    val payload: JsonElement?,
    @SerializedName("created_at_ms") val createdAtMs: Long,
)

enum class GameMoveKind(val wire: String) {
    GOMOKU_PLACE("gomoku_place"),
    DRAW_STROKE("draw_stroke"),
    DRAW_CLEAR("draw_clear"),
    DRAW_GUESS("draw_guess"),
    DRAW_PHASE("draw_phase"),
    ;

    companion object {
        fun fromWire(wire: String?): GameMoveKind? =
            entries.firstOrNull { it.wire == wire }
    }
}

data class GomokuPlacePayload(
    val kind: String = GameMoveKind.GOMOKU_PLACE.wire,
    val x: Int,
    val y: Int,
)

data class DrawStrokePayload(
    val kind: String = GameMoveKind.DRAW_STROKE.wire,
    val seq: Int,
    val points: List<List<Float>>,
    val color: String = "#222222",
    val width: Float = 4f,
)

data class DrawClearPayload(
    val kind: String = GameMoveKind.DRAW_CLEAR.wire,
)

data class DrawGuessPayload(
    val kind: String = GameMoveKind.DRAW_GUESS.wire,
    val text: String,
)

data class DrawPhasePayload(
    val kind: String = GameMoveKind.DRAW_PHASE.wire,
    val phase: String,
)
