// AuditLogger.kt - 审计日志工具类
package com.example.funlife.utils

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.OperationLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object AuditLogger {
    
    private var logDao: com.example.funlife.data.dao.OperationLogDao? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun initialize(context: Context) {
        val database = AppDatabase.getDatabase(context)
        logDao = database.operationLogDao()
    }
    
    // 记录登录
    fun logLogin(userId: Long, username: String, success: Boolean, errorMessage: String = "") {
        val details = JSONObject().apply {
            put("username", username)
            put("userId", userId)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_LOGIN,
            details = details,
            result = if (success) OperationLog.RESULT_SUCCESS else OperationLog.RESULT_FAILED,
            errorMessage = errorMessage
        )
    }
    
    // 记录登出
    fun logLogout(userId: Long, username: String) {
        val details = JSONObject().apply {
            put("username", username)
            put("userId", userId)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_LOGOUT,
            details = details,
            result = OperationLog.RESULT_SUCCESS
        )
    }
    
    // 记录注册
    fun logRegister(userId: Long, username: String, success: Boolean, errorMessage: String = "") {
        val details = JSONObject().apply {
            put("username", username)
            put("userId", userId)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_REGISTER,
            details = details,
            result = if (success) OperationLog.RESULT_SUCCESS else OperationLog.RESULT_FAILED,
            errorMessage = errorMessage
        )
    }
    
    // 记录转盘
    fun logSpin(userId: Long, result: String, mode: String, coinCost: Int, coinReward: Int) {
        val details = JSONObject().apply {
            put("result", result)
            put("mode", mode)
            put("coinCost", coinCost)
            put("coinReward", coinReward)
            put("netProfit", coinReward - coinCost)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_SPIN,
            details = details,
            result = OperationLog.RESULT_SUCCESS
        )
    }
    
    // 记录连抽
    fun logMultiSpin(userId: Long, count: Int, results: List<String>, mode: String, totalCost: Int, totalReward: Int) {
        val details = JSONObject().apply {
            put("count", count)
            put("results", results.joinToString(","))
            put("mode", mode)
            put("totalCost", totalCost)
            put("totalReward", totalReward)
            put("netProfit", totalReward - totalCost)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_MULTI_SPIN,
            details = details,
            result = OperationLog.RESULT_SUCCESS
        )
    }
    
    // 记录购买
    fun logPurchase(userId: Long, itemName: String, price: Int, success: Boolean, errorMessage: String = "") {
        val details = JSONObject().apply {
            put("itemName", itemName)
            put("price", price)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_PURCHASE,
            details = details,
            result = if (success) OperationLog.RESULT_SUCCESS else OperationLog.RESULT_FAILED,
            errorMessage = errorMessage
        )
    }
    
    // 记录打卡
    fun logCheckin(userId: Long, habitName: String, date: String, coinsEarned: Int) {
        val details = JSONObject().apply {
            put("habitName", habitName)
            put("date", date)
            put("coinsEarned", coinsEarned)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_CHECKIN,
            details = details,
            result = OperationLog.RESULT_SUCCESS
        )
    }
    
    // 记录补卡
    fun logMakeup(userId: Long, habitName: String, date: String, success: Boolean, errorMessage: String = "") {
        val details = JSONObject().apply {
            put("habitName", habitName)
            put("date", date)
        }.toString()
        
        log(
            userId = userId,
            operation = OperationLog.OP_MAKEUP,
            details = details,
            result = if (success) OperationLog.RESULT_SUCCESS else OperationLog.RESULT_FAILED,
            errorMessage = errorMessage
        )
    }
    
    // 记录金币变动
    fun logCoinChange(userId: Long, amount: Int, reason: String, isAdd: Boolean) {
        val details = JSONObject().apply {
            put("amount", amount)
            put("reason", reason)
            put("type", if (isAdd) "add" else "spend")
        }.toString()
        
        log(
            userId = userId,
            operation = if (isAdd) OperationLog.OP_ADD_COINS else OperationLog.OP_SPEND_COINS,
            details = details,
            result = OperationLog.RESULT_SUCCESS
        )
    }
    
    // 通用日志记录方法
    private fun log(
        userId: Long,
        operation: String,
        details: String,
        result: String,
        errorMessage: String = ""
    ) {
        scope.launch {
            try {
                val log = OperationLog(
                    userId = userId,
                    operation = operation,
                    details = details,
                    result = result,
                    errorMessage = errorMessage
                )
                logDao?.insert(log)
                
                // 定期清理旧日志
                logDao?.cleanOldLogs()
            } catch (e: Exception) {
                android.util.Log.e("AuditLogger", "Failed to log operation", e)
            }
        }
    }
}
