package com.example.funlife.social.game.engine.pacmaze

import com.example.funlife.social.game.model.GameMoveKind

/**
 * 权威服 → 客户端的状态快照（Phase1 房主广播）。
 */
object PacMazeStateSnapshot {

    val WIRE_KIND = GameMoveKind.PAC_STATE_SNAPSHOT.wire

    fun encode(world: PacMazeWorldState): Map<String, Any?> = buildMap {
        put("kind", WIRE_KIND)
        put("tick", world.tick)
        put("phase", world.phase.name)
        put("tiles", world.tiles.toList())
        put("w", world.width)
        put("h", world.height)
        put("entities", world.entities.map(::encodeEntity))
        put("score_a", world.playerScoreA)
        put("score_b", world.playerScoreB)
        put("lives_a", world.playerLivesA)
        put("lives_b", world.playerLivesB)
        put("pellets", world.pelletsRemaining)
        put("power", world.powerTicksLeft)
        put("ghost_release", world.ghostReleaseTicksLeft)
        put("ghost_mode", world.ghostMode.name)
        put("ghost_mode_ticks", world.ghostModeTicksLeft)
        put("attack_charges", world.attackCharges)
        put("attack_cd", world.attackCooldownTicksLeft)
        put("elapsed", world.onlineElapsedSeconds)
        put("zone_a", world.pelletZoneA.toList())
        put("zone_b", world.pelletZoneB.toList())
        put("zone_a_init", world.pelletZoneAInitial)
        put("zone_b_init", world.pelletZoneBInitial)
        put("winner", world.onlineWinnerEntityId)
        put("end_reason", world.onlineEndReason)
        if (world.projectiles.isNotEmpty()) {
            put("projectiles", world.projectiles.map { p ->
                mapOf(
                    "id" to p.id,
                    "x" to p.x,
                    "y" to p.y,
                    "dir" to p.direction.name,
                )
            })
        }
    }

    fun decode(payload: Map<String, Any?>, template: PacMazeWorldState): PacMazeWorldState? {
        val tick = payload["tick"]?.asLong() ?: return null
        val phaseWire = payload["phase"]?.toString() ?: return null
        val phase = runCatching { PacMazePhase.valueOf(phaseWire) }.getOrNull() ?: return null
        val tilesRaw = payload["tiles"] as? List<*> ?: return null
        val tiles = IntArray(tilesRaw.size) { i -> (tilesRaw[i] as? Number)?.toInt() ?: 0 }
        val entitiesRaw = payload["entities"] as? List<*> ?: return null
        val entities = entitiesRaw.mapNotNull { decodeEntity(it) }
        if (entities.isEmpty()) return null

        val projectiles = (payload["projectiles"] as? List<*>)?.mapNotNull { el ->
            val m = el as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val x = m["x"]?.asFloat() ?: return@mapNotNull null
            val y = m["y"]?.asFloat() ?: return@mapNotNull null
            val dir = m["dir"]?.toString()?.let { runCatching { Direction.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            PacMazeProjectile(id = id, x = x, y = y, direction = dir)
        }.orEmpty()

        return template.copy(
            tick = tick,
            phase = phase,
            tiles = tiles,
            width = payload["w"]?.asInt() ?: template.width,
            height = payload["h"]?.asInt() ?: template.height,
            entities = entities,
            playerScoreA = payload["score_a"]?.asInt() ?: 0,
            playerScoreB = payload["score_b"]?.asInt() ?: 0,
            playerLivesA = payload["lives_a"]?.asInt() ?: template.playerLivesA,
            playerLivesB = payload["lives_b"]?.asInt() ?: template.playerLivesB,
            pelletsRemaining = payload["pellets"]?.asInt() ?: template.pelletsRemaining,
            powerTicksLeft = payload["power"]?.asInt() ?: 0,
            ghostReleaseTicksLeft = payload["ghost_release"]?.asInt() ?: 0,
            ghostMode = payload["ghost_mode"]?.toString()?.let {
                runCatching { GhostMode.valueOf(it) }.getOrNull()
            } ?: template.ghostMode,
            ghostModeTicksLeft = payload["ghost_mode_ticks"]?.asInt() ?: template.ghostModeTicksLeft,
            attackCharges = payload["attack_charges"]?.asInt() ?: 0,
            attackCooldownTicksLeft = payload["attack_cd"]?.asInt() ?: 0,
            onlineElapsedSeconds = payload["elapsed"]?.asInt() ?: 0,
            pelletZoneA = (payload["zone_a"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet()
                ?: template.pelletZoneA,
            pelletZoneB = (payload["zone_b"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toSet()
                ?: template.pelletZoneB,
            pelletZoneAInitial = payload["zone_a_init"]?.asInt() ?: template.pelletZoneAInitial,
            pelletZoneBInitial = payload["zone_b_init"]?.asInt() ?: template.pelletZoneBInitial,
            onlineWinnerEntityId = payload["winner"]?.toString(),
            onlineEndReason = payload["end_reason"]?.toString(),
            projectiles = projectiles,
            score = (payload["score_a"]?.asInt() ?: 0) + (payload["score_b"]?.asInt() ?: 0),
        )
    }

    fun payloadFromMove(obj: Map<String, Any?>): Map<String, Any?>? {
        if (obj["kind"]?.toString() != WIRE_KIND) return null
        return obj
    }

    private fun encodeEntity(entity: PacMazeEntity): Map<String, Any?> = buildMap {
        put("id", entity.id)
        put("role", entity.role)
        put("x", entity.x)
        put("y", entity.y)
        entity.direction?.let { put("dir", it.name) }
        put("facing", entity.facing.name)
        put("vx", entity.velX)
        put("vy", entity.velY)
        put("gm", entity.ghostMode.name)
        put("gk", entity.ghostKind.name)
        if (entity.hitStunTicksLeft > 0) put("stun", entity.hitStunTicksLeft)
        if (entity.opportunistBurstTicksLeft > 0) put("burst", entity.opportunistBurstTicksLeft)
    }

    private fun decodeEntity(raw: Any?): PacMazeEntity? {
        val m = raw as? Map<*, *> ?: return null
        val id = m["id"]?.toString() ?: return null
        val role = m["role"]?.toString() ?: return null
        val x = m["x"]?.asFloat() ?: return null
        val y = m["y"]?.asFloat() ?: return null
        val dir = m["dir"]?.toString()?.let { runCatching { Direction.valueOf(it) }.getOrNull() }
        val facing = m["facing"]?.toString()?.let { runCatching { Direction.valueOf(it) }.getOrNull() }
            ?: dir ?: Direction.RIGHT
        val ghostMode = m["gm"]?.toString()?.let { runCatching { GhostMode.valueOf(it) }.getOrNull() }
            ?: GhostMode.CHASE
        val ghostKind = m["gk"]?.toString()?.let { runCatching { GhostKind.valueOf(it) }.getOrNull() }
            ?: GhostKind.STRIKER
        return PacMazeEntity(
            id = id,
            role = role,
            x = x,
            y = y,
            direction = dir,
            speed = PacMazeConstants.PAC_SPEED,
            ghostMode = ghostMode,
            facing = facing,
            velX = m["vx"]?.asFloat() ?: 0f,
            velY = m["vy"]?.asFloat() ?: 0f,
            hitStunTicksLeft = m["stun"]?.asInt() ?: 0,
            ghostKind = ghostKind,
            opportunistBurstTicksLeft = m["burst"]?.asInt() ?: 0,
        )
    }

    private fun Any.asLong(): Long? = when (this) {
        is Number -> toLong()
        is String -> toLongOrNull()
        else -> null
    }

    private fun Any.asInt(): Int? = when (this) {
        is Number -> toInt()
        is String -> toIntOrNull()
        else -> null
    }

    private fun Any.asFloat(): Float? = when (this) {
        is Number -> toFloat()
        is String -> toFloatOrNull()
        else -> null
    }
}
