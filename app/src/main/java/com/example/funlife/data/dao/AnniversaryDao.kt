// AnniversaryDao.kt - 纪念日数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.flow.Flow

@Dao
interface AnniversaryDao {
    
    // 获取所有纪念日（置顶的在前，然后按日期排序）
    @Query("SELECT * FROM anniversaries ORDER BY isPinned DESC, date ASC")
    fun getAllAnniversaries(): Flow<List<Anniversary>>
    
    // 获取置顶的纪念日
    @Query("SELECT * FROM anniversaries WHERE isPinned = 1 ORDER BY date ASC LIMIT 1")
    fun getPinnedAnniversary(): Flow<Anniversary?>
    
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
    @Query("UPDATE anniversaries SET isPinned = 0")
    suspend fun unpinAll()
}
