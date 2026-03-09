// NotificationHelper.kt - 通知帮助类
package com.example.funlife.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.funlife.MainActivity
import com.example.funlife.R

class NotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "anniversary_reminders"
        private const val CHANNEL_NAME = "纪念日提醒"
        private const val CHANNEL_DESCRIPTION = "纪念日到期提醒通知"
    }
    
    init {
        createNotificationChannel()
    }
    
    // 创建通知渠道
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // 发送纪念日提醒通知
    fun sendAnniversaryReminder(anniversaryName: String, daysRemaining: Long, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = when {
            daysRemaining == 0L -> "今天是 $anniversaryName"
            daysRemaining == 1L -> "明天是 $anniversaryName"
            else -> "$anniversaryName 即将到来"
        }
        
        val content = when {
            daysRemaining == 0L -> "就是今天！"
            daysRemaining == 1L -> "还有 1 天"
            else -> "还有 $daysRemaining 天"
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
    
    // 取消通知
    fun cancelNotification(notificationId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId)
        }
    }
    
    // 取消所有通知
    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}
