package com.example.funlife.social.game.engine.pacmaze

/**
 * 企业级摇杆输入控制器：原始样本 → 固定 tick 采样 → FSM 提交。
 *
 * 360° 转杆：累计转角 + 扇区跳变 → [spinLocked]；锁定期间禁止提交；
 * 停稳（近期无转角/扇区变化）或用力 breakout 后才提交最终方向。
 */
class PacMazeInputController(
    private val config: PacMazeInputConfig = PacMazeInputConfig.Default,
) {
    private var raw: PacMazeRawJoystickSample = PacMazeRawJoystickSample.Released
    private var generation: Long = 0L
    private var committed: Direction? = null
    private var pendingSector: Direction? = null
    private var hysteresisSector: Direction? = null
    private var sectorStableTicks: Int = 0
    private val recentSectorChangeTicks = ArrayDeque<Long>()

    private var lastAngleDeg: Float? = null
    private val angleDeltaSamples = ArrayDeque<Pair<Long, Float>>()

    private var spinLocked: Boolean = false
    private var postRotationStableTicks: Int = 0

    fun submitRaw(sample: PacMazeRawJoystickSample) {
        raw = sample
    }

    fun reset() {
        raw = PacMazeRawJoystickSample.Released
        generation = 0L
        committed = null
        pendingSector = null
        hysteresisSector = null
        sectorStableTicks = 0
        recentSectorChangeTicks.clear()
        lastAngleDeg = null
        angleDeltaSamples.clear()
        spinLocked = false
        postRotationStableTicks = 0
    }

    fun advanceTick(tick: Long): PacMazeTickInput {
        if (!raw.fingerDown) {
            clearAngleTracking()
            hysteresisSector = null
            spinLocked = false
            postRotationStableTicks = 0
            return PacMazeTickInput.Inactive.copy(tick = tick, generation = generation)
        }

        val strength = raw.strength.coerceIn(0f, 1f)
        recordAngleDelta(tick, raw.offsetX, raw.offsetY, strength)

        val sector = joystickResolveSector(
            offsetX = raw.offsetX,
            offsetY = raw.offsetY,
            strength = strength,
            deadZone = config.deadZone,
            previous = hysteresisSector,
        )
        if (sector != null) {
            hysteresisSector = sector
        } else {
            hysteresisSector = null
        }

        if (sector == null) {
            sectorStableTicks = 0
            pendingSector = null
            trimSectorWindow(tick)
            if (spinLocked) {
                postRotationStableTicks++
            }
            updateSpinLock(tick, strength, sector = null)
            if (spinLocked) {
                return spinInput(tick, strength, facing = pendingSector)
            }
            return PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.DeadZone,
                strength = strength,
                sector = null,
                facing = pendingSector ?: committed,
                committed = null,
            )
        }

        if (sector != pendingSector) {
            pendingSector = sector
            sectorStableTicks = 1
            recentSectorChangeTicks.addLast(tick)
            postRotationStableTicks = 0
        } else {
            sectorStableTicks++
            postRotationStableTicks++
        }
        trimSectorWindow(tick)

        if (shouldEnterSpinLock()) {
            spinLocked = true
        }
        updateSpinLock(tick, strength, sector)

        if (spinLocked) {
            return spinInput(tick, strength, facing = sector)
        }

        val shouldCommit = when {
            strength >= config.commitThreshold -> true
            sectorStableTicks >= config.stableTicksRequired -> true
            strength >= config.softCommitThreshold && sectorStableTicks >= config.stableTicksRequired -> true
            else -> false
        }

        if (shouldCommit) {
            if (committed != sector) {
                generation++
                committed = sector
            }
            recentSectorChangeTicks.clear()
            angleDeltaSamples.clear()
            return PacMazeTickInput(
                tick = tick,
                generation = generation,
                active = true,
                mode = PacMazeInputMode.Committed,
                strength = strength,
                sector = sector,
                facing = sector,
                committed = sector,
            )
        }

        return pendingInput(tick, strength, sector)
    }

    fun replay(samplesByTick: Map<Long, PacMazeRawJoystickSample>): Map<Long, PacMazeTickInput> {
        reset()
        return samplesByTick.keys.sorted().associateWith { tick ->
            submitRaw(samplesByTick.getValue(tick))
            advanceTick(tick)
        }
    }

    private fun spinInput(tick: Long, strength: Float, facing: Direction?): PacMazeTickInput =
        PacMazeTickInput(
            tick = tick,
            generation = generation,
            active = true,
            mode = PacMazeInputMode.Spin,
            strength = strength,
            sector = facing,
            facing = facing,
            committed = null,
        )

    private fun pendingInput(tick: Long, strength: Float, sector: Direction): PacMazeTickInput =
        PacMazeTickInput(
            tick = tick,
            generation = generation,
            active = true,
            mode = PacMazeInputMode.Pending,
            strength = strength,
            sector = sector,
            facing = sector,
            committed = null,
        )

    private fun updateSpinLock(tick: Long, strength: Float, sector: Direction?) {
        if (!spinLocked) return

        val rotatingNow = isRotatingNow(tick)
        if (rotatingNow) {
            postRotationStableTicks = 0
        }

        val breakout = sector != null &&
            strength >= config.spinBreakoutStrength &&
            postRotationStableTicks >= config.spinBreakoutStableTicks
        val released = !rotatingNow &&
            postRotationStableTicks >= config.spinReleaseStableTicks

        if (breakout || released) {
            spinLocked = false
            if (breakout) {
                recentSectorChangeTicks.clear()
                angleDeltaSamples.clear()
            }
        }
    }

    /** 是否应进入旋转锁定（看整个滑动窗口）。 */
    private fun shouldEnterSpinLock(): Boolean =
        angleTravelInWindow() >= config.spinAngleDeg ||
            recentSectorChangeTicks.size >= config.spinSectorChanges

    /** 当前是否仍在转（只看最近几 tick，用于解除锁定）。 */
    private fun isRotatingNow(tick: Long): Boolean {
        if (angleTravelSince(tick - 2L) >= 15f) return true
        val lastSectorChange = recentSectorChangeTicks.lastOrNull() ?: return false
        return tick == lastSectorChange
    }

    private fun recordAngleDelta(tick: Long, offsetX: Float, offsetY: Float, strength: Float) {
        if (strength < config.deadZone) {
            lastAngleDeg = null
            return
        }
        val angleDeg = joystickSampleAngleDeg(offsetX, offsetY)
        val previous = lastAngleDeg
        if (previous != null) {
            val delta = joystickAngleDeltaDeg(previous, angleDeg)
            if (delta >= 0.8f) {
                angleDeltaSamples.addLast(tick to delta)
            }
        }
        lastAngleDeg = angleDeg
        trimAngleSamples(tick)
    }

    private fun trimAngleSamples(tick: Long) {
        val minTick = tick - config.spinWindowTicks
        while (angleDeltaSamples.isNotEmpty() && angleDeltaSamples.first().first < minTick) {
            angleDeltaSamples.removeFirst()
        }
    }

    private fun trimSectorWindow(tick: Long) {
        val minTick = tick - config.spinWindowTicks
        while (recentSectorChangeTicks.isNotEmpty() && recentSectorChangeTicks.first() < minTick) {
            recentSectorChangeTicks.removeFirst()
        }
    }

    private fun angleTravelInWindow(): Float =
        angleDeltaSamples.sumOf { it.second.toDouble() }.toFloat()

    private fun angleTravelSince(sinceTick: Long): Float =
        angleDeltaSamples.filter { it.first >= sinceTick }.sumOf { it.second.toDouble() }.toFloat()

    private fun clearAngleTracking() {
        lastAngleDeg = null
        angleDeltaSamples.clear()
    }
}
