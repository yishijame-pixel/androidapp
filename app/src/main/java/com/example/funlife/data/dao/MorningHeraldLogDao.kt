// MorningHeraldLogDao.kt — v53 阅光书房 · 晨光信使日志
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.MorningHeraldLog

@Dao
interface MorningHeraldLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(log: MorningHeraldLog): Long

    @Query("SELECT * FROM morning_herald_log WHERE userId = :userId AND dateYmd = :dateYmd LIMIT 1")
    suspend fun getOf(userId: Long, dateYmd: Int): MorningHeraldLog?

    /** 最近 N 天本周已经推送过几次（用于普通用户每周 2 次限制） */
    @Query("""
        SELECT COUNT(*) FROM morning_herald_log
        WHERE userId = :userId AND dateYmd >= :sinceYmd
    """)
    suspend fun countSince(userId: Long, sinceYmd: Int): Int
}
