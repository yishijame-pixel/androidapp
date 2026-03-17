// SpinWheelTemplateDao.kt - 转盘模板数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.SpinWheelTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface SpinWheelTemplateDao {
    
    // 获取所有模板
    @Query("SELECT * FROM spin_wheel_templates WHERE userId = :userId ORDER BY usageCount DESC, name ASC")
    fun getAllTemplates(userId: Long): Flow<List<SpinWheelTemplate>>
    
    // 根据分类获取模板
    @Query("SELECT * FROM spin_wheel_templates WHERE userId = :userId AND category = :category ORDER BY name ASC")
    fun getTemplatesByCategory(userId: Long, category: String): Flow<List<SpinWheelTemplate>>
    
    // 获取默认模板
    @Query("SELECT * FROM spin_wheel_templates WHERE userId = :userId AND isDefault = 1")
    fun getDefaultTemplates(userId: Long): Flow<List<SpinWheelTemplate>>
    
    // 根据ID获取模板
    @Query("SELECT * FROM spin_wheel_templates WHERE userId = :userId AND id = :id")
    suspend fun getTemplateById(userId: Long, id: Int): SpinWheelTemplate?
    
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
    @Query("UPDATE spin_wheel_templates SET usageCount = usageCount + 1 WHERE userId = :userId AND id = :id")
    suspend fun incrementUsageCount(userId: Long, id: Int)
    
    // 搜索模板
    @Query("SELECT * FROM spin_wheel_templates WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTemplates(userId: Long, query: String): Flow<List<SpinWheelTemplate>>
}
