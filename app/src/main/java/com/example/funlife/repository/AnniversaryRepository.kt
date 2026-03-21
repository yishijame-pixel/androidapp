// AnniversaryRepository.kt - 纪念日仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.AnniversaryDao
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.flow.Flow

class AnniversaryRepository(private val anniversaryDao: AnniversaryDao) {
    
    fun getAllAnniversaries(userId: Long): Flow<List<Anniversary>> = anniversaryDao.getAllAnniversaries(userId)
    fun getPinnedAnniversary(userId: Long): Flow<Anniversary?> = anniversaryDao.getPinnedAnniversary(userId)
    
    suspend fun insert(anniversary: Anniversary) {
        anniversaryDao.insertAnniversary(anniversary)
    }
    
    suspend fun update(anniversary: Anniversary) {
        anniversaryDao.updateAnniversary(anniversary)
    }
    
    suspend fun delete(anniversary: Anniversary) {
        anniversaryDao.deleteAnniversary(anniversary)
    }
    
    suspend fun pinAnniversary(userId: Long, anniversary: Anniversary) {
        // 先取消所有置顶
        anniversaryDao.unpinAll(userId)
        // 再置顶当前纪念日
        anniversaryDao.updateAnniversary(anniversary.copy(isPinned = true))
    }
    
    suspend fun unpinAnniversary(anniversary: Anniversary) {
        anniversaryDao.updateAnniversary(anniversary.copy(isPinned = false))
    }
    
    // 筛选和搜索
    fun getAnniversariesByType(userId: Long, type: String): Flow<List<Anniversary>> = 
        anniversaryDao.getAnniversariesByType(userId, type)
    
    fun searchAnniversaries(userId: Long, query: String): Flow<List<Anniversary>> = 
        anniversaryDao.searchAnniversaries(userId, query)
    
    fun getUpcomingAnniversaries(userId: Long): Flow<List<Anniversary>> = 
        anniversaryDao.getUpcomingAnniversaries(userId)
    
    fun getExpiredAnniversaries(userId: Long): Flow<List<Anniversary>> = 
        anniversaryDao.getExpiredAnniversaries(userId)
    
    fun getAnniversariesByImportance(userId: Long): Flow<List<Anniversary>> = 
        anniversaryDao.getAnniversariesByImportance(userId)
    
    // 更新排序
    suspend fun updateAnniversariesOrder(anniversaries: List<Anniversary>) {
        anniversaryDao.updateAnniversaries(anniversaries)
    }
}
