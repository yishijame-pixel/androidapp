package com.example.funlife.ui.screens.pacmaze.components

import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class PacMazeRenderTickLoopTest {

    private val stepNs = 1_000_000_000L / PacMazeConstants.TICKS_PER_SECOND

    @Test
    fun step_runsTicksAndKeepsAccumulatorRemainder() {
        var tickCount = 0
        val result = PacMazeRenderTickLoop.step(
            frameDeltaNs = stepNs * 2,
            accumulatorNs = 0L,
            stepNs = stepNs,
            maxTicksPerFrame = 2,
        ) {
            tickCount++
            true
        }
        assertEquals(2, result.ticksThisFrame)
        assertEquals(2, tickCount)
        assertEquals(0L, result.accumulatorNs)
    }

    @Test
    fun renderBlend_isFractionOfStepRemainingInAccumulator() {
        val blend = PacMazeRenderTickLoop.renderBlend(accumulatorNs = stepNs / 4, stepNs = stepNs)
        assertEquals(0.25f, blend, 0.001f)
    }

    @Test
    fun displayProgressBlend_twoTicksWithOneStepRemainder_isTwoThirds() {
        val blend = PacMazeRenderTickLoop.displayProgressBlend(
            accumulatorNs = stepNs,
            stepNs = stepNs,
            ticksThisFrame = 2,
        )
        assertEquals(2f / 3f, blend, 0.001f)
    }

    @Test
    fun displayProgressBlend_twoTicksNoRemainder_isOne() {
        val blend = PacMazeRenderTickLoop.displayProgressBlend(
            accumulatorNs = 0L,
            stepNs = stepNs,
            ticksThisFrame = 2,
        )
        assertEquals(1f, blend, 0.001f)
    }

    @Test
    fun accumInterpolationBlend_whenElapsedExceedsStep_clampsToOne() {
        val blend = PacMazeRenderTickLoop.accumInterpolationBlend(stepNs * 2, stepNs)
        assertEquals(1f, blend, 0.001f)
    }

    @Test
    fun accumInterpolationBlend_midStep_isHalf() {
        val blend = PacMazeRenderTickLoop.accumInterpolationBlend(
            accumulatorNs = stepNs / 2,
            stepNs = stepNs,
        )
        assertEquals(0.5f, blend, 0.001f)
    }

    @Test
    fun accumInterpolationBlend_afterTick_isZero() {
        val blend = PacMazeRenderTickLoop.accumInterpolationBlend(
            accumulatorNs = 0L,
            stepNs = stepNs,
        )
        assertEquals(0f, blend, 0.001f)
    }

    @Test
    fun displaySpanBlend_midFrame_isHalf() {
        val start = 1_000_000L
        val duration = 16_666_666L
        val blend = PacMazeRenderTickLoop.displaySpanBlend(
            spanStartNs = start,
            spanDurationNs = duration,
            displayClockNs = start + duration / 2,
            fallbackBlend = 1f,
        )
        assertEquals(0.5f, blend, 0.02f)
    }

    @Test
    fun displaySpanBlend_noSpan_usesFallback() {
        val blend = PacMazeRenderTickLoop.displaySpanBlend(
            spanStartNs = 0L,
            spanDurationNs = 0L,
            displayClockNs = 999L,
            fallbackBlend = 0.667f,
        )
        assertEquals(0.667f, blend, 0.001f)
    }

    @Test
    fun step_capsTicksPerFrame() {
        var tickCount = 0
        val result = PacMazeRenderTickLoop.step(
            frameDeltaNs = stepNs * 10,
            accumulatorNs = 0L,
            stepNs = stepNs,
            maxTicksPerFrame = 2,
        ) {
            tickCount++
            true
        }
        assertEquals(2, result.ticksThisFrame)
        assertEquals(2, tickCount)
    }
}
