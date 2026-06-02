// VipRepository.kt - VIP仓库（增强安全版）
package com.example.funlife.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.funlife.data.dao.UserVipDao
import com.example.funlife.data.dao.RedeemCodeDao
import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.database.AppDatabase
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
    private val context: Context,
    private val db: AppDatabase? = null
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
            // 🔒 严重漏洞修复：签名校验失败 = 数据被外部篡改，必须降级到非 VIP，
            //    不能"用当前数据重新生成签名"——那会把伪造的 vipLevel/expireDate 洗白。
            val validation = securityValidator.validateVipStatus(existing)
            if (!validation.isValid) {
                android.util.Log.w("VipRepository", "VIP 状态签名异常 → 强制降级: ${validation.getErrorMessage()}")
                val downgraded = UserVip(userId = userId, vipLevel = 0, expireDate = null)
                val newSignature = securityValidator.signVipStatus(downgraded)
                userVipDao.insertOrUpdate(downgraded.copy(signature = newSignature))
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
            // 🔄 领取前主动同步一次云端配置：保证用户领到与后台一致的最新金额。
            //   受 VipRuntimeConfig 内部 30s 节流，不会变成高频调用。
            //   网络失败时静默降级用本地缓存，绝不阻塞用户领取。
            try {
                com.example.funlife.vip.VipRuntimeConfig.refresh(context, force = false)
            } catch (e: Exception) {
                android.util.Log.w("VipRepository", "claim 前刷新配置失败，使用本地缓存: ${e.message}")
            }
            // 🔄 运行时配置 > 枚举默认（后台 SKU 配置可动态调整）
            val coins = com.example.funlife.vip.VipRuntimeConfig.dailyCoinsOf(vipLevel)

            // 🔒 顺序至关重要 —— 防止"领金币后锁没记上导致重复领取":
            //    5. 先占锁（recordClaim 写加密 SharedPreferences）
            //    6. 占锁成功后才发金币 + 写日期；任一失败也已占了锁，宁愿漏领不可重复领
            if (!dailyCoinManager.recordClaim(userId)) {
                return Result.failure(Exception("记录领取时间失败"))
            }

            // 6. 添加金币 + 更新日期 一起放在 Room 事务里
            val database = db
            if (database != null) {
                database.withTransaction {
                    coinDao.addCoins(userId, coins)
                    userVipDao.updateLastDailyClaimDate(userId, LocalDate.now().toString())
                }
            } else {
                coinDao.addCoins(userId, coins)
                userVipDao.updateLastDailyClaimDate(userId, LocalDate.now().toString())
            }

            Result.success(coins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun redeemCode(userId: Long, code: String): Result<String> {
        // 🔒 并发防护：整个兑换流程在单写事务内串行执行，避免双读同一未使用记录后双发。
        val database = db
        return if (database != null) {
            try { database.withTransaction { redeemCodeImpl(userId, code) } }
            catch (e: Exception) { Result.failure(e) }
        } else redeemCodeImpl(userId, code)
    }

    private suspend fun redeemCodeImpl(userId: Long, code: String): Result<String> {
        return try {
            android.util.Log.d("VipRepository", "========== 开始兑换码流程 ==========")
            android.util.Log.d("VipRepository", "用户ID: $userId, 兑换码: $code")
            
            // 1. 输入验证
            if (code.isBlank() || code.length < 4) {
                android.util.Log.e("VipRepository", "兑换码格式错误: 长度=${code.length}")
                return Result.failure(Exception("兑换码格式错误"))
            }
            
            // 2. 在IO线程生成兑换码哈希（保持军事级加密，避免UI卡顿）
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
                    
                    // 根据 VIP 等级设置过期日期（与云端 sku.js 三档严格一致）
                    val expireDate = when (vipLevel) {
                        1 -> LocalDate.now().plusDays(30).toString()   // 月卡 30 天
                        2 -> LocalDate.now().plusDays(365).toString()  // 年卡 365 天
                        3 -> null   // 终身永久
                        99 -> null  // PERMANENT（旧体系兼容）永久
                        else -> LocalDate.now().plusDays(30).toString()
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
                    
                    // 根据 VIP 等级赠送金币（与云端 sku.js bonusCoins 保持一致）
                    val bonusCoins = when (vipLevel) {
                        1 -> 50    // 月卡激活
                        2 -> 300   // 年卡激活
                        3, 99 -> 1000  // 终身激活
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
    
    suspend fun purchaseVip(userId: Long, vipLevel: Int, days: Int, cost: Int): Result<String> {
        // 🔒 安全策略：禁止用金币购买 VIP（防止本地刷金币换 VIP）
        // VIP 必须通过云端发行的卡密激活，由服务端做 HMAC 签名校验。
        return Result.failure(Exception("VIP 现已统一通过激活码开通，请到\"我的-VIP-激活码\"页面输入卡密"))
        @Suppress("UNREACHABLE_CODE")
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
