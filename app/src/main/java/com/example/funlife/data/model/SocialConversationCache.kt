package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

/** 私聊会话本地缓存（按 FunLife userId 隔离）。 */
@Entity(
    tableName = "social_conversation_cache",
    primaryKeys = ["userId", "conversationId"],
    indices = [
        Index("userId"),
        Index(value = ["userId", "lastMessageAt"]),
    ],
)
data class SocialConversationCache(
    val userId: Long,
    val conversationId: String,
    val peerPbId: String,
    val peerUsername: String,
    val peerDisplayName: String,
    val peerAvatarUrl: String?,
    val lastPreview: String,
    val lastMessageAt: Long,
    val updatedAt: Long,
)
