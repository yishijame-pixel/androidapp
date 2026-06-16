package com.example.funlife.social.game.engine.pacmaze

import java.util.ArrayDeque
import kotlin.math.abs

/**
 * 幽灵网格 BFS：绕墙最短路径，避免贪心曼哈顿在走廊/回廊里卡死。
 */
object PacMazeGhostPathfind {

    const val MAX_BFS_NODES = 420
    const val STUCK_ESCAPE_TICKS = 12
    const val STUCK_FORCE_TICKS = 36

    /** 从当前格到 [targetX],[targetY] 的第一步方向；不可达时走向最近可达格。 */
    fun nextStepToward(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        targetX: Int,
        targetY: Int,
        options: List<Direction>,
        allowReverse: Boolean,
    ): Direction? {
        if (options.isEmpty()) return null
        val startX = PacMazeMotion.tileX(ghost.x)
        val startY = PacMazeMotion.tileY(ghost.y)
        if (startX == targetX && startY == targetY) {
            return preferStraight(options, ghost.direction)
        }

        val blockedReverse = if (allowReverse) null else ghost.direction?.opposite()
        val visit = BooleanArray(state.width * state.height)
        val firstStep = IntArray(state.width * state.height) { -1 }
        val queue = ArrayDeque<Pair<Int, Int>>()

        fun idx(x: Int, y: Int) = y * state.width + x
        fun enqueue(x: Int, y: Int, stepDir: Int) {
            val i = idx(x, y)
            if (visit[i]) return
            visit[i] = true
            firstStep[i] = stepDir
            queue.add(x to y)
        }

        enqueue(startX, startY, -1)

        var nodes = 0
        var bestX = startX
        var bestY = startY
        var bestDist = abs(startX - targetX) + abs(startY - targetY)
        var foundTarget = false

        while (queue.isNotEmpty() && nodes < MAX_BFS_NODES) {
            val (cx, cy) = queue.removeFirst()
            nodes++
            val dist = abs(cx - targetX) + abs(cy - targetY)
            if (dist < bestDist) {
                bestDist = dist
                bestX = cx
                bestY = cy
            }
            if (cx == targetX && cy == targetY) {
                foundTarget = true
                break
            }

            for (dir in Direction.entries) {
                if (!allowReverse && dir == blockedReverse) continue
                val (dx, dy) = dir.delta()
                val nx = cx + dx
                val ny = cy + dy
                if (nx !in 0 until state.width || ny !in 0 until state.height) continue
                if (!PacMazeRules.isWalkable(state, nx, ny, forGhost = true, ghost = ghost)) continue
                val i = idx(nx, ny)
                if (visit[i]) continue
                val step = if (cx == startX && cy == startY) dir.ordinal else firstStep[idx(cx, cy)]
                enqueue(nx, ny, step)
            }
        }

        val goalX = if (foundTarget) targetX else bestX
        val goalY = if (foundTarget) targetY else bestY
        if (goalX == startX && goalY == startY) return null

        val stepOrdinal = firstStep[idx(goalX, goalY)]
        if (stepOrdinal < 0) return null
        val dir = Direction.entries[stepOrdinal]
        return if (dir in options) dir else null
    }

    /** 恐惧模式：尽量远离玩家。 */
    fun nextStepAway(
        state: PacMazeWorldState,
        ghost: PacMazeEntity,
        fromX: Int,
        fromY: Int,
        options: List<Direction>,
    ): Direction? {
        if (options.isEmpty()) return null
        return options.maxByOrNull { dir ->
            val (dx, dy) = dir.delta()
            val nx = PacMazeMotion.tileX(ghost.x) + dx
            val ny = PacMazeMotion.tileY(ghost.y) + dy
            if (!PacMazeRules.isWalkable(state, nx, ny, forGhost = true, ghost = ghost)) {
                Int.MIN_VALUE / 4
            } else {
                abs(nx - fromX) + abs(ny - fromY)
            }
        }
    }

    private fun preferStraight(options: List<Direction>, current: Direction?): Direction? {
        if (current != null && current in options) return current
        return options.firstOrNull()
    }
}
