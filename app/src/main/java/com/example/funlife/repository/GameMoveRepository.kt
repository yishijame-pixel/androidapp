package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.game.engine.GomokuBoardSync
import com.example.funlife.social.game.engine.GomokuRules
import com.example.funlife.social.game.model.DrawClearPayload
import com.example.funlife.social.game.model.DrawGuessPayload
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.DrawGuessPlayState
import com.example.funlife.social.game.model.DrawPhasePayload
import com.example.funlife.social.game.model.DrawStrokePayload
import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.GameRoomStateCodec
import com.example.funlife.social.game.model.GameRoomStatePayload
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.GomokuMove
import com.example.funlife.social.game.model.GomokuPlacePayload
import com.example.funlife.social.game.model.GomokuPlayState
import com.example.funlife.social.game.engine.DrawGuessWordBank
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameMoveRepository(
    context: Context,
    private val socialDao: SocialDao,
    private val roomRepo: GameRoomRepository,
) {
    private val api = PocketBaseApiClient(context.applicationContext)
    private val gson = Gson()

    suspend fun listMoves(token: String, roomId: String): List<GameMoveDto> =
        withContext(Dispatchers.IO) { api.listGameMoves(token, roomId) }

    suspend fun submitGomokuMove(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        x: Int,
        y: Int,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<Pair<GameRoomDto, List<GameMoveDto>>> = withContext(Dispatchers.IO) {
        try {
            val authId = runCatching { PocketBaseApiClient.recordIdFromToken(token) }
                .getOrElse { throw it }
            if (authId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("社交身份未就绪，请返回大厅刷新"))
            }
            val state = resolveState(room)
            val gomoku = state.gomoku?.let { resolveGomokuPlayers(it, room) }
                ?: return@withContext Result.failure(IllegalStateException("非五子棋对局"))
            var moves = resolveMovesForSubmit(token, room.id, gomoku, cachedMoves)
            val placements = gomokuPlacements(moves)
            val (replayedBoard, replayCount) = GomokuRules.replayFromMoves(
                gomoku.blackPbId,
                gomoku.whitePbId,
                placements,
            )
            val working = gomoku.copy(board = replayedBoard, moveCount = replayCount)
            val derivedTurn = GomokuBoardSync.deriveTurn(
                blackPbId = working.blackPbId,
                whitePbId = working.whitePbId,
                stoneCount = replayCount,
                status = room.status,
                serverTurn = room.currentTurnPbId,
            )
            if (authId != derivedTurn) {
                return@withContext Result.failure(IllegalStateException("还没轮到你"))
            }
            val color = GomokuRules.colorForPbId(working.blackPbId, working.whitePbId, authId)
                ?: return@withContext Result.failure(IllegalStateException("你不在这盘棋里"))
            if (!GomokuRules.validateMove(working.board, x, y, color)) {
                return@withContext Result.failure(IllegalStateException("此处不能落子"))
            }
            val alreadyOnServer = moveAlreadyExists(moves, authId, x, y)
            if (!alreadyOnServer) {
                val created = api.createGameMove(
                    token = token,
                    roomId = room.id,
                    playerPbId = authId,
                    moveIndex = nextMoveIndex(moves),
                    payload = mapOf(
                        "kind" to "gomoku_place",
                        "x" to x,
                        "y" to y,
                    ),
                )
                moves = moves + created
            }
            val movePersisted = moveAlreadyExists(moves, authId, x, y)
            val newBoard = GomokuRules.applyMove(working.board, x, y, color)
            val moveCount = working.moveCount + 1
            val winnerColor = GomokuRules.checkWinner(newBoard, x, y)
            val winnerPbId = winnerColor?.let {
                GomokuRules.pbIdForColor(working.blackPbId, working.whitePbId, it)
            }
            val nextTurn = resolveNextTurn(working.blackPbId, working.whitePbId, color, room)
            val isDraw = winnerPbId == null && GomokuRules.isDraw(newBoard, moveCount)
            val updatedGomoku = working.copy(
                board = newBoard,
                moveCount = moveCount,
                lastMove = GomokuMove(x, y, color.toString()),
            )
            val newState = state.copy(gomoku = updatedGomoku)
            val newStatus = when {
                winnerPbId != null || isDraw -> GameRoomStatus.FINISHED
                else -> GameRoomStatus.PLAYING
            }
            if (newStatus == GameRoomStatus.PLAYING && movePersisted) {
                // PB hook 已同步棋盘+回合；PLAYING 不再 PATCH（省 ~1 次 RTT）
                val updatedRoom = room.copy(
                    status = GameRoomStatus.PLAYING,
                    currentTurnPbId = nextTurn,
                    gameState = newState,
                )
                return@withContext Result.success(updatedRoom to moves)
            }
            val patchResult = roomRepo.patchPlayState(
                userId = userId,
                myPbId = authId,
                token = token,
                roomId = room.id,
                state = newState,
                status = newStatus,
                currentTurnPbId = if (newStatus == GameRoomStatus.PLAYING) nextTurn else null,
                winnerPbId = winnerPbId,
            )
            patchResult.fold(
                onSuccess = { updated -> Result.success(updated to moves) },
                onFailure = { patchErr ->
                    if (!movePersisted) {
                        return@withContext Result.failure(patchErr)
                    }
                    val fallbackRoom = api.getGameRoom(token, room.id)
                    Result.success(fallbackRoom to moves)
                },
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDrawStroke(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        seq: Int,
        points: List<List<Float>>,
        color: String,
        width: Float,
        strokeId: String? = null,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<GameMoveDto> = withContext(Dispatchers.IO) {
        try {
            val play = resolveState(room).drawGuess
                ?: return@withContext Result.failure(IllegalStateException("非你画我猜对局"))
            if (play.drawerPbId != myPbId || play.phase != DrawGuessPhase.DRAWING.wire) {
                return@withContext Result.failure(IllegalStateException("当前不能作画"))
            }
            val moves = resolveMovesForSubmitGeneric(token, room.id, cachedMoves)
            val payload = buildMap {
                put("kind", "draw_stroke")
                put("seq", seq)
                put("round", play.round)
                put("points", points)
                put("color", color)
                put("width", width)
                if (!strokeId.isNullOrBlank()) put("stroke_id", strokeId)
            }
            if (!strokeId.isNullOrBlank()) {
                findDrawStrokeByStrokeId(moves, myPbId, strokeId, play.round)?.let { existing ->
                    val oldCount = existing.payloadPointsSize()
                    if (points.size >= oldCount) {
                        return@withContext patchOrCreateDrawStrokePreview(
                            token = token,
                            room = room,
                            myPbId = myPbId,
                            moves = moves,
                            existing = existing,
                            payload = payload,
                        )
                    }
                    return@withContext Result.success(existing)
                }
            }
            if (strokeAlreadyExists(moves, myPbId, seq, play.round)) {
                val existing = moves.last { move ->
                    val obj = move.payload?.asJsonObject ?: return@last false
                    obj.get("kind")?.asString == "draw_stroke" &&
                        obj.get("seq")?.asInt == seq &&
                        (obj.get("round")?.asInt ?: play.round) == play.round
                }
                if (points.size > existing.payloadPointsSize()) {
                    return@withContext patchOrCreateDrawStrokePreview(
                        token = token,
                        room = room,
                        myPbId = myPbId,
                        moves = moves,
                        existing = existing,
                        payload = payload,
                    )
                }
                return@withContext Result.success(existing)
            }
            val dto = api.createGameMove(
                token = token,
                roomId = room.id,
                playerPbId = myPbId,
                moveIndex = nextMoveIndex(moves),
                payload = payload,
            )
            // PLAYING 笔画：PB hook 同步 stroke_seq；不再 PATCH 省 RTT
            Result.success(dto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDrawClear(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<GameMoveDto> = withContext(Dispatchers.IO) {
        try {
            val play = resolveState(room).drawGuess
                ?: return@withContext Result.failure(IllegalStateException("非你画我猜对局"))
            if (play.drawerPbId != myPbId) {
                return@withContext Result.failure(IllegalStateException("只有画家可以清空画板"))
            }
            if (play.phase != DrawGuessPhase.DRAWING.wire) {
                return@withContext Result.failure(IllegalStateException("当前不能清空画板"))
            }
            val moves = resolveMovesForSubmitGeneric(token, room.id, cachedMoves)
            Result.success(
                api.createGameMove(
                    token, room.id, myPbId, nextMoveIndex(moves),
                    mapOf("kind" to "draw_clear", "round" to play.round),
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDrawGuess(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        text: String,
    ): Result<GameRoomDto> = withContext(Dispatchers.IO) {
        try {
            val state = resolveState(room)
            val play = state.drawGuess ?: return@withContext Result.failure(IllegalStateException("非你画我猜对局"))
            val phase = DrawGuessPhase.fromWire(play.phase)
            if (phase != DrawGuessPhase.DRAWING && phase != DrawGuessPhase.GUESSING) {
                return@withContext Result.failure(IllegalStateException("当前不能发送消息"))
            }
            if (play.drawerPbId == myPbId) {
                return@withContext Result.failure(IllegalStateException("画家不能发送消息"))
            }
            val trimmed = text.trim()
            if (trimmed.isBlank()) {
                return@withContext Result.failure(IllegalStateException("消息不能为空"))
            }
            val moves = resolveMovesForSubmitGeneric(token, room.id, null)
            api.createGameMove(
                token = token,
                roomId = room.id,
                playerPbId = myPbId,
                moveIndex = nextMoveIndex(moves),
                payload = mapOf("kind" to "draw_guess", "text" to trimmed, "round" to play.round),
            )
            // PB hook 权威计分；优先读服务端房间，省 PATCH RTT 且防双写
            val hooked = api.getGameRoom(token, room.id)
            val hookedPlay = resolveState(hooked).drawGuess
            if (hookedPlay != null && hookedPlay.guesses.size > play.guesses.size) {
                return@withContext Result.success(hooked)
            }
            val isDrawer = play.drawerPbId == myPbId
            val correct = !isDrawer && normalizeGuess(trimmed) == normalizeGuess(play.word)
            val newGuesses = play.guesses + com.example.funlife.social.game.model.DrawGuessGuess(myPbId, trimmed, correct)
            val newScores = play.scores.toMutableMap()
            if (correct) {
                newScores[myPbId] = (newScores[myPbId] ?: 0) + 1
            }
            val updated = if (correct) {
                advanceAfterCorrectGuess(play.copy(guesses = newGuesses, scores = newScores), room)
            } else {
                play.copy(guesses = newGuesses, scores = newScores)
            }
            val finished = updated.phase == DrawGuessPhase.FINISHED.wire
            val winnerPbId = if (finished) resolveDrawGuessWinner(updated) else null
            roomRepo.patchPlayState(
                userId, myPbId, token, room.id,
                state.copy(drawGuess = updated.copy(
                    phaseStartedAtMs = if (updated.phase != play.phase) System.currentTimeMillis() else updated.phaseStartedAtMs,
                )),
                status = if (finished) GameRoomStatus.FINISHED else GameRoomStatus.PLAYING,
                currentTurnPbId = updated.drawerPbId.takeIf { it.isNotBlank() },
                winnerPbId = winnerPbId,
            ).getOrThrow().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitDrawPhase(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        phase: DrawGuessPhase,
    ): Result<GameRoomDto> = withContext(Dispatchers.IO) {
        try {
            val state = resolveState(room)
            val play = state.drawGuess ?: return@withContext Result.failure(IllegalStateException("非你画我猜对局"))
            var moves = resolveMovesForSubmitGeneric(token, room.id, null)
            if (phase == DrawGuessPhase.DRAWING && play.phase == DrawGuessPhase.ROUND_END.wire) {
                moves = advanceRoundMoves(play, room, moves, myPbId, token)
            }
            api.createGameMove(
                token, room.id, myPbId, nextMoveIndex(moves),
                mapOf(
                    "kind" to "draw_phase",
                    "phase" to phase.wire,
                    "round" to if (phase == DrawGuessPhase.DRAWING && play.phase == DrawGuessPhase.ROUND_END.wire) {
                        play.round + 1
                    } else {
                        play.round
                    },
                ),
            )
            val hooked = api.getGameRoom(token, room.id)
            val hookedPlay = resolveState(hooked).drawGuess
            if (hookedPlay != null && phaseApplied(hookedPlay, phase, play)) {
                return@withContext Result.success(hooked)
            }
            val updated = when (phase) {
                DrawGuessPhase.GUESSING -> play.copy(
                    phase = phase.wire,
                    phaseStartedAtMs = System.currentTimeMillis(),
                )
                DrawGuessPhase.DRAWING -> when (play.phase) {
                    DrawGuessPhase.ROUND_END.wire -> buildNextDrawerState(play, room)
                    else -> play.copy(
                        phase = phase.wire,
                        strokeSeq = 0,
                        phaseStartedAtMs = System.currentTimeMillis(),
                    )
                }
                DrawGuessPhase.ROUND_END -> {
                    val current = DrawGuessPhase.fromWire(play.phase)
                    if (current != DrawGuessPhase.GUESSING && current != DrawGuessPhase.DRAWING) {
                        return@withContext Result.failure(IllegalStateException("当前不能结束本轮"))
                    }
                    if (current == DrawGuessPhase.DRAWING && play.drawerPbId != myPbId) {
                        return@withContext Result.failure(IllegalStateException("只有画家可以结束本轮"))
                    }
                    if (current == DrawGuessPhase.GUESSING) {
                        val elapsedMs = System.currentTimeMillis() - play.phaseStartedAtMs
                        val timedOut = play.phaseStartedAtMs > 0L &&
                            elapsedMs >= play.guessSeconds * 1000L
                        if (!timedOut && play.drawerPbId != myPbId) {
                            return@withContext Result.failure(IllegalStateException("猜词时间未到"))
                        }
                    }
                    play.copy(phase = phase.wire, phaseStartedAtMs = System.currentTimeMillis())
                }
                else -> play.copy(phase = phase.wire, phaseStartedAtMs = System.currentTimeMillis())
            }
            val finished = updated.phase == DrawGuessPhase.FINISHED.wire
            val winnerPbId = if (finished) resolveDrawGuessWinner(updated) else null
            roomRepo.patchPlayState(
                userId, myPbId, token, room.id,
                state.copy(drawGuess = updated),
                status = if (finished) GameRoomStatus.FINISHED else GameRoomStatus.PLAYING,
                currentTurnPbId = updated.drawerPbId.takeIf { it.isNotBlank() },
                winnerPbId = winnerPbId,
            ).getOrThrow().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun postSimpleMove(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
        payload: Map<String, Any?>,
    ): Result<GameMoveDto> = try {
        val moves = api.listGameMoves(token, room.id)
        Result.success(api.createGameMove(token, room.id, myPbId, nextMoveIndex(moves), payload))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun submitPacMove(
        token: String,
        roomId: String,
        myPbId: String,
        payload: Map<String, Any?>,
    ): Result<GameMoveDto> = submitPacMoveAtIndex(
        token = token,
        roomId = roomId,
        myPbId = myPbId,
        moveIndex = null,
        payload = payload,
    )

    /** 高频输入帧：指定 moveIndex，避免每帧 list 全量 moves。 */
    suspend fun submitPacMoveAtIndex(
        token: String,
        roomId: String,
        myPbId: String,
        moveIndex: Int?,
        payload: Map<String, Any?>,
    ): Result<GameMoveDto> = withContext(Dispatchers.IO) {
        try {
            val index = moveIndex ?: run {
                val moves = api.listGameMoves(token, roomId)
                nextMoveIndex(moves)
            }
            Result.success(api.createGameMove(token, roomId, myPbId, index, payload))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun advanceAfterCorrectGuess(
        play: DrawGuessPlayState,
        room: GameRoomDto,
    ): DrawGuessPlayState {
        if (play.round >= play.maxRounds) {
            return play.copy(phase = DrawGuessPhase.FINISHED.wire)
        }
        return play.copy(phase = DrawGuessPhase.ROUND_END.wire)
    }

    private suspend fun advanceRoundMoves(
        play: DrawGuessPlayState,
        room: GameRoomDto,
        moves: List<GameMoveDto>,
        myPbId: String,
        token: String,
    ): List<GameMoveDto> {
        if (play.round >= play.maxRounds) {
            throw IllegalStateException("已达最大轮次")
        }
        val nextRound = play.round + 1
        val clearMove = api.createGameMove(
            token, room.id, myPbId, nextMoveIndex(moves),
            mapOf("kind" to "draw_clear", "round" to nextRound),
        )
        return moves + clearMove
    }

    private fun buildNextDrawerState(play: DrawGuessPlayState, room: GameRoomDto): DrawGuessPlayState {
        if (play.round >= play.maxRounds) {
            return play.copy(phase = DrawGuessPhase.FINISHED.wire)
        }
        val host = room.hostPbId
        val guest = room.guestPbId ?: play.drawerPbId
        val nextDrawer = if (play.drawerPbId == host) guest else host
        val used = play.usedWords.toMutableSet().apply { if (play.word.isNotBlank()) add(play.word) }
        return play.copy(
            round = play.round + 1,
            phase = DrawGuessPhase.DRAWING.wire,
            drawerPbId = nextDrawer,
            word = DrawGuessWordBank.randomWord(used),
            usedWords = used.toList(),
            guesses = emptyList(),
            strokeSeq = 0,
            phaseStartedAtMs = System.currentTimeMillis(),
        )
    }

    private fun resolveDrawGuessWinner(play: DrawGuessPlayState): String? {
        if (play.scores.isEmpty()) return null
        val topScore = play.scores.values.maxOrNull() ?: return null
        val leaders = play.scores.filterValues { it == topScore }.keys
        return leaders.singleOrNull()
    }

    private fun normalizeGuess(text: String): String =
        text.trim().lowercase().replace("\\s+".toRegex(), "")

    private fun guessAlreadyExists(play: DrawGuessPlayState, myPbId: String, text: String): Boolean {
        val norm = normalizeGuess(text)
        return play.guesses.any { it.pbId == myPbId && normalizeGuess(it.text) == norm }
    }

    private fun guessRecorded(play: DrawGuessPlayState, myPbId: String, text: String): Boolean {
        val norm = normalizeGuess(text)
        return play.guesses.any { it.pbId == myPbId && normalizeGuess(it.text) == norm }
    }

    private fun phaseApplied(hooked: DrawGuessPlayState, requested: DrawGuessPhase, before: DrawGuessPlayState): Boolean =
        when (requested) {
            DrawGuessPhase.GUESSING -> hooked.phase == DrawGuessPhase.GUESSING.wire
            DrawGuessPhase.DRAWING -> hooked.phase == DrawGuessPhase.DRAWING.wire &&
                (before.phase != DrawGuessPhase.ROUND_END.wire || hooked.round > before.round)
            DrawGuessPhase.ROUND_END -> hooked.phase == DrawGuessPhase.ROUND_END.wire
            else -> hooked.phase == requested.wire
        }

    private fun resolveGuesserPbId(room: GameRoomDto, drawerPbId: String): String {
        val host = room.hostPbId
        val guest = room.guestPbId.orEmpty()
        return if (drawerPbId == host) guest else host
    }

    private fun findDrawStrokeByStrokeId(
        moves: List<GameMoveDto>,
        playerPbId: String,
        strokeId: String,
        round: Int,
    ): GameMoveDto? = moves.lastOrNull { move ->
        if (move.playerPbId != playerPbId) return@lastOrNull false
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@lastOrNull false
        obj.get("kind")?.asString == "draw_stroke" &&
            obj.get("stroke_id")?.asString == strokeId &&
            (obj.get("round")?.asInt ?: 1) == round
    }

    private fun GameMoveDto.payloadPointsSize(): Int {
        val arr = payload?.takeIf { it.isJsonObject }?.asJsonObject?.getAsJsonArray("points") ?: return 0
        return arr.size()
    }

    /** PATCH 预览；若 PB 未开 updateRule（403）则降级为新建 move */
    private fun patchOrCreateDrawStrokePreview(
        token: String,
        room: GameRoomDto,
        myPbId: String,
        moves: List<GameMoveDto>,
        existing: GameMoveDto,
        payload: Map<String, Any?>,
    ): Result<GameMoveDto> = try {
        Result.success(api.patchGameMove(token, existing.id, payload))
    } catch (e: com.example.funlife.social.PocketBaseApiException) {
        if (e.code == 403) {
            Result.success(
                api.createGameMove(
                    token = token,
                    roomId = room.id,
                    playerPbId = myPbId,
                    moveIndex = nextMoveIndex(moves),
                    payload = payload,
                ),
            )
        } else {
            Result.failure(e)
        }
    }

    private fun strokeAlreadyExists(
        moves: List<GameMoveDto>,
        playerPbId: String,
        seq: Int,
        round: Int,
    ): Boolean = moves.any { move ->
        if (move.playerPbId != playerPbId) return@any false
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
        obj.get("kind")?.asString == "draw_stroke" &&
            obj.get("seq")?.asInt == seq &&
            (obj.get("round")?.asInt ?: 1) == round
    }

    private fun resolveMovesForSubmitGeneric(
        token: String,
        roomId: String,
        cachedMoves: List<GameMoveDto>?,
    ): List<GameMoveDto> {
        if (!cachedMoves.isNullOrEmpty()) return cachedMoves
        return api.listGameMoves(token, roomId)
    }

    private fun resolveState(room: GameRoomDto): GameRoomStatePayload {
        val entry = com.example.funlife.social.game.catalog.SocialGameCatalog.find(room.gameType)
        val maxPlayers = entry?.maxPlayers?.coerceIn(2, 4) ?: 2
        val minPlayers = entry?.minPlayers?.coerceIn(2, maxPlayers) ?: 2
        val raw = room.gameState ?: GameRoomStateCodec.fromLegacy(
            room.hostPbId, room.guestPbId, maxPlayers, minPlayers,
            room.status, room.inviteMode, room.guestReady,
        )
        return GameRoomStateCodec.normalize(raw, room.hostPbId)
    }

    /** PocketBase 必填 number 把 0 当 blank，move_index 必须从 1 起编。 */
    private fun nextMoveIndex(moves: List<GameMoveDto>): Int =
        (moves.maxOfOrNull { it.moveIndex } ?: 0) + 1

    /** 优先用本地账本，避免每手 listGameMoves（~1 RTT）。 */
    private fun resolveMovesForSubmit(
        token: String,
        roomId: String,
        gomoku: GomokuPlayState,
        cachedMoves: List<GameMoveDto>?,
    ): List<GameMoveDto> {
        if (cachedMoves.isNullOrEmpty()) {
            return api.listGameMoves(token, roomId)
        }
        val placements = gomokuPlacements(cachedMoves)
        val (_, replayCount) = GomokuRules.replayFromMoves(
            gomoku.blackPbId,
            gomoku.whitePbId,
            placements,
        )
        return if (replayCount == gomoku.moveCount) {
            cachedMoves
        } else {
            api.listGameMoves(token, roomId)
        }
    }

    private fun gomokuPlacements(moves: List<GameMoveDto>): List<Pair<String, Pair<Int, Int>>> =
        moves.mapNotNull { move ->
            val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            if (obj.get("kind")?.asString != "gomoku_place") return@mapNotNull null
            val px = obj.get("x")?.asInt ?: return@mapNotNull null
            val py = obj.get("y")?.asInt ?: return@mapNotNull null
            move.playerPbId to (px to py)
        }

    private fun moveAlreadyExists(
        moves: List<GameMoveDto>,
        authId: String,
        x: Int,
        y: Int,
    ): Boolean = moves.any { move ->
        if (move.playerPbId != authId) return@any false
        val obj = move.payload?.takeIf { it.isJsonObject }?.asJsonObject ?: return@any false
        obj.get("kind")?.asString == "gomoku_place" &&
            obj.get("x")?.asInt == x &&
            obj.get("y")?.asInt == y
    }

    private fun resolveGomokuPlayers(gomoku: GomokuPlayState, room: GameRoomDto): GomokuPlayState {
        val repaired = repairGomokuPlayers(gomoku, room)
        val black = repaired.blackPbId.ifBlank { room.hostPbId }
        val white = repaired.whitePbId.ifBlank { room.guestPbId.orEmpty() }
        return repaired.copy(blackPbId = black, whitePbId = white)
    }

    private fun resolveNextTurn(
        blackPbId: String,
        whitePbId: String,
        afterColor: Char,
        room: GameRoomDto,
    ): String? {
        val nextColor = GomokuRules.opponentColor(afterColor)
        return GomokuRules.pbIdForColor(blackPbId, whitePbId, nextColor)?.takeIf { it.isNotBlank() }
            ?: when (nextColor) {
                GomokuRules.CELL_BLACK -> blackPbId.ifBlank { room.hostPbId }
                GomokuRules.CELL_WHITE -> whitePbId.ifBlank { room.guestPbId.orEmpty() }
                else -> null
            }?.takeIf { it.isNotBlank() }
    }

    private fun repairGomokuPlayers(gomoku: GomokuPlayState, room: GameRoomDto): GomokuPlayState {
        val state = resolveState(room)
        val guestFromMembers = state.members
            .filter { it.status == com.example.funlife.social.game.model.LobbyMemberStatus.JOINED.wire }
            .map { it.pbId }
            .firstOrNull { it.isNotBlank() && it != room.hostPbId }
        val black = gomoku.blackPbId.takeIf { it.isNotBlank() } ?: room.hostPbId
        val white = gomoku.whitePbId.takeIf { it.isNotBlank() }
            ?: room.guestPbId?.takeIf { it.isNotBlank() }
            ?: guestFromMembers.orEmpty()
        return gomoku.copy(blackPbId = black, whitePbId = white)
    }
}
