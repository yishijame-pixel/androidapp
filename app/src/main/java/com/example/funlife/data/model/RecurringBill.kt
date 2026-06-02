// RecurringBill.kt — 定期账单模板（Phase 3 / DB v50）
//
// 🔒 数据隔离：userId 无默认值；@Index("userId")。
//
// 用法：
//   - 用户创建一条 RecurringBill（房租 / 订阅服务 / 工资）
//   - period + dayOfPeriod 决定每个周期的"应发生日"
//   - 进入 ChatBillScreen 时自动检测：lastGeneratedAt 之后到现在所有"应发生日"
//     都未生成的，一并补充插入 Bill，并更新 lastGeneratedAt
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object RecurringPeriod {
    const val MONTHLY = "MONTHLY"
    const val WEEKLY = "WEEKLY"
    const val YEARLY = "YEARLY"
}

@Entity(
    tableName = "recurring_bills",
    indices = [Index("userId")]
)
data class RecurringBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,                       // 🔒 无默认
    val name: String,                       // "房租" / "Netflix"
    val amount: Double,                     // 含正负号：正=收入，负=支出
    val category: String,                   // 关联分类
    val note: String = "",
    val accountId: Long? = null,            // 关联账户（可空）
    val period: String = RecurringPeriod.MONTHLY,
    /** 月度：日期 1-28；周度：1-7（周一-周日）；年度：以 startDate 月日为准 */
    val dayOfPeriod: Int = 1,
    val isActive: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),  // 起始时间，决定首次应发生日
    val lastGeneratedAt: Long = 0L,                    // 上次自动生成的"应发生日"
    val color: Long = 0xFF7E57C2L,
    val sortOrder: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
