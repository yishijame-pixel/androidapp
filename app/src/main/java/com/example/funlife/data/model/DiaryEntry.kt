// DiaryEntry.kt — 日记本条目（DB v55）
//
// 区别于 mood_entries.note（短小情绪日记）：DiaryEntry 是用户在"古籍日记本"中
// 写下的长文反思，每天一篇，标题 + 正文 + 元数据（天气/心情/温度）。
//
// 🔒 数据隔离：userId 无默认值；@Index("userId")；(userId, date) 唯一索引避免一日多篇。
// 📅 date：yyyy-MM-dd，作为业务主键的一部分，跨设备同步友好。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [
        Index("userId"),
        Index(value = ["userId", "date"], unique = true)
    ]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认
    val date: String,                       // yyyy-MM-dd
    val title: String = "",                 // 标题（可空，UI 显示时用日期作 fallback）
    val content: String = "",               // 正文（纯文本，富文本留 v2）
    val weather: String? = null,            // "晴" / "小雨" / null
    val temperature: Float? = null,         // 摄氏度
    val moodEmoji: String? = null,          // "😊" 等，可关联当日 mood_entries.mood
    val location: String? = null,           // 可选地理位置
    val bookmarked: Boolean = false,        // 是否加书签（v2 红绳书签用）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
