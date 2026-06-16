package com.example.funlife.ui.screens.pacmaze.cosmetic

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazeMotion
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.social.game.engine.pacmaze.TileType
import com.example.funlife.social.game.engine.pacmaze.PacMazeTickInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeCorridorPassTest {

    private fun corridorWorld(): PacMazeWorldState {
        // 5x3：中间一行可走，上下墙
        val w = 5
        val h = 3
        val tiles = IntArray(w * h) { TileType.WALL.code }
        for (x in 0 until w) tiles[x + h / 2 * w] = TileType.EMPTY.code
        return PacMazeWorldState(
            tick = 0L,
            levelId = 1,
            tiles = tiles,
            width = w,
            height = h,
            entities = listOf(
                PacMazeEntity(
                    id = "pac",
                    role = "pac",
                    x = 2f,
                    y = 1f,
                    direction = Direction.RIGHT,
                    speed = 0f,
                ),
            ),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
    }

    @Test
    fun defaultBodyRadius_canMoveInCorridor() {
        val world = corridorWorld()
        val pac = world.entities.first()
        assertTrue(
            PacMazeMotion.canMoveInDir(world, pac.x, pac.y, Direction.RIGHT, forGhost = false),
        )
    }

    @Test
    fun oversizedBodyRadius_blockedInCorridor() {
        val world = corridorWorld()
        val pac = world.entities.first()
        val huge = PacMazeMotion.MAX_CORRIDOR_BODY_RADIUS + 0.02f
        assertFalse(
            PacMazeMotion.canMoveInDir(
                world, pac.x, pac.y, Direction.RIGHT, forGhost = false, bodyRadius = huge,
            ),
        )
    }

    @Test
    fun bitmapAt100Percent_canPassCorridor() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        assertTrue(PacMazeCosmeticCatalog.canPassCorridor(loadout, userDrawScale = 1f))
    }

    @Test
    fun bitmapAt140Percent_cannotPassButCanStand() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        assertFalse(PacMazeCosmeticCatalog.canPassCorridor(loadout, userDrawScale = 1.4f))
    }

    @Test
    fun bitmapAt200Percent_cannotPassCorridor() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        assertFalse(PacMazeCosmeticCatalog.canPassCorridor(loadout, userDrawScale = 2f))
    }

    @Test
    fun bitmapAt100Percent_tickPlayerAdvances() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        val passRadius = PacMazeCosmeticCatalog.gameplayPassRadius(loadout, userDrawScale = 1f)
        val world = corridorWorld()
        val pac = world.entities.first()
        val input = PacMazeTickInput.committed(1L, Direction.RIGHT)
        val after = PacMazeMotion.tickPlayer(world, pac, input, passRadius = passRadius)
        assertTrue("expected horizontal advance at 100%", after.x > pac.x)
    }

    @Test
    fun bitmapAt140Percent_stillMovesWithCappedPassRadius() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        val passRadius = PacMazeCosmeticCatalog.gameplayPassRadius(loadout, userDrawScale = 1.4f)
        assertEquals(PacMazeMotion.MAX_CORRIDOR_BODY_RADIUS, passRadius, 0.001f)
        val world = corridorWorld()
        val pac = world.entities.first()
        val input = PacMazeTickInput.committed(1L, Direction.RIGHT)
        val after = PacMazeMotion.tickPlayer(world, pac, input, passRadius = passRadius)
        assertTrue("140% visual should still move with capped pass radius", after.x > pac.x)
    }

    @Test
    fun bitmapPassRadius_neverBelowBodyRadius() {
        val loadout = PacMazeAvatarLoadout(skinId = PacMazeSkinId.FOOD_TOUSHI_WALK)
        val passRadius = PacMazeCosmeticCatalog.gameplayPassRadius(loadout, userDrawScale = 1f)
        assertEquals(PacMazeMotion.BODY_RADIUS, passRadius, 0.001f)
    }

    @Test
    fun oversizedPassRadius_stillLegalStandingPosition() {
        val world = corridorWorld()
        val pac = world.entities.first()
        val huge = PacMazeMotion.MAX_CORRIDOR_BODY_RADIUS + 0.02f
        assertTrue(
            PacMazeMotion.isPositionLegal(world, pac.x, pac.y, forGhost = false, bodyRadius = PacMazeMotion.BODY_RADIUS),
        )
        assertFalse(
            PacMazeMotion.canMoveInDir(
                world, pac.x, pac.y, Direction.RIGHT, forGhost = false, bodyRadius = huge,
            ),
        )
    }
}
