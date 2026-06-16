package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeMotionCollisionTest {

    @Test
    fun isPositionLegal_ignoresDiagonalWallWhenCardinalsWalkable() {
        val tiles = IntArray(9) { TileType.WALL.code }
        tiles[1 * 3 + 1] = TileType.EMPTY.code
        tiles[1 * 3 + 2] = TileType.EMPTY.code
        tiles[2 * 3 + 1] = TileType.EMPTY.code
        tiles[2 * 3 + 2] = TileType.WALL.code
        val state = PacMazeWorldState(
            tick = 0L,
            levelId = 1,
            tiles = tiles,
            width = 3,
            height = 3,
            entities = emptyList(),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
        assertTrue(
            PacMazeMotion.isPositionLegal(state, 1.2f, 1.2f, forGhost = false),
        )
    }

    @Test
    fun isPositionLegal_blocksWhenCardinalWallTouchesBody() {
        val tiles = IntArray(9) { TileType.WALL.code }
        tiles[1 * 3 + 0] = TileType.EMPTY.code
        tiles[1 * 3 + 1] = TileType.EMPTY.code
        val state = PacMazeWorldState(
            tick = 0L,
            levelId = 1,
            tiles = tiles,
            width = 3,
            height = 3,
            entities = emptyList(),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
        assertFalse(
            PacMazeMotion.isPositionLegal(state, 1.2f, 1.0f, forGhost = false),
        )
    }

    @Test
    fun clampRenderAnchor_fallsBackToLogicalWhenExtrapIllegal() {
        val json = PacMazeMazeGenerator.buildLevelJson(PacMazeMazeRunOptions(seed = 7L))
        val level = PacMazeMapLoader.parseLevelJson(json)
        val world = PacMazeMapLoader.buildInitialWorld(level, json, seed = 7L)
        val pac = world.entities.first { it.role == "pac" }
        assertTrue(PacMazeMotion.isPositionLegal(world, pac.x, pac.y, forGhost = false))
        val (rx, ry) = PacMazeMotion.clampRenderAnchor(world, pac, pac.x + 20f, pac.y)
        assertEquals(pac.x, rx, 0.001f)
        assertEquals(pac.y, ry, 0.001f)
    }
}
