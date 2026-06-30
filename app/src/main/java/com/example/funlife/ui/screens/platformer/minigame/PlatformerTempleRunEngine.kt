package com.example.funlife.ui.screens.platformer.minigame

import kotlin.math.abs
import kotlin.random.Random

/**
 * 神庙跑酷：三车道 procedurally 生成的赛道与障碍序列。
 * dist 从远 (≈1.4) 向近 (≈0.72) 滚动，玩家在 dist≈0.82 处。
 */
object PlatformerTempleRunEngine {

    const val LANE_COUNT = 3
    private const val PLAYER_DIST = 0.82f
    private const val SPAWN_FAR = 1.35f
    private const val DESPAWN_NEAR = 0.68f

    enum class ObstacleKind { BLOCK, LOW_BAR, PIT, COIN, GEM }

    data class Obstacle(
        val id: Int,
        val lane: Int,
        var dist: Float,
        val kind: ObstacleKind,
    )

    data class TrackDecor(
        val side: Int,
        var dist: Float,
        val kind: Int,
    )

    data class State(
        var lane: Int = 1,
        var jumpOffset: Float = 0f,
        var jumpVel: Float = 0f,
        var sliding: Boolean = false,
        var slideTicks: Int = 0,
        var score: Int = 0,
        var coins: Int = 0,
        var speed: Float = 0.011f,
        var tick: Int = 0,
        var gameOver: Boolean = false,
        var patternCursor: Int = 0,
        var nextPatternAt: Int = 0,
        var nextId: Int = 1,
        val obstacles: MutableList<Obstacle> = mutableListOf(),
        val decors: MutableList<TrackDecor> = mutableListOf(),
    )

    private data class PatternEntry(val lane: Int, val kind: ObstacleKind, val distOffset: Float = 0f)

    /** 预置障碍波次：跳跃 / 滑铲 / 换道 / 收集 */
    private val WAVES: List<List<PatternEntry>> = listOf(
        listOf(PatternEntry(1, ObstacleKind.BLOCK)),
        listOf(PatternEntry(0, ObstacleKind.COIN), PatternEntry(2, ObstacleKind.COIN)),
        listOf(PatternEntry(1, ObstacleKind.LOW_BAR)),
        listOf(PatternEntry(0, ObstacleKind.BLOCK), PatternEntry(2, ObstacleKind.BLOCK)),
        listOf(PatternEntry(1, ObstacleKind.PIT)),
        listOf(PatternEntry(0, ObstacleKind.LOW_BAR), PatternEntry(2, ObstacleKind.COIN)),
        listOf(PatternEntry(1, ObstacleKind.GEM)),
        listOf(PatternEntry(2, ObstacleKind.BLOCK), PatternEntry(0, ObstacleKind.COIN, 0.06f)),
        listOf(PatternEntry(1, ObstacleKind.BLOCK), PatternEntry(0, ObstacleKind.COIN, 0.04f), PatternEntry(2, ObstacleKind.COIN, 0.04f)),
        listOf(PatternEntry(1, ObstacleKind.LOW_BAR), PatternEntry(0, ObstacleKind.BLOCK, 0.12f)),
        listOf(PatternEntry(0, ObstacleKind.PIT), PatternEntry(2, ObstacleKind.COIN, 0.08f)),
        listOf(PatternEntry(1, ObstacleKind.GEM), PatternEntry(0, ObstacleKind.LOW_BAR, 0.1f), PatternEntry(2, ObstacleKind.LOW_BAR, 0.1f)),
    )

    fun reset(): State = State(nextPatternAt = 60)

    fun moveLeft(state: State) {
        if (state.gameOver) return
        state.lane = (state.lane - 1).coerceAtLeast(0)
    }

    fun moveRight(state: State) {
        if (state.gameOver) return
        state.lane = (state.lane + 1).coerceAtMost(LANE_COUNT - 1)
    }

    fun jump(state: State) {
        if (state.gameOver || state.sliding) return
        if (state.jumpOffset <= 0.01f) {
            state.jumpVel = 0.038f
        }
    }

    fun slide(state: State) {
        if (state.gameOver || state.jumpOffset > 0.05f) return
        if (!state.sliding) {
            state.sliding = true
            state.slideTicks = 42
        }
    }

    fun tick(state: State) {
        if (state.gameOver) return
        state.tick++
        state.score++

        if (state.tick % 120 == 0) {
            state.speed = (state.speed + 0.0012f).coerceAtMost(0.022f)
        }

        if (state.sliding) {
            state.slideTicks--
            if (state.slideTicks <= 0) state.sliding = false
        }

        if (state.jumpVel != 0f || state.jumpOffset > 0f) {
            state.jumpOffset += state.jumpVel
            state.jumpVel -= 0.0028f
            if (state.jumpOffset <= 0f) {
                state.jumpOffset = 0f
                state.jumpVel = 0f
            }
        }

        if (state.tick >= state.nextPatternAt) {
            spawnWave(state)
            val gap = (110 - state.tick / 15).coerceIn(55, 110)
            state.nextPatternAt = state.tick + gap + Random.nextInt(0, 25)
        }

        if (state.tick % 35 == 0) {
            decors(state, side = 0, kind = state.tick / 70 % 3)
            decors(state, side = 2, kind = (state.tick / 70 + 1) % 3)
        }

        val scroll = state.speed
        state.obstacles.forEach { it.dist -= scroll }
        state.decors.forEach { it.dist -= scroll * 0.85f }

        val collected = state.obstacles.filter { obs ->
            obs.lane == state.lane &&
                abs(obs.dist - PLAYER_DIST) < 0.045f &&
                (obs.kind == ObstacleKind.COIN || obs.kind == ObstacleKind.GEM) &&
                canPass(state, obs.kind)
        }
        collected.forEach { obs ->
            if (obs.kind == ObstacleKind.GEM) {
                state.coins += 5
                state.score += 50
            } else {
                state.coins++
                state.score += 10
            }
        }
        state.obstacles.removeAll(collected.toSet())

        val hit = state.obstacles.any { obs ->
            obs.lane == state.lane &&
                abs(obs.dist - PLAYER_DIST) < 0.05f &&
                obs.kind != ObstacleKind.COIN &&
                obs.kind != ObstacleKind.GEM &&
                !canPass(state, obs.kind)
        }
        if (hit) state.gameOver = true

        state.obstacles.removeAll { it.dist < DESPAWN_NEAR }
        state.decors.removeAll { it.dist < DESPAWN_NEAR - 0.1f }
    }

    private fun canPass(state: State, kind: ObstacleKind): Boolean = when (kind) {
        ObstacleKind.COIN, ObstacleKind.GEM -> true
        ObstacleKind.BLOCK, ObstacleKind.PIT -> state.jumpOffset > 0.12f
        ObstacleKind.LOW_BAR -> state.sliding
    }

    private fun spawnWave(state: State) {
        val wave = WAVES[state.patternCursor % WAVES.size]
        state.patternCursor++
        wave.forEach { entry ->
            state.obstacles += Obstacle(
                id = state.nextId++,
                lane = entry.lane.coerceIn(0, LANE_COUNT - 1),
                dist = SPAWN_FAR + entry.distOffset,
                kind = entry.kind,
            )
        }
    }

    private fun decors(state: State, side: Int, kind: Int) {
        state.decors += TrackDecor(side = side, dist = SPAWN_FAR + 0.1f, kind = kind)
    }

    /** 透视车道中心 x（0~1），dist 越大越远（靠屏幕上方）。 */
    fun laneCenterX(lane: Int, dist: Float): Float {
        val t = ((dist - 0.65f) / 0.75f).coerceIn(0f, 1f)
        val spread = 0.12f + t * 0.22f
        val center = 0.5f
        return center + (lane - 1) * spread
    }

    fun laneWidth(dist: Float): Float {
        val t = ((dist - 0.65f) / 0.75f).coerceIn(0f, 1f)
        return 0.14f + t * 0.12f
    }
}
