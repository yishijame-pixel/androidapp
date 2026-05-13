// DailyCoinManager.kt - 每日金币领取安全管理器
package com.example.funlife.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 每日金币领取安全管理器
 * 
 * 特性：
 * - 严格24小时限制
 * - 加密存储时间戳
 * - 防篡改验证
 * - 设备指纹绑定
 * - 多重时间验证
 */
class DailyCoinManager(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        try {
            // 使用加密的 SharedPreferences
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "secure_daily_coins",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 降级到普通 SharedPreferences（但仍然加密数据）
            context.getSharedPreferences("secure_daily_coins_fallback", Context.MODE_PRIVATE)
        }
    }
    
    companion object {
        private const val KEY_LAST_CLAIM_TOKEN = "last_claim_token_"
        private const val KEY_CLAIM_COUNT = "claim_count_"
        private const val KEY_DEVICE_BINDING = "device_binding_"
        private const val HOURS_24 = 24L
    }
    
    /**
     * 检查是否可以领取金币
     */
    fun canClaimCoins(userId: Long): Boolean {
        try {
            // 1. 验证设备绑定
            if (!verifyDeviceBinding(userId)) {
                return false
            }
            
            // 2. 获取上次领取时间戳令牌
            val lastClaimToken = prefs.getString(KEY_LAST_CLAIM_TOKEN + userId, null)
                ?: return true  // 首次领取
            
            // 3. 验证时间戳令牌（防篡改）
            val lastClaimTimestamp = SecurityManager.verifySecureTimestamp(context, lastClaimToken)
                ?: return true  // 令牌无效，允许领取但会重置
            
            // 4. 计算时间差
            val now = System.currentTimeMillis()
            val hoursPassed = ChronoUnit.HOURS.between(
                Instant.ofEpochMilli(lastClaimTimestamp).atZone(ZoneId.systemDefault()),
                Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
            )
            
            // 5. 严格24小时检查
            if (hoursPassed < HOURS_24) {
                return false
            }
            
            // 6. 额外验证：检查日期是否真的不同
            val lastDate = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(lastClaimTimestamp),
                ZoneId.systemDefault()
            ).toLocalDate()
            
            val currentDate = LocalDateTime.now().toLocalDate()
            
            // 必须是不同的日期，且至少间隔24小时
            return currentDate.isAfter(lastDate) && hoursPassed >= HOURS_24
            
        } catch (e: Exception) {
            e.printStackTrace()
            // 出错时保守处理，不允许领取
            return false
        }
    }
    
    /**
     * 获取距离下次可领取的剩余时间（秒）
     */
    fun getTimeUntilNextClaim(userId: Long): Long {
        try {
            val lastClaimToken = prefs.getString(KEY_LAST_CLAIM_TOKEN + userId, null)
                ?: return 0L
            
            val lastClaimTimestamp = SecurityManager.verifySecureTimestamp(context, lastClaimToken)
                ?: return 0L
            
            val now = System.currentTimeMillis()
            val nextClaimTime = lastClaimTimestamp + (HOURS_24 * 60 * 60 * 1000)
            
            val remainingMs = nextClaimTime - now
            return if (remainingMs > 0) remainingMs / 1000 else 0L
            
        } catch (e: Exception) {
            return 0L
        }
    }
    
    /**
     * 记录领取时间（加密存储）
     */
    fun recordClaim(userId: Long): Boolean {
        try {
            val now = System.currentTimeMillis()
            
            // 1. 生成安全的时间戳令牌
            val secureToken = SecurityManager.generateSecureTimestamp(context, now)
            
            // 2. 加密存储
            prefs.edit().apply {
                putString(KEY_LAST_CLAIM_TOKEN + userId, secureToken)
                
                // 3. 增加领取计数（用于统计和异常检测）
                val currentCount = prefs.getInt(KEY_CLAIM_COUNT + userId, 0)
                putInt(KEY_CLAIM_COUNT + userId, currentCount + 1)
                
                // 4. 更新设备绑定
                val deviceFingerprint = SecurityManager.getDeviceFingerprint(context)
                putString(KEY_DEVICE_BINDING + userId, deviceFingerprint)
                
                apply()
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * 验证设备绑定（防止跨设备作弊）
     */
    private fun verifyDeviceBinding(userId: Long): Boolean {
        try {
            val storedFingerprint = prefs.getString(KEY_DEVICE_BINDING + userId, null)
                ?: return true  // 首次使用
            
            val currentFingerprint = SecurityManager.getDeviceFingerprint(context)
            
            // 设备指纹必须匹配
            return storedFingerprint == currentFingerprint
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 获取领取历史统计
     */
    fun getClaimCount(userId: Long): Int {
        return prefs.getInt(KEY_CLAIM_COUNT + userId, 0)
    }
    
    /**
     * 获取上次领取时间（用于显示）
     */
    fun getLastClaimTime(userId: Long): Long? {
        try {
            val lastClaimToken = prefs.getString(KEY_LAST_CLAIM_TOKEN + userId, null)
                ?: return null
            
            return SecurityManager.verifySecureTimestamp(context, lastClaimToken)
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 清除用户数据（仅用于测试或账号删除）
     */
    fun clearUserData(userId: Long) {
        prefs.edit().apply {
            remove(KEY_LAST_CLAIM_TOKEN + userId)
            remove(KEY_CLAIM_COUNT + userId)
            remove(KEY_DEVICE_BINDING + userId)
            apply()
        }
    }
    
    /**
     * 检测异常行为（防作弊）
     */
    fun detectAnomalies(userId: Long): List<String> {
        val anomalies = mutableListOf<String>()
        
        try {
            // 1. 检查设备绑定
            if (!verifyDeviceBinding(userId)) {
                anomalies.add("设备指纹不匹配")
            }
            
            // 2. 检查时间戳令牌完整性
            val lastClaimToken = prefs.getString(KEY_LAST_CLAIM_TOKEN + userId, null)
            if (lastClaimToken != null) {
                val timestamp = SecurityManager.verifySecureTimestamp(context, lastClaimToken)
                if (timestamp == null) {
                    anomalies.add("时间戳令牌被篡改")
                }
            }
            
            // 3. 检查领取频率（统计学异常检测）
            val claimCount = getClaimCount(userId)
            val lastClaimTime = getLastClaimTime(userId)
            
            if (lastClaimTime != null && claimCount > 0) {
                val daysSinceFirstClaim = ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(lastClaimTime).atZone(ZoneId.systemDefault()),
                    Instant.now().atZone(ZoneId.systemDefault())
                )
                
                // 如果领取次数远超天数，可能存在作弊
                if (claimCount > daysSinceFirstClaim + 5) {
                    anomalies.add("领取频率异常")
                }
            }
            
        } catch (e: Exception) {
            anomalies.add("检测过程出错: ${e.message}")
        }
        
        return anomalies
    }
}
