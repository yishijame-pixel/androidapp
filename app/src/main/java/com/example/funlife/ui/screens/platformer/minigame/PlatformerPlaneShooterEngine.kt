package com.example.funlife.ui.screens.platformer.minigame

import kotlin.math.abs
import kotlin.random.Random

/** 天空射击：波次敌机、弹幕、星野 parallax。 */
object PlatformerPlaneShooterEngine {

    private const val PLAYER_Y = 0.78f

    enum class EnemyKind(val hp: Int, val speed: Float, val score: Int, val radius: Float) {
        SCOUT(1, 0.014f, 10, 0.032f),
        HEAVY(3, 0.009f, 30, 0.048f),
        DART(1, 0.022f, 15, 0.025f),
    }

    data class Bullet(var x: Float, var y: Float, var vx: Float = 0f, val player: Boolean = true)

    data class Enemy(
        val id: Int,
        var x: Float,
        var y: Float,
        var hp: Int,
        val kind: EnemyKind,
        var vx: Float = 0f,
    )

    data class StarLayer(val speed: Float, var scroll: Float, val stars: List<Pair<Float, Float>>)

    data class State(
        var planeX: Float = 0.5f,
        var score: Int = 0,
        var lives: Int = 3,
        var tick: Int = 0,
        var waveIndex: Int = 0,
        var nextWaveAt: Int = 90,
        var fireCooldown: Int = 0,
        var rapidFireTicks: Int = 0,
        var invincibleTicks: Int = 0,
        var gameOver: Boolean = false,
        var nextEnemyId: Int = 1,
        val bullets: MutableList<Bullet> = mutableListOf(),
        val enemies: MutableList<Enemy> = mutableListOf(),
        val starLayers: List<StarLayer> = buildStarField(),
    )

    private fun buildStarField(): List<StarLayer> = listOf(
        StarLayer(0.0018f, 0f, List(40) { Random.nextFloat() to Random.nextFloat() }),
        StarLayer(0.0032f, 0f, List(28) { Random.nextFloat() to Random.nextFloat() }),
        StarLayer(0.0055f, 0f, List(16) { Random.nextFloat() to Random.nextFloat() }),
    )

    fun reset(): State = State()

    fun setPlaneX(state: State, x: Float) {
        if (state.gameOver) return
        state.planeX = x.coerceIn(0.08f, 0.92f)
    }

    fun fire(state: State) {
        if (state.gameOver) return
        val interval = if (state.rapidFireTicks > 0) 6 else 14
        if (state.fireCooldown > 0) return
        state.fireCooldown = interval
        val spread = if (state.rapidFireTicks > 0) 0.04f else 0f
        state.bullets += Bullet(state.planeX - spread, PLAYER_Y - 0.06f)
        state.bullets += Bullet(state.planeX, PLAYER_Y - 0.06f)
        state.bullets += Bullet(state.planeX + spread, PLAYER_Y - 0.06f)
    }

    fun tick(state: State) {
        if (state.gameOver) return
        state.tick++
        if (state.fireCooldown > 0) state.fireCooldown--
        if (state.rapidFireTicks > 0) state.rapidFireTicks--
        if (state.invincibleTicks > 0) state.invincibleTicks--

        if (state.tick % 12 == 0) fire(state)

        if (state.tick >= state.nextWaveAt) {
            spawnWave(state)
            state.nextWaveAt = state.tick + (130 - state.waveIndex * 4).coerceIn(70, 130)
        }

        state.bullets.forEach { b ->
            b.y -= if (b.player) 0.022f else 0.015f
            b.x += b.vx
        }
        state.bullets.removeAll { it.y < -0.05f || it.y > 1.05f || it.x < -0.05f || it.x > 1.05f }

        state.enemies.forEach { e ->
            e.y += e.kind.speed
            e.x += e.vx
            if (e.x < 0.06f || e.x > 0.94f) e.vx = -e.vx
        }
        state.enemies.removeAll { it.y > 1.08f }

        val hits = mutableListOf<Pair<Bullet, Enemy>>()
        state.bullets.filter { it.player }.forEach { b ->
            state.enemies.forEach { e ->
                if (abs(b.x - e.x) < e.kind.radius && abs(b.y - e.y) < e.kind.radius) {
                    hits += b to e
                }
            }
        }
        hits.distinctBy { it.second.id }.forEach { (_, enemy) ->
            enemy.hp--
            if (enemy.hp <= 0) {
                state.score += enemy.kind.score
                if (Random.nextFloat() < 0.12f) state.rapidFireTicks = 180
            }
        }
        state.bullets.removeAll(hits.map { it.first }.toSet())
        state.enemies.removeAll { it.hp <= 0 }

        if (state.invincibleTicks <= 0) {
            val crashed = state.enemies.any { e ->
                abs(e.x - state.planeX) < (e.kind.radius + 0.06f) &&
                    abs(e.y - PLAYER_Y) < (e.kind.radius + 0.05f)
            }
            if (crashed) {
                state.lives--
                state.invincibleTicks = 90
                if (state.lives <= 0) state.gameOver = true
            }
        }

        scrollStars(state)
    }

    private fun scrollStars(state: State) {
        state.starLayers.forEach { layer ->
            layer.scroll = (layer.scroll + layer.speed) % 1f
        }
    }

    fun starY(layer: StarLayer, starY: Float): Float = (starY + layer.scroll) % 1f

    private fun spawnWave(state: State) {
        val wave = state.waveIndex % 8
        state.waveIndex++
        when (wave) {
            0 -> spawnLine(state, EnemyKind.SCOUT, 5)
            1 -> spawnVFormation(state, EnemyKind.SCOUT)
            2 -> spawnLine(state, EnemyKind.HEAVY, 3)
            3 -> spawnDartSwarm(state)
            4 -> spawnVFormation(state, EnemyKind.HEAVY)
            5 -> {
                spawnLine(state, EnemyKind.SCOUT, 4)
                spawnLine(state, EnemyKind.DART, 3, yStart = -0.05f, xOffset = 0.15f)
            }
            6 -> spawnDiamond(state)
            else -> spawnLine(state, EnemyKind.HEAVY, 2)
        }
    }

    private fun spawnLine(
        state: State,
        kind: EnemyKind,
        count: Int,
        yStart: Float = -0.12f,
        xOffset: Float = 0f,
    ) {
        val step = 0.75f / (count + 1)
        repeat(count) { i ->
            val x = 0.12f + step * (i + 1) + xOffset
            state.enemies += Enemy(
                id = state.nextEnemyId++,
                x = x.coerceIn(0.08f, 0.92f),
                y = yStart - i * 0.04f,
                hp = kind.hp,
                kind = kind,
            )
        }
    }

    private fun spawnVFormation(state: State, kind: EnemyKind) {
        val cx = 0.5f
        listOf(
            cx to -0.1f,
            cx - 0.12f to -0.18f,
            cx + 0.12f to -0.18f,
            cx - 0.22f to -0.26f,
            cx + 0.22f to -0.26f,
        ).forEach { (x, y) ->
            state.enemies += Enemy(state.nextEnemyId++, x, y, kind.hp, kind)
        }
    }

    private fun spawnDartSwarm(state: State) {
        repeat(6) {
            state.enemies += Enemy(
                id = state.nextEnemyId++,
                x = Random.nextFloat().coerceIn(0.1f, 0.9f),
                y = -0.1f - it * 0.03f,
                hp = EnemyKind.DART.hp,
                kind = EnemyKind.DART,
                vx = Random.nextFloat() * 0.008f - 0.004f,
            )
        }
    }

    private fun spawnDiamond(state: State) {
        val pts = listOf(
            0.5f to -0.12f,
            0.38f to -0.2f,
            0.62f to -0.2f,
            0.5f to -0.28f,
        )
        pts.forEach { (x, y) ->
            state.enemies += Enemy(state.nextEnemyId++, x, y, EnemyKind.SCOUT.hp, EnemyKind.SCOUT)
        }
        state.enemies += Enemy(state.nextEnemyId++, 0.5f, -0.36f, EnemyKind.HEAVY.hp, EnemyKind.HEAVY)
    }

    fun playerY(): Float = PLAYER_Y
}
