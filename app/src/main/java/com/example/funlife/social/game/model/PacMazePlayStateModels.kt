package com.example.funlife.social.game.model

import com.google.gson.annotations.SerializedName

data class PacMazePlayerMeta(
    @SerializedName("pb_id") val pbId: String = "",
    @SerializedName("entity_id") val entityId: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("skin_id") val skinId: String = "",
    @SerializedName("ready") val ready: Boolean = false,
    @SerializedName("score") val score: Int = 0,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("pb_id", pbId)
        put("entity_id", entityId)
        if (displayName.isNotBlank()) put("display_name", displayName)
        if (skinId.isNotBlank()) put("skin_id", skinId)
        put("ready", ready)
        put("score", score)
    }
}

data class PacMazeMatchResultWire(
    @SerializedName("winner_pb_id") val winnerPbId: String? = null,
    @SerializedName("end_reason") val endReason: String = "normal",
    @SerializedName("duration_sec") val durationSec: Int = 0,
    @SerializedName("scores") val scores: Map<String, Int> = emptyMap(),
    @SerializedName("elo_delta") val eloDelta: Map<String, Int> = emptyMap(),
    @SerializedName("team_stars") val teamStars: Int = 0,
    @SerializedName("draw") val draw: Boolean = false,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        winnerPbId?.let { put("winner_pb_id", it) }
        put("end_reason", endReason)
        put("duration_sec", durationSec)
        if (scores.isNotEmpty()) put("scores", scores)
        if (eloDelta.isNotEmpty()) put("elo_delta", eloDelta)
        if (teamStars > 0) put("team_stars", teamStars)
        put("draw", draw)
    }
}

data class PacMazePlayState(
    @SerializedName("protocol_version") val protocolVersion: Int = 1,
    @SerializedName("match_mode") val matchMode: String = "versus_duel",
    @SerializedName("versus_rule") val versusRule: String = "race_pellets",
    @SerializedName("match_seed") val matchSeed: Long = 0L,
    @SerializedName("level_id") val levelId: Int = 1,
    @SerializedName("arena_id") val arenaId: String = "arena_001",
    @SerializedName("time_limit_sec") val timeLimitSec: Int = 150,
    @SerializedName("phase") val phase: String = "lobby",
    @SerializedName("sim_tick") val simTick: Long = 0L,
    @SerializedName("started_at_ms") val startedAtMs: Long = 0L,
    @SerializedName("host_pb_id") val hostPbId: String = "",
    @SerializedName("guest_pb_id") val guestPbId: String = "",
    @SerializedName("player_a") val playerA: PacMazePlayerMeta = PacMazePlayerMeta(),
    @SerializedName("player_b") val playerB: PacMazePlayerMeta = PacMazePlayerMeta(),
    @SerializedName("result") val result: PacMazeMatchResultWire? = null,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("protocol_version", protocolVersion)
        put("match_mode", matchMode)
        put("versus_rule", versusRule)
        put("match_seed", matchSeed)
        put("level_id", levelId)
        put("arena_id", arenaId)
        put("time_limit_sec", timeLimitSec)
        put("phase", phase)
        put("sim_tick", simTick)
        put("started_at_ms", startedAtMs)
        put("host_pb_id", hostPbId)
        put("guest_pb_id", guestPbId)
        put("player_a", playerA.toMap())
        put("player_b", playerB.toMap())
        result?.let { put("result", it.toMap()) }
    }

    fun bothReady(): Boolean = playerA.ready && playerB.ready
}
