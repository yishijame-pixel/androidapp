package com.example.funlife.game.platformer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerCampaignValidatorTest {

    @Test
    fun allCampaignManifestsPassValidation() {
        val report = PlatformerLevelValidator.validateAll(PlatformerCampaignLevelCatalog.all)
        if (!report.ok) {
            val detail = report.issues.joinToString("\n") { "L${it.levelId} ${it.code}: ${it.message}" }
            error("Campaign validation failed:\n$detail")
        }
        assertEquals(52, report.levelCount)
        assertTrue(
            "Total tiles ${report.totalTargetTiles} should exceed enterprise minimum 50000",
            report.totalTargetTiles >= 50_000,
        )
    }

    @Test
    fun allSkyManifestsPassValidation() {
        val report = PlatformerLevelValidator.validateAll(PlatformerSkyLevelCatalog.all)
        if (!report.ok) {
            val detail = report.issues.joinToString("\n") { "L${it.levelId} ${it.code}: ${it.message}" }
            error("Sky validation failed:\n$detail")
        }
        assertEquals(10, report.levelCount)
    }

    @Test
    fun scrollCheckpointRespawnsAtSkySpawnNotGround() {
        val level = PlatformerSkyLevelCatalog.buildManifest(61)
        val script = level.campaignSegmentScript!!
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val buffer = PlatformerCampaignLengthSpec.INITIAL_BUFFER_SEGMENTS.coerceAtMost(script.size)
        val width = buffer * PlatformerSegmentLibrary.SEGMENT_W +
            PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD
        val canvas = PlatformerMapCanvas(width, h)
        PlatformerSegmentLevelFactory.paintScript(null, canvas, g, script, 0, buffer)
        val def = PlatformerSegmentLevelFactory.bakeLevelDef(level, canvas, g, width)
        val world = PlatformerLevels.buildWorldFromRowsInternal(def, PlatformerCharacterId.CHICK_PRO_MAX)
        val cpWorld = world.copy(
            campaignScrollMode = true,
            campaignScript = script,
            campaignScriptIndex = buffer,
            campaignTotalSegments = script.size,
            campaignCheckpoints = listOf(
                PlatformerCampaignCheckpoint(0, 0, world.player.x, world.player.y),
            ),
            campaignLastCheckpointIndex = 0,
            levelSpawnX = world.player.x,
            levelSpawnY = world.player.y,
        )
        val respawned = PlatformerCampaignScrollRunner.respawnAtCheckpoint(cpWorld)!!
        assertTrue(
            "Respawn Y ${respawned.player.y} should be above ground ${g * PLATFORMER_TILE_PX}",
            respawned.player.y < g * PLATFORMER_TILE_PX - PLATFORMER_TILE_PX,
        )
    }

    @Test
    fun skyLevel53SpawnsHighAndBakes() {
        val level = PlatformerSkyLevelCatalog.buildManifest(53)
        val script = level.campaignSegmentScript!!
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val width = script.size * PlatformerSegmentLibrary.SEGMENT_W +
            PlatformerSkyLengthSpec.SEGMENT_TAIL_PAD
        val canvas = PlatformerMapCanvas(width, h)
        PlatformerSegmentLevelFactory.paintScript(null, canvas, g, script, 0, script.size)
        val baked = PlatformerSegmentLevelFactory.bakeLevelDef(level, canvas, g, width)
        assertTrue(baked.rows.isNotEmpty())
        val spawnRow = baked.rows.indexOfFirst { it.contains('@') }
        assertTrue("Spawn should be above ground", spawnRow < g)
        assertTrue(baked.trapSpawns.isNotEmpty())
    }

    @Test
    fun lengthSpecCoversEightHourTarget() {
        val total = PlatformerCampaignLengthSpec.totalCampaignTiles()
        val minutes = total / (PlatformerCampaignLengthSpec.EFFECTIVE_TILES_PER_SECOND * 60f)
        assertTrue("Estimated campaign minutes $minutes should be >= 420", minutes >= 420f)
    }

    @Test
    fun segmentBakeProducesFullWidthRowsForLevel1() {
        val level = PlatformerCampaignLevelCatalog.buildManifest(1)
        val script = level.campaignSegmentScript!!
        val h = PLATFORMER_LEVEL_ROWS
        val g = h - 1
        val width = script.size * PlatformerSegmentLibrary.SEGMENT_W +
            PlatformerCampaignLengthSpec.SEGMENT_TAIL_PAD
        val canvas = PlatformerMapCanvas(width, h)
        PlatformerSegmentLevelFactory.paintScript(null, canvas, g, script, 0, script.size)
        val baked = PlatformerSegmentLevelFactory.bakeLevelDef(level, canvas, g, width)
        assertTrue(baked.rows.isNotEmpty())
        assertTrue(baked.rows.maxOf { it.length } >= 300)
        assertTrue(baked.enemySpawns.isNotEmpty())
        assertTrue(
            "Campaign level 1 should have many traps; got ${baked.trapSpawns.size}",
            baked.trapSpawns.size >= 10,
        )
    }

    @Test
    fun storyLevelsUseScrollAndNoStandaloneTmx() {
        val story = PlatformerCampaignLevelCatalog.all.filter { it.id in 17..22 }
        story.forEach { level ->
            assertTrue(level.useCampaignScroll)
            assertEquals(null, level.tmxAsset)
            assertTrue(level.campaignSegmentScript!!.any {
                it.kind == PlatformerSegmentLibrary.SegmentKind.STORY_ROOM
            })
        }
    }
}
