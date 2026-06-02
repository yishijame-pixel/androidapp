// LetterRecipient.kt — 时光信箱：收信人（DB v51）
//
// 用户给"过去/未来的自己 / 已故亲人 / 异地爱人 / 暧昧未表白的他"写信。
// 收信人是一个由用户创建的"角色卡"，AI 在生成回信时按这张卡的人设扮演。
//
// 🔒 数据隔离：userId 无默认值；@Index("userId")。
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 收信人关系类型（用于 AI prompt 与 UI 标签）
 *   SELF_PAST    - 过去的自己（含具体时间）
 *   SELF_FUTURE  - 未来的自己
 *   FAMILY       - 家人（父母 / 长辈 / 已故亲人）
 *   LOVER        - 恋人 / 暧昧对象 / 前任
 *   FRIEND       - 朋友 / 知己
 *   CUSTOM       - 自定义
 */
object RecipientRelation {
    const val SELF_PAST = "self_past"
    const val SELF_FUTURE = "self_future"
    const val FAMILY = "family"
    const val LOVER = "lover"
    const val FRIEND = "friend"
    const val CUSTOM = "custom"
}

@Entity(
    tableName = "letter_recipients",
    indices = [Index("userId")]
)
data class LetterRecipient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认
    val name: String,                       // "5 年前的我"、"爷爷"、"她"
    val avatar: String = "✉️",              // emoji（无自定义图时用）
    val customAvatarUri: String? = null,    // 用户上传的头像 URI
    val relation: String = RecipientRelation.CUSTOM,
    /** AI 扮演时的人设描述（一段自然语言）：
     *  例如"我的爷爷，已经过世。生前是退休教师，性格严厉但内心柔软，
     *  喜欢下象棋，常说'吃得苦中苦方为人上人'"
     *  这段会作为 LLM system prompt 的核心。 */
    val persona: String = "",
    /** 时间锚点（毫秒，可选）：
     *  - SELF_PAST=2020-01-01 → AI 知道"5 年前的你"是哪一年
     *  - SELF_FUTURE=2030-01-01 → AI 装作那一年的你
     *  - 其它关系可为 null */
    val timeAnchor: Long? = null,
    val sortOrder: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
