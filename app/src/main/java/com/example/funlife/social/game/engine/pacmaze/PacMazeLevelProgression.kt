package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.hypot
import kotlin.math.max
/** 闯关模式关卡进度：难度递增、道具工厂与道具强度缩放。 */
object PacMazeLevelProgression {

    const val TOTAL_LEVELS = 23

    fun ghostSpeedFloor(levelId: Int): Float {
        val base = 0.42f + (levelId - 1) * 0.035f
        val extremeBoost = if (levelId > 13) (levelId - 13) * 0.018f else 0f
        return base + extremeBoost
    }

    fun aiAggressionFloor(levelId: Int): Float {
        val base = 0.45f + (levelId - 1) * 0.032f
        val extremeBoost = if (levelId > 13) (levelId - 13) * 0.022f else 0f
        return (base + extremeBoost).coerceAtMost(1f)
    }

    /** 闯关模式以关卡序号为难度基准；程序生成关（id<=0）使用 JSON 配置。 */
    fun resolveDifficulty(
        levelId: Int,
        authoredSpeed: Float,
        authoredAggression: Float,
    ): Pair<Float, Float> =
        if (levelId <= 0) {
            authoredSpeed.coerceIn(0.55f, 1.25f) to authoredAggression.coerceIn(0.35f, 1f)
        } else {
            ghostSpeedFloor(levelId) to aiAggressionFloor(levelId)
        }

    fun spawnerCount(levelId: Int): Int = when {
        levelId <= 2 -> 2
        levelId <= 5 -> 3
        levelId <= 9 -> 4
        levelId <= 18 -> 5
        else -> 6
    }

    fun spawnerIntervalTicks(levelId: Int): Int =
        (900 - (levelId - 1) * 28).coerceIn(360, 900)

    fun itemPool(levelId: Int, spawnerIndex: Int): List<PacMazeItemKind> {
        val tierPools = when {
            levelId <= 2 -> listOf(
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SHIELD),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.MAGNET),
            )
            levelId <= 4 -> listOf(
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SPEED),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.SPEED),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SHIELD, PacMazeItemKind.SPEED),
            )
            levelId <= 7 -> listOf(
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.SHIELD),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SPEED),
                listOf(PacMazeItemKind.SPEED, PacMazeItemKind.FROST),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.MAGNET, PacMazeItemKind.FROST),
            )
            levelId <= 10 -> listOf(
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.DOUBLE),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.CHARGE),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.SPEED),
                listOf(PacMazeItemKind.DOUBLE, PacMazeItemKind.FROST, PacMazeItemKind.CHARGE),
            )
            levelId <= 18 -> listOf(
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.CHARGE, PacMazeItemKind.DOUBLE),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SHIELD, PacMazeItemKind.CHARGE),
                listOf(PacMazeItemKind.SPEED, PacMazeItemKind.DOUBLE, PacMazeItemKind.FROST),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.CHARGE, PacMazeItemKind.MAGNET),
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.DOUBLE, PacMazeItemKind.SPEED, PacMazeItemKind.CHARGE),
            )
            else -> listOf(
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.CHARGE, PacMazeItemKind.DOUBLE, PacMazeItemKind.SHIELD),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.SHIELD, PacMazeItemKind.CHARGE, PacMazeItemKind.SPEED),
                listOf(PacMazeItemKind.SPEED, PacMazeItemKind.DOUBLE, PacMazeItemKind.FROST, PacMazeItemKind.MAGNET),
                listOf(PacMazeItemKind.SHIELD, PacMazeItemKind.CHARGE, PacMazeItemKind.MAGNET, PacMazeItemKind.DOUBLE),
                listOf(PacMazeItemKind.FROST, PacMazeItemKind.DOUBLE, PacMazeItemKind.SPEED, PacMazeItemKind.CHARGE),
                listOf(PacMazeItemKind.MAGNET, PacMazeItemKind.FROST, PacMazeItemKind.SHIELD, PacMazeItemKind.CHARGE, PacMazeItemKind.DOUBLE),
            )
        }
        return tierPools[spawnerIndex % tierPools.size]
    }

    fun itemDurationMultiplier(levelId: Int): Float =
        1f + (levelId - 1) * 0.018f

    fun itemPickupScore(levelId: Int): Int =
        PacMazeItemConstants.ITEM_PICKUP_SCORE + levelId * 4

    fun magnetRadiusCells(levelId: Int): Float =
        PacMazeItemConstants.MAGNET_RADIUS_CELLS + (levelId - 1) * 0.07f

    fun shieldGrant(levelId: Int): Int =
        PacMazeItemConstants.SHIELD_STACK + if (levelId >= 10) 1 else 0

    fun chargeGrant(levelId: Int): Int =
        PacMazeItemConstants.CHARGE_BONUS + if (levelId >= 8) 1 else 0

    fun difficultyLabel(levelId: Int): String = when {
        levelId <= 3 -> "简单"
        levelId <= 7 -> "普通"
        levelId <= 10 -> "困难"
        levelId <= 16 -> "挑战"
        levelId <= 20 -> "极限"
        else -> "地狱"
    }

    fun starCriteria(levelId: Int): PacMazeStarCriteria {
        val base = 1000 + levelId * 120
        return PacMazeStarCriteria(
            twoStarMinScore = base + 400,
            threeStarMinScore = base + 1200,
            threeStarMaxSeconds = 100 + levelId * 8,
            threeStarNoDeath = levelId >= 9,
        )
    }

    fun enrichItemSpawners(
        levelId: Int,
        width: Int,
        height: Int,
        tiles: IntArray,
        pacSpawn: Pair<Int, Int>,
        ghostSpawns: List<PacMazeGhostSpawnDef>,
        existing: List<PacMazeItemSpawnerDef>,
        linkPortalTiles: Set<Pair<Int, Int>> = emptySet(),
    ): List<PacMazeItemSpawnerDef> {
        if (levelId !in 1..TOTAL_LEVELS) return existing
        val upgraded = existing.mapIndexed { index, def ->
            def.copy(
                intervalTicks = minInterval(def.intervalTicks, spawnerIntervalTicks(levelId)),
                pool = def.pool.ifEmpty { itemPool(levelId, index) },
            )
        }
        val sanitized = upgraded.filterNot { def ->
            isBlockedItemSpawnerTile(tiles, width, def.x, def.y, linkPortalTiles)
        }
        val target = spawnerCount(levelId)
        val occupied = buildSet {
            add(pacSpawn)
            ghostSpawns.forEach { add(it.position) }
            sanitized.forEach { add(it.x to it.y) }
            linkPortalTiles.forEach { add(it) }
        }
        val need = (target - sanitized.size).coerceAtLeast(0)
        val autoPositions = findSpawnerPositions(
            width = width,
            height = height,
            tiles = tiles,
            pacSpawn = pacSpawn,
            occupied = occupied,
            count = need,
            blockedTiles = linkPortalTiles,
        )
        val autoSpawners = autoPositions.mapIndexed { offset, (x, y) ->
            val index = sanitized.size + offset
            PacMazeItemSpawnerDef(
                id = "auto_sp_${levelId}_$index",
                x = x,
                y = y,
                intervalTicks = spawnerIntervalTicks(levelId),
                pool = itemPool(levelId, index),
            )
        }
        return sanitized + autoSpawners
    }

    private fun minInterval(authored: Int, levelInterval: Int): Int =
        if (authored == PacMazeItemConstants.SPAWNER_INTERVAL_TICKS) {
            levelInterval
        } else {
            authored
        }

    private fun findSpawnerPositions(
        width: Int,
        height: Int,
        tiles: IntArray,
        pacSpawn: Pair<Int, Int>,
        occupied: Set<Pair<Int, Int>>,
        count: Int,
        blockedTiles: Set<Pair<Int, Int>> = emptySet(),
    ): List<Pair<Int, Int>> {
        if (count <= 0) return emptyList()
        val cx = width / 2f
        val cy = height / 2f
        val quadrants = listOf(
            { x: Int, y: Int -> x < cx && y < cy },
            { x: Int, y: Int -> x >= cx && y < cy },
            { x: Int, y: Int -> x < cx && y >= cy },
            { x: Int, y: Int -> x >= cx && y >= cy },
            { x: Int, y: Int -> x < cx && y == cy.toInt() },
        )
        val picked = mutableListOf<Pair<Int, Int>>()
        val used = occupied.toMutableSet()

        quadrants.forEach { inQuad ->
            if (picked.size >= count) return@forEach
            val candidates = mutableListOf<Triple<Int, Int, Float>>()
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    if (!inQuad(x, y)) continue
                    if ((x to y) in used) continue
                    if ((x to y) in blockedTiles) continue
                    if (!isFactoryTile(tiles, width, x, y, blockedTiles)) continue
                    val distPac = hypot(
                        (x - pacSpawn.first).toFloat(),
                        (y - pacSpawn.second).toFloat(),
                    )
                    if (distPac < 3.5f) continue
                    val openness = countOpenNeighbors(tiles, width, height, x, y)
                    val score = distPac * 1.4f + openness * 0.6f
                    candidates.add(Triple(x, y, score))
                }
            }
            candidates.maxByOrNull { it.third }?.let { (x, y, _) ->
                picked.add(x to y)
                used.add(x to y)
            }
        }

        if (picked.size < count) {
            val fallback = mutableListOf<Triple<Int, Int, Float>>()
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    if ((x to y) in used) continue
                    if ((x to y) in blockedTiles) continue
                    if (!isFactoryTile(tiles, width, x, y, blockedTiles)) continue
                    val distPac = hypot(
                        (x - pacSpawn.first).toFloat(),
                        (y - pacSpawn.second).toFloat(),
                    )
                    if (distPac < 2.5f) continue
                    fallback.add(Triple(x, y, distPac))
                }
            }
            fallback.sortedByDescending { it.third }.forEach { (x, y, _) ->
                if (picked.size >= count) return@forEach
                if ((x to y) !in used) {
                    picked.add(x to y)
                    used.add(x to y)
                }
            }
        }
        return picked
    }

    private fun isFactoryTile(
        tiles: IntArray,
        width: Int,
        x: Int,
        y: Int,
        blockedTiles: Set<Pair<Int, Int>> = emptySet(),
    ): Boolean {
        if ((x to y) in blockedTiles) return false
        val tile = TileType.entries.firstOrNull { it.code == tiles[y * width + x] } ?: return false
        if (isPortalOrGateTile(tile)) return false
        if (!tile.isWalkableFloor()) return false
        return countOpenNeighbors(tiles, width, tiles.size / width, x, y) >= 2
    }

    fun isPortalOrGateTile(tile: TileType): Boolean =
        tile == TileType.DOOR || tile == TileType.TUNNEL || tile == TileType.PORTAL

    fun isBlockedItemSpawnerTile(
        tiles: IntArray,
        width: Int,
        x: Int,
        y: Int,
        linkPortalTiles: Set<Pair<Int, Int>>,
    ): Boolean {
        if ((x to y) in linkPortalTiles) return true
        if (x !in 0 until width || y !in 0 until tiles.size / width) return true
        val tile = TileType.entries.firstOrNull { it.code == tiles[y * width + x] } ?: return true
        return isPortalOrGateTile(tile)
    }

    private fun countOpenNeighbors(
        tiles: IntArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ): Int {
        var open = 0
        for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
            val nx = x + dx
            val ny = y + dy
            if (nx !in 0 until width || ny !in 0 until height) continue
            val tile = TileType.entries.firstOrNull { it.code == tiles[ny * width + nx] }
            if (tile?.isWalkableFloor() == true) open++
        }
        return open
    }

    fun mapComplexityScore(width: Int, height: Int, hazardCount: Int, wallTiles: Int): Int =
        width * height + hazardCount * 120 + wallTiles * 2

    private val WALL_GRID_CHARS = setOf('#', 'b', 'w', 't')
    private val MECHANISM_GRID_CHARS = setOf('&', '=', 'G', '@')

    fun wallTilesInGrid(grid: List<String>): Int =
        grid.sumOf { row -> row.count { it in WALL_GRID_CHARS } }

    fun mechanismTilesInGrid(grid: List<String>): Int =
        grid.sumOf { row -> row.count { it in MECHANISM_GRID_CHARS } }

    fun computeLevelComplexity(levelId: Int, width: Int, height: Int, hazardCount: Int, grid: List<String>): Int {
        val walls = wallTilesInGrid(grid)
        val mechanisms = mechanismTilesInGrid(grid)
        return levelId * 60 + walls * 3 + hazardCount * 140 + mechanisms * 28 + (width + height) * 6
    }

    /** 每关结构复杂度下限（不直接用面积，避免小图关卡误判）。 */
    fun complexityFloor(levelId: Int): Int {
        val extreme = if (levelId > 13) (levelId - 13) * 90 else 0
        return 180 + levelId * 55 + levelId * levelId * 2 + extreme
    }
}
