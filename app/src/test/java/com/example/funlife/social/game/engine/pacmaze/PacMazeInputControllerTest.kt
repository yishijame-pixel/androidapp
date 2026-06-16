package com.example.funlife.social.game.engine.pacmaze

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacMazeInputControllerTest {

    private val maxRadius = 100f
    private val config = PacMazeInputConfig(
        deadZone = 0.08f,
        commitThreshold = 0.42f,
        softCommitThreshold = 0.42f,
        stableTicksRequired = 2,
        spinSectorChanges = 4,
        spinWindowTicks = 10,
        spinAngleDeg = 90f,
        spinReleaseStableTicks = 10,
        spinBreakoutStrength = 0.80f,
        spinBreakoutStableTicks = 4,
    )

    private fun sample(offsetX: Float, offsetY: Float, fingerDown: Boolean = true) =
        PacMazeRawJoystickSample(offsetX, offsetY, maxRadius, fingerDown)

    private fun left(strength: Float = 0.9f) = sample(-maxRadius * strength, 0f)

    private fun right(strength: Float = 0.9f) = sample(maxRadius * strength, 0f)

    private fun up(strength: Float = 0.9f) = sample(0f, -maxRadius * strength)

    private fun down(strength: Float = 0.9f) = sample(0f, maxRadius * strength)

    @Test
    fun advanceTick_inactiveWhenFingerUp() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(sample(0f, 0f, fingerDown = false))
        val input = controller.advanceTick(1L)
        assertFalse(input.active)
        assertEquals(PacMazeInputMode.Idle, input.mode)
    }

    @Test
    fun advanceTick_deadZoneWhenCenter() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(sample(0f, 0f))
        val input = controller.advanceTick(1L)
        assertTrue(input.active)
        assertEquals(PacMazeInputMode.DeadZone, input.mode)
        assertNull(input.committed)
    }

    @Test
    fun advanceTick_commitsAfterStableTicks() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(left(0.35f))
        val first = controller.advanceTick(1L)
        assertEquals(PacMazeInputMode.Pending, first.mode)
        assertNull(first.committed)
        assertEquals(Direction.LEFT, first.sector)

        controller.submitRaw(left(0.35f))
        val second = controller.advanceTick(2L)
        assertEquals(PacMazeInputMode.Committed, second.mode)
        assertEquals(Direction.LEFT, second.committed)
        assertTrue(second.generation >= 1L)
    }

    @Test
    fun advanceTick_commitsImmediatelyOnStrongPush() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(left(0.95f))
        val input = controller.advanceTick(1L)
        assertEquals(PacMazeInputMode.Committed, input.mode)
        assertEquals(Direction.LEFT, input.committed)
    }

    @Test
    fun advanceTick_spinModeOnRapidSectorChanges() {
        val controller = PacMazeInputController(config)
        val sectors = listOf(
            right(0.35f) to 1L,
            up(0.35f) to 2L,
            left(0.35f) to 3L,
            down(0.35f) to 4L,
            right(0.35f) to 5L,
        )
        var last = PacMazeTickInput.Inactive
        for ((sample, tick) in sectors) {
            controller.submitRaw(sample)
            last = controller.advanceTick(tick)
        }
        assertEquals(PacMazeInputMode.Spin, last.mode)
        assertNull(last.committed)
        assertEquals(Direction.RIGHT, last.facing)
    }

    @Test
    fun replay_spinThenLeft_commitsLeft() {
        val controller = PacMazeInputController(config)
        val script = buildMap {
            put(1L, right(0.35f))
            put(2L, up(0.35f))
            put(3L, left(0.35f))
            put(4L, down(0.35f))
            put(5L, sample(0f, 0f))
            put(6L, left(0.95f))
            put(7L, left(0.95f))
            put(8L, left(0.95f))
            put(9L, left(0.95f))
            put(10L, left(0.95f))
        }
        val trace = controller.replay(script)
        assertEquals(PacMazeInputMode.Spin, trace.getValue(5L).mode)
        assertEquals(Direction.LEFT, trace.getValue(10L).committed)
    }

    @Test
    fun spinLock_blocksCommitDuringContinuousRotation() {
        val controller = PacMazeInputController(
            config.copy(
                spinAngleDeg = 25f,
                spinSectorChanges = 8,
                commitThreshold = 0.42f,
                softCommitThreshold = 0.42f,
            ),
        )
        var last = PacMazeTickInput.Inactive
        for (tick in 1L..20L) {
            val angleDeg = tick * 40f
            val rad = Math.toRadians(angleDeg.toDouble())
            val strength = 0.35f
            controller.submitRaw(
                sample(
                    offsetX = (kotlin.math.cos(rad) * maxRadius * strength).toFloat(),
                    offsetY = (-kotlin.math.sin(rad) * maxRadius * strength).toFloat(),
                ),
            )
            last = controller.advanceTick(tick)
            assertNull("tick=$tick should not commit during rotation", last.committed)
            assertTrue(
                "tick=$tick mode=${last.mode}",
                last.mode == PacMazeInputMode.Spin || last.mode == PacMazeInputMode.Pending,
            )
        }
    }

    @Test
    fun spinLock_commitsAfterStableHoldOnFinalDirection() {
        val controller = PacMazeInputController(
            config.copy(spinAngleDeg = 60f, spinSectorChanges = 3),
        )
        for (tick in 1L..12L) {
            val angleDeg = tick * 30f
            val rad = Math.toRadians(angleDeg.toDouble())
            controller.submitRaw(
                sample(
                    offsetX = (kotlin.math.cos(rad) * maxRadius * 0.4f).toFloat(),
                    offsetY = (-kotlin.math.sin(rad) * maxRadius * 0.4f).toFloat(),
                ),
            )
            controller.advanceTick(tick)
        }
        var committed: PacMazeTickInput? = null
        for (tick in 13L..30L) {
            controller.submitRaw(left(0.95f))
            val input = controller.advanceTick(tick)
            if (input.mode == PacMazeInputMode.Committed) {
                committed = input
                break
            }
        }
        assertEquals(Direction.LEFT, committed?.committed)
    }

    @Test
    fun reset_clearsCommittedState() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(left(0.95f))
        controller.advanceTick(1L)
        controller.reset()
        controller.submitRaw(sample(0f, 0f, fingerDown = false))
        val input = controller.advanceTick(2L)
        assertFalse(input.active)
        assertNull(input.committed)
    }

    @Test
    fun generation_incrementsOnCommittedChange() {
        val controller = PacMazeInputController(config)
        controller.submitRaw(left(0.95f))
        val leftInput = controller.advanceTick(1L)
        for (tick in 2L..12L) {
            controller.submitRaw(left(0.95f))
            controller.advanceTick(tick)
        }
        controller.submitRaw(right(0.95f))
        var rightInput = controller.advanceTick(13L)
        for (tick in 14L..20L) {
            controller.submitRaw(right(0.95f))
            rightInput = controller.advanceTick(tick)
        }
        assertTrue(rightInput.generation > leftInput.generation)
    }

    @Test
    fun toLegacyState_mapsCommittedForMotion() {
        val tickInput = PacMazeTickInput.committed(3L, Direction.UP, generation = 2L)
        val legacy = tickInput.toLegacyState()
        assertTrue(legacy.active)
        assertFalse(legacy.holdOnly)
        assertEquals(Direction.UP, legacy.queued)
        assertEquals(PacMazeInputMode.Committed, legacy.mode)
    }
}
