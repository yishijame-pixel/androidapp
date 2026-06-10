package com.example.funlife.social.game.engine.pacmaze

/**
 * 程序生成完美迷宫（Recursive Backtracker），输出与关卡 JSON 兼容的字符串。
 */
object PacMazeMazeGenerator {

    fun buildLevelJson(seed: Long, width: Int = 15, height: Int = 15): String {
        val w = (width.coerceAtLeast(9) or 1)
        val h = (height.coerceAtLeast(9) or 1)
        val rng = PacMazeDeterministicRng(seed)
        val cells = Array(h) { BooleanArray(w) }
        carve(cells, w, h, 1, 1, rng)

        val grid = Array(h) { y ->
            CharArray(w) { x -> if (cells[y][x]) '.' else '#' }
        }

        val startX = 1
        val startY = 1
        val exitX = w - 2
        val exitY = h - 2
        grid[startY][startX] = '.'
        grid[exitY][exitX] = 'E'

        val gridJson = grid.joinToString(
            prefix = "[\n    \"",
            postfix = "\"\n  ]",
            separator = "\",\n    \"",
        ) { String(it) }

        return """
            {
              "id": 0,
              "name": "程序迷宫",
              "width": $w,
              "height": $h,
              "grid": $gridJson,
              "spawn": {
                "pac": [$startX, $startY],
                "ghosts": [[${w / 2}, ${h / 2}]]
              },
              "difficulty": { "ghost_speed_mul": 0.65, "ai_aggression": 0.35 },
              "markers": [
                { "type": "start", "x": $startX, "y": $startY, "label": "起点" },
                { "type": "exit", "x": $exitX, "y": $exitY, "label": "出口" }
              ],
              "modeRules": {
                "winCondition": "reach_exit",
                "timeLimitSeconds": 180
              },
              "starCriteria": {
                "twoStarMinScore": 200,
                "threeStarMinScore": 500,
                "threeStarMaxSeconds": 90,
                "threeStarNoDeath": true
              }
            }
        """.trimIndent()
    }

    private fun carve(
        cells: Array<BooleanArray>,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
        rng: PacMazeDeterministicRng,
    ) {
        cells[y][x] = true
        for (dir in shuffleDirections(rng)) {
            val (dx, dy) = dir.delta()
            val nx = x + dx * 2
            val ny = y + dy * 2
            if (nx in 1 until w - 1 && ny in 1 until h - 1 && !cells[ny][nx]) {
                cells[y + dy][x + dx] = true
                carve(cells, w, h, nx, ny, rng)
            }
        }
    }

    private fun shuffleDirections(rng: PacMazeDeterministicRng): List<Direction> {
        val list = Direction.entries.toMutableList()
        for (i in list.lastIndex downTo 1) {
            val j = rng.nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}
