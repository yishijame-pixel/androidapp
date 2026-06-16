package com.example.funlife.social.game

import android.content.Context
import android.util.Log
import com.example.funlife.FunLifeApplication
import com.example.funlife.navigation.Screen
import com.example.funlife.notifications.SocialAlertBus
import com.example.funlife.notifications.SocialHeadsUpAlert
import com.example.funlife.repository.GameRoomRepository
import com.example.funlife.repository.SocialLinkRepository
import com.example.funlife.social.SocialSessionManager
import com.example.funlife.social.game.model.LocalGameRoomDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用级游戏邀请通知：不依赖任何 [GameCenterViewModel] 实例（MainActivity / NavGraph 各自 VM 也能共享）。
 *
 * 去重键为 (roomId, updatedAtMs)，同一房间再次邀请会触发新通知。
 */
object GameInviteNotifier {

    private const val TAG = "GameInviteNotifier"
    /** 已推送过的邀请版本：roomId -> 上次通知时的 room.updatedAtMs */
    private val lastNotifiedUpdatedAt = ConcurrentHashMap<Long, MutableMap<String, Long>>()
    private val handledRoomIds = ConcurrentHashMap<Long, MutableSet<String>>()
    /** markHandled 时记录的 room.updatedAtMs（或本地时间兜底） */
    private val handledUpdatedAt = ConcurrentHashMap<Long, MutableMap<String, Long>>()
    private val _handledSnapshot = MutableStateFlow<Map<Long, Set<String>>>(emptyMap())
    val handledSnapshot: StateFlow<Map<Long, Set<String>>> = _handledSnapshot.asStateFlow()

    fun handledFor(userId: Long): Set<String> = _handledSnapshot.value[userId].orEmpty()

    fun isHandled(userId: Long, roomId: String): Boolean =
        roomId in handledRoomIds.getOrDefault(userId, emptySet())

    /**
     * 用户已点接受/婉拒：在 [roomUpdatedAtMs] 未变前不再弹层。
     * 房主再次邀请（updated 变大）时自动解除。
     */
    fun markHandled(userId: Long, roomId: String, roomUpdatedAtMs: Long = 0L) {
        handledRoomIds.getOrPut(userId) { mutableSetOf() }.add(roomId)
        if (roomUpdatedAtMs > 0L) {
            handledUpdatedAt.getOrPut(userId) { mutableMapOf() }[roomId] = roomUpdatedAtMs
        } else {
            handledUpdatedAt[userId]?.remove(roomId)
        }
        publishHandledSnapshot()
        if (SocialAlertBus.alert.value?.id == alertId(roomId)) {
            SocialAlertBus.dismiss()
        }
        Log.d(TAG, "markHandled userId=$userId room=$roomId at=$roomUpdatedAtMs")
    }

    fun unmarkHandled(userId: Long, roomId: String) {
        if (handledRoomIds[userId]?.remove(roomId) == true) {
            handledUpdatedAt[userId]?.remove(roomId)
            publishHandledSnapshot()
            Log.d(TAG, "unmarkHandled userId=$userId room=$roomId")
        }
    }

    /** 服务端 room.updated 比已处理版本新 → 视为新一轮邀请，清除门禁 */
    fun reconcileHandled(userId: Long, room: LocalGameRoomDraft, myPbId: String) {
        if (!isHandled(userId, room.roomId)) return
        if (!room.isIncomingInviteFor(myPbId)) {
            unmarkHandled(userId, room.roomId)
            return
        }
        val handledAt = handledUpdatedAt[userId]?.get(room.roomId) ?: 0L
        if (handledAt <= 0L || room.updatedAtMs > handledAt) {
            unmarkHandled(userId, room.roomId)
            Log.d(TAG, "reconcileHandled reopen userId=$userId room=${room.roomId} updated=${room.updatedAtMs}")
        }
    }

    private fun shouldNotify(userId: Long, room: LocalGameRoomDraft, myPbId: String): Boolean {
        reconcileHandled(userId, room, myPbId)
        if (isHandled(userId, room.roomId)) return false
        val last = lastNotifiedUpdatedAt[userId]?.get(room.roomId) ?: 0L
        return room.updatedAtMs > last
    }

    suspend fun publishNewInvites(ctx: Context, userId: Long) {
        if (userId <= 0L) return
        val appCtx = ctx.applicationContext
        val db = (appCtx as? FunLifeApplication)?.database ?: return
        val socialDao = db.socialDao()
        val myPbId = socialDao.getLink(userId)?.pbRecordId?.takeIf { it.isNotBlank() }
            ?: SocialSessionManager.snapshot.value.pbRecordId?.takeIf { it.isNotBlank() }
            ?: run {
                Log.w(TAG, "skip publish: no pbRecordId userId=$userId")
                return
            }
        val repo = GameRoomRepository(appCtx, socialDao, SocialLinkRepository(appCtx, socialDao))
        val incoming = repo.listIncomingInviteDrafts(userId, myPbId)
        Log.i(
            TAG,
            "publish check userId=$userId myPbId=$myPbId incoming=${incoming.size} " +
                "rooms=${incoming.map { "${it.roomId}@${it.updatedAtMs}" }} handled=${handledFor(userId)} " +
                "foreground=${SocialAlertBus.isAppForeground}",
        )
        val notified = lastNotifiedUpdatedAt.getOrPut(userId) { mutableMapOf() }
        incoming.forEach { room ->
            if (!shouldNotify(userId, room, myPbId)) return@forEach
            notified[room.roomId] = room.updatedAtMs
            val host = room.hostDisplayName ?: room.peerDisplayName ?: "好友"
            Log.i(TAG, "publish banner room=${room.roomId} updated=${room.updatedAtMs} host=$host game=${room.gameTitle}")
            SocialAlertBus.publish(
                SocialHeadsUpAlert(
                    id = alertId(room.roomId),
                    title = "对战邀请",
                    body = "$host 邀请你玩${room.gameTitle}",
                    deepLinkRoute = if (room.gameId == "pac_maze") {
                        Screen.pacMazeRoute(onlineLobbyRoomId = room.roomId)
                    } else {
                        Screen.SocialGameLobby.route(room.roomId)
                    },
                ),
            )
        }
    }

    fun clearUser(userId: Long) {
        lastNotifiedUpdatedAt.remove(userId)
        handledRoomIds.remove(userId)
        handledUpdatedAt.remove(userId)
        publishHandledSnapshot()
    }

    private fun publishHandledSnapshot() {
        _handledSnapshot.value = handledRoomIds.mapValues { (_, set) -> set.toSet() }
    }

    private fun alertId(roomId: String) = "game_invite_$roomId"
}
