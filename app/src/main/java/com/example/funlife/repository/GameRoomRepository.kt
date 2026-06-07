package com.example.funlife.repository

import android.content.Context
import android.util.Log
import com.example.funlife.data.dao.SocialDao
import com.example.funlife.data.model.SocialFriendCache
import com.example.funlife.data.model.SocialGameRoomCache
import com.example.funlife.social.PocketBaseApiClient
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.game.RoomCodeGenerator
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.GameRoomDto
import com.example.funlife.social.game.model.GameRoomStateCodec
import com.example.funlife.social.game.model.GameRoomMemberWire
import com.example.funlife.social.game.model.GameRoomStatePayload
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.InviteMode
import com.example.funlife.social.game.model.LobbyMember
import com.example.funlife.social.game.model.LobbyMemberStatus
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.model.DrawGuessPhase
import com.example.funlife.social.game.model.GomokuEndReason
import com.example.funlife.social.game.model.PlayStateFactory
import com.example.funlife.social.game.model.GameRoomStateMachine
import com.example.funlife.social.model.FriendshipStatus
import com.example.funlife.social.model.PbUserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class GameRoomRepository(
    context: Context,
    private val socialDao: SocialDao,
    private val linkRepo: SocialLinkRepository,
) {
    private val appCtx = context.applicationContext
    private val api = PocketBaseApiClient(appCtx)
    private val gson = Gson()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val MUTATION_MAX_ATTEMPTS = 3
        const val TAG = "GameRoomRepo"
    }

    fun observeRooms(userId: Long): Flow<List<LocalGameRoomDraft>> =
        socialDao.observeGameRooms(userId).map { list -> list.map { it.toDraft() } }

    suspend fun listIncomingInviteDrafts(userId: Long, myPbId: String): List<LocalGameRoomDraft> =
        withContext(Dispatchers.IO) {
            if (myPbId.isBlank()) return@withContext emptyList()
            socialDao.getGameRooms(userId)
                .map { it.toDraft() }
                .filter { it.isIncomingInviteFor(myPbId) }
        }

    /** 仅拉取待处理邀请（1 次 HTTP），用于首页送达与 Realtime 在线时的兜底轮询。 */
    suspend fun refreshIncomingInvitesOnly(
        userId: Long,
        myPbId: String,
        token: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val invites = api.listIncomingGameInvites(token, myPbId)
                    .filter { canGuestAcceptInvite(it, myPbId) }
                if (invites.isEmpty()) return@withContext Result.success(Unit)
                val friends = socialDao.getFriends(userId)
                val existingById = socialDao.getGameRooms(userId).associateBy { it.roomId }
                val caches = invites.map { dto ->
                    buildRoomCache(userId, myPbId, dto, friends, token, existingById[dto.id])
                }
                socialDao.upsertGameRooms(caches)
                Result.success(Unit)
            } catch (e: PocketBaseApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun refreshRooms(userId: Long, myPbId: String, token: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val dtos = buildList {
                    addAll(api.listMyGameRooms(token, myPbId))
                    addAll(api.listIncomingGameInvites(token, myPbId).filter { canGuestAcceptInvite(it, myPbId) })
                }.distinctBy { it.id }.toMutableList()
                dedupeHostRoomsInRefresh(userId, myPbId, token, dtos)
                val friends = socialDao.getFriends(userId)
                val existingById = socialDao.getGameRooms(userId).associateBy { it.roomId }
                val caches = dtos.map { dto ->
                    val existing = existingById[dto.id]
                    buildRoomCache(userId, myPbId, dto, friends, token, existing)
                }
                socialDao.upsertGameRooms(caches)
                Result.success(Unit)
            } catch (e: PocketBaseApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun refreshRoomById(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
        lite: Boolean = true,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dto = if (lite) api.getGameRoomLite(token, roomId) else api.getGameRoom(token, roomId)
            val state = resolveState(dto)
            val joined = GameRoomStateCodec.joinedCount(state)
            Log.i(TAG, "refreshRoomById roomId=$roomId joined=$joined lite=$lite")
            cacheRoomDto(userId, myPbId, token, dto, lite = lite)
            Result.success(Unit)
        } catch (e: PocketBaseApiException) {
            if (e.code == 404) {
                val cached = socialDao.getGameRoom(userId, roomId)
                val keepPending = cached != null && (
                    cached.pendingInvitePbId == myPbId ||
                        cached.guestPbId == myPbId
                    )
                if (keepPending) {
                    Log.w(TAG, "refreshRoomById 404 keep pending invite cache roomId=$roomId")
                    return@withContext Result.failure(
                        PocketBaseApiException(404, "房间同步中，请稍候"),
                    )
                }
                socialDao.deleteGameRoom(userId, roomId)
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** SSE 推送：先写本地缓存再 HTTP 对账，房主可即时看见宾客入座。 */
    suspend fun cacheRoomFromRemoteDto(
        userId: Long,
        myPbId: String,
        token: String,
        dto: GameRoomDto,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val state = resolveState(dto)
            Log.i(TAG, "cacheRoomFromRemoteDto roomId=${dto.id} joined=${GameRoomStateCodec.joinedCount(state)}")
            cacheRoomDto(userId, myPbId, token, dto, lite = true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheRoomDto(
        userId: Long,
        myPbId: String,
        token: String,
        dto: GameRoomDto,
        lite: Boolean,
    ) {
        val friends = socialDao.getFriends(userId)
        val existing = socialDao.getGameRoom(userId, dto.id)
        socialDao.upsertGameRooms(
            listOf(buildRoomCache(userId, myPbId, dto, friends, token, existing, lite = lite)),
        )
    }

    private suspend fun buildRoomCache(
        userId: Long,
        myPbId: String,
        dto: GameRoomDto,
        friends: List<SocialFriendCache>,
        token: String,
        existing: SocialGameRoomCache?,
        lite: Boolean = false,
    ): SocialGameRoomCache {
        val existingMembers = existing?.let { parseMembersJson(it.membersJson) }.orEmpty()
        val needsFetch = !lite &&
            (existing == null || membersMissingProfiles(existingMembers))
        val fresh = dto.toCache(userId, myPbId, friends, token, fetchMissingProfiles = needsFetch)
        return fresh.mergeFromPrevious(existing)
    }

    suspend fun dismissLocalRoom(userId: Long, roomId: String) {
        withContext(Dispatchers.IO) {
            socialDao.deleteGameRoom(userId, roomId)
        }
    }

    suspend fun createOpenRoom(
        userId: Long,
        myPbId: String,
        token: String,
        gameId: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            reconcileExclusiveSession(userId, myPbId, token, keepRoomId = null, localCache = true, server = false)

            val entry = SocialGameCatalog.find(gameId)
            val maxPlayers = entry?.maxPlayers?.coerceIn(2, 4) ?: 2
            val minPlayers = entry?.minPlayers?.coerceIn(2, maxPlayers) ?: 2
            val roomCode = generateUniqueRoomCode(token)
            val state = GameRoomStateCodec.initial(myPbId, maxPlayers, minPlayers)
            val body = mapOf(
                "game_type" to gameId,
                "invite_mode" to InviteMode.OPEN.wire,
                "room_code" to roomCode,
                "host" to myPbId,
                "status" to GameRoomStatus.WAITING.wire,
                "host_ready" to true,
                "guest_ready" to false,
                "expires_at" to expiresAtMinutes(10),
                "game_state" to GameRoomStateCodec.toMap(state),
            )
            val dto = api.createGameRoom(token, body)
            cacheSingle(userId, myPbId, dto, token)
            scheduleExclusiveSessionCleanup(userId, myPbId, token, keepRoomId = dto.id)
            Result.success(dto.id)
        } catch (e: Exception) {
            Log.w(TAG, "createOpenRoom failed userId=$userId host=$myPbId game=$gameId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun inviteFriendToRoom(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
        guestPbId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureAcceptedFriend(userId, guestPbId)
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                transform = { current, state ->
                    GameRoomStateMachine.requireActiveRoom(current)
                    GameRoomStateMachine.requireHost(current, myPbId)
                    GameRoomStateMachine.requireRoomNotFull(state)
                    val next = prepareStateForDirectInvite(state, guestPbId)
                    directInvitePatch(next, guestPbId)
                },
                verify = { dto, state ->
                    dto.guestPbId == guestPbId ||
                        state.pendingInvitePbId == guestPbId ||
                        state.members.any { it.pbId == guestPbId && it.status == LobbyMemberStatus.PENDING.wire }
                },
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearDeclinedRoom(userId: Long, roomId: String) =
        dismissLocalRoom(userId, roomId)

    suspend fun findHostActiveRoom(userId: Long, myPbId: String, gameId: String): LocalGameRoomDraft? =
        socialDao.getGameRooms(userId)
            .map { it.toDraft() }
            .firstOrNull { room ->
                room.hostPbId == myPbId &&
                    room.gameId == gameId &&
                    room.status in GameRoomStatus.ACTIVE
            }

    suspend fun joinByRoomCode(
        userId: Long,
        myPbId: String,
        token: String,
        roomCode: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val room = api.findGameRoomByCode(token, roomCode.uppercase())
                ?: return@withContext Result.failure(IllegalStateException("未找到该房间"))
            if (room.hostPbId == myPbId) {
                return@withContext Result.success(room.id)
            }
            val state = resolveState(room)
            if (GameRoomStateCodec.isMember(state, myPbId)) {
                cacheSingle(userId, myPbId, room, token)
                return@withContext Result.success(room.id)
            }
            ensureAcceptedFriend(userId, room.hostPbId)
            mutateRoomState(
                token = token,
                roomId = room.id,
                userId = userId,
                myPbId = myPbId,
                transform = { current, normalized ->
                    GameRoomStateMachine.requireActiveRoom(current)
                    GameRoomStateMachine.requireCanJoin(normalized)
                    val next = GameRoomStateCodec.withDirectJoin(normalized, myPbId)
                    joinedGuestPatch(
                        state = next,
                        current = current,
                        guestPbId = myPbId,
                        status = GameRoomStateCodec.resolveStatusAfterJoin(next),
                    )
                },
                verify = { _, after -> GameRoomStateCodec.isMember(after, myPbId) },
            )
            scheduleExclusiveSessionCleanup(userId, myPbId, token, keepRoomId = room.id)
            Result.success(room.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val invited = resolveRoomForAcceptInvite(userId, myPbId, token, roomId)
            cacheRoomDto(userId, myPbId, token, invited, lite = false)
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                transform = { current, state ->
                    GameRoomStateMachine.requireActiveRoom(current)
                    GameRoomStateMachine.requireCanAcceptInvite(state, myPbId)
                    val next = GameRoomStateCodec.withAcceptedInvite(state, myPbId)
                    joinedGuestPatch(
                        state = next,
                        current = current,
                        guestPbId = myPbId,
                        status = GameRoomStateCodec.resolveStatusAfterJoin(next),
                    )
                },
                verify = { _, after ->
                    after.members.any {
                        it.pbId == myPbId && it.status == LobbyMemberStatus.JOINED.wire
                    }
                },
            )
            scheduleExclusiveSessionCleanup(userId, myPbId, token, keepRoomId = roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectInvite(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                cache = false,
                transform = { current, state ->
                    GameRoomStateMachine.requireActiveRoom(current)
                    val next = GameRoomStateCodec.withRejectedInvite(state, myPbId)
                    openLobbyPatch(next)
                },
                verify = { _, after -> !GameRoomStateCodec.isMember(after, myPbId) },
            )
            socialDao.deleteGameRoom(userId, roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun withdrawInvite(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                transform = { current, state ->
                    GameRoomStateMachine.requireActiveRoom(current)
                    GameRoomStateMachine.requireHost(current, myPbId)
                    val next = GameRoomStateCodec.withWithdrawnInvite(state)
                    openLobbyPatch(next)
                },
                verify = { _, after -> after.pendingInvitePbId.isNullOrBlank() },
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dismissDeclineNotice(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                transform = { current, state ->
                    GameRoomStateMachine.requireHost(current, myPbId)
                    LobbyPatchOptions(
                        state = GameRoomStateCodec.withClearedDecline(state),
                        status = current.status,
                    )
                },
                verify = { _, after -> after.declinedByPbId.isNullOrBlank() },
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "leaveRoom start userId=$userId roomId=$roomId myPbId=$myPbId")
        try {
            val current = runCatching { api.getGameRoom(token, roomId) }
                .getOrElse { error ->
                    if (error is PocketBaseApiException && error.code == 404) {
                        socialDao.deleteGameRoom(userId, roomId)
                        return@withContext Result.success(Unit)
                    }
                    throw error
                }
            if (current.hostPbId == myPbId) {
                socialDao.deleteGameRoom(userId, roomId)
                return@withContext Result.success(Unit)
            }
            val state = resolveState(current)
            if (!GameRoomStateCodec.isMember(state, myPbId)) {
                socialDao.deleteGameRoom(userId, roomId)
                return@withContext Result.success(Unit)
            }
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                cache = false,
                confirmOnServer = true,
                transform = { dto, normalized ->
                    GameRoomStateMachine.requireNotHost(dto, myPbId)
                    val next = GameRoomStateCodec.withMemberLeft(normalized, myPbId)
                    memberLeftPatch(next, dto, myPbId)
                },
                verify = { _, after -> !GameRoomStateCodec.isMember(after, myPbId) },
            )
            socialDao.deleteGameRoom(userId, roomId)
            Log.i(TAG, "leaveRoom ok roomId=$roomId")
            Result.success(Unit)
        } catch (e: PocketBaseApiException) {
            Log.w(TAG, "leaveRoom failed roomId=$roomId code=${e.code} msg=${e.message}")
            if (e.code == 404) {
                socialDao.deleteGameRoom(userId, roomId)
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRoom(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = api.getGameRoom(token, roomId)
            GameRoomStateMachine.requireHost(current, myPbId)
            GameRoomStateMachine.requireActiveRoom(current)
            api.updateGameRoom(token, roomId, mapOf("status" to GameRoomStatus.CANCELLED.wire))
            socialDao.deleteGameRoom(userId, roomId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startGame(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                transform = { current, state ->
                    GameRoomStateMachine.requireHost(current, myPbId)
                    GameRoomStateMachine.requireActiveRoom(current)
                    GameRoomStateMachine.requireReadyToStart(state)
                    val guestPbId = state.members
                        .filter { it.status == LobbyMemberStatus.JOINED.wire && it.pbId != current.hostPbId }
                        .firstOrNull()?.pbId
                        ?: current.guestPbId?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("对手尚未加入")
                    val withPlay = PlayStateFactory.mergePlayIntoLobby(
                        state, current.gameType, current.hostPbId, guestPbId,
                    )
                    LobbyPatchOptions(
                        state = withPlay,
                        status = GameRoomStatus.PLAYING,
                        currentTurnPbId = PlayStateFactory.firstTurnPbId(current.gameType, current.hostPbId),
                        clearWinner = true,
                    )
                },
                verify = { dto, _ -> dto.status == GameRoomStatus.PLAYING },
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun patchPlayState(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
        state: GameRoomStatePayload,
        status: GameRoomStatus? = null,
        currentTurnPbId: String? = null,
        winnerPbId: String? = null,
    ): Result<GameRoomDto> = withContext(Dispatchers.IO) {
        try {
            val targetStatus = status ?: GameRoomStatus.PLAYING
            val dto = mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                maxAttempts = 3,
                transform = { _, _ ->
                    LobbyPatchOptions(
                        state = state,
                        status = targetStatus,
                        currentTurnPbId = currentTurnPbId,
                        winnerPbId = winnerPbId,
                    )
                },
                verify = { dto, _ ->
                    when {
                        winnerPbId != null -> dto.winnerPbId == winnerPbId
                        currentTurnPbId != null && targetStatus == GameRoomStatus.PLAYING ->
                            dto.currentTurnPbId == currentTurnPbId
                        else -> true
                    }
                },
            )
            Result.success(dto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 对局进行中主动退出：判己方负、对手胜，并结束房间。 */
    suspend fun abandonPlay(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        Log.i(TAG, "abandonPlay start userId=$userId roomId=$roomId myPbId=$myPbId")
        try {
            val current = runCatching { api.getGameRoom(token, roomId) }
                .getOrElse { error ->
                    if (error is PocketBaseApiException && error.code == 404) {
                        socialDao.deleteGameRoom(userId, roomId)
                        return@withContext Result.success(Unit)
                    }
                    throw error
                }
            if (current.status != GameRoomStatus.PLAYING) {
                socialDao.deleteGameRoom(userId, roomId)
                return@withContext Result.success(Unit)
            }
            val state = resolveState(current)
            GameRoomStateMachine.requireMember(state, myPbId)
            val opponentPbId = resolvePlayOpponent(current, state, myPbId)
                ?: return@withContext Result.failure(IllegalStateException("找不到对手"))
            val winnerPbId = opponentPbId
            mutateRoomState(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                cache = false,
                transform = { dto, normalized ->
                    val updatedState = when (dto.gameType) {
                        "gomoku" -> {
                            val g = normalized.gomoku
                                ?: throw IllegalStateException("对局数据异常")
                            normalized.copy(
                                gomoku = g.copy(endReason = GomokuEndReason.RESIGN),
                            )
                        }
                        "draw_guess" -> {
                            val d = normalized.drawGuess
                                ?: throw IllegalStateException("对局数据异常")
                            normalized.copy(
                                drawGuess = d.copy(phase = DrawGuessPhase.FINISHED.wire),
                            )
                        }
                        else -> normalized
                    }
                    LobbyPatchOptions(
                        state = updatedState,
                        status = GameRoomStatus.FINISHED,
                        winnerPbId = winnerPbId,
                    )
                },
                verify = { dto, _ ->
                    dto.status == GameRoomStatus.FINISHED && dto.winnerPbId == winnerPbId
                },
            )
            socialDao.deleteGameRoom(userId, roomId)
            Log.i(TAG, "abandonPlay ok roomId=$roomId winner=$winnerPbId")
            Result.success(Unit)
        } catch (e: PocketBaseApiException) {
            Log.w(TAG, "abandonPlay failed roomId=$roomId code=${e.code} msg=${e.message}")
            if (e.code == 404) {
                socialDao.deleteGameRoom(userId, roomId)
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 落子后仅补写 current_turn（完整 game_state PATCH 失败时的兜底）。 */
    suspend fun patchCurrentTurnOnly(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
        currentTurnPbId: String,
    ): Result<GameRoomDto> = withContext(Dispatchers.IO) {
        try {
            if (currentTurnPbId.isBlank()) {
                return@withContext Result.failure(IllegalStateException("回合方为空"))
            }
            val dto = patchLobby(
                token = token,
                roomId = roomId,
                userId = userId,
                myPbId = myPbId,
                options = LobbyPatchOptions(
                    state = resolveState(api.getGameRoom(token, roomId)),
                    status = GameRoomStatus.PLAYING,
                    currentTurnPbId = currentTurnPbId,
                ),
            )
            Result.success(dto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 单条 GET 在旧服 ACL 下可能对受邀宾客 404，但 list 仍可见（game_state ~ auth.id）。
     * 接受/拒绝邀请等变更前用 list 兜底，避免 "The requested resource wasn't found."
     */
    private suspend fun fetchRoomForMutation(
        token: String,
        roomId: String,
        myPbId: String,
    ): GameRoomDto {
        return try {
            api.getGameRoom(token, roomId)
        } catch (e: PocketBaseApiException) {
            if (e.code != 404) throw e
            api.listIncomingGameInvites(token, myPbId).firstOrNull { it.id == roomId }
                ?: api.listMyGameRooms(token, myPbId).firstOrNull { it.id == roomId }
                ?: api.listActiveRoomsForUser(token, myPbId).firstOrNull { it.id == roomId }
                ?: throw e
        }
    }

    private suspend fun resolveRoomForAcceptInvite(
        userId: Long,
        myPbId: String,
        token: String,
        roomId: String,
    ): GameRoomDto {
        api.listIncomingGameInvites(token, myPbId)
            .firstOrNull { it.id == roomId && canGuestAcceptInvite(it, myPbId) }
            ?.let { return it }
        runCatching { fetchRoomForMutation(token, roomId, myPbId) }.getOrNull()?.let { dto ->
            if (canGuestAcceptInvite(dto, myPbId)) {
                Log.i(TAG, "resolveRoomForAcceptInvite via fetch roomId=$roomId status=${dto.status}")
                return dto
            }
        }
        val cached = socialDao.getGameRoom(userId, roomId)
        if (cached != null && (cached.pendingInvitePbId == myPbId || cached.guestPbId == myPbId)) {
            Log.w(
                TAG,
                "resolveRoomForAcceptInvite cache-only roomId=$roomId pending=${cached.pendingInvitePbId} guest=${cached.guestPbId}",
            )
        }
        throw PocketBaseApiException(404, "邀请已失效，请让房主重新邀请")
    }

    private fun canGuestAcceptInvite(dto: GameRoomDto, myPbId: String): Boolean {
        if (dto.hostPbId == myPbId || dto.status !in GameRoomStatus.ACTIVE) return false
        val state = resolveState(dto)
        if (state.pendingInvitePbId == myPbId) return true
        if (state.members.any { it.pbId == myPbId && it.status == LobbyMemberStatus.PENDING.wire }) return true
        if (dto.guestPbId == myPbId && dto.status == GameRoomStatus.WAITING &&
            state.members.none { it.pbId == myPbId && it.status == LobbyMemberStatus.JOINED.wire }
        ) {
            return true
        }
        return false
    }

    private suspend fun mutateRoomState(
        token: String,
        roomId: String,
        userId: Long,
        myPbId: String,
        cache: Boolean = true,
        maxAttempts: Int = MUTATION_MAX_ATTEMPTS,
        confirmOnServer: Boolean = false,
        transform: (GameRoomDto, GameRoomStatePayload) -> LobbyPatchOptions,
        verify: (GameRoomDto, GameRoomStatePayload) -> Boolean = { _, _ -> true },
    ): GameRoomDto {
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                val current = fetchRoomForMutation(token, roomId, myPbId)
                val normalized = GameRoomStateCodec.normalize(resolveState(current), current.hostPbId)
                val patch = transform(current, normalized)
                val dto = patchLobby(token, roomId, userId, myPbId, patch, cache = cache)
                val afterResponse = GameRoomStateCodec.normalize(resolveState(dto), dto.hostPbId)
                val afterIntended = GameRoomStateCodec.normalize(patch.state, dto.hostPbId)
                if (confirmOnServer) {
                    if (!verify(dto, afterResponse) && !verify(dto, afterIntended)) {
                        throw GameRoomStateMachine.ConflictException("状态校验未通过")
                    }
                    delay(120L)
                    val confirmed = fetchRoomForMutation(token, roomId, myPbId)
                    val confirmedState = GameRoomStateCodec.normalize(resolveState(confirmed), confirmed.hostPbId)
                    if (!verify(confirmed, confirmedState)) {
                        throw GameRoomStateMachine.ConflictException("服务端未确认变更")
                    }
                    return confirmed
                }
                if (!verify(dto, afterResponse) && !verify(dto, afterIntended)) {
                    throw GameRoomStateMachine.ConflictException("状态校验未通过")
                }
                return dto
            } catch (e: GameRoomStateMachine.ConflictException) {
                lastError = e
                delay(250L * (attempt + 1))
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts - 1) delay(250L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("房间状态更新失败")
    }

    private fun scheduleExclusiveSessionCleanup(
        userId: Long,
        myPbId: String,
        token: String,
        keepRoomId: String?,
    ) {
        cleanupScope.launch {
            runCatching {
                reconcileExclusiveSession(
                    userId = userId,
                    myPbId = myPbId,
                    token = token,
                    keepRoomId = keepRoomId,
                    localCache = false,
                    server = true,
                )
            }
        }
    }

    private suspend fun reconcileExclusiveSession(
        userId: Long,
        myPbId: String,
        token: String,
        keepRoomId: String?,
        localCache: Boolean,
        server: Boolean,
    ) = coroutineScope {
        if (localCache) {
            socialDao.getGameRooms(userId)
                .filter { cache ->
                    cache.hostPbId == myPbId &&
                        cache.roomId != keepRoomId &&
                        GameRoomStatus.fromWire(cache.status) in GameRoomStatus.ACTIVE
                }
                .map { cache ->
                    async { runCatching { cancelRemoteAndLocal(userId, token, cache.roomId) } }
                }
                .awaitAll()
        }
        if (server) {
            val active = api.listActiveRoomsForUser(token, myPbId)
                .filter { it.id != keepRoomId }
            val hostRooms = active.filter { it.hostPbId == myPbId }
            val staleHostIds = if (hostRooms.size > 1) {
                val keep = hostRooms.maxWith(
                    compareBy<GameRoomDto> { GameRoomStateCodec.joinedCount(resolveState(it)) }
                        .thenBy { it.updatedAtMs },
                )
                hostRooms.filter { it.id != keep.id }.map { it.id }.toSet()
            } else {
                emptySet()
            }
            active.forEach { room ->
                runCatching {
                    when {
                        room.id in staleHostIds -> cancelRemoteAndLocal(userId, token, room.id)
                        room.hostPbId == myPbId -> {
                            val state = resolveState(room)
                            if (shouldPreserveActiveHostRoom(state)) {
                                Log.d(TAG, "preserve host room ${room.id} pending=${state.pendingInvitePbId}")
                                return@runCatching
                            }
                            cancelRemoteAndLocal(userId, token, room.id)
                        }
                        else -> leaveGuestFromRoom(userId, myPbId, token, room)
                    }
                }
            }
        }
    }

    /** 仍有待处理邀请或已有人入座的主机房间，不因「单房间策略」被自动取消 */
    private fun shouldPreserveActiveHostRoom(state: GameRoomStatePayload): Boolean {
        if (!state.pendingInvitePbId.isNullOrBlank()) return true
        if (GameRoomStateCodec.joinedCount(state) > 1) return true
        return false
    }

    private suspend fun dedupeHostRoomsInRefresh(
        userId: Long,
        myPbId: String,
        token: String,
        dtos: MutableList<GameRoomDto>,
    ) {
        val hostActive = dtos.filter { it.hostPbId == myPbId && it.status in GameRoomStatus.ACTIVE }
        if (hostActive.size <= 1) return
        val keep = hostActive.maxWith(
            compareBy<GameRoomDto> { GameRoomStateCodec.joinedCount(resolveState(it)) }
                .thenBy { it.updatedAtMs },
        )
        hostActive.filter { it.id != keep.id }.forEach { stale ->
            runCatching {
                cancelRemoteAndLocal(userId, token, stale.id)
                dtos.removeAll { it.id == stale.id }
            }
        }
    }

    private suspend fun cancelRemoteAndLocal(userId: Long, token: String, roomId: String) {
        api.updateGameRoom(token, roomId, mapOf("status" to GameRoomStatus.CANCELLED.wire))
        socialDao.deleteGameRoom(userId, roomId)
    }

    private suspend fun leaveGuestFromRoom(
        userId: Long,
        myPbId: String,
        token: String,
        room: GameRoomDto,
    ) {
        val state = resolveState(room)
        when {
            GameRoomStateCodec.isMember(state, myPbId) -> {
                val next = GameRoomStateCodec.withMemberLeft(state, myPbId)
                patchLobby(
                    token, room.id, userId, myPbId,
                    memberLeftPatch(next, room, myPbId),
                    cache = false,
                )
            }
            room.guestPbId == myPbId || state.pendingInvitePbId == myPbId -> {
                val next = GameRoomStateCodec.withRejectedInvite(state, myPbId)
                patchLobby(
                    token, room.id, userId, myPbId,
                    openLobbyPatch(next),
                    cache = false,
                )
            }
        }
        socialDao.deleteGameRoom(userId, room.id)
    }

    private suspend fun ensureAcceptedFriend(userId: Long, friendPbId: String) {
        val friend = socialDao.getFriends(userId).firstOrNull {
            it.friendPbId == friendPbId && it.status == "accepted"
        }
        if (friend == null) {
            throw IllegalStateException("仅好友可加入或受邀")
        }
    }

    private suspend fun generateUniqueRoomCode(token: String): String {
        repeat(5) {
            val code = RoomCodeGenerator.generate()
            if (api.findGameRoomByCode(token, code) == null) return code
        }
        throw IllegalStateException("房间号生成失败，请重试")
    }

    private suspend fun cacheSingle(userId: Long, myPbId: String, dto: GameRoomDto, token: String) {
        val friends = socialDao.getFriends(userId)
        socialDao.upsertGameRooms(listOf(dto.toCache(userId, myPbId, friends, token)))
    }

    private fun expiresAtMinutes(minutes: Long): String =
        Instant.now().plus(minutes, ChronoUnit.MINUTES)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .toString()

    private data class LobbyPatchOptions(
        val state: GameRoomStatePayload,
        val status: GameRoomStatus = GameRoomStatus.WAITING,
        val inviteMode: InviteMode? = null,
        val guestPbId: String? = null,
        val clearGuest: Boolean = false,
        val guestReady: Boolean? = null,
        val hostReady: Boolean? = null,
        val expiresMinutes: Long? = null,
        val currentTurnPbId: String? = null,
        val winnerPbId: String? = null,
        val clearWinner: Boolean = false,
        /** 再次邀请时写入 invite_message，确保 PB updated 变化 */
        val touchInviteMessage: Boolean = false,
    )

    private fun openLobbyPatch(
        state: GameRoomStatePayload,
        status: GameRoomStatus = GameRoomStatus.WAITING,
    ) = LobbyPatchOptions(
        state = state,
        status = status,
        inviteMode = InviteMode.OPEN,
        clearGuest = true,
        guestReady = false,
    )

    /** 宾客加入后保留 guest 关系字段，确保 PocketBase ACL 可读写（直至离座清除）。 */
    private fun joinedGuestPatch(
        state: GameRoomStatePayload,
        current: GameRoomDto,
        guestPbId: String,
        status: GameRoomStatus,
    ) = LobbyPatchOptions(
        state = state,
        status = status,
        inviteMode = current.inviteMode,
        guestPbId = guestPbId,
        guestReady = true,
        hostReady = true,
    )

    /** 宾客离座：以 game_state.members 为准；status 强制回到 waiting 以便再次邀请。 */
    private fun memberLeftPatch(
        state: GameRoomStatePayload,
        current: GameRoomDto,
        leftPbId: String,
    ): LobbyPatchOptions {
        return LobbyPatchOptions(
            state = state,
            status = GameRoomStatus.WAITING,
            inviteMode = current.inviteMode,
            clearGuest = false,
            guestReady = false,
            hostReady = true,
        )
    }

    /** 再次 direct 邀请前：清掉宾客旧席位（含已离座但 members 未同步的情况）。 */
    private fun prepareStateForDirectInvite(
        state: GameRoomStatePayload,
        guestPbId: String,
    ): GameRoomStatePayload {
        var base = state
        if (base.members.any { it.pbId == guestPbId }) {
            base = if (base.members.any { it.pbId == guestPbId && it.status == LobbyMemberStatus.JOINED.wire }) {
                GameRoomStateCodec.withMemberLeft(base, guestPbId)
            } else {
                base.copy(
                    members = base.members.filter { it.pbId != guestPbId },
                    pendingInvitePbId = if (base.pendingInvitePbId == guestPbId) null else base.pendingInvitePbId,
                )
            }
        }
        return GameRoomStateCodec.withPendingInvite(base, guestPbId)
    }

    private fun directInvitePatch(
        state: GameRoomStatePayload,
        guestPbId: String,
    ) = LobbyPatchOptions(
        state = state,
        status = GameRoomStatus.WAITING,
        inviteMode = InviteMode.DIRECT,
        guestPbId = guestPbId,
        guestReady = false,
        expiresMinutes = 5,
        touchInviteMessage = true,
    )

    private suspend fun patchLobby(
        token: String,
        roomId: String,
        userId: Long,
        myPbId: String,
        options: LobbyPatchOptions,
        cache: Boolean = true,
    ): GameRoomDto {
        val body = mutableMapOf<String, Any?>(
            "game_state" to GameRoomStateCodec.toMap(options.state),
            "status" to options.status.wire,
        )
        options.inviteMode?.let { body["invite_mode"] = it.wire }
        when {
            options.clearGuest -> { /* 禁止 guest=""（PB 报 Cannot be blank）；留空表示不改动关系字段 */ }
            else -> options.guestPbId?.takeIf { it.isNotBlank() }?.let { body["guest"] = it }
        }
        options.guestReady?.let { body["guest_ready"] = it }
        options.hostReady?.let { body["host_ready"] = it }
        options.expiresMinutes?.let { body["expires_at"] = expiresAtMinutes(it) }
        if (options.touchInviteMessage) {
            body["invite_message"] = System.currentTimeMillis().toString()
        }
        // 关系字段禁止传 ""，否则 PocketBase 返回 Cannot be blank
        options.currentTurnPbId?.takeIf { it.isNotBlank() }?.let { body["current_turn"] = it }
        when {
            options.clearWinner -> body["winner"] = ""
            else -> options.winnerPbId?.takeIf { it.isNotBlank() }?.let { body["winner"] = it }
        }
        val dto = api.updateGameRoom(token, roomId, body)
        if (cache) {
            cacheSingle(userId, myPbId, dto, token)
        }
        return dto
    }

    private fun resolvePlayOpponent(
        current: GameRoomDto,
        state: GameRoomStatePayload,
        myPbId: String,
    ): String? {
        state.gomoku?.let { g ->
            if (myPbId == g.blackPbId) return g.whitePbId.takeIf { it.isNotBlank() }
            if (myPbId == g.whitePbId) return g.blackPbId.takeIf { it.isNotBlank() }
        }
        if (current.hostPbId == myPbId) return current.guestPbId?.takeIf { it.isNotBlank() }
        if (current.guestPbId == myPbId) return current.hostPbId.takeIf { it.isNotBlank() }
        return state.members
            .firstOrNull { it.status == LobbyMemberStatus.JOINED.wire && it.pbId != myPbId }
            ?.pbId
    }

    private fun resolveState(dto: GameRoomDto): GameRoomStatePayload {
        val entry = SocialGameCatalog.find(dto.gameType)
        val maxPlayers = entry?.maxPlayers?.coerceIn(2, 4) ?: 2
        val minPlayers = entry?.minPlayers?.coerceIn(2, maxPlayers) ?: 2
        val raw = dto.gameState ?: GameRoomStateCodec.fromLegacy(
            dto.hostPbId,
            dto.guestPbId,
            maxPlayers,
            minPlayers,
            dto.status,
            dto.inviteMode,
            dto.guestReady,
        )
        return GameRoomStateCodec.normalize(raw, dto.hostPbId)
    }

    private suspend fun GameRoomDto.toCache(
        userId: Long,
        myPbId: String,
        friends: List<SocialFriendCache>,
        token: String,
        fetchMissingProfiles: Boolean = true,
    ): SocialGameRoomCache {
        val state = resolveState(this)
        val members = enrichMembers(state, hostProfile, guestProfile, friends, token, fetchMissingProfiles)
        val peerPbId = if (hostPbId == myPbId) guestPbId else hostPbId
        val hostName = hostProfile?.displayName?.ifBlank { hostProfile.funlifeUsername }.orEmpty()
        val hostAvatar = hostProfile?.avatarUrl.orEmpty()
        val guestName = when {
            guestPbId.isNullOrBlank() -> ""
            guestProfile != null -> guestProfile.displayName.ifBlank { guestProfile.funlifeUsername }
            else -> friends.firstOrNull { it.friendPbId == guestPbId }?.displayName.orEmpty()
        }
        val guestAvatar = when {
            guestPbId.isNullOrBlank() -> ""
            guestProfile != null -> guestProfile.avatarUrl.orEmpty()
            else -> friends.firstOrNull { it.friendPbId == guestPbId }?.avatarUrl.orEmpty()
        }
        val peerName = when {
            peerPbId == null -> ""
            guestProfile != null && guestPbId == peerPbId ->
                guestProfile.displayName.ifBlank { guestProfile.funlifeUsername }
            hostProfile != null && hostPbId == peerPbId ->
                hostProfile.displayName.ifBlank { hostProfile.funlifeUsername }
            else -> friends.firstOrNull { it.friendPbId == peerPbId }?.displayName.orEmpty()
        }
        val peerAvatar = when {
            peerPbId == null -> ""
            guestProfile != null && guestPbId == peerPbId -> guestProfile.avatarUrl.orEmpty()
            hostProfile != null && hostPbId == peerPbId -> hostProfile.avatarUrl.orEmpty()
            else -> friends.firstOrNull { it.friendPbId == peerPbId }?.avatarUrl.orEmpty()
        }
        return SocialGameRoomCache(
            userId = userId,
            roomId = id,
            gameType = gameType,
            inviteMode = inviteMode.wire,
            roomCode = roomCode,
            hostPbId = hostPbId,
            guestPbId = guestPbId,
            hostDisplayName = hostName,
            hostAvatarUrl = hostAvatar,
            guestProfileName = guestName,
            guestProfileAvatar = guestAvatar,
            guestDisplayName = peerName,
            peerAvatarUrl = peerAvatar,
            status = status.wire,
            inviteMessage = inviteMessage,
            declinedByGuest = declinedByGuest,
            declinedByPbId = declinedByPbId.orEmpty(),
            membersJson = gson.toJson(members),
            maxPlayers = state.maxPlayers,
            minPlayers = state.minPlayers,
            pendingInvitePbId = state.pendingInvitePbId.orEmpty(),
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
        )
    }

    private suspend fun enrichMembers(
        state: GameRoomStatePayload,
        hostProfile: PbUserProfile?,
        guestProfile: PbUserProfile?,
        friends: List<SocialFriendCache>,
        token: String,
        fetchMissingProfiles: Boolean,
    ): List<LobbyMember> {
        val profileById = mutableMapOf<String, PbUserProfile>()
        hostProfile?.let { profileById[it.id] = it }
        guestProfile?.let { profileById[it.id] = it }

        fun resolveProfile(pbId: String): PbUserProfile? {
            profileById[pbId]?.let { return it }
            friends.firstOrNull { it.friendPbId == pbId }?.let { friend ->
                return PbUserProfile(
                    id = pbId,
                    funlifeUsername = friend.funlifeUsername,
                    displayName = friend.displayName.ifBlank { friend.funlifeUsername },
                    avatarUrl = friend.avatarUrl,
                    online = friend.online,
                ).also { profileById[pbId] = it }
            }
            return if (fetchMissingProfiles) {
                api.getUserById(token, pbId)?.also { profileById[pbId] = it }
            } else {
                null
            }
        }

        return state.members.map { wire ->
            val profile = resolveProfile(wire.pbId)
            val name = profile?.displayName?.ifBlank { profile.funlifeUsername }.orEmpty()
            LobbyMember(
                pbId = wire.pbId,
                seat = wire.seat,
                status = LobbyMemberStatus.fromWire(wire.status),
                displayName = name.takeIf { it.isNotBlank() },
                avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() },
            )
        }
    }

    private fun parseMembersJson(json: String): List<LobbyMember> =
        runCatching {
            val type = object : TypeToken<List<LobbyMember>>() {}.type
            gson.fromJson<List<LobbyMember>>(json, type)
        }.getOrDefault(emptyList())

    private fun membersMissingProfiles(members: List<LobbyMember>): Boolean =
        members.any {
            it.status == LobbyMemberStatus.JOINED &&
                (it.avatarUrl.isNullOrBlank() || it.displayName.isNullOrBlank())
        }

    private fun SocialGameRoomCache.mergeFromPrevious(previous: SocialGameRoomCache?): SocialGameRoomCache {
        if (previous == null) return this
        val prevById = parseMembersJson(previous.membersJson).associateBy { it.pbId }
        val freshMembers = parseMembersJson(membersJson)
        val freshMemberIds = freshMembers.map { it.pbId }.toSet()
        val mergedMembers = freshMembers.map { member ->
            val prev = prevById[member.pbId] ?: return@map member
            member.copy(
                displayName = member.displayName?.takeIf { it.isNotBlank() } ?: prev.displayName,
                avatarUrl = member.avatarUrl?.takeIf { it.isNotBlank() } ?: prev.avatarUrl,
            )
        }
        val guestStillSeated = !guestPbId.isNullOrBlank() && guestPbId in freshMemberIds
        return copy(
            membersJson = gson.toJson(mergedMembers),
            hostDisplayName = hostDisplayName.takeIf { it.isNotBlank() } ?: previous.hostDisplayName,
            hostAvatarUrl = hostAvatarUrl.takeIf { it.isNotBlank() } ?: previous.hostAvatarUrl,
            guestProfileName = if (guestStillSeated) {
                guestProfileName.takeIf { it.isNotBlank() } ?: previous.guestProfileName
            } else {
                guestProfileName
            },
            guestProfileAvatar = if (guestStillSeated) {
                guestProfileAvatar.takeIf { it.isNotBlank() } ?: previous.guestProfileAvatar
            } else {
                guestProfileAvatar
            },
            guestDisplayName = if (guestStillSeated) {
                guestDisplayName.takeIf { it.isNotBlank() } ?: previous.guestDisplayName
            } else {
                guestDisplayName
            },
            peerAvatarUrl = if (guestStillSeated) {
                peerAvatarUrl.takeIf { it.isNotBlank() } ?: previous.peerAvatarUrl
            } else {
                peerAvatarUrl
            },
        )
    }

    private fun SocialGameRoomCache.toDraft(): LocalGameRoomDraft {
        val entry = SocialGameCatalog.find(gameType)
        val members = parseMembersJson(membersJson)
        val memberIds = members.map { it.pbId }.toSet()
        return LocalGameRoomDraft(
            roomId = roomId,
            roomCode = roomCode,
            gameId = gameType,
            gameTitle = entry?.title ?: gameType,
            inviteMode = InviteMode.fromWire(inviteMode),
            status = GameRoomStatus.fromWire(status),
            hostPbId = hostPbId,
            guestPbId = guestPbId?.takeIf { it in memberIds || it == pendingInvitePbId },
            hostDisplayName = hostDisplayName.takeIf { it.isNotBlank() },
            hostAvatarUrl = hostAvatarUrl.takeIf { it.isNotBlank() },
            guestDisplayName = guestProfileName.takeIf { it.isNotBlank() },
            guestAvatarUrl = guestProfileAvatar.takeIf { it.isNotBlank() },
            peerDisplayName = guestDisplayName.takeIf { it.isNotBlank() },
            peerAvatarUrl = peerAvatarUrl.takeIf { it.isNotBlank() },
            declinedByGuest = declinedByGuest,
            declinedByPbId = declinedByPbId.takeIf { it.isNotBlank() },
            members = members,
            maxPlayers = maxPlayers,
            minPlayers = minPlayers,
            pendingInvitePbId = pendingInvitePbId.takeIf { it.isNotBlank() },
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
        )
    }
}
