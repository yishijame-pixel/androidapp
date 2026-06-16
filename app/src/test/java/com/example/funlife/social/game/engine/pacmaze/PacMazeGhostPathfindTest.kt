package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PacMazeGhostPathfindTest {

    private fun wallGrid(vararg rows: String): PacMazeWorldState {
        val height = rows.size
        val width = rows.first().length
        val tiles = IntArray(width * height)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                tiles[y * width + x] = when (ch) {
                    '#' -> TileType.WALL.code
                    else -> TileType.PATH.code
                }
            }
        }
        return PacMazeWorldState(
            tick = 0L,
            levelId = 9,
            tiles = tiles,
            width = width,
            height = height,
            entities = emptyList(),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
    }

    private fun ghostAt(x: Int, y: Int, dir: Direction = Direction.RIGHT) = PacMazeEntity(
        id = "ghost_0",
        role = "ghost",
        x = x.toFloat(),
        y = y.toFloat(),
        direction = dir,
        facing = dir,
        speed = 1f,
        ghostMode = GhostMode.CHASE,
        ghostKind = GhostKind.STRIKER,
    )

    @Test
    fun bfs_routesAroundWallInsteadOfGreedyDeadEnd() {
        val state = wallGrid(
            "#######",
            "#.....#",
            "#.###.#",
            "#.#...#",
            "#.#.###",
            "#.....#",
            "#######",
        )
        val ghost = ghostAt(1, 5, Direction.UP)
        val options = Direction.entries.filter { dir ->
            PacMazeMotion.canMoveInDir(state, ghost.x, ghost.y, dir, forGhost = true, ghost = ghost)
        }
        val step = PacMazeGhostPathfind.nextStepToward(state, ghost, targetX = 5, targetY = 5, options, allowReverse = false)
        assertEquals(Direction.RIGHT, step)
    }

    @Test
    fun bfs_findsReachableStepWhenTargetOnOtherSideOfWall() {
        val state = wallGrid(
            "#######",
            "#.....#",
            "#.###.#",
            "#.#...#",
            "#.#.###",
            "#.....#",
            "#######",
        )
        val ghost = ghostAt(1, 5, Direction.UP)
        val options = Direction.entries.filter { dir ->
            PacMazeMotion.canMoveInDir(state, ghost.x, ghost.y, dir, forGhost = true, ghost = ghost)
        }
        val step = PacMazeGhostPathfind.nextStepToward(state, ghost, targetX = 5, targetY = 1, options, allowReverse = false)
        assertNotEquals(null, step)
        assertEquals(true, step in options)
    }
}
