// ═════════════════════════════════════════════════════════════════════════
// NotificationBootstrap.kt
// 启动入口 — App 启动 / 用户切换 / 开机重启时调用
// 负责：创建所有渠道 + 重新调度每日推送
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context

object NotificationBootstrap {

    fun init(context: Context) {
        try {
            NotificationChannels.ensureAll(context)
            DailyDigestScheduler.rescheduleAll(context)
            android.util.Log.d("NotifBootstrap", "init done")
        } catch (e: Exception) {
            android.util.Log.e("NotifBootstrap", "init failed", e)
        }
    }
}
