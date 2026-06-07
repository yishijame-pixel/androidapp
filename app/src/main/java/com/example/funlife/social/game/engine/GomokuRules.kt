package com.example.funlife.social.game.engine

/**
 * 五子棋纯规则引擎（无 Android 依赖，可单测）。
 * 15×15，黑先，五连即胜；禁手/长连首期不做。
 */
object GomokuRules {
    const val SIZE = 15
    const val CELL_EMPTY = '.'
    const val CELL_BLACK = 'B'
    const val CELL_WHITE = 'W'
    const val MAX_MOVES = 200

    fun emptyBoard(): String = CELL_EMPTY.toString().repeat(SIZE * SIZE)

    fun index(x: Int, y: Int): Int = y * SIZE + x

    fun cell(board: String, x: Int, y: Int): Char {
        if (x !in 0 until SIZE || y !in 0 until SIZE) return CELL_EMPTY
        return board.getOrElse(index(x, y)) { CELL_EMPTY }
    }

    fun isEmpty(board: String, x: Int, y: Int): Boolean = cell(board, x, y) == CELL_EMPTY

    fun validateMove(board: String, x: Int, y: Int, color: Char): Boolean {
        if (color != CELL_BLACK && color != CELL_WHITE) return false
        if (x !in 0 until SIZE || y !in 0 until SIZE) return false
        return isEmpty(board, x, y)
    }

    fun applyMove(board: String, x: Int, y: Int, color: Char): String {
        require(validateMove(board, x, y, color)) { "非法落子" }
        val chars = board.toCharArray()
        chars[index(x, y)] = color
        return String(chars)
    }

    fun colorForPbId(blackPbId: String, whitePbId: String, pbId: String): Char? = when (pbId) {
        blackPbId -> CELL_BLACK
        whitePbId -> CELL_WHITE
        else -> null
    }

    fun opponentColor(color: Char): Char = if (color == CELL_BLACK) CELL_WHITE else CELL_BLACK

    fun pbIdForColor(blackPbId: String, whitePbId: String, color: Char): String? = when (color) {
        CELL_BLACK -> blackPbId
        CELL_WHITE -> whitePbId
        else -> null
    }

    fun checkWinner(board: String, lastX: Int, lastY: Int): Char? {
        val c = cell(board, lastX, lastY)
        if (c == CELL_EMPTY) return null
        val dirs = listOf(
            1 to 0,
            0 to 1,
            1 to 1,
            1 to -1,
        )
        for ((dx, dy) in dirs) {
            var count = 1
            var nx = lastX + dx
            var ny = lastY + dy
            while (cell(board, nx, ny) == c) {
                count++
                nx += dx
                ny += dy
            }
            nx = lastX - dx
            ny = lastY - dy
            while (cell(board, nx, ny) == c) {
                count++
                nx -= dx
                ny -= dy
            }
            if (count >= 5) return c
        }
        return null
    }

    fun isDraw(board: String, moveCount: Int): Boolean =
        moveCount >= MAX_MOVES || board.none { it == CELL_EMPTY }

    /** 从 move 记录回放棋盘，作为 game_state 的兜底同步。 */
    fun replayFromMoves(
        blackPbId: String,
        whitePbId: String,
        moves: List<Pair<String, Pair<Int, Int>>>,
    ): Pair<String, Int> {
        var board = emptyBoard()
        var count = 0
        moves.forEach { (pbId, pos) ->
            val color = colorForPbId(blackPbId, whitePbId, pbId) ?: return@forEach
            val (x, y) = pos
            if (validateMove(board, x, y, color)) {
                board = applyMove(board, x, y, color)
                count++
            }
        }
        return board to count
    }
}
