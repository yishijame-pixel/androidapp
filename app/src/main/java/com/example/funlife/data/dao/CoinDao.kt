// CoinDao.kt - 金币数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserCoins
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    
    @Query("SELECT * FROM user_coins WHERE id = 1")
    fun getUserCoins(): Flow<UserCoins?>
    
    @Query("SELECT coins FROM user_coins WHERE id = 1")
    suspend fun getCoinsAmount(): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCoins(userCoins: UserCoins)
    
    @Query("UPDATE user_coins SET coins = coins + :amount, totalEarned = totalEarned + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)
    
    @Query("UPDATE user_coins SET coins = coins - :amount WHERE id = 1")
    suspend fun spendCoins(amount: Int)
    
    @Query("INSERT OR IGNORE INTO user_coins (id, coins, totalEarned) VALUES (1, 0, 0)")
    suspend fun initializeCoins()
}
