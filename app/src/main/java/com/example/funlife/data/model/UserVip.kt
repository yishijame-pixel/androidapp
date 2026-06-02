// UserVip.kt - 用户VIP信息
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "user_vip")
data class UserVip(
    @PrimaryKey
    val userId: Long,
    val vipLevel: Int = 0,  // 0=普通, 1-3=VIP等级, 99=永久VIP
    val expireDate: String? = null,  // VIP过期日期，null表示永久
    val lastDailyClaimDate: String? = null,  // 上次领取每日金币的日期
    val totalDaysActive: Int = 0,  // VIP累计激活天数
    val signature: String? = null  // 防篡改签名
) {
    fun isVip(): Boolean = vipLevel > 0

    /**
     * 是否永久 VIP —— 严格按等级判定（vipLevel=3 终身 / 99 旧体系永久），
     * **不再以 expireDate==null 兜底**，避免月卡(VIP1) 因脏数据/降级回写为 null
     * 而被误识别为永久。
     */
    fun isPermanent(): Boolean = vipLevel == 99 || vipLevel == 3

    fun isExpired(): Boolean {
        if (!isVip()) return false
        if (isPermanent()) return false
        if (expireDate == null) return true   // VIP1/VIP2 必须有有效期，缺失视为已过期
        
        return try {
            val expire = LocalDate.parse(expireDate)
            LocalDate.now().isAfter(expire)
        } catch (e: Exception) {
            true
        }
    }
    
    fun canClaimDailyCoins(): Boolean {
        if (!isVip() || isExpired()) return false
        
        val today = LocalDate.now().toString()
        return lastDailyClaimDate != today
    }
    
    fun getCurrentVipLevel(): VipLevel {
        return if (isExpired()) {
            VipLevel.NORMAL
        } else {
            VipLevel.fromLevel(vipLevel)
        }
    }
    
    fun getRemainingDays(): Int? {
        if (isPermanent()) return null
        if (expireDate == null) return 0
        
        return try {
            val expire = LocalDate.parse(expireDate)
            val today = LocalDate.now()
            if (expire.isBefore(today)) 0
            else java.time.temporal.ChronoUnit.DAYS.between(today, expire).toInt()
        } catch (e: Exception) {
            0
        }
    }
}
