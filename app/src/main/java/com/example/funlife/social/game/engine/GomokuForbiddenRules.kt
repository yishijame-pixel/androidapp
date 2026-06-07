package com.example.funlife.social.game.engine

/**
 * 五子棋禁手规则引擎（纯函数，可单测）。
 *
 * 标准禁手规则（仅限黑棋）��
 * - 三三禁手：同时形成两个或以上活三
 * - 四四禁手：同时形成两个或以上四（含冲四、活四）
 * - 长连禁手：六连或以上
 *
 * 白棋无禁手，长连也算胜利。
 */
object GomokuForbiddenRules {

    private const val SIZE = GomokuRules.SIZE
    private const val CELL_BLACK = GomokuRules.CELL_BLACK
    private const val CELL_WHITE = GomokuRules.CELL_WHITE
    private const val CELL_EMPTY = GomokuRules.CELL_EMPTY

    /** 四个方向向量：水平、垂直、对角线、反对角线 */
    private val DIRECTIONS = listOf(
        1 to 0,   // 水平
        0 to 1,   // 垂直
        1 to 1,   // 对角线
        1 to -1,  // 反对角线
    )

    /**
     * 检查黑棋在 (x, y) 落子是否为禁手。
     * @return 禁手类型，null 表示非禁手
     */
    fun checkForbidden(board: String, x: Int, y: Int): ForbiddenType? {
        // 仅黑棋有禁手
        if (!GomokuRules.isEmpty(board, x, y)) return null

        // 模拟落子
        val testBoard = GomokuRules.applyMove(board, x, y, CELL_BLACK)

        // 1. 检查长连禁手（六连或以上）
        if (checkOverline(testBoard, x, y)) {
            return ForbiddenType.OVERLINE
        }

        // 2. 如果形成五连，非禁手（胜利）
        if (GomokuRules.checkWinner(testBoard, x, y) == CELL_BLACK) {
            return null
        }

        // 3. 检查四四禁手
        val fourCount = countFours(testBoard, x, y)
        if (fourCount >= 2) {
            return ForbiddenType.DOUBLE_FOUR
        }

        // 4. 检查三三禁手
        val threeCount = countLiveThrees(testBoard, x, y)
        if (threeCount >= 2) {
            return ForbiddenType.DOUBLE_THREE
        }

        return null
    }

    /**
     * 检查是否为长连（六连或以上）
     */
    private fun checkOverline(board: String, x: Int, y: Int): Boolean {
        for ((dx, dy) in DIRECTIONS) {
            val count = countConsecutive(board, x, y, dx, dy, CELL_BLACK)
            if (count >= 6) return true
        }
        return false
    }

    /**
     * 统计某点在所有方向形成的"四"的数量
     * 包括活四（XOOOO_）和冲四（XOOOOX 一端被堵）
     */
    private fun countFours(board: String, x: Int, y: Int): Int {
        var count = 0
        for ((dx, dy) in DIRECTIONS) {
            val pattern = extractPattern(board, x, y, dx, dy, 6)
            if (isFour(pattern)) count++
        }
        return count
    }

    /**
     * 统计某点在所有方向形成的"活三"的数量
     * 活三：可以形成活四的三（两端都有空位）
     */
    private fun countLiveThrees(board: String, x: Int, y: Int): Int {
        var count = 0
        for ((dx, dy) in DIRECTIONS) {
            if (isLiveThree(board, x, y, dx, dy)) count++
        }
        return count
    }

    /**
     * 统计连续同色棋子数量（包含当前点）
     */
    private fun countConsecutive(
        board: String,
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
        color: Char,
    ): Int {
        var count = 1
        // 正向
        var nx = x + dx
        var ny = y + dy
        while (GomokuRules.cell(board, nx, ny) == color) {
            count++
            nx += dx
            ny += dy
        }
        // 反向
        nx = x - dx
        ny = y - dy
        while (GomokuRules.cell(board, nx, ny) == color) {
            count++
            nx -= dx
            ny -= dy
        }
        return count
    }

    /**
     * 提取某方向的模式字符串（用于模式匹配）
     * 以落子点为中心，向两侧各延伸 radius 格
     */
    private fun extractPattern(
        board: String,
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
        radius: Int,
    ): String {
        val sb = StringBuilder()
        for (i in -radius..radius) {
            val nx = x + dx * i
            val ny = y + dy * i
            val c = when {
                nx !in 0 until SIZE || ny !in 0 until SIZE -> 'X' // 边界视为墙
                else -> GomokuRules.cell(board, nx, ny)
            }
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * 判断模式是否为"四"（含活四、冲四）
     * 四 = 连续四个黑子 + 至少一端有空位可成五
     */
    private fun isFour(pattern: String): Boolean {
        val center = pattern.length / 2
        // 找到包含中心点的连续黑子段
        var start = center
        var end = center
        while (start > 0 && pattern[start - 1] == CELL_BLACK) start--
        while (end < pattern.length - 1 && pattern[end + 1] == CELL_BLACK) end++

        val consecutive = end - start + 1
        if (consecutive != 4) return false

        // 检查两端
        val leftOpen = start > 0 && pattern[start - 1] == CELL_EMPTY
        val rightOpen = end < pattern.length - 1 && pattern[end + 1] == CELL_EMPTY

        // 至少一端开放才算四
        return leftOpen || rightOpen
    }

    /**
     * 判断某方向是否形成活三
     * 活三 = 可以一步形成活四的三
     */
    private fun isLiveThree(
        board: String,
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
    ): Boolean {
        val pattern = extractPattern(board, x, y, dx, dy, 5)
        val center = pattern.length / 2

        // 找连续黑子段
        var start = center
        var end = center
        while (start > 0 && pattern[start - 1] == CELL_BLACK) start--
        while (end < pattern.length - 1 && pattern[end + 1] == CELL_BLACK) end++

        val consecutive = end - start + 1

        // 跳三模式检测：X_XXX_ 或 XX_XX 等
        if (consecutive == 3) {
            // 标准活三：两端都有空位
            val leftOpen = start > 0 && pattern[start - 1] == CELL_EMPTY
            val rightOpen = end < pattern.length - 1 && pattern[end + 1] == CELL_EMPTY

            if (leftOpen && rightOpen) {
                // 还需要确保再外侧不是墙/对方棋子
                val farLeft = start > 1 && pattern[start - 2] != CELL_WHITE
                val farRight = end < pattern.length - 2 && pattern[end + 2] != CELL_WHITE
                if (farLeft && farRight) return true
            }
        }

        // 跳三模式：.X.XX. 或 .XX.X. (中间有空位)
        if (consecutive < 3) {
            return checkJumpThree(board, x, y, dx, dy)
        }

        return false
    }

    /**
     * 检查跳三模式（中间有空位的三）
     */
    private fun checkJumpThree(
        board: String,
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
    ): Boolean {
        // 以 (x,y) 为中心的 9 格模式
        val pattern = extractPattern(board, x, y, dx, dy, 4)

        // 跳三模式列表（B=黑, .=空, X=边界/白）
        val jumpThreePatterns = listOf(
            ".B.BB.",  // 跳三
            ".BB.B.",  // 跳三
            "..BBB..", // 标准活三（确认）
        )

        // 简化检测：计算模式中的黑子数和空位分布
        val blacks = pattern.count { it == CELL_BLACK }
        val empties = pattern.count { it == CELL_EMPTY }

        // 活三需要 3 个黑子，且两端有空位
        if (blacks == 3 && empties >= 2) {
            // 检查是否能形成活四
            return canFormLiveFour(board, x, y, dx, dy)
        }

        return false
    }

    /**
     * 检查在此方向是否能通过一步形成活四
     */
    private fun canFormLiveFour(
        board: String,
        x: Int,
        y: Int,
        dx: Int,
        dy: Int,
    ): Boolean {
        // 在此方向的空位尝试落子，检查是否形成活四
        for (offset in -4..4) {
            val nx = x + dx * offset
            val ny = y + dy * offset
            if (nx !in 0 until SIZE || ny !in 0 until SIZE) continue
            if (!GomokuRules.isEmpty(board, nx, ny)) continue

            val testBoard = GomokuRules.applyMove(board, nx, ny, CELL_BLACK)
            val newPattern = extractPattern(testBoard, x, y, dx, dy, 5)
            if (isLiveFour(newPattern)) return true
        }
        return false
    }

    /**
     * 判断模式是否为活四
     */
    private fun isLiveFour(pattern: String): Boolean {
        val center = pattern.length / 2
        var start = center
        var end = center
        while (start > 0 && pattern[start - 1] == CELL_BLACK) start--
        while (end < pattern.length - 1 && pattern[end + 1] == CELL_BLACK) end++

        val consecutive = end - start + 1
        if (consecutive != 4) return false

        // 活四：两端都是空位
        val leftOpen = start > 0 && pattern[start - 1] == CELL_EMPTY
        val rightOpen = end < pattern.length - 1 && pattern[end + 1] == CELL_EMPTY

        return leftOpen && rightOpen
    }

    /**
     * 获取所有禁手点（用于 UI 标记）
     */
    fun findAllForbiddenPoints(board: String): List<ForbiddenPoint> {
        val result = mutableListOf<ForbiddenPoint>()
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (GomokuRules.isEmpty(board, x, y)) {
                    checkForbidden(board, x, y)?.let { type ->
                        result.add(ForbiddenPoint(x, y, type))
                    }
                }
            }
        }
        return result
    }

    /**
     * 扩展 GomokuRules 的落子验证
     */
    fun validateMoveWithForbidden(
        board: String,
        x: Int,
        y: Int,
        color: Char,
        enableForbidden: Boolean = true,
    ): MoveValidation {
        if (!GomokuRules.validateMove(board, x, y, color)) {
            return MoveValidation.Invalid("此处不能落子")
        }

        if (enableForbidden && color == CELL_BLACK) {
            val forbidden = checkForbidden(board, x, y)
            if (forbidden != null) {
                return MoveValidation.Forbidden(forbidden)
            }
        }

        return MoveValidation.Valid
    }
}

/** 禁手类型 */
enum class ForbiddenType(val displayName: String) {
    DOUBLE_THREE("三三禁手"),
    DOUBLE_FOUR("四四禁手"),
    OVERLINE("长连禁手"),
}

/** 禁手点 */
data class ForbiddenPoint(
    val x: Int,
    val y: Int,
    val type: ForbiddenType,
)

/** 落子验证结果 */
sealed class MoveValidation {
    object Valid : MoveValidation()
    data class Invalid(val reason: String) : MoveValidation()
    data class Forbidden(val type: ForbiddenType) : MoveValidation()

    val isValid: Boolean get() = this is Valid
}
