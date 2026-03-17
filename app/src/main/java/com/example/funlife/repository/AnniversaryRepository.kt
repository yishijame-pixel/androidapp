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
}
