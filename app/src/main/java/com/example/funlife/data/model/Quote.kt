// Quote.kt — v53 阅光书房 · 摘抄 + 时光胶囊
//
// 一条 Quote = 一句摘抄。
// 当 capsuleDeliveryAt > 0 时，这条摘抄会被时光信箱投递 Worker 在到期日推送给用户。
//
// 加密策略：text 字段以明文存储（与 Book.note/favoriteQuote 保持一致便于本地搜索），
// 但在投递通知/星河发布等"对外"路径上由 Repository 决定是否脱敏/加密。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quotes",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "bookId"]),
        Index(value = ["userId", "capsuleDeliveryAt"]),
    ]
)
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    /** 摘抄正文（最长建议 500 字，UI 层校验） */
    val text: String,
    /** 摘录页码（0 = 未填） */
    val page: Int = 0,
    /** 用户对该句的私人评分 0~5（用于心情低谷召回筛优质句） */
    val rating: Int = 0,
    /** 钉选（同 rating ≥4 一并视为"金句"） */
    val pinned: Boolean = false,
    /** 时光胶囊投递时间戳（毫秒）；0 表示未绑定胶囊 */
    val capsuleDeliveryAt: Long = 0L,
    /** 胶囊是否已送达（投递完置 true，避免重复推送） */
    val capsuleDelivered: Boolean = false,
    /** 是否已发布到匿名摘抄星河（避免重复发布） */
    val publishedToGalaxy: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
