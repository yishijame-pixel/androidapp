package com.example.funlife.resource

import android.content.Context
import com.example.funlife.notifications.FunChannel
import com.example.funlife.notifications.NotificationCenter
import com.example.funlife.notifications.NotificationSpec

/**
 * 游戏资源后台下载结果推送（系统通知 + 收件箱）。
 */
object GameResourceDownloadNotifier {

    private const val NOTIF_ID_SUCCESS = 0x60A7E001
    private const val NOTIF_ID_FAILURE = 0x60A7E002

    fun notifySuccess(context: Context, bundleIds: List<String>) {
        if (bundleIds.isEmpty()) return
        val names = bundleIds.map { GameResourceBundles.displayName(it) }.distinct()
        val body = when (names.size) {
            1 -> "${names.first()}已更新，可以畅玩了"
            else -> "${names.joinToString("、")}已更新，可以畅玩了"
        }
        NotificationCenter.notify(
            context.applicationContext,
            NotificationSpec(
                channel = FunChannel.GAME_RESOURCE,
                id = NOTIF_ID_SUCCESS,
                title = "游戏资源下载完成",
                body = body,
                deepLinkRoute = null,
                dedupWindowMs = 5_000L,
                bypassQuietHours = false,
            ),
        )
    }

    fun notifyFailure(context: Context, detail: String?) {
        val body = detail?.takeIf { it.isNotBlank() }
            ?: "请检查网络后重试，或在首页点击「重试」"
        NotificationCenter.notify(
            context.applicationContext,
            NotificationSpec(
                channel = FunChannel.GAME_RESOURCE,
                id = NOTIF_ID_FAILURE,
                title = "游戏资源下载失败",
                body = body,
                deepLinkRoute = null,
                dedupWindowMs = 10_000L,
                bypassQuietHours = true,
            ),
        )
    }
}
