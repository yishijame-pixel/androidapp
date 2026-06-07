package com.example.funlife.social.game.engine

import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.viewmodel.DrawStrokeUi
import com.google.gson.JsonObject

/**
 * 你画我猜同步引擎：move 账本回放笔画，按轮次隔离画布。
 * 原则与五子棋一致——game_moves 为笔画权威，room.draw_guess 为阶段/分数元数据。
 */
object DrawGuessSync {

    private const val POINT_EPS = 0.00005f

    fun appendPointsUnique(
        existing: List<Pair<Float, Float>>,
        incoming: List<Pair<Float, Float>>,
    ): List<Pair<Float, Float>> {
        if (existing.isEmpty()) return incoming
        if (incoming.isEmpty()) return existing
        val (ex, ey) = existing.last()
        val (ix, iy) = incoming.first()
        val dropFirst = kotlin.math.abs(ex - ix) < POINT_EPS && kotlin.math.abs(ey - iy) < POINT_EPS
        return existing + if (dropFirst) incoming.drop(1) else incoming
    }

    /** 合并同一 stroke 的点：支持前缀/后缀重叠与全量快照，拒绝把不连续路径硬连 */
    fun mergeStrokePoints(
        existing: List<Pair<Float, Float>>,
        incoming: List<Pair<Float, Float>>,
    ): List<Pair<Float, Float>> {
        if (incoming.isEmpty()) return existing
        if (existing.isEmpty()) return incoming
        if (isPointPrefix(existing, incoming)) return incoming
        if (isPointPrefix(incoming, existing)) return existing
        val overlap = suffixPrefixOverlapCount(existing, incoming)
        if (overlap > 0) return existing + incoming.drop(overlap)
        if (pointsNear(existing.last(), incoming.first())) {
            return appendPointsUnique(existing, incoming)
        }
        return appendPointsUnique(existing, incoming)
    }

    private fun pointsNear(a: Pair<Float, Float>, b: Pair<Float, Float>): Boolean =
        kotlin.math.abs(a.first - b.first) < POINT_EPS &&
            kotlin.math.abs(a.second - b.second) < POINT_EPS

    private fun suffixPrefixOverlapCount(
        existing: List<Pair<Float, Float>>,
        incoming: List<Pair<Float, Float>>,
    ): Int {
        val max = minOf(existing.size, incoming.size)
        for (overlap in max downTo 1) {
            var match = true
            for (i in 0 until overlap) {
                val (ex, ey) = existing[existing.size - overlap + i]
                val (ix, iy) = incoming[i]
                if (!pointsNear(ex to ey, ix to iy)) {
                    match = false
                    break
                }
            }
            if (match) return overlap
        }
        return 0
    }

    private fun isPointPrefix(
        shorter: List<Pair<Float, Float>>,
        longer: List<Pair<Float, Float>>,
    ): Boolean {
        if (shorter.size > longer.size) return false
        return shorter.indices.all { i ->
            val (sx, sy) = shorter[i]
            val (lx, ly) = longer[i]
            kotlin.math.abs(sx - lx) < POINT_EPS && kotlin.math.abs(sy - ly) < POINT_EPS
        }
    }

    fun coalesceStrokes(strokes: List<DrawStrokeUi>): List<DrawStrokeUi> {
        if (strokes.size <= 1) return strokes
        val result = mutableListOf<DrawStrokeUi>()
        val indexByStrokeId = mutableMapOf<String, Int>()
        strokes.forEach { stroke ->
            val sid = stroke.strokeId?.takeIf { it.isNotBlank() }
            if (sid != null) {
                val idx = indexByStrokeId[sid]
                if (idx != null) {
                    val old = result[idx]
                    result[idx] = old.copy(
                        points = pickBestStrokePoints(old.points, stroke.points),
                        color = stroke.color,
                        width = stroke.width,
                    )
                } else {
                    indexByStrokeId[sid] = result.size
                    result.add(stroke)
                }
            } else {
                result.add(stroke)
            }
        }
        return result.sortedBy { it.seq }
    }

    /** 合并同 stroke 时始终保留点更多的路径，避免 ledger/live 竞态导致笔画缩短 */
    private fun pickBestStrokePoints(
        existing: List<Pair<Float, Float>>,
        incoming: List<Pair<Float, Float>>,
    ): List<Pair<Float, Float>> {
        if (incoming.isEmpty()) return existing
        if (existing.isEmpty()) return incoming
        val merged = mergeStrokePoints(existing, incoming)
        return listOf(existing, incoming, merged).maxByOrNull { it.size } ?: merged
    }

    fun mergeMoves(local: List<GameMoveDto>, incoming: List<GameMoveDto>): List<GameMoveDto> =
        GomokuBoardSync.mergeMoves(local, incoming)

    fun maxMoveIndex(moves: List<GameMoveDto>): Int = GomokuBoardSync.maxMoveIndex(moves)

    fun parseStrokes(moves: List<GameMoveDto>, currentRound: Int): List<DrawStrokeUi> {
        val strokes = mutableListOf<DrawStrokeUi>()
        moves.sortedBy { it.moveIndex }.forEach { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
            when (obj.get("kind")?.asString) {
                "draw_clear" -> if (strokeRound(obj, currentRound)) strokes.clear()
                "draw_phase" -> {
                    // 阶段切换只更新元数据，不重置画布；清屏仅认 draw_clear
                }
                "draw_stroke" -> {
                    if (!strokeRound(obj, currentRound)) return@forEach
                    val points = obj.getAsJsonArray("points")?.map { pt ->
                        val arr = pt.asJsonArray
                        arr[0].asFloat to arr[1].asFloat
                    }.orEmpty()
                    strokes.add(
                        DrawStrokeUi(
                            seq = obj.get("seq")?.asInt ?: strokes.size + 1,
                            points = points,
                            color = obj.get("color")?.asString ?: "#222222",
                            width = obj.get("width")?.asFloat ?: 4f,
                            strokeId = obj.get("stroke_id")?.asString,
                        ),
                    )
                }
            }
        }
        return coalesceStrokes(strokes)
    }

    /** 当前轮最后一次清屏 move 的 index；无清屏则为 0 */
    fun lastClearMoveIndex(moves: List<GameMoveDto>, currentRound: Int): Int =
        moves.filter { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@filter false
            obj.get("kind")?.asString == "draw_clear" && strokeRound(obj, currentRound)
        }.maxOfOrNull { it.moveIndex } ?: 0

    fun clearToken(moves: List<GameMoveDto>, currentRound: Int): Int =
        moves.count { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@count false
            obj.get("kind")?.asString == "draw_clear" && strokeRound(obj, currentRound)
        }

    fun strokeExists(moves: List<GameMoveDto>, playerPbId: String, seq: Int, round: Int): Boolean =
        moves.any { move ->
            if (move.playerPbId != playerPbId) return@any false
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
            obj.get("kind")?.asString == "draw_stroke" &&
                obj.get("seq")?.asInt == seq &&
                strokeRound(obj, round)
        }

    fun isDrawStrokeMove(move: GameMoveDto): Boolean {
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
        return obj.get("kind")?.asString == "draw_stroke"
    }

    /** 单条 move 解析为笔画（增量同步用，不做全量 replay） */
    fun parseStrokeMove(move: GameMoveDto, currentRound: Int): DrawStrokeUi? {
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        if (obj.get("kind")?.asString != "draw_stroke" || !strokeRound(obj, currentRound)) return null
        val points = obj.getAsJsonArray("points")?.map { pt ->
            val arr = pt.asJsonArray
            arr[0].asFloat to arr[1].asFloat
        }.orEmpty()
        if (points.size < 2 && points.size == 1) {
            val p = points.first()
            return DrawStrokeUi(
                seq = obj.get("seq")?.asInt ?: move.moveIndex,
                points = listOf(p, p),
                color = obj.get("color")?.asString ?: "#222222",
                width = obj.get("width")?.asFloat ?: 4f,
                strokeId = obj.get("stroke_id")?.asString,
            )
        }
        if (points.size < 2) return null
        return DrawStrokeUi(
            seq = obj.get("seq")?.asInt ?: move.moveIndex,
            points = points,
            color = obj.get("color")?.asString ?: "#222222",
            width = obj.get("width")?.asFloat ?: 4f,
            strokeId = obj.get("stroke_id")?.asString,
        )
    }

    private fun strokeRound(obj: JsonObject, currentRound: Int): Boolean {
        if (!obj.has("round")) return currentRound == 1
        return obj.get("round")?.asInt == currentRound
    }
}
