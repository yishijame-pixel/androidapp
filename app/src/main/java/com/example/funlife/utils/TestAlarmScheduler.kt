package com.example.funlife.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * 🧪 仅供 Debug 测试：N 秒后触发一次纪念日通知
 *
 * 用 setAlarmClock() 注册，确保 App 被杀也能触发，方便验证后台通知功能。
 * 测试完成后可整体删除本文件以及 AnniversaryScreen 中调用它的 Debug 按钮。
 */
object TestAlarmScheduler {

    private const val TAG = "TestAlarmScheduler"
    private const val TEST_REQUEST_CODE = 99999

    /**
     * @param context 上下文
     * @param seconds 几秒后触发
     */
    fun scheduleIn(context: Context, seconds: Int) {
        try {
            val triggerMillis = System.currentTimeMillis() + seconds * 1000L
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AnniversaryReminderReceiver::class.java).apply {
                putExtra("anniversary_id", -999)        // 测试用特殊 ID
                putExtra("anniversary_name", "🧪 测试通知")
                putExtra("days_before", 0)
            }
            val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                context,
                TEST_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
            )

            // 用 setAlarmClock — 最高优先级，App 被杀/清后台都能触发
            try {
                val showIntent = Intent(context, com.example.funlife.MainActivity::class.java)
                val showPi = PendingIntent.getActivity(
                    context,
                    TEST_REQUEST_CODE + 1,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
                )
                val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
                alarmManager.setAlarmClock(info, pi)
                android.util.Log.d(TAG, "✅ setAlarmClock 测试闹钟 已注册，${seconds}秒后触发")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "setAlarmClock 失败，降级 setExactAndAllowWhileIdle", e)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerMillis, pi
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "调度测试闹钟失败", e)
        }
    }
}
