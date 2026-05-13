// RedeemCode.kt - 兑换码模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "redeem_codes")
data class RedeemCode(
    @PrimaryKey
    val code: String,
    val type: String,  // "VIP", "COINS", "ITEM"
    val value: String,  // VIP等级或金币数量或物品ID
    val maxUses: Int = -1,  // -1表示无限次使用
    val currentUses: Int = 0,
    val expiryDate: String? = null,  // 兑换码过期日期
    val isActive: Boolean = true
) {
    fun isExpired(): Boolean {
        if (expiryDate == null) return false
        
        return try {
            val expire = java.time.LocalDate.parse(expiryDate)
            java.time.LocalDate.now().isAfter(expire)
        } catch (e: Exception) {
            true
        }
    }
    
    fun canUse(): Boolean {
        if (!isActive || isExpired()) return false
        if (maxUses == -1) return true
        return currentUses < maxUses
    }
}

@Entity(tableName = "user_redeem_history")
data class UserRedeemHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val code: String,
    val redeemDate: String,
    val reward: String
)
