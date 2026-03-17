// CoinDao.kt - 金币数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserCoins
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    
    @Query("SELECT * FROM user_coins WHERE userId = :userId")
    fun getUserCoins(userId: Long): Flow<UserCoins?>
    
    @Query("SELECT coins FROM user_coins WHERE userId = :userId")
    suspend fun getCoinsAmount(userId: Long): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCoins(userCoins: UserCoins)
    
    @Query("UPDATE user_coins SET coins = coins + :amount, totalEarned = totalEarned + :amount WHERE userId = :userId")
    suspend fun addCoins(userId: Long, amount: Int)
    
    @Query("UPDATE user_coins SET coins = coins - :amount WHERE userId = :userId AND coins >= :amount")
    suspend fun spendCoinsAtomic(userId: Long, amount: Int): Int  // 返回受影响的行数
    
    @Query("INSERT OR IGNORE INTO user_coins (userId, coins, totalEarned) VALUES (:userId, 0, 0)")
    suspend fun initializeCoins(userId: Long)
}
