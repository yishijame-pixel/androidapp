package com.example.funlife.social.game.engine.pacmaze

import kotlin.math.abs

/** 渲染环用的世界态快照（仅插值所需字段，避免 tick 后引用被覆盖）。 */
fun PacMazeWorldState.renderInterpolationSnapshot(): PacMazeWorldState = copy(
    tiles = tiles.copyOf(),
    entities = entities.map { it.copy() },
    projectiles = projectiles.map { it.copy() },
    enemyBullets = enemyBullets.map { it.copy() },
)

private const val RENDER_MOVE_EPS = 1e-4f

/** 本机玩家相对 [before] 是否有位移（贴墙空转 tick 时不插值）。 */
fun PacMazeWorldState.playerPacMovedFrom(before: PacMazeWorldState?): Boolean {
    if (before == null) return true
    val afterPac = entities.firstOrNull { it.isPlayerPac() } ?: return false
    val beforePac = before.entities.firstOrNull { it.id == afterPac.id } ?: return true
    return abs(afterPac.x - beforePac.x) > RENDER_MOVE_EPS ||
        abs(afterPac.y - beforePac.y) > RENDER_MOVE_EPS
}