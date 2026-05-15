// UserPreferencesDao.kt - 用户偏好数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    
    // 🔥 修改：根据用户ID获取偏好
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getPreferences(userId: Long): Flow<UserPreferences?>
    
    // 🔥 修改：根据用户ID同步获取偏好
    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getPreferencesSync(userId: Long): UserPreferences?
    
    // 插入或更新偏好
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)
    
    // 更新偏好
    @Update
    suspend fun updatePreferences(preferences: UserPreferences)
    
    // 🔥 修改：更新深色模式
    @Query("UPDATE user_preferences SET isDarkMode = :isDarkMode WHERE userId = :userId")
    suspend fun updateDarkMode(userId: Long, isDarkMode: Boolean)
    
    // 🔥 修改：更新通知设置
    @Query("UPDATE user_preferences SET enableNotifications = :enable WHERE userId = :userId")
    suspend fun updateNotifications(userId: Long, enable: Boolean)
    
    // 🔥 修改：更新默认加分值
    @Query("UPDATE user_preferences SET defaultScoreIncrement = :increment WHERE userId = :userId")
    suspend fun updateScoreIncrement(userId: Long, increment: Int)
    
    // 🔥 新增：更新音效设置
    @Query("UPDATE user_preferences SET enableSound = :enable WHERE userId = :userId")
    suspend fun updateSound(userId: Long, enable: Boolean)
    
    // 🔥 新增：更新震动设置
    @Query("UPDATE user_preferences SET enableVibration = :enable WHERE userId = :userId")
    suspend fun updateVibration(userId: Long, enable: Boolean)
    
    // 🔥 新增：更新转盘主题
    @Query("UPDATE user_preferences SET wheelTheme = :theme WHERE userId = :userId")
    suspend fun updateWheelTheme(userId: Long, theme: String)
    
    // 🔥 新增：更新权重可视化
    @Query("UPDATE user_preferences SET showWeightVisualization = :show WHERE userId = :userId")
    suspend fun updateWeightVisualization(userId: Long, show: Boolean)
    
    // 🔥 新增：更新粒子效果
    @Query("UPDATE user_preferences SET particleEffectEnabled = :enable WHERE userId = :userId")
    suspend fun updateParticleEffect(userId: Long, enable: Boolean)
    
    // 🔥 新增：更新烟花效果
    @Query("UPDATE user_preferences SET fireworksEnabled = :enable WHERE userId = :userId")
    suspend fun updateFireworks(userId: Long, enable: Boolean)
    
    // 🔥 新增：更新金币动画
    @Query("UPDATE user_preferences SET coinAnimationEnabled = :enable WHERE userId = :userId")
    suspend fun updateCoinAnimation(userId: Long, enable: Boolean)
    
    // 🔥 新增：保存最后使用的模板
    @Query("UPDATE user_preferences SET lastTemplateId = :templateId WHERE userId = :userId")
    suspend fun updateLastTemplate(userId: Long, templateId: Int?)
    
    // 🔥 新增：保存最后自定义的选项
    @Query("UPDATE user_preferences SET lastCustomOptions = :options WHERE userId = :userId")
    suspend fun updateLastCustomOptions(userId: Long, options: String)
    
    // 🔥 新增：保存最后使用的转盘模式
    @Query("UPDATE user_preferences SET lastSpinMode = :mode WHERE userId = :userId")
    suspend fun updateLastSpinMode(userId: Long, mode: String)
    
    // 🔥 新增：保存最后使用的自定义模式ID
    @Query("UPDATE user_preferences SET lastCustomModeId = :modeId WHERE userId = :userId")
    suspend fun updateLastCustomModeId(userId: Long, modeId: Int?)
    
    // 🔥 新增：更新三个模式的独立选项配置
    @Query("UPDATE user_preferences SET normalModeOptions = :options WHERE userId = :userId")
    suspend fun updateNormalModeOptions(userId: Long, options: String)
    
    @Query("UPDATE user_preferences SET advancedModeOptions = :options WHERE userId = :userId")
    suspend fun updateAdvancedModeOptions(userId: Long, options: String)
    
    @Query("UPDATE user_preferences SET luckyModeOptions = :options WHERE userId = :userId")
    suspend fun updateLuckyModeOptions(userId: Long, options: String)
    
    // 🔥 新增：更新首页面板自定义文字
    @Query("UPDATE user_preferences SET homePanelText = :text WHERE userId = :userId")
    suspend fun updateHomePanelText(userId: Long, text: String)
    
    // 🔥 新增：更新艺术字颜色主题
    @Query("UPDATE user_preferences SET homePanelTextStyle = :style WHERE userId = :userId")
    suspend fun updateHomePanelTextStyle(userId: Long, style: String)
    
    // 🔥 新增：更新转盘结算面板皮肤
    @Query("UPDATE user_preferences SET spinResultPanelSkin = :skin WHERE userId = :userId")
    suspend fun updateSpinResultPanelSkin(userId: Long, skin: String)
    
    // 🔥 新增：更新转盘按钮皮肤
    @Query("UPDATE user_preferences SET spinButtonSkin = :skin WHERE userId = :userId")
    suspend fun updateSpinButtonSkin(userId: Long, skin: String)
    
    // 🔥 新增：更新转盘旋转音量
    @Query("UPDATE user_preferences SET spinRotationVolume = :volume WHERE userId = :userId")
    suspend fun updateSpinRotationVolume(userId: Long, volume: Float)
    
    // 🔥 新增：更新装备的头像框
    @Query("UPDATE user_preferences SET equippedAvatarFrame = :frameAssetPath WHERE userId = :userId")
    suspend fun updateEquippedAvatarFrame(userId: Long, frameAssetPath: String?)
}
