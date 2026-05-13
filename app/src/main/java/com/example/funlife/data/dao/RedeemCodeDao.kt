// RedeemCodeDao.kt - 兑换码数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.RedeemCode
import com.example.funlife.data.model.UserRedeemHistory

@Dao
interface RedeemCodeDao {
    
    @Query("SELECT * FROM redeem_codes WHERE code = :code")
    suspend fun getRedeemCode(code: String): RedeemCode?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeemCode(redeemCode: RedeemCode)
    
    @Update
    suspend fun updateRedeemCode(redeemCode: RedeemCode)
    
    @Query("UPDATE redeem_codes SET currentUses = currentUses + 1 WHERE code = :code")
    suspend fun incrementUses(code: String)
    
    @Query("SELECT * FROM user_redeem_history WHERE userId = :userId AND code = :code")
    suspend fun getUserRedeemHistory(userId: Long, code: String): UserRedeemHistory?
    
    @Insert
    suspend fun insertRedeemHistory(history: UserRedeemHistory)
    
    @Query("SELECT * FROM user_redeem_history WHERE userId = :userId ORDER BY redeemDate DESC")
    suspend fun getUserRedeemHistoryList(userId: Long): List<UserRedeemHistory>
}
