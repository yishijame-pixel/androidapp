// DiaryEntry.kt — 日记本条目（DB v56）
//
// 区别于 mood_entries.note（短小情绪日记）：DiaryEntry 是用户在某一册「古籍日记本」
// （按 bookSkinId 区分皮肤）中写下的长文，每页一篇，标题 + 正文 + 元数据。
//
// 🔒 数据隔离：userId + bookSkinId + pageSlot 唯一；不同皮肤的书互不相通。
// 📄 pageSlot：在书中的页码（2 = 第一内容页，不含封面/题辞）。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [
        Index("userId"),
        Index("bookSkinId"),
        Index(value = ["userId", "bookSkinId"]),
        Index(value = ["userId", "bookSkinId", "pageSlot"], unique = true),
    ]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认
    /** 所属魔法书皮肤，如 builtin::qingchuan */
    val bookSkinId: String,
    /** 在书中的页码（2 起为正文页，与 PageCurl 页索引一致） */
    val pageSlot: Int,
    val date: String,                       // yyyy-MM-dd（元数据，可编辑）
    val title: String = "",
    val content: String = "",
    val weather: String? = null,
    val temperature: Float? = null,
    val moodEmoji: String? = null,
    val location: String? = null,
    val bookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
