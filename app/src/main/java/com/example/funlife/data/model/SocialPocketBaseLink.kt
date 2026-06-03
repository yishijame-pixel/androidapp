package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 本地账号 ↔ PocketBase 用户映射（Token 存 EncryptedSharedPreferences，不进 Room）。 */
@Entity(
    tableName = "social_pb_links",
    indices = [Index("userId")],
)
data class SocialPocketBaseLink(
    @PrimaryKey val userId: Long,
    val pbRecordId: String,
    /** PocketBase 登录 identity（合成邮箱，非用户真实邮箱） */
    val pbIdentity: String,
    val linkedAt: Long,
)
