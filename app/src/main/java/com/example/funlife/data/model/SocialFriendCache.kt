package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

/** 好友列表本地缓存 + 仅本机可见的备注（按 userId 隔离）。 */
@Entity(
    tableName = "social_friend_cache",
    primaryKeys = ["userId", "friendPbId"],
    indices = [Index("userId")],
)
data class SocialFriendCache(
    val userId: Long,
    val friendPbId: String,
    val funlifeUsername: String,
    val displayName: String,
    val avatarUrl: String?,
    val friendshipId: String,
    /** accepted | pending_in | pending_out */
    val status: String,
    val remark: String,
    val online: Boolean = false,
    val updatedAt: Long,
)
