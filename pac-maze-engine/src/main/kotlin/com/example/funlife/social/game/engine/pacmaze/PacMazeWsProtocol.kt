package com.example.funlife.social.game.engine.pacmaze

/**
 * Pac-Maze WebSocket 线协议（权威服 ↔ 客户端）。
 * 文档：docs/pac-maze-authoritative-architecture.md §4
 */
object PacMazeWsProtocol {
    const val KIND_JOINED = "joined"
    const val KIND_STATE = "state"
    const val KIND_INPUT = "input"
    const val KIND_ROOM_GO = "room_go"
    const val KIND_MATCH_END = "match_end"
    const val KIND_PING = "ping"
    const val KIND_PONG = "pong"
    const val KIND_ERROR = "error"
    const val KIND_READY = "ready"
    const val KIND_PEER_INPUT = "peer_input"

    fun buildPeerInput(entityId: String, payload: Map<String, Any?>): Map<String, Any?> =
        buildMap {
            put("t", KIND_PEER_INPUT)
            put("entityId", entityId)
            put("seq", payload["seq"] ?: 0L)
            put("attack", payload["attack"] == true)
            payload["tick"]?.let { put("tick", it) }
            if (payload["release"] == true) {
                put("release", true)
            } else {
                payload["dir"]?.toString()?.takeIf { it.isNotBlank() }?.let { put("dir", it) }
            }
        }

    fun buildInputFrame(
        entityId: String,
        tick: Long,
        dir: Direction?,
        attack: Boolean,
        seq: Long,
        release: Boolean = false,
    ): Map<String, Any?> = buildMap {
        put("t", KIND_INPUT)
        put("tick", tick)
        put("seq", seq)
        put("attack", attack)
        if (release) put("release", true)
        else if (dir != null) put("dir", dir.name)
    }

    fun buildInput(dir: Direction?, attack: Boolean, seq: Long): Map<String, Any?> =
        buildMap {
            put("t", KIND_INPUT)
            put("seq", seq)
            if (dir != null) put("dir", dir.name)
            put("attack", attack)
        }

    fun parseInput(payload: Map<String, Any?>): PacMazeTickInput {
        val dirWire = payload["dir"]?.toString()?.takeIf { it.isNotBlank() }
        return if (dirWire != null) {
            val dir = runCatching { Direction.valueOf(dirWire) }.getOrNull()
            if (dir != null) PacMazeTickInput.committed(0L, dir) else PacMazeTickInput.Inactive
        } else {
            PacMazeTickInput.Inactive
        }
    }

    fun buildStateMessage(
        world: PacMazeWorldState,
        inputs: Map<String, PacMazeTickInput>,
        config: PacMazeOnlineMatchConfig,
    ): Map<String, Any?> {
        val snap = PacMazeStateSnapshot.encode(world)
        val hostIn = inputs[config.hostEntityId]
        val guestIn = inputs[config.guestEntityId]
        return buildMap {
            putAll(snap)
            put("t", KIND_STATE)
            if (hostIn?.committed != null) put("input_host", hostIn.committed!!.name)
            else put("input_host_release", true)
            if (guestIn?.committed != null) put("input_guest", guestIn.committed!!.name)
            else put("input_guest_release", true)
        }
    }

    @Deprecated("Use buildStateMessage with inputs for online sync")
    fun buildStateMessage(world: PacMazeWorldState): Map<String, Any?> {
        val snap = PacMazeStateSnapshot.encode(world)
        return snap + ("t" to KIND_STATE)
    }

    fun buildJoined(
        entityId: String,
        isHost: Boolean,
        tick: Long,
        state: PacMazeWorldState?,
    ): Map<String, Any?> = buildMap {
        put("t", KIND_JOINED)
        put("entityId", entityId)
        put("isHost", isHost)
        put("tick", tick)
        state?.let { put("state", PacMazeStateSnapshot.encode(it)) }
    }

    fun buildRoomGo(startMs: Long): Map<String, Any?> = mapOf(
        "t" to KIND_ROOM_GO,
        "startMs" to startMs,
    )

    fun buildRoomState(
        peerCount: Int,
        readyCount: Int,
        expectedPeers: Int = 2,
    ): Map<String, Any?> = mapOf(
        "t" to "room_state",
        "peerCount" to peerCount,
        "readyCount" to readyCount,
        "expectedPeers" to expectedPeers,
    )

    fun buildMatchEnd(
        winnerEntityId: String?,
        scoreA: Int,
        scoreB: Int,
        reason: String,
    ): Map<String, Any?> = buildMap {
        put("t", KIND_MATCH_END)
        winnerEntityId?.let { put("winner", it) }
        put("score_a", scoreA)
        put("score_b", scoreB)
        put("reason", reason)
    }

    fun buildError(code: String, message: String): Map<String, Any?> = mapOf(
        "t" to KIND_ERROR,
        "code" to code,
        "message" to message,
    )
}
