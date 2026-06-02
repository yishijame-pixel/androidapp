// SystemQuotaUsed.kt — v53 阅光书房 · 系统赠送配额计数
//
// 用于"心情低谷召回"等系统主动赠送类功能的月度去重。
// 复合主键 (userId, quotaKey, monthYm)。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "system_quota_used",
    primaryKeys = ["userId", "quotaKey", "monthYm"],
    indices = [Index(value = ["userId"])]
)
data class SystemQuotaUsed(
    val userId: Long,
    /** 配额 key：例如 "quiet_rescue" / "morning_herald_book_pairing" */
    val quotaKey: String,
    /** YYYYMM，例如 202605 */
    val monthYm: Int,
    val count: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
)
