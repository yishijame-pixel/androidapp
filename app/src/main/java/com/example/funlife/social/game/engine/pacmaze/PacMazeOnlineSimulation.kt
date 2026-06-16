package com.example.funlife.social.game.engine.pacmaze

/**
 * 在线双人对局专用 tick：支持 pac_a / pac_b，不破坏单机 [PacMazeSimulation]。
 */
object PacMazeOnlineSimulation {

    fun tick(
        state: PacMazeWorldState,
        inputs: Map<String, PacMazeTickInput>,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
        attacks: Set<String> = emptySet(),
    ): PacMazeWorldState {
        if (state.phase != PacMazePhase.PLAYING) return state
        var world = PacMazeItems.tickEffects(PacMazeMapDynamics.tick(advanceTimers(state, config)))
        world = PacMazeItems.tick(world)

        val hostId = config.hostEntityId
        val guestId = config.guestEntityId
        val hostInput = inputs[hostId] ?: PacMazeTickInput.Inactive
        val guestInput = inputs[guestId] ?: PacMazeTickInput.Inactive

        world = tickPlayer(world, hostId, hostInput, attacks.contains(hostId), level, config)
        world = tickPlayer(world, guestId, guestInput, attacks.contains(guestId), level, config)

        if (world.ghostReleaseTicksLeft <= 0 && !PacMazeItems.ghostsFrozen(world)) {
            val pacs = world.playerPacs()
            val lead = leadingPac(world, pacs, config)
            world = world.copy(
                entities = world.entities.map { entity ->
                    if (entity.role != "ghost") entity
                    else if (entity.hitStunTicksLeft > 0) entity.copy(velX = 0f, velY = 0f)
                    else {
                        val rng = PacMazeDeterministicRng(world.rngSeed + world.tick)
                        sanitizeGhost(
                            world,
                            tickGhostEntity(world, entity, lead, rng, level),
                        )
                    }
                },
            )
        }

        world = PacMazeCombat.tickProjectiles(world)
        world = PacMazeHazards.tick(world, level)
        world = resolveOnlineCollisions(world, level, config)
        world = PacMazeRules.checkExitReached(world, level)
        world = checkOnlineWin(world, level, config)
        world = world.copy(onlineElapsedSeconds = (world.tick / PacMazeConstants.TICKS_PER_SECOND).toInt())
        return world
    }

    private fun tickPlayer(
        state: PacMazeWorldState,
        entityId: String,
        input: PacMazeTickInput,
        fireAttack: Boolean,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        var world = state
        val pac = world.pacById(entityId) ?: return world
        if (fireAttack) {
            world = fireAttackFor(world, pac)
        }
        val moved = sanitizePac(world, tickPacEntity(world, pac, input))
        world = PacMazePortals.tryArmLinkPair(world, moved, level)
        val pacAfter = sanitizePac(
            world,
            PacMazePortals.applyTransit(world, moved, level),
        )
        world = world.copy(
            entities = world.entities.map { if (it.id != pac.id) it else pacAfter },
        )
        val pelletsBefore = world.pelletsRemaining
        world = eatPelletForPlayer(world, pacAfter, level, config)
        if (world.pelletsRemaining < pelletsBefore) {
            world = triggerOpportunistBurst(world)
        }
        world = PacMazeItems.tryPickup(world, pacAfter.x, pacAfter.y)
        world = PacMazeCheckpointVisits.applyForEntity(world, level, pacAfter)
        world = PacMazeMazeExploration.tickForEntity(world, level, pacAfter)
        return world
    }

    private fun eatPelletForPlayer(
        state: PacMazeWorldState,
        pac: PacMazeEntity,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        val tx = PacMazeMotion.tileX(pac.x)
        val ty = PacMazeMotion.tileY(pac.y)
        if (tx !in 0 until state.width || ty !in 0 until state.height) return state
        val idx = ty * state.width + tx
        val tile = state.tiles[idx]
        if (tile != TileType.PELLET.code && tile != TileType.POWER.code) return state
        val newTiles = state.tiles.copyOf()
        newTiles[idx] = TileType.EMPTY.code
        val addScore = if (tile == TileType.POWER.code) PacMazeConstants.POWER_SCORE else PacMazeItems.pelletScore(state)
        val pelletsLeft = (state.pelletsRemaining - 1).coerceAtLeast(0)
        var world = state.copy(
            tiles = newTiles,
            pelletsRemaining = pelletsLeft,
            score = state.score + addScore,
        )
        if (tile == TileType.POWER.code) {
            world = world.copy(
                powerTicksLeft = PacMazeConstants.POWER_DURATION_TICKS,
                attackCharges = world.attackCharges + 1,
            )
        }
        world = applyPlayerScore(world, pac.id, addScore, config)
        if (config.mode == PacMazeOnlineMatchMode.COOP_CAMPAIGN &&
            pelletsLeft == 0 &&
            level.modeRules.winCondition == PacMazeWinCondition.CLEAR_PELLETS
        ) {
            world = world.copy(phase = PacMazePhase.LEVEL_CLEAR)
        }
        var zoneA = world.pelletZoneA
        var zoneB = world.pelletZoneB
        if (idx in zoneA) zoneA = zoneA - idx
        if (idx in zoneB) zoneB = zoneB - idx
        world = world.copy(pelletZoneA = zoneA, pelletZoneB = zoneB)
        return world
    }

    private fun applyPlayerScore(
        state: PacMazeWorldState,
        entityId: String,
        delta: Int,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState = when (entityId) {
        config.hostEntityId -> state.copy(playerScoreA = state.playerScoreA + delta)
        config.guestEntityId -> state.copy(playerScoreB = state.playerScoreB + delta)
        else -> state
    }

    private fun resolveOnlineCollisions(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        var world = state
        val pacs = world.playerPacs()
        if (pacs.size >= 2) {
            val a = pacs[0]
            val b = pacs[1]
            if (PacMazeMotion.tileX(a.x) == PacMazeMotion.tileX(b.x) &&
                PacMazeMotion.tileY(a.y) == PacMazeMotion.tileY(b.y)
            ) {
                world = bouncePacs(world, a, b)
            }
        }
        pacs.forEach { pac ->
            world.entities.filter { it.role == "ghost" }.forEach { ghost ->
                if (PacMazeMotion.tileX(ghost.x) != PacMazeMotion.tileX(pac.x) ||
                    PacMazeMotion.tileY(ghost.y) != PacMazeMotion.tileY(pac.y)
                ) return@forEach
                world = resolveGhostHit(world, pac, ghost, level, config)
            }
        }
        return world
    }

    private fun bouncePacs(state: PacMazeWorldState, a: PacMazeEntity, b: PacMazeEntity): PacMazeWorldState {
        val ax = if (a.x <= b.x) a.x - 0.3f else a.x + 0.3f
        val bx = if (a.x <= b.x) b.x + 0.3f else b.x - 0.3f
        return state.copy(
            entities = state.entities.map { entity ->
                when (entity.id) {
                    a.id -> entity.copy(x = ax.coerceIn(0.5f, state.width - 1.5f), velX = 0f, velY = 0f)
                    b.id -> entity.copy(x = bx.coerceIn(0.5f, state.width - 1.5f), velX = 0f, velY = 0f)
                    else -> entity
                }
            },
        )
    }

    private fun resolveGhostHit(
        state: PacMazeWorldState,
        pac: PacMazeEntity,
        ghost: PacMazeEntity,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        if (ghost.ghostMode == GhostMode.FRIGHTENED) {
            return state.copy(
                entities = state.entities.map { e ->
                    if (e.id != ghost.id) e else e.copy(ghostMode = GhostMode.EATEN, velX = 0f, velY = 0f)
                },
                score = state.score + PacMazeConstants.GHOST_SCORE,
            ).let { applyPlayerScore(it, pac.id, PacMazeConstants.GHOST_SCORE, config) }
        }
        if (ghost.ghostMode == GhostMode.EATEN) return state
        PacMazeItems.tryConsumeShield(state)?.let { return it }
        return respawnPacAfterHit(state, pac, level, config)
    }

    private fun respawnPacAfterHit(
        state: PacMazeWorldState,
        pac: PacMazeEntity,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        val isCoop = config.mode == PacMazeOnlineMatchMode.COOP_CAMPAIGN
        val spawn = if (pac.id == config.hostEntityId) level.pacSpawn else (level.pacSpawnB ?: level.pacSpawn)
        val resetPac = pac.copy(
            x = spawn.first + 0.5f,
            y = spawn.second + 0.5f,
            direction = null,
            velX = 0f,
            velY = 0f,
            nextDirection = null,
        )
        var world = state.copy(
            entities = state.entities.map { if (it.id == pac.id) resetPac else it },
        )
        world = if (isCoop) {
            val lives = (world.teamLives - 1).coerceAtLeast(0)
            world.copy(
                teamLives = lives,
                lives = lives,
                phase = if (lives <= 0) PacMazePhase.GAME_OVER else PacMazePhase.PLAYING,
            )
        } else {
            when (pac.id) {
                config.hostEntityId -> {
                    val lives = world.playerLivesA - 1
                    world.copy(
                        playerLivesA = lives.coerceAtLeast(0),
                        phase = if (lives <= 0) PacMazePhase.GAME_OVER else PacMazePhase.PLAYING,
                        onlineWinnerEntityId = if (lives <= 0) config.guestEntityId else null,
                    )
                }
                config.guestEntityId -> {
                    val lives = world.playerLivesB - 1
                    world.copy(
                        playerLivesB = lives.coerceAtLeast(0),
                        phase = if (lives <= 0) PacMazePhase.GAME_OVER else PacMazePhase.PLAYING,
                        onlineWinnerEntityId = if (lives <= 0) config.hostEntityId else null,
                    )
                }
                else -> world
            }
        }
        return world
    }

    private fun checkOnlineWin(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        if (state.phase == PacMazePhase.LEVEL_CLEAR || state.phase == PacMazePhase.GAME_OVER) return state
        val elapsed = state.onlineElapsedSeconds
        if (config.mode == PacMazeOnlineMatchMode.VERSUS_DUEL) {
            when (config.versusRule) {
                PacMazeVersusRule.RACE_PELLETS -> {
                    if (state.pelletZoneA.isEmpty() && state.pelletZoneAInitial > 0) {
                        return state.copy(
                            phase = PacMazePhase.LEVEL_CLEAR,
                            onlineWinnerEntityId = config.hostEntityId,
                        )
                    }
                    if (state.pelletZoneB.isEmpty() && state.pelletZoneBInitial > 0) {
                        return state.copy(
                            phase = PacMazePhase.LEVEL_CLEAR,
                            onlineWinnerEntityId = config.guestEntityId,
                        )
                    }
                }
                PacMazeVersusRule.LAST_LIFE -> {
                    if (state.playerLivesA <= 0 || state.playerLivesB <= 0) return state
                }
                PacMazeVersusRule.RACE_EXIT -> {
                    // handled by checkExitReached per player — extend if needed
                }
            }
            if (elapsed >= config.timeLimitSeconds) {
                val winner = when {
                    state.playerScoreA > state.playerScoreB -> config.hostEntityId
                    state.playerScoreB > state.playerScoreA -> config.guestEntityId
                    else -> null
                }
                return state.copy(
                    phase = PacMazePhase.LEVEL_CLEAR,
                    onlineWinnerEntityId = winner,
                    onlineEndReason = if (winner == null) PacMazeOnlineEndReason.DRAW else PacMazeOnlineEndReason.TIMEOUT,
                )
            }
        }
        return state
    }

    private fun leadingPac(
        world: PacMazeWorldState,
        pacs: List<PacMazeEntity>,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeEntity {
        if (config.mode != PacMazeOnlineMatchMode.VERSUS_DUEL) {
            return pacs.firstOrNull() ?: world.primaryPac() ?: pacs.first()
        }
        return when {
            world.playerScoreA > world.playerScoreB -> pacs.first { it.id == config.hostEntityId }
            world.playerScoreB > world.playerScoreA -> pacs.first { it.id == config.guestEntityId }
            else -> pacs.first()
        }
    }

    private fun fireAttackFor(state: PacMazeWorldState, pac: PacMazeEntity): PacMazeWorldState {
        if (state.attackCharges <= 0 || state.attackCooldownTicksLeft > 0) return state
        val dir = pac.facing
        val projectile = PacMazeProjectile(
            id = "proj_${pac.id}_${state.tick}",
            x = pac.x,
            y = pac.y,
            direction = dir,
        )
        return state.copy(
            attackCharges = state.attackCharges - 1,
            attackCooldownTicksLeft = PacMazeConstants.ATTACK_COOLDOWN_TICKS,
            projectiles = state.projectiles + projectile,
        )
    }

    private fun tickPacEntity(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        input: PacMazeTickInput,
    ): PacMazeEntity = PacMazeMotion.tickPlayer(
        state = state,
        entity = entity,
        input = input,
        movementMode = state.movementMode,
        cosmeticSpeedMultiplier = 1f,
    )

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
        val speed = PacMazeConstants.ghostSpeedCellsPerSec(mode, level.ghostSpeedMul * current.ghostKind.speedMul)
        val picked = PacMazeGhostAi.pickDirection(
            state = state,
            ghost = current.copy(ghostMode = mode),
            pac = pac,
            rng = rng,
            level = level,
            escapeOnly = false,
            allowReverse = false,
            speedCellsPerSec = speed,
        ) ?: current.direction ?: Direction.LEFT
        current = current.copy(ghostMode = mode, speed = speed, direction = picked, facing = picked)
        val moved = PacMazePortals.applyTransit(state, PacMazeMotion.tickGhost(state, current, picked, speed), level)
        return moved.copy(facing = moved.direction ?: picked)
    }

    private fun sanitizePac(state: PacMazeWorldState, entity: PacMazeEntity): PacMazeEntity {
        if (PacMazeMotion.isPositionLegal(state, entity.x, entity.y, forGhost = false)) return entity
        return PacMazeMotion.sanitize(state, entity, forGhost = false)
    }

    private fun sanitizeGhost(state: PacMazeWorldState, entity: PacMazeEntity): PacMazeEntity {
        if (PacMazeMotion.isPositionLegal(state, entity.x, entity.y, forGhost = true, ghost = entity)) return entity
        return PacMazeMotion.sanitize(state, entity, forGhost = true)
    }

    private fun advanceTimers(state: PacMazeWorldState, config: PacMazeOnlineMatchConfig): PacMazeWorldState {
        var ghostMode = state.ghostMode
        var ghostTicks = state.ghostModeTicksLeft - 1
        if (ghostTicks <= 0) {
            ghostMode = if (ghostMode == GhostMode.CHASE) GhostMode.SCATTER else GhostMode.CHASE
            ghostTicks = PacMazeConstants.GHOST_MODE_CYCLE_TICKS
        }
        val release = (state.ghostReleaseTicksLeft - 1).coerceAtLeast(0)
        return state.copy(
            tick = state.tick + 1,
            powerTicksLeft = (state.powerTicksLeft - 1).coerceAtLeast(0),
            ghostMode = ghostMode,
            ghostModeTicksLeft = ghostTicks,
            ghostReleaseTicksLeft = release,
            attackCooldownTicksLeft = (state.attackCooldownTicksLeft - 1).coerceAtLeast(0),
        )
    }

    private fun triggerOpportunistBurst(state: PacMazeWorldState): PacMazeWorldState =
        state.copy(
            entities = state.entities.map { entity ->
                if (entity.role == "ghost" &&
                    entity.ghostKind.behaviorArchetype == GhostBehaviorArchetype.OPPORTUNIST
                ) {
                    entity.copy(opportunistBurstTicksLeft = PacMazeGhostRoster.OPPORTUNIST_BURST_TICKS)
                } else entity
            },
        )
}
