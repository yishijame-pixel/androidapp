package com.example.funlife.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机自启 / App 更新后重新调度纪念日闹钟
 * 因为 AlarmManager 的闹钟在重启后会丢失
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                android.util.Log.d("BootReceiver", "重启/更新检测，重新调度纪念日提醒")
                try {
                    val sessionManager = UserSessionManager(context)
                    val userId = sessionManager.getCurrentUserId()
                    if (userId > 0L) {
                        AnniversaryReminderScheduler.scheduleAllForUser(context, userId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "重新调度失败", e)
                }
            }
        }
    }
}
