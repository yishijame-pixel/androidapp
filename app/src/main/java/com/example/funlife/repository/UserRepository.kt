// UserRepository.kt - 用户仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.UserDao
import com.example.funlife.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    
    suspend fun login(username: String, password: String): User? {
        return userDao.login(username, password)
    }
    
    suspend fun register(username: String, password: String, nickname: String): Result<Long> {
        return try {
            // 检查用户名是否已存在
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                return Result.failure(Exception("用户名已存在"))
            }
            
            val user = User(
                username = username,
                password = password,
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
}
