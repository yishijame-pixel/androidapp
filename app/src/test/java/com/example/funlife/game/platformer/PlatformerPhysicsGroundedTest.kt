package com.example.funlife.game.platformer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformerPhysicsGroundedTest {

    @Test
    fun standingOnSolidTile_staysGroundedAfterPhysicsTick() {
        val w = 20
        val h = 10
        val groundRow = h - 1
        val cells = Array(w * h) { PlatformerCell.AIR }
        for (x in 0 until w) {
            cells[groundRow * w + x] = PlatformerCell.SOLID
        }
        val tile = PLATFORMER_TILE_PX
        val ph = PlatformerPhysics.playerH(tile)
        val level = PlatformerLevelDef(
            id = 0,
            title = "test",
            subtitle = "",
            theme = PlatformerTheme.GRASS,
            rows = List(h) { if (it == groundRow) "#".repeat(w) else ".".repeat(w) },
            skyTop = 0,
            skyBottom = 0,
        )
        var world = PlatformerWorld(
            level = level,
            width = w,
            height = h,
            cells = cells,
            gems = emptyList(),
            player = PlatformerPlayer(
                x = tile * 2f,
                y = groundRow * tile - ph,
                grounded = true,
            ),
            tilePx = tile,
        )

        repeat(30) {
            world = PlatformerPhysics.tick(
                world,
                PlatformerInput(),
                dt = 1f / 60f,
            )
        }

        assertTrue(
            "Player on flat ground should remain grounded (was: grounded=${world.player.grounded}, vy=${world.player.vy})",
            world.player.grounded,
        )
        assertFalse(
            "Standing still on ground should not use jump clip condition",
            !world.player.grounded,
        )
    }

    @Test
    fun jumpInput_leavesGroundThenReturns() {
        val baseWorld = PlatformerLevels.buildEmergencyWorld(
            PlatformerLevelDef(
                id = 1,
                title = "jump",
                subtitle = "",
                theme = PlatformerTheme.GRASS,
                rows = emptyList(),
                skyTop = 0,
                skyBottom = 0,
            ),
            PlatformerCharacterId.CHICK_PRO_MAX,
        )
        var current = baseWorld.copy(
            player = baseWorld.player.copy(grounded = true),
        )
        current = PlatformerPhysics.tick(
            current,
            PlatformerInput(jumpPressed = true, jumpHeld = true),
            dt = 1f / 60f,
        )
        assertFalse("Should leave ground when jumping", current.player.grounded)

        repeat(120) {
            current = PlatformerPhysics.tick(
                current,
                PlatformerInput(),
                dt = 1f / 60f,
            )
            if (current.player.grounded) return@repeat
        }
        assertTrue("Should land and become grounded again", current.player.grounded)
    }
}
