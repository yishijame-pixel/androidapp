package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameMoveKind
import com.google.gson.JsonParser

object PacMazeBoardSync {

    data class ParsedInput(
        val entityId: String,
        val tick: Long,
        val input: PacMazeTickInput,
        val attack: Boolean,
    )

    fun entityIdForPbId(config: PacMazeOnlineMatchConfig, pbId: String): String? = when (pbId) {
        config.hostPbId -> config.hostEntityId
        config.guestPbId -> config.guestEntityId
        else -> null
    }

    fun pbIdForEntityId(config: PacMazeOnlineMatchConfig, entityId: String): String? = when (entityId) {
        config.hostEntityId -> config.hostPbId
        config.guestEntityId -> config.guestPbId
        else -> null
    }

    fun parseInputMoves(
        moves: List<GameMoveDto>,
        config: PacMazeOnlineMatchConfig,
    ): List<ParsedInput> = moves.flatMap { move ->
        val obj = move.payload?.asJsonObject ?: return@flatMap emptyList()
        if (obj.get("kind")?.asString != GameMoveKind.PAC_INPUT_FRAME.wire) return@flatMap emptyList()
        val entityId = entityIdForPbId(config, move.playerPbId) ?: return@flatMap emptyList()
        val frames = obj.getAsJsonArray("frames") ?: return@flatMap emptyList()
        frames.mapNotNull { frameEl ->
            val frame = frameEl.asJsonObject
            val tick = frame.get("tick")?.asLong ?: return@mapNotNull null
            val gen = frame.get("gen")?.asLong ?: 0L
            val modeWire = frame.get("mode")?.asString ?: "idle"
            val dirWire = frame.get("dir")?.asString
            val attack = frame.get("attack")?.asBoolean ?: false
            val dir = dirWire?.let { wire ->
                runCatching { Direction.valueOf(wire) }.getOrNull()
            }
            val input = when (modeWire) {
                "committed" -> {
                    val committedDir = dir ?: return@mapNotNull null
                    PacMazeTickInput(
                        tick = tick,
                        generation = gen,
                        active = true,
                        mode = PacMazeInputMode.Committed,
                        strength = 1f,
                        sector = committedDir,
                        facing = committedDir,
                        committed = committedDir,
                    )
                }
                "pending" -> {
                    val pendingDir = dir ?: return@mapNotNull null
                    PacMazeTickInput.pending(tick, pendingDir, generation = gen)
                }
                "dead_zone" -> PacMazeTickInput.deadZone(tick, dir, gen)
                "spin" -> dir?.let { PacMazeTickInput.spin(tick, it, gen) }
                    ?: PacMazeTickInput.Inactive.copy(tick = tick, generation = gen)
                else -> PacMazeTickInput.Inactive.copy(tick = tick, generation = gen)
            }
            ParsedInput(entityId = entityId, tick = tick, input = input, attack = attack)
        }
    }.sortedBy { it.tick }

    fun buildInputPayload(fromTick: Long, frames: List<Map<String, Any?>>): Map<String, Any?> =
        mapOf(
            "kind" to GameMoveKind.PAC_INPUT_FRAME.wire,
            "from_tick" to fromTick,
            "frames" to frames,
        )

    fun inputFrameMap(
        tick: Long,
        input: PacMazeTickInput,
        attack: Boolean,
    ): Map<String, Any?> = buildMap {
        put("tick", tick)
        put("gen", input.generation)
        put("mode", when (input.mode) {
            PacMazeInputMode.DeadZone -> "dead_zone"
            PacMazeInputMode.Pending -> "pending"
            PacMazeInputMode.Spin -> "spin"
            PacMazeInputMode.Idle -> "idle"
            PacMazeInputMode.Committed -> "committed"
        })
        (input.committed ?: input.sector)?.let { put("dir", it.name) }
        put("attack", attack)
    }

    fun isPacInputMove(move: GameMoveDto): Boolean =
        move.payload?.asJsonObject?.get("kind")?.asString == GameMoveKind.PAC_INPUT_FRAME.wire

    fun isPacAuthoritativeMove(move: GameMoveDto): Boolean {
        val kind = move.payload?.asJsonObject?.get("kind")?.asString ?: return false
        return kind == GameMoveKind.PAC_INPUT_DIRECT.wire ||
            kind == GameMoveKind.PAC_STATE_SNAPSHOT.wire
    }

    fun hasSurrender(moves: List<GameMoveDto>, pbId: String): Boolean =
        moves.any { move ->
            move.playerPbId == pbId &&
                move.payload?.asJsonObject?.get("kind")?.asString == GameMoveKind.PAC_SURRENDER.wire
        }

    fun parseReadyMove(move: GameMoveDto): Boolean? {
        val obj = move.payload?.asJsonObject ?: return null
        if (obj.get("kind")?.asString != GameMoveKind.PAC_READY.wire) return null
        return obj.get("ready")?.asBoolean ?: true
    }
}
