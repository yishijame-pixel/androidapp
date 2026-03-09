// SpinWheelTemplateDao.kt - 转盘模板数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.SpinWheelTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface SpinWheelTemplateDao {
    
    // 获取所有模板
    @Query("SELECT * FROM spin_wheel_templates ORDER BY usageCount DESC, name ASC")
    fun getAllTemplates(): Flow<List<SpinWheelTemplate>>
    
    // 根据分类获取模板
    @Query("SELECT * FROM spin_wheel_templates WHERE category = :category ORDER BY name ASC")
    fun getTemplatesByCategory(category: String): Flow<List<SpinWheelTemplate>>
    
    // 获取默认模板
    @Query("SELECT * FROM spin_wheel_templates WHERE isDefault = 1")
    fun getDefaultTemplates(): Flow<List<SpinWheelTemplate>>
    
    // 根据ID获取模板
    @Query("SELECT * FROM spin_wheel_templates WHERE id = :id")
    suspend fun getTemplateById(id: Int): SpinWheelTemplate?
    
    // 插入模板
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SpinWheelTemplate)
    
    // 更新模板
    @Update
    suspend fun updateTemplate(template: SpinWheelTemplate)
    
    // 删除模板
    @Delete
    suspend fun deleteTemplate(template: SpinWheelTemplate)
    
    // 增加使用次数
    @Query("UPDATE spin_wheel_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: Int)
    
    // 搜索模板
    @Query("SELECT * FROM spin_wheel_templates WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTemplates(query: String): Flow<List<SpinWheelTemplate>>
}
