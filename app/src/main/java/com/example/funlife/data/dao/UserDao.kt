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
}
