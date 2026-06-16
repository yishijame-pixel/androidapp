package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private fun tick(world: PacMazeWorldState, input: PacMazeTickInput?): PacMazeWorldState =
        PacMazeSimulation.tick(world, input, level)

    private fun tick(world: PacMazeWorldState, direction: Direction?): PacMazeWorldState =
        tick(
            world,
            direction?.let { PacMazeTickInput.committed(world.tick, it) },
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
    fun campaignGhosts_moveContinuously_afterRelease_onLevelOneLayout() {
        val json = """
            {
              "id": 1,
              "name": "campaign-ghost-move",
              "width": 17,
              "height": 13,
              "grid": [
                "#################",
                "#*.............*#",
                "#.##.#.....#.##.#",
                "#.#..#.....#..#.#",
                "#.#.##.#H#.##.#.#",
                "#...>.....<.....#",
                "#.##.#.....#.##.#",
                "#=.............=#",
                "#.##.#.....#.##.#",
                "#.#..#.....#..#.#",
                "#.#.##....##..#.#",
                "#*.............*#",
                "#################"
              ],
              "spawn": {
                "pac": [8, 11],
                "ghosts": [[6, 5], [7, 5], [8, 5], [9, 5]]
              },
              "difficulty": { "ghost_speed_mul": 0.42, "ai_aggression": 0.45 }
            }
        """.trimIndent()
        val campaignLevel = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(campaignLevel, json, seed = 99L)
        val startPositions = world.entities
            .filter { it.role == "ghost" }
            .associate { it.id to (it.x to it.y) }
        world = world.copy(ghostReleaseTicksLeft = 0)
        repeat(360) {
            world = PacMazeSimulation.tick(world, PacMazeTickInput.Inactive, campaignLevel)
        }
        val ghosts = world.entities.filter { it.role == "ghost" && it.ghostMode != GhostMode.EATEN }
        assertTrue(ghosts.isNotEmpty())
        val movingGhosts = ghosts.count { ghost ->
            val start = startPositions[ghost.id]!!
            val movedAnchor = ghost.x != start.first || ghost.y != start.second
            val hasVelocity = ghost.velX != 0f || ghost.velY != 0f
            movedAnchor || hasVelocity || ghost.ghostStuckTicks < 6
        }
        assertTrue(
            "Campaign ghosts should patrol after release ($movingGhosts/${ghosts.size})",
            movingGhosts >= (ghosts.size * 0.75).toInt().coerceAtLeast(1),
        )
    }

    @Test
    fun ghost_tickGhost_acceptsSubTileProgress_atCampaignSpeed() {
        val speed = PacMazeConstants.ghostSpeedCellsPerSec(GhostMode.SCATTER, 0.42f * 0.88f)
        val world = PacMazeWorldState(
            tick = 0L,
            levelId = 1,
            tiles = IntArray(5 * 5) { TileType.EMPTY.code },
            width = 5,
            height = 5,
            entities = emptyList(),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
        val ghost = PacMazeEntity(
            id = "ghost_0",
            role = "ghost",
            x = 2f,
            y = 2f,
            direction = Direction.RIGHT,
            facing = Direction.RIGHT,
            speed = speed,
            ghostMode = GhostMode.SCATTER,
            ghostKind = GhostKind.STRIKER,
        )
        val moved = PacMazeMotion.tickGhost(world, ghost, Direction.RIGHT, speed)
        assertTrue(moved.x > ghost.x)
        assertTrue(moved.velX > 0f)
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
                        y = 3.38f,
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
        assertEquals(Direction.RIGHT, after.facing)
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
    fun joystickOffsetToDirection_mapsScreenOffsets() {
        assertEquals(Direction.LEFT, joystickOffsetToDirection(-1f, 0f))
        assertEquals(Direction.RIGHT, joystickOffsetToDirection(1f, 0f))
        assertEquals(Direction.UP, joystickOffsetToDirection(0f, -1f))
        assertEquals(Direction.DOWN, joystickOffsetToDirection(0f, 1f))
    }

    @Test
    fun pac_spinMode_doesNotStartMoving() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 56L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                if (it.id == pac.id) it.copy(x = 2f, y = 2f, direction = null) else it
            },
            ghostReleaseTicksLeft = 999,
        )
        world = PacMazeSimulation.tick(world, PacMazeTickInput.spin(world.tick, Direction.RIGHT), level)
        val after = world.entities.first { it.role == "pac" }
        assertNull(after.direction)
        assertEquals(Direction.RIGHT, after.facing)
    }

    @Test
    fun pac_afterSpinSimulation_followsFinalJoystickDirection() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 55L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                if (it.id == pac.id) {
                    it.copy(
                        x = 2f,
                        y = 2f,
                        direction = null,
                        nextDirection = Direction.UP,
                        facing = Direction.UP,
                        velX = 0f,
                        velY = 0f,
                    )
                } else {
                    it
                }
            },
            ghostReleaseTicksLeft = 999,
        )
        world = PacMazeSimulation.tick(world, PacMazeTickInput.deadZone(world.tick), level)
        val stalled = world.entities.first { it.role == "pac" }
        assertNull(stalled.direction)
        world = tick(world, Direction.LEFT)
        world = tick(world, Direction.LEFT)
        val after = world.entities.first { it.role == "pac" }
        assertEquals(Direction.LEFT, after.direction)
        assertEquals(Direction.LEFT, after.facing)
    }

    @Test
    fun portal_requiresBothArmedBeforeWarp() {
        val json = """
            {
              "id": 1,
              "name": "portal-arm",
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
        val level = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 1L)
        val onLeft = world.entities.first { it.role == "pac" }.copy(
            x = 1f, y = 1.8f, direction = Direction.DOWN, facing = Direction.DOWN,
        )

        val notArmed = PacMazePortals.applyTransit(world, onLeft, level)
        assertEquals(1, PacMazeMotion.tileX(notArmed.x))
        assertEquals(0, PacMazePortals.armedPortalCount(world, level))

        world = PacMazePortals.tryArmLinkPair(world, onLeft, level)
        assertEquals(1, PacMazePortals.armedPortalCount(world, level))
        assertFalse(PacMazePortals.isLinkArmed(world, level))

        val stillBlocked = PacMazePortals.applyTransit(world, onLeft, level)
        assertEquals(1, PacMazeMotion.tileX(stillBlocked.x))

        val onRight = onLeft.copy(x = 5f, y = 2.2f, direction = Direction.UP, facing = Direction.UP)
        world = PacMazePortals.tryArmLinkPair(world, onRight, level)
        assertEquals(2, PacMazePortals.armedPortalCount(world, level))
        assertTrue(PacMazePortals.isLinkArmed(world, level))

        val warped = PacMazePortals.applyTransit(world, onLeft, level)
        assertEquals(5, PacMazeMotion.tileX(warped.x))
        assertEquals(3, PacMazeMotion.tileY(warped.y))
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
        world = world.copy(
            visitedCheckpointTags = setOf(
                PacMazePortals.armedTagAt(1, 2),
                PacMazePortals.armedTagAt(5, 2),
            ),
        )
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
        world = world.copy(
            visitedCheckpointTags = setOf(
                PacMazePortals.armedTagAt(1, 2),
                PacMazePortals.armedTagAt(5, 2),
            ),
        )
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

    @Test
    fun mazeGhosts_keepMovingThroughDeadEnds() {
        val options = PacMazeMazeRunOptions(seed = 77L)
        val json = PacMazeMazeGenerator.buildLevelJson(options)
        val level = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 77L)
        val startTiles = world.entities
            .filter { it.role == "ghost" }
            .associate { it.id to (PacMazeMotion.tileX(it.x) to PacMazeMotion.tileY(it.y)) }
        repeat(600) {
            world = PacMazeSimulation.tick(world, PacMazeTickInput.Inactive, level)
        }
        val ghosts = world.entities.filter { it.role == "ghost" && it.ghostMode != GhostMode.EATEN }
        assertTrue(ghosts.isNotEmpty())
        val movingGhosts = ghosts.count { ghost ->
            val start = startTiles[ghost.id]!!
            val end = PacMazeMotion.tileX(ghost.x) to PacMazeMotion.tileY(ghost.y)
            start != end || ghost.ghostStuckTicks < 8
        }
        assertTrue(
            "Maze ghosts should keep patrolling ($movingGhosts/${ghosts.size})",
            movingGhosts >= (ghosts.size * 0.75).toInt().coerceAtLeast(1),
        )
    }

    @Test
    fun mazeMode_neverSwitchesToScatter() {
        val options = PacMazeMazeRunOptions(seed = 5L)
        val json = PacMazeMazeGenerator.buildLevelJson(options)
        val level = PacMazeMapLoader.parseLevelJson(json)
        var world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 5L)
        repeat(PacMazeConstants.GHOST_MODE_CYCLE_TICKS + 40) {
            world = PacMazeSimulation.tick(world, PacMazeTickInput.Inactive, level)
        }
        assertEquals(GhostMode.CHASE, world.ghostMode)
    }

    @Test
    fun pac_staysInsideWalkableTiles_whenPushingAllDirections() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 88L)
        world = world.copy(ghostReleaseTicksLeft = 999)
        val directions = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
        repeat(600) { step ->
            val dir = directions[step % directions.size]
            world = tick(world, dir)
            val pac = world.entities.first { it.role == "pac" }
            assertTrue(
                "tick=$step pos=(${pac.x},${pac.y})",
                PacMazeMotion.isPositionLegal(world, pac.x, pac.y, forGhost = false),
            )
            val tx = PacMazeMotion.tileX(pac.x)
            val ty = PacMazeMotion.tileY(pac.y)
            assertTrue(
                "tick=$step tile=($tx,$ty)",
                PacMazeRules.isWalkable(world, tx, ty, forGhost = false),
            )
        }
    }

    @Test
    fun pac_blockedAtBottomWall_cannotCrossBoundary() {
        var world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 90L)
        val pac = world.entities.first { it.role == "pac" }
        world = world.copy(
            entities = world.entities.map {
                if (it.id == pac.id) {
                    it.copy(x = 8f, y = 11f, direction = Direction.DOWN, facing = Direction.DOWN)
                } else if (it.role == "ghost") {
                    it.copy(x = 1f, y = 1f)
                } else {
                    it
                }
            },
            ghostReleaseTicksLeft = 999,
        )
        repeat(120) {
            world = tick(world, Direction.DOWN)
        }
        val after = world.entities.first { it.role == "pac" }
        assertTrue(PacMazeMotion.isPositionLegal(world, after.x, after.y, forGhost = false))
        assertTrue(PacMazeMotion.tileY(after.y) <= 11)
    }

    @Test
    fun pac_verticalMovement_railsLaneAndKeepsVelocity() {
        val world = PacMazeMapLoader.buildInitialWorld(level, levelJson, seed = 91L)
        val pac = world.entities.first { it.role == "pac" }.copy(
            x = 3.22f,
            y = 2f,
            direction = Direction.DOWN,
            facing = Direction.DOWN,
        )
        val input = PacMazeTickInput.committed(1L, Direction.DOWN)
        val after = PacMazeMotion.tickPlayer(world, pac, input)
        assertEquals(3f, after.x, 0.001f)
        assertTrue("y should advance downward", after.y > pac.y)
        assertTrue("velY should stay active in vertical corridor", after.velY > 0f)
    }
}
