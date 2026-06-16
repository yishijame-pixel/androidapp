package com.example.funlife.social.game.engine.pacmaze

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object PacMazeMapLoader {

  fun parseLevelJson(json: String): PacMazeLevelConfig {
    val root = JsonParser.parseString(json).asJsonObject
    val id = root.get("id").asInt
    val name = root.get("name").asString
    val width = root.get("width").asInt
    val height = root.get("height").asInt
    val spawn = root.getAsJsonObject("spawn")
    val pac = spawn.getAsJsonArray("pac")
    val pacSpawn = pac[0].asInt to pac[1].asInt
    val positions = parseGhostPositions(spawn)
    val hasDynamic = scanHasDynamicTiles(root, width, height)
    val hasGates = scanHasEnergyGates(root, width, height)
    val explicitGhosts = parseExplicitGhostSpawns(spawn)
    val ghosts = if (explicitGhosts.isNotEmpty()) {
      explicitGhosts
    } else {
      PacMazeGhostRoster.resolveSpawns(
        levelId = id,
        positions = positions,
        hasDynamicTiles = hasDynamic,
        hasEnergyGates = hasGates,
      )
    }
    val diff = root.getAsJsonObject("difficulty")
    val authoredSpeed = diff?.get("ghost_speed_mul")?.asFloat ?: 1f
    val authoredAggression = diff?.get("ai_aggression")?.asFloat ?: 0.8f
    val (speedMul, aggression) = PacMazeLevelProgression.resolveDifficulty(id, authoredSpeed, authoredAggression)
    val markers = parseMarkers(root, width, height, pacSpawn)
    val gridHazards = scanGridHazards(root, width, height)
    val jsonHazards = parseHazardsFromJson(root, startIndex = gridHazards.size)
    val tiles = decodeTiles(root, width, height)
    val parsedSpawners = parseItemSpawners(root, width, height)
    val linkPortalTiles = linkPortalTiles(markers, width)
    val itemSpawners = PacMazeLevelProgression.enrichItemSpawners(
        levelId = id,
        width = width,
        height = height,
        tiles = tiles,
        pacSpawn = pacSpawn,
        ghostSpawns = ghosts,
        existing = parsedSpawners,
        linkPortalTiles = linkPortalTiles,
    )
    val modeRules = parseModeRules(root)
    return PacMazeLevelConfig(
      id = id,
      name = name,
      width = width,
      height = height,
      pacSpawn = pacSpawn,
      ghostSpawns = ghosts,
      ghostSpeedMul = speedMul,
      aiAggression = aggression,
      markers = markers,
      hazards = gridHazards + jsonHazards,
      itemSpawners = itemSpawners,
      starCriteria = PacMazeStarCriteria.fromLevelJson(root),
      modeRules = modeRules,
    )
  }

  private fun parseItemSpawners(root: JsonObject, width: Int, height: Int): List<PacMazeItemSpawnerDef> {
    var index = 0
    val fromJson = root.getAsJsonArray("itemSpawners")?.mapNotNull { element ->
      val obj = element.asJsonObject
      val x = obj.get("x")?.asInt ?: return@mapNotNull null
      val y = obj.get("y")?.asInt ?: return@mapNotNull null
      val id = obj.get("id")?.asString ?: "item_sp_${index++}"
      val interval = obj.get("intervalTicks")?.asInt
        ?: obj.get("interval")?.asInt
        ?: PacMazeItemConstants.SPAWNER_INTERVAL_TICKS
      val pool = obj.getAsJsonArray("pool")?.mapNotNull { el ->
        PacMazeItemKind.fromId(el.asString)
      }.orEmpty().ifEmpty { PacMazeItemKind.DEFAULT_POOL }
      PacMazeItemSpawnerDef(id = id, x = x, y = y, intervalTicks = interval, pool = pool)
    }.orEmpty()
    val fromGrid = scanGridItemSpawners(root, width, height, startIndex = fromJson.size)
        .filter { grid -> fromJson.none { it.x == grid.x && it.y == grid.y } }
    return fromJson + fromGrid
  }

  private fun scanGridItemSpawners(
    root: JsonObject,
    width: Int,
    height: Int,
    startIndex: Int,
  ): List<PacMazeItemSpawnerDef> {
    val grid = root.getAsJsonArray("grid") ?: return emptyList()
    val spawners = mutableListOf<PacMazeItemSpawnerDef>()
    var index = startIndex
    for (y in 0 until height) {
      val row = grid[y].asString
      for (x in 0 until width) {
        if (row.getOrElse(x) { '#' } == '$') {
          spawners.add(
            PacMazeItemSpawnerDef(
              id = "grid_sp_$index",
              x = x,
              y = y,
            ),
          )
          index++
        }
      }
    }
    return spawners
  }

  private fun parseHazardsFromJson(root: JsonObject, startIndex: Int): List<PacMazeHazardDef> {
    var index = startIndex
    return root.getAsJsonArray("hazards")?.mapNotNull { element ->
      val obj = element.asJsonObject
      val type = obj.get("type")?.asString?.lowercase() ?: return@mapNotNull null
      val id = obj.get("id")?.asString ?: "hz_${index++}"
      when (type) {
        "laser_h", "laser_row" -> {
          val y = obj.get("y")?.asInt ?: return@mapNotNull null
          val x1 = obj.get("x1")?.asInt ?: obj.get("rangeStart")?.asInt ?: return@mapNotNull null
          val x2 = obj.get("x2")?.asInt ?: obj.get("rangeEnd")?.asInt ?: return@mapNotNull null
          val x = obj.get("x")?.asInt ?: minOf(x1, x2)
          PacMazeHazardDef(id, PacMazeHazardKind.LASER_ROW, x, y, minOf(x1, x2), maxOf(x1, x2))
        }
        "laser_v", "laser_col" -> {
          val x = obj.get("x")?.asInt ?: return@mapNotNull null
          val y1 = obj.get("y1")?.asInt ?: obj.get("rangeStart")?.asInt ?: return@mapNotNull null
          val y2 = obj.get("y2")?.asInt ?: obj.get("rangeEnd")?.asInt ?: return@mapNotNull null
          val y = obj.get("y")?.asInt ?: minOf(y1, y2)
          PacMazeHazardDef(id, PacMazeHazardKind.LASER_COL, x, y, minOf(y1, y2), maxOf(y1, y2))
        }
        "turret" -> {
          val x = obj.get("x")?.asInt ?: return@mapNotNull null
          val y = obj.get("y")?.asInt ?: return@mapNotNull null
          val dir = parseDirection(obj.get("dir")?.asString ?: "right")
          PacMazeHazardDef(id, PacMazeHazardKind.TURRET, x, y, x, x, dir)
        }
        else -> null
      }
    }.orEmpty()
  }

  private fun parseDirection(raw: String): Direction = when (raw.lowercase()) {
    "up" -> Direction.UP
    "down" -> Direction.DOWN
    "left" -> Direction.LEFT
    else -> Direction.RIGHT
  }

  private fun scanGridHazards(root: JsonObject, width: Int, height: Int): List<PacMazeHazardDef> {
    val grid = root.getAsJsonArray("grid")
    val tiles = IntArray(width * height)
    for (y in 0 until height) {
      val row = grid[y].asString
      for (x in 0 until width) {
        tiles[y * width + x] = charToTile(row.getOrElse(x) { '#' }).code
      }
    }
    val hazards = mutableListOf<PacMazeHazardDef>()
    var index = 0
    for (y in 0 until height) {
      val row = grid[y].asString
      for (x in 0 until width) {
        when (row.getOrElse(x) { '#' }) {
          'H' -> {
            val (left, right) = horizontalRange(tiles, width, y, x)
            hazards.add(
              PacMazeHazardDef("ghz_$index", PacMazeHazardKind.LASER_ROW, x, y, left, right),
            )
            index++
          }
          'I' -> {
            val (top, bottom) = verticalRange(tiles, width, height, x, y)
            hazards.add(
              PacMazeHazardDef("gvz_$index", PacMazeHazardKind.LASER_COL, x, y, top, bottom),
            )
            index++
          }
          '>' -> {
            hazards.add(PacMazeHazardDef("gt_$index", PacMazeHazardKind.TURRET, x, y, x, x, Direction.RIGHT))
            index++
          }
          '<' -> {
            hazards.add(PacMazeHazardDef("gt_$index", PacMazeHazardKind.TURRET, x, y, x, x, Direction.LEFT))
            index++
          }
          '^' -> {
            hazards.add(PacMazeHazardDef("gt_$index", PacMazeHazardKind.TURRET, x, y, x, x, Direction.UP))
            index++
          }
          'v' -> {
            hazards.add(PacMazeHazardDef("gt_$index", PacMazeHazardKind.TURRET, x, y, x, x, Direction.DOWN))
            index++
          }
        }
      }
    }
    return hazards
  }

  private fun horizontalRange(tiles: IntArray, width: Int, y: Int, anchorX: Int): Pair<Int, Int> {
    var left = anchorX
    var right = anchorX
    while (left - 1 >= 0 && !TileType.isSolidWallCode(tiles[y * width + left - 1])) left--
    while (right + 1 < width && !TileType.isSolidWallCode(tiles[y * width + right + 1])) right++
    return left to right
  }

  private fun verticalRange(tiles: IntArray, width: Int, height: Int, x: Int, anchorY: Int): Pair<Int, Int> {
    var top = anchorY
    var bottom = anchorY
    while (top - 1 >= 0 && !TileType.isSolidWallCode(tiles[(top - 1) * width + x])) top--
    while (bottom + 1 < height && !TileType.isSolidWallCode(tiles[(bottom + 1) * width + x])) bottom++
    return top to bottom
  }

  private fun linkPortalTiles(markers: List<PacMazeMapMarker>, width: Int): Set<Pair<Int, Int>> =
      PacMazePortals.pairs(markers, width)
          .flatMap { pair -> listOf(pair.left.x to pair.left.y, pair.right.x to pair.right.y) }
          .toSet()

  private fun parseMarkers(
    root: JsonObject,
    width: Int,
    height: Int,
    pacSpawn: Pair<Int, Int>,
  ): List<PacMazeMapMarker> {
    val explicit = root.getAsJsonArray("markers")?.mapNotNull { element ->
      val obj = element.asJsonObject
      val type = obj.get("type")?.asString ?: return@mapNotNull null
      val x = obj.get("x")?.asInt ?: return@mapNotNull null
      val y = obj.get("y")?.asInt ?: return@mapNotNull null
      val label = obj.get("label")?.asString ?: ""
      val tag = obj.get("tag")?.asString ?: ""
      val kind = when (type.lowercase()) {
        "start" -> PacMazeMarkerKind.START
        "checkpoint", "cp" -> PacMazeMarkerKind.CHECKPOINT
        "exit" -> PacMazeMarkerKind.EXIT
        "item_factory", "factory", "spawner" -> PacMazeMarkerKind.ITEM_FACTORY
        else -> return@mapNotNull null
      }
      PacMazeMapMarker(kind = kind, x = x, y = y, label = label, tag = tag)
    }.orEmpty()
    if (explicit.isNotEmpty()) return explicit

    val grid = root.getAsJsonArray("grid")
    val auto = buildList {
      add(PacMazeMapMarker(PacMazeMarkerKind.START, pacSpawn.first, pacSpawn.second))
      var cpIndex = 1
      for (y in 0 until height) {
        val row = grid[y].asString
        for (x in 0 until width) {
          if (row.getOrElse(x) { '#' } == '=') {
            add(
              PacMazeMapMarker(
                kind = PacMazeMarkerKind.CHECKPOINT,
                x = x,
                y = y,
                label = cpIndex.toString().padStart(3, '0'),
                tag = "LINK",
              ),
            )
            cpIndex++
          }
        }
      }
    }
    return auto
  }

  fun buildInitialWorld(level: PacMazeLevelConfig, json: String, seed: Long): PacMazeWorldState {
    val root = JsonParser.parseString(json).asJsonObject
    val tiles = decodeTiles(root, level.width, level.height)
    val pellets = tiles.count { it == TileType.PELLET.code || it == TileType.POWER.code }
    val entities = buildList {
      add(
        PacMazeEntity(
          id = PacMazeConstants.PLAYER_ID,
          role = "pac",
          x = level.pacSpawn.first.toFloat(),
          y = level.pacSpawn.second.toFloat(),
          direction = null,
          speed = PacMazeConstants.PAC_SPEED,
        ),
      )
      level.ghostSpawns.forEachIndexed { index, spawn ->
        add(
          PacMazeEntity(
            id = "ghost_$index",
            role = "ghost",
            x = spawn.x.toFloat(),
            y = spawn.y.toFloat(),
            direction = null,
            speed = PacMazeConstants.GHOST_SPEED * level.ghostSpeedMul * spawn.kind.speedMul,
            ghostMode = if (level.id <= 0) GhostMode.CHASE else GhostMode.SCATTER,
            ghostKind = spawn.kind,
            ghostSpecialty = spawn.specialty,
          ),
        )
      }
    }
    val hazardStates = PacMazeHazards.initStates(level.hazards)
    val itemSpawners = level.itemSpawners
    val itemSpawnerStates = PacMazeItems.initSpawnerStates(itemSpawners)
    val baseWorld = PacMazeWorldState(
      tick = 0L,
      levelId = level.id,
      tiles = tiles,
      width = level.width,
      height = level.height,
      entities = entities,
      score = 0,
      lives = PacMazeConstants.INITIAL_LIVES,
      pelletsRemaining = pellets,
      phase = PacMazePhase.PLAYING,
      rngSeed = seed,
      ghostMode = if (level.id <= 0) GhostMode.CHASE else GhostMode.SCATTER,
      ghostModeTicksLeft = if (level.id <= 0) {
        PacMazeConstants.GHOST_MODE_CYCLE_TICKS / 2
      } else {
        PacMazeConstants.GHOST_MODE_CYCLE_TICKS
      },
      ghostReleaseTicksLeft = if (level.id <= 0) 0 else PacMazeConstants.GHOST_RELEASE_TICKS,
      hazards = level.hazards,
      hazardStates = hazardStates,
      itemSpawners = itemSpawners,
      itemSpawnerStates = itemSpawnerStates,
    )
    return baseWorld.copy(
      entities = entities.map { entity ->
        var sanitized = PacMazeMotion.sanitize(baseWorld, entity, entity.role == "ghost")
        if (sanitized.role == "ghost" && sanitized.direction == null) {
          val initialDir = Direction.entries.firstOrNull { dir ->
            PacMazeMotion.canMoveInDir(baseWorld, sanitized.x, sanitized.y, dir, forGhost = true, ghost = sanitized)
          }
          if (initialDir != null) {
            sanitized = sanitized.copy(direction = initialDir, facing = initialDir)
          }
        }
        sanitized
      },
    ).let { world ->
      var next = world
      if (level.modeRules.fogEnabled) next = PacMazeMazeExploration.initExplored(next, level)
      next.copy(
        intelPointsRemaining = level.modeRules.intelPointsMax,
        mirrorDynamicWalls = level.modeRules.mirrorDynamicWalls,
        dynamicWallSpeedMul = level.modeRules.dynamicWallSpeedMul,
      )
    }
  }

  private fun decodeTiles(root: JsonObject, width: Int, height: Int): IntArray {
    val grid = root.getAsJsonArray("grid")
    val tiles = IntArray(width * height) { TileType.WALL.code }
    for (y in 0 until height) {
      val row = grid[y].asString
      for (x in 0 until width) {
        val ch = row.getOrElse(x) { '#' }
        tiles[y * width + x] = charToTile(ch).code
      }
    }
    return tiles
  }

  private fun charToTile(ch: Char): TileType = when (ch) {
    '#' -> TileType.WALL
    'b', 'B' -> TileType.BRICK_WALL
    'w' -> TileType.WOOD_WALL
    't', 'T' -> TileType.TILE_WALL
    '.' -> TileType.PELLET
    'o', ' ' -> TileType.EMPTY
    '*' -> TileType.POWER
    '=' -> TileType.DOOR
    '-' -> TileType.TUNNEL
    '@' -> TileType.PORTAL
    'G' -> TileType.ENERGY_GATE
    '&' -> TileType.DYNAMIC_WALL
    'H', 'I', '>', '<', '^', 'v' -> TileType.EMPTY
    '$' -> TileType.EMPTY
    'E' -> TileType.EMPTY
    else -> TileType.WALL
  }

  private fun parseModeRules(root: JsonObject): PacMazeModeRules {
    val obj = root.getAsJsonObject("modeRules") ?: return PacMazeModeRules()
    val win = when (obj.get("winCondition")?.asString?.lowercase()) {
      "reach_exit", "exit" -> PacMazeWinCondition.REACH_EXIT
      else -> PacMazeWinCondition.CLEAR_PELLETS
    }
    val requiredKeyTags = obj.getAsJsonArray("requiredKeyTags")
        ?.mapNotNull { it.asString }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
    val orderedKeyTags = obj.getAsJsonArray("orderedKeyTags")
        ?.mapNotNull { it.asString }
        ?.filter { it.isNotBlank() }
        ?: requiredKeyTags
    val ghostSignatureIds = obj.getAsJsonArray("ghostSignatureIds")
        ?.mapNotNull { it.asString }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
    return PacMazeModeRules(
      winCondition = win,
      timeLimitSeconds = obj.get("timeLimitSeconds")?.asInt ?: 0,
      fogEnabled = obj.get("fogEnabled")?.asBoolean ?: false,
      fogRadius = obj.get("fogRadius")?.asInt ?: 2,
      radarEnabled = obj.get("radarEnabled")?.asBoolean ?: false,
      requiredKeyTags = requiredKeyTags.toSet(),
      orderedKeyTags = orderedKeyTags,
      scoreMultiplier = obj.get("scoreMultiplier")?.asFloat ?: 1f,
      starTimeBonusSeconds = obj.get("starTimeBonusSeconds")?.asInt ?: 0,
      sealedKeyOrder = obj.get("sealedKeyOrder")?.asBoolean ?: false,
      hintPelletsEnabled = obj.get("hintPelletsEnabled")?.asBoolean ?: true,
      intelPointsMax = obj.get("intelPointsMax")?.asInt ?: 0,
      huntEscalation = obj.get("huntEscalation")?.asBoolean ?: false,
      ghostSignatureIds = ghostSignatureIds,
      mutatorId = obj.get("mutatorId")?.asString ?: "none",
      variantId = obj.get("variantId")?.asString ?: "standard",
      mirrorDynamicWalls = obj.get("mirrorDynamicWalls")?.asBoolean ?: false,
      radarCooldownMultiplier = obj.get("radarCooldownMultiplier")?.asFloat ?: 1f,
      revealExitOnLastKey = obj.get("revealExitOnLastKey")?.asBoolean ?: false,
      extraGhostCount = obj.get("extraGhostCount")?.asInt ?: 0,
      dynamicWallSpeedMul = obj.get("dynamicWallSpeedMul")?.asFloat ?: 1f,
    )
  }

  private fun parseExplicitGhostSpawns(spawn: JsonObject): List<PacMazeGhostSpawnDef> {
    val arr = spawn.getAsJsonArray("ghosts") ?: return emptyList()
    return arr.mapNotNull { element ->
      if (!element.isJsonObject) return@mapNotNull null
      val obj = element.asJsonObject
      val kindRaw = obj.get("kind")?.asString ?: return@mapNotNull null
      val x = obj.get("x")?.asInt ?: return@mapNotNull null
      val y = obj.get("y")?.asInt ?: return@mapNotNull null
      val kind = GhostKind.entries.firstOrNull { it.id == kindRaw } ?: return@mapNotNull null
      PacMazeGhostSpawnDef(x = x, y = y, kind = kind)
    }
  }

  private fun parseGhostPositions(spawn: JsonObject): List<Pair<Int, Int>> {
    val arr = spawn.getAsJsonArray("ghosts") ?: return emptyList()
    return arr.mapNotNull { element ->
      when {
        element.isJsonArray -> {
          val a = element.asJsonArray
          if (a.size() >= 2) a[0].asInt to a[1].asInt else null
        }
        element.isJsonObject -> {
          val obj = element.asJsonObject
          val x = obj.get("x")?.asInt ?: return@mapNotNull null
          val y = obj.get("y")?.asInt ?: return@mapNotNull null
          x to y
        }
        else -> null
      }
    }
  }

  private fun scanHasDynamicTiles(root: JsonObject, width: Int, height: Int): Boolean {
    val grid = root.getAsJsonArray("grid") ?: return false
    for (y in 0 until minOf(grid.size(), height)) {
      val row = grid[y].asString
      for (x in 0 until minOf(row.length, width)) {
        if (row[x] == '&') return true
      }
    }
    return false
  }

  private fun scanHasEnergyGates(root: JsonObject, width: Int, height: Int): Boolean {
    val grid = root.getAsJsonArray("grid") ?: return false
    for (y in 0 until minOf(grid.size(), height)) {
      val row = grid[y].asString
      for (x in 0 until minOf(row.length, width)) {
        if (row[x] == 'G') return true
      }
    }
    return false
  }
}
