// UserPreferencesDao.kt - 用户偏好数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    
    // 获取用户偏好
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>
    
    // 插入或更新偏好
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)
    
    // 更新偏好
    @Update
    suspend fun updatePreferences(preferences: UserPreferences)
    
    // 更新深色模式
    @Query("UPDATE user_preferences SET isDarkMode = :isDarkMode WHERE id = 1")
    suspend fun updateDarkMode(isDarkMode: Boolean)
    
    // 更新通知设置
    @Query("UPDATE user_preferences SET enableNotifications = :enable WHERE id = 1")
    suspend fun updateNotifications(enable: Boolean)
    
    // 更新默认加分值
    @Query("UPDATE user_preferences SET defaultScoreIncrement = :increment WHERE id = 1")
    suspend fun updateScoreIncrement(increment: Int)
}
