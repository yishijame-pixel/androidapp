package com.example.funlife.social.game.model

enum class GameCenterTab {
    ONLINE,
    LOCAL,
    MY_GAMES,
}

enum class InviteMode(val wire: String) {
    DIRECT("direct"),
    OPEN("open"),
    ;

    companion object {
        fun fromWire(wire: String?): InviteMode =
            entries.firstOrNull { it.wire == wire } ?: DIRECT
    }
}

enum class GameRoomStatus(val wire: String) {
    WAITING("waiting"),
    ACCEPTED("accepted"),
    PLAYING("playing"),
    FINISHED("finished"),
    CANCELLED("cancelled"),
    EXPIRED("expired"),
    ABANDONED("abandoned"),
    ;

    companion object {
        fun fromWire(wire: String?): GameRoomStatus =
            entries.firstOrNull { it.wire == wire } ?: WAITING

        val ACTIVE: Set<GameRoomStatus> = setOf(WAITING, ACCEPTED, PLAYING)
    }
}

data class LocalGameRoomDraft(
    val roomId: String,
    val roomCode: String,
    val gameId: String,
    val gameTitle: String,
    val inviteMode: InviteMode,
    val status: GameRoomStatus,
    val hostPbId: String,
    val guestPbId: String? = null,
    val hostDisplayName: String? = null,
    val hostAvatarUrl: String? = null,
    val guestDisplayName: String? = null,
    val guestAvatarUrl: String? = null,
    val peerDisplayName: String? = null,
    val peerAvatarUrl: String? = null,
    val declinedByGuest: Boolean = false,
    val declinedByPbId: String? = null,
    val members: List<LobbyMember> = emptyList(),
    val maxPlayers: Int = 2,
    val minPlayers: Int = 2,
    val pendingInvitePbId: String? = null,
    val createdAtMs: Long,
) {
    val joinedMembers: List<LobbyMember>
        get() = members.filter { it.status == LobbyMemberStatus.JOINED }

    val joinedCount: Int
        get() = joinedMembers.size

    val hasGuestJoined: Boolean
        get() = status == GameRoomStatus.ACCEPTED ||
            status == GameRoomStatus.PLAYING ||
            joinedCount >= 2

    val isInvitePending: Boolean
        get() = !pendingInvitePbId.isNullOrBlank() && status == GameRoomStatus.WAITING

    /** 受邀方首页弹层：direct 邀请 / pending 成员 / guest 字段任一命中即可。 */
    fun isIncomingInviteFor(myPbId: String): Boolean {
        if (myPbId.isBlank() || hostPbId == myPbId || status != GameRoomStatus.WAITING) return false
        if (pendingInvitePbId == myPbId) return true
        if (guestPbId == myPbId && !hasGuestJoined) return true
        if (members.any { it.pbId == myPbId && it.status == LobbyMemberStatus.PENDING }) return true
        return false
    }

    val isSoloLobby: Boolean
        get() = joinedCount <= 1 && !isInvitePending

    val isRoomFull: Boolean
        get() = joinedCount >= maxPlayers

    val canStartGame: Boolean
        get() = joinedCount >= minPlayers &&
            status in setOf(GameRoomStatus.WAITING, GameRoomStatus.ACCEPTED)
}

data class MyGameItemUi(
    val roomId: String,
    val gameId: String,
    val gameTitle: String,
    val gameEmoji: String,
    val status: GameRoomStatus,
    val subtitle: String,
    val accentColors: List<Long>,
)
