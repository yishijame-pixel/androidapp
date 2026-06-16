package com.example.funlife.ui.screens.pacmaze.cosmetic.skin

import com.example.funlife.social.game.engine.pacmaze.Direction
import com.example.funlife.ui.screens.pacmaze.character.PacMazeCharacterPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeSpriteWalkAnimTest {

    private fun pose(
        phase: Float,
        moving: Boolean = true,
        preview: Boolean = false,
        frameOverride: Int? = null,
    ) = PacMazeCharacterPose(
        facing = Direction.LEFT,
        animPhase = phase,
        isMoving = moving,
        powerActive = false,
        walkPreview = preview,
        spriteFrameOverride = frameOverride,
    )

    @Test
    fun gameplayStepsThroughAllFourFrames() {
        val seen = mutableSetOf<Int>()
        var phase = 0f
        repeat(80) {
            seen += PacMazeSpriteWalkAnim.frameIndex(pose(phase), 4)
            phase += 0.08f
        }
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test
    fun previewOverrideCyclesExplicitFrames() {
        repeat(4) { frame ->
            assertEquals(frame, PacMazeSpriteWalkAnim.frameIndex(pose(0f, frameOverride = frame), 4))
        }
    }

    @Test
    fun idleUsesFirstFrame() {
        assertEquals(0, PacMazeSpriteWalkAnim.frameIndex(pose(5f, moving = false), 4))
    }

    @Test
    fun eachFrameGetsEqualGameplayDuration() {
        val durations = IntArray(4)
        var last = PacMazeSpriteWalkAnim.frameIndex(pose(0f), 4)
        var phase = 0f
        repeat(200) {
            phase += 0.08f
            val index = PacMazeSpriteWalkAnim.frameIndex(pose(phase), 4)
            durations[index]++
            last = index
        }
        assertTrue(durations.all { it in 45..55 })
    }
}
