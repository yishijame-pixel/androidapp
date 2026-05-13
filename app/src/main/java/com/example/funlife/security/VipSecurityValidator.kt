// VipSecurityValidator.kt - VIP状态安全验证器
package com.example.funlife.security

import android.content.Context
import com.example.funlife.data.model.UserVip
import java.time.LocalDate

/**
 * VIP状态安全验证器
 * 
 * 特性：
 * - VIP状态完整性检查
 * - 防篡改签名验证
 * - 过期状态验证
 * - 异常检测
 */
class VipSecurityValidator(private val context: Context) {
    
    /**
     * 验证VIP状态完整性
     */
    fun validateVipStatus(userVip: UserVip): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            // 1. 基本字段验证
            if (userVip.userId <= 0) {
                errors.add("无效的用户ID")
            }
            
            if (userVip.vipLevel < 0 || userVip.vipLevel > 99) {
                errors.add("无效的VIP等级")
            }
            
            // 2. 过期日期验证
            if (userVip.expireDate != null) {
                try {
                    val expireDate = LocalDate.parse(userVip.expireDate)
                    val now = LocalDate.now()
                    
                    // 检查日期是否合理（不能太远的未来）
                    if (expireDate.isAfter(now.plusYears(10))) {
                        errors.add("过期日期异常（超过10年）")
                    }
                } catch (e: Exception) {
                    errors.add("过期日期格式错误")
                }
            }
            
            // 3. 签名验证（如果存在）
            if (userVip.signature != null) {
                val isValid = SecurityManager.verifyVipSignature(
                    userVip.userId,
                    userVip.vipLevel,
                    userVip.expireDate,
                    userVip.signature,
                    context
                )
                
                if (!isValid) {
                    errors.add("VIP状态签名验证失败（可能被篡改）")
                }
            }
            
            // 4. 逻辑一致性检查
            if (userVip.vipLevel == 99 && userVip.expireDate != null) {
                errors.add("永久VIP不应有过期日期")
            }
            
            if (userVip.vipLevel > 0 && userVip.vipLevel < 99 && userVip.expireDate == null) {
                errors.add("非永久VIP必须有过期日期")
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
     * 生成VIP状态签名
     */
    fun signVipStatus(userVip: UserVip): String {
        return SecurityManager.generateVipSignature(
            userVip.userId,
            userVip.vipLevel,
            userVip.expireDate,
            context
        )
    }
    
    /**
     * 检测VIP状态异常
     */
    fun detectVipAnomalies(userVip: UserVip): List<String> {
        val anomalies = mutableListOf<String>()
        
        try {
            // 1. 检查是否过期但仍标记为有效
            if (userVip.isExpired() && userVip.vipLevel > 0) {
                anomalies.add("VIP已过期但等级未重置")
            }
            
            // 2. 检查每日领取日期
            if (userVip.lastDailyClaimDate != null) {
                try {
                    val lastClaimDate = LocalDate.parse(userVip.lastDailyClaimDate)
                    val now = LocalDate.now()
                    
                    // 领取日期不能在未来
                    if (lastClaimDate.isAfter(now)) {
                        anomalies.add("每日领取日期在未来（可能被篡改）")
                    }
                    
                    // 领取日期不能太久远（超过1年）
                    if (lastClaimDate.isBefore(now.minusYears(1))) {
                        anomalies.add("每日领取日期过于久远")
                    }
                } catch (e: Exception) {
                    anomalies.add("每日领取日期格式错误")
                }
            }
            
            // 3. 检查VIP等级合理性
            if (userVip.vipLevel > 0 && userVip.expireDate == null && userVip.vipLevel != 99) {
                anomalies.add("非永久VIP缺少过期日期")
            }
            
        } catch (e: Exception) {
            anomalies.add("异常检测出错: ${e.message}")
        }
        
        return anomalies
    }
    
    /**
     * 验证VIP升级操作
     */
    fun validateVipUpgrade(
        currentVip: UserVip?,
        newVipLevel: Int,
        newExpireDate: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            // 1. 新等级验证
            if (newVipLevel < 0 || newVipLevel > 99) {
                errors.add("无效的VIP等级")
            }
            
            // 2. 降级检查（通常不允许降级）
            if (currentVip != null && !currentVip.isExpired()) {
                if (newVipLevel < currentVip.vipLevel && newVipLevel != 0) {
                    errors.add("不允许VIP降级")
                }
            }
            
            // 3. 过期日期验证
            if (newExpireDate != null) {
                try {
                    val expireDate = LocalDate.parse(newExpireDate)
                    val now = LocalDate.now()
                    
                    if (expireDate.isBefore(now)) {
                        errors.add("过期日期不能在过去")
                    }
                    
                    if (expireDate.isAfter(now.plusYears(10))) {
                        errors.add("过期日期不能超过10年")
                    }
                } catch (e: Exception) {
                    errors.add("过期日期格式错误")
                }
            }
            
            // 4. 永久VIP验证
            if (newVipLevel == 99 && newExpireDate != null) {
                errors.add("永久VIP不应有过期日期")
            }
            
        } catch (e: Exception) {
            errors.add("验证过程出错: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
) {
    fun getErrorMessage(): String {
        return errors.joinToString("\n")
    }
}
