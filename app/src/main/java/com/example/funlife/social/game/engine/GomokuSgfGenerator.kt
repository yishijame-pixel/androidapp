package com.example.funlife.social.game.engine

/**
 * 五子棋棋谱生成器（SGF 格式）
 *
 * SGF (Smart Game Format) 是通用的棋类棋谱格式
 */
object GomokuSgfGenerator {

    /**
     * 生成 SGF 格式棋谱
     *
     * @param moves 落子记录列表 [(pbId, x, y), ...]
     * @param blackPbId 黑方 pbId
     * @param whitePbId 白方 pbId
     * @param blackName 黑方昵称
     * @param whiteName 白方昵称
     * @param result 对局结果 (B+胜/W+胜/平局)
     * @param date 对局日期
     */
    fun generate(
        moves: List<Triple<String, Int, Int>>,
        blackPbId: String,
        whitePbId: String,
        blackName: String = "Black",
        whiteName: String = "White",
        result: String? = null,
        date: String? = null,
    ): String {
        val sb = StringBuilder()

        // SGF 头部
        sb.append("(;GM[4]")  // GM[4] = 五子棋
        sb.append("FF[4]")   // SGF 格式版本
        sb.append("SZ[${GomokuRules.SIZE}]")  // 棋盘大小
        sb.append("PB[$blackName]")  // 黑方名称
        sb.append("PW[$whiteName]")  // 白方名称

        date?.let { sb.append("DT[$it]") }
        result?.let { sb.append("RE[$it]") }

        sb.append("\n")

        // 落子记录
        moves.forEachIndexed { index, (pbId, x, y) ->
            val color = if (pbId == blackPbId) "B" else "W"
            val coord = coordToSgf(x, y)
            sb.append(";$color[$coord]")
            if ((index + 1) % 10 == 0) sb.append("\n")
        }

        sb.append(")")
        return sb.toString()
    }

    /**
     * 从 SGF 解析落子记录
     */
    fun parse(sgf: String): ParsedSgf? {
        return try {
            val moves = mutableListOf<SgfMove>()
            var blackName = "Black"
            var whiteName = "White"
            var result: String? = null
            var boardSize = 15

            // 解析头部信息
            val pbMatch = Regex("""PB\[([^\]]*)]""").find(sgf)
            val pwMatch = Regex("""PW\[([^\]]*)]""").find(sgf)
            val reMatch = Regex("""RE\[([^\]]*)]""").find(sgf)
            val szMatch = Regex("""SZ\[(\d+)]""").find(sgf)

            pbMatch?.let { blackName = it.groupValues[1] }
            pwMatch?.let { whiteName = it.groupValues[1] }
            reMatch?.let { result = it.groupValues[1] }
            szMatch?.let { boardSize = it.groupValues[1].toIntOrNull() ?: 15 }

            // 解析落子
            val movePattern = Regex(""";([BW])\[([a-o])([a-o])]""")
            movePattern.findAll(sgf).forEach { match ->
                val color = if (match.groupValues[1] == "B") {
                    GomokuRules.CELL_BLACK
                } else {
                    GomokuRules.CELL_WHITE
                }
                val (x, y) = sgfToCoord(match.groupValues[2] + match.groupValues[3])
                moves.add(SgfMove(x, y, color))
            }

            ParsedSgf(
                moves = moves,
                blackName = blackName,
                whiteName = whiteName,
                result = result,
                boardSize = boardSize,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 坐标转 SGF 格式 (0-14 -> a-o)
     */
    private fun coordToSgf(x: Int, y: Int): String {
        val xChar = ('a' + x).toString()
        val yChar = ('a' + y).toString()
        return "$xChar$yChar"
    }

    /**
     * SGF 格式转坐标
     */
    private fun sgfToCoord(sgf: String): Pair<Int, Int> {
        require(sgf.length == 2)
        val x = sgf[0] - 'a'
        val y = sgf[1] - 'a'
        return x to y
    }

    /**
     * 从 GameMoveDto 列表生成 SGF
     */
    fun fromMoves(
        moves: List<com.example.funlife.social.game.model.GameMoveDto>,
        blackPbId: String,
        whitePbId: String,
        blackName: String = "Black",
        whiteName: String = "White",
        winnerPbId: String? = null,
    ): String {
        val placements = moves.mapNotNull { move ->
            val payload = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            if (payload.get("kind")?.asString != "gomoku_place") return@mapNotNull null
            val x = payload.get("x")?.asInt ?: return@mapNotNull null
            val y = payload.get("y")?.asInt ?: return@mapNotNull null
            Triple(move.playerPbId, x, y)
        }.sortedBy { moves.indexOfFirst { m -> m.playerPbId == it.first && it.second == m.payload?.asJsonObject?.get("x")?.asInt } }

        val result = when (winnerPbId) {
            blackPbId -> "B+Win"
            whitePbId -> "W+Win"
            null -> "Draw"
            else -> null
        }

        return generate(
            moves = placements,
            blackPbId = blackPbId,
            whitePbId = whitePbId,
            blackName = blackName,
            whiteName = whiteName,
            result = result,
            date = java.time.LocalDate.now().toString(),
        )
    }
}

/**
 * SGF 落子记录
 */
data class SgfMove(
    val x: Int,
    val y: Int,
    val color: Char,
)

/**
 * 解析后的 SGF 数据
 */
data class ParsedSgf(
    val moves: List<SgfMove>,
    val blackName: String,
    val whiteName: String,
    val result: String?,
    val boardSize: Int,
)
