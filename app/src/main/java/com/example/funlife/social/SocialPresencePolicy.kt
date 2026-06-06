package com.example.funlife.social

/** 在线状态判定：强杀进程不会走 onStop，需结合 PB users.updated 心跳过期。 */
object SocialPresencePolicy {

    /** 超过此时间未更新 users 记录，则视为离线（即使 online=true） */
    private const val STALE_MS = 90_000L

    fun isEffectivelyOnline(onlineFlag: Boolean, userUpdatedAtMs: Long): Boolean {
        if (!onlineFlag) return false
        if (userUpdatedAtMs <= 0L) return false
        return System.currentTimeMillis() - userUpdatedAtMs <= STALE_MS
    }
}
