package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

/** 私聊消息本地缓存（按 FunLife userId 隔离）。 */
@Entity(
    tableName = "social_message_cache",
    primaryKeys = ["userId", "messageId"],
    indices = [
        Index("userId"),
        Index(value = ["userId", "conversationId", "createdAt"]),
    ],
)
data class SocialMessageCache(
    val userId: Long,
    val conversationId: String,
    val messageId: String,
    val senderPbId: String,
    val body: String,
    val createdAt: Long,
)
