package com.example.funlife.ui.screens.pacmaze.maptheme

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.social.game.engine.pacmaze.PacMazeEntity
import com.example.funlife.social.game.engine.pacmaze.PacMazePhase
import com.example.funlife.social.game.engine.pacmaze.PacMazeWorldState
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeMapThemeId.CYBERPUNK
import com.example.funlife.ui.screens.pacmaze.maptheme.PacMazeThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeLaneCenterTest {

    private fun context(entity: PacMazeEntity): PacMazeMapRenderContext {
        val world = PacMazeWorldState(
            tick = 0L,
            levelId = 1,
            tiles = IntArray(16),
            width = 4,
            height = 4,
            entities = listOf(entity),
            score = 0,
            lives = 3,
            pelletsRemaining = 0,
            phase = PacMazePhase.PLAYING,
            rngSeed = 1L,
        )
        val cell = 40f
        return PacMazeMapRenderContext(
            world = world,
            previous = null,
            blend = 1f,
            cell = cell,
            cellX = cell,
            cellY = cell,
            offsetX = 10f,
            offsetY = 20f,
            mapW = cell * world.width,
            mapH = cell * world.height,
            animPhase = 0f,
            canvasSize = androidx.compose.ui.geometry.Size(200f, 200f),
            config = PacMazeThemeRegistry.configFor(CYBERPUNK),
            renderAnchor = { e -> e.x to e.y },
        )
    }

    @Test
    fun bitmapDrawCenter_usesWalkableCorridorCenter() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.RIGHT,
            speed = 0f,
        )
        val ctx = context(entity)
        val draw = ctx.playerDrawCenter(entity, PacMazeSkinId.FOOD_CHICK_DAZE)
        val corridor = ctx.entityBitmapCorridorCenter(entity)
        assertEquals(corridor.x, draw.x, 0.001f)
        assertEquals(corridor.y, draw.y, 0.001f)
    }

    @Test
    fun drawnSkin_keepsYOffsetBelowGridCenter() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.RIGHT,
            speed = 0f,
        )
        val ctx = context(entity)
        val logical = ctx.entityCenter(entity)
        val draw = ctx.playerDrawCenter(entity, PacMazeSkinId.LINE_BUNNY)
        assertEquals(logical.x, draw.x, 0.001f)
        assertNotEquals(logical.y, draw.y, 0.001f)
    }

    @Test
    fun bitmapCorridorCenterY_smoothAcrossTileBoundary() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 1f,
            direction = Direction.UP,
            speed = 1f,
            velY = -1f,
        )
        val ctx = context(entity)
        val before = ctx.entityBitmapCorridorCenter(entity.copy(y = 1.95f)).y
        val after = ctx.entityBitmapCorridorCenter(entity.copy(y = 2.05f)).y
        val delta = kotlin.math.abs(after - before)
        assertTrue(delta in 0.5f..15f)
    }

    @Test
    fun bitmapWallBox_top_smoothAcrossTileBoundary() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 1f,
            direction = Direction.UP,
            speed = 1f,
            velY = -1f,
        )
        val ctx = context(entity)
        val before = ctx.entityBitmapWallBox(entity.copy(y = 1.95f)).top
        val after = ctx.entityBitmapWallBox(entity.copy(y = 2.05f)).top
        val delta = kotlin.math.abs(after - before)
        assertTrue(delta in 0.5f..15f)
    }

    @Test
    fun horizontalFeet_alignsWithLaneCenter_forCenterPivotScale() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.RIGHT,
            speed = 1f,
        )
        val ctx = context(entity)
        val anchor = ctx.bitmapDrawAnchor(entity)
        assertEquals(ctx.entityLaneCenterX(entity), anchor.x, 0.01f)
        assertEquals(ctx.entityLaneCenterY(entity), anchor.y, 0.01f)
    }

    @Test
    fun bitmapDrawAnchor_horizontal_feetOnCorridorFloor() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.RIGHT,
            speed = 1f,
        )
        val ctx = context(entity)
        val anchor = ctx.bitmapDrawAnchor(entity, Direction.RIGHT)
        assertEquals(ctx.entityLaneCenterX(entity), anchor.x, 0.01f)
        assertEquals(ctx.entityLaneCenterY(entity), anchor.y, 0.01f)
    }

    @Test
    fun bitmapDrawAnchor_horizontal_ignoresDrawFacingUp() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1.35f,
            y = 2.42f,
            direction = Direction.RIGHT,
            facing = Direction.UP,
            speed = 1f,
            velX = 1f,
        )
        val ctx = context(entity.copy(x = 1.35f, y = 2.42f))
        val anchor = ctx.bitmapDrawAnchor(entity)
        assertEquals(ctx.entityLaneCenterY(entity), anchor.y, 0.01f)
        assertNotEquals(
            ctx.entityVerticalTravelAnchorY(entity),
            anchor.y,
            0.01f,
        )
    }

    @Test
    fun bitmapDrawAnchor_fractionalY_usesLaneCenterFromRow() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1.7f,
            y = 2.15f,
            direction = Direction.RIGHT,
            speed = 1f,
            velX = 1f,
        )
        val ctx = context(entity)
        assertEquals(ctx.entityLaneCenterY(entity), ctx.bitmapDrawAnchor(entity).y, 0.01f)
    }

    @Test
    fun verticalTravelAnchorX_matchesLaneCenter() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.UP,
            speed = 1f,
            velY = -1f,
        )
        val ctx = context(entity)
        val anchor = ctx.bitmapDrawAnchor(entity, Direction.UP)
        assertEquals(ctx.entityLaneCenterX(entity), anchor.x, 0.01f)
    }

    @Test
    fun verticalTravelAnchorY_betweenGridCenterAndTileBottom() {
        val entity = PacMazeEntity(
            id = "pac",
            role = "pac",
            x = 1f,
            y = 2f,
            direction = Direction.DOWN,
            speed = 1f,
            velY = 1f,
        )
        val ctx = context(entity)
        val centerY = ctx.entityLaneCenterY(entity)
        val floor = ctx.entityCorridorFloorY(entity)
        val anchorY = ctx.bitmapDrawAnchor(entity, Direction.DOWN).y
        assertTrue(anchorY > centerY)
        assertTrue(anchorY < floor)
    }

}
