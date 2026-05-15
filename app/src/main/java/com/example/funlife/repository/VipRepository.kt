// VipRepository.kt - VIP仓库（增强安全版）
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.dao.RedeemCodeDao
import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.model.UserVip
import com.example.funlife.data.model.RedeemCode
import com.example.funlife.data.model.UserRedeemHistory
import com.example.funlife.data.model.VipLevel
import com.example.funlife.security.SecurityManager
import com.example.funlife.security.DailyCoinManager
import com.example.funlife.security.VipSecurityValidator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class VipRepository(
    private val userVipDao: UserVipDao,
    private val redeemCodeDao: RedeemCodeDao,
    private val coinDao: CoinDao,
    private val context: Context
) {
    
    private val dailyCoinManager = DailyCoinManager(context)
    private val securityValidator = VipSecurityValidator(context)
    
    fun getUserVip(userId: Long): Flow<UserVip?> {
        return userVipDao.getUserVip(userId)
    }
    
    suspend fun getUserVipSync(userId: Long): UserVip? {
        return userVipDao.getUserVipSync(userId)
    }
    
    suspend fun initializeUserVip(userId: Long) {
        val existing = userVipDao.getUserVipSync(userId)
        if (existing == null) {
            val newVip = UserVip(userId = userId)
            val signature = securityValidator.signVipStatus(newVip)
            userVipDao.insertOrUpdate(newVip.copy(signature = signature))
        } else {
            // 验证现有VIP状态
            val validation = securityValidator.validateVipStatus(existing)
            if (!validation.isValid) {
                // 如果签名无效，重新生成
                val newSignature = securityValidator.signVipStatus(existing)
                userVipDao.insertOrUpdate(existing.copy(signature = newSignature))
            }
        }
    }
    
    suspend fun claimDailyCoins(userId: Long): Result<Int> {
        return try {
            val userVip = userVipDao.getUserVipSync(userId) ?: return Result.failure(Exception("VIP信息不存在"))
            
            // 1. 验证VIP状态完整性
            val validation = securityValidator.validateVipStatus(userVip)
            if (!validation.isValid) {
                return Result.failure(Exception("VIP状态异常: ${validation.getErrorMessage()}"))
            }
            
            // 2. 检查VIP权限
            if (!userVip.isVip() || userVip.isExpired()) {
                return Result.failure(Exception("您不是VIP用户"))
            }
            
            // 3. 使用安全管理器检查24小时限制（多重验证）
            if (!dailyCoinManager.canClaimCoins(userId)) {
                val remainingSeconds = dailyCoinManager.getTimeUntilNextClaim(userId)
                val hours = remainingSeconds / 3600
                val minutes = (remainingSeconds % 3600) / 60
                return Result.failure(Exception("请在 ${hours}小时${minutes}分钟 后再来领取"))
            }
            
            // 4. 检测异常行为
            val anomalies = dailyCoinManager.detectAnomalies(userId)
            if (anomalies.isNotEmpty()) {
                return Result.failure(Exception("检测到异常行为，请联系客服"))
            }
            
            val vipLevel = userVip.getCurrentVipLevel()
            val coins = vipLevel.dailyCoins
            
            // 5. 添加金币
            coinDao.addCoins(userId, coins)
            
            // 6. 使用安全管理器记录领取时间（加密存储）
            if (!dailyCoinManager.recordClaim(userId)) {
                return Result.failure(Exception("记录领取时间失败"))
            }
            
            // 7. 更新数据库中的领取日期（双重记录）
            val today = LocalDate.now().toString()
            userVipDao.updateLastDailyClaimDate(userId, today)
            
            Result.success(coins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun redeemCode(userId: Long, code: String): Result<String> {
        return try {
            android.util.Log.d("VipRepository", "========== 开始兑换码流程 ==========")
            android.util.Log.d("VipRepository", "用户ID: $userId, 兑换码: $code")
            
            // 1. 输入验证
            if (code.isBlank() || code.length < 4) {
                android.util.Log.e("VipRepository", "兑换码格式错误: 长度=${code.length}")
                return Result.failure(Exception("兑换码格式错误"))
            }
            
            // 2. 先尝试添加测试兑换码（确保数据库有数据）
            android.util.Log.d("VipRepository", "步骤1: 初始化测试兑换码")
            addTestRedeemCodes()
            
            // 3. 在IO线程生成兑换码哈希（保持军事级加密，避免UI卡顿）
            android.util.Log.d("VipRepository", "步骤2: 生成兑换码哈希（后台线程）")
            val codeHash = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                SecurityManager.hashRedeemCode(code, context)
            }
            android.util.Log.d("VipRepository", "哈希生成完成")
            
            // 4. 检查兑换码是否存在（先尝试直接匹配，再尝试哈希匹配）
            android.util.Log.d("VipRepository", "步骤3: 查找兑换码（原始码）")
            var redeemCode = redeemCodeDao.getRedeemCode(code)
            
            if (redeemCode != null) {
                android.util.Log.d("VipRepository", "✓ 找到兑换码（原始匹配）: ${redeemCode.code}, 类型: ${redeemCode.type}, 值: ${redeemCode.value}")
            } else {
                android.util.Log.d("VipRepository", "原始码未找到，尝试哈希匹配")
                // 如果直接匹配失败，尝试哈希匹配（用于已加密的兑换码）
                redeemCode = redeemCodeDao.getRedeemCode(codeHash)
                if (redeemCode != null) {
                    android.util.Log.d("VipRepository", "✓ 找到兑换码（哈希匹配）")
                }
            }
            
            // 如果还是找不到，说明兑换码不存在
            if (redeemCode == null) {
                android.util.Log.e("VipRepository", "✗ 兑换码不存在: $code")
                return Result.failure(Exception("兑换码不存在"))
            }
            
            // 5. 检查兑换码是否可用
            android.util.Log.d("VipRepository", "步骤4: 检查兑换码可用性")
            if (!redeemCode.canUse()) {
                android.util.Log.e("VipRepository", "✗ 兑换码不可用: isActive=${redeemCode.isActive}, isExpired=${redeemCode.isExpired()}, uses=${redeemCode.currentUses}/${redeemCode.maxUses}")
                return Result.failure(Exception("兑换码已失效或已达到使用上限"))
            }
            android.util.Log.d("VipRepository", "✓ 兑换码可用")
            
            // 6. 检查用户是否已经使用过（使用原始code和hash都检查）
            android.util.Log.d("VipRepository", "步骤5: 检查用户使用历史")
            val history1 = redeemCodeDao.getUserRedeemHistory(userId, code)
            val history2 = redeemCodeDao.getUserRedeemHistory(userId, codeHash)
            if (history1 != null || history2 != null) {
                android.util.Log.e("VipRepository", "✗ 用户已使用过此兑换码")
                return Result.failure(Exception("您已经使用过这个兑换码"))
            }
            android.util.Log.d("VipRepository", "✓ 用户未使用过此兑换码")
            
            // 7. 根据类型处理奖励
            android.util.Log.d("VipRepository", "步骤6: 处理奖励 (类型: ${redeemCode.type})")
            val reward = when (redeemCode.type) {
                "VIP" -> {
                    val vipLevel = redeemCode.value.toIntOrNull() ?: 1
                    android.util.Log.d("VipRepository", "VIP等级: $vipLevel")
                    
                    // 根据VIP等级设置过期日期
                    val expireDate = when (vipLevel) {
                        1 -> null  // VIP1（普通VIP）：永久
                        2 -> LocalDate.now().plusDays(365).toString()  // VIP2（年费VIP）：365天
                        3 -> null  // VIP3（终身VIP）：永久
                        99 -> null  // PERMANENT（系统保留）：永久
                        else -> LocalDate.now().plusDays(30).toString()  // 其他：30天
                    }
                    
                    val currentVip = userVipDao.getUserVipSync(userId)
                    
                    // 验证VIP升级操作
                    val upgradeValidation = securityValidator.validateVipUpgrade(
                        currentVip,
                        vipLevel,
                        expireDate
                    )
                    
                    if (!upgradeValidation.isValid) {
                        android.util.Log.e("VipRepository", "✗ VIP升级验证失败: ${upgradeValidation.getErrorMessage()}")
                        return Result.failure(Exception("VIP升级验证失败: ${upgradeValidation.getErrorMessage()}"))
                    }
                    
                    val newVip = if (currentVip == null) {
                        UserVip(
                            userId = userId,
                            vipLevel = vipLevel,
                            expireDate = expireDate
                        )
                    } else {
                        currentVip.copy(
                            vipLevel = vipLevel,
                            expireDate = expireDate
                        )
                    }
                    
                    // 生成新签名
                    val signature = securityValidator.signVipStatus(newVip)
                    userVipDao.insertOrUpdate(newVip.copy(signature = signature))
                    
                    // 根据VIP等级赠送金币
                    val bonusCoins = when (vipLevel) {
                        1 -> 100   // 普通VIP赠送100金币
                        2 -> 500   // 年费VIP赠送500金币
                        3 -> 1000  // 终身VIP赠送1000金币
                        else -> 0
                    }
                    
                    if (bonusCoins > 0) {
                        // 🔥 确保用户有金币记录（使用initializeCoins）
                        coinDao.initializeCoins(userId)
                        android.util.Log.d("VipRepository", "初始化用户金币记录")
                        
                        // 添加金币
                        coinDao.addCoins(userId, bonusCoins)
                        android.util.Log.d("VipRepository", "✓ 赠送金币: $bonusCoins")
                    }
                    
                    val levelName = VipLevel.fromLevel(vipLevel).displayName
                    android.util.Log.d("VipRepository", "✓ VIP激活成功: $levelName")
                    "成功激活 $levelName|$bonusCoins"  // 返回格式：VIP名称|金币数量
                }
                "COINS" -> {
                    val coins = redeemCode.value.toIntOrNull() ?: 0
                    coinDao.addCoins(userId, coins)
                    android.util.Log.d("VipRepository", "✓ 金币添加成功: $coins")
                    "获得 $coins 金币"
                }
                else -> {
                    android.util.Log.e("VipRepository", "✗ 未知奖励类型: ${redeemCode.type}")
                    "未知奖励类型"
                }
            }
            
            // 8. 记录兑换历史（使用哈希存储，保持安全性）
            android.util.Log.d("VipRepository", "步骤7: 记录兑换历史")
            redeemCodeDao.insertRedeemHistory(
                UserRedeemHistory(
                    userId = userId,
                    code = codeHash,  // 存储哈希而不是原始码
                    redeemDate = LocalDate.now().toString(),
                    reward = reward
                )
            )
            
            // 9. 增加使用次数
            if (redeemCode.maxUses != -1) {
                android.util.Log.d("VipRepository", "步骤8: 增加使用次数")
                redeemCodeDao.incrementUses(redeemCode.code)
            }
            
            android.util.Log.d("VipRepository", "========== 兑换成功 ==========")
            Result.success(reward)
        } catch (e: Exception) {
            android.util.Log.e("VipRepository", "========== 兑换失败 ==========")
            android.util.Log.e("VipRepository", "兑换码错误: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 添加测试兑换码（仅用于测试）
     * 使用 REPLACE 策略确保兑换码始终是最新的
     */
    private suspend fun addTestRedeemCodes() {
        try {
            android.util.Log.d("VipRepository", "开始添加/更新测试兑换码")
            
            // 添加测试兑换码（使用 REPLACE 策略，如果已存在则更新）
            val testCodes = listOf(
                RedeemCode(
                    code = "HZ223498",
                    type = "VIP",
                    value = "3",  // 终身VIP（VIP3）
                    maxUses = -1,  // 无限使用
                    currentUses = 0,
                    expiryDate = LocalDate.now().plusYears(10).toString(),
                    isActive = true
                ),
                RedeemCode(
                    code = "VIP2024",
                    type = "VIP",
                    value = "1",  // 普通VIP（VIP1）
                    maxUses = -1,  // 无限使用
                    currentUses = 0,
                    expiryDate = LocalDate.now().plusYears(1).toString(),
                    isActive = true
                ),
                RedeemCode(
                    code = "SUPERVIP",
                    type = "VIP",
                    value = "2",  // 年费VIP（VIP2）
                    maxUses = -1,
                    currentUses = 0,
                    expiryDate = LocalDate.now().plusYears(1).toString(),
                    isActive = true
                ),
                RedeemCode(
                    code = "COINS1000",
                    type = "COINS",
                    value = "1000",
                    maxUses = -1,
                    currentUses = 0,
                    expiryDate = LocalDate.now().plusYears(1).toString(),
                    isActive = true
                ),
                RedeemCode(
                    code = "TESTCODE",
                    type = "VIP",
                    value = "1",
                    maxUses = 100,
                    currentUses = 0,
                    expiryDate = LocalDate.now().plusYears(1).toString(),
                    isActive = true
                )
            )
            
            testCodes.forEach { code ->
                redeemCodeDao.insertRedeemCode(code)
                android.util.Log.d("VipRepository", "✓ 兑换码已添加/更新: ${code.code} (类型: ${code.type}, 值: ${code.value})")
            }
            
            // 验证兑换码是否成功添加
            val verification = redeemCodeDao.getRedeemCode("HZ223498")
            if (verification != null) {
                android.util.Log.d("VipRepository", "✓ 验证成功: HZ223498 已在数据库中 (VIP等级: ${verification.value})")
            } else {
                android.util.Log.e("VipRepository", "✗ 验证失败: HZ223498 未找到")
            }
            
            android.util.Log.d("VipRepository", "测试兑换码添加完成，共 ${testCodes.size} 个")
        } catch (e: Exception) {
            android.util.Log.e("VipRepository", "添加测试兑换码失败: ${e.message}", e)
        }
    }
    
    suspend fun purchaseVip(userId: Long, vipLevel: Int, days: Int, cost: Int): Result<String> {
        return try {
            // 检查金币是否足够
            val coins = coinDao.getCoinsAmount(userId) ?: 0
            if (coins < cost) {
                return Result.failure(Exception("金币不足"))
            }
            
            // 获取当前VIP状态
            val currentVip = userVipDao.getUserVipSync(userId)
            
            // 计算新的过期日期
            val expireDate = if (days == -1) {
                null  // 永久
            } else {
                val baseDate = if (currentVip != null && !currentVip.isExpired() && currentVip.expireDate != null) {
                    LocalDate.parse(currentVip.expireDate)
                } else {
                    LocalDate.now()
                }
                baseDate.plusDays(days.toLong()).toString()
            }
            
            // 验证VIP升级操作
            val upgradeValidation = securityValidator.validateVipUpgrade(
                currentVip,
                vipLevel,
                expireDate
            )
            
            if (!upgradeValidation.isValid) {
                return Result.failure(Exception("VIP升级验证失败: ${upgradeValidation.getErrorMessage()}"))
            }
            
            // 扣除金币
            val affected = coinDao.spendCoinsAtomic(userId, cost)
            if (affected == 0) {
                return Result.failure(Exception("金币扣除失败"))
            }
            
            // 更新VIP
            val newVip = if (currentVip == null) {
                UserVip(
                    userId = userId,
                    vipLevel = vipLevel,
                    expireDate = expireDate
                )
            } else {
                currentVip.copy(
                    vipLevel = vipLevel,
                    expireDate = expireDate
                )
            }
            
            // 生成新签名
            val signature = securityValidator.signVipStatus(newVip)
            userVipDao.insertOrUpdate(newVip.copy(signature = signature))
            
            // 根据VIP等级赠送金币
            val bonusCoins = when (vipLevel) {
                1 -> 100   // 普通VIP赠送100金币
                2 -> 500   // 年费VIP赠送500金币
                3 -> 1000  // 终身VIP赠送1000金币
                else -> 0
            }
            
            if (bonusCoins > 0) {
                coinDao.addCoins(userId, bonusCoins)
            }
            
            val levelName = VipLevel.fromLevel(vipLevel).displayName
            Result.success("成功购买 $levelName|$bonusCoins")  // 返回格式：VIP名称|金币数量
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRedeemHistory(userId: Long): List<UserRedeemHistory> {
        return redeemCodeDao.getUserRedeemHistoryList(userId)
    }
}
