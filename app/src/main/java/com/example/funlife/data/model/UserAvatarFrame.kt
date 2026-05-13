// UserAvatarFrame.kt - 用户头像框拥有数据模型
package com.example.funlife.data.model

import androidx.room.Entity

/**
 * 用户拥有的头像框
 */
@Entity(
    tableName = "user_avatar_frames",
    primaryKeys = ["userId", "frameId"]
)
data class UserAvatarFrame(
    val userId: Long,              // 用户ID
    val frameId: Int,              // 头像框商品ID
    val purchasedAt: Long = System.currentTimeMillis(),  // 购买时间
    val isEquipped: Boolean = false  // 是否装备中
)
