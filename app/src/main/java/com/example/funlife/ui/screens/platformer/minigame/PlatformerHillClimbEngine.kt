package com.example.funlife.ui.screens.platformer.minigame

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 登山挑战：程序化山地赛道 + 简易车辆物理（车身/双轮素材驱动）。
 */
object PlatformerHillClimbEngine {

    private const val GRAVITY = 0.00055f
    private const val GAS_FORCE = 0.0014f
    private const val BRAKE_FORCE = 0.0009f
    private const val MAX_VEL = 0.045f
    private const val WHEEL_BASE = 0.14f
    private const val TERRAIN_STEP = 0.025f

    data class TerrainPoint(val x: Float, val y: Float)

    data class State(
        var carX: Float = 0.12f,
        var carY: Float = 0f,
        var angle: Float = 0f,
        var velocity: Float = 0f,
        var distance: Float = 0f,
        var fuel: Float = 100f,
        var tick: Int = 0,
        var gameOver: Boolean = false,
        var flipped: Boolean = false,
        val terrain: MutableList<TerrainPoint> = mutableListOf(),
        var terrainSeed: Long = 42L,
    )

    fun reset(): State {
        val s = State()
        ensureTerrain(s, 0f, 2.5f)
        s.carY = sampleHeight(s, s.carX)
        s.angle = terrainAngle(s, s.carX)
        return s
    }

    fun tick(state: State, gas: Boolean, brake: Boolean) {
        if (state.gameOver) return
        state.tick++

        if (gas && state.fuel > 0f) {
            state.velocity = (state.velocity + GAS_FORCE).coerceAtMost(MAX_VEL)
            state.fuel = (state.fuel - 0.045f).coerceAtLeast(0f)
        } else if (brake) {
            state.velocity = (state.velocity - BRAKE_FORCE).coerceAtLeast(0f)
        } else {
            state.velocity = (state.velocity - GRAVITY * 0.35f).coerceAtLeast(0f)
        }

        val slope = terrainAngle(state, state.carX)
        state.velocity = (state.velocity - sin(slope) * GRAVITY * 1.6f).coerceIn(-0.018f, MAX_VEL)

        state.carX += state.velocity
        state.distance = state.carX * 420f

        ensureTerrain(state, state.carX - 0.05f, state.carX + 1.8f)
        trimTerrain(state, state.carX - 0.4f)

        val groundY = sampleHeight(state, state.carX)
        val frontY = sampleHeight(state, state.carX + WHEEL_BASE * 0.5f)
        val backY = sampleHeight(state, state.carX - WHEEL_BASE * 0.5f)
        state.carY = (frontY + backY) / 2f
        state.angle = atan2(frontY - backY, WHEEL_BASE)

        if (abs(state.angle) > 1.35f) {
            state.flipped = true
            state.gameOver = true
        }
        if (state.carY - groundY > 0.25f && state.velocity > 0.01f) {
            state.flipped = true
            state.gameOver = true
        }
        if (state.fuel <= 0f && abs(state.velocity) < 0.0005f && isSteep(state, state.carX)) {
            state.gameOver = true
        }
    }

    fun sampleHeight(state: State, x: Float): Float {
        if (state.terrain.isEmpty()) return 0.72f
        if (x <= state.terrain.first().x) return state.terrain.first().y
        if (x >= state.terrain.last().x) return state.terrain.last().y
        var i = 0
        while (i < state.terrain.size - 1 && state.terrain[i + 1].x < x) i++
        val a = state.terrain[i]
        val b = state.terrain[i + 1]
        val t = ((x - a.x) / (b.x - a.x).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
        return a.y + (b.y - a.y) * smooth(t)
    }

    private fun smooth(t: Float): Float = t * t * (3f - 2f * t)

    private fun terrainAngle(state: State, x: Float): Float {
        val dx = 0.02f
        val y0 = sampleHeight(state, x - dx)
        val y1 = sampleHeight(state, x + dx)
        return atan2(y1 - y0, dx * 2f)
    }

    private fun isSteep(state: State, x: Float): Boolean = abs(terrainAngle(state, x)) > 0.55f

    private fun ensureTerrain(state: State, fromX: Float, toX: Float) {
        if (state.terrain.isEmpty()) {
            state.terrain += TerrainPoint(0f, 0.72f)
        }
        var x = state.terrain.last().x
        if (x < fromX) x = fromX
        val rng = Random(state.terrainSeed + (x * 1000).toLong())
        while (x < toX) {
            x += TERRAIN_STEP
            val segment = ((x * 420f).toInt() / 80) % 6
            val base = 0.72f
            val y = when (segment) {
                0 -> base + sin(x * 8f) * 0.015f
                1 -> base - (x % 0.3f) * 0.08f
                2 -> base + (x % 0.25f) * 0.12f
                3 -> base + sin(x * 14f) * 0.025f + rng.nextFloat() * 0.01f
                4 -> base - 0.06f + sin(x * 6f) * 0.04f
                else -> base + cos(x * 10f) * 0.02f
            }.coerceIn(0.38f, 0.82f)
            state.terrain += TerrainPoint(x, y)
        }
    }

    private fun trimTerrain(state: State, minX: Float) {
        while (state.terrain.size > 2 && state.terrain[1].x < minX) {
            state.terrain.removeAt(0)
        }
    }

    /** 视口内绘制地形的 x 范围（归一化 0~1）。 */
    fun visibleRange(state: State): Pair<Float, Float> {
        val viewW = 0.55f
        val left = (state.carX - 0.18f).coerceAtLeast(0f)
        return left to (left + viewW)
    }

    fun wheelBase(): Float = WHEEL_BASE

    fun cameraOffset(state: State): Float = state.carX - 0.22f
}
