package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

/**
 * 程序生成迷宫：钥印门 + 线索豆 + 动态墙 + 多鬼，按 [PacMazeMazeRunOptions] 组合规则。
 */
object PacMazeMazeGenerator {

    fun buildLevelJson(options: PacMazeMazeRunOptions): String = buildLevelJson(
        seed = options.seed,
        options = options,
    )

    fun buildLevelJson(seed: Long, options: PacMazeMazeRunOptions): String {
        val w = (options.effectiveMapSize.coerceAtLeast(9) or 1)
        val h = w
        val rng = PacMazeDeterministicRng(seed)
        val cells = Array(h) { BooleanArray(w) }
        carve(cells, w, h, 1, 1, rng)

        val grid = Array(h) { y ->
            CharArray(w) { x -> if (cells[y][x]) 'o' else '#' }
        }

        val startX = 1
        val startY = 1
        grid[startY][startX] = 'o'

        val reachable = bfsDistances(cells, w, h, startX, startY)
        val sortedByDist = reachable.entries.sortedByDescending { it.value }
        val exitCell = sortedByDist.firstOrNull()?.key ?: (w - 2 to h - 2)
        val (exitX, exitY) = exitCell
        grid[exitY][exitX] = 'E'

        val keyTags = (1..options.effectiveKeyCount).map { "MAZE_KEY_$it" }
        val keyCells = pickKeyCells(sortedByDist, keyTags.size, exclude = setOf(startX to startY, exitX to exitY))
        val markers = mutableListOf<String>()
        markers.add("""{ "type": "start", "x": $startX, "y": $startY, "label": "起点" }""")
        keyCells.zip(keyTags).forEach { (cell, tag) ->
            val (kx, ky) = cell
            grid[ky][kx] = '='
            markers.add("""{ "type": "checkpoint", "x": $kx, "y": $ky, "label": "钥印", "tag": "$tag" }""")
        }
        markers.add("""{ "type": "exit", "x": $exitX, "y": $exitY, "label": "出口" }""")

        val deadEnds = findDeadEnds(cells, w, h).filter { (x, y) ->
            (x to y) !in keyCells && (x to y) != (startX to startY) && (x to y) != (exitX to exitY)
        }
        deadEnds.take((deadEnds.size * 0.6f).toInt().coerceAtLeast(1)).forEach { (x, y) ->
            grid[y][x] = '.'
        }

        scatterCorridorPellets(grid, cells, w, h, rng, pelletDensity(options))
        placePowerPellets(grid, sortedByDist, exclude = setOf(startX to startY, exitX to exitY) + keyCells.toSet())
        applyThemedWalls(grid, w, h, rng, themeIndex = (seed % 4).toInt())
        if (options.difficulty != PacMazeMazeDifficulty.SCOUT) {
            addCorridorLoops(cells, grid, w, h, rng, loopCount(options))
        }

        val hintCells = if (options.hintPelletsEnabled) {
            pickHintPellets(cells, w, h, startX, startY, exitX, exitY, keyCells, rng)
        } else {
            emptyList()
        }
        hintCells.forEach { (x, y) ->
            if (grid[y][x] == 'o') grid[y][x] = '.'
            markers.add("""{ "type": "checkpoint", "x": $x, "y": $y, "label": "", "tag": "HINT" }""")
        }

        if (options.placeTwinPortals) {
            placePortalPair(cells, grid, w, h, rng, markers, exclude = keyCells + listOf(startX to startY, exitX to exitY))
        }

        if (options.difficulty.dynamicWalls && options.contract != PacMazeMazeContract.SILENT) {
            placeDynamicWalls(cells, grid, w, h, rng, exclude = keyCells + listOf(startX to startY, exitX to exitY))
        }

        placeMazeMechanisms(
            options = options,
            cells = cells,
            grid = grid,
            w = w,
            h = h,
            rng = rng,
            sortedByDist = sortedByDist,
            exclude = keyCells + listOf(startX to startY, exitX to exitY),
        )

        val itemSpawnerJson = if (options.placeItemRooms) {
            buildItemSpawners(cells, w, h, rng, keyCells, deadEnds, options)
        } else {
            ""
        }

        val signatures = PacMazeMazeGhostSignature.forDailySeed(seed, options.effectiveGhostCount)
        val ghostJson = buildGhostSpawns(cells, w, h, options, rng, startX, startY, signatures)
        val gridJson = grid.joinToString(
            prefix = "[\n    \"",
            postfix = "\"\n  ]",
            separator = "\",\n    \"",
        ) { String(it) }

        val markersJson = markers.joinToString(prefix = "[\n    ", postfix = "\n  ]", separator = ",\n    ")
        val timeLimit = options.effectiveTimeLimitSeconds
        val starMaxSec = (timeLimit * 0.55f).toInt() + options.starTimeBonusSeconds
        val requiredTagsJson = keyTags.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val ghostSpeed = when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> 0f
            PacMazeMazeDifficulty.STANDARD -> 0.88f
            PacMazeMazeDifficulty.ABYSS -> 1.05f
        }
        val aggression = when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> 0f
            PacMazeMazeDifficulty.STANDARD -> 0.52f
            PacMazeMazeDifficulty.ABYSS -> 0.72f
        }

        val orderedTagsJson = keyTags.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val signatureIdsJsonFixed = signatures.joinToString(prefix = "[", postfix = "]") { sig -> "\"${sig.id}\"" }

        val title = buildString {
            append(options.difficulty.displayName)
            append("迷宫")
            if (options.dailyChallenge) append(" · 每日")
            if (options.variant != PacMazeMazeVariant.STANDARD) append(" · ${options.variant.displayName}")
            if (options.contract != PacMazeMazeContract.NONE) append(" · ${options.contract.displayName}")
        }

        return """
            {
              "id": 0,
              "name": "$title",
              "width": $w,
              "height": $h,
              "grid": $gridJson,
              "spawn": {
                "pac": [$startX, $startY],
                "ghosts": [$ghostJson]
              },
              "difficulty": { "ghost_speed_mul": $ghostSpeed, "ai_aggression": $aggression },
              "markers": $markersJson,
              ${if (itemSpawnerJson.isNotBlank()) "\"itemSpawners\": [$itemSpawnerJson]," else ""}
              "modeRules": {
                "winCondition": "reach_exit",
                "timeLimitSeconds": $timeLimit,
                "fogEnabled": true,
                "fogRadius": ${options.effectiveFogRadius},
                "radarEnabled": true,
                "requiredKeyTags": $requiredTagsJson,
                "orderedKeyTags": $orderedTagsJson,
                "scoreMultiplier": ${options.scoreMultiplier},
                "starTimeBonusSeconds": ${options.starTimeBonusSeconds},
                "sealedKeyOrder": ${options.keyMode == PacMazeMazeKeyMode.SEALED},
                "hintPelletsEnabled": ${options.hintPelletsEnabled},
                "intelPointsMax": ${options.intelPointsMax},
                "huntEscalation": ${options.huntEscalation},
                "ghostSignatureIds": $signatureIdsJsonFixed,
                "mutatorId": "${options.mutator.id}",
                "variantId": "${options.variant.id}",
                "mirrorDynamicWalls": ${options.mirrorDynamicWalls},
                "radarCooldownMultiplier": ${options.radarCooldownMultiplier},
                "revealExitOnLastKey": ${options.revealExitOnLastKey},
                "dynamicWallSpeedMul": ${options.dynamicWallSpeedMul}
              },
              "starCriteria": {
                "twoStarMinScore": 120,
                "threeStarMinScore": 280,
                "threeStarMaxSeconds": $starMaxSec,
                "threeStarNoDeath": true,
                "threeStarRequiredTags": $requiredTagsJson
              }
            }
        """.trimIndent()
    }

    private fun buildItemSpawners(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        keyCells: List<Pair<Int, Int>>,
        deadEnds: List<Pair<Int, Int>>,
        options: PacMazeMazeRunOptions,
    ): String {
        val pool = when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> listOf(
                PacMazeItemKind.FROST,
                PacMazeItemKind.SHIELD,
            )
            PacMazeMazeDifficulty.STANDARD -> listOf(
                PacMazeItemKind.FROST,
                PacMazeItemKind.SPEED,
                PacMazeItemKind.SHIELD,
                PacMazeItemKind.MAGNET,
            )
            PacMazeMazeDifficulty.ABYSS -> listOf(
                PacMazeItemKind.FROST,
                PacMazeItemKind.SPEED,
                PacMazeItemKind.SHIELD,
                PacMazeItemKind.MAGNET,
                PacMazeItemKind.DOUBLE,
                PacMazeItemKind.CHARGE,
            )
        }
        val target = when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> 1
            PacMazeMazeDifficulty.STANDARD -> 2
            PacMazeMazeDifficulty.ABYSS -> 3
        }
        val walkable = mutableListOf<Pair<Int, Int>>()
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                if (cells[y][x]) walkable.add(x to y)
            }
        }
        val candidates = (deadEnds + keyCells + walkable.filter { branchingCount(cells, w, h, it.first, it.second) >= 3 })
            .distinct()
            .shuffled(rng)
            .take(target)
        return candidates.mapIndexed { index, (x, y) ->
            val kinds = pool.shuffled(rng).take(3).joinToString(prefix = "[", postfix = "]") { "\"${it.id}\"" }
            """{ "id": "maze_item_$index", "x": $x, "y": $y, "intervalTicks": 780, "pool": $kinds }"""
        }.joinToString(", ")
    }

    private fun pelletDensity(options: PacMazeMazeRunOptions): Float = when (options.difficulty) {
        PacMazeMazeDifficulty.SCOUT -> 0.38f
        PacMazeMazeDifficulty.STANDARD -> 0.58f
        PacMazeMazeDifficulty.ABYSS -> 0.72f
    }

    private fun loopCount(options: PacMazeMazeRunOptions): Int = when (options.difficulty) {
        PacMazeMazeDifficulty.SCOUT -> 0
        PacMazeMazeDifficulty.STANDARD -> 3
        PacMazeMazeDifficulty.ABYSS -> 5
    }

    private fun scatterCorridorPellets(
        grid: Array<CharArray>,
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        density: Float,
    ) {
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (!cells[y][x] || grid[y][x] != 'o') continue
                if (rng.nextFloat() < density) grid[y][x] = '.'
            }
        }
    }

    private fun placePowerPellets(
        grid: Array<CharArray>,
        sortedByDist: List<Map.Entry<Pair<Int, Int>, Int>>,
        exclude: Set<Pair<Int, Int>>,
        count: Int = 4,
    ) {
        val picks = sortedByDist
            .map { it.key }
            .filter { it !in exclude }
            .filterIndexed { index, _ -> index % (sortedByDist.size / count.coerceAtLeast(1)).coerceAtLeast(3) == 0 }
            .take(count)
        picks.forEach { (x, y) ->
            if (grid[y][x] == 'o' || grid[y][x] == '.') grid[y][x] = '*'
        }
    }

    private fun applyThemedWalls(
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        themeIndex: Int,
    ) {
        val themed = when (themeIndex) {
            0 -> 'b'
            1 -> 'w'
            2 -> 't'
            else -> '#'
        }
        if (themed == '#') return
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (grid[y][x] != '#') continue
                val touchesFloor = Direction.entries.any { dir ->
                    val (dx, dy) = dir.delta()
                    val nx = x + dx
                    val ny = y + dy
                    nx in 0 until w && ny in 0 until h && grid[ny][nx] != '#'
                }
                if (touchesFloor && rng.nextFloat() < 0.48f) {
                    grid[y][x] = themed
                }
            }
        }
    }

    private fun addCorridorLoops(
        cells: Array<BooleanArray>,
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        count: Int,
    ) {
        if (count <= 0) return
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                if (cells[y][x]) continue
                val horizontal = cells[y][x - 1] && cells[y][x + 1]
                val vertical = cells[y - 1][x] && cells[y + 1][x]
                if (horizontal || vertical) candidates.add(x to y)
            }
        }
        candidates.shuffled(rng).take(count).forEach { (x, y) ->
            cells[y][x] = true
            grid[y][x] = 'o'
        }
    }

    private fun placeMazeMechanisms(
        options: PacMazeMazeRunOptions,
        cells: Array<BooleanArray>,
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        sortedByDist: List<Map.Entry<Pair<Int, Int>, Int>>,
        exclude: List<Pair<Int, Int>>,
    ) {
        val excludeSet = exclude.toSet()
        val midCells = sortedByDist
            .drop(sortedByDist.size / 3)
            .take(sortedByDist.size / 3)
            .map { it.key }
            .filter { it !in excludeSet && cells[it.second][it.first] }

        when (options.difficulty) {
            PacMazeMazeDifficulty.SCOUT -> {
                placeTunnelTile(cells, grid, w, h, rng, excludeSet)
            }
            PacMazeMazeDifficulty.STANDARD -> {
                placeMechanismAt(midCells, grid, rng, excludeSet) { x, y -> grid[y][x] = 'G' }
                placeMechanismAt(midCells, grid, rng, excludeSet) { x, y -> grid[y][x] = '>' }
                placeLaserRow(grid, w, h)
                placeTunnelTile(cells, grid, w, h, rng, excludeSet)
            }
            PacMazeMazeDifficulty.ABYSS -> {
                repeat(2) { placeMechanismAt(midCells, grid, rng, excludeSet) { x, y -> grid[y][x] = 'G' } }
                repeat(2) { placeMechanismAt(midCells, grid, rng, excludeSet) { x, y -> grid[y][x] = '>' } }
                placeLaserRow(grid, w, h)
                placeLaserColumn(grid, w, h)
                repeat(2) { placeTunnelTile(cells, grid, w, h, rng, excludeSet) }
            }
        }
    }

    private fun placeMechanismAt(
        candidates: List<Pair<Int, Int>>,
        grid: Array<CharArray>,
        rng: PacMazeDeterministicRng,
        exclude: Set<Pair<Int, Int>>,
        place: (Int, Int) -> Unit,
    ) {
        val cell = candidates.shuffled(rng).firstOrNull { (x, y) ->
            (x to y) !in exclude && grid[y][x] in setOf('o', '.')
        } ?: return
        place(cell.first, cell.second)
    }

    private fun placeLaserRow(grid: Array<CharArray>, w: Int, h: Int) {
        val y = (h / 2).coerceIn(2, h - 3)
        val x = (w / 2).coerceIn(2, w - 3)
        if (grid[y][x] == 'o' || grid[y][x] == '.') grid[y][x] = 'H'
    }

    private fun placeLaserColumn(grid: Array<CharArray>, w: Int, h: Int) {
        val y = (h / 3).coerceIn(2, h - 3)
        val x = (w / 3).coerceIn(2, w - 3)
        if (grid[y][x] == 'o' || grid[y][x] == '.') grid[y][x] = 'I'
    }

    private fun placeTunnelTile(
        cells: Array<BooleanArray>,
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        exclude: Set<Pair<Int, Int>>,
    ) {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                if (!cells[y][x] || (x to y) in exclude) continue
                if (branchingCount(cells, w, h, x, y) == 2) candidates.add(x to y)
            }
        }
        val cell = candidates.shuffled(rng).firstOrNull() ?: return
        grid[cell.second][cell.first] = '-'
    }

    private fun placePortalPair(
        cells: Array<BooleanArray>,
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        markers: MutableList<String>,
        exclude: List<Pair<Int, Int>>,
    ) {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                if (!cells[y][x] || (x to y) in exclude) continue
                if (branchingCount(cells, w, h, x, y) >= 2) candidates.add(x to y)
            }
        }
        val picks = candidates.shuffled(rng).take(2)
        if (picks.size < 2) return
        picks.forEach { (x, y) ->
            grid[y][x] = '@'
            markers.add("""{ "type": "checkpoint", "x": $x, "y": $y, "label": "门", "tag": "LINK" }""")
        }
    }

    private fun buildGhostSpawns(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        options: PacMazeMazeRunOptions,
        rng: PacMazeDeterministicRng,
        startX: Int,
        startY: Int,
        signatures: List<PacMazeMazeGhostSignature>,
    ): String {
        if (options.effectiveGhostCount <= 0) return ""
        val dist = bfsDistances(cells, w, h, startX, startY)
        val farCells = dist.entries
            .filter { it.value >= 4 }
            .sortedByDescending { it.value }
            .map { it.key }
            .filter { (x, y) -> ghostSpawnViable(cells, w, h, x, y) }
        return (0 until options.effectiveGhostCount).mapIndexed { index, _ ->
            val cell = farCells.getOrElse(index) { w / 2 to h / 2 }
            val sig = signatures.getOrNull(index)
            val kind = sig?.ghostKind?.id ?: if (index == 0) "opportunist" else "flanker"
            """{ "x": ${cell.first}, "y": ${cell.second}, "kind": "$kind" }"""
        }.joinToString(", ")
    }

    private fun ghostSpawnViable(cells: Array<BooleanArray>, w: Int, h: Int, x: Int, y: Int): Boolean {
        if (x !in 1 until w - 1 || y !in 1 until h - 1 || !cells[y][x]) return false
        val exits = Direction.entries.count { dir ->
            val (dx, dy) = dir.delta()
            val nx = x + dx
            val ny = y + dy
            nx in 1 until w - 1 && ny in 1 until h - 1 && cells[ny][nx]
        }
        return exits >= 2
    }

    private fun pickKeyCells(
        sortedByDist: List<Map.Entry<Pair<Int, Int>, Int>>,
        count: Int,
        exclude: Set<Pair<Int, Int>>,
    ): List<Pair<Int, Int>> {
        val picks = mutableListOf<Pair<Int, Int>>()
        val step = (sortedByDist.size / (count + 1)).coerceAtLeast(2)
        var i = step
        while (picks.size < count && i < sortedByDist.size) {
            val cell = sortedByDist[i].key
            if (cell !in exclude && cell !in picks) picks.add(cell)
            i += step
        }
        return picks
    }

    private fun findDeadEnds(cells: Array<BooleanArray>, w: Int, h: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (!cells[y][x]) continue
                val neighbors = Direction.entries.count { dir ->
                    val (dx, dy) = dir.delta()
                    val nx = x + dx
                    val ny = y + dy
                    nx in 1 until w - 1 && ny in 1 until h - 1 && cells[ny][nx]
                }
                if (neighbors == 1) result.add(x to y)
            }
        }
        return result
    }

    private fun pickHintPellets(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        startX: Int,
        startY: Int,
        exitX: Int,
        exitY: Int,
        keyCells: List<Pair<Int, Int>>,
        rng: PacMazeDeterministicRng,
    ): List<Pair<Int, Int>> {
        val path = bfsPath(cells, w, h, startX, startY, exitX, exitY) ?: return emptyList()
        val junctions = path.filterIndexed { index, cell ->
            index > 0 && index < path.lastIndex && branchingCount(cells, w, h, cell.first, cell.second) >= 3
        }
        return junctions.shuffled(rng).take(3)
    }

    private fun branchingCount(cells: Array<BooleanArray>, w: Int, h: Int, x: Int, y: Int): Int =
        Direction.entries.count { dir ->
            val (dx, dy) = dir.delta()
            val nx = x + dx
            val ny = y + dy
            nx in 1 until w - 1 && ny in 1 until h - 1 && cells[ny][nx]
        }

    private fun placeDynamicWalls(
        cells: Array<BooleanArray>,
        grid: Array<CharArray>,
        w: Int,
        h: Int,
        rng: PacMazeDeterministicRng,
        exclude: List<Pair<Int, Int>>,
    ) {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in 2 until h - 2) {
            for (x in 2 until w - 2) {
                if (!cells[y][x]) continue
                if ((x to y) in exclude) continue
                if (branchingCount(cells, w, h, x, y) != 2) continue
                candidates.add(x to y)
            }
        }
        candidates.shuffled(rng).take((candidates.size * 0.08f).toInt().coerceIn(2, 8)).forEach { (x, y) ->
            grid[y][x] = '&'
        }
    }

    private fun bfsDistances(cells: Array<BooleanArray>, w: Int, h: Int, sx: Int, sy: Int): Map<Pair<Int, Int>, Int> {
        val dist = mutableMapOf<Pair<Int, Int>, Int>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(sx to sy)
        dist[sx to sy] = 0
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            val d = dist[x to y] ?: continue
            Direction.entries.forEach { dir ->
                val (dx, dy) = dir.delta()
                val nx = x + dx
                val ny = y + dy
                if (nx !in 1 until w - 1 || ny !in 1 until h - 1) return@forEach
                if (!cells[ny][nx]) return@forEach
                val key = nx to ny
                if (key !in dist) {
                    dist[key] = d + 1
                    queue.add(key)
                }
            }
        }
        return dist
    }

    private fun bfsPath(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        sx: Int,
        sy: Int,
        ex: Int,
        ey: Int,
    ): List<Pair<Int, Int>>? {
        val prev = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>?>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(sx to sy)
        prev[sx to sy] = null
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (cur == ex to ey) {
                val path = mutableListOf<Pair<Int, Int>>()
                var node: Pair<Int, Int>? = cur
                while (node != null) {
                    path.add(node)
                    node = prev[node]
                }
                return path.reversed()
            }
            val (x, y) = cur
            Direction.entries.forEach { dir ->
                val (dx, dy) = dir.delta()
                val nx = x + dx
                val ny = y + dy
                if (nx !in 1 until w - 1 || ny !in 1 until h - 1) return@forEach
                if (!cells[ny][nx]) return@forEach
                val key = nx to ny
                if (key !in prev) {
                    prev[key] = cur
                    queue.add(key)
                }
            }
        }
        return null
    }

    private fun carve(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
        rng: PacMazeDeterministicRng,
    ) {
        cells[y][x] = true
        for (dir in shuffleDirections(rng)) {
            val (dx, dy) = dir.delta()
            val nx = x + dx * 2
            val ny = y + dy * 2
            if (nx in 1 until w - 1 && ny in 1 until h - 1 && !cells[ny][nx]) {
                cells[y + dy][x + dx] = true
                carve(cells, w, h, nx, ny, rng)
            }
        }
    }

    private fun shuffleDirections(rng: PacMazeDeterministicRng): List<Direction> {
        val list = Direction.entries.toMutableList()
        for (i in list.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }

    private fun <T> List<T>.shuffled(rng: PacMazeDeterministicRng): List<T> {
        val list = toMutableList()
        for (i in list.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
