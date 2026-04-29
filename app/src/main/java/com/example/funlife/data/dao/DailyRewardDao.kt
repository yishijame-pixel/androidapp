// DailyRewardDao.kt - 每日奖励数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.DailyReward

@Dao
interface DailyRewardDao {
    
    @Query("SELECT * FROM daily_rewards WHERE userId = :userId AND rewardType = :rewardType LIMIT 1")
    suspend fun getDailyReward(userId: Long, rewardType: String): DailyReward?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyReward(dailyReward: DailyReward)
    
    @Query("UPDATE daily_rewards SET lastClaimDate = :date, claimCount = claimCount + 1 WHERE userId = :userId AND rewardType = :rewardType")
    suspend fun updateClaimDate(userId: Long, rewardType: String, date: String)
}
