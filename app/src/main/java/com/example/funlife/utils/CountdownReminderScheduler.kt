// ═════════════════════════════════════════════════════════════════════════
// CountdownReminderScheduler.kt
// 倒数日到点提醒：AlarmManager + 独立通知渠道
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.funlife.MainActivity
import com.example.funlife.R

object CountdownReminderScheduler {

    const val CHANNEL_ID = "goal_countdown_reminder"
    private const val EXTRA_TITLE = "countdown_title"
    private const val EXTRA_NOTE = "countdown_note"
    private const val EXTRA_ID = "countdown_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, "倒数日提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "倒数日到达时通知"
                    enableLights(true)
                    enableVibration(true)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun schedule(context: Context, countdownId: Int, title: String, note: String, triggerAtMs: Long) {
        ensureChannel(context)
        val app = context.applicationContext
        val intent = Intent(app, CountdownReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, countdownId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_NOTE, note)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(app, 80000 + countdownId, intent, flags)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    fun cancel(context: Context, countdownId: Int) {
        val app = context.applicationContext
        val intent = Intent(app, CountdownReminderReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getBroadcast(app, 80000 + countdownId, intent, flags)
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
    }

    internal fun fireNotification(context: Context, countdownId: Int, title: String, note: String) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        val contentIntent = PendingIntent.getActivity(context, 90000 + countdownId, openIntent, piFlags)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏳ $title")
            .setContentText(if (note.isBlank()) "你设置的倒数日到啦！" else note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (note.isBlank()) "你设置的倒数日到啦！" else note))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(90000 + countdownId, n)
        } catch (_: SecurityException) { /* 缺少 POST_NOTIFICATIONS 权限时忽略 */ }
        // 写入应用内收件箱
        runCatching {
            com.example.funlife.notifications.InboxStore.add(
                context,
                com.example.funlife.notifications.FunChannel.COUNTDOWN,
                "⏳ $title",
                if (note.isBlank()) "你设置的倒数日到啦！" else note,
                "goal"
            )
        }
    }
}

class CountdownReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("countdown_id", -1)
        if (id == -1) return
        val title = intent.getStringExtra("countdown_title") ?: "倒数日"
        val note = intent.getStringExtra("countdown_note") ?: ""
        CountdownReminderScheduler.fireNotification(context, id, title, note)
    }
}
