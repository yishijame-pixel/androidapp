// ReaderDnaCard.kt — v53 阅光书房 · 读者 DNA 人格画像
//
// 每生成一次画像写一条记录。雷达图 6 维向量 + AI 生成的 tagline + Top 关键词。
// vectorJson 结构：
//   {"rationality":0.7,"sensibility":0.6,"inward":0.8,"outward":0.4,
//    "gentleness":0.7,"sharpness":0.3,"keywords":["孤独","自由","成长","记忆","勇气"]}
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reader_dna_cards",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "generatedAt"]),
    ]
)
data class ReaderDnaCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val generatedAt: Long = System.currentTimeMillis(),
    /** 6 维向量 + Top 关键词，JSON 文本 */
    val vectorJson: String,
    /** 一句话人格概括 */
    val tagline: String,
    /** 生成时已读完书数（用于展示"基于 N 本书"） */
    val basedOnBookCount: Int = 0,
)
