// UserVipDao.kt - VIP数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserVip
import kotlinx.coroutines.flow.Flow

@Dao
interface UserVipDao {
    
    @Query("SELECT * FROM user_vip WHERE userId = :userId")
    fun getUserVip(userId: Long): Flow<UserVip?>
    
    @Query("SELECT * FROM user_vip WHERE userId = :userId")
    suspend fun getUserVipSync(userId: Long): UserVip?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userVip: UserVip)
    
    @Update
    suspend fun update(userVip: UserVip)
    
    @Query("UPDATE user_vip SET lastDailyClaimDate = :date WHERE userId = :userId")
    suspend fun updateLastDailyClaimDate(userId: Long, date: String)
    
    @Query("UPDATE user_vip SET vipLevel = :level, expireDate = :expireDate WHERE userId = :userId")
    suspend fun updateVipLevel(userId: Long, level: Int, expireDate: String?)
    
    @Query("DELETE FROM user_vip WHERE userId = :userId")
    suspend fun deleteUserVip(userId: Long)
}
