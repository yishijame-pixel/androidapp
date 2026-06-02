// MorningHeraldLog.kt — v53 阅光书房 · 晨光信使日志
//
// 防重复：同一用户同一天最多一条。
// 复合主键 (userId, dateYmd)。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "morning_herald_log",
    primaryKeys = ["userId", "dateYmd"],
    indices = [Index(value = ["userId"])]
)
data class MorningHeraldLog(
    val userId: Long,
    /** YYYYMMDD */
    val dateYmd: Int,
    /** quote / capsule_preview / mood_pairing / galaxy_hot */
    val contentType: String,
    /** 推送内容摘要（用于去重展示和回访） */
    val payloadSummary: String = "",
    val sentAt: Long = System.currentTimeMillis(),
)
