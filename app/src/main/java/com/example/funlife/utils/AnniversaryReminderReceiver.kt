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
        if (anniversaryId == -1) return
        
        android.util.Log.d("AnniversaryReceiver", "纪念日闹钟触发 #$anniversaryId")
        
        // 标记已触发
        AnniversaryReminderManager.markTriggered(anniversaryId)
        
        // 触发完整的提醒：循环震动 + 循环铃声 + Heads-up + App内Banner + 全局悬浮窗
        AnniversaryReminderManager.triggerAlarm(context)
        
        // 如果是每年重复型，自动调度明年的同一天
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).anniversaryDao()
                val list = dao.getAllForUserOnce(intent.getLongExtra("user_id", 1L))
                val target = list.firstOrNull { it.id == anniversaryId }
                if (target != null && target.isYearly) {
                    AnniversaryReminderScheduler.schedule(context, target)
                }
            } catch (e: Exception) {
                android.util.Log.e("AnniversaryReceiver", "重新调度失败", e)
            }
        }
    }
}
