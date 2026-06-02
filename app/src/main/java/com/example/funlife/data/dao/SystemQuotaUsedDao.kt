// SystemQuotaUsedDao.kt — v53 阅光书房 · 系统赠送配额
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.SystemQuotaUsed

@Dao
interface SystemQuotaUsedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: SystemQuotaUsed)

    @Query("""
        SELECT IFNULL(count, 0) FROM system_quota_used
        WHERE userId = :userId AND quotaKey = :key AND monthYm = :monthYm
    """)
    suspend fun getCount(userId: Long, key: String, monthYm: Int): Int?

    /** 原子 +1（不存在则插入） */
    @Query("""
        INSERT INTO system_quota_used(userId, quotaKey, monthYm, count, updatedAt)
        VALUES(:userId, :key, :monthYm, 1, :now)
        ON CONFLICT(userId, quotaKey, monthYm)
        DO UPDATE SET count = count + 1, updatedAt = :now
    """)
    suspend fun increment(userId: Long, key: String, monthYm: Int, now: Long = System.currentTimeMillis())
}
