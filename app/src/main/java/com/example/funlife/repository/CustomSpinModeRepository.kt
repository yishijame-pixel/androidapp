// CustomSpinModeRepository.kt - 自定义转盘模式仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.CustomSpinModeDao
import com.example.funlife.data.model.CustomSpinMode
import com.example.funlife.data.model.PresetModeType
import com.example.funlife.data.model.createPresetMode
import kotlinx.coroutines.flow.Flow

class CustomSpinModeRepository(private val modeDao: CustomSpinModeDao) {
    
    fun getAllActiveModes(userId: Long): Flow<List<CustomSpinMode>> = modeDao.getAllActiveModes(userId)
    
    fun getDefaultModes(userId: Long): Flow<List<CustomSpinMode>> = modeDao.getDefaultModes(userId)
    
    fun getCustomModes(userId: Long): Flow<List<CustomSpinMode>> = modeDao.getCustomModes(userId)
    
    suspend fun getModeById(userId: Long, id: Int): CustomSpinMode? = modeDao.getModeById(userId, id)
    
    suspend fun insert(mode: CustomSpinMode): Long = modeDao.insert(mode)
    
    suspend fun update(mode: CustomSpinMode) = modeDao.update(mode)
    
    suspend fun delete(mode: CustomSpinMode) = modeDao.delete(mode)
    
    suspend fun deleteById(userId: Long, id: Int) = modeDao.deleteById(userId, id)
    
    suspend fun incrementUsageCount(userId: Long, id: Int) = modeDao.incrementUsageCount(userId, id)
    
    suspend fun setActive(userId: Long, id: Int, isActive: Boolean) = modeDao.setActive(userId, id, isActive)
    
    // 初始化默认模式
    suspend fun initializeDefaultModes(userId: Long) {
        val count = modeDao.getDefaultModesCount(userId)
        if (count == 0) {
            // 插入三个预设模式
            modeDao.insert(createPresetMode(PresetModeType.NORMAL).copy(userId = userId))
            modeDao.insert(createPresetMode(PresetModeType.ADVANCED).copy(userId = userId))
            modeDao.insert(createPresetMode(PresetModeType.LUCKY).copy(userId = userId))
        }
    }
}
