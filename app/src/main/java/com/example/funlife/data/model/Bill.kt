package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0,
    val amount: Double,              // 金额（正=收入，负=支出）
    val category: String,            // 分类：餐饮/交通/购物/娱乐...
    val note: String = "",           // 备注
    val timestamp: Long = System.currentTimeMillis(),
    val linkedMessageId: Long? = null, // 关联的聊天消息ID
    // 🆕 Phase 2A：多账户支持。null = 兼容旧账单未指定账户
    val accountId: Long? = null
)
