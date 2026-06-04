package com.example.funlife.notifications

import android.content.Context

/** 私聊新消息 → 系统通知 + 应用内 heads-up 横幅 */
object ChatMessageNotifier {

    private const val PREFS = "fun_chat_message_notif"
    private const val K_NOTIFIED_PREFIX = "notified_"
    private val notifyLock = Any()

    fun notifyIncoming(
        ctx: Context,
        userId: Long,
        messageId: String,
        peerPbId: String,
        peerDisplayName: String,
        peerUsername: String,
        body: String,
    ) {
        if (userId <= 0L || messageId.isBlank() || peerPbId.isBlank()) return
        synchronized(notifyLock) {
            val appCtx = ctx.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val key = "${K_NOTIFIED_PREFIX}$userId"
            val notified = HashSet(prefs.getStringSet(key, emptySet()) ?: emptySet())
            if (messageId in notified) return

            val preview = body.trim().replace('\n', ' ').take(80)
            val name = peerDisplayName.ifBlank { peerUsername }
            val username = peerUsername.ifBlank { "用户" }
            val dedupeKey = "chat_msg_$messageId"
            val deepLink = "friend_chat/$peerPbId"

            val delivered = NotificationCenter.notify(
                appCtx,
                NotificationSpec(
                    channel = FunChannel.SOCIAL,
                    id = messageId.hashCode() and 0x7FFFFFFF,
                    title = name,
                    body = if (preview.isBlank()) "发来一条新消息" else preview,
                    deepLinkRoute = deepLink,
                    dedupWindowMs = 0L,
                    bypassQuietHours = true,
                    skipInbox = true,
                    inboxDedupeKey = dedupeKey,
                ),
            )
            if (delivered) {
                notified.add(messageId)
                prefs.edit().putStringSet(key, notified).apply()
                SocialAlertBus.publish(
                    SocialHeadsUpAlert(
                        id = messageId,
                        title = name,
                        body = if (preview.isBlank()) "@$username 发来新消息" else preview,
                        deepLinkRoute = deepLink,
                    ),
                )
            }
        }
    }

    fun clearUser(ctx: Context, userId: Long) {
        if (userId <= 0L) return
        ctx.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("${K_NOTIFIED_PREFIX}$userId")
            .apply()
    }
}
