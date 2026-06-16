package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeItemSystemTest {

    private val levelJson = """
        {
          "id": 99,
          "name": "item-test",
          "width": 7,
          "height": 7,
          "grid": [
            "#######",
            "#.....#",
            "#.$...#",
            "#.....#",
            "#..G..#",
            "#..P..#",
            "#######"
          ],
          "spawn": {
            "pac": [3, 5],
            "ghosts": [[3, 4]]
          },
          "itemSpawners": [
            { "id": "f1", "x": 2, "y": 2, "intervalTicks": 30, "pool": ["magnet"] }
          ],
          "difficulty": { "ghost_speed_mul": 0.3, "ai_aggression": 0.1 }
        }
    """.trimIndent()

    private val level = PacMazeMapLoader.parseLevelJson(levelJson)

    @Test
    fun parseLevel_loadsItemSpawnerFromGridAndJson() {
        assertTrue(level.itemSpawners.size >= 1)
        assertTrue(level.itemSpawners.any { it.x == 2 && it.y == 2 })
    }

    @Test
    fun buildInitialWorld_initializesSpawnerStates() {
        val world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 1L)
        assertTrue(world.itemSpawnerStates.size >= 1)
        assertTrue(world.floorItems.isEmpty())
    }

    @Test
    fun spawner_producesFloorItemAfterCooldown() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 42L)
        repeat(120) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        assertTrue(world.floorItems.isNotEmpty())
    }

    @Test
    fun pickup_appliesMagnetEffect() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 1L)
        world = world.copy(
            floorItems = listOf(
                PacMazeFloorItem(id = "t1", kind = PacMazeItemKind.MAGNET, x = 3, y = 5),
            ),
            entities = world.entities.map {
                if (it.role == "pac") it.copy(x = 3f, y = 5f) else it
            },
        )
        val after = PacMazeItems.tryPickup(world, 3f, 5f)
        assertTrue(after.magnetTicksLeft > 0)
        assertTrue(after.floorItems.isEmpty())
    }

    @Test
    fun shield_blocksGhostCollision() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 2L)
        val livesBefore = world.lives
        world = world.copy(
            ghostReleaseTicksLeft = 0,
            shieldCharges = 1,
            entities = world.entities.map {
                when (it.role) {
                    "pac" -> it.copy(x = 3f, y = 4f)
                    "ghost" -> it.copy(x = 3f, y = 4f, ghostMode = GhostMode.CHASE)
                    else -> it
                }
            },
        )
        val after = PacMazeSimulation.tick(world, null, level)
        assertEquals(livesBefore, after.lives)
        assertEquals(0, after.shieldCharges)
    }

    @Test
    fun frost_preventsGhostMovement() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 3L)
        world = world.copy(
            ghostReleaseTicksLeft = 0,
            frostTicksLeft = 120,
            entities = world.entities.map {
                if (it.role == "ghost") it.copy(x = 1f, y = 4f, direction = Direction.RIGHT) else it
            },
        )
        val ghostBefore = world.entities.first { it.role == "ghost" }
        val after = PacMazeSimulation.tick(world, null, level)
        val ghostAfter = after.entities.first { it.role == "ghost" }
        assertEquals(ghostBefore.x, ghostAfter.x, 0.001f)
        assertEquals(ghostBefore.y, ghostAfter.y, 0.001f)
    }

    @Test
    fun magnet_pullsNearbyPellets() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 4L)
        val idx = 5 * world.width + 4
        val tiles = IntArray(world.tiles.size) { TileType.EMPTY.code }
        tiles[idx] = TileType.PELLET.code
        world = world.copy(
            tiles = tiles,
            magnetTicksLeft = 60,
            pelletsRemaining = 1,
            entities = world.entities.map {
                if (it.role == "pac") it.copy(x = 3f, y = 5f) else it
            },
        )
        val afterStart = PacMazeItems.tickMagnet(world)
        assertEquals(TileType.EMPTY.code, afterStart.tiles[idx])
        assertEquals(1, afterStart.magnetPulls.size)
        assertEquals(1, afterStart.pelletsRemaining)

        var flying = afterStart
        repeat(40) {
            flying = PacMazeItems.tickMagnet(flying)
        }
        assertTrue(flying.magnetPulls.isEmpty())
        assertEquals(0, flying.pelletsRemaining)
    }

    @Test
    fun spawner_doesNotRespawnUntilPickup() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 42L)
        repeat(120) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        val firstCount = world.floorItems.size
        assertTrue(firstCount >= 1)
        val firstId = world.floorItems.first().id

        repeat(300) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        assertEquals(firstCount, world.floorItems.size)
        assertEquals(firstId, world.floorItems.first().id)
    }

    @Test
    fun doubleScore_doublesPelletPoints() {
        val world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 5L)
            .copy(scoreBoostTicksLeft = 100)
        assertEquals(PacMazeConstants.PELLET_SCORE * 2, PacMazeItems.pelletScore(world))
    }

    @Test
    fun magnet_pullsPowerPellets_andAppliesPowerEffect() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 6L)
        val idx = 5 * world.width + 4
        val tiles = IntArray(world.tiles.size) { TileType.EMPTY.code }
        tiles[idx] = TileType.POWER.code
        world = world.copy(
            tiles = tiles,
            magnetTicksLeft = 60,
            pelletsRemaining = 1,
            entities = world.entities.map {
                if (it.role == "pac") it.copy(x = 3f, y = 5f) else it
            },
        )
        val afterStart = PacMazeItems.tickMagnet(world)
        assertEquals(TileType.EMPTY.code, afterStart.tiles[idx])
        assertEquals(1, afterStart.magnetPulls.size)
        assertTrue(afterStart.magnetPulls.first().isPower)

        var flying = afterStart
        repeat(40) {
            flying = PacMazeItems.tickMagnet(flying)
        }
        assertTrue(flying.magnetPulls.isEmpty())
        assertTrue(flying.powerTicksLeft > 0)
        assertTrue(flying.attackCharges > 0)
    }

    @Test
    fun projectileHit_stunsGhostThenRecovers() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 8L)
        val ghost = world.entities.first { it.role == "ghost" }
        world = world.copy(
            projectiles = listOf(
                PacMazeProjectile(id = "p1", x = ghost.x, y = ghost.y, direction = Direction.RIGHT),
            ),
            entities = world.entities.map {
                if (it.role == "ghost") it.copy(x = 3f, y = 4f, ghostMode = GhostMode.CHASE) else it
            },
        )
        val afterHit = PacMazeCombat.tickProjectiles(world)
        val stunned = afterHit.entities.first { it.role == "ghost" }
        assertTrue(stunned.hitStunTicksLeft > 0)
        assertEquals(GhostMode.CHASE, stunned.ghostMode)

        var recovering = afterHit
        repeat(PacMazeConstants.GHOST_HIT_STUN_TICKS + 2) {
            recovering = PacMazeSimulation.tick(recovering, null, level)
        }
        val recovered = recovering.entities.first { it.role == "ghost" }
        assertEquals(0, recovered.hitStunTicksLeft)
        assertEquals(GhostMode.CHASE, recovered.ghostMode)
    }
}
