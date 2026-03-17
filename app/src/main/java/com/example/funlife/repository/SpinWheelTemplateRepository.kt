// SpinWheelTemplateRepository.kt - 转盘模板仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.SpinWheelTemplateDao
import com.example.funlife.data.model.SpinWheelTemplate
import kotlinx.coroutines.flow.Flow

class SpinWheelTemplateRepository(private val templateDao: SpinWheelTemplateDao) {
    
    fun getAllTemplates(userId: Long): Flow<List<SpinWheelTemplate>> = templateDao.getAllTemplates(userId)
    
    fun getTemplatesByCategory(userId: Long, category: String): Flow<List<SpinWheelTemplate>> {
        return templateDao.getTemplatesByCategory(userId, category)
    }
    
    fun getDefaultTemplates(userId: Long): Flow<List<SpinWheelTemplate>> {
        return templateDao.getDefaultTemplates(userId)
    }
    
    suspend fun getTemplateById(userId: Long, id: Int): SpinWheelTemplate? {
        return templateDao.getTemplateById(userId, id)
    }
    
    suspend fun insert(template: SpinWheelTemplate) {
        templateDao.insertTemplate(template)
    }
    
    suspend fun update(template: SpinWheelTemplate) {
        templateDao.updateTemplate(template)
    }
    
    suspend fun delete(template: SpinWheelTemplate) {
        templateDao.deleteTemplate(template)
    }
    
    suspend fun incrementUsageCount(userId: Long, id: Int) {
        templateDao.incrementUsageCount(userId, id)
    }
    
    fun searchTemplates(userId: Long, query: String): Flow<List<SpinWheelTemplate>> {
        return templateDao.searchTemplates(userId, query)
    }
}
