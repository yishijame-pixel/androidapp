// AnniversaryReminder.kt - 纪念日提醒数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anniversary_reminders")
data class AnniversaryReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val anniversaryId: Int,  // 关联的纪念日ID
    val userId: Long,        // 用户ID
    val daysBeforeList: String,  // 提前提醒天数列表（逗号分隔）
    val reminderTime: String,    // 提醒时间（HH:mm格式）
    val isEnabled: Boolean,      // 是否启用提醒
    val notifyOnDay: Boolean     // 当天是否提醒
) {
    // 获取提前天数列表（解析为Int列表）
    fun parseDaysBeforeList(): List<Int> {
        return daysBeforeList.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
    }
    
    // 设置提前天数列表
    fun withDaysBeforeList(days: List<Int>): AnniversaryReminder {
        return copy(daysBeforeList = days.joinToString(","))
    }
}
