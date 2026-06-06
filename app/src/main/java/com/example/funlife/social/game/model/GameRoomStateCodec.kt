package com.example.funlife.social.game.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

enum class LobbyMemberStatus(val wire: String) {
    JOINED("joined"),
    PENDING("pending"),
    ;

    companion object {
        fun fromWire(wire: String?): LobbyMemberStatus =
            entries.firstOrNull { it.wire == wire } ?: JOINED
    }
}

data class LobbyMember(
    val pbId: String,
    val seat: Int,
    val status: LobbyMemberStatus,
    val displayName: String? = null,
    val avatarUrl: String? = null,
)

data class GameRoomStatePayload(
    @SerializedName("max_players") val maxPlayers: Int = 2,
    @SerializedName("min_players") val minPlayers: Int = 2,
    val members: List<GameRoomMemberWire> = emptyList(),
    @SerializedName("pending_invite_pb_id") val pendingInvitePbId: String? = null,
    @SerializedName("declined_by_pb_id") val declinedByPbId: String? = null,
    @SerializedName("member_ids") val memberIds: List<String> = emptyList(),
)

data class GameRoomMemberWire(
    @SerializedName("pb_id") val pbId: String,
    val seat: Int,
    val status: String,
)

object GameRoomStateCodec {
    private val gson = Gson()

    fun initial(hostPbId: String, maxPlayers: Int, minPlayers: Int): GameRoomStatePayload {
        val host = GameRoomMemberWire(hostPbId, 0, LobbyMemberStatus.JOINED.wire)
        return GameRoomStatePayload(
            maxPlayers = maxPlayers.coerceIn(2, 4),
            minPlayers = minPlayers.coerceIn(2, maxPlayers),
            members = listOf(host),
            memberIds = listOf(hostPbId),
        )
    }

    fun parse(element: JsonElement?): GameRoomStatePayload? {
        if (element == null || element.isJsonNull) return null
        val obj = when {
            element.isJsonObject -> element
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                runCatching { JsonParser.parseString(element.asString) }.getOrNull()
                    ?.takeIf { it.isJsonObject }
            }
            else -> null
        } ?: return null
        return runCatching { gson.fromJson(obj, GameRoomStatePayload::class.java) }.getOrNull()
    }

    fun toMap(state: GameRoomStatePayload): Map<String, Any?> = buildMap {
        put("max_players", state.maxPlayers)
        put("min_players", state.minPlayers)
        put(
            "members",
            state.members.map {
                mapOf("pb_id" to it.pbId, "seat" to it.seat, "status" to it.status)
            },
        )
        put("member_ids", state.memberIds)
        state.pendingInvitePbId?.takeIf { it.isNotBlank() }?.let { put("pending_invite_pb_id", it) }
        state.declinedByPbId?.takeIf { it.isNotBlank() }?.let { put("declined_by_pb_id", it) }
    }

    fun fromLegacy(
        hostPbId: String,
        guestPbId: String?,
        maxPlayers: Int,
        minPlayers: Int,
        status: GameRoomStatus,
        inviteMode: InviteMode,
        guestReady: Boolean,
    ): GameRoomStatePayload {
        val members = mutableListOf(GameRoomMemberWire(hostPbId, 0, LobbyMemberStatus.JOINED.wire))
        var pending: String? = null
        if (!guestPbId.isNullOrBlank()) {
            val guestJoined = status == GameRoomStatus.ACCEPTED ||
                status == GameRoomStatus.PLAYING ||
                (inviteMode == InviteMode.OPEN && guestReady) ||
                (inviteMode == InviteMode.DIRECT && guestReady)
            if (guestJoined) {
                members += GameRoomMemberWire(guestPbId, 1, LobbyMemberStatus.JOINED.wire)
            } else if (inviteMode == InviteMode.DIRECT && status == GameRoomStatus.WAITING) {
                members += GameRoomMemberWire(guestPbId, 1, LobbyMemberStatus.PENDING.wire)
                pending = guestPbId
            }
        }
        return GameRoomStatePayload(
            maxPlayers = maxPlayers,
            minPlayers = minPlayers,
            members = members,
            pendingInvitePbId = pending,
            memberIds = members.map { it.pbId },
        )
    }

    fun joinedCount(state: GameRoomStatePayload): Int =
        state.members.count { it.status == LobbyMemberStatus.JOINED.wire }

    fun nextSeat(state: GameRoomStatePayload): Int =
        (state.members.maxOfOrNull { it.seat } ?: -1) + 1

    fun isMember(state: GameRoomStatePayload, pbId: String): Boolean =
        state.members.any { it.pbId == pbId }

    fun withPendingInvite(state: GameRoomStatePayload, guestPbId: String): GameRoomStatePayload {
        require(!isMember(state, guestPbId)) { "该好友已在房间内" }
        require(state.pendingInvitePbId.isNullOrBlank()) { "当前有进行中的邀请" }
        require(joinedCount(state) < state.maxPlayers) { "房间已满" }
        val seat = nextSeat(state)
        val members = state.members + GameRoomMemberWire(guestPbId, seat, LobbyMemberStatus.PENDING.wire)
        return state.copy(
            members = members,
            pendingInvitePbId = guestPbId,
            declinedByPbId = null,
            memberIds = members.map { it.pbId }.distinct(),
        )
    }

    fun withAcceptedInvite(state: GameRoomStatePayload, guestPbId: String): GameRoomStatePayload {
        val members = state.members.map {
            if (it.pbId == guestPbId) it.copy(status = LobbyMemberStatus.JOINED.wire) else it
        }
        return state.copy(
            members = members,
            pendingInvitePbId = null,
            declinedByPbId = null,
            memberIds = joinedMemberIds(members),
        )
    }

    fun withDirectJoin(state: GameRoomStatePayload, pbId: String): GameRoomStatePayload {
        require(!isMember(state, pbId)) { "你已在房间内" }
        require(joinedCount(state) < state.maxPlayers) { "房间已满" }
        val seat = nextSeat(state)
        val members = state.members + GameRoomMemberWire(pbId, seat, LobbyMemberStatus.JOINED.wire)
        return state.copy(
            members = members,
            memberIds = joinedMemberIds(members),
        )
    }

    fun withMemberLeft(state: GameRoomStatePayload, pbId: String): GameRoomStatePayload {
        val members = state.members.filter { it.pbId != pbId }
        return state.copy(
            members = members,
            pendingInvitePbId = if (state.pendingInvitePbId == pbId) null else state.pendingInvitePbId,
            declinedByPbId = null,
            memberIds = joinedMemberIds(members),
        )
    }

    fun withRejectedInvite(state: GameRoomStatePayload, guestPbId: String): GameRoomStatePayload {
        val members = state.members.filter { it.pbId != guestPbId }
        return state.copy(
            members = members,
            pendingInvitePbId = null,
            declinedByPbId = guestPbId,
            memberIds = allMemberIds(members),
        )
    }

    fun withWithdrawnInvite(state: GameRoomStatePayload): GameRoomStatePayload {
        val pendingId = state.pendingInvitePbId ?: return state.copy(declinedByPbId = null)
        val members = state.members.filter { it.pbId != pendingId }
        return state.copy(
            members = members,
            pendingInvitePbId = null,
            declinedByPbId = null,
            memberIds = allMemberIds(members),
        )
    }

    fun withClearedDecline(state: GameRoomStatePayload): GameRoomStatePayload =
        state.copy(declinedByPbId = null)

    fun resolveStatusAfterJoin(state: GameRoomStatePayload): GameRoomStatus =
        if (joinedCount(state) >= state.minPlayers.coerceAtLeast(2)) {
            GameRoomStatus.ACCEPTED
        } else {
            GameRoomStatus.WAITING
        }

    /**
     * 规范化 game_state：去重成员、同步 memberIds、清理悬空 pending。
     */
    fun normalize(state: GameRoomStatePayload, hostPbId: String? = null): GameRoomStatePayload {
        val deduped = state.members
            .groupBy { it.pbId }
            .values
            .map { group ->
                group.maxWith(
                    compareBy<GameRoomMemberWire> { it.status == LobbyMemberStatus.JOINED.wire }
                        .thenBy { -it.seat },
                )
            }
            .sortedBy { it.seat }
            .let { members ->
                if (hostPbId.isNullOrBlank()) return@let members
                val hostIdx = members.indexOfFirst { it.pbId == hostPbId }
                if (hostIdx <= 0) return@let members
                val host = members[hostIdx]
                val rest = members.filterIndexed { i, _ -> i != hostIdx }
                listOf(host.copy(seat = 0)) + rest.mapIndexed { i, m -> m.copy(seat = i + 1) }
            }
        val pending = state.pendingInvitePbId?.takeIf { pid ->
            deduped.any { it.pbId == pid && it.status == LobbyMemberStatus.PENDING.wire }
        }
        val declined = state.declinedByPbId?.takeIf { id ->
            deduped.none { it.pbId == id }
        }
        return state.copy(
            members = deduped,
            pendingInvitePbId = pending,
            declinedByPbId = declined,
            memberIds = joinedMemberIds(deduped),
        )
    }

    private fun joinedMemberIds(members: List<GameRoomMemberWire>): List<String> =
        members.filter { it.status == LobbyMemberStatus.JOINED.wire }.map { it.pbId }

    private fun allMemberIds(members: List<GameRoomMemberWire>): List<String> =
        members.map { it.pbId }.distinct()
}
