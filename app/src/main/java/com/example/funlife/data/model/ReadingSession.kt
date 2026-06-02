// ReadingSession.kt — v53 阅光书房 · 阅读时长打卡
//
// 每次"今天读了 X 分钟"打卡产生一条记录。
// 用于：连续天数计算、月度阅读时长曲线、单本书阅读心电图。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_sessions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "dateYmd"]),
        Index(value = ["userId", "bookId"]),
    ]
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    /** null 表示"自由阅读"打卡，不绑书 */
    val bookId: Long? = null,
    /** 阅读时长（分钟） */
    val minutes: Int,
    /** 当地时区日期，YYYYMMDD 整数（如 20260527），便于连续天数 SQL 直查 */
    val dateYmd: Int,
    /** 当时所在页（用于阅读心电图聚合，可空 0） */
    val atPage: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
