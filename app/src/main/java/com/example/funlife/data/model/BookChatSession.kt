// BookChatSession.kt — v54 阅光书房 · AI 读书伴侣长对话存档（VIP3 专享）
//
// 设计要点：
//   - 一次完整对话（用户进入 BookChatScreen 直到退出/新建）= 一条 BookChatSession
//   - 消息列表用 messagesJson 持久化为 JSON 字符串（避免再开一张表）
//   - 仅 VIP3 / 永久会员 才会被 BookChatViewModel 写入；非 VIP3 对话仅内存
//   - 多用户隔离：所有查询 WHERE userId = ?
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_chat_sessions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "bookId"]),
        Index(value = ["userId", "lastMessageAt"]),
    ]
)
data class BookChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    /** 对话标题（默认从首条用户消息截 20 字；用户也可改） */
    val title: String = "",
    /** 消息数组的 JSON 序列化结果。
     *  schema: [ { "role": "user|ai|system", "text": "...", "ts": <ms> }, ... ]  */
    val messagesJson: String = "[]",
    /** 总轮次（user msg 数） */
    val turnCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long = System.currentTimeMillis(),
)
