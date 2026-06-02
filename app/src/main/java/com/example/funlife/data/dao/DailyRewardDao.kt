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

    // 🔒 原子领取：仅当日期与今天不同才会真正更新（事务内防并发双发）
    @Query("""
        UPDATE daily_rewards SET lastClaimDate = :date, claimCount = claimCount + 1
        WHERE userId = :userId AND rewardType = :rewardType
          AND (lastClaimDate IS NULL OR lastClaimDate != :date)
    """)
    suspend fun claimIfNewDay(userId: Long, rewardType: String, date: String): Int

    // 🔒 去重历史脏数据：同 (userId, rewardType) 只保留最早一条
    @Query("""
        DELETE FROM daily_rewards
        WHERE userId = :userId AND rewardType = :rewardType
          AND id NOT IN (
            SELECT MIN(id) FROM daily_rewards WHERE userId = :userId AND rewardType = :rewardType
          )
    """)
    suspend fun dedupe(userId: Long, rewardType: String): Int
}
