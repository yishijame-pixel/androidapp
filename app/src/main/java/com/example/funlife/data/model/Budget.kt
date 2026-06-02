// Budget.kt — 预算实体（聊天记账 Phase 2B）
//
// 🔒 数据隔离：userId 无默认值；@Index("userId") 高频过滤。
//
// 三档 scope × 三档 period 的组合可表达：
//   - 月度总预算 (TOTAL/MONTHLY)
//   - 餐饮月度预算 (CATEGORY/MONTHLY, targetKey="餐饮")
//   - 信用卡周预算 (ACCOUNT/WEEKLY, targetKey=accountId.toString())
//   - 年度旅行总预算 (CATEGORY/YEARLY, targetKey="旅行")
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object BudgetScope {
    const val TOTAL = "TOTAL"        // 全局总预算（targetKey 忽略）
    const val CATEGORY = "CATEGORY"  // 按分类（targetKey = 分类名）
    const val ACCOUNT = "ACCOUNT"    // 按账户（targetKey = accountId）
}

object BudgetPeriod {
    const val MONTHLY = "MONTHLY"    // 自然月
    const val WEEKLY = "WEEKLY"      // 周一到周日
    const val YEARLY = "YEARLY"      // 自然年
}

@Entity(
    tableName = "budgets",
    indices = [
        Index("userId"),
        Index(value = ["userId", "scope", "period"], unique = false)
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认值
    val name: String,                       // 预算名（"月度总预算" / "餐饮控制" 等）
    val scope: String = BudgetScope.TOTAL,
    val targetKey: String? = null,          // CATEGORY: 分类名；ACCOUNT: accountId 字符串；TOTAL: null
    val period: String = BudgetPeriod.MONTHLY,
    val amount: Double,                     // 预算金额（正数）
    val startDate: Long = System.currentTimeMillis(),
    val rollover: Boolean = false,          // 是否将上期未用结余加到下期（暂未实现，预留）
    val warnThreshold: Float = 0.8f,        // 警示阈值（默认 80%）
    val isActive: Boolean = true,
    val color: Long = 0xFFFF8A80L,
    val sortOrder: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
