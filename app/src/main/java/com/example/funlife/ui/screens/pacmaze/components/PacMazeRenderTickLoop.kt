package com.example.funlife.ui.screens.pacmaze.components

import com.example.funlife.social.game.engine.pacmaze.PacMazeConstants

/**
 * 固定步长 accumulator：逻辑以 60Hz 追帧；渲染 blend = accum/step，
 * 在相邻两次 sim 态之间插值，配合 display-time animPhase 避免位置/行走帧不同步。
 */
internal object PacMazeRenderTickLoop {

    data class StepResult(
        val accumulatorNs: Long,
        val ticksThisFrame: Int,
    )

    /** @deprecated  capped 多 tick 时 accum≈step 会恒为 1；请用 [displayProgressBlend]。 */
    fun renderBlend(accumulatorNs: Long, stepNs: Long): Float {
        if (stepNs <= 0L) return 1f
        return (accumulatorNs.toFloat() / stepNs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * 本帧 sim 进度：已消耗的 tick 时间 / (已消耗 + 剩余 accum)。
     * 30fps + 每帧 2 tick 时 accum 常剩 ~1 step，旧公式 blend≈1 导致无插值。
     */
    fun displayProgressBlend(
        accumulatorNs: Long,
        stepNs: Long,
        ticksThisFrame: Int,
    ): Float {
        if (ticksThisFrame <= 0 || stepNs <= 0L) return 1f
        val simAdvanceNs = ticksThisFrame.toLong() * stepNs
        val totalNs = simAdvanceNs + accumulatorNs.coerceAtLeast(0L)
        if (totalNs <= 0L) return 1f
        return (simAdvanceNs.toFloat() / totalNs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Fix Your Timestep 渲染 α：accum/step，lerp(penultimate→current, α)。
     * tick 后 accum≈0 → α≈0 显示 previous；帧内 accum↑ → α→1 滑向 current。
     */
    fun accumInterpolationBlend(accumulatorNs: Long, stepNs: Long): Float {
        if (stepNs <= 0L) return 1f
        return (accumulatorNs.toFloat() / stepNs.toFloat()).coerceIn(0f, 1f)
    }

    /** 显示时钟驱动的连续插值（每 vsync 重绘，0→1 跨整帧 wall time）。 */
    fun displaySpanBlend(
        spanStartNs: Long,
        spanDurationNs: Long,
        displayClockNs: Long,
        fallbackBlend: Float,
    ): Float {
        if (spanDurationNs <= 0L || spanStartNs <= 0L) {
            return fallbackBlend.coerceIn(0f, 1f)
        }
        val clockNs = if (displayClockNs > 0L) displayClockNs else System.nanoTime()
        val elapsed = (clockNs - spanStartNs).coerceAtLeast(0L)
        return (elapsed.toFloat() / spanDurationNs.toFloat()).coerceIn(0f, 1f)
    }

    fun step(
        frameDeltaNs: Long,
        accumulatorNs: Long,
        stepNs: Long,
        maxTicksPerFrame: Int = PacMazeConstants.RENDER_MAX_TICKS_PER_FRAME,
        onTick: () -> Boolean,
    ): StepResult {
        var accum = accumulatorNs + frameDeltaNs.coerceAtLeast(0L)
        val maxAccum = stepNs * PacMazeConstants.MAX_SIM_TICKS_PER_FRAME
        if (accum > maxAccum) accum = maxAccum

        var ticks = 0
        while (accum >= stepNs && ticks < maxTicksPerFrame) {
            if (!onTick()) break
            accum -= stepNs
            ticks++
        }

        return StepResult(
            accumulatorNs = accum,
            ticksThisFrame = ticks,
        )
    }
}
