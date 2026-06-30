package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import com.example.funlife.ui.screens.pacmaze.cosmetic.PacMazeSkinId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PacMazeRemoteSkinAnimCatalogTest {

    @Test
    fun proMax_pacMaze_excludesIdle() {
        val clips = PacMazeRemoteSkinAnimCatalog.pacMazeClips(PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX)
        assertFalse(PacMazeSkinAnimClip.IDLE in clips)
        assertEquals(PacMazeSkinAnimClip.WALK in clips, true)
    }

    @Test
    fun proMax_idleUsesWalkClip() {
        val pose = PacMazeCharacterPose(
            facing = Direction.RIGHT,
            animPhase = 0f,
            isMoving = false,
            powerActive = false,
        )
        val clip = PacMazeRemoteSkinAnimCatalog.pickClip(PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX, pose)
        assertEquals(PacMazeSkinAnimClip.WALK, clip)
    }

    @Test
    fun proMax_walkFrameIndex_whenStationary_isZero() {
        val pose = PacMazeCharacterPose(
            facing = Direction.RIGHT,
            animPhase = 12f,
            isMoving = false,
            powerActive = false,
        )
        val index = PacMazeRemoteSkinAnimCatalog.frameIndex(
            skinId = PacMazeSkinId.FOOD_CHICK_WALKER_PRO_MAX,
            pose = pose,
            clip = PacMazeSkinAnimClip.WALK,
            frameCount = 17,
        )
        assertEquals(0, index)
    }
}
