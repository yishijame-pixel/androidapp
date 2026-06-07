package com.example.funlife.social.game.engine

import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.GomokuMove
import com.example.funlife.social.game.model.GomokuPlayState

/**
 * 五子棋棋盘同步引擎（纯函数 + 轻量状态机）。
 *
 * 企业级原则：
 * - **game_moves 是唯一权威**，game_state.board 仅作元数据
 * - 任何时刻棋盘 = replay(moves) + pending 乐观子
 * - 禁止用滞后的 room 快照回退棋盘
 */
object GomokuBoardSync {

    data class BoardSnapshot(
        val board: String,
        val stoneCount: Int,
        val moveCount: Int,
        val lastMove: GomokuMove?,
        val gomokuPlacements: Int,
    )

    fun mergeMoves(
        local: List<GameMoveDto>,
        incoming: List<GameMoveDto>,
    ): List<GameMoveDto> {
        if (incoming.isEmpty()) return local
        if (local.isEmpty()) return incoming.sortedWith(moveComparator())
        val merged = LinkedHashMap<String, GameMoveDto>(local.size + incoming.size)
        local.forEach { merged[it.id] = it }
        incoming.forEach { merged[it.id] = it }
        return merged.values.sortedWith(moveComparator())
    }

    fun gomokuPlacements(moves: List<GameMoveDto>): List<Pair<String, Pair<Int, Int>>> =
        moves.mapNotNull { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            if (obj.get("kind")?.asString != "gomoku_place") return@mapNotNull null
            val x = obj.get("x")?.asInt ?: return@mapNotNull null
            val y = obj.get("y")?.asInt ?: return@mapNotNull null
            move.playerPbId to (x to y)
        }

    fun resolvePlayerIds(
        gomoku: GomokuPlayState?,
        room: GameRoomDto,
    ): Pair<String, String> {
        val black = gomoku?.blackPbId?.takeIf { it.isNotBlank() } ?: room.hostPbId
        val white = gomoku?.whitePbId?.takeIf { it.isNotBlank() } ?: room.guestPbId.orEmpty()
        return black to white
    }

    /**
     * 从 move 列表回放棋盘，并叠加尚未入库的乐观落子。
     */
    fun buildSnapshot(
        blackPbId: String,
        whitePbId: String,
        moves: List<GameMoveDto>,
        pending: List<Pair<Int, Int>> = emptyList(),
        myPbId: String? = null,
    ): BoardSnapshot {
        val placements = gomokuPlacements(moves)
        val (replayBoard, replayCount) = if (placements.isEmpty()) {
            GomokuRules.emptyBoard() to 0
        } else {
            GomokuRules.replayFromMoves(blackPbId, whitePbId, placements)
        }
        var board = replayBoard
        var count = replayCount

        if (!myPbId.isNullOrBlank() && pending.isNotEmpty()) {
            pending.forEach { (x, y) ->
                if (placementExists(moves, myPbId, x, y)) return@forEach
                val color = GomokuRules.colorForPbId(blackPbId, whitePbId, myPbId) ?: return@forEach
                if (GomokuRules.validateMove(board, x, y, color)) {
                    board = GomokuRules.applyMove(board, x, y, color)
                    count++
                }
            }
        }

        val lastFromMoves = placements.lastOrNull()
        val lastPending = pending.lastOrNull()
        val lastMove = when {
            lastFromMoves != null -> {
                val (pbId, pos) = lastFromMoves
                val color = GomokuRules.colorForPbId(blackPbId, whitePbId, pbId)?.toString().orEmpty()
                GomokuMove(pos.first, pos.second, color)
            }
            lastPending != null && !myPbId.isNullOrBlank() -> {
                val color = GomokuRules.colorForPbId(blackPbId, whitePbId, myPbId)?.toString().orEmpty()
                GomokuMove(lastPending.first, lastPending.second, color)
            }
            else -> null
        }

        val stoneCount = board.count { it != GomokuRules.CELL_EMPTY }
        return BoardSnapshot(
            board = board,
            stoneCount = stoneCount,
            moveCount = count,
            lastMove = lastMove,
            gomokuPlacements = placements.size,
        )
    }

    /** 黑先：偶数子 → 黑，奇数子 → 白 */
    fun deriveTurn(
        blackPbId: String,
        whitePbId: String,
        stoneCount: Int,
        status: GameRoomStatus,
        serverTurn: String? = null,
    ): String? {
        if (status != GameRoomStatus.PLAYING) return serverTurn
        if (blackPbId.isBlank() || whitePbId.isBlank()) return serverTurn
        return if (stoneCount % 2 == 0) blackPbId else whitePbId
    }

    fun maxMoveIndex(moves: List<GameMoveDto>): Int =
        moves.maxOfOrNull { it.moveIndex } ?: 0

    fun placementExists(
        moves: List<GameMoveDto>,
        playerPbId: String,
        x: Int,
        y: Int,
    ): Boolean = moves.any { move ->
        if (move.playerPbId != playerPbId) return@any false
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
        obj.get("kind")?.asString == "gomoku_place" &&
            obj.get("x")?.asInt == x &&
            obj.get("y")?.asInt == y
    }

    private fun moveComparator(): Comparator<GameMoveDto> =
        compareBy<GameMoveDto>({ it.moveIndex }, { it.createdAtMs }, { it.id })
}
