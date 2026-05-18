package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_personas")
data class ChatPersona(
    @PrimaryKey val id: String,      // "dad", "girlfriend", "roast", "gentle"
    val name: String,                // 显示名称
    val avatar: String,              // 头像emoji
    val bubbleColor: Long,           // 气泡颜色 ARGB
    val systemPrompt: String,        // 系统提示词
    val isBuiltin: Boolean = true,   // 内置/用户自定义
    val sortOrder: Int = 0,
    val customAvatarUri: String? = null // 用户自定义头像URI
)

@Entity(tableName = "chat_persona_state")
data class ChatPersonaState(
    @PrimaryKey val personaId: String,
    val userId: Long = 0,
    val affection: Int = 50,         // 好感度 0-100
    val mood: String = "normal",     // happy/normal/angry/sad
    val interactionCount: Int = 0
)
