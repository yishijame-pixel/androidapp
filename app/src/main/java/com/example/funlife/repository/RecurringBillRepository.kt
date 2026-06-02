// RecurringBillRepository.kt — 定期账单仓库（Phase 3）
//
// 🔒 全部 require(userId > 0)
// 🧮 generateDueBills：检测自 lastGeneratedAt 起到现在所有"应发生日"，
//    为每一个未生成的 occurrence 插入一条 Bill，并更新 lastGeneratedAt。
package com.example.funlife.repository

import com.example.funlife.data.dao.BillDao
import com.example.funlife.data.dao.RecurringBillDao
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.RecurringBill
import com.example.funlife.data.model.RecurringPeriod
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RecurringBillRepository(
    private val recurringBillDao: RecurringBillDao,
    private val billDao: BillDao
) {

    fun getAll(userId: Long): Flow<List<RecurringBill>> {
        require(userId > 0L)
        return recurringBillDao.getAll(userId)
    }

    suspend fun getById(userId: Long, id: Long): RecurringBill? {
        require(userId > 0L)
        return recurringBillDao.getById(userId, id)
    }

    suspend fun insert(rb: RecurringBill): Long {
        require(rb.userId > 0L)
        require(rb.amount != 0.0) { "amount must be non-zero" }
        return recurringBillDao.insert(rb)
    }

    suspend fun update(rb: RecurringBill) {
        require(rb.userId > 0L)
        recurringBillDao.update(rb.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(rb: RecurringBill) {
        require(rb.userId > 0L)
        recurringBillDao.delete(rb)
    }

    suspend fun setActive(userId: Long, id: Long, active: Boolean) {
        require(userId > 0L)
        recurringBillDao.setActive(userId, id, active)
    }

    /**
     * 自动生成定期账单。
     * 返回新生成的账单数。
     */
    suspend fun generateDueBills(userId: Long, now: Long = System.currentTimeMillis()): Int {
        require(userId > 0L)
        var generated = 0
        val active = recurringBillDao.getActive(userId)
        active.forEach { rb ->
            // 检测窗口起点：上次生成 OR 起始日（取较大）
            val windowStart = maxOf(rb.lastGeneratedAt, rb.startDate)
            val occurrences = computeOccurrences(rb, windowStart, now)
            if (occurrences.isEmpty()) return@forEach
            occurrences.forEach { occMillis ->
                billDao.insert(
                    Bill(
                        userId = userId,
                        amount = rb.amount,
                        category = rb.category,
                        note = if (rb.note.isBlank()) "📌 ${rb.name}" else "📌 ${rb.name} · ${rb.note}",
                        timestamp = occMillis,
                        accountId = rb.accountId
                    )
                )
                generated++
            }
            recurringBillDao.markGenerated(userId, rb.id, occurrences.last())
        }
        return generated
    }

    /**
     * 计算 [windowStart, now] 内 RecurringBill 的所有应发生日列表（升序，含端点）。
     * 不含 windowStart 自身（即首次为 lastGeneratedAt 的下一个 occurrence）。
     */
    private fun computeOccurrences(rb: RecurringBill, windowStart: Long, now: Long): List<Long> {
        val list = mutableListOf<Long>()
        if (now <= windowStart) return list
        when (rb.period) {
            RecurringPeriod.MONTHLY -> {
                // 从 windowStart 月度的"应发生日"开始；如果未到、就直接加 1 个月
                val cal = Calendar.getInstance().apply {
                    timeInMillis = windowStart
                    set(Calendar.DAY_OF_MONTH, rb.dayOfPeriod.coerceIn(1, 28))
                    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= windowStart) cal.add(Calendar.MONTH, 1)
                while (cal.timeInMillis <= now) {
                    list.add(cal.timeInMillis)
                    cal.add(Calendar.MONTH, 1)
                }
            }
            RecurringPeriod.WEEKLY -> {
                // dayOfPeriod: 1=周一 ... 7=周日（与 java.time.DayOfWeek 一致）
                val cal = Calendar.getInstance().apply {
                    timeInMillis = windowStart
                    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                // Calendar.DAY_OF_WEEK: 1=周日 2=周一 ... 7=周六
                val target = ((rb.dayOfPeriod % 7) + 1)  // 1->2 (周一)，7->1 (周日)
                while (cal.get(Calendar.DAY_OF_WEEK) != target) cal.add(Calendar.DAY_OF_YEAR, 1)
                if (cal.timeInMillis <= windowStart) cal.add(Calendar.WEEK_OF_YEAR, 1)
                while (cal.timeInMillis <= now) {
                    list.add(cal.timeInMillis)
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RecurringPeriod.YEARLY -> {
                val anchor = Calendar.getInstance().apply { timeInMillis = rb.startDate }
                val cal = Calendar.getInstance().apply {
                    timeInMillis = windowStart
                    set(Calendar.MONTH, anchor.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, anchor.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= windowStart) cal.add(Calendar.YEAR, 1)
                while (cal.timeInMillis <= now) {
                    list.add(cal.timeInMillis)
                    cal.add(Calendar.YEAR, 1)
                }
            }
        }
        return list
    }
}
