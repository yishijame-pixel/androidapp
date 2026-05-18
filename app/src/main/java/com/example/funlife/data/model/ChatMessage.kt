package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val role: String,                // "user" | "ai"
    val content: String,             // 消息文本
    val personaId: String = "default", // 当前人格ID
    val type: String = "text",       // "text" | "bill" | "system"
    val billId: Long? = null,        // 关联账单ID（记账消息才有）
    val timestamp: Long = System.currentTimeMillis()
)
