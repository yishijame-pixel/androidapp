package com.example.funlife.social.game

import android.content.Context
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.GameMoveRepository
import com.example.funlife.repository.GameRoomRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.SocialCredentials
import com.example.funlife.social.SocialOperationGate
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.GameMoveDto
import com.example.funlife.social.game.model.GameRoomDto

class GamePlayInteractor(
    appCtx: Context,
    private val userId: Long,
) {
    private val ctx = appCtx.applicationContext
    private val socialDao = (ctx as FunLifeApplication).database.socialDao()
    private val linkRepo = SocialLinkRepository(ctx, socialDao)
    private val roomRepo = GameRoomRepository(ctx, socialDao, linkRepo)
    private val moveRepo = GameMoveRepository(ctx, socialDao, roomRepo)
    private val api = PocketBaseApiClient(ctx)
    private val lobbyInteractor = GameRoomInteractor(ctx, userId)

    fun observeRooms() = lobbyInteractor.observeRooms()

    suspend fun ensurePlayCredentials() = GamePlayCredentialGate.ensure(ctx, userId)

    suspend fun refreshPlayState(
        roomId: String,
        updateLocalCache: Boolean = false,
    ): Result<Pair<GameRoomDto, List<GameMoveDto>>> =
        runPlayRead("同步对局") { cred ->
            if (updateLocalCache) {
                roomRepo.refreshRoomById(userId, cred.pbRecordId, cred.token, roomId)
            }
            val (dto, moves) = api.fetchPlayState(cred.token, roomId, includeProfiles = true)
            Result.success(dto to moves)
        }

    suspend fun placeGomoku(
        roomId: String,
        x: Int,
        y: Int,
        roomHint: GameRoomDto? = null,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<Pair<GameRoomDto, List<GameMoveDto>>> =
        runPlayMutation(roomId, "落子", SocialOperationGate.playMoveTimeoutMs()) { cred ->
            val dto = roomHint ?: api.getGameRoom(cred.token, roomId)
            moveRepo.submitGomokuMove(
                userId, cred.pbRecordId, cred.token, dto, x, y, cachedMoves,
            )
        }

    suspend fun submitDrawStroke(
        roomId: String,
        seq: Int,
        points: List<List<Float>>,
        color: String,
        width: Float,
        strokeId: String? = null,
        roomHint: GameRoomDto? = null,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<GameMoveDto> =
        runPlayStroke(roomId) { cred ->
            val dto = roomHint ?: api.getGameRoom(cred.token, roomId)
            moveRepo.submitDrawStroke(
                userId, cred.pbRecordId, cred.token, dto, seq, points, color, width, strokeId, cachedMoves,
            )
        }

    suspend fun clearDrawCanvas(
        roomId: String,
        roomHint: GameRoomDto? = null,
        cachedMoves: List<GameMoveDto>? = null,
    ): Result<GameMoveDto> =
        runPlayMutation(roomId, "清屏") { cred ->
            val dto = roomHint ?: api.getGameRoom(cred.token, roomId)
            moveRepo.submitDrawClear(userId, cred.pbRecordId, cred.token, dto, cachedMoves)
        }

    suspend fun submitGuess(roomId: String, text: String): Result<GameRoomDto> =
        runPlayMutation(roomId, "猜词") { cred ->
            val dto = api.getGameRoom(cred.token, roomId)
            moveRepo.submitDrawGuess(userId, cred.pbRecordId, cred.token, dto, text)
        }

    suspend fun setDrawPhase(roomId: String, phase: DrawGuessPhase): Result<GameRoomDto> =
        runPlayMutation(roomId, "切换阶段") { cred ->
            val dto = api.getGameRoom(cred.token, roomId)
            moveRepo.submitDrawPhase(userId, cred.pbRecordId, cred.token, dto, phase)
        }

    suspend fun abandonPlay(roomId: String): Result<Unit> =
        lobbyInteractor.abandonPlay(roomId)

    fun mapError(t: Throwable): String = lobbyInteractor.mapErrorMessage(t)

    private suspend fun runPlayRead(
        operation: String,
        block: suspend (SocialCredentials) -> Result<Pair<GameRoomDto, List<GameMoveDto>>>,
    ): Result<Pair<GameRoomDto, List<GameMoveDto>>> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = operation,
            timeoutMs = SocialOperationGate.playSyncTimeoutMs(),
            forceSession = true,
        ) { cred ->
            GamePlayCredentialGate.validate(cred).fold(
                onSuccess = { valid -> block(valid) },
                onFailure = { Result.failure(it) },
            )
        }

    /** 高频笔画：不占用 mutating 锁，避免 Realtime 笔迹被丢弃。 */
    private suspend fun <T> runPlayStroke(
        roomId: String,
        block: suspend (SocialCredentials) -> Result<T>,
    ): Result<T> {
        val timeoutMs = SocialOperationGate.playMoveTimeoutMs()
        return executeMutation("作画", timeoutMs, block).let { first ->
            if (first.isSuccess) return@let first
            val err = first.exceptionOrNull() ?: return@let first
            if (!GamePlayCredentialGate.isRecoverableError(err)) return@let first
            GamePlayCredentialGate.invalidateAndRebind(ctx, userId)
            executeMutation("作画", timeoutMs, block)
        }
    }

    private suspend fun <T> runPlayMutation(
        roomId: String,
        operation: String,
        timeoutMs: Long = SocialOperationGate.playMoveTimeoutMs(),
        block: suspend (SocialCredentials) -> Result<T>,
    ): Result<T> {
        GameRoomSyncCoordinator.markMutating(roomId)
        return try {
            executeMutation(operation, timeoutMs, block).let { first ->
                if (first.isSuccess) return@let first
                val err = first.exceptionOrNull() ?: return@let first
                if (!GamePlayCredentialGate.isRecoverableError(err)) return@let first
                GamePlayCredentialGate.invalidateAndRebind(ctx, userId)
                executeMutation(operation, timeoutMs, block)
            }
        } finally {
            GameRoomSyncCoordinator.clearMutating(roomId)
        }
    }

    private suspend fun <T> executeMutation(
        operation: String,
        timeoutMs: Long,
        block: suspend (SocialCredentials) -> Result<T>,
    ): Result<T> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = operation,
            timeoutMs = timeoutMs,
            forceSession = true,
        ) { cred ->
            GamePlayCredentialGate.validate(cred).fold(
                onSuccess = { valid -> block(valid) },
                onFailure = { Result.failure(it) },
            )
        }
}
