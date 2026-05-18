package com.example.funlife.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 用户滑掉通知 / 点击通知动作 → 停止铃声 + 震动
 */
class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("StopAlarmReceiver", "用户滑掉通知，停止铃声/震动")
        AnniversaryReminderManager.stopAlarm(context)
        AnniversaryReminderManager.dismissInAppBanner()
        OverlayBannerService.stop(context)
    }
}
