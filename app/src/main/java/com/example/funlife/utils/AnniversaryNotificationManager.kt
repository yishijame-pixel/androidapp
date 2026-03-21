// AnniversaryNotificationManager.kt - 纪念日通知管理器
package com.example.funlife.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.funlife.MainActivity
import com.example.funlife.R
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.AnniversaryReminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

class AnniversaryNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "anniversary_reminders"
        const val CHANNEL_NAME = "纪念日提醒"
        const val CHANNEL_DESCRIPTION = "纪念日到期提醒通知"
    }
    
    init {
        createNotificationChannel()
    }
    
    // 创建通知渠道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // 检查通知权限
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    // 显示通知
    fun showNotification(
        anniversary: Anniversary,
        daysRemaining: Long,
        notificationId: Int = anniversary.id
    ) {
        if (!hasNotificationPermission()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "anniversary")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val title = when {
            daysRemaining == 0L -> "🎉 ${anniversary.name} 就是今天！"
            daysRemaining == 1L -> "⏰ ${anniversary.name} 明天到了"
            else -> "📅 ${anniversary.name} 还有 $daysRemaining 天"
        }
        
        val content = buildString {
            append(anniversary.getFormattedDate())
            if (anniversary.isYearly) {
                val years = anniversary.getYearsPassed()
                if (years > 0) {
                    append(" • 已经 $years 年了")
                }
            }
            if (!anniversary.note.isNullOrEmpty()) {
                append("\n${anniversary.note}")
            }
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
    
    // 安排提醒
    fun scheduleReminder(
        anniversary: Anniversary,
        reminder: AnniversaryReminder
    ) {
        if (!reminder.isEnabled) return
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val daysRemaining = anniversary.getDaysRemaining()
        
        // 解析提醒时间
        val timeParts = reminder.reminderTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
        
        // 为每个提前天数设置提醒
        reminder.parseDaysBeforeList().forEach { daysBefore ->
            if (daysRemaining >= daysBefore) {
                val reminderDate = LocalDate.now().plusDays(daysRemaining - daysBefore)
                val reminderDateTime = LocalDateTime.of(reminderDate, LocalTime.of(hour, minute))
                val triggerTime = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                if (triggerTime > System.currentTimeMillis()) {
                    scheduleAlarm(alarmManager, anniversary, triggerTime, daysBefore)
                }
            }
        }
        
        // 当天提醒
        if (reminder.notifyOnDay && daysRemaining >= 0) {
            val reminderDate = LocalDate.now().plusDays(daysRemaining)
            val reminderDateTime = LocalDateTime.of(reminderDate, LocalTime.of(hour, minute))
            val triggerTime = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            if (triggerTime > System.currentTimeMillis()) {
                scheduleAlarm(alarmManager, anniversary, triggerTime, 0)
            }
        }
    }
    
    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        anniversary: Anniversary,
        triggerTime: Long,
        daysBefore: Int
    ) {
        val intent = Intent(context, AnniversaryReminderReceiver::class.java).apply {
            putExtra("anniversary_id", anniversary.id)
            putExtra("anniversary_name", anniversary.name)
            putExtra("days_before", daysBefore)
        }
        
        val requestCode = anniversary.id * 100 + daysBefore
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
    
    // 取消提醒
    fun cancelReminder(anniversaryId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 取消所有相关的提醒（0-30天）
        for (daysBefore in 0..30) {
            val intent = Intent(context, AnniversaryReminderReceiver::class.java)
            val requestCode = anniversaryId * 100 + daysBefore
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}
