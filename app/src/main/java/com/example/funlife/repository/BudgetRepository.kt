// BudgetRepository.kt — 预算仓库（Phase 2B）
//
// 🔒 全部公开方法 require(userId > 0)。
// 🧮 计算 BudgetProgress：在指定时间窗内聚合已发生支出（amount < 0），与预算金额对比。
package com.example.funlife.repository

import com.example.funlife.data.dao.BillDao
import com.example.funlife.data.dao.BudgetDao
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.Budget
import com.example.funlife.data.model.BudgetPeriod
import com.example.funlife.data.model.BudgetScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 纯计算（无 DB 依赖）：用于在 ViewModel 里把 Flow<List<Budget>> + Flow<List<Bill>> combine 后
 * 实时算出 BudgetProgress 列表，避免每次重新 suspend 调用 DAO。
 */
object BudgetCalc {

    fun compute(
        userId: Long,
        budget: Budget,
        billsInWindow: List<Bill>,
        now: Long = System.currentTimeMillis(),
        windowStart: Long,
        windowEnd: Long
    ): BudgetProgress {
        require(userId > 0L)
        require(budget.userId == userId)
        val used = billsInWindow.asSequence()
            .filter { it.userId == userId }
            .filter { it.amount < 0 }
            .filter { matchScope(budget, it.category, it.accountId) }
            .sumOf { -it.amount }
        val pct = if (budget.amount > 0) (used / budget.amount).toFloat() else 0f
        return BudgetProgress(
            budget = budget,
            used = used,
            percent = pct,
            exceeded = pct >= 1f,
            warned = pct >= budget.warnThreshold,
            periodStart = windowStart,
            periodEnd = windowEnd
        )
    }

    /**
     * 用全量账单 + 当前时间，对一条预算进行计算（自动算出 windowStart/End）。
     */
    fun computeFromAll(
        userId: Long,
        budget: Budget,
        allUserBills: List<Bill>,
        now: Long = System.currentTimeMillis()
    ): BudgetProgress {
        val (s, e) = periodRange(budget.period, now)
        val window = allUserBills.filter { it.timestamp in s until e }
        return compute(userId, budget, window, now, s, e)
    }

    fun periodRange(period: String, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            BudgetPeriod.WEEKLY -> {
                val s = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                s.timeInMillis to s.timeInMillis + 7L * 24 * 60 * 60 * 1000
            }
            BudgetPeriod.YEARLY -> {
                val s = (cal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val e = (s.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                s.timeInMillis to e.timeInMillis
            }
            else -> {
                val s = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val e = (s.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                s.timeInMillis to e.timeInMillis
            }
        }
    }

    private fun matchScope(b: Budget, billCategory: String, billAccountId: Long?): Boolean = when (b.scope) {
        BudgetScope.TOTAL -> true
        BudgetScope.CATEGORY -> b.targetKey?.let { it == billCategory } ?: false
        BudgetScope.ACCOUNT -> b.targetKey?.toLongOrNull()?.let { it == billAccountId } ?: false
        else -> false
    }
}

/**
 * 单条预算的实时进度。
 *
 * @param used 当前周期内已发生的支出（绝对值，正数）
 * @param percent used / amount 比例，可大于 1（超额）
 * @param exceeded 是否已超额（percent >= 1）
 * @param warned 是否触发警示（percent >= warnThreshold）
 */
data class BudgetProgress(
    val budget: Budget,
    val used: Double,
    val percent: Float,
    val exceeded: Boolean,
    val warned: Boolean,
    val periodStart: Long,
    val periodEnd: Long
)

class BudgetRepository(
    private val budgetDao: BudgetDao,
    private val billDao: BillDao
) {

    fun getActiveBudgets(userId: Long): Flow<List<Budget>> {
        require(userId > 0L) { "userId must be > 0" }
        return budgetDao.getActiveBudgets(userId)
    }

    fun getAllBudgets(userId: Long): Flow<List<Budget>> {
        require(userId > 0L)
        return budgetDao.getAllBudgets(userId)
    }

    suspend fun getById(userId: Long, id: Long): Budget? {
        require(userId > 0L)
        return budgetDao.getById(userId, id)
    }

    suspend fun insert(budget: Budget): Long {
        require(budget.userId > 0L) { "Budget.userId must be > 0" }
        require(budget.amount > 0.0) { "Budget.amount must be > 0" }
        return budgetDao.insert(budget)
    }

    suspend fun update(budget: Budget) {
        require(budget.userId > 0L)
        require(budget.amount > 0.0)
        budgetDao.update(budget.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(budget: Budget) {
        require(budget.userId > 0L)
        budgetDao.delete(budget)
    }

    suspend fun setActive(userId: Long, id: Long, active: Boolean) {
        require(userId > 0L)
        budgetDao.setActive(userId, id, active)
    }

    /**
     * 仅当不存在时插入；用于把旧版 SharedPreferences 单笔月度预算迁移成 Budget 行。
     * 返回是否真的插入了。
     */
    suspend fun ensureMonthlyTotal(userId: Long, amount: Double): Boolean {
        require(userId > 0L)
        if (amount <= 0.0) return false
        val existing = budgetDao.findOne(userId, BudgetScope.TOTAL, BudgetPeriod.MONTHLY, null)
        if (existing != null) return false
        budgetDao.insert(
            Budget(
                userId = userId,
                name = "月度总预算",
                scope = BudgetScope.TOTAL,
                period = BudgetPeriod.MONTHLY,
                amount = amount,
                color = 0xFFFF6B9DL,
                sortOrder = 0
            )
        )
        return true
    }

    /**
     * 计算单条预算的实时进度（DB 查询版）。
     */
    suspend fun computeProgress(userId: Long, budget: Budget, now: Long = System.currentTimeMillis()): BudgetProgress {
        require(userId > 0L) { "userId must be > 0" }
        require(budget.userId == userId) { "Budget.userId mismatch" }
        val (start, end) = BudgetCalc.periodRange(budget.period, now)
        val bills = billDao.getBillsByDateRange(userId, start, end)
        return BudgetCalc.compute(userId, budget, bills, now, start, end)
    }

    /**
     * 批量计算所有激活预算的进度（DB 查询版）。
     */
    suspend fun computeAllProgress(userId: Long, now: Long = System.currentTimeMillis()): List<BudgetProgress> {
        require(userId > 0L)
        val budgets = budgetDao.getActiveBudgets(userId).first()
        return budgets.map { computeProgress(userId, it, now) }
    }

}

