// CoinRepository.kt - 金币仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.model.UserCoins
import kotlinx.coroutines.flow.Flow

class CoinRepository(private val coinDao: CoinDao) {
    
    fun getUserCoins(userId: Long): Flow<UserCoins?> = coinDao.getUserCoins(userId)
    
    suspend fun initializeCoins(userId: Long) = coinDao.initializeCoins(userId)
    
    suspend fun getCoinsAmount(userId: Long): Int = coinDao.getCoinsAmount(userId) ?: 0
    
    suspend fun addCoins(userId: Long, amount: Int) = coinDao.addCoins(userId, amount)
    
    suspend fun spendCoins(userId: Long, amount: Int): Boolean {
        // 使用原子操作，防止金币变成负数
        val rowsAffected = coinDao.spendCoinsAtomic(userId, amount)
        return rowsAffected > 0
    }
    
    suspend fun removeCoins(userId: Long, amount: Int): Boolean {
        // 使用原子操作
        val rowsAffected = coinDao.spendCoinsAtomic(userId, amount)
        return rowsAffected > 0
    }
}
