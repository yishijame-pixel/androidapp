// AnniversaryDao.kt - 纪念日数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryDao {
    
    // 获取所有纪念日（置顶的在前，然后按日期排序）
    @Query("SELECT * FROM anniversaries WHERE userId = :userId ORDER BY isPinned DESC, date ASC")
    fun getAllAnniversaries(userId: Long): Flow<List<Anniversary>>
    
    // 获取置顶的纪念日
    @Query("SELECT * FROM anniversaries WHERE userId = :userId AND isPinned = 1 ORDER BY date ASC LIMIT 1")
    fun getPinnedAnniversary(userId: Long): Flow<Anniversary?>
    
    // 插入纪念日
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnniversary(anniversary: Anniversary)
    
    // 更新纪念日
    @Update
    suspend fun updateAnniversary(anniversary: Anniversary)
    
    // 删除纪念日
    @Delete
    suspend fun deleteAnniversary(anniversary: Anniversary)
    
    // 根据ID删除
    @Query("DELETE FROM anniversaries WHERE id = :id")
    suspend fun deleteById(id: Int)
    
    // 取消所有置顶
    @Query("UPDATE anniversaries SET isPinned = 0 WHERE userId = :userId")
    suspend fun unpinAll(userId: Long)
    
    // 按类型筛选
    @Query("SELECT * FROM anniversaries WHERE userId = :userId AND type = :type ORDER BY isPinned DESC, date ASC")
    fun getAnniversariesByType(userId: Long, type: String): Flow<List<Anniversary>>
    
    // 搜索纪念日（按名称或备注）
    @Query("SELECT * FROM anniversaries WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') ORDER BY isPinned DESC, date ASC")
    fun searchAnniversaries(userId: Long, query: String): Flow<List<Anniversary>>
    
    // 获取即将到来的纪念日（N天内）
    @Query("SELECT * FROM anniversaries WHERE userId = :userId ORDER BY date ASC")
    fun getUpcomingAnniversaries(userId: Long): Flow<List<Anniversary>>
    
    // 获取已过期的纪念日（不重复的）
    @Query("SELECT * FROM anniversaries WHERE userId = :userId AND isYearly = 0 ORDER BY date DESC")
    fun getExpiredAnniversaries(userId: Long): Flow<List<Anniversary>>
    
    // 按重要程度排序
    @Query("SELECT * FROM anniversaries WHERE userId = :userId ORDER BY importance DESC, isPinned DESC, date ASC")
    fun getAnniversariesByImportance(userId: Long): Flow<List<Anniversary>>
    
    // 按自定义顺序排序
    @Query("SELECT * FROM anniversaries WHERE userId = :userId ORDER BY customOrder ASC, isPinned DESC, date ASC")
    fun getAnniversariesByCustomOrder(userId: Long): Flow<List<Anniversary>>
    
    // 批量更新排序
    @Update
    suspend fun updateAnniversaries(anniversaries: List<Anniversary>)
}
