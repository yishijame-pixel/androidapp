// UserAvatar.kt - 用户头像信息模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户头像信息
 */
@Entity(tableName = "user_avatars")
data class UserAvatar(
    @PrimaryKey
    val userId: Long,
    val avatarUri: String? = null,  // 头像URI（本地或网络）
    val frameId: String? = null,    // 当前使用的头像框ID
    val backgroundId: String? = null, // 当前使用的背景ID
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 头像框数据模型
 */
@Entity(tableName = "avatar_frames")
data class AvatarFrame(
    @PrimaryKey
    val id: String,  // 唯一标识，如 "gold_circle", "diamond_border"
    val name: String,  // 显示名称
    val icon: String,  // 图标emoji或资源
    val price: Int,  // 价格（金币）
    val requiredVipLevel: Int,  // 需要的VIP等级（0=普通用户，1=VIP1，2=VIP2，3=VIP3）
    val animationType: String,  // 动画类型：none, glow, rotate, 3d
    val category: String = "basic",  // 分类：basic, premium, exclusive
    val description: String = "",  // 描述
    val isDefault: Boolean = false  // 是否为默认头像框
)

/**
 * 背景主题数据模型
 */
@Entity(tableName = "profile_backgrounds")
data class ProfileBackground(
    @PrimaryKey
    val id: String,  // 唯一标识
    val name: String,  // 显示名称
    val preview: String,  // 预览图标emoji
    val price: Int,  // 价格
    val requiredVipLevel: Int,  // 需要的VIP等级
    val gradientColors: String,  // 渐变色列表，用逗号分隔，如 "#FFD700,#FFA500"
    val particleType: String,  // 粒子类型：none, gold, diamond, crown, star, meteor
    val description: String = "",
    val isDefault: Boolean = false
)

/**
 * 用户拥有的头像框
 */
@Entity(tableName = "user_owned_frames")
data class UserOwnedFrame(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val frameId: String,
    val purchasedAt: Long = System.currentTimeMillis()
)

/**
 * 用户拥有的背景
 */
@Entity(tableName = "user_owned_backgrounds")
data class UserOwnedBackground(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val backgroundId: String,
    val purchasedAt: Long = System.currentTimeMillis()
)

/**
 * 用户统计信息
 */
data class UserStatistics(
    val registeredDays: Int,  // 注册天数
    val totalCheckIns: Int,  // 累计签到天数
    val totalCoins: Int,  // 累计获得金币
    val vipDays: Int  // VIP天数
)
