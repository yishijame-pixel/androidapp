package com.example.funlife.social.game.model

/**
 * 大厅退出策略：统一 host/guest/pending 的服务端动作，避免 UI 层散落 if-else。
 */
enum class LobbyExitAction {
    /** 仅本地导航（如房主暂时离开，房间保留） */
    LOCAL_ONLY,
    /** 宾客拒绝待处理邀请 */
    REJECT_INVITE,
    /** 已加入宾客离座 */
    LEAVE_SEAT,
}

object LobbyExitPolicy {

    fun resolve(
        isHost: Boolean,
        guestCanRespond: Boolean,
        isRoomParticipant: Boolean,
        isJoinedGuest: Boolean = false,
    ): LobbyExitAction = when {
        guestCanRespond -> LobbyExitAction.REJECT_INVITE
        !isHost && (isRoomParticipant || isJoinedGuest) -> LobbyExitAction.LEAVE_SEAT
        else -> LobbyExitAction.LOCAL_ONLY
    }

    fun needsServerNotify(action: LobbyExitAction): Boolean =
        action != LobbyExitAction.LOCAL_ONLY
}
