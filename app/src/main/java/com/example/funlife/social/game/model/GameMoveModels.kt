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
    PAC_INPUT_FRAME("pac_input_frame"),
    /** 权威服模式：轻量直连输入（无 tick 绑定）。 */
    PAC_INPUT_DIRECT("pac_input_direct"),
    /** 权威服模式：房主广播的世界快照。 */
    PAC_STATE_SNAPSHOT("pac_state_snapshot"),
    PAC_ATTACK("pac_attack"),
    PAC_READY("pac_ready"),
    PAC_SURRENDER("pac_surrender"),
    PAC_COSMETIC("pac_cosmetic"),
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

data class PacInputFrameWire(
    val tick: Long,
    val gen: Long = 0L,
    val mode: String = "committed",
    val dir: String? = null,
    val attack: Boolean = false,
)

data class PacInputFramePayload(
    val kind: String = GameMoveKind.PAC_INPUT_FRAME.wire,
    @SerializedName("from_tick") val fromTick: Long = 0L,
    val frames: List<PacInputFrameWire> = emptyList(),
)

data class PacReadyPayload(
    val kind: String = GameMoveKind.PAC_READY.wire,
    val ready: Boolean = true,
)

data class PacSurrenderPayload(
    val kind: String = GameMoveKind.PAC_SURRENDER.wire,
)

data class PacCosmeticPayload(
    val kind: String = GameMoveKind.PAC_COSMETIC.wire,
    @SerializedName("skin_id") val skinId: String = "",
)
