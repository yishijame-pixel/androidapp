// AnniversaryReminderReceiver.kt - 纪念日提醒广播接收器
package com.example.funlife.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnniversaryReminderReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val anniversaryId = intent.getIntExtra("anniversary_id", -1)
        val daysBefore = intent.getIntExtra("days_before", 0)
        
        if (anniversaryId == -1) return
        
        // 在协程中查询纪念日信息并显示通知
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val anniversaryDao = database.anniversaryDao()
                
                // 这里需要一个同步查询方法，暂时使用简单的通知
                val notificationManager = AnniversaryNotificationManager(context)
                
                // 创建临时 Anniversary 对象用于通知
                val anniversaryName = intent.getStringExtra("anniversary_name") ?: "纪念日"
                val tempAnniversary = Anniversary(
                    id = anniversaryId,
                    name = anniversaryName,
                    date = "",
                    userId = 0
                )
                
                notificationManager.showNotification(
                    tempAnniversary,
                    daysBefore.toLong(),
                    anniversaryId
                )
            } catch (e: Exception) {
                android.util.Log.e("AnniversaryReminder", "Error showing notification", e)
            }
        }
    }
}
