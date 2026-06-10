package com.example.funlife.social.game.engine.pacmaze

object PacMazeSimulation {

    fun tick(
        state: PacMazeWorldState,
        pacInput: PacMazeInputState?,
        level: PacMazeLevelConfig,
        fireAttack: Boolean = false,
    ): PacMazeWorldState {
        val input = pacInput ?: PacMazeInputState.Inactive
        if (state.phase != PacMazePhase.PLAYING) return state

        var world = PacMazeMapDynamics.tick(advanceTimers(state))
        if (fireAttack) {
            world = PacMazeCombat.tryFireAttack(world)
        }
        val pac = world.entities.firstOrNull { it.role == "pac" } ?: return world

        val pacAfter = sanitizeEntity(
            world,
            PacMazePortals.applyTransit(world, tickPacEntity(world, pac, input), level),
            forGhost = false,
        )
        world = world.copy(
            entities = world.entities.map { entity ->
                if (entity.id != pac.id) entity else pacAfter
            },
        )
        world = PacMazeRules.eatPellet(world, pacAfter.x, pacAfter.y)

        if (world.ghostReleaseTicksLeft <= 0) {
            val updatedPac = world.entities.first { it.id == pac.id }
            val rng = PacMazeDeterministicRng(world.rngSeed + world.tick)
            world = world.copy(
                entities = world.entities.map { entity ->
                    if (entity.role != "ghost") entity
                    else sanitizeEntity(
                        world,
                        tickGhostEntity(world, entity, updatedPac, rng, level),
                        forGhost = true,
                    )
                },
            )
        }

        world = PacMazeCombat.tickProjectiles(world)
        world = PacMazeHazards.tick(world, level)
        world = resolveCollisions(world, level)
        return PacMazeRules.checkExitReached(world, level)
    }

    private fun sanitizeEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        forGhost: Boolean,
    ): PacMazeEntity {
        if (PacMazeMotion.isPositionLegal(state, entity.x, entity.y, forGhost)) return entity
        return PacMazeMotion.sanitize(state, entity, forGhost)
    }

    private fun advanceTimers(state: PacMazeWorldState): PacMazeWorldState {
        var ghostMode = state.ghostMode
        var ghostTicks = state.ghostModeTicksLeft - 1
        if (ghostTicks <= 0) {
            ghostMode = if (ghostMode == GhostMode.CHASE) GhostMode.SCATTER else GhostMode.CHASE
            ghostTicks = PacMazeConstants.GHOST_MODE_CYCLE_TICKS
        }
        val power = (state.powerTicksLeft - 1).coerceAtLeast(0)
        val release = (state.ghostReleaseTicksLeft - 1).coerceAtLeast(0)
        val attackCooldown = (state.attackCooldownTicksLeft - 1).coerceAtLeast(0)
        return state.copy(
            tick = state.tick + 1,
            powerTicksLeft = power,
            ghostMode = ghostMode,
            ghostModeTicksLeft = ghostTicks,
            ghostReleaseTicksLeft = release,
            attackCooldownTicksLeft = attackCooldown,
        )
    }

    private fun tickPacEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        input: PacMazeInputState,
    ): PacMazeEntity = PacMazeMotion.tickPlayer(state, entity, input)

    private fun tickGhostEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
    ): PacMazeEntity {
        var current = entity
        val mode = when {
            state.powerTicksLeft > 0 && current.ghostMode != GhostMode.EATEN -> GhostMode.FRIGHTENED
            current.ghostMode == GhostMode.EATEN -> GhostMode.EATEN
            else -> state.ghostMode
        }
        val speed = PacMazeConstants.ghostSpeedCellsPerSec(mode, level.ghostSpeedMul)
        val currentDir = current.direction
        val forwardBlocked = currentDir != null &&
            !PacMazeMotion.canMoveInDir(state, current.x, current.y, currentDir, forGhost = true)
        val atDecision = PacMazeMotion.isGhostDecisionPoint(state, current)

        if (currentDir == null || atDecision || forwardBlocked) {
            val picked = PacMazeGhostAi.pickDirection(
                state = state,
                ghost = current,
                pac = pac,
                rng = rng,
                level = level,
                escapeOnly = forwardBlocked && !atDecision,
            )
            val nextDir = picked ?: currentDir
            if (nextDir != null) {
                current = current.copy(
                    ghostMode = mode,
                    speed = speed,
                    direction = nextDir,
                    facing = nextDir,
                )
            }
        } else {
            current = current.copy(ghostMode = mode, speed = speed)
        }

        val dir = current.direction ?: return current
        return PacMazePortals.applyTransit(state, PacMazeMotion.tickGhost(state, current, dir, speed), level)
    }

    private fun resolveCollisions(state: PacMazeWorldState, level: PacMazeLevelConfig): PacMazeWorldState {
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        var world = state
        world.entities.filter { it.role == "ghost" }.forEach { ghost ->
            if (PacMazeMotion.tileX(ghost.x) == PacMazeMotion.tileX(pac.x) &&
                PacMazeMotion.tileY(ghost.y) == PacMazeMotion.tileY(pac.y)
            ) {
                world = when {
                    ghost.ghostMode == GhostMode.FRIGHTENED -> {
                        val ghosts = world.entities.map { e ->
                            if (e.id == ghost.id) e.copy(ghostMode = GhostMode.EATEN) else e
                        }
                        world.copy(
                            entities = ghosts,
                            score = world.score + PacMazeConstants.GHOST_SCORE,
                        )
                    }
                    ghost.ghostMode != GhostMode.EATEN -> {
                        val lives = world.lives - 1
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
                        val resetGhosts = world.entities.map { e ->
                            if (e.role == "ghost") {
                                val idx = e.id.removePrefix("ghost_").toIntOrNull() ?: 0
                                val spawn = level.ghostSpawns.getOrElse(idx) { level.pacSpawn }
                                e.copy(
                                    x = spawn.first.toFloat(),
                                    y = spawn.second.toFloat(),
                                    ghostMode = GhostMode.SCATTER,
                                    direction = null,
                                    velX = 0f,
                                    velY = 0f,
                                )
                            } else if (e.id == pac.id) resetPac else e
                        }
                        world.copy(
                            entities = resetGhosts,
                            lives = lives,
                            phase = phase,
                            ghostReleaseTicksLeft = PacMazeConstants.GHOST_RELEASE_TICKS / 2,
                        )
                    }
                    else -> world
                }
            }
        }
        return world
    }

    fun restartLevel(
        level: PacMazeLevelConfig,
        json: String,
        seed: Long,
        lives: Int,
        score: Int,
    ): PacMazeWorldState {
        val world = PacMazeMapLoader.buildInitialWorld(level, json, seed)
        return world.copy(lives = lives, score = score, phase = PacMazePhase.PLAYING)
    }
}
