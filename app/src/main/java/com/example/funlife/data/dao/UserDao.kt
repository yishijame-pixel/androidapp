// UserDao.kt - 用户数据访问对象
package com.example.funlife.data.dao

import androidx.room.*
import com.example.funlife.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // 🔥 修改：移除密码比对，改为只查询用户名
    // 密码验证在 Repository 层使用哈希比对
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?
    
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: Long): Flow<User?>
    
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long
    
    @Update
    suspend fun update(user: User)
    
    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: Long, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
    
    // 🔥 新增：头像框装备相关
    
    /**
     * 更新用户装备的头像框
     */
    @Query("UPDATE users SET equippedFrameId = :frameId WHERE id = :userId")
    suspend fun updateEquippedFrame(userId: Long, frameId: Int?)
    
    /**
     * 获取用户装备的头像框ID
     */
    @Query("SELECT equippedFrameId FROM users WHERE id = :userId")
    suspend fun getEquippedFrameId(userId: Long): Int?
    
    /**
     * 检查用户是否为VIP
     */
    @Query("SELECT isVip FROM users WHERE id = :userId")
    suspend fun isUserVip(userId: Long): Boolean
    
    /**
     * 检查用户是否为VIP（Flow）
     */
    @Query("SELECT isVip FROM users WHERE id = :userId")
    fun isUserVipFlow(userId: Long): Flow<Boolean>
    
    /**
     * 更新用户VIP状态
     */
    @Query("UPDATE users SET isVip = :isVip, vipExpireAt = :expireAt WHERE id = :userId")
    suspend fun updateVipStatus(userId: Long, isVip: Boolean, expireAt: Long?)
}
