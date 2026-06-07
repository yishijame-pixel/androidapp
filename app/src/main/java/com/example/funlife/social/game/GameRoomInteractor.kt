package com.example.funlife.social.game

import android.content.Context
import com.example.funlife.FunLifeApplication
import com.example.funlife.repository.GameRoomRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.PocketBaseApiException
import com.example.funlife.social.SocialFailureException
import com.example.funlife.social.SocialOperationGate
import com.example.funlife.social.game.model.GameRoomStateMachine
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.social.game.GameInviteNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRoomInteractor(
    appCtx: Context,
    private val userId: Long,
) {
    private val ctx = appCtx.applicationContext
    private val socialDao = (ctx as FunLifeApplication).database.socialDao()
    private val linkRepo = SocialLinkRepository(ctx, socialDao)
    private val repo = GameRoomRepository(ctx, socialDao, linkRepo)

    fun observeRooms(): Flow<List<LocalGameRoomDraft>> = repo.observeRooms(userId)

    suspend fun refreshRooms(forceSession: Boolean = false): Result<Unit> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = "同步对局",
            timeoutMs = SocialOperationGate.TIMEOUT_SYNC_MS,
            forceSession = forceSession,
        ) { cred ->
            repo.refreshRooms(userId, cred.pbRecordId, cred.token)
        }

    /** 首页邀请送达：本地 Token 优先，401 时由 Gate 自动重绑。 */
    suspend fun refreshRoomsForDelivery(): Result<Unit> = refreshRooms(forceSession = false)

    /** 轻量邀请同步：仅 listIncomingGameInvites，适合 Realtime 在线时的兜底轮询。 */
    suspend fun refreshIncomingInvitesOnly(): Result<Unit> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = "同步邀请",
            timeoutMs = SocialOperationGate.TIMEOUT_SYNC_MS,
            forceSession = false,
        ) { cred ->
            repo.refreshIncomingInvitesOnly(userId, cred.pbRecordId, cred.token)
        }

    suspend fun refreshRoomById(roomId: String): Result<Unit> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = "同步房间",
            timeoutMs = SocialOperationGate.TIMEOUT_SYNC_MS,
            forceSession = false,
        ) { cred ->
            repo.refreshRoomById(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun dismissLocalRoom(roomId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                repo.dismissLocalRoom(userId, roomId)
            }
        }

    suspend fun createOpenRoom(gameId: String): Result<String> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = "开房间",
            forceSession = true,
        ) { cred ->
            repo.createOpenRoom(userId, cred.pbRecordId, cred.token, gameId)
        }

    suspend fun inviteFriendToRoom(roomId: String, guestPbId: String): Result<Unit> =
        mutateRoom(roomId, "邀请好友", forceSession = false) { cred ->
            repo.inviteFriendToRoom(userId, cred.pbRecordId, cred.token, roomId, guestPbId)
        }

    suspend fun clearDeclinedRoom(roomId: String): Result<Unit> =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                repo.clearDeclinedRoom(userId, roomId)
            }
        }

    suspend fun findHostActiveRoom(gameId: String): LocalGameRoomDraft? =
        withContext(Dispatchers.IO) {
            val pbId = SocialOperationGate.peek(ctx, userId)?.pbRecordId ?: return@withContext null
            repo.findHostActiveRoom(userId, pbId, gameId)
        }

    suspend fun joinByRoomCode(roomCode: String): Result<String> =
        SocialOperationGate.run(
            ctx = ctx,
            userId = userId,
            operation = "加入房间",
            forceSession = false,
        ) { cred ->
            repo.joinByRoomCode(userId, cred.pbRecordId, cred.token, roomCode)
        }

    suspend fun acceptInvite(roomId: String): Result<Unit> =
        mutateRoom(roomId, "接受邀请", forceSession = false) { cred ->
            repo.acceptInvite(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun withdrawInvite(roomId: String): Result<Unit> =
        mutateRoom(roomId, "撤回邀请", forceSession = false) { cred ->
            repo.withdrawInvite(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun dismissDeclineNotice(roomId: String): Result<Unit> =
        mutateRoom(roomId, "清除提示", forceSession = false) { cred ->
            repo.dismissDeclineNotice(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun rejectInvite(roomId: String): Result<Unit> =
        mutateRoom(roomId, "拒绝邀请", forceSession = false) { cred ->
            repo.rejectInvite(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun leaveRoom(roomId: String): Result<Unit> =
        mutateRoom(roomId, "离开房间", forceSession = false) { cred ->
            repo.leaveRoom(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun cancelRoom(roomId: String): Result<Unit> =
        mutateRoom(roomId, "取消房间", forceSession = false) { cred ->
            repo.cancelRoom(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun startGame(roomId: String): Result<Unit> =
        mutateRoom(roomId, "开始游戏", forceSession = false) { cred ->
            repo.startGame(userId, cred.pbRecordId, cred.token, roomId)
        }

    suspend fun abandonPlay(roomId: String): Result<Unit> =
        mutateRoom(roomId, "退出对局", forceSession = true) { cred ->
            repo.abandonPlay(userId, cred.pbRecordId, cred.token, roomId)
        }

    private suspend fun mutateRoom(
        roomId: String,
        operation: String,
        forceSession: Boolean = true,
        block: suspend (com.example.funlife.social.SocialCredentials) -> Result<Unit>,
    ): Result<Unit> {
        GameRoomSyncCoordinator.markMutating(roomId)
        return try {
            SocialOperationGate.run(
                ctx = ctx,
                userId = userId,
                operation = operation,
                forceSession = forceSession,
            ) { cred -> block(cred) }
        } finally {
            GameRoomSyncCoordinator.clearMutating(roomId)
        }
    }

    fun mapErrorMessage(t: Throwable): String {
        val root = generateSequence(t) { it.cause }.last()
        val raw = root.message.orEmpty()
        if (raw.contains("Unsupported Content-Type", ignoreCase = true)) {
            return "同步请求格式异常，请检查 PocketBase 与 draw_ws 是否已启动并重试"
        }
        return when (root) {
            is SocialFailureException -> root.failure.userMessage
            is PocketBaseApiException -> root.toUserMessage("操作失败")
            is GameRoomStateMachine.ConflictException -> root.message ?: "房间状态已变化，请重试"
            is IllegalStateException -> root.message ?: "操作失败"
            else -> when {
                raw.contains("Cannot be blank", ignoreCase = true) ->
                    "落子同步失败，请再点一次"
                raw.contains("requested resource", ignoreCase = true) ||
                    raw.contains("wasn't found", ignoreCase = true) ->
                    "房间不存在或无权访问，可能已结束"
                else -> raw.ifBlank { "网络异常，请稍后重试" }
            }
        }
    }
}
