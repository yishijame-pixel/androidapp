// CoinRepository.kt - 金币仓库（增强安全版）
package com.example.funlife.repository

import android.content.Context
import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.model.UserCoins
import com.example.funlife.security.CoinSecurityManager
import com.example.funlife.security.CoinOperationType
import kotlinx.coroutines.flow.Flow

class CoinRepository(
    private val coinDao: CoinDao,
    private val context: Context? = null
) {
    
    private val securityManager: CoinSecurityManager? = context?.let { CoinSecurityManager(it) }
    
    fun getUserCoins(userId: Long): Flow<UserCoins?> = coinDao.getUserCoins(userId)
    
    suspend fun initializeCoins(userId: Long) = coinDao.initializeCoins(userId)
    
    suspend fun getCoinsAmount(userId: Long): Int = coinDao.getCoinsAmount(userId) ?: 0
    
    suspend fun addCoins(userId: Long, amount: Int) {
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
    }
    
    suspend fun spendCoins(userId: Long, amount: Int): Boolean {
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
        }
        
        return success
    }
    
    suspend fun removeCoins(userId: Long, amount: Int): Boolean {
        // 使用 spendCoins 的安全机制
        return spendCoins(userId, amount)
    }
    
    // 商城积分相关方法
    suspend fun getShopPoints(userId: Long): Int = coinDao.getShopPoints(userId) ?: 0
    
    suspend fun addShopPoints(userId: Long, amount: Int) {
        coinDao.addShopPoints(userId, amount)
    }
    
    suspend fun spendShopPoints(userId: Long, amount: Int): Boolean {
        val rowsAffected = coinDao.spendShopPointsAtomic(userId, amount)
        return rowsAffected > 0
    }
}
