// Book.kt — 方案 F · 人生书架
//
// 用户记录读过的每一本书：书名、作者、读完日期、心得、最爱句子、星级。
// VIP3 专享：调用 AI 把当年所有书整理成"个人书评年鉴"。
package com.example.funlife.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "finishedAt"]),
    ]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val author: String = "",
    /** 0~5 星，0 表示未评分 */
    val rating: Int = 0,
    /** 读完时间（毫秒）；未读完时为 0 */
    val finishedAt: Long = 0L,
    /** 用户心得（自由长文） */
    val note: String = "",
    /** 最喜欢的一句话 / 摘抄 */
    val favoriteQuote: String = "",
    /** 标签（逗号分隔，例如 "成长,小说,推荐"） */
    val tags: String = "",
    @ColumnInfo(name = "createdAt") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),

    /* ---------------- v53 阅光书房扩展 ---------------- */
    /** 总页数（0 = 未填） */
    val totalPages: Int = 0,
    /** 当前阅读到的页数 */
    val currentPage: Int = 0,
    /** 开篇期待信（"我希望从此书获得 X"） */
    val openingLetter: String = "",
    /** 开篇时的心情 emoji 或简短描述 */
    val openingMood: String = "",
    /** 读完时的心情快照（emoji + 一句话） */
    val finishedMood: String = "",
)

/** 年度统计 + 年鉴展示用 */
data class BookYearStats(
    val year: Int,
    val totalBooks: Int,
    val totalRated: Int,
    val avgRating: Double,
    val topTags: List<Pair<String, Int>>,   // (tag, count) 前 5
)
