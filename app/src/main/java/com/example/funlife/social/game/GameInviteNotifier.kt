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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用级游戏邀请通知：不依赖任何 [GameCenterViewModel] 实例（MainActivity / NavGraph 各自 VM 也能共享）。
 */
object GameInviteNotifier {

    private const val TAG = "GameInviteNotifier"
    private val notifiedRoomIds = ConcurrentHashMap<Long, MutableSet<String>>()
    private val handledRoomIds = ConcurrentHashMap<Long, MutableSet<String>>()
    private val _handledSnapshot = MutableStateFlow<Map<Long, Set<String>>>(emptyMap())
    val handledSnapshot: StateFlow<Map<Long, Set<String>>> = _handledSnapshot.asStateFlow()

    fun handledFor(userId: Long): Set<String> = _handledSnapshot.value[userId].orEmpty()

    fun isHandled(userId: Long, roomId: String): Boolean =
        roomId in handledRoomIds.getOrDefault(userId, emptySet())

    /** 用户已点接受/婉拒：永久门禁，直到登出 [clearUser]；不因本地缓存闪断而清除。 */
    fun markHandled(userId: Long, roomId: String) {
        handledRoomIds.getOrPut(userId) { mutableSetOf() }.add(roomId)
        notifiedRoomIds.getOrPut(userId) { mutableSetOf() }.add(roomId)
        publishHandledSnapshot()
        if (SocialAlertBus.alert.value?.id == alertId(roomId)) {
            SocialAlertBus.dismiss()
        }
        Log.d(TAG, "markHandled userId=$userId room=$roomId")
    }

    fun unmarkHandled(userId: Long, roomId: String) {
        if (handledRoomIds[userId]?.remove(roomId) == true) {
            publishHandledSnapshot()
            Log.d(TAG, "unmarkHandled userId=$userId room=$roomId")
        }
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
            .filter { !isHandled(userId, it.roomId) }
        Log.i(TAG, "publish check userId=$userId myPbId=$myPbId incoming=${incoming.size} " +
            "rooms=${incoming.map { it.roomId }} handled=${handledFor(userId)} foreground=${SocialAlertBus.isAppForeground}")
        val seen = notifiedRoomIds.getOrPut(userId) { mutableSetOf() }
        incoming.forEach { room ->
            if (room.roomId in seen) return@forEach
            seen.add(room.roomId)
            val host = room.hostDisplayName ?: room.peerDisplayName ?: "好友"
            Log.i(TAG, "publish banner room=${room.roomId} host=$host game=${room.gameTitle}")
            SocialAlertBus.publish(
                SocialHeadsUpAlert(
                    id = alertId(room.roomId),
                    title = "对战邀请",
                    body = "$host 邀请你玩${room.gameTitle}",
                    deepLinkRoute = Screen.SocialGameLobby.route(room.roomId),
                ),
            )
        }
    }

    fun clearUser(userId: Long) {
        notifiedRoomIds.remove(userId)
        handledRoomIds.remove(userId)
        publishHandledSnapshot()
    }

    private fun publishHandledSnapshot() {
        _handledSnapshot.value = handledRoomIds.mapValues { (_, set) -> set.toSet() }
    }

    private fun alertId(roomId: String) = "game_invite_$roomId"
}
