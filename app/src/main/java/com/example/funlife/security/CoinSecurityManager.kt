// CoinSecurityManager.kt - 金币安全管理器
package com.example.funlife.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 金币安全管理器
 * 
 * 特性：
 * - 金币操作日志加密存储
 * - 异常交易检测
 * - 防刷金币机制
 * - 交易签名验证
 */
class CoinSecurityManager(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "secure_coin_operations",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("secure_coin_operations_fallback", Context.MODE_PRIVATE)
        }
    }
    
    companion object {
        private const val KEY_LAST_OPERATION_TIME = "last_operation_time_"
        private const val KEY_OPERATION_COUNT = "operation_count_"
        private const val KEY_TOTAL_EARNED = "total_earned_"
        private const val KEY_TOTAL_SPENT = "total_spent_"
        private const val KEY_LAST_BALANCE = "last_balance_"
        
        // 防刷金币：每分钟最多操作次数
        private const val MAX_OPERATIONS_PER_MINUTE = 30
        private const val MINUTE_IN_MILLIS = 60 * 1000L
        
        // 异常检测阈值
        private const val SUSPICIOUS_AMOUNT_THRESHOLD = 10000  // 单次超过10000金币视为可疑
        private const val SUSPICIOUS_FREQUENCY_THRESHOLD = 50  // 每分钟超过50次操作视为可疑
    }
    
    /**
     * 验证金币操作是否合法
     */
    fun validateCoinOperation(
        userId: Long,
        amount: Int,
        operationType: CoinOperationType
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            // 1. 金额验证
            if (amount <= 0) {
                errors.add("金额必须大于0")
            }
            
            if (amount > SUSPICIOUS_AMOUNT_THRESHOLD) {
                errors.add("单次操作金额过大（可疑）")
            }
            
            // 2. 频率检测
            val now = System.currentTimeMillis()
            val lastOperationTime = prefs.getLong(KEY_LAST_OPERATION_TIME + userId, 0)
            val operationCount = prefs.getInt(KEY_OPERATION_COUNT + userId, 0)
            
            if (now - lastOperationTime < MINUTE_IN_MILLIS) {
                if (operationCount >= MAX_OPERATIONS_PER_MINUTE) {
                    errors.add("操作过于频繁，请稍后再试")
                }
            }
            
            // 3. 设备指纹验证
            val deviceFingerprint = SecurityManager.getDeviceFingerprint(context)
            val storedFingerprint = prefs.getString("device_fingerprint_$userId", null)
            
            if (storedFingerprint != null && storedFingerprint != deviceFingerprint) {
                errors.add("设备验证失败")
            }
            
        } catch (e: Exception) {
            errors.add("验证过程出错: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * 记录金币操作
     */
    fun recordCoinOperation(
        userId: Long,
        amount: Int,
        operationType: CoinOperationType,
        currentBalance: Int
    ) {
        try {
            val now = System.currentTimeMillis()
            val lastOperationTime = prefs.getLong(KEY_LAST_OPERATION_TIME + userId, 0)
            
            // 重置计数器（如果超过1分钟）
            val operationCount = if (now - lastOperationTime >= MINUTE_IN_MILLIS) {
                1
            } else {
                prefs.getInt(KEY_OPERATION_COUNT + userId, 0) + 1
            }
            
            // 更新统计
            val totalEarned = prefs.getInt(KEY_TOTAL_EARNED + userId, 0)
            val totalSpent = prefs.getInt(KEY_TOTAL_SPENT + userId, 0)
            
            prefs.edit().apply {
                putLong(KEY_LAST_OPERATION_TIME + userId, now)
                putInt(KEY_OPERATION_COUNT + userId, operationCount)
                putInt(KEY_LAST_BALANCE + userId, currentBalance)
                
                when (operationType) {
                    CoinOperationType.EARN -> {
                        putInt(KEY_TOTAL_EARNED + userId, totalEarned + amount)
                    }
                    CoinOperationType.SPEND -> {
                        putInt(KEY_TOTAL_SPENT + userId, totalSpent + amount)
                    }
                }
                
                // 存储设备指纹
                val deviceFingerprint = SecurityManager.getDeviceFingerprint(context)
                putString("device_fingerprint_$userId", deviceFingerprint)
                
                apply()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检测异常交易
     */
    fun detectAnomalies(userId: Long): List<String> {
        val anomalies = mutableListOf<String>()
        
        try {
            val now = System.currentTimeMillis()
            val lastOperationTime = prefs.getLong(KEY_LAST_OPERATION_TIME + userId, 0)
            val operationCount = prefs.getInt(KEY_OPERATION_COUNT + userId, 0)
            
            // 1. 检查操作频率
            if (now - lastOperationTime < MINUTE_IN_MILLIS) {
                if (operationCount > SUSPICIOUS_FREQUENCY_THRESHOLD) {
                    anomalies.add("操作频率异常（每分钟${operationCount}次）")
                }
            }
            
            // 2. 检查设备指纹
            val deviceFingerprint = SecurityManager.getDeviceFingerprint(context)
            val storedFingerprint = prefs.getString("device_fingerprint_$userId", null)
            
            if (storedFingerprint != null && storedFingerprint != deviceFingerprint) {
                anomalies.add("设备指纹不匹配")
            }
            
            // 3. 检查余额异常变化
            val lastBalance = prefs.getInt(KEY_LAST_BALANCE + userId, 0)
            if (lastBalance < 0) {
                anomalies.add("余额出现负数")
            }
            
        } catch (e: Exception) {
            anomalies.add("异常检测出错: ${e.message}")
        }
        
        return anomalies
    }
    
    /**
     * 生成交易签名
     */
    fun generateTransactionSignature(
        userId: Long,
        amount: Int,
        operationType: CoinOperationType,
        timestamp: Long
    ): String {
        val deviceFingerprint = SecurityManager.getDeviceFingerprint(context)
        val data = "$userId:$amount:${operationType.name}:$timestamp:$deviceFingerprint"
        return SecurityManager.sha512Hash(data)
    }
    
    /**
     * 验证交易签名
     */
    fun verifyTransactionSignature(
        userId: Long,
        amount: Int,
        operationType: CoinOperationType,
        timestamp: Long,
        signature: String
    ): Boolean {
        val expectedSignature = generateTransactionSignature(userId, amount, operationType, timestamp)
        return signature == expectedSignature
    }
    
    /**
     * 获取用户金币统计
     */
    fun getCoinStatistics(userId: Long): CoinStatistics {
        return CoinStatistics(
            totalEarned = prefs.getInt(KEY_TOTAL_EARNED + userId, 0),
            totalSpent = prefs.getInt(KEY_TOTAL_SPENT + userId, 0),
            lastBalance = prefs.getInt(KEY_LAST_BALANCE + userId, 0),
            operationCount = prefs.getInt(KEY_OPERATION_COUNT + userId, 0),
            lastOperationTime = prefs.getLong(KEY_LAST_OPERATION_TIME + userId, 0)
        )
    }
    
    /**
     * 清除用户数据
     */
    fun clearUserData(userId: Long) {
        prefs.edit().apply {
            remove(KEY_LAST_OPERATION_TIME + userId)
            remove(KEY_OPERATION_COUNT + userId)
            remove(KEY_TOTAL_EARNED + userId)
            remove(KEY_TOTAL_SPENT + userId)
            remove(KEY_LAST_BALANCE + userId)
            remove("device_fingerprint_$userId")
            apply()
        }
    }
}

/**
 * 金币操作类型
 */
enum class CoinOperationType {
    EARN,   // 获得金币
    SPEND   // 消费金币
}

/**
 * 金币统计
 */
data class CoinStatistics(
    val totalEarned: Int,
    val totalSpent: Int,
    val lastBalance: Int,
    val operationCount: Int,
    val lastOperationTime: Long
)
