// UserRepository.kt - 用户仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.UserDao
import com.example.funlife.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    
    suspend fun login(username: String, password: String): User? {
        val user = userDao.getUserByUsername(username) ?: return null
        
        // 🔥 新增：使用密码哈希验证
        return if (com.example.funlife.utils.PasswordHasher.verifyPassword(password, user.password)) {
            user
        } else {
            null
        }
    }
    
    // 🔥 新增：检查用户名是否存在（用于精确错误提示）
    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }
    
    suspend fun register(username: String, password: String, nickname: String): Result<Long> {
        return try {
            // 检查用户名是否已存在
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                return Result.failure(Exception("用户名已存在"))
            }
            
            // 🔥 新增：哈希密码
            val hashedPassword = com.example.funlife.utils.PasswordHasher.hashPassword(password)
            
            val user = User(
                username = username,
                password = hashedPassword,  // 存储哈希后的密码
                nickname = nickname.ifEmpty { username }
            )
            
            val userId = userDao.insert(user)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)
    }
    
    fun getUserByIdFlow(userId: Long): Flow<User?> {
        return userDao.getUserByIdFlow(userId)
    }
    
    suspend fun updateUser(user: User) {
        userDao.update(user)
    }
    
    suspend fun updateLastLogin(userId: Long) {
        userDao.updateLastLogin(userId, System.currentTimeMillis())
    }
    
    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
    
    // 🔥 新增：修改密码
    suspend fun changePassword(userId: Long, oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = userDao.getUserById(userId) ?: return Result.failure(Exception("用户不存在"))
            
            // 验证旧密码
            if (!com.example.funlife.utils.PasswordHasher.verifyPassword(oldPassword, user.password)) {
                return Result.failure(Exception("旧密码错误"))
            }
            
            // 哈希新密码
            val hashedNewPassword = com.example.funlife.utils.PasswordHasher.hashPassword(newPassword)
            
            // 更新密码
            userDao.update(user.copy(password = hashedNewPassword))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
