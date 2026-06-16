package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

object PacMazeHazards {

    fun initStates(defs: List<PacMazeHazardDef>): List<PacMazeHazardState> =
        defs.map { def ->
            val start = when (def.kind) {
                PacMazeHazardKind.LASER_ROW -> def.rangeStart.toFloat() + 0.5f
                PacMazeHazardKind.LASER_COL -> def.rangeStart.toFloat() + 0.5f
                PacMazeHazardKind.TURRET -> 0f
            }
            val cooldown = when (def.kind) {
                PacMazeHazardKind.TURRET -> 20
                else -> 0
            }
            PacMazeHazardState(
                id = def.id,
                scanPos = start,
                fireCooldown = cooldown,
            )
        }

    fun tick(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        if (state.phase != PacMazePhase.PLAYING || state.hazards.isEmpty()) return state
        var world = updateLaserStates(state)
        world = fireTurrets(world)
        world = tickEnemyBullets(world)
        return resolvePacHits(world, level)
    }

    private fun updateLaserStates(state: PacMazeWorldState): PacMazeWorldState {
        val cycle = PacMazeConstants.LASER_WARN_TICKS + PacMazeConstants.LASER_LETHAL_TICKS
        val phase = (state.tick % cycle).toInt()
        val lethal = phase >= PacMazeConstants.LASER_WARN_TICKS
        val step = PacMazeConstants.LASER_SCAN_SPEED_CELLS_PER_SEC / PacMazeConstants.TICKS_PER_SECOND

        val updated = state.hazardStates.map { runtime ->
            val def = state.hazards.firstOrNull { it.id == runtime.id } ?: return@map runtime
            when (def.kind) {
                PacMazeHazardKind.TURRET -> runtime.copy(
                    fireCooldown = (runtime.fireCooldown - 1).coerceAtLeast(0),
                    lethal = false,
                )
                PacMazeHazardKind.LASER_ROW, PacMazeHazardKind.LASER_COL -> {
                    var pos = runtime.scanPos + runtime.scanDir * step
                    var dir = runtime.scanDir
                    val min = def.rangeStart + 0.5f
                    val max = def.rangeEnd + 0.5f
                    if (pos >= max) {
                        pos = max
                        dir = -1
                    } else if (pos <= min) {
                        pos = min
                        dir = 1
                    }
                    runtime.copy(scanPos = pos, scanDir = dir, lethal = lethal)
                }
            }
        }
        return state.copy(hazardStates = updated)
    }

    private fun fireTurrets(state: PacMazeWorldState): PacMazeWorldState {
        var bullets = state.enemyBullets
        val states = state.hazardStates.map { runtime ->
            val def = state.hazards.firstOrNull { it.id == runtime.id } ?: return@map runtime
            if (def.kind != PacMazeHazardKind.TURRET) return@map runtime
            if (runtime.fireCooldown > 0) return@map runtime
            val bullet = PacMazeEnemyBullet(
                id = "eb_${state.tick}_${def.id}",
                x = def.x.toFloat(),
                y = def.y.toFloat(),
                direction = def.direction,
                hazardId = def.id,
            )
            bullets = bullets + bullet
            runtime.copy(fireCooldown = PacMazeConstants.TURRET_FIRE_INTERVAL_TICKS)
        }
        return state.copy(hazardStates = states, enemyBullets = bullets)
    }

    private fun tickEnemyBullets(state: PacMazeWorldState): PacMazeWorldState {
        if (state.enemyBullets.isEmpty()) return state
        val step = PacMazeConstants.ENEMY_BULLET_SPEED_CELLS_PER_SEC / PacMazeConstants.TICKS_PER_SECOND
        val remaining = mutableListOf<PacMazeEnemyBullet>()
        state.enemyBullets.forEach { bullet ->
            val (dx, dy) = bullet.direction.delta()
            val nx = bullet.x + dx * step
            val ny = bullet.y + dy * step
            val tx = nx.toInt()
            val ty = ny.toInt()
            if (tx !in 0 until state.width || ty !in 0 until state.height) return@forEach
            if (!PacMazeRules.isWalkable(state, tx, ty, forGhost = false)) return@forEach
            remaining.add(bullet.copy(x = nx, y = ny))
        }
        return state.copy(enemyBullets = remaining)
    }

    private fun resolvePacHits(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val pacCx = pac.x + 0.5f
        val pacCy = pac.y + 0.5f

        val laserHit = state.hazardStates.any { runtime ->
            if (!runtime.lethal) return@any false
            val def = state.hazards.firstOrNull { it.id == runtime.id } ?: return@any false
            when (def.kind) {
                PacMazeHazardKind.LASER_ROW -> {
                    val rowY = def.y + 0.5f
                    abs(pacCy - rowY) < 0.42f &&
                        abs(pacCx - runtime.scanPos) < 0.38f &&
                        runtime.scanPos in (def.rangeStart + 0.5f)..(def.rangeEnd + 0.5f)
                }
                PacMazeHazardKind.LASER_COL -> {
                    val colX = def.x + 0.5f
                    abs(pacCx - colX) < 0.42f &&
                        abs(pacCy - runtime.scanPos) < 0.38f &&
                        runtime.scanPos in (def.rangeStart + 0.5f)..(def.rangeEnd + 0.5f)
                }
                PacMazeHazardKind.TURRET -> false
            }
        }

        val bulletHit = state.enemyBullets.any { bullet ->
            abs(pac.x - bullet.x) < 0.45f && abs(pac.y - bullet.y) < 0.45f
        }

        if (!laserHit && !bulletHit) return state
        return PacMazeItems.applyFatalDamage(state, level)
    }

    fun applyPacDamage(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val lives = state.lives - 1
        val phase = if (lives <= 0) PacMazePhase.GAME_OVER else PacMazePhase.PLAYING
        val resetPac = pac.copy(
            x = level.pacSpawn.first.toFloat(),
            y = level.pacSpawn.second.toFloat(),
            direction = null,
            inputActive = false,
            velX = 0f,
            velY = 0f,
            nextDirection = null,
        )
        val resetGhosts = state.entities.map { entity ->
            if (entity.role == "ghost") {
                val idx = entity.id.removePrefix("ghost_").toIntOrNull() ?: 0
                val spawn = level.ghostSpawns.getOrElse(idx) {
                    PacMazeGhostSpawnDef(
                        level.pacSpawn.first,
                        level.pacSpawn.second,
                        GhostKind.STRIKER,
                    )
                }
                entity.copy(
                    x = spawn.x.toFloat(),
                    y = spawn.y.toFloat(),
                    ghostMode = GhostMode.SCATTER,
                    direction = null,
                    velX = 0f,
                    velY = 0f,
                    opportunistBurstTicksLeft = 0,
                    phaseWalkCooldownTicksLeft = 0,
                    ghostStuckTicks = 0,
                    ghostDecisionTileKey = -1,
                )
            } else if (entity.id == pac.id) {
                resetPac
            } else {
                entity
            }
        }
        return state.copy(
            entities = resetGhosts,
            lives = lives,
            phase = phase,
            ghostReleaseTicksLeft = PacMazeConstants.GHOST_RELEASE_TICKS / 2,
            enemyBullets = emptyList(),
        )
    }
}
