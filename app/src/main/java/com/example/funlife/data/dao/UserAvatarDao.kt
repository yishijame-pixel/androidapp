// UserAvatarDao.kt - 用户头像数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAvatarDao {
    
    // ========== 用户头像信息 ==========
    
    @Query("SELECT * FROM user_avatars WHERE userId = :userId")
    fun getUserAvatar(userId: Long): Flow<UserAvatar?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserAvatar(userAvatar: UserAvatar)
    
    @Query("UPDATE user_avatars SET avatarUri = :avatarUri, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAvatarUri(userId: Long, avatarUri: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_avatars SET frameId = :frameId, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateFrameId(userId: Long, frameId: String?, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_avatars SET backgroundId = :backgroundId, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateBackgroundId(userId: Long, backgroundId: String?, updatedAt: Long = System.currentTimeMillis())
    
    // ========== 头像框 ==========
    
    @Query("SELECT * FROM avatar_frames ORDER BY requiredVipLevel, price")
    fun getAllFrames(): Flow<List<AvatarFrame>>
    
    @Query("SELECT * FROM avatar_frames WHERE id = :frameId")
    suspend fun getFrameById(frameId: String): AvatarFrame?
    
    @Query("SELECT * FROM avatar_frames WHERE requiredVipLevel <= :vipLevel ORDER BY requiredVipLevel, price")
    fun getAvailableFrames(vipLevel: Int): Flow<List<AvatarFrame>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrame(frame: AvatarFrame)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrames(frames: List<AvatarFrame>)
    
    // ========== 背景主题 ==========
    
    @Query("SELECT * FROM profile_backgrounds ORDER BY requiredVipLevel, price")
    fun getAllBackgrounds(): Flow<List<ProfileBackground>>
    
    @Query("SELECT * FROM profile_backgrounds WHERE id = :backgroundId")
    suspend fun getBackgroundById(backgroundId: String): ProfileBackground?
    
    @Query("SELECT * FROM profile_backgrounds WHERE requiredVipLevel <= :vipLevel ORDER BY requiredVipLevel, price")
    fun getAvailableBackgrounds(vipLevel: Int): Flow<List<ProfileBackground>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackground(background: ProfileBackground)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackgrounds(backgrounds: List<ProfileBackground>)
    
    // ========== 用户拥有的头像框 ==========
    
    @Query("SELECT * FROM user_owned_frames WHERE userId = :userId")
    fun getUserOwnedFrames(userId: Long): Flow<List<UserOwnedFrame>>
    
    @Query("SELECT COUNT(*) > 0 FROM user_owned_frames WHERE userId = :userId AND frameId = :frameId")
    suspend fun hasFrame(userId: Long, frameId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOwnedFrame(ownedFrame: UserOwnedFrame)
    
    // ========== 用户拥有的背景 ==========
    
    @Query("SELECT * FROM user_owned_backgrounds WHERE userId = :userId")
    fun getUserOwnedBackgrounds(userId: Long): Flow<List<UserOwnedBackground>>
    
    @Query("SELECT COUNT(*) > 0 FROM user_owned_backgrounds WHERE userId = :userId AND backgroundId = :backgroundId")
    suspend fun hasBackground(userId: Long, backgroundId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addOwnedBackground(ownedBackground: UserOwnedBackground)
    
    // ========== 组合查询 ==========
    
    /**
     * 获取用户可用的头像框（已拥有或VIP等级足够的免费框）
     */
    @Query("""
        SELECT f.* FROM avatar_frames f
        LEFT JOIN user_owned_frames uof ON f.id = uof.frameId AND uof.userId = :userId
        WHERE f.requiredVipLevel <= :vipLevel 
        AND (f.price = 0 OR uof.frameId IS NOT NULL)
        ORDER BY f.requiredVipLevel, f.price
    """)
    fun getUserAvailableFrames(userId: Long, vipLevel: Int): Flow<List<AvatarFrame>>
    
    /**
     * 获取用户可用的背景（已拥有或VIP等级足够的免费背景）
     */
    @Query("""
        SELECT b.* FROM profile_backgrounds b
        LEFT JOIN user_owned_backgrounds uob ON b.id = uob.backgroundId AND uob.userId = :userId
        WHERE b.requiredVipLevel <= :vipLevel 
        AND (b.price = 0 OR uob.backgroundId IS NOT NULL)
        ORDER BY b.requiredVipLevel, b.price
    """)
    fun getUserAvailableBackgrounds(userId: Long, vipLevel: Int): Flow<List<ProfileBackground>>
}
