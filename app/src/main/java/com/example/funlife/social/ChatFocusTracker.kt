package com.example.funlife.social

/**
 * 当前用户正在查看的私聊会话（按 FunLife userId + peerPbId）。
 * Realtime 收到消息时，若正在该会话内则不弹横幅/系统通知。
 */
object ChatFocusTracker {

    @Volatile
    private var focusedUserId: Long = 0L

    @Volatile
    private var focusedPeerPbId: String? = null

    fun setFocus(userId: Long, peerPbId: String?) {
        focusedUserId = userId
        focusedPeerPbId = peerPbId?.takeIf { it.isNotBlank() }
    }

    fun clearFocus(userId: Long) {
        if (focusedUserId == userId) {
            focusedUserId = 0L
            focusedPeerPbId = null
        }
    }

    fun isFocused(userId: Long, peerPbId: String): Boolean =
        userId == focusedUserId && peerPbId == focusedPeerPbId
}
