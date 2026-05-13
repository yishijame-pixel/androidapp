// UserAvatarFrameDao.kt - 用户头像框DAO
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.UserAvatarFrame
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAvatarFrameDao {
    
    /**
     * 获取用户拥有的所有头像框
     */
    @Query("SELECT * FROM user_avatar_frames WHERE userId = :userId ORDER BY purchasedAt DESC")
    fun getUserFrames(userId: Long): Flow<List<UserAvatarFrame>>
    
    /**
     * 检查用户是否拥有某个头像框
     */
    @Query("SELECT COUNT(*) > 0 FROM user_avatar_frames WHERE userId = :userId AND frameId = :frameId")
    suspend fun isFrameOwned(userId: Long, frameId: Int): Boolean
    
    /**
     * 添加头像框到用户背包
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserFrame(userFrame: UserAvatarFrame)
    
    /**
     * 获取用户当前装备的头像框
     */
    @Query("SELECT * FROM user_avatar_frames WHERE userId = :userId AND isEquipped = 1 LIMIT 1")
    suspend fun getEquippedFrame(userId: Long): UserAvatarFrame?
    
    /**
     * 装备头像框（先取消所有装备，再装备指定的）
     */
    @Transaction
    suspend fun equipFrame(userId: Long, frameId: Int) {
        // 取消所有装备
        unequipAllFrames(userId)
        // 装备指定头像框
        updateFrameEquipStatus(userId, frameId, true)
    }
    
    /**
     * 取消所有头像框装备
     */
    @Query("UPDATE user_avatar_frames SET isEquipped = 0 WHERE userId = :userId")
    suspend fun unequipAllFrames(userId: Long)
    
    /**
     * 更新头像框装备状态
     */
    @Query("UPDATE user_avatar_frames SET isEquipped = :isEquipped WHERE userId = :userId AND frameId = :frameId")
    suspend fun updateFrameEquipStatus(userId: Long, frameId: Int, isEquipped: Boolean)
    
    /**
     * 删除用户的头像框
     */
    @Query("DELETE FROM user_avatar_frames WHERE userId = :userId AND frameId = :frameId")
    suspend fun deleteUserFrame(userId: Long, frameId: Int)
}
