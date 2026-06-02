// Account.kt — 多账户系统（聊天记账 Phase 2A）
//
// ⚠️ 数据隔离：
//   - userId 字段无默认值（DEVELOPMENT_PRINCIPLES §1.1）
//   - @Index("userId") 高频过滤
//   - 默认账户由 DefaultAccountSeeder 在每个用户首次进入时插入（不在迁移里硬塞 userId=1）
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账户实体（现金 / 支付宝 / 微信 / 储蓄卡 / 信用卡 / 其它…）。
 *
 * @param balance 当前余额（= 初始余额 + Σ 关联账单 amount，Phase 2A 暂仅作展示，不强一致）
 * @param sortOrder 越小越靠前（默认账户 0~5，自定义从 100 起）
 * @param systemKey 默认账户的稳定键（"cash"/"alipay"/...），用户自建为 null。
 *                  用于跨设备/重装时识别同一类账户。
 */
@Entity(
    tableName = "accounts",
    indices = [Index("userId"), Index(value = ["userId", "systemKey"], unique = false)]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                     // 🔒 无默认值：编译期强制传
    val name: String,
    val type: String,                     // CASH / ALIPAY / WECHAT / DEBIT / CREDIT / OTHER
    val icon: String = "💰",              // emoji
    val color: Long = 0xFFFF8A80L,        // ARGB
    val initialBalance: Double = 0.0,
    val balance: Double = 0.0,            // 当前余额（缓存值，可由账单聚合刷新）
    val isArchived: Boolean = false,
    val sortOrder: Int = 100,
    val systemKey: String? = null,        // 默认账户稳定键
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 账户类型枚举值（数据库存 String，避免 Migration 复杂度）。
 */
object AccountType {
    const val CASH = "CASH"
    const val ALIPAY = "ALIPAY"
    const val WECHAT = "WECHAT"
    const val DEBIT = "DEBIT"
    const val CREDIT = "CREDIT"
    const val OTHER = "OTHER"
}

/**
 * 默认 6 个账户的元信息（每个新用户首次进入聊天记账时由 Seeder 复制一份）。
 */
data class DefaultAccountSpec(
    val systemKey: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: Long,
    val sortOrder: Int
)

val DEFAULT_ACCOUNT_SPECS: List<DefaultAccountSpec> = listOf(
    DefaultAccountSpec("cash",   "现金",   AccountType.CASH,   "💵", 0xFF66BB6AL, 0),
    DefaultAccountSpec("alipay", "支付宝", AccountType.ALIPAY, "🅰️", 0xFF2196F3L, 1),
    DefaultAccountSpec("wechat", "微信",   AccountType.WECHAT, "💬", 0xFF4CAF50L, 2),
    DefaultAccountSpec("debit",  "储蓄卡", AccountType.DEBIT,  "💳", 0xFF7E57C2L, 3),
    DefaultAccountSpec("credit", "信用卡", AccountType.CREDIT, "🏦", 0xFFFF7043L, 4),
    DefaultAccountSpec("other",  "其它",   AccountType.OTHER,  "💰", 0xFF9E9E9EL, 5),
)
