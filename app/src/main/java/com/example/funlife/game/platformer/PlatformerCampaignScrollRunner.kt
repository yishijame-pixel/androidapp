package com.example.funlife.game.platformer



import android.content.Context

import com.example.funlife.game.platformer.PlatformerSegmentLibrary.SegmentSpec

import kotlin.math.max



/**

 * 主线滚动缓冲：复用无尽跑酷的片段左移/追加，带 Checkpoint + 终点。

 * 玩家正常操控（非自动奔跑）；相机随玩家。

 */

object PlatformerCampaignScrollRunner {



    private val segW get() = PlatformerSegmentLibrary.SEGMENT_W

    private val shiftTrigger get() = PlatformerEndlessRunner.SHIFT_TRIGGER_TILES



    fun buildInitial(

        context: Context,

        level: PlatformerLevelDef,

        script: List<SegmentSpec>,

        characterId: PlatformerCharacterId,

    ): PlatformerWorld {

        val buffer = PlatformerCampaignLengthSpec.INITIAL_BUFFER_SEGMENTS.coerceAtMost(script.size)

        val h = PLATFORMER_LEVEL_ROWS

        val g = h - 1

        PlatformerSegmentLevelFactory.prebakeStoryRooms(context, script, g)

        val width = buffer * segW + PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD

        val canvas = PlatformerMapCanvas(width, h)

        PlatformerSegmentLevelFactory.paintScript(context, canvas, g, script, 0, buffer)

        val def = PlatformerSegmentLevelFactory.bakeLevelDef(level, canvas, g, width)

        val base = PlatformerLevels.buildWorldFromRowsInternal(def, characterId)

        val checkpoints = buildInitialCheckpoints(base, level.checkpointEverySegments)

        return base.copy(

            campaignScrollMode = true,

            campaignScript = script,

            campaignScriptIndex = buffer,

            campaignTotalSegments = script.size,

            campaignTilesRun = 0,

            campaignCheckpoints = checkpoints,

            campaignLastCheckpointIndex = if (checkpoints.isNotEmpty()) 0 else -1,

            levelSpawnX = base.player.x,

            levelSpawnY = base.player.y,

            phase = PlatformerPhase.PLAYING,

            cameraX = 0f,

        )

    }



    fun afterPhysics(world: PlatformerWorld, dt: Float, viewWorldW: Float): PlatformerWorld {

        if (!world.campaignScrollMode || world.phase != PlatformerPhase.PLAYING) return world



        val tile = world.tileF

        val playerTile = (world.player.x / tile).toInt()

        val camTile = (world.cameraX / tile).toInt()

        val tilesAhead = world.width - camTile



        var next = maybeRecordCheckpoint(world, playerTile)



        if (playerTile > shiftTrigger && tilesAhead < world.width - shiftTrigger &&

            next.campaignScriptIndex < next.campaignTotalSegments

        ) {

            next = shiftAndAppend(next)

        }

        return next

    }



    fun respawnAtCheckpoint(world: PlatformerWorld): PlatformerWorld? {

        val idx = world.campaignLastCheckpointIndex

        if (idx < 0 || idx >= world.campaignCheckpoints.size) return null

        val cp = world.campaignCheckpoints[idx]

        val tile = world.tileF

        val localX = cp.spawnX - world.campaignTilesRun * tile

        val localY = cp.spawnY

        if (localX < tile || localX > (world.width - 2) * tile) return null

        val resetCam = cp.segmentIndex == 0 && world.campaignTilesRun == 0

        return PlatformerSkyChickSystem.resetOnRespawn(
            world.copy(

            player = world.player.copy(

                x = localX,

                y = localY,

                vx = 0f,

                vy = 0f,

                grounded = true,

            ),

            phase = PlatformerPhase.PLAYING,

            projectiles = emptyList(),

            cameraX = if (resetCam) 0f else world.cameraX,

            ),
        )

    }



    /** 非滚动关：回到关卡出生点（保留宝石/进度）。 */

    fun respawnAtLevelStart(world: PlatformerWorld): PlatformerWorld {

        val x = if (world.levelSpawnX > 0f) world.levelSpawnX else world.player.x

        val y = if (world.levelSpawnY > 0f) world.levelSpawnY else world.player.y

        return PlatformerSkyChickSystem.resetOnRespawn(
            world.copy(

            player = world.player.copy(

                x = x,

                y = y,

                vx = 0f,

                vy = 0f,

                grounded = true,

            ),

            phase = PlatformerPhase.PLAYING,

            projectiles = emptyList(),

            cameraX = 0f,

            ),
        )

    }



    private fun buildInitialCheckpoints(

        world: PlatformerWorld,

        everySegments: Int,

    ): List<PlatformerCampaignCheckpoint> {

        if (everySegments <= 0) return emptyList()

        return listOf(

            PlatformerCampaignCheckpoint(

                segmentIndex = 0,

                absoluteTile = 0,

                spawnX = world.player.x,

                spawnY = world.player.y,

            ),

        )

    }



    private fun maybeRecordCheckpoint(world: PlatformerWorld, playerTile: Int): PlatformerWorld {

        val every = world.level.checkpointEverySegments

        if (every <= 0 || (world.campaignScript.isNullOrEmpty() && world.campaignBakedSegments.isNullOrEmpty())) return world



        val absoluteTile = world.campaignTilesRun + playerTile

        val segIndex = absoluteTile / segW

        if (segIndex <= 0 || segIndex % every != 0) return world



        val existing = world.campaignCheckpoints.any { it.segmentIndex == segIndex }

        if (existing) return world



        val tile = world.tileF

        val cp = PlatformerCampaignCheckpoint(

            segmentIndex = segIndex,

            absoluteTile = absoluteTile,

            spawnX = world.campaignTilesRun * tile + world.player.x,

            spawnY = world.player.y,

        )

        val checkpoints = world.campaignCheckpoints + cp

        return world.copy(

            campaignCheckpoints = checkpoints,

            campaignLastCheckpointIndex = checkpoints.lastIndex,

        )

    }



    private fun shiftAndAppend(world: PlatformerWorld): PlatformerWorld {

        val shift = segW

        val w = world.width

        val h = world.height

        val tile = world.tileF

        val g = h - 1

        val script = world.campaignScript

        val baked = world.campaignBakedSegments

        if (script == null && baked == null) return world

        val segIndex = world.campaignScriptIndex

        if (baked != null) {

            if (segIndex >= baked.size) return world

        } else if (script != null && segIndex >= script.size) {

            return world

        }



        val shiftedCells = Array(world.cells.size) { PlatformerCell.AIR }

        for (y in 0 until h) {

            for (x in 0 until w - shift) {

                shiftedCells[y * w + x] = world.cells[y * w + (x + shift)]

            }

        }



        val canvas = PlatformerMapCanvas(w, h)

        canvas.importFromCells(shiftedCells, w, h)

        if (baked != null) {

            PlatformerSuperTuxScrollFactory.paintBakedSegment(canvas, w - shift, baked[segIndex])

        } else {

            PlatformerSegmentLevelFactory.paintSegment(

                context = null,

                canvas = canvas,

                groundY = g,

                startX = w - shift,

                spec = script!![segIndex],

                sectionIndex = segIndex,

            )

        }



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



        val visualSegs = world.campaignBakedVisualSegments
        var nextVisual = world.supertuxVisualTiles
        if (nextVisual != null && visualSegs != null && segIndex < visualSegs.size) {
            val shiftedVisual = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w - shift) {
                    shiftedVisual[y * w + x] = nextVisual[y * w + (x + shift)]
                }
            }
            PlatformerSuperTuxScrollFactory.paintVisualSegment(
                shiftedVisual,
                w,
                h,
                w - shift,
                visualSegs[segIndex],
            )
            nextVisual = shiftedVisual
        }

        return world.copy(

            cells = newCells,

            enemies = enemies + extraEnemies,

            traps = traps + extraTraps,

            projectiles = projectiles,

            gems = gems + newGemMarks,

            player = player,

            cameraX = cam,

            campaignTilesRun = world.campaignTilesRun + shift,

            campaignScriptIndex = segIndex + 1,

            supertuxVisualTiles = nextVisual,

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

}


