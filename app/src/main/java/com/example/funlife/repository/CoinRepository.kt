// CoinRepository.kt - 金币仓库（增强安全版）
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.model.UserCoins
import com.example.funlife.security.CoinSecurityManager
import com.example.funlife.security.CoinOperationType
import com.example.funlife.vip.CoinCloudReporter
import kotlinx.coroutines.flow.Flow

class CoinRepository(
    private val coinDao: CoinDao,
    private val context: Context? = null
) {
    
    private val securityManager: CoinSecurityManager? = context?.let { CoinSecurityManager(it) }
    
    fun getUserCoins(userId: Long): Flow<UserCoins?> = coinDao.getUserCoins(userId)
    
    suspend fun initializeCoins(userId: Long) = coinDao.initializeCoins(userId)
    
    suspend fun getCoinsAmount(userId: Long): Int = coinDao.getCoinsAmount(userId) ?: 0

    /** 异步上报金币变动到云端（fire-and-forget；context 为空或获取用户名失败则跳过） */
    private suspend fun reportToCloud(userId: Long, op: String, amount: Int, reason: String) {
        val ctx = context ?: return
        try {
            val db = AppDatabase.getDatabase(ctx)
            val user = db.userDao().getUserById(userId) ?: return
            val balance = getCoinsAmount(userId)
            val coins = db.coinDao().getCoinsAmount(userId)
            val totalEarned = db.coinDao().getUserCoinsSync(userId)?.totalEarned ?: 0
            CoinCloudReporter.report(
                context = ctx,
                username = user.username,
                op = op,
                amount = amount,
                reason = reason,
                balance = balance,
                totalEarned = totalEarned,
                totalSpent = (totalEarned - balance).coerceAtLeast(0),
            )
        } catch (_: Exception) { /* 静默 */ }
    }

    suspend fun addCoins(userId: Long, amount: Int, reason: String = "unknown") {
        // 安全验证
        securityManager?.let { manager ->
            val validation = manager.validateCoinOperation(userId, amount, CoinOperationType.EARN)
            if (!validation.isValid) {
                throw SecurityException("金币操作验证失败: ${validation.getErrorMessage()}")
            }
        }
        
        // 执行操作
        coinDao.addCoins(userId, amount)
        
        // 记录操作
        securityManager?.let { manager ->
            val currentBalance = getCoinsAmount(userId)
            manager.recordCoinOperation(userId, amount, CoinOperationType.EARN, currentBalance)
        }

        // 云端上报（异步、非阻塞）
        reportToCloud(userId, "earn", amount, reason)
    }

    suspend fun spendCoins(userId: Long, amount: Int, reason: String = "unknown"): Boolean {
        // 安全验证
        securityManager?.let { manager ->
            val validation = manager.validateCoinOperation(userId, amount, CoinOperationType.SPEND)
            if (!validation.isValid) {
                throw SecurityException("金币操作验证失败: ${validation.getErrorMessage()}")
            }
            
            // 异常检测
            val anomalies = manager.detectAnomalies(userId)
            if (anomalies.isNotEmpty()) {
                throw SecurityException("检测到异常行为: ${anomalies.joinToString(", ")}")
            }
        }
        
        // 使用原子操作，防止金币变成负数
        val rowsAffected = coinDao.spendCoinsAtomic(userId, amount)
        val success = rowsAffected > 0
        
        // 记录操作
        if (success) {
            securityManager?.let { manager ->
                val currentBalance = getCoinsAmount(userId)
                manager.recordCoinOperation(userId, amount, CoinOperationType.SPEND, currentBalance)
            }
            // 云端上报（异步、非阻塞）
            reportToCloud(userId, "spend", amount, reason)
        }
        
        return success
    }
    
    suspend fun removeCoins(userId: Long, amount: Int): Boolean {
        // 使用 spendCoins 的安全机制
        return spendCoins(userId, amount)
    }
    
    // 商城积分相关方法
    suspend fun getShopPoints(userId: Long): Int = coinDao.getShopPoints(userId) ?: 0

    /** 从云端 account_recover 钱包快照写回本机（清数据恢复）。 */
    suspend fun restoreWalletFromCloudSnapshot(
        userId: Long,
        wallet: com.example.funlife.account.CloudWalletSnapshot,
    ) {
        coinDao.initializeCoins(userId)
        if (!wallet.hasSnapshot && wallet.balance == 0 && wallet.pointsBalance == 0) return
        val updated = coinDao.setWalletSnapshot(
            userId = userId,
            coins = wallet.balance.coerceAtLeast(0),
            totalEarned = wallet.totalEarned.coerceAtLeast(0),
            shopPoints = wallet.pointsBalance.coerceAtLeast(0),
        )
        if (updated == 0) {
            coinDao.insertUserCoins(
                UserCoins(
                    userId = userId,
                    coins = wallet.balance.coerceAtLeast(0),
                    totalEarned = wallet.totalEarned.coerceAtLeast(0),
                    shopPoints = wallet.pointsBalance.coerceAtLeast(0),
                ),
            )
        }
    }

    /** 异步上报积分变动到云端（fire-and-forget） */
    private suspend fun reportPointsToCloud(userId: Long, op: String, amount: Int, reason: String) {
        val ctx = context ?: return
        try {
            val db = AppDatabase.getDatabase(ctx)
            val user = db.userDao().getUserById(userId) ?: return
            val pointsBalance = getShopPoints(userId)
            val coinsBalance = getCoinsAmount(userId)
            val totalEarned = db.coinDao().getUserCoinsSync(userId)?.totalEarned ?: 0
            CoinCloudReporter.report(
                context = ctx,
                username = user.username,
                op = op,                          // "point_earn" / "point_spend"
                amount = amount,
                reason = reason,
                balance = coinsBalance,           // 兼容字段：金币余额
                totalEarned = totalEarned,
                totalSpent = (totalEarned - coinsBalance).coerceAtLeast(0),
                pointsBalance = pointsBalance,    // 🔒 新增：积分当前余额
            )
        } catch (_: Exception) { /* 静默 */ }
    }

    /**
     * 🔒 启动时快照上报：把当前金币+积分余额发到云端，让运营对账。
     * 服务端可对比"累计 delta vs 余额"，发现"凭空增加"等异常。
     * 失败完全静默，不影响主流程。
     */
    suspend fun reportBalancesSnapshot(userId: Long) {
        val ctx = context ?: return
        try {
            val db = AppDatabase.getDatabase(ctx)
            val user = db.userDao().getUserById(userId) ?: return
            val coinsBalance = getCoinsAmount(userId)
            val pointsBalance = getShopPoints(userId)
            val totalEarned = db.coinDao().getUserCoinsSync(userId)?.totalEarned ?: 0
            CoinCloudReporter.report(
                context = ctx,
                username = user.username,
                op = "snapshot",
                amount = 1,                  // 占位（不会计入累计）
                reason = "startup_snapshot",
                balance = coinsBalance,
                totalEarned = totalEarned,
                totalSpent = (totalEarned - coinsBalance).coerceAtLeast(0),
                pointsBalance = pointsBalance,
            )
        } catch (_: Exception) { /* 静默 */ }
    }

    /**
     * 🔒 已经在外部事务内 DAO 直写 user_coins 后，调用本方法补上"安全记录 + 云端上报"。
     * 不重复入库，仅做审计与异常检测。
     */
    suspend fun notifyShopPointsAdded(userId: Long, amount: Int, reason: String) {
        if (amount <= 0) return
        try {
            securityManager?.let { manager ->
                val balance = getShopPoints(userId)
                manager.recordCoinOperation(userId, amount, CoinOperationType.POINT_EARN, balance)
            }
            reportPointsToCloud(userId, "point_earn", amount, reason)
        } catch (_: Exception) { /* 静默 */ }
    }

    suspend fun addShopPoints(userId: Long, amount: Int, reason: String = "unknown") {
        if (amount <= 0) return
        // 🔒 安全校验（积分阈值更严）
        securityManager?.let { manager ->
            val validation = manager.validateCoinOperation(userId, amount, CoinOperationType.POINT_EARN)
            if (!validation.isValid) {
                throw SecurityException("积分操作验证失败: ${validation.getErrorMessage()}")
            }
        }
        coinDao.addShopPoints(userId, amount)
        // 记录操作（用于频率检测）
        securityManager?.let { manager ->
            val balance = getShopPoints(userId)
            manager.recordCoinOperation(userId, amount, CoinOperationType.POINT_EARN, balance)
        }
        // 云端上报（异步）
        reportPointsToCloud(userId, "point_earn", amount, reason)
    }

    suspend fun spendShopPoints(userId: Long, amount: Int, reason: String = "unknown"): Boolean {
        if (amount <= 0) return false
        // 🔒 安全校验
        securityManager?.let { manager ->
            val validation = manager.validateCoinOperation(userId, amount, CoinOperationType.POINT_SPEND)
            if (!validation.isValid) {
                throw SecurityException("积分操作验证失败: ${validation.getErrorMessage()}")
            }
            val anomalies = manager.detectAnomalies(userId)
            if (anomalies.isNotEmpty()) {
                throw SecurityException("检测到异常行为: ${anomalies.joinToString(", ")}")
            }
        }
        // 原子扣减
        val rowsAffected = coinDao.spendShopPointsAtomic(userId, amount)
        val success = rowsAffected > 0
        if (success) {
            securityManager?.let { manager ->
                val balance = getShopPoints(userId)
                manager.recordCoinOperation(userId, amount, CoinOperationType.POINT_SPEND, balance)
            }
            reportPointsToCloud(userId, "point_spend", amount, reason)
        }
        return success
    }
}
