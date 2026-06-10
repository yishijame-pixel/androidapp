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
    val ghosts = spawn.getAsJsonArray("ghosts").map { arr ->
      val a = arr.asJsonArray
      a[0].asInt to a[1].asInt
    }
    val diff = root.getAsJsonObject("difficulty")
    val speedMul = diff?.get("ghost_speed_mul")?.asFloat ?: 1f
    val aggression = diff?.get("ai_aggression")?.asFloat ?: 0.8f
    val markers = parseMarkers(root, width, height, pacSpawn)
    val gridHazards = scanGridHazards(root, width, height)
    val jsonHazards = parseHazardsFromJson(root, startIndex = gridHazards.size)
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
      starCriteria = PacMazeStarCriteria.fromLevelJson(root),
      modeRules = modeRules,
    )
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
      level.ghostSpawns.forEachIndexed { index, (gx, gy) ->
        add(
          PacMazeEntity(
            id = "ghost_$index",
            role = "ghost",
            x = gx.toFloat(),
            y = gy.toFloat(),
            direction = null,
            speed = PacMazeConstants.GHOST_SPEED * level.ghostSpeedMul,
            ghostMode = GhostMode.SCATTER,
          ),
        )
      }
    }
    val hazardStates = PacMazeHazards.initStates(level.hazards)
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
      ghostMode = GhostMode.SCATTER,
      ghostModeTicksLeft = PacMazeConstants.GHOST_MODE_CYCLE_TICKS,
      ghostReleaseTicksLeft = PacMazeConstants.GHOST_RELEASE_TICKS,
      hazards = level.hazards,
      hazardStates = hazardStates,
    )
    return baseWorld.copy(
      entities = entities.map { entity ->
        var sanitized = PacMazeMotion.sanitize(baseWorld, entity, entity.role == "ghost")
        if (sanitized.role == "ghost" && sanitized.direction == null) {
          val initialDir = Direction.entries.firstOrNull { dir ->
            PacMazeMotion.canMoveInDir(baseWorld, sanitized.x, sanitized.y, dir, forGhost = true)
          }
          if (initialDir != null) {
            sanitized = sanitized.copy(direction = initialDir, facing = initialDir)
          }
        }
        sanitized
      },
    )
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
    'E' -> TileType.EMPTY
    else -> TileType.WALL
  }

  private fun parseModeRules(root: JsonObject): PacMazeModeRules {
    val obj = root.getAsJsonObject("modeRules") ?: return PacMazeModeRules()
    val win = when (obj.get("winCondition")?.asString?.lowercase()) {
      "reach_exit", "exit" -> PacMazeWinCondition.REACH_EXIT
      else -> PacMazeWinCondition.CLEAR_PELLETS
    }
    return PacMazeModeRules(
      winCondition = win,
      timeLimitSeconds = obj.get("timeLimitSeconds")?.asInt ?: 0,
    )
  }
}
