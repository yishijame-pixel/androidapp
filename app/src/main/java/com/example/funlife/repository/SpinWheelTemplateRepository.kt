// SpinWheelTemplateRepository.kt - 转盘模板仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.SpinWheelTemplateDao
import com.example.funlife.data.model.SpinWheelTemplate
import kotlinx.coroutines.flow.Flow

class SpinWheelTemplateRepository(private val templateDao: SpinWheelTemplateDao) {
    
    val allTemplates: Flow<List<SpinWheelTemplate>> = templateDao.getAllTemplates()
    
    fun getTemplatesByCategory(category: String): Flow<List<SpinWheelTemplate>> {
        return templateDao.getTemplatesByCategory(category)
    }
    
    fun getDefaultTemplates(): Flow<List<SpinWheelTemplate>> {
        return templateDao.getDefaultTemplates()
    }
    
    suspend fun getTemplateById(id: Int): SpinWheelTemplate? {
        return templateDao.getTemplateById(id)
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
    
    suspend fun incrementUsageCount(id: Int) {
        templateDao.incrementUsageCount(id)
    }
    
    fun searchTemplates(query: String): Flow<List<SpinWheelTemplate>> {
        return templateDao.searchTemplates(query)
    }
}
