// AnniversaryRepository.kt - 纪念日仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.AnniversaryDao
import com.example.funlife.data.model.Anniversary
import kotlinx.coroutines.flow.Flow

class AnniversaryRepository(private val anniversaryDao: AnniversaryDao) {
    
    val allAnniversaries: Flow<List<Anniversary>> = anniversaryDao.getAllAnniversaries()
    val pinnedAnniversary: Flow<Anniversary?> = anniversaryDao.getPinnedAnniversary()
    
    suspend fun insert(anniversary: Anniversary) {
        anniversaryDao.insertAnniversary(anniversary)
    }
    
    suspend fun update(anniversary: Anniversary) {
        anniversaryDao.updateAnniversary(anniversary)
    }
    
    suspend fun delete(anniversary: Anniversary) {
        anniversaryDao.deleteAnniversary(anniversary)
    }
    
    suspend fun pinAnniversary(anniversary: Anniversary) {
        // 先取消所有置顶
        anniversaryDao.unpinAll()
        // 再置顶当前纪念日
        anniversaryDao.updateAnniversary(anniversary.copy(isPinned = true))
    }
    
    suspend fun unpinAnniversary(anniversary: Anniversary) {
        anniversaryDao.updateAnniversary(anniversary.copy(isPinned = false))
    }
}
