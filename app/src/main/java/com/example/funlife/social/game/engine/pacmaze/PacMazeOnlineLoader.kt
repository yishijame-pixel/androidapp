package com.example.funlife.social.game.engine.pacmaze

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** 在线对战竞技场与双人合作关卡加载。 */
object PacMazeOnlineLoader {

    fun loadArena(context: Context, arenaId: String): Pair<PacMazeLevelConfig, String> {
        val path = "pac_maze/arenas/$arenaId.json"
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        return parseArenaJson(json)
    }

    fun parseArenaJson(json: String): Pair<PacMazeLevelConfig, String> {
        val root = JsonParser.parseString(json).asJsonObject
        val id = root.get("id")?.asString ?: "arena_001"
        val name = root.get("name")?.asString ?: "竞技场"
        val width = root.get("width").asInt
        val height = root.get("height").asInt
        val spawn = root.getAsJsonObject("spawn")
        val spawnA = readSpawnPair(spawn, "pac_a", "pac")
        val spawnB = readSpawnPair(spawn, "pac_b", fallback = spawnA.first to (height - 2).coerceAtLeast(1))
        val ghosts = parseArenaGhosts(spawn)
        val level = PacMazeLevelConfig(
            id = -arenaNumericId(id),
            name = name,
            width = width,
            height = height,
            pacSpawn = spawnA,
            pacSpawnB = spawnB,
            ghostSpawns = ghosts,
            ghostSpeedMul = 0.55f,
            aiAggression = 0.65f,
            modeRules = PacMazeModeRules(
                winCondition = PacMazeWinCondition.CLEAR_PELLETS,
                timeLimitSeconds = 150,
            ),
        )
        return level to json
    }

    fun buildOnlineWorld(
        level: PacMazeLevelConfig,
        json: String,
        config: PacMazeOnlineMatchConfig,
    ): PacMazeWorldState {
        val base = PacMazeMapLoader.buildInitialWorld(level, json, config.matchSeed)
        val isCoop = config.mode == PacMazeOnlineMatchMode.COOP_CAMPAIGN
        val isVersus = config.mode == PacMazeOnlineMatchMode.VERSUS_DUEL
        val spawnB = level.pacSpawnB ?: mirrorSpawn(level.pacSpawn, level.width)
        val entities = buildList {
            add(
                PacMazeEntity(
                    id = config.hostEntityId,
                    role = "pac_a",
                    x = level.pacSpawn.first.toFloat(),
                    y = level.pacSpawn.second.toFloat(),
                    direction = null,
                    speed = PacMazeConstants.PAC_SPEED,
                ),
            )
            add(
                PacMazeEntity(
                    id = config.guestEntityId,
                    role = "pac_b",
                    x = spawnB.first.toFloat(),
                    y = spawnB.second.toFloat(),
                    direction = null,
                    speed = PacMazeConstants.PAC_SPEED,
                ),
            )
            // 在线对战 Phase 1：仅双人，不投放地图幽灵 AI
        }
        val zoneA = if (isVersus) pelletZoneIndices(base, level.width) { x, _ -> x < level.width / 2 } else emptySet()
        val zoneB = if (isVersus) pelletZoneIndices(base, level.width) { x, _ -> x > level.width / 2 } else emptySet()
        return base.copy(
            entities = entities,
            lives = if (isCoop) config.teamLives else config.playerLivesEach,
            teamLives = if (isCoop) config.teamLives else 0,
            playerLivesA = if (isVersus) config.playerLivesEach else 0,
            playerLivesB = if (isVersus) config.playerLivesEach else 0,
            playerScoreA = 0,
            playerScoreB = 0,
            matchModeId = config.mode.id,
            pelletZoneA = zoneA,
            pelletZoneB = zoneB,
            pelletZoneAInitial = zoneA.size,
            pelletZoneBInitial = zoneB.size,
            hostEntityId = config.hostEntityId,
            guestEntityId = config.guestEntityId,
            ghostReleaseTicksLeft = 0,
        )
    }

    fun coopEnabledLevel(levelId: Int): Boolean = levelId in 1..8

    private fun arenaNumericId(id: String): Int =
        id.removePrefix("arena_").toIntOrNull() ?: 1

    private fun readSpawnPair(
        spawn: JsonObject,
        primaryKey: String,
        fallbackKey: String? = null,
        fallback: Pair<Int, Int>? = null,
    ): Pair<Int, Int> {
        val arr = spawn.getAsJsonArray(primaryKey)
            ?: fallbackKey?.let { spawn.getAsJsonArray(it) }
        if (arr != null && arr.size() >= 2) {
            return arr[0].asInt to arr[1].asInt
        }
        return fallback ?: (1 to 1)
    }

    private fun parseArenaGhosts(spawn: JsonObject): List<PacMazeGhostSpawnDef> {
        val positions = spawn.getAsJsonArray("ghosts")?.mapNotNull { el ->
            val a = el.asJsonArray
            if (a.size() < 2) return@mapNotNull null
            a[0].asInt to a[1].asInt
        }.orEmpty()
        return positions.mapIndexed { index, (x, y) ->
            PacMazeGhostSpawnDef(x, y, GhostKind.STRIKER)
        }
    }

    private fun mirrorSpawn(spawn: Pair<Int, Int>, width: Int): Pair<Int, Int> =
        (width - 1 - spawn.first).coerceIn(1, width - 2) to spawn.second

    private fun pelletZoneIndices(
        world: PacMazeWorldState,
        width: Int,
        predicate: (x: Int, y: Int) -> Boolean,
    ): Set<Int> {
        val result = mutableSetOf<Int>()
        for (y in 0 until world.height) {
            for (x in 0 until width) {
                if (!predicate(x, y)) continue
                val idx = y * width + x
                val tile = world.tiles[idx]
                if (tile == TileType.PELLET.code || tile == TileType.POWER.code) {
                    result.add(idx)
                }
            }
        }
        return result
    }
}
