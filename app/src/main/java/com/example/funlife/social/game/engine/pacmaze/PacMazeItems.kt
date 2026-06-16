package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.hypot

/** 道具系统：工厂产出、拾取、持续效果。 */
object PacMazeItems {

    private val magnetDt = 1f / PacMazeConstants.TICKS_PER_SECOND

    fun initSpawnerStates(defs: List<PacMazeItemSpawnerDef>): List<PacMazeItemSpawnerState> =
        defs.mapIndexed { index, def ->
            val stagger = (def.intervalTicks / defs.size.coerceAtLeast(1)) * index
            PacMazeItemSpawnerState(
                id = def.id,
                cooldownTicks = def.intervalTicks / 3 + stagger,
            )
        }

    fun tickEffects(state: PacMazeWorldState): PacMazeWorldState {
        if (state.phase != PacMazePhase.PLAYING) return state
        return state.copy(
            magnetTicksLeft = (state.magnetTicksLeft - 1).coerceAtLeast(0),
            frostTicksLeft = (state.frostTicksLeft - 1).coerceAtLeast(0),
            speedBoostTicksLeft = (state.speedBoostTicksLeft - 1).coerceAtLeast(0),
            scoreBoostTicksLeft = (state.scoreBoostTicksLeft - 1).coerceAtLeast(0),
        )
    }

    fun tick(
        state: PacMazeWorldState,
    ): PacMazeWorldState {
        if (state.phase != PacMazePhase.PLAYING) return state
        if (state.itemSpawners.isEmpty()) return tickFloorItems(state)

        val rng = PacMazeDeterministicRng(state.rngSeed + state.tick * 31L)
        var world = state
        val updatedStates = state.itemSpawnerStates.map { runtime ->
            val def = state.itemSpawners.firstOrNull { it.id == runtime.id } ?: return@map runtime
            val hasUnpickedItem = world.floorItems.any { it.spawnerId == def.id }
            if (hasUnpickedItem) {
                return@map runtime.copy(pulseTick = runtime.pulseTick + 1)
            }

            var cooldown = runtime.cooldownTicks - 1
            var pulse = runtime.pulseTick + 1
            if (cooldown <= 0) {
                val spawn = findSpawnTile(world, def, rng)
                if (spawn != null) {
                    val kind = def.pool.random(rng)
                    val itemId = "item_${world.nextFloorItemId}"
                    world = world.copy(
                        floorItems = world.floorItems + PacMazeFloorItem(
                            id = itemId,
                            kind = kind,
                            x = spawn.first,
                            y = spawn.second,
                            spawnerId = def.id,
                        ),
                        nextFloorItemId = world.nextFloorItemId + 1,
                    )
                    cooldown = def.intervalTicks
                    pulse = 0
                } else {
                    cooldown = 90
                }
            }
            runtime.copy(cooldownTicks = cooldown, pulseTick = pulse)
        }
        world = world.copy(itemSpawnerStates = updatedStates)
        return tickFloorItems(world)
    }

    private fun tickFloorItems(state: PacMazeWorldState): PacMazeWorldState {
        val remaining = state.floorItems.mapNotNull { item ->
            if (item.spawnerId.isNotEmpty()) {
                item
            } else {
                val left = item.ticksLeft - 1
                if (left <= 0) null else item.copy(ticksLeft = left)
            }
        }
        return if (remaining.size == state.floorItems.size) state else state.copy(floorItems = remaining)
    }

    private fun findSpawnTile(
        state: PacMazeWorldState,
        def: PacMazeItemSpawnerDef,
        rng: PacMazeDeterministicRng,
    ): Pair<Int, Int>? {
        val candidates = mutableListOf<Pair<Int, Int>>()
        val offsets = listOf(0 to 0, 0 to -1, 0 to 1, -1 to 0, 1 to 0)
        for ((dx, dy) in offsets.shuffled(rng)) {
            val x = def.x + dx
            val y = def.y + dy
            if (!isSpawnableTile(state, x, y)) continue
            if (state.floorItems.any { it.x == x && it.y == y }) continue
            candidates.add(x to y)
        }
        return candidates.firstOrNull()
    }

    private fun isSpawnableTile(state: PacMazeWorldState, x: Int, y: Int): Boolean {
        if (x !in 0 until state.width || y !in 0 until state.height) return false
        val tile = state.tileAt(x, y)
        if (PacMazeLevelProgression.isPortalOrGateTile(tile)) return false
        if (!tile.isWalkableFloor()) return false
        if (tile == TileType.POWER) return false
        return state.entities.none {
            PacMazeMotion.tileX(it.x) == x && PacMazeMotion.tileY(it.y) == y
        }
    }

    fun tryPickup(state: PacMazeWorldState, pacX: Float, pacY: Float): PacMazeWorldState {
        val tx = PacMazeMotion.tileX(pacX)
        val ty = PacMazeMotion.tileY(pacY)
        val item = state.floorItems.firstOrNull { it.x == tx && it.y == ty } ?: return state
        val without = state.floorItems.filterNot { it.id == item.id }
        val spawnerStates = if (item.spawnerId.isNotEmpty()) {
            val interval = state.itemSpawners.firstOrNull { it.id == item.spawnerId }?.intervalTicks
                ?: PacMazeItemConstants.SPAWNER_INTERVAL_TICKS
            state.itemSpawnerStates.map { runtime ->
                if (runtime.id == item.spawnerId) runtime.copy(cooldownTicks = interval) else runtime
            }
        } else {
            state.itemSpawnerStates
        }
        return applyItem(
            state.copy(
                floorItems = without,
                itemSpawnerStates = spawnerStates,
                score = state.score + PacMazeLevelProgression.itemPickupScore(state.levelId),
            ),
            item.kind,
        )
    }

    fun applyItem(state: PacMazeWorldState, kind: PacMazeItemKind): PacMazeWorldState {
        val durationMul = PacMazeLevelProgression.itemDurationMultiplier(state.levelId)
        fun scaledTicks(base: Int): Int = (base * durationMul).toInt().coerceAtLeast(base)
        return when (kind) {
            PacMazeItemKind.MAGNET -> state.copy(
                magnetTicksLeft = scaledTicks(PacMazeItemConstants.MAGNET_DURATION_TICKS),
            )
            PacMazeItemKind.SHIELD -> state.copy(
                shieldCharges = state.shieldCharges + PacMazeLevelProgression.shieldGrant(state.levelId),
            )
            PacMazeItemKind.FROST -> state.copy(
                frostTicksLeft = scaledTicks(PacMazeItemConstants.FROST_DURATION_TICKS),
            )
            PacMazeItemKind.SPEED -> state.copy(
                speedBoostTicksLeft = scaledTicks(PacMazeItemConstants.SPEED_DURATION_TICKS),
            )
            PacMazeItemKind.DOUBLE -> state.copy(
                scoreBoostTicksLeft = scaledTicks(PacMazeItemConstants.DOUBLE_DURATION_TICKS),
            )
            PacMazeItemKind.CHARGE -> state.copy(
                attackCharges = state.attackCharges + PacMazeLevelProgression.chargeGrant(state.levelId),
            )
        }
    }

    fun tickMagnet(state: PacMazeWorldState): PacMazeWorldState {
        if (state.magnetTicksLeft <= 0 && state.magnetPulls.isEmpty()) return state
        val pac = state.entities.firstOrNull { it.role == "pac" } ?: return state
        val px = PacMazeMotion.centerX(pac.x)
        val py = PacMazeMotion.centerY(pac.y)

        var world = if (state.magnetTicksLeft > 0) beginMagnetPulls(state, px, py) else state
        world = advanceMagnetPulls(world, px, py)
        return world
    }

    private fun beginMagnetPulls(state: PacMazeWorldState, px: Float, py: Float): PacMazeWorldState {
        val radius = PacMazeLevelProgression.magnetRadiusCells(state.levelId)
        val activeSources = state.magnetPulls.map { it.sourceX to it.sourceY }.toSet()
        val tiles = state.tiles.copyOf()
        val newPulls = mutableListOf<PacMazeMagnetPull>()
        var nextId = state.nextMagnetPullId

        for (y in 0 until state.height) {
            for (x in 0 until state.width) {
                if ((x to y) in activeSources) continue
                val idx = y * state.width + x
                val tile = tiles[idx]
                val isPower = tile == TileType.POWER.code
                val isPellet = tile == TileType.PELLET.code
                if (!isPower && !isPellet) continue
                val cx = x + 0.5f
                val cy = y + 0.5f
                if (hypot(cx - px, cy - py) > radius) continue
                tiles[idx] = TileType.EMPTY.code
                newPulls.add(
                    PacMazeMagnetPull(
                        id = "mp_$nextId",
                        x = cx,
                        y = cy,
                        sourceX = x,
                        sourceY = y,
                        isPower = isPower,
                    ),
                )
                nextId++
            }
        }
        if (newPulls.isEmpty()) return state
        return state.copy(
            tiles = tiles,
            magnetPulls = state.magnetPulls + newPulls,
            nextMagnetPullId = nextId,
        )
    }

    private fun advanceMagnetPulls(state: PacMazeWorldState, px: Float, py: Float): PacMazeWorldState {
        if (state.magnetPulls.isEmpty()) return state
        val step = PacMazeItemConstants.MAGNET_PULL_SPEED_CELLS_PER_SEC * magnetDt
        val collectRadius = PacMazeItemConstants.MAGNET_COLLECT_RADIUS_CELLS
        var world = state
        val remaining = mutableListOf<PacMazeMagnetPull>()

        state.magnetPulls.forEach { pull ->
            val dx = px - pull.x
            val dy = py - pull.y
            val dist = hypot(dx, dy)
            if (dist <= collectRadius) {
                world = applyMagnetCollected(world, pull)
            } else {
                val move = (step / dist).coerceAtMost(1f)
                remaining.add(
                    pull.copy(
                        x = pull.x + dx * move,
                        y = pull.y + dy * move,
                    ),
                )
            }
        }
        return world.copy(magnetPulls = remaining)
    }

    private fun applyMagnetCollected(state: PacMazeWorldState, pull: PacMazeMagnetPull): PacMazeWorldState {
        val scoreGain = if (pull.isPower) {
            PacMazeConstants.POWER_SCORE
        } else {
            pelletScore(state)
        }
        val pelletsLeft = (state.pelletsRemaining - 1).coerceAtLeast(0)
        val phase = if (pelletsLeft == 0 && state.phase == PacMazePhase.PLAYING) {
            PacMazePhase.LEVEL_CLEAR
        } else {
            state.phase
        }
        return state.copy(
            score = state.score + scoreGain,
            pelletsRemaining = pelletsLeft,
            powerTicksLeft = if (pull.isPower) PacMazeConstants.POWER_DURATION_TICKS else state.powerTicksLeft,
            attackCharges = if (pull.isPower) state.attackCharges + 1 else state.attackCharges,
            phase = phase,
        )
    }

    fun pelletScore(state: PacMazeWorldState): Int {
        val mul = if (state.scoreBoostTicksLeft > 0) 2 else 1
        return PacMazeConstants.PELLET_SCORE * mul
    }

    fun pacSpeedMultiplier(state: PacMazeWorldState): Float =
        if (state.speedBoostTicksLeft > 0) PacMazeItemConstants.SPEED_MULTIPLIER else 1f

    fun ghostsFrozen(state: PacMazeWorldState): Boolean = state.frostTicksLeft > 0

    fun tryConsumeShield(state: PacMazeWorldState): PacMazeWorldState? {
        if (state.shieldCharges <= 0) return null
        return state.copy(shieldCharges = state.shieldCharges - 1)
    }

    fun applyFatalDamage(
        state: PacMazeWorldState,
        level: PacMazeLevelConfig,
    ): PacMazeWorldState {
        tryConsumeShield(state)?.let { return it }
        return PacMazeHazards.applyPacDamage(state, level)
    }
}

private fun List<PacMazeItemKind>.random(rng: PacMazeDeterministicRng): PacMazeItemKind {
    if (isEmpty()) return PacMazeItemKind.MAGNET
    return this[rng.nextInt(size)]
}

private fun List<Pair<Int, Int>>.shuffled(rng: PacMazeDeterministicRng): List<Pair<Int, Int>> {
    val copy = toMutableList()
    for (i in copy.lastIndex downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = copy[i]
        copy[i] = copy[j]
        copy[j] = tmp
    }
    return copy
}
