// ═════════════════════════════════════════════════════════════════════════
// NotificationCenter.kt
// 企业级通知中心 — 唯一统一发送入口
// 自动校验：总开关 / 渠道开关 / 静默时段（强提醒可绕过）/ 24h 去重
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.funlife.MainActivity
import com.example.funlife.R

/**
 * 一条待发送通知的描述。
 * @param channel    所属渠道
 * @param id         通知 id（同 id 会替换旧的）
 * @param title      标题
 * @param body       内容（支持 BigText）
 * @param emojiPrefix 是否在标题前加渠道 emoji（默认开）
 * @param deepLinkRoute 点击跳转的导航 route（在 MainActivity 内消费），可空
 * @param dedupWindowMs 同一渠道在该窗口内只允许发一次（0 表示不去重）
 * @param bypassQuietHours 强制不被静默时段拦截
 * @param onlyIfChannelEnabled 当前渠道关闭时是否拦截（默认 true）
 * @param alwaysDeliverInbox 为 true 时：无论总开关/渠道/静默，都写入收件箱（首页铃铛红点）
 * @param skipInbox 为 true 时：不写首页铃铛收件箱（私聊等仅系统栏 + 应用内横幅）
 * @param inboxDedupeKey 收件箱去重键（同一 key 只保留一条，如 friend_req_xxx）
 */
data class NotificationSpec(
    val channel: FunChannel,
    val id: Int,
    val title: String,
    val body: String,
    val emojiPrefix: Boolean = true,
    val deepLinkRoute: String? = null,
    val dedupWindowMs: Long = 0L,
    val bypassQuietHours: Boolean = false,
    val onlyIfChannelEnabled: Boolean = true,
    val alwaysDeliverInbox: Boolean = false,
    val skipInbox: Boolean = false,
    val inboxDedupeKey: String? = null,
)

object NotificationCenter {
    private const val TAG = "NotificationCenter"
    const val EXTRA_DEEP_LINK = "fun_deep_link"

    /**
     * 发送一条通知。返回是否真的发出。
     * 不会抛出异常 —— 任何系统级错误都被吞掉并记录日志，避免拖垮调用方。
     */
    fun notify(context: Context, spec: NotificationSpec): Boolean {
        return try {
            NotificationChannels.ensureAll(context)

            if (spec.alwaysDeliverInbox && !spec.skipInbox) {
                runCatching {
                    InboxStore.add(
                        context, spec.channel, spec.title, spec.body, spec.deepLinkRoute,
                        dedupeKey = spec.inboxDedupeKey,
                    )
                }
            }

            val globalOn = NotificationPrefs.isGlobalEnabled(context)
            val channelOn = !spec.onlyIfChannelEnabled ||
                NotificationPrefs.isChannelEnabled(context, spec.channel)
            val quietBlocked = !spec.bypassQuietHours && spec.channel.respectQuietHours &&
                NotificationPrefs.nowInQuietHours(context)
            val dedupBlocked = spec.dedupWindowMs > 0 &&
                NotificationPrefs.firedWithin(context, spec.channel, spec.dedupWindowMs)

            val canShowSystem = globalOn && channelOn && !quietBlocked && !dedupBlocked
            if (!canShowSystem) {
                if (spec.alwaysDeliverInbox) {
                    android.util.Log.d(TAG, "inbox only (system blocked): ${spec.channel.id}")
                    return true
                }
                when {
                    !globalOn -> android.util.Log.d(TAG, "blocked by global switch: ${spec.channel.id}")
                    !channelOn -> android.util.Log.d(TAG, "blocked by channel switch: ${spec.channel.id}")
                    quietBlocked -> android.util.Log.d(TAG, "blocked by quiet hours: ${spec.channel.id}")
                    dedupBlocked -> android.util.Log.d(TAG, "blocked by dedup: ${spec.channel.id}")
                }
                return false
            }

            // 构造 PendingIntent → 打开 MainActivity，可携带深链
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                spec.deepLinkRoute?.let { putExtra(EXTRA_DEEP_LINK, it) }
            }
            val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val contentIntent = PendingIntent.getActivity(
                context,
                spec.channel.id.hashCode() xor spec.id,
                openIntent,
                piFlags
            )

            val title = if (spec.emojiPrefix) "${spec.channel.emoji} ${spec.title}" else spec.title
            if (!spec.alwaysDeliverInbox && !spec.skipInbox) {
                runCatching {
                    InboxStore.add(
                        context, spec.channel, spec.title, spec.body, spec.deepLinkRoute,
                        dedupeKey = spec.inboxDedupeKey,
                    )
                }
            }

            val notificationBuilder = NotificationCompat.Builder(context, spec.channel.id)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(spec.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(spec.body))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)

            if (spec.channel == FunChannel.SOCIAL) {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setOnlyAlertOnce(false)
                    .setVibrate(longArrayOf(0, 120, 60, 120, 60, 180))
            } else {
                notificationBuilder
                    .setPriority(mapPriority(spec.channel.systemImportance))
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
            }

            val notification = notificationBuilder.build()

            try {
                NotificationManagerCompat.from(context).notify(spec.id, notification)
            } catch (_: SecurityException) {
                // 缺 POST_NOTIFICATIONS：系统栏不弹，但收件箱与红点仍有效
                if (spec.dedupWindowMs > 0) NotificationPrefs.markFired(context, spec.channel)
                return true
            }

            if (spec.dedupWindowMs > 0) NotificationPrefs.markFired(context, spec.channel)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "notify failed", e)
            false
        }
    }

    fun cancel(context: Context, id: Int) {
        try { NotificationManagerCompat.from(context).cancel(id) } catch (_: Throwable) {}
    }

    fun cancelAll(context: Context) {
        try { NotificationManagerCompat.from(context).cancelAll() } catch (_: Throwable) {}
    }

    private fun mapPriority(importance: Int): Int = when {
        importance >= android.app.NotificationManager.IMPORTANCE_HIGH ->
            NotificationCompat.PRIORITY_HIGH
        importance >= android.app.NotificationManager.IMPORTANCE_DEFAULT ->
            NotificationCompat.PRIORITY_DEFAULT
        else -> NotificationCompat.PRIORITY_LOW
    }
}
