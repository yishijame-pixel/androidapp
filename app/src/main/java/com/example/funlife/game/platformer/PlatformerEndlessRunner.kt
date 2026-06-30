package com.example.funlife.game.platformer

import android.content.Context
import com.example.funlife.game.platformer.catalog.PlatformerEnemyCatalog
import kotlin.math.max
import kotlin.random.Random

/**
 * 横版无尽跑酷：片段滚动 + 自动卷轴 + 生物群系轮换。
 */
object PlatformerEndlessRunner {

    const val BASE_SCROLL_SPEED = 175f
    const val SCROLL_ACCEL_PER_TILE = 0.42f
    const val MAX_SCROLL_SPEED = 340f
    const val BUFFER_SEGMENTS = 8
    const val SHIFT_TRIGGER_TILES = 2 * PlatformerSegmentLibrary.SEGMENT_W

    var bestTilesRun: Int = 0

    fun buildInitial(
        context: Context,
        characterId: PlatformerCharacterId,
        seed: Long = Random.nextLong(),
    ): PlatformerWorld {
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val segW = PlatformerSegmentLibrary.SEGMENT_W
        val width = segW * BUFFER_SEGMENTS
        val m = PlatformerMapCanvas(width, h)
        repeat(BUFFER_SEGMENTS) { i ->
            val biome = PlatformerEndlessBiomes.biomeForSegment(i)
            val spec = PlatformerSegmentLibrary.pickEndlessSpec(i, seed, tilesRun = 0, biome = biome)
            PlatformerSegmentLibrary.paint(m, g, i * segW, spec, i, endlessFloor = true)
        }
        var level = PlatformerLevelEnhancer.finalize(
            PlatformerLevelDef(
                id = 0,
                title = "无尽跑酷",
                subtitle = "Endless · 翠野平原",
                theme = PlatformerTheme.GRASS,
                tilesetPack = PlatformerTilesetPack.GOODLY,
                skyTop = 0xFF87CEEB,
                skyBottom = 0xFFB0E0FF,
                parallaxHill = 0xFF5DAD5D,
                rows = m.toRows(),
                seriesId = "endless",
                seriesOrder = 1,
            ),
            m, g, width,
        )
        level = PlatformerEndlessBiomes.applyToLevel(level, 0)
        val world = PlatformerLevels.buildWorld(context, level, characterId)
        return world.copy(
            endlessMode = true,
            endlessSeed = seed,
            endlessSegmentIndex = BUFFER_SEGMENTS,
            endlessTilesRun = 0,
            endlessScrollSpeed = BASE_SCROLL_SPEED,
            endlessBiomeIndex = PlatformerEndlessBiomes.biomeIndexForSegment(0),
            phase = PlatformerPhase.PLAYING,
            cameraX = 0f,
        )
    }

    fun afterPhysics(world: PlatformerWorld, dt: Float, viewWorldW: Float): PlatformerWorld {
        if (!world.endlessMode || world.phase != PlatformerPhase.PLAYING) return world

        val tile = world.tileF
        val scroll = world.endlessScrollSpeed.coerceAtMost(MAX_SCROLL_SPEED)
        var cam = world.cameraX + scroll * dt
        var player = world.player

        val leftKill = cam + tile * 0.8f
        if (player.x < leftKill) {
            return finalizeGameOver(world)
        }

        val minScreenX = viewWorldW * 0.22f
        val screenX = player.x - cam
        if (screenX < minScreenX) {
            player = player.copy(x = cam + minScreenX)
        }

        val playerTile = (player.x / tile).toInt()
        val camTile = (cam / tile).toInt()
        val tilesAhead = world.width - camTile
        var next = world.copy(
            player = player,
            cameraX = cam,
            endlessScrollSpeed = minOf(
                BASE_SCROLL_SPEED + world.endlessTilesRun * SCROLL_ACCEL_PER_TILE,
                MAX_SCROLL_SPEED,
            ),
        )

        if (playerTile > SHIFT_TRIGGER_TILES && tilesAhead < world.width - SHIFT_TRIGGER_TILES) {
            next = shiftAndAppend(next)
        }
        return applyActiveBiome(next)
    }

    /** 按当前跑动位置切换天空/地砖群系（约每 112 格一切）。 */
    private fun applyActiveBiome(world: PlatformerWorld): PlatformerWorld {
        val seg = activeSegmentIndex(world)
        val bioIdx = PlatformerEndlessBiomes.biomeIndexForSegment(seg)
        if (bioIdx == world.endlessBiomeIndex) return world
        return world.copy(
            endlessBiomeIndex = bioIdx,
            level = PlatformerEndlessBiomes.applyToLevel(world.level, seg),
        )
    }

    private fun activeSegmentIndex(world: PlatformerWorld): Int {
        val tile = world.tileF
        val absoluteTile = world.endlessTilesRun + (world.player.x / tile).toInt()
        return absoluteTile / PlatformerSegmentLibrary.SEGMENT_W
    }

    private fun shiftAndAppend(world: PlatformerWorld): PlatformerWorld {
        val shift = PlatformerSegmentLibrary.SEGMENT_W
        val w = world.width
        val h = world.height
        val tile = world.tileF
        val g = h - 1

        val shiftedCells = Array(world.cells.size) { PlatformerCell.AIR }
        for (y in 0 until h) {
            for (x in 0 until w - shift) {
                shiftedCells[y * w + x] = world.cells[y * w + (x + shift)]
            }
        }

        val canvas = PlatformerMapCanvas(w, h)
        canvas.importFromCells(shiftedCells, w, h)
        val segIndex = world.endlessSegmentIndex
        val biome = PlatformerEndlessBiomes.biomeForSegment(segIndex)
        val spec = PlatformerSegmentLibrary.pickEndlessSpec(
            segIndex, world.endlessSeed, tilesRun = world.endlessTilesRun, biome = biome,
        )
        PlatformerSegmentLibrary.paint(canvas, g, w - shift, spec, segIndex, endlessFloor = true)

        val newCells = rowsToCells(canvas.toRows(), w, h)
        val shiftPx = shift * tile
        var nextEnemyId = (world.enemies.maxOfOrNull { it.id } ?: -1) + 1
        var nextTrapId = (world.traps.maxOfOrNull { it.id } ?: -1) + 1

        val enemies = world.enemies
            .map { e -> e.copy(x = e.x - shiftPx) }
            .filter { it.x > -tile * 2 }
        val traps = world.traps
            .map { t -> t.copy(x = t.x - shiftPx) }
            .filter { it.x > -tile * 2 }
        val projectiles = world.projectiles
            .map { p -> p.copy(x = p.x - shiftPx) }
            .filter { it.x > -tile * 2 }
        val gems = world.gems
            .map { gem ->
                val nx = gem.x - shiftPx
                if (gem.collected || nx < -tile) gem.copy(collected = true)
                else gem.copy(x = nx)
            }
            .filter { !it.collected }

        val extraEnemies = canvas.enemySpawns()
            .filter { spawn -> spawn.tileX >= w - shift }
            .map { spawn ->
                val shifted = spawn.copy(tileX = spawn.tileX - shift)
                PlatformerEnemySystem.spawnSingle(world, shifted, nextEnemyId++)
            }
        val biomeIdx = PlatformerEndlessBiomes.biomeIndexForSegment(segIndex)
        val catalogEnemyId = PlatformerEnemyCatalog.endlessCatalogEnemyForBiome(biomeIdx)
        val catalogSpawn = if (catalogEnemyId != null && biomeIdx >= 14 && segIndex % 2 == 0) {
            val spawn = PlatformerEnemySpawn(
                tileX = (w - shift + 10).coerceAtMost(w - 2),
                tileY = g - 1,
                type = PlatformerEnemyCatalog.fallbackType(catalogEnemyId),
                patrolTiles = 4,
                catalogId = catalogEnemyId,
            )
            listOf(PlatformerEnemySystem.spawnSingle(world, spawn, nextEnemyId++))
        } else {
            emptyList()
        }
        val extraTraps = canvas.trapSpawns()
            .filter { spawn -> spawn.tileX >= w - shift }
            .map { spawn ->
                val shifted = spawn.copy(tileX = spawn.tileX - shift)
                PlatformerTrapSystem.spawnSingle(shifted, nextTrapId++, world.tilePx)
            }

        val newGemMarks = mutableListOf<PlatformerGem>()
        for (y in 0 until h) {
            for (x in w - shift until w) {
                if (newCells[y * w + x] == PlatformerCell.GEM) {
                    newGemMarks += PlatformerGem(
                        x = x * tile + tile / 2f,
                        y = y * tile + tile / 2f,
                    )
                    newCells[y * w + x] = PlatformerCell.AIR
                }
            }
        }

        val player = world.player.copy(x = world.player.x - shiftPx)
        val cam = max(0f, world.cameraX - shiftPx)

        return world.copy(
            cells = newCells,
            enemies = enemies + extraEnemies + catalogSpawn,
            traps = traps + extraTraps,
            projectiles = projectiles,
            gems = gems + newGemMarks,
            player = player,
            cameraX = cam,
            endlessTilesRun = world.endlessTilesRun + shift,
            endlessSegmentIndex = segIndex + 1,
        )
    }

    private fun rowsToCells(rows: List<String>, width: Int, height: Int): Array<PlatformerCell> {
        val cells = Array(height * width) { PlatformerCell.AIR }
        rows.forEachIndexed { y, row ->
            if (y >= height) return@forEachIndexed
            row.padEnd(width, '.').forEachIndexed { x, ch ->
                if (x < width) {
                    cells[y * width + x] = PlatformerCell.fromChar(ch) ?: PlatformerCell.AIR
                }
            }
        }
        return cells
    }

    fun finalizeGameOver(world: PlatformerWorld): PlatformerWorld {
        val run = world.endlessTilesRun + (world.player.x / world.tileF).toInt()
        if (run > bestTilesRun) bestTilesRun = run
        return world.copy(phase = PlatformerPhase.GAME_OVER)
    }

    fun onPlayerDeath(world: PlatformerWorld): PlatformerWorld {
        if (!world.endlessMode) return world
        return finalizeGameOver(world)
    }
}
