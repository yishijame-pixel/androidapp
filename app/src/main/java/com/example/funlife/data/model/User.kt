// User.kt - 用户数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val password: String, // 实际项目中应该加密存储
    val nickname: String = username,
    val avatar: String = "", // 头像URL或资源ID
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

// 用户会话信息
data class UserSession(
    val userId: Long,
    val username: String,
    val nickname: String,
    val avatar: String
)
