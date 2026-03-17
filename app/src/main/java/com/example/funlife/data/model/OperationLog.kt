// OperationLog.kt - 操作日志数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_logs")
data class OperationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 0,  // 用户ID（暂时为0，多用户功能完成后使用）
    val operation: String,  // 操作类型：login, logout, spin, purchase, checkin 等
    val details: String,    // 操作详情（JSON格式）
    val result: String,     // 操作结果：success, failed, error
    val errorMessage: String = "",  // 错误信息（如果有）
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        // 操作类型常量
        const val OP_LOGIN = "login"
        const val OP_LOGOUT = "logout"
        const val OP_REGISTER = "register"
        const val OP_SPIN = "spin"
        const val OP_MULTI_SPIN = "multi_spin"
        const val OP_PURCHASE = "purchase"
        const val OP_CHECKIN = "checkin"
        const val OP_MAKEUP = "makeup"
        const val OP_ADD_COINS = "add_coins"
        const val OP_SPEND_COINS = "spend_coins"
        const val OP_CREATE_HABIT = "create_habit"
        const val OP_DELETE_HABIT = "delete_habit"
        const val OP_CREATE_GOAL = "create_goal"
        const val OP_DELETE_GOAL = "delete_goal"
        
        // 结果常量
        const val RESULT_SUCCESS = "success"
        const val RESULT_FAILED = "failed"
        const val RESULT_ERROR = "error"
    }
    
    // 获取格式化的时间
    fun getFormattedTime(): String {
        return try {
            val dateTime = java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            )
            dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }
}
