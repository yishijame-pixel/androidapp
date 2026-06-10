package com.example.funlife.social.game.engine.pacmaze

/**
 * 001 / 002 成对传送门（左右位置不变，竖直跃迁，**保持进入方向**）：
 * - 从 **上方** 向下进入 → 从对侧传送门 **下方** 穿出，继续向下
 * - 从 **下方** 向上进入 → 从对侧传送门 **上方** 穿出，继续向上
 * （002 → 001 对称）
 */
object PacMazePortals {

    data class PortalPair(
        val left: PacMazeMapMarker,
        val right: PacMazeMapMarker,
    )

    fun pairs(markers: List<PacMazeMapMarker>): List<PortalPair> =
        markers
            .filter { it.kind == PacMazeMarkerKind.CHECKPOINT }
            .sortedBy { it.x }
            .chunked(2)
            .mapNotNull { chunk ->
                if (chunk.size == 2) PortalPair(left = chunk[0], right = chunk[1]) else null
            }

    fun applyTransit(
        state: PacMazeWorldState,
        entity: PacMazeEntity,
        level: PacMazeLevelConfig,
    ): PacMazeEntity {
        val dir = entity.direction ?: return entity
        if (dir != Direction.UP && dir != Direction.DOWN) return entity

        val tx = PacMazeMotion.tileX(entity.x)
        val ty = PacMazeMotion.tileY(entity.y)
        val forGhost = entity.role == "ghost"

        for (pair in pairs(level.markers)) {
            val onLeft = tx == pair.left.x && ty == pair.left.y
            val onRight = tx == pair.right.x && ty == pair.right.y
            if (!onLeft && !onRight) continue
            if (!shouldWarp(entity, dir)) continue

            val exitPortal = if (onLeft) pair.right else pair.left
            val dest = resolveExitTile(state, exitPortal, dir, forGhost) ?: continue
            val warped = entity.copy(
                x = dest.first.toFloat(),
                y = dest.second.toFloat(),
                direction = dir,
                facing = dir,
            )
            if (PacMazeMotion.isPositionLegal(state, warped.x, warped.y, forGhost)) {
                return warped
            }
        }
        return entity
    }

    /** 刚进入传送格时触发（按实体中心在格内的竖直位置判断进出方向）。 */
    private fun shouldWarp(entity: PacMazeEntity, dir: Direction): Boolean {
        val ty = PacMazeMotion.tileY(entity.y)
        val frac = PacMazeMotion.centerY(entity.y) - ty
        return when (dir) {
            Direction.DOWN -> frac <= 0.35f
            Direction.UP -> frac >= 0.65f
            else -> false
        }
    }

    private fun resolveExitTile(
        state: PacMazeWorldState,
        exitPortal: PacMazeMapMarker,
        dir: Direction,
        forGhost: Boolean,
    ): Pair<Int, Int>? {
        val primaryY = when (dir) {
            Direction.DOWN -> exitPortal.y + 1
            Direction.UP -> exitPortal.y - 1
            else -> return null
        }
        // 左右边传送门的竖直通道与门在同一列 (x=1 / x=15)，不能偏到内侧一格
        val innerX = if (exitPortal.x < state.width / 2) {
            exitPortal.x + 1
        } else {
            exitPortal.x - 1
        }
        val candidates = buildList {
            add(exitPortal.x to primaryY)
            add(exitPortal.x to exitPortal.y)
            add(innerX to primaryY)
            for (dy in -2..2) {
                add(exitPortal.x to primaryY + dy)
            }
        }
        for ((x, y) in candidates) {
            if (PacMazeRules.isWalkable(state, x, y, forGhost)) return x to y
        }
        return null
    }
}
