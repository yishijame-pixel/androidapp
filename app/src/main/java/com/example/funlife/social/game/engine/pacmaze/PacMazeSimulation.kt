package com.example.funlife.social.game.engine.pacmaze

object PacMazeSimulation {

    fun tick(
        state: PacMazeWorldState,
        pacInput: PacMazeTickInput?,
        level: PacMazeLevelConfig,
        fireAttack: Boolean = false,
        cosmeticSpeedMultiplier: Float = 1f,
        attackCooldownTicks: Int = PacMazeConstants.ATTACK_COOLDOWN_TICKS,
        playerPassRadius: Float = PacMazeMotion.BODY_RADIUS,
    ): PacMazeWorldState {
        val input = pacInput ?: PacMazeTickInput.Inactive
        if (state.phase != PacMazePhase.PLAYING) return state

        var world = PacMazeItems.tickEffects(PacMazeMapDynamics.tick(advanceTimers(state)))
        world = PacMazeItems.tick(world)
        if (fireAttack) {
            world = PacMazeCombat.tryFireAttack(world, attackCooldownTicks)
        }
        val pac = world.entities.firstOrNull { it.role == "pac" } ?: return world

        val movedPac = tickPacEntity(world, pac, input, cosmeticSpeedMultiplier, playerPassRadius)
        world = PacMazePortals.tryArmLinkPair(world, movedPac, level)
        val pacAfter = sanitizeEntity(
            world,
            PacMazePortals.applyTransit(
                world,
                movedPac,
                level,
            ),
            forGhost = false,
            bodyRadius = PacMazeMotion.BODY_RADIUS,
        )
        world = world.copy(
            entities = world.entities.map { entity ->
                if (entity.id != pac.id) entity else pacAfter
            },
        )
        val pelletsBefore = world.pelletsRemaining
        world = PacMazeRules.eatPellet(world, pacAfter.x, pacAfter.y, level.modeRules.winCondition, level)
        if (level.modeRules.scoreMultiplier != 1f && world.score > state.score) {
            val gained = world.score - state.score
            val bonus = (gained * (level.modeRules.scoreMultiplier - 1f)).toInt()
            if (bonus > 0) world = world.copy(score = world.score + bonus)
        }
        if (world.pelletsRemaining < pelletsBefore) {
            world = triggerOpportunistBurst(world)
        }
        world = PacMazeItems.tryPickup(world, pacAfter.x, pacAfter.y)
        world = PacMazeCheckpointVisits.apply(world, level)
        world = PacMazeItems.tickMagnet(world)
        world = PacMazeMazeExploration.tick(world, level)
        world = PacMazeMazeMechanics.tick(world, level, elapsedSeconds = (world.tick / PacMazeConstants.TICKS_PER_SECOND).toInt())

        if (world.ghostReleaseTicksLeft <= 0 && !PacMazeItems.ghostsFrozen(world)) {
            val updatedPac = world.entities.first { it.id == pac.id }
            val rng = PacMazeDeterministicRng(world.rngSeed + world.tick)
            world = world.copy(
                entities = world.entities.map { entity ->
                    if (entity.role != "ghost") entity
                    else if (entity.hitStunTicksLeft > 0) {
                        entity.copy(velX = 0f, velY = 0f)
                    } else sanitizeEntity(
                        world,
                        tickGhostEntity(world, entity, updatedPac, rng, level),
                        forGhost = true,
                    )
                },
            )
        } else if (PacMazeItems.ghostsFrozen(world)) {
            world = world.copy(
                entities = world.entities.map { entity ->
                    if (entity.role != "ghost" || entity.ghostMode == GhostMode.EATEN) entity
                    else entity.copy(velX = 0f, velY = 0f)
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
        bodyRadius: Float = PacMazeMotion.BODY_RADIUS,
    ): PacMazeEntity {
        val ghost = if (forGhost) entity else null
        val radius = if (forGhost) PacMazeMotion.BODY_RADIUS else bodyRadius
        if (PacMazeMotion.isPositionLegal(state, entity.x, entity.y, forGhost, ghost, radius)) return entity
        return PacMazeMotion.sanitize(state, entity, forGhost, radius)
    }

    private fun tickPacEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        input: PacMazeTickInput,
        cosmeticSpeedMultiplier: Float,
        playerPassRadius: Float,
    ): PacMazeEntity = PacMazeMotion.tickPlayer(
        state = state,
        entity = entity,
        input = input,
        movementMode = state.movementMode,
        cosmeticSpeedMultiplier = cosmeticSpeedMultiplier,
        passRadius = playerPassRadius,
    )

    private fun triggerOpportunistBurst(state: PacMazeWorldState): PacMazeWorldState =
        state.copy(
            entities = state.entities.map { entity ->
                if (entity.role == "ghost" &&
                    entity.ghostKind.behaviorArchetype == GhostBehaviorArchetype.OPPORTUNIST
                ) {
                    entity.copy(opportunistBurstTicksLeft = PacMazeGhostRoster.OPPORTUNIST_BURST_TICKS)
                } else {
                    entity
                }
            },
        )

    private fun advanceTimers(state: PacMazeWorldState): PacMazeWorldState {
        var ghostMode = state.ghostMode
        var ghostTicks = state.ghostModeTicksLeft - 1
        if (ghostTicks <= 0) {
            ghostMode = if (state.levelId <= 0) {
                // 迷雾迷宫：程序地图无固定角落，保持追击避免散场卡死
                GhostMode.CHASE
            } else if (ghostMode == GhostMode.CHASE) {
                GhostMode.SCATTER
            } else {
                GhostMode.CHASE
            }
            ghostTicks = PacMazeConstants.GHOST_MODE_CYCLE_TICKS
        }
        val power = (state.powerTicksLeft - 1).coerceAtLeast(0)
        val release = (state.ghostReleaseTicksLeft - 1).coerceAtLeast(0)
        val attackCooldown = (state.attackCooldownTicksLeft - 1).coerceAtLeast(0)
        val entities = state.entities.map { entity ->
            when {
                entity.role != "ghost" -> entity
                entity.hitStunTicksLeft > 0 -> entity.copy(
                    hitStunTicksLeft = entity.hitStunTicksLeft - 1,
                    velX = 0f,
                    velY = 0f,
                )
                else -> entity.copy(
                    opportunistBurstTicksLeft = (entity.opportunistBurstTicksLeft - 1).coerceAtLeast(0),
                    phaseWalkCooldownTicksLeft = (entity.phaseWalkCooldownTicksLeft - 1).coerceAtLeast(0),
                )
            }
        }
        return state.copy(
            tick = state.tick + 1,
            powerTicksLeft = power,
            ghostMode = ghostMode,
            ghostModeTicksLeft = ghostTicks,
            ghostReleaseTicksLeft = release,
            attackCooldownTicksLeft = attackCooldown,
            entities = entities,
        )
    }

    private fun tickGhostEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        pac: PacMazeEntity,
        rng: PacMazeDeterministicRng,
        level: PacMazeLevelConfig,
    ): PacMazeEntity = PacMazeGhostController.tick(state, entity, pac, rng, level)

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
                        PacMazeItems.tryConsumeShield(world)?.let { shielded ->
                            world = shielded
                            return@forEach
                        }
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
                                val spawn = level.ghostSpawns.getOrElse(idx) {
                                    PacMazeGhostSpawnDef(
                                        level.pacSpawn.first,
                                        level.pacSpawn.second,
                                        GhostKind.STRIKER,
                                    )
                                }
                                e.copy(
                                    x = spawn.x.toFloat(),
                                    y = spawn.y.toFloat(),
                                    ghostMode = GhostMode.SCATTER,
                                    direction = null,
                                    velX = 0f,
                                    velY = 0f,
                                    opportunistBurstTicksLeft = 0,
                                    ghostStuckTicks = 0,
                                    ghostDecisionTileKey = -1,
                                    phaseWalkCooldownTicksLeft = 0,
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
