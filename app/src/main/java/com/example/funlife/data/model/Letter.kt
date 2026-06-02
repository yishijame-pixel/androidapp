// Letter.kt — 时光信箱：信件（DB v51）
//
// 一条 Letter 既可能是用户写的（direction = TO_RECIPIENT），
// 也可能是 AI 替身回的（direction = FROM_RECIPIENT，parentLetterId 指向用户那封）。
//
// 🌟 延时投递核心字段：
//   - sentAt:      用户写信 / AI 生成回信的时间
//   - deliveryAt:  应送达时间（VIP 可选立即；免费用户最少 +3 天）
//   - deliveredAt: 实际送达时间（null = 还未送达，UI 显示"在路上"）
//
// 🔒 数据隔离：userId 无默认值；@Index("userId")、@Index("recipientId")。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object LetterDirection {
    const val TO_RECIPIENT = "to_recipient"     // 用户 → AI 替身
    const val FROM_RECIPIENT = "from_recipient" // AI 替身 → 用户
}

object LetterStatus {
    const val PENDING = "pending"       // 已写出 / 已生成，等待"投递时间"到达
    const val DELIVERED = "delivered"   // 已送达，可阅读
    const val FAILED = "failed"         // AI 生成失败（人工兜底文案 / 让用户重试）
}

@Entity(
    tableName = "letters",
    indices = [Index("userId"), Index("recipientId"), Index("deliveryAt")]
)
data class Letter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认
    val recipientId: Long,
    val direction: String,                  // TO_RECIPIENT / FROM_RECIPIENT
    val content: String,                    // 信件正文（用户写的 or AI 生成的）
    val mood: String? = null,               // 用户写信时附的心情标签（可选 emoji）
    val sentAt: Long = System.currentTimeMillis(),
    val deliveryAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val status: String = LetterStatus.PENDING,
    val isRead: Boolean = false,
    /** 如果是 FROM_RECIPIENT 回信，指向用户写的那封 TO_RECIPIENT */
    val parentLetterId: Long? = null,
    /** AI 生成失败原因（仅 status=FAILED 时填写，便于人工兜底/重试） */
    val failureReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
