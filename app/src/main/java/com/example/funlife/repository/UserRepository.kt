// UserRepository.kt - 用户仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.UserDao
import com.example.funlife.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    suspend fun login(username: String, password: String): User? {
        val user = userDao.getUserByUsername(username) ?: return null
        return if (com.example.funlife.utils.PasswordHasher.verifyPassword(password, user.password)) {
            user
        } else {
            null
        }
    }

    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    /**
     * 云端验密通过后在本机重建账号（清数据场景）。
     */
    suspend fun recreateLocalAccount(username: String, password: String, nickname: String): User {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            return resetLocalPassword(existing, password, nickname)
        }
        val hashed = com.example.funlife.utils.PasswordHasher.hashPassword(password)
        val user = User(
            username = username,
            password = hashed,
            nickname = nickname.ifBlank { username },
        )
        val id = userDao.insert(user)
        return user.copy(id = id)
    }

    /** 本地残留错误密码哈希时，用云端验密结果覆盖。 */
    suspend fun resetLocalPassword(user: User, password: String, nickname: String): User {
        val hashed = com.example.funlife.utils.PasswordHasher.hashPassword(password)
        val updated = user.copy(
            password = hashed,
            nickname = nickname.ifBlank { user.nickname },
            lastLoginAt = System.currentTimeMillis(),
        )
        userDao.update(updated)
        return updated
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
