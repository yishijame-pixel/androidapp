package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.model.GameMoveKind

/**
 * 在线权威服：直连四向输入，无转圈锁定 FSM。
 */
object PacMazeOnlineInput {

    val WIRE_KIND = GameMoveKind.PAC_INPUT_DIRECT.wire

    /** 在线对战略降低死区，方向切换更灵敏。 */
    private const val ONLINE_DEAD_ZONE = 0.05f

    fun sampleDirect(
        raw: PacMazeRawJoystickSample,
        tick: Long,
        generation: Long,
    ): PacMazeTickInput {
        if (!raw.fingerDown) {
            return PacMazeTickInput.Inactive.copy(tick = tick, generation = generation)
        }
        val strength = raw.strength.coerceIn(0f, 1f)
        if (strength < ONLINE_DEAD_ZONE) {
            return PacMazeTickInput.Inactive.copy(tick = tick, generation = generation)
        }
        val dir = joystickResolveSector(
            offsetX = raw.offsetX,
            offsetY = raw.offsetY,
            strength = strength,
            deadZone = ONLINE_DEAD_ZONE,
            previous = null,
        ) ?: return PacMazeTickInput.Inactive.copy(tick = tick, generation = generation)
        return PacMazeTickInput.committed(tick, dir, generation)
    }

    fun parseDirectPayload(payload: Map<String, Any?>): PacMazeTickInput? {
        val dirWire = payload["dir"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val dir = runCatching { Direction.valueOf(dirWire) }.getOrNull() ?: return null
        val attack = payload["attack"]?.toString()?.toBooleanStrictOrNull() == true
        if (attack) {
            // attack handled separately; still return direction input
        }
        return PacMazeTickInput.committed(tick = 0L, direction = dir, generation = 0L)
    }

    fun buildDirectPayload(dir: Direction?, attack: Boolean, seq: Long): Map<String, Any?> =
        buildMap {
            put("kind", WIRE_KIND)
            put("seq", seq)
            if (dir != null) put("dir", dir.name)
            put("attack", attack)
        }
}