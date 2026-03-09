// CoinRepository.kt - 金币仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.model.UserCoins
import kotlinx.coroutines.flow.Flow

class CoinRepository(private val coinDao: CoinDao) {
    
    val userCoins: Flow<UserCoins?> = coinDao.getUserCoins()
    
    suspend fun initializeCoins() = coinDao.initializeCoins()
    
    suspend fun getCoinsAmount(): Int = coinDao.getCoinsAmount() ?: 0
    
    suspend fun addCoins(amount: Int) = coinDao.addCoins(amount)
    
    suspend fun spendCoins(amount: Int): Boolean {
        val currentCoins = getCoinsAmount()
        return if (currentCoins >= amount) {
            coinDao.spendCoins(amount)
            true
        } else {
            false
        }
    }
    
    suspend fun removeCoins(amount: Int) {
        val currentCoins = getCoinsAmount()
        if (currentCoins >= amount) {
            coinDao.spendCoins(amount)
        }
    }
}
