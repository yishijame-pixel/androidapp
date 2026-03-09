// CustomSpinModeRepository.kt - 自定义转盘模式仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.CustomSpinModeDao
import com.example.funlife.data.model.CustomSpinMode
import com.example.funlife.data.model.PresetModeType
import com.example.funlife.data.model.createPresetMode
import kotlinx.coroutines.flow.Flow

class CustomSpinModeRepository(private val modeDao: CustomSpinModeDao) {
    
    fun getAllActiveModes(): Flow<List<CustomSpinMode>> = modeDao.getAllActiveModes()
    
    fun getDefaultModes(): Flow<List<CustomSpinMode>> = modeDao.getDefaultModes()
    
    fun getCustomModes(): Flow<List<CustomSpinMode>> = modeDao.getCustomModes()
    
    suspend fun getModeById(id: Int): CustomSpinMode? = modeDao.getModeById(id)
    
    suspend fun insert(mode: CustomSpinMode): Long = modeDao.insert(mode)
    
    suspend fun update(mode: CustomSpinMode) = modeDao.update(mode)
    
    suspend fun delete(mode: CustomSpinMode) = modeDao.delete(mode)
    
    suspend fun deleteById(id: Int) = modeDao.deleteById(id)
    
    suspend fun incrementUsageCount(id: Int) = modeDao.incrementUsageCount(id)
    
    suspend fun setActive(id: Int, isActive: Boolean) = modeDao.setActive(id, isActive)
    
    // 初始化默认模式
    suspend fun initializeDefaultModes() {
        val count = modeDao.getDefaultModesCount()
        if (count == 0) {
            // 插入三个预设模式
            modeDao.insert(createPresetMode(PresetModeType.NORMAL))
            modeDao.insert(createPresetMode(PresetModeType.ADVANCED))
            modeDao.insert(createPresetMode(PresetModeType.LUCKY))
        }
    }
}
