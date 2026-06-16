package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PacMazeBitmapFacingTest {

    @Before
    fun resetFacingMemory() {
        PacMazeBitmapFacingState.clearAll()
    }

    @Test
    fun firstSpawn_facesNaturalLeft() {
        val entity = idlePac(facing = Direction.RIGHT)
        assertEquals(
            Direction.LEFT,
            PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(
                PacMazeSkinId.FOOD_CHICK_DAZE,
                entity,
            ),
        )
    }

    @Test
    fun movingRight_facesRight() {
        val entity = idlePac(facing = Direction.RIGHT).copy(
            direction = Direction.RIGHT,
            velX = 4f,
            inputActive = true,
        )
        assertEquals(
            Direction.RIGHT,
            PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(
                PacMazeSkinId.FOOD_CHICK_DAZE,
                entity,
            ),
        )
    }

    @Test
    fun stoppedAfterMovingRight_keepsRight() {
        val moving = idlePac(facing = Direction.RIGHT).copy(
            direction = Direction.RIGHT,
            velX = 4f,
            inputActive = true,
        )
        PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(PacMazeSkinId.FOOD_CHICK_DAZE, moving)

        val stopped = moving.copy(
            direction = null,
            velX = 0f,
            velY = 0f,
            inputActive = false,
            facing = Direction.RIGHT,
        )
        assertEquals(
            Direction.RIGHT,
            PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(
                PacMazeSkinId.FOOD_CHICK_DAZE,
                stopped,
            ),
        )
    }

    @Test
    fun levelRestart_resetsToNaturalLeft() {
        val moving = idlePac(facing = Direction.RIGHT).copy(
            direction = Direction.RIGHT,
            velX = 4f,
            inputActive = true,
        )
        PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(PacMazeSkinId.FOOD_CHICK_DAZE, moving)

        PacMazeBitmapFacingState.clear("pac")
        assertEquals(
            Direction.LEFT,
            PacMazeSkinRenderProfileCatalog.resolveBitmapDrawFacing(
                PacMazeSkinId.FOOD_CHICK_DAZE,
                idlePac(facing = Direction.RIGHT),
            ),
        )
    }

    private fun idlePac(facing: Direction) = PacMazeEntity(
        id = "pac",
        role = "pac",
        x = 1f,
        y = 1f,
        direction = null,
        speed = 0f,
        facing = facing,
    )
}
