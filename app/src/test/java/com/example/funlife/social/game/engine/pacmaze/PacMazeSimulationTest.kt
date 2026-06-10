package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeSimulationTest {

    private val levelJson = """
        {
          "id": 1,
          "name": "test",
          "width": 5,
          "height": 5,
          "grid": [
            "#####",
            "#...#",
            "#.#.#",
            "#...#",
            "#####"
          ],
          "spawn": {
            "pac": [2, 3],
            "ghosts": [[2, 1]]
          },
          "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.2 }
        }
    """.trimIndent()

    private val level = PacMazeMapLoader.parseLevelJson(levelJson)

    private fun activeInput(direction: Direction): PacMazeInputState =
        PacMazeInputState(current = direction, queued = direction, active = true)

    private fun tick(world: PacMazeWorldState, direction: Direction?): PacMazeWorldState =
        PacMazeSimulation.tick(
            world,
            direction?.let { activeInput(it) },
            level,
        )

    @Test
    fun buildInitialWorld_hasPellets() {
        val world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 42L)
        assertTrue(world.pelletsRemaining > 0)
        assertEquals(PacMazePhase.PLAYING, world.phase)
        assertEquals(PacMazeConstants.INITIAL_LIVES, world.lives)
    }

    @Test
    fun tick_advancesWithoutCrash() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 7L)
        repeat(120) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        assertTrue(world.tick > 0L)
    }

    @Test
    fun eatPellet_reducesCount() {
        val world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 1L)
        val pac = world.entities.first { it.role == "pac" }
        val after = PacMazeRules.eatPellet(world, pac.x, pac.y)
        assertTrue(after.pelletsRemaining <= world.pelletsRemaining)
    }

    @Test
    fun pac_blockedByWall_doesNotChangeTile() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 3L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(x = 2f, y = 1f, direction = Direction.UP, facing = Direction.UP)
                    it.role == "ghost" -> it.copy(x = 3f, y = 1f)
                    else -> it
                }
            },
        )
        repeat(60 * 3) {
            world = tick(world, Direction.UP)
        }
        val after = world.entities.first { it.role == "pac" }
        assertEquals(1, PacMazeMotion.tileY(after.y))
    }

    @Test
    fun pac_movesContinuously_withSubCellProgress() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 5L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(x = 1f, y = 3f, direction = Direction.RIGHT, facing = Direction.RIGHT)
                    it.role == "ghost" -> it.copy(x = 3f, y = 1f)
                    else -> it
                }
            },
        )
        val beforeX = world.entities.first { it.role == "pac" }.x
        world = tick(world, Direction.RIGHT)
        val afterX = world.entities.first { it.role == "pac" }.x
        assertTrue(afterX > beforeX)
        assertTrue(afterX < beforeX + 1f)
    }

    @Test
    fun pac_eachDirection_movesOnWalkableTile() {
        val base = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 9L)

        fun worldAt(x: Float, y: Float) = base.copy(
            entities = base.entities.map {
                when {
                    it.id == base.entities.first { e -> e.role == "pac" }.id ->
                        it.copy(x = x, y = y, direction = null)
                    it.role == "ghost" -> it.copy(x = 1f, y = 1f)
                    else -> it
                }
            },
        )

        val up = tick(worldAt(3f, 3f), Direction.UP)
        assertTrue(up.entities.first { it.role == "pac" }.y < 3f)

        val down = tick(worldAt(3f, 2f), Direction.DOWN)
        assertTrue(down.entities.first { it.role == "pac" }.y > 2f)

        val left = tick(worldAt(3f, 3f), Direction.LEFT)
        assertTrue(left.entities.first { it.role == "pac" }.x < 3f)

        val right = tick(worldAt(2f, 3f), Direction.RIGHT)
        assertTrue(right.entities.first { it.role == "pac" }.x > 2f)
    }

    @Test
    fun ghost_picksValidDirection_afterRelease() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 11L)
        world = world.copy(ghostReleaseTicksLeft = 0)
        val ghostStart = world.entities.first { it.role == "ghost" }
        repeat(PacMazeConstants.GHOST_MOVE_INTERVAL_TICKS * 2) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        val ghostAfter = world.entities.first { it.role == "ghost" }
        assertTrue(
            ghostAfter.x != ghostStart.x ||
                ghostAfter.y != ghostStart.y ||
                ghostAfter.direction != null,
        )
    }

    @Test
    fun pac_turnsImmediately_atIntersectionCenter() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 31L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(x = 3f, y = 3f, direction = Direction.RIGHT, facing = Direction.RIGHT)
                    it.role == "ghost" -> it.copy(x = 1f, y = 1f)
                    else -> it
                }
            },
            ghostReleaseTicksLeft = 999,
        )
        world = tick(world, Direction.UP)
        val after = world.entities.first { it.role == "pac" }
        assertEquals(Direction.UP, after.direction)
        assertEquals(Direction.UP, after.facing)
    }

    @Test
    fun pac_queuesTurnUntilAligned_inCorridor() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 32L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(
                        x = 2.35f,
                        y = 3.2f,
                        direction = Direction.RIGHT,
                        facing = Direction.RIGHT,
                    )
                    it.role == "ghost" -> it.copy(x = 1f, y = 1f)
                    else -> it
                }
            },
            ghostReleaseTicksLeft = 999,
        )
        world = tick(world, Direction.UP)
        val after = world.entities.first { it.role == "pac" }
        assertEquals(Direction.UP, after.nextDirection)
        assertEquals(Direction.UP, after.facing)
        assertEquals(Direction.RIGHT, after.direction)
    }

    @Test
    fun pac_pushingWall_doesNotJitter() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 41L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(x = 2f, y = 1f, direction = Direction.UP)
                    it.role == "ghost" -> it.copy(x = 3f, y = 3f)
                    else -> it
                }
            },
            ghostReleaseTicksLeft = 999,
        )
        val samples = mutableListOf<Pair<Float, Float>>()
        repeat(40) {
            world = tick(world, Direction.UP)
            val p = world.entities.first { it.role == "pac" }
            samples.add(p.x to p.y)
        }
        val settled = samples.takeLast(20)
        val ref = settled.first()
        settled.forEach { (x, y) ->
            assertEquals(ref.first, x, 0.001f)
            assertEquals(ref.second, y, 0.001f)
        }
    }

    @Test
    fun pac_cannotEnterWallTile_whenPushingIntoWall() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 21L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                when {
                    it.id == pac.id -> it.copy(x = 2f, y = 1f, direction = Direction.UP)
                    it.role == "ghost" -> it.copy(x = 3f, y = 3f)
                    else -> it
                }
            },
        )
        repeat(60 * 3) {
            world = tick(world, Direction.UP)
        }
        val after = world.entities.first { it.role == "pac" }
        assertTrue(PacMazeRules.isWalkable(world, PacMazeMotion.tileX(after.x), PacMazeMotion.tileY(after.y)))
        assertTrue(PacMazeMotion.isPositionLegal(world, after.x, after.y, forGhost = false))
    }

    @Test
    fun ghost_staysOnWalkableTiles_afterManyTicks() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 23L)
        world = world.copy(ghostReleaseTicksLeft = 0)
        repeat(300) {
            world = PacMazeSimulation.tick(world, null, level)
        }
        world.entities.filter { it.role == "ghost" }.forEach { ghost ->
            assertTrue(PacMazeMotion.isPositionLegal(world, ghost.x, ghost.y, forGhost = true))
            assertTrue(PacMazeRules.isWalkable(world, PacMazeMotion.tileX(ghost.x), PacMazeMotion.tileY(ghost.y), forGhost = true))
        }
    }

    @Test
    fun joystickAngleToDirection_mapsQuadrants() {
        assertEquals(Direction.RIGHT, joystickAngleToDirection(0))
        assertEquals(Direction.UP, joystickAngleToDirection(90))
        assertEquals(Direction.LEFT, joystickAngleToDirection(180))
        assertEquals(Direction.DOWN, joystickAngleToDirection(270))
    }

    @Test
    fun portal_verticalDown_warpsLeftToRightLowerChannel() {
        val json = """
            {
              "id": 1,
              "name": "portal-vertical",
              "width": 7,
              "height": 5,
              "grid": [
                "#######",
                "#.....#",
                "#=...=#",
                "#.....#",
                "#######"
              ],
              "spawn": { "pac": [3, 2], "ghosts": [[4, 2]] },
              "markers": [
                { "type": "checkpoint", "x": 1, "y": 2, "label": "001", "tag": "LINK" },
                { "type": "checkpoint", "x": 5, "y": 2, "label": "002", "tag": "LINK" }
              ],
              "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.2 }
            }
        """.trimIndent()
        val portalLevel = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(portalLevel, json, seed = 1L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                if (it.id == pac.id) {
                    it.copy(x = 1f, y = 1.8f, direction = Direction.DOWN, facing = Direction.DOWN)
                } else {
                    it
                }
            },
        )
        val warped = PacMazePortals.applyTransit(world, world.entities.first { it.role == "pac" }, portalLevel)
        assertEquals(5, PacMazeMotion.tileX(warped.x))
        assertEquals(3, PacMazeMotion.tileY(warped.y))
        assertEquals(Direction.DOWN, warped.direction)
    }

    @Test
    fun portal_verticalUp_warpsLeftToRightUpperChannel() {
        val json = """
            {
              "id": 1,
              "name": "portal-vertical",
              "width": 7,
              "height": 5,
              "grid": [
                "#######",
                "#.....#",
                "#=...=#",
                "#.....#",
                "#######"
              ],
              "spawn": { "pac": [3, 2], "ghosts": [[4, 2]] },
              "markers": [
                { "type": "checkpoint", "x": 1, "y": 2, "label": "001", "tag": "LINK" },
                { "type": "checkpoint", "x": 5, "y": 2, "label": "002", "tag": "LINK" }
              ],
              "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.2 }
            }
        """.trimIndent()
        val portalLevel = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(portalLevel, json, seed = 1L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                if (it.id == pac.id) {
                    it.copy(x = 1f, y = 2.2f, direction = Direction.UP, facing = Direction.UP)
                } else {
                    it
                }
            },
        )
        val warped = PacMazePortals.applyTransit(world, world.entities.first { it.role == "pac" }, portalLevel)
        assertEquals(5, PacMazeMotion.tileX(warped.x))
        assertEquals(1, PacMazeMotion.tileY(warped.y))
        assertEquals(Direction.UP, warped.direction)
    }

    @Test
    fun doorTile_walkableForPac() {
        val json = """
            {
              "id": 2,
              "name": "door",
              "width": 3,
              "height": 3,
              "grid": ["###", "#=#", "###"],
              "spawn": { "pac": [1, 1], "ghosts": [[1, 1]] },
              "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.2 }
            }
        """.trimIndent()
        val cfg = PacMazeMapLoader.parseLevelJson(json)
        val world = PacMazeMapLoader.buildInitialWorld(cfg, json, seed = 1L)
        assertTrue(PacMazeRules.isWalkable(world, 1, 1, forGhost = false))
        assertTrue(PacMazeRules.isWalkable(world, 1, 1, forGhost = true))
    }

    @Test
    fun loader_parsesCourtyardMaterialTiles() {
        val json = """
            {
              "id": 99,
              "name": "material-test",
              "width": 5,
              "height": 3,
              "grid": [
                "bbbbb",
                "b.w.b",
                "bbbbb"
              ],
              "spawn": { "pac": [2, 1], "ghosts": [[1, 1]] },
              "difficulty": { "ghost_speed_mul": 0.5, "ai_aggression": 0.5 }
            }
        """.trimIndent()
        val config = PacMazeMapLoader.parseLevelJson(json)
        val world = PacMazeMapLoader.buildInitialWorld(config, json, seed = 1L)
        assertEquals(TileType.BRICK_WALL, world.tileAt(0, 0))
        assertEquals(TileType.WOOD_WALL, world.tileAt(2, 1))
        assertTrue(PacMazeMapDynamics.isTileBlocking(world, TileType.WOOD_WALL, 2, 1, forGhost = false))
    }
}
